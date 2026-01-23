<script setup lang="ts">
import BasicLayout from '@/layouts/BasicLayout.vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'

import { healthCheck } from '@/api/healthController.ts'

healthCheck().then((res) => {
  console.log(res)
})
</script>

<template>
  <div>
    <a-config-provider :locale="zhCN">
      <router-view />
    </a-config-provider>
  </div>
  <BasicLayout />
</template>

<style></style>
