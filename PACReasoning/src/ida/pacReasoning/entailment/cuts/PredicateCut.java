package ida.pacReasoning.entailment.cuts;

import ida.ilp.logic.Literal;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.utils.Sugar;
import ida.utils.collections.DoubleCounters;
import ida.utils.tuples.Pair;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * More of a filter than a cut.
 * <p>
 * Created by martin.svatos on 19. 6. 2018.
 */
public class PredicateCut implements Cut {


    private final java.util.function.Predicate<Pair<String, Integer>> filter;

    private PredicateCut(java.util.function.Predicate<Pair<String, Integer>> filter) {
        this.filter = filter;
    }

    @Override
    public boolean isEntailed(Literal literal, double votes, long constants) {
        return filter.test(new Pair<>(literal.predicate(), literal.arity()));
    }

    @Override
    public boolean isEntailed(Predicate predicate, int a, double votes, long constants) {
        return filter.test(predicate.getPair());
    }

    @Override
    public boolean isEntailed(String predicate, int arity, int a, double votes, long constants) {
        return filter.test(new Pair<>(predicate, arity));
    }

    @Override
    public VECollector entailed(VECollector data) {
        return VECollector.create(data.getEvidence()
                , DoubleCounters.createCounters(Sugar.parallelStream(data.getVotes().entrySet(), true)
                        .filter(e -> this.isEntailed(e.getKey(), e.getValue(), data.constantsSize()))
                        .map(e -> new Pair<>(e.getKey(), e.getValue()))
                        .collect(Collectors.toSet()))
                , data.getK()
                , data.constantsSize()
                , data.getConstants()
                , data.originalEvidenceSize());
    }


    @Override
    public Set<Predicate> predicates() {
        return Sugar.set();
    }

    @Override
    public Cut cut(int evidenceSize) {
        return this;
    }

    @Override
    public String name() {
        return "p-cut";
    }

    public static PredicateCut create(Predicate predicate) {
        return create(predicate.getName(), predicate.getArity());
    }

    public static PredicateCut create(String predicate, int arity) {
        return create((pair) -> pair.getS() == arity && pair.r.equals(predicate));
    }

    public static PredicateCut create(java.util.function.Predicate<Pair<String, Integer>> filter) {
        return new PredicateCut(filter);
    }
}
