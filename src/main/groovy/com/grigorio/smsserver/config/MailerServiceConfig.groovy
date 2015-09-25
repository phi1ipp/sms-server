package com.grigorio.smsserver.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = 'mail')
class MailerServiceConfig {
    String server, user, password
    String domain, forward
    Integer port
}
