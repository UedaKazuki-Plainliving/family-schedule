# テストデータ管理

このフォルダにはテスト用のデータ準備・クリーンアップ用 SQL スクリプトが入っています。

## ファイル一覧

| ファイル | 用途 | 対象テスト | 対象DB |
|---------|------|-----------|-------|
| `seed_at.sql` | ATテスト前の初期データ投入 | 受け入れテスト（AT） | PostgreSQL |
| `cleanup_at.sql` | ATテスト後のデータ削除・初期化 | 受け入れテスト（AT） | PostgreSQL |
| `cleanup_load.sql` | 負荷テスト後の残存データ削除 | 負荷テスト | PostgreSQL |

---

## 各テスト種別のデータ管理方針

### IT（結合テスト） — H2インメモリDB（devプロファイル）

- **起動コマンド**: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
- **初期データ**: Flyway が起動時に自動投入（メンバー5名・スケジュール0件）
- **リセット方法**: **サーバーを再起動するだけでOK**（H2はメモリなので再起動で消える）
- **このフォルダのSQLは不要**（再起動で自動リセットされるため）

```bash
# Ctrl+C でサーバー停止 → 再起動でリセット完了
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### ST（システムテスト） — PostgreSQL（Docker Compose）

- **起動コマンド**: `docker compose up -d`
- **初期データ**: Flyway が初回起動時に自動投入（2回目以降は残存）
- **リセット方法A（推奨）**: `reset_it_st.sql` を実行

```bash
docker compose exec db psql -U family -d family_schedule \
  -f docs/tests/data/reset_it_st.sql
```

- **リセット方法B（確実）**: ボリュームごと削除

```bash
docker compose down -v && docker compose up -d
```

---

### AT（受け入れテスト） — PostgreSQL（本番環境）

ATテストは実機スマホ + 本番サーバーを使うため、事前データ準備と事後クリーンアップが必要。

#### テスト前の手順

1. サーバーの PostgreSQL に接続する

```bash
# EC2サーバーで実行する場合
psql -U postgres -d familydb
```

2. まずクリーンアップを実行（前回のデータが残っている場合）

```sql
\i docs/tests/data/cleanup_at.sql
```

3. シードデータを投入する

```sql
\i docs/tests/data/seed_at.sql
```

4. 投入結果を確認する

```sql
SELECT member_id, date, content FROM schedules ORDER BY member_id, date;
```

メンバー5名・計13件のスケジュールが表示されれば準備完了。

#### テスト後の手順

テストが完了したら、以下でデータをリセットする：

```sql
\i docs/tests/data/cleanup_at.sql
```

---

### 負荷テスト — PostgreSQL（本番環境）

詳細は `docs/tests/06_load_test_spec.md` の Section 7 を参照。

**テスト前のシードデータ投入:**
```sql
-- 06_load_test_spec.md Section 7 のSQLを実行
INSERT INTO schedules (member_id, date, content, created_at, updated_at)
SELECT (i % 5) + 1, CURRENT_DATE + (i % 14), '負荷テスト予定' || i, now(), now()
FROM generate_series(1, 50) AS i;
```

**テスト後のクリーンアップ:**
```sql
\i docs/tests/data/cleanup_load.sql
```

---

## テストの実行順序（AT）

ATテストはシナリオ間に依存関係があります。以下の順で実施してください：

```
AT-P01-01 → AT-P01-02 → AT-P01-03（おじいちゃん追加）
                              ↓
AT-GR-02（id=6 への予定登録 → BUG-VALIDATOR 確認）
```

> **重要**: AT-GR-02 は AT-P01-03（6人目のメンバー追加）を先に実施してから行うこと。
