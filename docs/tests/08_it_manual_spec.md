# 結合テスト仕様書

| 項目 | 内容 |
|------|------|
| 文書番号 | IT-SPEC-001 |
| 作成日 | 2026-04-26 |
| 対象システム | 家族スケジュール共有システム（Spring Boot 3.3.4） |
| ステータス | ドラフト |

---

## 目次

1. [テスト方針](#1-テスト方針)
2. [環境セットアップ](#2-環境セットアップ)
3. [テスト観点一覧](#3-テスト観点一覧)
4. [メンバー管理テストケース](#4-メンバー管理テストケース)
5. [スケジュールテストケース](#5-スケジュールテストケース)
6. [シナリオフローテスト](#6-シナリオフローテスト)
7. [エラーレスポンスフォーマット確認リスト](#7-エラーレスポンスフォーマット確認リスト)
8. [既知バグ BUG-VALIDATOR 専用テスト](#8-既知バグ-bug-validator-専用テスト)

---

## 1. テスト方針

### 1.1 目的

REST API の各エンドポイントについて、正常系・異常系・境界値を網羅的に検証し、  
サービス全体が設計仕様どおりに動作することを確認する。  
あわせて、既知バグ **BUG-VALIDATOR**（`ScheduleValidator.VALID_MEMBER_IDS` のハードコード）を  
テストとして記録し、再現手順を明確に残す。

### 1.2 スコープ

**対象（In Scope）**

- メンバー管理 API（`/api/members`）
- スケジュール管理 API（`/api/schedules`）
- 論理削除・復元・物理削除の一連フロー
- バリデーションエラー（400）・Not Found（404）・競合エラー（409）

**対象外（Out of Scope）

- 認証・認可
- フロントエンド UI
- 負荷テスト・性能テスト

### 1.3 合格基準

- 全テストケースで期待 HTTP ステータスコードが一致すること
- JSON レスポンスのフィールド・値が仕様と一致すること
- BUG-VALIDATOR 関連テスト（IT-S-11、IT-SC-02）は **FAIL（既知バグ）** として記録し、合格対象外とする

### 1.4 環境前提

| 項目 | 値 |
|------|-----|
| ベース URL | `http://localhost:8080` |
| DB | H2 インメモリ（`spring.datasource.url=jdbc:h2:mem:familydb`） |
| 初期データ | members テーブルに ID=1〜5 の 5 名、schedules テーブルは空 |
| テスト実行ツール | curl（コマンドライン） |

---

## 2. 環境セットアップ

### 2.1 サーバー起動

```bash
# プロジェクトルートで実行
./mvnw spring-boot:run

# または JAR を直接実行する場合
java -jar target/family-schedule-*.jar
```

起動確認:

```bash
curl -s http://localhost:8080/api/members | jq .
```

5 名のメンバー一覧が返れば起動成功。

### 2.2 初期データ確認

```bash
# メンバー一覧取得（5名・displayOrder昇順で返ること）
curl -s http://localhost:8080/api/members

# 期待レスポンス（抜粋）
# [
#   {"id":1,"name":"お父さん","displayOrder":1},
#   {"id":2,"name":"お母さん","displayOrder":2},
#   {"id":3,"name":"長女","displayOrder":3},
#   {"id":4,"name":"次女","displayOrder":4},
#   {"id":5,"name":"長男","displayOrder":5}
# ]
```

### 2.3 H2 コンソール

ブラウザで `http://localhost:8080/h2-console` にアクセスする。

| 項目 | 値 |
|------|-----|
| JDBC URL | `jdbc:h2:mem:familydb` |
| ユーザー名 | `sa` |
| パスワード | （空白） |

よく使う確認 SQL:

```sql
-- メンバー一覧
SELECT * FROM members ORDER BY display_order;

-- スケジュール一覧（論理削除含む）
SELECT * FROM schedules ORDER BY date, member_id, id;

-- 論理削除済みスケジュール
SELECT * FROM schedules WHERE deleted_at IS NOT NULL;
```

### 2.4 テスト前のリセット方法

H2 インメモリ DB のため、**サーバーを再起動すると初期状態に戻る**。  
テストケースによっては前のテストの副作用を避けるためにサーバー再起動を推奨する。  
各テストケースの「前提条件」欄に再起動が必要な場合は明記する。

---

## 3. テスト観点一覧

### 3.1 メンバー管理

| テストケース ID | エンドポイント | 観点 | 期待ステータス | 優先度 |
|---------------|-------------|------|-------------|------|
| IT-M-01 | GET /api/members | 一覧取得・displayOrder 昇順 | 200 | H |
| IT-M-02 | POST /api/members | 正常登録・DB 確認 | 201 | H |
| IT-M-03 | POST /api/members | 10 名上限超過 | 400 | M |
| IT-M-04 | POST /api/members | 重複名 | 400 | M |
| IT-M-05 | POST /api/members | 名前空文字 | 400 | H |
| IT-M-06 | PUT /api/members/{id} | 正常更新 | 200 | H |
| IT-M-07 | PUT /api/members/{id} | 存在しない ID | 404 | H |
| IT-M-08 | DELETE /api/members/{id} | 予定なし・正常削除 | 204 | H |
| IT-M-09 | DELETE /api/members/{id} | 予定あり・制約違反 | 409 | H |

### 3.2 スケジュール管理

| テストケース ID | エンドポイント | 観点 | 期待ステータス | 優先度 |
|---------------|-------------|------|-------------|------|
| IT-S-01 | GET /api/schedules | 0 件 | 200 | H |
| IT-S-02 | GET /api/schedules | 複数件・ソート確認 | 200 | H |
| IT-S-03 | GET /api/schedules | from > to | 400 | M |
| IT-S-04 | GET /api/schedules | from パラメータ欠落 | 400 | M |
| IT-S-05 | GET /api/schedules | 不正日付形式 | 400 | M |
| IT-S-06 | GET /api/schedules | 論理削除済みは除外 | 200 | H |
| IT-S-07 | POST /api/schedules | 正常登録・Location ヘッダー確認 | 201 | H |
| IT-S-08 | POST /api/schedules | content ちょうど 100 コードポイント | 201 | H |
| IT-S-09 | POST /api/schedules | content 101 コードポイント | 400 | H |
| IT-S-10 | POST /api/schedules | 絵文字 100 コードポイント | 201 | M |
| IT-S-11 | POST /api/schedules | memberId=6（BUG-VALIDATOR） | 400 | H |
| IT-S-12 | POST /api/schedules | memberId=5（上限は問題なし） | 201 | M |
| IT-S-13 | PUT /api/schedules/{id} | 正常更新 | 200 | H |
| IT-S-14 | PUT /api/schedules/{id} | 存在しない ID | 404 | H |
| IT-S-15 | PUT /api/schedules/{id} | 論理削除済み ID | 404 | H |
| IT-S-16 | DELETE /api/schedules/{id} | 論理削除（deleted_at 設定） | 204 | H |
| IT-S-17 | DELETE /api/schedules/{id} | 2 回目の論理削除 | 404 | H |
| IT-S-18 | POST /api/schedules/{id}/restore | 論理削除からの復元 | 200 | H |
| IT-S-19 | POST /api/schedules/{id}/restore | 未削除 ID に restore | 404 | H |
| IT-S-20 | POST /api/schedules/{id}/purge | 物理削除 | 204 | H |
| IT-S-21 | POST /api/schedules/{id}/purge | 未削除 ID に purge | 404 | H |

### 3.3 シナリオフロー

| テストケース ID | 観点 | 優先度 |
|---------------|------|------|
| IT-SC-01 | 登録→一覧→削除→復元→purge の一気通貫フロー | H |
| IT-SC-02 | メンバー追加（ID=6）→予定登録 → BUG-VALIDATOR 再現 | H |
| IT-SC-03 | 閏日 2028-02-29 登録 → 201 | M |
| IT-SC-04 | 存在しない閏日 2027-02-29 登録 → 400 | M |

---

## 4. メンバー管理テストケース

### IT-M-01 メンバー一覧取得: 200 + displayOrder 昇順

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-01 |
| 前提条件 | 初期データ（5 名）が存在する |
| 優先度 | H |

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | GET /api/members | 200 + JSON 配列（5 要素、id=1〜5 の順） |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  http://localhost:8080/api/members
```

**期待レスポンス:**

- HTTP ステータス: `200 OK`
- `Content-Type: application/json`
- ボディ: `id` が 1, 2, 3, 4, 5 の順（`displayOrder` 昇順）

```json
[
  {"id":1,"name":"お父さん","displayOrder":1},
  {"id":2,"name":"お母さん","displayOrder":2},
  {"id":3,"name":"長女","displayOrder":3},
  {"id":4,"name":"次女","displayOrder":4},
  {"id":5,"name":"長男","displayOrder":5}
]
```

---

### IT-M-02 メンバー登録: 正常 → 201 + DB 確認

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-02 |
| 前提条件 | 初期データ（5 名）が存在する |
| 優先度 | H |

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | POST /api/members `{"name":"おじいちゃん"}` | 201 + `{"id":6,"name":"おじいちゃん","displayOrder":6}` |
| 2 | H2 コンソールで `SELECT * FROM members WHERE id=6` | 1 行ヒット |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"おじいちゃん"}'
```

**期待レスポンス:**

- HTTP ステータス: `201 Created`

```json
{"id":6,"name":"おじいちゃん","displayOrder":6}
```

**DB 確認 SQL（H2 コンソール）:**

```sql
SELECT * FROM members WHERE name = 'おじいちゃん';
```

---

### IT-M-03 メンバー登録: 10 名上限超過 → 400

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-03 |
| 前提条件 | メンバーが 10 名登録済みの状態（初期 5 名 + 追加 5 名） |
| 優先度 | M |

**前提セットアップ（5 名追加）:**

```bash
for name in "おじいちゃん" "おばあちゃん" "叔父さん" "叔母さん" "いとこ"; do
  curl -s -X POST http://localhost:8080/api/members \
    -H "Content-Type: application/json" \
    -d "{\"name\":\"${name}\"}"
done
```

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | POST /api/members（11 人目） | 400 + `error: "VALIDATION"` |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"11人目"}'
```

**期待レスポンス:**

- HTTP ステータス: `400 Bad Request`

```json
{
  "error": "VALIDATION",
  "message": "入力に誤りがあります"
}
```

---

### IT-M-04 メンバー登録: 重複名 → 400 + fields.name

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-04 |
| 前提条件 | 初期データ（5 名）が存在する |
| 優先度 | M |

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | POST /api/members `{"name":"お父さん"}` | 400 + `fields.name` にエラーメッセージ |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"お父さん"}'
```

**期待レスポンス:**

- HTTP ステータス: `400 Bad Request`

```json
{
  "error": "VALIDATION",
  "message": "入力に誤りがあります",
  "fields": {
    "name": "既に使用されている名前です"
  }
}
```

---

### IT-M-05 メンバー登録: 名前空文字 → 400

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-05 |
| 前提条件 | 初期データ（5 名）が存在する |
| 優先度 | H |

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | POST /api/members `{"name":""}` | 400 + `fields.name` にエラーメッセージ |

**curl コマンド（空文字）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":""}'
```

**curl コマンド（null）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":null}'
```

**期待レスポンス:**

- HTTP ステータス: `400 Bad Request`

```json
{
  "error": "VALIDATION",
  "message": "入力に誤りがあります",
  "fields": {
    "name": "名前は必須です"
  }
}
```

**追加確認（名前 21 文字以上）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"あいうえおかきくけこさしすせそたちつてとな"}'
```

期待: `400 Bad Request`（`name.length() > 20` のため）

---

### IT-M-06 メンバー更新: 正常 → 200

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-06 |
| 前提条件 | 初期データ（5 名）が存在する |
| 優先度 | H |

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | PUT /api/members/1 `{"name":"パパ"}` | 200 + `{"id":1,"name":"パパ","displayOrder":1}` |
| 2 | GET /api/members | id=1 の name が「パパ」に変わっている |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X PUT http://localhost:8080/api/members/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"パパ"}'
```

**期待レスポンス:**

- HTTP ステータス: `200 OK`

```json
{"id":1,"name":"パパ","displayOrder":1}
```

---

### IT-M-07 メンバー更新: 存在しない ID → 404

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-07 |
| 前提条件 | ID=999 のメンバーが存在しない |
| 優先度 | H |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X PUT http://localhost:8080/api/members/999 \
  -H "Content-Type: application/json" \
  -d '{"name":"存在しない"}'
```

**期待レスポンス:**

- HTTP ステータス: `404 Not Found`

```json
{
  "error": "NOT_FOUND",
  "message": "指定されたメンバーが見つかりません"
}
```

---

### IT-M-08 メンバー削除: 予定なし → 204

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-08 |
| 前提条件 | ID=5（長男）に予定が紐付いていない |
| 優先度 | H |

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | DELETE /api/members/5 | 204 No Content（ボディなし） |
| 2 | GET /api/members | 5 名から 4 名に減っている |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X DELETE http://localhost:8080/api/members/5
```

**期待レスポンス:**

- HTTP ステータス: `204 No Content`
- ボディ: なし

**DB 確認 SQL:**

```sql
SELECT * FROM members WHERE id = 5;
-- 0 行であること
```

---

### IT-M-09 メンバー削除: 予定あり → 409（ON DELETE RESTRICT）

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-M-09 |
| 前提条件 | ID=1（お父さん）に予定が 1 件以上紐付いている |
| 優先度 | H |

**前提セットアップ（予定を登録）:**

```bash
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
```

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 上記で予定を登録 | 201 |
| 2 | DELETE /api/members/1 | 409（ON DELETE RESTRICT による DataIntegrityViolationException） |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X DELETE http://localhost:8080/api/members/1
```

**期待レスポンス:**

- HTTP ステータス: `409 Conflict`

```json
{
  "error": "CONFLICT",
  "message": "このメンバーには予定が紐付いているため削除できません"
}
```

---

## 5. スケジュールテストケース

### IT-S-01 スケジュール一覧: 0 件 → 200 + []

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-01 |
| 前提条件 | schedules テーブルが空（サーバー起動直後） |
| 優先度 | H |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "http://localhost:8080/api/schedules?from=2026-01-01&to=2026-12-31"
```

**期待レスポンス:**

- HTTP ステータス: `200 OK`
- ボディ: `[]`

---

### IT-S-02 スケジュール一覧: 複数件 → date 昇順→memberId 昇順→id 昇順

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-02 |
| 前提条件 | 以下の 3 件が登録済み |
| 優先度 | H |

**前提セットアップ:**

```bash
# 同じ日付・異なるメンバー
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":3,"date":"2026-05-01","content":"学校"}'

curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'

# 異なる日付
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":2,"date":"2026-04-30","content":"買い物"}'
```

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "http://localhost:8080/api/schedules?from=2026-04-01&to=2026-05-31"
```

**期待レスポンス:**

- HTTP ステータス: `200 OK`
- ソート順: `date` 昇順 → `memberId` 昇順 → `id` 昇順

```json
[
  {"id":3,"memberId":2,"date":"2026-04-30","content":"買い物"},
  {"id":2,"memberId":1,"date":"2026-05-01","content":"出張"},
  {"id":1,"memberId":3,"date":"2026-05-01","content":"学校"}
]
```

---

### IT-S-03 スケジュール一覧: from > to → 400

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-03 |
| 前提条件 | なし |
| 優先度 | M |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "http://localhost:8080/api/schedules?from=2026-12-31&to=2026-01-01"
```

**期待レスポンス:**

- HTTP ステータス: `400 Bad Request`

```json
{
  "error": "VALIDATION",
  "message": "入力に誤りがあります"
}
```

---

### IT-S-04 スケジュール一覧: from パラメータ欠落 → 400

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-04 |
| 前提条件 | なし |
| 優先度 | M |

**curl コマンド（from のみ欠落）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "http://localhost:8080/api/schedules?to=2026-12-31"
```

**curl コマンド（両方欠落）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "http://localhost:8080/api/schedules"
```

**期待レスポンス:**

- HTTP ステータス: `400 Bad Request`

---

### IT-S-05 スケジュール一覧: 不正日付形式 → 400

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-05 |
| 前提条件 | なし |
| 優先度 | M |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "http://localhost:8080/api/schedules?from=2026/04/01&to=2026-12-31"
```

**期待レスポンス:**

- HTTP ステータス: `400 Bad Request`

---

### IT-S-06 スケジュール一覧: 論理削除済みは除外される

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-06 |
| 前提条件 | なし |
| 優先度 | H |

**手順:**

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | POST /api/schedules（1 件登録） | 201 + id=X |
| 2 | DELETE /api/schedules/X | 204（論理削除） |
| 3 | GET /api/schedules（同じ期間） | 200 + []（削除済みは含まない） |

**curl コマンド:**

```bash
# ステップ1: 登録
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"テスト予定"}'

# ステップ2: 論理削除（ID は上で返ってきた値に置き換える）
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X DELETE http://localhost:8080/api/schedules/1

# ステップ3: 一覧取得
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  "http://localhost:8080/api/schedules?from=2026-05-01&to=2026-05-01"
```

**期待レスポンス（ステップ 3）:**

- HTTP ステータス: `200 OK`
- ボディ: `[]`

**DB 確認 SQL:**

```sql
-- deleted_at が設定されていること
SELECT id, content, deleted_at FROM schedules WHERE id = 1;
```

---

### IT-S-07 スケジュール登録: 正常 → 201 + Location ヘッダー

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-07 |
| 前提条件 | 初期データが存在する |
| 優先度 | H |

**curl コマンド（ヘッダーも表示）:**

```bash
curl -s -D - -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
```

**期待レスポンス:**

- HTTP ステータス: `201 Created`
- `Location` ヘッダー: `/api/schedules/{新規ID}` が含まれること

```json
{"id":1,"memberId":1,"date":"2026-05-01","content":"出張","deletedAt":null}
```

---

### IT-S-08 スケジュール登録: content ちょうど 100 コードポイント → 201

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-08 |
| 前提条件 | 初期データが存在する |
| 優先度 | H |

**curl コマンド（「あ」×100 文字 = 100 コードポイント）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"ああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああ"}'
```

> 注意: 上記の「あ」の文字数が 100 個であることを確認してから実行すること。

**期待レスポンス:**

- HTTP ステータス: `201 Created`

---

### IT-S-09 スケジュール登録: content 101 コードポイント → 400

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-09 |
| 前提条件 | 初期データが存在する |
| 優先度 | H |

**curl コマンド（「あ」×101 文字 = 101 コードポイント）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"あああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああああい"}'
```

> 注意: 上記は「あ」100 個 + 「い」1 個 = 101 コードポイント。

**期待レスポンス:**

- HTTP ステータス: `400 Bad Request`

```json
{
  "error": "VALIDATION",
  "message": "入力に誤りがあります",
  "fields": {
    "content": "内容は100文字以内で入力してください"
  }
}
```

---

### IT-S-10 スケジュール登録: 絵文字 100 コードポイント → 201

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-10 |
| 前提条件 | 初期データが存在する |
| 優先度 | M |

**説明:**

`content` のバリデーションは **コードポイント数**で計測される（`String.codePointCount` または相当）。  
絵文字はサロゲートペア（UTF-16 で 2 char）だが、1 コードポイントとして数える。  
😀（U+1F600）×100 個 = 100 コードポイント → 201 になるべき。

**curl コマンド:**

```bash
python3 -c "import json; print(json.dumps({'memberId':1,'date':'2026-05-10','content':'😀'*100}))" | \
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d @-
```

または直接指定:

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  --data-binary $'{"memberId":1,"date":"2026-05-10","content":"😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀😀"}'
```

> 注意: 上記 😀 は 100 個であることを確認すること。

**期待レスポンス:**

- HTTP ステータス: `201 Created`

---

### IT-S-11 スケジュール登録: memberId=6 → 400（BUG-VALIDATOR）

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-11 |
| バグ番号 | BUG-VALIDATOR |
| 前提条件 | API でメンバーを 1 名追加済み（ID=6 が存在する） |
| 優先度 | H |
| 備考 | **これは既知バグ**。詳細は [セクション 8](#8-既知バグ-bug-validator-専用テスト) 参照 |

**手順:**

| ステップ | curlコマンド | 期待結果 |
|---------|------------|---------|
| 1 | POST /api/members `{"name":"おじいちゃん"}` | 201 + id=6 |
| 2 | POST /api/schedules `{"memberId":6,"date":"2026-04-26","content":"散歩"}` | 400 + fields.memberId="不正なメンバーです" |

**curl コマンド（ステップ 1）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"おじいちゃん"}'
```

**curl コマンド（ステップ 2）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":6,"date":"2026-04-26","content":"散歩"}'
```

**期待レスポンス（ステップ 2）:**

- HTTP ステータス: `400 Bad Request`

```json
{
  "error": "VALIDATION",
  "message": "入力に誤りがあります",
  "fields": {
    "memberId": "不正なメンバーです"
  }
}
```

---

### IT-S-12 スケジュール登録: memberId=5 → 201（上限の 5 は問題なし）

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-12 |
| 前提条件 | 初期データ（5 名）が存在する |
| 優先度 | M |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":5,"date":"2026-05-01","content":"部活"}'
```

**期待レスポンス:**

- HTTP ステータス: `201 Created`

```json
{"id":1,"memberId":5,"date":"2026-05-01","content":"部活","deletedAt":null}
```

---

### IT-S-13 スケジュール更新: 正常 → 200

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-13 |
| 前提条件 | ID=1 のスケジュールが登録済み |
| 優先度 | H |

**前提セットアップ:**

```bash
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
```

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X PUT http://localhost:8080/api/schedules/1 \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-02","content":"出張（変更）"}'
```

**期待レスポンス:**

- HTTP ステータス: `200 OK`

```json
{"id":1,"memberId":1,"date":"2026-05-02","content":"出張（変更）","deletedAt":null}
```

---

### IT-S-14 スケジュール更新: 存在しない ID → 404

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-14 |
| 前提条件 | ID=999 のスケジュールが存在しない |
| 優先度 | H |

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X PUT http://localhost:8080/api/schedules/999 \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"存在しない"}'
```

**期待レスポンス:**

- HTTP ステータス: `404 Not Found`

```json
{
  "error": "NOT_FOUND",
  "message": "指定された予定が見つかりません"
}
```

---

### IT-S-15 スケジュール更新: 論理削除済み ID → 404

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-15 |
| 前提条件 | ID=1 のスケジュールが論理削除済み |
| 優先度 | H |

**前提セットアップ:**

```bash
# 登録
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'

# 論理削除
curl -s -X DELETE http://localhost:8080/api/schedules/1
```

**curl コマンド（論理削除済みに更新を試みる）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X PUT http://localhost:8080/api/schedules/1 \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"更新しようとする"}'
```

**期待レスポンス:**

- HTTP ステータス: `404 Not Found`

```json
{
  "error": "NOT_FOUND",
  "message": "指定された予定が見つかりません"
}
```

---

### IT-S-16 スケジュール削除: 論理削除 → 204（deleted_at 設定）

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-16 |
| 前提条件 | ID=1 のスケジュールが存在し、deleted_at が null |
| 優先度 | H |

**説明:**

`DELETE /api/schedules/{id}` は**論理削除**である。  
DB の `deleted_at` カラムに削除日時が設定されるが、レコード自体は残る。  
（物理削除は `POST /api/schedules/{id}/purge` で行う。IT-S-20 参照。）

**前提セットアップ:**

```bash
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
```

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X DELETE http://localhost:8080/api/schedules/1
```

**期待レスポンス:**

- HTTP ステータス: `204 No Content`
- ボディ: なし

**DB 確認 SQL（論理削除の確認）:**

```sql
-- deleted_at が NULL 以外の値になっていること（物理削除ではない）
SELECT id, content, deleted_at FROM schedules WHERE id = 1;
-- → deleted_at に日時が入っていること
```

---

### IT-S-17 スケジュール削除: 2 回目の論理削除 → 404

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-17 |
| 前提条件 | ID=1 のスケジュールが既に論理削除済み |
| 優先度 | H |

**前提セットアップ（IT-S-16 の状態を継続）:**

```bash
# 1 回目の削除（論理削除）
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
curl -s -X DELETE http://localhost:8080/api/schedules/1
```

**curl コマンド（2 回目の論理削除）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X DELETE http://localhost:8080/api/schedules/1
```

**期待レスポンス:**

- HTTP ステータス: `404 Not Found`

```json
{
  "error": "NOT_FOUND",
  "message": "指定された予定が見つかりません"
}
```

---

### IT-S-18 スケジュール復元: 論理削除から復元 → 200

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-18 |
| 前提条件 | ID=1 のスケジュールが論理削除済み |
| 優先度 | H |

**説明:**

`POST /api/schedules/{id}/restore` は論理削除された予定を復元する。  
`deleted_at` が `null` に戻り、一覧取得で再び表示される。

**前提セットアップ:**

```bash
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
curl -s -X DELETE http://localhost:8080/api/schedules/1
```

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules/1/restore
```

**期待レスポンス:**

- HTTP ステータス: `200 OK`

```json
{"id":1,"memberId":1,"date":"2026-05-01","content":"出張","deletedAt":null}
```

**DB 確認 SQL:**

```sql
-- deleted_at が NULL に戻っていること
SELECT id, content, deleted_at FROM schedules WHERE id = 1;
```

---

### IT-S-19 スケジュール復元: 未削除 ID に restore → 404

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-19 |
| 前提条件 | ID=1 のスケジュールが存在し、deleted_at が null（未削除状態） |
| 優先度 | H |

**前提セットアップ:**

```bash
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
```

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules/1/restore
```

**期待レスポンス:**

- HTTP ステータス: `404 Not Found`

```json
{
  "error": "NOT_FOUND",
  "message": "指定された予定が見つかりません（論理削除済みではありません）"
}
```

---

### IT-S-20 スケジュール物理削除: purge → 204

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-20 |
| 前提条件 | ID=1 のスケジュールが論理削除済み |
| 優先度 | H |

**説明:**

`POST /api/schedules/{id}/purge` は**物理削除**である。  
DB からレコードが完全に消去される。  
（論理削除の `DELETE` と異なり、復元不可能。）

**前提セットアップ:**

```bash
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
curl -s -X DELETE http://localhost:8080/api/schedules/1
```

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules/1/purge
```

**期待レスポンス:**

- HTTP ステータス: `204 No Content`
- ボディ: なし

**DB 確認 SQL（物理削除の確認）:**

```sql
-- レコード自体が存在しないこと（論理削除と違い行ごと消える）
SELECT * FROM schedules WHERE id = 1;
-- → 0 行であること
```

---

### IT-S-21 スケジュール物理削除: 未削除 ID に purge → 404

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-S-21 |
| 前提条件 | ID=1 のスケジュールが存在し、deleted_at が null（未削除状態） |
| 優先度 | H |

**前提セットアップ:**

```bash
curl -s -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"出張"}'
```

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules/1/purge
```

**期待レスポンス:**

- HTTP ステータス: `404 Not Found`

```json
{
  "error": "NOT_FOUND",
  "message": "指定された予定が見つかりません（論理削除済みではありません）"
}
```

---

## 6. シナリオフローテスト

### IT-SC-01 一気通貫フロー: 登録→一覧→削除→復元→purge

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-SC-01 |
| 前提条件 | サーバー起動直後（schedules テーブルが空） |
| 優先度 | H |

**手順（全ステップを順番に実行すること）:**

| ステップ | 操作 | curlコマンド | 期待結果 |
|---------|------|------------|---------|
| 1 | スケジュール登録 | `POST /api/schedules` | 201 + id=1 + Location ヘッダー |
| 2 | 一覧取得（表示確認） | `GET /api/schedules?from=2026-05-01&to=2026-05-01` | 200 + 1 件含む |
| 3 | 論理削除 | `DELETE /api/schedules/1` | 204 |
| 4 | 一覧取得（除外確認） | `GET /api/schedules?from=2026-05-01&to=2026-05-01` | 200 + [] |
| 5 | 復元 | `POST /api/schedules/1/restore` | 200 + deletedAt=null |
| 6 | 一覧取得（復元確認） | `GET /api/schedules?from=2026-05-01&to=2026-05-01` | 200 + 1 件含む |
| 7 | 物理削除 | `POST /api/schedules/1/purge` | 204 |
| 8 | 一覧取得（物理削除確認） | `GET /api/schedules?from=2026-05-01&to=2026-05-01` | 200 + [] |
| 9 | DB 確認 | H2 コンソールで SELECT | 0 行（レコード消滅） |

**実行スクリプト:**

```bash
BASE="http://localhost:8080"

echo "=== ステップ1: スケジュール登録 ==="
curl -s -D - -X POST "$BASE/api/schedules" \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2026-05-01","content":"一気通貫テスト"}'
echo ""

echo "=== ステップ2: 一覧取得（登録確認） ==="
curl -s "$BASE/api/schedules?from=2026-05-01&to=2026-05-01"
echo ""

echo "=== ステップ3: 論理削除 ==="
curl -s -w "HTTP_STATUS:%{http_code}" -X DELETE "$BASE/api/schedules/1"
echo ""

echo "=== ステップ4: 一覧取得（論理削除後 - 空のはず） ==="
curl -s "$BASE/api/schedules?from=2026-05-01&to=2026-05-01"
echo ""

echo "=== ステップ5: 復元 ==="
curl -s -w "HTTP_STATUS:%{http_code}" -X POST "$BASE/api/schedules/1/restore"
echo ""

echo "=== ステップ6: 一覧取得（復元確認） ==="
curl -s "$BASE/api/schedules?from=2026-05-01&to=2026-05-01"
echo ""

echo "=== ステップ7: 物理削除(purge) ==="
curl -s -w "HTTP_STATUS:%{http_code}" -X POST "$BASE/api/schedules/1/purge"
echo ""

echo "=== ステップ8: 一覧取得（物理削除後 - 空のはず） ==="
curl -s "$BASE/api/schedules?from=2026-05-01&to=2026-05-01"
echo ""
```

---

### IT-SC-02 BUG-VALIDATOR 再現: メンバー追加（ID=6）→ 予定登録 → 400

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-SC-02 |
| バグ番号 | BUG-VALIDATOR |
| 前提条件 | 初期データ（5 名）のみ存在する |
| 優先度 | H |
| 期待結果 | **バグにより FAIL**（201 になるべきところが 400 になる） |

**手順:**

| ステップ | 操作 | 期待結果（正しい動作） | 実際の動作（バグ） |
|---------|------|-----------------|--------------|
| 1 | POST /api/members `{"name":"おじいちゃん"}` | 201 + id=6 | 201 + id=6（正常） |
| 2 | POST /api/schedules `{"memberId":6,...}` | 201 Created | **400 Bad Request**（バグ） |

**curl コマンド:**

```bash
echo "=== ステップ1: メンバー追加（ID=6 が払い出されるはず） ==="
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"おじいちゃん"}'
echo ""

echo "=== ステップ2: 追加メンバー（ID=6）で予定登録 → BUG により 400 になる ==="
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":6,"date":"2026-04-26","content":"散歩"}'
echo ""
```

**バグの根本原因:**

`ScheduleValidator.java` の 12 行目:

```java
private static final Set<Integer> VALID_MEMBER_IDS = Set.of(1, 2, 3, 4, 5);
```

有効なメンバー ID がハードコードされており、API でメンバーを追加しても  
バリデーションの許可リストが更新されない。  
**これは既知バグであり、`ScheduleValidator.VALID_MEMBER_IDS` のハードコードが原因。**  
修正は DB からメンバー ID を動的に取得するよう変更する必要がある。

---

### IT-SC-03 閏日 2028-02-29 登録 → 201

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-SC-03 |
| 前提条件 | 初期データが存在する |
| 優先度 | M |

**説明:**

2028 年は閏年（4 の倍数かつ 100 の倍数でない）。  
2028-02-29 は存在する有効な日付のため 201 になること。

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2028-02-29","content":"閏日テスト"}'
```

**期待レスポンス:**

- HTTP ステータス: `201 Created`

```json
{"id":1,"memberId":1,"date":"2028-02-29","content":"閏日テスト","deletedAt":null}
```

---

### IT-SC-04 存在しない閏日 2027-02-29 登録 → 400

| 項目 | 内容 |
|------|------|
| テストケース ID | IT-SC-04 |
| 前提条件 | 初期データが存在する |
| 優先度 | M |

**説明:**

2027 年は閏年ではない（4 の倍数だが 100 の倍数でもなく、ただし 2027 自体は 4 で割り切れない）。  
2027-02-29 は存在しない日付のため 400 になること。

**curl コマンド:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":1,"date":"2027-02-29","content":"存在しない閏日"}'
```

**期待レスポンス:**

- HTTP ステータス: `400 Bad Request`

```json
{
  "error": "VALIDATION",
  "message": "入力に誤りがあります",
  "fields": {
    "date": "無効な日付です"
  }
}
```

---

## 7. エラーレスポンスフォーマット確認リスト

全エラーレスポンスは以下のフォーマットに準拠していること。

### 7.1 標準エラーフォーマット

```json
{
  "error": "<エラーコード>",
  "message": "<説明>",
  "fields": {
    "<フィールド名>": "<フィールド別エラーメッセージ>"
  }
}
```

`fields` は検証エラー（400）の場合のみ含まれる。

### 7.2 HTTP ステータスコード別確認表

| HTTP ステータス | `error` フィールド値 | 発生条件 | 確認テストケース |
|--------------|-----------------|--------|--------------|
| 400 | `VALIDATION` | バリデーションエラー | IT-M-03〜05, IT-S-03〜05, IT-S-09, IT-S-11, IT-SC-04 |
| 404 | `NOT_FOUND` | リソース未存在 | IT-M-07, IT-S-14, IT-S-15, IT-S-17, IT-S-19, IT-S-21 |
| 405 | （任意） | 未対応 HTTP メソッド | 下記 curl で確認 |
| 409 | `CONFLICT` | ON DELETE RESTRICT 違反 | IT-M-09 |
| 415 | （任意） | Content-Type 不正 | 下記 curl で確認 |

### 7.3 405・415 の確認コマンド

**405 確認（PATCH は未対応想定）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X PATCH http://localhost:8080/api/members/1 \
  -H "Content-Type: application/json" \
  -d '{"name":"パパ"}'
```

**415 確認（Content-Type なし）:**

```bash
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/members \
  -d '{"name":"テスト"}'
```

---

## 8. 既知バグ BUG-VALIDATOR 専用テスト

### バグ概要

| 項目 | 内容 |
|------|------|
| バグ番号 | BUG-VALIDATOR |
| 発見箇所 | `ScheduleValidator.java` 12 行目 |
| バグ種別 | ロジックバグ（静的ハードコード） |
| 重大度 | 高（メンバー追加後に予定が登録できない） |
| ステータス | 未修正（既知） |

### バグの詳細説明

`ScheduleValidator.java` の 12 行目において、有効なメンバー ID が以下のように**静的なセット**としてハードコードされている:

```java
private static final Set<Integer> VALID_MEMBER_IDS = Set.of(1, 2, 3, 4, 5);
```

**これは既知バグであり、`ScheduleValidator.VALID_MEMBER_IDS` のハードコードが原因である。**

システムの初期状態では ID が 1〜5 の 5 名のメンバーが存在するため、通常の操作では問題が表面化しない。  
しかし `POST /api/members` でメンバーを追加すると ID=6 以降が払い出されるにもかかわらず、  
`ScheduleValidator` の許可リストには ID=6 以降が含まれていないため、  
ID=6 以降のメンバーを指定した予定登録は常に `400 Bad Request` になる。

### 影響範囲

- `POST /api/schedules` で `memberId` が 6 以上の場合、常に 400 エラー
- `PUT /api/schedules/{id}` で `memberId` を 6 以上に変更する場合も同様に 400 エラー（推定）

### 再現手順（詳細）

```bash
# 手順1: 初期状態確認（メンバー5名のみ）
echo "--- 初期メンバー確認 ---"
curl -s http://localhost:8080/api/members | python3 -m json.tool

# 手順2: 6人目のメンバーを追加
echo "--- 6人目追加 ---"
RESPONSE=$(curl -s -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"おじいちゃん"}')
echo "$RESPONSE"
# {"id":6,"name":"おじいちゃん","displayOrder":6} が返ること

# 手順3: ID=6 のメンバーで予定を登録 → バグ再現
echo "--- ID=6 で予定登録（400 になるはず） ---"
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":6,"date":"2026-04-26","content":"散歩"}'
# 期待（バグなし）: 201 Created
# 実際（バグあり）: 400 {"error":"VALIDATION","fields":{"memberId":"不正なメンバーです"}}

# 手順4: ID=5 のメンバー（初期値内）では問題なく登録できること
echo "--- ID=5 で予定登録（201 になるはず） ---"
curl -s -w "\nHTTP_STATUS:%{http_code}" \
  -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d '{"memberId":5,"date":"2026-04-26","content":"散歩"}'
# 期待: 201 Created（ID=5 は VALID_MEMBER_IDS に含まれるため正常）
```

### 修正方針（参考）

`ScheduleValidator` でメンバー ID の検証を行う際、静的なセットではなく  
`MemberRepository` を通じて DB から動的にメンバー ID を取得するよう変更する。

```java
// 修正前（バグあり）
private static final Set<Integer> VALID_MEMBER_IDS = Set.of(1, 2, 3, 4, 5);

// 修正後（案）
@Autowired
private MemberRepository memberRepository;

private boolean isValidMemberId(Integer memberId) {
    return memberRepository.existsById(memberId);
}
```

### テスト結果記録欄

| テストケース ID | 実行日 | 実行者 | 結果 | 備考 |
|--------------|------|------|------|------|
| IT-S-11 | | | FAIL（既知バグ） | BUG-VALIDATOR |
| IT-S-12 | | | | |
| IT-SC-02 | | | FAIL（既知バグ） | BUG-VALIDATOR |
