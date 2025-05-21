package com.example.demo.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.UserDTO;
import com.example.demo.dto.UserDTO.LoginValidation;
import com.example.demo.dto.UserDTO.RegisterValidation;
import com.example.demo.entity.User;
import com.example.demo.service.UserService;
import com.example.demo.dto.UserProfileDTO;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);
    private final UserService userService;

    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Validated(RegisterValidation.class) @RequestBody UserDTO userDTO) {
        logger.info("收到用户注册请求 - 用户信息: username={}, contactInfo={}, role={}", 
            userDTO.getUsername(), userDTO.getContactInfo(), userDTO.getRole());
        
        try {
            // 验证请求数据
            if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
                logger.error("注册失败 - 用户名为空");
                return new ResponseEntity<>(Map.of("error", "用户名不能为空"), HttpStatus.BAD_REQUEST);
            }
            if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
                logger.error("注册失败 - 密码为空");
                return new ResponseEntity<>(Map.of("error", "密码不能为空"), HttpStatus.BAD_REQUEST);
            }
            if (userDTO.getContactInfo() == null || userDTO.getContactInfo().trim().isEmpty()) {
                logger.error("注册失败 - 联系方式为空");
                return new ResponseEntity<>(Map.of("error", "联系方式不能为空"), HttpStatus.BAD_REQUEST);
            }

            logger.debug("开始处理用户注册 - 验证通过，准备保存用户信息");
            User user = userService.register(userDTO);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("name", user.getName());
            response.put("contactInfo", user.getContactInfo());
            response.put("role", user.getRole());
            
            logger.info("用户注册成功 - ID: {}, 用户名: {}, 角色: {}", 
                user.getId(), user.getUsername(), user.getRole());
            
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            logger.error("用户注册失败 - 用户名: {}, 异常信息: {}", 
                userDTO.getUsername(), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            logger.error("用户注册发生未预期的错误 - 用户名: {}, 异常信息: {}", 
                userDTO.getUsername(), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "注册过程中发生错误：" + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Validated(LoginValidation.class) @RequestBody UserDTO userDTO) {
        logger.info("收到用户登录请求 - 用户名: {}", userDTO.getUsername());
        
        try {
            // 验证请求数据
            if (userDTO.getUsername() == null || userDTO.getUsername().trim().isEmpty()) {
                logger.error("登录失败 - 用户名为空");
                return new ResponseEntity<>(Map.of("error", "用户名不能为空"), HttpStatus.BAD_REQUEST);
            }
            if (userDTO.getPassword() == null || userDTO.getPassword().trim().isEmpty()) {
                logger.error("登录失败 - 密码为空");
                return new ResponseEntity<>(Map.of("error", "密码不能为空"), HttpStatus.BAD_REQUEST);
            }

            logger.debug("开始处理用户登录 - 用户名: {}", userDTO.getUsername());
            return userService.login(userDTO.getUsername(), userDTO.getPassword())
                    .map(user -> {
                        Map<String, Object> response = new HashMap<>();
                        response.put("id", user.getId());
                        response.put("username", user.getUsername());
                        response.put("name", user.getName());
                        response.put("contactInfo", user.getContactInfo());
                        response.put("role", user.getRole());
                        
                        logger.info("用户登录成功 - ID: {}, 用户名: {}, 角色: {}", 
                            user.getId(), user.getUsername(), user.getRole());
                        
                        return new ResponseEntity<>(response, HttpStatus.OK);
                    })
                    .orElseGet(() -> {
                        logger.warn("用户登录失败 - 用户名: {}, 原因: 用户名或密码错误", 
                            userDTO.getUsername());
                        return new ResponseEntity<>(
                            Map.of("error", "用户名或密码错误"), 
                            HttpStatus.UNAUTHORIZED);
                    });
        } catch (Exception e) {
            logger.error("用户登录发生未预期的错误 - 用户名: {}, 异常信息: {}", 
                userDTO.getUsername(), e.getMessage(), e);
            Map<String, String> error = new HashMap<>();
            error.put("error", "登录过程中发生错误：" + e.getMessage());
            return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        logger.info("收到获取所有用户请求");
        List<User> users = userService.getAllUsers();
        logger.info("成功获取所有用户 - 用户数量: {}", users.size());
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        logger.info("收到获取用户信息请求 - 用户ID: {}", id);
        return userService.getUserById(id)
                .map(user -> {
                    logger.info("成功获取用户信息 - ID: {}, 用户名: {}", 
                        user.getId(), user.getUsername());
                    return new ResponseEntity<>(user, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.warn("获取用户信息失败 - 用户ID: {}, 原因: 用户不存在", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        logger.info("收到更新用户信息请求 - 用户ID: {}", id);
        return userService.getUserById(id)
                .map(existingUser -> {
                    user.setId(id);
                    User updatedUser = userService.updateUser(user);
                    logger.info("成功更新用户信息 - ID: {}, 用户名: {}", 
                        updatedUser.getId(), updatedUser.getUsername());
                    return new ResponseEntity<>(updatedUser, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.warn("更新用户信息失败 - 用户ID: {}, 原因: 用户不存在", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        logger.info("收到删除用户请求 - 用户ID: {}", id);
        return userService.getUserById(id)
                .map(user -> {
                    userService.deleteUser(id);
                    logger.info("成功删除用户 - ID: {}, 用户名: {}", 
                        user.getId(), user.getUsername());
                    return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
                })
                .orElseGet(() -> {
                    logger.warn("删除用户失败 - 用户ID: {}, 原因: 用户不存在", id);
                    return new ResponseEntity<>(HttpStatus.NOT_FOUND);
                });
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, Object> logoutRequest) {
        logger.info("收到用户登出请求");
        
        try {
            // 获取并验证请求参数
            Long userId = Long.valueOf(logoutRequest.get("id").toString());
            String username = (String) logoutRequest.get("username");
            
            if (userId == null || username == null || username.trim().isEmpty()) {
                logger.error("登出失败 - 用户ID或用户名为空");
                return new ResponseEntity<>(Map.of("error", "用户ID和用户名不能为空"), HttpStatus.BAD_REQUEST);
            }

            // 验证用户是否存在
            return userService.getUserById(userId)
                .map(user -> {
                    if (!user.getUsername().equals(username)) {
                        logger.error("登出失败 - 用户ID和用户名不匹配");
                        return new ResponseEntity<>(Map.of("error", "用户信息验证失败"), HttpStatus.BAD_REQUEST);
                    }
                    
                    logger.info("用户验证成功，处理登出请求 - 用户ID: {}, 用户名: {}", userId, username);
                    Map<String, String> response = new HashMap<>();
                    response.put("message", "登出成功");
                    return new ResponseEntity<>(response, HttpStatus.OK);
                })
                .orElseGet(() -> {
                    logger.error("登出失败 - 用户不存在");
                    return new ResponseEntity<>(Map.of("error", "用户不存在"), HttpStatus.NOT_FOUND);
                });
        } catch (Exception e) {
            logger.error("登出过程中发生错误", e);
            return new ResponseEntity<>(Map.of("error", "登出过程中发生错误：" + e.getMessage()), 
                HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<UserProfileDTO> getUserProfile(@PathVariable Long userId) {
        return userService.getUserProfile(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
} 