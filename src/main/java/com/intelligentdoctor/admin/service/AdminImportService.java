package com.intelligentdoctor.admin.service;

import com.intelligentdoctor.admin.dto.ImportJobView;
import com.intelligentdoctor.admin.dto.ImportResultSummary;
import com.intelligentdoctor.admin.entity.ImportJobEntity;
import com.intelligentdoctor.admin.repository.ImportJobRepository;
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
import com.intelligentdoctor.common.TextChunker;
import com.intelligentdoctor.knowledge.entity.KnowledgeChunkEntity;
import com.intelligentdoctor.knowledge.repository.KnowledgeChunkRepository;
import com.intelligentdoctor.knowledge.service.KnowledgeSearchService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class AdminImportService {

    private static final Set<String> ALLOWED_FILE_TYPES = Set.of("csv", "xlsx", "xls", "pdf", "md", "markdown", "txt");

    private final ImportJobRepository importJobRepository;
    private final HospitalRepository hospitalRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final DepartmentRepository departmentRepository;
    private final ClinicRoomRepository clinicRoomRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleSlotRepository scheduleSlotRepository;
    private final RegistrationRuleRepository registrationRuleRepository;
    private final KnowledgeSearchService knowledgeSearchService;
    private final TextChunker textChunker;
    private final JsonUtils jsonUtils;

    public AdminImportService(ImportJobRepository importJobRepository,
                              HospitalRepository hospitalRepository,
                              KnowledgeChunkRepository knowledgeChunkRepository,
                              DepartmentRepository departmentRepository,
                              ClinicRoomRepository clinicRoomRepository,
                              DoctorRepository doctorRepository,
                              ScheduleSlotRepository scheduleSlotRepository,
                              RegistrationRuleRepository registrationRuleRepository,
                              KnowledgeSearchService knowledgeSearchService,
                              TextChunker textChunker,
                              JsonUtils jsonUtils) {
        this.importJobRepository = importJobRepository;
        this.hospitalRepository = hospitalRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.departmentRepository = departmentRepository;
        this.clinicRoomRepository = clinicRoomRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleSlotRepository = scheduleSlotRepository;
        this.registrationRuleRepository = registrationRuleRepository;
        this.knowledgeSearchService = knowledgeSearchService;
        this.textChunker = textChunker;
        this.jsonUtils = jsonUtils;
    }

    @Transactional
    public ImportJobView createImportJob(String hospitalId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        ImportJobEntity job = new ImportJobEntity();
        job.setHospitalId(hospitalId);
        job.setFileName(Objects.requireNonNullElse(file.getOriginalFilename(), "upload.dat"));
        job.setFileType(detectFileType(job.getFileName()));
        if (!ALLOWED_FILE_TYPES.contains(job.getFileType())) {
            throw new IllegalArgumentException("不支持的文件类型: " + job.getFileType());
        }
        job.setStatus("PENDING");
        job.setRetryCount(0);
        job = importJobRepository.save(job);

        try {
            Path storedPath = storagePath(job);
            Files.createDirectories(storedPath.getParent());
            Files.copy(file.getInputStream(), storedPath, StandardCopyOption.REPLACE_EXISTING);
            job.setStoragePath(storedPath.toString());
            job = importJobRepository.save(job);
        } catch (IOException ex) {
            job.setStatus("FAILED");
            job.setErrorMessage("保存上传文件失败：" + ex.getMessage());
            job = importJobRepository.save(job);
        }

        return toView(job);
    }

    @Async
    public void processImportAsync(String jobId) {
        processImport(jobId, false);
    }

    @Async
    public void retryImportAsync(String jobId) {
        processImport(jobId, true);
    }

    public void processImportForTest(String jobId) {
        processImport(jobId, false);
    }

    @Transactional
    public ImportJobView retryImport(String jobId) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("导入任务不存在：" + jobId));
        if (!"FAILED".equals(job.getStatus())) {
            throw new IllegalArgumentException("只有失败任务可以重试");
        }
        job.setStatus("PENDING");
        job.setRetryCount(job.getRetryCount() == null ? 1 : job.getRetryCount() + 1);
        job.setErrorMessage(null);
        job = importJobRepository.save(job);
        return toView(job);
    }

    public List<ImportJobView> listJobs(String hospitalId) {
        return importJobRepository.findByHospitalIdOrderByCreatedAtDesc(hospitalId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public ImportResultSummary rebuildVectors(String hospitalId) {
        knowledgeSearchService.rebuild(hospitalId);
        int chunks = knowledgeChunkRepository.findByHospitalId(hospitalId).size();
        return new ImportResultSummary(chunks, 0, 0, 0, 0, 0, 0, knowledgeSearchService.providerName());
    }

    private void processImport(String jobId, boolean retry) {
        ImportJobEntity job = markProcessing(jobId);
        try {
            if (job.getStoragePath() == null || job.getStoragePath().isBlank()) {
                throw new IllegalStateException("任务缺少原始文件，无法解析");
            }
            Path path = Path.of(job.getStoragePath());
            ImportResultSummary summary = switch (job.getFileType()) {
                case "csv" -> importCsv(job.getHospitalId(), job.getFileName(), path);
                case "xlsx", "xls" -> importExcel(job.getHospitalId(), job.getFileName(), path);
                case "pdf" -> importTextualKnowledge(job.getHospitalId(), job.getFileName(), extractPdf(path));
                case "md", "markdown", "txt" -> importTextualKnowledge(job.getHospitalId(), job.getFileName(), Files.readString(path, StandardCharsets.UTF_8));
                default -> throw new IllegalArgumentException("暂不支持该文件格式：" + job.getFileType());
            };
            markCompleted(jobId, summary);
        } catch (Exception ex) {
            markFailed(jobId, (retry ? "重试失败：" : "导入失败：") + ex.getMessage());
        }
    }

    @Transactional
    protected ImportJobEntity markProcessing(String jobId) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("导入任务不存在：" + jobId));
        job.setStatus("PROCESSING");
        job.setErrorMessage(null);
        return importJobRepository.save(job);
    }

    @Transactional
    protected void markCompleted(String jobId, ImportResultSummary summary) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("导入任务不存在：" + jobId));
        job.setStatus("COMPLETED");
        job.setSummaryJson(jsonUtils.toJson(summary));
        job.setErrorMessage(null);
        importJobRepository.save(job);
    }

    @Transactional
    protected void markFailed(String jobId, String message) {
        ImportJobEntity job = importJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("导入任务不存在：" + jobId));
        job.setStatus("FAILED");
        job.setErrorMessage(message);
        importJobRepository.save(job);
    }

    private ImportResultSummary importCsv(String hospitalId, String sourceName, Path path) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).build().parse(reader)) {
            List<Map<String, String>> rows = parser.stream().map(this::csvRecordToMap).toList();
            return importStructuredRows(hospitalId, sourceName, rows);
        }
    }

    private ImportResultSummary importExcel(String hospitalId, String sourceName, Path path) throws IOException {
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        List<Map<String, String>> structuredRows = new ArrayList<>();
        List<KnowledgeChunkEntity> sheetChunks = new ArrayList<>();

        try (var workbook = WorkbookFactory.create(path.toFile())) {
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                List<String> headers = readHeaders(sheet, formatter);
                if (headers.contains("type")) {
                    for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                        Row row = sheet.getRow(rowIndex);
                        if (row == null) {
                            continue;
                        }
                        Map<String, String> values = new LinkedHashMap<>();
                        for (int col = 0; col < headers.size(); col++) {
                            values.put(headers.get(col), formatter.formatCellValue(row.getCell(col)).trim());
                        }
                        if (values.values().stream().anyMatch(value -> !value.isBlank())) {
                            structuredRows.add(values);
                        }
                    }
                } else {
                    StringBuilder text = new StringBuilder("工作表：").append(sheet.getSheetName()).append('\n');
                    for (Row row : sheet) {
                        row.forEach(cell -> text.append(formatter.formatCellValue(cell)).append(" | "));
                        text.append('\n');
                    }
                    sheetChunks.addAll(createChunks(hospitalId, sourceName, sheet.getSheetName(), text.toString(),
                            Map.of("sourceName", sourceName, "sheet", sheet.getSheetName(), "type", "excel-sheet")));
                }
            }
        }

        ImportResultSummary structured = structuredRows.isEmpty()
                ? new ImportResultSummary(0, 0, 0, 0, 0, 0, 0, knowledgeSearchService.providerName())
                : importStructuredRows(hospitalId, sourceName, structuredRows);

        if (!sheetChunks.isEmpty()) {
            knowledgeChunkRepository.saveAll(sheetChunks);
            knowledgeSearchService.rebuild(hospitalId);
        }

        return new ImportResultSummary(
                structured.chunks() + sheetChunks.size(),
                structured.hospitals(),
                structured.departments(),
                structured.clinics(),
                structured.doctors(),
                structured.schedules(),
                structured.rules(),
                knowledgeSearchService.providerName()
        );
    }

    private ImportResultSummary importStructuredRows(String hospitalId, String sourceName, List<Map<String, String>> rows) {
        ImportCounters counters = new ImportCounters();
        Map<String, String> departmentIds = loadDepartmentIds(hospitalId);
        Map<String, String> clinicIds = loadClinicIds(hospitalId);
        Map<String, String> doctorIds = loadDoctorIds(hospitalId);
        List<KnowledgeChunkEntity> knowledgeChunks = new ArrayList<>();

        for (Map<String, String> row : rows) {
            String type = value(row, "type").toLowerCase(Locale.ROOT);
            if (type.isBlank()) {
                continue;
            }
            switch (type) {
                case "hospital" -> {
                    upsertHospital(hospitalId, row);
                    counters.hospitals++;
                }
                case "department" -> {
                    DepartmentEntity entity = upsertDepartment(hospitalId, row);
                    departmentIds.put(entity.getDepartmentCode(), entity.getId());
                    counters.departments++;
                }
                case "clinic", "clinic_room" -> {
                    ClinicRoomEntity entity = upsertClinic(hospitalId, row, departmentIds);
                    clinicIds.put(entity.getClinicCode(), entity.getId());
                    counters.clinics++;
                }
                case "doctor" -> {
                    DoctorEntity entity = upsertDoctor(hospitalId, row, departmentIds, clinicIds);
                    doctorIds.put(entity.getDoctorCode(), entity.getId());
                    counters.doctors++;
                }
                case "schedule" -> {
                    upsertSchedule(hospitalId, row, departmentIds, clinicIds, doctorIds);
                    counters.schedules++;
                }
                case "rule", "registration_rule" -> {
                    upsertRule(hospitalId, row, departmentIds);
                    counters.rules++;
                }
                case "knowledge" -> {
                    String title = firstNonBlank(value(row, "name"), value(row, "title"), "知识片段");
                    String content = firstNonBlank(value(row, "content"), value(row, "description"));
                    knowledgeChunks.addAll(createChunks(hospitalId, sourceName, title, content,
                            Map.of("sourceName", sourceName, "title", title, "type", "knowledge")));
                }
                default -> throw new IllegalArgumentException("未知导入类型：" + type);
            }
        }

        if (!knowledgeChunks.isEmpty()) {
            knowledgeChunkRepository.saveAll(knowledgeChunks);
        }
        knowledgeSearchService.rebuild(hospitalId);

        return new ImportResultSummary(knowledgeChunks.size(), counters.hospitals, counters.departments, counters.clinics,
                counters.doctors, counters.schedules, counters.rules, knowledgeSearchService.providerName());
    }

    private ImportResultSummary importTextualKnowledge(String hospitalId, String sourceName, String content) {
        knowledgeChunkRepository.deleteByHospitalIdAndSourceName(hospitalId, truncate(sourceName, 128));
        List<KnowledgeChunkEntity> chunks = createChunks(hospitalId, sourceName, sourceName, content,
                Map.of("sourceName", sourceName, "type", "document"));
        knowledgeChunkRepository.saveAll(chunks);
        knowledgeSearchService.rebuild(hospitalId);
        return new ImportResultSummary(chunks.size(), 0, 0, 0, 0, 0, 0, knowledgeSearchService.providerName());
    }

    private HospitalEntity upsertHospital(String hospitalId, Map<String, String> row) {
        HospitalEntity entity = hospitalRepository.findByHospitalCode(hospitalId).orElseGet(HospitalEntity::new);
        entity.setHospitalCode(firstNonBlank(value(row, "code"), hospitalId));
        entity.setName(require(row, "name"));
        entity.setDescription(value(row, "description"));
        entity.setAddress(value(row, "address"));
        entity.setPhone(value(row, "phone"));
        return hospitalRepository.save(entity);
    }

    private DepartmentEntity upsertDepartment(String hospitalId, Map<String, String> row) {
        String code = require(row, "code");
        DepartmentEntity entity = departmentRepository.findByHospitalIdAndDepartmentCode(hospitalId, code).orElseGet(DepartmentEntity::new);
        entity.setHospitalId(hospitalId);
        entity.setDepartmentCode(code);
        entity.setName(require(row, "name"));
        entity.setDescription(value(row, "description"));
        entity.setCategory(value(row, "category"));
        entity.setSortOrder(parseInt(value(row, "sort"), 0));
        return departmentRepository.save(entity);
    }

    private ClinicRoomEntity upsertClinic(String hospitalId, Map<String, String> row, Map<String, String> departmentIds) {
        String code = require(row, "code");
        ClinicRoomEntity entity = clinicRoomRepository.findByHospitalIdAndClinicCode(hospitalId, code).orElseGet(ClinicRoomEntity::new);
        entity.setHospitalId(hospitalId);
        entity.setDepartmentId(resolveId(row, departmentIds, "departmentId", "departmentCode"));
        entity.setClinicCode(code);
        entity.setName(require(row, "name"));
        entity.setLocation(value(row, "location"));
        entity.setDescription(value(row, "description"));
        return clinicRoomRepository.save(entity);
    }

    private DoctorEntity upsertDoctor(String hospitalId, Map<String, String> row, Map<String, String> departmentIds, Map<String, String> clinicIds) {
        String code = require(row, "code");
        DoctorEntity entity = doctorRepository.findByHospitalIdAndDoctorCode(hospitalId, code).orElseGet(DoctorEntity::new);
        entity.setHospitalId(hospitalId);
        entity.setDepartmentId(resolveId(row, departmentIds, "departmentId", "departmentCode"));
        entity.setClinicRoomId(optionalId(row, clinicIds, "clinicRoomId", "clinicCode"));
        entity.setDoctorCode(code);
        entity.setName(require(row, "name"));
        entity.setTitle(value(row, "title"));
        entity.setSpecialty(value(row, "specialty"));
        entity.setIntroduction(firstNonBlank(value(row, "introduction"), value(row, "description")));
        entity.setHotExpert(parseBoolean(value(row, "hotExpert")));
        entity.setConsultationFee(parseInt(value(row, "consultationFee"), 0));
        return doctorRepository.save(entity);
    }

    private void upsertSchedule(String hospitalId, Map<String, String> row, Map<String, String> departmentIds,
                                Map<String, String> clinicIds, Map<String, String> doctorIds) {
        String doctorId = resolveId(row, doctorIds, "doctorId", "doctorCode");
        LocalDate slotDate = LocalDate.parse(require(row, "slotDate"));
        String period = require(row, "period");
        ScheduleSlotEntity entity = scheduleSlotRepository
                .findByHospitalIdAndDoctorIdAndSlotDateAndPeriod(hospitalId, doctorId, slotDate, period)
                .orElseGet(ScheduleSlotEntity::new);
        entity.setHospitalId(hospitalId);
        entity.setDepartmentId(resolveId(row, departmentIds, "departmentId", "departmentCode"));
        entity.setClinicRoomId(resolveId(row, clinicIds, "clinicRoomId", "clinicCode"));
        entity.setDoctorId(doctorId);
        entity.setSlotDate(slotDate);
        entity.setPeriod(period);
        int stock = parseInt(value(row, "stockTotal"), 10);
        entity.setStockTotal(stock);
        entity.setStockAvailable(parseInt(value(row, "stockAvailable"), stock));
        entity.setHotSlot(parseBoolean(value(row, "hotSlot")));
        scheduleSlotRepository.save(entity);
    }

    private void upsertRule(String hospitalId, Map<String, String> row, Map<String, String> departmentIds) {
        RegistrationRuleEntity entity = new RegistrationRuleEntity();
        entity.setHospitalId(hospitalId);
        entity.setDepartmentId(resolveId(row, departmentIds, "departmentId", "departmentCode"));
        entity.setRuleText(firstNonBlank(value(row, "ruleText"), value(row, "content"), value(row, "description")));
        entity.setNotice(value(row, "notice"));
        registrationRuleRepository.save(entity);
    }

    private List<KnowledgeChunkEntity> createChunks(String hospitalId,
                                                    String sourceName,
                                                    String title,
                                                    String content,
                                                    Map<String, Object> metadata) {
        List<String> chunkTexts = textChunker.chunk(content, 420, 60);
        List<KnowledgeChunkEntity> chunks = new ArrayList<>();
        for (int i = 0; i < chunkTexts.size(); i++) {
            String chunkKey = safeKey(sourceName + "-" + title + "-" + i);
            Map<String, Object> meta = new HashMap<>(metadata);
            meta.put("chunkIndex", i);
            meta.put("text", chunkTexts.get(i));
            KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
            chunk.setHospitalId(hospitalId);
            chunk.setSourceType("document");
            chunk.setSourceName(truncate(sourceName, 128));
            chunk.setChunkKey(chunkKey);
            chunk.setChunkText(chunkTexts.get(i));
            chunk.setMetadataJson(jsonUtils.toJson(meta));
            chunk.setExternalVectorId(chunkKey);
            chunks.add(chunk);
        }
        return chunks;
    }

    private String extractPdf(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private List<String> readHeaders(Sheet sheet, DataFormatter formatter) {
        Row header = sheet.getRow(0);
        if (header == null) {
            return List.of();
        }
        List<String> headers = new ArrayList<>();
        header.forEach(cell -> headers.add(formatter.formatCellValue(cell).trim()));
        return headers;
    }

    private Map<String, String> csvRecordToMap(CSVRecord record) {
        Map<String, String> values = new LinkedHashMap<>();
        record.toMap().forEach((key, value) -> values.put(key.trim(), value == null ? "" : value.trim()));
        return values;
    }

    private Map<String, String> loadDepartmentIds(String hospitalId) {
        Map<String, String> ids = new HashMap<>();
        departmentRepository.findByHospitalIdOrderBySortOrderAscNameAsc(hospitalId)
                .forEach(entity -> ids.put(entity.getDepartmentCode(), entity.getId()));
        return ids;
    }

    private Map<String, String> loadClinicIds(String hospitalId) {
        Map<String, String> ids = new HashMap<>();
        clinicRoomRepository.findByHospitalId(hospitalId).stream()
                .forEach(entity -> ids.put(entity.getClinicCode(), entity.getId()));
        return ids;
    }

    private Map<String, String> loadDoctorIds(String hospitalId) {
        Map<String, String> ids = new HashMap<>();
        doctorRepository.findByHospitalId(hospitalId).stream()
                .forEach(entity -> ids.put(entity.getDoctorCode(), entity.getId()));
        return ids;
    }

    private String resolveId(Map<String, String> row, Map<String, String> ids, String idField, String codeField) {
        String rawId = value(row, idField);
        if (!rawId.isBlank() && ids.containsValue(rawId)) {
            return rawId;
        }
        String code = firstNonBlank(value(row, codeField), rawId);
        String resolved = ids.get(code);
        if (resolved == null || resolved.isBlank()) {
            throw new IllegalArgumentException("无法解析关联字段：" + idField + "/" + codeField + "=" + code);
        }
        return resolved;
    }

    private String optionalId(Map<String, String> row, Map<String, String> ids, String idField, String codeField) {
        String code = firstNonBlank(value(row, codeField), value(row, idField));
        if (code.isBlank()) {
            return null;
        }
        return ids.getOrDefault(code, code);
    }

    private String require(Map<String, String> row, String key) {
        String value = value(row, key);
        if (value.isBlank()) {
            throw new IllegalArgumentException("缺少必填字段：" + key);
        }
        return value;
    }

    private String value(Map<String, String> row, String key) {
        return row.getOrDefault(key, "").trim();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private boolean parseBoolean(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "是".equals(value) || "yes".equalsIgnoreCase(value);
    }

    private int parseInt(String value, int defaultValue) {
        try {
            return value == null || value.isBlank() ? defaultValue : Integer.parseInt(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private String detectFileType(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "unknown";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private Path storagePath(ImportJobEntity job) {
        String fileName = job.getFileName().replaceAll("[^a-zA-Z0-9._-]", "_");
        return Path.of("data", "imports", job.getId(), fileName);
    }

    private String safeKey(String raw) {
        String normalized = raw.replaceAll("[^a-zA-Z0-9._-]", "-");
        if (normalized.length() > 56) {
            normalized = normalized.substring(0, 56);
        }
        return normalized + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private ImportJobView toView(ImportJobEntity entity) {
        return new ImportJobView(
                entity.getId(),
                entity.getFileName(),
                entity.getFileType(),
                entity.getStatus(),
                entity.getRetryCount(),
                entity.getSummaryJson(),
                entity.getErrorMessage()
        );
    }

    private static class ImportCounters {
        int hospitals;
        int departments;
        int clinics;
        int doctors;
        int schedules;
        int rules;
    }
}
