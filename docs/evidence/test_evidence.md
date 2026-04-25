# テスト結果・エビデンス

- 対象：家族スケジュール共有システム MVP v0.1
- 実施日時：2026-04-24 (UTC)
- 実施環境：
  - Java 21.0.10
  - Spring Boot 3.3.4
  - PostgreSQL 16.13
  - Playwright Java 1.49.0 / Chromium 141.0.7390.37 (headless)
  - Maven 3.9.11
- 関連文書：
  - [テスト戦略](../tests/01_test_strategy.md)
  - [APIテスト仕様](../tests/02_api_test_spec.md)
  - [結合テスト仕様](../tests/03_integration_test_spec.md)
  - [シナリオテスト仕様](../tests/04_scenario_test_spec.md)
  - [画面遷移テスト仕様](../tests/05_screen_transition_test_spec.md)
  - [システムテスト結果](../tests/system_test_result.md)
  - [最終ペルソナレビュー](../tests/final_persona_review.md)

---

## 1. サマリ

| レベル | テストクラス | 件数 | 結果 |
|---|---|---:|---:|
| 単体 | `ScheduleValidatorTest` | 10 | **10/10 PASS** |
| 単体 | `ScheduleServiceTest` | 10 | **10/10 PASS** |
| 単体 (WebMvc) | `ScheduleControllerTest` | 7 | **7/7 PASS** |
| 単体 (JPA) | `ScheduleRepositoryTest` | 2 | **2/2 PASS** |
| API | `MembersApiIT` (Playwright) | 1 | **1/1 PASS** |
| API | `SchedulesApiIT` (Playwright) | 20 | **20/20 PASS** |
| 結合・シナリオ | `ScenarioFlowIT` | 9 | **9/9 PASS** |
| E2E（ブラウザ） | `ScenarioE2ETest` | 4 | **4/4 PASS** |
| E2E（ブラウザ） | `ScreenTransitionE2ETest` | 6 | **6/6 PASS** |
| **合計** | | **69** | **69/69 PASS** |

**Maven ビルド：BUILD SUCCESS**（[summary.txt](./reports/_summary.txt)）

---

## 2. エビデンス構成

```
docs/evidence/
├── test_evidence.md                  ← 本ファイル
├── reports/                          ← Surefire / Failsafe のXML/TXTレポート
│   ├── _summary.txt
│   ├── TEST-com.family.schedule.*.xml
│   └── com.family.schedule.*.txt
├── api/
│   └── api_curl_evidence.txt         ← curlでのAPI動作証跡
├── db/
│   └── db_evidence.txt               ← PostgreSQL 状態スナップショット
├── screenshots/
│   ├── _map.tsv                      ← ファイル名→日本語テスト名 対応表
│   └── *.png                         ← E2EテストのスクリーンショットEvidence
└── videos/
    ├── _map.tsv                      ← ファイル名→日本語テスト名 対応表
    └── *.webm                        ← E2E実行の録画動画（iPhone 13 ビューポート）
```

---

## 3. 単体／APIテスト個別結果

各クラスのテキストレポートは [`reports/`](./reports/) に格納。例：

- [ScheduleValidatorTest](./reports/com.family.schedule.service.ScheduleValidatorTest.txt)
- [ScheduleServiceTest](./reports/com.family.schedule.service.ScheduleServiceTest.txt)
- [ScheduleRepositoryTest](./reports/com.family.schedule.repository.ScheduleRepositoryTest.txt)
- [ScheduleControllerTest](./reports/com.family.schedule.web.ScheduleControllerTest.txt)
- [MembersApiIT](./reports/com.family.schedule.api.MembersApiIT.txt)
- [SchedulesApiIT](./reports/com.family.schedule.api.SchedulesApiIT.txt)
- [ScenarioFlowIT](./reports/com.family.schedule.api.ScenarioFlowIT.txt)
- [ScenarioE2ETest](./reports/com.family.schedule.e2e.ScenarioE2ETest.txt)
- [ScreenTransitionE2ETest](./reports/com.family.schedule.e2e.ScreenTransitionE2ETest.txt)

XML（CIで読み込み可能）も同フォルダに保存済み。

---

## 4. APIエビデンス（curl）

完全な記録：[api/api_curl_evidence.txt](./api/api_curl_evidence.txt)

抜粋：

### TC-API-01：GET /api/members（200）
```
$ curl -s http://localhost:8080/api/members
[{"id":1,"name":"お父さん","displayOrder":1},
 {"id":2,"name":"お母さん","displayOrder":2},
 {"id":3,"name":"長女","displayOrder":3},
 {"id":4,"name":"次女","displayOrder":4},
 {"id":5,"name":"長男","displayOrder":5}]
```

### TC-API-03-1：POST /api/schedules（201）
```
$ curl -i -X POST http://localhost:8080/api/schedules \
       -H 'Content-Type: application/json' \
       -d '{"memberId":3,"date":"2026-04-24","content":"部活"}'

HTTP/1.1 201
Location: /api/schedules/1
Content-Type: application/json
{"id":1,"memberId":3,"memberName":"長女","date":"2026-04-24","content":"部活"}
```

### TC-API-03-2：内容空 → 400
```
HTTP/1.1 400
{"error":"VALIDATION","message":"入力に誤りがあります",
 "fields":{"content":"内容を入力してください"}}
```

### TC-API-03-4：101文字 → 400
```
HTTP/1.1 400
{"error":"VALIDATION","message":"入力に誤りがあります",
 "fields":{"content":"内容は100文字以内で入力してください"}}
```

### TC-API-04-2：存在しないID → 404
```
HTTP/1.1 404
{"error":"NOT_FOUND","message":"schedule not found: 99999","fields":null}
```

### TC-API-05-1：DELETE → 204
```
HTTP/1.1 204
```

---

## 5. DBエビデンス

完全な記録：[db/db_evidence.txt](./db/db_evidence.txt)

### members テーブル（5件のマスタが正しく投入）
```
 id |   name   | display_order
----+----------+---------------
  1 | お父さん |             1
  2 | お母さん |             2
  3 | 長女     |             3
  4 | 次女   |             4
  5 | 長男 |             5
```

### schedules テーブル（API 経由で4件登録した直後）
```
 id | member_id |    date    |   content    |          created_at           |          updated_at
----+-----------+------------+--------------+-------------------------------+-------------------------------
  1 |         1 | 2026-04-24 | 在宅         | 2026-04-24 23:31:45.157012+00 | 2026-04-24 23:31:45.157012+00
  2 |         2 | 2026-04-24 | 病院         | 2026-04-24 23:31:45.193627+00 | 2026-04-24 23:31:45.193627+00
  3 |         3 | 2026-04-24 | 部活         | 2026-04-24 23:31:45.226930+00 | 2026-04-24 23:31:45.226930+00
  4 |         5 | 2026-04-25 | サッカー教室 | 2026-04-24 23:31:45.259813+00 | 2026-04-24 23:31:45.259813+00
```

### スキーマ（VARCHAR(400) + char_length CHECK）
```
   Column   |           Type           | Nullable |        Default
------------+--------------------------+----------+---------------------------------------
 id         | bigint                   | not null | nextval('schedules_id_seq')
 member_id  | integer                  | not null |
 date       | date                     | not null |
 content    | character varying(400)   | not null |
 created_at | timestamp with time zone | not null | now()
 updated_at | timestamp with time zone | not null | now()
Indexes:
    "schedules_pkey" PRIMARY KEY, btree (id)
    "ix_schedules_date_member" btree (date, member_id)
Check constraints:
    "schedules_content_check" CHECK (char_length(content::text) >= 1)
Foreign-key constraints:
    "schedules_member_id_fkey" FOREIGN KEY (member_id) REFERENCES members(id) ON DELETE RESTRICT
```

---

## 6. E2E スクリーンショット

ファイル名はASCII安全な変換名。元の日本語テスト名は [`screenshots/_map.tsv`](./screenshots/_map.tsv) を参照。

### TC-SC-01 お母さんの朝（FR-20 / FR-21 / FR-22 を実証）

| ステップ | 画面 | スクリーンショット |
|---|---|---|
| 1 | 初回起動 → S-01 利用者選択 | [画像](./screenshots/ScenarioE2ETest_TC_SC_01__________01____S01______.png) |
| 2 | "お母さん" タップ → S-02 スケジュール画面 | [画像](./screenshots/ScenarioE2ETest_TC_SC_01__________02_S02_________.png) |
| 3 | 2回目起動 → 利用者選択をスキップして S-02 直行（FR-20） | [画像](./screenshots/ScenarioE2ETest_TC_SC_01__________03_2_____S02__.png) |
| 4 | "+追加" タップ → S-03 予定追加モーダル | [画像](./screenshots/ScenarioE2ETest_TC_SC_01__________04_S03_________.png) |
| 5 | "誰が"=長男、"内容"="学童お迎え" 入力 | [画像](./screenshots/ScenarioE2ETest_TC_SC_01__________05_S03_____.png) |
| 6 | "保存" 押下 → トースト "保存しました" | [画像](./screenshots/ScenarioE2ETest_TC_SC_01__________06_______.png) |
| 7 | 終了時の最終状態 | [画像](./screenshots/ScenarioE2ETest_TC_SC_01__________end.png) |

### TC-SC-05 削除と確認ダイアログ（FR-17 / FR-19 を実証）

| ステップ | 画面 | スクリーンショット |
|---|---|---|
| 1 | 初期表示（"サッカー教室" が見える） | [画像](./screenshots/ScenarioE2ETest_TC_SC_05______________01_____.png) |
| 2 | 予定行タップ → 編集フォーム | [画像](./screenshots/ScenarioE2ETest_TC_SC_05______________02_______.png) |
| 3 | 削除確認ダイアログ "『サッカー教室』を削除しますか？" | [画像](./screenshots/ScenarioE2ETest_TC_SC_05______________03__________.png) |
| 4 | 削除完了トースト | [画像](./screenshots/ScenarioE2ETest_TC_SC_05______________04_____.png) |
| 5 | 終了時の最終状態 | [画像](./screenshots/ScenarioE2ETest_TC_SC_05______________end.png) |

### その他のシナリオ／画面遷移（最終状態）

- [TC-SC-02 長女の夜の予定（end）](./screenshots/ScenarioE2ETest_TC_SC_02___________end.png)
- [TC-SC-04 次女の誤操作防止（end）（FR-23）](./screenshots/ScenarioE2ETest_TC_SC_04_____________end.png)
- [T01 初回起動はS01](./screenshots/ScreenTransitionE2ETest_T01______S01___end.png)
- [T02 LocalStorageありはS02](./screenshots/ScreenTransitionE2ETest_T02_LocalStorage___S02___end.png)
- [T03 名前ボタンタップでS02](./screenshots/ScreenTransitionE2ETest_T03__________S02___end.png)
- [T08 +追加でS03モーダル](./screenshots/ScreenTransitionE2ETest_T08_______S03________end.png)
- [T14 今日に戻る](./screenshots/ScreenTransitionE2ETest_T14____________end.png)
- [T17 キャンセルでS02に戻る](./screenshots/ScreenTransitionE2ETest_T17_______S02______end.png)

---

## 7. E2E 実行動画（Playwright 録画）

各 E2E テストを実行した様子を WebM で録画しています。ファイルは [`videos/`](./videos/) 配下、
ファイル名→日本語テスト名の対応は [`videos/_map.tsv`](./videos/_map.tsv) を参照してください。
WebM はモダンブラウザ（Chrome / Safari / Edge / Firefox）で再生可能です。

### シナリオテスト（TC-SC）

| テスト | 動画 | 検証している要件 |
|---|---|---|
| TC-SC-01 お母さんの朝 | [動画](./videos/ScenarioE2ETest_TC_SC_01_________.webm) | FR-20（利用者記憶）、FR-21（3ステップ登録）、FR-22（誰が1タップ）、FR-16（トースト） |
| TC-SC-02 長女の夜の予定 | [動画](./videos/ScenarioE2ETest_TC_SC_02__________.webm) | FR-07〜10（登録）、FR-16（トースト） |
| TC-SC-04 次女の誤操作防止 | [動画](./videos/ScenarioE2ETest_TC_SC_04____________.webm) | FR-23（長押しで編集フォームを開かない） |
| TC-SC-05 削除と確認ダイアログ | [動画](./videos/ScenarioE2ETest_TC_SC_05_____________.webm) | FR-13（削除）、FR-17（対象内容を含む確認）、FR-19（削除ボタンの視覚区別）、FR-16（トースト） |

### 画面遷移テスト（TC-ST）

| テスト | 動画 | 遷移 |
|---|---|---|
| T01 初回起動はS01 | [動画](./videos/ScreenTransitionE2ETest_T01______S01__.webm) | 起動 → S-01 |
| T02 LocalStorageありはS02 | [動画](./videos/ScreenTransitionE2ETest_T02_LocalStorage___S02__.webm) | 起動 → S-02 直行（FR-20） |
| T03 名前ボタンタップでS02 | [動画](./videos/ScreenTransitionE2ETest_T03__________S02__.webm) | S-01 → S-02 |
| T08 追加ボタンでS03モーダル | [動画](./videos/ScreenTransitionE2ETest_T08_______S03_______.webm) | S-02 → S-03 |
| T14 今日に戻るボタン | [動画](./videos/ScreenTransitionE2ETest_T14___________.webm) | S-02 内で viewDate リセット |
| T17 キャンセルでS02に戻る | [動画](./videos/ScreenTransitionE2ETest_T17_______S02_____.webm) | S-03 → S-02 |

動画の仕様：
- 解像度：390 × 844（iPhone 13 縦）
- フォーマット：WebM (VP8)
- 合計サイズ：約 290 KB（10本）
- 録画には ffmpeg が必要。Playwright 同梱のものを使用

---

## 8. 要件と証跡のトレーサビリティ

| 要件ID | 内容 | 証跡 |
|---|---|---|
| FR-01〜02 | 5人の利用者選択／セッション保持 | `MembersApiIT`、`ScreenTransitionE2ETest.T01/T03` |
| FR-03〜06 | 今日と明日の予定をグリッド表示 | `SchedulesApiIT.TC_API_02_*`、`ScenarioFlowIT.TC_IT_04_*` |
| FR-07〜10 | 予定登録、バリデーション | `ScheduleValidatorTest`、`SchedulesApiIT.TC_API_03_*`、`ScenarioE2ETest.TC_SC_01` |
| FR-11〜12 | 予定編集 | `SchedulesApiIT.TC_API_04_*`、`ScenarioFlowIT.TC_IT_02_*` |
| FR-13 | 予定削除と確認ダイアログ | `SchedulesApiIT.TC_API_05_*`、`ScenarioE2ETest.TC_SC_05` |
| FR-14〜15 | フリックと「今日に戻る」 | `ScreenTransitionE2ETest.T14`、`ScenarioFlowIT.TC_IT_05_1` |
| FR-16 | 保存／更新／削除でトースト | `ScenarioE2ETest.TC_SC_01/05`（スクリーンショット） |
| FR-17 | 削除確認ダイアログに対象内容を含める | `ScenarioE2ETest.TC_SC_05`（スクリーンショット 03） |
| FR-18 | 内容100文字制限（コードポイント基準） | `ScheduleValidatorTest`、`SchedulesApiIT.TC_API_03_4/5/6`（境界値・絵文字含む） |
| FR-19 | 削除ボタンを保存／キャンセルと視覚的に区別 | `ScenarioE2ETest.TC_SC_05`（スクリーンショット 02） |
| FR-20 | 利用者選択は初回のみ、以降記憶 | `ScenarioE2ETest.TC_SC_01`（ステップ3）、`ScreenTransitionE2ETest.T02` |
| FR-21 | 登録は3ステップ以内 | `ScenarioE2ETest.TC_SC_01`（5ステップ目）、`ScenarioFlowIT.TC_SC_01_3step` |
| FR-22 | 「誰が」のボタン並列・1タップ切替 | `ScenarioE2ETest.TC_SC_01`（ステップ5） |
| FR-23 | 長押しで編集フォームを開かない | `ScenarioE2ETest.TC_SC_04` |
| NFR-04 / 04' | 16px / タップ領域44px | `style.css`、視覚的に画面確認（iPhone 13サイズの ViewPort で全件成功） |
| データ整合 | API → DB の整合 | `db_evidence.txt` |

---

## 9. 環境固有の注記

- **テストはローカルの本物の PostgreSQL ではなく、H2（PostgreSQL モード）+ Flyway でセットアップ**しています（テスト高速化のため）。
  本番想定ではPostgreSQLそのものを使い、`mvn spring-boot:run` 経由で `application.yml` (dev profile) もしくは `application-prod.yml` (prod profile) を読みます。
- E2E は **iPhone 13 相当のビューポート（390×844）** で実行。スマホ縦画面での見え方を模倣しています。
- DS-5（DB列を VARCHAR(400) に拡張、文字数バリデーションは Java 側 `codePointCount` で担保）が最終仕様。サロゲートペア絵文字 100文字も保存可能・101文字でエラーとなることを実機・APIで確認しています。

---

## 10. 再現手順

任意のローカル環境で再現するには：

```bash
cd family-schedule
sudo -u postgres pg_ctlcluster 16 main start  # 既に起動済みならスキップ
LANG=C.UTF-8 LC_ALL=C.UTF-8 mvn clean verify
# スクリーンショット: target/evidence/*.png
# 動画:               target/evidence/videos/*.webm
# レポート:           target/{surefire,failsafe}-reports/
```

動画録画には ffmpeg が必要です。Playwright 同梱版（`/opt/pw-browsers/ffmpeg-*/ffmpeg-linux`）
が Playwright のバージョンとずれている場合はシンボリックリンクで解決できます：
```bash
# 例：Playwright 1.49 は ffmpeg-1010 を探すが ffmpeg-1011 しかない環境
sudo ln -s /opt/pw-browsers/ffmpeg-1011 /opt/pw-browsers/ffmpeg-1010
```
