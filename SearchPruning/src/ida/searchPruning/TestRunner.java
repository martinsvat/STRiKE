package ida.searchPruning;

import ida.ilp.logic.Clause;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.utils.Sugar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 18. 1. 2018.
 */
public class TestRunner {

    public static void main(String[] args) throws IOException {
        System.setProperty("ida.grid", "off");

        System.out.println(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));

//        compareResults();
        mineTheory();
//        runCPM();
//        runPropositialization();
        //propFeaturesOI();
//        farmerCheck();
    }

    private static void farmerCheck() throws IOException {
        /**/ //farmer output check -- more queries than aleph
        // conjunction
        Path q1 = Paths.get(".\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train");
        System.out.println(q1.toAbsolutePath());

        List<Clause> farmer = Files.readAllLines(Paths.get("E:\\experiments\\farmerBonds\\bonds\\farmerB4_100\\9.hypotheses")).stream().filter(line -> !line.contains("emptyClause")).map(Clause::parse).map(LogicUtils::flipSigns).collect(Collectors.toList());
        Set<IsoClauseWrapper> bfs = Files.list(Paths.get("E:\\experiments\\modeBonds\\modeBonds\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train\\output_bfs-bond-2-4_100_OI_15_45_100_3_6_1_1_2147483647_2_none_100_100_none")).
                //Set<IsoClauseWrapper> bfs = Files.list(Paths.get("E:\\experiments\\modeBonds\\modeBonds\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train\\output_bfs-bond-2-4_100_OI_15_45_100_3_6_1_1_2147483647_2_complete_100_100_none")).
                        flatMap(file -> {
                    try {
                        return Files.readAllLines(file).stream();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    throw new IllegalStateException();
                }).filter(line -> !line.contains("emptyClause")).map(Clause::parse).map(LogicUtils::flipSigns).map(IsoClauseWrapper::create).collect(Collectors.toSet());
        /*UltraShortConstraintLearnerFaster scl = UltraShortConstraintLearnerFaster.create(MEDataset.create(Paths.get(".\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train"), Matching.THETA_SUBSUMPTION),
                3, 6, 0, 1, 1, Integer.MAX_VALUE);
        //domain constraints (for saturation) and constraints with minsupport (only for pruning)
        Pair<List<Clause>, List<Clause>> constriants = scl.learnConstraints(true, false, false, Integer.MAX_VALUE, Sugar.list(),
                false, 2, false, Sugar.set(), 100);
        ConjunctureSaturator saturator = ConjunctureSaturator.create(constriants.getR(), false, true);

        for (Clause clause : farmer) {
            Clause saturatedBody = saturator.saturate(clause);
            IsoClauseWrapper icw = IsoClauseWrapper.create(saturatedBody);
            if (!bfs.contains(icw)) {
                System.out.println("\ndiff here");
                System.out.println("orig:\t" + clause);
                System.out.println("satc:\t" + saturatedBody);
            } else {
                // is in, no one cares
            }
        }*/

        if (false) {
            Map<IsoClauseWrapper, IsoClauseWrapper> cache = new HashMap<>();
            for (Clause clause : farmer) {
                IsoClauseWrapper icw = IsoClauseWrapper.create(clause);
                if (!bfs.contains(icw)) {
                    System.out.println("\nis not there\t" + clause);
                }
                if (!cache.containsKey(icw)) {
                    cache.put(icw, icw);
                } else {
                    System.out.println("\nduplicate\n\t" + clause + "\n\t" + cache.get(icw).getOriginalClause());
                }
            }
            // ve vypisu CPM jsem nasel !car(V2), !2_bond(V4, V1), !ar_bond(V1, V2), !2_bond(V1, V4) to by mel byt ten prvni

//        is not there	ar_bond(V1N1, V1N0), 2_bond(V1N1, V1N2), 2_bond(V1N2, V1N1), car(V1N0)
//        is not there	2_bond(V1N1, V1N2), 2_bond(V1N2, V1N1), am_bond(V1N0, V1N1), am_bond(V1N1, V1N0)
//        is not there	nam(V1N0), 2_bond(V1N1, V1N2), 2_bond(V1N2, V1N1), am_bond(V1N0, V1N1), am_bond(V1N1, V1N0)
//        is not there	o2(V1N2), 2_bond(V1N1, V1N2), 2_bond(V1N2, V1N1), am_bond(V1N0, V1N1), am_bond(V1N1, V1N0)
//        is not there	ar_bond(V1N0, V1N1), car(V1N0), ar_bond(V1N1, V1N2), 2_bond(V1N2, V1N3), 2_bond(V1N3, V1N2)
//        is not there	nam(V1N0), o2(V1N2), 2_bond(V1N1, V1N2), 2_bond(V1N2, V1N1), am_bond(V1N0, V1N1), am_bond(V1N1, V1N0)
        }
        if (true) {
            bfs.stream().map(IsoClauseWrapper::getOriginalClause).filter(c ->
                    c.predicates().contains("car") && c.predicates().contains("2_bond") && c.predicates().contains("ar_bond")
                            && c.countLiterals() == 4
                            && c.variables().size() == 3)
                    .forEach(c -> System.out.println(c));
        }
    }


    private static void mineTheory() {
        System.setProperty("ida.logicStuff.constraints.maxComponents", "1");
        System.setProperty("ida.logicStuff.constraints.maxPosLit", "1");
        System.setProperty("ida.logicStuff.constraints.maxNegLit", "" + Integer.MAX_VALUE);

        int lits = 3;
        int vars = 2 * lits;
        System.setProperty("ida.logicStuff.constraints.maxLiterals", "" + lits);
        System.setProperty("ida.logicStuff.constraints.maxVariables", "" + vars);
        System.setProperty("ida.logicStuff.constraints.minSupport", "1");
        System.setProperty("ida.logicStuff.constraints.learner", "smarter");
//        System.setProperty("ida.logicStuff.constraints.learner", "complete"); //  smarter, none, complete, sampling5-10-100     (parallel tady nendava smysl)
        System.setProperty("ida.logicStuff.constraints.useSaturation", "true"); // true, false .... tohle nastaveni nas tu nejvice zajima
        System.setProperty("ida.searchPruning.runner.overallLimit", 1 * 60 + ""); // [min]
        System.setProperty("ida.searchPruning.mining", "theory");
        System.setProperty("ida.searchPruning.storeOutput", "theory");

        System.setProperty("ida.logicStuff.constraints.saturationMode", "3");

        System.setProperty("ida.logicStuff.constraints.useTypes", "false");
        System.setProperty("ida.searchPruning.badRefinementsTo", "1000");

        System.setProperty("ida.logicStuff.constraints.oneLiteralHCrefinement", "false");
        System.setProperty("ida.logicStuff.constraints.hcExtendedRefinement", "false");
        System.setProperty("ida.searchPruning.ruleSaturator.useForbidden","false"); // toto nastaveni je pouze pro development

        System.out.println("!!!!!!!!!!!!!todo kdyz je types=true a useSaturation=true tak nefunguje pridavani vsech pravidel pro typy jeste :))");

//        System.setProperty("ida.searchPruning.simplifyTheory", "true");

        //System.setProperty("ida.searchPruning.cpm.theoryBias", "pddl");
        //System.setProperty("ida.searchPruning.input", Paths.get("..", "..", "pddl", "grip", "examplesHalf").toString());
        //System.setProperty("ida.searchPruning.input", Paths.get("..", "..", "grupDatasety", "loopDataset").toString());
        //System.setProperty("ida.searchPruning.input", Paths.get(".", "datasets","mlns","protein", "pacLabeledDataset.evidence").toString());
        //System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac\\yago2\\train.db.oneLineExample");
        //System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\mlns\\kinships\\train.db.oneLine");
        //System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\kinships\\train.db.oneLine");
        //System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\nationsA2\\train.db.oneLine");
//        System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\yago-states\\train.db.oneLine");
        //System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\kinships-ntp\\train.db.oneLine");
        //System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\groups\\loopDataset");
//        System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\nci_transformed\\gi50_screen_BT_549.txt");
//        System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\debugTest\\types.db");
//        System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\uwcs\\train.db.oneLine");
        System.setProperty("ida.searchPruning.input", "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\protein\\train.db.oneLine");
        // nations | protein | uwcs | umls | kinships\\train.db.oneLine
        // groups\\loopDataset
        // pddl\\grip\\examplesHalf     pustit nejak LB?
        // nci_transformed\\gi50_screen_BT_549.txt

        System.out.println("zkusit hledat s neomezenym mnozstvym komponent, aby bylo prave srovnani s claudienem :))");
        System.out.println("jo a taky claudien zlvada jenom range restricted takze na to tady zavest taky prepinac TODO !!!!!!!!!!!!!!!!!!!!!!! dulezite");

        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");

        System.out.println("starting");

        System.out.println("todo dodelat debugTest types.db !!!!!");
        System.out.println("dulezite -- verze -- " + System.getProperty("ida.logicStuff.constraints.learner"));

        //nekde chyba v implementaci oddebugovat proc nefunguje nastaveni tohle  tak aby v nasledujicim nebyly ty literaly s negaci
        //complex(V1, V2), complex(V2, V1)	|	complex(V1, V2), complex(V2, V1), !function(V2, V2), !complex(V1, V1), !location(V1, V1), !enzyme(V2, V2), !enzyme(V1, V1), !phenotype(V2, V2), !complex(V2, V2), !phenotype(V1, V1), !protein_class(V1, V1), !protein_class(V2, V2), !function(V1, V1), !location(V2, V2)
        System.out.println("vypsat si co za klauzule je v posledni vrstve tam kde se to lame -- jednou je to 5k podruhe se zapnutymi saturacemi mene :))");

        try {
            ConjunctivePatterMiner.main(new String[]{});
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private static void propFeaturesOI() throws IOException {
        // ida.searchPruning.propositialization.hypotheses path to hypotheses folder in which all files with ending *.hypotheses are traversed
        // ida.searchPruning.propositialization.train path to train file
        // ida.searchPruning.propositialization.test by | separated paths to files which should be tranformed

        // we do expect dataset of interpretations
        System.setProperty("ida.searchPruning.propositialization.method",
                "existential"
                //"sampling"
                //"counting"
        );
        System.setProperty("ida.searchPruning.propositialization.filter",
                //"none"
                "uniqueValuesShorter"
                //"existentialUniqueValuesShorter"
        );

        Propositializationer propositializationer = Propositializationer.create();
        Files.list(Paths.get("E:\\experiments\\featuresOI\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train"))
                .filter(folder -> {
                    try {
                        return Files.list(folder).anyMatch(file -> file.toFile().isFile() && file.toFile().getName().endsWith(".hypotheses"))
                                && !Files.list(folder).anyMatch(file -> file.toFile().isFile() && file.toFile().getName().endsWith(".pro"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .parallel()
                .forEach(folder -> {
                    try {
                        System.out.println("prop of " + folder);
                        propositializationer.propositionalizeHypothesesOnData(
                                folder.toString(),
                                Sugar.path("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", "train"),
                                Sugar.list(Sugar.path("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", "test")),
                                // just a hack to avoid the last hypotheses file which is in the directory, but is produced from the last, not-fully computed layer
                                (list) -> {
                                    Comparator<? super Path> comparator = Comparator.comparingInt(e -> Integer.parseInt(e.toFile().getName().substring(0, e.toFile().getName().indexOf('.'))));
                                    List<Path> filtered = list.stream().filter(path -> path.toFile().getName().endsWith(".hypotheses")).collect(Collectors.toList());
                                    if (filtered.isEmpty()) {
                                        return Sugar.list();
                                    }
                                    return filtered.stream().sorted(comparator).limit(filtered.size() - 1).collect(Collectors.toList());
                                }
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
        // \_\__\datasets\splitted\nci_transformed\gi50_screen_KM20L2\split7030\train\output_bfs_1_null_3_6_1_3_6_1_1_2147483647_2_none_1_100_none
//        System.setProperty("ida.searchPruning.propositialization.hypotheses", Sugar.path("_", "__", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", "train", "output_bfs_1_null_3_6_1_3_6_1_1_2147483647_2_none_1_100_none"));
//        System.setProperty("ida.searchPruning.propositialization.train", Sugar.path("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", "train"));
//        System.setProperty("ida.searchPruning.propositialization.test", Sugar.path("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", "test"));


    }

    private static void runPropositialization() throws IOException {
        // ida.searchPruning.propositialization.hypotheses path to hypotheses folder in which all files with ending *.hypotheses are traversed
        // ida.searchPruning.propositialization.train path to train file
        // ida.searchPruning.propositialization.test by | separated paths to files which should be tranformed

        // we do expect dataset of interpretations
        // \_\__\datasets\splitted\nci_transformed\gi50_screen_KM20L2\split7030\train\output_bfs_1_null_3_6_1_3_6_1_1_2147483647_2_none_1_100_none
        //Path inputDir = Paths.get("E:\\experiments\\modes\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train");
        Path inputDir = Paths.get("E:\\experiments\\groups\\_\\__\\datasets\\groups\\moufangLoop35");
        System.setProperty("ida.searchPruning.propositialization.hypotheses", inputDir.toString());
        System.setProperty("ida.searchPruning.propositialization.train",
//                Sugar.path("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", "train")
                Sugar.path("..", "datasets", "groups", "moufangLoop35")
        );
        System.setProperty("ida.searchPruning.propositialization.test",
                //Sugar.path("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", "test")
                Sugar.path("..", "datasets", "groups", "moufangLoop35")
        );
        System.setProperty("ida.searchPruning.propositialization.method",
                "existential"
                //"sampling"
                //"counting"
        );
        System.setProperty("ida.searchPruning.propositialization.filter",
                //"none"
                "uniqueValuesShorter"
                //"existentialUniqueValuesShorter"
        );

        /*Propositializationer propositializationer = Propositializationer.create();
        propositializationer.propositionalizeHypothesesOnData(
                System.getProperty("ida.searchPruning.propositialization.hypotheses"),
                System.getProperty("ida.searchPruning.propositialization.train"),
                Arrays.stream(System.getProperty("ida.searchPruning.propositialization.test").split("\\|")).collect(Collectors.toList())
        );*/
        System.out.println("prop path");

        Propositializationer propositializationer = Propositializationer.create();
        Files.list(inputDir)
                .filter(folder -> {
                    try { // just a cache
                        return Files.list(folder).anyMatch(file -> file.toFile().isFile() && file.toFile().getName().endsWith(".hypotheses"))
                                && !Files.list(folder).anyMatch(file -> file.toFile().isFile() && file.toFile().getName().endsWith(".pro"))
                                && !folder.toString().contains("farmer");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                })
                .parallel()
                .forEach(folder -> {
                    try {
                        System.out.println("prop of " + folder);
                        propositializationer.propositionalizeHypothesesOnData(
                                folder.toString(),
                                Sugar.path(System.getProperty("ida.searchPruning.propositialization.train", "")),
                                Sugar.list(System.getProperty("ida.searchPruning.propositialization.test", "")),
                                // just a hack to avoid the last hypotheses file which is in the directory, but is produced from the last, not-fully computed layer
                                (list) -> {
                                    Comparator<? super Path> comparator = Comparator.comparingInt(e -> Integer.parseInt(e.toFile().getName().substring(0, e.toFile().getName().indexOf('.'))));
                                    List<Path> filtered = list.stream().filter(path -> path.toFile().getName().endsWith(".hypotheses")).collect(Collectors.toList());
                                    if (filtered.isEmpty()) {
                                        return Sugar.list();
                                    }
                                    System.out.println("remember that the last *.hypotheses is thrown away... change it ad libitum (it is overfitted for our special case, not for general purpose)");
                                    return filtered.stream().sorted(comparator).limit(filtered.size() - 1).collect(Collectors.toList());
                                }
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

    }

    private static void runCPM() {
        //System.setProperty("ida.logicStuff.constraints.useMinSuppForFiltering","false"); // probably not implemented
        System.setProperty("ida.searchPruning.mining.bfs.adaptiveConstraints",
//                "updateFrom3-selectTop5-sampling-dep"
                //"none"
                //"updateOddFrom2-selectTop5-sampling-dep"
                //"updateOddFrom1-selectTop5-sampling-dep"
                //"parallel"
                //"none"
                //"updateOddFrom2-selectTop5-sampling-dep"
                //"updateFrom5-selectTop10-sampling-dep"
                //"updateFrom1-selectTop10-sampling-dep"
                "none"
        );
        System.setProperty("ida.logicStuff.constraints.maxLiterals", "1");
        System.setProperty("ida.logicStuff.constraints.minSupport", "1");
        System.setProperty("ida.logicStuff.constraints.learner", "none"); //  none, complete, sampling5-10-100, parallel
        System.setProperty("ida.searchPruning.runner.overallLimit", 1 * 60 + ""); // [min]
        System.setProperty("ida.searchPruning.mining", "bfs"); // bfs, bfs-sampling-1000
        System.setProperty("ida.searchPruning.minSupport", "1");
        int maxLiterals = 4;
        System.setProperty("ida.searchPruning.maxDepth", maxLiterals + "");
        System.setProperty("ida.searchPruning.maxVariables", (2 * maxLiterals) + "");
        System.setProperty("ida.searchPruning.storeOutput", "features");

        //System.setProperty("ida.searchPruning.modeDeclaration", "molecular");
        //System.setProperty("ida.logicStuff.mode", "bond-2-4");

        System.setProperty("ida.searchPruning.datasetSubsumption", "OI"); // oi, theta
        System.setProperty("ida.searchPruning.input", Sugar.path("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", "train"));


        // TODO set-up, test, repair if needed
//        System.setProperty("ida.searchPruning.input", Sugar.path(".", "datasets", "mlns", "proteinPac", "merged.txt.transformed"));
//        System.setProperty("ida.searchPruning.mlnInput","true");
        //System.setProperty("ida.searchPruning.targetPredicate","interaction/2");
//        System.setProperty("ida.searchPruning.targetPredicate","location/2"); // location/2

//        System.setProperty("ida.searchPruning.input", Sugar.path("..", "datasets", "mlns", "uwcs", "all.db.transformed"));
//        System.setProperty("ida.searchPruning.mlnInput", "true");
//        System.setProperty("ida.searchPruning.targetPredicate", "publication/2");


//        System.out.println("sequential version");
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");
        System.out.println("threads:\t" + System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));

        System.out.println("starting");

        try {
            ConjunctivePatterMiner.main(new String[]{});
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }


    private static void compareResults() {
//        List<Clause> farmer = load("E:\\experiments\\modes\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train\\farmer_100\\1.hypotheses");
        List<Clause> alephPost = load("E:\\experiments\\modes\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train\\aleph_100\\2.hypotheses");
//        List<Clause> alephRaw = load("C:\\data\\school\\development\\Xpruning\\experiments\\aleph\\connectedMode3\\kml\\duplicity\\aleph_100\\1.hypotheses");
        List<Clause> bfs = load("E:\\experiments\\modes\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train\\output_bfs_100_OI_15_30_100_3_6_1_1_2147483647_2_none_100_100_none\\2.hypotheses");
        List<Clause> sat = load("E:\\experiments\\modes\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train\\output_bfs_100_OI_15_30_100_3_6_1_1_2147483647_2_complete_100_100_none\\2.hypotheses");

//        System.out.println("farmer - bfs");
//        diff(farmer,bfs).forEach(System.out::println);
//        System.out.println("bfs - farmer");
//        diff(bfs,farmer).forEach(System.out::println);
//        System.out.println("bfs - alephRaw");
//        diff(bfs,alephRaw).forEach(System.out::println);
//        System.out.println("alephRaw - bfs");
//        diff(alephRaw,bfs).forEach(System.out::println);

        System.out.println("\nbfs - alephPost");
        diff(bfs, alephPost).forEach(System.out::println);
        System.out.println("\nalephPost - bfs");
        diff(alephPost, bfs).forEach(System.out::println);

//        System.out.println("\nsat - alephPost");
//        diff(sat, alephPost).forEach(System.out::println);
//        System.out.println("\nalephPost - sat");
//        diff(alephPost, sat).forEach(System.out::println);


//        System.out.println("\nalephRaw - alephPost");
//        diff(alephRaw,alephPost).forEach(System.out::println);
//        System.out.println("\nalephPost - alephRaw");
//        diff(alephPost,alephRaw).forEach(System.out::println);
//        System.out.println("alephPost - alephRaw");
//        System.out.println("alephRaw - alephPost");

    }

    private static List<Clause> diff(List<Clause> l1, List<Clause> l2) {
        Set<IsoClauseWrapper> set = l2.stream().map(IsoClauseWrapper::new).collect(Collectors.toSet());
        return l1.stream()
                .map(IsoClauseWrapper::new)
                .collect(Collectors.toSet())
                .stream()
                .filter(c -> !set.contains(c))
                .map(IsoClauseWrapper::getOriginalClause)
                .collect(Collectors.toList());
    }

    private static List<Clause> load(String path) {
        try {
            return Files.lines(Paths.get(path))
                    .map(Clause::parse)
                    .filter(c -> c.countLiterals() > 0)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }
}
