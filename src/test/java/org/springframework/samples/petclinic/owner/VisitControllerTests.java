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

package org.springframework.samples.petclinic.owner;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.samples.petclinic.security.PetClinicUserDetails;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Test class for {@link VisitController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@DisabledInNativeImage
@DisabledInAotMode
class VisitControllerTests {

	private static final int TEST_OWNER_ID = 1;

	private static final int TEST_PET_ID = 1;

	private static final User STAFF_USER = new User("staff", "password", true, true, true, true,
			Collections.singletonList(new SimpleGrantedAuthority("ROLE_STAFF")));

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private VetRepository vetRepository;

	@MockitoBean
	private VisitRepository visitRepository;

	@MockitoBean
	private AppointmentService appointmentService;

	@BeforeEach
	void init() {
		Owner owner = new Owner();
		Pet pet = new Pet();
		owner.addPet(pet);
		pet.setId(TEST_PET_ID);
		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(owner));
		given(this.vetRepository.findAll()).willReturn(List.of());
	}

	@Test
	void initNewVisitForm() throws Exception {
		mockMvc
			.perform(
					get("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID).with(user(STAFF_USER)))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"));
	}

	@Test
	void processNewVisitFormSuccess() throws Exception {
		mockMvc
			.perform(
					post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID).with(user(STAFF_USER))
						.with(csrf())
						.param("name", "George")
						.param("date", LocalDate.now().plusDays(1).toString())
						.param("description", "Visit Description"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void processNewVisitFormWithVetAndTimeSlot() throws Exception {
		Vet vet = new Vet();
		vet.setId(1);
		vet.setFirstName("James");
		vet.setLastName("Carter");
		given(this.vetRepository.findById(1)).willReturn(Optional.of(vet));
		given(this.appointmentService.isSlotAvailable(eq(1), any(LocalDate.class), eq(LocalTime.of(9, 0))))
			.willReturn(true);

		mockMvc
			.perform(
					post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID).with(user(STAFF_USER))
						.with(csrf())
						.param("date", LocalDate.now().plusDays(1).toString())
						.param("description", "Annual checkup")
						.param("vetId", "1")
						.param("startTime", "09:00"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void processNewVisitFormRejectsDoubleBooking() throws Exception {
		Vet vet = new Vet();
		vet.setId(1);
		given(this.vetRepository.findById(1)).willReturn(Optional.of(vet));
		given(this.appointmentService.isSlotAvailable(eq(1), any(LocalDate.class), eq(LocalTime.of(9, 0))))
			.willReturn(false);

		mockMvc
			.perform(
					post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID).with(user(STAFF_USER))
						.with(csrf())
						.param("date", LocalDate.now().plusDays(1).toString())
						.param("description", "Checkup")
						.param("vetId", "1")
						.param("startTime", "09:00"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasFieldErrors("visit", "startTime"))
			.andExpect(view().name("pets/createOrUpdateVisitForm"));
	}

	@Test
	void processNewVisitFormHasErrors() throws Exception {
		mockMvc
			.perform(
					post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID).with(user(STAFF_USER))
						.with(csrf())
						.param("name", "George"))
			.andExpect(model().attributeHasErrors("visit"))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"));
	}

	@Test
	void processNewVisitFormHasErrorsWhenVisitDateIsNotInFuture() throws Exception {
		mockMvc
			.perform(
					post("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID).with(user(STAFF_USER))
						.with(csrf())
						.param("name", "George")
						.param("date", LocalDate.now().toString())
						.param("description", "Visit Description"))
			.andExpect(model().attributeHasFieldErrors("visit", "date"))
			.andExpect(model().attributeHasFieldErrorCode("visit", "date", "typeMismatch.visitDate"))
			.andExpect(status().isOk())
			.andExpect(view().name("pets/createOrUpdateVisitForm"));
	}

	@Test
	void getAvailableSlotsReturnsJson() throws Exception {
		given(this.appointmentService.getAvailableTimeSlots(1, LocalDate.of(2026, 3, 16)))
			.willReturn(List.of(LocalTime.of(9, 0), LocalTime.of(9, 30), LocalTime.of(10, 0)));

		mockMvc
			.perform(get("/owners/{ownerId}/pets/{petId}/visits/available-slots", TEST_OWNER_ID, TEST_PET_ID)
				.with(user(STAFF_USER))
				.param("vetId", "1")
				.param("date", "2026-03-16"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0]").value("09:00"))
			.andExpect(jsonPath("$[1]").value("09:30"))
			.andExpect(jsonPath("$[2]").value("10:00"));
	}

	@Test
	void cancelVisitChangesStatus() throws Exception {
		Visit visit = new Visit();
		visit.setId(10);
		visit.setStatus("SCHEDULED");
		given(this.visitRepository.findById(10)).willReturn(Optional.of(visit));

		mockMvc
			.perform(post("/owners/{ownerId}/pets/{petId}/visits/{visitId}/cancel", TEST_OWNER_ID, TEST_PET_ID, 10)
				.with(user(STAFF_USER))
				.with(csrf()))
			.andExpect(status().is3xxRedirection());
	}

	@Nested
	class AuthorizationTests {

		@Test
		void ownerCanAddVisitToOwnPet() throws Exception {
			PetClinicUserDetails ownerUser = new PetClinicUserDetails("owner_george", "password", true,
					Collections.singletonList(new SimpleGrantedAuthority("ROLE_OWNER")), TEST_OWNER_ID);

			mockMvc
				.perform(get("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
					.with(user(ownerUser)))
				.andExpect(status().isOk());
		}

		@Test
		void ownerCannotAddVisitToOtherOwnerPet() throws Exception {
			PetClinicUserDetails ownerUser = new PetClinicUserDetails("owner_betty", "password", true,
					Collections.singletonList(new SimpleGrantedAuthority("ROLE_OWNER")), 2);

			mockMvc
				.perform(get("/owners/{ownerId}/pets/{petId}/visits/new", TEST_OWNER_ID, TEST_PET_ID)
					.with(user(ownerUser)))
				.andExpect(status().isForbidden());
		}

	}

}
