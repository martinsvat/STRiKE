package ida.gnns;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.utils.Sugar;
import ida.utils.collections.MultiList;
import ida.utils.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 26. 3. 2021.
 */
public class ForwardReranker {

    public static void main(String[] args) throws IOException {
        Path path = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hyForwardDown\\kinships-ntp-o\\anyburl");
        rerank(path);
    }

    private static void rerank(Path path) throws IOException {
        MultiList<String, Pair<Integer, Path>> collected = new MultiList<>();
        Files.list(path).filter(p -> p.toString().endsWith(".entailed"))
                .forEach(p -> {
                    // complex_rules500.all_3_10.entailed
                    String[] split = p.getFileName().toString().split("_");
                    String embedding = split[0];
                    String rules = split[1];
                    int iter = Integer.parseInt(split[2]);
                    String steps = split[3].substring(0, split[3].length() - ".entailed".length());
                    String unify = embedding + "_" + rules + "_" + steps;
                    collected.put(unify, new Pair<>(iter, p));
                });


        collected.entrySet().forEach(e -> rerank(path, e.getKey(), e.getValue()));
    }

    private static void rerank(Path path, String key, List<Pair<Integer, Path>> value) {
        VECollector collector = VECollector.create(Sugar.list(), 0);
        List<Pair<Integer, Path>> order = value.stream().sorted(Comparator.comparing(Pair::getR)).collect(Collectors.toList());
        Integer min = order.get(0).getR();
        Collections.reverse(order);
        int substract = 0;
        for (Pair<Integer, Path> pair : order) {
            System.out.println("loading\t" + pair.s);
            VECollector current = VECollector.load(pair.getS(), 0);
            collector.getEvidence().addAll(current.getEvidence());
            for (Map.Entry<Literal, Double> entry : current.getVotes().entrySet()) {
                if (!collector.getVotes().keySet().contains(entry.getKey())) {
                    collector.getVotes().add(entry.getKey(), entry.getValue() - substract);
                }
            }
            substract += 100; // that should be enough
        }

        Path out = Paths.get(path.toString(), key + "_m" + min + ".entailedT");
        System.out.println("storing to\t" + out);
        try {
            Files.write(out, Sugar.list(collector.asOutput()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
