# Plagiarism Segment Navigation Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在不推翻现有 AST 粗颗粒度查重总分的前提下，为相似对详情页补齐“多个相似片段定位 + 顶部坐标导航条 + 双窗同步跳转 + 整段逻辑高亮”能力。

**Architecture:** 保留当前 `TeacherPlagiarismExecutionService` 的总分计算逻辑，不动现有相似对生成与排序；在详情接口 `getPairDetail` 上新增“定位专用片段提取器”，基于原始源码和现有 AST 归一化能力生成 `segments`。前端详情页从“按共享单行整行染色”升级为“按 segment 导航 + segment 区间高亮 + 点击滚动定位”。

**Tech Stack:** Spring Boot, MyBatis-Plus, JavaParser, Vue 3, Element Plus

---

## Scope And Constraints

- 当前系统已经有 AST 粗颗粒度相似度计算，但详情高亮仅依赖共享单行文本，根因在：
  - `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java`
  - `frontend/src/views/teacher/SimilarityPairDetail.vue`
- 本期目标不是替换现有查重评分算法，而是补“详情定位能力”。
- 本期优先保证：
  - 一个作业对可返回多个相似片段
  - 每个片段能落到原始代码行号
  - 点击导航条可同步滚动左右窗体
  - 高亮是整段逻辑，不再只命中 `return 0`
- 本期尽量避免数据库 schema 变更；片段数据优先在详情请求时动态构建。
- 未来更精确的新查重后端上线后，应可继续复用本期新增的 DTO、前端导航条与滚动交互。

## Current Code Map

**Backend existing files**
- `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismExecutionService.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/domain/JavaAstSignatureExtractor.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherPlagiarismPairDetailView.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherPlagiarismCodeCompareView.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherCodePaneView.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherCodeHighlightRangeView.java`

**Frontend existing files**
- `frontend/src/views/teacher/SimilarityPairDetail.vue`
- `frontend/src/api/teacherAssignments.js`
- `frontend/src/views/teacher/assignmentMappers.js`
- `frontend/src/router/index.js`

**Tests likely to modify**
- `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`
- 新增前端 helper 测试文件，避免把导航与高亮逻辑直接堆在 `.vue`

## Target API Contract

在现有 `pairDetail.codeCompare` 基础上新增 `segments`，示例：

```json
{
  "pairId": 101,
  "codeCompare": {
    "left": {
      "title": "ScoreUtils.java",
      "code": "...",
      "highlights": [
        { "startLine": 12, "endLine": 18 }
      ]
    },
    "right": {
      "title": "ScoreHelper.java",
      "code": "...",
      "highlights": [
        { "startLine": 15, "endLine": 21 }
      ]
    },
    "segments": [
      {
        "id": "seg-1",
        "label": "片段 1",
        "summary": "平均值计算逻辑",
        "score": 92,
        "leftFile": "ScoreUtils.java",
        "leftStartLine": 4,
        "leftEndLine": 10,
        "rightFile": "ScoreHelper.java",
        "rightStartLine": 4,
        "rightEndLine": 12
      },
      {
        "id": "seg-2",
        "label": "片段 2",
        "summary": "最大值计算逻辑",
        "score": 88,
        "leftFile": "ScoreUtils.java",
        "leftStartLine": 12,
        "leftEndLine": 20,
        "rightFile": "ScoreHelper.java",
        "rightStartLine": 15,
        "rightEndLine": 24
      }
    ]
  }
}
```

## File Structure Plan

**Create**
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherCodeSegmentView.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/domain/TeacherComparableCodeBlock.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/domain/TeacherComparableCodeBlockExtractor.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/domain/TeacherComparableCodeSegmentLocator.java`
- `frontend/src/views/teacher/similarityPairDetailHelpers.js`
- `frontend/src/views/teacher/similarityPairDetailHelpers.test.js`

**Modify**
- `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherPlagiarismCodeCompareView.java`
- `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java`
- `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`
- `frontend/src/views/teacher/assignmentMappers.js`
- `frontend/src/api/teacherAssignments.js`
- `frontend/src/views/teacher/SimilarityPairDetail.vue`

## Segment Extraction Rules

### Core principles

- 不再按“共享单行文本”构建高亮
- 以“可比较代码块”为单位构建片段
- 每个块必须保留原始文件行号范围
- 一对代码可以命中多个片段
- 必须过滤低价值噪音块

### Candidate blocks to extract

- `MethodDeclaration`
- `IfStmt`
- `ForStmt`
- `ForEachStmt`
- `WhileStmt`
- `DoStmt`
- `TryStmt`
- `SwitchStmt`
- 必要时补 `BlockStmt`，但仅限包含有效语句数达到阈值的 block

### Normalization rules

- 标识符统一成 `ID`
- 方法名统一成 `ID`
- 类型统一成 `TYPE`
- 字符串常量统一成 `STR`
- 数值常量统一成 `0`
- 去注释
- 连续空白压缩

### Noise filtering rules

- 以下块不得单独作为 segment：
  - 只有 `return 0` / `return null` / `return false`
  - 只有单行赋值
  - 只有变量声明
  - 只有括号/空行/注释
  - `package` / `import`
  - 归一化后文本长度小于阈值
  - 有效语句数小于阈值
- 建议阈值：
  - 归一化长度 `< 24` 直接过滤
  - 有效语句数 `< 2` 直接过滤

### Matching strategy

- 左右两侧提取候选块
- 按 `kind + normalizedSignatureHash` 分桶
- 桶内比较 `normalizedSnippet`
- 完全相同或高度相似则视为命中
- 对重叠或相邻片段进行合并

## Frontend Interaction Plan

### Layout

- 在 `SimilarityPairDetail.vue` 的代码区域顶部增加：
  - 片段导航条
  - 当前片段摘要
  - 上一个 / 下一个片段按钮（可选）

### Navigation behavior

- 每个 segment 显示为一个可点击标记
- hover 展示 `左右行号 + 摘要`
- 点击后：
  - 左窗滚动到 `leftStartLine`
  - 右窗滚动到 `rightStartLine`
  - 当前 segment 设为 active

### Highlight behavior

- 当前激活 segment：更强高亮
- 其他 segment：弱高亮
- 默认不再只根据旧 `highlights` 单独染行
- v1 先做整段高亮，v2 再做行内 token 高亮

### Scroll synchronization

- v1 不做复杂的像素级同步滚动，只做“点击片段时双窗同步跳转”
- 左右代码窗增加 `ref`
- 每行增加 `data-line`
- 点击 segment 后通过 `scrollIntoView({ block: 'center' })` 定位到起始行

## Chunk 1: Backend DTO And Contract

### Task 1: Add segment DTOs

**Files:**
- Create: `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherCodeSegmentView.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/dto/TeacherPlagiarismCodeCompareView.java`

- [ ] **Step 1: Write the failing backend test**
  在 `TeacherPlagiarismJobServiceImplTest` 中新增断言：
  - `detail.codeCompare().segments()` 非空
  - 至少可返回两个 segment

- [ ] **Step 2: Run test to verify it fails**

```bash
Set-Location 'D:\学校\毕业设计\项目\graduation_project\back'
mvn -q "-Dmaven.repo.local=D:\学校\毕业设计\项目\graduation_project\back\.m2repo" "-Dtest=TeacherPlagiarismJobServiceImplTest" test
```

- [ ] **Step 3: Add minimal DTO implementation**
  新增 `TeacherCodeSegmentView`：
  - `String id`
  - `String label`
  - `String summary`
  - `Integer score`
  - `String leftFile`
  - `Integer leftStartLine`
  - `Integer leftEndLine`
  - `String rightFile`
  - `Integer rightStartLine`
  - `Integer rightEndLine`

  修改 `TeacherPlagiarismCodeCompareView`，新增：
  - `List<TeacherCodeSegmentView> segments`

- [ ] **Step 4: Re-run the test**
- [ ] **Step 5: Commit**

## Chunk 2: Backend Segment Locator

### Task 2: Introduce comparable code block extractor

**Files:**
- Create: `back/src/main/java/com/ast/back/modules/plagiarism/domain/TeacherComparableCodeBlock.java`
- Create: `back/src/main/java/com/ast/back/modules/plagiarism/domain/TeacherComparableCodeBlockExtractor.java`
- Test: `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`

- [ ] **Step 1: Write failing tests for multi-segment extraction**
  覆盖：
  - 两个方法均相似时返回两个 segment
  - `return 0;` 不被单独识别成 segment
  - segment 的 `startLine/endLine` 命中真实方法体

- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement extractor**

  提取器输出：
  - 节点类型
  - 文件名
  - 原始起止行列
  - 原始源码
  - 归一化源码
  - 可读摘要

  注意：
  - 不要复用当前 `AstSignatureProfile`，它只有聚合计数，不够定位
  - 允许复用 `JavaAstSignatureExtractor` 的归一化思想，但不要直接改动当前总分计算路径

- [ ] **Step 4: Re-run tests**
- [ ] **Step 5: Commit**

### Task 3: Implement segment locator and wire into pair detail

**Files:**
- Create: `back/src/main/java/com/ast/back/modules/plagiarism/domain/TeacherComparableCodeSegmentLocator.java`
- Modify: `back/src/main/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImpl.java`
- Test: `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`

- [ ] **Step 1: Write failing test for `getPairDetail`**
  断言：
  - `detail.codeCompare().segments().size() >= 2`
  - `segments[0].leftStartLine < segments[0].leftEndLine`
  - `segments[0].rightStartLine < segments[0].rightEndLine`

- [ ] **Step 2: Run test to verify failure**
- [ ] **Step 3: Implement locator**

  v1 策略：
  - 提取左右候选块
  - 同类型块优先比较
  - 归一化文本完全相等时直接命中
  - 归一化文本近似时计算相似度并命中
  - 合并重叠/相邻块
  - 生成 `segments`

  在 `buildCodeCompare` 中：
  - 保留现有 `leftCode/rightCode`
  - `highlights` 改为由 `segments` 派生
  - 旧 `buildHighlightRanges` 标记为兼容 fallback，后续可删

- [ ] **Step 4: Re-run tests**
- [ ] **Step 5: Commit**

## Chunk 3: Frontend Data Model

### Task 4: Normalize segment data in frontend

**Files:**
- Modify: `frontend/src/views/teacher/assignmentMappers.js`
- Modify: `frontend/src/api/teacherAssignments.js`

- [ ] **Step 1: Write failing frontend helper test**
  覆盖：
  - `normalizePairDetail` 正确保留 `codeCompare.segments`
  - 空 segments 回退为空数组

- [ ] **Step 2: Run test to verify failure**

```bash
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
node --input-type=module -e "import('./src/views/teacher/assignmentMappers.test.js')"
```

- [ ] **Step 3: Minimal implementation**
- [ ] **Step 4: Re-run tests**
- [ ] **Step 5: Commit**

## Chunk 4: Frontend Segment Navigation UI

### Task 5: Add segment helpers and active-segment logic

**Files:**
- Create: `frontend/src/views/teacher/similarityPairDetailHelpers.js`
- Create: `frontend/src/views/teacher/similarityPairDetailHelpers.test.js`

- [ ] **Step 1: Write failing helper tests**
  覆盖：
  - segment 排序
  - 从 segment 派生左右高亮范围
  - 根据当前 active segment 返回高亮状态

- [ ] **Step 2: Run test to verify failure**

```bash
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
node --input-type=module -e "import('./src/views/teacher/similarityPairDetailHelpers.test.js')"
```

- [ ] **Step 3: Implement helper functions**
  建议导出：
  - `normalizeSegments`
  - `buildSegmentRangeMap`
  - `findSegmentById`
  - `buildLineHighlightState`

- [ ] **Step 4: Re-run tests**
- [ ] **Step 5: Commit**

### Task 6: Upgrade `SimilarityPairDetail.vue`

**Files:**
- Modify: `frontend/src/views/teacher/SimilarityPairDetail.vue`

- [ ] **Step 1: Add failing UI behavior checklist**
  手工/开发验证清单：
  - 顶部出现片段导航条
  - 点击片段 1 时左右窗都跳到对应片段
  - 当前片段整段高亮
  - 没有 segments 时仍能展示旧代码对比

- [ ] **Step 2: Implement navigation UI**
  新增区域：
  - `segment-nav`
  - 当前片段摘要
  - 片段编号 chips

  新增状态：
  - `activeSegmentId`
  - `leftEditorBodyRef`
  - `rightEditorBodyRef`

  新增行为：
  - `handleSegmentClick(segment)`
  - `scrollToLine(container, lineNumber)`

- [ ] **Step 3: Switch highlight source**
  从：
  - `leftHighlightRanges = codeCompare.left.highlights`
  - `rightHighlightRanges = codeCompare.right.highlights`

  切到：
  - 有 `activeSegment` 时用 active segment
  - 没有 active segment 时显示全部 segments 的弱高亮
  - 无 segments 时回退旧 highlights

- [ ] **Step 4: Re-verify page syntax**

```bash
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
@'
import { readFileSync } from 'node:fs'
import { parse } from '@vue/compiler-sfc'
const file = 'src/views/teacher/SimilarityPairDetail.vue'
const source = readFileSync(file, 'utf8')
const result = parse(source, { filename: file })
if (result.errors.length) {
  for (const error of result.errors) console.error(error.message || error)
  process.exit(1)
}
console.log('vue-parse-ok')
'@ | node
```

- [ ] **Step 5: Commit**

## Chunk 5: Verification And Regression

### Task 7: Backend regression verification

**Files:**
- Test: `back/src/test/java/com/ast/back/modules/plagiarism/application/impl/TeacherPlagiarismJobServiceImplTest.java`

- [ ] **Step 1: Run targeted backend tests**

```bash
Set-Location 'D:\学校\毕业设计\项目\graduation_project\back'
mvn -q "-Dmaven.repo.local=D:\学校\毕业设计\项目\graduation_project\back\.m2repo" "-Dtest=TeacherPlagiarismJobServiceImplTest" test
```

- [ ] **Step 2: Add one realistic multi-segment case**
  确保测试数据包含：
  - 两个方法都相似
  - 一个低价值 `return 0` 噪音
  - 结果中只命中两个真实片段

- [ ] **Step 3: Commit**

### Task 8: Frontend regression verification

**Files:**
- Test: `frontend/src/views/teacher/similarityPairDetailHelpers.test.js`
- Modify if needed: `frontend/src/views/teacher/SimilarityPairDetail.vue`

- [ ] **Step 1: Run frontend helper tests**

```bash
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
node --input-type=module -e "import('./src/views/teacher/similarityPairDetailHelpers.test.js')"
```

- [ ] **Step 2: Smoke-check mapper test**

```bash
Set-Location 'D:\学校\毕业设计\项目\graduation_project\frontend'
node --input-type=module -e "import('./src/views/teacher/assignmentMappers.test.js')"
```

- [ ] **Step 3: If build is attempted, document existing blocker**
  当前项目已知 blocker：
  - `frontend/src/views/Login.vue` 模板语法错误可能导致全量 build 失败

  因此本功能验证以：
  - 目标 `.vue` 语法解析
  - helper tests
  - 页面联调
  为主

## Future Compatibility Notes

- 新查重后端上线后，本期 `TeacherCodeSegmentView` 与前端导航条应直接复用
- 新后端只需要替换 segment 生成来源，不必重写：
  - `SimilarityPairDetail.vue` 交互
  - `assignmentMappers.js` 数据结构
  - 顶部坐标导航条
- 本期请避免把 segment 逻辑耦合进总分计算链路，保持“评分”与“定位”两套职责分离

## Delivery Checklist

- [ ] `pairDetail.codeCompare.segments` 可返回多个片段
- [ ] 片段不再只命中 `return 0`
- [ ] 顶部存在片段坐标导航条
- [ ] 点击导航点左右代码窗同步跳转
- [ ] 高亮的是整段逻辑范围
- [ ] 无 `segments` 时前端可回退旧逻辑
- [ ] 后端 targeted tests 通过
- [ ] 前端 helper tests 与 Vue syntax check 通过

Plan complete and saved to `docs/superpowers/plans/2026-04-02-plagiarism-segment-navigation-plan.md`. Ready to execute?
