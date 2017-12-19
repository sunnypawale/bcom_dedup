package com.agoda.bcom.dedup.utils

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility
import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.{DeserializationFeature, ObjectMapper}
import com.fasterxml.jackson.datatype.joda.JodaModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import com.fasterxml.jackson.module.scala.experimental.ScalaObjectMapper


object JsonUtil {
  val mapper = new ObjectMapper() with ScalaObjectMapper
  val simpleModule = new SimpleModule()
  val jodaModule = new JodaModule()
  mapper.registerModule(DefaultScalaModule)
  mapper.registerModule(simpleModule)
  mapper.registerModule(jodaModule)

  mapper.setSerializationInclusion(Include.NON_NULL)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  mapper.setVisibility(PropertyAccessor.CREATOR, Visibility.NONE)
  mapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE)
  mapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE)
  mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY)

  def toJson(value: Map[Symbol, Any]): String = {
    toJson(value map { case (k, v) => k.name -> v })
  }

  def toJson(value: Any): String = {
    val st = mapper.writeValueAsString(value)
    return st
  }

  def toMap[V](json: String)(implicit m: Manifest[V]) = fromJson[Map[String, V]](json)

  def fromJson[T](json: String)(implicit m: Manifest[T]): T = {
    mapper.readValue[T](json)
  }

  def fromJsonBytes[T](json: Array[Byte])(implicit m: Manifest[T]): T = {
    mapper.readValue[T](json)
  }

}
