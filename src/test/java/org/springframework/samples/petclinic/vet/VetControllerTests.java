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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.samples.petclinic.security.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Test class for the {@link VetController}
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisabledInNativeImage
@DisabledInAotMode
class VetControllerTests {

	private static final User STAFF_USER = new User("staff", "password", true, true, true, true,
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")));

	private static final User VET_USER = new User("vet", "password", true, true, true, true,
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_VET")));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private VetRepository vets;

	@MockitoBean
	private SpecialtyRepository specialties;

	@MockitoBean
	private UserRepository userRepository;

	private Specialty radiology() {
		Specialty radiology = new Specialty();
		radiology.setId(1);
		radiology.setName("radiology");
		return radiology;
	}

	private Vet james() {
		Vet james = new Vet();
		james.setFirstName("James");
		james.setLastName("Carter");
		james.setId(1);
		return james;
	}

	private Vet helen() {
		Vet helen = new Vet();
		helen.setFirstName("Helen");
		helen.setLastName("Leary");
		helen.setId(2);
		helen.addSpecialty(radiology());
		return helen;
	}

	@BeforeEach
	void setup() {
		given(this.vets.findAll()).willReturn(Lists.newArrayList(james(), helen()));
		given(this.vets.findAll(any(Pageable.class)))
			.willReturn(new PageImpl<Vet>(Lists.newArrayList(james(), helen())));
		given(this.vets.findById(1)).willReturn(Optional.of(james()));
		given(this.vets.save(any(Vet.class))).willAnswer(invocation -> {
			Vet v = invocation.getArgument(0);
			if (v.getId() == null) {
				v.setId(99);
			}
			return v;
		});
		given(this.specialties.findAllByOrderByNameAsc()).willReturn(List.of(radiology()));
		given(this.specialties.findByNameIgnoreCase("radiology")).willReturn(Optional.of(radiology()));
	}

	@Test
	void showVetListHtml() throws Exception {
		mockMvc.perform(get("/vets.html?page=1").with(user(STAFF_USER)))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("listVets"))
			.andExpect(view().name("vets/vetList"));
	}

	@Test
	void showResourcesVetList() throws Exception {
		ResultActions actions = mockMvc.perform(get("/vets").with(user(STAFF_USER)).accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk());
		actions.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.vetList[0].id").value(1));
	}

	@Test
	void showVetDetails() throws Exception {
		mockMvc.perform(get("/vets/{vetId}", 1).with(user(STAFF_USER)))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("vet"))
			.andExpect(view().name("vets/vetDetails"));
	}

	@Test
	void initCreationForm() throws Exception {
		mockMvc.perform(get("/vets/new").with(user(STAFF_USER)))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("vet"))
			.andExpect(view().name("vets/createOrUpdateVetForm"));
	}

	@Test
	void processCreationFormSuccess() throws Exception {
		mockMvc
			.perform(post("/vets/new").with(user(STAFF_USER))
				.with(csrf())
				.param("firstName", "Sam")
				.param("lastName", "Schultz")
				.param("specialties", "radiology"))
			.andExpect(status().is3xxRedirection());
		verify(this.vets).save(any(Vet.class));
	}

	@Test
	void processUpdateFormSuccess() throws Exception {
		mockMvc
			.perform(post("/vets/{vetId}/edit", 1).with(user(STAFF_USER))
				.with(csrf())
				.param("firstName", "James")
				.param("lastName", "Carter-Smith"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/vets/1"));
		verify(this.vets).save(any(Vet.class));
	}

	@Test
	void deleteVet() throws Exception {
		mockMvc.perform(post("/vets/{vetId}/delete", 1).with(user(STAFF_USER)).with(csrf()))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/vets.html"));
		verify(this.vets).delete(any(Vet.class));
	}

	@Test
	void addWorkingHours() throws Exception {
		mockMvc
			.perform(post("/vets/{vetId}/working-hours/new", 1).with(user(STAFF_USER))
				.with(csrf())
				.param("dayOfWeek", "MONDAY")
				.param("startTime", "09:00")
				.param("endTime", "17:00"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/vets/1"));
		verify(this.vets).save(any(Vet.class));
	}

	@Test
	void addWorkingHoursRejectsInvalidRange() throws Exception {
		mockMvc
			.perform(post("/vets/{vetId}/working-hours/new", 1).with(user(STAFF_USER))
				.with(csrf())
				.param("dayOfWeek", "MONDAY")
				.param("startTime", "17:00")
				.param("endTime", "09:00"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/vets/1"));
	}

	@Test
	void managementIsForbiddenForNonStaff() throws Exception {
		mockMvc.perform(get("/vets/new").with(user(VET_USER))).andExpect(status().isForbidden());
	}

}
