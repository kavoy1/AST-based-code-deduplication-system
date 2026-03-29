# AST 智能查重系统 API 文档

本文档基于当前项目代码整理，只描述**已经在代码中落地**的接口、行为与已知限制，不包含尚未实现的设想功能。

- 项目根目录：`D:\学校\毕业设计\项目\graduation_project`
- 后端目录：`D:\学校\毕业设计\项目\graduation_project\back`
- 前端目录：`D:\学校\毕业设计\项目\graduation_project\frontend`
- 统一前缀：`/api`

---

## 1. 通用约定

### 1.1 鉴权

- 当前后端使用 `Sa-Token`
- 登录成功后，前端通过请求头传递 `satoken: <token>`
- 未登录访问受保护接口时，由后端返回未登录状态，前端路由守卫会跳转到登录页

### 1.2 统一响应结构

绝大多数 JSON 接口返回：

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

说明：

- 前端请求层通常会直接解包 `data`
- 文件下载、CSV 导出等接口直接返回文件流，不使用 `Result` 包装

### 1.3 时间字段说明

- 当前项目时间字段以**后端实际序列化结果**为准
- 教师作业创建/更新/重开等写接口，前端当前按 `YYYY-MM-DDTHH:mm:ss` 发送
- 展示层通常会在前端格式化为本地时间

### 1.4 当前前后端接口现状

- 文档中既记录后端接口，也同步标注当前前端接入情况
- 个别接口已落地但返回结构还不够统一，前端会做适配
- 例如教师作业列表接口当前返回 `List`，而不是标准分页对象

---

## 2. 认证与个人信息

### 2.1 登录与注册

- `POST /api/login`
- `POST /api/code`
- `POST /api/verify-code`
- `POST /api/register`
- `POST /api/reset-password`

当前用途：

- 登录
- 获取验证码 / 验证验证码
- 注册账号
- 重置密码

### 2.2 当前登录用户

- `GET /api/user/info`
- `POST /api/user/password`

当前用途：

- 获取当前登录用户基本信息
- 修改当前用户密码

---

## 3. 教师端 API

### 3.1 教师端当前已落地范围

当前教师端后端能力已覆盖：

- 教师首页统计
- 班级管理与班级学生管理
- 学生检索辅助能力
- 作业创建、编辑、重开、列表、详情
- 作业资料附件管理
- 提交概览与作业统计
- AST 查重任务创建、报告查看、CSV 导出
- 相似对详情、状态更新、AI 解释

当前教师端前端已接入：

- `Home.vue`：首页概览
- `ClassManage.vue` / `ClassStudentList.vue`：班级与班内学生
- `AssignmentManage.vue`：作业总览、发布编辑、提交概览、查重视图
- `AssignmentDetail.vue`：作业详情页
- `SubmissionDetail.vue`：提交详情页
- `SimilarityPairDetail.vue`：相似对详情页

---

### 3.2 教师首页统计

#### 3.2.1 获取教师首页统计

- `GET /api/teacher/stats`

当前用于教师首页展示：

- 班级总数
- 学生总数
- 作业总数
- 重点班级 / 班级分布
- 作业截止统计（今天 / 3天内 / 7天内）
- 最近截止作业列表

说明：

- 如果运行环境中尚未建好 `assignment` 表，作业相关统计会自动降级为空数据，避免首页直接报错

---

### 3.3 班级管理

#### 3.3.1 获取教师班级列表

- `GET /api/teacher/classes?page=1&limit=10`

返回 `IPage<Clazz>`。

当前前端用途：

- 教师班级卡片列表
- 班级人数、邀请码、创建时间展示

#### 3.3.2 获取班级详情

- `GET /api/teacher/classes/{id}`

#### 3.3.3 创建班级

- `POST /api/teacher/classes/create`

请求体示例：

```json
{
  "className": "软件工程 2201",
  "description": "Java 工程实践班级"
}
```

#### 3.3.4 删除班级

- `DELETE /api/teacher/classes/{id}`

#### 3.3.5 获取班级学生列表

- `GET /api/teacher/classes/{classId}/students`

当前前端用途：

- 班级内学生列表
- 查看班级成员基本信息

#### 3.3.6 移除班级学生

- `DELETE /api/teacher/classes/{classId}/students/{studentId}`

#### 3.3.7 邀请学生入班

- `POST /api/teacher/classes/{classId}/students/invite?studentId=10001`

#### 3.3.8 获取待审核入班申请

- `GET /api/teacher/classes/applications`

#### 3.3.9 审核入班申请

- `POST /api/teacher/classes/applications/{id}/approve`
- `POST /api/teacher/classes/applications/{id}/reject`

说明：

- 班级人数统计已按 `studentCount` 字段映射修正，后端重启后可返回真实人数

---

### 3.4 教师侧学生检索辅助接口

这组接口虽然挂在 `/api/student` 下，但当前教师端“学生管理 / 邀请学生”页面会实际使用。

#### 3.4.1 学生列表检索

- `GET /api/student/list?keyword=&college=&className=`

#### 3.4.2 学院选项

- `GET /api/student/colleges`

#### 3.4.3 班级选项

- `GET /api/student/class-options?college=`

---

### 3.5 教师作业管理

#### 3.5.1 创建作业

- `POST /api/teacher/assignments`

请求体示例：

```json
{
  "title": "实验一：类与对象",
  "language": "JAVA",
  "classIds": [1, 2],
  "startAt": "2026-03-21T08:00:00",
  "endAt": "2026-03-28T23:59:59",
  "description": "请输入作业说明、提交要求和评分重点",
  "allowResubmit": true,
  "allowLateSubmit": false,
  "maxFiles": 20
}
```

说明：

- 当前模型为“作业主体 + 班级关联”
- 一份作业可发布到多个班级
- 教师前端已接入创建与编辑流程

#### 3.5.2 获取教师作业列表

- `GET /api/teacher/assignments?keyword=&status=&page=1&size=10`

当前返回 `List<TeacherAssignmentSummaryView>`，前端会自行兼容为列表视图。

当前字段包括：

- `id`
- `title`
- `language`
- `startAt`
- `endAt`
- `status`
- `classCount`
- `studentCount`
- `submittedStudentCount`
- `unsubmittedStudentCount`
- `lateSubmissionCount`
- `materialCount`

说明：

- 当前后端尚未输出标准分页对象
- 前端页面已经支持关键字、状态、班级等筛选展示

#### 3.5.3 获取作业详情

- `GET /api/teacher/assignments/{assignmentId}`

当前详情结构 `TeacherAssignmentDetailView` 包含：

- `id`
- `title`
- `language`
- `description`
- `startAt`
- `endAt`
- `status`
- `allowResubmit`
- `allowLateSubmit`
- `maxFiles`
- `classes`
- `materials`
- `stats`

#### 3.5.4 更新作业

- `PUT /api/teacher/assignments/{assignmentId}`

请求体与创建接口一致。

#### 3.5.5 重开作业

- `POST /api/teacher/assignments/{assignmentId}/reopen`

请求体示例：

```json
{
  "startAt": "2026-04-01T08:00:00",
  "endAt": "2026-04-03T23:59:59",
  "reason": "补交窗口"
}
```

说明：

- 已结束作业可重开
- 重开日志会记录到 `assignment_reopen_log`

---

### 3.6 作业资料附件

#### 3.6.1 上传资料附件

- `POST /api/teacher/assignments/{assignmentId}/materials`
- `Content-Type: multipart/form-data`
- 字段名：`files`

#### 3.6.2 删除资料附件

- `DELETE /api/teacher/assignments/{assignmentId}/materials/{materialId}`

#### 3.6.3 下载资料附件

- `GET /api/teacher/assignments/{assignmentId}/materials/{materialId}/download`

说明：

- 当前附件元数据入库，文件本体走本地存储
- 教师详情页已支持上传、删除、下载资料

---

### 3.7 提交概览与作业统计

#### 3.7.1 获取作业提交概览

- `GET /api/teacher/assignments/{assignmentId}/submissions`

当前返回 `List<TeacherSubmissionOverview>`，主要字段：

- `submissionId`
- `studentId`
- `version`
- `isLate`
- `isValid`
- `parseOkFiles`
- `totalFiles`

说明：

- 当前主要用于教师查看“谁提交了、版本情况、是否迟交、解析是否有效”
- 前端已接入概览页与详情跳转
- 当前接口尚未完整提供文件级明细

#### 3.7.2 获取作业统计

- `GET /api/teacher/assignments/{assignmentId}/stats`
- 可选参数：`classId`

返回 `TeacherAssignmentStatsView`，当前字段包括：

- `classCount`
- `studentCount`
- `submittedStudentCount`
- `unsubmittedStudentCount`
- `lateSubmissionCount`
- `latestSubmissionCount`

说明：

- 教师详情页已接入这组统计

---

### 3.8 查重任务与报告

#### 3.8.1 创建查重任务

- `POST /api/teacher/assignments/{assignmentId}/plagiarism-jobs`

请求体示例：

```json
{
  "thresholdScore": 80,
  "topKPerStudent": 10
}
```

当前规则：

- 仅比较 `is_latest = 1` 且 `is_valid = 1` 的提交
- 默认在同一班级内做两两比较
- 阈值判断使用原始相似度结果

#### 3.8.2 获取作业下的查重任务列表

- `GET /api/teacher/assignments/{assignmentId}/plagiarism-jobs`

返回 `List<TeacherPlagiarismJobSummaryView>`，字段包括：

- `jobId`
- `status`
- `progressTotal`
- `progressDone`
- `createTime`
- `thresholdScore`
- `topKPerStudent`
- `executionMode`
- `reusedFromJobId`
- `thresholdMatchedPairs`

#### 3.8.3 获取任务状态

- `GET /api/teacher/plagiarism-jobs/{jobId}`

#### 3.8.4 获取查重报告

- `GET /api/teacher/plagiarism-jobs/{jobId}/report?minScore=80&perStudentTopK=10`

当前返回 `TeacherPlagiarismJobReport`，包括：

- `jobId`
- `status`
- `message`
- `minScore`
- `perStudentTopK`
- `jobStats`
- `pairs`
- `incomparableSubmissions`

#### 3.8.5 导出查重报告

- `GET /api/teacher/plagiarism-jobs/{jobId}/export?minScore=80&perStudentTopK=10`

当前导出格式：CSV。

说明：

- 教师前端已接入任务列表、报告筛选、导出

---

### 3.9 相似对与 AI 解释

#### 3.9.1 获取相似对详情

- `GET /api/teacher/similarity-pairs/{pairId}`

当前返回 `TeacherPlagiarismPairDetailView`，包括：

- `pairId`
- `jobId`
- `studentA`
- `studentB`
- `score`
- `status`
- `teacherNote`
- `evidences`
- `latestAiExplanation`

#### 3.9.2 更新相似对状态

- `POST /api/teacher/similarity-pairs/{pairId}/status`

请求体示例：

```json
{
  "status": "CONFIRMED",
  "teacherNote": "需要课堂复核"
}
```

当前支持状态：

- `PENDING`
- `CONFIRMED`
- `FALSE_POSITIVE`

#### 3.9.3 手动触发 AI 解释

- `POST /api/teacher/similarity-pairs/{pairId}/ai-explanations`

#### 3.9.4 获取最新 AI 解释

- `GET /api/teacher/similarity-pairs/{pairId}/ai-explanations/latest`

说明：

- 当前为教师手动触发，不自动批量生成
- AI 解释只作为说明增强，不参与最终判定
- 当前已接入 Provider 抽象，并支持配置模型相关参数

---

## 4. 学生端 API

### 4.1 学生端当前已落地范围

当前学生端后端能力已覆盖：

- 我的班级
- 通过邀请码加入班级
- 按班级查看作业列表
- 作业详情
- 文件上传提交
- 文本代码提交
- 提交历史
- 个人查重摘要

当前学生端前端已接入：

- `MyClass.vue`
- `TaskList.vue`
- 学生端 `AssignmentDetail.vue`

---

### 4.2 我的班级

#### 4.2.1 获取我的班级

- `GET /api/student/classes`

当前前端展示：

- 已加入班级列表
- 班级审核状态
- 班级入口跳转

#### 4.2.2 通过邀请码申请加入班级

- `POST /api/student/classes/join?inviteCode=ABC123`

---

### 4.3 学生作业

#### 4.3.1 获取班级作业列表

- `GET /api/student/classes/{classId}/assignments`

当前前端支持：

- 按班级切换
- 按状态过滤（全部 / 进行中 / 未开始 / 已结束）
- 按标题关键字搜索

#### 4.3.2 获取作业详情

- `GET /api/student/assignments/{assignmentId}`

当前前端展示：

- 作业基础信息
- 时间窗
- 作业说明
- 个人查重摘要
- 提交入口与提交历史

---

### 4.4 学生提交

#### 4.4.1 文件上传提交

- `POST /api/student/assignments/{assignmentId}/submissions`
- `Content-Type: multipart/form-data`
- 字段名：`files`

当前规则：

- 支持上传一个或多个 `.java` 文件
- 是否允许提交由作业时间窗决定
- 迟交作业会写入 `is_late`
- 是否允许重复提交由作业配置控制

#### 4.4.2 文本代码提交

- `POST /api/student/assignments/{assignmentId}/submissions/text`

请求体示例：

```json
{
  "entries": [
    {
      "filename": "Main.java",
      "content": "public class Main { public static void main(String[] args) {} }"
    }
  ]
}
```

说明：

- 文本代码会转换为统一的提交流程
- 与文件上传共用解析、版本与查重入库逻辑

#### 4.4.3 获取提交历史

- `GET /api/student/assignments/{assignmentId}/submissions`

当前返回 `List<Submission>`。

---

### 4.5 学生查重摘要

#### 4.5.1 获取个人查重摘要

- `GET /api/student/assignments/{assignmentId}/plagiarism-summary`

当前返回 `StudentPlagiarismSummary`，主要字段包括：

- `generated`
- `message`
- `highestScore`
- `status`
- `teacherNote`

说明：

- 只有教师对该作业跑过查重后，学生才能看到摘要
- 学生端不会返回其他人的身份或详细证据

---

## 5. 管理员端 API

### 5.1 管理员端当前已落地范围

当前管理员端后端能力已覆盖：

- 管理员首页统计
- 用户管理
- 系统公告管理
- 系统配置与 AI 配置
- 反馈管理

当前管理员端前端已接入：

- `AdminCommandCenter.vue`
- `UserManage.vue`
- `NoticeManage.vue`
- `FeedbackManage.vue`
- `SystemSettings.vue`

---

### 5.2 管理员首页统计

#### 5.2.1 获取管理员首页统计

- `GET /api/admin/stats`

当前返回字段以当前实现为准，常见字段包括：

- `userCount`
- `noticeCount`
- `studentCount`
- `teacherCount`
- `adminCount`
- `status`
- `resource`
- `weeklyVisits`

---

### 5.3 用户管理

#### 5.3.1 获取用户列表

- `GET /api/admin/users?page=1&size=10&keyword=&role=&status=&sortBy=&sortOrder=`

返回 `IPage<User>`。

#### 5.3.2 更新用户状态

- `PUT /api/admin/users/status?id=1&status=1`

#### 5.3.3 删除用户

- `DELETE /api/admin/users/{id}`

说明：

- 前端已接入筛选、排序、分页、状态切换、删除与右侧用户上下文面板

---

### 5.4 系统公告管理

#### 5.4.1 发布公告

- `POST /api/admin/notices`

#### 5.4.2 获取公告列表

- `GET /api/admin/notices`

#### 5.4.3 删除公告

- `DELETE /api/admin/notices/{id}`

说明：

- 前端已接入公告发布、列表、删除、内容预览

---

### 5.5 系统配置

#### 5.5.1 获取系统配置

- `GET /api/admin/config`

#### 5.5.2 批量更新普通配置

- `PUT /api/admin/config`

#### 5.5.3 更新密钥类配置

- `PUT /api/admin/config/secret`

当前已接入的 AI 相关配置项包括：

- `ai.provider`
- `ai.base_url`
- `ai.model`
- `ai.timeout_ms`
- `ai.prompt_version`
- `ai.api_key`

说明：

- 密钥支持清空 / 替换
- 如启用加密存储，运行环境仍需提供 `APP_MASTER_KEY`

---

### 5.6 反馈管理

#### 5.6.1 提交反馈

- `POST /api/feedback/submit`

#### 5.6.2 获取我的反馈

- `GET /api/feedback/my?userId=1`

#### 5.6.3 获取反馈列表

- `GET /api/feedback/list?page=1&size=10&status=&keyword=`

#### 5.6.4 标记反馈已解决

- `POST /api/feedback/resolve/{id}`

#### 5.6.5 回复反馈

- `POST /api/feedback/reply/{id}`

#### 5.6.6 更新反馈状态

- `POST /api/feedback/status/{id}?status=RESOLVED`

#### 5.6.7 获取今日新增反馈数

- `GET /api/feedback/stats/today`

说明：

- 前端已接入列表、详情面板、状态更新、回复、今日统计

---

## 6. 通知 API

### 6.1 获取通知列表

- `GET /api/notice/list`

### 6.2 标记通知已读

- `POST /api/notice/read/{id}`

当前前端用途：

- 全局通知中心
- 已读 / 未读筛选与状态更新

---

## 7. 当前已知限制

### 7.1 教师端

- 提交文件明细接口尚未补齐，`SubmissionDetail` 页面对文件级细节仍是占位增强态
- 教师作业列表接口当前仍是 `List`，尚未做标准分页封装
- 首页作业统计依赖 `assignment` 相关表；数据库未建表时会降级为空数据

### 7.2 学生端

- 当前主要面向 Java 作业提交
- 暂不支持压缩包上传
- 不开放相似对证据级详情

### 7.3 管理端

- 更细粒度的权限控制与操作审计仍待补强
- 系统监控仍以摘要信息为主，未形成完整运维面板

