package com.agoda.bcom.dedup.repository

import java.sql.Connection

import com.agoda.bcom.dedup.utils.Utils
import com.typesafe.scalalogging.LazyLogging
import org.h2.jdbcx.JdbcConnectionPool
import org.h2.tools.DeleteDbFiles

/**
  * Created by npawale on 12/16/17.
  */


class DBPlugin extends DB {

  import DBPlugin._

  override def withConnection[A](block: (Connection) => A): A = {
    Utils.retry(3) {
      val connection = connections.getConnection
      try {
        block(connection)
      } finally {
        connection.close()
      }
    }
  }

  def createNewDB(isOKay: Boolean) = {
    if (isOKay) {
      DeleteDbFiles.execute(DB_LOCATION, DB_NAME, true)
    } else {

    }
  }

  def shutdown(): Unit = {
    connections.dispose()
  }

}

object DBPlugin extends LazyLogging {

  val DB_DRIVER = "org.h2.Driver"
  val DB_NAME = "dedup"
  val DB_LOCATION = "./data/local"
  val DB_CONNECTION = s"jdbc:h2:$DB_LOCATION/$DB_NAME"

  Class.forName(DB_DRIVER)
  val connections = JdbcConnectionPool.create(DB_CONNECTION, "", "")

}
