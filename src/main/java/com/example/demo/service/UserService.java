package com.example.demo.service;

import java.util.List;
import java.util.Optional;

import com.example.demo.dto.UserDTO;
import com.example.demo.entity.User;

public interface UserService {
    User register(UserDTO userDTO);
    Optional<User> login(String username, String password);
    boolean existsByUsername(String username);
    Optional<User> getUserById(Long id);
    List<User> getAllUsers();
    Optional<User> getUserByUsername(String username);
    void deleteUser(Long id);
    User updateUser(User user);
} 