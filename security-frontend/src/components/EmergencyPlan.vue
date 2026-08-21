<template>
  <div class="emergency-plan-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <h2 class="page-title">应急预案管理</h2>
      <el-button v-if="isAdmin" type="primary" @click="openDialog()">
        <el-icon><Plus /></el-icon> 添加预案
      </el-button>
    </div>

    <!-- 预案表格 -->
    <el-table :data="plans" stripe border style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" align="center" />
      <el-table-column prop="alertType" label="告警类型" width="150" />
      <el-table-column prop="title" label="预案标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="处理步骤" min-width="320">
        <template #default="{ row }">
          <div class="steps-list">
            <div v-for="(step, idx) in splitSteps(row.steps)" :key="idx" class="step-item">
              <span class="step-marker">{{ idx + 1 }}.</span>
              <span class="step-text">{{ step }}</span>
            </div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" align="center" fixed="right">
        <template #default="{ row }">
          <el-button v-if="isAdmin" type="primary" link @click="openDialog(row)">
            <el-icon><Edit /></el-icon> 编辑
          </el-button>
          <el-button v-if="isAdmin" type="danger" link @click="deletePlan(row.id)">
            <el-icon><Delete /></el-icon> 删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 添加/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      destroy-on-close
      :close-on-click-modal="false"
    >
      <el-form :model="form" label-width="100px" label-position="right" :rules="rules" ref="formRef">
        <el-form-item label="告警类型" prop="alertType">
          <el-input v-model="form.alertType" placeholder="例如：烟雾超标、高温告警" />
        </el-form-item>
        <el-form-item label="预案标题" prop="title">
          <el-input v-model="form.title" placeholder="简要描述预案用途" />
        </el-form-item>
        <el-form-item label="处理步骤" prop="steps">
          <el-input
            v-model="form.steps"
            type="textarea"
            :rows="6"
            placeholder="每行写一个步骤，例如：&#10;立即查看告警信息，确认发生位置&#10;通知现场人员撤离至安全区域&#10;启动排烟系统"
          />
          <div class="steps-hint">💡 提示：每行写一个步骤，保存后将自动编号显示。</div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
/* eslint-disable */
import { ref, onMounted, computed } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

// 数据
const plans = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref('添加预案')
const formRef = ref(null)
const form = ref({
  id: null,
  alertType: '',
  title: '',
  steps: ''
})

const props = defineProps({
  userRole: {
    type: String,
    default: 'user'
  }
})
const isAdmin = computed(() => props.userRole === 'admin')

// 表单校验规则
const rules = {
  alertType: [{ required: true, message: '请输入告警类型', trigger: 'blur' }],
  title: [{ required: true, message: '请输入预案标题', trigger: 'blur' }],
  steps: [{ required: true, message: '请输入处理步骤', trigger: 'blur' }]
}

// 将 steps 按换行拆分为数组（自动去除空行）
const splitSteps = (stepsStr) => {
  if (!stepsStr) return []
  return stepsStr.split(/\r?\n/).filter(line => line.trim().length > 0)
}

// 获取预案列表
const fetchPlans = async () => {
  try {
    const res = await axios.get('/api/plan/list')
    plans.value = res.data
  } catch (error) {
    ElMessage.error('获取预案列表失败')
  }
}

// 打开弹窗（新增/编辑）
const openDialog = (row = null) => {
  if (row) {
    dialogTitle.value = '编辑预案'
    form.value = { ...row }
  } else {
    dialogTitle.value = '添加预案'
    form.value = { id: null, alertType: '', title: '', steps: '' }
  }
  dialogVisible.value = true
  // 清除校验
  if (formRef.value) formRef.value.clearValidate()
}

// 保存预案
const savePlan = async () => {
  if (!form.value.alertType || !form.value.title || !form.value.steps) {
    ElMessage.warning('请完整填写所有字段')
    return
  }
  try {
    await axios.post('/api/plan/save', form.value)
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchPlans()
  } catch (error) {
    ElMessage.error('保存失败：' + (error.response?.data?.message || error.message))
  }
}

// 删除预案
const deletePlan = async (id) => {
  ElMessageBox.confirm('确定删除该应急预案吗？删除后不可恢复。', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    try {
      await axios.delete(`/api/plan/delete/${id}`)
      ElMessage.success('删除成功')
      fetchPlans()
    } catch (error) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

onMounted(() => {
  fetchPlans()
})
</script>

<style scoped>
.emergency-plan-container {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #1f2d3d;
}

/* 步骤列表样式 */
.steps-list {
  line-height: 1.7;
}

.step-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 6px;
  font-size: 14px;
}

.step-marker {
  display: inline-block;
  width: 24px;
  flex-shrink: 0;
  color: #409eff;
  font-weight: 500;
}

.step-text {
  flex: 1;
  color: #2c3e50;
  word-break: break-word;
}

.steps-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
  background: #f5f7fa;
  padding: 6px 10px;
  border-radius: 4px;
}
</style>