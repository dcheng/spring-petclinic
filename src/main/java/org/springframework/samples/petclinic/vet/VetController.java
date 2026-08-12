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
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Ken Krebs
 * @author Arjen Poutsma
 */
@Controller
class VetController {

	private static final String VIEWS_VET_CREATE_OR_UPDATE_FORM = "vets/createOrUpdateVetForm";

	private final VetRepository vetRepository;

	private final SpecialtyRepository specialtyRepository;

	public VetController(VetRepository vetRepository, SpecialtyRepository specialtyRepository) {
		this.vetRepository = vetRepository;
		this.specialtyRepository = specialtyRepository;
	}

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setDisallowedFields("id");
	}

	@ModelAttribute("allSpecialties")
	public List<Specialty> populateSpecialties() {
		return this.specialtyRepository.findAllByOrderByNameAsc();
	}

	@ModelAttribute("daysOfWeek")
	public DayOfWeek[] populateDaysOfWeek() {
		return DayOfWeek.values();
	}

	@GetMapping("/vets.html")
	public String showVetList(@RequestParam(defaultValue = "1") int page, Model model) {
		Page<Vet> paginated = findPaginated(page);
		return addPaginationModel(page, paginated, model);
	}

	private String addPaginationModel(int page, Page<Vet> paginated, Model model) {
		List<Vet> listVets = paginated.getContent();
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPages", paginated.getTotalPages());
		model.addAttribute("totalItems", paginated.getTotalElements());
		model.addAttribute("listVets", listVets);
		return "vets/vetList";
	}

	private Page<Vet> findPaginated(int page) {
		int pageSize = 5;
		Pageable pageable = PageRequest.of(page - 1, pageSize);
		return vetRepository.findAll(pageable);
	}

	@GetMapping({ "/vets" })
	public @ResponseBody Vets showResourcesVetList() {
		// Here we are returning an object of type 'Vets' rather than a collection of Vet
		// objects so it is simpler for JSon/Object mapping
		Vets vets = new Vets();
		vets.getVetList().addAll(this.vetRepository.findAll());
		return vets;
	}

	// --- CRUD for individual vets -------------------------------------------------

	private Vet loadVet(int vetId) {
		return this.vetRepository.findById(vetId)
			.orElseThrow(() -> new IllegalArgumentException("Vet not found with id: " + vetId
					+ ". Please ensure the ID is correct and the vet exists in the database."));
	}

	@GetMapping("/vets/{vetId}")
	public String showVet(@PathVariable("vetId") int vetId, Model model) {
		model.addAttribute("vet", loadVet(vetId));
		return "vets/vetDetails";
	}

	@GetMapping("/vets/new")
	public String initCreationForm(Model model) {
		model.addAttribute("vet", new Vet());
		return VIEWS_VET_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/vets/new")
	public String processCreationForm(@Valid Vet vet, BindingResult result, RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in creating the veterinarian.");
			return VIEWS_VET_CREATE_OR_UPDATE_FORM;
		}
		Vet saved = this.vetRepository.save(vet);
		redirectAttributes.addFlashAttribute("message", "New Veterinarian Created");
		return "redirect:/vets/" + saved.getId();
	}

	@GetMapping("/vets/{vetId}/edit")
	public String initUpdateForm(@PathVariable("vetId") int vetId, Model model) {
		model.addAttribute("vet", loadVet(vetId));
		return VIEWS_VET_CREATE_OR_UPDATE_FORM;
	}

	@PostMapping("/vets/{vetId}/edit")
	public String processUpdateForm(@Valid Vet vet, BindingResult result, @PathVariable("vetId") int vetId,
			RedirectAttributes redirectAttributes) {
		if (result.hasErrors()) {
			redirectAttributes.addFlashAttribute("error", "There was an error in updating the veterinarian.");
			return VIEWS_VET_CREATE_OR_UPDATE_FORM;
		}

		// Update the managed entity in place so its working hours are preserved. Only the
		// name and specialties are editable from this form.
		Vet existing = loadVet(vetId);
		existing.setFirstName(vet.getFirstName());
		existing.setLastName(vet.getLastName());
		existing.clearSpecialties();
		vet.getSpecialties().forEach(existing::addSpecialty);
		this.vetRepository.save(existing);

		redirectAttributes.addFlashAttribute("message", "Veterinarian Values Updated");
		return "redirect:/vets/" + vetId;
	}

	@PostMapping("/vets/{vetId}/delete")
	public String deleteVet(@PathVariable("vetId") int vetId, RedirectAttributes redirectAttributes) {
		Vet vet = loadVet(vetId);
		this.vetRepository.delete(vet);
		redirectAttributes.addFlashAttribute("message", "Veterinarian Deleted");
		return "redirect:/vets.html";
	}

	// --- Working hours / availability ---------------------------------------------

	@PostMapping("/vets/{vetId}/working-hours/new")
	public String addWorkingHours(@PathVariable("vetId") int vetId, @RequestParam("dayOfWeek") DayOfWeek dayOfWeek,
			@RequestParam("startTime") @org.springframework.format.annotation.DateTimeFormat(
					pattern = "HH:mm") LocalTime startTime,
			@RequestParam("endTime") @org.springframework.format.annotation.DateTimeFormat(
					pattern = "HH:mm") LocalTime endTime,
			RedirectAttributes redirectAttributes) {
		Vet vet = loadVet(vetId);
		if (!endTime.isAfter(startTime)) {
			redirectAttributes.addFlashAttribute("error", "End time must be after start time.");
			return "redirect:/vets/" + vetId;
		}
		VetWorkingHours hours = new VetWorkingHours();
		hours.setDayOfWeek(dayOfWeek);
		hours.setStartTime(startTime);
		hours.setEndTime(endTime);
		vet.addWorkingHours(hours);
		this.vetRepository.save(vet);
		redirectAttributes.addFlashAttribute("message", "Working hours added");
		return "redirect:/vets/" + vetId;
	}

	@PostMapping("/vets/{vetId}/working-hours/{hoursId}/delete")
	public String deleteWorkingHours(@PathVariable("vetId") int vetId, @PathVariable("hoursId") int hoursId,
			RedirectAttributes redirectAttributes) {
		Vet vet = loadVet(vetId);
		vet.getWorkingHours()
			.stream()
			.filter(h -> Objects.equals(h.getId(), hoursId))
			.findFirst()
			.ifPresent(vet::removeWorkingHours);
		this.vetRepository.save(vet);
		redirectAttributes.addFlashAttribute("message", "Working hours removed");
		return "redirect:/vets/" + vetId;
	}

}
