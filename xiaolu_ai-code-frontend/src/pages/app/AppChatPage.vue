<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="header-left">
        <h1 class="app-name">{{ appInfo?.appName || '网站生成器' }}</h1>
        <a-tag v-if="appInfo?.codeGenType" color="blue" class="code-gen-type-tag">
          {{ formatCodeGenType(appInfo.codeGenType) }}
        </a-tag>
        <a-tag v-if="appInfo" :color="getAppGenStatusMeta(appInfo?.genStatus).color" class="gen-status-tag">
          {{ getAppGenStatusMeta(appInfo?.genStatus).label }}
        </a-tag>
      </div>
      <div class="header-right">
        <a-button type="default" @click="showAppDetail">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button type="primary" ghost @click="downloadCode" :loading="downloading" :disabled="!isOwner">
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <a-button type="primary" @click="deployApp" :loading="deploying">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-section">
        <!-- 消息区域 -->
        <div class="messages-container" ref="messagesContainer">
          <!-- 加载更多按钮 -->
          <div v-if="hasMoreHistory" class="load-more-container">
            <a-button type="link" @click="loadMoreHistory" :loading="loadingHistory" size="small">
              加载更多历史消息
            </a-button>
          </div>
          <div v-for="(message, index) in messages" :key="index" class="message-item">
            <div v-if="message.type === 'user'" class="user-message">
              <div class="message-content">{{ message.content }}</div>
              <div class="message-avatar">
                <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              </div>
            </div>
            <div v-else class="ai-message">
              <div class="message-avatar">
                <a-avatar :src="aiAvatar" />
              </div>
              <div class="message-content">
                <!-- 按 segments 顺序渲染，工具调用和文字保持原始顺序 -->
                <template v-if="message.segments && message.segments.length">
                  <template v-for="(segment, sIdx) in message.segments" :key="sIdx">
                    <!-- 文本段 -->
                    <div v-if="segment.type === 'text'" class="segment-text">
                      <MarkdownRenderer :content="segment.content!" />
                    </div>

                    <!-- 选择工具指示器 -->
                    <div v-else-if="segment.type === 'tool-request'" class="tool-request-badge">
                      <span class="tool-request-dot"></span>
                      <span>正在调用 {{ segment.toolName }}</span>
                    </div>

                    <!-- 写入文件工具调用卡片 -->
                    <div v-else-if="segment.type === 'tool-write'" class="tool-call-card">
                      <div class="tool-call-header" @click="toggleSegmentCollapse(index, sIdx)">
                        <div class="tool-call-header-left">
                          <span class="tool-call-icon">📝</span>
                          <span class="tool-call-action">写入文件</span>
                          <code class="tool-call-filepath">{{ segment.filePath }}</code>
                        </div>
                        <span class="tool-call-toggle">
                          {{ isSegmentCollapsed(index, sIdx) ? '▶' : '▼' }}
                        </span>
                      </div>
                      <div v-show="!isSegmentCollapsed(index, sIdx)" class="tool-call-body">
                        <MarkdownRenderer :content="buildSegmentCodeBlock(segment)" />
                      </div>
                    </div>

                    <!-- 修改文件工具调用卡片 -->
                    <div v-else-if="segment.type === 'tool-modify'" class="tool-call-card">
                      <div class="tool-call-header" @click="toggleSegmentCollapse(index, sIdx)">
                        <div class="tool-call-header-left">
                          <span class="tool-call-icon">✏️</span>
                          <span class="tool-call-action">修改文件</span>
                          <code class="tool-call-filepath">{{ segment.filePath }}</code>
                        </div>
                        <span class="tool-call-toggle">
                          {{ isSegmentCollapsed(index, sIdx) ? '▶' : '▼' }}
                        </span>
                      </div>
                      <div v-show="!isSegmentCollapsed(index, sIdx)" class="tool-call-body">
                        <MarkdownRenderer :content="buildSegmentCodeBlock(segment)" />
                      </div>
                    </div>

                    <!-- 简单工具调用标记 (读取文件/删除文件/读取目录) -->
                    <div v-else-if="segment.type === 'tool-simple'" class="tool-simple-badge">
                      <span class="tool-simple-icon">⚒️</span>
                      <span class="tool-simple-action">{{ segment.toolName }}</span>
                      <code class="tool-simple-path">{{ segment.filePath }}</code>
                    </div>
                  </template>
                </template>

                <!-- 无 segments 时的 fallback -->
                <MarkdownRenderer v-else-if="message.content" :content="message.content" />

                <div v-if="message.loading" class="loading-indicator">
                  <a-spin size="small" />
                  <span>AI 正在思考...</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 选中元素信息展示 -->
        <a-alert v-if="selectedElementInfo" class="selected-element-alert" type="info" closable
          @close="clearSelectedElement">
          <template #message>
            <div class="selected-element-info">
              <div class="element-header">
                <span class="element-tag">
                  选中元素：{{ selectedElementInfo.tagName.toLowerCase() }}
                </span>
                <span v-if="selectedElementInfo.id" class="element-id">
                  #{{ selectedElementInfo.id }}
                </span>
                <span v-if="selectedElementInfo.className" class="element-class">
                  .{{ selectedElementInfo.className.split(' ').join('.') }}
                </span>
              </div>
              <div class="element-details">
                <div v-if="selectedElementInfo.textContent" class="element-item">
                  内容: {{ selectedElementInfo.textContent.substring(0, 50) }}
                  {{ selectedElementInfo.textContent.length > 50 ? '...' : '' }}
                </div>
                <div v-if="selectedElementInfo.pagePath" class="element-item">
                  页面路径: {{ selectedElementInfo.pagePath }}
                </div>
                <div class="element-item">
                  选择器:
                  <code class="element-selector-code">{{ selectedElementInfo.selector }}</code>
                </div>
              </div>
            </div>
          </template>
        </a-alert>

        <!-- 用户消息输入框 -->
        <div class="input-container">
          <div class="input-wrapper">
            <a-tooltip v-if="!isOwner" title="无法在别人的作品下对话哦~" placement="top">
              <a-textarea v-model:value="userInput" :placeholder="getInputPlaceholder()" :rows="4" :maxlength="1000"
                @keydown.enter.prevent="sendMessage" :disabled="isGenerating || !isOwner" />
            </a-tooltip>
            <a-textarea v-else v-model:value="userInput" :placeholder="getInputPlaceholder()" :rows="4"
              :maxlength="1000" @keydown.enter.prevent="sendMessage" :disabled="isGenerating" />
            <div class="input-actions">
              <a-button type="primary" @click="sendMessage" :loading="isGenerating" :disabled="!isOwner">
                <template #icon>
                  <SendOutlined />
                </template>
              </a-button>
            </div>
          </div>
        </div>
      </div>
      <!-- 右侧网页展示区域 -->
      <div class="preview-section">
        <div class="preview-header">
          <h3>生成后的网页展示</h3>
          <div class="preview-actions">
            <a-button v-if="isOwner && previewUrl" type="link" :danger="isEditMode" @click="toggleEditMode"
              :class="{ 'edit-mode-active': isEditMode }" style="padding: 0; height: auto; margin-right: 12px">
              <template #icon>
                <EditOutlined />
              </template>
              {{ isEditMode ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button v-if="previewUrl" type="link" @click="openInNewTab">
              <template #icon>
                <ExportOutlined />
              </template>
              新窗口打开
            </a-button>
          </div>
        </div>
        <div class="preview-content">
          <div v-if="isGenerating" class="preview-loading">
            <a-spin size="large" />
            <p>正在生成网站...</p>
          </div>
          <div v-else-if="isBuilding" class="preview-building">
            <a-spin size="large" />
            <p>正在构建项目...</p>
            <a-progress class="build-progress" :percent="buildProgressPercent" :status="buildProgressStatus" />
            <p class="build-message">{{ buildProgress?.message || '构建中...' }}</p>
          </div>
          <div v-else-if="!previewUrl" class="preview-placeholder">
            <div class="placeholder-icon">🌐</div>
            <p>网站文件生成完成后将在这里展示</p>
          </div>
          <iframe v-else :src="previewUrl" class="preview-iframe" frameborder="0" @load="onIframeLoad"></iframe>
        </div>
      </div>
    </div>

    <!-- 应用详情弹窗 -->
    <AppDetailModal v-model:open="appDetailVisible" :app="appInfo" :show-actions="isOwner || isAdmin" @edit="editApp"
      @delete="deleteApp" />

    <!-- 部署成功弹窗 -->
    <DeploySuccessModal v-model:open="deployModalVisible" :deploy-url="deployUrl" @open-site="openDeployedSite" />
  </div>
</template>

<script setup lang="ts">
import {
  deleteApp as deleteAppApi,
  deployApp as deployAppApi,
  getAppVoById,
} from '@/api/appController'
import { listAppChatHistory } from '@/api/chatHistoryController'
import request from '@/request'
import { useLoginUserStore } from '@/stores/loginUser'
import { CodeGenTypeEnum, formatCodeGenType } from '@/utils/codeGenTypes'
import { message } from 'ant-design-vue'
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import aiAvatar from '@/assets/aiAvatar.png'
import AppDetailModal from '@/components/AppDetailModal.vue'
import DeploySuccessModal from '@/components/DeploySuccessModal.vue'
import MarkdownRenderer from '@/components/MarkdownRenderer.vue'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { VisualEditor, type ElementInfo } from '@/utils/visualEditor'
import { AppGenStatusEnum, getAppGenStatusMeta } from '@/utils/appGenStatus'

import {
  CloudUploadOutlined,
  DownloadOutlined,
  EditOutlined,
  ExportOutlined,
  InfoCircleOutlined,
  SendOutlined,
} from '@ant-design/icons-vue'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用信息
const appInfo = ref<API.AppVO>()
const appId = ref<any>()

// 对话相关
interface ContentSegment {
  type: 'text' | 'tool-write' | 'tool-modify' | 'tool-simple' | 'tool-request'
  content?: string
  filePath?: string
  language?: string
  code?: string
  oldCode?: string
  newCode?: string
  toolName?: string
}

interface Message {
  type: 'user' | 'ai'
  content: string
  loading?: boolean
  createTime?: string
  segments?: ContentSegment[]
}

interface BuildProgressEvent {
  status: 'waiting' | 'running' | 'success' | 'failed'
  step?: string
  percent?: number
  message?: string
}

const messages = ref<Message[]>([])
const userInput = ref('')
const isGenerating = ref(false)
const messagesContainer = ref<HTMLElement>()

// 对话历史相关
const loadingHistory = ref(false)
const hasMoreHistory = ref(false)
const lastCreateTime = ref<string>()
const historyLoaded = ref(false)

// 预览相关
const previewUrl = ref('')
const previewReady = ref(false)
const isBuilding = ref(false)
const buildProgress = ref<BuildProgressEvent | null>(null)
let buildEventSource: EventSource | null = null

// 部署相关
const deploying = ref(false)
const deployModalVisible = ref(false)
const deployUrl = ref('')

// 下载相关
const downloading = ref(false)

// 可视化编辑相关
const isEditMode = ref(false)
const selectedElementInfo = ref<ElementInfo | null>(null)
const visualEditor = new VisualEditor({
  onElementSelected: (elementInfo: ElementInfo) => {
    selectedElementInfo.value = elementInfo
  },
})

// 权限相关
const isOwner = computed(() => {
  return appInfo.value?.userId === loginUserStore.loginUser.id
})

const isAdmin = computed(() => {
  return loginUserStore.loginUser.userRole === 'admin'
})

const buildProgressPercent = computed(() => buildProgress.value?.percent ?? 0)
const buildProgressStatus = computed(() => {
  const status = buildProgress.value?.status
  if (status === 'failed') {
    return 'exception'
  }
  if (status === 'success') {
    return 'success'
  }
  return 'active'
})

// 应用详情相关
const appDetailVisible = ref(false)

// 显示应用详情
const showAppDetail = () => {
  appDetailVisible.value = true
}

const getLanguageByPath = (filePath: string) => {
  const match = filePath.match(/\.([a-zA-Z0-9]+)$/)
  return match ? match[1] : ''
}

// 折叠状态管理
const collapsedSegments = ref(new Set<string>())

const toggleSegmentCollapse = (messageIdx: number, segmentIdx: number) => {
  const key = `${messageIdx}-${segmentIdx}`
  const newSet = new Set(collapsedSegments.value)
  if (newSet.has(key)) {
    newSet.delete(key)
  } else {
    newSet.add(key)
  }
  collapsedSegments.value = newSet
}

const isSegmentCollapsed = (messageIdx: number, segmentIdx: number) => {
  return collapsedSegments.value.has(`${messageIdx}-${segmentIdx}`)
}

// 将工具调用段构建为 Markdown 代码块用于渲染
const buildSegmentCodeBlock = (segment: ContentSegment): string => {
  if (segment.type === 'tool-write') {
    const language = segment.language || ''
    const content = segment.code ?? ''
    return `\`\`\`${language}\n${content}\n\`\`\``
  }
  if (segment.type === 'tool-modify') {
    const oldCode = segment.oldCode ?? ''
    const newCode = segment.newCode ?? ''
    return `**替换前：**\n\`\`\`\n${oldCode}\n\`\`\`\n\n**替换后：**\n\`\`\`\n${newCode}\n\`\`\``
  }
  return ''
}

/**
 * 将原始 AI 输出内容解析为有序的内容段数组，
 * 保持文本和工具调用的原始出现顺序。
 */
const parseContentIntoSegments = (rawContent: string): ContentSegment[] => {
  const segments: ContentSegment[] = []

  interface ToolMatch {
    startIndex: number
    endIndex: number
    segment: ContentSegment
  }

  const toolMatches: ToolMatch[] = []

  let match: RegExpExecArray | null

  // 1. 匹配写入文件工具调用: [⚒️工具调用] 写入文件 path\n```lang\ncontent\n```
  const writeFileRegex = /\[⚒️工具调用\]\s*写入文件\s+([^\n]+)\n\s*```([^\n]*)\n([\s\S]*?)\n\s*```/g
  while ((match = writeFileRegex.exec(rawContent)) !== null) {
    toolMatches.push({
      startIndex: match.index,
      endIndex: match.index + match[0].length,
      segment: {
        type: 'tool-write',
        toolName: '写入文件',
        filePath: match[1].trim(),
        language: (match[2] || '').trim() || getLanguageByPath(match[1].trim()),
        code: match[3] ?? '',
      },
    })
  }

  // 2. 匹配修改文件工具调用
  const modifyFileRegex =
    /\[⚒️工具调用\]\s*修改文件\s+([^\n]+)\s*\n\s*\n?\s*替换前[：:]\s*\n\s*```[^\n]*\n([\s\S]*?)\n\s*```\s*\n\s*\n?\s*替换后[：:]\s*\n\s*```[^\n]*\n([\s\S]*?)\n\s*```/g
  while ((match = modifyFileRegex.exec(rawContent)) !== null) {
    toolMatches.push({
      startIndex: match.index,
      endIndex: match.index + match[0].length,
      segment: {
        type: 'tool-modify',
        toolName: '修改文件',
        filePath: match[1].trim(),
        oldCode: match[2] ?? '',
        newCode: match[3] ?? '',
      },
    })
  }

  // 3. 匹配简单工具调用 (读取文件、删除文件、读取目录)
  const simpleToolRegex = /\[⚒️工具调用\]\s*(读取文件|删除文件|读取目录)\s+([^\n]*)/g
  while ((match = simpleToolRegex.exec(rawContent)) !== null) {
    const overlaps = toolMatches.some(
      (tm) => match!.index >= tm.startIndex && match!.index < tm.endIndex,
    )
    if (!overlaps) {
      toolMatches.push({
        startIndex: match.index,
        endIndex: match.index + match[0].length,
        segment: {
          type: 'tool-simple',
          toolName: match[1].trim(),
          filePath: match[2].trim(),
        },
      })
    }
  }

  // 4. 匹配选择工具标记: [选择工具] toolName
  const toolRequestRegex = /\[选择工具\]\s*([^\n]+)/g
  while ((match = toolRequestRegex.exec(rawContent)) !== null) {
    const overlaps = toolMatches.some(
      (tm) => match!.index >= tm.startIndex && match!.index < tm.endIndex,
    )
    if (!overlaps) {
      toolMatches.push({
        startIndex: match.index,
        endIndex: match.index + match[0].length,
        segment: {
          type: 'tool-request',
          toolName: match[1].trim(),
        },
      })
    }
  }

  // 按位置排序
  toolMatches.sort((a, b) => a.startIndex - b.startIndex)

  // 按原始顺序构建 segments
  let currentIndex = 0
  for (const tm of toolMatches) {
    if (tm.startIndex > currentIndex) {
      const textContent = rawContent.slice(currentIndex, tm.startIndex).trim()
      if (textContent) {
        segments.push({ type: 'text', content: textContent })
      }
    }
    segments.push(tm.segment)
    currentIndex = tm.endIndex
  }

  // 处理末尾剩余内容
  if (currentIndex < rawContent.length) {
    const remainingText = rawContent.slice(currentIndex).trim()
    // 检查是否有未完成的工具调用（流式传输中可能出现）
    const pendingWriteIdx = remainingText.lastIndexOf('[⚒️工具调用] 写入文件')
    const pendingModifyIdx = remainingText.lastIndexOf('[⚒️工具调用] 修改文件')
    const pendingIdx = Math.max(pendingWriteIdx, pendingModifyIdx)

    if (pendingIdx !== -1) {
      const textBefore = remainingText.slice(0, pendingIdx).trim()
      if (textBefore) {
        segments.push({ type: 'text', content: textBefore })
      }
      // 未完成的工具调用不添加到 segments，等下一次解析
    } else if (remainingText) {
      segments.push({ type: 'text', content: remainingText })
    }
  }

  return segments
}



// 加载对话历史
const loadChatHistory = async (isLoadMore = false) => {
  if (!appId.value || loadingHistory.value) return
  loadingHistory.value = true
  try {
    const params: API.listAppChatHistoryParams = {
      appId: appId.value,
      pageSize: 10,
    }
    // 如果是加载更多，传递最后一条消息的创建时间作为游标
    if (isLoadMore && lastCreateTime.value) {
      params.lastCreateTime = lastCreateTime.value
    }
    const res = await listAppChatHistory(params)
    if (res.data.code === 0 && res.data.data) {
      const chatHistories = res.data.data.records || []
      if (chatHistories.length > 0) {
        // 将对话历史转换为消息格式，并按时间正序排列（老消息在前）
        const historyMessages: Message[] = chatHistories
          .map((chat) => {
            const type = (chat.messageType === 'user' ? 'user' : 'ai') as 'user' | 'ai'
            const content = chat.message || ''
            let segments: ContentSegment[] | undefined = undefined
            if (type === 'ai') {
              segments = parseContentIntoSegments(content)
            }
            return {
              type,
              content,
              createTime: chat.createTime,
              segments,
            }
          })
          .reverse() // 反转数组，让老消息在前
        if (isLoadMore) {
          // 加载更多时，将历史消息添加到开头
          messages.value.unshift(...historyMessages)
        } else {
          // 初始加载，直接设置消息列表
          messages.value = historyMessages
        }
        // 更新游标
        lastCreateTime.value = chatHistories[chatHistories.length - 1]?.createTime
        // 检查是否还有更多历史
        hasMoreHistory.value = chatHistories.length === 10
      } else {
        hasMoreHistory.value = false
      }
      historyLoaded.value = true
    }
  } catch (error) {
    console.error('加载对话历史失败：', error)
    message.error('加载对话历史失败')
  } finally {
    loadingHistory.value = false
  }
}

// 加载更多历史消息
const loadMoreHistory = async () => {
  await loadChatHistory(true)
}

// 获取应用信息
const fetchAppInfo = async () => {
  const id = route.params.id as string
  if (!id) {
    message.error('应用ID不存在')
    router.push('/')
    return
  }

  appId.value = id

  try {
    const res = await getAppVoById({ id: id as unknown as number })
    if (res.data.code === 0 && res.data.data) {
      appInfo.value = res.data.data

      // 先加载对话历史
      await loadChatHistory()
      // 如果有至少2条对话记录，展示对应的网站
      if (messages.value.length >= 2) {
        updatePreview()
      }
      // 检查是否需要自动发送初始提示词
      // 只有在是自己的应用且没有对话历史时才自动发送
      if (
        appInfo.value.initPrompt &&
        isOwner.value &&
        messages.value.length === 0 &&
        historyLoaded.value
      ) {
        await sendInitialMessage(appInfo.value.initPrompt)
      }
    } else {
      message.error('获取应用信息失败')
      router.push('/')
    }
  } catch (error) {
    console.error('获取应用信息失败：', error)
    message.error('获取应用信息失败')
    router.push('/')
  }
}

// 发送初始消息
const sendInitialMessage = async (prompt: string) => {
  // 添加用户消息
  messages.value.push({
    type: 'user',
    content: prompt,
  })

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    segments: [],
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(prompt, aiMessageIndex)
}

// 发送消息
const sendMessage = async () => {
  if (!userInput.value.trim() || isGenerating.value) {
    return
  }

  let message = userInput.value.trim()
  // 如果有选中的元素，将元素信息添加到提示词中
  if (selectedElementInfo.value) {
    let elementContext = `\n\n选中元素信息：`
    if (selectedElementInfo.value.pagePath) {
      elementContext += `\n- 页面路径: ${selectedElementInfo.value.pagePath}`
    }
    elementContext += `\n- 标签: ${selectedElementInfo.value.tagName.toLowerCase()}\n- 选择器: ${selectedElementInfo.value.selector}`
    if (selectedElementInfo.value.textContent) {
      elementContext += `\n- 当前内容: ${selectedElementInfo.value.textContent.substring(0, 100)}`
    }
    message += elementContext
  }
  userInput.value = ''
  // 添加用户消息（包含元素信息）
  messages.value.push({
    type: 'user',
    content: message,
  })

  // 发送消息后，清除选中元素并退出编辑模式
  if (selectedElementInfo.value) {
    clearSelectedElement()
    if (isEditMode.value) {
      toggleEditMode()
    }
  }

  // 添加AI消息占位符
  const aiMessageIndex = messages.value.length
  messages.value.push({
    type: 'ai',
    content: '',
    loading: true,
    segments: [],
  })

  await nextTick()
  scrollToBottom()

  // 开始生成
  isGenerating.value = true
  await generateCode(message, aiMessageIndex)
}

const closeBuildProgressStream = () => {
  if (buildEventSource) {
    buildEventSource.close()
    buildEventSource = null
  }
}

const startBuildProgressStream = () => {
  if (!appId.value) return
  closeBuildProgressStream()
  isBuilding.value = true
  buildProgress.value = {
    status: 'waiting',
    percent: 0,
    message: '等待构建开始...',
  }
  previewUrl.value = ''
  previewReady.value = false
  isEditMode.value = false

  let buildStreamCompleted = false
  try {
    const baseURL = request.defaults.baseURL || API_BASE_URL
    const params = new URLSearchParams({
      appId: appId.value || '',
    })
    const url = `${baseURL}/app/build/progress?${params}`
    buildEventSource = new EventSource(url, {
      withCredentials: true,
    })

    buildEventSource.addEventListener('progress', function (event) {
      if (buildStreamCompleted) return
      try {
        const data = JSON.parse((event as MessageEvent).data) as BuildProgressEvent
        buildProgress.value = data
      } catch (error) {
        console.error('解析构建进度失败:', error)
      }
    })

    buildEventSource.addEventListener('done', async function (event) {
      if (buildStreamCompleted) return
      buildStreamCompleted = true
      if ((event as MessageEvent).data) {
        try {
          const data = JSON.parse((event as MessageEvent).data) as BuildProgressEvent
          buildProgress.value = data
        } catch (error) {
          console.error('解析构建完成事件失败:', error)
        }
      }
      const finalStatus = buildProgress.value?.status
      isBuilding.value = false
      closeBuildProgressStream()
      if (finalStatus === 'failed') {
        message.error(buildProgress.value?.message || '构建失败，请重试')
        return
      }
      await fetchAppInfo()
      updatePreview()
    })

    buildEventSource.addEventListener('error', function (event) {
      if (buildStreamCompleted) return
      buildStreamCompleted = true
      try {
        const data = JSON.parse((event as MessageEvent).data)
        message.error(data?.message || '构建失败，请重试')
      } catch (error) {
        message.error('构建失败，请重试')
      }
      isBuilding.value = false
      closeBuildProgressStream()
    })

    buildEventSource.onerror = function () {
      if (buildStreamCompleted) return
      if (buildEventSource?.readyState === EventSource.CONNECTING) {
        buildStreamCompleted = true
        isBuilding.value = false
        closeBuildProgressStream()
      } else {
        message.error('构建进度连接错误')
        isBuilding.value = false
        closeBuildProgressStream()
      }
    }
  } catch (error) {
    console.error('创建构建进度 EventSource 失败：', error)
    message.error('构建进度连接失败')
    isBuilding.value = false
    closeBuildProgressStream()
  }
}

// 生成代码 - 使用 EventSource 处理流式响应
const generateCode = async (userMessage: string, aiMessageIndex: number) => {
  let eventSource: EventSource | null = null
  let streamCompleted = false
  let rawContent = ''

  try {
    if (appInfo.value) {
      appInfo.value.genStatus = AppGenStatusEnum.GENERATING
    }
    // 获取 axios 配置的 baseURL
    const baseURL = request.defaults.baseURL || API_BASE_URL

    // 构建URL参数
    const params = new URLSearchParams({
      appId: appId.value || '',
      message: userMessage,
    })

    const url = `${baseURL}/app/chat/gen/code?${params}`

    // 创建 EventSource 连接
    eventSource = new EventSource(url, {
      withCredentials: true,
    })

    // 处理接收到的消息
    eventSource.onmessage = function (event) {
      if (streamCompleted) return

      try {
        // 解析JSON包装的数据
        const parsed = JSON.parse(event.data)
        const content = parsed.d

        // 拼接内容并按段解析
        if (content !== undefined && content !== null) {
          rawContent += content
          const segments = parseContentIntoSegments(rawContent)
          messages.value[aiMessageIndex].content = rawContent
          messages.value[aiMessageIndex].segments = segments
          messages.value[aiMessageIndex].loading = false
          scrollToBottom()
        }
      } catch (error) {
        console.error('解析消息失败:', error)
        handleError(error, aiMessageIndex)
      }
    }

    // 处理business-error事件（后端限流等错误）
    eventSource.addEventListener('business-error', function (event: MessageEvent) {
      if (streamCompleted) return

      try {
        const errorData = JSON.parse(event.data)
        console.error('SSE业务错误事件:', errorData)

        // 显示具体的错误信息
        const errorMessage = errorData.message || '生成过程中出现错误'
        messages.value[aiMessageIndex].content = `❌ ${errorMessage}`
        messages.value[aiMessageIndex].loading = false
        message.error(errorMessage)

        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()
      } catch (parseError) {
        console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
        handleError(new Error('服务器返回错误'), aiMessageIndex)
      }
    })

    // 处理done事件
    eventSource.addEventListener('done', function () {
      if (streamCompleted) return

      streamCompleted = true
      isGenerating.value = false
      eventSource?.close()

      // 最终解析：去除流式中的 [选择工具] 标记（历史记录不包含这些标记）
      const finalContent = rawContent.replace(/\[选择工具\]\s*[^\n]+\n*/g, '')
      const segments = parseContentIntoSegments(finalContent)
      if (messages.value[aiMessageIndex]) {
        messages.value[aiMessageIndex].content = finalContent
        messages.value[aiMessageIndex].segments = segments
        messages.value[aiMessageIndex].loading = false
      }

      const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
      if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
        startBuildProgressStream()
      } else {
        // 延迟更新预览，确保后端已完成处理
        setTimeout(async () => {
          await fetchAppInfo()
          updatePreview()
        }, 1000)
      }
    })

    // 处理错误
    eventSource.onerror = function () {
      if (streamCompleted || !isGenerating.value) return
      // 检查是否是正常的连接关闭
      if (eventSource?.readyState === EventSource.CONNECTING) {
        streamCompleted = true
        isGenerating.value = false
        eventSource?.close()

        const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
        if (codeGenType === CodeGenTypeEnum.VUE_PROJECT) {
          startBuildProgressStream()
        } else {
          setTimeout(async () => {
            await fetchAppInfo()
            updatePreview()
          }, 1000)
        }
      } else {
        handleError(new Error('SSE连接错误'), aiMessageIndex)
      }
    }
  } catch (error) {
    console.error('创建 EventSource 失败：', error)
    handleError(error, aiMessageIndex)
  }
}

// 错误处理函数
const handleError = (error: unknown, aiMessageIndex: number) => {
  console.error('生成代码失败：', error)
  messages.value[aiMessageIndex].content = '抱歉，生成过程中出现了错误，请重试。'
  messages.value[aiMessageIndex].loading = false
  message.error('生成失败，请重试')
  isGenerating.value = false
  if (appInfo.value) {
    appInfo.value.genStatus = AppGenStatusEnum.FAILED
  }
}

// 更新预览
const updatePreview = () => {
  if (appId.value) {
    const codeGenType = appInfo.value?.codeGenType || CodeGenTypeEnum.HTML
    const newPreviewUrl = getStaticPreviewUrl(codeGenType, appId.value)
    previewUrl.value = newPreviewUrl
    previewReady.value = true
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

// 下载代码
const downloadCode = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }
  downloading.value = true
  try {
    const API_BASE_URL = request.defaults.baseURL || ''
    const url = `${API_BASE_URL}/app/download/${appId.value}`
    const response = await fetch(url, {
      method: 'GET',
      credentials: 'include',
    })
    if (!response.ok) {
      throw new Error(`下载失败: ${response.status}`)
    }
    // 获取文件名
    const contentDisposition = response.headers.get('Content-Disposition')
    const fileName = contentDisposition?.match(/filename="(.+)"/)?.[1] || `app-${appId.value}.zip`
    // 下载文件
    const blob = await response.blob()
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    // 清理
    URL.revokeObjectURL(downloadUrl)
    message.success('代码下载成功')
  } catch (error) {
    console.error('下载失败：', error)
    message.error('下载失败，请重试')
  } finally {
    downloading.value = false
  }
}

// 部署应用
const deployApp = async () => {
  if (!appId.value) {
    message.error('应用ID不存在')
    return
  }

  deploying.value = true
  try {
    const res = await deployAppApi({
      appId: appId.value as unknown as number,
    })

    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalVisible.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败：' + res.data.message)
    }
  } catch (error) {
    console.error('部署失败：', error)
    message.error('部署失败，请重试')
  } finally {
    deploying.value = false
  }
}

// 在新窗口打开预览
const openInNewTab = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 打开部署的网站
const openDeployedSite = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// iframe加载完成
const onIframeLoad = () => {
  previewReady.value = true
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (iframe) {
    visualEditor.init(iframe)
    visualEditor.onIframeLoad()
  }
}

// 编辑应用
const editApp = () => {
  if (appInfo.value?.id) {
    router.push(`/app/edit/${appInfo.value.id}`)
  }
}

// 删除应用
const deleteApp = async () => {
  if (!appInfo.value?.id) return

  try {
    const res = await deleteAppApi({ id: appInfo.value.id })
    if (res.data.code === 0) {
      message.success('删除成功')
      appDetailVisible.value = false
      router.push('/')
    } else {
      message.error('删除失败：' + res.data.message)
    }
  } catch (error) {
    console.error('删除失败：', error)
    message.error('删除失败')
  }
}

// 可视化编辑相关函数
const toggleEditMode = () => {
  // 检查 iframe 是否已经加载
  const iframe = document.querySelector('.preview-iframe') as HTMLIFrameElement
  if (!iframe) {
    message.warning('请等待页面加载完成')
    return
  }
  // 确保 visualEditor 已初始化
  if (!previewReady.value) {
    message.warning('请等待页面加载完成')
    return
  }
  const newEditMode = visualEditor.toggleEditMode()
  isEditMode.value = newEditMode
}

const clearSelectedElement = () => {
  selectedElementInfo.value = null
  visualEditor.clearSelection()
}

const getInputPlaceholder = () => {
  if (selectedElementInfo.value) {
    return `正在编辑 ${selectedElementInfo.value.tagName.toLowerCase()} 元素，描述您想要的修改...`
  }
  return '请描述你想生成的网站，越详细效果越好哦'
}

// 页面加载时获取应用信息
onMounted(() => {
  fetchAppInfo()

  // 监听 iframe 消息
  window.addEventListener('message', (event) => {
    visualEditor.handleIframeMessage(event)
  })
})

// 清理资源
onUnmounted(() => {
  closeBuildProgressStream()
})
</script>

<style scoped>
#appChatPage {
  height: 100vh;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #fdfdfd;
}

/* 顶部栏 */
.header-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.code-gen-type-tag {
  font-size: 12px;
}

.gen-status-tag {
  font-size: 12px;
}

.app-name {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
}

.header-right {
  display: flex;
  gap: 12px;
}

/* 主要内容区域 */
.main-content {
  flex: 1;
  display: flex;
  gap: 16px;
  padding: 8px;
  overflow: hidden;
}

/* 左侧对话区域 */
.chat-section {
  flex: 2;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.messages-container {
  flex: 0.9;
  padding: 16px;
  overflow-y: auto;
  scroll-behavior: smooth;
}

.message-item {
  margin-bottom: 12px;
}

.user-message {
  display: flex;
  justify-content: flex-end;
  align-items: flex-start;
  gap: 8px;
}

.ai-message {
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  gap: 8px;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 12px;
  line-height: 1.5;
  word-wrap: break-word;
}

.user-message .message-content {
  background: #1890ff;
  color: white;
}

.ai-message .message-content {
  background: #f5f5f5;
  color: #1a1a1a;
  padding: 8px 12px;
}

.message-avatar {
  flex-shrink: 0;
}

/* 文本段 */
.segment-text {
  margin: 0;
}

/* 选择工具指示器 */
.tool-request-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 14px;
  margin: 8px 0;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 16px;
  font-size: 12px;
  color: #3b82f6;
}

.tool-request-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #3b82f6;
  animation: toolDotPulse 1.5s infinite;
}

@keyframes toolDotPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* 工具调用卡片 */
.tool-call-card {
  margin: 10px 0;
  border-radius: 8px;
  border: 1px solid #e5e7eb;
  overflow: hidden;
  background: #ffffff;
}

.tool-call-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: linear-gradient(to right, #f8f9fa, #f0f2f5);
  cursor: pointer;
  user-select: none;
  transition: background 0.15s;
}

.tool-call-header:hover {
  background: linear-gradient(to right, #eef0f3, #e6e9ed);
}

.tool-call-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  overflow: hidden;
}

.tool-call-icon {
  flex-shrink: 0;
  font-size: 14px;
}

.tool-call-action {
  font-size: 13px;
  font-weight: 600;
  color: #374151;
  white-space: nowrap;
}

.tool-call-filepath {
  font-size: 12px;
  color: #6b7280;
  font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 8px;
  border-radius: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.tool-call-toggle {
  flex-shrink: 0;
  font-size: 10px;
  color: #9ca3af;
  margin-left: 8px;
}

.tool-call-body {
  border-top: 1px solid #e5e7eb;
  max-height: 400px;
  overflow-y: auto;
}

.tool-call-body :deep(.markdown-content) {
  margin: 0;
}

.tool-call-body :deep(pre) {
  margin: 0;
  border: none;
  border-radius: 0;
}

.tool-call-body :deep(pre.hljs) {
  border-radius: 0;
}

/* 简单工具调用标记 */
.tool-simple-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  margin: 8px 0;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  font-size: 12px;
  color: #6b7280;
}

.tool-simple-icon {
  font-size: 12px;
}

.tool-simple-action {
  font-weight: 500;
  color: #374151;
}

.tool-simple-path {
  font-family: 'Monaco', 'Menlo', monospace;
  font-size: 11px;
  color: #6b7280;
  background: rgba(0, 0, 0, 0.03);
  padding: 1px 6px;
  border-radius: 3px;
}

.loading-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #666;
}

/* 加载更多按钮 */
.load-more-container {
  text-align: center;
  padding: 8px 0;
  margin-bottom: 16px;
}

/* 输入区域 */
.input-container {
  padding: 16px;
  background: white;
}

.input-wrapper {
  position: relative;
}

.input-wrapper .ant-input {
  padding-right: 50px;
}

.input-actions {
  position: absolute;
  bottom: 8px;
  right: 8px;
}

/* 右侧预览区域 */
.preview-section {
  flex: 3;
  display: flex;
  flex-direction: column;
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid #e8e8e8;
}

.preview-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.preview-actions {
  display: flex;
  gap: 8px;
}

.preview-content {
  flex: 1;
  position: relative;
  overflow: hidden;
}

.preview-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.preview-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.preview-building {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  color: #666;
}

.preview-loading p {
  margin-top: 16px;
}

.build-progress {
  width: 70%;
  margin-top: 12px;
}

.build-message {
  margin-top: 8px;
  color: #888;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.selected-element-alert {
  margin: 0 16px;
}

/* 响应式设计 */
@media (max-width: 1024px) {
  .main-content {
    flex-direction: column;
  }

  .chat-section,
  .preview-section {
    flex: none;
    height: 50vh;
  }
}

@media (max-width: 768px) {
  .header-bar {
    padding: 12px 16px;
  }

  .app-name {
    font-size: 16px;
  }

  .main-content {
    padding: 8px;
    gap: 8px;
  }

  .message-content {
    max-width: 85%;
  }

  /* 选中元素信息样式 */
  .selected-element-alert {
    margin: 0 16px;
  }

  .selected-element-info {
    line-height: 1.4;
  }

  .element-header {
    margin-bottom: 8px;
  }

  .element-details {
    margin-top: 8px;
  }

  .element-item {
    margin-bottom: 4px;
    font-size: 13px;
  }

  .element-item:last-child {
    margin-bottom: 0;
  }

  .element-tag {
    font-family: 'Monaco', 'Menlo', monospace;
    font-size: 14px;
    font-weight: 600;
    color: #007bff;
  }

  .element-id {
    color: #28a745;
    margin-left: 4px;
  }

  .element-class {
    color: #ffc107;
    margin-left: 4px;
  }

  .element-selector-code {
    font-family: 'Monaco', 'Menlo', monospace;
    background: #f6f8fa;
    padding: 2px 4px;
    border-radius: 3px;
    font-size: 12px;
    color: #d73a49;
    border: 1px solid #e1e4e8;
  }

  /* 编辑模式按钮样式 */
  .edit-mode-active {
    background-color: #52c41a !important;
    border-color: #52c41a !important;
    color: white !important;
  }

  .edit-mode-active:hover {
    background-color: #73d13d !important;
    border-color: #73d13d !important;
  }
}
</style>
