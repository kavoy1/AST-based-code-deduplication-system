# AI Explanation Dual-Mode Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为教师端相似对处理页补齐“双模式 AI 解释 + 结构化评分 + 误差分析 + 历史记录”能力，且前端不暴露语言输入，所有上下文由后端自动组装。

**Architecture:** 保留现有“相似对详情 / 处理总结 / AI 解释”链路，但把 AI 解释从“单次纯文本”升级为“结构化记录”。后端新增双模式请求、上下文组装、JSON 结果解析和历史查询；前端新增模式选择弹窗、当前结果卡和历史记录列表，支持“仅代码”与“代码 + 系统证据”两条独立生成路径，并可选附带教师备注。

**Tech Stack:** Spring Boot, MyBatis-Plus, Jackson, Vue 3, Element Plus

---

## Scope And Assumptions

- 本期只做教师端 `SimilarityPairSummary.vue` 的 AI 解释体验升级，不扩展到学生端或管理员端。
- 前端不提供语言输入、Prompt 输入、证据文本输入；后端根据 `SimilarityPair`、`SimilarityEvidence`、代码对比数据和教师备注自动组装上下文。
- AI 解释每次生成都保留历史，不覆盖旧记录。
- 新输出以结构化 JSON 为主，纯文本 `result` 字段继续保留作兼容摘要。
- 教师备注默认不传给 AI；只有在弹窗里明确勾选才传。
- 历史记录保留“仅代码”和“代码 + 系统证据”两种模式，便于后续做实验对比、论文展示和答辩截图。

## Current Code Map

**Backend existing files**
- `back/src/main/java/com/ast/back/infra/ai/AiExplanationRequest.java`
- `back/src/main/java/com/ast/back/infra/ai/AiExplanationProvider.java`
- `back/src/main/java/com/ast/back/infra/ai/AiExplanationResponse.java`
- `back/src/main/java/com/ast/back/infra/ai/QwenAiExplanationProvider.java`
- `back/src/main/java/com/ast/back/infra/ai/QwenPromptBuilder.java`
- `back/src/main/java/com/ast/back/modules/ai/application/AiExplanationService.java`
- `back/src/main/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImpl.java`
- `back/src/main/java/com/ast/back/modules/ai/persistence/entity/AiExplanation.java`
- `back/src/main/java/com/ast/back/modules/ai/persistence/mapper/AiExplanationMapper.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/application/TeacherPlagiarismJobService.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobController.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherPlagiarismPairDetailView.java`

**Frontend existing files**
- `frontend/src/api/teacherAssignments.js`
- `frontend/src/views/teacher/assignmentMappers.js`
- `frontend/src/views/teacher/assignmentMappers.test.js`
- `frontend/src/views/teacher/SimilarityPairSummary.vue`

**Existing tests**
- `back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java`
- `back/src/test/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobControllerTest.java`
- `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`

## File Structure Plan

**Create**
- `back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationMode.java`
- `back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationStructuredResult.java`
- `back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationContextSummary.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationCreateRequest.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationView.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationHistoryItemView.java`
- `back/src/main/java/com/ast/back/infra/ai/AiExplanationStructuredResultParser.java`
- `back/src/main/resources/sql/2026-04-03-ai-explanation-structured.sql`
- `frontend/src/views/teacher/similarityPairSummaryHelpers.js`
- `frontend/src/views/teacher/similarityPairSummaryHelpers.test.js`
- `frontend/src/views/teacher/components/AiExplanationModeDialog.vue`
- `frontend/src/views/teacher/components/AiExplanationResultCard.vue`
- `frontend/src/views/teacher/components/AiExplanationHistoryList.vue`

**Modify**
- `back/src/main/java/com/ast/back/infra/ai/AiExplanationRequest.java`
- `back/src/main/java/com/ast/back/infra/ai/AiExplanationResponse.java`
- `back/src/main/java/com/ast/back/infra/ai/QwenPromptBuilder.java`
- `back/src/main/java/com/ast/back/infra/ai/QwenAiExplanationProvider.java`
- `back/src/main/java/com/ast/back/modules/ai/application/AiExplanationService.java`
- `back/src/main/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImpl.java`
- `back/src/main/java/com/ast/back/modules/ai/persistence/entity/AiExplanation.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/application/TeacherPlagiarismJobService.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobController.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherPlagiarismPairDetailView.java`
- `back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java`
- `back/src/test/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobControllerTest.java`
- `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`
- `frontend/src/api/teacherAssignments.js`
- `frontend/src/views/teacher/assignmentMappers.js`
- `frontend/src/views/teacher/assignmentMappers.test.js`
- `frontend/src/views/teacher/SimilarityPairSummary.vue`

---

## Chunk 1: Backend Contract And Persistence

### Task 1: Add request DTOs and explanation mode contract

**Files:**
- Create: `back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationMode.java`
- Create: `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationCreateRequest.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobController.java`
- Test: `back/src/test/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobControllerTest.java`

- [ ] **Step 1: Write the failing controller tests**

覆盖：
- `POST /api/teacher/similarity-pairs/{pairId}/ai-explanations` 能接收：
  ```json
  {
    "mode": "CODE_ONLY",
    "includeTeacherNote": false
  }
  ```
- 不传请求体时仍能走默认值
- `mode` 非法时返回参数错误

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\back'
mvn -q "-Dmaven.repo.local=D:\学校\毕业设计\项目\graduation_project\back\.m2repo" "-Dtest=PlagiarismJobControllerTest" test
```

Expected: FAIL because request DTO / controller signature is still request-less.

- [ ] **Step 3: Add minimal request contract**

实现：
- `AiExplanationMode` 仅包含：
  - `CODE_ONLY`
  - `CODE_WITH_SYSTEM_EVIDENCE`
- `TeacherAiExplanationCreateRequest` 字段：
  - `AiExplanationMode mode`
  - `Boolean includeTeacherNote`
- `PlagiarismJobController#createAiExplanation(...)` 改为接收请求体，并给出默认值：
  - `mode = CODE_ONLY`
  - `includeTeacherNote = false`

- [ ] **Step 4: Re-run the controller test**

- [ ] **Step 5: Commit**

```bash
git add back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationMode.java back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationCreateRequest.java back/src/main/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobController.java back/src/test/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobControllerTest.java
git commit -m "feat: add AI explanation generation request contract"
```

### Task 2: Extend AI explanation persistence for history and structured fields

**Files:**
- Create: `back/src/main/resources/sql/2026-04-03-ai-explanation-structured.sql`
- Modify: `back/src/main/java/com/ast/back/modules/ai/persistence/entity/AiExplanation.java`
- Test: `back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java`

- [ ] **Step 1: Write the failing service test**

覆盖：
- 成功记录会保存：
  - `mode`
  - `includeTeacherNote`
  - `aiScore`
  - `systemScore`
  - `scoreDiff`
  - `structuredResultJson`
- 失败记录也会保存：
  - `mode`
  - `includeTeacherNote`
  - `status = FAILED`

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\back'
mvn -q "-Dmaven.repo.local=D:\学校\毕业设计\项目\graduation_project\back\.m2repo" "-Dtest=AiExplanationServiceImplTest" test
```

Expected: FAIL because entity lacks structured fields and tests cannot assert them.

- [ ] **Step 3: Add persistence fields and migration**

`AiExplanation` 新增字段：
- `String mode`
- `Integer includeTeacherNote`
- `Integer aiScore`
- `Integer systemScore`
- `Integer scoreDiff`
- `String structuredResultJson`

SQL 脚本：
- 对 `ai_explanation` 做增量 `ALTER TABLE`
- 为 `pair_id, create_time` 建议补索引（如当前库没有）

- [ ] **Step 4: Re-run the service test**

- [ ] **Step 5: Commit**

```bash
git add back/src/main/resources/sql/2026-04-03-ai-explanation-structured.sql back/src/main/java/com/ast/back/modules/ai/persistence/entity/AiExplanation.java back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java
git commit -m "feat: persist structured AI explanation history"
```

---

## Chunk 2: Backend AI Context Assembly And Structured Parsing

### Task 3: Refactor AI request assembly for dual modes

**Files:**
- Create: `back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationContextSummary.java`
- Modify: `back/src/main/java/com/ast/back/infra/ai/AiExplanationRequest.java`
- Modify: `back/src/main/java/com/ast/back/modules/ai/application/AiExplanationService.java`
- Modify: `back/src/main/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImpl.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/application/TeacherPlagiarismJobService.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java`
- Test: `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`
- Test: `back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

覆盖：
- `CODE_ONLY` 模式时，AI request 不带系统证据摘要
- `CODE_WITH_SYSTEM_EVIDENCE` 模式时，AI request 带：
  - 系统分数
  - 相似片段数
  - 行号范围摘要
  - 关键证据标签
- `includeTeacherNote=false` 时，不传 `teacherNote`
- `includeTeacherNote=true` 时，才把当前 `pair.teacherNote` 放入 request

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\back'
mvn -q "-Dmaven.repo.local=D:\学校\毕业设计\项目\graduation_project\back\.m2repo" "-Dtest=AiExplanationServiceImplTest,TeacherPlagiarismJobServiceImplTest" test
```

Expected: FAIL because current service only accepts `(pair, evidence)` and has no dual-mode branching.

- [ ] **Step 3: Implement request assembly**

`AiExplanationRequest` 升级为显式字段，而不是只塞 `(pair, evidence)`：
- `pairId`
- `mode`
- `leftCode`
- `rightCode`
- `systemScore`
- `evidenceSummary`
- `contextSummary`
- `teacherNote`

`TeacherPlagiarismJobService#createAiExplanation(...)` 改成接收：
- `Long teacherId`
- `Long pairId`
- `AiExplanationMode mode`
- `boolean includeTeacherNote`

后端自行从：
- `SimilarityPair`
- `SimilarityEvidence`
- `TeacherPlagiarismPairDetailView.codeCompare`
中组装上下文。

- [ ] **Step 4: Re-run the service tests**

- [ ] **Step 5: Commit**

```bash
git add back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationContextSummary.java back/src/main/java/com/ast/back/infra/ai/AiExplanationRequest.java back/src/main/java/com/ast/back/modules/ai/application/AiExplanationService.java back/src/main/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImpl.java back/src/main/java/com/ast/back/modules/plagiarism/application/TeacherPlagiarismJobService.java back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java
git commit -m "feat: assemble dual-mode AI explanation context"
```

### Task 4: Make provider output structured JSON and parse it safely

**Files:**
- Create: `back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationStructuredResult.java`
- Create: `back/src/main/java/com/ast/back/infra/ai/AiExplanationStructuredResultParser.java`
- Modify: `back/src/main/java/com/ast/back/infra/ai/QwenPromptBuilder.java`
- Modify: `back/src/main/java/com/ast/back/infra/ai/AiExplanationResponse.java`
- Modify: `back/src/main/java/com/ast/back/infra/ai/QwenAiExplanationProvider.java`
- Modify: `back/src/main/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImpl.java`
- Test: `back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

覆盖：
- provider 返回合法 JSON 时，服务能解析出：
  - `aiScore`
  - `systemScore`
  - `scoreDiff`
  - `diffDirection`
  - `riskLevel`
  - `conclusion`
  - `reasoning`
  - `evidenceSummary`
- provider 返回非 JSON 时：
  - 记录 `FAILED`
  - 保留原始 `responsePayload`
  - `errorMsg` 明确说明“结构化解析失败”

- [ ] **Step 2: Run test to verify failure**

- [ ] **Step 3: Update prompt and parsing path**

Prompt 规则：
- 明确要求只返回 JSON
- `aiScore` 必须是 `0~100` 整数
- `systemScore` 由后端直接注入
- `scoreDiff` 必须等于两者绝对差值
- `CODE_ONLY` 与 `CODE_WITH_SYSTEM_EVIDENCE` 使用不同提示段落

解析规则：
- 单独封装 `AiExplanationStructuredResultParser`
- 容忍大模型返回 ```json fenced block```，先清洗再解析
- 解析后再做二次字段校验

`AiExplanation.result` 存摘要句：
- 优先用 `conclusion`
- 兼容旧页面和旧导出逻辑

- [ ] **Step 4: Re-run the AI service tests**

- [ ] **Step 5: Commit**

```bash
git add back/src/main/java/com/ast/back/modules/ai/dto/AiExplanationStructuredResult.java back/src/main/java/com/ast/back/infra/ai/AiExplanationStructuredResultParser.java back/src/main/java/com/ast/back/infra/ai/QwenPromptBuilder.java back/src/main/java/com/ast/back/infra/ai/AiExplanationResponse.java back/src/main/java/com/ast/back/infra/ai/QwenAiExplanationProvider.java back/src/main/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImpl.java back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java
git commit -m "feat: parse structured AI explanation output"
```

---

## Chunk 3: Backend Read Models And API Responses

### Task 5: Add history list endpoint and response DTOs

**Files:**
- Create: `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationView.java`
- Create: `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationHistoryItemView.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherPlagiarismPairDetailView.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/application/TeacherPlagiarismJobService.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobController.java`
- Test: `back/src/test/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobControllerTest.java`
- Test: `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`

- [ ] **Step 1: Write the failing tests**

覆盖：
- `GET /api/teacher/similarity-pairs/{pairId}/ai-explanations` 返回历史列表，按 `id desc`
- `GET /latest` 返回最新一条结构化视图，不直接暴露实体原字段
- `getPairDetail(...)` 中的 `latestAiExplanation` 同步为新视图结构

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\back'
mvn -q "-Dmaven.repo.local=D:\学校\毕业设计\项目\graduation_project\back\.m2repo" "-Dtest=PlagiarismJobControllerTest,TeacherPlagiarismJobServiceImplTest" test
```

Expected: FAIL because list endpoint and DTO mapping do not exist.

- [ ] **Step 3: Implement read models**

`TeacherAiExplanationView` 字段建议：
- `id`
- `mode`
- `includeTeacherNote`
- `status`
- `provider`
- `model`
- `createTime`
- `aiScore`
- `systemScore`
- `scoreDiff`
- `diffDirection`
- `riskLevel`
- `conclusion`
- `reasoning`
- `evidenceSummary`
- `result`

历史列表可直接复用该 DTO，或使用 `TeacherAiExplanationHistoryItemView` 裁剪字段。

- [ ] **Step 4: Re-run controller/service tests**

- [ ] **Step 5: Commit**

```bash
git add back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationView.java back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherAiExplanationHistoryItemView.java back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherPlagiarismPairDetailView.java back/src/main/java/com/ast/back/modules/plagiarism/application/TeacherPlagiarismJobService.java back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java back/src/main/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobController.java back/src/test/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobControllerTest.java back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java
git commit -m "feat: expose AI explanation history and structured views"
```

---

## Chunk 4: Frontend Data Mapping And API Wiring

### Task 6: Add frontend API methods for create/list/latest with the new contract

**Files:**
- Modify: `frontend/src/api/teacherAssignments.js`
- Modify: `frontend/src/views/teacher/assignmentMappers.js`
- Modify: `frontend/src/views/teacher/assignmentMappers.test.js`

- [ ] **Step 1: Write the failing frontend mapper test**

覆盖：
- `normalizePairDetail(detail)` 能正确接收新的 `latestAiExplanation`
- 新增 `normalizeAiExplanationHistory(records)`，能把后端 history 列表映射为前端可用结构
- `createTeacherAiExplanation(pairId, payload)` 会发送：
  - `mode`
  - `includeTeacherNote`

- [ ] **Step 2: Run test to verify it fails**

Run:
```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
node --test 'src/views/teacher/assignmentMappers.test.js'
```

Expected: FAIL because helper normalization and request payload support do not exist yet.

- [ ] **Step 3: Implement API and normalization**

新增前端 API：
- `createTeacherAiExplanation(pairId, payload)`
- `fetchTeacherAiExplanationHistory(pairId)`
- `fetchLatestTeacherAiExplanation(pairId)`（保留但适配新返回）

前端统一结果结构：
- `aiScore`
- `systemScore`
- `scoreDiff`
- `diffDirection`
- `mode`
- `includeTeacherNote`
- `riskLevel`
- `conclusion`
- `reasoning[]`
- `evidenceSummary[]`
- `status`
- `provider`
- `model`
- `createTime`

- [ ] **Step 4: Re-run mapper test**

- [ ] **Step 5: Commit**

```bash
git add frontend/src/api/teacherAssignments.js frontend/src/views/teacher/assignmentMappers.js frontend/src/views/teacher/assignmentMappers.test.js
git commit -m "feat: wire frontend AI explanation history APIs"
```

### Task 7: Extract focused helpers for mode labels, score diff, and history presentation

**Files:**
- Create: `frontend/src/views/teacher/similarityPairSummaryHelpers.js`
- Create: `frontend/src/views/teacher/similarityPairSummaryHelpers.test.js`

- [ ] **Step 1: Write the failing helper tests**

覆盖：
- `formatAiExplanationMode('CODE_ONLY') => '仅代码'`
- `formatAiExplanationMode('CODE_WITH_SYSTEM_EVIDENCE') => '代码 + 系统证据'`
- `buildScoreDiffTone(0/5/20)` 分层正确
- `buildTeacherNoteBadge(true/false)` 文案正确
- `buildHistorySubtitle(record)` 输出稳定短句

- [ ] **Step 2: Run test to verify failure**

Run:
```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
node --test 'src/views/teacher/similarityPairSummaryHelpers.test.js'
```

- [ ] **Step 3: Implement helper file**

保持 `.vue` 组件简洁，把：
- 模式标签
- 误差颜色
- 状态标签
- 历史时间展示
- 结论占位文本
全部抽到 helper 里。

- [ ] **Step 4: Re-run helper test**

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/teacher/similarityPairSummaryHelpers.js frontend/src/views/teacher/similarityPairSummaryHelpers.test.js
git commit -m "feat: add summary helpers for AI explanation history"
```

---

## Chunk 5: Frontend UX For Dual-Mode Generation And History

### Task 8: Build the generation mode dialog

**Files:**
- Create: `frontend/src/views/teacher/components/AiExplanationModeDialog.vue`
- Modify: `frontend/src/views/teacher/SimilarityPairSummary.vue`

- [ ] **Step 1: Write the failing UI behavior test or manual acceptance checklist**

如果当前仓库没有 Vue component test harness，则使用页面级验收清单替代自动化测试：
- 点击 `生成 AI 解释` 不直接请求接口
- 弹出模式选择对话框
- 默认模式为 `仅代码`
- `附带当前教师备注` 默认关闭
- 点击确认才发请求

- [ ] **Step 2: Implement the dialog component**

弹窗字段：
- 单选：
  - `仅代码`
  - `代码 + 系统证据`
- 开关：
  - `附带当前教师备注`

要求：
- 不出现语言输入
- 不出现 Prompt 输入
- 不出现证据文本输入

- [ ] **Step 3: Wire dialog actions into `SimilarityPairSummary.vue`**

- [ ] **Step 4: Verify in local build**

Run:
```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
npm.cmd run build
```

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/teacher/components/AiExplanationModeDialog.vue frontend/src/views/teacher/SimilarityPairSummary.vue
git commit -m "feat: add AI explanation mode selection dialog"
```

### Task 9: Redesign AI explanation area into result card + history list

**Files:**
- Create: `frontend/src/views/teacher/components/AiExplanationResultCard.vue`
- Create: `frontend/src/views/teacher/components/AiExplanationHistoryList.vue`
- Modify: `frontend/src/views/teacher/SimilarityPairSummary.vue`

- [ ] **Step 1: Replace chat-style rendering with structured cards**

当前纯文本打字机效果改为：
- 顶部：当前记录结论卡
  - `AI 相似度`
  - `系统相似度`
  - `误差值`
  - `误差方向`
  - `风险等级`
  - `AI 结论`
  - `关键依据`
- 下方：历史记录列表
  - 时间
  - 模式
  - 是否带备注
  - 分数
  - 状态

- [ ] **Step 2: Add record switching**

要求：
- 点击历史记录可切换上方内容
- 默认选中最新一条
- 新生成成功后自动选中新记录

- [ ] **Step 3: Add loading / failed / empty states**

空态：
- “还没有 AI 解释记录”
失败态：
- “本次生成失败，可查看错误原因并重试”
加载态：
- 骨架屏或轻提示，不再使用长打字动画

- [ ] **Step 4: Verify in local build**

- [ ] **Step 5: Commit**

```bash
git add frontend/src/views/teacher/components/AiExplanationResultCard.vue frontend/src/views/teacher/components/AiExplanationHistoryList.vue frontend/src/views/teacher/SimilarityPairSummary.vue
git commit -m "feat: redesign AI explanation area with history"
```

---

## Chunk 6: End-To-End Verification And Documentation

### Task 10: Verify backend and frontend paths together

**Files:**
- Verify: `back/src/test/java/com/ast/back/modules/ai/application/impl/AiExplanationServiceImplTest.java`
- Verify: `back/src/test/java/com/ast/back/modules/plagiarism/controller/PlagiarismJobControllerTest.java`
- Verify: `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`
- Verify: `frontend/src/views/teacher/assignmentMappers.test.js`
- Verify: `frontend/src/views/teacher/similarityPairSummaryHelpers.test.js`

- [ ] **Step 1: Run backend targeted tests**

```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\back'
mvn -q "-Dmaven.repo.local=D:\学校\毕业设计\项目\graduation_project\back\.m2repo" "-Dtest=AiExplanationServiceImplTest,PlagiarismJobControllerTest,TeacherPlagiarismJobServiceImplTest" test
```

Expected:
- All targeted AI explanation tests PASS

- [ ] **Step 2: Run frontend targeted tests**

```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
node --test 'src/views/teacher/assignmentMappers.test.js'
node --test 'src/views/teacher/similarityPairSummaryHelpers.test.js'
```

Expected:
- Both test files PASS

- [ ] **Step 3: Run full frontend build**

```powershell
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
npm.cmd run build
```

Expected:
- Build succeeds
- Existing Node/Vite warning can remain if unrelated

- [ ] **Step 4: Manually verify the teacher flow**

Checklist:
- 相似对处理总结页点击 `生成 AI 解释`
- 先弹模式选择窗
- 生成 `仅代码` 解释，记录写入历史
- 再生成 `代码 + 系统证据` 解释，记录写入历史
- 切换历史记录时，上方结果卡同步更新
- `AI 分数 / 系统分数 / 误差值` 都正确展示
- `附带备注` 开关关闭和打开时，后端请求体正确

- [ ] **Step 5: Commit**

```bash
git add .
git commit -m "feat: add dual-mode AI explanation workflow"
```

---

## Notes For The Implementer

- 不要把“老师手动处理状态 / 最终判定”默认塞进 AI 上下文，这会污染独立判断实验。
- `CODE_ONLY` 与 `CODE_WITH_SYSTEM_EVIDENCE` 必须走两套清晰 prompt 分支，不要只靠一句附加文案混过去。
- 历史列表接口要稳定排序，优先 `id desc` 或 `create_time desc`，并确保前端切换时不闪烁。
- 如果当前数据库环境暂时不能立即跑 SQL 迁移，至少要把 migration 脚本和实体字段一并提交，避免接口设计先跑偏。
- 如果 AI Provider 当前不稳定，优先保证“失败记录也能写历史”，否则老师看不到失败原因。
