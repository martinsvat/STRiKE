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
import ida.utils.collections.FakeMap;
import ida.utils.collections.FakeSet;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.theories.TheorySimplifier;
import logicStuff.learning.languageBias.LanguageBias;
import logicStuff.learning.saturation.ConjunctureSaturator;
import logicStuff.learning.saturation.IdentitySaturator;
import logicStuff.learning.saturation.Saturator;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;


/**
 * Created by ondrejkuzelka on 01/02/17.
 * <p>
 * Version with caching and pruning hacks, which should be faster than the original (MS). All samples are tested on theta-subsumption, tested clause is cached (so multiple testing of a clause should not occur), and being minimal is computed by querying cached parents.
 * <p>
 * big TODO make the learnConstraint methods nicer !!!
 */
public class UltraShortConstraintLearnerFasterSmarter {

    private final boolean useTypeChecking;
    private final Map<Pair<Predicate, Integer>, Type> types;
    private final Map<Pair<Predicate, Integer>, String> simplifiedTyping;
    private Map<Pair<String, Integer>, Predicate> predicateMap = new ConcurrentHashMap<>();

    {
        System.out.println("TODO: kdyz se pousti DTL se saturacemi tak pocitat saturaci saturace s novou DT at se usetri neco ;))");
        System.out.println("u refinements zbyva pridelat dalsi fukncionalitu -- vedle isForbiden (kvuli monotonicite) si drzet i ty ktere jsou zakazane diky constraints (ty potom budou urcite vystupem v saturacich)");
    }

    public static final int ONLY_POSITIVE_SATURATIONS = -1;

    public static final int ONLY_NEGATIVE_SATURATIONS = -2;
    public static final int BOTH_SATURATIONS = -3;
    public static final int NONE = -4;
    private final LanguageBias bias;
    private Random random;

    private final boolean oneLiteralHCrefinement;
    private final boolean hcExtendedRefinement;


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
    // else, just use negative saturations all the time and positive up to level i

    // this should be the depth rather then 0, because it is quite a usable pruning technique
    private int storeBadRefinementsTo = Integer.parseInt(System.getProperty("ida.searchPruning.badRefinementsTo", "0"));

    {
        System.out.println("predelat ida.logicStuff.UltraShortConstraintLearnerFaster. na ida.logicStuff.UltraShortConstraintLearnerFasterSmarter. nakonec!");
        System.out.println("parametrize storeBadRefinementsTo !!!!!!!!!!!!!!!");
    }


    private final boolean parallel = 1 < Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1"))
            || Boolean.parseBoolean(System.getProperty("ida.searchPruning.parallel", "false"));

    private boolean verbose = Boolean.parseBoolean(System.getProperty("ida.logicStuff.UltraShortConstraintLearnerFaster.verbose", "true"));

    public UltraShortConstraintLearnerFasterSmarter(DatasetInterface dataset, int maxLiterals, int maxVariables, int absoluteAllowedSubsumptions, int maxComponents, int maxPosLiterals, int maxNegLiterals, BiConsumer<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> outputPipe, LanguageBias bias, Map<Pair<Predicate, Integer>, Type> types, boolean oneLiteralHCrefinement, boolean hcExtendedRefinement) {
        this.dataset = dataset;
        // language bias
        this.allPredicates = /*Sugar.setDifference(this.dataset.allPredicates(),
                Sugar.parallelStream(typesTheory)
                        .map(clause -> Sugar.chooseOne(LogicUtils.positiveLiterals(clause)).getPredicate())
                        .collect(Collectors.toSet()));*/
                Sugar.setDifference(this.dataset.allPredicates(),
                        types.values().stream().map(Type::predicate).map(Predicate::getPair).collect(Collectors.toSet()));
        // types.keySet().stream().map(Pair::getR).map(Predicate::getPair).collect(Collectors.toSet()) tohle je uplne blbe
        this.maxLiterals = maxLiterals;
        this.maxVariables = maxVariables;
        this.maxComponents = maxComponents;
        this.maxPosLiterals = maxPosLiterals;
        this.maxNegLiterals = maxNegLiterals;
        this.absoluteAllowedSubsumptions = absoluteAllowedSubsumptions;
        this.outputPipe = outputPipe;
        this.random = new Random();
        this.bias = bias;
        this.types = types;
        this.useTypeChecking = null != types && types.keySet().size() > 1;
        this.simplifiedTyping = this.useTypeChecking ? TypesInducer.simplify(types) : null;
        this.oneLiteralHCrefinement = oneLiteralHCrefinement;
        this.hcExtendedRefinement = hcExtendedRefinement;
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
        if (recomputingSaturations) {
            System.out.println("recomputing=true is not implemented");
            throw new IllegalStateException(); //NotImplementedException();
        }

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
                    "\n\tminSupport\t" + minSupport +
                    "\n\tstores bad to\t" + storeBadRefinementsTo +
                    "\n\ttypes\t" + Sugar.setFromCollections(this.types.values()).size()
            );
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


        Set<IsoClauseWrapper> hardConstraints = Sugar.setFromCollections(initialTheory);
        Set<IsoClauseWrapper> minSuppConstraints = Sugar.setFromCollections(initialMinSuppConstraints);
        Map<IsoClauseWrapper, Double> softConstraints = new HashMap<>();
        Set<Refinement> level = new HashSet<>();
        Refinement empty = emptyRefinement();
        level.add(empty);
        Map<IsoClauseWrapper, Coverage> coverageCache = new HashMap<>();
        coverageCache.put(empty.getNonSaturatedICW(), CoverageFactory.getInstance().get(dataset.size()));
        //Counters<IsoClauseWrapper> statistics = new Counters<>();
        Set<IsoClauseWrapper> parentsCache = Sugar.set(); // cache of parents so we can check whether a candidate is minimal fast (by looking for all of its parents shorther of exactly one literal)
        // this cache has to be composed of non saturated ICW, we can't use saturated clauses here
        parentsCache.add(empty.getNonSaturatedICW());

        String statsResult = "";

        boolean minSupportPrune = 1 != minSupport;

        MultiMap<Pair<String, Integer>, String> oneLiteralConstraints = new MultiMap<>();
        Map<Pair<String, Integer>, List<Pair<Literal, Clause>>> longerConstraints = new HashMap<>(); // map<predicate, list of 'head' and body with existential quantificiation> (it is not head in the horn type meaning)

        List<Refinement> debugConstraints = Sugar.list();

        for (int currentLevel = 1; currentLevel <= maxLiterals; currentLevel++) {
            //MultiMap<Integer, Clause> processed = new MultiMap<>(); for recomputing saturations=true mode
            Map<IsoClauseWrapper, Coverage> coverageNextCache = new HashMap<>();
            Set<Refinement> nextLevel = new HashSet<>();
            Set<Refinement> levelFoundConstraints = Sugar.set();
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

            Saturator<Clause> saturator = (useSaturations) ? ConjunctureSaturator.create(simplifiedConstraints, whichSaturations.apply(currentLevel), allowedSaturatedPredicates, types) : IdentitySaturator.create();
            Saturator<Clause> filter = (useSaturations && minSupport > 1) // the minSupport > 1 condition is just a speed-up hack; see, if minSupport=1 then minSuppConstraints are always empty and thereafter there are two times saturation computation (filter, saturation) with the same theory
                    ? ConjunctureSaturator.create(Sugar.listFromCollections(simplifiedConstraints, minSuppConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList())))
                    : IdentitySaturator.create();

            Set<IsoClauseWrapper> isoProcessed = ConcurrentHashMap.newKeySet();
            Set<Refinement> isoSaturated = ConcurrentHashMap.newKeySet();
            // tohle jeste vyzkouset, u samplovaci verze by bad refinements mohly byt
            //MultiMap<IsoClauseWrapper, Literal> newBadRefinements = (currentLevel <= 2) ? new MultiMap<>(MultiMap.CONCURENT_HASH_SET) : null;
            // bad refinementy v tady te implementaci jsou delane jinak


            /*System.out.println("\n\ndebug vypis ");
            level.stream().sorted((r1, r2) -> {
                int size = Integer.compare(r1.getNonSaturated().countLiterals(), r2.getNonSaturated().countLiterals());
                if (0 == size) {
                    return r1.getNonSaturated().toString().compareTo(r2.getNonSaturated().toString());
                }
                return size;
            }).forEach(r -> System.out.println(r.getNonSaturated() + "\t|\t" + r.getSaturated()));

            System.out.println("\n\n");*/

            for (Refinement parent : subsample(level, subsamplingSize)) {
                opened++; // debug

                if (!time.enough()) {
                    break;
                }

                Pair<List<Refinement>, Set<Literal>> rPair = refinements(parent, saturator, isoProcessed, filter, oneLiteralConstraints, longerConstraints);
                Set<Literal> badRefinements = rPair.s;
                for (Refinement refinement : rPair.r) {
                    //System.out.println("r\t\t" + refinement.getNonSaturated());
//                    System.out.println(refinement.getNonSaturated());

                    if (!time.enough()) {
                        break;
                    }

                    //Clause candidate = refinement.getSaturated();
                    //IsoClauseWrapper canonicCandidate = refinement.getSaturatedICW();
                    /*if(useSaturations && !recomputingSaturations){ this should use space of saturated hypotheses but didn't work for the first time; let's fix it
                        candidate = refinement.getSaturated();
                        canonicCandidate = refinement.getSaturatedICW();
                    }*/

                    debugRefinementsCount++;

                    if (this.verbose && debugRefinementsCount % 1000 == 0) {
                        runningStats(debugRefinementsCount, debugStart, hardConstraints, level, opened, softConstraints, computeStatistic,
                                //statistics,
                                minSuppConstraints);
                    }

                    if (useSaturations &&
                            ((!recomputingSaturations && !isoSaturated.add(refinement)) // isoSaturated matches refinements by saturated clauses
                                    //        || (recomputingSaturations && inProcessed(refinement, saturator, processed))
                            )) {
                        if (computeStatistic) { // slows down the computation a little bit
                            throw new IllegalStateException(); //NotImplementedException();
                            //statistics.addAll(refinement.addedLiteralsStats());
                        }
                        // we saved cover computation (it's zero) but we would also like to prune its neighborous from this particular refinement
                        //badRefinements.addAll(refinement.addedLiterals()); this prune would not be complete, it could in some cases prune the space more than it is wanted -- there would have to be some mode of remembering these particular literals and updating the wrapper each time when two clauses are isomorphic
                        refinement.gc();
                        continue;
                    }

                    this.debugOverAllEQref++;

                    Pair<Boolean, Coverage> pair = matchesAny(refinement.getSaturated(), parent.getNonSaturatedICW(), coverageCache);
                    boolean matchesCondition = !pair.r;

                    Clause candidate = refinement.getNonSaturated(); // that's better because in minimal we test each single shorther clause by one, thus it is smarter to test the shorther clause

                    // cannot be merged with the lower part
                    // candidate is a conjunction
                    if (!matchesCondition && useSaturations && minSupportPrune && pair.s.size() < minSupport && LogicUtils.negativeLiteralsCount(candidate) < 1) {
                        Clause minimalCandidateConjunction = minimizeCandidate(candidate, minSupport);
                        minSuppConstraints.add(IsoClauseWrapper.create(LogicUtils.flipSigns(minimalCandidateConjunction)));
                        System.out.println("add this refinement to the bad ones? probably not, because in siblings it can lead to some hard constraints");
                        continue; // just skip the lower part in minsupp>1 case
                    }

                    if (matchesCondition) { // tady uz vim ze to pokryva cely dataset, je to ale minimalni?
                        if (useSaturations) {
                            // do not forget that when we would like to use parentsCache in chechMinimal, we have to verify how parentsCache is updated!
                            if (checkMinimal(candidate, subsamplingSize, minSupportPrune)) {
                                hardConstraints.add(IsoClauseWrapper.create(LogicUtils.flipSigns(candidate)));
                                levelFoundConstraints.add(refinement);
                            }
                        } else if (allParentsInCache(candidate, parentsCache, minSupportPrune)) {
                            hardConstraints.add(new IsoClauseWrapper(LogicUtils.flipSigns(candidate)));
                            levelFoundConstraints.add(refinement);
                        }
                        badRefinements.addAll(refinement.addedLiterals());
                    } else if (candidate.countLiterals() < maxLiterals) {//|| (currentLevel <= maxLiterals)) {
                        if (minSupport <= pair.s.size()) { // we can prune this candidate, either we've solve them before (so, just a check), or its children will have lower support than the threshold
                            nextLevel.add(refinement);
                            coverageNextCache.put(refinement.getNonSaturatedICW(), pair.s); // nemelo by se to delat podle saturovane klauzule
                            // add it to the set if we can (the bad refinement set)
                        }
                        if (pair.s.size() < absoluteAllowedSubsumptions) {
                            // appendix, throw away
                            softConstraints.put(IsoClauseWrapper.create(LogicUtils.flipSigns(candidate)), pair.s.size() / (1.0 * dataset.size()));
                            // add it to the set if we can (the bad refinement set)
                        }
//                    } else if (candidate.countLiterals() != maxLiterals) { // just check, should never occur (only in case of non-saturated space)
//                        System.out.println("the last layer? " + candidate.countLiterals() + " " + currentLevel + " " + maxLiterals);
                    }
                    refinement.gc();
                }

                if (badRefinements.isEmpty()) {
                    rPair.r.parallelStream().forEach(Refinement::adjustEmptyBadRefinements);
                }
            }


            if (verbose) {
                runningStats(debugRefinementsCount, debugStart, hardConstraints, level, opened, softConstraints, computeStatistic,
                        //statistics,
                        minSuppConstraints);

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
                // tohle je desnej shit kterej je asi i spatne
                int finalCurrentLevel = currentLevel;
                ConjunctureSaturator levelSaturator = ConjunctureSaturator.create(hardConstraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()),
                        whichSaturations.apply(finalCurrentLevel),
                        allowedSaturatedPredicates, types);
                Set<Refinement> saturatedConstraints = Sugar.parallelStream(nextLevel, this.parallel)
                        .map(refinement -> {
                            Clause saturation = levelSaturator.saturate(refinement.getSaturated());
                            if (null == saturation) {
                                return null;
                            }
                            return Refinement.create(saturation, refinement.getNonSaturated(), null, Sugar.set(), Sugar.set());
                        })
                        .filter(Objects::nonNull).collect(Collectors.toSet());
                // tyhle dve zakomentovany radky fakt nechapu :(
                //Set<Refinement> filteredBeam = Sugar.setFromCollections(saturatedConstraints).stream()
                //        .map(Refinement::getNonSaturatedICW).collect(Collectors.toSet());
                Set<Refinement> filteredBeam = saturatedConstraints;

                if (verbose) {
                    System.out.println("\nafter level pruned\t" + (nextLevel.size() - filteredBeam.size()) + "\n");
                }
                level = filteredBeam;
            } else {
                /*if (!useSaturations) { proc je tady negace saturaci?
                    parentsCache = coverageNextCache.keySet();
                }*/
                parentsCache = (useSaturations) ? Sugar.set() : coverageNextCache.keySet();
                level = nextLevel;
            }
            coverageCache = coverageNextCache;

            statsResult += levelStats(currentLevel, isoProcessed, isoSaturated, debugStart, hardConstraints);


            // gathering / processing information for refinement operator extension
            if (1 == currentLevel && this.oneLiteralHCrefinement) {
                levelFoundConstraints.stream().forEach(r -> {
                    Literal literal = Sugar.chooseOne(r.getNonSaturated().literals());
                    oneLiteralConstraints.put(literal.getPredicate(), toCanon(literal));
                });
            } else if (currentLevel > 1 && this.hcExtendedRefinement) {
                for (Refinement r : levelFoundConstraints) {
                    // refinement represents an existentially quantified conjunction
                    if (r.getNonSaturated().literals().stream().anyMatch(Literal::isNegated)) { // all must be positive <=> negative literal in conjunction
                        continue; // we do this only for positivie lite
                    }
                    List<Literal> literals = Sugar.listFromCollections(r.getNonSaturated().literals());
                    for (int idx = 0; idx < literals.size(); idx++) {
                        Literal literal = literals.get(idx);
                        List<Pair<Literal, Clause>> transformed = longerConstraints.get(literal.getPredicate());
                        if (null == transformed) {
                            transformed = Sugar.list();
                            longerConstraints.put(literal.getPredicate(), transformed);
                        }
                        int finalIdx = idx;
                        List<Literal> body = IntStream.range(0, literals.size()).filter(innerIdx -> innerIdx != finalIdx).mapToObj(literals::get).collect(Collectors.toList());
                        transformed.add(new Pair<>(literal, new Clause(body)));
                    }
                }
            }

            debugConstraints.addAll(levelFoundConstraints);

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
            /*System.out.println("counters");
            System.out.println("literal stats");
            statistics.keySet().stream()
                    .map(icw -> new Pair<>(icw, statistics.get(icw)))
                    .sorted((o1, o2) -> o2.s - o1.s)
                    .forEach(pair -> System.out.println("\t" + pair.r.getOriginalClause() + "\t\t" + pair.s));
            */
            System.out.println("uscl:theory simplifier is turned-off");
        }

        //hardConstraints.forEach(System.out::println);


        /**/
        System.out.println("\n\n constraints plain \t\t\t saturated");
        debugConstraints.stream().sorted((r1, r2) -> {
            int size = Integer.compare(r1.getNonSaturated().countLiterals(), r2.getNonSaturated().countLiterals());
            if (0 == size) {
                return r1.getNonSaturated().toString().compareTo(r2.getNonSaturated().toString());
            }
            return size;
        }).forEach(r -> System.out.println(r.getNonSaturated() + "\t|\t" + r.getSaturated()));
        /**/

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

    private String toCanon(Literal literal) {
        StringBuilder sb = new StringBuilder();
        //sb.append(literal.predicate() + "_");
        Map<Term, Integer> map = new HashMap<>();
        for (int idx = 0; idx < literal.arity(); idx++) {
            Term term = literal.get(idx);
            Integer originalPosition = map.get(term);
            if (null == originalPosition) {
                originalPosition = map.keySet().size() + 1;
                map.put(term, originalPosition);
            }
            if (idx > 0) {
                sb.append("," + originalPosition);
            }
        }
        return sb.toString();
    }


    private String levelStats(int currentLevel, Set<IsoClauseWrapper> isoProcessed, Set<Refinement> isoSaturated, long debugStart, Collection<IsoClauseWrapper> hardConstraints) {
        java.util.function.Function<Long, String> format = (number) -> String.format("% 9d", number);

        return "\nlevel\t\t" + currentLevel
                + format.apply(this.debugRefinementsCardinality) + "\t"
                + format.apply((long) isoProcessed.size()) + "\t"
                + format.apply((long) isoSaturated.size()) + "\t"
                + format.apply((long) hardConstraints.size()) + "\t"
                + "\t\t\t\t" + ((System.nanoTime() - debugStart) / (60 * 1000000000.0))
                ;
    }


    private Refinement emptyRefinement() {
        Clause empty = new Clause();
        return Refinement.create(empty, empty, Sugar.set());
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
        System.out.println("todo: metodu minimize candidate zkontrolovat");

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

    private <R> Collection<R> subsample(Set<R> beam, int maxBeamWidth) {
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

    private void runningStats(long debugRefinementsCount, long debugStart, Set<IsoClauseWrapper> hardConstraints, Set<Refinement> beam, long opened, Map<IsoClauseWrapper, Double> softConstraints,
                              boolean computeStatistics,
                              //Counters<IsoClauseWrapper> statistics,
                              Set<IsoClauseWrapper> minSuppConstraints) {
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
        /*if (computeStatistics) {
            System.out.println("literal stats");
            statistics.keySet().stream()
                    .map(icw -> new Pair<>(icw, statistics.get(icw)))
                    .sorted((o1, o2) -> o2.s - o1.s)
                    .forEach(pair -> System.out.println("\t" + pair.r.getOriginalClause() + "\t\t" + pair.s));
        }*/
    }

    // on contrary, if the c \ l_i is not connected, we can look up by ICW whether all components of it have non-zero support -- but we would have to keep this information of opened nodes somewhere
    // slo by tohle nejak hacknout do subsumpcniho algoritmu statefully?
    // nemuzu kontrolovat minimal pomoci all parents in cache protoze tam muzou byt i nesouvisly komponenty
    // trochu prasecina
    private boolean checkMinimal(Clause candidate, int subsamplingSize, boolean nontrivialMinSupport) {
        /*boolean cannotUseCaching = Integer.MAX_VALUE != subsamplingSize || nontrivialMinSupport;  nevidim proc ted
        if (Integer.MAX_VALUE == this.maxComponents && !cannotUseCaching) { // some interesting case that can be solved in O(1), really?
            return true;
        }*/
        for (Literal l : candidate.literals()) {
            /*if (!cannotUseCaching && l.arity() < 2) { // why? asi je zřejmné že unarni predikat je dalsi podminka a bez ni je ten test jednodušší... co když je to tvaru c(X), h(X)....
                continue;
            }*/
            Clause shorther = new Clause(Sugar.collectionDifference(candidate.literals(), l));
            //if (cannotUseCaching || shorther.connectedComponents().size() > this.maxComponents) { // nevidim ty podminky tady, proste to musim otestovat
//            System.out.println(shorther);
            if (!dataset.matchesAtLeastOne(shorther)) {
//                System.out.println("\tfalse");
                return false;

            }
//            System.out.println("\ttrue");
            //}
        }
        return true;
    }


    {
        System.out.println("velke TODO");
        System.out.println("tady musi byt anti LB bias a hlavne anti type bias, tzn kdyz to neni podle typu ok, tak to nemuze byt ani v cache !!!");
    }

    // equivalent method to minimal by querying cached parents
    private boolean allParentsInCache(Clause cand, Set<IsoClauseWrapper> parentsCache, boolean minSupportPrune) {
        boolean componentsLimited = Integer.MAX_VALUE != this.maxComponents;
        // the check is in case that only connected components are mined

        //Map<Variable, Type> clauseTypes = collectToType(cand);

        for (Literal l : cand.literals()) {
            Clause shorther = new Clause(Sugar.collectionDifference(cand.literals(), l));
            if ((componentsLimited && shorther.connectedComponents().size() > this.maxComponents)
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

    private Pair<List<Refinement>, Set<Literal>> refinements(Refinement parent, Saturator<Clause> saturator, Set<IsoClauseWrapper> isoProcessed, Saturator<Clause> filter, MultiMap<Pair<String, Integer>, String> oneLiteralConstraints, Map<Pair<String, Integer>, List<Pair<Literal, Clause>>> longerConstraints) {
        Set<Literal> finalBadLiterals = parent.getNonSaturated().countLiterals() <= this.storeBadRefinementsTo ? ConcurrentHashMap.newKeySet() : new FakeSet<>();
        List<Pair<IsoClauseWrapper, Set<Literal>>> rawRefinements = Sugar.list();
        //Map<Variable, Type> variableTypes = collectToType(parent.getNonSaturated());
        Set<Variable> variables = parent.getNonSaturated().variables();
        Matching world = (this.hcExtendedRefinement && !longerConstraints.keySet().isEmpty()) ? Matching.create(LogicUtils.constantizeClause(parent.getSaturated()), Matching.THETA_SUBSUMPTION) : null;
        for (Pair<String, Integer> predicate : allPredicates) {
            rawRefinements.addAll(Sugar.union(refinements(parent, predicate, true
                    //, variableTypes
                    , oneLiteralConstraints, world, longerConstraints, finalBadLiterals, variables),
                    refinements(parent, predicate, false
                            //, variableTypes
                            , oneLiteralConstraints, world, longerConstraints, finalBadLiterals, variables)));
//        for (Pair<String, Integer> predicate : Sugar.set(new Pair<>("interaction", 2))) {
//            rawRefinements.addAll(refinements(parent, predicate, true, variableTypes));
        }
        this.debugRefinementsCardinality += rawRefinements.size();

//        System.out.println("generated\t" + parent.getNonSaturated());
//        rawRefinements.forEach(c -> System.out.println(c.r.getOriginalClause()));

//        System.out.println("debug exit here");
//        System.exit(-12222);

        // if we want parallel stream here, we would need ICW with synchronization amongs enriched, etc.
        List<Pair<IsoClauseWrapper, Set<Literal>>> filtered = rawRefinements.stream().filter(pair -> !isoProcessed.contains(pair.r)).collect(Collectors.toList());

        /*Set<Literal> badLiterals = null;
        if (parent.getNonSaturated().countLiterals() <= this.storeBadRefinementsTo) {
            badLiterals = ConcurrentHashMap.newKeySet();
            badLiterals.addAll(parent.getBadRefinements());
        }
        Set<Literal> finalBadLiterals = badLiterals;
        */

        List<Refinement> refinements = Sugar.parallelStream(filtered, this.parallel)
                .filter(pair -> !filter.isPruned(pair.r.getOriginalClause())) // however, even if the saturation clause is not pruned by IdentitySaturator (in case of minSupport=1) it may be inconsistent with the theory
                .map(pair -> {
                    Clause saturated = saturator.saturate(new Clause(Sugar.union(pair.r.getOriginalClause().literals(), parent.getSaturated().literals()))
                            //,(l) -> finalBadLiterals.contains(l) || parent.isForbidden(l));
                            //,(l) -> parent.isForbidden(l) // todo: tady by nemelo byt parent.isForbidden(l) ale parent.isForbiddenByTheory(l), jinak se tu budou michat jablka s hruskami (cast literalu je zakazana kvuli (anti-)monotonicite, druha cast literalu je zakazana kvuli proriznuti pomoci oneLitHC a multiLitHC -- prave na tu druhou se pouze chceme dotazovat)
                    );
                    if (null == saturated) {
                        finalBadLiterals.addAll(pair.s);
                        return null;
                    }
                    Clause refinementWithoutSaturations = new Clause(Sugar.union(parent.getNonSaturated().literals()
                            , Sugar.setDifference(pair.r.getOriginalClause().literals(), parent.getSaturated().literals())));
                    return Refinement.create(saturated,
                            refinementWithoutSaturations,
                            parent,
                            pair.getS(),
                            finalBadLiterals);
                }).filter(Objects::nonNull).collect(Collectors.toList());

        int bef = isoProcessed.size();
        rawRefinements.stream().forEach(pair -> isoProcessed.add(pair.r));
        int added = isoProcessed.size() - bef;

        this.isoPrune += rawRefinements.size() - added; // todo: zokntrolovat ukladani
        this.killedBySaturations += added - refinements.size(); // todo: tohle by melo byt obracene, ne? refinements.size() - added, protoze pocet prvni je vytvorencyh a added je # kterych je novych... mozna je to cele spatne

        return new Pair<>(refinements, finalBadLiterals);
    }

    // clause is assumed to be filled with variables only
    private Map<Variable, Type> collectToType(Clause clause) {
        Map<Variable, Type> retVal = new HashMap<>();
        for (Literal literal : clause.literals()) {
            IntStream.range(0, literal.arity()).forEach(idx -> {
                Variable variable = (Variable) literal.get(idx);
                if (!retVal.containsKey(variable)) {
                    retVal.put(variable, this.types.get(new Pair<>(predicate(literal), idx)));
                }
            });
        }
        return retVal;
    }

    // just for lowering memory consumption
    private Predicate predicate(Literal literal) {
        Pair<String, Integer> key = literal.getPredicate();
        Predicate predicate = this.predicateMap.get(key);
        if (null == predicate) {
            predicate = Predicate.create(key);
            this.predicateMap.put(key, predicate);
        }
        return predicate;
    }

    // the statefull changes within accumulator are not nice, but it should be obvious from the name
    // returns refinements of the non-saturated clause
    private List<Pair<IsoClauseWrapper, Set<Literal>>> refinements(Refinement parent, Pair<String, Integer> predicate, boolean negated, MultiMap<Pair<String, Integer>, String> oneLiteralConstraints, Matching world, Map<Pair<String, Integer>, List<Pair<Literal, Clause>>> longerConstraints, Set<Literal> badRefinementsAccumulator, Set<Variable> variables) { // , Map<Variable, Type> variableTypes
        // hopefully quick check for speed-up language bias check
        if ((negated && parent.getNonSaturated().literals().stream().filter(Literal::isNegated).count() >= this.maxPosLiterals) // >= implies +1> for the left side, thus ending when the clause cannot be refined (it would violate the LB)
                || (!negated && parent.getNonSaturated().literals().stream().filter(literal -> !literal.isNegated()).count() >= this.maxNegLiterals)
                || parent.getNonSaturated().countLiterals() >= this.maxLiterals) {
            return Sugar.list();
        }

        // watch out, linked hash set needs to be as the underlying collection (since order is preserved); in the case different data structure would be used, we would need to use map<ICW, Pair<literal, Set<literal>>> where the the first literal of the pair would be the one literal inserted to the original clause, so the literal would be in the clause represented by ICW
        MultiMap<IsoClauseWrapper, Literal> refinements = new MultiMap<>(MultiMap.LINKED_HASH_SET);
        Predicate pred = Predicate.create(predicate);
        List<Variable> freshVariables = useTypeChecking ? LogicUtils.freshVariables(variables, this.simplifiedTyping, pred) : Sugar.listFromCollections(LogicUtils.freshVariables(variables, predicate.s));
        Literal freshLiteral = new Literal(predicate.r, negated, freshVariables);

//        Literal finalFreshLiteral = freshLiteral;
//        Map<Variable, Type> newVariablesTypes = new HashMap<>();
//        IntStream.range(0, freshLiteral.arity()).forEach(idx -> newVariablesTypes.put((Variable) finalFreshLiteral.get(idx), this.types.get(new Pair<>(pred, idx))));


        Clause init = new Clause(Sugar.union(parent.getNonSaturated().literals(), freshLiteral));
        IsoClauseWrapper initialRef = new IsoClauseWrapper(init);

        refinements.put(initialRef, freshLiteral);
        this.debugTotalRefinementGenerated++;

//        IsoClauseWrapper debugICW = IsoClauseWrapper.create(Clause.parse("taughtBy(V1, V2, V3), ta(V1, V4, V3), Faculty_adjunct(V2)"));

        for (int i = 0; i < predicate.s; i++) {
            MultiMap<IsoClauseWrapper, Literal> newRefinements = new MultiMap<>(MultiMap.LINKED_HASH_SET);
            for (Map.Entry<IsoClauseWrapper, Set<Literal>> entry : refinements.entrySet()) {
                Literal refLiteral = entry.getValue().iterator().next();
                Variable x = (Variable) refLiteral.get(i);
                for (Variable v : entry.getKey().getOriginalClause().variables()) {

                    if (v != x &&
                            (!this.useTypeChecking || areSameTypes(v, x))
                        //areSameTypes(v, x, variableTypes, newVariablesTypes)
                            ) {
                        Literal newLiteral = LogicUtils.substitute(refLiteral, x, v);

                        if (predicate.s == i + 1    // just a hack to run the minimum number of tests for forbidden and saturated
                                && (parent.isForbidden(newLiteral) // I can do this here because literals containing only variables from parent are in forbidden; and from such literal I cannot (by this method) generate literals with newly added variables
                                //(null != parent.getBadRefinements() && parent.getBadRefinements().contains(newLiteral)) // some old coment, irrelevant: this should happen only if i == predicate.s-1, shouldn't it?
                                || parent.getSaturated().literals().contains(newLiteral) // because we want to prune those saturations which are already pruned by the saturations
                        )) {
                            continue;
                        }
                        Clause substituted = LogicUtils.substitute(entry.getKey().getOriginalClause(), x, v);
                        if (substituted.countLiterals() > parent.getNonSaturated().countLiterals() && !substituted.containsLiteral(newLiteral.negation())) {

                            newRefinements.put(IsoClauseWrapper.create(substituted), newLiteral);

                            this.debugTotalRefinementGenerated++;
                        }

                    }
                }
            }
            refinements.putAll(newRefinements);
        }

//        stare typy
//        if (violatesTypes(freshLiteral, newVariablesTypes)) {
//            refinements.remove(initialRef);
//        }

        Stream<Map.Entry<IsoClauseWrapper, Set<Literal>>> refinementStream = Sugar.parallelStream(refinements.entrySet().stream(), this.parallel);
        if (this.oneLiteralHCrefinement && oneLiteralConstraints.keySet().size() > 0) {
            refinementStream = refinementStream.map(pair -> {
                Literal l = Sugar.chooseOne(pair.getValue());
                Set<String> forbiddens = oneLiteralConstraints.get(l.getPredicate());
                if (!forbiddens.isEmpty() && forbiddens.contains(toCanon(l))) {
                    badRefinementsAccumulator.addAll(pair.getValue());
                    return null;
                }
                return pair;
            }).filter(Objects::nonNull);
        }
        refinementStream = refinementStream.filter(pair -> languageBiasCheck(pair.getKey().getOriginalClause()));

        // !negated because we are operating can use this pruning only on existentially conjunctions
        if (this.hcExtendedRefinement && !negated && !longerConstraints.keySet().isEmpty()) {
            refinementStream = refinementStream
                    .sequential() // because we can't run matching (world) in parallel
                    .map(pair -> {

                        Literal l = Sugar.chooseOne(pair.getValue());
                        List<Pair<Literal, Clause>> constraints = longerConstraints.get(l.getPredicate());
                        if (null == constraints || constraints.isEmpty()) {
                            return pair;
                        }

                        for (Literal literal : pair.getValue()) {
                            if (pair.getKey().getOriginalClause().literals().contains(literal)) {
                                l = literal; // kind of overloading, not nice
                                break;
                            }
                        }

                        for (Pair<Literal, Clause> constraintPair : constraints) {
                            Map<Term, Term> partialSubstitutions = unify(constraintPair.getR(), LogicUtils.constantize(l));
                            if (null == partialSubstitutions) {
                                continue;
                            }
                            Clause substitutedBody = LogicUtils.substitute(constraintPair.getS(), partialSubstitutions); // existentially quantified
                            if (world.subsumption(substitutedBody, 0)) {
                                badRefinementsAccumulator.addAll(pair.getValue());
                                return null;
                            }
                        }

                        return pair;
                    }).filter(Objects::nonNull);
        }

        List<Pair<IsoClauseWrapper, Set<Literal>>> retVal = refinementStream
                .map(pair -> new Pair<>(pair.getKey(), pair.getValue()))
                .collect(Collectors.toList());

        this.debugTotalEQCRef += retVal.size();

        return retVal;
    }

    private boolean areSameTypes(Variable v, Variable x) {
        return (null == v.type() && null == x.type())
                || (null != v.type() && v.type().equals(x.type()));
    }

    // returns null iff unification is not possible
    private Map<Term, Term> unify(Literal nonGround, Literal ground) {
        if (!nonGround.getPredicate().equals(ground.getPredicate()) || nonGround.isNegated() != ground.isNegated()) {
            return null;
        }
        Map<Term, Term> retVal = new HashMap<>();
        for (int idx = 0; idx < nonGround.arity(); idx++) {
            Term src = nonGround.get(idx);
            Term img = ground.get(idx);
            Term alreadyImg = retVal.get(src);
            if (null == alreadyImg) {
                retVal.put(src, img);
            } else if (!alreadyImg.equals(img)) {
                return null;
            }
        }

        return retVal;
    }

    private boolean violatesTypes(Literal literal, Map<Variable, Type> newVariablesTypes) {
        Map<Variable, Type> map = new HashMap<>();
        for (int idx = 0; idx < literal.arity(); idx++) {
            Variable variable = (Variable) literal.arguments()[idx];
            Type type = newVariablesTypes.get(variable);
            Pair<Pair<String, Integer>, Integer> key = new Pair<>(literal.getPredicate(), idx);
            Type previouslyDefinedType = map.get(key);
            if (null == previouslyDefinedType) {
                map.put(variable, type);
            } else if (!previouslyDefinedType.equals(type)) {
                return true;
            }
        }
        return false;
    }

    private boolean areSameTypes(Variable v, Variable x, Map<Variable, Type> variableTypes, Map<Variable, Type> newVariablesTypes) {
        Type vType = variableTypes.get(v);
        if (null == vType) {
            vType = newVariablesTypes.get(v);
        }

        Type xType = newVariablesTypes.get(x);
        if (null == xType) {
            xType = variableTypes.get(x);
        }
//        System.out.println(vType.getId() + "\t" + xType.getId());

        return vType.equals(xType);
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
        //testMinimal2();
        //testMinimalTypeRefinement();
        testMinimalTypeRefinement2();

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

    private static void testMinimalTypeRefinement2() {
        ///////////// setting
        // only needed to get literals there ;)
        MEDataset dataset = MEDataset.create(Sugar.list(Clause.parse("protein_class(a,b), enzyme(a,c)")), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);

        Predicate p = Predicate.construct("protein_class/2");
        Predicate e = Predicate.construct("enzyme/2");

        // goal -> p(T1,T2), e(T1,T3)
        HashMap<Pair<Predicate, Integer>, Type> testTypes = new HashMap<>();
        Type t1 = new Type(1, p, 0);
        Type t2 = new Type(2, p, 1);
        Type t3 = new Type(3, e, 1);
        testTypes.put(new Pair<>(p, 0), t1);
        testTypes.put(new Pair<>(e, 0), t1);
        testTypes.put(new Pair<>(p, 1), t2);
        testTypes.put(new Pair<>(e, 1), t3);


        int absoluteMaxSupport = 0;
        int maxLiterals = 7;
        int maxVariables = 2 * maxLiterals;
        UltraShortConstraintLearnerFasterSmarter uscl = UltraShortConstraintLearnerFasterSmarter.create(dataset, 0, 0, 0, 1, 1, 1, null, null, testTypes, false, false);


        ////////////////////// collectToType
        Clause c = Clause.parse("protein_class(V1,V2)");
        Map<Variable, Type> variableTypes = uscl.collectToType(c);


//        System.out.println("variable types\t" + variableTypes);
//        variableTypes.keySet().forEach(v -> System.out.println(v + "\t" + variableTypes.get(v)));

        if (!variableTypes.get(Variable.construct("V1")).equals(t1)) {
            System.out.println("error in collectToType\t1");
            System.out.println(variableTypes.get(Variable.construct("V1")) + "\tvs\t" + t1);
        }
        if (!variableTypes.get(Variable.construct("V2")).equals(t2)) {
            System.out.println(variableTypes.get(Variable.construct("V2")) + "\tvs\t" + t2);
            System.out.println("error in collectToType\t2");
        }

        Refinement parent = Refinement.create(c, c);
        List<Pair<IsoClauseWrapper, Set<Literal>>> refinements = uscl.refinements(parent, e.getPair(), false
                //, variableTypes
                , new MultiMap<>(), null, new FakeMap<>(), new FakeSet<>(), parent.getNonSaturated().variables());

        System.out.println("refinements");
        refinements.forEach(System.out::println);
    }


    private static void testMinimalTypeRefinement() {
        ///////////// setting
        // only needed to get literals there ;)
        MEDataset dataset = MEDataset.create(Sugar.list(Clause.parse("p(a,a), b(b,b)")), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);

        Predicate p = Predicate.construct("p/2");
        Predicate b = Predicate.construct("b/2");

        // goal -> p(T1,T1), b(T2,T3)
        HashMap<Pair<Predicate, Integer>, Type> testTypes = new HashMap<>();
        Type t1 = new Type(1, p, 0);
        Type t2 = new Type(2, b, 0);
        Type t3 = new Type(3, b, 1);
        testTypes.put(new Pair<>(p, 0), t1);
        testTypes.put(new Pair<>(p, 1), t1);
        testTypes.put(new Pair<>(b, 0), t2);
        testTypes.put(new Pair<>(b, 1), t3);


        int absoluteMaxSupport = 0;
        int maxLiterals = 7;
        int maxVariables = 2 * maxLiterals;
        UltraShortConstraintLearnerFasterSmarter uscl = UltraShortConstraintLearnerFasterSmarter.create(dataset, 0, 0, 0, 1, 1, 1, null, null, testTypes, false, false);


        ////////////////////// collectToType
        Clause c = Clause.parse("p(V1,V2), b(V3, V4)");
        Map<Variable, Type> variableTypes = uscl.collectToType(c);


//        System.out.println("variable types\t" + variableTypes);
//        variableTypes.keySet().forEach(v -> System.out.println(v + "\t" + variableTypes.get(v)));

        if (!variableTypes.get(Variable.construct("V1")).equals(t1)) {
            System.out.println("error in collectToType\t1");
            System.out.println(variableTypes.get(Variable.construct("V1")) + "\tvs\t" + t1);
        }
        if (!variableTypes.get(Variable.construct("V2")).equals(t1)) {
            System.out.println(variableTypes.get(Variable.construct("V1")) + "\tvs\t" + t1);
            System.out.println("error in collectToType\t2");
        }

        Refinement parent = Refinement.create(c, c);
        List<Pair<IsoClauseWrapper, Set<Literal>>> refinements = uscl.refinements(parent, b.getPair(), false
                //,variableTypes
                , new MultiMap<>(), null, new FakeMap<>(), new FakeSet<>(), parent.getNonSaturated().variables());

        System.out.println("refinements");
        refinements.forEach(System.out::println);
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

        //UltraShortConstraintLearnerFasterSmarter uscl = UltraShortConstraintLearnerFasterSmarter.create(dataset, 0, 0, 0, 1, 1, 1);
        //System.out.println("is minimal\t" + uscl.checkMinimal(c, 2, false));
    }

    private static void testMinimal2() throws IOException {
        Clause c = LogicUtils.flipSigns(Clause.parse("!interaction(V2, V2), !location(V3, V2)"));

        Path dataPath = Paths.get("..", "datasets", "protein", "train.db.oneLine");
        MEDataset dataset = MEDataset.create(dataPath, Matching.THETA_SUBSUMPTION);

        int absoluteMaxSupport = 0;
        int maxLiterals = 7;
        int maxVariables = 2 * maxLiterals;

        UltraShortConstraintLearnerFasterSmarter uscl = UltraShortConstraintLearnerFasterSmarter.create(dataset, 0, 0, 0, 1, 1, 1, null, null, new HashMap<>(), false, false);
        System.out.println("is minimal\t" + uscl.checkMinimal(c, Integer.MAX_VALUE, false));
    }


/*
    public static UltraShortConstraintLearnerFasterSmarter create(DatasetInterface dataset, int maxLiterals, int maxVariables, int absoluteAllowedSubsumption, int maxComponents, int maxPosLiterals, int maxNegLiterals) {
        return create(dataset, maxLiterals, maxVariables, absoluteAllowedSubsumption, maxComponents, maxPosLiterals, maxNegLiterals, null);
    }

    public static UltraShortConstraintLearnerFasterSmarter create(DatasetInterface dataset, int maxLiterals, int maxVariables, int absoluteAllowedSubsumption, int maxComponents, int maxPosLiterals, int maxNegLiterals, BiConsumer<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> outputPipe) {
        return create(dataset, maxLiterals, maxVariables, absoluteAllowedSubsumption, maxComponents, maxPosLiterals, maxNegLiterals, outputPipe, NoneBias.create());
    }
*/

    public static UltraShortConstraintLearnerFasterSmarter create(DatasetInterface dataset, int maxLiterals, int maxVariables, int absoluteAllowedSubsumption, int maxComponents, int maxPosLiterals, int maxNegLiterals, BiConsumer<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> outputPipe, LanguageBias bias, Map<Pair<Predicate, Integer>, Type> types, boolean oneLiteralHCrefinement, boolean hcExtendedRefinement) {
        return new UltraShortConstraintLearnerFasterSmarter(dataset, maxLiterals, maxVariables, absoluteAllowedSubsumption, maxComponents, maxPosLiterals, maxNegLiterals, outputPipe, bias, types, oneLiteralHCrefinement, hcExtendedRefinement);
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }


}


