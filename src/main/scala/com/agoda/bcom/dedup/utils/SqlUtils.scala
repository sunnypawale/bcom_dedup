package com.agoda.bcom.dedup.utils

import java.sql
import java.sql.Timestamp

import org.joda.time.{DateTime, DateTimeZone, LocalDate}

object SqlUtils {

  implicit class JodaDateTimeToSql(d: DateTime) {
    def toSql: Timestamp = new Timestamp(d.getMillis)

    def toSqlDate: sql.Date = new sql.Date(d.getMillis)

  }

  implicit class JodaLocalDateToSql(d: LocalDate) {
    def toSql: Timestamp = new Timestamp(d.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis)

    def toSqlDate: sql.Date = new sql.Date(d.toDateTimeAtStartOfDay(DateTimeZone.UTC).getMillis)

  }

}
