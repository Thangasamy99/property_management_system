package com.pcc.identity.config;

import com.pcc.identity.entity.Role;
import com.pcc.identity.entity.User;
import com.pcc.identity.enums.AccountStatus;
import com.pcc.identity.repository.RoleRepository;
import com.pcc.identity.repository.UserRepository;
import com.pcc.identity.util.PasswordGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * One-time bootstrap of the first ADMIN account.
 *
 * Runs only when no user holds the ADMIN role. Credentials come from
 * PCC_ADMIN_* environment variables; when PCC_ADMIN_PASSWORD is not provided a
 * random password is generated and printed ONCE to the startup log.
 * The account is created with mustChangePassword=true so the password must be
 * rotated at first login. Because creation is guarded by the ADMIN-exists
 * check, the bootstrap can never recreate or overwrite an admin account.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AdminBootstrapRunner implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityProperties properties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepository.existsByRoles_Code("ADMIN")) {
            log.info("Admin account already initialized - bootstrap skipped.");
            return;
        }

        Role adminRole = roleRepository.findByCode("ADMIN").orElse(null);
        if (adminRole == null) {
            log.warn("ADMIN role not found - bootstrap deferred. Ensure Flyway seed migration V2 ran; bootstrap will retry on next start.");
            return;
        }

        SecurityProperties.Bootstrap bootstrap = properties.getBootstrap();

        String password = bootstrap.getAdminPassword();
        boolean generated = password == null || password.isBlank();
        if (generated) {
            password = PasswordGenerator.temporaryPassword();
        }

        User admin = new User();
        admin.setEmployeeCode(bootstrap.getAdminEmployeeCode());
        admin.setUsername(bootstrap.getAdminUsername());
        admin.setFirstName(bootstrap.getAdminFirstName());
        admin.setLastName(bootstrap.getAdminLastName());
        admin.setEmail(bootstrap.getAdminEmail());
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setAccountStatus(AccountStatus.ACTIVE);
        admin.setMustChangePassword(true);
        admin.setRoles(Set.of(adminRole));
        admin.setCreatedBy("bootstrap");
        userRepository.save(admin);

        log.warn("=".repeat(72));
        log.warn(" INITIAL ADMIN ACCOUNT CREATED");
        log.warn("   username : {}", admin.getUsername());
        log.warn("   password : {}", generated ? password : "(from PCC_ADMIN_PASSWORD env var)");
        log.warn("   NOTE     : password change is FORCED on first login.");
        if (generated) {
            log.warn("   Save this generated password now - it is shown only once.");
        }
        log.warn("=".repeat(72));
    }
}
