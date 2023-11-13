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

/**
 * Data access object for common AMQP message headers.
 */
public class CommonHeadersDao {
  private final Map<String, Object> rawHeaders;

  public CommonHeadersDao(Map<String, Object> rawHeaders) {
    // Clone instead of reference, to avoid dangling ref counts on Message blocking GC
    this.rawHeaders = new HashMap<>(rawHeaders);
  }

  /**
   * Checks if a specific header key is present in the headers.
   *
   * @param key The header key to check
   * @return true if the header is present, false otherwise
   */
  public boolean contains(String key) {
    return rawHeaders.containsKey(key);
  }

  /**
   * Retrieves the optional message category from the headers.
   *
   * @return Optional containing the message category, or empty if not present
   */
  public Optional<MessageCategory> getOptionalMessageCategory() {
    return getAsString(Headers.MESSAGE_CATEGORY).map(MessageCategory::valueOf);
  }

  /**
   * Retrieves the message category from the headers.
   *
   * @return The message category
   * @throws MessageHeaderMissingException if the header is not present
   */
  public MessageCategory getMessageCategory() {
    return getOptionalMessageCategory().orElseThrow(MessageHeaderMissingException::new);
  }

  /**
   * Retrieves the optional job ID from the headers.
   *
   * @return Optional containing the job ID, or empty if not present
   */
  public Optional<UUID> getOptionalJobId() {
    return getAsString(Headers.JOB_ID).map(UUID::fromString);
  }

  /**
   * Retrieves the job ID from the headers.
   *
   * @return The job ID
   * @throws MessageHeaderMissingException if the header is not present
   */
  public UUID getJobId() {
    return getOptionalJobId().orElseThrow(MessageHeaderMissingException::new);
  }

  /**
   * Retrieves the optional job state from the headers.
   *
   * @return Optional containing the job state, or empty if not present
   */
  public Optional<JobState> getOptionalJobState() {
    return getAsString(Headers.JOB_STATE).map(JobState::valueOf);
  }

  /**
   * Retrieves the job state from the headers.
   *
   * @return The job state
   * @throws MessageHeaderMissingException if the header is not present
   */
  public JobState getJobState() {
    return getOptionalJobState().orElseThrow(MessageHeaderMissingException::new);
  }

  /**
   * Retrieves the optional job type from the headers.
   *
   * @return Optional containing the job type, or empty if not present
   */
  public Optional<JobType> getOptionalJobType() {
    return getAsString(Headers.JOB_TYPE).map(JobType::valueOf);
  }

  /**
   * Retrieves the job type from the headers.
   *
   * @return The job type
   * @throws MessageHeaderMissingException if the header is not present
   */
  public JobType getJobType() {
    return getOptionalJobType().orElseThrow(MessageHeaderMissingException::new);
  }

  /**
   * Retrieves the optional sender ID from the headers.
   *
   * @return Optional containing the sender ID, or empty if not present
   */
  public Optional<String> getOptionalSenderId() {
    return getAsString(Headers.SENDER_ID);
  }

  /**
   * Retrieves the sender ID from the headers.
   *
   * @return The sender ID
   * @throws MessageHeaderMissingException if the header is not present
   */
  public String getSenderId() {
    return getOptionalSenderId().orElseThrow(MessageHeaderMissingException::new);
  }

  /**
   * Retrieves the optional transaction ID from the headers.
   *
   * @return Optional containing the transaction ID, or empty if not present
   */
  public Optional<UUID> getOptionalTransactionId() {
    return getAsString(Headers.TRANSACTION_ID).map(UUID::fromString);
  }

  /**
   * Retrieves the transaction ID from the headers.
   *
   * @return The transaction ID
   * @throws MessageHeaderMissingException if the header is not present
   */
  public UUID getTransactionId() {
    return getOptionalTransactionId().orElseThrow(MessageHeaderMissingException::new);
  }

  /**
   * Retrieves the optional transaction state from the headers.
   *
   * @return Optional containing the transaction state, or empty if not present
   */
  public Optional<TransactionState> getOptionalTransactionState() {
    return getAsString(Headers.TRANSACTION_STATE).map(TransactionState::valueOf);
  }

  /**
   * Retrieves the transaction state from the headers.
   *
   * @return The transaction state
   * @throws MessageHeaderMissingException if the header is not present
   */
  public TransactionState getTransactionState() {
    return getOptionalTransactionState().orElseThrow(MessageHeaderMissingException::new);
  }

  /**
   * Retrieves the optional timestamp from the headers.
   *
   * @return Optional containing the timestamp, or empty if not present
   */
  public Optional<Date> getOptionalTimestamp() {
    return Optional.ofNullable(rawHeaders.get(Headers.TIMESTAMP))
        .map(Long.class::cast)
        .map(Instant::ofEpochMilli)
        .map(Date::from);
  }

  /**
   * Retrieves the timestamp from the headers.
   *
   * @return The timestamp
   * @throws MessageHeaderMissingException if the header is not present
   */
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

  /**
   * Applies the headers to a message.
   *
   * @param message The message to apply the headers to
   * @return The modified message
   */
  public Message apply(Message message) {
    applyToMessage(message);
    return message;
  }

  /**
   * Applies the headers to a message and sets the job ID as the correlation ID.
   *
   * @param message The message to apply the headers to
   * @return The modified message
   */
  public Message applyAndSetJobIdAsCorrelationId(Message message) {
    applyToMessage(message);
    getOptionalJobId()
        .ifPresent(id -> message.getMessageProperties().setCorrelationId(id.toString()));
    return message;
  }

  /**
   * Applies the headers to a message and sets the transaction ID as the correlation ID.
   *
   * @param message The message to apply the headers to
   * @return The modified message
   */
  public Message applyAndSetTransactionIdAsCorrelationId(Message message) {
    applyToMessage(message);
    getOptionalTransactionId()
        .ifPresent(id -> message.getMessageProperties().setCorrelationId(id.toString()));
    return message;
  }

  public Map<String, Object> toMap() {
    return this.rawHeaders;
  }

  /**
   * Builder for creating instances of {@code CommonHeadersDao}.
   */
  public static CommonHeaderBuilder builder() {
    return new CommonHeaderBuilder();
  }

  /**
   * Builder for creating instances of {@code CommonHeadersDao} based on an existing instance.
   */
  public static CommonHeaderBuilder builder(CommonHeadersDao header) {
    return new CommonHeaderBuilder()
        .messageCategory(header.getOptionalMessageCategory().orElse(null))
        .jobId(header.getOptionalJobId().orElse(null))
        .jobState(header.getOptionalJobState().orElse(null))
        .jobType(header.getOptionalJobType().orElse(null))
        .senderId(header.getOptionalSenderId().orElse(null))
        .transactionId(header.getOptionalTransactionId().orElse(null))
        .transactionState(header.getOptionalTransactionState().orElse(null))
        .timestamp(header.getOptionalTimestamp().orElse(null));
  }

  /**
   * Builder class for {@code CommonHeadersDao}.
   */
  @Setter()
  @Accessors(fluent = true, chain = true)
  public static class CommonHeaderBuilder {
    private MessageCategory messageCategory;
    private String senderId;
    private UUID jobId;
    private JobState jobState;
    private JobType jobType;
    private UUID transactionId;
    private TransactionState transactionState;
    private Date timestamp;

    protected CommonHeaderBuilder() {}

    /**
     * Sets the timestamp to the current time.
     *
     * @return The updated builder
     */
    public CommonHeaderBuilder timestamp() {
      this.timestamp = Date.from(Instant.now());
      return this;
    }

    /**
     * Sets the timestamp to the specified instant.
     *
     * @param ts The instant to set
     * @return The updated builder
     */
    public CommonHeaderBuilder timestamp(Instant ts) {
      this.timestamp = Date.from(ts);
      return this;
    }

    /**
     * Sets the timestamp to the specified date.
     *
     * @param ts The date to set
     * @return The updated builder
     */
    public CommonHeaderBuilder timestamp(Date ts) {
      this.timestamp = ts;
      return this;
    }

    /**
     * Builds an instance of {@code CommonHeadersDao} based on the current builder state.
     *
     * @return The built instance
     */
    public CommonHeadersDao build() {
      final Map<String, Object> newHeaders = new HashMap<>();
      Optional.ofNullable(messageCategory)
          .ifPresent(v -> newHeaders.put(Headers.MESSAGE_CATEGORY, v.toString()));
      Optional.ofNullable(jobId).ifPresent(v -> newHeaders.put(Headers.JOB_ID, v.toString()));
      Optional.ofNullable(jobState).ifPresent(v -> newHeaders.put(Headers.JOB_STATE, v.toString()));
      Optional.ofNullable(jobType).ifPresent(v -> newHeaders.put(Headers.JOB_TYPE, v.toString()));
      Optional.ofNullable(senderId).ifPresent(v -> newHeaders.put(Headers.SENDER_ID, v));
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
