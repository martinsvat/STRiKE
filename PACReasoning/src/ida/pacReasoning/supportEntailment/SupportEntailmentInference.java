package ida.pacReasoning.supportEntailment;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialBinaryPredicates;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.SubsetFactory;
import ida.pacReasoning.entailment.collectors.Entailed;
import ida.pacReasoning.entailment.collectors.StratifiedCollector;
import ida.pacReasoning.entailment.collectors.KECollector;
import ida.pacReasoning.entailment.theories.FOL;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.entailment.theories.Theory;
import ida.pacReasoning.supportEntailment.speedup.BSet;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 11. 10. 2018.
 */
public class SupportEntailmentInference {


    private final Set<Literal> evidence;

    public SupportEntailmentInference(Set<Literal> evidence) {
        this.evidence = evidence;
    }

    public Entailed entails(Possibilistic theory) {
        StratifiedCollector collector = new StratifiedCollector(this.evidence, 0);
        Set<Clause> constraints = theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).isEmpty()).collect(Collectors.toSet());
        Set<Clause> rules = theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toSet());

        KECollector partiallyEntailed = entails(FOL.create(constraints, rules), this.evidence);
        if (null == partiallyEntailed) {
            return collector;
        }
        partiallyEntailed.getEntailed().forEach(l -> collector.add(l, Double.MAX_VALUE));

        int iter = 0;
        for (Pair<Double, Clause> pair : theory.getSoftRules()) {
            System.out.println("# " + iter + "\t/\t" + theory.getSoftRules().size());
            iter++;
            Double weight = pair.r;
            Clause rule = pair.s;

            rules.add(rule);
            partiallyEntailed = entails(FOL.create(constraints, rules), Sugar.iterable(this.evidence, collector.getMemory().keySet()));
            if (null == partiallyEntailed) {
                return collector;
            }
            partiallyEntailed.getEntailed().forEach(l -> collector.add(l, weight));
        }
        return collector;
    }


    public Entailed entailsOldApproachStoresOnlyTheLasWeight(Possibilistic theory) {
        Set<Clause> constraints = theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).isEmpty()).collect(Collectors.toSet());
        List<Clause> rules = theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toList());
//        int minimum = rules.size();
        List<Double> weights = rules.stream().map(x -> Double.MAX_VALUE).collect(Collectors.toList());
//        weights.addAll(theory.getSoftRules().stream().map(Pair::getR).collect(Collectors.toList()));
        //rules.addAll(theory.getSoftRules().stream().map(Pair::getS).collect(Collectors.toList()));
        for (Clause clause : theory.getHardRules()) {
            if (LogicUtils.positiveLiterals(clause).size() > 1) {
                throw new IllegalStateException("not implemented for non-definite constraints");
            }
        }

        LeastHerbrandModel herbrandComputer = new LeastHerbrandModel();
        List<Clause> definiteRules = theory.getSoftRules().stream().map(Pair::getS).collect(Collectors.toList());
        // binary search
        //int start = minimum - 1;
        int start = 0;
        int end = theory.getSoftRules().size();
        Set<Literal> provableEvidence = herbrandComputer.herbrandModel(Sugar.listFromCollections(rules, definiteRules), this.evidence, constraints);
        if (null == provableEvidence) {
            provableEvidence = herbrandComputer.herbrandModel(rules, this.evidence, constraints);
            if (null == provableEvidence) {
                return new StratifiedCollector(this.evidence, 0);
            }
            provableEvidence = herbrandComputer.herbrandModel(Sugar.listFromCollections(rules, definiteRules.subList(0, 1)), this.evidence, constraints);
            if (null == provableEvidence) {
                return new StratifiedCollector(this.evidence, 0);
            }
            /**/
            if (true) {
                while (true) {
                    int half = (end - start) / 2 + start;
                    List<Clause> cut = rules.isEmpty() ? definiteRules.subList(start, half) : Sugar.listFromCollections(rules, definiteRules.subList(start, half));
                    provableEvidence = herbrandComputer.herbrandModel(cut, this.evidence, constraints);

                    if ((end - start) <= 1) { // in case of 1, half = start; the end stands for cut which is already inconsistent
                        break;
                    }

                    if (null == provableEvidence) {
                        end = half;
                    } else {
                        start = half;
                    }
                }/**/
            } else {
                /**/
                for (int idx = end; idx >= start; idx--) {
                    provableEvidence = herbrandComputer.herbrandModel(rules.subList(0, idx), this.evidence, constraints);
                    if (null != provableEvidence) {
                        int finished = idx;
                        break;
                    }
                }/**/
            }
        }
        double weight = Double.MAX_VALUE;
        if (start < theory.getSoftRules().size() && start >= 0) {
            weight = theory.getSoftRules().get(start).getR();
        }

/*        double weight = Double.MAX_VALUE;
        if (false) {
            if (null == provableEvidence) {
                provableEvidence = Sugar.set();
            } else {
                int cutRule = start - minimum;
                if (cutRule < 0) {
                    weight = Double.MAX_VALUE;
                } else if (theory.getSoftRules().size() > cutRule) {
                    weight = theory.getSoftRules().get(cutRule).getR();
                } else {
                    System.out.println(minimum + "\t" + start + "\t" + cutRule);
                    throw new IllegalStateException();
                }
            }
        } else{
            if(finised < 0){

            }else{

            }
        }
*/
        StratifiedCollector collector = new StratifiedCollector(this.evidence, 0);
        if (null != provableEvidence) {
            for (Literal literal : provableEvidence) {
                if (!this.evidence.contains(literal)) {
                    collector.add(literal, weight);
                }
            }
        }
        return collector;
    }

    // classical entailment
    public KECollector entails(FOL theory) {
        return entails(theory, this.evidence);
    }

    // classical entailment
    public KECollector entails(FOL theory, Iterable<Literal> groundEvidence) {
        LeastHerbrandModel herbrandComputer = new LeastHerbrandModel();
        Set<Literal> provableEvidence = herbrandComputer.herbrandModel(theory.getImplications(), groundEvidence, theory.getHardRules());

        if (null == provableEvidence) {
            return KECollector.create(this.evidence);
        }

        /*Matching world = Matching.get(new Clause(provableEvidence), Matching.THETA_SUBSUMPTION);
        for (Clause clause : theory.getHardRules()) {
            if (LogicUtils.positiveLiterals(clause).size() > 0) {
                throw new NotImplementedException();
            }
            if (world.subsumption(LogicUtils.flipSigns(clause), 0)) {
                // there is some constraint violated
                System.out.println("clash");
                return KECollector.get(this.evidence);
            }
        }*/


        KECollector collector = KECollector.create(this.evidence);
        for (Literal literal : provableEvidence) {
            if (!this.evidence.contains(literal)) {
                collector.add(literal, Subset.create(0));
            }
        }
        return collector;
    }

    // this does not return weights for predicted literals
    public Entailed oneChain(Theory theory) {
        Theory predictiveRules = FOL.create(Sugar.set(), theory.allRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toSet()));
        return solveFol(predictiveRules, Integer.MAX_VALUE, true);
    }

    public Entailed entails(Theory theory, int k) {
        if (theory instanceof FOL) {
            Pair<Theory, Integer> pair = (theory instanceof FOL) ? new Pair<>(constraintsAndDefiniteRulesApart((FOL) theory), 0) : convertToFol(theory);
        /*if (!onlyConstraintsWithNegativeLiterals(theory)) {
            System.out.println("This theory cannot be used, because it contains hard rules with positive literals");
            return null;
        }*/
            if (0 == pair.s) {
                return solveFol(pair.r, k, false);
            }
        }
        return solvePoss8((Possibilistic) theory, k, true);
/*
        //return solvePoss(pair.r, pair.s, k);
        //return solvePoss2(pair.r, pair.s, k);
        //return solvePoss3((Possibilistic) theory, k);
        if (System.getProperty("devMod", "").endsWith("standard")) {
            System.out.println("standard devMod");
            return solvePoss4((Possibilistic) theory, k); // back to root implementace
        } else if (System.getProperty("devMod", "").endsWith("Opt")) {
            System.out.println("new implementation opt");
            return solvePoss5((Possibilistic) theory, k); // back to root implementace
        } else if (System.getProperty("devMod", "").endsWith("Opt4")) {
            System.out.println("new implementation opt4");
            return solvePoss7((Possibilistic) theory, k); // back to root implementace
        } else if (System.getProperty("devMod", "").endsWith("Opt3")) {
            System.out.println("new implementation opt3");
            return solvePoss6((Possibilistic) theory, k); // back to root implementace
       } else if (System.getProperty("devMod", "").endsWith("AllOpt5")) {
            System.out.println("new implementation opt5");
            return solvePoss8((Possibilistic) theory, k, false); // back to root implementace
        } else if (System.getProperty("devMod", "").endsWith("CurOpt5")) {
            System.out.println("new implementation opt5");
            return solvePoss8((Possibilistic) theory, k, true); // back to root implementace
        } else if (System.getProperty("devMod", "").endsWith("burteforce")) {
            System.out.println("bruteforce implementation");
            return solvePoss9((Possibilistic) theory, k); // back to root implementace
        }
        return solveDebugPoss2((Possibilistic) theory, k);
        //return solveDebugPoss((Possibilistic) theory, k);
        //throw new IllegalStateException();
*/
    }

    private FOL constraintsAndDefiniteRulesApart(FOL theory) {
        Set<Clause> definiteRules = Sugar.set();
        Set<Clause> constraints = Sugar.set();
        for (Clause clause : theory.allRules()) {
            Set<Literal> posLit = LogicUtils.positiveLiterals(clause);
            if (posLit.isEmpty()) {
                constraints.add(clause);
            } else if (posLit.size() == 1) {
                definiteRules.add(clause);
            } else {
                throw new IllegalStateException();
            }
        }

        return FOL.create(constraints, definiteRules);
    }
/* spatne toto, je to blbost
    private Entailed solvePossVariantion2(Theory inputTheory, int hardImplicationsInSoftRules, int k) {
        Possibilistic theory = (Possibilistic) inputTheory; // this theory contains constraints as hard rules and rules with acc 1.0 which are also hard are in implications (soft rules); their number is denoted by hardImplicationsInSoftRules
        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);

        System.out.println("var2 theory\t" + theory.getHardRules().size() + "\t" + theory.getSoftRules().size() + "\tbut start from\t" + hardImplicationsInSoftRules);

        for (int idx = theory.getSoftRules().size(); idx >= hardImplicationsInSoftRules; idx--) {
            System.out.println("\t" + idx + "\t/\t" + hardImplicationsInSoftRules);
            FOL cut = FOL.get(theory.getHardRules(), theory.getSoftRules().subList(0, idx).stream().map(Pair::getS).collect(Collectors.toSet()));
            KECollector entailed = solveFol(cut, k, false);
            if (entailed.getEntailed().size() == this.evidence.size()) {
                System.out.println("nothing was entailed, end***");
                continue;
            }
            System.out.println("on cut\t" + idx + "\t/\t" + theory.getSoftRules().size() + "\tentailed\t" + entailed.getEntailed().size() + "\t on evidence\t" + this.evidence.size());
            return entailed;
        }
        SubsetFactory.getInstance().clear();
        return retVal;
    }
    */

//    private Entailed solvePoss2(Theory inputTheory, int hardImplicationsInSoftRules, int k) {
//        Possibilistic theory = (Possibilistic) inputTheory; // this theory contains constraints as hard rules and rules with acc 1.0 which are also hard are in implications (soft rules); their number is denoted by hardImplicationsInSoftRules
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//
//        for (int idx = hardImplicationsInSoftRules; idx <= theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            double currentRuleWeight = idx == hardImplicationsInSoftRules ? Double.MAX_VALUE : theory.getSoftRules().get(idx).r;
//            KECollector entailed = solveFol(FOL.get(theory.getHardRules(), theory.getSoftRules().subList(0, hardImplicationsInSoftRules).stream().map(Pair::getS).collect(Collectors.toSet())), k, false);
//            entailed.getEntailed().forEach(l -> {
//                if (!this.evidence.contains(l)) {
//                    retVal.add(l, currentRuleWeight);
//                }
//            });
//        }
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }

    //    List<Literal> debug = Sugar.list("advisedBy(1:person402, 1:person235)", "Faculty_adjunct(1:person235)").stream().map(Literal::parseLiteral).collect(Collectors.toList());

//    private Entailed solvePoss4(Possibilistic theory, int k) {
//        System.out.println("tady bych mel byt solvePoss4");
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
//        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());
//
//        List<Clause> negatedConstraints = preprocessConstraints(constraints);
//        List<Pair<Clause, Literal>> negatedRules = preprocessRules(predictive);
//
//        SupportsHolder holder = SupportsHolder.get(evidence, k);
//        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            double weight = Double.MAX_VALUE;
//            if (idx >= 0) {
//                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
//                weight = currentRule.r;
//                negatedRules.addAll(preprocessRules(Sugar.list(theory.getSoftRules().get(idx).getS())));
//            }
//            long iteration = 0;
//            while (true) {
//                System.out.println("iteration\t" + iteration);
////                holder.forwardRules(negatedRules, debugLastIteration == idx ? -1 : iteration); // debug, potom zmenit
//                holder.forwardRules(negatedRules, iteration);
//
//                if (quickDebug) {
//                    System.out.println(holder.debugPrint().hashCode());
//                }
//
//                boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration);
//                if (nextRoundNotNeeded) {
//                    break;
//                }
//                iteration++;
//                /*tohle je spatne ! mohlo by pridat veci ktere nemaji platit; nicmene u entailmentu bez constraints je vysledek stejny
//                for (Literal literal : holder.entailed().getEntailed()) {
//                    retVal.add(literal, weight);
//                }
//                */
//
//                if (debugLastIteration == idx) {
//                    System.out.println("debug out");
//                    System.out.println(holder.debugPrint());
//                }
//            }
//            for (Literal literal : holder.entailed().getEntailed()) {
//                retVal.add(literal, weight);
//            }
//
//            if (debugLastIteration == idx) {
//                System.out.println(holder.debugPrint());
//                retVal.getMemory().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
//                System.out.println("debug end");
//                break;
//            }
//            if (quickDebug) {
//                System.out.println(holder.debugPrint().hashCode());
//            }
//
//        }
//        if (quickDebug) {
//            System.out.println(holder.debugPrint());
//        }
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }

//    private Entailed solvePoss5(Possibilistic theory, int k) {
//        System.out.println("tady bych mel byt solvePoss5");
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
//        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());
//
//        List<Clause> negatedConstraints = preprocessConstraints(constraints);
//        List<Pair<Clause, Literal>> negatedRules = preprocessRules(predictive);
//
//        SupportsHolderOpt holder = SupportsHolderOpt.get(evidence, k);
//        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            double weight = Double.MAX_VALUE;
//            Set<Clause> newlyAddedRules = null;
//            if (idx >= 0) {
//                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
//                weight = currentRule.r;
//                List<Pair<Clause, Literal>> preprocessed = preprocessRules(Sugar.list(currentRule.getS()));
//                negatedRules.addAll(preprocessed);
//                newlyAddedRules = new HashSet<>(1);
//                newlyAddedRules.add(preprocessed.get(0).r);
//            } else {
//                newlyAddedRules = negatedRules.stream().map(Pair::getR).collect(Collectors.toSet());
//            }
//
//            long iteration = 0;
//            while (true) {
//                System.out.println("iteration\t" + iteration);
////                holder.forwardRules(negatedRules, debugLastIteration == idx ? -1 : iteration, newlyAddedRules); // debug, potom zmenit
//                holder.forwardRules(negatedRules, iteration, newlyAddedRules);
//                boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration);
//                if (nextRoundNotNeeded) {
//                    break;
//                }
//                iteration++;
//
////                if (debugLastIteration == idx) {
////                    System.out.println("debug out");
////                    System.out.println(holder.debugPrint());
////                }
//
//            }
//            for (Literal literal : holder.entailed().getEntailed()) {
//                retVal.add(literal, weight);
//            }
//
////            if (debugLastIteration == idx) {
////                System.out.println(holder.debugPrint());
////                retVal.getMemory().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
////                System.out.println("debug end");
////                break;
////            }
////            System.out.println(holder.debugPrint().hashCode());
//
//        }
//
//        retVal.setMP(holder.debugPrint());
////        System.out.println(holder.debugPrint());
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }

//    private Entailed solvePoss6(Possibilistic theory, int k) {
//        System.out.println("tady bych mel byt solvePoss6 protoze opt holder 3");
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
//        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());
//
//        List<Clause> negatedConstraints = preprocessConstraints(constraints);
//        List<Pair<Clause, Literal>> negatedRules = preprocessRules(predictive);
//
//        Map<Pair<String, Integer>, Integer> predicatesToBitsets = createDictionary(evidence, theory.allRules());
//
//        SupportsHolderOpt3 holder = SupportsHolderOpt3.get(evidence, k, predicatesToBitsets);
//        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            double weight = Double.MAX_VALUE;
//            Set<Clause> newlyAddedRules = null;
//            if (idx >= 0) {
//                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
//                weight = currentRule.r;
//                List<Pair<Clause, Literal>> preprocessed = preprocessRules(Sugar.list(currentRule.getS()));
//                negatedRules.addAll(preprocessed);
//                newlyAddedRules = new HashSet<>(1);
//                newlyAddedRules.add(preprocessed.get(0).r);
//            } else {
//                newlyAddedRules = negatedRules.stream().map(Pair::getR).collect(Collectors.toSet());
//            }
//
//            if (idx == debugLastIteration) {
//                wantedHead = "affects(1:cell_Function, 1:organ_or_Tissue_Function)";
//            }
//
//            long iteration = 0;
//            while (true) {
//                System.out.println("iteration\t" + iteration);
////                holder.forwardRules(negatedRules, debugLastIteration == idx ? -1 : iteration, newlyAddedRules); // debug, potom zmenit
//                holder.forwardRules(negatedRules, iteration, newlyAddedRules);
//
//                if (quickDebug) {
//                    System.out.println(holder.debugPrint().hashCode());
//                }
//
//                boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration);
//                if (nextRoundNotNeeded) {
//                    break;
//                }
//                iteration++;
//
//                if (debugLastIteration == idx) {
//                    System.out.println("debug out");
//                    //System.out.println(holder.debugPrint());
//                }
//
//            }
//            for (Literal literal : holder.entailed().getEntailed()) {
//                retVal.add(literal, weight);
//            }
//
//            if (debugLastIteration == idx) {
//                System.out.println(holder.debugPrint());
//                retVal.getMemory().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
//                System.out.println("debug end");
//                break;
//            }
//            if (quickDebug) {
//                {
//                    System.out.println(holder.debugPrint().hashCode());
//                }
//            }
//
//        }
//        if (quickDebug) {
//            //System.out.println(holder.debugPrint());
//        }
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }

    //    //int debugLastIteration = 70;
    public static int debugLastIteration = -142;
    //    //        public static int debugLastIteration = 70;
    public static boolean quickDebug = false;
    //    //public static String wantedHead = "affects(1:biologic_Function, 1:mental_Process)";
    public static String wantedHead = "";
    public static Set<String> wantedHeads = Sugar.set();

//
//    private Entailed solveDebugPoss(Possibilistic theory, int k) {
//        System.out.println("debug poss opt3 vs opt5 after iteration finishes");
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//        StratifiedCollector retVal4 = new StratifiedCollector(this.evidence, k);
//        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
//        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());
//
//        List<Clause> negatedConstraints = preprocessConstraints(constraints);
//        List<Pair<Clause, Literal>> negatedRules = preprocessRules(predictive);
//
//        Map<Pair<String, Integer>, Integer> predicatesToBitsets = createDictionary(evidence, theory.allRules());
//
//        SupportsHolderOpt opt = SupportsHolderOpt.get(evidence, k);
//        //SupportsHolderOpt4 opt4 = SupportsHolderOpt4.get(evidence, k, predicatesToBitsets);
//        SupportsHolderOpt5 opt5 = SupportsHolderOpt5.get(evidence, k, predicatesToBitsets, true);
//        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            double weight = Double.MAX_VALUE;
//            Set<Clause> newlyAddedRules = null;
//            if (idx >= 0) {
//                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
//                weight = currentRule.r;
//                List<Pair<Clause, Literal>> preprocessed = preprocessRules(Sugar.list(currentRule.getS()));
//                negatedRules.addAll(preprocessed);
//                newlyAddedRules = new HashSet<>(1);
//                newlyAddedRules.add(preprocessed.get(0).r);
//
//                System.out.println("***** pridavam pravidlo\t" + Sugar.chooseOne(preprocessed).s + " <- " + Sugar.chooseOne(preprocessed).r);
//            } else {
//                newlyAddedRules = negatedRules.stream().map(Pair::getR).collect(Collectors.toSet());
//            }
//
//            if (idx == debugLastIteration) {
//                //wantedHead = "affects(1:biologic_Function, 1:disease_or_Syndrome)";
//                //wantedHead = "affects(1:biologic_Function, 1:mental_Process)";
//                //wantedHeads = Sugar.set("result_of(1:acquired_Abnormality, 1:biologic_Function)");
//                //wantedHeads = Sugar.set("co-occurs_with(1:pathologic_Function, 1:mental_or_Behavioral_Dysfunction)");
//            }
//
//
//            long iteration = 0;
//            while (true) {
//                System.out.println("opt see\titeration\t" + iteration);
//                opt.forwardRules(negatedRules, iteration, newlyAddedRules);
//                boolean nextRoundNotNeeded2 = opt.constraintsCheck(negatedConstraints, iteration);
//                if (nextRoundNotNeeded2) {
//                    break;
//                }
//                iteration++;
//
//            }
//            for (Literal literal : opt.entailed().getEntailed()) {
//                retVal.add(literal, weight);
//            }
//
//            long iteration4 = 0;
//            while (true) {
//                System.out.println("opt5 see\titeration\t" + iteration4);
//                opt5.forwardRules(negatedRules, iteration4, newlyAddedRules);
//                boolean nextRoundNotNeeded = opt5.constraintsCheck(negatedConstraints, iteration4);
//                if (nextRoundNotNeeded) {
//                    break;
//                }
//                iteration4++;
//            }
//            for (Literal literal : opt5.entailed().getEntailed()) {
//                retVal4.add(literal, weight);
//            }
///*
//            System.out.println("ended with\t" + iteration + "\t" + iteration4);
//            debugCheck(opt5, opt, idx, -1, "after iteration ended");
//            if (!retVal.asOutput().trim().equals(retVal4.asOutput().trim())) {
//                System.out.println("!entailed literals do not equal\t" + retVal.asOutput().hashCode() + "\t" + retVal4.asOutput().hashCode());
//                System.out.println("see opt3 result\n" + retVal.asOutput());
//                System.out.println("see opt4 result\n" + retVal4.asOutput());
//                System.out.println("proofs opt3\n" + opt.debugPrint().hashCode() + "\n" + opt.debugPrint());
//                System.out.println("proofs opt4\n" + opt5.debugPrint().hashCode() + "\n" + opt5.debugPrint());
//                System.exit(-4445786);
//            }*/
//        /*
//            if (debugLastIteration == idx) {
//                System.out.println(opt4.debugPrint());
//                retVal.getMemory().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
//                System.out.println("debug end");
//                break;
//            }
//            if (quickDebug) {
//                System.out.println(opt4.debugPrint().hashCode());
//            }
//            */
//        }
//        if (quickDebug) {
////            System.out.println(holder.debugPrint());
//        }
//
//        System.out.println("opt vysledky*****************\n" + opt.debugPrint());
//        System.out.println("opt5 vysledky*****************\n" + opt5.debugPrint());
//
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }
//
//    private Entailed solveDebugPoss2(Possibilistic theory, int k) {
//        System.out.println("debug poss opt5 vs opt5 after iteration finishes");
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//        StratifiedCollector retValPrune = new StratifiedCollector(this.evidence, k);
//        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
//        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());
//
//        List<Clause> negatedConstraints = preprocessConstraints(constraints);
//        List<Pair<Clause, Literal>> negatedRules = preprocessRules(predictive);
//
//        Map<Pair<String, Integer>, Integer> predicatesToBitsets = createDictionary(evidence, theory.allRules());
//
//        SupportsHolderOpt5 opt = SupportsHolderOpt5.get(evidence, k, predicatesToBitsets, false);
//        SupportsHolderOpt5 optPrune = SupportsHolderOpt5.get(evidence, k, predicatesToBitsets, true);
//        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            double weight = Double.MAX_VALUE;
//            Set<Clause> newlyAddedRules = null;
//            if (idx >= 0) {
//                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
//                weight = currentRule.r;
//                List<Pair<Clause, Literal>> preprocessed = preprocessRules(Sugar.list(currentRule.getS()));
//                negatedRules.addAll(preprocessed);
//                newlyAddedRules = new HashSet<>(1);
//                newlyAddedRules.add(preprocessed.get(0).r);
//
//                System.out.println("***** pridavam pravidlo\t" + Sugar.chooseOne(preprocessed).s + " <- " + Sugar.chooseOne(preprocessed).r);
//            } else {
//                newlyAddedRules = negatedRules.stream().map(Pair::getR).collect(Collectors.toSet());
//            }
//
//            if (idx == debugLastIteration) {
//                //wantedHead = "affects(1:biologic_Function, 1:disease_or_Syndrome)";
//                //wantedHead = "affects(1:biologic_Function, 1:mental_Process)";
//                //wantedHeads = Sugar.set("result_of(1:acquired_Abnormality, 1:biologic_Function)");
//                //wantedHeads = Sugar.set("co-occurs_with(1:pathologic_Function, 1:mental_or_Behavioral_Dysfunction)");
//                wantedHeads = Sugar.set("affects(1:organ_or_Tissue_Function, 1:disease_or_Syndrome)");
//            }
//
//            String optAfterFirstIteration = null;
//            long iteration = 0;
//            while (true) {
//                System.out.println("opt see\titeration\t" + iteration);
//                opt.forwardRules(negatedRules, iteration, newlyAddedRules);
//                if (0 == iteration) {
//                    optAfterFirstIteration = opt.debugPrint();
//                }
//
//                boolean nextRoundNotNeeded2 = opt.constraintsCheck(negatedConstraints, iteration);
//                if (nextRoundNotNeeded2) {
//                    break;
//                }
//                iteration++;
//
//            }
//            for (Literal literal : opt.entailed().getEntailed()) {
//                retVal.add(literal, weight);
//            }
//
//            String optPruneAfterFirstIteration = null;
//            long iteration4 = 0;
//            while (true) {
//                System.out.println("opt5 see\titeration\t" + iteration4);
//                optPrune.forwardRules(negatedRules, iteration4, newlyAddedRules);
//                if (0 == iteration) {
//                    optPruneAfterFirstIteration = opt.debugPrint();
//                }
//
//                boolean nextRoundNotNeeded = optPrune.constraintsCheck(negatedConstraints, iteration4);
//                if (nextRoundNotNeeded) {
//                    break;
//                }
//                iteration4++;
//            }
//            for (Literal literal : optPrune.entailed().getEntailed()) {
//                retValPrune.add(literal, weight);
//            }
//
//
//            if (!optAfterFirstIteration.equals(optPruneAfterFirstIteration)) {
//                System.out.println("they do not equal after the first iteration");
//            }
//
//            String ropt = opt.debugPrint();
//            String roptPrune = optPrune.debugPrint();
//
//            /*if (!ropt.equals(roptPrune)) {
//                System.out.println("they do not equal!");
//                System.out.println("\n----opt result");
//                //System.out.println(ropt);
//                System.out.println("\n----optPrune result");
//                //System.out.println(roptPrune);
//
//                System.exit(-7854785);
//            }*/
//        }
//
//
//        System.out.println("opt vysledky*****************\n" + opt.debugPrint());
//        System.out.println("optPrune vysledky*****************\n" + optPrune.debugPrint());
//
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }
//

    /*
    private Entailed solveDebugPossEachIteration(Possibilistic theory, int k) {
        System.out.println("debug poss opt3 vs opt4 each iteration check");
        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());

        List<Clause> negatedConstraints = preprocessConstraints(constraints);
        List<Pair<Clause, Literal>> negatedRules = preprocessRules(predictive);

        Map<Pair<String, Integer>, Integer> predicatesToBitsets = createDictionary(evidence, theory.allRules());

        SupportsHolderOpt3 opt3 = SupportsHolderOpt3.get(evidence, k, predicatesToBitsets);
        SupportsHolderOpt4 opt4 = SupportsHolderOpt4.get(evidence, k, predicatesToBitsets);
        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
            double weight = Double.MAX_VALUE;
            Set<Clause> newlyAddedRules = null;
            if (idx >= 0) {
                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
                weight = currentRule.r;
                List<Pair<Clause, Literal>> preprocessed = preprocessRules(Sugar.list(currentRule.getS()));
                negatedRules.addAll(preprocessed);
                newlyAddedRules = new HashSet<>(1);
                newlyAddedRules.add(preprocessed.get(0).r);
            } else {
                newlyAddedRules = negatedRules.stream().map(Pair::getR).collect(Collectors.toSet());
            }

            if (idx == debugLastIteration) {
                //wantedHead = "affects(1:biologic_Function, 1:disease_or_Syndrome)";
                //wantedHead = "affects(1:biologic_Function, 1:mental_Process)";
                wantedHeads = Sugar.set("result_of(1:human_caused_Phenomenon_or_Process, 1:cell_Function)"
                        , "result_of(1:laboratory_or_Test_Result, 1:biologic_Function)"
                        , "result_of(1:laboratory_or_Test_Result, 1:disease_or_Syndrome)"
                        , "result_of(1:organism_Function, 1:organ_or_Tissue_Function)");
            }


            long iteration = 0;
            while (true) {
                System.out.println("iteration\t" + iteration);
//                holder.forwardRules(negatedRules, debugLastIteration == idx ? -1 : iteration, newlyAddedRules); // debug, potom zmenit
                System.out.println("opt4 see");
                opt4.forwardRules(negatedRules, iteration, newlyAddedRules);
                System.out.println("opt3 see");
                opt3.forwardRules(negatedRules, iteration, newlyAddedRules);

                debugCheck(opt4, opt3, idx, iteration, "after forward pass");
                boolean nextRoundNotNeeded = opt4.constraintsCheck(negatedConstraints, iteration);
                boolean nextRoundNotNeeded2 = opt3.constraintsCheck(negatedConstraints, iteration);
                debugCheck(opt4, opt3, idx, iteration, "after constraint check");

                if (nextRoundNotNeeded != nextRoundNotNeeded2) {
                    System.out.println("different number of rounds\n\t" + idx + "\t" + iteration);
                    System.exit(-147147147);
                }
//                if (quickDebug) {
//                    System.out.println(opt4.debugPrint().hashCode());
//                }


                if (nextRoundNotNeeded) {
                    break;
                }
                iteration++;

                if (debugLastIteration == idx) {
                    System.out.println("debug out");
//                    System.out.println(holder.debugPrint());
                }

                if (idx >= debugLastIteration && debugLastIteration >= 0 && iteration >= 3) {
                    System.out.println("exiting here since the bug found by random execution should have happen :( try another round");
                    System.exit(754101215);
                }

            }
            for (Literal literal : opt4.entailed().getEntailed()) {
                retVal.add(literal, weight);
            }


//            if (debugLastIteration == idx) {
//                System.out.println(opt4.debugPrint());
//                retVal.getMemory().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
//                System.out.println("debug end");
//                break;
//            }
//            if (quickDebug) {
//                System.out.println(opt4.debugPrint().hashCode());
//            }

        }
        if (quickDebug) {
//            System.out.println(holder.debugPrint());
        }


        SubsetFactory.getInstance().clear();
        return retVal;
    }
*/

    /*private void debugCheck(SupportsHolderOpt5 opt5, SupportsHolderOpt opt0, int idx, long iteration, String where) {
        StringBuilder sb = new StringBuilder();

        opt0.map.keySet().stream().sorted(Comparator.comparing(Object::toString)).forEach(l0 -> {
            Set<Support> s5 = opt5.map.get(l0);
            if (null == s5) {
                s5 = Sugar.set();
            }
            Set<String> ss5 = s5.stream().map(opt5::toCanon).collect(Collectors.toSet());
            StringBuilder innerSb = new StringBuilder();

            opt0.map.get(l0).stream().map(opt0::toCanon).sorted().filter(sl0 -> !ss5.contains(sl0)).forEach(sl0 ->
                    innerSb.append("\n0\t" + sl0));

            String fs = innerSb.toString();
            if (!fs.isEmpty()) {
                sb.append("\n" + l0 + "\t" + opt0.map.get(l0).size() + "\t" + s5.size() + fs);

                sb.append("\n" + l0 + " diffs\nopt0\n\t" + opt0.map.get(l0).stream().map(opt0::toCanon).sorted().collect(Collectors.joining("\n\t"))
                        + "\nopt5\n\t" + ss5.stream().sorted().collect(Collectors.joining("\n\t "))
                );
            }
        });

        opt5.map.keySet().stream().sorted(Comparator.comparing(Object::toString)).forEach(l5 -> {
            Set<Support> s3 = opt0.map.get(l5);
            if (null == s3) {
                s3 = Sugar.set();
            }
            Set<String> ss3 = s3.stream().map(opt0::toCanon).collect(Collectors.toSet());
            StringBuilder innerSb = new StringBuilder();

            opt5.map.get(l5).stream().map(opt5::toCanon).sorted().filter(sl4 -> !ss3.contains(sl4)).forEach(sl4 ->
                    innerSb.append("\n4\t" + sl4));

            String fs = innerSb.toString();
            if (!fs.isEmpty()) {
                sb.append("\n" + l5 + "\t" + opt5.map.get(l5).size() + "\t" + s3.size() + fs);

                sb.append("\n" + l5 + " diffs\nopt0\n\t" + ss3.stream().sorted().collect(Collectors.joining("\n\t")) +
                        "\nopt5\n\t" + opt5.map.get(l5).stream().map(opt5::toCanon).sorted().collect(Collectors.joining("\n\t"))
                );
            }
        });


        String out = sb.toString();
        if (!out.isEmpty()) {
            // nejaka nedeterministicka chyba, muze za to poradi cart? vuci tomu by to melo byt nezavisle, ne?
            System.out.println("inequals in\t" + where + "\n\t" + idx + "\t" + iteration);
            System.out.println(opt0.debugPrint().hashCode() + "\t" + opt5.debugPrint().hashCode());
            System.out.println(out);

            System.out.println("debugs are\nopt3\n" + opt0.debugPrint() + "\nopt4\n" + opt5.debugPrint());
            System.exit(-147147148);
        }
    }*/


//    private Entailed solvePoss7(Possibilistic theory, int k) {
//        System.out.println("tady bych mel byt solvePoss7 protoze opt holder 4");
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
//        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());
//
//        List<Clause> negatedConstraints = preprocessConstraints(constraints);
//        List<Pair<Clause, Literal>> negatedRules = preprocessRules(predictive);
//
//        Map<Pair<String, Integer>, Integer> predicatesToBitsets = createDictionary(evidence, theory.allRules());
//
//        SupportsHolderOpt4 holder = SupportsHolderOpt4.get(evidence, k, predicatesToBitsets);
//        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            double weight = Double.MAX_VALUE;
//            Set<Clause> newlyAddedRules = null;
//            if (idx >= 0) {
//                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
//                weight = currentRule.r;
//                List<Pair<Clause, Literal>> preprocessed = preprocessRules(Sugar.list(currentRule.getS()));
//                negatedRules.addAll(preprocessed);
//                newlyAddedRules = new HashSet<>(1);
//                newlyAddedRules.add(preprocessed.get(0).r);
//            } else {
//                newlyAddedRules = negatedRules.stream().map(Pair::getR).collect(Collectors.toSet());
//            }
//
//            if (idx == debugLastIteration) {
//                wantedHead = "";
//            }
//
//
//            long iteration = 0;
//            while (true) {
//                System.out.println("iteration\t" + iteration);
////                holder.forwardRules(negatedRules, debugLastIteration == idx ? -1 : iteration, newlyAddedRules); // debug, potom zmenit
//                holder.forwardRules(negatedRules, iteration, newlyAddedRules);
//                boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration);
//
//                if (quickDebug) {
//                    System.out.println(holder.debugPrint().hashCode());
//                }
//
//
//                if (nextRoundNotNeeded) {
//                    break;
//                }
//                iteration++;
//
//                if (debugLastIteration == idx) {
//                    System.out.println("debug out");
////                    System.out.println(holder.debugPrint());
//                }
//
//            }
//            for (Literal literal : holder.entailed().getEntailed()) {
//                retVal.add(literal, weight);
//            }
//
//            if (debugLastIteration == idx) {
//                System.out.println(holder.debugPrint());
//                retVal.getMemory().entrySet().stream().sorted(Comparator.comparing(e -> e.getKey().toString())).forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
//                System.out.println("debug end");
//                break;
//            }
//            if (quickDebug) {
//                System.out.println(holder.debugPrint().hashCode());
//            }
//
//
//        }
//        if (quickDebug) {
////            System.out.println(holder.debugPrint());
//        }
//
//
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }

    private Entailed solvePoss8(Possibilistic theory, int k, boolean useExtraPruning) {
        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());


        Map<Pair<String, Integer>, Integer> predicatesToBitsets = createDictionary(evidence, theory.allRules());

//        SupportsHolderOpt5 holder = SupportsHolderOpt5.get(evidence, k, predicatesToBitsets, useExtraPruning);


//        System.out.println("solvePoss 8 with useExtraPruning\t" + useExtraPruning);
        List<Clause> negatedConstraints = preprocessConstraints(constraints);

//        SupportsHolderOpt5Backup holder = SupportsHolderOpt5Backup.create(evidence, k, predicatesToBitsets, useExtraPruning);
        SupportsHolderOpt6BSet holder = SupportsHolderOpt6BSet.create(evidence, k, predicatesToBitsets, useExtraPruning);
//        SupportsHolderOpt7ForwardOnly holder = SupportsHolderOpt7ForwardOnly.create(evidence, k, predicatesToBitsets, useExtraPruning);
//        SupportsHolderOpt8ForwardMoreThanOne holder = SupportsHolderOpt8ForwardMoreThanOne.create(evidence, k, predicatesToBitsets, useExtraPruning);
//        SupportsHolderOpt9ForwardBatch holder = SupportsHolderOpt9ForwardBatch.create(evidence, k, predicatesToBitsets, useExtraPruning);
//        SupportsHolderOpt10ForwardMoreThanOneFix holder = SupportsHolderOpt10ForwardMoreThanOneFix.create(evidence, k, predicatesToBitsets, useExtraPruning, negatedConstraints);
//        SupportsHolderOpt11ForwardMTOFConstraints holder = SupportsHolderOpt11ForwardMTOFConstraints.create(evidence, k, predicatesToBitsets, useExtraPruning, negatedConstraints);

        List<Triple<Clause, Literal, BSet>> negatedRules = preprocessRules(predictive, holder::constantsToSubset);

        //System.out.println(holder.getClass());

        //System.out.println("watch out, supportsHolderOpt5 and below do not handle partially grounded rule heads!");
        long start = System.nanoTime();
        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
            System.out.println("# \t" + idx + "\t/\t" + theory.getSoftRules().size() + "\t" + (System.nanoTime() - start) / 1000000000.0);
            double weight = Double.MAX_VALUE;
            Set<Triple<Clause, Literal, BSet>> newlyAddedRules = null;
            if (idx >= 0) {
                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
                weight = currentRule.r;
                List<Triple<Clause, Literal, BSet>> preprocessed = preprocessRules(Sugar.list(currentRule.getS()), holder::constantsToSubset);
                negatedRules.addAll(preprocessed);
                newlyAddedRules = new HashSet<>(1);
                newlyAddedRules.add(preprocessed.get(0));
            } else {
                newlyAddedRules = Sugar.setFromCollections(negatedRules);
            }

            long iteration = 0;

            while (true) {
                if (1 == iteration && 9 == idx) {
//                    SupportsHolderOpt6BSet.debugNow = true;
//                }else{///
//                    SupportsHolderOpt6BSet.debugNow = false;
                }
/*
                if (iteration == 0 && 28 == idx) {
                    holder.debug();
//                    holder.debugIt2();
                    System.out.println("debug end");
                    System.exit(-1);
                }
*/
                if (iteration == 0) {
                    //System.out.println("iteration\t" + iteration);
                }
                holder.forwardRules(negatedRules, iteration, newlyAddedRules);
                boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration);
                if (nextRoundNotNeeded) {
                    break;
                }
                iteration++;
            }
            for (Literal literal : holder.entailed().getEntailed()) {
                retVal.add(literal, weight);
            }
        }
        //retVal.setMP(holder.debugPrint());
        SubsetFactory.getInstance().clear();
        return retVal;
    }


//    private Entailed solvePoss9(Possibilistic theory, int k) {
//        System.out.println("tady bych mel byt solvePoss9 protoze bruteForce holder");
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//        Set<Clause> constraints = LogicUtils.constraints(theory.getHardRules());
//        Set<Clause> predictive = LogicUtils.definiteRules(theory.getHardRules());
//
//        List<Clause> negatedConstraints = preprocessConstraints(constraints);
//        List<Pair<Clause, Literal>> negatedRules = preprocessRules(predictive);
//
//        SupportsHolderBruteForce holder = SupportsHolderBruteForce.get(evidence, k);
//        for (int idx = -1; idx < theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            double weight = Double.MAX_VALUE;
//            Set<Clause> newlyAddedRules = null;
//            if (idx >= 0) {
//                Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
//                weight = currentRule.r;
//                List<Pair<Clause, Literal>> preprocessed = preprocessRules(Sugar.list(currentRule.getS()));
//                negatedRules.addAll(preprocessed);
//                newlyAddedRules = new HashSet<>(1);
//                newlyAddedRules.add(preprocessed.get(0).r);
//            } else {
//                newlyAddedRules = negatedRules.stream().map(Pair::getR).collect(Collectors.toSet());
//            }
//
//            long iteration = 0;
//            while (true) {
//                System.out.println("iteration\t" + iteration);
//                holder.forwardRules(negatedRules);
//                boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration);
//                if (nextRoundNotNeeded) {
//                    break;
//                }
//                iteration++;
//            }
//            for (Literal literal : holder.entailed().getEntailed()) {
//                retVal.add(literal, weight);
//            }
//        }
//        retVal.setMP(holder.debugPrint());
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }


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


//    private Entailed solvePoss3(Possibilistic theory, int k) {
////        System.out.println("input values\t"
////                + this.evidence.stream().map(Literal::toString).sorted().collect(Collectors.joining(" ")).hashCode()
////                + "\t" + theory.allRules().stream().map(Clause::toString).sorted().collect(Collectors.joining(" ")).hashCode());
//
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//        Set<Clause> constraints = theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).isEmpty()).collect(Collectors.toSet());
//        Set<Clause> predictive = theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toCollection(LinkedHashSet::new));
//
//        KECollector entailed = solveFol(FOL.get(constraints, predictive), k, false);
//        entailed.getEntailed().forEach(l -> {
//            if (!this.evidence.contains(l)) {
//                retVal.add(l, Double.MAX_VALUE);
//            }
//        });
//
//        for (int idx = 0; idx < theory.getSoftRules().size(); idx++) {
////            System.out.println("debug2");
////            idx = theory.getSoftRules().size() - 2;
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            Pair<Double, Clause> current = theory.getSoftRules().get(idx);
//            predictive.add(current.s);
//            double currentRuleWeight = current.r;
//            entailed = solveFol(FOL.get(constraints, predictive), k, false);
//            entailed.getEntailed().forEach(l -> {
//                if (!this.evidence.contains(l)) {
//                    retVal.add(l, currentRuleWeight);
//                }
//
//
////                if (debug.contains(l)) {
////                    System.out.println("tady predavam\t" + l);
////                }
//            });
////            System.out.println("idx\t" + idx + "\t" + entailed.getEntailed().size());
////            System.out.println("debug2");
////            break;
////            System.out.println("\t" + retVal.getMemory().size());
//        }
//
////        System.out.println(retVal.getMemory().size());
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }


//    private Entailed solvePoss(Theory inputTheory, int hardImplicationsInSoftRules, int k) {
//        Possibilistic theory = (Possibilistic) inputTheory; // this theory contains constraints as hard rules and rules with acc 1.0 which are also hard are in implications (soft rules); their number is denoted by hardImplicationsInSoftRules
//        StratifiedCollector retVal = new StratifiedCollector(this.evidence, k);
//
//        List<Clause> negatedConstraints = preprocessConstraints(theory.getHardRules());
//        SupportsHolder holder = SupportsHolder.get(evidence, k);
//        List<Pair<Clause, Literal>> negatedRules = preprocessRules(theory.getSoftRules().subList(0, hardImplicationsInSoftRules).stream().map(Pair::getS).collect(Collectors.toSet()));
//        for (int idx = hardImplicationsInSoftRules; idx < theory.getSoftRules().size(); idx++) {
//            System.out.println("\t" + idx + "\t/\t" + theory.getSoftRules().size());
//            Pair<Double, Clause> currentRule = theory.getSoftRules().get(idx);
//            /*if(idx +1 < theory.getSoftRules().size()
//                    && 0 == Double.compare(theory.getSoftRules().get(idx+1).r, currentRule.r)){
//                continue; // hack for rules with the same weights
//            }*/
//            negatedRules.addAll(preprocessRules(Sugar.set(theory.getSoftRules().get(idx).getS())));
//            long iteration = 0;
//            while (true) {
//                // tady by se dalo vnutit aby to nehledalo jiste groundingy, ktere maji vice konstant nez je k
//
//                //System.out.println("mozna tady budou problemy s tema iteracema :(");
//
////                System.out.println("iteration\t" + iteration);
//                holder.forwardRules(negatedRules, iteration);
//
//                //System.out.println("\n********************current world\n\t" + holder.entailed().asOutput() + "\n.............................\n");
//                //System.out.println("current world\t" + ((KECollector) holder.entailed()).getEntailed().size());
//
////                System.out.println("constraints check");
//                boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration);
//
//                //System.out.println("\n--------------------current world\n\t" + holder.entailed().asOutput() + "\n.............................\n");
//                //System.out.println("current world\t" + ((KECollector) holder.entailed()).getEntailed().size());
//
//                if (nextRoundNotNeeded) {
//                    break;
//                }
//                iteration++;
////            if (iteration > 10) {
////                System.out.println("debug break");
////                break;
////            }
//            }
//
//            //System.out.println("problem je v tom ze human(liz) dostane support liz -- bude se to spatne uklada a nebo je spatne udelane constraint check");
//
//            //SubsetFactory.getInstance().clear();
//            holder.entailed().getEntailed().forEach(l -> {
//                if (!this.evidence.contains(l)) {
//                    retVal.add(l, currentRule.r);
//                }
//            });
//        }
//
//        SubsetFactory.getInstance().clear();
//        return retVal;
//    }

    private KECollector solveFol(Theory inputTheory, int k, final boolean endAfterFirstRound) {
        FOL theory = null;
        if (inputTheory instanceof Possibilistic) {
            if (LogicUtils.definiteRules(inputTheory.getHardRules()).size() > 0) {
                throw new IllegalStateException("shift definite rules from hard constraints to soft ones firstly ;)");
            }
            theory = FOL.create(inputTheory.getHardRules(), ((Possibilistic) inputTheory).getSoftRules().stream().map(Pair::getS).collect(Collectors.toSet()));
        } else if (inputTheory instanceof FOL) {
            theory = (FOL) inputTheory;
        } else {
            throw new IllegalStateException();//throw new NotImplementedException();
        }

        List<Clause> negatedConstraints = preprocessConstraints(theory.getHardRules());
        Map<Pair<String, Integer>, Integer> predicatesToBitsets = createDictionary(evidence, theory.allRules());

        //negatedRules.
        //List<Pair<Clause, Literal>> negatedRules = preprocessRules(theory.getImplications());
        //List<Clause> negatedConstraints = preprocessConstraints(theory.getHardRules());
        //SupportsHolder holder = SupportsHolder.get(evidence, k);
//        SupportsHolderOpt5 holder = SupportsHolderOpt5.get(evidence, k, predicatesToBitsets, true);

        //SupportsHolderOpt5Backup holder = SupportsHolderOpt5Backup.get(evidence, k, predicatesToBitsets, true);
        SupportsHolderOpt6BSet holder = SupportsHolderOpt6BSet.create(evidence, k, predicatesToBitsets, true);

        List<Triple<Clause, Literal, BSet>> negatedRules = preprocessRules(theory.getImplications(), holder::constantsToSubset);
        Set<Triple<Clause, Literal, BSet>> newlyAddedRules = Sugar.setFromCollections(negatedRules);

        long iteration = 0;
        while (true) {
            // tady by se dalo vnutit aby to nehledalo jiste groundingy, ktere maji vice konstant nez je k

            //System.out.println("mozna tady budou problemy s tema iteracema :(");

            System.out.println("# iteration\t" + iteration);
            holder.forwardRules(negatedRules, iteration, newlyAddedRules);

            //System.out.println("\n********************current world\n\t" + holder.entailed().asOutput() + "\n.............................\n");
            //System.out.println("current world\t" + ((KECollector) holder.entailed()).getEntailed().size());

            System.out.println("# constraints check");
            boolean nextRoundNotNeeded = holder.constraintsCheck(negatedConstraints, iteration);

            //System.out.println("\n--------------------current world\n\t" + holder.entailed().asOutput() + "\n.............................\n");
            //System.out.println("current world\t" + ((KECollector) holder.entailed()).getEntailed().size());

            if (nextRoundNotNeeded) {
                break;
            }
            iteration++;
//            if (iteration > 10) {
//                System.out.println("debug break");
//                break;
//            }
            if (endAfterFirstRound) {
                break;
            }
        }

        //System.out.println("problem je v tom ze human(liz) dostane support liz -- bude se to spatne uklada a nebo je spatne udelane constraint check");
//        holder.debugIt();

        SubsetFactory.getInstance().clear();
        return holder.entailed();

    }

    // tak tenhle nazev doze zmast
    private Pair<Theory, Integer> convertToFol(Theory theory) {
        if (!(theory instanceof Possibilistic)) {
            throw new IllegalStateException();
        }

        Possibilistic input = (Possibilistic) theory;
        if (!input.getConstrainsHornOthers().t.isEmpty()) {
            throw new IllegalStateException();
        }

        List<Pair<Double, Clause>> horns = theory.getHardRules().stream()
                .filter(c -> LogicUtils.positiveLiterals(c).size() == 1)
                .map(c -> new Pair<>(Double.MAX_VALUE, c))
                .collect(Collectors.toList());
        int hardImplications = horns.size();
        horns.addAll(input.getSoftRules());

        Possibilistic poss = Possibilistic.create(theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() < 1).collect(Collectors.toSet()),
                horns);

        return new Pair<>(poss, hardImplications);
    }

    private List<Clause> preprocessConstraints(Set<Clause> hardRules) {
        return hardRules.stream().map(LogicUtils::flipSigns).collect(Collectors.toList());
    }

    private List<Triple<Clause, Literal, BSet>> preprocessRules(Collection<Clause> clauses, Function<Literal, BSet> literalToSubset) {
        // return triple < negatedBody , positive head , set of constants in head> (i.e. the negated body is existentially quantified literals from body of the horn rule)
        // the set of constants is here because of partially grounded rules, e.g. "manages(professional_or_occupational_group, Y)	<-	carries_out(Y, governmental_or_regulatory_activity), @alldiff(Y)"
        return clauses.stream()
                .map(clause -> {
                    if (!LogicUtils.isRangeRestricted(clause)) {
                        throw new IllegalStateException("clauses have to be range restricted!\n\t" + clause);
                    }
                    Literal positiveHead = Sugar.chooseOne(LogicUtils.positiveLiterals(clause));
                    Clause flippedBody = new Clause(LogicUtils.flipSigns(Sugar.setDifference(clause.literals(), Sugar.set(positiveHead))));
                    return new Triple<>(flippedBody, positiveHead, literalToSubset.apply(positiveHead));
                })
                .collect(Collectors.toList());
    }

    /*
        private Stream<Pair<Literal, Subset>> findViolatedRulesWithNonMinimalFiltering(Matching world, List<Pair<Literal, Clause>> negatedRules, int k, Map<Literal, Set<Subset>> entailed) {
            return negatedRules.stream()
                    .flatMap(pair -> {
                        Pair<Term[], List<Term[]>> substitutions = world.allSubstitutions(pair.s, 0, Integer.MAX_VALUE);
                        List<Pair<Literal, Subset>> retVal = Sugar.list();
                        for (Term[] substitution : substitutions.s) {
                            Set<Constant> constants = LogicUtils.constants(substitution);
                            if (constants.size() <= k) {
                                Subset subset = constantsToSubset(constants);
                                Set<Subset> support = entailed.get(subset);
                                if (null == support || !support.stream().parallel().anyMatch(s -> s.isSubsetOf(subset))) {
                                    retVal.add(new Pair<>(LogicUtils.substitute(pair.r, substitutions.r, substitution), subset));
                                }
                            }
                        }
                        return retVal.stream();
                    });
        }
    private Map<Literal, Set<Subset>> initialize(int k) {
        Map<Literal, Set<Subset>> retVal = new HashMap<>();
        for (Literal literal : this.evidence) {
            Set<Constant> constants = LogicUtils.constantsFromLiteral(literal);
            retVal.put(literal, Sugar.set(constantsToSubset(constants)));
        }
        return retVal;
    }


    */

    private boolean onlyConstraintsWithNegativeLiterals(FOL theory) {
        return theory.getHardRules().stream().allMatch(c -> LogicUtils.positiveLiterals(c).size() < 1);
    }

    public static SupportEntailmentInference create(Set<Literal> evidence) {
        return new SupportEntailmentInference(evidence);
    }


    // this returns also maximal weight for derived literal
    public StratifiedCollector oneStep(Theory theory) {
        // constraints are not used in this type of inference
        List<Triple<Literal, Clause, Double>> preparedRules = Sugar.list();
        if (theory instanceof FOL) {
            throw new IllegalStateException("this call is very very stupid, use one chain instead ;) (always the same value would propagate ;))");
        } else if (theory instanceof Possibilistic) {
            ((Possibilistic) theory).getHardRules().stream().filter(c -> LogicUtils.isDefiniteRule(c)).forEach(c -> {
                HornClause horn = HornClause.create(c);
                preparedRules.add(new Triple<>(horn.head(), horn.toExistentiallyQuantifiedConjunction(), Double.MAX_VALUE));
            });
            ((Possibilistic) theory).getSoftRules().forEach(p -> {
                HornClause horn = HornClause.create(p.s);
                preparedRules.add(new Triple<>(horn.head(), horn.body(), p.r));
            });
        } else {
            throw new IllegalStateException();
        }

        Map<Literal, Double> literals = new HashMap<>(2 * this.evidence.size());
        Matching world = Matching.create(new Clause(this.evidence), Matching.THETA_SUBSUMPTION);

        for (Triple<Literal, Clause, Double> triple : preparedRules) {
            Pair<Term[], List<Term[]>> sub = world.allSubstitutions(triple.s, 0, Integer.MAX_VALUE);
            Term[] src = sub.r;
            LogicUtils.substituteStatefullyPreparation(src);
            for (Term[] img : sub.s) {
                //Literal substitutedHead = LogicUtils.substitute(triple.r, src, img);
                Literal substitutedHead = LogicUtils.substituteStatefully(triple.r, img);
                if (literals.containsKey(substitutedHead)) {
                    Double val = literals.get(substitutedHead);
                    if (val < triple.t) { // this can be redundant since walking in descending order of rule weights should be enough
                        literals.put(substitutedHead, triple.t); // we are storing maximum
                    }
                } else {
                    literals.put(substitutedHead, triple.t);
                }
            }
        }

        StratifiedCollector collector = new StratifiedCollector(this.evidence, 0);
        literals.entrySet().forEach(e -> {
            if (!this.evidence.contains(e.getKey())) {
                collector.add(e.getKey(), e.getValue());
            }
        });
        return collector;
    }
}
