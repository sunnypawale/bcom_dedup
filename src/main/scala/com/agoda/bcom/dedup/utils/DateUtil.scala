package com.agoda.bcom.dedup.utils

import org.joda.time.{DateTime, LocalDate}
import org.joda.time.format.DateTimeFormat

object DateUtil {

  val fmtYYYYMMDD = DateTimeFormat.forPattern("yyyy-MM-dd")


  case class DateStep(start: LocalDate, end: LocalDate)

  def dateBucket(start: LocalDate, end: LocalDate, bucketSize: Int = 10): List[DateStep] = {
    val dates = Iterator.iterate(start)(_.plusDays(1)).takeWhile(!_.isAfter(end)).toList
    dates.grouped(bucketSize).map(group => {
      val gStart = group.head
      val gEnd = group.lastOption.getOrElse(group.head)
      DateStep(gStart, gEnd)
    }).toList
  }

  def parseYYYYMMDD(d: String): DateTime = fmtYYYYMMDD.parseDateTime(d)

  def toYYYYMMDD(d: DateTime): String = fmtYYYYMMDD.print(d)
}
