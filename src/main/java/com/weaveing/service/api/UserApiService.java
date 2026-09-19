package com.weaveing.service.api;

import com.weaveing.dto.SignupRequest;
import com.weaveing.dto.api.UserApiCreateRequest;
import com.weaveing.dto.api.UserApiResponse;
import com.weaveing.entity.User;
import com.weaveing.repository.UserRepository;
import com.weaveing.service.UserService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserApiService {

    private final UserRepository userRepository;
    private final UserService userService;

    public UserApiService(
            UserRepository userRepository,
            UserService userService) {

        this.userRepository = userRepository;
        this.userService = userService;
    }

    public List<UserApiResponse> getUsers() {
        return userRepository.findAll()
                .stream()
                .map(UserApiResponse::from)
                .toList();
    }

    public UserApiResponse getUser(Long id) {
        return userRepository.findById(id)
                .map(UserApiResponse::from)
                .orElse(null);
    }

    public UserApiResponse createUser(UserApiCreateRequest request) {
        SignupRequest signupRequest = new SignupRequest();
        signupRequest.setName(request.getName());
        signupRequest.setUsername(request.getUsername());
        signupRequest.setEmail(request.getEmail());
        signupRequest.setPassword(request.getPassword());
        signupRequest.setConfirmPassword(request.getConfirmPassword());

        String result = userService.registerUser(signupRequest);

        if (!"Registration successful".equals(result)) {
            throw new IllegalArgumentException(result);
        }

        User user = userRepository.findByUsername(
                request.getUsername().trim()
        ).orElseThrow(() ->
                new IllegalStateException("Created user could not be found")
        );

        return UserApiResponse.from(user);
    }
}
