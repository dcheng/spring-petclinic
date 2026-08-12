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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link PetClinicAuthenticationSuccessHandler}.
 *
 * @author PetClinic contributors
 */
class PetClinicAuthenticationSuccessHandlerTests {

	private PetClinicAuthenticationSuccessHandler handler;

	private MockHttpServletRequest request;

	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		this.handler = new PetClinicAuthenticationSuccessHandler();
		this.request = new MockHttpServletRequest();
		this.response = new MockHttpServletResponse();
	}

	@Test
	void ownerUserIsRedirectedToOwnProfile() throws Exception {
		PetClinicUserDetails ownerUser = new PetClinicUserDetails("owner_george", "password", true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_OWNER")), 5);

		Authentication authentication = new UsernamePasswordAuthenticationToken(ownerUser, null,
				ownerUser.getAuthorities());

		this.handler.onAuthenticationSuccess(this.request, this.response, authentication);

		assertThat(this.response.getRedirectedUrl()).isEqualTo("/owners/5");
	}

	@Test
	void ownerWithNullOwnerIdIsRedirectedToDefault() throws Exception {
		PetClinicUserDetails ownerUser = new PetClinicUserDetails("owner_no_id", "password", true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_OWNER")), null);

		Authentication authentication = new UsernamePasswordAuthenticationToken(ownerUser, null,
				ownerUser.getAuthorities());

		this.handler.onAuthenticationSuccess(this.request, this.response, authentication);

		assertThat(this.response.getRedirectedUrl()).isEqualTo("/");
	}

	@Test
	void staffUserIsRedirectedToDefault() throws Exception {
		PetClinicUserDetails staffUser = new PetClinicUserDetails("admin", "password", true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")), null);

		Authentication authentication = new UsernamePasswordAuthenticationToken(staffUser, null,
				staffUser.getAuthorities());

		this.handler.onAuthenticationSuccess(this.request, this.response, authentication);

		assertThat(this.response.getRedirectedUrl()).isEqualTo("/");
	}

	@Test
	void vetUserIsRedirectedToDefault() throws Exception {
		PetClinicUserDetails vetUser = new PetClinicUserDetails("vet_james", "password", true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_VET")), null);

		Authentication authentication = new UsernamePasswordAuthenticationToken(vetUser, null,
				vetUser.getAuthorities());

		this.handler.onAuthenticationSuccess(this.request, this.response, authentication);

		assertThat(this.response.getRedirectedUrl()).isEqualTo("/");
	}

	@Test
	void nonPetClinicUserDetailsIsRedirectedToDefault() throws Exception {
		User genericUser = new User("generic", "password", true, true, true, true,
				Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")));

		Authentication authentication = new UsernamePasswordAuthenticationToken(genericUser, null,
				genericUser.getAuthorities());

		this.handler.onAuthenticationSuccess(this.request, this.response, authentication);

		assertThat(this.response.getRedirectedUrl()).isEqualTo("/");
	}

}
