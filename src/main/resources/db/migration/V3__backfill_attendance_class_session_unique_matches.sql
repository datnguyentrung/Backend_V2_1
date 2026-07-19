WITH matching_sessions AS (
    SELECT
        sa.attendance_id,
        MIN(cs.session_id) AS class_session_id,
        COUNT(cs.session_id) AS matching_session_count
    FROM operation.student_attendance sa
    JOIN operation.student_enrollment se
        ON se.enrollment_id = sa.student_enrollment_id
    JOIN operation.class_session cs
        ON cs.class_schedule_schedule_id = se.schedule_id
       AND cs.session_date = sa.session_date
    WHERE sa.class_session_id IS NULL
    GROUP BY sa.attendance_id
)
UPDATE operation.student_attendance sa
SET class_session_id = matching_sessions.class_session_id
FROM matching_sessions
WHERE sa.attendance_id = matching_sessions.attendance_id
  AND matching_sessions.matching_session_count = 1;
