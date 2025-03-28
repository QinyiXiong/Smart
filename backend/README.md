# SmartMockInterview 后端项目

## 项目配置

### 技术栈
- Java 8+
- Spring Boot
- MyBatis
- Shiro (认证与授权)
- Redis (Token管理)
- Lucene (全文搜索)

### 依赖管理
- Maven (pom.xml)

### 数据库
- MySQL

## 项目结构

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/sdumagicode/backend/
│   │   │   ├── answer/ - 答案相关控制器
│   │   │   ├── auth/ - 认证授权模块
│   │   │   ├── config/ - 配置类
│   │   │   ├── controller/ - 控制器
│   │   │   ├── core/ - 核心模块
│   │   │   ├── dto/ - 数据传输对象
│   │   │   ├── entity/ - 实体类
│   │   │   ├── enumerate/ - 枚举
│   │   │   ├── handler/ - 处理器
│   │   │   ├── lucene/ - 全文搜索
│   │   │   ├── mapper/ - MyBatis映射
│   │   │   ├── openai/ - OpenAI集成
│   │   │   ├── service/ - 服务层
│   │   │   └── util/ - 工具类
│   │   └── resources/ - 资源文件
│   └── test/ - 测试代码
├── docker/ - Docker相关配置
├── lucene/ - Lucene索引
└── pom.xml - Maven配置
```

## 实现功能

### 核心功能
1. 用户认证与授权 (JWT + Shiro)
2. 答案管理
3. 全文搜索 (Lucene)
4. OpenAI集成

### 其他功能
- Redis Token管理
- 异常统一处理
- MyBatis配置

## 新增功能记录

- [2023-XX-XX] 新增XXX功能