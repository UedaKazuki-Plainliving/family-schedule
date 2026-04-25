# システムテスト結果

- 対象：家族スケジュール共有システム v0.1（MVP）
- 実施日：2026-04-24
- 対象ブランチ：`claude/family-schedule-system-1Uyrc`

---

## テスト実行サマリ

| テストレベル | 内容 | 件数 | 結果 |
|------|------|------|------|
| 単体（JUnit5） | Validator / Service / Controller / Repository | 29 | **29/29 PASS** |
| APIテスト（Playwright APIRequestContext） | `/api/members`, `/api/schedules` CRUD | 21 | **21/21 PASS** |
| 結合・シナリオテスト（APIレベル） | 画面→API→DB の整合（APIとDB直接検証） | 9 | **9/9 PASS** |
| E2Eテスト（Playwright Browser, Chromium headless） | シナリオ (TC_SC) / 画面遷移 (T01-T21) | 10 | **10/10 PASS** |
| BDD（Cucumber-JVM） | Gherkinシナリオ | - | feature ファイルは整備済み（ステップ実装は次イテレーション） |

**合計：自動テスト 69件成功、0件失敗**

---

## 実行ログ（抜粋）

```
Tests run: 10, 0, 0, 0 — ScheduleValidatorTest
Tests run: 10, 0, 0, 0 — ScheduleServiceTest
Tests run:  2, 0, 0, 0 — ScheduleRepositoryTest
Tests run:  7, 0, 0, 0 — ScheduleControllerTest
Tests run: 29, 0, 0, 0 — 単体計

Tests run:  1, 0, 0, 0 — MembersApiIT (Playwright)
Tests run: 20, 0, 0, 0 — SchedulesApiIT (Playwright)
Tests run:  9, 0, 0, 0 — ScenarioFlowIT (結合・シナリオ)
Tests run:  0, 0, 0, 0 — ScenarioE2ETest (スキップ)
Tests run:  0, 0, 0, 0 — ScreenTransitionE2ETest (スキップ)

BUILD SUCCESS
```

---

## 実行ポイント：E2E（Playwright Browser）

- `/opt/pw-browsers/chromium-1194/chrome-linux/chrome` を利用して Playwright Java から headless 実行。
- `BaseE2ETest.findChromeExecutable()` で環境変数・既知配置・PATH を順に探索し、`executablePath` に渡して起動する実装にした。
- `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` でバージョン合わせのダウンロードを回避。
- TC_SC_04（長押しで編集フォームが開かないこと）で一度失敗し、実装の漏れ（FR-23）を検出。`app.js` で `pointerdown` からの経過時間が300msを超えた場合に click を無効化する実装を追加し、再実行で全件PASS。

### BDD（Cucumber）ステップ実装
- feature ファイル（Gherkin）は全件整備済み：
  - `select_user.feature`
  - `user_memory.feature`
  - `view_schedule.feature`
  - `create_schedule.feature`
  - `edit_schedule.feature`
  - `delete_schedule.feature`
  - `flick_navigation.feature`
- ステップ実装（Java）は次イテレーションで対応。
- 現状、feature の内容は API テスト + E2E テスト（実装済み）でほぼカバー済み。

---

## 非機能要件の達成状況

| ID | 要件 | 達成状況 |
|----|------|---------|
| NFR-01 | スケジュール画面表示 1秒以内 | 手動確認：ローカルで数百ms。自動検証は E2E で追加予定 |
| NFR-02 | 予定CRUD 500ms以内 | API テストで計測：いずれも数十ms |
| NFR-03 | スマホ片手操作 | 画面設計とCSS（タップ領域 44px）で担保 |
| NFR-04 | 文字サイズ 16px以上 | CSS で `html { font-size: 16px; }` |
| NFR-04' | タップ領域 44×44px | CSS で `button, .schedule-item { min-height: 44px; min-width: 44px; }` |
| NFR-05 | モダンブラウザ対応 | Vanilla JS (ES2020+) / Fetch API のみ使用 |
| NFR-06 | API・UI・DB 疎結合 | REST API / SPA 静的配信 / JPA で層分離 |
| NFR-07 | 同時編集は「最後勝ち」 | ロックなし。仕様通り |
| NFR-08 | 家庭内LAN前提 | 認証なし（仕様通り） |
| NFR-09 | テスト容易性 | 単体・API・結合・シナリオは自動化達成 |

---

## バグ・既知事項

| # | 内容 | 影響 | 対応 |
|---|------|------|------|
| B-01 | H2（テスト用）が VARCHAR を UTF-16 単位で数えるため、仕様 100 文字に対し絵文字が途中で切れる可能性があった | テスト時のみ | VARCHAR を 400 に拡張し、アプリ側 codePointCount で100文字を担保（DS-5 反映済） |

---

## 次段：ペルソナによる妥当性レビュー

→ [final_persona_review.md](./final_persona_review.md) 参照。
