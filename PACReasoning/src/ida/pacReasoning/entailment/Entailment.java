package ida.pacReasoning.entailment;

import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.subsumption.Matching;
import ida.pacReasoning.entailment.collectors.Entailed;
import ida.pacReasoning.entailment.collectors.KECollector;
import ida.pacReasoning.entailment.collectors.LevelCollector;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.entailment.nodes.ClassicalLogicNode;
import ida.pacReasoning.entailment.nodes.MutableNode;
import ida.pacReasoning.entailment.nodes.PossLogicNode;
import ida.pacReasoning.entailment.theories.FOL;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.entailment.theories.Theory;
import ida.utils.Sugar;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Quadruple;
import ida.utils.tuples.Triple;
import logicStuff.theories.GroundTheorySolver;
import logicStuff.theories.TheorySimplifier;
import logicStuff.theories.TheorySolver;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 7. 6. 2018.
 */
public class Entailment {

    public static final int K_ENTAILMENT = 1;
    public static final int V_ENTAILMENT = 2;
    public static final int All_EVIDENCE = 3;
    public static final int R_ENTAILMENT = 4;
    public static final int ONE_CHAIN_ENTAILMENT = 5;
    public static final int ONE_STEP = 6;
    public static final int All_EVIDENCE_WITHOUT_CONSTRAINTS = 7;
    public static final int K_WITHOUT_CONSTRAINTS = 8;
    public static final int K_CONSTRAINTS_FILTER = 9;
    public static final int All_EVIDENCE_CONSTRAINTS_FILTER = 10;

    public static final int NONE_SATURATION = 0;
    public static final int FULL_SATURATION = 1;
    public static final int SYMMETRY_SATURATION = 2;
    private static final int AT_MOST_HORN = 0;
    private static final int HORN_ONLY = 1;
    private static final int MORE_THAN_HORN = 1;

    private final boolean verbose;

    private final MultiMap<Constant, Literal> constantToLiterals;
    private final Set<Constant> constants;
    private final Set<Literal> evidence;

    private final boolean parallel = true;
    private final Map<Integer, MultiMap<Subset, Literal>> subsetToLiterals;
    private final Set<Subset> subsetConstants;
    private final Map<Subset, Constant> subsetToConstant;
    private final Map<Constant, Subset> constantToSubset;
    private final EntailmentSetting setting;

    private final int theorySimplifierSubsumption = Matching.THETA_SUBSUMPTION;
    private final int theorySolverMode = TheorySolver.CUTTING_PLANES;
    private final int theorySolverSubsumption = Matching.THETA_SUBSUMPTION;

    // query part, most of the things are for speed-up only
    private final Set<Literal> queries;
    private final boolean queriesMode;
    private final Set<Subset> queryTargetConstants;

    public Entailment(Set<Literal> evidence, Set<Constant> constants, EntailmentSetting setting, boolean verbose,
                      MultiMap<Constant, Literal> constantToLiterals,
                      Map<Constant, Subset> constantToSubset,
                      Set<Subset> subsetConstants,
                      Map<Subset, Constant> subsetToConstant,
                      Map<Integer, MultiMap<Subset, Literal>> subsetToLiterals,
                      Set<Literal> queries) {
        this.constants = constants;
        this.evidence = evidence;
        this.setting = setting;
        this.constantToLiterals = constantToLiterals;
        this.constantToSubset = constantToSubset;
        this.subsetToLiterals = subsetToLiterals;
        this.subsetConstants = subsetConstants;
        this.subsetToConstant = subsetToConstant;
        this.verbose = verbose;
        this.queries = queries;
        this.queriesMode = null != queries;
        this.queryTargetConstants = (null == queries) ? Sugar.set() : queries.stream().map(LogicUtils::constantsFromLiteral).distinct().map(set -> constantsToSubset(set, constantToSubset)).collect(Collectors.toSet());
    }

    public Entailed entails(Theory theory, boolean parallel, Predicate allowedPredicate, java.util.function.Function<Integer, Path> storingLevelByLevel) {
        Subset all = SubsetFactory.getInstance().get(constants.size(), 0);
        for (Subset s : subsetConstants) {
            all = all.union(s);
        }
        Set<Subset> initial = Sugar.set(all);
        int k = all.size();
        return entails(theory, k, initial, parallel, allowedPredicate, storingLevelByLevel);
    }

    public Entailed entails(Theory theory, int k, boolean parallel, Predicate allowedPredicate, Function<Integer, Path> storingLevelByLevel) {
        Set<Subset> initial = setting.initialByScout()
                ? scoutSubset(theory.allRules(), k, this.evidence).stream().map(this::constantsToSubset).collect(Collectors.toSet())
                : subsetConstants;
        return entails(theory, k, initial, parallel, allowedPredicate, storingLevelByLevel);
    }

    private Subset constantsToSubset(Set<Constant> elements) {
        Subset retVal = SubsetFactory.getInstance().get(constants.size());
        for (Constant element : elements) {
            retVal = retVal.union(constantToSubset.get(element));
        }
        return retVal;
    }

    private Subset constantsToSubset(Set<Constant> elements, Map<Constant, Subset> constantToSubset) {
        Subset retVal = SubsetFactory.getInstance().get(constants.size());
        for (Constant element : elements) {
            retVal = retVal.union(constantToSubset.get(element));
        }
        return retVal;
    }


//    Pair<Boolean, Boolean> debugPair = new Pair<>(false, false);

    private Entailed entails(Theory theory, final int k, Set<Subset> initial, boolean parallel, Predicate allowedPredicate, Function<Integer, Path> storingLevelByLevel) {
//        Set<String> debug = Sugar.set("Post_Quals(1:person217)", "Post_Quals(1:person155)", "Faculty(1:person101)", "Post_Quals(1:person391)");
//        Subset debugSubset = constantsToSubset(LogicUtils.constantsFromLiteral(Literal.parseLiteral("p(1:person235, 1:person402, 3:title108)")));

        int theoryMode = mode(theory);
        // for classical FOL entailment this trick can be done for sure; check whether it can be done for PL as well
        boolean canSkipProving = theory instanceof FOL && HORN_ONLY == theoryMode; // this version is safe for sure, ned change inside findConsistentModelTheory....(HORN_ONLY == theoryMode || AT_MOST_HORN == theoryMode) && theory instanceof FOL;
        boolean allPredicatesAllowed = null == allowedPredicate;

        if (!allPredicatesAllowed && setting.useMegaScout()) {
            throw new IllegalStateException("cannot compute v/k-entailment for only one predicate with mega scout setting");
        } else if (setting.useMegaScout() && setting.useScout()) {
            throw new IllegalStateException("cannot compute with mega scout and scout at the same time");
        }

//        int theoryHash = theory.allRules().stream().map(Clause::toString).sorted().collect(Collectors.joining(" ")).hashCode();
//        int evidenceHash = this.evidence.stream().map(Literal::toString).sorted().collect(Collectors.joining(" ")).hashCode();
//        System.out.println("debug\t1" + theoryHash + "\t" + evidenceHash);

//        System.out.println("debug 1 \t" + theoryMode + "\t" + this.evidence.size() + "\t" + this.evidence); debug PL

        System.out.println("reporting initial\t" + initial.size());
        if (verbose) { // || true
            initial.forEach(s -> System.out.println(canon(s) + "\t" + s.toString()));
        }

        if (setting.entailmentMode() != V_ENTAILMENT && setting.entailmentMode() != K_ENTAILMENT && setting.entailmentMode() != All_EVIDENCE) {
            throw new IllegalStateException("unknown entailment mode");
        }

        boolean allEvidenceOnly = setting.entailmentMode() == All_EVIDENCE;

        Entailed entailed = setting.entailmentMode() == V_ENTAILMENT
                ? (!allEvidenceOnly && null != storingLevelByLevel ? LevelCollector.create(this.evidence, k, theory.allRules().stream().flatMap(clause -> clause.literals().stream()).mapToInt(Literal::arity).max().orElse(0))
                : VECollector.create(this.evidence, k, theory.allRules().stream().flatMap(clause -> clause.literals().stream()).mapToInt(Literal::arity).max().orElse(0))
        )
                : KECollector.create(this.evidence);

        Set<Subset> atomicElements;
        if (allEvidenceOnly) {
            atomicElements = Sugar.set();
//            k is set to number of constants
//            if (0 != k) {
//                throw new IllegalStateException("k should be 0 for allEvidence setting");
//            }
        } else if (setting.isDataDrivenApproach()) {
            atomicElements = mineAtomicSubsets(this.subsetToLiterals, k);
        } else {
            atomicElements = this.subsetConstants;
        }

        System.out.println("baseElements\t" + atomicElements.size());
        if (verbose) {
            atomicElements.forEach(s -> System.out.println("\t" + canon(subsetToConst(s))));
        }
        int maximalBaseCardinality = atomicElements.stream().mapToInt(Subset::size).max().orElse(1);
        MultiMap<Integer, MutableNode> queue = new MultiMap<>(MultiMap.CONCURENT_HASH_SET);

        // init via hint, needable... use baseElements instead for potentially bigger space
        initial.forEach(subset -> queue.put(subset.size(), createNode(subset, theory, Sugar.set(), Sugar.set(), true)));

        //Map<Theory, Set<Literal>> entailedByTheory = prepareMegaScout(theory);

        List<Subset> clashMakers = Sugar.list(); // it is only to be traversed; no duplicities will occur
        Map<Integer, Map<Subset, Set<Literal>>> maskCache = new HashMap<>();
        // init it, due to synchronization overhead
        IntStream.range(1, constants.size() + 1).forEach(size -> maskCache.put(size, new ConcurrentHashMap<>()));
//        System.out.println("debug -> parallel set to false");
        for (int level = allEvidenceOnly ? Sugar.chooseOne(initial).size() : 1; level <= k; level++) {
//            if (!queue.containsKey(level)) { before on the fly storing was introduced
//                continue;
//            }
            if (queue.containsKey(level)) {

                Set<MutableNode> currentQueue = queue.get(level);
                System.out.println("computing level\t" + level + "\twith candidates\t" + currentQueue.size());
                int finalLevel = level;

                List<Pair<Subset, Stream<MutableNode>>> candidates = Sugar.parallelStream(currentQueue, parallel)
                        .map(mutableNode -> {
                            Subset subset = mutableNode.getConstants();

                            for (Subset clashMaker : clashMakers) { // protoze nektere fragmenty mohou byt pridany pri inicializaci, ale
                                if (clashMaker.isSubsetOf(subset)) {
                                    return null;
                                }
                            }


                            Set<Literal> proved = Sugar.set(); // set of entailed literals of within this step
                            Theory currentTheory = mutableNode.getTheory();
                            boolean theoryUnchanged = false;

//                            boolean soutDebug = debugSubset.isSubsetOf(subset) && subset.isSubsetOf(debugSubset);
//                            this.debugPair.r = soutDebug;
//                            boolean soutDebug = false;
//                            if (soutDebug) {
//                                System.out.println("jsem tady v tom:\t" + canon(subset) + "\n" + mask(subset, maskCache) + "\n" + mutableNode.entailedByAncestor());
//                            }

                            boolean inconsistentTheory = false;
                            if (mutableNode.checkConsistency() || setting.cuttingPlanesRelaxation()) {
                                // TODO proc je tady this.evidence v tom unionu? asi by tam melo byt neco jineho ;))
                                GroundTheorySolver inconsistentChecker = new GroundTheorySolver(mutableNode.getTheory().allRules(),
                                        //        Sugar.union(this.evidence, mutableNode.entailedByAncestor())
                                        Sugar.union(mask(subset, maskCache), mutableNode.entailedByAncestor())
                                );
                                inconsistentTheory = null == inconsistentChecker.solve();
                                if (inconsistentTheory && currentTheory instanceof ClassicalLogicNode) {
                                    return new Pair<>(mutableNode.getConstants(), null);
                                }
                            }

                            if (mutableNode.evaluate() || inconsistentTheory) {
                                Set<Literal> currentEvidence = mask(subset, maskCache);
                                if (!currentEvidence.isEmpty()) {
                                    Pair<? extends Theory, Set<Literal>> pair = findConsistentTheoryAndProveable(mutableNode.getTheory(), currentEvidence, mutableNode.entailedByAncestor(), theoryMode, allEvidenceOnly);
//                                    if (soutDebug) {
//                                        System.out.println("tady, tady");
//                                    }

                                    if (null == pair) { // this C' \cup \Phi is inconsistent no matter which (soft, non-hard rules, etc.) rules are removed
                                        return new Pair<>(subset, null);
                                    }

//                                    if (soutDebug) {
//                                        System.out.println("debug3\n" + pair.getS().size() + "\n" + pair.s + "\n" + (pair.r.allRules().size() - pair.r.getHardRules().size()) + "\t" + (mutableNode.getTheory().allRules().size() - mutableNode.getTheory().getHardRules().size()));
//                                    }

                                    currentTheory = pair.r;
                                    Set<Clause> merged = Sugar.union(currentTheory.allRules(), currentEvidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()));
                                    theoryUnchanged = currentTheory.size() == mutableNode.getTheory().size();
                                    Set<Literal> hints = pair.s;

//                                    if (soutDebug) {
//                                        System.out.println("hints are\n" + hints.stream().map(Object::toString).collect(Collectors.joining("\n\t")));
//                                    }
//                                    System.out.println("hints are\n" + hints); debug PL

                                    boolean finalTheoryUnchanged = theoryUnchanged;
                                    proved.addAll(Sugar.parallelStream(hints, parallel)
                                            .filter(query -> !this.evidence.contains(query)
                                                    && proveQuery(query, finalLevel, k)
                                                    && (!allEvidenceOnly || (allEvidenceOnly && (allPredicatesAllowed || (!allPredicatesAllowed && allowedPredicate.getName().equals(query.predicate()) && allowedPredicate.getArity() == query.arity())))) // speed-up hack for case that standard logic is used and only predicate for one entailment is computed
                                                    && !isEntailedByAncestor(query, mutableNode, finalTheoryUnchanged) // if entailed by direct ancestor, then do not solve; otherwise need to be tested
                                                    && (canSkipProving // e.g. if we have only horn rules findConsistentTheoryAndProveable returns the least Herbrand model, thus we do not have to test them once more; in the of having constraints as well the latter method returns null if any of them are violated
                                                    || setting.cuttingPlanesRelaxation()
                                                    || TheorySimplifier.isGroundLiteralImplied(query, merged, theorySimplifierSubsumption, theorySolverMode))
                                            )
                                            .collect(Collectors.toList()));

                                    for (Literal query : proved) {
                                        if ((allPredicatesAllowed || (allowedPredicate.getName().equals(query.predicate()) && allowedPredicate.getArity() == query.arity())) // not a nice hack around
                                                && needsToBeAddedToEntailed(query, mutableNode)) {
//                                            System.out.println("addt\t" + query); debug PL
//                                            if (debug.contains(query.toString())) {
//                                                System.out.println("adding\t" + query + "\t" + canon(subset));
//                                            }
//                                            if (soutDebug) {
//                                                System.out.println("I'm entailing\t" + query);
//                                            }
                                            entailed.add(query, subset);
                                        }
                                    }

                                }

                                currentEvidence = null; // GC
                            } // no other else here, should be done beforehand

                            // System.out.println(canon(subset) + "\t" + proved.size() + "\t" +  canon(proved));

                            if (subset.size() >= k) {// prune the successors, they won't be evaluated, so do not generate them
                                return null;
                            }

                            // this is important
                            Set<Literal> entailedByDirectAncestor = theoryUnchanged ? Sugar.union(mutableNode.entailedByAncestor(), proved) : proved; // by the ancestor and this node itself
                            Theory finalTheory = currentTheory;

                            Stream<Triple<Subset, Set<Literal>, Boolean>> successorsWithMask = null;
                            if (setting.useMegaScout()) {
                                // how can we do this in parallel???? is there another option -- e.g. processing nodes in parallel???

                            /*List<Pair<Clause, Matching>> worlds = currentTheory.allRules().stream()
                                    .map(rule -> new Pair<>(rule, Matching.get(new Clause(Sugar.union(evidence,entailedByDirectAncestor)), Matching.THETA_SUBSUMPTION)))
                                    .collect(Collectors.toList());

                            successorsWithMask =
                                    Sugar.parallelStream(worlds, parallel)
                                            .flatMap(pair -> {
                                                Clause rule = pair.getR();
                                                Matching world = pair.getS();
                            */
                                // sequential version now because of
                                Matching world = Matching.create(new Clause(Sugar.union(evidence, entailedByDirectAncestor)), Matching.THETA_SUBSUMPTION);
                                successorsWithMask = currentTheory.allRules().stream()
                                        .flatMap(rule -> {
                                            Pair<Term[], List<Term[]>> substitutions = world.allSubstitutions(LogicUtils.flipSigns(rule), 0, Integer.MAX_VALUE);
                                            Set<Subset> refinements = Sugar.set();
                                            for (Term[] subs : substitutions.s) {
                                                Set<Constant> set = Sugar.set();
                                                for (int idx = 0; idx < subs.length; idx++) {
                                                    set.add((Constant) subs[idx]); // if exception here, then some bug in input, since all should be variable free
                                                }

                                                // here is the place for possible better bounds, e.g. by checking some kind of dependency graph (of entailed literals)
                                                Subset refinement = mutableNode.getConstants().union(constantsToSubset(set));
                                                if (refinement.size() <= k) {
                                                    refinements.add(refinement);
                                                }
                                            }

                                            return refinements.stream();
                                        })
                                        .distinct()
                                        .filter(refinement -> {
                                            if (queriesMode && refinement.size() == k) {
                                                return this.queryTargetConstants.stream().anyMatch(target -> refinement.contains(target));
                                            }
                                            return true;
                                        })
                                        .map(refinement -> new Triple<>(refinement, mask(refinement, maskCache), true));

                            } else {
                                // this stream can be parallel since    1) each children is different
                                //                                      2) nodes are processed sequentially
                                successorsWithMask = Sugar.parallelStream(atomicElements, parallel)
                                        .map(c -> subset.union(c))
                                        .filter(refinement -> refinement.size() > subset.size()
                                                && refinement.size() <= k
                                                && noClash(refinement, clashMakers))
                                        .map(refinement -> {
                                            Set<Literal> maskedEvidence = mask(refinement, maskCache);
                                            boolean needToEvaluate = true;
                                            if (setting.useScout()) {
                                                Set<Literal> currentEvidence = Sugar.union(entailedByDirectAncestor, maskedEvidence);
                                                Set<Literal> possibleProved = scout(finalTheory.allRules(), k, currentEvidence);
                                                needToEvaluate = !currentEvidence.containsAll(possibleProved);
                                            }
                                            return new Triple<>(refinement, maskedEvidence, needToEvaluate);
                                        });

                        /* this is alternative way of implementation of the same thing; less modular, little bit more memory friendly
                        if (setting.useScout()) {
                            successorsWithMask = successorsWithMask.map(triple -> {
                                // triple.s -- evidence masked by this subset
                                Set<Literal> currentEvidence = Sugar.union(finalProved, triple.s);
                                Set<Literal> possibleProved = scout(finalTheory.allRules(), finalK, currentEvidence);
                                boolean needToEvaluate = !currentEvidence.containsAll(possibleProved);
                                return new Triple<>(triple.r, triple.s, needToEvaluate);
                            });
                        }
                        */
                            }

                            Set<Literal> entailedByAllSuccessors = Sugar.union(mutableNode.entailed(), proved);
                            Stream<MutableNode> successors = successorsWithMask
                                    .map(triple -> createNode(triple.r, finalTheory, entailedByDirectAncestor, entailedByAllSuccessors, triple.t));
                            // optionally .collect(Collectors.toList()), in case it causes some speed, memory, etc., issues

                            return new Pair<>(null // not null only in case of this subset causes inconsistency
                                    , successors);
                        })
                        .filter(p -> null != p) // p == null iff we are in the last layer
                        .map(pair -> (Pair<Subset, Stream<MutableNode>>) pair)
                        .collect(Collectors.toList()); // we want to evaluate nodes at parallel, but merging their children is done in sequential


                Set<Subset> forbidden = ConcurrentHashMap.newKeySet();

                // can be dealt without the pair.r null by creation of some wrapper we would ask different question
                candidates.forEach(pair -> {
                    if (null == pair.r) { // process children
                        pair.s
                                .collect(Collectors.toList()).stream() // without this, the old version (most likely) behaves non-deterministically
                    /*
                    uzel {3, 89, 291, 533, 810}	85995762	complex_id_140020020, location_id_730001, protein_YDR129c, protein_YIR006c, protein_YOR181w
                    je ukázkou toho, že je problém při strkání uzlů do queue, proto

                    generating	85995762	{3, 89, 291, 533, 810}	complex_id_140020020, location_id_730001, protein_YDR129c, protein_YIR006c, protein_YOR181w
                    from	85995738	{3, 89, 533, 810}	complex_id_140020020, location_id_730001, protein_YIR006c, protein_YOR181w
                    entailed	location(protein_YOR181w, location_id_730001)
                    proved	location(protein_YOR181w, location_id_730001)

                    by měl mít jako entailed také location(protein_YOR181w, location_id_730001)

                    ale při expanzi uzlu nastane

                    {3, 89, 291, 533, 810}	85995762	complex_id_140020020, location_id_730001, protein_YDR129c, protein_YIR006c, protein_YOR181w
                    needs to be added	true
                    entailed by ancestor	false
                    entailed by ancestor: [location(protein_YDR129c, location_id_730001)]
                    entailed	false
                    entailed: [location(protein_YDR129c, location_id_730001)]

                    kdyz se ptame jestli to obsahuje location(protein_YOR181w, location_id_730001), coz by melo mergem uzlu 85995762 nastat... kdyz se udela collector(Collectors.toList()).stream() tak to funguje jak ma (alespon testovano pri 2k bezich)
                    */
                                .forEach(mutableNode -> queue.put(mutableNode.getConstants().size(), mutableNode));
                    } else { // pair.r is the subset causing inconsistency
                        forbidden.add(pair.r);
                    }
                });

                clashMakers.addAll(forbidden);
                // GC
                queue.remove(level);
                maskCache.remove(level);
                if (level > maximalBaseCardinality) {
                    SubsetFactory.getInstance().clear(level);
                }
            }

            if (null != storingLevelByLevel && !allEvidenceOnly) { // store now
                // we need to iterate trough the k-level, else some might be skippend and would not be written to the results
                Path levelOutput = storingLevelByLevel.apply(level);
                try {
                    Files.write(levelOutput, Sugar.list(entailed.removeK(level).asOutput()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        System.out.println("inconsistent found\t" + clashMakers.size());
//        System.out.println("forbidden");
//        clashMakers.stream().map(this::canon).sorted().forEach(c -> System.out.println("\t" + c));
        return entailed;
    }

    // returns false only we are computing the layer k and the query is in the test queries set
    private boolean proveQuery(Literal query, final int currentLevel, final int k) {
        if (!queriesMode || currentLevel != k) {
            return true;
        }
        return queries.contains(query);
    }

    private int mode(Theory theory) {
        Triple<Set<Clause>, Set<Clause>, Set<Clause>> triple = theory.getConstrainsHornOthers();
        if (triple.t.size() > 0) {
            return MORE_THAN_HORN;
        } else if (triple.r.isEmpty()) {
            return HORN_ONLY;
        }
        return AT_MOST_HORN;
    }

    private boolean isEntailedByAncestor(Literal literal, MutableNode mutableNode, boolean theoryUnchanged) {
        if (setting.useClassLogic()) {
            return mutableNode.entailed().contains(literal);
        } else if (setting.usePossLogic()) {
            if (theoryUnchanged) {
                return mutableNode.entailedByAncestor().contains(literal);
            }
            // if the theory was changed, then we cannot rely on what was entailed by its direct predecessor
            return false;
        }
        throw new IllegalStateException();// NotImplementedException();
    }

    private boolean needsToBeAddedToEntailed(Literal literal, MutableNode mutableNode) {
        if (V_ENTAILMENT == setting.entailmentMode()) {
            return !mutableNode.entailed().contains(literal);
        } else if (K_ENTAILMENT == setting.entailmentMode()) { // adding in this case does not hurt anything
            return true;
        } else if (All_EVIDENCE == setting.entailmentMode()) {
            return true;
        }
        throw new IllegalStateException(); //NotImplementedException();
    }

    // return null iff removing all soft rules cannot make it consistent (PL case) or the theory is inconsistent with the evidence (FOL case)
    private Pair<? extends Theory, Set<Literal>> findConsistentTheoryAndProveable(Theory theory, Set<Literal> currentEvidence, Set<Literal> entailedByDirecAncestor, int theoryMode, boolean allEvidenceOnly) {
        // maybe there is som bug there
        /*if ((!allEvidenceOnly && theory instanceof FOL && (AT_MOST_HORN == theoryMode || HORN_ONLY == theoryMode))
                || (allEvidenceOnly && theory instanceof FOL && HORN_ONLY == theoryMode)) {
            return findHerbrandModel(theory, Sugar.union(currentEvidence, entailedByDirecAncestor));
        }*/ // this condition here needs to be consistent with the saying canSkipProving
        if (theory instanceof FOL && HORN_ONLY == theoryMode) { // this is safe for sure ;)
            return findHerbrandModel(theory, Sugar.union(currentEvidence, entailedByDirecAncestor));
        } // horn_only means definite rules (and may be no constraints)

        TheorySolver solver = new TheorySolver();
        solver.setSubsumptionMode(theorySolverSubsumption);
        solver.setMode(theorySolverMode);
//        TypedTheorySolver solver = new TypedTheorySolver(TypedTheorySolver.CUTTING_PLANES, Matching.THETA_SUBSUMPTION); // thole by ale melo byt parametrizovane theorySolverSubsumption a theorySolverMode

//        solver.setDebug = this.debugPair.r;

        // dokazane literaly se pridaji do evidence pro zrychleni vypoctu; pri PL explicitne bokem protoze se to bude muset resit zvlast
        if (theory instanceof FOL) {
            return resolveFOLTheory(theory, Sugar.union(currentEvidence, entailedByDirecAncestor), solver);
        } else if (theory instanceof Possibilistic) {
            return resolvePLTheory((Possibilistic) theory, currentEvidence, entailedByDirecAncestor, solver);
        }
        throw new IllegalStateException("implementations exist only for classical and possibilistic theory");
    }

    private Pair<? extends Theory, Set<Literal>> findHerbrandModel(Theory theory, Set<Literal> evidence) {
        LeastHerbrandModel modelFinder = new LeastHerbrandModel();
        Triple<Set<Clause>, Set<Clause>, Set<Clause>> triple = theory.getConstrainsHornOthers();
        Set<Clause> constraints = triple.r;
        Set<Clause> horn = triple.s;
        Set<Literal> model = modelFinder.herbrandModel(horn, evidence);

        if (constraints.size() > 0) {
            Matching check = Matching.create(new Clause(model), Matching.THETA_SUBSUMPTION);
            for (Clause constraint : constraints) {
                if (check.subsumption(LogicUtils.flipSigns(constraint), 0)) {
                    return null;
                }
            }
        }

        return new Pair<>(theory, model);
    }

    private Pair<? extends Theory, Set<Literal>> resolvePLTheory(Possibilistic theory, Set<Literal> currentEvidence, Set<Literal> entailedByDirectAncestor, TheorySolver solver) {
//    private Pair<? extends Theory, Set<Literal>> resolvePLTheory(Possibilistic theory, Set<Literal> currentEvidence, Set<Literal> entailedByDirectAncestor, TypedTheorySolver solver) {
        List<Clause> rules = Sugar.list();
        rules.addAll(theory.getHardRules());
        int baseRules = rules.size();
        int lowerBound = rules.size(); // for now, just hard rules
        theory.getSoftRules().stream().forEach(p -> rules.add(p.s));
        int upperBound = rules.size();

        // TODO can we use somehow entialedByDirectAncestor here? e.g. for the some provable testing?????
        //Set<Literal> proveable = solver.solve(rules.subList(0, upperBound), Sugar.union(currentEvidence,entailedByDirectAncestor));


        // big todo here:
//        System.out.println("recall that here we can use solver.solvePreprocessed");
        Set<Literal> proveable = solver.solve(rules.subList(0, upperBound), currentEvidence);
        if (null != proveable) {
            return new Pair<>(theory, proveable);
        }

        // big todo here:
//        System.out.println("note that here we can call solver.solvePreprocessed");
        proveable = solver.solve(rules.subList(0, lowerBound), currentEvidence); // at least all hard rules (change lowerBound bound the minimal number of soft rules)
        if (null == proveable) { // even hard rules make it inconsistent
            return null;
        }

        // lower bound -- is always consistent
        // upper bound -- we do not know, but not need to be tested
        // if lower == upper, then end, it is consistent
        Set<Literal> lowerBoundProved = proveable;
        while (true) {
//            System.out.println(lowerBound + "\t\t" + upperBound); debug PL
            if (lowerBound + 1 == upperBound) {
                return new Pair<>(Possibilistic.create(theory.getHardRules(), theory.getSoftRules().subList(0, lowerBound - baseRules))
                        , lowerBoundProved);
            }

            int mid = lowerBound + (upperBound - lowerBound) / 2;

            if (mid == lowerBound) {
                mid = upperBound;
            }

//            solver.setDebug = this.debugPair.r;
            proveable = solver.solve(rules.subList(0, mid), currentEvidence);
//            System.out.println("\t" + mid);
            if (null == proveable) {
                // lower bound stays the same; upper is lowered
                upperBound = mid;
            } else {
                // lower bound is increased; upper stays the same
                lowerBound = mid;
                lowerBoundProved = proveable;
            }
        }
    }

    private Pair<Theory, Set<Literal>> resolveFOLTheory(Theory theory, Set<Literal> currentEvidence, TheorySolver solver) {
//    private Pair<Theory, Set<Literal>> resolveFOLTheory(Theory theory, Set<Literal> currentEvidence, TypedTheorySolver solver) {
        Set<Literal> proveable = solver.solve(theory.allRules(), currentEvidence);
        if (null == proveable) {
            return null;
        }
        return new Pair<>(theory, proveable);
    }

    private Set<Literal> scout(Set<Clause> theory, int k, Iterable<Literal> maskedEvidence) {
        return scoutPossible(theory, k, maskedEvidence, false).r;
    }

    private Set<Set<Constant>> scoutSubset(Set<Clause> theory, int k, Iterable<Literal> maskedEvidence) {
        // here can be speed up
        return scoutPossible(theory, k, maskedEvidence, false).s;
    }

    private Pair<Set<Literal>, Set<Set<Constant>>> scoutPossible(Set<Clause> theory, int k, Iterable<Literal> maskedEvidence, boolean onlyHorns) {
        //List<Literal> allConstants = Sugar.list(new Literal("", true, Sugar.listFromCollections(constants)));
        Clause evidence = new Clause(maskedEvidence);
        Matching world = Matching.create(evidence, Matching.THETA_SUBSUMPTION);
        Set<Set<Constant>> subsets = Sugar.set();
        Set<Literal> violatedHeads = Sugar.set();

        for (Clause rule : theory) {
            Literal posLit = Sugar.chooseOne(LogicUtils.positiveLiterals(rule));
            if (onlyHorns && null == posLit) {
                continue;
            }

            Pair<Term[], List<Term[]>> substitutions = world.allSubstitutions(LogicUtils.flipSigns(rule), 0, Integer.MAX_VALUE);
            for (Term[] subs : substitutions.s) {
                Set<Constant> set = Sugar.set();
                for (int idx = 0; idx < subs.length; idx++) {
                    set.add((Constant) subs[idx]); // if exception here, then some bug in input, since all should be variable free
                }
                if (set.size() <= k) {
                    if (null != posLit) {
                        violatedHeads.add(LogicUtils.substitute(posLit, substitutions.r, subs));
                    }
//                    else{
//                        System.out.println("wierd here, not?\t" + rule);
//                    }
                    subsets.add(set);
                }
//                System.out.println(LogicUtils.substitute(rule, substitutions.r, subs) + "\t" + (set.size() <= k));
            }
        }
        return new Pair<>(violatedHeads, subsets);
    }


    private Set<Literal> mask(Subset subset, Map<Integer, Map<Subset, Set<Literal>>> cache) {
        Map<Subset, Set<Literal>> layer = cache.get(subset.size());
//        if (null == layer) {
//            synchronized (cache) {
//                layer = cache.get(subset.size());
//                if (null == layer) {
//                    layer = new HashMap<>();
//                    cache.put(subset.size(), layer);
//                }
//            }
//        }

        Set<Literal> mask = layer.get(subset);
        // does it make sense to give here synchronization?
        if (null == mask) {
            mask = mask(subset);
            layer.put(subset, mask);
        }
        return mask;
    }

    private Set<Literal> mask(Subset subset) {
//        Set<Literal> retVal = Sugar.set();
//        for (Map.Entry<Integer, MultiMap<Subset, Literal>> layer : subsetToLiterals.entrySet()) {
//            layer.getValue().entrySet().stream()
//                    .filter(entry -> entry.getKey().isSubsetOf(mask))
//                    .forEach(entry -> entry.getValue().forEach(retVal::add));
//        }
//        return retVal;
        return Sugar.parallelStream(subsetToLiterals.entrySet(), parallel)
                .flatMap(layer -> layer.getValue().entrySet().stream())
                .filter(entry -> entry.getKey().isSubsetOf(subset))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet());
    }

    private MutableNode createNode(Subset subset, Theory theory, Set<Literal> impliedByDirectAncestor, Set<Literal> implied, boolean needToEvaluate) {
        if (setting.usePossLogic()) {
            return PossLogicNode.create(subset, theory, impliedByDirectAncestor, implied, needToEvaluate);
        }
        return ClassicalLogicNode.create(subset, theory, impliedByDirectAncestor, implied, needToEvaluate);
    }

    /**
     * returns set of subsets which are minimal in the sense that they cannot be splitted to multiple subsets, each one of them having non empty mask
     *
     * @param subsetToLiterals
     * @param k
     * @return
     */
    private Set<Subset> mineAtomicSubsets(Map<Integer, MultiMap<Subset, Literal>> subsetToLiterals, int k) {
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
        return null != union && s.size() == union.size();
    }

    private boolean noClash(Subset subset, Collection<Subset> forbidden) {
        // experiment, compare, try... speed by using following instead
        // return Sugar.parallelStream(forbidden,true).anyMatch(s -> s.isSubsetOf(subset));
        for (Subset clashMaker : forbidden) {
            if (clashMaker.isSubsetOf(subset)) {
                return false;
            }
        }
        return true;
    }

    private String canon(Set<? extends Object> constants) {
        return constants.stream()
                //.map(Constant::name)
                .map(Object::toString)
                .sorted()
                .collect(Collectors.joining(", "));
    }

    private Set<Constant> subsetToConst(Subset candidate) {
        return subsetToConstant.entrySet().stream()
                .filter(entry -> entry.getKey().isSubsetOf(candidate))
                .map(entry -> entry.getValue())
                .collect(Collectors.toSet());
    }

    private String canon(Subset s) {
        return canon(subsetToConst(s));
    }


    // TODO: udelat pro query dotazy verzi taky

    /**
     * evidence is supposed to be ground
     *
     * @param evidence
     * @param setting
     * @return
     */
    public static Entailment create(Collection<Literal> evidence, EntailmentSetting setting) {
        return settingCreator(evidence, setting, null);
    }

    private static Entailment settingCreator(Collection<Literal> evidence, EntailmentSetting setting, Set<Literal> queries) {
        Set<Constant> evidenceConstant = evidence.stream().flatMap(l -> l.terms().stream()).map(t -> (Constant) t).collect(Collectors.toSet());
        MultiMap<Constant, Literal> constantsToLiterals = new MultiMap<>();
        // change to one line stream
        evidence.forEach(literal -> LogicUtils.constantsFromLiteral(literal).forEach(constant -> constantsToLiterals.put(constant, literal)));
        Quadruple<Set<Subset>, Map<Integer, MultiMap<Subset, Literal>>, Map<Subset, Constant>, Map<Constant, Subset>> bitsetRepre = createBitsetRepresentation(evidence);

        Set<Subset> subsetConst = bitsetRepre.r;
        Map<Integer, MultiMap<Subset, Literal>> subsetToLits = bitsetRepre.s;
        Map<Subset, Constant> subsetToConst = bitsetRepre.t;
        Map<Constant, Subset> constantsToSubs = bitsetRepre.u;

        return new Entailment(Sugar.setFromCollections(evidence), evidenceConstant, setting, false
                , constantsToLiterals
                , constantsToSubs, subsetConst, subsetToConst, subsetToLits
                , queries);
    }

    private static Quadruple<Set<Subset>, Map<Integer, MultiMap<Subset, Literal>>, Map<Subset, Constant>, Map<Constant, Subset>> createBitsetRepresentation(Collection<Literal> evidence) {
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

    public static Entailment createHitsInterference(Set<Literal> evidence, EntailmentSetting entailmentSetting, Set<Literal> queries) {
        return settingCreator(evidence, entailmentSetting, queries);
    }
}
