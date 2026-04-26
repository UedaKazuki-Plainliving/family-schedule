# 探索的テスト 第2ラウンド 最終レポート

- 対象：家族スケジュール共有システム MVP
- 実施日：2026-04-26
- 手法：5専門家同時テスト（機能・バグパターン・ビジネス要件・UX・非機能セキュリティ）
- 検出件数：**21件（ET-001〜ET-021）**

---

## 1. 専門家別テスト観点と検出概要

| 専門家 | 観点 | 主な検出 |
|--------|------|---------|
| 機能テスト開発屋 | 境界値・データフロー・ホワイトボックス | ET-004（日付必須バリデーション欠落）、ET-005（TOCTOU競合）、ET-006（DB列長不整合）、ET-009（絵文字名前長）、ET-012（PUT の BUG-VALIDATOR 波及） |
| バグパターン屋 | WEB系頻出バグ・ブラックボックス | ET-003（ソフト削除 + メンバー削除競合）、ET-007（pending undo 確定時の UI 更新漏れ）、ET-010（auto-purge 失敗のサイレント無視）、ET-011（タッチ後ゴーストクリック）、ET-015（モーダル閉鎖で暗黙保存） |
| ビジネス要件専門家 | 実使用シナリオ・ユーザビリティ | ET-013（「今日に戻る」無効状態未表示）、ET-014（過去日閲覧時の新規追加日付）、ET-016（DataIntegrityViolation のメッセージ区別） |
| UX専門家 | 操作性・デザイン・見やすさ | ET-001（✕ボタン タップ領域 20×20px 不足）、ET-017（削除確認ダイアログのボタンラベル）、ET-018（ユーザーボタン font-size 14px → 16px） |
| 非機能セキュリティ専門家 | 堅牢性・アクセシビリティ・パフォーマンス | ET-008（ヘッダードラッグ誤操作）、ET-019（user-scalable=no WCAG 2.1 SC 1.4.4 違反） |

---

## 2. 全検出項目と対応状況

| ET番号 | 分類 | 概要 | 重大度 | ステータス |
|--------|------|------|--------|---------|
| ET-001 | UX | ✕ボタンのタップ領域が 20×20px（44px 未達） | High | ✅ 修正済み |
| ET-002 | UX | スケジュール一覧の「予定なし」表示が薄すぎる（スキップ） | Low | ⏸ バックログ |
| ET-003 | 機能 | ソフト削除予定が残るとメンバーを物理削除できない | High | ✅ 修正済み |
| ET-004 | 機能 | フォーム送信時に日付未入力チェックがない | High | ✅ 修正済み |
| ET-005 | 機能 | メンバーID手動採番に TOCTOU 競合の可能性 | Medium | ✅ 修正済み |
| ET-006 | 機能 | DB の content 列が VARCHAR(400) でアプリ上限(100)と不整合 | Medium | ✅ 修正済み |
| ET-007 | 機能 | commitPendingUndo 実行時にトースト非表示・失敗通知がない | Medium | ✅ 修正済み |
| ET-008 | 機能 | ヘッダー上でのマウスダウンがフリックとして誤検知 | Medium | ✅ 修正済み |
| ET-009 | 機能 | MemberService の名前長チェックが String.length()（絵文字で2カウント） | Medium | ✅ 修正済み |
| ET-010 | 機能 | auto-purge（5秒後）の失敗がサイレント無視 | Low | ✅ 修正済み |
| ET-011 | 機能 | タッチ操作後のゴーストマウスイベントが誤フリックを引き起こす | Medium | ✅ 修正済み |
| ET-012 | 機能 | BUG-VALIDATOR が PUT（更新）にも波及（create 同様） | High | ✅ 修正済み |
| ET-013 | UX | 今日表示中も「今日に戻る」ボタンが有効のまま | Low | ✅ 修正済み |
| ET-014 | UX | 過去日閲覧中に新規追加すると過去日がデフォルト設定される | Medium | ✅ 修正済み |
| ET-015 | 機能 | メンバーリネーム中にモーダルを閉じると blur → 暗黙保存 | High | ✅ 修正済み |
| ET-016 | UX | DataIntegrityViolationException のエラーメッセージが用途に関わらず固定 | Low | ✅ 修正済み |
| ET-017 | UX | 削除確認ダイアログのボタンが「いいえ/はい」（意図が不明瞭） | Medium | ✅ 修正済み |
| ET-018 | アクセシビリティ | 現在のユーザーボタンの font-size が 14px（16px 未達） | Medium | ✅ 修正済み |
| ET-019 | アクセシビリティ | viewport に user-scalable=no（WCAG 2.1 SC 1.4.4 違反） | High | ✅ 修正済み |
| ET-020 | セキュリティ | CORS の preflight 拒否を確認（問題なし） | - | ✅ 問題なし |
| ET-021 | セキュリティ | LocalStorage 破損時の復旧確認（問題なし） | - | ✅ 問題なし |

**対応完了：19件 / バックログ：1件（ET-002）/ 問題なし：2件（ET-020/021）**

---

## 3. 修正ファイル一覧

| ファイル | 修正内容 |
|---------|---------|
| `V4__member_id_sequence.sql` | ET-005: members_id_seq シーケンス追加 |
| `V5__content_max_100.sql` | ET-006: content 列長 400→100 |
| `Member.java` | ET-005: `@GeneratedValue(SEQUENCE)` 追加、コンストラクタ変更 |
| `MemberRepository.java` | ET-005: `findMaxId()` 削除 |
| `MemberService.java` | ET-003: 削除前ソフト削除予定をpurge、ET-009: codePointCount |
| `ScheduleRepository.java` | ET-003: `deleteByMemberIdAndDeletedAtIsNotNull()` 追加 |
| `ScheduleValidator.java` | ET-012/BUG-VALIDATOR: 動的 validMemberIds 引数化 |
| `ScheduleService.java` | ET-012/BUG-VALIDATOR: `nameById().keySet()` を渡す |
| `Schedule.java` | ET-006: `@Column(length=100)` |
| `GlobalExceptionHandler.java` | ET-016: unique/FK 制約エラーメッセージ分岐 |
| `ScheduleValidatorTest.java` | 全テスト: validate() 第2引数追加 |
| `style.css` | ET-001: ✕ボタン 44px、ET-018: font-size 16px、ET-013: disabled スタイル |
| `index.html` | ET-019: user-scalable=no 削除、ET-017: ボタンラベル変更 |
| `app.js` | ET-004/007/008/010/011/013/014/015: 各種 JS 修正 |

---

## 4. セッション記録

各専門家の仮説・手順・発見の連鎖は個別セッションファイルを参照。

| セッション | 担当 | ファイル |
|---|---|---|
| EX-006 | 機能テスト開発屋 | [sessions/EX-006_機能テスト開発屋.md](sessions/EX-006_機能テスト開発屋.md) |
| EX-007 | バグパターン屋 | [sessions/EX-007_バグパターン屋.md](sessions/EX-007_バグパターン屋.md) |
| EX-008 | ビジネス要件専門家 | [sessions/EX-008_ビジネス要件専門家.md](sessions/EX-008_ビジネス要件専門家.md) |
| EX-009 | UX専門家 | [sessions/EX-009_UX専門家.md](sessions/EX-009_UX専門家.md) |
| EX-010 | 非機能セキュリティ専門家 | [sessions/EX-010_非機能セキュリティ専門家.md](sessions/EX-010_非機能セキュリティ専門家.md) |

---

## 5. バックログ追加項目

| ID | 内容 | 由来 |
|----|------|------|
| BL-24 | 「予定なし」表示のコントラスト改善 | ET-002 |
