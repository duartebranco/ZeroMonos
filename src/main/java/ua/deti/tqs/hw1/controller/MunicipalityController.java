package ua.deti.tqs.hw1.controller;

import java.util.List;
import org.springframework.web.bind.annotation.*;
import ua.deti.tqs.hw1.service.MunicipalityService;

@RestController
@RequestMapping("/municipalities")
@CrossOrigin
public class MunicipalityController {

    private final MunicipalityService service;

    public MunicipalityController(MunicipalityService service) {
        this.service = service;
    }

    @GetMapping
    public List<String> getAll() {
        return service.getMunicipalities();
    }
}
