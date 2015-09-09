package com.grigorio.smsserver.util

import javax.persistence.AttributeConverter
import javax.persistence.Converter
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@Converter(autoApply = true)
class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp>{
    @Override
    Timestamp convertToDatabaseColumn(LocalDateTime date) {
        Instant instant = Instant.from(date.atZone(ZoneId.systemDefault()));
        return Timestamp.from(instant);
    }

    @Override
    LocalDateTime convertToEntityAttribute(Timestamp dbData) {
        Instant instant = dbData.toInstant()
        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
    }
}
