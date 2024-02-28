package com.practicalalgorithms;

public class Course {
    /* Add comments later */
    private String name;
    private GenEdArea GE;
    private Season[] quarters_offered;
    private int units;
    private boolean isUSCP;
    private boolean isGWR;
    private boolean taken;

    public Course(String name, GenEdArea GE, Season[] quarters_offered, int units, boolean isUSCP, boolean isGWR) {
        this.name = name;
        this.GE = GE;
        this.quarters_offered = quarters_offered;
        this.units = units;
        this.isUSCP = isUSCP;
        this.isGWR = isGWR;
        this.taken = false;
    }

    public String getName() {
        return this.name;
    }

    public GenEdArea getGE() {
        return this.GE;
    }

    public Season[] getQuartersOffered() {
        return this.quarters_offered.clone();  // returning a clone to prevent external modification
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

    public boolean getTaken() {
        return this.taken;
    }

    public void takeCourse() {
        this.taken = true;
    }
}
