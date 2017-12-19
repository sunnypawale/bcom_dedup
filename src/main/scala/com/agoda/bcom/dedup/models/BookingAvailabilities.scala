package com.agoda.bcom.dedup.models

import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.zip.CRC32

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties, JsonInclude, JsonProperty}
import com.google.common.io.ByteStreams

case class BlocKey(hotelId: Int, checkIn: String, los: Short, blockId: String, channelId: Short) {
  @JsonIgnore lazy val key = s"${hotelId}_${checkIn}_${los}_${blockId}_$channelId"
}

case class BookingCharge(id: Int,
                         amount: Double,
                         @JsonProperty("charge_amount") chargeAmount: Double,
                         mode: Int)

case class BookingCharges(inc: List[BookingCharge],
                          @JsonInclude(Include.NON_NULL) exc: List[BookingCharge])

case class BookingAvailability(@JsonProperty("hotel_id") hotelId: Int,
                               @JsonProperty("checkin") checkIn: String,
                               @JsonProperty("los") lengthOfStay: Short,
                               @JsonProperty("room_id") roomId: Long,
                               @JsonProperty("block_id") blockId: String,
                               @JsonProperty("occupancy") occupancy: Short,
                               @JsonProperty("policygroup_id") policyGroupId: Long,
                               @JsonProperty("mealplan") mealPlan: Short,
                               @JsonProperty("max_persons") maxPersons: Short,
                               @JsonProperty("nr_stays") numberOfStays: Short,
                               @JsonProperty("currency") currency: String,
                               @JsonProperty("channel") channel: Short,
                               @JsonProperty("price") price: Double,
                               @JsonProperty("free_cancel_until") freeCancelUntil: Long,
                               @JsonProperty("cancel_until_fee") cancelUntilFee: Long,
                               @JsonProperty("cancel_until_time") cancelUntilTime: Long,
                               @JsonProperty("no_wcc") isNoCreditCard: Boolean,
                               @JsonProperty("deposit_required") isDepositRequired: Boolean,
                               charges: BookingCharges) {

  @JsonIgnore lazy val blockKey = BlocKey(hotelId, checkIn, lengthOfStay, blockId, channel)

  // be careful. changing this method causes hashes mismatch
  private def toBinary: Array[Byte] = {
    val o = ByteStreams.newDataOutput
    o.writeInt(hotelId)
    o.write(checkIn.getBytes)
    o.writeShort(lengthOfStay)
    o.write(blockId.getBytes)
    o.writeShort(channel)
    o.writeDouble(cancelUntilFee)
    o.writeLong(cancelUntilTime)
    o.writeLong(freeCancelUntil)
    o.writeDouble(price)
    o.write(currency.getBytes)
    o.writeShort(maxPersons)
    o.writeShort(if (isNoCreditCard) 1 else 0)
    o.writeShort(numberOfStays)
    o.writeShort(if (isDepositRequired) 1 else 0)

    def putCharge(c: BookingCharge): Unit = {
      o.writeInt(c.id)
      o.writeDouble(c.amount)
      o.writeDouble(c.chargeAmount)
      o.writeInt(c.mode)
    }

    if (charges.exc != null) {
      charges.exc.sortBy(_.id).foreach(c => {
        putCharge(c)
      })
    }

    charges.inc.sortBy(_.id).foreach(c => {
      putCharge(c)
    })

    o.toByteArray
  }

   def getHash(withMD5: Boolean) = {
    withMD5 match {
      case true => getCrcMD5()
      case _ => toByteArray(getCrc32())

    }
  }

  private def toByteArray(a: Int) = {
    val buffer = ByteBuffer.allocate(4)
    buffer.putInt(a)
    buffer.array()
  }

  private def getCrc32(): Int = {
    val crc32 = new CRC32()
    crc32.update(toBinary)
    crc32.getValue.toInt
  }

  private def getCrcMD5(): Array[Byte] = {
    val md5 = MessageDigest.getInstance("MD5")
    md5.update(toBinary)
    md5.digest()
  }

}

case class BookingHotelRooms(@JsonProperty("hotel_id") hotelId: String,
                             @JsonProperty("room_ids") rooms: List[String])

case class BookingUnavailable(@JsonProperty("los") lengthOfStay: Int,
                              hotels: List[BookingHotelRooms])

case class BookingUnavailables(@JsonProperty("checkin") checkIn: String,
                               @JsonProperty("by_los") byLengthOfStays: List[BookingUnavailable])

case class BookingAvailabilities(available: List[BookingAvailability], unavailable: List[BookingUnavailables])
