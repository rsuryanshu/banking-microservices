package com.banking.auth_service.dto;

import com.banking.auth_service.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    private String username;

    public UserDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
    }
}
