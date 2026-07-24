package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.domain.Core.ClassSchedule;
import com.dat.ai_receptionist_web.domain.Operation.ClassSession;
import com.dat.ai_receptionist_web.domain.Operation.StudentEnrollment;
import com.dat.ai_receptionist_web.dto.Operation.CheckInStudentProjection;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.enums.Operation.SessionStatus;
import com.dat.ai_receptionist_web.enums.Operation.StudentEnrollmentStatus;
import com.dat.ai_receptionist_web.mapper.Operation.StudentAttendanceMapper;
import com.dat.ai_receptionist_web.repository.Core.CoachRepository;
import com.dat.ai_receptionist_web.repository.Core.StudentRepository;
import com.dat.ai_receptionist_web.repository.Operation.ClassSessionRepository;
import com.dat.ai_receptionist_web.repository.Operation.StudentAttendanceRepository;
import com.dat.ai_receptionist_web.repository.Operation.StudentEnrollmentRepository;
import com.dat.ai_receptionist_web.service.Core.CoachService;
import com.dat.ai_receptionist_web.service.Core.StudentService;
import com.dat.ai_receptionist_web.service.Operation.*;
import com.dat.ai_receptionist_web.service.Security.AuthTokenService;
import com.dat.ai_receptionist_web.util.error.AppException;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentAttendanceServiceDuplicateTest {

    @Test
    void concurrentDuplicateCheckInReturnsConflictErrorCode() {
        StudentAttendanceRepository attendanceRepository = mock(StudentAttendanceRepository.class);
        ClassSessionRepository classSessionRepository = mock(ClassSessionRepository.class);
        StudentRepository studentRepository = mock(StudentRepository.class);
        StudentEnrollmentRepository enrollmentRepository = mock(StudentEnrollmentRepository.class);

        StudentAttendanceService service = new StudentAttendanceService(
                attendanceRepository,
                mock(CoachService.class),
                mock(StudentAttendanceMapper.class),
                mock(StudentEnrollmentService.class),
                mock(NotificationService.class),
                mock(AuthTokenService.class),
                mock(StudentService.class),
                mock(CoachRepository.class),
                classSessionRepository,
                studentRepository,
                enrollmentRepository,
                mock(ApplicationEventPublisher.class),
                mock(AttendanceNotificationTaskExecutor.class),
                mock(AttendanceNotificationDispatcher.class)
        );

        String studentCode = "S001";
        UUID enrollmentId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        ClassSchedule schedule = ClassSchedule.builder()
                .scheduleId("A001")
                .startTime(LocalTime.now().plusHours(1))
                .build();
        StudentEnrollment enrollment = StudentEnrollment.builder()
                .enrollmentId(enrollmentId)
                .classSchedule(schedule)
                .build();
        ClassSession classSession = ClassSession.builder()
                .sessionId(sessionId)
                .classSchedule(schedule)
                .sessionDate(LocalDate.now())
                .status(SessionStatus.ACTIVE)
                .build();

        when(studentRepository.findCheckInStudentByStudentCode(studentCode))
                .thenReturn(Optional.of(new TestCheckInStudentProjection(studentCode)));
        when(enrollmentRepository.findByStudent_StudentCodeAndStatusWithClassSchedule(
                studentCode,
                StudentEnrollmentStatus.ACTIVE
        )).thenReturn(List.of(enrollment));
        when(classSessionRepository.findBySessionDateAndStatusAndClassSchedule_ScheduleIdIn(
                any(),
                org.mockito.ArgumentMatchers.eq(SessionStatus.ACTIVE),
                org.mockito.ArgumentMatchers.eq(List.of("A001"))
        )).thenReturn(List.of(classSession));
        when(attendanceRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(
                        "duplicate constraint uk_student_attendance_enrollment_session"
                ));

        AppException exception = assertThrows(
                AppException.class,
                () -> service.createAttendanceRecord(new StudentAttendanceDTO.CreateRequest(studentCode))
        );

        assertEquals(ErrorCode.ATTENDANCE_ALREADY_EXISTS, exception.getErrorCode());
    }

    private record TestCheckInStudentProjection(String studentCode) implements CheckInStudentProjection {
        @Override
        public UUID getPersonId() {
            return UUID.randomUUID();
        }

        @Override
        public String getStudentCode() {
            return studentCode;
        }

        @Override
        public StudentStatus getStudentStatus() {
            return StudentStatus.ACTIVE;
        }

        @Override
        public String getFullName() {
            return "Test Student";
        }
    }
}
