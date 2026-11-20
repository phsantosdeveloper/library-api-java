package com.example.library.services;

import com.example.library.entities.Role;
import com.example.library.entities.User;
import com.example.library.projections.UserDetailsProjection;
import com.example.library.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

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
}
