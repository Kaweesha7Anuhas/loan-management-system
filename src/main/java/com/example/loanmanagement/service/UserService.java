package com.example.loanmanagement.service;

import com.example.loanmanagement.dto.request.CreateUserRequest;
import com.example.loanmanagement.dto.response.UserDtoResponse;
import java.util.List;

public interface UserService {
    UserDtoResponse createUser(CreateUserRequest request);
    List<UserDtoResponse> getAllUsers();
    void toggleUserStatus(Long id);
}