package ida.gnns;

import ida.ilp.logic.Constant;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.Term;
import ida.pacReasoning.data.Reformatter;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.evaluation.Data;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Sugar;
import ida.utils.collections.Counters;
import ida.utils.collections.MultiList;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Quadruple;
import ida.utils.tuples.Triple;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

import static ida.pacReasoning.data.Reformatter.loadData;

/**
 * Created by martin.svatos on 22. 3. 2021.
 */
public class AnyBurlCheck {

    public static void main(String[] args) {
//        incorporateEmbeddings();
//        checkRanks(); // vypise histogram poctu unikatnich hodnot pro literaly z anyburl
//        checkRanking();
        transferRankings();
//        debugWN18RR();
//        debugHits();
    }


    private static void incorporateEmbeddings() {
        String dataset = "umls";
        String embeddingName = "complex";
        List<Path> src = Sugar.list(
                Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_10")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_50")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_100")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_500")
        );

        /*Map<Literal, Double> scores = ForwardChaining.loadEmbeddings(Paths.get("..", "inferred", dataset, embeddingName, "train.log"));

        for (Path path : src) {
            System.out.println(path);
            Path outPath = Paths.get(path + "-" + embeddingName);
            replaceWithEmbbeddingScores(path, outPath, scores);
        }
        */
    }

    private static void replaceWithEmbbeddingScores(Path path, Path outPath, Map<Literal, Double> collector) {
        List<String> lines = Sugar.list();

        if (false) {
            // nekde je tu nejspise chyba
            throw new IllegalStateException();// NotImplementedException();
        }
        Set<Term> entities = collector.keySet().stream().flatMap(literal -> literal.argumentsStream()).collect(Collectors.toSet());

        Pair<Literal, String> state = new Pair<>(null, null);
        try {
            Files.lines(path).filter(l -> !l.trim().isEmpty())
                    .forEach(line -> {
                        StringBuilder sb = new StringBuilder();
                        List<Pair<String, Double>> vals = Sugar.list();
                        if (line.startsWith("Heads:")) {
                            sb.append("Heads:");
                            String[] split = line.split("\\s+");
/**/
                            for (int idx = 1; idx < split.length; idx += 2) {
                                Literal corrupted = new Literal(state.getR().predicate(), Constant.construct(split[idx]), state.getR().get(1));
                                vals.add(new Pair<>(split[idx], collector.get(corrupted)));
                            }
 /**/
                            /** /
                             System.out.println("forcing all");
                             for (Term entity : entities) { // forcing all
                             Literal corrupted = new Literal(state.getR().predicate(), entity, state.getR().get(1));
                             vals.add(new Pair<>(entity.toString(), collector.get(corrupted)));
                             }/**/

                            vals = vals.stream().sorted(Comparator.comparing(Pair::getS)).collect(Collectors.toList());
                            Collections.reverse(vals);
                            vals.forEach(p -> sb.append("\t" + p.getR() + "\t" + p.getS()));
                        } else if (line.startsWith("Tails:")) {
                            sb.append("Tails:");
                            String[] split = line.split("\\s+");
/**/
                            for (int idx = 1; idx < split.length; idx += 2) {
                                Literal corrupted = new Literal(state.getR().predicate(), state.getR().get(0), Constant.construct(split[idx]));
                                vals.add(new Pair<>(split[idx], collector.get(corrupted)));
                            }
 /**/
/** /
 System.out.println("forcing all");
 for (Term entity : entities) { // forcing all
 Literal corrupted = new Literal(state.getR().predicate(), state.getR().get(0), entity);
 vals.add(new Pair<>(entity.toString(), collector.get(corrupted)));
 }/**/

                            vals = vals.stream().sorted(Comparator.comparing(Pair::getS)).collect(Collectors.toList());
                            Collections.reverse(vals);
                            vals.forEach(p -> sb.append("\t" + p.getR() + "\t" + p.getS()));
                        } else {
                            String[] split = line.split("\\s+");
                            assert split.length == 3;
                            state.r = Literal.parseLiteral(split[1] + "(" + split[0] + "," + split[2] + ")");
                            sb.append(line);
                        }
                        lines.add(sb.toString().trim());
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            Files.write(outPath, lines);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void transferRankings() {
//        String dataset = "nations";
//        String dataset = "umls";
//        String dataset = "kinships";
//        String dataset = "codexmedium";
//        String dataset = "wn18rr";
        String dataset = "fb15k237";
        List<Path> src = Sugar.list(
//                Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_10")
//                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_50")
//                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_100")
//                ,
                //Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500")
//                Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10"),
//                Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-50")
//                Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-500_all")
                //Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-500"),
                Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-500_20000")
                //Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-100")
        );

        Path base = Paths.get("..", "datasets", dataset + "-ntp-o");
        Quadruple<Set<Literal>, Set<Literal>, Set<Literal>, Boolean> data = Reformatter.loadData(dataset);
        Set<Literal> evidence = data.r;
        Set<Literal> valid = data.s;
        Set<Literal> test = data.t;
        boolean renameToIndexes = data.u;

        Set<Literal> forbidden = Sugar.union(evidence, valid);
        for (Path path : src) {
            System.out.println(path);
            Path out = Paths.get(path + ".entailed");
            System.out.println(out);
            transferToEntailed(path, forbidden, out, renameToIndexes);
        }

    }

    private static void transferToEntailed(Path path, Set<Literal> forbidden, Path out, boolean renameToIndexes) {
        transferToEntailed(path, forbidden, out, renameToIndexes, Sugar.set());
    }

    private static void transferToEntailed(Path path, Set<Literal> forbidden, Path out, boolean renameToIndexes, Set<Literal> baseEvidence) {
        MultiList<Literal, String> map = new MultiList<>();
        Pair<Literal, String> state = new Pair<>(null, null); // awkward
        try {
            Files.lines(path).filter(l -> !l.trim().isEmpty())
                    .forEach(line -> {
                        if (line.startsWith("Heads:") || line.startsWith("Tails:")) {
                            String[] split = line.split("\\s+");
                            for (int idx = 1; idx < split.length; idx += 2) {

                                Term constant = Constant.construct(split[idx]);
                                if (renameToIndexes) {
                                    constant = Reformatter.stripFirstLettersFromNonVariables(constant);
                                }

                                Literal corrupted = line.startsWith("Heads:") ?
                                        new Literal(state.getR().predicate(), constant, state.getR().get(1))
                                        : new Literal(state.getR().predicate(), state.getR().get(0), constant);

                                if (forbidden.contains(corrupted)) {
                                    continue;
                                }
                               /* if ((
                                        //Sugar.set(map.get(corrupted)).size() >= 2
                                        //!map.get(corrupted).isEmpty()
                                        //      && !map.get(corrupted).contains(split[idx + 1]))
                                        //|| (corrupted.toString().equals("21(3, 13018)"))
                                        "12(6577, 4620)".equals(corrupted.toString()))
                                ) {
                                    System.out.println("here!");
                                    System.out.println(state.r);
                                    System.out.println(corrupted);
                                    System.out.println(split[idx + 1]);
                                    System.out.println(line);
                                    //System.exit(-1);
                                }*/
                                //map.put(corrupted, split[idx + 1]);
                                // instead in place max!
                                List<String> val = map.get(corrupted);
                                if (!val.isEmpty()) {
                                    if (Double.compare(Double.parseDouble(val.get(0)), Double.parseDouble(split[idx + 1])) <= 0) {
                                        val.remove(0);
                                        map.put(corrupted, split[idx + 1]);
                                    }
                                } else {
                                    map.put(corrupted, split[idx + 1]);
                                }
                            }
                        } else {
                            String[] split = line.split("\\s+");
                            assert split.length == 3;
                            state.r = Literal.parseLiteral(split[1] + "(" + split[0] + "," + split[2] + ")");
                            if (renameToIndexes) {
                                state.r = Reformatter.stripFirstLettersFromNonVariables(state.r);
                            }
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }

        /*Counters<Integer> histogram = new Counters<>();
        map.entrySet().forEach(e -> histogram.increment(Sugar.setFromCollections(e.getValue()).size()));
        System.out.println(histogram);
        */

        /*map.entrySet().forEach(e -> {
            if (Sugar.setFromCollections(e.getValue()).size() > 2) {
                System.out.println(e.getKey());
                Sugar.setFromCollections(e.getValue()).forEach(v -> System.out.println("\t" + v));
            }
        });
        */

//        VECollector collector = VECollector.create(forbidden, 0);

        /*map.entrySet().forEach(e -> collector.getVotes().add(
                e.getKey()
                , e.getValue().stream().mapToDouble(Double::parseDouble).average().orElseThrow(IllegalStateException::new)
        ));
        try {
            Files.write(out, Sugar.list(collector.asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }*/
        File fout = new File(out.toString());
        try {
            FileOutputStream fos = new FileOutputStream(fout);
            OutputStreamWriter osw = new OutputStreamWriter(fos);

            /*map.entrySet().forEach(e -> collector.getVotes().add(
                    e.getKey()
                    , e.getValue().stream().mapToDouble(Double::parseDouble).average().orElseThrow(IllegalStateException::new)
            ));*/
            baseEvidence.forEach(literal -> {
                try {
                    osw.write("true\t-1.0\t" + literal + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            map.entrySet().stream()
                    .filter(entry -> !forbidden.contains(entry.getKey()))
                    .forEach(entry -> {
                        try {
                            osw.write("false\t"
                                    + entry.getValue().stream().mapToDouble(Double::parseDouble).max().getAsDouble()
                                    + "\t" + entry.getKey() + "\n");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });

            osw.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private static void checkRanks() {
//        String dataset = "kinships";
        String dataset = "umls";
        List<Path> src = Sugar.list(Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_10")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_50")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_100")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500")
        );

        Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "train.nl"));
        Set<Literal> valid = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "dev.nl"));
        Set<Literal> forbidden = Sugar.union(evidence, valid);
        for (Path path : src) {
            System.out.println(src);
            ranksCardinalityCheck(path, forbidden);
        }
    }


    private static void checkRanking() {
//                String dataset = "kinships";
        String dataset = "nations";
/*        List<Path> src = Sugar.list(Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_10")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_50")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_100")
                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500")
        );
*/
        List<Path> src = Sugar.list(
                //Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_500")
//                ,Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_10-complex")
//                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_50-complex")
//                , Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_100-complex")
//                ,Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-10_500-complex")
                //Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500")

                /*Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500")
                ,Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-80-ntp-o-all_500")
                ,Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-50-ntp-o-all_500")
                ,Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-20-ntp-o-all_500")
*/
                // Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-500")
                Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-100") // there is no 500 for nations

//                Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500_0.9")
//                ,Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500_0.8")
//                ,Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500_0.7")
//                ,Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-all_500_0.5")
        );
        /* we can't use this because tail can be predicted with different value than head
        List<Pair<Path, Path>> entailed = src.stream().map(p -> new Pair<>(p, Paths.get(p + ".entailed"))).collect(Collectors.toList());
        entailed.stream().filter(p -> !p.getS().toFile().exists())
                .forEach(p -> storeToCollector(p.getR(), p.getS()));
        */

        Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "train.nl"));
        Set<Literal> valid = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "dev.nl"));
        Set<Literal> test = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "test.nl"));
        /*Set<Literal> allFacts = Sugar.union(evidence, test, valid);
        Set<String> entities = allFacts.stream().flatMap(l -> l.terms().stream())
                .map(Term::toString).collect(Collectors.toSet());
        Utils u = Utils.create();

        Map<Literal, Set<Literal>> queries = u.generateHitsCompanion(test, allFacts, entities);

        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);

        for (Pair<Path, Path> pathPathPair : entailed) {
            VECollector predicted = VECollector.load(pathPathPair.getS(), 0);
            Random random = new Random();

            Map<Literal, Integer> overallRanks = u.loadRanks(predicted);
            List<Double> hits = u.computeRanks(queries, overallRanks, random);
            //System.out.println("MRR\t" + df.format(hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size()));
            System.out.println("MRR\t" + hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size());
            for (Integer hit : Sugar.list(1, 3, 10)) {
                System.out.println("hits " + hit + "\t" + df.format((hits.stream().filter(val -> val <= hit).count() * 1.0 / hits.size())));
            }
        }
        */

        Set<Literal> forbidden = Sugar.union(valid, evidence, test);
        DecimalFormat df = new DecimalFormat();
        df.setMaximumFractionDigits(3);

        int maxConstants = forbidden.stream().flatMap(l -> l.argumentsStream()).collect(Collectors.toSet()).size();
        for (Path path : src) {
            System.out.println(path);
            List<Double> hits = ranks(path, forbidden, maxConstants, false);
            System.out.println(hits.stream().sorted().collect(Collectors.toList()));
            //System.out.println("MRR\t" + df.format(hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size()));
            System.out.println("MRR\t" + hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size());
            for (Integer hit : Sugar.list(1, 3, 10)) {
                System.out.println("hits " + hit + "\t" + df.format((hits.stream().filter(val -> val <= hit).count() * 1.0 / hits.size())));
            }
        }
    }


    private static void debugHits() {
        for (String dataset : Sugar.list("codexmedium", "fb15k237", "dbpedia50", "codexsmall", "wn18rr")) {
            Path datasetPath = Paths.get("..", "datasets", dataset + "-ntp-o");
            boolean useRenaming = !Sugar.list("kinships", "umls", "nations").contains(dataset);
            Map<String, Double> results = AnyBurlCheck.anyBurlRanking(dataset);
            System.out.println(dataset);
            System.out.println("all test\t" + results.get("total"));
            System.out.println("hit10\t" + results.get("hit10") + "\t" + results.get("hit10") / (2 * results.get("total")));
            System.out.println("hit100\t" + results.get("hit100") + "\t" + results.get("hit100") / (2 * results.get("total")));
            System.out.println("hit500\t" + results.get("hit500") + "\t" + results.get("hit500") / (2 * results.get("total")));
            System.out.println("hit1000\t" + results.get("hit1000") + "\t" + results.get("hit1000") / (2 * results.get("total")));
            System.out.println("hit2000\t" + results.get("hit2000") + "\t" + results.get("hit2000") / (2 * results.get("total")));
            System.out.println("hit5000\t" + results.get("hit5000") + "\t" + results.get("hit5000") / (2 * results.get("total")));
            System.out.println("hit10000\t" + results.get("hit10000") + "\t" + results.get("hit10000") / (2 * results.get("total")));
        }
    }

    private static void debugWN18RR() {
        String dataset = "wn18rr";
        Path datasetPath = Paths.get("..", "datasets", dataset + "-ntp-o");
        boolean useRenaming = !Sugar.list("kinships", "umls", "nations").contains(dataset);
        Quadruple<Set<Literal>, Set<Literal>, Set<Literal>, Boolean> data = Reformatter.loadDataAndRename(datasetPath, useRenaming); // force rename since we are using optimized impl onwards
        Utils u = Utils.create();
        List<Data<Double, Double>> plots = Sugar.list();

        for (String suffix : Sugar.list("_10", "_50", "_100", "_500")) {
            Path anyBurl = Paths.get("C:\\data\\school\\development\\anyburl\\predictions", dataset + "-ntp-o-500" + suffix + ".entailed");
            VECollector anyburlCollector = VECollector.load(anyBurl, 0);
            // neni tady problem pro AnyBURL kdyz nedokaze vsechno co by mel????? to se tady snazim zjistit :))
            Data<Number, Number> anyburlPoints = u.createTestPRCurveExact(data.r, data.s, data.t, anyburlCollector, "AnyBURL ");
            Data<Double, Double> anyPoints = new Data<>(anyburlPoints.getData().stream().map(p -> new Pair<>(p.r.doubleValue(), p.s.doubleValue())).collect(Collectors.toList()),
                    anyburlPoints.getName() + " " + suffix.replace("_", " "));
            anyPoints.setAttributes("anyburl");
            plots.add(anyPoints);
        }

        boolean useExtendedRanking = true;
        boolean filterForTest = true;
        String q = u.plot(plots, dataset + (filterForTest ? " (filtered corrupted)" : "") + (useExtendedRanking ? " extended ranking" : ""));
        System.out.println(q);
    }

    public static Map<String, Double> anyBurlRanking(String dataset) {
        return anyBurlRanking(dataset, "nations".equals(dataset) ? "100" : "500");
    }

    public static Map<String, Double> anyBurlRanking(String dataset, String suffix) {
//        Path path = Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-500");
//        if (dataset.equals("nations")) {
//            path = Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-100"); // there is no 500 for nations
//        }
        Path path = Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-" + suffix);

        Path base = Paths.get("..", "datasets", dataset + "-ntp-o");
        Quadruple<Set<Literal>, Set<Literal>, Set<Literal>, Boolean> data = Reformatter.loadData(dataset);
        Set<Literal> evidence = data.r;
        Set<Literal> valid = data.s;
        Set<Literal> test = data.t;
        boolean renameToIndexes = data.u;

        Set<Literal> forbidden = Sugar.union(valid, evidence, test);

        int maxConstants = forbidden.stream().flatMap(l -> l.argumentsStream()).collect(Collectors.toSet()).size();
        List<Double> hits = ranks(path, forbidden, maxConstants, renameToIndexes);
        Map<String, Double> retVal = new HashMap<>();
        retVal.put("MRR", hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size());
        retVal.put("hits1", (hits.stream().filter(val -> val <= 1.0).count() * 1.0 / hits.size()));
        retVal.put("hits3", (hits.stream().filter(val -> val <= 3.0).count() * 1.0 / hits.size()));
        retVal.put("hits10", (hits.stream().filter(val -> val <= 10.0).count() * 1.0 / hits.size()));

        retVal.put("hit10", hits.stream().filter(val -> val <= 10.0).count() * 1.0);
        retVal.put("hit100", hits.stream().filter(val -> val <= 100.0).count() * 1.0);
        retVal.put("hit500", hits.stream().filter(val -> val <= 500.0).count() * 1.0);
        retVal.put("hit1000", hits.stream().filter(val -> val <= 1000.0).count() * 1.0);
        retVal.put("hit2000", hits.stream().filter(val -> val <= 2000.0).count() * 1.0);
        retVal.put("hit5000", hits.stream().filter(val -> val <= 5000.0).count() * 1.0);
        retVal.put("hit10000", hits.stream().filter(val -> val <= 10000.0).count() * 1.0);
        retVal.put("total", test.size() * 1.0);
        return retVal;
    }


    public static Map<String, Double> getAnyBurlRanks(String dataset) {
        Path path = Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-500");
        if (dataset.equals("nations")) {
            path = Paths.get("C:\\data\\school\\development\\anyburl\\predictions\\" + dataset + "-ntp-o-100"); // there is no 500 for nations
        }

        Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "train.nl"));
        Set<Literal> valid = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "dev.nl"));
        Set<Literal> test = LogicUtils.loadEvidence(Paths.get("..", "datasets", dataset + "-ntp-o", "test.nl"));

        Set<Literal> forbidden = Sugar.union(valid, evidence, test);

        int maxConstants = forbidden.stream().flatMap(l -> l.argumentsStream()).collect(Collectors.toSet()).size();
        return getRanks(path, forbidden, maxConstants);
    }

    private static void ranksCardinalityCheck(Path path, Set<Literal> forbidden) {
        MultiMap<Literal, String> map = new MultiMap<>();
        Pair<Literal, String> state = new Pair<>(null, null); // awkward
        try {
            Files.lines(path).filter(l -> !l.trim().isEmpty())
                    .forEach(line -> {
                        if (line.startsWith("Heads:") || line.startsWith("Tails:")) {
                            String[] split = line.split("\\s+");
                            for (int idx = 1; idx < split.length; idx += 2) {

                                Literal corrupted = line.startsWith("Heads:") ?
                                        new Literal(state.getR().predicate(), Constant.construct(split[idx]), state.getR().get(1))
                                        : new Literal(state.getR().predicate(), state.getR().get(0), Constant.construct(split[idx]));
                                if (forbidden.contains(corrupted)) {
                                    continue;
                                }
                                map.put(corrupted, split[idx + 1]);
                            }
                        } else {
                            String[] split = line.split("\\s+");
                            assert split.length == 3;
                            state.r = Literal.parseLiteral(split[1] + "(" + split[0] + "," + split[2] + ")");
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
        Counters<Integer> histogram = new Counters<>();
        map.entrySet().forEach(e -> histogram.increment(e.getValue().size()));
        histogram.keySet().stream().sorted().forEach(key -> System.out.println(key + "\t" + histogram.get(key)));
    }


    // this works correctly only if all possible corrupted are present -- otherwise, MRR could be biased, but hits<=10 should be correct if at least 10 predictions are made
    private static List<Double> ranks(Path anyBurlApplyOutput, Set<Literal> forbidden, int maxConstants, boolean isRenamed) {
        List<Double> retVal = Sugar.list();
        Pair<Literal, String> state = new Pair<>(null, null); // awkward
        //Set<String> debug = Sugar.set();
//        Map<String, String> b = new HashMap<>();
        Set<String> relations = forbidden.stream().map(Literal::predicate).collect(Collectors.toSet());
        Set<String> entities = forbidden.stream().flatMap(literal -> literal.argumentsStream()).map(Term::name).collect(Collectors.toSet());
        if (isRenamed) {
            System.out.println("AnyBurlCheck:ranks: stripping first character in place!");
        }
        try {
            Files.lines(anyBurlApplyOutput).filter(l -> !l.trim().isEmpty())
                    .forEach(line -> {
                        if (line.startsWith("Heads:")) {
                            if (null != state.r) {
                                int previous = 0;
                                int filteredConstants = 0;
                                boolean targetTripleFound = false;
                                String[] split = line.split("\\s+");
                                for (int idx = 1; idx < split.length; idx += 2) {
                                    Term headEntity = Reformatter.stripFirstLettersFromNonVariables(Constant.construct(split[idx]));
                                    Literal corrupted = new Literal(state.getR().predicate(), headEntity, state.getR().get(1));
                                    // this is wrong corrupted = Reformatter.stripFirstLettersFromNonVariables(corrupted);
//                                    System.out.println(corrupted + "\t" + split[idx + 1]);
                                    if (corrupted.equals(state.getR())) {
                                        previous++;
                                        targetTripleFound = true;
                                        //debug.add(corrupted + "H\t" + previous);
                                        break;
                                    }
                                    if (forbidden.contains(corrupted)) {
//                                        System.out.println("skipping");
                                        filteredConstants++;
                                        continue;
                                    }
                                    previous++;
                                }
//                                b.put(state.r.toString(), "" + (previous * 1.0));
//                                System.out.println(state.r.toString() + " -> " + (previous * 1.0));
                                /*if (0 != previous) {
                                    retVal.add(1.0 * previous);
                                } else {
                                    retVal.add(1.0 * maxConstants / 2);
                                    //System.out.println(state.r);
                                }*/
                                if (targetTripleFound) {
                                    retVal.add(1.0 * previous);
                                } else {
                                    retVal.add(1.0 * ((maxConstants - filteredConstants) / 2) + previous); // this is not sound! when all corrupted triplets are not entailed before the target one!!!!
                                    //System.out.println(state.r);
                                }
                            }
                        } else if (line.startsWith("Tails:")) {
                            if (null != state.r) {
//                                System.out.println("tails");
                                int previous = 0;
                                int filteredConstants = 0;
                                boolean targetTripleFound = false;
                                String[] split = line.split("\\s+");
                                List<String> constants = Sugar.list();
                                for (int idx = 1; idx < split.length; idx += 2) {
                                    constants.add(split[idx + 1]);
                                    Term tailEntity = Reformatter.stripFirstLettersFromNonVariables(Constant.construct(split[idx]));
                                    Literal corrupted = new Literal(state.getR().predicate(), state.getR().get(0), tailEntity);
                                    //this is wrong corrupted = Reformatter.stripFirstLettersFromNonVariables(corrupted);
                                    //                                    System.out.println(corrupted + "\t" + split[idx + 1]);
                                    if (corrupted.equals(state.getR())) {
                                        previous++;
                                        targetTripleFound = true;
                                        //debug.add(corrupted + "L\t" + previous);
                                        break;
                                    }
                                    if (forbidden.contains(corrupted)) {
                                        filteredConstants++;
                                        continue;
                                    }
                                    previous++;


//                            if("adjacent_to(tissue, body_space_or_junction)".equals(state.r.toString())){
//                                System.out.println("end");
//                                System.exit(-1);
//                            }

                                }
                                if (targetTripleFound) {
                                    retVal.add(1.0 * previous);
                                } else {
                                    retVal.add(1.0 * ((maxConstants - filteredConstants) / 2) + previous); // this is not sound! when all corrupted triplets are not entailed before the target one!!!!
                                    //System.out.println(state.r);
                                }

//                                b.put(state.r.toString(), b.get(state.r.toString()) + "\t" + (1.0 * previous));
//                                System.out.println(state.r.toString() + " -> " + b.get(state.r.toString()));
                            }
                        } else {
                            String[] split = line.split("\\s+");
                            assert split.length == 3;
                            state.r = Reformatter.stripFirstLettersFromNonVariables(Literal.parseLiteral(split[1] + "(" + split[0] + "," + split[2] + ")"));
//                            if (!"affects(amino_acid_peptide_or_protein, biologic_function)".equals(state.r.toString())) { // debug!
//                                state.r = null;
//                            }
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }

        //debug.stream().sorted().forEach(System.out::println);
//        b.keySet().stream().sorted().forEach(l -> System.out.println(l + "\t" + b.get(l)));
        return retVal;
    }

    private static Map<String, Double> getRanks(Path anyBurlApplyOutput, Set<Literal> forbidden, int maxConstants) {
        Map<String, Double> retVal = new HashMap<>();
        Pair<Literal, String> state = new Pair<>(null, null); // awkward
        try {
            Files.lines(anyBurlApplyOutput).filter(l -> !l.trim().isEmpty())
                    .forEach(line -> {
                        if (line.startsWith("Heads:")) {
                            if (null != state.r) {
                                int previous = 0;
                                String[] split = line.split("\\s+");
                                for (int idx = 1; idx < split.length; idx += 2) {
                                    Literal corrupted = new Literal(state.getR().predicate(), Constant.construct(split[idx]), state.getR().get(1));
//                                    System.out.println(corrupted + "\t" + split[idx + 1]);
                                    if (corrupted.equals(state.getR())) {
                                        previous++;
                                        //debug.add(corrupted + "H\t" + previous);
                                        break;
                                    }
                                    if (forbidden.contains(corrupted)) {
//                                        System.out.println("skipping");
                                        continue;
                                    }
                                    previous++;
                                }
//                                b.put(state.r.toString(), "" + (previous * 1.0));
//                                System.out.println(state.r.toString() + " -> " + (previous * 1.0));
                                if (0 != previous) {
                                    //retVal.add(1.0 * previous);
                                    retVal.put(state.r + "-h", 1.0 * previous);
                                } else {
//                                    retVal.add(1.0 * maxConstants / 2);
                                    retVal.put(state.r + "-h", 1.0 * maxConstants / 2);
                                    //System.out.println(state.r);
                                }

                            }
                        } else if (line.startsWith("Tails:")) {
                            if (null != state.r) {
//                                System.out.println("tails");
                                int previous = 0;
                                String[] split = line.split("\\s+");
                                List<String> constants = Sugar.list();
                                for (int idx = 1; idx < split.length; idx += 2) {
                                    constants.add(split[idx + 1]);
                                    Literal corrupted = new Literal(state.getR().predicate(), state.getR().get(0), Constant.construct(split[idx]));
//                                    System.out.println(corrupted + "\t" + split[idx + 1]);
                                    if (corrupted.equals(state.getR())) {
                                        previous++;
                                        //debug.add(corrupted + "L\t" + previous);
                                        break;
                                    }
                                    if (forbidden.contains(corrupted)) {
                                        continue;
                                    }
                                    previous++;


//                            if("adjacent_to(tissue, body_space_or_junction)".equals(state.r.toString())){
//                                System.out.println("end");
//                                System.exit(-1);
//                            }

                                }
                                if (0 != previous) {
                                    //retVal.add(1.0 * previous);
                                    retVal.put(state.r + "-t", 1.0 * previous);
                                } else {
                                    //retVal.add(1.0 * maxConstants / 2);
                                    retVal.put(state.r + "-t", 1.0 * maxConstants / 2);
                                    //System.out.println(state.r);
                                }

//                                b.put(state.r.toString(), b.get(state.r.toString()) + "\t" + (1.0 * previous));
//                                System.out.println(state.r.toString() + " -> " + b.get(state.r.toString()));
                            }
                        } else {
                            String[] split = line.split("\\s+");
                            assert split.length == 3;
                            state.r = Literal.parseLiteral(split[1] + "(" + split[0] + "," + split[2] + ")");
//                            if (!"affects(amino_acid_peptide_or_protein, biologic_function)".equals(state.r.toString())) { // debug!
//                                state.r = null;
//                            }
                        }
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }

        //debug.stream().sorted().forEach(System.out::println);
//        b.keySet().stream().sorted().forEach(l -> System.out.println(l + "\t" + b.get(l)));
        return retVal;
    }


    private static void storeToCollector(Path anyBurlApplyOutput, Path s) {
        Map<Literal, Double> values = new HashMap<>();
        try {
            String debugL = "term21(person84, person85)";
//            String debugL="";
            Pair<Literal, String> state = new Pair<>(null, null); // awkward
            Files.lines(anyBurlApplyOutput).filter(l -> !l.trim().isEmpty())
                    .forEach(line -> {
                        if (line.startsWith("Heads:")) {
                            String[] split = line.split("\\s+");
                            for (int idx = 1; idx < split.length; idx += 2) {
                                Constant constant = Constant.construct(split[idx]);
                                Literal l = new Literal(state.getR().predicate(), constant, state.getR().get(1));
                                double value = Double.parseDouble(split[idx + 1]);

                                if (l.toString().equals(debugL)) {
                                    System.out.println("adding in head\n" + state.r + "\n" + l + "\n" + value + "\n" + line);
                                    throw new IllegalStateException();
                                }
                                if (values.containsKey(l) && Double.compare(value, values.get(l)) != 0) {
                                    System.out.println("problem here\t" + l + "\t" + values.get(l) + "\t" + value + "\n"
                                            + state.r + "\n"
                                            + l + "\n"
                                            + "head" + "\n" + line);
                                    throw new IllegalStateException();
                                }
                                values.put(l, value);
                            }
                        } else if (line.startsWith("Tails:")) {
                            String[] split = line.split("\\s+");
                            for (int idx = 1; idx < split.length; idx += 2) {
                                Constant constant = Constant.construct(split[idx]);
                                Literal l = new Literal(state.getR().predicate(), state.getR().get(0), constant);
                                double value = Double.parseDouble(split[idx + 1]);
                                if (l.toString().equals(debugL)) {
                                    System.out.println("adding in tail\n" + state.r + "\n" + l + "\n" + value + "\n" + line);
                                    throw new IllegalStateException();
                                }
                                if (values.containsKey(l) && Double.compare(value, values.get(l)) != 0) {
                                    System.out.println("problem here\t" + l + "\t" + values.get(l) + "\t" + value + "\n"
                                            + state.r + "\n"
                                            + l + "\n"
                                            + "tail" + "\n" + line);
                                    throw new IllegalStateException();
                                }
                                values.put(l, value);
                            }
                        } else {
                            String[] split = line.split("\\s+");
                            assert split.length == 3;
                            state.r = Literal.parseLiteral(split[1] + "(" + split[0] + "," + split[2] + ")");
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        VECollector collector = VECollector.create(Sugar.list(), 1);
        for (Map.Entry<Literal, Double> entry : values.entrySet()) {
            collector.getVotes().add(entry.getKey(), entry.getValue());
        }

        try {
            Files.write(s, Sugar.list(collector.asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    // infers anyburl rules on inductive setting :))
    public static void inferAnyBurl() throws IOException {
        //String dataset = "kinships";
        //String dataset = "nationsA2";
        String dataset = "umls";
         int rulesTime = 500; // for kinships & umls
//        int rulesTime = 100; // for nations
        String queries = "test-uniform";
        String queriesShortcut = "test-uniform".equals(queries) ? "tu" : null;
        Path baseAnyBurl = Paths.get("C:\\data\\school\\development\\anyburl");
        Path rules = Paths.get(baseAnyBurl.toString(), "rules", dataset + "-" + rulesTime);
        Path apply = Paths.get(baseAnyBurl.toString(), "run-apply.properties");

        Path targetEvidence = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", dataset, "test.db");
        Path queriesDir = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", dataset, queries);
        Path tmp = Paths.get(baseAnyBurl.toString(), "tmp");
        if (!tmp.toFile().exists()) {
            tmp.toFile().mkdirs();
        }
        Path output = Paths.get(tmp.toString(), "out.predictions");
        Path queriesEvidence = Paths.get(tmp.toString(), "evidence.nl");


        Set<Literal> testEvidence = LogicUtils.loadEvidence(targetEvidence);
        int entities = LogicUtils.constants(testEvidence).size();
        Path test = Paths.get(tmp.toString(), "test.nl");
        Files.write(test, testEvidence.stream()
                .map(literal -> literal.get(0) + "\t" + literal.predicate() + "\t" + literal.get(1)) // this should be in reformatter!
                .collect(Collectors.toList()));

        Path empty = Paths.get(tmp.toString(), "empty.nl");
        Files.write(empty, Sugar.list());
        int atMost = 2000;
        int step = 10;
        Path baseOutput = Paths.get("E:\\dev\\pac-executer", "anyburl", dataset, queriesShortcut + "_" + rulesTime);
        if (!baseOutput.toFile().exists()) {
            baseOutput.toFile().mkdirs();
        }
        try {
            Files.list(queriesDir).filter(file -> file.toString().endsWith(".db"))
                    .map(file -> {
                        String size = file.toFile().getName().substring("queries".length(), file.toFile().getName().length() - 3);
                        int len = 0;
                        try {
                            len = Integer.parseInt(size);
                        } catch (Exception e) {
                            return null;
                        }
                        return new Pair<>(len, file);
                    }).filter(Objects::nonNull)
                    .sorted(Comparator.comparingInt(pair -> pair.r))
                    .filter(pair -> pair.r % step == 0 && pair.r <= atMost)
                    .forEach(pair -> {
                        System.out.println("executing\t" + pair.s);
                        Set<Literal> queryEvidence = LogicUtils.loadEvidence(pair.getS());
                        try {
                            Files.write(queriesEvidence, queryEvidence.stream()
                                    .map(literal -> literal.get(0) + "\t" + literal.predicate() + "\t" + literal.get(1))
                                    .collect(Collectors.toList()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        List<String> properties = Sugar.list(("PATH_TRAINING  = " + queriesEvidence + "\n" +
                                "PATH_TEST      = " + test + "\n" +
                                "PATH_VALID     = " + empty + "\n" +
                                "PATH_RULES     = " + rules + "\n" +
                                "PATH_OUTPUT    = " + output + "\n" +
                                "UNSEEN_NEGATIVE_EXAMPLES = 5\n" +
                                "TOP_K_OUTPUT = " + entities + "\n" +
                                "WORKER_THREADS = 7").replace("\\", "/"));
                        try {
                            Files.write(apply, properties);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        try {
//                            Process r = Runtime.getRuntime().exec("java -cp C:\\data\\school\\development\\anyburl\\AnyBURL-RE.jar  de.unima.ki.anyburl.Apply " + apply);
                            String cmd = "java -cp C:\\data\\school\\development\\anyburl\\AnyBURL-RE.jar  de.unima.ki.anyburl.Apply " + apply;
                            Runtime run = Runtime.getRuntime();
                            Process r = run.exec(cmd);
                            try {
                                r.waitFor();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            BufferedReader buf = new BufferedReader(new InputStreamReader(r.getInputStream()));
                            String line = "";
                            while ((line = buf.readLine()) != null) {
                                System.out.println(line);
                            }
                            buf = new BufferedReader(new InputStreamReader(r.getErrorStream()));
                            line = "";
                            while ((line = buf.readLine()) != null) {
                                System.out.println(line);
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        Path currentPredictionOut = Paths.get(baseOutput.toString(), pair.s.getFileName().toString());
                        transferToEntailed(output, queryEvidence, currentPredictionOut, false, queryEvidence);
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
