package ua.deti.tqs.hw1.controller;

import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ua.deti.tqs.hw1.model.Booking;
import ua.deti.tqs.hw1.service.BookingService;
import ua.deti.tqs.hw1.service.MunicipalityService;

/* Simple controller that serves Thymeleaf views for the application. */
@Controller
public class ViewController {

    private final MunicipalityService municipalityService;
    private final BookingService bookingService;

    public ViewController(
        MunicipalityService municipalityService,
        BookingService bookingService
    ) {
        this.municipalityService = municipalityService;
        this.bookingService = bookingService;
    }

    @GetMapping("/")
    public String index() {
        // Landing chooser page (no municipalities required)
        return "index";
    }

    @GetMapping("/citizen")
    public String citizen() {
        // Redirect legacy /citizen to the new reserve page
        return "redirect:/citizen/reserve";
    }

    @GetMapping("/citizen/reserve")
    public String citizenReserve(Model model) {
        // Provide municipalities for the booking form
        model.addAttribute(
            "municipalities",
            municipalityService.getMunicipalities()
        );

        // Provide item types for the booking form
        model.addAttribute("itemTypes", Booking.ItemType.getAllTypes());

        // Inform the layout to show only citizen nav (Reservar / Consultar)
        model.addAttribute("citizenOnlyNav", true);
        return "citizen-reserve";
    }

    @GetMapping("/citizen/check")
    public String citizenCheck(Model model) {
        // Serve the token lookup page (no municipalities required)
        // Inform the layout to show only citizen nav (Reservar / Consultar)
        model.addAttribute("citizenOnlyNav", true);
        return "citizen-check";
    }

    @GetMapping("/staff")
    public String staff(
        Model model,
        @RequestParam(required = false) String municipality,
        @RequestParam(required = false) @DateTimeFormat(
            iso = DateTimeFormat.ISO.DATE
        ) LocalDate date
    ) {
        List<Booking> bookings;

        // Apply filters if provided
        if (municipality != null && !municipality.isEmpty() && date != null) {
            bookings = bookingService.getByDateAndMunicipality(
                date,
                municipality
            );
        } else if (date != null) {
            bookings = bookingService.getByDate(date);
        } else if (municipality != null && !municipality.isEmpty()) {
            bookings = bookingService.getByMunicipality(municipality);
        } else {
            bookings = bookingService.getAll();
        }

        model.addAttribute("bookings", bookings);
        model.addAttribute(
            "municipalities",
            municipalityService.getMunicipalities()
        );
        model.addAttribute("selectedMunicipality", municipality);
        model.addAttribute("selectedDate", date);

        return "staff";
    }
}
