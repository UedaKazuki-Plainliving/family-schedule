# 基本設計書 - 家族スケジュール共有システム

- バージョン：v0.1
- 作成日：2026-04-24
- 前提要件：[要件定義 v0.3](../requirements/requirements.md)

---

## 1. システム構成

### 1.1 構成図

```
┌─────────────────────────────────────────────────────────┐
│  ブラウザ (スマホ優先 / PC も可)                         │
│  ┌───────────────────────────────────────────────────┐ │
│  │ SPA: HTML + Vanilla JS (+ 必要最小限の CSS)         │ │
│  │  - LocalStorage に "currentUser" を保存 (FR-20)    │ │
│  │  - fetch で Backend API を呼び出し                  │ │
│  └───────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────┘
                            │ HTTPS / JSON
                            ▼
┌─────────────────────────────────────────────────────────┐
│  Backend (Java 21 / Spring Boot 3.x)                     │
│  ┌───────────────────────────────────────────────────┐ │
│  │ Web Layer      : REST Controller (/api/**)          │ │
│  │ Service Layer  : ScheduleService, MemberService     │ │
│  │ Repository     : Spring Data JPA                    │ │
│  │ Static Resources: /static で SPA を配信             │ │
│  └───────────────────────────────────────────────────┘ │
└───────────────────────────┬─────────────────────────────┘
                            │ JDBC
                            ▼
┌─────────────────────────────────────────────────────────┐
│  PostgreSQL 16                                           │
│  - members (マスタ、5件固定)                              │
│  - schedules (予定)                                       │
└─────────────────────────────────────────────────────────┘
```

### 1.2 技術スタック

| レイヤ | 技術 |
|---|---|
| フロントエンド | HTML5 + Vanilla JavaScript（フレームワーク不使用。軽量維持） |
| スタイル | プレーンCSS（タップ領域44px・文字16px以上を共通ルール化） |
| バックエンド | Java 21 / Spring Boot 3.x / Spring Web / Spring Data JPA |
| DB | PostgreSQL 16 |
| ビルド | Maven |
| 単体テスト | JUnit 5 + Mockito |
| BDD | Cucumber-JVM (Gherkin日本語) |
| API/E2Eテスト | Playwright for Java |
| 起動 | `./mvnw spring-boot:run` |

---

## 2. レイヤ構成とパッケージ

```
com.family.schedule
├── FamilyScheduleApplication.java    # SpringBootApplication
├── web/                              # REST コントローラ
│   ├── MemberController.java
│   ├── ScheduleController.java
│   └── dto/
│       ├── ScheduleRequest.java
│       ├── ScheduleResponse.java
│       └── MemberResponse.java
├── service/                          # ビジネスロジック
│   ├── MemberService.java
│   └── ScheduleService.java
├── domain/                           # JPA エンティティ
│   ├── Member.java
│   └── Schedule.java
├── repository/                       # Spring Data JPA
│   ├── MemberRepository.java
│   └── ScheduleRepository.java
└── config/
    └── WebConfig.java                # CORS, static resources
```

---

## 3. 画面構成（サマリ）

詳細は [画面詳細仕様書](./02_screen_spec.md) 参照。

| 画面ID | 画面名 | 役割 |
|---|---|---|
| S-01 | 利用者選択画面 | 初回起動時のみ。5人からタップで選ぶ |
| S-02 | スケジュール画面 | 本システムのメイン画面。今日と明日を表示 |
| S-03 | 予定入力フォーム (モーダル) | 新規登録 |
| S-04 | 予定編集フォーム (モーダル) | 編集＋削除 |
| S-05 | 削除確認ダイアログ | 対象内容を表示して確認 |

**レイアウト方針（レビュー F1-4 反映）：**
- スケジュール画面は **「人＝行」「日付＝列（今日／明日の2列）」** の格子状
- スマホ縦画面でも5人×2列が見渡せるよう、各セルは可変高さ

---

## 4. 処理フロー（主要）

### 4.1 起動時

```
ブラウザ起動
  │
  ▼
LocalStorage に currentUser?
  │              │
 No│              │Yes
  ▼              ▼
S-01 表示    S-02 表示 (GET /api/schedules?from=today&to=tomorrow)
  │
  ▼
ユーザー選択 → LocalStorage 保存 → S-02 表示
```

### 4.2 予定登録（FR-21：3ステップ以内）

```
S-02 [+追加] をタップ  ← 1ステップ目
  ▼
S-03 モーダル (誰が=現在の利用者, 日付=表示中日付, 内容=空) ← フォーム入力 (2ステップ目)
  ▼
[保存] タップ  ← 3ステップ目
  ▼
POST /api/schedules
  ▼
成功: トースト表示 → S-02 に反映
失敗 (validation): フォーム内エラー表示
```

### 4.3 フリック

```
S-02 で左スワイプ → 表示日付 = (today+1, today+2)
S-02 で右スワイプ → 表示日付 = (today-1, today)
[今日に戻る] → 表示日付 = (today, today+1)
```

フリックは **画面内部状態**（JSの `viewDate`）のみ変化し、
サーバーからは該当2日分のみ取得する。

---

## 5. 非機能方針

| 項目 | 方針 |
|---|---|
| 性能 | スケジュール取得 API は必要日付2件のみ返す。インデックス（date）を貼る |
| 可搬性 | DB接続情報は環境変数（`SPRING_DATASOURCE_URL` 等） |
| テスト | DB は Testcontainers で本番同等の PostgreSQL を起動 |
| ビルド成果物 | `jar` 単体で起動。`static/` に SPA をバンドル |
| ログ | Spring Boot デフォルト（INFO） |

---

## 6. 開発環境

| 項目 | 値 |
|---|---|
| Java | 21 (LTS) |
| Maven | 3.9+ |
| PostgreSQL | 16 （ローカルは docker compose で起動） |
| ブラウザ | Chrome / Safari / Edge 最新2バージョン |
