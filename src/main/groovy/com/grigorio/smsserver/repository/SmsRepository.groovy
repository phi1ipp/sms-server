package com.grigorio.smsserver.repository

import com.grigorio.smsserver.domain.Sms
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

import org.springframework.data.repository.CrudRepository

import java.time.LocalDateTime

@Repository
interface SmsRepository extends CrudRepository<Sms, Long>{
    List<Sms> findByAddressAndTsAfterOrderByTsDesc(String addr, LocalDateTime dt)

    List<Sms> findByTsAfterAndStatusNotOrderByTsDesc(LocalDateTime dt, char status)

    //statistics for month and year
    @Query(value = 'select status, count(*), sum(parts) from sms where month(ts) = :m and year(ts) = :y group by status',
            nativeQuery = true)
    List<Object[]> getStatByMonthForYear(@Param('y') int year, @Param('m') int month)

    @Modifying
    @Query(value = 'delete from sms where timestampadd(day,:history,ts) < now()', nativeQuery = true)
    void cleanOldSms(@Param('history') int historyDepth)
}
