package com.grigorio.smsserver.domain

import com.grigorio.smsserver.exception.SmsException
import groovy.util.logging.Slf4j
import org.ajwcc.pduUtils.gsm3040.Pdu
import org.ajwcc.pduUtils.gsm3040.PduParser
import org.ajwcc.pduUtils.gsm3040.SmsDeliveryPdu
import org.ajwcc.pduUtils.gsm3040.SmsStatusReportPdu
import org.apache.commons.codec.binary.Hex
import org.smslib.Message
import org.smslib.OutboundMessage
import org.springframework.format.annotation.DateTimeFormat

import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Transient
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.regex.Matcher
import java.util.regex.Pattern

import static com.grigorio.smsserver.exception.SmsException.Reason.*


@Slf4j
@Entity
class Sms {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    String address, txt
    @DateTimeFormat(pattern = 'yyyy-MM-dd\nHH:mm:ss')
    LocalDateTime ts

    int refNo
    char status = 'u'   //undefined
    boolean incoming = false
    int parts = 0

    @Transient int iValidHours
    @Transient int channel

    public Sms(String address, String txt) {

        this.address = address
        this.txt = txt

        ts = LocalDateTime.now()
        refNo = (new Random().nextInt() & 0xff) as byte

        checkAndCorrectAddress()
    }

    protected Sms() {}

    static Sms valueOf(String rawPdu) {
        if (rawPdu == null) {
            throw new SmsException(argNull)
        }

        // if it's not a status report and it's a part of a multipart message
        Pdu pdu = new PduParser().parsePdu(rawPdu)
        if (!(pdu instanceof SmsStatusReportPdu) && pdu.concatInfo != null) {
            throw new SmsException(argMultiPart)
        }

        if (pdu instanceof SmsDeliveryPdu) {
            def sms = new Sms(pdu.address, pdu.decodedText)
            sms.status = 'i'
            sms.checkAndCorrectAddress()
            sms

        } else if (pdu instanceof SmsStatusReportPdu) {
            SmsStatusReportPdu reportPdu = pdu as SmsStatusReportPdu

            char status = 'u'

            switch (reportPdu.status) {
                case 0 :
                    status = 'd'
                    break
                default :
                    status = 'n'
            }
            def sms = new StatusReportSms(reportPdu.address, status)

            sms.refNo = reportPdu.messageReference
            sms

        } else
            new Sms(pdu.address, pdu.getDecodedText())
    }

    static Sms valueOf(Pdu pdu) {
        if (pdu.concatInfo != null) {
            throw new SmsException(argMultiPart)
        }

        new Sms(pdu.address, pdu.getDecodedText())
    }

    static Sms valueOf(List<String> lst) {
        return valueOf(lst as String[])
    }

    static Sms valueOf(String[] rawPdus) {
        if (rawPdus.length == 0)
            throw new SmsException(zeroLength)

        PduParser parser = new PduParser()

        Pdu[] pdus = rawPdus.collect { parser.parsePdu(it) }

        if (pdus[0].concatInfo == null)
            throw new SmsException(argMultiPart)

        if (pdus[0].mpMaxNo != rawPdus.length)
            throw new SmsException(notAllPdus)

        if (! pdus.every { it.mpRefNo == pdus[0].mpRefNo })
            throw new SmsException(mixedPdus)

        if (! pdus.every { it.address == pdus[0].address })
            throw new SmsException(mixedPdus)

        pdus = pdus.sort { a, b -> a.mpSeqNo <=> b.mpSeqNo }

        return new Sms(
                pdus[0].address,
                pdus.inject(new StringBuilder()) {
                    res, pdu -> res.append(pdu.decodedText)
                }.toString()
        )
    }

    Sms setIncoming(boolean flag) {
        incoming = flag
        this
    }

    List<String> toRawPdu(String smsc) {
        log.trace '>> toRawPdu'

        OutboundMessage msg = new OutboundMessage(address, txt)
        msg.setEncoding(Message.MessageEncodings.ENCUCS2)
        msg.statusReport = true
        msg.date = Date.from(ts.atZone(ZoneId.systemDefault()).toInstant())

        List<String> lstRawPdus = msg.getPdus(smsc, refNo)

        List<String> lst = lstRawPdus.collect {
            new StringBuilder()
                    .append(getLength(it))
                    .append(',')
                    .append(it)
                    .append(',')
                    .append(Integer.toHexString(getChecksum(it) & 0xff).toUpperCase())
                    .toString()
        }

        log.trace '<< toRawPdu'
        lst
    }

    @Override
    String toString() {
        new StringBuilder('SMS [Address: ')
                .append(address).append(' Text: ').append(txt)
                .append(' ref#: ').append(refNo)
                .append(' Date: ').append(ts.toString())
                .append(' Status: ').append(status)
                .append(' Channel: ').append(channel)
                .append(']').toString()
    }

    static int getLength(String str) {
        final Hex hex = new Hex()

        byte[] bt = hex.decode(str)

        return bt.size() - 8
    }

    static byte getChecksum(String str) {
        final Hex hex = new Hex()

        byte[] bt = hex.decode(str)

        byte cs = 0;
        bt.each { cs += it }
        return cs
    }

    protected String checkAndCorrectAddress() {
        Pattern ptn = Pattern.compile('^\\+\\d{11}$')
        Matcher matcher = ptn.matcher(address)

        // if addresses not in a required format, try to guess and fix
        if (!matcher.matches())
            if (address.toCharArray().each {it.isDigit()} && address.length() == 11)
                address = '+' + address

        address
    }

    Sms setStatus(char status) {
        this.status = status
        this
    }

    Sms setChannel(int channel) {
        this.channel = channel
        this
    }

    public Sms valid(int iHours) {
        this.iValidHours = iHours
        this
    }

}
