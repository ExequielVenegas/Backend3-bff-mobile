package cl.duoc.bancoxyz.bff;
import cl.duoc.bancoxyz.bff.common.client.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest @AutoConfigureMockMvc
class BffMobileApplicationTests {
 @Autowired MockMvc mvc; @MockitoBean AccountServiceClient client;
 @Test void contextLoads() {}
 @Test void returnsLightweightMobileRepresentation() throws Exception {
  when(client.findAccount(1)).thenReturn(new AccountServiceResponse(1,"Ana","AHORRO",bd("1050")));
  mvc.perform(get("/api/mobile/cuentas/1").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_mobile.read")))).andExpect(status().isOk()).andExpect(jsonPath("$.saldoDisponible").value(1050)).andExpect(jsonPath("$.saldoOriginal").doesNotExist()).andExpect(jsonPath("$.movimientos").doesNotExist());
 }
 private BigDecimal bd(String v){return new BigDecimal(v);}
}
