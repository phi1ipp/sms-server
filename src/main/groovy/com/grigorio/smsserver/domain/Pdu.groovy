package com.grigorio.smsserver.domain

import org.ajwcc.pduUtils.gsm3040.PduParser

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.Transient

@Entity
class Pdu {
    @Id
    @GeneratedValue
    long id

    @Column(name = 'sms_id')
    long smsId

    String pdu
    int refNo
    char status = 's'

    @Transient
    String addr

    Pdu(String aPdu, int aRefNo, long smsId) {
        this.pdu = aPdu
        this.refNo = aRefNo
        this.smsId = smsId

        addr = new PduParser().parsePdu(this.pdu).address

        if (aRefNo < 0)
            status = 'u'
    }

    @Override
    String toString() {
        return "[PDU - status=$status refNo=$refNo smsId=$smsId pdu=$pdu]"
    }

    // default constructor
    Pdu() {}
}
