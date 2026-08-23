package com.dat.ai_receptionist_web.domain.Security;

import com.dat.ai_receptionist_web.enums.Security.PermissionAction;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "permission", schema = "security", uniqueConstraints =
        @UniqueConstraint(name = "uk_permission_code", columnNames = "code"))
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Integer permissionId;

    @Column(name = "code", nullable = false, length = 100)
    private String code;

    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 30)
    private PermissionAction action;

    @Column(name = "description", nullable = false, length = 255)
    private String description;
}
