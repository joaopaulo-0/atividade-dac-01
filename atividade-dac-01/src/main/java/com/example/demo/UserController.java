package com.example.demo;

import com.example.demo.UserEntity;
import com.example.demo.UserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @PostMapping
    public String create(@RequestBody UserEntity user) {

        service.createUser(user);

        return "Usuário criado";
    }

    @GetMapping
    public List<UserEntity> listUsers() {
        return service.listUsers();
    }
}