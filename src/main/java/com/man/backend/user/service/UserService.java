package com.man.backend.user.service;

import com.man.backend.user.model.AppUser;
import com.man.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AppUser getOrCreateByOpenid(String openid) {
        return userRepository.findByOpenid(openid)
                .orElseGet(() -> userRepository.save(new AppUser(null, openid)));
    }
}
