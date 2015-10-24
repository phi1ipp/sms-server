package com.grigorio.smsserver.repository

import com.grigorio.smsserver.domain.Sms
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

import org.springframework.data.repository.CrudRepository

import java.time.LocalDateTime

@Repository
interface SmsRepository extends CrudRepository<Sms, Long>{
    List<Sms> findByAddressAndTsAfterOrderByTsDesc(String addr, LocalDateTime dt)

    List<Sms> findByTsAfterAndStatusNotOrderByTsDesc(LocalDateTime dt, char status)

    //statistics
    @Query(value = 'select status, count(*), sum(parts) from sms where month(ts) = :m group by status',
            nativeQuery = true)
    List<Object[]> getStatByMonth(@Param('m') int month)
}
