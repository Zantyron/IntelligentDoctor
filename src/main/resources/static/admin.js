function apiUrl(path) {
    const configuredBaseUrl = localStorage.getItem("API_BASE_URL")
        || (window.__APP_CONFIG__ && window.__APP_CONFIG__.API_BASE_URL)
        || "";
    return `${configuredBaseUrl.replace(/\/$/, "")}${path}`;
}

async function fetchJson(url, options) {
    const response = await fetch(apiUrl(url), options);
    const payload = await response.json();
    if (!payload.success) {
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
}

function escapeHtml(value) {
    return String(value || "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;");
}

function formatSummary(summaryJson) {
    if (!summaryJson) {
        return "暂无结果";
    }
    try {
        const summary = JSON.parse(summaryJson);
        return `知识块 ${summary.chunks}，医院 ${summary.hospitals}，科室 ${summary.departments}，诊室 ${summary.clinics}，医生 ${summary.doctors}，排班 ${summary.schedules}，规则 ${summary.rules}，向量库 ${summary.vectorProvider}`;
    } catch (error) {
        return summaryJson;
    }
}

async function loadProfile() {
    const profile = await fetchJson("/api/system/profile");
    document.getElementById("systemProfile").innerHTML = `
        <div>默认医院：<strong>${escapeHtml(profile.defaultHospitalId)}</strong></div>
        <div>AI 提供者：<strong>${escapeHtml(profile.aiProvider)}</strong></div>
        <div>向量库：<strong>${escapeHtml(profile.vectorProvider)}</strong></div>
        <div>库存策略：<strong>${escapeHtml(profile.stockProvider)}</strong></div>
        <div>事件链路：<strong>${escapeHtml(profile.eventProvider)}</strong></div>
    `;
}

async function retryImport(jobId) {
    const feedback = document.getElementById("importFeedback");
    feedback.textContent = "正在重新提交失败任务...";
    await fetchJson(`/api/admin/imports/${jobId}/retry`, { method: "POST" });
    feedback.textContent = "重试任务已提交，稍后刷新查看结果";
    setTimeout(loadImports, 800);
}

async function loadImports() {
    const jobs = await fetchJson("/api/admin/imports");
    const list = document.getElementById("importList");
    list.innerHTML = jobs.length
        ? jobs.map(job => `
            <article class="list-item">
                <strong>${escapeHtml(job.fileName)}</strong>
                <span>${escapeHtml(job.fileType)} / ${escapeHtml(job.status)} / 重试 ${job.retryCount || 0}</span>
                <div>${escapeHtml(job.errorMessage || formatSummary(job.summaryJson))}</div>
                ${job.status === "FAILED" ? `<button class="tiny-btn" data-retry="${escapeHtml(job.id)}">重试</button>` : ""}
            </article>
        `).join("")
        : '<div class="empty">还没有导入任务</div>';

    list.querySelectorAll("[data-retry]").forEach(button => {
        button.addEventListener("click", () => retryImport(button.dataset.retry).catch(error => {
            document.getElementById("importFeedback").textContent = `重试失败：${error.message}`;
        }));
    });
}

async function loadOrders() {
    const orders = await fetchJson("/api/admin/orders");
    const list = document.getElementById("orderList");
    list.innerHTML = orders.length
        ? orders.map(order => `
            <article class="list-item">
                <strong>${escapeHtml(order.orderNo)}</strong>
                <span>${escapeHtml(order.patientName)} / ${escapeHtml(order.status)}</span>
                <div>slot: ${escapeHtml(order.slotId)}</div>
                <div>visit: ${escapeHtml(order.visitDate)} ${escapeHtml(order.visitPeriod)}</div>
            </article>
        `).join("")
        : '<div class="empty">暂无挂号订单</div>';
}

document.getElementById("uploadForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const file = document.getElementById("uploadFile").files[0];
    if (!file) {
        return;
    }
    const formData = new FormData();
    formData.append("file", file);
    const feedback = document.getElementById("importFeedback");
    feedback.textContent = "正在上传并创建导入任务...";
    try {
        const response = await fetch(apiUrl("/api/admin/imports"), {
            method: "POST",
            body: formData
        });
        const payload = await response.json();
        if (!payload.success) {
            throw new Error(payload.message);
        }
        feedback.textContent = `任务已提交：${payload.data.fileName} / ${payload.data.status}`;
        await loadImports();
        setTimeout(loadImports, 1500);
    } catch (error) {
        feedback.textContent = `导入失败：${error.message}`;
    }
});

document.getElementById("reindexBtn").addEventListener("click", async () => {
    const feedback = document.getElementById("importFeedback");
    feedback.textContent = "正在重建向量索引...";
    try {
        const summary = await fetchJson("/api/admin/vector/reindex", { method: "POST" });
        feedback.textContent = `索引重建完成：${summary.chunks} 个知识块，向量库 ${summary.vectorProvider}`;
    } catch (error) {
        feedback.textContent = `索引重建失败：${error.message}`;
    }
});

document.getElementById("refreshImports").addEventListener("click", loadImports);
document.getElementById("refreshOrders").addEventListener("click", loadOrders);

loadProfile().catch(error => {
    document.getElementById("systemProfile").textContent = `加载失败：${error.message}`;
});
loadImports();
loadOrders();
