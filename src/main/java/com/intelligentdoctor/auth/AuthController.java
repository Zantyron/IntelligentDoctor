package com.intelligentdoctor.auth;

import com.intelligentdoctor.auth.dto.AdminLoginRequest;
import com.intelligentdoctor.auth.dto.AdminLoginResponse;
import com.intelligentdoctor.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AdminAuthService adminAuthService;
    private final TerminalAuthService terminalAuthService;

    public AuthController(AdminAuthService adminAuthService,
                          TerminalAuthService terminalAuthService) {
        this.adminAuthService = adminAuthService;
        this.terminalAuthService = terminalAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(adminAuthService.login(request.username(), request.password()));
    }

    @PostMapping("/terminal/login")
    public ApiResponse<AdminLoginResponse> terminalLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ApiResponse.success(terminalAuthService.login(request.username(), request.password()));
    }
}
