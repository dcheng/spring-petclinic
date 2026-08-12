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

import java.time.DayOfWeek;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.samples.petclinic.model.BaseEntity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlTransient;

/**
 * A single block of working hours (availability) for a {@link Vet} on a given day of the
 * week.
 *
 * @author PetClinic contributors
 */
@Entity
@Table(name = "vet_working_hours")
public class VetWorkingHours extends BaseEntity {

	@ManyToOne
	@JoinColumn(name = "vet_id")
	private Vet vet;

	@Column(name = "day_of_week")
	@Enumerated(EnumType.STRING)
	@NotNull
	private DayOfWeek dayOfWeek;

	@Column(name = "start_time")
	@DateTimeFormat(pattern = "HH:mm")
	@NotNull
	private LocalTime startTime;

	@Column(name = "end_time")
	@DateTimeFormat(pattern = "HH:mm")
	@NotNull
	private LocalTime endTime;

	@JsonIgnore
	@XmlTransient
	public Vet getVet() {
		return this.vet;
	}

	public void setVet(Vet vet) {
		this.vet = vet;
	}

	public DayOfWeek getDayOfWeek() {
		return this.dayOfWeek;
	}

	public void setDayOfWeek(DayOfWeek dayOfWeek) {
		this.dayOfWeek = dayOfWeek;
	}

	public LocalTime getStartTime() {
		return this.startTime;
	}

	public void setStartTime(LocalTime startTime) {
		this.startTime = startTime;
	}

	public LocalTime getEndTime() {
		return this.endTime;
	}

	public void setEndTime(LocalTime endTime) {
		this.endTime = endTime;
	}

}
