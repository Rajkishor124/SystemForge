package com.systemforge.backend.admin.config;

import com.systemforge.backend.common.enums.UserRole;
import com.systemforge.backend.user.entity.User;
import com.systemforge.backend.user.enums.AccountStatus;
import com.systemforge.backend.user.enums.AuthProvider;
import com.systemforge.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Initializes the default admin user on application startup.
 *
 * <p>This replaces the raw SQL insert in Flyway, allowing us to securely Use
 * Bcrypt to encode the admin password. It also includes an auto-recovery
 * mechanism for existing databases where the admin was created with a NULL password.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AdminInitializer {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:admin@systemforge.io}")
    private String adminEmail;

    @Value("${app.admin.password:admin123}")
    private String adminPassword;

    @Value("${app.admin.name:SystemForge Admin}")
    private String adminName;

    @Bean
    @Transactional
    public CommandLineRunner createDefaultAdmin() {
        return args -> {
            log.info("Checking for default admin account...");

            Optional<User> existingAdminOpt = userRepository.findByEmailIgnoreCase(adminEmail);

            if (existingAdminOpt.isEmpty()) {
                // 1. Admin doesn't exist -> Create it securely
                User admin = User.builder()
                        .name(adminName)
                        .email(adminEmail.toLowerCase())
                        .password(passwordEncoder.encode(adminPassword))
                        .role(UserRole.ADMIN)
                        .accountStatus(AccountStatus.ACTIVE)
                        .authProvider(AuthProvider.LOCAL)
                        .isEmailVerified(true)
                        .build();

                userRepository.save(admin);
                log.info("✅ Default admin created successfully: {}", adminEmail);

            } else {
                // 2. Admin exists -> Check if recovery is needed (NULL password bug)
                User existingAdmin = existingAdminOpt.get();

                if (existingAdmin.getPassword() == null) {
                    existingAdmin.setPassword(passwordEncoder.encode(adminPassword));
                    // Also ensure it is a LOCAL auth provider so they can login with password
                    existingAdmin.setAuthProvider(AuthProvider.LOCAL);
                    userRepository.save(existingAdmin);

                    log.warn("⚠️ Fixed existing admin account with NULL password: {}", adminEmail);
                } else {
                    log.info("ℹ️ Default admin account exists and is secure.");
                }
            }
        };
    }
}
