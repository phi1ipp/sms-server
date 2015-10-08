package com.grigorio.smsserver.repository

import com.grigorio.smsserver.domain.Pdu
import org.springframework.data.repository.CrudRepository

interface PduRepository extends CrudRepository<Pdu, Long> {
    List<Pdu> findByPdu(String aPdu)
    List<Pdu> findBySmsId(long anId)
    List<Pdu> findByRefNo(int refNo)
}
