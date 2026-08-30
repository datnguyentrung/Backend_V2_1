-- V5: Chuẩn hóa schema runtime theo thiết kế ClassSession theo Course.
-- 1) Bổ sung course.name (entity đã có, DB chưa có cột).
ALTER TABLE catalog.course ADD COLUMN IF NOT EXISTS name VARCHAR(255);
UPDATE catalog.course
   SET name = 'COURSE-' || LEFT(course_id::text, 8)
 WHERE name IS NULL OR name = '';
ALTER TABLE catalog.course ALTER COLUMN name SET NOT NULL;

-- 2) Chuẩn hóa person_code của hai system person sang prefix VQT_.
UPDATE core.person SET person_code = 'VQT_SUPER_ADMIN' WHERE person_code = 'SYS-SUPER-ADMIN';
UPDATE core.person SET person_code = 'VQT_SYSTEM_ADMIN' WHERE person_code = 'SYS-SYSTEM-ADMIN';

-- 3) person_code bắt buộc có giá trị và có prefix VQ_ / VQT_; sai dữ liệu thì fail migration.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM core.person
        WHERE person_code IS NULL
           OR NOT (person_code LIKE 'VQ\_%' OR person_code LIKE 'VQT\_%')
    ) THEN
        RAISE EXCEPTION 'person_code must be NOT NULL and start with VQ_ or VQT_ prefix';
    END IF;
END $$;

ALTER TABLE core.person ALTER COLUMN person_code SET NOT NULL;
ALTER TABLE core.person
    ADD CONSTRAINT ck_person_code_prefix CHECK (
        person_code LIKE 'VQ\_%' OR person_code LIKE 'VQT\_%'
    );

-- 4) Dọn FK lỗi còn sót từ student_attendance.coach_assignment_id -> core.person.
DO $$
DECLARE
    fk_name text;
BEGIN
    FOR fk_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = 'training'
          AND t.relname = 'student_attendance'
          AND c.contype = 'f'
          AND c.confrelid = 'core.person'::regclass
          AND EXISTS (
              SELECT 1
              FROM pg_attribute a
              WHERE a.attrelid = c.conrelid
                AND a.attnum = ANY (c.conkey)
                AND a.attname = 'coach_assignment_id'
          )
    LOOP
        EXECUTE format('ALTER TABLE training.student_attendance DROP CONSTRAINT %I', fk_name);
    END LOOP;
END $$;

-- FK mới tới coach_assignment (thêm NOT VALID ở V2) được validate.
ALTER TABLE training.student_attendance
    VALIDATE CONSTRAINT fk_student_attendance_coach_assignment;
