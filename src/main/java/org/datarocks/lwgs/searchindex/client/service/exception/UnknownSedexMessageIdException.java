package org.datarocks.lwgs.searchindex.client.service.exception;

public class UnknownSedexMessageIdException extends RuntimeException {
  public UnknownSedexMessageIdException(String messageId) {
    super("Couldn't find matching job for sedex message with messageId: " + messageId);
  }
}
