package ida.playground;

import ida.gnns.LearnedRule;
import ida.gnns.RulesLike;
import ida.ilp.logic.*;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.pacReasoning.entailment.cuts.Cut;
import ida.pacReasoning.entailment.cuts.KCut;
import ida.pacReasoning.Pipeline;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.evaluation.Utils;
import ida.searchPruning.ConjunctivePatterMiner;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;


public class DissertationExperiments {


    public static <T> void main(String[] args) throws IOException, InterruptedException {
//        List<Pair<String, String>> q = Sugar.list(new Pair<>("maxad3", "ml3"),
//                new Pair<>("maxad4", "ml4"),
//                new Pair<>("mins2", "mins2"),
//                new Pair<>("m2-10", "m2-10"),
//                new Pair<>("default", "default")
//        );
//        for (Pair<String, String> pair : q) {
//            amieOutputToTheory(Paths.get("C:\\data\\school\\development\\amie3\\amie\\predictions\\umls." + pair.getR() + ".amiePostprocess"),
//                    Paths.get("D:\\dev\\pac-dissertation\\umls\\AR." + pair.getS() + ".poss")); // amie raw! range restricted
//        }

//            amieOutputToTheory(Paths.get("C:\\data\\school\\development\\amie3\\amie\\predictions\\nations.t4.amiePostprocess"),
//                    Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\AR.t4.poss")); // amie raw! range restricted
        //Paths.get("D:\\dev\\pac-dissertation\\kinships\\test.txt")); // amie raw! range restricted

        //customIJCAIInference();
//        plotIJCAI17Protein(true);

//        customInference();
        //zkusit jeste prepsat strike-teorie s depth3, pustit s a bez constraints a kouknout se co to dela :)) jak si to povede oproti amie
//        showPR();
        showAucPR2();
//        showAucPR();
//        pruneConstraints();

//        typesCheck(); to je v poradku protoze types jsou ulozene jako "predicate/arity argument-index type" :))
//        constraintsCheck();

//        anyburlOutputToTheory(Paths.get("C:\\data\\school\\development\\anyburl23\\umls-ver.output-100"),
//                Paths.get("D:\\dev\\pac-dissertation\\umls\\AB-100.5.poss")
//                , 0.5);

//        addAllDiff(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\AR.ml3.poss"));
//        count();
    }

    private static void count() throws IOException {
        /*Pair<List<Clause>, List<Double>> data = MEDataset.load(Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\nci_transformed\\gi50_screen_KM20L2.txt"));
        Set<Pair<String,Integer>> s = Sugar.set();
        data.getR().forEach(c -> s.addAll(LogicUtils.predicates(c)));
        List<Integer> idxs = s.stream().map(p -> p.getS()).distinct().sorted().collect(Collectors.toList());
        idxs.forEach(idx -> {
            List<Pair<String, Integer>> sub = s.stream().filter(pair -> pair.getS().equals(idx)).collect(Collectors.toList());
            System.out.println(idx + "\t" + sub.size() + "\t" + sub);
        });
        */
        List<String> paths = Sugar.list(
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\mlns\\uwcs\\all.db.oneLine",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\mlns\\protein\\merged.txt.oneLine",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\mlns\\imdb\\merged.db.oneLine",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\groups\\loopDataset",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\groups\\moufangLoop35",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\groups\\moufangs100",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\groups\\quasiDataset",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\pddl\\grip\\examplesHalf");
        for (String path : paths) {
            Pair<List<Clause>, List<Double>> data = MEDataset.load(Paths.get(path));
            Set<Pair<String, Integer>> s = Sugar.set();
            data.getR().forEach(c -> s.addAll(LogicUtils.predicates(c)));
            System.out.println();
            System.out.println(path);
            System.out.println(data.r.size());
            List<Integer> idxs = s.stream().map(p -> p.getS()).distinct().sorted().collect(Collectors.toList());
            idxs.forEach(idx -> {
                List<Pair<String, Integer>> sub = s.stream().filter(pair -> pair.getS().equals(idx)).collect(Collectors.toList());
                System.out.println(idx + "\t" + sub.size() + "\t" + sub);
            });
        }
    }

    private static void addAllDiff(Path path) throws IOException {
        Possibilistic origin = Possibilistic.create(path);
        List<Pair<Double, Clause>> horn = origin.getSoftRules().stream()
                .map(pair -> {
                    Set<Variable> variables = LogicUtils.variables(pair.s);
                    pair.s.addLiteral(new Literal(SpecialVarargPredicates.ALLDIFF, true, Sugar.listFromCollections(variables)));
                    return new Pair<>(pair.r, pair.s);
                })
                .collect(Collectors.toList());
        Possibilistic theory = Possibilistic.create(Sugar.set(), horn);
        Path out = Paths.get(path.getParent().toString(), "ad." + path.getFileName());
        Files.write(out, Sugar.list(theory.asOutput()));
    }


    private static void plotIJCAI17Protein(boolean displayDifference) {
        boolean cumulative = true;
//        String domain = "protein";
        String domain = "uwcs";
        //Path dir = Paths.get(".", "pac", domain);
        Path dir = Paths.get("D:\\dev\\pac", domain);
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
                // kPLS
                , new Pair<>(new Pair<>(Paths.get(dir.toString(), "src-q_t-ICAJ.poss_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5), KCut.K_CUT)
                // one step
                , new Pair<>(new Pair<>(Paths.get("D:\\dev\\pac-dissertation", domain, "10src-q_t-ICAJ.CS.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 5), KCut.K_CUT)
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

    private static void constraintsCheck() throws IOException, InterruptedException {
        int lits = 2;
        int vars = 4;
        System.out.println(lits + " literals and " + vars + " variables");

        String trainData = "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\umls\\train.db.typed";
        //String typed = "C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\umls\\typing.txt";
        String typed = null;
        System.setProperty("ida.searchPruning.input", trainData + ".oneLine");
        System.setProperty("ida.logicStuff.constraints.saturationMode", "3");

        System.setProperty("ida.logicStuff.constraints.maxComponents", "1");
        System.setProperty("ida.logicStuff.constraints.maxPosLit", "1");
        System.setProperty("ida.logicStuff.constraints.maxNegLit", "2");
        System.setProperty("ida.logicStuff.constraints.maxLiterals", "" + lits);
        System.setProperty("ida.logicStuff.constraints.maxVariables", "" + vars);
        System.setProperty("ida.logicStuff.constraints.minSupport", "1");
        System.setProperty("ida.logicStuff.constraints.learner", "smarter");
        System.setProperty("ida.logicStuff.constraints.useSaturation", "false"); // true, false .... tohle nastaveni nas tu nejvice zajima
        System.setProperty("ida.searchPruning.runner.overallLimit", 1 * 60 + ""); // [min]
        System.setProperty("ida.searchPruning.mining", "theory");
        System.setProperty("ida.logicStuff.constraints.useTypes", "" + (null != typed));
        if (null != typed) {
            System.setProperty("ida.logicStuff.constraints.loadTypes", typed.toString());
        }
        System.setProperty("ida.searchPruning.badRefinementsTo", "1000");
        System.setProperty("ida.logicStuff.constraints.oneLiteralHCrefinement", "true");
        System.setProperty("ida.logicStuff.constraints.hcExtendedRefinement", "false");
        System.setProperty("ida.searchPruning.ruleSaturator.useForbidden", "false"); // toto nastaveni je pouze pro development

        Path constraintsTarget = Paths.get("D:\\dev\\pac-dissertation\\custom-constraints\\constraints_" + lits + "_" + vars + ".constraints");
        if (!constraintsTarget.toFile().exists()) {
            System.setProperty("ida.searchPruning.storeTo", constraintsTarget.toString());
            System.out.println("learning theory... should store to\t" + constraintsTarget);
            ConjunctivePatterMiner.main(new String[]{});
        }
    }

    private static void typesCheck() {
        Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", "umls", "train.db"));

        TypesInducer inducer = new TypesInducer();
        Map<Pair<Predicate, Integer>, Type> r1 = inducer.induce(evidence);
        Map<Pair<Predicate, Integer>, Type> r2 = inducer.rename(r1);

        System.out.println("inducer result");
        inducer.writeDown(r2);
    }

    private static void pruneConstraints() throws IOException {
//        String domain = "nationsA2";
//        String domain = "kinships";
        String domain = "umls";
        Path train = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Path test = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        Set<Literal> trainEvd = LogicUtils.loadEvidence(train);
        Set<Literal> testEvd = LogicUtils.loadEvidence(test);

//        Path theorySource = Paths.get("D:\\dev\\pac-resubmit\\nationsA2\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c2196.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-resubmit\\kinships\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c522.fol.poss.k5.CS.ptt");
        Path theorySource = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umls\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c2878.fol.poss.k5.CS.ptt");
        Possibilistic theory = Possibilistic.create(theorySource);

        /*
        Matching trainMatch = Matching.create(new Clause(trainEvd), Matching.THETA_SUBSUMPTION);
        Matching testMatch = Matching.create(new Clause(testEvd), Matching.THETA_SUBSUMPTION);
        for (Clause hardRule : theory.getHardRules()) {
            Clause rule = LogicUtils.untype(hardRule);
            if(LogicUtils.positiveLiterals(rule).size() > 0){
                System.out.println(rule + "\tis horn");
                continue;
            }
            Boolean trainSupport = trainMatch.subsumption(rule, 0);
            Boolean testSupport = testMatch.subsumption(rule, 0);
            System.out.println(rule + "\t" + trainSupport + "\t" + testSupport);
        }*/

        Path constraintsSource = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + domain + "\\constraints.txt");
        List<Clause> file = Files.lines(constraintsSource).map(Clause::parse).collect(Collectors.toList());
        Set<IsoClauseWrapper> theoryConstraints = theory.getHardRules().stream().map(LogicUtils::untype).map(IsoClauseWrapper::create).collect(Collectors.toSet());
        Set<IsoClauseWrapper> fileConstraints = file.stream().map(LogicUtils::untype).map(IsoClauseWrapper::create).collect(Collectors.toSet());

        //always, there is a constraint in the theory constraints and not in file constraints... don't know exactly why :(
        System.out.println("in theory, not in constfile");
        Set<Integer> in = Sugar.set();
        Set<Integer> out = Sugar.set();
        for (IsoClauseWrapper rule : theoryConstraints) {
            if (!fileConstraints.contains(rule)) {
                System.out.println(rule.getOriginalClause());
                out.add(LogicUtils.variables(rule.getOriginalClause()).size());
            } else {
                in.add(LogicUtils.variables(rule.getOriginalClause()).size());
            }
        }
        System.out.println("in\t" + in.toString());
        System.out.println("out\t" + out.toString());

        System.out.println("****");
        System.out.println("in const file, not in theory");
        for (IsoClauseWrapper rule : fileConstraints) {
            if (!theoryConstraints.contains(rule)) {
                System.out.println(rule.getOriginalClause());
            }
        }

        /*
        Set<Clause> hardOnly = theory.getHardRules().stream().filter(rule -> {
            if (LogicUtils.positiveLiterals(rule).size() > 0) {
                return true;
            }
            return fileConstraints.contains(IsoClauseWrapper.create(LogicUtils.untype(rule)));
        }).collect(Collectors.toSet());
        System.out.println("new constraints size\t" + hardOnly.size());
        Files.write(Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\lessConstraints.ptt"),
                Sugar.list(Possibilistic.create(hardOnly, theory.getSoftRules()).asOutput()));
        // lessConstraints.ptt je soubor ktery vzniknul pridanim pouze puvodnich constraints v domain/constraints.txt namisto tech rozburelych constraints-2-4 ktere ani nevim jak poradne vznikly
         */
    }

    private static void showPR() {
        Utils u = Utils.create();

//        String domain = "nationsA2";
//        String domain = "kinships";
        String domain = "umls";

        Integer precise = 500;

        List<Pair<Path, Integer>> paths = Sugar.list();
        if ("kinships".equals(domain)) {
            paths.addAll(Sugar.list(
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-ad.AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c467.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c467.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-ad.AB-100.5.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
            ));
            precise = 500; // nebo 500!
        } else if ("nationsA2".equals(domain)) {
            paths.addAll(Sugar.list(
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-ad.AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-ad.AB-100.5.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
            ));
            precise = 400;
        } else if ("umls".equals(domain)) {
            paths.addAll(Sugar.list(
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-ad.AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-ad.AB-100.5.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
            ));
            precise = 350; // ideally 450
        }

        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        String res = u.plotPRs(groundEvidence, paths, 100, precise);
        System.out.println(res);
    }

    private static void showAucPR2() {
        String domain = "kinships";
//        String domain = "nationsA2";
//        String domain = "umls";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        Set<Literal> allTest = LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "test.db"));

        List<Pair<Path, Integer>> paths = Sugar.list();
        if ("kinships".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
                    //new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-ad.AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c467.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c467.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)

                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-ad.AB-100.5.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)

//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-AR.t3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-AR.t4.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
            ));
        } else if ("nationsA2".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-AR.default.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-ad.AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)

                    , new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-ad.AB-100.5.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)

//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-AR.t3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-AR.t4.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
            ));
        } else if ("umls".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-AR.default.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-ad.AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
                    , new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-ad.AB-100.5.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)

//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
            ));
        }

//        String res = u.plotAUCPRInterpolated(groundEvidence, paths);
        String res = u.plotExecutionTime(paths.subList(1, paths.size())); // these are filtered-corrupted
//        String res = u.plotAUCPRInterpolatedOptimized(groundEvidence,paths);
//        String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);//kdyz se to pocita tak se nesmi odebrat evidence, jinak to pujde zakonite dolu!

    }


    private static void showAucPR() {
//        String domain = "kinships";
        String domain = "nationsA2";
//        String domain = "umls";
        Path groundEvidence = Paths.get("..", "datasets", domain, "test" + ("protein".equals(domain) ? ".symmetric" : "") + ".db");
        Utils u = Utils.create();

        Set<Literal> allTest = LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "test.db"));

        List<Pair<Path, Integer>> paths = Sugar.list();
        if ("kinships".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-kinships.d3.minis1.AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-kinships.d3.minis1.AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-kinships.d3.minis1.AO.pss.CE.r5_mv5.logic.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c467.fol.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c467.fol.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\kinships\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c467.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
            ));
        } else if ("nationsA2".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt_k-5_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)

//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c2196.fol.poss.k5.CS.ptt_k-7_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c2196.fol.poss.k5.CS.ptt_k-7_em-KWC_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)


//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-AR.default.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-AR.mins2.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\nationsA2\\10src-tu_t-AR.m2-10.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)

            ));
        } else if ("umls".equals(domain)) {
            paths.addAll(Sugar.list(
                    new Pair<>(Paths.get("..", "datasets", domain, "test-uniform"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-0_em-A_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1254.fol.poss.k5.CS.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)

//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-lessConstraints.ptt_k-5_em-K_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-AR.default.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-AR.ml3.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-AR.mins2.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1),
//                    new Pair<>(Paths.get("D:\\dev\\pac-dissertation\\umls\\10src-tu_t-AR.m2-10.poss_k-0_em-OS_l-PL_is-f_dd-f_cpr-f_scout-f_sat-none_ms-f_alg-support"), 1)


            ));
        }

        //String res = u.plotAUCPRInterpolated(groundEvidence, paths, );
        String res = u.plotAUCPRInterpolated(groundEvidence, paths, allTest); // these are filtered-corrupted
//        String res = u.plotAUCPRInterpolatedOptimized(groundEvidence,paths);
//        String res = u.plotAUCPR(groundEvidence, paths, true, true);
        System.out.println(res);//kdyz se to pocita tak se nesmi odebrat evidence, jinak to pujde zakonite dolu!


    }

    private static void customIJCAIInference() {
//        String domain = "uwcs";
        String domain = "protein";

        // uwcs
        Path theorySource = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac\\" + domain + "\\ICAJ.CS.poss");

        Path typed = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", domain, "typing.txt");
        Path queries = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", domain, "queries");
        Possibilistic theory = Possibilistic.create(theorySource);

        for (Integer atMost : Sugar.list(2000)) {

            System.setProperty("atMost", "" + atMost);
            System.setProperty("inference.modulo", "1");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");

            // oneS
            /**/
            System.setProperty("ida.pacReasoning.entailment.mode", "oneS");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            Pipeline.runInference(theory, queries, typed, domain);
            /**/
        }

//    }

    }

    private static void customInference() {
//        String domain = "kinships";
//        String domain = "nationsA2";
        String domain = "umls";// docuit AB na nations2
        //Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\AR.ml3.poss");
        //Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\AR.ml4.poss");

//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\ad.AR.ml3.poss");
        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\ad.AB-100.5.poss");

        /*
        for (String setting : Sugar.list("ml3", "ml4", "mins2", "m2-10", "default")) {
            Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\AR." + setting + ".poss");
            if (!theorySource.toFile().exists()) {
                continue;
            }
            */
//        Path theorySource = Paths.get("D:\\dev\\pac-resubmit\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c522.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1l.fol.poss.k5.CS.ptt"); // handmade constraints

        // kinships

//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c467.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\AR.default.poss");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\AR.ml3.poss");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\kinships.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c467.fol.poss.k5.CS.ptt");

//         nations
//        Path theorySource = Paths.get("D:\\dev\\pac-resubmit\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c2196.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1476.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c1476.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\nationsA2.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt");

//         umls
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c2878.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d4_mv5_ttrue_c1254.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c1254.fol.poss.k5.CS.ptt");
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\umls.db.typed.amieOutput.postprocess.r5_mv5.logic.poss.k5.CS.ptt");

        // general less constriants
//        Path theorySource = Paths.get("D:\\dev\\pac-dissertation\\" + domain + "\\lessConstraints.ptt");


        Path typed = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", domain, "typing.txt");
        Path queries = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", domain, "test-uniform");
        Possibilistic theory = Possibilistic.create(theorySource);

        int k = 5;

        //a zkusit without-constraints s k=4????

        for (Integer atMost : Sugar.list(3000)) {

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

            /** /  // PL
             System.setProperty("ida.pacReasoning.entailment.mode", "a");
             System.setProperty("ida.pacReasoning.entailment.k", "0");
             System.setProperty("ida.pacReasoning.entailment.logic", "pl");
             //        System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
             Pipeline.runInference(theory, queries, typed, domain);
             /**/


            /** /    // kPL/WC
             System.setProperty("ida.pacReasoning.entailment.mode", "kWC"); // k without constraints
             //            System.setProperty("ida.pacReasoning.entailment.mode", "k"); // k entailment
             System.setProperty("ida.pacReasoning.entailment.k", "" + k);
             System.setProperty("ida.pacReasoning.entailment.logic", "pl");
             System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
             Pipeline.runInference(theory, queries, typed, domain);
             /**/

        }

//    }

    }

    private static void amieOutputToTheory(Path path, Path storeTo) {
        if (!path.toFile().exists()) {
            System.out.println("ending since I the input file doesn't exist");
            return;
        }
        if (storeTo.toFile().exists()) {
            System.out.println("ending since I do not want to rewrite already existing file !");
//            return;
        }
//        Reformatter r = Reformatter.create();
//        Collection<Clause> clauses = r.loadAmieOutput(path);
//        Set<IsoClauseWrapper> selected = clauses.stream()
////                .filter(c -> LogicUtils.variables(c).size() <= maxVariables)
//                .map(HornClause::new)
//                .filter(LogicUtils::isRangeRestricted)
//                //.map(saturator::saturate) // TODO this given here because it takes to much time to saturate 200k of rules
//                .filter(Objects::nonNull)
//                .map(IsoClauseWrapper::create)
//                .collect(Collectors.toSet());

        RulesLike rl = new RulesLike();
        List<LearnedRule> rules = rl.loadAmie(path);
        List<Pair<Double, Clause>> horn = Sugar.list();
        for (LearnedRule rule : rules) {
            HornClause hc = HornClause.create(rule.getRule());
            if (LogicUtils.isRangeRestricted(hc) && LogicUtils.constants(rule.getRule()).isEmpty()) {
                horn.add(new Pair<>(Double.valueOf(rule.getProperty("pcaconf")), rule.getRule()));
            }
        }
        Possibilistic theory = Possibilistic.create(Sugar.set(), horn);
        try {
            File parent = storeTo.getParent().toFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            Files.write(storeTo, Sugar.list(theory.asOutput()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    private static void anyburlOutputToTheory(Path path, Path storeTo, double minConfidence) {
        if (!path.toFile().exists()) {
            System.out.println("ending since I the input file doesn't exist");
            return;
        }
        if (storeTo.toFile().exists()) {
            System.out.println("ending since I do not want to rewrite already existing file !");
//            return;
        }

        RulesLike rl = new RulesLike();
        List<LearnedRule> rules = rl.loadAnyBurl(path);
        List<Pair<Double, Clause>> horn = Sugar.list();
        for (LearnedRule rule : rules) {
            HornClause hc = HornClause.create(rule.getRule());
            if (LogicUtils.isRangeRestricted(hc) && LogicUtils.constants(rule.getRule()).isEmpty() && Double.valueOf(rule.getProperty("confidence")) >= minConfidence) {
                horn.add(new Pair<>(Double.valueOf(rule.getProperty("confidence")), rule.getRule()));
            }
        }
        Possibilistic theory = Possibilistic.create(Sugar.set(), horn);
        try {
            File parent = storeTo.getParent().toFile();
            if (!parent.exists()) {
                parent.mkdirs();
            }
            Files.write(storeTo, Sugar.list(theory.asOutput()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
