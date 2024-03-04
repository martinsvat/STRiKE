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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 15. 10. 2018.
 */
public class SupportsHolderOpt4 {


    private final Map<Constant, Subset> constantToSubset;
    private final List<Constant> evidenceConstants;
    private final Map<Set<Constant>, Subset> constantsToSubset;
    private final Set<Literal> evidence;
    public final Map<Literal, Set<Support>> map; // public just for debug
    private final int k;
    private final Map<Subset, Support> supportCache;
    private final Set<Support> forbidden;
    private final Map<Pair<String, Integer>, Integer> predicatesIdx;
    private BitSet changedPredicatesInLastIteration;
    private BitSet changedPredicatesInThisIteration;
    private Set<Support> currentSupport;
    private final Map<Literal, Subset> literalsCache;
    private long sizeOfSupports = 0;
    private int sizeOfWorld;
    private final boolean parallel = true;
    private boolean change;
    private Map<Literal, Set<Support>> changedLiteralsInLastIteration;

    {
        // TODO
//        System.out.println("SupportsHolder -- todo: namisto metody mask pouzit nejakou jinou datovou strukturu aby i support mel seznam literalu ktere uvozuje");
        //TODO nekde je tu bug, kdyz neni zapnuta cache na supporty tak to obcas se asi muze stat ze jsou dva supporty rozdilne objekty a potom to pada protoze se nekde operuje s nullem... coz je zlvastni
    }

    public SupportsHolderOpt4(int k, Set<Literal> evidence, Map<Constant, Subset> constantToSubset, List<Constant> evidenceConstants, Map<Literal, Set<Support>> map, Map<Subset, Support> supportCache, Map<Pair<String, Integer>, Integer> predicatesIdx) {
        System.out.println("sho4");
        this.k = k;
        this.evidence = evidence;
        this.constantToSubset = constantToSubset;
        this.evidenceConstants = evidenceConstants;
        this.constantsToSubset = new HashMap<>();
        this.map = map;
        this.supportCache = supportCache;
        this.literalsCache = new ConcurrentHashMap<>();
        this.forbidden = ConcurrentHashMap.newKeySet();
        this.predicatesIdx = predicatesIdx;
        this.changedLiteralsInLastIteration = new HashMap<>();
        this.changedPredicatesInLastIteration = new BitSet(predicatesIdx.size());
        this.changedPredicatesInThisIteration = new BitSet(predicatesIdx.size());
    }

//    Set<String> debug = Sugar.set("Post_Quals(1:person217)", "Post_Quals(1:person155)", "Faculty(1:person101)", "Post_Quals(1:person391)");


    // pri forward pruchodu se berou do karteskeho soucinu uz supporty ktere jsou (tzn. i nejaky ze soucasne iterace tam muze zpropagovat se), ale constraints (forbidden) si s tim dystak poradi
    // the main point is to fill in currentSupport and currentHeads as these two are then used for constraints part; the rest fills in data into map, etc.
    // also to fill the newly computed into map
    public void forwardRules(List<Pair<Clause, Literal>> rulesWithNegated, long iteration, Set<Clause> newlyAddedRules) {
        this.changedPredicatesInLastIteration = this.changedPredicatesInThisIteration;
        this.changedPredicatesInThisIteration = new BitSet(predicatesIdx.size());
        this.sizeOfSupports = supportsInWorld();
        this.sizeOfWorld = map.keySet().size();
        this.currentSupport = Sugar.set();
        this.change = false;
        Map<Literal, Set<Support>> literalsWithChangedSupports = new HashMap<>();
        Matching world = Matching.create(new Clause(map.keySet()), Matching.THETA_SUBSUMPTION);

        int hcd = -1;
        if (SupportEntailmentInference.quickDebug) {
            hcd = map.keySet().hashCode();
            System.out.println("\nhcd\t" + hcd + "\t" + debugPrint().hashCode());
        }

        String wantedHead = SupportEntailmentInference.wantedHead;

        // complementary to this approach, we could collect violated rules (to Map<head,Literal<groundBodies>>) and to do all the minimal operation of one head in one place;
        // that would, however, need more and more memory
        for (Pair<Clause, Literal> preparedRule : rulesWithNegated) {
            if (0 == iteration && !newlyAddedRules.contains(preparedRule.r)) {
                // in this case the rule cannot add anything, thus we can skip computation of groundings of this rule
                continue;
            } else if (iteration > 0) {
                boolean anyChanged = false;
                for (Literal literal : preparedRule.getR().literals()) {
                    if (this.changedPredicatesInLastIteration.get(this.predicatesIdx.get(literal.getPredicate()))) {
                        anyChanged = true;
                        break;
                    }
                }
                if (!anyChanged) {
                    continue;
                }
            }

            boolean canUsePruning = 0 != iteration || !newlyAddedRules.contains(preparedRule.r);
            Pair<Term[], List<Term[]>> subts = world.allSubstitutions(preparedRule.r, 0, Integer.MAX_VALUE);
            for (Term[] terms : subts.s) {
                Literal head = LogicUtils.substitute(preparedRule.s, subts.r, terms);
                Clause groundBody = LogicUtils.substitute(preparedRule.r, subts.r, terms); // we work with range restricted rules only; otherwise we would need to generate the support by adding constants from the head, wouldn't we?

                //boolean beVerbouse = head.toString().equals(wantedHead);
//                boolean beVerbouse = head.toString().equals(wantedHead) || SupportEntailmentInference.wantedHeads.contains(head.toString());
                boolean beVerbouse = false;
                if (beVerbouse) {
                    System.out.println(head + "\t <- " + groundBody + "\t\t" + hcd);
                }

                // mozna je tady chyba v tom ze to neuvazuje nove hlavy ktere nejsou vubec jeste v mape :))

                // in place due to speed; this should do the work
                boolean allUntouched = true;
                boolean atLeastOneNonEvidence = false;
                if (canUsePruning) {
                    for (Literal literal : groundBody.literals()) {
                        boolean isSpecialPredicate = SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate());
                        if (isSpecialPredicate) {
                            continue;
                        }
                        atLeastOneNonEvidence = atLeastOneNonEvidence | (!this.evidence.contains(literal) && !isSpecialPredicate);
                        if (this.changedLiteralsInLastIteration.containsKey(literal)) {
                            allUntouched = false;
                            break;

                        }
                    }
                } else {
                    allUntouched = false; // we set this variable to false to say that we cannot use the pruning here (it would not be consistent)
                    // the reason for this is that this rule is newly added, therefore
                }

                // (allUntouched && atLeastOneNonEvidence && map.containsKey(head)) this is a condition, which when true, says that this particular support is already in map.get(head) and thus can be skipped
                if ((allUntouched && atLeastOneNonEvidence && map.containsKey(head)) || isForbidden(terms)) {
                    // daly by se forbidden vnutit pri hledani ground substituci???? a slo by to vubec?
/*                    if (beVerbouse) {
                        System.out.println("\tforbidden\t" + isForbidden(terms) + "\t|\t" + allUntouched + " & " + atLeastOneNonEvidence + "\t\t" + map.containsKey(head));
                    }
*/
                    continue;
                }

                Set<Support> sup = Sugar.set();
                for (Literal literal : groundBody.literals()) {
                    if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                        continue;
                    }
                    sup = cartesian(supports(literal), sup);
                    if (sup.isEmpty()) {
                        if (beVerbouse) {
                            System.out.println("end because cart is empty");
                        }
                        break;
                    }
                }

                if (sup.isEmpty()) {
                    continue;
                }

                //spocitat nejdrive minimalni ze sups
                List<Support> newSupports = minimal(forbiddenFree(sup)); // aka the original ones
                Set<Support> previousSupports = map.get(head);
                Set<Support> toRemove = new HashSet<>(null == previousSupports ? 0 : previousSupports.size());
                List<Support> toAdd = Sugar.list();

                if (sup.isEmpty()) {
                    continue;
                }

                //potom zkontrolovat jejich minimalitu vuci previousSupports
                if (null == previousSupports) {
                    Set<Support> changeLits = literalsWithChangedSupports.get(head);
                    if (null == changeLits) {
                        changeLits = Sugar.set();
                        literalsWithChangedSupports.put(head, changeLits);
                    }
                    changeLits.addAll(newSupports);
                    // in-place store
                    this.changedPredicatesInThisIteration.set(this.predicatesIdx.get(head.getPredicate()));
                    this.change = true;
                    this.map.put(head, Sugar.setFromCollections(newSupports));
                    for (Support support : newSupports) {
                        support.addLiteral(head);
                    }

                    if (beVerbouse) {
                        List<Support> minimals = newSupports;
                        System.out.println(head + "\t" + groundBody.hashCode() + "\t" + "-1" + "\t" + minimals.size());
                        minimals.stream().map(this::toCanon).forEach(p -> System.out.println("\t" + p));
                    }

                } else {
                    for (Support newOne : newSupports) {
                        boolean someoneSubsumesMe = false;
                        for (Support oldOne : previousSupports) {
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

                    Set<Support> debugPreviousSize = previousSupports;
                    if (!toRemove.isEmpty() || !toAdd.isEmpty()) {
                        this.changedPredicatesInThisIteration.set(this.predicatesIdx.get(head.getPredicate())); // mozna tohle hodit napsat pres nejakou metodu aby to bylo trochu prehlednejsi
                        Set<Support> changeLits = literalsWithChangedSupports.get(head);
                        if (null == changeLits) {
                            changeLits = Sugar.set();
                            literalsWithChangedSupports.put(head, changeLits);
                        }
                        changeLits.addAll(toAdd);
                        // in-place store
                        this.changedPredicatesInThisIteration.set(this.predicatesIdx.get(head.getPredicate()));
                        this.change = true;
                        for (Support support : toRemove) {
                            previousSupports.remove(support);
                            support.removeHead(head);
                        }
                        for (Support support : toAdd) {
                            previousSupports.add(support);
                            support.addLiteral(head);
                        }
                    }
                    if (beVerbouse) {
                        Set<Support> minimals = previousSupports;
                        System.out.println(head + "\t" + groundBody.hashCode() + "\t" + debugPreviousSize + "\t" + minimals.size());
                        minimals.stream().map(this::toCanon).forEach(p -> System.out.println("\t" + p));
                    }

                }
            }
        }
        this.changedLiteralsInLastIteration = literalsWithChangedSupports;
    }

    public String toCanon(Support s) { // public just for debug
        return "{" + constantToSubset.entrySet().stream()
                .filter(entry -> entry.getValue().isSubsetOf(s.getSubset()))
                .map(entry -> entry.getKey().toString())
                .sorted().collect(Collectors.joining(", "))
                + "}";
    }

    private long supportsInWorld() {
        return map.values().stream().flatMap(Collection::stream).distinct().count();
    }

    private List<Support> forbiddenFree(Set<Support> supports) {
        return Sugar.parallelStream(supports, parallel)
                //.filter(s -> forbidden.stream().noneMatch(clash -> s.getSet().isSubsetOf(clash.getSet())))
                .filter(s -> forbidden.stream().noneMatch(clash -> clash.getSubset().isSubsetOf(s.getSubset())))
                .collect(Collectors.toList());
    }

    private boolean isForbidden(Term[] terms) {
        Set<Constant> constants = Sugar.set();
        for (Term term : terms) { // we know, everything is grounded
            constants.add((Constant) term);
        }
        return isForbidden(constantsToSubset(constants));
    }

    private boolean isForbidden(Subset subset) {
        //return Sugar.parallelStream(forbidden, parallel).anyMatch(set -> subset.isSubsetOf(set.getSet()));
        return Sugar.parallelStream(forbidden, parallel).anyMatch(set -> set.getSubset().isSubsetOf(subset));
    }

    private Subset literalToSubset(Literal literal) {
        return constantsToSubset(LogicUtils.constantsFromLiteral(literal));
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

    private List<Support> minimal(List<Support> supports) {
        List<Support> retVal = Sugar.list();
        Collections.sort(supports, Comparator.comparingInt(Support::size));
        for (int outer = supports.size() - 1; outer >= 0; outer--) {
            Subset outerSubset = supports.get(outer).getSubset();
            boolean nothingSubsumesMe = true;
            for (int inner = 0; inner < outer && nothingSubsumesMe; inner++) {
                nothingSubsumesMe = nothingSubsumesMe && !supports.get(inner).getSubset().isSubsetOf(outerSubset);
            }
            if (nothingSubsumesMe) {
                retVal.add(supports.get(outer));
            }
        }

        return retVal;
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

    private BitSet union(BitSet b1, BitSet b2) {
        BitSet copy = (BitSet) b1.clone();
        copy.or(b2);
        return copy;
    }

    private Support union(Support support, Support old) {
        return cacheSupport(support.merge(old));
    }

    private Support cacheSupport(Support support) {
        if (true) {
            Support cached = supportCache.get(support.getSubset());
            if (null == cached) {
                supportCache.put(support.getSubset(), support);
                return support;
            }
            /*if (cached.getLiterals().size() != support.getLiterals().size() || !cached.getLiterals().equals(support.getLiterals())) {
                System.out.println("a bug is somewhere"); // tohle je uz nejspise opravene

                System.out.println("cached\n\t" + cached.getLiterals() + "\n\t" + cached.getSet());
                System.out.println("given\n\t" + support.getLiterals() + "\n\t" + support.getSet());
                throw new IllegalStateException();
            }*/

            return cached;
        } else {
            return support;
        }
    }

    private Set<Support> supports(Literal literal) {
        Set<Support> support = map.get(literal);
        if (null == support) {
            return Sugar.set(); // just a work around
        }
        return support;
    }

    //List<Literal> debug = Sugar.list("advisedBy(1:person402, 1:person235)", "Faculty_adjunct(1:person235)").stream().map(Literal::parseLiteral).collect(Collectors.toList());

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

    public static SupportsHolderOpt4 create(Set<Literal> evidence, int k, Map<Pair<String, Integer>, Integer> predicatesToBitset) {
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

        return new SupportsHolderOpt4(k, evidence, constantToSubset, constants, map, //heads,
                supportCache, predicatesToBitset);
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

    public boolean constraintsCheck(List<Clause> negatedConstraints, long iteration) {
        // tady udelat nejakou chytristiku, nejake proriynuti jenom na to co opravdu musime otestovat

        //Set<Subset> newOnes = this.currentSupport.stream().map(Support::getSet).collect(Collectors.toSet());
        //Set<Support> testingBecauseNewHeads = needToBeTestedAsWell(newOnes); tohle je spatne, je to marny
        //Set<Support> testingBecauseNewHeads = needToBeTestedAsWell(newOnes);

        //System.out.println("tested because of newHeads\n");
        //testingBecauseNewHeads.stream().forEach(s -> System.out.println(s));

        //checkAdding(negatedConstraints, testingBecauseNewHeads);

        List<Clause> filteredNegatedConstraints = new LinkedList<>();
        for (Clause negatedConstraint : negatedConstraints) {
            for (Literal literal : negatedConstraint.literals()) {
                if (this.changedPredicatesInThisIteration.get(this.predicatesIdx.get(literal.getPredicate()))) {
                    filteredNegatedConstraints.add(negatedConstraint);
                    break;
                }
            }
        }


        if (!filteredNegatedConstraints.isEmpty()) {
            // 0 == iteration is here because of possible calls from kPL entailment with reusages of the holder
            checkAdding(filteredNegatedConstraints, Sugar.set(), 0 == iteration);
        }
        //gcLike(violatedSupports);


//        System.out.println("\nafter constraints check");
//        this.map.keySet().stream().map(Object::toString).sorted().forEach(System.out::println);
//        System.out.println("\n");

//        System.out.println("diffs\n\t" + this.sizeOfWorld + "\t" + map.keySet().size() + "\n\t" + this.sizeOfSupports + "\t" + supportsInWorld());

//        System.out.println("world size after constraints check\t" + this.map.keySet().size());

        return !this.change // this.change is true if some change occurred in the forward phase
                && map.keySet().size() == this.sizeOfWorld
                && supportsInWorld() == this.sizeOfSupports;
    }

    private Set<Support> needToBeTestedAsWell(Set<Subset> subsets) {
        System.out.println("je tohle naimplementovany spravne?");
        return this.map.entrySet().stream()
                .filter(e -> isSubsetOfSome(e.getKey(), subsets))
                .flatMap(e -> e.getValue().stream())
                .collect(Collectors.toSet());
    }

    private boolean isSubsetOfSome(Literal key, Set<Subset> subsets) {
        return Sugar.parallelStream(subsets, parallel).anyMatch(subset -> isSubsetOf(key, subset));
    }

    private boolean isSubsetOf(Literal literal, Subset subset) {
        Subset cache = literalsCache.get(literal);
        if (null == cache) {
            cache = constantsToSubset(LogicUtils.constantsFromLiteral(literal));
            literalsCache.put(literal, cache);
        }
        return cache.isSubsetOf(subset);
    }

//    Set<Clause> debugGroundConstraints = Sugar.set("!tempAdvisedBy(1:person299, 1:person235), !Faculty_adjunct(1:person235)", "!Faculty_adjunct(1:person235), !Faculty(1:person235)", "!Post_Generals(1:person175), !tempAdvisedBy(1:person175, 1:person107)", "!advisedBy(1:person100, 1:person235), !taughtBy(4:course139, 1:person235, 5:winter_0203), !Faculty_adjunct(1:person235)", "!advisedBy(1:person402, 1:person235), !taughtBy(4:course139, 1:person235, 5:winter_0203), !Faculty_adjunct(1:person235)", "!taughtBy(4:course139, 1:person235, 5:winter_0203), !Faculty_adjunct(1:person235), !publication(3:title294, 1:person235)", "!publication(3:title108, 1:person235), !taughtBy(4:course139, 1:person235, 5:winter_0203), !Faculty_adjunct(1:person235)").stream().map(Clause::parse).collect(Collectors.toSet());

    private void checkAdding(List<Clause> negatedConstraints, Set<Support> testingBecauseNewHeads, boolean checkAllSupports) {
        Set<Support> removeSupports = Sugar.set();

//        System.out.println("\ndebug outprint");
//        this.map.keySet().stream().sorted(Comparator.comparing(Object::toString))
//                .filter(l -> !this.evidence.contains(l))
//                .forEach(l -> System.out.println(l + "\t" + this.map.get(l).stream().map(this::toCanon).sorted().collect(Collectors.joining("\t"))));
//        System.out.println("debug outprinted\n");

        Set<Support> supports = (testingBecauseNewHeads.isEmpty()) ? this.currentSupport : Sugar.union(this.currentSupport, testingBecauseNewHeads);
        if (checkAllSupports) {
            supports = this.map.values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
        }
//        Set<Support> supports = this.map.values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new)); // tak timhle to ocividne neni

        System.out.println("vytahni constraints ven, udelej filtr pred touhle funkci ad se vi jestli se vubec musi ty constraint zkouset ;)");

        for (Support support : supports) {
            Matching world = Matching.create(new Clause(mask(support)), Matching.THETA_SUBSUMPTION);

//            System.out.println(support + "\t\t" + (new Clause(mask(support))));
            for (Clause constraint : negatedConstraints) {
                if (world.allSubstitutions(constraint, 0, 1).getS().size() > 0) {
                    // wanna remove this support
                    //System.out.println("wanna remove\t" + toCanon(support) + "\tbecause of\t" + constraint);
                    removeSupports.add(support);
                    break;
                }

                /*Pair<Term[], List<Term[]>> subt = world.allSubstitutions(constraint, 0, Integer.MAX_VALUE);
                if (subt.getS().size() > 0) {
                    // wanna remove this support
                    System.out.println("wanna remove\t" + toCanon(support) + "\tbecause of\t" + constraint);
                    for (Term[] terms : subt.s) {
                        System.out.println("\t" + LogicUtils.substitute(constraint, subt.r, terms));
                    }
                    removeSupports.add(support);
                    break;
                }*/
            }
        }

//        System.out.println("\nremoving following supports");
        for (Support support : removeSupports) {
//            System.out.println("adding to forbiddens\t" + toCanon(support));
            this.forbidden.add(support);
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

                //
            }

            if (support.getLiterals().isEmpty()) {
                supportCache.remove(support.getSubset());
            } else {
                support.setLiterals(nonRemoved);
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
}

