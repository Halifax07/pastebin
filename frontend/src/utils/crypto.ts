import CryptoJS from 'crypto-js'

/**
 * 加密标识前缀
 * 用于识别内容是否已加密
 */
const ENCRYPTED_PREFIX = 'ENC:'

/**
 * 使用 AES 加密文本
 * @param plainText 明文
 * @param password 密码
 * @returns 加密后的密文（带标识前缀）
 */
export function encrypt(plainText: string, password: string): string {
  if (!password || !plainText) {
    throw new Error('密码和内容不能为空')
  }
  
  try {
    const encrypted = CryptoJS.AES.encrypt(plainText, password).toString()
    return ENCRYPTED_PREFIX + encrypted
  } catch (error) {
    throw new Error('加密失败')
  }
}

/**
 * 使用 AES 解密文本
 * @param cipherText 密文
 * @param password 密码
 * @returns 解密后的明文
 */
export function decrypt(cipherText: string, password: string): string {
  if (!password || !cipherText) {
    throw new Error('密码和内容不能为空')
  }
  
  try {
    // 移除标识前缀
    const actualCipherText = cipherText.startsWith(ENCRYPTED_PREFIX) 
      ? cipherText.substring(ENCRYPTED_PREFIX.length) 
      : cipherText
    
    const decrypted = CryptoJS.AES.decrypt(actualCipherText, password)
    const plainText = decrypted.toString(CryptoJS.enc.Utf8)
    
    if (!plainText) {
      throw new Error('密码错误或数据已损坏')
    }
    
    return plainText
  } catch (error) {
    throw new Error('解密失败：密码错误或数据已损坏')
  }
}

/**
 * 检查内容是否已加密
 * @param content 内容
 * @returns 是否加密
 */
export function isEncrypted(content: string): boolean {
  return content.startsWith(ENCRYPTED_PREFIX)
}

/**
 * 生成随机短码
 * @param length 长度（默认 8）
 * @returns 随机短码
 */
export function generateKey(length: number = 8): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}
