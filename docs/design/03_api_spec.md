# API詳細仕様書

- バージョン：v0.2
- 更新履歴：
  - v0.1：初版
  - v0.2：メンバーCRUD API 追加、BL-16（restore/purge）API 追加、エラー仕様拡充
- ベースURL：`/api`
- 形式：JSON (UTF-8)
- 認証：なし（MVP）
- 日付形式：`YYYY-MM-DD`（ISO-8601）

---

## エンドポイント一覧

| メソッド | パス | 概要 | 関連FR |
|---|---|---|---|
| GET | `/api/members` | 家族メンバー一覧取得 | FR-01, FR-08 |
| POST | `/api/members` | メンバー追加 | FR-24 |
| PUT | `/api/members/{id}` | メンバー名前変更 | FR-24 |
| DELETE | `/api/members/{id}` | メンバー削除 | FR-24 |
| GET | `/api/schedules` | 指定日付範囲の予定一覧取得 | FR-03〜06 |
| POST | `/api/schedules` | 予定新規登録 | FR-07〜10, FR-18 |
| PUT | `/api/schedules/{id}` | 予定編集 | FR-11〜12, FR-18 |
| DELETE | `/api/schedules/{id}` | 予定削除（soft delete） | FR-13 |
| POST | `/api/schedules/{id}/restore` | 削除済み予定を復元 | BL-16 |
| POST | `/api/schedules/{id}/purge` | 削除済み予定を完全削除 | BL-16 |

---

## 共通

### エラーレスポンス

| HTTP | 意味 | body |
|---|---|---|
| 400 | バリデーションエラー / パラメータ不正 | `{ "error": "VALIDATION", "message": "...", "fields": { "content": "必須" } }` |
| 404 | リソース未存在 | `{ "error": "NOT_FOUND", "message": "schedule not found" }` |
| 405 | 未対応 HTTP メソッド | `{ "error": "METHOD_NOT_ALLOWED", "message": "..." }` + `Allow` ヘッダ |
| 409 | 競合（FK制約など） | `{ "error": "CONFLICT", "message": "このメンバーには予定が登録されています。先に予定を削除してください。" }` |
| 415 | Content-Type 不正 | `{ "error": "UNSUPPORTED_MEDIA_TYPE", "message": "..." }` |
| 500 | 内部エラー | `{ "error": "INTERNAL", "message": "サーバーエラーが発生しました" }` |

### ID表現

- `member.id`：整数（マスタ）
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
  { "id": 3, "name": "長女",     "displayOrder": 3 },
  { "id": 4, "name": "次女",   "displayOrder": 4 },
  { "id": 5, "name": "長男", "displayOrder": 5 }
]
```

並び順は `displayOrder` 昇順。

---

## 2. POST /api/members

メンバーを追加する。

### リクエストボディ

```json
{ "name": "おじいちゃん" }
```

### バリデーション

| 項目 | ルール | NG時メッセージ |
|---|---|---|
| `name` | 必須、トリム後1〜20文字 | 空→"名前を入力してください" / 超過→"名前は20文字以内で入力してください" |
| `name` | 既存メンバーと重複不可 | "同じ名前のメンバーが既に存在します" |
| 合計人数 | 10名未満 | "メンバーは最大10名までです" |

### レスポンス 201 Created

```json
{ "id": 6, "name": "おじいちゃん", "displayOrder": 6 }
```

---

## 3. PUT /api/members/{id}

メンバーの名前を変更する。

### リクエストボディ

```json
{ "name": "お兄ちゃん" }
```

### バリデーション

POST と同じ（重複チェックは自分自身を除く）。

### レスポンス 200

```json
{ "id": 6, "name": "お兄ちゃん", "displayOrder": 6 }
```

### エラー

- 400：バリデーション
- 404：該当メンバーが存在しない

---

## 4. DELETE /api/members/{id}

メンバーを削除する。

### レスポンス 204 No Content

body なし。

### エラー

- 404：該当メンバーが存在しない
- 409：当該メンバーに予定が紐づいている（FK制約）

---

## 5. GET /api/schedules

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
    "memberName": "長女",
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

## 6. POST /api/schedules

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
| `memberId` | 存在するメンバーID | "誰を選んでください" |
| `date` | ISO日付、必須 | "日付を入力してください" |
| `content` | 必須、トリム後1〜100文字 | 空→"内容を入力してください" / 101文字以上→"内容は100文字以内で入力してください" |

### レスポンス 201 Created

```json
{
  "id": 103,
  "memberId": 3,
  "memberName": "長女",
  "date": "2026-04-25",
  "content": "塾"
}
```

`Location: /api/schedules/103` ヘッダも返す。

### エラー

- 400：上記バリデーション

---

## 7. PUT /api/schedules/{id}

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

## 8. DELETE /api/schedules/{id}

予定を soft delete（`deleted_at` を現在時刻にセット）。

### パスパラメータ

| 名前 | 説明 |
|---|---|
| `id` | 予定ID |

### レスポンス 204 No Content

body なし。

### エラー

- 404：該当予定が存在しない

---

## 9. POST /api/schedules/{id}/restore

soft delete された予定を復元する（`deleted_at` を NULL に戻す）。

### レスポンス 200

復元後の `ScheduleResponse`。

### エラー

- 404：該当予定が存在しない、または削除済みでない

---

## 10. POST /api/schedules/{id}/purge

soft delete された予定を物理削除する。

### レスポンス 204 No Content

body なし。

### エラー

- 404：該当予定が存在しない、または削除済みでない

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

// メンバー入力
public record MemberRequest(
    String name
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
