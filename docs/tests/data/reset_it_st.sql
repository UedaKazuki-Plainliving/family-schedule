-- IT / ST 手動テスト用 リセットスクリプト
-- 実行タイミング: IT または ST のテスト実施前（毎回）
-- 対象DB: PostgreSQL（ローカル Docker Compose: localhost:5432/family_schedule）
--
-- 実行方法:
--   docker exec -i family-schedule-db psql -U family -d family_schedule -f docs/tests/data/reset_it_st.sql
-- または
--   docker compose exec db psql -U family -d family_schedule -f /dev/stdin < docs/tests/data/reset_it_st.sql

-- ========================================
-- Step 1: スケジュールを全削除（物理削除）
-- ========================================
TRUNCATE TABLE schedules RESTART IDENTITY CASCADE;

-- ========================================
-- Step 2: メンバーをテスト追加分（id=6以降）だけ削除
-- ========================================
DELETE FROM members WHERE id > 5;

-- id=1〜5 の名前を初期状態に戻す（テスト中に名前変更した場合のリセット）
UPDATE members SET name = 'お父さん', display_order = 1 WHERE id = 1;
UPDATE members SET name = 'お母さん', display_order = 2 WHERE id = 2;
UPDATE members SET name = '長女',     display_order = 3 WHERE id = 3;
UPDATE members SET name = '次女',     display_order = 4 WHERE id = 4;
UPDATE members SET name = '長男',     display_order = 5 WHERE id = 5;

-- ========================================
-- 確認クエリ（実行後に確認してください）
-- ========================================
-- メンバー5名・スケジュール0件になっていれば成功
SELECT id, name, display_order FROM members ORDER BY id;
SELECT COUNT(*) AS schedule_count FROM schedules;
