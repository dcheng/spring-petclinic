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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.VetWorkingHours;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * Tests for {@link AppointmentService}.
 */
@ExtendWith(MockitoExtension.class)
class AppointmentServiceTests {

	@Mock
	private VetRepository vetRepository;

	@Mock
	private VisitRepository visitRepository;

	@InjectMocks
	private AppointmentService appointmentService;

	private Vet vet;

	@BeforeEach
	void setup() {
		vet = new Vet();
		vet.setId(1);
		vet.setFirstName("James");
		vet.setLastName("Carter");

		// Add working hours: Monday 09:00-12:00 (6 x 30-min slots)
		VetWorkingHours hours = new VetWorkingHours();
		hours.setDayOfWeek(DayOfWeek.MONDAY);
		hours.setStartTime(LocalTime.of(9, 0));
		hours.setEndTime(LocalTime.of(12, 0));
		vet.addWorkingHours(hours);
	}

	@Test
	void getAvailableTimeSlotsReturnsAllSlotsWhenNoneBooked() {
		LocalDate monday = LocalDate.of(2026, 3, 16);
		given(vetRepository.findById(1)).willReturn(Optional.of(vet));
		given(visitRepository.findActiveVisitsByVetAndDate(1, monday)).willReturn(List.of());

		List<LocalTime> slots = appointmentService.getAvailableTimeSlots(1, monday);

		assertThat(slots).hasSize(6);
		assertThat(slots.get(0)).isEqualTo(LocalTime.of(9, 0));
		assertThat(slots.get(5)).isEqualTo(LocalTime.of(11, 30));
	}

	@Test
	void getAvailableTimeSlotsExcludesBookedSlots() {
		LocalDate monday = LocalDate.of(2026, 3, 16);
		given(vetRepository.findById(1)).willReturn(Optional.of(vet));

		Visit bookedVisit = new Visit();
		bookedVisit.setStartTime(LocalTime.of(9, 0));
		bookedVisit.setEndTime(LocalTime.of(9, 30));
		bookedVisit.setStatus("SCHEDULED");
		given(visitRepository.findActiveVisitsByVetAndDate(1, monday)).willReturn(List.of(bookedVisit));

		List<LocalTime> slots = appointmentService.getAvailableTimeSlots(1, monday);

		assertThat(slots).hasSize(5);
		assertThat(slots).doesNotContain(LocalTime.of(9, 0));
		assertThat(slots).contains(LocalTime.of(9, 30));
	}

	@Test
	void getAvailableTimeSlotsReturnsEmptyForNonWorkingDay() {
		LocalDate tuesday = LocalDate.of(2026, 3, 17);
		given(vetRepository.findById(1)).willReturn(Optional.of(vet));

		List<LocalTime> slots = appointmentService.getAvailableTimeSlots(1, tuesday);

		assertThat(slots).isEmpty();
	}

	@Test
	void getAvailableTimeSlotsReturnsEmptyForUnknownVet() {
		given(vetRepository.findById(999)).willReturn(Optional.empty());

		List<LocalTime> slots = appointmentService.getAvailableTimeSlots(999, LocalDate.now());

		assertThat(slots).isEmpty();
	}

	@Test
	void isSlotAvailableReturnsTrueWhenNoOverlap() {
		LocalDate monday = LocalDate.of(2026, 3, 16);
		given(visitRepository.findOverlappingVisits(eq(1), eq(monday), eq(LocalTime.of(9, 0)), eq(LocalTime.of(9, 30))))
			.willReturn(List.of());

		boolean available = appointmentService.isSlotAvailable(1, monday, LocalTime.of(9, 0));

		assertThat(available).isTrue();
	}

	@Test
	void isSlotAvailableReturnsFalseWhenOverlapping() {
		LocalDate monday = LocalDate.of(2026, 3, 16);
		Visit existing = new Visit();
		given(visitRepository.findOverlappingVisits(eq(1), eq(monday), eq(LocalTime.of(9, 0)), eq(LocalTime.of(9, 30))))
			.willReturn(List.of(existing));

		boolean available = appointmentService.isSlotAvailable(1, monday, LocalTime.of(9, 0));

		assertThat(available).isFalse();
	}

	@Test
	void bookAppointmentSucceedsWhenSlotAvailable() {
		LocalDate monday = LocalDate.of(2026, 3, 16);
		Visit visit = new Visit();
		visit.setDate(monday);
		visit.setStartTime(LocalTime.of(10, 0));
		visit.setVet(vet);
		visit.setDescription("Checkup");

		given(visitRepository.findOverlappingVisits(eq(1), eq(monday), eq(LocalTime.of(10, 0)),
				eq(LocalTime.of(10, 30))))
			.willReturn(List.of());
		given(visitRepository.save(any(Visit.class))).willAnswer(invocation -> invocation.getArgument(0));

		Visit result = appointmentService.bookAppointment(visit);

		assertThat(result.getEndTime()).isEqualTo(LocalTime.of(10, 30));
		assertThat(result.getStatus()).isEqualTo("SCHEDULED");
	}

	@Test
	void bookAppointmentThrowsWhenSlotTaken() {
		LocalDate monday = LocalDate.of(2026, 3, 16);
		Visit visit = new Visit();
		visit.setDate(monday);
		visit.setStartTime(LocalTime.of(10, 0));
		visit.setVet(vet);

		Visit existing = new Visit();
		given(visitRepository.findOverlappingVisits(eq(1), eq(monday), eq(LocalTime.of(10, 0)),
				eq(LocalTime.of(10, 30))))
			.willReturn(List.of(existing));

		assertThatThrownBy(() -> appointmentService.bookAppointment(visit)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("no longer available");
	}

}
