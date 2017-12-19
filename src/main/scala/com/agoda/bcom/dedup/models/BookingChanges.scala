package com.agoda.bcom.dedup.models

import com.fasterxml.jackson.annotation.JsonProperty
import org.joda.time.{DateTime, DateTimeZone, LocalDate}
import org.joda.time.format.DateTimeFormat

import scala.concurrent.duration._

case class BookingHotelChange(@JsonProperty("hotel_id") hotelId: Int,
                              @JsonProperty("room_ids") roomIds: Set[Long])

case class BookingChange(first: LocalDate, last: LocalDate, items: List[BookingHotelChange])

case class BookingChanges(from: DateTime,
                          to: Option[DateTime],
                          last: DateTime,
                          changes: List[BookingChange])

object BookingChanges {
  val fmt = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC()

  def dateToMillis(date: LocalDate): Long = date.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis

  implicit class BookingChangeNext(val change: BookingChanges) extends AnyVal {

    def getNextFrom(oldFromTime: DateTime): DateTime = {
      change.to.map { to => to.plusSeconds(1) }.getOrElse(oldFromTime)
    }
  }

  def toISO8601(d: DateTime): String = {
    fmt.print(d)
  }

  def getNow(minus: FiniteDuration = 120.seconds): DateTime = DateTime.now().minusMillis(minus.toMillis.toInt).withMillisOfSecond(0)

  def getNextTo(from: String, shift: FiniteDuration) = {
    val dateTime = fmt.parseDateTime(from)
    val duration = System.currentTimeMillis() - dateTime.getMillis
    fmt.print(dateTime.plusMillis(duration.toInt + shift.toMillis.toInt))
  }

}