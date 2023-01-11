package org.datarocks.lwgs.commons.sedex.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

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

  @JsonIgnore
  public SedexStatus getSedexStatus() {
    return SedexStatus.valueOf(statusCode);
  }
}
