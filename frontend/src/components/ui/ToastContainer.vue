<script setup lang="ts">
import { ref } from "vue"
import type { ToastItem, ToastType } from "@/types"

const toasts = ref<ToastItem[]>([])
let nextId = 1

function show(message: string, type: ToastType = "info", duration = 4000) {
  const id = nextId++
  toasts.value.push({ id, message, type })
  setTimeout(() => dismiss(id), duration)
}

function dismiss(id: number) {
  toasts.value = toasts.value.filter((t) => t.id !== id)
}

defineExpose({ show, dismiss })
</script>

<template>
  <Teleport to="body">
    <div
      class="pointer-events-none fixed inset-x-0 top-2 z-[100] flex flex-col items-center gap-2 px-4"
      aria-live="polite"
    >
      <TransitionGroup name="toast">
        <div
          v-for="toast in toasts"
          :key="toast.id"
          class="pointer-events-auto w-full max-w-sm cursor-pointer rounded-lg border px-4 py-3 text-sm font-bold shadow-lg"
          :class="{
            'border-green-300 bg-green-50 text-green-900': toast.type === 'success',
            'border-red-300 bg-red-50 text-red-900': toast.type === 'error',
            'border-amber-300 bg-amber-50 text-amber-900': toast.type === 'warning',
            'border-blue-300 bg-blue-50 text-blue-900': toast.type === 'info',
          }"
          @click="dismiss(toast.id)"
        >
          {{ toast.message }}
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(-12px) scale(0.96);
}
</style>
