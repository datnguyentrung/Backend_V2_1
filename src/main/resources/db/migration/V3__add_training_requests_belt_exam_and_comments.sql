DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'core'
          AND table_name = 'person'
          AND column_name = 'belt'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'core'
          AND table_name = 'person'
          AND column_name = 'current_belt'
    ) THEN
        ALTER TABLE core.person RENAME COLUMN belt TO current_belt;
    END IF;
END $$;

ALTER TABLE core.person DROP CONSTRAINT IF EXISTS ck_person_belt;
ALTER TABLE core.person DROP CONSTRAINT IF EXISTS ck_person_current_belt;
ALTER TABLE core.person
    ADD CONSTRAINT ck_person_current_belt CHECK (current_belt IN (
        'C10', 'C9', 'C8', 'C7', 'C6', 'C5', 'C4', 'C3', 'C2', 'C1',
        'D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'D7', 'D8', 'D9', 'D10'
    ));

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'finance'
          AND table_name = 'wallet_transaction'
          AND column_name = 'approved_by_user_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'finance'
          AND table_name = 'wallet_transaction'
          AND column_name = 'reviewed_by_user_id'
    ) THEN
        ALTER TABLE finance.wallet_transaction RENAME COLUMN approved_by_user_id TO reviewed_by_user_id;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'finance'
          AND table_name = 'wallet_transaction'
          AND column_name = 'approved_at'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'finance'
          AND table_name = 'wallet_transaction'
          AND column_name = 'reviewed_at'
    ) THEN
        ALTER TABLE finance.wallet_transaction RENAME COLUMN approved_at TO reviewed_at;
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'training'
          AND table_name = 'student_attendance'
          AND column_name = 'evaluated_by_coach_id'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'training'
          AND table_name = 'student_attendance'
          AND column_name = 'coach_assignment_id'
    ) THEN
        ALTER TABLE training.student_attendance RENAME COLUMN evaluated_by_coach_id TO coach_assignment_id;
    END IF;
END $$;

ALTER TABLE training.student_attendance
    DROP CONSTRAINT IF EXISTS student_attendance_evaluated_by_coach_id_fkey;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_student_attendance_coach_assignment'
          AND conrelid = 'training.student_attendance'::regclass
    ) THEN
        ALTER TABLE training.student_attendance
            ADD CONSTRAINT fk_student_attendance_coach_assignment
            FOREIGN KEY (coach_assignment_id)
            REFERENCES training.coach_assignment(coach_assignment_id)
            NOT VALID;
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS training.belt_exam (
    belt_exam_id UUID PRIMARY KEY,
    person_id UUID NOT NULL REFERENCES core.person(person_id),
    from_belt VARCHAR(20) NOT NULL,
    target_belt VARCHAR(20) NOT NULL,
    year INTEGER NOT NULL,
    quarter INTEGER NOT NULL,
    exam_date DATE,
    result VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    note VARCHAR(1000),
    created_by_user_id UUID NOT NULL REFERENCES security.users(user_id),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    type VARCHAR(20) NOT NULL,
    CONSTRAINT ck_belt_exam_from_belt CHECK (from_belt IN (
        'C10', 'C9', 'C8', 'C7', 'C6', 'C5', 'C4', 'C3', 'C2', 'C1',
        'D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'D7', 'D8', 'D9', 'D10'
    )),
    CONSTRAINT ck_belt_exam_target_belt CHECK (target_belt IN (
        'C10', 'C9', 'C8', 'C7', 'C6', 'C5', 'C4', 'C3', 'C2', 'C1',
        'D1', 'D2', 'D3', 'D4', 'D5', 'D6', 'D7', 'D8', 'D9', 'D10'
    )),
    CONSTRAINT ck_belt_exam_result CHECK (result IN ('PASSED', 'FAILED', 'ABSENT', 'PENDING')),
    CONSTRAINT ck_belt_exam_type CHECK (type IN ('PROMOTION', 'MOCK')),
    CONSTRAINT chk_belt_exam_business_rule CHECK (
        year > 0
        AND quarter BETWEEN 1 AND 4
        AND from_belt <> target_belt
        AND (
            result = 'PENDING'
            OR exam_date IS NOT NULL
        )
    )
);
CREATE INDEX IF NOT EXISTS idx_belt_exam_person_period ON training.belt_exam(person_id, year, quarter);
CREATE INDEX IF NOT EXISTS idx_belt_exam_type_result ON training.belt_exam(type, result);
CREATE INDEX IF NOT EXISTS idx_belt_exam_exam_date ON training.belt_exam(exam_date);
CREATE INDEX IF NOT EXISTS idx_belt_exam_created_by ON training.belt_exam(created_by_user_id);

CREATE TABLE IF NOT EXISTS training.leave_request (
    leave_request_id UUID PRIMARY KEY,
    person_id UUID NOT NULL REFERENCES core.person(person_id),
    requester_type VARCHAR(30) NOT NULL,
    leave_date DATE,
    leave_class_session_id UUID REFERENCES training.class_session(class_session_id),
    makeup_class_session_id UUID REFERENCES training.class_session(class_session_id),
    leave_context VARCHAR(1000) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by_user_id UUID NOT NULL REFERENCES security.users(user_id),
    reviewed_by_user_id UUID REFERENCES security.users(user_id),
    reviewed_at TIMESTAMP,
    review_note VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_leave_request_requester_type CHECK (requester_type IN ('STUDENT', 'SYSTEM_EMPLOYEE')),
    CONSTRAINT ck_leave_request_status CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    CONSTRAINT chk_leave_request_business_rule CHECK (
        (
            (
                requester_type = 'STUDENT'
                AND leave_class_session_id IS NOT NULL
                AND makeup_class_session_id IS NOT NULL
            )
            OR
            (
                requester_type = 'SYSTEM_EMPLOYEE'
                AND leave_date IS NOT NULL
            )
        )
        AND
        (
            leave_class_session_id IS NULL
            OR makeup_class_session_id IS NULL
            OR leave_class_session_id <> makeup_class_session_id
        )
        AND
        (
            status NOT IN ('APPROVED', 'REJECTED')
            OR (
                reviewed_by_user_id IS NOT NULL
                AND reviewed_at IS NOT NULL
            )
        )
        AND
        (
            status <> 'PENDING'
            OR (
                reviewed_by_user_id IS NULL
                AND reviewed_at IS NULL
            )
        )
    )
);
CREATE INDEX IF NOT EXISTS idx_leave_request_person_status ON training.leave_request(person_id, status);
CREATE INDEX IF NOT EXISTS idx_leave_request_type_status ON training.leave_request(requester_type, status);
CREATE INDEX IF NOT EXISTS idx_leave_request_leave_date ON training.leave_request(leave_date);
CREATE INDEX IF NOT EXISTS idx_leave_request_leave_session ON training.leave_request(leave_class_session_id);
CREATE INDEX IF NOT EXISTS idx_leave_request_makeup_session ON training.leave_request(makeup_class_session_id);
CREATE INDEX IF NOT EXISTS idx_leave_request_created_by ON training.leave_request(created_by_user_id);

CREATE OR REPLACE FUNCTION pg_temp.comment_schema_if_exists(target_schema text, comment_text text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = target_schema) THEN
        EXECUTE format('COMMENT ON SCHEMA %I IS %L', target_schema, comment_text);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.comment_table_if_exists(target_schema text, target_table text, comment_text text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF to_regclass(format('%I.%I', target_schema, target_table)) IS NOT NULL THEN
        EXECUTE format('COMMENT ON TABLE %I.%I IS %L', target_schema, target_table, comment_text);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.comment_column_if_exists(target_schema text, target_table text, target_column text, comment_text text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = target_schema
          AND table_name = target_table
          AND column_name = target_column
    ) THEN
        EXECUTE format('COMMENT ON COLUMN %I.%I.%I IS %L', target_schema, target_table, target_column, comment_text);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.comment_constraint_if_exists(target_schema text, target_table text, target_constraint text, comment_text text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = target_schema
          AND t.relname = target_table
          AND c.conname = target_constraint
    ) THEN
        EXECUTE format('COMMENT ON CONSTRAINT %I ON %I.%I IS %L', target_constraint, target_schema, target_table, comment_text);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION pg_temp.comment_index_if_exists(target_schema text, target_index text, comment_text text)
RETURNS void LANGUAGE plpgsql AS $$
BEGIN
    IF to_regclass(format('%I.%I', target_schema, target_index)) IS NOT NULL THEN
        EXECUTE format('COMMENT ON INDEX %I.%I IS %L', target_schema, target_index, comment_text);
    END IF;
END $$;

SELECT pg_temp.comment_schema_if_exists('security', 'Nhóm bảng bảo mật, tài khoản, vai trò, quyền và phiên đăng nhập.');
SELECT pg_temp.comment_schema_if_exists('core', 'Nhóm bảng lõi mô tả con người, chi nhánh và liên kết người dùng với hồ sơ cá nhân.');
SELECT pg_temp.comment_schema_if_exists('catalog', 'Nhóm bảng danh mục lịch học, khóa học và bảng giá.');
SELECT pg_temp.comment_schema_if_exists('training', 'Nhóm bảng nghiệp vụ đào tạo, buổi học, điểm danh, huấn luyện viên, thi đai và xin nghỉ.');
SELECT pg_temp.comment_schema_if_exists('finance', 'Nhóm bảng tài chính, ví, giao dịch ví và mua khóa học.');
SELECT pg_temp.comment_schema_if_exists('notification', 'Nhóm bảng thông báo và trạng thái nhận thông báo.');
SELECT pg_temp.comment_schema_if_exists('skill', 'Nhóm bảng kỹ năng, bài tập thể lực và lịch sử ghi nhận.');

SELECT pg_temp.comment_table_if_exists('security', 'users', 'Tài khoản đăng nhập vào hệ thống.');
SELECT pg_temp.comment_column_if_exists('security', 'users', 'user_id', 'Khóa chính định danh tài khoản.');
SELECT pg_temp.comment_column_if_exists('security', 'users', 'phone_number', 'Số điện thoại đăng nhập, duy nhất trong hệ thống.');
SELECT pg_temp.comment_column_if_exists('security', 'users', 'password_hash', 'Mật khẩu đã băm, không lưu mật khẩu gốc.');
SELECT pg_temp.comment_column_if_exists('security', 'users', 'user_status', 'Trạng thái tài khoản như chờ kích hoạt, hoạt động, khóa hoặc vô hiệu hóa.');
SELECT pg_temp.comment_column_if_exists('security', 'users', 'authorization_version', 'Phiên bản phân quyền dùng để vô hiệu hóa cache hoặc token quyền cũ.');
SELECT pg_temp.comment_column_if_exists('security', 'users', 'created_at', 'Thời điểm tạo tài khoản.');
SELECT pg_temp.comment_column_if_exists('security', 'users', 'updated_at', 'Thời điểm cập nhật tài khoản gần nhất.');
SELECT pg_temp.comment_column_if_exists('security', 'users', 'last_login_at', 'Thời điểm đăng nhập gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('security', 'users', 'ck_user_status', 'Giới hạn trạng thái tài khoản theo enum UserStatus.');

SELECT pg_temp.comment_table_if_exists('security', 'role', 'Vai trò hệ thống dùng để nhóm quyền.');
SELECT pg_temp.comment_column_if_exists('security', 'role', 'role_id', 'Mã vai trò, đồng thời là khóa chính.');
SELECT pg_temp.comment_column_if_exists('security', 'role', 'name', 'Tên hiển thị của vai trò.');
SELECT pg_temp.comment_column_if_exists('security', 'role', 'description', 'Mô tả mục đích sử dụng của vai trò.');
SELECT pg_temp.comment_column_if_exists('security', 'role', 'permission_version', 'Phiên bản tập quyền của vai trò để đồng bộ phân quyền.');

SELECT pg_temp.comment_table_if_exists('security', 'permission', 'Danh mục quyền thao tác trên từng resource.');
SELECT pg_temp.comment_column_if_exists('security', 'permission', 'permission_id', 'Khóa chính tự tăng của quyền.');
SELECT pg_temp.comment_column_if_exists('security', 'permission', 'code', 'Mã quyền duy nhất, dùng trong kiểm tra PreAuthorize.');
SELECT pg_temp.comment_column_if_exists('security', 'permission', 'model', 'Resource hoặc module mà quyền áp dụng.');
SELECT pg_temp.comment_column_if_exists('security', 'permission', 'action', 'Hành động được cấp quyền như tạo, đọc, cập nhật, xóa hoặc duyệt.');
SELECT pg_temp.comment_column_if_exists('security', 'permission', 'description', 'Mô tả ngắn về quyền.');
SELECT pg_temp.comment_constraint_if_exists('security', 'permission', 'ck_permission_action', 'Giới hạn hành động quyền theo enum PermissionAction.');

SELECT pg_temp.comment_table_if_exists('security', 'user_role', 'Bảng nối tài khoản và vai trò.');
SELECT pg_temp.comment_column_if_exists('security', 'user_role', 'user_id', 'Tài khoản được gán vai trò.');
SELECT pg_temp.comment_column_if_exists('security', 'user_role', 'role_id', 'Vai trò được gán cho tài khoản.');
SELECT pg_temp.comment_index_if_exists('security', 'idx_user_role_role', 'Tăng tốc truy vấn danh sách tài khoản theo vai trò.');

SELECT pg_temp.comment_table_if_exists('security', 'role_permission', 'Bảng nối vai trò và quyền.');
SELECT pg_temp.comment_column_if_exists('security', 'role_permission', 'role_id', 'Vai trò được cấu hình quyền.');
SELECT pg_temp.comment_column_if_exists('security', 'role_permission', 'permission_id', 'Quyền thuộc vai trò.');
SELECT pg_temp.comment_index_if_exists('security', 'idx_role_permission_permission', 'Tăng tốc truy vấn vai trò theo quyền.');

SELECT pg_temp.comment_table_if_exists('core', 'person', 'Hồ sơ cá nhân của học viên, huấn luyện viên, nhân viên hoặc người liên quan.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'person_id', 'Khóa chính định danh hồ sơ cá nhân.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'full_name', 'Họ tên đầy đủ đã chuẩn hóa.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'gender', 'Giới tính dạng boolean theo quy ước ứng dụng.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'birth_date', 'Ngày sinh.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'email', 'Email liên hệ.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'national_code', 'Mã định danh cá nhân, duy nhất nếu có.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'face_embedding', 'Vector khuôn mặt 512 chiều phục vụ nhận diện.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'face_image_path', 'Đường dẫn ảnh khuôn mặt đại diện.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'created_at', 'Thời điểm tạo hồ sơ cá nhân.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'updated_at', 'Thời điểm cập nhật hồ sơ cá nhân gần nhất.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'person_code', 'Mã hồ sơ nội bộ, duy nhất trong hệ thống.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'current_belt', 'Cấp đai hiện tại của người học hoặc người tập.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'status', 'Trạng thái hồ sơ cá nhân.');
SELECT pg_temp.comment_column_if_exists('core', 'person', 'start_date', 'Ngày bắt đầu tham gia hoặc làm việc.');
SELECT pg_temp.comment_constraint_if_exists('core', 'person', 'ck_person_current_belt', 'Giới hạn cấp đai hiện tại theo enum Belt.');
SELECT pg_temp.comment_constraint_if_exists('core', 'person', 'ck_person_status', 'Giới hạn trạng thái hồ sơ theo enum PersonStatus.');

SELECT pg_temp.comment_table_if_exists('core', 'user_person', 'Liên kết tài khoản đăng nhập với hồ sơ cá nhân và quan hệ quản lý.');
SELECT pg_temp.comment_column_if_exists('core', 'user_person', 'user_person_id', 'Khóa chính của liên kết tài khoản và hồ sơ cá nhân.');
SELECT pg_temp.comment_column_if_exists('core', 'user_person', 'user_id', 'Tài khoản đăng nhập.');
SELECT pg_temp.comment_column_if_exists('core', 'user_person', 'person_id', 'Hồ sơ cá nhân được liên kết.');
SELECT pg_temp.comment_column_if_exists('core', 'user_person', 'relationship_type', 'Kiểu quan hệ giữa tài khoản và hồ sơ cá nhân.');
SELECT pg_temp.comment_column_if_exists('core', 'user_person', 'active', 'Đánh dấu liên kết còn hiệu lực hay không.');
SELECT pg_temp.comment_column_if_exists('core', 'user_person', 'created_at', 'Thời điểm tạo liên kết.');
SELECT pg_temp.comment_column_if_exists('core', 'user_person', 'updated_at', 'Thời điểm cập nhật liên kết gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('core', 'user_person', 'uk_user_person_relationship', 'Không cho trùng cùng tài khoản, hồ sơ và kiểu quan hệ.');
SELECT pg_temp.comment_constraint_if_exists('core', 'user_person', 'ck_user_person_relationship', 'Giới hạn kiểu quan hệ theo enum RelationshipType.');
SELECT pg_temp.comment_index_if_exists('core', 'idx_user_person_person', 'Tăng tốc truy vấn liên kết theo hồ sơ cá nhân.');

SELECT pg_temp.comment_table_if_exists('security', 'auth_session', 'Phiên đăng nhập và refresh token của tài khoản.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'auth_session_id', 'Khóa chính định danh phiên đăng nhập.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'user_id', 'Tài khoản sở hữu phiên đăng nhập.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'refresh_token_hash', 'Hash của refresh token, duy nhất để tra cứu và thu hồi.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'device_info', 'Thông tin thiết bị đăng nhập.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'platform', 'Nền tảng đăng nhập như web hoặc mobile.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'fcm_token', 'Token FCM để gửi thông báo đẩy.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'active_user_person_id', 'Hồ sơ cá nhân đang được chọn trong phiên.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'expires_at', 'Thời điểm hết hạn phiên.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'revoked', 'Đánh dấu phiên đã bị thu hồi.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'revoked_at', 'Thời điểm thu hồi phiên.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'created_at', 'Thời điểm tạo phiên.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'updated_at', 'Thời điểm cập nhật phiên gần nhất.');
SELECT pg_temp.comment_column_if_exists('security', 'auth_session', 'version', 'Phiên bản phiên dùng cho kiểm soát cập nhật đồng thời.');
SELECT pg_temp.comment_index_if_exists('security', 'idx_auth_session_user', 'Tăng tốc truy vấn phiên theo tài khoản.');

SELECT pg_temp.comment_table_if_exists('core', 'branch', 'Chi nhánh hoặc cơ sở tập luyện.');
SELECT pg_temp.comment_column_if_exists('core', 'branch', 'branch_id', 'Khóa chính tự tăng của chi nhánh.');
SELECT pg_temp.comment_column_if_exists('core', 'branch', 'name', 'Tên chi nhánh.');
SELECT pg_temp.comment_column_if_exists('core', 'branch', 'address', 'Địa chỉ chi nhánh.');
SELECT pg_temp.comment_column_if_exists('core', 'branch', 'hotline', 'Số điện thoại liên hệ của chi nhánh.');
SELECT pg_temp.comment_column_if_exists('core', 'branch', 'opened_date', 'Ngày chi nhánh bắt đầu hoạt động.');
SELECT pg_temp.comment_column_if_exists('core', 'branch', 'status', 'Trạng thái hoạt động của chi nhánh.');
SELECT pg_temp.comment_column_if_exists('core', 'branch', 'created_at', 'Thời điểm tạo chi nhánh.');
SELECT pg_temp.comment_column_if_exists('core', 'branch', 'updated_at', 'Thời điểm cập nhật chi nhánh gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('core', 'branch', 'ck_branch_status', 'Giới hạn trạng thái chi nhánh theo enum BranchStatus.');

SELECT pg_temp.comment_table_if_exists('catalog', 'class_schedule', 'Lịch học cố định theo chi nhánh, thứ, trình độ, địa điểm và khung giờ.');
SELECT pg_temp.comment_column_if_exists('catalog', 'class_schedule', 'schedule_id', 'Khóa chính định danh lịch học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'class_schedule', 'branch_id', 'Chi nhánh tổ chức lịch học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'class_schedule', 'weekday', 'Thứ trong tuần của lịch học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'class_schedule', 'level', 'Trình độ lớp học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'class_schedule', 'location', 'Địa điểm hoặc hình thức học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'class_schedule', 'status', 'Trạng thái lịch học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'class_schedule', 'start_time', 'Giờ bắt đầu lịch học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'class_schedule', 'end_time', 'Giờ kết thúc lịch học.');
SELECT pg_temp.comment_constraint_if_exists('catalog', 'class_schedule', 'ck_schedule_time', 'Đảm bảo giờ kết thúc lớn hơn giờ bắt đầu.');
SELECT pg_temp.comment_constraint_if_exists('catalog', 'class_schedule', 'ck_schedule_weekday', 'Giới hạn thứ trong tuần theo enum Weekday.');
SELECT pg_temp.comment_constraint_if_exists('catalog', 'class_schedule', 'ck_schedule_level', 'Giới hạn trình độ theo enum ScheduleLevel.');
SELECT pg_temp.comment_constraint_if_exists('catalog', 'class_schedule', 'ck_schedule_location', 'Giới hạn địa điểm theo enum ScheduleLocation.');
SELECT pg_temp.comment_constraint_if_exists('catalog', 'class_schedule', 'ck_schedule_status', 'Giới hạn trạng thái lịch học theo enum ScheduleStatus.');

SELECT pg_temp.comment_table_if_exists('catalog', 'course', 'Khóa học mở theo một lịch học cố định.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course', 'course_id', 'Khóa chính định danh khóa học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course', 'schedule_id', 'Lịch học gốc của khóa học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course', 'capacity', 'Sức chứa tối đa của khóa học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course', 'status', 'Trạng thái vận hành của khóa học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course', 'created_at', 'Thời điểm tạo khóa học.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course', 'updated_at', 'Thời điểm cập nhật khóa học gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('catalog', 'course', 'ck_course_status', 'Giới hạn trạng thái khóa học theo enum CourseStatus.');

SELECT pg_temp.comment_table_if_exists('catalog', 'course_price', 'Gói giá của khóa học theo thời lượng và số buổi.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course_price', 'course_price_id', 'Khóa chính định danh gói giá.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course_price', 'course_id', 'Khóa học áp dụng gói giá.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course_price', 'duration_months', 'Số tháng hiệu lực của gói.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course_price', 'session_count', 'Số buổi học trong gói.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course_price', 'base_price', 'Giá gốc trước điều chỉnh.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course_price', 'final_price', 'Giá cuối cùng dùng để thu tiền.');
SELECT pg_temp.comment_column_if_exists('catalog', 'course_price', 'status', 'Trạng thái gói giá.');
SELECT pg_temp.comment_constraint_if_exists('catalog', 'course_price', 'ck_course_price_status', 'Giới hạn trạng thái gói giá theo enum CoursePriceStatus.');

SELECT pg_temp.comment_table_if_exists('finance', 'wallet', 'Ví tiền của một hồ sơ cá nhân.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet', 'wallet_id', 'Khóa chính định danh ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet', 'person_id', 'Hồ sơ cá nhân sở hữu ví, duy nhất mỗi người một ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet', 'balance', 'Số dư hiện tại của ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet', 'status', 'Trạng thái ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet', 'created_at', 'Thời điểm tạo ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet', 'updated_at', 'Thời điểm cập nhật ví gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('finance', 'wallet', 'ck_wallet_status', 'Giới hạn trạng thái ví theo enum WalletStatus.');

SELECT pg_temp.comment_table_if_exists('finance', 'wallet_transaction', 'Giao dịch làm thay đổi hoặc ghi nhận biến động ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'wallet_transaction_id', 'Khóa chính định danh giao dịch ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'wallet_id', 'Ví liên quan tới giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'type', 'Loại giao dịch ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'direction', 'Chiều tiền vào hoặc ra khỏi ví.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'amount', 'Số tiền giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'balance_before', 'Số dư ví trước giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'balance_after', 'Số dư ví sau giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'external_reference', 'Mã tham chiếu nghiệp vụ hoặc hệ thống bên ngoài.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'created_by_user_id', 'Tài khoản tạo giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'reviewed_by_user_id', 'Tài khoản rà soát hoặc duyệt giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'reviewed_at', 'Thời điểm rà soát hoặc duyệt giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'note', 'Ghi chú giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'created_at', 'Thời điểm tạo giao dịch.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'updated_at', 'Thời điểm cập nhật giao dịch gần nhất.');
SELECT pg_temp.comment_column_if_exists('finance', 'wallet_transaction', 'status', 'Trạng thái xử lý giao dịch.');
SELECT pg_temp.comment_constraint_if_exists('finance', 'wallet_transaction', 'ck_wallet_tx_type', 'Giới hạn loại giao dịch theo enum WalletTransactionType.');
SELECT pg_temp.comment_constraint_if_exists('finance', 'wallet_transaction', 'ck_wallet_tx_direction', 'Giới hạn chiều giao dịch theo enum WalletTransactionDirection.');
SELECT pg_temp.comment_constraint_if_exists('finance', 'wallet_transaction', 'ck_wallet_tx_status', 'Giới hạn trạng thái giao dịch theo enum WalletTransactionStatus.');
SELECT pg_temp.comment_constraint_if_exists('finance', 'wallet_transaction', 'uk_wallet_tx_type_reference', 'Không cho trùng loại giao dịch và mã tham chiếu ngoài.');
SELECT pg_temp.comment_index_if_exists('finance', 'idx_wallet_tx_wallet_created', 'Tăng tốc xem lịch sử giao dịch của ví theo thời gian tạo giảm dần.');

SELECT pg_temp.comment_table_if_exists('finance', 'course_purchase', 'Giao dịch mua gói khóa học của học viên.');
SELECT pg_temp.comment_column_if_exists('finance', 'course_purchase', 'course_purchase_id', 'Khóa chính định danh lượt mua khóa học.');
SELECT pg_temp.comment_column_if_exists('finance', 'course_purchase', 'student_person_id', 'Học viên mua khóa học.');
SELECT pg_temp.comment_column_if_exists('finance', 'course_purchase', 'course_price_id', 'Gói giá được mua.');
SELECT pg_temp.comment_column_if_exists('finance', 'course_purchase', 'debit_transaction_id', 'Giao dịch trừ tiền ví tương ứng, duy nhất cho lượt mua.');
SELECT pg_temp.comment_index_if_exists('finance', 'idx_course_purchase_price', 'Tăng tốc thống kê lượt mua theo gói giá.');

SELECT pg_temp.comment_table_if_exists('training', 'student_enrollment', 'Ghi danh học viên vào lịch học sau khi mua khóa.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'student_enrollment_id', 'Khóa chính định danh ghi danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'student_person_id', 'Học viên được ghi danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'course_purchase_id', 'Lượt mua khóa học tạo ra ghi danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'class_schedule_id', 'Lịch học mà học viên tham gia.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'start_date', 'Ngày bắt đầu hiệu lực ghi danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'end_date', 'Ngày kết thúc hiệu lực ghi danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'status', 'Trạng thái ghi danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'created_at', 'Thời điểm tạo ghi danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_enrollment', 'updated_at', 'Thời điểm cập nhật ghi danh gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('training', 'student_enrollment', 'ck_enrollment_status', 'Giới hạn trạng thái ghi danh theo enum StudentEnrollmentStatus.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_enrollment_schedule', 'Tăng tốc truy vấn học viên theo lịch học.');

SELECT pg_temp.comment_table_if_exists('training', 'class_session', 'Buổi học thực tế được tạo từ khóa học.');
SELECT pg_temp.comment_column_if_exists('training', 'class_session', 'class_session_id', 'Khóa chính định danh buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'class_session', 'course_id', 'Khóa học chứa buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'class_session', 'session_date', 'Ngày diễn ra buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'class_session', 'status', 'Trạng thái buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'class_session', 'is_attendance_closed', 'Đánh dấu đã khóa điểm danh hay chưa.');
SELECT pg_temp.comment_column_if_exists('training', 'class_session', 'start_time', 'Giờ bắt đầu buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'class_session', 'end_time', 'Giờ kết thúc buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'class_session', 'note', 'Ghi chú buổi học.');
SELECT pg_temp.comment_constraint_if_exists('training', 'class_session', 'ck_class_session_status', 'Giới hạn trạng thái buổi học theo enum SessionStatus.');

SELECT pg_temp.comment_table_if_exists('training', 'student_attendance', 'Bản ghi điểm danh và đánh giá học viên trong một buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'student_attendance_id', 'Khóa chính định danh điểm danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'class_session_id', 'Buổi học được điểm danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'student_enrollment_id', 'Ghi danh của học viên được điểm danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'check_in_time', 'Thời điểm học viên check in.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'attendance_status', 'Trạng thái điểm danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'evaluation_status', 'Đánh giá của huấn luyện viên cho buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'coach_assignment_id', 'Phân công huấn luyện viên phụ trách nếu có.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'note', 'Ghi chú điểm danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'created_at', 'Thời điểm tạo điểm danh.');
SELECT pg_temp.comment_column_if_exists('training', 'student_attendance', 'updated_at', 'Thời điểm cập nhật điểm danh gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('training', 'student_attendance', 'uk_attendance_session_enrollment', 'Mỗi ghi danh chỉ có một bản ghi điểm danh trong một buổi học.');
SELECT pg_temp.comment_constraint_if_exists('training', 'student_attendance', 'ck_attendance_status', 'Giới hạn trạng thái điểm danh theo enum AttendanceStatus.');
SELECT pg_temp.comment_constraint_if_exists('training', 'student_attendance', 'ck_evaluation_status', 'Giới hạn đánh giá buổi học theo enum EvaluationStatus hoặc để trống.');
SELECT pg_temp.comment_constraint_if_exists('training', 'student_attendance', 'fk_student_attendance_coach_assignment', 'Liên kết điểm danh với phân công huấn luyện viên phụ trách.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_attendance_enrollment', 'Tăng tốc truy vấn lịch sử điểm danh theo ghi danh.');

SELECT pg_temp.comment_table_if_exists('training', 'coach_assignment', 'Phân công huấn luyện viên phụ trách khóa học.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'coach_assignment_id', 'Khóa chính định danh phân công.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'coach_person_id', 'Hồ sơ cá nhân của huấn luyện viên.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'course_id', 'Khóa học được phân công.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'assigned_date', 'Ngày bắt đầu phân công.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'end_date', 'Ngày kết thúc phân công nếu có.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'coach_assignment_status', 'Trạng thái phân công huấn luyện viên.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'note', 'Ghi chú phân công.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'created_at', 'Thời điểm tạo phân công.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_assignment', 'updated_at', 'Thời điểm cập nhật phân công gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('training', 'coach_assignment', 'ck_coach_assignment_status', 'Giới hạn trạng thái phân công theo enum CoachAssignmentStatus.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_coach_assignment_course', 'Tăng tốc truy vấn phân công theo khóa học.');

SELECT pg_temp.comment_table_if_exists('training', 'coach_timesheet', 'Bảng công check in và check out của huấn luyện viên theo buổi học.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_timesheet', 'coach_timesheet_id', 'Khóa chính định danh bảng công huấn luyện viên.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_timesheet', 'coach_assignment_id', 'Phân công huấn luyện viên liên quan.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_timesheet', 'class_session_id', 'Buổi học được ghi nhận công.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_timesheet', 'check_in_time', 'Giờ huấn luyện viên check in.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_timesheet', 'check_out_time', 'Giờ huấn luyện viên check out.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_timesheet', 'note', 'Ghi chú bảng công.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_timesheet', 'created_at', 'Thời điểm tạo bảng công.');
SELECT pg_temp.comment_column_if_exists('training', 'coach_timesheet', 'updated_at', 'Thời điểm cập nhật bảng công gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('training', 'coach_timesheet', 'uk_timesheet_assignment_session', 'Mỗi phân công chỉ có một bảng công trong một buổi học.');

SELECT pg_temp.comment_table_if_exists('training', 'belt_exam', 'Kỳ thi đai của học viên hoặc người tập.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'belt_exam_id', 'Khóa chính định danh kỳ thi đai.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'person_id', 'Người tham gia kỳ thi đai.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'from_belt', 'Cấp đai trước khi thi.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'target_belt', 'Cấp đai mục tiêu sau khi thi.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'year', 'Năm tổ chức hoặc ghi nhận kỳ thi.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'quarter', 'Quý tổ chức hoặc ghi nhận kỳ thi.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'exam_date', 'Ngày thi thực tế, bắt buộc khi kết quả không còn chờ xử lý.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'result', 'Kết quả kỳ thi đai.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'note', 'Ghi chú kỳ thi đai.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'created_by_user_id', 'Tài khoản tạo bản ghi kỳ thi đai.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'created_at', 'Thời điểm tạo bản ghi kỳ thi đai.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'updated_at', 'Thời điểm cập nhật kỳ thi đai gần nhất.');
SELECT pg_temp.comment_column_if_exists('training', 'belt_exam', 'type', 'Loại kỳ thi đai như thi lên cấp hoặc thi thử.');
SELECT pg_temp.comment_constraint_if_exists('training', 'belt_exam', 'ck_belt_exam_from_belt', 'Giới hạn cấp đai xuất phát theo enum Belt.');
SELECT pg_temp.comment_constraint_if_exists('training', 'belt_exam', 'ck_belt_exam_target_belt', 'Giới hạn cấp đai mục tiêu theo enum Belt.');
SELECT pg_temp.comment_constraint_if_exists('training', 'belt_exam', 'ck_belt_exam_result', 'Giới hạn kết quả thi đai theo enum BeltExamResult.');
SELECT pg_temp.comment_constraint_if_exists('training', 'belt_exam', 'ck_belt_exam_type', 'Giới hạn loại kỳ thi đai theo enum BeltExamType.');
SELECT pg_temp.comment_constraint_if_exists('training', 'belt_exam', 'chk_belt_exam_business_rule', 'Đảm bảo năm và quý hợp lệ, đai xuất phát khác đai mục tiêu, và có ngày thi khi đã có kết quả.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_belt_exam_person_period', 'Tăng tốc tra cứu kỳ thi đai theo người, năm và quý.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_belt_exam_type_result', 'Tăng tốc lọc kỳ thi đai theo loại và kết quả.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_belt_exam_exam_date', 'Tăng tốc lọc kỳ thi đai theo ngày thi.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_belt_exam_created_by', 'Tăng tốc tra cứu kỳ thi đai theo tài khoản tạo.');

SELECT pg_temp.comment_table_if_exists('training', 'leave_request', 'Đơn xin nghỉ hoặc xin học bù của học viên, nhân viên hệ thống.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'leave_request_id', 'Khóa chính định danh đơn xin nghỉ.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'person_id', 'Người xin nghỉ hoặc được tạo đơn nghỉ.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'requester_type', 'Loại người yêu cầu như học viên hoặc nhân viên hệ thống.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'leave_date', 'Ngày nghỉ áp dụng cho nhân viên hệ thống.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'leave_class_session_id', 'Buổi học xin nghỉ của học viên.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'makeup_class_session_id', 'Buổi học bù mong muốn của học viên.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'leave_context', 'Nội dung hoặc lý do xin nghỉ.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'status', 'Trạng thái xử lý đơn xin nghỉ.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'created_by_user_id', 'Tài khoản tạo đơn xin nghỉ.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'reviewed_by_user_id', 'Tài khoản duyệt hoặc từ chối đơn xin nghỉ.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'reviewed_at', 'Thời điểm duyệt hoặc từ chối đơn.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'review_note', 'Ghi chú duyệt hoặc lý do từ chối.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'created_at', 'Thời điểm tạo đơn xin nghỉ.');
SELECT pg_temp.comment_column_if_exists('training', 'leave_request', 'updated_at', 'Thời điểm cập nhật đơn xin nghỉ gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('training', 'leave_request', 'ck_leave_request_requester_type', 'Giới hạn loại người yêu cầu theo enum RequesterType.');
SELECT pg_temp.comment_constraint_if_exists('training', 'leave_request', 'ck_leave_request_status', 'Giới hạn trạng thái đơn xin nghỉ theo enum LeaveRequestStatus.');
SELECT pg_temp.comment_constraint_if_exists('training', 'leave_request', 'chk_leave_request_business_rule', 'Đảm bảo học viên có buổi nghỉ và buổi học bù, nhân viên có ngày nghỉ, buổi nghỉ khác buổi học bù, và dữ liệu duyệt phù hợp trạng thái.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_leave_request_person_status', 'Tăng tốc tra cứu đơn xin nghỉ theo người và trạng thái.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_leave_request_type_status', 'Tăng tốc lọc đơn xin nghỉ theo loại người yêu cầu và trạng thái.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_leave_request_leave_date', 'Tăng tốc lọc đơn xin nghỉ theo ngày nghỉ.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_leave_request_leave_session', 'Tăng tốc tra cứu đơn xin nghỉ theo buổi nghỉ.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_leave_request_makeup_session', 'Tăng tốc tra cứu đơn xin nghỉ theo buổi học bù.');
SELECT pg_temp.comment_index_if_exists('training', 'idx_leave_request_created_by', 'Tăng tốc tra cứu đơn xin nghỉ theo tài khoản tạo.');

SELECT pg_temp.comment_table_if_exists('notification', 'notification', 'Thông báo được tạo trong hệ thống.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification', 'notification_id', 'Khóa chính định danh thông báo.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification', 'title', 'Tiêu đề thông báo.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification', 'body', 'Nội dung thông báo.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification', 'notification_type', 'Loại thông báo.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification', 'reference_type', 'Loại đối tượng nghiệp vụ được tham chiếu nếu có.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification', 'reference_id', 'Mã đối tượng nghiệp vụ được tham chiếu nếu có.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification', 'payload', 'Dữ liệu mở rộng của thông báo dạng text hoặc JSON.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification', 'created_at', 'Thời điểm tạo thông báo.');
SELECT pg_temp.comment_constraint_if_exists('notification', 'notification', 'ck_notification_type', 'Giới hạn loại thông báo theo enum NotificationType.');

SELECT pg_temp.comment_table_if_exists('notification', 'notification_recipient', 'Trạng thái nhận thông báo của từng tài khoản.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'notification_recipient_id', 'Khóa chính định danh người nhận thông báo.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'notification_id', 'Thông báo được gửi.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'recipient_user_id', 'Tài khoản nhận thông báo.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'read', 'Đánh dấu người nhận đã đọc thông báo.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'read_at', 'Thời điểm đọc thông báo.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'delivered_at', 'Thời điểm thông báo được giao thành công.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'notification_recipient_status', 'Trạng thái gửi hoặc lưu trữ thông báo cho người nhận.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'created_at', 'Thời điểm tạo bản ghi người nhận.');
SELECT pg_temp.comment_column_if_exists('notification', 'notification_recipient', 'updated_at', 'Thời điểm cập nhật người nhận gần nhất.');
SELECT pg_temp.comment_constraint_if_exists('notification', 'notification_recipient', 'uk_notification_recipient', 'Mỗi tài khoản chỉ có một bản ghi nhận cho một thông báo.');
SELECT pg_temp.comment_constraint_if_exists('notification', 'notification_recipient', 'ck_notification_recipient_status', 'Giới hạn trạng thái người nhận theo enum NotificationRecipientStatus.');
SELECT pg_temp.comment_index_if_exists('notification', 'idx_notification_recipient_user', 'Tăng tốc truy vấn thông báo theo người nhận.');

SELECT pg_temp.comment_table_if_exists('skill', 'fitness', 'Danh mục bài tập hoặc chỉ tiêu thể lực theo trình độ.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness', 'fitness_id', 'Khóa chính tự tăng của bài tập thể lực.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness', 'schedule_level', 'Trình độ áp dụng bài tập.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness', 'amount', 'Số lượng yêu cầu của bài tập.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness', 'duration', 'Thời lượng yêu cầu của bài tập.');
SELECT pg_temp.comment_constraint_if_exists('skill', 'fitness', 'ck_fitness_schedule_level', 'Giới hạn trình độ bài tập theo enum ScheduleLevel.');

SELECT pg_temp.comment_table_if_exists('skill', 'fitness_record', 'Lịch sử ghi nhận kết quả thể lực của học viên.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness_record', 'fitness_record_id', 'Khóa chính tự tăng của kết quả thể lực.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness_record', 'record_date', 'Ngày ghi nhận kết quả.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness_record', 'student_person_id', 'Học viên được ghi nhận kết quả.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness_record', 'duration', 'Thời lượng thực hiện được ghi nhận.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness_record', 'fitness_id', 'Bài tập thể lực được ghi nhận.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness_record', 'recorded_by_coach_id', 'Huấn luyện viên ghi nhận kết quả.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness_record', 'created_at', 'Thời điểm tạo kết quả thể lực.');
SELECT pg_temp.comment_column_if_exists('skill', 'fitness_record', 'updated_at', 'Thời điểm cập nhật kết quả thể lực gần nhất.');
