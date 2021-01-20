package org.datarocks.lwgs.commons.sedex.model;

import static org.datarocks.lwgs.commons.sedex.model.SedexStatusCategory.*;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

public enum SedexStatus {
  MESSAGE_CORRECT_TRANSMITTED(SUCCESS, 100),
  INVALID_ENVELOPE_SYNTAX(MESSAGE_ERROR, 200),
  DUPLICATE_MESSAGE_ID(MESSAGE_ERROR, 201),
  NO_PAYLOAD_FOUND(MESSAGE_ERROR, 202),
  MESSAGE_TOO_OLD_TO_SEND(MESSAGE_ERROR, 203),
  MESSAGE_EXPIRED(MESSAGE_ERROR, 20),
  UNKNOWN_SENDER_ID(AUTHORIZATION_ERROR, 300),
  UNKNOWN_RECIPIENT_ID(AUTHORIZATION_ERROR, 301),
  UNKNOWN_PHYSICAL_SENDER_ID(AUTHORIZATION_ERROR, 302),
  INVALID_MESSAGE_TYPE(AUTHORIZATION_ERROR, 303),
  INVALID_MESSAGE_CLASS(AUTHORIZATION_ERROR, 304),
  NOT_ALLOWED_TO_SEND(AUTHORIZATION_ERROR, 310),
  NOT_ALLOWED_TO_RECEIVE(AUTHORIZATION_ERROR, 311),
  USER_CERTIFICATE_NOT_VALID(AUTHORIZATION_ERROR, 31),
  OTHER_RECIPIENTS_ARE_NOT_ALLOWED(AUTHORIZATION_ERROR, 313),
  MESSAGE_SIZE_EXCEEDS_LIMIT(AUTHORIZATION_ERROR, 330),
  NETWORK_ERROR(TRANSPORT_ERROR, 400),
  OSCI_HUB_NOT_REACHABLE(TRANSPORT_ERROR, 401),
  DIRECTORY_NOT_REACHABLE(TRANSPORT_ERROR, 402),
  LOGGING_SERVICE_NOT_REACHABLE(TRANSPORT_ERROR, 403),
  AUTHORIZATION_SERVICE_NOT_REACHABLE(TRANSPORT_ERROR, 404),
  INTERNAL_ERROR(ADAPTER_ERROR, 500),
  ERROR_DURING_RECEIVING(ADAPTER_ERROR, 501),
  MESSAGE_SUCCESSFULLY_SENT(PARTIAL_SUCCESS, 601),
  MESSAGE_EXPIRES_SOON(WARNING, 701);

  private static final Map<Integer, SedexStatus> map = new HashMap<>();

  static {
    for (SedexStatus status : SedexStatus.values()) {
      map.put(status.statusCode, status);
    }
  }

  @Getter private final SedexStatusCategory category;
  @Getter private final int statusCode;

  SedexStatus(SedexStatusCategory category, int statusCode) {
    this.category = category;
    this.statusCode = statusCode;
  }

  public static SedexStatus valueOf(int statusCode) {
    return map.get(statusCode);
  }
}
