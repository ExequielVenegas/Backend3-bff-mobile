package cl.duoc.bancoxyz.bff.common.client;
import cl.duoc.bancoxyz.bff.common.exception.ResourceNotFoundException;
import io.github.resilience4j.circuitbreaker.*;
import io.github.resilience4j.retry.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
@Component
public class AccountServiceClient {
 private final RestClient client;
 private final CircuitBreaker circuit=CircuitBreaker.of("legacy-account",CircuitBreakerConfig.custom().slidingWindowSize(8).minimumNumberOfCalls(4)
  .failureRateThreshold(50).waitDurationInOpenState(java.time.Duration.ofSeconds(10)).ignoreExceptions(ResourceNotFoundException.class,HttpClientErrorException.class).build());
 private final Retry retry=Retry.of("legacy-account-read",RetryConfig.custom().maxAttempts(2).waitDuration(java.time.Duration.ofMillis(200))
  .retryOnException(e->e instanceof ResourceAccessException || e instanceof HttpServerErrorException).build());
 public AccountServiceClient(@Qualifier("accountRestClientBuilder") RestClient.Builder builder,@Value("${services.account.base-url}") String url) { client=builder.clone().baseUrl(url).build(); }
 public AccountServiceResponse findAccount(Integer id) {
  try {
   return CircuitBreaker.decorateSupplier(circuit,Retry.decorateSupplier(retry,()->client.get().uri("/internal/accounts/{id}/summary",id).retrieve()
    .onStatus(s->s.value()==404,(req,res)->{throw new ResourceNotFoundException("No existe la cuenta "+id);}).body(AccountServiceResponse.class))).get();
  } catch(RestClientException|CallNotPermittedException e) {
   throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Account temporalmente no disponible");
  } catch(IllegalStateException e) {
   if("No instances available for account-service".equals(e.getMessage())) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Account temporalmente no disponible");
   throw e;
  }
 }
}
