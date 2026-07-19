CREATE OR REPLACE VIEW operation.student_attendance_class_session_unresolved AS
WITH match_counts AS (
    SELECT
        sa.attendance_id,
        sa.student_enrollment_id,
        sa.session_date,
        COUNT(cs.session_id) AS matching_session_count,
        ARRAY_REMOVE(ARRAY_AGG(cs.session_id), NULL) AS matching_session_ids
    FROM operation.student_attendance sa
    JOIN operation.student_enrollment se
        ON se.enrollment_id = sa.student_enrollment_id
    LEFT JOIN operation.class_session cs
        ON cs.class_schedule_schedule_id = se.schedule_id
       AND cs.session_date = sa.session_date
    WHERE sa.class_session_id IS NULL
    GROUP BY sa.attendance_id, sa.student_enrollment_id, sa.session_date
),
duplicate_attendance AS (
    SELECT
        student_enrollment_id,
        class_session_id,
        COUNT(*) AS duplicate_count,
        ARRAY_AGG(attendance_id ORDER BY attendance_id) AS attendance_ids
    FROM operation.student_attendance
    WHERE class_session_id IS NOT NULL
    GROUP BY student_enrollment_id, class_session_id
    HAVING COUNT(*) > 1
)
SELECT
    attendance_id,
    student_enrollment_id,
    NULL::UUID AS class_session_id,
    session_date,
    CASE
        WHEN matching_session_count = 0 THEN 'NO_MATCHING_CLASS_SESSION'
        ELSE 'MULTIPLE_MATCHING_CLASS_SESSIONS'
    END AS issue_type,
    matching_session_count,
    matching_session_ids,
    NULL::BIGINT AS duplicate_count,
    NULL::UUID[] AS duplicate_attendance_ids
FROM match_counts
WHERE matching_session_count <> 1

UNION ALL

SELECT
    UNNEST(attendance_ids) AS attendance_id,
    student_enrollment_id,
    class_session_id,
    NULL::DATE AS session_date,
    'DUPLICATE_ATTENDANCE_FOR_CLASS_SESSION' AS issue_type,
    NULL::BIGINT AS matching_session_count,
    NULL::UUID[] AS matching_session_ids,
    duplicate_count,
    attendance_ids AS duplicate_attendance_ids
FROM duplicate_attendance;
