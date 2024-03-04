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
public class SupportsHolderOpt {


    private final Map<Constant, Subset> constantToSubset;
    private final List<Constant> evidenceConstants;
    private final Map<Set<Constant>, Subset> constantsToSubset;
    private final Set<Literal> evidence;
    public final Map<Literal, Set<Support>> map; // public just because of debug
    private final int k;
    private final Map<Subset, Support> supportCache;
    private final Set<Support> forbidden;
    private Set<Support> currentSupport;
    private final Map<Literal, Subset> literalsCache;
    private long sizeOfSupports = 0;
    private int sizeOfWorld;
    private final boolean parallel = true;
    private boolean change;
    private Set<Literal> changedInLastIteration;

    {
        // TODO
//        System.out.println("SupportsHolder -- todo: namisto metody mask pouzit nejakou jinou datovou strukturu aby i support mel seznam literalu ktere uvozuje");
        //TODO nekde je tu bug, kdyz neni zapnuta cache na supporty tak to obcas se asi muze stat ze jsou dva supporty rozdilne objekty a potom to pada protoze se nekde operuje s nullem... coz je zlvastni
    }

    public SupportsHolderOpt(int k, Set<Literal> evidence, Map<Constant, Subset> constantToSubset, List<Constant> evidenceConstants, Map<Literal, Set<Support>> map, Map<Subset, Support> supportCache) {
        System.out.println("sho");
        this.k = k;
        this.evidence = evidence;
        this.constantToSubset = constantToSubset;
        this.evidenceConstants = evidenceConstants;
        this.constantsToSubset = new HashMap<>();
        this.map = map;
        this.supportCache = supportCache;
        this.literalsCache = new ConcurrentHashMap<>();
        this.forbidden = ConcurrentHashMap.newKeySet();
        this.changedInLastIteration = ConcurrentHashMap.newKeySet();
    }

//    Set<String> debug = Sugar.set("Post_Quals(1:person217)", "Post_Quals(1:person155)", "Faculty(1:person101)", "Post_Quals(1:person391)");


    // the main point is to fill in currentSupport and currentHeads as these two are then used for constraints part; the rest fills in data into map, etc.
    // also to fill the newly computed into map
    public void forwardRules(List<Pair<Clause, Literal>> rulesWithNegated, long iteration, Set<Clause> newlyAddedRules) {
        boolean speak = -1 == iteration;

        //

        this.sizeOfSupports = supportsInWorld();
        this.sizeOfWorld = map.keySet().size();
        this.currentSupport = Sugar.set();
        Set<Literal> literalsWithChangedSupports = new HashSet<>();
        Matching world = Matching.create(new Clause(map.keySet()), Matching.THETA_SUBSUMPTION);

        Set<String> usedRules = Sugar.set();
        int hcd = -1;
//        int hcd = map.keySet().hashCode();

        if (SupportEntailmentInference.quickDebug) {
            hcd = map.keySet().hashCode();
            System.out.println("\nhcd\t" + hcd + "\t" + debugPrint().hashCode());
        }

        /*if((iteration == 2 || iteration == 3) && !SupportEntailmentInference.wantedHeads.isEmpty()){
            System.out.println(map.keySet().stream().map(Object::toString).sorted().collect(Collectors.joining(", ")));
            System.out.println(debugPrint());
        }*/
        //String wantedHead = "affects(1:physiologic_Function, 1:natural_Phenomenon_or_Process)";
//        String wantedHead = "affects(1:physiologic_Function, 1:disease_or_Syndrome)";
        String wantedHead = "property_of(1:laboratory_or_Test_Result, 1:disease_or_Syndrome)";


        if (speak) {
            System.out.println(new Clause(map.keySet()));
        }
        // orthogonally to this approach, we could collect violated rules (to Map<head,Literal<groundBodies>>) and to do all the minimal operation of one head in one place;
        // that would, however, need more and more memory
        this.change = false;
        boolean debug = false;
        for (Pair<Clause, Literal> preparedRule : rulesWithNegated) {
  /*          if(!SupportEntailmentInference.wantedHeads.isEmpty() && (preparedRule.s + " <- " + preparedRule.r).equals("result_of(1:V0, 1:V1) <- diagnoses(1:V3, 1:V0), result_of(1:V2, 1:V1), @alldiff(1:V3, 1:V2, 1:V1, 1:V0), @alldiff(1:V3, 1:V0, 1:V2, 1:V1)")){
                System.out.println("debug odsud");
                debug = true;
            }else{debug = false;}
*/

            if (0 == iteration && !newlyAddedRules.contains(preparedRule.r)) {
                // in this case the rule cannot add anything, thus we can skip computation of groundings of this rule
                continue;
            }

            boolean canUsePruning = 0 != iteration || !newlyAddedRules.contains(preparedRule.r);
            Pair<Term[], List<Term[]>> subts = world.allSubstitutions(preparedRule.r, 0, Integer.MAX_VALUE);
//            System.out.println(preparedRule.s + " <- " + preparedRule.r);
            for (Term[] terms : subts.s) {
                Literal head = LogicUtils.substitute(preparedRule.s, subts.r, terms);
                Clause groundBody = LogicUtils.substitute(preparedRule.r, subts.r, terms); // we work with range restricted rules only; otherwise we would need to generate the support by adding constants from the head, wouldn't we?

//                boolean beVerbouse = head.toString().equals(wantedHead);
//                boolean beVerbouse = SupportEntailmentInference.wantedHeads.contains(head.toString());
//                boolean beVerbouse = !SupportEntailmentInference.wantedHeads.isEmpty();
                boolean beVerbouse = false;
                if (beVerbouse) {
/*                    if(debug){
                        System.out.println("am here");
                    }
*/
                    System.out.println((head + " <- " + groundBody).hashCode() + "\t" + head + " <- " + groundBody + "\t\t" + map.keySet().hashCode());
                }

                /*
                if ((head + " <- " + groundBody).equals("co-occurs_with(1:pathologic_Function, 1:mental_or_Behavioral_Dysfunction) <- @alldiff(1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function), @alldiff(1:pathologic_Function, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction), manifestation_of(1:pathologic_Function, 1:disease_or_Syndrome), complicates(1:pathologic_Function, 1:mental_or_Behavioral_Dysfunction)")) {
                    beVerbouse = true;
                    System.out.println("tady tady tady \titeration\t" + iteration);
                    System.out.println((head + " <- " + groundBody).hashCode() + "\t" + (head + " <- " + groundBody));
                }
*/

//                System.out.println(groundBody);

                // mozna je tady chyba v tom ze to neuvazuje nove hlavy ktere nejsou vubec jeste v mape :))

//                if (speak && head.toString().equals("result_of(1:natural_Phenomenon_or_Process, 1:clinical_Attribute)")) {
//                    System.out.println("i am here\t" + head);
//                }

                // in place due to speed; this should do the work
                boolean allUntouched = true;
                boolean atLeastOneNonEvidence = false;
                if (canUsePruning) {
                    for (Literal literal : groundBody.literals()) {
                        boolean isSpecialPredicate = SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate());
                        atLeastOneNonEvidence = atLeastOneNonEvidence | (!this.evidence.contains(literal) && !isSpecialPredicate);
                        if (this.changedInLastIteration.contains(literal)) {// tady urcite alldiff pridany nebude, takze je to v poradku ;)
                            allUntouched = false;
                            //break;
                        }
                    }
                } else {
                    allUntouched = false; // we set this variable to false to say that we cannot use the pruning here (it would not be consistent)
                    // the reason for this is that this rule is newly added, therefore
                }

                // (allUntouched && atLeastOneNonEvidence && map.containsKey(head)) this is a condition, which when true, says that this particular support is already in map.get(head) and thus can be skipped
                if ((allUntouched && atLeastOneNonEvidence && map.containsKey(head)) || isForbidden(terms)) {
                    // daly by se forbidden vnutit pri hledani ground substituci???? a slo by to vubec?
//                    if (speak) {
//                        System.out.println("breaking\t" + head + "\t" + allUntouched + "\t" + atLeastOneNonEvidence);
//                    }
                    if (beVerbouse) {
                        System.out.println("skipping\t" + allUntouched + "\t" + atLeastOneNonEvidence + "\t" + map.containsKey(head) + "\t" + isForbidden(terms));
                        // debug pro 140.db
                        System.out.println("\tjust to review it it\t" + head + " <- " + groundBody + "\n\t\t" + this.map.containsKey(head) + "\t" + this.changedInLastIteration.contains(head));
                        groundBody.literals().stream().sorted(Comparator.comparing(Object::toString)).forEach(l -> {
                            if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(l.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(l.predicate())) {
//                                System.out.println(l + "\t" + this.changedInLastIteration.contains(l));
//                                System.out.println("\tis special, skip him");
                            } else {
                                System.out.println("\t" + l + "\t" + this.changedInLastIteration.contains(l));
                                /*
                                String beforeThat = null;
                                if (this.map.containsKey(l)) {
                                    beforeThat = "\n\t" + this.map.get(l).stream().map(this::toCanon).sorted().collect(Collectors.joining("\n\t"));
                                }
                                if (null != beforeThat) {
                                    System.out.println("this and previous rounds" + beforeThat);
                                }*/
                            }
                        });
                    }

                    if (beVerbouse && false) {
                        System.out.println("\tforbidden\t" + isForbidden(terms) + "\t|\t" + allUntouched + " & " + atLeastOneNonEvidence + "\t\t" + map.containsKey(head));
                        for (Literal literal : groundBody.literals()) {
                            if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                                continue;
                            }
                            System.out.println("\t" + literal + "\t" + this.evidence.contains(literal) + "\n\t" + this.changedInLastIteration.contains(literal));
                            this.map.get(literal).stream().map(this::toCanon).sorted().forEach(s -> System.out.println("\t\t" + s));
                        }


                        System.out.println("\t" + (null == map.get(head) ? 0 : map.get(head).size()) + "\t" + (null == map.get(head) ? 0 : map.get(head).size()));
                        Set<Support> debugMins = map.get(head);
                        debugMins = null == debugMins ? Sugar.set() : debugMins;
                        System.out.println("\n\t" + debugMins.stream().map(this::toCanon).sorted().collect(Collectors.joining("\n\t")));

                        System.out.println("trying to generate new support from these by cartesian\t");
                        Set<Support> debugSup = Sugar.set();
                        for (Literal literal : groundBody.literals()) {
                            if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                                continue;
                            }
                            debugSup = cartesian(supports(literal), debugSup);
                            if (debugSup.isEmpty()) {
                                if (beVerbouse) {
                                    System.out.println("end because cart is empty");
                                }
                                break;
                            }
                        }
                        System.out.println("the results are");
                        for (Support support : debugSup) {
                            System.out.println("\t" + toCanon(support));
                        }
                        System.out.println("end of results");
                    }

                    continue;
                }

                Set<Support> sup = Sugar.set();
                for (Literal literal : groundBody.literals()) {
                    if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                        continue;
                    }

                    if (beVerbouse) {
//                        Set<Support> supports = supports(literal);
//                        String spts = null == supports ? "{}" : supports.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "));
//                        System.out.println("\t" + literal + "\t" + spts);
                    }
                    sup = cartesian(supports(literal), sup);
                    if (sup.isEmpty()) {
                        if (beVerbouse) {
                            System.out.println("end because cart is empty");
                        }
                        break;
                    }
                }
                // takovahle implementace byla puvodne, chtelo se tm optimalizovat vypocty
//                Collection<Support> sups = forbiddenFree(sup);
//                Set<Support> minimals = Sugar.setFromCollections(minimal(sups, map.get(head)));

                if (beVerbouse && false) {
                    for (Literal literal : groundBody.literals()) {
                        if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                            continue;
                        }
                        System.out.println("\t" + literal + "\t" + this.evidence.contains(literal));
                        this.map.get(literal).stream().map(this::toCanon).sorted().forEach(s -> System.out.println("\t\t" + s));
                    }

                    for (Support support : sup) {
                        System.out.println("\t" + toCanon(support));
                    }
                }
                /*if (beVerbouse) {
                    for (Support support : sup) {
                        if (toCanon(support).equals("{1:disease_or_Syndrome, 1:natural_Phenomenon_or_Process, 1:organ_or_Tissue_Function, 1:phenomenon_or_Process, 1:physiologic_Function}")) {
                            System.out.println("I'm here, a wrongly implemented minimal is here");
                        }
                    }
                }*/

                //spocitat nejdrive minimalni ze sups
                List<Support> newSupports = minimal(forbiddenFree(sup)); // aka the original ones

                if (beVerbouse) {
                    System.out.println("\tnew minimal forbidden free supports\t" + newSupports.size());
                }

                /*if (!SupportEntailmentInference.wantedHeads.isEmpty()) {
                    System.out.println((preparedRule.s + " <- " + preparedRule.r).hashCode() + "\t" + preparedRule.s + " <- " + preparedRule.r + "\t\t" + newSupports.size());
                }*/

                //Set<Support> minimals = Sugar.setFromCollections(minimal(sups, map.get(head))); // tohle usporadani forbiddenFree a minimals bude fungovat pouze pokud v map.get(head) se budou aktualizovat a vyhazovat nadmnoziny zakazanych
                Set<Support> minimals = null;
                Set<Support> previousSupports = map.get(head);
                Set<Support> toRemove = new HashSet<>(null == previousSupports ? 0 : previousSupports.size());
                List<Support> toAdd = Sugar.list();

                /*for (Support minimal : newSupports) {
                    if (toCanon(minimal).equals("{1:disease_or_Syndrome, 1:embryonic_Structure, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}")) {
                        System.out.println("this grouding gave rise of the wanted support");
                        System.out.println(head + " <- " + groundBody);
                    }
                }*/


                //potom zkontrolovat jejich minimalitu vuci previousSupports
                if (null == previousSupports) {
                    minimals = Sugar.setFromCollections(newSupports);
                    literalsWithChangedSupports.add(head);
/*
                    if (beVerbouse) {
                        System.out.println("adding new supports\t" + minimals.size());
                        minimals.stream().map(this::toCanon).sorted().forEach(r -> System.out.println("\t" + r));
                    }

                    if (!SupportEntailmentInference.wantedHeads.isEmpty()) {
                        System.out.println(head + "\t" + minimals.size() + "\twhole new");
                    }
                    usedRules.add(preparedRule.s + " <- " + preparedRule.r);
*/
                    for (Support support : toAdd) {
                        String c = toCanon(support);
                        if (c.equals("{1:disease_or_Syndrome, 1:embryonic_Structure, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}") && head.toString().equals("co-occurs_with(1:pathologic_Function, 1:mental_or_Behavioral_Dysfunction)")) {
                            System.out.println("adding it brand new");
                        }
                    }


                } else {
                    for (Support newOne : newSupports) {
                        boolean anyoneSubsumesMe = false;

                        for (Support oldOne : previousSupports) {
                            if (oldOne.getSubset().isSubsetOf(newOne.getSubset())) {
                                anyoneSubsumesMe = true;
                                break;
                            } else if (newOne.getSubset().isSubsetOf(oldOne.getSubset())) {
                                toRemove.add(oldOne);
                            }
                        }

                        if (!anyoneSubsumesMe) {
                            toAdd.add(newOne);
                        }
                    }

                    if (!toRemove.isEmpty() || !toAdd.isEmpty()) {
                        literalsWithChangedSupports.add(head);
                        usedRules.add(preparedRule.s + " <- " + preparedRule.r);
                    }
                    minimals = previousSupports;
                    minimals.removeAll(toRemove);
                    minimals.addAll(toAdd);

                    if (beVerbouse) {
                        if (!toRemove.isEmpty()) {
                            System.out.println("removing old ones\t" + toRemove.size());
                            toRemove.stream().map(this::toCanon).sorted().forEach(r -> System.out.println("\t" + r));
                        }
                        System.out.println("adding new supports\t" + toAdd.size());
                        toAdd.stream().map(this::toCanon).sorted().forEach(r -> System.out.println("\t" + r));
                    }

                    for (Support support : toRemove) {
                        String c = toCanon(support);
                        if (c.equals("{1:disease_or_Syndrome, 1:embryonic_Structure, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}") && head.toString().equals("co-occurs_with(1:pathologic_Function, 1:mental_or_Behavioral_Dysfunction)")) {
                            System.out.println("removing it");
                        }
                    }

                    for (Support support : toAdd) {
                        String c = toCanon(support);
                        if (c.equals("{1:disease_or_Syndrome, 1:embryonic_Structure, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}") && head.toString().equals("co-occurs_with(1:pathologic_Function, 1:mental_or_Behavioral_Dysfunction)")) {
                            System.out.println("adding it");
                        }
                    }
                    /*if (!SupportEntailmentInference.wantedHeads.isEmpty()) {
                        System.out.println(head + "\t" + minimals.size() + "\t" + toAdd.size() + "\t\t" + toRemove.size());
                    }*/

                }

                // radek 47 toho difu
/*                if (beVerbouse && (preparedRule.s + " <- " + preparedRule.r).equals("result_of(1:V0, 1:V1) <- diagnoses(1:V3, 1:V0), result_of(1:V2, 1:V1), @alldiff(1:V3, 1:V2, 1:V1, 1:V0), @alldiff(1:V3, 1:V0, 1:V2, 1:V1)")) {
                    System.out.println(allUntouched + "\t" + atLeastOneNonEvidence + "\t" + this.map.containsKey(head) + "\t");
                }
*/
                if (beVerbouse && false) {
                    System.out.println("\t" + (null == map.get(head) ? 0 : map.get(head).size()) + "\t" + minimals.size());
                    System.out.println("\n\t" + minimals.stream().map(this::toCanon).sorted().collect(Collectors.joining("\n\t")));
                    if (minimals.isEmpty()) {
                        System.out.println("\t" + (null == previousSupports ? 0 : previousSupports.size()) + "\t" + newSupports.size() + "\t" + sup.size());
                    }
                }
//                if (speak) {
//                    System.out.println("bef aft\t\t" + (null == previousSupports ? 0 : previousSupports.size()) + "\t" + minimals.size() + "\t" + head);
//                }
                if (minimals.size() > 0) {
                    this.change = store(head, minimals, currentSupport) | change;
                }
            }
        }

//        System.out.println("world size after rule forwarding\t" + map.keySet().size());

//        System.out.println("in");
//        map.keySet().stream().filter(l -> !this.evidence.contains(l)).forEach(k -> System.out.println("\t" + k));
//        System.out.println("\n");

        this.changedInLastIteration = literalsWithChangedSupports;

        // debug to cele co pouziva usedRules
        if (!SupportEntailmentInference.wantedHeads.isEmpty()) {
            System.out.println("\tused rules");
            usedRules.stream().sorted().forEach(r -> System.out.println("\t\t" + r));
        }
    }

    public String toCanon(Support s) {// public just because of debug
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

    private boolean store(Literal head, Set<Support> supports, Set<Support> currentSupport) {
        if (supports.isEmpty()) {
            throw new IllegalStateException();
        }

        boolean change = !supports.equals(this.map.get(head));
        if (change && this.map.containsKey(head)) { // here, we take old supports (there are new ones to be added) and for each of them we remove support for this head
            this.map.get(head).stream().filter(support -> !supports.contains(support)).forEach(support -> support.removeHead(head));
        }
        this.map.put(head, supports);
        //if (null == mapSupport) {
        //    mapSupport = Sugar.set();
        //    this.map.put(head, mapSupport);
        //}
        //mapSupport.addAll(supports); this is for the incrementing version with heads violation

        for (Support support : supports) {
            support.addLiteral(head);
        }
        //System.out.println("adding to\t" + head + "\t" + sup);
        //List<Support> newOnesOnly = sup.stream().filter(s -> s.getIteration() == iteration).collect(Collectors.toList());
        //currentSupport.addAll(newOnesOnly);
        currentSupport.addAll(supports);
        /*if (!newOnesOnly.isEmpty()) {
            currentHeads.add(Head.get(head, literalToSubset(head)));
        }*/
        return change;
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
            for (Support support : supports) {
                if (support.size() <= k) {
                    //retVal.add(new Pair<>(support.getLiterals(), support.getSet().getBitset()));
                    retVal.add(support.getSubset().getBitset());
                }
            }
        } else {
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
            //cacheSupport(new Support(pair.r, SubsetFactory.getInstance().get(pair.s)))
            /*Subset subset = SubsetFactory.getInstance().get(pair.s);
            Support support = supportCache.get(subset);
            if(null == support){
                support = Support.get(subset);
            }
            support.addLiteral(todo tady);*/
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

    public static SupportsHolderOpt create(Set<Literal> evidence, int k) {
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

        return new SupportsHolderOpt(k, evidence, constantToSubset, constants, map, //heads,
                supportCache);
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

        if (!negatedConstraints.isEmpty()) {
            // 0 == iteration is here because of possible calls from kPL entailment with reusages of the holder
            checkAdding(negatedConstraints, Sugar.set(), 0 == iteration);
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
                this.changedInLastIteration.add(literal);
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

