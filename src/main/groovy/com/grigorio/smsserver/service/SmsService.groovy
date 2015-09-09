package com.grigorio.smsserver.service

import com.grigorio.smsserver.config.SmsServiceConfig
import com.grigorio.smsserver.exception.SmsServiceException
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service

import javax.annotation.PostConstruct

import static com.grigorio.smsserver.exception.TelnetServiceException.Reason.*
import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.exception.SmsException
import com.grigorio.smsserver.exception.TelnetServiceException
import groovy.util.logging.Slf4j
import org.ajwcc.pduUtils.gsm3040.PduParser
import org.springframework.beans.factory.annotation.Autowired

@Slf4j
@Service
@EnableConfigurationProperties(SmsServiceConfig.class)
class SmsService {
    static def NL = '\r\n', OK = 'OK' + NL
    @Autowired
    TelnetService telnetService

    @Autowired
    SmsServiceConfig cfg

    @PostConstruct
    void connect() {
        if (telnetService == null) {
            throw new SmsServiceException(SmsServiceException.Reason.nullTelnetSvc)
        }

        telnetService.connect()
    }

    public boolean newSms(int channel) {
        return readSMSList(channel).size() > 0
    }

    void sendSms(String addr, String txt) {
        int channel = selectChannel()

        List<String> pdus = new Sms(addr, txt).toRawPdu(cfg.smsc)

        try {
            telnetService.write 'at!g=a6'
            log.debug telnetService.readUntil(OK)

            pdus.each {
                telnetService.write "AT^SM=$channel,$it"
                log.debug telnetService.readUntil(NL)

                def res = telnetService.readUntil NL
                log.debug res

                if (res.contains('*smserr'))
                    throw new TelnetServiceException(sendFailed)
            }
        } finally {
            telnetService.write 'at!g=55'
            log.debug telnetService.readUntil(OK)
        }
    }

    List<Sms> getNewSms(int channel) {
        log.trace '>> getNewSms'

        def smsMap = readSMSList(channel)
        log.debug "smsMap: $smsMap"

        def res = []
        Map<Integer, String> exceptions = [:]

        smsMap.each {
            try {
                def rawPdu = readRawSmsPdu(channel, it.key)
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

    public void deleteSms(int channel, int idx) {
        log.trace '>> deleteSms'

        if (channel < 0 || channel > 1)
            throw new TelnetServiceException(invChannel)

        try {
            telnetService.write 'at!g=a6'
            log.debug telnetService.readUntil(OK)

            telnetService.write "AT^SD=$channel,$idx"

            log.debug telnetService.readUntil(NL)
            def read = telnetService.readUntil NL

            if (read.contains('*smserr'))
                throw new TelnetServiceException(delFailed)
        } finally {
            telnetService.write 'at!g=55'
            log.debug telnetService.readUntil(OK)
        }

        log.debug read

        log.trace '<< deleteSms'
    }

    public Map<Integer, Integer> readSMSList(int channel) {
        log.trace '>> readSMSList'

        telnetService.write 'at!g=a6'
        log.debug telnetService.readUntil(OK)

        String strRawList = ''
        try {
            telnetService.write "AT^SX=${channel}"
            log.debug telnetService.readUntil(NL)

            strRawList = telnetService.readUntil("*smsinc: $channel,0,0,255$NL")
            log.debug "raw list of SMS on channel ${channel}: ${strRawList}"
        } finally {
            telnetService.write 'at!g=55'
            log.debug telnetService.readUntil(OK)
        }

        def res = strRawList.split(NL).collectEntries {
            Integer[] parts = it.substring(it.indexOf(' ') + 1).split(',').collect {Integer.valueOf(it)}
            def m = [:]
            m.put parts[1], parts[2]
            m
        }

        res.remove(0)

        log.trace '<< readSMSList'
        res
    }

    public String readRawSmsPdu(int channel, int idx) {
        log.trace '>> readRawSmsPdu'

        if (channel < 0 || channel > 1)
            throw new TelnetServiceException(invChannel)

        String smspdu = ''
        try {
            telnetService.write 'at!g=a6'
            log.debug telnetService.readUntil(OK)

            telnetService.write "AT^SR=$channel.$idx"
            log.debug telnetService.readUntil(NL)

            smspdu = telnetService.readUntil(NL)
            log.debug "smspdu: $smspdu"
        } finally {
            telnetService.write 'at!g=55'
            log.debug telnetService.readUntil(OK)
        }

        if (smspdu.contains('*smserr'))
            throw new TelnetServiceException(readFailed)

        String[] parts = smspdu.substring(smspdu.indexOf(' ') + 1).split(',')

        log.trace '<< readRawSmsPdu'

        parts[4]
    }

    private int selectChannel() {
        return new Random().nextInt(2)
    }
}
