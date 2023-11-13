package ch.ejpd.lgs.commons.sedex.model;

/**
 * Enum representing different categories for Sedex status codes.
 */
public enum SedexStatusCategory {
  SUCCESS,
  MESSAGE_ERROR,
  AUTHORIZATION_ERROR,
  TRANSPORT_ERROR,
  ADAPTER_ERROR,
  PARTIAL_SUCCESS,
  WARNING
}
