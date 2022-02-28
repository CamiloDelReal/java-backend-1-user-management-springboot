package org.xapps.services.usermanagementservice.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.xapps.services.usermanagementservice.security.SecurityConfig;
import org.xapps.services.usermanagementservice.dtos.LoginRequest;
import org.xapps.services.usermanagementservice.dtos.LoginResponse;
import org.xapps.services.usermanagementservice.dtos.UserRequest;
import org.xapps.services.usermanagementservice.dtos.UserResponse;
import org.xapps.services.usermanagementservice.entities.Role;
import org.xapps.services.usermanagementservice.entities.User;
import org.xapps.services.usermanagementservice.entities.UserRole;
import org.xapps.services.usermanagementservice.exceptions.DuplicityException;
import org.xapps.services.usermanagementservice.exceptions.InvalidCredentials;
import org.xapps.services.usermanagementservice.exceptions.NotFoundException;
import org.xapps.services.usermanagementservice.repositories.RoleRepository;
import org.xapps.services.usermanagementservice.repositories.UserRepository;
import org.xapps.services.usermanagementservice.repositories.UserRoleRepository;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService implements UserDetailsService {
    private final ModelMapper modelMapper;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final SecurityConfig securityConfig;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Autowired
    public UserService(ModelMapper modelMapper, ObjectMapper objectMapper, BCryptPasswordEncoder passwordEncoder,
                       @Lazy AuthenticationManager authenticationManager, SecurityConfig securityConfig,
                       UserRepository userRepository, RoleRepository roleRepository, UserRoleRepository userRoleRepository) {
        log.debug("User service is being created");
        this.modelMapper = modelMapper;
        this.objectMapper = objectMapper;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.securityConfig = securityConfig;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        log.debug("loadUserByUsername " + username);
        UserDetails userDetails = null;
        User user = userRepository.findByEmail(username).orElse(null);
        if (user != null) {
            List<GrantedAuthority> authorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority(role.getName())).collect(Collectors.toList());
            userDetails = new org.springframework.security.core.userdetails.User(user.getEmail(), user.getProtectedPassword(), true, true, true, true, authorities);
        }
        return userDetails;
    }

    public LoginResponse login(LoginRequest loginRequest) throws InvalidCredentials {
        log.debug("login " + loginRequest.getEmail());
        LoginResponse response = null;
        try {
            Authentication authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
            User user = userRepository.findByEmail(loginRequest.getEmail()).orElse(null);
            if (user != null) {
                long currentTimestamp = Instant.now().toEpochMilli();
                Date expiration = new Date(currentTimestamp + securityConfig.getValidity());
                UserResponse innerUser = modelMapper.map(user, UserResponse.class);
                String token = Jwts.builder()
                        .setSubject(objectMapper.writeValueAsString(innerUser))
                        .setIssuedAt(new Date(currentTimestamp))
                        .setExpiration(expiration)
                        .signWith(SignatureAlgorithm.HS256, securityConfig.getKey())
                        .compact();
                response = new LoginResponse(token, securityConfig.getType(), expiration.getTime());
            }
        } catch (Exception ex) {
            log.error("Exception captured", ex);
            throw new InvalidCredentials();
        }
        return response;
    }

    public List<UserResponse> getAll() {
        log.debug("getAll");
        List<User> users = userRepository.findAll();
        List<UserResponse> response = null;
        if (users != null && !users.isEmpty()) {
            response = users.stream().map(u -> modelMapper.map(u, UserResponse.class)).collect(Collectors.toList());
        } else {
            log.debug("No users in database");
            response = new ArrayList<>();
        }
        return response;
    }

    public UserResponse getById(Long id) throws NotFoundException {
        log.debug("getById " + id);
        Optional<User> user = userRepository.findById(id);
        UserResponse response = null;
        if(user.isPresent()) {
            response = modelMapper.map(user.get(), UserResponse.class);
        } else {
            log.debug("Nonexistent user with id " + id);
            throw new NotFoundException("Nonexistent user with id " + id);
        }
        return response;
    }

    public UserResponse create(UserRequest userRequest) throws DuplicityException {
        log.debug("create " + userRequest);
        UserResponse response = null;
        Optional<User> duplicity = userRepository.findByEmail(userRequest.getEmail());
        if (duplicity.isPresent()) {
            log.debug("Email " + userRequest.getEmail() + " is in use");
            throw new DuplicityException("Email " + userRequest.getEmail() + " is in use");
        } else {
            User user = modelMapper.map(userRequest, User.class);
            user.setProtectedPassword(passwordEncoder.encode(userRequest.getPassword()));
            List<Role> roles = null;
            if(userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
                roles = roleRepository.findByIds(userRequest.getRoles());
            }
            if(roles == null || roles.isEmpty()) {
                Role guestRole = roleRepository.findByName(Role.GUEST).orElse(null);
                roles = List.of(guestRole);
            }
            user.setRoles(roles);
            userRepository.save(user);
            response = modelMapper.map(user, UserResponse.class);
        }
        return response;
    }

    public UserResponse edit(Long id, UserRequest userRequest) throws NotFoundException, DuplicityException {
        log.debug("edit " + id + " " + userRequest);
        UserResponse response = null;
        Optional<User> userContainer = userRepository.findById(id);
        if (userContainer.isPresent()) {
            Optional<User> duplicity = userRepository.findByIdNotAndEmail(id, userRequest.getEmail());
            if (duplicity.isPresent()) {
                throw new DuplicityException("Email " + userRequest.getEmail() + " is in use");
            } else {
                User user = userContainer.get();
                user.setFirstName(userRequest.getFirstName());
                user.setLastName(userRequest.getLastName());
                user.setEmail(userRequest.getEmail());
                user.setProtectedPassword(passwordEncoder.encode(userRequest.getPassword()));
                if (userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
                    List<Role> roles = roleRepository.findByIds(userRequest.getRoles());
                    if(roles != null && !roles.isEmpty()) {
                        userRoleRepository.deleteRolesByUserId(user.getId());
                        user.setRoles(roles);
                        userRoleRepository.saveAll(roles.stream().map(role -> new UserRole(new UserRole.UserRoleId(user.getId(), role.getId()))).collect(Collectors.toList()));
                    }
                }
                userRepository.save(user);
                response = modelMapper.map(user, UserResponse.class);
            }
        } else {
            throw new NotFoundException("Nonexistent user with id " + id);
        }
        return response;
    }

    public void delete(Long id) throws NotFoundException {
        log.debug("delete " + id);
        Optional<User> userContainer = userRepository.findById(id);
        if (userContainer.isPresent()) {
            userRoleRepository.deleteRolesByUserId(userContainer.get().getId());
            userRepository.delete(userContainer.get());
        } else {
            throw new NotFoundException("Nonexistent user with id " + id);
        }
    }

    public boolean hasAdminRole(UserRequest userRequest) {
        boolean has = false;
        if (userRequest.getRoles() != null && !userRequest.getRoles().isEmpty()) {
            Role administratorRole = roleRepository.findByName(Role.ADMINISTRATOR).orElse(null);
            has = (administratorRole != null && userRequest.getRoles().stream().anyMatch(id -> Objects.equals(id, administratorRole.getId())));
        }
        return has;
    }

    public boolean hasAdminRole(User user) {
        boolean has = false;
        if (user.getRoles() != null && !user.getRoles().isEmpty()) {
            Role administratorRole = roleRepository.findByName(Role.ADMINISTRATOR).orElse(null);
            has = (administratorRole != null && user.getRoles().stream().anyMatch(role -> Objects.equals(role.getId(), administratorRole.getId())));
        }
        return has;
    }
}
