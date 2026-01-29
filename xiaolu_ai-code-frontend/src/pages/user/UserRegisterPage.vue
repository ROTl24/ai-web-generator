<template>
  <div id="userRegisterPage">
    <h2 class="title">AI 应用生成 - 用户注册</h2>
    <div class="desc">解放双手，生成完整应用</div>
    <a-form
      :model="formState"
      name="basic"
      autocomplete="off"
      @finish="handleSubmit"
    >
      <a-form-item name="userAccount" :rules="[{ required: true, message: '请输入账号' }]">
        <a-input v-model:value="formState.userAccount" placeholder="请输入账号" />
      </a-form-item>
      <a-form-item
        name="userPassword"
        :rules="[
          { required: true, message: '请输入密码' },
          { min: 8, message: '密码不能小于 8 位' },
        ]"
      >
        <a-input-password v-model:value="formState.userPassword" placeholder="请输入密码" />
      </a-form-item>
      <a-form-item
        name="checkPassword"
        :rules="[
          { required: true, message: '请确认密码' },
          { min: 8, message: '密码不能小于 8 位' },
          { validator: validateCheckPassword },
        ]"
      >
        <a-input-password v-model:value="formState.checkPassword" placeholder="请确认密码" />
      </a-form-item>
      <div class="tips">
        已有账号？
        <RouterLink to="/user/login">去登录</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">注册</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>

<script lang="ts" setup>
import { userRegister } from '@/api/userController'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { reactive } from 'vue'
import { useRouter } from 'vue-router'

const formState = reactive<API.UserRegisterRequest>({
  userAccount: '',
  userPassword: '',
  checkPassword: '',
})

const router = useRouter()

/**
 * 校验确认密码
 */
const validateCheckPassword = async (_rule: Rule, value: string) => {
  if (value !== formState.userPassword) {
    return Promise.reject('两次输入的密码不一致')
  }
  return Promise.resolve()
}

/**
 * 提交表单
 * @param values 表单值
 */
const handleSubmit = async (values: API.UserRegisterRequest) => {
  const res = await userRegister(values)
  // 注册成功
  if (res.data.code === 0 && res.data.data) {
    message.success('注册成功，即将跳转到登录页')
    // 延迟 1.5 秒后跳转到登录页
    setTimeout(() => {
      router.push({
        path: '/user/login',
        replace: true, // 注册成功后不允许返回注册页
      })
    }, 1500)
  } else {
    message.error('注册失败：' + res.data.message)
  }
}
</script>

<style scoped>
#userRegisterPage {
  max-width: 400px;
  margin: 0 auto;
  padding: 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.06);
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
}

.title {
  text-align: center;
  margin-bottom: 16px;
  font-size: 24px;
  font-weight: 600;
  color: #1a1a1a;
}

.desc {
  text-align: center;
  color: #999;
  margin-bottom: 32px;
  font-size: 14px;
}

.tips {
  margin-bottom: 16px;
  color: #999;
  font-size: 13px;
  text-align: right;
}

.tips a {
  color: #1890ff;
  text-decoration: none;
}

.tips a:hover {
  color: #40a9ff;
  text-decoration: underline;
}

/* 响应式设计 */
@media (max-width: 768px) {
  #userRegisterPage {
    max-width: 90%;
    padding: 32px 24px;
  }
}

@media (max-width: 480px) {
  #userRegisterPage {
    max-width: 95%;
    padding: 24px 20px;
    position: static;
    transform: none;
    margin-top: 20px;
  }

  .title {
    font-size: 20px;
  }
}
</style>
