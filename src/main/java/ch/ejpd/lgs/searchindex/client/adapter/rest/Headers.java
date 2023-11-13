package ch.ejpd.lgs.searchindex.client.adapter.rest;

/**
 * Protected constructor to prevent instantiation as this class only contains static constants.
 */
public class Headers {
  protected Headers() {
    // nothing to do
  }

  /**
   * Constant representing the custom HTTP header for LGS sender ID.
   */
  public static final String X_LGS_SENDER_ID = "X-LGS-Sender-Id";
}
