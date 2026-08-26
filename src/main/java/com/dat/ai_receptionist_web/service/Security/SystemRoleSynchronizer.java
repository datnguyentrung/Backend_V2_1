package com.dat.ai_receptionist_web.service.Security;

import com.dat.ai_receptionist_web.domain.Security.Role;
import com.dat.ai_receptionist_web.enums.Security.SystemRoleDefinition;
import com.dat.ai_receptionist_web.repository.Security.RoleRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class SystemRoleSynchronizer implements ApplicationRunner {

    private final RoleRepository roleRepository;

    /**
     * Tác dụng: Chạy tác vụ khởi động hoặc đồng bộ dữ liệu theo ngữ cảnh của lớp.
     * Input: Nhận ApplicationArguments args từ caller hoặc request.
     * Output: Không trả về dữ liệu; cập nhật trạng thái hoặc ném lỗi khi xử lý thất bại.
     */
    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) {

        Map<String, Role> existingRoles =
                roleRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                Role::getCode,
                                Function.identity()
                        ));

        int inserted = 0;
        int updated = 0;

        for (SystemRoleDefinition definition : SystemRoleDefinition.values()) {

            Role role = existingRoles.get(definition.getCode());

            // Role chÆ°a tá»“n táº¡i -> táº¡o má»›i
            if (role == null) {
                roleRepository.save(
                        new Role(
                                definition.getCode(),
                                definition.getName(),
                                definition.getDescription(),
                                0L
                        )
                );

                inserted++;
                continue;
            }

            // Role tá»“n táº¡i -> chá»‰ sá»­a náº¿u metadata thay Ä‘á»•i
            boolean changed = false;

            if (!Objects.equals(role.getName(), definition.getName())) {
                role.setName(definition.getName());
                changed = true;
            }

            if (!Objects.equals(
                    role.getDescription(),
                    definition.getDescription()
            )) {
                role.setDescription(definition.getDescription());
                changed = true;
            }

            if (changed) {
                // KhÃ´ng cáº§n save(role)
                // Entity Ä‘ang managed -> Hibernate dirty checking tá»± UPDATE
                updated++;
            }
        }

        log.info(
                "System role sync completed: total={}, inserted={}, updated={}",
                SystemRoleDefinition.values().length,
                inserted,
                updated
        );
    }
}


