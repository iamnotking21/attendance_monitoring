package ph.attendance.domain

/**
 * Validation at the boundary, mirroring the Zod schemas the web app applies.
 *
 * The student number is the important one: it doubles as the QR payload, and anyone can print a
 * QR code and hold it to the camera. Nothing reaches storage until it has passed this.
 */

sealed interface Validated<out T> {
    data class Valid<T>(val value: T) : Validated<T>
    data class Invalid(val message: String) : Validated<Nothing>
}

fun <T> Validated<T>.valueOrNull(): T? = (this as? Validated.Valid)?.value

private val STUDENT_NUMBER = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*$")
private const val STUDENT_NUMBER_MAX = 32

/**
 * Letters, digits, dots, hyphens, and underscores only. Deliberately narrow: it is the primary
 * defence against a hostile QR code, because anything that could be read as markup, a URL, or a
 * spreadsheet formula never becomes a valid lookup key.
 */
fun validateStudentNumber(raw: String): Validated<String> {
    val value = raw.trim()
    return when {
        value.isEmpty() -> Validated.Invalid("A student number is required.")
        value.length > STUDENT_NUMBER_MAX ->
            Validated.Invalid("A student number must be $STUDENT_NUMBER_MAX characters or fewer.")
        !STUDENT_NUMBER.matches(value) -> Validated.Invalid(
            "A student number may contain only letters, digits, dots, hyphens, and underscores.",
        )
        else -> Validated.Valid(value)
    }
}

/** Trims, strips control and format characters, and bounds the length. */
fun validateText(raw: String, label: String, max: Int, required: Boolean = true): Validated<String> {
    val value = raw.filterNot { it.isISOControl() || it.category == CharCategory.FORMAT }.trim()
    return when {
        required && value.isEmpty() -> Validated.Invalid("$label is required.")
        value.length > max -> Validated.Invalid("$label must be $max characters or fewer.")
        else -> Validated.Valid(value)
    }
}

data class ScheduleDraft(
    val title: String,
    val venue: String,
    val present: TimeWindow,
    val late: TimeWindow,
)

/**
 * A window must move forwards, and the late window may not open before the present one has shut.
 * Overlapping windows would make the recorded status depend on evaluation order rather than on
 * when the student actually arrived.
 */
fun validateSchedule(draft: ScheduleDraft): Validated<ScheduleDraft> {
    val title = validateText(draft.title, "Title", 80)
    if (title is Validated.Invalid) return title

    val venue = validateText(draft.venue, "Venue", 80, required = false)
    if (venue is Validated.Invalid) return venue

    val times = listOf(draft.present.start, draft.present.end, draft.late.start, draft.late.end)
    if (times.any { !isValidTime24(it) }) {
        return Validated.Invalid("Times must be in 24-hour HH:mm format.")
    }
    if (draft.present.start >= draft.present.end) {
        return Validated.Invalid("The present window must end after it starts.")
    }
    if (draft.late.start >= draft.late.end) {
        return Validated.Invalid("The late window must end after it starts.")
    }
    if (draft.late.start < draft.present.end) {
        return Validated.Invalid(
            "The late window must start when the present window ends, or after it.",
        )
    }

    return Validated.Valid(
        draft.copy(
            title = (title as Validated.Valid).value,
            venue = (venue as Validated.Valid).value,
        ),
    )
}

/**
 * Excel and Sheets treat a cell beginning with `=`, `+`, `-`, `@`, or a leading tab as a formula.
 * A student named `=cmd|'/c calc'!A1` would otherwise become executable content the moment a
 * coordinator opened the export.
 */
fun escapeSpreadsheetCell(value: String): String =
    if (value.isNotEmpty() && value.first() in charArrayOf('=', '+', '-', '@', '\t', '\r')) {
        "'$value"
    } else {
        value
    }

/** RFC 4180 quoting, applied on top of formula neutralisation. */
fun toCsvCell(value: String?): String {
    if (value == null) return ""
    val safe = escapeSpreadsheetCell(value)
    return if (safe.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + safe.replace("\"", "\"\"") + "\""
    } else {
        safe
    }
}

fun toCsv(rows: List<List<String?>>): String =
    rows.joinToString("\r\n") { row -> row.joinToString(",") { toCsvCell(it) } }

/** Strips path separators and reserved characters so a name can safely become a filename. */
fun toSafeFilename(value: String, fallback: String = "export"): String {
    val cleaned = value
        .filterNot { it.isISOControl() }
        .replace(Regex("[/\\\\?%*:|\"<>.]"), "-")
        .replace(Regex("\\s+"), "-")
        .replace(Regex("-+"), "-")
        .trim('-')
        .take(80)
    return cleaned.ifEmpty { fallback }
}
