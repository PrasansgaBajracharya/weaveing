package com.weaveing.security;

import com.weaveing.entity.User;
import com.weaveing.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CustomUserDetailsService
        implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(
            UserRepository userRepository) {

        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String login)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(login)
                .orElseGet(() ->
                        userRepository.findByUsername(login)
                                .orElseThrow(() ->
                                        new UsernameNotFoundException(
                                                "Invalid email/username or password"
                                        )
                                )
                );

        List<String> authorities =
                new ArrayList<>();

        authorities.add("ROLE_USER");

        if (user.isAdmin()) {
            authorities.add("ROLE_ADMIN");
        }

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(
                        authorities.toArray(
                                new String[0]
                        )
                )
                .disabled(!user.isVerified() || user.isBanned())
                .build();
    }
}