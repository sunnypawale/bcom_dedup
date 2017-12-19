package com.agoda.bcom.dedup.repository

import java.sql.Connection

/**
  * Created by npawale on 12/16/17.
  */
trait DB {

  def withConnection[A](block: Connection => A): A
  def shutdown(): Unit
}
