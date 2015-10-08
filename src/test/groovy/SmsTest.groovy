import com.grigorio.smsserver.domain.Sms
import com.grigorio.smsserver.domain.StatusReportSms
import com.grigorio.smsserver.exception.SmsException
import org.ajwcc.pduUtils.gsm3040.Pdu
import org.ajwcc.pduUtils.gsm3040.PduParser
import org.ajwcc.pduUtils.gsm3040.SmsDeliveryPdu
import org.ajwcc.pduUtils.gsm3040.SmsStatusReportPdu
import org.junit.Test
import org.smslib.Message
import org.smslib.OutboundMessage

import java.util.regex.Matcher
import java.util.regex.Pattern

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

    @Test
    void testToRawPdu() {
        def msg = 'Прингл время от времени кивал круглой головой, фарфоровое сверкание меж складчатых губ выдавало сильное возбуждение.'
        def smsc = '+79262909090'

        def str = new Sms('+14692379239', msg).toRawPdu(smsc)

        str.each {
            String[] parts = it.split(',')

            assert Sms.getChecksum(parts[1]) == (Integer.valueOf(parts[2], 16) & 0xff) as byte
            assert Sms.getLength(parts[1]) == Integer.valueOf(parts[0])
        }

        println str
    }

    @Test
    void testToRawPduValidity() {
        def msg = 'Прингл время от времени кивал круглой головой, фарфоровое сверкание меж складчатых губ выдавало сильное возбуждение.'
        def smsc = '+79262909090'

        def str = new Sms('+14692379239', msg).valid(1).toRawPdu(smsc)

        str.each {
            String[] parts = it.split(',')

            assert Sms.getChecksum(parts[1]) == (Integer.valueOf(parts[2], 16) & 0xff) as byte
            assert Sms.getLength(parts[1]) == Integer.valueOf(parts[0])
        }

        println str
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

    @Test
    void testDeliverReport() {
        def pdu = '07919762020033F106920B919771655885F9519070021333215190700213532100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF'
        def pd = new PduParser().parsePdu(pdu)
        if (pd instanceof SmsStatusReportPdu)
            println "status=$pd.status time=$pd.dischargeTime"

    }
    @Test
    void testSmsPdus() {
        OutboundMessage msg = new OutboundMessage('+14692379239', 'Проверка телефона')
        msg.setEncoding(Message.MessageEncodings.ENCUCS2)
        msg.setValidityPeriod(60)

        println msg.getPdus('+79262909090', 1)
    }

    @Test
    void testDeliveryPdu() {
        println new PduParser().parsePdu('07919762020033F106BA0B919771655885F9519052504093215190525040142100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF')
        println Sms.valueOf('07919762020033F106BA0B919771655885F9519052504093215190525040142100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF')
    }

    @Test
    void testParseIncoming() {
        println new PduParser().parsePdu('07919762020033F12414D0CDF2396C7CBB23F7B21800085190525001902136041F0440043E043204350440043A0430002C0020043E043D04300020043204410435043C0020043F0440043E043204350440043A0430')
    }

    @Test
    void testRefNo() {
        println 'Outgoing: ' + new PduParser().parsePdu('07919762929090F031000B919771655885F90008FF52041D04430020043A043E0433043404300020043604350020043E043D002004370430044004300431043E0442043004350442002C0020044D0442043E0020044104420430044204430441003F0021000D000A')
        println 'Incoming: ' + new PduParser().parsePdu('07919762020033F106BF0B919771655885F9519052702454215190527024742100FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF')
    }

    @Test
    void testCorrectAddress() {
        println new Sms('79168492412', 'test').toRawPdu('+79168492412')
        println new Sms('79168492412', 'test').toRawPdu('+79168492412')
    }
}
