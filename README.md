# optimal-courses
A tool to find the optimal (shortest) course path given the following:
  1. Courses already taken / requirements already met
  2. Courses needed / requirements needed
  3. Possible courses (and information about them including what quarters they are offered, what requirements they can be used for, and how many units they count for)


Two algorithms used:
- Dynamic programming - O((numCourses - 1) * courseList.size()) where courseList is the number of possible courses to take and numCourses is the "depth" that the user wants the algorithm to search
- Edmonds-Karp for max flow - O(|V||E|^2) = O((numCourses + numQuarters + 2) * ((numQuarters + 1) * numCourses + numQuarters)^2)


Note: Absolute-Optimal branch is similar except that it contains a function that finds the optimal courses and makes sure that these courses fit the season requirements
