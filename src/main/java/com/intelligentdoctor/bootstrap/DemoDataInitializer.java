package com.intelligentdoctor.bootstrap;

import com.intelligentdoctor.catalog.entity.ClinicRoomEntity;
import com.intelligentdoctor.catalog.entity.DepartmentEntity;
import com.intelligentdoctor.catalog.entity.DoctorEntity;
import com.intelligentdoctor.catalog.entity.HospitalEntity;
import com.intelligentdoctor.catalog.entity.RegistrationRuleEntity;
import com.intelligentdoctor.catalog.entity.ScheduleSlotEntity;
import com.intelligentdoctor.catalog.repository.ClinicRoomRepository;
import com.intelligentdoctor.catalog.repository.DepartmentRepository;
import com.intelligentdoctor.catalog.repository.DoctorRepository;
import com.intelligentdoctor.catalog.repository.HospitalRepository;
import com.intelligentdoctor.catalog.repository.RegistrationRuleRepository;
import com.intelligentdoctor.catalog.repository.ScheduleSlotRepository;
import com.intelligentdoctor.common.JsonUtils;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.knowledge.entity.KnowledgeChunkEntity;
import com.intelligentdoctor.knowledge.repository.KnowledgeChunkRepository;
import com.intelligentdoctor.knowledge.service.KnowledgeSearchService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;

@Component
@Order(1)
public class DemoDataInitializer implements CommandLineRunner {

    private final AppProperties properties;
    private final HospitalRepository hospitalRepository;
    private final DepartmentRepository departmentRepository;
    private final ClinicRoomRepository clinicRoomRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RegistrationRuleRepository registrationRuleRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeSearchService knowledgeSearchService;
    private final JsonUtils jsonUtils;

    public DemoDataInitializer(AppProperties properties,
                               HospitalRepository hospitalRepository,
                               DepartmentRepository departmentRepository,
                               ClinicRoomRepository clinicRoomRepository,
                               DoctorRepository doctorRepository,
                               ScheduleSlotRepository scheduleSlotRepository,
                               RegistrationRuleRepository registrationRuleRepository,
                               KnowledgeChunkRepository knowledgeChunkRepository,
                               KnowledgeSearchService knowledgeSearchService,
                               JsonUtils jsonUtils) {
        this.properties = properties;
        this.hospitalRepository = hospitalRepository;
        this.departmentRepository = departmentRepository;
        this.clinicRoomRepository = clinicRoomRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.registrationRuleRepository = registrationRuleRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.knowledgeSearchService = knowledgeSearchService;
        this.jsonUtils = jsonUtils;
    }

    @Override
    public void run(String... args) {
        if (departmentRepository.count() > 0) {
            return;
        }

        String hospitalCode = properties.getDefaultHospitalId();
        HospitalEntity hospital = new HospitalEntity();
        hospital.setHospitalCode(hospitalCode);
        hospital.setName("星海市第一人民医院");
        hospital.setDescription("面向演示的三级综合医院，提供智能导诊、线上预约和热门专家号源管理。");
        hospital.setAddress("星海市海云路 88 号");
        hospital.setPhone("021-55667788");
        hospitalRepository.save(hospital);

        DepartmentEntity respiratory = saveDepartment(hospitalCode, "RESP", "呼吸内科", "发热、咳嗽、咽痛、慢性咳嗽和支气管炎等呼吸系统问题。", "内科", 1);
        DepartmentEntity neuro = saveDepartment(hospitalCode, "NEUR", "神经内科", "头痛、头晕、失眠、偏头痛和肢体麻木等神经系统症状。", "内科", 2);
        DepartmentEntity cardio = saveDepartment(hospitalCode, "CARD", "心内科", "胸闷、胸痛、心慌、高血压和冠心病风险评估。", "内科", 3);

        ClinicRoomEntity respClinic = saveClinic(hospitalCode, respiratory.getId(), "RESP-A", "呼吸门诊 A", "门诊楼 3F", "普通呼吸内科门诊。");
        ClinicRoomEntity neuroClinic = saveClinic(hospitalCode, neuro.getId(), "NEUR-A", "神经门诊 A", "门诊楼 4F", "头痛头晕与睡眠障碍门诊。");
        ClinicRoomEntity cardioClinic = saveClinic(hospitalCode, cardio.getId(), "CARD-VIP", "心内专家门诊", "门诊楼 5F", "胸痛和心血管风险专家门诊。");

        DoctorEntity doctorLin = saveDoctor(hospitalCode, respiratory.getId(), respClinic.getId(), "DOC001", "林知远", "主任医师",
                "呼吸系统感染、慢性咳嗽、肺部结节评估", "擅长呼吸系统炎症、慢性咳嗽和肺部影像异常的综合判断。", true, 80);
        DoctorEntity doctorXu = saveDoctor(hospitalCode, neuro.getId(), neuroClinic.getId(), "DOC002", "许清和", "副主任医师",
                "偏头痛、神经性头晕、睡眠障碍", "长期从事头痛、头晕和睡眠障碍门诊诊疗。", false, 60);
        DoctorEntity doctorQiao = saveDoctor(hospitalCode, cardio.getId(), cardioClinic.getId(), "DOC003", "乔一帆", "主任医师",
                "胸闷胸痛、高血压、冠心病风险筛查", "擅长胸痛快速分诊和心血管慢病管理。", true, 120);

        saveSchedule(hospitalCode, respiratory.getId(), respClinic.getId(), doctorLin.getId(), LocalDate.now().plusDays(1), "上午", 18, 18, false);
        saveSchedule(hospitalCode, respiratory.getId(), respClinic.getId(), doctorLin.getId(), LocalDate.now().plusDays(2), "下午", 12, 9, false);
        saveSchedule(hospitalCode, neuro.getId(), neuroClinic.getId(), doctorXu.getId(), LocalDate.now().plusDays(1), "下午", 16, 11, false);
        saveSchedule(hospitalCode, cardio.getId(), cardioClinic.getId(), doctorQiao.getId(), LocalDate.now().plusDays(1), "上午", 6, 4, true);
        saveSchedule(hospitalCode, cardio.getId(), cardioClinic.getId(), doctorQiao.getId(), LocalDate.now().plusDays(2), "上午", 6, 6, true);

        saveRule(hospitalCode, respiratory.getId(), "呼吸内科支持普通号和主任号。发热患者请先测温并佩戴口罩。", "连续高热或呼吸困难请直接急诊评估。");
        saveRule(hospitalCode, neuro.getId(), "神经内科适合头痛、头晕、睡眠障碍等非急症场景。", "突发肢体无力、言语不清请优先急诊。");
        saveRule(hospitalCode, cardio.getId(), "心内科专家号每日限量，热门号源建议提前预约。", "胸痛持续超过 15 分钟请直接急诊胸痛中心。");

        saveKnowledge(hospitalCode, "呼吸内科导诊", "发热伴咳嗽、咽痛、黄痰时优先推荐呼吸内科。若伴随呼吸困难、血氧下降或高热不退，应提示线下急诊风险。");
        saveKnowledge(hospitalCode, "心内科导诊", "胸闷、胸痛、心慌和高血压患者可优先匹配心内科。活动后胸痛加重或持续超过 15 分钟，应优先急诊胸痛中心。");
        saveKnowledge(hospitalCode, "挂号规则", "热门专家号源采用限量库存机制，系统先预占号源再确认订单，避免高并发下超卖。");

        if (isVectorStoreConfigured()) {
            knowledgeSearchService.rebuild(hospitalCode);
        }
    }

    private DepartmentEntity saveDepartment(String hospitalId, String code, String name, String description, String category, int sort) {
        DepartmentEntity entity = new DepartmentEntity();
        entity.setHospitalId(hospitalId);
        entity.setDepartmentCode(code);
        entity.setName(name);
        entity.setDescription(description);
        entity.setCategory(category);
        entity.setSortOrder(sort);
        return departmentRepository.save(entity);
    }

    private ClinicRoomEntity saveClinic(String hospitalId, String departmentId, String code, String name, String location, String description) {
        ClinicRoomEntity entity = new ClinicRoomEntity();
        entity.setHospitalId(hospitalId);
        entity.setDepartmentId(departmentId);
        entity.setClinicCode(code);
        entity.setName(name);
        entity.setLocation(location);
        entity.setDescription(description);
        return clinicRoomRepository.save(entity);
    }

    private DoctorEntity saveDoctor(String hospitalId, String departmentId, String clinicRoomId, String code, String name, String title,
                                    String specialty, String intro, boolean hot, int fee) {
        DoctorEntity entity = new DoctorEntity();
        entity.setHospitalId(hospitalId);
        entity.setDepartmentId(departmentId);
        entity.setClinicRoomId(clinicRoomId);
        entity.setDoctorCode(code);
        entity.setName(name);
        entity.setTitle(title);
        entity.setSpecialty(specialty);
        entity.setIntroduction(intro);
        entity.setHotExpert(hot);
        entity.setConsultationFee(fee);
        return doctorRepository.save(entity);
    }

    private ScheduleSlotEntity saveSchedule(String hospitalId, String departmentId, String clinicRoomId, String doctorId,
                                            LocalDate date, String period, int total, int available, boolean hot) {
        ScheduleSlotEntity entity = new ScheduleSlotEntity();
        entity.setHospitalId(hospitalId);
        entity.setDepartmentId(departmentId);
        entity.setClinicRoomId(clinicRoomId);
        entity.setDoctorId(doctorId);
        entity.setSlotDate(date);
        entity.setPeriod(period);
        entity.setStockTotal(total);
        entity.setStockAvailable(available);
        entity.setHotSlot(hot);
        return scheduleSlotRepository.save(entity);
    }

    private void saveRule(String hospitalId, String departmentId, String text, String notice) {
        RegistrationRuleEntity entity = new RegistrationRuleEntity();
        entity.setHospitalId(hospitalId);
        entity.setDepartmentId(departmentId);
        entity.setRuleText(text);
        entity.setNotice(notice);
        registrationRuleRepository.save(entity);
    }

    private void saveKnowledge(String hospitalId, String sourceName, String text) {
        KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
        chunk.setHospitalId(hospitalId);
        chunk.setSourceType("seed");
        chunk.setSourceName(sourceName);
        chunk.setChunkKey("seed-" + knowledgeChunkRepository.count());
        chunk.setChunkText(text);
        chunk.setMetadataJson(jsonUtils.toJson(Map.of("sourceName", sourceName, "text", text)));
        chunk.setExternalVectorId(chunk.getChunkKey());
        knowledgeChunkRepository.save(chunk);
    }

    private boolean isVectorStoreConfigured() {
        if (!"pinecone".equalsIgnoreCase(properties.getVectorStore().getProvider())) {
            return true;
        }
        return hasText(properties.getVectorStore().getPinecone().getApiKey())
                && hasText(properties.getVectorStore().getPinecone().getIndexHost());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
