package dev.haotangyuan.shortlink.controller.core;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpServletResponse;

@Controller
public class ScalarController {

    @GetMapping("/scalar.html")
    public void scalar(HttpServletResponse response) throws IOException {
        response.setContentType(MediaType.TEXT_HTML_VALUE);
        ClassPathResource resource = new ClassPathResource("static/scalar.html");
        try (InputStream in = resource.getInputStream()) {
            response.getOutputStream().write(in.readAllBytes());
        }
    }
}
