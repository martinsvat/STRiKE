package logicStuff.learning.constraints.shortConstraintLearner;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.learning.saturation.RuleSaturator;
import logicStuff.theories.TheorySimplifier;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 27. 10. 2017.
 */
public class CLDebugger {

    public static void main(String[] args) throws IOException {
        System.out.println("val of java.util.concurrent.ForkJoinPool.common.parallelism\t" + System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "nic"));

        //debugNowOrNever();
        //hornFind();
        //debugSubsumption();
        debugSampling();
        //debugEnh();
        //simplifier();
    }

    private static void debugSubsumption() throws IOException {
        String dataPath = Sugar.path("..", "datasets", "nci_transformed", "gi50_screen_KM20L2.txt");
        //Clause conjunction = LogicUtils.flipSigns(Clause.parse("!s3(V1), !c2(V3), 3_bond(V1,V3)"));
        //Clause c = Clause.parse("!s3(V1), !c2(V3),3_bond(V1,V3)");
        Clause c = Clause.parse("!s3(V1),!c2(V3),3_bond(V1,V3)");
        HornClause hc = HornClause.create(c);
        Clause conjunction = LogicUtils.flipSigns(c);
        conjunction = LogicUtils.flipSigns(conjunction);
        System.out.println(conjunction);
        MEDataset dataset = MEDataset.create(dataPath, Matching.THETA_SUBSUMPTION);
        System.out.println(dataset.subsumed(conjunction));
        System.out.println(dataset.numExistentialMatches(hc, 0));

    }

    private static void hornFind() {
        Stream<String> s = Sugar.list("").stream();
        s.map(Clause::parse).filter(c -> {
            long pos = c.literals().stream().filter(l -> !l.isNegated()).count();
            if (pos == 1) {
                return true;
                //System.out.println(c);

            }
            return false;
        }).sorted(Comparator.comparingInt(Clause::countLiterals))
                .forEach(c -> System.out.println(c));
    }

    private static void debugSampling() throws IOException {
        System.out.println("debugSampling()");

        String dataPath = Sugar.path("..", "datasets", "nci_transformed", "gi50_screen_KM20L2.txt");
        System.out.println("dataset path\t" + dataPath);

        int subsumptionMode = Matching.THETA_SUBSUMPTION;
        MEDataset dataset = MEDataset.create(dataPath, subsumptionMode);

        int absoluteMaxSupport = 0;

        int maxLiterals = 10;
        int maxVariables = 2 * maxLiterals;


        SamplerConstraintLearner learner = SamplerConstraintLearner.create(dataset, absoluteMaxSupport);
        long time = System.nanoTime();

        Pair<List<Clause>,List<Clause>> enh = learner.learnConstraints(10, 100, maxVariables, maxLiterals,
                1, 10, 1, 100,true);

        time = System.nanoTime() - time;
        System.out.println("\nneeded time\t" + time);

        System.out.println("learned\t" + enh.r.size());

    }


    private static void debugNowOrNever() {
        List<Clause> original = Sugar.list("").stream().map(Clause::parse).collect(Collectors.toList());
        List<Clause> saturated = Sugar.list("").stream().map(Clause::parse).collect(Collectors.toList());
        List<Clause> theory = Sugar.list("").stream().map(Clause::parse).collect(Collectors.toList());

        diffs(original, saturated);
        Set<IsoClauseWrapper> saturatedSet = saturated.stream().map(IsoClauseWrapper::create).collect(Collectors.toSet());
        Set<IsoClauseWrapper> out = original.stream().map(IsoClauseWrapper::create).filter(icw -> !saturatedSet.contains(icw)).collect(Collectors.toSet());

        System.out.println("out size\t" + out.size());

        RuleSaturator saturator = RuleSaturator.create(theory);
        Set<IsoClauseWrapper> sat = out.stream().map(IsoClauseWrapper::getOriginalClause).map(c -> saturator.negativeSaturation(c))
                //.filter(c -> null != c)
                .map(IsoClauseWrapper::create).collect(Collectors.toSet());
        System.out.println("size sat\t" + sat.size());
    }

    private static void simplifier() {
        Collection<Clause> theory = null;
        List<Clause> simpler = TheorySimplifier.simplify(theory, theory.stream().mapToInt(c -> c.variables().size()).max().orElse(0) + 1);
        System.out.println(theory.size() + "\t" + simpler.size());
        System.out.println("\noriginal theory");
        theory.forEach(c -> System.out.println(c));
        System.out.println("\nsimplified theory");
        simpler.forEach(c -> System.out.println(c));
        System.out.println(String.join(",", simpler.stream().map(c -> "\"" + c.toString() + "\"").collect(Collectors.toList())));
    }

    public static void debugEnh() throws IOException {
        System.out.println("this class serves only for debugging of enhancedMDShortConstraintLearner and MDShortConstraintLearner");

//        String dataPath = Sugar.path("..", "datasets", "imdb", "merged.db.oneLine");
        //String dataPath = Sugar.path("..", "..", "datasets", "mlns", "imdb", "merged.db.oneLine");
        //String dataPath = Sugar.path("..", "..", "datasets", "mlns", "uwcs", "all.db.oneLine");
        //String dataPath = Sugar.path("..", "..", "datasets", "mlns", "protein", "merged.txt.oneLine");
        String dataPath = String.join(File.separator, Sugar.list("..", "datasets", "nci_transformed", "gi50_screen_KM20L2.txt"));
        System.out.println("dataset path\t" + dataPath);

        int subsumptionMode = Matching.THETA_SUBSUMPTION;
        MEDataset dataset = MEDataset.create(dataPath, subsumptionMode);

        int maxLiterals = 3;
        int maxVariables = 2 * maxLiterals;


        int absoluteMaxSupport = 0;

        UltraShortConstraintLearnerFaster enhanced = UltraShortConstraintLearnerFaster.create(dataset, maxLiterals, maxVariables, absoluteMaxSupport, 1, 1, 10);

        long time = System.nanoTime();

        Pair<List<Clause>,List<Clause>> enh = enhanced.learnConstraints(true, false, false, Integer.MAX_VALUE, Sugar.set(),
                false, 2, false, Sugar.set(), 100); //100

        time = System.nanoTime() - time;
        System.out.println("\nneeded time\t" + time);

        System.out.println("learned\t" + enh.r.size());

        DecimalFormat format = new DecimalFormat(".###");
        enh.r.forEach(pair -> System.out.println(pair));

        //enh.sort(Comparator.comparingDouble(p -> p.s));
        //enh.forEach(pair -> System.out.println(format.format(pair.s) + "\t" + pair.r));
    }

    private static Pair<Stream<Clause>, Stream<Clause>> diffs(List<Clause> simple, List<Clause> enh) {
        System.out.println("error in saturator or its usage?");

        Set<IsoClauseWrapper> simpleConst = simple.stream().map(c -> IsoClauseWrapper.create(c)).collect(Collectors.toSet());
        Set<IsoClauseWrapper> enhConst = enh.stream().map(c -> IsoClauseWrapper.create(c)).collect(Collectors.toSet());

        System.out.println("redundancy check first\t" + simple.size() + "\t" + simpleConst.size());
        System.out.println("redundancy check second\t" + enh.size() + "\t" + enh.size());

        System.out.println("is in first but not in second");
        diff(simpleConst, enhConst).forEach(System.out::println);

        System.out.println("is in second but not in first");
        diff(enhConst, simpleConst).forEach(System.out::println);

        return new Pair<>(diff(simpleConst, enhConst), diff(enhConst, simpleConst));
    }

    private static Stream<Clause> diff(Set<IsoClauseWrapper> base, Set<IsoClauseWrapper> remove) {
        Set<IsoClauseWrapper> copySimple = new HashSet<>(base);
        copySimple.removeAll(remove);
        return copySimple.stream().map(iso -> iso.getOriginalClause());
    }

    private static List<Clause> strToClauses(List<String> list) {
        return list.stream().map(Clause::parse).collect(Collectors.toList());
    }

    private static long saturatorPerformance(RuleSaturator saturator, Clause clause, int iter) {
        long start = System.nanoTime();
        for (int i = 0; i < iter; i++) {
            saturator.saturate(clause);
//            saturator.positiveSaturation(clause);/
//            saturator.negativeSaturation(clause);
        }
        return System.nanoTime() - start;
    }


}
