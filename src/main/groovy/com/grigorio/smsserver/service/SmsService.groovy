package com.grigorio.smsserver.service

import com.grigorio.smsserver.config.SmsServiceConfig
import com.grigorio.smsserver.exception.SmsServiceException
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service

import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

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
    static final private String NL = '\r\n'
    @Autowired
    TelnetService telnetService

    @Autowired
    SmsServiceConfig cfg

    Random rnd = new Random()
    Lock lock = new ReentrantLock()
    ConcurrentLinkedQueue<Map<String, Integer>> resendQueue = new ConcurrentLinkedQueue<>()

    Map<String, Integer> sendSms(Sms sms) {
        log.trace '>> sendSms'

        int channel = selectChannel()
        log.debug "channel to send: ${channel}"

        List<String> pdus = sms.toRawPdu(cfg.smsc)
        log.debug "pdus to send: $pdus"

        Map<String, Integer> mapPdu = new HashMap<>()

        synchronized (this) {
            log.trace 'locking access to telnet service'
            lock.lock()

            try {
                telnetService.connect()
                telnetService.privMode(true)

                pdus.each {
                    def key = it.split(',')[1]
                    mapPdu[key] = '-1'
                    mapPdu[key] = sendPdu(it, channel)
                }
                log.trace '<< sendSms'
            } catch (Exception e) {
                log.error "Exception sending sms: ${e.stackTrace}"
            } finally {
                telnetService.disconnect()

                log.trace 'unlocking access to telnet service'
                lock.unlock()
            }
        }

        return mapPdu
    }

    Map<String, Integer> sendSms(String addr, String txt) {
        return sendSms(new Sms(addr, txt))
    }

    int sendPdu(String pdu, int channel) {
        log.trace '>> sendPdu'
        log.debug "sendPdu with $pdu on channel $channel"

        int refNo = -1

        if (!(channel in cfg.channels)) {
            log.error "$channel not if configured channels"
            throw new TelnetServiceException(invChannel)
        }

        if (!telnetService.connected || !telnetService.priveleged)
            throw new SmsServiceException(SmsServiceException.Reason.telnetNoPriv)

        try {
            String cmd = "AT^SM=$channel,$pdu"
            telnetService.write cmd

            // read until *smserr, *smsout or exception
            String res
            boolean bRun = true
            while (bRun) {
                res = telnetService.readUntil(NL, TelnetService.lineLength)
                log.debug res

                if (res.contains('*smserr')) {
                    log.warn "modem failed to send pdu, adding to retry list"
                    resendQueue.add(Collections.singletonMap(pdu, channel))
                    bRun = false

                } else if (res.contains('*smsout')) {
                    String strRefNo = res.split(':')[1].split(',')[1]
                    refNo = Integer.valueOf(strRefNo)

                    log.info "pdu has been sent with ref #$refNo"
                    bRun = false
                }
            }
        } catch (Exception e) {
            log.error "Exception ${e.message} sending pdu"

            log.trace 'adding pdu to resend queue'
            resendQueue.add(Collections.singletonMap(pdu, channel))
        }

        log.trace '<< sendPdu'
        return refNo
    }

    Map<String, Integer> resendPdu() {
        log.trace '>> resendPdu'

        Map<String, Integer> resendMap = new HashMap<>()

        if (resendQueue.size() > 0) {
            log.trace 'moving elements from resend queue into a local copy'
            Queue<Map<String, Integer>> queue = new LinkedList<>()
            queue.addAll(resendQueue)
            resendQueue.clear()

            log.trace 'locking access to telnet service'
            lock.lock()
            try {
                telnetService.connect()
                telnetService.privMode(true)

                log.info 'resending PDUs'
                queue.each {
                    map -> map.each {
                        String pdu, Integer channel ->
                            try {
                                resendMap << [pdu: sendPdu(pdu, channel)]
                            } catch(Exception e) {
                                    log.error "exception resending PDU: ${e.message}"
                                    resendQueue.add(Collections.singletonMap(pdu, channel))
                            }
                    }
                }
            } finally {
                telnetService.disconnect()

                log.trace 'unlocking access to telnet service'
                lock.unlock()
            }
        } else {
            log.info 'resend queue is empty'
        }

        log.trace '<< resendPdu'
        return resendMap
    }

    public List<Sms> getNewSms() {
        log.trace '>> getNewSms'
        log.debug 'getNewSms w/o params'

        List<Sms> res = []

        synchronized (this) {
            log.trace 'locking access to telnet service'
            lock.lock()

            try {
                telnetService.connect()
                telnetService.privMode(true)

                cfg.channels.each {
                    def tmp = getNewSmsFromChannel(it)

                    log.debug "list from channel $it: $tmp"

                    res.addAll(tmp)
                    log.debug "res: $res"
                }
            } catch (Exception e) {
                log.error "Exception getting new SMS: ${e.message}"
            } finally {
                telnetService.disconnect()

                log.trace 'unlocking access to telnet service'
                lock.unlock()
            }
        }

        log.trace '<< getNewSms'
        res
    }

    private List<Sms> getNewSmsFromChannel(int channel) {
        log.trace '>> getNewSmsFromChannel'
        log.debug "getNewSmsFromChannel with $channel"

        if (!(channel in cfg.channels)) {
            log.error "$channel not if configured channels"
            throw new TelnetServiceException(invChannel)
        }

        def smsMap
        def res = []
        Map<Integer, String> exceptions = [:]

        log.trace 'reading sms list'
        smsMap = readSMSList(channel)
        log.debug "smsMap: $smsMap"

        log.trace 'reading PDUs'
        smsMap.each {
            try {
                def rawPdu = readRawSmsPdu(channel, it.key)
                try {
                    res.add(Sms
                            .valueOf(rawPdu)
                            .setStatus('i' as char)
                            .setIncoming(true)
                    )

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

        log.trace 'collecting multipart messages'
        def mapEx = [:]
        exGroups.each {
            try {
                res.add(Sms
                        .valueOf(it.value.collect {map -> map.value})
                        .setStatus('i' as char)
                        .setIncoming(true)
                )

                log.trace 'deleting successfully processed parts'
                it.value.each {
                    map -> deleteSms(channel, map.key)
                }
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

        if (!(channel in cfg.channels)) {
            log.error "$channel not if configured channels"
            throw new TelnetServiceException(invChannel)
        }

        def cmd = "AT^SD=$channel,$idx"
        telnetService.write cmd

        // read until *smserr, *smsdel or exception
        String res
        boolean bRun = true
        try {
            while (bRun) {
                res = telnetService.readUntil(NL, TelnetService.lineLength)
                log.debug res

                if (res.contains('*smserr')) {
                    log.error "modem failed to delete sms"
                    bRun = false

                } else if (res.contains('*smsdel')) {
                    log.info 'SMS deleted'
                    bRun = false
                }
            }
        } catch (TelnetServiceException e) {
            log.error "exception deleting sms from modem: $e.message"
            throw new SmsServiceException(delFailed)
        }

        log.trace '<< deleteSms'
    }

    private Map<Integer, Integer> readSMSList(int channel) {
        log.trace '>> readSMSList'

        String strRawList

        if (!(channel in cfg.channels)) {
            log.error "$channel not if configured channels"
            throw new TelnetServiceException(invChannel)
        }

        def cmd = "AT^SX=$channel"
        telnetService.write cmd

        // read until *smserr, *smsinc....255 or exception
        String res
        StringBuilder sbSmsinc = new StringBuilder()
        boolean bRun = true
        try {
            while (bRun) {
                res = telnetService.readUntil(NL, TelnetService.lineLength)
                log.debug "got: $res"

                if (res.contains('*smserr')) {
                    log.error "modem failed to read sms"
                    bRun = false

                } else if (res.contains('*smsinc')) {
                    sbSmsinc.append(res)

                    if (res.contains("*smsinc: $channel,0,0,255"))
                        bRun = false
                }
            }

        } catch (TelnetServiceException e) {
            log.error "exception reading sms list from modem: $e.message"
            throw e
        }

        strRawList = sbSmsinc.toString()
        log.debug "raw list of SMS on channel $channel: $strRawList"

        def map = strRawList.split(NL).collectEntries {
            Integer[] parts = it.substring(it.indexOf(' ') + 1).split(',').collect {Integer.valueOf(it)}
            def m = [:]
            m.put parts[1], parts[2]
            m
        }

        map.remove(0)

        log.trace '<< readSMSList'
        map
    }

    private String readRawSmsPdu(int channel, int idx) {
        log.trace '>> readRawSmsPdu'

        if (!(channel in cfg.channels)) {
            log.error "$channel not if configured channels"
            throw new TelnetServiceException(invChannel)
        }

        def cmd = "AT^SR=$channel.$idx"
        telnetService.write cmd

        // read until *smserr, *smspdu or exception
        String res
        StringBuilder sbPdu = new StringBuilder()
        boolean bRun = true
        boolean bPdu = false
        try {
            while (bRun) {
                res = telnetService.readUntil(NL, TelnetService.lineLength)
                log.debug res

                if (res.contains('*smserr')) {
                    log.error "modem failed to read sms"
                    bRun = false

                } else if (res.contains('*smspdu')) {
                    bPdu = true
                }

                if (bPdu) {
                    sbPdu.append(res)

                    if (res.endsWith(NL))
                        bRun = false
                }
            }

        } catch (TelnetServiceException e) {
            log.error "exception deleting sms from modem: $e.message"
            throw new SmsServiceException(delFailed)
        }

        String smspdu = sbPdu.toString()
        String[] parts = smspdu.substring(smspdu.indexOf(' ') + 1).split(',')

        log.trace '<< readRawSmsPdu'

        parts[4]
    }

    private int selectChannel() {
        return cfg.channels[rnd.nextInt(cfg.channels.size())]
    }
}
