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
public class SupportsHolderBruteForce {


    private final Map<Constant, Subset> constantToSubset;
    private final List<Constant> evidenceConstants;
    private final Map<Set<Constant>, Subset> constantsToSubset;
    private final Set<Literal> evidence;
    public final Map<Literal, Set<Support>> map; // public just for debug
    private final int k;
    private final Map<Subset, Support> supportCache;
    private Set<Support> currentSupport;
    private long sizeOfSupports = 0;
    private int sizeOfWorld;
    private final boolean parallel = true;
    private boolean change;

    {
        // TODO
//        System.out.println("SupportsHolder -- todo: namisto metody mask pouzit nejakou jinou datovou strukturu aby i support mel seznam literalu ktere uvozuje");
        //TODO nekde je tu bug, kdyz neni zapnuta cache na supporty tak to obcas se asi muze stat ze jsou dva supporty rozdilne objekty a potom to pada protoze se nekde operuje s nullem... coz je zlvastni
    }

    public SupportsHolderBruteForce(int k, Set<Literal> evidence, Map<Constant, Subset> constantToSubset, List<Constant> evidenceConstants, Map<Literal, Set<Support>> map, Map<Subset, Support> supportCache) {
        this.k = k;
        this.evidence = evidence;
        this.constantToSubset = constantToSubset;
        this.evidenceConstants = evidenceConstants;
        this.constantsToSubset = new HashMap<>();
        this.map = map;
        this.supportCache = supportCache;
    }

    // pri forward pruchodu se berou do karteskeho soucinu uz supporty ktere jsou (tzn. i nejaky ze soucasne iterace tam muze zpropagovat se), ale constraints (forbidden) si s tim dystak poradi
    // the main point is to fill in currentSupport and currentHeads as these two are then used for constraints part; the rest fills in data into map, etc.
    // also to fill the newly computed into map
    public void forwardRules(List<Pair<Clause, Literal>> rulesWithNegated) {
        System.out.println("shbf");
        this.sizeOfWorld = map.keySet().size();
        this.currentSupport = Sugar.set();
        this.sizeOfSupports = supportsInWorld();
        this.change = false;
        Map<Literal,Set<Support>>literalsWithSupportsToBeAdded = new HashMap<>();
        Matching world = Matching.create(new Clause(map.keySet()), Matching.THETA_SUBSUMPTION);

        for (Pair<Clause, Literal> preparedRule : rulesWithNegated) {

            Pair<Term[], List<Term[]>> subts = world.allSubstitutions(preparedRule.r, 0, Integer.MAX_VALUE);
            for (Term[] terms : subts.s) {
                Literal head = LogicUtils.substitute(preparedRule.s, subts.r, terms);
                Clause groundBody = LogicUtils.substitute(preparedRule.r, subts.r, terms); // we work with range restricted rules only; otherwise we would need to generate the support by adding constants from the head, wouldn't we?

                Set<BitSet> rawSupports = Sugar.set();
                for (Literal literal : groundBody.literals()) {
                    if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                        continue;
                    }
                    rawSupports = cartesianOpt(supports(literal), rawSupports); // todo, toto zrychlit! nebo zmensit pocet volani tohoto
                    if (rawSupports.isEmpty()) {
                        break;
                    }
                }

                Set<Support> newSupports = toMinimalSupports(rawSupports);
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
                    if(toRemove.size() > 0 && toAdd.size() < 1){
                        throw new IllegalStateException(); // better be safe than sorry
                    }

                    if (!toAdd.isEmpty()) {
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
                        }
                    }
                }
            }
        }
        this.change = incorporateNewSupports(literalsWithSupportsToBeAdded, currentSupport);
    }

    // proste projdi mapu a nahaz tam ty supporty tak aby byli minimalni :))
    private boolean incorporateNewSupports(Map<Literal, Set<Support>> literalsWithSupportsToBeAdded, Set<Support> currentSupport) {
        // these supports are already minimal w.r.t. the ones in the this.map, but not within each other
        boolean madeChange = false;
        for (Map.Entry<Literal, Set<Support>> entry : literalsWithSupportsToBeAdded.entrySet()) {
            if (entry.getValue().isEmpty()) {
                throw new IllegalStateException();// safety check, should not occur
            }
            Literal head = entry.getKey();
            Pair<List<Support>, List<Support>> minimalSubsumed = minimal(Sugar.listFromCollections(entry.getValue()));
            /*if (!minimalSubsumed.s.isEmpty()) { tohle je zbytecne pro BF verzi protoze ta to nepouzije
                entry.getValue().removeAll(minimalSubsumed.s);
            }*/

            Set<Support> placeToAdd = this.map.get(head);
            if (null == placeToAdd) {
                placeToAdd = new LinkedHashSet<>(minimalSubsumed.r.size());
                this.map.put(head, placeToAdd);
            }

            for (Support support : minimalSubsumed.r) {
                placeToAdd.add(support);
                support.addLiteral(head);
                currentSupport.add(support);
            }
            madeChange |= true;
        }
        return madeChange;
    }

    // this is really not nice because two filters (forbidden and minimal) and one factory-like operation are done within one method
    private Set<Support> toMinimalSupports(Set<BitSet> rawSupports) {
        List<BitSet> nonForbidden = Sugar.listFromCollections(rawSupports);
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

    public static SupportsHolderBruteForce create(Set<Literal> evidence, int k) {
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

        return new SupportsHolderBruteForce(k, evidence, constantToSubset, constants, map, //heads,
                supportCache);
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
        if (!negatedConstraints.isEmpty()) {
            throw new IllegalStateException();// NotImplementedException();
        }

        return !this.change;
        /*return !this.change // this.change is true if some change occurred in the forward phase
                && map.keySet().size() == this.sizeOfWorld
                && supportsInWorld() == this.sizeOfSupports;
                */
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
        return this.map.keySet().stream()
                .sorted(Comparator.comparing(Objects::toString)).map(l -> {
            return l + "\n\t" + map.get(l).stream().map(this::toCanon).sorted().collect(Collectors.joining(", "));
        }).collect(Collectors.joining("\n"));
    }
}

