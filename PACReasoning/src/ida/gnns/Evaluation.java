package ida.gnns;

import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.Term;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 18. 3. 2021.
 */
public class Evaluation {

    public static void main(String[] args) {
        String dataset = "nations";
        Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "train.nl"));
        Set<Literal> valid = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "dev.nl"));
        Set<Literal> test = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "test.nl"));
        Set<Literal> allFacts = Sugar.union(evidence, test, valid);
        Set<String> entities = allFacts.stream().flatMap(l -> l.terms().stream())
                .map(Term::toString).collect(Collectors.toSet());
        Utils u = Utils.create();

        String embeddingName = "gntp-complex";
        List<Pair<String, String>> l = Sugar.list(
                new Pair<>(embeddingName + ".entailed", embeddingName)
                //new Pair<>(embeddingName + ".entailed", embeddingName)
//                , new Pair<>("rotate" + ".entailed", "rotate")
//                , new Pair<>("distmult" + ".entailed", "distmult")
        );
        List<Pair<VECollector, String>> vals = l.stream()
                .map(s -> {
                    //Path p = Paths.get("..", "forwardPredictions", dataset + "-ntp-o", s.getR());
                    Path p = Paths.get("..", "forwardPredictions", dataset, s.getR());
                    return new Pair<>(VECollector.load(p, 1), s.getS());
                })
                .collect(Collectors.toList());


        /**/
        String threshold = "elowq";
        for (Pair<String, String> pair : Sugar.list(
//                new Pair<>("./forwardPredictionsDev/" + dataset + "-ntp-o/" + embeddingName + "_0.85.rules_ehighq.entailed", " ehighq")
//                , new Pair<>("./forwardPredictionsDev/" + dataset + "-ntp-o/" + embeddingName + "_0.85.rules_emedian.entailed", " emedian")
//                , new Pair<>("./forwardPredictionsDev/" + dataset + "-ntp-o/" + embeddingName + "_0.85.rules_elowq.entailed", " elowq")
//                ,
                /**/
                new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.95.rules_" + threshold + ".entailed", "0.95 " + threshold)
                , new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.9.rules_" + threshold + ".entailed", "0.9 " + threshold)
                , new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.85.rules_" + threshold + ".entailed", "0.85 " + threshold)
                , new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.8.rules_" + threshold + ".entailed", "0.8 " + threshold)
                , new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.7.rules_" + threshold + ".entailed", "0.7 " + threshold)
                , new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.5.rules_" + threshold + ".entailed", "0.5 " + threshold)
                /**/


                /*new Pair<>("../hyForwardDown/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_rules500.all_10_m1.entailedT", "complex forwardDown m1")
                , new Pair<>("../hyForwardUpDown/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_rules500.all_ex_1_10.entailed", "complex forwardUpDown 1")
                , new Pair<>("../hyForwardUpDown/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_rules500.all_ex_3_10.entailed", "complex forwardUpDown 3")
                , new Pair<>("../hyForwardUpDown/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_rules500.all_ex_5_10.entailed", "complex forwardUpDown 5")
                , new Pair<>("../hyForwardUpDown/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_rules500.all_ex_6_10.entailed", "complex forwardUpDown 6")
                , new Pair<>("../hyForwardUpDown/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_rules500.all_ex_7_10.entailed", "complex forwardUpDown 7")
                , new Pair<>("../hyForwardUpDown/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_rules500.all_ex_8_10.entailed", "complex forwardUpDown 8")
                , new Pair<>("../hyForwardUpDown/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_rules500.all_ex_9_10.entailed", "complex forwardUpDown 9")
*/
                /**new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.85.rules_ehighq.entailed", "0.85 ehighq")
                 , new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.85.rules_emedian.entailed", "0.85 emedian")
                 , new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.85.rules_elowq.entailed", "0.85 elowq")
                 , new Pair<>("../hyperion/" + dataset + "-ntp-o/anyburl/" + embeddingName + "_0.85.rules_zero.entailed", "0.85 zero")
                 /**/
        )) {
            Path p = Paths.get(pair.r);
            System.out.println(p);
            System.out.println(p.toAbsolutePath());
            if (!p.toFile().exists()) {
                System.out.println("not found! skipping!");
            } else {
                vals.add(new Pair<>(VECollector.load(p, 1), pair.s));
            }
        }
        /**/

        Map<Literal, Set<Literal>> queries = u.generateHitsCompanion(test, allFacts, entities);

        Path p = Paths.get("..", "forwardPredictions", dataset, embeddingName + ".entailed");
        VECollector basicCollector = VECollector.load(p, 0);

//        if (true) {
//            System.out.println("zkontrolovat");
//            throw new IllegalStateException();
//        }

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);

        double addValue = -10000.0;
        for (Pair<VECollector, String> pair : vals) {
            Random random = new Random();
            System.out.println("\n" + pair.getS());
            VECollector predicted = pair.getR();
/**/
            basicCollector.getVotes().entrySet().forEach(e -> {
                if (!predicted.getVotes().keySet().contains(e.getKey())) {
                    predicted.getVotes().add(e.getKey(), e.getValue() + addValue);
                }
            });
/**/
            //System.out.println("predicted size\t" + predicted.getVotes().size());

            Map<Literal, Integer> overallRanks = u.loadRanks(predicted);
            List<Double> hits = u.computeRanks(queries, overallRanks, random);
            //System.out.println("MRR\t" + df.format(hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size()));
            //System.out.println(hits.stream().sorted().collect(Collectors.toList()));
            System.out.println("MRR\t" + hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size());
            for (Integer hit : Sugar.list(1, 3, 10)) {
                System.out.println("hits " + hit + "\t" + df.format((hits.stream().filter(val -> val <= hit).count() * 1.0 / hits.size())));
            }

        }

    }
}
