package ida.pacReasoning.supportEntailment;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialBinaryPredicates;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.pacReasoning.entailment.collectors.KECollector;
import ida.pacReasoning.supportEntailment.speedup.BSet;
import ida.pacReasoning.supportEntailment.speedup.BSupport;
import ida.pacReasoning.supportEntailment.speedup.FastSubsetFactory;
import ida.utils.Sugar;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Step-by-step with prune cartesian which would be repetitive (applicable only with change of supports in a single literal).
 * <p>
 * Created by martin.svatos on 15. 10. 2018.
 */
public class SupportsHolderOpt10ForwardMoreThanOneFix {


    private final Map<Constant, BSet> constantToSubset;
    private final Map<Constant, Integer> constantToInteger;
    private final List<Constant> evidenceConstants;
    private final Map<Set<Constant>, BSet> constantsToSubset;
    private final Set<Literal> evidence;
    private final Map<Literal, Set<BSupport>> map;
    private final int k;
    private final Map<BSet, BSupport> supportCache;
    private final Set<BSupport> forbidden;

    private final Map<Pair<String, Integer>, Integer> predicatesToIdx;
    private final boolean useExtraPruning;
    private final boolean evidenceBased;
    private final List<Clause> negatedConstraints;
    // ? private final boolean allPreviousOnly;
    private BitSet changedPredicatesInLastIteration;
    private Set<BSupport> supportsOfBrandNewHeads;
    private long sizeOfSupports = 0;
    private int sizeOfWorld;
    private final boolean parallel = true;
    private boolean change;
    private Map<Literal, Set<BSupport>> changedLiteralsInLastIteration;
    private Set<Literal> brandNewHeads;
    // dev
    public int forbiddenBInvocatioins = 0;

    public long forbiddenBCallsWithin = 0;
    public int forbiddenBStops = 0;
    private boolean oldVersion = true;
    public static boolean debugNow = false;

    // speeding up?
    private final Map<Integer, Set<BSupport>> forbiddens;

    private long forbiddensSize;
    private long cannotSkip = 0l;
    private long ones = 0;
    private long carts;

    {
        System.out.println("old version\t" + this.oldVersion + "\t ! nová verze ale musí být porovnaná na menších datech jestli dává stejné výsledky!");
    }

    {
        // dev verze pro minimal-forward pass (zatim bez constraint)
    }

    public SupportsHolderOpt10ForwardMoreThanOneFix(int k, Set<Literal> evidence, Map<Constant, BSet> constantToSubset, List<Constant> evidenceConstants, Map<Literal, Set<BSupport>> map, Map<BSet, BSupport> supportCache, Map<Pair<String, Integer>, Integer> predicateToIdx, boolean useExtraPruning, Map<Constant, Integer> constantToInteger, List<Clause> negatedConstraints) {
        this.k = k;
        this.evidence = evidence;
        this.constantToInteger = constantToInteger;
        this.constantToSubset = constantToSubset;
        this.evidenceConstants = evidenceConstants;
        this.constantsToSubset = new HashMap<>();
        this.map = map;
        this.supportCache = supportCache;
        this.forbidden = ConcurrentHashMap.newKeySet();
        this.predicatesToIdx = predicateToIdx;
        this.changedLiteralsInLastIteration = new HashMap<>();
        this.changedPredicatesInLastIteration = new BitSet(predicateToIdx.size());
        this.useExtraPruning = useExtraPruning;
        this.brandNewHeads = Sugar.set();
        // testing of this combination was slower on UMLS than without it
        // ? this.allPreviousOnly = false;//System.getProperty("devMod").contains("APO");

        this.negatedConstraints = negatedConstraints;

        this.forbiddensSize = 0l;
        this.forbiddens = new ConcurrentHashMap<>();
        this.evidenceBased = true;
        System.out.println("forbid derivation of evidence\t" + this.evidenceBased);
    }

    // jen jeden pass!
    public void forwardRules(List<Triple<Clause, Literal, BSet>> rulesWithNegated, long iteration, Set<Triple<Clause, Literal, BSet>> newlyAddedRules) {
        // tyhle nejspise budou muset byt inicializovane nekde uvnitr
        System.out.println("map\t" + map.keySet().size() + "\t"
                + map.values().stream().flatMap(Collection::stream).distinct().count() + "\t"
                + map.values().stream().mapToInt(Collection::size).sum());
        System.out.println(cannotSkip + "\t" + ones + "\t" + (1.0 * ones / cannotSkip));
        this.carts = 0l;
        MultiMap<Literal, BSupport> globalChanges = new MultiMap<>();
        BitSet globalChangedPredicates = new BitSet(predicatesToIdx.size());
        Matching world = Matching.create(new Clause(map.keySet()), Matching.THETA_SUBSUMPTION);

//        String debugStr = "manages(health_care_related_organization, group)";//"manages(health_care_related_organization, disease_or_syndrome)";
        String debugStr = "";//"manages(health_care_related_organization, disease_or_syndrome)";
//        String debugStr = "prevents(medical_device, cell_or_molecular_dysfunction)";

        for (int currentK = 1; currentK <= k; currentK++) {
            int currentIteration = 0;
            BitSet changedPredicates = new BitSet(predicatesToIdx.size());
            MultiMap<Literal, BSupport> literalsWithChangedSupports = new MultiMap<>();
            MultiMap<Literal, BSupport> completelyNewLiterals = new MultiMap<>();

            do {
//                System.out.println(currentK + "\t->\t" + currentIteration);
//                System.out.println("map\t" + map.keySet().size() + "\t" + sizeOfSupports);

                MultiMap<Literal, BSupport> gainedNewSupports = new MultiMap<>();
                MultiMap<Literal, BSupport> brandNewLiteral = new MultiMap<>();
                BitSet currentlyChangedPredicates = new BitSet(predicatesToIdx.size());

                MultiMap<Literal, BSet> changesToBeAdded = new MultiMap<>();

                for (Triple<Clause, Literal, BSet> preparedRule : rulesWithNegated) {
                    boolean firstIterationOfNewRule = 0 == currentIteration && newlyAddedRules.contains(preparedRule);
                    if (firstIterationOfNewRule || isWorthTrying(preparedRule, changedPredicates, globalChangedPredicates, currentIteration)) {
                        world = completelyNewLiterals.isEmpty() ? world : Matching.create(new Clause(map.keySet()), Matching.THETA_SUBSUMPTION);
                        Pair<Term[], List<Term[]>> subts = world.allSubstitutions(preparedRule.r, 0, Integer.MAX_VALUE);
                        for (Term[] terms : subts.s) {
                            //Literal head = LogicUtils.substitute(preparedRule.s, subts.r, terms);
                            //Clause groundBody = LogicUtils.substitute(preparedRule.r, subts.r, terms); // we work with range restricted rules only; otherwise we would need to generate the support by adding constants from the head, wouldn't we?
                            // well, that's a speedup!
                            // Sub
                            LogicUtils.substituteStatefullyPreparation(subts.r);
                            Literal head = LogicUtils.substituteStatefully(preparedRule.s, terms);

                            if (this.evidenceBased && this.evidence.contains(head)) {
                                continue;
                            }

                            List<Literal> groundBody = LogicUtils.substituteStatefully(preparedRule.r, terms, true); // tady by mohl byt i set ale to se asi mockrat stavat nenbude
                            // SubL is not faster than Sub
                            /*substituteStatefullyPreparation(subts.r);
                            Literal head = preparedRule.s.substituteStatefully(terms);
                            //List<Literal> groundBody = LogicUtils.substituteStatefully(preparedRule.r, terms, true);
                            List<Literal> groundBody = new ArrayList<>(preparedRule.r.countLiterals());
                            */

                            //boolean debugThisIteration = false;
                            boolean debugThisIteration = head.toString().equals(debugStr);
                            if (debugThisIteration) {
                                System.out.println(head);
                                System.out.println("\t" + groundBody.stream().map(Literal::toString).sorted().collect(Collectors.joining(", ")));
                                System.out.println(">>\t" + groundBody.size() + "\t" + preparedRule.r.literals().size());
                                System.out.println(head.hashCode() + "\t" + groundBody.stream().map(Literal::toString).sorted().collect(Collectors.joining(", ")).hashCode());
                            }
                            //debugThisIteration = debugThisIteration && 1947460170 == groundBody.stream().map(Literal::toString).sorted().collect(Collectors.joining(", ")).hashCode();


                            if (debugThisIteration) {
                                groundBody.stream().sorted(Comparator.comparing(Literal::toString)).
                                        forEach(l -> {
                                            System.out.println("- " + l);
                                            this.map.get(l).stream()
                                                    .map(x -> this.toCanon(x) + "\t" + x.getSet().stream().mapToObj(z -> "" + z).collect(Collectors.joining(", ")))
//                                        .map(x -> this.toCanon(x))
                                                    .sorted().forEach(x -> System.out.println("-- " + x));
                                        });
                            }

                            int updatedLiterals = 2;
                            MultiMap<Literal, BSupport> lookup = null;
                            if (!firstIterationOfNewRule) {
                                //if ((0 == currentIteration && canSkipGrounding(groundBody, globalChanges))
                                //        || (currentIteration > 0 && canSkipGrounding(groundBody, completelyNewLiterals))) {
                                //if (canSkipGrounding(groundBody, 0 == currentIteration ? globalChanges : completelyNewLiterals)) {
                                lookup = 0 == currentIteration ? globalChanges : literalsWithChangedSupports;
                                updatedLiterals = numberOfChangedSupports(groundBody, lookup);
                                if (0 == updatedLiterals) {
                                    // prune this if possible!
                                    if (debugThisIteration) {
                                        System.out.println("both sipping\t" + firstIterationOfNewRule + "\t" + true);
                                        //System.out.println(iteration + "\t" + newlyAddedRules.contains(preparedRule) + "\t" + canSkipGrounding(groundBody, 0 == currentIteration ? globalChanges : completelyNewLiterals) + "\t" + currentIteration);
                                        System.out.println("q\t" + globalChanges.size() + " vs " + literalsWithChangedSupports.size());
                                        for (Literal literal : groundBody) {
                                            System.out.println("g " + literal + globalChanges.containsKey(literal) + "\t " + literalsWithChangedSupports.containsKey(literal));
                                            this.map.get(literal).stream().map(x -> toCanon(x) + " " + x.getSet().toString()).forEach(x -> System.out.println("x- " + x));
                                        }
                                    }
                                    continue;
                                } else {
                                    cannotSkip++;
                                    ones += 1 == updatedLiterals ? 1 : 0;
                                }
                            }

                            Set<BSet> candidates = cartesianProduct(groundBody, currentK, preparedRule, 1 == updatedLiterals ? lookup : null);

                            if (candidates.isEmpty()) {
                                continue;
                            }
                            changesToBeAdded.putAll(head, candidates);

                        }
                    }
                }
                currentIteration++;

                for (Map.Entry<Literal, Set<BSet>> entry : changesToBeAdded.entrySet()) {
                    boolean thisChanged = false;
                    Literal head = entry.getKey();
                    List<BSet> minimals = minimal(entry.getValue());
                    if (null == this.map.get(head)) {
                        Set<BSupport> supports = Sugar.set();
                        for (BSet set : minimals) {
                            BSupport support = supportCache.get(set);
                            if (null == support) {
                                support = BSupport.create(set);
                                supportCache.put(set, support);
                            }
                            supports.add(support);
                            //support.addLiteral(head); muzu mit vice pravidel, ktere vyusti ve stejny support BSet, a kdyz pak budu odebirat, tak mi to asi moc nepomuze, kdyz bych to chtel odebirat -- musel by tam byt counter... takze to spis nepouzivat
                        }
                        this.map.put(head, supports);
                        thisChanged = true;
                        brandNewLiteral.putAll(head, supports);
                        globalChanges.putAll(head, supports);
                        gainedNewSupports.putAll(head, supports);
                    } else {
                        Set<BSupport> oldSupports = this.map.get(head);
                        List<BSupport> newFiltered = nonSubsumed(oldSupports, minimals);
                        if (newFiltered.isEmpty()) {
                            thisChanged = false;
                            continue;
                        }
                        thisChanged = true;
                        List<BSupport> toRemove = filterToRemove(newFiltered, oldSupports);

                        if (oldSupports.size() == toRemove.size()) {
                            this.map.put(head, Sugar.setFromCollections(newFiltered));
                        } else {
                            // todo pro constraints se tady potom musi udelat i support.addHead(head) a pri mazani zase odmazat
                            toRemove.forEach(oldSupports::remove);
                            oldSupports.addAll(newFiltered);
                        }
                        globalChanges.putAll(head, newFiltered);
                        gainedNewSupports.putAll(head, newFiltered);
                    }

                    if (thisChanged) {
                        globalChangedPredicates.set(predicatesToIdx.get(head.getPredicate()));
                        currentlyChangedPredicates.set(predicatesToIdx.get(head.getPredicate()));
                    }
                }

                changedPredicates = currentlyChangedPredicates;
                completelyNewLiterals = brandNewLiteral;
                literalsWithChangedSupports = gainedNewSupports;
            }
            while (!literalsWithChangedSupports.isEmpty());
        }
        System.out.println("carts\t" + carts);
    }

    private List<BSupport> nonSubsumed(Set<BSupport> filters, List<BSet> candidates) {
        List<BSupport> retVal = Sugar.list();
        for (BSet candidate : candidates) {
            boolean isUnique = true;
            for (BSupport filter : filters) {
                if (filter.isSubsetOf(candidate)) {
                    isUnique = false;
                    break;
                }
            }
            if (isUnique) {
                BSupport support = this.supportCache.get(candidate);
                if (null == support) {
                    support = BSupport.create(candidate);
                    this.supportCache.put(candidate, support);
                }
                retVal.add(support);
            }
        }
        return retVal;
    }

    private List<BSupport> filterToRemove(List<BSupport> filters, Set<BSupport> supports) {
        List<BSupport> retVal = Sugar.list();
        for (BSupport support : supports) {
            for (BSupport filter : filters) {
                if (filter.getSet().isSubsetOf(support.getSet())) {
                    /*if (filter.getSet().cardinality().equals(support.getSet().cardinality())) {
                        throw new IllegalStateException();
                    }*/
                    retVal.add(support);
                    break;
                }
            }
        }
        return retVal;
    }

    private List<BSet> minimal(Set<BSet> candidates) {
        List<BSet> retVal = Sugar.list();
        List<BSet> cands = Sugar.listFromCollections(candidates);
        cands.sort(Comparator.comparing(BSet::cardinality));
        for (int outerIdx = cands.size() - 1; outerIdx >= 0; outerIdx--) {
            boolean someoneSubumesMe = false;
            BSet outer = cands.get(outerIdx);
            for (int innerIdx = 0; innerIdx < outerIdx && !someoneSubumesMe; innerIdx++) {
                someoneSubumesMe = cands.get(innerIdx).isSubsetOf(outer);
            }
            if (!someoneSubumesMe) {
                BSet set = FastSubsetFactory.getInstance().get(outer);
                retVal.add(set);
            }
        }

        return retVal;
    }

    private Set<BSet> cartesianProduct(List<Literal> groundBody, int currentK, Triple<Clause, Literal, BSet> preparedRule, MultiMap<Literal, BSupport> pruneLookup) {
        if (1 == groundBody.size()) {
            if (preparedRule.t.cardinality() > 0) {
                return this.map.get(groundBody.get(0)).stream()
                        .map(s -> s.getSet().union(preparedRule.t))
                        .filter(bset -> bset.cardinality() == currentK)
                        .collect(Collectors.toSet());
            } else {
                return this.map.get(groundBody.get(0)).stream()
                        .filter(support -> support.getSet().cardinality() == currentK)
                        .map(BSupport::getSet)
                        .collect(Collectors.toSet());
            }
        }
        Set<BSet> previousSupports = null;
        if (preparedRule.t.cardinality() > 0) {
            previousSupports = Sugar.set(preparedRule.t);
        }
        for (Literal literal : groundBody) {
            Set<BSet> next = Sugar.set();
            Set<BSupport> supports = null != pruneLookup && pruneLookup.containsKey(literal) ? pruneLookup.get(literal) : supports(literal);
            if (null == previousSupports) {
                supports.forEach(support -> {
                    if (support.getSet().cardinality() <= currentK) {
                        next.add(support.getSet());
                    }
                });
            } else {
                for (BSupport support : supports) {
                    for (BSet previousSupport : previousSupports) {
                        BSet union = support.getSet().union(previousSupport);
                        if (union.cardinality() <= currentK) {
                            next.add(union);
                        }
                    }
                }
            }

            if (next.isEmpty()) {
                return next;
            }
            previousSupports = next;
        }
        this.carts += previousSupports.size();
        return previousSupports.stream().filter(bset -> bset.cardinality() == currentK).collect(Collectors.toSet());
    }

    /**
     * returns true iff some predicate in the rule body (preparedRule.r.literals()) has been changed in the last iteration (= is set to true in changedPredicates)
     *
     * @param preparedRule
     * @return
     */

    private boolean isWorthTrying(Triple<Clause, Literal, BSet> preparedRule, BitSet predicates, BitSet globallyChangedPredicates, int currentIteration) {
        BitSet changedPredigates = 0 == currentIteration ? globallyChangedPredicates : predicates;

        for (Literal literal : preparedRule.getR().literals()) {
            if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate())
                    || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                continue;
            }
            if (changedPredigates.get(this.predicatesToIdx.get(literal.getPredicate()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * returns true if this grounding can be cannotSkip, i.e. there was no change in the supports of any literal in the body in the last iteration
     *
     * @param groundBody
     * @param literalsWithChangedSupports
     * @return
     */
    private boolean canSkipGrounding(List<Literal> groundBody, Set<Literal> literalsWithChangedSupports) {
        for (Literal literal : groundBody) {
            if (literalsWithChangedSupports.contains(literal)) {
                return false;
            }
        }
        return true;
    }

    /**
     * returns the number of lit
     *
     * @param groundBody
     * @param literalsWithChangedSupports
     * @return
     */
    private int numberOfChangedSupports(List<Literal> groundBody, MultiMap<Literal, BSupport> literalsWithChangedSupports) {
        int within = 0;
        for (Literal literal : groundBody) {
            if (literalsWithChangedSupports.containsKey(literal)) {
                within++;
            }
        }
        return within;
    }


    private String toCanon(BSupport s) {
        return "{" + constantToSubset.entrySet().stream()
                .filter(entry -> entry.getValue().isSubsetOf(s.getSet()))
                .map(entry -> entry.getKey().toString())
                .sorted().collect(Collectors.joining(", "))
                + "}";
    }

    private String toCanon(BSet set) {
        return "{" + constantToSubset.entrySet().stream()
                .filter(entry -> entry.getValue().isSubsetOf(set))
                .map(entry -> entry.getKey().toString())
                .sorted().collect(Collectors.joining(", "))
                + "}";
    }


    private long supportsInWorld() {
        return map.values().stream().flatMap(Collection::stream).distinct().count();
    }

    private List<BSupport> forbiddenFree(Set<BSupport> supports) {
        assert false;
        System.out.println("this is probably outdated");
        return Sugar.parallelStream(supports, parallel)
                //.filter(s -> forbidden.stream().noneMatch(clash -> s.getSet().isSubsetOf(clash.getSet())))
                .filter(s -> forbidden.stream().noneMatch(clash -> clash.getSet().isSubsetOf(s.getSet())))
                .collect(Collectors.toList());
    }

    // Ondra has an idea of multimap<constant,support> which would make faster retrieving of getting the supports of set subset of constant


    private Set<BSupport> supports(Literal literal) {
        Set<BSupport> support = map.get(literal);
        if (null == support) {
            return Sugar.set(); // just a work around
        }
        return support;
    }

    public Map<Literal, Set<BSupport>> getSupports() {
        // not a copy, be careful!
        return this.map;
    }

    public KECollector entailed() {

        KECollector retVal = KECollector.create(evidence);
        //SupportCollector retVal = SupportCollector.get(this.constantToSubset);
//        try {
//
//            for (Map.Entry<Literal, Set<Support>> literalSetEntry : map.entrySet()) {
//                System.out.println(literalSetEntry);
//                System.out.println("\t"+literalSetEntry.getValue());
//                System.out.println("\t"+Sugar.chooseOne(literalSetEntry.getValue()).getSet());
//            }


//        map.entrySet().stream().forEach(e -> {
//            if (debug.contains(e.getKey())) {
//                System.out.println("output!!!!");
//                System.out.println(e.getKey());
//                e.getValue().forEach(s -> System.out.println("\t\t" + toCanon(s)));
//            }
//        });

        /*map.entrySet().stream().forEach(e -> {
            System.out.println(e.getKey());
            e.getValue().forEach(s -> System.out.println("\t\t" + toCanon(s)));
        });*/

        //map.entrySet().stream().forEach(e -> retVal.add(e.getKey(), Sugar.chooseOne(e.getValue()).getSet()));
        // TODO hot fix, it should be as is above; the subset is thrown away anyway inside
        map.entrySet().stream().forEach(e -> retVal.add(e.getKey(), null));

        //map.entrySet().stream().forEach(e -> retVal.add(e.getKey(), Sugar.chooseOne(e.getValue()).getSet()));
//            map.entrySet().stream().forEach(e -> {
//                Support o = Sugar.chooseOne(e.getValue());
//                Subset s = o.getSet();
//                retVal.add(e.getKey(), s);
//            });
//        } catch (Exception e) {
//            System.out.println(map);
//            map.entrySet().forEach(l -> {
//                System.out.println("" + l.getKey());
//                System.out.println("\t" + l.getValue());
//            });
//            e.printStackTrace();
//            System.exit(-1);
//        }
        return retVal;
    }

    public static SupportsHolderOpt10ForwardMoreThanOneFix create(Set<Literal> evidence, int k, Map<Pair<String, Integer>, Integer> predicatesToBitset, boolean useExtraPruning, List<Clause> negatedConstraints) {
        List<Constant> constants = LogicUtils.constants(evidence).stream().sorted(Comparator.comparing(Constant::toString)).collect(Collectors.toList());

        Map<Constant, BSet> constantToSubset = new HashMap<>();
        Map<Constant, Integer> constantToInteger = new HashMap<>();
        IntStream.range(0, constants.size()).forEach(idx -> {
            constantToSubset.put(constants.get(idx), FastSubsetFactory.getInstance().get(constants.size(), idx));
            constantToInteger.put(constants.get(idx), idx);
        });

        Map<BSet, BSupport> supportCache = new ConcurrentHashMap<>();
        Map<Literal, Set<BSupport>> map = new ConcurrentHashMap<>();

        for (Literal literal : evidence) {
            BSet set = constantsToSubset(LogicUtils.constantsFromLiteral(literal), constantToInteger);
            BSupport support = supportCache.get(set);
            if (null == support) {
                support = BSupport.create(set);
                supportCache.put(set, support);
            }
            map.put(literal, Sugar.set(support));
            support.addLiteral(literal);
        }

        return new SupportsHolderOpt10ForwardMoreThanOneFix(k, evidence, constantToSubset, constants, map, //heads,
                supportCache, predicatesToBitset, useExtraPruning, constantToInteger, negatedConstraints);
    }

    private BSet constantsToSubsetTranslator(Literal literal) {
        Set<Constant> constants = Sugar.set();
        for (Term term : literal.terms()) {
            constants.add((Constant) term);
        }
        return constantsToSubset(constants);
    }

    private BSet constantsToSubset(Set<Constant> constants) {
        BSet retVal = constantsToSubset.get(constants);
        if (null != retVal) {
            return retVal;
        }
        //retVal = FastSubsetFactory.getInstance().union(constants.stream().map(c -> constantToSubset.get(c)).collect(Collectors.toList()));
        List<Integer> constantsIndexes = constants.stream().map(constantToInteger::get).collect(Collectors.toList());
        retVal = FastSubsetFactory.getInstance().get(this.constantToInteger.keySet().size(), constantsIndexes);
        constantsToSubset.put(constants, retVal);
        return retVal;
    }

    public BSet constantsToSubset(Literal literal) {
        List<Integer> constantsIndexes = literal.terms().stream().filter(term -> term instanceof Constant)
                .map(constantToInteger::get).collect(Collectors.toList());
        return FastSubsetFactory.getInstance().get(constantToInteger.keySet().size(), constantsIndexes);
    }


    public static BSet constantsToSubset(Set<Constant> constants, Map<Constant, Integer> constantToInteger) {
        List<Integer> constantsIndexes = constants.stream().map(constantToInteger::get).collect(Collectors.toList());
        return FastSubsetFactory.getInstance().get(constantToInteger.keySet().size(), constantsIndexes);
    }

    // returns true if another iteration is NOT needed
    public boolean constraintsCheck(List<Clause> negatedConstraints, long iteration) {
        if (!negatedConstraints.isEmpty()) {
            throw new IllegalStateException();
        }
        //System.out.println("this version is without constraints");
        return true;
    }


    private Iterable<? extends Literal> mask(BSupport support) {
        return map.entrySet().parallelStream()
                .filter(entry -> entry.getValue().stream().anyMatch(literalSupport -> literalSupport.getSet().isSubsetOf(support.getSet()))) // bude tohle fungovat spravne?
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void gcLike(List<Support> violatedSupports) {
        for (Support support : violatedSupports) {
            supportCache.remove(support);
        }

    }

    public void debugIt2() {
        if (this.oldVersion) {// old version
            this.forbidden.stream().map(this::toCanon).sorted().forEach(p -> System.out.println("\t" + p));

        } else { // speed-up version
            this.forbiddens.values().stream().flatMap(c -> c.stream()).map(this::toCanon).sorted().forEach(p -> System.out.println("\t" + p));

        }
    }

    public void debugIt() {
        System.out.println("forbidden");
        System.out.println("this is outdated for this.forbidden (old version); not compatible with speedup version");
        this.forbidden.stream().map(this::toCanon).sorted().forEach(p -> System.out.println("\t" + p));

        List<Clause> constraints = Sugar.list("!Post_Generals(1:V0), !Pre_Quals(1:V0)",
                "!Post_Quals(1:V0), !Pre_Quals(1:V0)",
                "!Faculty_adjunct(1:V0), !Faculty(1:V0)",
                "!Post_Quals(1:V0), !Post_Generals(1:V0)").stream().map(Clause::parse).collect(Collectors.toList());

        Matching world = Matching.create(new Clause(this.map.keySet()), Matching.THETA_SUBSUMPTION);
        for (Clause constraint : constraints) {
            Pair<Term[], List<Term[]>> sub = world.allSubstitutions(LogicUtils.flipSigns(constraint), 0, Integer.MAX_VALUE);
            System.out.println("rule\t" + constraint);
            for (Term[] terms : sub.getS()) {
                System.out.println("\t" + LogicUtils.substitute(constraint, sub.r, terms));
            }
        }
        System.out.println(this.map.keySet().size() + "\t" + this.map.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(";")).hashCode());

    }

    public String debugPrint() {
        return this.map.keySet().stream().sorted(Comparator.comparing(Objects::toString)).map(l -> {
            return l + "\n\t" + map.get(l).stream().map(this::toCanon).sorted().collect(Collectors.joining(", "));
        }).collect(Collectors.joining("\n"));
    }

    public List<BSet> setsToUnions(List<Set<BSupport>> supports) {
        List<BSet> retVal = new ArrayList<>(supports.size());
        for (Set<BSupport> support : supports) {
            BSet bs = FastSubsetFactory.getInstance().get(this.constantToSubset.size());
            for (BSupport sup : support) {
                bs = bs.union(sup.getSet());
            }
            retVal.add(bs);
        }
        return retVal;
    }

    public BSet toUnion(Set<BSet> bts) {
        //BitSet retVal = new BitSet(this.constantToSubset.size());
        BSet retVal = FastSubsetFactory.getInstance().get(this.constantToSubset.size());
        for (BSet set : bts) {
            retVal = retVal.union(set);
        }
        return retVal;
    }

    public void substituteStatefullyPreparation(Term[] variables) {
        for (int idx = 0; idx < variables.length; idx++) {
            ((Variable) variables[idx]).setStatefullIndex(idx);
        }
    }

    public void debug() {
        System.out.println("out");
        System.out.println("map\t" + map.keySet().size() + "\t"
                + map.values().stream().flatMap(Collection::stream).distinct().count() + "\t"
                + map.values().stream().mapToInt(Collection::size).sum());

        this.map.keySet().stream().sorted(Comparator.comparing(Literal::toString))
                .forEach(key -> {
                    System.out.println(key);
                    this.map.get(key).stream().map(this::toCanon).sorted()
                            .forEach(s -> System.out.println("\t" + s));
                });
    }
}

