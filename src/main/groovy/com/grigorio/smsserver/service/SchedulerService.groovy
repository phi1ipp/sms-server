package com.grigorio.smsserver.service

import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.repository.SmsRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Slf4j
@Service
class SchedulerService {
    @Autowired
    MailerService mailerService

    @Autowired
    SmsService smsService

    @Autowired
    SmsRepository smsRepository

    @Scheduled(fixedDelay = 30000l)
    void checkInboxAndSend() {
        log.trace '>> checkInboxAndSend'

        log.trace 'checking mailbox'
        List<Sms> smsList = mailerService.getSmsList()
        log.debug "number of sms to send: ${smsList.size()}"

        smsList.each {
            log.trace "sending SMS: $it"
            smsService.sendSms(it)
            smsRepository.save(it)
        }
        log.trace '<< checkInboxAndSend'
    }
}

