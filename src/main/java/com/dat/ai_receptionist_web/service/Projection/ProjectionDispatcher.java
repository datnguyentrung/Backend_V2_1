package com.dat.ai_receptionist_web.service.Projection;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.service.Projection.handler.ProjectionHandler;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class ProjectionDispatcher {
    private final Map<ProjectionType, ProjectionHandler> handlers = new EnumMap<>(ProjectionType.class);

    /**
     * Tác dụng: Thực hiện logic ProjectionDispatcher của lớp hiện tại.
     * Input: Nhận List<ProjectionHandler> handlerList từ caller hoặc request.
     * Output: Khởi tạo instance của lớp với các phụ thuộc đầu vào.
     */
    public ProjectionDispatcher(List<ProjectionHandler> handlerList) {
        for (ProjectionHandler handler : handlerList) {
            ProjectionHandler previous = handlers.put(handler.supports(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate projection handler for " + handler.supports());
            }
        }
    }

    /**
     * Tác dụng: Xử lý một đơn vị công việc theo logic nghiệp vụ của lớp.
     * Input: Nhận ProjectionJob job từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    public void process(ProjectionJob job) {
        ProjectionHandler handler = handlers.get(job.projectionType());
        if (handler == null) {
            throw new IllegalStateException("No projection handler for " + job.projectionType());
        }
        handler.process(job);
    }
}


