package com.ericthilen.travelbookingplatform.controller;

import com.ericthilen.travelbookingplatform.dto.AdminDepartureRequest;
import com.ericthilen.travelbookingplatform.dto.AdminRoomTypeRequest;
import com.ericthilen.travelbookingplatform.dto.AdminTravelRequest;
import com.ericthilen.travelbookingplatform.model.ManagementStatus;
import com.ericthilen.travelbookingplatform.service.AdminTravelManagementService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminTravelManagementController {

    private final AdminTravelManagementService travelManagementService;

    public AdminTravelManagementController(
            AdminTravelManagementService travelManagementService
    ) {
        this.travelManagementService = travelManagementService;
    }

    @GetMapping("/admin/resor")
    public String showTravelManagement(Model model) {
        model.addAttribute(
                "travels",
                travelManagementService.getTravels()
        );
        model.addAttribute(
                "departures",
                travelManagementService.getDepartures()
        );
        model.addAttribute(
                "roomTypes",
                travelManagementService.getRoomTypes()
        );
        model.addAttribute(
                "statuses",
                ManagementStatus.values()
        );
        model.addAttribute(
                "travelRequest",
                new AdminTravelRequest()
        );
        model.addAttribute(
                "departureRequest",
                new AdminDepartureRequest()
        );
        model.addAttribute(
                "roomTypeRequest",
                new AdminRoomTypeRequest()
        );

        return "admin-travel-management";
    }

    @PostMapping("/admin/resor")
    public String createTravel(
            @ModelAttribute AdminTravelRequest request,
            RedirectAttributes redirectAttributes
    ) {
        travelManagementService.createTravel(request);
        redirectAttributes.addFlashAttribute(
                "adminTravelMessage",
                "Resan har skapats."
        );

        return "redirect:/admin/resor";
    }

    @PostMapping("/admin/resor/{travelId}")
    public String updateTravel(
            @PathVariable Long travelId,
            @ModelAttribute AdminTravelRequest request,
            RedirectAttributes redirectAttributes
    ) {
        travelManagementService.updateTravel(
                travelId,
                request
        );
        redirectAttributes.addFlashAttribute(
                "adminTravelMessage",
                "Resan har uppdaterats."
        );

        return "redirect:/admin/resor#travels";
    }

    @PostMapping("/admin/resor/{travelId}/status")
    public String updateTravelStatus(
            @PathVariable Long travelId,
            @RequestParam ManagementStatus status,
            RedirectAttributes redirectAttributes
    ) {
        travelManagementService.updateTravelStatus(
                travelId,
                status
        );
        redirectAttributes.addFlashAttribute(
                "adminTravelMessage",
                "Resans status har uppdaterats."
        );

        return "redirect:/admin/resor#travels";
    }

    @PostMapping("/admin/avgangar")
    public String createDeparture(
            @ModelAttribute AdminDepartureRequest request,
            RedirectAttributes redirectAttributes
    ) {
        travelManagementService.createDeparture(request);
        redirectAttributes.addFlashAttribute(
                "adminTravelMessage",
                "Avgången har skapats."
        );

        return "redirect:/admin/resor#departures";
    }

    @PostMapping("/admin/avgangar/{departureId}")
    public String updateDeparture(
            @PathVariable Long departureId,
            @ModelAttribute AdminDepartureRequest request,
            RedirectAttributes redirectAttributes
    ) {
        travelManagementService.updateDeparture(
                departureId,
                request
        );
        redirectAttributes.addFlashAttribute(
                "adminTravelMessage",
                "Avgången har uppdaterats."
        );

        return "redirect:/admin/resor#departures";
    }

    @PostMapping("/admin/avgangar/{departureId}/status")
    public String updateDepartureStatus(
            @PathVariable Long departureId,
            @RequestParam ManagementStatus status,
            RedirectAttributes redirectAttributes
    ) {
        travelManagementService.updateDepartureStatus(
                departureId,
                status
        );
        redirectAttributes.addFlashAttribute(
                "adminTravelMessage",
                "Avgångens status har uppdaterats."
        );

        return "redirect:/admin/resor#departures";
    }

    @PostMapping("/admin/rumstyper")
    public String createRoomType(
            @ModelAttribute AdminRoomTypeRequest request,
            RedirectAttributes redirectAttributes
    ) {
        try {
            travelManagementService.createRoomType(request);
            redirectAttributes.addFlashAttribute(
                    "adminTravelMessage",
                    "Rumstypen har skapats."
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute(
                    "adminTravelError",
                    exception.getMessage()
            );
        }

        return "redirect:/admin/resor#rooms";
    }

    @PostMapping("/admin/rumstyper/{roomTypeId}")
    public String updateRoomType(
            @PathVariable Long roomTypeId,
            @ModelAttribute AdminRoomTypeRequest request,
            RedirectAttributes redirectAttributes
    ) {
        travelManagementService.updateRoomType(
                roomTypeId,
                request
        );
        redirectAttributes.addFlashAttribute(
                "adminTravelMessage",
                "Rumstypen har uppdaterats."
        );

        return "redirect:/admin/resor#rooms";
    }

    @PostMapping("/admin/rumstyper/{roomTypeId}/status")
    public String updateRoomTypeStatus(
            @PathVariable Long roomTypeId,
            @RequestParam ManagementStatus status,
            RedirectAttributes redirectAttributes
    ) {
        travelManagementService.updateRoomTypeStatus(
                roomTypeId,
                status
        );
        redirectAttributes.addFlashAttribute(
                "adminTravelMessage",
                "Rumstypens status har uppdaterats."
        );

        return "redirect:/admin/resor#rooms";
    }
}
