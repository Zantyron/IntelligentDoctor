<script setup lang="ts">
/**
 * 左侧会话侧边栏 - Codex 风格
 * 始终可见，列出所有历史对话
 */
import type { ChatSession } from "@/types"

defineProps<{
  sessions: ChatSession[]
  activeSessionId: string
  loading?: boolean
  collapsed?: boolean
}>()

defineEmits<{
  select: [sessionId: string]
  delete: [sessionId: string]
  newSession: []
  endVisit: []
  toggleCollapse: []
}>()
</script>

<template>
  <aside
    class="flex h-full flex-col border-r border-gray-100 bg-gray-50/50 transition-all duration-300"
    :class="collapsed ? 'w-0 overflow-hidden border-0 lg:w-[260px] lg:min-w-[260px] lg:border-r' : 'w-[260px] min-w-[260px]'"
  >
    <!-- 顶部操作 -->
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
        结束就诊
      </button>
    </div>

    <!-- 会话列表 -->
    <div class="scrollbar-custom flex-1 overflow-y-auto px-2 py-2">
      <!-- 加载骨架 -->
      <div v-if="loading" class="space-y-2 p-1">
        <div v-for="i in 4" :key="i" class="skeleton h-10 rounded-lg" />
      </div>

      <!-- 空状态 -->
      <div v-else-if="!sessions.length" class="flex flex-col items-center justify-center py-10 text-center">
        <p class="text-sm text-gray-400">暂无历史对话</p>
      </div>

      <!-- 列表 -->
      <ul v-else class="space-y-1">
        <li v-for="session in sessions" :key="session.sessionId">
          <div
            class="group relative overflow-hidden rounded-lg transition-colors"
            :class="
              session.sessionId === activeSessionId
                ? 'bg-white shadow-sm ring-1 ring-brand-200'
                : 'hover:bg-white/60'
            "
          >
            <button
              type="button"
              class="w-full px-3 py-2.5 text-left"
              @click="$emit('select', session.sessionId)"
            >
              <p
                class="truncate text-sm font-medium leading-snug"
                :class="session.sessionId === activeSessionId ? 'text-brand-700' : 'text-gray-700'"
              >
                {{ session.title || "新对话" }}
              </p>
            </button>
            <!-- 删除按钮 -->
            <button
              type="button"
              class="absolute right-1 top-1/2 -translate-y-1/2 rounded p-1 text-gray-300 opacity-0 transition-all hover:bg-red-50 hover:text-red-500 group-hover:opacity-100"
              title="删除此对话"
              @click.stop="$emit('delete', session.sessionId)"
            >
              <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673a2.25 2.25 0 01-2.244 2.077H8.084a2.25 2.25 0 01-2.244-2.077L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916m7.5 0a48.667 48.667 0 00-7.5 0" />
              </svg>
            </button>
          </div>
        </li>
      </ul>
    </div>

    <!-- 底部信息 -->
    <div class="shrink-0 border-t border-gray-100 px-3 py-2 text-center">
      <p class="text-[11px] text-gray-400">
        {{ sessions.length }} 条对话记录
      </p>
    </div>
  </aside>
</template>
