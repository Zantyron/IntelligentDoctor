package com.intelligentdoctor.ai.tools;

import com.intelligentdoctor.catalog.dto.ClinicRoomView;
import com.intelligentdoctor.catalog.dto.DepartmentView;
import com.intelligentdoctor.catalog.dto.DoctorView;
import com.intelligentdoctor.catalog.dto.RegistrationRuleView;
import com.intelligentdoctor.catalog.dto.ScheduleSlotView;
import com.intelligentdoctor.catalog.service.CatalogQueryService;
import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.registration.dto.CreateDraftCommand;
import com.intelligentdoctor.registration.dto.RegistrationDraftView;
import com.intelligentdoctor.registration.service.RegistrationService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AgentToolService {

    private final CatalogQueryService catalogQueryService;
    private final RegistrationService registrationService;
    private final ChatHistoryService chatHistoryService;

    public AgentToolService(CatalogQueryService catalogQueryService,
                            RegistrationService registrationService,
                            ChatHistoryService chatHistoryService) {
        this.catalogQueryService = catalogQueryService;
        this.registrationService = registrationService;
        this.chatHistoryService = chatHistoryService;
    }

    public List<DepartmentView> searchDepartments(String sessionId, String hospitalId, String keyword) {
        List<DepartmentView> result = catalogQueryService.searchDepartments(hospitalId, keyword);
        chatHistoryService.storeToolTrace(sessionId, "searchDepartments", Map.of("keyword", keyword), result);
        return result;
    }

    public List<ClinicRoomView> searchClinics(String sessionId, String hospitalId, String departmentId) {
        List<ClinicRoomView> result = catalogQueryService.searchClinics(hospitalId, departmentId);
        chatHistoryService.storeToolTrace(sessionId, "searchClinics", Map.of("departmentId", departmentId), result);
        return result;
    }

    public List<DoctorView> searchDoctors(String sessionId, String hospitalId, String departmentId, String clinicRoomId) {
        List<DoctorView> result = catalogQueryService.searchDoctors(hospitalId, departmentId, clinicRoomId);
        chatHistoryService.storeToolTrace(sessionId, "searchDoctors", Map.of("departmentId", departmentId, "clinicRoomId", clinicRoomId), result);
        return result;
    }

    public List<ScheduleSlotView> querySchedules(String sessionId, String hospitalId, String departmentId, String doctorId) {
        List<ScheduleSlotView> result = catalogQueryService.querySchedules(hospitalId, departmentId, doctorId);
        chatHistoryService.storeToolTrace(sessionId, "querySchedules", Map.of("departmentId", departmentId, "doctorId", doctorId), result);
        return result;
    }

    public List<RegistrationRuleView> queryRegistrationRules(String sessionId, String hospitalId, String departmentId) {
        List<RegistrationRuleView> result = catalogQueryService.queryRegistrationRules(hospitalId, departmentId);
        chatHistoryService.storeToolTrace(sessionId, "queryRegistrationRules", Map.of("departmentId", departmentId), result);
        return result;
    }

    public RegistrationDraftView createRegistrationDraft(String sessionId, CreateDraftCommand command) {
        RegistrationDraftView result = registrationService.createDraft(command);
        chatHistoryService.storeToolTrace(sessionId, "createRegistrationDraft", Map.of("command", command), result);
        return result;
    }
}
