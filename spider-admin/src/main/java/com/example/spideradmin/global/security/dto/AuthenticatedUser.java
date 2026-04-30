package com.example.spideradmin.global.security.dto;

import com.example.spideradmin.domain.user.enums.UserState;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticatedUser implements Serializable {

    private String userId;
    private String userName;
    private String roleId;
    private String password;
    private UserState userState;
}
