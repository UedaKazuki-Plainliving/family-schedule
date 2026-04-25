# テスト戦略書

- バージョン：v0.1
- 前提：要件定義 v0.3、基本設計 v0.1（レビュー反映）

---

## テストレベルとツール

| レベル | 目的 | ツール | 対象 |
|---|---|---|---|
| 単体テスト | クラス・メソッド単位の正しさ | JUnit 5 + Mockito | Service、バリデーション、DTO変換 |
| リポジトリテスト | JPA マッピング / クエリ | JUnit 5 + Spring Boot Test + Testcontainers(PostgreSQL) | Repository |
| APIテスト | HTTP越しの入出力・ステータス | Playwright for Java (`APIRequestContext`) | Controller + Service + DB (インプロセス起動) |
| 結合テスト | 画面 → API → DB が一貫して動くか | Playwright for Java (Browser + API) + 仕様書 | End-to-end に近いが焦点は「APIと画面の整合」 |
| シナリオテスト | ペルソナのユースケースが通して実行できるか | Playwright for Java | ユーザーストーリー |
| 画面遷移テスト | 画面遷移図通りに遷移するか | Playwright for Java | 遷移網羅 |
| BDD（ATDD） | Gherkin 受け入れ基準のパス | Cucumber-JVM + Playwright | feature ファイル全件 |
| **負荷テスト** | NFR-01/02 の性能目標を満たすか | k6 | 本番同等環境の全APIエンドポイント |
| **探索的テスト** | 自動化できない UX リスク・境界条件の発見 | SBTM（手動、30分タイムボックス） | 全機能（チャーター C1〜C13） |

---

## 回帰方針

- 単体・API・E2E は**コード変更のたびに全件実行**（修正が入るたびに回す）。
- Maven プロファイルで分離：
  - `mvn test`：単体＋リポジトリ
  - `mvn verify -Papi`：API テスト
  - `mvn verify -Pe2e`：E2E（画面）
  - `mvn verify -Pall`：全件

---

## テスト仕様書の構成

1. [APIテスト仕様書](./02_api_test_spec.md)
2. [結合テスト仕様書](./03_integration_test_spec.md)
3. [シナリオテスト仕様書](./04_scenario_test_spec.md)
4. [画面遷移テスト仕様書](./05_screen_transition_test_spec.md)

---

## 受け入れ判定

以下すべてを満たしたとき MVP 完了とする。

- 単体テスト：カバレッジ（分岐）70%以上、全件パス
- APIテスト：全仕様書項目パス
- E2E（結合・シナリオ・画面遷移）：全仕様書項目パス
- Gherkin：全 feature パス
- **ペルソナ（プロダクトオーナー扮）による最終レビュー**：重大な要求不足なし
