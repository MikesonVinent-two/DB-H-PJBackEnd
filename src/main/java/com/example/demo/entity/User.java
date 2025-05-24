package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "users")
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 50, message = "用户名长度必须在3-50个字符之间")
    @Column(unique = true)
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, message = "密码长度不能小于6个字符")
    private String password;
    
    @Column(name = "name")
    private String name;
    
    @Column(name = "contact_info")
    private String contactInfo;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role = UserRole.CROWDSOURCE_USER; // 默认为众包用户
    
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;
    
    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;
    
    // 构造函数
    public User() {
        this.createdAt = java.time.LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    
    public User(String username, String password, String contactInfo) {
        this();
        this.username = username;
        this.password = password;
        this.contactInfo = contactInfo;
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
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getContactInfo() {
        return contactInfo;
    }
    
    public void setContactInfo(String contactInfo) {
        this.contactInfo = contactInfo;
    }
    
    public UserRole getRole() {
        return role;
    }
    
    public void setRole(UserRole role) {
        this.role = role;
    }
    
    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public java.time.LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(java.time.LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    // 更新时间的方法
    public void updateTimestamp() {
        this.updatedAt = java.time.LocalDateTime.now();
    }
} 