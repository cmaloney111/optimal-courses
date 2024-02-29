package com.practicalalgorithms;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Arrays;

public class RequirementEntry {
    private List<Requirement> requirements;
    private List<Integer> courses;

    public RequirementEntry() {
        requirements = new ArrayList<Requirement>();
        courses = new ArrayList<Integer>();
    }

    public RequirementEntry(ArrayList<Requirement> requirements, ArrayList<Integer> courses) {
        this.requirements = new ArrayList<Requirement>();
        this.courses = new ArrayList<Integer>();
    }

    public RequirementEntry(ArrayList<Requirement> requirements, int course) {
        this.requirements = requirements;
        this.courses = Arrays.asList(course);
    }

    // Getter methods
    public List<Requirement> getRequirements() {
        return new ArrayList<>(requirements); // Defensive copy to prevent external modification
    }

    public ArrayList<Integer> getCourses() {
        return new ArrayList<>(courses);
    }

    public void addCourse(ArrayList<Requirement> req, Integer course) {
        requirements.addAll(req);
        courses.add(course);
    }

    public void setRequirements(ArrayList<Requirement> requirements) {
        this.requirements = requirements;
    }

    public void setCourses(ArrayList<Integer> courses) {
        this.courses = courses;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RequirementEntry that = (RequirementEntry) o;
        return Objects.equals(requirements, that.requirements) &&
                Objects.equals(courses, that.courses);
    }
}
