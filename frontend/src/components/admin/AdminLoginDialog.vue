<script setup lang="ts">
import { ref, watch, nextTick } from "vue"
import { apiUrl } from "@/lib/api"

const props = defineProps<{
  visible: boolean
}>()

const emit = defineEmits<{
  close: []
  success: [username: string]
}>()

const username = ref("")
const password = ref("")
const loading = ref(false)
const error = ref("")
const usernameInput = ref<HTMLInputElement | null>(null)

watch(
  () => props.visible,
  async (v) => {
    if (v) {
      username.value = ""
      password.value = ""
      error.value = ""
      await nextTick()
      usernameInput.value?.focus()
    }
  },
)

async function handleLogin() {
  error.value = ""
  if (!username.value.trim() || !password.value.trim()) {
    error.value = "请输入用户名和密码"
    return
  }

  loading.value = true
  try {
    const response = await fetch(apiUrl("/api/auth/login"), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        username: username.value.trim(),
        password: password.value,
      }),
    })

    const payload = await response.json()
    if (!response.ok || !payload.success) {
      throw new Error(payload.message || "登录失败，请检查用户名和密码")
    }

    const data = payload.data
    // 存储 token 到 sessionStorage（admin.js 会用到）
    sessionStorage.setItem("ADMIN_AUTHENTICATED", "true")
    sessionStorage.setItem("ADMIN_TOKEN", data.token)
    sessionStorage.setItem("ADMIN_AUTH_HEADER", `${data.tokenType || "Bearer"} ${data.token}`)
    sessionStorage.setItem("ADMIN_HOSPITAL_ID", data.hospitalId || "")

    emit("success", data.username || username.value)
    // 跳转到管理后台
    window.location.href = "/admin.html"
  } catch (e) {
    error.value = e instanceof Error ? e.message : "登录失败，请稍后再试"
  } finally {
    loading.value = false
  }
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === "Enter") handleLogin()
  if (e.key === "Escape") emit("close")
}
</script>

<template>
  <Teleport to="body">
    <Transition name="dialog">
      <div
        v-if="visible"
        class="fixed inset-0 z-50 flex items-center justify-center"
        @click.self="emit('close')"
        @keydown="onKeydown"
      >
        <!-- 遮罩 -->
        <div class="absolute inset-0 bg-black/30 backdrop-blur-sm" />

        <!-- 弹窗 -->
        <div class="relative z-10 mx-4 w-full max-w-sm overflow-hidden rounded-2xl bg-white shadow-2xl">
          <!-- 头部 -->
          <div class="bg-gradient-to-r from-brand-500 to-brand-600 px-6 py-5">
            <h2 class="text-lg font-bold text-white">管理员登录</h2>
            <p class="mt-1 text-sm text-brand-100">请输入管理员账号以进入后台</p>
          </div>

          <!-- 表单 -->
          <div class="space-y-4 px-6 py-5">
            <!-- 用户名 -->
            <div>
              <label class="mb-1.5 block text-sm font-semibold text-gray-700">用户名</label>
              <div class="relative">
                <svg
                  class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z"
                  />
                </svg>
                <input
                  ref="usernameInput"
                  v-model="username"
                  type="text"
                  autocomplete="username"
                  placeholder="请输入用户名"
                  class="w-full rounded-lg border border-gray-200 py-2.5 pl-9 pr-3 text-sm text-gray-900 placeholder-gray-400 transition-colors focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
                  :disabled="loading"
                />
              </div>
            </div>

            <!-- 密码 -->
            <div>
              <label class="mb-1.5 block text-sm font-semibold text-gray-700">密码</label>
              <div class="relative">
                <svg
                  class="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-gray-400"
                  fill="none"
                  viewBox="0 0 24 24"
                  stroke="currentColor"
                  stroke-width="2"
                >
                  <path
                    stroke-linecap="round"
                    stroke-linejoin="round"
                    d="M16.5 10.5V6.75a4.5 4.5 0 10-9 0v3.75m-.75 11.25h10.5a2.25 2.25 0 002.25-2.25v-6.75a2.25 2.25 0 00-2.25-2.25H6.75a2.25 2.25 0 00-2.25 2.25v6.75a2.25 2.25 0 002.25 2.25z"
                  />
                </svg>
                <input
                  v-model="password"
                  type="password"
                  autocomplete="current-password"
                  placeholder="请输入密码"
                  class="w-full rounded-lg border border-gray-200 py-2.5 pl-9 pr-3 text-sm text-gray-900 placeholder-gray-400 transition-colors focus:border-brand-400 focus:outline-none focus:ring-2 focus:ring-brand-100"
                  :disabled="loading"
                />
              </div>
            </div>

            <!-- 错误信息 -->
            <Transition name="error-fade">
              <div
                v-if="error"
                class="rounded-lg bg-red-50 px-3 py-2.5 text-sm font-medium text-red-600"
              >
                {{ error }}
              </div>
            </Transition>

            <!-- 按钮 -->
            <div class="flex gap-2.5 pt-1">
              <button
                type="button"
                class="flex-1 rounded-lg border border-gray-200 py-2.5 text-sm font-semibold text-gray-600 transition-colors hover:bg-gray-50"
                :disabled="loading"
                @click="emit('close')"
              >
                取消
              </button>
              <button
                type="button"
                class="btn-primary flex-1 gap-2 text-sm disabled:opacity-60"
                :disabled="loading"
                @click="handleLogin"
              >
                <svg
                  v-if="loading"
                  class="h-4 w-4 animate-spin"
                  fill="none"
                  viewBox="0 0 24 24"
                >
                  <circle
                    class="opacity-25"
                    cx="12"
                    cy="12"
                    r="10"
                    stroke="currentColor"
                    stroke-width="4"
                  />
                  <path
                    class="opacity-75"
                    fill="currentColor"
                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
                  />
                </svg>
                {{ loading ? "登录中..." : "登录" }}
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.dialog-enter-active,
.dialog-leave-active {
  transition: opacity 0.25s ease;
}
.dialog-enter-active > :last-child,
.dialog-leave-active > :last-child {
  transition: transform 0.25s ease, opacity 0.25s ease;
}
.dialog-enter-from,
.dialog-leave-to {
  opacity: 0;
}
.dialog-enter-from > :last-child {
  transform: scale(0.95) translateY(8px);
  opacity: 0;
}
.dialog-leave-to > :last-child {
  transform: scale(0.95) translateY(8px);
  opacity: 0;
}

.error-fade-enter-active,
.error-fade-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.error-fade-enter-from,
.error-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
