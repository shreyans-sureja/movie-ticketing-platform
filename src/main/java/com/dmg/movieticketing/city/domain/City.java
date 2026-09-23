package com.dmg.movieticketing.city.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "city")
public class City {

    @Id
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "state_or_ut", nullable = false, length = 120)
    private String stateOrUt;

    protected City() {
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getStateOrUt() {
        return stateOrUt;
    }
}
