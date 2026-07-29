package com.dat.ai_receptionist_web.service.Operation;

import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.Core.CoachResDTO;
import com.dat.ai_receptionist_web.dto.Core.StudentResDTO;
import com.dat.ai_receptionist_web.dto.Operation.CheckInStudentProjection;
import com.dat.ai_receptionist_web.dto.Operation.CoachTimesheetDTO;
import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;
import com.dat.ai_receptionist_web.enums.Core.CoachStatus;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.service.Core.CoachService;
import com.dat.ai_receptionist_web.service.Core.PersonService;
import com.dat.ai_receptionist_web.service.Core.StudentService;
import com.dat.ai_receptionist_web.util.error.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** Coordinates face matching with the student/coach operational check-in workflows. */
@Service
@RequiredArgsConstructor
@Slf4j
public class FaceCheckInService {

    private final PersonService personService;
    private final PersonRepository personRepository;
    private final StudentService studentService;
    private final CoachService coachService;
    private final StudentAttendanceService studentAttendanceService;
    private final CoachTimesheetService coachTimesheetService;

    public PersonDTO.FaceCheckInResult checkInByFaceImage(MultipartFile file) {
        var embeddingResponse = personService.generateFaceEmbedding(file);
        PersonService.NearestPersonMatch nearestPerson = personService
                .findNearestPersonByEmbedding(embeddingResponse.embedding());
        if (nearestPerson.personId() == null) {
            throw new AppException(ErrorCode.FACE_NOT_MATCHED);
        }

        PersonRepository.FaceCheckInSubjectProjection subject = personRepository
                .findFaceCheckInSubjectByPersonId(nearestPerson.personId())
                .orElseThrow(() -> new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID));

        boolean isStudent = subject.getStudentCode() != null;
        boolean isCoach = subject.getStaffCode() != null;
        if (isStudent == isCoach) {
            throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID);
        }

        if (isStudent) {
            StudentResDTO.StudentDetail studentDetail = studentService.getStudentDetail(subject.getPersonId());
            try {
                StudentAttendanceDTO.Response attendance = studentAttendanceService
                        .createAttendanceRecordForResolvedStudent(new ResolvedStudentCheckIn(subject));
                return PersonDTO.FaceCheckInResult.builder()
                        .personType("STUDENT")
                        .checkInSuccess(true)
                        .studentDetail(studentDetail)
                        .studentAttendance(attendance)
                        .build();
            } catch (Exception exception) {
                return studentCheckInFailure(studentDetail, subject.getPersonId(), exception);
            }
        }

        CoachResDTO.CoachDetail coachDetail = coachService.getCoachDetail(subject.getPersonId());
        try {
            CoachTimesheetDTO.Response timesheet = coachTimesheetService.checkInResolvedCoach(
                    subject.getPersonId(),
                    toCoachStatus(subject.getCoachStatus()),
                    subject.getFullName(),
                    subject.getStaffCode()
            );
            return PersonDTO.FaceCheckInResult.builder()
                    .personType("COACH")
                    .checkInSuccess(true)
                    .coachDetail(coachDetail)
                    .coachTimesheet(timesheet)
                    .build();
        } catch (Exception exception) {
            return coachCheckInFailure(coachDetail, subject.getPersonId(), exception);
        }
    }

    private PersonDTO.FaceCheckInResult studentCheckInFailure(
            StudentResDTO.StudentDetail studentDetail,
            java.util.UUID personId,
            Exception exception
    ) {
        ErrorCode errorCode = resolveCheckInErrorCode(personId, exception);
        return PersonDTO.FaceCheckInResult.builder()
                .personType("STUDENT")
                .checkInSuccess(false)
                .checkInErrorCode(errorCode.name())
                .checkInErrorMessage(errorCode.getMessage())
                .studentDetail(studentDetail)
                .build();
    }

    private PersonDTO.FaceCheckInResult coachCheckInFailure(
            CoachResDTO.CoachDetail coachDetail,
            java.util.UUID personId,
            Exception exception
    ) {
        ErrorCode errorCode = resolveCheckInErrorCode(personId, exception);
        return PersonDTO.FaceCheckInResult.builder()
                .personType("COACH")
                .checkInSuccess(false)
                .checkInErrorCode(errorCode.name())
                .checkInErrorMessage(errorCode.getMessage())
                .coachDetail(coachDetail)
                .build();
    }

    private ErrorCode resolveCheckInErrorCode(java.util.UUID personId, Exception exception) {
        if (exception instanceof AppException appException) {
            return appException.getErrorCode();
        }
        log.error("Unexpected face check-in failure after person identification. personId={}, exceptionType={}",
                personId, exception.getClass().getName(), exception);
        return ErrorCode.UNCATEGORIZED_EXCEPTION;
    }

    private static CoachStatus toCoachStatus(String status) {
        try {
            return CoachStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID, exception);
        }
    }

    private record ResolvedStudentCheckIn(PersonRepository.FaceCheckInSubjectProjection subject)
            implements CheckInStudentProjection {

        @Override
        public java.util.UUID getPersonId() {
            return subject.getPersonId();
        }

        @Override
        public String getStudentCode() {
            return subject.getStudentCode();
        }

        @Override
        public StudentStatus getStudentStatus() {
            try {
                return StudentStatus.valueOf(subject.getStudentStatus());
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID, exception);
            }
        }

        @Override
        public String getFullName() {
            return subject.getFullName();
        }
    }
}
