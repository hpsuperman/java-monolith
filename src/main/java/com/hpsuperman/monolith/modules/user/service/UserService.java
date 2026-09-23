package com.hpsuperman.monolith.modules.user.service;

import com.hpsuperman.monolith.modules.user.dto.LoginRequest;
import com.hpsuperman.monolith.modules.user.dto.RegisterRequest;
import com.hpsuperman.monolith.modules.user.dto.TokenVO;

public interface UserService {
    TokenVO register(RegisterRequest request);
    //  ↑返回    ↑方法名   ↑参数类型        ↑参数名
    TokenVO login(LoginRequest request);
}
