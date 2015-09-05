package com.grigorio.smsserver.service

import com.grigorio.smsserver.exception.TelnetServiceException
import com.grigorio.smsserver.config.TelnetServiceConfig
import groovy.util.logging.Slf4j
import org.apache.commons.net.telnet.TelnetClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component

import static TelnetServiceException.Reason.*

@Slf4j
@Component(value = "TelnetService")
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

        assert cfg != null

        String strLogin = 'login: '
        String strPwd = 'Password: '
        tc.connect(cfg.host, cfg.port)

        is = tc.getInputStream()
        os = new PrintStream(tc.getOutputStream())

        log.debug readUntil(strLogin)
        write cfg.login + NL

        log.debug readUntil(strPwd)
        write cfg.password + NL

        log.debug readUntil(OK)

        write 'AT!G=A6' + NL
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
        try {
            char lastChar = pattern.charAt(pattern.length() - 1)

            StringBuffer sb = new StringBuffer()

            char ch = (char) is.read()

            while(true) {
                sb.append ch
                if (ch == lastChar)
                    if (sb.toString().endsWith(pattern))
                        return sb.toString()

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
        try {
            os.println value
            os.flush()
        }
        catch( Exception e ) {
            log.error e.message, e.cause
        }

        log.trace '<< write'
    }

    public Map<Integer, Integer> readSMSList(int channel) {
        log.trace '>> readSMSList'

        write "AT^SX=${channel}\r\n"
        log.debug readUntil('\r\n')

        String strRawList = readUntil("*smsinc: ${channel},0,0,255\r\n")
        log.debug "raw list of SMS on channel ${channel}: ${strRawList}"

        def res = strRawList.split('\r\n').collectEntries {
            Integer[] parts = it.substring(it.indexOf(' ') + 1).split(',').collect {Integer.valueOf(it)}
            def m = [:]
            m.put parts[1], parts[2]
            m
        }

        res.remove(0)

        log.trace '<< readSMSList'
        res
    }

    public String readRawSmsPdu(int channel, int idx) {
        log.trace '>> readRawSmsPdu'

        if (channel < 0 || channel > 1)
            throw new TelnetServiceException(invChannel)

        write "AT^SR=${channel}.${idx}$NL"
        log.debug readUntil(NL)

        String smspdu = readUntil(NL)
        log.debug "smspdu: ${smspdu}"

        if (smspdu.contains('*smserr'))
            throw new TelnetServiceException(readFailed)

        String[] parts = smspdu.substring(smspdu.indexOf(' ') + 1).split(',')

        log.trace '<< readRawSmsPdu'

        parts[4]
    }

    public void deleteSms(int channel, int idx) {
        log.trace '>> deleteSms'

        if (channel < 0 || channel > 1)
            throw new TelnetServiceException(invChannel)

        write "AT^SD=$channel,$idx$NL"

        log.debug readUntil(NL)
        def read = readUntil NL

        if (read.contains('*smserr'))
            throw new TelnetServiceException(delFailed)

        log.debug read

        log.trace '<< deleteSms'
    }
}
