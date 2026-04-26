# 負荷テスト仕様書

- バージョン：v0.1
- 作成日：2026-04-25
- 対象：家族スケジュール共有システム MVP
- 関連要件：NFR-01（1秒以内）、NFR-02（500ms以内）

---

## 1. テスト目標

| NFR | 要件 | 測定対象 |
|---|---|---|
| NFR-01 | スケジュール画面の表示は **1秒以内** | `GET /api/schedules` の応答時間 |
| NFR-02 | 予定の保存・更新・削除は **500ms以内** | `POST / PUT / DELETE /api/schedules` の応答時間 |

---

## 2. テスト環境

| 項目 | 値 |
|---|---|
| 対象サーバー | EC2（本番同等環境）`54.162.107.130:8082` |
| DB | RDS PostgreSQL 18.3 |
| 実行端末 | ローカル PC（テスト実行元） |
| ツール | [k6](https://k6.io/) v0.50以上 |
| 事前データ | メンバー5名・予定50件（後述のシードスクリプトで投入） |

---

## 3. 前提・制約

- 家族5名が対象のシステムのため、**同時接続数は現実的に1〜3**。高負荷シナリオはシステム限界確認を目的とする。
- 認証なし（MVP）のため、全リクエストは認証なしで実行する。
- テスト中は実データを汚染しないよう、専用テスト用DBまたはステージング環境を推奨。

---

## 4. テストシナリオ

### LT-01：通常負荷（Normal Load）

**目的**：日常利用（最大3名同時アクセス）で NFR を満たすことを確認する。

| 項目 | 値 |
|---|---|
| 仮想ユーザー数 | 3 VU |
| 持続時間 | 3分 |
| リクエスト内容 | 閲覧70%・登録20%・削除10% |

```javascript
// k6 スクリプト例：lt01_normal.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = 'http://54.162.107.130:8082';
const HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  vus: 3,
  duration: '3m',
  thresholds: {
    'http_req_duration{name:get_schedules}': ['p(95)<1000'],
    'http_req_duration{name:post_schedule}': ['p(95)<500'],
    'http_req_duration{name:put_schedule}':  ['p(95)<500'],
    'http_req_duration{name:delete_schedule}': ['p(95)<500'],
    'http_req_failed': ['rate<0.01'],
  },
};

export default function () {
  const rand = Math.random();
  const today = new Date().toISOString().slice(0, 10);
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);

  if (rand < 0.70) {
    // 閲覧
    const r = http.get(`${BASE}/api/schedules?from=${today}&to=${tomorrow}`,
      { tags: { name: 'get_schedules' } });
    check(r, { 'status 200': (r) => r.status === 200 });
  } else if (rand < 0.90) {
    // 登録
    const r = http.post(`${BASE}/api/schedules`,
      JSON.stringify({ memberId: 1, date: today, content: '負荷テスト' }),
      { headers: HEADERS, tags: { name: 'post_schedule' } });
    check(r, { 'status 201': (r) => r.status === 201 });
    if (r.status === 201) {
      const id = r.json('id');
      // 後片付け（purge）
      http.post(`${BASE}/api/schedules/${id}/purge`);
    }
  } else {
    // メンバー一覧（画面起動時）
    const r = http.get(`${BASE}/api/members`, { tags: { name: 'get_members' } });
    check(r, { 'status 200': (r) => r.status === 200 });
  }

  sleep(1);
}
```

---

### LT-02：ストレス負荷（Stress Load）

**目的**：全メンバー（5名）が同時に操作するピーク時の応答劣化を確認する。

| 項目 | 値 |
|---|---|
| 仮想ユーザー数 | 5 VU（家族全員同時） |
| 持続時間 | 2分 |
| ランプアップ | 30秒かけて5VUに増加 |

```javascript
// k6 スクリプト例：lt02_stress.js
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = 'http://54.162.107.130:8082';

export const options = {
  stages: [
    { duration: '30s', target: 5 },  // ランプアップ
    { duration: '90s', target: 5 },  // 維持
    { duration: '30s', target: 0 },  // ランプダウン
  ],
  thresholds: {
    'http_req_duration{name:get_schedules}': ['p(95)<1000', 'p(99)<2000'],
    'http_req_duration{name:post_schedule}': ['p(95)<1000'],
    'http_req_failed': ['rate<0.05'],
  },
};

export default function () {
  const today = new Date().toISOString().slice(0, 10);
  const tomorrow = new Date(Date.now() + 86400000).toISOString().slice(0, 10);
  const r = http.get(`${BASE}/api/schedules?from=${today}&to=${tomorrow}`,
    { tags: { name: 'get_schedules' } });
  check(r, { 'status 200': (r) => r.status === 200 });
  sleep(0.5);
}
```

---

### LT-03：スパイク（Spike Test）

**目的**：急激なアクセス増加（例：家族が同時に起動）に対してシステムが回復できるかを確認する。

| 項目 | 値 |
|---|---|
| ベースライン | 1 VU |
| スパイク | 10 VU（30秒間） |
| 回復確認 | スパイク後に 1VU に戻り正常応答することを確認 |

```javascript
// lt03_spike.js
export const options = {
  stages: [
    { duration: '30s', target: 1  },  // ベースライン
    { duration: '10s', target: 10 },  // スパイク
    { duration: '30s', target: 10 },  // スパイク維持
    { duration: '10s', target: 1  },  // 回復
    { duration: '30s', target: 1  },  // 回復後確認
  ],
  thresholds: {
    'http_req_failed': ['rate<0.10'],  // スパイク中10%未満
  },
};
```

---

## 5. 合否判定基準（SLO）

| シナリオ | 指標 | 合格条件 |
|---|---|---|
| LT-01 | `GET /api/schedules` P95 | < 1,000ms |
| LT-01 | `POST/PUT/DELETE /api/schedules` P95 | < 500ms |
| LT-01 | エラー率 | < 1% |
| LT-02 | `GET /api/schedules` P95 | < 1,000ms |
| LT-02 | `GET /api/schedules` P99 | < 2,000ms |
| LT-02 | エラー率 | < 5% |
| LT-03 | スパイク中エラー率 | < 10% |
| LT-03 | 回復後エラー率 | < 1% |

---

## 6. 実行手順

```bash
# k6 のインストール（macOS）
brew install k6

# Windows（Chocolatey）
choco install k6

# 実行
k6 run docs/tests/load/lt01_normal.js
k6 run docs/tests/load/lt02_stress.js
k6 run docs/tests/load/lt03_spike.js

# HTML レポート出力
k6 run --out json=result.json lt01_normal.js
```

---

## 7. 事前シードデータ（SQL）

テスト実行前に以下を本番DBに投入する。

```sql
-- テスト用予定50件（member_id を1〜5に分散）
INSERT INTO schedules (member_id, date, content, created_at, updated_at)
SELECT
  (i % 5) + 1,
  CURRENT_DATE + (i % 14),
  '負荷テスト予定' || i,
  now(), now()
FROM generate_series(1, 50) AS i;
```

**投入方法:**

```bash
# EC2サーバーで直接実行
psql -U postgres -d familydb -c "INSERT INTO schedules ..."

# またはファイルで実行
psql -U postgres -d familydb -f docs/tests/data/seed_at.sql
```

## 7b. テスト後クリーンアップ（SQL）

テスト終了後に以下を実行してシードデータを削除する。

```sql
-- 負荷テスト用シードデータの削除
DELETE FROM schedules WHERE content LIKE '負荷テスト予定%';

-- k6 テスト中の残存データ削除（content が '負荷テスト' または 'ストレステスト' のもの）
DELETE FROM schedules WHERE content IN ('負荷テスト', 'ストレステスト');

-- 論理削除済みで purge されていないレコードの完全削除（任意）
-- DELETE FROM schedules WHERE deleted_at IS NOT NULL;

-- クリーンアップ後の確認
SELECT
  COUNT(*) FILTER (WHERE deleted_at IS NULL) AS "有効なスケジュール数",
  COUNT(*) FILTER (WHERE deleted_at IS NOT NULL) AS "論理削除済み数"
FROM schedules;
```

クリーンアップ用SQLファイルは `docs/tests/data/cleanup_load.sql` にも格納されています。

---

## 8. 結果記録

### 2026-04-26 実施結果

| 項目 | 記録 |
|---|---|
| 実施日時 | 2026-04-26 |
| 実施環境 | EC2 `54.162.107.130:8082` / RDS PostgreSQL / k6 v0.55.0 |
| LT-01 GET P95 | **218ms** ✅（閾値 1,000ms） |
| LT-01 POST P95 | **235ms** ✅（閾値 500ms） |
| LT-01 エラー率 | **0%** ✅（閾値 1%）|
| LT-02 GET P95 | **合格** ✅（exit code 0）|
| LT-02 エラー率 | **合格** ✅（閾値 5%）|
| LT-03 GET P95 | **203ms** ✅（スパイク10VU時も安定）|
| LT-03 エラー率（スパイク中） | **0%** ✅（閾値 10%）|
| LT-03 エラー率（回復後） | **0%** ✅（閾値 1%）|
| 合否判定 | **全シナリオ合格** |
| 備考 | purge は soft-delete 後でないと 404 になる仕様のため、k6スクリプトを DELETE → purge の順に修正して再実行。レスポンスタイムは全項目で NFR を大きく下回った（avg ≈ 195〜210ms）。 |
