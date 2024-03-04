package logicStuff.learning.datasets;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.utils.tuples.Pair;

import java.util.Set;

/**
 * Created by martin.svatos on 27. 6. 2017.
 */
public interface DatasetInterface {

    // TODO -- uklidit jednou, hodne balastu navic, nebo opakujiciho se

    public int numExistentialMatches(HornClause hc, int maxNum);

    public int numExistentialMatches(HornClause hc, int maxNum, Coverage checkOnly);

    public Set<Pair<String, Integer>> queryPredicates();

    public Set<Pair<String, Integer>> allPredicates();

    public Pair<Coverage, Coverage> classify(HornClause hc, Coverage learnFrom);

    Coverage getPosIdxs();

    Coverage getNegIdxs();

    public int size();

    boolean matchesAtLeastOne(Clause clause);

    Coverage subsumed(Clause clause, Coverage scount);

    boolean allNegativeExamples(Coverage learnFrom);

    double[] getTargets(); // this is type specific, should not be in the interface !!! TODO

    DatasetInterface subDataset(Coverage parentsCoverage);

    DatasetInterface deepCopy(int subsumptionType);

    DatasetInterface flatten(int subsumptionMode);
}
