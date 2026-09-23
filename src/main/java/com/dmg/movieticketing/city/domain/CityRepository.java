package com.dmg.movieticketing.city.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findAllByOrderByNameAscIdAsc();
}
