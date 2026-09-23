package com.dmg.movieticketing.city.api;

import com.dmg.movieticketing.city.application.CityCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cities")
public class CityController {

    private final CityCatalogService cityCatalogService;

    public CityController(CityCatalogService cityCatalogService) {
        this.cityCatalogService = cityCatalogService;
    }

    @GetMapping
    public CityListResponse list() {
        return new CityListResponse(cityCatalogService.list().stream().map(CityResponse::from).toList());
    }

    @GetMapping("/{cityId}")
    public CityResponse get(@PathVariable long cityId) {
        return CityResponse.from(cityCatalogService.get(cityId));
    }
}
