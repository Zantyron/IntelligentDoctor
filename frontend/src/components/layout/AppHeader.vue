<script setup lang="ts">
defineProps<{
  sidebarCollapsed: boolean
}>()

defineEmits<{
  toggleSidebar: []
  endVisit: []
  deleteCurrent: []
}>()
</script>

<template>
  <header class="shrink-0 border-b border-gray-100 bg-white/80 backdrop-blur-sm">
    <div class="flex items-center justify-between px-4 py-2.5">
      <div class="flex items-center gap-3">
        <button
          type="button"
          class="btn-ghost flex h-8 w-8 items-center justify-center rounded-lg p-0"
          :aria-label="sidebarCollapsed ? '展开历史对话' : '收起历史对话'"
          @click="$emit('toggleSidebar')"
        >
          <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
          </svg>
        </button>

        <div class="flex items-center gap-2.5">
          <div class="flex h-8 w-8 shrink-0 items-center justify-center rounded-lg bg-brand-500 text-base font-bold text-white" aria-hidden="true">
            医
          </div>
          <div class="hidden sm:block">
            <p class="text-base font-bold leading-tight text-gray-900">智能导诊助手</p>
            <p class="text-[11px] text-gray-500">帮您看该挂什么科</p>
          </div>
        </div>
      </div>

      <div class="flex items-center gap-1.5">
        <button
          type="button"
          class="flex h-8 items-center gap-1.5 rounded-lg border border-red-200 bg-white px-2.5 text-xs font-semibold text-red-600 transition-colors hover:bg-red-50"
          @click="$emit('deleteCurrent')"
        >
          <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M14.74 9l-.346 9m-4.788 0L9.26 9m9.968-3.21c.342.052.682.107 1.022.166m-1.022-.165L18.16 19.673A2.25 2.25 0 0115.916 21H8.084a2.25 2.25 0 01-2.244-1.327L4.772 5.79m14.456 0a48.108 48.108 0 00-3.478-.397m-12 .562c.34-.059.68-.114 1.022-.165m0 0a48.11 48.11 0 013.478-.397m7.5 0v-.916c0-1.18-.91-2.164-2.09-2.201a51.964 51.964 0 00-3.32 0c-1.18.037-2.09 1.022-2.09 2.201v.916" />
          </svg>
          <span class="hidden sm:inline">删除记录</span>
        </button>

        <button
          type="button"
          class="btn-ghost h-8 gap-1.5 rounded-lg px-2.5 text-xs"
          @click="$emit('endVisit')"
        >
          <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
            <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 9V5.25A2.25 2.25 0 0013.5 3h-6a2.25 2.25 0 00-2.25 2.25v13.5A2.25 2.25 0 007.5 21h6a2.25 2.25 0 002.25-2.25V15m3 0l3-3m0 0l-3-3m3 3H9" />
          </svg>
          <span class="hidden sm:inline">结束问诊</span>
        </button>
      </div>
    </div>
  </header>
</template>
