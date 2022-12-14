package org.datarocks.lwgs.commons.sedex;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import org.datarocks.lwgs.commons.sedex.model.SedexReceipt;
import org.datarocks.lwgs.commons.sedex.model.SedexStatus;
import org.junit.jupiter.api.Test;

class SedexReceiptReaderTest {
  private final SedexReceiptReader receiptReader = new SedexReceiptReader();
  private static final int STATUS_CODE = 100;
  private static final String MESSAGE_ID = "62fdee70d9ea77646f6e8686a3f9332e";
  private static final String RAW_VALID_RECEIPT =
      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
          + "<receipt xmlns=\"http://www.ech.ch/xmlns/eCH-0090/2\"\n"
          + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
          + "xsi:schemaLocation=\"http://www.ech.ch/xmlns/eCH-0090/2 http://www.ech.ch/xmlns/eCH-\n"
          + "0090/1/eCH-0090-2-0.xsd\"\n"
          + "version=\"2.0\">\n"
          + "<eventDate>2008-10-16T14:13:51Z</eventDate>\n"
          + "<statusCode>"
          + STATUS_CODE
          + "</statusCode>\n"
          + "<statusInfo>Message correct transmitted</statusInfo>\n"
          + "<messageId>"
          + MESSAGE_ID
          + "</messageId>\n"
          + "<messageType>94</messageType>\n"
          + "<messageClass>0</messageClass>\n"
          + "<senderId>1-351-1</senderId>\n"
          + "<recipientId>3-CH-1</recipientId>\n"
          + "</receipt>";

  @Test
  void readFromString() {
    final Optional<SedexReceipt> receipt = receiptReader.readFromString(RAW_VALID_RECEIPT);

    assertTrue(receipt.isPresent());
    assertAll(
        () -> assertEquals(STATUS_CODE, receipt.get().getStatusCode()),
        () -> assertEquals(MESSAGE_ID, receipt.get().getMessageId()),
        () -> assertEquals(STATUS_CODE, receipt.get().getSedexStatus().getStatusCode()),
        () ->
            assertEquals(SedexStatus.MESSAGE_CORRECT_TRANSMITTED, receipt.get().getSedexStatus()));
  }

  @Test
  void readFromFile() {
    final Path testFile = getResourceAsPath("sedex/receipt.xml");
    assertNotNull(testFile);

    final Optional<SedexReceipt> receipt = receiptReader.readFromFile(testFile);

    assertTrue(receipt.isPresent());
    assertAll(
        () -> assertEquals(STATUS_CODE, receipt.get().getStatusCode()),
        () -> assertEquals(MESSAGE_ID, receipt.get().getMessageId()));
  }

  @Test
  void failOnMalformedXML() {
    final Path testFile = getResourceAsPath("sedex/receipt-invalid.xml");
    assertNotNull(testFile);

    final Optional<SedexReceipt> receipt = receiptReader.readFromFile(testFile);

    assertFalse(receipt.isPresent());
  }

  private Path getResourceAsPath(String resourceString) {
    ClassLoader classLoader = getClass().getClassLoader();
    URL resourceUrl = classLoader.getResource(resourceString);

    if (resourceUrl == null) {
      return null;
    }
    return Paths.get(resourceUrl.getPath());
  }
}
