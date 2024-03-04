package ida.pacReasoning.entailment.nodes;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.theories.Theory;

import java.util.Set;

/**
 * Created by martin.svatos on 8. 6. 2018.
 */
public interface MutableNode {
    Subset getConstants();

    Theory getTheory();

    Set<Literal> entailed(); // entailed by any ancestor
    Set<Literal> entailedByAncestor(); // entailed by direct ancestor (e.g. the ones having exactly one constant shorter)

    boolean evaluate(); // if true then all possible literals are entailed

    boolean checkConsistency(); // returns true iff theories with different sizes were used for parents
}
