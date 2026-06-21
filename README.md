# memoir

> **外國旅客在台灣初期探索文化的體驗設計**(MCIS Project)— 期末專案
> 重新設計「文化被理解的方式」:解決資訊存在卻無法被連結與理解的問題。
> MVP 範圍:**台南單城市試點**(NFR-18),驗證後再擴張全台。

---

## ✅ 目前實作狀態

> 專案採 **mobile-first** 推進:目前唯一已實作的元件是 **原生 Android app**(`frontend/mobile/`)。
> 下方「技術棧」「Quick Start」「專案結構」中標示 _(規劃中)_ 的元件(backend / ai-services / kiosk / admin / deploy)尚未建立,屬目標架構。

### 已實作功能(Android MVP)

- **記憶建立流程**:多步驟 wizard(範本選擇 → 照片擷取 / 選取 → 編輯 → 反思),資料持久化於 Room。
- **記憶庫(Memories)**:Route / Bookmark 雙分頁。Route 顯示進行中 / 已完成的記憶(讀 Room 即時資料);Bookmark 顯示已收藏景點並支援搜尋。
- **記憶卡片操作**:三點選單 Edit / Delete / Duplicate / Share(透過 `FileProvider` 分享照片);草稿可從卡片本體直接續編。
- **景點詳情(Spot Detail)**:從 Bookmark 點入查看景點內容。
- **AI 反思產生**:反思頁按「Polish with AI」呼叫 **DeepSeek**(OpenAI 相容 API)產生可分享圖說,支援 Copy / Regenerate / Save,含 loading / error 狀態與重試。
- **多語系**:en / zh 透過 AppCompat locale 切換。
- **示範照片匯入**:內建 demo 照片便於展示。

> 各功能完整規格見 [`openspec/changes/`](./openspec/changes/) 下的 change(如 `memory-library-actions`、`ai-reflection-generation`)。

---

## 📚 文件導覽

> 全部文件集中在 [`references/`](./references/) 與 [`CLAUDE.md`](./CLAUDE.md)。
> **新加入專案先讀** §1 → §2 → §3,其他依角色取用。

### §1 需求與設計

| 文件 | 內容 | 何時要看 |
|---|---|---|
| [`references/requirements.md`](./references/requirements.md) | 完整需求規格:Actors、UC1–UC18、FR-01–FR-23、NFR-01–NFR-20、口試回饋對照 | 不確定「要做什麼」時 |
| [`references/architecture.md`](./references/architecture.md) | 系統架構:6 張分層圖(總覽 / Client / 14 個服務 / 資料層 / 外部整合 / 端到端流程)+ 部署選型建議 | 不確定「服務怎麼分」時 |
| [`references/diagram.md`](./references/diagram.md) | Activity Diagram(5 張)+ Domain Class Diagram + System Sequence Diagram(6 張) | 寫某 use case 之前 |

### §2 開發流程與排程

| 文件 | 內容 | 何時要看 |
|---|---|---|
| [`references/schedule.md`](./references/schedule.md) | **6 週 agile sprint 進度表**:Backlog(13 Epic / 62 Story)、各 sprint goal、容量配置、Demo Script、Risk Register | 不確定「現在該做什麼」時 |
| [`references/agile-guide.md`](./references/agile-guide.md) | **Agile / Scrum 入門指南**:角色、儀式流程、Story Points、DoR/DoD、GitHub Project 操作、FAQ | 第一次跑 agile / 卡在某個儀式 |

### §3 開發準則

| 文件 | 內容 | 何時要看 |
|---|---|---|
| [`CLAUDE.md`](./CLAUDE.md) | 編碼規範(Kotlin / Android / React / Python)、測試 / Git / CI / CD / 部署 / 安全 / 命令速查 / 與 AI 協作原則 | **每天會用** — 寫程式前 / push 前 / 部署前 |

### §4 進行中的工作

- **GitHub Project**:[MCIS Sprint Board](https://github.com/users/killin0415/projects/1) — 看本週承諾、卡點、進度
- **Issues**:[killin0415/memoir/issues](https://github.com/killin0415/memoir/issues) — 62 張 backlog ticket(對應 `schedule.md` §5)

---

## 🛠 技術棧速查

> 完整選型理由與替代方案見 [`CLAUDE.md`](./CLAUDE.md) §1.1 & §2。**核心原則:免費 / 自架 / 開源優先。**

| 層 | 技術 |
|---|---|
| **Backend** _(規劃中)_ | Kotlin + Spring Boot(JDK 21)+ Gradle Kotlin DSL,PostgreSQL + Redis,Flyway,SpringDoc OpenAPI |
| **Mobile**(★ 已實作) | 原生 Android(Kotlin + Jetpack Compose),Navigation3、Room + KSP、DataStore、CameraX;AI 反思經 `com.aallam.openai` client + Ktor CIO 呼叫 **DeepSeek**(OpenAI 相容)。JDK 11 / compileSdk 36 / minSdk 24 |
| **AI Services** _(規劃中)_ | Python 3.11+,uv 環境管理,Gemini API(runtime)+ 訂閱版 LLM(build-time 內容)+ Ollama(fallback)。目前 app 端反思直接呼叫 DeepSeek,尚未抽出獨立服務 / proxy |
| **Kiosk** _(規劃中)_ | 瀏覽器 SPA(Next.js / Vite) |
| **Admin CMS** _(規劃中)_ | Vite + React + TypeScript + react-admin / Refine,TanStack Query + Zustand |
| **CI/CD** | GitHub Actions(目前有 `mobile-ci.yml`:assemble / unit test / lint)。Docker + Kubernetes(自建 k3s 為主、AKS 備援)+ Kustomize / Helm _(規劃中)_ |
| **觀測性** _(規劃中)_ | Prometheus + Grafana + Loki + GlitchTip(Sentry 開源版,全部自架) |
| **地圖** _(規劃中)_ | OSM + Leaflet/MapLibre + 台灣 PTX/TDX(免費) |

---

## 🚀 Quick Start

### Android app(目前唯一可跑的元件)

```bash
# 1. 設定 DeepSeek API key(AI 反思功能需要;沒設也能建置,只是 AI 反思會回報錯誤)
cd frontend/mobile
cp local.properties.example local.properties
# 編輯 local.properties,把 DEEPSEEK_API_KEY 換成你的 key(此檔已 gitignore,不會被 commit)

# 2. 建置 / 安裝到裝置或模擬器(用 Gradle CLI,不依賴 Android Studio)
./gradlew :app:installDebug      # 安裝 debug 版
# 或只打包不安裝:./gradlew :app:assembleDebug

# 3. 測試與 lint
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug
```

> 環境需求:JDK 11、Android SDK(compileSdk 36 / minSdk 24)。CI 流程見 [`.github/workflows/mobile-ci.yml`](./.github/workflows/mobile-ci.yml)。

### 其他元件(規劃中)

backend / ai-services / kiosk / admin 等服務尚未建立。原本規劃的本地起法(Docker compose、`bootRun`、`pnpm dev`、`uv run` 等)見 [`CLAUDE.md`](./CLAUDE.md) §10,實際以各資料夾建立後的說明為準。

---

## 📁 專案結構

```
memoir/
├── frontend/
│   └── mobile/        # ★ 原生 Android app(Kotlin + Compose)— 目前唯一已實作
├── data/              # 內容資料(tainan-route 等),建置時掛載為 app assets
├── references/        # ★ 需求 / 架構 / 排程 / 流程文件
├── openspec/          # OpenSpec change 規格(各功能的提案 / 設計 / 任務)
├── docs/              # 其他文件
├── deprecated/        # 已淘汰的內容
├── .github/workflows/ # GitHub Actions CI(mobile-ci.yml)
├── .claude/           # Claude Code 設定 + memory(隨 git 同步)
├── CLAUDE.md          # ★ 開發準則(必讀)
└── README.md          # 本文件
```

> **規劃中(尚未建立)**:`backend/`、`ai-services/`、`kiosk/`、`admin/`、`deploy/`、`docker/` — 第一次需要時補上。

---

## 👥 團隊

- **設計組(2 人)**:D1(Lead Designer / PO)+ D2(Content / UX Researcher)
- **系統組(4–6 人)**:S1(Backend Lead / SM / Tech Lead)+ S2 ~ S6(Backend / Android / AI / Admin / DevOps)

詳細分工見 [`schedule.md`](./references/schedule.md) §3。

---

## 📜 授權

見 [`LICENSE`](./LICENSE)。

---

## 🔗 相關連結

- **Repo**:https://github.com/killin0415/memoir
- **Project Board**:https://github.com/users/killin0415/projects/1
- **Issues**:https://github.com/killin0415/memoir/issues
