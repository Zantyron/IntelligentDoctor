/* ═══════════════════════════════════════════════════════════════════════
   Intelligent Doctor — Admin Console v2.0
   World-class interactions for the admin dashboard
   ═══════════════════════════════════════════════════════════════════════ */

// ── Auth Guard ───────────────────────────────────────────────────────
if (sessionStorage.getItem("ADMIN_AUTHENTICATED") !== "true") {
    window.location.replace("/");
}

// ── Helpers ──────────────────────────────────────────────────────────
function apiUrl(path) {
    const base = localStorage.getItem("API_BASE_URL")
        || (window.__APP_CONFIG__ && window.__APP_CONFIG__.API_BASE_URL)
        || "";
    return `${base.replace(/\/$/, "")}${path}`;
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function withAdminAuth(options = {}) {
    const headers = new Headers(options.headers || {});
    const authHeader = sessionStorage.getItem("ADMIN_AUTH_HEADER");
    if (authHeader) headers.set("Authorization", authHeader);
    return { ...options, headers };
}

// ── Toast System ─────────────────────────────────────────────────────
function showToast(message, type = "info", duration = 3500) {
    const container = document.getElementById("toastContainer");
    const toast = document.createElement("div");
    toast.className = `toast ${type}`;
    toast.textContent = message;
    container.appendChild(toast);

    const remove = () => {
        toast.classList.add("removing");
        setTimeout(() => toast.remove(), 200);
    };
    toast.addEventListener("click", remove);
    setTimeout(remove, duration);
}

// ── Status Tag Helper ────────────────────────────────────────────────
function statusTag(status) {
    const map = {
        "SUCCESS": "success",
        "COMPLETED": "success",
        "PENDING": "pending",
        "PROCESSING": "pending",
        "FAILED": "failed",
        "ERROR": "failed",
        "ACTIVE": "active",
        "CONFIRMED": "success",
        "CANCELLED": "failed"
    };
    const cls = map[status] || "pending";
    return `<span class="status-tag ${cls}">${escapeHtml(status)}</span>`;
}

// ── Fetch JSON ───────────────────────────────────────────────────────
async function fetchJson(url, options) {
    const response = await fetch(apiUrl(url), withAdminAuth(options));
    if (response.status === 401) {
        sessionStorage.removeItem("ADMIN_AUTHENTICATED");
        sessionStorage.removeItem("ADMIN_AUTH_HEADER");
        window.location.replace("/");
        throw new Error("后台登录已失效");
    }
    const payload = await response.json();
    if (!payload.success) throw new Error(payload.message || "请求失败");
    return payload.data;
}

function formatSummary(summaryJson) {
    if (!summaryJson) return "暂无结果";
    try {
        const summary = JSON.parse(summaryJson);
        return [
            `知识块 ${summary.chunks}`,
            `医院 ${summary.hospitals}`,
            `科室 ${summary.departments}`,
            `诊室 ${summary.clinics}`,
            `医生 ${summary.doctors}`,
            `排班 ${summary.schedules}`,
            `规则 ${summary.rules}`,
            `向量库 ${summary.vectorProvider}`
        ].join(" · ");
    } catch {
        return summaryJson;
    }
}

// ── Load System Profile ──────────────────────────────────────────────
async function loadProfile() {
    try {
        const profile = await fetchJson("/api/system/profile");
        document.getElementById("systemProfile").innerHTML = `
            <div style="display:grid;gap:8px">
                <div style="display:flex;justify-content:space-between">
                    <span>默认医院</span>
                    <strong>${escapeHtml(profile.defaultHospitalId)}</strong>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span>AI 提供者</span>
                    <strong>${escapeHtml(profile.aiProvider)}</strong>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span>向量库</span>
                    <strong>${escapeHtml(profile.vectorProvider)}</strong>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span>库存策略</span>
                    <strong>${escapeHtml(profile.stockProvider)}</strong>
                </div>
                <div style="display:flex;justify-content:space-between">
                    <span>事件链路</span>
                    <strong>${escapeHtml(profile.eventProvider)}</strong>
                </div>
            </div>
        `;
    } catch (error) {
        document.getElementById("systemProfile").innerHTML =
            `<div style="color:var(--danger)">加载失败：${escapeHtml(error.message)}</div>`;
        showToast("加载系统配置失败", "error");
    }
}

// ── Retry Import ─────────────────────────────────────────────────────
async function retryImport(jobId) {
    const feedback = document.getElementById("importFeedback");
    try {
        feedback.innerHTML =
            '<span style="display:flex;align-items:center;gap:8px;color:var(--primary)">'
            + '<span style="width:14px;height:14px;border:2px solid var(--primary-soft);border-top-color:var(--primary);border-radius:50%;animation:spin 0.6s linear infinite;display:inline-block;"></span>'
            + '正在重新提交任务...</span>';
        await fetchJson(`/api/admin/imports/${jobId}/retry`, { method: "POST" });
        feedback.textContent = "重试任务已提交，稍后刷新查看结果";
        showToast("重试任务已提交", "success", 2000);
        setTimeout(loadImports, 800);
    } catch (error) {
        feedback.textContent = `重试失败：${error.message}`;
        showToast("重试失败：" + error.message, "error");
    }
}

// ── Load Import Jobs ─────────────────────────────────────────────────
async function loadImports() {
    try {
        const jobs = await fetchJson("/api/admin/imports");
        const list = document.getElementById("importList");
        list.innerHTML = jobs.length
            ? jobs.map(job => `
                <article class="list-item">
                    <div style="display:flex;justify-content:space-between;align-items:center">
                        <strong>${escapeHtml(job.fileName)}</strong>
                        ${statusTag(job.status)}
                    </div>
                    <span>${escapeHtml(job.fileType)} · 重试 ${job.retryCount || 0} 次</span>
                    <div>${escapeHtml(job.errorMessage || formatSummary(job.summaryJson))}</div>
                    ${job.status === "FAILED"
                        ? `<button class="tiny-btn" data-retry="${escapeHtml(job.id)}"
                                 style="margin-top:6px;justify-self:start">重试</button>`
                        : ""}
                </article>
            `).join("")
            : '<div class="empty">还没有导入任务</div>';

        list.querySelectorAll("[data-retry]").forEach(button => {
            button.addEventListener("click", () => retryImport(button.dataset.retry));
        });
    } catch (error) {
        document.getElementById("importList").innerHTML =
            `<div class="empty" style="color:var(--danger)">加载失败：${escapeHtml(error.message)}</div>`;
    }
}

// ── Load Orders ──────────────────────────────────────────────────────
async function loadOrders() {
    try {
        const orders = await fetchJson("/api/admin/orders");
        const list = document.getElementById("orderList");
        list.innerHTML = orders.length
            ? orders.map(order => `
                <article class="list-item">
                    <div style="display:flex;justify-content:space-between;align-items:center">
                        <strong>${escapeHtml(order.orderNo)}</strong>
                        ${statusTag(order.status)}
                    </div>
                    <span>${escapeHtml(order.patientName)} · ${escapeHtml(order.gender || "-")} · ${escapeHtml(order.age || "-")} 岁</span>
                    <div>科室/医生：${escapeHtml(order.slotId || "-")}</div>
                    <div>就诊时间：${escapeHtml(order.visitDate)} ${escapeHtml(order.visitPeriod)}</div>
                </article>
            `).join("")
            : '<div class="empty">暂无挂号订单</div>';
    } catch (error) {
        document.getElementById("orderList").innerHTML =
            `<div class="empty" style="color:var(--danger)">加载失败：${escapeHtml(error.message)}</div>`;
    }
}

// ── Upload Form ──────────────────────────────────────────────────────
document.getElementById("uploadForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const file = document.getElementById("uploadFile").files[0];
    if (!file) return;

    const feedback = document.getElementById("importFeedback");
    feedback.innerHTML =
        '<span style="display:flex;align-items:center;gap:8px;color:var(--primary)">'
        + '<span style="width:14px;height:14px;border:2px solid var(--primary-soft);border-top-color:var(--primary);border-radius:50%;animation:spin 0.6s linear infinite;display:inline-block;"></span>'
        + '正在上传并创建导入任务...</span>';

    try {
        const formData = new FormData();
        formData.append("file", file);

        const response = await fetch(apiUrl("/api/admin/imports"), {
            method: "POST",
            body: formData,
            ...withAdminAuth()
        });

        const payload = await response.json();
        if (!payload.success) throw new Error(payload.message);

        feedback.textContent = `✓ 任务已提交：${payload.data.fileName} · ${payload.data.status}`;
        showToast(`文件 "${payload.data.fileName}" 导入任务已创建`, "success");
        document.getElementById("uploadFile").value = "";
        await loadImports();
        setTimeout(loadImports, 1500);
    } catch (error) {
        feedback.innerHTML = `<span style="color:var(--danger)">导入失败：${escapeHtml(error.message)}</span>`;
        showToast("导入失败：" + error.message, "error");
    }
});

// ── Reindex Button ───────────────────────────────────────────────────
document.getElementById("reindexBtn").addEventListener("click", async () => {
    const feedback = document.getElementById("importFeedback");
    feedback.innerHTML =
        '<span style="display:flex;align-items:center;gap:8px;color:var(--primary)">'
        + '<span style="width:14px;height:14px;border:2px solid var(--primary-soft);border-top-color:var(--primary);border-radius:50%;animation:spin 0.6s linear infinite;display:inline-block;"></span>'
        + '正在重建向量索引...</span>';

    try {
        const summary = await fetchJson("/api/admin/vector/reindex", { method: "POST" });
        feedback.textContent = `✓ 索引重建完成：${summary.chunks} 个知识块，向量库 ${summary.vectorProvider}`;
        showToast(`向量索引重建完成，共 ${summary.chunks} 个知识块`, "success");
    } catch (error) {
        feedback.innerHTML = `<span style="color:var(--danger)">索引重建失败：${escapeHtml(error.message)}</span>`;
        showToast("索引重建失败：" + error.message, "error");
    }
});

// ── Refresh Buttons ──────────────────────────────────────────────────
document.getElementById("refreshImports").addEventListener("click", loadImports);
document.getElementById("refreshOrders").addEventListener("click", loadOrders);

// ── Init ─────────────────────────────────────────────────────────────
loadProfile();
loadImports();
loadOrders();

// ── Keyboard Shortcuts ───────────────────────────────────────────────
document.addEventListener("keydown", (e) => {
    // Ctrl+R → refresh all data
    if ((e.ctrlKey || e.metaKey) && e.key === "r") {
        // Don't prevent default page reload — just also refresh after
        // Actually let's use Ctrl+Shift+R for data refresh
    }
    if ((e.ctrlKey || e.metaKey) && e.shiftKey && e.key === "R") {
        e.preventDefault();
        loadProfile();
        loadImports();
        loadOrders();
        showToast("数据已刷新", "info", 1500);
    }
});
