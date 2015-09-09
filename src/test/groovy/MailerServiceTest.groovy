import com.grigorio.smsserver.config.MailerServiceConfig
import com.grigorio.smsserver.service.MailerService
import org.junit.Before
import org.junit.Test

class MailerServiceTest {
    MailerService service
    @Before
    void setup() {
        MailerServiceConfig config = new MailerServiceConfig()
        config.server = '192.168.0.7'
        config.port = 110
        config.user = 'sms'
        config.password = 'zDf!12gfIUY'
        config.domain = 'icsynergy.com'

        service = new MailerService()
        service.cfg = config

        service.connect()
    }

    @Test
    void getService() {
        assert service != null
        assert service.session != null
        assert service.store != null
    }

    @Test
    void testCheckInbox() {
        println service.checkInbox()
    }

    @Test
    void testGetHeaders() {
        println service.getHeaders()
    }

    @Test
    void testGetSmsList() {
        def lst = service.getSmsList()
        println lst
    }
}
