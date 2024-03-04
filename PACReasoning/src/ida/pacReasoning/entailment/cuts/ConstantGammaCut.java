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

/**
 * Created by martin.svatos on 19. 7. 2018.
 */
public class ConstantGammaCut implements Cut, Outputable {
    private final String name;
    private final Map<Integer, GammasCut> gammasGivenConstantSize;
    private final Integer k;

    public ConstantGammaCut(Map<Integer, GammasCut> gammasGivenConstantSize, Integer k, String name) {
        this.gammasGivenConstantSize = gammasGivenConstantSize;
        this.k = k;
        this.name = name;
    }

    @Override
    public String asOutput() {
        return "k:\t" + k
                + "\n" +
                Sugar.parallelStream(gammasGivenConstantSize.entrySet())
                        .sorted(Comparator.comparingInt(Map.Entry::getKey))
                        .map(entry -> "constants:\t" + entry.getKey()
                                + "\n"
                                + entry.getValue().asOutput())
                        .collect(Collectors.joining("\n"));
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
        GammasCut cut = nearestCut(data.constantsSize());

        Set<Pair<Literal, Double>> entailedLiterals = Sugar.parallelStream(data.getVotes().entrySet())
                .filter(entry -> cut.isEntailed(entry.getKey(), entry.getValue(), data.constantsSize()))
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                .collect(Collectors.toSet());

        return VECollector.create(data.getEvidence()
                , DoubleCounters.createCounters(entailedLiterals)
                , data.getK()
                , data.constantsSize()
                , data.getConstants()
                , data.originalEvidenceSize());

    }

    private GammasCut nearestCut(int constantsSize) {
        if (!gammasGivenConstantSize.containsKey(constantsSize)) {
            return Sugar.parallelStream(gammasGivenConstantSize.entrySet())
                    .map(entry -> new Pair<>(Math.abs(constantsSize - entry.getKey()), entry.getValue()))
                    .min(Comparator.comparingInt(Pair::getR))
                    .map(Pair::getS)
                    .orElse(GammasCut.ALL_BANNED);
        }
        return gammasGivenConstantSize.get(constantsSize);
    }

    @Override
    public Cut cut(int evidenceSize) {
        return this;
    }

    @Override
    public Set<Predicate> predicates() {
        return Sugar.parallelStream(gammasGivenConstantSize.values())
                .flatMap(gammaCut -> gammaCut.predicates().stream())
                .collect(Collectors.toSet());
    }

    @Override
    public String name() {
        return this.name;
    }

    // it is k-free model, but k needs to be added in order to do inference
    public static ConstantGammaCut load(Path path, int k) {
        try {
            return load(Files.lines(path).collect(Collectors.toList()), path.toString(), k);
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("file could not be read:\t" + path);
    }

    public static ConstantGammaCut load(List<String> content, String name) {
        return load(content, name, null);
    }

    public static ConstantGammaCut load(List<String> content, String name, Integer k) {
        Triple<Integer, Integer, List<String>> cache = new Triple<>(k, null, null);
        Map<Integer, GammasCut> gammas = new HashMap<>();
        content.stream()
                .filter(line -> !line.isEmpty() && !line.trim().startsWith("#"))
                .forEach(line -> {
                    if (line.startsWith("k:")) {
                        cache.r = Integer.parseInt(line.substring("k:".length()).trim());
                    } else if (line.startsWith("constants:")) {
                        String[] splitted = line.split("\t");
                        assert 2 == splitted.length;
                        if (null != cache.s && null != cache.t) {
                            gammas.put(cache.s, GammasCut.load(cache.t, cache.r, "constants: " + cache.s));
                        }
                        cache.s = Integer.parseInt(splitted[1]);
                        cache.t = Sugar.list();
                    } else {
                        cache.t.add(line);
                    }
                });

        if (null != cache.s && null != cache.t) {
            gammas.put(cache.s, GammasCut.load(cache.t, cache.r, "constants: " + cache.s));
        }
        return create(gammas, cache.r, name);
    }

    public static ConstantGammaCut create(int constant, Predicate predicate, double gamma, Integer k, String name) {
        return create(constant, GammasCut.create(predicate, gamma, k), k, name);
    }

    public static ConstantGammaCut create(int constant, GammasCut gammaCut, Integer k, String name) {
        Map<Integer, GammasCut> ratios = new HashMap<>();
        ratios.put(constant, gammaCut);
        return create(ratios, k, name);
    }

    public static ConstantGammaCut create(Map<Integer, GammasCut> gammasGivenConstantSize, Integer k, String name) {
        return new ConstantGammaCut(gammasGivenConstantSize, k, name);
    }
}
