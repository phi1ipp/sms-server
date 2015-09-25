import com.grigorio.smsserver.config.MailerServiceConfig
import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.service.MailerService
import org.junit.Before
import org.junit.Test

import java.util.regex.Matcher
import java.util.regex.Pattern

class MailerServiceTest {
    MailerService service
    @Before
    void setup() {
        MailerServiceConfig config = new MailerServiceConfig()
        config.server = '192.168.0.7'
        config.port = 143
        config.user = 'sms'
        config.password = 'zDf!12gfIUY'
        config.domain = 'icsynergy.com'
        config.forward = 'philipp.grigoryev@icsynergy.com'

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

    @Test
    void testParseContent() {
        def content = 'user=maxavia\n' +
                'pass=ticis\n' +
                'tels=+79175685589\n' +
                'fromphone=MAXAVIA\n' +
                'mess= всем привет\n\n'

        Map<String, String> fields = [:]

        Pattern pattern = Pattern.compile('mess=(.*)$', Pattern.DOTALL)
        Matcher matcher = pattern.matcher(content)

        println matcher.find()
        println matcher.group(0)

        pattern = Pattern.compile 'tels=(.*)'
        matcher = pattern.matcher(content)

        println matcher.find()
        println matcher.group(0)
    }

    @Test
    void testSendMail() {
        service.sendMail(service.cfg.forward, 'Проверка отправки email')
    }
}
