# Java Deep Plagiarism Mode Design

**Date:** 2026-04-21

**Owner:** Codex

**Status:** Approved for planning

## 1. Goal

Build a production-ready Java `DEEP` plagiarism mode that is clearly different from the existing `FAST` mode, yields higher precision on suspicious pairs, and provides teacher-facing evidence that explains why a pair is considered highly similar.

This round covers code, API, frontend flow, and automated tests. It does not cover deployment runbooks, operational monitoring, or release documentation.

## 2. Product Definition

### 2.1 FAST mode

`FAST` remains a coarse screening mode:

- Use normalized AST signature counts to compare whole submissions quickly.
- Favor throughput and broad recall.
- Return a lightweight evidence summary suitable for ranking suspicious pairs.
- Keep the current role of "quick scan" for teachers.

### 2.2 DEEP mode

`DEEP` becomes a high-accuracy review mode for Java submissions:

- Perform multi-stage structural analysis instead of relying on one aggregate AST score.
- Use method-level matching, control-flow evidence, and global structure consistency checks.
- Down-rank low-information similarities such as trivial wrappers, short methods, and template-style boilerplate.
- Return richer evidence payloads that help teachers understand the final score.

The expected teacher mental model is:

- `FAST`: "show me what might be suspicious"
- `DEEP`: "show me what is structurally close, and why"

## 3. Scope

### 3.1 In scope

- Java `DEEP` scoring redesign
- Java-specific `DEEP` evidence payload redesign
- Teacher launch page support for explicit `FAST` / `DEEP` selection
- Teacher results page behavior that distinguishes the two modes
- Teacher pair detail page support for deeper Java evidence
- Backend and frontend automated regression coverage

### 3.2 Out of scope

- C-language `DEEP` redesign
- Java multi-file project archive support
- New database tables unless strictly required
- Operations, release docs, and deployment scripts

## 4. Current State Summary

The codebase already contains the following foundations:

- `FAST`/`DEEP` mode plumbing in job creation and execution
- Java parsing and normalized AST extraction
- A Java deep extractor and deep similarity calculator
- Teacher report, pair detail, export, and AI explanation workflows
- Teacher results page mode switching

The main gap is not the existence of a `DEEP` branch. The main gap is that Java `DEEP` is not yet strong enough, visible enough, or explainable enough to justify its use as a more accurate mode.

## 5. Problem Statement

The current Java `DEEP` mode does not yet guarantee a clearly stronger decision process than `FAST`. If the two modes produce similar rankings, similar evidence, or only small algorithmic differences, teachers cannot trust `DEEP` as the higher-accuracy option.

We need a Java-only `DEEP` pipeline that:

- uses richer structural signals than `FAST`
- suppresses boilerplate-driven false positives
- exposes an interpretable breakdown of the score
- produces UI-visible differences between modes

## 6. Accuracy-First Design Principles

### 6.1 Precision over speed

For Java `DEEP`, it is acceptable to spend more CPU time per pair if the result quality improves in a way teachers can see and trust.

### 6.2 Multi-layer agreement

No pair should receive a very high `DEEP` score solely because of one strong local similarity. High confidence should come from agreement across several structural signals.

### 6.3 Low-information code must not dominate

Very short methods, accessors, basic wrappers, and other template-like methods should not heavily influence the final score.

### 6.4 Evidence must match the score

If a pair gets a high `DEEP` score, the pair detail page must be able to show method matches and structural segments that justify that score.

## 7. Deep Mode Analysis Pipeline

Java `DEEP` will be implemented as a three-stage decision pipeline.

### 7.1 Stage A: coarse structural recall

Use the existing normalized AST-based deep/coarse features to keep broad structural recall. This stage helps identify pairs that are worth deeper inspection.

Outputs:

- coarse recall score
- submission-level normalized structure signals

### 7.2 Stage B: method-level structural matching

Extract and compare methods as first-class units. For each method, build a richer structural representation including:

- parameter-count category
- return-kind category
- statement sequence skeleton
- branch/loop/exception structure
- nesting depth profile
- method call pattern profile
- local control-flow fragments

Method matching will try to pair methods between two submissions by strongest structural correspondence rather than by name.

Outputs:

- matched method pairs
- per-method match scores
- high-confidence match count
- matched method coverage rate
- unmatched core method ratio

### 7.3 Stage C: segment-level evidence validation

For the strongest matched methods, derive teacher-facing segments such as:

- loop-heavy logic
- branch-heavy logic
- nested control-flow blocks
- exception handling blocks
- method body segments with distinctive structure

This stage ensures the pair detail page can show concrete evidence rather than only aggregates.

Outputs:

- matched logic segments
- highlight ranges
- segment labels and summaries

## 8. Java DEEP Feature Model

The redesigned Java `DEEP` mode will use five feature groups.

### 8.1 Method skeleton features

Describe each method by normalized structure rather than identifiers:

- parameter count
- return category
- ordered statement skeleton
- loop / branch / switch / try-catch presence and nesting
- call arity pattern
- exit pattern, such as early return or branch-heavy return paths

### 8.2 Control-flow segment features

Describe localized logic blocks that are often preserved under renaming:

- branch chains
- nested loops
- switch branches
- try-catch-finally patterns
- condition complexity buckets

### 8.3 Method correspondence features

Measure whether two submissions contain a stable set of strong method matches:

- top matched method average
- total matched method count
- weighted matched method coverage
- unmatched method penalty

### 8.4 Global structure features

Capture high-level submission organization:

- method count distribution
- field / method balance
- class member scale buckets
- main flow organization
- helper-method decomposition patterns

### 8.5 Noise suppression features

Identify and down-weight low-information methods:

- getters / setters
- tiny wrappers
- trivial forwarding methods
- print-only or return-constant methods
- very short methods below a structural richness threshold

## 9. Scoring Model

### 9.1 Final score

The Java `DEEP` final score will be computed with a weighted combination and penalties:

`finalScore = 0.40 * methodMatchScore + 0.25 * controlFlowScore + 0.20 * globalStructureScore + 0.15 * coarseRecallScore - penalties`

### 9.2 Component goals

- `methodMatchScore`: the main signal; determines whether two submissions are built around similar method structures
- `controlFlowScore`: validates that meaningful logic blocks are similar, not just overall counts
- `globalStructureScore`: prevents local similarity from dominating when global organization diverges
- `coarseRecallScore`: keeps alignment with broad structural overlap and helps stabilize ranking

### 9.3 Penalties

Penalties are required to make `DEEP` more accurate than `FAST`.

Penalty sources:

- similarity concentrated in too few methods
- similarity dominated by low-information methods
- strong local matches but weak global structure agreement
- one-sided coverage where many core methods cannot be matched

### 9.4 High-score expectations

A very high `DEEP` score should require:

- multiple high-confidence method matches
- meaningful control-flow similarity
- acceptable global structure consistency
- low boilerplate dominance

## 10. Data and Payload Design

The current `FAST` evidence payload is not expressive enough for Java `DEEP`. Java `DEEP` payloads must include a richer breakdown.

### 10.1 Required payload fields

- `plagiarismMode`
- `algoVersion`
- `engineType`
- `scoreBreakdown`
- `penalties`
- `methodMatches`
- `segmentEvidence`
- `parseFailures`
- `summary`

### 10.2 Score breakdown fields

- `coarseRecallScore`
- `methodMatchScore`
- `controlFlowScore`
- `globalStructureScore`
- `penaltyTotal`
- `finalScore`

### 10.3 Penalty fields

Each penalty item should include:

- `type`
- `value`
- `reason`

Examples:

- `LOW_INFORMATION_DOMINANCE`
- `LOW_METHOD_COVERAGE`
- `LOCAL_ONLY_SIMILARITY`

### 10.4 Method match fields

Each top method match should include:

- left method key
- right method key
- score
- method type classification
- structural summary

### 10.5 Segment evidence fields

Each evidence segment should include:

- left/right file name
- line ranges
- block type
- summary
- confidence or contribution weight

## 11. Backend Changes

### 11.1 Domain layer

Enhance Java deep extraction and comparison to expose richer signals required for the score model:

- richer method metadata
- method classification
- richer control-flow descriptors
- low-information method detection
- matching coverage calculations
- penalty computation

### 11.2 Execution layer

In `TeacherPlagiarismExecutionService`, Java `DEEP` should:

- build the new Java deep profiles
- compute the redesigned score breakdown
- serialize the new Java `DEEP` payload
- preserve existing `FAST` behavior

### 11.3 Service layer

In `TeacherPlagiarismJobServiceImpl`, Java `DEEP` should:

- surface mode-specific explanation text
- preserve report compatibility
- return richer detail data for pair detail views
- ensure exports contain useful `DEEP`-specific information when available

### 11.4 Compatibility requirements

- existing `FAST` jobs must keep working
- report endpoints must remain backward compatible
- pair detail endpoints must tolerate old payloads while supporting new `DEEP` payloads

## 12. Frontend Changes

### 12.1 Launch page

Teachers must be able to explicitly choose mode when launching plagiarism analysis.

Requirements:

- add visible `FAST` / `DEEP` choice
- explain the trade-off:
  - `FAST`: faster, broader screening
  - `DEEP`: slower, more accurate structural review
- send `plagiarismMode` in the launch request
- default behavior should remain explicit, not hidden

### 12.2 Results page

The results page should make the active mode obvious.

Requirements:

- preserve mode switch behavior
- use mode-specific copy
- show `DEEP` as a more accurate review mode
- avoid implying `FAST` and `DEEP` are interchangeable

### 12.3 Pair detail page

The pair detail page is the key trust surface for `DEEP`.

Requirements:

- show score breakdown for `DEEP`
- show penalty explanations for `DEEP`
- emphasize top method matches
- emphasize segment-level structural evidence
- keep `FAST` detail simpler

### 12.4 Teacher understanding

Teachers should be able to answer these questions from the page:

- why is this pair high-scoring?
- is the score driven by several methods or just one?
- did the system down-rank template-like similarity?

## 13. Testing Strategy

### 13.1 Domain tests

Add or extend tests for:

- identifier renaming invariance
- literal substitution invariance
- structurally similar methods scoring highly
- globally different submissions being separated even with some local similarity
- low-information methods receiving reduced influence

### 13.2 Scoring regression tests

Create Java pair fixtures covering:

- true positive structural plagiarism
- renamed-but-structurally-preserved plagiarism
- same assignment pattern but independently implemented logic
- boilerplate-heavy false positives
- short-method noise cases

Expected outcome:

- `DEEP` should rank the true positives above the false positives more reliably than `FAST`

### 13.3 Service tests

Verify:

- `DEEP` job creation preserves mode
- `DEEP` execution writes redesigned payloads
- report and pair detail endpoints remain correct
- pair detail returns breakdown, method matches, and segment evidence

### 13.4 Frontend tests

Verify:

- launch page sends chosen mode
- results page loads the correct mode
- mode switch behavior remains correct
- pair detail renders `DEEP`-specific data without breaking `FAST`

## 14. Acceptance Criteria

This work is considered ready for testing handoff when all of the following are true:

1. Teachers can explicitly launch Java plagiarism analysis in `FAST` or `DEEP` mode.
2. Java `DEEP` produces materially different results and richer evidence than `FAST`.
3. Java `DEEP` pair detail includes score breakdown, method matches, and structural segment evidence.
4. Java `DEEP` down-ranks low-information/template-heavy similarity.
5. Automated tests demonstrate that `DEEP` is more discriminating than `FAST` on curated Java fixtures.
6. Existing `FAST` flows continue to work.

## 15. Risks

### 15.1 Overfitting to hand-made fixtures

If scoring is tuned too narrowly, it may perform well only on test samples.

Mitigation:

- use multiple fixture categories
- compare relative ranking behavior, not only single thresholds

### 15.2 Payload/UI mismatch

If backend returns rich data but the detail page does not explain it well, teachers still will not trust `DEEP`.

Mitigation:

- design payload and UI together
- keep pair detail work in scope

### 15.3 Performance drift

The new `DEEP` mode will be slower than `FAST`.

Mitigation:

- keep `FAST` untouched as the quick path
- limit the amount of teacher-facing segment evidence serialized per pair

## 16. Non-Goals for This Round

The following are intentionally deferred:

- Java project archive submission support
- cross-language normalization work
- teacher release notes and operator docs
- deployment automation changes

## 17. Implementation Recommendation

Implement the redesign in focused layers:

1. strengthen Java deep domain features and scoring
2. redesign payload and service mapping
3. expose launch-page mode choice
4. upgrade results and detail page rendering
5. finish with regression-oriented backend and frontend tests

This order keeps the most critical success condition first: `DEEP` must genuinely become more accurate before UI polish claims that it is.
