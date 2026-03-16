# AST 智能查重系统 API 接口文档

本文档基于当前后端代码（Spring Boot + Sa-Token + MyBatis-Plus）整理，接口返回统一使用 `Result<T>` 包装。

## 1. 基本约定

### 1.1 Base URL

- 开发环境（前端 Vite 代理）：`/api`
- 后端实际路径前缀：`/api`

示例：前端请求 `POST /api/login` 会被代理到 `http://localhost:8080/api/login`。

### 1.2 鉴权方式（Sa-Token）

- 登录成功后，后端会创建 Sa-Token 登录态，并在登录响应 `data.token` 中返回 token。
- 前端可将 token 写入请求头：`satoken: <token>`（项目当前前端实现会从 `localStorage.satoken` 读取并自动携带）。
- 浏览器同源情况下也可能通过 Cookie 自动携带（取决于 Sa-Token 默认策略/配置）。

### 1.3 统一响应结构

```json
{
  "code": 200,
  "msg": "操作成功",
  "data": {}
}
```

- `code = 200`：成功
- `code = 401`：未登录或登录过期
- `code = 403`：无权操作
- `code = 500`：业务/系统错误

## 2. 认证与账号

### 2.1 登录

- 方法：`POST`
- 路径：`/api/login`
- 鉴权：否
- Body（JSON）：

```json
{
  "username": "string",
  "password": "string"
}
```

- 成功响应 `data`：

```json
{
  "user": {
    "id": 1,
    "username": "u",
    "nickname": "张三",
    "role": "STUDENT",
    "email": "a@b.com",
    "status": 1,
    "uid": "1234567890",
    "createTime": "2026-03-15T12:00:00",
    "updateTime": "2026-03-15T12:00:00"
  },
  "notice": {
    "id": 1,
    "title": "公告标题",
    "content": "公告内容",
    "authorId": 1,
    "receiverId": null,
    "isRead": 0,
    "type": "SYSTEM",
    "createTime": "2026-03-15T12:00:00"
  },
  "token": "string"
}
```

### 2.2 发送邮箱验证码

- 方法：`POST`
- 路径：`/api/code`
- 鉴权：否
- Query：
  - `email`：邮箱地址
- 成功响应：`data` 为提示字符串

### 2.3 校验邮箱验证码（注册第一步）

- 方法：`POST`
- 路径：`/api/verify-code`
- 鉴权：否
- Query：
  - `email`
  - `code`
- 成功响应：`data = "验证通过"`

### 2.4 注册

- 方法：`POST`
- 路径：`/api/register`
- 鉴权：否
- Body（JSON，字段与后端 `RegisterDTO` 对齐）：

```json
{
  "user": {
    "username": "string",
    "password": "string",
    "nickname": "string",
    "email": "string",
    "role": "STUDENT | TEACHER"
  },
  "inviteCode": "string",
  "emailCode": "string",
  "studentInfo": {
    "studentNumber": "string",
    "school": "string",
    "college": "string",
    "major": "string",
    "className": "string"
  },
  "teacherInfo": {
    "teacherNumber": "string",
    "school": "string",
    "college": "string"
  }
}
```

- 备注：
  - 教师注册会校验 `inviteCode`（当前逻辑要求为 `TEA888`）。
  - 注册会校验邮箱验证码（通过 Session 保存的验证码信息）。

### 2.5 重置密码

- 方法：`POST`
- 路径：`/api/reset-password`
- 鉴权：否
- Body（JSON）：

```json
{
  "email": "string",
  "code": "string",
  "newPassword": "string"
}
```

## 3. 用户

### 3.1 获取当前登录用户信息

- 方法：`GET`
- 路径：`/api/user/info`
- 鉴权：是
- 成功响应 `data`：`User`（密码字段不返回）

### 3.2 修改密码

- 方法：`POST`
- 路径：`/api/user/password`
- 鉴权：是
- Body（JSON）：

```json
{
  "oldPassword": "string",
  "newPassword": "string"
}
```

## 4. 通知

### 4.1 获取通知列表（全局公告 + 私人通知）

- 方法：`GET`
- 路径：`/api/notice/list`
- 鉴权：是
- 成功响应 `data`：`Notice[]`

### 4.2 标记私人通知为已读

- 方法：`POST`
- 路径：`/api/notice/read/{id}`
- 鉴权：是
- 路径参数：
  - `id`：通知 ID
- 备注：
  - 仅允许标记 `receiverId = 当前用户ID` 的通知。
  - 全局公告（`receiverId = null`）不建议用该接口改动 `isRead`（会影响所有用户），前端当前采用本地已读记录方案。

## 5. 反馈

### 5.1 提交反馈

- 方法：`POST`
- 路径：`/api/feedback/submit`
- 鉴权：是
- Body（JSON）：

```json
{
  "userId": 1,
  "title": "string",
  "content": "string(>=10)"
}
```

- 备注：
  - `userId` 若传入，后端会校验必须等于当前登录用户。

### 5.2 获取我的反馈列表

- 方法：`GET`
- 路径：`/api/feedback/my`
- 鉴权：是
- Query（可选）：
  - `userId`：若传入，后端会校验必须等于当前登录用户
- 成功响应 `data`：`Feedback[]`

### 5.3 管理员获取反馈列表（分页）

- 方法：`GET`
- 路径：`/api/feedback/list`
- 鉴权：是（当前代码未做管理员角色强校验）
- Query：
  - `page`：默认 1
  - `size`：默认 10
  - `status`：可选（如 `PENDING` / `PROCESSING` / `RESOLVED`）
  - `keyword`：可选（模糊匹配标题/内容）
- 成功响应 `data`：MyBatis-Plus `Page<Feedback>`，并填充以下展示字段：
  - `submitterName`
  - `submitterUid`
  - `submitterRole`

### 5.4 回复反馈

- 方法：`POST`
- 路径：`/api/feedback/reply/{id}`
- 鉴权：是
- Body（JSON）：

```json
{
  "reply": "string"
}
```

### 5.5 标记反馈为已解决

- 方法：`POST`
- 路径：`/api/feedback/resolve/{id}`
- 鉴权：是

### 5.6 更新反馈状态

- 方法：`POST`
- 路径：`/api/feedback/status/{id}`
- 鉴权：是
- Query：
  - `status`：状态字符串

### 5.7 今日新增反馈数量

- 方法：`GET`
- 路径：`/api/feedback/stats/today`
- 鉴权：是
- 成功响应 `data`：`number`

## 6. 班级与学生（教师端）

### 6.1 教师班级列表（分页）

- 方法：`GET`
- 路径：`/api/teacher/classes`
- 鉴权：是（教师登录态）
- Query：
  - `page`：默认 1
  - `limit`：默认 10
- 成功响应 `data`：MyBatis-Plus `IPage<Clazz>`（常见字段：`records`、`total`、`size`、`current`、`pages` 等）

### 6.2 新建班级

- 方法：`POST`
- 路径：`/api/teacher/classes/create`
- 鉴权：是
- Body（JSON）：

```json
{
  "className": "string",
  "courseName": "string"
}
```

- 成功响应 `data`：创建后的 `Clazz`（会自动生成 6 位 `inviteCode`）

### 6.3 获取班级详情

- 方法：`GET`
- 路径：`/api/teacher/classes/{id}`
- 鉴权：是

### 6.4 删除班级

- 方法：`DELETE`
- 路径：`/api/teacher/classes/{id}`
- 鉴权：是

### 6.5 获取班级学生列表

- 方法：`GET`
- 路径：`/api/teacher/classes/{classId}/students`
- 鉴权：是
- 成功响应 `data`：`Map[]`（包含基础用户信息与学生信息字段，如 `student_number`、`college`、`admin_class` 等）

### 6.6 移除班级学生

- 方法：`DELETE`
- 路径：`/api/teacher/classes/{classId}/students/{studentId}`
- 鉴权：是

### 6.7 邀请学生直接加入班级

- 方法：`POST`
- 路径：`/api/teacher/classes/{classId}/students/invite`
- 鉴权：是
- Query：
  - `studentId`

### 6.8 入班申请列表（待审核）

- 方法：`GET`
- 路径：`/api/teacher/classes/applications`
- 鉴权：是
- 成功响应 `data`：`Map[]`（包含申请记录 id、学生信息、班级名、申请时间等）

### 6.9 通过入班申请

- 方法：`POST`
- 路径：`/api/teacher/classes/applications/{id}/approve`
- 鉴权：是
- 备注：通过后会创建一条私人通知（`Notice.receiverId = 学生ID`）。

### 6.10 拒绝入班申请

- 方法：`POST`
- 路径：`/api/teacher/classes/applications/{id}/reject`
- 鉴权：是
- 备注：拒绝后会创建一条私人通知（`Notice.receiverId = 学生ID`）。

## 7. 学生端

### 7.1 获取学生已加入/审核中的班级列表

- 方法：`GET`
- 路径：`/api/student/classes`
- 鉴权：是
- 成功响应 `data`：`Map[]`（包含 `teacherName`、`joinTime`、`status` 等）

### 7.2 申请加入班级

- 方法：`POST`
- 路径：`/api/student/classes/join`
- 鉴权：是
- Query：
  - `inviteCode`：6 位邀请码
- 成功响应：`data` 为提示字符串（成功/失败）

### 7.3 学生信息筛选列表（教师端页面复用）

- 方法：`GET`
- 路径：`/api/student/list`
- 鉴权：未强制（当前代码未校验角色）
- Query（可选）：
  - `keyword`
  - `college`
  - `className`
- 成功响应 `data`：`Map[]`

### 7.4 学院选项

- 方法：`GET`
- 路径：`/api/student/colleges`
- 鉴权：未强制
- 成功响应 `data`：`string[]`

### 7.5 班级选项（按学院过滤）

- 方法：`GET`
- 路径：`/api/student/class-options`
- 鉴权：未强制
- Query（可选）：
  - `college`
- 成功响应 `data`：`string[]`

## 8. 教师统计

### 8.1 教师端首页统计

- 方法：`GET`
- 路径：`/api/teacher/stats`
- 鉴权：是
- 成功响应 `data`（示例字段）：
  - `classCount`
  - `studentCount`
  - `homeworkCount`（当前实现固定为 0）
  - `warningCount`（当前实现固定为 0）
  - `chartNames` / `chartValues`

## 9. 管理员接口

> 注意：当前后端未对管理员接口做强制角色校验（Spring Security 配置中也放行了所有请求），实际部署建议补充权限控制。

### 9.1 用户管理

- `GET /api/admin/users`：获取所有用户
- `PUT /api/admin/users/status?id=1&status=0|1`：启用/禁用
- `DELETE /api/admin/users/{id}`：删除用户

### 9.2 通知管理

- `POST /api/admin/notices`：发布通知（Body 为 `Notice`，至少包含 `title`、`content`；`authorId` 会从 Session 当前用户补充）
- `GET /api/admin/notices`：获取通知列表
- `DELETE /api/admin/notices/{id}`：删除通知

### 9.3 后台统计

- `GET /api/admin/stats`：系统状态、用户数、通知数、各角色人数、资源占用等
