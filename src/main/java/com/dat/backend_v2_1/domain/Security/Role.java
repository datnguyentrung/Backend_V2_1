package com.dat.backend_v2_1.domain.Security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "Role", schema = "security")
public class Role {
    @Id
    @Column(name = "role_id", updatable = false, nullable = false)
    private String roleId;

    @Column(name = "role_code", unique = true, nullable = false)
    private String roleCode;

    @Column(name = "description")
    private String description;

    @Column(name = "role_name", unique = true, nullable = false)
    private String role_name;
}
