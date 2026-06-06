const sessionId = `session-${Date.now()}`;
let currentMode = "diagnosis";
let currentDraft = null;
let confirming = false;

function apiUrl(path) {
    const configuredBaseUrl = localStorage.getItem("API_BASE_URL")
        || (window.__APP_CONFIG__ && window.__APP_CONFIG__.API_BASE_URL)
        || "";
    return `${configuredBaseUrl.replace(/\/$/, "")}${path}`;
}

const chatLog = document.getElementById("chatLog");
const messageInput = document.getElementById("messageInput");
const modeTitle = document.getElementById("modeTitle");
const resultPanel = document.getElementById("resultPanel");
const recommendationsEl = document.getElementById("recommendations");
const evidenceListEl = document.getElementById("evidenceList");
const draftState = document.getElementById("draftState");
const confirmForm = document.getElementById("confirmForm");
const orderResult = document.getElementById("orderResult");
const patientNameInput = document.getElementById("patientName");
const patientPhoneInput = document.getElementById("patientPhone");
const idCardInput = document.getElementById("idCard");

document.querySelectorAll(".mode-btn").forEach(button => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".mode-btn").forEach(item => item.classList.remove("active"));
        button.classList.add("active");
        currentMode = button.dataset.mode;
        modeTitle.textContent = currentMode === "diagnosis" ? "病情诊断" : "智能挂号";
        appendBubble("system", currentMode === "diagnosis"
            ? "已切换到病情诊断模式，请描述你的病情。"
            : "已切换到智能挂号模式，我会结合医院知识推荐科室、医生和可预约号源。");
    });
});

document.querySelectorAll(".quick-prompts button").forEach(button => {
    button.addEventListener("click", () => {
        messageInput.value = button.dataset.prompt;
        messageInput.focus();
    });
});

document.getElementById("chatForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const content = messageInput.value.trim();
    if (!content) {
        return;
    }

    appendBubble("user", content);
    messageInput.value = "";
    resultPanel.classList.add("hidden");
    orderResult.classList.add("hidden");

    const placeholder = appendBubble("assistant", "...");
    const endpoint = currentMode === "diagnosis"
        ? "/api/chat/diagnosis/stream"
        : "/api/chat/registration/stream";

    try {
        const response = await fetch(apiUrl(endpoint), {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                "Accept": "text/event-stream"
            },
            body: JSON.stringify({
                sessionId,
                messages: [{ role: "user", content }],
                consentToStoreHistory: document.getElementById("consent").checked
            })
        });

        if (!response.ok || !response.body) {
            placeholder.textContent = "请求失败，请检查后端服务是否已启动。";
            return;
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder("utf-8");
        let buffer = "";
        let assembled = "";

        while (true) {
            const { value, done } = await reader.read();
            if (done) {
                break;
            }
            buffer += decoder.decode(value, { stream: true });
            const events = buffer.split("\n\n");
            buffer = events.pop() || "";

            for (const rawEvent of events) {
                const lines = rawEvent.split("\n").filter(Boolean);
                let eventName = "message";
                let data = "";
                for (const line of lines) {
                    if (line.startsWith("event:")) {
                        eventName = line.slice(6).trim();
                    }
                    if (line.startsWith("data:")) {
                        data += line.slice(5).trim();
                    }
                }
                if (!data) {
                    continue;
                }
                const payload = JSON.parse(data);
                if (eventName === "chunk") {
                    assembled += payload.content;
                    placeholder.textContent = assembled;
                }
                if (eventName === "result") {
                    renderResult(payload.metadata);
                }
                if (eventName === "error") {
                    placeholder.textContent = `请求失败：${payload.content}`;
                }
            }
        }
    } catch (error) {
        placeholder.textContent = `请求失败：${error.message}`;
    }
});

confirmForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!currentDraft || confirming) {
        return;
    }
    confirming = true;
    const button = confirmForm.querySelector("button");
    button.disabled = true;
    button.textContent = "提交中...";
    orderResult.classList.remove("hidden");
    orderResult.textContent = "正在预占号源并生成正式订单...";

    try {
        const response = await fetch(apiUrl("/api/registration/confirm"), {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                draftId: currentDraft.draftId,
                sessionId,
                idempotencyKey: `confirm-${currentDraft.draftId}`,
                patientName: patientNameInput.value.trim(),
                patientPhone: patientPhoneInput.value.trim(),
                idCard: idCardInput.value.trim()
            })
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "确认挂号失败");
        }
        renderOrder(payload.data);
        confirmForm.classList.add("hidden");
    } catch (error) {
        orderResult.textContent = `确认失败：${error.message}`;
    } finally {
        confirming = false;
        button.disabled = false;
        button.textContent = "确认挂号";
    }
});

function appendBubble(role, content) {
    const bubble = document.createElement("div");
    bubble.className = `bubble ${role}`;
    bubble.textContent = content;
    chatLog.appendChild(bubble);
    chatLog.scrollTop = chatLog.scrollHeight;
    return bubble;
}

function renderResult(metadata) {
    resultPanel.classList.remove("hidden");
    const recommendations = metadata.recommendations || [];
    recommendationsEl.innerHTML = recommendations.length
        ? recommendations.map(item => `
            <article class="card">
                <span>${escapeHtml(item.type)}</span>
                <h4>${escapeHtml(item.title)}</h4>
                <p>${escapeHtml(item.description || "")}</p>
                <small>${escapeHtml(item.reason || "")}</small>
            </article>
        `).join("")
        : '<div class="empty">当前没有结构化推荐卡片。</div>';

    const evidence = metadata.evidence || [];
    evidenceListEl.innerHTML = evidence.length
        ? evidence.map(item => `<div class="evidence"><span>推荐依据</span><div>${escapeHtml(item)}</div></div>`).join("")
        : '<div class="empty">当前没有额外证据片段。</div>';

    const draft = metadata.metadata && metadata.metadata.draft;
    if (draft) {
        currentDraft = draft;
        renderDraft(draft);
    }
}

function renderDraft(draft) {
    draftState.classList.remove("draft-empty");
    draftState.innerHTML = `
        <strong>草稿编号：${escapeHtml(draft.draftId)}</strong>
        <div>状态：${escapeHtml(draft.status || "DRAFT")}</div>
        <div>科室：${escapeHtml(draft.departmentId || "-")}</div>
        <div>医生：${escapeHtml(draft.doctorId || "-")}</div>
        <div>排班：${escapeHtml(draft.visitDate || "-")} ${escapeHtml(draft.visitPeriod || "")}</div>
    `;
    patientNameInput.value = draft.patientName || patientNameInput.value;
    patientPhoneInput.value = draft.patientPhone || patientPhoneInput.value;
    idCardInput.value = draft.idCard || idCardInput.value;
    confirmForm.classList.remove("hidden");
}

function renderOrder(order) {
    orderResult.classList.remove("hidden");
    orderResult.innerHTML = `
        <strong>挂号成功</strong>
        <div>正式订单号：${escapeHtml(order.orderNo)}</div>
        <div>状态：${escapeHtml(order.status)}</div>
        <div>就诊时间：${escapeHtml(order.visitDate)} ${escapeHtml(order.visitPeriod)}</div>
    `;
    appendBubble("system", `挂号成功，订单号：${order.orderNo}`);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
