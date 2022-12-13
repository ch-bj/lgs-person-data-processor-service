package org.datarocks.lwgs.commons.sedex.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.io.Serializable;
import java.util.Date;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

/* Example from documentation
   <?xml version="1.0" encoding="UTF-8"?>
   <envelope xmlns="http://www.ech.ch/xmlns/eCH-0090/1"
   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
   xsi:schemaLocation="http://www.ech.ch/xmlns/eCH-0090/1 http://www.ech.ch/xmlns/eCH-
   0090/1/eCH-0090-1-0.xsd"
   version="1.0">
   <messageId>62fdee70d9ea77646f6e8686a3f9332e</messageId>
   <messageType>99</messageType>
   <messageClass>0</messageClass>
   <senderId>1-351-1</senderId>
   <recipientId>3-CH-1</recipientId>
   <eventDate>2007-01-01T00:00:00</eventDate>
   <messageDate>2007-09-06T14:13:51</messageDate>
   </envelope>
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
  String eCH0090 = "http://www.ech.ch/xmlns/eCH-0090/1";

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
