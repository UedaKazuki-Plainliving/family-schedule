# 家族スケジュール - クラウドデプロイ手順（Fly.io）

家族が外出先からもスマホで使えるよう、Fly.io にデプロイする手順です。

> **ローカルや自宅サーバーで Docker を使ってまとめて動かしたい場合**は
> このドキュメント末尾の「Docker でローカルにまとめて動かす」を参照。

## 前提

- **セキュリティ注意**：このMVPは認証（BL-06）未実装です。
  Fly.io の公開URL（`https://<app>.fly.dev`）を**知っている人は誰でも読み書きできます**。
  家族以外にURLを共有しないでください。アカウント乗っ取り等のリスクがあります。
  長期的には認証の追加を強く推奨します。
- Fly.io のアカウントと `flyctl` がインストール済みであること。
  - 未導入なら：`curl -L https://fly.io/install.sh | sh`（Linux/macOS）
  - Windows：<https://fly.io/docs/hands-on/install-flyctl/>
- クレジットカード登録が必要（フリーの範囲で収まるはず）

---

## 初回セットアップ（1回だけ）

### 1. ログイン

```bash
flyctl auth login
```

ブラウザが開くのでログイン。

### 2. アプリを作成（deploy はまだしない）

プロジェクトルート `family-schedule/` で：

```bash
cd /path/to/family-schedule
flyctl launch --copy-config --no-deploy
```

- 質問が出るので：
  - アプリ名：`family-schedule-ueda` など任意の一意名を指定（既に埋まっていたら別名に）
  - リージョン：`nrt (Tokyo)` を選択
  - Postgres／Redis を足すか：**Postgres は次のステップでやる**ので **No**
  - デプロイ：**No**（`--no-deploy` 指定済）
- 実行後、`fly.toml` の `app = ...` が実名に書き換わります。

### 3. PostgreSQL を別アプリとして作成

```bash
flyctl postgres create --name family-schedule-db --region nrt
```

- プラン：**Development - 1x shared CPU, 256MB RAM, 1GB disk**（無料枠）
- 完了したら **PG の superuser パスワードが表示される**ので必ずメモ

### 4. アプリに PG をアタッチ

```bash
flyctl postgres attach family-schedule-db -a family-schedule-ueda
```

これで `DATABASE_URL` が**アプリのシークレットに自動設定**されます。
ただし Spring Boot は Heroku 形式（`postgres://`）をそのままは読めないので、
個別に環境変数を設定します（次ステップ）。

### 5. シークレット（環境変数）を設定

`flyctl postgres attach` が返した `DATABASE_URL` を使って、
Spring Boot 用の3変数を手で設定します。

```bash
# DATABASE_URL の形式：
#   postgres://<user>:<password>@<host>:5432/<db>
# これを jdbc 形式に直して投入
flyctl secrets set \
  SPRING_DATASOURCE_URL="jdbc:postgresql://<host>:5432/<db>?sslmode=require" \
  SPRING_DATASOURCE_USERNAME="<user>" \
  SPRING_DATASOURCE_PASSWORD="<password>" \
  -a family-schedule-ueda
```

※ `<host>` は内部アドレス（例：`family-schedule-db.internal`）で可。
   `DATABASE_URL` 全体は `flyctl secrets list` では値が見えないので、
   `flyctl postgres attach` の出力をメモしておくのがコツ。

### 6. デプロイ

```bash
flyctl deploy -a family-schedule-ueda
```

Docker ビルド → push → 起動、が走ります。初回は5〜10分程度。

### 7. 動作確認

```bash
flyctl status -a family-schedule-ueda
flyctl open -a family-schedule-ueda
```

ブラウザで `https://family-schedule-ueda.fly.dev/` が開きます。
URL を家族のスマホに LINE などで共有 → ホーム画面に追加してもらうのが楽。

---

## ふだんのデプロイ（コードを変えたあと）

```bash
flyctl deploy -a family-schedule-ueda
```

`fly.toml` の `app` 名が設定済みなら `-a` 省略可。

---

## トラブルシュート

### ログ確認

```bash
flyctl logs -a family-schedule-ueda
```

### DB に直接つなぐ

```bash
flyctl postgres connect -a family-schedule-db
# psql が開く。例：
#   \dt         -- テーブル一覧
#   SELECT * FROM schedules;
```

### コンテナに入る

```bash
flyctl ssh console -a family-schedule-ueda
```

### リスタート

```bash
flyctl apps restart family-schedule-ueda
```

---

## コスト目安

- アプリ：shared-cpu-1x / 512MB / 常時1台稼働
- PG：Development プラン（1GB ストレージ）
- 家族5人の利用なら **無料枠内に収まる**見込み
- 想定外に超えないように：
  ```bash
  flyctl billing                       # 請求状況確認
  flyctl scale vm shared-cpu-1x -a ... # スケール維持
  ```

---

## 既知の制約と次にやること

| 項目 | 状況 |
|---|---|
| 認証 | **無し**。URL知れば誰でもアクセス可能 → BL-06 を次に検討 |
| HTTPS | Fly.io が自動で対応 |
| タイムゾーン | `Asia/Tokyo` を JVM に渡すなら `-Duser.timezone=Asia/Tokyo` を `JAVA_TOOL_OPTIONS` に追加 |
| バックアップ | PG は Fly.io が自動スナップショットを取るが、定期 `pg_dump` 推奨 |

---

## 代替案：Render へデプロイしたくなった場合

`render.yaml` を別途用意すれば Render にも出せます。ただし無料プランは
15分無通信でスリープし、起動に30〜60秒かかるため、「パッと開く」体験は
Fly.io より劣化します。必要なら追加対応します。

---

## Docker でローカルにまとめて動かす

「自宅PCを常時起動サーバーにしたい」「クラウドより先にDocker一発で試したい」
ときの手順です。`docker-compose.yml` で **アプリ + PostgreSQL** を一緒に上げます。

### 前提

- Docker Engine / Docker Desktop と `docker compose` コマンド
- ローカルでポート 8080 が空いていること

### 起動

プロジェクトルート（`family-schedule/`）で：

```bash
# 初回はビルドから（5〜10分）
docker compose up -d --build

# ログ確認
docker compose logs -f app
# 「Tomcat started on port 8080」が出れば起動完了

# ブラウザで
#   http://localhost:8080/
```

LAN 上の他端末（家族のスマホ）から見せたい場合：

```bash
# サーバーのLAN IPを確認
hostname -I

# スマホのブラウザで
#   http://192.168.x.x:8080/
```

ファイアウォールで 8080 を開ける必要があるかもしれません。

### 停止と再起動

```bash
docker compose down              # 停止（DBデータは volume に残る）
docker compose up -d             # 再起動（ビルド済イメージから）
docker compose down -v           # ★データごと消す（要注意）
```

### コードを変更したあと

```bash
docker compose up -d --build     # 再ビルドして起動し直す
```

### DB に直接つなぐ

```bash
docker compose exec db psql -U family family_schedule
```

### 構成図

```
┌──────────────────────────────────────────────┐
│  docker compose                              │
│  ┌─────────────┐         ┌────────────────┐ │
│  │  app        │────────▶│  db (postgres) │ │
│  │  :8080      │  jdbc   │                │ │
│  │  Spring Boot│         │  volume:pgdata │ │
│  └──────┬──────┘         └────────────────┘ │
│         │                                    │
└─────────┼────────────────────────────────────┘
          │
          ▼  (8080 をホストに公開)
   家族のスマホ／PCのブラウザ
```

### Docker 経由のクラウドデプロイ

`docker-compose.yml` をそのまま動かせるサービスもあります：
- **Fly.io**：`docker-compose.yml` は使わず、`Dockerfile` だけ使うので別途上記の Fly.io 手順を参照
- **Railway / Coolify / Dokploy**：`docker-compose.yml` をそのまま読んでくれるサービスあり
- **VPS（さくら/Vultr/Linode）+ Docker**：`docker compose up -d` を VPS で叩くだけ

家族5人の用途なら、自宅PCに Docker で常駐 → ルーターでポート転送 or VPN という
方式も現実的です（NFR-08 通り、認証なしのMVPなので外公開は VPN 経由を推奨）。
