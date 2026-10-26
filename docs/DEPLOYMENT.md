# 部署與 CI（Docker / Jenkins）

> 狀態（2026-10-27）：Docker Desktop 29.9.2（engine 29.9.2）已安裝，Compose 棧**已實機驗證**（見下方驗證結果）。
> Jenkins 章節仍為待驗證設定。

## 1. Docker Compose

三個服務：`db`（PostgreSQL 17）、`backend`（Spring Boot + JRE 17）、`frontend`（nginx 提供 build 後的靜態檔並反向代理 `/api`）。

```bat
copy .env.example .env
:: 編輯 .env：POSTGRES_PASSWORD、JWT_SECRET 至少改成自己的值
docker compose up -d --build
docker compose ps
```

| 入口 | 網址 |
| --- | --- |
| 前端 | http://localhost:8082 |
| 後端 API | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| 健康檢查 | http://localhost:8080/actuator/health |

停止與清空資料：

```bat
docker compose down
docker compose down -v
```

### 設計說明

- **前端不需要 `VITE_API_BASE_URL`**：`client.js` 的 baseURL 固定 `/api`。dev 由 Vite proxy 轉到 8080，compose 由 nginx `proxy_pass http://backend:8080` 轉發，兩邊都是同源請求，因此不需要 CORS。
- **後端 `depends_on: condition: service_healthy`**：等 `pg_isready` 通過才啟動，避免 Flyway 首次連線失敗。
- **healthcheck**：後端打 `/actuator/health`，前端打 `/`；`docker compose ps` 可直接看到健康狀態。
- **映像檔**：後端 multi-stage（JDK 編譯 → JRE 執行，非 root 使用者）；前端 `npm ci` + `vite build` → nginx:alpine。
- **資料持久化**：named volume `crm-db-data`。

### 驗證結果（2026-10-27 實測）

| 檢查 | 結果 |
| --- | --- |
| `docker compose build` | 兩個映像檔建置成功，共 91 秒（Maven 依賴走容器內快取） |
| `docker compose up -d` → `health=UP` | 42 秒；`db` healthy、`backend` healthy、`frontend` healthy |
| 前端 `GET /` | 200，含 App 入口 |
| SPA fallback `GET /customers/1` | 200，回 `index.html` |
| nginx `/api` 反向代理登入 | 200，`role=ADMIN` |
| 經代理分頁查詢 | `totalElements=20`、`size=5` |
| 經代理報表摘要 | 200 |
| 未帶 token 存取 | 401 |
| 後端執行身分 | `uid=100(app)`，非 root |
| 容器內 `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db:5432/simplecrm` |

尚待人工確認（非阻斷）：`compose down` 後再 `up -d` 的 volume 資料保留、瀏覽器內完整操作流程（本次以 HTTP 契約驗證，未開瀏覽器）。

## 2. Jenkins（本機，port 8081）

Jenkins 僅為履歷展示，跑在容器裡並透過 Docker socket 啟動 build agent 容器。

```bat
docker run -d --name jenkins -p 8081:8080 -p 50000:50000 ^
  -v jenkins_home:/var/jenkins_home ^
  -v //var/run/docker.sock:/var/run/docker.sock ^
  --user root ^
  jenkins/jenkins:lts-jdk17

:: 初始管理員密碼
docker exec jenkins cat /var/jenkins_home/secrets/initialAdminPassword
```

首次登入後安裝外掛：**Docker Pipeline**、**JUnit**、**Git**（其餘用預設即可）。

建立 Pipeline job → Pipeline script from SCM（或直接貼上 repo 根目錄的 `Jenkinsfile`），然後 Build。

### Jenkinsfile 重點

| 項目 | 值 | 原因 |
| --- | --- | --- |
| 後端 agent | `maven:3.9-eclipse-temurin-17` | 不必在主機安裝 Maven／JDK |
| 前端 agent | `node:22-alpine` | 與 GitHub Actions 版本一致 |
| DB | `host.docker.internal:5432/simplecrm_test` | 重用本機可攜版 PostgreSQL；測試 profile 允許用 `TEST_DATABASE_*` 覆寫 |
| `-v jenkins-m2:/root/.m2` | Maven 依賴快取 | 第二次建置不用重新下載 |

### 已知風險

- `--user root` + 掛載 Docker socket 等於把主機 root 權限交給 Jenkins，只適合本機示範，**不要**用在公開伺服器。
- Jenkins 容器需能解析 `host.docker.internal`（Docker Desktop for Windows 預設可以）。
- 本機 8081 給 Jenkins、8080 給後端，兩者不衝突；若 Jenkins 也開 8080 會撞後端。

## 3. GitHub Actions

`.github/workflows/ci.yml`，推上 GitHub 後自動執行：

- backend：`postgres:17-alpine` service → `bash mvnw -B verify`（32 測試）→ 上傳 surefire 報告
- frontend：`npm ci` → `npm run lint` → `npm run build` → 上傳 `dist`

注意 `mvnw` 必須保持可執行權限（repo 內已設為 `100755`）；workflow 用 `bash mvnw` 呼叫，即使檔案權限遺失也能執行。
