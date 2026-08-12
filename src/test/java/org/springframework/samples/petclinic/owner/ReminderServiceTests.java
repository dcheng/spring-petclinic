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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.vet.Vet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link ReminderService}.
 */
@ExtendWith(MockitoExtension.class)
class ReminderServiceTests {

	@Mock
	private VisitRepository visitRepository;

	@InjectMocks
	private ReminderService reminderService;

	@Test
	void sendRemindersMarksVisitsAsNotified() {
		LocalDate tomorrow = LocalDate.now().plusDays(1);

		Vet vet = new Vet();
		vet.setFirstName("James");
		vet.setLastName("Carter");

		Visit visit = new Visit();
		visit.setId(1);
		visit.setDate(tomorrow);
		visit.setStartTime(LocalTime.of(9, 0));
		visit.setEndTime(LocalTime.of(9, 30));
		visit.setVet(vet);
		visit.setDescription("Annual checkup");
		visit.setStatus("SCHEDULED");
		visit.setNotificationSent(false);

		given(visitRepository.findByStatusAndDateAndNotificationSentFalse("SCHEDULED", tomorrow))
			.willReturn(List.of(visit));
		given(visitRepository.save(any(Visit.class))).willAnswer(invocation -> invocation.getArgument(0));

		reminderService.sendReminders();

		assertThat(visit.isNotificationSent()).isTrue();
		verify(visitRepository, times(1)).save(visit);
	}

	@Test
	void sendRemindersDoesNothingWhenNoVisitsFound() {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		given(visitRepository.findByStatusAndDateAndNotificationSentFalse(eq("SCHEDULED"), eq(tomorrow)))
			.willReturn(List.of());

		reminderService.sendReminders();

		verify(visitRepository, times(0)).save(any(Visit.class));
	}

}
