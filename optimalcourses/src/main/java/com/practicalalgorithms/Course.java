package com.practicalalgorithms;

import java.util.Objects;

public class Course {
    /* Add comments later */
    private String name;
    private GenEdArea GE;
    private Season[] quarters_offered;
    private int units;
    private boolean isUSCP;
    private boolean isGWR;

    public Course(String name, GenEdArea GE, Season[] quarters_offered, int units, boolean isUSCP, boolean isGWR) {
        this.name = name;
        this.GE = GE;
        this.quarters_offered = quarters_offered;
        this.units = units;
        this.isUSCP = isUSCP;
        this.isGWR = isGWR;
    }

    public String getName() {
        return this.name;
    }

    public GenEdArea getGE() {
        return this.GE;
    }

    public Season[] getQuartersOffered() {
        return this.quarters_offered.clone();
    }

    public int getUnits() {
        return this.units;
    }

    public boolean isUSCP() {
        return this.isUSCP;
    }

    public boolean isGWR() {
        return this.isGWR;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Course that = (Course) o;
        return Objects.equals(this.name, that.getName()) &&
                Objects.equals(this.GE, that.getGE());
    }
}
