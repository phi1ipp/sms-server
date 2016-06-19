package com.grigorio.smsserver.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = 'sms')
class SmsServiceConfig {
    String smsc
    int validHours
    int[] channels
    String[] numbers
}
