-- 負荷テスト後のクリーンアップ
-- 実行タイミング: k6 負荷テスト完了後
-- 対象DB: PostgreSQL（本番環境 EC2: 54.162.107.130:8082）

-- ========================================
-- 負荷テスト用シードデータの削除
-- ========================================
-- seed_at.sql または 06_load_test_spec.md Section 7 で投入したデータを削除

-- 負荷テスト予定（content が '負荷テスト予定%' のもの）を削除
DELETE FROM schedules WHERE content LIKE '負荷テスト予定%';

-- k6 スクリプト内でクリーンアップされなかった残存データを削除
DELETE FROM schedules WHERE content IN ('負荷テスト', 'ストレステスト');

-- ========================================
-- 論理削除済みデータの完全削除（任意）
-- ========================================
-- k6 テスト中に論理削除（DELETE）されたが purge されていないレコードが残る場合がある
-- 必要であれば以下を実行：

-- DELETE FROM schedules WHERE deleted_at IS NOT NULL;

-- ========================================
-- 確認用クエリ
-- ========================================
SELECT
  COUNT(*) FILTER (WHERE deleted_at IS NULL) AS "有効なスケジュール数",
  COUNT(*) FILTER (WHERE deleted_at IS NOT NULL) AS "論理削除済み数"
FROM schedules;
