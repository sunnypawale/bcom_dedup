package com.agoda.bcom.dedup

import java.sql.DriverManager

import com.agoda.bcom.dedup.repository.RawSql
import org.h2.tools.{DeleteDbFiles, Server}
import org.joda.time.{DateTime, LocalDate}
import com.agoda.bcom.dedup.utils.SqlUtils._

object dedup extends App {
  Class.forName("org.h2.Driver")
  val connection = DriverManager.
    getConnection("jdbc:h2:./sunny")
  openServerModeInBrowser()

  DeleteDbFiles.execute(".", "sunny", true)

  try {

    val stmt = connection.createStatement()
    stmt.execute(RawSql.Create_Dedup_table)
    stmt.close()
    val q = RawSql.Insert_Dedup
    val preparedStatement = connection.prepareStatement(q);
    //preparedStatement.setInt(1, 1)
    preparedStatement.setString(1, "Key_value2")
    preparedStatement.setInt(2, 123)
    preparedStatement.setDate(3, new LocalDate().toSqlDate)
    preparedStatement.setDate(4, new LocalDate().toSqlDate)
    preparedStatement.setTimestamp(5, new DateTime().toSql)
    preparedStatement.executeUpdate()
    preparedStatement.close()

    val stmt1 = connection.createStatement()
    val rs = stmt1.executeQuery("select * from DeDup")
    println("H2 Database inserted through Statement")
    while (rs.next()) {
      println("Id " + rs.getInt("id") + " Key " + rs.getString("Key") + "hash" + rs.getInt("hash") + "from" + rs.getDate("from_date")
        + "to" + rs.getString("to_date") + " lo" + rs.getTimestamp("cd_call_time"))
    }
    stmt1.close()
  }


  def openServerModeInBrowser() {
    val server = Server.createWebServer().start()
    println("Server started and connection is open.")
    println("URL: jdbc:h2:" + server.getURL() + "/:./sunny")
  }
}
