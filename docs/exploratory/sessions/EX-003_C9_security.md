# EX-003：C9 セキュリティ（XSS / SQLi / 境界）

| 項目 | 値 |
|---|---|
| Charter | C9 |
| 担当 | Claude |
| 開始 | 2026-04-24 23:54 UTC |
| タイムボックス | 30分 |
| 実時間 | 約18分（ブラウザ検証含む） |
| Setup% / Test% / Bug% | 10% / 70% / 20% |
| 関連機能 | 表示 / API |

## ミッション
> Explore 「ユーザー入力経由の出力箇所」 with 「`<script>`、`<img onerror>`、SQL片、`../` パストラバーサル、HTTPヘッダ汚染」 to discover 「永続XSS、CORS漏れ、HTTPヘッダ Injection、SQL Injection」

## テスト実施内容と結果

| # | テストケース | 期待 | 実結果 | 判定 |
|---|---|---|---|---|
| T1 | `<script>alert("XSS")</script>` を保存 → ブラウザ表示 | XSS が発火しない | 保存 201。ブラウザでは `textContent` で出力されるためダイアログ未発火（`window.__xss === undefined`） | OK |
| T2 | `<img src=x onerror=...>` 保存 → 表示 | 発火しない | 同上、未発火 | OK |
| T3 | content に `'; DROP TABLE schedules; --` | 文字列として保存（SQLi 効かない） | 保存成功、テーブル健在 | OK（JPA） |
| T4 | memberId に `"1 OR 1=1"`（型違い文字列） | 400 | 400 だが **Jackson 生メッセージ漏出** | BUG-3 系 |
| T5 | date に `"2026-04-24'; DROP--"` | 400 | 400 だが **DateTimeParseException 生メッセージ漏出** | BUG-3 系 |
| T6 | GET URL に SQL片 (`from=2026-04-24%27`) | 400 | 400 だが **内部例外メッセージ漏出** | BUG-3 系 |
| T7 | パストラバーサル `/../etc/passwd` | 404 | **HTTP 500 INTERNAL** "No static resource etc/passwd." | **BUG-5** |
| T8 | `/` （静的トップ） | 200 + index.html | 200 OK | OK |
| T9 | `/static/../../../etc/passwd` | 404/403 | **HTTP 500 INTERNAL** | **BUG-5** |
| T10 | リクエストヘッダに `<script>` 注入 | 影響なし | 200 OK・影響なし | OK |
| T11 | URL に CRLF 注入（`%0d%0a`） | 400 | 400 OK（datetime parse で弾かれた） | OK |
| T12 | memberId に巨大数値 (`99999999999`) | 400 | 400 + Jackson 生メッセージ | BUG-3 系 |
| T13 | memberId に配列 `[1,2,3]` | 400 | 400 + Jackson 生メッセージ | BUG-3 系 |
| T14 | 不要フィールド (`admin: true`) を含む | 受け流される | 受け流された（body にも反映なし） | OK（safe） |
| T15 | content に HTML を入れて GET 応答確認 | レスポンスは JSON エスケープでも OK | 期待通り JSON 文字列として返る | OK |

XSS 発火検証は Playwright E2E（`XssExploreTest.EX003_T1/T2`）で自動化済み・全件 PASS（ダイアログ未発火＋`window.__xss` 未設定）。

## 検出された不具合（要修正）

- **BUG-5（中）**：パストラバーサル試行や存在しない静的リソース（`/etc/passwd`）に対して **HTTP 500 + 内部メッセージ「No static resource etc/passwd.」** が返る。本来 **404** を返すべき。BUG-1/2 と同根（包括的 `Exception.class` ハンドラが NoResourceFoundException を吸ってしまう）。

## OK だったが要追跡

- **OBS-7**：HTML/Script タグはサーバ側でサニタイズされず、そのままDB保存される。フロントは `textContent` で安全に出しているため**現状XSS発火しない**が、将来 `innerHTML` を使う変更が入ると即座に永続XSSになる。**多層防御**として、保存時にもHTMLエスケープまたはタグ拒否したい。
- **OBS-8**：CORS 既定設定で別オリジンからの preflight は 403。これは MVP の用途（家族内＋同一オリジン or VPN想定）に合致するため OK。

## 次の探索アイデア
- CSP ヘッダの確認（現状なし）
- セキュリティヘッダ全般（X-Content-Type-Options, X-Frame-Options 等）
