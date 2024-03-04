package ida.pacReasoning.entailment.collectors;

import ida.ilp.logic.Constant;
import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;
import ida.utils.collections.MultiMap;

import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is just a collector for debugging.
 * <p>
 * Created by martin.svatos on 11. 12. 2018.
 */
public class SupportCollector implements Entailed {
    private final Map<Subset, Constant> subsetToConstant;
    private final MultiMap<Literal, Subset> collector;
    private long time;

    public SupportCollector(Map<Subset, Constant> subsetToConstant) {
        this.subsetToConstant = subsetToConstant;
        this.collector = new MultiMap<>();
    }

    public SupportCollector setTime(long time) {
        this.time = time;
        return this;
    }

    @Override
    public long getTime() {
        return this.time;
    }

    @Override
    public Double getEntailedValue(Literal query) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(Literal query, Subset subset) {
        this.collector.put(query, subset);
    }

    @Override
    public String asOutput() {
        return collector.keySet().stream().sorted(Comparator.comparing(Literal::toString))
                .map(literal -> literal + "\t" + toCanon(collector.get(literal)))
                .collect(Collectors.joining("\n"));
    }

    private String toCanon(Set<Subset> subsets) {
        return subsets.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "));
    }

    private String toCanon(Subset subset) {
        return "{" + subsetToConstant.entrySet().stream()
                .filter(entry -> entry.getKey().isSubsetOf(subset))
                .map(entry -> entry.getValue().toString())
                .sorted().collect(Collectors.joining(", "))
                + "}";
    }

    @Override
    public Entailed removeK(int k) {
        return this;
    }


    public static SupportCollector create(Map<Constant, Subset> constantToSubset) {
        return new SupportCollector(constantToSubset.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey)));
    }
}
