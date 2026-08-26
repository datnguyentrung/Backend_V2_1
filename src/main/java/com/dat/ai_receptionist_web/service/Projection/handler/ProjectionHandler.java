package com.dat.ai_receptionist_web.service.Projection.handler;

import com.dat.ai_receptionist_web.enums.Infrastructure.ProjectionType;
import com.dat.ai_receptionist_web.service.Projection.ProjectionJob;

public interface ProjectionHandler {
    ProjectionType supports();

    void process(ProjectionJob job);
}


