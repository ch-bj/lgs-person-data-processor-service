package org.datarocks.lwgs.commons.sedex;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.lwgs.commons.sedex.model.SedexReceipt;

@Slf4j
public class SedexReceiptReader {
  private final XmlMapper mapper;

  public SedexReceiptReader() {
    mapper = new XmlMapper();
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  public Optional<SedexReceipt> readFromFile(Path path) {
    try {
      return Optional.ofNullable(mapper.readValue(path.toFile(), SedexReceipt.class));
    } catch (IOException e) {
      return Optional.empty();
    }
  }

  public Optional<SedexReceipt> readFromString(String input) {
    try {
      return Optional.ofNullable(mapper.readValue(input, SedexReceipt.class));
    } catch (Exception e) {
      return Optional.empty();
    }
  }
}
