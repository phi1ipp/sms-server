import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.exception.SmsException
import org.ajwcc.pduUtils.gsm3040.PduParser
import org.junit.Test

class SmsTest {
    def simpleSms = '07919761989901F0040B919771655885F900085190404191522118041F0440043E04410442043E002004420435044104420020'
    def part1 = '07919761989901F0440B919771655885F90008519040713232218C0500031A0401042204350445043D043E043B043E04330438044704350441043A0438043C0438002000AB043F0435044004350434043E04320438043A0430043C043800BB002004320020043C043E04310438043B044C043D043E0439002004410432044F0437043800200442044004300434043804460438043E043D043D043E002004410447043804420430'
    def part2 = '07919761989901F0440B919771655885F90008519040713203218C0500031A0402044E04420441044F0020042F043F043E043D0438044F002004380020042E0436043D0430044F0020041A043E04400435044F002C0020043F043E0020043E04460435043D043A043500200043006F006E00740065006E00740020005200650076006900650077002C0020043E043D04380020043E043A043004370430043B04380441044C0020'
    def part3 = '07919761989901F0440B919771655885F90008519040713263218C0500031A040304320020043A043E043D04460435002004400435043904420438043D04330430002004410020043D0435043E0436043804340430043D043D043E00200432044B0441043E043A0438043C0438002004460435043D0430043C0438002004370430002004330438043304300431043004390442002000280441043C002E00200438043D0444043E'
    def part4 = '07919761989901F0440B919771655885F9000851904071323421180500031A040404330440043004440438043A04430029002E'

    def invPart = '07919761989901F0440B919771655885F90008519040617511210E05000319040404350445002E0020'

    @Test
    void testSimpleSmsFromPdu() {
        println Sms.valueOf(new PduParser().parsePdu(simpleSms))
    }

    @Test
    void testSimpleSmsFromString() {
        println Sms.valueOf(simpleSms)
    }

    @Test
    void failSimpleSms() {
        try {
            println Sms.valueOf(part1)
        } catch (SmsException e) {
            println "Test OK: ${e.message}"
        }
    }

    @Test
    void testSmsFromPdus() {
        def ar = [part1, part2, part3, part4]

        println Sms.valueOf(ar as String[])
    }

    @Test
    void testSmsFromRawPduList() {
        def ar = [part1, part2, part3, part4]

        println Sms.valueOf(ar)
    }

    @Test
    void testSmsFromShuffledPdus() {
        def ar = [part3, part1, part2, part4]

        println Sms.valueOf(ar as String[])
    }

    @Test
    void failSmsFromPdusDiffRefNo() {
        def ar = [part3, part1, invPart, part4]

        try {
            println Sms.valueOf(ar as String[])
        } catch (SmsException e) {
            println "Test OK: ${e.message}"
        }
    }

    @Test
    void failSmsFromNotAllPdus() {
        def ar = [part3, part1, part4]

        try {
            println Sms.valueOf(ar as String[])
        } catch (SmsException e) {
            println "Test OK: ${e.message}"
        }
    }

    void testPdu() {
        println new PduParser().parsePdu(part1).mpRefNo
        println new PduParser().parsePdu(part1).mpMaxNo
        println new PduParser().parsePdu(part1).mpSeqNo

        println new PduParser().parsePdu(part2).mpRefNo
        println new PduParser().parsePdu(part2).mpMaxNo
        println new PduParser().parsePdu(part2).mpSeqNo

        println new PduParser().parsePdu(part3).mpRefNo
        println new PduParser().parsePdu(part3).mpMaxNo
        println new PduParser().parsePdu(part3).mpSeqNo

        println new PduParser().parsePdu(part4).mpRefNo
        println new PduParser().parsePdu(part4).mpMaxNo
        println new PduParser().parsePdu(part4).mpSeqNo

        println new PduParser().parsePdu(invPart).mpRefNo
    }
}
