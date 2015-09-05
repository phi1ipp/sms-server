import org.ajwcc.pduUtils.gsm3040.Pdu
import org.ajwcc.pduUtils.gsm3040.PduParser
import org.junit.Test
import org.smslib.Message
import org.smslib.OutboundMessage
import org.smslib.Service
import org.apache.commons.codec.binary.Hex

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
        String str = '07919761989901F0440B919771655885F90008519040616515218C05000319040104210443044904350441044204320443043504420020044304410442043E0439044704380432044B04390020043C043804440020043E0020043A04300447043504410442043204350020043E0431044004300437043E04320430043D0438044F0020043200200410043D0433043B04380438002E0020041D04350020044104420430043D0443'

        assert Integer.toHexString(getChecksum(str) & 0xff) == 'c3'
        assert getLength(str) != 159

        Pdu pdu = new PduParser().parsePdu(str)
        println pdu.concatInfo
        println pdu.getDecodedText()

        str = '07919761989901F0440B919771655885F90008519040616585218C0500031904020020044D0442043E0433043E0020043E044204400438044604300442044C0020002D0020043A0430044704350441044204320435043D043D043E04350020043E0431044004300437043E04320430043D0438044F0020043200200410043D0433043B043804380020043D04350441043E043C043D0435043D043D043E0020043504410442044C'

        assert Integer.toHexString(getChecksum(str) & 0xff) == '77'
        assert getLength(str) != 159

        pdu = new PduParser().parsePdu(str)
        println pdu.concatInfo
        println pdu.getDecodedText()

        str = '07919761989901F0440B919771655885F90008519040617540218C050003190403002C00200432043E044200200442043E043B044C043A043E0020043E043D043E002004410430043C043E002C002004320020043E0442043B04380447043804380020043C0438044404300020043E0020043D0435043C002C0020002D00200441043E043204410435043C0020043D043500200434043B044F00200434043B044F002004320441'

        assert Integer.toHexString(getChecksum(str) & 0xff) == '83'
        assert getLength(str) != 159

        pdu = new PduParser().parsePdu(str)
        println pdu.concatInfo
        println pdu.getDecodedText()

        str = '07919761989901F0440B919771655885F90008519040617511210E05000319040404350445002E0020'

        pdu = new PduParser().parsePdu(str)
        println pdu.concatInfo
        println pdu.getDecodedText()
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
        def m = [1:2, 2:2, 3:3]
        println m
    }
}
