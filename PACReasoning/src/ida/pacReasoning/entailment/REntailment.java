package ida.pacReasoning.entailment;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.SpecialBinaryPredicates;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.entailment.theories.Theory;
import ida.pacReasoning.supportEntailment.Support;
import ida.pacReasoning.supportEntailment.SupportsHolderOpt5BackupFactBased;
import ida.utils.Sugar;
import ida.utils.collections.Counters;
import ida.utils.collections.DoubleCounters;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 7. 2. 2019.
 */
public class REntailment {

    private final Set<Literal> evidence;
    private final int k;

    public REntailment(Set<Literal> evidence) {
        this.evidence = evidence;
        this.k = -1;
        // using k here?
    }

    public VECollector entails(Theory theory) {
//        System.out.println(evidence.size() + "\t" + evidence);
//        System.out.println(theory.getHardRules().size() + "\t" + ((FOL) theory).getImplications().size());

        // this is gonna be stateful (as the previous ones)
        Map<Literal, Set<Subset>> literalEntailedByProof = this.forwardChaining(theory);
        Map<Subset, Double> factWeight = this.assignWeight(literalEntailedByProof);
        Map<Literal, Double> entailedByVotes = this.collectVotes(literalEntailedByProof, factWeight);
        DoubleCounters<Literal> counter = new DoubleCounters<>();
        entailedByVotes.forEach((key, value) -> counter.add(key, value));
        this.evidence.forEach(literal -> counter.add(literal, Double.POSITIVE_INFINITY));
        return VECollector.create(this.evidence, counter, this.k, 0, Sugar.set(), 0);
    }

    private Map<Literal, Double> collectVotes(Map<Literal, Set<Subset>> literalEntailedByProof, Map<Subset, Double> factWeight) {
        Map<Literal, Double> retVal = new HashMap<>();
        literalEntailedByProof.forEach((key, value) -> {
            if (this.evidence.contains(key)) {
                retVal.put(key, Double.POSITIVE_INFINITY);
                System.out.println("this is weird, this should noc occur");
                assert false;
            } else {
                Double sumOfVotes = value.stream()
                        .mapToDouble(subset -> this.evidenceLiterals(subset)
                                .filter(factWeight::containsKey)
                                .mapToDouble(factWeight::get)
                                .min()
                                .orElseThrow(() -> new IllegalStateException())
                        )
                        .sum();
                retVal.put(key, sumOfVotes);
            }
        });
        return retVal;
    }

    private Map<Subset, Double> assignWeight(Map<Literal, Set<Subset>> literalEntailedByProof) {
        Counters<Subset> entailed = new Counters<>();
        literalEntailedByProof.values().forEach(values -> values.forEach(subset -> this.evidenceLiterals(subset).forEach(entailed::increment)));
        Map<Subset, Double> retVal = new HashMap<>();
        entailed.keySet().forEach(subset -> retVal.put(subset, 1.0 / entailed.get(subset)));
        return retVal;
    }

    private Stream<Subset> evidenceLiterals(Subset subset) {
        // TODO
        throw new IllegalStateException();

    }

    private Map<Pair<String, Integer>, Integer> createDictionary(Set<Literal> evidence, Set<Clause> clauses) {
        Set<Pair<String, Integer>> predicates = Sugar.set();
        for (Literal literal : evidence) {
            predicates.add(literal.getPredicate());
        }
        for (Clause clause : clauses) {
            for (Literal literal : clause.literals()) {
                if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(literal.predicate()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(literal.predicate())) {
                    continue;
                }
                predicates.add(literal.getPredicate());
            }
        }

        Map<Pair<String, Integer>, Integer> retVal = new HashMap<>();
        for (Pair<String, Integer> predicate : predicates) {
            if (!retVal.containsKey(predicate)) {
                retVal.put(predicate, retVal.size());
            }
        }
        return retVal;
    }

    private Map<Literal, Set<Subset>> forwardChaining(Theory theory) {
        //System.out.println("theory can be only FOL nothing else (for now)");

        Triple<Set<Clause>, Set<Clause>, Set<Clause>> rules = theory.getConstrainsHornOthers();
        if (null != rules.getT() && !rules.getT().isEmpty()) {
            throw new IllegalStateException("This implementation does not support clauses with more than one positive literal.");
        }
        List<Clause> negatedConstraints = rules.getR().stream().map(LogicUtils::flipSigns).collect(Collectors.toList());
        List<Pair<Clause, Literal>> negatedRules = theory.allRules().stream()
                .filter(rule -> !LogicUtils.positiveLiterals(rule).isEmpty())
                .map(clause -> {
                    if (!LogicUtils.isRangeRestricted(clause)) {
                        throw new IllegalStateException("clauses have to be range restricted!\n\t" + clause);
                    }
                    Literal positiveHead = Sugar.chooseOne(LogicUtils.positiveLiterals(clause));
                    Clause flippedBody = new Clause(LogicUtils.flipSigns(Sugar.setDifference(clause.literals(), Sugar.set(positiveHead))));
                    return new Pair<>(flippedBody, positiveHead);
                })
                .collect(Collectors.toList());

        Map<Pair<String, Integer>, Integer> predicatesToBitsets = createDictionary(evidence, theory.allRules());
        Set<Pair<Clause, Literal>> negatedRulesAll = Sugar.setFromCollections(negatedRules);
        // tady by mohl byt i jiny, klidne ne tolik optimalizovany holder, protoze nedelame stratifikovanou inferenci
        SupportsHolderOpt5BackupFactBased holder = SupportsHolderOpt5BackupFactBased.create(evidence, k, predicatesToBitsets, true);
        long iteration = 0;
        List<Clause> predictiveRules = theory.allRules().stream().filter(rule -> !LogicUtils.positiveLiterals(rule).isEmpty()).collect(Collectors.toList());
        while (true) {
            System.out.println("iteration\t" + iteration);
            holder.forwardRules(negatedRules, iteration, negatedRulesAll);
            boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration, predictiveRules);
            if (nextRoundNotNeeded) {
                break;
            }
            iteration++;
        }

        Map<Literal, Set<Subset>> retVal = new HashMap<>();
        holder.getSupports().forEach((literal, set) -> retVal.put(literal, set.stream().map(Support::getSubset).collect(Collectors.toSet())));
        SubsetFactory.getInstance().clear();
        return retVal;
    }


    public static REntailment create(Collection<Literal> evidence) {
        return new REntailment(evidence instanceof Set ? (Set) evidence : Sugar.setFromCollections(evidence));
    }


    public static void main(String[] args) {
//        t1();
        t2();
//        t3();
    }

    private static void t1() {
        Set<Clause> theory = Sugar.list("!sm(X), !fr(X,Y), sm(Y)").stream()
                .map(Clause::parse).collect(Collectors.toSet());

        REntailment entrailment = REntailment.create(Clause.parse("sm(a), fr(a,b), fr(b,e), fr(a,c), fr(a,d)").literals());

        Set<Clause> hardRules = Sugar.set();
        throw new IllegalStateException();
        /*
        DoubleCounters<Literal> counter = entrailment.compute(FOL.get(hardRules, theory));

        for (Literal literal : counter.keySet()) {
            System.out.println(literal + "\t" + counter.get(literal));
        }
        System.out.println("all the non-evidence literals should have the same weight as for each of the derivation sm(a) is used");
        */
    }

    private static void t2() {
        Set<Clause> theory = Sugar.list("!sm(X), !fr(X,Y), sm(Y)", "!q(X), !q(Y), !fr(X,Y), sm(Y)").stream()
                .map(Clause::parse).collect(Collectors.toSet());

        REntailment entrailment = REntailment.create(Clause.parse("sm(a), fr(a,b), fr(a,c), q(a), q(b)").literals());

        Set<Clause> hardRules = Sugar.set();
        throw new IllegalStateException();
        /*
        DoubleCounters<Literal> counter = entrailment.compute(FOL.get(hardRules, theory));

        for (Literal literal : counter.keySet()) {
            System.out.println(literal + "\t" + counter.get(literal));
        }
        System.out.println("there should be difference in sm(b), since it has bigger weight");
        */
    }


    private static void t3() {
        Set<Clause> theory = Sugar.list("!g(X), a(X)", "!fr(X,Y), fr(Y,X)", "!fr(X,Y),h(X)", "!h(X), !a(X)").stream()
                .map(Clause::parse).collect(Collectors.toSet());

        REntailment entrailment = REntailment.create(Clause.parse("g(l), fr(a,l)").literals());

        Set<Clause> hardRules = Sugar.set();
        throw new IllegalStateException();
        /*
        DoubleCounters<Literal> counter = entrailment.compute(FOL.get(hardRules, theory));

        for (Literal literal : counter.keySet()) {
            System.out.println(literal + "\t" + counter.get(literal));
        }
        System.out.println("should derive fr(l,a), h(l), h(a), a(l); there should be some penalty for h(l)");
        */
    }
}

/*
    TODO udelat
    si nejake
    mini testy
    */