package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.SubsetFactory;
import ida.utils.Combinatorics;
import ida.utils.Sugar;
import ida.utils.collections.Counters;
import ida.utils.collections.DoubleCounters;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Quadruple;
import ida.utils.tuples.Triple;
import logicStuff.theories.GroundTheorySolver;
import logicStuff.theories.TheorySimplifier;
import logicStuff.theories.TheorySolver;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 22. 3. 2018.
 */
public class TestComputing {

    private final int theorySimplifierSubsumption = Matching.THETA_SUBSUMPTION;
    private final int theorySolverSubsumption = Matching.THETA_SUBSUMPTION;

//    {
//        System.out.println("ground all mode now");
//    }

    private final int theorySolverMode = TheorySolver.CUTTING_PLANES;//TheorySolver.GROUND_ALL;// TheorySolver.CUTTING_PLANES;

    // when done, change to private
    private final MultiMap<Constant, Literal> constantToLiterals;
    private final Set<Constant> constants;
    private final Set<Literal> queries;
    private final Set<Literal> evidence;

    private final boolean parallel = true;
    private final Map<Integer, MultiMap<Subset, Literal>> subsetToLiterals;
    private final Set<Subset> subsetConstants;
    private final Map<Subset, Constant> subsetToConstant;
    private final Map<Integer, BigDecimal> factorialCache = new HashMap<>();
    private final HashMap<Pair<Integer, Integer>, Double> cachedAncestors;
    private final Map<Constant, Subset> constantToSubset;


    private TestComputing(Set<Literal> evidence, Set<Literal> queries, Set<Constant> constants, MultiMap<Constant, Literal> constantToLiterals, Quadruple<Set<Subset>, Map<Integer, MultiMap<Subset, Literal>>, Map<Subset, Constant>, Map<Constant, Subset>> bitsetRepre) {
        this.evidence = evidence;
        this.queries = queries;
        this.constants = constants;
        this.constantToLiterals = constantToLiterals;
        this.subsetConstants = bitsetRepre.r;
        this.subsetToLiterals = bitsetRepre.s;
        this.subsetToConstant = bitsetRepre.t;
        this.constantToSubset = bitsetRepre.u;
        //System.out.println("sizes\t" + constants.size() + "\t" + subsetConstants.size() + "\t" + subsetConstants.iterator().next().debugSize());

        this.cachedAncestors = new HashMap<>();
        factorialCache.put(1, BigDecimal.ONE);
        factorialCache.put(0, BigDecimal.ONE);
    }

    public double votingEntailmentOrderedParallel(Set<Clause> theory, int k, Literal query) {
        double votes = 0;

        Set<Constant> startingSubset = LogicUtils.constantsFromLiteral(query);
        List<Set<Constant>> queue = Sugar.list(startingSubset);
        Set<Constant> cRest = Sugar.collectionDifference(constants, startingSubset);

        // neni pomoci apriori stylu generovani, ale pomoci drzeni podmnozin explicitne
        // nicmene apriori styl se projevuje v tom ze se prozkouma pouze potomek ktery ma prave dva rodice
        int level = startingSubset.size();
        while (!queue.isEmpty()) {
            Pair<Counters<Set<Constant>>, Double> layer = Sugar.parallelStream(queue, true)
                    .map(subset -> {
                        Set<Literal> evidence = maskEvidence(subset);
                        if (!evidence.isEmpty()) {
                            TheorySolver solver = new TheorySolver();
                            solver.setSubsumptionMode(theorySolverSubsumption);
                            solver.setMode(theorySolverMode);
                            Set<Clause> merged = Sugar.setFromCollections(theory, evidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
                            Set<Literal> proveable = solver.solve(merged);
                            if (null == proveable) {
                                return null; // this C' \cup \Phi is inconsistent
                            } else if (proveable.contains(query)
                                    && TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode)) {
//                                System.out.println("entails\t" + subset);
                                //double successors = Combinatorics.factorial(subset.size() - startingSubset.size())
                                //        * Combinatorics.variantion(constants.size() - subset.size(), k - subset.size());
                                double successors = Combinatorics.factorial(subset.size())
                                        * Combinatorics.variantion(constants.size() - subset.size(), k - subset.size());


                                return new Pair<>(new Counters<Set<Constant>>(), successors);
                            }
                        }

                        Counters<Set<Constant>> followers = new Counters<>();
                        for (Constant cr : cRest) {
                            Set<Constant> refinement = Sugar.union(subset, cr);
                            if (refinement.size() > subset.size() && refinement.size() <= k) {
                                followers.increment(refinement);
                            }
                        }
                        return new Pair<>(followers, 0.0d);
                    })
                    .filter(p -> null != p)
                    .reduce(new Pair<>(new Counters<>(), 0.0d),
                            (p1, p2) -> new Pair<>(p1.getR().addAll(p2.getR()), p1.getS() + p2.getS()));

            int candidateSize = level;
            votes += layer.getS();
            queue = layer.getR().keySet().stream()
                    .filter(key -> layer.getR().get(key) == candidateSize + 1)
                    .collect(Collectors.toList());
            level++;
        }
        return votes;
    }


    //pro dokazani vice query prorezani plati dal (conter==2), ale pozor aby se nepocitalo neco vicekrat -- musi se preposilat literaly ktere to ma implikovat a a posilat nahoru jenom ty ktere maji support dva , tak se da pouzit stavajici strategie
    //dalsi rozdil je ze pro kazdy literal z query to vraci double, tzn # votes

    public DoubleCounters<Literal> votingEntailmentOrderedReduction(Set<Clause> theory, int k, boolean parallel, String allowedPredicate, int allowedArity) {
        System.out.println("dev version, vyzkouseni redukcniho kroku jinak nez pres counters");
        System.out.println("prezitek...");
        DoubleCounters<Literal> votes = new DoubleCounters<>();
        // opet apriori like pristup generovani ale podmnozin (bez explicitniho usporadani)
        // narozdil od dalsich, atribut entailment node queries zde ma vyznam tech literalu, ktere jsou uz implikovane predky, aby nedoslo k viceronasobnemu pricteni hlasu


        boolean allPredicatesAllowed = null == allowedPredicate;
//        System.out.println("\n\ncomputing via subsets");
//        MultiMap<Integer, Subset> forbidden = minimalEmpty(this.subsetConstants, k);
        System.out.println("\n\nwithout forbibidden");
        MultiMap<Integer, Subset> forbidden = new MultiMap<>();

        Set<EntailmentNode<Subset>> queue = subsetConstants.stream()
                .map(constant -> EntailmentNode.create(constant, Sugar.set()))
                .collect(Collectors.toSet());

        for (int level = 1; level <= k; level++) {
            Set<Subset> currentForbidden = forbidden.containsKey(level + 1) ? forbidden.get(level + 1) : Sugar.set();
            System.out.println("there are forbidden\t" + currentForbidden.size());

            System.out.println("computing level\t" + level + "\twith candidates\t" + queue.size());
            List<Pair<Set<Literal>, List<Subset>>> candidates = Sugar.parallelStream(queue, parallel)
                    .map(node -> {
                        Set<Literal> evidence = maskEvidence(node.getConstants());
                        Set<Literal> impliedLiterals = ConcurrentHashMap.newKeySet();
                        if (!evidence.isEmpty()) {
                            TheorySolver solver = new TheorySolver();
                            solver.setSubsumptionMode(theorySolverSubsumption);
                            solver.setMode(theorySolverMode);
                            Set<Clause> merged = Sugar.setFromCollections(theory, evidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
                            Set<Literal> proveable = solver.solve(merged);

                            if (null == proveable) {
                                return null; // this C' \cup \Phi is inconsistent
                            }
                            Sugar.parallelStream(proveable, parallel)
                                    .filter(query -> !node.getQueries().contains(query)
                                            && (allPredicatesAllowed || (allowedPredicate.equals(query.predicate()) && allowedArity == query.arity())) // not a nice hack around
                                            // add evidence filter here, smt like
                                            //&& !this.evidence.contains(query)
                                            && TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode))
                                    .forEach(query -> {
                                        //double impliedSuccessors = Combinatorics.factorial(node.getConstants().size())
                                        //        * Combinatorics.variantion(constants.size() - node.getConstants().size(), k - node.getConstants().size());
                                        double impliedSuccessors = Combinatorics.factorialBig(node.getConstants().size())
                                                .multiply(Combinatorics.variantionBig(constants.size() - node.getConstants().size(), k - node.getConstants().size()))
                                                .doubleValue();
                                        if ("interaction(protein_YLR328w, protein_YLR328w)".equals(query.toString())) {
                                            System.out.println(query
                                                    + "\t" + impliedSuccessors
                                                    + "\t" + constants.size()
                                                    + "\t" + node.getConstants().size()
                                                    + "\t" + canon(subsetToConst(node.getConstants()))
                                                    + "\t" + System.nanoTime());
                                        }
                                        synchronized (votes) {
                                            votes.add(query, impliedSuccessors);
                                        }
                                        impliedLiterals.add(query);
                                    });
                        }
                        evidence = null; // GC
                        if (node.getConstants().size() == k) {
                            // prune the successors, they won't be evaluated, so do not generate them
                            return null;
                        }
                        return new Pair<>(Sugar.setFromCollections(impliedLiterals, node.getQueries())
                                , subsetConstants.stream()
                                .filter(c -> !node.getConstants().contains(c))
                                .map(c -> node.getConstants().union(c))
                                .filter(subset -> !currentForbidden.contains(subset))
                                .collect(Collectors.toList()));
                    })
                    .filter(p -> null != p)
                    .collect(Collectors.toList());

            int candidateSize = level;
            queue = Sugar.set();
            HashMap<Subset, List<Set<Literal>>> cache = new HashMap<>();
            /*for (Pair<Set<Literal>, List<Subset>> pair : candidates) {
                Set<Literal> entailed = pair.getR();
                for (Subset subset : pair.getS()) {
                    if (!cache.containsKey(subset)) {
                        cache.put(subset, Sugar.list());
                    }
                    cache.get(subset).add(entailed);
                    if (cache.get(subset).size() == candidateSize) {
                        Set<Literal> union = cache.get(subset).stream().flatMap(impl -> impl.stream()).collect(Collectors.toSet());
                        Set<Literal> myUnion = Sugar.set();
                        for (Set<Literal> impli : cache.get(subset)) {
                            myUnion.addAll(impli);
                        }
                        System.out.println("s\t" + canon(subsetToConst(subset))
                                + "\t" + cache.get(subset).size()
                                + "\t" + myUnion.size()
                                + "\t" + cache.get(subset).stream().flatMap(impl -> impl.stream()).collect(Collectors.toSet()).size()
                        );

                        queue.add(EntailmentNode.loadResults(subset
                                , union));
                        cache.remove(subset);
                    }
                }
            }*/
            for (Pair<Set<Literal>, List<Subset>> pair : candidates) {
                Set<Literal> implied = pair.getR();
                for (Subset subset : pair.getS()) {
                    if (!cache.containsKey(subset)) {
                        cache.put(subset, Sugar.list());
                    }
                    cache.get(subset).add(implied);
                }
            }
            for (Map.Entry<Subset, List<Set<Literal>>> entry : cache.entrySet()) {
                if (entry.getValue().size() <= candidateSize) {
                    continue;
                }
                Subset subset = entry.getKey();
                List<Set<Literal>> impliedLiterals = entry.getValue();
                Set<Literal> union = impliedLiterals.stream().flatMap(impl -> impl.stream()).collect(Collectors.toSet());
//                Set<Literal> myUnion = Sugar.set();
//                for (Set<Literal> impli : impliedLiterals) {
//                    myUnion.addAll(impli);
//                }
//                System.out.println("s\t" + canon(subsetToConst(subset))
//                        + "\t" + cache.get(subset).size()
//                        + "\t" + myUnion.size()
//                        + "\t" + impliedLiterals.stream().flatMap(impl -> impl.stream()).collect(Collectors.toSet()).size()
//                );

                queue.add(EntailmentNode.create(subset, union));
            }
            cache = null;

            System.out.println("queue\nlen\n" + queue.size());
//            queue = Sugar.parallelStream(candidates.entrySet(), parallel)
//                    .filter(entry -> candidateSize + 1 == entry.getValue().size()) // apriori like candidate generation, it has to have exactly two parents
//                    .map(e -> EntailmentNode.loadResults(e.getKey(), Sugar.union(e.getValue().get(0), e.getValue().get(1))))
//                    .collect(Collectors.toList());
        }
        return votes;
    }

    public Set<Literal> kEntailmentOrderedDataDrive(Set<Clause> theory, int k, boolean parallel, String allowedPredicate, int allowedArity) {
        boolean verbouse = false;
        System.out.println("dev version kEnt, data driven, pristup 1");

        Set<Subset> needed = mineNeedable(theory, k);

        System.out.println("reporting neededable\t" + needed.size());
        if (verbouse) {
            needed.forEach(s -> System.out.println(canon(s) + "\t" + s.toString()));
        }

        Set<Literal> entailed = ConcurrentHashMap.newKeySet();

        prepareFactorialCache(k);

        boolean allPredicatesAllowed = null == allowedPredicate;
        Set<Subset> baseElements = baseSubsets(this.subsetToLiterals, k);
        int max = baseElements.stream().mapToInt(Subset::size).max().orElse(1);
        System.out.println("baseElements\t" + baseElements.size());
        if (verbouse) {
            System.out.println("baseElements\t" + baseElements.size());
            baseElements.forEach(s -> System.out.println("\t" + canon(subsetToConst(s))));
        }
        MultiMap<Integer, Subset> queue = new MultiMap<>();

        // init via hint, needable... use baseElements instead for potentially bigger space
        needed.forEach(subset -> queue.put(subset.size(), subset));

        List<Subset> clashMakers = Sugar.list();
        for (int level = 1; level <= k; level++) {
            if (!queue.containsKey(level)) {
                continue;
            }
            Set<Subset> currentQueue = queue.get(level);
            System.out.println("computing level\t" + level + "\twith candidates\t" + currentQueue.size());
            int finalLevel = level;
            List<Pair<Subset, Stream<Subset>>> candidates = Sugar.parallelStream(currentQueue, parallel)
                    .map(subset -> {
                        Set<Literal> currentEvidence = maskEvidence(subset);
                        if (!currentEvidence.isEmpty()) {
                            TheorySolver solver = new TheorySolver();
                            solver.setSubsumptionMode(theorySolverSubsumption);
                            solver.setMode(theorySolverMode);
                            Set<Clause> merged = Sugar.setFromCollections(theory, currentEvidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
                            Set<Literal> proveable = solver.solve(merged);

                            if (null == proveable) {
                                return new Pair<>(subset, null);
                                //return null; // this C' \cup \Phi is inconsistent
                            }
                            Sugar.parallelStream(proveable, parallel)
                                    .filter(query -> (allPredicatesAllowed || (allowedPredicate.equals(query.predicate()) && allowedArity == query.arity())) // not a nice hack around
                                            && !this.evidence.contains(query))
                                    .forEach(query -> {
                                        if (!entailed.contains(query)
                                                && TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode)) {
                                            entailed.add(query);
                                        }
                                    });
                        }

                        currentEvidence = null; // GC
                        if (subset.size() == k) {
                            // prune the successors, they won't be evaluated, so do not generate them
                            return null;
                        }
                        return new Pair<>(null
                                , baseElements.stream()
                                .map(c -> subset.union(c))
                                .filter(refinement -> refinement.size() > subset.size()
                                        && refinement.size() <= k
                                        && noClash(refinement, clashMakers))
                                // pripadne zkusit sem dat .collect(Collectors.toList()) kdyby byl nejaky problem s rychlosti, pameti, etc.
                        );
                    })
                    .filter(p -> null != p)
                    .map(triple -> (Pair<Subset, Stream<Subset>>) triple)
                    .collect(Collectors.toList());

            List<Subset> forbidden = Sugar.list();

            for (Pair<Subset, Stream<Subset>> pair : candidates) {
                if (null == pair.r) { // process children
                    pair.s.forEach(successor -> {
                        queue.put(successor.size(), successor);
                    });
                } else { // this one makes inconsistency
                    forbidden.add(pair.r);
                }
            }

            clashMakers.addAll(forbidden);
            queue.remove(level);
            if (level > max) {
                SubsetFactory.getInstance().clear(level);
            }
        }
        System.out.println("inconsistent found\t" + clashMakers.size());

        return entailed;
    }

    // pokus o pridani prorezavani pro mene dokazovani
    public Set<Literal> kEntailmentOrderedDataDriveScout(Set<Clause> theory, int k, boolean parallel, String allowedPredicate, int allowedArity) {
        boolean verbouse = false;
//        boolean usePruning = true;
//        System.out.println("dev version kEnt, data driven, pristup 2, scount\t" + usePruning);
        System.out.println("dev version kEnt, data driven, pristup 3, scount");

//        MutableInteger count = new MutableInteger(0);

        Set<Subset> needed = mineNeedable(theory, k);

        System.out.println("reporting neededable\t" + needed.size());
        if (verbouse) {
            needed.forEach(s -> System.out.println(canon(s) + "\t" + s.toString()));
        }

        Set<Literal> entailed = ConcurrentHashMap.newKeySet();

        prepareFactorialCache(k);

        boolean allPredicatesAllowed = null == allowedPredicate;
        Set<Subset> baseElements = baseSubsets(this.subsetToLiterals, k);
        int max = baseElements.stream().mapToInt(Subset::size).max().orElse(1);
        System.out.println("baseElements\t" + baseElements.size());
        if (verbouse) {
            System.out.println("baseElements\t" + baseElements.size());
            baseElements.forEach(s -> System.out.println("\t" + canon(subsetToConst(s))));
        }
        MultiMap<Integer, RMutableNode> queue = new MultiMap<>();

        // init via hint, needable... use baseElements instead for potentially bigger space
        needed.forEach(subset -> queue.put(subset.size(), RMutableNode.create(subset, true, Sugar.set())));

        List<Subset> clashMakers = Sugar.list();
        for (int level = 1; level <= k; level++) {
            if (!queue.containsKey(level)) {
                continue;
            }

            Map<Subset, Set<Literal>> maskCache = new HashMap<>();

            Set<RMutableNode> currentQueue = queue.get(level);
            System.out.println("computing level\t" + level + "\twith candidates\t" + currentQueue.size());
            int finalLevel = level;
            List<Pair<Subset, Stream<RMutableNode>>> candidates = Sugar.parallelStream(currentQueue, parallel)
                    .map(RMutableNode -> {
                        Subset subset = RMutableNode.getConstants();
                        Set<Literal> proved = Sugar.set();


                        if (RMutableNode.isEvaluate()) {
//                            synchronized (count) {
//                                count.increment();
//                            }
                            Set<Literal> currentEvidence = maskEvidence(subset);
                            if (!currentEvidence.isEmpty()) {
                                TheorySolver solver = new TheorySolver();
                                solver.setSubsumptionMode(theorySolverSubsumption);
                                solver.setMode(theorySolverMode);
                                Set<Clause> merged = Sugar.setFromCollections(theory, currentEvidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
                                Set<Literal> proveable = solver.solve(merged);

                                if (null == proveable) {
                                    return new Pair<>(subset, null);
                                    //return null; // this C' \cup \Phi is inconsistent
                                }
                                proved.addAll(Sugar.parallelStream(proveable, parallel)
                                        .filter(query -> (allPredicatesAllowed || (allowedPredicate.equals(query.predicate()) && allowedArity == query.arity())) // not a nice hack around
                                                && !this.evidence.contains(query))
                                        .filter(query ->
                                                //entailed.contains(query) || -- tohle tady nemuze byt, protoze by se mohl se potom mohlo neco nepropagovat
                                                RMutableNode.getProved().contains(query)
                                                        || TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode))
                                        .map(query -> {
                                            entailed.add(query); // tady se provadi jedno redundantni pridani, protoze kdyz je v RMutableNode, tak musi byt uz v entailed
                                            return query;
                                        }).collect(Collectors.toList()));
                            }

                            currentEvidence = null; // GC
                        } else {
                            if (subset.size() < k) { // if subset.size() == k, we do not loadResults followers anyway :))
                                GroundTheorySolver inconsistentChecker = new GroundTheorySolver(theory, Sugar.union(this.evidence, RMutableNode.getProved()));
                                if (inconsistentChecker.solve() == null) {
                                    return null;
                                }
                            }
                            proved = Sugar.set();
                        }

//                        System.out.println(canon(subset) + "\t" + canon(proved));

                        if (subset.size() == k) {
                            // prune the successors, they won't be evaluated, so do not generate them
                            return null;
                        }
                        Set<Literal> finalProved = Sugar.union(proved, RMutableNode.getProved());
                        return new Pair<>(null
                                , baseElements.stream()
                                .map(c -> subset.union(c))
                                .filter(refinement -> refinement.size() > subset.size()
                                        && refinement.size() <= k
                                        && noClash(refinement, clashMakers))
                                .map(refinement -> {
//                                    if (usePruning) {
//                                    System.out.println("tady predat cache ktera bude pro jednu vrstvu, evaluace streamu je na konci a sekvencni, takze to pujde, akorat pamet overhead");
                                    Set<Literal> refinementsEvidence = maskCache.get(refinement);
                                    if (null == refinementsEvidence) {
                                        refinementsEvidence = maskEvidence(refinement);
                                        maskCache.put(refinement, refinementsEvidence);
                                    }
                                    Iterable<Literal> currentEvidence = Sugar.iterable(finalProved, refinementsEvidence);
                                    Set<Literal> possibleProved = mineNeedable(theory, k, currentEvidence);
                                    boolean neededToEvaluate = !finalProved.containsAll(possibleProved);
                                    return RMutableNode.create(refinement, neededToEvaluate, finalProved);
//                                    }
//                                    return RMutableNode.loadResults(refinement, true, Sugar.set());
                                })
                                // pripadne zkusit sem dat .collect(Collectors.toList()) kdyby byl nejaky problem s rychlosti, pameti, etc.
                        );
                    })
                    .filter(p -> null != p)
                    .map(pair -> (Pair<Subset, Stream<RMutableNode>>) pair)
                    .collect(Collectors.toList());

            List<Subset> forbidden = Sugar.list();

            for (Pair<Subset, Stream<RMutableNode>> pair : candidates) {
                if (null == pair.r) { // process children
                    pair.s.forEach(RMutableNode -> queue.put(RMutableNode.getConstants().size(), RMutableNode));
                } else { // this one makes inconsistency
                    forbidden.add(pair.r);
                }
            }

            clashMakers.addAll(forbidden);
            queue.remove(level);
            if (level > max) {
                SubsetFactory.getInstance().clear(level);
            }
        }
        System.out.println("inconsistent found\t" + clashMakers.size());
//        System.out.println("count of sat calls\t" + count.value());

//        System.out.println("overall proved\n" + canon(entailed));
        return entailed;
    }

    public Set<Literal> kEntailmentOrderedDataDriveScoutPL(PossibilisticTheory theory, int k, boolean parallel, String allowedPredicate, int allowedArity, boolean withoutKEntailment) {
        boolean verbouse = false;
//        System.out.println("dev version kEnt, data driven + scout for possibilistic theory" + ((withoutKEntailment) ? "; only PL !!!!" : ""));

        Set<Subset> needed;
        if (withoutKEntailment) {
            Subset all = SubsetFactory.getInstance().get(constants.size(), 0);
            for (Subset s : subsetConstants) {
                all = all.union(s);
            }
            needed = Sugar.set(all);
            k = all.size();
        } else {
            needed = mineNeedable(theory.allRules(), k);
        }

        System.out.println("reporting initial\t" + needed.size());
        if (verbouse || true) { // debug here !!!
            needed.forEach(s -> System.out.println(canon(s) + "\t" + s.toString()));
        }

        Set<Literal> entailed = ConcurrentHashMap.newKeySet();

        prepareFactorialCache(k);

        boolean allPredicatesAllowed = null == allowedPredicate;
        Set<Subset> baseElements = baseSubsets(this.subsetToLiterals, k);
        int max = baseElements.stream().mapToInt(Subset::size).max().orElse(1);
        System.out.println("baseElements\t" + baseElements.size());
        if (verbouse) {
            System.out.println("baseElements\t" + baseElements.size());
            baseElements.forEach(s -> System.out.println("\t" + canon(subsetToConst(s))));
        }
        MultiMap<Integer, PossibilisticMutableNode> queue = new MultiMap<>();

        // init via hint, needable... use baseElements instead for potentially bigger space
        needed.forEach(subset -> queue.put(subset.size(), PossibilisticMutableNode.create(subset, true, Sugar.set(), theory)));

        List<Subset> clashMakers = Sugar.list();
        for (int level = (withoutKEntailment) ? k : 1; level <= k; level++) {
            if (!queue.containsKey(level)) {
                continue;
            }

            // by se mohla jeste preposilat z predchozi vrstvy
            Map<Subset, Set<Literal>> maskCache = new HashMap<>();

            Set<PossibilisticMutableNode> currentQueue = queue.get(level);
            System.out.println("computing level\t" + level + "\twith candidates\t" + currentQueue.size());
            int finalLevel = level;
            int finalK = k;
            List<Pair<Subset, Stream<PossibilisticMutableNode>>> candidates = Sugar.parallelStream(currentQueue, parallel)
                    .map(possibilisticMutableNode -> {
                        Subset subset = possibilisticMutableNode.getConstants();
                        Set<Literal> proved = Sugar.set();

                        boolean inconsistentTheory = false;
                        if (!possibilisticMutableNode.isEvaluate() && !possibilisticMutableNode.wasTheoryTrimmed()) {
                            GroundTheorySolver inconsistentChecker = new GroundTheorySolver(possibilisticMutableNode.getTheory().allRules(), Sugar.union(this.evidence, possibilisticMutableNode.getImpliedLiterals()));
                            if (inconsistentChecker.solve() == null) {
                                inconsistentTheory = true;
                            }
                        }

                        PossibilisticTheory outputTheory = possibilisticMutableNode.getTheory();
                        if (possibilisticMutableNode.isEvaluate() ||
                                possibilisticMutableNode.wasTheoryTrimmed() ||
                                inconsistentTheory) {
                            Set<Literal> currentEvidence = maskEvidence(subset);
                            if (!currentEvidence.isEmpty()) {
                                Triple<PossibilisticTheory, Set<Literal>, Collection<Clause>> triple = findConsistentTheoryAndProveable(possibilisticMutableNode.getTheory(), currentEvidence);

                                if (null == triple) {
                                    return new Pair<>(subset, null);
                                    //return null; // this C' \cup \Phi is inconsistent no matter which soft rules are removed

                                }

                                Set<Clause> merged = Sugar.unique(triple.r.allRules()
                                        , currentEvidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));

                                outputTheory = triple.r;
                                Set<Literal> cachedProved = outputTheory.size() == possibilisticMutableNode.getTheory().size()
                                        ? possibilisticMutableNode.getImpliedLiterals() : Sugar.set();
                                Set<Literal> proveable = triple.s;

                                PossibilisticTheory finalOutputTheory = outputTheory;
                                proved.addAll(Sugar.parallelStream(proveable, parallel)
                                        .filter(query -> (allPredicatesAllowed || (allowedPredicate.equals(query.predicate()) && allowedArity == query.arity())) // not a nice hack around
                                                && !this.evidence.contains(query))
                                        .filter(query ->
                                                //entailed.contains(query) || -- tohle tady nemuze byt, protoze by se mohl se potom mohlo neco nepropagovat
                                                cachedProved.contains(query)
                                                        || TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode))
                                        .map(query -> {
                                            entailed.add(query); // redundantni pridani, protoze kdyz je to v possibilisticMutableNode.getProved, tak uz to musi byt v entailed
                                            return query;
                                        }).collect(Collectors.toList()));
                            }

                            currentEvidence = null; // GC
                        } // no other else here, should be done beforehead

//                        System.out.println(canon(subset) + "\t" + canon(proved));


                        if (subset.size() >= finalK) {
                            // prune the successors, they won't be evaluated, so do not generate them
                            return null;
                        }

                        // toto dulezite
                        Set<Literal> finalProved = (outputTheory.size() == possibilisticMutableNode.getTheory().size())
                                ? Sugar.union(proved, possibilisticMutableNode.getImpliedLiterals()) : proved;
                        PossibilisticTheory finalOutputTheory = outputTheory;
                        return new Pair<>(null
                                , baseElements.stream()
                                .map(c -> subset.union(c))
                                .filter(refinement -> refinement.size() > subset.size()
                                        && refinement.size() <= finalK
                                        && noClash(refinement, clashMakers))
                                .map(refinement -> {
                                    Set<Literal> refinementsEvidence = maskCache.get(refinement);
                                    if (null == refinementsEvidence) {
                                        refinementsEvidence = maskEvidence(refinement);
                                        maskCache.put(refinement, refinementsEvidence);
                                    }
                                    Iterable<Literal> currentEvidence = Sugar.iterable(finalProved, refinementsEvidence);
                                    boolean neededToEvaluate;
                                    if (finalOutputTheory.size() < possibilisticMutableNode.getTheory().size()) {
                                        neededToEvaluate = true;
                                    } else {
                                        Set<Literal> possibleProved = mineNeedable(finalOutputTheory.allRules(), finalK, currentEvidence);
                                        neededToEvaluate = !finalProved.containsAll(possibleProved);
                                    }
                                    return PossibilisticMutableNode.create(refinement, neededToEvaluate, finalProved, finalOutputTheory);
                                })
                                // pripadne zkusit sem dat .collect(Collectors.toList()) kdyby byl nejaky problem s rychlosti, pameti, etc.
                        );
                    })
                    .filter(p -> null != p)
                    .map(pair -> (Pair<Subset, Stream<PossibilisticMutableNode>>) pair)
                    .collect(Collectors.toList());


            List<Subset> forbidden = Sugar.list();

            for (Pair<Subset, Stream<PossibilisticMutableNode>> pair : candidates) {
                if (null == pair.r) { // process children
                    pair.s.forEach(possMutableNode -> queue.put(possMutableNode.getConstants().size(), possMutableNode));
                } else { // this one makes inconsistency
                    forbidden.add(pair.r);
                }
            }

            clashMakers.addAll(forbidden);
            queue.remove(level);
            if (level > max) {
                SubsetFactory.getInstance().clear(level);
            }
        }
        System.out.println("inconsistent found\t" + clashMakers.size());
//        System.out.println("count of sat calls\t" + count.value());

//        System.out.println("overall proved\n" + canon(entailed));
        return entailed;
    }


    // return null iff removing all soft rules cannot make it consistent
    private Triple<PossibilisticTheory, Set<Literal>, Collection<Clause>> findConsistentTheoryAndProveable(PossibilisticTheory theory, Set<Literal> currentEvidence) {
        TheorySolver solver = new TheorySolver();
        solver.setSubsumptionMode(theorySolverSubsumption);
        solver.setMode(theorySolverMode);
        /*
        for now, at least hard rules
        if (theory.getImplications().isEmpty()) {
            return null; // cannot trim any soft rule
        }*/

        List<Clause> rules = Sugar.list();
        rules.addAll(theory.getHardRules());
        int baseRules = rules.size();
        //int lowerBound = rules.size() + 1; // at least one soft rule
        int lowerBound = rules.size(); // for now, just hard rules
        theory.getSoftRules().stream().forEach(p -> rules.add(p.s));
        int upperBound = rules.size();

        Set<Literal> proveable = solver.solve(rules.subList(0, upperBound),currentEvidence);
        if (null != proveable) {
            return new Triple<>(theory, proveable, rules);
        }
        /* uncomment this to emulate kE in vanila logic
        if (true) {
            // trying to make k-like mode
            return null;
        }
        */
        proveable = solver.solve(rules.subList(0, lowerBound),currentEvidence); // at least one soft rule with the biggest weight
        if (null == proveable) {
//            System.out.println("tady asi chyba");
//            System.out.println(currentEvidence.stream().map(Object::toString).collect(Collectors.joining(", ")));
//            System.out.println(rules.subList(0,lowerBound).stream().map(Object::toString).collect(Collectors.joining("; ")));
            return null; // even the rule with the biggest weight makes it inconsistent
        }

        // lower bound -- is always consistent
        // upper bound -- we do not know, but not need to be tested
        // if lower == upper, then end, it is consistent
        Set<Literal> lowerBoundProved = proveable;
        while (true) {
            if (lowerBound + 1 == upperBound) {
                return new Triple<>(PossibilisticTheory.create(theory.getHardRules(), theory.getSoftRules().subList(0, lowerBound - baseRules))
                        , lowerBoundProved
                        , rules.subList(0, lowerBound));
            }

            int mid = lowerBound + (upperBound - lowerBound) / 2;

            if (mid == lowerBound) {
                mid = upperBound;
            }

            proveable = solver.solve(rules.subList(0, mid));
            if (null == proveable) {
                // lower bound stays the same; upper is lowered
                upperBound = mid;
            } else {
                // lower bound is increased; upper stays the same
                lowerBound = mid;
                lowerBoundProved = proveable;
            }
        }

/*
        List<Clause> rules = currentEvidence.stream().map(l -> new Clause(l)).collect(Collectors.toList());
        rules.addAll(theory.getHardRules());
        int baseRules = rules.size();
        int lowerBound = rules.size() + 1; // at least one soft rule
        theory.getImplications().stream().forEach(p -> rules.add(p.s));
        int upperBound = rules.size();

        Set<Literal> proveable = solver.solve(rules.subList(0, upperBound));
        if (null != proveable) {
            return new Triple<>(theory, proveable, rules);
        }

        proveable = solver.solve(rules.subList(0, lowerBound)); // at least one soft rule with the biggest weight
        if (null == proveable) {
            return null; // even the rule with the biggest weight makes it inconsistent
        }

        // lower bound -- is always consistent
        // upper bound -- we do not know, but not need to be tested
        // if lower == upper, then end, it is consistent
        Set<Literal> lowerBoundProved = proveable;
        while (true) {
            if (lowerBound + 1 == upperBound) {
                return new Triple<>(PossibilisticTheory.loadResults(theory.getHardRules(), theory.getImplications().subList(0, lowerBound - baseRules))
                        , lowerBoundProved
                        , rules.subList(0, lowerBound));
            }

            int mid = lowerBound + (upperBound - lowerBound) / 2;

            if (mid == lowerBound) {
                mid = upperBound;
            }

            proveable = solver.solve(rules.subList(0, mid));
            if (null == proveable) {
                // lower bound stays the same; upper is lowered
                upperBound = mid;
            } else {
                // lower bound is increased; upper stays the same
                lowerBound = mid;
                lowerBoundProved = proveable;
            }
        }*/
    }


    public DoubleCounters<Literal> votingEntailmentOrderedDataDrive(Set<Clause> theory, int k,
                                                                    boolean parallel, String allowedPredicate, int allowedArity, boolean experimentalLastLayer) {
        System.out.println("dev version, data driven, pristup 1");
        System.out.println("experimental last layer\t" + experimentalLastLayer);
        boolean verbouse = false;

        Set<Subset> needed = mineNeedable(theory, k);

        System.out.println("reporting neededable\t" + needed.size());
        if (verbouse) {
            needed.forEach(s -> System.out.println(canon(s) + "\t" + s.toString()));
        }

        DoubleCounters<Literal> finalLayerVotes = new DoubleCounters<>();
        DoubleCounters<Literal> votes = new DoubleCounters<>();
        // opet apriori like pristup generovani ale podmnozin (bez explicitniho usporadani)
        // narozdil od dalsich, atribut entailment node queries zde ma vyznam tech literalu, ktere jsou uz implikovane predky, aby nedoslo k viceronasobnemu pricteni hlasu

        prepareFactorialCache(k);

        boolean allPredicatesAllowed = null == allowedPredicate;
        System.out.println("\n\ncomputing via subsets");
        Set<Subset> baseElements = baseSubsets(this.subsetToLiterals, k);
        int max = baseElements.stream().mapToInt(Subset::size).max().orElse(1);
        System.out.println("baseElements\t" + baseElements.size());
        if (verbouse) {
            System.out.println("baseElements\t" + baseElements.size());
            baseElements.forEach(s -> System.out.println("\t" + canon(subsetToConst(s))));
        }
        Map<Integer, MultiMap<Subset, Literal>> queue = new HashMap<>();

        // init via hint, needable... use baseElements instead for potentially bigger space
        needed.forEach(subset -> {
            if (!queue.containsKey(subset.size())) {
                queue.put(subset.size(), new MultiMap<>());
            }
            queue.get(subset.size()).putAll(subset, Sugar.set());
        });

        List<Subset> clashMakers = Sugar.list();
        for (int level = 1; level <= k; level++) {
            if (!queue.containsKey(level)) {
                continue;
            }
            MultiMap<Subset, Literal> currentQueue = queue.get(level);
            System.out.println("computing level\t" + level + "\twith candidates\t" + currentQueue.size());
            int finalLevel = level;
            List<Triple<Subset, Set<Literal>, Stream<Subset>>> candidates = Sugar.parallelStream(currentQueue.entrySet(), parallel)
                    .map(entry -> {
                        Set<Literal> currentEvidence = maskEvidence(entry.getKey());
                        Collection<Literal> impliedLiterals = new ConcurrentLinkedQueue<>();//ConcurrentHashMap.newKeySet();
                        if (!currentEvidence.isEmpty()) {
                            TheorySolver solver = new TheorySolver();
                            solver.setSubsumptionMode(theorySolverSubsumption);
                            solver.setMode(theorySolverMode);
                            Set<Clause> merged = Sugar.setFromCollections(theory, currentEvidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
                            Set<Literal> proveable = solver.solve(merged);

                            if (null == proveable) {
                                return new Triple<>(entry.getKey(), null, null);
                                //return null; // this C' \cup \Phi is inconsistent
                            }
                            if (experimentalLastLayer && finalLevel == k) {
                                proveable.stream()
                                        .filter(query -> !entry.getValue().contains(query)
                                                //// not a nice hack around
                                                && (allPredicatesAllowed || (allowedPredicate.equals(query.predicate()) && allowedArity == query.arity()))
                                                // add evidence filter here, smt like
                                                && !this.evidence.contains(query)
                                        )
                                        .forEach(query -> {
                                            synchronized (finalLayerVotes) {
                                                finalLayerVotes.add(query, 1);
                                            }
                                        });
                            } else {
                                Sugar.parallelStream(proveable, parallel)
                                        .filter(query -> !entry.getValue().contains(query)
                                                //// not a nice hack around
                                                && (allPredicatesAllowed || (allowedPredicate.equals(query.predicate()) && allowedArity == query.arity()))
                                                // add evidence filter here, smt like
                                                && !this.evidence.contains(query)
                                                && TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode))
                                        .forEach(query -> {
                                            double impliedSuccessors = impliedAncestorSafeSmart(entry.getKey().size(), k);
                                            synchronized (votes) {
                                                System.out.println(query + "\t" + canon(entry.getKey()) + "\t" + impliedSuccessors);
                                                votes.add(query, impliedSuccessors);
                                            }
                                            impliedLiterals.add(query);
                                        });
                            }
                        }
                        currentEvidence = null; // GC
                        if (entry.getKey().size() == k) {
                            // prune the successors, they won't be evaluated, so do not generate them
                            return null;
                        }
                        return new Triple<>(null
                                , Sugar.union(impliedLiterals, entry.getValue())
                                , baseElements.stream()
                                .map(c -> entry.getKey().union(c))
                                .filter(refinement -> refinement.size() > entry.getKey().size()
                                                && refinement.size() <= k
                                                && noClash(refinement, clashMakers)
                                        //&& isNeedable(refinement, needed) redundant, initialization is done by adding needable
                                )
                                // pripadne zkusit sem dat .collect(Collectors.toList()) kdyby byl nejaky problem s rychlosti, pameti, etc.
                        );
                    })
                    .filter(p -> null != p)
                    .map(triple -> (Triple<Subset, Set<Literal>, Stream<Subset>>) triple)
                    .collect(Collectors.toList());

            List<Subset> forbidden = Sugar.list();

            if (experimentalLastLayer && k - 1 == finalLevel) {
                System.out.println("skipping the last layer");
                queue.put(k, new MultiMap<>());
                continue;
            }

            for (Triple<Subset, Set<Literal>, Stream<Subset>> triple : candidates) {
                if (null == triple.r) { // process children
                    triple.t.forEach(successor -> {
                        if (!queue.containsKey(successor.size())) {
                            queue.put(successor.size(), new MultiMap<>());
                        }
                        queue.get(successor.size()).putAll(successor, triple.s);
                    });
                } else { // this one makes inconsistency
                    forbidden.add(triple.r);
                }
            }

            clashMakers.addAll(forbidden);
            queue.remove(level);
            if (level > max) {
                SubsetFactory.getInstance().clear(level);
            }
        }
        System.out.println("inconsistent found\t" + clashMakers.size());

        System.out.println("finalLayerVotes");
        finalLayerVotes.keySet().forEach(key -> System.out.println(key + "\t" + finalLayerVotes.get(key) + "\t" + this.evidence.contains(key)));
        System.out.println("dataDriven out");
        return votes;
    }

    private int unique(Term... terms) {
        Set<Term> set = Sugar.set();
        for (int idx = 0; idx < terms.length; idx++) {
            set.add(terms[idx]);
        }
        return set.size();
    }

    private Set<Literal> mineNeedable(Set<Clause> theory, int k, Iterable<Literal> mask) {
        //List<Literal> allConstants = Sugar.list(new Literal("", true, Sugar.listFromCollections(constants)));
        Clause evidence = new Clause(mask);
        Matching world = Matching.create(evidence, Matching.THETA_SUBSUMPTION);

        Set<Literal> violatedHeads = Sugar.set();
        for (Clause rule : theory) {
            Literal posLit = Sugar.chooseOne(LogicUtils.positiveLiterals(rule));
            if (null == posLit) {
                continue;
            }
            Pair<Term[], List<Term[]>> substitutions = world.allSubstitutions(LogicUtils.flipSigns(rule), 0, Integer.MAX_VALUE);
            for (Term[] subs : substitutions.s) {
                if (unique(subs) <= k) {
                    violatedHeads.add(LogicUtils.substitute(posLit, substitutions.r, subs));
                }
            }
        }
        return violatedHeads;
    }

    // based on cutting plaine interference
    private Set<Subset> mineNeedable(Set<Clause> theory, int k) {
        //List<Literal> allConstants = Sugar.list(new Literal("", true, Sugar.listFromCollections(constants)));
        Clause worldEvidence = new Clause(this.evidence);
        Matching world = Matching.create(worldEvidence, Matching.THETA_SUBSUMPTION);

        List<Clause> violated = Sugar.list();
        for (Clause rule : theory) {
            Pair<Term[], List<Term[]>> substitutions = world.allSubstitutions(LogicUtils.flipSigns(rule), 0, Integer.MAX_VALUE);
            for (Term[] subs : substitutions.s) {
                violated.add(LogicUtils.substitute(rule, substitutions.r, subs));
//                System.out.println(LogicUtils.substitute(rule, substitutions.r, subs));
            }
//            if(!substitutions.getS().isEmpty()){
//                System.out.println("ha\t" + rule);
//            }
        }
        return violated.stream()
                .map(clause -> constantsToSubset(LogicUtils.constants(clause)))
                .filter(subset -> subset.size() <= k)
                .collect(Collectors.toSet());
    }

    private Subset constantsToSubset(Set<Constant> cs) {
        Subset retVal = null;
        for (Constant c : cs) {
            Subset cBit = constantToSubset.get(c);
            if (null == retVal) {
                retVal = cBit;
            } else {
                retVal = retVal.union(cBit);
            }
        }
        return retVal;
    }

    private void prepareFactorialCache(int k) {
        // factorialCache can be probably thrown away, or, may be only f(0)...f(k) is enough to cache :))
        synchronized (factorialCache) {
            BigDecimal fact = BigDecimal.ONE;
            //int total = constants.size();
            // with caching it is enough to compute only f(k)
            int total = k;
            for (int idx = 1; idx <= total; idx++) {
                fact = fact.multiply(BigDecimal.valueOf(idx));
                if ((0 <= idx && idx <= k)
                        || (total - k <= idx && idx <= total)) {
                    factorialCache.put(idx, fact);
                }
            }
        }
        synchronized (cachedAncestors) {
            // cause dividing big decimal is too expensive
            for (int sizeS = 1; sizeS < k; sizeS++) {
                BigDecimal val = factorialCache.get(sizeS);
                for (int t = sizeS; t <= k - 1; t++) {
                    val = val.multiply(BigDecimal.valueOf(constants.size() - t));
                }
                cachedAncestors.put(new Pair<>(sizeS, k), val.doubleValue());
                System.out.println("put for s k\t" + sizeS + "\t" + k + "\t" + val);

            }
        }
    }

    private double impliedAncestorSafeSmart(int usedConstants, int k) {
        if (k == usedConstants) {
            return factorialCache.get(usedConstants).doubleValue();
        }
        return cachedAncestors.get(new Pair<>(usedConstants, k));
    }

    private boolean noClash(Subset subset, Collection<Subset> forbidden) {
        for (Subset clashMaker : forbidden) {
            if (clashMaker.isSubsetOf(subset)) {
                return false;
            }
        }
        return true;
    }

    private String canon(Subset s) {
        return canon(subsetToConst(s));
    }

    private String canon(Set<? extends Object> constants) {
        return constants.stream()
                //.map(Constant::name)
                .map(Object::toString)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    // returns set of subsets which are minimal in the sense that they cannot be splitted to multiple subsets, each one of them having non empty mask
    private Set<Subset> baseSubsets(Map<Integer, MultiMap<Subset, Literal>> subsetToLiterals, int k) {
        Set<Subset> minimal = Sugar.set();
        int max = subsetToLiterals.keySet().stream().mapToInt(i -> i).max().orElse(0);
        for (int i = 0; i <= Math.min(max, k); i++) {
            if (!subsetToLiterals.containsKey(i) || null == subsetToLiterals.get(i)) {
                continue;
            }
            List<Subset> layerMinimal = Sugar.list();
            for (Subset s : subsetToLiterals.get(i).keySet()) {
                if (!canBeComposed(s, minimal)) {
                    layerMinimal.add(s);
                }
            }
            minimal.addAll(layerMinimal);
        }
        return minimal;
    }

    private boolean canBeComposed(Subset s, Set<Subset> minimals) {
        Subset union = null;
        for (Subset minimal : minimals) {
            if (s.isSubsetOf(minimal)) {
                if (null == union) {
                    union = minimal;
                } else {
                    union = union.union(minimal);
                }
            }
        }
        if (null == union) {
            return false;
        }
        return s.size() == union.size();
    }

    private Set<Literal> myMaskEvidence(Set<Constant> c) {
        return evidence.stream()
                .filter(literal -> c.containsAll(LogicUtils.constantsFromLiteral(literal)))
                .collect(Collectors.toSet());
    }

    private Set<Literal> atLeastOne(Set<Constant> c) {
        return evidence.stream()
                .filter(literal -> LogicUtils.constantsFromLiteral(literal).stream().anyMatch(ec -> c.contains(ec)))
                .collect(Collectors.toSet());
    }

    // jenze minimals nedavaji smysl
    private MultiMap<Integer, Subset> minimalEmpty(Set<Subset> allConst, int k) {
        // chtel jsem aby to vracelo minimalni mnoziny konstant takovy, ze nemaji zadny prunik literalu, napr
        // nemusi nutne pomahat na vsech domenach
        MultiMap<Integer, Subset> map = new MultiMap<>();
        Map<Subset, Boolean> cache = new HashMap<>();
        allConst.forEach(c -> minimals(c, cache, k, allConst, map));

        // leaving minimals only
        int max = map.keySet().stream().mapToInt(i -> i).max().orElse(0);
        for (int idx = max - 1; idx > 0; idx--) {
//            System.out.println(idx+ "\toriginal size\t" + map.get(idx+1).size());
            Set<Subset> successors = map.get(idx + 1);
            Set<Subset> parents = map.get(idx);
//            System.out.println(map.get(idx+1));

            List<Subset> toRemove = successors.stream()
                    .filter(succ -> parents.stream().anyMatch(parent -> parent.isSubsetOf(succ)))
                    .collect(Collectors.toList());
            map.get(idx + 1).removeAll(toRemove);
//            System.out.println("to remove\t" + toRemove.size());
//            System.out.println("after size\t" + map.get(idx+1).size());
        }

        return map;
    }

    // jenze minimals nedavaji smysl
    private boolean minimals(Subset candidate, Map<Subset, Boolean> cache, int k, Set<Subset> allConst, MultiMap<Integer, Subset> aggregator) {
        // dynamical programming
        if (cache.containsKey(candidate)) {
            return cache.get(candidate);
        }

        if (candidate.isNonEmpty()) {
            // evaluate part
            Set<Literal> literals = maskEvidence(candidate);
            if (!literals.isEmpty()) {
                cache.put(candidate, false);
                return false;
            }
        }

        // exploit part
        boolean allSuccessorsEmpty = true;
        if (candidate.size() < k) {
            for (Subset c : allConst) {
                Subset refinement = candidate.union(c);
                if (refinement.size() > candidate.size()) {
                    allSuccessorsEmpty &= minimals(refinement, cache, k, allConst, aggregator);
                }
            }
        }
        if (allSuccessorsEmpty) {
            aggregator.put(candidate.size(), candidate);
        }
        cache.put(candidate, allSuccessorsEmpty);
        return allSuccessorsEmpty;
    }

    private Set<Constant> subsetToConst(Subset candidate) {
        return subsetToConstant.entrySet().stream()
                .filter(entry -> entry.getKey().isSubsetOf(candidate))
                .map(entry -> entry.getValue())
                .collect(Collectors.toSet());
    }

    // brute force
    public int votingEntailment(Set<Clause> theory, int k, Literal literal) {
        int votes = 0;
        Iterator<Set<Literal>> subsets = kEvidenceGeneratorPrune(k, Sugar.list(literal));
        while (subsets.hasNext()) {
            Set<Literal> subset = subsets.next();
//            System.out.println("testing:\t" + LogicUtils.constantsFromLiterals(subset) + "\t" + literal);
            if (kEntails(theory, subset, literal)) {
//                System.out.println("subste of entails:\t" + LogicUtils.constantsFromLiterals(subset) + "\t" + literal);
                votes++;
            }
        }
        return votes;
    }

    // set of literals as evidence instead of cPrime, so the empty mask of cPrime can be prune in advance
    private boolean kEntails(Set<Clause> theory, Set<Literal> evidence, Literal query) {
        TheorySolver solver = new TheorySolver();
        solver.setSubsumptionMode(theorySolverSubsumption);
        solver.setMode(theorySolverMode);
        MultiMap<Constant, Literal> constantsToLiters = new MultiMap<>();
        // change to one line stream
        evidence.forEach(literal -> LogicUtils.constantsFromLiteral(literal).forEach(constant -> constantsToLiters.put(constant, literal)));

        Set<Constant> cPrime = evidence.stream().flatMap(literal -> LogicUtils.constantsFromLiteral(literal).stream()).collect(Collectors.toSet());
        int k = cPrime.size();

        Set<Constant> startingSubset = LogicUtils.constantsFromLiteral(query);
        Set<Literal> subsetEvidence = maskEvidence(startingSubset, constantsToLiters);

        // awful recopy of the task, but the try to solve the statefulness in another way, without generate-queue-store-then-evaluate principle
        if (!subsetEvidence.isEmpty()) {
            Set<Clause> merged = Sugar.setFromCollections(theory, subsetEvidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
            Set<Literal> proveable = solver.solve(merged);
            if (null == proveable) {
                return false;
            } else if (proveable.contains(query) && TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode)) {
//                System.out.println("returning true because of\t" + startingSubset);
                return true;
            }
        }

        Set<Set<Constant>> queue = Sugar.set(startingSubset);

        while (!queue.isEmpty()) {
            Set<Set<Constant>> closed = Sugar.set();
            Set<Set<Constant>> next = Sugar.set();
            for (Set<Constant> subset : queue) {
                for (Constant cr : cPrime) {
                    if (subset.contains(cr)) {
                        continue;
                    }
                    Set<Constant> refinement = Sugar.union(subset, cr);
                    if (closed.contains(refinement)) {
                        continue;
                    }
                    closed.add(refinement);
                    Set<Literal> refinementEvidence = maskEvidence(refinement, constantsToLiters);
                    if (!refinementEvidence.isEmpty()) {
                        Set<Clause> merged = Sugar.setFromCollections(theory, refinementEvidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
                        Set<Literal> proveable = solver.solve(merged);
                        if (null != proveable) {
                            // do nothing, kill this refinement
                        } else if (proveable.contains(query) && TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode)) {
//                            System.out.println("returning true because of2\t" + refinement);
                            return true;
                        } else if (refinement.size() < k) { // doesn't k'-entail nor causes inconsistency
                            next.add(refinement);
                        }
                    }
                }
            }
            queue = next;
        }
        return false;
    }

    private Set<Literal> maskEvidence(Subset mask) {
//        Set<Literal> retVal = Sugar.set();
//        for (Map.Entry<Integer, MultiMap<Subset, Literal>> layer : subsetToLiterals.entrySet()) {
//            layer.getValue().entrySet().stream()
//                    .filter(entry -> entry.getKey().isSubsetOf(mask))
//                    .forEach(entry -> entry.getValue().forEach(retVal::add));
//        }
//        return retVal;
        return Sugar.parallelStream(subsetToLiterals.entrySet(), parallel)
                .flatMap(layer -> layer.getValue().entrySet().stream())
                .filter(entry -> entry.getKey().isSubsetOf(mask))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
    }

    private Set<Literal> maskEvidence(Set<Constant> mask, MultiMap<Constant, Literal> constantsToLiters) {
        return constants.stream().flatMap(constant -> constantsToLiters.get(constant).stream())
                .distinct()
                .filter(literal -> mask.containsAll(LogicUtils.constantsFromLiteral(literal)))
                .collect(Collectors.toSet());
    }

    private Set<Literal> maskEvidence(Collection<Constant> constants) {
        return maskEvidence(Sugar.setFromCollections(constants), constantToLiterals);
    }

    // asi nejaky bruteforce
    public Counters<Literal> votingEntailment(Set<Clause> theory, int k) {
        Counters<Literal> counter = new Counters<>();
        Sugar.parallelStream(queries, parallel)
                .forEach(literal -> counter.add(literal, votingEntailment(theory, k, literal)));
        return counter;
    }


    // bylo by pekne kdyby to vratilo i ROC krivku a navic jeste k tomu by to melo vratit neco jako |C|^(k-a)?
    public Counters<Literal> testWithKOnlyEntailment(Set<Clause> theory, int k) {
        Counters<Literal> counter = new Counters<>(queries);
        queries.forEach(counter::decrement);

        TheorySolver solver = new TheorySolver();
        solver.setSubsumptionMode(theorySolverSubsumption);
        solver.setMode(theorySolverMode);
        if (false) { //  kEvidence version
            kEvidence(k)
                    .map(subset -> subset.map(l -> new Clause(l)).collect(Collectors.toSet()))
                    .filter(subset -> !subset.isEmpty())
                    .forEach(subset -> {
                        System.out.println("subste of\t" + subset);
                        Set<Clause> merged = Sugar.setFromCollections(theory, subset);
                        Set<Literal> solved = solver.solve(merged);
                        if (null != solved && !solved.isEmpty()) { // \Phi \cup {\kappa(\Upsilon)<C'>} is consistent
                            Sugar.parallelStream(solved.stream(), parallel)
                                    .filter(queries::contains) // only literals from queries are computed
                                    .filter(literal -> TheorySimplifier.isGroundLiteralImplied(literal, merged, theorySimplifierSubsumption, theorySolverMode))
                                    .forEach(counter::increment); // it's oki to run in parallel even this, since there is a set of literals produced ;)
                        }
                    });
        }
        if (true) {
            Iterator<Set<Literal>> iterator = kEvidenceGeneratorPrune(k, queries);
            while (iterator.hasNext()) {
                Set<Literal> subset = iterator.next();
                Set<Clause> merged = Sugar.setFromCollections(theory, subset.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
                Set<Literal> solved = solver.solve(merged);
                if (null != solved && !solved.isEmpty()) { // \Phi \cup {\kappa(\Upsilon)<C'>} is consistent
                    Sugar.parallelStream(solved.stream(), parallel)
                            .filter(queries::contains) // only literals from queries are computed
                            .filter(literal -> TheorySimplifier.isGroundLiteralImplied(literal, merged, theorySimplifierSubsumption, theorySolverMode))
                            .forEach(counter::increment); // it's oki to run in parallel even this, since there is a set of literals produced ;)
                }
            }
        }
        return counter;
    }

    // returns non-empty evidence of k-size subsets
    // prochazi se pouze takovy podprostor kombinaci, jehoz prvky vzdy obsahuji alespon jednu mnozinu konstant z nejakeho target literalu/query (to by asi slo jeste nejak zrychlit)
    private Iterator<Set<Literal>> kEvidenceGeneratorPrune(int k, Collection<Literal> targetQueries) {
        Set<Set<Constant>> needed = targetQueries.stream()
                .map(literal -> literal.argumentsStream().map(t -> (Constant) t).collect(Collectors.toSet()))
                .collect(Collectors.toSet());
        Set<Constant> sure = needed.stream().flatMap(Set::stream).collect(Collectors.toSet());
        int minSure = sure.size();
        List<Constant> elements = Sugar.list();
        elements.addAll(sure);
        constants.stream().filter(c -> !sure.contains(c)).forEach(elements::add);

        if (k > constants.size()) {
            throw new IllegalArgumentException("k cannot be bigger than number of constants");
        }

        return new Iterator<Set<Literal>>() {
            int[] currentState = null;
            int[] nextState = null;
            Set<Literal> nextOutput = null;

            private Set<Constant> idxsToConst(int[] state) {
                return Arrays.stream(state).mapToObj(elements::get).collect(Collectors.toSet());
            }

            // just generate successor k-subset
            private int[] generateSuccessor(int[] state, boolean lastNonNeeded) {
                int idx = k - 1;
                for (; idx >= -1; idx--) {
                    if (idx < 0) {
                        return null; //  we are at the end
                    }
                    if (lastNonNeeded && (state[idx] < minSure && state[idx] + k - idx < elements.size())) {
                        break;
                    } else if (!lastNonNeeded && (state[idx] + k - idx < elements.size())) {
                        break;
                    }
                }
                int[] succ = new int[k];
                for (int sucIdx = 0; sucIdx < k; sucIdx++) {
                    if (sucIdx < idx) {
                        succ[sucIdx] = state[sucIdx];
                    } else {
                        succ[sucIdx] = state[idx] + (sucIdx - idx) + 1;
                    }
                }
                return succ;
            }


            // returns false iff there no constants' subset in the state that would appear in any of the queries' constants sets
            private boolean constantsSubsetCheck(int[] state) {
                if (state[0] >= minSure) {
                    return false;
                }
                Set<Constant> stateConstants = idxsToConst(state);
                return needed.stream().anyMatch(stateConstants::containsAll);
            }

            private Set<Literal> prepareOutput(int[] state) {
                Set<Constant> stateConstants = idxsToConst(state);
                return stateConstants.stream().flatMap(constant -> constantToLiterals.get(constant).stream())
                        .distinct()
                        .filter(literal -> stateConstants.containsAll(LogicUtils.constantsFromLiteral(literal)))
                        .collect(Collectors.toSet());
            }

            @Override
            public boolean hasNext() {
                int[] next;
                if (null == currentState) {
                    next = IntStream.range(0, k).toArray();
                } else {
                    next = generateSuccessor(currentState, false);
                }

                while (null != next) {
                    if (constantsSubsetCheck(next)) {
                        nextOutput = prepareOutput(next);
                        if (!nextOutput.isEmpty()) {
                            break;
                        }
                        next = generateSuccessor(next, false);
                    } else {
                        next = generateSuccessor(next, true);
                    }
                }

                if (null == next) {
                    return false;
                }
                nextState = next;

                return true;
            }

            @Override
            public Set<Literal> next() {
                if (null == nextState || null == nextOutput) {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                }
                currentState = nextState;
                Set<Literal> cache = nextOutput;
                nextState = null;
                nextOutput = null;
                return cache;
            }
        };
    }


    // returns non-empty evidence of k-size subsets
    private Iterator<Set<Literal>> kEvidenceGenerator(int k) {
        Set<Set<Constant>> needed = queries.stream()
                .map(literal -> literal.argumentsStream().map(t -> (Constant) t).collect(Collectors.toSet()))
                .collect(Collectors.toSet());
        Set<Constant> sure = needed.stream().flatMap(Set::stream).collect(Collectors.toSet());
        int minSure = sure.size();
        List<Constant> elements = Sugar.list();
        elements.addAll(sure);
        constants.stream().filter(c -> !sure.contains(c)).forEach(elements::add);

        if (k > constants.size()) {
            throw new IllegalArgumentException("k cannot be bigger than number of constants");
        }

        return new Iterator<Set<Literal>>() {
            int[] currentState = null;
            int[] nextState = null;
            Set<Literal> nextOutput = null;

            private Set<Constant> idxsToConst(int[] state) {
                return Arrays.stream(state).mapToObj(elements::get).collect(Collectors.toSet());
            }

            // just generate successor k-subset
            private int[] generateSuccessor(int[] state) {
                int idx = k - 1;
                for (; idx >= -1; idx--) {
                    if (idx < 0) {
                        return null; //  we are at the end
                    }
                    if (state[idx] + k - idx < elements.size()) {
                        break;
                    }
                }
                int[] succ = new int[k];
                for (int sucIdx = 0; sucIdx < k; sucIdx++) {
                    if (sucIdx < idx) {
                        succ[sucIdx] = state[sucIdx];
                    } else {
                        succ[sucIdx] = state[idx] + (sucIdx - idx) + 1;
                    }
                }
                return succ;
            }


            // returns false iff there no constants' subset in the state that would appear in any of the queries' constants sets
            private boolean constantsSubsetCheck(int[] state) {
                if (state[0] >= minSure) {
                    return false;
                }
                Set<Constant> stateConstants = idxsToConst(state);
                return needed.stream().anyMatch(stateConstants::containsAll);
            }

            private Set<Literal> prepareOutput(int[] state) {
                Set<Constant> stateConstants = idxsToConst(state);
                return stateConstants.stream().flatMap(constant -> constantToLiterals.get(constant).stream())
                        .distinct()
                        .filter(literal -> stateConstants.containsAll(LogicUtils.constantsFromLiteral(literal)))
                        .collect(Collectors.toSet());
            }

            @Override
            public boolean hasNext() {
                int[] next;
                if (null == currentState) {
                    next = IntStream.range(0, k).toArray();
                } else {
                    next = generateSuccessor(currentState);
                }

                while (null != next) {
                    if (constantsSubsetCheck(next)) {
                        nextOutput = prepareOutput(next);
                        if (!nextOutput.isEmpty()) {
                            break;
                        }
                    }
                    next = generateSuccessor(next);
                }

                if (null == next) {
                    return false;
                }
                nextState = next;

                return true;
            }

            @Override
            public Set<Literal> next() {
                if (null == nextState || null == nextOutput) {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                }
                currentState = nextState;
                Set<Literal> cache = nextOutput;
                nextState = null;
                nextOutput = null;
                return cache;
            }
        };
    }

    // jeste promyslet jestli nejake prorezavani substeu na zaklade toho ze tam musi byt alespon nejake konstanty ktere jsou
    // z queries
    // returns literals of particular k-size constants' subset, all of possible combinations are tested
    private Stream<Stream<Literal>> kEvidence(int k) {
        // tady probehne k-subset generace a nasledne z toho vybrani literalu, aby se mohlo prorezavat uz tady a optimalizovat :))
        // if facts-subset empty, return empty stream
        // space left for parallel version

        // brute force, space for heuristics
        return Combinatorics.subset(Sugar.listFromCollections(constants), k)
                .stream()
                .map(subset -> subset.stream()
                        .flatMap(constant -> constantToLiterals.get(constant).stream())
                        .distinct()
                        .filter(literal -> subset.containsAll(LogicUtils.constantsFromLiteral(literal)))
                );
    }

    public static TestComputing create(Set<Literal> evidence, Set<Literal> queries, boolean constantVariableNotation) {
        if (!constantVariableNotation) { // mlns notation
            return create(evidence.stream().map(LogicUtils::constantize).collect(Collectors.toSet()),
                    queries.stream().map(LogicUtils::constantize).collect(Collectors.toSet()));
        }
        return create(evidence, queries);
    }

    public static TestComputing create(Set<Literal> evidence, Set<Literal> queries) {
        Clause all = new Clause(Sugar.setFromCollections(evidence, queries));
        if (!LogicUtils.variables(all).isEmpty()) {
            throw new IllegalArgumentException("both evidence and queries are supposed to be ground");
        }
        Set<Constant> constants = LogicUtils.constants(all);
        MultiMap<Constant, Literal> constantsToLiterals = new MultiMap<>();
        // change to one line stream
        evidence.forEach(literal -> LogicUtils.constantsFromLiteral(literal).forEach(constant -> constantsToLiterals.put(constant, literal)));
        Quadruple<Set<Subset>, Map<Integer, MultiMap<Subset, Literal>>, Map<Subset, Constant>, Map<Constant, Subset>> bitsetRepre = createBitsetRepresentation(evidence);
        return new TestComputing(evidence, queries, constants, constantsToLiterals, bitsetRepre);
    }

    private static Quadruple<Set<Subset>, Map<Integer, MultiMap<Subset, Literal>>, Map<Subset, Constant>, Map<Constant, Subset>> createBitsetRepresentation(Set<Literal> evidence) {
        // we could add constants from queries as well, but they must appear in evidence in order to be entailed
        List<Constant> allConstants = LogicUtils.constants(new Clause(evidence)).stream()
                .sorted(Comparator.comparing(Constant::name))
                .collect(Collectors.toList());

        Map<Subset, Constant> subsetToConstant = new HashMap<>();
        Map<Constant, Subset> constantToSubset = new HashMap<>();
        for (int idx = 0; idx < allConstants.size(); idx++) {
            Subset subset = SubsetFactory.getInstance().get(allConstants.size(), idx);
            Constant constant = allConstants.get(idx);
            subsetToConstant.put(subset, constant);
            constantToSubset.put(constant, subset);
        }

        Map<Integer, MultiMap<Subset, Literal>> subsetToLiterals = new HashMap<>();
        evidence.stream()
                .filter(literal -> literal.arity() > 0) // redundant literal
                .forEach(literal -> {
                    Subset subset = literal.argumentsStream().map(t -> {
                        Constant constant = (Constant) t;
                        return constantToSubset.get(constant);
                    }).reduce(Subset::union).orElse(null);
                    if (null == subset) { // jst a check
                        throw new IllegalStateException();
                    }
                    if (!subsetToLiterals.containsKey(subset.size())) {
                        subsetToLiterals.put(subset.size(), new MultiMap<>());
                    }
                    subsetToLiterals.get(subset.size()).put(subset, literal);
                });

        return new Quadruple(subsetToConstant.keySet(), subsetToLiterals, subsetToConstant, constantToSubset);
    }

    public static TestComputing create(Path evidenceFile, Path queriesFile, boolean pseudoPrologNotation) {
        Set<Literal> evidence = null;
        Set<Literal> queries = null;
        try {
            evidence = Files.readAllLines(evidenceFile)
                    .stream()
                    .flatMap(line -> Clause.parse(line).literals().stream())
                    .collect(Collectors.toSet());

            queries = Files.readAllLines(queriesFile)
                    .stream()
                    .flatMap(line -> Clause.parse(line).literals().stream())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null == evidence || null == queries) {
            throw new IllegalArgumentException("either evidence or queries file cannot be read");
        }
        return create(evidence, queries, pseudoPrologNotation);
    }

}
