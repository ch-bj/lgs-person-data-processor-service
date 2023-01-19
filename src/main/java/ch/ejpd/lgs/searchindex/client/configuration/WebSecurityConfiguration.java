package ch.ejpd.lgs.searchindex.client.configuration;

import ch.ejpd.lgs.commons.security.APIKeyAuthFilter;
import ch.ejpd.lgs.commons.security.APIKeyAuthenticationManager;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@Order(1)
public class WebSecurityConfiguration {
  @Value("${security.enable-csrf:true}")
  private boolean csrfEnabled;

  @Value("${lwgs.searchindex.client.security.auth.header}")
  private String apiKeyAuthHeader;

  @Value("${lwgs.searchindex.client.security.auth.prefix}")
  private String apiKeyPrefix;

  @Value("${lwgs.searchindex.client.security.auth.api-key}")
  private String apiKey;

  @Bean
  @SuppressWarnings({"squid:S4502"})
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    final APIKeyAuthFilter filter = new APIKeyAuthFilter(apiKeyAuthHeader, apiKeyPrefix);
    filter.setAuthenticationManager(
        new APIKeyAuthenticationManager(apiKeyAuthHeader, Collections.singletonList(apiKey)));

    http.cors()
        .and()
        .authorizeRequests()
        .antMatchers(HttpMethod.GET, "/swagger-ui.html")
        .permitAll()
        .antMatchers(HttpMethod.GET, "/swagger-ui/**")
        .permitAll()
        .antMatchers(HttpMethod.GET, "/api-docs/**")
        .permitAll()
        .antMatchers(HttpMethod.GET, "/actuator/**")
        .permitAll()
        .anyRequest()
        .authenticated()
        .and()
        .addFilter(filter)
        .sessionManagement()
        .sessionCreationPolicy(SessionCreationPolicy.STATELESS);

    if (!csrfEnabled) {
      http.csrf().disable();
    }

    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);

    // *** URL below needs to match the Vue client URL and port ***
    config.setAllowedOrigins(Collections.singletonList("http://localhost:8080"));
    config.setAllowedMethods(Collections.singletonList("*"));
    config.setAllowedHeaders(Collections.singletonList("*"));

    source.registerCorsConfiguration("/**", config);

    return source;
  }
}
