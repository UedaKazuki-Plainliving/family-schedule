# システムテスト仕様書

- ドキュメントID：ST-SPEC-001
- バージョン：v1.0
- 作成日：2026-04-26
- 作成者：テスト担当
- 対象システム：家族スケジュール共有システム
- 関連文書：
  - [要件定義書 v0.6](../requirements/requirements.md)
  - [画面詳細仕様書 v0.4](../design/02_screen_spec.md)
  - [API仕様書 v0.2](../design/03_api_spec.md)

---

## 1. テスト方針

### 1.1 目的

本仕様書は、家族スケジュール共有システム（Spring Boot 3.3.4 + バニラJS SPA）のシステムテストを実施するにあたり、テストケース・操作手順・期待結果・証跡管理方法を定義する。

主要な確認観点は以下のとおり：

1. **画面遷移・状態管理**：LocalStorage を用いた利用者選択フロー（FR-20）が正しく動作すること
2. **CRUD操作**：予定の追加・編集・削除がUI操作・API双方で正常に完結すること
3. **バリデーション**：content 100文字制限（FR-18）、メンバー最大10名制限（FR-24）等が適切に機能すること
4. **UNDO機能**：削除後5秒間の元に戻す操作（FR-27）が仕様通りに動作すること
5. **FR-23誤操作防止**：長押しによる誤起動が抑止されていること
6. **セキュリティ・堅牢性**：XSS耐性、壊れたLocalStorageへの耐障害性
7. **既知バグ（BUG-VALIDATOR）**：動作を確認し、ステータスを記録すること

### 1.2 スコープ

| 対象 | 内容 |
|------|------|
| 対象画面 | S-01（利用者選択）、S-02（スケジュール表示）、S-03（予定追加モーダル）、S-04（インライン編集）、S-05（削除確認ダイアログ）、S-06（メンバー管理モーダル） |
| 対象API | `/api/members`、`/api/schedules`（CRUD + restore + purge） |
| テストレベル | システムテスト（E2E） |
| テスト種別 | 機能テスト、バリデーションテスト、操作性テスト、セキュリティテスト、堅牢性テスト |
| 除外範囲 | 認証・認可（BL-06、MVPスコープ外）、性能負荷テスト（NFR-01/02 は手動計測のみ）、複数端末同時編集の競合（NFR-07 より想定外） |

### 1.3 合格基準

| 項目 | 基準 |
|------|------|
| 優先度H テストケース | 全件PASS（既知バグ ST-06-07 は KNOWN_FAIL として記録） |
| 優先度M テストケース | 全件PASS |
| BUG-VALIDATOR（ST-06-07） | バグの再現を確認し、エラー表示内容を記録する |
| セキュリティ（ST-XS-01） | スクリプトが実行されないことを確認 |
| 堅牢性（ST-LS-01） | アプリがクラッシュしないことを確認 |
| リグレッション | 前回PASSしたテストケースが新たにFAILしないこと |

### 1.4 環境前提

| 項目 | 内容 |
|------|------|
| サーバー | Spring Boot 3.3.4 + PostgreSQL（Docker Compose or ローカル起動） |
| ブラウザ | Google Chrome 最新版（PC）、Safari / Chrome（iOS実機またはシミュレータ） |
| 自動テストツール | Playwright for Java（ページオブジェクト不使用の直接操作スタイル） |
| スマホ実機テスト | iPhone SE 相当（375×667px）推奨 |
| ネットワーク | localhost または家庭内LAN |
| 認証 | なし（MVP） |

### 1.5 証跡管理方針

- 各テストケース実行後、スクリーンショットを `docs/evidence/{テストケースID}/` 配下に保存する
- スクリーンショットのファイル名は `{YYYYMMDD}_{テストケースID}_{ステップ番号}_{説明}.png` とする
- 自動テストの場合は Playwright の `page.screenshot()` で取得し、スナップショット箇所は `snap("XX_説明")` として手順中に明示する
- テスト結果は以下のステータスで記録する：`PASS` / `FAIL` / `KNOWN_FAIL` / `SKIP`
- `KNOWN_FAIL` はバグ番号（例：BUG-VALIDATOR）とともに記録する

---

## 2. 環境セットアップ

### 2.1 サーバー起動

```bash
# Docker Compose で起動（推奨）
cd /path/to/family-schedule
docker-compose up -d

# または Maven + ローカル PostgreSQL
./mvnw spring-boot:run
```

起動確認：`http://localhost:8080/api/members` にアクセスし、以下のレスポンスが返ることを確認する。

```json
[
  { "id": 1, "name": "お父さん", "displayOrder": 1 },
  { "id": 2, "name": "お母さん", "displayOrder": 2 },
  { "id": 3, "name": "長女",     "displayOrder": 3 },
  { "id": 4, "name": "次女",     "displayOrder": 4 },
  { "id": 5, "name": "長男",     "displayOrder": 5 }
]
```

### 2.2 Playwright 設定

```javascript
// playwright.config.js（抜粋）
module.exports = {
  use: {
    baseURL: 'http://localhost:8080',
    viewport: { width: 375, height: 667 },  // iPhone SE 相当
    locale: 'ja-JP',
    timezoneId: 'Asia/Tokyo',
  },
  timeout: 30000,
};
```

**ヘルパー関数（各テストファイル先頭に定義）：**

```javascript
// スクリーンショット取得ヘルパー
async function snap(page, name) {
  await page.screenshot({ path: `docs/evidence/${name}.png`, fullPage: false });
}

// LocalStorage に利用者を設定するヘルパー（S-02直行用）
async function setCurrentUser(page, user) {
  // user例: { id: 4, name: "次女", displayOrder: 4 }
  await page.evaluate((u) => {
    localStorage.setItem('familySchedule.currentUser', JSON.stringify(u));
  }, user);
}

// LocalStorage をクリアするヘルパー
async function clearCurrentUser(page) {
  await page.evaluate(() => {
    localStorage.removeItem('familySchedule.currentUser');
  });
}
```

### 2.3 初期データ

各テストケースの前提条件に記載のあるデータは、テスト実行前にAPIまたはUIで投入する。

**APIで予定を登録するスクリプト例：**

```bash
# 長男(id=5)の今日の予定を登録
TODAY=$(date +%Y-%m-%d)
curl -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d "{\"memberId\":5,\"date\":\"${TODAY}\",\"content\":\"閲覧のみ\"}"
```

**テストの独立性に関する注意：** 各テストケースは実行順序に依存しないよう、前提条件の設定（LocalStorage・DB データ）は各テスト開始時に行い、終了後にクリーンアップすること。特にメンバー管理テスト（ST-06系）はメンバー追加・削除がグローバル状態を変更するため、実行後に初期状態（5名構成）に戻すこと。

---

## 3. テスト観点一覧

| 観点ID | 観点名 | 対象画面 | 優先度 |
|--------|--------|----------|--------|
| ST-01-01 | 初回起動でS-01（利用者選択）が表示される | S-01 | H |
| ST-01-02 | S-01にメンバーボタンが全員分表示される | S-01 | H |
| ST-01-03 | メンバーボタンタップでS-02へ遷移し名前が表示される | S-01→S-02 | H |
| ST-01-04 | S-01でボタンタップするとlocalStorageに選択ユーザーが保存される | S-01 | H |
| ST-02-01 | 2回目起動ではS-02（スケジュール画面）に直行する | S-02 | H |
| ST-02-02 | S-02に「今日」「明日」の2カラムが表示される | S-02 | H |
| ST-02-03 | 予定なし時は「予定なし」と表示される | S-02 | M |
| ST-02-04 | 左フリックで翌日に進む | S-02 | H |
| ST-02-05 | 右フリックで前日に戻る | S-02 | H |
| ST-02-06 | 「今日に戻る」ボタンで当日に戻る | S-02 | M |
| ST-02-07 | ユーザー切替ボタンでS-01へ戻る | S-02→S-01 | H |
| ST-03-01 | ＋追加ボタンで予定追加モーダルが開閉する | S-03 | H |
| ST-03-02 | 予定追加モーダルに全メンバーの「誰が」ボタンが表示される | S-03 | H |
| ST-03-03 | モーダル初期値で現在の利用者が選択済みになっている | S-03 | H |
| ST-03-04 | 3ステップ（画面表示→＋追加→保存）で予定追加が完了する | S-03 | H |
| ST-03-05 | content空のまま保存するとエラーメッセージが表示される | S-03 | H |
| ST-03-06 | content 101文字で保存するとエラーメッセージが表示される | S-03 | H |
| ST-04-01 | 自分の予定テキストをタップするとインライン編集モードになる | S-04 | H |
| ST-04-02 | インライン編集中にEnterキーで保存される | S-04 | H |
| ST-04-03 | インライン編集中にEscapeキーでキャンセルされる | S-04 | H |
| ST-04-04 | インライン編集中に入力欄外クリック（blur）で保存される | S-04 | M |
| ST-04-05 | FR-23: 他メンバーの予定は長押し（>300ms）でも編集モードが起動しない | S-04 | H |
| ST-04-06 | FR-23: 他メンバーの予定は短押し（タップ）でも編集モードが起動しない | S-04 | H |
| ST-04-07 | インライン編集中に101文字入力するとエラーが表示され保存されない | S-04 | H |
| ST-05-01 | モーダルの削除ボタンをタップすると削除確認ダイアログが表示される | S-05 | H |
| ST-05-02 | 削除確認ダイアログに予定内容が20文字truncateで表示される | S-05 | M |
| ST-05-03 | 削除確認「はい」で予定が削除されUNDOトーストが表示される | S-05 | H |
| ST-05-04 | 削除確認「いいえ」でキャンセルされ予定が残る | S-05 | H |
| ST-05-05 | ✕ボタンで即削除されUNDOトーストが表示される | S-02/S-05 | H |
| ST-05-06 | UNDOトースト「元に戻す」タップで削除が復元される | S-02/S-05 | H |
| ST-05-07 | UNDOトーストが5秒後に自動消滅しpurgeされる | S-02/S-05 | H |
| ST-06-01 | ⚙ボタンでメンバー管理モーダルが開閉する | S-06 | H |
| ST-06-02 | メンバーを新規追加できる | S-06 | H |
| ST-06-03 | メンバー名を変更できる（変更ボタン→入力→Enter） | S-06 | H |
| ST-06-04 | 予定のないメンバーを削除できる | S-06 | H |
| ST-06-05 | 予定のあるメンバーを削除しようとするとエラーが表示される | S-06 | H |
| ST-06-06 | 10名到達時にメンバー追加フォームが非表示になる | S-06 | H |
| ST-06-07 | BUG-VALIDATOR: 追加メンバー（id=6以降）への予定登録がAPIで400になる | S-03/S-06 | H |
| ST-XS-01 | XSS: scriptタグを含むcontent・メンバー名がスクリプトとして実行されない | 全画面 | H |
| ST-LS-01 | 壊れたlocalStorageの値が存在してもアプリが正常起動する | S-01/S-02 | H |

---

## 4. 画面別テストケース

### 4.1 S-01 利用者選択画面

#### ST-01-01 初回起動でS-01（利用者選択画面）が表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-01-01 |
| 機能要件 | FR-20 |
| 対象画面 | S-01 |
| 前提条件 | `familySchedule.currentUser` キーがLocalStorageに存在しない（初回起動状態） |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | ブラウザのLocalStorageから `familySchedule.currentUser` を削除し、`http://localhost:8080` を開く | S-01（利用者選択画面）が表示される |
| 2 | 画面の表示内容を確認する | `#screen-select-user` が visible であり、`#screen-schedule` は非表示 |
| 3 | タイトルと案内テキストを確認する | 「家族スケジュール」「あなたは誰？」が表示されている |

`snap("ST-01-01_S01表示確認")`

Playwright操作例:
```javascript
test('ST-01-01: 初回起動でS-01が表示される', async ({ page }) => {
  await clearCurrentUser(page);
  await page.goto('/');
  await expect(page.locator('#screen-select-user')).toBeVisible();
  await expect(page.locator('#screen-schedule')).toBeHidden();
  await snap(page, 'ST-01-01_S01表示確認');
});
```

---

#### ST-01-02 S-01にメンバーボタンが全員分表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-01-02 |
| 機能要件 | FR-01 |
| 対象画面 | S-01 |
| 前提条件 | LocalStorageなし。サーバー初期データ（5名）が存在する |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-01を表示する（ST-01-01の手順1に同じ） | S-01が表示される |
| 2 | `#member-btns` 内のボタン一覧を確認する | 「お父さん」「お母さん」「長女」「次女」「長男」の5ボタンが表示されている |
| 3 | 各ボタンをタップ可能な大きさ（44×44px以上）であることを確認する | タップ領域が十分に確保されている |

`snap("ST-01-02_メンバーボタン一覧")`

Playwright操作例:
```javascript
test('ST-01-02: メンバーボタン全員分表示', async ({ page }) => {
  await clearCurrentUser(page);
  await page.goto('/');
  const buttons = page.locator('#member-btns .member-btn');
  await expect(buttons).toHaveCount(5);
  const names = ['お父さん', 'お母さん', '長女', '次女', '長男'];
  for (const name of names) {
    await expect(page.locator('#member-btns .member-btn', { hasText: name })).toBeVisible();
  }
  await snap(page, 'ST-01-02_メンバーボタン一覧');
});
```

---

#### ST-01-03 メンバーボタンタップでS-02に遷移し名前が表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-01-03 |
| 機能要件 | FR-01, FR-02, FR-20 |
| 対象画面 | S-01 → S-02 |
| 前提条件 | LocalStorageなし |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-01を表示する | 利用者選択画面が表示される |
| 2 | 「お母さん」ボタンをタップする | S-02（スケジュール画面）に遷移する |
| 3 | ヘッダ左のユーザーボタンを確認する | `#current-user-name` に「お母さん」が表示されている |
| 4 | `#screen-select-user` の表示状態を確認する | S-01が非表示になっている |

`snap("ST-01-03_S02遷移後")`

Playwright操作例:
```javascript
test('ST-01-03: メンバーボタンタップでS-02へ遷移', async ({ page }) => {
  await clearCurrentUser(page);
  await page.goto('/');
  await page.locator('#member-btns .member-btn', { hasText: 'お母さん' }).click();
  await expect(page.locator('#screen-schedule')).toBeVisible();
  await expect(page.locator('#screen-select-user')).toBeHidden();
  await expect(page.locator('#current-user-name')).toHaveText('お母さん');
  await snap(page, 'ST-01-03_S02遷移後');
});
```

---

#### ST-01-04 S-01でボタンタップするとLocalStorageに選択ユーザーが保存されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-01-04 |
| 機能要件 | FR-02, FR-20 |
| 対象画面 | S-01 |
| 前提条件 | LocalStorageなし |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-01を表示する | 利用者選択画面が表示される |
| 2 | 「次女」ボタンをタップする | S-02へ遷移する |
| 3 | ブラウザのDevToolsでLocalStorageを確認する | `familySchedule.currentUser` に `{"id":4,"name":"次女","displayOrder":4}` 相当のJSONが保存されている |

Playwright操作例:
```javascript
test('ST-01-04: LocalStorageへの保存確認', async ({ page }) => {
  await clearCurrentUser(page);
  await page.goto('/');
  await page.locator('#member-btns .member-btn', { hasText: '次女' }).click();
  const stored = await page.evaluate(() =>
    localStorage.getItem('familySchedule.currentUser')
  );
  const user = JSON.parse(stored);
  expect(user.name).toBe('次女');
  expect(user.id).toBe(4);
});
```

---

### 4.2 S-02 スケジュール表示画面

#### ST-02-01 2回目起動でS-02（スケジュール画面）に直行すること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-02-01 |
| 機能要件 | FR-20 |
| 対象画面 | S-02 |
| 前提条件 | `familySchedule.currentUser` がLocalStorageに設定済み（お父さん: id=1） |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | LocalStorageに `{"id":1,"name":"お父さん","displayOrder":1}` を設定した後、`http://localhost:8080` を開く | S-01をスキップし、直接S-02が表示される |
| 2 | 表示状態を確認する | `#screen-schedule` が visible、`#screen-select-user` は非表示 |
| 3 | ヘッダのユーザー名を確認する | 「お父さん」が表示されている |

`snap("ST-02-01_S02直行確認")`

Playwright操作例:
```javascript
test('ST-02-01: 2回目起動でS-02直行', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await expect(page.locator('#screen-schedule')).toBeVisible();
  await expect(page.locator('#screen-select-user')).toBeHidden();
  await snap(page, 'ST-02-01_S02直行確認');
});
```

---

#### ST-02-02 S-02に「今日」「明日」の2カラムが表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-02-02 |
| 機能要件 | FR-03, FR-04 |
| 対象画面 | S-02 |
| 前提条件 | LocalStorageにお父さん設定済み |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02を表示する | スケジュールグリッドが表示される |
| 2 | 日付ラベル（`#date-today`, `#date-tomorrow`）を確認する | 今日の日付と翌日の日付が表示されている |
| 3 | グリッドの列数を確認する | 今日列・明日列の2カラム構成になっている |
| 4 | メンバー行の数を確認する | 全登録メンバー（初期5名）分の行がある |

`snap("ST-02-02_2カラム表示")`

Playwright操作例:
```javascript
test('ST-02-02: 2カラム（今日・明日）表示', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await expect(page.locator('#date-today')).toBeVisible();
  await expect(page.locator('#date-tomorrow')).toBeVisible();
  await expect(page.locator('#schedule-grid')).toBeVisible();
  await snap(page, 'ST-02-02_2カラム表示');
});
```

---

#### ST-02-03 予定なし時に「予定なし」と表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-02-03 |
| 機能要件 | FR-05 |
| 対象画面 | S-02 |
| 前提条件 | LocalStorageにお父さん設定済み。対象日の全メンバーに予定が登録されていない |
| 優先度 | M |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02を表示する | スケジュール画面が表示される |
| 2 | 予定が登録されていないメンバーのセルを確認する | 「予定なし」のテキストが表示されている |
| 3 | APIレスポンスを確認する（DevToolsのNetworkタブ） | `GET /api/schedules` が200を返し、対象メンバー・日付のデータがない場合はUIが「予定なし」を表示している |

`snap("ST-02-03_予定なし表示")`

---

#### ST-02-04 左フリックで翌日に進むこと

| 項目 | 内容 |
|------|------|
| テストケースID | ST-02-04 |
| 機能要件 | FR-14 |
| 対象画面 | S-02 |
| 前提条件 | LocalStorageにお父さん設定済み。S-02が表示されている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02を表示し、現在の日付ラベルを確認する | 「今日」と「明日」の日付が表示されている |
| 2 | 画面上で左方向に水平30px以上・垂直20px以下のフリック操作を行う | 表示日付が1日進む（元の「明日」が新しい「今日」になる） |
| 3 | 日付ラベルを再確認する | フリック前の「明日」の日付が左列に表示されている |

`snap("ST-02-04_フリック左後")`

Playwright操作例:
```javascript
test('ST-02-04: 左フリックで翌日に進む', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  const beforeDate = await page.locator('#date-today').textContent();
  // 左フリック操作：touchstart→touchmove→touchend
  await page.locator('#schedule-grid').dispatchEvent('touchstart', {
    touches: [{ clientX: 300, clientY: 300 }]
  });
  await page.locator('#schedule-grid').dispatchEvent('touchmove', {
    touches: [{ clientX: 250, clientY: 300 }]
  });
  await page.locator('#schedule-grid').dispatchEvent('touchend', {
    changedTouches: [{ clientX: 250, clientY: 300 }]
  });
  // マウスドラッグ代替
  await page.mouse.move(300, 300);
  await page.mouse.down();
  await page.mouse.move(240, 300);
  await page.mouse.up();
  await page.waitForTimeout(300);
  const afterDate = await page.locator('#date-today').textContent();
  expect(afterDate).not.toBe(beforeDate);
  await snap(page, 'ST-02-04_フリック左後');
});
```

---

#### ST-02-05 右フリックで前日に戻ること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-02-05 |
| 機能要件 | FR-14 |
| 対象画面 | S-02 |
| 前提条件 | LocalStorageにお父さん設定済み。S-02が表示されている。1日以上翌日に進んだ状態 |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02を表示し、左フリックで1日進める | 翌日の表示になる |
| 2 | 現在の日付ラベルを確認・メモする | 翌日の日付が「今日」列に表示されている |
| 3 | 画面上で右方向に水平30px以上・垂直20px以下のフリック操作を行う | 表示日付が1日戻る |
| 4 | 日付ラベルを再確認する | 元の今日の日付に戻っている |

`snap("ST-02-05_フリック右後")`

---

#### ST-02-06 「今日に戻る」ボタンで当日表示に戻ること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-02-06 |
| 機能要件 | FR-15 |
| 対象画面 | S-02 |
| 前提条件 | LocalStorageにお父さん設定済み。フリックで数日先の日付を表示している状態 |
| 優先度 | M |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 左フリックを複数回行い、今日から離れた日付を表示する | 今日とは異なる日付が表示される |
| 2 | `#btn-today` ボタン（「今日に戻る」）をタップする | 今日・明日の日付表示に戻る |
| 3 | 日付ラベルを確認する | `#date-today` に本日の日付が表示されている |

`snap("ST-02-06_今日ボタン後")`

Playwright操作例:
```javascript
test('ST-02-06: 今日ボタンで当日戻り', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  // 複数回フリックで日付を進める（マウスドラッグ代替）
  for (let i = 0; i < 3; i++) {
    await page.mouse.move(300, 300);
    await page.mouse.down();
    await page.mouse.move(240, 300);
    await page.mouse.up();
    await page.waitForTimeout(200);
  }
  await page.locator('#btn-today').click();
  const todayText = await page.locator('#date-today').textContent();
  const today = new Date().toLocaleDateString('ja-JP', { month: 'numeric', day: 'numeric' });
  expect(todayText).toContain(today.replace(/\//g, '/'));
  await snap(page, 'ST-02-06_今日ボタン後');
});
```

---

#### ST-02-07 ユーザー切替ボタンでS-01へ戻ること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-02-07 |
| 機能要件 | FR-25 |
| 対象画面 | S-02 → S-01 |
| 前提条件 | LocalStorageにお父さん設定済み。S-02が表示されている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02を表示する | ヘッダに「👤 お父さん」が表示されている |
| 2 | `#btn-switch-user` ボタン（「👤 お父さん」）をタップする | S-01（利用者選択画面）に遷移する |
| 3 | S-01の表示を確認する | `#screen-select-user` が表示されている |
| 4 | LocalStorageを確認する | `familySchedule.currentUser` が削除されている |

`snap("ST-02-07_S01戻り確認")`

Playwright操作例:
```javascript
test('ST-02-07: ユーザー切替でS-01へ', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await page.locator('#btn-switch-user').click();
  await expect(page.locator('#screen-select-user')).toBeVisible();
  const stored = await page.evaluate(() =>
    localStorage.getItem('familySchedule.currentUser')
  );
  expect(stored).toBeNull();
  await snap(page, 'ST-02-07_S01戻り確認');
});
```

---

### 4.3 S-03 予定追加モーダル

#### ST-03-01 ＋追加ボタンで予定追加モーダルが開閉すること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-03-01 |
| 機能要件 | FR-07 |
| 対象画面 | S-03 |
| 前提条件 | LocalStorageにお父さん設定済み。S-02が表示されている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | `#btn-add`（＋追加 FABボタン）をタップする | `#modal` が表示される |
| 2 | モーダルタイトルを確認する | 「予定を追加」が表示されている |
| 3 | `#btn-cancel`（キャンセルボタン）をタップする | `#modal` が閉じる |
| 4 | モーダルの状態を確認する | モーダルが非表示になっている |

`snap("ST-03-01_モーダル開")` / `snap("ST-03-01_モーダル閉")`

Playwright操作例:
```javascript
test('ST-03-01: +ボタンでモーダル開閉', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await page.locator('#btn-add').click();
  await expect(page.locator('#modal')).toBeVisible();
  await snap(page, 'ST-03-01_モーダル開');
  await page.locator('#btn-cancel').click();
  await expect(page.locator('#modal')).toBeHidden();
  await snap(page, 'ST-03-01_モーダル閉');
});
```

---

#### ST-03-02 予定追加モーダルに全メンバーの「誰が」ボタンが表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-03-02 |
| 機能要件 | FR-08, FR-22 |
| 対象画面 | S-03 |
| 前提条件 | LocalStorageにお父さん設定済み。S-03が開いている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | `#btn-add` をタップしてモーダルを開く | モーダルが表示される |
| 2 | `#who-btns` 内のボタン一覧を確認する | 全メンバー分（初期5名）のボタンが横並びで表示されている |
| 3 | 各ボタンが1タップで選択切替できることを確認する（FR-22） | ボタンをタップすると選択状態に切り替わる |

`snap("ST-03-02_誰がボタン一覧")`

Playwright操作例:
```javascript
test('ST-03-02: 誰がボタン全員表示', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await page.locator('#btn-add').click();
  const whoButtons = page.locator('#who-btns button');
  await expect(whoButtons).toHaveCount(5);
  await snap(page, 'ST-03-02_誰がボタン一覧');
});
```

---

#### ST-03-03 モーダル初期値で現在の利用者が選択済みになっていること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-03-03 |
| 機能要件 | FR-09 |
| 対象画面 | S-03 |
| 前提条件 | LocalStorageに次女（id=4）設定済み |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02を表示し、`#btn-add` をタップしてモーダルを開く | モーダルが表示される |
| 2 | `#who-btns` 内の選択状態を確認する | 「次女」ボタンが選択状態（選択中は色反転などの視覚的区別）になっている |
| 3 | `#date-input` の初期値を確認する | 表示中の日付がセットされている |

`snap("ST-03-03_デフォルト自分選択")`

---

#### ST-03-04 3ステップで予定追加が完了すること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-03-04 |
| 機能要件 | FR-07, FR-08, FR-21 |
| 対象画面 | S-02 → S-03 |
| 前提条件 | LocalStorageにお父さん（id=1）設定済み |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02が表示されている | スケジュール画面が表示されている（ステップ1：画面表示） |
| 2 | `#btn-add`（＋追加）をタップする | モーダルが開く（ステップ2：＋追加） |
| 3 | `#content-input` に「在宅勤務」と入力する | テキストエリアに文字が入力される |
| 4 | `#btn-save`（保存）をタップする | モーダルが閉じ、`POST /api/schedules` が201で返る（ステップ3：保存） |
| 5 | S-02のグリッドを確認する | お父さんの今日のセルに「在宅勤務」が追加されている |
| 6 | トーストメッセージを確認する | 「保存しました」が表示される |

`snap("ST-03-04_保存後グリッド確認")`

Playwright操作例:
```javascript
test('ST-03-04: 3ステップで予定追加完了', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  // ステップ1: S-02表示
  await expect(page.locator('#screen-schedule')).toBeVisible();
  // ステップ2: +追加
  await page.locator('#btn-add').click();
  await expect(page.locator('#modal')).toBeVisible();
  // ステップ3: 保存
  await page.locator('#content-input').fill('在宅勤務');
  await page.locator('#btn-save').click();
  await expect(page.locator('#modal')).toBeHidden();
  // グリッドに反映されていることを確認
  await expect(page.locator('#schedule-grid')).toContainText('在宅勤務');
  await snap(page, 'ST-03-04_保存後グリッド確認');
});
```

---

#### ST-03-05 content空のまま保存するとエラーメッセージが表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-03-05 |
| 機能要件 | FR-10 |
| 対象画面 | S-03 |
| 前提条件 | LocalStorageにお父さん設定済み。モーダルが開いている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | モーダルを開く | モーダルが表示される |
| 2 | `#content-input` を空のまま `#btn-save` をタップする | モーダルが閉じずに `#error-msg` が表示される |
| 3 | エラーメッセージの内容を確認する | 「内容を入力してください」が表示されている |
| 4 | `POST /api/schedules` が呼ばれていないことを確認する（DevTools Network） | APIリクエストが発行されていない |

`snap("ST-03-05_空コンテンツエラー")`

Playwright操作例:
```javascript
test('ST-03-05: content空でエラー表示', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await page.locator('#btn-add').click();
  // content-inputを空のまま保存
  await page.locator('#btn-save').click();
  await expect(page.locator('#error-msg')).toBeVisible();
  await expect(page.locator('#error-msg')).toContainText('内容を入力してください');
  await expect(page.locator('#modal')).toBeVisible();
  await snap(page, 'ST-03-05_空コンテンツエラー');
});
```

---

#### ST-03-06 content 101文字で保存するとエラーメッセージが表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-03-06 |
| 機能要件 | FR-18 |
| 対象画面 | S-03 |
| 前提条件 | LocalStorageにお父さん設定済み。モーダルが開いている |
| 優先度 | H |

**注意：** JSのバリデーションは `codePointLength > 100`（Unicodeコードポイント単位）で判定する。日本語の絵文字や結合文字を含む場合、文字数計算に注意が必要。

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | モーダルを開く | モーダルが表示される |
| 2 | `#content-input` に101文字のテキスト（例：「あ」×101文字）を入力する | テキストエリアに101文字が入力される |
| 3 | `#btn-save` をタップする | `#error-msg` が表示される |
| 4 | エラーメッセージを確認する | 「内容は100文字以内で入力してください」が表示されている |
| 5 | `#content-input` を100文字に修正して `#btn-save` をタップする | 保存が成功し、モーダルが閉じる |

`snap("ST-03-06_101文字エラー")` / `snap("ST-03-06_100文字保存成功")`

Playwright操作例:
```javascript
test('ST-03-06: content 101文字でエラー', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await page.locator('#btn-add').click();
  const over100 = 'あ'.repeat(101);
  await page.locator('#content-input').fill(over100);
  await page.locator('#btn-save').click();
  await expect(page.locator('#error-msg')).toBeVisible();
  await expect(page.locator('#error-msg')).toContainText('100文字以内');
  await snap(page, 'ST-03-06_101文字エラー');
  // 100文字に修正して保存成功を確認
  await page.locator('#content-input').fill('あ'.repeat(100));
  await page.locator('#btn-save').click();
  await expect(page.locator('#modal')).toBeHidden();
  await snap(page, 'ST-03-06_100文字保存成功');
});
```

---

### 4.4 S-04 インライン編集

#### ST-04-01 自分の予定テキストをタップするとインライン編集モードになること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-04-01 |
| 機能要件 | FR-11 |
| 対象画面 | S-04 |
| 前提条件 | LocalStorageに長女（id=3）設定済み。長女の今日の予定「部活」が登録済み |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02を表示する | 長女のセルに「部活」が表示されている |
| 2 | 「部活」のテキスト（`.schedule-item-text`）をタップ（短押し）する | 同じ位置に `.schedule-item-input`（textarea）が表示される |
| 3 | textareaの内容を確認する | 「部活」が入力済み状態になっている |
| 4 | textareaに青枠が表示されていることを確認する（`.schedule-item-editing`） | 編集中の視覚的フィードバックがある |

`snap("ST-04-01_インライン編集開始")`

Playwright操作例:
```javascript
test('ST-04-01: 自分の予定タップで編集開始', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 3, name: '長女', displayOrder: 3 });
  await page.reload();
  // 長女の予定「部活」が表示されているセルを取得
  const itemText = page.locator('.schedule-item-text', { hasText: '部活' }).first();
  await itemText.click();
  // textareaが表示されることを確認
  const editInput = page.locator('.schedule-item-input').first();
  await expect(editInput).toBeVisible();
  await expect(editInput).toHaveValue('部活');
  await snap(page, 'ST-04-01_インライン編集開始');
});
```

---

#### ST-04-02 インライン編集中にEnterキーで保存されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-04-02 |
| 機能要件 | FR-26 |
| 対象画面 | S-04 |
| 前提条件 | LocalStorageに長女設定済み。長女の今日の予定「部活」が登録済み。インライン編集モード |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 長女の「部活」をタップしてインライン編集モードに入る | textareaが表示され「部活」が入力されている |
| 2 | テキストを「部活（試合）」に変更する | テキストエリアの内容が変更される |
| 3 | Enterキー（単独）を押す | テキストエリアが閉じ、`PUT /api/schedules/{id}` が呼ばれる |
| 4 | セルの表示を確認する | 「部活（試合）」に更新されている |
| 5 | トーストメッセージを確認する | 「更新しました」が表示される |

`snap("ST-04-02_Enter保存後")`

Playwright操作例:
```javascript
test('ST-04-02: Enter保存', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 3, name: '長女', displayOrder: 3 });
  await page.reload();
  const itemText = page.locator('.schedule-item-text', { hasText: '部活' }).first();
  await itemText.click();
  const editInput = page.locator('.schedule-item-input').first();
  await editInput.fill('部活（試合）');
  await editInput.press('Enter');
  await page.waitForTimeout(500);
  await expect(page.locator('.schedule-item-text', { hasText: '部活（試合）' })).toBeVisible();
  await snap(page, 'ST-04-02_Enter保存後');
});
```

---

#### ST-04-03 インライン編集中にEscapeキーでキャンセルされること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-04-03 |
| 機能要件 | FR-26 |
| 対象画面 | S-04 |
| 前提条件 | LocalStorageに長女設定済み。長女の今日の予定「部活」が登録済み |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 「部活」をタップしてインライン編集モードに入る | textareaに「部活」が表示される |
| 2 | テキストを「部活キャンセルテスト」に変更する | テキストエリアの内容が変更される |
| 3 | Escapeキーを押す | textareaが閉じる |
| 4 | セルの表示を確認する | 元の「部活」に戻っている（APIは呼ばれない） |

`snap("ST-04-03_Escape後")`

Playwright操作例:
```javascript
test('ST-04-03: Escapeキャンセル', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 3, name: '長女', displayOrder: 3 });
  await page.reload();
  const itemText = page.locator('.schedule-item-text', { hasText: '部活' }).first();
  await itemText.click();
  const editInput = page.locator('.schedule-item-input').first();
  await editInput.fill('部活キャンセルテスト');
  await editInput.press('Escape');
  await page.waitForTimeout(300);
  await expect(page.locator('.schedule-item-text', { hasText: '部活' }).first()).toBeVisible();
  await expect(page.locator('.schedule-item-text', { hasText: '部活キャンセルテスト' })).toHaveCount(0);
  await snap(page, 'ST-04-03_Escape後');
});
```

---

#### ST-04-04 インライン編集中に入力欄外クリック（blur）で保存されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-04-04 |
| 機能要件 | FR-26（blurイベント） |
| 対象画面 | S-04 |
| 前提条件 | LocalStorageに長女設定済み。長女の今日の予定「部活」が登録済み |
| 優先度 | M |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 「部活」をタップしてインライン編集モードに入る | textareaが表示される |
| 2 | テキストを「部活（blur保存）」に変更する | テキストエリアの内容が変更される |
| 3 | 入力欄の外側（別のセル）をクリックする | textareaが閉じ、`PUT /api/schedules/{id}` が呼ばれる |
| 4 | セルを確認する | 「部活（blur保存）」に更新されている |

**注意：** 内容が変更なし、または空の場合はAPIを呼ばずキャンセル扱いとなることも確認すること。

`snap("ST-04-04_blur保存後")`

---

#### ST-04-05 FR-23: 他メンバーの予定は長押し（>300ms）でも編集モードが起動しないこと

| 項目 | 内容 |
|------|------|
| テストケースID | ST-04-05 |
| 機能要件 | FR-23 |
| 対象画面 | S-02 / S-04 |
| 前提条件 | LocalStorageに次女（id=4）設定済み。長男（id=5）の今日の予定「閲覧のみ」が登録済み |
| 優先度 | H |

**FR-23の仕様解釈：** 画面詳細仕様書では「長押し（>300ms）ではインライン編集モードにならない」と定義されている。ただし、通常のタップ（短押し）でも他メンバーの予定が編集モードになるべきかどうかについては、仕様書上の記述が「タップ以外では編集フォームを開かない（誤操作防止）」（FR-23）と「他メンバーの予定のタップ（短押し）でもインライン編集を起動しない」（ST-04-06）で区別されており、実装確認が必要。

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 次女でログイン済みのS-02を表示する | 長男の予定「閲覧のみ」が表示されている |
| 2 | 長男の「閲覧のみ」テキスト上で長押し（300ms以上）を行う | textareaが表示されない（インライン編集モードが起動しない） |
| 3 | 長男のセルの表示を確認する | `.schedule-item-input` が出現していない |
| 4 | コンテキストメニューイベントも無反応であることを確認する | 編集モードが起動しない |

`snap("ST-04-05_長押し編集不可確認")`

Playwright操作例:
```javascript
test('ST-04-05: 他メンバーは長押しで編集不可', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 4, name: '次女', displayOrder: 4 });
  await page.reload();
  // 長男の予定アイテムを取得（data-member-idで絞り込み）
  const otherItem = page.locator('[data-member-id="5"] .schedule-item-text').first();
  // 長押し（>300ms）
  const box = await otherItem.boundingBox();
  await page.mouse.move(box.x + box.width / 2, box.y + box.height / 2);
  await page.mouse.down();
  await page.waitForTimeout(400);  // 400ms保持
  await page.mouse.up();
  // 編集モードが起動しないことを確認
  await expect(page.locator('[data-member-id="5"] .schedule-item-input')).toHaveCount(0);
  await snap(page, 'ST-04-05_長押し編集不可確認');
});
```

---

#### ST-04-06 FR-23: 他メンバーの予定は短押し（タップ）でも編集モードが起動しないこと

| 項目 | 内容 |
|------|------|
| テストケースID | ST-04-06 |
| 機能要件 | FR-23 |
| 対象画面 | S-02 / S-04 |
| 前提条件 | LocalStorageに次女（id=4）設定済み。長男（id=5）の今日の予定「閲覧のみ」が登録済み |
| 優先度 | H |

**FR-23の曖昧さに関する注記：** FR-23の原文は「予定のタップ以外では編集フォームを開かない」とある。しかし「✕削除ボタン（`.schedule-item-delete`）による削除は他メンバーの予定に対しても可能」とする挙動が確認されており、これが仕様なのか欠陥なのかは確認が必要。本テストケースでは「他メンバーの予定を短押しタップしても編集モードが起動しないこと」のみを検証する。✕ボタンについては ST-05-05 で別途確認する。

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 次女でログイン済みのS-02を表示する | 長男の予定「閲覧のみ」が表示されている |
| 2 | 長男の「閲覧のみ」テキスト（`.schedule-item-text`）を短押し（通常のタップ）する | textareaが表示されない（インライン編集モードが起動しない） |
| 3 | 長男のセルの表示を確認する | 予定テキストが通常表示のままである |

`snap("ST-04-06_他メンバー短押し編集不可")`

Playwright操作例:
```javascript
test('ST-04-06: 他メンバーは短押しでも編集不可', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 4, name: '次女', displayOrder: 4 });
  await page.reload();
  const otherItem = page.locator('[data-member-id="5"] .schedule-item-text').first();
  await otherItem.click();
  // 編集モードが起動しないことを確認
  await expect(page.locator('[data-member-id="5"] .schedule-item-input')).toHaveCount(0);
  await snap(page, 'ST-04-06_他メンバー短押し編集不可');
});
```

#### ST-04-07 インライン編集中に101文字入力するとエラーが表示され保存されないこと

| 項目 | 内容 |
|------|------|
| テストケースID | ST-04-07 |
| 機能要件 | content最大100コードポイント制限（FR相当） |
| 対象画面 | S-04（インライン編集） |
| 前提条件 | LocalStorageにお父さん（id=1）設定済み。お父さんの今日の予定「既存の予定」が登録済み |
| 優先度 | H |

**UI操作手順**:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | アプリを起動（S-02表示） | お父さんの「既存の予定」が表示される |
| 2 | 「既存の予定」をタップしてインライン編集を開始する | `.schedule-item-input`（textarea）が表示される |
| 3 | 入力欄の内容を全選択してから「あ」を101文字入力する | テキストエリアに101文字が入力される |
| 4 | Enterキーを押す | `#error-msg` 相当のエラーメッセージが表示される（「内容は100文字以内で入力してください」） |
| 5 | 予定一覧を確認する | 元の「既存の予定」のテキストのまま変更されていないこと |

`snap("ST-04-07_インライン編集101文字エラー")`

**Playwright操作例**:

```javascript
test('ST-04-07: インライン編集101文字バリデーション', async ({ page }) => {
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  // 予定登録（APIで）
  await page.request.post('/api/schedules', {
    data: { memberId: 1, date: today(), content: '既存の予定' }
  });
  await page.goto('/');
  await page.locator('.schedule-item-text:has-text("既存の予定")').click();
  const input = page.locator('.schedule-item-input');
  await expect(input).toBeVisible();
  await input.fill('あ'.repeat(101));
  await input.press('Enter');
  // エラーメッセージが表示されること
  await expect(page.locator('.schedule-item')).not.toContainText('あ'.repeat(101));
  // または error表示を確認
  await snap(page, 'ST-04-07_インライン編集101文字エラー');
});
```

> **注意**: インライン編集のバリデーションエラー表示方法は `app.js` の実装に依存します。モーダル編集（S-03）と同じ `#error-msg` を使うか、別のUI要素を使うかを実装確認してから期待結果を修正してください。

---

### 4.5 S-05 削除確認ダイアログ・UNDO

**注意：** 画面詳細仕様書 v0.4 では S-05（削除確認ダイアログ）は廃止とされ、✕ボタンによる即削除 + UNDOトーストに変更されている。ただしテスト観点一覧に ST-05-01〜ST-05-04 が含まれている場合は、実装で `#confirm` ダイアログが残存しているかを確認し、存在すれば以下の手順で検証する。存在しない場合は `SKIP（設計変更による廃止）` として記録すること。

#### ST-05-01 モーダルの削除ボタンをタップすると削除確認ダイアログが表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-05-01 |
| 機能要件 | 旧FR-17（v0.4で廃止）/ 実装確認 |
| 対象画面 | S-05 |
| 前提条件 | LocalStorageにお父さん（id=1）設定済み。お父さんの今日の予定「確認削除テスト」が登録済み。モーダル（S-03）から対象予定を開いている（`#btn-delete` が表示されている場合） |
| 優先度 | H |

**実装確認事項：** `#btn-delete` がS-03/S-04モーダル内に存在するか確認。`hidden` 属性が解除されて表示される条件を確認する。

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 予定「確認削除テスト」が表示されているモーダルを開く（編集モード） | モーダルが表示され `#btn-delete` が visible になっている |
| 2 | `#btn-delete`（削除ボタン）をタップする | `#confirm`（削除確認ダイアログ）が表示される |
| 3 | ダイアログの内容を確認する | 削除確認の文言と「はい」「いいえ」ボタンが表示されている |

`snap("ST-05-01_削除確認ダイアログ")`

---

#### ST-05-02 削除確認ダイアログに予定内容が20文字truncateで表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-05-02 |
| 機能要件 | 旧FR-17 |
| 対象画面 | S-05 |
| 前提条件 | ST-05-01の続き。30文字以上の予定内容「あ」×30文字が登録済み |
| 優先度 | M |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 30文字の予定を対象に削除確認ダイアログを開く | ダイアログが表示される |
| 2 | ダイアログ内のテキストを確認する | 最大20文字でtruncateされた予定内容が表示されている（例：「あ」×20文字 + 「...」） |

`snap("ST-05-02_truncate表示")`

---

#### ST-05-03 削除確認「はい」で予定が削除されUNDOトーストが表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-05-03 |
| 機能要件 | FR-27 |
| 対象画面 | S-05 |
| 前提条件 | LocalStorageにお父さん設定済み。削除確認ダイアログが表示されている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 削除確認ダイアログで「はい」ボタンをタップする | ダイアログが閉じる。`DELETE /api/schedules/{id}` が204で返る |
| 2 | スケジュールグリッドを確認する | 対象予定が非表示になっている |
| 3 | トーストメッセージを確認する | `#toast`（「削除しました 元に戻す」）が表示されている |

`snap("ST-05-03_削除後UNDOトースト")`

---

#### ST-05-04 削除確認「いいえ」でキャンセルされ予定が残ること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-05-04 |
| 機能要件 | 旧FR-17 |
| 対象画面 | S-05 |
| 前提条件 | LocalStorageにお父さん設定済み。削除確認ダイアログが表示されている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 削除確認ダイアログで「いいえ」ボタンをタップする | ダイアログが閉じる |
| 2 | スケジュールグリッドを確認する | 対象予定がそのまま表示されている |
| 3 | APIが呼ばれていないことを確認する（DevTools Network） | `DELETE /api/schedules/{id}` が呼ばれていない |

`snap("ST-05-04_いいえ後状態確認")`

---

#### ST-05-05 ✕ボタンで即削除されUNDOトーストが表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-05-05 |
| 機能要件 | FR-13, FR-27 |
| 対象画面 | S-02 |
| 前提条件 | LocalStorageにお父さん（id=1）設定済み。お父さんの今日の予定「即削除テスト」が登録済み |
| 優先度 | H |

**FR-23の曖昧さに関する注記（再掲）：** ✕ボタン（`.schedule-item-delete`）による削除は、現在の実装では他メンバーの予定に対しても機能することが確認されている。これが仕様（誰でも削除できる家族共有の思想）なのか、欠陥（削除も本人のみ許可すべき）なのかは要確認。本テストケースでは自分の予定のみを対象とするが、他メンバーの予定に対する✕ボタン動作も合わせて記録すること。

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02でお父さんの「即削除テスト」が表示されていることを確認する | 予定が表示されている |
| 2 | 予定右上の✕ボタン（`.schedule-item-delete`）をタップする | 予定が即座にグリッドから消える |
| 3 | `DELETE /api/schedules/{id}` が呼ばれることを確認する | 204が返っている |
| 4 | トーストを確認する | `#toast`（「削除しました 元に戻す」）が表示されている |

`snap("ST-05-05_即削除後UNDOトースト")`

Playwright操作例:
```javascript
test('ST-05-05: ✕ボタンで即削除', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  // テスト用予定を事前にAPI登録
  const today = new Date().toISOString().slice(0, 10);
  await page.request.post('/api/schedules', {
    data: { memberId: 1, date: today, content: '即削除テスト' }
  });
  await page.reload();
  const deleteBtn = page.locator('.schedule-item-delete').first();
  await deleteBtn.click();
  await expect(page.locator('#schedule-grid')).not.toContainText('即削除テスト');
  await expect(page.locator('#toast')).toBeVisible();
  await expect(page.locator('#toast')).toContainText('削除しました');
  await snap(page, 'ST-05-05_即削除後UNDOトースト');
});
```

---

#### ST-05-06 UNDOトースト「元に戻す」タップで削除が復元されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-05-06 |
| 機能要件 | FR-27 |
| 対象画面 | S-02 |
| 前提条件 | ST-05-05の直後（UNDOトーストが表示されている状態） |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | `#toast` が表示されていることを確認する | 「削除しました 元に戻す」が表示されている |
| 2 | トースト内の「元に戻す」ボタン（`.toast-undo-btn`）をタップする | `POST /api/schedules/{id}/restore` が呼ばれる |
| 3 | スケジュールグリッドを確認する | 削除した予定が復元されて表示されている |
| 4 | トーストが消えることを確認する | `#toast` が非表示になる |

`snap("ST-05-06_UNDO復元後")`

Playwright操作例:
```javascript
test('ST-05-06: UNDO（元に戻す）で復元', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  const today = new Date().toISOString().slice(0, 10);
  await page.request.post('/api/schedules', {
    data: { memberId: 1, date: today, content: 'UNDO復元テスト' }
  });
  await page.reload();
  // 削除
  await page.locator('.schedule-item-delete').first().click();
  await expect(page.locator('#toast')).toBeVisible();
  // UNDO
  await page.locator('.toast-undo-btn').click();
  await expect(page.locator('#schedule-grid')).toContainText('UNDO復元テスト');
  await snap(page, 'ST-05-06_UNDO復元後');
});
```

---

#### ST-05-07 UNDOトーストが5秒後に自動消滅しpurgeされること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-05-07 |
| 機能要件 | FR-27 |
| 対象画面 | S-02 |
| 前提条件 | 予定を削除し、UNDOトーストが表示されている状態 |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | 予定の✕ボタンをタップして削除する | UNDOトーストが表示される |
| 2 | 5秒間待機する（UNDOを行わない） | 5秒後にトーストが自動消滅する |
| 3 | `POST /api/schedules/{id}/purge` が呼ばれることを確認する（DevTools Network） | purgeAPIが呼ばれ、204が返る |
| 4 | その後、`POST /api/schedules/{id}/restore` を呼んでも失敗することを確認する（任意） | 404が返り、復元不可 |

`snap("ST-05-07_5秒後トースト消滅")`

Playwright操作例:
```javascript
test('ST-05-07: UNDO 5秒後に自動purge', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  const today = new Date().toISOString().slice(0, 10);
  await page.request.post('/api/schedules', {
    data: { memberId: 1, date: today, content: 'purgeテスト' }
  });
  await page.reload();
  await page.locator('.schedule-item-delete').first().click();
  await expect(page.locator('#toast')).toBeVisible();
  // 5秒以上待機（UNDOしない）
  await page.waitForTimeout(6000);
  await expect(page.locator('#toast')).toBeHidden();
}, { timeout: 15000 });
```

---

### 4.6 S-06 メンバー管理モーダル

#### ST-06-01 ⚙ボタンでメンバー管理モーダルが開閉すること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-06-01 |
| 機能要件 | FR-24 |
| 対象画面 | S-06 |
| 前提条件 | LocalStorageにお父さん設定済み。S-02が表示されている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | S-02のヘッダ右端にある ⚙ ボタン（`#btn-member-settings`）をタップする | `#member-modal` が表示される |
| 2 | モーダルのタイトルを確認する | 「メンバー管理」が表示されている |
| 3 | 現在のメンバー一覧（`#member-manage-list`）を確認する | 全メンバーが表示されている |
| 4 | ✕ボタン（`#btn-member-modal-close`）をタップする | モーダルが閉じる |

`snap("ST-06-01_メンバー管理モーダル開")` / `snap("ST-06-01_モーダル閉")`

Playwright操作例:
```javascript
test('ST-06-01: ⚙ボタンでメンバー管理モーダル開閉', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await page.locator('#btn-member-settings').click();
  await expect(page.locator('#member-modal')).toBeVisible();
  await snap(page, 'ST-06-01_メンバー管理モーダル開');
  await page.locator('#btn-member-modal-close').click();
  await expect(page.locator('#member-modal')).toBeHidden();
  await snap(page, 'ST-06-01_モーダル閉');
});
```

---

#### ST-06-02 メンバーを新規追加できること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-06-02 |
| 機能要件 | FR-24 |
| 対象画面 | S-06 |
| 前提条件 | LocalStorageにお父さん設定済み。メンバー管理モーダルが開いている。現在のメンバー数が9名以下 |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | `#member-add-input` に「おじいちゃん」と入力する | テキストフィールドに文字が入力される |
| 2 | `#btn-member-add`（追加ボタン）をタップする | `POST /api/members` が201で返る |
| 3 | `#member-manage-list` を確認する | 「おじいちゃん」が一覧に追加されている |
| 4 | S-02のグリッドを確認する（モーダルを閉じて） | おじいちゃんの行が追加されている |

`snap("ST-06-02_メンバー追加後リスト")`

Playwright操作例:
```javascript
test('ST-06-02: メンバー追加', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  await page.locator('#btn-member-settings').click();
  await page.locator('#member-add-input').fill('おじいちゃん');
  await page.locator('#btn-member-add').click();
  await expect(page.locator('#member-manage-list')).toContainText('おじいちゃん');
  await snap(page, 'ST-06-02_メンバー追加後リスト');
  // クリーンアップ：追加したメンバーを削除
  // （テスト後の状態管理はチームのポリシーに従う）
});
```

---

#### ST-06-03 メンバー名を変更できること（変更ボタン→入力→Enter）

| 項目 | 内容 |
|------|------|
| テストケースID | ST-06-03 |
| 機能要件 | FR-24 |
| 対象画面 | S-06 |
| 前提条件 | LocalStorageにお父さん設定済み。メンバー管理モーダルが開いている |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | `#member-manage-list` 内で「長男」行の「名前変更」ボタンをタップする | 行内に入力フィールドが展開され、現在の名前「長男」が入力されている |
| 2 | 入力フィールドの内容を「弟」に変更する | 内容が変更される |
| 3 | Enterキーを押す（または確定ボタンをタップする） | `PUT /api/members/5` が呼ばれ、200が返る |
| 4 | `#member-manage-list` を確認する | 「弟」に名前が変更されている |
| 5 | 操作後に「長男」に戻す（クリーンアップ） | 名前が「長男」に戻る |

`snap("ST-06-03_名前変更後")`

---

#### ST-06-04 予定のないメンバーを削除できること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-06-04 |
| 機能要件 | FR-24 |
| 対象画面 | S-06 |
| 前提条件 | LocalStorageにお父さん設定済み。次女（id=4）に予定が登録されていない |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | メンバー管理モーダルを開く | モーダルが表示される |
| 2 | 「次女」行の「削除」ボタンをタップする | 削除確認ダイアログが表示される（実装に確認ダイアログが存在する場合） |
| 3 | 確認ダイアログで「はい」をタップする（またはそのまま削除される場合） | `DELETE /api/members/4` が204で返る |
| 4 | `#member-manage-list` を確認する | 「次女」が一覧から消えている |
| 5 | S-02のグリッドを確認する（モーダルを閉じて） | 次女の行が消えている |
| 6 | 次女を再登録するなど、テスト後のクリーンアップを行う | 初期状態（5名）に戻す |

`snap("ST-06-04_メンバー削除後")`

---

#### ST-06-05 予定のあるメンバーを削除しようとするとエラーが表示されること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-06-05 |
| 機能要件 | FR-24 |
| 対象画面 | S-06 |
| 前提条件 | LocalStorageにお父さん設定済み。長女（id=3）に今日の予定が1件以上登録済み |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | メンバー管理モーダルを開く | モーダルが表示される |
| 2 | 「長女」行の「削除」ボタンをタップする | `DELETE /api/members/3` が409を返す |
| 3 | エラー表示を確認する | `#member-modal-error` に「このメンバーには予定が登録されています。先に予定を削除してください。」が表示される |
| 4 | `#member-manage-list` を確認する | 「長女」が一覧に残っている |

`snap("ST-06-05_削除エラー表示")`

Playwright操作例:
```javascript
test('ST-06-05: 予定ありメンバー削除でエラー', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  // 長女に予定を登録（前提条件）
  const today = new Date().toISOString().slice(0, 10);
  await page.request.post('/api/schedules', {
    data: { memberId: 3, date: today, content: '削除エラーテスト用' }
  });
  await page.locator('#btn-member-settings').click();
  // 長女の削除ボタンを押す
  const nagajoRow = page.locator('#member-manage-list .member-manage-item', { hasText: '長女' });
  await nagajoRow.locator('button', { hasText: '削除' }).click();
  await expect(page.locator('#member-modal-error')).toBeVisible();
  await expect(page.locator('#member-modal-error')).toContainText('予定が登録されています');
  await snap(page, 'ST-06-05_削除エラー表示');
});
```

---

#### ST-06-06 10名到達時にメンバー追加フォームが非表示になること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-06-06 |
| 機能要件 | FR-24 |
| 対象画面 | S-06 |
| 前提条件 | LocalStorageにお父さん設定済み。現在のメンバー数が9名 |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | メンバーを9名の状態にする（初期5名 + 追加4名） | メンバー管理一覧に9名が表示されている |
| 2 | `#member-add-section` の表示を確認する | 追加フォームが表示されている（9名では表示される） |
| 3 | 10人目のメンバーを追加する（`#member-add-input` に名前入力 → 追加ボタン） | `POST /api/members` が201で返る |
| 4 | `#member-add-section` の表示状態を確認する | 追加フォームが非表示（`display:none` または `hidden` 属性）になっている |
| 5 | テスト後に追加したメンバーを削除してクリーンアップする | 初期状態（5名）に戻す |

`snap("ST-06-06_9名時追加フォーム表示")` / `snap("ST-06-06_10名到達追加フォーム非表示")`

Playwright操作例:
```javascript
test('ST-06-06: 10名到達で追加フォーム非表示', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  // 初期5名に5名追加して10名にする
  const addNames = ['テスト1', 'テスト2', 'テスト3', 'テスト4', 'テスト5'];
  for (const name of addNames) {
    await page.request.post('/api/members', { data: { name } });
  }
  await page.locator('#btn-member-settings').click();
  await page.waitForTimeout(500);
  await snap(page, 'ST-06-06_10名到達追加フォーム非表示');
  await expect(page.locator('#member-add-section')).toBeHidden();
});
```

---

#### ST-06-07 BUG-VALIDATOR: 追加メンバー（id=6以降）への予定登録がAPIで400になること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-06-07 |
| 機能要件 | FR-24, FR-07（バグ確認） |
| 対象画面 | S-03 / S-06 |
| 前提条件 | LocalStorageにお父さん設定済み。新規メンバー「おじいちゃん」（id=6）が追加済み |
| 優先度 | H |
| 期待ステータス | **KNOWN_FAIL（BUG-VALIDATOR）** |

**バグ概要：** メンバー追加機能（FR-24）によって id=6 以降の連番で作成されたメンバーに対して、予定登録（`POST /api/schedules`）を行うとAPIレベルで 400 エラーが返る。初期5名（id=1〜5）への予定登録は正常に動作する。

**想定される原因：** APIのバリデーション処理でメンバーIDの存在チェック時に、初期データのIDのみをハードコードしているか、または新規メンバーのIDがサーバーのキャッシュ・バリデーションロジックに正しく反映されていない可能性がある。

UI操作手順:

| ステップ | 操作 | 期待結果（正常系） | 実際の結果（バグ） |
|---------|------|-----------------|-----------------|
| 1 | メンバー管理モーダルで「おじいちゃん」を追加する | 201 Created で追加される | 追加自体は成功する |
| 2 | モーダルを閉じてS-02を確認する | 「おじいちゃん」行が表示される | おじいちゃん行が表示される |
| 3 | `#btn-add` をタップしてS-03（予定追加モーダル）を開く | モーダルが開く | モーダルが開く |
| 4 | `#who-btns` で「おじいちゃん」を選択する | 「おじいちゃん」が選択状態になる | 選択状態になる |
| 5 | `#content-input` に「温泉旅行」と入力する | テキストが入力される | テキストが入力される |
| 6 | `#btn-save` をタップする | `POST /api/schedules` が201を返し、予定が追加される | **400 Bad Request が返り、`#error-msg` にエラーが表示される** |
| 7 | `#error-msg` の内容を記録する | エラーは発生しない | エラーメッセージの内容を記録する（例：「誰を選んでください」等） |

`snap("ST-06-07_KNOWN_FAIL_追加メンバー予定登録400エラー")`

**API直接検証手順（curl）：**

```bash
# 事前：おじいちゃんを追加し、返された id を確認
NEW_ID=$(curl -s -X POST http://localhost:8080/api/members \
  -H "Content-Type: application/json" \
  -d '{"name":"おじいちゃん"}' | jq '.id')

# 追加メンバーへの予定登録（バグ再現）
TODAY=$(date +%Y-%m-%d)
curl -v -X POST http://localhost:8080/api/schedules \
  -H "Content-Type: application/json" \
  -d "{\"memberId\":${NEW_ID},\"date\":\"${TODAY}\",\"content\":\"温泉旅行\"}"
# 期待：400 Bad Request が返る（バグ再現）
```

**記録事項：**
- HTTPステータスコード：
- レスポンスボディ（`error` / `message` / `fields` の内容）：
- UI上の `#error-msg` に表示されるテキスト：
- 初期メンバー（id=1〜5）への予定登録が正常に動作することも合わせて確認する

Playwright操作例:
```javascript
test('ST-06-07: BUG-VALIDATOR 追加メンバーへの予定登録', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  // 新メンバー追加
  const res = await page.request.post('/api/members', {
    data: { name: 'おじいちゃん' }
  });
  const newMember = await res.json();
  console.log('新メンバーID:', newMember.id);  // id=6以降
  await page.reload();
  // 追加メンバーへの予定登録試行
  const today = new Date().toISOString().slice(0, 10);
  const schedRes = await page.request.post('/api/schedules', {
    data: { memberId: newMember.id, date: today, content: '温泉旅行' }
  });
  console.log('APIステータス:', schedRes.status());  // 400を期待（バグ）
  // UI上の確認
  await page.locator('#btn-add').click();
  const whoBtn = page.locator('#who-btns button', { hasText: 'おじいちゃん' });
  await whoBtn.click();
  await page.locator('#content-input').fill('温泉旅行');
  await page.locator('#btn-save').click();
  await snap(page, 'ST-06-07_KNOWN_FAIL_追加メンバー予定登録400エラー');
  // このテストはKNOWN_FAILとして記録
  // エラー表示の内容を記録するため、assertはせず状態を観察する
  const errorMsg = await page.locator('#error-msg').textContent().catch(() => '非表示');
  console.log('エラーメッセージ:', errorMsg);
});
```

---

## 5. セキュリティ・堅牢性テスト

#### ST-XS-01 XSS: scriptタグを含むcontent・メンバー名がスクリプトとして実行されないこと

| 項目 | 内容 |
|------|------|
| テストケースID | ST-XS-01 |
| 機能要件 | NFR-08 |
| 対象画面 | 全画面 |
| 前提条件 | LocalStorageにお父さん設定済み |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | `#btn-add` をタップして予定追加モーダルを開く | モーダルが表示される |
| 2 | `#content-input` に `<script>document.title='XSS'</script>` と入力する | テキストが入力される |
| 3 | `#btn-save` をタップする | 保存が成功する（contentとして登録される） |
| 4 | グリッドに表示されたアイテムを確認する | scriptタグがテキストとして表示されており、スクリプトは実行されていない |
| 5 | `document.title` を確認する | 「XSS」に変わっていない（元のタイトルのまま） |
| 6 | メンバー管理モーダルで `<img src=x onerror=alert(1)>` という名前でメンバーを追加する | エラーが発生せずにテキストとして扱われるか、バリデーションエラーになる |
| 7 | アラートが表示されないことを確認する | `alert` が実行されていない |

`snap("ST-XS-01_XSS無害化確認")`

Playwright操作例:
```javascript
test('ST-XS-01: XSS無害化', async ({ page }) => {
  await page.goto('/');
  await setCurrentUser(page, { id: 1, name: 'お父さん', displayOrder: 1 });
  await page.reload();
  const originalTitle = await page.title();
  // scriptタグ入力
  await page.locator('#btn-add').click();
  await page.locator('#content-input').fill("<script>document.title='XSS'</script>");
  await page.locator('#btn-save').click();
  await page.waitForTimeout(500);
  // タイトルが変わっていないことを確認
  const currentTitle = await page.title();
  expect(currentTitle).toBe(originalTitle);
  // グリッドにscriptがテキストとして表示されているかを確認
  await snap(page, 'ST-XS-01_XSS無害化確認');
  // アラートダイアログが自動で出現しないことを確認（page.on('dialog', ...)で監視）
  let dialogDetected = false;
  page.on('dialog', () => { dialogDetected = true; });
  await page.waitForTimeout(1000);
  expect(dialogDetected).toBe(false);
});
```

---

#### ST-LS-01 壊れたlocalStorageの値が存在してもアプリが正常起動すること

| 項目 | 内容 |
|------|------|
| テストケースID | ST-LS-01 |
| 機能要件 | FR-20（堅牢性） |
| 対象画面 | S-01 / S-02 |
| 前提条件 | なし |
| 優先度 | H |

UI操作手順:

| ステップ | 操作 | 期待結果 |
|---------|------|---------|
| 1 | LocalStorageの `familySchedule.currentUser` に不正なJSON文字列 `{broken json` をセットする | 値がセットされる |
| 2 | `http://localhost:8080` を開く | アプリがクラッシュしない |
| 3 | 表示状態を確認する | S-01（利用者選択画面）が表示される（不正な値は無視されてS-01にフォールバックする） |
| 4 | コンソールエラーを確認する | `JSON.parse` エラーはコンソールに出力されても良いが、アプリがクラッシュしていないこと |
| 5 | 通常の利用者選択フローが実行できることを確認する | メンバーボタンをタップしてS-02へ遷移できる |

`snap("ST-LS-01_壊れたLS起動確認")`

Playwright操作例:
```javascript
test('ST-LS-01: 壊れたlocalStorageでもアプリが動く', async ({ page }) => {
  await page.goto('/');
  // 壊れたJSON値をセット
  await page.evaluate(() => {
    localStorage.setItem('familySchedule.currentUser', '{broken json: not valid');
  });
  await page.reload();
  // アプリがクラッシュしていないことを確認（S-01にフォールバック）
  await expect(page.locator('#screen-select-user')).toBeVisible();
  await snap(page, 'ST-LS-01_壊れたLS起動確認');
  // 通常操作が可能であることも確認
  await page.locator('#member-btns .member-btn', { hasText: 'お父さん' }).click();
  await expect(page.locator('#screen-schedule')).toBeVisible();
});
```

---

## 6. 付録: よくある失敗と対処法

### 6.1 LocalStorageが残っていてS-01に遷移しない

**症状：** ST-01-01 で S-02 が表示されてしまう。

**原因：** 前のテストで LocalStorage に `familySchedule.currentUser` が残っている。

**対処：**
- 各テスト開始前に `clearCurrentUser(page)` を実行する
- Playwright の `beforeEach` フックで確実にクリアする
- ブラウザの「サイトデータをすべてクリア」を手動で実行する

---

### 6.2 フリック操作がPlaywrightで認識されない

**症状：** ST-02-04/ST-02-05 で日付が変わらない。

**原因：** Playwright のタッチイベントエミュレーションが実装のフリック判定（水平30px以上・垂直20px以下）を満たしていない場合がある。

**対処：**
- `page.touchscreen.tap()` の代わりに `dispatchEvent` でtouchstart/touchmove/touchendを順に発行する
- マウスドラッグ代替を試みる（`page.mouse.move` + `down` + `move` + `up`）
- 実機スマホでのUI操作手順で確認する（PC Playwright は代替）

---

### 6.3 インライン編集のtextareaが表示されない

**症状：** ST-04-01 で `.schedule-item-input` が現れない。

**原因：**
- LocalStorage のユーザーが他のメンバーになっており、FR-23 により編集が抑制されている
- `click()` が `mousedown` ベースであり、タッチデバイスの実装と挙動が異なる場合がある

**対処：**
- 前提条件のLocalStorage設定を確認する（ログインユーザーと予定のオーナーが一致しているか）
- `click()` の代わりに `tap()` を試みる
- 実機スマホで動作確認する

---

### 6.4 UNDOテスト（ST-05-07）のタイムアウト

**症状：** 5秒待機中に Playwright のデフォルトタイムアウト（30秒）が超過し、テストが失敗する。

**原因：** 想定外の原因ではないが、テストの timeout 設定が不十分。

**対処：**
- 該当テストケースに `{ timeout: 15000 }` を設定する（`page.waitForTimeout(6000)` + マージン）
- `playwright.config.js` の `timeout` を適切な値に設定する

---

### 6.5 BUG-VALIDATOR（ST-06-07）後のクリーンアップ忘れ

**症状：** ST-06-07 実行後に追加した「おじいちゃん」が残り、後続テストのメンバー数カウントがズレる（特に ST-06-06 に影響）。

**対処：**
- `afterEach` または `afterAll` フックで追加したメンバーを削除する
- テストスイートの実行順序を考慮し、ST-06-06 を ST-06-07 より先に実行する
- テスト用メンバーは一意な名前（例：`テスト用_${Date.now()}`）を使用して識別しやすくする

---

### 6.6 メンバー管理テスト後に初期データが崩れる

**症状：** ST-06-04（メンバー削除）後、以降のテストで削除したメンバーの予定セルが存在しない。

**対処：**
- ST-06系テストは独立した実行環境（別のDBインスタンスまたはトランザクションロールバック）で行う
- または Docker Compose でテスト前後にDBを初期化する：

```bash
# テスト後にDBを初期状態に戻す
docker-compose down -v && docker-compose up -d
# Flyway マイグレーション + 初期データ投入を待つ
```

---

*以上*
