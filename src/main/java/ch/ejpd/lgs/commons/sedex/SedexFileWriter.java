package ch.ejpd.lgs.commons.sedex;

import static com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator.Feature.WRITE_XML_DECLARATION;

import ch.ejpd.lgs.commons.sedex.model.SedexEnvelope;
import ch.ejpd.lgs.searchindex.client.model.JobCollectedPersonData;
import ch.ejpd.lgs.searchindex.client.model.JobMetaData;
import ch.ejpd.lgs.searchindex.client.model.ProcessedPersonData;
import ch.ejpd.lgs.searchindex.client.service.exception.WritingSedexFilesFailedException;
import com.ctc.wstx.stax.WstxInputFactory;
import com.ctc.wstx.stax.WstxOutputFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

// ... (Several methods within the SedexFileWriter class not commented for brevity)
@Slf4j
public class SedexFileWriter {
  private final Path sedexOutboxPath;
  private final boolean createDirectories;
  private final XmlMapper xmlMapper;

  private final Gson gson = new GsonBuilder().create();

  /**
   * Constructor for SedexFileWriter.
   *
   * @param sedexOutboxPath   The path to the Sedex outbox directory.
   * @param createDirectories Flag indicating whether to create directories if they don't exist.
   */
  public SedexFileWriter(final Path sedexOutboxPath, boolean createDirectories) {
    this.sedexOutboxPath = sedexOutboxPath;
    this.createDirectories = createDirectories;
    XMLInputFactory xmlInputFactory = new WstxInputFactory();
    xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
    XMLOutputFactory xmlOutputFactory = new WstxOutputFactory();
    xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, false);
    this.xmlMapper = new XmlMapper(xmlInputFactory, xmlOutputFactory);
    this.xmlMapper.enable(SerializationFeature.INDENT_OUTPUT);
    this.xmlMapper.configure(WRITE_XML_DECLARATION, true);
    this.xmlMapper.setDateFormat(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"));
    this.xmlMapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
  }

  private void setupOutputFile(final File file) throws WritingSedexFilesFailedException {
    final File parentDir = file.getParentFile();
    if (createDirectories && !parentDir.exists() && !parentDir.mkdirs()) {
      throw new WritingSedexFilesFailedException(
          WritingSedexFilesFailedException.FailureCause.DIRECTORY_CREATION_FAILED);
    }
    try {
      Files.createFile(file.toPath());
    } catch (IOException exception) {
      throw new WritingSedexFilesFailedException(
          WritingSedexFilesFailedException.FailureCause.FILE_CREATION_FAILED);
    }
  }

  private void cleanupOutputFile(final File file) {
    try {
      Files.delete(file.toPath());
    } catch (Exception cleanupException) {
      log.warn("Cleanup of broken sedex payload failed: ", cleanupException);
    }
  }

  protected File sedexEnvelopeFile(@NonNull final UUID fileIdentifier) {
    return sedexOutboxPath.resolve("envl_" + fileIdentifier + ".xml").toFile();
  }

  protected File sedexDataFile(@NonNull final UUID fileIdentifier) {
    return sedexOutboxPath.resolve("data_" + fileIdentifier + ".zip").toFile();
  }

  /**
   * Writes a Sedex envelope to a file.
   *
   * @param fileIdentifier The unique identifier for the Sedex file.
   * @param sedexEnvelope  The SedexEnvelope object to be written.
   * @throws WritingSedexFilesFailedException If writing the Sedex envelope fails.
   */
  public void writeSedexEnvelope(
      @NonNull final UUID fileIdentifier, @NonNull final SedexEnvelope sedexEnvelope)
      throws WritingSedexFilesFailedException {
    final File sedexEnvelopeFile = sedexEnvelopeFile(fileIdentifier);

    setupOutputFile(sedexEnvelopeFile);

    try (FileOutputStream fileOutputStream = new FileOutputStream(sedexEnvelopeFile)) {
      xmlMapper.writeValue(fileOutputStream, sedexEnvelope);
    } catch (Exception e) {
      log.error("Error writing sedex envelope file: {}.", e.getMessage());
      cleanupOutputFile(sedexEnvelopeFile);
      throw new WritingSedexFilesFailedException(
          WritingSedexFilesFailedException.FailureCause.FILE_WRITE_FAILED);
    }
  }

  public void writeSedexPayload(
      @NonNull final UUID fileIdentifier,
      @NonNull final JobCollectedPersonData jobCollectedPersonData,
      @NonNull final JobMetaData metaData)
      throws WritingSedexFilesFailedException {
    final File sedexPayloadFile = sedexDataFile(fileIdentifier);
    final Set<UUID> processedTransactions = new HashSet<>();

    setupOutputFile(sedexPayloadFile);

    try (FileOutputStream fileOutputStream = new FileOutputStream(sedexPayloadFile)) {
      try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
        zipOutputStream.putNextEntry(new ZipEntry("metadata.json"));
        zipOutputStream.write(gson.toJson(metaData).getBytes());

        for (ProcessedPersonData processedPersonData :
            jobCollectedPersonData.getProcessedPersonDataList()) {
          if (!processedTransactions.contains(processedPersonData.getTransactionId())) {
            zipOutputStream.putNextEntry(
                new ZipEntry("GBPersonEvent-" + processedPersonData.getTransactionId() + ".json"));
            zipOutputStream.write(processedPersonData.getPayload().getBytes());
            processedTransactions.add(processedPersonData.getTransactionId());
          } else {
            // This should never happen, but managed to produce this with restarting service during
            // processing of messages.
            log.error(
                "Duplicate transactionId found: {}. Skipping processedPersonData.",
                processedPersonData.getTransactionId());
          }
        }
      }
    } catch (Exception e) {
      log.error("Error writing sedex payload file: {}.", e.getMessage());
      cleanupOutputFile(sedexPayloadFile);
      throw new WritingSedexFilesFailedException(
          WritingSedexFilesFailedException.FailureCause.FILE_WRITE_FAILED);
    }
  }
}
