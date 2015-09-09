package com.grigorio.smsserver.service

import com.grigorio.smsserver.config.TelnetServiceConfig
import com.grigorio.smsserver.exception.TelnetServiceException
import groovy.util.logging.Slf4j
import org.apache.commons.net.telnet.TelnetClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service

@Slf4j
@Service(value = "TelnetService")
@EnableConfigurationProperties(TelnetServiceConfig.class)
class TelnetService {
    @Autowired
    TelnetServiceConfig cfg

    TelnetClient tc = new TelnetClient()
    InputStream is
    PrintStream os

    static final String OK = 'OK'+NL, NL = '\r\n'

    public void connect() {
        log.trace '>> init'

        if (cfg == null) {
            throw new TelnetServiceException(TelnetServiceException.Reason.noConfig)
        }

        String strLogin = 'login: '
        String strPwd = 'Password: '
        tc.connect(cfg.host, cfg.port)

        is = tc.getInputStream()
        os = new PrintStream(tc.getOutputStream())

        log.debug readUntil(strLogin)
        write cfg.login

        log.debug readUntil(strPwd)
        write cfg.password

        log.debug readUntil(OK)

        log.trace '<< init'
    }

    public void disconnect() {
        log.trace '>> disconnect'

        tc.disconnect()

        log.trace '<< disconnect'
    }

    public String readUntil( String pattern ) {
        log.trace '>> readUntil'
        log.debug "readUntil with $pattern"
        try {
            char lastChar = pattern.charAt(pattern.length() - 1)

            StringBuffer sb = new StringBuffer()

            char ch = (char) is.read()

            while(true) {
                sb.append ch
                if (ch == lastChar)
                    if (sb.toString().endsWith(pattern)) {
                        log.trace '<< readUntil'
                        return sb.toString()
                    }

                ch = (char) is.read()
            }
        }
        catch(Exception e) {
            log.error(e.message, e.cause)
        }

        log.trace '<< readUntil'
        return null
    }

    public void write(String value) {
        log.trace '>> write'
        log.debug "write with $value"
        try {
            os.println value + NL
            os.flush()
        }
        catch( Exception e ) {
            log.error e.message, e.cause
        }

        log.trace '<< write'
    }
}

