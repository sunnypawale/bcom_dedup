package com.agoda.bcom.dedup.utils

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Future}

object FutureUtils {

  implicit class FutureWait[T](val f: Future[T]) extends AnyVal {
    def get(duration: FiniteDuration): T = Await.result(f, duration)
  }
}
