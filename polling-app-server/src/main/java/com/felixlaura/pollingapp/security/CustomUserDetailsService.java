package com.felixlaura.pollingapp.security;

import com.felixlaura.pollingapp.model.User;
import com.felixlaura.pollingapp.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * Using interface UserDetailsService we custom the load a user's data given its username
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        //Let people login with either email or username
        User user = userRepository.findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
                .orElseThrow(()->new UsernameNotFoundException("User not found with the username or email " + usernameOrEmail));
        return UserPrincipal.create(user);
    }

    //This method is used by JWTAuthenticationFilter
    @Transactional
    public UserDetails loadUserById(Long id){
        User user = userRepository.findById(id).orElseThrow(()-> new UsernameNotFoundException("User not found with id: " + id));
        return UserPrincipal.create(user);
    }
}
