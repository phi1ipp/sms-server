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
    OutputStream os

    boolean connected = false
    boolean priveleged = false

    static final String OK = 'OK' + NL, NL = '\r\n'

    public void connect() {
        log.trace '>> connect'

        if (cfg == null) {
            throw new TelnetServiceException(TelnetServiceException.Reason.noConfig)
        }

        if (!connected) {
            String strLogin = 'login: '
            String strPwd = 'Password: '

            log.trace 'connecting'
            tc.connect(cfg.host, cfg.port)

            log.trace 'getting streams'
            is = tc.getInputStream()
            os = new PrintStream(tc.getOutputStream())

            log.trace 'logging in'
            log.debug readUntil(strLogin)
            write cfg.login

            log.debug readUntil(strPwd)
            write cfg.password

            log.debug readUntil(OK)
        } else
            log.trace 'already connected'

        connected = true
        priveleged = false

        log.trace '<< connect'
    }

    public void disconnect() {
        log.trace '>> disconnect'

        if (!connected)
            log.info 'not connected'
        else
            tc.disconnect()

        connected = false
        priveleged = false

        log.trace '<< disconnect'
    }

    public void privMode(boolean to) {
        log.trace '>> privMode'
        log.debug "privMode with $to"

        if (!connected) {
            log.error 'not connected'
            throw new TelnetServiceException('Not connected')
        }

        if (to && priveleged) {
            log.debug 'already in priv mode'
        } else if (to && !priveleged) {
            write 'at!g=a6'
            readUntil OK

            priveleged = true
            log.debug 'in priv mode`'
        } else if (!to && !priveleged) {
            log.debug 'already in non-priv mode'
        } else {
            write 'at!g=55'
            readUntil OK

            priveleged = false

            log.debug 'in non-priv mode'
        }
        log.trace '<< privMode'
    }

    public String readUntil(String pattern, int maxRead) {
        log.trace '>> readUntil'
        log.debug "readUntil with $pattern and $maxRead"
        try {
            char lastChar = pattern.charAt(pattern.length() - 1)

            StringBuffer sb = new StringBuffer()

            char ch = (char) is.read()

            while(sb.length() < maxRead) {
                sb.append ch

//                log.debug sb.toString()

                if (ch == lastChar)
                    if (sb.toString().endsWith(pattern)) {
                        log.trace '<< readUntil'
                        return sb.toString()
                    }

                ch = (char) is.read()
            }

            log.trace '<< readUntil'
            sb.toString()

        } catch (Exception e) {
            log.error(e.message, e.cause)
            throw e
        }
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
        } catch (Exception e) {
            log.error(e.message, e.cause)
            throw e
        }
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
            throw e
        }

        log.trace '<< write'
    }
}

