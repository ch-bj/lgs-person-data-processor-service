package ch.ejpd.lgs.searchindex.client.model;

import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import java.util.UUID;

public record JobMetaData(JobType type, UUID jobId, int pageNr, boolean isLastPage) {}
