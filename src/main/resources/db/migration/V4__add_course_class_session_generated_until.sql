ALTER TABLE catalog.course
    ADD COLUMN IF NOT EXISTS class_session_generated_until DATE;

COMMENT ON COLUMN catalog.course.class_session_generated_until IS
    'Ngày cuối cùng mà hệ thống đã sinh buổi học tự động cho khóa học.';
