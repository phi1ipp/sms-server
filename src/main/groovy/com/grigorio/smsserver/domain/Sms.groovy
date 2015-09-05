package com.grigorio.smsserver.domain

import com.grigorio.smsserver.exception.SmsException
import org.ajwcc.pduUtils.gsm3040.Pdu
import org.ajwcc.pduUtils.gsm3040.PduParser

import static com.grigorio.smsserver.exception.SmsException.Reason.*


class Sms {
    String address, txt

    public Sms(String address, String txt) {
        this.address = address
        this.txt = txt
    }

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

    @Override
    String toString() {
        new StringBuilder('Address: ').append(address).append(' Text: ').append(txt).toString()
    }
}
