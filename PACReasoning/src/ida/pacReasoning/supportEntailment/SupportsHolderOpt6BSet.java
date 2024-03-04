package ida.pacReasoning.supportEntailment;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialBinaryPredicates;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.collectors.KECollector;
import ida.pacReasoning.supportEntailment.speedup.BSet;
import ida.pacReasoning.supportEntailment.speedup.BSupport;
import ida.pacReasoning.supportEntailment.speedup.FastSubsetFactory;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Version with bruteforce forbidden.
 * <p>
 * Created by martin.svatos on 15. 10. 2018.
 */
public class SupportsHolderOpt6BSet {


    private final Map<Constant, BSet> constantToSubset;
    private final Map<Constant, Integer> constantToInteger;
    private final List<Constant> evidenceConstants;
    private final Map<Set<Constant>, BSet> constantsToSubset;
    private final Set<Literal> evidence;
    private final Map<Literal, Set<BSupport>> map;
    private final int k;
    private final Map<BSet, BSupport> supportCache;
    private final Set<BSupport> forbidden;

    private final Map<Pair<String, Integer>, Integer> predicatesIdx;
    private final boolean useExtraPruning;
    private final boolean allPreviousOnly;
    private final boolean evidenceBased;
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

//    {
//        System.out.println("old version\t" + this.oldVersion + "\t ! nová verze ale musí být porovnaná na menších datech jestli dává stejné výsledky!");
//    }

    {
        // TODO
//        System.out.println("SupportsHolder -- todo: namisto metody mask pouzit nejakou jinou datovou strukturu aby i support mel seznam literalu ktere uvozuje");
        //TODO nekde je tu bug, kdyz neni zapnuta cache na supporty tak to obcas se asi muze stat ze jsou dva supporty rozdilne objekty a potom to pada protoze se nekde operuje s nullem... coz je zlvastni
    }

    public SupportsHolderOpt6BSet(int k, Set<Literal> evidence, Map<Constant, BSet> constantToSubset, List<Constant> evidenceConstants, Map<Literal, Set<BSupport>> map, Map<BSet, BSupport> supportCache, Map<Pair<String, Integer>, Integer> predicatesIdx, boolean useExtraPruning, Map<Constant, Integer> constantToInteger) {
        this.k = k;
        this.evidence = evidence;
        this.constantToInteger = constantToInteger;
        this.constantToSubset = constantToSubset;
        this.evidenceConstants = evidenceConstants;
        this.constantsToSubset = new HashMap<>();
        this.map = map;
        this.supportCache = supportCache;
        this.forbidden = ConcurrentHashMap.newKeySet();
        this.predicatesIdx = predicatesIdx;
        this.changedLiteralsInLastIteration = new HashMap<>();
        this.changedPredicatesInLastIteration = new BitSet(predicatesIdx.size());
        this.useExtraPruning = useExtraPruning;
        this.brandNewHeads = Sugar.set();
        // testing of this combination was slower on UMLS than without it
        this.allPreviousOnly = false;//System.getProperty("devMod").contains("APO");

        this.forbiddensSize = 0l;
        this.forbiddens = new ConcurrentHashMap<>();

        this.evidenceBased = true;
        //System.out.println("forbid derivation of evidence\t" + this.evidenceBased);
    }

    // pri forward pruchodu se berou do karteskeho soucinu uz supporty ktere jsou (tzn. i nejaky ze soucasne iterace tam muze zpropagovat se), ale constraints (forbidden) si s tim dystak poradi
    // the main point is to fill in supportsOfBrandNewHeads and currentHeads as these two are then used for constraints part; the rest fills in data into map, etc.
    // also to fill the newly computed into map
    public void forwardRules(List<Triple<Clause, Literal, BSet>> rulesWithNegated, long iteration, Set<Triple<Clause, Literal, BSet>> newlyAddedRules) {

        /* dev info!
        if (0 == iteration) {
            System.out.println("map\t" + map.keySet().size() + "\t"
                    + map.values().stream().flatMap(Collection::stream).distinct().count() + "\t"
                    + map.values().stream().mapToInt(Collection::size).sum());
            System.out.println("forbidden size\t" + this.forbidden.size());
        }*/

//        System.out.println("forbidden size\t" + this.forbiddensSize);
//        System.out.println("map\t" + map.keySet().size() + "\t" + sizeOfSupports);

        /*if (debugNow) {
            map.keySet().stream().map(l -> new Pair<Literal, String>(l, l.toString())).sorted(Comparator.comparing(Pair::getS))
                    .forEach(key -> {
                        System.out.println(key.getS());
                        map.get(key.getR()).stream().map(this::toCanon).sorted().forEach(x -> System.out.println("\t" + x));
                    });
        }*/

        List<String> debugs = Sugar.list();

        this.sizeOfWorld = map.keySet().size();
        this.supportsOfBrandNewHeads = Sugar.set();
        this.sizeOfSupports = supportsInWorld();
        this.change = false;
        BitSet changedPredicates = new BitSet(predicatesIdx.size());
        Map<Literal, Set<BSupport>> literalsWithSupportsToBeAdded = new HashMap<>();
        Matching world = Matching.create(new Clause(map.keySet()), Matching.THETA_SUBSUMPTION);
        Set<Literal> headsNeverSeenBefore = Sugar.set();


        // dev & debug
//        Set<BSet> asked = Sugar.set();
//        long askedFor = 0l;
        Map<BSet, Boolean> isNowForbidden = new HashMap<>();
//        Set<String> wantedCartesians = Sugar.set();
//        long wantedFor = 0l;

        // complementary to this approach, we could collect violated rules (to Map<head,Literal<groundBodies>>) and to do all the minimal operation of one head in one place;
        // that would, however, need more and more memory
        //boolean debug = false;
        int rule = 0;
        for (Triple<Clause, Literal, BSet> preparedRule : rulesWithNegated) {
            rule++;

            //kouknout jestli se vytvari v cartesian productu opravdu jenom ze supportu zmenenych v minulem beh

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
                    if (this.changedPredicatesInLastIteration.get(this.predicatesIdx.get(literal.getPredicate()))) { // TODO njn, ale tohle je na urovni predikatu nikoliv literalu
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


//            String debugStr = "issue_in(professional_or_occupational_group, occupation_or_discipline)";
            //String debugStr = "practices(professional_or_occupational_group, occupation_or_discipline)";
//            String debugStr = "associated_with(therapeutic_or_preventive_procedure, congenital_abnormality)";//"manages(health_care_related_organization, disease_or_syndrome)";
            //String debugStr = "";

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

                List<Literal> groundBody = LogicUtils.substituteStatefully(preparedRule.r, terms, true);
                // SubL is not faster than Sub
                /*substituteStatefullyPreparation(subts.r);
                Literal head = preparedRule.s.substituteStatefully(terms);
                //List<Literal> groundBody = LogicUtils.substituteStatefully(preparedRule.r, terms, true);
                List<Literal> groundBody = new ArrayList<>(preparedRule.r.countLiterals());
                */
                /* most likely this was outsourced for profiling
                for (Literal literal : preparedRule.r.literals()) {
                    if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                        continue;
                    }
                    groundBody.add(literal.substituteStatefully(terms));
                }*/

                //boolean debugThisIteration = false;
//                boolean debugThisIteration = head.toString().equals(debugStr);
//                if (debugThisIteration) {
//                    System.out.println(head);
//                    System.out.println("\t" + groundBody.stream().map(Literal::toString).sorted().collect(Collectors.joining(", ")));
//                    System.out.println(">>\t" + groundBody.size() + "\t" + preparedRule.r.literals().size());
//                    System.out.println(head.hashCode() + "\t" + groundBody.stream().map(Literal::toString).sorted().collect(Collectors.joining(", ")).hashCode());
//                }


                boolean allUntouched = true;
                boolean atLeastOneNonEvidence = false;
                boolean someBrandNew = false;
                int changedLiterals = 0;
                if (canUsePruning) {
                    for (Literal literal : groundBody) {
                        atLeastOneNonEvidence = atLeastOneNonEvidence || !this.evidence.contains(literal);
                        //allUntouched = allUntouched && !this.changedLiteralsInLastIteration.containsKey(literal);
                        if (this.changedLiteralsInLastIteration.containsKey(literal)) { // TODO ok, tohle je na urovni literalu
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

                /*if (badRule) {
                    System.out.println(allUntouched + "\t" + atLeastOneNonEvidence + "\t" + map.containsKey(head) + "\t||\t" + isForbiddenT(terms));
                }*/
//                if (debugThisIteration) {
//                    System.out.println(allUntouched + "\t" + atLeastOneNonEvidence + "\t" + map.containsKey(head) + "\t||\t" + isForbiddenT(terms));
//                }

                // (allUntouched && atLeastOneNonEvidence && map.containsKey(head)) this is a condition, which when true, says that this particular support is already in map.get(head) and thus can be skipped
                if ((allUntouched && atLeastOneNonEvidence && map.containsKey(head)) || isForbiddenT(terms)) {
                    continue;
                }
                boolean moreThanOneLiteralChanged = changedLiterals > 1;


                /*
                wantedFor++;
                wantedCartesians.add(groundBody.stream().map(literal -> {
                    Set<BSupport> support = supports(literal);
                    return "'" + support.stream().map(bSupport -> bSupport.getSet().toString()) + "'";
                }).sorted().collect(Collectors.joining("; ")));
                */
                boolean debugThisIteration = false;
                Set<BSet> rawSupports = computeCartesian(iteration, isNewlyAddedRule, literalsWithSupportsToBeAdded, preparedRule, groundBody, someBrandNew, moreThanOneLiteralChanged, debugThisIteration);
                // possible place for speedup
                //Set<BitSet> rawSupports = computeCartesianMinSort(iteration, newlyAddedRules, literalsWithSupportsToBeAdded, preparedRule, groundBody, someBrandNew, moreThanOneLiteralChanged);
                //Set<BitSet> rawSupports = computeCartesianCompelmentarySymmetry(iteration, newlyAddedRules, literalsWithSupportsToBeAdded, preparedRule, groundBody, someBrandNew, moreThanOneLiteralChanged);

//                if (debugThisIteration) {
//                    groundBody.stream().sorted(Comparator.comparing(Literal::toString)).
//                            forEach(l -> {
//                                System.out.println("- " + l);
//                                this.map.get(l).stream()
//                                        .map(x -> this.toCanon(x) + "\t" + x.getSet().stream().mapToObj(z -> "" + z).collect(Collectors.joining(", ")))
////                                        .map(x -> this.toCanon(x))
//                                        .sorted().forEach(x -> System.out.println("-- " + x));
//                            });
//                }

                /*
                askedFor += rawSupports.size();
                asked.addAll(rawSupports);
                */

                Set<BSupport> newSupports = toMinimalForbiddenFreeSupports(rawSupports, isNowForbidden);

//                if (debugThisIteration) {
//                    rawSupports.stream().map(x -> toCanon(x) + "\t" + x.stream().mapToObj(z -> "" + z).collect(Collectors.joining(", "))).sorted().forEach(x -> System.out.println("rs " + x));
//                    newSupports.stream().map(x -> toCanon(x) + "\t" + x.getSet().stream().mapToObj(z -> "" + z).collect(Collectors.joining(", "))).sorted().forEach(x -> System.out.println("ns " + x));
//                }

//                if (debugThisIteration) {
//                    System.out.println(rawSupports.size() + "\t" + newSupports.size());
//                }

                if (newSupports.isEmpty()) {
                    continue;
                }

                Set<BSupport> previousSupports = map.get(head);
                List<BSupport> toRemove = new LinkedList<>();
                List<BSupport> toAdd = Sugar.list();

//                if (debugThisIteration) {
//                    System.out.println("is evidence-based\t" + this.evidence.contains(head));
//                    System.out.println(preparedRule.s + "\t<-\t" + preparedRule.getR());
//                    System.out.println(head + "\t<-\t" + new Clause(groundBody));
//                    if (null != map.get(head)) {
//                        map.get(head).stream().map(x -> toCanon(x) + "\t" + x.getSet().stream().mapToObj(z -> "" + z).collect(Collectors.joining(", ")))
//                                .sorted()
//                                .forEach(x -> System.out.println("\t" + x));
//                    }
//                }

                if (null == previousSupports) { // i.e. no previous support for this head
//                    if (debugThisIteration) {
//                        System.out.println("no previous supports");
//                    }
                    Set<BSupport> changeLits = literalsWithSupportsToBeAdded.get(head);
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
                    for (BSupport newOne : newSupports) {
                        boolean someoneSubsumesMe = false;
                        for (BSupport oldOne : previousSupports) {
                            // note that both of these condition cannot happen at for one oldOne support, because then we would have non-minimal proofs in the previousSupports (i.e. there would be a circuit)
                            if (oldOne.isSubsetOf(newOne.getSet())) {
                                someoneSubsumesMe = true;
                                break;
                            } else if (newOne.isSubsetOf(oldOne.getSet())) {
                                toRemove.add(oldOne);
                            }
                        }
                        if (!someoneSubsumesMe) {
                            toAdd.add(newOne);
                        }
                    }

                    if (!toAdd.isEmpty()) { // !toRemove.isEmpty() implies this one, but the converse does not hold
                        changedPredicates.set(this.predicatesIdx.get(head.getPredicate()));
                        Set<BSupport> changeLits = literalsWithSupportsToBeAdded.get(head);
                        if (null == changeLits) {
                            changeLits = Sugar.set();
                            literalsWithSupportsToBeAdded.put(head, changeLits);
                        }
                        changeLits.addAll(toAdd);
                        // in-place removal of
                        for (BSupport support : toRemove) {
                            previousSupports.remove(support);
                            support.removeHead(head);
                            // here can be also deletion of supports with no heads (gc friendly)
                        }
                    }

//                    if (debugThisIteration) {
//                        System.out.println("to remove");
//                        toRemove.stream().map(x -> toCanon(x) + x.getSet().stream().mapToObj(z -> "" + z).collect(Collectors.joining(", "))).sorted().forEach(i -> System.out.println("\t" + i));
//                        System.out.println("to add");
//                        toAdd.stream().map(x -> toCanon(x) + x.getSet().stream().mapToObj(z -> "" + z).collect(Collectors.joining(", "))).sorted().forEach(i -> System.out.println("\t" + i));
//                    }
                }
            }
        }

        this.changedLiteralsInLastIteration = literalsWithSupportsToBeAdded;
        this.changedPredicatesInLastIteration = changedPredicates;
        this.change = incorporateNewSupports(literalsWithSupportsToBeAdded, supportsOfBrandNewHeads);
        this.brandNewHeads = headsNeverSeenBefore;


//        System.out.println(this.forbiddenBInvocatioins + "\t" + this.forbiddenBStops + "\t" + this.forbiddenBCallsWithin + "\t" + (this.forbiddenBStops / (1.0 * this.forbiddenBInvocatioins)));
//        System.out.println("asked\t" + askedFor + "\t" + asked.size() + "\t" + (asked.size() / (askedFor * 1.0)));
//        System.out.println("carte\t" + wantedFor + "\t" + wantedCartesians.size() + "\t" + (wantedCartesians.size() / (1.0 * wantedFor)));

//        if (!debugs.isEmpty()) {
//            debugs.stream().sorted().forEach(s -> System.out.println(s));
//        }

        //        debugIt2();
    }

    private Set<BSet> computeCartesian(long iteration, boolean isNewlyAddedRule, Map<Literal, Set<BSupport>> literalsWithSupportsToBeAdded, Triple<Clause, Literal, BSet> preparedRule, List<Literal> groundBody, boolean someBrandNew, boolean moreThanOneLiteralChanged, boolean debugThisIteration) {
        Set<BSet> rawSupports = Sugar.set();
        if (preparedRule.t.cardinality() > 0) {
            rawSupports.add(preparedRule.t);
        }

        for (Literal literal : groundBody) {
            /*if (debugThisIteration && "carries_out(Y, governmental_or_regulatory_activity)".equals(literal.toString())) {
                System.out.println("tady rucni debug");
            }*/
            Set<BSupport> literalsSupports = null;
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
            // zrychleni TODO tady brat do raw supports jenom zmeny z posledni iterace!!!! pujde to nejak? nebo nejak prepouzivat znalost z tech co uz vygenerovane jsou... nejak to zakomponovat
            if (rawSupports.isEmpty()) {
                break;
            }
            /*if (debugThisIteration) {
                System.out.println("after iteration\t" + literal);
                rawSupports.stream().map(x -> toCanon(x) + "\t" + x.stream().mapToObj(z -> "" + z).collect(Collectors.joining(", ")))
                        .forEach(x -> System.out.println("i " + x));
            }*/
        }
        return rawSupports;
    }

    private Set<BSet> computeCartesianMinSort(long iteration, Set<Clause> newlyAddedRules, Map<Literal, Set<BSupport>> literalsWithSupportsToBeAdded, Pair<Clause, Literal> preparedRule, List<Literal> groundBody, boolean someBrandNew, boolean moreThanOneLiteralChanged) {
        List<Set<BSupport>> supports = new ArrayList<>(groundBody.size());
        for (Literal literal : groundBody) {
            Set<BSupport> literalsSupports = null;
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
        Set<BSet> rawSupports = Sugar.set();
        for (Set<BSupport> support : supports) {
            rawSupports = cartesianOpt(support, rawSupports); // todo, toto zrychlit!
            if (rawSupports.isEmpty()) {
                break;
            }
        }
        return rawSupports;
    }

    private Set<BSet> computeCartesianCompelmentarySymmetry(long iteration, Set<Clause> newlyAddedRules, Map<Literal, Set<BSupport>> literalsWithSupportsToBeAdded, Pair<Clause, Literal> preparedRule, List<Literal> groundBody, boolean someBrandNew, boolean moreThanOneLiteralChanged) {
        // supports for the cartesian
        List<Set<BSupport>> supports = new ArrayList<>(groundBody.size());
        for (Literal literal : groundBody) {
            Set<BSupport> literalsSupports = null;
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
            return supports.get(0).stream().map(s -> s.getSet()).collect(Collectors.toSet());
        }

        // to unions
        List<BSet> unions = setsToUnions(supports);
        boolean[] available = new boolean[unions.size()];
        for (int idx = 0; idx < unions.size(); idx++) {
            available[idx] = true;
        }

        // select first two which should be cartesianed
        Triple<Integer, Integer, Integer> firstTwo = findMaximalCS(unions);
        available[firstTwo.r] = false;
        available[firstTwo.s] = false;
        Set<BSet> cartes = cart(supports.get(firstTwo.r).stream().map(s -> s.getSet()).collect(Collectors.toSet()), supports.get(firstTwo.s));


        if (cartes.isEmpty()) {
            return cartes;
        }
        // do the similar thing for the rest
        for (int iter = 0; iter < unions.size() - 2; iter++) {
            BSet currentUnion = toUnion(cartes);
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

    private Set<BSet> cart(Set<BSet> collect, Set<BSupport> supports) {
        Set<BSet> retVal = new LinkedHashSet<>(collect.size() * supports.size());
        for (BSet bitSet : collect) {
            for (BSupport support : supports) {
                BSet union = bitSet.union(support.getSet());
                if (union.cardinality() <= k) {
                    retVal.add(union);
                }
            }
        }
        return retVal;
    }

    // <first, second, distance between them>
    private Triple<Integer, Integer, Integer> findMaximalCS(List<BSet> unions) {
        Triple<Integer, Integer, Integer> retVal = new Triple(null, null, null);
        for (int outer = 0; outer < unions.size(); outer++) {
            BSet s1 = unions.get(outer);
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

    private int complementarySymmetry(BSet s1, BSet s2) {
        /*
        BitSet union = (BitSet) s1.clone();
        union.or(s2);
        BitSet intersection = (BitSet) s1.clone();
        intersection.and(s2);
        union.andNot(intersection);
        return union.cardinality();
        */
        throw new IllegalStateException();// NotImplementedException();
    }


    // proste projdi mapu a nahaz tam ty supporty tak aby byli minimalni :))
    private boolean incorporateNewSupports(Map<Literal, Set<BSupport>> literalsWithSupportsToBeAdded, Set<BSupport> suppportsOfBrandNewHeads) {
        // these supports are already minimal w.r.t. the ones in the this.map, but not within each other
        boolean madeChange = false;
        for (Map.Entry<Literal, Set<BSupport>> entry : literalsWithSupportsToBeAdded.entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalStateException();// safety check, should not occur
            }
            /*if (entry.getKey().toString().equals("result_of(embryonic_structure, acquired_abnormality)")) {
                System.out.println("here");
                entry.getValue().stream().map(this::toCanon).sorted().forEach(s -> System.out.println("\t" + s));
            }*/


            Literal head = entry.getKey();
            Pair<List<BSupport>, List<BSupport>> minimalSubsumed = minimal(Sugar.listFromCollections(entry.getValue()));
            if (!minimalSubsumed.s.isEmpty()) {
                entry.getValue().removeAll(minimalSubsumed.s);
            }

            /*if (entry.getKey().toString().equals("result_of(embryonic_structure, acquired_abnormality)")) {
                System.out.println("minimal");
                minimalSubsumed.r.stream().map(this::toCanon).sorted().forEach(s -> System.out.println("\t" + s));
                System.out.println("throw away");
                minimalSubsumed.s.stream().map(this::toCanon).sorted().forEach(s -> System.out.println("\t" + s));
            }*/

            // todo kdyz se tohle pocita po dvojicich, nemohou se nektere vypocty opakovat????


            Set<BSupport> placeToAdd = this.map.get(head);
            boolean isBrandNew = null == placeToAdd;
            if (isBrandNew) {
                placeToAdd = new LinkedHashSet<>(minimalSubsumed.r.size());
                this.map.put(head, placeToAdd);
            }

            for (BSupport support : minimalSubsumed.r) {
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

//    {
//        System.out.println("!!!! TODO kouknout na toMinimalForbiddenFreeSupports jestli je správně, jestli náhodou jí neprojdou i neminimální");
//    }


    // this is really not nice because two filters (forbidden and minimal) and one factory-like operation are done within one method
    private Set<BSupport> toMinimalForbiddenFreeSupports(Set<BSet> rawSupports, Map<BSet, Boolean> isNowForbidden) {
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
        List<BSet> nonForbidden = rawSupports.stream()
                .filter(bset -> !this.isForbiddenB(bset))
                /*.filter(bset -> {
                    Boolean result = isNowForbidden.get(bset);
                    if (null == result) {
                        result = this.isForbiddenB(bset);
                        isNowForbidden.put(bset, this.isForbiddenB(bset));
                    }
                    return !result;
                })*/
                .sorted(Comparator.comparing(BSet::cardinality))
                .collect(Collectors.toList());

        Set<BSupport> retVal = new LinkedHashSet<>(rawSupports.size());
        // throwing out non-minimal ones
        for (int outer = nonForbidden.size() - 1; outer >= 0; outer--) {
            boolean someoneSubsumesMe = false;
            BSet me = nonForbidden.get(outer);
            for (int inner = 0; inner < outer; inner++) {
//                BSet intersection = (BitSet) nonForbidden.get(inner).clone();
//                intersection.and(me);
//                if (intersection.cardinality() == nonForbidden.get(inner).cardinality()) {
                // TODO je tohle dobre co se tyce minimality?????
                if (nonForbidden.get(inner).isSubsetOf(me)) {
                    someoneSubsumesMe = true;
                    break;
                }
            }
            // adding to factory
            if (!someoneSubsumesMe) {
                BSet set = FastSubsetFactory.getInstance().get(me);
                BSupport support = supportCache.get(set);
                if (null == support) {
                    support = BSupport.create(set);
                    supportCache.put(set, support);
                }
                retVal.add(support);
            }
        }
        return retVal;
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

    private boolean isForbiddenT(Term[] terms) {
        Set<Constant> constants = Sugar.set();
        for (Term term : terms) { // we know, everything is grounded
            constants.add((Constant) term);
        }
        return isForbiddenS(constantsToSubset(constants));
    }

    private boolean isForbiddenS(BSet subset) {
        //return Sugar.parallelStream(forbidden, parallel).anyMatch(set -> subset.isSubsetOf(set.getSet()));
        //return Sugar.parallelStream(forbidden, parallel).anyMatch(set -> set.getSet().isSubsetOf(subset));  //todo tohle napsat rychleji
        //return forbidden.stream().anyMatch(set -> itf2(set, subset));  //todo tohle napsat rychleji


        /**/
        if (this.oldVersion) {// old version
            for (BSupport support : forbidden) {

                if (support.getSet().isSubsetOf(subset)) {
                    return true;
                }
            }
            return false;
        } else {
//            int atMostSize = subset.cardinality();
            int next = subset.nextSetBit(0);
            while (next > -1) {
                Set<BSupport> possibleSubsets = this.forbiddens.get(next);
                if (null != possibleSubsets) {
                    for (BSupport support : possibleSubsets) {
//                        if (support.size() <= atMostSize && support.getSet().isSubsetOf(subset)) {
                        if (support.getSet().isSubsetOf(subset)) {
                            return true;
                        }
                    }
//                    atMostSize--;
                }
                next = subset.nextSetBit(next + 1);
            }

            return false;
        }
        /**/
    }

    private boolean itf2(Support set, Subset subset) {
        return set.getSubset().isSubsetOf(subset);
    }

    private boolean isForbiddenB(BSet set) {
        //return Sugar.parallelStream(forbidden, parallel).anyMatch(set -> set.getSet().isSubsetOf(bitset));
        //return forbidden.stream().anyMatch(set -> ift1(set, bitset));
        this.forbiddenBInvocatioins++;

        if (this.oldVersion) {// old version
        /**/
            for (BSupport support : forbidden) {
                this.forbiddenBCallsWithin++;
                if (support.isSubsetOf(set)) {
                    this.forbiddenBStops++;
                    return true;
                }
            }
            return false;
         /**/
        } else { // speedup version ?
        /**/
            int next = set.nextSetBit(0);
            while (next > -1) {
                Set<BSupport> possibleSubsets = this.forbiddens.get(next);
                if (null != possibleSubsets) {
                    for (BSupport support : possibleSubsets) {
//                        if (support.size() <= atMostSize && support.getSet().isSubsetOf(subset)) {
                        this.forbiddenBCallsWithin++;
                        if (support.getSet().isSubsetOf(set)) {
                            this.forbiddenBStops++;
                            return true;
                        }
                    }
//                    atMostSize--;
                }
                next = set.nextSetBit(next + 1);
            }
            return false;
        /**/
        }

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

    private Pair<List<BSupport>, List<BSupport>> minimal(List<BSupport> supports) {
        // return pair of minimals and the one removed from the initial list to get the minimal
        List<BSupport> minimal = new LinkedList<>();
        List<BSupport> subsumed = new LinkedList<>();
        Collections.sort(supports, Comparator.comparingInt(BSupport::size));
        for (int outer = supports.size() - 1; outer >= 0; outer--) {
            BSet outerSubset = supports.get(outer).getSet();
            boolean nothingSubsumesMe = true;
            for (int inner = 0; inner < outer && nothingSubsumesMe; inner++) {
                nothingSubsumesMe = nothingSubsumesMe && !supports.get(inner).getSet().isSubsetOf(outerSubset);
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
    private Set<BSupport> cartesian(Set<BSupport> supports, Set<BSupport> previous) {//}, long iteration) {
        // supports is either empty (null in the current world) or a set of supports
        // previous contains cartesian product of the previous literals
        Set<BSet> retVal = Sugar.set();
        if (null == previous || previous.isEmpty()) {
//            System.out.println("vstup 1\t" + supports.size() + "\t\t" + supports);
            for (BSupport support : supports) {
                if (support.size() <= k) {
                    //retVal.add(new Pair<>(support.getLiterals(), support.getSet().getBitset()));
                    retVal.add(support.getSet());
                }
            }
        } else {
//            System.out.println("vstup 2\t" + supports.size() + "\t\t" + supports + "\n\t\t" + previous.size() + "\t\t" + previous);
            for (BSupport support : supports) {
                for (BSupport old : previous) {
                    //Pair<Set<Literal>, BitSet> union = new Pair<>(Sugar.union(support.getLiterals(), old.getLiterals()), union(support.getSet().getBitset(), old.getSet().getBitset()));
                    BSet union = support.getSet().union(old.getSet());
                    //Support union = union(support, old);
                    if (union.cardinality() <= k) {
                        retVal.add(union);
                    }
                }
            }
        }

        return retVal.stream().map(bitset -> {
            BSet set = FastSubsetFactory.getInstance().get(bitset);
            BSupport support = supportCache.get(set);
            if (null == support) {
                support = BSupport.create(set);
                supportCache.put(set, support);
            }
            return support;
        }).collect(Collectors.toSet());
    }

    // ultra opt version
    private Set<BSet> cartesianOpt(Set<BSupport> supports, Set<BSet> previous) {//}, long iteration) {
        // supports is either empty (null in the current world) or a set of supports
        // previous contains cartesian product of the previous literals
        Set<BSet> retVal = Sugar.set();
        if (null == previous || previous.isEmpty()) {
            for (BSupport support : supports) {
                if (support.size() <= k) {
                    retVal.add(support.getSet());
                }
            }
        } else {
            for (BSupport support : supports) {
                BSet newOne = support.getSet();
                for (BSet old : previous) {
                    BSet union = newOne.union(old);
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

    public static SupportsHolderOpt6BSet create(Set<Literal> evidence, int k, Map<Pair<String, Integer>, Integer> predicatesToBitset, boolean useExtraPruning) {
        List<Constant> constants = LogicUtils.constants(evidence).stream().sorted(Comparator.comparing(Constant::toString)).collect(Collectors.toList());

        Map<Constant, BSet> constantToSubset = new HashMap<>();
        Map<Constant, Integer> constantToInteger = new HashMap<>();
        IntStream.range(0, constants.size()).forEach(idx -> {
            constantToSubset.put(constants.get(idx), FastSubsetFactory.getInstance().get(constants.size(), idx));
            constantToInteger.put(constants.get(idx), idx);
            //System.out.println(idx + "\t" + constants.get(idx));
        });


        Map<BSet, BSupport> supportCache = new ConcurrentHashMap<>();
        Map<Literal, Set<BSupport>> map = new ConcurrentHashMap<>();
        //Map<Literal, Head> heads = new ConcurrentHashMap<>();

        for (Literal literal : evidence) {
            BSet set = constantsToSubset(LogicUtils.constantsFromLiteral(literal), constantToInteger);
            BSupport support = supportCache.get(set);
            if (null == support) {
                support = BSupport.create(set);
                supportCache.put(set, support);
            }
            map.put(literal, Sugar.set(support));
            support.addLiteral(literal);
            //heads.put(literal, Head.get(literal, subset));
        }

        return new SupportsHolderOpt6BSet(k, evidence, constantToSubset, constants, map, //heads,
                supportCache, predicatesToBitset, useExtraPruning, constantToInteger);
    }

    public BSet constantsToSubset(Literal literal) {
        Set<Constant> constants = Sugar.set();
        for (Term term : literal.terms()) {
            if (term instanceof Constant) {
                constants.add((Constant) term);
            }
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

    public static BSet constantsToSubset(Set<Constant> constants, Map<Constant, Integer> constantToInteger) {
        List<Integer> constantsIndexes = constants.stream().map(constantToInteger::get).collect(Collectors.toList());
        return FastSubsetFactory.getInstance().get(constantToInteger.keySet().size(), constantsIndexes);
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

        /*if (debugNow) {
            this.map.keySet().stream().map(Literal::toString).sorted().forEach(System.out::println);
        }*/


        return !this.change // this.change is true if some change occurred in the forward phase
                && map.keySet().size() == this.sizeOfWorld
                && supportsInWorld() == this.sizeOfSupports;
    }


    private void checkAdding(List<Clause> negatedConstraints, Set<BSupport> testingBecauseNewHeads, boolean checkAllSupports) {
        // I have to go over all supports and for supersets of the ones that induce the brand new literals (i.e. non-entailed in the previous run),
        // ok, here I mostly go through the newly added supports only.... and then, if some one introduces inconsistency, then remove it and all its supersets (having find them first)
        /*Set<Support> selectedSupports = (testingBecauseNewHeads.isEmpty()) ? this.supportsOfBrandNewHeads : Sugar.union(this.supportsOfBrandNewHeads, testingBecauseNewHeads);
        if (checkAllSupports) {
            selectedSupports = this.map.values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new));
        }
        // well, it is the above really sound
//        Set<Support> supports = this.map.values().stream().flatMap(Collection::stream).collect(Collectors.toCollection(LinkedHashSet::new)); // tak timhle to ocividne neni
        */
        Set<BSupport> removeSupports = this.map.values().stream()
                .flatMap(Collection::stream).distinct()
                .filter(support -> {
                    if (this.supportsOfBrandNewHeads.parallelStream().noneMatch(newSupport -> newSupport.getSet().isSubsetOf(support.getSet()))) {
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
        //System.out.println("remove supports\t" + removeSupports.size() + "\t" + negatedConstraints.size());
        // removing following supports
        for (BSupport support : removeSupports) {
            if (this.oldVersion) {// old version
                this.forbidden.add(support);
                this.forbiddensSize++;
            } else {
                if (null == this.forbiddens.get(support.getSet().getMinimalValue())) {
                    this.forbiddens.put(support.getSet().getMinimalValue(), Sugar.set());
                }
                this.forbiddens.get(support.getSet().getMinimalValue()).add(support);
                this.forbiddensSize++;
            }

            Set<Literal> nonRemoved = ConcurrentHashMap.newKeySet();
            for (Literal literal : support.getLiterals()) {
                if (evidence.contains(literal)) {
                    // this can be done more efficiently
                    if (support.getSet().isSubsetOf(constantsToSubset(literal))) { // in this case, constants(literal) is equal to support
                        nonRemoved.add(literal);
                        continue;
                    }
                }
                Set<BSupport> sups = map.get(literal);
                sups.remove(support);
                if (sups.isEmpty()) {
                    map.remove(literal);
                }
                // here, we have to take care of the heads and their supports in this.changedLiteralsInLastIteration
                Set<BSupport> lastIteration = this.changedLiteralsInLastIteration.get(literal);
                if (null != lastIteration) {
                    lastIteration.remove(support);
                    if (lastIteration.isEmpty()) {
                        this.changedLiteralsInLastIteration.remove(literal);
                    }
                    this.brandNewHeads.remove(literal);
                }
            }
            if (support.getLiterals().isEmpty()) {
                supportCache.remove(support.getSet());
            } else {
                support.setLiterals(nonRemoved);
            }
        }

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

