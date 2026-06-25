package com.intelligentdoctor.system.controller;

import com.intelligentdoctor.common.ApiResponse;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.system.service.ProviderStatusService;
import com.intelligentdoctor.tenant.TenantContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final AppProperties properties;
    private final ProviderStatusService providerStatusService;

    public SystemController(AppProperties properties,
                            ProviderStatusService providerStatusService) {
        this.properties = properties;
        this.providerStatusService = providerStatusService;
    }

    @GetMapping("/profile")
    public ApiResponse<Map<String, Object>> profile() {
        String currentHospitalId = TenantContext.getHospitalId();
        return ApiResponse.success(Map.of(
                "currentHospitalId", currentHospitalId == null ? properties.getDefaultHospitalId() : currentHospitalId,
                "defaultHospitalId", properties.getDefaultHospitalId(),
                "aiProvider", properties.getAi().getProvider(),
                "vectorProvider", properties.getVectorStore().getProvider(),
                "stockProvider", properties.getRegistration().getStockProvider(),
                "eventProvider", properties.getRegistration().getEventProvider(),
                "providers", providerStatusService.statuses()
        ));
    }
}
