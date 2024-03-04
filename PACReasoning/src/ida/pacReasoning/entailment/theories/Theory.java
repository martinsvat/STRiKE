package ida.pacReasoning.entailment.theories;

import ida.ilp.logic.Clause;
import ida.utils.tuples.Triple;

import java.util.List;
import java.util.Set;

/**
 * Created by martin.svatos on 8. 6. 2018.
 */
public interface Theory {

    int size();

    Set<Clause> allRules();

    Set<Clause> getHardRules();

    // i know, afwul, the first are just hard constraints without horn rules from them, the second are horns, the third is rest
    Triple<Set<Clause>,Set<Clause>,Set<Clause>> getConstrainsHornOthers();

    String getName();


}
