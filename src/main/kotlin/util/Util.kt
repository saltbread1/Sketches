package util

import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

fun timestamp(format: String): String
{
    val now = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
    val formatter = DateTimeFormatter.ofPattern(format)
    return now.format(formatter)
}
