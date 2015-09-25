package com.grigorio.smsserver.service

import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.domain.StatusReportSms
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
    void checkInboxAndModem() {
        log.trace '>> checkInboxAndSend'

        log.trace 'checking mailbox'
        List<Sms> smsList = mailerService.getSmsList()
        log.debug "number of sms to send: ${smsList.size()}"

        smsList.each {
            log.trace "sending SMS: $it"
            smsService.sendSms(it)
            smsRepository.save(it)
        }

        log.trace 'checking new sms'
        smsList = smsService.getNewSms()
        log.debug "number of new sms: ${smsList.size()}"

        log.trace 'processing incoming sms'
        smsList.findAll {! it instanceof StatusReportSms}.each {
            mailerService.sendMail(mailerService.cfg.forward, it.toString())
            smsRepository.save(it)
        }

        log.trace 'processing status reports'
        smsList.findAll {it instanceof StatusReportSms}.each {
            sms ->
                log.debug "addr: ${sms.address} refNo: ${sms.refNo}"
                List<Sms> saved = smsRepository.findByAddressAndRefNoOrderByTsDesc('+' + sms.address, sms.refNo)
                log.debug "list of sms in repo: $saved"
        }

        log.trace '<< checkInboxAndSend'
    }
}

