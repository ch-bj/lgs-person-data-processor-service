package ch.ejpd.lgs.searchindex.client.service.amqp;

import ch.ejpd.lgs.searchindex.client.entity.type.JobState;
import ch.ejpd.lgs.searchindex.client.entity.type.JobType;
import ch.ejpd.lgs.searchindex.client.entity.type.TransactionState;
import ch.ejpd.lgs.searchindex.client.service.exception.MessageHeaderMissingException;
import java.time.Instant;
import java.util.*;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.amqp.core.Message;

public class CommonHeadersDao {
  private final Map<String, Object> rawHeaders;

  public CommonHeadersDao(Map<String, Object> rawHeaders) {
    // Clone instead of reference, to avoid dangling ref counts on Message blocking GC
    this.rawHeaders = new HashMap<>(rawHeaders);
  }

  public boolean contains(String key) {
    return rawHeaders.containsKey(key);
  }

  public Optional<MessageCategory> getOptionalMessageCategory() {
    return getAsString(Headers.MESSAGE_CATEGORY).map(MessageCategory::valueOf);
  }

  public MessageCategory getMessageCategory() {
    return getOptionalMessageCategory().orElseThrow(MessageHeaderMissingException::new);
  }

  public Optional<UUID> getOptionalJobId() {
    return getAsString(Headers.JOB_ID).map(UUID::fromString);
  }

  public UUID getJobId() {
    return getOptionalJobId().orElseThrow(MessageHeaderMissingException::new);
  }

  public Optional<JobState> getOptionalJobState() {
    return getAsString(Headers.JOB_STATE).map(JobState::valueOf);
  }

  public JobState getJobState() {
    return getOptionalJobState().orElseThrow(MessageHeaderMissingException::new);
  }

  public Optional<JobType> getOptionalJobType() {
    return getAsString(Headers.JOB_TYPE).map(JobType::valueOf);
  }

  public JobType getJobType() {
    return getOptionalJobType().orElseThrow(MessageHeaderMissingException::new);
  }

  public Optional<String> getOptionalSenderId() {
    return getAsString(Headers.SENDER_ID);
  }

  public String getSenderId() {
    return getOptionalSenderId().orElseThrow(MessageHeaderMissingException::new);
  }

  public Optional<String> getOptionalLandRegister() {
    return getAsString(Headers.LAND_REGISTER);
  }

  public String getLandRegister() {
    return getOptionalLandRegister().orElse(null);
  }

  public Optional<UUID> getOptionalTransactionId() {
    return getAsString(Headers.TRANSACTION_ID).map(UUID::fromString);
  }

  public UUID getTransactionId() {
    return getOptionalTransactionId().orElseThrow(MessageHeaderMissingException::new);
  }

  public Optional<TransactionState> getOptionalTransactionState() {
    return getAsString(Headers.TRANSACTION_STATE).map(TransactionState::valueOf);
  }

  public TransactionState getTransactionState() {
    return getOptionalTransactionState().orElseThrow(MessageHeaderMissingException::new);
  }

  public Optional<Date> getOptionalTimestamp() {
    return Optional.ofNullable(rawHeaders.get(Headers.TIMESTAMP))
        .map(Long.class::cast)
        .map(Instant::ofEpochMilli)
        .map(Date::from);
  }

  public Date getTimestamp() {
    return getOptionalTimestamp().orElseThrow(MessageHeaderMissingException::new);
  }

  private Optional<String> getAsString(String key) {
    return Optional.ofNullable(rawHeaders.get(key)).map(String.class::cast);
  }

  private void applyToMessage(Message message) {
    this.rawHeaders.forEach(
        (String header, Object value) -> message.getMessageProperties().setHeader(header, value));
  }

  public Message apply(Message message) {
    applyToMessage(message);
    return message;
  }

  public Message applyAndSetJobIdAsCorrelationId(Message message) {
    applyToMessage(message);
    getOptionalJobId()
        .ifPresent(id -> message.getMessageProperties().setCorrelationId(id.toString()));
    return message;
  }

  public Message applyAndSetTransactionIdAsCorrelationId(Message message) {
    applyToMessage(message);
    getOptionalTransactionId()
        .ifPresent(id -> message.getMessageProperties().setCorrelationId(id.toString()));
    return message;
  }

  public Map<String, Object> toMap() {
    return this.rawHeaders;
  }

  public static CommonHeaderBuilder builder() {
    return new CommonHeaderBuilder();
  }

  public static CommonHeaderBuilder builder(CommonHeadersDao header) {
    return new CommonHeaderBuilder()
        .messageCategory(header.getOptionalMessageCategory().orElse(null))
        .jobId(header.getOptionalJobId().orElse(null))
        .jobState(header.getOptionalJobState().orElse(null))
        .jobType(header.getOptionalJobType().orElse(null))
        .senderId(header.getOptionalSenderId().orElse(null))
        .landRegister(header.getLandRegister())
        .transactionId(header.getOptionalTransactionId().orElse(null))
        .transactionState(header.getOptionalTransactionState().orElse(null))
        .timestamp(header.getOptionalTimestamp().orElse(null));
  }

  @Setter()
  @Accessors(fluent = true, chain = true)
  public static class CommonHeaderBuilder {
    private MessageCategory messageCategory;
    private String senderId;
    private String landRegister;
    private UUID jobId;
    private JobState jobState;
    private JobType jobType;
    private UUID transactionId;
    private TransactionState transactionState;
    private Date timestamp;

    protected CommonHeaderBuilder() {}

    public CommonHeaderBuilder timestamp() {
      this.timestamp = Date.from(Instant.now());
      return this;
    }

    public CommonHeaderBuilder timestamp(Instant ts) {
      this.timestamp = Date.from(ts);
      return this;
    }

    public CommonHeaderBuilder timestamp(Date ts) {
      this.timestamp = ts;
      return this;
    }

    public CommonHeadersDao build() {
      final Map<String, Object> newHeaders = new HashMap<>();
      Optional.ofNullable(messageCategory)
          .ifPresent(v -> newHeaders.put(Headers.MESSAGE_CATEGORY, v.toString()));
      Optional.ofNullable(jobId).ifPresent(v -> newHeaders.put(Headers.JOB_ID, v.toString()));
      Optional.ofNullable(jobState).ifPresent(v -> newHeaders.put(Headers.JOB_STATE, v.toString()));
      Optional.ofNullable(jobType).ifPresent(v -> newHeaders.put(Headers.JOB_TYPE, v.toString()));
      Optional.ofNullable(senderId).ifPresent(v -> newHeaders.put(Headers.SENDER_ID, v));
      Optional.ofNullable(landRegister).ifPresent(v -> newHeaders.put(Headers.LAND_REGISTER, v));
      Optional.ofNullable(transactionId)
          .ifPresent(v -> newHeaders.put(Headers.TRANSACTION_ID, v.toString()));
      Optional.ofNullable(transactionState)
          .ifPresent(v -> newHeaders.put(Headers.TRANSACTION_STATE, v.toString()));
      Optional.ofNullable(timestamp)
          .ifPresent(v -> newHeaders.put(Headers.TIMESTAMP, v.toInstant().toEpochMilli()));
      return new CommonHeadersDao(newHeaders);
    }
  }
}
