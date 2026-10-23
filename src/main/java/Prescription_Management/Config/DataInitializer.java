package Prescription_Management.Config;

import Prescription_Management.Entity.Role;
import Prescription_Management.Entity.User;
import Prescription_Management.Repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByRole(Role.ADMIN)) {
            User admin = new User();
            admin.setName("System Administrator");
            admin.setRegistrationNumber("ADMIN001");
            admin.setPhoneNumber("+1000000000");
            admin.setPassword(passwordEncoder.encode("Admin@12345"));
            admin.setRole(Role.ADMIN);

            userRepository.save(admin);
            log.info("Default administrator account initialized: Registration Number = ADMIN001, Password = Admin@12345");
        }
    }
}
