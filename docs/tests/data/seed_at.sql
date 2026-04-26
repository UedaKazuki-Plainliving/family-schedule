-- 受け入れテスト（AT）用シードデータ
-- 対象DB: PostgreSQL（本番環境 / ステージング環境）
-- 投入タイミング: ATテスト実施の直前
-- 注意: 既存のスケジュールデータを削除してから投入すること（cleanup_at.sql を先に実行）

-- ========================================
-- 今日・明日・来月のサンプル予定を投入
-- ========================================

-- お父さん（id=1）の予定
INSERT INTO schedules (member_id, date, content, created_at, updated_at) VALUES
  (1, CURRENT_DATE,        '朝のミーティング',     now(), now()),
  (1, CURRENT_DATE + 1,    '出張（帰りは遅い）',   now(), now()),
  (1, CURRENT_DATE + 30,   '年次休暇',             now(), now());

-- お母さん（id=2）の予定
INSERT INTO schedules (member_id, date, content, created_at, updated_at) VALUES
  (2, CURRENT_DATE,        'PTAの集まり',          now(), now()),
  (2, CURRENT_DATE + 1,    '歯医者（14:00）',      now(), now()),
  (2, CURRENT_DATE + 30,   '旅行の準備',           now(), now());

-- 長女（id=3）の予定
INSERT INTO schedules (member_id, date, content, created_at, updated_at) VALUES
  (3, CURRENT_DATE,        '部活（バスケ）',       now(), now()),
  (3, CURRENT_DATE + 1,    'テスト勉強',           now(), now());

-- 次女（id=4）の予定
INSERT INTO schedules (member_id, date, content, created_at, updated_at) VALUES
  (4, CURRENT_DATE,        '習い事（ピアノ）',     now(), now()),
  (4, CURRENT_DATE + 1,    '遠足',                 now(), now());

-- 長男（id=5）の予定
INSERT INTO schedules (member_id, date, content, created_at, updated_at) VALUES
  (5, CURRENT_DATE,        'サッカー練習',         now(), now()),
  (5, CURRENT_DATE + 1,    '公園（友達と）',       now(), now());
