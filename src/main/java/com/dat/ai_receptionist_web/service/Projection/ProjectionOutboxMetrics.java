package com.dat.ai_receptionist_web.service.Projection;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectionOutboxMetrics {
    private final ProjectionOutboxService outboxService;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    void register() {
        Gauge.builder("projection.outbox.pending.count", outboxService,
                        service -> service.countOutstanding())
                .register(meterRegistry);
        Gauge.builder("projection.outbox.oldest.age.seconds", outboxService,
                        service -> service.oldestOutstandingAgeSeconds())
                .register(meterRegistry);
        Gauge.builder("projection.outbox.dead.count", outboxService,
                        service -> service.countDead())
                .register(meterRegistry);
    }
}


