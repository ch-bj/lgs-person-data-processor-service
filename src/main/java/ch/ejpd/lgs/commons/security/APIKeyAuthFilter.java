package ch.ejpd.lgs.commons.security;

import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

/**
 * Custom Spring Security filter for processing API key-based authentication.
 */
public class APIKeyAuthFilter extends AbstractPreAuthenticatedProcessingFilter {
  private static final String PREFIX_SEPARATOR = " ";

  private final String principalRequestHeader;
  private final String apiKeyPrefix;

  /**
   * Constructs an APIKeyAuthFilter with the specified API key header and prefix.
   *
   * @param principalRequestHeader The header containing the API key.
   * @param apiKeyPrefix           The expected prefix in the API key header.
   */
  public APIKeyAuthFilter(String principalRequestHeader, String apiKeyPrefix) {
    this.principalRequestHeader = principalRequestHeader;
    this.apiKeyPrefix = apiKeyPrefix;
  }

  /**
   * Retrieves the principal (API key header) from the provided HttpServletRequest.
   *
   * @param httpServletRequest The HttpServletRequest containing the API key header.
   * @return The API key header as the principal.
   */
  @Override
  protected Object getPreAuthenticatedPrincipal(HttpServletRequest httpServletRequest) {
    return this.principalRequestHeader;
  }

  /**
   * Retrieves the credentials (API key token) from the provided HttpServletRequest.
   *
   * @param httpServletRequest The HttpServletRequest containing the API key header and token.
   * @return The API key token as the credentials.
   */
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
