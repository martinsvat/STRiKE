package ida.pacReasoning.data;

import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.CustomPredicate;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SolutionConsumer;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.searchPruning.search.collections.SearchNodeInfo;
import ida.utils.Sugar;
import ida.utils.VectorUtils;
import ida.utils.collections.MultiList;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Quadruple;
import ida.utils.tuples.Triple;
import logicStuff.typing.TypesInducer;


import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


/**
 * This is very very awful quick implementation of one dataset we just needed.
 * <p>
 * Created by martin.svatos on 7. 1. 2019.
 */
public class SubsampledDataset {


    public static final int ACCURACY = 2;
    public static final int CROSS_ENTROPY = 1;


    private final Set<Literal> evidence;
    private final Set<Literal> goldComplement;
    private final Set<Literal> groundTruth;
    private final MultiMap<String, Term> typeHistogram;
    private final Map<Pair<Predicate, Integer>, String> typing;
    private Matching world;
    private final Path src;
    private List<Pair<Double, HornClause>> rules;
    private Map<Literal, Double> positiveLiteralsEntailedByTheory;
    private Map<Literal, Double> falseLiteralsEntailedByTheory; // (1 - conditional) is stored in the values
    //    private ConcurrentHashMap<SearchNodeInfo, Quadruple<Double, Double, Set<Literal>, Set<Literal>>> memory; // R= crossentropy, S= memory PSI, T= TP literals, U= FP literals
//     R muze mit taky vyznam -1*acc pro ACCURACY mod
    private Pair<SearchNodeInfo, Quadruple<Double, Double, Set<Literal>, Set<Literal>>> best = null;
    private boolean storeRangeRestricted;
    private double implicitBound;
    private Map<Pair<String, Integer>, Long> predicateHistogram;
    private static final boolean computeEntropy = true; // true -> entropy, false -> square loss
    private Pair<String, Integer> targetPredicate;

    private double possibleNegativeLiterals;
    private Set<Constant> constants;
    private double crossentropy;
    private Set<IsoClauseWrapper> negateTheory;
    private List<Literal> predicateGroundTruth;
    private int mode;

    public SubsampledDataset(Set<Literal> evidence, Set<Literal> complement, Path src) {
        this.negateTheory = Sugar.set();
        this.evidence = evidence;
        this.goldComplement = complement;
        this.groundTruth = Sugar.union(evidence, complement);
        MultiMap<String, Term> histogram = new MultiMap<>();
        this.groundTruth.stream().flatMap(Literal::argumentsStream)
                .forEach(term -> histogram.put(null == term.type() ? "" : term.type(), term));
        this.typeHistogram = histogram;
        this.positiveLiteralsEntailedByTheory = new HashMap<>();
        this.falseLiteralsEntailedByTheory = new HashMap<>();
        this.rules = Sugar.list();
//        this.memory = new ConcurrentHashMap<>();

        double totalPossibleFacts = 0;
        MultiList<Predicate, Integer> predicatesTypesList = new MultiList<>();
        System.out.println(evidence);
        Map<Pair<Predicate, Integer>, String> typing = null;
        Set<Literal> evidenceForTyping = Sugar.union(evidence, groundTruth);
        try {
            typing = TypesInducer.create().collect(evidenceForTyping);
        } catch (Exception e) {
            typing = new HashMap<>();
            Set<Pair<String, Integer>> preds = LogicUtils.predicates(new Clause(evidenceForTyping));
            for (Pair<String, Integer> pred : preds) {
                Predicate predicate = Predicate.create(pred);
                for (int i = 0; i < pred.s; i++) {
                    typing.put(new Pair<>(predicate, i), null);
                }
            }
        }

        this.typing = typing;
        this.constants = LogicUtils.constantsFromLiterals(this.groundTruth);
        if (typing.values().stream().anyMatch(o -> null == o)) { // there is at least one type missing => continue as no types are introduced at all
            for (Pair<String, Integer> predicate : LogicUtils.predicates(new Clause(this.groundTruth))) {
                double current = 1;
                for (int ar = 0; ar < predicate.s; ar++) {
                    current *= constants.size();
                }
                totalPossibleFacts += current;
            }
        } else {
            for (Map.Entry<Pair<Predicate, Integer>, String> entry : typing.entrySet()) {
                Predicate predicate = entry.getKey().r;
                Set<Term> counts = this.typeHistogram.get(entry.getValue());
                predicatesTypesList.put(predicate, counts.size());
            }
            for (Map.Entry<Predicate, List<Integer>> entry : predicatesTypesList.entrySet()) {
                double current = 1;
                for (Integer counts : entry.getValue()) {
                    current = current * counts;
                }
                totalPossibleFacts += current;
            }
        }

        this.implicitBound = groundTruth.size() / totalPossibleFacts;
        this.possibleNegativeLiterals = totalPossibleFacts - this.groundTruth.size();
        this.world = Matching.create(new Clause(evidence), Matching.THETA_SUBSUMPTION);
        this.predicateHistogram = groundTruth.stream().map(Literal::getPredicate).collect(Collectors.groupingBy(p -> p, Collectors.counting()));
        this.src = src;
        this.crossentropy = 0.0;
        this.mode = CROSS_ENTROPY;
    }


    public Set<Literal> getEvidence() {
        return evidence;
    }

    public Set<Literal> getGoldComplement() {
        return goldComplement;
    }

    private static SubsampledDataset load(Path evidence, Path groundTruth) {
        return new SubsampledDataset(LogicUtils.loadEvidence(evidence),
                LogicUtils.loadEvidence(groundTruth), evidence);
    }

    public static SubsampledDataset load(Path evidence) {
        Path complement = Paths.get(evidence.toString() + ".complement");
        if (!evidence.toFile().exists() || !complement.toFile().exists()) {
            throw new IllegalStateException("at least one of the dataset file is missing\n\t" + evidence + "\n\t" + complement);
        }
        return load(evidence, complement);
    }

    public static SubsampledDataset create(Set<Literal> evidence, Path src) {
        return new SubsampledDataset(evidence, Sugar.set(), src);
    }


    public Pair<Set<Literal>, Set<Literal>> evaluate(HornClause rule) {
        return evaluate(rule, LogicUtils.isRangeRestricted(rule));
    }

    private Pair<Set<Literal>, Set<Literal>> evaluate(HornClause rule, boolean isRangeRestricted) {
        if (isRangeRestricted) {
            return evaluateRangeRestricted(rule);
        }
        return evaluateNonRestricted(rule);
    }


    {
        System.out.println("vyhodnoceni rekurzivnich non-range restricted pravidel pomoci samplingu?");
    }


    {
        System.out.println("not thread-safe");
    }


    {
        System.out.println("co tady udelat taky ten trik s consumerem? +++ tady predelat maxValue na 2*procentualni zastoupeni FP");
    }

    private Pair<Set<Literal>, Set<Literal>> evaluateNonRestricted(HornClause rule) {
        //consumer ?
        // a co rekurzivni pravidla?


        /*
        Pair<Term[], List<Term[]>> posSubs = world.allSubstitutions(new Clause(Sugar.iterable(rule.body().literals(), Sugar.list(rule.head()))), 0, Integer.MAX_VALUE);
        Set<Literal> pos = Sugar.set();
        for (Term[] target : posSubs.getS()) {
            Literal substituted = LogicUtils.substitute(rule.head(), posSubs.r, target); // but this won't be ground since the rule is not range restricted, thus pos will not add anything
            Pair<Term[], List<Term[]>> headGroundings = world.allSubstitutions(new Clause(substituted), 0, Integer.MAX_VALUE);
            for (Term[] img : headGroundings.s) {
                Literal groundHead = LogicUtils.substitute(substituted, headGroundings.r, img);
                if (this.groundTruth.contains(groundHead)) {
                    pos.add(groundHead);
                }
            }
        }

        int subsampleSize = 5 * (pos.size());
        int step = 10;
        Set<Literal> neg = Sugar.set();
        //Triple<Term[], List<Term[]>, Double> negSubs = m.searchTreeSampler(rule.toExistentiallyQuantifiedConjunction(), 0, subsampleSize, step);
        Pair<Term[], List<Term[]>> negSubs = world.allSubstitutions(rule.toExistentiallyQuantifiedConjunction(), 0, subsampleSize);
        for (Term[] target : negSubs.s) {
            Literal substituted = LogicUtils.substitute(rule.head(), negSubs.r, target);
            if (!this.groundTruth.contains(substituted)) {
                neg.add(substituted);
            }
        }*/

        //this.world = Matching.get(new Clause(evidence), Matching.THETA_SUBSUMPTION); //  ted test :))
        MultiMap<Pair<String, Integer>, Literal> herbrand = new MultiMap<>();
        SolutionConsumer solutionConsumer = new HerbrandSolutionConsumer(rule.head(), rule.head().getPredicate(), herbrand);
        this.world.getEngine().addCustomPredicate(new TupleNotIn(rule.head().predicate(), rule.head().arity(), herbrand.get(rule.head().getPredicate())));
        this.world.getEngine().addSolutionConsumer(solutionConsumer);


        Literal alldiff = new Literal(SpecialVarargPredicates.ALLDIFF, false, Sugar.listFromCollections(rule.variables()));
        Clause query = new Clause(Sugar.iterable(rule.body().literals(), Sugar.list(alldiff,
                new Literal(tupleNotInPredicateName(rule.head().predicate(), rule.head().arity()), false, rule.head().arguments()))));
//        System.out.println(rule);
//        System.out.println(query);
        //Clause query = new Clause(Sugar.iterable(clause.literals()));
        //int atMost = (int) (this.predicateHistogram.get(rule.head().getPredicate()) * partOfAtMost) + 1;
        int atMost = 100000;
        //System.out.println(this.predicateHistogram.get(rule.head().getPredicate()) + "\t" + atMost + "\t" + (atMost > 1000 ? "tady" : ""));
        this.world.allSubstitutionsWithVariableOrder(query, 0, atMost, Sugar.listFromCollections(Sugar.setDifference(LogicUtils.variables(rule.head()), rule.body().variables())));
//        this.world.allSubstitutions(query, 0, atMost);
        this.world.getEngine().removeSolutionConsumer(solutionConsumer);

        Set<Literal> entailed = herbrand.get(rule.head().getPredicate());
        Set<Literal> pos = Sugar.set();
        Set<Literal> neg = Sugar.set();
        for (Literal literal : entailed) {
            if (groundTruth.contains(literal)) {
                pos.add(literal);
            } else {
                neg.add(literal);
            }
        }

//        System.out.println(neg.size() + "\t" + pos.size() + "\t" + ((neg.size() + pos.size()) > 1000));
//
//        if (neg.size() + pos.size() > 1000) {
//            System.out.println("some problem here\t" + neg.size() + "\t" + pos.size() + "\t" + entailed.size() + "\t" + atMost + "\t" + this.predicateHistogram.get(rule.head().getPredicate()));
//        }

        return new Pair<>(pos, neg);
    }

    private Pair<Set<Literal>, Set<Literal>> evaluateRangeRestricted(HornClause rule) {
        MultiMap<Pair<String, Integer>, Literal> herbrand = new MultiMap<>();
        Set<Pair<String, Integer>> preds = LogicUtils.predicates(rule.toClause());

        if (LogicUtils.isGround(rule.head())) {
            throw new IllegalStateException();// NotImplementedException();
        }

        int before = -1;
        Literal head = rule.head();
        Clause clause = rule.body(); // existentially quantified

        Literal alldiff = new Literal(SpecialVarargPredicates.ALLDIFF, false, Sugar.listFromCollections(rule.variables()));
        Pair<String, Integer> headSignature = head.getPredicate();
        while (herbrand.get(rule.head().getPredicate()).size() != before) {
            Set<Literal> entailedHeads = herbrand.get(rule.head().getPredicate());
            before = herbrand.get(rule.head().getPredicate()).size();

            Matching matching = new Matching(Sugar.list(new Clause(Sugar.iterable(entailedHeads, this.evidence))));
            SolutionConsumer solutionConsumer = new HerbrandSolutionConsumer(head, headSignature, herbrand);
            for (Pair<String, Integer> predicate : preds) {
                //may overwrite the previous ones which is actually what we want
                matching.getEngine().addCustomPredicate(new TupleNotIn(predicate.r, predicate.s, herbrand.get(predicate)));
            }

            matching.getEngine().addSolutionConsumer(solutionConsumer);
            Clause query = new Clause(Sugar.iterable(clause.literals(), Sugar.list(alldiff,
                    new Literal(tupleNotInPredicateName(head.predicate(), head.arity()), false, head.arguments()))));
            //Clause query = new Clause(Sugar.iterable(clause.literals()));
            matching.allSubstitutions(query, 0, Integer.MAX_VALUE);
            matching.getEngine().removeSolutionConsumer(solutionConsumer);
        }

        Set<Literal> entailed = herbrand.get(rule.head().getPredicate());
        Set<Literal> pos = Sugar.set();
        Set<Literal> neg = Sugar.set();
        for (Literal literal : entailed) {
            if (groundTruth.contains(literal)) {
                pos.add(literal);
            } else {
                neg.add(literal);
            }
        }
        return new Pair<>(pos, neg);
    }


    // recopy from LeastHrebrandModel by OK
    private static String tupleNotInPredicateName(String predicate, int arity) {
        return "@tuplenotin-" + predicate + "/" + arity;
    }

    public List<Pair<Double, HornClause>> getRules() {
        return rules;
    }

    public Triple<Double, Double, Pair<Integer, Integer>> computeConditionalAndCrossEntropy(SearchNodeInfo candidate) {
        if (!candidate.getRule().head().getPredicate().equals(this.targetPredicate)) {
            throw new IllegalStateException();
        }
        boolean isRangeRestricted = LogicUtils.isRangeRestricted(candidate.getRule());
        Pair<Set<Literal>, Set<Literal>> coverage = evaluate(candidate.getRule(), isRangeRestricted);

        if (ACCURACY == this.mode) {
            coverage = uncoveredAccuracy(coverage);
        }

        Set<Literal> pos = coverage.r;
        Set<Literal> neg = coverage.s;


        if (pos.size() + neg.size() == 0
                || (pos.size() == 0 //&& isRangeRestricted
        )) {
            return null;
        }

/*        if (pos.size() + neg.size() > 1000) {
            System.out.println("hey joe\t" + coverage);
        }
*/
        double conditional = (pos.size() * 1.0) / (pos.size() + neg.size());
        /*if (conditional < this.implicitBound) {
            return null;
        }*/


        double crossEntropy = CROSS_ENTROPY == this.mode
                ? computeEntropy(pos, neg, conditional) + this.crossentropy // crossentropy case, selecting minimum
                : -conditional; // accuracy case; we are minimizing in the end, but we need to maximize acc, therefore -1*

        /*Double crossEntropy = null; // computeEntropy(pos, neg, conditional);
        Double conditional = 0.0;
        for (Double cond : Sugar.list("0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9").stream().mapToDouble(Double::parseDouble).boxed().collect(Collectors.toList())) {
            double current = computeEntropy(pos, neg, cond);
            System.out.println(cond + "\t" + current);
            if (null == crossEntropy || crossEntropy > current) {
                crossEntropy = current;
                conditional = cond;
            }
        }
        System.out.println("finally\t" + conditional + "\tinstead of\t" + ((pos.size() * 1.0) / (pos.size() + neg.size())));
        */
        boolean storeOnlyRangeRestricted = true;
        if (!this.storeRangeRestricted || isRangeRestricted) { // or do some update policy (and remove select max at the level finalization)
            //this.memory.put(candidate, new Quadruple<>(crossEntropy, conditional, pos, neg));
            synchronized (this) { // memory friendly version
                if ((null == this.best || this.best.s.r > crossEntropy) && (!storeOnlyRangeRestricted || isRangeRestricted)) {
                    //this.memory.clear();
                    Quadruple<Double, Double, Set<Literal>, Set<Literal>> quadruple = new Quadruple<>(crossEntropy, conditional, pos, neg);
                    //this.memory.put(candidate, quadruple);
                    this.best = new Pair<>(candidate, quadruple);
                    System.out.println("updating best\t" + candidate.getRule() + "\t" + quadruple.r + " | " + quadruple.s + " | " + quadruple.t.size() + " | " + quadruple.u.size());
//                    System.out.println(this.best);
//                    System.out.println(this.memory.size());
//                    System.out.println(this.memory.get(candidate));
                }
            }
        }


        return new Triple<>(conditional, crossEntropy, new Pair<>(pos.size(), neg.size()));
    }

    private Pair<Set<Literal>, Set<Literal>> uncoveredAccuracy(Pair<Set<Literal>, Set<Literal>> coverage) {
        return new Pair<>(coverage.r.stream().filter(l -> !this.positiveLiteralsEntailedByTheory.containsKey(l)).collect(Collectors.toSet()),
                coverage.s.stream().filter(l -> !this.falseLiteralsEntailedByTheory.containsKey(l)).collect(Collectors.toSet()));
    }


    public double computeEntropyForPruning(Set<Literal> tp, Set<Literal> fp, double conditional) {
        double sum = 0;

        // computing entropy on positive labels
        for (Literal literal : this.predicateGroundTruth) { // groundTruth
            //System.out.println("je to pro kazdy predikat zvlast nebo ne? pokud ano tak sem dat mapu pro cache "); slo by i zvlast pro kazdy predikat jinak
            Double psi = implicitBound;
            if (tp.contains(literal)) {
                psi = conditional;
            }
            sum -= Math.log(psi);// *1
        }

        // computing entropy on negative labels
        double negativeConditional = 1.0 - conditional;
        long negativeCounted = 0;

        Set<Literal> negativePredictions = fp;
        for (Literal literal : negativePredictions) {
            Double val = falseLiteralsEntailedByTheory.get(literal);
            if (null == val || (fp.contains(literal) && val > negativeConditional)) {
                val = negativeConditional;
            }
            sum -= Math.log(val);
            negativeCounted++;
        }

        // add the rest of non entailed
        sum -= Math.log(1 - this.implicitBound) * (this.possibleNegativeLiterals - negativeCounted);
        sum /= Math.log(2);

        return sum;
    }

    public double computeEntropy(Set<Literal> truePositive, Set<Literal> falsePositive, double conditional) {
        //double conditional = (1.0 * truePositive.size()) / (truePositive.size() + falsePositive.size());
        double sum = 0;

        /*
        if (!Sugar.setDifference(truePositive, this.groundTruth).isEmpty()) {
            System.out.println("problem here\t" + Sugar.setDifference(truePositive, this.groundTruth));
            throw new IllegalStateException();
        }

        if (!Sugar.intersection(falsePositive, this.groundTruth).isEmpty()) {
            System.out.println("another problem here\t" + Sugar.intersection(falsePositive, this.groundTruth));
            throw new IllegalStateException();
        }
        */

        /*if (conditional < this.implicitBound) {
            System.out.println("conditionalPSI is lower than the implicit bound\t" + conditional + "\t" + this.implicitBound);
            System.out.println(truePositive.size() + "\t" + falsePositive.size());
            throw new IllegalStateException();
        }*/
//        double log2 = Math.log(2);

        // computing entropy on positive labels
        for (Literal literal : this.predicateGroundTruth) { // groundTruth
            //System.out.println("je to pro kazdy predikat zvlast nebo ne? pokud ano tak sem dat mapu pro cache "); slo by i zvlast pro kazdy predikat jinak
            Double psi = implicitBound;
            if (truePositive.contains(literal)) {
                psi = conditional;
            }
            if (positiveLiteralsEntailedByTheory.containsKey(literal)) {
                psi = Math.max(psi, positiveLiteralsEntailedByTheory.get(literal));
            }
            //System.out.println("jaky je zaklad toho logaritmu tady?");
            //System.out.println(psi + "\t" + Math.log(psi));
            if (computeEntropy) {
                //sum -= Math.log(psi) / log2; // *1
                sum -= Math.log(psi);// *1
            } else { // squared loss
                sum += Math.pow(1 - psi, 2);
            }
        }

        double posPart = sum;
        if (computeEntropy) {
            posPart /= Math.log(2);
        }

        // computing entropy on negative labels
        double negativeConditional = 1.0 - conditional;
        long negativeCounted = 0;

        Set<Literal> negativePredictions = Sugar.set(); // Sugar.union(falseLiteralsEntailedByTheory.keySet(), falsePositive)
        negativePredictions.addAll(falsePositive);
        for (Literal literal : falseLiteralsEntailedByTheory.keySet()) {
            if (literal.getPredicate().equals(this.targetPredicate)) {
                negativePredictions.add(literal);
            }
        }


        for (Literal literal : negativePredictions) {
            Double val = falseLiteralsEntailedByTheory.get(literal);
            if (null == val || (falsePositive.contains(literal) && val > negativeConditional)) {
                val = negativeConditional;
            }
            if (computeEntropy) {
                //sum -= Math.log(val) / log2;
                sum -= Math.log(val);
            } else { // squared loss
                sum += Math.pow(1 - val, 2);
            }
            negativeCounted++;
        }

        /*
        if (Double.isInfinite(sum)) {
            System.out.println();
            System.out.println("val is\t" + conditional);
            System.out.println(Math.log(conditional));
            System.exit(-1);
        }*/

        // add the rest of non entailed
        if (computeEntropy) {
            sum -= Math.log(1 - this.implicitBound) * (this.possibleNegativeLiterals - negativeCounted);
            sum /= Math.log(2);
        } else {
            sum += Math.pow(0 - this.implicitBound, 2) * (this.possibleNegativeLiterals - negativeCounted);
        }
        //double negPart = sum - posPart;
        //System.out.println("\t" + posPart + "\t" + negPart);

        return sum;
    }

    {
        System.out.println(" v this.negateTheory jsou nejaka divna data z tech ICW, negace a nenegace v tele, to opravit :))");
    }

    // S= crossentropy, T= memory PSI
    public Triple<SearchNodeInfo, Double, Double> finishLevel(boolean rangeRestrictedOnly) {
        synchronized (this) {
            /*System.out.println("velikost memory\t" + this.memory.keySet().size());

            Stream<SearchNodeInfo> updateStream = this.memory.keySet().stream();
            if (rangeRestrictedOnly) {
                System.out.println("going only through range restricted");
                updateStream = updateStream.filter(sni -> LogicUtils.isRangeRestricted(sni.getRule()));
            } else {
                System.out.println("allowing non-range restricted");
            }


            //debugMe();

            // here we can add preference for range restricted rules :))
            SearchNodeInfo min = updateStream.min(Comparator.comparingDouble((sni) -> this.memory.get(sni).r)).orElse(null);

            if (null == min) {
                if (null != this.best) {
                    throw new IllegalStateException();
                }
                this.best = null;
                //this.memory = new ConcurrentHashMap<>();
                return null;
            }

            if (this.best.r != min) {
                // add this.best = null?
                //this.memory = new ConcurrentHashMap<>();
                throw new IllegalStateException();
            }
            */
//
//            if (min.getRule().toString().equals("location(1:V0, 5:V1) <- @alldiff(1:V2, 5:V1), function(1:V0, 4:V4), location(1:V2, 5:V1), @alldiff(1:V2, 1:V0, 5:V1, 4:V4)")) {
//                System.out.println("debug tady");
//                System.out.println(this.positiveLiteralsEntailedByTheory.keySet().size());
//                System.out.println(this.falseLiteralsEntailedByTheory.keySet().size());
//
//                Quadruple<Double, Double, Set<Literal>, Set<Literal>> value = this.memory.get(min);
//
//                System.out.println(value.r + "\t" + value.s + "\t" + value.t.size() + "\t" + value.u.size());
///*                for (Literal literal : value.t) {
//                    if (!this.positiveLiteralsEntailedByTheory.containsKey(literal) || this.positiveLiteralsEntailedByTheory.get(literal) != value.s) {
//                        System.out.println("pos diff\t" + literal + "\t" + value.s + "\t" + this.positiveLiteralsEntailedByTheory.get(literal));
//                    }
//                }
//
//                for (Literal literal : value.u) {
//                    if (!this.falseLiteralsEntailedByTheory.containsKey(literal) || this.falseLiteralsEntailedByTheory.get(literal) != (1 - value.s)) {
//                        System.out.println("neg diff\t" + literal + "\t" + (1 - value.s) + "\t" + this.falseLiteralsEntailedByTheory.get(literal));
//                    }
//                }*/
//            }

            if (null == this.best) {
                return null;
            }

            //Quadruple<Double, Double, Set<Literal>, Set<Literal>> value = this.memory.get(min);
            Quadruple<Double, Double, Set<Literal>, Set<Literal>> value = this.best.s;
            SearchNodeInfo min = this.best.r;
            /*System.out.println("min is\t" + min.getRule());

            if (this.negateTheory.contains(min.getRuleICW())) {
                System.out.println("min is in");
                this.negateTheory.forEach(c -> System.out.println("\t" + c.getOriginalClause()));
            }*/

            System.out.println("negateTheory for check");
            this.negateTheory.forEach(c -> System.out.println("\t" + LogicUtils.flipSigns(c.getOriginalClause())));


            Set<Literal> removed = Sugar.set();
            for (Literal literal : min.getRule().body().literals()) {
                HornClause shorter = HornClause.create(min.getRule().head(), Sugar.setDifference(min.getRule().body().literals(), Sugar.union(removed, literal)));
                if (!LogicUtils.isRangeRestricted(shorter)) {
                    continue;
                }
                Pair<Set<Literal>, Set<Literal>> coverage = evaluateRangeRestricted(shorter);
                if (value.t.size() == coverage.r.size() && value.u.size() == coverage.s.size()) {
                    removed.add(literal);
                }
            }

            //Quadruple<Double, Double, Set<Literal>, Set<Literal>> cached = this.memory.get(min);
            Set<Literal> truePositive = value.t;
            Set<Literal> falsePositive = value.u;
            double conditionalPSI = value.s;
            double crossentropy = value.r;
            if (!removed.isEmpty()) {
                HornClause pruned = HornClause.create(min.getRule().head(), Sugar.setDifference(min.getRule().body().literals(), removed));
                System.out.println("simplifying the rule\n\t" + min.getRule() + "\n\t" + pruned);
                min = SearchNodeInfo.create(pruned, null, min.getPositiveCoveredExamples(), min.getNegativeCoveredExamples(), min.getAccuracy(), min.isAllowed());
            }

            if (this.negateTheory.contains(min.getRuleICW())) {
                System.out.println("the rule is already in the negateTheory\n\t" + min.getRule());
                negateTheory.stream().forEach(icw -> System.out.println("\t" + HornClause.create(LogicUtils.flipSigns(icw.getOriginalClause()))));

//                System.out.println("debug, neres ted");
                this.best = null;
//                this.memory = new ConcurrentHashMap<>();
                return null;
            }
            this.crossentropy = value.r;
            this.negateTheory.add(min.getRuleICW());
            Triple result = new Triple(min, value.r, value.s);
            incorporateRule(min, truePositive, falsePositive, conditionalPSI, crossentropy);

//            debugMe();
            this.best = null;
            //this.memory = new ConcurrentHashMap<>();
            return result;
        }
    }

    /*
        private void debugMe() {
            double sum = 0.0;

            double log2 = Math.log(2);
            for (Literal literal : groundTruth) {
                double val = this.implicitBound;
                if (this.positiveLiteralsEntailedByTheory.containsKey(literal)) {
                    val = positiveLiteralsEntailedByTheory.get(literal);
                }

                sum -= Math.log(val) / log2;
            }

            for (Map.Entry<Literal, Double> entry : falseLiteralsEntailedByTheory.entrySet()) {
                sum -= Math.log(entry.getValue()) / log2;
            }
            System.out.println("tady ta entropii je stejnak ale spatne spocinata, chybi tam ten posledni cen");
            System.out.println("debugMe crossentropy\t" + sum);
        }

        private void incorporateRule(SearchNodeInfo sni) {
            Quadruple<Double, Double, Set<Literal>, Set<Literal>> mem = this.memory.get(sni);
            Set<Literal> truePositive = mem.t;
            Set<Literal> falsePositive = mem.u;
            Double conditionalPSI = mem.s;
            incorporateRule(sni, truePositive, falsePositive, conditionalPSI, mem.r);
        }
    */
    private void incorporateRule(SearchNodeInfo sni, Set<Literal> truePositive, Set<Literal> falsePositive, double conditionalPSI, double crossentropy) {
        if (CROSS_ENTROPY == this.mode) {
            incorporateCrossEntropyDrivenRule(sni, truePositive, falsePositive, conditionalPSI, crossentropy);
        } else if (ACCURACY == this.mode) {
            incorporateAccuracyDrivenRule(sni, truePositive, falsePositive, conditionalPSI);
        } else {
            throw new IllegalStateException();
        }
    }

    private void incorporateAccuracyDrivenRule(SearchNodeInfo sni, Set<Literal> truePositive, Set<Literal> falsePositive, double conditionalPSI) {
        for (Literal literal : truePositive) {
            this.positiveLiteralsEntailedByTheory.put(literal, 1.0);
        }
        for (Literal literal : falsePositive) {
            this.falseLiteralsEntailedByTheory.put(literal, 1.0);
        }
        this.rules.add(new Pair<>(conditionalPSI, addAllDiff(sni.getRule())));
        this.negateTheory.add(sni.getRuleICW());
    }

    private HornClause addAllDiff(HornClause rule) {
        List<Literal> body = Sugar.listFromCollections(rule.body().literals());
        body.add(new Literal(SpecialVarargPredicates.ALLDIFF, false, Sugar.listFromCollections(rule.variables())));
        return HornClause.create(rule.head(), body);
    }

    private void incorporateCrossEntropyDrivenRule(SearchNodeInfo sni, Set<Literal> truePositive, Set<Literal> falsePositive, double conditionalPSI, double crossentropy) {
        if (this.rules.stream().anyMatch(p -> p.getS().equals(sni.getRule()))) {
            System.out.println("this rule is already there");
        }

        System.out.println("rule incorporation\n\t" + sni.getRule() + "\n\t" + crossentropy + "\n\t" + conditionalPSI + "\n\t" + truePositive.size() + "\n\t" + falsePositive.size());

        for (Literal literal : truePositive) {
            Double val = this.positiveLiteralsEntailedByTheory.get(literal);
            if (null == val || val < conditionalPSI) {
                this.positiveLiteralsEntailedByTheory.put(literal, conditionalPSI);
//                System.out.println("poslit\t" + literal + "\t" + val + "\t" + conditionalPSI
//                        + "\n\t" +
//                        (Math.log(null == val ? this.implicitBound : val) / Math.log(2)) + "\t" + (Math.log(conditionalPSI) / Math.log(2)));
            }
        }

        double negatedConditional = 1.0 - conditionalPSI;
        for (Literal literal : falsePositive) {
            Double val = this.falseLiteralsEntailedByTheory.get(literal);
            if (null == val || val > negatedConditional) {
                this.falseLiteralsEntailedByTheory.put(literal, negatedConditional);
//                System.out.println("poslit\t" + literal + "\t" + val + "\t" + negatedConditional
//                        + "\n\t" + (Math.log(null == val ? 1 : val) / Math.log(2)) + "\t" + (Math.log(negatedConditional) / Math.log(2)));
            }
        }
        this.rules.add(new Pair<>(conditionalPSI, addAllDiff(sni.getRule())));
        this.negateTheory.add(sni.getRuleICW());
    }

    public void incorporateRule(Set<Literal> truePositive, Set<Literal> falsePositive, double conditionalPSI) {
        for (Literal literal : truePositive) {
            Double val = this.positiveLiteralsEntailedByTheory.get(literal);
            if (null == val || val < conditionalPSI) {
                this.positiveLiteralsEntailedByTheory.put(literal, conditionalPSI);
            }
        }

        double negatedConditional = 1 - conditionalPSI;
        for (Literal literal : falsePositive) {
            Double val = this.falseLiteralsEntailedByTheory.get(literal);
            if (null == val || val > negatedConditional) {
                this.falseLiteralsEntailedByTheory.put(literal, negatedConditional);
            }
        }
    }

    public void setStoreRangeRestricted(boolean storeRangeRestricted) {
        this.storeRangeRestricted = storeRangeRestricted;
    }

    public void predicateFinished() {
        synchronized (this) {
            System.out.println("creating new world");
            this.world = Matching.create(new Clause(evidence), Matching.THETA_SUBSUMPTION);
        }
    }


    // you need to call this function before evaluation of rules with this predicate
    // the scheme is to search each predicate sequentialy, i.e. \forall p/a learnRulesWithTarget(p/a)
    public void setTargetPredicate(Pair<String, Integer> predicate) {
        double totalPossibleFacts = 1;
        if (this.typing.values().stream().anyMatch(o -> null == o)) { // there is at least one type missing => continue as no types are introduced at all
            for (int idx = 0; idx < predicate.s; idx++) {
                totalPossibleFacts *= this.constants.size();
            }
        } else { // typed version
            Predicate pred = Predicate.create(predicate);
            System.out.println(pred);
            for (int idx = 0; idx < predicate.s; idx++) {
                String type = this.typing.get(new Pair<>(pred, idx));
                totalPossibleFacts *= this.typeHistogram.get(type).size();
                System.out.println(type + "\t" + this.typeHistogram.get(type).size());
            }
            System.out.println("total possible facts\t" + totalPossibleFacts + "\n");
        }


        double groundPredicate = 1.0 * this.groundTruth.stream().filter(l -> l.getPredicate().equals(predicate)).count();
        this.implicitBound = groundPredicate / totalPossibleFacts;
        this.possibleNegativeLiterals = totalPossibleFacts - groundPredicate;
        this.targetPredicate = predicate;
        this.predicateGroundTruth = this.groundTruth.stream().filter(l -> l.getPredicate().equals(predicate)).collect(Collectors.toList());
    }

    public Path getSrc() {
        return src;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void addRulesDeprected(List<Pair<Double, HornClause>> rules) {
        this.rules.addAll(rules);
    }


    // recopy from LeastHrebrandModel by OK
    private class TupleNotIn implements CustomPredicate {

        private Set<Literal> literals;

        private String name;

        private String predicate;

        TupleNotIn(String predicate, int arity, Set<Literal> literals) {
            this.predicate = predicate;
            this.name = tupleNotInPredicateName(predicate, arity);
            this.literals = literals;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public boolean isSatisfiable(Term... arguments) {
            if (Sugar.countNulls(arguments) > 0) {
                return true;
            }
            return !literals.contains(new Literal(predicate, arguments));
        }
    }

    // recopy from LeastHrebrandModel by OK
    private class HerbrandSolutionConsumer implements SolutionConsumer {

        private Literal head;

        private Pair<String, Integer> headSignature;

        private MultiMap<Pair<String, Integer>, Literal> herbrand;

        private HerbrandSolutionConsumer(Literal head, Pair<String, Integer> headSignature, MultiMap<Pair<String, Integer>, Literal> herbrand) {
            this.head = head;
            this.headSignature = headSignature;
            this.herbrand = herbrand;
        }

        @Override
        public void solution(Term[] template, Term[] solution) {
            herbrand.put(headSignature, LogicUtils.substitute(head, template, solution));
        }
    }


    public static void main(String[] args) {
        //t1();
        //t2();
        //t3();
        t4();
    }


    private static void t4() {
        Set<Literal> evidence = Clause.parse("p(a,b)").literals();
        Set<Literal> complement = Clause.parse("p(b,b)").literals();
        SubsampledDataset dataset = new SubsampledDataset(evidence, complement, null);

        // tp only
//        Set<Literal> tp = Clause.parse("p(b,b)").literals();
//        Set<Literal> fp = Clause.parse("").literals();
        // fp only
        Set<Literal> tp = Clause.parse("").literals();
        Set<Literal> fp = Clause.parse("p(c,c)").literals();

        for (Double cond : Sugar.list(0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0)) {
            System.out.println(cond + "\t" + dataset.computeEntropy(tp, fp, cond));
        }

        double conditional = 0.9;
        double entropy = dataset.computeEntropy(tp, fp, conditional);
        System.out.println("evidence\t" + evidence);
        System.out.println("complement\t" + complement);
        System.out.println("conditional\t" + conditional);
        System.out.println("tp\t" + tp);
        System.out.println("entropy\t" + entropy);
    }


    private static void t3() {
        Set<Literal> evidence = Clause.parse("p(a,b)").literals();
        Set<Literal> complement = Clause.parse("p(b,b)").literals();
        SubsampledDataset dataset = new SubsampledDataset(evidence, complement, null);

        Set<Literal> tp = Clause.parse("p(q,b)").literals();
        Set<Literal> fp = Clause.parse("").literals();

        double conditional = 0.9;
        double entropy = dataset.computeEntropy(tp, fp, conditional);
        System.out.println("evidence\t" + evidence);
        System.out.println("complement\t" + complement);
        System.out.println("conditional\t" + conditional);
        System.out.println("tp\t" + tp);
        System.out.println("entropy\t" + entropy);
    }

    private static void t2() {

        //HornClause rule = HornClause.parse("complex(1:A,2:B) <- protein_class(1:A,7:C), interaction(1:D,1:A), complex(1:D,2:B), protein_class(1:D,7:C)");
        //HornClause rule = HornClause.parse("protein_class(A,B) <- protein_class(D,B), complex(A,C), complex(D,C)");
        HornClause rule = HornClause.parse("protein_class(1:A,7:B) <- protein_class(1:D,7:B), complex(1:A,2:C), complex(1:D,2:C)");

        SubsampledDataset dataset = SubsampledDataset.load(Paths.get(".", "pac", "protein", "train.db.typed.uni1.db"));

        Pair<Set<Literal>, Set<Literal>> posNeg = dataset.evaluate(rule);
        Integer pos = posNeg.r.size();
        Integer neg = posNeg.s.size();
        System.out.println(rule + "\ncovers\t" + pos + "\t" + neg);


        Matching m = Matching.create(new Clause(LogicUtils.loadEvidence(Paths.get(".", "pac", "protein", "train.db.typed.uni1.db"))), Matching.THETA_SUBSUMPTION);
        Set<Literal> implied = Sugar.set();
        Pair<Term[], List<Term[]>> subs = m.allSubstitutions(rule.body(), 0, Integer.MAX_VALUE);
        for (Term[] img : subs.s) {
            implied.add(LogicUtils.substitute(rule.head(), subs.r, img));
        }
        System.out.println("implied\t" + implied.size() + "\t" + subs.s.size() + "\t" + implied);


        dataset.herbrandModel(Sugar.list(rule), dataset.groundTruth);
    }


    private static void t1() {

        HornClause hc = HornClause.parse("smoker(X) <- friend(X,Y), friend(Y,Z), smoker(Z)");
        System.out.println(hc);
        System.out.println(hc.head());
        System.out.println(hc.body());

        Clause evidence = Clause.parse("p(a),p(b),q(a),q(b)");
        HornClause rule = HornClause.parse("q(X) <- p(X)");

        SubsampledDataset dataset = new SubsampledDataset(evidence.literals(), Sugar.set(), null);

        Pair<Set<Literal>, Set<Literal>> posNeg = dataset.evaluate(rule);
        Integer pos = posNeg.r.size();
        Integer neg = posNeg.s.size();
        System.out.println("rule\t" + rule + "\non evidence\t" + evidence + "\npos\t" + pos + "\t\tneg\t" + neg);


    }


    // tohle je pouze pro debug
    private Set<Literal> herbrandModel(Collection<HornClause> rules, Collection<Literal> groundEvidence) {
        System.out.println("herbrandModel\n\t" + rules + "\n\t" + groundEvidence.size() + "\t" + groundEvidence);
        MultiMap<Pair<String, Integer>, Literal> herbrand = new MultiMap<Pair<String, Integer>, Literal>();


        Set<Pair<String, Integer>> headSignatures = new HashSet<Pair<String, Integer>>();
        for (HornClause rule : rules) {
            Literal head = rule.head();
            Pair<String, Integer> headSignature = new Pair<String, Integer>(head.predicate(), head.arity());
            headSignatures.add(headSignature);
            herbrand.set(headSignature, new HashSet<Literal>());
        }

        /*for (Literal groundLiteral : groundEvidence) {
            herbrand.put(new Pair<String, Integer>(groundLiteral.predicate(), groundLiteral.arity()), groundLiteral);
        }*/

        boolean changed;
        do {
            int herbrandSize0 = VectorUtils.sum(herbrand.sizes());
            Matching matching = new Matching(Sugar.<Clause>list(new Clause(Sugar.union(Sugar.flatten(herbrand.values()), groundEvidence))));
            for (Pair<String, Integer> predicate : headSignatures) {
                //may overwrite the previous ones which is actually what we want
                matching.getEngine().addCustomPredicate(new TupleNotIn(predicate.r, predicate.s, herbrand.get(predicate)));
            }
            for (HornClause rule : rules) {
                Literal head = rule.head();
                Pair<String, Integer> headSignature = new Pair<String, Integer>(head.predicate(), head.arity());
                SolutionConsumer solutionConsumer = new HerbrandSolutionConsumer(head, headSignature, herbrand);
                matching.getEngine().addSolutionConsumer(solutionConsumer);
                if (LogicUtils.isGround(head)) {
                    Clause query = LogicUtils.flipSigns(rule.toClause());
                    if (matching.subsumption(query, 0)) {
                        herbrand.put(headSignature, head);
                    }
                } else {
                    //Clause query = new Clause(LogicUtils.flipSigns(Sugar.union(rule.toClause().literals(), new Literal(tupleNotInPredicateName(head.predicate(), head.arity()), true, head.arguments()))));
                    Clause query = new Clause(LogicUtils.flipSigns(Sugar.union(LogicUtils.flipSigns(rule.body()).literals(), new Literal(tupleNotInPredicateName(head.predicate(), head.arity()), true, head.arguments()))));
//                    Clause query = new Clause(LogicUtils.flipSigns(rule.toClause().literals()));
                    matching.allSubstitutions(query, 0, Integer.MAX_VALUE); // Ondra psal pres email ze to nize muze skoncit v nekonecne smycce
//                    Pair<Term[], List<Term[]>> substitutions;
//                    do {
//                 //       not super optimal but the rule grounding will dominate the runtime anyway...
//                        substitutions = matching.allSubstitutions(query, 0, 512);
//                    } while (substitutions.s.size() > 0);
                }
                matching.getEngine().removeSolutionConsumer(solutionConsumer);
            }
            int herbrandSize1 = VectorUtils.sum(herbrand.sizes());
            changed = herbrandSize1 > herbrandSize0;
        } while (changed);

        System.out.println("herbrand\t" + herbrand.get(rules.iterator().next().head().getPredicate()).size() + "\t" + herbrand.get(rules.iterator().next().head().getPredicate()));

        return Sugar.setFromCollections(Sugar.flatten(herbrand.values()));
    }


}


