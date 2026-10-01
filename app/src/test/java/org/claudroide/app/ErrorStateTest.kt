package org.claudroide.app

import org.claudroide.app.core.design.AppUiError
import org.claudroide.app.core.design.DataTransmissionStatus
import org.claudroide.app.core.design.ErrorSanitizer
import org.claudroide.app.core.design.UiErrorType
import org.junit.Assert.*
import org.junit.Test

class ErrorStateTest {

    @Test
    fun errorSanitizer_masksAnthropicAndOpenAiApiKeys() {
        val rawAnthropic = "Request failed with 401 using key sk-ant-api03-abcdef1234567890_test"
        val sanitizedAnthropic = ErrorSanitizer.sanitizeErrorMessage(rawAnthropic)
        assertFalse(sanitizedAnthropic.contains("sk-ant-api03"))
        assertTrue(sanitizedAnthropic.contains("[SCHLÜSSEL AUSGEBLENDET]"))

        val rawOpenAi = "Error from proxy: Authorization Bearer sk-1234567890abcdef12345678 rejected"
        val sanitizedOpenAi = ErrorSanitizer.sanitizeErrorMessage(rawOpenAi)
        assertFalse(sanitizedOpenAi.contains("sk-1234567890abcdef"))
        assertTrue(sanitizedOpenAi.contains("[SCHLÜSSEL AUSGEBLENDET]"))
    }

    @Test
    fun errorSanitizer_masksPasswordsAndTokens() {
        val rawParam = "Connection refused to https://api.custom.com?token=secret12345&password=mysecretpassword"
        val sanitizedParam = ErrorSanitizer.sanitizeErrorMessage(rawParam)
        assertFalse(sanitizedParam.contains("secret12345"))
        assertFalse(sanitizedParam.contains("mysecretpassword"))
        assertTrue(sanitizedParam.contains("token=[GESCHÜTZT]"))
        assertTrue(sanitizedParam.contains("password=[GESCHÜTZT]"))
    }

    @Test
    fun appUiError_clearlySeparatesRetryFromCancel() {
        val error = AppUiError(
            type = UiErrorType.RATE_LIMITED,
            title = "Anbieter-Limit erreicht",
            message = "Das Kontingent der Schnittstelle ist vorübergehend erschöpft.",
            transmissionStatus = DataTransmissionStatus.NOT_SENT,
            canRetry = true,
            retryLabel = "In 30s erneut versuchen",
            cancelLabel = "Vorgang abbrechen"
        )

        assertTrue(error.canRetry)
        assertNotEquals(error.retryLabel, error.cancelLabel)
        assertEquals(DataTransmissionStatus.NOT_SENT, error.transmissionStatus)
        assertTrue(error.transmissionStatus.explanationDe.contains("keine Daten"))
    }

    @Test
    fun dataTransmissionStatus_providesBothGermanAndEnglishExplanations() {
        for (status in DataTransmissionStatus.entries) {
            assertTrue(status.explanationDe.isNotBlank())
            assertTrue(status.explanationEn.isNotBlank())
        }
    }
}
