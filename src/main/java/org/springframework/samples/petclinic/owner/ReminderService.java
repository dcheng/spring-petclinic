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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service that sends appointment reminders for visits scheduled for tomorrow. In a
 * production environment, this would integrate with email/SMS providers. Currently logs
 * reminders and marks visits as notified.
 *
 * @author PetClinic contributors
 */
@Service
public class ReminderService {

	private static final Logger logger = LoggerFactory.getLogger(ReminderService.class);

	private final VisitRepository visitRepository;

	public ReminderService(VisitRepository visitRepository) {
		this.visitRepository = visitRepository;
	}

	/**
	 * Sends reminders for appointments scheduled for tomorrow. Runs daily at 9:00 AM.
	 */
	@Scheduled(cron = "0 0 9 * * *")
	public void sendReminders() {
		LocalDate tomorrow = LocalDate.now().plusDays(1);
		List<Visit> visits = this.visitRepository.findByStatusAndDateAndNotificationSentFalse("SCHEDULED", tomorrow);

		for (Visit visit : visits) {
			String vetName = visit.getVet() != null ? visit.getVet().getFirstName() + " " + visit.getVet().getLastName()
					: "unassigned";
			String timeSlot = visit.getStartTime() != null ? visit.getStartTime() + "-" + visit.getEndTime()
					: "no time set";

			logger.info("REMINDER: Appointment on {} at {} with Dr. {} for '{}' (Visit ID: {})", visit.getDate(),
					timeSlot, vetName, visit.getDescription(), visit.getId());

			visit.setNotificationSent(true);
			this.visitRepository.save(visit);
		}

		if (!visits.isEmpty()) {
			logger.info("Sent {} appointment reminder(s) for {}", visits.size(), tomorrow);
		}
	}

}
