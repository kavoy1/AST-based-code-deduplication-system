# 教师端多文件相似代码对比前后端改造说明

## 1. 改造背景

教师端原有“相似对详情”页面只支持一组固定的左右代码：

- `left.code`
- `right.code`

这种结构默认把“相似代码对比”理解成：

- 一个学生一份文件
- 另一个学生一份文件
- 两边文件天然一一对应

但真实提交中，经常会出现以下情况：

- 文件名完全不同，但实现逻辑高度相似
- 同一段逻辑被拆到多个 `.java` 文件中
- 一个学生把逻辑写在 `Utils.java`，另一个写在 `Main.java`
- 一侧存在独有文件，另一侧没有对应文件

因此，后续展示和数据结构都不能再按“文件名相同才匹配”的思路处理，而要改成：

- **按代码结构和片段匹配**
- **按相似映射展示**
- **支持多文件、多映射、多片段**

---

## 2. 目标

本次改造目标如下：

1. 后端在相似对详情接口中返回多文件映射结果
2. 前端相似对详情页支持切换不同文件映射
3. 支持以下四类展示：
   - 匹配文件对
   - 跨文件片段
   - 仅学生 A 存在
   - 仅学生 B 存在
4. 保持当前页面主视图不变，仍以“左右对比代码 + 片段定位”为核心
5. 保持 AI 解释、处理总结、相似度等原有教师端功能不被破坏

---

## 3. 后端改造

### 3.1 DTO 扩展

#### 3.1.1 `TeacherPlagiarismCodeCompareView`

文件：

- `D:\学校\毕业设计\项目\graduation_project\back\src\main\java\com\ast\back\modules\plagiarism\dto\TeacherPlagiarismCodeCompareView.java`

新增字段：

- `key`
- `label`
- `relationType`
- `left`
- `right`
- `segments`

用于描述一组“可切换的代码对比视图”。

#### 3.1.2 `TeacherPlagiarismUnmatchedFileView`

新增文件：

- `D:\学校\毕业设计\项目\graduation_project\back\src\main\java\com\ast\back\modules\plagiarism\dto\TeacherPlagiarismUnmatchedFileView.java`

用于表示“仅一侧存在”的文件：

- `key`
- `label`
- `pane`

#### 3.1.3 `TeacherPlagiarismPairDetailView`

文件：

- `D:\学校\毕业设计\项目\graduation_project\back\src\main\java\com\ast\back\modules\plagiarism\dto\TeacherPlagiarismPairDetailView.java`

新增字段：

- `matchedFilePairs`
- `unmatchedLeftFiles`
- `unmatchedRightFiles`
- `crossFileSegments`

同时保留：

- `codeCompare`

作为兼容主对比入口。

---

### 3.2 服务层改造

核心文件：

- `D:\学校\毕业设计\项目\graduation_project\back\src\main\java\com\ast\back\modules\plagiarism\application\impl\TeacherPlagiarismJobServiceImpl.java`

#### 3.2.1 核心思路

在 `getPairDetail(...)` 中，不再只构造一组固定代码对比，而是构造一个多文件映射结果包：

- 读取两个学生最新有效提交
- 读取两个提交下所有可解析文件
- 提取每个文件的代码文本与可比较结构片段
- 对全部文件做片段级匹配
- 将结果按“文件对”聚合
- 区分：
  - 匹配文件对
  - 跨文件片段
  - 左独有文件
  - 右独有文件

#### 3.2.2 新增/调整的方法

主要新增了以下逻辑：

- `buildPairCompareBundle(...)`
- `buildAggregateCodeCompare(...)`
- `buildFilePaneAssemblies(...)`
- `buildFilePaneAssembly(...)`
- `buildFilePairCompareView(...)`
- `relabelSegments(...)`
- `buildUnmatchedFileViews(...)`
- `comparePriorityComparator()`
- `highestSegmentScore(...)`
- `buildFilePairKey(...)`
- `splitFilePairKey(...)`

#### 3.2.3 返回规则

服务层最终返回：

- `matchedFilePairs`
  - 正常文件对映射
- `crossFileSegments`
  - 一个文件对应多个文件或多片段的跨文件结果
- `unmatchedLeftFiles`
  - 仅左侧存在
- `unmatchedRightFiles`
  - 仅右侧存在

同时选出一个 `primaryCompare` 作为默认初始视图：

1. 优先第一组匹配文件对
2. 其次第一组跨文件片段
3. 最后使用聚合视图兜底

---

## 4. 前端改造

### 4.1 数据映射

文件：

- `D:\学校\毕业设计\项目\graduation_project\frontend\src\views\teacher\assignmentMappers.js`

`normalizePairDetail(detail)` 现在会额外映射：

- `matchedFilePairs`
- `unmatchedLeftFiles`
- `unmatchedRightFiles`
- `crossFileSegments`

---

### 4.2 相似对详情辅助函数

文件：

- `D:\学校\毕业设计\项目\graduation_project\frontend\src\views\teacher\similarityPairDetailHelpers.js`

新增/重写能力：

- `normalizeCodeCompare(compare)`
- `normalizeUnmatchedFile(file, side)`
- `buildCompareTabs(detail)`
- `normalizeHighlightRanges(...)`
- `buildSegmentRailMarkers(...)`
- `buildCodeLines(...)`

其中 `buildCompareTabs(detail)` 会把后端返回整理成前端可切换的标签页：

- `matched`
- `cross`
- `left-only`
- `right-only`

---

### 4.3 相似对详情页面

文件：

- `D:\学校\毕业设计\项目\graduation_project\frontend\src\views\teacher\SimilarityPairDetail.vue`

本次页面已支持：

1. 顶部保留返回、处理总结、相似度
2. 新增文件映射切换条
3. 当前选中的映射结果驱动：
   - 左侧代码窗
   - 右侧代码窗
   - 片段定位轨
4. 对于“仅单侧存在”的文件：
   - 真实文件在一侧展示
   - 另一侧显示空白占位

当前页面已经不再只依赖：

- `pairDetail.codeCompare`

而是通过：

- `compareTabs`
- `activeTabKey`
- `currentTab`

控制当前查看的是哪一组映射。

---

## 5. 前端交互规则

### 5.1 文件映射条

页面顶部新增映射切换条，支持以下类型：

- 匹配对
- 跨文件
- 仅 A 存在
- 仅 B 存在

点击后切换当前代码视图。

### 5.2 片段定位

右侧片段定位轨仍然保留，但现在只作用于：

- 当前选中的映射页签

如果当前映射没有片段，则隐藏片段轨，仅保留空内容提示。

### 5.3 空侧占位

当某个文件只存在于一边时：

- 左侧或右侧真实展示代码
- 另一侧展示“暂无对应文件”

这样教师可以明确看到：

- 哪些文件是单边存在的
- 而不是误以为系统漏数据

---

## 6. 测试与验证

### 6.1 前端测试

文件：

- `D:\学校\毕业设计\项目\graduation_project\frontend\src\views\teacher\similarityPairDetailHelpers.test.js`

新增并通过了以下测试：

- `buildCompareTabs groups matched pairs, cross-file fragments and unmatched files`

验证了：

- 匹配对
- 跨文件片段
- 左独有
- 右独有

都能正确生成标签页。

### 6.2 后端测试

文件：

- `D:\学校\毕业设计\项目\graduation_project\back\src\test\java\com\ast\back\modules\plagiarism\application\impl\TeacherPlagiarismJobServiceImplTest.java`

新增并通过了：

- `getPairDetailBuildsMatchedPairsCrossFileGroupsAndUnmatchedFiles`

验证了：

- `Utils.java ↔ Main.java` 能被识别为匹配对
- `LeftOnly.java` 能落入 `unmatchedLeftFiles`
- `Extra.java` 能落入 `unmatchedRightFiles`

### 6.3 构建验证

已完成：

- 前端 `npm.cmd run build` 通过
- 前端 `node --test` 通过
- 后端 `TeacherPlagiarismJobServiceImplTest` 通过

---

## 7. 当前能力边界

本次已经完成的是：

- 多文件映射 DTO
- 多文件映射计算
- 前端文件映射切换
- 单边文件展示

当前还没有进一步展开的是：

- 更高级的“一个文件对应多个文件”的多重映射详情交互
- 点击片段时跨页签自动跳转
- 复杂项目级目录树展示

这些属于下一阶段增强，不影响当前版本已经从“按文件名单对单展示”升级为“按结构和片段映射展示”。

---

## 8. 后续建议

后续建议继续做三件事：

1. 在相似对详情页增加“映射统计摘要”
   - 匹配对数量
   - 跨文件片段数量
   - 单边独有文件数量

2. 在片段定位里支持跨页签联动
   - 点击某个跨文件片段时自动切换到对应文件组

3. 后端增加更明确的映射解释字段
   - 例如为什么判定为跨文件
   - 哪个方法/类是主要命中来源

这样教师端对“文件名不同但逻辑相似”的解释会更完整、更有说服力。
