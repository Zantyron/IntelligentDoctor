/* ═══════════════════════════════════════════════════════════════════════
   Intelligent Doctor — Patient App v2.0
   World-class interactions: toast, ripple, celebration, SSE streaming
   ═══════════════════════════════════════════════════════════════════════ */

// ── State ────────────────────────────────────────────────────────────
let sessionId = localStorage.getItem("ACTIVE_SESSION_ID") || createSessionId();
let currentMode = "diagnosis";
let currentDraft = null;
let confirming = false;
let sending = false;

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
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function createSessionId() {
    return `session-${Date.now()}-${Math.random().toString(16).slice(2, 8)}`;
}

async function copyText(text) {
    const value = String(text || "").trim();
    if (!value) return;
    if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(value);
        return;
    }
    const area = document.createElement("textarea");
    area.value = value;
    area.style.position = "fixed";
    area.style.opacity = "0";
    document.body.appendChild(area);
    area.select();
    document.execCommand("copy");
    area.remove();
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

// ── Ripple Effect ────────────────────────────────────────────────────
function addRipple(event) {
    const button = event.currentTarget;
    const ripple = document.createElement("span");
    ripple.className = "ripple";
    const rect = button.getBoundingClientRect();
    const size = Math.max(rect.width, rect.height);
    ripple.style.width = ripple.style.height = `${size}px`;
    ripple.style.left = `${event.clientX - rect.left - size / 2}px`;
    ripple.style.top = `${event.clientY - rect.top - size / 2}px`;
    button.appendChild(ripple);
    ripple.addEventListener("animationend", () => ripple.remove());
}

// Attach ripple to all primary buttons, composer send, and confirm
document.addEventListener("click", (e) => {
    const btn = e.target.closest(".primary-wide, .primary-btn, .composer button[type=submit], .confirm-form button[type=submit]");
    if (btn) addRipple(e);
});

// ── Celebration Overlay ──────────────────────────────────────────────
function showCelebration(orderNo) {
    const overlay = document.getElementById("celebrationOverlay");
    const card = overlay.querySelector(".celebration-card");
    card.innerHTML = `
        <div class="checkmark">OK</div>
        <h2>挂号成功</h2>
        <p id="celebrationMessage">订单号：${escapeHtml(orderNo)}</p>
        <small>请按预约时间到院就诊，并携带身份证件。</small>
        <button id="closeCelebrationBtn" type="button">我知道了</button>
    `;
    card.querySelector("#closeCelebrationBtn").addEventListener("click", () => {
        overlay.classList.add("hidden");
    });
    overlay.classList.remove("hidden");
    spawnConfetti();
    return;
    document.getElementById("celebrationMessage").textContent =
        `订单号：${orderNo}`;
    overlay.classList.remove("hidden");

    // Confetti particles
    spawnConfetti();

    setTimeout(() => {
        overlay.classList.add("hidden");
    }, 3000);
}

function spawnConfetti() {
    const colors = ["#0d9488", "#f59e0b", "#10b981", "#6366f1", "#ef4444", "#14b8a6", "#fbbf24"];
    const container = document.body;

    for (let i = 0; i < 60; i++) {
        const particle = document.createElement("div");
        const size = Math.random() * 10 + 6;
        const color = colors[Math.floor(Math.random() * colors.length)];

        Object.assign(particle.style, {
            position: "fixed",
            zIndex: "1001",
            pointerEvents: "none",
            width: `${size}px`,
            height: `${size * (Math.random() * 0.6 + 0.4)}px`,
            background: color,
            borderRadius: Math.random() > 0.5 ? "50%" : "2px",
            left: `${Math.random() * 100}vw`,
            top: "-5%",
            opacity: "1",
            transform: `rotate(${Math.random() * 360}deg)`,
            transition: `all ${Math.random() * 2 + 2}s cubic-bezier(0.25, 0.46, 0.45, 0.94)`,
        });

        container.appendChild(particle);

        requestAnimationFrame(() => {
            particle.style.top = `${100 + Math.random() * 20}vh`;
            particle.style.left = `${parseFloat(particle.style.left) + (Math.random() - 0.5) * 30}vw`;
            particle.style.opacity = "0";
            particle.style.transform = `rotate(${Math.random() * 720}deg) scale(0.3)`;
        });

        setTimeout(() => particle.remove(), 3500);
    }
}

// ── DOM Refs ─────────────────────────────────────────────────────────
const chatLog = document.getElementById("chatLog");
const messageInput = document.getElementById("messageInput");
const modeTitle = document.getElementById("modeTitle");
const modeDescription = document.getElementById("modeDescription");
const modeToggle = document.getElementById("modeToggle");
const resultPanel = document.getElementById("resultPanel");
const recommendationToggle = document.getElementById("recommendationToggle");
const hideRecommendationBtn = document.getElementById("hideRecommendationBtn");
const recommendationsEl = document.getElementById("recommendations");
const evidenceListEl = document.getElementById("evidenceList");
const draftState = document.getElementById("draftState");
const draftBadge = document.getElementById("draftBadge");
const confirmForm = document.getElementById("confirmForm");
const orderResult = document.getElementById("orderResult");
const patientNameInput = document.getElementById("patientName");
const patientPhoneInput = document.getElementById("patientPhone");
const idCardInput = document.getElementById("idCard");
const genderInput = document.getElementById("gender");
const ageInput = document.getElementById("age");
const sendBtn = document.getElementById("sendBtn");
const streamState = document.getElementById("streamState");
const sessionList = document.getElementById("sessionList");
const adminLoginDialog = document.getElementById("adminLoginDialog");
const adminLoginError = document.getElementById("adminLoginError");

// ── Init ─────────────────────────────────────────────────────────────
localStorage.setItem("ACTIVE_SESSION_ID", sessionId);
renderWelcome();
loadSessions();

// ── Mode Switching ───────────────────────────────────────────────────
document.querySelectorAll(".mode-btn").forEach(button => {
    button.addEventListener("click", () => {
        document.querySelectorAll(".mode-btn").forEach(item => item.classList.remove("active"));
        button.classList.add("active");

        const isRegistration = button.dataset.mode === "registration";
        modeToggle.classList.toggle("registration-active", isRegistration);
        currentMode = button.dataset.mode;

        modeTitle.textContent = isRegistration ? "智能挂号" : "病情咨询";
        modeDescription.textContent = isRegistration
            ? "请说明想挂号的症状或科室。系统会结合历史上下文、知识库和排班生成挂号草稿。"
            : "请描述症状、持续时间、年龄性别、体温和既往病史。系统会结合知识库给出导诊建议。";

        const msg = isRegistration
            ? "已切换到智能挂号模式。如果上一句提到过症状，我会一起参考。"
            : "已切换到病情咨询模式。";
        appendBubble("system", msg);
        showToast(isRegistration ? "已切换到智能挂号模式" : "已切换到病情咨询模式", "info", 2000);
    });
});

// ── Quick Prompts ────────────────────────────────────────────────────
document.querySelectorAll(".quick-prompts button").forEach(button => {
    button.addEventListener("click", () => {
        messageInput.value = button.dataset.prompt;
        messageInput.focus();
        // Auto-expand textarea
        messageInput.style.height = "auto";
        messageInput.style.height = messageInput.scrollHeight + "px";
    });
});

// ── Enter to Send ────────────────────────────────────────────────────
messageInput.addEventListener("keydown", (event) => {
    if (event.key !== "Enter" || event.shiftKey || event.isComposing) return;
    event.preventDefault();
    document.getElementById("chatForm").requestSubmit();
});

// Auto-resize textarea
messageInput.addEventListener("input", () => {
    messageInput.style.height = "auto";
    messageInput.style.height = Math.min(messageInput.scrollHeight, 160) + "px";
});

// ── New Session ──────────────────────────────────────────────────────
document.getElementById("newSessionBtn").addEventListener("click", () => {
    sessionId = createSessionId();
    localStorage.setItem("ACTIVE_SESSION_ID", sessionId);
    currentDraft = null;
    chatLog.innerHTML = "";
    resultPanel.classList.add("hidden");
    document.getElementById("draftCard").classList.add("hidden");
    confirmForm.classList.add("hidden");
    orderResult.classList.add("hidden");
    draftBadge.textContent = "未生成";
    draftState.className = "draft-empty";
    draftState.textContent = "进入智能挂号模式并描述诉求后，系统会尝试生成待确认挂号草稿。";
    renderWelcome();
    loadSessions();
    showToast("已创建新对话", "info", 2000);
});

// ── Delete All Sessions ──────────────────────────────────────────────
document.getElementById("deleteAllSessionsBtn").addEventListener("click", async () => {
    if (!window.confirm("确认删除全部聊天记录吗？数据库中的会话、消息、Prompt 轨迹和工具调用记录都会同步删除。")) return;

    try {
        await fetchJson("/api/chat/sessions/all", { method: "DELETE" });
        sessionId = createSessionId();
        localStorage.setItem("ACTIVE_SESSION_ID", sessionId);
        chatLog.innerHTML = "";
        renderWelcome();
        loadSessions();
        showToast("已删除全部会话记录", "success", 3000);
    } catch (e) {
        showToast("删除失败：" + e.message, "error");
    }
});

// ── Recommendation Toggle ────────────────────────────────────────────
recommendationToggle.addEventListener("click", () => {
    if (resultPanel.classList.contains("user-hidden")) {
        resultPanel.classList.remove("user-hidden");
        resultPanel.classList.remove("collapsed");
        recommendationToggle.setAttribute("aria-expanded", "true");
        recommendationToggle.querySelector("strong").textContent = "收起";
        return;
    }
    const collapsed = resultPanel.classList.toggle("collapsed");
    recommendationToggle.setAttribute("aria-expanded", String(!collapsed));
    recommendationToggle.querySelector("strong").textContent = collapsed ? "展开" : "收起";
});

hideRecommendationBtn.addEventListener("click", () => {
    resultPanel.classList.add("user-hidden");
    resultPanel.classList.add("collapsed");
    recommendationToggle.setAttribute("aria-expanded", "false");
    recommendationToggle.querySelector("strong").textContent = "显示";
    showToast("智能推荐已隐藏，点击右下角入口可重新显示", "info", 2200);
});

// ── Chat Form Submit ─────────────────────────────────────────────────
document.getElementById("chatForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (sending) return;

    const content = messageInput.value.trim();
    if (!content) return;

    sending = true;
    sendBtn.disabled = true;
    sendBtn.innerHTML = '<span class="send-icon">⋯</span> 生成中';
    streamState.textContent = "模型正在理解你的描述...";
    streamState.classList.add("streaming");

    appendBubble("user", content);
    messageInput.value = "";
    messageInput.style.height = "";
    messageInput.blur();
    resultPanel.classList.add("hidden");
    orderResult.classList.add("hidden");

    const placeholder = appendBubble("assistant", "");
    placeholder.classList.add("thinking");
    placeholder.innerHTML =
        '<span class="typing-dot"></span><span class="typing-dot"></span><span class="typing-dot"></span><em>正在检索知识库并生成回复</em>';

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
            throw new Error("后端服务未返回流式响应");
        }

        await consumeEventStream(response, placeholder);
        await loadSessions();
    } catch (error) {
        placeholder.classList.remove("thinking");
        placeholder.innerHTML =
            `<div class="bubble-content" style="color:var(--danger)">请求失败：${escapeHtml(error.message)}</div>`;
        showToast("请求失败：" + error.message, "error");
    } finally {
        sending = false;
        sendBtn.disabled = false;
        sendBtn.innerHTML = '<span class="send-icon">↑</span> 发送';
        streamState.textContent = "慢慢说，我们会耐心听清楚每一处不舒服，陪您一步步找到合适的就诊方向。";
        streamState.classList.remove("streaming");
    }
});

// ── Confirm Registration ─────────────────────────────────────────────
confirmForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    if (!currentDraft || confirming) return;

    confirming = true;
    const button = confirmForm.querySelector("button");
    button.disabled = true;
    button.textContent = "提交中...";
    orderResult.classList.remove("hidden");
    orderResult.innerHTML =
        '<div style="display:flex;align-items:center;gap:8px;color:var(--primary)">'
        + '<span style="width:16px;height:16px;border:2px solid var(--primary-soft);border-top-color:var(--primary);border-radius:50%;animation:spin 0.6s linear infinite;display:inline-block;"></span>'
        + '正在预占号源并生成正式订单...</div>';

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
                idCard: idCardInput.value.trim(),
                gender: genderInput.value,
                age: Number(ageInput.value)
            })
        });
        const payload = await response.json();
        if (!response.ok || !payload.success) {
            throw new Error(payload.message || "确认挂号失败");
        }
        renderOrder(payload.data);
        confirmForm.classList.add("hidden");
        showCelebration(payload.data.orderNo);
        showToast("挂号成功！订单号：" + payload.data.orderNo, "success", 5000);
    } catch (error) {
        orderResult.innerHTML =
            `<div style="color:var(--danger)">确认失败：${escapeHtml(error.message)}</div>`;
        showToast("挂号失败：" + error.message, "error");
    } finally {
        confirming = false;
        button.disabled = false;
        button.textContent = "确认挂号";
    }
});

// ── Admin Login ──────────────────────────────────────────────────────
document.getElementById("adminEntryBtn").addEventListener("click", () => {
    adminLoginError.classList.add("hidden");
    adminLoginDialog.showModal();
});

document.getElementById("cancelAdminLogin").addEventListener("click", () => {
    adminLoginDialog.close();
});

document.getElementById("adminLoginForm").addEventListener("submit", async (event) => {
    event.preventDefault();
    const username = document.getElementById("adminUsername").value.trim();
    const password = document.getElementById("adminPassword").value;
    try {
        const response = await fetch(apiUrl("/api/auth/login"), {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ username, password })
        });
        const payload = await response.json();
        if (!response.ok || !payload.success || !payload.data?.token) {
            throw new Error(payload.message || "Login failed");
        }
        sessionStorage.setItem("ADMIN_AUTHENTICATED", "true");
        sessionStorage.setItem("ADMIN_AUTH_HEADER", `Bearer ${payload.data.token}`);
        sessionStorage.setItem("ADMIN_HOSPITAL_ID", payload.data.hospitalId || "");
        window.location.href = "/admin.html";
        return;
    } catch (error) {
        adminLoginError.textContent = error.message || "Login failed";
    }
    adminLoginError.classList.remove("hidden");
    adminLoginError.style.animation = "none";
    adminLoginError.offsetHeight; // trigger reflow
    adminLoginError.style.animation = "shake 0.4s ease";
});

// Add shake keyframe dynamically
const shakeStyle = document.createElement("style");
shakeStyle.textContent = `
    @keyframes shake {
        0%, 100% { transform: translateX(0); }
        20% { transform: translateX(-6px); }
        40% { transform: translateX(6px); }
        60% { transform: translateX(-4px); }
        80% { transform: translateX(4px); }
    }
`;
document.head.appendChild(shakeStyle);

// ── SSE Stream Consumer ──────────────────────────────────────────────
async function consumeEventStream(response, placeholder) {
    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    let assembled = "";

    while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const events = buffer.split("\n\n");
        buffer = events.pop() || "";

        for (const rawEvent of events) {
            const parsed = parseSseEvent(rawEvent);
            if (!parsed.data) continue;

            const payload = JSON.parse(parsed.data);

            if (parsed.eventName === "meta") {
                streamState.textContent = payload.content || "模型开始生成回复...";
            }

            if (parsed.eventName === "chunk") {
                assembled += payload.content;
                placeholder.classList.remove("thinking");
                placeholder.parentElement.dataset.copyText = assembled;

                // Render markdown-style content nicely
                placeholder.innerHTML =
                    `<div class="bubble-content">${formatContent(assembled)}</div>`;
                smoothScrollToBottom(chatLog);
            }

            if (parsed.eventName === "result") {
                renderResult(payload.metadata);
            }

            if (parsed.eventName === "error") {
                placeholder.classList.remove("thinking");
                placeholder.innerHTML =
                    `<div class="bubble-content" style="color:var(--danger)">请求失败：${escapeHtml(payload.content)}</div>`;
                showToast("AI 响应错误：" + payload.content, "error");
            }
        }
    }
}

function parseSseEvent(rawEvent) {
    const lines = rawEvent.split("\n").filter(Boolean);
    let eventName = "message";
    let data = "";
    for (const line of lines) {
        if (line.startsWith("event:")) eventName = line.slice(6).trim();
        if (line.startsWith("data:")) data += line.slice(5).trim();
    }
    return { eventName, data };
}

// ── Content Formatting ───────────────────────────────────────────────
function formatContent(text) {
    const lines = escapeHtml(text).replace(/\r\n/g, "\n").split("\n");
    const html = [];
    let listType = null;

    const closeList = () => {
        if (listType) {
            html.push(`</${listType}>`);
            listType = null;
        }
    };
    const inline = value => value
        .replace(/\*\*(.+?)\*\*/g, "<strong>$1</strong>")
        .replace(/`([^`]+?)`/g, "<code>$1</code>");

    for (const rawLine of lines) {
        const line = rawLine.trim();
        if (!line) {
            closeList();
            continue;
        }
        const heading = line.match(/^(#{1,3})\s+(.+)$/);
        if (heading) {
            closeList();
            const level = Math.min(heading[1].length + 2, 5);
            html.push(`<h${level}>${inline(heading[2])}</h${level}>`);
            continue;
        }
        const plainHeading = line.match(/^([^:：]{2,12})[:：]\s*(.*)$/);
        if (plainHeading) {
            closeList();
            html.push(`<h4>${plainHeading[1]}</h4>`);
            if (plainHeading[2]) {
                html.push(`<p>${inline(plainHeading[2])}</p>`);
            }
            continue;
        }
        const unordered = line.match(/^[-*]\s+(.+)$/);
        if (unordered) {
            if (listType !== "ul") {
                closeList();
                listType = "ul";
                html.push("<ul>");
            }
            html.push(`<li>${inline(unordered[1])}</li>`);
            continue;
        }
        const ordered = line.match(/^\d+[.]\s+(.+)$/);
        if (ordered) {
            if (listType !== "ol") {
                closeList();
                listType = "ol";
                html.push("<ol>");
            }
            html.push(`<li>${inline(ordered[1])}</li>`);
            continue;
        }
        closeList();
        html.push(`<p>${inline(line)}</p>`);
    }
    closeList();
    return `<div class="ai-md">${html.join("")}</div>`;
}
// ── Smooth Scroll ────────────────────────────────────────────────────
function smoothScrollToBottom(el) {
    const scrollHeight = el.scrollHeight;
    const currentScroll = el.scrollTop + el.clientHeight;
    // Only auto-scroll if user is near the bottom
    if (scrollHeight - currentScroll < 120) {
        el.scrollTo({ top: scrollHeight, behavior: "smooth" });
    }
}

// ── Append Bubble ────────────────────────────────────────────────────
function appendBubble(role, content, messageId) {
    const bubble = document.createElement("div");
    bubble.className = `bubble ${role}`;
    bubble.dataset.copyText = content || "";

    const copyButton = document.createElement("button");
    copyButton.className = "bubble-copy";
    copyButton.type = "button";
    copyButton.textContent = "复制";
    copyButton.title = "复制这条消息";
    copyButton.addEventListener("click", async (e) => {
        e.stopPropagation();
        await copyText(bubble.dataset.copyText || bubble.innerText);
        showToast("已复制消息", "success", 1800);
    });
    bubble.appendChild(copyButton);

    if (messageId) {
        bubble.dataset.messageId = messageId;
        const deleteButton = document.createElement("button");
        deleteButton.className = "bubble-delete";
        deleteButton.type = "button";
        deleteButton.innerHTML = "×";
        deleteButton.title = "删除此消息";
        deleteButton.addEventListener("click", (e) => {
            e.stopPropagation();
            deleteMessage(messageId, bubble);
        });
        bubble.appendChild(deleteButton);
    }

    if (!messageId) {
        const deleteButton = document.createElement("button");
        deleteButton.className = "bubble-delete";
        deleteButton.type = "button";
        deleteButton.textContent = "删除";
        deleteButton.title = "删除这条消息";
        deleteButton.addEventListener("click", (e) => {
            e.stopPropagation();
            bubble.remove();
        });
        bubble.appendChild(deleteButton);
    }

    const body = document.createElement("div");
    body.className = "bubble-content";
    body.textContent = content;
    bubble.appendChild(body);
    chatLog.appendChild(bubble);

    // Stagger delay based on bubble count
    const bubbles = chatLog.querySelectorAll(".bubble");
    bubble.style.animationDelay = "0s";

    smoothScrollToBottom(chatLog);
    return body;
}

// ── Render Result ────────────────────────────────────────────────────
function renderResult(metadata) {
    resultPanel.classList.remove("hidden");
    resultPanel.classList.remove("user-hidden");
    resultPanel.classList.add("collapsed");
    recommendationToggle.setAttribute("aria-expanded", "false");
    recommendationToggle.querySelector("strong").textContent = "展开";

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

    const appointmentOptions = (metadata.metadata && metadata.metadata.appointmentOptions) || [];
    if (appointmentOptions.length) {
        recommendationsEl.innerHTML += renderAppointmentTable(appointmentOptions);
    }

    const evidence = metadata.evidence || [];
    evidenceListEl.innerHTML = evidence.length
        ? evidence.map(item =>
            `<div class="evidence"><span>推荐依据</span><div>${escapeHtml(item)}</div></div>`
          ).join("")
        : '<div class="empty" style="margin-top:10px">当前未召回到额外知识片段。</div>';

    const draft = metadata.metadata && metadata.metadata.draft;
    if (draft) {
        currentDraft = draft;
        renderDraft(draft);
        showToast("已生成挂号草稿，请确认信息", "info", 3000);
    }
}

// ── Render Draft ─────────────────────────────────────────────────────
function renderAppointmentTable(options) {
    return `
        <section class="appointment-table-wrap">
            <div class="appointment-table-title">
                <strong>可预约号源</strong>
                <span>已按当前症状匹配诊室、医生和时间段</span>
            </div>
            <div class="appointment-table-scroll">
                <table class="appointment-table">
                    <thead>
                        <tr>
                            <th>诊室</th>
                            <th>医生</th>
                            <th>职称</th>
                            <th>擅长</th>
                            <th>日期</th>
                            <th>时段</th>
                            <th>余号</th>
                            <th>费用</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${options.map(item => `
                            <tr>
                                <td>
                                    <strong>${escapeHtml(item.clinicName || "-")}</strong>
                                    <small>${escapeHtml(item.clinicLocation || "")}</small>
                                </td>
                                <td>${escapeHtml(item.doctorName || "-")}</td>
                                <td>${escapeHtml(item.doctorTitle || "-")}</td>
                                <td>${escapeHtml(item.specialty || "-")}</td>
                                <td>${escapeHtml(item.slotDate || "-")}</td>
                                <td>${escapeHtml(item.period || "-")}</td>
                                <td><span class="stock-pill">${escapeHtml(item.stockAvailable ?? "-")}</span></td>
                                <td>${escapeHtml(item.consultationFee ?? "-")} 元</td>
                            </tr>
                        `).join("")}
                    </tbody>
                </table>
            </div>
        </section>
    `;
}

function renderDraft(draft) {
    document.getElementById("draftCard").classList.remove("hidden");
    draftBadge.textContent = draft.status || "DRAFT";
    draftBadge.style.animation = "none";
    draftBadge.offsetHeight;
    draftBadge.style.animation = "popIn 0.3s cubic-bezier(0.34, 1.56, 0.64, 1)";

    draftState.className = "draft-summary";
    draftState.innerHTML = `
        <div><strong>草稿编号</strong><span>${escapeHtml(draft.draftId)}</span></div>
        <div><strong>科室</strong><span>${escapeHtml(draft.departmentId || "-")}</span></div>
        <div><strong>医生</strong><span>${escapeHtml(draft.doctorId || "-")}</span></div>
        <div><strong>时间</strong><span>${escapeHtml(draft.visitDate || "-")} ${escapeHtml(draft.visitPeriod || "")}</span></div>
    `;

    if (draft.patientName) patientNameInput.value = draft.patientName;
    if (draft.patientPhone) patientPhoneInput.value = draft.patientPhone;
    if (draft.idCard) idCardInput.value = draft.idCard;
    if (draft.gender) genderInput.value = draft.gender;
    if (draft.age) ageInput.value = draft.age;

    confirmForm.classList.remove("hidden");
}

// ── Render Order ─────────────────────────────────────────────────────
function renderOrder(order) {
    orderResult.classList.remove("hidden");
    orderResult.innerHTML = `
        <strong>✓ 挂号成功</strong>
        <div>正式订单号：<strong>${escapeHtml(order.orderNo)}</strong></div>
        <div>患者：${escapeHtml(order.patientName)} / ${escapeHtml(order.gender)} / ${escapeHtml(order.age)} 岁</div>
        <div>就诊时间：${escapeHtml(order.visitDate)} ${escapeHtml(order.visitPeriod)}</div>
    `;

    // Highlight draft card
    const draftCard = document.getElementById("draftCard");
    draftCard.style.transition = "all 0.3s ease";
    draftCard.style.borderColor = "var(--success)";
    setTimeout(() => {
        draftCard.style.borderColor = "";
    }, 3000);

    appendBubble("system", `挂号成功，订单号：${order.orderNo}`);
}

// ── Load Sessions ────────────────────────────────────────────────────
async function loadSessions() {
    try {
        const data = await fetchJson("/api/chat/sessions").catch(() => []);
        sessionList.innerHTML = data.length
            ? data.map(item => `
                <div class="session-row ${item.sessionId === sessionId ? "active" : ""}">
                    <button class="session-item"
                            data-session="${escapeHtml(item.sessionId)}" type="button">
                        <span>${escapeHtml(item.title || "新对话")}</span>
                        <small>${escapeHtml(item.mode || "-")}</small>
                    </button>
                    <button class="session-delete" data-delete-session="${escapeHtml(item.sessionId)}" type="button" title="删除该对话">删除</button>
                </div>
            `).join("")
            : '<div class="empty">暂无历史对话</div>';

        sessionList.querySelectorAll("[data-session]").forEach(button => {
            button.addEventListener("click", () => openSession(button.dataset.session));
        });
        sessionList.querySelectorAll("[data-delete-session]").forEach(button => {
            button.addEventListener("click", event => {
                event.stopPropagation();
                deleteSession(button.dataset.deleteSession);
            });
        });
    } catch (e) {
        sessionList.innerHTML = '<div class="empty">加载失败</div>';
    }
}

async function deleteSession(targetSessionId) {
    if (!window.confirm("确认删除这个历史对话吗？数据库中的消息、Prompt 轨迹和工具调用记录会同步删除。")) {
        return;
    }
    try {
        await fetchJson(`/api/chat/sessions?sessionId=${encodeURIComponent(targetSessionId)}`, { method: "DELETE" });
        if (targetSessionId === sessionId) {
            sessionId = createSessionId();
            localStorage.setItem("ACTIVE_SESSION_ID", sessionId);
            currentDraft = null;
            chatLog.innerHTML = "";
            resultPanel.classList.add("hidden");
            document.getElementById("draftCard").classList.add("hidden");
            confirmForm.classList.add("hidden");
            orderResult.classList.add("hidden");
            renderWelcome();
        }
        await loadSessions();
        showToast("历史对话已删除", "success", 2200);
    } catch (e) {
        showToast("删除失败：" + e.message, "error");
    }
}

async function openSession(nextSessionId) {
    sessionId = nextSessionId;
    localStorage.setItem("ACTIVE_SESSION_ID", sessionId);
    chatLog.innerHTML =
        '<div style="text-align:center;padding:40px;color:var(--muted)">'
        + '<span style="width:24px;height:24px;border:2px solid var(--primary-soft);border-top-color:var(--primary);border-radius:50%;animation:spin 0.6s linear infinite;display:inline-block;"></span>'
        + ' 加载历史消息...</div>';

    try {
        const messages = await fetchJson(`/api/chat/messages?sessionId=${encodeURIComponent(sessionId)}`).catch(() => []);
        chatLog.innerHTML = "";
        if (!messages.length) {
            renderWelcome();
        } else {
            messages.forEach(message => appendBubble(message.role, message.content, message.id));
        }
    } catch (e) {
        chatLog.innerHTML = "";
        renderWelcome();
    }

    loadSessions();
}

// ── Delete Message ───────────────────────────────────────────────────
async function deleteMessage(messageId, bubble) {
    try {
        await fetchJson(`/api/chat/messages?messageId=${encodeURIComponent(messageId)}`, { method: "DELETE" });
        bubble.style.opacity = "0";
        bubble.style.transform = "scale(0.9)";
        bubble.style.transition = "all 0.2s ease-out";
        setTimeout(() => bubble.remove(), 200);
        showToast("消息已删除", "info", 2000);
    } catch (e) {
        showToast("删除失败：" + e.message, "error");
    }
}

// ── Fetch JSON ───────────────────────────────────────────────────────
async function fetchJson(path, options) {
    const response = await fetch(apiUrl(path), options);
    const payload = await response.json();
    if (!response.ok || !payload.success) {
        throw new Error(payload.message || "请求失败");
    }
    return payload.data;
}

// ── Welcome Message ──────────────────────────────────────────────────
function renderWelcome() {
    appendBubble("assistant",
        "你好，我是智能导诊助手。请描述你的症状（持续时间、年龄性别、体温、既往病史等），"
        + "我会结合医院知识库为你提供导诊建议。\n\n"
        + "如需挂号，请切换到「智能挂号」模式并补充身份信息。");
}

// ── Keyboard Shortcuts ───────────────────────────────────────────────
document.addEventListener("keydown", (e) => {
    // Ctrl+K / Cmd+K → focus composer
    if ((e.ctrlKey || e.metaKey) && e.key === "k") {
        e.preventDefault();
        messageInput.focus();
    }
    // Escape → close dialogs
    if (e.key === "Escape" && adminLoginDialog.open) {
        adminLoginDialog.close();
    }
});
