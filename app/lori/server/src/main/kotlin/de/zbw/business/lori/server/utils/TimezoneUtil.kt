package de.zbw.business.lori.server.utils

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

object TimezoneUtil {
    val TIME_ZONE_BERLIN: ZoneId = ZoneId.of("Europe/Berlin")
    val TIME_ZONE_UTC: ZoneId = ZoneId.of("UTC")

    fun utcOffsetDateTimeToBerlinDate(utcOdt: OffsetDateTime): LocalDate =
        utcOdt
            .atZoneSameInstant(ZoneOffset.UTC)
            .withZoneSameInstant(TIME_ZONE_BERLIN)
            .toLocalDate()
}
