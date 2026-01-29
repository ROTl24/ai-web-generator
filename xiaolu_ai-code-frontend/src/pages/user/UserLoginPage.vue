<template>
  <div id="userLoginPage">
    <h2 class="title">AI 应用生成 - 用户登录</h2>
    <div class="desc">解放双手，生成完整应用</div>
    <a-form
    :model="formState"
    name="basic"
    autocomplete="off"
    @finish="handleSubmit">
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
      <div class="tips">
        没有账号？
        <RouterLink to="/user/register">去注册</RouterLink>
      </div>
      <a-form-item>
        <a-button type="primary" html-type="submit" style="width: 100%">登录</a-button>
      </a-form-item>
    </a-form>
  </div>
</template>
<script lang="ts" setup>
import { userLogin } from '@/api/userController';
import { useLoginUserStore } from '@/stores/loginUser';
import { message } from 'ant-design-vue';
import { reactive } from 'vue'
import { useRouter } from 'vue-router';
const formState = reactive<API.UserLoginRequest>({
  userAccount: '',
  userPassword: '',
})

const router = useRouter();
const loginUserStore = useLoginUserStore();

/**
 * 提交表单
 * @param values 表单值
 */
const handleSubmit = async (values: API.UserLoginRequest) => {
  const res = await userLogin(values);
  //登陆成功
  if (res.data.code === 0 && res.data.data){
    await loginUserStore.fetchLoginUser();
    message.success('登录成功');
    router.push({
      path: '/',
      replace: true,// 登录成功后不允许返回登录页
    });
  }else{
    message.error('登陆失败' + res.data.message);
  }
}
</script>

<style scoped>
#userLoginPage {
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
  #userLoginPage {
    max-width: 90%;
    padding: 32px 24px;
  }
}

@media (max-width: 480px) {
  #userLoginPage {
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
