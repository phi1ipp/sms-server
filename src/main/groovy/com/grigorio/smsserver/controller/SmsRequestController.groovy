package com.grigorio.smsserver.controller

import com.grigorio.smsserver.domain.Pdu
import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.repository.PduRepository
import com.grigorio.smsserver.repository.SmsRepository
import com.grigorio.smsserver.service.SmsService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
class SmsRequestController {
    @Autowired
    SmsService service

    @Autowired
    SmsRepository smsRepo

    @Autowired
    PduRepository pduRepo

    @RequestMapping("/send")
    public String sendSms(
            @RequestParam(value = 'http_username') String user,
            @RequestParam(value = 'http_password') String pwd,
            @RequestParam(value = 'phone_list') String addr,
            @RequestParam(value = 'message') String txt) {
        log.trace '>> sendSms'

        log.debug "sendSms: user=$user pwd=$pwd addr=$addr txt=$txt"

        log.trace 'sending sms'
        Sms sms = new Sms(addr, txt)
        Map<String, Integer> mapPdu = service.sendSms(sms)

        sms.parts = mapPdu.size()

        log.trace 'saving SMS into db'
        smsRepo.save(sms)

        log.trace 'saving sent PDUs into db'
        mapPdu.each {
           pduRepo.save(new Pdu(it.key, it.value, sms.id))
        }

        log.trace '<< sendSms'
        return "inside sendSms with user=$user pwd=$pwd addr=$addr txt=$txt"
    }
}
