<script setup lang="ts">
import { reactive, watch } from "vue"
import type { RegistrationDraft } from "@/types"

const props = defineProps<{
  draft: RegistrationDraft
  confirming: boolean
}>()

const emit = defineEmits<{
  confirm: [form: { patientName: string; patientPhone: string; idCard: string; gender: string; age: number }]
}>()

const form = reactive({
  patientName: "", patientPhone: "", idCard: "", gender: "", age: "" as string | number,
})

watch(() => props.draft, (draft) => {
  if (draft.patientName) form.patientName = draft.patientName
  if (draft.patientPhone) form.patientPhone = draft.patientPhone
  if (draft.idCard) form.idCard = draft.idCard
  if (draft.gender) form.gender = draft.gender
  if (draft.age) form.age = draft.age
}, { immediate: true })

function submit() {
  emit("confirm", {
    patientName: form.patientName.trim(), patientPhone: form.patientPhone.trim(),
    idCard: form.idCard.trim(), gender: form.gender, age: Number(form.age),
  })
}
</script>

<template>
  <div class="animate-fade-up overflow-hidden rounded-xl bg-white mt-3">
    <div class="bg-brand-50 px-5 py-3.5">
      <h3 class="text-heading font-bold text-gray-900">确认挂号信息</h3>
      <p class="mt-1 text-sm text-gray-600">请填写您的个人信息，确认后即可完成挂号</p>
    </div>

    <div class="grid gap-2 border-b border-gray-100 px-5 py-3 sm:grid-cols-2">
      <div><span class="text-xs text-gray-500">科室</span><p class="text-base font-bold text-gray-900">{{ draft.departmentId || "待确认" }}</p></div>
      <div><span class="text-xs text-gray-500">医生</span><p class="text-base font-bold text-gray-900">{{ draft.doctorId || "待确认" }}</p></div>
      <div class="sm:col-span-2"><span class="text-xs text-gray-500">就诊时间</span><p class="text-base font-bold text-brand-600">{{ draft.visitDate || "—" }} {{ draft.visitPeriod || "" }}</p></div>
    </div>

    <form class="space-y-4 px-5 py-4" @submit.prevent="submit">
      <label class="block"><span class="mb-1 block text-sm font-bold text-gray-700">您的姓名</span><input v-model="form.patientName" class="input-field" autocomplete="name" placeholder="请输入您的姓名" required /></label>
      <label class="block"><span class="mb-1 block text-sm font-bold text-gray-700">手机号码</span><input v-model="form.patientPhone" class="input-field" type="tel" autocomplete="tel" inputmode="tel" placeholder="请输入手机号码" required /></label>
      <label class="block"><span class="mb-1 block text-sm font-bold text-gray-700">身份证号</span><input v-model="form.idCard" class="input-field" autocomplete="off" placeholder="请输入身份证号码" required /></label>
      <div class="grid gap-4 sm:grid-cols-2">
        <label class="block"><span class="mb-1 block text-sm font-bold text-gray-700">性别</span><select v-model="form.gender" class="select-field" required><option value="">请选择</option><option value="男">男</option><option value="女">女</option></select></label>
        <label class="block"><span class="mb-1 block text-sm font-bold text-gray-700">年龄</span><input v-model="form.age" type="number" min="1" max="130" class="input-field" inputmode="numeric" placeholder="请输入年龄" required /></label>
      </div>
      <button type="submit" class="btn-primary w-full" :disabled="confirming">{{ confirming ? "正在挂号，请稍等..." : "确认挂号" }}</button>
      <p class="text-center text-xs text-gray-400">确认挂号即代表您同意将以上信息用于本次挂号服务</p>
    </form>
  </div>
</template>
