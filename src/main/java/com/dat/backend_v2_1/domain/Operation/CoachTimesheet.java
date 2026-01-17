package com.dat.backend_v2_1.domain.Operation;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder // Giúp tạo object dễ dàng hơn: AuthToken.builder()...build()
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "CoachTimesheet", schema = "operation")
public class CoachTimesheet {
}
