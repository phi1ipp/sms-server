package com.grigorio.smsserver.controller

import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.repository.SmsRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle

@Slf4j
@Controller
class StatusController {
    @Autowired
    SmsRepository smsRepo

    @RequestMapping(value = '/status', produces = 'text/html; charset=utf-8')
    String showStatus(Model model) {
        log.trace '>> showStatus'

        //todo add as a parameter?
        def depth = 2

        List<Sms> smsList =
                smsRepo.findByTsAfterAndStatusNotOrderByTsDesc(LocalDateTime.now().minusDays(depth), 'i' as char)
        log.debug "sms list: $smsList"

        model.addAttribute('smsList', smsList)
        model.addAttribute('depth', depth)

        log.trace '<< showStatus'
        return 'current'
    }

    /**
     * Produces monthly statistics
     * @param model
     * @param month
     * @return
     */
    @RequestMapping(value = '/stat/month/{month}', produces = 'text/html; charset=utf-8')
    String showMonthlyStat(Model model, @PathVariable int month) {
        log.trace '>> showMonthlyStat'
        log.debug "showMonthlyStat: $month"

        log.trace 'querying DB'
        List<Object[]> lst = smsRepo.getStatByMonth(month)

        log.debug "Data from DB: $lst"

        model.addAttribute('month', Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, new Locale('ru', 'RU')))
        model.addAttribute('stat', lst)

        log.trace '<< showMonthlyStat'
        return 'monthlyStat'
    }
}
