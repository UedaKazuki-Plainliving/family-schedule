# 探索的テスト 不具合ログ

- 期間：2026-04-24 23:51 〜 2026-04-25 00:13 UTC
- 対象：家族スケジュール共有システム MVP v0.1（コミット `6763aac` 時点）
- セッション数：5（EX-001 〜 EX-005）
- 検出件数：**Bug 5件、Observation 5件**

---

## Bug（要修正）

### BUG-1：未対応 Content-Type で HTTP 500 を返す

| 項目 | 値 |
|---|---|
| 重要度 | **High**（HTTP規約違反、内部メッセージ漏出） |
| 検出 | EX-002 T11/T12 |
| 再現 | `curl -X POST /api/schedules -H 'Content-Type: text/plain' -d '...'` |
| 期待 | 415 Unsupported Media Type |
| 実際 | **500 INTERNAL** + `"Content-Type 'text/plain;charset=UTF-8' is not supported"` |
| 想定原因 | `GlobalExceptionHandler.handleOther(Exception)` が `HttpMediaTypeNotSupportedException` を吸ってしまう |
| 修正案 | `@ExceptionHandler(HttpMediaTypeNotSupportedException.class)` を追加し 415 を返す |

### BUG-2：未対応 HTTP メソッドで HTTP 500 を返す

| 項目 | 値 |
|---|---|
| 重要度 | **High** |
| 検出 | EX-002 T15/T16 |
| 再現 | `curl -X PATCH /api/schedules/1 ...` または `GET /api/schedules/1` |
| 期待 | 405 Method Not Allowed |
| 実際 | **500 INTERNAL** + `"Request method 'PATCH' is not supported"` |
| 想定原因 | 同上、`HttpRequestMethodNotSupportedException` の専用ハンドラ未実装 |
| 修正案 | 専用ハンドラを追加し 405 を返す。`Allow` ヘッダも付与すべき |

### BUG-3：型変換エラーの内部例外メッセージがクライアントに漏出

| 項目 | 値 |
|---|---|
| 重要度 | **Medium**（情報漏えい） |
| 検出 | EX-002 T9/T10/T13、EX-003 T4/T5/T6/T12/T13 |
| 再現 | `PUT /api/schedules/abc`、`POST` で memberId に文字列など |
| 期待 | 400 + 「不正な値です」のような汎用メッセージ |
| 実際 | 400 だが `"Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'..."` のような Java 内部例外がそのまま `message` フィールドに |
| 想定原因 | `MissingServletRequestParameterException`/`MethodArgumentTypeMismatchException`/`HttpMessageNotReadableException` のメッセージを `ex.getMessage()` で素通り返却 |
| 修正案 | これらのハンドラで汎用メッセージに置換（例：「リクエストパラメータが不正です」）。詳細は `fields` にセット |

### BUG-4：パストラバーサル試行時に HTTP 500 を返す

| 項目 | 値 |
|---|---|
| 重要度 | **Medium** |
| 検出 | EX-003 T7/T9 |
| 再現 | `curl /../etc/passwd` または `curl /static/../../../etc/passwd` |
| 期待 | 404 Not Found |
| 実際 | **500 INTERNAL** + `"No static resource etc/passwd."` |
| 想定原因 | `NoResourceFoundException` が `Exception.class` ハンドラに吸われる |
| 修正案 | 専用ハンドラまたは `ResponseStatus(NOT_FOUND)` を活かす |

### BUG-5（軽）：JSON 構文エラー時のメッセージが内部実装を露出

| 項目 | 値 |
|---|---|
| 重要度 | **Low** |
| 検出 | EX-002 T13 |
| 再現 | `curl -X POST -d '{not json}'` |
| 期待 | 400 + 簡潔な「JSON が不正です」 |
| 実際 | 400 だが `"JSON parse error: Unexpected character ('n' (code 110))..."` というJackson 内部メッセージ |
| 想定原因 | BUG-3 と同根 |
| 修正案 | BUG-3 と同時に修正 |

---

## Observation（観察事項）

### OBS-1：HTML タグ文字列がDBに素のまま保存される

- 検出：EX-001 T1, EX-003 T1
- 影響：現状フロントは `textContent` で出力するため XSS は発火しない（E2E で検証済）。
- リスク：将来 `innerHTML` を使う変更が入ると即座に永続XSS。
- 推奨：保存時のサニタイズ／HTMLタグ拒否で**多層防御**。

### OBS-2：改行入りの内容が、表示時に1行になってしまう

- 検出：EX-001 T2
- 影響：入力時に見た文字列と表示が異なる。混乱の元。
- 推奨：表示側 CSS に `white-space: pre-line;` を当てる（または改行禁止のヘルプ表示）。

### OBS-3：ZWJ家族絵文字（👨‍👩‍👧‍👦）は1絵文字＝7コードポイント

- 検出：EX-001 T6
- 影響：「100文字制限」がユーザー体感より厳しく感じる（家族絵文字14個で打ち切られる）。
- 推奨：仕様としては正しい。ヘルプ表示に「絵文字によっては短く感じる場合があります」と注記しておくと親切。

### OBS-4：不正な LocalStorage（実在しないmember/型違い）でもスケジュール画面に進めてしまう

- 検出：EX-004 T2/T3
- 影響：認証なしのMVPなので実害は極小だが、ヘッダに偽の利用者名（"hacker"等）が出る可能性。
- 推奨：起動時に `currentUser.id` が `/api/members` の結果に含まれることを検証し、ない場合は S-01 に戻す。

### OBS-5：フロントの「今日／明日」判定が利用者端末のタイムゾーン依存

- 検出：EX-005 観察
- 影響：海外旅行など端末TZがずれている場合、深夜帯に「今日」表示が想定と1日ずれる。
- 推奨：将来要件として TZ 明示指定（Asia/Tokyo 固定など）。バックログ追加候補。

---

## サマリ集計

| 区分 | 件数 |
|---|---|
| Bug (High) | 2 |
| Bug (Medium) | 2 |
| Bug (Low) | 1 |
| Observation | 5 |

## 重要度別の対応優先

1. **BUG-1 / BUG-2 / BUG-4**：`GlobalExceptionHandler` に専用ハンドラを3〜4個追加することで一括解決可能。実装コスト 1時間程度。**早急に対応推奨**。
2. **BUG-3 / BUG-5**：エラーメッセージを汎用化。BUG-1〜4 と同じ修正の延長で対応可。
3. **OBS-1**：保守時の事故防止のため、保存時にHTML文字列をエスケープか拒否する方針を決める。
4. **OBS-2 / OBS-3 / OBS-4 / OBS-5**：MVP範囲外として **バックログに追加**。
