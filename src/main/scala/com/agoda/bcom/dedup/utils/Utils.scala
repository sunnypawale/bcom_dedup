package com.agoda.bcom.dedup.utils

import scala.util.{Failure, Success, Try}

/**
  * Created by npawale on 12/18/17.
  */
object Utils {

  @annotation.tailrec
  def retry[T](n: Int = 3)(fn: => T): T = {
    Try {
      fn
    } match {
      case Success(x) => x
      case _ if n > 1 => retry(n - 1)(fn)
      case Failure(e) => throw e
    }
  }
}
