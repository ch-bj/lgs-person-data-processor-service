package org.datarocks.lwgs.searchindex.client.model;

import java.util.UUID;
import org.datarocks.lwgs.searchindex.client.entity.type.JobType;

public record JobMetaData(JobType type, UUID jobId, int pageNr, boolean isLastPage) {}
