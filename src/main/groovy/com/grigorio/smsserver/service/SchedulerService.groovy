package com.grigorio.smsserver.service

import com.grigorio.smsserver.config.ApplicationConfig
import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.domain.StatusReportSms
import com.grigorio.smsserver.repository.SmsRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.sql.SQLException
import java.time.LocalDateTime

@Slf4j
@Service
class SchedulerService {
    @Autowired
    MailerService mailerService

    @Autowired
    SmsService smsService

    @Autowired
    SmsRepository smsRepository

    @Autowired
    ApplicationConfig appCfg

    @Scheduled(fixedDelay = 30000l)
    void checkInboxAndModem() {
        log.trace '>> checkInboxAndSend'

        log.trace 'checking mailbox'
        List<Sms> smsList = mailerService.getSmsList()
        log.info "number of sms to send: ${smsList.size()}"

        smsList.each {
            log.trace "sending SMS: $it"
            smsService.sendSms(it)

            try {
                smsRepository.save(it)
            } catch (SQLException e) {
                log.error "exception saving data to db: ${e.message}"

                smsRepository.save(it)
            }
        }

        log.trace 'checking new sms'
        smsList = smsService.getNewSms()
        log.info "number of new sms to send in INBOX: ${smsList.size()}"

        if (smsList.size() > 0) {
            log.trace 'processing incoming sms'
            smsList.findAll { !(it instanceof StatusReportSms) }.each {
                log.trace 'pulling a history of communications'

                List<Sms> history

                try {
                    history =
                            smsRepository.findByAddressAndTsAfterOrderByTsDesc(
                                    it.address, LocalDateTime.now().minusDays(appCfg.historyDepth))
                } catch (SQLException e) {
                    log.error "exception getting history from DB: ${e.message}"

                    history =
                            smsRepository.findByAddressAndTsAfterOrderByTsDesc(
                                    it.address, LocalDateTime.now().minusDays(appCfg.historyDepth))
                }

                log.debug "sms history: $history"
                history.add(0, it.setIncoming(true))

                log.trace 'sending an email'
                mailerService.sendHistory(mailerService.cfg.forward, history)

                log.trace 'saving sms into db'
                smsRepository.save(it.setIncoming(true))
            }

            log.trace 'processing status reports'
            smsList.findAll { it instanceof StatusReportSms }.each {
                sms ->
                    log.debug "addr: ${sms.address} refNo: ${sms.refNo}"

                    List<Sms> saved

                    try {
                        saved =
                                smsRepository.findByAddressAndTsAfterOrderByTsDesc(
                                        sms.address, LocalDateTime.now().minusDays(appCfg.historyDepth))
                    } catch (SQLException e) {
                        log.error "exception pulling information from DB: ${e.message}"

                        saved =
                                smsRepository.findByAddressAndTsAfterOrderByTsDesc(
                                        sms.address, LocalDateTime.now().minusDays(appCfg.historyDepth))
                    }

                    log.debug "list of sms in repo: $saved"

                    Sms smsToUpdate = saved.get(0)
                    smsToUpdate.status = sms.status
                    smsRepository.save(smsToUpdate)
            }
        }

        log.trace '<< checkInboxAndSend'
    }

    @Scheduled(fixedDelay = 10000l)
    void resendFailedPdus() {
        log.trace '>> resendFailedPdus'

        smsService.resendPdu()

        log.trace '<< resendFailedPdus'
    }

    // every hour ping DB to avoid timeouts
    @Scheduled(fixedRate = 3600000l)
    void pingDB() {
        log.trace '>> pingDB'

        log.debug "count sms in DB: ${smsRepository.count()}"
        log.trace '<< pingDB'
    }
}

