package org.datarocks.lwgs.commons.sedex;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
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
import org.datarocks.lwgs.commons.sedex.model.SedexEnvelope;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;
import org.datarocks.lwgs.searchindex.client.model.JobCollectedPersonData;
import org.datarocks.lwgs.searchindex.client.model.JobMetaData;
import org.datarocks.lwgs.searchindex.client.model.ProcessedPersonData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.Job;

class SedexFileWriterTest {
  private static final String PERSON_DATA_PREFIX = "GBPersonEvent-";
  private static final String PERSON_DATA_SUFFIX = ".json";

  private static final String TEST_DIR = "/tmp/lwgs-sedex-test";

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
                    .jobId(jobId)
                    .processedPersonDataList(
                        Arrays.asList(
                            ProcessedPersonData.builder()
                                .transactionId(transactionOne)
                                .payload("{}")
                                .build(),
                            ProcessedPersonData.builder()
                                .transactionId(transactionTwo)
                                .payload("{}")
                                .build()))
                        .messageId(messageId)
                        .page(0)
                    .build(),
                    new JobMetaData(JobType.FULL, jobId, 0, true)
            )
            );

    assertTrue(sedexFileWriter.sedexDataFile(messageId).exists());

    List<String> files = getZipContentFileList(sedexFileWriter.sedexDataFile(messageId));

    assertNotNull(files);

    assertTrue(files.contains(PERSON_DATA_PREFIX + transactionOne + PERSON_DATA_SUFFIX));
    assertTrue(files.contains(PERSON_DATA_PREFIX + transactionTwo + PERSON_DATA_SUFFIX));
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
}
