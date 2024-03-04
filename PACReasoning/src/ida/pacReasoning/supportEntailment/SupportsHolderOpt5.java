package ida.pacReasoning.supportEntailment;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialBinaryPredicates;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.SubsetFactory;
import ida.pacReasoning.entailment.collectors.KECollector;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 15. 10. 2018.
 */
public class SupportsHolderOpt5 {

    //todo je tady nekde chyba protoze se pridama daleko mene do

    private final Map<Constant, Subset> constantToSubset;
    private final List<Constant> evidenceConstants;
    private final Map<Set<Constant>, Subset> constantsToSubset;
    private final Set<Literal> evidence;
    private final Map<Literal, Set<Support>> map;
    private final int k;
    private final Map<Subset, Support> supportCache;
    //private final Set<Support> forbidden;
    // optimized
    //private final Map<Support> forbidden;
    //private final Set<Support>[] forbidden;
    private final List<Set<Support>> forbidden;
    private final Map<Pair<String, Integer>, Integer> predicatesIdx;
    private final boolean useExtraPruning;
    private final boolean allPreviousOnly;
    private BitSet changedPredicatesInLastIteration;
    private Set<Support> supportsOfBrandNewHeads;
    private long sizeOfSupports = 0;
    private int sizeOfWorld;
    private final boolean parallel = true;
    private boolean change;
    private Map<Literal, Set<Support>> changedLiteralsInLastIteration;
    private Set<Literal> brandNewHeads;

    {
        // TODO
//        System.out.println("SupportsHolder -- todo: namisto metody mask pouzit nejakou jinou datovou strukturu aby i support mel seznam literalu ktere uvozuje");
        //TODO nekde je tu bug, kdyz neni zapnuta cache na supporty tak to obcas se asi muze stat ze jsou dva supporty rozdilne objekty a potom to pada protoze se nekde operuje s nullem... coz je zlvastni
    }

    public SupportsHolderOpt5(int k, Set<Literal> evidence, Map<Constant, Subset> constantToSubset, List<Constant> evidenceConstants, Map<Literal, Set<Support>> map, Map<Subset, Support> supportCache, Map<Pair<String, Integer>, Integer> predicatesIdx, boolean useExtraPruning) {
        this.k = k;
        this.evidence = evidence;
        this.constantToSubset = constantToSubset;
        this.evidenceConstants = evidenceConstants;
        this.constantsToSubset = new HashMap<>();
        this.map = map;
        this.supportCache = supportCache;
        this.predicatesIdx = predicatesIdx;
        this.changedLiteralsInLastIteration = new HashMap<>();
        this.changedPredicatesInLastIteration = new BitSet(predicatesIdx.size());
        this.useExtraPruning = useExtraPruning;
        this.brandNewHeads = Sugar.set();
        // testing of this combination was slower on UMLS than without it
        this.allPreviousOnly = false;//System.getProperty("devMod").contains("APO");
        //this.forbidden = new Set<Support>[constantToSubset.size()];
        this.forbidden = IntStream.range(0, constantToSubset.size()).mapToObj(i -> new LinkedHashSet<Support>()).collect(Collectors.toList());
    }

    // pri forward pruchodu se berou do karteskeho soucinu uz supporty ktere jsou (tzn. i nejaky ze soucasne iterace tam muze zpropagovat se), ale constraints (forbidden) si s tim dystak poradi
    // the main point is to fill in supportsOfBrandNewHeads and currentHeads as these two are then used for constraints part; the rest fills in data into map, etc.
    // also to fill the newly computed into map
    public void forwardRules(List<Pair<Clause, Literal>> rulesWithNegated, long iteration, Set<Pair<Clause, Literal>> newlyAddedRules) {
        System.out.println("forbidden size\t" + forbiddenSize());

        this.sizeOfWorld = map.keySet().size();
        this.supportsOfBrandNewHeads = Sugar.set();
        this.sizeOfSupports = supportsInWorld();
        this.change = false;
        BitSet changedPredicates = new BitSet(predicatesIdx.size());
        Map<Literal, Set<Support>> literalsWithSupportsToBeAdded = new HashMap<>();
        Matching world = Matching.create(new Clause(map.keySet()), Matching.THETA_SUBSUMPTION);
        Set<Literal> headsNeverSeenBefore = Sugar.set();

        // complementary to this approach, we could collect violated rules (to Map<head,Literal<groundBodies>>) and to do all the minimal operation of one head in one place;
        // that would, however, need more and more memory
        //boolean debug = false;
        for (Pair<Clause, Literal> preparedRule : rulesWithNegated) {
            boolean isNewlyAddedRule = newlyAddedRules.contains(preparedRule);
            if (0 == iteration && !isNewlyAddedRule) {
                // in this case the rule cannot add anything, thus we can skip computation of groundings of this rule
                continue;
            } else if (iteration > 0) {
                boolean someLiteralChanged = false; // is true if there is a predicate in the rule body for which there has been a change in the literals in the world (e.g. some literal of that predicate was added or its support changed)
                for (Literal literal : preparedRule.getR().literals()) {
                    if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                        continue;
                    }
                    if (this.changedPredicatesInLastIteration.get(this.predicatesIdx.get(literal.getPredicate()))) {
                        someLiteralChanged = true;
                        break;
                    }
                }
                if (!someLiteralChanged) {
                    continue; // all groundings of this rule were found and added in the previous step, thus it can be skipped in this pass
                }
            }

            boolean canUsePruning = 0 != iteration || !isNewlyAddedRule;
            Pair<Term[], List<Term[]>> subts = world.allSubstitutions(preparedRule.r, 0, Integer.MAX_VALUE);
            for (Term[] terms : subts.s) {
                //Literal head = LogicUtils.substitute(preparedRule.s, subts.r, terms);
                //Clause groundBody = LogicUtils.substitute(preparedRule.r, subts.r, terms); // we work with range restricted rules only; otherwise we would need to generate the support by adding constants from the head, wouldn't we?
                // well, that's a speedup!
                // Sub
                LogicUtils.substituteStatefullyPreparation(subts.r);
                Literal head = LogicUtils.substituteStatefully(preparedRule.s, terms);
                List<Literal> groundBody = LogicUtils.substituteStatefully(preparedRule.r, terms, true);
                // SubL is not faster than Sub
                /*substituteStatefullyPreparation(subts.r);
                Literal head = preparedRule.s.substituteStatefully(terms);
                //List<Literal> groundBody = LogicUtils.substituteStatefully(preparedRule.r, terms, true);
                List<Literal> groundBody = new ArrayList<>(preparedRule.r.countLiterals());
                */
                for (Literal literal : preparedRule.r.literals()) {
                    if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                        continue;
                    }
                    groundBody.add(literal.substituteStatefully(terms));
                }

                boolean allUntouched = true;
                boolean atLeastOneNonEvidence = false;
                boolean someBrandNew = false;
                int changedLiterals = 0;
                if (canUsePruning) {
                    for (Literal literal : groundBody) {
                        atLeastOneNonEvidence = atLeastOneNonEvidence || !this.evidence.contains(literal);
                        //allUntouched = allUntouched && !this.changedLiteralsInLastIteration.containsKey(literal);
                        if (this.changedLiteralsInLastIteration.containsKey(literal)) {
                            allUntouched = false;
                            changedLiterals++;
                        }
                        if (this.useExtraPruning) {
                            someBrandNew = someBrandNew || this.brandNewHeads.contains(literal);
                        }
                    }
                } else {
                    allUntouched = false; // we set this variable to false to say that we cannot use the pruning here (it would not be consistent)
                    // the reason for this is that this rule is newly added, therefore
                }

                // (allUntouched && atLeastOneNonEvidence && map.containsKey(head)) this is a condition, which when true, says that this particular support is already in map.get(head) and thus can be skipped
                if ((allUntouched && atLeastOneNonEvidence && map.containsKey(head)) || isForbiddenT(terms)) {
                    continue;
                }
                boolean moreThanOneLiteralChanged = changedLiterals > 1;

                Set<BitSet> rawSupports = computeCartesian(iteration, isNewlyAddedRule, literalsWithSupportsToBeAdded, preparedRule, groundBody, someBrandNew, moreThanOneLiteralChanged);
                // possible place for speedup
                //Set<BitSet> rawSupports = computeCartesianMinSort(iteration, newlyAddedRules, literalsWithSupportsToBeAdded, preparedRule, groundBody, someBrandNew, moreThanOneLiteralChanged);
                //Set<BitSet> rawSupports = computeCartesianCompelmentarySymmetry(iteration, newlyAddedRules, literalsWithSupportsToBeAdded, preparedRule, groundBody, someBrandNew, moreThanOneLiteralChanged);

                Set<Support> newSupports = toMinimalForbiddenFreeSupports(rawSupports);
                if (newSupports.isEmpty()) {
                    continue;
                }

                Set<Support> previousSupports = map.get(head);
                List<Support> toRemove = new LinkedList<>();
                List<Support> toAdd = Sugar.list();

                if (null == previousSupports) { // i.e. no previous support for this head
                    Set<Support> changeLits = literalsWithSupportsToBeAdded.get(head);
                    if (null == changeLits) {
                        changeLits = Sugar.set();
                        literalsWithSupportsToBeAdded.put(head, changeLits);
                    }
                    changeLits.addAll(newSupports);
                    changedPredicates.set(this.predicatesIdx.get(head.getPredicate()));
                    if (this.useExtraPruning) {
                        headsNeverSeenBefore.add(head);
                    }
                } else {
                    for (Support newOne : newSupports) {
                        boolean someoneSubsumesMe = false;
                        for (Support oldOne : previousSupports) {
                            // note that both of these condition cannot happen at for one oldOne support, because then we would have non-minimal proofs in the previousSupports (i.e. there would be a circuit)
                            if (oldOne.getSubset().isSubsetOf(newOne.getSubset())) {
                                someoneSubsumesMe = true;
                                break;
                            } else if (newOne.getSubset().isSubsetOf(oldOne.getSubset())) {
                                toRemove.add(oldOne);
                            }
                        }
                        if (!someoneSubsumesMe) {
                            toAdd.add(newOne);
                        }
                    }

                    if (!toAdd.isEmpty()) { // !toRemove.isEmpty() implies this one, but the converse does not hold
                        changedPredicates.set(this.predicatesIdx.get(head.getPredicate()));
                        Set<Support> changeLits = literalsWithSupportsToBeAdded.get(head);
                        if (null == changeLits) {
                            changeLits = Sugar.set();
                            literalsWithSupportsToBeAdded.put(head, changeLits);
                        }
                        changeLits.addAll(toAdd);
                        // in-place removal of
                        for (Support support : toRemove) {
                            previousSupports.remove(support);
                            support.removeHead(head);
                            // here can be also deletion of supports with no heads (gc friendly)
                        }
                    }

                }
            }
        }

        this.changedLiteralsInLastIteration = literalsWithSupportsToBeAdded;
        this.changedPredicatesInLastIteration = changedPredicates;
        this.change = incorporateNewSupports(literalsWithSupportsToBeAdded, supportsOfBrandNewHeads);
        this.brandNewHeads = headsNeverSeenBefore;
    }

    private int forbiddenSize() {
        return this.forbidden.stream().flatMap(i -> i.stream()).collect(Collectors.toSet()).size();
    }

    private Set<BitSet> computeCartesian(long iteration, boolean isNewlyAddedRule, Map<Literal, Set<Support>> literalsWithSupportsToBeAdded, Pair<Clause, Literal> preparedRule, List<Literal> groundBody, boolean someBrandNew, boolean moreThanOneLiteralChanged) {
        Set<BitSet> rawSupports = Sugar.set();
        for (Literal literal : groundBody) {
            Set<Support> literalsSupports = null;
            if (this.useExtraPruning) {
                literalsSupports = ((0 == iteration && isNewlyAddedRule) || someBrandNew || moreThanOneLiteralChanged) ? supports(literal) : this.changedLiteralsInLastIteration.get(literal);
                if (null == literalsSupports) {
                    literalsSupports = supports(literal);
                }
            } else {
                literalsSupports = supports(literal);
                if (!this.allPreviousOnly && null != literalsWithSupportsToBeAdded.get(literal)) {
                    literalsSupports = Sugar.union(literalsSupports, literalsWithSupportsToBeAdded.get(literal));
                }
            }
            rawSupports = cartesianOpt(literalsSupports, rawSupports); // todo, toto zrychlit!
            if (rawSupports.isEmpty()) {
                break;
            }
        }
        return rawSupports;
    }

    private Set<BitSet> computeCartesianMinSort(long iteration, Set<Clause> newlyAddedRules, Map<Literal, Set<Support>> literalsWithSupportsToBeAdded, Pair<Clause, Literal> preparedRule, List<Literal> groundBody, boolean someBrandNew, boolean moreThanOneLiteralChanged) {
        List<Set<Support>> supports = new ArrayList<>(groundBody.size());
        for (Literal literal : groundBody) {
            Set<Support> literalsSupports = null;
            if (this.useExtraPruning) {
                literalsSupports = ((0 == iteration && newlyAddedRules.contains(preparedRule.r)) || someBrandNew || moreThanOneLiteralChanged) ? supports(literal) : this.changedLiteralsInLastIteration.get(literal);
                if (null == literalsSupports) {
                    literalsSupports = supports(literal);
                }
            } else {
                literalsSupports = supports(literal);
                if (!this.allPreviousOnly && null != literalsWithSupportsToBeAdded.get(literal)) { //ted pokus, vysledky v allprev ; takhle to bere i ty nove najite
                    literalsSupports = Sugar.union(literalsSupports, literalsWithSupportsToBeAdded.get(literal)); // takhle to dela uple to same jako opt + proriznuti pres predikaty
                }
            }
            supports.add(literalsSupports);
        }
        supports.sort(Comparator.comparingInt(Collection::size));
        Set<BitSet> rawSupports = Sugar.set();
        for (Set<Support> support : supports) {
            rawSupports = cartesianOpt(support, rawSupports); // todo, toto zrychlit!
            if (rawSupports.isEmpty()) {
                break;
            }
        }
        return rawSupports;
    }

    private Set<BitSet> computeCartesianCompelmentarySymmetry(long iteration, Set<Clause> newlyAddedRules, Map<Literal, Set<Support>> literalsWithSupportsToBeAdded, Pair<Clause, Literal> preparedRule, List<Literal> groundBody, boolean someBrandNew, boolean moreThanOneLiteralChanged) {
        // supports for the cartesian
        List<Set<Support>> supports = new ArrayList<>(groundBody.size());
        for (Literal literal : groundBody) {
            Set<Support> literalsSupports = null;
            if (this.useExtraPruning) {
                literalsSupports = ((0 == iteration && newlyAddedRules.contains(preparedRule.r)) || someBrandNew || moreThanOneLiteralChanged) ? supports(literal) : this.changedLiteralsInLastIteration.get(literal);
                if (null == literalsSupports) {
                    literalsSupports = supports(literal);
                }
            } else {
                literalsSupports = supports(literal);
                if (!this.allPreviousOnly && null != literalsWithSupportsToBeAdded.get(literal)) { //ted pokus, vysledky v allprev ; takhle to bere i ty nove najite
                    literalsSupports = Sugar.union(literalsSupports, literalsWithSupportsToBeAdded.get(literal)); // takhle to dela uple to same jako opt + proriznuti pres predikaty
                }
            }
            supports.add(literalsSupports);
        }

        if (1 == supports.size()) {
            return supports.get(0).stream().map(s -> s.getSubset().getBitset()).collect(Collectors.toSet());
        }

        // to unions
        List<BitSet> unions = setsToUnions(supports);
        boolean[] available = new boolean[unions.size()];
        for (int idx = 0; idx < unions.size(); idx++) {
            available[idx] = true;
        }

        // select first two which should be cartesianed
        Triple<Integer, Integer, Integer> firstTwo = findMaximalCS(unions);
        available[firstTwo.r] = false;
        available[firstTwo.s] = false;
        Set<BitSet> cartes = cart(supports.get(firstTwo.r).stream().map(s -> s.getSubset().getBitset()).collect(Collectors.toSet()), supports.get(firstTwo.s));


        if (cartes.isEmpty()) {
            return cartes;
        }
        // do the similar thing for the rest
        for (int iter = 0; iter < unions.size() - 2; iter++) {
            BitSet currentUnion = toUnion(cartes);
            Integer distance = null;
            Integer another = null;
            for (int idx = 0; idx < unions.size(); idx++) {
                if (available[idx]) {
                    int dist = complementarySymmetry(currentUnion, unions.get(idx));
                    if (null == distance || distance < dist) {
                        distance = dist;
                        another = idx;
                    }
                }
            }
            cartes = cart(cartes, supports.get(another));
            available[another] = false;
            if (cartes.isEmpty()) {
                return cartes;
            }
        }
        return cartes;
    }

    private Set<BitSet> cart(Set<BitSet> collect, Set<Support> supports) {
        Set<BitSet> retVal = new LinkedHashSet<>(collect.size() * supports.size());
        for (BitSet bitSet : collect) {
            for (Support support : supports) {
                BitSet innerBitset = support.getSubset().getBitset();
                BitSet union = union(bitSet, innerBitset);
                if (union.cardinality() <= k) {
                    retVal.add(union);
                }
            }
        }
        return retVal;
    }

    // <first, second, distance between them>
    private Triple<Integer, Integer, Integer> findMaximalCS(List<BitSet> unions) {
        Triple<Integer, Integer, Integer> retVal = new Triple(null, null, null);
        for (int outer = 0; outer < unions.size(); outer++) {
            BitSet s1 = unions.get(outer);
            for (int inner = outer + 1; inner < unions.size(); inner++) {
                int distance = complementarySymmetry(s1, unions.get(inner));
                if (null == retVal.t || retVal.t < distance) {
                    retVal.r = outer;
                    retVal.s = inner;
                    retVal.t = distance;
                }
            }
        }
        return retVal;
    }

    private int complementarySymmetry(BitSet s1, BitSet s2) {
        BitSet union = (BitSet) s1.clone();
        union.or(s2);
        BitSet intersection = (BitSet) s1.clone();
        intersection.and(s2);
        union.andNot(intersection);
        return union.cardinality();
    }


    // proste projdi mapu a nahaz tam ty supporty tak aby byli minimalni :))
    private boolean incorporateNewSupports(Map<Literal, Set<Support>> literalsWithSupportsToBeAdded, Set<Support> suppportsOfBrandNewHeads) {
        // these supports are already minimal w.r.t. the ones in the this.map, but not within each other
        boolean madeChange = false;
        for (Map.Entry<Literal, Set<Support>> entry : literalsWithSupportsToBeAdded.entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalStateException();// safety check, should not occur
            }
            Literal head = entry.getKey();
            Pair<List<Support>, List<Support>> minimalSubsumed = minimal(Sugar.listFromCollections(entry.getValue()));
            if (!minimalSubsumed.s.isEmpty()) {
                entry.getValue().removeAll(minimalSubsumed.s);
            }

            Set<Support> placeToAdd = this.map.get(head);
            boolean isBrandNew = null == placeToAdd;
            if (isBrandNew) {
                placeToAdd = new LinkedHashSet<>(minimalSubsumed.r.size());
                this.map.put(head, placeToAdd);
            }

            for (Support support : minimalSubsumed.r) {
                placeToAdd.add(support);
                support.addLiteral(head);
                if (isBrandNew) {
                    suppportsOfBrandNewHeads.add(support);
                }
            }
            madeChange = true;
        }
        return madeChange;
    }

    // this is really not nice because two filters (forbidden and minimal) and one factory-like operation are done within one method
    private Set<Support> toMinimalForbiddenFreeSupports(Set<BitSet> rawSupports) {
        // tohle je v extra metode jen kvuli profilovani; kdyby to bylo moc pomale tak treba paralelizovat pres hledani forbidden-free a tak ;)
        //spocitat nejdrive minimalni ze sups
        // throwing out forbidden ones
        /*List<BitSet> nonForbidden = new ArrayList<>(rawSupports.size());
        for (BitSet bitset : rawSupports) {
            boolean someForbiddenSubsumesMe = false;
            for (Support forbiddenSupport : forbidden) {
                if (forbiddenSupport.getSet().isSubsetOf(bitset)) {
                    someForbiddenSubsumesMe = true;
                    break;
                }
            }
            if (!someForbiddenSubsumesMe) {
                nonForbidden.add(bitset);
            }
        }*/
        List<BitSet> nonForbidden = rawSupports.stream()
                .filter(bitset -> !this.isForbiddenB(bitset))
                .collect(Collectors.toList());

        Set<Support> retVal = new LinkedHashSet<>(rawSupports.size());
        // throwing out non-minimal ones
        for (int outer = nonForbidden.size() - 1; outer >= 0; outer--) {
            boolean someoneSubsumesMe = false;
            BitSet me = nonForbidden.get(outer);
            for (int inner = 0; inner < outer; inner++) {
                BitSet intersection = (BitSet) nonForbidden.get(inner).clone();
                intersection.and(me);
                if (intersection.cardinality() == nonForbidden.get(inner).cardinality()) {
                    someoneSubsumesMe = true;
                    break;
                }
            }
            // adding to factory
            if (!someoneSubsumesMe) {
                Subset subset = SubsetFactory.getInstance().get(me);
                Support support = supportCache.get(subset);
                if (null == support) {
                    support = Support.create(subset);
                    supportCache.put(subset, support);
                }
                retVal.add(support);
            }
        }
        return retVal;
    }

    private String toCanon(Support s) {
        return "{" + constantToSubset.entrySet().stream()
                .filter(entry -> entry.getValue().isSubsetOf(s.getSubset()))
                .map(entry -> entry.getKey().toString())
                .sorted().collect(Collectors.joining(", "))
                + "}";
    }

    private long supportsInWorld() {
        return map.values().stream().flatMap(Collection::stream).distinct().count();
    }

    /*private List<Support> forbiddenFree(Set<Support> supports) {
        return Sugar.parallelStream(supports, parallel)
                //.filter(s -> forbidden.stream().noneMatch(clash -> s.getSet().isSubsetOf(clash.getSet())))
                .filter(s -> forbidden.stream().noneMatch(clash -> clash.getSet().isSubsetOf(s.getSet())))
                .collect(Collectors.toList());
    }*/

    private List<Support> forbiddenFree(Set<Support> supports) {
        return Sugar.parallelStream(supports, parallel)
                //.filter(s -> forbidden.stream().noneMatch(clash -> s.getSet().isSubsetOf(clash.getSet())))
                .filter(s -> isNotForbidden(s))
                .collect(Collectors.toList());
    }

    private boolean isNotForbidden(Support s) {
        BitSet bitset = s.getSubset().getBitset();
        int i = bitset.nextSetBit(0);
        if (i != -1) {
            while (true) {
                if (++i < 0) break;
                if ((i = bitset.nextSetBit(i)) < 0) break;
                int endOfRun = bitset.nextClearBit(i);
                do {
                    for (Support support : forbidden.get(i)) {
                        if (support.getSubset().isSubsetOf(bitset)) {
                            return false;
                        }
                    }
                }
                while (++i != endOfRun);
            }
        }
        return true;
    }

    private boolean isForbiddenT(Term[] terms) {// tohle je trochu pres hlavu kdyz uz je ta opt verze forbidden free
        Set<Constant> constants = Sugar.set();
        for (Term term : terms) { // we know, everything is grounded
            constants.add((Constant) term);
        }
        return isForbiddenS(constantsToSubset(constants));
    }

    private boolean isForbiddenS(Subset subset) {
        //return Sugar.parallelStream(forbidden, parallel).anyMatch(set -> subset.isSubsetOf(set.getSet()));
        //return Sugar.parallelStream(forbidden, parallel).anyMatch(set -> set.getSet().isSubsetOf(subset));  //todo tohle napsat rychleji
        //return forbidden.stream().anyMatch(set -> itf2(set, subset));  //todo tohle napsat rychleji
        /*for (Support support : forbidden) {
            if (support.getSet().isSubsetOf(subset)) {
                return true;
            }
        }
        return false;
        */
        BitSet bitset = subset.getBitset();
        int i = bitset.nextSetBit(0);
        if (i != -1) {
            while (true) {
                if (++i < 0) break;
                if ((i = bitset.nextSetBit(i)) < 0) break;
                int endOfRun = bitset.nextClearBit(i);
                do {
                    for (Support support : forbidden.get(i)) {
                        if (support.getSubset().isSubsetOf(bitset)) {
                            return true;
                        }
                    }
                }
                while (++i != endOfRun);
            }
        }
        return false;
    }

    private boolean itf2(Support set, Subset subset) {
        return set.getSubset().isSubsetOf(subset);
    }

    private boolean isForbiddenB(BitSet bitset) {
        //return Sugar.parallelStream(forbidden, parallel).anyMatch(set -> set.getSet().isSubsetOf(bitset));
        //return forbidden.stream().anyMatch(set -> ift1(set, bitset));
        /*for (Support support : forbidden) {
            if (support.getSet().isSubsetOf(bitset)) {
                return true;
            }
        }
        return false;
        */

        int i = bitset.nextSetBit(0);
        if (i != -1) {
            while (true) {
                if (++i < 0) break;
                if ((i = bitset.nextSetBit(i)) < 0) break;
                int endOfRun = bitset.nextClearBit(i);
                do {
                    for (Support support : forbidden.get(i)) {
                        if (support.getSubset().isSubsetOf(bitset)) {
                            return true;
                        }
                    }
                }
                while (++i != endOfRun);
            }
        }
        return false;
    }

    private boolean ift1(Support set, BitSet bitset) {
        return set.getSubset().isSubsetOf(bitset);
    }


    // Ondra has an idea of multimap<constant,support> which would make faster retrieving of getting the supports of set subset of constant
    private Collection<Support> minimal(Collection<Support> sup, Set<Support> supports) {
        List<Support> retVal = Sugar.list();
        List<Support> all = Sugar.list();
        all.addAll(sup);
        if (null != supports) {
            all.addAll(supports);
        }
        Collections.sort(all, Comparator.comparingInt(Support::size));
        for (int outer = all.size() - 1; outer >= 0; outer--) {
            Subset outerSubset = all.get(outer).getSubset();
            boolean nothingSubsumesMe = true;
            for (int inner = 0; inner < outer && nothingSubsumesMe; inner++) {
                nothingSubsumesMe = nothingSubsumesMe && !all.get(inner).getSubset().isSubsetOf(outerSubset);
            }
            if (nothingSubsumesMe) {
                retVal.add(all.get(outer));
            }
        }

        return retVal;
    }

    private Pair<List<Support>, List<Support>> minimal(List<Support> supports) {
        // return pair of minimals and the one removed from the initial list to get the minimal
        List<Support> minimal = new LinkedList<>();
        List<Support> subsumed = new LinkedList<>();
        Collections.sort(supports, Comparator.comparingInt(Support::size));
        for (int outer = supports.size() - 1; outer >= 0; outer--) {
            Subset outerSubset = supports.get(outer).getSubset();
            boolean nothingSubsumesMe = true;
            for (int inner = 0; inner < outer && nothingSubsumesMe; inner++) {
                nothingSubsumesMe = nothingSubsumesMe && !supports.get(inner).getSubset().isSubsetOf(outerSubset);
            }
            if (nothingSubsumesMe) {
                minimal.add(supports.get(outer));
            } else {
                subsumed.add(supports.get(outer));
            }
        }

        return new Pair<>(minimal, subsumed);
    }

    // old implementation, just optimise it as well
    /*private Set<Support> cartesian(Set<Support> supports, Set<Support> previous) {//}, long iteration) {
        // supports is either empty (null in the current world) or a set of supports
        // previous contains cartesian product of the previous literals
        Set<Support> retVal = Sugar.set();
        if (null == previous || previous.isEmpty()) {
            for (Support support : supports) {
                if (support.size() <= k) {
                    retVal.add(support);
                }
            }
        } else {
            for (Support support : supports) {
                for (Support old : previous) {
                    Support union = union(support, old);
                    if (union.size() <= k) {
                        retVal.add(union);
                    }
                }
            }
        }
        return retVal;
    }*/

    // opt version
    private Set<Support> cartesian(Set<Support> supports, Set<Support> previous) {//}, long iteration) {
        // supports is either empty (null in the current world) or a set of supports
        // previous contains cartesian product of the previous literals
        Set<BitSet> retVal = Sugar.set();
        if (null == previous || previous.isEmpty()) {
//            System.out.println("vstup 1\t" + supports.size() + "\t\t" + supports);
            for (Support support : supports) {
                if (support.size() <= k) {
                    //retVal.add(new Pair<>(support.getLiterals(), support.getSet().getBitset()));
                    retVal.add(support.getSubset().getBitset());
                }
            }
        } else {
//            System.out.println("vstup 2\t" + supports.size() + "\t\t" + supports + "\n\t\t" + previous.size() + "\t\t" + previous);
            for (Support support : supports) {
                for (Support old : previous) {
                    //Pair<Set<Literal>, BitSet> union = new Pair<>(Sugar.union(support.getLiterals(), old.getLiterals()), union(support.getSet().getBitset(), old.getSet().getBitset()));
                    BitSet union = union(support.getSubset().getBitset(), old.getSubset().getBitset());
                    //Support union = union(support, old);
                    if (union.cardinality() <= k) {
                        retVal.add(union);
                    }
                }
            }
        }

        return retVal.stream().map(bitset -> {
            Subset subset = SubsetFactory.getInstance().get(bitset);
            Support support = supportCache.get(subset);
            if (null == support) {
                support = Support.create(subset);
                supportCache.put(subset, support);
            }
            return support;
        }).collect(Collectors.toSet());
    }

    // ultra opt version
    private Set<BitSet> cartesianOpt(Set<Support> supports, Set<BitSet> previous) {//}, long iteration) {
        // supports is either empty (null in the current world) or a set of supports
        // previous contains cartesian product of the previous literals
        Set<BitSet> retVal = Sugar.set();
        if (null == previous || previous.isEmpty()) {
            for (Support support : supports) {
                if (support.size() <= k) {
                    retVal.add(support.getSubset().getBitset());
                }
            }
        } else {
            for (Support support : supports) {
                BitSet newOne = support.getSubset().getBitset();
                for (BitSet old : previous) {
                    BitSet union = union(newOne, old);
                    if (union.cardinality() <= k) {
                        retVal.add(union);
                    }
                }
            }
        }
        return retVal;
  /*return retVal.stream().map(bitset -> {
            Subset subset = SubsetFactory.getInstance().get(bitset);
            Support support = supportCache.get(subset);
            if (null == support) {
                support = Support.get(subset);
                supportCache.put(subset, support);
            }
            return support;
        }).collect(Collectors.toSet());*/
    }


    private BitSet union(BitSet b1, BitSet b2) {
        BitSet copy = (BitSet) b1.clone();
        copy.or(b2);
        return copy;
    }

    private Set<Support> supports(Literal literal) {
        Set<Support> support = map.get(literal);
        if (null == support) {
            return Sugar.set(); // just a work around
        }
        return support;
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

        map.entrySet().stream().forEach(e -> retVal.add(e.getKey(), Sugar.chooseOne(e.getValue()).getSubset()));
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

    public static SupportsHolderOpt5 create(Set<Literal> evidence, int k, Map<Pair<String, Integer>, Integer> predicatesToBitset, boolean useExtraPruning) {
        List<Constant> constants = LogicUtils.constants(evidence).stream().collect(Collectors.toList());

        Map<Constant, Subset> constantToSubset = new HashMap<>();
        IntStream.range(0, constants.size()).forEach(idx -> constantToSubset.put(constants.get(idx), SubsetFactory.getInstance().get(constants.size(), idx)));

        Map<Subset, Support> supportCache = new ConcurrentHashMap<>();
        Map<Literal, Set<Support>> map = new ConcurrentHashMap<>();
        //Map<Literal, Head> heads = new ConcurrentHashMap<>();

        for (Literal literal : evidence) {
            Subset subset = constantsToSubset(LogicUtils.constantsFromLiteral(literal), constantToSubset);
            Support support = supportCache.get(subset);
            if (null == support) {
                support = Support.create(subset);
                supportCache.put(subset, support);
            }
            map.put(literal, Sugar.set(support));
            support.addLiteral(literal);
            //heads.put(literal, Head.get(literal, subset));
        }

        return new SupportsHolderOpt5(k, evidence, constantToSubset, constants, map, //heads,
                supportCache, predicatesToBitset, useExtraPruning);
    }

    private Subset constantsToSubset(Literal literal) {
        Set<Constant> constants = Sugar.set();
        for (Term term : literal.terms()) {
            constants.add((Constant) term);
        }
        return constantsToSubset(constants);
    }

    private Subset constantsToSubset(Set<Constant> constants) {
        Subset retVal = constantsToSubset.get(constants);
        if (null != retVal) {
            return retVal;
        }
        retVal = SubsetFactory.getInstance().union(constants.stream().map(c -> constantToSubset.get(c)).collect(Collectors.toList()));
        constantsToSubset.put(constants, retVal);
        return retVal;
    }

    public static Subset constantsToSubset(Set<Constant> constants, Map<Constant, Subset> map) {
        Subset retVal = map.get(constants);
        if (null != retVal) {
            return retVal;
        }

        for (Constant constant : constants) {
            Subset subset = map.get(constant);
            if (null == retVal) {
                retVal = subset;
            } else {
                retVal = retVal.union(subset);
            }
        }

        return retVal;
    }

    // returns true if another iteration is needed
    public boolean constraintsCheck(List<Clause> negatedConstraints, long iteration) {
        // tady udelat nejakou chytristiku, nejake proriznuti jenom na to co opravdu musime otestovat
        //Set<Support> testingBecauseNewHeads = needToBeTestedAsWell(newOnes); tohle je spatne, je to marny

        List<Clause> filteredNegatedConstraints = new LinkedList<>();
        for (Clause negatedConstraint : negatedConstraints) {
            for (Literal literal : negatedConstraint.literals()) {
                if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                    continue;
                }
                if (this.changedPredicatesInLastIteration.get(this.predicatesIdx.get(literal.getPredicate()))) {
                    filteredNegatedConstraints.add(negatedConstraint);
                    break;
                }
            }
        }

        if (!filteredNegatedConstraints.isEmpty()) {
            // 0 == iteration is here because of possible calls from kPL entailment with re-usage of the holder
            checkAdding(filteredNegatedConstraints, Sugar.set(), 0 == iteration);
        }
        //gcLike(violatedSupports);


        return !this.change // this.change is true if some change occurred in the forward phase
                && map.keySet().size() == this.sizeOfWorld
                && supportsInWorld() == this.sizeOfSupports;
    }


    private void checkAdding(List<Clause> negatedConstraints, Set<Support> testingBecauseNewHeads, boolean checkAllSupports) {
        // I have to go over all supports and for supersets of the ones that induce the brand new literals (i.e. non-entailed in the previous run),
        // ok, here I mostly go through the newly added supports only.... and then, if some one introduces inconsistency, then remove it and all its supersets (having find them first)
        /*Set<Support> selectedSupports = (testingBecauseNewHeads.isEmpty()) ? this.supportsOfBrandNewHeads : Sugar.union(this.supportsOfBrandNewHeads, testingBecauseNewHeads);
        if (checkAllSupports) {
            selectedSupports = this.map.values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        // well, it is the above really sound
//        Set<Support> supports = this.map.values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new)); // tak timhle to ocividne neni
        */
        Set<Support> removeSupports = this.map.values().stream()
                .flatMap(Collection::stream).distinct()
                .filter(support -> {
                    if (this.supportsOfBrandNewHeads.parallelStream().noneMatch(newSupport -> newSupport.getSubset().isSubsetOf(support.getSubset()))) {
                        return false;
                    }
                    Matching world = Matching.create(new Clause(mask(support)), Matching.THETA_SUBSUMPTION);
                    for (Clause constraint : negatedConstraints) {
                        if (world.subsumption(constraint, 0)) {
                            // wanna remove this support
                            return true;
                        }
                    }
                    return false;
                }).collect(Collectors.toCollection(LinkedHashSet::new));

        // removing following supports
        for (Support support : removeSupports) {
            addToForbidden(support);
            Set<Literal> nonRemoved = ConcurrentHashMap.newKeySet();
            for (Literal literal : support.getLiterals()) {
                if (evidence.contains(literal)) {
                    // this can be done more efficiently
                    if (support.getSubset().isSubsetOf(constantsToSubset(literal))) { // in this case, constants(literal) is equal to support
                        nonRemoved.add(literal);
                        continue;
                    }
                }
                Set<Support> sups = map.get(literal);
                sups.remove(support);
                if (sups.isEmpty()) {
                    map.remove(literal);
                }
                // here, we have to take care of the heads that thier supports in this.changedLiteralsInLastIteration
                Set<Support> lastIteration = this.changedLiteralsInLastIteration.get(literal);
                if (null != lastIteration) {
                    lastIteration.remove(support);
                    if (lastIteration.isEmpty()) {
                        this.changedLiteralsInLastIteration.remove(literal);
                    }
                    this.brandNewHeads.remove(literal);
                }
            }
            if (support.getLiterals().isEmpty()) {
                supportCache.remove(support.getSubset());
            } else {
                support.setLiterals(nonRemoved);
            }
        }

    }

    private void addToForbidden(Support support) {
        BitSet bitset = support.getSubset().getBitset();
        int i = bitset.nextSetBit(0);
        if (i != -1) {
            while (true) {
                if (++i < 0) break;
                if ((i = bitset.nextSetBit(i)) < 0) break;
                int endOfRun = bitset.nextClearBit(i);
                do {
                    this.forbidden.get(i).add(support);
                }
                while (++i != endOfRun);
            }
        }
    }

    private Iterable<? extends Literal> mask(Support support) {
        return map.entrySet().parallelStream()
                .filter(entry -> entry.getValue().stream().anyMatch(literalSupport -> literalSupport.getSubset().isSubsetOf(support.getSubset()))) // bude tohle fungovat spravne?
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void gcLike(List<Support> violatedSupports) {
        for (Support support : violatedSupports) {
            supportCache.remove(support);
        }

    }

    public void debugIt() {
        System.out.println("forbidden");
        //this.forbidden.stream().map(this::toCanon).sorted().forEach(p -> System.out.println("\t" + p));

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

    public List<BitSet> setsToUnions(List<Set<Support>> supports) {
        List<BitSet> retVal = new ArrayList<>(supports.size());
        for (Set<Support> support : supports) {
            BitSet bs = new BitSet(this.constantToSubset.size());
            for (Support sup : support) {
                bs.or(sup.getSubset().getBitset());
            }
            retVal.add(bs);
        }
        return retVal;
    }

    public BitSet toUnion(Set<BitSet> bts) {
        BitSet retVal = new BitSet(this.constantToSubset.size());
        for (BitSet bt : bts) {
            retVal.or(bt);
        }
        return retVal;
    }

    public void substituteStatefullyPreparation(Term[] variables) {
        for (int idx = 0; idx < variables.length; idx++) {
            ((Variable) variables[idx]).setStatefullIndex(idx);
        }
    }
}

