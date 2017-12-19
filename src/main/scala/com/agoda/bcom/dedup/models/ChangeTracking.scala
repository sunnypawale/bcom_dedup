package com.agoda.bcom.dedup.models

import org.joda.time.{DateTime, DateTimeZone, LocalDate}

/**
  * Created by npawale on 12/14/17.
  */
case class ChangeTracking(hotelCode: Int,
                          fromInMillis: Long,
                          toInMillis: Long,
                          ctFromTime: DateTime,
                          ctToTime: DateTime,
                          roomCodes: Set[Long]) {

  val fromDate: LocalDate = dateFromMillis(fromInMillis)
  val toDate: LocalDate = dateFromMillis(toInMillis)


  private def dateFromMillis(millis: Long): LocalDate = new LocalDate(millis, DateTimeZone.UTC)


}