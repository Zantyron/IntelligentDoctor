<script setup lang="ts">
import type { RegistrationOrder } from "@/types"
defineProps<{ order: RegistrationOrder }>()
</script>

<template>
  <div class="animate-bounce-in overflow-hidden rounded-xl bg-brand-50 py-6 text-center">
    <div class="mx-auto flex h-14 w-14 items-center justify-center rounded-full bg-green-500 shadow-lg">
      <svg class="h-7 w-7 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="3">
        <path stroke-linecap="round" stroke-linejoin="round" d="m4.5 12.75 6 6 9-13.5" />
      </svg>
    </div>
    <h2 class="mt-4 text-heading-lg font-bold text-gray-900">挂号成功！</h2>
    <div class="mx-auto mt-4 max-w-xs space-y-2.5 rounded-xl bg-white p-4">
      <div class="flex justify-between text-sm"><span class="text-gray-500">订单号</span><span class="font-bold text-gray-900">{{ order.orderNo }}</span></div>
      <div class="flex justify-between text-sm"><span class="text-gray-500">姓名</span><span class="font-bold text-gray-900">{{ order.patientName }}</span></div>
      <div class="flex justify-between text-sm"><span class="text-gray-500">就诊日期</span><span class="font-bold text-brand-600">{{ order.visitDate || "—" }}</span></div>
      <div class="flex justify-between text-sm"><span class="text-gray-500">时段</span><span class="font-bold text-brand-600">{{ order.visitPeriod || "—" }}</span></div>
    </div>
    <div class="mt-4 space-y-1.5">
      <p class="text-base font-bold text-gray-800">请携带<strong class="text-brand-600">身份证</strong>按时到医院就诊</p>
      <p class="text-sm text-gray-500">如有疑问请联系医院服务台</p>
    </div>
  </div>
</template>
