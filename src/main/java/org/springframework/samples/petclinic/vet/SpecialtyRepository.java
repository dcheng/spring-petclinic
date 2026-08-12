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

import java.util.List;
import java.util.Optional;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository class for <code>Specialty</code> domain objects.
 *
 * @author PetClinic contributors
 */
public interface SpecialtyRepository extends JpaRepository<Specialty, Integer> {

	/**
	 * Retrieve all <code>Specialty</code>s from the data store, ordered by name.
	 * @return a list of <code>Specialty</code>s
	 */
	@Transactional(readOnly = true)
	List<Specialty> findAllByOrderByNameAsc();

	/**
	 * Retrieve a <code>Specialty</code> by its (case-insensitive) name.
	 * @param name the name to search for
	 * @return the matching <code>Specialty</code>, if any
	 */
	@Transactional(readOnly = true)
	Optional<Specialty> findByNameIgnoreCase(String name);

	// Evict the 'vets' cache on specialty changes: a vet's cached representation embeds
	// its specialties, so renaming or removing one must invalidate those entries.
	@Override
	@CacheEvict(cacheNames = "vets", allEntries = true)
	<S extends Specialty> S save(S entity);

	@Override
	@CacheEvict(cacheNames = "vets", allEntries = true)
	void delete(Specialty entity);

}
