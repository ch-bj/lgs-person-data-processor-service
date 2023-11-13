package ch.ejpd.lgs.commons.sedex.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

/**
 * Represents a Sedex receipt, providing information about the status of a Sedex message.
 */
@Data
@NoArgsConstructor
@JacksonXmlRootElement(namespace = "http://www.ech.ch/xmlns/eCH-0090/2", localName = "receipt")
public class SedexReceipt implements Serializable {
  @NonNull Date eventDate;
  @NonNull Integer statusCode;
  @NonNull String statusInfo;
  @NonNull String messageId;
  @NonNull String messageType;
  @NonNull String messageClass;
  @NonNull String senderId;
  @NonNull String recipientId;

  /**
   * Gets the corresponding SedexStatus based on the statusCode.
   *
   * @return The SedexStatus enum corresponding to the statusCode.
   */
  @JsonIgnore
  public SedexStatus getSedexStatus() {
    return SedexStatus.valueOf(statusCode);
  }
}
