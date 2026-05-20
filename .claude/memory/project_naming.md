---
name: project-naming
description: 專案命名一律用「memoir」,MCIS 只是課程名稱
metadata:
  type: feedback
---

專案的正式名稱是 **memoir**(repo 名 `claudeProject/memoir`)。MCIS 只是這堂課的課名,不是專案識別。

**Why:** 使用者在 2026-05-20 我把 backend scaffold 命名成 `com.mcis` / `mcis-backend` / `POSTGRES_DB=mcis` 之後明確指正 — 雖然 CLAUDE.md 寫「MCIS Project」是因為混了課程脈絡,但任何代表「這個產品 / 這個系統」的名稱都該叫 memoir。

**How to apply:**
- 凡是需要挑「專案識別名」的場合,**一律用 memoir** — Kotlin/Java package(`com.memoir.*`)、Gradle group、Spring application name、Docker image / container 名、Postgres DB / user 名、K8s namespace / label、image tag prefix、CI workflow 名等等
- **不要**用 mcis,**除非**字面上就是在引用課程(例:「這份是 MCIS 課程的期末專案」)
- 既存 external state 命名有 mcis 的(例如 [[github-project]] 提到的 `MCIS Sprint Board` GitHub Project)**不主動改名** — 改名會破壞既有連結,等使用者明確要求再動
- CLAUDE.md 本身的 "MCIS Project" 字樣**不主動改** — 那是使用者寫的文件,可能有刻意混用課程/專案的意圖;要改要先問
