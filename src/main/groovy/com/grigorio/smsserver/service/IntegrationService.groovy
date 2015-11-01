package com.grigorio.smsserver.service

import com.grigorio.smsserver.domain.Pdu
import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.repository.PduRepository
import com.grigorio.smsserver.repository.SmsRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Slf4j
@Service
class IntegrationService {
    @Autowired
    SmsRepository smsRepository

    @Autowired
    PduRepository pduRepository

    @Autowired
    SmsService smsService

    void sendSms(Sms sms) {
        log.trace '>> sendSms'
        log.debug "sendSms: $sms"

        List<Pdu> lstPdu = smsService.sendSms(sms)

        log.debug "lstPdu after sending the message: $lstPdu"

        sms.parts = lstPdu.size()

        // if every PDU sent
        if (lstPdu.every { it.refNo > 0 } )
            sms.status = 's'
        else
            sms.status = 'u'

        log.trace 'saving SMS into db'
        smsRepository.save(sms)

        log.trace 'saving PDUs into db'
        lstPdu.each { pdu ->
            pdu.smsId = sms.id
            pduRepository.save(pdu)
        }

        // adding failed PDUs into the resend queue
        synchronized (smsService) {
            log.trace 'adding PDUs into the resend queue'

            log.debug "resend queue before: $smsService.resendQueue"
            smsService.resendQueue.addAll(lstPdu.findAll { it -> it.refNo == -1 })
            log.debug "resend queue after: $smsService.resendQueue"
        }

        log.trace '<< sendSms'
    }
}
