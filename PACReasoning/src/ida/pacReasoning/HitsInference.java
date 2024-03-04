package ida.pacReasoning;

import ida.ilp.logic.*;
import ida.pacReasoning.entailment.Entailment;
import ida.pacReasoning.entailment.EntailmentSetting;
import ida.pacReasoning.entailment.SubsetFactory;
import ida.pacReasoning.entailment.collectors.Entailed;
import ida.pacReasoning.entailment.collectors.KECollector;
import ida.pacReasoning.entailment.theories.FOL;
import ida.pacReasoning.evaluation.Utils;
import ida.pacReasoning.supportEntailment.SupportEntailmentInference;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 24. 9. 2018.
 */
public class HitsInference {

    private final Map<Pair<Predicate, Integer>, String> typing;

    {
        System.out.println("remizi resit nahodne");
    }

    public static int SUPPORT_MODE = 1;
    public static int SEARCH_MODE = 2;

    private int mode = SUPPORT_MODE;

    public HitsInference(Map<Pair<Predicate, Integer>, String> typing) {
        this.typing = typing;
    }

    public Path infer(int k, Path rulesPath, Set<Literal> evidence, Path testLiterals, List<Clause> rules, Set<Clause> constraints) {
        if (null != typing) {
            evidence = LogicUtils.addTyping(evidence, typing);
        }


        Path output = Paths.get(rulesPath.toString() + "-k" + k + "_c" + constraints.size());
        System.out.println("storing to\t" + output);

        if (!output.toFile().exists()) {
            output.toFile().mkdirs();
        }

        System.setProperty("ida.pacReasoning.entailment.k", "" + k);
        System.setProperty("ida.pacReasoning.entailment.mode", "k");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.saturation", "none");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
        System.setProperty("ida.pacReasoning.entailment.logic", "classical");
        EntailmentSetting setting = EntailmentSetting.create();

        Utils u = Utils.create();
        Map<Literal, Set<Literal>> queries = u.loadHitsTest(testLiterals);


        Integer last = findLastState(output);
        if (null == last) {
            last = rules.size();
        }
        //last = rules.size();
//        System.out.println("debug changing last to previous!\t" + last);


        System.out.println("running first inference with rules up to\t" + last);

        System.out.println("running with inference pruning");
//        Entailment entailment = Entailment.createHitsInterference(evidence, setting, flatten(queries));
//        System.out.println("running with classical entailment without interference prunings");
//        Entailment entailment = Entailment.get(evidence, setting);

        FOL theory = FOL.create(constraints, Sugar.setFromCollections(rules.subList(0, last)));

        Set<Literal> entailedLiterals;
        if (SEARCH_MODE == this.mode) {
            Entailment entailment = Entailment.createHitsInterference(evidence, setting, flatten(queries));
            entailedLiterals = ((KECollector) entailment.entails(theory, k, true, null, null)).getEntailed();
        } else if (SUPPORT_MODE == this.mode) {
            SupportEntailmentInference support = SupportEntailmentInference.create(evidence);
            entailedLiterals = ((KECollector) support.entails(theory, k)).getEntailed();
        } else {
            throw new IllegalStateException();// NotImplementedException();
        }


        store(rules.size(), entailedLiterals, output);
        saveDone(rules.size(), output);

        Pair<Map<Literal, Set<Literal>>, Map<Literal, Set<Literal>>> pair = upperPartAndLowerPartQueries(queries, entailedLiterals);
        queries = pair.s;

        // kde se budou davat dokazany?
        compute(0, last, evidence, queries, rules, constraints, output, k, setting);

        // clear memory hopefully :)
        SubsetFactory.getInstance().clear();
        return output;
    }

    private void compute(int minInclusive, int maxExclusive, Set<Literal> evidence, Map<Literal, Set<Literal>> queries, List<Clause> rules, Set<Clause> constraints, Path output, int k, EntailmentSetting setting) {
        Set<Literal> test = flatten(queries);
        System.out.println("\ncomputing " + minInclusive + "\t" + maxExclusive + "\twith\t" + queries.size() + "\t" + flatten(queries).size() + "\n");

        boolean allContainOnlyOneQuery = queries.values().stream().allMatch(s -> s.size() <= 1);

        if (test.isEmpty() || allContainOnlyOneQuery) {
            System.out.println("test is empty\t" + allContainOnlyOneQuery);
            //System.out.println("inclusive or exclusive in the save state computation?");
            store(maxExclusive, queries, output);
            saveDone(maxExclusive, output);
            if (0 == minInclusive) { // to end earlier
                saveDone(0, output);
            }
            return;
        }
        if (maxExclusive - minInclusive <= 1) {
            System.out.println("the bounds are too tight");
            //System.out.println("inclusive or exclusive in the save state computation?");
            saveDone(maxExclusive, output);
            store(maxExclusive, queries, output);
            return;
        }

        System.out.println("running with inference pruning");

        //System.out.println("running with classical logic without inference");
//        Entailment entailment = Entailment.get(evidence, setting);

        int half = (minInclusive + maxExclusive) / 2;
        FOL theory = FOL.create(constraints, Sugar.setFromCollections(rules.subList(0, half)));

        Entailed entailed;
        if (SEARCH_MODE == this.mode) {
            Entailment entailment = Entailment.createHitsInterference(evidence, setting, flatten(queries));
            entailed = entailment.entails(theory, k, true, null, null);
        } else if (SUPPORT_MODE == this.mode) {
            SupportEntailmentInference support = SupportEntailmentInference.create(evidence);
            entailed = support.entails(theory, k);
        } else {
            throw new IllegalStateException();// NotImplementedException();
        }
        Set<Literal> entailedLiterals = ((KECollector) entailed).getEntailed();

        Pair<Map<Literal, Set<Literal>>, Map<Literal, Set<Literal>>> pair = upperPartAndLowerPartQueries(queries, entailedLiterals);

        Map<Literal, Set<Literal>> upperPart = pair.r;
        Map<Literal, Set<Literal>> lowerPart = pair.s;

        compute(half, maxExclusive, evidence, upperPart, rules, constraints, output, k, setting);

        store(half, entailedLiterals, output);
        saveDone(half, output);
        compute(minInclusive, half, evidence, lowerPart, rules, constraints, output, k, setting);
    }

    // can this be done nicer somehow? e.g. reusing the same datastructure in one run
    private Pair<Map<Literal, Set<Literal>>, Map<Literal, Set<Literal>>> upperPartAndLowerPartQueries
    (Map<Literal, Set<Literal>> queries, Set<Literal> entailedLiteralsByHalf) {
        Map<Literal, Set<Literal>> diff = new HashMap<>();
        Map<Literal, Set<Literal>> filteredForBelowPart = new HashMap<>();

        for (Map.Entry<Literal, Set<Literal>> entry : queries.entrySet()) {
            Literal query = entry.getKey();
            Set<Literal> testingInLowerPart = Sugar.set();
            Set<Literal> testingInUpperPart = Sugar.set();

            for (Literal querySample : entry.getValue()) {
                if (entailedLiteralsByHalf.contains(querySample)) {
                    testingInLowerPart.add(querySample);
                } else {
                    testingInUpperPart.add(querySample);
                }
            }

            diff.put(query, testingInUpperPart);

            // better readability
            if (!testingInLowerPart.contains(query) && testingInLowerPart.size() > 0) {
                // the query was not entailed by the selected rules but some of its corruption was, therefore remove all the corrupted queries because their are redundant (we are not interested in ordering between them)
                // do nothing
            } else {
                if (testingInLowerPart.size() == 1) {
                    // can skip, since we are interested in ordering only
                } else if (testingInLowerPart.size() > 0) {
                    filteredForBelowPart.put(query, testingInLowerPart);
                }
            }
        }

        return new Pair<>(diff, filteredForBelowPart);
    }

    private void store(int idx, Map<Literal, Set<Literal>> queries, Path output) {
        store(idx, flatten(queries), output);
    }

    private void store(int idx, Collection<Literal> queries, Path output) {
        //System.out.println("storing to\t" + idx);
        try {
            Path path = Paths.get(output.toString(), idx + ".entailed");
            Set<Literal> lits = Sugar.set();
            if (path.toFile().exists()) {
                lits.addAll(LogicUtils.loadEvidence(path));
            }
            lits.addAll(queries);
            Files.write(path, lits.stream().map(Literal::toString).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveDone(int idx, Path output) {
        try {
            Files.write(Paths.get(output.toString(), "state"), Sugar.list("" + idx));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<Literal> flatten(Map<Literal, Set<Literal>> queries) {
        return Sugar.parallelStream(queries.values()).flatMap(Collection::stream).collect(Collectors.toSet());
    }


    private Integer findLastState(Path dir) {
        try {
            List<Path> states = Files.list(dir).filter(p -> p.toFile().getName().equals("state")).collect(Collectors.toList());
            if (states.isEmpty()) {
                return null;
            }
            if (states.size() > 1) {
                throw new IllegalStateException("more states files than expected!");
            }
            return Integer.parseInt(Files.lines(states.get(0)).limit(1).collect(Collectors.toList()).get(0));
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    public void bruteforce(int k, Path rulesPath, Set<Literal> evidence, Path testLiterals, List<Clause> rules, Set<Clause> constraints) {
        Path output = Paths.get(rulesPath.toString() + "-k" + k + "_c" + constraints.size() + "_bruteforce");
        System.out.println("storing to\t" + output);

        if (!output.toFile().exists()) {
            output.toFile().mkdirs();
        }

        System.setProperty("ida.pacReasoning.entailment.k", "" + k);
        System.setProperty("ida.pacReasoning.entailment.mode", "k");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.saturation", "none");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
        System.setProperty("ida.pacReasoning.entailment.logic", "classical");
        EntailmentSetting setting = EntailmentSetting.create();

        Utils u = Utils.create();
        Map<Literal, Set<Literal>> queries = u.loadHitsTest(testLiterals);

        for (int idx = 1; idx <= rules.size(); idx++) {
            System.out.println("cut\t" + idx);

            Entailment entailment = Entailment.create(evidence, setting);
            FOL theory = FOL.create(constraints, Sugar.setFromCollections(rules.subList(0, idx)));
            Entailed entailed = entailment.entails(theory, k, true, null, null);

            store(idx, ((KECollector) entailed).getEntailed(), output);

        }

        // clear memory hopefully :)
        SubsetFactory.getInstance().clear();
    }
}
