package com.practicalalgorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class CourseSelector {
    private static Map<String, List<String>> cscRequirements = new HashMap<>();
    private static Map<String, List<String>> spanRequirements = new HashMap<>();
    public static void main(String args[]) {
        CourseScraper cs = new CourseScraper();
        cs.populateCoursesByPrefix();
        List<Requirement> requirements = initializeRequirements(cs);
        ArrayList<String> coursesTakenByCode = populateCoursesTakenByCode();
        ArrayList<Course> coursesTaken = populateCoursesTaken(cs, coursesTakenByCode);

        Map<Requirement, Integer> requirementsNotMet = new HashMap<>();
        for (Requirement requirement: requirements) {
            if (requirement.isRequirementFulfilled(coursesTaken) == 0) {
                //System.out.println("Requirement fulfilled: " + requirement.getRequirementName());
            }
            else {
                requirementsNotMet.put(requirement, requirement.isRequirementFulfilled(coursesTaken));
                //System.out.println("Requirement not fulfilled: " + requirement.getRequirementName() + ", " + requirement.isRequirementFulfilled(coursesTaken) + " units left");
            }
        }

        findCoursesNaiveMethod(requirementsNotMet);

        // Example: Print course names
        /*for (Requirement requirement : requirements) {
            for (Course course : requirement.getNecessaryCourses()) {
                System.out.println(course.getName());
            }
        }*/
    }    


    private static List<Requirement> initializeRequirements(CourseScraper cs) {
        List<Requirement> requirements = new ArrayList<>();

        Map<String, List<Course>> coursesByPrefix = cs.getCoursesByPrefix();

        /* for (Course course : coursesByPrefix.get("csc")) {
            System.out.println(course.getName());
        } */
        Map<String, Course> cscCourses = cs.getCoursesFromMajor("https://catalog.calpoly.edu/collegesandprograms/collegeofengineering/computersciencesoftwareengineering/bscomputerscience/");
        Map<String, Course> spanCourses = cs.getCoursesFromMajor("https://catalog.calpoly.edu/collegesandprograms/collegeofliberalarts/worldlanguagescultures/baspanish/");
       
        /* for (Map.Entry<String, Course> course : spanCourses.entrySet()) {
            System.out.println("Prefix: " + course.getKey() + " Name: " + course.getValue().getName());
        } */

        initializeCscRequirements();
        
        for (Map.Entry<String, List<String>> requirement : cscRequirements.entrySet()) {    
            String[] parts = requirement.getKey().split("\\s+");
            String prefix = parts[0];
            int units = Integer.parseInt(parts[1]);
            requirements.add(makeRequirement(cs, prefix, requirement.getValue(), units));
        }

        initializeSpanRequirements();
        
        for (Map.Entry<String, List<String>> requirement : spanRequirements.entrySet()) {    
            String[] parts = requirement.getKey().split("\\s+");
            String prefix = parts[0];
            int units = Integer.parseInt(parts[1]);
            requirements.add(makeRequirement(cs, prefix, requirement.getValue(), units));
        }
        
        ArrayList<Course> spanElectives = new ArrayList<Course>();    
        for (Map.Entry<String, Course> span : spanCourses.entrySet()) {
            String[] acceptedCourses = {"span303", "span305", "span307", "span340", "span350", "span351", "span410", "span470"};
            if (!span.getKey().equals("wlc360") && !span.getKey().equals("wlc460") && !span.getKey().substring(0, 4).equals("span") || Arrays.asList(acceptedCourses).contains(span.getKey())) {
                spanElectives.add(span.getValue());
            }
        }
        requirements.add(new Requirement("span_electives", spanElectives, 12));
        /*for (Requirement requirement : requirements) {
            System.out.print("Requirement name: " + requirement.getRequirementName() + ", Requirement courses: ");
            for (Course course : requirement.getNecessaryCourses()) {
                System.out.print(course.getName().substring(0, 8) + ", ");
            }
            System.out.println("");
        }*/

        Map<String, List<Course>> genEdCoursesMap = new HashMap<>();

        ArrayList<Course> USCPcourses = new ArrayList<Course>();
        ArrayList<Course> GWRcourses = new ArrayList<Course>();
        // Iterate over each prefix in the original map
        for (List<Course> prefix : coursesByPrefix.values()) {
            // Iterate over each course in the prefix
            for (Course course : prefix) {
                // Get the GenEdArea of the course
                String genEdArea = course.getGE().toString();

                // Check if the GenEdArea is valid (excluding None)
                if (!"NONE".equals(genEdArea)) {
                    // Add the course to the corresponding list in the new map
                    genEdCoursesMap
                            .computeIfAbsent(genEdArea, k -> new ArrayList<>())
                            .add(course);
                }

                if (course.isUSCP()) {
                    USCPcourses.add(course);
                }
                if (course.isGWR()) {
                    GWRcourses.add(course);
                }
            }
        }

        for (Map.Entry<String, List<Course>> genEdArea : genEdCoursesMap.entrySet()) {
            requirements.add(new Requirement(genEdArea.getKey(), (ArrayList<Course>) genEdArea.getValue(), 4));
        }

        ArrayList<Course> bElectives = new ArrayList<Course>();
        bElectives.addAll(genEdCoursesMap.get("B1"));
        bElectives.addAll(genEdCoursesMap.get("B2"));
        bElectives.addAll(genEdCoursesMap.get("B4"));
        bElectives.addAll(genEdCoursesMap.get("UDB"));
        requirements.add(new Requirement("Area B electives", bElectives, 8));

        ArrayList<Course> ldcElectives = new ArrayList<Course>();
        ldcElectives.addAll(genEdCoursesMap.get("C1"));
        ldcElectives.addAll(genEdCoursesMap.get("C2"));
        requirements.add(new Requirement("Lower division C electives", ldcElectives, 8));
        
        requirements.add(new Requirement("USCP", USCPcourses, 4));
        requirements.add(new Requirement("GWR", GWRcourses, 4));

        /* for (Requirement requirement : requirements) {
            System.out.print("Requirement name: " + requirement.getRequirementName() + ", Requirement courses: ");
            for (Course course : requirement.getNecessaryCourses()) {
                System.out.print(course.getName().substring(0, 8) + ", ");
            }
            System.out.println("");
        } */
        /* Course course = (Course) coursesByPrefix.get("csc").stream()
                            .filter(co -> "TH 201".equals(co.getName().substring(0, 6)))
                            .collect(Collectors.toList()).get(0); */


        //requirements.add(new Requirement("CSC 123", new ArrayList<>(List.of(new Course("CourseA", GenEdArea.GEN_ED_1, new Season[]{Season.FALL}, 3, true, false))), 1)) 
        //requirements.add(new Course("CourseA", GenEdArea.GEN_ED_1, new Season[]{Season.FALL}, 3, true, false));

        return requirements;
    }

    private static Requirement makeRequirement(CourseScraper cs, String reqName, List<String> names, int number) {
        ArrayList<Course> necessaryCourses = new ArrayList<Course>();
        for (String name : names) {
            String prefix = name.replaceAll("[0-9]", "");
            String num = name.replaceAll("[^0-9]", "");
            necessaryCourses.add(cs.getCourseByCode(prefix, num));
        }
        return new Requirement(reqName, necessaryCourses, number);
    }

    private static void findCoursesNaiveMethod(Map<Requirement, Integer> requirementsNotMet) {
        ArrayList<Course> possibleCourses = new ArrayList<Course>();
        for (Map.Entry<Requirement, Integer> requirement : requirementsNotMet.entrySet()) {
            for (int i = 0; i < requirement.getValue(); i += 4) {
                for (Course course : requirement.getKey().getNecessaryCourses()) {
                    if (!possibleCourses.contains(course)) {
                        possibleCourses.add(course);
                        break;
                    }
                }
                
            }
        }

            
        System.out.println("\nSpring");
        int counter = 0;
        ArrayList<Course> coursesToRemove = new ArrayList<Course>();
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.SPRING)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                if (counter == 5) {
                    break;
                }
            }
        }
        for (Course course : coursesToRemove) {
            possibleCourses.remove(course);
        }
        coursesToRemove.clear();
        System.out.println("\nSummer");
        counter = 0;
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.SUMMER)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                if (counter == 10) {
                    break;
                } 
            }
        }
        for (Course course : coursesToRemove) {
            possibleCourses.remove(course);
        }
        coursesToRemove.clear();
        System.out.println("\nFall");
        counter = 0;
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.FALL)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                if (counter == 5) {
                    break;
                }
            }
        }
        for (Course course : coursesToRemove) {
            possibleCourses.remove(course);
        }
        coursesToRemove.clear();
        System.out.println("\nWinter");
        counter = 0;
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.WINTER)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                if (counter == 5) {
                    break;
                } 
            }
        }
        for (Course course : coursesToRemove) {
            possibleCourses.remove(course);
        }
        coursesToRemove.clear();
        System.out.println("\nSpring");
        counter = 0;
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.SPRING)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                if (counter == 5) {
                    break;
                }
            }
        }
    }

    private static void initializeCscRequirements() {
        // major
        cscRequirements.put("csc101 4", Arrays.asList("csc101"));
        cscRequirements.put("csc123 4", Arrays.asList("csc123"));
        cscRequirements.put("csc202 4", Arrays.asList("csc202"));
        cscRequirements.put("csc203 4", Arrays.asList("csc203"));
        cscRequirements.put("csc225 4", Arrays.asList("csc225"));
        cscRequirements.put("csc248 4", Arrays.asList("csc248"));
        cscRequirements.put("ethics 4", Arrays.asList("csc300", "phil323"));
        cscRequirements.put("csc307 4", Arrays.asList("csc307")); // given AI concentration
        cscRequirements.put("security 4", Arrays.asList("csc321", "csc323", "csc325"));
        cscRequirements.put("csc349 4", Arrays.asList("csc349"));
        cscRequirements.put("csc357 4", Arrays.asList("csc357"));
        cscRequirements.put("networks/distributed 4", Arrays.asList("csc364", "cpe464", "csc469")); // not accurate
        cscRequirements.put("csc365 4", Arrays.asList("csc365"));
        cscRequirements.put("csc430 4", Arrays.asList("csc430"));
        cscRequirements.put("csc445 4", Arrays.asList("csc445"));
        cscRequirements.put("csc453 4", Arrays.asList("csc453"));
        cscRequirements.put("csc497 2", Arrays.asList("csc497"));
        cscRequirements.put("csc498 2", Arrays.asList("csc498"));

        // AI concentration
        cscRequirements.put("csc466 4", Arrays.asList("csc466"));
        cscRequirements.put("csc480 4", Arrays.asList("csc480"));
        cscRequirements.put("csc487 4", Arrays.asList("csc487"));
        cscRequirements.put("stat344 4", Arrays.asList("stat334"));
        cscRequirements.put("cs_electives 8", Arrays.asList("cpe428", "csc481", "csc482", "csc566", "csc580", "csc581", "csc582", "csc587", "data301", "ee509", "stat434"));
        
        // Support courses
        cscRequirements.put("GRC 4", Arrays.asList("es350", "es351"));
        cscRequirements.put("math141 4", Arrays.asList("math141"));
        cscRequirements.put("math142 4", Arrays.asList("math142"));
        cscRequirements.put("math143 4", Arrays.asList("math143"));
        cscRequirements.put("linear 4", Arrays.asList("math206", "math244"));
        cscRequirements.put("phil 4", Arrays.asList("phil230", "phil231"));
        cscRequirements.put("stat312 4", Arrays.asList("stat312"));
        cscRequirements.put("life_science 4", Arrays.asList("bio111", "bio213", "mcro221", "bio161", "bot121"));
        cscRequirements.put("physical_science 12", Arrays.asList("phys141", "phys142", "phys143"));
        cscRequirements.put("additional_science 4", Arrays.asList("bio111", "bot121", "mcro221", "bio161", "chem124", "phys141"));
    }

    private static void initializeSpanRequirements() {
        // major
        spanRequirements.put("span201 4", Arrays.asList("span201"));
        spanRequirements.put("span202 4", Arrays.asList("span202"));
        spanRequirements.put("span3 4", Arrays.asList("span203", "span206"));
        spanRequirements.put("span207 4", Arrays.asList("span207"));
        spanRequirements.put("span233 4", Arrays.asList("span233"));
        spanRequirements.put("span301 4", Arrays.asList("span301"));
        spanRequirements.put("span302 4", Arrays.asList("span302"));
        // following requirement is waived due to study abroad
        // spanRequirements.put("span_300s 12", Arrays.asList("span303", "span305", "span307", "span340", "span390"));
        spanRequirements.put("span_400s 4", Arrays.asList("span402", "span410", "span416", "span470"));
        // note, I am not adding capstone or "minor and upper div span" courses as these are waived as part of my double major
    }

    private static ArrayList<String> populateCoursesTakenByCode() {
        ArrayList<String> coursesTakenByCode = new ArrayList<String>();
        coursesTakenByCode.add("csc101");
        coursesTakenByCode.add("csc123");
        coursesTakenByCode.add("csc202");
        coursesTakenByCode.add("csc203");
        coursesTakenByCode.add("csc225");
        coursesTakenByCode.add("csc248");
        coursesTakenByCode.add("csc349");
        coursesTakenByCode.add("csc357");
        coursesTakenByCode.add("cpe464");
        coursesTakenByCode.add("csc469"); // maybe unnecessary
        coursesTakenByCode.add("csc365");
        coursesTakenByCode.add("csc445");
        coursesTakenByCode.add("csc453");
        coursesTakenByCode.add("math141");
        coursesTakenByCode.add("math142");
        coursesTakenByCode.add("math143");
        coursesTakenByCode.add("math244");
        coursesTakenByCode.add("phil230");
        coursesTakenByCode.add("stat312");
        coursesTakenByCode.add("bio111");
        coursesTakenByCode.add("phys141");
        coursesTakenByCode.add("phys142");
        coursesTakenByCode.add("phys143");
        coursesTakenByCode.add("chem124");
        coursesTakenByCode.add("coms101");
        coursesTakenByCode.add("hlth250");
        coursesTakenByCode.add("th210");
        coursesTakenByCode.add("engl134");
        coursesTakenByCode.add("engl147");
        coursesTakenByCode.add("pols112");
        coursesTakenByCode.add("es252");
        coursesTakenByCode.add("span233");
        coursesTakenByCode.add("span201");
        coursesTakenByCode.add("span202");
        coursesTakenByCode.add("span203");
        coursesTakenByCode.add("phil350");
        return coursesTakenByCode;
    }
    private static ArrayList<Course> populateCoursesTaken(CourseScraper cs, ArrayList<String> coursesTakenByCode) {
        ArrayList<Course> coursesTaken = new ArrayList<Course>();
        for (String courseCode : coursesTakenByCode) {
            String prefix = courseCode.replaceAll("[0-9]", "");
            String num = courseCode.replaceAll("[^0-9]", "");
            coursesTaken.add(cs.getCourseByCode(prefix, num));
        }
        return coursesTaken;
    }
}
