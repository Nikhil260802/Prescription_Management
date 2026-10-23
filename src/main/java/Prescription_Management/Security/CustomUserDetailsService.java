package Prescription_Management.Security;

import Prescription_Management.Entity.User;
import Prescription_Management.Repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String registrationNumber) throws UsernameNotFoundException {
        User user = userRepository.findByRegistrationNumber(registrationNumber)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with registration number: " + registrationNumber));
        return new CustomUserDetails(user);
    }
}
