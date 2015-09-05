package com.grigorio.smsserver.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = 'telnet')
class TelnetServiceConfig {
    String host, login, password
    int port
}
