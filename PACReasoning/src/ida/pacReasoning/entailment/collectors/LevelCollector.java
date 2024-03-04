package ida.pacReasoning.entailment.collectors;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;


import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 23. 8. 2018.
 */
public class LevelCollector implements Entailed {
    private final List<VECollector> collectors;
    private long time;

    public LevelCollector(Collection<Literal> evidence, int k, int maxArity) {
        this.collectors = IntStream.range(1, k + 1)
                .mapToObj(idx -> VECollector.create(evidence, idx, maxArity))
                .collect(Collectors.toList());
    }

    public LevelCollector setTime(long time) {
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
        int size = subset.size();
        for (VECollector collector : collectors) {
            if (collector.getK() >= size) {
                collector.add(query, subset);
            }
        }
    }

    @Override
    public String asOutput() {
        if (collectors.isEmpty()) {
            System.out.println("you have run out of k");
            throw new IllegalStateException();// NotImplementedException();
        }
        return collectors.get(collectors.size() - 1).asOutput();
    }

    @Override
    public Entailed removeK(int k) {
        if (collectors.isEmpty()) {
            return null;
        }
        int idx = 0;
        for (; idx < collectors.size(); idx++) {
            if (collectors.get(idx).getK() == k) {
                break;
            }
        }
        VECollector found = collectors.get(idx);
        for (; idx >= 0; idx--) {
            collectors.remove(idx);
        }
        return found;
    }

    public static LevelCollector create(Collection<Literal> evidence, int k, int maxArity) {
        return new LevelCollector(evidence, k, maxArity);
    }

}
