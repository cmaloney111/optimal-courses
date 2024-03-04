package com.practicalalgorithms;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.Arrays;

public class CourseSelector {
    // Class for finding the ideal course list using various methods

    private static Map<String, List<String>> cscRequirements = new HashMap<>();
    private static Map<String, List<String>> spanRequirements = new HashMap<>();

    public static void main(String args[]) {

        CourseScraper cs = new CourseScraper();
        System.out.println("Please wait about 5 seconds while courses are fetched from the cal poly course catalog.\n");
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<?> future = executorService.submit(() -> {
             // Start a separate thread to populate the courses by their prefixes (takes about 5 seconds)
            cs.populateCoursesByPrefix();
        });

        // Display a "progress bar" in the main thread
        try {
            while (!future.isDone()) {
                System.out.print(".");
                Thread.sleep(500); // Wait 0.5 seconds before printing each dot
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Shutdown the executor service when the task is done
            executorService.shutdown();
        }
        System.out.println("\nCourses populated\n");



        Scanner scanner = new Scanner(System.in);

        while (true) {
            // Necessary data for finding the ideal course list
            List<Requirement> requirements = initializeRequirements(cs);
            ArrayList<String> coursesTakenByCode = populateCoursesTakenByCode();
            ArrayList<Course> coursesTaken = populateCoursesTaken(cs, coursesTakenByCode);
            Map<Requirement, Integer> requirementsNotMet = checkRequirements(requirements, coursesTaken);
            // Display menu options
            System.out.println("\nPlease choose an option for how to find your ideal course list:");
            System.out.println("1. Find Courses (Naive/Random Method - doesn't find ideal course list)");
            System.out.println("2. Find Courses (Brute Force Method - search all possible course lists to find ideal)");
            System.out.println("3. Find Courses (Divide Then DP - finds ideal course list for each season but may not \n\t\t\t\t  find overall ideal course list, finds \"approximation\")");
            System.out.println("4. Find Courses (DP Then Divide (Using Ford-Fulkerson algorithm for max flow) - finds an ideal course list, best option)");
            System.out.println("0. Exit");

            int choice = getUserChoice(scanner);

            // Process user choice
            switch (choice) {
                case 1:
                    findCoursesNaiveMethod(requirementsNotMet);
                    break;
                case 2:
                    findCoursesBruteForceMethod(requirementsNotMet, requirements, coursesTaken);
                    break;
                case 3:
                    findCoursesDivideThenDP(requirementsNotMet, requirements, coursesTaken);
                    break;
                case 4:
                    findCoursesDPThenDivide(requirementsNotMet, requirements, coursesTaken);
                    break;
                case 0:
                    System.out.println("Exiting program. Goodbye!");
                    System.exit(0);
                default:
                    System.out.println("Invalid choice. Please enter a valid option.");
            }
        }
    }    

    private static int getUserChoice(Scanner scanner) {
        // Get user input and validate
        while (!scanner.hasNextInt()) {
            System.out.println("Invalid input. Please enter a number.");
            scanner.next(); // consume invalid input
        }
        return scanner.nextInt();
    }


    private static List<Requirement> initializeRequirements(CourseScraper cs) {
        // Find all requirements needed given majors. In my case, I am a spanish and CS major,
        // so I put in all of my required courses. This will differ from person to person.
        List<Requirement> requirements = new ArrayList<>();

        Map<String, List<Course>> coursesByPrefix = cs.getCoursesByPrefix();
        Map<String, Course> spanCourses = cs.getCoursesFromMajor("https://catalog.calpoly.edu/collegesandprograms/collegeofliberalarts/worldlanguagescultures/baspanish/");


        initializeCscRequirements(); // populates the cscRequirements map (static variable outside this function)
        
        // Add CS requirements to requirement list
        for (Map.Entry<String, List<String>> requirement : cscRequirements.entrySet()) {    
            String[] parts = requirement.getKey().split("\\s+");
            String prefix = parts[0];
            int units = Integer.parseInt(parts[1]);
            requirements.add(makeRequirement(cs, prefix, requirement.getValue(), units));
        }

        initializeSpanRequirements(); // populates the spanRequirements map (static variable outside this function)
        
        // Add Spanish requirements to requirement list
        for (Map.Entry<String, List<String>> requirement : spanRequirements.entrySet()) {    
            String[] parts = requirement.getKey().split("\\s+");
            String prefix = parts[0];
            int units = Integer.parseInt(parts[1]);
            requirements.add(makeRequirement(cs, prefix, requirement.getValue(), units));
        }
        
        // Add spanish electives to requirement list (these could have been added in spanRequirements but
        // it was much easier to scrape them from the Spanish major page since there are a lot)
        ArrayList<Course> spanElectives = new ArrayList<Course>();    
        for (Map.Entry<String, Course> span : spanCourses.entrySet()) {
            String[] acceptedCourses = {"span303", "span305", "span307", "span340", "span350", "span351", "engl310"}; // not adding 470 or 410 since they are already included in span_400s
            if (!span.getKey().equals("wlc360") && !span.getKey().equals("wlc460") && !span.getKey().substring(0, 4).equals("span") || Arrays.asList(acceptedCourses).contains(span.getKey())) {
                spanElectives.add(span.getValue());
            }
        }
        requirements.add(new Requirement("span_electives", spanElectives, 12));


        Map<String, List<Course>> genEdCoursesMap = new HashMap<>(); // find which courses fit into each GE
        ArrayList<Course> USCPcourses = new ArrayList<Course>(); // find USCP (United States Cultural Pluralism) courses
        ArrayList<Course> GWRcourses = new ArrayList<Course>(); // find GWR (Graduation Writing Requirement) courses


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

                // If the course is USCP or GWR, add it to the corresponding lists
                if (course.isUSCP()) {
                    USCPcourses.add(course);
                }
                if (course.isGWR()) {
                    GWRcourses.add(course);
                }
            }
        }

        // Add requirements from the above lists to the requirements list (in other words, add GE requirements)
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

        return requirements;
    }

    private static Map<Requirement, Integer> checkRequirements(List<Requirement> requirements, ArrayList<Course> coursesTaken) {
        // See which requirements are met and not met
        Map<Requirement, Integer> requirementsNotMet = new HashMap<>();

        // If a requirement is not fulfilled, add it to the requirementsNotMet list
        for (Requirement requirement: requirements) {
            if (requirement.isRequirementFulfilled(coursesTaken) != 0) {
                requirementsNotMet.put(requirement, requirement.isRequirementFulfilled(coursesTaken));
            }
        }
        return requirementsNotMet;
    }

    private static Requirement makeRequirement(CourseScraper cs, String reqName, List<String> names, int number) {
        // Make a requirement object given a requirement name, a list of course names, and a unit number
        ArrayList<Course> necessaryCourses = new ArrayList<Course>();
        for (String name : names) {
            String prefix = name.replaceAll("[0-9]", "");
            String num = name.replaceAll("[^0-9]", "");
            necessaryCourses.add(cs.getCourseByCode(prefix, num));
        }
        return new Requirement(reqName, necessaryCourses, number);
    }

    private static void findCoursesNaiveMethod(Map<Requirement, Integer> requirementsNotMet) {
        // Find a random list of courses that fits season capacities
        // (in my case, 5 in spring, 2 in summer, 5 in fall, 5 in winter, then 4 in spring)
        // No advanced algorithms are used, this method is just a test to show how
        // using algorithms can have a significant improvement compared to not using them.
        // You can check this by looking at the number of requirements fulfilled with this method
        // and compare it to others.
        ArrayList<Course> possibleCourses = new ArrayList<Course>();

        // Get some possible courses to take
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

        ArrayList<Course> coursesTaken = new ArrayList<Course>();

        // Find the first five courses that are offered in spring from the possibleCourses list
        System.out.println("\nSpring");
        int counter = 0;
        ArrayList<Course> coursesToRemove = new ArrayList<Course>();
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.SPRING)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                coursesTaken.add(course);
                if (counter == 5) {
                    break;
                }
            }
        }
        // Remove the courses taken from possibleCourses list
        for (Course course : coursesToRemove) {
            possibleCourses.remove(course);
        }
        coursesToRemove.clear();

        // Repeat above for summer (with 2 courses max)
        System.out.println("\nSummer");
        counter = 0;
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.SUMMER)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                coursesTaken.add(course);
                if (counter == 2) {
                    break;
                } 
            }
        }
        for (Course course : coursesToRemove) {
            possibleCourses.remove(course);
        }
        coursesToRemove.clear();

        // Repeat above for fall
        System.out.println("\nFall");
        counter = 0;
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.FALL)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                coursesTaken.add(course);
                if (counter == 5) {
                    break;
                }
            }
        }
        for (Course course : coursesToRemove) {
            possibleCourses.remove(course);
        }
        coursesToRemove.clear();

        // Repeat above for winter
        System.out.println("\nWinter");
        counter = 0;
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.WINTER)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                coursesTaken.add(course);
                if (counter == 5) {
                    break;
                } 
            }
        }
        for (Course course : coursesToRemove) {
            possibleCourses.remove(course);
        }
        coursesToRemove.clear();

        // Repeat one last time for spring with 4 courses
        System.out.println("\nSpring");
        counter = 0;
        for (Course course : possibleCourses) {
            if (Arrays.asList(course.getQuartersOffered()).contains(Season.SPRING)) {
                counter++;
                System.out.println("course " + counter + ": " + course.getName().substring(0, 8));
                coursesToRemove.add(course);
                coursesTaken.add(course);
                if (counter == 4) {
                    break;
                }
            }
        }

        // See what requirements are fulfilled and how many
        Set<Requirement> requirementsNotMetList = requirementsNotMet.keySet();
        int reqsFulfilled = Requirement.whatFulfillsIt(requirementsNotMetList, coursesTaken);
        System.out.println("Total number of requirements fulfilled: " + reqsFulfilled);
    }

    private static void findCoursesBruteForceMethod(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, ArrayList<Course> coursesTaken) {
        // Brute force search all course combinations (for my specific future course layout of 5 spring courses, 2 summer, 5 fall, 5 winter, and 4 spring)
        ArrayList<Course> coursesForReqsNotMet = new ArrayList<Course>();

        ArrayList<Requirement> spring = new ArrayList<Requirement>();
        ArrayList<Requirement> summer = new ArrayList<Requirement>();
        ArrayList<Requirement> fall = new ArrayList<Requirement>();
        ArrayList<Requirement> winter = new ArrayList<Requirement>();

        // Get all requirements with courses offered in spring, summer, fall, and winter
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

        // Print out some useful math to warn the user before they run this program.
        // The user should expect that the universe will end before this program finishes in most scenarios
        System.out.println("Number of requirements not met: " + requirementsNotMet.size());
        System.out.println("Number of courses for requirements not met: " + coursesForReqsNotMet.size());
        System.out.println("Number of requirements that contain spring courses: " + spring.size());
        System.out.println("Number of requirements that contain summer courses: " + summer.size());
        System.out.println("Number of requirements that contain fall courses: " + fall.size());
        System.out.println("Number of requirements that contain winter courses: " + winter.size());

        // Get all courses offered in each season
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

        // Print some more useful math
        System.out.println("Number of courses that are offered in spring: " + springCourses.size());
        System.out.println("Number of courses that are offered in summer: " + summerCourses.size());
        System.out.println("Number of courses that are offered in fall: " + fallCourses.size());
        System.out.println("Number of courses that are offered in winter: " + winterCourses.size());
        System.out.println("\nGiven the data above, note that this brute force method has to calculate (" +
                            springCourses.size() + "^9) * (" + summerCourses.size() + "^2) * (" + fallCourses.size() + "^5) * (" + winterCourses.size() +
                            "^5) \ndifferent combinations of courses... that's " + (Math.pow(springCourses.size(), 9)*Math.pow(summerCourses.size(), 2)*Math.pow(fallCourses.size(),5)*Math.pow(winterCourses.size(),5)) + " courses!");

        Scanner in = new Scanner(System.in);
 
        System.out.println("\nAlso, note that this method only works for my specific scenario \n(5 courses next spring, 2 courses in summer, 5 courses in fall, 5 courses in winter, then 4 courses in spring).\nThat being said, are you sure you would like to continue? This may take a while... [Y] / [N]");
        while (true) {
            String s = in.nextLine();
            if (s.equals("Y")) {
                in.close();
                break;
            }
            if (s.equals("N")) {
                System.out.println("\nSmart choice");
                in.close();
                return;
            }
        }

        in.close();

        // Begin the madness
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
                                                                                            System.out.println("Course combinations checked: " + count);
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
        }}}}}}}}}}}}}}}}}}}}}} // laziness

        // This code is likely never getting reached. If it is reached, it prints out the ideal course found.
        int courseNum = 0;
        for (ArrayList<Course> winningCourseSelection : winningCourseList) {
            courseNum++;
            System.out.print("Course list number " + courseNum + ": ");
            for (Course course : winningCourseSelection) {
                System.out.print(course.getName().substring(0, 9) + ", ");
            }
            System.out.println("");
        }
    }

    private static void findCoursesDivideThenDP(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, ArrayList<Course> coursesTaken) {
        // A method for dividing the courses into individual seasons, then doing dynamic programming on each season.
        // You can try this method a few times and see the number of requirements it fulfills. In my case, the max
        // amount of requirements that can be fulfilled is 22, and this method tends to get close. Sometimes, it even
        // reaches it, depending on the layout of the parameters. It is fast, but not optimal.

        // All courses that could meet at least one requirement.
        ArrayList<Course> coursesForReqsNotMet = new ArrayList<Course>();
        for (Map.Entry<Requirement, Integer> requirement: requirementsNotMet.entrySet()) {
            coursesForReqsNotMet.addAll(requirement.getKey().getNecessaryCourses());            
        }
        ArrayList<Course> springCourses = new ArrayList<Course>();
        ArrayList<Course> summerCourses = new ArrayList<Course>();
        ArrayList<Course> winterCourses = new ArrayList<Course>();
        ArrayList<Course> fallCourses = new ArrayList<Course>();
        
        // Divide courses into their respective seasons (if a course has multiple respective seasons, repeat it)
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

        // For spring courses, run a dynamic programming algorithm to find the ideal course list.
        // In other words, find the courses in spring that meet the most amount of requirements.
        System.out.println("Spring");
        Set<Requirement> requirementsNotMetList = requirementsNotMet.keySet();
        List<Course> coursesFound = findBestCourses(requirementsNotMet, requirements, springCourses, Season.SPRING, 5);
        // Remove courses from other lists
        for (Course course : coursesFound) {
            summerCourses.remove(course);
            fallCourses.remove(course);
            winterCourses.remove(course);
            springCourses.remove(course);
        }
        coursesTaken.addAll(coursesFound);

        // Update map of requirements not met.
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        coursesFound.clear();


        // Repeat above for summer
        System.out.println("\nSummer");
        coursesFound = findBestCourses(requirementsNotMet, requirements, summerCourses, Season.SUMMER, 2);
        for (Course course : coursesFound) {
            fallCourses.remove(course);
            winterCourses.remove(course);
            springCourses.remove(course);
        }
        coursesTaken.addAll(coursesFound);
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        coursesFound.clear();


        // Repeat above for fall
        System.out.println("\nFall");
        coursesFound = findBestCourses(requirementsNotMet, requirements, fallCourses, Season.FALL, 5);
        for (Course course : coursesFound) {
            winterCourses.remove(course);
            springCourses.remove(course);
        }
        coursesTaken.addAll(coursesFound);
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        coursesFound.clear();

        // Repeat above for winter
        System.out.println("\nWinter");
        coursesFound = findBestCourses(requirementsNotMet, requirements, winterCourses, Season.WINTER, 5);
        for (Course course : coursesFound) {
            springCourses.remove(course);
        }
        coursesTaken.addAll(coursesFound);

        // Repeat above for last spring
        System.out.println("\nSpring 2");
        requirementsNotMet = checkRequirements(requirements, coursesTaken);
        coursesFound = findBestCourses(requirementsNotMet, requirements, springCourses, Season.SPRING, 4); 
        coursesTaken.addAll(coursesFound);
        requirementsNotMet = checkRequirements(requirements, coursesTaken);

        // Print any requirements not met
        System.out.println("\nRequirements not met:");
        for (Map.Entry<Requirement, Integer> req : requirementsNotMet.entrySet()) {
            System.out.println(req.getKey().getRequirementName());
        }   

        // See what requirements are fulfilled and how many
        System.out.println("\nRequirements met:");
        int reqsFulfilled = Requirement.whatFulfillsIt(requirementsNotMetList, coursesTaken);
        System.out.println("Total number of requirements fulfilled: " + reqsFulfilled);
    }

    private static void findCoursesDPThenDivide(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, ArrayList<Course> coursesTaken) {
        // This method finds an optimal course path (there may be many), then attempts to divide it into seasons.
        // If it cannot, it lets the user know which courses cannot be fit into seasons. This function will always
        // find an ideal solution, but takes slightly longer than the Naive or DivideThenDP methods

        // Find all courses that that fulfill at least one requirement that is not met
        ArrayList<Course> coursesForReqsNotMet = new ArrayList<Course>();
        for (Map.Entry<Requirement, Integer> requirement: requirementsNotMet.entrySet()) {
            coursesForReqsNotMet.addAll(requirement.getKey().getNecessaryCourses());            
        }

        ArrayList<Course> futureCoursesTaken = new ArrayList<Course>();
        System.out.println("Requirements fulfilled by each course:\n");
        int reqsFulfilled = 0;

        // Loop through the DP algorithm since the DP algorithm may miss a few courses on its first run
        // due to requirements that need more than one course to complete (such as many elective requirements)
        while (!requirementsNotMet.isEmpty()) {
            Set<Requirement> requirementsNotMetList = requirementsNotMet.keySet();
            // Run dp algorithm
            List<Course> coursesFound = findBestCourses(requirementsNotMet, requirements, coursesForReqsNotMet, Season.TBD, 22); 
           
            // Update requirement map and lists of courses taken
            // futureCoursesTaken consists of all new courses taken (not yet taken by the user, courses the user plans to take)
            // coursesTaken includes all new and old courses taken (courses taken by the user and courses the user will take)
            coursesTaken.addAll(coursesFound);
            requirementsNotMet = checkRequirements(requirements, coursesTaken);
            futureCoursesTaken = union(futureCoursesTaken, coursesFound);
            coursesTaken = union(coursesTaken, coursesFound);

            // See what requirements are fulfilled and how many
            reqsFulfilled += Requirement.whatFulfillsIt(requirementsNotMetList, coursesTaken);

            // Make it so you cannot take the same course twice
            coursesForReqsNotMet.removeAll(coursesTaken);
        }
        System.out.println("Total number of requirements fulfilled: " + reqsFulfilled);

        // Print out future courses to take
        int i = 0;
        System.out.println("\n");
        for (Course c : futureCoursesTaken) {
            i++;
            System.out.println("Course " + i + ": " + c.getName());
        }

        // Split the courses into seasons
        ArrayList<Course> futureFallCourses = new ArrayList<>();
        ArrayList<Course> futureWinterCourses = new ArrayList<>();
        ArrayList<Course> futureSpringCourses = new ArrayList<>();
        ArrayList<Course> futureSummerCourses = new ArrayList<>();

        int[] seasonCapacities = {5, 5, 9, 2}; // Edit based on capacities (fall, winter, spring, summer)
        MaxFlow.fillSeasonLists(futureCoursesTaken, futureFallCourses, futureWinterCourses, futureSpringCourses, futureSummerCourses, seasonCapacities);

        // Print out each season's future courses
        System.out.print("\nFuture Spring Courses: ");
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

        ArrayList<Course> combinedCourseList = new ArrayList<Course>();
        combinedCourseList.addAll(futureFallCourses);
        combinedCourseList.addAll(futureWinterCourses);
        combinedCourseList.addAll(futureSpringCourses);
        combinedCourseList.addAll(futureSummerCourses);
        futureCoursesTaken.removeAll(combinedCourseList);
        
        // Print out courses that don't fit into seasons
        System.out.println("\n\nCourses that cannot fit given your season capacity requirements (shown below): " + "\nFall: " + seasonCapacities[0]
                            + "\nWinter: " + seasonCapacities[1] + "\nSpring: " + seasonCapacities[2] + "\nSummer: " + seasonCapacities[3]);
        for (Course courseLeftOut : futureCoursesTaken) {
            System.out.println(courseLeftOut.getName().substring(0, courseLeftOut.getName().indexOf(".")) + ", ");
        }
        if (futureCoursesTaken.size() == 0) {
            System.out.println("None! All courses fit!");
        }
    }

    
    private static List<Course> findBestCourses(Map<Requirement, Integer> requirementsNotMet, List<Requirement> requirements, ArrayList<Course> courses, Season season, int numCourses) {
        // A dynamic programming approach to finding the best course list, explained in more detail below.
        // Note that numCourses is simply the max number of courses that one is willing to take, so if one wants
        // to find an optimal course path to fulfill all of their requirements, they should set it to a number
        // higher than the number of courses that they would need to take if they fulfilled each requirement one by one.
        // This method will find the optimal course path given numCourses courses and then trim it down to the real optimal course path.

        // Find all requirements fulfilled by each course 
        Map<Course, ArrayList<Requirement>> requirementsFulfilledByCourse = new HashMap<Course, ArrayList<Requirement>>();
        for (Course course: courses) {
            ArrayList<Requirement> requirementsFulfilled = new ArrayList<Requirement>();
            for (Requirement req: requirementsNotMet.keySet()) {
                if (req.getNecessaryCourses().contains(course)) {
                    requirementsFulfilled.add(req);
                }
            }
            requirementsFulfilledByCourse.put(course, requirementsFulfilled);
            
        }


        // Definition: dp[i, j] = Max number of requirements that can be fulfilled with i courses using course j
        // Base case(s): dp[1, j] = number of requirements fulfilled by course j
        // Solution: Max{dp[numCourses, j]} for 0 <= j <= size of course list
        // Formula: dp[i, j] = Max{dp[i-1, k] + number of unique requirements fulfilled 
        //                         by course j (not already fulfilled by the courses in dp[i-1, k]) }
        
        List<Course> bestCourses = new ArrayList<Course>();
        ArrayList<ArrayList<RequirementEntry>> dp = new ArrayList<>();
        
        // initializing dp 2d array
        for (int i = 0; i < numCourses+1; i++) {
            ArrayList<RequirementEntry> courseList = new ArrayList<>();
            for (int j = 0; j < courses.size(); j++) {
                courseList.add(new RequirementEntry());
            }
            dp.add(courseList);
        }

        // base case
        for (int j = 0; j < courses.size(); j++) {
            dp.get(1).set(j, new RequirementEntry(requirementsFulfilledByCourse.get(courses.get(j)), j));
        }

        // dp algorithm
        for (int i = 2; i < numCourses+1; i++) {
            for (int j = 0; j < courses.size(); j++) {
                for (int k = 0; k < courses.size(); k++) {
                    ArrayList<Requirement> uniqueRequirements = new ArrayList<Requirement>();
                    uniqueRequirements.addAll((dp.get(i-1).get(k).getRequirements()));
                    uniqueRequirements = union(uniqueRequirements, requirementsFulfilledByCourse.get(courses.get(j)));
                    
                    // Loop through all values for dp[i-1, k] to find max number of unique requirements fulfilled
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

        // Find solution at Max{dp[numCourses, j]} for 0 <= j <= size of course list
        int maxReqs = 0;
        ArrayList<Integer> bestCourseIntegerList = new ArrayList<Integer>();
        for (int j = 0; j < courses.size(); j++) {
            if (dp.get(numCourses).get(j).getRequirements().size() > maxReqs) {
                maxReqs = dp.get(numCourses).get(j).getRequirements().size();
                bestCourseIntegerList = dp.get(numCourses).get(j).getCourses();
            }
        }
        
        // Convert solution (integerized) to actual course objects
        for (int courseInt : bestCourseIntegerList) {
            if (!bestCourses.contains(courses.get(courseInt))) {
                bestCourses.add(courses.get(courseInt));
            }
        }

        // If a season is given, print out course information for that season
        if (season != Season.TBD) {
            int i = 0;
            for (Course c: bestCourses) {
                i++;
                System.out.println("Course " + i + ": " + c.getName());
            }
        }
        else {
            // If not, remove the last element of the list (trim)
            bestCourses.remove(bestCourses.size()-1);
        }
        return bestCourses;
    }


    private static <T> ArrayList<T> union(List<T> list1, List<T> list2) {
        // Find then union of two lists
        Set<T> set = new HashSet<T>();

        set.addAll(list1);
        set.addAll(list2);

        return new ArrayList<T>(set);
    } 

    private static void initializeCscRequirements() {
        // Add all computer science major requirements to the cscRequirements map (static variable outside this function)

        // Major
        cscRequirements.put("csc101 4", Arrays.asList("csc101"));
        cscRequirements.put("csc123 4", Arrays.asList("csc123"));
        cscRequirements.put("csc202 4", Arrays.asList("csc202"));
        cscRequirements.put("csc203 4", Arrays.asList("csc203"));
        cscRequirements.put("csc225 4", Arrays.asList("csc225"));
        cscRequirements.put("csc248 4", Arrays.asList("csc248"));
        cscRequirements.put("ethics 4", Arrays.asList("csc300", "phil323"));
        cscRequirements.put("csc307 4", Arrays.asList("csc307")); // recommended given AI concentration
        cscRequirements.put("security 4", Arrays.asList("csc321", "csc323", "csc325"));
        cscRequirements.put("csc349 4", Arrays.asList("csc349"));
        cscRequirements.put("csc357 4", Arrays.asList("csc357"));
        cscRequirements.put("networks/distributed 4", Arrays.asList("csc364", "cpe464"));
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
        // Add all Spanish major requirements to the spanRequirements map (static variable outside this function)
        
        // Major
        spanRequirements.put("span201 4", Arrays.asList("span201"));
        spanRequirements.put("span202 4", Arrays.asList("span202"));
        spanRequirements.put("span3 4", Arrays.asList("span203", "span206"));
        spanRequirements.put("span207 4", Arrays.asList("span207"));
        spanRequirements.put("span233 4", Arrays.asList("span233"));
        spanRequirements.put("span301 4", Arrays.asList("span301"));
        spanRequirements.put("span302 4", Arrays.asList("span302"));

        // The following requirement is waived due to study abroad
        // spanRequirements.put("span_300s 12", Arrays.asList("span303", "span305", "span307", "span340", "span390"));
        
        spanRequirements.put("span_400s 4", Arrays.asList("span402", "span410", "span416", "span470")); // would be 8, but 4 waived due to study abroad
        
        // Note: I am not adding capstone or "minor and upper div span" courses as these are waived as part of my double major
    }

    private static ArrayList<String> populateCoursesTakenByCode() {
        // Put in all of the courses I have taken that are relevant to my two majors
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
        // Given the coursesTakenByCode list (of strings), convert each course taken to the 
        // actual course object associated and return a list with the course objects
        ArrayList<Course> coursesTaken = new ArrayList<Course>();
        for (String courseCode : coursesTakenByCode) {
            String prefix = courseCode.replaceAll("[0-9]", "");
            String num = courseCode.replaceAll("[^0-9]", "");
            coursesTaken.add(cs.getCourseByCode(prefix, num));
        }
        return coursesTaken;
    }
}
