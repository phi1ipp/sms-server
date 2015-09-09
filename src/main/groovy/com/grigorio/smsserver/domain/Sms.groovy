package com.grigorio.smsserver.domain

import com.grigorio.smsserver.exception.SmsException
import groovy.util.logging.Slf4j
import org.ajwcc.pduUtils.gsm3040.Pdu
import org.ajwcc.pduUtils.gsm3040.PduParser
import org.apache.commons.codec.binary.Hex
import org.smslib.Message
import org.smslib.OutboundMessage

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType
import javax.persistence.Id
import javax.persistence.Transient
import java.lang.reflect.Field
import java.time.LocalDateTime

import static com.grigorio.smsserver.exception.SmsException.Reason.*


@Slf4j
@Entity
class Sms {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    long id

    String address, txt, status
    LocalDateTime ts

    byte refNo

    @Transient int iValidHours

    public Sms(String address, String txt) {
        this.address = address
        this.txt = txt

        ts = LocalDateTime.now()
        refNo = new Random().nextInt()
    }

    public Sms valid(int iHours) {
        this.iValidHours = iHours
        this
    }

    protected Sms() {}

    static Sms valueOf(String rawPdu) {
        if (rawPdu == null) {
            throw new SmsException(argNull)
        }

        Pdu pdu = new PduParser().parsePdu(rawPdu)
        if (pdu.concatInfo != null) {
            throw new SmsException(argMultiPart)
        }

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

    List<String> toRawPdu(String smsc) {
        log.trace '>> toRawPdu'

        OutboundMessage msg = new OutboundMessage(address, txt)
        msg.setEncoding(Message.MessageEncodings.ENCUCS2)
        msg.statusReport = true

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
        new StringBuilder('SMS[Address: ').append(address).append(' Text: ').append(txt).append(']').toString()
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
}
