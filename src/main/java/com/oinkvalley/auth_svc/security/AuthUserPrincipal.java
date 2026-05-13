package com.oinkvalley.auth_svc.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class AuthUserPrincipal implements UserDetails {

	private final long userId;
	private final String username;
	private final String passwordHash;
	private final Collection<? extends GrantedAuthority> authorities;

	public AuthUserPrincipal(
			long userId,
			String username,
			String passwordHash,
			Collection<? extends GrantedAuthority> authorities
	) {
		this.userId = userId;
		this.username = username;
		this.passwordHash = passwordHash;
		this.authorities = authorities;
	}

	public long getUserId() {
		return userId;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return authorities;
	}

	@Override
	public String getPassword() {
		return passwordHash;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return true;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return true;
	}
}
