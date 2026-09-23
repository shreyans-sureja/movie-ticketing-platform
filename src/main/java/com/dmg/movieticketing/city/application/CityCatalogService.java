package com.dmg.movieticketing.city.application;

import com.dmg.movieticketing.city.domain.City;
import com.dmg.movieticketing.city.domain.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CityCatalogService {

    private final CityRepository cityRepository;

    public CityCatalogService(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Transactional(readOnly = true)
    public List<City> list() {
        return cityRepository.findAllByOrderByNameAscIdAsc();
    }

    @Transactional(readOnly = true)
    public City get(long cityId) {
        return cityRepository.findById(cityId).orElseThrow(CityNotFoundException::new);
    }
}
