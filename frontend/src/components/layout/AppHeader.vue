<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from "vue"

defineProps<{
  sidebarCollapsed: boolean
  terminalUsername?: string
}>()

const emit = defineEmits<{
  toggleSidebar: []
  endVisit: []
  openAdminLogin: []
  logoutTerminal: []
}>()

const menuOpen = ref(false)
const menuRef = ref<HTMLDivElement | null>(null)

function closeMenu() {
  menuOpen.value = false
}

function toggleSidebarFromMenu() {
  emit("toggleSidebar")
  closeMenu()
}

function openAdminFromMenu() {
  emit("openAdminLogin")
  closeMenu()
}

function onClickOutside(e: MouseEvent) {
  if (menuRef.value && !menuRef.value.contains(e.target as Node)) {
    closeMenu()
  }
}

onMounted(() => {
  document.addEventListener("click", onClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener("click", onClickOutside)
})
</script>

<template>
  <header class="shrink-0 border-b border-gray-100 bg-white/80 backdrop-blur-sm">
    <div class="flex items-center justify-between px-4 py-2.5">
      <div class="flex items-center gap-3">
        <div ref="menuRef" class="relative">
          <button
            type="button"
            class="btn-ghost flex h-8 w-8 items-center justify-center rounded-lg p-0"
            aria-label="打开菜单"
            :aria-expanded="menuOpen"
            @click.stop="menuOpen = !menuOpen"
          >
            <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
            </svg>
          </button>

          <Transition name="menu-fade">
            <div
              v-if="menuOpen"
              class="absolute left-0 top-10 z-30 w-48 overflow-hidden rounded-xl border border-gray-100 bg-white py-1 shadow-lg"
            >
              <button
                type="button"
                class="flex w-full items-center gap-2 px-3 py-2.5 text-left text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50"
                @click.stop="toggleSidebarFromMenu"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M3.75 6.75h16.5M3.75 12h16.5m-16.5 5.25h16.5" />
                </svg>
                {{ sidebarCollapsed ? "显示历史列表" : "隐藏历史列表" }}
              </button>
              <div class="mx-2 my-1 h-px bg-gray-100" />
              <button
                type="button"
                class="flex w-full items-center gap-2 px-3 py-2.5 text-left text-sm font-semibold text-gray-700 transition-colors hover:bg-gray-50"
                @click.stop="openAdminFromMenu"
              >
                <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
                  <path stroke-linecap="round" stroke-linejoin="round" d="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
                </svg>
                管理员登录
              </button>
            </div>
          </Transition>
        </div>

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

      <div class="flex items-center gap-2">
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
        <button
          v-if="terminalUsername"
          type="button"
          class="btn-ghost h-8 gap-1.5 rounded-lg px-2.5 text-xs"
          @click="$emit('logoutTerminal')"
        >
          <span class="hidden sm:inline">{{ terminalUsername }}</span>
          <span>退出</span>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.menu-fade-enter-active,
.menu-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.menu-fade-enter-from,
.menu-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
