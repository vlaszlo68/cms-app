package hu.laci.cms.backend.servlet.health;

import hu.laci.cms.backend.servlet.support.ServletTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Unit test for the unauthenticated health endpoint. */
class HelloServletTest {

    @Test
    void returnsJsonHealthPayload() throws Exception {
        HelloServlet servlet = new HelloServlet();
        ServletTestSupport.ResponseFixture response = ServletTestSupport.response();

        servlet.doGet(ServletTestSupport.request().build(), response.build());

        Assertions.assertEquals(200, response.getStatus());
        Assertions.assertEquals("application/json", response.getContentType());
        Assertions.assertTrue(response.getBody().contains("Hello CMS"));
    }
}
