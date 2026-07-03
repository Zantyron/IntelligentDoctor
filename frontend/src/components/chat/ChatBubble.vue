<script setup lang="ts">
import { computed, ref, onBeforeUnmount } from "vue"
import type { ChatMessage } from "@/types"
import { renderMarkdown } from "@/lib/markdown"
import { copyText } from "@/lib/api"

const props = defineProps<{
  message: ChatMessage
}>()

const copied = ref(false)

const htmlContent = computed(() =>
  props.message.role === "assistant" && props.message.content
    ? renderMarkdown(props.message.content)
    : "",
)

async function onCopy() {
  await copyText(props.message.content)
  copied.value = true
  setTimeout(() => (copied.value = false), 2000)
}

// 思考提示语轮换
const thinkingTexts = [
  "正在分析您的症状...",
  "正在匹配相关科室...",
  "正在查找专业医生建议...",
  "正在整理推荐内容...",
]
const thinkingIndex = ref(0)
let thinkingTimer: ReturnType<typeof setInterval> | null = null

if (typeof window !== "undefined") {
  thinkingTimer = setInterval(() => {
    thinkingIndex.value = (thinkingIndex.value + 1) % thinkingTexts.length
  }, 2500)
}
onBeforeUnmount(() => {
  if (thinkingTimer) clearInterval(thinkingTimer)
})
</script>

<template>
  <div
    class="flex gap-2.5"
    :class="
      message.role === 'user'
        ? 'flex-row-reverse'
        : message.role === 'system'
          ? 'justify-center'
          : ''
    "
  >
    <!-- 助手头像 -->
    <div
      v-if="message.role === 'assistant'"
      class="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-100 text-sm font-bold text-brand-600"
      aria-hidden="true"
    >
      助
    </div>

    <!-- 气泡内容 -->
    <div
      class="max-w-[75%]"
      :class="message.role === 'system' ? 'max-w-full' : ''"
    >
      <!-- 用户消息 -->
      <div v-if="message.role === 'user'" class="bubble-user">
        {{ message.content }}
      </div>

      <!-- 系统消息 -->
      <div v-else-if="message.role === 'system'" class="bubble-system">
        {{ message.content }}
      </div>

      <!-- 助手消息 -->
      <div v-else class="bubble-assistant group relative">
        <!-- 正在思考动画 -->
        <div
          v-if="message.streaming && !message.content"
          class="thinking-indicator flex flex-col gap-3 py-2"
        >
          <!-- 思考图标 + 波浪点 -->
          <div class="flex items-center gap-3">
            <!-- 旋转的大脑图标 -->
            <div class="thinking-icon relative flex h-9 w-9 shrink-0 items-center justify-center">
              <div class="absolute inset-0 animate-ping rounded-full bg-brand-100 opacity-60" />
              <svg
                class="relative z-10 h-5 w-5 animate-thinking-pulse text-brand-500"
                fill="none"
                viewBox="0 0 24 24"
                stroke="currentColor"
                stroke-width="2"
              >
                <path
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456zM16.894 20.567L16.5 21.75l-.394-1.183a2.25 2.25 0 00-1.423-1.423L13.5 18.75l1.183-.394a2.25 2.25 0 001.423-1.423l.394-1.183.394 1.183a2.25 2.25 0 001.423 1.423l1.183.394-1.183.394a2.25 2.25 0 00-1.423 1.423z"
                />
              </svg>
            </div>

            <!-- 波浪跳动点 + 文字 -->
            <div class="flex flex-col gap-1.5">
              <div class="flex items-center gap-1.5">
                <span class="thinking-dot h-2 w-2 rounded-full bg-brand-400" style="animation-delay: 0s" />
                <span class="thinking-dot h-2 w-2 rounded-full bg-brand-500" style="animation-delay: 0.2s" />
                <span class="thinking-dot h-2 w-2 rounded-full bg-brand-600" style="animation-delay: 0.4s" />
              </div>
              <span class="text-sm font-semibold text-brand-700">{{ thinkingTexts[thinkingIndex] }}</span>
            </div>
          </div>

          <!-- 进度条 -->
          <div class="thinking-progress mx-1 h-1 overflow-hidden rounded-full bg-brand-100">
            <div class="h-full w-1/3 animate-thinking-slide rounded-full bg-gradient-to-r from-brand-300 via-brand-500 to-brand-300" />
          </div>
        </div>

        <!-- Markdown 渲染 -->
        <div v-else-if="htmlContent" class="prose-chat" v-html="htmlContent" />

        <!-- 纯文本 -->
        <div v-else class="whitespace-pre-wrap leading-relaxed">{{ message.content }}</div>

        <!-- 流式输出光标 -->
        <span
          v-if="message.streaming && message.content"
          class="ml-0.5 inline-block h-[1.2em] w-[2px] animate-pulse rounded-full bg-brand-500 align-text-bottom"
        />

        <!-- 复制按钮 -->
        <button
          v-if="!message.streaming && message.content"
          class="absolute -bottom-1 right-2 translate-y-full rounded-md bg-white px-2 py-1 text-xs font-semibold text-gray-400 opacity-0 ring-1 ring-gray-200/50 transition-all duration-200 hover:text-brand-600 group-hover:opacity-100"
          @click="onCopy"
        >
          {{ copied ? "已复制 ✓" : "复制" }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 思考动画 - 波浪跳动 */
.thinking-dot {
  animation: thinking-bounce 0.8s ease-in-out infinite;
}

@keyframes thinking-bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-6px);
  }
}

/* 思考进度条滑动 */
.thinking-progress .animate-thinking-slide {
  animation: thinking-slide 1.5s ease-in-out infinite;
}

@keyframes thinking-slide {
  0% {
    transform: translateX(-100%);
  }
  100% {
    transform: translateX(400%);
  }
}

/* 星星图标闪烁 */
.animate-thinking-pulse {
  animation: thinking-icon-pulse 1.5s ease-in-out infinite;
}

@keyframes thinking-icon-pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.6;
    transform: scale(1.15);
  }
}
</style>
