package com.agoda.bcom.dedup.actors

import akka.actor.{Actor, Props}
import com.agoda.bcom.dedup.AppLogger
import com.agoda.bcom.dedup.actors.AvRoomWorker.Change
import com.agoda.bcom.dedup.client.BookingClient
import com.agoda.bcom.dedup.models.{BlocKey, BookingAvailability, ChangeTracking}
import com.agoda.bcom.dedup.repository.{DeDupRepo, DedupRepository}
import com.agoda.bcom.dedup.utils.JsonUtil
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.LocalDate

import scala.collection.mutable
import scala.util.{Failure, Success}

/**
  * Created by npawale on 12/16/17.
  */
class AvRoomWorker(bcomClient: BookingClient, dbRepo: DedupRepository, withMD5: Boolean, withAvail: Boolean) extends Actor with AppLogger {

  implicit val ec = context.system.dispatcher

  val localHashMap = new mutable.HashMap[String, Array[Byte]]()

  override def receive: Receive = {
    case Change(ct) => {

      logger.info(s"Change Data for: ${ct.hotelCode}...${ct.fromDate}... ${ct.toDate} ")
      var fromDate = ct.fromDate
      var toDate = ct.toDate
      val minFromDate = LocalDate.now().minusDays(1)
      val maxToDate = minFromDate.plusDays(180)
      if (ct.fromDate.isBefore(minFromDate)) {
        logger.warn(s"From date $fromDate is before min allowed 'from date'")
        fromDate = minFromDate
      }
      if (toDate.isAfter(maxToDate)) {
        logger.warn(s" To date $toDate is after max allowed 'to date' $maxToDate. Use $maxToDate as toDate")
        toDate = maxToDate.plusDays(1)
      }
      val availabilitiesFutr = bcomClient.getRoomAvailabilities(ct.roomCodes, fromDate, toDate)
      availabilitiesFutr.onComplete {
        case Success(Left(result)) =>
          if (result.available.nonEmpty) {
            // select cheapest price
            val data = result.available.groupBy(_.blockKey).map { case (blockKey, avils) =>
              val cheapestPriceAv = avils.sortBy(_.price).head
              val currentHash = cheapestPriceAv.getHash(withMD5)
              val isDedup = checkForDedup(blockKey, currentHash)
              val rawAvail = getRawAvail(cheapestPriceAv)
              DeDupRepo(blockKey.key, currentHash, fromDate, toDate, ct.ctFromTime, ct.ctToTime, isDedup, rawAvail)
            }.toList
            dbRepo.insert(data)
          } else {
            logger.info(s"Empty Availabilities for ${ct.roomCodes}, from:$fromDate, to:$toDate ")
          }
        case Success(Right(error)) =>
          logger.error(s"Error while polling for av rooms  ${ct.roomCodes}, from:$fromDate, to:$toDate Msg:$error ")
        case Failure(ex) =>
          logger.error(s"Future failed while polling for changes Msg: ${ex.getMessage}")
      }
    }
  }

  def getRawAvail(cheapestPriceAv: BookingAvailability): String = {
    if (withAvail) {
      JsonUtil.toJson(cheapestPriceAv)
    } else {
      ""
    }
  }

  private def checkForDedup(blockKey: BlocKey, currentHash: Array[Byte]): Boolean = {
    localHashMap.get(blockKey.key) match {
      case Some(oldHash) if currentHash sameElements oldHash => true
      case _ =>
        localHashMap.put(blockKey.key, currentHash)
        false
    }
  }
}

object AvRoomWorker {

  case class Change(change: ChangeTracking)

  def props(bcomClient: BookingClient, dbRepo: DedupRepository, withMD5: Boolean, withAvail: Boolean) = {
    Props(new AvRoomWorker(bcomClient, dbRepo, withMD5, withAvail))
  }
}

