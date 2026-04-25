# 探索的テスト 最終レポート

- 対象：家族スケジュール共有システム MVP v0.1（コミット `6763aac` 時点）
- 期間：2026-04-24 23:51 〜 2026-04-25 00:13 UTC（実時間 約62分）
- 手法：Session-Based Test Management（チャーター駆動・タイムボックス制）
- 担当：Claude

## 1. 全体サマリ

| 区分 | 数値 |
|---|---|
| 計画チャーター数 | 10（C1〜C10） |
| 実施セッション数 | **5**（C5, C7, C9, C10, C6 を High優先で先行） |
| 総タイムボックス | 150分 |
| 実時間 | **約62分**（うち探索 約45分、報告 約17分） |
| 検出 Bug | **5**（High×2, Med×2, Low×1） |
| 検出 Observation | 5 |
| 自動化済テストケース | **12**（Playwright/E2E：XSS×2、LocalStorage×4、日付×6） |

## 2. ハイライト

### 重大不具合（要早急対応）

1. **BUG-1**：未対応 Content-Type で **HTTP 500 + 内部メッセージ漏出**（本来 415）
2. **BUG-2**：未対応 HTTP メソッドで **HTTP 500 + 内部メッセージ漏出**（本来 405）
3. **BUG-3**：型変換エラーで **Java の内部例外メッセージがクライアントに漏出**

3件とも `GlobalExceptionHandler` の包括 `Exception.class` ハンドラに HttpMediaTypeNotSupportedException / HttpRequestMethodNotSupportedException / TypeMismatchException が吸われていることが原因。専用ハンドラ追加で一括解決可能。

### 良かった点（堅牢性が確認できた箇所）

- **SQL injection** 効かず（JPA / PreparedStatement）
- **CRLF Injection / HTTP Response Splitting** 効かず（バリデーションで阻止）
- **XSS** 永続的に保存はされるが、フロント `textContent` で安全に出力（**ブラウザで実機検証済**）
- **CORS** 別オリジンからの preflight は 403 で拒否
- **日付境界**（閏日、年またぎ、極大値）は適切に処理
- **LocalStorage** 壊れた JSON や空 JSON は適切に S-01 へフォールバック

## 3. チャーター実施状況

| Charter | テーマ | 実施 | 検出 Bug | 検出 Obs |
|---|---|---|---:|---:|
| C1 | お母さん誤操作 | ⏸ 未実施（後続候補） | - | - |
| C2 | お父さん起動 | ⏸ 未実施 | - | - |
| C3 | そよ操作 | ⏸ 未実施 | - | - |
| C4 | ゆうり保護 | ⏸ 未実施（既存テストで概ねカバー） | - | - |
| C5 | 入力境界 | ✅ EX-001 | 0 | 3 |
| C6 | 日付境界 | ✅ EX-005 | 0 | 1 |
| C7 | API 直叩き | ✅ EX-002 | **3** | 0 |
| C8 | 同時編集 | ⏸ 未実施 | - | - |
| C9 | セキュリティ | ✅ EX-003 | **2** | 1 |
| C10 | LS 腐敗 | ✅ EX-004 | 0 | 1 |
| **計** | | **5/10** | **5** | **5** |

不具合検出が5件確定したため、初回ラウンドのHi優先5本を打ち切り、修正サイクルへ回す判断とした。
C1/C2/C3/C4/C8 は次ラウンドで実施予定。

## 4. 動的な探索の判断ポイント

| タイミング | 判断 | 根拠 |
|---|---|---|
| EX-002 中盤 | API直叩きから派生して BUG-1 を発見 → **同種が複数あるはず**と仮説 → T15/T16 で BUG-2 を確認 | パターン認識（包括Exception ハンドラ起因） |
| EX-002 終了後 | パストラバーサル（C9 の範疇）を EX-003 で先に確認 | 同根の BUG-4 を続けて検出する効率的経路 |
| EX-003 中盤 | XSS は API では確認できない → **ブラウザ実機で検証**するため Playwright で `XssExploreTest` を起こした | 静的観察だけでは判定できないリスク |
| EX-004/005 | バグ発見が打ち止めの感触 → 残量タイムは記録に回す | 投資効率 |

## 5. 自動回帰用に起こしたテスト

EX-003〜005 で発見・確認した内容は、Playwright で **回帰テスト化済み**。今後コードを変えるたびに走らせれば再発を検知できる。

| テストクラス | 内容 |
|---|---|
| `com.family.schedule.explore.XssExploreTest` | `<script>` / `<img onerror>` が画面で発火しない |
| `com.family.schedule.explore.LocalStorageExploreTest` | LocalStorage 4種腐敗パターンでクラッシュしない |
| `com.family.schedule.explore.DateBoundaryExploreTest` | 閏日・年またぎ・極大年・不正日付 |

`mvn verify` の対象に含まれており、**全12件 PASS**。

## 6. 次にやること

### 即時対応（Bug 修正）

1. `GlobalExceptionHandler` に専用ハンドラを追加し、BUG-1〜5 を一括解決
   - `HttpMediaTypeNotSupportedException` → 415
   - `HttpRequestMethodNotSupportedException` → 405 (+ Allow ヘッダ)
   - `NoResourceFoundException` → 404
   - 型変換エラー → 400 + 汎用メッセージへ書き換え
2. 既存テスト（API IT 等）に **回帰ケースを追加**
   - 415, 405, 404 が返ること
   - エラーメッセージに「java.lang」「Failed to convert」が含まれないこと

### バックログへ追加

| ID | 内容 | 由来 |
|---|---|---|
| BL-19 | 改行入り content の表示時 white-space: pre-line | OBS-2 |
| BL-20 | LocalStorage の currentUser を起動時に再検証 | OBS-4 |
| BL-21 | 日付計算のTZ明示指定（Asia/Tokyo 固定） | OBS-5 |
| BL-22 | 多層XSS防御として保存時にもサニタイズまたはタグ拒否 | OBS-1 |
| BL-23 | 100文字制限のヘルプ表示に絵文字・ZWJ補足 | OBS-3 |

### 次回探索ラウンド候補

- **C1**：お母さん視点の連打／キャンセル／リロード
- **C2**：お父さん視点の起動パフォーマンス
- **C8**：同時編集の競合（2ブラウザ）
- セキュリティ追加：CSP / X-Frame-Options 等のヘッダ確認

## 7. 教訓（次に活かす）

- Spring Boot の包括 `Exception.class` ハンドラは便利だが、**フレームワーク提供例外を吸ってしまう副作用**に注意。本来 4xx で返すべきものが 5xx になり、結果としてアラート設計やHTTP規約適合性に影響する。
- XSS は **API 単位での検証は限界**があり、必ず**ブラウザ実機（Playwright）で発火確認**を入れる必要がある。
- 探索のなかで **重要パターン（包括 Exception ハンドラ起因）** を見つけたら、同根バグを連続で当てに行くと効率がよい。1セッション内で派生して横展開できる。

## 8. ファイル構成

```
docs/exploratory/
├── 01_charters.md           ← 10チャーター定義
├── 02_matrix.md             ← Charter × 機能 マトリックス
├── 03_bug_log.md            ← Bug 5件 + Observation 5件 集約
├── 04_final_report.md       ← 本ファイル
└── sessions/                ← 各セッションの詳細記録
    ├── EX-001_C5_input_boundary.md
    ├── EX-002_C7_api_attacks.md
    ├── EX-003_C9_security.md
    ├── EX-004_C10_localstorage.md
    └── EX-005_C6_date_boundary.md
```

回帰用 E2E：

```
src/test/java/com/family/schedule/explore/
├── XssExploreTest.java
├── LocalStorageExploreTest.java
└── DateBoundaryExploreTest.java
```
