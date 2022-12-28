package org.datarocks.lwgs.searchindex.client.adapter.rest;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.Arrays;
import java.util.List;
import org.datarocks.lwgs.searchindex.client.entity.Log;
import org.datarocks.lwgs.searchindex.client.entity.type.SeverityType;
import org.datarocks.lwgs.searchindex.client.entity.type.SourceType;
import org.datarocks.lwgs.searchindex.client.repository.LogRepository;
import org.springdoc.core.converters.models.PageableAsQueryParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/logs", produces = MediaType.APPLICATION_JSON_VALUE)
public class LogController {
  final LogRepository logRepository;

  @Autowired
  public LogController(LogRepository logRepository) {
    this.logRepository = logRepository;
  }

  @GetMapping()
  @PageableAsQueryParam
  public Page<Log> getLogs(
      @RequestParam(required = false) List<SourceType> filterSourceTypes,
      @RequestParam(required = false) List<SeverityType> filterSeverityTypes,
      @PageableDefault @Parameter(hidden = true) Pageable pageable) {

    if (filterSourceTypes == null) {
      filterSourceTypes = Arrays.stream(SourceType.values()).toList();
    }

    if (filterSeverityTypes == null) {
      filterSeverityTypes = Arrays.stream(SeverityType.values()).toList();
    }

    return logRepository.findAllBySourceIsInAndSeverityIsIn(
        filterSourceTypes, filterSeverityTypes, pageable);
  }
}
