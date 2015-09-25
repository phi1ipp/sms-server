import com.grigorio.smsserver.Application
import com.grigorio.smsserver.repository.SmsRepository
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.boot.test.WebIntegrationTest
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*


@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest(['server.port=0'])
class SmsRequestControllerTest {
    @Autowired
    WebApplicationContext wac

    @Autowired
    SmsRepository smsRepo

    MockMvc mockMvc

    @Before
    void setup() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build()
    }

    @Test
    void testSendSms() {
        mockMvc
                .perform(
                    get('/send')
                            .param('phone_list', '+79175685589')
                            .param('message', 'Проверка, проверка...')
                            .param('http_username', 'testUser')
                            .param('http_password', 'testPassword'))
                .andExpect(status().isOk())

        assert smsRepo.count() > 0
        assert smsRepo.findByAddress('+14692379239').size() > 0

        println smsRepo.findByAddress('+14692379239').get(0).txt

        println smsRepo.findByAddressAndRefNo('+14692379239', Byte.valueOf('18')).size()
    }
}
