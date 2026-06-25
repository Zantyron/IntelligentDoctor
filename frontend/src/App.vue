<script setup lang="ts">
import { computed, onMounted, onBeforeUnmount, ref } from "vue"
import AppHeader from "@/components/layout/AppHeader.vue"
import ConversationSidebar from "@/components/layout/ConversationSidebar.vue"
import ChatLog from "@/components/chat/ChatLog.vue"
import ChatInput from "@/components/chat/ChatInput.vue"
import RecommendationDrawer from "@/components/recommendation/RecommendationDrawer.vue"
import ToastContainer from "@/components/ui/ToastContainer.vue"
import ConfirmDialog from "@/components/ui/ConfirmDialog.vue"
import { createSessionId, fetchJson } from "@/lib/api"
import { streamChat } from "@/lib/sse"
import type {
  ChatMessage,
  ChatResultMetadata,
  ChatSession,
  RegistrationDraft,
  RegistrationOrder,
} from "@/types"

// ========= 鍏变韩缁堢妯″紡 =========
// 鍖婚櫌鍏辩敤璁惧锛氫笉鎸佷箙鍖?sessionId锛屾瘡娆″姞杞介兘鍏ㄦ柊寮€濮?
// 瓒呰繃绌洪棽鏃堕棿鑷姩閲嶇疆锛屼繚鎶ゆ偅鑰呴殣绉?
const IDLE_TIMEOUT_MS = 60 * 1000 // 1 minute of inactivity locks the shared terminal.

// ========= Toast =========
const toastRef = ref<InstanceType<typeof ToastContainer> | null>(null)
function toast(msg: string, type: "success" | "error" | "info" = "info") {
  toastRef.value?.show(msg, type)
}

// ========= 绌洪棽璁℃椂鍣?=========
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
  loadSessions()
  // 鐩戝惉鐢ㄦ埛娲诲姩浠ラ噸缃┖闂茶鏃?
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

const hasRecommendationData = computed(() => {
  const m = resultMetadata.value
  if (!m) return false
  return (
    (m.recommendations?.length ?? 0) > 0 ||
    (m.evidence?.length ?? 0) > 0 ||
    (m.metadata?.appointmentOptions?.length ?? 0) > 0
  )
})

// 鍙充晶闈㈡澘鏄惁鎵撳紑
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
  toast("宸插紑濮嬫柊瀵硅瘽", "info")
}

async function openSession(id: string) {
  if (idleLocked.value) {
    // 绌洪棽閿佸畾鏃讹紝鐐瑰巻鍙插璇濈浉褰撲簬"鎭㈠"
    idleLocked.value = false
    resetIdleTimer()
  }
  sessionId.value = id
  await loadMessages(id)
  loadSessions()
  resetIdleTimer()
}

function endVisit() {
  confirmDialog.value = {
    title: "确定要结束本次问诊吗？",
    message: "结束问诊只会清空当前屏幕，不会删除已保存的历史记录。如需保护隐私，请使用“删除记录”。",
    action: async () => {
      fullReset()
      await loadSessions()
      toast("问诊已结束，界面已清空", "info")
    },
  }
}

function deleteCurrentSession() {
  confirmDialog.value = {
    title: "删除本次聊天记录？",
    message: "删除后，当前问诊内容会从历史对话中移除，下一位患者无法再从左侧历史恢复查看。",
    action: async () => {
      try {
        await fetchJson(`/api/chat/sessions?sessionId=${encodeURIComponent(sessionId.value)}`, {
          method: "DELETE",
        })
        fullReset()
        await loadSessions()
        toast("本次聊天记录已删除", "success")
      } catch {
        toast("删除失败，请稍后再试", "error")
      }
    },
  }
}

function deleteSession(id: string) {
  confirmDialog.value = {
    title: "删除这条对话？",
    message: "删除后找不回来了，确定要删吗？",
    action: async () => {
      try {
        await fetchJson(`/api/chat/sessions?sessionId=${encodeURIComponent(id)}`, {
          method: "DELETE",
        })
        if (id === sessionId.value) newSession()
        await loadSessions()
        resetIdleTimer()
        toast("已删除", "success")
      } catch {
        toast("删除失败，请稍后再试", "error")
      }
    },
  }
}

// ========= AI 鍥炲 =========
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
    const response = await fetch(`/api/registration/confirm`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
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

// ========= 绌洪棽閿佸畾瑕嗙洊 =========
function dismissIdleLock() {
  idleLocked.value = false
  resetIdleTimer()
}
</script>

<template>
  <div class="flex h-dvh flex-col bg-surface-muted">
    <!-- 鏋佺畝澶撮儴 -->
    <AppHeader
      :sidebar-collapsed="sidebarCollapsed"
      @toggle-sidebar="sidebarCollapsed = !sidebarCollapsed"
      @end-visit="endVisit"
      @delete-current="deleteCurrentSession"
    />

    <!-- 涓讳綋涓夋爮甯冨眬 -->
    <div class="flex min-h-0 flex-1">
      <!-- 宸︿晶锛氫細璇濆垪琛紙濮嬬粓鍙锛?-->
      <ConversationSidebar
        :sessions="sessions"
        :active-session-id="sessionId"
        :loading="sessionsLoading"
        :collapsed="sidebarCollapsed"
        @select="openSession"
        @delete="deleteSession"
        @new-session="newSession"
        @end-visit="endVisit"
      />

      <!-- 涓棿锛氳亰澶╁尯 -->
      <main class="relative flex min-h-0 min-w-0 flex-1 flex-col">
        <ChatLog
          :messages="messages"
          :show-welcome="showWelcome"
          @select-prompt="selectPrompt"
        />

        <!-- 搴曢儴杈撳叆鍖猴紙绌洪棽閿佸畾鏃堕殣钘忥級 -->
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
          鏌ョ湅鏅鸿兘鎺ㄨ崘
        </button>

        <!-- 绌洪棽閿佸畾鎻愮ず -->
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

      <!-- 鍙充晶锛氭帹鑽愰潰鏉匡紙榛樿闅愯棌锛屾湁鍐呭鎵嶅睍绀猴級 -->
      <Transition name="slide-right">
        <aside
          v-if="rightPanelOpen"
          class="w-[360px] min-w-[360px] border-l border-gray-100 bg-white"
        >
          <div class="scrollbar-custom h-full overflow-y-auto">
            <div class="p-4 space-y-4">
              <!-- 鏌ョ湅寤鸿鎸夐挳 / 鍐呭 -->
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

    <ToastContainer ref="toastRef" />

    <ConfirmDialog
      v-if="confirmDialog"
      :title="confirmDialog.title"
      :message="confirmDialog.message"
      confirm-label="纭畾鍒犻櫎"
      @confirm="onConfirmDialog"
      @cancel="confirmDialog = null"
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



