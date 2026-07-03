package com.intelligentdoctor.admin.controller;

import com.intelligentdoctor.admin.dto.ImportJobView;
import com.intelligentdoctor.admin.dto.ImportResultSummary;
import com.intelligentdoctor.admin.dto.AdminUserView;
import com.intelligentdoctor.admin.dto.CreateAdminUserRequest;
import com.intelligentdoctor.admin.dto.ResetAdminPasswordRequest;
import com.intelligentdoctor.admin.dto.UpdateAdminUserRequest;
import com.intelligentdoctor.admin.service.AdminImportService;
import com.intelligentdoctor.admin.service.AdminDashboardService;
import com.intelligentdoctor.admin.service.AdminUserManagementService;
import com.intelligentdoctor.auth.AdminSecurityContext;
import com.intelligentdoctor.auth.AdminPrincipal;
import com.intelligentdoctor.common.ApiResponse;
import com.intelligentdoctor.registration.dto.RegistrationOrderView;
import com.intelligentdoctor.registration.service.RegistrationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminImportService adminImportService;
    private final AdminDashboardService adminDashboardService;
    private final RegistrationService registrationService;
    private final AdminUserManagementService adminUserManagementService;

    public AdminController(AdminImportService adminImportService,
                           AdminDashboardService adminDashboardService,
                           RegistrationService registrationService,
                           AdminUserManagementService adminUserManagementService) {
        this.adminImportService = adminImportService;
        this.adminDashboardService = adminDashboardService;
        this.registrationService = registrationService;
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping("/overview")
    public ApiResponse<Map<String, Object>> overview() {
        return ApiResponse.success(adminDashboardService.overview(currentHospitalId()));
    }

    @GetMapping("/departments")
    public ApiResponse<List<Map<String, Object>>> departments() {
        return ApiResponse.success(adminDashboardService.departments(currentHospitalId()));
    }

    @GetMapping("/doctors")
    public ApiResponse<List<Map<String, Object>>> doctors() {
        return ApiResponse.success(adminDashboardService.doctors(currentHospitalId()));
    }

    @GetMapping("/schedules")
    public ApiResponse<List<Map<String, Object>>> schedules() {
        return ApiResponse.success(adminDashboardService.schedules(currentHospitalId()));
    }

    @GetMapping("/chat/sessions")
    public ApiResponse<List<com.intelligentdoctor.chat.dto.ChatSessionView>> chatSessions() {
        return ApiResponse.success(adminDashboardService.chatSessions(currentHospitalId()));
    }

    @GetMapping("/chat/messages")
    public ApiResponse<List<com.intelligentdoctor.chat.dto.ChatMessageView>> chatMessages(@org.springframework.web.bind.annotation.RequestParam String sessionId) {
        return ApiResponse.success(adminDashboardService.chatMessages(currentHospitalId(), sessionId));
    }

    @PostMapping("/imports")
    public ApiResponse<ImportJobView> importFile(@RequestPart("file") MultipartFile file) {
        ImportJobView job = adminImportService.createImportJob(currentHospitalId(), file);
        adminImportService.processImportAsync(job.id());
        return ApiResponse.success("导入任务已提交", job);
    }

    @PostMapping("/imports/{jobId}/retry")
    public ApiResponse<ImportJobView> retryImport(@PathVariable String jobId) {
        ImportJobView job = adminImportService.retryImport(currentHospitalId(), jobId);
        adminImportService.retryImportAsync(job.id());
        return ApiResponse.success("导入任务已重新提交", job);
    }

    @PostMapping("/vector/reindex")
    public ApiResponse<ImportResultSummary> reindex() {
        return ApiResponse.success("向量索引重建完成", adminImportService.rebuildVectors(currentHospitalId()));
    }

    @GetMapping("/orders")
    public ApiResponse<List<RegistrationOrderView>> orders() {
        return ApiResponse.success(registrationService.listOrders(currentHospitalId()));
    }

    @GetMapping("/imports")
    public ApiResponse<List<ImportJobView>> imports() {
        return ApiResponse.success(adminImportService.listJobs(currentHospitalId()));
    }

    @GetMapping("/users")
    public ApiResponse<List<AdminUserView>> users() {
        return ApiResponse.success(adminUserManagementService.listUsers(currentPrincipal()));
    }

    @PostMapping("/users")
    public ApiResponse<AdminUserView> createUser(@Valid @RequestBody CreateAdminUserRequest request) {
        return ApiResponse.success("导诊终端账号已创建", adminUserManagementService.createUser(currentPrincipal(), request));
    }

    @PatchMapping("/users/{userId}")
    public ApiResponse<AdminUserView> updateUser(@PathVariable String userId,
                                                 @Valid @RequestBody UpdateAdminUserRequest request) {
        return ApiResponse.success("导诊终端账号已更新", adminUserManagementService.updateUser(currentPrincipal(), userId, request));
    }

    @PostMapping("/users/{userId}/password")
    public ApiResponse<AdminUserView> resetPassword(@PathVariable String userId,
                                                    @Valid @RequestBody ResetAdminPasswordRequest request) {
        return ApiResponse.success("导诊终端账号密码已重置", adminUserManagementService.resetPassword(currentPrincipal(), userId, request));
    }

    @DeleteMapping("/users/{userId}")
    public ApiResponse<Void> deleteUser(@PathVariable String userId) {
        adminUserManagementService.deleteUser(currentPrincipal(), userId);
        return ApiResponse.success(null);
    }

    private String currentHospitalId() {
        return AdminSecurityContext.require().hospitalId();
    }

    private AdminPrincipal currentPrincipal() {
        return AdminSecurityContext.require();
    }
}
