package com.banking.auth_service.controller;

import com.banking.auth_service.dto.UserDTO;
import com.banking.auth_service.entity.User;
import com.banking.auth_service.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserRestController {

    @Autowired
    private UserService userService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/user/{username}")
    public ResponseEntity<?> getUserById(@PathVariable String username) {
        User user = userService.getUserByUsername(username);
        return new ResponseEntity<>(new UserDTO(user), HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_USER')")
    @PutMapping("/user/{username}")
    public ResponseEntity<?> addRole(@PathVariable String username) {
        User user = userService.addRole(username);
        return new ResponseEntity<>(new UserDTO(user), HttpStatus.OK);
    }
}
