package cl.duoc.bancoxyz.bff;
import cl.duoc.bancoxyz.bff.common.client.*;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.web.server.ResponseStatusException;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
@SpringBootTest @AutoConfigureMockMvc
class ChannelFlowTests {
 static final RSAKey KEY; static final HttpServer JWKS;
 static {
  try {
   KEY=new RSAKeyGenerator(2048).keyID("test-channel").generate();
   JWKS=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
   JWKS.createContext("/jwks",e->{var bytes=new JWKSet(KEY.toPublicJWK()).toString().getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json");e.sendResponseHeaders(200,bytes.length);try(var out=e.getResponseBody()){out.write(bytes);}});
   JWKS.start();
  } catch(Exception e) {throw new ExceptionInInitializerError(e);}
 }
 @DynamicPropertySource static void properties(DynamicPropertyRegistry r) { r.add("spring.security.oauth2.resourceserver.jwt.jwk-set-uri",()->"http://127.0.0.1:"+JWKS.getAddress().getPort()+"/jwks"); }
 @AfterAll static void close() { JWKS.stop(0); }
 @Autowired MockMvc mvc;
 @MockitoBean CustomerServiceClient customers; @MockitoBean ManagedAccountClient accounts; @MockitoBean PaymentServiceClient payments;
 final UUID id=UUID.fromString("10000000-0000-0000-0000-000000000001");
 JwtRequestPostProcessor auth(String scope) { return jwt().jwt(j->j.subject("mobile-demo")).authorities(new SimpleGrantedAuthority("SCOPE_mobile."+scope)); }
 @Test void missingTokenAndOtherChannelScopeAreRejected() throws Exception {
  mvc.perform(get("/api/mobile/clientes/"+id)).andExpect(status().isUnauthorized());
  mvc.perform(get("/api/mobile/clientes/"+id).with(jwt().authorities(new SimpleGrantedAuthority("SCOPE_web.customers.read")))).andExpect(status().isForbidden());
  verifyNoInteractions(customers);
 }
 @Test void signedWebTokenCannotBeUsedInThisBff() throws Exception {
  var token=new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("test-channel").build(),
   new JWTClaimsSet.Builder().issuer("http://localhost:9000").audience("bff-web").subject("web-demo").expirationTime(new Date(System.currentTimeMillis()+60000)).claim("scope","mobile.customers.read").build());
  token.sign(new RSASSASigner(KEY));
  mvc.perform(get("/api/mobile/clientes/"+id).header("Authorization","Bearer "+token.serialize())).andExpect(status().isUnauthorized());
  verifyNoInteractions(customers);
 }
 @Test void profileResponseIsReducedAndTokenForwarded() throws Exception {
  when(customers.find(eq(id),anyString())).thenReturn(ResponseEntity.ok(new tools.jackson.databind.ObjectMapper().writeValueAsString(Map.of("clienteId",id.toString(),"nombre","Demo","email","demo@example.invalid","documento","privado","direccion","privado"))));
  mvc.perform(get("/api/mobile/clientes/"+id).with(auth("customers.read"))).andExpect(status().isOk())
   .andExpect(jsonPath("$.nombre").value("Demo")).andExpect(jsonPath("$.documento").doesNotExist()).andExpect(jsonPath("$.direccion").doesNotExist());
  verify(customers).find(id,"token");
 }
 @Test void accountResponsePreservesDecimalPrecision() throws Exception {
  when(accounts.find(eq(id),anyString())).thenReturn(ResponseEntity.ok(new tools.jackson.databind.ObjectMapper().writeValueAsString(Map.of("cuentaId",id.toString(),"saldo",new java.math.BigDecimal("9007199254740991.25"),"moneda","CLP","estado","ACTIVA","created_by","privado"))));
  mvc.perform(get("/api/mobile/cuentas-modernas/"+id+"").with(auth("accounts.read"))).andExpect(status().isOk())
   .andExpect(content().string(containsString("9007199254740991.25"))).andExpect(jsonPath("$.created_by").doesNotExist());
 }
 @Test void acceptedPaymentKeepsLocationButOmitsInternalDetails() throws Exception {
  when(payments.create(any(),anyString(),anyString())).thenReturn(ResponseEntity.status(202).body(new tools.jackson.databind.ObjectMapper().writeValueAsString(Map.of("pagoId",id.toString(),"estado","PENDIENTE","actor","privado","referencia","privado"))));
  mvc.perform(post("/api/mobile/pagos").with(auth("payments.write")).header("Idempotency-Key",id).contentType("application/json").content("{}"))
   .andExpect(status().isAccepted()).andExpect(header().string("Location","/api/mobile/pagos/"+id))
   .andExpect(jsonPath("$.estado").value("PENDIENTE")).andExpect(jsonPath("$.actor").doesNotExist()).andExpect(jsonPath("$.referencia").doesNotExist());
  verify(payments).create(id,"{}","token");
 }
 @Test void backendOutageReturns503WithoutInventingData() throws Exception {
  when(accounts.find(eq(id),anyString())).thenThrow(new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Servicio temporalmente no disponible"));
  mvc.perform(get("/api/mobile/cuentas-modernas/"+id+"").with(auth("accounts.read"))).andExpect(status().isServiceUnavailable());
 }

 @Test void accountOpeningIsForwardedWithPublicLocation() throws Exception {
  when(accounts.create(any(),anyString(),anyString())).thenReturn(ResponseEntity.status(201).body("{\"cuentaId\":\""+id+"\",\"saldo\":0}"));
  mvc.perform(post("/api/mobile/cuentas-modernas").with(auth("accounts.write")).header("Idempotency-Key",id).contentType("application/json").content("{}"))
   .andExpect(status().isCreated()).andExpect(header().string("Location","/api/mobile/cuentas-modernas/"+id));
 }

}
