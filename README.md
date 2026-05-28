# 校园无人车配送系统

基于Vue 3 + Spring Boot的校园无人车配送路径规划系统，集成遗传算法实现多车辆路径优化。

## 项目简介

本系统旨在解决校园内快递配送的路径规划问题，通过遗传算法优化配送路线，提高配送效率。

## 技术栈

### 前端
- **框架**: Vue 3 + Vite
- **UI组件**: Element Plus
- **状态管理**: Pinia
- **地图服务**: 百度地图WebGL API
- **语言**: JavaScript

### 后端
- **框架**: Spring Boot 3.2.x
- **数据库**: SQLite
- **ORM**: Spring Data JPA
- **算法**: 遗传算法 + 2-opt局部优化
- **语言**: Java 21

## 功能特性

### 用户端
- 用户注册与登录
- 预约配送下单
- 订单查询与跟踪

### 管理员端
- 管理员登录
- 订单管理（查看、分配车辆）
- 车辆管理（添加、状态监控）
- 路径规划（可视化展示）

### 路径规划算法
- 基于遗传算法的多车辆路径优化
- 支持车辆容量约束
- 2-opt局部搜索优化
- 路径可视化展示

## 快速开始

### 环境要求
- JDK 21+
- Node.js 18+
- Maven

### 后端启动

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

服务启动后访问: http://localhost:8080

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端启动后访问: http://localhost:5173

### 配置说明

在 `frontend/.env.local` 中配置百度地图AK：

```env
VITE_BAIDU_MAP_AK_BROWSER=your_browser_ak
VITE_BAIDU_MAP_AK_SERVER=your_server_ak
```

## 项目结构

```
campus_delivery/
├── backend/                    # Spring Boot后端
│   ├── src/main/java/         # Java源代码
│   │   ├── controller/        # REST控制器
│   │   ├── service/           # 业务服务层
│   │   ├── repository/        # 数据访问层
│   │   ├── entity/            # JPA实体
│   │   ├── dto/               # 数据传输对象
│   │   └── algorithm/         # 遗传算法实现
│   ├── db/                    # 数据库初始化脚本
│   └── pom.xml                # Maven配置
├── frontend/                  # Vue前端
│   ├── .env.local             #配置百度地图AK
│   ├── src/
│   │   ├── views/             # 页面组件
│   │   ├── stores/            # Pinia状态管理
│   │   ├── router/            # Vue Router配置
│   │   └── api/               # API调用封装
│   └── package.json           # npm配置
└── README.md                  # 项目说明
```

## 核心算法

### 编码方式
采用基于路径的整数编码，染色体长度为N（客户点数量），每个基因对应配送点ID。

### 遗传算子
- **选择算子**: 锦标赛选择（锦标赛大小=5）
- **交叉算子**: 部分映射交叉（交叉率=0.9）
- **变异算子**: 交换变异（变异率=0.15）
- **局部优化**: 2-opt局部搜索（每10代执行）

### 参数设置
- 种群大小: 150
- 迭代次数: 300
- 精英率: 10%
- 车辆容量: 15kg

## 数据库设计

### 核心表
- **users**: 用户信息
- **admins**: 管理员信息
- **orders**: 订单信息
- **vehicles**: 车辆信息
- **buildings**: 配送点信息（含坐标）
- **delivery_paths**: 路径规划结果

## 开发说明

### 前端页面
- `LoginView.vue`: 用户登录
- `AdminLogin.vue`: 管理员登录
- `OrderSubmit.vue`: 下单页面
- `OrderQuery.vue`: 订单查询
- `OrderManage.vue`: 订单管理
- `VehicleManage.vue`: 车辆管理
- `PathPlanning.vue`: 路径规划（含地图可视化）

### 后端接口
- `/api/auth/user/login`: 用户登录
- `/api/auth/admin/login`: 管理员登录
- `/api/orders`: 订单CRUD
- `/api/vehicles`: 车辆CRUD
- `/api/path/calculate`: 路径规划计算

## 许可证

MIT License

## 作者

Yvonne

---

*校园无人车配送系统 - 基于遗传算法的智能路径规划解决方案*
