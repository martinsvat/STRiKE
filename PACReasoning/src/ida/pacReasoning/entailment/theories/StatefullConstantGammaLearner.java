package ida.pacReasoning.entailment.theories;

import ida.ilp.logic.Literal;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.entailment.EntailmentSetting;
import ida.pacReasoning.entailment.Inference;
import ida.pacReasoning.evaluation.Utils;
import ida.pacReasoning.entailment.collectors.Entailed;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.entailment.cuts.ConstantGammaCut;
import ida.pacReasoning.entailment.cuts.GammasCut;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 19. 7. 2018.
 */
public class StatefullConstantGammaLearner {

    public static final int PLAIN = 1;
    public static final int EMPIRIC = 2;

    private final String partialEnding = ".partialResult.";
    private final String finalEnding = "result.";
    private final String dbEnding = ".db";
    private final EntailmentSetting setting;
    private final Utils utils = Utils.create();
    private final int mode;

    public StatefullConstantGammaLearner(EntailmentSetting setting) {
        this.setting = setting;
        this.mode = (null == setting) ? PLAIN : EMPIRIC;

        if (mode == PLAIN) {
            throw new IllegalStateException();// NotImplementedException();
        }
    }

    /**
     * loads if already computed; learn, stores and returns otherwise
     *
     * @param path
     * @param ratios
     * @return
     */
    private GammasCut learnFromConstantFolder(Path path, int k, Theory theory, Map<Predicate, Double> ratios) {
        try {
            if (EMPIRIC == this.mode) {
                runInferenceIfNeeded(path, k, theory);
            }
            partialResultsToResults(path, k, ratios, theory);
            return GammasCut.load(finalResultsFile(path, k, theory));
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException("all files has to be processed:\t" + path);
        }
    }

    private Path finalResultsFile(Path path, int k, Theory theory) {
        return Paths.get(path.toString(), finalEnding + k + "_" + mode + "." + (null == theory ? "null" : theory.getName()) + setting.canon());
    }

    private Map<Pair<String, Integer>, Double> loadFinalResult(Path path) throws IOException {
        return Files.lines(path)
                .filter(line -> line.trim().length() > 0 && !line.startsWith("#"))
                .map(line -> {
                    String[] splitted = line.split("\t");
                    assert 2 == splitted.length;
                    return new Pair<>(Predicate.construct(splitted[0]).getPair(), Double.parseDouble(splitted[1]));
                })
                .collect(Collectors.toMap(Pair::getR, Pair::getS));
    }

    private void runInferenceIfNeeded(Path path, int k, Theory theory) throws IOException {
        Files.list(path)
                .filter(file -> file.toFile().getName().endsWith(dbEnding))
                .filter(file -> {
                    //File partial = new File(file.toString() + partialEnding + k);
                    Path partialResultsFile = partialResult(file, k, theory, setting);
                    return !partialResultsFile.toFile().exists();
                })
                .forEach(file -> {
                    try {
                        System.out.println("inference on\t" + path);
//                        System.out.println("tady pridat postupne ukladani inference po vrstvach aby to bylo rychlejsi :))... nezapomen ze storing by level bere jenom kcko a nepoci ta se tam defaultne s nazvem toho souboru, jestli jde do vnitr slozka nebo soubor");
                        Function<Integer, Path> storing = (currentK) -> partialResult(file, currentK, theory, setting);
                        computeAndStore(file, k, theory, partialResult(file, k, theory, setting), storing);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    // ok, output a storingLevelByLevel je dost redundantni
    private void computeAndStore(Path evidence, int k, Theory theory, Path output, Function<Integer, Path> storingLevelByLevel) throws IOException {
        Inference inference = new Inference(theory, false);
        Entailed entailed = inference.infer(evidence, setting, storingLevelByLevel);
        if (null == storingLevelByLevel) {
            Files.write(output, Sugar.list(entailed.asOutput()));
        }
    }

    private void partialResultsToResults(Path path, int k, Map<Predicate, Double> ratios, Theory theory) throws IOException {
        File finalResult = finalResultsFile(path, k, theory).toFile();
        if (!finalResult.exists() || true) { // pokazde to radsi prepiseme !
            // only first one
            Path filePath = Paths.get(path.toString(), "0.db");
            Path partialResults = partialResult(filePath, k, theory, setting);
            VECollector data = VECollector.load(partialResults, k);

//            Path p = Paths.get(path.toString(), "0.db" + partialEnding + k);
//            System.out.println("min now");
//            GammasCut model = dataToGammaCut(data, k);
//            System.out.println(data.constantsSize() + ".\t" + p);
            GammasCut model = dataToGammaCutByRatios(data, k, ratios);

            Files.write(Paths.get(finalResult.toString()), Sugar.list(model.asOutput()));
//            Map<Pair<String, Integer>, Double> result = Files.list(path)
//                    .filter(file -> file.toString().endsWith(dbEnding))
//                    .map(file -> EMPIRIC == mode
//                            ? computeRatios(file, Paths.get(file.toString() + partialEnding + k), k)
//                            : computeRatios(file)
//                    )
//                    .flatMap(map -> map.entrySet().stream())
//                    .collect(Collectors.groupingBy(entry -> entry.getKey()))
//                    .entrySet().stream()
//                    .collect(Collectors.toMap(entry -> entry.getKey()
//                            , entry -> entry.getValue().stream()
//                                    .mapToDouble(pair -> pair.getValue())
//                                    .average().orElse(0)
//                    ));
//
//            Files.write(Paths.get(finalResult.toString()),
//                    result.entrySet().stream()
//                            .map(entry -> Predicate.get(entry.getKey()).toString() + "\t" + entry.getValue())
//                            .collect(Collectors.toList()));
        }
    }

    private Path partialResult(Path filePath, int k, Theory theory, EntailmentSetting setting) {
        return Paths.get(filePath.toString() + partialEnding + "k." + k + "." + (null == theory ? "null" : theory.getName()) + "." + setting.canon().replace("k-" + setting.k() + "_","k-" + k + "_"));
    }

    private GammasCut dataToGammaCutByRatios(VECollector data, int k, Map<Predicate, Double> ratios) {
        Map<Pair<String, Integer>, Double> gammas = data.getVotes().entrySet()
                .stream()
                .collect(Collectors.groupingBy(e -> e.getKey().getPredicate()))
                .entrySet()
                .stream()
                .filter(entry -> null != ratios.get(Predicate.create(entry.getKey()))) // just a speedup
                .map(entry -> {
                    Double bestGamma = findGamma(entry.getValue(), ratios.get(Predicate.create(entry.getKey())), data, k, entry.getKey().s);

//                    System.out.println("q\t" + data.constantsSize() + "\t" + bestGamma);

                    return new Pair<>(entry.getKey(), bestGamma);
                })
                .filter(pair -> null != pair.s)
                .collect(Collectors.toMap(Pair::getR, Pair::getS));
        return GammasCut.create(gammas, k);
    }

    private Double findGamma(List<Map.Entry<Literal, Double>> votes, Double ratio, VECollector data, int k, int arity) {
        if (null == ratio) {
            return null;
        }
        Map<Double, Long> histogram = Sugar.parallelStream(votes)
                .map(e -> toGamma(e.getKey(), e.getValue(), data, k))
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        int fp = votes.size();

        double allPossible = Math.pow(data.constantsSize(), arity);

//        System.out.println("dalsi\t" + data.constantsSize());
//        histogram.entrySet().stream()
//                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
//                .forEach(System.out::println);

        for (Map.Entry<Double, Long> gamaLiterals : histogram.entrySet().stream()
                .sorted(Comparator.comparingDouble(Map.Entry::getKey))
                .collect(Collectors.toList())) {
            int comp = Double.compare(fp / allPossible, ratio);
            if (comp <= 0) {
                return gamaLiterals.getKey();
            }
            fp -= gamaLiterals.getValue();
        }

        return null;
    }

    private GammasCut dataToGammaCut(VECollector data, int k) {
        Map<Pair<String, Integer>, Double> gammas = data.getVotes().entrySet()
                .stream()
                .collect(Collectors.groupingBy(e -> e.getKey().getPredicate()))
                .entrySet()
                .stream()
                .map(entry -> {
                    Double minGama = entry.getValue().stream()
                            .mapToDouble(e -> toGamma(e.getKey(), e.getValue(), data, k))
                            .min()
                            .orElse(Double.POSITIVE_INFINITY);
                    return new Pair<>(entry.getKey(), minGama);
                }).collect(Collectors.toMap(Pair::getR, Pair::getS));
        return GammasCut.create(gammas, k);
    }

    private Double toGamma(Literal literal, Double votes, VECollector collector, int k) {
        return votes / collector.getCToX(literal.terms().size(), k);
    }

    /*private Map<Pair<String, Integer>, Double> computeRatios(Path groundTruth, Path predicted, int k) {
        // predicted je tady jenom jako apendix, kdbysy se to chtelo porovnavat s tim co se da dokazat... ale chtelo by ten vysledek jeste svazat pomoci nejakeho hashe z teorie
        Set<Literal> evidence = utils.loadEvidence(groundTruth);
        VECollector collector = VECollector.load(predicted, k);
        Set<Literal> data = collector.cut(KCut.K_CUT);
        Double constants = 1.0 * LogicUtils.constants(new Clause(evidence)).size();
        return utils.predicates(evidence).stream() // evidence here should be everything, so it does not matter from where we take the predicates
                .collect(Collectors.toMap(Predicate::getPair,
                        predicate -> data.stream().filter(literal -> literal.getPredicate().equals(predicate.getPair()))
                                .count() / constants));
    }*/


    /*private Map<Pair<String, Integer>, Double> computeRatios(Path groundTruth) {
        Set<Literal> evidence = utils.loadEvidence(groundTruth);
        Double constants = 1.0 * LogicUtils.constants(new Clause(evidence)).size();
        return utils.predicates(evidence).stream()
                .collect(Collectors.toMap(Predicate::getPair,
                        predicate -> evidence.stream().filter(literal -> literal.getPredicate().equals(predicate.getPair()))
                                .count() / constants));
    }*/

    /**
     * the path is a folder with train data wrt to constant size, e.g. folder containing a folder constSize_1 with content 0.db, 1.db,...
     *
     * @param path
     * @return
     */
    public ConstantGammaCut learn(Path path, int k, Theory theory, Map<Predicate, Double> ratios) {
        return learn(path, k, theory, ratios, (p) -> true);
    }

    public ConstantGammaCut learn(Path path, int k, Theory theory, Double ratio) {
        Map<Predicate, Double> ratios = new Map<Predicate, Double>() {
            @Override
            public int size() {
                return 0;
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
            public Double put(Predicate key, Double value) {
                return null;
            }

            @Override
            public Double remove(Object key) {
                return null;
            }


            @Override
            public void putAll(Map m) {

            }

            @Override
            public void clear() {

            }

            @Override
            public Set keySet() {
                return null;
            }

            @Override
            public Collection values() {
                return null;
            }

            @Override
            public Set<Entry<Predicate, Double>> entrySet() {
                return null;
            }

            @Override
            public boolean equals(Object o) {
                return false;
            }

            @Override
            public int hashCode() {
                return 0;
            }
        };
        return learn(path, k, theory, ratios, (p) -> true);
    }


    public ConstantGammaCut learn(Path path, int k, Theory theory, Map<Predicate, Double> ratios, java.util.function.Predicate<Path> folderFilter) {
        Map<Integer, GammasCut> gammasGivenConstantSize = null;
        try {
            gammasGivenConstantSize = Files.list(path)
                    .filter(folder -> folder.toFile().getName().matches("\\d+") && folderFilter.test(folder))
                    .map(folder -> new Pair<>(Integer.parseInt(folder.toFile().getName()), folder))
                    .collect(Collectors.toMap(Pair::getR, pair -> learnFromConstantFolder(pair.getS(), k, theory, ratios)));
        } catch (IOException e) {
            e.printStackTrace();
        }


        return new ConstantGammaCut(gammasGivenConstantSize, k, path.toString());
    }

    public static StatefullConstantGammaLearner create() {
        return create(null);
    }

    public static StatefullConstantGammaLearner create(EntailmentSetting setting) {
        return new StatefullConstantGammaLearner(setting);
    }
}
