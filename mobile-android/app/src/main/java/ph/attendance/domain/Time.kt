package ph.attendance.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Everything here works in the device's local calendar on purpose.
 *
 * A school day is a local concept: converting to UTC would push an evening record into the next
 * day for anyone east of Greenwich and silently corrupt every report built on it. Timestamps sent
 * over the wire are instants in UTC; dates are not.
 */
object Clocks {
    fun nowIso(instant: Instant = Instant.now()): String =
        DateTimeFormatter.ISO_INSTANT.format(instant.truncatedToMillis())

    private fun Instant.truncatedToMillis(): Instant =
        Instant.ofEpochMilli(toEpochMilli())

    fun today(zone: ZoneId = ZoneId.systemDefault()): String = LocalDate.now(zone).toString()

    fun minutesOfDay(zone: ZoneId = ZoneId.systemDefault()): Int =
        LocalTime.now(zone).let { it.hour * 60 + it.minute }

    fun minutesOf(dateTime: LocalDateTime): Int = dateTime.hour * 60 + dateTime.minute

    fun isoDate(dateTime: LocalDateTime): String = dateTime.toLocalDate().toString()

    /** An ISO instant for a local wall-clock moment. Used by the seed and by tests. */
    fun isoInstant(dateTime: LocalDateTime, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ISO_INSTANT.format(dateTime.atZone(zone).toInstant())
}

fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    return parts[0].toInt() * 60 + parts[1].toInt()
}

fun minutesToTime(minutes: Int): String {
    val wrapped = ((minutes % 1440) + 1440) % 1440
    return "%02d:%02d".format(wrapped / 60, wrapped % 60)
}

/** `14:05` renders as `2:05 PM` — the format the original app and its users expect. */
fun formatTime12(time: String): String {
    val (hours, minutes) = time.split(":").map(String::toInt)
    val period = if (hours < 12) "AM" else "PM"
    val hour12 = if (hours % 12 == 0) 12 else hours % 12
    return "%d:%02d %s".format(hour12, minutes, period)
}

fun isValidTime24(value: String): Boolean =
    Regex("^([01]\\d|2[0-3]):[0-5]\\d$").matches(value)

fun isValidIsoDate(value: String): Boolean =
    try {
        LocalDate.parse(value)
        Regex("^\\d{4}-\\d{2}-\\d{2}$").matches(value)
    } catch (_: DateTimeParseException) {
        false
    }

fun addDays(date: String, days: Long): String = LocalDate.parse(date).plusDays(days).toString()

/** Inclusive on both ends; empty when the range is inverted. */
fun datesBetween(start: String, end: String): List<String> {
    if (start > end) return emptyList()
    val from = LocalDate.parse(start)
    val to = LocalDate.parse(end)
    return generateSequence(from) { current -> current.plusDays(1).takeIf { !it.isAfter(to) } }
        .map(LocalDate::toString)
        .toList()
}

fun monthRange(date: String): Pair<String, String> {
    val day = LocalDate.parse(date)
    return day.withDayOfMonth(1).toString() to day.withDayOfMonth(day.lengthOfMonth()).toString()
}

fun formatDateLong(date: String): String =
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy"))

fun formatDateShort(date: String): String =
    LocalDate.parse(date).format(DateTimeFormatter.ofPattern("d MMM"))

/**
 * Parses the legacy Android format `h:mm:AM` into `HH:mm`. Used only when importing data from
 * the 2019 app; returns null on anything it does not recognise.
 */
fun parseLegacyTime(value: String): String? {
    val match = Regex("^(\\d{1,2}):([0-5]\\d):(AM|PM)$", RegexOption.IGNORE_CASE)
        .matchEntire(value.trim()) ?: return null

    val hour = match.groupValues[1].toInt()
    if (hour !in 1..12) return null

    val minutes = match.groupValues[2]
    val isAm = match.groupValues[3].equals("AM", ignoreCase = true)
    val hours24 = if (isAm) hour % 12 else (hour % 12) + 12
    return "%02d:%s".format(hours24, minutes)
}
