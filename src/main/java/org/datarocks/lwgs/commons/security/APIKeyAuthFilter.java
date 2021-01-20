package org.datarocks.lwgs.commons.security;

import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

public class APIKeyAuthFilter extends AbstractPreAuthenticatedProcessingFilter {
  private static final String PREFIX_SEPARATOR = " ";

  private final String principalRequestHeader;
  private final String apiKeyPrefix;

  public APIKeyAuthFilter(String principalRequestHeader, String apiKeyPrefix) {
    this.principalRequestHeader = principalRequestHeader;
    this.apiKeyPrefix = apiKeyPrefix;
  }

  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest httpServletRequest) {
    return this.principalRequestHeader;
  }

  @Override
  protected Object getPreAuthenticatedCredentials(HttpServletRequest httpServletRequest) {
    String rawValue = httpServletRequest.getHeader(this.principalRequestHeader);

    if (rawValue == null) {
      return null;
    }

    rawValue = rawValue.trim();

    if (apiKeyPrefix.length() == 0) {
      return rawValue;
    } else if (rawValue.length() > apiKeyPrefix.length() + 1
        && rawValue.contains(PREFIX_SEPARATOR)) {
      final String[] value = rawValue.split(PREFIX_SEPARATOR, 2);

      final String prefix = value[0];
      final String token = value[1];

      if (prefix.equalsIgnoreCase(apiKeyPrefix)) {
        return token;
      }
    }

    return null;
  }
}
