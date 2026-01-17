package com.dat.backend_v2_1.domain.Core;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Builder // Giúp tạo object dễ dàng hơn: AuthToken.builder()...build()
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "Branch", schema = "core")
public class Branch {
}
