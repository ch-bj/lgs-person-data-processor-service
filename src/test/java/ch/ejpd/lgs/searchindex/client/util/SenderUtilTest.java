package ch.ejpd.lgs.searchindex.client.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import ch.ejpd.lgs.searchindex.client.service.exception.SenderIdValidationException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

class SenderUtilTest {

  private static final String LAND_REGISTER = "Register01";
  @Mock private SedexConfiguration sedexConfiguration = mock(SedexConfiguration.class);

  private static final Set<String> MULTIPLE_SENDER_IDS = Set.of("SendId01", "SendId02", "SendId03");
  private static final String SINGLE_SENDER_ID = "SendId05";

  private SenderUtil singleSenderUtil;
  private SenderUtil multiSenderUtil;

  @BeforeEach
  void setUp() {
    singleSenderUtil = generateSingleSenderUtil();
    multiSenderUtil = generateMultipleSendersUtil();
  }

  @AfterEach
  void tearDown() {}

  @Test
  void getSenderId_givenSingleSenderAndRandomInput() {
    assertEquals(SINGLE_SENDER_ID, singleSenderUtil.getSenderId(LAND_REGISTER));
  }

  @Test
  void getSenderId_givenSingleSenderAndNullInput() {
    assertEquals(SINGLE_SENDER_ID, singleSenderUtil.getSenderId(null));
  }

  @Test
  void getSenderId_givenSingleSenderAndEmptyInput() {
    assertEquals(SINGLE_SENDER_ID, singleSenderUtil.getSenderId(""));
  }

  @Test
  void getSenderId_givenMultipleSendersAndRandomInput() {
    assertThrows(
        SenderIdValidationException.class, () -> multiSenderUtil.getSenderId(LAND_REGISTER));
  }

  @Test
  void getSenderId_givenMultipleSendersAndNullInput() {
    assertThrows(SenderIdValidationException.class, () -> multiSenderUtil.getSenderId(null));
  }

  @Test
  void getSenderId_givenMultipleSendersAndEmptyInput() {
    assertThrows(SenderIdValidationException.class, () -> multiSenderUtil.getSenderId(""));
  }

  @Test
  void getSenderId_givenMultipleSenderAndValidInput() {
    assertEquals("SendId01", multiSenderUtil.getSenderId("SendId01"));
  }

  @Test
  void getRegionId_givenMultipleSendersAndValidInput() {
    assertNull(multiSenderUtil.getLandRegister(LAND_REGISTER));
  }

  @Test
  void getRegionId_givenMultipleSendersAndNullInput() {
    assertNull(multiSenderUtil.getLandRegister(null));
  }

  @Test
  void getRegionId_givenMultipleSendersAndEmptyInput() {
    assertNull(multiSenderUtil.getLandRegister(null));
  }

  @Test
  void getRegionId_givenSingleSenderAndValidInput() {
    assertEquals(LAND_REGISTER, singleSenderUtil.getLandRegister(LAND_REGISTER));
  }

  @Test
  void validate_givenNull() {
    generateLengthValidationSenderUtil().validate(null);
  }

  @Test
  void validate_givenEmptyString() {
    generateLengthValidationSenderUtil().validate("");
  }

  @Test
  void validate_givenValidSenderId() {
    generateLengthValidationSenderUtil().validate(LAND_REGISTER);
  }

  @Test
  void validate_givenInvalidSenderId() {
    SenderIdValidationException ex = assertThrows(
            SenderIdValidationException.class,
            () -> generateLengthValidationSenderUtil().validate("TooLongRegisterName"));

    assertEquals(
            "Validation of senderId failed, given senderId TooLongRegisterName, exceeds the maximum allowed length of : 10.",
            ex.getMessage());
  }

  private SenderUtil generateMultipleSendersUtil() {
    when(sedexConfiguration.getSedexSenderIds()).thenReturn(MULTIPLE_SENDER_IDS);
    when(sedexConfiguration.getSedexSenderId()).thenReturn(null);
    when(sedexConfiguration.isInMultiSenderMode()).thenReturn(true);
    return new SenderUtil(sedexConfiguration);
  }

  private SenderUtil generateSingleSenderUtil() {
    when(sedexConfiguration.getSedexSenderId()).thenReturn(SINGLE_SENDER_ID);
    when(sedexConfiguration.isInMultiSenderMode()).thenReturn(false);
    return new SenderUtil(sedexConfiguration);
  }

  private SenderUtil generateLengthValidationSenderUtil() {
    when(sedexConfiguration.getMaxSenderLength()).thenReturn(10);
    return new SenderUtil(sedexConfiguration);
  }
}
