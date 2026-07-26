package com.dat.ai_receptionist_web.service.Core;

import com.dat.ai_receptionist_web.client.PythonBackendClient;
import com.dat.ai_receptionist_web.client.PythonBackendClient.PythonBackendClientException;
import com.dat.ai_receptionist_web.dto.Core.PersonDTO;
import com.dat.ai_receptionist_web.dto.Operation.CheckInStudentProjection;
import com.dat.ai_receptionist_web.dto.PageResponse;
import com.dat.ai_receptionist_web.enums.ErrorCode;
import com.dat.ai_receptionist_web.enums.Core.CoachStatus;
import com.dat.ai_receptionist_web.enums.Core.StudentStatus;
import com.dat.ai_receptionist_web.mapper.Core.PersonMapper;
import com.dat.ai_receptionist_web.repository.Core.PersonRepository;
import com.dat.ai_receptionist_web.service.Operation.CoachTimesheetService;
import com.dat.ai_receptionist_web.service.Operation.StudentAttendanceService;
import com.dat.ai_receptionist_web.util.error.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class PersonService {

    private final PersonRepository personRepository;
    private final PersonMapper personMapper;
    private final PythonBackendClient pythonBackendClient;
    private final StudentAttendanceService studentAttendanceService;
    private final CoachTimesheetService coachTimesheetService;

    @Transactional(readOnly = true)
    public PageResponse<PersonDTO.SearchItem> searchStudentsAndCoaches(String search, Pageable pageable) {
        String normalizedSearch = search == null || search.trim().isEmpty()
                ? null
                : "%" + search.trim().toLowerCase() + "%";
        Page<PersonRepository.PersonSearchProjection> people =
                personRepository.searchStudentsAndCoaches(normalizedSearch, pageable);
        return PageResponse.of(people, personMapper::toSearchItem);
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FACE_IMAGE_INVALID);
        }

        String contentType = file.getContentType();

        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.FACE_IMAGE_INVALID);
        }

        long maxSize = 10 * 1024 * 1024L;

        if (file.getSize() > maxSize) {
            throw new AppException(ErrorCode.FACE_IMAGE_INVALID);
        }
    }

    public PersonDTO.FaceCheckInResponse checkInByFaceImage(MultipartFile file) {
        validateImage(file);

        try {
            var response = pythonBackendClient.checkInByFaceImage(file);
            if (!response.matched()) {
                throw new AppException(ErrorCode.FACE_NOT_RECOGNIZED);
            }
            if (response.personId() == null) {
                throw new AppException(ErrorCode.PYTHON_BACKEND_ERROR);
            }
            PersonRepository.FaceCheckInSubjectProjection subject = personRepository
                    .findFaceCheckInSubjectByPersonId(response.personId())
                    .orElseThrow(() -> new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID));

            boolean isStudent = subject.getStudentCode() != null;
            boolean isCoach = subject.getStaffCode() != null;
            if (isStudent == isCoach) {
                throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID);
            }

            if (isStudent) {
                return studentAttendanceService.createAttendanceRecordForResolvedStudent(
                        new ResolvedStudentCheckIn(subject)
                );
            }
            return coachTimesheetService.checkInResolvedCoach(
                    subject.getPersonId(),
                    toCoachStatus(subject.getCoachStatus())
            );
        } catch (PythonBackendClientException exception) {
            ErrorCode errorCode = exception.getFailureType().isUnavailable()
                    ? ErrorCode.PYTHON_BACKEND_UNAVAILABLE
                    : ErrorCode.PYTHON_BACKEND_ERROR;
            throw new AppException(errorCode, exception);
        }
    }

    private static StudentStatus toStudentStatus(String status) {
        try {
            return StudentStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new AppException(ErrorCode.FACE_CHECK_IN_PERSON_TYPE_INVALID, exception);
        }
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
            return toStudentStatus(subject.getStudentStatus());
        }

        @Override
        public String getFullName() {
            return subject.getFullName();
        }
    }
}
