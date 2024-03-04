package com.practicalalgorithms;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.*;


public class CourseScraper {
    // Class that does all functions relating to scraping the cal poly catalog

    Map<String, List<Course>> coursesByPrefix = new HashMap<>(); // map of each course prefix (csc, econ, etc.) to its respective courses (csc101, econ222, etc.)
    
    // Populate the coursesByPrefix map by first finding all course prefixing, then finding all courses within each prefix
    public void populateCoursesByPrefix() {
        try {
            List<String> urls = extractHyperlinks("https://catalog.calpoly.edu/coursesaz/"); // find prefixes
            for (String url: urls) {
                String prefix = url.substring(url.substring(1).indexOf('/') + 2); // get prefix
                List<Course> courses = scrapeCourseData("https://catalog.calpoly.edu/coursesaz/" + prefix); // find courses that belong to prefix
                this.coursesByPrefix.put(prefix.substring(0, prefix.length() - 1), courses); // populate map
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, Course> getCoursesFromMajor(String url) {
        // Function that takes a major's curriculum page (the required and elective courses) and scrapes it
        try {
            // Fetch the HTML content from the URL
            Document document = Jsoup.connect(url).get();

            // Initialize a HashMap to store the course objects
            Map<String, Course> courseMap = new HashMap<>();

            // Select all elements with the class "bubblelink code"
            Elements courseElements = document.select(".bubblelink.code");

            // Iterate through each course element
            for (Element courseElement : courseElements) {
                // Extract the course code (like csc101, econ222) from the onclick attribute
                String onClickValue = courseElement.attr("onclick");
                String courseCode = onClickValue.substring(findLastIndex(onClickValue, ",") + 3, onClickValue.length() - 3);

                if (courseCode.length() < 2) {
                    continue;
                }
                // Split the course code into prefix and number
                String[] parts = courseCode.split("\\s+");
                String prefix = parts[0];
                String number = parts[1];

                Course course = getCourseByCode(prefix, number); // get the course object

                // Create a lowercase string representing the course name (e.g., 'csc101')
                String courseNameKey = prefix.toLowerCase() + number;

                // Store the course object in the map
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
        // Find course by pipelining map data into a stream
        return (Course) this.coursesByPrefix.get(prefix.toLowerCase()).stream()
                            .filter(course -> (prefix.toUpperCase() + " " + number).equals(course.getName().substring(0, prefix.length() + number.length() + 1)))
                            .collect(Collectors.toList()).get(0);
    }

    private static List<String> extractHyperlinks(String url) throws IOException {
        // Function to find prefixes from the cal poly course catalog "courses" section
        List<String> urls = new ArrayList<>();
        Document document = Jsoup.connect(url).get();

        // Select the hyperlinks with the class "sitemaplink" within the specific part of the page
        Elements hyperlinks = document.select("tbody tr td a.sitemaplink");

        // For each hyperlink, get the "href" component (which is the prefix) and add that to the urls list
        for (Element hyperlink : hyperlinks) {
            String href = hyperlink.attr("href");
            urls.add(href);
        }

        return urls;
    }

    private static List<Course> scrapeCourseData(String url) throws IOException {
        // Function to return a list of courses by scraping the data from each course's entry in the cal poly catalog
        List<Course> courses = new ArrayList<>();
        Document document = Jsoup.connect(url).get();

        // Select course elements on the webpage
        Elements courseElements = document.select(".courseblock");

        // For each course entry, extract relevant data (name, GEs, seasons offered, etc.)
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
        // Function that takes the text given at the beginning of each course element and finds what GE it fulfills (in 2021-later catalogs)
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
        // Takes the text at the beginning of a course element and finds what seasons it is offered in 
        Season[] seasonList = new Season[4];

        int indexOfColon = seasonText.indexOf(':');
        int endIndex = indexOfColon + 12;
        
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
        // Helper function to find the last index of any of the characters in last in the string s
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