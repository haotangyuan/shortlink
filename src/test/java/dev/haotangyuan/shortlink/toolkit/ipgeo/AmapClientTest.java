package dev.haotangyuan.shortlink.toolkit.ipgeo;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class AmapClientTest {

    @Test
    void returnsUnknownGeoInfoWhenProviderRejectsRequest() throws IOException {
        byte[] responseBody = """
                {"status":"0","info":"INVALID_USER_KEY"}
                """.getBytes(StandardCharsets.UTF_8);
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/ip", exchange -> {
            exchange.sendResponseHeaders(200, responseBody.length);
            exchange.getResponseBody().write(responseBody);
            exchange.close();
        });
        server.start();

        try {
            String endpoint = "http://127.0.0.1:" + server.getAddress().getPort() + "/ip";
            GeoInfo result = new AmapClient("invalid-key", endpoint, 1000).query("1.1.1.1");

            assertNotNull(result);
            assertEquals("Unknown", result.getProvince());
            assertEquals("Unknown", result.getCity());
        } finally {
            server.stop(0);
        }
    }
}
