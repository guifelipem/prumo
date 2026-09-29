package com.prumo.transaction;

import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class CorrelationIdFilterTest {
    @Test
    void preservesIncomingIdAndClearsThreadContext() throws ServletException, IOException {
        var request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-Id", "request-123");
        var response = new MockHttpServletResponse();
        new CorrelationIdFilter().doFilter(request, response, (req, res) ->
                assertEquals("request-123", MDC.get("correlationId")));

        assertEquals("request-123", response.getHeader("X-Correlation-Id"));
        assertNull(MDC.get("correlationId"));
    }

    @Test
    void generatesIdForDirectCalls() throws ServletException, IOException {
        var response = new MockHttpServletResponse();
        new CorrelationIdFilter().doFilter(new MockHttpServletRequest(), response,
                (req, res) -> assertNotNull(MDC.get("correlationId")));
        assertNotNull(response.getHeader("X-Correlation-Id"));
        assertNull(MDC.get("correlationId"));
    }
}
