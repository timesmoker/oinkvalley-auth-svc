package com.oinkvalley.auth_svc.service;

import com.oinkvalley.auth_svc.db.domain.User;
import com.oinkvalley.auth_svc.db.repository.UserRepository;
import com.oinkvalley.auth_svc.security.AuthUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DatabaseUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) {
		// 이름만 username 이고 우리는 email로 쓰고 있음
		User user = userRepository.findByEmail(email.trim())
				.orElseThrow(() -> new UsernameNotFoundException(email.trim()));
		List<SimpleGrantedAuthority> authorities = new ArrayList<>();
		for (String r : user.getRoles()) {
			if (r == null || r.isBlank()) {
				continue;
			}
			String role = r.trim();
			authorities.add(new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role));
		}
		if (authorities.isEmpty()) {
			authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
		}
		return new AuthUserPrincipal(user.getId(), user.getEmail(), user.getPasswordHash(), authorities);
	}
}
