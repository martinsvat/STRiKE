package ida.searchPruning.metavo;

import ida.utils.Sugar;
import ida.utils.collections.MultiList;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Simple parameter-values grid generator.
 * <p>
 * Be aware, the iterator and iterable interfaces are implemented only partially to support basic for- and iterator-like traversals. It is not thread safe.
 * <p>
 * The functionality of constraints is not fully tested. It is aimed to prune parameter configurations which violate user specified constraintes.
 * <p>
 * Created by martin.svatos on 17. 1. 2018.
 */
public class ParameterGrid implements Iterator<Map<String, String>>, Iterable<Map<String, String>> {


    private final MultiList<String, Object> parameters;
    private final List<Predicate<Map<String, String>>> constraints;
    private final List<String> keys;
    private Map<String, Integer> currentState;

    public ParameterGrid(MultiList<String, Object> parameters, List<Predicate<Map<String, String>>> constraints) {
        this.parameters = parameters;
        this.constraints = constraints;
        this.keys = Sugar.listFromCollections(parameters.keySet());
        this.currentState = null;
    }

    public String onliner(Map<String, String> state, Predicate<String> keyFilter) {
        return state.entrySet().stream()
                .filter(entry -> keyFilter.test(entry.getKey()))
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .sorted()
                .collect(Collectors.joining(" "));
    }

    public String dPropertiesOnliner(Map<String, String> state) {
        return onliner(state, (str) -> str.startsWith("-D"));
    }

    private Map<String, String> mapToOutput(Map<String, Integer> currentState) {
        return currentState.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> parameters.get(entry.getKey()).get(entry.getValue()).toString()));
    }

    @Override
    public boolean hasNext() {
        return null != computeNext(currentState);
    }

    @Override
    public Map<String, String> next() {
        currentState = computeNext(currentState);
        return mapToOutput(currentState);
    }


    // it could be simplified, with caching, but I'm to lazy right now
    private Map<String, Integer> computeNext(Map<String, Integer> state) {
        if (null == state) {
            if (parameters.values().stream().mapToInt(l -> l.size()).min().orElse(0) < 1) {
                return null;
            }
            Map<String, Integer> initial = keys.stream().collect(Collectors.toMap(key -> key, (key) -> 0));
            while(null != initial && !consistentState(initial)){
                initial = generateSuccessor(initial);
            }
            return initial;
        }
        Map<String, Integer> successor = currentState;
        do {
            successor = generateSuccessor(successor);
        } while (null != successor && !consistentState(successor));
        return successor;
    }

    private Map<String, Integer> generateSuccessor(Map<String, Integer> state) {
        boolean incrementNextParameter = true;
        Map<String, Integer> successor = new HashMap<>();
        for (String key : keys) {
            int nextValue = state.get(key) + 1;
            if (!incrementNextParameter) {
                nextValue = state.get(key);
            } else if (1 == parameters.get(key).size()) {
                nextValue = 0;
            } else if (parameters.get(key).size() == nextValue) {
                nextValue = 0;
                incrementNextParameter = true;
            } else {
                incrementNextParameter = false;
            }
            successor.put(key, nextValue);
        }
        if (incrementNextParameter) {
            return null;
        }
        return successor;
    }

    private boolean consistentState(Map<String, Integer> rawState) {
        if(null == rawState){
            return false;
        }
        Map<String, String> state = mapToOutput(rawState);
        //return constraints.stream().filter(predicate -> predicate.test(state)).count() == constraints.size();
        return constraints.stream().allMatch(predicate -> predicate.test(state));
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEachRemaining(Consumer<? super Map<String, String>> action) {
        throw new UnsupportedOperationException();
    }


    @Override
    public Iterator<Map<String, String>> iterator() {
        return this;
    }


    @Override
    public void forEach(Consumer<? super Map<String, String>> action) {
        while (hasNext()) {
            action.accept(next());
        }
    }

    @Override
    public Spliterator<Map<String, String>> spliterator() {
        throw new UnsupportedOperationException();
    }


    public static void main(String[] args) {
        MultiList<String, Object> parameters = new MultiList<>();
        parameters.putAll("-Dida.logicStuff.constraints.minSupport", Sugar.list(100));
        parameters.putAll("-Dida.logicStuff.constraints.learner", Sugar.list("complete", "sampling5-10-100"));
        parameters.putAll("-Dida.searchPruning.runner.overallLimit", Sugar.list(25 * 60));
        parameters.putAll("-Dida.searchPruning.mining", Sugar.list("bfs", "minSaturatedBfs"));
        parameters.putAll("-Dida.searchPruning.maxDepth", Sugar.list(10));
        parameters.putAll("-Dida.searchPruning.storeOutput", Sugar.list(true));
        parameters.putAll("-Dida.searchPruning.input", Sugar.list("../datasets/splitted/nci_transformed/gi50_screen_KM20L2.txt"));
        parameters.putAll("-Djava.util.concurrent.ForkJoinPool.common.parallelism", Sugar.list(10));

        ParameterGrid grid = ParameterGrid.create(parameters, Sugar.list());
        Iterator<Map<String, String>> iterator = grid.iterator();
        while (iterator.hasNext()) {
            System.out.println(grid.dPropertiesOnliner(iterator.next()));
        }
    }

    public static ParameterGrid create(MultiList<String, Object> parameters, List<Predicate<Map<String, String>>> constraints) {
        return new ParameterGrid(parameters, constraints);
    }

}
