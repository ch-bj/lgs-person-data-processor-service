package org.datarocks.lwgs.commons.sedex.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/* Example from documentation:

  sedex Client 6.0.9
  Installation and User Manual
  [do-e-00.09-sedex_client_handbuch_v6.pdf, p48]

  <?xml version="1.0" encoding="UTF-8"?>
  <eCH-0090:envelope version="1.0"
  xmlns:eCH-0090="http://www.ech.ch/xmlns/eCH-0090/1"
  xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://www.ech.ch/xmlns/eCH-0090/1/eCH-0090-1-0.xsd">
  <eCH-0090:messageId>TestMessageId</eCH-0090:messageId>
  <eCH-0090:messageType>Use case MessageType</eCH-0090:messageType>
  <eCH-0090:messageClass>0</eCH-0090:messageClass>
  <eCH-0090:senderId>Your sedex ID here</eCH-0090:senderId>
  <eCH-0090:recipientId>Your sedex ID here</eCH-0090:recipientId>
  <eCH-0090:eventDate>2019-12-02T11:30:00</eCH-0090:eventDate>
  <eCH-0090:messageDate>YYYY-MM-DDTHH:MM:SS</eCH-0090:messageDate>
  </eCH-0090:envelope>
*/

@Data
@Builder
@JacksonXmlRootElement(localName = "eCH-0090:envelope")
public class SedexEnvelope implements Serializable {
  private static final String NS = "http://www.ech.ch/xmlns/eCH-0090/1";

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true)
  String version = "1.0";

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true, localName = "xmlns:eCH-0090")
  String eCH0090 = NS;

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
  String xsi = "http://www.w3.org/2001/XMLSchema-instance";

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true, localName = "xsi:schemaLocation")
  String schemaLocation = "http://www.ech.ch/xmlns/eCH-0090/1/eCH-0090-1-0.xsd";

  @JacksonXmlProperty(localName = "eCH-0090:messageId")
  @NonNull
  String messageId;

  @JacksonXmlProperty(localName = "eCH-0090:messageType")
  @NonNull
  Integer messageType;

  @JacksonXmlProperty(localName = "eCH-0090:messageClass")
  @NonNull
  Integer messageClass;

  @JacksonXmlProperty(localName = "eCH-0090:referenceMessageId")
  String referenceMessageId;

  @JacksonXmlProperty(localName = "eCH-0090:senderId")
  @NonNull
  String senderId;

  @JacksonXmlProperty(localName = "eCH-0090:recipientId")
  @NonNull
  String recipientId;

  @JacksonXmlProperty(localName = "eCH-0090:eventDate")
  @NonNull
  Date eventDate;

  @JacksonXmlProperty(localName = "eCH-0090:messageDate")
  @NonNull
  Date messageDate;
}
