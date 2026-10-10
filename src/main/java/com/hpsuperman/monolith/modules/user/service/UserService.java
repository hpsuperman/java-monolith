package com.hpsuperman.monolith.modules.user.service;

import com.hpsuperman.monolith.modules.user.dto.*;

public interface UserService {
    TokenVO register(RegisterRequest request);

    //  ↑返回    ↑方法名   ↑参数类型        ↑参数名
    TokenVO login(LoginRequest request);

    TokenVO refresh(RefreshRequest request);

    void logout(LogoutRequest request);

    void updateProfile(Long userId,UpdateProfileRequest request);

    UserVO getProfile(Long userId);
}
