package com.grigorio.smsserver.controller

import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.service.IntegrationService
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Slf4j
@RestController
class SmsRequestController {
    @Autowired
    IntegrationService service

    @RequestMapping("/send")
    public String sendSms(
            @RequestParam(value = 'http_username') String user,
            @RequestParam(value = 'http_password') String pwd,
            @RequestParam(value = 'phone_list') String addr,
            @RequestParam(value = 'message') String txt) {
        log.trace '>> sendSms'

        log.debug "sendSms: user=$user pwd=$pwd addr=$addr txt=$txt"

        log.trace 'sending sms in a separate thread'
        new Thread(new Runnable() {
            @Override
            void run() {
                service.sendSms(new Sms(addr.trim(), txt.trim()))
            }
        }).start()

        log.trace '<< sendSms'
        return "Message sent"
    }
}
