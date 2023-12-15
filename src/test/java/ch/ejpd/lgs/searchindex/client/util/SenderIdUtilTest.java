package ch.ejpd.lgs.searchindex.client.util;

import ch.ejpd.lgs.searchindex.client.configuration.SedexConfiguration;
import ch.ejpd.lgs.searchindex.client.service.exception.SenderIdValidationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SenderIdUtilTest {

    private static final String LAND_REGISTER = "Register01";
    @Mock private SedexConfiguration sedexConfiguration = mock(SedexConfiguration.class);

    private static final Set<String> MULTIPLE_SENDER_IDS = Set.of("SendId01", "SendId02", "SendId03");
    private static final String SINGLE_SENDER_ID = "SendId05";

    private SenderIdUtil singleSenderUtil;
    private SenderIdUtil multiSenderUtil;


    @BeforeEach
    void setUp() {
        singleSenderUtil = generateSingleSenderUtil();
        multiSenderUtil = generateMultipleSendersUtil();
    }

    @AfterEach
    void tearDown() {
    }

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
        assertThrows(SenderIdValidationException.class, () -> multiSenderUtil.getSenderId(LAND_REGISTER));
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
        assertNull(multiSenderUtil.getRegionId(LAND_REGISTER));
    }

    @Test
    void getRegionId_givenMultipleSendersAndNullInput() {
        assertNull(multiSenderUtil.getRegionId(null));
    }

    @Test
    void getRegionId_givenMultipleSendersAndEmptyInput() {
        assertNull(multiSenderUtil.getRegionId(null));
    }

    @Test
    void getRegionId_givenSingleSenderAndValidInput() {
        assertEquals(LAND_REGISTER, singleSenderUtil.getRegionId(LAND_REGISTER));
    }

    private SenderIdUtil generateMultipleSendersUtil() {
        when(sedexConfiguration.getSedexSenderIds()).thenReturn(MULTIPLE_SENDER_IDS);
        when(sedexConfiguration.getSedexSenderId()).thenReturn(null);
        when(sedexConfiguration.isInMultiSenderMode()).thenReturn(true);
        return new SenderIdUtil(sedexConfiguration);
    }

    private SenderIdUtil generateSingleSenderUtil() {
        when(sedexConfiguration.getSedexSenderId()).thenReturn(SINGLE_SENDER_ID);
        when(sedexConfiguration.isInMultiSenderMode()).thenReturn(false);
        return new SenderIdUtil(sedexConfiguration);
    }
}