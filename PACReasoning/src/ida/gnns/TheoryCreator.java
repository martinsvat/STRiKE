package ida.gnns;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.pacReasoning.data.SubsampledDataset;
import ida.pacReasoning.HornLearner;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.utils.Sugar;
import ida.utils.collections.Counters;
import ida.utils.tuples.Pair;
import logicStuff.learning.saturation.RuleSaturator;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A unified class for theory creation... either it's from AIME, AnyBurl outputs... whatever. CE selection heuristic or other....
 * <p>
 * Created by martin.svatos on 2. 2. 2021.
 */
public class TheoryCreator {


    public void main(String[] args) {

    }

    // taken from Pipeline & changed a little bit... get an interface for this
    public Possibilistic crossEntropySelectioin(List<Clause> rulesPool, int rounds, int maxVariables,
                                                SubsampledDataset dataset, Set<Clause> constraints,
                                                Path typed, int k, boolean saturate) {
        RuleSaturator saturator = RuleSaturator.create(constraints);
        Stream<HornClause> stream = rulesPool.stream()
                .filter(c -> LogicUtils.variables(c).size() <= maxVariables)
                .map(HornClause::new)
                .filter(LogicUtils::isRangeRestricted);
        if (saturate) {// TODO this given here because it takes to much time to saturate 200k of rules
            stream = stream.map(saturator::saturate)
                    .filter(Objects::nonNull);
        }
        Set<IsoClauseWrapper> selected = stream
                .map(IsoClauseWrapper::create)
                .collect(Collectors.toSet());

        System.out.println("** clause (rules pool) loaded");

        boolean debugRewrite = false;
        System.out.println("**********\t\t\t\tpozor pozor debug rewrite\t" + debugRewrite);

        // rule-selection itself

        HornLearner learner = new HornLearner();
        long start = System.nanoTime();
        System.setProperty("ida.searchPruning.simpleLearner.compareByAccuracy", "true"); // this invokes top-k-beam search
        System.out.println("learning horn rules with setting\trounds " + rounds + "\tbeamSize " + "0" + "\tvariables " + maxVariables + "\tt " + (null != typed));
        System.out.println("inside is " + selected.size());
        List<Pair<Double, HornClause>> rules = learner.selectEntropyDriven(dataset, rounds, selected);

        System.out.println("storing rules");
        List<Pair<Double, Clause>> soft = rules.stream().map(p -> new Pair<>(p.r, p.s.toClause())).sorted(Comparator.comparing(Pair<Double, Clause>::getR).reversed()).collect(Collectors.toList());
        Possibilistic ps = Possibilistic.create(constraints, soft);

        double secondsNeeded = 1.0 * (System.nanoTime() - start) / 1000_000_000;
        System.out.println("theory learned within \t" + secondsNeeded + " s");
        return ps;
    }

    public static TheoryCreator create() {
        return new TheoryCreator();
    }

}
