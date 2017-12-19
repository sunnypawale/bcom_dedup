package com.agoda.bcom.dedup.repository

import com.agoda.bcom.dedup.AppLogger
import org.joda.time.{DateTime, LocalDate}
import com.agoda.bcom.dedup.utils.SqlUtils._

/**
  * Created by npawale on 12/16/17.
  */

case class DeDupRepoMD5(key: String
                        , hash: Array[Byte], from: LocalDate, to: LocalDate, ctFromTime: DateTime, ctToTime: DateTime, isDedup: Boolean)

case class DeDupRepoCRC32(key: String
                          , hash: Int, from: LocalDate, to: LocalDate, ctFromTime: DateTime, ctToTime: DateTime, isDedup: Boolean)

case class DeDupRepo(key: String, hash: Array[Byte], from: LocalDate, to: LocalDate, ctFromTime: DateTime, ctToTime: DateTime, isDedup: Boolean,
                     avail: String = "")


class DedupRepository(db: DB) extends AppLogger {

  def createDeDupTable() = {
    db.withConnection(conn => {
      val preparedStatement = conn.prepareStatement(RawSql.Create_Dedup_table)
      preparedStatement.executeUpdate()
      preparedStatement.close()
    })
  }

  def insert(avails: List[DeDupRepo]) = {
    db.withConnection(conn => {
      val preparedStatement = conn.prepareStatement(RawSql.Insert_Dedup)
      avails.foreach { av =>
        preparedStatement.setString(1, av.key)
        preparedStatement.setBytes(2, av.hash)
        preparedStatement.setDate(3, av.from.toSqlDate)
        preparedStatement.setDate(4, av.to.toSqlDate)
        preparedStatement.setString(5, av.ctFromTime.toString)
        preparedStatement.setString(6, av.ctToTime.toString)
        preparedStatement.setBoolean(7, av.isDedup)
        preparedStatement.setString(8, av.avail)
        preparedStatement.addBatch()
      }
      preparedStatement.executeBatch()
      preparedStatement.close()
    })
  }

}