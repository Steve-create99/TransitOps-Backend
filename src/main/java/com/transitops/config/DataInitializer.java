package com.transitops.config;

import com.transitops.entity.Role;
import com.transitops.entity.User;
import com.transitops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        seedUser("Kwame",  "Asante",  "admin@transitops.gh",  "admin1234",  Role.ADMIN);
        seedUser("Kofi",   "Mensah",  "driver1@transitops.gh", "driver1234", Role.DRIVER);
        seedUser("Ama",    "Owusu",   "driver2@transitops.gh", "driver1234", Role.DRIVER);
    }

    private void seedUser(String firstName, String lastName, String email,
                          String rawPassword, Role role) {
        if (userRepository.existsByEmail(email)) {
            log.info("Seed user already exists — skipping: {}", email);
            return;
        }
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .role(role)
                .build();
        userRepository.save(user);
        log.info("Seeded {} user: {}", role, email);
    }
}
