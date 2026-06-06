package com.intelligentdoctor.admin.controller;

import com.intelligentdoctor.admin.dto.ImportJobView;
import com.intelligentdoctor.admin.dto.ImportResultSummary;
import com.intelligentdoctor.admin.service.AdminImportService;
import com.intelligentdoctor.common.ApiResponse;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.registration.dto.RegistrationOrderView;
import com.intelligentdoctor.registration.service.RegistrationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminImportService adminImportService;
    private final RegistrationService registrationService;
    private final AppProperties properties;

    public AdminController(AdminImportService adminImportService,
                           RegistrationService registrationService,
                           AppProperties properties) {
        this.adminImportService = adminImportService;
        this.registrationService = registrationService;
        this.properties = properties;
    }

    @PostMapping("/imports")
    public ApiResponse<ImportJobView> importFile(@RequestParam(required = false) String hospitalId,
                                                 @RequestPart("file") MultipartFile file) {
        ImportJobView job = adminImportService.createImportJob(resolveHospitalId(hospitalId), file);
        adminImportService.processImportAsync(job.id());
        return ApiResponse.success("导入任务已提交", job);
    }

    @PostMapping("/imports/{jobId}/retry")
    public ApiResponse<ImportJobView> retryImport(@PathVariable String jobId) {
        ImportJobView job = adminImportService.retryImport(jobId);
        adminImportService.retryImportAsync(job.id());
        return ApiResponse.success("导入任务已重新提交", job);
    }

    @PostMapping("/vector/reindex")
    public ApiResponse<ImportResultSummary> reindex(@RequestParam(required = false) String hospitalId) {
        return ApiResponse.success("向量索引重建完成",
                adminImportService.rebuildVectors(resolveHospitalId(hospitalId)));
    }

    @GetMapping("/orders")
    public ApiResponse<List<RegistrationOrderView>> orders(@RequestParam(required = false) String hospitalId) {
        return ApiResponse.success(registrationService.listOrders(resolveHospitalId(hospitalId)));
    }

    @GetMapping("/imports")
    public ApiResponse<List<ImportJobView>> imports(@RequestParam(required = false) String hospitalId) {
        return ApiResponse.success(adminImportService.listJobs(resolveHospitalId(hospitalId)));
    }

    private String resolveHospitalId(String hospitalId) {
        return hospitalId == null || hospitalId.isBlank() ? properties.getDefaultHospitalId() : hospitalId;
    }
}
