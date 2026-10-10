package cl.duoc.bancoxyz.bff.channels;
import cl.duoc.bancoxyz.bff.common.client.*;
import jakarta.validation.constraints.*;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.*;
import tools.jackson.databind.node.*;
@RestController @Validated @RequestMapping("/api/mobile")
public class ChannelController {
 private final CustomerServiceClient customers; private final ManagedAccountClient accounts; private final PaymentServiceClient payments; private final ObjectMapper json;
 public ChannelController(CustomerServiceClient customers,ManagedAccountClient accounts,PaymentServiceClient payments,ObjectMapper json) {
  this.customers=customers; this.accounts=accounts; this.payments=payments; this.json=json;
 }
 @GetMapping("/clientes/{id}") public ResponseEntity<String> customer(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
  return adapted(customers.find(id,jwt.getTokenValue()),null,null,"clienteId","nombre","email","version");
 }
 @GetMapping("/cuentas-modernas/{id}") public ResponseEntity<String> account(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
  return adapted(accounts.find(id,jwt.getTokenValue()),null,null,"cuentaId","clienteId","alias","tipo","saldo","moneda","estado","version");
 }

 @PostMapping("/cuentas-modernas") public ResponseEntity<String> open(@RequestHeader("Idempotency-Key") UUID key,@RequestBody String body,@AuthenticationPrincipal Jwt jwt) {
  return adapted(accounts.create(key,body,jwt.getTokenValue()),"cuentaId","/api/mobile/cuentas-modernas/","cuentaId","clienteId","alias","tipo","saldo","moneda","estado","version");
 }
 @GetMapping("/cuentas-modernas") public ResponseEntity<String> accounts(@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@AuthenticationPrincipal Jwt jwt) {
  return adapted(accounts.list(page,size,jwt.getTokenValue()),null,null,"cuentaId","alias","saldo","moneda","estado","version");
 }

 @PostMapping("/pagos") public ResponseEntity<String> pay(@RequestHeader("Idempotency-Key") UUID key,@RequestBody String body,@AuthenticationPrincipal Jwt jwt) {
  return adapted(payments.create(key,body,jwt.getTokenValue()),"pagoId","/api/mobile/pagos/","pagoId","tipo","monto","estado","motivo");
 }
 @GetMapping("/pagos/{id}") public ResponseEntity<String> payment(@PathVariable UUID id,@AuthenticationPrincipal Jwt jwt) {
  return adapted(payments.find(id,jwt.getTokenValue()),null,null,"pagoId","tipo","monto","estado","motivo");
 }
 private ResponseEntity<String> adapted(ResponseEntity<String> response,String idField,String location,String... fields) {
  if(!response.getStatusCode().is2xxSuccessful()) return ResponseEntity.status(response.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(response.getBody());
  JsonNode source=json.readerFor(JsonNode.class).with(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS).readValue(response.getBody());
  JsonNode output;
  if(source.isArray()) {
   ArrayNode array=json.createArrayNode(); for(JsonNode item:source) array.add(select(item,fields)); output=array;
  } else output=select(source,fields);
  var result=ResponseEntity.status(response.getStatusCode()).contentType(MediaType.APPLICATION_JSON);
  if(location!=null) result.location(URI.create(location+UUID.fromString(source.get(idField).asText())));
  return result.body(json.writeValueAsString(output));
 }
 private ObjectNode select(JsonNode source,String[] fields) {
  var result=json.createObjectNode(); for(String field:fields) if(source.has(field)) result.set(field,source.get(field)); return result;
 }
}
