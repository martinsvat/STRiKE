package ida.pacReasoning.entailment.cuts;

import ida.ilp.logic.Literal;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.entailment.collectors.VECollector;

import java.util.Set;

/**
 *
 * Cuts are fixed for one k.
 *
 * Created by martin.svatos on 18. 6. 2018.
 */
public interface Cut {

    // prvni tri jsou asi prezitek
    boolean isEntailed(Literal literal, double votes, long constants);

    boolean isEntailed(Predicate predicate, int a, double votes, long constants);

    boolean isEntailed(String predicate, int arity, int a, double votes, long constants);

    VECollector entailed(VECollector data);

    Cut cut(int evidenceSize);

    Set<Predicate> predicates();

    String name();
}
