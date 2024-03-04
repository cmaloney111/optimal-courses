package com.practicalalgorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MaxFlow {
    // A class for implementing the Ford-Fulkerson algorithmm for finding max flow in a network
    private static final int INF = Integer.MAX_VALUE;

    // A directed edge with a capacity and a flow (part of the problem setup)
    public static class FlowEdge {
        int from, to, capacity, flow;

        public FlowEdge(int from, int to, int capacity) {
            this.from = from;
            this.to = to;
            this.capacity = capacity;
            this.flow = 0;
        }
    }

    public static void fillSeasonLists(List<Course> futureCoursesTaken, ArrayList<Course> futureFallCourses,
            ArrayList<Course> futureWinterCourses, ArrayList<Course> futureSpringCourses,
            ArrayList<Course> futureSummerCourses, int[] seasonCapacities) {
        // Given 4 season lists and a list of courses, fill the season lists with the list of courses where each course
        // can only go into a season list if the course is offered during that season. Also, max capacities for each season
        // are given. This problem is modeled as a graph where:
        // - Each course is a node. Each season is a node. There are also two extra nodes, a source node and a sink node
        // - The source node is connected by edges to the course nodes with an outgoing edge of capacity 1
        // - The course nodes are connected by an edge to season nodes with an outgoing edge of infinite capacity
        // - The season nodes are connected by an edge to the sink node with their corresponding capacities.
        // - Edges of capacity 0 are added in opposite directions to each of the previously mentioned edges.
        // After setting up this graph, the max flow in the network is found using the Ford-Fulkerson algorithm/method.
        // Then, for each course node, we analyze each of its outgoing edges and find the one with positive flow.
        // The node on the other end of the edge with positive flow will be the node corresponding to the season where
        // we should place the course node. This leads us to populate all course lists optimally. 

        int numCourses = futureCoursesTaken.size(); // (0 to numCourses-1 in the graph's adjacency list)
        int numSeasons = 4; // Fall, Winter, Spring, Summer (numCourses to numCourses + 3 in the graph's adjacency list)
        int numNodes = numCourses + numSeasons + 2; // Courses + Seasons + Source + Sink 

        int source = numNodes - 2; // position of source node in adjacency list (second to last node)
        int sink = numNodes - 1; // position of sink node in adjacency list (last node)

        List<FlowEdge>[] graph = new List[numNodes]; // create an adjacency list to represent the graph
        for (int i = 0; i < numNodes; i++) {
            graph[i] = new ArrayList<>(); // list to contain outgoing edges
        }

        // Connect source to courses
        for (int i = 0; i < numCourses; i++) {
            graph[source].add(new FlowEdge(source, i, 1));
            graph[i].add(new FlowEdge(i, source, 0));
        }

        // Connect courses to seasons
        for (int i = 0; i < numCourses; i++) {
            Course course = futureCoursesTaken.get(i);
            Season[] quartersOffered = course.getQuartersOffered();

            // If course offered in season, connect course to that season's node
            for (int j = 0; j < numSeasons; j++) {
                if (quartersOffered[j] != null) {
                    graph[i].add(new FlowEdge(i, numCourses + j, INF));
                    graph[numCourses + j].add(new FlowEdge(numCourses + j, i, 0));
                }
            }
        }

        // Connect seasons to sink
        for (int i = 0; i < numSeasons; i++) {
            int capacity;
            switch (i) {
                case 0: // Fall
                    capacity = seasonCapacities[0];
                    break;
                case 1: // Winter
                    capacity = seasonCapacities[1];
                    break;
                case 2: // Spring
                    capacity = seasonCapacities[2];
                    break;
                case 3: // Summer
                    capacity = seasonCapacities[3];
                    break;
                default:
                    capacity = 0;
            }
            graph[numCourses + i].add(new FlowEdge(numCourses + i, sink, capacity));
            graph[sink].add(new FlowEdge(sink, numCourses + i, 0));
        }

        maxFlow(graph, source, sink);

        // Retrieve allocated courses based on the flow
        for (int i = 0; i < numCourses; i++) {
            for (FlowEdge edge : graph[i]) {
                if (edge.flow > 0) {
                    switch (edge.to - numCourses) {
                        case 0:
                            futureFallCourses.add(futureCoursesTaken.get(i));
                            break;
                        case 1:
                            futureWinterCourses.add(futureCoursesTaken.get(i));
                            break;
                        case 2:
                            futureSpringCourses.add(futureCoursesTaken.get(i));
                            break;
                        case 3:
                            futureSummerCourses.add(futureCoursesTaken.get(i));
                            break;
                    }
                }
            }
        }
    }

    public static void maxFlow(List<FlowEdge>[] graph, int source, int sink) {
        // Use the Ford-Fulkerson method (specifically the Edmonds-Karp algorithm) to find the max flow in the flow network
        // The algorithm involves traversing the residual graph, finding augmenting paths using BFS, and updating the flow
        // in the network until no more augmenting paths can be found. Here I will explain some terms:
        // Augmenting Path:
        // - An augmenting path is a path in the residual graph from the source to the sink along 
        // which additional flow can be sent. Starting from the source, the algorithm searches for
        // an augmenting path using BFS (Breadth-First Search) in the residual graph. The presence 
        // of an augmenting path implies that there is room to increase the flow in the network.
        // Residual Graph:
        // - The residual graph is a modified version of the original flow network (the graph passed in as a
        // parameter) that accounts for the current flow. It contains edges with capacities that represent 
        // the remaining capacity for additional flow. For each original edge in the network, the residual 
        // graph contains a backward edge with capacity equal to the current flow on the original edge.
        // More specifically, consider an edge (u, v) with capacity C and current flow F. In the residual graph,
        // the forward edge (u, v) has residual capacity C - F and the backward edge (v, u) has residual capacity F.
        // The residual graph is updated during each iteration of the algorithm based on the flow adjustments 
        // along the augmenting path. Here's some pseudocode to explain how exactly the residual graph is updated:
        
        // for each edge (u, v) in the augmenting path:
        //     residual_capacity_forward_edge(u, v) = capacity(u, v) - flow(u, v)
        //     residual_capacity_backward_edge(v, u) = flow(u, v)
        //     flow(u, v) += min_capacity
        //     flow(v, u) -= min_capacity

        while (true) {
            // Array to store parent vertices in the augmenting path
            int[] parent = new int[graph.length];
            Arrays.fill(parent, -1);
            
            // Queue for BFS traversal to find augmenting paths
            Queue<Integer> queue = new LinkedList<>();
            queue.offer(source);
            parent[source] = source;
    
            // BFS to find augmenting paths from source to sink
            while (!queue.isEmpty() && parent[sink] == -1) {
                int current = queue.poll();
                for (FlowEdge edge : graph[current]) {
                    int next = edge.to;
                    // Check if the edge is not saturated and the next vertex is not visited
                    if (parent[next] == -1 && edge.capacity > edge.flow) {
                        parent[next] = current;
                        queue.offer(next);
                    }
                }
            }
    
            // If no augmenting path is found, exit the loop
            if (parent[sink] == -1) {
                break;
            }
    
            // Find the minimum capacity along the augmenting path
            int minCapacity = INF;
            for (int current = sink; current != source; current = parent[current]) {
                // Find edges in the current node's parents that go to the current node
                for (FlowEdge edge : graph[parent[current]]) {
                    if (edge.to == current) {
                        // Update the minimum capacity
                        minCapacity = Math.min(minCapacity, edge.capacity - edge.flow);
                        break;
                    }
                }
            }
    
            // Update the flow along the augmenting path
            for (int current = sink; current != source; current = parent[current]) {
                for (FlowEdge edge : graph[parent[current]]) {
                    if (edge.to == current) {
                        // Increase the flow on forward edge
                        edge.flow += minCapacity;
                        break;
                    }
                }
    
                for (FlowEdge edge : graph[current]) {
                    if (edge.to == parent[current]) {
                        // Decrease the flow on backward edge
                        edge.flow -= minCapacity;
                        break;
                    }
                }
            }
        }
    }
}
