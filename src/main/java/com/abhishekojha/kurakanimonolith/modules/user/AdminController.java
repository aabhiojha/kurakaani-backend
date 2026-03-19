package com.abhishekojha.kurakanimonolith.modules.user;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(Authentication authentication) {
        return Map.of(
                "message", "Admin content",
                "principal", authentication.getName(),
                "authorities", authentication.getAuthorities()
        );
    }
}
