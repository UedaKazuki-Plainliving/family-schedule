ALTER TABLE schedules ADD COLUMN deleted_at TIMESTAMP WITH TIME ZONE;
CREATE INDEX ix_schedules_deleted_at ON schedules(deleted_at);
