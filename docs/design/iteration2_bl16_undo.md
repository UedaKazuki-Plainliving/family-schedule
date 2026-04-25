# イテレーション2 設計書：BL-16 削除 UNDO

- 対象：[BL-16 削除 UNDO（5秒以内なら復元）](../usecases/usecases.md)
- 昇格後ID：**FR-24**
- 作成日：2026-04-24
- 関連：要件定義 v0.4（本書で更新）

---

## 1. 目的

お母さん（最重要ユーザー）から「誤操作からの復帰」を強く要望。
削除操作を即確定せず、**5秒以内なら元に戻せる**ようにする。

---

## 2. 機能仕様（FR-24）

### 2.1 UX

- ユーザーが削除確定（S-05 で「はい」）すると、
  1. 画面から当該予定がすぐに消える（楽観的UI）
  2. トーストに「削除しました [元に戻す]」リンクが表示される
  3. **5秒間**トーストは表示されつづける
  4. 5秒以内に「元に戻す」をタップすると、元通り復元（同じIDで戻す）
  5. 5秒経過後はトーストが消え、削除が**確定**
- 別の予定の追加／編集／削除を行った時点で、保留中のUNDOはキャンセル（＝確定）される

### 2.2 サーバ側データ

- 物理削除を**ソフト削除に変更**：
  - `schedules` テーブルに `deleted_at TIMESTAMPTZ NULL` 列を追加
  - `DELETE` API：実際には `deleted_at = now()` で更新
  - `GET` API：`deleted_at IS NULL` のみ返す（現行挙動と一致）
  - 新API：`POST /api/schedules/{id}/restore` → `deleted_at = NULL` に戻す
  - 新API：`POST /api/schedules/{id}/purge` → 物理削除（UNDO タイムアウト時に呼ぶ）
- これにより、ID を維持したまま復元でき、参照整合も壊さない

### 2.3 フロント側

- 削除時：
  1. `DELETE /api/schedules/{id}` を呼ぶ
  2. 5秒カウントダウンのトーストを表示、「元に戻す」ボタン付き
  3. 5秒経過：`POST /api/schedules/{id}/purge` を非同期で投げる（失敗は無視）
  4. 5秒以内に「元に戻す」押下：`POST /api/schedules/{id}/restore` を呼び、画面を再取得

### 2.4 エラーケース

| 状況 | 挙動 |
|------|------|
| restore 対象が存在しない | 404 を返す、フロントは「復元に失敗しました」トースト |
| purge 対象が存在しない | 404 ただし無視（既に消えている） |
| 5秒待ち中に別の削除 | 前のUNDOを確定（purge）→ 新しいUNDOを開始 |

---

## 3. API 仕様の追記

### 3.1 変更：DELETE /api/schedules/{id}

- 挙動：**物理削除 → ソフト削除に変更**
- ステータス：204 No Content（変わらず）
- 互換性：外部仕様としてはレスポンスが同じなので破壊的変更にならない

### 3.2 新設：POST /api/schedules/{id}/restore

- 目的：ソフト削除を取り消す
- リクエストボディ：なし
- 成功：200 OK、body はレストア後の `ScheduleResponse`
- 404：該当予定がない、または `deleted_at IS NULL`（すでに有効）

### 3.3 新設：POST /api/schedules/{id}/purge

- 目的：ソフト削除を確定（物理削除）
- リクエストボディ：なし
- 成功：204 No Content
- 404：該当予定がない or まだ `deleted_at IS NULL`（未削除なら purge できない）

---

## 4. DB マイグレーション（V2）

```sql
ALTER TABLE schedules ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX ix_schedules_deleted_at ON schedules(deleted_at);
```

既存の検索クエリは `WHERE ... AND deleted_at IS NULL` に更新。

---

## 5. テスト仕様

### 5.1 追加する単体テスト

| # | 内容 |
|---|------|
| U-01 | `ScheduleService.delete(id)` は論理削除を行い、`findRange` の結果から消える |
| U-02 | `ScheduleService.restore(id)` は deleted_at を null にし、`findRange` に復帰 |
| U-03 | `ScheduleService.purge(id)` は物理削除する |
| U-04 | 削除済の予定を `update` しようとすると NotFound |
| U-05 | すでに削除されていない予定を `restore` しようとすると NotFound |
| U-06 | 物理削除後の `purge` は NotFound |

### 5.2 追加する API テスト

| # | 内容 |
|---|------|
| A-01 | DELETE 後 GET すると当該予定が含まれない（現状維持） |
| A-02 | DELETE → POST /restore 200 → GET に戻っている |
| A-03 | DELETE → POST /purge 204 → POST /restore は 404 |
| A-04 | 削除されていない ID への /restore は 404 |

### 5.3 追加する E2E テスト

| # | 内容 |
|---|------|
| E-01 | 予定を削除 → トーストに "元に戻す" が出る → タップすると画面に戻ってくる |
| E-02 | 予定を削除 → トーストを放置 5秒 → トーストが消える → 画面には残らない |

---

## 6. リスク

- 5秒の計測は JS タイマに依存。タブを閉じると UNDO できない（許容）。
- サーバ側のソフト削除データはクリーンアップされない → バックログ BL-19「ソフト削除レコードの定期パージ」を追加。
