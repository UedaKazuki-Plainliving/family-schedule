CREATE TABLE members (
    id            INTEGER     PRIMARY KEY,
    name          VARCHAR(50) NOT NULL UNIQUE,
    display_order INTEGER     NOT NULL
);

INSERT INTO members (id, name, display_order) VALUES
    (1, 'お父さん', 1),
    (2, 'お母さん', 2),
    (3, 'そよ',     3),
    (4, 'ゆうり',   4),
    (5, 'いちろう', 5);

CREATE TABLE schedules (
    id          BIGSERIAL PRIMARY KEY,
    member_id   INTEGER NOT NULL REFERENCES members(id) ON DELETE RESTRICT,
    date        DATE    NOT NULL,
    content     VARCHAR(400) NOT NULL CHECK (char_length(content) >= 1),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX ix_schedules_date_member ON schedules(date, member_id);
