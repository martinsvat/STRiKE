package ida.pacReasoning;

import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.pacReasoning.data.PACAccuracyDataset;
import ida.pacReasoning.entailment.*;
import ida.pacReasoning.entailment.collectors.Entailed;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.entailment.cuts.*;
import ida.pacReasoning.entailment.theories.FOL;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.entailment.theories.StatefullConstantGammaLearner;
import ida.pacReasoning.entailment.theories.Theory;
import ida.pacReasoning.evaluation.Data;
import ida.pacReasoning.evaluation.GammaPlotter;
import ida.pacReasoning.evaluation.Utils;
import ida.pacReasoning.supportEntailment.SupportEntailmentInference;
import ida.pacReasoning.thrashDeprected.*;
import ida.utils.Statistics;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.learning.saturation.ConjunctureSaturator;
import logicStuff.learning.saturation.RuleSaturator;
import logicStuff.typing.TypesInducer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 22. 3. 2018.
 */
public class TestRunner {


    public static void main(String[] args) throws IOException {
//        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");
//        System.out.println("threads:\t" + System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));
//
//        System.out.println("dalsi datasety, minimum!");
//
//        System.out.println("ohodnotit i pravidla naucena pomoci negative rules a ty ktere uz mam z amie na datasetech jako kinships nations");
//        System.out.println("pro yago datasety pustit inferenci na states, musician, people");
//        System.out.println("ukazat nasamplovat train data pro gamu pro yago poddatasety");
//
//        System.out.println("vse srovnavat oproti klasickemu entailmentu");
//        System.out.println("open world - yago subdatasets");
//        System.out.println("pro yago vzit amie pravidla bez constraints a vyevaluovat, a potom i s constraints... ale zahodit skore pomoci nasi accuracy");
//
//        System.out.println("na tom udelat k-entailment inferenci");
//        System.out.println("dopsat preliminaries");
//        System.out.println("sepsat experimenty");
//        System.out.println("amie inferenci na yago sub-datasetech pomoci vsech pravidel najitych pomoci amie");
//        System.out.println("uwcs s voting entailmentem abychom zobrazili rozdil oproti scatter plotu");
//
//        System.out.println("do minima -- pridat obrazek nations a trebas i proteins");
//        System.out.println("do minima -- grafy bez constraints");
//
//        System.out.println("do clanku -- nations dataset binarnimi predikcemi pouze, na tom vyzkouset jestli jsme schopni upocitat vice");
//        System.out.println("rozbehat countries dataset domenu z NTlambda");
//        System.out.println("vygenerovani train examplu pro hits -- jak velke examply pro NTlambda, jak velke okoli pro nas");
//        System.out.println("hits evaluace (logaritmicka evaluace napr) + ve finalni vrstve se da udelat nejake prorezani aby se nepocitaly uz dalsi kdy mame co jsme nasli (nebo v posledni vrstve pocitat pouze ty daneho literalu)");
//        System.out.println("rozbehat NTlambda a sorvnat se na hitts -- jak myslel to nauceni jejich modelu?");
//
//        System.out.println("poslat Ondrovi graf bez symmetrii na proteins oproti MLNs");
//        System.out.println("countries s2 by mohl byt vyresen voting entailmentem ");
//        System.out.println("pripravit data (test examply) a inferenci na HITs (asi 10, 50, 1000)");
//        System.out.println("NT datasety -- preformatovat, naucit se constraints, naucit se teorie beamem, pustit inferenci");

//        filterEquals();
//        runKinships();
//        runYago();
//        runInferences();
//        runInference();
//        learnConstRatioGammas();
//        plotRatiosHistogram();
//        plotResultsSampled();
//        plotFPFN();
//        plotFinal();
//        plotResults();
//        learnGammas();
//        plotFPFNwrtRatios();
//        clearSampledData();
//        plotFPFNByDifferentK();

        //plotScatter();
//        filterSymmetry();

//        checkVE();
//        fitGammas();
        //plotResults();
//        plotDiff();
//        plotStats();
//        compare();
//        plotStats();

//        yagoIncorporateConstraints();
//        selectRules();
//        aggregateRules();

//        scoreRules();

//        subselectRules();

//        System.out.println("nations\t" + Utils.loadEvidence(Paths.get("..","datasets","nations","train.db")).size());
//        System.out.println("umls\t" + Utils.loadEvidence(Paths.get("..","datasets","umls","train.db")).size());
//        System.out.println("uwcs\t" + Utils.loadEvidence(Paths.get("..","datasets","uwcs","train.db")).size());
//        System.out.println("protein\t" + Utils.loadEvidence(Paths.get("..","datasets","protein","train.db")).size());
//        System.out.println("kinships\t" + Utils.loadEvidence(Paths.get("..","datasets","kinships","train.db")).size());

//        System.out.println(Utils.get().predicates(Paths.get("..","datasets","nations","train.db")).size());
//        System.out.println(Utils.get().predicates(Paths.get("..","datasets","umls","train.db")).size());
//        System.out.println(Utils.get().predicates(Paths.get("..","datasets","uwcs","train.db")).size());
//        System.out.println(Utils.get().predicates(Paths.get("..","datasets","protein","train.db")).size());
//        System.out.println(Utils.get().predicates(Paths.get("..","datasets","kinships","train.db")).size());
        //System.out.println(Utils.get().predicates(Paths.get("..","datasets","mlns","kinships","train.db")).size());
        //System.out.println(Utils.get().predicates(Paths.get(".","pac","yago2","test.db")).size());
//        System.out.println(LogicUtils.constants(Utils.loadEvidence(Paths.get(".","pac","yago2","train.db"))).size());
//        System.out.println(Utils.get().predicates(Paths.get(".", "pac", "yago2", "train.db")).size());
//        System.out.println(LogicUtils.constants(Utils.loadEvidence(Paths.get("..", "datasets", "uwcs-queries", "test.db"))).size());
//                System.out.println(LogicUtils.constants(Utils.loadEvidence(Paths.get("..", "datasets", "mlns","kinships", "train.db"))).size());
//        System.out.println(Utils.loadEvidence(Paths.get("..", "datasets", "uwcs-queries", "test.db")).size());
//        System.out.println(Utils.get().predicates(Paths.get("..", "datasets", "uwcs-queries", "test.db")).size());

//        yagoDataset();

//        fillInSymmetreis();
        //findHorns();
//        HornLearner.parse();

        //computeHitsScore();
//        pickRules();
//        runTest();

//        debugHits();

//        supportEntailment();
        //runEntailmentsEvaluation();
//        compare();


//        runInference();
        plotUCWStheories();

//        typeTheory();

        System.out.println("u umls a nations to nejspise bude chtit vice pravidel aby to vubec neco dokazovalo");
        System.out.println("dopsat vypsani tabulky hits vysledku, pustit runTest bez constraints");
        System.out.println("spusteny s constrainama z 70 trainu to nic nedokazuje, asi nekonzistence");

        System.out.println("TBD hits inference -- udelat randomizaci");

//        debugDTLOutput();
    }


    private static void typeTheory() throws IOException {
        String domain = "uwcs";
        Map<Pair<Predicate, Integer>, String> typing = TypesInducer.load(Paths.get("..", "datasets", domain, "typing.txt"));

        for (String file : Sugar.list("icajPredictiveMyConstraints.poss"
                , "icajPredictive.poss")) {
            Path path = Paths.get(".", "pac", domain, file);
            System.out.println("typing\t" + path);
            Possibilistic pl = Possibilistic.create(path);
            pl = pl.addTyping(typing);
//            pl = pl.untype();
            Files.write(path, Sugar.list(pl.asOutput()));
        }

    }

    private static void plotUCWStheories() {
        Path evidenceFolder = Paths.get("..", "datasets", "uwcs", "test.db");
        Utils u = Utils.create();
/*
        nekde bude chyba pri inferenci poss logiky :( pouziva to spravny algoritmus?
                je to dost mozná kvuli typum :(
*/
        List<Pair<Path, Integer>> inputs = Sugar.list(
                new Pair<>(Paths.get(".", "pac", "uwcs", "mlns"), 1)
                , new Pair<>(Paths.get(".", "pac", "uwcs", "src-queries_t-icajPredictiveMyConstraints.poss_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 10)
                , new Pair<>(Paths.get(".", "pac", "uwcs", "src-queries_t-src_train.db.typed.uni0.5.db_theory_r15_bs5_d4_ttrue_c116.fol.filtered.poss_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t_alg-null"), 10)
        );
        String plot = u.plotCHED(evidenceFolder, inputs, false);
        System.out.println(plot);
    }

    private static void debugDTLOutput() throws IOException {
        // najite podminky
        //List<String> smarter = Sugar.list("	!location(V2, V2)", "	!function(V2, V2)", "	!complex(V2, V2)        ", "	!enzyme(V2, V2)        ", "	!protein_class(V2, V2)        ", "	!phenotype(V2, V2)        ", "	phenotype(V2, V2), !location(V3, V2)        ", "	phenotype(V2, V2), !function(V2, V4)        ", "	phenotype(V2, V2), !complex(V3, V2)        ", "	phenotype(V2, V2), !enzyme(V2, V4)        ", "	phenotype(V2, V2), !protein_class(V3, V2)        ", "	phenotype(V2, V2), !interaction(V2, V4)        ", "	phenotype(V2, V2), !interaction(V2, V2)        ", "	phenotype(V2, V2), !phenotype(V3, V2)        ", "	complex(V1, V2), !location(V1, V4)        ", "	complex(V1, V2), !function(V1, V4)        ", "	complex(V1, V2), !complex(V3, V2)        ", "	complex(V1, V2), !enzyme(V2, V4)        ", "	complex(V1, V2), !protein_class(V3, V1)        ", "	complex(V1, V2), !interaction(V3, V2)        ", "	complex(V1, V2), !interaction(V1, V1)        ", "	complex(V1, V2), !phenotype(V1, V4)        ", "	interaction(V2, V2), !location(V3, V2)        ", "	interaction(V2, V2), !function(V2, V4)        ", "	interaction(V2, V2), !complex(V3, V2)        ", "	interaction(V2, V2), !enzyme(V2, V4)        ", "	interaction(V2, V2), !protein_class(V3,V2)        ", "	interaction(V2, V2), !phenotype(V3, V2)        ", "	!phenotype(V1, V2), !location(V1, V4)        ", "	!phenotype(V1, V2), !location(V3, V1)        ", "	!phenotype(V1, V2), !location(V2, V1)        ", "	!phenotype(V1, V2), !location(V3, V2)        ", "	!phenotype(V1, V2), !location(V2, V4)        ", "	!phenotype(V1, V2), !location(V1, V2)        ", "	!phenotype(V1, V2), !function(V3, V1)        ", "	!phenotype(V1, V2), !function(V3, V2)        ", "	!phenotype(V1, V2), !function(V1, V2)        ", "	!phenotype(V1, V2), !function(V2, V4)        ", "	!phenotype(V1, V2), !function(V1, V4)        ", "	!phenotype(V1, V2), !function(V2, V1)        ", "	!phenotype(V1, V2), !complex(V2, V1)        ", "	!phenotype(V1, V2), !complex(V3, V1)        ", "	!phenotype(V1, V2), !complex(V1, V4)        ", "	!phenotype(V1, V2), !complex(V3, V2)        ", "	!phenotype(V1, V2), !complex(V2, V4)        ", "	!phenotype(V1, V2), !complex(V1, V2)        ", "	!phenotype(V1, V2), !enzyme(V2, V4)        ", "	!phenotype(V1, V2), !enzyme(V1, V4)        ", "	!phenotype(V1, V2), !enzyme(V3, V1)        ", "	!phenotype(V1, V2), !enzyme(V1, V2)        ", "	!phenotype(V1, V2), !enzyme(V3, V2)        ", "	!phenotype(V1, V2), !enzyme(V2, V1)        ", "	!phenotype(V1, V2), !protein_class(V3, V2)        ", "	!phenotype(V1, V2), !protein_class(V3, V1)        ", "	!phenotype(V1, V2), !protein_class(V1, V2)        ", "	!phenotype(V1, V2), !protein_class(V2, V4)        ", "	!phenotype(V1, V2), !protein_class(V1, V4)        ", "	!phenotype(V1, V2), !protein_class(V2, V1)        ", "	!phenotype(V1, V2), interaction(V3, V1)        ", "	!phenotype(V1, V2), !interaction(V1, V1)        ", "	!phenotype(V1, V2), !interaction(V3, V2)        ", "	!phenotype(V1, V2), !interaction(V2, V1)        ", "	!phenotype(V1, V2), !interaction(V1, V2)        ", "	!phenotype(V1, V2), !interaction(V2, V2)        ", "	!phenotype(V1, V2), !interaction(V1, V4)        ", "	!phenotype(V1, V2), !interaction(V2, V4)        ", "	!phenotype(V1, V2), !interaction(V3, V1)        ", "	!phenotype(V1, V2), !phenotype(V2, V4)        ", "	!phenotype(V1, V2), !phenotype(V2, V1)        ", "	!phenotype(V1, V2), !phenotype(V1, V4)        ", "	!phenotype(V1, V2), !phenotype(V3, V2)        ", "	!function(V1, V2), !location(V2, V4)        ", "	!function(V1, V2), !location(V1, V4)        ", "	!function(V1, V2), !location(V3, V1)        ", "	!function(V1, V2), !location(V2, V1)        ", "	!function(V1, V2), !location(V1, V2)        ", "	!function(V1, V2), !location(V3, V2)        ", "	!function(V1, V2), !function(V1, V4)        ", "	!function(V1, V2), !function(V3, V2)        ", "	!function(V1, V2), !function(V2, V4)        ", "	!function(V1, V2), !function(V2, V1)        ", "	!function(V1, V2), !complex(V2, V4)        ", "	!function(V1, V2), !complex(V3, V1)        ", "	!function(V1, V2), !complex(V1, V2)        ", "	!function(V1, V2), !complex(V3, V2)        ", "	!function(V1, V2), !complex(V1, V4)        ", "	!function(V1, V2), !complex(V2, V1)        ", "	!function(V1, V2), !enzyme(V2, V4)        ", "	!function(V1, V2), !enzyme(V1, V4)        ", "	!function(V1, V2), !enzyme(V3, V1)        ", "	!function(V1, V2), !enzyme(V3, V2)        ", "	!function(V1, V2), !enzyme(V2, V1)        ", "	!function(V1, V2), !enzyme(V1, V2)        ", "	!function(V1, V2), !protein_class(V2, V4)        ", "	!function(V1, V2), !protein_class(V3, V1)        ", "	!function(V1, V2), !protein_class(V1, V2)        ", "	!function(V1, V2), !protein_class(V3, V2)        ", "	!function(V1, V2), !protein_class(V1, V4)        ", "	!function(V1, V2), !protein_class(V2, V1)        ", "	!function(V1, V2), interaction(V3, V2)        ", "	!function(V1, V2), !interaction(V3, V2)        ", "	!function(V1, V2), !interaction(V1, V4)        ", "	!function(V1, V2), !interaction(V3, V1)        ", "	!function(V1, V2), !interaction(V2, V1)        ", "	!function(V1, V2), !interaction(V1, V2)        ", "	!function(V1, V2), !interaction(V1, V1)        ", "	!function(V1, V2), !interaction(V2, V2)        ", "	!function(V1, V2), !interaction(V2, V4)        ", "	!protein_class(V1, V2), !location(V2, V4)        ", "	!protein_class(V1, V2), !location(V3, V1)        ", "	!protein_class(V1, V2), !location(V3, V2)        ", "	!protein_class(V1, V2), !location(V1, V4)        ", "	!protein_class(V1, V2), !location(V2, V1)        ", "	!protein_class(V1, V2), !location(V1, V2)        ", "	!protein_class(V1, V2), !complex(V2, V4)        ", "	!protein_class(V1, V2), !complex(V1, V4)        ", "	!protein_class(V1, V2), !complex(V2, V1)        ", "	!protein_class(V1, V2), !complex(V1, V2)        ", "	!protein_class(V1, V2), !complex(V3, V1)        ", "	!protein_class(V1, V2), !complex(V3, V2)        ", "	!protein_class(V1, V2), !enzyme(V1, V2)        ", "	!protein_class(V1, V2), !enzyme(V1, V4)        ", "	!protein_class(V1, V2), !enzyme(V3, V2)        ", "	!protein_class(V1, V2), !enzyme(V3, V1)        ", "	!protein_class(V1, V2), !enzyme(V2, V4)        ", "	!protein_class(V1, V2), !enzyme(V2, V1)        ", "	!protein_class(V1, V2), !protein_class(V1, V4)        ", "	!protein_class(V1, V2), !protein_class(V2, V1)        ", "	!protein_class(V1, V2), !protein_class(V2, V4)        ", "	!protein_class(V1, V2), !protein_class(V3, V2)        ", "	!protein_class(V1, V2), interaction(V3, V2)        ", "	!protein_class(V1, V2), !interaction(V2, V1)        ", "	!protein_class(V1, V2), !interaction(V1, V4)        ", "	!protein_class(V1, V2), !interaction(V2, V2)        ", "	!protein_class(V1, V2), !interaction(V3, V2)        ", "	!protein_class(V1, V2), !interaction(V1, V2)        ", "	!protein_class(V1, V2), !interaction(V1, V1)        ", "	!protein_class(V1, V2), !interaction(V3, V1)        ", "	!protein_class(V1, V2), !interaction(V2, V4)        ", "	interaction(V1, V2), !location(V2, V4)        ", "	interaction(V1, V2), !complex(V3, V2)        ", "	interaction(V1, V2), !enzyme(V3, V1)        ", "	!location(V1, V2), !location(V3, V2)        ", "	!location(V1, V2), !location(V2, V4)        ", "	!location(V1, V2), !location(V1, V4)        ", "	!location(V1, V2), !location(V2, V1)        ", "	!location(V1, V2), !complex(V1, V4)        ", "	!location(V1, V2), !complex(V3, V1)        ", "	!location(V1, V2), !complex(V2, V4)        ", "	!location(V1, V2), !complex(V2, V1)        ", "	!location(V1, V2), !complex(V1, V2)        ", "	!location(V1, V2), !complex(V3, V2)        ", "	!location(V1, V2), !enzyme(V1, V4)        ", "	!location(V1, V2), !enzyme(V3, V2)        ", "	!location(V1, V2), !enzyme(V1, V2)        ", "	!location(V1, V2), !enzyme(V2, V4)        ", "	!location(V1, V2), !enzyme(V3, V1)        ", "	!location(V1, V2), !enzyme(V2, V1)        ", "	!location(V1, V2), !interaction(V2, V1)        ", "	!location(V1, V2), !interaction(V3, V2)        ", "	!location(V1, V2), !interaction(V1, V1)        ", "	!location(V1, V2), !interaction(V2, V4)        ", "	!location(V1, V2), !interaction(V1, V4)        ", "	!location(V1, V2), !interaction(V1, V2)        ", "	!location(V1, V2), !interaction(V2, V2)      ", "	!location(V1, V2), !interaction(V3, V1)        ", "	!interaction(V1, V2), !complex(V3, V1)        ", "	!interaction(V1, V2), !complex(V2, V1)        ", "	!interaction(V1, V2), !complex(V3, V2)        ", "	!interaction(V1, V2), !complex(V1, V4)        ", "	!interaction(V1, V2), !complex(V1, V2)        ", "	!interaction(V1, V2), !complex(V2, V4)        ", "	!interaction(V1, V2), !enzyme(V2, V1)        ", "	!interaction(V1, V2), !enzyme(V1, V2)        ", "	!interaction(V1, V2), !enzyme(V1, V4)        ", "	!interaction(V1, V2), !enzyme(V2, V4)        ", "	!interaction(V1, V2), !enzyme(V3, V1)        ", "	!interaction(V1, V2), !enzyme(V3, V2)        ", "	!interaction(V1, V2), !interaction(V1, V4)        ", "	!interaction(V1, V2), !interaction(V2, V1)        ", "	!interaction(V1, V2), !interaction(V1, V1)        ", "	!interaction(V1, V2), !interaction(V2, V2)        ", "	!interaction(V1, V2), !interaction(V2, V4)        ", "	!interaction(V1, V2), !interaction(V3, V2)        ", "	!enzyme(V1, V2), !complex(V3, V1)        ", "	!enzyme(V1, V2), !complex(V3, V2)        ", "	!enzyme(V1, V2), !complex(V1, V2)        ", "	!enzyme(V1, V2), !complex(V2, V4)        ", "	!enzyme(V1, V2), !complex(V1, V4)        ", "	!enzyme(V1, V2), !complex(V2, V1)        ", "	!enzyme(V1, V2), !enzyme(V2, V1)        ", "	!enzyme(V1, V2), !enzyme(V2, V4)        ", "	!enzyme(V1, V2), !enzyme(V3, V2)        ", "	!enzyme(V1, V2), !enzyme(V1, V4)        ", "	!enzyme(V1, V2), !interaction(V2, V2)        ", "	!enzyme(V1, V2), !interaction(V1, V1)        ", "	!interaction(V2, V2), !complex(V2, V4)        ", "	!interaction(V2, V2), !complex(V3, V2)        ", "	!complex(V1, V2), !complex(V3, V2)        ", "	!complex(V1, V2), !complex(V1, V4)        ", "	!complex(V1, V2), !complex(V2, V4)        ", "	!complex(V1, V2), !complex(V2, V1)");
        //List<String> complete = Sugar.list("!complex(V2, V2)", "	!phenotype(V2, V2)", "	!function(V2, V2)", "	!protein_class(V2, V2)", "	!location(V2, V2)", "	!enzyme(V2, V2)", "	!phenotype(V1, V2), !function(V2, V1)", "	!phenotype(V1, V2), !protein_class(V2, V1)", "	!phenotype(V1, V2), !location(V2, V4)", "	!phenotype(V1, V2), !protein_class(V3, V2)", "	!phenotype(V1, V2), !location(V3, V2)", "	!phenotype(V1, V2), !enzyme(V2, V4)", "	!phenotype(V1, V2), !phenotype(V2, V1)", "	!phenotype(V1, V2), !complex(V1, V2)", "	!phenotype(V1, V2), !interaction(V2, V1)", "	!phenotype(V1, V2), !function(V3, V1)", "	!phenotype(V1, V2), !complex(V3, V2)", "	!phenotype(V1, V2), !interaction(V2, V2)", "	!phenotype(V1, V2), !complex(V2, V4)", "	!phenotype(V1, V2), !phenotype(V2, V4)", "	!phenotype(V1, V2), !function(V3, V2)", "	!phenotype(V1, V2), !protein_class(V3, V1)", "	!phenotype(V1, V2), !enzyme(V2, V1)", "	!phenotype(V1, V2), !protein_class(V2, V4)", "	!phenotype(V1, V2), !location(V1, V2)", "	!phenotype(V1, V2), !enzyme(V1, V2)", "	!phenotype(V1, V2), !enzyme(V3, V1)", "	!interaction(V3, V2), !phenotype(V1, V2)", "	!phenotype(V1, V2), !interaction(V1, V2)", "	!phenotype(V1, V2), !location(V2, V1)", "	!phenotype(V1, V2), !complex(V3, V1)", "	!phenotype(V1, V2), !protein_class(V1, V2)", "	!phenotype(V1, V2), !interaction(V2, V4)", "	!phenotype(V1, V2), !location(V3, V1)", "	!phenotype(V1, V2), !function(V1, V2)", "	!phenotype(V1, V2), !complex(V2, V1)", "	!phenotype(V1, V2), !enzyme(V3, V2)", "	!phenotype(V1, V2), !function(V2, V4)", "	!function(V1, V2), !location(V3, V2)", "	!function(V1, V2), !protein_class(V3, V1)", "	!enzyme(V3, V1), !function(V1, V2)", "	!protein_class(V1, V2), !function(V1, V2)", "	!complex(V2, V1), !function(V1, V2)", "	!function(V1, V2), !function(V2, V4)", "	!interaction(V3, V2), !function(V1, V2)", "	!complex(V2, V4), !function(V1, V2)", "	!protein_class(V2, V1), !function(V1, V2)", "	!location(V1, V2), !function(V1, V2)", "	!enzyme(V2, V1), !function(V1, V2)", "	!protein_class(V2, V4), !function(V1, V2)", "	!function(V1, V2), !function(V2, V1)", "	!location(V2, V1), !function(V1, V2)", "	!function(V1, V2), !location(V2, V4)", "	!interaction(V2, V2), !function(V1, V2)", "	!enzyme(V1, V2), !function(V1, V2)", "	!function(V1, V2), !complex(V3, V1)", "	!function(V1, V2), !location(V3, V1)", "	!function(V1, V2), !protein_class(V3, V2)", "	!function(V1, V2), !interaction(V2, V4)", "	!complex(V1, V2), !function(V1, V2)", "	!complex(V3, V2), !function(V1, V2)", "	!enzyme(V2, V4), !function(V1, V2)", "	!function(V1, V2), !interaction(V2, V1)", "	!enzyme(V3, V2), !function(V1, V2)", "	!function(V1, V2), !interaction(V1, V2)", "	!protein_class(V1, V2), !protein_class(V2, V1)", "	!enzyme(V2, V1), !protein_class(V1, V2)", "	!interaction(V3, V2), !protein_class(V1, V2)", "	!protein_class(V1, V2), !location(V1, V2)", "	!protein_class(V1, V2), !interaction(V2, V2)", "	!protein_class(V1, V2), !complex(V3, V1)", "	!complex(V2, V1), !protein_class(V1, V2)", "	!enzyme(V1, V2), !protein_class(V1, V2)", "	!protein_class(V1, V2), !location(V3, V1)", "	!protein_class(V1, V2), !protein_class(V2, V4)", "	!complex(V1, V2), !protein_class(V1, V2)", "	!protein_class(V1, V2), !location(V2, V1)", "	!protein_class(V1, V2), !enzyme(V2, V4)", "	!complex(V3, V2), !protein_class(V1, V2)", "	!enzyme(V3, V1), !protein_class(V1, V2)", "	!complex(V2, V4), !protein_class(V1, V2)", "	!protein_class(V1, V2), !interaction(V2, V1)", "	!protein_class(V1, V2), !enzyme(V3, V2)", "	!protein_class(V1, V2), !location(V3, V2)", "	!protein_class(V1, V2), !interaction(V1, V2)", "	!protein_class(V1, V2), !location(V2, V4)", "	!protein_class(V1, V2), !interaction(V2, V4)", "	!location(V3, V1), !interaction(V1, V2)", "	!enzyme(V3, V1), !interaction(V1, V2)", "	!enzyme(V1, V2), !interaction(V1, V2)", "	!complex(V3, V2), !interaction(V1, V2)", "	!complex(V1, V2), !interaction(V1, V2)", "	!location(V1, V2), !interaction(V1, V2)", "	!enzyme(V3, V2), !interaction(V1, V2)", "	!complex(V2, V1), !interaction(V1, V2)", "	!location(V2, V1), !interaction(V1, V2)", "	!interaction(V1, V2), !location(V3, V2)", "	!complex(V3, V1), !interaction(V1, V2)", "	!enzyme(V2, V1), !interaction(V1, V2)", "	!location(V1, V2), !location(V2, V1)", "	!complex(V2, V4), !location(V1, V2)", "	!complex(V1, V2), !location(V1, V2)", "	!location(V1, V2), !enzyme(V3, V2)", "	!enzyme(V3, V1), !location(V1, V2)", "	!complex(V3, V2), !location(V1, V2)", "	!enzyme(V1, V2), !location(V1, V2)", "	!enzyme(V2, V1), !location(V1, V2)", "	!complex(V2, V1), !location(V1, V2)", "	!location(V1, V2), !location(V2, V4)", "	!location(V1, V2), !enzyme(V2, V4)", "	!location(V1, V2), !complex(V3, V1)", "	!interaction(V2, V2), !location(V1, V2)", "	!complex(V2, V1), !enzyme(V1, V2)", "	!complex(V1, V2), !enzyme(V1, V2)", "	!enzyme(V1, V2), !complex(V3, V1)", "	!complex(V3, V2), !enzyme(V1, V2)", "	!enzyme(V1, V2), !interaction(V2, V2)", "	!enzyme(V1, V2), !enzyme(V2, V4)", "	!enzyme(V1, V2), !enzyme(V2, V1)", "	!complex(V2, V4), !enzyme(V1, V2)", "	!complex(V3, V2), !interaction(V2, V2)", "	!complex(V1, V2), !complex(V2, V4)", "	!complex(V1, V2), !complex(V2, V1)", "	!enzyme(V1, V4), !protein_class(V1, V2), !complex(V1, V5)", "	!enzyme(V1, V2), !phenotype(V1, V4), !interaction(V1, V1)", "	!complex(V1, V4), !enzyme(V1, V2), !interaction(V1, V1)", "	!protein_class(V2, V5), !complex(V2, V4), !interaction(V2, V2)", "	!protein_class(V1, V4), !phenotype(V1, V2), !interaction(V1, V1)");

        // kandidati ve druhe vrstve pro lits=3, vars=2*lits
//        List<String> complete = Sugar.list("location(V2, V1), !phenotype(V1, V2)","!function(V2, V4), interaction(V1, V2)","!function(V1, V4), phenotype(V1, V2)","!phenotype(V2, V4), protein_class(V1, V2)","!interaction(V2, V2), interaction(V2, V4)","!interaction(V1, V2), interaction(V1, V1)","phenotype(V1, V2), !protein_class(V3, V1)","!protein_class(V1, V2), interaction(V1, V1)","!function(V2, V4), location(V1, V2)","!phenotype(V2, V1), function(V1, V2)","!protein_class(V1, V4), interaction(V1, V2)","location(V1, V4), !location(V1, V2)","enzyme(V1, V4), !enzyme(V1, V2)","!function(V1, V2), enzyme(V1, V2)","protein_class(V1, V4), !complex(V1, V2)","location(V1, V2), !function(V3, V1)","protein_class(V1, V2), !phenotype(V2, V1)","protein_class(V1, V2), !enzyme(V1, V2)","protein_class(V1, V4), function(V1, V2)","protein_class(V2, V4), !complex(V1, V2)","enzyme(V1, V2), enzyme(V3, V2)","!protein_class(V1, V2), enzyme(V1, V4)","!function(V2, V1), location(V1, V2)","enzyme(V2, V4), !complex(V1, V2)","!location(V1, V4), protein_class(V1, V2)","!interaction(V1, V2), location(V1, V4)","!function(V1, V2), complex(V3, V2)","complex(V2, V1), !complex(V1, V2)","!protein_class(V1, V2), interaction(V2, V2)","phenotype(V1, V2), !protein_class(V1, V4)","phenotype(V1, V2), phenotype(V1, V4)","!function(V3, V2), protein_class(V1, V2)","!enzyme(V3, V1), function(V1, V2)","!interaction(V1, V2), complex(V1, V2)","protein_class(V1, V2), !location(V1, V2)","!protein_class(V3, V1), interaction(V1, V2)","complex(V2, V4), !protein_class(V1, V2)","!interaction(V2, V2), phenotype(V2, V4)","!function(V2, V1), protein_class(V1, V2)","interaction(V1, V4), function(V1, V2)","phenotype(V1, V2), enzyme(V1, V4)","interaction(V3, V2), !enzyme(V1, V2)","!enzyme(V1, V4), function(V1, V2)","enzyme(V1, V2), !location(V1, V2)","!complex(V1, V2), protein_class(V3, V1)","!interaction(V2, V1), phenotype(V1, V2)","!enzyme(V1, V2), interaction(V1, V1)","!location(V2, V4), function(V1, V2)","enzyme(V2, V4), !phenotype(V1, V2)","!interaction(V2, V2), location(V2, V4)","!interaction(V1, V2), phenotype(V1, V2)","protein_class(V1, V2), !complex(V1, V2)","interaction(V1, V4), protein_class(V1, V2)","protein_class(V1, V2), !protein_class(V1, V4)","protein_class(V1, V4), phenotype(V1, V2)","!protein_class(V1, V2), function(V1, V2)","complex(V2, V4), interaction(V1, V2)","!function(V1, V2), phenotype(V1, V2)","!interaction(V2, V1), function(V1, V2)","!interaction(V2, V4), phenotype(V1, V2)","!function(V1, V2), enzyme(V3, V2)","!interaction(V2, V2), function(V3, V2)","!interaction(V3, V1), phenotype(V1, V2)","!location(V2, V4), phenotype(V1, V2)","complex(V2, V1), !phenotype(V1, V2)","!function(V1, V2), complex(V2, V4)","complex(V1, V4), !location(V1, V2)","!protein_class(V1, V2), enzyme(V2, V1)","enzyme(V1, V4), !complex(V1, V2)","!interaction(V1, V4), function(V1, V2)","interaction(V3, V2), !phenotype(V1, V2)","complex(V2, V4), !interaction(V2, V2)","!function(V3, V1), interaction(V1, V2)","!complex(V1, V2), function(V3, V2)","phenotype(V1, V2), phenotype(V3, V2)","!function(V2, V4), function(V1, V2)","!function(V3, V1), function(V1, V2)","!enzyme(V2, V1), protein_class(V1, V2)","complex(V3, V2), !location(V1, V2)","location(V1, V4), location(V1, V2)","protein_class(V1, V2), interaction(V1, V1)","protein_class(V1, V2), !location(V2, V1)","!phenotype(V1, V2), function(V1, V2)","phenotype(V1, V2), !enzyme(V3, V2)","!phenotype(V3, V1), function(V1, V2)","!interaction(V1, V2), complex(V3, V2)","complex(V2, V4), !complex(V1, V2)","!complex(V1, V2), complex(V3, V1)","phenotype(V1, V2), interaction(V1, V1)","!location(V3, V2), function(V1, V2)","complex(V1, V2), !phenotype(V1, V2)","!enzyme(V1, V2), interaction(V2, V2)","!interaction(V2, V2), enzyme(V2, V4)","!interaction(V1, V2), complex(V3, V1)","!function(V2, V1), interaction(V1, V2)","!phenotype(V3, V2), protein_class(V1, V2)","!interaction(V2, V4), protein_class(V1, V2)","location(V1, V2), location(V3, V2)","!complex(V1, V2), interaction(V1, V2)","!protein_class(V1, V2), enzyme(V2, V4)","!protein_class(V3, V2), interaction(V1, V2)","!protein_class(V2, V1), location(V1, V2)","protein_class(V2, V1), !complex(V1, V2)","location(V2, V4), interaction(V1, V2)","protein_class(V1, V2), !enzyme(V3, V1)","interaction(V1, V4), !complex(V1, V2)","!enzyme(V3, V2), protein_class(V1, V2)","enzyme(V2, V1), !enzyme(V1, V2)","complex(V2, V4), !location(V1, V2)","!complex(V1, V2), location(V3, V1)","phenotype(V3, V2), !complex(V1, V2)","enzyme(V1, V2), interaction(V1, V1)","!function(V1, V2), enzyme(V3, V1)","complex(V2, V4), !phenotype(V1, V2)","!interaction(V1, V2), enzyme(V1, V2)","enzyme(V2, V1), !location(V1, V2)","!function(V2, V1), function(V1, V2)","!location(V3, V1), function(V1, V2)","location(V1, V4), !phenotype(V1, V2)","interaction(V3, V1), !enzyme(V1, V2)","!complex(V1, V2), function(V1, V2)","phenotype(V1, V2), !phenotype(V2, V4)","phenotype(V1, V2), !phenotype(V3, V1)","enzyme(V1, V4), function(V1, V2)","complex(V3, V1), !location(V1, V2)","interaction(V3, V1), function(V1, V2)","interaction(V3, V2), interaction(V1, V2)","!function(V1, V2), complex(V3, V1)","phenotype(V1, V2), !phenotype(V3, V2)","!complex(V1, V2), function(V2, V4)","!interaction(V1, V2), location(V3, V2)","phenotype(V1, V2), !complex(V1, V2)","!interaction(V1, V2), protein_class(V1, V2)","location(V1, V4), phenotype(V1, V2)","phenotype(V2, V1), !complex(V1, V2)","protein_class(V1, V2), !location(V3, V1)","enzyme(V3, V2), !phenotype(V1, V2)","interaction(V3, V1), protein_class(V1, V2)","!protein_class(V1, V2), complex(V3, V1)","!interaction(V2, V2), protein_class(V2, V4)","!enzyme(V1, V2), location(V3, V2)","!function(V1, V2), location(V1, V2)","!complex(V1, V2), function(V2, V1)","phenotype(V1, V2), !enzyme(V3, V1)","!function(V3, V2), interaction(V1, V2)","complex(V1, V2), !enzyme(V1, V2)","!enzyme(V1, V2), complex(V3, V1)","!protein_class(V2, V4), function(V1, V2)","complex(V3, V2), !protein_class(V1, V2)","enzyme(V3, V1), !complex(V1, V2)","phenotype(V1, V2), !protein_class(V2, V4)","!complex(V1, V2), interaction(V2, V1)","location(V1, V4), !enzyme(V1, V2)","!interaction(V2, V4), function(V1, V2)","location(V1, V2), interaction(V1, V1)","!interaction(V1, V2), location(V2, V1)","enzyme(V1, V4), location(V1, V2)","complex(V1, V4), location(V1, V2)","phenotype(V1, V2), !protein_class(V2, V1)","!function(V1, V4), function(V1, V2)","interaction(V1, V4), phenotype(V1, V2)","interaction(V3, V2), !location(V1, V2)","!location(V2, V4), protein_class(V1, V2)","protein_class(V1, V2), !interaction(V3, V2)","!interaction(V1, V2), interaction(V1, V4)","enzyme(V1, V2), enzyme(V1, V4)","phenotype(V1, V2), !enzyme(V2, V1)","phenotype(V1, V2), !location(V2, V1)","!interaction(V1, V2), location(V2, V4)","enzyme(V2, V1), !phenotype(V1, V2)","function(V1, V2), !location(V2, V1)","interaction(V3, V1), !complex(V1, V2)","!interaction(V1, V2), location(V3, V1)","complex(V3, V2), !phenotype(V1, V2)","!function(V2, V4), phenotype(V1, V2)","enzyme(V3, V2), !location(V1, V2)","complex(V1, V2), !protein_class(V1, V2)","!function(V1, V2), complex(V2, V1)","!interaction(V2, V2), location(V3, V2)","location(V1, V4), function(V1, V2)","interaction(V2, V2), !complex(V1, V2)","!interaction(V1, V2), interaction(V3, V2)","interaction(V2, V2), !location(V1, V2)","interaction(V1, V2), !location(V1, V2)","function(V1, V2), function(V3, V2)","interaction(V3, V1), !interaction(V1, V2)","!interaction(V1, V2), interaction(V2, V4)","complex(V1, V2), !location(V1, V2)","complex(V1, V4), !protein_class(V1, V2)","interaction(V1, V4), interaction(V1, V2)","!function(V2, V1), phenotype(V1, V2)","location(V2, V1), !complex(V1, V2)","interaction(V3, V1), phenotype(V1, V2)","complex(V1, V4), function(V1, V2)","!function(V1, V4), protein_class(V1, V2)","enzyme(V1, V4), interaction(V1, V2)","!protein_class(V2, V1), protein_class(V1, V2)","!interaction(V1, V2), function(V1, V2)","!protein_class(V2, V1), function(V1, V2)","!protein_class(V1, V2), enzyme(V3, V1)","!interaction(V1, V2), interaction(V2, V1)","complex(V1, V4), !enzyme(V1, V2)","phenotype(V1, V2), !phenotype(V1, V4)","interaction(V3, V1), !phenotype(V1, V2)","location(V1, V2), !protein_class(V3, V1)","protein_class(V1, V4), protein_class(V1, V2)","phenotype(V3, V1), !complex(V1, V2)","phenotype(V1, V2), !location(V3, V2)","!function(V1, V4), interaction(V1, V2)","phenotype(V1, V2), complex(V1, V4)","!function(V3, V2), function(V1, V2)","interaction(V2, V4), !location(V1, V2)","!function(V1, V2), interaction(V1, V2)","!protein_class(V2, V1), interaction(V1, V2)","enzyme(V3, V1), !phenotype(V1, V2)","!interaction(V1, V2), enzyme(V2, V4)","!function(V1, V2), enzyme(V1, V4)","!interaction(V1, V2), location(V1, V2)","!phenotype(V1, V2), location(V2, V4)","phenotype(V1, V2), !enzyme(V1, V4)","!protein_class(V1, V2), enzyme(V1, V2)","interaction(V1, V4), !location(V1, V2)","function(V3, V1), !complex(V1, V2)","location(V1, V2), !phenotype(V1, V2)","!protein_class(V2, V4), location(V1, V2)","enzyme(V3, V2), !complex(V1, V2)","interaction(V3, V2), !interaction(V2, V2)","!interaction(V1, V2), interaction(V2, V2)","complex(V3, V2), !interaction(V2, V2)","!interaction(V1, V2), enzyme(V3, V2)","interaction(V2, V1), !location(V1, V2)","!enzyme(V1, V2), location(V2, V4)","!location(V1, V4), function(V1, V2)","!complex(V1, V2), interaction(V2, V4)","phenotype(V1, V2), !enzyme(V1, V2)","!enzyme(V2, V4), function(V1, V2)","complex(V2, V4), !enzyme(V1, V2)","protein_class(V1, V2), !phenotype(V1, V2)","!complex(V1, V2), function(V1, V4)","!interaction(V1, V2), complex(V2, V1)","!function(V1, V2), protein_class(V1, V2)","enzyme(V1, V4), !phenotype(V1, V2)","complex(V1, V4), enzyme(V1, V2)","phenotype(V1, V2), !interaction(V3, V2)","location(V1, V4), interaction(V1, V2)","!interaction(V2, V2), phenotype(V3, V2)","phenotype(V1, V2), !location(V3, V1)","!complex(V1, V2), protein_class(V3, V2)","phenotype(V1, V2), !function(V3, V1)","!protein_class(V2, V4), protein_class(V1, V2)","protein_class(V1, V2), !protein_class(V3, V1)","!protein_class(V3, V2), protein_class(V1, V2)","!complex(V1, V2), location(V2, V4)","enzyme(V3, V1), !enzyme(V1, V2)","!enzyme(V1, V2), enzyme(V2, V4)","!protein_class(V1, V2), enzyme(V3, V2)","!enzyme(V1, V2), interaction(V1, V2)","!function(V1, V2), interaction(V2, V2)","!interaction(V1, V2), enzyme(V1, V4)","interaction(V2, V2), interaction(V1, V2)","interaction(V3, V2), !complex(V1, V2)","!enzyme(V1, V2), interaction(V2, V4)","function(V1, V2), interaction(V1, V1)","!function(V1, V2), complex(V1, V2)","!phenotype(V1, V2), location(V3, V1)","!interaction(V2, V2), enzyme(V3, V2)","!enzyme(V1, V2), function(V1, V2)","!phenotype(V1, V4), protein_class(V1, V2)","!phenotype(V1, V2), location(V3, V2)","complex(V2, V1), !enzyme(V1, V2)","enzyme(V1, V2), !phenotype(V1, V2)","!enzyme(V1, V2), location(V3, V1)","!enzyme(V2, V4), protein_class(V1, V2)","complex(V2, V1), !location(V1, V2)","enzyme(V1, V2), !complex(V1, V2)","interaction(V1, V1), !location(V1, V2)","enzyme(V3, V1), !location(V1, V2)","function(V1, V2), !protein_class(V1, V4)","complex(V1, V2), complex(V1, V4)","phenotype(V1, V2), function(V1, V4)","!enzyme(V1, V2), enzyme(V3, V2)","!phenotype(V1, V2), complex(V3, V1)","enzyme(V1, V4), protein_class(V1, V2)","!interaction(V3, V1), protein_class(V1, V2)","enzyme(V2, V4), interaction(V1, V2)","complex(V1, V4), !complex(V1, V2)","!function(V1, V2), enzyme(V2, V4)","complex(V3, V2), !complex(V1, V2)","function(V1, V2), function(V1, V4)","!phenotype(V2, V4), function(V1, V2)","!protein_class(V3, V1), function(V1, V2)","!protein_class(V2, V4), interaction(V1, V2)","phenotype(V1, V2), !location(V1, V2)","!interaction(V1, V4), protein_class(V1, V2)","!function(V1, V4), location(V1, V2)","phenotype(V1, V2), !phenotype(V2, V1)","!function(V1, V2), complex(V1, V4)","interaction(V3, V1), !location(V1, V2)","complex(V1, V4), interaction(V1, V2)","interaction(V2, V4), interaction(V1, V2)","complex(V2, V4), interaction(V2, V2)","location(V1, V4), !complex(V1, V2)","!interaction(V2, V2), protein_class(V3, V2)","!protein_class(V1, V2), location(V1, V2)","!function(V1, V2), enzyme(V2, V1)","interaction(V2, V1), interaction(V1, V2)","interaction(V2, V2), !phenotype(V1, V2)","!protein_class(V3, V2), location(V1, V2)","location(V1, V2), !protein_class(V1, V4)","!location(V1, V2), location(V3, V2)","enzyme(V2, V4), !location(V1, V2)","!complex(V1, V2), location(V3, V2)","enzyme(V2, V1), !complex(V1, V2)","!phenotype(V1, V2), interaction(V2, V4)","location(V1, V2), !complex(V1, V2)","!enzyme(V3, V2), function(V1, V2)","!protein_class(V3, V2), phenotype(V1, V2)","complex(V1, V4), protein_class(V1, V2)","!enzyme(V1, V2), location(V2, V1)","!interaction(V2, V2), function(V2, V4)","!phenotype(V1, V4), function(V1, V2)","complex(V2, V1), !protein_class(V1, V2)","!location(V1, V4), phenotype(V1, V2)","!interaction(V3, V1), function(V1, V2)","phenotype(V1, V2), !protein_class(V1, V2)","location(V1, V4), protein_class(V1, V2)","interaction(V1, V4), !phenotype(V1, V2)","!phenotype(V1, V2), interaction(V1, V1)","!phenotype(V3, V2), function(V1, V2)","interaction(V1, V4), !enzyme(V1, V2)","!interaction(V2, V1), protein_class(V1, V2)","phenotype(V1, V2), !interaction(V1, V4)","phenotype(V1, V2), !enzyme(V2, V4)","function(V1, V2), !interaction(V3, V2)","location(V3, V1), !location(V1, V2)","location(V2, V4), !location(V1, V2)","!function(V2, V4), protein_class(V1, V2)","protein_class(V1, V2), protein_class(V3, V2)","!interaction(V1, V2), enzyme(V2, V1)","location(V2, V1), !location(V1, V2)","function(V1, V2), !location(V1, V2)","!function(V3, V2), location(V1, V2)","protein_class(V1, V2), !function(V3, V1)","complex(V3, V2), !enzyme(V1, V2)","complex(V1, V4), !phenotype(V1, V2)","interaction(V1, V1), interaction(V1, V2)","phenotype(V1, V4), !complex(V1, V2)","!interaction(V1, V2), enzyme(V3, V1)","!function(V3, V2), phenotype(V1, V2)","protein_class(V1, V2), !enzyme(V1, V4)","!phenotype(V1, V2), interaction(V1, V2)","!interaction(V1, V2), complex(V2, V4)","!phenotype(V1, V2), interaction(V2, V1)","!protein_class(V1, V2), interaction(V1, V2)","!function(V1, V2), interaction(V1, V1)","!protein_class(V3, V2), function(V1, V2)","protein_class(V1, V2), !location(V3, V2)","protein_class(V1, V2), !phenotype(V3, V1)","!interaction(V1, V2), complex(V1, V4)","!enzyme(V1, V2), interaction(V2, V1)","!complex(V1, V2), interaction(V1, V1)","enzyme(V1, V4), !location(V1, V2)","!enzyme(V1, V2), location(V1, V2)","complex(V1, V2), complex(V3, V2)","phenotype(V2, V4), !complex(V1, V2)","!enzyme(V2, V1), function(V1, V2)");
//        List<String> smarter =  Sugar.list("!phenotype(V2, V2), interaction(V2, V2)","!phenotype(V1, V2), location(V2, V1)","phenotype(V1, V2), !protein_class(V3, V1)","location(V1, V2), !function(V2, V4)","function(V1, V2), !phenotype(V2, V1)","!location(V1, V2), location(V1, V4)","!enzyme(V1, V2), enzyme(V1, V4)","!complex(V1, V2), protein_class(V1, V4)","location(V1, V2), !function(V3, V1)","protein_class(V1, V2), !enzyme(V1, V2)","!location(V2, V2), interaction(V2, V4)","location(V1, V2), !function(V2, V1)","interaction(V1, V2), !function(V2, V2)","protein_class(V1, V2), !location(V1, V4)","!phenotype(V2, V2), phenotype(V2, V4)","!interaction(V1, V2), complex(V1, V2)","!interaction(V2, V2), phenotype(V2, V4)","phenotype(V1, V2), enzyme(V1, V4)","!location(V1, V2), enzyme(V1, V2)","!phenotype(V2, V2), complex(V3, V2)","phenotype(V1, V2), !interaction(V2, V1)","function(V1, V2), !location(V2, V4)","!interaction(V2, V2), location(V2, V4)","!complex(V1, V2), protein_class(V1, V2)","protein_class(V1, V2), interaction(V1, V4)","protein_class(V1, V2), !complex(V1, V1)","phenotype(V1, V2), !enzyme(V2, V2)","function(V1, V2), !protein_class(V1, V2)","phenotype(V1, V2), !function(V1, V2)","function(V1, V2), !interaction(V2, V1)","phenotype(V1, V2), !interaction(V2, V4)","!function(V1, V2), enzyme(V3, V2)","!interaction(V2, V2), function(V3, V2)","!phenotype(V1, V2), complex(V2, V1)","!phenotype(V2, V2), enzyme(V3, V2)","!function(V1, V2), complex(V2, V4)","!location(V1, V2), complex(V1, V4)","function(V1, V2), !interaction(V1, V4)","!phenotype(V1, V2), interaction(V3, V2)","!interaction(V2, V2), complex(V2, V4)","interaction(V1, V2), !function(V3, V1)","!complex(V1, V2), function(V3, V2)","function(V1, V2), !function(V2, V4)","function(V1, V2), !function(V3, V1)","protein_class(V1, V2), !enzyme(V2, V1)","function(V1, V2), !phenotype(V1, V2)","interaction(V1, V2), !function(V1, V1)","!interaction(V1, V2), complex(V3, V2)","phenotype(V1, V2), interaction(V1, V1)","!interaction(V1, V2), complex(V3, V1)","interaction(V1, V2), !function(V2, V1)","protein_class(V1, V2), !interaction(V2, V4)","!complex(V1, V2), interaction(V1, V2)","!protein_class(V1, V2), enzyme(V2, V4)","location(V1, V2), !protein_class(V2, V1)","interaction(V1, V2), !protein_class(V3, V2)","!complex(V1, V2), protein_class(V2, V1)","location(V1, V2), interaction(V3, V1)","protein_class(V1, V2), !enzyme(V3, V1)","!complex(V1, V2), interaction(V1, V4)","protein_class(V1, V2), !enzyme(V3, V2)","!enzyme(V1, V2), enzyme(V2, V1)","!location(V1, V2), complex(V2, V4)","!complex(V1, V2), phenotype(V3, V2)","!interaction(V1, V2), enzyme(V1, V2)","protein_class(V1, V2), !enzyme(V1, V1)","enzyme(V1, V2), !function(V1, V1)","!complex(V1, V2), function(V1, V2)","phenotype(V1, V2), !phenotype(V2, V4)","phenotype(V1, V2), !phenotype(V3, V1)","function(V1, V2), enzyme(V1, V4)","!complex(V1, V2), phenotype(V1, V2)","!enzyme(V2, V2), interaction(V2, V2)","!complex(V1, V2), phenotype(V2, V1)","protein_class(V1, V2), !location(V3, V1)","!phenotype(V1, V2), enzyme(V3, V2)","!complex(V2, V2), location(V2, V4)","!location(V2, V2), interaction(V3, V2)","!protein_class(V1, V2), complex(V3, V1)","!enzyme(V1, V2), location(V3, V2)","location(V1, V2), !function(V1, V2)","phenotype(V1, V2), !enzyme(V3, V1)","interaction(V1, V2), !function(V3, V2)","!enzyme(V1, V2), complex(V1, V2)","location(V1, V2), !protein_class(V2, V2)","!protein_class(V1, V2), complex(V3, V2)","!phenotype(V2, V2), function(V3, V2)","!complex(V1, V2), enzyme(V3, V1)","!complex(V1, V2), interaction(V2, V1)","phenotype(V1, V2), !function(V1, V1)","!enzyme(V1, V2), location(V1, V4)","function(V1, V2), !interaction(V2, V4)","!protein_class(V2, V2), enzyme(V2, V4)","!phenotype(V2, V2), protein_class(V2, V4)","phenotype(V1, V2), !protein_class(V2, V1)","function(V1, V2), !function(V1, V4)","!phenotype(V2, V2), location(V2, V4)","!enzyme(V2, V2), enzyme(V2, V4)","protein_class(V1, V2), !location(V2, V4)","protein_class(V1, V2), !interaction(V3, V2)","enzyme(V1, V2), enzyme(V1, V4)","phenotype(V1, V2), !location(V2, V1)","!interaction(V1, V2), location(V2, V4)","!complex(V1, V2), interaction(V3, V1)","!interaction(V1, V2), location(V3, V1)","!phenotype(V1, V2), complex(V3, V2)","phenotype(V1, V2), !function(V2, V4)","!protein_class(V1, V2), complex(V1, V2)","!interaction(V2, V2), location(V3, V2)","function(V1, V2), location(V1, V4)","!interaction(V1, V2), interaction(V3, V2)","!location(V1, V2), interaction(V2, V2)","function(V1, V2), function(V3, V2)","!interaction(V1, V2), interaction(V2, V4)","!interaction(V1, V2), interaction(V3, V1)","!location(V1, V2), complex(V1, V2)","!protein_class(V1, V2), complex(V1, V4)","interaction(V1, V2), interaction(V1, V4)","phenotype(V1, V2), interaction(V3, V1)","function(V1, V2), complex(V1, V4)","phenotype(V1, V2), !complex(V1, V1)","protein_class(V1, V2), !function(V1, V4)","interaction(V1, V2), enzyme(V1, V4)","location(V1, V2), !protein_class(V1, V1)","function(V1, V2), !interaction(V1, V2)","!interaction(V1, V2), interaction(V2, V1)","location(V1, V2), !protein_class(V3, V1)","protein_class(V1, V2), protein_class(V1, V4)","!complex(V1, V2), phenotype(V3, V1)","phenotype(V1, V2), !location(V3, V2)","!location(V1, V2), interaction(V2, V4)","interaction(V1, V2), !function(V1, V2)","interaction(V1, V2), !protein_class(V2, V1)","!interaction(V1, V2), enzyme(V2, V4)","!function(V1, V2), enzyme(V1, V4)","!interaction(V1, V2), location(V1, V2)","!phenotype(V1, V2), location(V2, V4)","!location(V2, V2), location(V3, V2)","!location(V1, V2), interaction(V1, V4)","!interaction(V1, V2), enzyme(V3, V2)","protein_class(V1, V2), !protein_class(V1, V1)","!location(V1, V2), interaction(V2, V1)","!enzyme(V1, V2), location(V2, V4)","!complex(V1, V2), interaction(V2, V4)","!enzyme(V1, V2), complex(V2, V4)","location(V1, V2), !enzyme(V1, V1)","!interaction(V1, V2), complex(V2, V1)","protein_class(V1, V2), !function(V2, V2)","location(V1, V2), interaction(V1, V4)","phenotype(V1, V2), !location(V3, V1)","protein_class(V1, V2), !protein_class(V2, V4)","protein_class(V1, V2), !protein_class(V3, V1)","!enzyme(V1, V2), enzyme(V2, V4)","!enzyme(V1, V2), enzyme(V3, V1)","!protein_class(V1, V2), enzyme(V3, V2)","!enzyme(V1, V2), interaction(V1, V2)","!function(V1, V2), interaction(V2, V2)","!interaction(V1, V2), enzyme(V1, V4)","phenotype(V1, V2), !enzyme(V1, V1)","interaction(V1, V2), interaction(V2, V2)","!complex(V1, V2), interaction(V3, V2)","phenotype(V1, V2), !protein_class(V2, V2)","!phenotype(V1, V2), location(V3, V1)","!enzyme(V2, V2), enzyme(V3, V2)","protein_class(V1, V2), !phenotype(V1, V4)","!phenotype(V1, V2), location(V3, V2)","!enzyme(V1, V2), complex(V2, V1)","!enzyme(V1, V2), location(V3, V1)","phenotype(V1, V2), !function(V2, V2)","protein_class(V1, V2), !enzyme(V2, V4)","!location(V1, V2), interaction(V1, V1)","complex(V1, V2), complex(V1, V4)","phenotype(V1, V2), function(V1, V4)","!enzyme(V1, V2), enzyme(V3, V2)","!phenotype(V1, V2), complex(V3, V1)","protein_class(V1, V2), !interaction(V3, V1)","interaction(V1, V2), enzyme(V2, V4)","!complex(V1, V2), complex(V3, V2)","function(V1, V2), !phenotype(V2, V4)","interaction(V1, V2), !protein_class(V2, V4)","protein_class(V1, V2), !interaction(V1, V4)","phenotype(V1, V2), !phenotype(V2, V1)","!location(V1, V2), interaction(V3, V1)","interaction(V1, V2), complex(V1, V4)","!interaction(V2, V2), protein_class(V3, V2)","location(V1, V2), !function(V1, V1)","!location(V2, V2), complex(V2, V4)","!function(V1, V2), enzyme(V2, V1)","interaction(V1, V2), interaction(V2, V1)","!phenotype(V1, V2), interaction(V2, V2)","location(V1, V2), !protein_class(V3, V2)","location(V1, V2), !protein_class(V1, V4)","!complex(V2, V2), interaction(V2, V4)","!complex(V1, V2), enzyme(V2, V1)","!phenotype(V1, V2), interaction(V2, V4)","!complex(V1, V2), location(V1, V2)","function(V1, V2), !enzyme(V3, V2)","protein_class(V1, V2), complex(V1, V4)","!enzyme(V1, V2), location(V2, V1)","!interaction(V2, V2), function(V2, V4)","!protein_class(V1, V2), complex(V2, V1)","phenotype(V1, V2), !location(V1, V4)","phenotype(V1, V2), !protein_class(V1, V2)","protein_class(V1, V2), location(V1, V4)","!phenotype(V1, V2), interaction(V1, V1)","!function(V2, V2), complex(V2, V4)","!enzyme(V1, V2), interaction(V1, V4)","protein_class(V1, V2), !complex(V2, V2)","phenotype(V1, V2), !interaction(V1, V4)","!function(V2, V2), complex(V3, V2)","phenotype(V1, V2), !enzyme(V2, V4)","phenotype(V1, V2), !location(V1, V1)","protein_class(V1, V2), !function(V2, V4)","protein_class(V1, V2), protein_class(V3, V2)","!interaction(V1, V2), enzyme(V2, V1)","!location(V1, V2), location(V2, V1)","function(V1, V2), !location(V1, V2)","!protein_class(V2, V2), complex(V2, V4)","protein_class(V1, V2), !function(V3, V1)","!enzyme(V1, V2), complex(V3, V2)","!phenotype(V1, V2), complex(V1, V4)","!complex(V1, V2), phenotype(V1, V4)","phenotype(V1, V2), !function(V3, V2)","!interaction(V1, V2), complex(V2, V4)","!function(V1, V2), interaction(V1, V1)","function(V1, V2), !protein_class(V3, V2)","protein_class(V1, V2), !location(V3, V2)","!enzyme(V1, V2), interaction(V2, V1)","!location(V1, V2), enzyme(V1, V4)","!enzyme(V1, V2), location(V1, V2)","complex(V1, V2), complex(V3, V2)","!phenotype(V2, V2), interaction(V2, V4)","!complex(V1, V2), phenotype(V2, V4)","function(V1, V2), !enzyme(V2, V1)","interaction(V1, V2), !function(V2, V4)","phenotype(V1, V2), !function(V1, V4)","protein_class(V1, V2), !phenotype(V2, V4)","!interaction(V2, V2), interaction(V2, V4)","!interaction(V1, V2), interaction(V1, V1)","!protein_class(V1, V2), interaction(V1, V1)","phenotype(V1, V2), !protein_class(V1, V1)","interaction(V1, V2), !protein_class(V1, V4)","!function(V1, V2), enzyme(V1, V2)","protein_class(V1, V2), !phenotype(V2, V1)","function(V1, V2), protein_class(V1, V4)","!complex(V1, V2), protein_class(V2, V4)","enzyme(V1, V2), enzyme(V3, V2)","!protein_class(V1, V2), enzyme(V1, V4)","!complex(V1, V2), enzyme(V2, V4)","!interaction(V1, V2), location(V1, V4)","!function(V1, V2), complex(V3, V2)","enzyme(V1, V2), !function(V2, V2)","!complex(V1, V2), complex(V2, V1)","!complex(V2, V2), complex(V3, V2)","!protein_class(V1, V2), interaction(V2, V2)","phenotype(V1, V2), !protein_class(V1, V4)","phenotype(V1, V2), phenotype(V1, V4)","protein_class(V1, V2), !function(V3, V2)","function(V1, V2), !enzyme(V3, V1)","!enzyme(V2, V2), complex(V3, V2)","protein_class(V1, V2), !location(V1, V2)","interaction(V1, V2), !protein_class(V3, V1)","!protein_class(V1, V2), complex(V2, V4)","protein_class(V1, V2), !function(V2, V1)","function(V1, V2), interaction(V1, V4)","!enzyme(V1, V2), interaction(V3, V2)","function(V1, V2), !enzyme(V1, V4)","!complex(V1, V2), protein_class(V3, V1)","!enzyme(V1, V2), interaction(V1, V1)","!phenotype(V1, V2), enzyme(V2, V4)","phenotype(V1, V2), !interaction(V1, V2)","protein_class(V1, V2), !protein_class(V1, V4)","phenotype(V1, V2), protein_class(V1, V4)","interaction(V1, V2), complex(V2, V4)","!complex(V2, V2), location(V3, V2)","!phenotype(V2, V2), location(V3, V2)","!protein_class(V2, V2), interaction(V2, V2)","phenotype(V1, V2), !interaction(V3, V1)","phenotype(V1, V2), !location(V2, V4)","!location(V2, V2), enzyme(V2, V4)","!protein_class(V1, V2), enzyme(V2, V1)","!complex(V1, V2), enzyme(V1, V4)","phenotype(V1, V2), phenotype(V3, V2)","!location(V1, V2), complex(V3, V2)","location(V1, V2), location(V1, V4)","protein_class(V1, V2), !location(V2, V1)","protein_class(V1, V2), interaction(V1, V1)","phenotype(V1, V2), !enzyme(V3, V2)","!phenotype(V2, V2), protein_class(V3, V2)","function(V1, V2), !phenotype(V3, V1)","!complex(V1, V2), complex(V2, V4)","!complex(V1, V2), complex(V3, V1)","function(V1, V2), !location(V3, V2)","!phenotype(V1, V2), complex(V1, V2)","!enzyme(V1, V2), interaction(V2, V2)","!interaction(V2, V2), enzyme(V2, V4)","protein_class(V1, V2), !phenotype(V3, V2)","location(V1, V2), location(V3, V2)","phenotype(V1, V2), !location(V2, V2)","protein_class(V1, V2), !function(V1, V1)","!function(V2, V2), interaction(V2, V2)","protein_class(V1, V2), !protein_class(V2, V2)","!complex(V1, V2), location(V3, V1)","enzyme(V1, V2), interaction(V1, V1)","!function(V1, V2), enzyme(V3, V1)","!phenotype(V1, V2), complex(V2, V4)","!location(V1, V2), enzyme(V2, V1)","function(V1, V2), !function(V2, V1)","function(V1, V2), !location(V3, V1)","!phenotype(V1, V2), location(V1, V4)","!enzyme(V1, V2), interaction(V3, V1)","!complex(V2, V2), interaction(V3, V2)","!location(V1, V2), complex(V3, V1)","function(V1, V2), interaction(V3, V1)","interaction(V1, V2), interaction(V3, V2)","!function(V1, V2), complex(V3, V1)","phenotype(V1, V2), !phenotype(V3, V2)","!complex(V1, V2), function(V2, V4)","!interaction(V1, V2), location(V3, V2)","protein_class(V1, V2), !interaction(V1, V2)","phenotype(V1, V2), location(V1, V4)","!complex(V2, V2), interaction(V2, V2)","protein_class(V1, V2), interaction(V3, V1)","!enzyme(V2, V2), complex(V2, V4)","!interaction(V2, V2), protein_class(V2, V4)","!complex(V1, V2), function(V2, V1)","!enzyme(V1, V2), complex(V3, V1)","function(V1, V2), !protein_class(V2, V4)","phenotype(V1, V2), !protein_class(V2, V4)","location(V1, V2), interaction(V1, V1)","!interaction(V1, V2), location(V2, V1)","location(V1, V2), enzyme(V1, V4)","location(V1, V2), complex(V1, V4)","phenotype(V1, V2), interaction(V1, V4)","!location(V1, V2), interaction(V3, V2)","!interaction(V1, V2), interaction(V1, V4)","phenotype(V1, V2), !enzyme(V2, V1)","!phenotype(V1, V2), enzyme(V2, V1)","function(V1, V2), !location(V2, V1)","!location(V1, V2), enzyme(V3, V2)","!function(V1, V2), complex(V2, V1)","!complex(V1, V2), interaction(V2, V2)","function(V1, V2), !enzyme(V1, V1)","!location(V1, V2), interaction(V1, V2)","phenotype(V1, V2), !function(V2, V1)","!complex(V1, V2), location(V2, V1)","protein_class(V1, V2), !protein_class(V2, V1)","function(V1, V2), !protein_class(V2, V1)","!protein_class(V1, V2), enzyme(V3, V1)","!enzyme(V1, V2), complex(V1, V4)","phenotype(V1, V2), !phenotype(V1, V4)","!phenotype(V1, V2), interaction(V3, V1)","interaction(V1, V2), !function(V1, V4)","phenotype(V1, V2), complex(V1, V4)","function(V1, V2), !function(V3, V2)","!phenotype(V2, V2), function(V2, V4)","!phenotype(V1, V2), enzyme(V3, V1)","function(V1, V2), !complex(V1, V1)","phenotype(V1, V2), !enzyme(V1, V4)","!protein_class(V1, V2), enzyme(V1, V2)","!complex(V1, V2), function(V3, V1)","!phenotype(V1, V2), location(V1, V2)","location(V1, V2), !protein_class(V2, V4)","!complex(V1, V2), enzyme(V3, V2)","!interaction(V2, V2), interaction(V3, V2)","!interaction(V1, V2), interaction(V2, V2)","!interaction(V2, V2), complex(V3, V2)","function(V1, V2), !location(V1, V4)","phenotype(V1, V2), !enzyme(V1, V2)","function(V1, V2), !enzyme(V2, V4)","interaction(V1, V2), !protein_class(V1, V1)","protein_class(V1, V2), !phenotype(V1, V2)","!complex(V1, V2), function(V1, V4)","!location(V2, V2), interaction(V2, V2)","protein_class(V1, V2), !function(V1, V2)","!phenotype(V1, V2), enzyme(V1, V4)","enzyme(V1, V2), complex(V1, V4)","phenotype(V1, V2), !interaction(V3, V2)","!phenotype(V2, V2), interaction(V3, V2)","!complex(V2, V2), complex(V2, V4)","!interaction(V2, V2), phenotype(V3, V2)","!complex(V1, V2), protein_class(V3, V2)","phenotype(V1, V2), !function(V3, V1)","protein_class(V1, V2), !protein_class(V3, V2)","function(V1, V2), !enzyme(V2, V2)","!complex(V1, V2), location(V2, V4)","location(V1, V2), !function(V2, V2)","!location(V2, V2), location(V2, V4)","!enzyme(V1, V2), interaction(V2, V4)","interaction(V1, V2), !protein_class(V2, V2)","function(V1, V2), interaction(V1, V1)","!function(V1, V2), complex(V1, V2)","interaction(V1, V2), !enzyme(V2, V2)","!interaction(V2, V2), enzyme(V3, V2)","function(V1, V2), !protein_class(V1, V1)","function(V1, V2), !enzyme(V1, V2)","function(V1, V2), !protein_class(V2, V2)","!phenotype(V1, V2), enzyme(V1, V2)","!location(V1, V2), complex(V2, V1)","!complex(V1, V2), enzyme(V1, V2)","phenotype(V1, V2), !complex(V2, V2)","!location(V1, V2), enzyme(V3, V1)","function(V1, V2), !protein_class(V1, V4)","location(V1, V2), !enzyme(V2, V2)","interaction(V1, V2), !enzyme(V1, V1)","protein_class(V1, V2), enzyme(V1, V4)","!complex(V1, V2), complex(V1, V4)","!function(V1, V2), enzyme(V2, V4)","function(V1, V2), function(V1, V4)","function(V1, V2), !protein_class(V3, V1)","!phenotype(V2, V2), complex(V2, V4)","phenotype(V1, V2), !location(V1, V2)","location(V1, V2), !function(V1, V4)","!function(V1, V2), complex(V1, V4)","interaction(V1, V2), interaction(V2, V4)","interaction(V2, V2), complex(V2, V4)","!complex(V1, V2), location(V1, V4)","location(V1, V2), !protein_class(V1, V2)","!location(V2, V2), complex(V3, V2)","function(V1, V2), !function(V1, V1)","!phenotype(V2, V2), enzyme(V2, V4)","function(V1, V2), !complex(V2, V2)","function(V1, V2), !location(V1, V1)","!location(V1, V2), location(V3, V2)","!location(V1, V2), enzyme(V2, V4)","!complex(V1, V2), location(V3, V2)","!complex(V2, V2), enzyme(V3, V2)","phenotype(V1, V2), !protein_class(V3, V2)","function(V1, V2), !phenotype(V1, V4)","!protein_class(V2, V2), complex(V3, V2)","!location(V2, V2), protein_class(V2, V4)","function(V1, V2), !interaction(V3, V1)","!complex(V2, V2), enzyme(V2, V4)","!phenotype(V1, V2), interaction(V1, V4)","function(V1, V2), !phenotype(V3, V2)","protein_class(V1, V2), !interaction(V2, V1)","function(V1, V2), !interaction(V3, V2)","!location(V1, V2), location(V2, V4)","!location(V1, V2), location(V3, V1)","!location(V2, V2), enzyme(V3, V2)","function(V1, V2), !function(V2, V2)","location(V1, V2), !function(V3, V2)","!protein_class(V2, V2), enzyme(V3, V2)","function(V1, V2), !location(V2, V2)","interaction(V1, V2), interaction(V1, V1)","!interaction(V1, V2), enzyme(V3, V1)","protein_class(V1, V2), !enzyme(V1, V4)","!phenotype(V1, V2), interaction(V1, V2)","!phenotype(V1, V2), interaction(V2, V1)","interaction(V1, V2), !protein_class(V1, V2)","protein_class(V1, V2), !phenotype(V3, V1)","!interaction(V1, V2), complex(V1, V4)","!complex(V1, V2), interaction(V1, V1)","protein_class(V1, V2), !enzyme(V2, V2)","!location(V2, V2), protein_class(V3, V2)","!phenotype(V2, V2), phenotype(V3, V2)");

        // hc ve druhe vrstve pro lits=3, vars=2*list, sat=2
        List<String> smarter = Sugar.list("!location(V2, V2)", "!function(V2, V2)", "!complex(V2, V2)", "!enzyme(V2, V2)", "!protein_class(V2, V2)", "!phenotype(V2, V2)", "phenotype(V2, V2), !location(V3, V2)", "phenotype(V2, V2), !function(V2, V4)", "phenotype(V2, V2), !complex(V3, V2)", "phenotype(V2, V2), !enzyme(V2, V4)", "phenotype(V2, V2), !protein_class(V3, V2)", "phenotype(V2, V2), !interaction(V2, V4)", "phenotype(V2, V2), !interaction(V2, V2)", "phenotype(V2, V2), !phenotype(V3, V2)", "complex(V1, V2), !location(V1, V4)", "complex(V1, V2), !function(V1, V4)", "complex(V1, V2), !complex(V3, V2)", "complex(V1, V2), !enzyme(V2, V4)", "complex(V1, V2), !protein_class(V3, V1)", "complex(V1, V2), !interaction(V3, V2)", "complex(V1, V2), !interaction(V1, V1)", "complex(V1, V2), !phenotype(V1, V4)", "interaction(V2, V2), !location(V3, V2)", "interaction(V2, V2), !function(V2, V4)", "interaction(V2, V2), !complex(V3, V2)", "interaction(V2, V2), !enzyme(V2, V4)", "interaction(V2, V2), !protein_class(V3, V2)", "interaction(V2, V2), !phenotype(V3, V2)", "!phenotype(V1, V2), !location(V1, V4)", "!phenotype(V1, V2), !location(V3, V1)", "!phenotype(V1, V2), !location(V2, V1)", "!phenotype(V1, V2), !location(V3, V2)", "!phenotype(V1, V2), !location(V2, V4)", "!phenotype(V1, V2), !location(V1, V2)", "!phenotype(V1, V2), !function(V3, V1)", "!phenotype(V1, V2), !function(V3, V2)", "!phenotype(V1, V2), !function(V1, V2)", "!phenotype(V1, V2), !function(V2, V4)", "!phenotype(V1, V2), !function(V1, V4)", "!phenotype(V1, V2), !function(V2, V1)", "!phenotype(V1, V2), !complex(V2, V1)", "!phenotype(V1, V2), !complex(V3, V1)", "!phenotype(V1, V2), !complex(V1, V4)", "!phenotype(V1, V2), !complex(V3, V2)", "!phenotype(V1, V2), !complex(V2, V4)", "!phenotype(V1, V2), !complex(V1, V2)", "!phenotype(V1, V2), !enzyme(V2, V4)", "!phenotype(V1, V2), !enzyme(V1, V4)", "!phenotype(V1, V2), !enzyme(V3, V1)", "!phenotype(V1, V2), !enzyme(V1, V2)", "!phenotype(V1, V2), !enzyme(V3, V2)", "!phenotype(V1, V2), !enzyme(V2, V1)", "!phenotype(V1, V2), !protein_class(V3, V2)", "!phenotype(V1, V2), !protein_class(V3, V1)", "!phenotype(V1, V2), !protein_class(V1, V2)", "!phenotype(V1, V2), !protein_class(V2, V4)", "!phenotype(V1, V2), !protein_class(V1, V4)", "!phenotype(V1, V2), !protein_class(V2, V1)", "!phenotype(V1, V2), interaction(V3, V1)", "!phenotype(V1, V2), !interaction(V1, V1)", "!phenotype(V1, V2), !interaction(V3, V2)", "!phenotype(V1, V2), !interaction(V2, V1)", "!phenotype(V1, V2), !interaction(V1, V2)", "!phenotype(V1, V2), !interaction(V2, V2)", "!phenotype(V1, V2), !interaction(V1, V4)", "!phenotype(V1, V2), !interaction(V2, V4)", "!phenotype(V1, V2), !interaction(V3, V1)", "!phenotype(V1, V2), !phenotype(V2, V4)", "!phenotype(V1, V2), !phenotype(V2, V1)", "!phenotype(V1, V2), !phenotype(V1, V4)", "!phenotype(V1, V2), !phenotype(V3, V2)", "!function(V1, V2), !location(V2, V4)", "!function(V1, V2), !location(V1, V4)", "!function(V1, V2), !location(V3, V1)", "!function(V1, V2), !location(V2, V1)", "!function(V1, V2), !location(V1, V2)", "!function(V1, V2), !location(V3, V2)", "!function(V1, V2), !function(V1, V4)", "!function(V1, V2), !function(V3, V2)", "!function(V1, V2), !function(V2, V4)", "!function(V1, V2), !function(V2, V1)", "!function(V1, V2), !complex(V2, V4)", "!function(V1, V2), !complex(V3, V1)", "!function(V1, V2), !complex(V1, V2)", "!function(V1, V2), !complex(V3, V2)", "!function(V1, V2), !complex(V1, V4)", "!function(V1, V2), !complex(V2, V1)", "!function(V1, V2), !enzyme(V2, V4)", "!function(V1, V2), !enzyme(V1, V4)", "!function(V1, V2), !enzyme(V3, V1)", "!function(V1, V2), !enzyme(V3, V2)", "!function(V1, V2), !enzyme(V2, V1)", "!function(V1, V2), !enzyme(V1, V2)", "!function(V1, V2), !protein_class(V2, V4)", "!function(V1, V2), !protein_class(V3, V1)", "!function(V1, V2), !protein_class(V1, V2)", "!function(V1, V2), !protein_class(V3, V2)", "!function(V1, V2), !protein_class(V1, V4)", "!function(V1, V2), !protein_class(V2, V1)", "!function(V1, V2), interaction(V3, V2)", "!function(V1, V2), !interaction(V3, V2)", "!function(V1, V2), !interaction(V1, V4)", "!function(V1, V2), !interaction(V3, V1)", "!function(V1, V2), !interaction(V2, V1)", "!function(V1, V2), !interaction(V1, V2)", "!function(V1, V2), !interaction(V1, V1)", "!function(V1, V2), !interaction(V2, V2)", "!function(V1, V2), !interaction(V2, V4)", "!protein_class(V1, V2), !location(V2, V4)", "!protein_class(V1, V2), !location(V3, V1)", "!protein_class(V1, V2), !location(V3, V2)", "!protein_class(V1, V2), !location(V1, V4)", "!protein_class(V1, V2), !location(V2, V1)", "!protein_class(V1, V2), !location(V1, V2)", "!protein_class(V1, V2), !complex(V2, V4)", "!protein_class(V1, V2), !complex(V1, V4)", "!protein_class(V1, V2), !complex(V2, V1)", "!protein_class(V1, V2), !complex(V1, V2)", "!protein_class(V1, V2), !complex(V3, V1)", "!protein_class(V1, V2), !complex(V3, V2)", "!protein_class(V1, V2), !enzyme(V1, V2)", "!protein_class(V1, V2), !enzyme(V1, V4)", "!protein_class(V1, V2), !enzyme(V3, V2)", "!protein_class(V1, V2), !enzyme(V3, V1)", "!protein_class(V1, V2), !enzyme(V2, V4)", "!protein_class(V1, V2), !enzyme(V2, V1)", "!protein_class(V1, V2), !protein_class(V1, V4)", "!protein_class(V1, V2), !protein_class(V2, V1)", "!protein_class(V1, V2), !protein_class(V2, V4)", "!protein_class(V1, V2), !protein_class(V3, V2)", "!protein_class(V1, V2), interaction(V3, V2)", "!protein_class(V1, V2), !interaction(V2, V1)", "!protein_class(V1, V2), !interaction(V1, V4)", "!protein_class(V1, V2), !interaction(V2, V2)", "!protein_class(V1, V2), !interaction(V3, V2)", "!protein_class(V1, V2), !interaction(V1, V2)", "!protein_class(V1, V2), !interaction(V1, V1)", "!protein_class(V1, V2), !interaction(V3, V1)", "!protein_class(V1, V2), !interaction(V2, V4)", "interaction(V1, V2), !location(V2, V4)", "interaction(V1, V2), !complex(V3, V2)", "interaction(V1, V2), !enzyme(V3, V1)", "!location(V1, V2), !location(V3, V2)", "!location(V1, V2), !location(V2, V4)", "!location(V1, V2), !location(V1, V4)", "!location(V1, V2), !location(V2, V1)", "!location(V1, V2), !complex(V1, V4)", "!location(V1, V2), !complex(V3, V1)", "!location(V1, V2), !complex(V2, V4)", "!location(V1, V2), !complex(V2, V1)", "!location(V1, V2), !complex(V1, V2)", "!location(V1, V2), !complex(V3, V2)", "!location(V1, V2), !enzyme(V1, V4)", "!location(V1, V2), !enzyme(V3, V2)", "!location(V1, V2), !enzyme(V1, V2)", "!location(V1, V2), !enzyme(V2, V4)", "!location(V1, V2), !enzyme(V3, V1)", "!location(V1, V2), !enzyme(V2, V1)", "!location(V1, V2), !interaction(V2, V1)", "!location(V1, V2), !interaction(V3, V2)", "!location(V1, V2), !interaction(V1, V1)", "!location(V1, V2), !interaction(V2, V4)", "!location(V1, V2), !interaction(V1, V4)", "!location(V1, V2), !interaction(V1, V2)", "!location(V1, V2), !interaction(V2, V2)", "!location(V1, V2), !interaction(V3, V1)", "!interaction(V1, V2), !complex(V3, V1)", "!interaction(V1, V2), !complex(V2, V1)", "!interaction(V1, V2), !complex(V3, V2)", "!interaction(V1, V2), !complex(V1, V4)", "!interaction(V1, V2), !complex(V1, V2)", "!interaction(V1, V2), !complex(V2, V4)", "!interaction(V1, V2), !enzyme(V2, V1)", "!interaction(V1, V2), !enzyme(V1, V2)", "!interaction(V1, V2), !enzyme(V1, V4)", "!interaction(V1, V2), !enzyme(V2, V4)", "!interaction(V1, V2), !enzyme(V3, V1)", "!interaction(V1, V2), !enzyme(V3, V2)", "!interaction(V1, V2), !interaction(V1, V4)", "!interaction(V1, V2), !interaction(V2, V1)", "!interaction(V1, V2), !interaction(V1, V1)", "!interaction(V1, V2), !interaction(V2, V2)", "!interaction(V1, V2), !interaction(V2, V4)", "!interaction(V1, V2), !interaction(V3, V2)", "!enzyme(V1, V2), !complex(V3, V1)", "!enzyme(V1, V2), !complex(V3, V2)", "!enzyme(V1, V2), !complex(V1, V2)", "!enzyme(V1, V2), !complex(V2, V4)", "!enzyme(V1, V2), !complex(V1, V4)", "!enzyme(V1, V2), !complex(V2, V1)", "!enzyme(V1, V2), !enzyme(V2, V1)", "!enzyme(V1, V2), !enzyme(V2, V4)", "!enzyme(V1, V2), !enzyme(V3, V2)", "!enzyme(V1, V2), !enzyme(V1, V4)", "!enzyme(V1, V2), !interaction(V2, V2)", "!enzyme(V1, V2), !interaction(V1, V1)", "!interaction(V2, V2), !complex(V2, V4)", "!interaction(V2, V2), !complex(V3, V2)", "!complex(V1, V2), !complex(V3, V2)", "!complex(V1, V2), !complex(V1, V4)", "!complex(V1, V2), !complex(V2, V4)", "!complex(V1, V2), !complex(V2, V1)");
        List<String> complete = Sugar.list("!complex(V2, V2)", "!phenotype(V2, V2)", "!function(V2, V2)", "!protein_class(V2, V2)", "!location(V2, V2)", "!enzyme(V2, V2)", "!phenotype(V1, V2), !function(V2, V1)", "!phenotype(V1, V2), !protein_class(V2, V1)", "!phenotype(V1, V2), !location(V2, V4)", "!phenotype(V1, V2), !protein_class(V3, V2)", "!phenotype(V1, V2), !location(V3, V2)", "!phenotype(V1, V2), !enzyme(V2, V4)", "!phenotype(V1, V2), !phenotype(V2, V1)", "!phenotype(V1, V2), !complex(V1, V2)", "!phenotype(V1, V2), !interaction(V2, V1)", "!phenotype(V1, V2), !function(V3, V1)", "!phenotype(V1, V2), !complex(V3, V2)", "!phenotype(V1, V2), !interaction(V2, V2)", "!phenotype(V1, V2), !complex(V2, V4)", "!phenotype(V1, V2), !phenotype(V2, V4)", "!phenotype(V1, V2), !function(V3, V2)", "!phenotype(V1, V2), !protein_class(V3, V1)", "!phenotype(V1, V2), !enzyme(V2, V1)", "!phenotype(V1, V2), !protein_class(V2, V4)", "!phenotype(V1, V2), !location(V1, V2)", "!phenotype(V1, V2), !enzyme(V1, V2)", "!phenotype(V1, V2), !enzyme(V3, V1)", "!interaction(V3, V2), !phenotype(V1, V2)", "!phenotype(V1, V2), !interaction(V1, V2)", "!phenotype(V1, V2), !location(V2, V1)", "!phenotype(V1, V2), !complex(V3, V1)", "!phenotype(V1, V2), !protein_class(V1, V2)", "!phenotype(V1, V2), !interaction(V2, V4)", "!phenotype(V1, V2), !location(V3, V1)", "!phenotype(V1, V2), !function(V1, V2)", "!phenotype(V1, V2), !complex(V2, V1)", "!phenotype(V1, V2), !enzyme(V3, V2)", "!phenotype(V1, V2), !function(V2, V4)", "!function(V1, V2), !location(V3, V2)", "!function(V1, V2), !protein_class(V3, V1)", "!enzyme(V3, V1), !function(V1, V2)", "!protein_class(V1, V2), !function(V1, V2)", "!complex(V2, V1), !function(V1, V2)", "!function(V1, V2), !function(V2, V4)", "!interaction(V3, V2), !function(V1, V2)", "!complex(V2, V4), !function(V1, V2)", "!protein_class(V2, V1), !function(V1, V2)", "!location(V1, V2), !function(V1, V2)", "!enzyme(V2, V1), !function(V1, V2)", "!protein_class(V2, V4), !function(V1, V2)", "!function(V1, V2), !function(V2, V1)", "!location(V2, V1), !function(V1, V2)", "!function(V1, V2), !location(V2, V4)", "!interaction(V2, V2), !function(V1, V2)", "!enzyme(V1, V2), !function(V1, V2)", "!function(V1, V2), !complex(V3, V1)", "!function(V1, V2), !location(V3, V1)", "!function(V1, V2), !protein_class(V3, V2)", "!function(V1, V2), !interaction(V2, V4)", "!complex(V1, V2), !function(V1, V2)", "!complex(V3, V2), !function(V1, V2)", "!enzyme(V2, V4), !function(V1, V2)", "!function(V1, V2), !interaction(V2, V1)", "!enzyme(V3, V2), !function(V1, V2)", "!function(V1, V2), !interaction(V1, V2)", "!protein_class(V1, V2), !protein_class(V2, V1)", "!enzyme(V2, V1), !protein_class(V1, V2)", "!interaction(V3, V2), !protein_class(V1, V2)", "!protein_class(V1, V2), !location(V1, V2)", "!protein_class(V1, V2), !interaction(V2, V2)", "!protein_class(V1, V2), !complex(V3, V1)", "!complex(V2, V1), !protein_class(V1, V2)", "!enzyme(V1, V2), !protein_class(V1, V2)", "!protein_class(V1, V2), !location(V3, V1)", "!protein_class(V1, V2), !protein_class(V2, V4)", "!complex(V1, V2), !protein_class(V1, V2)", "!protein_class(V1, V2), !location(V2, V1)", "!protein_class(V1, V2), !enzyme(V2, V4)", "!complex(V3, V2), !protein_class(V1, V2)", "!enzyme(V3, V1), !protein_class(V1, V2)", "!complex(V2, V4), !protein_class(V1, V2)", "!protein_class(V1, V2), !interaction(V2, V1)", "!protein_class(V1, V2), !enzyme(V3, V2)", "!protein_class(V1, V2), !location(V3, V2)", "!protein_class(V1, V2), !interaction(V1, V2)", "!protein_class(V1, V2), !location(V2, V4)", "!protein_class(V1, V2), !interaction(V2, V4)", "!location(V3, V1), !interaction(V1, V2)", "!enzyme(V3, V1), !interaction(V1, V2)", "!enzyme(V1, V2), !interaction(V1, V2)", "!complex(V3, V2), !interaction(V1, V2)", "!complex(V1, V2), !interaction(V1, V2)", "!location(V1, V2), !interaction(V1, V2)", "!enzyme(V3, V2), !interaction(V1, V2)", "!complex(V2, V1), !interaction(V1, V2)", "!location(V2, V1), !interaction(V1, V2)", "!interaction(V1, V2), !location(V3, V2)", "!complex(V3, V1), !interaction(V1, V2)", "!enzyme(V2, V1), !interaction(V1, V2)", "!location(V1, V2), !location(V2, V1)", "!complex(V2, V4), !location(V1, V2)", "!complex(V1, V2), !location(V1, V2)", "!location(V1, V2), !enzyme(V3, V2)", "!enzyme(V3, V1), !location(V1, V2)", "!complex(V3, V2), !location(V1, V2)", "!enzyme(V1, V2), !location(V1, V2)", "!enzyme(V2, V1), !location(V1, V2)", "!complex(V2, V1), !location(V1, V2)", "!location(V1, V2), !location(V2, V4)", "!location(V1, V2), !enzyme(V2, V4)", "!location(V1, V2), !complex(V3, V1)", "!interaction(V2, V2), !location(V1, V2)", "!complex(V2, V1), !enzyme(V1, V2)", "!complex(V1, V2), !enzyme(V1, V2)", "!enzyme(V1, V2), !complex(V3, V1)", "!complex(V3, V2), !enzyme(V1, V2)", "!enzyme(V1, V2), !interaction(V2, V2)", "!enzyme(V1, V2), !enzyme(V2, V4)", "!enzyme(V1, V2), !enzyme(V2, V1)", "!complex(V2, V4), !enzyme(V1, V2)", "!complex(V3, V2), !interaction(V2, V2)", "!complex(V1, V2), !complex(V2, V4)", "!complex(V1, V2), !complex(V2, V1)");

        Clause conjunctionCandidate = Clause.parse("interaction(V2, V2), complex(V2, V4)");
        MEDataset med = MEDataset.create(Paths.get("..", "datasets", "protein", "train.db.oneLine"), Matching.THETA_SUBSUMPTION);

        Set<IsoClauseWrapper> smarterICWs = smarter.stream().map(Clause::parse).map(IsoClauseWrapper::create).collect(Collectors.toSet());
        Set<IsoClauseWrapper> completeICWs = complete.stream().map(Clause::parse).map(IsoClauseWrapper::create).collect(Collectors.toSet());

        System.out.println(smarter.size() + "\t" + smarterICWs.size());
        System.out.println(complete.size() + "\t" + completeICWs.size());

        System.out.println("\nis in smarter but not in complete");
        int i = 0;
        for (IsoClauseWrapper icw : Sugar.setDifference(smarterICWs, completeICWs)) {
            System.out.println(icw.getOriginalClause());
            i++;
        }
        System.out.println("total:\t" + i);

        //vypadato jako kdyby smarter byl pusteny bez saturaci a complete se saturaci
        //        nebo mozna spatne nastaveni forbidden :))
        // a nejak tak nejde do treti vrstvy -- proc?

        System.out.println("\nis in complete but not in smarter");
        i = 0;
        for (IsoClauseWrapper icw : Sugar.setDifference(completeICWs, smarterICWs)) {
            System.out.println(icw.getOriginalClause());
            i++;
        }
        System.out.println("total:\t" + i);


        Clause c = Clause.parse("!interaction(V2, V2), !complex(V2, V4)");
//        MEDataset med = MEDataset.get(Paths.get("..", "datasets", "protein", "train.db.oneLine"), Matching.THETA_SUBSUMPTION);
        Clause conjunction = LogicUtils.flipSigns(c);
        boolean matches = med.matchesAtLeastOne(conjunction);
        System.out.println("matches at least once\t" + matches);
        int smallerMatches = 0;
        for (Literal literal : conjunction.literals()) {
            Clause smaller = new Clause(Sugar.setDifference(conjunction.literals(), literal));
            boolean match = med.matchesAtLeastOne(smaller);
            System.out.println("matches\t" + match + "\t" + smaller);
            if (match) {
                smallerMatches++;
            }
        }
        if (conjunction.countLiterals() == smallerMatches) {
            System.out.println("this clause is minimal");
        }


        IsoClauseWrapper smarterSaturated = IsoClauseWrapper.create(Clause.parse("!location(V2, V4), !location(V4, V2), interaction(V2, V2), !complex(V2, V2), !enzyme(V4, V4), !function(V2, V2), complex(V2, V4), !enzyme(V2, V4), !enzyme(V4, V2), !enzyme(V2, V2), !protein_class(V4, V4), !interaction(V4, V4), !protein_class(V2, V2), !interaction(V2, V4), !interaction(V4, V2), !phenotype(V4, V4), !protein_class(V2, V4), !protein_class(V4, V2), !phenotype(V2, V4), !phenotype(V4, V2), !phenotype(V2, V2), !complex(V2, V4), !complex(V4, V2), !complex(V4, V4), !location(V2, V2), !function(V4, V4), !function(V2, V4), !function(V4, V2), !interaction(V2, V2), !location(V4, V4)"));
        IsoClauseWrapper completeSaturated = IsoClauseWrapper.create(Clause.parse("!location(V2, V4), !location(V4, V2), interaction(V2, V2), !complex(V2, V2), !enzyme(V4, V4), !function(V2, V2), complex(V2, V4), !enzyme(V2, V4), !enzyme(V4, V2), !enzyme(V2, V2), !protein_class(V4, V4), !interaction(V4, V4), !protein_class(V2, V2), !interaction(V2, V4), !interaction(V4, V2), !phenotype(V4, V4), !protein_class(V2, V4), !protein_class(V4, V2), !phenotype(V2, V4), !phenotype(V4, V2), !phenotype(V2, V2), !complex(V2, V4), !complex(V4, V2), !complex(V4, V4), !location(V2, V2), !function(V2, V4), !function(V4, V2), !function(V4, V4), !interaction(V2, V2), !location(V4, V4)"));
        System.out.println("are saturation same\t" + smarterSaturated.equals(completeSaturated));
        System.out.println("different literals\n\t" + Sugar.setDifference(smarterSaturated.getOriginalClause().literals(), completeSaturated.getOriginalClause().literals())
                + "\n\t" + Sugar.setDifference(completeSaturated.getOriginalClause().literals(), smarterSaturated.getOriginalClause().literals()));

        List<Clause> theory = Sugar.list("!complex(V2, V2)", "!phenotype(V2, V2)", "!function(V2, V2)", "!protein_class(V2, V2)", "!location(V2, V2)", "!enzyme(V2, V2)").stream().map(Clause::parse).collect(Collectors.toList());
        Clause negativeSaturation = ConjunctureSaturator.create(theory, false, true).saturate(conjunctionCandidate);
        System.out.println(smarterSaturated.getOriginalClause().countLiterals() + "\t" + completeSaturated.getOriginalClause().countLiterals());
        System.out.println("negative\t" + negativeSaturation.countLiterals() + "\t" + negativeSaturation);

        Clause positiveSaturation = ConjunctureSaturator.create(theory, true, false).saturate(conjunctionCandidate);
        System.out.println("positive\t" + positiveSaturation.countLiterals() + "\t" + positiveSaturation);

        Clause both = ConjunctureSaturator.create(theory, true, true).saturate(conjunctionCandidate);
        System.out.println("both\t" + both.countLiterals() + "\t" + both);


        System.out.println("negative matches\t" + med.matchesAtLeastOne(negativeSaturation));
        System.out.println("both matches\t" + med.matchesAtLeastOne(both));

        System.out.println("s puvodnim theory solverem to dava stejne vysledky jako se typed verzi -- nemuze byt tedy chyba i v puvodnim? kouknout jakto ye je entailovany !x(V2, V4)");

        Clause withoutXones = Clause.parse("interaction(V2, V2), complex(V2, V4), !location(V2, V2), !location(V4, V4), !complex(V2, V2), !complex(V4, V4), !enzyme(V4, V4),  !enzyme(V2, V2), !function(V2, V2), !function(V4, V4), !interaction(V4, V4), , !protein_class(V2, V2),  !protein_class(V4, V4), !phenotype(V4, V4),   !phenotype(V2, V2)");
        System.out.println("withoutXones and !interaction(V2, V2) matches\t" + med.matchesAtLeastOne(withoutXones));


        Clause t1 = ConjunctureSaturator.create(Sugar.list("!phenotype(V2, V2)").stream().map(Clause::parse).collect(Collectors.toList()), true, true).saturate(conjunctionCandidate);
        System.out.println("t1\t" + t1.countLiterals() + "\t" + med.matchesAtLeastOne(t1) + "\t" + t1);

        /*System.out.println("iterative debug");
        Clause iterTest = Clause.parse("interaction(V2, V2)");
        for (Literal literal : Sugar.list(" complex(V2, V4)", "!phenotype(V4, V4)", "!phenotype(V2, V2)", "!phenotype(V2, V4)", "!phenotype(V4, V2)",
                "!interaction(V2, V4)", "!interaction(V4, V2)", "!interaction(V4, V4)"
                , "!complex(V4, V2)", "!complex(V2, V2)", "!complex(V4, V4),"
                , "!interaction(V2, V2)", "!complex(V2, V4),"
        ).stream().map(Literal::parseLiteral).collect(Collectors.toList())) {
            iterTest = new Clause(Sugar.iterable(iterTest.literals(), Sugar.list(literal)));
            System.out.println(literal + " added\tmatches\t" + med.matchesAtLeastOne(iterTest) + "\t" + iterTest);
        }*/
    }

    private static void runEntailmentsEvaluation() {
        boolean rewrite = false;
        int k = 7;
        String domain = "protein";
        String inputDir = "queries";

        System.setProperty("ida.pacReasoning.entailment.mode", "k");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
        System.setProperty("ida.pacReasoning.entailment.k", "" + k);
        System.setProperty("ida.pacReasoning.entailment.saturation", "sym");

        Path theoryPath = Paths.get(".", "pac", "protein", "protein.poss");
        System.getProperty("ida.pacReasoning.entailment.logic");
        FOL theory = FOL.create(theoryPath);

        Path evidenceFolder = Paths.get("..", "datasets", domain, inputDir);

        Inference inference = new Inference(theory, rewrite);

        int atMost = 1700;

        // search version
        System.setProperty("ida.pacReasoning.entailment.algorithm", "search");
        EntailmentSetting setting = EntailmentSetting.create();
        Path outputEvidence = Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + theoryPath.toFile().getName() + "_" + setting.canon());
        inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost);

        SubsetFactory.getInstance().clear();
// support version
        Path tSpath = Paths.get(".", "pac", "protein", "proteinWithoutHornHardConstraintsBenchmarkWithSymmetry.poss");
        System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
        Inference supportInference = new Inference(FOL.create(tSpath), rewrite);
        setting = EntailmentSetting.create();
        outputEvidence = Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + tSpath.toFile().getName() + "_" + setting.canon());
        supportInference.inferFolder(evidenceFolder, outputEvidence, setting, atMost);

    }

    private static void supportEntailment() {
        //Path evidencePath = Paths.get("..","datasets",domain,"train.db");
        //Path theoryPath = Paths.get(".","pac","");
        int k = 5;


        System.setProperty("ida.pacReasoning.entailment.mode", "k");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
        System.setProperty("ida.pacReasoning.entailment.k", "" + k);

        Path theoryPath = Paths.get(".", "pac", "protein", "proteinWithoutHornHardConstraints.poss");
        System.getProperty("ida.pacReasoning.entailment.logic");


        /*
                Set<Literal> evidence = Clause.parse("giraffe(k), friends(k,a), human(a)").literals();
        FOL theory = FOL.get(Sugar.set(Clause.parse("!friends(X,Y), !human(Y)")
                , Clause.parse("!friends(X,Y), !human(X)"))
                , Sugar.set(Clause.parse("!giraffe(X), animal(X)")
                ));

         */

        /*Set<Literal> evidence = Clause.parse("giraffe(liz),friends(ann,liz)").literals();
        FOL theory = FOL.get(Sugar.set(Clause.parse("!human(X), !animal(X)"))
                , Sugar.set(Clause.parse("!giraffe(X), animal(X)")
                        , Clause.parse("!friends(X,Y), friends(Y,X)")
                        , Clause.parse("!friends(X,Y), human(X)")));
        System.out.println("expected output is giraffe(liz), frineds(ann,liz), animal(liz)");
        */

        /* */
        Set<Literal> evidence = Clause.parse("giraffe(liz),friends(ann,liz)").literals();
        FOL theory = FOL.create(Sugar.set(Clause.parse("!human(X), !animal(X)"))
                , Sugar.set(Clause.parse("!giraffe(X), animal(X)")
                        , Clause.parse("!friends(X,Y), friends(Y,X)")
                        , Clause.parse("!friends(X,Y), human(X)")));
        System.out.println("expected output is giraffe(liz), friends(ann,liz), animal(liz)");
        /**/

        /* * /
        k = 2;
        Set<Literal> evidence = Clause.parse("a(a), b(a,b), x(b)").literals();
        FOL theory = FOL.get(Sugar.set()
                , Sugar.set(Clause.parse("!a(X), !b(X,Y), !x(Y), g(X)")
                        , Clause.parse("!a(X), c(X)")
                        , Clause.parse("!c(X), g(X)")
                ));
        System.out.println("expected output is g(a) {a} .... that is the main point, the rest is something like c(a) {a} and evidence with its support");
        /**/

        System.out.println("udelat si tenhle priklad v ruce pomoci Ondrovo support algoritmu :))");

        SupportEntailmentInference engine = SupportEntailmentInference.create(evidence);
        Entailed entailed = engine.entails(theory, k);
//        Entailment engine = Entailment.get(evidence, EntailmentSetting.get());
//        Entailed entailed = engine.entails(theory, k, true, null, null);

        System.out.println("\nresults\n---------------\n\n" + entailed.asOutput());

/*        //Path evidencePath = Paths.get("..","datasets",domain,"train.db");
        //Path theoryPath = Paths.get(".","pac","");
        int k = 5;


        System.setProperty("ida.pacReasoning.entailment.mode", "k");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");
        System.setProperty("ida.pacReasoning.entailment.k", "" + k);

        Path theoryPath = Paths.get(".", "pac", "protein", "proteinWithoutHornHardConstraints.poss");
        System.getProperty("ida.pacReasoning.entailment.logic");

        long start = System.nanoTime();
        for (int i = 1; i < 2000; i++) {
//        for (int i = 649; i < 700; i++) {

            System.out.println("path\t" + i);

            Path evidencePath = Paths.get("..", "datasets", "protein", "queries", "queries"+i+".db");

            Set<Literal> evidence = Utils.loadEvidence(evidencePath);
            FOL theory = FOL.get(theoryPath);

//            SupportEntailmentInference engine = SupportEntailmentInference.get(evidence);
//            Entailed entailed = engine.entails(theory, k);
            Entailment engine = Entailment.get(evidence,EntailmentSetting.get());
            Entailed entailed = engine.entails(theory,k,true,null,null);

            System.out.println("\n\noutput\n-------------------------\n" + entailed.asOutput().split("\n").length);
        }
        System.out.println("time needed\t" + (System.nanoTime() - start) / 1000000000);
*/
    }

    private static void debugHits() throws IOException {
        String domain = "nationsA2";
        int n = 5;
        int constraints = 0;

        Utils u = Utils.create();
        Path testLiterals = Paths.get("..", "datasets", domain, "hitsQueries.db");
//        Path testLiterals = Paths.get("..", "datasets", domain, "hitsQueriesDebug.db");
        Path binary = Paths.get(".", "pac", domain, "saturatedDepth1N" + n + ".pairs-k5_c" + constraints + "_debug");

        Path brutefoce = Paths.get(".", "pac", domain, "saturatedDepth1N" + n + ".pairs-k5_c" + constraints + "_bruteforce");

        List<Pair<Literal, Double>> bin = u.scoreHitsR(testLiterals, binary);

        System.out.println("scored hits by library function\n");
        bin.forEach(System.out::println);
        System.out.println("\n\n\n");

        Map<Literal, Set<Literal>> test = u.loadHitsTest(testLiterals);

        Map<Literal, Integer> here = new HashMap<>();
        Files.list(brutefoce)
                .filter(p -> p.toFile().getName().endsWith(".entailed"))
                .sorted((p1, p2) -> Integer.compare(Integer.valueOf(p1.toFile().getName().substring(0, p1.toFile().getName().length() - ".entailed".length())),
                        Integer.valueOf(p2.toFile().getName().substring(0, p2.toFile().getName().length() - ".entailed".length()))))
                .forEach(p -> {
                    int idx = Integer.parseInt(p.toFile().getName().substring(0, p.toFile().getName().length() - ".entailed".length()));
                    LogicUtils.loadEvidence(p).forEach(l -> {
                        if (here.containsKey(l)) {
                            if (idx < here.get(l)) {
                                //System.out.println("uadding\t" + l + "\t" + here.get(l));
                            }
                            here.put(l, Math.min(idx, here.get(l)));
                        } else {
                            here.put(l, idx);
                            //System.out.println("adding\t" + l + "\t" + here.get(l));
                        }
                    });
                });

//        test.values().stream().flatMap(l -> l.stream()).distinct()
//                .forEach(l -> System.out.println(l + "\t" + here.get(l)));

        System.out.println("running comparison");
        test.entrySet().forEach(e -> {
            Double innerRank = 0.0;
//            System.out.println("l\t" + e.getKey());
            for (Boolean corruptedHead : Sugar.list(true, false)) {
                int before = 0;
                int sameLevel = 1;
                Integer gold = here.containsKey(e.getKey()) ? here.get(e.getKey()) : Integer.MAX_VALUE;
                for (Literal literal : e.getValue()) {
                    if (!literal.equals(e.getKey())
                            && ((!corruptedHead && literal.get(0).equals(e.getKey().get(0)))
                            || (corruptedHead && literal.get(1).equals(e.getKey().get(1))))) {
                        int r = here.containsKey(literal) ? here.get(literal) : Integer.MAX_VALUE;
//                        System.out.println("\t" + literal + "\t" + (r < gold) + "\t" + (r == gold) + "\t\t" + r + "\t" + gold);
                        if (r < gold) {
                            before++;
                        } else if (r == gold) {
                            sameLevel++;
                        }
                    }
                }

                if (sameLevel % 2 == 0) {
//                    System.out.println("v1\t" + ((sameLevel / 2.0) + 0.5) + "\t" + sameLevel);
                    innerRank += before + (sameLevel / 2.0) + 0.5;
                } else {
//                    System.out.println("v2\t" + (before + ((sameLevel + 1) / 2.0)) + "\t" + sameLevel);
                    innerRank += before + ((sameLevel + 1) / 2.0);
                }
//                System.out.println("\t" + before + "\t" + sameLevel);
//                System.out.println("\t\t" + innerRank);
            }
            innerRank = innerRank / 2;

            Pair<Literal, Double> outerRank = Sugar.chooseOne(bin.stream().filter(p -> p.r.equals(e.getKey())).limit(1).collect(Collectors.toList()));
            System.out.println(e.getKey() + "\t" + innerRank + "\t" + outerRank.s + "\t" + (Double.compare(innerRank, outerRank.s) == 0));
        });


    }


    private static void computeHitsScore() {
        Utils u = Utils.create();

        String domain = "kinships";
//        String domain = "nations-ntp - kopie";
        String domainFinal = domain;
//        String domainFinal = "nations-ntp";
//        Path testLiterals = Paths.get("..", "datasets", domainFinal, "ntp-test.txt");
        Path testLiterals = Paths.get("..", "datasets", domainFinal, "hitsQueries.db");
        Path predictions = Paths.get(".", "pac", domain, "n10.pairs-k5_c0");

//        u.scoreHits(testLiterals, predictions);

        /**/
        int constraintsSize = 467;
        List<Path> predictionsList = Sugar.list(
                1, 2, 3, 4, 5, 10, 15, 30
                , 50, 100, 300
                //, 100, 150, 200, 250, 300

        ).stream().map(size ->
                        //Paths.get(".", "pac", domain, "n" + size + ".pairs-k5_c" + constraintsSize)
                        //Paths.get(".", "pac", domain, "saturatedDepth1N" + size + ".pairs-k5_c" + constraintsSize + "_bruteforce")
                        Paths.get(".", "pac", domain, "mn" + size + ".pairs-k5_c" + constraintsSize)
//                        Paths.get(".", "pac", domain, "saturatedDepth1N" + size + ".pairs-k5_c" + constraintsSize + "_debug")
//                Paths.get(".", "pac", domain, "saturatedDepth1N" + size + ".pairs-k5_c" + constraintsSize + "_debug2")
//                        Paths.get(".", "pac", domain, "nn" + size + ".pairs-k5_c" + constraintsSize + "_bruteforce")
                //20.pairs-k5_c0_bruteforce
        ).collect(Collectors.toList());
        //throw new NotImplementedException();
        throw new IllegalStateException();
        /*u.scoreHits(testLiterals, predictionsList, Sugar.list(1, 3, 5, 10, 20, 50));

        System.out.println(domain);*/
        /**/
    }


    private static void runTest() throws IOException {
        int k = 5;
//        String domain = "umls-ntp";
//        int n = 100;
        System.out.println("pustit verzi s constraints bez npt datasetu :))");

        for (String domain : Sugar.list(
                //"umls-ntp", "kinships-ntp", "nations-ntp"
                //"nationsA2",
//                "kinships", "umls", "nationsA2"
                "nationsA2", "umls", "kinships"
        )) {
            for (Path constraints : Sugar.list(Paths.get("..", "datasets", "emptyConstraints.txt")
                    , Paths.get("..", "datasets", domain, "constraints.txt")
            )) {
                System.out.println("********************** constraints\t" + constraints);
                for (Integer n : Sugar.list(
                        //10, 50, 100//,
                        //150, 200, 250, 300
                        //5, 10//, 10, 20
                        //5, 10
                        //1, 2, 3, 4, 5, 10, 15, 30, 50, 100//, 150, 200, 250, 300
                        300
                )) {

                    if ((constraints.toFile().getName().endsWith("constraints.txt") && "nationsA2".equals(domain))
                            || (!constraints.toFile().getName().endsWith("constraints.txt") && !"nationsA2".equals(domain))) {
                        continue;
                    }

                    System.out.println("\n--------------------------\ncomputing\n\t\t" + domain + "\t" + n + "\n\n");


                    Path sortedRules = Paths.get(".", "pac", domain, "mn" + n + ".pairs");
                    //Path sortedRules = Paths.get(".", "pac", domain, "n" + n + ".pairs");
                    //Path sortedRules = Paths.get(".", "pac", domain, "saturatedDepth1N" + n + ".pairs");

                /* pro ntp data
                Path evidence = Paths.get("..", "datasets", domain, "train.db");
                Path testLiterals = Paths.get("..", "datasets", domain, "ntp-test.txt");
//                Path constraints = Paths.get("..", "datasets", domain, "constraints.txt");
//                Path constraints = Paths.get("..", "datasets", domain.substring(0,domain.length()-"-ntp".length()), "constraints.txt");
                Path constraints = Paths.get("..", "datasets", "emptyConstraints.txt");
                */
                    Path evidence = Paths.get("..", "datasets", domain, "testQueriesEvidence.db");
                    Path testLiterals = Paths.get("..", "datasets", domain, "hitsQueries.db");
//                    Path evidence = Paths.get("..", "datasets", domain, "train.db");
//                    Path testLiterals = Paths.get("..", "datasets", domain, "ntp-test.txt");


                    Utils u = Utils.create();

                    List<Clause> rules = Files.lines(sortedRules).filter(line -> line.trim().length() > 0).map(line -> Clause.parse(line.split("\\s+", 2)[1])).collect(Collectors.toList());
                    HitsInference inference = new HitsInference(null);
                    inference.infer(k, sortedRules, LogicUtils.loadEvidence(evidence), testLiterals, rules, u.loadClauses(constraints));

                    if (false) {
                        System.out.println("debug inference");
                        inference.bruteforce(k, sortedRules, LogicUtils.loadEvidence(evidence), testLiterals, rules, u.loadClauses(constraints));
                    }
                }
            }
        }
    }

    // assume already filtered out
    private static void pickRules() throws IOException {
//        String domain = "nations-ntp";
//        int n = 100;
        Utils u = Utils.create();

        boolean comparingSetting = false;
        System.out.println("using length for comparison as well\t" + comparingSetting);

        for (String domain : Sugar.list("nations-ntp", "umls-ntp", "kinships-ntp")) {
//        for (String domain : Sugar.list("nationsA2", "kinships", "umls")) {
//        {
//            String domain = "nationsA2";

            System.out.println(domain);

            Path pairs = Paths.get(".", "pac", domain, "hornLearner.logic.pairs");

            // max acc, max # literals if comparingSettig = true
//            Comparator<? super Pair<Double, Clause>> comparator = (comparingSetting) ? (p1, p2) -> {
//                int acc = Double.compare(p2.getR(), p1.getR());
//                if (0 == acc) {
//                    return Integer.compare(p2.getS().countLiterals(), p1.getS().countLiterals());
//                }
//                return acc;}
//                ? (p1,p2) -> Double.compare(p2.getR(), p1.getR())
//                ;

            Comparator<? super Pair<Double, Clause>> comparator = (p1, p2) -> {
                int acc = Double.compare(p2.getR(), p1.getR());
                if (!comparingSetting) {
                    return acc;
                }
                if (0 == acc) {
                    return Integer.compare(p2.getS().countLiterals(), p1.getS().countLiterals());
                }
                return acc;
            };

            List<Clause> constraints = Sugar.listFromCollections(u.loadClauses(Paths.get("..", "datasets", domain, "constraints.txt")));
//            RuleSaturator saturator = "nationsA2".equals(domain) ? null : RuleSaturator.get(constraints);
            RuleSaturator saturator = null;
            System.out.println("saturator is turned off since the data comes from search which uses saturations");

            Map<Literal, List<Pair<Double, Clause>>> rules = Files.lines(pairs).filter(line -> line.trim().length() > 0)
                    .map(line -> {
                        String[] splitted = line.split("\\s+", 2);
                        Clause clause = Clause.parse(splitted[1]);

                        // just a check
                        if (null != saturator) {
                            if (saturator.isPruned(clause)) {
                                return null;
                            }
                        }

                        return new Pair<>(Double.parseDouble(splitted[0]), clause);
                    }).filter(Objects::nonNull)
                    .collect(Collectors.groupingBy(pair -> Sugar.chooseOne(LogicUtils.positiveLiterals(pair.getS()))));

            for (Integer n : Sugar.list(1, 2, 3, 4, 5, 10, 15, 30, 50, 100, 150, 200, 250, 300)) {
                System.out.println("\t" + n);
                //Path out = Paths.get(".", "pac", domain, "saturatedDepth1N" + n + ".pairs");
                Path out = Paths.get(".", "pac", domain, "mn" + n + ".pairs");
                if (out.toFile().exists()) {
                    continue;
                }
                List<Pair<Double, Clause>> selected = rules.entrySet().stream()
                        .flatMap(entry -> entry.getValue().stream().sorted(comparator).limit(n))
                        .sorted(comparator)
                        .collect(Collectors.toList());
                Files.write(out, selected.stream().map(p -> p.getR() + "\t" + p.getS()).collect(Collectors.toList()));
            }
        }

    }

    private static void findHorns() throws IOException {
        Path ruleFile = Paths.get("..", "datasets", "umls-ntp", "constraints.txt");

        Files.lines(ruleFile).filter(line -> line.trim().length() > 0)
                .filter(line -> LogicUtils.positiveLiterals(Clause.parse(line)).size() > 0)
                .forEach(System.out::println);
    }


    private static void subselectRules() throws IOException {
        int maxLit = 3;
        Path ruleFile = Paths.get(".", "pac", "nationsA2", "hornLearner.logic.pairs");
        Files.write(Paths.get(ruleFile.toString() + ".maxLength" + maxLit),
                Files.lines(ruleFile)
                        .filter(line -> line.trim().length() > 0)
                        .filter(line -> {
                            String[] splitted = line.split("\\s", 2);
                            return Clause.parse(splitted[1]).literals().size() <= maxLit;
                        })
                        .collect(Collectors.toList())
        );
    }

    private static void fillInSymmetreis() throws IOException {
        Path test = Paths.get("..", "datasets", "protein", "train.db");
        Path output = Paths.get("..", "datasets", "protein", "train.symmetric.db");


        if (!output.toFile().exists()) {
            Set<Literal> literals = LogicUtils.loadEvidence(test);
            Set<Literal> extended = literals.stream().filter(l -> l.getPredicate().r.equals("interaction"))
                    .map(l -> {
                        List<Term> list = l.argumentsStream().collect(Collectors.toList());
                        Collections.reverse(list);
                        return new Literal(l.getPredicate().r, false, list);
                    }).collect(Collectors.toSet());
            extended.addAll(literals);
            Files.write(output, extended.stream().map(Object::toString).collect(Collectors.toList()));
            Files.write(Paths.get(output.toString() + ".oneLine"),
                    Sugar.list("+ " + (new Clause(extended)).toString()));
        }

    }

    private static void filterSymmetry() throws IOException {
        String domain = "protein";
        Path proteinMlns = Paths.get(".", "pac", domain, "mlns");
        Path evidence = Paths.get("..", "datasets", domain, "queries");

        /*
        System.out.println(proteinMlns);
        Files.list(proteinMlns).filter(p -> p.toString().endsWith(".db"))
                .forEach(path -> {
                    System.out.println(path);
                    Set<Literal> evd = Utils.loadEvidence(Paths.get(evidence.toString(), "queries" + path.toFile().getName()));
                    Set<Literal> predicted = Utils.loadEvidence(Paths.get(proteinMlns.toString(), path.toFile().getName()));
                    List<Literal> filtered = predicted.stream().filter(l -> {
                        if (l.predicate().equals("interaction") && l.arity() == 2) {
                            Literal reversed = LogicUtils.reverseArguments(l);
                            if (!evd.contains(l) && evd.contains(reversed)) {
                                return false;
                            }
                        }
                        return true;
                    }).collect(Collectors.toList());
                    Path target = Paths.get(proteinMlns.toString() + "WithoutSymmetry");
                    if (!target.toFile().exists()) {
                        target.toFile().mkdirs();
                    }
                    Path out = Paths.get(target.toString(), path.toFile().getName());
                    try {
                        Files.write(out, filtered.stream().map(Object::toString).collect(Collectors.toList()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println(path + "\t" + predicted.size() + "\t" + filtered.size());
                });
        */

        Path k5 = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        System.out.println("\nk5\n");
        int k = 5;
        System.out.println(k5);
        Files.list(k5)
                .filter(p -> p.toString().endsWith(".db"))
                .forEach(path -> {
                    Set<Literal> evd = LogicUtils.loadEvidence(Paths.get(evidence.toString(), path.toFile().getName()));
                    VECollector vec = VECollector.load(Paths.get(k5.toString(), path.toFile().getName()), 5);
                    Set<Literal> filtered = vec.getEvidence().stream().filter(l -> {
                        if (l.predicate().equals("interaction") && l.arity() == 2) {
                            Literal reversed = LogicUtils.reverseArguments(l);
                            if (!evd.contains(l) && evd.contains(reversed)) {
                                return false;
                            }
                        }
                        return true;
                    }).collect(Collectors.toSet());
                    Path target = Paths.get(k5.toString() + "WithoutSymmetry");
                    if (!target.toFile().exists()) {
                        target.toFile().mkdirs();
                    }
                    Path out = Paths.get(target.toString(), path.toFile().getName());
                    try {
                        VECollector filteredVEC = VECollector.create(filtered, vec.getVotes(), k, vec.constantsSize(), vec.getConstants(), vec.originalEvidenceSize());
                        Files.write(out, Sugar.list(filteredVEC.asOutput()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println(path + "\t" + vec.getEvidence().size() + "\t" + filtered.size());
                });

    }


    private static void plotScatter() {
        Utils utils = Utils.create();

        /* * /
        Path groundTruth = Paths.get("..", "datasets", "kinships", "test.db");
        //Path evidence = Paths.get("..", "datasets", "kinships", "queries"); // pro protein a uwcs
        Path evidence = Paths.get("..", "datasets", "kinships", "test-uniform");
//        Path dataA = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000.aggergatedKG.train.db.approxAcc_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k2 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K2Gorig.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K3Gorig.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K4Gorig.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k5 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K5Gorig.theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");

        Path dataA = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000.aggergatedKGConstraints.train.db.approxAcc_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k2 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K2GorigConstraints.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K3GorigConstraints.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K4GorigConstraints.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k5 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K5GorigConstraints.theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");


        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<>(new Pair<>(dataA, 0), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k2, 2), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k3, 3), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k4, 4), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k5, 5), Sugar.list(KCut.K_CUT))
        );

        List<Data<Integer, VECollector>> data = paths.stream()
                .flatMap(p -> {
                    Data<Integer, VECollector> d = Data.loadResults(p.r);
                    return p.getS().stream().map(cut -> {
                        Data<Integer, VECollector> cutted = utils.cut(d, cut);
                        return cutted.sublist(0, 300, cutted.getName());
                    });
                }).collect(Collectors.toList());

        int binSize = 10;
        /**/

        // domain
        /*
                        new Pair<>(2000, "protein") // max 4338
                , new Pair<>(1000, "kinships") // max 2649
                , new Pair<>(300, "uwcs") // max 1002
                , new Pair<>(400, "umls") // max 1410
                , new Pair<>(400, "nations") // max 701 // nations je hodne tezky
         */
        /* */
        int to = 2000;
        int binSize = to / 10;
        String domain = "protein";
        //Path groundTruth = Paths.get("..", "datasets", domain, "test.db");
        Path groundTruth = Paths.get("..", "datasets", domain, "test.symmetric.db");
        Path evidence = Paths.get("..", "datasets", domain, "uwcs".equals(domain) || "protein".equals(domain) ? "queries" : "test-uniform");
//        Path evidence = Paths.get("..", "datasets", domain, "queries");

        String input = "uwcs".equals(domain) || "protein".equals(domain) ? "queries" : "test-uniform";
        String sat = "protein".equals(domain) ? "sym" : "full";

        String withoutConstraints = false ? "WC" : "";

        Path dataAprecis = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp0n10000m10G" + withoutConstraints + ".theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k2precis = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp0n10000m10G" + withoutConstraints + ".theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k3precis = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp0n10000m10G" + withoutConstraints + ".theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k4precis = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp0n10000m10G" + withoutConstraints + ".theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k5precis = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp0n10000m10G" + withoutConstraints + ".theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k6precis = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp0n10000m10G" + withoutConstraints + ".theory_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");

        int merpp = 5;
        Path dataAerr = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp" + merpp + "n10000m10G" + withoutConstraints + ".theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k2err = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp" + merpp + "n10000m10G" + withoutConstraints + ".theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k3err = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp" + merpp + "n10000m10G" + withoutConstraints + ".theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k4err = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp" + merpp + "n10000m10G" + withoutConstraints + ".theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k5err = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp" + merpp + "n10000m10G" + withoutConstraints + ".theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k6err = Paths.get(".", "pac", domain, "src-" + input + "_t-merpp" + merpp + "n10000m10G" + withoutConstraints + ".theory_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");

        Path uwcsPLentailment = Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path uwcsK5 = Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-5_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path uwcsMlns = Paths.get(".", "pac", "uwcs", "mlns");

        Path proteinPosTheoryK5 = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path proteinMlns = Paths.get(".", "pac", domain, "mlns");
        Path proteinPosLogTheoryClassicalEntailment = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");

        Path proteinMlnsWithoutSym = Paths.get(".", "pac", domain, "mlnsWithoutSymmetry");
        Path proteinPosTheoryK5WithoutSym = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-tWithoutSymmetry");

//        BiFunction<Path, Integer, List<Cut>> genCuts = (p, k) -> {
//            int innerMerpp = p.toString().contains("-merpp" + merpp) ? merpp : 0;
//            return Sugar.list(KCut.K_CUT
//                    , ConstantGammaCut.load(Paths.get(".", "pac", domain, "t-merpp" + innerMerpp + "n10000m10G.theory-train-randomWalk-k" + k + "-ratio-1.0-cg.cut"), k)
//                    , ConstantGammaCut.load(Paths.get(".", "pac", domain, "t-merpp" + innerMerpp + "n10000m10G.theory-train-randomWalk-k" + k + "-ratio-1.0E-5-cg.cut"), k)
//                    , ConstantGammaCut.load(Paths.get(".", "pac", domain, "t-merpp" + innerMerpp + "n10000m10G.theory-train-snowball5-k" + k + "-ratio-1.0-cg.cut"), k)
//                    , ConstantGammaCut.load(Paths.get(".", "pac", domain, "t-merpp" + innerMerpp + "n10000m10G.theory-train-snowball5-k" + k + "-ratio-1.0E-5-cg.cut"), k)
//            );
//        };

        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
//                new Pair<>(new Pair<>(dataAprecis, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k2precis, 2), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k3precis, 3), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k4precis, 4), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k5precis, 5), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k6precis, 6), Sugar.list(KCut.K_CUT))
//                ,
//                new Pair<>(new Pair<>(dataAerr, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k2err, 2), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k3err, 3), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k4err, 4), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k5err, 5), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k6err, 6), Sugar.list(KCut.K_CUT))

//                , new Pair<>(new Pair<>(k4precis, 4), genCuts.apply(k6precis, 4))
//                , new Pair<>(new Pair<>(k4err, 4), genCuts.apply(k6err, 4))

//                , new Pair<>(new Pair<>(uwcsPLentailment, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(uwcsK5, 5), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(uwcsMlns, 0), Sugar.list(KCut.K_CUT))

//                new Pair<>(new Pair<>(uwcsPLentailment, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(uwcsK5, 5), Sugar.list(KCut.K_CUT
//                        , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss-train-randomWalk-k5-ratio-1.0E-5-cg.cut"), 5)
//                        , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss-train-randomWalk-k5-ratio-1.0-cg.cut"), 5)
//                        , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss-train-snowball5-k5-ratio-1.0E-5-cg.cut"), 5)
//                        , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss-train-snowball5-k5-ratio-1.0-cg.cut"), 5)
//                ))
//                , new Pair<>(new Pair<>(uwcsMlns, 0), Sugar.list(KCut.K_CUT))

//                new Pair<>(new Pair<>(proteinPosLogTheoryClassicalEntailment, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(proteinPosTheoryK5, 5), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(proteinPosTheoryK5, 5), Sugar.list(KCut.K_CUT
//                        , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-randomWalk-k5-ratio-1.0E-5-cg.cut"), 5)
//                        , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-randomWalk-k5-ratio-1.0-cg.cut"), 5)
//                        , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k5-ratio-1.0E-5-cg.cut"), 5)
//                        , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k5-ratio-1.0-cg.cut"), 5)
//        ))
                //, new Pair<>(new Pair<>(proteinMlns, 0), Sugar.list(KCut.K_CUT))
                new Pair<>(new Pair<>(proteinPosTheoryK5WithoutSym, 5), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(proteinMlnsWithoutSym, 0), Sugar.list(KCut.K_CUT))
        );
        /**/

        /* * /
        // states -> 500
        // politics -> 300
        // people -> 200
        Integer merpp = null;
// politics,statse,
        int to = 200;
        int binSize = to / 10;
        String domain = "yago-politics";
        Path groundTruth = Paths.get("..", "datasets", domain, "test.db");
        Path evidence = Paths.get("..", "datasets", domain, "uwcs".equals(domain) || "protein".equals(domain) ? "queries" : "test-uniform");
//        Path evidence = Paths.get("..", "datasets", domain, "queries");

        String input = "uwcs".equals(domain) || "protein".equals(domain) ? "queries" : "test-uniform";
        String sat = "protein".equals(domain) ? "sym" : "full";
        if (domain.contains("yago")) {
            sat = "none";
        }

        String ms = true ? "MS" : "";
        Path entailment = Paths.get(".", "pac", domain, "src-" + input + "_t-amieRuleDepth4" + ms + ".logic_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k2 = Paths.get(".", "pac", domain, "src-" + input + "_t-amieRuleDepth4" + ms + ".logic_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k3 = Paths.get(".", "pac", domain, "src-" + input + "_t-amieRuleDepth4" + ms + ".logic_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k4 = Paths.get(".", "pac", domain, "src-" + input + "_t-amieRuleDepth4" + ms + ".logic_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
//        Path k5 = Paths.get(".", "pac", domain, "src-" + input + "_t-amieRuleDepth4"+ms+".logic_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");
        Path k6 = Paths.get(".", "pac", domain, "src-" + input + "_t-amieRuleDepth4" + ms + ".logic_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-" + sat + "_ms-t");

        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<>(new Pair<>(entailment, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k2, 2), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k3, 3), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k4, 4), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k5, 5), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(k6, 6), Sugar.list(KCut.K_CUT))
        );
        /**/

        List<Data<Integer, VECollector>> data = paths.stream()
                .flatMap(p -> {
                    Data<Integer, VECollector> d = Data.loadResults(p.r);
                    return p.getS().stream().map(cut -> {
                        Data<Integer, VECollector> cutted = utils.cut(d, cut);
                        return cutted.sublist(0, to, cutted.getName());
                    });
                }).collect(Collectors.toList());


        System.out.println(utils.scatter(groundTruth, evidence, data, binSize));
        System.out.println("% binSize = " + binSize + "\tmerpp = " + merpp + "\tdomain " + domain + "\tto " + to);

        System.out.println(utils.plotXHEDCollectors(groundTruth, data, true, true));
    }

    private static void plotFinal() {
        boolean maskGT = true;
        Utils utils = Utils.create();

        /* */
        // kinships with and without constraints
        Path groundTruth = Paths.get("..", "datasets", "kinships", "test.db");
        Path evd = Paths.get("..", "datasets", "kinships", "test-uniform");
//        Path dataA = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000.aggergatedKG.train.db.approxAcc_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k2 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K2Gorig.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K3Gorig.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K4Gorig.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k5 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K5Gorig.theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");

//        Path dataA = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000.aggergatedKGConstraints.train.db.approxAcc_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k2 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K2GorigConstraints.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K3GorigConstraints.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K4GorigConstraints.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k5 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K5GorigConstraints.theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");

        Path dataAprecise = Paths.get(".", "pac", "kinships", "src-test-uniform_t-merpp0n10000m10G.theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path dataAerror = Paths.get(".", "pac", "kinships", "src-test-uniform_t-merpp4n10000m10G.theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path k6precise = Paths.get(".", "pac", "kinships", "src-test-uniform_t-merpp0n10000m10G.theory_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path k6error = Paths.get(".", "pac", "kinships", "src-test-uniform_t-merpp4n10000m10G.theory_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");


        List<Triple<Path, Integer, List<Cut>>> data = Sugar.list(new Triple<>(dataAprecise, 0, Sugar.list(KCut.K_CUT))
                , new Triple<>(dataAerror, 0, Sugar.list(KCut.K_CUT))
                , new Triple<>(k6precise, 6, Sugar.list(KCut.K_CUT))
                , new Triple<>(k6error, 6, Sugar.list(KCut.K_CUT))
        );
        int binSize = 50;
        int atMost = 500;

        /*
        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<>(new Pair<>(dataA, 0), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k2, 2), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k3, 3), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k4, 4), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k5, 5), Sugar.list(KCut.K_CUT))
        );

        List<Data<Integer, VECollector>> fpInput = paths.stream()
                .flatMap(p -> {
                    Data<Integer, VECollector> d = Data.loadResults(p.r);
                    return p.getS().stream().map(cut -> {
                        Data<Integer, VECollector> cutted = utils.cut(d, cut);
                        return cutted.sublist(0, 300, cutted.getName());
                    });
                }).collect(Collectors.toList());
         */
        /**/

        /* * /
        Path groundTruth = Paths.get("..", "datasets", "protein", "test.db");
        Path evd = Paths.get("..", "datasets", "protein", "queries");
        Path mlns = Paths.get(".", "pac", "protein", "mlns");
        Path k5 = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        List<Cut> k5Cuts = Sugar.list(KCut.K_CUT
                , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-randomWalk-k5-ratio-1.0-cg.cut"), 5)
//                , (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-randomWalk-k5-ratio-1.0E-5-cg.cut"), 5)
        );

        Path k0merpp4 = Paths.get(".", "pac", "protein", "src-queries_t-merpp4n10000m10G.theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path k6merpp4 = Paths.get(".", "pac", "protein", "src-queries_t-merpp4n10000m10G.theory_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");

        List<Triple<Path, Integer, List<Cut>>> data = Sugar.list(new Triple<>(mlns, 0, Sugar.list(KCut.K_CUT))
                , new Triple<>(k5, 5, k5Cuts)
                , new Triple<>(k0merpp4, 0, Sugar.list(KCut.K_CUT))
                , new Triple<>(k6merpp4, 6, Sugar.list(KCut.K_CUT))
        );

        //        System.out.println(replacement(utils.plotCHEDwithGroupCut(groundTruth, paths, maskGT)));
//        System.out.println(replacement(utils.plotFPFN(Utils.loadEvidence(groundTruth), fpInput, true, null)));
        int atMost = 1300;
        int binSize = 100;
        /**/
         /* * /

        Path groundTruth = Paths.get("..", "datasets", "uwcs", "test.db");
        Path evd = Paths.get("..", "datasets", "uwcs", "queries");
        Path mlns = Paths.get(".", "pac", "uwcs", "mlns");
        Path k5 = Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-5_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        List<Cut> k5Cuts = Sugar.list(KCut.K_CUT);

        List<Triple<Path, Integer, List<Cut>>> data = Sugar.list(new Triple<>(mlns, 0, Sugar.list(KCut.K_CUT))
                , new Triple<>(k5, 5, k5Cuts));

        //        System.out.println(replacement(utils.plotCHEDwithGroupCut(groundTruth, paths, maskGT)));
//        System.out.println(replacement(utils.plotFPFN(Utils.loadEvidence(groundTruth), fpInput, true, null)));
        int atMost = 500;
        int binSize = 50;
        /**/

//        List<Pair<Pair<Path, Integer>, List<Cut>>> pairs = data.stream().map(triple -> new Pair<>(new Pair<>(triple.r, triple.s), triple.t)).collect(Collectors.toList());
//        System.out.println(replacement(utils.plotCHEDwithGroupCut(groundTruth, pairs, true)));
        System.out.println(utils.scatterCuts(groundTruth, evd, data, binSize, atMost));
    }

    private static void yagoIncorporateConstraints() throws IOException {
        Utils u = Utils.create();
        for (String domain : Sugar.list("yago-states", "yago-politics", "yago-people", "yago-musician", "yago-film")) {

            Path constraintsPath = Paths.get("..", "datasets", domain, "constraints.txt");
            Path amie = Paths.get(".", "pac", domain, "amieRuleDepth4.logic");

            Set<Clause> hardRules = u.loadClauses(constraintsPath);
            Set<Clause> implications = u.loadClauses(amie);
            FOL theory = FOL.create(hardRules, implications);

            Files.write(Paths.get(".", "pac", domain, "amieRuleDepth4Constraints.logic"), Sugar.list(theory.toString()));
        }
    }

    private static void selectRules() throws IOException {
        Utils u = Utils.create();
        boolean groundOnly = true;
        int maxRulesPerPredicate = 10; // randomly choosen
//        for (String domain : Sugar.list("yago-states", "yago-politics", "yago-people", "yago-musician", "yago-film")) {
        for (String domain : Sugar.list("kinships", "umls", "nations", "uwcs", "protein")) {
//        {
//            String domain = "protein";
            System.out.println("domain\t" + domain);

            Matching matching = Matching.create(new Clause(LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "train.db"))), Matching.THETA_SUBSUMPTION);

            Path constraintsPath = Paths.get("..", "datasets", domain, "constraints.txt");

            List<Clause> constraints = Sugar.listFromCollections(u.loadClauses(constraintsPath));
            RuleSaturator saturator = RuleSaturator.create(constraints);


            int maxK = 5;

//            for (Integer k : Sugar.list(2, 3, 4)) {
            {
                for (Integer merpp : Sugar.list(0, 10, 100, 1000)) {
                    if (100 <= merpp && !domain.equals("protein")) {
                        continue;
                    }
                    if (10 == merpp && domain.equals("nations")) {
                        merpp = 5;
                    }
                    System.out.println("\tmerpp\t" + merpp);
                    // merpp max rules with error per predicate
//                    Path output = Paths.get(".", "pac", domain, "maxErr" + maxError + "K" + k + "orig" + subsampled + ".theory");
                    Path output = Paths.get(".", "pac", domain, "merpp" + merpp + "n10000m" + maxRulesPerPredicate + (groundOnly ? "G" : "") + ".theory");
                    Path zeroError = Paths.get(".", "pac", domain, "merpp0n10000m" + maxRulesPerPredicate + (groundOnly ? "G" : "") + ".theory");

                    if (!output.toFile().exists()) {
                        //Path train = Paths.get(".", "pac", domain, "amieRuleDepth4K" + k + ".train.db." + subsampled + ".uniformLit.subampled.approxAcc");
                        Path train = Paths.get(".", "pac", domain, "hornLearner.logic.pairs");

                        List<Pair<Double, Clause>> pairs = Files.lines(train).filter(line -> line.trim().length() > 0 && !line.trim().startsWith("#"))
                                .map(line -> {
                                    String[] splitted = line.split("\t", 2);
                                    Clause clause = Clause.parse(splitted[1]);

                                    if (groundOnly) {
                                        HornClause horn = HornClause.create(clause);
                                    /*if (!matching.subsumption(horn.body(), 0)) { // no grounding of the body exists
                                        return null;
                                    }*/
                                        Pair<Term[], List<Term[]>> substitution = matching.allSubstitutions(horn.body(), 0, Integer.MAX_VALUE);
                                        boolean atLeastOneKSizeGround = substitution.s.stream().anyMatch(arr -> Arrays.stream(arr).distinct().count() <= maxK);
                                        if (!atLeastOneKSizeGround) {
                                            return null;
                                        }
                                    }

                                    return new Pair<>(Double.parseDouble(splitted[0]), clause);
                                }).filter(Objects::nonNull).sorted(Comparator.comparing(Pair<Double, Clause>::getR).reversed()).collect(Collectors.toList());

                        int idx = 0;
                        for (; idx < pairs.size(); idx++) {
                            if (pairs.get(idx).r < 1.0) {
                                break;
                            }
                        }

                        List<Clause> selected = Sugar.list();
                        List<Pair<Double, Clause>> moreThanZeroTrainingError = pairs.subList(idx, pairs.size());

                        if (0 == merpp) {
                            Map<Pair<String, Integer>, List<Clause>> map = pairs.subList(0, idx).stream().collect(Collectors.groupingBy(p ->
                                            Sugar.chooseOne(LogicUtils.positiveLiterals(p.s)).getPredicate()
                                    , Collectors.mapping(Pair::getS, Collectors.toList())));

                            for (Map.Entry<Pair<String, Integer>, List<Clause>> pairListEntry : map.entrySet()) {
                                List<Clause> rules = pairListEntry.getValue();
                                //Collections.shuffle(rules);
                                Collections.sort(rules, (r1, r2) -> {
                                    Set<Variable> v1 = LogicUtils.variables(r1);
                                    Set<Variable> v2 = LogicUtils.variables(r2);
                                    if (v1.size() != v2.size()) {
                                        return Integer.compare(v1.size(), v2.size());
                                    }
                                    return Integer.compare(r1.countLiterals(), r2.countLiterals());
                                });

                                if ("nations".equals(domain)) { // protoze uvnitr nejsou profiltrovany sporny s teorii
                                    int canAdd = maxRulesPerPredicate;
                                    for (Clause rule : rules) {
                                        if (canAdd < 1) {
                                            break;
                                        }
                                        if (!saturator.isRuleInconsistent(HornClause.create(rule))) {
                                            selected.add(rule);
                                            canAdd--;
                                        } else {
                                            System.out.println("ta");
                                        }
                                    }
                                } else {
                                    selected.addAll(rules.subList(0, Math.min(maxRulesPerPredicate, rules.size())));
                                }
                            }
                        } else {
                            FOL theory = FOL.create(zeroError);
                            selected.addAll(Sugar.setDifference(theory.allRules(), theory.getHardRules()));
                        }

                        Map<Pair<String, Integer>, List<Pair<Double, Clause>>> rulesWithError = moreThanZeroTrainingError.stream().collect(Collectors.groupingBy(p ->
                                        Sugar.chooseOne(LogicUtils.positiveLiterals(p.s)).getPredicate()
                                , Collectors.toList()));

                        Integer finalMerpp = merpp;
                        rulesWithError.entrySet().forEach(entry -> {
                            List<Pair<Double, Clause>> rules = entry.getValue();
                            Collections.sort(rules, Comparator.comparing(Pair<Double, Clause>::getR).reversed());
                            if ("nations".equals(domain)) { // protoze uvnitr nejsou profiltrovany sporny s teorii
                                int canAdd = finalMerpp;
                                for (Pair<Double, Clause> p : rules) {
                                    if (canAdd < 1) {
                                        break;
                                    }
//                                    HornClause saturatedBody = saturator.saturate(HornClause.get(p.s));
                                    if (!saturator.isRuleInconsistent(HornClause.create(p.s))) {
                                        selected.add(p.s);
                                        canAdd--;
                                    } else {
                                        System.out.println("ta");
                                    }
                                }
                            } else {
                                selected.addAll(rules.subList(0, Math.min(rules.size(), finalMerpp)).stream().map(Pair::getS).collect(Collectors.toList()));
                            }
                        });

                        List<String> all = Sugar.list("hard rules");
                        all.addAll(constraints.stream().map(Object::toString).collect(Collectors.toList()));
                        all.add("implications");
                        all.addAll(selected.stream().map(Object::toString).collect(Collectors.toList()));
                        Files.write(output, all);
                    }

                    Path outputWithoutConstraints = Paths.get(".", "pac", domain, "merpp" + merpp + "n10000m" + maxRulesPerPredicate + (groundOnly ? "G" : "") + "WC.theory");
                    Path zeroErrorWithoutConstraints = Paths.get(".", "pac", domain, "merpp0n10000m" + maxRulesPerPredicate + (groundOnly ? "G" : "") + "WC.theory");
                    for (Pair<Path, Path> pathPathPair : Sugar.list(new Pair<>(output, outputWithoutConstraints), new Pair<>(zeroError, zeroErrorWithoutConstraints))) {
                        Path withoutConstraints = pathPathPair.s;
                        if (!withoutConstraints.toFile().exists()) {
                            FOL fol = FOL.create(pathPathPair.r);
                            Set<Clause> implications = Sugar.setDifference(fol.allRules(), fol.getHardRules());
                            Files.write(withoutConstraints, Sugar.list(FOL.create(Sugar.set(), implications).toString()));
                        }
                    }


                }
            }
        }
    }

    private static void selectRulesWrtError() throws IOException {
//        double maxError = 0.01;
//        String domain = "yago-states";
//        int k = 2;
//        int subsampled = 4000;
        Utils u = Utils.create();
        boolean groundOnly = true;
        int maxRulesPerPredicate = 10; // randomly choosen
//        for (String domain : Sugar.list("yago-states", "yago-politics", "yago-people", "yago-musician", "yago-film")) {
//        for (String domain : Sugar.list("kinships", "umls", "nations", "uwcs", "protein")) {
        {
            String domain = "protein";
            System.out.println("domain\t" + domain);

            Matching matching = Matching.create(new Clause(LogicUtils.loadEvidence(Paths.get("..", "datasets", domain, "train.db"))), Matching.THETA_SUBSUMPTION);

            Path constraintsPath = Paths.get("..", "datasets", domain, "constraints.txt");

            List<Clause> constraints = Sugar.listFromCollections(u.loadClauses(constraintsPath));

            int maxK = 5;

//            for (Integer k : Sugar.list(2, 3, 4)) {
            {
                for (Double maxErr : Sugar.list(0.00001, 0.0)) {
                    System.out.println("\terr\t" + maxErr);
                    double maxError = maxErr;

//                    Path output = Paths.get(".", "pac", domain, "maxErr" + maxError + "K" + k + "orig" + subsampled + ".theory");
                    Path output = Paths.get(".", "pac", domain, "maxErr" + maxError + "n10000m" + maxRulesPerPredicate + (groundOnly ? "G" : "") + ".theory");

                    if (!output.toFile().exists()) {
                        //Path train = Paths.get(".", "pac", domain, "amieRuleDepth4K" + k + ".train.db." + subsampled + ".uniformLit.subampled.approxAcc");
                        Path train = Paths.get(".", "pac", domain, "hornLearner.logic.pairs");

                        List<Pair<Double, Clause>> pairs = Files.lines(train).filter(line -> line.trim().length() > 0 && !line.trim().startsWith("#"))
                                .map(line -> {
                                    String[] splitted = line.split("\t", 2);
                                    Clause clause = Clause.parse(splitted[1]);

                                    if (groundOnly) {
                                        HornClause horn = HornClause.create(clause);
                                    /*if (!matching.subsumption(horn.body(), 0)) { // no grounding of the body exists
                                        return null;
                                    }*/
                                        Pair<Term[], List<Term[]>> substitution = matching.allSubstitutions(horn.body(), 0, Integer.MAX_VALUE);
                                        boolean atLeastOneKSizeGround = substitution.s.stream().anyMatch(arr -> Arrays.stream(arr).distinct().count() <= maxK);
                                        if (!atLeastOneKSizeGround) {
                                            return null;
                                        }
                                    }

                                    return new Pair<>(Double.parseDouble(splitted[0]), clause);
                                }).filter(Objects::nonNull).sorted(Comparator.comparing(Pair<Double, Clause>::getR).reversed()).collect(Collectors.toList());

                        int idx = 0;
                        for (; idx < pairs.size(); idx++) {
                            if (pairs.get(idx).r < 1.0) {
                                break;
                            }
                        }

                        List<Clause> selected = Sugar.list();
                        List<Pair<Double, Clause>> lessThanZeroTrainingError = pairs.subList(idx, pairs.size());
                        Map<Pair<String, Integer>, List<Clause>> map = pairs.subList(0, idx).stream().collect(Collectors.groupingBy(p ->
                                        Sugar.chooseOne(LogicUtils.positiveLiterals(p.s)).getPredicate()
                                , Collectors.mapping(Pair::getS, Collectors.toList())));

                        for (Map.Entry<Pair<String, Integer>, List<Clause>> pairListEntry : map.entrySet()) {
                            List<Clause> rules = pairListEntry.getValue();
                            Collections.shuffle(rules);
                            selected.addAll(rules.subList(0, Math.min(maxRulesPerPredicate, rules.size())));
                        }


                        for (Pair<Double, Clause> pair : lessThanZeroTrainingError) {
                            Double currentError = 1.0 - pair.getR();
                            if (maxError - currentError < 0.0) {
                                break;
                            }
                            maxError -= currentError;
                            selected.add(pair.getS());
                        }

                        List<String> all = Sugar.list("hard rules");
                        all.addAll(constraints.stream().map(Object::toString).collect(Collectors.toList()));
                        all.add("implications");
                        all.addAll(selected.stream().map(Object::toString).collect(Collectors.toList()));
                        Files.write(output, all);
                    }
                }
            }
        }
    }

    private static void aggregateRules() throws IOException {
        Utils u = Utils.create();
        Set<Clause> hardRules = Sugar.set();
        Set<Clause> implications = Sugar.set();

        Set<Clause> constraints = u.loadClauses(Paths.get("..", "datasets", "kinships", "constraints.txt"));

        for (int k = 2; k <= 5; k++) {
            Path theoryFile = Paths.get(".", "pac", "kinships", "horn1000maxErr0.0K" + k + "Gorig.theory");
            FOL theory = FOL.create(theoryFile);
            hardRules.addAll(theory.getHardRules());
            implications.addAll(Sugar.setDifference(theory.allRules(), theory.getHardRules()));

            Path insertConstraiants = Paths.get(".", "pac", "kinships", "horn1000maxErr0.0K" + k + "GorigConstraints.theory");
            FOL constraitned = FOL.create(theory.allRules(), constraints);
            Files.write(insertConstraiants, Sugar.list(constraitned.toString()));
        }

        FOL merged = FOL.create(hardRules, implications);
        Files.write(Paths.get(".", "pac", "kinships", "horn1000.aggergatedKG.train.db.approxAcc"), Sugar.list(merged.toString()));
        FOL mergedWithConstraints = FOL.create(Sugar.union(hardRules, constraints), implications);
        Files.write(Paths.get(".", "pac", "kinships", "horn1000.aggergatedKGConstraints.train.db.approxAcc"), Sugar.list(mergedWithConstraints.toString()));
    }

    private static void selectRulesYago() throws IOException {
//        double maxError = 0.01;
//        String domain = "yago-states";
//        int k = 2;
        Utils u = Utils.create();
//            Path constraintsPath = Paths.get("..", "datasets", "mlns", "constraints.txt");
//            List<Clause> constarints = Sugar.listFromCollections(u.loadClauses(constraintsPath));

        boolean groundOnly = true;
        Matching matching = Matching.create(new Clause(LogicUtils.loadEvidence(Paths.get("..", "datasets", "kinships", "train.db"))), Matching.THETA_SUBSUMPTION);

        for (Integer k : Sugar.list(2, 3, 4, 5)) {

            for (Double maxErr : Sugar.list(0.0)) {
                double maxError = maxErr;

                List<Clause> selected = Sugar.list();
                Path output = Paths.get(".", "pac", "kinships", "maxErr" + maxError + "K" + k + (groundOnly ? "G" : "") + "orig.theory");

                if (!output.toFile().exists() || true) {

                    Path train = Paths.get(".", "pac", "kinships", "horn1000." + k + ".train.db.approxAcc");
                    List<Pair<Double, Clause>> pairs = Files.lines(train).filter(line -> line.trim().length() > 0 && !line.trim().startsWith("#"))
                            .map(line -> {

                                String[] splitted = line.split("\t", 2);
                                Clause clause = Clause.parse(splitted[1]);

                                if (groundOnly) {
                                    HornClause horn = HornClause.create(clause);
                                    /*if (!matching.subsumption(horn.body(), 0)) { // no grounding of the body exists
                                        return null;
                                    }*/
                                    Pair<Term[], List<Term[]>> substitution = matching.allSubstitutions(horn.body(), 0, Integer.MAX_VALUE);
                                    boolean atLeastOneKSizeGround = substitution.s.stream().anyMatch(arr -> Arrays.stream(arr).distinct().count() <= k);
                                    if (!atLeastOneKSizeGround) {
                                        return null;
                                    }
                                }

                                return new Pair<>(Double.parseDouble(splitted[0]), clause);
                            }).filter(Objects::nonNull).sorted(Comparator.comparing(Pair<Double, Clause>::getR).reversed()).collect(Collectors.toList());

                    for (Pair<Double, Clause> pair : pairs) {
                        Double currentError = 1.0 - pair.getR();
                        if (maxError - currentError < 0.0) {
                            break;
                        }
                        maxError -= currentError;
                        selected.add(pair.getS());
                    }

                    List<String> all = Sugar.list("hard rules");
//                    all.addAll(constarints.stream().map(Object::toString).collect(Collectors.toList()));
                    all.add("implications");
                    all.addAll(selected.stream().map(Object::toString).collect(Collectors.toList()));
                    Files.write(output, all);
                }
            }
        }

    }


    private static void yagoDataset() {
        Path data = Paths.get("C:\\data\\school\\development\\amie\\yago2\\coreFacts.logic");
        Utils u = Utils.create();
        Set<Literal> evidence = LogicUtils.loadEvidence(data);
        Set<Predicate> predicates = u.predicates(evidence);
        Set<Constant> constants = LogicUtils.constants(evidence);

        System.out.println("sizes");
        System.out.println("evidence\t" + evidence.size());
        System.out.println("constants\t" + constants.size());
        System.out.println("predicates\t" + predicates.size());

        //predicates.stream().map(Predicate::toString).sorted().forEach(System.out::println);
        predicates.forEach(p -> {
            long occurrences = evidence.stream().filter(l -> l.predicate().equals(p.getName())).count();
            System.out.println(p + "\t" + (occurrences * 1.0 / evidence.size()));
        });
    }


    private static void scoreRulesYago() throws IOException {
//        Path train = Paths.get(".", "pac", "yago2", "train.db");
//        Path rules = Paths.get(".", "pac", "yago2", "amieAll.theory");

//        String domain = "mlns\\kinships";
//        String domain = "yago-states";
//        String domain = "protein-queries";

        for (String domain : Sugar.list("yago-states", "yago-film", "yago-people", "yago-musician", "yago-politics")) {


            //Path train = Paths.get("..", "datasets", domain, "train.db");
            Path train = Paths.get("..", "datasets", domain, "train.db.4000.uniformLit.subampled");
//        Path rules = Paths.get(".", "pac", "protein", "theory");
            Path rules = Paths.get(".", "pac", domain, "amieRuleDepth4.logic");


//        Path train = Paths.get("..", "datasets", "mlns", "kinships", "train.db");
//        Path rules = Paths.get(".", "pac", "kinships", "negativeSamples-1-1.theory");
//        Path rules = Paths.get(".", "pac", "kinships", "amie.theory");

            for (Integer k : Sugar.list(2, 3, 4)) {
                System.out.println("doing\t k" + k + "\t" + domain);

                Path output = Paths.get(".", "pac", domain, "amieRuleDepth4K" + k + "." + train.toFile().getName() + ".approxAcc");
                if (output.toFile().exists()) {
                    System.out.println("skipping, already done");
                    continue;
                }

                Utils u = Utils.create();
                Set<Clause> theory = u.loadClauses(rules);
                Set<Literal> evidence = LogicUtils.loadEvidence(train);
                PACAccuracyDataset dataset = PACAccuracyDataset.create(evidence, k);

                List<String> sorted = theory.stream()
                        .map(clause -> new Pair<>(dataset.accuracyApprox(clause), clause))
                        .sorted(Comparator.comparingDouble(Pair<Double, Clause>::getR).reversed())
                        .map(p -> p.r + "\t" + p.getS())
                        .collect(Collectors.toList());

                Files.write(output, sorted);
            }
        }

    }

    private static void scoreRules() throws IOException {
        Path train = Paths.get("..", "datasets", "mlns", "kinships", "train.db");
        Path rules = Paths.get(".", "pac", "kinships", "horn1000.theory");

        for (Integer k : Sugar.list(2, 3, 4, 5)) {
            System.out.println("doing\t k" + k);

            Path output = Paths.get(".", "pac", "kinships", "horn1000." + k + "." + train.toFile().getName() + ".approxAcc");
            if (output.toFile().exists()) {
                System.out.println("skipping, already done");
                continue;
            }

            Utils u = Utils.create();
            Set<Clause> theory = u.loadClauses(rules);
            Set<Literal> evidence = LogicUtils.loadEvidence(train);
            PACAccuracyDataset dataset = PACAccuracyDataset.create(evidence, k);

            List<String> sorted = theory.stream()
                    .map(clause -> new Pair<>(dataset.accuracyApprox(clause), clause))
                    .sorted(Comparator.comparingDouble(Pair<Double, Clause>::getR).reversed())
                    .map(p -> p.r + "\t" + p.getS())
                    .collect(Collectors.toList());

            Files.write(output, sorted);
        }
    }

    private static void plotFPFNwrtRatios() {
        Utils u = Utils.create();

        Path gPath = Paths.get("..", "datasets", "protein-queries", "test.db");
        Pair<Path, Integer> dataK4 = new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 4);
        Predicate predicateOfInterest = Predicate.construct("location/2");
        Data<Integer, VECollector> predicted = Data.loadResults(dataK4);
        Set<Literal> groundTruth = LogicUtils.loadEvidence(gPath);
        Pair<Data<Double, Integer>, Data<Double, Integer>> fpfn = u.fpFnHistogram(groundTruth, predicted, predicateOfInterest);
        System.out.println(u.plot(Sugar.list(fpfn.getR()), ("fp " + predicateOfInterest).replace("/", "-")));
        System.out.println(u.plot(Sugar.list(fpfn.getS()), ("fn " + predicateOfInterest)).replace("/", "-"));

    }

    private static void clearSampledData() throws IOException {
        Path trainData = Paths.get("..", "datasets", "protein-queries", "train-snowball5");
        Files.list(trainData).forEach(dir -> {
            try {
                List<Path> files = Files.list(dir)
                        .filter(file -> !file.toFile().getName().equals("0.db"))
                        .collect(Collectors.toList());


                System.out.println("these will be deleted");
                files.forEach(System.out::println);
                /*files.forEach(f -> {
                    try {
                        Files.delete(f);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });*/


            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    private static void checkVE() throws IOException {
        int k = 5;
        Path old = Paths.get(".", "pac", "protein", "t-protein.poss_k-" + k + "_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym_ms-f");
        Path megaScout = Paths.get(".", "pac", "protein", "t-protein.poss_k-" + k + "_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Utils u = Utils.create();
//        u.compareVE(old,megaScout,k);
        int interest = 1991;
        u.compareVE(old, megaScout, k, interest, interest);
    }


    private static void plotFPFNByDifferentK() {
        /**/
        Path groundTruth = Paths.get("..", "datasets", "protein", "test.db");
        Pair<Path, Integer> k4 = new Pair<>(Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 4);
        Pair<Path, Integer> k5 = new Pair<>(Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 5);
        Pair<Path, Integer> k6 = new Pair<>(Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 6);
        Pair<Path, Integer> k7 = new Pair<>(Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-7_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 7);
        //List<Data<Integer, VECollector>> data = Sugar.list(Data.loadResults(k4), Data.loadResults(k5), Data.loadResults(k6), Data.loadResults(k7));
        /**/

        Path k4errn10 = Paths.get(".", "pac", "protein", "src-queries_t-merpp10n10000m10G.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path k4errn100 = Paths.get(".", "pac", "protein", "src-queries_t-merpp100n10000m10G.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
//        paths.add(new Pair<>(new Pair<>(k4errn10,4),Sugar.list(KCut.K_CUT)));
//        paths.add(new Pair<>(new Pair<>(k4errn100,4),Sugar.list(KCut.K_CUT)));
        List<Data<Integer, VECollector>> data = Sugar.list(Data.loadResults(k4), Data.loadResults(k4errn10, 4), Data.loadResults(k4errn100, 4));

        /** /
         Path groundTruth = Paths.get("..", "datasets", "uwcs-queries", "test.db");
         Pair<Path, Integer> k3 = new Pair<>(Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t"), 3);
         Pair<Path, Integer> k4 = new Pair<>(Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t"), 4);
         Pair<Path, Integer> k5 = new Pair<>(Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-5_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t"), 5);
         List<data<Integer, VECollector>> data = Sugar.list(data.loadResults(k3), data.loadResults(k4), data.loadResults(k5));
         /**/

        boolean cumulative = true;
        Utils u = Utils.create();
        System.out.println(replacement(u.plotFPFN(groundTruth, data, cumulative)));
    }

    private static void plotFPFN() {
        /* */
        Path groundTruth = Paths.get("..", "datasets", "protein-queries", "test.db");
        //Pair<Path, Integer> dataK = new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 5);
        Pair<Path, Integer> dataK5 = new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 5);
        //        ConstantGammaCut snowballEmpiric = ConstantGammaCut.load(Paths.get(".", "pac", "protein", "snowball_arcg_empiric.cut"), 5);
        //        ConstantGammaCut randomEmpiric = ConstantGammaCut.load(Paths.get(".", "pac", "protein", "random_arcg_empiric.cut"), 5);

//         List<Cut> cuts = Sugar.list(KCut.K_CUT
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-cg.cut"), 4)
        //, ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-1.0-cg.cut"), 4)
//         , GammasCut.ALL_BANNED
//         , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-complex_2-1.0_enzyme_2-1.0_function_2-1.0_interaction_2-1.0_location_2-1.0E-6_phenotype_2-1.0_protein_class_2-1.0-cg.cut"), 4)
//         , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-complex_2-1.0_enzyme_2-1.0_function_2-1.0_interaction_2-1.0_location_2-1.0E-5_phenotype_2-1.0_protein_class_2-1.0-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", ""), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", ""), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", ""), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-0.01-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-0.001-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-1.0E-4-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-1.0E-5-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-1.0E-6-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-1.0E-7-cg.cut"), 4)

        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-1.005985614405714E-4-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-1.25E-5-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-4.619321698801748E-6-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-3.086300682844026E-6-cg.cut"), 4)
        //                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-location_2-0.001189767995240928-cg.cut"), 4)
//         , BinCut.load(Paths.get(dataK4.getR().toString(), "bin-gamma.cut")));


        List<Cut> cuts = Sugar.list();
        int k = 5;
        cuts.add(KCut.K_CUT);
//        cuts.add(ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5" + "-k" + k + "-ratio-" + "1.0" + "-cg.cut"), k));
//        cuts.add(ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5" + "-k" + k + "-ratio-" + "1.0E-5" + "-cg.cut"), k));
        cuts.add(ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-randomWalk" + "-k" + k + "-ratio-" + "1.0" + "-cg.cut"), k));
        cuts.add(ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-randomWalk" + "-k" + k + "-ratio-" + "1.0E-5" + "-cg.cut"), k));

        Utils u = Utils.create();
        boolean cumulative = true;
        System.out.println(replacement(u.plotFPFN(groundTruth, dataK5, cuts, cumulative)));

        /**/

        /* * /
         Path groundTruth = Paths.get("..", "datasets", "uwcs-queries", "test.db");
         int k = 4;
         Pair<Path, Integer> data = new Pair<>(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-" + k + "_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t"), k);

         List<Cut> cuts = Sugar.list(KCut.K_CUT
         , ConstantGammaCut.load(Paths.get(".", "pac", "uwcs", "train-snowball5-k" + k + "-cg.cut"), k)
         , BinCut.load(Paths.get(data.getR().toString(), "bin-gamma.cut")));
         Utils u = Utils.get();
         boolean cumulative = true;
         System.out.println(replacement(u.plotFPFN(groundTruth, data, cuts, cumulative)));
         /**/

        /* * /
        Path groundTruth = Paths.get(".", "pac", "yago2", "test.db");
        Path amieK2 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amieK20 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-20_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amieAllK2 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-amieAll.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path nsK2 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-1.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amie = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path plain = Paths.get(".", "pac", "yago2", "test-uniform300");


        Utils u = Utils.get();
        boolean cumulative = true;
        System.out.println(replacement(u.plotFPFN(groundTruth, Sugar.list(
                Data.loadResults(amie, 0)
                , Data.loadResults(amieAllK2, 2)
                , Data.loadResults(plain, 0)
//                , Data.loadResults(amieK20, 2)
        ), cumulative)));
        /**/

        /* * /
        Path groundTruth = Paths.get("..", "datasets", "mlns", "kinships", "test.db");
        Path amieK3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-amie.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t-prvnich500");
        Path nsK4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-negativeSamples-1-1.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        /**/

//        String domain = "yago-politics";
//        String domain = "yago-states";
//        String domain = "yago-film";
        /*String domain = "yago-people";
//        String domain = "yago-musician";
        Path groundTruth = Paths.get("..", "datasets", domain, "test.db");
        Path dataA = Paths.get(".", "pac", domain, "src-test-uniform_t-maxErr1.0E-4K4orig4000.theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k2 = Paths.get(".", "pac", domain, "src-test-uniform_t-maxErr1.0E-4K2orig4000.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k3 = Paths.get(".", "pac", domain, "src-test-uniform_t-maxErr1.0E-4K3orig4000.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k4 = Paths.get(".", "pac", domain, "src-test-uniform_t-maxErr1.0E-4K4orig4000.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");


        Utils u = Utils.get();
        boolean cumulative = true;
        System.out.println(replacement(u.plotFPFN(groundTruth, Sugar.list(
                Data.loadResults(dataA, 0).sublist(0,500,"")
                , Data.loadResults(k2, 2)
                , Data.loadResults(k3, 3)
                , Data.loadResults(k4, 4)
        ), cumulative)));
        */

        /* * /
        Path groundTruth = Paths.get("..", "datasets", "kinships", "test.db");
        Path dataA = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000.aggergatedKG.train.db.approxAcc_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k2 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K2Gorig.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K3Gorig.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K4Gorig.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k5 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K5Gorig.theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");


        Utils u = Utils.get();
        boolean cumulative = true;
        System.out.println(replacement(u.plotFPFN(groundTruth, Sugar.list(
                Data.loadResults(dataA, 0)
                , Data.loadResults(k2, 2)
                , Data.loadResults(k3, 3)
                , Data.loadResults(k4, 4)
                , Data.loadResults(k5, 5)
        ), cumulative)));
        /**/

    }


    private static void plotRatiosHistogram() throws IOException {

        String domain = "protein";
        int k = 4;
        Path trainData = Paths.get("..", "datasets", domain + "-queries", "train-snowball5");
        Predicate predicateOfInterest = Predicate.create("location", 2);

        Stream<Pair<Double, Long>> plotData = Files.list(trainData)
                .parallel()
                .filter(path -> path.toFile().isDirectory())
                .map(path -> Paths.get(path.toString(), "0.db.partialResult." + k))
                .map(p -> {
                    VECollector data = VECollector.load(p, k);
                    Map<Double, Long> histo = Sugar.parallelStream(data.getVotes().entrySet())
                            .filter(e -> e.getKey().getPredicate().equals(predicateOfInterest.getPair()))
                            .map(e -> new Pair<>(e.getKey()
                                    , e.getValue() / data.getCToX(e.getKey().terms().size(), k)))
                            .collect(Collectors.groupingBy(Pair::getS, Collectors.counting()));
                    return histo; // histogram ratio - # occurrences
                }).flatMap(map -> map.entrySet().stream())
                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.counting()))
                .entrySet().stream().parallel()
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .map(e -> new Pair<>(e.getKey(), e.getValue()));

        Data<Double, Long> plot = new Data<>(plotData, "points");
        System.out.println(Utils.create().plot(Sugar.list(plot), "histogram of occurrences of ratios in " + trainData));
    }

    private static void learnConstRatioGammas() throws IOException {
        //String domain = true ? "protein" : "uwcs";
//        String domain = "uwcs";
        int mode = StatefullConstantGammaLearner.EMPIRIC;
//        int mode = StatefullConstantGammaLearner.PLAIN;
        Theory theory = null;
        EntailmentSetting setting = null;

        for (String domain : Sugar.list("uwcs", "protein", "kinships", "umls", "nations")) {

//        if ("uwcs".equals(domain)) {
//            System.setProperty("ida.pacReasoning.entailment.logic", "possibilistic");
//        } else {
            System.setProperty("ida.pacReasoning.entailment.logic", "classical");
//        }

            if (mode == StatefullConstantGammaLearner.EMPIRIC) {
                System.setProperty("ida.pacReasoning.entailment.mode", "v");
                System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
                System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
                System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
                System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
                System.setProperty("ida.pacReasoning.entailment.megaScout", "true");

                for (String merpp : Sugar.list("100", "10", "5", "0")) {

                    //Path theorySource = Paths.get(".", "pac", domain, domain + ".poss");
//            Path theorySource = Paths.get(".", "pac", domain, "amie.theory");
                    Path theorySource = Paths.get(".", "pac", domain, "merpp" + merpp + "n10000m10G.theory");
                    if (!theorySource.toFile().exists()) {
                        continue;
                    }
                    theory = System.getProperty("ida.pacReasoning.entailment.logic").equals("possibilistic")
                            ? Possibilistic.create(theorySource)
                            : FOL.create(theorySource);


                    //List<Double> ratios = Sugar.list(1.0, 0.1, 0.05, 0.01, 0.001, 0.00001);
                    List<Double> ratios = Sugar.list(1.0, 0.01, 0.00001);
                    List<String> trainFolders = Sugar.list("train-snowball5",
                            "train-randomWalk");

                    for (Integer k : Sugar.list(6, 5, 4, 3, 2)) {
                        //for (Integer k : Sugar.list(4, 5,6)) {
//        for (Integer k : Sugar.list(4, 5, 6, 7)) {
//            for (String theorySrc : Sugar.list("amie.theory", "negativeSamples-1-1.theory", "negativeSamplesLong.theory")) { // , "negativeSamples-1-2.theory"
//                Path theorySource = Paths.get(".", "pac", domain, theorySrc);
//                theory = System.getProperty("ida.pacReasoning.entailment.logic").equals("possibilistic")
//                        ? Possibilistic.get(theorySource)
//                        : FOL.get(theorySource);
                        {

                            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
                            setting = EntailmentSetting.create();
                            StatefullConstantGammaLearner learner = StatefullConstantGammaLearner.create(setting);

                            for (String trainFolder : trainFolders) {
                                //Path trainData = Paths.get("..", "datasets", domain + "-queries", trainFolder); // uwcs || protein
                                Path trainData = Paths.get("..", "datasets", domain, trainFolder);

                                for (Double ratio : ratios) {

                                    ConstantGammaCut model = learner.learn(trainData, k, theory
                                            , ratio
//                , (path) -> path.toFile().getName().equals("829") // debug toto
                                    );

//                    String canonicRatios = Utils.get().canon(ratios.entrySet().stream(), (entry) -> entry.getKey().toString().replace("/", "_") + "-" + entry
//                            .getValue(), "_");

                                    Files.write(Paths.get(".", "pac", domain, "t-" + theory.getName() + "-" + trainFolder + "-k" + k + "-ratio-" + ratio + "-cg.cut"), Sugar.list(model.asOutput()));
                                    System.out.println("learned and stored\t" + Paths.get(".", "pac", domain, "t-" + theory.getName() + "-" + trainFolder + "-k" + k + "-ratio-" + ratio + "-cg.cut"));


                                }

                            }
                        }
                    }
                }
            }
        }
    }

    private static String replacement(String s) {
        return s
                // uwcs
                .replace(".\\textbackslash{}pac\\textbackslash{}uwcs\\textbackslash{}", "")
                .replace("..\\textbackslash{}datasets\\textbackslash{}uwcs-queries\\textbackslash{}", "")
                .replace("tr-train:t-uwcs.poss:k-3:em-V:l-PL:is-t:dd-t:cpr-f:scout-t:sat-full\\textbackslash{}", "k3-train ")
                .replace("t-uwcs.poss:k-3:em-V:l-PL:is-t:dd-t:cpr-f:scout-t:sat-full\\textbackslash{}", "k3-test ")
                .replace("t-uwcs.poss:k-3:em-V:l-PL:is-t:dd-t:cpr-f:scout-t:sat-full\\textbackslash{}", "test k3 PL ")
                .replace("mlns k-cut", "mlns ")
                // protein
                .replace(".\\textbackslash{}pac\\textbackslash{}protein\\textbackslash{}tr-train:t-protein.poss:k-5:em-V:l-c:is-t:dd-t:cpr-f:scout-t:sat-sym\\textbackslash{}", " k5 train")

                .replace("..\\textbackslash{}datasets\\textbackslash{}protein-queries\\textbackslash{}", "")
                // snimatelne
                .replace("tr-train_t-uwcs.poss:k-3:em-V:l-PL:is-t:dd-t:cpr-f:scout-t:sat-full", "train k3 ")
                .replace("t-uwcs.poss:k-3:em-V:l-PL:is-t:dd-t:cpr-f:scout-t:sat-full", "k3 ")
                .replace("t-uwcs.poss:k-0:em-A:l-PL:is-t:dd-t:cpr-f:scout-t:sat-full", "PL allevidence")

                //protein sampled
                .replace("-cg.cut", "")
                .replace(".\\textbackslash{}pac\\textbackslash{}protein\\textbackslash{}train-", "")
                .replace("src-test-randomWalk:t-protein.poss:k-4:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "rw k4")
                .replace("src-test-randomWalk:t-protein.poss:k-5:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "rw k5")
                .replace("src-test-randomWalk:t-protein.poss:k-6:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "rw k6")
                .replace("src-test-randomWalk:t-protein.poss:k-7:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "rw k7")

                .replace("src-test-snowball5:t-protein.poss:k-4:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "sb k4")
                .replace("src-test-snowball5:t-protein.poss:k-5:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "sb k5")
                .replace("src-test-snowball5:t-protein.poss:k-6:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "sb k6")
                .replace("src-test-snowball5:t-protein.poss:k-7:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "sb k7")

                .replace("src-queries:t-protein.poss:k-4:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "mlnsQueries k4")
                .replace("src-queries:t-protein.poss:k-5:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "mlnsQueries k5")
                .replace("src-queries:t-protein.poss:k-6:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "mlnsQueries k6")
                .replace("src-queries:t-protein.poss:k-7:em-V:l-c:is-t:dd-t:cpr-f:scout-f:sat-sym:ms-t", "mlnsQueries k7")

                ;
    }

    private static void plotResults() {
        boolean maskGT = true;

        // proteins k4, k5 + ve all fitted and bin500 fitted
        // protein data
        /* * /
        Path groundTruth = Paths.get("..", "datasets", "protein-queries", "test.db");
        Path pl = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path dataK4 = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path dataK5 = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path dataK6 = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path dataK7 = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-7_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");


        ConstantGammaCut empriciSnowball5 = ConstantGammaCut.load(Paths.get(".", "pac", "protein", "snowball_arcg_empiric.cut"), 5);
        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<Pair<Path, Integer>, List<Cut>>(
                        new Pair<>(Paths.get(".", "pac", "protein", "mlns"), 0), Sugar.list(KCut.K_CUT)
                )
                , new Pair<>(new Pair<>(pl, 0), Sugar.list(KCut.K_CUT))
                //                                , new Pair<Pair<Path, Integer>, List<Cut>>(
                //                        new Pair<>(Paths.get(".", "pac", "protein", "yeast_poss_predictions-2000.txt"), 0)
                //                        , Sugar.list(KCut.K_CUT)) %, new Pair<Pair<Path, Integer>, List<Cut>>(
                //                        new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-sym"), 5)
                //                        , Sugar.list(KCut.K_CUT))
                , new Pair<>(
                        new Pair<>(dataK4, 4),
                        Sugar.list(KCut.K_CUT
//                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-complex_2-1.0_enzyme_2-1.0_function_2-1.0_interaction_2-1.0_location_2-1.0E-6_phenotype_2-1.0_protein_class_2-1.0-cg.cut"), 4)
//                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-complex_2-1.0_enzyme_2-1.0_function_2-1.0_interaction_2-1.0_location_2-1.0E-5_phenotype_2-1.0_protein_class_2-1.0-cg.cut"), 4)
//                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-complex_2-1.0_enzyme_2-1.0_function_2-1.0_interaction_2-1.0_location_2-1.0E-4_phenotype_2-1.0_protein_class_2-1.0-cg.cut"), 4)
//                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "train-snowball5-k4-complex_2-1.0_enzyme_2-1.0_function_2-1.0_interaction_2-1.0_location_2-0.1_phenotype_2-1.0_protein_class_2-1.0-cg.cut"), 4)
                                //                                , GammasCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"))
                                //                                , BinCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin.cut"))
                                //                                , GammasCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"))
                                //                                                                , BinCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin.cut"))
                        )
                )
                , new Pair<>(
                        new Pair<>(dataK5, 5),
                        Sugar.list(KCut.K_CUT
                                //                                , GammasCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"))

                                //
                                //                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "random_arcg_empiric.cut"), 5)
                                //
                                //                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "random_arcg_empiric.cut"), 5)
                                //                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "random_arcg_plain.cut"), 5)

                                //                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "snowball_arcg_empiric.cut"), 5)
                                //                                        .scale(1000)
                                //                                ,empriciSnowball5.scale(0.9)
                                //                                ,empriciSnowball5.scale(0.8)
                                //                                ,empriciSnowball5.scale(0.7)
                                //                                ,empriciSnowball5.scale(0.6)
                                //                                ,empriciSnowball5.scale(0.5)
                                //                                ,empriciSnowball5.scale(0.4)
                                //                                ,empriciSnowball5.scale(0.3)
                                //                                , ConstantGammaCut.load(Paths.get(".", "pac", "protein", "snowball_arcg_plain.cut"), 5)
                                //                                        .scale(1000)
                                //           , GammasCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gamma.cut"))
                                //                                , BinCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin-gamma.cut"))
                                //                                , ConstantRatioCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "cnstr.cut"))
                                //                                , GammasCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gamma.cut"))
                                //                                , BinCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin-gamma.cut"))
                                //                                , ConstantRatioCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "cnstr.cut"))

                                //                                , BinCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "binFixBorders.cut"))
                                //                                , BinCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "mGTbinFixBorders.cut"))
                                //                                , GammasCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "mGTgammas.cut"))
                                //                                , BinCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "mGTbin.cut"))
                                //                                , GammasCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "mGTgammas.cut"))
                                //                                , BinCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "mGTbin.cut"))
                        )
                )
                , new Pair<>(new Pair<>(dataK6, 6), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(dataK7, 7), Sugar.list(KCut.K_CUT))
                //                , new Pair<>(
                //                        new Pair<>(dataK6, 6),
                //                        Sugar.list(KCut.K_CUT
                //                                , GammasCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"))
                //                                , BinCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin.cut"))
                //                                , GammasCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"))
                //                                , BinCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin.cut"))
                //                        )
                //                )
        );/**/

        /* * /
         Path groundTruth = Paths.get("..", "datasets", "uwcs-queries", "test.db");
         //         Path possK3 = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full");
         //         Path possK4 = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full");
         //         Path k3 = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-full");
         //         Path k4 = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-full");
         Path k3 = Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
         Path k4 = Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
         Path k5 = Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-5_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
         Path pl = Paths.get(".", "pac", "uwcs", "src-queries_t-uwcs.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");

         Path oldPl = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full");

         List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
         new Pair<Pair<Path, Integer>, List<Cut>>(
         new Pair<>(Paths.get(".", "pac", "uwcs", "mlns"), 0)
         , Sugar.list(KCut.K_CUT))
         , new Pair<>(new Pair<>(pl, 0), Sugar.list(KCut.K_CUT))
         //                , new Pair<>(new Pair<>(oldPl, 0), Sugar.list(KCut.K_CUT))
         , new Pair<>(new Pair<>(k3, 3), Sugar.list(KCut.K_CUT))
         , new Pair<>(new Pair<>(k4, 4), Sugar.list(KCut.K_CUT))
         , new Pair<>(new Pair<>(k5, 5), Sugar.list(KCut.K_CUT))

         //                , new Pair<>(new Pair<>(k3, 3), Sugar.list(KCut.K_CUT))
         //                , new Pair<>(new Pair<>(k4, 4), Sugar.list(KCut.K_CUT))
         //                , new Pair<>(
         //                        new Pair<>(possK3, 3),
         //                        Sugar.list(KCut.K_CUT
         //                                , GammasCut.load(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "gamma.cut"))
         //                                , BinCut.load(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "bin-gamma.cut"))
         //                                , ConstantRatioCut.load(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "cnstr.cut"))
         //                                , GammasCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "gamma.cut"))
         //                                , BinCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "bin-gamma.cut"))
         //                                , ConstantRatioCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "cnstr.cut"))
         //
         //                        )
         //                )
         //                , new Pair<>(
         //                        new Pair<>(possK4, 4),
         //                        Sugar.list(KCut.K_CUT
         //                                , GammasCut.load(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "gammas.cut"))
         //                                , BinCut.load(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "bin.cut"))
         //                                , GammasCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "gammas.cut"))
         //                                , BinCut.load(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "bin.cut"))
         //                        )
         //                )
         );/**/

        /*List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<>(
                        new Pair<>(Paths.get(".", "pac", "protein", "mlns"), 0)
                        , Sugar.list(KCut.K_CUT))
                , new Pair<>(
                        new Pair<>(Paths.get(".", "pac", "protein", "yeast_poss_predictions-2000.txt"), 0)
                        , Sugar.list(KCut.K_CUT))
                , new Pair<>(
                        new Pair<>(dataK4, 4), Sugar.list(KCut.K_CUT
                        , GammasCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"))
                        , BinCut.load(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin.cut"))
                        , GammasCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"))
                        , BinCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin.cut"))
                )
//                        , GammasCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"))
//                        , BinCut.load(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "bin.cut"))
                )

//                new Pair<>(
//                        new Pair<>(dataK4, 4),
//                        Sugar.list(KCut.K_CUT
//                                , GammasCut.load(Double.parseDouble("3.5366681756450882E-6"),4))
//        )
        );*/

        /* */
        Path groundTruth = Paths.get(".", "pac", "yago2", "test.db");
        Path plain = Paths.get(".", "pac", "yago2", "test-uniform300");
        Path amieK2 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amieK3 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amieK4 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amieK5 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amieK20 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-20_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amie = Paths.get(".", "pac", "yago2", "src-test-uniform_t-theory.txt_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");

        Path amieAllK2 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-amieAll.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path amieAllK15 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-amieAll.theory_k-15_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");


        Path nsK2 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-1.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path nsK3 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-1.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path nsK4 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-1.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path nsK5 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-1.theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path ns = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-1.theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");

        Path ns2K2 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-2.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path ns2K3 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-2.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path ns2K4 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-2.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path ns2 = Paths.get(".", "pac", "yago2", "src-test-uniform_t-negativeSamples-100-10-1-2.theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");


        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<>(new Pair<>(plain, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(amie, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(amieK2, 2), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(amieK20, 20), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(amieAllK2, 2), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(amieAllK15, 15), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(amieK3, 3), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(amieK4, 4), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(amieK5, 5), Sugar.list(KCut.K_CUT))
//        ,new Pair<>(new Pair<>(ns, 0), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(nsK2, 2), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(nsK3, 3), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(nsK4, 4), Sugar.list(KCut.K_CUT))
//                , new Pair<>(new Pair<>(nsK5, 5), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(ns2, 0), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(ns2K2, 2), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(ns2K3, 3), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(ns2K4, 4), Sugar.list(KCut.K_CUT))
        );/**/

        /* * / //  amie vs neg samples on kinships
        String theory = "amie.theory"; // "negativeSamples.theory" "amie.theory"
        Path groundTruth = Paths.get("..", "datasets", "mlns", "kinships", "test.db");
        Path k3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-" + theory + "_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path k4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-" + theory + "_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path k5 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-" + theory + "_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path fol = Paths.get(".", "pac", "kinships", "src-test-uniform_t-" + theory + "_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");

        theory = "negativeSamples.theory";
        Path folNS = Paths.get(".", "pac", "kinships", "src-test-uniform_t-" + theory + "_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path k3NS = Paths.get(".", "pac", "kinships", "src-test-uniform_t-" + theory + "_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path k4NS = Paths.get(".", "pac", "kinships", "src-test-uniform_t-" + theory + "_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");

        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<>(new Pair<>(fol, 0), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k3, 3), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k4, 4), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k5, 5), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(folNS, 0), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k3NS, 3), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k4NS, 4), Sugar.list(KCut.K_CUT))
        );/**/


        Utils utils = Utils.create();
        System.out.println(replacement(utils.plotCHEDwithGroupCut(groundTruth, paths, maskGT)));
    }

    private static void plotResultsSampled() {
        boolean maskGT = true;

        // proteins k4, k5 + ve all fitted and bin500 fitted
        // protein data
        /* */
        String data = "src-test-randomWalk_";
//        String data = "src-test-snowball5_";
//        String data = "src-queries_";
        System.out.println("tohle je testovano na random walk a gamy jsou trenovane snowball5... nemam pro to zatim mlns vystup");
        Path groundTruth = Paths.get("..", "datasets", "protein", "test.db");
        Path dataK4 = Paths.get(".", "pac", "protein", data + "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path dataK5 = Paths.get(".", "pac", "protein", data + "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path dataK6 = Paths.get(".", "pac", "protein", data + "t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path dataK7 = Paths.get(".", "pac", "protein", data + "t-protein.poss_k-7_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");

        //String cutStart = "train-randomWalk";
        String cutStart = "train-snowball5";
        String cutStart2 = "train-randomWalk";

        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list();

        if (data.contains("queries")) {
            paths.add(new Pair<>(new Pair<>(Paths.get(".", "pac", "protein", "mlns"), 0), Sugar.list(KCut.K_CUT)));
        }

        IntStream
                //.range(4, 8)
                .range(5, 6)
                .forEach(k -> {
                    List<Cut> cuts = Sugar.list();
                    cuts.add(KCut.K_CUT);
                    cuts.addAll(Sugar.list("1.0", "0.1"//, "0.1", "0.05", "0.01", "0.001"
                                    , "1.0E-5").stream()
                            .map(ratio -> (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "protein", cutStart + "-k" + k + "-ratio-" + ratio + "-cg.cut"), k))
                            .collect(Collectors.toList()));
                    cuts.addAll(Sugar.list(
                                    "1.0", "0.1"//, "0.05", "0.01", "0.001"
                                    , "1.0E-5").stream()
                            .map(ratio -> (Cut) ConstantGammaCut.load(Paths.get(".", "pac", "protein", cutStart2 + "-k" + k + "-ratio-" + ratio + "-cg.cut"), k))
                            .collect(Collectors.toList()));
                    paths.add(new Pair<>(new Pair<>(Paths.get(".", "pac", "protein", data + "t-protein.poss_k-" + k + "_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), k)
                            , cuts));
                });
        /**/

        Path k4errn10 = Paths.get(".", "pac", "protein", "src-queries_t-merpp10n10000m10G.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        Path k4errn100 = Paths.get(".", "pac", "protein", "src-queries_t-merpp100n10000m10G.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t");
        paths.add(new Pair<>(new Pair<>(k4errn10, 4), Sugar.list(KCut.K_CUT)));
        paths.add(new Pair<>(new Pair<>(k4errn100, 4), Sugar.list(KCut.K_CUT)));

        /* * /
         System.out.println("tohle je testovano na random walk a gamy jsou trenovane snowball5... nemam pro to zatim mlns vystup");
         Path groundTruth = Paths.get("..", "datasets", "uwcs-queries", "test.db");
         Path dataK3 = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
         Path dataK4 = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
         Path dataK5 = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-5_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
         Path dataA = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-0_em-A_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");


         List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
         new Pair<>(new Pair<>(dataA, 0), Sugar.list(KCut.K_CUT))
         , new Pair<>(
         new Pair<>(dataK3, 3),
         Sugar.list(KCut.K_CUT
         , ConstantGammaCut.load(Paths.get(".", "pac", "uwcs", "train-snowball5-k3-cg.cut"), 3)
         , BinCut.load(Paths.get(dataK3.toString(), "bin-gamma.cut"))
         )
         )
         , new Pair<>(
         new Pair<>(dataK4, 4),
         Sugar.list(KCut.K_CUT
         , ConstantGammaCut.load(Paths.get(".", "pac", "uwcs", "train-snowball5-k4-cg.cut"), 4)
         , BinCut.load(Paths.get(dataK4.toString(), "bin-gamma.cut")))
         )
         , new Pair<>(
         new Pair<>(dataK5, 5),
         Sugar.list(KCut.K_CUT
         , ConstantGammaCut.load(Paths.get(".", "pac", "uwcs", "train-snowball5-k5-cg.cut"), 5)
         , BinCut.load(Paths.get(dataK5.toString(), "bin-gamma.cut")))
         )
         );
         /**/

        /* * /
        // kinships
        String data = "src-test-uniform_";
        Path groundTruth = Paths.get("..", "datasets", "mlns", "kinships", "test.db");
        Path amie = Paths.get(".", "pac", "kinships", data + "t-amie.theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t-prvnich500");
        Path amieK3 = Paths.get(".", "pac", "kinships", data + "t-amie.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path amieK4 = Paths.get(".", "pac", "kinships", data + "t-amie.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");

        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list();

        paths.add(new Pair<>(new Pair<>(amie, 0), Sugar.list(KCut.K_CUT)));
        paths.add(new Pair<>(new Pair<>(amieK3, 3), Sugar.list(KCut.K_CUT
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-amie.theory-train-randomWalk-k3-ratio-1.0-cg.cut"), 3)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-amie.theory-train-randomWalk-k3-ratio-0.1-cg.cut"), 3)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-amie.theory-train-randomWalk-k3-ratio-0.01-cg.cut"), 3)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-amie.theory-train-randomWalk-k3-ratio-1.0E-5-cg.cut"), 3)
        )));

        paths.add(new Pair<>(new Pair<>(amieK4, 4), Sugar.list(KCut.K_CUT
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-amie.theory-train-randomWalk-k4-ratio-1.0-cg.cut"), 4)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-amie.theory-train-randomWalk-k4-ratio-0.1-cg.cut"), 4)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-amie.theory-train-randomWalk-k4-ratio-0.01-cg.cut"), 4)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-amie.theory-train-randomWalk-k4-ratio-1.0E-5-cg.cut"), 4)
        )));

        Path fol = Paths.get(".", "pac", "kinships", data + "t-negativeSamples-1-1.theory_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path folK3 = Paths.get(".", "pac", "kinships", data + "t-negativeSamples-1-1.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");
        Path folK4 = Paths.get(".", "pac", "kinships", data + "t-negativeSamples-1-1.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t");

        // my theory
        paths.add(new Pair<>(new Pair<>(fol, 0), Sugar.list(KCut.K_CUT)));
        paths.add(new Pair<>(new Pair<>(folK3, 3), Sugar.list(KCut.K_CUT
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-negativeSamples-1-1.theory-train-randomWalk-k3-ratio-1.0-cg.cut"), 3)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-negativeSamples-1-1.theory-train-randomWalk-k3-ratio-0.1-cg.cut"), 3)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-negativeSamples-1-1.theory-train-randomWalk-k3-ratio-0.01-cg.cut"), 3)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-negativeSamples-1-1.theory-train-randomWalk-k3-ratio-1.0E-5-cg.cut"), 3)
        )));

        paths.add(new Pair<>(new Pair<>(folK4, 4), Sugar.list(KCut.K_CUT
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-negativeSamples-1-1.theory-train-randomWalk-k4-ratio-1.0-cg.cut"), 4)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-negativeSamples-1-1.theory-train-randomWalk-k4-ratio-0.1-cg.cut"), 4)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-negativeSamples-1-1.theory-train-randomWalk-k4-ratio-0.01-cg.cut"), 4)
                , ConstantGammaCut.load(Paths.get(".", "pac", "kinships", "t-negativeSamples-1-1.theory-train-randomWalk-k4-ratio-1.0E-5-cg.cut"), 4)
        )));
        /** /

        String domain = "yago-politics";
//        String domain = "yago-states";
//        String domain = "yago-film";
//        String domain = "yago-people";
//        String domain = "yago-musician";
        Path groundTruth = Paths.get("..", "datasets", domain, "test.db");
        String c = "";
        //String c = "Constraints";
        Path dataA = Paths.get(".", "pac", domain, "src-test-uniform_t-amieRuleDepth4" + c + ".logic_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k2 = Paths.get(".", "pac", domain, "src-test-uniform_t-amieRuleDepth4" + c + ".logic_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k3 = Paths.get(".", "pac", domain, "src-test-uniform_t-amieRuleDepth4" + c + ".logic_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k4 = Paths.get(".", "pac", domain, "src-test-uniform_t-amieRuleDepth4" + c + ".logic_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k5 = Paths.get(".", "pac", domain, "src-test-uniform_t-amieRuleDepth4" + c + ".logic_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");

        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<>(new Pair<>(dataA, 0), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k2, 2), Sugar.list(
                        KCut.K_CUT
                ))
                , new Pair<>(new Pair<>(k3, 3), Sugar.list(
                        KCut.K_CUT
                ))
                , new Pair<>(new Pair<>(k4, 4), Sugar.list(
                        KCut.K_CUT
                ))
                , new Pair<>(new Pair<>(k5, 5), Sugar.list(
                        KCut.K_CUT
                )));
        /**/

        /* * /
        Path groundTruth = Paths.get("..", "datasets", "kinships", "test.db");
//        Path dataA = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000.aggergatedKG.train.db.approxAcc_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k2 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K2Gorig.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K3Gorig.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K4Gorig.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
//        Path k5 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K5Gorig.theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");

        Path dataA = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000.aggergatedKGConstraints.train.db.approxAcc_k-0_em-A_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k2 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K2GorigConstraints.theory_k-2_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k3 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K3GorigConstraints.theory_k-3_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k4 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K4GorigConstraints.theory_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");
        Path k5 = Paths.get(".", "pac", "kinships", "src-test-uniform_t-horn1000maxErr0.0K5GorigConstraints.theory_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-none_ms-t");


        List<Pair<Pair<Path, Integer>, List<Cut>>> paths = Sugar.list(
                new Pair<>(new Pair<>(dataA, 0), Sugar.list(KCut.K_CUT))
                , new Pair<>(new Pair<>(k2, 2), Sugar.list(
                        KCut.K_CUT
                ))
                , new Pair<>(new Pair<>(k3, 3), Sugar.list(
                        KCut.K_CUT
                ))
                , new Pair<>(new Pair<>(k4, 4), Sugar.list(
                        KCut.K_CUT
                ))
                , new Pair<>(new Pair<>(k5, 5), Sugar.list(
                        KCut.K_CUT
                ))
        );
        /**/

        Utils utils = Utils.create();
        System.out.println(replacement(utils.plotCHEDwithGroupCut(groundTruth, paths, maskGT)));
    }


    private static void learnGammas() {
        GammaFitter fitter = GammaFitter.create(GammaFitter.GAMMAS); // optimalizuje na CHED
//        GammaFitter constantRatio = GammaFitter.get(GammaFitter.CONSTANT_RATIO);
//        GammaFitter max = GammaFitter.get(GammaFitter.RATIOS_MAX);
//        GammaFitter min = GammaFitter.get(GammaFitter.RATIOS_MIN);
//        GammaFitter avg = GammaFitter.get(GammaFitter.RATIOS_AVG);
//        GammaFitter med = GammaFitter.get(GammaFitter.RATIOS_MED);

//        GammasCut debug = (GammasCut) fitter.fit(Paths.get("..", "datasets", "uwcs-queries", "train.db"),
//                new Pair<>(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full"), 3));
//
//        System.exit(-3);

        /** /
         List<Triple<Path, List<Pair<Path, Integer>>, Integer>> process = Sugar.list(
         new Triple<>(Paths.get("..", "datasets", "protein-queries", "train.db")
         , Sugar.list(
         new Pair<>(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 4)
         , new Pair<>(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 5)
         //                        , new Pair<>(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 6)
         )
         , 500)
         , new Triple<>(Paths.get("..", "datasets", "protein-queries", "test.db")
         , Sugar.list(
         new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 4)
         , new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 5)
         //                        , new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 6)
         )
         , 500)
         );/**/
        /** /
         //List<Triple<Path, List<Pair<Path, Integer>>, Integer>> process =
         process.addAll(Sugar.list(
         new Triple<>(Paths.get("..", "datasets", "uwcs-queries", "train.db")
         , Sugar.list(new Pair<>(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full"), 3)
         //                        ,new Pair<>(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full"), 4)
         )
         , 100)
         , new Triple<>(Paths.get("..", "datasets", "uwcs-queries", "test.db")
         , Sugar.list(new Pair<>(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full"), 3)
         //                        ,new Pair<>(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full"), 4)
         )
         , 100)
         )
         );/**/


        // snowball train sampling learned; tested random walk
        /*List<Triple<Path, List<Pair<Path, Integer>>, Integer>> process = Sugar.list(
                new Triple<>(Paths.get("..", "datasets", "protein-queries", "test.db")
                        , Sugar.list(
                        new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 4)
                        , new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 5)
                        , new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t"), 6)
                )
                        , 500)
        );/**/

        /**/
        List<Triple<Path, List<Pair<Path, Integer>>, Integer>> process = Sugar.list(
                new Triple<>(Paths.get("..", "datasets", "uwcs-queries", "test.db")
                        , Sugar.list(
                        new Pair<>(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t"), 3)
                        , new Pair<>(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-4_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t"), 4)
                        , new Pair<>(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-5_em-V_l-PL_is-t_dd-t_cpr-f_scout-f_sat-full_ms-t"), 5)
                )
                        , 100)
        );
        /**/

        List<Pair<String, GammaFitter>> learners = Sugar.list(
                new Pair<>("gamma.cut", fitter)
//                , new Pair<>("cnstr.cut", constantRatio)
        );
        for (Triple<Path, List<Pair<Path, Integer>>, Integer> triple : process) {
            Path groundTruth = triple.r;
            int binSize = triple.t;
            triple.s.forEach(data -> {
                learners.forEach(pair -> {
                    String ending = pair.r;
                    GammaFitter learner = pair.s;

                    if (!data.r.toFile().exists()) {
                        System.out.println("skipping folder since it does not exists\t" + data.r);
                        return;
                    }
                    System.out.println("solving\t" + data);
                    Path binOutput = Paths.get(data.r.toString(), "bin-" + ending);
                    if (!binOutput.toFile().exists()) {
                        BinCut bin = learner.fit(groundTruth, data, binSize);
                        try {
                            Files.write(binOutput, Sugar.list(bin.asOutput()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("skipping bin cut learning since it already exists");
                    }

                    Path gammasOutput = Paths.get(data.r.toString(), ending);
                    if (!gammasOutput.toFile().exists()) {
                        Outputable all = (Outputable) learner.fit(groundTruth, data);
                        try {
                            Files.write(gammasOutput, Sugar.list(all.asOutput()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("skipping gammas cut learning since it already exists");
                    }
                });
            });
        }

        System.out.println("all cut learned and stored");
    }

    private static void fitGammas() {
        GammaFitter fitter = GammaFitter.create(GammaFitter.GAMMAS);
        Path groundTruth = Paths.get("..", "datasets", "protein-queries", "test.db");
        Path data = Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-f_cpr-f_scout-t_sat-sym");
        //Cut cut = fitter.fit(groundTruth,data);
        int binSize = 500;
        int k = 4;
        Cut cut = fitter.fit(groundTruth, new Pair<>(data, k), binSize);
        Cut all = fitter.fit(groundTruth, new Pair<>(data, k));
//        Cut cut = null;
        if (cut instanceof Outputable) {
            System.out.println(((Outputable) cut).asOutput());
            System.out.println("\nend of cut description\n");
        }

        BinCut bc = (BinCut) cut;

        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<Pair<Path, Integer>, Cut>(new Pair<>(Paths.get(".", "pac", "protein", "mlns"), 0), KCut.K_CUT)
                , new Pair<Pair<Path, Integer>, Cut>(new Pair<>(data, k), KCut.K_CUT)
                , new Pair<Pair<Path, Integer>, Cut>(new Pair<>(data, k), cut)
                , new Pair<Pair<Path, Integer>, Cut>(new Pair<>(data, k), all)
        );

        Utils utils = Utils.create();
        System.out.println(utils.plotCHEDwithCut(groundTruth, paths, true));


        //fitter.storeCut(data,cut);
    }

    private static void compare() {
        Utils utils = Utils.create();
//        Path groundTruth = Paths.get("..", "datasets", "protein-queries", "test.db");
        Path first = Paths.get(".", "pac", "protein", "src-queries_t-protein.poss_k-7_em-K_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t_alg-search");
        Path second = Paths.get(".", "pac", "protein", "src-queries_t-proteinWithoutHornHardConstraintsBenchmarkWithSymmetry.poss_k-7_em-K_l-c_is-t_dd-t_cpr-f_scout-f_sat-sym_ms-t_alg-support");
        int k = 7;

        utils.compare(first, second, k);
    }


    private static void plotStats() {
        Utils utils = Utils.create();
        int mode = 1; // stats
//        int mode = 2; // FPFN

        boolean plotAggretageted = false;
        boolean plotEachSinglePredicate = true;
        boolean maskGT = true; // dava smysl hlavne pri FPFN, jinak na tom zas tolik nezalezi

        GammaPlotter gplotter = GammaPlotter.load(Sugar.list(
                        new Triple<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gamma.cut"), GammasCut::load, Sugar.list("protein", "k-5"))
//                , new Triple<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "mGTgammas.cut"), GammasCut::load, Sugar.list("protein", "k-5"))
                        , new Triple<>(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gamma.cut"), GammasCut::load, Sugar.list("protein", "k-5")) // , "train"
//                , new Triple<>(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "mGTgammas.cut"), GammasCut::load, Sugar.list("protein", "k-5")) // , "train"
//                , new Triple<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"), GammasCut::load, Sugar.list("protein", "k-6"))
//                , new Triple<>(Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym", "gammas.cut"), GammasCut::load, Sugar.list("protein", "k-6")) // , "train"
//                , new Triple<>(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "gammas.cut"), GammasCut::load, Sugar.list("uwcs", "k-3"))
//                , new Triple<>(Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "mGTgammas.cut"), GammasCut::load, Sugar.list("uwcs", "k-3"))

                        //, new Triple<>(Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full", "gammas.cut"), GammasCut::load, Sugar.list("uwcs", "k-3")) // , "train"
                )
                , false);

//        System.out.println("empty gplotter now... debug just location/2 predicate");
//        gplotter = GammaPlotter.EMPTY_ONE;

        if (true) { // protein


            int k = 5;// vypsat i pro k=4 kvuli tomu ocasku
            Path groundTruth = Paths.get("..", "datasets", "protein-queries", "test.db");
            Path data = Paths.get(".", "pac", "protein", "t-protein.poss_k-" + k + "_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym");

            System.out.println("debug only location");

            System.out.println("\n*****************\ntest");
            if (plotEachSinglePredicate) {
                System.out.println(utils.plotStats(groundTruth, new Pair<>(data, k), utils.predicates(groundTruth), mode, maskGT, gplotter));
//                System.out.println(utils.plotStats(groundTruth, new Pair<>(data, k), Sugar.set(Predicate.get("location", 2)), mode, maskGT, gplotter));
            }
            if (plotAggretageted) {
                System.out.println(utils.plotStats(groundTruth, new Pair<>(data, k), mode, maskGT));
            }


            System.out.println("\n*****************\ntrain");
            groundTruth = Paths.get("..", "datasets", "protein-queries", "train.db");
            data = Paths.get(".", "pac", "protein", "tr-train_t-protein.poss_k-" + k + "_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym");

            if (plotEachSinglePredicate) {
                System.out.println(utils.plotStats(groundTruth, new Pair<>(data, k), utils.predicates(groundTruth), mode, maskGT, gplotter));
//                System.out.println(utils.plotStats(groundTruth, new Pair<>(data, k), Sugar.set(Predicate.get("location", 2)), mode, maskGT, gplotter));
            }
            if (plotAggretageted) {
                System.out.println(utils.plotStats(groundTruth, new Pair<>(data, k), mode, maskGT));
            }
        }

        if (false) { // uwcs
            int k = 3;

//            Path DgroundTruth = Paths.get("..", "datasets", "uwcs-queries", "train.db");
//            Path Ddata = Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-3_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full");
//            k = 3;
//            System.out.println(utils.plotStats(DgroundTruth, new Pair<>(Ddata, k), Sugar.list(Predicate.construct("Post_Quals/1")), mode));
//            System.out.println("end,debug");
//            System.exit(-4);

            System.out.println("\n*****************\ntest");
            Path groundTruth = Paths.get("..", "datasets", "uwcs-queries", "test.db");
            Path data = Paths.get(".", "pac", "uwcs", "t-uwcs.poss_k-" + k + "_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full");
            if (plotEachSinglePredicate) {
                System.out.println(replacement(utils.plotStats(groundTruth, new Pair<>(data, k), utils.predicates(groundTruth), mode, maskGT, gplotter)));
            }
            if (plotAggretageted) {
                System.out.println(replacement(utils.plotStats(groundTruth, new Pair<>(data, k), mode, maskGT)));
            }

            System.out.println("\n*****************\ntrain");
            groundTruth = Paths.get("..", "datasets", "uwcs-queries", "train.db");
            data = Paths.get(".", "pac", "uwcs", "tr-train_t-uwcs.poss_k-" + k + "_em-V_l-PL_is-t_dd-t_cpr-f_scout-t_sat-full");

            if (plotEachSinglePredicate) {
                System.out.println(replacement(utils.plotStats(groundTruth, new Pair<>(data, k), utils.predicates(groundTruth), mode, maskGT, gplotter)));
            }
            if (plotAggretageted) {
                System.out.println(replacement(utils.plotStats(groundTruth, new Pair<>(data, k), mode, maskGT)));
            }
        }

        //        Path data = Paths.get(".", "pac", "protein", "veIcajKoffset3");

//        System.out.println(utils.plotFNFP(testEvidence, data));
//        System.out.println(utils.plotStats(groundTruth, new Pair<>(data, k), Sugar.set(Predicate.construct("complex/2"))));

//        utils.gammas(new Pair<>(data, k), true);
    }

    private static void plotDiff() {
        System.out.println("tohle se musi predelat aby to zohlednovalo i k");
        Utils utils = Utils.create();
        boolean maskGT = false;
        Path testEvidence = Paths.get("..", "datasets", "protein-queries", "test.db");
//        List<Path> paths = Sugar.list(
//                Paths.get(".", "pac", "protein", "mlns")
//                , Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym")
//                , Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"));

        List<Pair<Pair<Path, Integer>, Cut>> paths = Sugar.list(
                new Pair<Pair<Path, Integer>, Cut>(new Pair<>(Paths.get(".", "pac", "protein", "mlns"), 0)
                        , KCut.K_CUT)
                , new Pair<Pair<Path, Integer>, Cut>(new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-4_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 4)
                        , KCut.K_CUT)
                , new Pair<Pair<Path, Integer>, Cut>(new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 5)
                        , KCut.K_CUT)
                , new Pair<Pair<Path, Integer>, Cut>(new Pair<>(Paths.get(".", "pac", "protein", "t-protein.poss_k-6_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym"), 6)
                        , KCut.K_CUT)
//                , new Pair<Pair<Path,Integer>, Cut>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym")
//                        , VotesThresholdCut.load(4.0))
//                , new Pair<Pair<Path,Integer>, Cut>(Paths.get(".", "pac", "protein", "t-protein.poss_k-5_em-V_l-c_is-t_dd-t_cpr-f_scout-t_sat-sym")
//                        , VotesThresholdCut.load(6.0))
        );

        System.out.println(utils.plotCHEDwithCut(testEvidence, paths, maskGT));
        System.out.println("cim je zpusoben ten rozdil oproti stare implementaci? preslechy v VEC pri paralelnim pricitani? nebo chyba ve vypoctu? zkusit starou implementaci s novym zpusobem vypoctu hlasu");
    }

    private static void runInferences() {
        System.setProperty("ida.pacReasoning.entailment.mode", "v");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");

        //for (String maxErr : Sugar.list("0.0", "1.0E-5")) {

        for (Pair<Integer, String> pair : Sugar.list(
                new Pair<>(2000, "protein") // max 4338
                , new Pair<>(1000, "kinships") // max 2649
                , new Pair<>(300, "uwcs") // max 1002
                , new Pair<>(400, "umls") // max 1410
                , new Pair<>(400, "nations") // max 701 // nations je hodne tezky
        )) {
            for (String merpp : Sugar.list("100", "10", "5", "0")) { // ,"1000"
                String domain = pair.s;
                int atMost = pair.r;
//            System.out.println("at most test!");
//            atMost = 200;
                System.setProperty("ida.pacReasoning.entailment.logic", "classical");
                //for (String theoryFileName : Sugar.list("maxErr0.0n10000m30G.theory"
                //        , "maxErr1.0E-5n10000m30G.theory"
                //)) {
                //String theoryFileName = "maxErr" + maxErr + "n10000m30G.theory";
                String theoryFileName = "merpp" + merpp + "n10000m10GWC.theory";
                Path theorySource = Paths.get(".", "pac", domain, theoryFileName);

                if (!theorySource.toFile().exists()) {
                    continue;
                }

                Theory theory = System.getProperty("ida.pacReasoning.entailment.logic").equals("possibilistic")
                        ? Possibilistic.create(theorySource)
                        : FOL.create(theorySource);

                if (theory.allRules().size() == theory.getHardRules().size()) {
                    System.out.println("skipping this theory since there are only constriants\t" + theoryFileName);
                    continue;
                }

                System.setProperty("ida.pacReasoning.entailment.saturation", "protein".equals(domain) ? "symmetry" : "full");

                for (Integer k : Sugar.list(0, 6)) {
                    if (0 == k) {
                        System.setProperty("ida.pacReasoning.entailment.mode", "a");
                        System.setProperty("ida.pacReasoning.entailment.k", "" + 0);
                    } else {
                        System.setProperty("ida.pacReasoning.entailment.mode", "v");
                        System.setProperty("ida.pacReasoning.entailment.k", "" + k);
                    }
                    EntailmentSetting setting = EntailmentSetting.create();

                    boolean mlnsSamples = domain.contains("uwcs") || domain.contains("protein");
                    String inputDir = mlnsSamples ? "queries" : "test-uniform";

                    Path evidenceFolder = Paths.get("..", "datasets", domain, inputDir);
                    Path outputEvidence = Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon());

                    java.util.function.Function<Integer, Path> storingLevelByLevel = (currentK) -> Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon().replace("k-" + k + "_", "k-" + currentK + "_"));

//                    storingLevelByLevel = null;

                    System.out.println("should store to\t" + outputEvidence);
                    Inference inference = new Inference(theory, false);
                    inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost, storingLevelByLevel);
                }
            }
        }
    }

    private static void runYago() {
        System.setProperty("ida.pacReasoning.entailment.mode", "v");
        System.setProperty("ida.pacReasoning.entailment.k", "0");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");

        //for (String maxErr : Sugar.list("0.0", "1.0E-5")) {
        for (Integer multiply : Sugar.list(1, 2, 3, 4)) {
            for (Pair<Integer, String> pair : Sugar.list(
                    new Pair<>(200, "yago-states") //
                    , new Pair<>(200, "yago-politics") // max
                    , new Pair<>(200, "yago-people") // max
                    , new Pair<>(200, "yago-musician") // max
                    , new Pair<>(200, "yago-film") // max
            )) {
                String domain = pair.s;
                int atMost = pair.r * multiply;
//            System.out.println("at most test!");
//            atMost = 200;
                System.setProperty("ida.pacReasoning.entailment.logic", "classical");
                String theoryFileName = "amieRuleDepth4MS.logic";
                Path theorySource = Paths.get(".", "pac", domain, theoryFileName);

                if (!theorySource.toFile().exists()) {
                    System.out.println("skipping since " + theorySource + " does not exists");
                    continue;
                }

                Theory theory = System.getProperty("ida.pacReasoning.entailment.logic").equals("possibilistic")
                        ? Possibilistic.create(theorySource)
                        : FOL.create(theorySource);

                System.setProperty("ida.pacReasoning.entailment.saturation", "none"); // there are no constraints

                for (Integer k : Sugar.list(0, 5)) {
                    if (0 == k) {
                        System.setProperty("ida.pacReasoning.entailment.mode", "a");
                        System.setProperty("ida.pacReasoning.entailment.k", "" + 0);
                    } else {
                        System.setProperty("ida.pacReasoning.entailment.mode", "v");
                        System.setProperty("ida.pacReasoning.entailment.k", "" + k);
                    }
                    EntailmentSetting setting = EntailmentSetting.create();

                    String inputDir = "test-uniform";

                    Path evidenceFolder = Paths.get("..", "datasets", domain, inputDir);
                    Path outputEvidence = Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon());

                    java.util.function.Function<Integer, Path> storingLevelByLevel = (currentK) -> Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon().replace("k-" + k + "_", "k-" + currentK + "_"));
//                    storingLevelByLevel = null;

                    System.out.println("should store to\t" + outputEvidence);
                    Inference inference = new Inference(theory, false);
                    inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost, storingLevelByLevel);
                }
            }
        }
    }

    private static void runKinships() {
        System.setProperty("ida.pacReasoning.entailment.mode", "a");
        System.setProperty("ida.pacReasoning.entailment.k", "0");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");

        int atMost = 300;

        String domain = "kinships";
        if ("uwcs".equals(domain)) {
            System.setProperty("ida.pacReasoning.entailment.logic", "possibilistic");
        } else {
            System.setProperty("ida.pacReasoning.entailment.logic", "classical");
        }
//        System.setProperty("ida.pacReasoning.entailment.logic", "possibilistic"); // classical pro k6 protein, jinak poss

        //System.setProperty("ida.pacReasoning.entailment.saturation", "uwcs".equals(domain) ? "full" : "symmetry");
        System.setProperty("ida.pacReasoning.entailment.saturation", "none");//  there are no constraints right now :))


        for (Integer k : Sugar.list(2, 3, 4, 5)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "v");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            Path theorySource = Paths.get(".", "pac", domain, "horn1000maxErr0.0K" + k + "GorigConstraints.theory");
//        Path theorySource = Paths.get(".", "pac", domain, "horn1000.aggergatedKGConstraints.train.db.approxAcc");
//        {


            Theory theory = System.getProperty("ida.pacReasoning.entailment.logic").equals("possibilistic")
                    ? Possibilistic.create(theorySource)
                    : FOL.create(theorySource);

            EntailmentSetting setting = EntailmentSetting.create();

            String inputDir = "test-uniform";
            Path evidenceFolder = Paths.get("..", "datasets", domain, inputDir);
            Path outputEvidence = Paths.get(".", "pac", domain, "src-" + inputDir + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon());

            System.out.println("should store to\t" + outputEvidence);
            Inference inference = new Inference(theory, false);
            inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost);
        }

    }

    private static void runInference() {
        System.setProperty("ida.pacReasoning.entailment.k", "0"); // 6
//        System.setProperty("ida.pacReasoning.entailment.k", "5"); // 6
//        System.setProperty("ida.pacReasoning.entailment.mode", "v");
        System.setProperty("ida.pacReasoning.entailment.mode", "a");
        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");

        String domain = "uwcs";
        //String source = "test-uniform";
        String source = "queries";
        int atMost = 500;

//        String theoryFileName = "negativeSamples.theory";
        //String theoryFileName = "src_train.db.typed.uni0.5.db_theory_r15_bs5_d4_ttrue_c116.fol.filtered.poss";
        //String theoryFileName = "icajPredictiveMyConstraints.poss";
        //String theoryFileName = "icajPredictiveMyConstraintsTypeless.poss";
        String theoryFileName = "icajPredictive.poss";

        System.out.println("ok - orezavaji se typy pro chedevaluaci vysledku udelat bez typingu at je to zpetne kompatabilni; melo by to byt zaneseno i ve vykreslovani FP/FN " +
                "hotovo - zavest typing do evidence pri inferenci aby to fungovalo jak ma" +
                "hotovo - opravit/dodat typy v teoriich" +
                "todo: do horn rule learneru pridat pruning co chtel jessie + pridat tam nejake hacky, pustit experimenty at jim mame co ukazat a tak");

        //System.setProperty("ida.pacReasoning.entailment.logic", "classical");
        System.setProperty("ida.pacReasoning.entailment.logic", "possibilistic"); // classical pro k6 protein, jinak poss
        Path theorySource = Paths.get(".", "pac", domain, theoryFileName);
        Theory theory = System.getProperty("ida.pacReasoning.entailment.logic").equals("possibilistic")
                ? Possibilistic.create(theorySource)
                : FOL.create(theorySource);

        System.out.println("debug, test theory\n" + theory.getHardRules() + "\n" + theory.allRules() + "\n" + ((Possibilistic) theory).asOutput());

        System.setProperty("ida.pacReasoning.entailment.saturation", "none");

//        for (Integer k : IntStream.range(3, 5).boxed().collect(Collectors.toList())) {
//            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
//            System.setProperty("ida.pacReasoning.entailment.mode", "v");

        EntailmentSetting setting = EntailmentSetting.create();
        //Path evidenceFolder = Paths.get("..", "datasets", "mlns", domain, source);
        Path evidenceFolder = Paths.get("..", "datasets", domain, source);

        Path outputEvidence = Paths.get(".", "pac", domain, "src-" + source + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon());

        System.out.println("should store to\t" + outputEvidence);

        Map<Pair<Predicate, Integer>, String> typing = true ? TypesInducer.load(Paths.get("..", "datasets", domain, "typing.txt")) : null;
        Inference inference = new Inference(theory, null, false, typing);
        inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost);
//        }

    }

    /*
    private static void runYago() {
        System.setProperty("ida.pacReasoning.entailment.k", "0");
        System.setProperty("ida.pacReasoning.entailment.mode", "a");

        System.setProperty("ida.pacReasoning.entailment.dataDriven", "true");
        System.setProperty("ida.pacReasoning.entailment.scoutInit", "true");
        System.setProperty("ida.pacReasoning.entailment.cpRelaxation", "false");
        System.setProperty("ida.pacReasoning.entailment.cpPrune", "false");
        System.setProperty("ida.pacReasoning.entailment.megaScout", "true");

        String queries = "test-uniform";
        int atMost = 200;

        System.setProperty("ida.pacReasoning.entailment.logic", "classical");
//        Path theorySource = Paths.get(".", "pac", "yago2", "negativeSamples-100-10-1-2.theory");
//        Path theorySource = Paths.get(".", "pac", "yago2", "negativeSamples-100-10-1-1.theory");
        //Path theorySource = Paths.get(".", "pac", "yago2", "theory.txt");
//        Path theorySource = Paths.get(".", "pac", "yago2", "amieAll.theory");
//        Theory theory = FOL.get(theorySource);

        System.setProperty("ida.pacReasoning.entailment.saturation", "none"); // there are no horn rules in constraints, at least I think so


//        String domain = "yago-politics";
//        String domain = "yago-states";
//        String domain = "yago-film";
//        String domain = "yago-people";
//        String domain = "yago-musician";

//        double maxError = 0.01;
//        double maxError = 0.0001;
//        int subsampled = 4000;

        for (String domain : Sugar.list("yago-politics", "yago-states", "yago-musician", "yago-film", "yago-people")) {

            for (Integer k : IntStream.range(2, 7).boxed().collect(Collectors.toList())) {
                if (6 == k) {
                    System.setProperty("ida.pacReasoning.entailment.mode", "a");
                    System.setProperty("ida.pacReasoning.entailment.k", "0");
                } else {
                    System.setProperty("ida.pacReasoning.entailment.mode", "v");
                    System.setProperty("ida.pacReasoning.entailment.k", "" + k);
                }
                //Path theorySource = Paths.get(".", "pac", domain, "amieRuleDepth4.logic");
                Path theorySource = Paths.get(".", "pac", domain, "amieRuleDepth4Constraints.logic");

//            System.setProperty("ida.pacReasoning.entailment.mode", "v");
//            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
//            Path theorySource = Paths.get(".", "pac", domain, "maxErr" + maxError + "K" + k + "orig" + subsampled + ".theory");

//            Path theorySource = Paths.get(".", "pac", domain, "maxErr1.0E-4K4orig4000.theory");
                Theory theory = FOL.get(theorySource);

                EntailmentSetting setting = EntailmentSetting.get();
                //Path evidenceFolder = Paths.get(".", "pac", domain, queries);
//            Path outputEvidence = Paths.get(".", "pac", domain, "src-" + queries + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon());

                Path evidenceFolder = Paths.get("..", "datasets", domain, queries);
                Path outputEvidence = Paths.get(".", "pac", domain, "src-" + queries + "_" + "t-" + theorySource.toFile().getName() + "_" + setting.canon());


                System.out.println("should store to\t" + outputEvidence);
                Inference inference = new Inference(theory, false);
                inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost);
                inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost, atMost);

            }
        }


//        System.out.println("should store to\t" + outputEvidence);
//        Inference inference = new Inference(theory, false);
        //inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost);
//        inference.inferFolder(evidenceFolder, outputEvidence, setting, atMost, atMost);
    }
    */
    public static int evidenceFileNumber(Path path) {
        if (path.toFile().getName().contains("queries")) {
            return Integer.parseInt(path.toFile().getName().split("\\.")[0].substring("queries".length()));
        }
        return Integer.parseInt(path.toFile().getName().split("\\.")[0]);
    }

    private static void checkConstnatsVE(Path queriesSource) {
        Map<Double, List<Integer>> scores = new HashMap<>();
        List<Integer> baseline = Sugar.list();
        try {
            Files.list(queriesSource)
                    .filter(dir -> dir.toFile().isFile())
                    .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                    .forEach(dir -> {
                        Pair<Set<Literal>, Map<Double, Set<Literal>>> pair = loadVotedEvidence(dir);
                        Set<Literal> currentEvidence = Sugar.set();
                        Map<Double, Set<Literal>> map = pair.getS();
                        for (Double key : map.keySet()) {
                            currentEvidence = Sugar.union(currentEvidence, map.get(key));
                        }
                        Set<Literal> found = Sugar.set();
                        currentEvidence.stream().filter(l -> LogicUtils.constantsFromLiteral(l).size() != 2)
                                .forEach(found::add);

                        if (!found.isEmpty()) {
                            System.out.println(dir);
                            found.forEach(l -> System.out.println("\t" + l));
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Pair<Set<Literal>, Map<Double, Set<Literal>>> loadVotedEvidence(Path file) {
        Map<Double, Set<Literal>> voted = new HashMap<>();
        Set<Literal> always = Sugar.set();

        try {
            Files.lines(file)
                    .filter(line -> line.trim().length() > 0)
                    .forEach(line -> {
                        String[] splitted = line.split("\t", 3);
                        boolean fromEvidence = Boolean.parseBoolean(splitted[0]);
                        Literal literal = Literal.parseLiteral(splitted[2]);
                        if (fromEvidence) {
                            always.add(literal);
                        } else {
                            double votes = Double.parseDouble(splitted[1]);
                            if (!voted.containsKey(votes)) {
                                voted.put(votes, Sugar.set());
                            }
                            voted.get(votes).add(literal);
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Pair<>(always, voted);
    }

    private static void compare(Path first, Path second, int atMost) throws IOException {
        List<Path> streamFirst = Files.list(first)
                .filter(dir -> dir.toFile().isFile())
                .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                .filter(dir -> evidenceFileNumber(dir) <= atMost).collect(Collectors.toList());
        List<Path> streamSecond = Files.list(second)
                .filter(dir -> dir.toFile().isFile())
                .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                .filter(dir -> evidenceFileNumber(dir) <= atMost).collect(Collectors.toList());

        for (int idx = 0; idx < Math.min(streamFirst.size(), streamSecond.size()); idx++) {
            Path f = streamFirst.get(idx);
            Path s = streamSecond.get(idx);
            Set<Literal> fset = loadEvidence(f);
            Set<Literal> sset = loadEvidence(s);
            if (!fset.equals(sset)) {
                System.out.println("comparing\n\t" + f + "\n\t" + s + "\n\t" + fset.equals(sset));
                System.out.println("in first, not in second");
                Sugar.setDifference(fset, sset).stream().forEach(l -> System.out.println("\t" + l));
                System.out.println("in second, not in first");
                Sugar.setDifference(sset, fset).stream().forEach(l -> System.out.println("\t" + l));
            }
        }
    }

    private static void compareVE(Path first, Path second, int atMost, double minVotes) throws IOException {
        List<Path> streamFirst = Files.list(first)
                .filter(dir -> dir.toFile().isFile())
                .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                .filter(dir -> evidenceFileNumber(dir) <= atMost).collect(Collectors.toList());
        List<Path> streamSecond = Files.list(second)
                .filter(dir -> dir.toFile().isFile())
                .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                .filter(dir -> evidenceFileNumber(dir) <= atMost).collect(Collectors.toList());

        for (int idx = 0; idx < Math.min(streamFirst.size(), streamSecond.size()); idx++) {
            Path f = streamFirst.get(idx);
            Path s = streamSecond.get(idx);
            //Set<Literal> fset = loadEvidence(f);
            Pair<Set<Literal>, Map<Double, Set<Literal>>> p = loadVotedEvidence(f);
            Set<Literal> fset = p.getR();
            p.s.entrySet().stream()
                    .filter(entry -> entry.getKey() >= minVotes)
                    .forEach(entry -> fset.addAll(entry.getValue()));
            Set<Literal> sset = loadEvidence(s);
            if (!fset.equals(sset)) {
                System.out.println("comparing\n\t" + f + "\n\t" + s + "\n\t" + fset.equals(sset));
                System.out.println("in first, not in second");
                Sugar.setDifference(fset, sset).stream().forEach(l -> System.out.println("\t" + l));
                System.out.println("in second, not in first");
                Sugar.setDifference(sset, fset).stream().forEach(l -> System.out.println("\t" + l));
            }
        }
    }


    private static void veResultsToPlain(Path first, Path second, double minVotes) throws IOException {
        Files.list(first)
                .filter(dir -> dir.toFile().isFile())
                .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                .forEach(dir -> {
                    Pair<Set<Literal>, Map<Double, Set<Literal>>> p = loadVotedEvidence(dir);
                    Set<Literal> entailed = Sugar.set();
                    entailed.addAll(p.r);
                    for (Map.Entry<Double, Set<Literal>> entry : p.getS().entrySet()) {
                        if (entry.getKey() >= minVotes) {
                            entailed.addAll(entry.getValue());
                        }
                    }
                    Path output = Paths.get(second.toString(), dir.getFileName() + "");
                    System.out.println("output to\t" + output);
                    try {
                        if (!output.getParent().toFile().exists()) {
                            output.getParent().toFile().mkdirs();
                        }
                        Files.write(output, entailed.stream().map(l -> l + "").collect(Collectors.toList()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static void fpfn(Path queriesSource, Path groundTruthFile, int atMost) {
        Set<Literal> groundTruth = LogicUtils.untype(loadEvidence(groundTruthFile));
        try {
            Stream<Set<Literal>> stream;
            if (queriesSource.toFile().isDirectory()) {
                stream = Files.list(queriesSource)
                        .filter(dir -> dir.toFile().isFile())
                        .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                        .filter(dir -> evidenceFileNumber(dir) <= atMost)
                        .map(dir -> loadEvidence(dir));
            } else {
                stream = Files.lines(queriesSource)
                        .limit(atMost)
                        .map(line -> Clause.parse(line.substring(1, line.length() - 1)).literals()); // jaky ma tohle vyznam?
            }
            List<Set<Literal>> list = stream.map(LogicUtils::untype).collect(Collectors.toList());

            List<Integer> fn = list.stream().map(entailed -> {
                Set<Literal> set = Sugar.setFromCollections(groundTruth);
                set.removeAll(entailed);
                return set.size();
            }).collect(Collectors.toList());

            List<Integer> fp = list.stream().map(entailed -> {
                Set<Literal> set = Sugar.setFromCollections(entailed);
                set.removeAll(groundTruth);
                return set.size();
            }).collect(Collectors.toList());

            System.out.println("cummulative fp\n" + Statistics.cummulativeSum(fp));
            System.out.println("cummulative fn\n" + Statistics.cummulativeSum(fn));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static Set<Literal> loadEvidence(String line) {
        boolean pseudoPrologNotation = false;
        boolean trimTicks = true;
        Set<Literal> literals = Clause.parse(trimTicks ? line.replace("\"", "") : line).literals();
        if (!pseudoPrologNotation) {
            literals = literals.stream().map(LogicUtils::constantize).collect(Collectors.toSet());
        }
        return literals;
    }

    static Set<Literal> loadEvidence(Path path) {
        Set<Literal> literals = null;
        try {
            literals = Files.readAllLines(path)
                    .stream()
                    .flatMap(line -> loadEvidence(line).stream())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return literals;
    }


    private static void pacLearning() throws IOException {
        Path path = Paths.get("..", "datasets", "queries", "train.db");
        boolean pseudoPrologNotation = false;
        //Path path = Paths.get(".", "datasets", "mlns", "protein", "pacInput.transformed");boolean pseudoPrologNotation = true;

        PSLearning pac = PSLearning.create();
        Set<Literal> literals = Files.readAllLines(path)
                .stream()
                .flatMap(line -> Clause.parse(line).literals().stream())
                .collect(Collectors.toSet());
        if (!pseudoPrologNotation) {
            literals = literals.stream().map(LogicUtils::constantize).collect(Collectors.toSet());
        }
        pac.learn(literals);
    }


    private static void filterEquals() {
        String domain = "protein";
        Path constraintsFile = Paths.get("..", "datasets", domain, "constraints.txt");
        //Path rulePair = Paths.get(".", "pac", domain, "hornLearner.logic.pairs");
        Path rulePair = Paths.get(".", "pac", domain, "hornLearner.out.protein.text");
        Path output = Paths.get(".", "pac", domain, "hornLearner.logic.filtered-oldImpl");

        Utils u = Utils.create();
        Set<Clause> constraints = u.loadClauses(constraintsFile);
        Set<IsoClauseWrapper> in = Sugar.set();
        RuleSaturator saturator = RuleSaturator.create(constraints);
        try {
            List<Pair<Double, Clause>> pairs = Files.lines(rulePair)
                    .map(line -> {
                        if (line.contains("#EmptyClause")) {
                            return null;
                        }
                        String[] splitted = line.trim().split("\t", 2);
                        if (splitted.length != 2) {
                            return null;
                        }
                        try {
                            double acc = Double.parseDouble(splitted[0]);
                            HornClause hc = HornClause.parse(splitted[1]);
                            Clause clause = hc.toClause();
                            //Clause clause = Clause.parse(splitted[1]);

                            HornClause saturated = saturator.saturate(HornClause.create(clause));
                            if (null == saturated) {
                                System.out.println("throwing out\t" + line);
                                return null;
                            }
                            Clause saturatedClause = saturated.toClause();
                            IsoClauseWrapper icw = IsoClauseWrapper.create(saturatedClause);
                            if (in.add(icw)) {
                                return new Pair<>(acc, clause);
                            }
                            return null;
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.comparingDouble(Pair<Double, Clause>::getR).reversed())
                    .collect(Collectors.toList());

            System.out.println("zadne ukladani!");
            //Files.write(Paths.get(output.toString() + ".pairs"), pairs.stream().map(p -> p.r + "\t" + p.s).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

