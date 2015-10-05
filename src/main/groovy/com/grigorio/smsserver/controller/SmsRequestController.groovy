package com.grigorio.smsserver.controller

import com.grigorio.smsserver.domain.Sms
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

    @RequestMapping("/send")
    public String sendSms(
            @RequestParam(value = 'http_username') String user,
            @RequestParam(value = 'http_password') String pwd,
            @RequestParam(value = 'phone_list') String addr,
            @RequestParam(value = 'message') String txt) {
        log.trace '>> sendSms'

        log.debug "sendSms: user=$user pwd=$pwd addr=$addr txt=$txt"

        log.trace 'sending sms'
        service.sendSms(addr, txt)

        log.trace 'saving into db'
        smsRepo.save(new Sms(addr, txt))

        log.trace '<< sendSms'
        return "inside sendSms with user=$user pwd=$pwd addr=$addr txt=$txt"
    }
}
