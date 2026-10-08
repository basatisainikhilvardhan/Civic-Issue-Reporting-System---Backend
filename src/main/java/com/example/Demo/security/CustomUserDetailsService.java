package com.example.Demo.security;

import com.example.Demo.UserRepository;
import com.example.Demo.Users;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String emailOrUsername) throws UsernameNotFoundException {
        Users user = userRepository.findByEmail(emailOrUsername)
                .orElseGet(() -> {
                    Users u = userRepository.findByUsername(emailOrUsername);
                    if (u == null) {
                        throw new UsernameNotFoundException("User not found with email or username: " + emailOrUsername);
                    }
                    return u;
                });

        return new User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
