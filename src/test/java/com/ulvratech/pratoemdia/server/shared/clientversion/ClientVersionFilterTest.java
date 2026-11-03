package com.ulvratech.pratoemdia.server.shared.clientversion;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientVersionFilterTest {
    private final ClientVersionFilter filter = new ClientVersionFilter(
            new ClientVersionProperties(Map.of("web", "1.2.0", "android", "2.0.0"))
    );

    private MockHttpServletResponse call(String header) throws Exception {
        var request = new MockHttpServletRequest("GET", "/api/v1/foods");
        if (header != null) {
            request.addHeader("X-Client-Version", header);
        }
        var response = new MockHttpServletResponse();
        var chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        if (response.getStatus() == 200) {
            assertNotNull(chain.getRequest());
        } else {
            assertNull(chain.getRequest());
        }
        return response;
    }

    @Test
    void clientBelowThePlatformMinimumGets426ClientOutdated() throws Exception {
        var response = call("web/1.1.9");

        assertEquals(426, response.getStatus());
        assertTrue(response.getContentAsString().contains("\"code\":\"CLIENT_OUTDATED\""));
        assertTrue(response.getContentAsString().contains("\"status\":426"));
    }

    @Test
    void minimumIsPerPlatform() throws Exception {
        assertEquals(200, call("web/1.2.0").getStatus());
        assertEquals(426, call("android/1.9.9").getStatus());
        assertEquals(200, call("android/2.0.0").getStatus());
    }

    @Test
    void missingMalformedOrUnknownPlatformPass() throws Exception {
        assertEquals(200, call(null).getStatus());
        assertEquals(200, call("garbage").getStatus());
        assertEquals(200, call("ios/0.0.1").getStatus());
    }
}
