package com.agoda.bcom.dedup.service


import akka.actor.{ActorRef, ActorSystem}
import com.agoda.bcom.dedup.AppLogger
import com.agoda.bcom.dedup.actors.AvRoomActor.PollForAvRooms
import com.agoda.bcom.dedup.client.BookingClient
import com.agoda.bcom.dedup.models.{BookingChange, BookingChanges, ChangeTracking}
import com.agoda.bcom.dedup.utils.DateUtil
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.{DateTime, DateTimeZone}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.util.control.NonFatal

class DiscoveryService(bcomClient: BookingClient, whiteListHotels: Set[Int], actorRef: ActorRef)
                      (implicit actorSystem: ActorSystem, ec: ExecutionContext) extends AppLogger {


  @volatile private var nextPollFormTime: Option[DateTime] = None
  @volatile private var lastApiCallTimestamp: DateTime = DateTime.now()
  @volatile private var pollDelay = 5.second

  def nextSchedule(delay: FiniteDuration): Unit = {
    logger.info(s" Scheduled to poll with delay:$delay and pollFormTime:$nextPollFormTime")
    actorSystem.scheduler.scheduleOnce(delay) {
      logger.info(s"${DateTime.now()}")
      pollForChange()
    }
  }

  private def pollForChange(): Unit = {
    try {
      logger.info(s"Poll For BCOM Change")
      val toTime = DateTime.now(DateTimeZone.UTC).withMillisOfSecond(0)
      val fromTime = getFromTimestamp(toTime)
      logger.info(s"Requesting changes: from: $fromTime, to: $toTime")
      bcomClient.getChanges(fromTime, toTime).onComplete {
        case Success(Left(result)) =>
          logger.info(s"Got changes: from: ${result.from}, to: ${result.to}, last: ${result.last} with Number of Changes: ${result.changes.size}")
          nextPollFormTime = Some(result.getNextFrom(fromTime))

          val changeTrackings = createChangeTrackingMsgs(result.changes, fromTime, toTime)
          actorRef ! PollForAvRooms(changeTrackings)
          nextSchedule(getPollDelay(toTime, result))
        case Success(Right(error)) =>
          logger.error(s"Error while polling for changes Msg: $error")
          nextSchedule(pollDelay)
        case Failure(ex) =>
          logger.error(s"Future failed while polling for changes Msg: ${ex.getMessage}")
          nextSchedule(pollDelay)
      }
    } catch {
      case NonFatal(ex) =>
        logger.error(s"Exception while polling for changes Msg: ${ex.getMessage}")
        nextSchedule(pollDelay)
    }

  }

  private def getPollDelay(toTime: DateTime, result: BookingChanges) = {
    if (result.to.isDefined && toTime.isAfter(result.to.get)) {
      0.seconds
    } else {
      pollDelay
    }
  }

  private def getFromTimestamp(now: DateTime): DateTime = {
    nextPollFormTime.getOrElse(now.minusSeconds(10))
  }

  private def createChangeTrackingMsgs(changes: List[BookingChange], from: DateTime, to: DateTime): List[ChangeTracking] = {
    val changeTrackings = changes.flatMap(change => {

      //println(change.items.map(_.hotelId).mkString(","))

      change.items
        .filter(hotelRooms => whiteListHotels.contains(hotelRooms.hotelId))
        .flatMap { hotelRooms =>
          DateUtil.dateBucket(change.first, change.last, 10).map { step =>
            ChangeTracking(hotelRooms.hotelId,
              BookingChanges.dateToMillis(step.start),
              BookingChanges.dateToMillis(step.end),
              from,
              to,
              hotelRooms.roomIds)
          }
        }
    })
    logger.info(s"Generated ${changeTrackings.size} change Tracking out of ${changes.size} changes")
    changeTrackings
  }
}
