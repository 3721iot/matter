package com.dsh.tether.utils

import com.google.protobuf.Timestamp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    /**
     * Timestamp format
     */
    private const val TIMESTAMP_FORMAT_PATTERN = "MM.dd.yy HH:mm:ss"

    /**
     * Formats a com.google.protobuf.Timestamp according to the specified pattern. If _pattern is null,
     * then the default is "MM.dd.yy HH:mm:ss".
     *
     * @param timestamp timestamp
     * @param _pattern format
     * @return the formatted timestamp as String
     */
    fun formatTimestamp(
        timestamp: Timestamp,
        _pattern: String? = TIMESTAMP_FORMAT_PATTERN
    ): String {
        val pattern = _pattern ?: TIMESTAMP_FORMAT_PATTERN
        val timestampFormatter = DateTimeFormatter
            .ofPattern(pattern)
            .withZone(ZoneId.of("UTC"))
        return timestampFormatter.format(Instant.ofEpochSecond(timestamp.seconds))
    }

    /**
     * Returns a com.google.protobuf.Timestamp for the current time.
     *
     * @return the timestamp
     */
    fun getTimestamp(): Timestamp {
        val now = Instant.now()
        return Timestamp.newBuilder().setSeconds(now.epochSecond).setNanos(now.nano).build()
    }

    /**
     * Formats a com.google.protobuf.Timestamp according to the specified pattern. If _pattern is null,
     * then the default is "MM.dd.yy HH:mm:ss".
     * @return the timestamp
     */
    fun getFormattedTimestamp(): String{
        val timestamp = getTimestamp()
        return formatTimestamp(timestamp)
    }
}