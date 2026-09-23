package com.dmg.movieticketing.city.api;

import com.dmg.movieticketing.city.domain.City;

public record CityResponse(long id, String name, String stateOrUt) {

    public static CityResponse from(City city) {
        return new CityResponse(city.getId(), city.getName(), city.getStateOrUt());
    }
}
