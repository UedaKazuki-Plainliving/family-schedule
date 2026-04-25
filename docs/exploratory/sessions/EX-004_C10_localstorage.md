# EX-004：C10 クライアント状態の腐敗（LocalStorage）

| 項目 | 値 |
|---|---|
| Charter | C10 |
| 担当 | Claude |
| 開始 | 2026-04-25 00:01 UTC |
| タイムボックス | 30分 |
| 実時間 | 約10分 |
| Setup% / Test% / Bug% | 10% / 70% / 20% |
| 関連機能 | 利用者選択 / LocalStorage |

## ミッション
> Explore 「LocalStorage」 with 「壊れたJSON、不正なid、削除されたmemberId、超巨大文字列保存」 to discover 「起動失敗、白画面、無限ループ」

## テスト実施内容と結果（E2E：`LocalStorageExploreTest`）

| # | テストケース | 期待 | 実結果 | 判定 |
|---|---|---|---|---|
| T1 | LocalStorage に `"this is not json"` | JSON.parseで失敗 → S-01 表示 | S-01 表示で復帰 | OK |
| T2 | id=99（存在しないmemberId） | できれば S-01 へ強制復帰 | **そのまま S-02 が表示される**。current-user-name に "hacker" と表示 | **OBS-9** |
| T3 | id="abc"（文字列） | 同上 | **そのまま S-02 が表示される**。＋追加した時の保存だけ失敗 | **OBS-9** |
| T4 | `{}`（id 無し） | S-01 へ復帰 | S-01 表示 OK | OK |

## 検出された観察

- **OBS-9（軽微・UX）**：LocalStorage に「実在しない／不正な型のメンバーID」を仕込むとそのままスケジュール画面が表示され、ヘッダに「hacker」のような偽名が出る。閲覧自体は出来てしまい（現実には認証なしのMVPなので影響極小）、`+追加`した時に初めて 400 でエラーが出る。
  - 推奨対応：起動時に `currentUser.id` が `/api/members` の結果に含まれることを検証し、なければ S-01 に戻す。

## OK だった点
- 壊れた JSON、空 JSON は try/catch で捕まえ S-01 にフォールバックする実装あり。白画面化はなし。

## 次の探索アイデア
- LocalStorage に巨大文字列（5MB）を入れて起動 → ブラウザ実装依存だが描画遅延の確認
- LocalStorage を別タブから書き換えた際の `storage` イベント未対応の挙動
