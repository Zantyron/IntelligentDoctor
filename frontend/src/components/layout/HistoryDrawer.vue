<script setup lang="ts">
import type { ChatSession } from "@/types"

defineProps<{
  sessions: ChatSession[]
  activeSessionId: string
  loading?: boolean
}>()

defineEmits<{
  select: [sessionId: string]
  delete: [sessionId: string]
  newSession: []
  close: []
}>()
</script>

<template>
  <aside class="flex h-full flex-col bg-white">
    <!-- 标题栏 -->
    <div class="flex items-center justify-between border-b border-surface-border px-4 py-3">
      <h2 class="text-heading font-bold text-gray-900">历史对话</h2>
      <button
        type="button"
        class="btn-ghost px-3 py-1.5 text-sm"
        aria-label="关闭历史对话"
        @click="$emit('close')"
      >
        关闭 ✕
      </button>
    </div>

    <!-- 新对话 -->
    <div class="border-b border-surface-border p-4">
      <button type="button" class="btn-primary w-full" @click="$emit('newSession')">
        ✚ 开始新对话
      </button>
    </div>

    <!-- 会话列表 -->
    <div class="scrollbar-custom flex-1 overflow-y-auto p-3">
      <!-- 加载骨架 -->
      <div v-if="loading" class="space-y-2.5">
        <div v-for="i in 3" :key="i" class="skeleton h-[56px]" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="!sessions.length" class="flex flex-col items-center justify-center py-12 text-center">
        <div class="text-3xl">📋</div>
        <p class="mt-3 text-base text-gray-400">还没有历史对话</p>
      </div>

      <!-- 列表 -->
      <ul v-else class="space-y-2.5">
        <li v-for="session in sessions" :key="session.sessionId">
          <div
            class="overflow-hidden rounded-card border transition-colors"
            :class="
              session.sessionId === activeSessionId
                ? 'border-brand-400 bg-brand-50'
                : 'border-surface-border bg-white'
            "
          >
            <button
              type="button"
              class="w-full px-4 py-3 text-left"
              @click="$emit('select', session.sessionId)"
            >
              <p class="truncate text-base font-bold text-gray-900">
                {{ session.title || "新对话" }}
              </p>
            </button>
            <div class="border-t border-surface-border px-4 pb-2.5 pt-1.5">
              <button
                type="button"
                class="text-xs font-semibold text-red-600 hover:text-red-700"
                @click.stop="$emit('delete', session.sessionId)"
              >
                删除这条
              </button>
            </div>
          </div>
        </li>
      </ul>
    </div>
  </aside>
</template>
