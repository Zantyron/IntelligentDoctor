<script setup lang="ts">
import { ref, watch, nextTick } from "vue"
import type { ChatMessage } from "@/types"
import ChatBubble from "./ChatBubble.vue"
import WelcomeScreen from "./WelcomeScreen.vue"

const props = defineProps<{
  messages: ChatMessage[]
  showWelcome: boolean
}>()

defineEmits<{
  selectPrompt: [prompt: string]
}>()

const logRef = ref<HTMLElement | null>(null)

watch(
  () => props.messages.length,
  async () => {
    await nextTick()
    if (!logRef.value) return
    const el = logRef.value
    const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 200
    if (nearBottom) {
      el.scrollTo({ top: el.scrollHeight, behavior: "smooth" })
    }
  },
)
</script>

<template>
  <div ref="logRef" class="scrollbar-custom flex-1 overflow-y-auto">
    <WelcomeScreen v-if="showWelcome" @select="$emit('selectPrompt', $event)" />

    <div v-else class="mx-auto max-w-[680px] space-y-3 px-4 py-3">
      <ChatBubble
        v-for="(message, index) in messages"
        :key="message.id || `msg-${index}`"
        :message="message"
      />
    </div>
  </div>
</template>
