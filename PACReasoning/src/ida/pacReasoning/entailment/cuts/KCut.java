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
 * Created by martin.svatos on 18. 6. 2018.
 */
public class KCut implements Cut {

    public static KCut K_CUT = new KCut();

    @Override
    public boolean isEntailed(Literal literal, double votes, long constants) {
        return true;
    }

    @Override
    public boolean isEntailed(Predicate predicate, int a, double votes, long constants) {
        return true;
    }

    @Override
    public boolean isEntailed(String predicate, int arity, int a, double votes, long constants) {
        return true;
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
        ,data.originalEvidenceSize());
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
        return "k-cut";
    }

}
