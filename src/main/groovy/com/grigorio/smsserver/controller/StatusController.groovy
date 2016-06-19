package com.grigorio.smsserver.controller

import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.repository.SmsRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.MessageSource
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.format.TextStyle

@Slf4j
@Controller
class StatusController {
    @Autowired
    SmsRepository smsRepo

    @Autowired
    MessageSource msgSrc

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
        return 'current_new'
    }

    /** * Produces monthly statistics
     * @param model
     * @param month
     * @return
     */
    @RequestMapping(value = '/stat/month/{month}', produces = 'text/html; charset=utf-8')
    String showMonthlyStat(Model model, @PathVariable int month) {
        log.trace '>> showMonthlyStat'
        log.debug "showMonthlyStat: $month"

        if (month < 1 || month > 12) {
            model.addAttribute('errmsg', String.format(msgSrc.getMessage('errmsg.invmonth', null, Locale.default), month))
            return 'error'
        }

        int year = LocalDate.now().getYear()

        log.trace '<< showMonthlyStat'
        return showMonthlyStatForYear(model, year, month)
    }

    /** * Produces monthly statistics
     * @param model
     * @param year
     * @param month
     * @return
     */
    @RequestMapping(value = '/stat/month/{month}/year/{year}', produces = 'text/html; charset=utf-8')
    String showMonthlyStatForYear(Model model, @PathVariable int year, @PathVariable int month) {
        log.trace '>> showMonthlyStatForYear'
        log.debug "showMonthlyStatForYear: year=$year month=$month"

        if (month < 1 || month > 12) {
            model.addAttribute('errmsg', String.format(msgSrc.getMessage('errmsg.invmonth', null, Locale.default), month))
            return 'error'
        }

        log.trace 'querying DB'
        List<Object[]> lst = smsRepo.getStatByMonthForYear(year, month)

        log.debug "Data from DB: $lst"

        model.addAttribute('year', year)
        model.addAttribute('month', Month.of(month).getDisplayName(TextStyle.FULL_STANDALONE, new Locale('ru', 'RU')))
        model.addAttribute('stat', lst)

        log.trace '<< showMonthlyStatForYear'
        return 'monthlyStatForYear'
    }
}
