package ch.ejpd.lgs.searchindex.client.util;

import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import ch.ejpd.lgs.searchindex.client.service.exception.SenderIdValidationException;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Getter
@Setter
public class SenderUtil {
  private final boolean isInMultiSenderMode;
  private final Set<String> validSenderIds;
  private final String singleSenderId;
  private static final String SENDER_ID_REGEX = "[a-zA-Z0-9-_]*";

  public SenderUtil(SedexConfiguration configuration) {
    this.singleSenderId = configuration.getSedexSenderId();
    this.isInMultiSenderMode = configuration.isInMultiSenderMode();
    this.validSenderIds =
        this.isInMultiSenderMode ? configuration.getSedexSenderIds() : Set.of(this.singleSenderId);
  }

  public String getSenderId(final String senderId) {
    if (!isInMultiSenderMode) {
      return singleSenderId;
    }

    if (isInMultiSenderMode && senderId != null && validSenderIds.contains(senderId)) {
      return senderId;
    }

    throw new SenderIdValidationException(
        String.format(
            "Validation of senderId failed, given senderId %s, valid senderId(s): %s.",
            senderId, validSenderIds));
  }

  public String getLandRegister(final String senderId) {
    if (isInMultiSenderMode) {
      return null;
    }

    return senderId;
  }

  public void validate(final String senderId) {
    if (senderId == null) {
      return;
    }

    if (!senderId.matches(SENDER_ID_REGEX)) {
      throw new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Invalid X-LGS-Sender-Id header. Valid characters: A-Z, a-z, -, _");
    }
  }
}
