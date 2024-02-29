package com.practicalalgorithms;

import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static void whatFulfillsIt(Set<Requirement> requirements, List<Course> courses) {
        for (Requirement req : requirements) {
            ArrayList<String> courseNames = new ArrayList<String>();
            int unitsFulfilled = 0;
            for (Course course : courses) {
                if (req.getNecessaryCourses().contains(course)) {
                    courseNames.add(course.getName());
                    unitsFulfilled += course.getUnits();
                }
            }
            if (unitsFulfilled >= req.getNum()) {
                System.out.print(req.getRequirementName() + ": ");
                for (String courseName : courseNames) {
                    System.out.print(courseName.substring(0, courseName.indexOf(".")) + ", ");
                }
                System.out.println("");
            }
        }
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

