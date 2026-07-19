DO $$
DECLARE
    unresolved_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO unresolved_count
    FROM operation.student_attendance_class_session_unresolved;

    IF unresolved_count > 0 THEN
        RAISE EXCEPTION
            'Cannot enforce student_attendance class_session_id constraints: % unresolved or duplicate rows. Query operation.student_attendance_class_session_unresolved for details.',
            unresolved_count;
    END IF;
END $$;

ALTER TABLE operation.student_attendance
    ALTER COLUMN class_session_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_student_attendance_enrollment_session'
          AND conrelid = 'operation.student_attendance'::regclass
    ) THEN
        ALTER TABLE operation.student_attendance
            ADD CONSTRAINT uk_student_attendance_enrollment_session
            UNIQUE (student_enrollment_id, class_session_id);
    END IF;
END $$;

ALTER TABLE operation.student_attendance
    DROP CONSTRAINT IF EXISTS uk_student_enrollment_date;
