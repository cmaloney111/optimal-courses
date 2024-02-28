package com.practicalalgorithms;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.*;


public class CourseScraper {

    Map<String, List<Course>> coursesByPrefix = new HashMap<>();
    public void populateCoursesByPrefix() {
        try {
            List<String> urls = extractHyperlinks("https://catalog.calpoly.edu/coursesaz/");
            for (String url: urls) {
                String prefix = url.substring(url.substring(1).indexOf('/') + 2);
                List<Course> courses = scrapeCourseData("https://catalog.calpoly.edu/coursesaz/" + prefix);
                this.coursesByPrefix.put(prefix.substring(0, prefix.length() - 1), courses);    
            }
            

            /* 
            for (Map.Entry<String, List<Course>> prefix : coursesByPrefix.entrySet()) {
                System.out.print("Prefix: " + prefix.getKey() + ", Value: ");
                Course course = prefix.getValue().get(0);
                System.out.println(course.getName() + ", offered in: " + Arrays.toString(course.getQuartersOffered()) + ", units: " + course.getUnits() + ", GE: " + course.getGE() + ", isUSCP: " + course.isUSCP() + ", isGWR: " + course.isGWR());
            }
            Course course = (Course) coursesByPrefix.get("th").stream()
                            .filter(co -> "TH 201".equals(co.getName().substring(0, 6)))
                            .collect(Collectors.toList()).get(0);
            System.out.println(course.getName() + ", offered in: " + Arrays.toString(course.getQuartersOffered()) + ", units: " + course.getUnits() + ", GE: " + course.getGE() + ", isUSCP: " + course.isUSCP() + ", isGWR: " + course.isGWR());
            */
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Course> getCoursesFromMajor(String url) {
        try {
            // "https://catalog.calpoly.edu/collegesandprograms/collegeofengineering/computersciencesoftwareengineering/bscomputerscience/";

            // Fetch the HTML content from the URL
            Document document = Jsoup.connect(url).get();

            // Initialize a HashMap to store the course objects
            Map<String, Course> courseMap = new HashMap<>();

            // Select all elements with the class "bubblelink code"
            Elements courseElements = document.select(".bubblelink.code");

            // Iterate through each course element
            for (Element courseElement : courseElements) {
                // Extract the course code (e.g., 'CSC 101') from the onclick attribute
                String onClickValue = courseElement.attr("onclick");
                String courseCode = onClickValue.substring(findLastIndex(onClickValue, ",") + 3, onClickValue.length() - 3);

                if (courseCode.length() < 2) {
                    continue;
                }
                // Split the course code into prefix and number
                String[] parts = courseCode.split("\\s+");
                String prefix = parts[0];
                String number = parts[1];

                // Run the getCourseByName function
                Course course = getCourseByCode(prefix, number);

                // Create a lowercase string representing the course name (e.g., 'csc101')
                String courseNameKey = prefix.toLowerCase() + number;

                // Store the course object in the HashMap
                courseMap.put(courseNameKey, course);
            }

            // Now the courseMap contains the course objects mapped to lowercase course names
            return courseMap;

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Course getCourseByCode(String prefix, String number) {
        // System.out.println("Prefix: " + prefix + ", number: " + number);
        return (Course) this.coursesByPrefix.get(prefix.toLowerCase()).stream()
                            .filter(course -> (prefix.toUpperCase() + " " + number).equals(course.getName().substring(0, prefix.length() + number.length() + 1)))
                            .collect(Collectors.toList()).get(0);
    }

    private static List<String> extractHyperlinks(String url) throws IOException {
        List<String> urls = new ArrayList<>();
        Document document = Jsoup.connect(url).get();

        // Select the hyperlinks with the class "sitemaplink" within the specific part of the page
        Elements hyperlinks = document.select("tbody tr td a.sitemaplink");

        for (Element hyperlink : hyperlinks) {
            String href = hyperlink.attr("href");
            urls.add(href);
        }

        return urls;
    }

    private static List<Course> scrapeCourseData(String url) throws IOException {
        List<Course> courses = new ArrayList<>();
        Document document = Jsoup.connect(url).get();

        // Select course elements on the webpage
        Elements courseElements = document.select(".courseblock");

        for (Element courseElement : courseElements) {
            Course course = extractCourse(courseElement);
            courses.add(course);
        }

        return courses;
    }

    private static Course extractCourse(Element courseElement) {
        // Extract relevant information from the courseElement
        String nameUnitsText = courseElement.select(".courseblocktitle strong").text();
        int unit_index = findLastIndex(nameUnitsText, "0123456789");
        int units = Integer.parseInt(nameUnitsText.substring(unit_index, unit_index + 1));
        String name = nameUnitsText.substring(0, findLastIndex(nameUnitsText, "."));
    
        String seasonReqText = courseElement.select(".courseextendedwrap").text();
        Season[] seasonList = extractSeasonList(seasonReqText);

        boolean isUSCP = false;
        boolean isGWR = false;

        if (seasonReqText.contains("GWR") && seasonReqText.indexOf("GWR") != (seasonReqText.indexOf("ompletion of GWR") + 13)) {
            isGWR = true;
        }
        if (seasonReqText.contains("USCP")) {
            isUSCP = true;
        }

        String GEText = courseElement.select(".courseblockdesc").text();
        
        GenEdArea GE = GenEdArea.NONE;
        if (GEText.contains("Fulfills GE")) {
            GE = extractGE(GEText.substring(GEText.indexOf("Fulfills GE")));
        }
        
        // Create and return a Course object
        return new Course(name, GE, seasonList, units, isUSCP, isGWR);
    }

    private static GenEdArea extractGE(String GEText) {
        if (GEText.charAt(12) == 'U') {
            return GenEdArea.valueOf("UD" + GEText.charAt(27));
        }
        else if (GEText.charAt(12) == 'A') {
            if (GEText.charAt(18) == ' ' || GEText.charAt(18) == '.') {
                return GenEdArea.valueOf(GEText.substring(17, 18));
            }
            try {
                return GenEdArea.valueOf(GEText.substring(17, 19));
            } catch (IllegalArgumentException e) {
                try {
                    return GenEdArea.valueOf("UD" + GEText.charAt(32));
                } catch (IllegalArgumentException ex) {
                    return GenEdArea.valueOf(GEText.substring(18, 20));
                }
            }
        }
        else {
            return GenEdArea.NONE;
        }
    }

    private static Season[] extractSeasonList(String seasonText) {
        Season[] seasonList = new Season[4];

        int indexOfColon = seasonText.indexOf(':');
        int endIndex = indexOfColon + 12;
        
        // Check if endIndex exceeds the length of the string
        if (endIndex > seasonText.length()) {
            endIndex = seasonText.length();
        }
        
        String seasons = seasonText.substring(indexOfColon, endIndex);
        if (seasons.contains("F,") || seasons.contains("F ") || (seasons.contains("F") && endIndex == seasonText.length())) {
            seasonList[0] = Season.FALL;
        }
        if (seasons.contains("W,") || seasons.contains("W ") || (seasons.contains("W") && endIndex == seasonText.length())) {
            seasonList[1] = Season.WINTER;
        }
        if (seasons.contains("SP,") || seasons.contains("SP ") || (seasons.contains("SP") && endIndex == seasonText.length())) {
            seasonList[2] = Season.SPRING;
        }
        if (seasons.contains("SU,") || seasons.contains("SU ") || (seasons.contains("SU") && endIndex == seasonText.length())) {
            seasonList[3] = Season.SUMMER;
        }
        if (seasons.contains("TBD")) {
            seasonList[0] = Season.TBD;
        }
        return seasonList;

    }
    private static int findLastIndex(String s, String last) {
        for (int i = s.length() - 1; i >= 0; i--) {
            for (int j = 0; j < last.length(); j++) {
                if (last.charAt(j) == s.charAt(i)) {
                    return i;
                }
            }
        }
        return -1; // No digit found
    }

    public Map<String, List<Course>> getCoursesByPrefix() {
        return this.coursesByPrefix;
    }

}