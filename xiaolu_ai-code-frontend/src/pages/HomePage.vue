<script setup lang="ts">
import { addApp, listGoodAppVoByPage, listMyAppVoByPage } from '@/api/appController'
import AppCard from '@/components/AppCard.vue'
import { getDeployUrl } from '@/config/env'
import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户提示词
const userPrompt = ref('')
const creating = ref(false)

// 我的应用数据
const myApps = ref<API.AppVO[]>([])
const myAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 精选应用数据
const featuredApps = ref<API.AppVO[]>([])
const featuredAppsPage = reactive({
  current: 1,
  pageSize: 6,
  total: 0,
})

// 设置提示词
const setPrompt = (prompt: string) => {
  userPrompt.value = prompt
}

// 优化提示词功能已移除

// 创建应用
const createApp = async () => {
  if (!userPrompt.value.trim()) {
    message.warning('请输入应用描述')
    return
  }

  if (!loginUserStore.loginUser.id) {
    message.warning('请先登录')
    await router.push('/user/login')
    return
  }

  creating.value = true
  try {
    const res = await addApp({
      initPrompt: userPrompt.value.trim(),
    })

    if (res.data.code === 0 && res.data.data) {
      message.success('应用创建成功')
      // 跳转到对话页面，确保ID是字符串类型
      const appId = String(res.data.data)
      await router.push(`/app/chat/${appId}`)
    } else {
      message.error('创建失败：' + res.data.message)
    }
  } catch (error) {
    console.error('创建应用失败：', error)
    message.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

// 加载我的应用
const loadMyApps = async () => {
  if (!loginUserStore.loginUser.id) {
    return
  }

  try {
    const res = await listMyAppVoByPage({
      pageNum: myAppsPage.current,
      pageSize: myAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      myApps.value = res.data.data.records || []
      myAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载我的应用失败：', error)
  }
}

// 加载精选应用
const loadFeaturedApps = async () => {
  try {
    const res = await listGoodAppVoByPage({
      pageNum: featuredAppsPage.current,
      pageSize: featuredAppsPage.pageSize,
      sortField: 'createTime',
      sortOrder: 'desc',
    })

    if (res.data.code === 0 && res.data.data) {
      featuredApps.value = res.data.data.records || []
      featuredAppsPage.total = res.data.data.totalRow || 0
    }
  } catch (error) {
    console.error('加载精选应用失败：', error)
  }
}

// 查看对话
const viewChat = (appId: string | number | undefined) => {
  if (appId) {
    router.push(`/app/chat/${appId}?view=1`)
  }
}

// 查看作品
const viewWork = (app: API.AppVO) => {
  if (app.deployKey) {
    const url = getDeployUrl(app.deployKey)
    window.open(url, '_blank')
  }
}

// 格式化时间函数已移除，不再需要显示创建时间

// 页面加载时获取数据
onMounted(() => {
  loadMyApps()
  loadFeaturedApps()
})
</script>

<template>
  <div id="homePage">
    <!-- Hero 区域：背景光晕 + 标题 + 输入框 -->
    <div class="hero-wrapper">
      <!-- 左侧渐变光晕 -->
      <div class="glow glow-left"></div>
      <!-- 右侧渐变光晕 -->
      <div class="glow glow-right"></div>

      <div class="hero-container">
        <!-- 网站标题和描述 -->
        <div class="hero-section">
          <h1 class="hero-title">
            AI 应用生成平台<br />
            轻松创建网站应用
          </h1>
          <p class="hero-description">
            使用简单的无代码 AI 应用构建器，<br />
            在几分钟内生成响应式、专业的网站设计，可用于企业、作品集、博客或商店等各种类型。
          </p>
        </div>

        <!-- 用户提示词输入框 -->
        <div class="input-section">
          <div class="input-card">
            <a-textarea
              v-model:value="userPrompt"
              placeholder="为你的服务创作网站"
              :rows="3"
              :maxlength="1000"
              class="prompt-input"
              @keydown.enter.ctrl="createApp"
            />
            <div class="input-actions">
              <button class="send-btn" @click="createApp" :disabled="creating">
                <span v-if="!creating" class="send-icon">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
                    <line x1="12" y1="19" x2="12" y2="5"></line>
                    <polyline points="5 12 12 5 19 12"></polyline>
                  </svg>
                </span>
                <span v-else class="loading-spinner"></span>
              </button>
            </div>
          </div>
        </div>

        <!-- 快捷按钮 -->
        <div class="quick-actions">
          <button
            class="quick-btn"
            @click="
              setPrompt(
                '创建一个现代化的个人博客网站，包含文章列表、详情页、分类标签、搜索功能、评论系统和个人简介页面。采用简洁的设计风格，支持响应式布局，文章支持Markdown格式，首页展示最新文章和热门推荐。',
              )
            "
          >
            个人博客网站
          </button>
          <button
            class="quick-btn"
            @click="
              setPrompt(
                '设计一个专业的企业官网，包含公司介绍、产品服务展示、新闻资讯、联系我们等页面。采用商务风格的设计，包含轮播图、产品展示卡片、团队介绍、客户案例展示，支持多语言切换和在线客服功能。',
              )
            "
          >
            企业官网
          </button>
          <button
            class="quick-btn"
            @click="
              setPrompt(
                '构建一个功能完整的在线商城，包含商品展示、购物车、用户注册登录、订单管理、支付结算等功能。设计现代化的商品卡片布局，支持商品搜索筛选、用户评价、优惠券系统和会员积分功能。',
              )
            "
          >
            在线商城
          </button>
          <button
            class="quick-btn"
            @click="
              setPrompt(
                '制作一个精美的作品展示网站，适合设计师、摄影师、艺术家等创作者。包含作品画廊、项目详情页、个人简历、联系方式等模块。采用瀑布流或网格布局展示作品，支持图片放大预览和作品分类筛选。',
              )
            "
          >
            作品展示网站
          </button>
        </div>
      </div>
    </div>

    <!-- 下方内容区域 -->
    <div class="content-wrapper">
      <div class="container">
        <!-- 我的作品 -->
        <div class="section">
          <h2 class="section-title">我的作品</h2>
          <div class="app-grid">
            <AppCard
              v-for="app in myApps"
              :key="app.id"
              :app="app"
              @view-chat="viewChat"
              @view-work="viewWork"
            />
          </div>
          <div class="pagination-wrapper">
            <a-pagination
              v-model:current="myAppsPage.current"
              v-model:page-size="myAppsPage.pageSize"
              :total="myAppsPage.total"
              :show-size-changer="false"
              :show-total="(total: number) => `共 ${total} 个应用`"
              @change="loadMyApps"
            />
          </div>
        </div>

        <!-- 精选案例 -->
        <div class="section">
          <h2 class="section-title">精选案例</h2>
          <div class="featured-grid">
            <AppCard
              v-for="app in featuredApps"
              :key="app.id"
              :app="app"
              :featured="true"
              @view-chat="viewChat"
              @view-work="viewWork"
            />
          </div>
          <div class="pagination-wrapper">
            <a-pagination
              v-model:current="featuredAppsPage.current"
              v-model:page-size="featuredAppsPage.pageSize"
              :total="featuredAppsPage.total"
              :show-size-changer="false"
              :show-total="(total: number) => `共 ${total} 个案例`"
              @change="loadFeaturedApps"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* ========== 全局页面 ========== */
#homePage {
  width: 100%;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  background: #ffffff;
  position: relative;
}

/* ========== Hero 区域 ========== */
.hero-wrapper {
  position: relative;
  width: 100%;
  min-height: 90vh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #fefefe;
}

/* 左侧橙色渐变光晕 */
.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  z-index: 0;
}

.glow-left {
  width: 700px;
  height: 700px;
  left: -200px;
  bottom: -100px;
  background: radial-gradient(
    circle,
    rgba(253, 186, 116, 0.45) 0%,
    rgba(251, 146, 60, 0.3) 30%,
    rgba(249, 115, 22, 0.15) 60%,
    transparent 80%
  );
  animation: glowPulseLeft 8s ease-in-out infinite alternate;
}

/* 右侧暖橙色渐变光晕 */
.glow-right {
  width: 600px;
  height: 600px;
  right: -150px;
  bottom: -50px;
  background: radial-gradient(
    circle,
    rgba(254, 215, 170, 0.45) 0%,
    rgba(253, 186, 116, 0.28) 30%,
    rgba(251, 146, 60, 0.14) 60%,
    transparent 80%
  );
  animation: glowPulseRight 10s ease-in-out infinite alternate;
}

@keyframes glowPulseLeft {
  0% {
    opacity: 0.7;
    transform: translate(0, 0) scale(1);
  }
  100% {
    opacity: 1;
    transform: translate(20px, -10px) scale(1.05);
  }
}

@keyframes glowPulseRight {
  0% {
    opacity: 0.6;
    transform: translate(0, 0) scale(1);
  }
  100% {
    opacity: 0.9;
    transform: translate(-15px, -15px) scale(1.08);
  }
}

.hero-container {
  position: relative;
  z-index: 2;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 0 24px;
}

/* ========== 标题区域 ========== */
.hero-section {
  text-align: center;
  padding: 0 0 48px;
}

.hero-title {
  font-size: 52px;
  font-weight: 800;
  margin: 0 0 28px;
  line-height: 1.25;
  color: #1c1917;
  letter-spacing: -1.5px;
}

.hero-description {
  font-size: 15px;
  margin: 0;
  line-height: 1.8;
  color: #6b7280;
  font-weight: 400;
}

/* ========== 输入框卡片 ========== */
.input-section {
  position: relative;
  margin: 0 auto 32px;
  max-width: 640px;
}

.input-card {
  position: relative;
  background: #ffffff;
  border-radius: 20px;
  box-shadow:
    0 4px 24px rgba(0, 0, 0, 0.06),
    0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  overflow: hidden;
  transition: box-shadow 0.3s ease, border-color 0.3s ease;
}

.input-card:focus-within {
  box-shadow:
    0 8px 40px rgba(249, 115, 22, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.06);
  border-color: rgba(249, 115, 22, 0.2);
}

.prompt-input {
  border: none !important;
  font-size: 15px;
  padding: 24px 24px 12px 24px !important;
  background: transparent !important;
  box-shadow: none !important;
  resize: none;
  color: #1f2937;
  line-height: 1.6;
}

.prompt-input::placeholder {
  color: #c4c4c4;
}

.prompt-input:focus {
  box-shadow: none !important;
  border: none !important;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  padding: 8px 16px 16px;
}

.send-btn {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  border: none;
  background: #ea580c;
  color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.25s ease;
}

.send-btn:hover {
  background: #c2410c;
  transform: scale(1.08);
  box-shadow: 0 4px 16px rgba(194, 65, 12, 0.3);
}

.send-btn:active {
  transform: scale(0.96);
}

.send-btn:disabled {
  background: #d1d5db;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
}

.send-icon {
  display: flex;
  align-items: center;
  justify-content: center;
}

.loading-spinner {
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255, 255, 255, 0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

/* ========== 快捷按钮 ========== */
.quick-actions {
  display: flex;
  gap: 10px;
  justify-content: center;
  flex-wrap: wrap;
  max-width: 640px;
  margin: 0 auto;
}

.quick-btn {
  padding: 8px 20px;
  border-radius: 24px;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #6b7280;
  font-size: 13.5px;
  cursor: pointer;
  transition: all 0.25s ease;
  white-space: nowrap;
}

.quick-btn:hover {
  border-color: #fb923c;
  color: #ea580c;
  background: #fff7ed;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(249, 115, 22, 0.1);
}

.quick-btn:active {
  transform: translateY(0);
}

/* ========== 下方内容区域 ========== */
.content-wrapper {
  position: relative;
  background: #ffffff;
  padding: 80px 0 60px;
  overflow: hidden;
}

/* 下方区域也加一个淡淡的橙色光晕，保持视觉一致性 */
.content-wrapper::before {
  content: '';
  position: absolute;
  width: 800px;
  height: 500px;
  left: -300px;
  top: 50%;
  transform: translateY(-50%);
  background: radial-gradient(
    ellipse,
    rgba(253, 186, 116, 0.12) 0%,
    rgba(249, 115, 22, 0.06) 40%,
    transparent 70%
  );
  filter: blur(80px);
  pointer-events: none;
}

.content-wrapper::after {
  content: '';
  position: absolute;
  width: 600px;
  height: 400px;
  right: -200px;
  top: 30%;
  background: radial-gradient(
    ellipse,
    rgba(254, 215, 170, 0.12) 0%,
    rgba(251, 146, 60, 0.06) 40%,
    transparent 70%
  );
  filter: blur(80px);
  pointer-events: none;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  width: 100%;
  box-sizing: border-box;
  position: relative;
  z-index: 1;
}

/* ========== 区块标题 ========== */
.section {
  margin-bottom: 64px;
}

.section-title {
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 32px;
  color: #1c1917;
  letter-spacing: -0.5px;
  position: relative;
  display: inline-block;
}

.section-title::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: -8px;
  width: 40px;
  height: 3px;
  border-radius: 2px;
  background: linear-gradient(90deg, #f97316, #fb923c);
}

/* ========== 应用卡片网格 ========== */
.app-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.featured-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

/* ========== 分页 ========== */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 32px;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .hero-wrapper {
    min-height: 80vh;
  }

  .hero-title {
    font-size: 32px;
    letter-spacing: -0.5px;
  }

  .hero-description {
    font-size: 14px;
  }

  .hero-description br {
    display: none;
  }

  .glow-left {
    width: 400px;
    height: 400px;
    left: -150px;
  }

  .glow-right {
    width: 350px;
    height: 350px;
    right: -100px;
  }

  .app-grid,
  .featured-grid {
    grid-template-columns: 1fr;
  }

  .quick-actions {
    gap: 8px;
  }

  .quick-btn {
    padding: 6px 16px;
    font-size: 13px;
  }
}

@media (max-width: 480px) {
  .hero-title {
    font-size: 26px;
  }

  .input-card {
    border-radius: 16px;
  }
}
</style>
