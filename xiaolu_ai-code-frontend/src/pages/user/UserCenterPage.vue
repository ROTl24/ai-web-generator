<template>
  <div id="userCenterPage">
    <div class="page-container">
      <!-- 页面标题 -->
      <div class="page-header">
        <h2 class="page-title">个人中心</h2>
        <a-tag :color="loginUser.userRole === 'admin' ? 'green' : 'blue'" class="role-tag">
          {{ loginUser.userRole === 'admin' ? '管理员' : '普通用户' }}
        </a-tag>
      </div>

      <!-- 用户基础信息 -->
      <div class="info-section">
        <h3 class="section-title">基础信息</h3>
        <div class="info-content">
          <div class="info-item">
            <span class="info-label">账号：</span>
            <span class="info-value">{{ loginUser.userAccount }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">用户ID：</span>
            <span class="info-value">{{ loginUser.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">注册时间：</span>
            <span class="info-value">{{ dayjs(loginUser.createTime).format('YYYY-MM-DD HH:mm:ss') }}</span>
          </div>
        </div>
      </div>

      <!-- 编辑表单 -->
      <div class="form-section">
        <h3 class="section-title">编辑资料</h3>
        <a-form
          :model="formState"
          :label-col="{ span: 3 }"
          :wrapper-col="{ span: 18 }"
          @finish="handleSubmit"
          class="user-form"
        >
          <a-form-item
            label="用户名"
            name="userName"
            :rules="[{ required: true, message: '请输入用户名' }]"
          >
            <a-input
              v-model:value="formState.userName"
              placeholder="请输入用户名"
              :maxlength="20"
              show-count
            />
          </a-form-item>

          <a-form-item label="头像" name="userAvatar">
            <div class="avatar-upload-section">
              <a-upload
                name="avatar"
                list-type="picture-card"
                class="avatar-uploader"
                :show-upload-list="false"
                :before-upload="beforeUpload"
                :custom-request="handleCustomUpload"
                :disabled="avatarUploading"
                @change="handleUploadChange"
              >
                <div class="avatar-wrapper">
                  <img
                    v-if="formState.userAvatar"
                    :src="formState.userAvatar"
                    class="user-avatar-img"
                    alt="用户头像"
                  />
                  <div v-else class="user-avatar-default">
                    {{ formState.userName?.substring(0, 1) ?? '用' }}
                  </div>
                  <div class="avatar-overlay">
                    <div class="upload-icon">
                      <camera-outlined />
                      <div class="upload-text">上传头像</div>
                    </div>
                  </div>
                </div>
              </a-upload>
              <div class="avatar-tips">
                <div class="tip-main">点击头像可上传本地图片</div>
                <div class="tip-detail">支持 JPG、PNG 格式，超过 2MB 会自动压缩</div>
              </div>
            </div>
          </a-form-item>

          <a-form-item label="个人简介" name="userProfile">
            <a-textarea
              v-model:value="formState.userProfile"
              placeholder="请输入个人简介"
              :rows="4"
              :maxlength="200"
              show-count
            />
          </a-form-item>

          <a-form-item
  :wrapper-col="{ offset: 3, span: 18 }"
  class="form-actions"
>
  <a-space size="middle">
    <a-button type="primary" html-type="submit" :loading="loading" size="large">
      保存修改
    </a-button>
    <a-button @click="resetForm" size="large">重置</a-button>
  </a-space>
</a-form-item>
        </a-form>
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
import { updateMyUser, uploadUserAvatar } from '@/api/userController'
import { useLoginUserStore } from '@/stores/loginUser'
import { CameraOutlined } from '@ant-design/icons-vue'
import type { UploadChangeParam } from 'ant-design-vue'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { onMounted, reactive, ref } from 'vue'

const loginUserStore = useLoginUserStore()
const loginUser = loginUserStore.loginUser

const loading = ref(false)
const avatarUploading = ref(false)

const MAX_AVATAR_SIZE = 2 * 1024 * 1024
const MAX_AVATAR_SIDE = 1024
const MIN_QUALITY = 0.6
const QUALITY_STEP = 0.1
const SCALE_STEP = 0.85
const MAX_SCALE_TRIES = 3

type UploadRequestOption = {
  file: File
  onSuccess?: (response: unknown, file: File) => void
  onError?: (error: Error) => void
  onProgress?: (event: { percent: number }) => void
}

const formState = reactive({
  userName: '',
  userAvatar: '',
  userProfile: '',
})

// 初始化表单数据
const initForm = () => {
  formState.userName = loginUser.userName ?? ''
  formState.userAvatar = loginUser.userAvatar ?? ''
  formState.userProfile = loginUser.userProfile ?? ''
}

// 重置表单
const resetForm = () => {
  initForm()
  message.info('已重置为当前信息')
}

const loadImage = (file: File) =>
  new Promise<HTMLImageElement>((resolve, reject) => {
    const img = new Image()
    const objectUrl = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(objectUrl)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(objectUrl)
      reject(new Error('图片加载失败'))
    }
    img.src = objectUrl
  })

const canvasToBlob = (canvas: HTMLCanvasElement, quality: number) =>
  new Promise<Blob>((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('图片压缩失败'))
        }
      },
      'image/jpeg',
      quality,
    )
  })

const drawImageToCanvas = (
  image: HTMLImageElement,
  width: number,
  height: number,
): HTMLCanvasElement => {
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const context = canvas.getContext('2d')
  if (!context) {
    throw new Error('图片压缩失败')
  }
  // 填充白底，避免 PNG 透明区域变黑
  context.fillStyle = '#ffffff'
  context.fillRect(0, 0, width, height)
  context.drawImage(image, 0, 0, width, height)
  return canvas
}

const compressImage = async (file: File) => {
  const image = await loadImage(file)
  const maxSide = Math.max(image.width, image.height)
  const scale =
    maxSide > MAX_AVATAR_SIDE ? MAX_AVATAR_SIDE / maxSide : 1
  let targetWidth = Math.max(1, Math.round(image.width * scale))
  let targetHeight = Math.max(1, Math.round(image.height * scale))
  let canvas = drawImageToCanvas(image, targetWidth, targetHeight)
  let quality = 0.92
  let blob = await canvasToBlob(canvas, quality)

  while (blob.size > MAX_AVATAR_SIZE && quality > MIN_QUALITY) {
    quality = Math.max(MIN_QUALITY, quality - QUALITY_STEP)
    blob = await canvasToBlob(canvas, quality)
  }

  let scaleTries = 0
  while (blob.size > MAX_AVATAR_SIZE && scaleTries < MAX_SCALE_TRIES) {
    targetWidth = Math.max(1, Math.round(targetWidth * SCALE_STEP))
    targetHeight = Math.max(1, Math.round(targetHeight * SCALE_STEP))
    canvas = drawImageToCanvas(image, targetWidth, targetHeight)
    quality = 0.92
    blob = await canvasToBlob(canvas, quality)
    while (blob.size > MAX_AVATAR_SIZE && quality > MIN_QUALITY) {
      quality = Math.max(MIN_QUALITY, quality - QUALITY_STEP)
      blob = await canvasToBlob(canvas, quality)
    }
    scaleTries += 1
  }

  if (blob.size > MAX_AVATAR_SIZE) {
    throw new Error('图片压缩后仍超过 2MB，请选择更小的图片')
  }

  const fileName = file.name.replace(/\.\w+$/, '') || 'avatar'
  return new File([blob], `${fileName}.jpg`, { type: 'image/jpeg' })
}

// 上传前校验
const beforeUpload = (file: File) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('只支持上传 JPG/PNG 格式的图片！')
    return false
  }

  return true
}

// 自定义上传
const handleCustomUpload = async (options: UploadRequestOption) => {
  if (avatarUploading.value) {
    return
  }
  const file = options.file as File
  avatarUploading.value = true
  try {
    let uploadFile = file
    if (file.size > MAX_AVATAR_SIZE) {
      message.info('头像超过 2MB，正在压缩...')
      uploadFile = await compressImage(file)
    }
    const res = await uploadUserAvatar(uploadFile)
    if (res.data.code === 0 && res.data.data) {
      formState.userAvatar = res.data.data
      message.success('头像上传成功，请点击保存修改')
      options.onSuccess?.(res.data, uploadFile)
    } else {
      message.error('头像上传失败：' + (res.data.message ?? '请稍后重试'))
      options.onError?.(new Error(res.data.message ?? '上传失败'))
    }
  } catch (error) {
    const errorMessage = error instanceof Error ? error.message : '头像上传失败，请稍后重试'
    message.error(errorMessage)
    options.onError?.(error as Error)
  } finally {
    avatarUploading.value = false
  }
}

// 处理图片上传变化
const handleUploadChange = (info: UploadChangeParam) => {
  // 这个函数主要用于监听上传状态，实际处理在自定义上传中
  console.log('Upload change:', info.file.status)
}

// 提交表单
const handleSubmit = async () => {
  loading.value = true
  try {
    const res = await updateMyUser({
      userName: formState.userName,
      userAvatar: formState.userAvatar,
      userProfile: formState.userProfile,
    })

    if (res.data.code === 0) {
      message.success('更新成功')
      // 更新本地存储的用户信息
      await loginUserStore.fetchLoginUser()
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch {
    message.error('更新失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  initForm()
})
</script>

<style scoped>
#userCenterPage {
  width: 800px;
  min-height: 100vh;
  background-color: #fff;
  padding: 40px 80px;
}

.page-container {
  max-width: 1000px;
  margin: 0 auto;
}

/* 页面标题 */
.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 32px;
  padding-bottom: 16px;
  border-bottom: 2px solid #f0f0f0;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.role-tag {
  font-size: 13px;
  padding: 2px 12px;
}

/* 信息区域 */
.info-section {
  margin-bottom: 40px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
}

.info-content {
  background: #fafafa;
  padding: 20px;
  border-radius: 4px;
}

.info-item {
  display: flex;
  padding: 8px 0;
  font-size: 14px;
}

.info-label {
  color: #666;
  min-width: 100px;
  font-weight: 500;
}

.info-value {
  color: #1a1a1a;
}

/* 表单区域 */
.form-section {
  margin-bottom: 40px;
}

.user-form {
  margin-top: 24px;
}

.user-form :deep(.ant-form-item) {
  margin-bottom: 40px;
}

.user-form :deep(.ant-form-item-label) {
  padding-right: 32px;
}

.user-form :deep(.ant-form-item-label > label) {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  height: 40px;
  display: flex;
  align-items: center;
}

.user-form :deep(.ant-input),
.user-form :deep(.ant-input-textarea) {
  margin-top: 4px;
}

.avatar-upload-section {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 16px;
}

.avatar-uploader {
  display: inline-block;
}


.avatar-uploader :deep(.ant-upload) {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  border: none;
  background: transparent;
  padding: 0;
}

.avatar-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  border: 3px solid #e8e8e8;
  transition: all 0.3s ease;
  background: #fff;
}

.avatar-wrapper:hover {
  border-color: #1890ff;
}

/* 头像图片 */
.user-avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
  transition: all 0.3s ease;
}

/* 默认头像（无图片时） */
.user-avatar-default {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 42px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  transition: all 0.3s ease;
}

avatar-uploader {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: all 0.3s ease;
  backdrop-filter: blur(3px);
}

.avatar-wrapper:hover .avatar-overlay {
  opacity: 1;
}

.avatar-wrapper:hover .user-avatar-img,
.avatar-wrapper:hover .user-avatar-default {
  filter: blur(4px);
  transform: scale(1.05);
}

.upload-icon {
  color: white;
  text-align: center;
  font-size: 16px;
}

.upload-icon .anticon {
  font-size: 32px;
  margin-bottom: 8px;
  display: block;
}

.upload-text {
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.avatar-tips {
  padding-left: 4px;
}

.tip-main {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin-bottom: 6px;
}

.tip-detail {
  font-size: 12px;
  color: #999;
  line-height: 1.5;

}

.form-actions {
  display: flex;
  justify-content: center;
}

/* 响应式设计 */
@media (max-width: 1200px) {
  #userCenterPage {
    padding: 32px 40px;
  }
}

@media (max-width: 768px) {
  #userCenterPage {
    padding: 20px;
  }

  .page-container {
    max-width: 100%;
  }

  .user-form :deep(.ant-form-item-label) {
    text-align: left;
  }

  .user-form :deep(.ant-form) .ant-form-item {
    flex-direction: column;
  }

  .user-form :deep(.ant-form-item-label) {
    padding: 0 0 8px;
  }
}
</style>
