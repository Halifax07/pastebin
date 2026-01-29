<template>
  <div class="home-container">
    <!-- Header -->
    <div class="header">
      <div class="header-left">
        <div class="logo-wrapper">
          <span class="logo-icon">🔐</span>
          <h1 class="logo">Secure Pastebin</h1>
        </div>
        <span class="tagline">安全 · 加密 · 私密分享</span>
      </div>
      <div class="header-right">
        <el-button type="primary" size="large" @click="handleSave" :loading="saving" class="save-btn">
          <el-icon><Share /></el-icon>
          <span>保存并分享</span>
        </el-button>
      </div>
    </div>

    <!-- Share Dialog -->
    <el-dialog
      v-model="showShareDialog"
      title="🎉 分享链接已生成"
      width="560px"
      :close-on-click-modal="false"
      class="share-dialog"
    >
      <div class="share-content">
        <p class="share-tip">将以下链接发送给任何人，即可查看内容：</p>
        
        <div class="share-link-box">
          <el-input
            v-model="shareUrl"
            readonly
            size="large"
            class="share-input"
          >
            <template #append>
              <el-button @click="copyShareLink" type="primary">
                <el-icon><CopyDocument /></el-icon>
                复制
              </el-button>
            </template>
          </el-input>
        </div>

        <div class="share-info">
          <div v-if="settings.password" class="info-item warning">
            <el-icon><Lock /></el-icon>
            <span>此内容已加密，查看者需要输入密码：<strong>{{ settings.password }}</strong></span>
          </div>
          <div v-if="settings.isBurnAfterReading" class="info-item danger">
            <span>🔥</span>
            <span>阅后即焚已开启，内容被查看后将自动销毁</span>
          </div>
          <div v-if="settings.expireMinutes" class="info-item">
            <el-icon><Clock /></el-icon>
            <span>链接将在 {{ getExpireLabel(settings.expireMinutes) }} 后过期</span>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="handleCreateNew" size="large">
          继续创建
        </el-button>
        <el-button type="primary" @click="handleViewPaste" size="large">
          查看内容
        </el-button>
      </template>
    </el-dialog>

    <!-- Editor -->
    <div class="editor-wrapper">
      <div class="editor-container">
        <vue-monaco-editor
          v-model:value="code"
          :language="settings.language"
          :options="editorOptions"
          theme="vs-dark"
          @mount="handleEditorMount"
        />
      </div>
    </div>

    <!-- Settings Panel -->
    <div class="settings-panel">
      <div class="settings-row">
        <div class="setting-item">
          <el-icon class="setting-icon"><Document /></el-icon>
          <label>语言</label>
          <el-select v-model="settings.language" placeholder="选择语言" class="setting-select">
            <el-option
              v-for="lang in LANGUAGE_OPTIONS"
              :key="lang.value"
              :label="lang.label"
              :value="lang.value"
            />
          </el-select>
        </div>

        <div class="setting-item">
          <el-icon class="setting-icon"><Clock /></el-icon>
          <label>过期时间</label>
          <el-select v-model="settings.expireMinutes" placeholder="选择过期时间" class="setting-select">
            <el-option
              v-for="option in EXPIRE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <div class="setting-item">
          <el-icon class="setting-icon"><Lock /></el-icon>
          <label>访问密码</label>
          <el-input
            v-model="settings.password"
            placeholder="可选，留空无需密码"
            class="setting-input"
            type="password"
            show-password
            clearable
          />
        </div>

        <div class="setting-item burn-switch">
          <el-tooltip content="开启后，内容被查看一次后将自动销毁" placement="top">
            <el-switch
              v-model="settings.isBurnAfterReading"
              active-text="🔥 阅后即焚"
              inactive-text="普通模式"
              active-color="#f56c6c"
            />
          </el-tooltip>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Share, Document, Clock, Lock, CopyDocument } from '@element-plus/icons-vue'
import { LANGUAGE_OPTIONS, EXPIRE_OPTIONS } from '@/utils/constants'
import { encrypt } from '@/utils/crypto'
import { generateShareUrl } from '@/utils/config'
import { createPaste } from '@/api/paste'
import type { PasteSettings } from '@/types/paste'

const router = useRouter()

// Monaco Editor
const code = ref('')
const saving = ref(false)
const showShareDialog = ref(false)
const shareUrl = ref('')
const savedKey = ref('')

const settings = reactive<PasteSettings>({
  language: 'plaintext',
  isBurnAfterReading: false,
  expireMinutes: null,
  password: ''
})

const editorOptions = {
  fontSize: 14,
  minimap: { enabled: true },
  automaticLayout: true,
  scrollBeyondLastLine: false,
  tabSize: 2,
  wordWrap: 'on',
  padding: { top: 16, bottom: 16 },
  lineNumbers: 'on',
  renderLineHighlight: 'all',
  cursorBlinking: 'smooth',
  smoothScrolling: true,
}

const handleEditorMount = (editor: any) => {
  // 编辑器挂载后可以进行额外配置
  editor.focus()
}

const handleSave = async () => {
  if (!code.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }

  saving.value = true
  
  try {
    let contentToSave = code.value

    // 如果设置了密码，进行客户端加密
    if (settings.password.trim()) {
      try {
        contentToSave = encrypt(code.value, settings.password)
        console.log('✅ 内容已加密')
      } catch (error: any) {
        ElMessage.error(error.message || '加密失败')
        saving.value = false
        return
      }
    }

    // 调用后端 API 保存
    const response = await createPaste({
      content: contentToSave,
      syntax: settings.language,
      isBurnAfterReading: settings.isBurnAfterReading,
      expireMinutes: settings.expireMinutes
    })

    // 生成完整的分享链接
    savedKey.value = response.key
    shareUrl.value = generateShareUrl(response.key)
    
    // 显示分享弹窗
    showShareDialog.value = true
    ElMessage.success('🎉 保存成功！')
    
  } catch (error: any) {
    console.error('保存失败:', error)
    ElMessage.error(error.response?.data?.message || '保存失败，请重试')
  } finally {
    saving.value = false
  }
}

// 复制分享链接
const copyShareLink = async () => {
  try {
    await navigator.clipboard.writeText(shareUrl.value)
    ElMessage.success('链接已复制到剪贴板！')
  } catch (err) {
    // 降级方案
    const input = document.createElement('input')
    input.value = shareUrl.value
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('链接已复制到剪贴板！')
  }
}

// 查看内容
const handleViewPaste = () => {
  showShareDialog.value = false
  router.push(`/${savedKey.value}`)
}

// 继续创建新内容
const handleCreateNew = () => {
  showShareDialog.value = false
  code.value = ''
  settings.language = 'plaintext'
  settings.isBurnAfterReading = false
  settings.expireMinutes = null
  settings.password = ''
}

// 获取过期时间标签
const getExpireLabel = (minutes: number) => {
  const option = EXPIRE_OPTIONS.find(opt => opt.value === minutes)
  return option ? option.label : `${minutes} 分钟`
}
</script>

<style scoped>
.home-container {
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
}

.logo-icon {
  font-size: 32px;
  animation: pulse 2s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.1); }
}

.logo {
  font-size: 28px;
  font-weight: 700;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin: 0;
}

.tagline {
  color: rgba(255, 255, 255, 0.6);
  font-size: 14px;
  padding-left: 24px;
  border-left: 1px solid rgba(255, 255, 255, 0.2);
}

.save-btn {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 12px 28px;
  font-size: 16px;
  font-weight: 600;
  border-radius: 8px;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(102, 126, 234, 0.4);
}

.save-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.6);
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

.settings-panel {
  background: rgba(0, 0, 0, 0.3);
  backdrop-filter: blur(10px);
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  padding: 20px 32px;
}

.settings-row {
  display: flex;
  align-items: center;
  gap: 40px;
  flex-wrap: wrap;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.setting-icon {
  color: rgba(255, 255, 255, 0.6);
  font-size: 18px;
}

.setting-item label {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  white-space: nowrap;
  font-weight: 500;
}

.setting-select {
  width: 160px;
}

.setting-input {
  width: 200px;
}

.burn-switch {
  margin-left: auto;
}

:deep(.el-select) {
  --el-fill-color-blank: rgba(255, 255, 255, 0.1);
  --el-text-color-regular: #fff;
  --el-border-color: rgba(255, 255, 255, 0.2);
}

:deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: none;
}

:deep(.el-input__inner) {
  color: #fff;
}

:deep(.el-input__inner::placeholder) {
  color: rgba(255, 255, 255, 0.4);
}

:deep(.el-switch__label) {
  color: rgba(255, 255, 255, 0.7);
}

:deep(.el-switch__label.is-active) {
  color: #fff;
}

/* Share Dialog Styles */
:deep(.share-dialog .el-dialog) {
  background: rgba(30, 30, 46, 0.98);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
}

:deep(.share-dialog .el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.share-dialog .el-dialog__title) {
  color: #fff;
  font-size: 20px;
}

:deep(.share-dialog .el-dialog__body) {
  padding: 24px;
}

:deep(.share-dialog .el-dialog__footer) {
  padding: 16px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.share-content {
  color: #fff;
}

.share-tip {
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
  margin-bottom: 16px;
}

.share-link-box {
  margin-bottom: 20px;
}

.share-input :deep(.el-input__wrapper) {
  background: rgba(102, 126, 234, 0.1);
  border: 2px solid rgba(102, 126, 234, 0.4);
}

.share-input :deep(.el-input__inner) {
  color: #667eea;
  font-size: 15px;
  font-weight: 500;
}

.share-input :deep(.el-input-group__append) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  padding: 0;
}

.share-input :deep(.el-input-group__append .el-button) {
  background: transparent;
  border: none;
  color: #fff;
  padding: 8px 20px;
}

.share-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.info-item.warning {
  background: rgba(230, 162, 60, 0.15);
  border: 1px solid rgba(230, 162, 60, 0.3);
  color: #e6a23c;
}

.info-item.warning strong {
  color: #fff;
  background: rgba(230, 162, 60, 0.3);
  padding: 2px 8px;
  border-radius: 4px;
  margin-left: 4px;
}

.info-item.danger {
  background: rgba(245, 108, 108, 0.15);
  border: 1px solid rgba(245, 108, 108, 0.3);
  color: #f56c6c;
}

.info-item .el-icon {
  font-size: 18px;
}
</style>
