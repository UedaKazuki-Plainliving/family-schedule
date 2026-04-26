-- ET-005: メンバーIDの手動採番をシーケンスに置き換え（TOCTOU競合を解消）
CREATE SEQUENCE IF NOT EXISTS members_id_seq START WITH 100 INCREMENT BY 1;
ALTER TABLE members ALTER COLUMN id SET DEFAULT nextval('members_id_seq');
