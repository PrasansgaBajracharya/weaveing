package com.weaveing.api;

import com.weaveing.dto.api.UserApiCreateRequest;
import com.weaveing.dto.api.UserApiResponse;
import com.weaveing.service.api.UserApiService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserApiService userApiService;

    public UserRestController(UserApiService userApiService) {
        this.userApiService = userApiService;
    }

    @GetMapping
    public ResponseEntity<List<UserApiResponse>> getUsers() {
        return ResponseEntity.ok(userApiService.getUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUser(@PathVariable Long id) {
        UserApiResponse user = userApiService.getUser(id);

        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "User not found"));
        }

        return ResponseEntity.ok(user);
    }

    @PostMapping
    public ResponseEntity<?> createUser(
            @Valid @RequestBody UserApiCreateRequest request) {

        try {
            UserApiResponse created = userApiService.createUser(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}
