import { useLoginUserStore } from '@/stores/loginUser'
import { message } from 'ant-design-vue'
import router from '@/router'

// 是否为首次获取登录用户
let firstFetchLoginUser = true

/**
 * 全局权限校验
 */
router.beforeEach(async (to, from, next) => {
  const loginUserStore = useLoginUserStore()
  let loginUser = loginUserStore.loginUser
  // 确保页面刷新，首次加载时，能够等后端返回用户信息后再校验权限
  if (firstFetchLoginUser) {
    await loginUserStore.fetchLoginUser()
    loginUser = loginUserStore.loginUser
    firstFetchLoginUser = false
  }
  const toUrl = to.fullPath
  
  // 管理员页面权限控制
  if (toUrl.startsWith('/admin')) {
    // 未登录，跳转到登录页
    if (!loginUser || !loginUser.id) {
      message.error('请先登录')
      next(`/user/login?redirect=${to.fullPath}`)
      return
    }
    // 已登录但不是管理员，跳转到主页
    if (loginUser.userRole !== 'admin') {
      message.error('无权限访问，仅管理员可访问')
      next('/')
      return
    }
  }
  
  next()
})
