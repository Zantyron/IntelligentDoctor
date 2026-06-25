<script setup lang="ts">
import { onBeforeUnmount, ref } from "vue"

const props = defineProps<{
  disabled?: boolean
  text?: string
}>()

const emit = defineEmits<{
  result: [text: string]
  status: [state: "idle" | "listening" | "processing" | "error"]
}>()

type VoiceState = "idle" | "listening" | "error"
type SrConstructor = new () => SpeechRecognition

const SpeechRecognitionAPI: SrConstructor | undefined =
  (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition

const state = ref<VoiceState>("idle")
const errorMsg = ref("")
const liveText = ref("")
const isSupported = ref(!!SpeechRecognitionAPI)

let recognition: SpeechRecognition | null = null
let baseText = ""
let finalTranscript = ""
let shouldFlushOnEnd = true

function composeText(interim = "") {
  return [baseText, finalTranscript, interim].filter(Boolean).join(baseText ? " " : "").trim()
}

function flush(interim = "") {
  const value = composeText(interim)
  liveText.value = value
  emit("result", value)
}

function stopRecognition(flushResult = true) {
  shouldFlushOnEnd = flushResult
  if (recognition) {
    try {
      recognition.stop()
    } catch {
      /* ignore */
    }
    recognition = null
  }
}

function startListening() {
  if (props.disabled) return
  if (!SpeechRecognitionAPI) {
    errorMsg.value = "当前浏览器不支持语音识别，请使用 Chrome 或 Edge。"
    state.value = "error"
    emit("status", "error")
    setTimeout(() => {
      state.value = "idle"
      emit("status", "idle")
    }, 2500)
    return
  }

  stopRecognition(false)
  baseText = (props.text || "").trim()
  finalTranscript = ""
  liveText.value = baseText
  errorMsg.value = ""
  shouldFlushOnEnd = true

  recognition = new SpeechRecognitionAPI()
  recognition.lang = "zh-CN"
  recognition.continuous = true
  recognition.interimResults = true
  recognition.maxAlternatives = 1

  recognition.onresult = (event: SpeechRecognitionEvent) => {
    let interim = ""
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const result = event.results[i]
      if (result.isFinal) {
        finalTranscript += result[0].transcript.trim()
      } else {
        interim += result[0].transcript
      }
    }
    flush(interim.trim())
  }

  recognition.onerror = (event: any) => {
    const messages: Record<string, string> = {
      "not-allowed": "请允许使用麦克风。",
      "no-speech": "没有检测到声音，请再试一次。",
      "audio-capture": "没有检测到麦克风。",
      network: "语音识别需要网络连接。",
    }
    errorMsg.value = messages[event.error] || "语音识别出错，请重试。"
    stopRecognition(false)
    state.value = "error"
    emit("status", "error")
    setTimeout(() => {
      state.value = "idle"
      emit("status", "idle")
    }, 2500)
  }

  recognition.onend = () => {
    recognition = null
    if (shouldFlushOnEnd) flush()
    if (state.value === "listening") {
      state.value = "idle"
      emit("status", "idle")
    }
  }

  try {
    recognition.start()
    state.value = "listening"
    emit("status", "listening")
  } catch {
    errorMsg.value = "语音识别启动失败，请重试。"
    state.value = "error"
    emit("status", "error")
  }
}

function toggleListening() {
  if (props.disabled) return
  if (state.value === "listening") {
    flush()
    state.value = "idle"
    emit("status", "idle")
    stopRecognition(false)
    return
  }
  startListening()
}

onBeforeUnmount(() => {
  stopRecognition(false)
})
</script>

<template>
  <div class="flex items-center gap-2">
    <button
      type="button"
      class="relative flex h-9 w-9 items-center justify-center rounded-full transition-all duration-200"
      :class="{
        'border-2 border-gray-200 bg-white text-gray-500 hover:border-brand-400 hover:text-brand-600': state === 'idle' && !disabled,
        'cursor-not-allowed border-2 border-gray-100 bg-gray-100 text-gray-300': state === 'idle' && disabled,
        'scale-105 bg-red-500 text-white shadow-lg shadow-red-500/30': state === 'listening',
        'border-2 border-red-200 bg-red-100 text-red-600': state === 'error',
      }"
      :disabled="!isSupported || disabled"
      :title="state === 'listening' ? '停止语音识别' : '开始语音识别'"
      :aria-label="state === 'listening' ? '停止语音识别' : '开始语音识别'"
      @click="toggleListening"
    >
      <span
        v-if="state === 'listening'"
        class="absolute inset-0 animate-ping rounded-full bg-red-400 opacity-30"
      />
      <svg v-if="state !== 'listening'" class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2">
        <path stroke-linecap="round" stroke-linejoin="round" d="M12 18.75a6 6 0 006-6v-1.5m-6 7.5a6 6 0 01-6-6v-1.5m6 7.5v3.75m-3.75 0h7.5M12 15.75a3 3 0 01-3-3V4.5a3 3 0 116 0v8.25a3 3 0 01-3 3z" />
      </svg>
      <svg v-else class="h-3.5 w-3.5" fill="currentColor" viewBox="0 0 24 24">
        <rect x="6" y="6" width="4" height="12" rx="1" />
        <rect x="14" y="4" width="4" height="16" rx="1" />
      </svg>
    </button>

    <div v-if="state === 'error'" class="min-w-0 max-w-[180px]">
      <p class="truncate text-xs font-medium text-red-600">{{ errorMsg }}</p>
    </div>
  </div>
</template>
