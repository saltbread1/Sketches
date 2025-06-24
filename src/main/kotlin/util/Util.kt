package util

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass

fun timestamp(format: String): String
{
    val now = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
    val formatter = DateTimeFormatter.ofPattern(format)
    return now.format(formatter)
}

fun createPalette(colors: String): IntArray
{
    return colors.split("-").map { it.toInt(16) or (0xff000000.toInt()) }.toIntArray()
}

fun saveName(clazz: KClass<*>): String
{
    return "output" + File.separator + clazz.simpleName + File.separator + timestamp("yyyyMMddHHmmss") + "-######.png"
}

fun clamp(value: Int, min: Int, max: Int): Int
{
    return if (value < min) min else if (value > max) max else value
}

fun clamp(value: Float, min: Float, max: Float): Float
{
    return if (value < min) min else if (value > max) max else value
}
