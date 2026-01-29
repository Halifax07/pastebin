# Secure Pastebin

一个基于 Spring Boot 3 + Vue 3 的安全在线剪贴板应用，支持客户端加密、阅后即焚等功能。

## 技术栈

### 后端
- **Java 17**
- **Spring Boot 3.2.1**
- **Spring Data JPA**
- **H2 Database** (嵌入式文件模式)
- **Lombok**
- **Maven**

### 前端
- **Vue 3**
- **TypeScript**
- **Vite**
- **Element Plus**
- **Vue Router**
- **Pinia**
- **Axios**
- **CryptoJS**

## 项目结构

```
pastebin/
├── src/                          # 后端源码
│   └── main/
│       ├── java/
│       │   └── com/pastebin/
│       │       └── SecurePastebinApplication.java
│       └── resources/
│           └── application.yml
├── frontend/                     # 前端项目
│   ├── src/
│   │   ├── api/                 # API 请求
│   │   ├── views/               # 页面组件
│   │   ├── router/              # 路由配置
│   │   ├── App.vue
│   │   └── main.ts
│   ├── index.html
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── package.json
├── pom.xml                       # Maven 配置
└── README.md

```

## 快速启动

### 前置要求
- JDK 17+
- Node.js 18+
- Maven 3.8+

### 1. 启动后端

在项目根目录执行：

```bash
# 使用 Maven 构建并运行
mvn spring-boot:run
```

后端将在 `http://localhost:8080` 启动，API 前缀为 `/api`

**H2 控制台**：访问 `http://localhost:8080/api/h2-console`
- JDBC URL: `jdbc:h2:file:./data/pastebin`
- 用户名: `sa`
- 密码: (留空)

### 2. 启动前端

在 `frontend` 目录执行：

```bash
# 安装依赖
npm install

# 启动开发服务器
npm run dev
```

前端将在 `http://localhost:3000` 启动

### 3. 访问应用

打开浏览器访问 `http://localhost:3000`

## 开发说明

### API 跨域处理
前端 Vite 已配置代理，所有 `/api` 请求会自动转发到后端 `http://localhost:8080`

### 局域网访问配置（让其他电脑也能访问）

1. **查看本机 IP 地址**：
   ```bash
   # Windows
   ipconfig
   
   # Linux/Mac
   ifconfig 或 ip addr
   ```
   找到类似 `192.168.x.x` 或 `10.x.x.x` 的 IPv4 地址

2. **配置分享链接**：
   编辑 `frontend/.env` 文件：
   ```
   VITE_SHARE_BASE_URL=http://你的IP地址:3000
   ```

3. **重启前端服务**（修改 .env 后需要重启）

4. **防火墙设置**：
   确保防火墙允许 3000 和 8080 端口的入站连接

5. **访问方式**：
   - 其他电脑访问：`http://你的IP地址:3000`
   - 生成的分享链接会自动使用配置的 IP 地址

### 数据库
H2 数据库采用文件模式，数据保存在项目根目录的 `./data/pastebin.mv.db` 文件中

### 构建生产版本

**后端：**
```bash
mvn clean package
java -jar target/secure-pastebin-1.0.0.jar
```

**前端：**
```bash
cd frontend
npm run build
# 构建产物在 frontend/dist 目录
```

## 核心功能（待开发）

- ✅ 项目初始化
- ⬜ 创建 Paste（支持客户端加密）
- ⬜ 查看 Paste（支持密码解密）
- ⬜ 阅后即焚功能
- ⬜ Monaco Editor 代码高亮
- ⬜ 短链接生成

## License

MIT