package com.grigorio.smsserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = 'sms')
class SmsServiceConfig {
    String smsc
}
