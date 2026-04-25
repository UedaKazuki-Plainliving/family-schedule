# APIテスト仕様書

- バージョン：v0.1
- 実装：Playwright for Java の `APIRequestContext`
- 対象：[API詳細仕様書](../design/03_api_spec.md)

---

## テスト方針

- 各ケースで **事前に DB を初期化**（`TRUNCATE schedules RESTART IDENTITY;`）
- 期待値は JSON 構造と必須フィールドで検証
- ステータスコードと Content-Type も検証

---

## TC-API-01 GET /api/members

| # | 条件 | 期待 |
|---|------|------|
| 01-1 | 正常 | 200 / 5件返却 / `displayOrder` 昇順 / name が ["お父さん","お母さん","そよ","ゆうり","いちろう"] |

---

## TC-API-02 GET /api/schedules

| # | 条件 | 期待 |
|---|------|------|
| 02-1 | 予定0件、`from=2026-04-24&to=2026-04-25` | 200 / `[]` |
| 02-2 | 予定2件登録後に同期間取得 | 200 / 2件 / `date` 昇順、同一日は `displayOrder` 昇順 |
| 02-3 | `from` 未指定 | 400 / `error="VALIDATION"` |
| 02-4 | `from > to` | 400 / `error="VALIDATION"` |
| 02-5 | 不正な日付形式 (`from=xxx`) | 400 |

---

## TC-API-03 POST /api/schedules

| # | 条件 | 期待 |
|---|------|------|
| 03-1 | 正常（memberId=3, date=今日, content="部活"） | 201 / Locationヘッダ / body に id,memberName="そよ" |
| 03-2 | `content=""`（空） | 400 / fields.content="内容を入力してください" |
| 03-3 | `content` が空白のみ（"   "） | 400 / fields.content |
| 03-4 | `content` が101文字 | 400 / fields.content="内容は100文字以内で入力してください" |
| 03-5 | `content` が100文字（境界） | 201 |
| 03-6 | 絵文字(サロゲートペア含) 100コードポイント | 201 |
| 03-7 | `memberId=99`（存在しない） | 400 / fields.memberId |
| 03-8 | `date` 未指定 | 400 |
| 03-9 | `memberId` 未指定 | 400 |

---

## TC-API-04 PUT /api/schedules/{id}

| # | 条件 | 期待 |
|---|------|------|
| 04-1 | 正常（既存ID、全項目更新） | 200 / body に更新後の値 |
| 04-2 | 存在しないID | 404 / error="NOT_FOUND" |
| 04-3 | content 空 | 400 |
| 04-4 | content 101文字 | 400 |
| 04-5 | memberId を別メンバーへ変更 | 200 / memberId 変化を反映 |

---

## TC-API-05 DELETE /api/schedules/{id}

| # | 条件 | 期待 |
|---|------|------|
| 05-1 | 正常 | 204 / body なし / 後続GETで消えている |
| 05-2 | 存在しないID | 404 |
| 05-3 | 連続2回同じIDで削除 | 1回目=204、2回目=404 |

---

## 共通チェック

- Content-Type：`application/json; charset=UTF-8`
- レスポンス時間：500ms 以内（NFR-02）
