<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from "vue"
import AppHeader from "@/components/layout/AppHeader.vue"
import ConversationSidebar from "@/components/layout/ConversationSidebar.vue"
import ChatLog from "@/components/chat/ChatLog.vue"
import ChatInput from "@/components/chat/ChatInput.vue"
import RecommendationDrawer from "@/components/recommendation/RecommendationDrawer.vue"
import ToastContainer from "@/components/ui/ToastContainer.vue"
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue"
import AdminLoginDialog from "@/components/admin/AdminLoginDialog.vue"
import { apiUrl, createSessionId, fetchJson, terminalAuthHeader } from "@/lib/api"
import { streamChat } from "@/lib/sse"
import type {
  ChatMessage,
  ChatResultMetadata,
  ChatSession,
  RegistrationDraft,
  RegistrationOrder,
} from "@/types"

// ========= 共享终端模式 =========
// 医院共用设备：不持久化 sessionId，每次加载都全新开始
// 超过空闲时间自动重置，保护患者隐私
const IDLE_TIMEOUT_MS = 60 * 1000 // 1 minute of inactivity locks the shared terminal.

// ========= Toast =========
const toastRef = ref<InstanceType<typeof ToastContainer> | null>(null)
function toast(msg: string, type: "success" | "error" | "info" = "info") {
  toastRef.value?.show(msg, type)
}

// ========= 导诊终端登录 =========
const terminalUsername = ref(sessionStorage.getItem("TERMINAL_USERNAME") || "")
const terminalToken = ref(sessionStorage.getItem("TERMINAL_TOKEN") || "")
const terminalLoginUsername = ref("")
const terminalLoginPassword = ref("")
const terminalLoginLoading = ref(false)
const terminalLoginError = ref("")
const terminalAuthenticated = computed(() => !!terminalToken.value)

// ========= 空闲计时器 =========
let idleTimer: ReturnType<typeof setTimeout> | null = null
let idleLocked = ref(false)

function resetIdleTimer() {
  if (idleTimer) clearTimeout(idleTimer)
  if (idleLocked.value) return
  idleTimer = setTimeout(() => {
    if (messages.value.length > 0) {
      idleLocked.value = true
      toast("长时间未操作，已自动保护您的隐私。", "info")
    }
  }, IDLE_TIMEOUT_MS)
}

function onUserActivity() {
  resetIdleTimer()
}

// ========= Session =========
const sessionId = ref(createSessionId())
const sessions = ref<ChatSession[]>([])
const sessionsLoading = ref(false)
const sidebarCollapsed = ref(false)

onMounted(() => {
  if (terminalAuthenticated.value) {
    loadSessions()
  }
  // 监听用户活动以重置空闲计时器
  document.addEventListener("click", onUserActivity)
  document.addEventListener("keydown", onUserActivity)
  document.addEventListener("touchstart", onUserActivity)
  resetIdleTimer()
})

onBeforeUnmount(() => {
  if (idleTimer) clearTimeout(idleTimer)
  document.removeEventListener("click", onUserActivity)
  document.removeEventListener("keydown", onUserActivity)
  document.removeEventListener("touchstart", onUserActivity)
})

async function loadSessions() {
  if (!terminalAuthenticated.value) return
  sessionsLoading.value = true
  try {
    sessions.value = await fetchJson<ChatSession[]>("/api/chat/sessions").catch(() => [])
  } finally {
    sessionsLoading.value = false
  }
}

// ========= Chat =========
const messages = ref<ChatMessage[]>([])
const input = ref("")
const sending = ref(false)
const streamHint = ref("")
const showWelcome = computed(() => messages.value.length === 0 || idleLocked.value)

const resultMetadata = ref<ChatResultMetadata | null>(null)
const showRecommendation = ref(false)
const currentDraft = ref<RegistrationDraft | null>(null)
const currentOrder = ref<RegistrationOrder | null>(null)
const confirming = ref(false)

const confirmDialog = ref<{ title: string; message: string; action: () => void } | null>(null)

const showAdminLogin = ref(false)

const hasRecommendationData = computed(() => {
  const m = resultMetadata.value
  if (!m) return false
  return (
    (m.recommendations?.length ?? 0) > 0 ||
    (m.evidence?.length ?? 0) > 0 ||
    (m.metadata?.appointmentOptions?.length ?? 0) > 0
  )
})

// 右侧面板是否打开
const rightPanelOpen = computed(() =>
  showRecommendation.value || !!currentDraft.value || !!currentOrder.value
)

const showRecommendationEntry = computed(() =>
  hasRecommendationData.value && !showRecommendation.value && !currentDraft.value && !currentOrder.value
)

function fullReset() {
  messages.value = []
  resultMetadata.value = null
  showRecommendation.value = false
  currentDraft.value = null
  currentOrder.value = null
  input.value = ""
  sessionId.value = createSessionId()
  idleLocked.value = false
  resetIdleTimer()
}

async function loadMessages(id: string) {
  fullReset()
  sessionId.value = id
  try {
    const data = await fetchJson<
      Array<{ id: string; role: ChatMessage["role"]; content: string }>
    >(`/api/chat/messages?sessionId=${encodeURIComponent(id)}`).catch(() => [])
    if (data.length) {
      messages.value = data.map((m) => ({ id: m.id, role: m.role, content: m.content }))
    }
  } catch {
    /* keep empty */
  }
}

async function loginTerminal() {
  terminalLoginError.value = ""
  if (!terminalLoginUsername.value.trim() || !terminalLoginPassword.value) {
    terminalLoginError.value = "请输入导诊终端账号和密码"
    return
  }
  terminalLoginLoading.value = true
  try {
    const response = await fetch(apiUrl("/api/auth/terminal/login"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: terminalLoginUsername.value.trim(),
        password: terminalLoginPassword.value,
      }),
    })
    const payload = await response.json()
    if (!response.ok || !payload.success) {
      throw new Error(payload.message || "登录失败")
    }
    sessionStorage.setItem("TERMINAL_TOKEN", payload.data.token)
    sessionStorage.setItem("TERMINAL_USERNAME", payload.data.username)
    terminalToken.value = payload.data.token
    terminalUsername.value = payload.data.username
    terminalLoginUsername.value = ""
    terminalLoginPassword.value = ""
    fullReset()
    await loadSessions()
    toast("导诊终端已登录", "success")
  } catch (e) {
    terminalLoginError.value = e instanceof Error ? e.message : "登录失败，请稍后再试"
  } finally {
    terminalLoginLoading.value = false
  }
}

function logoutTerminal() {
  sessionStorage.removeItem("TERMINAL_TOKEN")
  sessionStorage.removeItem("TERMINAL_USERNAME")
  terminalToken.value = ""
  terminalUsername.value = ""
  fullReset()
  sessions.value = []
}

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

function newSession() {
  sessionId.value = createSessionId()
  messages.value = []
  resultMetadata.value = null
  showRecommendation.value = false
  currentDraft.value = null
  currentOrder.value = null
  input.value = ""
  idleLocked.value = false
  resetIdleTimer()
  loadSessions()
  toast("已开始新对话", "info")
}

function endVisit() {
  confirmDialog.value = {
    title: "确定要结束本次问诊吗？",
    message: "结束后会进入新的聊天页面。历史记录仅在患者端匿名展示，具体内容需管理员登录后查看和删除。",
    action: async () => {
      fullReset()
      await loadSessions()
      toast("问诊已结束，界面已清空", "info")
    },
  }
}

function openAdmin() {
  showAdminLogin.value = true
}

function onAdminLoginSuccess(username: string) {
  toast(`欢迎，${username}`, "success")
}

// ========= AI 回复 =========
function applyResult(metadata: ChatResultMetadata) {
  resultMetadata.value = metadata
  showRecommendation.value = false

  const draft = metadata.metadata?.draft
  if (draft) {
    currentDraft.value = draft
    currentOrder.value = null
    toast("已为您准备好挂号信息，请往下填写确认", "info")
  }
  resetIdleTimer()
}

async function sendMessage() {
  const content = input.value.trim()
  if (!content || sending.value || idleLocked.value) return

  sending.value = true
  streamHint.value = "正在帮您分析，请稍等..."
  input.value = ""

  messages.value.push({ role: "user", content })
  const assistantIndex = messages.value.length
  messages.value.push({ role: "assistant", content: "", streaming: true })

  resultMetadata.value = null
  showRecommendation.value = false
  currentOrder.value = null

  try {
    await streamChat(
      "/api/chat/registration/stream",
      {
        sessionId: sessionId.value,
        messages: [{ role: "user", content }],
        consentToStoreHistory: true,
      },
      {
        onMeta: (text) => {
          streamHint.value = text
        },
        onChunk: (_chunk, assembled) => {
          messages.value[assistantIndex] = {
            role: "assistant",
            content: assembled,
            streaming: true,
          }
        },
        onResult: applyResult,
        onError: (msg) => {
          messages.value[assistantIndex] = {
            role: "assistant",
            content: `抱歉，出了点问题：${msg}。\n\n请稍后再试，或重新描述您的症状。`,
            streaming: false,
          }
          toast("回复出错，请稍后再试", "error")
        },
      },
    )

    const final = messages.value[assistantIndex]
    if (final.streaming) {
      messages.value[assistantIndex] = { ...final, streaming: false }
    }

    await loadSessions()
  } catch {
    messages.value[assistantIndex] = {
      role: "assistant",
      content: "抱歉，网络不太稳定，请检查一下网络后再试。",
      streaming: false,
    }
    toast("发送失败，请稍后再试", "error")
  } finally {
    sending.value = false
    streamHint.value = ""
    resetIdleTimer()
  }
}

// ========= Registration =========
async function confirmRegistration(form: {
  patientName: string
  patientPhone: string
  idCard: string
  gender: string
  age: number
}) {
  if (!currentDraft.value || confirming.value) return

  confirming.value = true
  try {
    const response = await fetch(apiUrl("/api/registration/confirm"), {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(terminalAuthHeader() ? { Authorization: terminalAuthHeader() as string } : {}),
      },
      body: JSON.stringify({
        draftId: currentDraft.value.draftId,
        sessionId: sessionId.value,
        idempotencyKey: `confirm-${currentDraft.value.draftId}`,
        ...form,
      }),
    })
    const payload = await response.json()
    if (!response.ok || !payload.success) {
      throw new Error(payload.message || "挂号失败")
    }

    currentOrder.value = payload.data
    currentDraft.value = null
    messages.value.push({
      role: "system",
      content: `挂号成功！订单号 ${payload.data.orderNo}，请按时就诊。`,
    })
    toast("挂号成功！", "success")
  } catch (e) {
    toast(e instanceof Error ? e.message : "挂号失败，请稍后再试", "error")
  } finally {
    confirming.value = false
    resetIdleTimer()
  }
}
function selectPrompt(prompt: string) {
  input.value = prompt
  resetIdleTimer()
}

function onConfirmDialog() {
  confirmDialog.value?.action()
  confirmDialog.value = null
  resetIdleTimer()
}

// ========= 空闲锁定覆盖 =========
function dismissIdleLock() {
  idleLocked.value = false
  resetIdleTimer()
}
</script>

<template>
  <div class="flex h-dvh flex-col bg-surface-muted">
    <div v-if="!terminalAuthenticated" class="flex min-h-dvh items-center justify-center bg-surface-muted px-4">
      <div class="w-full max-w-md rounded-2xl border border-gray-100 bg-white p-7 shadow-xl">
        <div class="mb-6 text-center">
          <div class="mx-auto mb-3 flex h-12 w-12 items-center justify-center rounded-xl bg-brand-500 text-xl font-bold text-white">
            医
          </div>
          <h1 class="text-2xl font-bold text-gray-900">导诊终端登录</h1>
          <p class="mt-2 text-sm text-gray-500">请输入医院后台创建的终端账号后使用智能导诊。</p>
        </div>

        <form class="space-y-4" @submit.prevent="loginTerminal">
          <label class="block">
            <span class="text-sm font-semibold text-gray-700">终端账号</span>
            <input
              v-model="terminalLoginUsername"
              class="input-field mt-1.5"
              autocomplete="username"
              placeholder="例如 machine-01 / frontdesk-02"
              :disabled="terminalLoginLoading"
            />
          </label>
          <label class="block">
            <span class="text-sm font-semibold text-gray-700">密码</span>
            <input
              v-model="terminalLoginPassword"
              type="password"
              class="input-field mt-1.5"
              autocomplete="current-password"
              placeholder="请输入密码"
              :disabled="terminalLoginLoading"
            />
          </label>
          <p v-if="terminalLoginError" class="rounded-lg bg-red-50 px-3 py-2 text-sm font-semibold text-red-600">
            {{ terminalLoginError }}
          </p>
          <button type="submit" class="btn-primary w-full" :disabled="terminalLoginLoading">
            {{ terminalLoginLoading ? "登录中..." : "进入智能导诊" }}
          </button>
        </form>

        <button type="button" class="btn-ghost mt-4 w-full text-sm" @click="openAdmin">
          管理员登录后台
        </button>
      </div>
    </div>

    <template v-else>
    <!-- 极简头部 -->
    <AppHeader
      :sidebar-collapsed="sidebarCollapsed"
      :terminal-username="terminalUsername"
      @toggle-sidebar="toggleSidebar"
      @end-visit="endVisit"
      @open-admin-login="openAdmin"
      @logout-terminal="logoutTerminal"
    />

    <!-- 主体三栏布局 -->
    <div class="flex min-h-0 flex-1">
      <!-- 左侧：会话列表（始终可见）-->
      <ConversationSidebar
        :sessions="sessions"
        :active-session-id="sessionId"
        :loading="sessionsLoading"
        :collapsed="sidebarCollapsed"
        @new-session="newSession"
        @end-visit="endVisit"
        @select-session="loadMessages"
      />

      <!-- 中间：聊天区 -->
      <main class="relative flex min-h-0 min-w-0 flex-1 flex-col">
        <ChatLog
          :messages="messages"
          :show-welcome="showWelcome"
          @select-prompt="selectPrompt"
        />

        <!-- 底部输入区（空闲锁定时隐藏） -->
        <ChatInput
          v-if="!idleLocked"
          v-model="input"
          :sending="sending"
          :hint="streamHint"
          @submit="sendMessage"
        />

        <button
          v-if="showRecommendationEntry"
          type="button"
          class="absolute right-4 top-4 z-10 hidden items-center gap-2 rounded-full border border-brand-200 bg-white px-3 py-2 text-sm font-semibold text-brand-700 shadow-sm transition-all hover:border-brand-300 hover:bg-brand-50 lg:inline-flex"
          @click="showRecommendation = true"
        >
          <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M12 18.75a6.75 6.75 0 006.75-6.75 6.75 6.75 0 00-13.5 0A6.75 6.75 0 0012 18.75zm0 0v2.25m-3 0h6" />
          </svg>
          查看智能推荐
        </button>

        <!-- 空闲锁定提示 -->
        <div
          v-if="idleLocked && messages.length > 0"
          class="shrink-0 bg-gradient-to-t from-white/98 to-transparent px-4 pb-6 pt-6"
        >
          <div class="mx-auto max-w-[680px] text-center">
            <div class="rounded-xl bg-amber-50 border border-amber-200 p-5">
              <p class="text-base font-bold text-amber-800 mb-2">长时间未操作</p>
              <p class="text-sm text-amber-700 mb-3">为保护您的隐私，界面已自动锁定。您可以继续使用，或开始新对话。</p>
              <div class="flex gap-2 justify-center">
                <button type="button" class="btn-ghost text-sm" @click="dismissIdleLock">
                  继续使用
                </button>
                <button type="button" class="btn-primary text-sm" @click="newSession">
                  开始新对话
                </button>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 右侧：推荐面板（默认隐藏，有内容才展示） -->
      <Transition name="slide-right">
        <aside
          v-if="rightPanelOpen"
          class="w-[360px] min-w-[360px] border-l border-gray-100 bg-white"
        >
          <div class="scrollbar-custom h-full overflow-y-auto">
            <div class="p-4 space-y-4">
              <!-- 查看建议按钮 / 内容 -->
              <RecommendationDrawer
                v-if="hasRecommendationData || currentDraft || currentOrder"
                :show="showRecommendation"
                :metadata="resultMetadata"
                :draft="currentDraft"
                :order="currentOrder"
                :confirming="confirming"
                @update:show="(val: boolean) => showRecommendation = val"
                @confirm="confirmRegistration"
              />
            </div>
          </div>
        </aside>
      </Transition>
    </div>
    </template>

    <ToastContainer ref="toastRef" />

    <ConfirmDialog
      v-if="confirmDialog"
      :title="confirmDialog.title"
      :message="confirmDialog.message"
      confirm-label="确定结束"
      @confirm="onConfirmDialog"
      @cancel="confirmDialog = null"
    />

    <AdminLoginDialog
      :visible="showAdminLogin"
      @close="showAdminLogin = false"
      @success="onAdminLoginSuccess"
    />
  </div>
</template>

<style scoped>
.slide-right-enter-active,
.slide-right-leave-active {
  transition: width 0.3s cubic-bezier(0.16, 1, 0.3, 1), min-width 0.3s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.25s ease;
  overflow: hidden;
}
.slide-right-enter-from,
.slide-right-leave-to {
  width: 0 !important;
  min-width: 0 !important;
  opacity: 0;
}
</style>
