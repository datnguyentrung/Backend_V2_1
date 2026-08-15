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

    public ProjectionDispatcher(List<ProjectionHandler> handlerList) {
        for (ProjectionHandler handler : handlerList) {
            ProjectionHandler previous = handlers.put(handler.supports(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate projection handler for " + handler.supports());
            }
        }
    }

    public void process(ProjectionJob job) {
        ProjectionHandler handler = handlers.get(job.projectionType());
        if (handler == null) {
            throw new IllegalStateException("No projection handler for " + job.projectionType());
        }
        handler.process(job);
    }
}
