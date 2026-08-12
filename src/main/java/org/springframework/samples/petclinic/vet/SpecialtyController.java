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

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * Controller providing CRUD management of veterinarian {@link Specialty} entities.
 *
 * @author PetClinic contributors
 */
@Controller
class SpecialtyController {

	private static final String VIEWS_SPECIALTY_CREATE_OR_UPDATE_FORM = "specialties/createOrUpdateSpecialtyForm";

	private final SpecialtyRepository specialties;

	private final VetRepository vets;

	public SpecialtyController(SpecialtyRepository specialties, VetRepository vets) {
		this.specialties = specialties;
		this.vets = vets;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@GetMapping("/specialties")
	public String showSpecialtyList(Model model) {
		model.addAttribute("specialties", this.specialties.findAllByOrderByNameAsc());
		return "specialties/specialtyList";
	}

	@GetMapping("/specialties/new")
	public String initCreationForm(Model model) {
		model.addAttribute("specialty", new Specialty());
		return VIEWS_SPECIALTY_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/specialties/new")
	public String processCreationForm(@Valid @ModelAttribute("specialty") Specialty specialty, BindingResult result,
			RedirectAttributes redirectAttributes) {
		validateName(specialty, null, result);
		if (result.hasErrors()) {
			return VIEWS_SPECIALTY_CREATE_OR_UPDATE_FORM;
		}
		this.specialties.save(specialty);
		redirectAttributes.addFlashAttribute("message", "New Specialty Created");
		return "redirect:/specialties";
	}

	@GetMapping("/specialties/{specialtyId}/edit")
	public String initUpdateForm(@PathVariable("specialtyId") int specialtyId, Model model) {
		model.addAttribute("specialty", loadSpecialty(specialtyId));
		return VIEWS_SPECIALTY_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/specialties/{specialtyId}/edit")
	public String processUpdateForm(@Valid @ModelAttribute("specialty") Specialty specialty, BindingResult result,
			@PathVariable("specialtyId") int specialtyId, RedirectAttributes redirectAttributes) {
		validateName(specialty, specialtyId, result);
		if (result.hasErrors()) {
			return VIEWS_SPECIALTY_CREATE_OR_UPDATE_FORM;
		}
		Specialty existing = loadSpecialty(specialtyId);
		existing.setName(specialty.getName());
		this.specialties.save(existing);
		redirectAttributes.addFlashAttribute("message", "Specialty Updated");
		return "redirect:/specialties";
	}

	@PostMapping("/specialties/{specialtyId}/delete")
	@Transactional
	public String deleteSpecialty(@PathVariable("specialtyId") int specialtyId, RedirectAttributes redirectAttributes) {
		Specialty specialty = loadSpecialty(specialtyId);
		detachFromVets(specialtyId);
		this.specialties.delete(specialty);
		redirectAttributes.addFlashAttribute("message", "Specialty Deleted");
		return "redirect:/specialties";
	}

	/**
	 * Removes the given specialty from every vet that references it so the join rows are
	 * cleared before the specialty itself is deleted (avoids a foreign-key violation).
	 * @param specialtyId the specialty being removed
	 */
	private void detachFromVets(int specialtyId) {
		for (Vet vet : this.vets.findBySpecialtiesId(specialtyId)) {
			var remaining = vet.getSpecialties().stream().filter(s -> !s.getId().equals(specialtyId)).toList();
			vet.clearSpecialties();
			remaining.forEach(vet::addSpecialty);
			this.vets.save(vet);
		}
	}

	private Specialty loadSpecialty(int specialtyId) {
		return this.specialties.findById(specialtyId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
					"Specialty not found with id: " + specialtyId));
	}

	private void validateName(Specialty specialty, Integer selfId, BindingResult result) {
		if (!StringUtils.hasText(specialty.getName())) {
			return; // handled by @NotBlank
		}
		Optional<Specialty> existing = this.specialties.findByNameIgnoreCase(specialty.getName().strip());
		if (existing.isPresent() && !existing.get().getId().equals(selfId)) {
			result.rejectValue("name", "duplicate", "is already in use");
		}
	}

}
