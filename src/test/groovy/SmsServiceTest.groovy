import com.grigorio.smsserver.config.SmsServiceConfig
import com.grigorio.smsserver.service.SmsService
import com.grigorio.smsserver.config.TelnetServiceConfig
import com.grigorio.smsserver.service.TelnetService
import org.junit.After
import org.junit.Before
import org.junit.Test

class SmsServiceTest {
    SmsService service

    @Before
    void setup() {
        service = new SmsService()
        service.telnetService = new TelnetService()

        TelnetServiceConfig config = new TelnetServiceConfig()

        config.host = '192.168.0.105'
        config.port = 23
        config.login = 'Admin'
        config.password = 'Admin'

        service.telnetService.cfg = config
        service.telnetService.connect()

        service.cfg = new SmsServiceConfig()
        service.cfg.channels = [0, 1]
        service.cfg.smsc = '+79262909090'
        service.cfg.validHours = 1
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
    void testGetNewSms() {
        println service.getNewSms()
    }

    @Test
    void testGetNewSmsMap() {
        println service.getNewSmsMap()
    }

    @Test
    void testSendSms() {
        println service.sendSms('+79175685589', 'Проверка связи, спи спокойно!')
    }

    @Test
    void testSendMultiSms() {
        service.sendSms('+79175685589', 'Поскольку во всех предыдущих погружениях мы обнаруживали Призраков лишь по счастливой случайности, то я прошу всех присутствующих быть предельно внимательными и сообщать мне обо всех необычных явлениях.')
    }

    @Test
    void testSendMultiSms2() {
        println service.sendSms('+79175685589', 'Поскольку во всех предыдущих погружениях мы обнаруживали Призраков лишь по счастливой случайности,')
    }
}
