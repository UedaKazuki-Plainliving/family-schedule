# API詳細仕様書

- バージョン：v0.1
- ベースURL：`/api`
- 形式：JSON (UTF-8)
- 認証：なし（MVP）
- 日付形式：`YYYY-MM-DD`（ISO-8601）

---

## エンドポイント一覧

| メソッド | パス | 概要 | 関連FR |
|---|---|---|---|
| GET | `/api/members` | 家族メンバー一覧取得 | FR-01, FR-08 |
| GET | `/api/schedules` | 指定日付範囲の予定一覧取得 | FR-03〜06 |
| POST | `/api/schedules` | 予定新規登録 | FR-07〜10, FR-18 |
| PUT | `/api/schedules/{id}` | 予定編集 | FR-11〜12, FR-18 |
| DELETE | `/api/schedules/{id}` | 予定削除 | FR-13 |

---

## 共通

### エラーレスポンス

| HTTP | 意味 | body |
|---|---|---|
| 400 | バリデーションエラー | `{ "error": "VALIDATION", "message": "...", "fields": { "content": "必須" } }` |
| 404 | リソース未存在 | `{ "error": "NOT_FOUND", "message": "schedule not found" }` |
| 500 | 内部エラー | `{ "error": "INTERNAL", "message": "..." }` |

### ID表現

- `member.id`：整数 (1〜5 固定、マスタ)
- `schedule.id`：BIGSERIAL の整数

---

## 1. GET /api/members

家族メンバー一覧を取得。

### リクエスト

パラメータなし。

### レスポンス 200

```json
[
  { "id": 1, "name": "お父さん", "displayOrder": 1 },
  { "id": 2, "name": "お母さん", "displayOrder": 2 },
  { "id": 3, "name": "そよ",     "displayOrder": 3 },
  { "id": 4, "name": "ゆうり",   "displayOrder": 4 },
  { "id": 5, "name": "いちろう", "displayOrder": 5 }
]
```

並び順は `displayOrder` 昇順。

---

## 2. GET /api/schedules

指定日付範囲の予定を一覧取得。

### クエリパラメータ

| 名前 | 必須 | 型 | 説明 |
|---|---|---|---|
| `from` | 必須 | date | 開始日（含む） |
| `to` | 必須 | date | 終了日（含む） |

MVP では常に `to - from = 1日` で呼ばれる想定。

### レスポンス 200

```json
[
  {
    "id": 101,
    "memberId": 1,
    "memberName": "お父さん",
    "date": "2026-04-24",
    "content": "在宅"
  },
  {
    "id": 102,
    "memberId": 3,
    "memberName": "そよ",
    "date": "2026-04-24",
    "content": "部活"
  }
]
```

**注意**：予定が1件もない場合は空配列 `[]` を返す。
画面側で「予定なし」を表示する（APIは "予定なし" を返さない）。

### エラー

- 400：`from`, `to` 未指定・不正な日付
- 400：`from > to` の場合

---

## 3. POST /api/schedules

予定を新規登録。

### リクエストボディ

```json
{
  "memberId": 3,
  "date": "2026-04-25",
  "content": "塾"
}
```

### バリデーション

| 項目 | ルール | NG時メッセージ |
|---|---|---|
| `memberId` | 1〜5 のいずれか | "誰を選んでください" |
| `date` | ISO日付、必須 | "日付を入力してください" |
| `content` | 必須、トリム後1〜100文字 | 空→"内容を入力してください" / 101文字以上→"内容は100文字以内で入力してください" |

### レスポンス 201 Created

```json
{
  "id": 103,
  "memberId": 3,
  "memberName": "そよ",
  "date": "2026-04-25",
  "content": "塾"
}
```

`Location: /api/schedules/103` ヘッダも返す。

### エラー

- 400：上記バリデーション

---

## 4. PUT /api/schedules/{id}

予定を編集（全項目差し替え）。

### パスパラメータ

| 名前 | 説明 |
|---|---|
| `id` | 予定ID |

### リクエストボディ

POST と同じ構造（`memberId`, `date`, `content`）。

### バリデーション

POST と同じ。

### レスポンス 200

POST と同じ構造。

### エラー

- 400：バリデーション
- 404：該当予定が存在しない

---

## 5. DELETE /api/schedules/{id}

予定を削除。

### パスパラメータ

| 名前 | 説明 |
|---|---|
| `id` | 予定ID |

### レスポンス 204 No Content

body なし。

### エラー

- 404：該当予定が存在しない

---

## DTO 定義（Java）

```java
// 入力
public record ScheduleRequest(
    Integer memberId,
    LocalDate date,
    String content
) {}

// 出力
public record ScheduleResponse(
    Long id,
    Integer memberId,
    String memberName,
    LocalDate date,
    String content
) {}

public record MemberResponse(
    Integer id,
    String name,
    Integer displayOrder
) {}

// エラー
public record ErrorResponse(
    String error,
    String message,
    Map<String, String> fields  // 任意
) {}
```

---

## 今後拡張（バックログ連動）

| 拡張 | API への影響 |
|---|---|
| BL-06 認証 | 全エンドポイントに Authorization ヘッダ必須 |
| BL-08 登録者記録 | `ScheduleResponse` に `createdBy` を追加 |
| BL-11 入力候補 | `GET /api/schedules/suggestions?memberId=...` 新設 |
| BL-12 送迎フラグ | `ScheduleRequest/Response` に `needsPickup: boolean` を追加 |
