<script setup lang="ts">
import type { ChatSession } from "@/types"

defineProps<{
  sessions: ChatSession[]
  activeSessionId: string
  loading?: boolean
  collapsed?: boolean
}>()

defineEmits<{
  newSession: []
  endVisit: []
  selectSession: [sessionId: string]
}>()
</script>

<template>
  <aside
    class="flex h-full flex-col border-r border-gray-100 bg-gray-50/50 transition-all duration-300 ease-in-out"
    :class="collapsed ? 'w-0 min-w-0 overflow-hidden border-0 opacity-0' : 'w-[260px] min-w-[260px] opacity-100'"
  >
    <div class="shrink-0 space-y-2 border-b border-gray-100 px-3 py-3">
      <button type="button" class="btn-primary w-full text-sm" @click="$emit('newSession')">
        <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="M12 4.5v15m7.5-7.5h-15" />
        </svg>
        新对话
      </button>

      <button
        type="button"
        class="flex w-full items-center justify-center gap-1.5 rounded-lg border border-red-200 bg-white px-3 py-2 text-xs font-semibold text-red-600 transition-colors hover:bg-red-50 active:bg-red-100"
        @click="$emit('endVisit')"
      >
        <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
          <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
        </svg>
        结束问诊
      </button>
    </div>

    <div class="scrollbar-custom flex-1 overflow-y-auto px-2 py-2">
      <div v-if="loading" class="space-y-2 p-1">
        <div v-for="i in 4" :key="i" class="skeleton h-10 rounded-lg" />
      </div>

      <div v-else-if="!sessions.length" class="flex flex-col items-center justify-center py-10 text-center">
        <p class="text-sm text-gray-400">暂无历史记录</p>
      </div>

      <ul v-else class="space-y-1">
        <li v-for="(session, index) in sessions" :key="session.sessionId">
          <button
            type="button"
            class="w-full rounded-lg bg-white px-3 py-2.5 text-left ring-1 ring-gray-100 transition-colors hover:bg-brand-50"
            :class="session.sessionId === activeSessionId ? 'ring-brand-200' : ''"
            @click="$emit('selectSession', session.sessionId)"
          >
            <p class="text-sm font-semibold leading-snug text-gray-700">对话记录 {{ index + 1 }}</p>
            <p class="mt-0.5 text-[11px] text-gray-400">患者端隐藏内容，管理员可查看</p>
          </button>
        </li>
      </ul>
    </div>

    <div class="shrink-0 border-t border-gray-100 px-3 py-2 text-center">
      <p class="text-[11px] text-gray-400">
        {{ sessions.length }} 条对话记录
      </p>
    </div>
  </aside>
</template>
