package com.agoda.bcom.dedup.actors

import akka.actor.{Actor, Props, Terminated}
import akka.routing._
import com.agoda.bcom.dedup.AppLogger
import com.agoda.bcom.dedup.actors.AvRoomActor.PollForAvRooms
import com.agoda.bcom.dedup.client.BookingClient
import com.agoda.bcom.dedup.models.ChangeTracking
import com.agoda.bcom.dedup.repository.DedupRepository
import com.typesafe.scalalogging.LazyLogging

/**
  * Created by npawale on 12/16/17.
  */
class AvRoomActor(bcomClient: BookingClient, dbRepo: DedupRepository, numberOfWorker: Int, withMD5: Boolean, withAvail: Boolean) extends Actor with AppLogger {

  implicit val ec = context.system.dispatcher

  private var router = {
    val workers = Vector.fill(numberOfWorker)(createWorker())
    Router(ConsistentHashingRoutingLogic(context.system), workers)
  }

  override def preStart(): Unit = {
    logger.info(s" Starting AvRoomActor")
  }

  override def receive: Receive = {
    case PollForAvRooms(changeTrackings) =>
      // logger.info(s"Received ${changeTrackings.size} change tracking to process")
      changeTrackings.foreach { change =>
        router.route(new ConsistentHashingRouter.ConsistentHashableEnvelope(AvRoomWorker.Change(change), change.hotelCode), sender)
      }
    case Terminated(ref) =>
      logger.warn(s" ############ Worker Terminated  #################")
      router = router.removeRoutee(ref)
      router = router.addRoutee(createWorker())
  }

  private def createWorker(): ActorRefRoutee = {
    val worker = context.actorOf(AvRoomWorker.props(bcomClient, dbRepo, withMD5, withAvail))
    context watch worker
    ActorRefRoutee(worker)
  }
}

object AvRoomActor {

  case class PollForAvRooms(changeTrackings: List[ChangeTracking])

  def props(bcomClient: BookingClient, dbRepo: DedupRepository, numberOfWorker: Int, withMD5: Boolean, withAvail: Boolean) = {
    Props(new AvRoomActor(bcomClient, dbRepo, numberOfWorker, withMD5, withAvail))
  }
}
