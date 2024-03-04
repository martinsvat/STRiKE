/*
 * Copyright (c) 2015 Ondrej Kuzelka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package logicStuff.learning.constraints.shortConstraintLearner;

import ida.ilp.logic.*;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Combinatorics;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.utils.collections.Counters;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.theories.TheorySimplifier;
import logicStuff.learning.languageBias.NoneBias;
import logicStuff.learning.languageBias.LanguageBias;
import logicStuff.learning.saturation.ConjunctureSaturator;
import logicStuff.learning.saturation.IdentitySaturator;
import logicStuff.learning.saturation.Saturator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;


/**
 * Created by ondrejkuzelka on 01/02/17.
 * <p>
 * Version with caching and pruning hacks, which should be faster than the original (MS). All samples are tested on theta-subsumption, tested clause is cached (so multiple testing of a clause should not occur), and being minimal is computed by querying cached parents.
 * <p>
 * big TODO make the learnConstraint methods nicer !!!
 */
public class UltraShortConstraintLearnerFaster {

    {
        System.out.println("TODO: kdyz se pousti DTL se saturacemi tak pocitat saturaci saturace s novou DT at se usetri neco ;))");
    }

    public static final int ONLY_POSITIVE_SATURATIONS = -1;
    public static final int ONLY_NEGATIVE_SATURATIONS = -2;
    public static final int BOTH_SATURATIONS = -3;
    public static final int NONE = -4;
    private final LanguageBias bias;
    // else, just use negative saturations all the time and positive up to level i

    private Random random;

    private int maxVariables = 5;

    private int maxLiterals = 2;

    private final DatasetInterface dataset;

    private Set<Pair<String, Integer>> allPredicates;
    private final int maxNegLiterals;
    private final int maxPosLiterals;
    private final int maxComponents;
    private final int absoluteAllowedSubsumptions;
    private final BiConsumer<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> outputPipe;

    // debug
    // sum vsech vygenerovanych refinementu (bez isomorf checkq a # variables podminky)
    private long debugTotalRefinementGenerated = 0l;
    // sum vygenerovanych neizomorfnich refinementu v ref(clause, predicate) splnujici vsechny podminky
    private long debugTotalEQCRef = 0l;
    // suma neizomorfnich refinementy expandovanych
    private long refinementsProcessed = 0l;
    // sum vsech vracenych refinementu funkci refinement na clausuli
    private long debugRefinementsCardinality = 0l;
    // suma neizomorfnich refinementy
    private long debugOverAllEQref = 0l;
    private long killedBySaturations = 0l;
    private long isoPrune = 0l;
    private boolean useBad = true;

    private final boolean parallel = 1 < Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1"))
            || Boolean.parseBoolean(System.getProperty("ida.searchPruning.parallel", "false"));

    private boolean verbose = Boolean.parseBoolean(System.getProperty("ida.logicStuff.UltraShortConstraintLearnerFaster.verbose", "true"));

    public UltraShortConstraintLearnerFaster(DatasetInterface dataset, int maxLiterals, int maxVariables, int absoluteAllowedSubsumptions, int maxComponents, int maxPosLiterals, int maxNegLiterals, BiConsumer<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> outputPipe, LanguageBias bias) {
        this.dataset = dataset;
        // language bias
        this.allPredicates = this.dataset.allPredicates();
        this.maxLiterals = maxLiterals;
        this.maxVariables = maxVariables;
        this.maxComponents = maxComponents;
        this.maxPosLiterals = maxPosLiterals;
        this.maxNegLiterals = maxNegLiterals;
        this.absoluteAllowedSubsumptions = absoluteAllowedSubsumptions;
        this.outputPipe = outputPipe;
        this.random = new Random();
        this.bias = bias;
    }

    public Pair<List<Clause>, List<Clause>> learnConstraints(boolean recomputingSaturations, boolean computeStatistic) {
        return learnConstraints(true, recomputingSaturations, computeStatistic, Integer.MAX_VALUE, Sugar.list(), false, BOTH_SATURATIONS, false);
    }

    public Pair<List<Clause>, List<Clause>> learnConstraints(boolean useSaturations, boolean recomputingSaturations, boolean computeStatistic, int subsamplingSize,
                                                             Collection<Predicate> allowedSaturatedPredicates, boolean useTheorySimplifier,
                                                             int saturationMode, boolean filterByLearnedTheoryAfterLevel) {
        return learnConstraints(useSaturations, recomputingSaturations, computeStatistic, subsamplingSize, allowedSaturatedPredicates, useTheorySimplifier, saturationMode, filterByLearnedTheoryAfterLevel, Sugar.set(), 1);
    }


    public Pair<List<Clause>, List<Clause>> learnConstraints(boolean useSaturations, boolean recomputingSaturations, boolean computeStatistic, int subsamplingSize,
                                                             Collection<Predicate> allowedSaturatedPredicates, boolean useTheorySimplifier,
                                                             int saturationMode, boolean filterByLearnedTheoryAfterLevel, Set<IsoClauseWrapper> initialTheory,
                                                             int minSupport, TimeDog time) {
        return learnConstraints(useSaturations,
                recomputingSaturations, computeStatistic, subsamplingSize, allowedSaturatedPredicates, useTheorySimplifier,
                saturationMode, filterByLearnedTheoryAfterLevel, initialTheory, Sugar.set(), minSupport, time);
    }

    public Pair<List<Clause>, List<Clause>> learnConstraints(boolean useSaturations, boolean recomputingSaturations, boolean computeStatistic, int subsamplingSize,
                                                             Collection<Predicate> allowedSaturatedPredicates, boolean useTheorySimplifier,
                                                             int saturationMode, boolean filterByLearnedTheoryAfterLevel, Set<IsoClauseWrapper> initialTheory,
                                                             int minSupport) {
        return learnConstraints(useSaturations, recomputingSaturations, computeStatistic, subsamplingSize,
                allowedSaturatedPredicates, useTheorySimplifier,
                saturationMode, filterByLearnedTheoryAfterLevel, initialTheory,
                minSupport, new TimeDog(0, true));
    }

    public Pair<List<Clause>, List<Clause>> learnConstraints(boolean useSaturations, boolean recomputingSaturations, boolean computeStatistic, int subsamplingSize,
                                                             Collection<Predicate> allowedSaturatedPredicates, boolean useTheorySimplifier,
                                                             int saturationMode, boolean filterByLearnedTheoryAfterLevel, Set<IsoClauseWrapper> initialTheory, Set<IsoClauseWrapper> initialMinSuppConstraints,
                                                             int minSupport) {
        return learnConstraints(useSaturations, recomputingSaturations, computeStatistic, subsamplingSize,
                allowedSaturatedPredicates, useTheorySimplifier,
                saturationMode, filterByLearnedTheoryAfterLevel, initialTheory, initialMinSuppConstraints,
                minSupport, new TimeDog(0, true));
    }


    /**
     * The learned constraints are assumed to have an implicit alldiff on all variables but this alldiff is not added
     * explicitly! To correctly use this type of rules with the TheorySolver, use theorySolver.setSubsumptionMode(Matching.OI_SUBSUMPTION).
     * <p>
     * returns pair composed of domain constraints (for saturation) and constraints with minsupport (only for pruning)
     *
     * @return
     */
    public Pair<List<Clause>, List<Clause>> learnConstraints(boolean useSaturations, boolean recomputingSaturations, boolean computeStatistic, int subsamplingSize,
                                                             Collection<Predicate> allowedSaturatedPredicates, boolean useTheorySimplifier,
                                                             int saturationMode, boolean filterByLearnedTheoryAfterLevel, Set<IsoClauseWrapper> initialTheory, Set<IsoClauseWrapper> initialMinSuppConstraints,
                                                             int minSupport, TimeDog time) {
        if (null == time) {
            time = new TimeDog(0, true);
        }
        if (this.verbose) {
            System.out.println("ultraSCL setting\n" +
                    "\tuseSaturations\t" + useSaturations +
                    "\n\trecomputingSaturations\t" + recomputingSaturations +
                    "\n\tcomputeStatistic\t" + computeStatistic +
                    "\n\tsubsamplingSize\t" + subsamplingSize +
                    "\n\tallowedSaturatedPredicates\t" + allowedSaturatedPredicates +
                    "\n\tuseTheorySimplifier\t" + useTheorySimplifier +
                    "\n\tmaxComponents\t" + maxComponents +
                    "\n\tvariables\t" + this.maxVariables +
                    "\n\tliterals\t" + this.maxLiterals +
                    "\n\tmax pos literals\t" + maxPosLiterals +
                    "\n\tmax neg literals\t" + maxNegLiterals +
                    "\n\tonly negative saturations (mode)\t" + saturationMode +
                    "\n\tfilterByLearnedTheoryAfterLevel\t" + filterByLearnedTheoryAfterLevel +
                    "\n\tinitialTheory\t" + initialTheory.size() +
                    "\n\tminSupport\t" + minSupport);
        }

        java.util.function.Function<Integer, Pair<Boolean, Boolean>> whichSaturations = (currentLevel) -> {
            switch (saturationMode) {
                case ONLY_NEGATIVE_SATURATIONS:
                    return new Pair<>(true, false);
                case ONLY_POSITIVE_SATURATIONS:
                    return new Pair<>(false, true);
                case BOTH_SATURATIONS:
                    return new Pair<>(true, true);
                case NONE:
                    return new Pair<>(false, false);
                default:
                    break;
            }
            return new Pair<>(true, currentLevel <= saturationMode); // in this case, saturation mode means up to which level negative saturations are computed (positive saturations are computed all the way around ;)
        };

        // recomputingSaturations -- if true, then space of non-saturated is searched,.... but in current setting, the set of saturated
        // computeStatistic -- iff false, than safe memory
        // subsamplingSize -- max absolute value of expanded nodes in a layer

        //debug
        long debugRefinementsCount = 0;
        long debugStart = System.nanoTime();

        Map<IsoClauseWrapper, Coverage> cache = new HashMap<>();

        Set<IsoClauseWrapper> hardConstraints = Sugar.setFromCollections(initialTheory);
        Set<IsoClauseWrapper> minSuppConstraints = Sugar.setFromCollections(initialMinSuppConstraints);
        Map<IsoClauseWrapper, Double> softConstraints = new HashMap<>();
        Set<IsoClauseWrapper> beam = new HashSet<>();
        IsoClauseWrapper empty = new IsoClauseWrapper(new Clause());
        beam.add(empty);
        cache.put(empty, CoverageFactory.getInstance().get(dataset.size()));
        Counters<IsoClauseWrapper> statistics = new Counters<>();
        MultiMap<IsoClauseWrapper, Literal> badRefinements = new MultiMap<>(MultiMap.CONCURENT_HASH_SET);
        Set<IsoClauseWrapper> parentsCache = Sugar.set();
        parentsCache.add(empty);

        String statsResult = "";

        boolean minSupportPrune = 1 != minSupport;

        for (int currentLevel = 1; currentLevel <= maxLiterals; currentLevel++) {
            MultiMap<Integer, Clause> processed = new MultiMap<>();
            Map<IsoClauseWrapper, Coverage> newCache = new HashMap<>();
            Set<IsoClauseWrapper> newBeam = new HashSet<>();
            long opened = 0;// debug

            List<Clause> simplifiedConstraints = null;
            if (useTheorySimplifier) { // add caching for speed-up
                if (verbose) {
                    System.out.println("constraints simplification\tpridat cachovani");
                }
                int maxVars = hardConstraints.stream().mapToInt(icw -> icw.getOriginalClause().variables().size()).max().orElse(0);
                simplifiedConstraints = TheorySimplifier.simplify(hardConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()), maxVars + 1);
            } else {
                simplifiedConstraints = hardConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList());
            }

            Saturator<Clause> saturator = (useSaturations) ? ConjunctureSaturator.create(simplifiedConstraints, whichSaturations.apply(currentLevel), allowedSaturatedPredicates) : IdentitySaturator.create();
            Saturator<Clause> filter = (useSaturations)
                    ? ConjunctureSaturator.create(Sugar.listFromCollections(simplifiedConstraints, minSuppConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList())))
                    : IdentitySaturator.create();

            Set<IsoClauseWrapper> isoProcessed = ConcurrentHashMap.newKeySet();
            Set<IsoClauseWrapper> isoSaturated = ConcurrentHashMap.newKeySet();
            // tohle jeste vyzkouset, u samplovaci verze by bad refinements mohly byt
            MultiMap<IsoClauseWrapper, Literal> newBadRefinements = (currentLevel <= 2) ? new MultiMap<>(MultiMap.CONCURENT_HASH_SET) : null;

            for (IsoClauseWrapper parent : subsample(beam, subsamplingSize)) {
                opened++; // debug

                if (!time.enough()) {
                    break;
                }

                List<Refinement> refinements = refinements(parent.getOriginalClause(), saturator, isoProcessed, (null == badRefinements) ? Sugar.set() : badRefinements.get(parent), newBadRefinements, filter);
                for (Refinement refinement : refinements) {
//                    System.out.println(refinement.getNonSaturated());

                    if (!time.enough()) {
                        break;
                    }

                    Clause candidate = refinement.getNonSaturated();
                    IsoClauseWrapper canonicCandidate = refinement.getNonSaturatedICW();
                    /*if(useSaturations && !recomputingSaturations){ this should use space of saturated hypotheses but didn't work for the first time; let's fix it
                        candidate = refinement.getSaturated();
                        canonicCandidate = refinement.getSaturatedICW();
                    }*/

                    debugRefinementsCount++;

                    if (this.verbose && debugRefinementsCount % 1000 == 0) {
                        runningStats(debugRefinementsCount, debugStart, hardConstraints, beam, opened, softConstraints, computeStatistic, statistics, minSuppConstraints);
                    }

                    if (useSaturations &&
                            ((!recomputingSaturations && !isoSaturated.add(refinement.getSaturatedICW()))
                                    || (recomputingSaturations && inProcessed(refinement, saturator, processed)))) {
                        if (computeStatistic) { // slows down the computation a little bit
                            throw new IllegalStateException();//NotImplementedException();
                            //statistics.addAll(refinement.addedLiteralsStats());
                        }
                        continue;
                    }

                    this.debugOverAllEQref++;

                    Pair<Boolean, Coverage> pair = matchesAny(candidate, parent, cache);
                    boolean matchesCondition = !pair.r;

                    // cannot be merged with the lower part
                    // candidate is a conjunction
                    if (!matchesCondition && useSaturations && minSupportPrune && pair.s.size() < minSupport && LogicUtils.negativeLiteralsCount(candidate) < 1) {
                        Clause minimalCandidateConjunction = minimizeCandidate(candidate, minSupport);
                        minSuppConstraints.add(IsoClauseWrapper.create(LogicUtils.flipSigns(minimalCandidateConjunction)));
                        continue; // just skip the lower part in minsupp>1 case
                    }

                    if (matchesCondition) {
                        if (useSaturations) {
                            if (checkMinimal(candidate, subsamplingSize, minSupportPrune)) {
                                hardConstraints.add(IsoClauseWrapper.create(LogicUtils.flipSigns(candidate)));
                            }
                        } else {
                            if (allParentsInCache(candidate, parentsCache, minSupportPrune)) {
                                hardConstraints.add(new IsoClauseWrapper(LogicUtils.flipSigns(candidate)));
                            }
                        }
//                        System.out.println("this should be thrown away");
                    } else if (candidate.countLiterals() < maxLiterals) {//|| (currentLevel <= maxLiterals)) {
                        if (minSupport <= pair.s.size()) { // we can prune this candidate, either we've solve them before (so, just a check), or its children will have lower support than the threshold
                            newBeam.add(canonicCandidate);
                            newCache.put(canonicCandidate, pair.s);
                        }
                        if (pair.s.size() < absoluteAllowedSubsumptions) {
                            // appendix, throw away
                            softConstraints.put(IsoClauseWrapper.create(LogicUtils.flipSigns(candidate)), pair.s.size() / (1.0 * dataset.size()));
                        }
                    } else if (candidate.countLiterals() != maxLiterals) { // just check, should never occur (only in case of non-saturated space)
                        //System.out.println("the last layer? " + candidate.countLiterals() + " " + currentLevel + " " + maxLiterals);
                    }
                }
            }

            if (verbose) {
                runningStats(debugRefinementsCount, debugStart, hardConstraints, beam, opened, softConstraints, computeStatistic, statistics, minSuppConstraints);

                System.out.println("level " + (currentLevel - 1));
                System.out.println("constraints");
                System.out.println(String.join(",",
                        hardConstraints.stream()
                                .map(c -> "\"" + c.getOriginalClause() + "\"")
                                .collect(Collectors.toList())));
                System.out.println("end of constraints");
            }

            if (filterByLearnedTheoryAfterLevel && useSaturations) {
                // this probably won't have any real effect (only time consuming)
                int finalCurrentLevel = currentLevel;
                ConjunctureSaturator levelSaturator = ConjunctureSaturator.create(hardConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()),
                        whichSaturations.apply(finalCurrentLevel),
                        allowedSaturatedPredicates);
                List<Refinement> saturatedConstraints = Sugar.parallelStream(newBeam, this.parallel)
                        .map(icw -> {
                            Clause saturated = levelSaturator.saturate(icw.getOriginalClause());
                            //return Refinement.create(saturated, icw.getOriginalClause(), Sugar.set(), icw.getOriginalClause());
                            return Refinement.create(saturated, icw.getOriginalClause());
                        }).collect(Collectors.toList());
                Set<IsoClauseWrapper> filteredBeam = Sugar.setFromCollections(saturatedConstraints).stream().map(Refinement::getNonSaturatedICW).collect(Collectors.toSet());

                if (verbose) {
                    System.out.println("\nafter level pruned\t" + (newBeam.size() - filteredBeam.size()) + "\n");
                }
                beam = filteredBeam;
            } else {
                if (!useSaturations) {
                    parentsCache = newCache.keySet();
                }
                beam = newBeam;
            }
            cache = newCache;
            badRefinements = newBadRefinements;


            statsResult += levelStats(currentLevel, isoProcessed, isoSaturated, debugStart, hardConstraints);

            if (!time.enough()) {
                break;
            }

            if (null != this.outputPipe) {
                this.outputPipe.accept(hardConstraints, minSuppConstraints);
            }
        }


        System.out.println("\noverall stats\n----------------------------");
        System.out.println("\t\t# total refinements\t\t\t non-isomorphic\t\t\t non-isomorphic saturated\t\t\t #HC\t\t\t\t time");
        System.out.println(statsResult);
        System.out.println("\n\n");

        if (verbose) {
            System.out.println("counters");
            System.out.println("literal stats");
            statistics.keySet().stream()
                    .map(icw -> new Pair<>(icw, statistics.get(icw)))
                    .sorted((o1, o2) -> o2.s - o1.s)
                    .forEach(pair -> System.out.println("\t" + pair.r.getOriginalClause() + "\t\t" + pair.s));
            System.out.println("uscl:theory simplifier is turned-off");
        }

        //hardConstraints.forEach(System.out::println);

        return new Pair<>(hardConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()),
                minSuppConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        //return NonredundantClauses.getDefault().nonredundantTheory(hardConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));

        /* // uncomment if you want to obtain greedily simplified theory
        int numVars = 0;
        Set<Clause> retVal = new HashSet<Clause>();
        for (IsoClauseWrapper icw : constraints) {
            retVal.add(icw.getOriginalClause());
            numVars = Math.max(numVars, icw.getOriginalClause().variables().size());
        }
        //System.out.println("theory size before simplification\t" + retVal.size());
        List<Clause> theory = TheorySimplifier.simplify(retVal, numVars + 1);
        //System.out.println("theory size after simplification\t" + theory.size());
        return theory;
        //return Sugar.listFromCollections(retVal);
        */
    }

    private String levelStats(int currentLevel, Set<IsoClauseWrapper> isoProcessed, Set<IsoClauseWrapper> isoSaturated, long debugStart, Collection<IsoClauseWrapper> hardConstraints) {
        java.util.function.Function<Long, String> format = (number) -> String.format("% 9d", number);

        return "\nlevel\t\t" + currentLevel
                + format.apply(this.debugRefinementsCardinality) + "\t"
                + format.apply((long) isoProcessed.size()) + "\t"
                + format.apply((long) isoSaturated.size()) + "\t"
                + format.apply((long) hardConstraints.size()) + "\t"
                + "\t\t\t\t" + ((System.nanoTime() - debugStart) / (60 * 1000000000.0))
                ;
    }

    /**
     * returns greedily minimized conjunction of the given candidate wrt to min support
     * it supposes that support of the given candidate is below min support
     *
     * @param candidate  (conjunction)
     * @param minSupport
     * @return
     */
    private Clause minimizeCandidate(Clause candidate, int minSupport) {
        List<Literal> literals = Sugar.listFromCollections(candidate.literals());
        Set<Literal> wasRemoved = Sugar.set();
        for (Literal literal : literals) {
            Clause shorter = new Clause(Sugar.collectionDifference(candidate.literals(), literal));
            Pair<Boolean, Coverage> pair = matchesAny(shorter, null, null);
            if (pair.s.size() < minSupport && shorter.connectedComponents().size() < this.maxComponents) {
                candidate = minimizeComponentsWise(shorter, literals, literal, minSupport);
                wasRemoved.add(literal);
            }
        }
        return candidate;
    }

    private Clause minimizeComponentsWise(Clause candidate, List<Literal> literals, Literal stopLiteral, int minSupport) {
        int connectedComponents = candidate.connectedComponents().size();
        for (Literal literal : literals) {
            if (literal.equals(stopLiteral)) { // implementation dependent, only previous literals are considered
                break;
            }
            if (literal.arity() < 2 || !candidate.literals().contains(literal)) {
                continue;
            }
            Clause shorter = new Clause(Sugar.setDifference(candidate.literals(), literal));
            if (shorter.connectedComponents().size() != connectedComponents) {
                continue;
            }
            Pair<Boolean, Coverage> pair = matchesAny(shorter, null, null);
            if (pair.s.size() < minSupport) {
                candidate = shorter;
            }
        }
        return candidate;
    }

    private Collection<IsoClauseWrapper> subsample(Set<IsoClauseWrapper> beam, int maxBeamWidth) {
        if (maxBeamWidth > beam.size()) {
            return beam;
        }
        return Combinatorics.randomSelect(beam, maxBeamWidth);
    }

    private boolean inProcessed(Refinement refinement, Saturator<Clause> saturator, MultiMap<Integer, Clause> processed) {
        int hash = refinement.getSaturatedICW().hashCode();
        if (!processed.containsKey(hash)) {
            processed.put(hash, refinement.getNonSaturated());
            return false;
        }
        List<Clause> saturatedSiblings = Sugar.parallelStream(processed.get(hash).stream(), this.parallel)
                .map(saturator::saturate)
                .collect(Collectors.toList());

        for (Clause sibling : saturatedSiblings) {
            if ((null == sibling && null == refinement.getSaturated())
                    || (null != sibling && IsoClauseWrapper.create(sibling).equals(refinement.getSaturatedICW()))) {
                return true;
            }
        }
        processed.put(hash, refinement.getNonSaturated());
        return false;
    }

    private void runningStats(long debugRefinementsCount, long debugStart, Set<IsoClauseWrapper> hardConstraints, Set<IsoClauseWrapper> beam, long opened, Map<IsoClauseWrapper, Double> softConstraints, boolean computeStatistics, Counters<IsoClauseWrapper> statistics, Set<IsoClauseWrapper> minSuppConstraints) {
        java.util.function.Function<Long, String> format = (number) -> String.format("% 9d", number);

        System.out.println("dr  "
                + format.apply(this.debugTotalRefinementGenerated) + "\t"
                + format.apply(this.debugTotalEQCRef) + "\t"
                + format.apply(this.debugRefinementsCardinality) + "\t"
                + format.apply(debugRefinementsCount) + "\t"
                + format.apply(this.debugOverAllEQref) + "\t"
                + format.apply(opened) + "   out of  "
                + format.apply((long) beam.size())
                + format.apply(this.isoPrune) + "\t"
                + format.apply(this.killedBySaturations) + "\t"
                + " h " + format.apply((long) hardConstraints.size())
                + " m " + format.apply((long) minSuppConstraints.size())
                + " s " + format.apply((long) softConstraints.keySet().size())
                + "   " + ((System.nanoTime() - debugStart) / (60 * 1000000000.0)));
        if (computeStatistics) {
            System.out.println("literal stats");
            statistics.keySet().stream()
                    .map(icw -> new Pair<>(icw, statistics.get(icw)))
                    .sorted((o1, o2) -> o2.s - o1.s)
                    .forEach(pair -> System.out.println("\t" + pair.r.getOriginalClause() + "\t\t" + pair.s));
        }
    }


    // trochu prasecina
    private boolean checkMinimal(Clause candidate, int subsamplingSize, boolean nontrivialMinSupport) {
        boolean cannotUseCaching = Integer.MAX_VALUE != subsamplingSize || nontrivialMinSupport;
        if (Integer.MAX_VALUE == this.maxComponents && !cannotUseCaching) {
            return true;
        }
        for (Literal l : candidate.literals()) {
            if (!cannotUseCaching && l.arity() < 2) {
                continue;
            }
            Clause shorther = new Clause(Sugar.collectionDifference(candidate.literals(), l));
            if (cannotUseCaching || shorther.connectedComponents().size() > this.maxComponents) {
                if (!dataset.matchesAtLeastOne(shorther)) {
                    return false;
                }
            }
        }
        return true;
    }

    // equivalent method to minimal by querying cached parents
    private boolean allParentsInCache(Clause cand, Set<IsoClauseWrapper> parentsCache, boolean minSupportPrune) {
        // the check is in case that only connected components are mined
        for (Literal l : cand.literals()) {
            Clause shorther = new Clause(Sugar.collectionDifference(cand.literals(), l));
            if ((Integer.MAX_VALUE != this.maxComponents && shorther.connectedComponents().size() > this.maxComponents)
                    || minSupportPrune) {
                // cannot use caching, has to check all samples
                if (!dataset.matchesAtLeastOne(shorther)) {
                    return false;
                }
            } else if (!parentsCache.contains(IsoClauseWrapper.create(shorther))) {
                return false;
            }
        }
        return true;
    }

    /**
     * returns <b,s> where s is set of subsume examples and b is true iff this set is non-empty
     * matchesAny with caching all
     *
     * @param candidate
     * @param parent
     * @param cache
     * @return
     */
    private Pair<Boolean, Coverage> matchesAny(Clause candidate, IsoClauseWrapper parent, Map<IsoClauseWrapper, Coverage> cache) {
        Coverage scount;
        if (null != parent && null != cache && cache.containsKey(parent)) {
            scount = cache.get(parent);
        } else {
            scount = CoverageFactory.getInstance().get(dataset.size());
        }

        // that's because of the conjunction testing
        Coverage subsumed = dataset.subsumed(candidate, scount);
        boolean foundSome = subsumed.isNotEmpty();
        return new Pair<>(foundSome, subsumed);
    }

    private List<Refinement> refinements(Clause clause, Saturator<Clause> saturator, Set<IsoClauseWrapper> isoProcessed, Set<Literal> forbidden, MultiMap<IsoClauseWrapper, Literal> badRefinements, Saturator<Clause> filter) { // sem pridat informace naucene z constraints
        Set<Literal> badLiterals = ConcurrentHashMap.newKeySet();
        Set<Pair<IsoClauseWrapper, Set<Literal>>> set = new HashSet<>();
        Set<Literal> badRef = (null == badRefinements) ? null : badRefinements.fillIfNeedAndGet(IsoClauseWrapper.create(clause)); // tohle je naprosta blbost
        for (Pair<String, Integer> predicate : allPredicates) {
//        for (Pair<String, Integer> predicate : Sugar.set(new Pair<>("interaction", 2))) {
            set.addAll(Sugar.union(refinements(clause, predicate, true, forbidden), refinements(clause, predicate, false, forbidden)));
//            set.addAll(refinements(clause, predicate, true, forbidden));
        }

//        System.out.println("debug end1\t" + set.size());
//        System.exit(-1111);

        this.debugRefinementsCardinality += set.size(); // non-saturated non-isomorphic generated refinements

        List<Pair<IsoClauseWrapper, Set<Literal>>> filtered = set.stream().filter(pair -> !isoProcessed.contains(pair.r)).collect(Collectors.toList());

        // pocita se tady ta saturace z te saturace????

        //System.out.println("filtered\t" + filtered);
        //System.out.println(filtered.size());

        List<Refinement> refinements = Sugar.parallelStream(filtered, this.parallel)
                .filter(pair -> !filter.isPruned(pair.r.getOriginalClause()))
                .map(pair -> {
                    // tady pocitat z rozsirene saturace ;))
                    Clause saturated = saturator.saturate(pair.r.getOriginalClause());
                    if (null == saturated) {
                        if (null != badRef) { //  cosi shnileho ve state danskem
                            badLiterals.addAll(pair.s);
                        }
                        return null;
                    }
                    /*return Refinement.create(saturated,
                            pair.r.getOriginalClause(),
                            Sugar.setDifference(saturated.literals(), pair.r.getOriginalClause().literals()), // je tohle vubec spravne? nemelo by to byt obracene?
                            clause);*/
                    return Refinement.create(saturated,
                            pair.r.getOriginalClause(),
                            badLiterals
                    );
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        int bef = isoProcessed.size();
        isoProcessed.addAll(set.stream().map(pair -> pair.r).collect(Collectors.toList())); // neni tohle zdvojeny s tim filtered nahore?
        int added = isoProcessed.size() - bef;

        this.isoPrune += set.size() - added;
        this.killedBySaturations += added - refinements.size();

        if (null != badRefinements) {
            badLiterals.addAll(badRefinements.get(clause));
            refinements.forEach(r -> badRefinements.putAll(r.getSaturatedICW(), badLiterals));
        }
        return refinements;
    }

    private List<Pair<IsoClauseWrapper, Set<Literal>>> refinements(Clause clause, Pair<String, Integer> predicate, boolean negated, Set<Literal> forbidden) {
        // hopefully quick check for speed-up language bias check
        // to negated a podobne jeste posunout sem, kdyz to rozsirime o jednicku tak je to lepsi nez to potom delat opakovane v LB check
        if ((negated && clause.literals().stream().filter(Literal::isNegated).count() >= this.maxPosLiterals) // >= implies +1> for the left side, thus ending when the clause cannot be refined (it would violate the LB)
                || (!negated && clause.literals().stream().filter(literal -> !literal.isNegated()).count() >= this.maxNegLiterals)
                || clause.countLiterals() >= this.maxLiterals) {
            return Sugar.list();
        }

        MultiMap<IsoClauseWrapper, Literal> refinements = new MultiMap<>();
        Set<Variable> variables = clause.variables();
        Set<Variable> freshVariables = LogicUtils.freshVariables(variables, predicate.s);
        Literal freshLiteral = LogicUtils.newLiteral(predicate.r, predicate.s, freshVariables);
        if (negated) {
            freshLiteral = freshLiteral.negation();
        }

        Clause init = new Clause(Sugar.union(clause.literals(), freshLiteral));
        refinements.put(new IsoClauseWrapper(init), freshLiteral);
        this.debugTotalRefinementGenerated++;

        for (int i = 0; i < predicate.s; i++) {
            MultiMap<IsoClauseWrapper, Literal> newRefinements = new MultiMap<>();
            for (Map.Entry<IsoClauseWrapper, Set<Literal>> entry : refinements.entrySet()) {
                Literal refLiteral = entry.getValue().iterator().next();
                Variable x = (Variable) refLiteral.get(i);
                for (Variable v : entry.getKey().getOriginalClause().variables()) {
                    if (v != x) {
                        Literal newLiteral = LogicUtils.substitute(refLiteral, x, v);
                        if (this.useBad && forbidden.contains(newLiteral)) { // this should happen only if i == predicate.s-1, shouldn't it?
                            continue;
                        }
                        Clause substituted = LogicUtils.substitute(entry.getKey().getOriginalClause(), x, v);
                        if (substituted.countLiterals() > clause.countLiterals() && !substituted.containsLiteral(newLiteral.negation())) {
                            newRefinements.put(IsoClauseWrapper.create(substituted), newLiteral);
                            this.debugTotalRefinementGenerated++;
                        }
                    }
                }
            }
            refinements.putAll(newRefinements);
        }

        List<Pair<IsoClauseWrapper, Set<Literal>>> retVal = Sugar.parallelStream(refinements.entrySet().stream(), this.parallel)
                .filter(pair -> languageBiasCheck(pair.getKey().getOriginalClause()))
                .map(pair -> new Pair<>(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());


        this.debugTotalEQCRef += retVal.size();

        return retVal;
    }

    private boolean languageBiasCheck(Clause clause) {
        if (!bias.predicate().test(clause)) {
            return false;
        }
        // tohle by slo asi delat rychleji uvnitr :))
        // todo: vyhodit tady odsud test na pos a neg !!!!!!
        //long negativeLiterals = clause.literals().stream().filter(Literal::isNegated).count();
        //long positiveLiterals = clause.literals().size() - negativeLiterals;
        return clause.connectedComponents().size() <= this.maxComponents
                //&& positiveLiterals + negativeLiterals <= this.maxLiterals
                //&& positiveLiterals <= this.maxNegLiterals // because this is bias to learned clauses (not conjunction)
                //&& negativeLiterals <= this.maxPosLiterals // because this is bias to learned clauses (not conjunction)
                && clause.variables().size() <= this.maxVariables;
    }


    public static void main(String[] args) throws Exception {
        testMinimal();
        /*
        //Clause db = Clause.parse("b(x3),b(x1),a(x4),a(x2)");
        Clause db = Clause.parse("a(x1),a(x2),b(x3),b(x4),e(x1,x2),e(x2,x1),e(x4,x5)");
        //Clause db = Clause.parse("a(x1),a(x2),b(x3),b(x4),e(x1,x2),e(x2,x1),e(x5,x4)");
        ShortConstraintLearner url = new ShortConstraintLearner(new Dataset(db, Matching.OI_SUBSUMPTION));

        List<Clause> rules = url.learnConstraints();
        for (Clause c : rules) {
            System.out.println(c);
        }
        */
    }

    private static void testMinimal() throws IOException {
        Clause c = LogicUtils.flipSigns(Clause.parse("!1_bond(V1, V3), ar_bond(V1, V3), !au(V3), !o3(V1)"));

        String dataPath = String.join(File.separator, Sugar.list("..", "datasets", "nci_transformed", "gi50_screen_KM20L2.txt"));
        System.out.println("dataset path\t" + dataPath);

        int subsumptionMode = Matching.THETA_SUBSUMPTION;
        MEDataset dataset = MEDataset.create(dataPath, subsumptionMode);

        int absoluteMaxSupport = 0;
        int maxLiterals = 7;
        int maxVariables = 2 * maxLiterals;

        UltraShortConstraintLearnerFaster uscl = UltraShortConstraintLearnerFaster.create(dataset, 0, 0, 0, 1, 1, 1);
        System.out.println("is minimal\t" + uscl.checkMinimal(c, 2, false));
    }


    public static UltraShortConstraintLearnerFaster create(DatasetInterface dataset, int maxLiterals, int maxVariables, int absoluteAllowedSubsumption, int maxComponents, int maxPosLiterals, int maxNegLiterals) {
        return create(dataset, maxLiterals, maxVariables, absoluteAllowedSubsumption, maxComponents, maxPosLiterals, maxNegLiterals, null);
    }

    public static UltraShortConstraintLearnerFaster create(DatasetInterface dataset, int maxLiterals, int maxVariables, int absoluteAllowedSubsumption, int maxComponents, int maxPosLiterals, int maxNegLiterals, BiConsumer<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> outputPipe) {
        return create(dataset, maxLiterals, maxVariables, absoluteAllowedSubsumption, maxComponents, maxPosLiterals, maxNegLiterals, outputPipe, NoneBias.create());
    }

    public static UltraShortConstraintLearnerFaster create(DatasetInterface dataset, int maxLiterals, int maxVariables, int absoluteAllowedSubsumption, int maxComponents, int maxPosLiterals, int maxNegLiterals, BiConsumer<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> outputPipe, LanguageBias bias) {
        return new UltraShortConstraintLearnerFaster(dataset, maxLiterals, maxVariables, absoluteAllowedSubsumption, maxComponents, maxPosLiterals, maxNegLiterals, outputPipe, bias);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}

