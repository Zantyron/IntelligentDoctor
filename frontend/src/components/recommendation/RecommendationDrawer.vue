<script setup lang="ts">
import { computed } from "vue"
import type { AppointmentOption, ChatResultMetadata, RegistrationDraft, RegistrationOrder } from "@/types"
import RegistrationForm from "@/components/registration/RegistrationForm.vue"
import RegistrationSuccess from "@/components/registration/RegistrationSuccess.vue"

const props = defineProps<{
  show: boolean
  metadata: ChatResultMetadata | null
  draft: RegistrationDraft | null
  order: RegistrationOrder | null
  confirming: boolean
}>()

const emit = defineEmits<{
  "update:show": [value: boolean]
  confirm: [form: { patientName: string; patientPhone: string; idCard: string; gender: string; age: number }]
}>()

const recommendations = computed(() => props.metadata?.recommendations ?? [])
const appointmentOptions = computed<AppointmentOption[]>(() => props.metadata?.metadata?.appointmentOptions ?? [])
const evidence = computed(() => props.metadata?.evidence ?? [])

const hasData = computed(() =>
  recommendations.value.length > 0 || appointmentOptions.value.length > 0 || evidence.value.length > 0,
)
</script>

<template>
  <div class="space-y-4">
    <div v-if="hasData" class="rounded-xl border border-gray-100 bg-white p-4">
      <div class="mb-3 flex items-center justify-between">
        <h3 class="text-base font-bold text-gray-900">智能推荐</h3>
        <button
          v-if="show"
          type="button"
          class="btn-ghost px-2 py-1 text-xs"
          aria-label="隐藏智能推荐"
          @click="$emit('update:show', false)"
        >
          <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
            <path stroke-linecap="round" stroke-linejoin="round" d="M6 18L18 6M6 6l12 12" />
          </svg>
        </button>
      </div>

      <button
        v-if="!show"
        type="button"
        class="btn-outline w-full text-sm"
        @click="$emit('update:show', true)"
      >
        <svg class="h-3.5 w-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="2.5">
          <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 15 7.5-7.5 7.5 7.5" />
        </svg>
        查看智能推荐
      </button>

      <div v-else class="space-y-4">
        <section v-if="recommendations.length">
          <p class="mb-2 text-sm font-bold text-gray-600">建议科室</p>
          <div class="space-y-2">
            <article v-for="(item, i) in recommendations" :key="i" class="rounded-lg bg-brand-50/60 p-3">
              <p class="text-sm font-bold text-brand-700">{{ item.title }}</p>
              <p v-if="item.description" class="mt-1 text-sm text-gray-700">{{ item.description }}</p>
              <p v-if="item.reason" class="mt-1 text-xs text-gray-500">{{ item.reason }}</p>
            </article>
          </div>
        </section>

        <section v-if="appointmentOptions.length">
          <p class="mb-2 text-sm font-bold text-gray-600">可预约号源</p>
          <div class="space-y-2">
            <article v-for="(item, i) in appointmentOptions" :key="i" class="rounded-lg bg-gray-50 p-3">
              <p class="text-sm font-bold text-gray-900">{{ item.doctorName || "医生待定" }}</p>
              <p class="mt-0.5 text-xs text-gray-600">
                {{ item.clinicName }}<span v-if="item.doctorTitle"> · {{ item.doctorTitle }}</span>
              </p>
              <p class="mt-1.5 text-sm font-bold text-brand-600">
                {{ item.slotDate }} {{ item.period }}
                <span class="text-xs font-normal text-gray-500"> · 剩余 {{ item.stockAvailable ?? "-" }} 个</span>
              </p>
            </article>
          </div>
        </section>

        <section v-if="evidence.length">
          <p class="mb-2 text-sm font-bold text-gray-600">推荐依据</p>
          <blockquote
            v-for="(item, i) in evidence"
            :key="i"
            class="mb-1.5 rounded-r border-l-2 border-amber-400 bg-amber-50/60 px-3 py-2 text-xs leading-relaxed text-gray-700"
          >
            {{ item }}
          </blockquote>
        </section>
      </div>
    </div>

    <RegistrationForm
      v-if="draft && !order"
      :draft="draft"
      :confirming="confirming"
      @confirm="emit('confirm', $event)"
    />
    <RegistrationSuccess v-if="order" :order="order" />
  </div>
</template>
