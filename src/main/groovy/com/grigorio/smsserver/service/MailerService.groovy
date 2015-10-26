package com.grigorio.smsserver.service

import com.grigorio.smsserver.config.MailerServiceConfig
import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.exception.MailerServiceException
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct
import javax.annotation.PreDestroy
import javax.mail.Authenticator
import javax.mail.Flags
import javax.mail.Folder
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Store
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage
import java.util.regex.Matcher
import java.util.regex.Pattern

import static com.grigorio.smsserver.exception.MailerServiceException.Reason.*

@Slf4j
@Service
@EnableConfigurationProperties(MailerServiceConfig.class)
class MailerService {

    @Autowired
    MailerServiceConfig cfg

    Session session
    Store store
    Folder inbox

    @PostConstruct
    void connect() {
        log.trace '>> connect'

        if (cfg == null)
            throw new MailerServiceException(noConfig)

        Properties props = new Properties()
        props.setProperty('mail.imap.host', cfg.server)
        props.setProperty('mail.smtp.host', cfg.server)
        props.setProperty('mail.imap.port', cfg.port as String)

        log.trace 'getting session'
        session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(cfg.user, cfg.password)
            }
        } as Authenticator)

        log.trace 'getting store'
        store = session.getStore('imap')

        log.trace 'connecting to store'
        store.connect()

        log.trace '<< connect'
    }

    @PreDestroy
    void disconnect() {
        store.close()
    }

    int checkInbox() {
        log.trace '>> checkInbox'

        try {
            log.trace 'getting INBOX folder'
            inbox = store.getFolder('INBOX')

            log.trace 'openning INBOX'
            inbox.open(Folder.READ_ONLY)

            log.trace '<< checkInbox'
            return inbox.getMessageCount()
        } finally {
            inbox.close(false)
        }
    }

    List<String> getHeaders() {
        log.trace '>> getHeaders'

        try {
            log.trace 'getting INBOX folder'
            inbox = store.getFolder('INBOX')

            log.trace 'openning INBOX'
            inbox.open(Folder.READ_ONLY)

            log.trace '<< getting messages'
            Message[] messages = inbox.getMessages()

            log.trace '<< getHeaders'
            messages.collect {new StringBuilder(it.subject).append(',').append(it.from[0]).toString()}
        } finally {
            inbox.close(false)
        }
    }

    List<Sms> getSmsList() {
        log.trace '>> getSmsList'

        List<Sms> res = new ArrayList<>()

        try {
            log.trace 'getting INBOX folder'
            inbox = store.getFolder('INBOX')

            log.trace 'openning INBOX'
            inbox.open(Folder.READ_WRITE)

            log.trace 'getting messages'
            Message[] messages = inbox.getMessages()
            log.info "${messages.size()} messages in inbox"

            log.trace 'processing messages'
            messages.each {
                String from = ''
                if (it.from[0] instanceof InternetAddress) {
                    InternetAddress ia = it.from[0] as InternetAddress
                    from = ia.getAddress()
                }

                if (from.endsWith(cfg.domain)) {
                    if (it.isMimeType('text/plain')) {

                        log.debug "content type: ${it.getContentType()}"

                        try {
                            String content = it.getContent()

                            log.debug "content: $content"


                            log.trace 'parsing email content'

                            Map<String, String> fields = [:]
                            Pattern pattern = Pattern.compile('mess=(.*)$', Pattern.DOTALL)
                            Matcher matcher = pattern.matcher(content)

                            if (!matcher.find()) {
                                log.error "No mess found in the email"
                                throw new MailerServiceException('No mess found in the email')
                            }

                            String mess = matcher.group(1)

                            pattern = Pattern.compile 'tels=(.*)'
                            matcher = pattern.matcher(content)

                            if (!matcher.find()) {
                                log.error "No tels found in the email"
                                throw new MailerServiceException('No tels found in the email')
                            }

                            String tels = matcher.group(1)

                            log.debug "tels=$tels mess=$mess"
                            log.trace 'trying to add sms'
                            res.add(new Sms(tels, mess))
                        } catch (Exception e) {
                            log.error "Exception ${e.cause} processing email from $from: ${e.stackTrace}"
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
        } catch (Exception e) {
            log.error "Exception checking inbox", e

            log.trace '<< getSmsList'
            res
        } finally {
            inbox.close(true)
        }
    }

    void sendMail(String to, String text) {
        sendMail(to, 'New SMS', text)
    }

    void sendMail(String to, String subj, String text) {
        log.trace '>> sendMail'
        log.debug "sendMail with $to - $subj"

        try {
            MimeMessage msg = new MimeMessage(session)

            msg.setFrom(new InternetAddress('sms@max-avia.ru'))
            msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to))

            msg.setSubject(subj)

            msg.setContent(text.replaceAll('\n', '<br>'), 'text/html; charset=utf-8')

            Transport.send(msg)

            log.info "Email to $to sent"
        } catch (Exception e) {
            log.error("Exception sending mail", e)
            throw e
        }

        log.trace '<< sendMail'
    }

    void sendHistory(String to, List<Sms> history) {
        log.trace '>> sendHistory'
        StringBuilder sb = new StringBuilder('<html><body>')

        log.trace 'searching history for an agent last talking'
        Sms smsForAgent = history.find {!it.incoming}

        String agent = ''
        if (smsForAgent != null && smsForAgent.txt.contains('//'))
            agent = smsForAgent.txt.substring(smsForAgent.txt.lastIndexOf('//') + 2)

        log.trace 'performing history formatting'
        history.each {
            if (it.incoming) {
                sb.append('<hr><font color="blue"><b>Получено</b> ').append(it.ts).append('<br>').append(it.txt)
            } else {
                sb.append('<hr><font color="red"><b>Отправлено</b> ').append(it.ts).append('<br>').append(it.txt)
            }
        }

        sb.append('</body></hmtl>')

        log.trace 'sending email with the history'
        sendMail(to, "SMS переписка с абонентом ${history.get(0).address} - $agent", sb.toString())

        log.trace '<< sendHistory'
    }
}
