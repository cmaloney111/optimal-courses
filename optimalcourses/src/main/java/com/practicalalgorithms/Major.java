package com.practicalalgorithms;

import java.util.ArrayList;
import java.util.List;

public class Major {
    private List<Requirement> majorCourses;
    private List<Requirement> supportCourses;
    private List<GenEdArea> genEdAreas;
    private boolean isUSCPMet;
    private boolean isGWRMet;

    public Major(List<Requirement> majorCourses, List<Requirement> supportCourses, List<GenEdArea> genEdAreas) {
        this.majorCourses = new ArrayList<>(majorCourses);
        this.supportCourses = new ArrayList<>(supportCourses);
        this.genEdAreas = new ArrayList<>(genEdAreas);
        this.isUSCPMet = false; // Assuming not initially met
        this.isGWRMet = false;  // Assuming not initially met
    }

    // Getter methods
    public List<Requirement> getMajorCourses() {
        return new ArrayList<>(majorCourses); // Defensive copy to prevent external modification
    }

    public List<Requirement> getSupportCourses() {
        return new ArrayList<>(supportCourses); // Defensive copy to prevent external modification
    }

    public List<GenEdArea> getGenEdAreas() {
        return new ArrayList<>(genEdAreas); // Defensive copy to prevent external modification
    }

    public boolean isUSCPMet() {
        return isUSCPMet;
    }

    public boolean isGWRMet() {
        return isGWRMet;
    }

    // Other methods to handle major requirements
    public void markUSCPMet() {
        this.isUSCPMet = true;
    }

    public void markGWRMet() {
        this.isGWRMet = true;
    }

    // Add more methods as needed for handling major requirements
}
