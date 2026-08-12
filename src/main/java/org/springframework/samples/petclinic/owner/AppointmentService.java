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
import java.util.ArrayList;
import java.util.List;

import org.springframework.samples.petclinic.vet.Vet;
import org.springframework.samples.petclinic.vet.VetRepository;
import org.springframework.samples.petclinic.vet.VetWorkingHours;
import org.springframework.stereotype.Service;

/**
 * Service for appointment scheduling logic: time-slot generation based on vet working
 * hours and double-booking prevention.
 *
 * @author PetClinic contributors
 */
@Service
public class AppointmentService {

	private static final int SLOT_DURATION_MINUTES = 30;

	private final VetRepository vetRepository;

	private final VisitRepository visitRepository;

	public AppointmentService(VetRepository vetRepository, VisitRepository visitRepository) {
		this.vetRepository = vetRepository;
		this.visitRepository = visitRepository;
	}

	/**
	 * Returns available 30-minute time slot start times for a given vet on a given date.
	 * Slots are generated from the vet's working hours for that day of the week, minus
	 * any already-booked (non-cancelled) slots.
	 * @param vetId the vet's id
	 * @param date the date to check
	 * @return list of available slot start times
	 */
	public List<LocalTime> getAvailableTimeSlots(Integer vetId, LocalDate date) {
		Vet vet = this.vetRepository.findById(vetId).orElse(null);
		if (vet == null) {
			return List.of();
		}

		List<LocalTime> allSlots = new ArrayList<>();
		for (VetWorkingHours hours : vet.getWorkingHours()) {
			if (hours.getDayOfWeek() == date.getDayOfWeek()) {
				LocalTime slotStart = hours.getStartTime();
				while (slotStart.plusMinutes(SLOT_DURATION_MINUTES).compareTo(hours.getEndTime()) <= 0) {
					allSlots.add(slotStart);
					slotStart = slotStart.plusMinutes(SLOT_DURATION_MINUTES);
				}
			}
		}

		// Remove slots that are already booked
		List<Visit> existingVisits = this.visitRepository.findActiveVisitsByVetAndDate(vetId, date);
		List<LocalTime> availableSlots = new ArrayList<>();
		for (LocalTime slot : allSlots) {
			LocalTime slotEnd = slot.plusMinutes(SLOT_DURATION_MINUTES);
			boolean isBooked = existingVisits.stream()
				.anyMatch(v -> v.getStartTime() != null && v.getEndTime() != null && v.getStartTime().isBefore(slotEnd)
						&& v.getEndTime().isAfter(slot));
			if (!isBooked) {
				availableSlots.add(slot);
			}
		}

		return availableSlots;
	}

	/**
	 * Checks if a specific 30-minute slot is available for a given vet on a given date.
	 * @param vetId the vet's id
	 * @param date the date to check
	 * @param startTime the proposed start time
	 * @return true if the slot is available
	 */
	public boolean isSlotAvailable(Integer vetId, LocalDate date, LocalTime startTime) {
		LocalTime endTime = startTime.plusMinutes(SLOT_DURATION_MINUTES);
		List<Visit> overlapping = this.visitRepository.findOverlappingVisits(vetId, date, startTime, endTime);
		return overlapping.isEmpty();
	}

	/**
	 * Books an appointment after validating that the time slot is still available.
	 * @param visit the visit to book (must have vet, date, startTime set)
	 * @return the saved visit
	 * @throws IllegalStateException if the slot is no longer available
	 */
	public Visit bookAppointment(Visit visit) {
		if (visit.getVet() != null && visit.getStartTime() != null) {
			if (!isSlotAvailable(visit.getVet().getId(), visit.getDate(), visit.getStartTime())) {
				throw new IllegalStateException("This time slot is no longer available");
			}
			visit.setEndTime(visit.getStartTime().plusMinutes(SLOT_DURATION_MINUTES));
		}
		visit.setStatus("SCHEDULED");
		return this.visitRepository.save(visit);
	}

}
