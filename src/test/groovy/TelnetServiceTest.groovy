import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.service.TelnetService
import com.grigorio.smsserver.config.TelnetServiceConfig
import org.junit.After
import org.junit.Before
import org.junit.Test

class TelnetServiceTest {
    TelnetService service

    @Before
    void setup() {
        TelnetServiceConfig config = new TelnetServiceConfig()

        config.host = '192.168.0.105'
        config.port = 23
        config.login = 'Admin'
        config.password = 'Admin'

        service = new TelnetService()
        service.cfg = config

        service.connect()
    }

    @After
    void disconnect() {
        service.disconnect()
    }

    @Test
    void testService() {
        assert service != null
    }

    @Test
    void testWrite() {
        service.write "AT"
    }

    @Test
    void testPrivMode() {
        service.privMode(true)
    }

    @Test
    void testPrivModeTwice() {
        service.privMode(true)
        service.privMode(true)
    }

    @Test
    void testNonPrivMode() {
        service.privMode(true)
        service.privMode(false)
    }

    @Test
    void testNonPrivModeTwice() {
        service.privMode(false)
    }

    @Test
    void testModStatus() {
        service.write 'AT!G=A6'
        println service.readUntil('OK')

        service.write('AT^MS=0')
        println service.readUntil(TelnetService.NL)
        println service.readUntil(TelnetService.NL)

        service.write('AT^MI=0')
        println service.readUntil(TelnetService.NL)
        println service.readUntil(TelnetService.NL)

        service.write('AT^MS=1')
        println service.readUntil(TelnetService.NL)
        println service.readUntil(TelnetService.NL)

        service.write('AT^MI=1')
        println service.readUntil(TelnetService.NL)
        println service.readUntil(TelnetService.NL)
    }

    @Test
    void testSMSlist(){
        service.write 'AT!G=A6'
        println service.readUntil('OK')

        service.write 'AT^SX=0'
        println service.readUntil('*smsinc: 0,0,0,255')


        service.write 'AT^SX=1'
        println service.readUntil('*smsinc: 1,0,0,255')
    }

    @Test
    void testReadSMS() {
        service.write 'AT!G=A6'
        println service.readUntil('OK\r\n')

        service.write 'AT^SR=0.11'
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

    @Test
    void testMpSMS() {
        def part1 = '07919762929090F041000B914196329732F900088C050003010201041F04400438043D0433043B0020043204400435043C044F0020043E04420020043204400435043C0435043D04380020043A043804320430043B0020043A044004430433043B043E043900200433043E043B043E0432043E0439002C00200444043004400444043E0440043E0432043E043500200441043204350440043A0430043D04380435'
        def part2 = '07919762929090F041000B914196329732F90008680500030102020020043C0435043600200441043A043B04300434044704300442044B0445002004330443043100200432044B0434043004320430043B043E002004410438043B044C043D043E043500200432043E043704310443043604340435043D04380435002E'
        service.write 'AT^SM=0,' + Sms.getLength(part1) + ',' + part1 + ',' + Integer.toHexString(Sms.getChecksum(part1) & 0xff)
        println service.readUntil('\r\n')
        println service.readUntil('\r\n')

        service.write 'AT^SM=0,' + Sms.getLength(part2) + ',' + part2 + ',' + Integer.toHexString(Sms.getChecksum(part2) & 0xff)
        println service.readUntil('\r\n')
        println service.readUntil('\r\n')
    }

    @Test
    void a() {
        println (0xFFFF as Character)
    }
}
