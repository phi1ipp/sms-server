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

    public void sendSms(Sms sms) {
        log.trace '>> sendSms'

        int channel = selectChannel()
        log.debug "channel to send: ${channel}"

        List<String> pdus = sms.toRawPdu(cfg.smsc)
        log.debug "pdus to send: $pdus"

        synchronized (this) {
            log.trace 'locking access to telnet service'
            lock.lock()

            try {
                telnetService.connect()
                telnetService.privMode(true)

                pdus.each {
                    sendPdu(it, channel)
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
    }

    void sendSms(String addr, String txt) {
        sendSms(new Sms(addr, txt))
    }

    void sendPdu(String pdu, int channel) {
        log.trace '>> sendPdu'
        log.debug "sendPdu with $pdu on channel $channel"

        if (!(channel in cfg.channels)) {
            log.error "$channel not if configured channels"
            throw new TelnetServiceException(invChannel)
        }

        if (!telnetService.connected || !telnetService.priveleged)
            throw new SmsServiceException(SmsServiceException.Reason.telnetNoPriv)

        try {
            String cmd = "AT^SM=$channel,$pdu"
            telnetService.write cmd
            log.debug telnetService.readUntil(cmd + NL, 2 * cmd.size())

            def res = telnetService.readUntil(NL, TelnetService.lineLength)
            log.debug res

            if (res.contains('*smserr')) {
                log.warn "modem failed to send pdu, adding to retry list"
                resendQueue.add(Collections.singletonMap(pdu, channel))
            }
        } catch (Exception e) {
            log.error "Exception ${e.message} sending pdu"

            log.trace 'adding pdu to resend queue'
            resendQueue.add(Collections.singletonMap(pdu, channel))
        }

        log.trace '<< sendPdu'
    }

    void resendPdu() {
        log.trace '>> resendPdu'

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
                                sendPdu(pdu, channel)
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

        log.debug telnetService.readUntil(NL, 2 * cmd.length())

        def read = telnetService.readUntil(NL, TelnetService.lineLength)
        log.debug read

        if (read.contains('*smserr'))
            throw new TelnetServiceException(delFailed)

        log.info 'SMS deleted'

        log.trace '<< deleteSms'
    }

    private Map<Integer, Integer> readSMSList(int channel) {
        log.trace '>> readSMSList'

        String strRawList

        if (!(channel in cfg.channels)) {
            log.error "$channel not if configured channels"
            throw new TelnetServiceException(invChannel)
        }

        def cmd = "AT^SX=${channel}"
        telnetService.write cmd

        def read = telnetService.readUntil(cmd + NL, 2 * cmd.length())
        log.debug "got: $read"
        if (!read.contains(cmd)) {
            log.error "couldn't read from modem"
            throw new TelnetServiceException(readFailed)
        }

        StringBuilder rawList = new StringBuilder()

        log.trace "reading list of SMS from modem"
        boolean done = false
        while (!done) {
            def line = telnetService.readUntil(NL, 80)
            log.debug "got: $line"

            if (line.contains("*smsinc: $channel,0,0,255"))
                done = true

            if (line.contains('*smsinc'))
                rawList.append(line)
        }

        strRawList = rawList.toString()
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

        if (!(channel in cfg.channels)) {
            log.error "$channel not if configured channels"
            throw new TelnetServiceException(invChannel)
        }

        def cmd = "AT^SR=$channel.$idx"
        telnetService.write cmd

        def read = telnetService.readUntil(cmd + NL, 2 * cmd.length())
        log.debug "got: $read"
        if (!read.contains(cmd)) {
            log.error "couldn't read from modem"
            throw new TelnetServiceException(readFailed)
        }

        String smspdu = telnetService.readUntil(NL, 10 * TelnetService.lineLength)
        log.debug "smspdu: $smspdu"

        if (!smspdu.contains('*smspdu')) {
            log.error "couldn't read from modem"
            throw new TelnetServiceException(readFailed)
        }

        String[] parts = smspdu.substring(smspdu.indexOf(' ') + 1).split(',')

        log.trace '<< readRawSmsPdu'

        parts[4]
    }

    private int selectChannel() {
        return cfg.channels[rnd.nextInt(cfg.channels.size())]
    }
}
