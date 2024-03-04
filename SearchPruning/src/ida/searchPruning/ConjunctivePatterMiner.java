package ida.searchPruning;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.searchPruning.evaluation.BreadthResults;
import ida.searchPruning.evaluation.LevelWiseStats;
import ida.searchPruning.search.collections.SearchNodeInfo;
import logicStuff.Reformatter;
import logicStuff.learning.languageBias.NoneBias;
import logicStuff.learning.languageBias.LanguageBias;
import ida.searchPruning.search.strategy.BreadthFirstSearch;
import ida.searchPruning.search.strategy.BreadthSearchable;
import ida.searchPruning.util.Utils;
import ida.utils.Combinatorics;
import ida.utils.MutableDouble;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.utils.collections.MultiList;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import logicStuff.Typing;
import logicStuff.learning.constraints.shortConstraintLearner.ConstraintLearnerProperties;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.learning.datasets.Yeast;
import logicStuff.learning.saturation.SaturatorProvider;
import logicStuff.learning.saturation.StatefullSaturator;
import logicStuff.theories.TheorySimplifier;
import logicStuff.theories.TheorySolver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 19. 12. 2017.
 */
public class ConjunctivePatterMiner {


    public static ConjunctivePatterMiner create() {
        return new ConjunctivePatterMiner();
    }

    public static void main(String[] arg) throws IOException, InterruptedException {
        if (System.getProperty("ida.searchPruning.preprocessInput", "").equals("farmer")) {
            System.out.println("[starting farmer output preprocess]");
            Reformatter reformatter = Reformatter.create();
            reformatter.preprocessFarmerAtMost(System.getProperty("ida.searchPruning.input"),
                    Integer.parseInt(System.getProperty("ida.searchPruning.preprocess.maxLiterals")));
            System.out.println("[exiting, no other commands will be processed]");
            return;
        }

        printSetting();


        if (!System.getProperty("os.name").toLowerCase().contains("win")
                && System.getProperty("ida.grid", "on").toLowerCase().equals("on")) {
            System.out.println("probably running on the grid, thus 2 minutes no rush\t" + System.getProperty("os.name"));
            Thread.sleep(2 * 60 * 1000);
        }
        ConjunctivePatterMiner miner = ConjunctivePatterMiner.create();
        LanguageBias bias = new NoneBias();//DummyBias.create(10, 0, 3, 6, Sugar.set());
        String dataPath = System.getProperty("ida.searchPruning.input", null);
        if (null == dataPath) {
            System.out.println("ConjunctivePatterMiner: no input file, cannot continue.");
            return;
        }

        int maxDepth = Integer.parseInt(System.getProperty("ida.searchPruning.maxDepth", "3"));
        int maxVariables = Integer.parseInt(System.getProperty("ida.searchPruning.maxVariables", "" + (2 * maxDepth)));
        int minsupp = Integer.parseInt(System.getProperty("ida.searchPruning.minSupport", "1"));

        int beamSize = Integer.MAX_VALUE;
        if (System.getProperty("ida.searchPruning.mining", "").startsWith("bfs-sampling")) {
            String value = System.getProperty("ida.searchPruning.mining");
            beamSize = Integer.parseInt(value.substring("bfs-sampling-".length()));
        } else if (System.getProperty("ida.searchPruning.mining", "").equals("theory")) {
            System.out.println("start mining");
            miner.mineTheory(dataPath);
            return;
        } else if (!System.getProperty("ida.searchPruning.mining", "").equals("bfs")) {
            System.out.println("ConjunctivePatterMiner: no mining, or unknown mining setting:\t" + System.getProperty("ida.searchPruning.mining"));
            return;
        }
        System.out.println("start mining");
        miner.mineWithBreadthStrategy(dataPath, bias, maxDepth, maxVariables, minsupp, beamSize);
    }

    public static void printSetting() {
        System.out.println("ConjunctivePatterMiner starting");

        Map<String, String> properties = new HashMap<>();
        System.getProperties().forEach((property, value) -> {
            if (property.toString().startsWith("ida.")) {
                properties.put(property.toString(), value.toString());
            }
        });
        properties.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey))
                .forEach(e -> System.out.println(e.getKey() + "\t" + e.getValue()));
    }


    private void mineTheory(String dataPath) throws IOException {
        LanguageBias bias = System.getProperty("ida.searchPruning.cpm.theoryBias", "").toLowerCase().equals("pddl") ? new LanguageBias() {
            @Override
            public Predicate<HornClause> predicate() {
                return (hornClause) -> {
                    if (hornClause.body().literals().stream().allMatch(literal -> literal.predicate() == "succ" && literal.arity() == 2)) {
                        return true;
                    }
                    Set<Term> times = hornClause.body().literals().stream().map(l -> l.argumentsStream().reduce((first, second) -> second).orElse(null)).collect(Collectors.toSet());
                    if (times.size() > 2) {
                        return false;
                    } else if (times.size() < 2) {
                        return true;
                    }
                    if (hornClause.body().literals().stream().filter(l -> l.arity() == 2 && l.predicate() == "succ").count() > 1) {
                        return false;
                    }
                    List<Term> timesList = Sugar.listFromCollections(times);
                    if (hornClause.body().literals().contains(new Literal("succ", timesList.get(0), timesList.get(1)))
                            || hornClause.body().literals().contains(new Literal("succ", true, timesList.get(0), timesList.get(1)))
                            || hornClause.body().literals().contains(new Literal("succ", timesList.get(1), timesList.get(0)))
                            || hornClause.body().literals().contains(new Literal("succ", true, timesList.get(1), timesList.get(0)))) {
                        return true;
                    }
                    return false;
                };
            }
        } : NoneBias.create();
        if (System.getProperty("ida.searchPruning.cpm.theoryBias", "").toLowerCase().equals("pddl")) {
            System.out.println("inserting PDDL bias: only succ/2 literals, at most 2 time variables, at most 1 succ literal");
        }

        TimeDog overallTime = new TimeDog(Integer.parseInt(System.getProperty("ida.searchPruning.runner.overallLimit", "" + (20 * 60))) * 60 * 1000_000_000l, false);
        MEDataset med = loadDataset(dataPath);
        Pair<SaturatorProvider<HornClause, StatefullSaturator<HornClause>>, Pair<List<Clause>, List<Clause>>> box = ConstraintLearnerProperties.create().getProviderAndConstraints(med, overallTime, bias);
        Pair<List<Clause>, List<Clause>> constraints = box.s;
        System.out.println("hard constraints");
        constraints.r.forEach(c -> System.out.println("\t" + c));
        System.out.println("minsupp constraints");
        constraints.s.forEach(c -> System.out.println("\t" + c));

        // a quick workaround for end-to-end running
        String storeTarget = System.getProperty("ida.searchPruning.storeTo");
        if (null != storeTarget) {
            System.out.println("storing hard constraints to\t" + storeTarget);
            File parent = new File(storeTarget).getParentFile();
            if(!parent.exists()){
                parent.mkdirs();
            }
            Files.write(Paths.get(storeTarget), constraints.r.stream().map(Clause::toString).collect(Collectors.toList()));
        }


        List<Literal> db = Sugar.list();
        med.getExamples().forEach(example -> db.addAll(example.literals()));
        Typing typing = Typing.create(new Clause(db));

        System.out.println("original length\t" + constraints.r.size() + " : " + constraints.s.size());
        List<Clause> hard = typing.filterTypeContradictions(constraints.r);
        List<Clause> minsupp = typing.filterTypeContradictions(constraints.s);
        System.out.println("after filter length\t" + hard.size() + " : " + minsupp.size());
        System.out.println("hardf constraints");
        hard.forEach(c -> System.out.println("\t" + c));
        System.out.println("minsuppf constraints");
        minsupp.forEach(c -> System.out.println("\t" + c));

        if (Boolean.parseBoolean(System.getProperty("ida.searchPruning.simplifyTheory", "false"))) {
            System.out.println("\n\nsimplifying\n");
            List<Clause> simplified = TheorySimplifier.simplify(hard, 1 + hard.stream().map(LogicUtils::variables).mapToInt(Set::size).max().orElse(0));
            System.out.println("simplified:\t" + simplified.size());
            simplified.forEach(System.out::println);
        }
    }

    // simple check whether p(...,t_i) has t_i in another arguments, not only in the last one and returns false if it has
//    private boolean isPDDLTypeTimeCorrect(Clause clause){
//        return clause.literals().stream()
//                .filter(literal -> literal.predicate() != "succ")
//                .allMatch(literal -> {
//                    List<Term> arguments = literal.argumentsStream().collect(Collectors.toList());
//                    Term time = arguments.get(arguments.size() - 1);
//                    return !IntStream.range(0,arguments.size()-1).anyMatch(idx -> arguments.get(idx) == time);
//                });
//    }

    private BreadthSearchable breadthFirstSearch(DatasetInterface med, int maxLiterals, int maxVariables, int maxDepth, TimeDog time, int minFrequency, LanguageBias bias, ConstraintLearnerProperties clp, int beamSize) {
        return (learnFrom) -> {
            long constraintStart = System.nanoTime();
            SaturatorProvider<HornClause, StatefullSaturator<HornClause>> saturatorProvider = ConstraintLearnerProperties.create().getProvider(med, time);
            long constraintEnd = System.nanoTime();

            long searchStart = System.nanoTime();
            BreadthFirstSearch search = new BreadthFirstSearch(med, saturatorProvider, time, minFrequency, constraintEnd - constraintStart, beamSize);
            BreadthResults result = search.search(maxDepth, searchStart, bias);
            long searchEnd = System.nanoTime();

            System.out.println("end of mining");
            return result;
        };
    }

    public List<Clause> mineWithBreadthStrategy(String dataPath, LanguageBias bias, int maxDepth, int maxVariables, int minSupport, int beamSize) throws IOException {
        ConstraintLearnerProperties clp = ConstraintLearnerProperties.create();
        String mln = System.getProperty("ida.searchPruning.targetPredicate", "").replace('/', '-').replace('_', '-');
        String mode = System.getProperty("ida.logicStuff.mode", "");
        List<String> properties = Sugar.list("output",
                System.getProperty("ida.searchPruning.mining")
                        + ("" != mln && Boolean.parseBoolean(System.getProperty("ida.searchPruning.mlnInput", "false")) ? "-" + mln : "")
                        + (("" != mode ? "-" : "") + mode),
                System.getProperty("ida.searchPruning.minSupport"),
                System.getProperty("ida.searchPruning.datasetSubsumption"),
                "" + maxDepth, "" + maxVariables, "" + minSupport);
        properties.addAll(clp.propertiesToList());

        boolean storeNone = System.getProperty("ida.searchPruning.storeOutput", "").toLowerCase().equals("none");

        File outputDir = storeNone ? null : Utils.createFolder(Sugar.path("_", dataPath, String.join("_", properties)));

        MEDataset med = loadDataset(dataPath);

        TimeDog overallTime = new TimeDog(Integer.parseInt(System.getProperty("ida.searchPruning.runner.overallLimit", "" + (20 * 60))) * 60 * 1000_000_000l, false);
        BreadthSearchable breadth = breadthFirstSearch(med, maxDepth, maxVariables, maxDepth, overallTime, minSupport, bias, clp, beamSize);

        BreadthResults result = breadth.searchTheory(IntStream.range(0, med.getExamples().size()).mapToObj(i -> i).collect(Collectors.toSet()));
        Set<SearchNodeInfo> allNodes = Sugar.set();
        for (int depth = 0; depth < result.depths(); depth++) {
            Set<SearchNodeInfo> features = result.getRules(depth);
            allNodes.addAll(features);

            LevelWiseStats stats = new LevelWiseStats(med, depth);
            stats.incorporate(result.getDepth(depth),
                    result.getRules(depth),
                    Sugar.set(),
                    Sugar.set(),
                    Sugar.set());
            // stats
            List<String> output = Sugar.list("value\nfolds: " + (stats.fullyComputed()),
                    "depth : " + (stats.getDepth()),
                    "constraint time [ms]: " + (stats.constraintTime() / 1000_000),
                    "search time [ms]: " + (stats.searchTime() / 1000_000),
                    "overall time [ms]: " + (stats.overallTime() / 1000_000),
                    "avg hypotheses length: " + (stats.avgHypothesesLength()),
                    "deviance hypotheses length: " + (stats.devianceHypothesesLength()),
                    "searched nodes: " + (stats.searchedNodes()),
                    "pruned nodes: " + (stats.avgPrunedNodes()),
                    "# hypotheses: " + (stats.nonReducedHypotheses()),
                    "# reduced: " + (stats.numberOfReduced()),
                    "avg killed: " + (stats.avgKilled()),
                    "avg extended: " + (stats.avgExtended()),
                    "avg hardRules: " + (stats.avgHardRules()),
                    "avg minSuppRules: " + (stats.avgMinSuppRules()));
            System.out.println(String.join("\n", output));
            if (!storeNone) {
                Utils.writeToFile(outputDir.getAbsolutePath() + "_" + depth + File.separator + "stats.txt", String.join("\n", output));
            }
            if (System.getProperty("ida.searchPruning.storeOutput").contains("features")) {
                String out = outputDir.getAbsolutePath() + File.separator + depth + ".hypotheses";
                Files.write(Paths.get(out),
                        features.stream().map(horn -> horn.getRule().toClause().toString()).collect(Collectors.toList()), Charset.forName("UTF-8"));
                System.out.println("look to folder" + out);
            }
        }

        return allNodes.stream().map(sni -> sni.getRule().toClause()).collect(Collectors.toList());
    }


    /*******

     all what is below is taken from MLNDataset by Ondra, just a quick recopy

     ******/


    private MEDataset loadDataset(String dataPath) throws IOException {
        int subsumptionType = (System.getProperty("ida.searchPruning.datasetSubsumption", "").toLowerCase().equals("oi"))
                ? Matching.OI_SUBSUMPTION : Matching.THETA_SUBSUMPTION;
        MEDataset med;
        if (Boolean.parseBoolean(System.getProperty("ida.searchPruning.mlnInput"))) {
            Clause clause = Yeast.yeastParseFromFile(dataPath);

            String query = null;
            int arity = 0;
            try {
                String[] splitted = System.getProperty("ida.searchPruning.targetPredicate").split("/");
                query = splitted[0];
                arity = Integer.parseInt(splitted[1]);
            } catch (Exception e) {
                System.err.println("Cannot parse -Dida.searchPruning.targetPredicate: " + System.getProperty("ida.searchPruning.targetPredicate"));
                e.printStackTrace();
            }

            int examples = 500;
            med = createDataset(clause, query, arity, examples, Sugar.set(), new Random(1), subsumptionType);
        }else if(Boolean.parseBoolean(System.getProperty("ida.searchPruning.oneEvidence"))){
            med = MEDataset.createOnePositiveExample(dataPath, subsumptionType);
        } else {
            med = MEDataset.create(dataPath, subsumptionType);
        }
        return med;
    }

    public static MEDataset createDataset(Clause example, String queryPredicate, int queryArity, int numExamples, Collection<Clause> hardRules, Random random, int subsumptionType) {
        List<Literal> positiveCandidates = Sugar.list();
        for (Literal l : example.getLiteralsByPredicate(queryPredicate)) {
            if (l.arity() == queryArity) {
                positiveCandidates.add(l);
            }
        }
        //positive examples
        List<Literal> positiveExamples = Combinatorics.randomCombination(positiveCandidates, Math.min(positiveCandidates.size(), numExamples)).toList();
        //negative examples
        Set<Literal> possibleExamples = new HashSet<>();
        MutableDouble estimatedNegativeCount = new MutableDouble();
        possibleExamples.addAll(possibleExamples(example, queryPredicate, queryArity, hardRules, numExamples, random, estimatedNegativeCount));
        Set<Literal> negativeExamples = Sugar.setDifference(possibleExamples, example.literals());
        if (estimatedNegativeCount != null) {
            estimatedNegativeCount.set(estimatedNegativeCount.value() * negativeExamples.size() / (double) possibleExamples.size());
        }
        MEDataset retVal = new MEDataset(Sugar.list(example),
                Sugar.listFromCollections(positiveExamples, negativeExamples),
                IntStream.range(0, positiveExamples.size() + negativeExamples.size())
                        .mapToObj(idx -> (idx < positiveExamples.size()) ? Double.valueOf(1.0) : Double.valueOf(0.0)).collect(Collectors.toList()),
                subsumptionType);


        /*
        retVal.negativeWeight = estimatedNegativeCount.value() / (double) sampleOfnegativeExamples.size();
        retVal.positiveWeight = positiveCandidates.size() / (double) sampleOfPositiveExamples.size();
        retVal.numPositiveExamples = sampleOfPositiveExamples.size();
        retVal.numNegativeExamples = sampleOfnegativeExamples.size();
        */

        return retVal;
    }

    private static List<Literal> possibleExamples(Clause db, String predicateName, int arity, Collection<Clause> hardRules, int desiredNum, Random random, MutableDouble estimatedCount) {

        List<Clause> theory = new ArrayList<>();
        theory.addAll(hardRules);

        TheorySolver ts = new TheorySolver();
        //ts.setSubsumptionMode(Matching.OI_SUBSUMPTION);

        List<Clause> theoryAndEvidence = new ArrayList<Clause>();
        theoryAndEvidence.addAll(theory);
        for (Literal l : db.literals()) {
            theoryAndEvidence.add(new Clause(l));
        }

        Clause fastCheckDB = new Clause(ts.solve(theoryAndEvidence));

        MultiList<Pair<String, Integer>, Constant> typing = typing(db);
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

    public static MultiList<Pair<String, Integer>, Constant> typing(Clause db) {
        MultiMap<Pair<String, Integer>, Constant> mm = new MultiMap<Pair<String, Integer>, Constant>();
        for (Literal l : db.literals()) {
            for (int i = 0; i < l.arity(); i++) {
                mm.put(new Pair<String, Integer>(l.predicate(), i), (Constant) l.get(i));
            }
        }
        MultiMap<Pair<String, Integer>, Constant> retVal = new MultiMap<Pair<String, Integer>, Constant>();
        for (Map.Entry<Pair<String, Integer>, Set<Constant>> entry1 : mm.entrySet()) {
            for (Map.Entry<Pair<String, Integer>, Set<Constant>> entry2 : mm.entrySet()) {
                if (!Sugar.intersection(entry1.getValue(), entry2.getValue()).isEmpty()) {
                    retVal.putAll(entry1.getKey(), entry1.getValue());
                    retVal.putAll(entry1.getKey(), entry2.getValue());
                    retVal.putAll(entry2.getKey(), entry1.getValue());
                    retVal.putAll(entry2.getKey(), entry2.getValue());
                }
            }
        }
        return retVal.toMultiList();
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


}
