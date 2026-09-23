package com.hpsuperman.monolith.modules.auth.service.impl;

import com.hpsuperman.monolith.common.security.LoginUser;
import com.hpsuperman.monolith.modules.user.dto.UserConverter;
import com.hpsuperman.monolith.modules.user.entity.User;
import com.hpsuperman.monolith.modules.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    private final UserService userService;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userService.getByUsername(username);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getNickname(),
                user.getStatus(),
                UserConverter.parseRoles(user.getRoles()));
    }
}
