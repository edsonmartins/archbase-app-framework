package br.com.archbase.plugin.manager.util;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


class DirectedGraphTest {

    private static DirectedGraph<Character> graph;

    @BeforeAll
    public static void setUp() {
        graph = new DirectedGraph<>();

        // adiciona vértice
        graph.addVertex('A');
        graph.addVertex('B');
        graph.addVertex('C');
        graph.addVertex('D');
        graph.addVertex('E');
        graph.addVertex('F');
        graph.addVertex('G');

        // adicionar bordas
        graph.addEdge('A', 'B');
        graph.addEdge('B', 'C');
        graph.addEdge('B', 'F');
        graph.addEdge('D', 'E');
        graph.addEdge('F', 'G');
    }

    @Test
    void reverseTopologicalSort() {
        List<Character> result = graph.reverseTopologicalSort();
        List<Character> expected = Arrays.asList('C', 'G', 'F', 'B', 'A', 'E', 'D');
        assertEquals(expected, result);
    }

    @Test
    void topologicalSort() {
        List<Character> result = graph.topologicalSort();
        List<Character> expected = Arrays.asList('D', 'E', 'A', 'B', 'F', 'G', 'C');
        assertEquals(expected, result);
    }

    @Test
    void inDegree() {
        Map<Character, Integer> result = graph.inDegree();
        Map<Character, Integer> expected = new HashMap<>(7);
        expected.put('A', 0);
        expected.put('B', 1);
        expected.put('C', 1);
        expected.put('D', 0);
        expected.put('E', 1);
        expected.put('F', 1);
        expected.put('G', 1);
        assertEquals(expected, result);
    }

    @Test
    void outDegree() {
        Map<Character, Integer> result = graph.outDegree();
        Map<Character, Integer> expected = new HashMap<>(7);
        expected.put('A', 1);
        expected.put('B', 2);
        expected.put('C', 0);
        expected.put('D', 1);
        expected.put('E', 0);
        expected.put('F', 1);
        expected.put('G', 0);
        assertEquals(expected, result);
    }

}
