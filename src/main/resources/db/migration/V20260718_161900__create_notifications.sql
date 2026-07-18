CREATE SCHEMA IF NOT EXISTS operation;

CREATE TABLE IF NOT EXISTS operation.notification (
    notification_id uuid PRIMARY KEY,
    title varchar(150) NOT NULL,
    body varchar(1000) NOT NULL,
    notification_type varchar(40) NOT NULL DEFAULT 'SYSTEM',
    reference_type varchar(100),
    reference_id varchar(100),
    payload jsonb,
    created_at timestamp NOT NULL
);

CREATE TABLE IF NOT EXISTS operation.notification_recipient (
    notification_recipient_id uuid PRIMARY KEY,
    notification_id uuid NOT NULL,
    recipient_user_id uuid NOT NULL,
    is_read boolean NOT NULL DEFAULT false,
    read_at timestamp,
    delivered_at timestamp,
    recipient_status varchar(30) NOT NULL DEFAULT 'PENDING',
    created_at timestamp NOT NULL,
    updated_at timestamp,
    CONSTRAINT fk_notification_recipient_notification
        FOREIGN KEY (notification_id)
        REFERENCES operation.notification(notification_id)
        ON DELETE CASCADE,
    CONSTRAINT fk_notification_recipient_user
        FOREIGN KEY (recipient_user_id)
        REFERENCES security."user"(user_id)
        ON DELETE CASCADE,
    CONSTRAINT uk_notification_recipient
        UNIQUE (notification_id, recipient_user_id)
);

CREATE INDEX IF NOT EXISTS idx_notification_created_at
    ON operation.notification (created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_type_created
    ON operation.notification (notification_type, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_nr_user_created
    ON operation.notification_recipient (recipient_user_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_nr_user_read_created
    ON operation.notification_recipient (recipient_user_id, is_read, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_nr_user_status_created
    ON operation.notification_recipient (recipient_user_id, recipient_status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_nr_notification
    ON operation.notification_recipient (notification_id);
