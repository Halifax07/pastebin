<template>
  <div class="view-container">
    <!-- Header -->
    <div class="header">
      <div class="header-left">
        <div class="logo-wrapper" @click="goHome" style="cursor: pointer;">
          <span class="logo-icon">🔐</span>
          <h1 class="logo">Secure Pastebin</h1>
        </div>
        <div class="tags-wrapper">
          <el-tag v-if="pasteData" class="custom-tag lang-tag">
            <el-icon><Document /></el-icon>
            {{ getLanguageLabel(pasteData.syntax) }}
          </el-tag>
          <el-tag v-if="pasteData?.isBurnAfterReading" class="custom-tag burn-tag">
            🔥 阅后即焚
          </el-tag>
        </div>
      </div>
      <div class="header-right">
        <el-button @click="handleAISummary" :loading="aiLoading" class="action-btn ai-btn">
          <span>✨ AI 总结</span>
        </el-button>
        <el-button @click="copyToClipboard" class="action-btn">
          <el-icon><CopyDocument /></el-icon>
          <span>复制代码</span>
        </el-button>
        <el-button @click="goHome" type="primary" class="action-btn create-btn">
          <el-icon><Plus /></el-icon>
          <span>创建新的</span>
        </el-button>
      </div>
    </div>

    <!-- Loading State -->
    <div v-if="loading" class="loading-container">
      <div class="loading-spinner"></div>
      <p>正在加载内容...</p>
    </div>

    <!-- Password Dialog -->
    <el-dialog
      v-model="showPasswordDialog"
      title="🔐 需要密码"
      width="420px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="custom-dialog"
    >
      <div class="dialog-content">
        <p class="dialog-desc">此内容已加密，请输入密码以查看</p>
        <el-input
          v-model="password"
          placeholder="请输入访问密码"
          type="password"
          show-password
          size="large"
          @keyup.enter="handlePasswordSubmit"
        />
      </div>
      <template #footer>
        <el-button @click="goHome" size="large">取消</el-button>
        <el-button type="primary" @click="handlePasswordSubmit" size="large">
          解密查看
        </el-button>
      </template>
    </el-dialog>

    <!-- Burn Warning Dialog -->
    <el-dialog
      v-model="showBurnWarning"
      title="⚠️ 阅后即焚警告"
      width="500px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      :show-close="false"
      class="custom-dialog"
    >
      <div class="burn-warning-content">
        <div class="warning-icon">🔥</div>
        <p class="warning-text">此内容设置为"阅后即焚"</p>
        <p class="warning-desc">一旦查看将立即销毁且无法恢复！</p>
      </div>
      <template #footer>
        <el-button @click="goHome" size="large">取消</el-button>
        <el-button type="danger" @click="confirmBurn" size="large">
          我知道了，继续查看
        </el-button>
      </template>
    </el-dialog>

    <!-- AI Summary Card -->
    <div v-if="aiSummary" class="ai-summary-container">
      <div class="ai-summary-card">
        <div class="summary-header">
          <span class="summary-title">✨ AI 智能总结</span>
          <el-button text @click="aiSummary = ''" class="close-btn">
            <el-icon><Close /></el-icon>
          </el-button>
        </div>
        <div class="summary-content">
          {{ aiSummary }}
        </div>
      </div>
    </div>

    <!-- Editor (Read-only) -->
    <div v-if="pasteData && !loading && displayContent" class="editor-wrapper">
      <div class="editor-container">
        <vue-monaco-editor
          v-model:value="displayContent"
          :language="pasteData.syntax"
          :options="readOnlyEditorOptions"
          theme="vs-dark"
        />
      </div>
    </div>

    <!-- Error State -->
    <div v-if="error" class="error-container">
      <div class="error-content">
        <div class="error-icon">😢</div>
        <h2>内容不可用</h2>
        <p>{{ error }}</p>
        <el-button type="primary" @click="goHome" size="large" class="back-btn">
          返回首页
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { CopyDocument, Loading, Close, MagicStick, Document, Plus } from '@element-plus/icons-vue'
import { LANGUAGE_OPTIONS } from '@/utils/constants'
import { decrypt, isEncrypted } from '@/utils/crypto'
import { getPaste } from '@/api/paste'
import { summarizeContent } from '@/api/ai'
import type { PasteData } from '@/types/paste'

const route = useRoute()
const router = useRouter()

const loading = ref(true)
const error = ref('')
const pasteData = ref<PasteData | null>(null)
const decryptedContent = ref('')
const showPasswordDialog = ref(false)
const showBurnWarning = ref(false)
const password = ref('')
const needsPassword = ref(false)
const aiLoading = ref(false)
const aiSummary = ref('')

const readOnlyEditorOptions = {
  fontSize: 14,
  minimap: { enabled: true },
  automaticLayout: true,
  scrollBeyondLastLine: false,
  readOnly: true,
  cursorStyle: 'line',
  renderLineHighlight: 'none',
  wordWrap: 'on',
  padding: { top: 16, bottom: 16 },
  smoothScrolling: true,
}

// 计算显示的内容（解密后或原始内容）
const displayContent = computed(() => {
  if (needsPassword.value && !decryptedContent.value) {
    return ''
  }
  return decryptedContent.value || pasteData.value?.content || ''
})

const getLanguageLabel = (value: string) => {
  return LANGUAGE_OPTIONS.find(lang => lang.value === value)?.label || value
}

const goHome = () => {
  router.push('/')
}

const copyToClipboard = async () => {
  const content = displayContent.value
  if (!content) {
    ElMessage.warning('没有可复制的内容')
    return
  }
  
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    ElMessage.error('复制失败')
  }
}

const handleAISummary = async () => {
  const content = displayContent.value
  if (!content) {
    ElMessage.warning('没有可总结的内容')
    return
  }

  if (aiLoading.value) {
    return
  }

  aiLoading.value = true
  aiSummary.value = ''

  try {
    const response = await summarizeContent({ content })
    aiSummary.value = response.summary
    ElMessage.success(`总结完成（使用 ${response.tokens} tokens）`)
  } catch (error: any) {
    console.error('AI 总结失败:', error)
    ElMessage.error(error.response?.data?.summary || 'AI 总结失败，请重试')
  } finally {
    aiLoading.value = false
  }
}

const handlePasswordSubmit = () => {
  if (!password.value.trim()) {
    ElMessage.warning('请输入密码')
    return
  }

  try {
    if (!pasteData.value) {
      ElMessage.error('数据加载失败')
      return
    }

    // 尝试解密
    const decrypted = decrypt(pasteData.value.content, password.value)
    decryptedContent.value = decrypted
    needsPassword.value = false
    showPasswordDialog.value = false
    
    ElMessage.success('解密成功！')
  } catch (error: any) {
    ElMessage.error(error.message || '密码错误，请重试')
    password.value = ''
  }
}

const confirmBurn = async () => {
  showBurnWarning.value = false
  
  // 阅后即焚确认后，重新加载数据（此时会触发后端删除）
  await loadPasteData(true)
}

const loadPasteData = async (skipBurnWarning: boolean = false) => {
  const key = route.params.id as string
  
  if (!key) {
    error.value = '无效的链接'
    loading.value = false
    return
  }

  loading.value = true
  
  try {
    const data = await getPaste(key)
    pasteData.value = data

    // 检查是否加密
    if (isEncrypted(data.content)) {
      needsPassword.value = true
      showPasswordDialog.value = true
      loading.value = false
      return
    }

    // 如果不加密，直接显示
    decryptedContent.value = data.content
    loading.value = false
    
  } catch (err: any) {
    console.error('加载失败:', err)
    error.value = err.response?.data?.message || err.message || '内容不存在或已过期'
    loading.value = false
  }
}

onMounted(async () => {
  const key = route.params.id as string
  
  if (!key) {
    error.value = '无效的链接'
    loading.value = false
    return
  }

  // 首次加载时，先获取数据判断是否阅后即焚
  try {
    const data = await getPaste(key)
    
    // 如果是阅后即焚，先显示警告
    if (data.isBurnAfterReading) {
      pasteData.value = data
      showBurnWarning.value = true
      loading.value = false
      return
    }

    // 不是阅后即焚，直接加载
    pasteData.value = data

    // 检查是否加密
    if (isEncrypted(data.content)) {
      needsPassword.value = true
      showPasswordDialog.value = true
      loading.value = false
      return
    }

    // 不加密，直接显示
    decryptedContent.value = data.content
    loading.value = false
    
  } catch (err: any) {
    console.error('加载失败:', err)
    error.value = err.response?.data?.message || err.message || '内容不存在或已过期'
    loading.value = false
  }
})
</script>

<style scoped>
.view-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 32px;
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 24px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 12px;
  transition: transform 0.2s ease;
}

.logo-wrapper:hover {
  transform: scale(1.02);
}

.logo-icon {
  font-size: 28px;
}

.logo {
  font-size: 24px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.tags-wrapper {
  display: flex;
  gap: 12px;
  padding-left: 24px;
  border-left: 1px solid rgba(255, 255, 255, 0.2);
}

.custom-tag {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 6px 14px;
  border-radius: 20px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.lang-tag {
  background: rgba(102, 126, 234, 0.2);
  border-color: rgba(102, 126, 234, 0.4);
}

.burn-tag {
  background: rgba(245, 108, 108, 0.2);
  border-color: rgba(245, 108, 108, 0.4);
  color: #f56c6c;
}

.header-right {
  display: flex;
  gap: 12px;
}

.action-btn {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 10px 20px;
  border-radius: 8px;
  transition: all 0.3s ease;
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-btn:hover {
  background: rgba(255, 255, 255, 0.2);
  transform: translateY(-2px);
}

.ai-btn {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.2) 0%, rgba(255, 152, 0, 0.2) 100%);
  border-color: rgba(255, 193, 7, 0.4);
}

.ai-btn:hover {
  background: linear-gradient(135deg, rgba(255, 193, 7, 0.3) 0%, rgba(255, 152, 0, 0.3) 100%);
}

.create-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.3);
}

.create-btn:hover {
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.5);
}

.editor-wrapper {
  flex: 1;
  padding: 16px 32px;
  overflow: hidden;
}

.editor-container {
  height: 100%;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.3);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.loading-container {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  color: rgba(255, 255, 255, 0.8);
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-container p {
  margin-top: 20px;
  font-size: 16px;
}

.error-container {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
}

.error-content {
  text-align: center;
  color: #fff;
}

.error-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.error-content h2 {
  font-size: 28px;
  margin-bottom: 12px;
}

.error-content p {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.6);
  margin-bottom: 30px;
}

.back-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 12px 32px;
}

.ai-summary-container {
  margin: 16px 32px 0;
  animation: slideDown 0.3s ease;
}

@keyframes slideDown {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.ai-summary-card {
  background: rgba(255, 193, 7, 0.1);
  border: 1px solid rgba(255, 193, 7, 0.3);
  border-radius: 12px;
  overflow: hidden;
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: rgba(255, 193, 7, 0.1);
  border-bottom: 1px solid rgba(255, 193, 7, 0.2);
}

.summary-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffc107;
}

.close-btn {
  color: rgba(255, 255, 255, 0.6);
}

.close-btn:hover {
  color: #fff;
}

.summary-content {
  padding: 20px;
  color: rgba(255, 255, 255, 0.9);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
  word-break: break-word;
}

/* Dialog Styles */
.custom-dialog {
  background: rgba(30, 30, 46, 0.95);
  backdrop-filter: blur(10px);
}

.dialog-content {
  padding: 10px 0;
}

.dialog-desc {
  color: rgba(255, 255, 255, 0.7);
  margin-bottom: 20px;
  text-align: center;
}

.burn-warning-content {
  text-align: center;
  padding: 20px 0;
}

.warning-icon {
  font-size: 64px;
  margin-bottom: 16px;
  animation: pulse 1.5s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.warning-text {
  font-size: 20px;
  font-weight: 600;
  color: #f56c6c;
  margin-bottom: 8px;
}

.warning-desc {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
}

:deep(.el-dialog) {
  background: rgba(30, 30, 46, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
}

:deep(.el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.el-dialog__title) {
  color: #fff;
  font-size: 18px;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: #fff;
}
</style>
