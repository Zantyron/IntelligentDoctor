<script setup lang="ts">
import { computed, ref, watch } from "vue"
import VoiceInput from "./VoiceInput.vue"

const props = defineProps<{
  modelValue: string
  sending: boolean
  hint?: string
}>()

const emit = defineEmits<{
  "update:modelValue": [value: string]
  submit: []
}>()

const textareaRef = ref<HTMLTextAreaElement | null>(null)
const voiceState = ref<"idle" | "listening" | "processing" | "error">("idle")

const canSubmit = computed(() => props.modelValue.trim() && !props.sending)

watch(
  () => props.modelValue,
  () => {
    const el = textareaRef.value
    if (!el) return
    el.style.height = "auto"
    el.style.height = `${Math.min(el.scrollHeight, 120)}px`
  },
)

function onKeydown(event: KeyboardEvent) {
  if (event.key !== "Enter" || event.shiftKey || event.isComposing) return
  event.preventDefault()
  if (canSubmit.value) emit("submit")
}

function onVoiceResult(text: string) {
  emit("update:modelValue", text)
}
</script>

<template>
  <div
    v-if="sending"
    class="mx-auto flex w-full max-w-[680px] items-center gap-2.5 px-4 pb-3"
  >
    <div class="flex w-full items-center gap-2.5 rounded-xl bg-brand-50/80 px-4 py-2.5 backdrop-blur">
      <span class="flex gap-1">
        <span class="h-2 w-2 animate-pulse rounded-full bg-brand-300" />
        <span class="h-2 w-2 animate-pulse rounded-full bg-brand-400 [animation-delay:0.15s]" />
        <span class="h-2 w-2 animate-pulse rounded-full bg-brand-500 [animation-delay:0.3s]" />
      </span>
      <span class="text-sm font-bold text-brand-700">
        {{ hint || "正在帮您分析，请稍等..." }}
      </span>
    </div>
  </div>

  <div class="shrink-0 bg-gradient-to-t from-white/98 via-white/90 to-transparent px-4 pb-4 pt-1">
    <div class="mx-auto max-w-[680px]">
      <div
        class="flex items-end gap-3 rounded-2xl border border-gray-200/80 bg-white px-4 py-3 shadow-sm transition-shadow duration-200 focus-within:border-brand-400/60 focus-within:shadow-md focus-within:shadow-brand-500/5"
      >
        <div class="shrink-0 pb-0.5">
          <VoiceInput
            :disabled="sending"
            :text="modelValue"
            @result="onVoiceResult"
            @status="voiceState = $event"
          />
        </div>

        <div class="min-w-0 flex-1">
          <textarea
            ref="textareaRef"
            :value="modelValue"
            rows="1"
            class="block w-full resize-none border-0 bg-transparent px-1 py-2 text-base leading-relaxed text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-0"
            :placeholder="voiceState === 'listening'
              ? '正在听您说话，再点一次麦克风停止识别...'
              : '请说出您的症状，也可以点击麦克风语音输入...'"
            :disabled="sending"
            aria-label="输入您的症状"
            @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
            @keydown="onKeydown"
          />
        </div>

        <div class="flex shrink-0 items-center gap-1.5">
          <button
            type="button"
            class="flex h-9 w-9 items-center justify-center rounded-full transition-all duration-200"
            :class="canSubmit
              ? 'bg-brand-500 text-white shadow-sm hover:bg-brand-600 active:scale-95'
              : 'cursor-not-allowed bg-gray-100 text-gray-400'"
            :disabled="!canSubmit"
            :aria-label="sending ? '发送中' : '发送消息'"
            @click="$emit('submit')"
          >
            <svg v-if="!sending" class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
              <path stroke-linecap="round" stroke-linejoin="round" d="M6 12L3.269 3.126A59.768 59.768 0 0121.485 12 59.77 59.77 0 013.27 20.876L5.999 12zm0 0h7.5" />
            </svg>
            <svg v-else class="h-4 w-4 animate-spin" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" />
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
            </svg>
          </button>
        </div>
      </div>

      <p v-if="voiceState === 'listening'" class="mt-2 text-center text-xs font-medium text-red-600">
        语音识别中。再次点击麦克风停止，点击发送按钮才会发送。
      </p>
    </div>
  </div>
</template>
