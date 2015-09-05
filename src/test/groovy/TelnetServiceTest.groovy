import com.grigorio.smsserver.service.TelnetService
import com.grigorio.smsserver.config.TelnetServiceConfig
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.ConfigFileApplicationContextInitializer
import org.springframework.core.env.Environment
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = [TelnetService.class], initializers = ConfigFileApplicationContextInitializer.class)
class TelnetServiceTest {
    @Autowired
    TelnetService service

    @Autowired
    Environment env

    @Before
    void setup() {
        TelnetServiceConfig config = new TelnetServiceConfig()

        config.host = env.getProperty('telnet.host')
        config.port = Integer.parseInt(env.getProperty('telnet.port'))
        config.login = env.getProperty('telnet.login')
        config.password = env.getProperty('telnet.password')

        service.cfg = config

        service.connect()
    }

    @After
    void disconnect() {
        service.disconnect()
    }

    @Test
    void testService() {
        assert config != null
        assert service != null
    }

    @Test
    void testModStatus() {
        service.write 'AT!G=A6\r\n'
        println service.readUntil('OK\r\n')

        service.write('AT^MS=0\r\n')
        println service.readUntil('\r\n')
        println service.readUntil('\r\n')

        service.write('AT^MI=0\r\n')
        println service.readUntil('\r\n')
        println service.readUntil('\r\n')

        service.write('AT^MS=1\r\n')
        println service.readUntil('\r\n')
        println service.readUntil('\r\n')

        service.write('AT^MI=1\r\n')
        println service.readUntil('\r\n')
        println service.readUntil('\r\n')
    }

    @Test
    void testSMSlist(){
        service.write 'AT!G=A6\r\n'
        println service.readUntil('OK\r\n')

        service.write 'AT^SX=0\r\n'
        println service.readUntil('*smsinc: 0,0,0,255\r\n')


        service.write 'AT^SX=1\r\n'
        println service.readUntil('*smsinc: 1,0,0,255\r\n')
    }

    @Test
    void testReadSMSList() {
        println service.readSMSList(0)
    }

    @Test
    void testReadSmsPdu() {
        String strPdu = service.readRawSmsPdu(0, 6)
        println strPdu
    }

    @Test
    void testReadSMS() {
        service.write 'AT!G=A6\r\n'
        println service.readUntil('OK\r\n')

        service.write 'AT^SR=0.6\r\n'
        println service.readUntil('\r\n')
        println service.readUntil('\r\n')
    }

    @Test
    void testDelSMS() {
        service.write 'AT!G=A6\r\n'
        println service.readUntil('OK\r\n')

        service.write 'AT^SD=0.1\r\n'
        println service.readUntil('\r\n')
        println service.readUntil('\r\n')
    }
}
