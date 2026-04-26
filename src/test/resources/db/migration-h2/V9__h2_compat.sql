-- H2 test compat fixes:
-- content 列: H2 は VARCHAR(N) を UTF-16 code unit（Java String.length()）で計測する。
-- PostgreSQL は Unicode code point で計測するため、
-- アプリバリデーター（codePointCount <= 100）が通した 100絵文字（= 200 code units）を
-- H2 の VARCHAR(100) が拒否してしまう。
-- 本番制約は PostgreSQL 側で保証するため、H2 テスト環境では VARCHAR(400) に広げる。
ALTER TABLE schedules ALTER COLUMN content VARCHAR(400);
