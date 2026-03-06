# 📋 Backend Service Return Type Conventions

## 🎯 Nguyên tắc chung

### **TẠI SAO CẦN QUY CHUẨN?**
1. ✅ **Nhất quán** - Dễ maintain và review code
2. ✅ **RESTful Best Practices** - Tuân thủ chuẩn HTTP
3. ✅ **Frontend-friendly** - FE biết chắc sẽ nhận gì
4. ✅ **Debugging dễ dàng** - Log rõ ràng, trace được data flow

---

## 📊 Quy tắc Return Type theo Operation

| Operation | HTTP Method | Return Type | Lý do |
|-----------|-------------|-------------|-------|
| **CREATE** | POST | `Response DTO` hoặc `ID/Code (String)` | FE cần data mới tạo để hiển thị/redirect |
| **UPDATE** | PUT/PATCH | `Response DTO` hoặc `void` | Tùy nghiệp vụ: Nếu FE cần data mới → DTO, chỉ confirm thành công → void |
| **DELETE** | DELETE | `void` | DELETE thành công = HTTP 204 No Content |
| **GET (Single)** | GET | `Response DTO` (never null) | Không tìm thấy → throw Exception |
| **GET (List)** | GET | `List<Response DTO>` hoặc `Page<>` | Empty list OK, không throw exception |
| **BATCH Operations** | POST/PATCH | `void` hoặc `BatchResult DTO` | Batch đơn giản → void, phức tạp (partial success) → DTO |

---

## 🔥 Chi tiết từng Operation

### 1️⃣ **CREATE (POST)** ✅

#### ❌ **KHÔNG NÊN:**
```java
public void createStudent(CreateRequest request) {
    // Save vào DB
    studentRepository.save(student);
    // Không return gì → FE không biết tạo thành công hay không, không có ID mới
}
```

#### ✅ **NÊN LÀM (Option 1 - Return ID/Code):**
```java
public String createStudent(StudentReqDTO.StudentCreate createDTO) {
    Student newStudent = // ... mapping logic
    studentRepository.save(newStudent);
    
    log.info("Created student successfully with code: {}", newStudent.getStudentCode());
    return newStudent.getStudentCode(); // Hoặc return UUID
}
```
**Ưu điểm:**
- Nhẹ nhàng, chỉ trả ID
- Phù hợp khi FE chỉ cần ID để redirect hoặc query lại sau

#### ✅ **NÊN LÀM (Option 2 - Return Full DTO):**
```java
public StudentAttendanceDTO.Response createAttendanceRecord(
        StudentAttendanceDTO.ManualLogRequest request, 
        String coachId) {
    
    // Validate
    Student student = studentService.getStudentById(request.getStudentId());
    Coach coach = coachService.getCoachById(coachId);
    ClassSchedule schedule = classScheduleService.getClassScheduleById(request.getClassScheduleId());
    
    // Create entity
    StudentAttendance attendance = StudentAttendance.builder()
            .student(student)
            .coach(coach)
            .classSchedule(schedule)
            .attendanceStatus(request.getAttendanceStatus())
            .checkInTime(request.getCheckInTime())
            .note(request.getNote())
            .build();
    
    StudentAttendance saved = attendanceRepository.save(attendance);
    
    log.info("Created attendance record for student {}, class {}", 
             student.getFullName(), schedule.getScheduleId());
    
    // Return DTO để FE hiển thị ngay
    return mapper.toResponse(saved);
}
```
**Ưu điểm:**
- FE nhận ngay data đầy đủ, không cần gọi API GET thêm 1 lần nữa
- Tiết kiệm roundtrip
- Phù hợp cho form create → hiển thị success message + data vừa tạo

**🎯 KẾT LUẬN:** **Return `Response DTO`** là best practice cho CREATE

---

### 2️⃣ **UPDATE (PUT/PATCH)** ⚡

#### ✅ **Option 1: Return void (Simple Case)**
```java
@Transactional
public void updateAttendanceStatus(UUID attendanceId, UpdateStatusRequest request) {
    StudentAttendance attendance = getAttendanceById(attendanceId);
    attendance.setAttendanceStatus(request.getAttendanceStatus());
    attendanceRepository.save(attendance);
    
    log.info("Updated attendance {} status to {}", attendanceId, request.getAttendanceStatus());
    // Không return gì, HTTP 204 No Content
}
```
**Khi nào dùng:**
- FE chỉ cần confirm thành công (toast message: "Cập nhật thành công!")
- FE đã có data cũ và tự update state local
- Update đơn giản, không sinh ra data mới

#### ✅ **Option 2: Return Response DTO (Complex Case)**
```java
@Transactional
public StudentAttendanceDTO.Response updateAttendance(
        UUID attendanceId, 
        FullUpdateRequest request) {
    
    StudentAttendance attendance = getAttendanceById(attendanceId);
    
    // Update nhiều field
    attendance.setAttendanceStatus(request.getAttendanceStatus());
    attendance.setEvaluationStatus(request.getEvaluationStatus());
    attendance.setNote(request.getNote());
    attendance.setUpdatedAt(Instant.now());
    
    StudentAttendance updated = attendanceRepository.save(attendance);
    
    log.info("Updated attendance {} with new evaluation status", attendanceId);
    
    // Return DTO vì có thể có data được computed (updatedAt, các trường khác...)
    return mapper.toResponse(updated);
}
```
**Khi nào dùng:**
- Update phức tạp, có logic business
- Có field được tự động tính toán (updatedAt, computed fields)
- FE cần data mới để refresh UI ngay

**🎯 KẾT LUẬN:** 
- Simple update → `void`
- Complex update hoặc cần refresh data → `Response DTO`

---

### 3️⃣ **DELETE** 🗑️

#### ✅ **LUÔN LUÔN Return void**
```java
@Transactional
public void deleteAttendanceRecord(UUID attendanceId) {
    if (!attendanceRepository.existsById(attendanceId)) {
        throw new BusinessException("Attendance record not found");
    }
    attendanceRepository.deleteById(attendanceId);
    log.info("Deleted attendance record: {}", attendanceId);
    // HTTP 204 No Content
}
```

#### ✅ **Soft Delete (Cũng return void)**
```java
@Transactional
public void deleteStudent(UUID userId) {
    Student student = getStudentById(userId);
    
    if (student.getStatus() == UserStatus.DEACTIVATED) {
        throw new BusinessException("Học viên này đã bị vô hiệu hóa trước đó!");
    }
    
    student.setStatus(UserStatus.DEACTIVATED);
    student.setStudentStatus(StudentStatus.DROPPED);
    studentRepository.save(student);
    
    log.info("Soft deleted student: {}", student.getStudentCode());
}
```

**🎯 KẾT LUẬN:** **LUÔN return `void`** cho DELETE

---

### 4️⃣ **GET Operations** 🔍

#### ✅ **Single Entity**
```java
public StudentAttendanceDTO.Response getAttendanceById(UUID attendanceId) {
    StudentAttendance attendance = attendanceRepository.findById(attendanceId)
        .orElseThrow(() -> new BusinessException("Không tìm thấy bản ghi điểm danh"));
    
    return mapper.toResponse(attendance);
}
```
**Quy tắc:**
- **KHÔNG BAO GIỜ return null**
- Không tìm thấy → **throw Exception (404)**
- Luôn return `Response DTO`

#### ✅ **List/Collection**
```java
public List<StudentAttendanceDTO.Response> getAttendancesBySchedule(
        UUID scheduleId, 
        LocalDate sessionDate) {
    
    List<StudentAttendance> attendances = attendanceRepository
        .findByClassSchedule_ScheduleIdAndSessionDate(scheduleId, sessionDate);
    
    // Empty list OK, không throw exception
    return attendances.stream()
        .map(mapper::toResponse)
        .toList();
}
```
**Quy tắc:**
- Empty list → return `Collections.emptyList()` hoặc `List.of()` (KHÔNG throw exception)
- Có pagination → return `Page<Response DTO>`

---

### 5️⃣ **BATCH Operations** 🔄

#### ✅ **Simple Batch (Return void)**
```java
@Transactional
public void batchCreateAttendance(BatchCreateRequest request) {
    List<Student> students = studentEnrollmentRepository
        .findActiveStudentsByScheduleId(request.getClassScheduleId());
    
    List<StudentAttendance> attendances = students.stream()
        .map(student -> StudentAttendance.builder()
            .student(student)
            .classSchedule(schedule)
            .sessionDate(request.getSessionDate())
            .attendanceStatus(AttendanceStatus.PRESENT)
            .build())
        .toList();
    
    attendanceRepository.saveAll(attendances);
    
    log.info("Created {} attendance records for schedule {}", 
             attendances.size(), request.getClassScheduleId());
    // Return void
}
```

#### ✅ **Complex Batch (Return Result DTO)**
```java
public BatchResult batchImportAttendance(List<ImportRequest> requests) {
    BatchResult result = new BatchResult();
    
    for (ImportRequest req : requests) {
        try {
            createAttendanceRecord(req);
            result.incrementSuccess();
        } catch (Exception e) {
            result.addError(req.getStudentId(), e.getMessage());
        }
    }
    
    log.info("Batch import completed: {} success, {} failed", 
             result.getSuccessCount(), result.getFailedCount());
    
    return result;
}

@Data
public static class BatchResult {
    private int successCount;
    private int failedCount;
    private List<ErrorDetail> errors;
}
```

**🎯 KẾT LUẬN:**
- All-or-nothing batch → `void`
- Partial success cần report → `BatchResult DTO`

---

## 🏆 Summary Table for StudentAttendanceService

| Method | Return Type | Giải thích |
|--------|-------------|-----------|
| `createAttendanceRecord()` | `StudentAttendanceDTO.Response` | FE cần data mới để hiển thị |
| `batchCreateAttendance()` | `void` | Tạo hàng loạt, FE sẽ gọi GET list sau |
| `updateAttendanceStatus()` | `void` | Update đơn giản, FE tự update local state |
| `updateEvaluation()` | `void` | Update đơn giản |
| `updateFullAttendance()` | `StudentAttendanceDTO.Response` | Update phức tạp, trả data mới |
| `deleteAttendance()` | `void` | Standard DELETE |
| `getAttendanceById()` | `StudentAttendanceDTO.Response` | GET single |
| `getAttendancesBySchedule()` | `List<StudentAttendanceDTO.Response>` | GET list |
| `markAsAbsentByScheduleId()` | `void` | Batch update status |

---

## 🎨 Exception Handling

```java
// ❌ KHÔNG NÊN return null
public StudentAttendanceDTO.Response getAttendance(UUID id) {
    return attendanceRepository.findById(id)
        .map(mapper::toResponse)
        .orElse(null); // ❌ BAD
}

// ✅ NÊN throw Exception
public StudentAttendanceDTO.Response getAttendance(UUID id) {
    StudentAttendance attendance = attendanceRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Attendance not found"));
    
    return mapper.toResponse(attendance);
}
```

---

## 📝 Controller Layer Convention

```java
@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {
    
    // CREATE → 201 Created + Return body
    @PostMapping
    public ResponseEntity<Response> create(@RequestBody ManualLogRequest request) {
        Response response = attendanceService.createAttendanceRecord(request, getCoachId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    // UPDATE (return DTO) → 200 OK + Return body
    @PutMapping("/{id}")
    public ResponseEntity<Response> update(@PathVariable UUID id, @RequestBody FullUpdateRequest request) {
        Response response = attendanceService.updateFullAttendance(id, request);
        return ResponseEntity.ok(response);
    }
    
    // UPDATE (return void) → 204 No Content
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable UUID id, @RequestBody UpdateStatusRequest request) {
        attendanceService.updateAttendanceStatus(id, request);
        return ResponseEntity.noContent().build();
    }
    
    // DELETE → 204 No Content
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        attendanceService.deleteAttendance(id);
        return ResponseEntity.noContent().build();
    }
    
    // GET single → 200 OK + Return body
    @GetMapping("/{id}")
    public ResponseEntity<Response> getById(@PathVariable UUID id) {
        Response response = attendanceService.getAttendanceById(id);
        return ResponseEntity.ok(response);
    }
    
    // GET list → 200 OK + Return body (có thể empty list)
    @GetMapping
    public ResponseEntity<List<Response>> getList(@RequestParam UUID scheduleId) {
        List<Response> list = attendanceService.getAttendancesBySchedule(scheduleId);
        return ResponseEntity.ok(list);
    }
}
```

---

## 🚀 Best Practices Checklist

- [ ] CREATE operations trả về `Response DTO` hoặc `ID/Code`
- [ ] Simple UPDATE trả về `void` (HTTP 204)
- [ ] Complex UPDATE trả về `Response DTO` (HTTP 200)
- [ ] DELETE luôn trả về `void` (HTTP 204)
- [ ] GET single entity luôn trả về DTO, không tìm thấy → throw Exception
- [ ] GET list có thể trả về empty list
- [ ] Batch operations: simple → void, complex → BatchResult DTO
- [ ] Controller sử dụng đúng HTTP status code
- [ ] Log rõ ràng action và kết quả
- [ ] DTO có đủ thông tin FE cần để hiển thị

---

**📌 Lưu ý cuối cùng:**
- Đây là convention, không phải hard rule
- Có thể điều chỉnh theo nghiệp vụ cụ thể
- Quan trọng nhất là **NHẤT QUÁN** trong toàn bộ dự án
