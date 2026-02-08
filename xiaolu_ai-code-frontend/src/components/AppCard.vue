<template>
  <div class="app-card" :class="{ 'app-card--featured': featured }">
    <div class="app-preview">
      <img v-if="app.cover" :src="normalizeAssetUrl(app.cover)" :alt="app.appName" />
      <div v-else class="app-placeholder">
        <span>🤖</span>
      </div>
      <div class="app-overlay">
        <a-space>
          <a-button type="primary" @click="handleViewChat">查看对话</a-button>
          <a-button v-if="app.deployKey" type="default" @click="handleViewWork">查看作品</a-button>
        </a-space>
      </div>
    </div>
    <div class="app-info">
      <div class="app-info-left">
        <a-avatar :src="app.user?.userAvatar" :size="40">
          {{ app.user?.userName?.charAt(0) || 'U' }}
        </a-avatar>
      </div>
      <div class="app-info-right">
        <div class="app-title-row">
          <h3 class="app-title">{{ app.appName || '未命名应用' }}</h3>
          <a-tag :color="getAppGenStatusMeta(app.genStatus).color" class="status-tag">
            {{ getAppGenStatusMeta(app.genStatus).label }}
          </a-tag>
        </div>
        <p class="app-author">
          {{ app.user?.userName || (featured ? '官方' : '未知用户') }}
        </p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { normalizeAssetUrl } from '@/utils/url'
import { getAppGenStatusMeta } from '@/utils/appGenStatus'

interface Props {
  app: API.AppVO
  featured?: boolean
}

interface Emits {
  (e: 'view-chat', appId: string | number | undefined): void
  (e: 'view-work', app: API.AppVO): void
}

const props = withDefaults(defineProps<Props>(), {
  featured: false,
})

const emit = defineEmits<Emits>()

const handleViewChat = () => {
  emit('view-chat', props.app.id)
}

const handleViewWork = () => {
  emit('view-work', props.app)
}
</script>

<style scoped>
.app-card {
  background: #ffffff;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border: 1px solid #f0f0f0;
  transition: all 0.3s ease;
  cursor: pointer;
}

.app-card:hover {
  transform: translateY(-4px);
  box-shadow:
    0 12px 32px rgba(249, 115, 22, 0.1),
    0 4px 12px rgba(0, 0, 0, 0.06);
  border-color: rgba(249, 115, 22, 0.15);
}

/* 精选卡片微调 */
.app-card--featured {
  border-color: rgba(249, 115, 22, 0.1);
}

.app-preview {
  height: 180px;
  background: #fafafa;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  position: relative;
}

.app-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.app-card:hover .app-preview img {
  transform: scale(1.03);
}

.app-placeholder {
  font-size: 48px;
  color: #e5e7eb;
}

.app-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(124, 45, 18, 0.6);
  backdrop-filter: blur(2px);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.app-card:hover .app-overlay {
  opacity: 1;
}

.app-overlay :deep(.ant-btn-primary) {
  background: #ea580c;
  border-color: #ea580c;
  border-radius: 8px;
}

.app-overlay :deep(.ant-btn-primary:hover) {
  background: #c2410c;
  border-color: #c2410c;
}

.app-overlay :deep(.ant-btn-default) {
  background: rgba(255, 255, 255, 0.95);
  border-color: transparent;
  color: #374151;
  border-radius: 8px;
}

.app-overlay :deep(.ant-btn-default:hover) {
  color: #ea580c;
  border-color: #ea580c;
}

.app-info {
  padding: 14px 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  border-top: 1px solid #f5f5f5;
}

.app-info-left {
  flex-shrink: 0;
}

.app-info-left :deep(.ant-avatar) {
  border: 2px solid #ffedd5;
}

.app-info-right {
  flex: 1;
  min-width: 0;
}

.app-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.app-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 3px;
  color: #1c1917;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.status-tag {
  margin: 0;
  flex-shrink: 0;
  border-radius: 6px;
  font-size: 12px;
}

.app-author {
  font-size: 13px;
  color: #9ca3af;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
