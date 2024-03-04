package logicStuff.learning.datasets;

import ida.utils.Sugar;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * To safe more memory, you may reimplement this by using consecutive intervals.
 * <p>
 * Created by martin.svatos on 22. 2. 2018.
 */
public class Coverage implements Iterable<Integer> {

    private final int[] idxs; // immutable!
    private final Integer hash;

    public Coverage(Set<Integer> set) {
        this.idxs = set.stream().mapToInt(i -> i).sorted().toArray(); // need for equals, otherwise implement it via immutable sets
        this.hash = set.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Coverage coverage = (Coverage) o;

        return Arrays.equals(idxs, coverage.idxs);
    }

    @Override
    public int hashCode() {
//        would be permutation specific :(
//        if (null == hash) {
//            hash = Arrays.hashCode(idxs);
//        }
        return hash;
    }

    @Override
    public String toString() {
        return "Coverage{" +
                "size=" + idxs.length +
                ",hash=" + hash +
                ",idxs=" + Arrays.toString(idxs) +
                '}';
    }

    public IntStream stream() {
        return Arrays.stream(idxs);
    }

    @Override
    public Iterator<Integer> iterator() {
        return IntStream.of(idxs).boxed().iterator();
    }

    @Override
    public void forEach(Consumer<? super Integer> action) {
        for (int idx = 0; idx < idxs.length; idx++) {
            action.accept(idxs[idx]);
        }
    }

    @Override
    public Spliterator<Integer> spliterator() {
        throw new UnsupportedOperationException();
    }

    public Set<Integer> asSet() {
        return Arrays.stream(idxs).boxed().collect(Collectors.toSet());
    }

    public static Coverage create(Collection<Integer> coverage) {
        Set<Integer> covers = Sugar.setFromCollections(coverage);
        return new Coverage(covers);
    }

    public static Coverage create(IntStream intStream) {
        return new Coverage(intStream.boxed().collect(Collectors.toSet()));
    }

    public int size() {
        return idxs.length;
    }

    public Coverage removeAll(Coverage remove) {
        Set<Integer> set = asSet();
        set.removeAll(remove.asSet());
        return CoverageFactory.getInstance().get(set);
    }

    public boolean isEmpty() {
        return idxs.length < 1;
    }

    public boolean isNotEmpty() {
        return idxs.length > 0;
    }

    public Coverage addAll(Coverage extension) {
        return CoverageFactory.getInstance().get(operation(asSet(), extension.asSet(), (first, second) -> first.addAll(second)));
    }

    public Coverage intersection(Coverage coverage) {
        return CoverageFactory.getInstance().get(operation(asSet(), coverage.asSet(), (first, second) -> first.retainAll(second)));
    }

    private <T> Set<T> operation(Set<T> first, Set<T> second, BiConsumer<Set<T>, Set<T>> operation) {
        operation.accept(first, second);
        return first;
    }

}
