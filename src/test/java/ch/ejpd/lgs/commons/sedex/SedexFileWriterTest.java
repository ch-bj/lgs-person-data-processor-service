package ch.ejpd.lgs.commons.sedex;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.ejpd.lgs.commons.sedex.model.SedexEnvelope;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.model.JobCollectedPersonData;
import ch.ejpd.lgs.searchindex.client.model.JobMetaData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonData;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import ch.ejpd.lgs.searchindex.client.service.exception.WritingSedexFilesFailedException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SedexFileWriterTest {
  private static final String PERSON_DATA_PREFIX = "GBPersonEvent-";
  private static final String PERSON_DATA_SUFFIX = ".json";
  private static final String SENDER_ID_A = "LGS-123-AAA";
  private static final String EXPECTED_SENDER_ID_IN_METADATA = "\"landRegister\":\"LGS-123-AAA\"";
  private static final String TEST_DIR = "/tmp/lwgs-sedex-test";
  private static final String METADATA_FILE_NAME = "metadata.json";

  private final UUID messageId = UUID.randomUUID();
  private final SedexFileWriter sedexFileWriter = new SedexFileWriter(Paths.get(TEST_DIR), true);

  private final Path envelope = Paths.get(TEST_DIR, messageId + ".xml");
  private final Path payload = Paths.get(TEST_DIR, messageId + ".zip");

  @AfterEach
  void cleanup() {
    try {
      if (envelope.toFile().exists()) {
        Files.delete(envelope);
      }
      if (payload.toFile().exists()) {
        Files.delete(payload);
      }
    } catch (IOException exception) {
      exception.printStackTrace();
    }
  }

  @Test
  void writeSedexEnvelope() {
    assertDoesNotThrow(
        () ->
            sedexFileWriter.writeSedexEnvelope(
                messageId,
                SedexEnvelope.builder()
                    .messageId(messageId.toString())
                    .messageType(1)
                    .messageClass(2)
                    .messageDate(new Date())
                    .eventDate(new Date())
                    .senderId("sender-123")
                    .recipientId("recipient-123")
                    .build()));

    assertTrue(sedexFileWriter.sedexEnvelopeFile(messageId).exists());
  }

  @Test
  void writeSedexPayload() {
    final UUID transactionOne = UUID.randomUUID();
    final UUID transactionTwo = UUID.randomUUID();
    final UUID jobId = UUID.randomUUID();

    assertDoesNotThrow(
        () ->
            sedexFileWriter.writeSedexPayload(
                messageId,
                JobCollectedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .jobId(jobId)
                    .processedPersonDataList(
                        Arrays.asList(
                            ProcessedPersonData.builder()
                                .senderId(SENDER_ID_A)
                                .transactionId(transactionOne)
                                .payload("{}")
                                .build(),
                            ProcessedPersonData.builder()
                                .senderId(SENDER_ID_A)
                                .transactionId(transactionTwo)
                                .payload("{}")
                                .build()))
                    .messageId(messageId)
                    .page(0)
                    .build(),
                JobMetaData.builder()
                    .jobId(jobId)
                    .type(JobType.FULL)
                    .pageNr(0)
                    .isLastPage(true)
                    .landRegister(SENDER_ID_A)
                    .build()));

    assertTrue(sedexFileWriter.sedexDataFile(messageId).exists());

    List<String> files = getZipContentFileList(sedexFileWriter.sedexDataFile(messageId));

    assertNotNull(files);

    assertTrue(files.contains(PERSON_DATA_PREFIX + transactionOne + PERSON_DATA_SUFFIX));
    assertTrue(files.contains(PERSON_DATA_PREFIX + transactionTwo + PERSON_DATA_SUFFIX));
  }

  @Test
  void testLandRegionInSedexPayload() throws WritingSedexFilesFailedException {
    final UUID transactionId = UUID.randomUUID();
    final UUID jobId = UUID.randomUUID();

    sedexFileWriter.writeSedexPayload(
            messageId,
            JobCollectedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .jobId(jobId)
                    .processedPersonDataList(
                            Arrays.asList(
                                    ProcessedPersonData.builder()
                                            .senderId(SENDER_ID_A)
                                            .transactionId(transactionId)
                                            .payload("{}")
                                            .build()))
                    .messageId(messageId)
                    .page(0)
                    .build(),
            JobMetaData.builder()
                    .jobId(jobId)
                    .type(JobType.FULL)
                    .pageNr(0)
                    .isLastPage(true)
                    .landRegister(SENDER_ID_A)
                    .build());

    File file = sedexFileWriter.sedexDataFile(messageId);
    String content = getContentOfEntryInZip(METADATA_FILE_NAME, file);

    assertTrue(content.contains(EXPECTED_SENDER_ID_IN_METADATA));
  }

  private List<String> getZipContentFileList(File zipFile) {
    try {
      ZipFile zf = new ZipFile(zipFile);
      return zf.stream().map(ZipEntry::getName).collect(Collectors.toList());
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  private String getContentOfEntryInZip(String entryName, File zipFile) {
      try {
          ZipFile zf = new ZipFile(zipFile);
          ZipEntry entry = zf.getEntry(entryName);
          InputStream stream = zf.getInputStream(entry);

          return new BufferedReader(new InputStreamReader(stream))
                  .lines().parallel().collect(Collectors.joining("\n"));
      } catch (IOException e) {
          e.printStackTrace();
          throw new RuntimeException(e);
      }
  }
}
