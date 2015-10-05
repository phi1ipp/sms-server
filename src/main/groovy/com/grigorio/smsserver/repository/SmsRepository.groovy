package com.grigorio.smsserver.repository

import com.grigorio.smsserver.domain.Sms
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository

import java.time.LocalDateTime

interface SmsRepository extends CrudRepository<Sms, Long>{
    List<Sms> findByAddress(String addr)
    List<Sms> findByAddressAndTsAfterOrderByTsDesc(String addr, LocalDateTime dt)

    @Query(value = 'select count(*) from sms', nativeQuery = true)
    List<Sms> findCount()
}
