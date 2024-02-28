package com.practicalalgorithms;

import java.util.List;
import java.util.ArrayList;

public class Requirement {
    private String requirementName;
    private List<Course> necessaryCourses;
    private int num; // Number of units needed in requirement
    private Boolean fulfilled;

    public Requirement(String requirementName, ArrayList<Course> necessaryCourses, int num) {
        this.requirementName = requirementName;
        this.necessaryCourses = necessaryCourses;
        this.num = num;
        fulfilled = false;
    }

    public String getRequirementName() {
        return this.requirementName;
    }

    public List<Course> getNecessaryCourses() {
        return necessaryCourses;
    }

    public int getNum() {
        return num;
    }

    // Method to check if a given list of courses fulfills the requirement
    public int isRequirementFulfilled(List<Course> courses) {
        if (fulfilled) {
            return 0;
        }
        int unitCount = 0;
        for (Course course : courses) {
            if (necessaryCourses.contains(course)) {
                unitCount += course.getUnits();
                if (unitCount >= this.num) {
                    fulfilled = true;
                    return 0;
                }
            }
        }
        return this.num - unitCount;
    }
}

