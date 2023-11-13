package ch.ejpd.lgs.searchindex.client.service.sedex;

/**
 * Interface for handling throttling updates.
 * Implementing classes should provide a mechanism to update throttling status.
 */
public interface ThrottleHandler {

  /**
   * Update the throttling status.
   *
   * @param active True if throttling is active, false otherwise.
   */
  void updateThrottling(boolean active);
}
