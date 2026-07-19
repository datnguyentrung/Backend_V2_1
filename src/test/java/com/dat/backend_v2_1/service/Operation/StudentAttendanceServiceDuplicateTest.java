package com.dat.backend_v2_1.service.Operation;

import com.dat.backend_v2_1.domain.Core.ClassSchedule;
import com.dat.backend_v2_1.domain.Operation.ClassSession;
import com.dat.backend_v2_1.domain.Operation.StudentEnrollment;
import com.dat.backend_v2_1.dto.Operation.CheckInStudentProjection;
import com.dat.backend_v2_1.dto.Operation.StudentAttendanceDTO;
import com.dat.backend_v2_1.enums.Core.StudentStatus;
import com.dat.backend_v2_1.enums.Operation.SessionStatus;
import com.dat.backend_v2_1.enums.Operation.StudentEnrollmentStatus;
import com.dat.backend_v2_1.mapper.Operation.StudentAttendanceMapper;
import com.dat.backend_v2_1.repository.Core.CoachRepository;
import com.dat.backend_v2_1.repository.Core.StudentRepository;
import com.dat.backend_v2_1.repository.Operation.ClassSessionRepository;
import com.dat.backend_v2_1.repository.Operation.StudentAttendanceRepository;
import com.dat.backend_v2_1.repository.Operation.StudentEnrollmentRepository;
import com.dat.backend_v2_1.service.Core.CoachService;
import com.dat.backend_v2_1.service.Core.StudentService;
import com.dat.backend_v2_1.service.Security.AuthTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentAttendanceServiceDuplicateTest {

    @Test
    void concurrentDuplicateCheckInReturnsNullForConflictPath() {
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
        when(attendanceRepository.existsByStudentEnrollment_EnrollmentIdAndClassSession_SessionId(enrollmentId, sessionId))
                .thenReturn(false);
        when(attendanceRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        StudentAttendanceDTO.Response response =
                service.createAttendanceRecord(new StudentAttendanceDTO.CreateRequest(studentCode));

        assertNull(response);
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
