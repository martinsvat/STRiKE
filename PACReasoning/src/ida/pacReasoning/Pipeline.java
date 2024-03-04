package ida.pacReasoning;

import ida.ilp.logic.*;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.pacReasoning.data.DatasetSampler;
import ida.pacReasoning.data.PACAccuracyDataset;
import ida.pacReasoning.data.Reformatter;
import ida.pacReasoning.data.SubsampledDataset;
import ida.pacReasoning.entailment.Entailment;
import ida.pacReasoning.entailment.EntailmentSetting;
import ida.pacReasoning.entailment.Inference;
import ida.pacReasoning.entailment.REntailment;
import ida.pacReasoning.entailment.collectors.Entailed;
import ida.pacReasoning.entailment.collectors.KECollector;
import ida.pacReasoning.entailment.collectors.StratifiedCollector;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.entailment.cuts.Cut;
import ida.pacReasoning.entailment.cuts.KCut;
import ida.pacReasoning.entailment.cuts.VotesThresholdCut;
import ida.pacReasoning.entailment.theories.FOL;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.entailment.theories.Theory;
import ida.pacReasoning.evaluation.Data;
import ida.pacReasoning.evaluation.Utils;
import ida.pacReasoning.supportEntailment.SupportEntailmentInference;
import ida.pacReasoning.thrashDeprected.PossibilisticTheory;
import ida.searchPruning.ConjunctivePatterMiner;
import ida.utils.Sugar;
import ida.utils.collections.Counters;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.learning.saturation.RuleSaturator;
import logicStuff.theories.TheorySimplifier;
import logicStuff.theories.TheorySolver;
import logicStuff.theories.TypedTheorySolver;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 2. 1. 2019.
 */
public class Pipeline {


    public static String theoryType = "";

    {
        System.out.println("do experimentu cas horn learneru");
//        throw new IllegalStateException("asi nefunguje spravne search-based verze PL na cele evidenci -- kouknout na to :)");
//        vystupy z kPL jsou ohodnocene podle toho jakou vahou nejnizsiho pravdla to bylo vyvozene, takze tam udelat nekolik threshold cutu podle tech vah, zvlast na pac a zlvast acc
        System.out.println("pridat inzenyrske oriznuti radoby memory leaku pri dlohotrvajicim matchingu");

        System.out.println("koukám se na to špatně, zjistit proč je jenom cast tech literalu v modelu a ten zbytek neni; podle toho zjistit proc se nenajdou ty constrainy v support verzi");


        System.out.println("v theorySolver jsem vyhodil alldiff specpred pri pridavani porusenych podminek");
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        //speedupTest();
//        runICAJ();
//        plotIJCAI17Comparison();

        generateHitsDataset();
//        appendKGrepresentation();
//        generateBaseEmbeddingTemplate();
//        formateBigDatasets();
//        generateFragments();
        System.exit(-3);

        System.out.println("jsou tady divnoty s tema alldifama, ze tam jsou vicekrat, opravit :))");

//        uniqueWeights();
        //compareTheories();

//        comparePredicates();
//        predicatesHistogram();
//        dataSize();
        //plotIJCAI17Comparison();
        //presentResultsUWCS_PRAUCijcai();
//

        //presentResultsUWCS_PRAUC();

        System.out.println("\t\tlearn rules for infernce on NTP task");

        System.out.println("            ntp\n" +
                "    nechat bezet co nejvic experimenty dalsi\nprecist jak delaji inferenci v guarrua\n");

        System.out.println("uklidit to tady... dodat amie pravidla + pustit vybrane inference na kinships, nationsA2, umls.... naucit se teorie pro NTPs -- zkontrolovat jestli, kdyz mame pouze constrainy delkz 2, se pouziva onHCliteralPrune.... proc se vlastne vola na subsampledDatasetu to vytvoreni noveho matchingu, kvuli jakemu bottlenecku?");

//        checkConstraints();
//        System.exit(-12);

//        Set<Literal> lits = Sugar.union(LogicUtils.loadEvidence(Paths.get("..", "datasets", "nations", "train.db")), LogicUtils.loadEvidence(Paths.get("..", "datasets", "nations", "test.db")));
//        Map<Pair<Predicate, Integer>, Type> typs = TypesInducer.get().induce(lits);
//        Map<Pair<Predicate, Integer>, Type> q = TypesInducer.get().anyBurlRenaming(typs);
//        TypesInducer.store(q, Paths.get("..", "datasets", "nations", "typing2.txt"));
//        System.exit(-111);
//        allDiffCheck(Paths.get(".", "pac", "umls", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.AS"));
//        System.exit(10);
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");


//        baselinePlot();
//        debugMask();
        //presentResultsUMLS_PRAUC();
//        System.exit(-1);

//        baselinePlot();
//        debugInference();
//        debugPLinHand();
        //debugFOL();
//        debugFOLSupport();

//        debugTheories();
//        System.exit(-1);

//        retypeTheory();
//        System.exit(0);
//        presentResultsCuts();

        //presentResults();
//        presentResultsProtein_PRAUC();
//        presentResultsUMLS();
//        presentResultsUMLS_PRAUC();
//        presentResultsNations();
//        presentResultsNationsA2_PRAUC();
//          presentResultsNations_PRAUC();
//        presentResultsUWCS2();
//        presentResultsUWCS_PRAUC();
//        presentResultsKinships();
//        presentResultsKinships_PRAUC();
//        compareResults();
//        System.exit(10);
        /**/
        /**/
//        String r1 = presentResultsUMLS_PRAUC();
//        System.out.println(r1);
/** /
 String r3 = presentResultsNationsA2_PRAUC();
 System.out.println(r3);
 String r5 = presentResultsKinships_PRAUC();
 System.out.println(r5);
 System.exit(-789);
 /**/
//        showPR();

/* * /
        System.out.println("these are what makes the final result figures");
/** / String r3 = presentResultsNationsA2_PRAUC();
        System.out.println(r3);
        System.exit(66620);
/** /
        String r5 = presentResultsKinships_PRAUC();
        System.out.println(r5);
//        String r1 = presentResultsUMLS_PRAUC();
//        System.out.println(r1);
        System.exit(66620);
/**/
/* * /
        //        presentResultsProtein_PRAUC();
        //        System.exit(17);
        //theoryType = ".UP";
        //theoryType = ".OP";
        theoryType = "";

        //        String rx = presentResultsUWCS_PRAUC();
        //        System.out.println(rx);
        //        System.exit(-1);

        String r1 = presentResultsUMLS_PRAUC();
//        String r2 = presentResultsNations_PRAUC();
        String r3 = presentResultsNationsA2_PRAUC();
        String r2 = "";
        //String r4 = presentResultsUWCS_PRAUC();

        String r4 = "";
        String r5 = presentResultsKinships_PRAUC();
        //String r6 = presentResultsProtein_PRAUC();
        String r6 = "";
        System.out.println("\n\n\ntohle vsechno je AUC-PR s pruned theory\t" + theoryType + "\n" + r1 + "\n" + r2 + "\n" + r3 + "\n" + r4 + "\n" + r5 + r6);
        System.exit(-78441);
 /**/
//        theoryType = ".OP";
//        String r5 = presentResultsKinships_PRAUC();
//        System.out.println(r5);
//        System.exit(10);
//        String r5 = presentResultsKinships_PRAUC_selected();
//        System.out.println(r5);
//        System.exit(-1);

        System.out.println("naucit amie teorii v ruce a pusit t s kPL :))");
        System.out.println("vyresit jak zobrazit UWCSE a protein:))");
//        naucit amie teorii v ruce a pusit t s kPL :))
//        vyplotit grafy s kf a Af
/* * /
        //System.setProperty("pipeline.subsampledDataset", "0.5");
        for (String maxDepth : Sugar.list("3"//, "4"
        )) {
            for (String subsampledPsi : Sugar.list("0.4"//, "0.5"
            )) {
                for (String inference : Sugar.list(
                        "kPLWC","kPL"
                        //        "kPL", "PL", "oneS", "kPLWC", "PLW"
                )) {
                    System.setProperty("entailment", inference);
                    System.setProperty("atMost", "1");
                    System.setProperty("pipeline.driven", "crossentropy");
                    //            System.setProperty("pipeline.driven", "amie");
                    for (String domain : Sugar.list(
//                            "nations-ntp",
                             "kinships-ntp"
                            //, "umls-ntp"
                    )) {

                        runNTPcomparison(domain, maxDepth, subsampledPsi);
                    }
                }
            }
        }
        System.out.println("NTPS comparison ends");
        System.exit(-1245);
 /**/
/* * /
//        Path queries = Paths.get("..", "datasets", "nations-ntp", "ntp-test.txt");
//        System.out.println("tady to nemysli na validacni mnozinu");
        Utils u = Utils.get();
        for (String queryPart : Sugar.list("testDev2", "test2")) {
            System.out.println(queryPart);

            Path queries = Paths.get("..", "datasets", "nations-ntp", "hitsQueries." + queryPart + ".txt");
            //        Path queries = Paths.get("..", "datasets", "nations-ntp", "hitsQueries.test2.txt");
//        Path queries = Paths.get("..", "datasets", "nations-ntp", "hitsQueries.testDev2.txt");
            //Path predictions = Paths.get(".", "pac", "nations-ntp", "src-ntpTest_t-src_train.db.uni0.4.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c55.fol.poss.k5.CS.mc_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");
//        Path predictions = Paths.get(".", "pac", "nations-ntp", "src-ntpTest_t-src_train.db.uni0.4.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c55.fol.poss.k5.CS.mc_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");
//        Path predictions = Paths.get(".", "pac", "nations-ntp", "src-ntpTest_t-src_train.db.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c55.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");
            //Path predictions = Paths.get(".", "pac", "nations-ntp", "src-ntpTest_t-src_train.db.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_tfalse_c55.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");

            for (String theory : Sugar.list("src-ntpTest_t-src_train.db.uni0.4.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c55.fol.poss.k5.CS.mc_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support",
                    "src-ntpTest_t-src_train.db.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c55.fol.poss.k5.CS.mc_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support")) {
                System.out.println(theory);
//        Path predictions = Paths.get("E:\\dev\\pac-resubmit\\", "nations-ntp", "src-ntpTest_t-src_train.db.uni0.4.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c55.fol.poss.k5.CS.mc_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");
//            Path predictions = Paths.get("E:\\dev\\pac-resubmit\\", "nations-ntp", "src-ntpTest_t-src_train.db.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c55.fol.poss.k5.CS.mc_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");
                Path predictions = Paths.get("E:\\dev\\pac-resubmit\\", "nations-ntp", theory, "queries1.db");
                // TODO inference nechat znovu spocitat :))a potom at to dava nejaky rozumny vypis :))
                //presentNPTResult(predictions, queries);
                for (String mode : Sugar.list("randomEpsilon", "lexi", "mean", "randomTies")) {
//                    System.out.println("mode:\t" + mode);
                    u.scoreHits(queries, Sugar.list(predictions), Sugar.list(1, 3, 5, 10), mode);
                }
            }
        }
        System.exit(-74);


        Path queries = Paths.get("..", "datasets", "kinships-ntp", "ntp-test.txt");
//        predictions = Paths.get(".", "pac", "kinships-ntp", "src-ntpTest_t-src_train.db.uni0.4.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c25.fol.poss.k5.CS.mc_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");
        Path predictions = Paths.get(".", "pac", "kinships-ntp", "src-ntpTest_t-src_train.db.uni0.4.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c25.fol.poss.k5.CS.mc_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");
        //        Path predictions = Paths.get(".", "pac", "nations-ntp", "src-ntpTest_t-src_train.db.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_tfalse_c55.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support", "queries1.db");
        presentNPTResult(predictions, queries);


        System.exit(-75);
 /**/


        System.out.println("nektery predikaty nejsou v trainu vubec (je pouze par literalu z nich v testu, takze mozna i v datech vubec) -- facutly_emeritus (uwcs), a potom dalsi v nations, kinships (term24, term25), nations, umls");

        boolean typed = true;
//        String domain = "protein";

        System.out.println("kouknout se jak funguje skorovani accuracy, nejlepe na nejakem testu");


        //Path train = Paths.get("..", "datasets", domain, "train.symmetric.db" + (typed ? ".typed" : ""));
//        Path train = Paths.get("..", "datasets", domain, "subsampledSnowballK5.train.symmetric.db.typed");
        //Path train = Paths.get("..", "datasets", domain, "train.symmetric.db.typed");
        /*
        Path train = Paths.get("..", "datasets", domain, "train.symmetric.db.typed");
        Path types = typed ? Paths.get(train.getParent().toString(), "typing.txt") : null;
        Path workingDir = Paths.get(".", "pac", domain);
        run(train, types, workingDir, domain);
        */

        System.setProperty("pipeline.subsampledDataset", "0.5");
//        System.setProperty("pipeline.subsampledDataset", "0.85"); // for IJCAI

        System.setProperty("pipeline.bestN", "5");
        System.setProperty("pipeline.k", "5");


        //interpolovat ty vysledky:))hodit to do cumsumD funkce


//        for (int i = 6; i < 70; i++) {
        //for (int i = 0; i < 50; i++) {


        //for (String depth : Sugar.list("2", "3", "4")) { // setting to learn theories and store time requreiments
        for (Integer depth : Sugar.list(3
                //        3,, 4
        )) {

            //System.out.println("tenhle beh je nastaveny na UMLS evd=500 abychom moli ukazat KWC na tom :))"); not anymore
            for (Integer mod : Sugar.list(10)) {
                System.setProperty("inference.modulo", "" + mod);
                for (Integer i : Sugar.list(10//,10//3,4,5,6,7,8//1, 5, 8
                )) {

                    System.setProperty("infer", "" + true);
                    System.setProperty("atMost", "" + (100 * i));

                    for (String domain : Sugar.list(
                            //"uwcs"
                            //"nations"
                            //"uwcs","kinships", "umls", "nations", "protein"
//                    "kinships"
                            //"uwcs", "kinships", "umls", "nationsA2", "protein", "nations"
//                    "kinships"
                            //"kinships", "nationsA2", "nations", "umls" //"uwcs", // "protein",  ,
                            "protein", "uwcs"
//                    "nationsA2"
                            //"nationsA2", "nations", "umls" //"uwcs", // "protein",  ,
                            //"kinships",
                            //"nationsA2"//, "umls"
                            //"kinships"
//                            "nationsA2"
                    )) {
                        for (String entailment : Sugar.list(//"one", "oneS", "PL", "kPL", "PLWC", "kPLWC", "classical", "k", "kPLf", "PLf"
                                //"oneS", "PL", "kPL", "kPLWC", "PLWC" // final setting for IJCAI19
                                // kPLWC dopocitat pouze pro nations 400 a kinships 500
                                //"oneS", "PL", "kPL"//, "kPLWC"//, "kPLWC", "PLWC" // kPLWC takes to much time (most probably derives to much things!"
                                //"oneS"
                                "kPL", "PL", "k"
                                //"oneS", "PL", "kPLWC", "PLWC" // poustim pouze cast, kPL potom samotne
                                //"kPL" // kPL samotne, protoze nejspise umre na umls
//                                "kPL"
                        )) {

                            System.setProperty("pipeline.entailment", entailment);
                            for (String criterion : Sugar.list(//"acc","pacAcc","accSort"
                                    "confidence"
                            )) {
                                System.setProperty("pipeline.criterion", criterion);

                                Path train = Paths.get("..", "datasets", domain, "train" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
                                Path types = typed ? Paths.get(train.getParent().toString(), "typing.txt") : null;
                                Path workingDir = Paths.get(".", "pac", domain);
                                //Path workingDir = Paths.get(".", "pac2", domain);
//                                Path workingDir = Paths.get("E:\\dev\\pac-resubmit", domain);

                                if (!workingDir.toFile().exists()) {
                                    workingDir.toFile().mkdirs();
                                }

                                for (String driven : Sugar.list(//"crossentropy",
                                        //"amieCE"//, "accuracy"
                                        //"amieT"
//                                    "amie"//, "accuracy", "amie", "ICAJ"
                                        "ICAJacc"
                                )) {
                                    System.setProperty("pipeline.driven", driven); // tohle menit pro crossentropy vs aleph-like pristup

                                    if (("uwcs".equals(domain) || "nations".equals(domain)) && driven.contains("amie")) {
                                        continue; // amie cannot learn rules non-binary predicates
                                    }
                                    if ((!"uwcs".equals(domain) && !"protein".equals(domain)) && "ICAJ".equals(driven)) {
                                        continue; // amie cannot learn rules non-binary predicates
                                    }
//                        if ("pacAcc".equals(criterion) && !"kPL".equals(entailment)) {
//                            continue;
//                        }
                                    if ("protein".equals(domain)) {
                                        System.setProperty("pipeline.maxVariables", "4");
                                        System.setProperty("pipeline.depth", "4");
                                        System.setProperty("pipeline.beamSize", "6");
                                        System.setProperty("pipeline.rounds", "7");
                                    } else if ("nations".equals(domain) || "umls".equals(domain) || "kinships".equals(domain) || "nationsA2".equals(domain)) {
                                        System.setProperty("pipeline.depth", "" + depth);
                                        System.out.println("setting depth to\t" + depth);
//                                        System.setProperty("pipeline.depth", "3");
                                        System.setProperty("pipeline.beamSize", "4");
                                        System.setProperty("pipeline.rounds", "5");
                                        System.setProperty("pipeline.maxVariables", "5");
//                            } else if ("nationsA2".equals(domain)) {
//                                System.setProperty("pipeline.depth", "2");
//                                System.out.println("just for trying out now!");
                                    } else {
                                        System.setProperty("pipeline.depth", "4");
                                        System.setProperty("pipeline.maxVariables", "5");
                                        System.setProperty("pipeline.beamSize", "5");
                                        System.setProperty("pipeline.rounds", "5");
                                    }

//                            System.out.println("ted jen test jestli pipeline dela vsechno co ma");
//                            System.setProperty("pipeline.depth", "1");
//                            System.setProperty("pipeline.beamSize", "5");
//                            System.setProperty("pipeline.rounds", "1");

                            /*for (String constraintsPrune : Sugar.list(
                                    //"oneStep", "ultimate"
                                    "oneStep", ""
                            ))*/
                                    //for (String minis : (driven.equals("amie") ? Sugar.list("100", "1") : Sugar.list(""))) {
                                    //for (String minis : (driven.equals("amie") ? Sugar.list("1") : Sugar.list(""))) {
                                    System.out.println("hardcoded for AMIE");
                                    String minis = "1";
                                    System.setProperty("amieMinSupport", minis);
                                    if (minis.equals("1")) {
                                        System.out.println("tenhle pripad vyresit protoze u umls nebo nationsA2 je problem s teorii");
                                    }

                                    String constraintsPrune = "";
                                    System.setProperty("constraintsPrune", constraintsPrune);

                                    System.out.println("\n\n******************** " + domain + "\t" + entailment + "\n\n");
                                    run(train, types, workingDir, domain);
                                    System.out.println("gc1");
//                            Thread.sleep(1 * 1000);
                                    System.gc();
                                    System.out.println("gc2");
//                            Thread.sleep(1 * 1000);
                                    //    }
                                }


                                System.out.println("don't forget to run amie as well ;)");
                            }
                        }
                    }
                }
            }
        }
    }


    private static void uniqueWeights() {
        uniqueWeights(Paths.get("E:\\dev\\pac-resubmit\\kinships\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt"));
        uniqueWeights(Paths.get("E:\\dev\\pac-resubmit\\kinships\\kinships.d3.minis1.AO.pss.T01.mv5.logic.poss.k5.CS"));

        System.exit(-7778);
    }

    private static void uniqueWeights(Path path) {
        PossibilisticTheory pl = PossibilisticTheory.create(path, false);
        Set<Double> s = pl.getSoftRules().stream().map(Pair::getR).collect(Collectors.toSet());
        System.out.println("predictive rules\t" + pl.getSoftRules().size() + "\nunique confidencies\t" + s.size());
    }

    private static void compareTheories() {
        PossibilisticTheory top = PossibilisticTheory.create(Paths.get("E:\\dev\\pac-resubmit\\nationsA2\\nationsA2.d3.minis1.AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt"), false);
        PossibilisticTheory ce = PossibilisticTheory.create(Paths.get("E:\\dev\\pac-resubmit\\nationsA2\\nationsA2.d3.minis1.AO.pss.r5_mv5.logic.poss.k5.CS.ptt"), false);

//        PossibilisticTheory top = PossibilisticTheory.get(Paths.get("E:\\dev\\pac-resubmit\\kinships\\kinships.d3.minis1.AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt"), false);
//        PossibilisticTheory ce = PossibilisticTheory.get(Paths.get("E:\\dev\\pac-resubmit\\kinships\\kinships.d3.minis1.AO.pss.r5_mv5.logic.poss.k5.CS.ptt"), false);


        Set<IsoClauseWrapper> topl = top.allRules().stream().map(IsoClauseWrapper::create).collect(Collectors.toSet());
        Set<IsoClauseWrapper> cel = ce.allRules().stream().map(IsoClauseWrapper::create).collect(Collectors.toSet());

        System.out.println(top.getHardRules().size());

        int inTnotC = 0;
        int inTnotCRec = 0;
        int tRec = 0;
        System.out.println("in TOP, not in CE");
        for (IsoClauseWrapper icw : topl) {
            if (selfRecursive(icw.getOriginalClause())) {
                System.out.println("\t\t" + HornClause.create(icw.getOriginalClause()));
                tRec++;
            }
            if (!cel.contains(icw)) {
                System.out.println(HornClause.create(icw.getOriginalClause()));
                inTnotC++;
                if (selfRecursive(icw.getOriginalClause())) {
                    inTnotCRec++;
                }
            }
        }
        System.out.println("mismatch\t\t" + inTnotC + "\t\tout of\t" + topl.size() + "\n\n");
        System.out.println(tRec);
        System.out.println(inTnotCRec);
        System.out.println("\n\n");

        int inCnotT = 0;
        int inCnotTRec = 0;
        int ceRec = 0;
        System.out.println("in CE, not in TOP");
        for (IsoClauseWrapper icw : cel) {
            if (selfRecursive(icw.getOriginalClause())) {
                System.out.println("\t\t" + HornClause.create(icw.getOriginalClause()));
                ceRec++;
            }
            if (!topl.contains(icw)) {
                System.out.println(HornClause.create(icw.getOriginalClause()));
                inCnotT++;
                if (selfRecursive(icw.getOriginalClause())) {
                    inCnotTRec++;
                }
            }
        }
        System.out.println("mismatch\t\t" + inCnotT + "\t\tout of\t" + cel.size() + "\n\n");
        System.out.println(ceRec);
        System.out.println(inCnotTRec);

        System.exit(-1234);
    }

    private static boolean selfRecursive(Clause originalClause) {
        Set<Literal> pos = LogicUtils.positiveLiterals(originalClause);
        if (pos.isEmpty()) {
            return false;
        }
        Literal posL = Sugar.chooseOne(pos);
        for (Literal literal : originalClause.literals()) {
            if (literal.isNegated() && literal.predicate().equals(posL.predicate())) {
                return true;
            }
        }
        return false;
    }


    private static void speedupTest() {
        //for (int atMost = 0; i < 10; i++) {
        for (Integer atMost : Sugar.list(10, 50, 100, 200, 300, 400, 500, 600, 700)) {

            //for (String dvm : Sugar.list("timeSub_CurOpt5","timeSub_AllOpt5","timeSub_APOAllOpt5")) {
            for (String dvm : Sugar.list("timeSubL_CurOpt5")) {
                System.setProperty("devMod", dvm);

                long start = System.nanoTime();
                Path queries = Paths.get("..", "datasets", "umls", "test-uniform");
                Path rules = Paths.get(".", "pac2", "umlsSpeedupTest", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.mc0.59.ptt");

                Possibilistic theory = Possibilistic.create(rules);
                Map<Pair<Predicate, Integer>, String> typing = TypesInducer.load(Paths.get("..", "datasets", "umls", "typing.txt"));
                //int atMost = 500;
                // chyba je uz pri 10, vyvozuje to mene nez by melo :(
                //int atMost = 290;
                //int atMost =
//        boolean force = false;
                boolean force = false;
//                System.setProperty("devMod", "debugDevTestOpt4");
                //System.setProperty("devMod", "t4Opt4");
//        System.setProperty("devMod", "t4Opt3");
//            System.setProperty("devMod", i + "_toOpt4");
                //System.setProperty("devMod", "splittedFullyAlll_Opt5");
                //System.setProperty("devMod", "opt5fix2_Opt5");
                //System.setProperty("devMod", "uOnlyNewlyAdded_Opt5");
//        System.setProperty("devMod", "opt5fixExtraPruning3_Opt5");
//        System.setProperty("devMod", "debugOptvsOpt5see");
//        System.setProperty("devMod", "holderOpt");
//System.setProperty("devMod", "standard");
//        System.setProperty("devMod", "debugstandard");
                //System.setProperty("devMod", "opt5vsOpt5parmetrizedDev");
//        System.setProperty("devMod", "reference_burteforce");
                //System.setProperty("devMod", "dev_AllOpt5");
                //System.setProperty("devMod", "fixdev_CurOpt5");
//        System.setProperty("devMod", "39debug_AllOpt5");
//        System.setProperty("devMod", "39debug_CurOpt5");
//        System.setProperty("devMod", "test_allprev_AllOpt5");
                //tu chybu musim oddebugovat tim ze si v kazdem kroku necham vypsat co to predikovalo a podle toho, protoze je to na sobe zavisle a nelze to delat od konce
                System.setProperty("ida.pacReasoning.entailment.k", "5");
                System.setProperty("ida.pacReasoning.entailment.mode", "KWC");
                System.setProperty("ida.pacReasoning.entailment.logic", "PL");
                System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
                EntailmentSetting setting = EntailmentSetting.create();

//        v opt4 rozdil na evidenci 280 oproti opt3 --> napsat opt5 from sratch se vsemi triky a vylespenimi a koukat co to pise za vysledky :)) (brute force verzi bych nepsal protoze jen tak nedobehne) nebo zkusit treba jen nektere proriznuti

                //je tam nekde bug, v opt4 je mene minimalnich dukazu u ten hlavy co tam je nez by melo, zjistit proc :)

//        udelat megaOpt verzi, ktera bude inspirovana tim, ze si budu pamatovat ktere predikaty byly posledne zmenene -- jenom pro pravidla s temahle predikatama v tele necham pocitat jejich grounding, to by mohlo pomoc jako kdyz se preskoci v nulte iteraci nektera pravidla
//                musi se to lastChanged rozlisit jestli to bylo ubrane nebo pridane? pomuze to necemu nebo ne?
//        nemusim si vubec pamatovat co bylo vymazano pomoci constraints (diky monotonicite), ale v opt3 chci udelat stejny trik pri pocitani constraints -- tedy vybirat jen ty pro ktere se neco zmenilo


                String mod = System.getProperty("devMod");
                System.out.println(setting.canon());

                System.setProperty("inference.modulo", "10");
                Path outputEvidence = Paths.get(".", "pac2", "umlsSpeedupTest", mod);
                System.out.println("output evidence\t" + outputEvidence);
                java.util.function.Function<Integer, Path> storingLevelByLevel = null;

                System.out.println("should store to\t" + outputEvidence);
                Inference inference = new Inference(theory, null, force, typing);
                inference.inferFolder(queries, outputEvidence, setting, atMost, storingLevelByLevel);

                //porovnat opt5 oproti opt (ten by mel byt rychly a spravne :)) a nebo ho radsi (opt) jeste zkontrolovat

                System.out.println("************pro opt4 jeste kouknout jestli by tam slo udelat to proriznuti na pocitani pouze nekterych supportu :)!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                long time = System.nanoTime() - start;
                System.out.println("time needed\t" + (time / 1_000_000_000));

            }
        }

        System.exit(-1495200);

    }

    private static void comparePredicates() {
        // writes out if some predicates are missing in a subsampled train, i.e. rules can't be learned thereafter
        for (String domain : Sugar.list("nationsA2", "kinships", "umls")) {

            Path wholeTrain = Paths.get("..", "datasets", domain, "train.db.typed");
            Path subsampledTrain = Paths.get(".", "pac2", domain, "train.db.typed.uni0.5.db");
            Path test = Paths.get("..", "datasets", domain, "test.db.typed");
            Set<Pair<String, Integer>> trainPredicates = LogicUtils.predicates(new Clause(LogicUtils.loadEvidence(wholeTrain)));
            Set<Pair<String, Integer>> subsampledPredicates = LogicUtils.predicates(new Clause(LogicUtils.loadEvidence(subsampledTrain)));
            Set<Pair<String, Integer>> testPredicates = LogicUtils.predicates(new Clause(LogicUtils.loadEvidence(test)));

            System.out.println("--------------------------------------");
            if (trainPredicates.equals(subsampledPredicates)) {
                System.out.println(domain + "\t is ok");
            } else {
                Set<Pair<String, Integer>> missing = Sugar.setDifference(trainPredicates, subsampledPredicates);
                System.out.println(domain + "\tmissing in subsampled train\t" + missing.size() + "\tout of\t" + trainPredicates.size() + "\n\t" + missing);
                Counters<Pair<String, Integer>> c = new Counters<>();
                LogicUtils.loadEvidence(wholeTrain).forEach(l -> c.increment(l.getPredicate()));
                c.keySet().stream().sorted(Comparator.comparing(Object::toString))
                        .forEach(stringIntegerPair -> System.out.println("\t" + stringIntegerPair + "\t" + c.get(stringIntegerPair)));

            }
            if (!testPredicates.equals(trainPredicates)) {
                Set<Pair<String, Integer>> missing = Sugar.setDifference(testPredicates, trainPredicates);
                System.out.println("there are no predicates in train with these predicates from test\t" + missing.size() + "\n\t" + missing);
            }
            System.out.println();
        }
        System.exit(-777864);
    }


    private static void predicatesHistogram() {
        Counters<Pair<String, Integer>> c = new Counters<>();

        //Sugar.union(LogicUtils.loadEvidence(Paths.get("..", "datasets", "umls", "train.db")), LogicUtils.loadEvidence(Paths.get("..", "datasets", "kinships", "test.db")))
        LogicUtils.loadEvidence(Paths.get("..", "datasets", "umls", "train.db")).forEach(l -> c.increment(l.getPredicate()));

        for (Pair<String, Integer> stringIntegerPair : c.keySet()) {
            System.out.println(stringIntegerPair + "\t" + c.get(stringIntegerPair));
        }


        System.exit(-17447);
    }

    private static void showPR() {
        Utils u = Utils.create();

        /* * /
        String domain = "kinships";
//        String minis = "100";
        String minis = "1";
        /* * / // submitted kinships
        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/** /
        // just AMIE things!
        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis1.AO.pss.T01.mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                ,new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis1.AO.pss.T50.mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );*/

        /* * /
        String domain = "umls";
        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/

        /* old impl, bug, paper experiments
        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/

        /* */
        System.out.println("udelat nations");

        String domain = "nationsA2";
        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis1.AO.pss.T01.mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis1.AO.pss.T50.mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );


        /* * / //fixed bug
        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/
        /*  * / //paper epxeriments with bugs
        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/


        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        String res = u.plotPRs(groundEvidence, paths, 100, 400);
        System.out.println(res);

        System.exit(-1845);
    }

//
//    old paths to paper results
//    private static void showPR() {
//        Utils u = Utils.get();
//
//        /*String domain = "kinships";
//        List<Pair<Path, Integer>> paths = Sugar.list(
//                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//        );*/
//
//        /**/
//        String domain = "umls";
//        List<Pair<Path, Integer>> paths = Sugar.list(
//                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//        );/**/
//
//        /*String domain = "nationsA2";
//        List<Pair<Path, Integer>> paths = Sugar.list(
//                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
////                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
////                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
////                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
////                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//        );*/
//
//
//        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
//        String res = u.plotPRs(groundEvidence, paths, 100);
//        System.out.println(res);
//
//        System.exit(-1845);
//    }

    private static void presentNPTResult(Path predictions, Path queriesPath) {
        Utils u = Utils.create();
        Map<Literal, Set<Literal>> queries = u.loadHitsTest(queriesPath);
        VECollector predicted = VECollector.load(predictions, 0);
        Map<Literal, Integer> ranking = u.loadRanks(predicted);

        /*System.out.println("debug ranking");
        ranking.entrySet().forEach(e -> System.out.println(e.getValue() + "\t" + e.getKey()));
        System.out.println("debug ranking");*/

        Random random = new Random();
        List<Double> hits = u.computeRanks(queries, ranking, random);

        System.out.println("MRR\t" + (hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size()));
        for (Integer hit : Sugar.list(1, 3, 5, 10, 20, 30, 40, 50, 100)) {
            System.out.println("hits " + hit + "\t" + (hits.stream().filter(val -> val <= hit).count() * 1.0 / hits.size()));
        }
//        System.out.println("hits 1\t" + (hits.stream().filter(val -> val == 1).count() * 1.0 / hits.size()));
//        System.out.println("hits 3\t" + (hits.stream().filter(val -> val <= 3).count() * 1.0 / hits.size()));
//        System.out.println("hits 5\t" + (hits.stream().filter(val -> val <= 5).count() * 1.0 / hits.size()));
//        System.out.println("hits 10\t" + (hits.stream().filter(val -> val <= 10).count() * 1.0 / hits.size()));
//        System.out.println("hits 10\t" + (hits.stream().filter(val -> val <= 10).count() * 1.0 / hits.size()));
        System.out.println(hits);
    }

    private static void runNTPcomparison(String domain, String maxDepth, String subsampledPsi) throws IOException, InterruptedException {
        System.out.println("ntp domain\t" + domain);

        Path trainData = Paths.get("..", "datasets", domain, "train.db");
        Path workingDir = Paths.get(".", "pac", domain);
        if (!workingDir.toFile().exists()) {
            workingDir.toFile().mkdirs();
        }

        System.out.println("** constraints learning or loading");
        Set<Clause> constraints = learnConstraints(trainData, null, workingDir);
        System.out.println("** constraints learned or loaded");

        double psi = Double.parseDouble(System.getProperty("pipeline.subsampledDataset", subsampledPsi));
        boolean rangeRestrictedOnly = true;
        SubsampledDataset dataset = datasetSubsampling(trainData, workingDir, psi, rangeRestrictedOnly);

        // horn clause learning
        int bestN = Integer.parseInt(System.getProperty("pipeline.bestN", "5"));
        int beamSize = Integer.parseInt(System.getProperty("pipeline.beamSize", "4"));
        int depth = Integer.parseInt(System.getProperty("pipeline.depth", maxDepth));
        int rounds = Integer.parseInt(System.getProperty("pipeline.rounds", "5"));
        int maxVariables = Integer.parseInt(System.getProperty("pipeline.maxVariables", "5"));
        boolean crossentropyDriven = "crossentropy".equals(System.getProperty("pipeline.driven"));
        int k = 5;

        System.out.println("** theory learning");

        String driven = System.getProperty("pipeline.driven");
        Path theorySource = null;
        // what is this null != null? typing?
        if ("crossentropy".equals(driven)) {
            theorySource = Paths.get(workingDir.toString(), "src_" + dataset.getSrc().getFileName().getFileName() +
                    "_cD" + crossentropyDriven + "_theory_r" + rounds + "_bs" + beamSize + "_d" + depth
                    + "_mv" + maxVariables + "_t" + (null != null) + "_c" + constraints.size() + ".fol");
            System.setProperty("domain", domain);
            runHornLearner(null, constraints, dataset, beamSize, depth, rounds, maxVariables, theorySource, crossentropyDriven);

//            if (true) {
//                System.out.println("ends runNTPComparison now!");
//                return;
//            }
        } else if ("amie".equals(driven)) {
            theorySource = Paths.get(workingDir.toString(), "src_" + dataset.getSrc().getFileName().getFileName() +
                    "_cD" + crossentropyDriven + "_theory_r" + rounds + "_bs" + beamSize + "_d" + depth
                    + "_mv" + maxVariables + "_t" + (null != null) + "_c" + constraints.size() + ".fol");
            Path amieRules = Paths.get(workingDir.toString(), domain + ".train.db.amieOutput.postprocess");
            theorySource = amieTheory(amieRules, amieRules, rounds, maxVariables, depth, dataset, constraints, null, k);

        } else {
            System.out.println("unknown driving setting");
            throw new IllegalStateException();
        }

        System.out.println("** theory learned or loaded");

        if (!theorySource.toString().endsWith(".poss")) {
            theorySource = Paths.get(theorySource.toString() + ".poss");
        }
        theorySource = confidenceSort(theorySource, dataset, k, constraints, 0.1);


        // adding or removing constraints
        if (!theorySource.toString().endsWith(".mc")) { // what does mean '.mc'? some kind of constraints prunning
            Utils u = Utils.create();
            String dom = domain.substring(0, domain.length() - "-ntp".length());
            // since nations-ntp is binar-arity only
            Set<Clause> cnstr = u.loadClauses(Paths.get("..", "datasets", "nations".equals(dom) ? "nationsA2" : dom, "train.db.typed_2_4.constraints"));
            cnstr = cnstr.stream().map(c -> LogicUtils.untype(c)).collect(Collectors.toSet());

            Set<Clause> finalConstraints = Sugar.set();
            Matching world = Matching.create(LogicUtils.loadEvidence(trainData), Matching.THETA_SUBSUMPTION);
            for (Clause clause : cnstr) {
                if (!LogicUtils.isConstraint(clause) || !world.subsumption(LogicUtils.flipSigns(clause), 0)) {
                    finalConstraints.add(clause);
                }
            }

            Files.write(Paths.get(theorySource.toString() + ".mc"), Sugar.list(Possibilistic.create(finalConstraints, Possibilistic.create(theorySource).getSoftRules()).asOutput()));
            theorySource = Paths.get(theorySource.toString() + ".mc");
        }


        // end of removing constraints


        String entailment = System.getProperty("entailment");

        Possibilistic pl = Possibilistic.create(theorySource);
        Theory theory = null;
        if ("PL".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "a");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("PLWC".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "awc");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
//            System.setProperty("ida.pacReasoning.entailment.algorithm", "serachBased");
            theory = pl;
        } else if ("kPL".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "k");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("kPLWC".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "kWC");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("oneS".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "oneS");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else {
            System.out.println("entailment");
            throw new IllegalStateException();//NotImplementedException();
        }

        Path testQueriesFolder = Paths.get("..", "datasets", domain, "ntpTest");
        List<Path> predictions = runInference(theory, testQueriesFolder, null, domain);
        System.out.println("the results are stored in");
        predictions.forEach(p -> System.out.println("\t" + p));

        System.out.println("TODO: compute hits from the entailed evidence a compare to... a part of it is already impelmented");
    }

    // quick stats about train and test data
    private static void dataSize() {
        System.out.println("domain\t\t overall datasets \t\ttrain #lit\ttrain #cons\ttest #lit\ttest#cons");
        for (String domain : Sugar.list("protein", "uwcs", "nations", "nationsA2", "kinships", "umls", "nations-ntp", "kinships-ntp", "umls-ntp")) {
            Path dir = Paths.get("..", "datasets", domain);
            Path trainPath = Paths.get(dir.toString(), "train" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
            Path testPath = Paths.get(dir.toString(), "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
            if (Sugar.set("nations-ntp", "kinships-ntp", "umls-ntp").contains(domain)) {
                trainPath = Paths.get(dir.toString(), "train.db");
                testPath = null;
            }
            Set<Literal> train = LogicUtils.loadEvidence(trainPath);
            Set<Literal> test = null == testPath ? Sugar.set() : LogicUtils.loadEvidence(testPath);

            String trainLitSize = train.size() + "";
            String trainConstants = LogicUtils.constants(train).size() + "";

            String testLitSize = "X";
            String testConstants = "X";
            String allLiterals = "X";
            String allConstants = "X";
            if (Sugar.set("nations", "nationsA2", "kinships", "umls", "protein", "uwcs").contains(domain)) {
                testLitSize = test.size() + "";
                testConstants = LogicUtils.constants(test).size() + "";
            }
            if (Sugar.set("nations", "kinships", "umls").contains(domain)) {
                Set<Literal> all = LogicUtils.loadEvidence(Paths.get(dir.toString(), domain + ".txt.db"));
                allLiterals = all.size() + "";
                allConstants = LogicUtils.constants(all).size() + "";
            }

            System.out.println(domain + "\t\t"
                    + allLiterals + "\t"
                    + allConstants + "\t"
                    + trainLitSize + "\t"
                    + trainConstants + "\t"
                    + testLitSize + "\t"
                    + testConstants
            );
        }
        System.exit(123);
    }

    private static void checkConstraints() throws IOException, InterruptedException {
        Utils u = Utils.create();
        //for (String domain : Sugar.list("nationsA2", "umls", "kinships")) {
        {
//            String domain = "umls";
            //Path srcK = Paths.get(".", "pac", "umls", "umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt");
//            Path srcK = Paths.get(".", "pac", "umls", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt");

            String domain = "kinships";
//            Path srcK = Paths.get(".", "pac", "kinships", "kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt");
            Path srcK = Paths.get(".", "pac", "kinships", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt");

//                        String domain = "nationsA2";
//            Path srcK = Paths.get(".", "pac", "nationsA2", "nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt");
//            Path srcK = Paths.get(".", "pac", "nationsA2", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt");


            System.out.println("domain\t" + domain);
            Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "test.db.typed"));
            //Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "train.db.typed"));
            //Set<Clause> rules = u.loadClauses(Paths.get("..", "datasets", domain, "train.db.typed_2_4.constraints"));

            for (String suffix : Sugar.list(".UP", ".OP", "")) {
                Path src = Paths.get(srcK.toString() + suffix);

                Set<Clause> constriants = LogicUtils.constraints(Possibilistic.create(src).getHardRules());
                Matching world = Matching.create(new Clause(evidence), Matching.THETA_SUBSUMPTION);
                int violatedConstraint = 0;
                for (Clause clause : constriants) {
                    if (world.subsumption(LogicUtils.flipSigns(clause), 0)) {
//                        System.out.println("\tconstraint violated\t" + clause);
                        violatedConstraint++;
                    }
                }
                System.out.println(src + "\n\t" + violatedConstraint + "\t / \t" + constriants.size());
                System.out.println("\n\n");
            }
        }
    }

    private static void checkConstraintsPrintOut() throws IOException, InterruptedException {
        Utils u = Utils.create();
        if (true) { // checking learned train constraints on test data
            for (String domain : Sugar.list("nationsA2", "umls", "kinships")) {
                System.out.println("domain\t" + domain);
                //Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "test.db.typed"));
                Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "train.db.typed"));
                Set<Clause> rules = u.loadClauses(Paths.get("..", "datasets", domain, "train.db.typed_2_4.constraints"));
                Matching world = Matching.create(new Clause(evidence), Matching.THETA_SUBSUMPTION);
//                int violatedConstraint = 0;
                for (Clause clause : LogicUtils.constraints(rules)) {
                    if (world.subsumption(LogicUtils.flipSigns(clause), 0)) {
                        System.out.println("\tconstraint violated\t" + clause);
//                        violatedConstraint++;
                    }
                }
                System.out.println("\n\n");
            }
        }
        /*
        // learning part
        for (String domain : Sugar.list("nationsA2", "kinship", "umls")) {
            Path input = Paths.get("..", "datasets", domain, "test.db.typed");
            Path oneLine = Paths.get(input.toString() + ".oneLine");
            if (!oneLine.toFile().exists()) {
                Files.write(oneLine, Sugar.list("+ " + LogicUtils.loadEvidence(input).stream().map(Object::toString).collect(Collectors.joining(", "))));
            }
            Set<Clause> constraints = learnConstraints(input, Paths.get("..", "datasets", domain, "typing.txt"), Paths.get("..", "datasets", domain));
        }

        for (String domain : Sugar.list("nationsA2", "kinship", "umls")) {
            comming undone -- here I wanted to compare train and test learned constraints
        }*/
    }


    private static void plotIJCAI17Comparison() {
        boolean displayDifference = true;
//        plotIJCAI17UWCSE(displayDifference);
        plotIJCAI17Protein(displayDifference);
        System.exit(-123456789);
    }

    private static void plotIJCAI17Protein(boolean displayDifference) {
        boolean cumulative = true;
        String domain = "protein";
        //Path dir = Paths.get(".", "pac", domain);
        Path dir = Paths.get("E:\\dev\\pac", domain);
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        /*
        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlns"), 5), KCut.K_CUT)
                // PL
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-queries_t-ICAJ.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5), KCut.K_CUT)
                // k-ent
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-queries_t-ICAJ.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5), KCut.K_CUT)
                // kPL
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-queries_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5), KCut.K_CUT)
        );
        */
        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlns"), 5), KCut.K_CUT)
                // PL
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5), KCut.K_CUT)
                // k-ent
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5), KCut.K_CUT)
                // kPL
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5), KCut.K_CUT)
        );
        String entailmentsRes = u.plotXHEX(groundEvidence, paths, cumulative, true, displayDifference);
        System.out.println(entailmentsRes);
        System.out.println("tohle byl IJCAI17 proti MLNs  s PL, k=5, PLk=5");

//        Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", domain, "ICAJ.poss"));
//        Pair<Path, Integer> data = new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5);
//        List<Pair<Pair<Path, Integer>, Cut>> stratifications = Sugar.list(
//                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlns"), 5), KCut.K_CUT)
//        );
//        stratifications.add(new Pair<>(data, VotesThresholdCut.get(Integer.MAX_VALUE)));
//        pl.getSoftRules().stream().map(Pair::getR).distinct().sorted(Comparator.reverseOrder()).forEach(threshold -> stratifications.add(new Pair<>(data, VotesThresholdCut.get(threshold))));
//        String stratificationRes = u.plotXHEX(groundEvidence, stratifications, cumulative, true, displayDifference);
//        System.out.println(stratificationRes);
        System.out.println("tohle byl IJCAI17 PLk=5 s ruznymi cutu");
    }

    private static void plotIJCAI17UWCSE(boolean displayDifference) {
        boolean cummulative = true;
        String domain = "uwcs";
        //Path dir = Paths.get(".", "pac", domain);
        Path dir = Paths.get("E:\\dev\\pac", domain);
//        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlns"), 1), KCut.K_CUT)
                // PL
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                // k-ent
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                // kPL
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                // PL without constraints
                //, new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJWC.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                // kPL without constraints
                //, new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJWC.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
        );
        String entailmentsRes = u.plotXHEX(groundEvidence, paths, cummulative, true, displayDifference);
        System.out.println(entailmentsRes);
        System.out.println("tohle byl IJCAI17 proti MLNs  s PL, k=5, PLk=5");

//        Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", domain, "ICAJ.poss"));
//        Pair<Path, Integer> data = new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5);
//        List<Pair<Pair<Path, Integer>, Cut>> stratifications = Sugar.list(
//                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlns"), 1), KCut.K_CUT)
//        );
//        stratifications.add(new Pair<>(data, VotesThresholdCut.get(Integer.MAX_VALUE)));
//        pl.getSoftRules().stream().map(Pair::getR).distinct().sorted(Comparator.reverseOrder()).forEach(threshold -> stratifications.add(new Pair<>(data, VotesThresholdCut.get(threshold))));
//        String stratificationRes = u.plotXHEX(groundEvidence, stratifications, cummulative, true, displayDifference);
//        System.out.println(stratificationRes);
        //System.out.println("tohle byl IJCAI17 PLk=5 s ruznymi cutu");
//        System.exit(-898);
    }

    private static void compareResults() {
        if (true) {
            cmpr();
            return;
        }
        Path oneShot = Paths.get(".", "pac", "kinships", "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support");
        Path kWC = Paths.get(".", "pac", "kinships", "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support");
        Set<Literal> gt = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "kinships", "test.db")));
        for (int idx = 9; idx < 40; idx++) {
            VECollector os = VECollector.load(Paths.get(oneShot.toString(), "queries" + idx + ".db"), 5).untype();
            VECollector kwc = VECollector.load(Paths.get(kWC.toString(), "queries" + idx + ".db"), 5).untype();

            Set<Literal> query = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "kinships", "test-uniform", "queries" + idx + ".db")));

            Set<Literal> allOS = os.cut(KCut.K_CUT);
            Set<Literal> allKWC = kwc.cut(KCut.K_CUT);

            if (!Sugar.setDifference(allOS, allKWC).isEmpty() || !Sugar.setDifference(allKWC, allOS).isEmpty()) {
                System.out.println("problem at \t" + idx + "\t\t" + allOS.size() + "\t" + allKWC.size());
                System.out.println(allOS);
                System.out.println("\n");
                System.out.println(allKWC);
                break;
            }

        }
    }

    private static void cmpr() {
    }


    private static void compareResults2() {
        Path supportBase = Paths.get(".", "pac", "uwcs", "src-queries_t-ICAJ.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support");
        Path searchBase = Paths.get(".", "pac", "uwcs", "src-queries_t-ICAJ.poss_k-5_em-K_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-alldiffOut");
        for (int idx = 1; idx < 245; idx++) {
            Set<Literal> support = VECollector.load(Paths.get(supportBase.toString(), "queries" + idx + ".db"), 5).cut(KCut.K_CUT);
            Set<Literal> search = LogicUtils.loadEvidence(Paths.get(searchBase.toString(), "queries" + idx + ".db"));
//            Set<Literal> search = VECollector.load(Paths.get(supportBase.toString() + "X", "queries" + idx + ".db"), 5).cut(KCut.K_CUT);

            Set<Literal> diff1 = Sugar.setDifference(support, search);
            Set<Literal> diff2 = Sugar.setDifference(search, support);
            System.out.println(idx + "\t" + search.size() + "\t" + support.size());
            if (!diff1.isEmpty() || !diff2.isEmpty()) {
                System.out.println("problem here\t" + idx);

                System.out.println("are in search, not in support\t" + diff2.size());
                diff2.forEach(l -> System.out.println("\t" + l));

                System.out.println("are in support, not in search\t" + diff1.size());
                diff1.forEach(l -> System.out.println("\t" + l));

                break;
            }

        }
    }

    private static void debugTheories() throws IOException, InterruptedException {
        Set<Clause> constraints = learnConstraints(Paths.get("..", "datasets", "uwcs", "train.db"), null, Paths.get(".", "pac", "uwcs"));

        constraints.forEach(c -> System.out.println("\t" + c));

        System.exit(-1);
        Possibilistic uwcs = Possibilistic.create(Paths.get(".", "pac", "uwcs", "uwcs.poss")).untype();
        Possibilistic icaj = Possibilistic.create(Paths.get(".", "pac", "uwcs", "ICAJuwcse.poss")).untype();

        Set<IsoClauseWrapper> uwcsSet = uwcs.allRules().stream().map(IsoClauseWrapper::create).collect(Collectors.toSet());
        Set<IsoClauseWrapper> icajSet = icaj.allRules().stream().map(IsoClauseWrapper::create).collect(Collectors.toSet());
        Set<IsoClauseWrapper> diff1 = Sugar.setDifference(uwcsSet, icajSet);
        Set<IsoClauseWrapper> diff2 = Sugar.setDifference(icajSet, uwcsSet);

        System.out.println("is in uwcs, not in icaj\t" + diff1.size());
        diff1.forEach(icw -> System.out.println("\t" + icw.getOriginalClause()));
        System.out.println("\n\nis in not uwcs, is in icaj\t" + diff2.size());
        diff2.forEach(icw -> System.out.println("\t" + icw.getOriginalClause()));
    }

    private static void baselinePlot2() {
        Utils u = Utils.create();
        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "umls", "test.db")));

        //     src-queries:t-ICAJuwcse.poss:k-0:em-A:l-PL:is-f:dd-f:cpr-f:scout-f:sat-none:ms-f:alg-support tohle dava stejne vysledky jako ICAJ17 clanek


        String targetFile = "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support";
        //String targetFile =  "src-queries_t-uwcs.untyped.withoutConstraints.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-searchBased";
        Data<Integer, VECollector> predicted = Data.loadResults(Paths.get(".", "pac", "umls", targetFile), 0);
        Data<Integer, VECollector> baseline = Data.loadResults(Paths.get("..", "datasets", "umls", "test-uniform"), 0);
        List<Data<Integer, VECollector>> loaded = Sugar.list(baseline, predicted);

        List<Data<Integer, Integer>> hammings = loaded.stream()
                .map(data -> u.hamming(evidence, u.untype(data), KCut.K_CUT, true)).collect(Collectors.toList());
        List<Data<Integer, Integer>> diffs = IntStream.range(1, loaded.size()).mapToObj(idx -> u.diff(hammings.get(0), hammings.get(idx))).collect(Collectors.toList());

        //hammings.get(0).getData().forEach(d -> System.out.println(d.r + "\t" + d.s));

        //System.exit(-80);
        if (true) {
            diffs = u.cumulate(diffs);
        }

        System.out.println(u.plot(diffs, (true ? "C" : "") + "HED against baseline"));
    }

    private static void baselinePlot() {
        Utils u = Utils.create();
        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "umls", "test.db")));

        //     src-queries:t-ICAJuwcse.poss:k-0:em-A:l-PL:is-f:dd-f:cpr-f:scout-f:sat-none:ms-f:alg-support tohle dava stejne vysledky jako ICAJ17 clanek


        //String targetFile = "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support";
        String targetFile = "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support";
        //String targetFile =  "src-queries_t-uwcs.untyped.withoutConstraints.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-searchBased";
        Data<Integer, VECollector> predicted = Data.loadResults(Paths.get(".", "pac", "umls", targetFile), 0);

        for (Pair<Integer, VECollector> p : predicted.getData()) {
            Set<Literal> tp = Sugar.set();
            Set<Literal> fp = Sugar.set();
            Set<Literal> cut = LogicUtils.untype(p.s.cut(KCut.K_CUT));
            for (Literal literal : cut) {
                if (evidence.contains(literal)) {
                    tp.add(literal);
                } else {
                    fp.add(literal);
                }
            }
            int fragmentSize = u.mask(evidence, LogicUtils.constantsFromLiterals(cut)).size();
            System.out.println(p.r + "\t" + tp.size() + "\t" + fp.size() + "\t\t" + (tp.size() + fp.size() > p.r) + "\t" + fragmentSize + "\t" + (fragmentSize > p.r));
        }
    }

    private static void debugMask() {
        Utils u = Utils.create();
        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "umls", "test.db.typed")));
        Data<Integer, VECollector> data = Data.loadResults(Paths.get(".", "pac", "umls", "maskDebug"), 5);
        Data<Integer, Double> res = u.aucPR(evidence, u.untype(data), true);
        System.out.println(u.plot(Sugar.list(res), "debugMask"));
    }

    private static void debugInference() {
        Map<Pair<Predicate, Integer>, String> types = TypesInducer.load(Paths.get("..", "datasets", "uwcs", "typing.txt"));
        Set<Literal> gt = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "uwcs", "test.db")));
//        Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "uwcs", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3"));
        Possibilistic pl = Possibilistic.create(Paths.get(".", "pac", "uwcs", "ICAJuwcse.poss"));
        Theory theory = pl;
        //Theory theory = Possibilistic.get(Paths.get(".", "pac", "uwcs", "ICAJuwcse.poss"));
//        theory = FOL.get(theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).isEmpty()).collect(Collectors.toSet())
//                , theory.allRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toSet()));
        //pl = pl.untype();
        int k = 5;


        Path evidenceSrc = Paths.get("..", "datasets", "uwcs", "queries", "queries170.db");
        Set<Literal> evidence = LogicUtils.addTyping(LogicUtils.loadEvidence(evidenceSrc), types);

//        List<Literal> debug = Sugar.list("advisedBy(1:person402, 1:person235)", "Faculty_adjunct(1:person235)").stream().map(Literal::parseLiteral).collect(Collectors.toList());
//        for (int idx = 0; idx < pl.getSoftRules().size(); idx++) {
        // for simplicity, do everything without types now :))

//        System.out.println("theory\t" + pl.asOutput());
//        System.out.println("evidence\t" + evidence);
//

//            System.out.println("\n\ntest\t " + idx + " / " + pl.getSoftRules().size() + "\n\n");

        //theory = Possibilistic.get(pl.getHardRules(), pl.getSoftRules().subList(0, idx));
        //theory = Possibilistic.get(pl.getHardRules(), pl.getSoftRules());
        SupportEntailmentInference support = SupportEntailmentInference.create(evidence);
        Entailed supportPredict = support.entails(theory, k);

        StratifiedCollector hits = (StratifiedCollector) supportPredict;
        System.out.println(hits.asOutput());


//            for (Literal literal : debug) {
//                if (hits.getMemory().containsKey(literal)) {
//                    System.out.println("tady na\t" + idx + "\t je\t" + literal);
//                    System.exit(-111);
//                }
//            }

//        System.out.println("output\t" + supportPredict.asOutput().split("\n").length + "\n\n" + supportPredict.asOutput());
        /* */
        System.setProperty("ida.pacReasoning.entailment.k", "" + k);
        System.setProperty("ida.pacReasoning.entailment.mode", "k");
        System.setProperty("ida.pacReasoning.entailment.logic", "PL");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
        System.out.println(EntailmentSetting.create().canon());
        Entailment searchBased = Entailment.create(evidence, EntailmentSetting.create());
        Entailed searchPredict = searchBased.entails(theory, k, true, null, null);
        /**/
/*            if (supportPredict.asOutput().split("\n").length != searchPredict.asOutput().split("\n").length) {
                System.out.println("\n\nkonci na idx\t" + idx);
                System.out.println("\n\n*****************\nsupport predict\t" + supportPredict.asOutput().split("\n").length + "\n" + supportPredict.asOutput() + "\n");
                System.out.println("\n\n**************\nsearch predict\t" + searchPredict.asOutput().split("\n").length + "\n" + searchPredict.asOutput() + "\n");

                break;
            }
*/
//            System.out.println("\n\n*****************\nsupport predict\t" + supportPredict.asOutput().split("\n").length + "\n" + supportPredict.asOutput() + "\n");
//            System.out.println("\n\n**************\nsearch predict\t" + searchPredict.asOutput().split("\n").length + "\n" + searchPredict.asOutput() + "\n");
//        }
    }

    private static void debugFOL2() {
        Map<Pair<Predicate, Integer>, String> types = TypesInducer.load(Paths.get("..", "datasets", "uwcs", "typing.txt"));
        Set<Literal> gt = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "uwcs", "test.db")));
        Possibilistic pl = Possibilistic.create(Paths.get(".", "pac", "uwcs", "ICAJuwcseDebug.poss"));
        Theory theory = FOL.create(pl.getHardRules(), pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toSet()));
        //pl = pl.untype();


        Path evidenceSrc = Paths.get("..", "datasets", "uwcs", "queries", "queries1.db");
        Set<Literal> evidence = LogicUtils.addTyping(LogicUtils.loadEvidence(evidenceSrc), types);

        SupportEntailmentInference support = SupportEntailmentInference.create(evidence);
        Entailed entails = support.entails(theory, 5);
        KECollector hit = (KECollector) entails;

        System.out.println("\n\nreally is entailed\t" + entails.asOutput().split("\n").length + "\n" + hit.getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());
        System.out.println(hit.asOutput());
    }

    private static void debugFOLSupport() {
        Map<Pair<Predicate, Integer>, String> types = TypesInducer.load(Paths.get("..", "datasets", "uwcs", "typing.txt"));
        Set<Literal> gt = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "uwcs", "test.db")));
        Possibilistic pl = Possibilistic.create(Paths.get(".", "pac", "uwcs", "ICAJuwcseDebug.poss"));
        Theory theory = FOL.create(pl.getHardRules(), pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toSet()));
        //pl = pl.untype();
        Set<Clause> constraints = LogicUtils.constraints(theory.allRules());

        //System.out.println("zkusit to ze jen na tenhle male evidenci pustim forward pass a optom na tom az hledam porusene vyjimky (lehce tady staci zmenit teoirii)");
        System.out.println("uz jsem to udelal, problem je v hledani porusenych constraints uvnitr support algoritmue");
        //theory = FOL.get(unAllDiffs(((FOL) theory).getHardRules()), unAllDiffs(((FOL) theory).getImplications()));
//        Set<Literal> evidence = Clause.parse("Pre_Quals(1:person402), publication(3:title108, 1:person402), publication(3:title108, 1:person235)").literals();

        Path evidenceSrc = Paths.get("..", "datasets", "uwcs", "queries", "queries170.db");
        Set<Literal> evidence = LogicUtils.addTyping(LogicUtils.loadEvidence(evidenceSrc), types);

//        theory = FOL.get(Sugar.set(), LogicUtils.definiteRules(theory.allRules()));

        SupportEntailmentInference support = SupportEntailmentInference.create(evidence);
        Entailed entails = support.entails(theory, 5);
        KECollector hit = (KECollector) entails;


        System.out.println("\n\n\nis there still a problem?\t" + hit.asOutput().contains("advisedBy(1:person402, 1:person235)") + "\t" + hit.asOutput().contains("Faculty_adjunct(1:person235)"));

        Matching world = Matching.create(new Clause(Sugar.union(evidence, hit.getEntailed())), Matching.THETA_SUBSUMPTION);
        System.out.println("\n\nlet's check constraints");
        for (Clause clause : constraints) {
            boolean shown = false;
            Pair<Term[], List<Term[]>> sub = world.allSubstitutions(LogicUtils.flipSigns(clause), 0, Integer.MAX_VALUE);
            for (Term[] terms : sub.s) {
                if (!shown) {
                    System.out.println(clause);
                }
                System.out.println("\t" + LogicUtils.substitute(clause, sub.r, terms));
                shown = true;
            }
        }

        System.out.println("\n\n" + hit.asOutput());
    }

    private static void debugFOL() {
        Map<Pair<Predicate, Integer>, String> types = TypesInducer.load(Paths.get("..", "datasets", "uwcs", "typing.txt"));
        Set<Literal> gt = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "uwcs", "test.db")));
        Possibilistic pl = Possibilistic.create(Paths.get(".", "pac", "uwcs", "ICAJuwcseDebug.poss"));
        Theory theory = FOL.create(pl.getHardRules(), pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toSet()));
        //pl = pl.untype();

        //theory = FOL.get(unAllDiffs(((FOL) theory).getHardRules()), unAllDiffs(((FOL) theory).getImplications()));

        for (int ix = 1; ix < 171; ix++) {
            ix = 170;

            Path evidenceSrc = Paths.get("..", "datasets", "uwcs", "queries", "queries" + ix + ".db");
            Set<Literal> evidence = LogicUtils.addTyping(LogicUtils.loadEvidence(evidenceSrc), types);

            SupportEntailmentInference support = SupportEntailmentInference.create(evidence);
            Entailed entails = support.entails(theory, 5);
            KECollector hit = (KECollector) entails;

            System.out.println("\n\nreally is entailed\t" + entails.asOutput().split("\n").length + "\n" + hit.getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());

            System.setProperty("ida.pacReasoning.entailment.k", "5");
            System.setProperty("ida.pacReasoning.entailment.mode", "k");
            System.setProperty("ida.pacReasoning.entailment.logic", "classical");
            System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
            System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
            System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
            System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
            System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
            System.out.println(EntailmentSetting.create().canon());
            Entailment searchBased = Entailment.create(evidence, EntailmentSetting.create());
            Entailed searchPredict = searchBased.entails(theory, 5, true, null, null);

//            System.out.println("\n\nreally is entailed\t" + entails.asOutput().split("\n").length + "\n" + ((KECollector) searchPredict).getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());
//            System.out.println("\n\nreally is entailed\t" + entails.asOutput().split("\n").length + "\n" + hit.getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());

            String s1 = Sugar.union(evidence, hit.getEntailed()).stream().map(Object::toString).sorted().collect(Collectors.joining("\n"));
            String s2 = Sugar.union(((KECollector) searchPredict).getEntailed(), evidence).stream().map(Object::toString).sorted().collect(Collectors.joining("\n"));
            if (!s1.equals(s2)) {
                System.out.println("problem here\t" + ix);
                System.out.println("\n\ns1\n" + s1);
                System.out.println("\n\ns2\n" + s2);
                break;
            }

            //System.out.println("\nsupport s oriznutim pouze na jeden cut\n" + Sugar.union(evidence, hit.getEntailed()).stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
            //System.out.println("\nsearch jako FOL\n" + Sugar.union(((KECollector) searchPredict).getEntailed(), evidence).stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
            //Set<Literal> entails = debugEntails(Sugar.listFromCollections(fol.allRules()), evidence);
//        System.out.println("\n\nreally is entailed\t" + entails.size() + "\n" + entails);

            // natvrdo beru cut bez posledniho pravidla
//        violatedConstraintsFinder(evidence, Sugar.union(pl.getHardRules(), pl.getSoftRules().subList(0, pl.getSoftRules().size() - 1).stream().map(Pair::getS).collect(Collectors.toList())));
        }
    }

    private static void debugFOL3() {
        Map<Pair<Predicate, Integer>, String> types = TypesInducer.load(Paths.get("..", "datasets", "uwcs", "typing.txt"));
        Set<Literal> gt = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "uwcs", "test.db")));
        Possibilistic pl = Possibilistic.create(Paths.get(".", "pac", "uwcs", "ICAJuwcseDebug.poss"));
        Theory theory = FOL.create(pl.getHardRules(), pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toSet()));
        //pl = pl.untype();

        FOL unalldiffedtheory = FOL.create(unAllDiffs(((FOL) theory).getHardRules()), unAllDiffs(((FOL) theory).getImplications()));

        int ix = 170;

        Path evidenceSrc = Paths.get("..", "datasets", "uwcs", "queries", "queries" + ix + ".db");
        Set<Literal> evidence = LogicUtils.addTyping(LogicUtils.loadEvidence(evidenceSrc), types);

        SupportEntailmentInference support = SupportEntailmentInference.create(evidence);
        Entailed entails = support.entails(theory, 5);
        KECollector hit = (KECollector) entails;

        KECollector hit2 = (KECollector) support.entails(unalldiffedtheory, 5);

        System.out.println("\n\nhit1 is entailed\t" + entails.asOutput().split("\n").length + "\n" + hit.getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());
        System.out.println("\n\nhit2 is entailed\t" + entails.asOutput().split("\n").length + "\n" + hit2.getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());


//            System.out.println("\n\nreally is entailed\t" + entails.asOutput().split("\n").length + "\n" + ((KECollector) searchPredict).getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());
//            System.out.println("\n\nreally is entailed\t" + entails.asOutput().split("\n").length + "\n" + hit.getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());

        String s1 = Sugar.union(evidence, hit.getEntailed()).stream().map(Object::toString).sorted().collect(Collectors.joining("\n"));
        String s2 = Sugar.union(evidence, hit2.getEntailed()).stream().map(Object::toString).sorted().collect(Collectors.joining("\n"));
        if (!s1.equals(s2)) {
            System.out.println("problem here\t" + ix);
            System.out.println("\n\ns1\n" + s1);
            System.out.println("\n\ns2\n" + s2);
        }

        //System.out.println("\nsupport s oriznutim pouze na jeden cut\n" + Sugar.union(evidence, hit.getEntailed()).stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
        //System.out.println("\nsearch jako FOL\n" + Sugar.union(((KECollector) searchPredict).getEntailed(), evidence).stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
        //Set<Literal> entails = debugEntails(Sugar.listFromCollections(fol.allRules()), evidence);
//        System.out.println("\n\nreally is entailed\t" + entails.size() + "\n" + entails);

        // natvrdo beru cut bez posledniho pravidla
//        violatedConstraintsFinder(evidence, Sugar.union(pl.getHardRules(), pl.getSoftRules().subList(0, pl.getSoftRules().size() - 1).stream().map(Pair::getS).collect(Collectors.toList())));
    }

    private static Set<Clause> unAllDiffs(Set<Clause> clauses) {
        return clauses.stream().map(c -> {

            return new Clause(c.literals().stream().filter(l -> !l.predicate().equals(SpecialVarargPredicates.ALLDIFF)).collect(Collectors.toList()));
        }).collect(Collectors.toSet());
    }

    private static void debugPLinHand() {
        Map<Pair<Predicate, Integer>, String> types = TypesInducer.load(Paths.get("..", "datasets", "uwcs", "typing.txt"));
        Set<Literal> gt = LogicUtils.untype(LogicUtils.loadEvidence(Paths.get("..", "datasets", "uwcs", "test.db")));
        Possibilistic pl = Possibilistic.create(Paths.get(".", "pac", "uwcs", "ICAJuwcse.poss"));
        //pl = pl.untype();

        Path evidenceSrc = Paths.get("..", "datasets", "uwcs", "queries", "queries170.db");
        Set<Literal> evidence = LogicUtils.addTyping(LogicUtils.loadEvidence(evidenceSrc), types);
//        Set<Literal> evidence = Clause.parse("Pre_Quals(1:person402), publication(3:title108, 1:person402), publication(3:title108, 1:person235)").literals();
//        Set<Literal> evidence = Clause.parse("Pre_Quals(1:person402), publication(3:title108, 1:person402), publication(3:title108, 1:person235), student(1:person402)").literals();

//        System.out.println("support je tady omezen pouze na softRules.size() - 2");
        SupportEntailmentInference support = SupportEntailmentInference.create(evidence);
        Entailed entails = support.entails(pl, 5);
        StratifiedCollector hit = (StratifiedCollector) entails;
        System.out.println("\n\nreally is entailed\t" + entails.asOutput().split("\n").length + "\n" + hit.getMemory().keySet().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());


        /** /
         List<Clause> allRulesSorted = Sugar.listFromCollections(pl.getHardRules());
         pl.getSoftRules().forEach(p -> allRulesSorted.add(p.getS()));
         Set<Literal> entails = debugEntails(allRulesSorted, evidence);
         System.out.println("\n\nreally is entailed\t" + entails.size() + "\n" + entails);
         /**/

//        FOL fol = FOL.get(LogicUtils.constraints(pl.allRules()),
//                Sugar.union(LogicUtils.definiteRules(pl.getHardRules()), pl.getSoftRules().subList(0, pl.getSoftRules().size() - 2).stream().map(Pair::getS).collect(Collectors.toList())));
        System.setProperty("ida.pacReasoning.entailment.k", "5");
        System.setProperty("ida.pacReasoning.entailment.mode", "k");
        System.setProperty("ida.pacReasoning.entailment.logic", "pl");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
        System.out.println(EntailmentSetting.create().canon());
        Entailment searchBased = Entailment.create(evidence, EntailmentSetting.create());
        Entailed searchPredict = searchBased.entails(pl, 5, true, null, null);
        //System.out.println("\n\n**************\nsearch predict\t" + searchPredict.asOutput().split("\n").length + "\n" + searchPredict.asOutput() + "\n");
        System.out.println("\n\nreally is entailed\t" + entails.asOutput().split("\n").length + "\n" + ((KECollector) searchPredict).getEntailed().stream().map(Literal::toString).sorted().collect(Collectors.joining(";")).hashCode());


        System.out.println("uz to neni na jeden cut ale cela PL!");
        System.out.println("\nsupport s oriznutim pouze na jeden cut\n" + Sugar.union(evidence, hit.getMemory().keySet()).stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
        System.out.println("\nsearch jako FOL\n" + Sugar.union(((KECollector) searchPredict).getEntailed(), evidence).stream().map(Object::toString).sorted().collect(Collectors.joining("\n")));
        //Set<Literal> entails = debugEntails(Sugar.listFromCollections(fol.allRules()), evidence);
//        System.out.println("\n\nreally is entailed\t" + entails.size() + "\n" + entails);

        // natvrdo beru cut bez posledniho pravidla
//        violatedConstraintsFinder(evidence, Sugar.union(pl.getHardRules(), pl.getSoftRules().subList(0, pl.getSoftRules().size() - 1).stream().map(Pair::getS).collect(Collectors.toList())));
    }

    private static void violatedConstraintsFinder(Set<Literal> evidence, Set<Clause> clauses) {
        LeastHerbrandModel herbrand = new LeastHerbrandModel();
        Set<Clause> definiteRules = LogicUtils.definiteRules(clauses);
        Set<Clause> constraints = LogicUtils.constraints(clauses);
        Set<Literal> model = herbrand.herbrandModel(definiteRules, evidence);
        System.out.println("model\t" + model.size() + "\t" + model);

        Matching world = Matching.create(new Clause(model), Matching.THETA_SUBSUMPTION);
        for (Clause constraint : constraints) {
            Pair<Term[], List<Term[]>> subts = world.allSubstitutions(LogicUtils.flipSigns(constraint), 0, Integer.MAX_VALUE);
            if (!subts.s.isEmpty()) {
                System.out.println("rule\t" + constraint);
                for (Term[] target : subts.s) {
                    System.out.println("\t" + LogicUtils.substitute(constraint, subts.r, target));
                }
            }
        }
    }

    private static Set<Literal> debugEntails(List<Clause> clauses, Set<Literal> evidence) {
        TheorySolver ts = new TheorySolver();

        int start = 0;
        int end = clauses.size();
        /*
        Set<Literal> solved = ts.solve(clauses, evidence);

        if(null == solved){
            int half = start + (end - start) / 2;
            solved = ts.solve(clauses.subList(0,half),evidence);

            if(null == solved){
                end = half;
            }else {
                start = half;
            }
        }
        */

        for (int i = clauses.size(); i >= 0; i--) {
            //int i = clauses.size() - 2; -> [student(1:person402)]
            List<Clause> sublist = Sugar.listFromCollections(clauses.subList(0, i));
            evidence.forEach(l -> sublist.add(new Clause(l)));
            Set<Literal> solved = ts.solve(sublist, evidence);
            if (null != solved) {
                solved = solved.stream()
                        .filter(l -> !evidence.contains(l))
                        .filter(l -> TheorySimplifier.isGroundLiteralImplied(l, sublist, Matching.THETA_SUBSUMPTION))
                        .collect(Collectors.toSet());
                System.out.println("end in\t" + i + "\t (from\t" + clauses.size() + ")");

                clauses.subList(0, i).forEach(c -> System.out.println("\t" + c));
                return solved;
            }
        }

        return null;
    }

    private static void runICAJ() {
        System.out.println("pustit se search-based pristupem a kouknout na vystup, najit fungujici verzi ktera dava stejny vystul jako ICAJ'17!");
        for (String domain : Sugar.list("uwcs"
                , "protein"
        )) {
//        String domain = "uwcs";
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "uwcs", "uwcs.untyped.withoutConstraints.poss"));
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "uwcs", "uwcse.poss"));
//        Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "uwcs", "uwcs.poss"));
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "uwcs", "uwcseWithMyConstraintsUntyped.poss"));
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "protein", "ICAJ.poss.k5.pac3.2"));
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "protein", "protein.poss"));
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "protein", "ICAJA3.poss"));
//        Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "protein", "ICAJ.poss"));
            Possibilistic pl = Possibilistic.create(Paths.get(".", "pac", domain, "ICAJ.poss"));
//        Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "protein", "src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c1.fol.poss.k5.pac3.pt"));
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "protein", "protein.poss"));
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "uwcs", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3"));
            //Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "uwcs", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3"));
//        Possibilistic pl = Possibilistic.get(Paths.get(".", "pac", "protein", "protein.poss"));
            //pl = pl.untype();
            System.out.println(pl.asOutput());

            System.setProperty("atMost", "uwcs".equals(domain) ? "500" : "2000");
            Theory theory = null;
            boolean typed = true;
            int k = 5;
            for (String entailment : Sugar.list(
                    //"classical", "PL", "k", "kPL"//"classical"//, "k", "kPL"//, "k", "kPL"//, "classical"
                    //        "classical", "PL", "k", "kPL", "one"
                    "kPL", "PL", "k", "PLWC", "kPLWC"
            )) {
                System.setProperty("ida.pacReasoning.entailment.saturation", "none");
                if ("classical".equals(entailment)) {
                    System.setProperty("ida.pacReasoning.entailment.mode", "a");
                    System.setProperty("ida.pacReasoning.entailment.k", "0");
                    System.setProperty("ida.pacReasoning.entailment.logic", "classical");
                    System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
                    theory = FOL.create(LogicUtils.constraints(pl.allRules()),
                            LogicUtils.definiteRules(pl.allRules()),
                            pl.getName());
                } else if ("k".equals(entailment)) {
                    System.setProperty("ida.pacReasoning.entailment.mode", "k");
                    System.setProperty("ida.pacReasoning.entailment.k", "" + k);
                    System.setProperty("ida.pacReasoning.entailment.logic", "classical");
                    System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
//                System.setProperty("ida.pacReasoning.entailment.algorithm", "alldiffOut");
//                System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
//                System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
//                System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
//                System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
//                System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
                    theory = FOL.create(pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() < 1).collect(Collectors.toSet()),
                            Sugar.union(pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toList()), pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toList())),
                            pl.getName());
                } else if ("PL".equals(entailment)) {
                    System.setProperty("ida.pacReasoning.entailment.mode", "a");
                    System.setProperty("ida.pacReasoning.entailment.k", "0");
                    System.setProperty("ida.pacReasoning.entailment.logic", "pl");
                    System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
//                System.setProperty("ida.pacReasoning.entailment.algorithm", "oldTS");
//            System.setProperty("ida.pacReasoning.entailment.algorithm", "searchBased");
//                System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
//                System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
//                System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
//                System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
//                System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
                    theory = pl;
                } else if ("kPL".equals(entailment)) {
                    System.setProperty("ida.pacReasoning.entailment.mode", "k");
                    System.setProperty("ida.pacReasoning.entailment.k", "" + k);
                    System.setProperty("ida.pacReasoning.entailment.logic", "pl");
                    System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
//                System.setProperty("ida.pacReasoning.entailment.algorithm", "oldTS");
//                System.setProperty("ida.pacReasoning.entailment.algorithm", "searchBased");
//                System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
//                System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
//                System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
//                System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
//                System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
                    theory = pl;
                } else if ("one".equals(entailment)) {
                    System.setProperty("ida.pacReasoning.entailment.mode", "one");
                    System.setProperty("ida.pacReasoning.entailment.k", "0");
                    System.setProperty("ida.pacReasoning.entailment.logic", "pl");
                    System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
                    theory = pl;
                } else if ("PLWC".equals(entailment)) {
                    System.setProperty("ida.pacReasoning.entailment.mode", "awc");
                    System.setProperty("ida.pacReasoning.entailment.k", "0");
                    System.setProperty("ida.pacReasoning.entailment.logic", "pl");
                    System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
                } else if ("kPLWC".equals(entailment)) {
                    System.setProperty("ida.pacReasoning.entailment.mode", "kWC");
                    System.setProperty("ida.pacReasoning.entailment.k", "" + k);
                    System.setProperty("ida.pacReasoning.entailment.logic", "pl");
                    System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
                } else {
                    throw new IllegalStateException();
                }

                Path train = Paths.get("..", "datasets", domain, "train" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
                Path types = typed ? Paths.get(train.getParent().toString(), "typing.txt") : null;
                //Path workingDir = Paths.get(".", "pac", domain);
                Path workingDir = Paths.get("E:\\download\\dev\\pac", domain);
                Path testQueriesFolder = Paths.get("..", "datasets", domain, (domain.equals("protein") || domain.equals("uwcs")) ? "queries" : "test-uniform");
                runInference(theory, testQueriesFolder, types, domain);
            }
        }
    }

    private static void presentResultsCuts() {
        String domain = "uwcs";
//        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlns"), 1), KCut.K_CUT)
        );

        //Pair<Path, Integer> data = new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss.k5.pac3.2.untyped_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5);
        //Path theory = Paths.get(".", "pac", domain, "ICAJ.poss.k5.pac3.2");
        //Pair<Path, Integer> data = new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c1.fol.poss.k5.pac3.pt.untyped_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5);
        //Path theory = Paths.get(".", "pac", domain, "src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c6.fol.poss.k5.pac3.pt");

        Pair<Path, Integer> data = new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5);
        Path theory = Paths.get(".", "pac", domain, "ICAJ.poss");
//        Pair<Path, Integer> data = new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5);
//        Path theory = Paths.get(".", "pac", domain, "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt");


        Possibilistic pl = Possibilistic.create(theory);

        paths.add(new Pair<>(data, VotesThresholdCut.create(Integer.MAX_VALUE)));
        pl.getSoftRules().stream().map(Pair::getR).distinct().sorted(Comparator.reverseOrder()).forEach(threshold -> paths.add(new Pair<>(data, VotesThresholdCut.create(threshold))));
        paths.add(new Pair<>(data, KCut.K_CUT));


        String res = u.plotXHED(groundEvidence, paths, true, true);
        System.out.println(res);
    }

    private static void presentResults() {
        String domain = "protein";
//        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlns"), 1), KCut.K_CUT)
/*                // ICAJ classical
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // CE classical
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c6.fol.poss.k5.pac3.pt_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
*/

/* *  /                // ICAJ PL
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // CE   PL
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c6.fol.poss.k5.pac3.pt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
/**/

/*                // ICAJ one chain
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // CE   one chain
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c6.fol.poss.k5.pac3.pt_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
*/

/*                // ICAJ k=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // CE   k=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c6.fol.poss.k5.pac3.pt_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
/**/

/*
                // ICAJ kPL=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // CE   kPL=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c6.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
/**/
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-protein.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)


//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss.k5.pac3.2.untyped_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss.k5.pac3.2.untyped_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss.k5.pac3.2.untyped_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                //, new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss.k5.pac3.2.untyped_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)

                //, new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJA3.poss.untyped_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                //, new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJA3.poss.untyped_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJA3.poss_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJA3.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJA3.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJA3.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                /*
                // ICAJ
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                // CE
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                */
        );
        String res = u.plotXHED(groundEvidence, paths, true, true);
        System.out.println(res);
    }


    private static void presentResultsUWCS2() {
        String domain = "uwcs";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );
        String res = u.plotXHED(groundEvidence, paths.stream().map(p -> new Pair<Pair<Path, Integer>, Cut>(p, KCut.K_CUT)).collect(Collectors.toList()), true, true);
        System.out.println(res);
        System.exit(-10);
    }


    private static void presentResultsKinships_PRAUC00() {
        String domain = "kinships";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );
        String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);
        System.exit(-10);
    }


    private static String presentResultsUWCS_PRAUC() {
        String domain = "uwcs";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Path, Integer>> paths = null;/*Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "queries"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );*/
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "queries"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );

//        String res = u.plotAUCPRInterpolatedOptimized(groundEvidence, paths);
        String res = u.plotAUCPRInterpolated(groundEvidence, paths);
        System.out.println(res);
        System.exit(-101010);
        return res;
    }


    private static String presentResultsUWCS_PRAUCijcai() {
        String domain = "uwcs";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Path, Integer>> paths = null;/*Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "queries"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );*/
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "queries"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, ""), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-q_t-ICAJ.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-q_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-q_t-ICAJWC.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-q_t-ICAJWC.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );

        String res = u.plotAUCPRInterpolated(groundEvidence, paths);
//        String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);
        System.exit(-64534);
        return res;
    }


    private static String presentResultsKinships_PRAUC() {
        String domain = "kinships";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        List<Pair<Path, Integer>> paths = null;/*Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );*/
        /* * /  //vysledky z clanku s experimenty s chybnou implementaci
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/


        /* */ // AMIE-CE 1s with confidence cut
//        String minis = "100";
        String minis = "1";
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis1.AO.pss.T01.mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis1.AO.pss.T50.mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/


        /* * / // AMIE-CE rule selection
//        String minis = "100";
        String minis = "1";
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/


        /* * / // nevim po pravde co je to za verzi ale CEAMIE bych neveril dvakrat
//        String minis = "100";
        String minis = "1";
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.CEAMIE.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.CEAMIE.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.CEAMIE.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-kinships.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/
        String res = u.plotAUCPRInterpolated(groundEvidence, paths);
//        String res = u.plotAUCPRInterpolatedOptimized(groundEvidence,paths);
//        String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);//kdyz se to pocita tak se nesmi odebrat evidence, jinak to pujde zakonite dolu!
        System.out.println("ahoj \t" + "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support");
        //System.exit(-10);
        return res;
    }

    private static String presentResultsKinships_PRAUC_selected() {
        String domain = "kinships";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        List<Pair<Path, Integer>> paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );
        //String res = u.plotAUCPR(groundEvidence, paths, true, true);

        String res = u.plotAUCPRCollectors(groundEvidence
                , paths.stream().map(pair -> Data.loadResultsSubPart(pair.r, pair.s, 300, 320)).collect(Collectors.toList())
                , true, true);
        System.out.println(res);
        //System.exit(-10);
        return res;
    }


    private static void presentResultsKinships() {
        String domain = "kinships";
//        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        /*List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.pac3_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.pac3_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.AS_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.AS_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.pac3_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.AS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.AS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.pac3_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
        );*/

        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
        );
        String res = u.plotXHED(groundEvidence, paths, true, true);
        System.out.println(res);
        System.exit(-3);
    }

    private static void presentResultsNations() {
        String domain = "nationsA2";
//        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        /*List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.pac3_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.pac3_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.pac3_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.AS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.pac3_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.pac3_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
        );*/
        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)

        );
        String res = u.plotXHED(groundEvidence, paths, true, true);
        System.out.println(res);
        System.exit(-1);
    }

    private static String presentResultsNationsA2_PRAUC() {
        String domain = "nationsA2";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        List<Pair<Path, Integer>> paths = null;
        /*Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );*/

        // vysledky s opravenou implementaci
//        String minis = "100";
        String minis = "1";

        /* */ // AMIE 1s with confidence cuts
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis1.AO.pss.T01.mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis1.AO.pss.T50.mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );
        /**/

        /* * / // AMIE-CE rule selection
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis" + minis + ".AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis" + minis + ".AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis" + minis + ".AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType \\\\\\\\\\\\\\\\\\+ "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );
        /**/

        /* // submitnute
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.CEAMIE.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.CEAMIE.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis" + minis + ".AO.pss.r5_mv5.logic.poss.CEAMIE.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType \\\\\\\\\\\\\\\\\\+ "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-nationsA2.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                //TD, new Pair<>(Paths.get("E:\\dev\\pac-resubmit", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );
        /**/

        /*  * / vysledky experimentu y clanku (chyba v impl)
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/

        String res = u.plotAUCPRInterpolated(groundEvidence, paths);
//        String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);
        //System.exit(-10);
        return res;
    }

    private static String presentResultsNations_PRAUC() {
        String domain = "nations";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        List<Pair<Path, Integer>> paths = null;
                /*Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );*/

        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c9309.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );

        String res = u.plotAUCPRInterpolated(groundEvidence, paths);
//        String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);
        //System.exit(-10);
        return res;
    }


    private static void presentResultsUMLS() {
        String domain = "umls";
//        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                /*new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.pac3_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.pac3_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.pac3_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.pac3_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.pac3_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.AS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.pac3_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.AS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                */
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
        );
        String res = u.plotXHED(groundEvidence, paths, true, true);
        System.out.println(res);
        System.exit(-4);
    }

    private static String presentResultsUMLS_PRAUC() {
        String domain = "umls";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Path, Integer>> paths = null;
        if (false) {
            paths = Sugar.list(
                    //new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    /**/
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get(".", "pac", domain, "src-test-uniform_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    /**/);
        }
/* bug, vysledky ze clanku
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );/**/

// opravene vypocty
        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 0)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-umls.d3.minis100.AO.pss.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get(".", "pac", domain, "10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-0_em-Af_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                , new Pair<>(Paths.get("E:\\dev\\pac", domain, "10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt" + theoryType + "_k-5_em-kf_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );

        long s = System.nanoTime();
        String res = u.plotAUCPRInterpolated(groundEvidence, paths);
        s = System.nanoTime() - s;
        System.out.println("time is\t" + s);
        System.out.println(res);
        //System.exit(-10);
        return res;
    }

    private static void presentResultsUWCS() {
        String domain = "uwcs";
//        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed");
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlns"), 1), KCut.K_CUT)
    /*            , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        0), KCut.K_CUT)
      */          /*, new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), VotesThresholdCut.get(0.9999999992032482)//KCut.K_CUT
                )
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), VotesThresholdCut.get(0.49))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac2_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), KCut.K_CUT)*/
/*                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac2_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), VotesThresholdCut.get(0.98))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "t-uwcs.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full"),
                        5), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"),
                        5), KCut.K_CUT)
*/
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
//          -      , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
//             -   , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-supportEachLayer"), 5), KCut.K_CUT)
//
//-                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.untyped.2constraintLen.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.untyped.2constraintLen.poss_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcse.poss_k-5_em-K_l-PL_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-searchBased"), 1), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcse.poss_k-5_em-K_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-searchBased"), 1), KCut.K_CUT)
//-                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcse.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-null"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-oldTS"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-5_em-K_l-PL_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-oldTS"), 5), KCut.K_CUT)


//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-oldTS"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-supportStepByStep"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)

//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-5_em-K_l-PL_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-oldTS"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)

//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-5_em-K_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-searchBased"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)

//-                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcseWithMyConstraintsUntyped.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)

//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-supportX"), 0), KCut.K_CUT)

                // pac3: ok, tyhle jsou stejne krasne
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-supportX"), 0), KCut.K_CUT)
                // stejne, akorat tu je k=5 klassicka logika pouze! search based upocital jenom 140 ale krivky vypadaji stejne
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-5_em-K_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // udelat znovu jak k-logiku, tak kPL udelat
                //, new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-5_em-K_l-PL_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)


                // icajuwcs.poss: PL: taky ok, ale nejsou stejne jako
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-supportX"), 0), KCut.K_CUT)
                // stejne, akorat tu je k=5 klassicka logika pouze! stejne akorat search based neni spocitany cely
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-5_em-K_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // kPL
                //, new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-5_em-K_l-PL_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 0), KCut.K_CUT)
                //, new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJuwcse.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)

                // uwcs.poss: tohle poustet bez typovane evidence ! ale i tak to nefunguje :(
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-uwcs.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-supportX"), 0), KCut.K_CUT)

                // final cut
/*                // icaj poss classical
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // accuracy pac3.pt classical
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // crossentropy pac3.pt classical
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-0_em-A_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
*/
/*                // ICAJ PL
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // accuracy pac3.pt PL
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // crossentropy pac3.pt PL
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
*/
                /*// one chain
                //ICAJ OC
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // accuracy pac3.pt OC
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // crossentropy pac3.pt OC
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-0_em-O_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
*/

                // k
/*                // ICAJ k=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // accuracy pac3.pt k=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // crossentropy pac3.pt k=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-c_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
*/
                // kPL
                /**/                // ICAJ
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // accuracy pac3.pt kPL=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDfalse_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                // crossentropy pac3.pt kPL=5
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(1.0))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999999999999366))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999999999998731))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999999999995559))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999999999965103))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.999999999995178))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999999999777932))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.999999999954254))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999999998445526))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999999998001391))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999986359489649))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.999972718979297))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9999413456722479))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9998949678260188))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9991997567260447))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9991983926750095))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9991902083687987))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.9990397080712536))
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs5_d4_mv5_ttrue_c116.fol.poss.k5.pac3.pt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 0),
                        VotesThresholdCut.create(0.997588357769853))


                /**/
                // debug searchBased
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-ICAJ.poss_k-5_em-K_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-alldiffOut"), 0), KCut.K_CUT)
                /*
                // ICAJ
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                // accuracy pac3.pt
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                // crossentropy pac3.pt
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)

                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, ""), 0), KCut.K_CUT)
*/

                //VotesThresholdCut.get(0.9999999987121041)) // 26 pravidel jako v predchozim cutu teto teorie true (akorat podle jineho serazeni); 26 pravidlo je toto 0.9999999911380493
                /**/
//                new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "mlnsDebug"), 1), KCut.K_CUT)
//                , new Pair<>(new Pair<>(Paths.get(".", "pac", domain, "srcDebug"), 1), KCut.K_CUT)

        );
        String res = u.plotXHED(groundEvidence, paths, true, true);
        System.out.println(res);
    }

    private static String presentResultsProtein_PRAUC() {
        String domain = "protein";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        List<Pair<Path, Integer>> paths = null;/*Sugar.list(
                new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-protein.symmetric.db.typed.amieOutput.postprocess.r7_mv4.logic.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-protein.symmetric.db.typed.amieOutput.postprocess.r7_mv4.logic.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-protein.symmetric.db.typed.amieOutput.postprocess.r7_mv4.logic.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r7_bs6_d4_mv4_ttrue_c1.fol.poss.k5.CS_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r7_bs6_d4_mv4_ttrue_c1.fol.poss.k5.CS_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "src-queries_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r7_bs6_d4_mv4_ttrue_c1.fol.poss.k5.CS_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );*/

        paths = Sugar.list(
                new Pair<>(Paths.get("..", "datasets", domain, "queries"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-protein.symmetric.db.typed.amieOutput.postprocess.r7_mv4.logic.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-protein.symmetric.db.typed.amieOutput.postprocess.r7_mv4.logic.poss.k5.CS.ptt_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-protein.symmetric.db.typed.amieOutput.postprocess.r7_mv4.logic.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r7_bs6_d4_mv4_ttrue_c1.fol.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r7_bs6_d4_mv4_ttrue_c1.fol.poss.k5.CS.ptt_k-0_em-AWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                , new Pair<>(Paths.get(".", "pac", domain, "10src-q_t-src_train.symmetric.db.typed.uni0.5.db_cDtrue_theory_r7_bs6_d4_mv4_ttrue_c1.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
        );


        String res = u.plotAUCPRInterpolated(groundEvidence, paths);
        //String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);
        return res;
    }


    private static void toTurtle() throws IOException {
        Reformatter r = Reformatter.create();
        for (String domain : Sugar.list(//"umls", "protein", "kinships"
                "kinships-ntp", "umls-ntp", "nations-ntp")) {
            //String file = "train.db.typed";
            String file = "train.db";
            if (domain.equals("protein")) {
                file = "train.symmetric.db.typed";
            }
            Path trainData = Paths.get("..", "datasets", domain, file);
            List<String> turtle = r.logicToTurtle(trainData);
            Files.write(Paths.get("..", "datasets", domain, file + ".turtle"), turtle);
        }
    }

    private static void formateBigDatasets() throws IOException {
        Reformatter r = Reformatter.create();
        for (String domain : Sugar.list("kinships")) { //"FB15K-237.2", "WN18RR" // "kinships-ntp", // equeryuation_of
            Path trainData = Paths.get("..", "datasets", domain, "train.txt");
            Path testData = Paths.get("..", "datasets", domain, "test.txt");
            Path validData = Paths.get("..", "datasets", domain, "valid.txt");
            Path dir = Paths.get("..", "datasets", domain + "-c");
            if (!dir.toFile().exists()) {
                dir.toFile().mkdirs();
            }
            r.convertToLrnn20FromBigDataset(trainData, testData, validData, dir);
        }
    }


    private static void generateFragments() throws IOException {
        Reformatter r = Reformatter.create();
        for (String dir : Sugar.list("FB15K-237.2-hitsSampled", "WN18RR-hitsSampled")) { // "FB15K-237.2-hitsSampled", "WN18RR-hitsSampled" "family-hitsSampled"
            Path examples = Paths.get("..", "datasets", dir, "examples.txt");
            Path queries = Paths.get("..", "datasets", dir, "queries.txt");
            r.generateFragments(Paths.get("..", "datasets", dir), examples, queries);
        }
    }

    private static void appendKGrepresentation() {
        Reformatter r = Reformatter.create();
        for (String domain : Sugar.list("umls-ntp-r_1")) { // "kinships-ntp", // equeryuation_of
            Path trainData = Paths.get("..", "datasets", domain, "trainQueries.txt");
            Path devData = Paths.get("..", "datasets", domain, "valQueries.txt");
            Path testData = Paths.get("..", "datasets", domain, "testQueries.txt");
            r.appendKGFromAnonymous(trainData, devData, testData);
        }
    }

    private static void generateHitsDataset() throws IOException {
        Reformatter r = Reformatter.create();
        for (String domain : Sugar.list("kinships-ntp", "umls-ntp")) { // "kinships-ntp", // equeryuation_of
            Path trainData = Paths.get("..", "datasets", domain, "train.db");
            Path devData = Paths.get("..", "datasets", domain, "dev.nl");
            Path testData = Paths.get("..", "datasets", domain, "test.db");
            Path dir = Paths.get("..", "datasets", domain + "-r");
            if (!dir.toFile().exists()) {
                dir.toFile().mkdirs();
            }
            r.convertToLrnn2021(trainData, devData, testData, dir, "r");
        }
    }

    private static void generateBaseEmbeddingTemplate() throws IOException {
        int dimension = 3;
        for (String domain : Sugar.list("kinships-ntp-b_1", "umls-ntp-b_1")) { // "kinships-ntp", // equeryuation_of
            Path exampleData = Paths.get("..", "datasets", domain, "trainEvidence.txt");
            Set<Literal> evd = LogicUtils.loadEvidence(exampleData);
            List<Literal> relations = evd.stream().filter(l -> l.arity() == 1 && l.predicate().equals("re")).collect(Collectors.toList());
            List<Literal> entities = evd.stream().filter(l -> l.arity() == 1 && l.predicate().equals("e")).collect(Collectors.toList());
            // TODO for attributes

            List<String> output = Sugar.list();
            relations.forEach(l -> output.add("{" + dimension + "} er(" + l.get(0) + ") :- " + l + "."));
            entities.forEach(l -> output.add("{" + dimension + "} ee(" + l.get(0) + ") :- " + l + "."));

            Files.write(Paths.get("..", "datasets", domain, "base_embedding.txt"), output);

        }
    }


    private static void typeData() {
        for (String domain : Sugar.list("umls", "kinships", "nations")) {
            Path trainData = Paths.get("..", "datasets", domain, "train.db");
            TypesInducer inducer = TypesInducer.create();
            Path typed = Paths.get("..", "datasets", domain, "typing.txt");
            Map<Pair<Predicate, Integer>, String> simplifiedTyping;
            if (typed.toFile().exists()) {
                simplifiedTyping = TypesInducer.load(typed);
            } else {
                Map<Pair<Predicate, Integer>, Type> types = inducer.rename(inducer.induce(LogicUtils.loadEvidence(trainData)));
                simplifiedTyping = inducer.simplify(types);
                TypesInducer.store(types, typed);
            }

            String variant = "";
            if (trainData.getFileName().startsWith("train")) {
                variant = trainData.getFileName().toString().substring("train".length());
                variant = variant.substring(0, variant.length() - ".db".length());
            } else {
                System.out.println("unknown input naming procedure");
            }

            Reformatter r = Reformatter.create();
            Path typedTrain = r.addTypes(trainData, typed, Sugar.list("train" + variant + ".db", "test" + variant + ".db", "train" + variant + ".db.oneLine"));
            System.out.println("reformatted\n*********************\n");

        }
    }

    private static void debugR() {
        String domain = "uwcs";
        Path train = Paths.get("..", "datasets", domain, "train.db.typed");
        Map<Pair<Predicate, Integer>, String> typing = TypesInducer.load(Paths.get(train.getParent().toString(), "typing.txt"));
        Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\uwcs\\queries\\queries1.db"));
        System.out.println("untyped evidence\t" + evidence);
        evidence = LogicUtils.addTyping(evidence, typing);
        System.out.println("typed evidence\t" + evidence);
        Path theorySource = Paths.get(".", "pac", "uwcs", "icajPredictiveMyConstraints.poss");
        Possibilistic pl = Possibilistic.create(theorySource);

        Set<Clause> definite = pl.allRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toSet());
        FOL fol = FOL.create(pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() < 1).collect(Collectors.toSet()),
                definite);

        System.out.println(evidence.size() + "\t" + evidence);
        System.out.println("theory");
        System.out.println(fol.getHardRules().size() + "\t" + fol.getImplications().size());
        System.out.println();

        REntailment er = REntailment.create(evidence);
        VECollector ret = er.entails(fol);
        System.out.println("ouput is");
        System.out.println(ret.asOutput());
    }

    private static void testMatching() {
        Matching m = Matching.create(Clause.parse("p(a,b)"), Matching.THETA_SUBSUMPTION);
        HornClause hc = HornClause.parse("q(X) <- p(X,Y)");
        System.out.println(hc);

        Pair<Term[], List<Term[]>> substA = m.allSubstitutions(hc.toClause(), 0, Integer.MAX_VALUE);
        System.out.println(substA.s.size() + "\thc.toClause()\t" + hc.toClause());
        for (Term[] img : substA.s) {
            System.out.println(LogicUtils.substitute(hc.toClause(), substA.r, img));
        }

        Pair<Term[], List<Term[]>> substB = m.allSubstitutions(LogicUtils.flipSigns(hc.toClause()), 0, Integer.MAX_VALUE);
        System.out.println(substB.s.size() + "\tLogicUtils.flipSings(hc.toClause())\t" + LogicUtils.flipSigns(hc.toClause()));
        for (Term[] img : substB.s) {
            System.out.println(LogicUtils.substitute(LogicUtils.flipSigns(hc.toClause()), substB.r, img));
        }
    }

    private static void debug() {
        Map<IsoClauseWrapper, String> third = load(Paths.get(".", "debug_", "round3.txt"));
        Map<IsoClauseWrapper, String> fourth = load(Paths.get(".", "debug_", "round4.txt"));
        Set<IsoClauseWrapper> common = Sugar.intersection(third.keySet().stream().filter(icw -> LogicUtils.isRangeRestricted(icw.getOriginalClause())).collect(Collectors.toSet()), fourth.keySet().stream().filter(icw -> LogicUtils.isRangeRestricted(icw.getOriginalClause())).collect(Collectors.toSet()));
        System.out.println("common\t" + common.size());
        for (IsoClauseWrapper isoClauseWrapper : common) {
            System.out.println(isoClauseWrapper.getOriginalClause() + "\t" + third.get(isoClauseWrapper) + "\t" + fourth.get(isoClauseWrapper) + "\t\t" + third.get(isoClauseWrapper).equals(fourth.get(isoClauseWrapper)));
        }
    }

    private static Map<IsoClauseWrapper, String> load(Path path) {
        Map<IsoClauseWrapper, String> map = new HashMap<>();
        try {
            //return
            Files.lines(path).filter(l -> l.startsWith("location") && !l.contains("false"))
                    .map(line -> {
//                        System.out.println(line);
                        String[] splitted = line.trim().split("\t");

                        System.out.println(Arrays.toString(splitted));

                        HornClause hc = HornClause.parse(splitted[0]);
                        //double val = Double.parseDouble(splitted[2]);
                        String val = splitted[1].trim();

                        return new Pair<>(IsoClauseWrapper.create(hc), val);
                    })
                    .forEach(p -> map.put(p.r, p.s));
            //          .collect(Collectors.toMap(Pair::getR, Pair::getS));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
        //throw new IllegalStateException();
    }

    private static void checkSupport() {
        Path src = Paths.get(".", "pac", "protein", "protein.ICAJ.typed.poss");
        Possibilistic pl = Possibilistic.create(src);
        List<Clause> hornRules = pl.allRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toList());

        Path testQueries = Paths.get("..", "datasets", "protein", "test.symmetric.db.typed.QueriesEvidence.db");
        Set<Literal> evidence = LogicUtils.loadEvidence(testQueries);

        System.out.println("horn rules\t" + hornRules.size() + "\t" + hornRules);
        System.out.println("evidence\t" + evidence.size() + "\t"); // + evidence
        LeastHerbrandModel herbrand = new LeastHerbrandModel();
        Set<Literal> herbrandLiterals = herbrand.herbrandModel(hornRules, evidence);
        Set<Literal> diff = Sugar.setDifference(herbrandLiterals, evidence);
        System.out.println("entailed\t" + diff.size()); // + "\t" + diff

    }

    private static void retypeTheory() throws IOException {
        Path src = Paths.get(".", "pac", "protein", "ICAJ.poss");
        Possibilistic theory = Possibilistic.create(src);
        Map<Pair<Predicate, Integer>, String> typing = TypesInducer.load(Paths.get("..", "datasets", "protein", "typing.txt"));
        theory = theory.addTyping(typing);
        Files.write(src, Sugar.list(theory.asOutput()));
    }

    public static void run(Path trainData, Path typed, Path workingDir, String domain) throws IOException, InterruptedException {
        Path dir = trainData.getParent();
        System.out.println("starting pipeline.run with arguments\n\t" + trainData + "\n\t" + typed);
        if (null != typed && !typed.toFile().exists()) { // typing
            resolveTyping(trainData, typed, workingDir, domain);
            return;
        }

        if (!TypesInducer.typingSubsetEq(LogicUtils.loadEvidence(trainData), TypesInducer.load(typed))) {
            System.out.println("typing is not the same for types and data");
            throw new IllegalStateException();
        }

        // constraint loading / learning
        Set<Clause> constraints = learnConstraints(trainData, typed, dir);


        if (!TypesInducer.typingSubsetEq(constraints.stream().flatMap(c -> c.literals().stream()).collect(Collectors.toList()), TypesInducer.load(typed))) {
            System.out.println("typing is not the same for constraints and types");
            throw new IllegalStateException();
        }


        // dataset sampling / loading
        double psi = Double.parseDouble(System.getProperty("pipeline.subsampledDataset", "0.5"));
        boolean rangeRestrictedOnly = true;
        SubsampledDataset dataset = datasetSubsampling(trainData, workingDir, psi, rangeRestrictedOnly);

        // horn clause learning
        System.out.println("needs to add sub-sampled datasets here");
        System.out.println("tady je potreba pridat nasamplovany dataset pro trenovani a udelat uceni horn clauses");
        int bestN = Integer.parseInt(System.getProperty("pipeline.bestN", "5"));
        int beamSize = Integer.parseInt(System.getProperty("pipeline.beamSize", "5"));
        int depth = Integer.parseInt(System.getProperty("pipeline.depth", "4"));
        int rounds = Integer.parseInt(System.getProperty("pipeline.rounds", "5"));
        int maxVariables = Integer.parseInt(System.getProperty("pipeline.maxVariables", "5"));
        boolean crossentropyDriven = "crossentropy".equals(System.getProperty("pipeline.driven"));
        int k = Integer.parseInt(System.getProperty("pipeline.k"));

        //Path theorySource = Paths.get(workingDir.toString(), "theory_bn" + bestN + "_bs" + beamSize + "_d" + depth + "_c" + constraints.size() + ".theory");
//        Path theorySource = Paths.get(".", "pac", "uwcs", "icajPredictiveMyConstraints.poss");
        //Path theorySource = Paths.get(".", "pac", "protein", "protein.ICAJ.typed.poss");
        //System.out.println("beru teorii z... muze byt i PL z ICAJ a podobne :))");
        //Path theorySource = Paths.get(".", "pac", "protein", "amie.poss");
        //Path amieRules = Paths.get("C:\\data\\school\\development\\amie\\protein.symmetric.txt");
        //System.out.println("beru teorii z... amie");

        //co se vypisuje, saturovane nebo nesarunovane?

        System.out.println("getting the right rules");
        String driven = System.getProperty("pipeline.driven");
        boolean runAmie = "amie".equals(driven);
        Path theorySource = null;
        if (runAmie) {
            System.out.println("amie");
            //theorySource = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed.amieOutput.postprocess");
            //Path amieRules = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed.amieOutput.postprocess");
            String minis = System.getProperty("amieMinSupport");
            theorySource = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".d" + depth + ".minis" + minis + ".AO.pss");
            Path amieRules = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".d" + depth + ".minis" + minis + ".AO.pss");
            if (!theorySource.toFile().exists()) {
                System.out.println("wannaB\t" + theorySource);
                System.exit(51);
            }
            System.out.println("*calling Pipeline.amieTheorySource");
            theorySource = amieTheory(theorySource, amieRules, rounds, maxVariables, depth, dataset, constraints, typed, k);
            System.out.println("*Pipeline.amieTheorySource finished");
        } else if ("amieCE".equals(driven)) {
            System.out.println("amie CE rule selection ");
            //theorySource = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed.amieOutput.postprocess");
            //Path amieRules = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed.amieOutput.postprocess");
            String minis = System.getProperty("amieMinSupport");
            theorySource = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".d" + depth + ".minis" + minis + ".AO.pss");
            Path amieRules = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".d" + depth + ".minis" + minis + ".AO.pss");
            if (!theorySource.toFile().exists()) {
                System.out.println("wannaB\t" + theorySource);
                System.exit(52);
            }
            System.out.println("*calling Pipeline.amieTheorySourceCE");
            theorySource = amieTheoryCE(theorySource, amieRules, rounds, maxVariables, depth, dataset, constraints, typed, k);
            System.out.println("*Pipeline.amieTheorySourceCE finished");
        } else if ("amieT".equals(driven)) {
            System.out.println("amie CE rule selection ");
            //theorySource = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed.amieOutput.postprocess");
            //Path amieRules = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".db.typed.amieOutput.postprocess");
            String minis = System.getProperty("amieMinSupport");
            theorySource = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".d" + depth + ".minis" + minis + ".AO.pss");
            Path amieRules = Paths.get(workingDir.toString(), domain + ("protein".equals(domain) ? ".symmetric" : "") + ".d" + depth + ".minis" + minis + ".AO.pss");
            if (!theorySource.toFile().exists()) {
                System.out.println("wannaB\t" + theorySource);
                System.exit(52);
            }
            System.out.println("*calling Pipeline.amieTheorySourceT");
            theorySource = amieTheoryT(theorySource, amieRules, maxVariables, depth, dataset, constraints, typed, k);
            System.out.println("*Pipeline.amieTheorySourceT finished");

        } else if ("icaj".equals(driven.toLowerCase())) {
            System.out.println("ijcai");
            theorySource = Paths.get(workingDir.toString(), "ICAJ.poss");
            if (!theorySource.toFile().exists()) {
                System.out.println("**************************** there is no ICAJ.poss theory, thus ending:\t" + theorySource);
                return;
            }
        } else if ("icajacc".equals(driven.toLowerCase())) {
            System.out.println("ICAJ ACC");
            theorySource = Paths.get(workingDir.toString(), "ICAJ.poss");
            Path targetTheorySource = Paths.get(workingDir.toString(), "ICAJ.CS.poss");
            if (!theorySource.toFile().exists()) {
                System.out.println("wannaB\t" + theorySource);
                System.exit(5);
            }
            System.out.println("*calling Pipeline.icajAcc");
            theorySource = theoryCSsort(PossibilisticTheory.create(theorySource, false), targetTheorySource, dataset);
            System.out.println("*Pipeline.icajAccfinished");
        } else {
            System.out.println("hl");
            theorySource = Paths.get(workingDir.toString(), "src_" + dataset.getSrc().getFileName().getFileName() +
                    "_cD" + crossentropyDriven + "_theory_r" + rounds + "_bs" + beamSize + "_d" + depth
                    + "_mv" + maxVariables + "_t" + (null != typed) + "_c" + constraints.size() + ".fol");
            if (!theorySource.toFile().exists()) {
                System.out.println("wannaA\t" + theorySource);
                System.exit(50);
            }
            runHornLearner(typed, constraints, dataset, beamSize, depth, rounds, maxVariables, theorySource, crossentropyDriven);
        }

//        System.out.println("TODO: AMIE LEARNER HERE!"); je udelano v ruce

        // vezmi PAC stratifikaci pokud je potreba
        if ("pacAcc".equals(System.getProperty("pipeline.criterion"))) {
            if (!theorySource.toString().endsWith(".poss")) {
                theorySource = Paths.get(theorySource.toString() + ".poss");
            }
            Path pacWeightedTheory = computePacAcc(theorySource, dataset, k, constraints);
            theorySource = pacWeightedTheory;
        } else if ("accSort".equals(System.getProperty("pipeline.criterion"))) {
            if (!theorySource.toString().endsWith(".poss")) {
                theorySource = Paths.get(theorySource.toString() + ".poss");
            }
            Path pacWeightedTheory = accSort(theorySource, dataset, k, constraints);
            theorySource = pacWeightedTheory;
        } else if ("confidence".equals(System.getProperty("pipeline.criterion"))) {
            if (!theorySource.toString().endsWith(".poss")) {
                theorySource = Paths.get(theorySource.toString() + ".poss");
            }
            System.out.println("confidence sortAnyBURL");
            //theorySource = confidenceSort(theorySource, dataset, k, constraints, 0.5);
            theorySource = confidenceSort(theorySource, dataset, k, constraints, -1);
//            theorySource = confidenceSort(theorySource, dataset, k, constraints, 0.01);
        } else if (!"acc".equals(System.getProperty("pipeline.criterion"))) {
            throw new IllegalStateException();//NotImplementedException();
        }

        System.out.println("brani pruned teorie.. tady se predpoklada ze uz jsou pravidla zasaturovana :))");
        if (false) {//trainData.toString().contains("nations") || trainData.toString().contains("umls") || trainData.toString().contains("kinships")) {
//        if (true) {
            System.out.println("theory prune je vypnuty protoze je narocny");
        } else {
            System.out.println("theory prune");
            System.out.println("zkusme to proriznout at to dela co to dela");
            theorySource = pruneTheory(theorySource);

            String prune = System.getProperty("constraintsPrune");
            if (!trainData.toString().contains("protein")) {
                if (prune.equals("oneStep")) {
                    theorySource = constraintsOnePrune(theorySource, dataset);
                } else if (prune.equals("ultimate")) {
                    theorySource = ultimateConstraintsPrune(theorySource, dataset);
                }
            }
        }

        if ("icaj".equals(driven.toLowerCase())) {
            theorySource = Paths.get(workingDir.toString(), "ICAJ.poss");
        }

//        System.out.println("konec pro debug ted, pouze kouknuti jak to vypada pro prune");
//        if (false) {
//            return;
//        }

        System.out.println("alldiff check probably");
        if (!Boolean.parseBoolean(System.getProperty("infer", "false"))) {
            System.out.println("inference je ted vypnuta, pouze jsem hledal pravidla\t" + !Boolean.parseBoolean(System.getProperty("infer", "false")));
            System.out.println("tak aspon zkontroluju allDiff");
            allDiffCheck(theorySource);
            return;
        }
        System.out.println("inference je zapnuta");
        // inference

        // neni pekny ale pro rychlost to nejde jinak
        String entailment = System.getProperty("pipeline.entailment");
        Possibilistic pl = Possibilistic.create(theorySource);
        Theory theory = null;
        if ("classical".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "a");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "classical");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = FOL.create(pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() < 1).collect(Collectors.toSet()),
                    Sugar.union(pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toList()), pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toList())),
                    pl.getName());
        } else if ("k".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "k");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "classical");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = FOL.create(pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() < 1).collect(Collectors.toSet()),
                    Sugar.union(pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toList()), pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toList())),
                    pl.getName());
        } else if ("PL".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "a");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
//            System.setProperty("ida.pacReasoning.entailment.algorithm", "serachBased");
            theory = pl;
        } else if ("PLWC".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "awc");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
//            System.setProperty("ida.pacReasoning.entailment.algorithm", "serachBased");
            theory = pl;
        } else if ("kPL".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "k");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("kPLWC".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "kWC");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("kPLf".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "kf");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("PLf".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "Af");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("one".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "one");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
            throw new IllegalStateException("I really do not know I got here");
        } else if ("oneS".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "oneS");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("none".equals(entailment)) {
            System.out.println("ending since inference set to none");
            return;
        } else {
            throw new IllegalStateException();//NotImplementedException();
        }

        if (true) {
            System.out.println("skipping inference now :))");
            return;
        }
        System.out.println("inference set");
        Path testQueriesFolder = Paths.get("..", "datasets", domain, (domain.equals("protein") || domain.equals("uwcs")) ? "queries" : "test-uniform");
        List<Path> predictions = runInference(theory, testQueriesFolder, typed, domain);
        System.out.println("the results are stored in");
        predictions.forEach(p -> System.out.println("\t" + p));
/*

                // hits inference
//        System.out.println("tady se musi pridat jeste generovani toho testu pomoci DatasetSampler.sampleHitTest");
        Possibilistic theory = Possibilistic.get(Paths.get(theorySource.toString() + (theorySource.getFileName().toString().endsWith(".poss") ? "" : ".poss")));
        Path hitsEvidence = Paths.get("..", "datasets", "protein", "test.symmetric.db.typed.QueriesEvidence.db");
        Path testQueries = Paths.get("..", "datasets", "protein", "test.symmetric.db.typed.hitsQueries.db");
        System.out.println("theory transformed to FOL, since hard constraints with positive literals are not allowed; constraints only with negative literals");
        List<Clause> stratifiedTheory = Sugar.listFromCollections(theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toList()),
                theory.getSoftRules().stream().map(Pair::getS).collect(Collectors.toList()));
        constraints = theory.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 0).collect(Collectors.toSet());

        System.out.println("debug prediction!");
        System.out.println("constraints");
        constraints.forEach(o -> System.out.println("\t" + o));
        System.out.println("theory");
        stratifiedTheory.forEach(o -> System.out.println("\t" + o));

        if (!TypesInducer.typingSubsetEq(stratifiedTheory.stream().flatMap(c -> c.literals().stream()).collect(Collectors.toList()), TypesInducer.load(typed))) {
            System.out.println("typing is not the same for theory and types");
            throw new IllegalStateException();
        }

        System.exit(-10);

//        Path predictions = runHitInference(constraints, theorySource, k, hitsEvidence, testQueries, stratifiedTheory, typed);
//         presentation of results :))
//        presentResultsUWCS(testQueries, predictions);


        testQueries = Paths.get("..", "datasets", domain, "queries");
        FOL fol = FOL.get(Sugar.set(), Sugar.setFromCollections(stratifiedTheory), theorySource.getFileName().toString() + "wC");
        List<Path> predictions = runInference(fol, testQueries, typed, domain);
        System.out.println("the results are stored in");
        predictions.forEach(p -> System.out.println("\t" + p));
        */
    }

    private static Path constraintsOnePrune(Path theorySource, SubsampledDataset dataset) {
        Path target = Paths.get(theorySource.toString() + ".OP");
        if (target.toFile().exists()) {
            return target;
        }
        Possibilistic pos = Possibilistic.create(theorySource);
        Set<Clause> definiteRules = LogicUtils.definiteRules(pos.getHardRules());
        Set<Clause> hardRules = definiteRules;
        Set<Clause> constraints = LogicUtils.constraints(pos.getHardRules());

        SubsampledDataset d = new SubsampledDataset(Sugar.union(dataset.getEvidence(), dataset.getGoldComplement()), Sugar.set(), Paths.get("."));

        Matching world = Matching.create(new Clause(Sugar.iterable(dataset.getEvidence(), dataset.getGoldComplement())), Matching.THETA_SUBSUMPTION);
        for (Pair<Double, Clause> pair : pos.getSoftRules()) {
            HornClause rule = HornClause.create(pair.s);
            Pair<String, Integer> predicate = rule.head().getPredicate();
            d.setTargetPredicate(predicate);
            Double conditional = pair.r;
            Set<Literal> tp = Sugar.set();
            Set<Literal> fp = Sugar.set();
            Pair<Term[], List<Term[]>> subt = world.allSubstitutions(rule.body(), 0, Integer.MAX_VALUE);
            for (Term[] img : subt.s) {
                Literal substituted = LogicUtils.substitute(rule.head(), subt.r, img);
                if (d.getEvidence().contains(substituted)) {
                    tp.add(substituted);
                } else {
                    fp.add(substituted);
                }
            }

            double baselineCE = d.computeEntropyForPruning(tp, fp, conditional);
            Matching augmentedWorld = Matching.create(new Clause(Sugar.iterable(tp, fp, d.getEvidence())), Matching.THETA_SUBSUMPTION);
            for (Clause constraint : constraints) {
                if (constraint.literals().stream().anyMatch(l -> predicate.equals(l.getPredicate()))) {
                    Clause flipped = LogicUtils.flipSigns(constraint);
                    Set<Literal> toRemove = Sugar.set();
                    Pair<Term[], List<Term[]>> subts = augmentedWorld.allSubstitutions(flipped, 0, Integer.MAX_VALUE);
                    for (Term[] img : subts.s) {
                        LogicUtils.substitute(flipped, subts.r, img).literals().stream()
                                .filter(l -> l.getPredicate().equals(predicate))
                                .forEach(toRemove::add);
                    }
                    double currentCE = d.computeEntropyForPruning(Sugar.setDifference(tp, toRemove), Sugar.setDifference(fp, toRemove), conditional);
                    if (currentCE < baselineCE) {
                        hardRules.add(constraint);
                    }
                }

            }

        }

        System.out.println("---- theory pruned by one step\t" + theorySource + "\n\t" + pos.getHardRules().size() + "\t -> \t" + hardRules.size());
        try {
            Files.write(target, Sugar.list(Possibilistic.create(hardRules, pos.getSoftRules()).asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return target;
    }

    private static Path ultimateConstraintsPrune(Path theorySource, SubsampledDataset dataset) {
        Path target = Paths.get(theorySource.toString() + ".UP");
        if (target.toFile().exists()) {
            return target;
        }
        Possibilistic pos = Possibilistic.create(theorySource);
        LeastHerbrandModel herbrandFinder = new LeastHerbrandModel();
        Set<Clause> definiteRules = LogicUtils.definiteRules(pos.allRules());
        Set<Literal> predictedLiterals = herbrandFinder.herbrandModel(definiteRules, Sugar.union(dataset.getEvidence(), dataset.getGoldComplement()));
        Set<Clause> hardRules = definiteRules;
        Matching world = Matching.create(new Clause(predictedLiterals), Matching.THETA_SUBSUMPTION);

        for (Clause clause : LogicUtils.constraints(pos.getHardRules())) {
            if (world.subsumption(LogicUtils.flipSigns(clause), 0)) {
                System.out.println("2 throwing away\t" + clause);
            } else {
                hardRules.add(clause);
            }
        }

        System.out.println("---- theory pruned ultimatelly\t" + theorySource + "\n\t" + pos.getHardRules().size() + "\t -> \t" + hardRules.size());

        try {
            Files.write(target, Sugar.list(Possibilistic.create(hardRules, pos.getSoftRules()).asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return target;
    }

    private static void allDiffCheck(Path theorySource) {
        Possibilistic pl = Possibilistic.create(theorySource);
        if (pl.getSoftRules().isEmpty()) {
            return;
        }
        long atMostAllDiffPerRule = pl.getSoftRules().stream()
                .mapToLong(p -> p.s.literals().stream()
                        .filter(l -> l.predicate().equals(SpecialVarargPredicates.ALLDIFF))
                        .count())
                .max().orElse(0);
        if (atMostAllDiffPerRule > 1) {
            List<Pair<Double, Clause>> corrected = pl.getSoftRules().stream().map(p -> new Pair<>(p.r, addNegativeAllDiff(LogicUtils.removeSpecialPredicates(p.s)))).collect(Collectors.toList());
            try {
                Files.write(theorySource, Sugar.list(Possibilistic.create(pl.getHardRules(), corrected).asOutput()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static Path confidenceSort(Path theorySource, SubsampledDataset dataset, int k, Set<Clause> constraints, double minConfidence) {
        //Path output = Paths.get(theorySource + ".k" + k + ".CS.mc" + minConfidence + "");
        Path output = Paths.get(theorySource + ".k" + k + ".CS");
        if (output.toFile().exists()) {
            return output;
        }
        System.out.println("\n\ncreating confidency sorted theory for\t" + theorySource + "\n\n");
        Possibilistic theory = Possibilistic.create(theorySource);

        RuleSaturator saturator = RuleSaturator.create(constraints); // for amieT it was run without constraint, e.g. Sugar.list() instead
        List<Triple<Double, IsoClauseWrapper, Clause>> triples = theory.getSoftRules().parallelStream()
                .map(p -> {
                    Clause saturated = saturator.saturate(LogicUtils.removeSpecialPredicates(p.s));
                    if (null == saturated) {
                        System.out.println("this is clashing\t" + p.r + "\t" + HornClause.create(p.s));
                        return null;
                    }
                    return new Triple<>(p.r, IsoClauseWrapper.create(saturated), p.s);
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
        Set<IsoClauseWrapper> alreadyIn = Sugar.set();
        List<Pair<Double, Clause>> pairs = Sugar.list();
        triples.stream().forEach(t -> {
            if (!alreadyIn.contains(t.s)) {
                alreadyIn.add(t.s);
                pairs.add(new Pair<>(t.r, t.t));
            }
        });

        List<Pair<Double, Clause>> accsSort = pairs.stream().sequential().distinct()
                .map(p -> new Pair<>(p.r, addNegativeAllDiff(p.s)))
                .filter(p -> {
                    if (p.r > minConfidence) {
                        return true;
                    }
//                    return true; // removing this right noe TODO a BUG somewhere? why all rules from AMIE has
                    System.out.println("3 throwing away\t" + p.r + "\t" + p.s);
                    return false;
                })
                .sorted(Comparator.comparingDouble(Pair<Double, Clause>::getR).reversed())
                .collect(Collectors.toList());

        Possibilistic reweighted = Possibilistic.create(theory.getHardRules(), accsSort);
        try {
            Files.write(output, Sugar.list(reweighted.asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n\nconfidency sortAnyBURL finished\t" + theorySource + "\n\n");
        return output;
    }

    private static Path accSort(Path theorySource, SubsampledDataset dataset, int k, Set<Clause> constraints) {
        Path output = Paths.get(theorySource + ".k" + k + ".AS");
        if (output.toFile().exists()) {
            return output;
        }
        System.out.println("\n\ncreating accuracy sorted theory for\t" + theorySource + "\n\n");
        Possibilistic theory = Possibilistic.create(theorySource);

        RuleSaturator saturator = RuleSaturator.create(constraints);


        List<Triple<Double, IsoClauseWrapper, Clause>> triples = theory.getSoftRules().parallelStream()
                .map(p -> {
                    Clause saturated = saturator.saturate(LogicUtils.removeSpecialPredicates(p.s));
                    if (null == saturated) {
                        System.out.println("this is clashing\t" + p.r + "\t" + HornClause.create(p.s));
                        return null;
                    }
                    return new Triple<>(p.r, IsoClauseWrapper.create(saturated), p.s);
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
        Set<IsoClauseWrapper> alreadyIn = Sugar.set();
        List<Pair<Double, Clause>> pairs = Sugar.list();
        triples.stream().forEach(t -> {
            if (!alreadyIn.contains(t.s)) {
                alreadyIn.add(t.s);
                pairs.add(new Pair<>(t.r, t.t));
            }
        });


        List<Pair<Double, Clause>> accsSort = pairs.stream().sequential().distinct()
                .map(p -> new Pair<>(p.r, addNegativeAllDiff(p.s)))
                .filter(p -> {
                    if (p.r > 0.5) {
                        return true;
                    }
                    System.out.println("4 throwing away\t" + p.r + "\t" + p.s);
                    return false;
                })
                .sorted(Comparator.comparingDouble(Pair<Double, Clause>::getR).reversed())
                .collect(Collectors.toList());

        Possibilistic reweighted = Possibilistic.create(theory.getHardRules(), accsSort);
        try {
            Files.write(output, Sugar.list(reweighted.asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n\naccuracy sortAnyBURL finished\t" + theorySource + "\n\n");
        return output;
    }

    private static Path pruneTheory(Path theorySource) {
        Path retVal = Paths.get(theorySource.toString() + ".ptt");
        if (retVal.toFile().exists()) {
            return retVal;
        }

        System.out.println("\n\n**************** starting to prune theory\t" + theorySource);
        Possibilistic pl = Possibilistic.create(theorySource);

        Set<Clause> allRules = Sugar.setFromCollections(pl.getHardRules());
        List<Pair<Double, Clause>> softRules = Sugar.list();
        Set<Clause> definiteRules = LogicUtils.definiteRules(pl.getHardRules());
        Set<Clause> constraints = LogicUtils.constraints(pl.getHardRules());

        LeastHerbrandModel herbrandComputer = new LeastHerbrandModel();
        System.out.println("rules at all\t" + pl.getSoftRules().size());
        int c = 0;
        for (Pair<Double, Clause> pair : pl.getSoftRules()) {
            // removing alldiff
            Set<Literal> evidence = Sugar.set();
            Set<Literal> groundNegatedConstraints = Sugar.set();
            LogicUtils.constantizeClause(LogicUtils.removeSpecialPredicates(pair.getS())).literals().forEach(l -> {
                // we are not working here explicitly with negation of the rule
                if (l.isNegated()) {
                    evidence.add(l.negation());
                } else {
                    groundNegatedConstraints.add(l.negation());
                }
            });
            Set<Literal> solved = herbrandComputer.herbrandModel(definiteRules, evidence, constraints, groundNegatedConstraints);
            if (null == solved) {
                System.out.println("this is redundant, throwing it away\t" + pair.r + "\t:\t" + pair.s);
            } else {
                System.out.println("done\t" + c++);
                softRules.add(pair);
                definiteRules.add(pair.s);
            }
        }

        int less = pl.getSoftRules().size() - softRules.size();
        System.out.println("\n\n**************** storing pruned theory source by\t" + less + "\t to \t" + retVal);
        try {
            Files.write(retVal, Sugar.list(Possibilistic.create(pl.getHardRules(), softRules).asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return retVal;
    }

    private static Path pruneTheoryPT(Path theorySource) {
        Path retVal = Paths.get(theorySource.toString() + ".pt");
        if (retVal.toFile().exists()) {
            return retVal;
        }

        System.out.println("\n\n**************** starting to prune theory\t" + theorySource);
        Possibilistic pl = Possibilistic.create(theorySource);

        Set<Clause> allRules = Sugar.setFromCollections(pl.getHardRules());
        List<Pair<Double, Clause>> softRules = Sugar.list();

        System.out.println("rules at all\t" + pl.getSoftRules().size());
        TypedTheorySolver ts = new TypedTheorySolver(TypedTheorySolver.CUTTING_PLANES, Matching.THETA_SUBSUMPTION);
        int c = 0;
        for (Pair<Double, Clause> pair : pl.getSoftRules()) {
            Set<Literal> skolemized = LogicUtils.constantizeClause(new Clause(LogicUtils.flipSigns(pair.s).literals().stream()
                    .filter(l -> !SpecialVarargPredicates.SPECIAL_PREDICATES.contains(l.predicate())).collect(Collectors.toList()))).literals();
            Set<Literal> solved = ts.solve(allRules, skolemized);
            if (null == solved) {
                System.out.println("this is redundant, throwing it away\t" + pair.r + "\t:\t" + pair.s);
            } else {
                System.out.println("done\t" + c++);
                softRules.add(pair);
                allRules.add(pair.s);
            }
        }

        int less = pl.getSoftRules().size() - softRules.size();
        System.out.println("\n\n**************** storing pruned theory source by\t" + less + "\t to \t" + retVal);
        try {
            Files.write(retVal, Sugar.list(Possibilistic.create(pl.getHardRules(), softRules).asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return retVal;
    }

    private static Path computePacAcc(Path theorySource, SubsampledDataset dataset, int k, Set<Clause> constarints) {
        Path output = Paths.get(theorySource + ".k" + k + ".pac3");
        if (output.toFile().exists()) {
            return output;
        }
        System.out.println("\n\nstarting to compute PAC accuracy for\t" + theorySource + "\n\n");
        Possibilistic theory = Possibilistic.create(theorySource);
        RuleSaturator saturator = RuleSaturator.create(constarints);

        List<Triple<Double, IsoClauseWrapper, Clause>> triples = theory.getSoftRules().parallelStream()
                .map(p -> {
                    Clause saturated = saturator.saturate(LogicUtils.removeSpecialPredicates(p.s));
                    if (null == saturated) {
                        System.out.println("this is clashing\t" + p.r + "\t" + HornClause.create(p.s));
                        return null;
                    }
                    return new Triple<>(p.r, IsoClauseWrapper.create(saturated), p.s);
                }).filter(Objects::nonNull)
                .collect(Collectors.toList());
        Set<IsoClauseWrapper> alreadyIn = Sugar.set();
        List<Pair<Double, Clause>> pairs = Sugar.list();
        triples.stream().forEach(t -> {
            if (!alreadyIn.contains(t.s)) {
                alreadyIn.add(t.s);
                pairs.add(new Pair<>(t.r, t.t));
            }
        });

        //PACAccuracyDataset computer = PACAccuracyDataset.get(Sugar.union(dataset.getEvidence(), dataset.getGoldComplement()), k); tohle by bylo pro .pac
        PACAccuracyDataset computer = PACAccuracyDataset.create(dataset.getEvidence(), k);

        List<Pair<Double, Clause>> pacAccs = pairs.stream().sequential().distinct()
                .map(p -> new Pair<>(p.r, addNegativeAllDiff(p.s)))
                .filter(p -> {
                    if (p.r > 0.5) {
                        return true;
                    }
                    System.out.println("5 throwing away\t" + p.r + "\t" + p.s);
                    return false;
                })
                .map(pair -> {
                    System.out.println(pair.s + "\toriginal acc\t" + pair.r);
                    double acc = computer.accuracyApprox(pair.s);
                    System.out.println("\t" + acc);
                    return new Pair<>(acc, pair.s);
                })
//                .map(pair -> new Pair<>(computer.accuracyApprox(pair.s), pair.s))
//                .map(p -> {
//                    System.out.println(p.getS() + "\t" + p.r);
//                    return p;
//                })
                .sorted(Comparator.comparingDouble(Pair<Double, Clause>::getR).reversed())
                .collect(Collectors.toList());

        Possibilistic reweighted = Possibilistic.create(theory.getHardRules(), pacAccs);
        try {
            Files.write(output, Sugar.list(reweighted.asOutput()));
        } catch (
                IOException e) {
            e.printStackTrace();
        }
        System.out.println("\n\nPAC accuracy finished for\t" + theorySource + "\n\n");
        return output;
    }

    private static Clause addNegativeAllDiff(Clause clause) {
        Literal alldiff = new Literal(SpecialVarargPredicates.ALLDIFF, true, Sugar.listFromCollections(LogicUtils.variables(clause)));
        return new Clause(Sugar.iterable(clause.literals(), Sugar.list(alldiff)));
    }

    public static List<Path> runInference(Theory theory, Path queries, Path typed, String domain) {
        Map<Pair<Predicate, Integer>, String> typing = null == typed ? null : TypesInducer.load(typed);
        List<Path> retVal = Sugar.list();
        //int atMost = 500;
        //int atMost = "protein".equals(domain) ? 2000 : 500;
        int atMost = Integer.parseInt(System.getProperty("atMost"));
        EntailmentSetting setting = EntailmentSetting.create();

        String modulo = System.getProperty("inference.modulo", "");
        if (modulo.length() > 0) {
            System.out.println("changing here output display of inference.modulo from\t" + modulo + "\tto\t10");
            modulo = "10";
        }
        String inputDir = queries.toFile().getName();
        //Path outputEvidence = Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + theory.getName() + "_" + setting.canon());
        String id = inputDir;
        if (id.equals("test-uniform")) {
            id = "tu";
        } else if (id.equals("queries")) {
            id = "q";
        }

        //Path workingDir = Paths.get("E:\\download\\dev\\pac", domain);
        //Path outputEvidence = Paths.get(".", "pac", domain, modulo + "src-" + id + "_" + "t-" + theory.getName() + "_" + setting.canon());
//        Path outputEvidence = Paths.get("E:\\dev\\pac", domain, modulo + "src-" + id + "_" + "t-" + theory.getName() + "_" + setting.canon());
        //Path outputEvidence = Paths.get("E:\\dev\\pac-executer", domain, modulo + "src-" + id + "_" + "t-" + theory.getName() + "_" + setting.canon());
        Path outputEvidence = Paths.get("D:\\dev\\pac-dissertation", domain, modulo + "src-" + id + "_" + "t-" + theory.getName() + "_" + setting.canon());

//            System.out.println("version \t" + entailmentMode);
        System.out.println("output evidence\t" + outputEvidence);

        //java.util.function.Function<Integer, Path> storingLevelByLevel = (currentK) -> Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon().replace("k-" + k + "_", "k-" + currentK + "_"));
        java.util.function.Function<Integer, Path> storingLevelByLevel = null;

        System.out.println("should store to\t" + outputEvidence);
        Inference inference = new Inference(theory, null, false, typing);
        inference.inferFolder(queries, outputEvidence, setting, atMost, storingLevelByLevel);
//        }

        return retVal;
    }

    private static void resolveTyping(Path trainData, Path typed, Path workingDir, String domain) throws IOException, InterruptedException {
        System.out.println("tahle cast asi neni dotahle uplne do funkci automaticke verze, pouzij Reformatter.main");
        String variant = "";
        if (trainData.getFileName().startsWith("train")) {
            variant = trainData.getFileName().toString().substring("train".length());
            variant = variant.substring(0, variant.length() - ".db".length());
        } else {
            System.out.println("unknown input naming procedure");
        }

        Reformatter r = Reformatter.create();
        Path typedTrain = r.addTypes(trainData, typed, Sugar.list("train" + variant + ".db", "test" + variant + ".db", "train" + variant + ".db.oneLine"));
        System.out.println("reformatted\n*********************\n");
        run(typedTrain, typed, workingDir, domain);
        return;
    }

    public static SubsampledDataset datasetSubsampling(Path trainData, Path workingDir, double psi, boolean rangeRestrictedOnly) {
        return datasetSubsampling(trainData, workingDir, psi, rangeRestrictedOnly, false);
    }

    public static SubsampledDataset datasetSubsampling(Path trainData, Path workingDir, double psi, boolean rangeRestrictedOnly, boolean rewrite) {
        DatasetSampler sampler = DatasetSampler.create(DatasetSampler.UNIFORM_RANDOM, false, rewrite);
        Path subsampled = Paths.get(workingDir.toString(), trainData.getFileName().toString() + ".uni" + psi + ".db");
        if (!subsampled.toFile().exists() || rewrite) {
//            if (true) {
//                System.out.println("this should not happen for us right now, it should be subsample already!");
//                throw new IllegalStateException();
//            }
            System.out.println("subsampling dataset");
            sampler.sampleAndStore(trainData, workingDir, psi);
        }
        System.out.println("loading dataset\t" + subsampled);
        SubsampledDataset dataset = SubsampledDataset.load(subsampled);
        dataset.setStoreRangeRestricted(rangeRestrictedOnly);
        return dataset;
    }

    public static Set<Clause> learnConstraints(Path trainData, Path typed, Path dir) throws IOException, InterruptedException {
        int lits = 3; // change to 3
        int vars = 2 * lits; // change to 2
        System.out.println(lits + " literals and " + vars + " variables");

        if (!trainData.toFile().toPath().toString().contains("uwcs")) {
            // radsi nechtejme prisnejsi at to potom nehleda prilis prisne omezeni na test
            lits = 2;
            vars = 2 * lits;
            System.out.println("constraints learning: switching to " + lits + " literals and " + vars + " variables");
        }
        if (trainData.toFile().toPath().toString().contains("ntp")) {
            lits = 1;
            vars = 2 * lits;
            System.out.println("constraints learning: switching to " + lits + " literals and " + vars + " variables");
        }

        System.setProperty("ida.searchPruning.input", trainData + ".oneLine");
        System.setProperty("ida.logicStuff.constraints.saturationMode", "3");

        System.setProperty("ida.logicStuff.constraints.maxComponents", "1");
        System.setProperty("ida.logicStuff.constraints.maxPosLit", "1");
        System.setProperty("ida.logicStuff.constraints.maxNegLit", "" + Integer.MAX_VALUE);
        System.setProperty("ida.logicStuff.constraints.maxLiterals", "" + lits);
        System.setProperty("ida.logicStuff.constraints.maxVariables", "" + vars);
        System.setProperty("ida.logicStuff.constraints.minSupport", "1");
        System.setProperty("ida.logicStuff.constraints.learner", "smarter");
        System.setProperty("ida.logicStuff.constraints.useSaturation", "true"); // true, false .... tohle nastaveni nas tu nejvice zajima
        System.setProperty("ida.searchPruning.runner.overallLimit", 1 * 60 + ""); // [min]
        System.setProperty("ida.searchPruning.mining", "theory");
        System.setProperty("ida.logicStuff.constraints.useTypes", "" + (null != typed));
        if (typed != null) {
            System.setProperty("ida.logicStuff.constraints.loadTypes", typed.toString());
        }
        System.setProperty("ida.searchPruning.badRefinementsTo", "1000");
        System.setProperty("ida.logicStuff.constraints.oneLiteralHCrefinement", "true");
        System.setProperty("ida.logicStuff.constraints.hcExtendedRefinement", "false");
        System.setProperty("ida.searchPruning.ruleSaturator.useForbidden", "false"); // toto nastaveni je pouze pro development

        Path constraintsTarget = Paths.get(dir.toString(), trainData.getFileName() + "_" + lits + "_" + vars + ".constraints");
        if (!constraintsTarget.toFile().exists()) {
            System.setProperty("ida.searchPruning.storeTo", constraintsTarget.toString());
            System.out.println("learning theory... should store to\t" + constraintsTarget);
            ConjunctivePatterMiner.main(new String[]{});
        }
        Set<Clause> constraints = Utils.create().loadClauses(constraintsTarget);
        System.out.println(constraints.size() + "\thard constraints loaded from\t" + constraintsTarget + "\n***************************\n");

        Map<Pair<Predicate, Integer>, String> types = null == typed ? null : TypesInducer.load(typed);
        if ((null != typed) && (constraints.stream().anyMatch(c -> c.variables().stream().anyMatch(v -> v.type().length() < 1)) || !TypesInducer.create().validTypes(constraints, types))) {
            throw new IllegalStateException("there are not types in the theory which will result in a problem later :(");
        }
        return constraints;
    }

    public static void runHornLearner(Path typed, Set<Clause> constraints, SubsampledDataset dataset, int beamSize, int depth, int rounds, int maxVariables, Path theorySource, boolean crossentropyDriven) throws IOException {
        HornLearner learner = new HornLearner();
        boolean debugRewrite = false;
        System.out.println("**********\t\t\t\tpozor pozor debug rewrite\t" + debugRewrite);
        System.out.println("want to load theory from\t" + theorySource);

        if (false) {
            System.out.println("ending runHornLearner now !");
            return;
        }
        if (!theorySource.toFile().exists() || debugRewrite) {
            long start = System.nanoTime();
            System.setProperty("ida.searchPruning.simpleLearner.compareByAccuracy", "true"); // this invokes top-k-beam search

            Map<Pair<Predicate, Integer>, Type> typing = null == typed ? null : TypesInducer.transform(TypesInducer.load(typed));
            //System.out.println("learning horn rules with setting\tbestN " + bestN + "\tbeamSize " + beamSize + "\tdepth " + depth);
            //List<Pair<Double, HornClause>> rules = learner.learnLevelWise(dataset, bestN, beamSize, depth, constraints, typing);

            System.out.println("learning horn rules with setting\trounds " + rounds + "\tbeamSize " + beamSize + "\tdepth " + depth + "\tvariables " + maxVariables + "\tt " + (null != typed));
            Set<Clause> constraintsForPruning = constraints;
            String domain = System.getProperty("domain"); // should be parameter of the method but time limits...
            if ((null != typed && typed.toString().contains("nations"))
                    || (null != domain && domain.contains("nations"))) {
                System.out.println("throwing out constraints for rule learning!");
                constraintsForPruning = Sugar.set();
            }

            List<Pair<Double, HornClause>> rules = learner.learnEntropyDriven(dataset, rounds, beamSize, depth, maxVariables, constraintsForPruning, typing, crossentropyDriven);

            System.out.println("storing rules");

            /*List<String> withoutWeights = Sugar.list();
            List<String> weights = Sugar.list();
            weights.add("hard rules");
            withoutWeights.add("hard rules");
            constraints.forEach(c -> {
                weights.add(Double.POSITIVE_INFINITY + "\t" + c);
                withoutWeights.add(c.toString());
            });

            weights.add("implications");
            withoutWeights.add("implications");
            rules.stream().sorted(Comparator.comparing(Pair<Double, HornClause>::getR).reversed()).forEach(p -> {
                weights.add(p.r + "\t" + p.s);
                withoutWeights.add(p.s.toString());
            });*/
            List<Pair<Double, Clause>> soft = rules.stream().map(p -> new Pair<>(p.r, p.s.toClause())).sorted(Comparator.comparing(Pair<Double, Clause>::getR).reversed()).collect(Collectors.toList());
            Possibilistic ps = Possibilistic.create(constraints, soft);
            //R q = soft.stream().map(p -> p.s).collect(LinkedHashSet::new, LinkedHashSet::addAll, LinkedHashSet::addAll);

            double secondsNeeded = 1.0 * (System.nanoTime() - start) / 1000_000_000;

            System.out.println("storing theory to\t" + theorySource + "\tand\t" + (theorySource + ".poss\twith time\t" + secondsNeeded + "\ts"));
            Files.write(theorySource, Sugar.list(FOL.create(constraints, soft.stream().map(Pair::getS).collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll)).toString()));
            Files.write(Paths.get(theorySource.toString() + ".poss"), Sugar.list(ps.asOutput()));
            Files.write(Paths.get(theorySource.toString() + ".time"), Sugar.list(secondsNeeded + ""));
        }
    }

    private static Path amieTheory(Path theorySource, Path amieRules, int rounds, int maxVariables, int depth, SubsampledDataset dataset, Set<Clause> constraints, Path typed, int k) {
        if (true) {
            System.out.println("this extension to CEAMIE was done on the last minute and hasn't been checked yet, therefore do not use it");
            throw new IllegalStateException();
        }
        Path retVal = Paths.get(theorySource.toString() + ".r" + rounds + "_mv" + maxVariables + ".logic.poss.CEAMIE");
        Path retVal2 = Paths.get(theorySource.toString() + ".r" + rounds + "_mv" + maxVariables + ".logic" + ".k" + k + ".pac3.poss.CEAMIE");
        System.out.println(retVal.toFile().exists() + "\t" + retVal);
        System.out.println(retVal2.toFile().exists() + "\t" + retVal2);
        //if (retVal.toFile().exists() && retVal2.toFile().exists()) {
        if (retVal.toFile().exists()) {
            System.out.println("file exists\t" + theorySource);
            return retVal;
        }

        Reformatter r = Reformatter.create();
        Collection<Clause> clauses = r.loadAmieOutput(theorySource);
        RuleSaturator saturator = RuleSaturator.create(constraints);
        Set<IsoClauseWrapper> selected = clauses.stream()
                .filter(c -> LogicUtils.variables(c).size() <= maxVariables)
                .map(HornClause::new)
                .filter(LogicUtils::isRangeRestricted)
                //.map(saturator::saturate) // TODO this given here because it takes to much time to satrate 200k of rules
                .filter(Objects::nonNull)
                .map(IsoClauseWrapper::create)
                .collect(Collectors.toSet());

        System.out.println("** clause loaded");

        Possibilistic accTheory = null;
        List<Pair<Double, Clause>> accRules = null;
        if (!retVal.toFile().exists()) {
            if (!retVal2.toString().contains("CEAMIE")) {
                System.out.println("selecting based on AMIE confidence");
                Counters<Pair<String, Integer>> counter = new Counters<>();
                // is sorted when added
                accRules = selected.stream()
                        .map(icw -> {
                            HornClause horn = HornClause.create(icw.getOriginalClause());
                            Pair<Set<Literal>, Set<Literal>> posNeg = dataset.evaluate(horn);
                            System.out.println(horn + "\t" + posNeg + (null != posNeg && 0 != posNeg.r.size() ? (posNeg.r.size() * 1.0) / (posNeg.r.size() + posNeg.s.size()) : ""));
                            if (null == posNeg || 0 == posNeg.r.size()) {
                                return null;
                            }
                            double acc = (posNeg.r.size() * 1.0) / (posNeg.r.size() + posNeg.s.size());
                            return new Pair<>(acc, horn);
                        }).filter(Objects::nonNull)
                        .sorted(Comparator.comparing(Pair<Double, HornClause>::getR).reversed())
                        .filter(p -> {
                            Pair<String, Integer> predicate = p.s.head().getPredicate();
                            if (counter.get(predicate) < rounds) {
                                System.out.println("*** incrementing\t" + predicate.toString());
                                counter.increment(predicate);
                                return true;
                            }
                            return false;
                        }).map(p -> new Pair<>(p.r, p.s.toClause()))
                        .collect(Collectors.toList());
            } else {
                System.out.println("CEAMIE kind of untested!");
                List<Pair<Double, HornClause>> accSelected = selected.stream().map(icw -> {
                            HornClause horn = HornClause.create(icw.getOriginalClause());
                            Pair<Set<Literal>, Set<Literal>> posNeg = dataset.evaluate(horn);
                            System.out.println(horn + "\t" + posNeg + (null != posNeg && 0 != posNeg.r.size() ? (posNeg.r.size() * 1.0) / (posNeg.r.size() + posNeg.s.size()) : ""));
                            double acc = (posNeg.r.size() * 1.0) / (posNeg.r.size() + posNeg.s.size());
                            return new Pair<>(acc, horn);
                        })
                        .filter(Objects::nonNull).collect(Collectors.toList());

                System.out.println("selecting based on CE from AMIE mined");
                HornLearner learner = new HornLearner();
                accRules = learner.learnEntropyDriven(dataset, rounds, learner.learnEntropyDriven(dataset, rounds, accSelected))
                        .stream().map(p -> new Pair<>(p.getR(), p.getS().toClause()))
                        .collect(Collectors.toList());


            }
            Set<IsoClauseWrapper> in = Sugar.set();
            List<Pair<Double, Clause>> accRules2 = Sugar.list();
            accRules.stream().forEach(pair -> {
                IsoClauseWrapper saturated = IsoClauseWrapper.create(saturator.saturate(new HornClause(pair.getS())));
                if (!in.contains(saturated)) {
                    in.add(saturated);
                    accRules2.add(pair);
                }
            });
            accRules = accRules2;

            accTheory = Possibilistic.create(constraints, accRules);
            try {
                System.out.println("storing to\t" + retVal);
                Files.write(retVal, Sugar.list(accTheory.asOutput()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            accTheory = Possibilistic.create(retVal);
        }

        if (false) {
            PACAccuracyDataset computer = PACAccuracyDataset.create(dataset.getEvidence(), k);
            // the same just for PAC acc
            if (!retVal2.toFile().exists()) {
                Counters<Pair<String, Integer>> counter = new Counters<>();
                // is sorted when added
                List<Pair<Double, Clause>> pacTheory = accTheory.getSoftRules().stream()
                        .filter(p -> {
                            if (p.r > 0.5) {
                                return true;
                            }
                            System.out.println("6 throwing away\t" + p.r + "\t" + p.s);
                            return false;
                        })
                        .map(p -> {
                            HornClause horn = HornClause.create(p.s);
                            double acc = computer.accuracy(horn);
                            System.out.println(horn + "\t" + acc);
                            return new Pair<>(acc, horn);
                        }).sorted(Comparator.comparing(Pair<Double, HornClause>::getR).reversed())
                        .filter(p -> {
                            Pair<String, Integer> predicate = p.s.head().getPredicate();
                            if (counter.get(predicate) < rounds) {
                                counter.increment(predicate);
                                return true;
                            }
                            return false;
                        }).map(p -> new Pair<>(p.r, p.s.toClause()))
                        .collect(Collectors.toList());
                Possibilistic pl = Possibilistic.create(constraints, pacTheory);
                try {
                    System.out.println("storing to\t" + retVal2);
                    Files.write(retVal2, Sugar.list(pl.asOutput()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("skipping sortAnyBURL by PAC acc on AMIE produced rules");
        }
//        if ("pacAcc".equals(System.getProperty("pipeline.criterion"))) {
//            return retVal2;
//        }
        return retVal;

    }

    private static Path amieTheoryCE(Path theorySource, Path amieRules, int rounds, int maxVariables, int depth, SubsampledDataset dataset, Set<Clause> constraints, Path typed, int k) {
        Path retVal = Paths.get(theorySource.toString() + ".CE" + ".r" + rounds + "_mv" + maxVariables + ".logic.poss");
        Path retVal2 = Paths.get(theorySource.toString() + ".CE" + ".r" + rounds + "_mv" + maxVariables + ".logic" + ".k" + k + ".pac3.poss");
        System.out.println(retVal.toFile().exists() + "\t" + retVal);
        System.out.println(retVal2.toFile().exists() + "\t" + retVal2);
        //if (retVal.toFile().exists() && retVal2.toFile().exists()) {
        if (retVal.toFile().exists()) {
            System.out.println("file exists\t" + theorySource);
            return retVal;
        }

        Reformatter r = Reformatter.create();
        Collection<Clause> clauses = r.loadAmieOutput(theorySource);
        RuleSaturator saturator = RuleSaturator.create(constraints);
        Set<IsoClauseWrapper> selected = clauses.stream()
                .filter(c -> LogicUtils.variables(c).size() <= maxVariables)
                .map(HornClause::new)
                .filter(LogicUtils::isRangeRestricted)
                //.map(saturator::saturate) // TODO this given here because it takes to much time to saturate 200k of rules
                .filter(Objects::nonNull)
                .map(IsoClauseWrapper::create)
                .collect(Collectors.toSet());

        System.out.println("** clause loaded");

        Possibilistic accTheory = null;
        List<Pair<Double, Clause>> accRules = null;
        boolean debugRewrite = false;
        System.out.println("**********\t\t\t\tpozor pozor debug rewrite\t" + debugRewrite);

        if (!retVal.toFile().exists() || debugRewrite) {
            // rule-selection itself

            HornLearner learner = new HornLearner();
            long start = System.nanoTime();
            System.setProperty("ida.searchPruning.simpleLearner.compareByAccuracy", "true"); // this invokes top-k-beam search
            System.out.println("learning horn rules with setting\trounds " + rounds + "\tbeamSize " + "0" + "\tdepth " + depth + "\tvariables " + maxVariables + "\tt " + (null != typed));
            System.out.println("inside is " + selected.size());
            List<Pair<Double, HornClause>> rules = learner.selectEntropyDriven(dataset, rounds, selected);

            System.out.println("storing rules");
            List<Pair<Double, Clause>> soft = rules.stream().map(p -> new Pair<>(p.r, p.s.toClause())).sorted(Comparator.comparing(Pair<Double, Clause>::getR).reversed()).collect(Collectors.toList());
            Possibilistic ps = Possibilistic.create(constraints, soft);

            double secondsNeeded = 1.0 * (System.nanoTime() - start) / 1000_000_000;

            System.out.println("storing theory to\t" + retVal + "\tand\t" + (retVal + ".poss\twith time\t" + secondsNeeded + "\ts"));
            try {
                Files.write(Paths.get(retVal.toString()), Sugar.list(ps.asOutput()));
                Files.write(Paths.get(retVal.toString() + ".time"), Sugar.list(secondsNeeded + ""));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            accTheory = Possibilistic.create(retVal);
        }

        if (false) {
            PACAccuracyDataset computer = PACAccuracyDataset.create(dataset.getEvidence(), k);
            // the same just for PAC acc
            if (!retVal2.toFile().exists()) {
                Counters<Pair<String, Integer>> counter = new Counters<>();
                // is sorted when added
                List<Pair<Double, Clause>> pacTheory = accTheory.getSoftRules().stream()
                        .filter(p -> {
                            if (p.r > 0.5) {
                                return true;
                            }
                            System.out.println("6 throwing away\t" + p.r + "\t" + p.s);
                            return false;
                        })
                        .map(p -> {
                            HornClause horn = HornClause.create(p.s);
                            double acc = computer.accuracy(horn);
                            System.out.println(horn + "\t" + acc);
                            return new Pair<>(acc, horn);
                        }).sorted(Comparator.comparing(Pair<Double, HornClause>::getR).reversed())
                        .filter(p -> {
                            Pair<String, Integer> predicate = p.s.head().getPredicate();
                            if (counter.get(predicate) < rounds) {
                                counter.increment(predicate);
                                return true;
                            }
                            return false;
                        }).map(p -> new Pair<>(p.r, p.s.toClause()))
                        .collect(Collectors.toList());
                Possibilistic pl = Possibilistic.create(constraints, pacTheory);
                try {
                    System.out.println("storing to\t" + retVal2);
                    Files.write(retVal2, Sugar.list(pl.asOutput()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("skipping sortAnyBURL by PAC acc on AMIE-CE produced rules");
        }
//        if ("pacAcc".equals(System.getProperty("pipeline.criterion"))) {
//            return retVal2;
//        }
        return retVal;

    }


    private static Path theoryCSsort(PossibilisticTheory theory, Path theoryTarget, SubsampledDataset dataset) {
        Path retVal = theoryTarget;
        if (retVal.toFile().exists()) {
            System.out.println("file exists\t" + theoryTarget);
            return retVal;
        }

        Reformatter r = Reformatter.create();
        RuleSaturator saturator = RuleSaturator.create(Sugar.list());
        Set<IsoClauseWrapper> selected = theory.getSoftRules().stream()
                .map(Pair::getS)
                .map(HornClause::new)
                .filter(LogicUtils::isRangeRestricted)
                .filter(Objects::nonNull)
                .map(IsoClauseWrapper::create)
                .collect(Collectors.toSet());

        System.out.println("** clause loaded");

        Possibilistic accTheory = null;
        List<Pair<Double, Clause>> accRules = null;
        boolean debugRewrite = false;
        System.out.println("**********\t\t\t\tpozor pozor debug rewrite\t" + debugRewrite);

        if (!retVal.toFile().exists() || debugRewrite) {
            // rule-selection itself

            HornLearner learner = new HornLearner();
            long start = System.nanoTime();
            System.setProperty("ida.searchPruning.simpleLearner.compareByAccuracy", "true"); // this invokes top-k-beam search
            System.out.println("inside is " + selected.size());
            List<Pair<Double, HornClause>> rules = learner.selectAcc(dataset, selected); // pouze ke vsemu doplni nasi confidency

            System.out.println("storing rules");
            List<Pair<Double, Clause>> soft = rules.stream().map(p -> new Pair<>(p.r, p.s.toClause())).sorted(Comparator.comparing(Pair<Double, Clause>::getR).reversed()).collect(Collectors.toList());
            Possibilistic ps = Possibilistic.create(theory.getHardRules(), soft);

            double secondsNeeded = 1.0 * (System.nanoTime() - start) / 1000_000_000;

            System.out.println("storing theory to\t" + retVal + "\tand\t" + (retVal + ".poss\twith time\t" + secondsNeeded + "\ts"));
            try {
                Files.write(Paths.get(retVal.toString()), Sugar.list(ps.asOutput()));
                Files.write(Paths.get(retVal.toString() + ".time"), Sugar.list(secondsNeeded + ""));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            accTheory = Possibilistic.create(retVal);
        }
        return retVal;

    }

    // generalize with amieTheoryT
    private static Path amieTheoryT(Path theorySource, Path amieRules, int maxVariables, int depth, SubsampledDataset dataset, Set<Clause> constraints, Path typed, int k) {
        Path retVal = Paths.get(theorySource.toString() + ".T50" + ".mv" + maxVariables + ".logic.poss");
        Path retVal2 = Paths.get(theorySource.toString() + ".T50" + ".mv" + maxVariables + ".logic" + ".k" + k + ".pac3.poss");
        System.out.println(retVal.toFile().exists() + "\t" + retVal);
        System.out.println(retVal2.toFile().exists() + "\t" + retVal2);
        //if (retVal.toFile().exists() && retVal2.toFile().exists()) {
        if (retVal.toFile().exists()) {
            System.out.println("file exists\t" + theorySource);
            return retVal;
        }

        Reformatter r = Reformatter.create();
        Collection<Clause> clauses = r.loadAmieOutput(theorySource);
        RuleSaturator saturator = RuleSaturator.create(Sugar.list());
        Set<IsoClauseWrapper> selected = clauses.stream()
                .filter(c -> LogicUtils.variables(c).size() <= maxVariables)
                .map(HornClause::new)
                .filter(LogicUtils::isRangeRestricted)
                //.map(saturator::saturate) // TODO this given here because it takes to much time to saturate 200k of rules
                .filter(Objects::nonNull)
                .map(IsoClauseWrapper::create)
                .collect(Collectors.toSet());

        System.out.println("** clause loaded");

        Possibilistic accTheory = null;
        List<Pair<Double, Clause>> accRules = null;
        boolean debugRewrite = false;
        System.out.println("**********\t\t\t\tpozor pozor debug rewrite\t" + debugRewrite);

        if (!retVal.toFile().exists() || debugRewrite) {
            // rule-selection itself

            HornLearner learner = new HornLearner();
            long start = System.nanoTime();
            System.setProperty("ida.searchPruning.simpleLearner.compareByAccuracy", "true"); // this invokes top-k-beam search
            System.out.println("learning horn rules with setting\tbeamSize " + "0" + "\tdepth " + depth + "\tvariables " + maxVariables + "\tt " + (null != typed));
            System.out.println("inside is " + selected.size());
            List<Pair<Double, HornClause>> rules = learner.selectAcc(dataset, selected); // pouze ke vsemu doplni nasi confidency

            System.out.println("storing rules");
            List<Pair<Double, Clause>> soft = rules.stream().map(p -> new Pair<>(p.r, p.s.toClause())).sorted(Comparator.comparing(Pair<Double, Clause>::getR).reversed()).collect(Collectors.toList());
            Possibilistic ps = Possibilistic.create(constraints, soft);

            double secondsNeeded = 1.0 * (System.nanoTime() - start) / 1000_000_000;

            System.out.println("storing theory to\t" + retVal + "\tand\t" + (retVal + ".poss\twith time\t" + secondsNeeded + "\ts"));
            try {
                Files.write(Paths.get(retVal.toString()), Sugar.list(ps.asOutput()));
                Files.write(Paths.get(retVal.toString() + ".time"), Sugar.list(secondsNeeded + ""));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            accTheory = Possibilistic.create(retVal);
        }

        if (false) {
            PACAccuracyDataset computer = PACAccuracyDataset.create(dataset.getEvidence(), k);
            // the same just for PAC acc
            if (!retVal2.toFile().exists()) {
                Counters<Pair<String, Integer>> counter = new Counters<>();
                // is sorted when added
                List<Pair<Double, Clause>> pacTheory = accTheory.getSoftRules().stream()
                        .filter(p -> {
                            if (p.r > 0.5) {
                                return true;
                            }
                            System.out.println("6 throwing away\t" + p.r + "\t" + p.s);
                            return false;
                        })
                        .map(p -> {
                            HornClause horn = HornClause.create(p.s);
                            double acc = computer.accuracy(horn);
                            System.out.println(horn + "\t" + acc);
                            return new Pair<>(acc, horn);
                        }).sorted(Comparator.comparing(Pair<Double, HornClause>::getR).reversed())
                        .filter(p -> {
                            Pair<String, Integer> predicate = p.s.head().getPredicate();
                            if (counter.get(predicate) < 10000000) {
                                counter.increment(predicate);
                                return true;
                            }
                            return false;
                        }).map(p -> new Pair<>(p.r, p.s.toClause()))
                        .collect(Collectors.toList());
                Possibilistic pl = Possibilistic.create(constraints, pacTheory);
                try {
                    System.out.println("storing to\t" + retVal2);
                    Files.write(retVal2, Sugar.list(pl.asOutput()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.out.println("skipping sortAnyBURL by PAC acc on AMIE-CE produced rules");
        }
//        if ("pacAcc".equals(System.getProperty("pipeline.criterion"))) {
//            return retVal2;
//        }
        return retVal;

    }


    private static Path runHitInference(Set<Clause> constraints, Path theorySource, int k, Path hitsEvidence, Path testQueries, List<Clause> stratifiedTheory, Path typed) {
        //        SupportEntailmentInference support = SupportEntailmentInference.get(LogicUtils.loadEvidence(hitsEvidence));
//        Entailed entails = support.entails(FOL.get(constraints, Sugar.setFromCollections(stratifiedTheory), ""), k);
//        System.out.println("entailed\t" + ((KECollector) entails).getEntailed().size());
//        System.exit(-42);

        HitsInference hits = new HitsInference(null == typed ? null : TypesInducer.load(typed));
        Set<Literal> evidence = LogicUtils.loadEvidence(hitsEvidence);
        return hits.infer(k, theorySource, evidence, testQueries, stratifiedTheory, constraints);
    }

    private static void presentResultsUWCS(Path testQueries, Path predictions) {
        Utils u = Utils.create();
        Path testLiterals = testQueries;
        throw new IllegalStateException();
        /*u.scoreHits(testLiterals, predictions); // randomization inside
        u.scoreHits(testLiterals, Sugar.list(predictions), Sugar.list(1, 3, 5, 10, 20, 50));
        */
    }
}



