package ida.searchPruning;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.subsumption.Matching;
import ida.searchPruning.evaluation.*;
import ida.searchPruning.search.criterion.CoverageCriterion;
import ida.searchPruning.search.criterion.Criterion;
import ida.searchPruning.search.*;
import logicStuff.learning.languageBias.LanguageBias;
import ida.searchPruning.search.strategy.*;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.searchPruning.util.Utils;
import ida.utils.tuples.Pair;
import logicStuff.learning.constraints.shortConstraintLearner.ConstraintLearnerProperties;
import logicStuff.learning.constraints.shortConstraintLearner.UltraShortConstraintLearnerFaster;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.learning.saturation.ConstantSaturationProvider;


import java.io.*;
import java.util.List;
import java.util.function.Predicate;

/**
 * Created by Admin on 04.05.2017.
 */
public class Runner {

    // in nanoseconds
    // use -J-Dida.searchPruning.runner.overallLimit= time limit in minutes
    private final static long OVERALL_LIMIT = Integer.parseInt(System.getProperty("ida.searchPruning.runner.overallLimit", "" + (20 * 60))) * 60 * 1000000000l;
    private final static long RULE_LEARNING_LIMIT = 2 * 60 * 1000000000l;

    // setting
    //int maxDepth = method == "beam" ? 8 : 6;
    private final static int BEAM_WIDTH = 20;
    private final static long MAX_NODES = Long.MAX_VALUE;


    public static void main(String[] args) throws IOException {

        String dataPath =
                "./in/nci_transformed/gi50_screen_SR.txt";
        //"./in/ptcmr/transformed.txt";
        int maxLength = 2;
        int maxVariables = 2;
                /*"./in/ptcmr/ptc_mr.txt"; int maxLength = 2; int variables=5;*/

        /** /
         maxLength = 0;
         maxVariables = 0;
         /**/

        int maxDepth = 3;
        int minFrequency = 10;

        run(dataPath, "breadth", maxLength, maxVariables, maxDepth, minFrequency, -1);
    }

    public static void run(String dataPath, String method, int maxLength, int maxVariables, int maxDepth, int minFrequency, int fold) throws IOException {
        Runner runner = new Runner();
        SimpleLearner.VERBOUSE = false;
        int maxNegCovered = 10;

        //Criterion criterion = new Compression(maxNegCovered);
        Criterion criterion = new CoverageCriterion(maxNegCovered);

        System.out.println("change time constraints");
        //35 namisto 2
        //10 *

        TimeDog overallTime = new TimeDog(OVERALL_LIMIT, false); // false -> zapnuty, true -> vypnuty timeDog
        //TimeDog overallTime = new TimeDog(30 * 60 * 1000000000l, "breadth" == method ? true : false); // false -> zapnuty, true -> vypnuty timeDog
        TimeDog ruleLearningTime = new TimeDog(RULE_LEARNING_LIMIT, "breadth" == method ? true : false); // false -> zapnuty, true -> vypnuty timeDog
        //TimeDog overallTime = new TimeDog(40 * 1000000000l, "breadth" == method ? true : false); // false -> zapnuty, true -> vypnuty timeDog
        //TimeDog ruleLearningTime = new TimeDog(20 * 1000000000l, "breadth" == method ? true : false); // false -> zapnuty, true -> vypnuty timeDog

        runner.run(dataPath, method, maxLength, maxVariables, criterion, overallTime, dataPath, maxNegCovered, ruleLearningTime, maxDepth, minFrequency, fold);
    }

    // ok, tohle je trochu nechutne
    private void run(String dataPath, String method, int maxLength, int maxVariables, Criterion criterion, TimeDog overallTime, String path, int maxNegCovered, TimeDog ruleLearningTime, int maxDepth, int minSupport, final int userFold) throws IOException {
        File outputDir = Utils.createFolder(path + File.separator +
                String.join("_", Sugar.list(method, maxLength + "", maxVariables + "", MAX_NODES + "", maxDepth + "", BEAM_WIDTH + "", minSupport + "")));
        // + method + "_" + maxLength + "_" + maxVariables + "_" + MAX_NODES + "_" + maxDepth + "_" + BEAM_WIDTH + "_" + minSupport);
        MEDataset med = MEDataset.create(dataPath, Matching.THETA_SUBSUMPTION);

        System.out.println("using theta subsumption for dataset");


        int folds = resolveFolds(userFold, med.getExamples().size()); // -1;//1;//10;

        System.out.println("setting for directory");
        System.out.println("criterion\t" + ("breadth".equals(method) ? "none because breadth is used" : criterion));
        System.out.println("output dir\t" + outputDir);
        System.out.println("method\t" + method);
        System.out.println("maxLength\t" + maxLength);
        System.out.println("maxVariables\t" + maxVariables);
        System.out.println("maxNegCovered\t" + maxNegCovered);
        System.out.println("maxNanoTime\t" + overallTime.getLimit());
        System.out.println("maxDepth\t" + maxDepth);
        System.out.println("BEAM_WIDTH\t" + BEAM_WIDTH);
        System.out.println("MAX_NODES\t" + MAX_NODES);
        System.out.println("folds\t" + folds);
        System.out.println("minimal frequency\t" + minSupport);


        Searchable<List<HornClause>> search = null;
        if ("beam".equals(method)) {
            System.out.println("you have to implement constraintLearnerProperties here! and uncomment the line below");
            throw new IllegalStateException();// NotImplementedException();
            //search = beamSearch(med, maxLength, maxVariables, maxDepth, BEAM_WIDTH, criterion, overallTime, ruleLearningTime, minFrequency);
        } else if ("best".equals(method)) {
            System.out.println("you have to implement constraintLearnerProperties here! and uncomment the line below");
            //search = bestFirstSearch(med, maxLength, maxVariables, MAX_NODES, criterion, overallTime, ruleLearningTime, minFrequency);
        }

        if (null != search) {
            Crossvalidation cross = new Crossvalidation(med);
            Stats<List<HornClause>> results = cross.run(search, folds);

            String result = "value\nfolds: " + (results.getResults().size()) + "\n" +
                    "constraint time [ms]: " + (results.constraintTime() / 1000000) + "\n" +
                    "search time [ms]: " + (results.searchTime() / 1000000) + "\n" +
                    "overall time [ms]: " + (results.overallTime() / 1000000) + "\n" +
                    "train acc: " + results.trainAccuracy() + "\n" +
                    "test acc: " + (results.testAccuracy()) + "\n" +
                    "searched nodes: " + (results.searchedNodes()) + "\n" +
                    "pruned nodes: " + (results.avgPrunedNodes()) + "\n" +
                    "avg hypotheses length: " + (results.avgHypothesesLength()) + "\n" +
                    "deviance hypotheses length: " + (results.devianceHypothesesLength()) + "\n" +
                    "avg killed: " + (results.avgKilled()) + "\n" +
                    "avg extended: " + (results.avgExtended()) + "\n" +
                    "learned rules: " + (results.avgNumberOfRules());

            System.out.println(result);
            writeToStatsFile(result, outputDir, -1);

            StringBuilder sb = new StringBuilder();
            sb.append("\nmodels");
            for (int idx = 0; idx < folds; idx++) {
                sb.append("\nmodel " + idx);
                results.getResults().get(idx).r.forEach(horn -> sb.append("\n\t" + horn));
            }
            System.out.println(sb.toString());
            writeToModelsFile(sb.toString(), outputDir);
        }

        if ("breadth".equals(method)) {
            ConstraintLearnerProperties clp = ConstraintLearnerProperties.create();
            BreadthSearchable breadth = breadthFirstSearch(med, maxLength, maxVariables, maxDepth, overallTime, minSupport, clp);
            Crossvalidation cross = new Crossvalidation(med);
            List<LevelWiseStats> results = cross.runBreadth(breadth, folds, outputDir, userFold);

            for (LevelWiseStats result : results) {
                maxDepth = result.getDepth();
                outputDir = Utils.createFolder(makeOutputDirName(path, method, maxLength, maxVariables, MAX_NODES, maxDepth, BEAM_WIDTH, minSupport, clp));

                System.out.println("storing " + result.getDepth() + "-th level at " + outputDir);
                if ((userFold < 0 && result.fullyComputed() != folds) || (result.fullyComputed() != 1 && userFold > -1)) {
                    System.out.println("stopping since this layer is not full (not every fold was computed)");
                    continue;
                }

                String output = "value\nfolds: " + (result.fullyComputed()) + "\n"
                        + "depth : " + (result.getDepth()) + "\n"
                        + "constraint time [ms]: " + (result.constraintTime() / 1000000) + "\n"
                        + "search time [ms]: " + (result.searchTime() / 1000000) + "\n"
                        + "overall time [ms]: " + (result.overallTime() / 1000000) + "\n"
                        + "avg hypotheses length: " + (result.avgHypothesesLength()) + "\n"
                        + "deviance hypotheses length: " + (result.devianceHypothesesLength()) + "\n"
                        + "searched nodes: " + (result.searchedNodes()) + "\n"
                        + "pruned nodes: " + (result.avgPrunedNodes()) + "\n"
                        + "# hypotheses: " + (result.nonReducedHypotheses()) + "\n"
                        + "# reduced: " + (result.numberOfReduced()) + "\n"
                        + "avg killed: " + (result.avgKilled()) + "\n"
                        + "avg extended: " + (result.avgExtended());
                System.out.println(result);
                writeToStatsFile(output, outputDir, userFold);

                for (int foldLike = 0; foldLike < result.fullyComputed(); foldLike++) {
                    int subscribe = foldLike;
                    if (userFold >= 0) {
                        subscribe = userFold;
                    }

                    System.out.println("rules/features found not written down (data and time saving)");
                    //Utils.writeData(result.getDataset(), result.trainIndexes(foldLike), new ArrayList<>(result.reduced(foldLike)), new PrintWriter(outputDir.getAbsolutePath() + File.separator + subscribe + ".train"));
                    //Utils.writeData(result.getDataset(), result.testIndexes(foldLike), new ArrayList<>(result.reduced(foldLike)), new PrintWriter(outputDir.getAbsolutePath() + File.separator + subscribe + ".test"));
                    //Utils.writeData(result.rules(foldLike), new PrintWriter(outputDir.getAbsolutePath() + File.separator + subscribe + ".all"));
                    //Utils.writeData(result.reduced(foldLike), new PrintWriter(outputDir.getAbsolutePath() + File.separator + subscribe + ".reduced"));
                }
            }
        } else if (null == search) {
            new IllegalStateException("do not know option '" + method + "'");
        }
    }

    private int resolveFolds(int userFold, int size) {
        if (userFold < 0) {
            return 1;
        } else if (size < userFold) { // one against all
            return size;
        } else if (0 == userFold) {
            return 1;
        }
        return userFold;
    }


    private Searchable<List<HornClause>> bestFirstSearch(MEDataset med, int maxLength, int maxVariables, long maxNodes, Criterion criterion, TimeDog time, TimeDog ruleLearningTime, int minFrequency) {
        return (learnFrom) -> {
            long constraintStart = System.nanoTime();
            UltraShortConstraintLearnerFaster shortConstraintLearner = UltraShortConstraintLearnerFaster.create(med, maxLength, maxVariables, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            Pair<List<Clause>, List<Clause>> constraintsPair = shortConstraintLearner.learnConstraints(false, false, false, Integer.MAX_VALUE, Sugar.set(), false, 0, false);
            List<Clause> constraints = constraintsPair.r;
            long constraintEnd = System.nanoTime();

            System.out.println("constraints learned: " + constraints.size() + " \t with setting " + maxLength + " : " + maxVariables);
            /*constraints.forEach(c -> System.out.println("\t" + c));
            if (constraints.isEmpty()) {
                System.out.println("the set of constraints is empty");
            }*/

            long searchStart = System.nanoTime();
            BestFirstSearch search = new BestFirstSearch(med, criterion, constraints, time, ruleLearningTime, minFrequency);
            SearchStatsWrapper<List<HornClause>> result = search.search(maxNodes);
            long searchEnd = System.nanoTime();
            return new SearchStatsWrapper<>(result.getT(), constraintEnd - constraintStart, searchEnd - searchStart, result.getAverageLengthOfHypothesis(), result.getTotalSearchedNodes(), result.getTotalPrunedNodes(), result.getExtendedHypothesis(), result.getKilledHypothesis());
        };
    }

    private Searchable<List<HornClause>> beamSearch(DatasetInterface med, int maxLength, int maxVariables, int maxDepth, int beamWidth, Criterion criterion, TimeDog time, TimeDog ruleLearningTime, int minFrequency) {
        return (learnFrom) -> {
            long constraintStart = System.nanoTime();
            //MultiDatasetShortConstraintLearnerFaster shortConstraintLearner = new MultiDatasetShortConstraintLearnerFaster(datasets, maxLength, maxVariables);
            //List<Clause> constraints = shortConstraintLearner.learnConstraints();
            UltraShortConstraintLearnerFaster shortConstraintLearner = UltraShortConstraintLearnerFaster.create(med, maxLength, maxVariables, 0, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
            Pair<List<Clause>, List<Clause>> constraintsPairs = shortConstraintLearner.learnConstraints(false, false, false, Integer.MAX_VALUE, Sugar.set(), false, 0, false);
            long constraintEnd = System.nanoTime();

            List<Clause> constraints = constraintsPairs.r;

            System.out.println("constraints learned: " + constraints.size() + " \t with setting " + maxLength + " : " + maxVariables);
            /*constraints.forEach(c -> System.out.println("\t" + c));
            if (constraints.isEmpty()) {
                System.out.println("the set of constraints is empty");
            }*/

            long searchStart = System.nanoTime();
            BeamSearch search = new BeamSearch(med, constraints, criterion, time.fromNow(), ruleLearningTime, minFrequency);
            SearchStatsWrapper<List<HornClause>> result = search.search(maxDepth, beamWidth);
            long searchEnd = System.nanoTime();
            return new SearchStatsWrapper<>(result.getT(), constraintEnd - constraintStart, searchEnd - searchStart, result.getAverageLengthOfHypothesis(), result.getTotalSearchedNodes(), result.getTotalPrunedNodes(), result.getExtendedHypothesis(), result.getKilledHypothesis());
        };
    }

    private BreadthSearchable breadthFirstSearch(MEDataset med, int maxLiterals, int maxVariables, int maxDepth, TimeDog time, int minSupport, ConstraintLearnerProperties clp) {
        return (learnFrom) -> {
            TimeDog overallTimeDog = time.fromNow();
            long constraintStart = System.nanoTime();

            Pair<List<Clause>, List<Clause>> constraintsPair = null;
            int saturationMode = Integer.parseInt(System.getProperty("ida.searchPruning.bfs.saturationMode", "2"));

            constraintsPair = clp.learnConstraints(med);
            List<Clause> constraints = constraintsPair.r;

            /* the old way, the new way of parametriziation is not updated
            if (System.getProperty("ida.searchPruning.bfs.constraintLearner", "complete").toLowerCase().equals("sampling")) {
                SamplerConstraintLearner sampler = SamplerConstraintLearner.create(med,0);
                // add saturation mode
                constraints = sampler.learnConstraints(10,100,7*2,7,1,10,1,minSupport);
            } else {//System.getProperty("ida.searchPruning.bfs.constraintLearner","complete").toLowerCase().equals("complete")

                UltraShortConstraintLearnerFaster uscl = UltraShortConstraintLearnerFaster.create(med, maxLiterals, maxVariables,
                        0, 1, 1, Integer.MAX_VALUE);
                constraints = uscl.learnConstraints(maxLiterals > 0 && maxVariables > 0, false,
                        false, Integer.MAX_VALUE, Sugar.set(), false, saturationMode, false, Sugar.set(), minSupport);

            }*/
            long constraintEnd = System.nanoTime();
            System.out.println("constraints learned: " + constraints.size() + " \t with setting " + maxLiterals + " : " + maxVariables);

            long searchStart = System.nanoTime();
            BreadthFirstSearch search = new BreadthFirstSearch(med, ConstantSaturationProvider.createFilterSaturator(constraints, constraintsPair.getS()), overallTimeDog, minSupport, constraintEnd - constraintStart, Integer.MAX_VALUE);
            BreadthResults result = search.search(maxDepth, searchStart, new LanguageBias() {
                @Override
                public Predicate<HornClause> predicate() {
                    return (hornClause) -> true;
                }
            });
            long searchEnd = System.nanoTime();

            System.out.println("end of time");
            return result;
        };
    }


    // all of this try to shift to utils
    private String makeOutputDirName(String path, String method, int maxLength, int maxVariables, long maxNodes, int maxDepth, int beamWidth, int minSupport, ConstraintLearnerProperties clp) {
        List<String> properties = Sugar.list(method, maxLength + "", maxVariables + "", maxNodes + "", maxDepth + "", beamWidth + "", minSupport + "");
        properties.addAll(clp.propertiesToList());
        return Sugar.path(path, String.join("_", properties));
    }

    public static void writeToModelsFile(String s, File outputDir) throws FileNotFoundException {
        Sugar.writeTo(s, new PrintWriter(outputDir + File.separator + "models.txt"));
    }

    public static void writeToStatsFile(String s, File outputDir, int fold) throws FileNotFoundException {
        String output = fold >= 0 ? outputDir + File.separator + "stats_" + fold + ".txt" : outputDir + File.separator + "stats.txt";
        Sugar.writeTo(s, new PrintWriter(output));
    }


}
