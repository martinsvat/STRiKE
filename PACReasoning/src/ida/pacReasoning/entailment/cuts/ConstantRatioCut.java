package ida.pacReasoning.entailment.cuts;

import ida.ilp.logic.Literal;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.Outputable;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.utils.Sugar;
import ida.utils.collections.DoubleCounters;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 17. 7. 2018.
 */
public class ConstantRatioCut implements Cut, Outputable {


    private final String name;
    private final Map<Pair<String, Integer>, List<Pair<Integer, Double>>> ratios; // each predicate has a ratio for a constant size (which is sorted for half-split searching the nearest one
    private final int k;

    public ConstantRatioCut(Map<Pair<String, Integer>, List<Pair<Integer, Double>>> ratios, int k, String name) {
        this.ratios = ratios;
        this.k = k;
        this.name = name;
    }

    @Override
    public boolean isEntailed(Literal literal, double votes, long constants) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEntailed(Predicate predicate, int a, double votes, long constants) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEntailed(String predicate, int arity, int a, double votes, long constants) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VECollector entailed(VECollector data) {
        Set<Pair<Literal, Double>> entailedLiterals = Sugar.parallelStream(data.getVotes().entrySet())
                .collect(Collectors.groupingBy(entry -> entry.getKey().getPredicate()))
                .entrySet().stream().parallel()
                .flatMap(group -> {
                    Pair<String, Integer> predicate = group.getKey();
                    List<Map.Entry<Literal, Double>> values = group.getValue();
                    return findEntailed(predicate, values, data);
                })
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());

        return VECollector.create(data.getEvidence()
                , DoubleCounters.createCounters(entailedLiterals)
                , data.getK()
                , data.constantsSize()
                , data.getConstants()
                , data.originalEvidenceSize());
    }


    private double gammaValue(Literal literal, Double votes, VECollector collector) {
        return votes / collector.getCToX(literal.terms().size());
    }

    // all the literals in values are composed of the predicate (no check is done inside)
    private Stream<Map.Entry<Literal, Double>> findEntailed(Pair<String, Integer> predicate, List<Map.Entry<Literal, Double>> values, VECollector collector) {
        double gamma = Double.POSITIVE_INFINITY;
        long entailed = 0l;

        List<Pair<Integer, Double>> ratios = this.ratios.get(predicate);
        if (null == ratios || ratios.isEmpty()) {
            return Stream.empty();
        }

        // firstly find the closest ratio w.r.t. # constants
        // this can be implemented by modification of binary search instead
        int closestValue = Integer.MAX_VALUE;
        double ratio = 0.0d;
        for (Pair<Integer, Double> pair : ratios) {
            int distance = Math.abs(pair.r - collector.constantsSize());
            if (distance < closestValue) {
                closestValue = distance;
                ratio = pair.s;
            } else {
                break; // getting further from the local minimum
            }
        }

        // compute how many literals of the given predicate are entailed by votes
        long entailedLiterals = Sugar.parallelStream(collector.getVotes().keySet())
                .filter(literal -> literal.getPredicate().equals(predicate))
                .count();
        Double idealNoTruePositive = entailedLiterals * ratio; // ideal number of entailed literals

        List<Map.Entry<Double, Long>> sorted = Sugar.parallelStream(values)
                .collect(Collectors.groupingByConcurrent(entry -> gammaValue(entry.getKey(), entry.getValue(), collector), Collectors.counting()))
                .entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .collect(Collectors.toList());

        long previouslyEntailed = 0l; // it does not entail anything

        // find gamma for which TP is closest to the idealNoTruePositive
        // it can by implemented by some kind of half-splitting
        for (int idx = sorted.size() - 1; idx >= 0; idx--) {
            Map.Entry<Double, Long> entry = sorted.get(idx);
            entailed += entry.getValue();

            // if you want to make some other kind of optimistic/pessimistic decision here, take either nearest above or nearest below... the current implementation takes the realistic (the closest one)

            System.out.println("nemela by tady ta nerovnost byt obracene?, asi jo, asi by tam melo byt > kdyz se ten rozdil ma zmensovat :))");
            if (Math.abs(idealNoTruePositive - previouslyEntailed) < Math.abs(idealNoTruePositive - entailed)) {
                previouslyEntailed = entailed;
                gamma = entry.getKey();
            } else {
                break; // the array is sorted
            }
        }

        double bestGamma = gamma;
        GammasCut gcut = GammasCut.create(bestGamma, this.k); // why to reimplement all of that here ;)
        return Sugar.parallelStream(values)
                .filter(e -> gcut.isEntailed(e.getKey(), e.getValue(), collector.constantsSize()));
    }

    @Override
    public Cut cut(int evidenceSize) {
        return this;
    }

    @Override
    public Set<Predicate> predicates() {
        return Sugar.parallelStream(ratios.keySet())
                .map(Predicate::create)
                .collect(Collectors.toSet());
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String asOutput() {
        return "k:\t" + k
                + "\n"
                + ratios.entrySet().stream()
                .map(entry -> Predicate.create(entry.getKey()).toString()
                        + "\n"
                        + entry.getValue().stream()
                        .map(pair -> pair.r + "\t" + pair.s)
                        .collect(Collectors.joining("\n")))
                .collect(Collectors.joining("\n"));
    }

    public static ConstantRatioCut load(Path path) {
        try {
            return load(Files.lines(path).collect(Collectors.toList()), path.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("file could not be read:\t" + path);
    }

    public static ConstantRatioCut load(List<String> content, String name) {
        return load(content, name, null);
    }

    public static ConstantRatioCut load(List<String> content, String name, Integer k) {
        Triple<Integer, Predicate, List<Pair<Integer, Double>>> cache = new Triple<>(k, null, null);
        Map<Pair<String, Integer>, List<Pair<Integer, Double>>> ratios = new HashMap<>();
        content.stream()
                .filter(line -> !line.isEmpty() && !line.trim().startsWith("#"))
                .forEach(line -> {
                    if (line.startsWith("k:")) {
                        cache.r = Integer.parseInt(line.substring("k:".length()).trim());
                    } else if (line.contains("/")) {
                        if (null != cache.s) {
                            ratios.put(cache.s.getPair(), cache.t);
                        }
                        cache.s = Predicate.construct(line);
                        cache.t = Sugar.list();
                    } else {
                        String[] splitted = line.split("\t");
                        assert splitted.length == 2;
                        cache.t.add(new Pair<>(
                                Integer.parseInt(splitted[0]),
                                Double.parseDouble(splitted[1])
                        ));
                    }
                });

        if (null != cache.s) {
            ratios.put(cache.s.getPair(), cache.t);
        }
        return create(ratios, cache.r, name);
    }

    public static ConstantRatioCut create(Map<Pair<String, Integer>, List<Pair<Integer, Double>>> ratios, int k) {
        return create(ratios, k, "");
    }

    public static ConstantRatioCut create(Map<Pair<String, Integer>, List<Pair<Integer, Double>>> ratios, int k, String name) {
        ratios.values().forEach(list -> Collections.sort(list, Comparator.comparing(Pair::getR)));
        return new ConstantRatioCut(ratios, k, name);
    }
}
