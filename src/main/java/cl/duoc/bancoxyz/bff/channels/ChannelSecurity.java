package cl.duoc.bancoxyz.bff.channels;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
@Configuration(proxyBeanMethods=false)
public class ChannelSecurity {
 @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
  return http.csrf(AbstractHttpConfigurer::disable).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/actuator/health").permitAll()
    .requestMatchers(HttpMethod.GET,"/api/mobile/clientes/*").hasAuthority("SCOPE_mobile.customers.read")
    .requestMatchers(HttpMethod.GET,"/api/mobile/cuentas-modernas","/api/mobile/cuentas-modernas/**").hasAuthority("SCOPE_mobile.accounts.read")
    .requestMatchers(HttpMethod.POST,"/api/mobile/cuentas-modernas").hasAuthority("SCOPE_mobile.accounts.write")
    .requestMatchers(HttpMethod.POST,"/api/mobile/pagos").hasAuthority("SCOPE_mobile.payments.write")
    .requestMatchers(HttpMethod.GET,"/api/mobile/pagos/*").hasAuthority("SCOPE_mobile.payments.read")
    .requestMatchers(HttpMethod.GET,"/api/mobile/cuentas/**").hasAuthority("SCOPE_mobile.read")
    .requestMatchers(HttpMethod.GET,"/actuator/info").hasAuthority("SCOPE_mobile.info")
    .anyRequest().denyAll()).oauth2ResourceServer(o->o.jwt(Customizer.withDefaults())).build();
 }
}
