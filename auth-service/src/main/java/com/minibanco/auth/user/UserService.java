package com.minibanco.auth.user;

import com.minibanco.auth.jwt.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {


    private final PasswordEncoder passwordEncoder;

    private final UserRepository userRepository;

    private final JwtService jwtService;

    private final UserMapper mapper;

    public UserService(PasswordEncoder passwordEncoder, UserRepository userRepository, JwtService jwtService, UserMapper mapper) {
        this.passwordEncoder = passwordEncoder;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.mapper = mapper;
    }


    public UserResponse register(String username, String password){

        if (userRepository.existsByUsername(username)){
            throw new RuntimeException(); //TODO: custom exception
        }

        String hashedPass = passwordEncoder.encode(password);

        return mapper.toResponse(userRepository.save(new User(username, hashedPass)));

    }

    public LoginResponse login(String username, String password){

        Optional<User> optionalUser = userRepository.findByUsername(username);

        if (optionalUser.isEmpty()){
            throw new RuntimeException(); // TODO: custom exeception
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new RuntimeException(); // TODO: custom exeception
        }

        String token = jwtService.generateToken(username);

        return new LoginResponse(token);

    }
}
