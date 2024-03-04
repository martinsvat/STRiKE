package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.*;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.searchPruning.evaluation.BreadthResults;
import ida.searchPruning.search.MutableStats;
import ida.searchPruning.search.SimpleLearner;
import ida.searchPruning.search.collections.SearchNodeInfo;
import ida.utils.MutableDouble;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.utils.collections.DoubleCounters;
import ida.utils.collections.MultiList;
import ida.utils.tuples.Pair;
import logicStuff.learning.constraints.shortConstraintLearner.UltraShortConstraintLearnerFaster;
import logicStuff.learning.datasets.*;
import logicStuff.learning.languageBias.LanguageBias;
import logicStuff.learning.saturation.ConjunctureSaturator;
import logicStuff.learning.saturation.ConstantSaturationProvider;
import logicStuff.theories.TheorySimplifier;
import logicStuff.theories.TheorySolver;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


/**
 * Created by martin.svatos on 3. 4. 2018.
 */
public class PSLearning {

    private PSLearning() {
    }


    public static PSLearning create() {
        return new PSLearning();
    }

    public void learn(Set<Literal> evidence) {
        System.out.println("zkontrolovat jeste ten rule miner, asi tam je nejaka chyba");
        TimeDog dog = new TimeDog(15 * 60 * 1000_000_000, true); // divne toto
        int minSupp = 1;

        //vzit ty pravidla co to naslo a znovu je propocitat na k/v-entailment, protoze jsem dal mozna moc male k :( aspon pocet promennych, ale max 5, vic asi nedame

        Clause evidenceClause = new Clause(evidence);
        long constraintTime = System.nanoTime();
        MEDataset med = MEDataset.create(Sugar.list(evidenceClause), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);


        UltraShortConstraintLearnerFaster cl = UltraShortConstraintLearnerFaster.create(med, 2, 4, 0, 1, 1, Integer.MAX_VALUE);
        Pair<List<Clause>, List<Clause>> constraints = cl.learnConstraints(true, false, false, Integer.MAX_VALUE, Sugar.list(), true, 2, false, Sugar.set(), minSupp);
//        Pair<List<Clause>, List<Clause>> constraints = new Pair<>(Sugar.list(),Sugar.list());

        long finalConstraintTime = System.nanoTime() - constraintTime;

        //Set<Clause> hardConstraintsSimplified = Sugar.setFromCollections(simplify(constraints.getR()));
        Set<Clause> hardConstraintsSimplified = Sugar.setFromCollections(constraints.getR());

        Coverage zeroCover = CoverageFactory.getInstance().take();
        Coverage totalCoverage = CoverageFactory.getInstance().take(0);

        Pair<Set<Literal>, Set<Clause>> adHocSymmetriciExtension = adhocSymmetricity(evidence, hardConstraintsSimplified);
        evidence = adHocSymmetriciExtension.r;
        Set<Clause> hardConstraints = adHocSymmetriciExtension.s;

        TestComputing entailment = TestComputing.create(evidence, Sugar.set());
        Set<IsoClauseWrapper> theory = Sugar.set();

        int subsampleSize = 100;
        int stepSize = 10;
        med.allPredicates().stream()
                //.filter(predicate -> predicate.getS() == 2 && predicate.getR().equals("interaction")) // debug filter
//                .filter(predicate -> !Sugar.set("location","function","complex","enzyme","protein_class","interaction").contains(predicate.getR()))
                .filter(predicate -> !predicate.r.equals("phenotype"))
                .filter(predicate -> predicate.r.equals("location"))
                .forEach(predicate -> {
                    System.out.println("\n\n*******\nsolving for predicate " + predicate.getR() + "/" + predicate.getS());

                    PacDataset pcd = new PacDataset(Clause.parse(evidenceClause.toString()));

                    // hack around to compute criterion we want, awful
                    // returns -2 if the body does not cover anything
                    // returns -1 if it is not range restricted
                    java.util.function.Function<SearchNodeInfo,SearchNodeInfo> evalStrategy = (sni) -> {
                        Clause clause = sni.getRule().toClause();
                        List<Term> variables = LogicUtils.variables(clause).stream().map(v -> (Term) v).collect(Collectors.toList());
                        Literal allDiff = new Literal(SpecialVarargPredicates.ALLDIFF, true, variables);
                        HornClause alldiffedHorn = HornClause.create(new Clause(Sugar.iterable(clause.literals(), Sugar.list(allDiff))));// alldiffied
                        if (!LogicUtils.isRangeRestricted(clause)) {
                            sni.setAllowability(false);
                            sni.setAccuracy(-1);
                            sni.setCoverages(totalCoverage,zeroCover);
                            return sni;
                        }
                        boolean atLeastOne = pcd.numExistentialMatches(alldiffedHorn, 1) > 0;
                        if (!atLeastOne) {
                            // takovy pravidlo by melo byt uplne zahozeno kvuli monotonicite
                            return null;
                            //return new SearchNodeInfo(zeroCover, zeroCover, horn, -2, false, totalCoverage);
                        }
                        //System.out.println("computing score for\t" + alldiffedHorn);
                        sni.setAccuracy(LogicUtils.isRangeRestricted(sni.getRule()) ? pcd.subsampleScore(alldiffedHorn, subsampleSize, stepSize) : -1);
                        sni.setAllowability(false);
                        return sni;
                    };

                    MutableStats stats = new MutableStats();
                    SimpleLearner sl = new SimpleLearner(pcd, ConstantSaturationProvider.createFilterSaturator(hardConstraints, constraints.getS()),
                            CoverageFactory.getInstance().get(pcd.size()), null, dog, stats, minSupp);

                    int maxDepth = 5;
                    int maxVariables = 5;
                    int beam = 10;
                    HornClause head = HornClause.create(Clause.parse(predicate.getR() + "(" + IntStream.range(0, predicate.getS()).mapToObj(i -> "V" + i).collect(Collectors.joining(",")) + ")"));

                    LanguageBias<HornClause> lb = () -> (horn) -> horn.variables().size() <= maxVariables;

                    //System.out.println("tady pridat jeste filtr na subsumovane uz drive pridane !!!!!!!!!"); uz je uvnitr BFS
                    // uvnitr refinement kroku simpleLearner je explicitne zakazano aby vznikla tautologie
                    // uncomment this to run the rule learning
                    /**/
                    BreadthResults result = sl.breadthFirstSearch(maxDepth, System.nanoTime(), finalConstraintTime, lb, beam, head, evalStrategy, theory, null); // TODO actualize
//                    result.getRules().stream()
//                            .flatMap(sni -> sni.stream())
//                            .forEach(sni -> System.out.println(sni.getAccuracy() + "\t" + sni.getRule()));

                    System.out.println("hula hop -------------------------------");

                    List<SearchNodeInfo> sorted = result.getRules().stream()
                            .flatMap(snis -> snis.stream())
                            .filter(sni -> sni.getAccuracy() > -1 || sni.getRule().body().countLiterals() < 1) // just a hack, -1 is for not range restricted clauses, -2 is for no existential matches at all
                            .filter(sni -> sni.getAccuracy() > 0.0) // just an evaluate hack to evaluate those which have at least one sampled true grounding
                            .sorted(Comparator.comparingDouble(SearchNodeInfo::getAccuracy).reversed())
                            .collect(Collectors.toList());

                    sorted.forEach(sni -> System.out.println(sni.getAccuracy() + "\t" + sni.getRule()));
                     /**/
                    /*** /
                     List<SearchNodeInfo> sorted = Sugar.list(new SearchNodeInfo(null, null
                     , HornClause.loadResults(Clause.parse("location(V0, V1), !complex(V0, V4), !location(V6, V1)"))
                     , 0, true, null));
                     /**/

                    System.out.println("# reasonable HC found\t" + sorted.size());

                    //entailment.allEntailed(theory)
                    // tady udelat rychle pocitani entailmentu, respektive vsech literalu ktere jsou implikovany teorii (nebo en jednim pravidlem???); z toho vzit prvnich n tak, ze suma jejich chyby je mensi nez nejaky threshold
                    // k = a+2

                    //int k = predicate.getS() + 2;
                    int k = 5;

                    int atMostLnonpresentImplied = 100;
                    Set<Literal> implied = Sugar.set();
                    // varianta dokaz co muzes
                    if (true) {
                        System.out.println("imply everything");
                        for (SearchNodeInfo sni : sorted) {
                            System.out.println("computing VE/k for\t" + sni.getRule());
                            System.out.println("\t" + sni.getAccuracy());
                            Set<Clause> t = Sugar.union(hardConstraints, Sugar.union(multiplySymmetries(sni.getRule().toClause(), hardConstraints), sni.getRule().toClause()));

                            //DoubleCounters<Literal> entails = entailment.votingEntailmentOrdered(t, k, true, predicate.getR(), predicate.getS());
                            //DoubleCounters<Literal> entails = entailment.votingEntailmentOrderedReduction(t, k, true, predicate.getR(), predicate.getS());
                            Set<Literal> entails = entailment.kEntailmentOrderedDataDrive(t, k, true, predicate.getR(), predicate.getS());
                            System.out.println("so it entails\t" + entails.size());
                            Set<Literal> union = Sugar.union(implied, entails);
                            if (union.size() > atMostLnonpresentImplied) {
                                break;
                            }
                            implied = union;
                        }

                    }
                    //varianta s neg examplama
                    if (false) {
                        System.out.println("generating negative examples");
                        Random r = new Random();
                        Set<Literal> negativeExamples = generateNegativeLiterals(evidenceClause, predicate, hardConstraints, 20, r);
                        List<HornClause> selected = Sugar.list();

                        double threshold = 100.0d;
                        for (SearchNodeInfo sni : sorted) {
                            System.out.println("computing VE for\t" + sni.getRule());
                            System.out.println("\t" + sni.getAccuracy());
                            Set<Clause> t = Sugar.union(hardConstraints, multiplySymmetries(sni.getRule().toClause(), hardConstraints));
                            //double kentails = entailment.votingEntailment(t,k,negativeExamples);
                            double kentails = 0;
                            for (Literal literal : negativeExamples) {
//                    System.out.println("\tsolving query\t" + literal);
                                //kentails += entailment.votingEntailmentOrderedParallel(t, k, literal);
                                System.out.println("tady to je spatne, gamma, tzn threshold je pro kazdy jeden literal, pokud teda to v poznamkach neni jinak");
                                //kentails += entailment.votingEntailmentOrdered(t, k, literal);
                                System.out.println("kentails += entailment.votingEntailmentOrdered(t, k, literal);");
                                if (threshold - kentails < 0) {
                                    break;
                                }
                            }

                            if (threshold - kentails < 0) {
                                break;
                            }
                            System.out.println("rule added\t" + threshold + "\t" + sni.getRule());
                            selected.add(sni.getRule());
                            threshold = threshold - kentails;
                        }
                    }
                });
    }


    private List<Clause> simplify(List<Clause> clauses) {
        int maxVars = clauses.stream().mapToInt(clause -> clause.variables().size()).max().orElse(0);
        return TheorySimplifier.simplify(clauses, maxVars + 1);
    }


    // what is below is just a copy of MLNDataset by Ondra
    public Set<Literal> generateNegativeLiterals(Clause evidence, Pair<String, Integer> predicate, Collection<Clause> hardRules, int numExamples, Random random) {
        Set<Literal> possibleExamples = new HashSet<>();
        MutableDouble estimatedNegativeCount = new MutableDouble();
        possibleExamples.addAll(possibleExamples(evidence, predicate.r, predicate.s, hardRules, numExamples, random, estimatedNegativeCount));
        Set<Literal> sampleOfnegativeExamples = Sugar.setDifference(possibleExamples, evidence.literals());
        return sampleOfnegativeExamples;
    }

    private List<Literal> possibleExamples(Clause db, String predicateName, int arity, Collection<Clause> hardRules, int desiredNum, Random random, MutableDouble estimatedCount) {
        List<Clause> theory = new ArrayList<>();
        theory.addAll(hardRules);

        TheorySolver ts = new TheorySolver();
        //ts.setSubsumptionMode(Matching.OI_SUBSUMPTION);

        List<Clause> theoryAndEvidence = new ArrayList<>();
        theoryAndEvidence.addAll(theory);
        for (Literal l : db.literals()) {
            theoryAndEvidence.add(new Clause(l));
        }

        Clause fastCheckDB = new Clause(ts.solve(theoryAndEvidence));

        MultiList<Pair<String, Integer>, Constant> typing = Dataset.typing(db);
        int max = 1;
        for (int i = 0; i < arity; i++) {
            max *= typing.get(new Pair<String, Integer>(predicateName, i)).size();
        }
        desiredNum = (int) (0.8 * Math.min(max, desiredNum));

        List<Constant> candidateConstants = typing.get(new Pair<String, Integer>(predicateName, 0));

        Set<Literal> retVal0 = new HashSet<Literal>();
        int maxTries = desiredNum * 5;
        int ok = 0;
        int chunk = 100;
        int tries = 0;
        for (int i = 0; i < maxTries; i++) {
            Set<Literal> newLs = new HashSet<Literal>();
            Set<Literal> additionalDBLits = new HashSet<Literal>();
            for (int j = 0; j < chunk; j++) {
                Literal newL = new Literal(predicateName, arity);
                for (int k = 0; k < arity; k++) {
                    List<Constant> t = typing.get(new Pair<String, Integer>(predicateName, k));
                    newL.set(t.get(random.nextInt(t.size())), k);
                }
                newLs.add(newL);
                Set<Literal> sol = ts.solve(theory, Sugar.set(newL));
                if (sol != null) {
                    additionalDBLits.addAll(sol);
                }
            }

            if (ts.findViolatedRules(theory, Sugar.union(fastCheckDB.literals(), additionalDBLits, newLs)).isEmpty()) {
                retVal0.addAll(newLs);
                ok += newLs.size();
                tries += chunk;
                //System.out.println("ok chunk");
            } else {
                for (Literal newL : newLs) {
                    List<Clause> testTheory = new ArrayList<Clause>();
                    testTheory.addAll(theory);
                    testTheory.add(new Clause(newL));
                    for (Literal dbLit : db.literals()) {
                        testTheory.add(new Clause(dbLit));
                    }
                    if (ts.solve(testTheory) != null) {
                        ok++;
                        retVal0.add(newL);
                    }
                    tries++;
                }
            }
            if (retVal0.size() >= desiredNum) {
                double est = (double) ok / (double) tries;
                for (int j = 0; j < arity; j++) {
                    est *= typing.get(new Pair<String, Integer>(predicateName, j)).size();
                }
                estimatedCount.set(est);
                return Sugar.listFromCollections(retVal0);
            }
            if (tries > 30 && retVal0.size() / (double) tries < 0.2) {
                break;
            }
        }

        List<Literal> current = new ArrayList<Literal>();
        current.add(LogicUtils.newLiteral(predicateName, arity));

        double mult = 1.0;
        for (int i = 0; i < arity; i++) {
            List<Clause> slice = theorySlice(theory, predicateName, arity, 0, i + 1);

            List<Clause> sliceTheory = new ArrayList<Clause>(slice);

            for (Literal l : db.literals()) {
                slice.add(new Clause(l));
            }
            List<Literal> next = new ArrayList<Literal>();
            List<Pair<Literal, Constant>> candidates = new ArrayList<Pair<Literal, Constant>>();

            for (Constant c : i == 0 ? candidateConstants : typing.get(new Pair<String, Integer>(predicateName, i))/*possibleConstants(db, candidateConstants, theory, predicateName, arity, i)*/) {
                for (Literal l : current) {
                    candidates.add(new Pair<Literal, Constant>(l, c));
                }
            }
            Collections.shuffle(candidates, random);
            int k = 0;
            for (Pair<Literal, Constant> pair : candidates) {
                Literal newL = new Literal(predicateName, i + 1);
                for (int j = 0; j < i; j++) {
                    newL.set(pair.r.get(j), j);
                }
                newL.set(pair.s, i);


                if (ts.findViolatedRules(sliceTheory, Sugar.union(fastCheckDB.literals(), newL)).isEmpty()) {
                    next.add(newL);
                    //System.out.println("pass: "+newL);
                } else if (ts.solve(Sugar.union(slice, new Clause(newL))) != null) {
                    next.add(newL);
                }
                if (next.size() >= desiredNum) {
                    mult *= (candidates.size() / (double) desiredNum);
                    break;
                }
                k++;
            }
            current = next;
        }
        estimatedCount.set(current.size() * mult);
        return current;
    }

    private static List<Clause> theorySlice(List<Clause> theory, String predicate, int arity, int start, int end) {
        List<Clause> retVal = new ArrayList<Clause>();
        outerLoop:
        for (Clause c : theory) {
            if (!c.predicates().contains(predicate)) {
                retVal.add(c);
            } else {
                List<Literal> l1 = new ArrayList<Literal>();
                List<Literal> l2 = new ArrayList<Literal>();
                for (Literal l : c.literals()) {
                    if (l.predicate().equals(predicate) && l.arity() == arity) {
                        l1.add(l);
                    } else {
                        l2.add(l);
                    }
                }
                List<Literal> l3 = new ArrayList<Literal>();
                for (Literal l : l1) {
                    for (int i : outerIntervals(start, end, arity)) {
                        int count = 0;
                        for (Literal byTerm : c.getLiteralsByTerm(l.get(i))) {
                            for (int j = 0; j < byTerm.arity(); j++) {
                                if (byTerm.get(j).equals(l.get(i))) {
                                    count++;
                                }
                            }
                            if (count > 1) {
                                continue outerLoop;
                            }
                        }
                    }
                    Literal shorter = new Literal(l.predicate(), l.isNegated(), end - start);
                    int j = 0;
                    for (int i = start; i < end; i++) {
                        shorter.set(l.get(i), j++);
                    }
                    l3.add(shorter);
                }
                retVal.add(new Clause(Sugar.union(l2, l3)));
            }
        }
        return retVal;
    }


    private static int[] outerIntervals(int start, int end, int length) {
        int[] retVal = new int[start + length - end];
        int j = 0;
        for (int i = 0; i < start; i++) {
            retVal[j] = i;
            j++;
        }
        for (int i = end; i < length; i++) {
            retVal[j] = i;
            j++;
        }
        return retVal;
    }


    public Set<Literal> computePossLogKE(Set<Literal> evidence, PossibilisticTheory theory, int offset, boolean verbouse, boolean withoutKEntailment) {
        int originalEv = evidence.size();
        if (true) {
//            System.out.println("saturovani neni koser, musime to explicitne zminit");
            ConjunctureSaturator saturator = ConjunctureSaturator.create(Sugar.listFromCollections(theory.getHardRules()), false, true);
            Clause saturated = saturator.saturate(new Clause(evidence));
            if (null != saturated) {
                evidence = saturated.literals().stream().map(LogicUtils::constantize).collect(Collectors.toSet());
            }
            System.out.println("saturation done");
        } else {
            System.out.println("saturation off");
        }

        if (verbouse) {
            System.out.println("orig\t" + originalEv + "\t" + evidence.size());
            System.out.println("symmetricity extended at the start");
            System.out.println("rules\t" + theory.size() + "\tno extension done probably");
            //theory.forEach(c -> System.out.println("\t" + c));
            System.out.println();
        }

        TestComputing computer = TestComputing.create(evidence, Sugar.set());
        int k = offset + theory.allRules().stream()
                .mapToInt(clause -> clause.literals().stream().filter(l -> !l.isNegated()).mapToInt(Literal::arity).max().orElse(0))
                .max().orElse(0);

        if (verbouse) {
            System.out.println("kEntailmentOrderedDataDriveScoutPL");
        }
//        System.out.println("setting parallel to false for debug in PL!");
        Set<Literal> entails = computer.kEntailmentOrderedDataDriveScoutPL(theory, k, true, null, 0, withoutKEntailment);
        int entailsOnly = entails.size();
        entails.addAll(evidence);
        entails.addAll(evidence);
//        System.out.println("k-entails\t" + entails.size() + "\twith " + evidence.size() + " literals from saturatedBody evidence");
//        System.out.println("definitely entails\n\t" + entails.size() + "\n\t" + entailsOnly + "\n\t" + originalEv + "\n\t" + evidence.size() + "\n\tD" +(entailsOnly == evidence.size()));
//        if(entailsOnly > 0){
//            System.out.println("heureka");
//        }
        if (verbouse) {
            entails.forEach(System.out::println);
        }
        return entails;
    }


    public Set<Literal> computeXE(Set<Literal> evidence, Set<Clause> theory, int offset, int entailmentMode, boolean verbouse, Path output) {
        Pair<Set<Literal>, Set<Clause>> adHocSymmetriciExtension = adhocSymmetricity(evidence, theory);
        System.out.println("sym done");
        int originalEv = evidence.size();
        evidence = adHocSymmetriciExtension.r;
        theory = adHocSymmetriciExtension.s;

        if (verbouse) {
            System.out.println("orig\t" + originalEv + "\t" + evidence.size());
            System.out.println("symmetricity extended at the start");
            System.out.println("rules\t" + theory.size());
            //theory.forEach(c -> System.out.println("\t" + c));
            System.out.println();
        }

        TestComputing computer = TestComputing.create(evidence, Sugar.set());
        Clause evidenceClause = new Clause(evidence);
        int k = offset + theory.stream()
                .mapToInt(clause -> clause.literals().stream().filter(l -> !l.isNegated()).mapToInt(Literal::arity).max().orElse(0))
                .max().orElse(0);
        if (1 == entailmentMode) {
            if (verbouse) {
                System.out.println("votingEntailmentOrderedCuttingPlainDataDriven");
            }
            DoubleCounters<Literal> entails = computer.votingEntailmentOrderedDataDrive(theory, k, true, null, 0, false);
            System.out.println("v-entails\t" + entails.keySet().size());
            if (verbouse) {
                entails.keySet().forEach(key -> System.out.println(key + "\t" + entails.get(key) + "\t" + evidenceClause.literals().contains(key)));
            }
            System.out.println("storing to\t" + output);
            try {
                evidenceClause.literals().forEach(lit -> entails.add(lit, -1));
                Files.write(output, entails.keySet().stream().map(lit -> {
                    boolean inEvidence = evidenceClause.literals().contains(lit);
                    return inEvidence + "\t" + (inEvidence ? "-1" : entails.get(lit)) + "\t" + lit;
                }).collect(Collectors.toList()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            throw new IllegalStateException("tady by se mely ty hodnoty jeste podelit podle toho jeslti je v dokazanem predikatu dve stejne konstanty nebo jenom jedna");
            //return null; // comming undonde, tady by melo byt oprahovani pres gammu (ktera jeste neni)
        } else if (2 == entailmentMode) {
            if (verbouse) {
                System.out.println("kEntailmentOrderedCuttingPlainDataDriven");
            }
            Set<Literal> entails = computer.kEntailmentOrderedDataDrive(theory, k, true, null, 0);
            entails.addAll(evidence);
            System.out.println("k-entails\t" + entails.size());
            if (verbouse) {
                entails.forEach(System.out::println);
            }

//            Set<Literal> finalEvidence = evidence;
//            entails.stream().filter(l -> !finalEvidence.contains(l)).forEach(System.out::println);

            return entails;

        } else if (3 == entailmentMode) {
            if (verbouse) {
                System.out.println("kEntailmentOrderedCuttingPlainDataDrivenWithPlainePrun");
            }

            Set<Literal> entails = computer.kEntailmentOrderedDataDriveScout(theory, k, false, null, 0);
            entails.addAll(evidence);
            System.out.println("k-entails\t" + entails.size());
            if (verbouse) {
                entails.forEach(System.out::println);
            }
            return entails;
        }
        return null;
    }

    private Pair<Set<Clause>, Set<Pair<String, Integer>>> findSymmetries(Collection<Clause> theory) {
        // add
        if (theory.stream().mapToInt(clause -> LogicUtils.positiveLiterals(clause).size()).anyMatch(size -> size > 1)) {
            // rules can have at most one positive literal
//            theory.stream().forEach(c -> {
//                if(LogicUtils.positiveLiterals(c).size() > 1){
//                    System.out.println(c);
//                }
//            });
            throw new IllegalStateException();// NotImplementedException();
        }
        Set<Clause> symmetric = theory.stream()
                .filter(clause -> {
                    if (clause.countLiterals() != 2) {
                        return false;
                    }
                    Set<Literal> posLits = LogicUtils.positiveLiterals(clause);
                    if (posLits.isEmpty()) {
                        return false;
                    }
                    Literal lit = Sugar.chooseOne(posLits);
                    List<Term> arguments = lit.argumentsStream().collect(Collectors.toList());
                    Collections.reverse(arguments);
                    return clause.literals().contains(new Literal(lit.predicate(), true, arguments));
                })
                .collect(Collectors.toSet());

        Set<Pair<String, Integer>> symmetricPredicates = symmetric.stream()
                .map(clause -> Sugar.chooseOne(LogicUtils.positiveLiterals(clause)).getPredicate())
                .collect(Collectors.toSet());
        return new Pair<>(symmetric, symmetricPredicates);
    }

    private Set<Clause> multiplySymmetries(Clause clause, Collection<Clause> hardConstraints) {
        Pair<Set<Clause>, Set<Pair<String, Integer>>> pair = findSymmetries(hardConstraints);
        Set<Pair<String, Integer>> symmetricPredicates = pair.getS();
        Set<Clause> symmetric = pair.getR();
        // symmetric rules are given here so that no other literal is entailed by the theory
        return saturateHornHeads(symmetricPredicates, Sugar.set(clause), symmetric);
    }


    private Pair<Set<Literal>, Set<Clause>> adhocSymmetricity(Set<Literal> evidence, Set<Clause> theory) {
        Pair<Set<Clause>, Set<Pair<String, Integer>>> pair = findSymmetries(theory);
        Set<Pair<String, Integer>> symmetricPredicates = pair.getS();
        Set<Clause> symmetric = pair.getR();

        // evidence saturation
        Set<Literal> saturated = saturateEvidence(evidence, theory, symmetricPredicates);

        //rule multiplication
        Set<Clause> nonSymetricRules = Sugar.setDifference(theory, symmetric);
        // symmetric rules are given here so that no other literal is entailed by the theory
        Set<Clause> extendedRules = saturateHornHeads(symmetricPredicates, nonSymetricRules, symmetric);

        nonSymetricRules.addAll(extendedRules);
        return new Pair<>(saturated, nonSymetricRules);
    }

    private Set<Clause> saturateHornHeads(Set<Pair<String, Integer>> symmetricPredicates, Set<Clause> rules, Collection<Clause> theory) {
        ConjunctureSaturator saturator = ConjunctureSaturator.create(Sugar.listFromCollections(theory), false, true);
        return rules.stream()
                .filter(clause -> {
                    Set<Literal> headPart = LogicUtils.positiveLiterals(clause);
                    if (headPart.size() != 1) {
                        return false;
                    }
                    return symmetricPredicates.contains(Sugar.chooseOne(headPart).getPredicate());
                })
                .flatMap(clause -> {
                    Set<Literal> posLit = LogicUtils.positiveLiterals(clause);
                    Literal lit = Sugar.chooseOne(posLit);
                    Clause conjuction = new Clause(posLit);
                    Clause saturatedHead = saturator.saturate(conjuction);
                    Set<Literal> body = LogicUtils.negativeLiterals(clause);
                    //saturatedHead.literals().forEach(c -> System.out.println("saturatedBody\t" + c));
                    return saturatedHead.literals().stream()
                            .filter(literal -> literal.getPredicate().equals(lit.predicate())) // just a check that only head of symmetricity is added.... to avoid problem when putting more theory than just symmetric rules
                            .map(head -> new Clause(Sugar.union(body, head)));
                })
                .collect(Collectors.toSet());
    }

    {
        System.out.println("saturate evidence has a hack inside to overcome time consumption !!!!! only interaction symmetricity");
    }


    private Set<Literal> saturateEvidence(Set<Literal> evidence, Set<Clause> theory, Set<Pair<String, Integer>> symmetricPredicates) {
        Set<Literal> saturated = Sugar.setFromCollections(evidence);

        if (true) {
            // interaction only
            return Sugar.union(saturated,
                    saturated.stream()
                            .filter(l -> l.getPredicate().r.equals("interaction"))
                            .map(lit -> {
                                List<Term> arguments = lit.argumentsStream().collect(Collectors.toList());
                                Collections.reverse(arguments);
                                return new Literal(lit.predicate(), false, arguments);
                            })
                            .collect(Collectors.toList()));

        } else {
            List<Clause> merged = Sugar.listFromCollections(Sugar.union(evidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()), theory));
            TheorySolver ts = new TheorySolver();
            ts.setSubsumptionMode(Matching.THETA_SUBSUMPTION);
            ts.setMode(TheorySolver.CUTTING_PLANES);
            Set<Literal> solved = ts.solve(merged);
            if (null != solved) {
                Set<Literal> implied = solved.stream()
                        .parallel()
                        .filter(literal -> !literal.isNegated() && symmetricPredicates.contains(literal.getPredicate())
                                        && true
                                // hack here for speed !!!!!!!!!
                                //&& TheorySimplifier.isGroundLiteralImplied(literal, merged, Matching.THETA_SUBSUMPTION, TheorySolver.CUTTING_PLANES)
                        )
                        .collect(Collectors.toSet());
                saturated.addAll(implied);
            }
            return saturated;
        }
    }
}
