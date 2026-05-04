package org.mycore.pi.urn;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;

import java.text.ParseException;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mycore.pi.urn.rest.MCRDNBURNRestClient;
import org.mycore.test.MyCoReTest;

import com.google.gson.JsonObject;

@MyCoReTest
public class MCRURNUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "2025-11-07T15:00:21.000Z",
        "2025-11-07T15:00:21Z",
        "2025-11-07T15:00:21.1Z",
        "2025-11-07T15:00:21-12:00",
        "2025-11-07T15:00:21+01:00",
        "2025-11-07T15:00:21.123456Z",
    })
    public void testGetDNBRegisterDateValid(String dateString) throws ParseException {
        try (MockedStatic<MCRDNBURNRestClient> urnClient = Mockito.mockStatic(MCRDNBURNRestClient.class)) {
            urnClient.when(() -> MCRDNBURNRestClient.getRegistrationInfo(anyString()))
                .thenReturn(createTestJson(dateString));
            Date returnDate = MCRURNUtils.getDNBRegisterDate("urn:test:123");
            assertNotNull(returnDate);
        }
    }

    @Test
    public void testGetDNBRegisterDateInvalid() {
        String dateString = "invalidDate";

        try (MockedStatic<MCRDNBURNRestClient> urnClient = Mockito.mockStatic(MCRDNBURNRestClient.class)) {
            urnClient.when(() -> MCRDNBURNRestClient.getRegistrationInfo(anyString()))
                .thenReturn(createTestJson(dateString));
            assertThrows(DateTimeParseException.class, () -> MCRURNUtils.getDNBRegisterDate("urn:test:123"));
        }
    }

    private static Optional<JsonObject> createTestJson(String dateString) {
        JsonObject json = new JsonObject();
        json.addProperty("created", dateString);
        json.addProperty("lastModified", dateString);
        json.addProperty("myUrls", "123");
        json.addProperty("namespace", "123");
        json.addProperty("self", "123");
        json.addProperty("urls", "123");
        json.addProperty("urn", "123");
        return Optional.of(json);
    }
}
