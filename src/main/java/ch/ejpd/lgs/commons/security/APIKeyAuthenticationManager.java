package ch.ejpd.lgs.commons.security;

import java.util.List;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

/**
 * Custom implementation of Spring Security's AuthenticationManager for API key-based authentication.
 */
public class APIKeyAuthenticationManager implements AuthenticationManager {
  private final String principalRequestHeader;
  private final List<String> apiKeys;

  /**
   * Constructs an APIKeyAuthenticationManager with the specified header and list of API keys.
   *
   * @param principalRequestHeader The header containing the API key.
   * @param apiKeys                The list of valid API keys.
   */
  public APIKeyAuthenticationManager(String principalRequestHeader, List<String> apiKeys) {
    this.principalRequestHeader = principalRequestHeader;
    this.apiKeys = apiKeys;
  }

  /**
   * Authenticates the provided Authentication object based on API key validation.
   *
   * @param authentication The Authentication object containing principal (API key header) and credentials (API key).
   * @return The authenticated object if the API key is valid.
   * @throws AccountExpiredException   If the API key is not set.
   * @throws BadCredentialsException   If the API key does not match any of the valid keys.
   */
  @Override
  public Authentication authenticate(Authentication authentication) {
    final String principal = (String) authentication.getPrincipal();
    final String apiKey = (String) authentication.getCredentials();

    if (principal == null || apiKey == null) {
      throw new AccountExpiredException("Api Key not set");
    }

    if (principal.equals(principalRequestHeader) && apiKeys.contains(apiKey)) {
      authentication.setAuthenticated(true);
      return authentication;
    }

    throw new BadCredentialsException("API Key not matching");
  }
}
