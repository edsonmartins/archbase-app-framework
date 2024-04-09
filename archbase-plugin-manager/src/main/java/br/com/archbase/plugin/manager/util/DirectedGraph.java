package br.com.archbase.plugin.manager.util;

import java.util.*;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Consulte a <a href="https://en.wikipedia.org/wiki/Directed_graph"> Wikipedia </a> para obter mais informações.
 */
public class DirectedGraph<V> {

    /**
     * A implementação aqui é basicamente uma lista de adjacências, mas em vez disso
     * de uma matriz de listas, um mapa é usado para mapear cada vértice para sua lista de
     * vértices adjacentes.
     */
    private Map<V, List<V>> neighbors = new HashMap<>();

    /**
     * Adicione um vértice ao gráfico. Nada acontece se o vértice já estiver no gráfico.
     */
    public void addVertex(V vertex) {
        if (containsVertex(vertex)) {
            return;
        }

        neighbors.put(vertex, new ArrayList<>());
    }

    /**
     * Verdadeiro se o gráfico contiver vértice.
     */
    public boolean containsVertex(V vertex) {
        return neighbors.containsKey(vertex);
    }

    public void removeVertex(V vertex) {
        neighbors.remove(vertex);
    }

    /**
     * Adicione uma borda ao gráfico; se nenhum dos vértices existir, ele será adicionado.
     * Esta implementação permite a criação de multi-arestas e auto-loops.
     */
    public void addEdge(V from, V to) {
        addVertex(from);
        addVertex(to);
        neighbors.get(from).add(to);
    }

    /**
     * Remova uma borda do gráfico. Nada acontece se não houver essa vantagem.
     *
     * @throws {@link IllegalArgumentException} se algum dos vértices não existir.
     */
    public void removeEdge(V from, V to) {
        if (!containsVertex(from)) {
            throw new IllegalArgumentException("Vértice inexistente " + from);
        }

        if (!containsVertex(to)) {
            throw new IllegalArgumentException("Vértice inexistente " + to);
        }

        neighbors.get(from).remove(to);
    }

    public List<V> getNeighbors(V vertex) {
        return containsVertex(vertex) ? neighbors.get(vertex) : new ArrayList<>();
    }

    /**
     * Relate (como um mapa) o grau externo (o número de extremidades da cauda adjacentes a um vértice) de cada vértice.
     */
    public Map<V, Integer> outDegree() {
        Map<V, Integer> result = new HashMap<>();
        neighbors.entrySet().forEach(item -> result.put(item.getKey(), item.getValue().size()));
        return result;
    }

    /**
     * Relate (como um {@link Map}) o grau (o número de pontas adjacentes a um vértice) de cada vértice.
     */
    public Map<V, Integer> inDegree() {
        Map<V, Integer> result = new HashMap<>();
        for (V vertex : neighbors.keySet()) {
            result.put(vertex, 0); // todos em graus são 0
        }

        for (Iterator<V> iterator = neighbors.keySet().iterator(); iterator.hasNext(); ) {
            V from = iterator.next();
            // incremento em grau
            neighbors.get(from).forEach(to -> result.put(to, result.get(to) + 1));
        }

        return result;
    }

    /**
     * Relatório (como uma lista) a classificação topológica dos vértices; null para tal tipo.
     * Consulte <a href="https://en.wikipedia.org/wiki/Topological_sorting"> isto </a> para obter mais informações.
     */
    public List<V> topologicalSort() {
        Map<V, Integer> degree = inDegree();

        // determina todos os vértices com zero grau
        Deque<V> zeroVertices = new LinkedBlockingDeque<>(); // pilha tão boa quanto qualquer aqui
        degree.keySet().stream().filter(v -> degree.get(v) == 0).forEach(zeroVertices::push);

        // determina a ordem topológica
        List<V> result = new ArrayList<>();
        while (!zeroVertices.isEmpty()) {
            V vertex = zeroVertices.pop(); // escolha um vértice com zero em grau
            result.add(vertex); // vértice 'v' é o próximo na ordem topológica
            // "remove" o vértice 'v' atualizando seus vizinhos
            for (V neighbor : neighbors.get(vertex)) {
                degree.put(neighbor, degree.get(neighbor) - 1);
                // lembre-se de todos os vértices que agora têm zero grau
                if (degree.get(neighbor) == 0) {
                    zeroVertices.push(neighbor);
                }
            }
        }

        // verifique se usamos todo o gráfico (se não, houve um ciclo)
        if (result.size() != neighbors.size()) {
            return Collections.emptyList();
        }

        return result;
    }

    /**
     * Relata (como uma lista) a classificação topológica reversa dos vértices; null para tal tipo.
     */
    public List<V> reverseTopologicalSort() {
        List<V> list = topologicalSort();
        if (list == null) {
            return Collections.emptyList();
        }

        Collections.reverse(list);

        return list;
    }

    /**
     * Verdadeiro se o gráfico for um dag (gráfico acíclico direcionado).
     */
    public boolean isDag() {
        return topologicalSort() != null;
    }

    /**
     * Representação de string do gráfico.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Iterator<V> iterator = neighbors.keySet().iterator(); iterator.hasNext(); ) {
            V vertex = iterator.next();
            sb.append("\n   ").append(vertex).append(" -> ").append(neighbors.get(vertex));
        }

        return sb.toString();
    }

}
