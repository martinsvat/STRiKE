package ida.pacReasoning.entailment.cuts;

import ida.ilp.logic.Literal;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.Outputable;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.utils.Sugar;
import ida.utils.collections.DoubleCounters;
import ida.utils.tuples.Pair;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 17. 7. 2018.
 */
public class AdaptiveRatioCut implements Cut, Outputable {
    private final String name;
    private final Map<Pair<String, Integer>, Double> ratios;
    private final int k;

    public AdaptiveRatioCut(Map<Pair<String, Integer>, Double> ratios, int k, String name) {
        this.ratios = ratios;
        this.name = name;
        this.k = k;
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
    public VECollector entailed(VECollector collector) {
        Set<Pair<Literal, Double>> entailedLiterals = Sugar.parallelStream(collector.getVotes().entrySet(), true)
                .collect(Collectors.groupingBy(entry -> entry.getKey().getPredicate()))
                .entrySet().stream().parallel()
                .flatMap(group -> {
                    Pair<String, Integer> predicate = group.getKey();
                    List<Map.Entry<Literal, Double>> values = group.getValue();
                    return findEntailed(predicate, values, collector);
                })
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());


        return VECollector.create(collector.getEvidence()
                , DoubleCounters.createCounters(entailedLiterals)
                , collector.getK()
                , collector.constantsSize()
                , collector.getConstants()
                , collector.originalEvidenceSize());
    }

    private double gammaValue(Literal literal, Double votes, VECollector collector) {
        return votes / collector.getCToX(literal.terms().size());
    }

    private Stream<Map.Entry<Literal, Double>> findEntailed(Pair<String, Integer> predicate, List<Map.Entry<Literal, Double>> values, VECollector collector) {
        double gamma = Double.POSITIVE_INFINITY;
        double previousThreshold = 0.0; // it does not entail anything
        long entailed = 0l;

        Double ratio = this.ratios.get(predicate);
        if (null == ratio) {
            return Stream.empty();
        }
        int evidenceCardinality = collector.originalEvidenceSize();
        Double goldenThreshold = evidenceCardinality * ratio; // ideal number of entailed literals

        List<Map.Entry<Double, Long>> sorted = Sugar.parallelStream(values)
                .collect(Collectors.groupingByConcurrent(entry -> gammaValue(entry.getKey(), entry.getValue(), collector), Collectors.counting()))
                .entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .collect(Collectors.toList());

        // it can by implemented by some kind of half-splitting
        for (int idx = sorted.size() - 1; idx >= 0; idx--) {
            Map.Entry<Double, Long> entry = sorted.get(idx);
            entailed += entry.getValue();

            // if you want to make some other kind of optimistic/pessimistic decision here, take either nearest above or nearest below... the current implementation takes the realistic (the closest one)

            if (Math.abs(goldenThreshold - previousThreshold) < Math.abs(goldenThreshold - entailed)) {
                previousThreshold = entailed;
                gamma = entry.getKey();
            } else {
                break;
            }
        }

        double bestGamma = gamma;
        throw new IllegalStateException();// NotImplementedException();
        //return Sugar.parallelStream(values)
        //        .filter(e -> e.getValue() >= bestGamma); chyba, nemuzeme porovnavat hrusky s jablkama (hlasy s gamou)

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


    public String asOutput() {
        return "k:\t" + k + "\n"
                + this.ratios.entrySet().stream()
                .map(entry -> entry.getKey().r + "/" + entry.getKey().s + "\t" + entry.getValue())
                .sorted()
                .collect(Collectors.joining("\n"));
    }


    public static Pair<Pair<String, Integer>, Double> parseLine(String line) {
        String[] splitted = line.split("\t");
        if (splitted.length != 2) {
            System.out.println("cannot parse line:\t" + line);
            throw new IllegalArgumentException();
        }
        String[] predicateSplitted = splitted[0].split("/");
        if (predicateSplitted.length != 2) {
            System.out.println("cannot parse predicate:\t" + splitted[0]);
            throw new IllegalArgumentException();
        }
        Pair<String, Integer> predicate = new Pair<>(predicateSplitted[0], Integer.parseInt(predicateSplitted[1]));
        return new Pair<>(predicate, Double.parseDouble(splitted[1]));
    }

    public static AdaptiveRatioCut load(Path path) {
        Map<Pair<String, Integer>, Double> ratios = null;
        Pair<Integer, Object> k = new Pair<>();
        try {
            ratios = Files.lines(path)
                    .parallel()
                    .filter(line -> line.trim().length() > 0 && !line.startsWith("#"))
                    .map(line -> {
                        if (line.startsWith("k:")) {
                            k.r = Integer.parseInt(line.split("\t")[1]);
                            return null;
                        }
                        String[] splitted = line.split("\t");
                        if (splitted.length != 2) {
                            System.out.println("cannot parse line:\t" + line);
                            throw new IllegalArgumentException();
                        }
                        return new Pair<>(Predicate.construct(splitted[0]).getPair(), Double.parseDouble(splitted[1]));
                    })
                    .filter(p -> null != p)
                    .collect(Collectors.toMap(Pair::getR, Pair::getS));
        } catch (IOException e) {
            e.printStackTrace();
        }

        return create(ratios, k.r, path.toString());
    }

    public static AdaptiveRatioCut create(Double ratio, int k) {
        Map<Pair<String, Integer>, Double> map = new Map<Pair<String, Integer>, Double>() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean containsKey(Object key) {
                return true;
            }

            @Override
            public boolean containsValue(Object value) {
                return false;
            }

            @Override
            public Double get(Object key) {
                return ratio;
            }

            @Override
            public Double put(Pair<String, Integer> key, Double value) {
                return null;
            }

            @Override
            public Double remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends Pair<String, Integer>, ? extends Double> m) {

            }

            @Override
            public void clear() {

            }

            @Override
            public Set<Pair<String, Integer>> keySet() {
                return null;
            }

            @Override
            public Collection<Double> values() {
                return null;
            }

            @Override
            public Set<Entry<Pair<String, Integer>, Double>> entrySet() {
                return null;
            }
        };
        return create(map, k, "");
    }

    public static AdaptiveRatioCut create(Map<Pair<String, Integer>, Double> ratios, int k) {
        return create(ratios, k, "");
    }

    public static AdaptiveRatioCut create(Map<Pair<String, Integer>, Double> ratios, int k, String name) {
        return new AdaptiveRatioCut(ratios, k, name);
    }

    public static AdaptiveRatioCut create(Predicate predicate, Double ratio, int k) {
        return create(predicate, ratio, k, "");
    }

    public static AdaptiveRatioCut create(Predicate predicate, Double ratio, int k, String name) {
        Map<Pair<String, Integer>, Double> map = new HashMap<>();
        map.put(predicate.getPair(), ratio);
        return create(map, k, name);
    }
}
