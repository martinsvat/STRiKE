package ida.searchPruning.metavo;

import ida.searchPruning.util.Utils;
import ida.utils.Sugar;
import ida.utils.collections.MultiList;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Support for generation of scripts for experiments.
 * <p>
 * Created by martin.svatos on 05.05.2017.
 */
public class ScriptGenerator {

    //    public static String place = "/auto/praha1/svatoma1/searchPruning/";
    public static String ending = "metavo";


    public static void main(String[] args) throws FileNotFoundException {
//        searchPruning();
        propositialization();
//        pacReasoning();
    }

    private static void pacReasoning() throws FileNotFoundException {
        String finalDir = "kEntailment1";
        String outputDir = Sugar.path(".", ending, finalDir); // fiha, tohle je spatne, jaktoze to funguje?

        File outputDirectory = new File(outputDir);
        outputDirectory.mkdirs();

        MultiList<String, Object> parameters = new MultiList<>();
        parameters.putAll("mem", Sugar.list("10g"));
        parameters.putAll("-Dida.pacReasoning.overallLimit", Sugar.list(2 * 60));
        parameters.putAll("-Dida.pacReasoning.mode", Sugar.list("kEntailmentInterference"));
        parameters.putAll("-Dida.pacReasoning.kEntailmentOffset", Sugar.list("4"));
        parameters.putAll("-Dida.pacReasoning.theory", Sugar.list("./uwcsTheory", "./proteinTheory"));
//        parameters.putAll("-Dida.pacReasoning.evidence", Sugar.list("../datasets/protein-queries/test.db", "../datasets/uwcs-queries/test.db"));
        parameters.putAll("-Dida.pacReasoning.evidence", Sugar.linkedListFromCollections(
                IntStream.range(1485, 2001).mapToObj(idx -> "../datasets/protein-queries/queries/queries" + idx + ".db").collect(Collectors.toList())
//                ,IntStream.range(1, 501).mapToObj(idx -> "../datasets/uwcs-queries/queries/queries" + idx + ".db").collect(Collectors.toList())
        ));

        parameters.putAll("-Djava.util.concurrent.ForkJoinPool.common.parallelism",Sugar.list("8"));

        List<Predicate<Map<String, String>>> constraints = Sugar.list(
                // filter, s.t. crossvalidation folds are preserved
                (map) -> (map.get("-Dida.pacReasoning.evidence").contains("uwcs")
                        && map.get("-Dida.pacReasoning.theory").contains("uwcs"))
                        || (map.get("-Dida.pacReasoning.evidence").contains("protein")
                        && map.get("-Dida.pacReasoning.theory").contains("protein"))
                //, (map)
        );

        ParameterGrid grid = ParameterGrid.create(parameters, constraints);
        StringBuilder qsub = storeGiveScripts(finalDir, outputDir, grid, "/storage/plzen1/home/svatoma1/pac/", "pac.jar", "-Dida.pacReasoning.overallLimit");
        Utils.writeToFile(Sugar.path(outputDir, "qsub.sh"), qsub);
    }


    private static void propositialization() throws FileNotFoundException {
        String finalDir = "propSubsampling04";
        String outputDir = Sugar.path(".", ending, finalDir);

        File outputDirectory = new File(outputDir);
        outputDirectory.mkdirs();

        MultiList<String, Object> parameters = new MultiList<>();
        parameters.putAll("mem", Sugar.list("10g"));
        parameters.putAll("-Dida.searchPruning.runner.overallLimit", Sugar.list(2 * 60));
        List<String> domains = Sugar.list("gi50_screen_HCT_116"
                ,"gi50_screen_OVCAR_8"
                ,"gi50_screen_HT29"
                ,"gi50_screen_NCI_H23"
                ,"gi50_screen_KM12"
                ,"gi50_screen_IGROV1"
                ,"gi50_screen_UACC_257"
                ,"gi50_screen_UACC_62"
                ,"gi50_screen_OVCAR_3"
                ,"gi50_screen_OVCAR_5"
                ,"gi50_screen_SK_MEL_5"
                ,"gi50_screen_NCI_H322M"
                ,"gi50_screen_SN12C"
                ,"gi50_screen_COLO_205"
        );
        parameters.putAll("-Dida.searchPruning.propositialization.train",
                domains.stream().flatMap(domain -> IntStream.range(0, 10).mapToObj(idx -> "../datasets/splitted/nci_transformed/" + domain + "/" + idx + "/train")).collect(Collectors.toList()));
        parameters.putAll("-Dida.searchPruning.propositialization.test",
                domains.stream().flatMap(domain -> IntStream.range(0, 10).mapToObj(idx -> "../datasets/splitted/nci_transformed/" + domain + "/" + idx + "/test")).collect(Collectors.toList()));
        parameters.putAll("-Dida.searchPruning.propositialization.inputDir",
                domains.stream().flatMap(domain -> IntStream.range(0, 10).mapToObj(idx -> "../subsampled04/" + domain + "/" + idx + "/train/")).collect(Collectors.toList())
        );

        parameters.putAll("-Dida.searchPruning.propositialization.method", Sugar.list("existential"));
        parameters.putAll("-Dida.searchPruning.propositialization.filter", Sugar.list("uniqueValuesShorter"));
        parameters.putAll("-Dida.searchPruning.propositialization.skipLastHypothesisFile", Sugar.list("false"));
        parameters.putAll("-Djava.util.concurrent.ForkJoinPool.common.parallelism", Sugar.list(8));


        List<Predicate<Map<String, String>>> constraints = Sugar.list(
                // filter, s.t. crossvalidation folds are preserved
                (map) -> {
                    return domains.stream().anyMatch(domain -> {
                        return IntStream.range(0, 10).anyMatch(idx -> {
                            String mask = "/" + domain + "/" + idx + "/";
                            return map.get("-Dida.searchPruning.propositialization.train").contains(mask)
                                    && map.get("-Dida.searchPruning.propositialization.test").contains(mask)
                                    && map.get("-Dida.searchPruning.propositialization.inputDir").contains(mask);
                        });
                    });
                }
        );
        ParameterGrid grid = ParameterGrid.create(parameters, constraints);
        StringBuilder qsub = storeGiveScripts(finalDir, outputDir, grid, "/storage/plzen1/home/svatoma1/xpruning/", "propositializationer.jar", "-Dida.searchPruning.runner.overallLimit");
        Utils.writeToFile(Sugar.path(outputDir, "qsub.sh"), qsub);
    }

    private static StringBuilder storeGiveScripts(String finalDir, String outputDir, ParameterGrid grid, String homedir, String jarExecutable, String timeLimitToken) throws FileNotFoundException {
        Iterator<Map<String, String>> iterator = grid.iterator();
        int scriptNo = 0;
        StringBuilder qsub = new StringBuilder();
        while (iterator.hasNext()) {
            scriptNo++;
            Map<String, String> instance = iterator.next();
            storeParameters(instance, grid, Sugar.path(outputDir, scriptNo + ".sh"), homedir + finalDir, jarExecutable);
            // update qsub
            int threads = Math.max(1, Integer.parseInt(instance.get("-Djava.util.concurrent.ForkJoinPool.common.parallelism")) + 1);
            String walltime = (Integer.parseInt(instance.get(timeLimitToken)) / 60 + 2) + ":00:00";
            qsub.append("qsub -l select=1:ncpus=" + threads + ":mem=" + (Integer.parseInt(instance.get("mem").substring(0, instance.get("mem").indexOf("g"))) + 1) + "g"
                    + ":scratch_local=5g -l walltime=" + walltime + " " + scriptNo + ".sh\n");
        }
        return qsub;
    }

    private static void searchPruning() throws FileNotFoundException {
        String finalDir = "subsampling04";
        String outputDir = Sugar.path(".", ending, finalDir);

        File outputDirectory = new File(outputDir);
        outputDirectory.mkdirs();

        MultiList<String, Object> parameters = new MultiList<>();
        parameters.putAll("mem", Sugar.list("10g"));
        parameters.putAll("-Dida.searchPruning.mining.bfs.adaptiveConstraints", Sugar.list(
                "none"
                //"updateFrom5-selectTop10-sampling-dep"//,
                //"updateOdd-selectTop5-sampling-dep",
                //"updateEveryLayer-selectTop5-sampling-dep"//,
        ));

        parameters.putAll("-Dida.logicStuff.constraints.useSaturation", Sugar.list("true"));
        parameters.putAll("-Dida.logicStuff.constraints.maxLiterals", Sugar.list(3));
        parameters.putAll("-Dida.logicStuff.constraints.maxVariables", Sugar.list(3 * 2));
        parameters.putAll("-Dida.logicStuff.constraints.learner", Sugar.list("complete", "none")); // "none", "sampling5-10-100", "parallel", "complete"
        parameters.putAll("-Dida.searchPruning.runner.overallLimit", Sugar.list(3 * 60));
        parameters.putAll("-Dida.logicStuff.constraints.minSupport", Sugar.list(100)); // 100,1
        parameters.putAll("-Dida.searchPruning.minSupport", Sugar.list(100));//100,1
        parameters.putAll("-Dida.searchPruning.mining", Sugar.list(
                //"theory"
                //"bfs"
                //, "bfs-sampling-1000"
                //"bfs"
                "bfs-sampling-100"
        ));
        int literals = 6;
        parameters.putAll("-Dida.searchPruning.maxDepth", Sugar.list(literals));
        parameters.putAll("-Dida.searchPruning.maxVariables", Sugar.list(literals * 2));
        parameters.putAll("-Dida.searchPruning.datasetSubsumption", Sugar.list("OI"));
        parameters.putAll("-Dida.searchPruning.storeOutput", Sugar.list("featuresOI")); // "featuresOI" , "theory"
        //parameters.putAll("-Dida.searchPruning.simplifyTheory", Sugar.list("true"));
        //parameters.putAll("-Dida.searchPruning.input", Sugar.list("../datasets/splitted/nci_transformed/gi50_screen_KM20L2/split7030/train")
        parameters.putAll("-Dida.searchPruning.input", //Sugar.list("../datasets/mlns/uwcs/all.db.transformed")
                /*Sugar.list(
                        "../datasets/groups/quasiDataset",
                        "../datasets/groups/loopDataset",
                        "../datasets/groups/moufangs100",
                        "../datasets/pddl/grip/examplesHalf",
                        "../datasets/mlns/uwcs/uw2.transformed",
                        "../datasets/mlns/protein/merged.txt.transformed",
                        "../datasets/mlns/imdb/merged.db.oneLine"
                )*/
                //Sugar.list("/storage/plzen1/home/svatoma1/xpruning/datasets/splitted/nci_transformed/gi50_screen_KM20L2/split7030/train")

                //Sugar.list("../datasets/splitted/nci_transformed/gi50_screen_KM20L2/split7030/train")
                Sugar.list( "gi50_screen_HCT_116"
                        ,"gi50_screen_OVCAR_8"
                        ,"gi50_screen_HT29"
                        ,"gi50_screen_NCI_H23"
                        ,"gi50_screen_KM12"
                        ,"gi50_screen_IGROV1"
                        ,"gi50_screen_UACC_257"
                        ,"gi50_screen_UACC_62"
                        ,"gi50_screen_OVCAR_3"
                        ,"gi50_screen_OVCAR_5"
                        ,"gi50_screen_SK_MEL_5"
                        ,"gi50_screen_NCI_H322M"
                        ,"gi50_screen_SN12C"
                        ,"gi50_screen_COLO_205"
//                        ,"gi50_screen_EKVX"
//                        ,"gi50_screen_HOP_62"
//                        ,"gi50_screen_SK_MEL_2"
//                        ,"gi50_screen_K_562"
//                        ,"gi50_screen_NCI_H522"
//                        ,"gi50_screen_OVCAR_4"
//                        ,"gi50_screen_NCI_H460"
//                        ,"gi50_screen_LOX_IMVI"
//                        ,"gi50_screen_CAKI_1"
//                        ,"gi50_screen_RPMI_8226"
                        //,"gi50_screen_MOLT_4"
//                        ,"gi50_screen_M14"
//                        ,"gi50_screen_SK_OV_3"
//                        ,"gi50_screen_ACHN"
//                        ,"gi50_screen_SNB_75"
//                        ,"gi50_screen_CCRF_CEM"
//                        ,"gi50_screen_MALME_3M"
//                        ,"gi50_screen_A498"
                ).stream()
                        .flatMap(domain -> IntStream.range(0, 10)
                                .mapToObj(idx -> "../datasets/splitted/nci_transformed/" + domain + "/" + idx + "/train"))
                        .collect(Collectors.toList())

                //IntStream.range(0,10).mapToObj(idx -> "../datasets/splitted/nci_transformed/gi50_screen_KM20L2/"+idx+"/train").collect(Collectors.toList())
        );
        //parameters.putAll("-Dida.searchPruning.cpm.theoryBias", Sugar.list("pddl", ""));
//        parameters.putAll("-Dida.searchPruning.modeDeclaration", Sugar.list("molecular"));
//        parameters.putAll("-Dida.logicStuff.mode",
//                IntStream.range(15, 17).mapToObj(idx -> "bond-2-" + idx).collect(Collectors.toList()));

        parameters.putAll("-Djava.util.concurrent.ForkJoinPool.common.parallelism", Sugar.list(8));


        /*parameters.putAll("-Dida.searchPruning.mlnInput", Sugar.list("true"));
        parameters.putAll("-Dida.searchPruning.targetPredicate", Sugar.list(
                //"interaction/2"//,
                "professor/1", "student/1", "publication/2"
        ));
        */

        List<Predicate<Map<String, String>>> constraints = Sugar.list(
                (map) -> {
                    if (map.get("-Dida.logicStuff.constraints.learner").equals("none")
                            && !map.get("-Dida.searchPruning.mining.bfs.adaptiveConstraints").equals("none")) {
                        return false;
                    }
                    if (!map.get("-Dida.searchPruning.mining.bfs.adaptiveConstraints").equals("none")
                            && map.get("-Dida.logicStuff.constraints.learner").equals("parallel")) {
                        return false;
                    }
                    return true;
                },
                (map) -> map.get("-Dida.searchPruning.minSupport").equals(map.get("-Dida.logicStuff.constraints.minSupport"))
                /*, (map) -> (map.get("-Dida.searchPruning.cpm.theoryBias").equals("pddl") && map.get("-Dida.searchPruning.input").contains("/pddl/"))
                        || (!map.get("-Dida.searchPruning.cpm.theoryBias").equals("pddl") && !map.get("-Dida.searchPruning.input").contains("/pddl/"))
                */
                /*(map) -> (map.get("-Dida.searchPruning.minSupport").equals("100") && map.get("-Dida.searchPruning.targetPredicate").equals("publication/2"))
                        || (map.get("-Dida.searchPruning.minSupport").equals("1") && !map.get("-Dida.searchPruning.targetPredicate").equals("publication/2"))
                */
                // just a quick hack, not working hack
                //, (map) -> !map.get("-Dida.logicStuff.mode").equals("molecular-bond-2-15") || map.get("-Dida.logicStuff.constraints.useSaturation").equals("true")
                //, (map) -> !map.get("-Dida.logicStuff.mode").equals("molecular-bond-2-16") || map.get("-Dida.logicStuff.constraints.useSaturation").equals("false")
        );
        ParameterGrid grid = ParameterGrid.create(parameters, constraints);
        StringBuilder qsub = storeGiveScripts(finalDir, outputDir, grid, "/storage/plzen1/home/svatoma1/xpruning/", "SearchPruning.jar", "-Dida.searchPruning.runner.overallLimit");
        Utils.writeToFile(Sugar.path(outputDir, "qsub.sh"), qsub);
    }

    private static void storeParameters(Map<String, String> instance, ParameterGrid grid, String outputFileName, String homedir, String jarExecutable) throws FileNotFoundException {
        String params = "cd " + homedir + "\n" +
                "module add jdk-8 \n" +
                "java  -XX:+UseSerialGC -XX:NewSize=5000m -Xms5g  -Xmx" + instance.get("mem") + " " +
                grid.dPropertiesOnliner(instance) + " -jar " + jarExecutable;
        Utils.writeToFile(outputFileName, params);
        System.out.println(grid.dPropertiesOnliner(instance));
    }
}
