package org.datarocks.lwgs.commons.security;

import java.util.List;
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;

public class APIKeyAuthenticationManager implements AuthenticationManager {
  private final String principalRequestHeader;
  private final List<String> apiKeys;

  public APIKeyAuthenticationManager(String principalRequestHeader, List<String> apiKeys) {
    this.principalRequestHeader = principalRequestHeader;
    this.apiKeys = apiKeys;
  }

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
