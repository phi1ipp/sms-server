package com.grigorio.smsserver.repository

import com.grigorio.smsserver.domain.Sms
import org.springframework.data.repository.CrudRepository

interface SmsRepository extends CrudRepository<Sms, Long>{
    List<Sms> findByAddress(String addr)
    List<Sms> findByAddressAndRefNo(String addr, Byte refNo)
}
