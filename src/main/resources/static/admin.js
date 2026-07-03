const viewTitles = {
    overview: "总览",
    imports: "数据导入",
    catalog: "医院数据",
    consultations: "问诊记录",
    orders: "挂号订单",
    users: "终端账号"
};

const catalogLabels = {
    departments: "科室",
    doctors: "医师",
    schedules: "排班"
};

const state = {
    catalogTab: "departments",
    sessions: [],
    selectedSessionId: null
};

if (sessionStorage.getItem("ADMIN_AUTHENTICATED") !== "true") {
    window.location.replace("/");
}

function $(selector) {
    return document.querySelector(selector);
}

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
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function withAdminAuth(options = {}) {
    const headers = new Headers(options.headers || {});
    const authHeader = sessionStorage.getItem("ADMIN_AUTH_HEADER");
    if (authHeader) headers.set("Authorization", authHeader);
    return { ...options, headers };
}

async function fetchJson(path, options) {
    const response = await fetch(apiUrl(path), withAdminAuth(options));
    if (response.status === 401 || response.status === 403) {
        clearAdminSession();
        window.location.replace("/");
        throw new Error("后台登录已失效，请重新登录");
    }
    const payload = await response.json();
    if (!response.ok || !payload.success) {
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
}

function clearAdminSession() {
    sessionStorage.removeItem("ADMIN_AUTHENTICATED");
    sessionStorage.removeItem("ADMIN_AUTH_HEADER");
    sessionStorage.removeItem("ADMIN_TOKEN");
    sessionStorage.removeItem("ADMIN_HOSPITAL_ID");
}

function showToast(message, type = "info", duration = 3200) {
    const container = $("#toastContainer");
    if (!container) return;
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

function formatDateTime(value) {
    if (!value) return "-";
    return String(value).replace("T", " ").slice(0, 19);
}

function formatMoney(value) {
    if (value === null || value === undefined || value === "") return "-";
    return `¥${Number(value).toFixed(2)}`;
}

function statusTag(status) {
    const text = String(status ?? "-");
    const cls = {
        SUCCESS: "success",
        COMPLETED: "success",
        CONFIRMED: "success",
        ENABLED: "success",
        OPEN: "success",
        true: "success",
        PENDING: "pending",
        PROCESSING: "pending",
        DRAFT: "pending",
        FAILED: "failed",
        ERROR: "failed",
        CANCELLED: "failed",
        DISABLED: "failed",
        false: "failed"
    }[text] || "pending";
    return `<span class="status-tag ${cls}">${escapeHtml(text)}</span>`;
}

function empty(text) {
    return `<div class="empty">${escapeHtml(text)}</div>`;
}

function setLoading(elementId, text = "加载中...") {
    const el = document.getElementById(elementId);
    if (el) el.innerHTML = empty(text);
}

function renderTable(columns, rows, emptyText) {
    if (!rows.length) return empty(emptyText);
    return `
        <table class="data-table">
            <thead>
                <tr>${columns.map(column => `<th>${escapeHtml(column.label)}</th>`).join("")}</tr>
            </thead>
            <tbody>
                ${rows.map(row => `
                    <tr>
                        ${columns.map(column => `<td>${column.render(row)}</td>`).join("")}
                    </tr>
                `).join("")}
            </tbody>
        </table>
    `;
}

function switchView(view) {
    document.querySelectorAll(".nav-item").forEach(button => {
        button.classList.toggle("active", button.dataset.view === view);
    });
    document.querySelectorAll("[data-view-panel]").forEach(panel => {
        panel.classList.toggle("active", panel.dataset.viewPanel === view);
    });
    $("#viewTitle").textContent = viewTitles[view] || "管理后台";

    if (view === "catalog") loadCatalog();
    if (view === "consultations") loadSessions();
    if (view === "orders") loadOrders();
    if (view === "users") loadUsers();
    if (view === "imports") loadImports();
}

async function loadProfile() {
    try {
        const profile = await fetchJson("/api/system/profile");
        $("#systemProfile").innerHTML = `
            <div class="kv-grid">
                <span>默认医院</span><strong>${escapeHtml(profile.defaultHospitalId)}</strong>
                <span>AI 提供商</span><strong>${escapeHtml(profile.aiProvider)}</strong>
                <span>向量库</span><strong>${escapeHtml(profile.vectorProvider)}</strong>
                <span>号源策略</span><strong>${escapeHtml(profile.stockProvider)}</strong>
                <span>事件链路</span><strong>${escapeHtml(profile.eventProvider)}</strong>
            </div>
        `;
    } catch (error) {
        $("#systemProfile").innerHTML = `<span style="color:var(--danger)">加载失败：${escapeHtml(error.message)}</span>`;
    }
}

async function loadOverview() {
    setLoading("overviewMetrics");
    try {
        const overview = await fetchJson("/api/admin/overview");
        const metrics = [
            ["终端账号", overview.adminUsers ?? 0],
            ["科室", overview.departments ?? 0],
            ["医师", overview.doctors ?? 0],
            ["可预约排班", overview.upcomingSchedules ?? 0],
            ["挂号订单", overview.orders ?? 0],
            ["知识片段", overview.knowledgeChunks ?? 0],
            ["问诊会话", overview.chatSessions ?? 0],
            ["最近问诊", formatDateTime(overview.latestSessionAt)]
        ];
        $("#overviewMetrics").innerHTML = metrics.map(([label, value]) => `
            <article class="metric-card">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(value)}</strong>
            </article>
        `).join("");
    } catch (error) {
        $("#overviewMetrics").innerHTML = empty(`总览加载失败：${error.message}`);
    }
}

function formatSummary(summaryJson) {
    if (!summaryJson) return "暂无结果";
    try {
        const summary = JSON.parse(summaryJson);
        return [
            `知识片段 ${summary.chunks ?? 0}`,
            `医院 ${summary.hospitals ?? 0}`,
            `科室 ${summary.departments ?? 0}`,
            `诊室 ${summary.clinics ?? 0}`,
            `医师 ${summary.doctors ?? 0}`,
            `排班 ${summary.schedules ?? 0}`,
            `规则 ${summary.rules ?? 0}`,
            `向量库 ${summary.vectorProvider ?? "-"}`
        ].join(" · ");
    } catch {
        return summaryJson;
    }
}

async function retryImport(jobId) {
    const feedback = $("#importFeedback");
    try {
        feedback.textContent = "正在重新提交导入任务...";
        await fetchJson(`/api/admin/imports/${encodeURIComponent(jobId)}/retry`, { method: "POST" });
        feedback.textContent = "重试任务已提交";
        showToast("重试任务已提交", "success");
        setTimeout(loadImports, 800);
    } catch (error) {
        feedback.textContent = `重试失败：${error.message}`;
        showToast(`重试失败：${error.message}`, "error");
    }
}

async function loadImports() {
    setLoading("importList");
    try {
        const jobs = await fetchJson("/api/admin/imports");
        $("#importList").innerHTML = jobs.length
            ? jobs.map(job => `
                <article class="list-item">
                    <div style="display:flex;justify-content:space-between;align-items:center;gap:10px">
                        <strong>${escapeHtml(job.fileName)}</strong>
                        ${statusTag(job.status)}
                    </div>
                    <span>${escapeHtml(job.fileType)} · 重试 ${job.retryCount || 0} 次 · ${formatDateTime(job.updatedAt)}</span>
                    <div>${escapeHtml(job.errorMessage || formatSummary(job.summaryJson))}</div>
                    ${job.status === "FAILED"
                        ? `<button class="tiny-btn" data-retry="${escapeHtml(job.id)}" style="justify-self:start;margin-top:6px">重试</button>`
                        : ""}
                </article>
            `).join("")
            : empty("还没有导入任务");
        document.querySelectorAll("[data-retry]").forEach(button => {
            button.addEventListener("click", () => retryImport(button.dataset.retry));
        });
    } catch (error) {
        $("#importList").innerHTML = empty(`导入任务加载失败：${error.message}`);
    }
}

async function loadCatalog() {
    setLoading("catalogTable", `${catalogLabels[state.catalogTab]}加载中...`);
    try {
        const rows = await fetchJson(`/api/admin/${state.catalogTab}`);
        let columns;
        if (state.catalogTab === "departments") {
            columns = [
                { label: "编码", render: row => escapeHtml(row.code) },
                { label: "科室名称", render: row => `<strong>${escapeHtml(row.name)}</strong>` },
                { label: "分类", render: row => escapeHtml(row.category || "-") },
                { label: "医师数", render: row => escapeHtml(row.doctors ?? 0) },
                { label: "近期排班", render: row => escapeHtml(row.schedules ?? 0) },
                { label: "说明", render: row => escapeHtml(row.description || "-") }
            ];
        } else if (state.catalogTab === "doctors") {
            columns = [
                { label: "工号", render: row => escapeHtml(row.code) },
                { label: "医师", render: row => `<strong>${escapeHtml(row.name)}</strong>` },
                { label: "科室", render: row => escapeHtml(row.departmentName) },
                { label: "职称", render: row => escapeHtml(row.title || "-") },
                { label: "专长", render: row => escapeHtml(row.specialty || "-") },
                { label: "费用", render: row => formatMoney(row.consultationFee) },
                { label: "热门", render: row => statusTag(row.hotExpert ? "是" : "否") }
            ];
        } else {
            columns = [
                { label: "日期", render: row => escapeHtml(row.slotDate) },
                { label: "时段", render: row => escapeHtml(row.period) },
                { label: "医师", render: row => `<strong>${escapeHtml(row.doctorName)}</strong>` },
                { label: "总号源", render: row => escapeHtml(row.stockTotal ?? 0) },
                { label: "剩余", render: row => escapeHtml(row.stockAvailable ?? 0) },
                { label: "状态", render: row => statusTag(row.status) },
                { label: "热门", render: row => statusTag(row.hotSlot ? "是" : "否") }
            ];
        }
        $("#catalogTable").innerHTML = renderTable(columns, rows, `暂无${catalogLabels[state.catalogTab]}数据`);
    } catch (error) {
        $("#catalogTable").innerHTML = empty(`${catalogLabels[state.catalogTab]}加载失败：${error.message}`);
    }
}

function renderSessionItem(session, compact = false) {
    const active = session.sessionId === state.selectedSessionId ? "active" : "";
    return `
        <article class="list-item ${active}" data-session-id="${escapeHtml(session.sessionId)}">
            <div style="display:flex;justify-content:space-between;align-items:center;gap:10px">
                <strong>${escapeHtml(session.title || "未命名问诊")}</strong>
                <span>${escapeHtml(session.terminalUsername || "未记录终端")}</span>
            </div>
            <span>${formatDateTime(session.updatedAt)} · ${escapeHtml(session.mode || "导诊")}</span>
            ${compact ? "" : `<div>会话 ID：${escapeHtml(session.sessionId)}</div>`}
        </article>
    `;
}

async function loadSessions() {
    setLoading("sessionList");
    try {
        const sessions = await fetchJson("/api/admin/chat/sessions");
        state.sessions = sessions;
        $("#sessionList").innerHTML = sessions.length
            ? sessions.map(session => renderSessionItem(session)).join("")
            : empty("暂无问诊记录");
        $("#recentSessions").innerHTML = sessions.length
            ? sessions.slice(0, 6).map(session => renderSessionItem(session, true)).join("")
            : empty("暂无问诊记录");
        document.querySelectorAll("[data-session-id]").forEach(item => {
            item.addEventListener("click", () => loadMessages(item.dataset.sessionId));
        });
    } catch (error) {
        $("#sessionList").innerHTML = empty(`问诊记录加载失败：${error.message}`);
        $("#recentSessions").innerHTML = empty(`问诊记录加载失败：${error.message}`);
    }
}

function summarizeMessages(session, messages) {
    const userCount = messages.filter(message => message.role === "user").length;
    const assistantCount = messages.filter(message => message.role === "assistant").length;
    const systemCount = messages.filter(message => message.role === "system").length;
    const firstUserMessage = messages.find(message => message.role === "user")?.content || "暂无患者描述";
    return `
        <div class="summary-grid">
            <span>终端账号</span><strong>${escapeHtml(session?.terminalUsername || "未记录")}</strong>
            <span>更新时间</span><strong>${formatDateTime(session?.updatedAt)}</strong>
            <span>患者消息</span><strong>${userCount}</strong>
            <span>AI 回复</span><strong>${assistantCount}</strong>
            <span>系统消息</span><strong>${systemCount}</strong>
        </div>
        <div style="margin-top:10px;color:var(--text-secondary)">
            <strong>首条描述：</strong>${escapeHtml(firstUserMessage).slice(0, 220)}
        </div>
    `;
}

async function loadMessages(sessionId) {
    state.selectedSessionId = sessionId;
    const session = state.sessions.find(item => item.sessionId === sessionId);
    document.querySelectorAll("[data-session-id]").forEach(item => {
        item.classList.toggle("active", item.dataset.sessionId === sessionId);
    });
    $("#sessionSummary").textContent = "正在加载消息明细...";
    $("#messageList").innerHTML = "";
    try {
        const messages = await fetchJson(`/api/admin/chat/messages?sessionId=${encodeURIComponent(sessionId)}`);
        $("#sessionSummary").innerHTML = summarizeMessages(session, messages);
        $("#messageList").innerHTML = messages.length
            ? messages.map(message => `
                <article class="audit-message">
                    <strong>${escapeHtml(message.role)} · ${formatDateTime(message.createdAt)}</strong>
                    <p>${escapeHtml(message.content)}</p>
                </article>
            `).join("")
            : empty("该会话暂无消息");
    } catch (error) {
        $("#sessionSummary").innerHTML = `<span style="color:var(--danger)">消息加载失败：${escapeHtml(error.message)}</span>`;
    }
}

async function loadOrders() {
    setLoading("orderList");
    try {
        const orders = await fetchJson("/api/admin/orders");
        const columns = [
            { label: "订单号", render: row => `<strong>${escapeHtml(row.orderNo)}</strong>` },
            { label: "患者", render: row => escapeHtml(row.patientName) },
            { label: "性别/年龄", render: row => `${escapeHtml(row.gender || "-")} / ${escapeHtml(row.age || "-")}` },
            { label: "手机号", render: row => escapeHtml(row.patientPhone || "-") },
            { label: "就诊日期", render: row => `${escapeHtml(row.visitDate)} ${escapeHtml(row.visitPeriod || "")}` },
            { label: "号源", render: row => escapeHtml(row.slotId || "-") },
            { label: "状态", render: row => statusTag(row.status) },
            { label: "创建时间", render: row => formatDateTime(row.createdAt) }
        ];
        $("#orderList").innerHTML = renderTable(columns, orders, "暂无挂号订单");
    } catch (error) {
        $("#orderList").innerHTML = empty(`挂号订单加载失败：${error.message}`);
    }
}

async function loadUsers() {
    setLoading("userList");
    try {
        const users = await fetchJson("/api/admin/users");
        $("#userList").innerHTML = users.length
            ? users.map(user => `
                <article class="list-item">
                    <div style="display:flex;justify-content:space-between;align-items:center;gap:10px">
                        <strong>${escapeHtml(user.username)}</strong>
                        ${statusTag(user.enabled ? "ENABLED" : "DISABLED")}
                    </div>
                    <span>创建：${formatDateTime(user.createdAt)} · 更新：${formatDateTime(user.updatedAt)}</span>
                    <div class="user-actions">
                        <button class="secondary" data-reset-user="${escapeHtml(user.id)}">重置密码</button>
                        <button class="secondary" data-toggle-user="${escapeHtml(user.id)}" data-enabled="${user.enabled}">
                            ${user.enabled ? "停用" : "启用"}
                        </button>
                        <button class="danger" data-delete-user="${escapeHtml(user.id)}">删除</button>
                    </div>
                </article>
            `).join("")
            : empty("还没有导诊终端账号");
        document.querySelectorAll("[data-reset-user]").forEach(button => {
            button.addEventListener("click", () => resetUserPassword(button.dataset.resetUser));
        });
        document.querySelectorAll("[data-toggle-user]").forEach(button => {
            button.addEventListener("click", () => toggleUser(button.dataset.toggleUser, button.dataset.enabled === "true"));
        });
        document.querySelectorAll("[data-delete-user]").forEach(button => {
            button.addEventListener("click", () => deleteUser(button.dataset.deleteUser));
        });
    } catch (error) {
        $("#userList").innerHTML = empty(`终端账号加载失败：${error.message}`);
    }
}

async function createUser(username, password) {
    const feedback = $("#userFeedback");
    try {
        await fetchJson("/api/admin/users", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password, enabled: true })
        });
        feedback.textContent = `终端账号 ${username} 已创建`;
        $("#userForm").reset();
        showToast("导诊终端账号已创建", "success");
        await loadUsers();
        await loadOverview();
    } catch (error) {
        feedback.innerHTML = `<span style="color:var(--danger)">创建失败：${escapeHtml(error.message)}</span>`;
        showToast(`创建失败：${error.message}`, "error");
    }
}

async function toggleUser(userId, enabled) {
    const action = enabled ? "停用" : "启用";
    if (!window.confirm(`确认${action}这个导诊终端账号？`)) return;
    try {
        await fetchJson(`/api/admin/users/${encodeURIComponent(userId)}`, {
            method: "PATCH",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ enabled: !enabled })
        });
        $("#userFeedback").textContent = `终端账号已${action}`;
        showToast(`终端账号已${action}`, "success");
        await loadUsers();
    } catch (error) {
        showToast(`${action}失败：${error.message}`, "error");
    }
}

async function resetUserPassword(userId) {
    const password = window.prompt("请输入新密码，至少 8 位");
    if (password === null) return;
    if (password.length < 8) {
        showToast("新密码至少需要 8 位", "error");
        return;
    }
    try {
        await fetchJson(`/api/admin/users/${encodeURIComponent(userId)}/password`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ password })
        });
        $("#userFeedback").textContent = "终端账号密码已重置";
        showToast("密码已重置", "success");
    } catch (error) {
        showToast(`重置密码失败：${error.message}`, "error");
    }
}

async function deleteUser(userId) {
    if (!window.confirm("确认删除这个导诊终端账号？删除后对应机器将无法继续登录智能导诊。")) return;
    try {
        await fetchJson(`/api/admin/users/${encodeURIComponent(userId)}`, { method: "DELETE" });
        $("#userFeedback").textContent = "终端账号已删除";
        showToast("终端账号已删除", "success");
        await loadUsers();
        await loadOverview();
    } catch (error) {
        showToast(`删除失败：${error.message}`, "error");
    }
}

async function uploadImport(event) {
    event.preventDefault();
    const file = $("#uploadFile").files[0];
    if (!file) return;
    const feedback = $("#importFeedback");
    feedback.textContent = "正在上传并创建导入任务...";
    try {
        const formData = new FormData();
        formData.append("file", file);
        const response = await fetch(apiUrl("/api/admin/imports"), {
            method: "POST",
            body: formData,
            ...withAdminAuth()
        });
        if (response.status === 401 || response.status === 403) {
            clearAdminSession();
            window.location.replace("/");
            return;
        }
        const payload = await response.json();
        if (!response.ok || !payload.success) throw new Error(payload.message || "上传失败");
        feedback.textContent = `任务已提交：${payload.data.fileName} · ${payload.data.status}`;
        $("#uploadFile").value = "";
        showToast("导入任务已创建", "success");
        await loadImports();
        setTimeout(loadImports, 1500);
    } catch (error) {
        feedback.innerHTML = `<span style="color:var(--danger)">导入失败：${escapeHtml(error.message)}</span>`;
        showToast(`导入失败：${error.message}`, "error");
    }
}

async function reindexVectors() {
    const feedback = $("#importFeedback");
    feedback.textContent = "正在重建向量索引...";
    try {
        const summary = await fetchJson("/api/admin/vector/reindex", { method: "POST" });
        feedback.textContent = `索引重建完成：${summary.chunks ?? 0} 个知识片段，向量库 ${summary.vectorProvider ?? "-"}`;
        showToast("向量索引重建完成", "success");
        await loadOverview();
    } catch (error) {
        feedback.innerHTML = `<span style="color:var(--danger)">索引重建失败：${escapeHtml(error.message)}</span>`;
        showToast(`索引重建失败：${error.message}`, "error");
    }
}

async function refreshAll() {
    await Promise.allSettled([
        loadOverview(),
        loadProfile(),
        loadImports(),
        loadCatalog(),
        loadSessions(),
        loadOrders(),
        loadUsers()
    ]);
    showToast("数据已刷新", "success", 1600);
}

function bindEvents() {
    document.querySelectorAll(".nav-item").forEach(button => {
        button.addEventListener("click", () => switchView(button.dataset.view));
    });
    document.querySelectorAll("[data-catalog-tab]").forEach(button => {
        button.addEventListener("click", () => {
            state.catalogTab = button.dataset.catalogTab;
            document.querySelectorAll("[data-catalog-tab]").forEach(item => {
                item.classList.toggle("active", item === button);
            });
            loadCatalog();
        });
    });
    $("#logoutAdminBtn").addEventListener("click", () => {
        clearAdminSession();
        window.location.replace("/");
    });
    $("#refreshAllBtn").addEventListener("click", refreshAll);
    $("#refreshImports").addEventListener("click", loadImports);
    $("#refreshSessions").addEventListener("click", loadSessions);
    $("#refreshOrders").addEventListener("click", loadOrders);
    $("#refreshUsers").addEventListener("click", loadUsers);
    $("#uploadForm").addEventListener("submit", uploadImport);
    $("#reindexBtn").addEventListener("click", reindexVectors);
    $("#userForm").addEventListener("submit", async event => {
        event.preventDefault();
        const username = $("#newUsername").value.trim();
        const password = $("#newPassword").value;
        if (!username || !password) return;
        await createUser(username, password);
    });
    document.addEventListener("keydown", event => {
        if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key.toUpperCase() === "R") {
            event.preventDefault();
            refreshAll();
        }
    });
}

bindEvents();
refreshAll();
