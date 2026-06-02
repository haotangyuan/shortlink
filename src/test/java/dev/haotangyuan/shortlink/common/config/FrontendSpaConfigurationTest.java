package dev.haotangyuan.shortlink.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.handler;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FrontendSpaConfigurationTest {

    @Test
    void forwardsAppRoutesToSpaIndex() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(get("/app")).andExpect(forwardedUrl("/app/index.html"));
        mockMvc.perform(get("/app/")).andExpect(forwardedUrl("/app/index.html"));
        mockMvc.perform(get("/app/tokens")).andExpect(forwardedUrl("/app/index.html"));
        mockMvc.perform(get("/app/settings/tokens")).andExpect(forwardedUrl("/app/index.html"));
    }

    @Test
    void doesNotForwardRootShortUri() throws Exception {
        mockMvc().perform(get("/abc123"))
                .andExpect(status().isOk())
                .andExpect(handler().methodName("shortUri"));
    }

    @Test
    void doesNotForwardAppStaticAssets() throws Exception {
        mockMvc().perform(get("/app/assets/main.js")).andExpect(status().isNotFound());
    }

    private MockMvc mockMvc() {
        FrontendSpaConfiguration configuration = new FrontendSpaConfiguration();
        return MockMvcBuilders.standaloneSetup(new ShortUriController())
                .addFilters(configuration.frontendSpaForwardFilter().getFilter())
                .build();
    }

    @RestController
    static class ShortUriController {

        @GetMapping("/{shortUri}")
        String shortUri() {
            return "shortUri";
        }
    }
}
