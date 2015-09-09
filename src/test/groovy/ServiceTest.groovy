import org.ajwcc.pduUtils.gsm3040.Pdu
import org.ajwcc.pduUtils.gsm3040.PduParser
import org.junit.Test
import org.smslib.Message
import org.smslib.OutboundMessage
import org.smslib.Service
import org.apache.commons.codec.binary.Hex

import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class ServiceTest {
    @Test
    void aGetService() {
        Service service = Service.getInstance()
        assert service != null
    }

    @Test
    void getPDU() {
        OutboundMessage msg = new OutboundMessage('79168492412', 'Привет, Джавдед, как дела?')
        msg.setEncoding(Message.MessageEncodings.ENCUCS2)
        msg.setFlashSms(false)
        String[] strPDU = msg.getPdus('+79262909090', 2)

        strPDU.each {println "PDU:${it} Length:${Integer.toHexString(getLength(it))}"}
    }

    @Test
    void decodePDU() {
        println new PduParser().parsePdu('07919762020033F12414D0CDF2396C7CBB23F7B2180008519040707172211C041F0440043E043204350440043A0430002004410432044F04370438').getDecodedText()
    }

    @Test
    void testGetCS() {
        String str = '07910661929909F2240B910661124604F00000601190110253400154'
        assert Integer.toHexString(getChecksum(str) & 0xff) == '94'

        str = '07910661929909F2240B910661124604F000006011901161414007D4F29C1E93CD00'
        assert Integer.toHexString(getChecksum(str) & 0xff) == '73'

        str = '07919762020033F12414D0CDF2396C7CBB23F7B2180008519040707172211C041F0440043E043204350440043A0430002004410432044F04370438'
        assert Integer.toHexString(getChecksum(str) & 0xff) == '2a'

        str = '07919761989901F0440B919771655885F90008519040616515218C05000319040104210443044904350441044204320443043504420020044304410442043E0439044704380432044B04390020043C043804440020043E0020043A04300447043504410442043204350020043E0431044004300437043E04320430043D0438044F0020043200200410043D0433043B04380438002E0020041D04350020044104420430043D0443'
        assert Integer.toHexString(getChecksum(str) & 0xff) == 'c3'
        assert getLength(str) != 159
    }

    @Test
    void parseSMS() {
        String str = '07919762929090F051000B914196329732F90008FF8C050003E90201041F043E0441043A043E043B044C043A044300200432043E002004320441043504450020043F044004350434044B043404430449043804450020043F043E04330440044304360435043D0438044F04450020043C044B0020043E0431043D0430044004430436043804320430043B04380020041F04400438043704400430043A043E04320020'

        Pdu pdu = new PduParser().parsePdu(str)
        println pdu

        str = '07919762929090F051000B914196329732F90008FF44050003E90202043B04380448044C0020043F043E002004410447043004410442043B04380432043E043900200441043B0443044704300439043D043E044104420438002C'

        pdu = new PduParser().parsePdu(str)
        println pdu
    }

    int getLength(String str) {
        final Hex hex = new Hex()

        byte[] bt = hex.decode(str)

        return bt.size()
    }

    byte getChecksum(String str) {
        final Hex hex = new Hex()
        println str

        byte[] bt = hex.decode(str)

        int cs = 0;
        bt.each { cs += it }
        return cs
    }

    @Test
    void slice() {
        Date dt = new Date()
        Instant instant = Instant.from(LocalDateTime.now().atZone(ZoneId.systemDefault()))
        Timestamp ts = Timestamp.from(instant)
        println ts
    }

    @Test
    void staticVar() {
        for (int i = 0; i < 10; i++) {
            println new Random().nextInt(2)
        }
    }
}
