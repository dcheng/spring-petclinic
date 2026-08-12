/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.security;

import java.util.Collections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Custom {@link UserDetailsService} that loads user details from the database.
 *
 * @author PetClinic contributors
 */
@Service
public class PetClinicUserDetailsService implements UserDetailsService {

	private static final Logger logger = LoggerFactory.getLogger(PetClinicUserDetailsService.class);

	private final UserRepository userRepository;

	public PetClinicUserDetailsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		User user = this.userRepository.findByUsername(username)
			.orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

		if ("ROLE_OWNER".equals(user.getRole()) && user.getOwnerId() == null) {
			logger.warn("User '{}' has ROLE_OWNER but no owner_id configured. "
					+ "This user will be denied access to all owner-scoped pages.", username);
		}

		return new PetClinicUserDetails(user.getUsername(), user.getPassword(), user.isEnabled(),
				Collections.singletonList(new SimpleGrantedAuthority(user.getRole())), user.getOwnerId());
	}

}
