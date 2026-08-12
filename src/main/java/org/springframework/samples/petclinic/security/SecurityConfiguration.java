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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the PetClinic application.
 *
 * @author PetClinic contributors
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

	private final PetClinicAuthenticationSuccessHandler successHandler;

	public SecurityConfiguration(PetClinicAuthenticationSuccessHandler successHandler) {
		this.successHandler = successHandler;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests(
				authorize -> authorize.requestMatchers("/login", "/css/**", "/webjars/**", "/resources/**")
					.permitAll()
					.requestMatchers("/owners/new")
					.hasRole("STAFF")
					.requestMatchers("/owners/find", "/owners")
					.hasAnyRole("STAFF", "VET")
					// Creating, editing and deleting vets, their working hours, and
					// specialties is restricted to staff.
					.requestMatchers("/vets/new", "/vets/*/edit", "/vets/*/delete", "/vets/*/working-hours/**",
							"/specialties/**", "/specialties")
					.hasRole("STAFF")
					.requestMatchers("/vets.html", "/vets", "/vets/**")
					.authenticated()
					.requestMatchers("/owners/{ownerId}/**")
					.authenticated()
					.anyRequest()
					.authenticated())
			.formLogin(form -> form.loginPage("/login").successHandler(this.successHandler).permitAll())
			// HTTP Basic is required for PetClinicIntegrationTests which use
			// RestTemplate with basicAuthentication to test endpoints programmatically.
			.httpBasic(basic -> {
			})
			.logout(logout -> logout.logoutSuccessUrl("/login?logout").permitAll());
		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

}
