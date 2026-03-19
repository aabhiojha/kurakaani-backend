package com.abhishekojha.kurakanimonolith.auth;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @GetMapping("/public")
    public Map<String, String> publicEndpoint() {
        return Map.of(
                "message", "Public auth endpoint",
                "loginUrl", "/oauth2/authorization/google"
        );
    }
}
