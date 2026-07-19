ALTER TABLE operation.student_attendance
    ADD COLUMN IF NOT EXISTS class_session_id UUID;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_attendance_class_session'
          AND conrelid = 'operation.student_attendance'::regclass
    ) THEN
        ALTER TABLE operation.student_attendance
            ADD CONSTRAINT fk_student_attendance_class_session
            FOREIGN KEY (class_session_id)
            REFERENCES operation.class_session(session_id)
            NOT VALID;
    END IF;
END $$;

ALTER TABLE operation.student_attendance
    VALIDATE CONSTRAINT fk_student_attendance_class_session;

CREATE INDEX IF NOT EXISTS idx_sa_class_session
    ON operation.student_attendance(class_session_id);
