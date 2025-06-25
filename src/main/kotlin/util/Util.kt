package util

import java.io.File
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.reflect.KClass
import processing.core.PApplet.*

fun timestamp(format: String): String
{
    val now = ZonedDateTime.now(ZoneId.of("Asia/Tokyo"))
    val formatter = DateTimeFormatter.ofPattern(format)
    return now.format(formatter)
}

fun createPalette(colors: String): IntArray =
    colors.split("-").map { it.toInt(16) or (0xff000000.toInt()) }.toIntArray()

fun saveName(clazz: KClass<*>): String =
    "output" + File.separator + clazz.simpleName + File.separator + timestamp("yyyyMMddHHmmss") + "-######.png"

fun clamp(value: Int, min: Int, max: Int): Int = min(max(min, value), max)

fun clamp(value: Float, min: Float, max: Float): Float = min(max(min, value), max)

fun easeInPolynomial(x: Float, d: Float): Float = pow(x, d)

fun easeOutPolynomial(x: Float, d: Float): Float = 1.0f - pow(1.0f - x, d)

fun easeInOutPolynomial(x: Float, d: Float): Float =
    if (x < 0.5f) 0.5f * pow(2.0f * x, d) else 1.0f - 0.5f * pow(2.0f * (1.0f - x), d)
