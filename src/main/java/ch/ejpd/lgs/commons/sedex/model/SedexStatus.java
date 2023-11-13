package ch.ejpd.lgs.commons.sedex.model;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Enum representing different status codes for Sedex messages.
 */
public enum SedexStatus {

  // SUCCESS
  MESSAGE_CORRECT_TRANSMITTED(SedexStatusCategory.SUCCESS, 100),

  // MESSAGE ERROR
  INVALID_ENVELOPE_SYNTAX(SedexStatusCategory.MESSAGE_ERROR, 200),
  DUPLICATE_MESSAGE_ID(SedexStatusCategory.MESSAGE_ERROR, 201),
  NO_PAYLOAD_FOUND(SedexStatusCategory.MESSAGE_ERROR, 202),
  MESSAGE_TOO_OLD_TO_SEND(SedexStatusCategory.MESSAGE_ERROR, 203),
  MESSAGE_EXPIRED(SedexStatusCategory.MESSAGE_ERROR, 20),

  // AUTHORIZATION ERROR
  UNKNOWN_SENDER_ID(SedexStatusCategory.AUTHORIZATION_ERROR, 300),
  UNKNOWN_RECIPIENT_ID(SedexStatusCategory.AUTHORIZATION_ERROR, 301),
  UNKNOWN_PHYSICAL_SENDER_ID(SedexStatusCategory.AUTHORIZATION_ERROR, 302),
  INVALID_MESSAGE_TYPE(SedexStatusCategory.AUTHORIZATION_ERROR, 303),
  INVALID_MESSAGE_CLASS(SedexStatusCategory.AUTHORIZATION_ERROR, 304),
  NOT_ALLOWED_TO_SEND(SedexStatusCategory.AUTHORIZATION_ERROR, 310),
  NOT_ALLOWED_TO_RECEIVE(SedexStatusCategory.AUTHORIZATION_ERROR, 311),
  USER_CERTIFICATE_NOT_VALID(SedexStatusCategory.AUTHORIZATION_ERROR, 31),
  OTHER_RECIPIENTS_ARE_NOT_ALLOWED(SedexStatusCategory.AUTHORIZATION_ERROR, 313),
  MESSAGE_SIZE_EXCEEDS_LIMIT(SedexStatusCategory.AUTHORIZATION_ERROR, 330),

  // TRANSPORT ERROR
  NETWORK_ERROR(SedexStatusCategory.TRANSPORT_ERROR, 400),
  OSCI_HUB_NOT_REACHABLE(SedexStatusCategory.TRANSPORT_ERROR, 401),
  DIRECTORY_NOT_REACHABLE(SedexStatusCategory.TRANSPORT_ERROR, 402),
  LOGGING_SERVICE_NOT_REACHABLE(SedexStatusCategory.TRANSPORT_ERROR, 403),
  AUTHORIZATION_SERVICE_NOT_REACHABLE(SedexStatusCategory.TRANSPORT_ERROR, 404),

  // ADAPTER ERROR
  INTERNAL_ERROR(SedexStatusCategory.ADAPTER_ERROR, 500),
  ERROR_DURING_RECEIVING(SedexStatusCategory.ADAPTER_ERROR, 501),

  // PARTIAL SUCCESS
  MESSAGE_SUCCESSFULLY_SENT(SedexStatusCategory.PARTIAL_SUCCESS, 601),

  // WARNING
  MESSAGE_EXPIRES_SOON(SedexStatusCategory.WARNING, 701);

  private static final Map<Integer, SedexStatus> map = new HashMap<>();

  static {
    for (SedexStatus status : SedexStatus.values()) {
      map.put(status.statusCode, status);
    }
  }

  @Getter private final SedexStatusCategory category;
  @Getter private final int statusCode;

  /**
   * Constructs a SedexStatus with the specified category and status code.
   *
   * @param category   The category of the Sedex status.
   * @param statusCode The numerical status code.
   */
  SedexStatus(SedexStatusCategory category, int statusCode) {
    this.category = category;
    this.statusCode = statusCode;
  }

  /**
   * Retrieves the SedexStatus enum corresponding to the given status code.
   *
   * @param statusCode The numerical status code.
   * @return The SedexStatus enum corresponding to the status code.
   */
  public static SedexStatus valueOf(int statusCode) {
    return map.get(statusCode);
  }
}
