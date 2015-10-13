package com.grigorio.smsserver.controller

import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.repository.SmsRepository
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.thymeleaf.TemplateEngine
import org.thymeleaf.context.WebContext

import javax.servlet.ServletContext
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.time.LocalDateTime

@Slf4j
@RestController
class StatusController {
    @Autowired
    SmsRepository smsRepo

    @Autowired
    ApplicationContext appCtx

    @Autowired
    ServletContext servletContext

    @RequestMapping(value = '/status', produces = 'text/html; charset=utf-8')
    String showStatus(HttpServletRequest req, HttpServletResponse resp) {
        log.trace '>> showStatus'

        def depth = 2

        List<Sms> smsList =
                smsRepo.findByTsAfterAndStatusNotOrderByTsDesc(LocalDateTime.now().minusDays(depth), 'i' as char)
        log.debug "sms list: $smsList"

        WebContext ctx = new WebContext(req, resp, servletContext)
        ctx.setVariable('smsList', smsList)
        ctx.setVariable('depth', depth)

        TemplateEngine engine = appCtx.getBean(TemplateEngine.class)

        return engine.process('current', ctx)
    }
}
