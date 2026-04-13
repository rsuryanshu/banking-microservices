package com.banking.auth_service.service;

import com.banking.auth_service.dto.RegisterDTO;
import com.banking.auth_service.entity.Role;
import com.banking.auth_service.entity.User;
import com.banking.auth_service.repository.RoleRepository;
import com.banking.auth_service.repository.UserRepository;
import com.banking.common_config.exception.BankingException;
import com.banking.common_config.exception.BankingExceptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.Set;

@Service
public class UserService {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    public void register(RegisterDTO registerDTO) {

        Optional<User> byUsername = userRepository.findByUsername(registerDTO.getUsername());
        if(byUsername.isPresent()) {
            throw new BankingException(BankingExceptionType.ALREADY_EXIST,"Username already exist");
        }
        Role role = roleRepository.findByName("ROLE_USER").orElseThrow(()-> new BankingException(BankingExceptionType.ELEMENT_NOT_FOUND, "Role not found"));
        User user = new User();
        user.setUsername(registerDTO.getUsername());
        user.setPassword(passwordEncoder.encode(registerDTO.getPassword()));
        user.setRoles(Set.of(role));
        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new BankingException(BankingExceptionType.USER_NOT_FOUND, "Username not found"));
    }

    public User addRole(String username){
        Role role = roleRepository.findByName("ROLE_ADMIN").orElseThrow(()->
                new BankingException(BankingExceptionType.ELEMENT_NOT_FOUND, "Role not found"));
        User user = userRepository.findByUsername(username).orElseThrow(()->
                new BankingException(BankingExceptionType.USER_NOT_FOUND, "Username not found"));
        user.getRoles().add(role);

        return userRepository.save(user);
    }
}
