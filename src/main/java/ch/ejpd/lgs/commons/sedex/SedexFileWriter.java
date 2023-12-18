package ch.ejpd.lgs.commons.sedex;

import static com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator.Feature.WRITE_XML_DECLARATION;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.Strings;

@Slf4j
public class SedexFileWriter {
  private final Path sedexOutboxPath;
  private final boolean createDirectories;
  private final XmlMapper xmlMapper;

  private final Gson gson = new GsonBuilder().create();

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

  protected File sedexEnvelopeFile(final String subdirectory, @NonNull final UUID fileIdentifier) {
    return Strings.isBlank(subdirectory)
        ? sedexOutboxPath.resolve("envl_" + fileIdentifier + ".xml").toFile()
        : sedexOutboxPath.resolve(subdirectory).resolve("envl_" + fileIdentifier + ".xml").toFile();
  }

  protected File sedexDataFile(final String subdirectory, @NonNull final UUID fileIdentifier) {
    return Strings.isBlank(subdirectory)
        ? sedexOutboxPath.resolve("data_" + fileIdentifier + ".zip").toFile()
        : sedexOutboxPath.resolve(subdirectory).resolve("data_" + fileIdentifier + ".zip").toFile();
  }

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

  public void writeSedexPayloadIntoMultipleFiles(
      @NonNull final UUID fileIdentifier,
      @NonNull final JobCollectedPersonData jobCollectedPersonData,
      @NonNull final JobMetaData metaData)
      throws WritingSedexFilesFailedException {
    Map<String, List<ProcessedPersonData>> personalDataByLandRegister =
        jobCollectedPersonData.getProcessedPersonDataList().stream()
            .collect(groupingBy(ProcessedPersonData::getLandRegisterSafely, toList()));

    final Set<UUID> processedTransactions = new HashSet<>();

    for (Map.Entry<String, List<ProcessedPersonData>> entry :
        personalDataByLandRegister.entrySet()) {
      final File sedexPayloadFile = sedexDataFile(entry.getKey(), fileIdentifier);
      log.info(
          "Start writing sedex payload file {} [messageId: {}, senderId: {}, landRegister: {}]",
          sedexPayloadFile.getAbsoluteFile(),
          metaData.getJobId().toString(),
          jobCollectedPersonData.getSenderId(),
          entry.getKey());
      if (!Strings.isBlank(entry.getKey())) {
        metaData.setLandRegister(entry.getKey());
      }

      setupOutputFile(sedexPayloadFile);

      try (FileOutputStream fileOutputStream = new FileOutputStream(sedexPayloadFile)) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream)) {
          zipOutputStream.putNextEntry(new ZipEntry("metadata.json"));
          zipOutputStream.write(gson.toJson(metaData).getBytes());

          for (ProcessedPersonData processedPersonData : entry.getValue()) {

            if (!processedTransactions.contains(processedPersonData.getTransactionId())) {
              zipOutputStream.putNextEntry(
                  new ZipEntry(
                      "GBPersonEvent-" + processedPersonData.getTransactionId() + ".json"));
              zipOutputStream.write(processedPersonData.getPayload().getBytes());
              processedTransactions.add(processedPersonData.getTransactionId());

            } else {
              // This should never happen, but managed to produce this with restarting service
              // during
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

  public void writeSedexEnvelopeIntoMultipleFiles(
      @NonNull final JobCollectedPersonData jobCollectedPersonData,
      @NonNull final UUID fileIdentifier,
      @NonNull final SedexEnvelope sedexEnvelope)
      throws WritingSedexFilesFailedException {
    Set<String> landRegisters =
        jobCollectedPersonData.getProcessedPersonDataList().stream()
            .map(ProcessedPersonData::getLandRegisterSafely)
            .collect(Collectors.toSet());

    for (String landRegister : landRegisters) {
      final File sedexEnvelopeFile = sedexEnvelopeFile(landRegister, fileIdentifier);

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
  }
}
