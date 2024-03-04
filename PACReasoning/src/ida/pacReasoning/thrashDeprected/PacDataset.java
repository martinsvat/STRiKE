package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 3. 4. 2018.
 */
public class PacDataset implements DatasetInterface {

    private final Clause example;
    private final Matching matching;

    public PacDataset(Clause clause) {
        this(clause, Matching.THETA_SUBSUMPTION);
    }

    public PacDataset(Clause clause, int substitutionType) {
        this.example = clause;
        this.matching = Matching.create(clause, substitutionType);
    }

    @Override
    public int numExistentialMatches(HornClause hc, int maxNum) {
        return matching.subsumption(hc.body(), 0) ? 1 : 0;
    }

    @Override
    public int numExistentialMatches(HornClause hc, int maxNum, Coverage checkOnly) {
        if (checkOnly.size() != 1 || !checkOnly.asSet().contains(0)) {
            return 0;
        }
        return numExistentialMatches(hc, maxNum);
    }

    @Override
    public Set<Pair<String, Integer>> queryPredicates() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Pair<String, Integer>> allPredicates() {
        return this.example.literals().stream().map(Literal::getPredicate).collect(Collectors.toSet());
    }

    @Override
    public Pair<Coverage, Coverage> classify(HornClause hc, Coverage learnFrom) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Coverage getPosIdxs() {
        return CoverageFactory.getInstance().take(0);
    }

    @Override
    public Coverage getNegIdxs() {
        return CoverageFactory.getInstance().take();
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean matchesAtLeastOne(Clause conjunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Coverage subsumed(Clause clause, Coverage scount) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean allNegativeExamples(Coverage learnFrom) {
        return false;
    }

    @Override
    public double[] getTargets() {
        return new double[1];
    }

    @Override
    public DatasetInterface subDataset(Coverage parentsCoverage) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DatasetInterface deepCopy(int subsumptionType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public DatasetInterface flatten(int subsumptionMode) {
        throw new UnsupportedOperationException();
    }

    public Triple<Term[], List<Term[]>, Double> sampleGroundingSubstiution(HornClause horn, int subsampleSize, int stepSize) {
        return matching.searchTreeSampler(horn.body(), 0, subsampleSize, stepSize);
    }

    // I would prefere this to be outside of the dataset as it is a metric which does not need to be dataset based
    public double subsampleScore(HornClause horn, int subsampleSize, int stepSize) {
        Triple<Term[], List<Term[]>, Double> groundings = sampleGroundingSubstiution(horn, subsampleSize, stepSize);

        Literal head = horn.head();
        Term[] variables = groundings.r;
        return Sugar.parallelStream(groundings.s, true)
                .filter(constants -> {
                    Literal impliedHead = LogicUtils.substitute(head, variables, constants);
                    return this.example.literals().contains(impliedHead);
                }).count() / (1.0 * subsampleSize);
        /*long truePositive = 0l;
        long falsePositive = 0l;
        Term[] variables = groundings.r;
        for (Term[] constants : groundings.s) {
            Literal impliedHead = LogicUtils.substitute(head, variables, constants);
            if (this.example.literals().contains(impliedHead)) {
                truePositive++;
            } else {
                falsePositive++;
            }
        }
        //System.out.println("nejaka jina metrika tady?");
        return truePositive / (truePositive * 1.0 + falsePositive);
        */
    }
}
