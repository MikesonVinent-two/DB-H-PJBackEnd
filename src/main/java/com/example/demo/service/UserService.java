package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import com.example.demo.dto.UserDTO;
import com.example.demo.entity.User;
import com.example.demo.dto.UserProfileDTO;

public interface UserService {
    User register(UserDTO userDTO);
    Optional<User> login(String username, String password);
    boolean existsByUsername(String username);
    Optional<User> getUserById(Long id);
    List<User> getAllUsers();
    Optional<User> getUserByUsername(String username);
    void deleteUser(Long id);
    User updateUser(User user);

    /**
     * 根据用户ID获取用户信息
     * @param userId 用户ID
     * @return 用户信息DTO
     */
    Optional<UserProfileDTO> getUserProfile(Long userId);
} 