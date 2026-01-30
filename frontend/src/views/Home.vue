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
        <el-button type="primary" size="default" @click="handleSave" :loading="saving" class="save-btn">
          <el-icon><Share /></el-icon>
          <span>保存并分享</span>
        </el-button>
      </div>
    </div>

    <!-- Toolbar -->
    <div class="toolbar-panel">
      <div class="toolbar-scroll">
        <div class="setting-item">
          <el-icon class="setting-icon"><Document /></el-icon>
          <label>语言</label>
          <el-select v-model="settings.language" placeholder="选择语言" class="setting-select" size="default">
            <el-option
              v-for="lang in LANGUAGE_OPTIONS"
              :key="lang.value"
              :label="lang.label"
              :value="lang.value"
            />
          </el-select>
        </div>

        <div class="divider"></div>

        <div class="setting-item">
          <el-icon class="setting-icon"><Clock /></el-icon>
          <label>过期时间</label>
          <el-select v-model="settings.expireMinutes" placeholder="选择过期时间" class="setting-select" size="default">
            <el-option
              v-for="option in EXPIRE_OPTIONS"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
        </div>

        <div class="divider"></div>

        <div class="setting-item">
          <el-icon class="setting-icon"><Lock /></el-icon>
          <label>密码保护</label>
          <el-input
            v-model="settings.password"
            placeholder="可选，留空公开"
            class="setting-input"
            type="password"
            show-password
            clearable
            size="default"
          />
        </div>

        <div class="burn-switch-wrapper">
          <el-tooltip content="内容被查看一次后自动销毁" placement="bottom">
            <el-switch
              v-model="settings.isBurnAfterReading"
              active-text="阅后即焚"
              inactive-text=""
              inline-prompt
              style="--el-switch-on-color: #f56c6c;"
            />
          </el-tooltip>
        </div>
      </div>
    </div>

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

    <!-- Share Dialog -->
    <el-dialog
      v-model="showShareDialog"
      width="520px"
      :close-on-click-modal="false"
      :show-close="true"
      class="share-dialog"
      align-center
    >
      <template #header>
        <div class="share-dialog-header">
          <div class="success-icon">
            <el-icon :size="28"><SuccessFilled /></el-icon>
          </div>
          <div class="header-text">
            <h3>分享链接已生成</h3>
            <p>将链接发送给任何人即可查看内容</p>
          </div>
        </div>
      </template>

      <div class="share-content">
        <!-- 链接复制区域 -->
        <div class="link-section">
          <label class="section-label">
            <el-icon><Link /></el-icon>
            分享链接
          </label>
          <div class="link-copy-box">
            <div class="link-text">{{ shareUrl }}</div>
            <el-button 
              type="primary" 
              @click="copyShareLink" 
              class="copy-btn"
              :icon="CopyDocument"
            >
              复制链接
            </el-button>
          </div>
        </div>

        <!-- 信息卡片区域 -->
        <div class="info-cards">
          <!-- 密码卡片 -->
          <div v-if="settings.password" class="info-card password-card">
            <div class="card-icon">
              <el-icon><Lock /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-label">访问密码</div>
              <div class="card-value password-value">
                <span class="password-text">{{ settings.password }}</span>
                <el-button 
                  text 
                  size="small" 
                  @click="copyPassword"
                  class="copy-password-btn"
                >
                  <el-icon><CopyDocument /></el-icon>
                </el-button>
              </div>
            </div>
          </div>

          <!-- 过期时间卡片 -->
          <div v-if="settings.expireMinutes" class="info-card expire-card">
            <div class="card-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="card-content">
              <div class="card-label">有效期</div>
              <div class="card-value">{{ getExpireLabel(settings.expireMinutes) }}</div>
            </div>
          </div>

          <!-- 阅后即焚提示 -->
          <div v-if="settings.isBurnAfterReading" class="info-card burn-card">
            <div class="card-icon burn-icon">
              <span>🔥</span>
            </div>
            <div class="card-content">
              <div class="card-label">阅后即焚</div>
              <div class="card-value">查看后自动销毁</div>
            </div>
          </div>
        </div>

        <!-- 安全提示 -->
        <div class="security-tip" v-if="settings.password">
          <el-icon><InfoFilled /></el-icon>
          <span>请将密码与链接一同发送给接收者</span>
        </div>
      </div>
      
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="handleCreateNew" size="large" class="footer-btn">
            <el-icon><Plus /></el-icon>
            继续创建
          </el-button>
          <el-button type="primary" @click="handleViewPaste" size="large" class="footer-btn primary-btn">
            <el-icon><View /></el-icon>
            查看内容
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Share, Document, Clock, Lock, CopyDocument, SuccessFilled, Link, InfoFilled, Plus, View } from '@element-plus/icons-vue'
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

// 复制密码
const copyPassword = async () => {
  try {
    await navigator.clipboard.writeText(settings.password)
    ElMessage.success('密码已复制到剪贴板！')
  } catch (err) {
    const input = document.createElement('input')
    input.value = settings.password
    document.body.appendChild(input)
    input.select()
    document.execCommand('copy')
    document.body.removeChild(input)
    ElMessage.success('密码已复制到剪贴板！')
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
  background-color: #0d1117; /* GitHub Dark Dimmed 背景色 */
  color: #c9d1d9;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 24px;
  background-color: #161b22;
  border-bottom: 1px solid #30363d;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.logo-wrapper {
  display: flex;
  align-items: center;
  gap: 10px;
}

.logo-icon {
  font-size: 24px;
}

.logo {
  font-size: 20px;
  font-weight: 600;
  color: #c9d1d9;
  margin: 0;
  letter-spacing: 0.5px;
}

.tagline {
  color: #8b949e;
  font-size: 13px;
  padding-left: 16px;
  border-left: 1px solid #30363d;
  display: none; /* 小屏幕隐藏 */
}

@media (min-width: 768px) {
  .tagline {
    display: inline-block;
  }
}

.save-btn {
  background-color: #238636;
  border: 1px solid rgba(240, 246, 252, 0.1);
  color: #ffffff;
  font-weight: 500;
  transition: 0.2s;
}

.save-btn:hover {
  background-color: #2ea043;
}

/* Toolbar Panel */
.toolbar-panel {
  background-color: #0d1117;
  border-bottom: 1px solid #30363d;
  padding: 8px 16px; /* 减小内边距 */
}

.toolbar-scroll {
  display: flex;
  align-items: center;
  gap: 16px; /* 减小间距 */
  overflow-x: auto;
  white-space: nowrap;
  padding-bottom: 4px; /* 为滚动条预留一点空间 */
}

/* 隐藏滚动条但保留功能 */
.toolbar-scroll::-webkit-scrollbar {
  height: 4px;
}
.toolbar-scroll::-webkit-scrollbar-thumb {
  background-color: #30363d;
  border-radius: 4px;
}

.setting-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.setting-icon {
  color: #8b949e;
  font-size: 16px;
}

.setting-item label {
  color: #8b949e;
  font-size: 13px; /* 字体改小 */
  font-weight: 500;
}

.divider {
  width: 1px;
  height: 20px;
  background-color: #30363d;
  margin: 0 4px;
}

.setting-select {
  width: 140px; /* 稍微窄一点 */
}

.setting-input {
  width: 160px;
}

.burn-switch-wrapper {
  margin-left: auto; /* 靠右 */
  display: flex;
  align-items: center;
}

/* Element Plus Overrides for Toolbar */
:deep(.el-input__wrapper),
:deep(.el-select__wrapper) { /* Element Plus 2.5+ uses select__wrapper */
  background-color: #0d1117 !important;
  box-shadow: 0 0 0 1px #30363d inset !important;
  transition: box-shadow 0.2s;
}

:deep(.el-input__wrapper.is-focus),
:deep(.el-select__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #58a6ff inset !important;
}

:deep(.el-input__inner) {
  color: #c9d1d9 !important;
  font-size: 13px;
}

/* Editor Area */
.editor-wrapper {
  flex: 1;
  padding: 0; /* 移除 padding，让编辑器全屏 */
  overflow: hidden;
  position: relative;
}

.editor-container {
  height: 100%;
  width: 100%;
}

/* Share Dialog - 全新设计 */
:deep(.share-dialog .el-dialog) {
  background: linear-gradient(135deg, #1a1f2e 0%, #161b22 100%);
  border: 1px solid #30363d;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

:deep(.share-dialog .el-dialog__header) {
  padding: 0;
  margin: 0;
  border: none;
}

:deep(.share-dialog .el-dialog__headerbtn) {
  top: 16px;
  right: 16px;
  width: 32px;
  height: 32px;
  font-size: 18px;
}

:deep(.share-dialog .el-dialog__headerbtn .el-dialog__close) {
  color: #8b949e;
}

:deep(.share-dialog .el-dialog__headerbtn:hover .el-dialog__close) {
  color: #c9d1d9;
}

:deep(.share-dialog .el-dialog__body) {
  padding: 0 28px 24px;
  color: #c9d1d9;
}

:deep(.share-dialog .el-dialog__footer) {
  padding: 20px 28px 24px;
  border-top: 1px solid rgba(48, 54, 61, 0.6);
  background: rgba(0, 0, 0, 0.15);
}

/* Dialog Header */
.share-dialog-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 28px 28px 20px;
}

.success-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #238636 0%, #2ea043 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 8px 16px rgba(35, 134, 54, 0.3);
}

.header-text h3 {
  margin: 0 0 4px;
  font-size: 20px;
  font-weight: 600;
  color: #f0f6fc;
}

.header-text p {
  margin: 0;
  font-size: 14px;
  color: #8b949e;
}

/* Link Section */
.link-section {
  margin-bottom: 20px;
}

.section-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 500;
  color: #8b949e;
  margin-bottom: 10px;
}

.link-copy-box {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.link-text {
  flex: 1;
  background: #0d1117;
  border: 1px solid #30363d;
  border-radius: 10px;
  padding: 14px 16px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  font-size: 14px;
  color: #58a6ff;
  word-break: break-all;
  line-height: 1.5;
  transition: all 0.2s;
}

.link-text:hover {
  border-color: #58a6ff;
  background: rgba(88, 166, 255, 0.05);
}

.copy-btn {
  padding: 14px 20px !important;
  border-radius: 10px !important;
  font-weight: 500 !important;
  background: linear-gradient(135deg, #238636 0%, #2ea043 100%) !important;
  border: none !important;
  white-space: nowrap;
  transition: all 0.2s !important;
}

.copy-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(35, 134, 54, 0.4) !important;
}

/* Info Cards */
.info-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 12px;
  margin-bottom: 16px;
}

.info-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.06);
  transition: all 0.2s;
}

.info-card:hover {
  background: rgba(255, 255, 255, 0.05);
  border-color: rgba(255, 255, 255, 0.1);
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.password-card .card-icon {
  background: linear-gradient(135deg, rgba(187, 128, 9, 0.2) 0%, rgba(187, 128, 9, 0.1) 100%);
  color: #f0b429;
}

.expire-card .card-icon {
  background: linear-gradient(135deg, rgba(88, 166, 255, 0.2) 0%, rgba(88, 166, 255, 0.1) 100%);
  color: #58a6ff;
}

.burn-card .card-icon {
  background: linear-gradient(135deg, rgba(248, 81, 73, 0.2) 0%, rgba(248, 81, 73, 0.1) 100%);
  font-size: 20px;
}

.card-content {
  flex: 1;
  min-width: 0;
}

.card-label {
  font-size: 12px;
  color: #8b949e;
  margin-bottom: 4px;
  font-weight: 500;
}

.card-value {
  font-size: 15px;
  font-weight: 600;
  color: #f0f6fc;
}

.password-value {
  display: flex;
  align-items: center;
  gap: 8px;
}

.password-text {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Roboto Mono', monospace;
  background: rgba(240, 180, 41, 0.15);
  padding: 4px 10px;
  border-radius: 6px;
  color: #f0b429;
  letter-spacing: 1px;
}

.copy-password-btn {
  color: #8b949e !important;
  padding: 4px !important;
}

.copy-password-btn:hover {
  color: #f0b429 !important;
}

/* Security Tip */
.security-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: rgba(88, 166, 255, 0.08);
  border: 1px solid rgba(88, 166, 255, 0.15);
  border-radius: 10px;
  font-size: 13px;
  color: #79c0ff;
}

.security-tip .el-icon {
  font-size: 16px;
}

/* Dialog Footer */
.dialog-footer {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
}

.footer-btn {
  padding: 12px 24px !important;
  border-radius: 10px !important;
  font-weight: 500 !important;
  font-size: 14px !important;
  display: flex !important;
  align-items: center !important;
  gap: 8px !important;
  transition: all 0.2s !important;
}

.footer-btn:not(.primary-btn) {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid #30363d !important;
  color: #c9d1d9 !important;
}

.footer-btn:not(.primary-btn):hover {
  background: rgba(255, 255, 255, 0.1) !important;
  border-color: #8b949e !important;
}

.primary-btn {
  background: linear-gradient(135deg, #1f6feb 0%, #388bfd 100%) !important;
  border: none !important;
}

.primary-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(56, 139, 253, 0.4) !important;
}
</style>
