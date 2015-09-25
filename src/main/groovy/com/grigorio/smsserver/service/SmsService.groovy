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
    static final private String NL = '\r\n', OK = 'OK'
    @Autowired
    TelnetService telnetService

    @Autowired
    SmsServiceConfig cfg

    public void sendSms(Sms sms) {
        log.trace '>> sendSms'

        int channel = selectChannel()
        log.debug "channel to send: ${channel}"

        List<String> pdus = sms.toRawPdu(cfg.smsc)
        log.debug "pdus to send: $pdus"

        try {
            telnetService.connect()
            telnetService.privMode(true)

            pdus.each {
                String cmd = "AT^SM=$channel,$it"
                telnetService.write cmd
                log.debug telnetService.readUntil(cmd + NL)

                def res = telnetService.readUntil NL
                log.debug res

                if (res.contains('*smserr'))
                    throw new TelnetServiceException(sendFailed)

                log.trace '<< sendSms'
            }
        } catch (Exception e) {
            log.error "Exception sending sms: ${e.stackTrace}"
            throw e
        } finally {
            telnetService.disconnect()
        }
    }

    void sendSms(String addr, String txt) {
        sendSms(new Sms(addr, txt))
    }

    public List<Sms> getNewSms() {
        log.trace '>> getNewSms'
        log.debug 'getNewSms w/o params'

        List<Sms> res = []
        telnetService.connect()
        telnetService.privMode(true)

        cfg.channels.each {
            def tmp = getNewSmsFromChannel(it)

            log.debug "list from channel $it: $tmp"

            res.addAll(tmp)
            log.debug "res: $res"
        }

        telnetService.privMode(false)
        telnetService.disconnect()

        log.trace '<< getNewSms'
        res
    }

    private List<Sms> getNewSmsFromChannel(int channel) {
        log.trace '>> getNewSmsFromChannel'
        log.debug "getNewSmsFromChannel with $channel"

        log.trace 'reading sms list'
        def smsMap = readSMSList(channel)
        log.debug "smsMap: $smsMap"

        def res = []
        Map<Integer, String> exceptions = [:]

        log.trace 'reading PDUs'
        smsMap.each {
            try {
                def rawPdu = readRawSmsPdu(channel, it.key)
                try {
                    res.add(Sms.valueOf(rawPdu))

                    deleteSms(channel, it.key)
                } catch (SmsException ignored) {
                    exceptions.put(it.key, rawPdu)
                }
            } catch (TelnetServiceException e) {
                log.error(e.message)
            }
        }

        log.debug "res: $res"
        log.debug "exceptions: $exceptions"

        Map<Integer, Map<Integer, String>> exGroups = exceptions.groupBy { it -> new PduParser().parsePdu(it.value).mpRefNo}
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
        log.debug "res: $res"
        log.trace '<< getNewSmsFromChannel'

        res
    }

    public void deleteSms(int channel, int idx) {
        log.trace '>> deleteSms'

        if (channel in cfg.channels) {
            telnetService.write "AT^SD=$channel,$idx"

            log.debug telnetService.readUntil(NL)
            def read = telnetService.readUntil NL

            if (read.contains('*smserr'))
                throw new TelnetServiceException(delFailed)

            log.debug read
            log.info 'SMS deleted'

        } else
            throw new TelnetServiceException(invChannel)

        log.trace '<< deleteSms'
    }

    private Map<Integer, Integer> readSMSList(int channel) {
        log.trace '>> readSMSList'

        String strRawList

        def cmd = "AT^SX=${channel}"
        telnetService.write cmd
        telnetService.readUntil cmd + NL

        strRawList = telnetService.readUntil("*smsinc: $channel,0,0,255$NL")
        log.debug "raw list of SMS on channel ${channel}: ${strRawList}"

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

    private String readRawSmsPdu(int channel, int idx) {
        log.trace '>> readRawSmsPdu'

        if (channel < 0 || channel > 1)
            throw new TelnetServiceException(invChannel)

        String smspdu
        def cmd = "AT^SR=$channel.$idx"
        telnetService.write cmd
        log.debug telnetService.readUntil(cmd + NL)

        smspdu = telnetService.readUntil(NL)
        log.debug "smspdu: $smspdu"

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
