import com.grigorio.smsserver.service.SmsService
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

@RunWith(SpringJUnit4ClassRunner)
@ContextConfiguration(classes = [SmsService.class, TelnetService.class], initializers = ConfigFileApplicationContextInitializer.class)
class SmsServiceTest {
    @Autowired
    SmsService service

    @Autowired
    Environment env

    @Before
    void setup() {
        TelnetServiceConfig config = new TelnetServiceConfig()

        config.host = env.getProperty('telnet.host')
        config.port = Integer.parseInt(env.getProperty('telnet.port'))
        config.login = env.getProperty('telnet.login')
        config.password = env.getProperty('telnet.password')

        service.telnetService.cfg = config
        service.telnetService.connect()
    }

    @After
    void disconnect() {
        service.telnetService.disconnect()
    }

    @Test
    void getService() {
        assert service != null
    }

    @Test
    void testNewSms() {
        println service.newSms(0)
    }

    @Test
    void testGetNewSms() {
        println service.getNewSms(0)
    }
}
