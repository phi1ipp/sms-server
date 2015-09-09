package com.grigorio.smsserver.service

import com.grigorio.smsserver.config.MailerServiceConfig
import com.grigorio.smsserver.domain.Sms
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service

import javax.mail.Authenticator
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Store
import javax.mail.internet.InternetAddress

@Slf4j
@Service
@EnableConfigurationProperties(MailerServiceConfig.class)
class MailerService {
    @Autowired
    MailerServiceConfig cfg

    Session session
    Store store
    Folder inbox

    void connect() {
        log.trace '>> connect'

        Properties props = new Properties()
        props.setProperty('mail.pop3.host', cfg.server)
        props.setProperty('mail.pop3.port', cfg.port as String)

        log.trace 'getting session'
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.user, cfg.password)
            }
        })

        log.trace 'getting store'
        store = session.getStore('pop3')

        log.trace '<< connect'
    }

    int checkInbox() {
        log.trace '>> checkInbox'

        try {
            log.trace 'connecting to store'
            store.connect()

            log.trace 'openning INBOX'
            inbox = store.getFolder('INBOX')
            inbox.open(Folder.READ_ONLY)

            log.trace '<< checkInbox'
            return inbox.getMessageCount()
        } finally {
            inbox.close(false)
            store.close()
        }
    }

    List<String> getHeaders() {
        log.trace '>> getHeaders'

        try {
            log.trace 'connecting to store'
            store.connect()

            log.trace 'openning INBOX'
            inbox = store.getFolder('INBOX')
            inbox.open(Folder.READ_ONLY)

            log.trace '<< getting messages'
            Message[] messages = inbox.getMessages()

            log.trace '<< getHeaders'
            messages.collect {new StringBuilder(it.subject).append(',').append(it.from[0]).toString()}
        } finally {
            inbox.close(false)
            store.close()
        }
    }

    List<Sms> getSmsList() {
        log.trace '>> getSmsMessages'

        List<Sms> res = new ArrayList<>()

        try {
            log.trace 'connecting to store'
            store.connect()

            log.trace 'openning INBOX'
            inbox = store.getFolder('INBOX')
            inbox.open(Folder.READ_WRITE)

            log.trace '<< getting messages'
            Message[] messages = inbox.getMessages()

            log.trace '<< getHeaders'
            messages.each {
                String from = ''
                if (it.from[0] instanceof InternetAddress) {
                    InternetAddress ia = it.from[0] as InternetAddress
                    from = ia.getAddress()
                }

                if (from.endsWith(cfg.domain)) {
                   if (it.isMimeType('text/plain')) {
                       try {
                           String content = it.getContent()

                           Map<String, String> fields = [:]

                           log.trace 'parsing email content'
                           content.split('\n').each {
                               line ->
                                   String[] tokens = line.split('=')
                                   String k = tokens[0]
                                   String v = tokens[1]
                                   fields.put(k, v)
                           }
                           log.debug "fields: $fields"

                           log.trace 'trying to add sms'
                           res.add(new Sms(fields['tels'].trim(), fields['mess'].trim()))
                       } catch (Exception e) {
                           log.error "Exception processing email from $from: ${e.stackTrace}"
                       }
                   } else {
                       log.warn "Message from $from is not a text message, skipping..."
                   }
                } else {
                    log.warn "Message from $from is not in the allowed domain, skipping..."
                }

                it.setFlag(Flags.Flag.DELETED, true)
            }

            log.trace '<< getSmsList'
            res
        } finally {
            inbox.close(true)
            store.close()
        }
    }
}
