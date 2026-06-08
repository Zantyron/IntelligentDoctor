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
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
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

    @Tool("按关键词检索医院科室")
    public List<DepartmentView> searchDepartments(
            @P("会话 ID") String sessionId,
            @P("医院 ID") String hospitalId,
            @P("科室关键词或症状关键词") String keyword) {
        List<DepartmentView> result = catalogQueryService.searchDepartments(hospitalId, keyword);
        chatHistoryService.storeToolTrace(sessionId, "searchDepartments", Map.of("keyword", keyword), result);
        return result;
    }

    @Tool("查询指定科室下的诊室")
    public List<ClinicRoomView> searchClinics(
            @P("会话 ID") String sessionId,
            @P("医院 ID") String hospitalId,
            @P("科室 ID") String departmentId) {
        List<ClinicRoomView> result = catalogQueryService.searchClinics(hospitalId, departmentId);
        chatHistoryService.storeToolTrace(sessionId, "searchClinics", Map.of("departmentId", departmentId), result);
        return result;
    }

    @Tool("查询指定科室和诊室下的医生")
    public List<DoctorView> searchDoctors(
            @P("会话 ID") String sessionId,
            @P("医院 ID") String hospitalId,
            @P("科室 ID") String departmentId,
            @P("诊室 ID") String clinicRoomId) {
        List<DoctorView> result = catalogQueryService.searchDoctors(hospitalId, departmentId, clinicRoomId);
        chatHistoryService.storeToolTrace(sessionId, "searchDoctors", Map.of("departmentId", departmentId, "clinicRoomId", clinicRoomId), result);
        return result;
    }

    @Tool("查询医生可预约排班")
    public List<ScheduleSlotView> querySchedules(
            @P("会话 ID") String sessionId,
            @P("医院 ID") String hospitalId,
            @P("科室 ID") String departmentId,
            @P("医生 ID") String doctorId) {
        List<ScheduleSlotView> result = catalogQueryService.querySchedules(hospitalId, departmentId, doctorId);
        chatHistoryService.storeToolTrace(sessionId, "querySchedules", Map.of("departmentId", departmentId, "doctorId", doctorId), result);
        return result;
    }

    @Tool("查询科室挂号规则")
    public List<RegistrationRuleView> queryRegistrationRules(
            @P("会话 ID") String sessionId,
            @P("医院 ID") String hospitalId,
            @P("科室 ID") String departmentId) {
        List<RegistrationRuleView> result = catalogQueryService.queryRegistrationRules(hospitalId, departmentId);
        chatHistoryService.storeToolTrace(sessionId, "queryRegistrationRules", Map.of("departmentId", departmentId), result);
        return result;
    }

    @Tool("创建待患者确认的挂号草稿")
    public RegistrationDraftView createRegistrationDraft(
            @P("会话 ID") String sessionId,
            @P("挂号草稿创建命令") CreateDraftCommand command) {
        RegistrationDraftView result = registrationService.createDraft(command);
        chatHistoryService.storeToolTrace(sessionId, "createRegistrationDraft", Map.of("command", command), result);
        return result;
    }
}
