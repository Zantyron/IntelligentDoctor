package com.intelligentdoctor.registration.controller;

import com.intelligentdoctor.common.ApiResponse;
import com.intelligentdoctor.registration.dto.ConfirmRegistrationRequest;
import com.intelligentdoctor.registration.dto.RegistrationDraftView;
import com.intelligentdoctor.registration.dto.RegistrationOrderView;
import com.intelligentdoctor.registration.service.RegistrationService;
import com.intelligentdoctor.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registration")
public class RegistrationController {

    private final RegistrationService registrationService;

    public RegistrationController(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @PostMapping("/confirm")
    public ApiResponse<RegistrationOrderView> confirm(@Valid @RequestBody ConfirmRegistrationRequest request) {
        return ApiResponse.success("挂号成功", registrationService.confirm(request));
    }

    @GetMapping("/draft/latest")
    public ApiResponse<RegistrationDraftView> latestDraft(@RequestParam String sessionId) {
        return ApiResponse.success(registrationService.latestDraft(TenantContext.requireHospitalId(), sessionId));
    }

    @GetMapping("/orders")
    public ApiResponse<List<RegistrationOrderView>> orders() {
        return ApiResponse.success(registrationService.listOrders(TenantContext.requireHospitalId()));
    }
}
