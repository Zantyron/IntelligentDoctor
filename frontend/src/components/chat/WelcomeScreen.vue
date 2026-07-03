<script setup lang="ts">
defineEmits<{
  select: [prompt: string]
}>()

const prompts = [
  { icon: "🤒", label: "我发烧咳嗽", text: "我发烧咳嗽好几天了，晚上更严重，不知道该挂什么科。" },
  { icon: "😵", label: "我头痛头晕", text: "我最近头痛头晕，有时候还想吐，帮我看看该挂什么科。" },
  { icon: "🤢", label: "我胃不舒服", text: "我胃痛反酸，吃饭后更明显，帮我看看该挂什么科。" },
  { icon: "🏥", label: "我想挂号", text: "我知道要看什么科了，请帮我预约挂号。" },
]
</script>

<template>
  <div class="mx-auto flex max-w-[640px] flex-col items-center px-4 py-10">
    <div class="flex h-14 w-14 animate-fade-up items-center justify-center rounded-2xl bg-brand-50" aria-hidden="true">
      <svg class="h-8 w-8 text-brand-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="1.8">
        <path stroke-linecap="round" stroke-linejoin="round" d="M8.625 12a3.375 3.375 0 106.75 0 3.375 3.375 0 00-6.75 0zM12 2.25c-2.429 0-4.563.938-6.234 2.484C4.094 6.28 3.25 8.414 3.25 10.843v.094c0 2.429.844 4.563 2.484 6.234 1.671 1.546 3.805 2.484 6.234 2.484s4.563-.938 6.234-2.484c1.64-1.671 2.484-3.805 2.484-6.234v-.094c0-2.429-.844-4.563-2.484-6.234C16.563 3.188 14.429 2.25 12 2.25z" />
      </svg>
    </div>

    <h1
      class="mt-5 animate-fade-up text-center text-heading-lg font-bold leading-tight tracking-normal text-gray-900"
      style="animation-delay: 0.1s"
    >
      <span class="block whitespace-nowrap">您好，请告诉我</span>
      <span class="mt-1 block whitespace-nowrap">哪里不舒服</span>
    </h1>

    <p class="mt-3 max-w-[460px] animate-fade-up text-center text-body leading-relaxed text-gray-600" style="animation-delay: 0.2s">
      您只管说症状，比如哪里疼、多久了、有没有发烧。<br />
      我会帮您分析该看什么科，还能帮您预约挂号。
    </p>

    <p class="mt-4 animate-fade-up rounded-lg border border-amber-200/60 bg-amber-50/60 px-4 py-2.5 text-center text-sm text-amber-800" style="animation-delay: 0.25s">
      本工具用于院内导诊参考，不能替代医生面诊。如有急症，请立即前往急诊或联系现场医护。
    </p>

    <div class="mt-7 w-full animate-fade-up" style="animation-delay: 0.3s">
      <p class="mb-3 text-center text-sm font-bold text-gray-600">也可以直接点下面的常见问题：</p>
      <div class="grid gap-2.5 sm:grid-cols-2">
        <button
          v-for="item in prompts"
          :key="item.label"
          type="button"
          class="tap-highlight flex items-center gap-2.5 rounded-xl border border-gray-100 bg-white px-3.5 py-3 text-left text-sm font-semibold text-gray-800 transition-all duration-200 hover:border-brand-300 hover:bg-brand-50/50 active:scale-[0.98]"
          @click="$emit('select', item.text)"
        >
          <span class="text-lg">{{ item.icon }}</span>
          <span>{{ item.label }}</span>
        </button>
      </div>
    </div>

    <p class="mt-6 text-center text-sm text-gray-400">
      不会打字？点下方麦克风图标并<span class="font-medium text-brand-500">开启语音输入</span>即可说话
    </p>
  </div>
</template>
