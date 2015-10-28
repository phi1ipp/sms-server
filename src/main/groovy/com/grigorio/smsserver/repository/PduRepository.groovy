package com.grigorio.smsserver.repository

import com.grigorio.smsserver.domain.Pdu
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import org.springframework.data.repository.query.Param

interface PduRepository extends CrudRepository<Pdu, Long> {
    List<Pdu> findByPdu(String aPdu)
    List<Pdu> findBySmsId(long anId)
    List<Pdu> findByRefNoAndChannel(int refNo, int chan)

    @Query(value = "select p.* \
                    from pdu p join sms s on p.sms_id = s.id \
                    where now() > date_add(s.ts, interval :hrs hour)",
        nativeQuery = true)
    List<Pdu> findExpired(@Param('hrs') int hrs)
}
