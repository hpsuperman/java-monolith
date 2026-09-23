package com.hpsuperman.monolith.modules.auth.service;

import com.hpsuperman.monolith.modules.auth.dto.LoginRequest;
import com.hpsuperman.monolith.modules.auth.dto.LoginVO;
import com.hpsuperman.monolith.modules.auth.dto.RefreshTokenRequest;
import com.hpsuperman.monolith.modules.user.dto.UserVO;

public interface AuthService {
    LoginVO login(LoginRequest request);

    LoginVO refresh(RefreshTokenRequest request);

    void logout(String refreshToken, String accessToken);

    UserVO currentUser();
}
