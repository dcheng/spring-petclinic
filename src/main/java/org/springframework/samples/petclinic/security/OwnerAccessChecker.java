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

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Shared service that enforces owner-level access control. OWNER-role users may only
 * access data belonging to their own owner record. STAFF and VET roles are not
 * restricted.
 *
 * @author PetClinic contributors
 */
@Component
public class OwnerAccessChecker {

	/**
	 * Verifies that the current authenticated user (if they have ROLE_OWNER) is
	 * authorized to access the specified owner's data.
	 *
	 * <p>
	 * NOTE: This method silently allows access when the principal is not a
	 * {@link PetClinicUserDetails} instance (e.g., anonymous tokens or mock users in
	 * tests). This is safe because the security filter chain requires authentication on
	 * all owner-scoped URLs. If a future change permits anonymous access to these URLs,
	 * this method must be updated to fail closed.
	 * </p>
	 * @param ownerId the owner ID from the request path
	 * @throws AccessDeniedException if the current user is an OWNER and their ownerId
	 * does not match
	 */
	public void checkOwnerAccess(int ownerId) {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication != null && authentication.getPrincipal() instanceof PetClinicUserDetails userDetails) {
			boolean isOwnerRole = userDetails.getAuthorities()
				.stream()
				.anyMatch(a -> "ROLE_OWNER".equals(a.getAuthority()));
			if (isOwnerRole) {
				Integer currentOwnerId = userDetails.getOwnerId();
				if (currentOwnerId == null || !currentOwnerId.equals(ownerId)) {
					throw new AccessDeniedException("You do not have access to this owner's data.");
				}
			}
		}
	}

}
