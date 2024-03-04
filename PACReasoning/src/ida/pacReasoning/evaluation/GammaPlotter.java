package ida.pacReasoning.evaluation;

import ida.ilp.logic.Predicate;
import ida.pacReasoning.entailment.cuts.Cut;
import ida.pacReasoning.entailment.cuts.GammasCut;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;


import java.nio.file.Path;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 30. 6. 2018.
 */
public class GammaPlotter {
    public static final GammaPlotter EMPTY_ONE = load(Sugar.list());

    private final List<Pair<Cut, List<String>>> gammas;
    private final boolean allPredicates;

    public GammaPlotter(List<Pair<Cut, List<String>>> gammas, boolean allPredicates) {
        this.allPredicates = allPredicates;
        this.gammas = gammas;
    }

    private <T extends Number> boolean any(List<String> keyWords, List<Data<T, T>> plots) {
        if (keyWords.isEmpty()) {
            return true;
        }
        for (Data<T, T> plot : plots) {
            if (keyWords.stream().allMatch(key -> plot.getName().contains(key))) {
                return true;
            }
        }
        return false;
    }

    private boolean any(List<String> keyWords, String name) {
        if (keyWords.isEmpty()) {
            return true;
        }
        for (String keyWord : keyWords) {
            if (!name.contains(keyWord)) {
                return false;
            }
        }
        return true;
    }

    public <T extends Number> List<Data<Double, Double>> gammas(Data<T, T> plot, String name) {
        return gammas(Sugar.list(plot), name);
    }

    public <T extends Number> List<Data<Double, Double>> gammas(List<Data<T, T>> plots, String name) {
        List<Pair<Cut, List<String>>> selected = gammas.stream()
                .filter(pair -> any(pair.s, name))
                .collect(Collectors.toList());
        //System.out.println("selected\t"  + selected.size());
        return gammas(plots, selected);
    }


    public <T extends Number> List<Data<Double, Double>> gammas(List<Data<T, T>> plots) {
        List<Pair<Cut, List<String>>> selected = gammas.stream()
                .filter(pair -> any(pair.s, plots))
                .collect(Collectors.toList());
        return gammas(plots, selected);
    }

    private <T extends Number> List<Data<Double, Double>> gammas(List<Data<T, T>> plots, List<Pair<Cut, List<String>>> selected) {
        Pair<Double, Double> boundaries = plots.stream()
                .flatMap(plot -> plot.getData().stream())
                .map(pair -> new Pair<>(pair.s.doubleValue(), pair.s.doubleValue()))
                .reduce((p1, p2) -> new Pair<>(
                        Math.min(p1.r, p2.r),
                        Math.max(p1.s, p2.s)))
                .orElse(new Pair<>(0.0, 0.0));

        return selected.stream()
                .flatMap(pair -> {
                    Cut cut = pair.r;
                    if (cut instanceof GammasCut) {
                        return gammaCut((GammasCut) cut, plots, boundaries.r, boundaries.s);
                    }
                    // no other than gammas cut is implemented
                    throw new IllegalStateException();// NotImplementedException();
                })
                .collect(Collectors.toList());
    }

    private <T extends Number> Stream<Data<Double, Double>> gammaCut(GammasCut cut, List<Data<T, T>> plots, Double yMin, Double yMax) {
        return cut.getGammas()
                .entrySet()
                .stream()
                .filter(entry -> display(entry.getKey(), plots))
                .map(entry -> {
                    Pair<String, Integer> predicate = entry.getKey();
                    double x = entry.getValue();
                    List<Pair<Double, Double>> points = Sugar.list(new Pair<>(x, yMin),
                            new Pair<>(x, yMax));
                    return new Data<>(points, predicate.r + "/" + predicate.s + " " + cut.name());
                });
    }


    private <T extends Number> boolean display(Pair<String, Integer> predicate, List<Data<T, T>> plots) {
        if (allPredicates) {
            return true;
        }
        String pred = new Predicate(predicate.r, predicate.s).toString();
        for (Data<T, T> plot : plots) {
            if (plot.getName().contains(pred)) {
                return true;
            }
        }
        return false;
    }

    public static GammaPlotter load(List<Triple<Path, Function<Path, Cut>, List<String>>> gammas) {
        return load(gammas, false);
    }

    public static GammaPlotter load(List<Triple<Path, Function<Path, Cut>, List<String>>> gammas, boolean allPredicates) {
        List<Pair<Cut, List<String>>> loaded = gammas.stream()
                .map(triple -> new Pair<>(triple.s.apply(triple.r), triple.t))
                .collect(Collectors.toList());
        return create(loaded, allPredicates);
    }

    public static GammaPlotter create(List<Pair<Cut, List<String>>> cutsWithKeywords, boolean allPredicates) {
        return new GammaPlotter(cutsWithKeywords, allPredicates);
    }

}
