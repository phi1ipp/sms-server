package com.grigorio.smsserver.util

import org.springframework.format.Formatter

import java.text.ParseException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class LocalDateTimeFormatter implements Formatter<LocalDateTime> {
    @Override
    LocalDateTime parse(String text, Locale locale) throws ParseException {
        return LocalDateTime.parse(text)
    }

    @Override
    String print(LocalDateTime object, Locale locale) {
        return object.format(DateTimeFormatter.ofPattern('yyyy-MM-dd\nHH:mm:ss', locale))
    }
}
