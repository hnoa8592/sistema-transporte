package com.transporte.security.service;

import com.transporte.security.port.UserAuthPort;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserAuthPort userAuthPort;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAuthPort.UserAuthData userData = userAuthPort.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<GrantedAuthority> authorities = Stream.concat(
                userData.roles().stream().<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role)),
                userData.permissions().stream().<GrantedAuthority>map(SimpleGrantedAuthority::new)
        ).collect(Collectors.toList());

        return TransporteUserDetails.builder()
                .userId(userData.userId())
                .username(userData.username())
                .password(userData.password())
                .tenantId(userData.tenantId())
                .active(userData.active())
                .authorities(authorities)
                .build();
    }
}
