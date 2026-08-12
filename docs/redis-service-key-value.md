# Redis keys/values trong service

Phạm vi rà soát: `src/main/java/com/dat/backend_v2_1/service`.

Nguồn cấu hình Redis:

- `RedisConfig`: bật `@EnableCaching`.
- Spring Cache prefix: `cacheName:` vì `computePrefixWith(cacheName -> cacheName + ":")`.
- Key serializer: `StringRedisSerializer`.
- Value/hash value serializer cho `RedisTemplate<String, Object>` và Spring Cache: `RedisSerializer.json()`.
- `StringRedisTemplate`: key/value/hash field/hash value đều là string.
- TTL mặc định Spring Cache: `7 ngày`.
- TTL riêng trong `RedisConfig`:
  - `classScheduleDetail`: `86400s + random 0..3599s`.
  - `fcmTokensByRole`: `3600s + random 0..299s`.
  - Các cache khác trong service nếu không có cấu hình riêng: `7 ngày`.

## Spring Cache active

| Redis key pattern thực tế | Nguồn tạo value | Value | TTL | Điều kiện không cache | Evict |
|---|---|---|---|---|---|
| `fitnessRecords:{search}-{skillLevel}-{pageNumber}-{pageSize}` | `FitnessRecordService.listFitnessRecords(search, skillLevel, pageable)` | `PageResponse<FitnessRecordDTO.Response>` | 7 ngày | Không có `unless` | `FitnessRecordService.createFitnessRecord`: xóa toàn bộ `fitnessRecords:*` |
| `classScheduleDetail:{scheduleId}` | `ClassScheduleService.getClassScheduleDetail(scheduleId)` | `ClassScheduleResDTO.ClassScheduleDetail` | 1 ngày + random 0..1 giờ | `result == null` | `ClassScheduleService.createClassSchedule/updateClassSchedule/deleteClassSchedule/updateStatus`; `CoachAssignmentService.create/delete/update`; `StudentEnrollmentService.create/delete/update` |
| `classScheduleList:{generatedKey}` | `ClassScheduleService.filterClassSchedules(branchId, weekday, scheduleLevel, scheduleShift, scheduleLocation, scheduleStatus, scheduleIds)` | `List<ClassScheduleResDTO.ClassScheduleDetail>` | 7 ngày | `result == null || result.isEmpty()` | `ClassScheduleService.createClassSchedule/updateClassSchedule/deleteClassSchedule/updateStatus`; `CoachAssignmentService.create/delete/update`; `StudentEnrollmentService.create/delete/update` |
| `coachAssignments:{userId}_{status}` | `CoachAssignmentService.findCoachAssignmentsByCoachId(userId, status)` | `List<CoachAssignmentResDTO.SimpleResponse>` | 7 ngày | `result == null || result.isEmpty()` | `CoachAssignmentService.createCoachAssignment/deleteCoachAssignment/updateCoachAssignment`: xóa toàn bộ |
| `detailedCoachAssignments:{userId}_{status}` | `CoachAssignmentService.findDetailedCoachAssignmentsByUserId(userId, status)` | `List<CoachAssignmentResDTO.Response>` | 7 ngày | `result == null || result.isEmpty()` | `CoachAssignmentService.createCoachAssignment/deleteCoachAssignment/updateCoachAssignment`: xóa toàn bộ |
| `studentEnrollmentsByClassDTO:{classScheduleId}` | `StudentEnrollmentService.getEnrolledStudentItemsByClass(classScheduleId)` | `List<StudentEnrollmentResDTO.EnrolledStudentItem>` | 7 ngày | `result == null || result.isEmpty()` | `StudentEnrollmentService.createStudentEnrollment/deleteStudentEnrollment/updateStudentEnrollment`: xóa toàn bộ |
| `fcmTokensByRole:{roleCode}` | `AuthTokenService.getAllFcmTokensByRoleCode(roleCode)` | `List<String>` FCM token distinct, không null/empty | 1 giờ + random 0..5 phút | `result == null || result.isEmpty()` | `AuthTokenService.logoutUserTokens/updateUserTokens/updateFcmTokenOnly/deleteFcmTokenOnly`: xóa toàn bộ |

Ghi chú key `classScheduleList:{generatedKey}`:

- Method không khai báo `key`, nên Spring tự sinh key từ toàn bộ tham số.
- Với nhiều tham số, key là dạng Spring generated key, thường stringify theo `SimpleKey [...]`, có cả tham số `null`.

## Redis thủ công trong LeaderboardService

| Redis key pattern | Redis type | Field/member | Value | Score | TTL | Ghi/đọc/xóa |
|---|---|---|---|---|---|---|
| `leaderboard:{year}:Q{quarter}` | ZSET | member = `studentCode` | Không có value riêng, member là mã học viên | `QuarterSummaryForRedis.totalQuarterScore` | Không set TTL trong rebuild | Đọc `reverseRange`, `zCard`; ghi lại trong `rebuildLeaderboardCache`; xóa trước khi rebuild |
| `leaderboard_data:{year}:Q{quarter}` | HASH | field = `studentCode` | `YearlySummaryDTO.QuarterSummaryForRedis` | Không áp dụng | Không set TTL trong rebuild | Đọc `multiGet`; ghi `putAll` trong `rebuildLeaderboardCache`; xóa trước khi rebuild |
| `leaderboard:fitness:{year}:Q{quarter}:{skillLevel}` | ZSET | member = `studentCode` | Không có value riêng, member là mã học viên | `getScore(FitnessRecordDTO.Metrics)` | Có thể 30 ngày khi gọi `updateFitnessLeaderboard`; rebuild/sync batch không set TTL | Đọc `reverseRange`, `zCard`, `score`, `reverseRank`; ghi `add`/pipeline `zAdd`; xóa member khi không còn record |
| `leaderboard_data:fitness:{year}:Q{quarter}:{skillLevel}` | HASH | field = `studentCode` | `FitnessRecordDTO.Metrics` | Không áp dụng | Có thể 30 ngày khi gọi `updateFitnessLeaderboard`; rebuild/sync batch không set TTL | Đọc `multiGet`; ghi `put`, `putAll`, pipeline `hSet`; xóa field khi không còn record |
| `leaderboard_history:fitness:{year}:Q{quarter}:{skillLevel}` | HASH | field = `studentCode` | String rank cũ, giá trị = rank 1-based trước khi cập nhật | Không áp dụng | 30 ngày khi có ghi history | Đọc `multiGet`; ghi `put`/pipeline `hSet`; xóa field khi không còn record |

Công thức score fitness trong `LeaderboardService.getScore(metrics)`:

```text
baseScore =
  fitnessLevel * 10_000_000
  + durationLevel * 1_000_000
  + amountLevel * 100_000
  + round((amount / duration) * 10_000)

dateBonus = (10_000_000_000 - createdAtEpochSecondsUTC) / 10_000_000_000
score = baseScore + dateBonus
```

Nếu `metrics.isQualified == false` thì `fitnessLevel = 0`. `duration <= 0` được thay bằng `1`.

## Cache name chỉ bị evict hoặc đã comment

| Cache/key | Tình trạng trong folder service | Ghi chú |
|---|---|---|
| `coachDetail:*` | Chỉ còn evict active trong `CoachAssignmentService`; các `@Cacheable/@CachePut/@CacheEvict` trong `CoachService` đã comment | `RedisConfig` vẫn cấu hình TTL 1 ngày + random 0..1 giờ, nhưng service không tạo value active |
| `coachDetailByCode:*` | Annotation trong `CoachService` đã comment | Không có writer active trong folder service |
| `coach:*`, `coachByCode:*`, `classSchedule:*` | Chỉ có TTL trong `RedisConfig` | Không thấy annotation active trong folder service |
| `studentEnrollmentsById:*`, `studentEnrollmentsByCode:*`, `studentEnrollmentsByClass:*`, `singleEnrollment:*` | Chỉ có TTL trong `RedisConfig` | Service active đang dùng `studentEnrollmentsByClassDTO`, không phải `studentEnrollmentsByClass` |
| `studentDetail:*` | Chỉ xuất hiện trong comment ở `ClassSessionService` và `StudentAttendanceService` | Không có tác động Redis active |

