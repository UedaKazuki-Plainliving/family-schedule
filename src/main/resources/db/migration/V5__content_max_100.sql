-- ET-006: DBのcontent列長をバリデーション上限(100)に合わせる
ALTER TABLE schedules ALTER COLUMN content TYPE VARCHAR(100);
