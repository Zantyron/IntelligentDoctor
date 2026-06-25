<script setup lang="ts">
defineProps<{
  title: string
  message: string
  confirmLabel?: string
}>()

defineEmits<{
  confirm: []
  cancel: []
}>()
</script>

<template>
  <Teleport to="body">
    <div
      class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4"
      role="alertdialog"
      aria-modal="true"
    >
      <div class="card w-full max-w-sm p-6 animate-fade-up">
        <div
          class="mx-auto mb-4 flex h-12 w-12 items-center justify-center rounded-full bg-red-100 text-2xl"
        >
          ⚠
        </div>
        <h2 class="text-center text-heading font-bold text-gray-900">{{ title }}</h2>
        <p class="mt-2 text-center text-base text-gray-700">{{ message }}</p>
        <div class="mt-5 grid grid-cols-2 gap-3">
          <button type="button" class="btn-ghost" @click="$emit('cancel')">取消</button>
          <button
            type="button"
            class="btn-danger"
            @click="$emit('confirm')"
          >
            {{ confirmLabel || "确定" }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>
