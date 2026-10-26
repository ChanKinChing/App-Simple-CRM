# Simple CRM 開發計劃

專案名稱：App-Simple-CRM-1.0.0
目標：以 Spring Boot 3.5.16 + PostgreSQL 17 建立中小企業客戶管理系統，作為 Java 後端求職作品集。
學習者背景：Java 有基礎、無 Spring Boot 實戰。

## 技術決策（已定案）

| 項目 | 決策 | 理由 |
| --- | --- | --- |
| Spring Boot | 3.5.16 | 與 HK 企業現行版本、教材生態對齊 |
| Java | 17（Temurin） | Boot 3.5 支援；本機已安裝 |
| 建置工具 | Maven Wrapper（mvnw） | 不需全域安裝 Maven |
| 資料庫 | PostgreSQL 17.11（可攜版） | 免管理員權限、免 Windows 服務 |
| 認證 | Spring Security + JWT（OAuth2 Resource Server） | Spring 官方路線，不手寫 filter |
| 前端 | React + Vite（不安裝 HTML 骨架階段） | 節省時間，CORS 概念在 React 階段學 |
| CI | GitHub Actions + 本機 Jenkins（Docker） | Jenkins 僅為履歷展示 |
| IDE | VS Code + Spring Boot Extension Pack | 使用者選擇 |
| 加分項 | 不做樂觀鎖／Spring Event／效能分析 | 先完成 MVP |

## 階段與預估

| 階段 | 內容 | 執行者 | 預估 |
| --- | --- | --- | --- |
| 0 | 環境修復、專案骨架、可啟動驗證 | AI | 1.5–2 h |
| 1 | spring.io 10 個 guide | 使用者 | 12–18 h |
| 2 | OpenAPI 契約、DDL、命名規範 | AI | 1–1.5 h |
| 3 | 後端分層 CRUD、驗證、全域異常、分頁搜尋 | AI | 5–6 h |
| 4 | Spring Security + JWT + 角色 | AI | 2.5–3.5 h |
| 5 | JUnit + MockMvc + 整合測試 | AI | 2.5–3 h |
| 6 | React 四頁（登入／列表／詳情／表單） | AI | 6–8 h |
| 7 | Jenkins（Docker 8081）+ GitHub Actions | AI | 2–3 h |
| 8 | Dockerfile、compose、README、Swagger UI | AI | 2–3 h |
| 9 | 使用者逐檔讀懂、改寫、錄 demo | 使用者 | 8–12 h |

## Port 分配

| Port | 用途 |
| --- | --- |
| 8080 | Spring Boot |
| 5432 | PostgreSQL |
| 5173 | Vite dev server |
| 8081 | Jenkins（容器） |

## 目錄結構

```
App-Simple-CRM-1.0.0/
├── backend/          Spring Boot（Maven）
├── frontend/         React + Vite
├── docs/             API 契約、DDL、截圖
├── PLAN.md
└── README.md
```

## 已知限制與風險

1. Spring Boot 3.5.x 的 OSS 支援已於 2026 年中結束，不再有新 CVE 修補；本專案為學習用途可接受。
2. 本機 System PATH 的第一個 java 是 Oracle JRE 8（需管理員權限才能移除），因此：
   - 一律使用 `mvnw`（讀 JAVA_HOME）
   - 直接執行 jar 時使用 `%JAVA_HOME%\bin\java.exe`
3. 並行 subagent 上限 2 個（16 GB RAM）。
4. 上傳 GitHub 前必須執行 `git-security-scan.ps1`。
