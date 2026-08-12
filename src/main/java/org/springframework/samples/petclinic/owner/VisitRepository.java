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

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository class for {@link Visit} domain objects. Provides methods for appointment
 * scheduling, double-booking detection, and reminder queries.
 *
 * @author PetClinic contributors
 */
public interface VisitRepository extends JpaRepository<Visit, Integer> {

	/**
	 * Find visits that overlap with a proposed time slot for a given vet on a given date.
	 * Only considers active (non-cancelled) visits.
	 * @param vetId the vet's id
	 * @param date the appointment date
	 * @param startTime the proposed start time
	 * @param endTime the proposed end time
	 * @return list of overlapping visits
	 */
	@Transactional(readOnly = true)
	@Query("SELECT v FROM Visit v WHERE v.vet.id = :vetId AND v.date = :date "
			+ "AND v.startTime < :endTime AND v.endTime > :startTime " + "AND v.status <> 'CANCELLED'")
	List<Visit> findOverlappingVisits(@Param("vetId") Integer vetId, @Param("date") LocalDate date,
			@Param("startTime") LocalTime startTime, @Param("endTime") LocalTime endTime);

	/**
	 * Find all active visits for a given vet on a specific date.
	 * @param vetId the vet's id
	 * @param date the date to query
	 * @return list of non-cancelled visits for the vet on that date
	 */
	@Transactional(readOnly = true)
	@Query("SELECT v FROM Visit v WHERE v.vet.id = :vetId AND v.date = :date AND v.status <> 'CANCELLED'")
	List<Visit> findActiveVisitsByVetAndDate(@Param("vetId") Integer vetId, @Param("date") LocalDate date);

	/**
	 * Find scheduled visits for a given date that have not yet been notified.
	 * @param date the date to check
	 * @return list of visits needing reminder notification
	 */
	@Transactional(readOnly = true)
	List<Visit> findByStatusAndDateAndNotificationSentFalse(String status, LocalDate date);

}
