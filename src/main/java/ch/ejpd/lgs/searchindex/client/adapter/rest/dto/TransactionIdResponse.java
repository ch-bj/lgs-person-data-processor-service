package ch.ejpd.lgs.searchindex.client.adapter.rest.dto;

import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TransactionIdResponse {
  UUID transactionId;
}
