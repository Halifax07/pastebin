/**
 * 应用配置
 */

// 获取分享链接的基础 URL
// 优先使用环境变量配置，否则使用当前页面的 origin (自动适配局域网或公网)
export const getShareBaseUrl = (): string => {
  // 1. 如果环境变量配置了非空值，优先使用 (通常用于生产环境固定域名)
  const customUrl = import.meta.env.VITE_SHARE_BASE_URL
  if (customUrl && customUrl.trim() !== '') {
    return customUrl
  }
  
  // 2. 默认使用当前浏览器的访问地址 (window.location.origin)
  // 这样无论是 localhost, 192.168.x.x 还是 .loca.lt 公网地址，分享出去的链接都和当前访问的一致
  return window.location.origin
}

/**
 * 生成分享链接
 * @param key Paste 的唯一标识
 * @returns 完整的分享链接
 */
export const generateShareUrl = (key: string): string => {
  const baseUrl = getShareBaseUrl()
  return `${baseUrl}/${key}`
}
