package com.dat.ai_receptionist_web.service.Projection.handler;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.service.Projection.ProjectionJob;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

@Component
public class FitnessRecordsCacheProjectionHandler implements ProjectionHandler {
    private final CacheManager cacheManager;

    /**
     * Tác dụng: Thực hiện logic FitnessRecordsCacheProjectionHandler của lớp hiện tại.
     * Input: Nhận CacheManager cacheManager từ caller hoặc request.
     * Output: Khởi tạo instance của lớp với các phụ thuộc đầu vào.
     */
    public FitnessRecordsCacheProjectionHandler(
            @Qualifier("redisCacheManager") CacheManager cacheManager
    ) {
        this.cacheManager = cacheManager;
    }

    @Override
    /**
     * Tác dụng: Thực hiện logic supports của lớp hiện tại.
     * Input: Không có tham số đầu vào.
     * Output: Trả về ProjectionType theo kết quả xử lý.
     */
    public ProjectionType supports() {
        return ProjectionType.FITNESS_RECORDS_CACHE;
    }

    @Override
    /**
     * Tác dụng: Xử lý một đơn vị công việc theo logic nghiệp vụ của lớp.
     * Input: Nhận ProjectionJob job từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void process(ProjectionJob job) {
        Cache cache = cacheManager.getCache("fitnessRecords");
        if (cache == null) {
            throw new IllegalStateException("Missing fitnessRecords cache");
        }
        cache.clear();
    }
}


