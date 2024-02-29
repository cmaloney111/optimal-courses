package com.practicalalgorithms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.stream.Collectors;
import java.util.Scanner;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;


public class CourseSelector {
    private static Map<String, List<String>> cscRequirements = new HashMap<>();
    private static Map<String, List<String>> spanRequirements = new HashMap<>();
    private static List<Course> bestDistribution;
    public static void main(String args[]) {
        CourseScraper cs = new CourseScraper();
        cs.populateCoursesByPrefix();
        List<Requirement> requirements = initializeRequirements(cs);
        ArrayList<String> coursesTakenByCode = populateCoursesTakenByCode();
        ArrayList<Course> coursesTaken = populateCoursesTaken(cs, coursesTakenByCode);

        Map<Requirement, Integer> requirementsNotMet = checkRequirements(requirements, coursesTaken);

        //findCoursesNaiveMethod(requirementsNotMet);
        //findCoursesBruteForceMethod(requirementsNotMet, requirements, coursesTaken);
        //findCoursesDivideThenDP(requirementsNotMet, requirements, coursesTaken);
        findCoursesDPThenDivide(requirementsNotMet, requirements, coursesTaken);
    
        



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
            String[] acceptedCourses = {"span303", "span305", "span307", "span340", "span350", "span351"}; // not adding 470 or 410 since they are already included in span_400s
            if (!span.getKey().equals("wlc360") && !span.getKey().equals("wlc460") && !span.getKey().substring(0, 4).equals("span") || Arrays.asList(acceptedCourses).contains(span.getKey())) {
                spanElectives.add(span.getValue());
            }
        }

        
        requirements.add(new Requirement("span_electives", spanElectives, 12));
        // fishy
        //requirements.add(new Requirement("span_electives1", spanElectives, 4));
        
        //requirements.add(new Requirement("span_electives2", spanElectives, 4));
        
        //requirements.add(new Requirement("span_electives3", spanElectives, 4));
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

    private static Map<Requirement, Integer> checkRequirements(List<Requirement> requirements, ArrayList<Course> coursesTaken) {
        Map<Requirement, Integer> requirementsNotMet = new HashMap<>();
        for (Requirement requirement: requirements) {
            if (requirement.isRequirementFulfilled(coursesTaken) != 0) {
                requirementsNotMet.put(requirement, requirement.isRequirementFulfilled(coursesTaken));
            }
        }
        return requirementsNotMet;
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

    private static void findCoursesBruteForceMethod(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, ArrayList<Course> coursesTaken) {
        ArrayList<Course> coursesForReqsNotMet = new ArrayList<Course>();

        ArrayList<Requirement> spring = new ArrayList<Requirement>();
        ArrayList<Requirement> summer = new ArrayList<Requirement>();
        ArrayList<Requirement> fall = new ArrayList<Requirement>();
        ArrayList<Requirement> winter = new ArrayList<Requirement>();
        for (Map.Entry<Requirement, Integer> requirement: requirementsNotMet.entrySet()) {
            coursesForReqsNotMet.addAll(requirement.getKey().getNecessaryCourses());            
            for (Course course : requirement.getKey().getNecessaryCourses()) {
                if (!spring.contains(requirement.getKey()) && Arrays.asList(course.getQuartersOffered()).contains(Season.SPRING)) {
                    spring.add(requirement.getKey());
                }
                if (!summer.contains(requirement.getKey()) && Arrays.asList(course.getQuartersOffered()).contains(Season.SUMMER)) {
                    summer.add(requirement.getKey());
                }
                if (!fall.contains(requirement.getKey()) && Arrays.asList(course.getQuartersOffered()).contains(Season.FALL)) {
                    fall.add(requirement.getKey());
                }
                if (!winter.contains(requirement.getKey()) && Arrays.asList(course.getQuartersOffered()).contains(Season.WINTER)) {
                    winter.add(requirement.getKey());
                }
            }
        }
        System.out.println("Number of requirements not met: " + requirementsNotMet.size());
        System.out.println("Number of courses for requirements not met: " + coursesForReqsNotMet.size());
        System.out.println("Number of requirements that contain spring courses: " + spring.size());
        System.out.println("Number of requirements that contain summer courses: " + summer.size());
        System.out.println("Number of requirements that contain fall courses: " + fall.size());
        System.out.println("Number of requirements that contain winter courses: " + winter.size());

        ArrayList<Course> springCourses = new ArrayList<Course>();
        ArrayList<Course> summerCourses = new ArrayList<Course>();
        ArrayList<Course> winterCourses = new ArrayList<Course>();
        ArrayList<Course> fallCourses = new ArrayList<Course>();
        for (Course c : coursesForReqsNotMet) {
            if (!springCourses.contains(c) && Arrays.asList(c.getQuartersOffered()).contains(Season.SPRING)) {
                springCourses.add(c);
            }
            if (!summerCourses.contains(c) && Arrays.asList(c.getQuartersOffered()).contains(Season.SUMMER)) {
                summerCourses.add(c);
            }
            if (!fallCourses.contains(c) && Arrays.asList(c.getQuartersOffered()).contains(Season.FALL)) {
                fallCourses.add(c);
            }
            if (!winterCourses.contains(c) && Arrays.asList(c.getQuartersOffered()).contains(Season.WINTER)) {
                winterCourses.add(c);  
            }
        }

        System.out.println("Number of courses that are offered in spring: " + springCourses.size());
        System.out.println("Number of courses that are offered in summer: " + summerCourses.size());
        System.out.println("Number of courses that are offered in fall: " + fallCourses.size());
        System.out.println("Number of courses that are offered in winter: " + winterCourses.size());
        System.out.println("\n Note that this brute force method has to calculate (" +
                            springCourses.size() + "^9) * (" + summerCourses.size() + "^2) * (" + fallCourses.size() + "^5) * (" + winterCourses.size() +
                            "^5) \ndifferent combinations of courses... that's " + (Math.pow(springCourses.size(), 9)*Math.pow(summerCourses.size(), 2)*Math.pow(fallCourses.size(),5)*Math.pow(winterCourses.size(),5)) + " courses!");

        Scanner in = new Scanner(System.in);
 
        System.out.println("Are you sure you would like to continue? This may take a while... [Y] / [N]");
        while (true) {
            String s = in.nextLine();
            if (s.equals("Y")) {
                break;
            }
            if (s.equals("N")) {
                System.out.println("Smart choice");
                return;
            }
        }
        ArrayList<ArrayList<Course>> winningCourseList = new ArrayList<ArrayList<Course>>();
        int count = 0;
        for (Course course_spring_1 : coursesForReqsNotMet) {
            if (Arrays.asList(course_spring_1.getQuartersOffered()).contains(Season.SPRING)) {
            for (Course course_spring_2 : coursesForReqsNotMet) {
                if (Arrays.asList(course_spring_2.getQuartersOffered()).contains(Season.SPRING)) {
                for (Course course_spring_3 : coursesForReqsNotMet) {
                    if (Arrays.asList(course_spring_3.getQuartersOffered()).contains(Season.SPRING)) {
                    for (Course course_spring_4 : coursesForReqsNotMet) {
                        if (Arrays.asList(course_spring_4.getQuartersOffered()).contains(Season.SPRING)) {
                        for (Course course_spring_5 : coursesForReqsNotMet) {
                            if (Arrays.asList(course_spring_5.getQuartersOffered()).contains(Season.SPRING)) {
                            for (Course course_summer_1 : coursesForReqsNotMet) {
                                if (Arrays.asList(course_summer_1.getQuartersOffered()).contains(Season.SUMMER)) {
                                for (Course course_summer_2 : coursesForReqsNotMet) {
                                    if (Arrays.asList(course_summer_2.getQuartersOffered()).contains(Season.SUMMER)) {
                                    for (Course course_fall_1 : coursesForReqsNotMet) {
                                        if (Arrays.asList(course_fall_1.getQuartersOffered()).contains(Season.FALL)) {
                                        for (Course course_fall_2 : coursesForReqsNotMet) {
                                            if (Arrays.asList(course_fall_2.getQuartersOffered()).contains(Season.FALL)) {
                                            for (Course course_fall_3 : coursesForReqsNotMet) {
                                                if (Arrays.asList(course_fall_3.getQuartersOffered()).contains(Season.FALL)) {
                                                for (Course course_fall_4 : coursesForReqsNotMet) {
                                                    if (Arrays.asList(course_fall_4.getQuartersOffered()).contains(Season.FALL)) {
                                                    for (Course course_fall_5 : coursesForReqsNotMet) {
                                                        if (Arrays.asList(course_fall_5.getQuartersOffered()).contains(Season.FALL)) {
                                                        for (Course course_winter_1 : coursesForReqsNotMet) {
                                                            if (Arrays.asList(course_winter_1.getQuartersOffered()).contains(Season.WINTER)) {
                                                            for (Course course_winter_2 : coursesForReqsNotMet) {
                                                                if (Arrays.asList(course_winter_2.getQuartersOffered()).contains(Season.WINTER)) {
                                                                for (Course course_winter_3 : coursesForReqsNotMet) {
                                                                    if (Arrays.asList(course_winter_3.getQuartersOffered()).contains(Season.WINTER)) {
                                                                    for (Course course_winter_4 : coursesForReqsNotMet) {
                                                                        if (Arrays.asList(course_winter_4.getQuartersOffered()).contains(Season.WINTER)) {
                                                                        for (Course course_winter_5 : coursesForReqsNotMet) {
                                                                            if (Arrays.asList(course_winter_5.getQuartersOffered()).contains(Season.WINTER)) {
                                                                            for (Course course_spring_6 : coursesForReqsNotMet) {
                                                                                if (Arrays.asList(course_spring_6.getQuartersOffered()).contains(Season.SPRING)) {
                                                                                for (Course course_spring_7 : coursesForReqsNotMet) {
                                                                                    if (Arrays.asList(course_spring_7.getQuartersOffered()).contains(Season.SPRING)) {
                                                                                    for (Course course_spring_8 : coursesForReqsNotMet) {
                                                                                        if (Arrays.asList(course_spring_8.getQuartersOffered()).contains(Season.SPRING)) {
                                                                                        for (Course course_spring_9 : coursesForReqsNotMet) {
                                                                                            if (Arrays.asList(course_spring_9.getQuartersOffered()).contains(Season.SPRING)) {
                                                                                            count++;
                                                                                            System.out.println(count);
                                                                                            ArrayList<Course> newCoursesTaken = new ArrayList<Course>();
                                                                                            newCoursesTaken.addAll(coursesTaken);
                                                                                            newCoursesTaken.add(course_spring_1);
                                                                                            newCoursesTaken.add(course_spring_2);
                                                                                            newCoursesTaken.add(course_spring_3);
                                                                                            newCoursesTaken.add(course_spring_4);
                                                                                            newCoursesTaken.add(course_spring_5);
                                                                                            newCoursesTaken.add(course_spring_6);
                                                                                            newCoursesTaken.add(course_spring_7);
                                                                                            newCoursesTaken.add(course_spring_8);
                                                                                            newCoursesTaken.add(course_spring_9);
                                                                                            newCoursesTaken.add(course_summer_1);
                                                                                            newCoursesTaken.add(course_summer_2);
                                                                                            newCoursesTaken.add(course_winter_1);
                                                                                            newCoursesTaken.add(course_winter_2);
                                                                                            newCoursesTaken.add(course_winter_3);
                                                                                            newCoursesTaken.add(course_winter_4);
                                                                                            newCoursesTaken.add(course_winter_5);
                                                                                            newCoursesTaken.add(course_fall_1);
                                                                                            newCoursesTaken.add(course_fall_2);
                                                                                            newCoursesTaken.add(course_fall_3);
                                                                                            newCoursesTaken.add(course_fall_4);
                                                                                            newCoursesTaken.add(course_fall_5);
                                                                                            for (Requirement requirement: requirements) {
                                                                                                if (requirement.isRequirementFulfilled(newCoursesTaken) != 0) {
                                                                                                    break;
                                                                                                }
                                                                                            }
                                                                                            ArrayList<Course> newCoursesTakens = new ArrayList<Course>();
                                                                                            newCoursesTakens.add(course_spring_1);
                                                                                            newCoursesTakens.add(course_spring_2);
                                                                                            newCoursesTakens.add(course_spring_3);
                                                                                            newCoursesTakens.add(course_spring_4);
                                                                                            newCoursesTakens.add(course_spring_5);
                                                                                            newCoursesTakens.add(course_summer_1);
                                                                                            newCoursesTakens.add(course_summer_2);
                                                                                            newCoursesTakens.add(course_winter_1);
                                                                                            newCoursesTakens.add(course_winter_2);
                                                                                            newCoursesTakens.add(course_winter_3);
                                                                                            newCoursesTakens.add(course_winter_4);
                                                                                            newCoursesTakens.add(course_winter_5);
                                                                                            newCoursesTakens.add(course_fall_1);
                                                                                            newCoursesTakens.add(course_fall_2);
                                                                                            newCoursesTakens.add(course_fall_3);
                                                                                            newCoursesTakens.add(course_fall_4);
                                                                                            newCoursesTakens.add(course_fall_5);
                                                                                            newCoursesTakens.add(course_spring_6);
                                                                                            newCoursesTakens.add(course_spring_7);
                                                                                            newCoursesTakens.add(course_spring_8);
                                                                                            newCoursesTakens.add(course_spring_9);
                                                                                            winningCourseList.add(newCoursesTakens);
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }                                   
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }}}}}}}}}}}}}}}}}}}}}}
        int co = 0;
        for (ArrayList<Course> winningCourseSelection : winningCourseList) {
            co++;
            System.out.print("Course list number " + co + ": ");
            for (Course course : winningCourseSelection) {
                System.out.print(course.getName().substring(0, 9) + ", ");
            }
            System.out.println("");
        }
    }

    private static void findCoursesDivideThenDP(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, ArrayList<Course> coursesTaken) {
        ArrayList<Course> coursesForReqsNotMet = new ArrayList<Course>();
        for (Map.Entry<Requirement, Integer> requirement: requirementsNotMet.entrySet()) {
            coursesForReqsNotMet.addAll(requirement.getKey().getNecessaryCourses());            
        }
        ArrayList<Course> springCourses = new ArrayList<Course>();
        ArrayList<Course> summerCourses = new ArrayList<Course>();
        ArrayList<Course> winterCourses = new ArrayList<Course>();
        ArrayList<Course> fallCourses = new ArrayList<Course>();
        
        for (Course c : coursesForReqsNotMet) {
            if (!springCourses.contains(c) && Arrays.asList(c.getQuartersOffered()).contains(Season.SPRING)) {
                springCourses.add(c);
            }
            if (!summerCourses.contains(c) && Arrays.asList(c.getQuartersOffered()).contains(Season.SUMMER)) {
                summerCourses.add(c);
            }
            if (!fallCourses.contains(c) && Arrays.asList(c.getQuartersOffered()).contains(Season.FALL)) {
                fallCourses.add(c);
            }
            if (!winterCourses.contains(c) && Arrays.asList(c.getQuartersOffered()).contains(Season.WINTER)) {
                winterCourses.add(c);  
            }
        }
        System.out.println("Spring");
        Set<Requirement> requirementsNotMetList = requirementsNotMet.keySet();
        List<Course> coursesFound = findBestCourses(requirementsNotMet, requirements, springCourses, Season.SPRING);
        for (Course course : coursesFound) {
            summerCourses.remove(course);
            fallCourses.remove(course);
            winterCourses.remove(course);
            springCourses.remove(course);
        }
        coursesTaken.addAll(coursesFound);
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        coursesFound.clear();
        System.out.println("\nSummer");
        coursesFound = findBestCourses(requirementsNotMet, requirements, summerCourses, Season.SUMMER);
        for (Course course : coursesFound) {
            fallCourses.remove(course);
            winterCourses.remove(course);
            springCourses.remove(course);
        }
        coursesTaken.addAll(coursesFound);
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        coursesFound.clear();
        System.out.println("\nFall");
        coursesFound = findBestCourses(requirementsNotMet, requirements, fallCourses, Season.FALL);
        for (Course course : coursesFound) {
            winterCourses.remove(course);
            springCourses.remove(course);
        }
        coursesTaken.addAll(coursesFound);
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        coursesFound.clear();
        System.out.println("\nWinter");
        coursesFound = findBestCourses(requirementsNotMet, requirements, winterCourses, Season.WINTER);
        for (Course course : coursesFound) {
            springCourses.remove(course);
        }
        coursesTaken.addAll(coursesFound);
        System.out.println("\nSpring 2");
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        coursesFound = findBestCourses(requirementsNotMet, requirements, springCourses, Season.SPRING); 
        coursesTaken.addAll(coursesFound);
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        for (Map.Entry<Requirement, Integer> req : requirementsNotMet.entrySet()) {
            System.out.println(req.getKey().getRequirementName());
        }    
        Requirement.whatFulfillsIt(requirementsNotMetList, coursesTaken);
    }

    private static void findCoursesDPThenDivide(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, ArrayList<Course> coursesTaken) {
        ArrayList<Course> coursesForReqsNotMet = new ArrayList<Course>();
        for (Map.Entry<Requirement, Integer> requirement: requirementsNotMet.entrySet()) {
            coursesForReqsNotMet.addAll(requirement.getKey().getNecessaryCourses());            
        }
        ArrayList<Course> futureCoursesTaken = new ArrayList<Course>();
        while (!requirementsNotMet.isEmpty()) {
            Set<Requirement> requirementsNotMetList = requirementsNotMet.keySet();
            List<Course> coursesFound = findBestCourses(requirementsNotMet, requirements, coursesForReqsNotMet, Season.TBD); 
            coursesTaken.addAll(coursesFound);
            requirementsNotMet = checkRequirements(requirements, coursesTaken);
            Requirement.whatFulfillsIt(requirementsNotMetList, coursesTaken);
            futureCoursesTaken = union(futureCoursesTaken, coursesFound);
            coursesTaken = union(coursesTaken, coursesFound);
        }

        ArrayList<Course> futureFallCourses = new ArrayList<>();
        ArrayList<Course> futureWinterCourses = new ArrayList<>();
        ArrayList<Course> futureSpringCourses = new ArrayList<>();
        ArrayList<Course> futureSummerCourses = new ArrayList<>();

    
        fillSeasonLists(futureCoursesTaken, futureFallCourses, futureWinterCourses, futureSpringCourses, futureSummerCourses);

        System.out.print("Future Spring Courses: ");
        for (Course c : futureSpringCourses) {
            System.out.print(c.getName().substring(0, c.getName().indexOf(".")) + ", ");
        }
        System.out.print("\nFuture Summer Courses: ");
        for (Course c : futureSummerCourses) {
            System.out.print(c.getName().substring(0, c.getName().indexOf(".")) + ", ");
        }
        System.out.print("\nFuture Fall Courses: ");
        for (Course c : futureFallCourses) {
            System.out.print(c.getName().substring(0, c.getName().indexOf(".")) + ", ");
        }
        System.out.print("\nFuture Winter Courses: ");
        for (Course c : futureWinterCourses) {
            System.out.print(c.getName().substring(0, c.getName().indexOf(".")) + ", ");
        }
    }

    private static void fillSeasonLists(ArrayList<Course> futureCoursesTaken, ArrayList<Course> futureFallCourses, ArrayList<Course> futureWinterCourses,
        ArrayList<Course> futureSpringCourses, ArrayList<Course> futureSummerCourses) {
        int fallLimit = 5;
        int winterLimit = 5;
        int springLimit = 9;
        int summerLimit = 2;

        int numSeasons = 4; // Fall, Winter, Spring, Summer
        int[][] dp = new int[numSeasons + 1][];
        dp[0] = new int[fallLimit + 1];
        dp[1] = new int[winterLimit + 1];
        dp[2] = new int[springLimit + 1];
        dp[3] = new int[summerLimit + 1];

        for (int i = 0; i < numSeasons + 1; i++) {
            for (int j = 0; j <= (i == 0 ? fallLimit : (i == 1 ? winterLimit : (i == 2 ? springLimit : summerLimit))); j++) {
                for (Course course : futureCoursesTaken) {
                    Season[] quartersOffered = course.getQuartersOffered();
                    int value = (int) Arrays.stream(quartersOffered).filter(Objects::nonNull).count();

                    if (i > 0 && quartersOffered[i - 1] != null && j >= value) {
                        dp[i][j] = Math.max(dp[i][j], dp[i - 1][j - value] + value);
                    }
                    
                    if (i > 0) {
                        dp[i][j] = Math.max(dp[i][j], dp[i - 1][j]);
                    }
                    
                    
                }
            }
        }

        reconstructCourses(futureCoursesTaken, futureFallCourses, futureWinterCourses, futureSpringCourses,
                futureSummerCourses, dp);
    }

    private static void reconstructCourses(List<Course> courses, ArrayList<Course> futureFallCourses,
            ArrayList<Course> futureWinterCourses, ArrayList<Course> futureSpringCourses,
            ArrayList<Course> futureSummerCourses, int[][] dp) {
        int i = dp.length - 1;
        int j = dp[0].length - 1;

        while (i > 0 && j > 0) {
            if (dp[i][j] != dp[i - 1][j]) {
                Course course = courses.get(j - 1);
                switch (i - 1) {
                    case 0:
                        futureFallCourses.add(course);
                        break;
                    case 1:
                        futureWinterCourses.add(course);
                        break;
                    case 2:
                        futureSpringCourses.add(course);
                        break;
                    case 3:
                        futureSummerCourses.add(course);
                        break;
                }
                j -= 1;
            } else {
                i -= 1;
            }
        }
    }

    
    private static List<Course> findBestCourses(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, ArrayList<Course> courses, Season season) {
        ArrayList<Requirement> uniqueReqs = new ArrayList<Requirement>();
        Map<Course, ArrayList<Requirement>> requirementsFulfilledByCourse = new HashMap<Course, ArrayList<Requirement>>();
        for (Course course: courses) {
            ArrayList<Requirement> requirementsFulfilled = new ArrayList<Requirement>();
            for (Requirement req: requirementsNotMet.keySet()) {
                if (req.getNecessaryCourses().contains(course)) {
                    requirementsFulfilled.add(req);
                    if (!uniqueReqs.contains(req)) {
                        uniqueReqs.add(req);
                    }
                }
            }
            requirementsFulfilledByCourse.put(course, requirementsFulfilled);
            
        }


        // brute force for now, do better later
        List<Course> bestCourses = new ArrayList<Course>();
        // Definition: C[i, j] = Max num reqs fulfilled with i courses using course j
        // Base cases: C[0, j] = 0 
        // Solution: Max{C[i, 5]}
        // Formula: C[i, j] = Max{C[i-1, j] + Requirements_fulfilled[i]}
        ArrayList<ArrayList<RequirementEntry>> dp = new ArrayList<>();
        int numCourses = 5;
        if (season == Season.SUMMER) {
            numCourses = 2;
        }
        if (season == Season.TBD) {
            numCourses = 22;
        }
        for (int i = 0; i < numCourses+1; i++) {
            ArrayList<RequirementEntry> courseList = new ArrayList<>();
            for (int j = 0; j < courses.size(); j++) {
                courseList.add(new RequirementEntry());
            }
            dp.add(courseList);
        }
        for (int i = 0; i < courses.size(); i++) {
            dp.get(1).set(i, new RequirementEntry(requirementsFulfilledByCourse.get(courses.get(i)), i));
        }

        for (int i = 2; i < numCourses+1; i++) {
            for (int j = 0; j < courses.size(); j++) {
                // int maxReqsFulfilled = 0;
                for (int k = 0; k < courses.size(); k++) {
                    ArrayList<Requirement> uniqueRequirements = new ArrayList<Requirement>();
                    uniqueRequirements.addAll((dp.get(i-1).get(k).getRequirements()));
                    uniqueRequirements = union(uniqueRequirements, requirementsFulfilledByCourse.get(courses.get(j)));
                    if (dp.get(i).get(j).getRequirements().size() < uniqueRequirements.size()) {
                    
                        ArrayList<Integer> uniqueCourses = dp.get(i-1).get(k).getCourses();
                        uniqueCourses.add(j);

                        RequirementEntry entryToUpdate = dp.get(i).get(j);
                        entryToUpdate.setRequirements(uniqueRequirements);
                        entryToUpdate.setCourses(uniqueCourses);
                        dp.get(i).set(j, entryToUpdate);
                    }

                }
            }
        }
        int maxReqs = 0;

        ArrayList<Integer> bestCourseIntegerList = new ArrayList<Integer>();
        for (int j = 0; j < courses.size(); j++) {
            // System.out.println(dp.get(5).get(j).getRequirements().size());
            if (dp.get(numCourses).get(j).getRequirements().size() > maxReqs) {
                maxReqs = dp.get(numCourses).get(j).getRequirements().size();
                bestCourseIntegerList = dp.get(numCourses).get(j).getCourses();
            }
        }
        
        for (int courseInt : bestCourseIntegerList) {
            if (!bestCourses.contains(courses.get(courseInt))) {
                bestCourses.add(courses.get(courseInt));
            }
        }

        if (season != Season.TBD) {
            int i = 0;
            for (Course c: bestCourses) {
                i++;
                System.out.println("Course " + i + ": " + c.getName());
            }
        }
        else {
            bestCourses.remove(bestCourses.size()-1);
        }
        return bestCourses;
        /*
            if (i > 2) {
                System.out.print("At least 3: " + course.getName() + ": ");
                for (Requirement req: requirementsNotMet.keySet()) {
                    if (req.getNecessaryCourses().contains(course)) {
                        System.out.print(req.getRequirementName() + ", ");
                    }
                }
                System.out.println("");
            }*/
    } 
    
    /* private static List<Course> findBestCourses(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, List<Course> courses, Season season) {
        int numCourses = courses.size();
        int numRequirements = requirements.size();
    
        int[][] dp = new int[numCourses + 1][numRequirements + 1];
    
        for (int i = 1; i <= numCourses; i++) {
            for (int j = 1; j <= numRequirements; j++) {
                Requirement req = requirements.get(j - 1);
                Course course = courses.get(i - 1);
    
                int withoutCourse = dp[i - 1][j];
                int withCourse = 0;
    
                if (req.getNecessaryCourses().contains(course)) {
                    int remainingCourses = requirementsNotMet.get(req) - 4;
                    if (remainingCourses >= 0) {
                        withCourse = dp[i - 1][j - 1] + 1;
                    }
                }
    
                dp[i][j] = Math.max(withCourse, withoutCourse);
            }
        }
    
        List<Course> bestCourses = new ArrayList<>();
    
        int i = numCourses;
        int j = numRequirements;
    
        while (i > 0 && j > 0) {
            if (dp[i][j] != dp[i - 1][j]) {
                Requirement req = requirements.get(j - 1);
                bestCourses.add(courses.get(i - 1));
                requirementsNotMet.put(req, requirementsNotMet.get(req) - 1);
                i--;
                j--;
            } else {
                i--;
            }
        }
    
        return bestCourses;
    } */

    private static <T> ArrayList<T> union(List<T> list1, List<T> list2) {
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
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

        cscRequirements.put("AI_electives 8", Arrays.asList("cpe428", "csc481", "csc482", "csc566", "csc580", "csc581", "csc582", "csc587", "data301", "ee509", "stat434"));
        
        //cscRequirements.put("AI_electives1 4", Arrays.asList("cpe428", "csc481", "csc482", "csc566", "csc580", "csc581", "csc582", "csc587", "data301", "ee509", "stat434"));
        //cscRequirements.put("AI_electives2 4", Arrays.asList("cpe428", "csc481", "csc482", "csc566", "csc580", "csc581", "csc582", "csc587", "data301", "ee509", "stat434"));
        
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
