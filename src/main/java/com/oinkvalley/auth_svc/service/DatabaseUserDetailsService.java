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

	/**
	 * {@code username} 파라미터에는 로그인 요청의 이메일(정규화된 문자열)이 전달된다.
	 */
	@Override
	public UserDetails loadUserByUsername(String username) {
		String email = username.trim();
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException(email));
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
		return new AuthUserPrincipal(user.getId(), user.getUsername(), user.getPasswordHash(), authorities);
	}
}
