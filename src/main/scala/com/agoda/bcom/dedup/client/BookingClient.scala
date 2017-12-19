package com.agoda.bcom.dedup.client

import com.agoda.bcom.dedup.AppLogger
import com.agoda.bcom.dedup.models.{BookingAvailabilities, BookingChanges}
import com.agoda.bcom.dedup.utils.JsonUtil
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.ws.WSAuthScheme.BASIC
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.{ExecutionContext, Future}


case class ChangeParams(from: DateTime, to: DateTime) {
  def toHttpQueryString = s"/json/avChanges?from=$from&to=$to"
}

case class AvailParams(key: String, ids: Set[Long], from: LocalDate, to: LocalDate) {
  private val fmt = DateTimeFormat.forPattern("yyyy-MM-dd")
  val formattedFrom: String = fmt.print(from)
  val formattedTo: String = fmt.print(to)

  def toHttpQueryString: String = s"/json/avRooms?$key=${ids.toList.mkString(",")}" +
    s"&first_checkin=$formattedFrom" +
    s"&last_checkin=$formattedTo"
}

case class ResponseStatus[A](result: Option[A], error: List[String] = List.empty)


class BookingClient(client: StandaloneAhcWSClient, username: String, password: String, url: String)
                   (implicit ec: ExecutionContext) extends AppLogger {

  private val changeEndpoint = "https://distribution-xml.booking.com"

  // Agoda network need proxy to connect
  //private val httpProxyServer: Option[WSProxyServer] = Some(DefaultWSProxyServer("sisproxy.ams.agoda.local", 3128))


  def getChanges(from: DateTime, to: DateTime) = {
    implicit val m = classOf[BookingChanges]
    invoke[BookingChanges](url + ChangeParams(from, to).toHttpQueryString, "changes")
  }


  def getRoomAvailabilities(roomIds: Set[Long], firstCheckIn: LocalDate, lastCheckIn: LocalDate) = {
    implicit val m = classOf[BookingAvailabilities]
    invoke[BookingAvailabilities](url + AvailParams("room_ids", roomIds, firstCheckIn, lastCheckIn).toHttpQueryString,
      "avRooms")
  }

  private def invoke[A](url: String, callType: String)(implicit m: Manifest[A]): Future[Either[A, String]] = {
    val request = client.url(url).withAuth(username, password, BASIC)
    /*if (useProxy) {
      httpProxyServer.foreach(ps => request = request.withProxyServer(ps))
    }*/
    val responseFut = request.get
    val result =
      responseFut.map { response =>
        val resp: Either[A, String] = response.status match {
          case 200 =>
            if (response.body.contains("ruid")) {
                Right(s"Error From BCOM Call  for url : $url  ${response.body}")
            } else {
              Left(JsonUtil.fromJson[A](response.body))
            }
          case invalidCode =>
            Right(s"Failed BCOM Call  Code: $invalidCode for url : $url")
        }
        resp
      }

    responseFut.onFailure { case ex =>
      logger.error(s"[$callType] Failed to send request for url : $url an ex: ${ex.getMessage}")
    }
    result
  }

}
