# EX-002：C7 API 直叩き（規約違反・不正値）

| 項目 | 値 |
|---|---|
| Charter | C7 |
| 担当 | Claude |
| 開始 | 2026-04-24 23:53 UTC |
| タイムボックス | 30分 |
| 実時間 | 約14分 |
| Setup% / Test% / Bug% | 5% / 60% / 35% |
| 関連機能 | 全API |

## ミッション
> Explore 「公開エンドポイント」 with 「不正Content-Type、巨大ペイロード、未知HTTPメソッド、URL不正、from=to同日、from>>to、負ID、非常に大きいID」 to discover 「500エラー、スタックトレース漏れ、サービス停止」

## テスト実施内容と結果

| # | テストケース | 期待 | 実結果 | 判定 |
|---|---|---|---|---|
| T1 | GET `from=to`（同日） | 200・該当日のデータ返却 | 200・正しい | OK |
| T2 | GET `from=1900-01-01..` | 200・空 | 200・空 | OK |
| T3 | GET `from=9999-01-01..` | 200・空 | 200・空 | OK |
| T4 | GET `from=0001-01-01..` | 200・空 | 200・空 | OK |
| T5 | GET 範囲外（データなし） | 200・空 | 200・空 | OK |
| T6 | PUT `id=0` | 404 NotFound | 404 OK | OK |
| T7 | PUT `id=-1` | 400 不正ID または 404 | 404 | OK（許容） |
| T8 | PUT `id=Long.MAX` | 404 | 404 OK | OK |
| T9 | PUT `id=Long.MAX+1`（オーバーフロー） | 400 | **400 だが 内部例外メッセージ漏出**「Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'」 | **BUG-3** |
| T10 | PUT `id=abc` | 400 | **400 だが 内部例外メッセージ漏出**（同上） | **BUG-3** |
| T11 | POST `Content-Type: text/plain` | 415 Unsupported Media Type | **HTTP 500 INTERNAL**「Content-Type 'text/plain;charset=UTF-8' is not supported」 | **BUG-1** |
| T12 | POST 無 Content-Type（form想定） | 415 | **HTTP 500 INTERNAL** | **BUG-1** |
| T13 | POST 不正JSON | 400 + 簡潔なメッセージ | **400 だが Jackson の生メッセージが漏出**「JSON parse error: Unexpected character...」 | BUG-3（軽） |
| T14 | POST 1MB のリクエスト | 413 か Validation 400 | 400（content 100文字超） | OK |
| T15 | PATCH `/api/schedules/{id}` | 405 Method Not Allowed | **HTTP 500 INTERNAL**「Request method 'PATCH' is not supported」 | **BUG-2** |
| T16 | GET `/api/schedules/{id}` | 405 Method Not Allowed | **HTTP 500 INTERNAL**「Request method 'GET' is not supported」 | **BUG-2** |
| T17 | OPTIONS `/api/schedules` （CORS preflight from `evil.example.com`） | 403 Invalid CORS（既定） | 403 「Invalid CORS request」 | OK（むしろ良い） |
| T18 | TRACE `/api/schedules` | 405 | 405 | OK |

## 検出された不具合（要修正）

- **BUG-1（重要）**：未対応 Content-Type（text/plain など）で **HTTP 500** が返る。HTTP 規約上は **415** が正しい。`HttpMediaTypeNotSupportedException` の専用ハンドラ未実装。
- **BUG-2（重要）**：未サポート HTTP メソッド（PATCH, GET on `/{id}` など）で **HTTP 500** が返る。HTTP 規約上は **405** が正しい。`HttpRequestMethodNotSupportedException` の専用ハンドラ未実装。
- **BUG-3（中）**：型変換エラー時に **Java 内部例外メッセージがクライアントに漏出**（"Failed to convert value of type 'java.lang.String' to required type 'java.lang.Long'..."）。攻撃者に内部実装情報を与える＋エンドユーザーには無意味。`MethodArgumentTypeMismatchException` のメッセージはマスクすべき。

## 次の探索アイデア
- パストラバーサル時の 500 エラー（`No static resource ...`）も同根 → EX-003 で確認
- Spring Boot のデフォルトのリクエストサイズ上限の確認
