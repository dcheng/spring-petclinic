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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link OwnerAccessChecker}.
 *
 * @author PetClinic contributors
 */
class OwnerAccessCheckerTests {

	private OwnerAccessChecker checker;

	@BeforeEach
	void setUp() {
		this.checker = new OwnerAccessChecker();
	}

	@AfterEach
	void tearDown() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void ownerCanAccessOwnData() {
		setUpOwnerUser(1);
		assertThatCode(() -> this.checker.checkOwnerAccess(1)).doesNotThrowAnyException();
	}

	@Test
	void ownerCannotAccessOtherOwnersData() {
		setUpOwnerUser(2);
		assertThatThrownBy(() -> this.checker.checkOwnerAccess(1)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void ownerWithHighIdCanAccessOwnData() {
		// IDs above 127 previously failed due to Integer identity comparison (!=).
		// This test verifies the fix using .equals().
		int highId = 200;
		setUpOwnerUser(highId);
		assertThatCode(() -> this.checker.checkOwnerAccess(highId)).doesNotThrowAnyException();
	}

	@Test
	void ownerWithHighIdCannotAccessOtherOwnersData() {
		setUpOwnerUser(200);
		assertThatThrownBy(() -> this.checker.checkOwnerAccess(201)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void ownerWithNullOwnerIdIsDenied() {
		PetClinicUserDetails userDetails = new PetClinicUserDetails("owner_broken", "password", true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_OWNER")), null);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

		assertThatThrownBy(() -> this.checker.checkOwnerAccess(1)).isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void staffUserIsNotRestricted() {
		PetClinicUserDetails userDetails = new PetClinicUserDetails("admin", "password", true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")), null);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

		assertThatCode(() -> this.checker.checkOwnerAccess(1)).doesNotThrowAnyException();
	}

	@Test
	void vetUserIsNotRestricted() {
		PetClinicUserDetails userDetails = new PetClinicUserDetails("vet_james", "password", true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_VET")), null);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));

		assertThatCode(() -> this.checker.checkOwnerAccess(1)).doesNotThrowAnyException();
	}

	@Test
	void noAuthenticationDoesNotThrow() {
		SecurityContextHolder.clearContext();
		assertThatCode(() -> this.checker.checkOwnerAccess(1)).doesNotThrowAnyException();
	}

	private void setUpOwnerUser(int ownerId) {
		PetClinicUserDetails userDetails = new PetClinicUserDetails("owner_user", "password", true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_OWNER")), ownerId);
		SecurityContextHolder.getContext()
			.setAuthentication(new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities()));
	}

}
