package com.example.demo.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserDTO {
    
    // 定义验证分组
    public interface RegisterValidation {}
    public interface LoginValidation {}
    
    private Long id;
    
    @NotBlank(message = "用户名不能为空", groups = {RegisterValidation.class, LoginValidation.class})
    @Size(min = 4, max = 50, message = "用户名长度必须在4-50个字符之间", groups = {RegisterValidation.class})
    private String username;
    
    @NotBlank(message = "邮箱不能为空", groups = RegisterValidation.class)
    @Email(message = "邮箱格式不正确", groups = RegisterValidation.class)
    private String email;
    
    @NotBlank(message = "密码不能为空", groups = {RegisterValidation.class, LoginValidation.class})
    @Size(min = 6, max = 100, message = "密码长度必须在6-100个字符之间", groups = {RegisterValidation.class})
    private String password;
    
    private String role;
    
    // 构造函数
    public UserDTO() {
    }
    
    public UserDTO(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = "USER"; // 默认角色
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getRole() {
        return role;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
} 