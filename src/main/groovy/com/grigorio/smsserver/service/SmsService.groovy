package com.grigorio.smsserver.service

import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.exception.SmsException
import com.grigorio.smsserver.exception.TelnetServiceException
import groovy.util.logging.Slf4j
import org.ajwcc.pduUtils.gsm3040.PduParser
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
class SmsService {
    @Autowired
    TelnetService telnetService

    public boolean newSms(int channel) {
        return telnetService.readSMSList(channel).size() > 0
    }

    List<Sms> getNewSms(int channel) {
        log.trace '>> getNewSms'

        def smsMap = telnetService.readSMSList(channel)
        log.debug "smsMap: $smsMap"

        def res = []
        Map<Integer, String> exceptions = [:]

        smsMap.each {
            try {
                def rawPdu = telnetService.readRawSmsPdu(channel, it.key)
                try {
                    res.add(Sms.valueOf(rawPdu))

                    //todo delete successfully processed ones
                } catch (SmsException ignored) {
                    exceptions.put(it.key, rawPdu)
                }
            } catch (TelnetServiceException e) {
                log.error(e.message)
            }
        }

        log.debug "exceptions: $exceptions"

        Map<Integer, Map<Integer, String>> exGroups = exceptions.groupBy {it -> new PduParser().parsePdu(it.value).mpRefNo}
        log.debug "exGroups: $exGroups"

        def mapEx = [:]
        exGroups.each {
            try {
                res.add(Sms.valueOf(it.value.collect {map -> map.value}))
                //todo delete successfully processed ones
            } catch (SmsException ignored) {
                mapEx.put it.key, it.value
            }
        }

        log.debug "mapEx: $mapEx"
        log.trace '<< getNewSms'

        res
    }
}
