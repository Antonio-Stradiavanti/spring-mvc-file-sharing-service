package ru.manannikov.filesharingservice.securityservice.services;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import ru.manannikov.filesharingservice.securityservice.dto.LoginResponse;
import ru.manannikov.filesharingservice.securityservice.dto.UserDto;
import ru.manannikov.filesharingservice.securityservice.models.UserEntity;
import ru.manannikov.filesharingservice.securityservice.repository.UserRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService service;

    public UserEntity signup(UserEntity user) {
        // Если пользователь с переданным email уже есть, то нужно выбросить exception
        if (
            repository.existsByUsername(user.getUsername())
        )
            throw new IllegalArgumentException("Пользователь с таким адресом электронной почты уже зарегистрирован.");
        // При регистрации надо зашифровать переданный пользователем пароль.
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        // Поскольку id у переданного пользователя -> null, то он будет сохранен в БД.
        return repository.save(user);
    }
    /**
     * <p>Выполняет аутентификацию пользователя, генерирует json web accessToken; <p/>
     * <p>AuthenticationManager будет искать пользователя в БД по email, если не найдет, то выбросит исключение, производное от AuthenticationException.<p/>
     * <p>Объект auth содержит:
       <ul>
       <li>email</li>
       <li>корректный пароль (проверен методом additionalAuthenticationChecks объекта DaoAuthenticationProvider)</li>
       <li>полномочия (которые передает DaoAuthenticationProvider, путем вызова метода getAuthorities() реализации UserDetails.</li>
       </ul> -> то есть данные пользователя, успешно прошедшего аутентификацию, то есть процесс проверки личности.
     * <p/>
     * @param email -> адрес электронной почты пользователя -> его principal ;
     * @param password -> пароль пользователя -> его credentials;
     * @return LoginResponse -> dto, содержащий всю информацию об авторизированном пользователе и его токен доступа.
     */
    public LoginResponse login(String email, String password) {

        // Выбросит исключение, если пользователь передаст некорректный пароль
        authenticationManager.authenticate(
            // DaoAuthenticationProvider поддерживает только аутентификацию с использованием UsernamePasswordAuthenticationToken.
            new UsernamePasswordAuthenticationToken(email, password)
        );

        final UserEntity user = repository.findByUsername(email).get();
        final Long id = user.getId();
        final String username = user.getUsername();

        return new LoginResponse(
            UserDto.fromEntity(user),
            service.generateAccessToken(id, username, user.getRole()),
            service.generateRefreshToken(id, username)
        );
    }

    public LoginResponse refresh(
        final String refreshToken
    ) {
        final UserEntity user = service.getUserFromRefreshToken(refreshToken);
        final Long id = user.getId();
        final String username = user.getUsername();

        return new LoginResponse(
            UserDto.fromEntity(user),
            service.generateAccessToken(id, username, user.getRole()),
            service.generateRefreshToken(id, username)
        );
    }
}