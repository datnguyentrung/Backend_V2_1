-- V6: Course schedule change support (pending schedule). Operational state lives on catalog.course.

ALTER TABLE catalog.course
    ADD COLUMN IF NOT EXISTS next_schedule_id UUID
        REFERENCES catalog.class_schedule(schedule_id);
ALTER TABLE catalog.course
    ADD COLUMN IF NOT EXISTS next_schedule_effective_from DATE;

ALTER TABLE catalog.course
    ADD CONSTRAINT ck_course_next_schedule_pair CHECK (
        (next_schedule_id IS NULL AND next_schedule_effective_from IS NULL)
        OR (next_schedule_id IS NOT NULL AND next_schedule_effective_from IS NOT NULL)
    );

CREATE INDEX IF NOT EXISTS idx_course_next_schedule_effective
    ON catalog.course(next_schedule_effective_from);

-- At most one non-cancelled session per course per day.
CREATE UNIQUE INDEX IF NOT EXISTS uk_class_session_course_date_active
    ON training.class_session(course_id, session_date)
    WHERE status <> 'CANCELLED';

CREATE INDEX IF NOT EXISTS idx_class_session_lifecycle
    ON training.class_session(status, session_date, start_time);
CREATE INDEX IF NOT EXISTS idx_class_session_closure
    ON training.class_session(status, is_attendance_closed, session_date);
CREATE INDEX IF NOT EXISTS idx_enrollment_status_period
    ON training.student_enrollment(status, start_date, end_date);
