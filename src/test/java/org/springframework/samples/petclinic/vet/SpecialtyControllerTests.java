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

package org.springframework.samples.petclinic.vet;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Test class for the {@link SpecialtyController}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisabledInNativeImage
@DisabledInAotMode
class SpecialtyControllerTests {

	private static final int TEST_SPECIALTY_ID = 1;

	private static final User STAFF_USER = new User("staff", "password", true, true, true, true,
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")));

	private static final User VET_USER = new User("vet", "password", true, true, true, true,
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_VET")));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private SpecialtyRepository specialties;

	@MockitoBean
	private VetRepository vets;

	@MockitoBean
	private UserRepository userRepository;

	private Specialty radiology() {
		Specialty radiology = new Specialty();
		radiology.setId(TEST_SPECIALTY_ID);
		radiology.setName("radiology");
		return radiology;
	}

	@BeforeEach
	void setup() {
		given(this.specialties.findAllByOrderByNameAsc()).willReturn(List.of(radiology()));
		given(this.specialties.findById(TEST_SPECIALTY_ID)).willReturn(Optional.of(radiology()));
		given(this.specialties.findByNameIgnoreCase(any())).willReturn(Optional.empty());
		given(this.vets.findBySpecialtiesId(any())).willReturn(Lists.newArrayList());
	}

	@Test
	void showSpecialtyList() throws Exception {
		mockMvc.perform(get("/specialties").with(user(STAFF_USER)))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("specialties"))
			.andExpect(view().name("specialties/specialtyList"));
	}

	@Test
	void initCreationForm() throws Exception {
		mockMvc.perform(get("/specialties/new").with(user(STAFF_USER)))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("specialty"))
			.andExpect(view().name("specialties/createOrUpdateSpecialtyForm"));
	}

	@Test
	void processCreationFormSuccess() throws Exception {
		mockMvc.perform(post("/specialties/new").with(user(STAFF_USER)).with(csrf()).param("name", "surgery"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/specialties"));
		verify(this.specialties).save(any(Specialty.class));
	}

	@Test
	void processCreationFormDuplicateName() throws Exception {
		given(this.specialties.findByNameIgnoreCase("radiology")).willReturn(Optional.of(radiology()));
		mockMvc.perform(post("/specialties/new").with(user(STAFF_USER)).with(csrf()).param("name", "radiology"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("specialty", "name"))
			.andExpect(view().name("specialties/createOrUpdateSpecialtyForm"));
	}

	@Test
	void processUpdateFormSuccess() throws Exception {
		mockMvc
			.perform(post("/specialties/{id}/edit", TEST_SPECIALTY_ID).with(user(STAFF_USER))
				.with(csrf())
				.param("name", "radiology-2"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/specialties"));
		verify(this.specialties).save(any(Specialty.class));
	}

	@Test
	void deleteSpecialty() throws Exception {
		mockMvc.perform(post("/specialties/{id}/delete", TEST_SPECIALTY_ID).with(user(STAFF_USER)).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/specialties"));
		verify(this.specialties).delete(any(Specialty.class));
	}

	@Test
	void managementIsForbiddenForNonStaff() throws Exception {
		mockMvc.perform(get("/specialties").with(user(VET_USER))).andExpect(status().isForbidden());
	}

}
