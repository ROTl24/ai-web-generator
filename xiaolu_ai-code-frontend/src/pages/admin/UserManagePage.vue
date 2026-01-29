<template>
  <div id="userManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="账号">
        <a-input v-model:value="searchParams.userAccount" placeholder="输入账号" />
      </a-form-item>
      <a-form-item label="用户名">
        <a-input v-model:value="searchParams.userName" placeholder="输入用户名" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <!-- 用户名 - 可编辑 -->
        <template v-if="column.dataIndex === 'userName'">
          <div v-if="editingKey === record.id">
            <a-input v-model:value="editingRecord.userName" placeholder="请输入用户名" />
          </div>
          <div v-else>{{ record.userName || '未命名' }}</div>
        </template>
        
        <!-- 头像 - 可编辑 -->
        <template v-else-if="column.dataIndex === 'userAvatar'">
          <div v-if="editingKey === record.id">
            <a-input v-model:value="editingRecord.userAvatar" placeholder="请输入头像URL" />
          </div>
          <div v-else>
            <a-image :src="record.userAvatar" :width="80" />
          </div>
        </template>
        
        <!-- 简介 - 可编辑 -->
        <template v-else-if="column.dataIndex === 'userProfile'">
          <div v-if="editingKey === record.id">
            <a-textarea
              v-model:value="editingRecord.userProfile"
              placeholder="请输入简介"
              :rows="2"
            />
          </div>
          <div v-else>{{ record.userProfile || '-' }}</div>
        </template>
        
        <!-- 用户角色 - 可编辑 -->
        <template v-else-if="column.dataIndex === 'userRole'">
          <div v-if="editingKey === record.id">
            <a-select v-model:value="editingRecord.userRole" style="width: 120px">
              <a-select-option value="user">普通用户</a-select-option>
              <a-select-option value="admin">管理员</a-select-option>
            </a-select>
          </div>
          <div v-else>
            <a-tag v-if="record.userRole === 'admin'" color="green">管理员</a-tag>
            <a-tag v-else color="blue">普通用户</a-tag>
          </div>
        </template>
        
        <!-- 创建时间 -->
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ dayjs(record.createTime).format('YYYY-MM-DD HH:mm:ss') }}
        </template>
        
        <!-- 操作列 -->
        <template v-else-if="column.key === 'action'">
          <div v-if="editingKey === record.id" class="action-buttons editing">
            <a-tooltip title="保存">
              <a-button
                type="primary"
                shape="circle"
                size="small"
                class="save-btn"
                @click="doSave"
              >
                <template #icon>
                  <CheckOutlined />
                </template>
              </a-button>
            </a-tooltip>
            <a-tooltip title="取消">
              <a-button shape="circle" size="small" class="cancel-btn" @click="cancelEdit">
                <template #icon>
                  <CloseOutlined />
                </template>
              </a-button>
            </a-tooltip>
          </div>
          <div v-else class="action-buttons">
            <a-tooltip title="编辑">
              <a-button type="link" size="small" class="edit-link" @click="doEdit(record)">
                <EditOutlined />
                编辑
              </a-button>
            </a-tooltip>
            <a-tooltip title="删除">
              <a-button type="link" danger size="small" class="delete-link" @click="doDelete(record.id)">
                <DeleteOutlined />
                删除
              </a-button>
            </a-tooltip>
          </div>
        </template>
      </template>
    </a-table>
  </div>
</template>
<script lang="ts" setup>
import { ref, computed, onMounted, h, reactive } from 'vue'
import { listUserVoByPage, deleteUser, updateUser } from '@/api/userController'
import { message, Modal } from 'ant-design-vue'
import {
  ExclamationCircleOutlined,
  CheckOutlined,
  CloseOutlined,
  EditOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser'
import { useRouter } from 'vue-router'
import dayjs from 'dayjs'
import type { TableProps } from 'ant-design-vue'

const loginUserStore = useLoginUserStore()
const router = useRouter()

// 编辑状态
const editingKey = ref<number | null>(null)
const editingRecord = reactive<API.UserUpdateRequest>({
  id: undefined,
  userName: '',
  userAvatar: '',
  userProfile: '',
  userRole: '',
})

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
    align: 'center' as const,
  },
  {
    title: '账号',
    dataIndex: 'userAccount',
    align: 'center' as const,
  },
  {
    title: '用户名',
    dataIndex: 'userName',
    align: 'center' as const,
  },
  {
    title: '头像',
    dataIndex: 'userAvatar',
    align: 'center' as const,
  },
  {
    title: '简介',
    dataIndex: 'userProfile',
    align: 'center' as const,
  },
  {
    title: '用户角色',
    dataIndex: 'userRole',
    align: 'center' as const,
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
    align: 'center' as const,
  },
  {
    title: '操作',
    key: 'action',
    align: 'center' as const,
  },
];
/**
 * 用户列表
 */
const data = ref<API.UserVO[]>([])
const total = ref(0)

const searchParams = ref<API.UserQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

const getUserList = async () => {
  // 二次权限校验
  if (loginUserStore.loginUser.userRole !== 'admin') {
    message.error('无权限访问')
    router.push('/')
    return
  }
  
  const res = await listUserVoByPage(searchParams.value)
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取用户列表失败' + res.data.message)
  }
}
// 分页参数
const pagination = computed(()=> {
  return {
    current: searchParams.value.pageNum ?? 1,
    pageSize: searchParams.value.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})
// 分页变化
const doTableChange: TableProps['onChange'] = (page) => {
  searchParams.value.pageNum = page.current ?? 1
  searchParams.value.pageSize = page.pageSize ?? 10
  getUserList()
}

// 搜索
const doSearch = () => {
  searchParams.value.pageNum = 1
  searchParams.value.pageSize = 10
  getUserList()
}

// 编辑
const doEdit = (record: API.UserVO) => {
  editingKey.value = record.id ?? null
  editingRecord.id = record.id
  editingRecord.userName = record.userName ?? ''
  editingRecord.userAvatar = record.userAvatar ?? ''
  editingRecord.userProfile = record.userProfile ?? ''
  editingRecord.userRole = record.userRole ?? ''
}

// 取消编辑
const cancelEdit = () => {
  editingKey.value = null
  editingRecord.id = undefined
  editingRecord.userName = ''
  editingRecord.userAvatar = ''
  editingRecord.userProfile = ''
  editingRecord.userRole = ''
}

// 保存编辑
const doSave = async () => {
  try {
    const res = await updateUser({
      id: editingRecord.id,
      userName: editingRecord.userName,
      userAvatar: editingRecord.userAvatar,
      userProfile: editingRecord.userProfile,
      userRole: editingRecord.userRole,
    })
    
    if (res.data.code === 0) {
      message.success('更新成功')
      cancelEdit()
      getUserList()
    } else {
      message.error('更新失败：' + res.data.message)
    }
  } catch {
    message.error('更新失败，请稍后重试')
  }
}

// 删除
const doDelete = (id: number) => {
  Modal.confirm({
    title: h('div', { style: { fontSize: '18px', fontWeight: '600', color: '#1a1a1a' } }, '确认删除'),
    icon: h(ExclamationCircleOutlined, { style: { color: '#ff4d4f' } }),
    content: h(
      'div',
      { style: { fontSize: '14px', color: '#666', lineHeight: '1.6', marginTop: '8px' } },
      '确定要删除该用户吗？删除后将无法恢复，请谨慎操作。'
    ),
    okText: '删除',
    cancelText: '取消',
    okType: 'danger',
    centered: true,
    width: 420,
    onOk: async () => {
      try {
        const res = await deleteUser({ id })
        if (res.data.code === 0) {
          message.success('删除成功')
          getUserList()
        } else {
          message.error('删除失败：' + res.data.message)
        }
      } catch {
        message.error('删除失败，请稍后重试')
      }
    },
  })
}

// 页面加载时获取用户列表
onMounted(() => {
  getUserList()
})
</script>

<style>

.ant-modal-confirm .ant-modal-content {
  border-radius: 12px !important;
  padding: 24px !important;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12), 0 4px 16px rgba(0, 0, 0, 0.08) !important;
}

.ant-modal-confirm .ant-modal-body {
  padding: 0 !important;
}

.ant-modal-confirm .ant-modal-confirm-body {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}

.ant-modal-confirm .ant-modal-confirm-title {
  font-weight: 600 !important;
  font-size: 18px !important;
  color: #1a1a1a !important;
}

.ant-modal-confirm .ant-modal-confirm-content {
  margin-top: 8px !important;
  font-size: 14px !important;
  color: #666 !important;
  line-height: 1.6 !important;
}

.ant-modal-confirm .ant-modal-confirm-btns {
  margin-top: 24px !important;
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.ant-modal-confirm .ant-btn {
  height: 36px !important;
  border-radius: 6px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  padding: 0 20px !important;
  transition: all 0.2s ease !important;
}

.ant-modal-confirm .ant-btn-default {
  background: #f5f5f5 !important;
  border: none !important;
  color: #333 !important;
}

.ant-modal-confirm .ant-btn-default:hover {
  background: #e8e8e8 !important;
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1) !important;
}

.ant-modal-confirm .ant-btn-dangerous {
  background: #ff4d4f !important;
  border: none !important;
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.2) !important;
}

.ant-modal-confirm .ant-btn-dangerous:hover {
  background: #ff7875 !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(255, 77, 79, 0.3) !important;
}

.ant-modal-mask {
  backdrop-filter: blur(4px) !important;
  background-color: rgba(0, 0, 0, 0.35) !important;
}

/* 可编辑表格样式 */
.action-buttons {
  display: flex;
  gap: 8px;
  justify-content: center;
  align-items: center;
}

.action-buttons.editing {
  gap: 12px;
}

/* 保存按钮 */
.save-btn {
  background: #52c41a !important;
  border-color: #52c41a !important;
  box-shadow: 0 2px 8px rgba(82, 196, 26, 0.3);
  transition: all 0.3s ease;
}

.save-btn:hover {
  background: #73d13d !important;
  border-color: #73d13d !important;
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(82, 196, 26, 0.4);
}

/* 取消按钮 */
.cancel-btn {
  background: #fff !important;
  border-color: #d9d9d9 !important;
  color: rgba(0, 0, 0, 0.65) !important;
  transition: all 0.3s ease;
}

.cancel-btn:hover {
  background: #ff4d4f !important;
  border-color: #ff4d4f !important;
  color: #fff !important;
  transform: scale(1.1);
  box-shadow: 0 2px 8px rgba(255, 77, 79, 0.3);
}

/* 编辑和删除链接按钮 */
.edit-link,
.delete-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.edit-link:hover {
  background: #e6f7ff;
}

.delete-link:hover {
  background: #fff1f0;
}

.editable-cell {
  position: relative;
}

.editable-cell-input-wrapper,
.editable-cell-text-wrapper {
  padding: 5px 12px;
}

.editable-cell-text-wrapper {
  padding: 5px 24px 5px 5px;
}

.editable-row:hover .editable-cell-icon {
  display: inline-block;
}

.editable-cell-icon {
  line-height: 18px;
  display: none;
}

.editable-cell-icon-check {
  line-height: 28px;
}

.editable-cell-icon:hover,
.editable-cell-icon-check:hover {
  color: #108ee9;
}

.editable-add-btn {
  margin-bottom: 8px;
}
</style>