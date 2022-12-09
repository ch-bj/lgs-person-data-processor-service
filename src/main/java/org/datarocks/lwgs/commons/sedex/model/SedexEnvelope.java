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
@JacksonXmlRootElement(namespace = "http://www.ech.ch/xmlns/eCH-0090/2", localName = "envelope")
public class SedexEnvelope implements Serializable {
  private static final String NS = "http://www.ech.ch/xmlns/eCH-0090/2";

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true)
  String version = "2.0";

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true, localName = "xsi:schemaLocation")
  String schemaLocation =
      "http://www.ech.ch/xmlns/eCH-0090/2 http://www.ech.ch/xmlns/eCH-0090/2/eCH-0090-2-0.xsd";

  @Builder.Default
  @JacksonXmlProperty(isAttribute = true, localName = "xmlns:xsi")
  String xsi = "http://www.w3.org/2001/XMLSchema-instance";

  @JacksonXmlProperty(namespace = NS)
  @NonNull
  String messageId;

  @JacksonXmlProperty(namespace = NS)
  @NonNull
  Integer messageType;

  @JacksonXmlProperty(namespace = NS)
  @NonNull
  Integer messageClass;

  @JacksonXmlProperty(namespace = NS)
  String referenceMessageId;

  @JacksonXmlProperty(namespace = NS)
  @NonNull
  String senderId;

  @JacksonXmlProperty(namespace = NS)
  @NonNull
  String recipientId;

  @JacksonXmlProperty(namespace = NS)
  @NonNull
  Date eventDate;

  @JacksonXmlProperty(namespace = NS)
  @NonNull
  Date messageDate;
}
