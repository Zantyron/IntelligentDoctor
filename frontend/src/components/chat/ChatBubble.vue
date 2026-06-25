<script setup lang="ts">
import { computed, ref } from "vue"
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
        <div v-if="message.streaming && !message.content" class="flex items-center gap-2 py-0.5">
          <span class="flex gap-1">
            <span class="h-2 w-2 animate-pulse rounded-full bg-brand-300" />
            <span class="h-2 w-2 animate-pulse rounded-full bg-brand-400 [animation-delay:0.15s]" />
            <span class="h-2 w-2 animate-pulse rounded-full bg-brand-500 [animation-delay:0.3s]" />
          </span>
          <span class="text-sm text-gray-500">正在帮您分析...</span>
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
