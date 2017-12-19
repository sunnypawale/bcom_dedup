package com.agoda.bcom.dedup.repository

/**
  * Created by npawale on 12/16/17.
  */
object RawSql {


  //val Create_Dedup_CRC32_table = "CREATE TABLE IF NOT EXISTS DeDup_CRC32(id bigint auto_increment, key varchar(60), hash int, from_date DATE, to_date DATE, cd_from_time varchar(30),  cd_to_time varchar(30), is_dedup BOOLEAN)"

  //val Create_Dedup_MD5_table = "CREATE TABLE IF NOT EXISTS DeDup_MD5(id bigint auto_increment, key varchar(60), hash BINARY(16), from_date DATE, to_date DATE, cd_from_time varchar(30),  cd_to_time varchar(30), is_dedup BOOLEAN)"

  val Create_Dedup_table = "CREATE TABLE IF NOT EXISTS DeDup(id bigint auto_increment, key varchar(60), hash BINARY(16), from_date DATE, to_date DATE, cd_from_time varchar(30),  cd_to_time varchar(30), is_dedup BOOLEAN, avail varchar(1000))"

  val Insert_Dedup = "INSERT INTO DeDup" + "(key, hash, from_date, to_date, cd_from_time, cd_to_time, is_dedup, avail) values" + "(?,?,?,?,?,?,?,?)"

}
