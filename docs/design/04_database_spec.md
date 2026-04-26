# データベース仕様書

- バージョン：v0.3（探索的テスト第2回対応）
- DBMS：PostgreSQL 16
- 更新履歴：
  - v0.1：初版
  - v0.2：[基本設計レビュー](./review_basic_design_v0.1.md) の DS-5（文字数バリデーション方針）を反映
  - v0.3：ET-005（メンバーID採番をシーケンス化）・ET-006（content 列長を 400→100 に変更）を反映
- スキーマ：`public`
- 文字コード：UTF-8
- タイムゾーン：Asia/Tokyo（アプリ側で統一）

---

## ER 図（概念）

```
┌──────────────┐          ┌──────────────────┐
│   members    │ 1      * │    schedules     │
│──────────────│──────────│──────────────────│
│ id    (PK)   │          │ id         (PK)  │
│ name  UK     │          │ member_id  (FK)  │
│ display_order│          │ date             │
└──────────────┘          │ content          │
                          │ created_at       │
                          │ updated_at       │
                          └──────────────────┘
```

---

## テーブル定義

### T-01：`members`（家族メンバーマスタ）

| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| `id` | INTEGER | PRIMARY KEY | 初期データは 1〜5。追加メンバーは `members_id_seq`（100起点）で自動採番 |
| `name` | VARCHAR(50) | NOT NULL, UNIQUE | 表示名（最大20文字、codePointCount 基準） |
| `display_order` | INTEGER | NOT NULL | 画面での並び順 |

**初期データ（V1 マイグレーションで INSERT）：**

| id | name | display_order |
|----|----------|------|
| 1 | お父さん | 1 |
| 2 | お母さん | 2 |
| 3 | 長女 | 3 |
| 4 | 次女 | 4 |
| 5 | 長男 | 5 |

- V4 マイグレーションで `members_id_seq`（START 100, INCREMENT 1）を追加。JPA `@GeneratedValue(SEQUENCE)` で採番。
- UIでの入力・閲覧は `display_order` 昇順で統一。

---

### T-02：`schedules`（予定）

| カラム | 型 | 制約 | 説明 |
|---|---|---|---|
| `id` | BIGSERIAL | PRIMARY KEY | 予定ID（自動採番） |
| `member_id` | INTEGER | NOT NULL, REFERENCES members(id) ON DELETE RESTRICT | 担当メンバー |
| `date` | DATE | NOT NULL | 予定日 |
| `content` | VARCHAR(100) | NOT NULL, CHECK (char_length(content) >= 1) | 予定内容（フリーテキスト、最大100文字）。V5 マイグレーションで 400→100 に変更。アプリ側バリデーション（codePointCount）と DB 制約を二重担保（DS-5 参照） |
| `created_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT now() | 作成日時 |
| `updated_at` | TIMESTAMP WITH TIME ZONE | NOT NULL DEFAULT now() | 更新日時（更新時にアプリ側で set） |

---

## インデックス

| 名前 | 対象 | 目的 |
|---|---|---|
| `pk_members` | members(id) | PK |
| `uk_members_name` | members(name) | UNIQUE |
| `pk_schedules` | schedules(id) | PK |
| `ix_schedules_date_member` | schedules(date, member_id) | 主クエリ「日付範囲 × メンバー」の最適化 |

メインの検索はほぼ `WHERE date BETWEEN ? AND ?` なので
複合インデックスの第一キーに `date` を置く。

---

## DDL（マイグレーション履歴）

```sql
-- V1__init.sql（初期スキーマ）
CREATE TABLE members (
    id            INTEGER     PRIMARY KEY,
    name          VARCHAR(50) NOT NULL UNIQUE,
    display_order INTEGER     NOT NULL
);
INSERT INTO members (id, name, display_order) VALUES
    (1, 'お父さん', 1), (2, 'お母さん', 2), (3, '長女', 3),
    (4, '次女', 4), (5, '長男', 5);

CREATE TABLE schedules (
    id          BIGSERIAL PRIMARY KEY,
    member_id   INTEGER NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    date        DATE    NOT NULL,
    content     VARCHAR(400) NOT NULL CHECK (char_length(content) >= 1),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX ix_schedules_date_member ON schedules(date, member_id);

-- V2〜V3：soft delete、UNDO 機能追加（deleted_at 列）

-- V4__member_id_sequence.sql（ET-005: TOCTOU 競合を解消）
CREATE SEQUENCE IF NOT EXISTS members_id_seq START WITH 100 INCREMENT BY 1;
ALTER TABLE members ALTER COLUMN id SET DEFAULT nextval('members_id_seq');

-- V5__content_max_100.sql（ET-006: DB 制約をアプリバリデーションと一致）
ALTER TABLE schedules ALTER COLUMN content TYPE VARCHAR(100);
```

**現在の有効スキーマ（V5 適用後）：**

```sql
-- members（現在）
id            INTEGER  PRIMARY KEY  DEFAULT nextval('members_id_seq')
name          VARCHAR(50) NOT NULL UNIQUE
display_order INTEGER NOT NULL

-- schedules（現在）
content       VARCHAR(100) NOT NULL CHECK (char_length(content) >= 1)
```

---

## 代表的なクエリ

### 今日と明日の予定取得

```sql
SELECT s.id, s.member_id, m.name AS member_name, s.date, s.content
FROM schedules s
JOIN members m ON m.id = s.member_id
WHERE s.date BETWEEN :from AND :to
ORDER BY s.date ASC, m.display_order ASC, s.id ASC;
```

### 予定追加

```sql
INSERT INTO schedules (member_id, date, content)
VALUES (:memberId, :date, :content)
RETURNING id, member_id, date, content;
```

### 予定更新

```sql
UPDATE schedules
SET member_id = :memberId,
    date      = :date,
    content   = :content,
    updated_at = now()
WHERE id = :id;
```

### 予定削除

```sql
DELETE FROM schedules WHERE id = :id;
```

---

## 保持・運用方針

| 項目 | 方針 |
|---|---|
| バックアップ | ローカル運用想定、日次 `pg_dump`（将来） |
| データ保持 | 過去データも全件保持（BL-09で将来30日制限） |
| 文字数 | DB側 `VARCHAR(100)` と アプリ側 100文字バリデーションで二重化。アプリ側は `content.codePointCount(0, content.length()) <= 100` でサロゲートペアも1文字として数える（DS-5）。PostgreSQL の VARCHAR(N) は文字（Unicode コードポイント）単位で数えるため整合する。V5 マイグレーション適用済み |
| トランザクション | CRUD は `@Transactional`、READ系は `readOnly=true` |
