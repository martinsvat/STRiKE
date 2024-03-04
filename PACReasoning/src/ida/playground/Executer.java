package ida.playground;

import ida.gnns.LearnedRule;
import ida.gnns.RulesLike;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.pacReasoning.Pipeline;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 15. 10. 2021.
 */
public class Executer {

    // holder6 zvlada constraints, ty dale jsou optimalizovane na teorie bez constraints (takze s constraints nefunguji)

    public static void main(String[] args) throws IOException {
//        runInference();
//        runInferenceCuts();
//        showPRAUC();
//        reformatteDataset();
//        theoryFromAnyBurl();
//        AnyBurlCheck.inferAnyBurl();
    }



    private static void theoryFromAnyBurl() {
        String dataset = "umls";
//        double minAcc = 0.85;
//        double minAcc = 0.75;
        double minAcc = 0.5;
//        double minAcc = 0.1;
//        int rulesTime = 100; // for nations
        int rulesTime = 500; // for kinshipps
        Path path = Paths.get("C:\\data\\school\\development\\anyburl\\rules\\" + dataset + "-" + rulesTime);
        Path out = Paths.get("C:\\data\\school\\development\\anyburl\\rules\\" + dataset + "-" + rulesTime + ".fol." + minAcc + ".rules");
        RulesLike rl = new RulesLike();
        List<LearnedRule> rules = rl.loadAnyBurl(path);
        rules = rules.stream().filter(rule -> LogicUtils.constants(rule.getRule()).isEmpty())
                .filter(rule -> Double.valueOf(rule.getProperty("confidence")) > minAcc)
                .collect(Collectors.toList());
        Possibilistic theory = Possibilistic.create(Sugar.set(),
                rules.stream().map(rule -> new Pair<>(Double.valueOf(rule.getProperty("confidence")), rule.getRule())).collect(Collectors.toList()));
        try {
            Files.write(out, Sugar.list(theory.asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void reformatteDataset() {
        String dataset = "nationsA2";
        Path path = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + dataset + "\\train.db");
        Path out = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + dataset + "\\anyburl.train.db");
        Set<Literal> data = LogicUtils.loadEvidence(path);
        List<String> lines = data.stream().map(literal -> literal.get(0) + "\t" + literal.predicate() + "\t" + literal.get(1)).collect(Collectors.toList());
        try {
            Files.write(out, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void showPRAUC() {
        String domain = "umls";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();


        Set<Literal> allTest = LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "test.db"));

        List<Pair<Path, Integer>> paths = Sugar.list();
        if ("kinships".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
                /* strike paper */
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    ,
                /**/
                    // subset of FOL anyburl rules (>confidence) with one-step prediction
                /*new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-kinships-500.fol.0.1.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-kinships-500.fol.0.5.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-kinships-500.fol.0.75.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-kinships-500.fol.0.85.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                */
                    // k-entailment with FOL anyburl rules
                    //new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-kinships-500.fol.0.85.rules_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    // anyburl inference
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\anyburl\\kinships\\tu_500"), 1)
            ));
        } else if ("umls".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
                /* strike paper + most one step */
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-umls-500.fol.0.1.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    ,
                    /**/
                /* subset of FOL anyburl rules (>confidence) with one-step prediction * /
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-umls-500.fol.0.1.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-umls-500.fol.0.5.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-umls-500.fol.0.75.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\umls\\10src-tu_t-umls-500.fol.0.85.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                /**/
                    // k-entailment with FOL anyburl rules
                    //new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-kinships-500.fol.0.85.rules_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    // anyburl inference
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\anyburl\\umls\\tu_500"), 1)
            ));
        } else if ("nationsA2".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
                /* strike paper + most one step */
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    //new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-nationsA2-100.fol.0.1.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                /**/
                /* subset of FOL anyburl rules (>confidence) with one-step prediction * /
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-nationsA2-100.fol.0.1.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-nationsA2-100.fol.0.5.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-nationsA2-100.fol.0.75.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\nationsA2\\10src-tu_t-nationsA2-100.fol.0.85.rules_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                /**/
                    // k-entailment with FOL anyburl rules
                    //new Pair<>(Paths.get("E:\\dev\\pac-executer\\kinships\\10src-tu_t-kinships-500.fol.0.85.rules_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    // anyburl inference
                    new Pair<>(Paths.get("E:\\dev\\pac-executer\\anyburl\\nationsA2\\tu_100"), 1)
            ));

        }


        //String res = u.plotAUCPRInterpolated(groundEvidence, paths, );
        String res = u.plotAUCPRInterpolated(groundEvidence, paths, allTest); // these are filtered-corrupted
//        String res = u.plotAUCPRInterpolatedOptimized(groundEvidence,paths);
//        String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);//kdyz se to pocita tak se nesmi odebrat evidence, jinak to pujde zakonite dolu!


    }


    private static void runInferenceCuts() {
//        String domain = "kinships";
        //String domain = "nationsA2";
        String domain = "umls";
        // kinships
        // PAC paper
        // Path theorySource = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\kinships", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt");

        // AnyBURL FOL acc > 0.5
        //Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "kinships-500.fol.0.85.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "kinships-500.fol.0.75.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "kinships-500.fol.0.5.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "kinships-500.fol.0.1.rules");


        // nations
        // strike paper
        //Path theorySource = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\nationsA2", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt");

        for (String cut : Sugar.list("0.85", "0.5", "0.1")) { // "0.75",

            //Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "nationsA2-100.fol." + cut + ".rules");
            Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "umls-500.fol." + cut + ".rules");

            Path typed = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", domain, "typing.txt");
            Path queries = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", domain, "test-uniform");
            Possibilistic theory = Possibilistic.create(theorySource);

            int k = 5;

            int atMost = 2000;
            System.setProperty("atMost", "" + atMost);
            System.setProperty("inference.modulo", "10");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");

            // oneS
        /**/
            System.setProperty("ida.pacReasoning.entailment.mode", "oneS");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            //        System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            Pipeline.runInference(theory, queries, typed, domain);
         /**/

            // PL
            /** / to je vlastne blbost bez constraints
             Pipeline.runInference(theory, queries, typed, domain);
             System.setProperty("ida.pacReasoning.entailment.mode", "a");
             System.setProperty("ida.pacReasoning.entailment.k", "0");
             System.setProperty("ida.pacReasoning.entailment.logic", "pl");
             //        System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
             Pipeline.runInference(theory, queries, typed, domain);
             /**/

            // kPLW
            /** /
             System.setProperty("ida.pacReasoning.entailment.mode", "kWC");
             System.setProperty("ida.pacReasoning.entailment.k", "" + k);
             System.setProperty("ida.pacReasoning.entailment.logic", "pl");
             System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
             Pipeline.runInference(theory, queries, typed, domain);
             /**/

            // kPL
            /** /
             System.setProperty("ida.pacReasoning.entailment.mode", "k");
             System.setProperty("ida.pacReasoning.entailment.k", "" + k);
             System.setProperty("ida.pacReasoning.entailment.logic", "pl");
             Pipeline.runInference(theory, queries, typed, domain);
             /**/

            //Path output = Paths.get("E:\\dev\\pac-executer\\kinships");


            //GNNs.runInference(theory, queries,output, Sugar.set(), 100);

//        Map<Pair<Predicate, Integer>, String> typing = TypesInducer.load(typed);
//        Inference inference = new Inference(theory, null, false, typing);
//        EntailmentSetting setting = EntailmentSetting.create();
//        java.util.function.Function<Integer, Path> storingLevelByLevel = null;
//        inference.inferFolder(queries, output, setting, atMost, storingLevelByLevel);
        }
    }

    private static void runInference() {
//        radsi prepocitat kinships a nationsA2 s KWC!!!!4
//        String domain = "kinships";
//        String domain = "nationsA2";
        String domain = "umls";
        // kinships
        // PAC paper
//         Path theorySource = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\kinships", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c522.fol.poss.k5.CS.ptt");

        // AnyBURL FOL acc > 0.5
        //Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "kinships-500.fol.0.85.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "kinships-500.fol.0.75.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "kinships-500.fol.0.5.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "kinships-500.fol.0.1.rules");


        // nations
        // strike paper
//        Path theorySource = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\nationsA2", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2196.fol.poss.k5.CS.ptt");

        //Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "nationsA2-100.fol.0.85.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "nationsA2-100.fol.0.75.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "nationsA2-100.fol.0.5.rules");
//        Path theorySource = Paths.get("C:\\data\\school\\development\\anyburl\\rules", "nationsA2ss-100.fol.0.1.rules");

        // umls
        // strike paper
        Path theorySource = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umls", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.ptt");


        Path typed = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", domain, "typing.txt");
        Path queries = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", domain, "test-uniform");
        Possibilistic theory = Possibilistic.create(theorySource);

        int k = 5;

        for (Integer atMost : Sugar.list(2000)) {

//        }
//        int atMost = 500;
            System.setProperty("atMost", "" + atMost);
            System.setProperty("inference.modulo", "10");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");

            // oneS
        /**/
            System.setProperty("ida.pacReasoning.entailment.mode", "oneS");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            //        System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            Pipeline.runInference(theory, queries, typed, domain);
         /**/

            // PL
            /** /
             Pipeline.runInference(theory, queries, typed, domain);
             System.setProperty("ida.pacReasoning.entailment.mode", "a");
             System.setProperty("ida.pacReasoning.entailment.k", "0");
             System.setProperty("ida.pacReasoning.entailment.logic", "pl");
             //        System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
             Pipeline.runInference(theory, queries, typed, domain);
             /**/

            // kPLW
        /**/
            System.setProperty("ida.pacReasoning.entailment.mode", "kWC");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            Pipeline.runInference(theory, queries, typed, domain);
        /**/

            // kPL
        /**/
            System.setProperty("ida.pacReasoning.entailment.mode", "k");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            Pipeline.runInference(theory, queries, typed, domain);
         /**/
        }

        //Path output = Paths.get("E:\\dev\\pac-executer\\kinships");


        //GNNs.runInference(theory, queries,output, Sugar.set(), 100);

//        Map<Pair<Predicate, Integer>, String> typing = TypesInducer.load(typed);
//        Inference inference = new Inference(theory, null, false, typing);
//        EntailmentSetting setting = EntailmentSetting.create();
//        java.util.function.Function<Integer, Path> storingLevelByLevel = null;
//        inference.inferFolder(queries, output, setting, atMost, storingLevelByLevel);

    }
}
