package com.agoda.bcom.dedup.actors

import java.util

import akka.actor.{Actor, Props}
import com.agoda.bcom.dedup.AppLogger
import com.agoda.bcom.dedup.actors.AvRoomWorker.Change
import com.agoda.bcom.dedup.client.BookingClient
import com.agoda.bcom.dedup.models._
import com.agoda.bcom.dedup.repository.{DeDupRepo, DedupRepository}
import com.agoda.bcom.dedup.utils.JsonUtil
import org.joda.time.{DateTime, LocalDate}

import scala.util.{Failure, Success}

/**
  * Created by npawale on 12/16/17.
  */
class AvRoomWorker(bcomClient: BookingClient, dbRepo: DedupRepository, withMD5: Boolean, withAvail: Boolean) extends Actor with AppLogger {

  implicit val ec = context.system.dispatcher

  //val localHashMap = new java.util.HashMap[String, Array[Byte]](1)

  val localHashMap1 = new java.util.HashMap[String, java.util.HashMap[String, Array[Byte]]](23000)

  override def receive: Receive = {
    case Change(ct) => {

      logger.info(s"Change Data for: ${ct.hotelCode}...${ct.fromDate}... ${ct.toDate} >>>>>")
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
            //val keys = result.available.groupBy(_.roomLevelKey)
            val data = result.available.groupBy(_.roomLevelKey).flatMap { case (roomLevelKey, availsByRoom) =>
              val blockLevelMap = availsByRoom.groupBy(_.blockLevelKey).map { case (blockLevelKey, blockLevelAvail) =>
                val cheapestPriceAv = blockLevelAvail.sortBy(_.price).head
                blockLevelKey -> cheapestPriceAv
              }
              checkForDedup(roomLevelKey, blockLevelMap, fromDate, toDate, ct.ctFromTime, ct.ctToTime)

              /*val cheapestPriceAv = availsByRoom.sortBy(_.price).head
              val currentHash = cheapestPriceAv.getHash(withMD5)
              val isDedup = checkForDedup1(roomLevelKey, blockKey, currentHash)
              val rawAvail = getRawAvail(cheapestPriceAv)
              DeDupRepo(blockKey.key, currentHash, fromDate, toDate, ct.ctFromTime, ct.ctToTime, isDedup, rawAvail)*/
            }.toList
            removeCloseOut(result.unavailable)
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

  private def removeCloseOut(unavailable: List[BookingUnavailables]) = {
    if (unavailable.nonEmpty) {
      unavailable.foreach { checkInUnavail =>
        checkInUnavail.byLengthOfStays.foreach { hotelsByLos =>
          hotelsByLos.hotels.foreach { hotelRooms =>
            hotelRooms.rooms.foreach { roomId =>
              logger.info(s" Rooom Id $roomId")
              val roomKey = RoomLevelKey(hotelRooms.hotelId, checkInUnavail.checkIn, hotelsByLos.lengthOfStay, roomId)
              val k = localHashMap1.remove(roomKey.key)
              if (k == null) {
                logger.info(s" dedup*  Null removed $roomKey")
              } else {
                logger.info(s" dedup* Actual value removed $roomKey")
              }
            }
          }
        }
      }
    }
  }

  private def checkForDedup(roomLevelKey: RoomLevelKey, blockLevelMap: Map[BlockLevelKey, BookingAvailability],
                            fromDate: LocalDate, toDate: LocalDate,
                            ctFromTime: DateTime,
                            ctToTime: DateTime) = {
    val roomLevelMap = localHashMap1.get(roomLevelKey.key)
    val repos = if (roomLevelMap == null) {
      createBlockLevelMapValue(roomLevelKey, blockLevelMap, fromDate, toDate, ctFromTime, ctToTime)
    } else {
      checkForDedupAtBlockLevel(roomLevelKey, roomLevelMap, blockLevelMap, fromDate, toDate, ctFromTime, ctToTime)
    }
    repos.toList
  }

  private def createBlockLevelMapValue(roomLevelKey: RoomLevelKey,
                                       blockLevelMap: Map[BlockLevelKey, BookingAvailability],
                                       fromDate: LocalDate,
                                       toDate: LocalDate,
                                       ctFromTime: DateTime,
                                       ctToTime: DateTime): Iterable[DeDupRepo] = {

    logger.info(s" dedup* Creating new map for ${roomLevelKey.key}")
    val roomLevelMapValue = new java.util.HashMap[String, Array[Byte]]()
    val dedupRpo = blockLevelMap.map { case (key, value) =>
      val currentHash = value.getHash(withMD5)
      roomLevelMapValue.put(key.key, value.getHash(withMD5))
      val rawAvail = getRawAvail(value)
      DeDupRepo(value.blockKey.key, currentHash, fromDate, toDate, ctFromTime, ctToTime, false, rawAvail)
    }
    localHashMap1.put(roomLevelKey.key, roomLevelMapValue)

    dedupRpo
  }

  private def checkForDedupAtBlockLevel(roomLevelKey: RoomLevelKey, roomLevelMap: util.HashMap[String, Array[Byte]],
                                        blockLevelMap: Map[BlockLevelKey, BookingAvailability],
                                        fromDate: LocalDate,
                                        toDate: LocalDate,
                                        ctFromTime: DateTime,
                                        ctToTime: DateTime): Iterable[DeDupRepo] = {
    logger.info(s" dedup* updating old map for ${roomLevelKey.key}")
    val dedupRpo = blockLevelMap.map { case (key, value) =>
      val hashValue = roomLevelMap.get(key.key)
      val currentHash = value.getHash(withMD5)
      val rawAvail = getRawAvail(value)
      if (hashValue != null && java.util.Arrays.equals(hashValue, currentHash)) {
        DeDupRepo(value.blockKey.key, currentHash, fromDate, toDate, ctFromTime, ctToTime, true, rawAvail)
      } else {
        roomLevelMap.put(key.key, currentHash)
        DeDupRepo(value.blockKey.key, currentHash, fromDate, toDate, ctFromTime, ctToTime, false, rawAvail)
      }
    }
    dedupRpo
  }


 /* private def checkForDedupOld(blockKey: BlocKey, currentHash: Array[Byte]): Boolean = {
    val hashValue = localHashMap.get(blockKey.key)
    if (hashValue != null && java.util.Arrays.equals(hashValue, currentHash)) {
      true
    } else {
      localHashMap.put(blockKey.key, currentHash)
      false
    }
  }*/
}

object AvRoomWorker {

  case class Change(change: ChangeTracking)

  def props(bcomClient: BookingClient, dbRepo: DedupRepository, withMD5: Boolean, withAvail: Boolean) = {
    Props(new AvRoomWorker(bcomClient, dbRepo, withMD5, withAvail))
  }
}

