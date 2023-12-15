package ch.ejpd.lgs.commons.sedex;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ch.ejpd.lgs.commons.sedex.model.SedexEnvelope;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.model.JobCollectedPersonData;
import ch.ejpd.lgs.searchindex.client.model.JobMetaData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonData;
import ch.ejpd.lgs.searchindex.client.service.exception.WritingSedexFilesFailedException;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class SedexFileWriterTest {
  private static final String PERSON_DATA_PREFIX = "GBPersonEvent-";
  private static final String PERSON_DATA_SUFFIX = ".json";
  private static final String SENDER_ID_A = "LGS-123-AAA";
  private static final String EXPECTED_SENDER_ID_IN_METADATA = "\"landRegister\":\"LGS-123-AAA\"";
  private static final String EXPECTED_METADATE_LAND_REGISTER_1 = "\"landRegister\":\"LandReg-001\"";
  private static final String EXPECTED_METADATE_LAND_REGISTER_2 = "\"landRegister\":\"LandReg-002\"";
  private static final String LAND_REGISTER_KEY = "\"landRegister\"";
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
  void testLandRegionsInSedexPayload() throws WritingSedexFilesFailedException {
    final UUID jobId = UUID.randomUUID();
    UUID noLandRegisterPerson1 = UUID.randomUUID();
    UUID noLandRegisterPerson2 = UUID.randomUUID();
    UUID landRegister1Person1 = UUID.randomUUID();
    UUID landRegister1Person2 = UUID.randomUUID();
    UUID landRegister1Person3 = UUID.randomUUID();
    UUID landRegister2Person1 = UUID.randomUUID();
    UUID landRegister2Person2 = UUID.randomUUID();

    String landRegister1 = "LandReg-001";
    String landRegister2 = "LandReg-002";

    // TODO kiril add transactions ids
    List<ProcessedPersonData> personData = Arrays.asList(
            ProcessedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .landRegister(null)
                    .transactionId(noLandRegisterPerson1)
                    .payload("{}")
                    .build(),
            ProcessedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .landRegister(null)
                    .transactionId(noLandRegisterPerson2)
                    .payload("{}")
                    .build(),
            ProcessedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .landRegister(landRegister1)
                    .transactionId(landRegister1Person1)
                    .payload("{}")
                    .build(),
            ProcessedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .landRegister(landRegister1)
                    .transactionId(landRegister1Person2)
                    .payload("{}")
                    .build(),
            ProcessedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .landRegister(landRegister1)
                    .transactionId(landRegister1Person3)
                    .payload("{}")
                    .build(),
            ProcessedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .landRegister(landRegister2)
                    .transactionId(landRegister2Person1)
                    .payload("{}")
                    .build(),
            ProcessedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .landRegister(landRegister2)
                    .transactionId(landRegister2Person2)
                    .payload("{}")
                    .build()
    );

    sedexFileWriter.writeSedexPayloadIntoMultipleFiles(
            messageId,
            JobCollectedPersonData.builder()
                    .senderId(SENDER_ID_A)
                    .jobId(jobId)
                    .processedPersonDataList(personData)
                    .messageId(messageId)
                    .page(0)
                    .build(),
            JobMetaData.builder()
                    .jobId(jobId)
                    .type(JobType.FULL)
                    .pageNr(0)
                    .isLastPage(true)
                    .build());

    File fileWithNoLandRegister = sedexFileWriter.sedexDataFile(messageId);
    String content = getContentOfEntryInZip(METADATA_FILE_NAME, fileWithNoLandRegister);
    assertFalse(content.contains(LAND_REGISTER_KEY));
    List<String> filesNoLR = getZipContentFileList(sedexFileWriter.sedexDataFile(messageId));
    assertNotNull(filesNoLR);
    assertTrue(filesNoLR.contains(PERSON_DATA_PREFIX + noLandRegisterPerson1 + PERSON_DATA_SUFFIX));
    assertTrue(filesNoLR.contains(PERSON_DATA_PREFIX + noLandRegisterPerson2 + PERSON_DATA_SUFFIX));

    File fileForLandRegister1 = sedexFileWriter.sedexDataFile(landRegister1, messageId);
    String contentLR1 = getContentOfEntryInZip(METADATA_FILE_NAME, fileForLandRegister1);
    assertTrue(contentLR1.contains(EXPECTED_METADATE_LAND_REGISTER_1));
    List<String> filesLR1 = getZipContentFileList(sedexFileWriter.sedexDataFile(landRegister1, messageId));
    assertNotNull(filesLR1);
    assertTrue(filesLR1.contains(PERSON_DATA_PREFIX + landRegister1Person1 + PERSON_DATA_SUFFIX));
    assertTrue(filesLR1.contains(PERSON_DATA_PREFIX + landRegister1Person2 + PERSON_DATA_SUFFIX));
    assertTrue(filesLR1.contains(PERSON_DATA_PREFIX + landRegister1Person3 + PERSON_DATA_SUFFIX));

    File fileForLandRegister2 = sedexFileWriter.sedexDataFile(landRegister2, messageId);
    String contentLR2 = getContentOfEntryInZip(METADATA_FILE_NAME, fileForLandRegister2);
    assertTrue(contentLR2.contains(EXPECTED_METADATE_LAND_REGISTER_2));
    List<String> filesLR2 = getZipContentFileList(sedexFileWriter.sedexDataFile(landRegister2, messageId));
    assertNotNull(filesLR2);
    assertTrue(filesLR2.contains(PERSON_DATA_PREFIX + landRegister2Person1 + PERSON_DATA_SUFFIX));
    assertTrue(filesLR2.contains(PERSON_DATA_PREFIX + landRegister2Person2 + PERSON_DATA_SUFFIX));
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
          .lines()
          .parallel()
          .collect(Collectors.joining("\n"));
    } catch (IOException e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }

  // TODO kiril add tests

}
