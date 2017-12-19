package com.agoda.bcom.dedup

import com.typesafe.scalalogging.Logger
import org.slf4j.LoggerFactory

/**
  * Created by npawale on 12/18/17.
  */
trait AppLogger {
  private val _logger = Logger(LoggerFactory.getLogger(this.getClass))

  def logger = _logger
}
