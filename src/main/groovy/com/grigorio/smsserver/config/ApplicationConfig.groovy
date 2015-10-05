package com.grigorio.smsserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties
class ApplicationConfig {
    int historyDepth
}
