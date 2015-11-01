package com.grigorio.smsserver.service

import com.grigorio.smsserver.config.ApplicationConfig
import com.grigorio.smsserver.domain.Pdu
import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.domain.StatusReportSms
import com.grigorio.smsserver.repository.PduRepository
import com.grigorio.smsserver.repository.SmsRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

import java.sql.SQLException
import java.time.LocalDateTime

@Slf4j
@Service
class SchedulerService {
    @Autowired
    IntegrationService integrationService

    @Autowired
    MailerService mailerService

    @Autowired
    SmsService smsService

    @Autowired
    SmsRepository smsRepository

    @Autowired
    PduRepository pduRepository

    @Autowired
    ApplicationConfig appCfg

    @Autowired
    MessageSource msgSrc

    @Scheduled(fixedDelay = 30000l)
    void checkInboxAndModem() {
        log.trace '>> checkInboxAndSend'

        log.trace 'checking mailbox'
        List<Sms> smsList = mailerService.getSmsList()
        log.info "number of sms to send: ${smsList.size()}"

        smsList.each {
            log.trace "sending SMS: $it"
            integrationService.sendSms(it)
        }

        log.trace 'checking new sms'
        smsList = smsService.getNewSms()
        log.info "number of new sms to send in INBOX: ${smsList.size()}"

        if (smsList.size() > 0) {
            log.trace 'processing incoming sms'
            smsList.findAll { !(it instanceof StatusReportSms) }.each {

                List<Sms> history = []

                // check ignore list to avoid saving/pulling history
                if (it.address in appCfg.ignoreHistoryFor) {
                    log.info "address $it.address is in ignore list"
                } else {
                    log.trace 'pulling a history of communications for ' + it.address

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

                    log.trace 'saving sms into db'
                    smsRepository.save(it.setIncoming(true))
                }

                log.trace 'adding SMS to the beginning of the history for mailing'
                history.add(0, it.setIncoming(true))

                log.trace 'sending an email with history'
                mailerService.sendHistory(mailerService.cfg.forward, history)
            }

            log.trace 'processing status reports'
            smsList.findAll { it instanceof StatusReportSms }.each {
                sms ->
                    log.debug "addr: ${sms.address} refNo: ${sms.refNo} chan: $sms.channel"

                    List<Pdu> savedPdus

                    try {
                        savedPdus = pduRepository.findByRefNoAndChannel(sms.refNo, sms.channel)
                    } catch (SQLException e) {
                        log.error "exception pulling information from DB: ${e.message}"

                        savedPdus = pduRepository.findByRefNoAndChannel(sms.refNo, sms.channel)
                    }

                    log.debug "list of PDUs with refNo: $sms.refNo and chan: $sms.channel in repo: $savedPdus"

                    if (savedPdus.size() < 1) {
                        log.error "can't find pdu with ref# $sms.refNo and chan: $sms.channel in DB"
                    } else if (savedPdus.size() > 1) {
                        log.error "more than one pdu in DB with ref# $sms.refNo and chan: $sms.channel"
                    } else {
                        Pdu pdu = savedPdus.get(0)
                        pdu.status = 'd'

                        log.trace 'changing status of pdu to delivered in DB'
                        pduRepository.save(pdu)

                        long smsId = pdu.smsId
                        log.debug "smsId: $smsId"

                        List<Pdu> allPduForSMS = pduRepository.findBySmsId(smsId)

                        log.trace 'checking if all PDUs for the sms are delivered'
                        if (allPduForSMS.every { it.status == 'd' as char}) {
                            log.info 'all PDUs are now delivered'

                            log.trace 'deleting PDUs from DB'
                            allPduForSMS.each {
                                pduRepository.delete(it)
                            }

                            Sms theSMS = smsRepository.findOne(smsId)

                            log.trace 'setting SMS status to delivered in DB'
                            theSMS.status = 'd'
                            smsRepository.save(theSMS)
                        }
                    }
            }
        }

        log.trace '<< checkInboxAndSend'
    }

    @Scheduled(fixedDelay = 10000l)
    void resendFailedPdus() {
        log.trace '>> resendFailedPdus'

        List<Pdu> lstPdu = smsService.resendPdu()

        log.debug "lstPdu after resending: $lstPdu"

        lstPdu.findAll { it.refNo > -1 }
                .each {
                    pdu ->
                        List<Pdu> savedPdu = pduRepository.findByPdu(pdu.pdu)
                        log.debug "list of PDUs in DB with the same pdu: $savedPdu"

                        if (savedPdu.size() < 1) {
                            log.error 'pdu not found in DB'
                        } else if (savedPdu.size() > 1) {
                            log.error 'more than one pdu found in DB'
                        } else {
                            Pdu foundPdu = savedPdu.get(0)
                            foundPdu.refNo = pdu.refNo

                            log.trace 'changing PDU ref# in DB'
                            pduRepository.save(foundPdu)
                        }
        }

        // add into the resend queue all failed PDUs
        synchronized (smsService) {
            smsService.resendQueue.addAll(lstPdu.findAll { it.refNo < 0 })
        }

        log.trace '<< resendFailedPdus'
    }

    @Scheduled(fixedDelay = 60000l)
    void processExpiredMessages() {
        log.trace '>> processExpiredMessages'

        log.trace 'searching for expired PDUs'
        log.debug "expiration time: $smsService.cfg.validHours"
        List<Pdu> expired = pduRepository.findExpired(smsService.cfg.validHours)
        log.debug "expired: $expired"

        if (expired.size() > 0) {
            log.trace 'setting sms status to NON-DELIVERED'

            log.trace 'loading email template'
            String strText = msgSrc.getMessage('mailer.nondelivered.text', null, Locale.default)
            log.debug "email template: $strText"

            expired
                    .groupBy { pdu -> pdu.smsId }
                    .each {
                        Sms sms = smsRepository.findOne(it.key)
                        log.debug "sms: $sms"

                        if (sms != null) {
                            sms.status = 'n'
                            smsRepository.save(sms)
                        }
                        else
                            log.warning "sms with id: $sms.id not found in DB"

                        log.info "sms marked as NON-DELIVERED: $sms "

                        log.trace 'preparing to send an email'

                        String strMsg = String.format(strText, sms.address, sms.ts)
                        log.debug "message to send: $strMsg"

                        log.trace 'sending the email'
                        mailerService.sendMail(
                                mailerService.cfg.forward,
                                msgSrc.getMessage('mailer.nondelivered.subj', null, Locale.default),
                                strMsg)
                    }

            log.trace 'deleting expired PDUs'
            expired.each {
                pdu ->
                    pduRepository.delete(pdu.id)
                    log.info "pdu deleted from DB: $pdu"
            }
        }

        log.trace '<< processExpiredMessages'
    }

    // every hour ping DB to avoid timeouts
    @Scheduled(fixedRate = 3600000l)
    void pingDB() {
        log.trace '>> pingDB'

        log.debug "count sms in DB: ${smsRepository.count()}"
        log.trace '<< pingDB'
    }
}

