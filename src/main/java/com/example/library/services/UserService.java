package com.example.library.services;

import com.example.library.dtos.UserResponseDTO;
import com.example.library.entities.Role;
import com.example.library.entities.User;
import com.example.library.projections.UserDetailsProjection;
import com.example.library.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        List<UserDetailsProjection> result = userRepository.searchUserAndRolesByUsername(username);

        if(result.isEmpty()) {
            throw new UsernameNotFoundException("Username not found");
        }

        User user = new User();

        user.setEmail(username);
        user.setPassword(result.get(0).getPassword());

        for(UserDetailsProjection x : result) {
            user.getRoles().add(new Role(x.getRoleId(), x.getAuthority()));
        }

        return user;
    }

    protected User authenticated() {

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            Jwt jwtPrincipal = (Jwt) authentication.getPrincipal();
            String username = jwtPrincipal.getClaim("username");
            return userRepository.findByEmail(username)
                    .orElseThrow(() -> new UsernameNotFoundException("Username not found"));

    }

    @Transactional(readOnly = true)
    public UserResponseDTO getMe() {
        User user = authenticated();
        return new UserResponseDTO(user);
    }
}
