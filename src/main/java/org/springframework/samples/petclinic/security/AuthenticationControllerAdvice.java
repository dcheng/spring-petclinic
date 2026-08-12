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

import java.security.Principal;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Makes the authenticated principal and role-based flags available to all Thymeleaf
 * templates.
 *
 * @author PetClinic contributors
 */
@ControllerAdvice
class AuthenticationControllerAdvice {

	@ModelAttribute("currentUser")
	public String currentUser(Principal principal) {
		return principal != null ? principal.getName() : null;
	}

	/**
	 * Returns {@code true} if the current user has ROLE_STAFF or ROLE_VET. Used in the
	 * navbar to hide links that OWNER-role users cannot access.
	 */
	@ModelAttribute("canSearchOwners")
	public boolean canSearchOwners(Principal principal) {
		if (principal instanceof AbstractAuthenticationToken authToken) {
			return authToken.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch(a -> "ROLE_STAFF".equals(a) || "ROLE_VET".equals(a));
		}
		return false;
	}

	/**
	 * Returns {@code true} if the current user has ROLE_STAFF. Used to show or hide the
	 * veterinarian and specialty management controls.
	 */
	@ModelAttribute("canManageVets")
	public boolean canManageVets(Principal principal) {
		if (principal instanceof AbstractAuthenticationToken authToken) {
			return authToken.getAuthorities()
				.stream()
				.map(GrantedAuthority::getAuthority)
				.anyMatch("ROLE_STAFF"::equals);
		}
		return false;
	}

}
