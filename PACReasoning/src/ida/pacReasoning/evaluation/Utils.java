package ida.pacReasoning.evaluation;

import java.io.*;

import auc.*;
import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.subsumption.Matching;
import ida.pacReasoning.TestRunner;
import ida.pacReasoning.entailment.collectors.*;
import ida.pacReasoning.entailment.cuts.*;
import ida.utils.MutableInteger;
import ida.utils.Sugar;
import ida.utils.collections.Counters;
import ida.utils.collections.DoubleCounters;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 15. 6. 2018.
 */
public class Utils {


    private Utils() {
    }

    // asi by to mohlo byt vsechno static, je to jenom na vyhodnoceni
    // procistit, vola se to dokola a neni to moc pekne
    public static Utils create() {
        return new Utils();
    }

    public static int evidenceFileNumber(Path path) {
        if (path.toFile().getName().contains("queries")) {
            return Integer.parseInt(path.toFile().getName().split("\\.")[0].substring("queries".length()));
        }
        String name = path.toFile().getName().split("\\.")[0];
        try{
            return Integer.parseInt(name);
        }catch (Exception e) {
            return 0;
        }
    }

    public String plotHEDwithCut(Path testEvidence, List<Pair<Path, Integer>> paths, boolean maskGT) {
        return plotXHED(testEvidence, paths.stream().map(p -> new Pair<Pair<Path, Integer>, Cut>(p, KCut.K_CUT)).collect(Collectors.toList()), false, maskGT);
    }

    public String plotCHED(Path testEvidence, List<Pair<Path, Integer>> paths, boolean maskGT) {
        return plotXHED(testEvidence, paths.stream().map(p -> new Pair<Pair<Path, Integer>, Cut>(p, KCut.K_CUT)).collect(Collectors.toList()), true, maskGT);
    }

    public String plotCHEDwithCut(Path testEvidence, List<Pair<Pair<Path, Integer>, Cut>> paths, boolean maskGT) {
        return plotXHED(testEvidence, paths, true, maskGT);
    }

    public String plotCHEDwithGroupCut(Path testEvidence, List<Pair<Pair<Path, Integer>, List<Cut>>> paths, boolean maskGT) {
        return plotXHEDCollectors(testEvidence
                , paths.stream()
                        .flatMap(pair -> {
                            Data<Integer, VECollector> ve = Data.loadResults(pair.getR());
                            return pair.getS().stream().map(cut -> cut(ve, cut));
                        }).collect(Collectors.toList())
                , true, maskGT);
    }

    public String plotXHED(Path testEvidence, List<Pair<Pair<Path, Integer>, Cut>> paths, boolean cumulative, boolean maskGT) {
        return plotXHEX(testEvidence, paths, cumulative, maskGT, true);
    }

    public String plotXHEX(Path testEvidence, List<Pair<Pair<Path, Integer>, Cut>> paths, boolean cumulative, boolean maskGT, boolean difference) {
        return plotXHEXCollectors(testEvidence
                , paths.stream().map(pair -> cut(Data.loadResults(pair.getR()), pair.getS())).collect(Collectors.toList())
                , cumulative, maskGT, difference);
    }

    // plots CHED for all paths compared to the first one
    public String plotXHEDCollectors(Path testEvidence, List<Data<Integer, VECollector>> loaded, boolean cumulative, boolean maskGT) {
        return plotXHEXCollectors(testEvidence, loaded, cumulative, maskGT, true);
    }


    public String plotXHEXCollectors(Path testEvidence, List<Data<Integer, VECollector>> loaded, boolean cumulative, boolean maskGT, boolean difference) {
        Cut cutter = KCut.K_CUT;

        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(testEvidence));
        List<Data<Integer, Integer>> hammings = loaded.stream()
                .map(data -> hamming(evidence, untype(data), cutter, maskGT)).collect(Collectors.toList());

        List<Data<Integer, Integer>> diffs = difference
                ? IntStream.range(1, loaded.size()).mapToObj(idx -> diff(hammings.get(0), hammings.get(idx))).collect(Collectors.toList())
                : hammings;

        //hammings.get(0).getData().forEach(d -> System.out.println(d.randomGenerator + "\t" + d.s));

        //System.exit(-80);
        if (cumulative) {
            diffs = cumulate(diffs);
        }

        return plot(diffs, (cumulative ? "C" : "") + "HE" + (difference ? "D" : ""));
    }

    public String plotExecutionTime(List<Pair<Path, Integer>> paths) {
        List<Data<Integer, Double>> executionTime = paths.stream().map(p -> Data.loadDirectoryExecutionTime(p.r)).collect(Collectors.toList());
        return plot(executionTime, "execution time");
    }


    public String plotAUCPRInterpolated(Path testEvidence, List<Pair<Path, Integer>> paths) {
        return plotAUCPRInterpolated(testEvidence, paths, null);
    }

    //v tehle verzi se musi dit nejaka nepleche, nebo odebirani evidence z predikce, neco takoveho
    public String plotAUCPRInterpolated(Path testEvidence, List<Pair<Path, Integer>> paths, Set<Literal> allTestEvidence) {
        // if allTestEvidence is true, then only corrupted ofo those are taken into account; so set this to non-null value and you'll obtain filtered-corrupted plots
        Set<Pair<String, Term>> left = Sugar.set();
        Set<Pair<Term, String>> right = Sugar.set();
        if (null != allTestEvidence) {
            LogicUtils.untype(allTestEvidence).forEach(l -> {
                left.add(new Pair<>(l.predicate(), l.get(0)));
                right.add(new Pair<>(l.get(1), l.predicate()));
            });
        }
        System.out.println("tady je dost mozna chyba v te implementaci");
        //java.util.function.Predicate<Path> filterFiles = p -> !p.toString().contains("WC");
        System.out.println("pro vykresleni UWCS ted pocitejme max evidenci 500!");
        java.util.function.Predicate<Path> filterFiles = p -> true;//p -> !p.toString().contains("WC");
        int minimum = sharedMinimum(paths, filterFiles);
//        int minimum = 900;
        int step = 10;
//        int minimum = 500;
//        int step = 20;
//        System.out.println("debug now!!!!");
        System.out.println("minimum\t" + minimum + "\tstep\t" + step);
        java.util.function.Predicate<Pair<Integer, Path>> filter = (p) -> p.r <= minimum && (p.r % step == 0);
        List<Data<Integer, VECollector>> loaded = paths.stream().map(p -> Data.loadDirectory(p.r, p.s, filter, false)).collect(Collectors.toList());

        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(testEvidence));
        List<Data<Integer, Double>> prAUCs = loaded.stream()
                //.parallel()
                .map(data -> {
                    if (null == allTestEvidence) {
                        return aucPR(evidence, untype(data), true);
                    } else {
                        Data<Integer, VECollector> filteredCorrupted = new Data<>(untype(data).getData().stream().map(pair -> {
                            Integer r = pair.getR();
                            VECollector collector = pair.getS();
                            VECollector corruptedFilteredCollector = VECollector.create(collector.getEvidence(), collector.getK());
                            collector.getVotes().entrySet().stream()
                                    .filter(entry -> left.contains(new Pair<>(entry.getKey().predicate(), entry.getKey().get(0)))
                                            || right.contains(new Pair<>(entry.getKey().get(1), entry.getKey().predicate())))
                                    .forEach(entry -> corruptedFilteredCollector.getVotes().add(entry.getKey(), entry.getValue()));
                            return new Pair<>(r, corruptedFilteredCollector);
                        }).collect(Collectors.toList()),
                                data.getName());
                        return aucPR(evidence, filteredCorrupted, true);
                    }
                }).collect(Collectors.toList());

        //  it's cumulative by default :))
//        prAUCs = prAUCs.stream().map(this::interpolatedCumulatedSum).collect(Collectors.toList());
//        return plot(prAUCs, "CIAUC-PR");
//
        return plot(prAUCs, "AUC-PR");
    }

    public String plotAUCPRInterpolatedOptimized(Path testEvidence, List<Pair<Path, Integer>> paths) {
        //java.util.function.Predicate<Path> filterFiles = p -> !p.toString().contains("WC");
        System.out.println("pro vykresleni UWCS ted pocitejme max evidenci 500!");
        java.util.function.Predicate<Path> filterFiles = p -> true;//p -> !p.toString().contains("WC");
        //int minimum = sharedMinimum(paths, filterFiles);
        //int minimum = 30;
        //int step = 10;
        int minimum = 500;
        int step = 20;
        System.out.println("ted minimum i step nastavaney rukou !!!!!");
//        int minimum = 500;
//        int step = 20;
//        System.out.println("debug now!!!!");
        System.out.println("minimum\t" + minimum + "\tstep\t" + step);
        java.util.function.Predicate<Pair<Integer, Path>> filter = (p) -> p.r <= minimum && (p.r % step == 0);
        List<Data<Integer, VECollector>> loaded = paths.stream().map(p -> Data.loadDirectory(p.r, p.s, filter, false)).collect(Collectors.toList());

        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(testEvidence));
        List<Data<Integer, VECollector>> untypedPredictions = loaded.stream().map(data -> untype(data)).collect(Collectors.toList());
        List<List<Pair<Integer, Double>>> vectorization = IntStream.range(0, untypedPredictions.size()).mapToObj(i -> Sugar.<Pair<Integer, Double>>list()).collect(Collectors.toList());

        Set<Pair<String, Integer>> predicates = LogicUtils.predicates(new Clause(evidence));
        int max = untypedPredictions.stream().mapToInt(d -> d.size()).max().orElse(0);
        for (int idx = 0; idx < max; idx++) {
            VECollector c0 = untypedPredictions.get(0).getData().get(idx).getS();
            Set<Literal> gt = mask(evidence, c0.getConstants());

            System.out.println("evd size\t" + c0.getEvidence().size());
            Literal l = new Literal("", false, Sugar.listFromCollections(LogicUtils.constants(gt)));
            Set<Literal> allPossibleLiterals = Sugar.set();
            Matching world = Matching.create(new Clause(l), Matching.THETA_SUBSUMPTION);
            for (Pair<String, Integer> predicate : predicates) {
                Literal wanna = new Literal(predicate.r, IntStream.range(0, predicate.s).mapToObj(i -> Variable.construct("X" + i)).collect(Collectors.toList()));
                Pair<Term[], List<Term[]>> subt = world.allSubstitutions(new Clause(wanna.negation()), 0, Integer.MAX_VALUE);
                for (Term[] terms : subt.getS()) {
                    Literal substituted = LogicUtils.substitute(wanna, subt.getR(), terms);
                    allPossibleLiterals.add(substituted);
                }
            }
            System.out.println("\tall possible\t" + allPossibleLiterals.size());

            for (int dataIdx = 0; dataIdx < untypedPredictions.size(); dataIdx++) {
                if (untypedPredictions.get(dataIdx).size() > idx) {
                    Integer pos = untypedPredictions.get(dataIdx).getData().get(idx).getR();
                    Double val = aucPRListPost(gt
                            , untypedPredictions.get(dataIdx).getData().get(idx).s
                            , allPossibleLiterals);
                    vectorization.get(dataIdx).add(new Pair<>(pos, val));
                }
            }
        }

        List<Data<Integer, Double>> prAUCs = IntStream.range(0, untypedPredictions.size())
                .mapToObj(idx -> new Data<Integer, Double>(vectorization.get(idx), loaded.get(idx).getName()))
                .collect(Collectors.toList());

        for (Data<Integer, Double> prAUC : prAUCs) {
            System.out.println(prAUC.getData().toString());
        }

        //  it's cumulative by default :))
        //prAUCs = prAUCs.stream().map(this::interpolatedCumulatedSum).collect(Collectors.toList());
        //return plot(prAUCs, "CIAUC-PR");

        System.out.println("there is a bug somewhere inside, because it gives different results non-optimized method");
//        throw new NotImplementedException();
        return plot(prAUCs, "AUC-PR");
    }


    private Data<Integer, Double> interpolatedCumulatedSum(Data<Integer, Double> data) {
        double cumSum = 0;
        List<Pair<Integer, Double>> cumuled = Sugar.list();
        Pair<Integer, Double> lastValue = null;
        for (Pair<Integer, Double> pair : data.getData()) {
            if (null == lastValue) {
                cumSum += pair.getS();
            } else {
                int step = pair.r - lastValue.r;
                cumSum += step * pair.getS();
            }
            cumuled.add(new Pair<>(pair.getR(), cumSum));
            lastValue = pair;
        }
        return new Data<>(cumuled, data.getName());
    }

    private int sharedMinimum(List<Pair<Path, Integer>> paths, java.util.function.Predicate<Path> filter) {
        return paths.stream()
                .filter(p -> filter.test(p.r))
                .mapToInt(p -> {
                    Path path = p.r;
                    System.out.println(path);
                    if (!path.toFile().exists()) {
                        System.out.println("this directory does not exist");
                    }
                    if (!path.toFile().isDirectory()) {
                        throw new IllegalStateException(); //NotImplementedException();
                    }
                    try {
                        //int retVal = Files.list(path).mapToInt(file -> evidenceFileNumber(file)).max().orElse(-1);
//                System.out.println(retVal + "\t" + path);
//                return retVal;
                        return Files.list(path).mapToInt(file -> evidenceFileNumber(file)).max().orElse(-1);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    throw new IllegalStateException();
                }).min().orElse(0);
    }

    public String plotAUCPR(Path testEvidence, List<Pair<Path, Integer>> paths, boolean cumulative, boolean maskGT) {
        return plotAUCPRCollectors(testEvidence
                , paths.stream().map(pair -> Data.loadResults(pair)).collect(Collectors.toList())
                , cumulative, maskGT);
    }

    public String plotAUCPRCollectors(Path testEvidence, List<Data<Integer, VECollector>> loaded, boolean cumulative, boolean maskGT) {
        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(testEvidence));
        List<Data<Integer, Double>> prAUCs = loaded.stream()
                //.parallel()
                .map(data -> aucPR(evidence, untype(data), maskGT)).collect(Collectors.toList());

        //System.exit(-80);
        if (cumulative) {
            prAUCs = cumulateD(prAUCs);
        }

        return plot(prAUCs, (cumulative ? "C" : "") + "AUC-PR");
    }

    public <X> Data<X, VECollector> untype(Data<X, VECollector> data) {
        return new Data<X, VECollector>(data.getData().stream().map(p -> new Pair<>(p.r, p.getS().untype())), data.getName());
    }

    public Data<Integer, VECollector> cut(Data<Integer, VECollector> data, Cut cut) {
        return cut(data, cut, false);
    }

    public Data<Integer, VECollector> cut(Data<Integer, VECollector> data, Cut cut, boolean cutHardEvidence) {
        return new Data<>(
                data.getData().stream()
                        .map(pair -> new Pair<>(pair.getR(), pair.s.cutCollector(cut.cut(pair.getR()), cutHardEvidence)))
                        .collect(Collectors.toList())
                , data.getName() + " " + cut.name());
    }


    public boolean plotFNFP(Path testEvidence, Path dataPath) {
        throw new IllegalStateException(); //NotImplementedException();
    }

    public String plotStats(Path groundTruth, Pair<Path, Integer> data, int mode, boolean maskGT) {
        return plotStats(groundTruth, data, null, mode, maskGT, GammaPlotter.EMPTY_ONE);
    }

    public String plotStats(Path groundTruth, Pair<Path, Integer> data, Collection<Predicate> predicates, int mode, boolean maskGT, GammaPlotter plotter) {
        return plotStats(groundTruth, Data.loadResults(data), data.s, predicates, mode, maskGT, plotter);
    }

    public String plotStats(Path groundTruth, Data<Integer, VECollector> data, int k, Collection<Predicate> predicates, int mode, boolean maskGT, GammaPlotter plotter) {
        if (1 == mode) {
//            System.out.println("debug D+G, zmena data na podcast");
            //data = data.sublist(1000,1500,data.getName() + " podcast pouze pro debug, 1k-2k");
            //data = data.sublist(1100,1200,data.getName() + " podcast pouze pro debug, 1,1k-1,2k");
            //data = data.sublist(1400,1500,data.getName() + " podcast pouze pro debug, 1,4k-1,5k");
//            data = data.sublist(1490, 1500, data.getName() + " podcast pouze pro debug, 1,499");
            return plotCHEDwrtGamma(groundTruth, data, k, predicates, maskGT, plotter);
        } else if (2 == mode) {
            return plotFPFNwrtGamma(groundTruth, data, k, predicates, maskGT, plotter);
        }
        throw new IllegalArgumentException("mode: 1->ched wrt gamma; 2->FPFN wrt gamma");
    }

    public String plotFPFNwrtGamma(Path groundTruth, Data<Integer, VECollector> data, int k, Collection<Predicate> predicates, boolean maskGT, GammaPlotter plotter) {
        Data<Integer, VECollector> finalData = untype(data);
        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(groundTruth));
        boolean allPredicates = null == predicates;
        List<List<Data<Double, Double>>> plots = Sugar.list();
        if (allPredicates) {
            plots.add(generateFPFN(evidence, finalData, (pair) -> true, k, "all", maskGT));
        } else {
            plots.addAll(Sugar.parallelStream(predicates, true)
                    .map(predicate -> generateFPFN(evidence, finalData, (pred) -> pred.equals(predicate.getPair()), k, predicate.toString(), maskGT))
                    .collect(Collectors.toList()));
        }

        return plots.stream()
                .filter(l -> null != l)
                .flatMap(l -> l.stream()) // oneByOne, pair otherwise
                .map(predicatePlot -> {
                    List<Data<Double, Double>> extendedPlot = plotter.gammas(predicatePlot, finalData.getName()
                            + //(allPredicates ? "" : predicatePlot.stream().map(plt -> plt.getName()).collect(Collectors.joining(" "))) // pair
                            (allPredicates ? "" : predicatePlot.getName()) // oneByOne
                    );
                    //extendedPlot.addAll(predicatePlot); // pair
                    extendedPlot.add(predicatePlot); // oneByOne
                    return plot(extendedPlot, (" FP n' FN of " + finalData.getName() + " w.randomGenerator.t. " + groundTruth + " and gamma").replace("_", ":").replace("\\", "\\textbackslash{}"));
                })
                .collect(Collectors.joining("\n\n"));
    }

    private List<Data<Double, Double>> generateFPFN(Set<Literal> evidence, Data<Integer, VECollector> data, java.util.function.Predicate<Pair<String, Integer>> filter, int k, String legend, boolean maskGT) {
        evidence = evidence.stream().filter(literal -> filter.test(literal.getPredicate())).collect(Collectors.toSet());

        DoubleCounters<Double> fp = new DoubleCounters<>();
        DoubleCounters<Double> fn = new DoubleCounters<>();
        /* tohle asi nebude fungovat podobne jako generateStatswrtGammas, muze tam byt nekde chyba uvnitr
        Set<Double> keys = Sugar.set();
        neni zavedena maskGT v teto implementaci, false by default
        double infinityThreshold = 0;
        for (Pair<Integer, VECollector> points : data.getData()) {
            VECollector filtered = points.getS().cutCollector(PredicateCut.load(filter), true);
            MultiMap<Double, Literal> reversed = new MultiMap<>();
            //filtered.getVotes().keySet().forEach(literal -> reversed.put(filtered.getVotes().get(literal), literal));
//            tady by se meli dava gammy, nikoliv hlasy
//                    plus pri rezu se musi predat pocet konstant
            filtered.getVotes().keySet()
                    .forEach(literal -> reversed.put(filtered.getVotes().get(literal) / filtered.getCToX(literal.terms().size()), literal));
            keys.addAll(reversed.keySet());

            double previousLastValueFP = infinityThreshold;
            double previousLastValueFN = infinityThreshold;
            for (Double gamma : keys.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
                Pair<Integer, Integer> fpfn = fpfn(evidence, filtered.cut(GammasCut.load(gamma, k)));
                int currentFp = fpfn.randomGenerator;
                int currentFn = fpfn.s;
                if (fp.keySet().contains(gamma)) {
                    previousLastValueFP = fp.get(gamma);
                    fp.add(gamma, currentFp);
                    previousLastValueFN = fn.get(gamma);
                    fn.add(gamma, currentFn);
                } else {
//                    previousLastValue = previousLastValue;
                    fp.add(gamma, previousLastValueFP + currentFp);
                    fn.add(gamma, previousLastValueFN + currentFn);
                }
            }

            infinityThreshold += hamming(evidence, filtered.getEvidence());
        }*/
        PredicateCut pc = PredicateCut.create(filter);
        Data<Integer, VECollector> filteredByPredicate = new Data<>(data.getData().stream()
                .map(p -> new Pair<>(p.r, p.s.cutCollector(pc, true)))
                .collect(Collectors.toList()), "p-cut ");
        Set<Literal> finalEvidence = evidence;
        MultiMap<Predicate, Double> gammas = gammas(filteredByPredicate, false);
        gammas.values().stream()
                .flatMap(Collection::stream)
                .distinct()
                .forEach(gamma -> {
                            Pair<Integer, Integer> fpfn = Sugar.parallelStream(filteredByPredicate.getData(), true)
                                    .map(pair -> fpfn(maskGT ? mask(finalEvidence, pair.s.getConstants()) : finalEvidence
                                            , pair.s.cut(GammasCut.create(gamma, pair.s.getK()))))
                                    .reduce((p1, p2) -> new Pair<>(p1.r + p2.r, p1.s + p2.s))
                                    .orElse(new Pair<>(0, 0));
                            fp.add(gamma, fpfn.r);
                            fn.add(gamma, fpfn.s);
                        }
                );

        if (fp.keySet().isEmpty()) {
            return null;
        }

        List<Pair<Double, Double>> fpPoints = fp.keySet().stream().sorted()
                .map(key -> new Pair<>(key, fp.get(key))).collect(Collectors.toList());
        List<Pair<Double, Double>> fnPoints = fn.keySet().stream().sorted()
                .map(key -> new Pair<>(key, fn.get(key))).collect(Collectors.toList());
        return Sugar.list(new Data<>(fpPoints, "FP " + legend),
                new Data<>(fnPoints, "FN " + legend));
    }

    private Pair<Integer, Integer> fpfn(Set<Literal> groundTruth, Set<Literal> predicted, Predicate predicate) {
        java.util.function.Function<Set<Literal>, Set<Literal>> filter = (set) -> set.parallelStream().filter(l -> l.getPredicate().equals(predicate.getPair())).collect(Collectors.toSet());
        return fpfn(filter.apply(groundTruth), filter.apply(predicted));
    }

    private Pair<Integer, Integer> fpfn(Set<Literal> groundTruth, Set<Literal> predicted) {
        int fp = Sugar.setDifference(predicted, groundTruth).size();
        int fn = Sugar.setDifference(groundTruth, predicted).size();
        return new Pair<>(fp, fn);
    }


    public String plotCHEDwrtGamma(Path groundTruth, Data<Integer, VECollector> data, int k, Collection<Predicate> predicates, boolean maskGT, GammaPlotter plotter) {
//        System.out.println("d1\t"  + data.getName());

        Set<Literal> evidence = LogicUtils.loadEvidence(groundTruth);
        boolean allPredicates = null == predicates;
        List<Data<Double, Double>> plots = Sugar.list();
        if (allPredicates) {
            plots.add(generateStats(evidence, data, (pair) -> true, k, "all", maskGT));
        } else {
            plots.addAll(Sugar.parallelStream(predicates, true)
                    .map(predicate -> generateStats(evidence, data, (pred) -> pred.equals(predicate.getPair()), k, predicate.toString(), maskGT))
                    .collect(Collectors.toList()));
        }

        plots = plots.stream().filter(this::isNotEmpty).collect(Collectors.toList());

        return
                oneByOne(plots)
//                groupByY(plots)
//                .map(subplots -> plot(subplots, (" CHED of " + data.getName() + " w.randomGenerator.t. " + groundTruth + " and gamma").replace("_", ":").replace("\\", "\\textbackslash{}")))
//                    plots.stream()
                        .map(subplots -> {
//                    nejaka chyba tady, grupovani asi funguje jinak nez ma :, nez v te radce nadtim
                            List<Data<Double, Double>> extendedPlots = plotter.gammas(subplots, data.getName());
                            extendedPlots.addAll(subplots);
//                            extendedPlots.add(subplots);
                            System.out.println(subplots.size() + "\t" + extendedPlots);
                            return plot(extendedPlots, (" CHED of " + data.getName() + " w.randomGenerator.t. " + groundTruth + " and gamma").replace("_", ":").replace("\\", "\\textbackslash{}"));
                        })
                        .collect(Collectors.joining("\n\n"));
//        return plot(plots, (" CHED of " + folder + " w.randomGenerator.t. " + groundTruth + " and gamma").replace("_", ":"));
    }

    private <T extends Number> Stream<List<Data<T, T>>> oneByOne(List<Data<T, T>> input) {
        return input.stream().map(Sugar::list);
    }

    private <T extends Number> Stream<List<Data<T, T>>> groupByY(List<Data<T, T>> input) {
        Map<Pair<Double, Double>, List<Data<T, T>>> map = new HashMap<>();

        for (Data<T, T> data : input) {
            double mean = data.getData().stream().mapToDouble(pair -> Double.valueOf(pair.getS().toString())).average().orElse(0);
            double std = Math.sqrt(data.getData().stream()
                    .mapToDouble(pair -> Math.pow((Double.valueOf(pair.getS().toString()) - mean), 2.0))
                    .sum() / data.getData().size());
            boolean added = false;
            for (Pair<Double, Double> cluster : map.keySet()) {
                if (Math.abs(mean - cluster.r) <= 3 * std
                        && Math.abs(cluster.r - mean) <= 3 * cluster.s) {
                    map.get(cluster).add(data);
                    added = true;
                    break;
                }
            }
            if (!added) {
                map.put(new Pair<>(mean, std), Sugar.list(data));
            }

        }

        return map.entrySet().stream().map(entry -> entry.getValue());
    }

    private boolean isNotEmpty(Data<? extends Number, ? extends Number> data) {
        return !data.getData().isEmpty();
    }

    // generuje CHD
    private Data<Double, Double> generateStats(Set<Literal> evidence, Data<Integer, VECollector> data, java.util.function.Predicate<Pair<String, Integer>> filter, int k, String legend, boolean maskGT) {
        // debug ted -- bez p-cutu hodnoty :))
        // ok, tak zase pridam  p-cut
        evidence = evidence.stream().filter(literal -> filter.test(literal.getPredicate())).collect(Collectors.toSet());

        DoubleCounters<Double> chd = new DoubleCounters<>();
        Set<Double> keys = Sugar.set();
        double infinityThreshold = 0;
//        Set<String> ld = ConcurrentHashMap.newKeySet();

        /* * /  tady ten zpusob je memory a speed friendly, akorat je v nem nejaka chyba, ze to hazi o par jednotek hodnoty jinak
         for (Pair<Integer, VECollector> points : data.getData()) {
         VECollector filtered = points.getS().cutCollector(PredicateCut.load(filter), true);
         MultiMap<Double, Literal> reversed = new MultiMap<>();
         //filtered.getVotes().keySet().forEach(literal -> reversed.put(filtered.getVotes().get(literal), literal));
         //            tady by se meli dava gammy, nikoliv hlasy
         //                    plus pri rezu se musi predat pocet konstant
         //            filtered.getVotes().keySet()
         //                    .forEach(literal -> reversed.put(filtered.getVotes().get(literal) / filtered.getCToX(literal.terms().size()), literal));

        neni tady zanesena verze s maskGT, false by default
         filtered.getVotes().keySet()
         .forEach(literal -> {
         double val = filtered.getVotes().get(literal) / filtered.getCToX(literal.terms().size());
         reversed.put(val, literal);
         ld.add(literal + "\t" + filtered.getVotes().get(literal) + "\t" + filtered.getCToX(literal.terms().size()) + "\t" + val);
         });


         keys.addAll(reversed.keySet());

         double previousLastValue = infinityThreshold;
         for (Double gamma : keys.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList())) {
         int error = hamming(evidence, filtered.cut(GammasCut.load(gamma, k)));
         if (chd.keySet().contains(gamma)) {
         previousLastValue = chd.get(gamma);
         chd.add(gamma, error);
         } else {
         //                    previousLastValue = previousLastValue;
         chd.add(gamma, previousLastValue + error);
         }
         }

         infinityThreshold += hamming(evidence, filtered.getEvidence());
         }
         /**/


        //debug tady, toto je puvodni verze
        /*
        PredicateCut pc = PredicateCut.get(filter);
        data<Integer, VECollector> filteredByPredicate = new data<>(data.getData().stream()
                .map(p -> new Pair<>(p.randomGenerator, p.s.cutCollector(pc, true)))
                .collect(Collectors.toList()), "p-cut ");
        Set<Literal> finalEvidence = evidence;
        MultiMap<Predicate, Double> gammas = gammas(filteredByPredicate, false);
        gammas.values().stream()
                .flatMap(Collection::stream)
                .distinct() // tohle mohlo potencionalne zpusobovat chybu kdyby to nebylo
                .sorted()
                .forEach(gamma ->
                        {
                            System.out.println("solving gamma\t" + gamma);
                            chd.add(gamma, Sugar.parallelStream(filteredByPredicate.getData().stream(), true)
                                    .mapToDouble(pair -> hamming(
                                            maskGT ? evidenceMask(finalEvidence, pair.s.getEvidence()) : finalEvidence
                                            , pair.s.cut(GammasCut.get(gamma, pair.s.getK())))
                                    )
                                    .sum())
                            ;
                        }
                );*/

        // a tady v tom zasahuji
        PredicateCut pc = PredicateCut.create(filter);
        //System.out.println("zkusime to bez predicate cutu :))");
        System.out.println("pridavam opet pc-cut");
        Data<Integer, VECollector> filteredByPredicate = new Data<>(data.getData().stream()
                .map(p -> new Pair<>(p.r, p.s.cutCollector(pc, true)))
                .collect(Collectors.toList()), "p-cut ");
        Set<Literal> finalEvidence = evidence;
//        data<Integer, VECollector> filteredByPredicate = data;
        MultiMap<Predicate, Double> gammas = gammas(filteredByPredicate, false);
        // muze hazet chybu
        Set<Predicate> preds = gammas.keySet().stream().filter(p -> filter.test(p.getPair())).collect(Collectors.toSet());
        if (preds.size() != 1) {
            System.out.println("chyba tady");
            System.out.println("legend\t" + legend);
            return new Data<>(Sugar.list(), "" + legend + " none gamas");
        }
        Predicate pred = Sugar.chooseOne(preds);
        List<Pair<Double, Double>> diff = Sugar.parallelStream(gammas.get(pred), true)
                .distinct() // tohle mohlo potencionalne zpusobovat chybu kdyby to nebylo
                .sorted()
                .map(gamma -> new Pair<>(gamma,
                                Sugar.parallelStream(filteredByPredicate.getData().stream(), true)
                                        .mapToDouble(pair -> hamming(maskGT ? mask(finalEvidence, pair.s.getConstants()) : finalEvidence
                                                , pair.s.cut(GammasCut.create(pred, gamma, pair.s.getK()))))
                                        .sum()
                        )
                ).collect(Collectors.toList());


        List<Pair<Double, Double>> points = diff;
        Double xMin = points.stream()
                .min(Comparator.comparingDouble(Pair::getS))
                .orElse(new Pair<>(-1.0, 0.0))
                .r;
        return new Data<>(points, "" + legend + "; xMin at = " + xMin);
    }

    public Set<Literal> evidenceMask(Set<Literal> evidence, Set<Literal> constants) {
        return mask(evidence, LogicUtils.constants(new Clause(constants)));
    }

    public Set<Literal> mask(Set<Literal> evidence, Collection<Constant> constants) {
        return Sugar.parallelStream(evidence, true)
                .filter(literal -> constants.containsAll(literal.terms()))
                .collect(Collectors.toSet());
    }

    public MultiMap<Predicate, Double> gammas(Data<Integer, VECollector> data, boolean verbose) {
        MultiMap<Predicate, Double> retVal = new MultiMap<>(MultiMap.CONCURENT_HASH_SET);
//        data.getData().forEach(pair -> {
//            DoubleCounters<Literal> votes = pair.getS().getVotes();
//            votes.keySet().forEach(key -> retVal.put(new Predicate(key.predicate(), key.arity()), votes.get(key)));
//        });
//        Set<String> ld = ConcurrentHashMap.newKeySet();

        Sugar.parallelStream(data.getData(), true)
                .forEach(pair -> {
                    DoubleCounters<Literal> votes = pair.getS().getVotes();
                    votes.keySet().forEach(key -> retVal.put(new Predicate(key.predicate(), key.arity()),
                            votes.get(key) / pair.getS().getCToX(key.terms().size())));
//                    votes.keySet().forEach(key -> {
//                        double val = votes.get(key) / pair.getS().getCToX(key.terms().size());
//                        if (key.predicate().toLowerCase().equals("faculty")) {
//                            ld.add(key + "\t" + votes.get(key) + "\t" + pair.s.getCToX(key.terms().size()) + "\t" + val);
//                        }
//                        retVal.put(new Predicate(key.predicate(), key.arity()),
//                                val);
//                    });

                });

//        System.out.println("debug here !");
//        System.out.println(ld.stream().sorted().collect(Collectors.joining("\n")));

        if (verbose) {
            retVal.keySet().forEach(key -> System.out.println(key + "\t" + retVal.get(key).size() + "\n\t" + retVal.get(key).stream().sorted().collect(Collectors.toList())));
        }

        return retVal;
    }

    public MultiMap<Predicate, Double> gammas(Pair<Path, Integer> data, boolean verbose) {
        return gammas(data.r, data.s, verbose);
    }

    public MultiMap<Predicate, Double> gammas(Path path, int k, boolean verbose) {
        if (verbose) {
            System.out.println("source\t" + path);
        }
        return gammas(Data.loadResults(path, k), verbose);
    }


    private Data<Integer, Integer> cumulative(Data<Integer, Integer> data) {
        int cumSum = 0;
        List<Pair<Integer, Integer>> cumuled = Sugar.list();
        for (Pair<Integer, Integer> pair : data.getData()) {
            cumSum += pair.getS();
            cumuled.add(new Pair<>(pair.getR(), cumSum));
        }
        return new Data<>(cumuled, data.getName());
    }

    // tohle opravdu neni pekny jak je to tu nekolikrat...tfuj
    private Data<Integer, Double> cumulativeD(Data<Integer, Double> data) {
        double cumSum = 0;
        List<Pair<Integer, Double>> cumuled = Sugar.list();
        for (Pair<Integer, Double> pair : data.getData()) {
            cumSum += pair.getS();
            cumuled.add(new Pair<>(pair.getR(), cumSum));
        }
        return new Data<>(cumuled, data.getName());
    }


    // TODO generalize
    private Data<Double, Double> cumulativeDouble(Data<Double, Double> data) {
        double cumSum = 0;
        List<Pair<Double, Double>> cumuled = Sugar.list();
        for (Pair<Double, Double> pair : data.getData()) {
            cumSum += pair.getS();
            cumuled.add(new Pair<>(pair.getR(), cumSum));
        }
        return new Data<>(cumuled, data.getName());
    }

    public <T extends Number, U extends Number> String plot(List<Data<T, U>> data, String caption) {
        StringBuilder plot = new StringBuilder();
        plot.append("\\begin{figure}\n" +
                "\\begin{tikzpicture}\n" +
                "    \\begin{axis}[legend pos=outer north east\n" +//,no marks\n" +
                "    %legend pos=north west\n" +
                "    ]\n" +
                "    \n");

        for (Data<? extends Number, ? extends Number> graph : data) {
            plot.append(toTikz(graph));
        }

        plot.append("    \\end{axis}\n" +
                "    \\end{tikzpicture}\\caption{" + caption + "}\n" +
                "\\end{figure}\n");

        return plot.toString();
    }


    public String toTikz(Data<? extends Number, ? extends Number> data) {
        StringBuilder retVal = new StringBuilder();
        String attrib = data.getAttributes().equals("") ? "" : "[" + data.getAttributes() + "]";
        retVal.append("\n \\addplot" + attrib + " coordinates { "
                + data.getData().stream().map(pair -> "(" + pair.getR() + "," + pair.getS() + ")")
                .collect(Collectors.joining(" "))
                + " };\n");
        retVal.append("    \\addlegendentry{" + data.getName().replaceAll("_", ":").replace("\\", "\\textbackslash{}") + "}\n");
        return retVal.toString();
    }

    public Data<Integer, Integer> diff(Data<Integer, Integer> first, Data<Integer, Integer> second) {
        return executeOperation(first, second, (i1, i2) -> i1 - i2, "-");
    }

    public Data<Integer, Integer> sum(Data<Integer, Integer> first, Data<Integer, Integer> second) {
        return executeOperation(first, second, (i1, i2) -> i1 + i2, "+");
    }

    public Data<Integer, Integer> executeOperation(Data<Integer, Integer> first, Data<Integer, Integer> second, BiFunction<Integer, Integer, Integer> operation, String operationName) {
        List<Pair<Integer, Integer>> difference = IntStream.range(0, Math.min(first.size(), second.size()))
                .mapToObj(idx -> new Pair<>(first.getData().get(idx).getR(),
                        operation.apply(first.getData().get(idx).s, second.getData().get(idx).s)))
                .collect(Collectors.toList());
        return new Data<>(difference, first.getName() + " " + operationName + " " + second.getName());

    }

    public int hamming(Set<Literal> a, Set<Literal> b) {
        int unique = 0;


        Set<Literal> copied = new HashSet<>(a);

        for (Literal literal : b) {
            if (!copied.remove(literal)) {
                unique++;
            }
        }
        unique += copied.size();
//        System.out.println("a\t" + a + "\nb\t" + b + "\n\t\t" + unique);
        return unique;
        //return Sugar.unique(a, b).size();
    }

    public Stream<Pair<Integer, Integer>> hammingStream(Set<Literal> evidence, Data<Integer, VECollector> data, Cut cut, boolean maskGT) {
        return Sugar.parallelStream(data.getData(), true)
                .map(pair -> new Pair<>(pair.r, hamming(
                        maskGT ? mask(evidence, pair.s.getConstants())
                                : evidence
                        , pair.s.cut(cut.cut(pair.r))))
                );
    }

    public Data<Integer, Integer> hamming(Set<Literal> evidence, Data<Integer, VECollector> data, Cut cut, boolean maskGT) {
        return new Data<>(hammingStream(evidence, data, cut, maskGT).collect(Collectors.toList()), data.getName());
    }

    public Data<Integer, Double> aucPR(Set<Literal> evidence, Data<Integer, VECollector> data, boolean maskGT) {
        return new Data<>(aucPRStream(evidence, data, maskGT).collect(Collectors.toList()), data.getName());
    }

    public Stream<Pair<Integer, Double>> aucPRStream(Set<Literal> evidence, Data<Integer, VECollector> data, boolean maskGT) {
        return Sugar.parallelStream(data.getData(), false)
                /*.filter(p -> {
                    if (p.r != 98) {
                        System.out.println("skipping\t" + p.r);
                        return false;
                    }
                    System.out.println("go on\t" + p.r);
                    return true;
                })*/
                //.filter(p -> p.r < 60)
                .map(pair -> {
                            //System.out.println(pair.r);
                            return new Pair<>(pair.r,
                                    aucPRList(
                                            //aucPRListBackup( // tedka zkusim tuhle protoze v te druhe implementaci je nejpise chyba -- tahle je pomala a fakt nebude fungovat
                                            maskGT ? mask(evidence, pair.s.getConstants())
                                                    : evidence
                                            , pair.s));
                        }
                );
    }

    Random randomGenerator = new Random();

    private Double aucPRList(Set<Literal> groundTruth, VECollector collector) {
        //System.out.println("evd size\t" + collector.getEvidence().size());

        Literal l = new Literal("", false, Sugar.listFromCollections(LogicUtils.constants(groundTruth)));
        Set<Pair<String, Integer>> predicates = LogicUtils.predicates(new Clause(groundTruth));
        Set<Literal> allPossibleLiterals = Sugar.set();
        /**/
        Matching world = Matching.create(new Clause(l), Matching.THETA_SUBSUMPTION);
//        System.out.println("constants introduction literal\t" + l);
//        System.out.println("gt is\t" + groundTruth);
        for (Pair<String, Integer> predicate : predicates) {
            Literal wanna = new Literal(predicate.r, IntStream.range(0, predicate.s).mapToObj(i -> Variable.construct("X" + i)).collect(Collectors.toList()));
//            System.out.println("wanna\t" + wanna);
            Pair<Term[], List<Term[]>> subt = world.allSubstitutions(new Clause(wanna.negation()), 0, Integer.MAX_VALUE);
            for (Term[] terms : subt.getS()) {
                Literal substituted = LogicUtils.substitute(wanna, subt.getR(), terms);
                // pro spravne fungovani musi byt zakomentovano (a naopak odkomentovano) vzdy par se stejnym pismenem
                /*if (!collector.getEvidence().contains(substituted)) { // *B*
                    allPossibleLiterals.add(substituted);
                }*/
//                jinak by to bylo pouze
                allPossibleLiterals.add(substituted);   // *A*
            }
        }
        //System.out.println("\tall possible\t" + allPossibleLiterals.size());

        // this below is parsing and reading of the input by AUCCalculator
        int var2 = 0;
        int var3 = 0;
        byte var4 = 0;
        LinkedList<ClassSort> data = new LinkedList();

        double var9;
        int var11;

        /*tohle je starsi verze sallPossibleLiterals.add(substituted); *A*    */
        for (Literal literal : allPossibleLiterals) {
            boolean gt = groundTruth.contains(literal);
            double weight = 0.0;
            if (collector.getEvidence().contains(literal)) { // timhle by se melo melo opravit i to, kdyz bude ve votes (bugem) predikovane fakta, ktera jsou uz v evidenci
                weight = 1.0;
            } else if (collector.getVotes().keySet().contains(literal)) {
                weight = 0.9 * Math.min(1.0, collector.getVotes().get(literal));
            }
            data.add(new ClassSort(weight, gt ? 1 : 0));
        }/**/

        /* * /          // *B*
        for (Literal literal : allPossibleLiterals) {
            boolean gt = groundTruth.contains(literal);
            double weight = 0.0;
            if (collector.getVotes().keySet().contains(literal)) {
                weight = Math.min(1.0, collector.getVotes().get(literal));
            }
            data.add(new ClassSort(weight, gt ? 1 : 0));
        }/**/

        ClassSort[] var21 = ReadList.convertList(data);
        ArrayList var22 = new ArrayList();
        var9 = var21[var21.length - 1].getProb();
        if (var21[var21.length - 1].getClassification() == 1) {
            ++var2;
        } else {
            ++var3;
        }
        int var20 = var4 + 1;

        for (var11 = var21.length - 2; var11 >= 0; --var11) {
            double var12 = var21[var11].getProb();
            int var14 = var21[var11].getClassification();
            if (var12 != var9) {
                var22.add(new PNPoint((double) var2, (double) var3));
            }
            var9 = var12;
            if (var14 == 1) {
                ++var2;
            } else {
                ++var3;
            }

            ++var20;
        }

        var22.add(new PNPoint((double) var2, (double) var3));
        Confusion var23 = new Confusion((double) var2, (double) var3);
        Iterator var24 = var22.iterator();

        while (var24.hasNext()) {
            PNPoint var13 = (PNPoint) var24.next();
            var23.addPoint(var13.getPos(), var13.getNeg());
        }

        System.out.println("\tsortAnyBURL & interpolation");
        var23.sort();
        var23.interpolate();
        System.out.println("\tcomputing AUCPR");
        return var23.calculateAUCPR(0.0D);
//        return var23.calculateAUCROC();
    }


    private Double aucPRListPost(Set<Literal> groundTruth, VECollector collector, Set<Literal> allPossibleLiterals) {

        // this below is parsing and reading of the input by AUCCalculator
        int var2 = 0;
        int var3 = 0;
        byte var4 = 0;
        LinkedList data = new LinkedList();

        double var9;
        int var11;

        for (Literal literal : allPossibleLiterals) {
            boolean gt = groundTruth.contains(literal);
            double weight = 0.0;
            if (collector.getEvidence().contains(literal)) { // timhle by se melo melo opravit i to, kdyz bude ve votes (bugem) predikovane fakta, kteroa jsou uz v evidenci
                weight = 1.0;
            } else if (collector.getVotes().keySet().contains(literal)) {
                weight = 0.9 * Math.min(1.0, collector.getVotes().get(literal));
            }
            data.add(new ClassSort(weight, gt ? 1 : 0));
        }

        ClassSort[] var21 = ReadList.convertList(data);
        ArrayList var22 = new ArrayList();
        var9 = var21[var21.length - 1].getProb();
        if (var21[var21.length - 1].getClassification() == 1) {
            ++var2;
        } else {
            ++var3;
        }
        int var20 = var4 + 1;

        for (var11 = var21.length - 2; var11 >= 0; --var11) {
            double var12 = var21[var11].getProb();
            int var14 = var21[var11].getClassification();
            if (var12 != var9) {
                var22.add(new PNPoint((double) var2, (double) var3));
            }
            var9 = var12;
            if (var14 == 1) {
                ++var2;
            } else {
                ++var3;
            }

            ++var20;
        }

        var22.add(new PNPoint((double) var2, (double) var3));
        Confusion var23 = new Confusion((double) var2, (double) var3);
        Iterator var24 = var22.iterator();

        while (var24.hasNext()) {
            PNPoint var13 = (PNPoint) var24.next();
            var23.addPoint(var13.getPos(), var13.getNeg());
        }

        System.out.println("\tsortAnyBURL & interpolation");
        var23.sort();
        var23.interpolate();
        System.out.println("\tcomputing AUCPR");
        return var23.calculateAUCPR(0.0D);
//        return var23.calculateAUCROC();
    }

    private Double aucPRListBackup(Set<Literal> groundTruth, VECollector collector) {
        Literal l = new Literal("", false, Sugar.listFromCollections(LogicUtils.constants(groundTruth)));
        Set<Pair<String, Integer>> predicates = LogicUtils.predicates(new Clause(groundTruth));
        Set<Literal> allPossibleLiterals = Sugar.set();
        /**/
        Matching world = Matching.create(new Clause(l), Matching.THETA_SUBSUMPTION);
//        System.out.println("constants introduction literal\t" + l);
//        System.out.println("gt is\t" + groundTruth);
        for (Pair<String, Integer> predicate : predicates) {
            Literal wanna = new Literal(predicate.r, IntStream.range(0, predicate.s).mapToObj(i -> Variable.construct("X" + i)).collect(Collectors.toList()));
//            System.out.println("wanna\t" + wanna);
            Pair<Term[], List<Term[]>> subt = world.allSubstitutions(new Clause(wanna.negation()), 0, Integer.MAX_VALUE);
            for (Term[] terms : subt.getS()) {
                Literal substituted = LogicUtils.substitute(wanna, subt.getR(), terms);
                allPossibleLiterals.add(substituted);
            }
        }/* * /
        for (Pair<String, Integer> predicate : predicates) {
            Combinatorics.variationsWithRepetition(, predicate.s);
            Literal wanna = new Literal(predicate.r, IntStream.range(0, predicate.s).mapToObj(i -> Variable.construct("X" + i)).collect(Collectors.toList()));
            System.out.println("wanna\t" + wanna);
            Pair<Term[], List<Term[]>> subt = world.allSubstitutions(new Clause(wanna), 0, Integer.MAX_VALUE);
            System.out.println(subt.s.size());
            for (Term[] terms : subt.getS()) {
                Literal substituted = LogicUtils.substitute(wanna, subt.getR(), terms);
                allPossibleLiterals.add(substituted);
            }
        }*/
        StringBuilder sb = new StringBuilder();
        for (Literal literal : allPossibleLiterals) {
            boolean gt = groundTruth.contains(literal);
            double weight = 0.0;
            if (collector.getVotes().keySet().contains(literal)) {
                weight = 0.9 * Math.min(1.0, collector.getVotes().get(literal));
            } else if (collector.getEvidence().contains(literal)) {
                weight = 1.0;
            }
            sb.append(weight + "\t" + (gt ? "1" : 0) + "\n");
        }
        int randomNumber;
        synchronized (randomGenerator) {
            randomNumber = randomGenerator.nextInt(100);
        }


//        System.out.println("davam mu'" + sb.toString() + "'");
        File temp = null;
        try {
            temp = File.createTempFile(System.nanoTime() + "_" + randomNumber + "_" + groundTruth.size(), ".tmp");
            Files.write(temp.toPath(), Sugar.list(sb.toString().trim()));

            Confusion var1 = ReadList.readFile(temp.getAbsolutePath(), "list");
            double val = var1.calculateAUCPR(0.0D);
            temp.delete();
//            double val = var1.calculateAUCROC() * (pPrime / p);
            return val;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }


    // ground truth is supposed to be masked
    private Double aucPRList2(Set<Literal> groundTruth, VECollector collector) {
        /*List<Pair<Double, Double>> pr = Sugar.union(collector.getVotes().counts(), Sugar.list(Double.MAX_VALUE - 1)).stream()
                .parallel()
                .map(t -> precisionRecall(groundTruth, collector.cut(VotesThresholdCut.get(t))))
                .sorted(Comparator.comparingDouble(Pair<Double, Double>::getR))
                .collect(Collectors.toList());
        Confusion confusion = null;//new Confusion();
        for (Pair<Double, Double> doubleDoublePair : pr) {
            confusion.addPoint(doubleDoublePair.randomGenerator, doubleDoublePair.s);
        }*/
        double p = groundTruth.size();
        double pPrime = 0;
        for (Literal literal : collector.getVotes().keySet()) {
            if (groundTruth.contains(literal)) {
                pPrime += 1;
            }
        }
        MutableInteger mi = new MutableInteger(0);
        StringBuilder sb = new StringBuilder();
        collector.getVotes().entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry<Literal, Double>::getValue).reversed())
                .forEach(e -> {
                    if (!collector.getEvidence().contains(e.getKey())) {
                        sb.append(Math.min(1.0, e.getValue()) * 0.9 + "\t" + (groundTruth.contains(e.getKey()) ? "1" : "0") + "\n");
                        mi.increment();
                    }

/*                    sb.append(Math.min(1.0, e.getValue()) + "\t" + (groundTruth.contains(e.getKey()) ? "1" : "0") + "\n");
                    if (!groundTruth.contains(e.getKey())) {
                        mi.increment();
                    }
                    */
                });

        if (0 == mi.value()) {
            double recall = Sugar.union(collector.getVotes().keySet(), collector.getEvidence()).size() / (1.0 * groundTruth.size());
            return recall; // because it is precision * recall but precision = 1 (there are no FP)
        }

        //collector.getEvidence().forEach(l -> sb.append((1.0 - 0.000001) + "\t1\n"));
        collector.getEvidence().forEach(l -> sb.append("1.0\t1\n"));
        int randomNumber;
        synchronized (randomGenerator) {
            randomNumber = randomGenerator.nextInt(100);
        }

        File temp = null;
        try {
            temp = File.createTempFile(System.nanoTime() + "_" + randomNumber + "_" + groundTruth.size(), ".tmp");
            Files.write(temp.toPath(), Sugar.list(sb.toString().trim()));

            Confusion var1 = ReadList.readFile(temp.getAbsolutePath(), "list");
            double val = var1.calculateAUCPR(0.0D) * (pPrime / p);
//            double val = var1.calculateAUCROC() * (pPrime / p);
            return val;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    private Double aucPR(Set<Literal> groundTruth, VECollector collector) {
        List<Pair<Double, Double>> pr = Sugar.union(collector.getVotes().counts(), Sugar.list(Double.MAX_VALUE - 1)).stream()
                .parallel()
                .map(t -> precisionRecall(groundTruth, collector.cut(VotesThresholdCut.create(t))))
                .sorted(Comparator.comparingDouble(Pair<Double, Double>::getR))
                .collect(Collectors.toList());
        Confusion confusion = null;//new Confusion();
        //todo here
        for (Pair<Double, Double> pair : pr) {
            confusion.addPoint(pair.r, pair.s);
        }
        double p = groundTruth.size();
        double pPrime = 0;
        for (Literal literal : collector.getVotes().keySet()) {
            if (groundTruth.contains(literal)) {
                pPrime += 1;
            }
        }
        MutableInteger mi = new MutableInteger(0);
        StringBuilder sb = new StringBuilder();
        collector.getVotes().entrySet().stream().sorted(Comparator.comparingDouble(Map.Entry<Literal, Double>::getValue).reversed())
                .forEach(e -> {
                    sb.append(Math.min(1.0, e.getValue()) + "\t" + (groundTruth.contains(e.getKey()) ? "1" : "0") + "\n");
                    if (!groundTruth.contains(e.getKey())) {
                        mi.increment();
                    }
                });

        if (0 == mi.value()) {
            double recall = Sugar.union(collector.getVotes().keySet(), collector.getEvidence()).size() / (1.0 * groundTruth.size());
            return recall; // because it is precision * recall but precision = 1 (there are no FP)
        }

        //collector.getEvidence().forEach(l -> sb.append((1.0 - 0.000001) + "\t1\n"));
        collector.getEvidence().forEach(l -> sb.append("1.0\t1\n"));
        int randomNumber;
        synchronized (randomGenerator) {
            randomNumber = randomGenerator.nextInt(100);
        }

        File temp = null;
        try {
            temp = File.createTempFile(System.nanoTime() + "_" + randomNumber + "_" + groundTruth.size(), ".tmp");
            Files.write(temp.toPath(), Sugar.list(sb.toString().trim()));

            Confusion var1 = ReadList.readFile(temp.getAbsolutePath(), "list");
            double val = var1.calculateAUCPR(0.0D) * (pPrime / p);
//            double val = var1.calculateAUCROC() * (pPrime / p);
            return val;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }


    private Pair<Double, Double> precisionRecall(Set<Literal> groundTruth, Set<Literal> entailed) {
        double tp = 0.0;
        double fp = 0.0;

        for (Literal literal : entailed) {
            if (groundTruth.contains(literal)) {
                tp += 1;
            } else {
                fp += 1;
            }
        }

        //double fn = groundTruth.size() - tp;

        double precision = tp / (tp + fp);
        double recall = tp / groundTruth.size();
        return new Pair<>(precision, recall);
    }


    public List<Data<Integer, Integer>> cumulate(List<Data<Integer, Integer>> data) {
        return data.stream().map(this::cumulative).collect(Collectors.toList());
    }

    public List<Data<Integer, Double>> cumulateD(List<Data<Integer, Double>> data) {
        return data.stream().map(this::cumulativeD).collect(Collectors.toList());
    }


    public Set<Predicate> predicates(Set<Literal> literals) {
        return literals.stream()
                .map(literal -> new Predicate(literal.predicate(), literal.arity()))
                .collect(Collectors.toSet());
    }

    public Set<Predicate> predicates(Path path) {
        return predicates(LogicUtils.loadEvidence(path));
    }

    public Set<Predicate> predicatesFromCuts(Collection<Cut> cuts) {
        return Sugar.parallelStream(cuts)
                .flatMap(cut -> cut.predicates().stream())
                .collect(Collectors.toSet());
    }

    public Set<Predicate> predicates(VECollector collector) {
        return predicates(collector.cut(KCut.K_CUT));
    }

    public Set<Predicate> predicates(Data<? extends Object, VECollector> data) {
        return predicates(data.getData().stream()
                .flatMap(pair -> pair.getS().cut(KCut.K_CUT).stream())
                .collect(Collectors.toSet()));
    }


    public MultiMap<Predicate, Double> votes(Data<Integer, VECollector> data, boolean verbose) {
        // can be paralelized somehow :))
        MultiMap<Predicate, Double> retVal = new MultiMap<>();
        data.getData().forEach(pair -> {
            DoubleCounters<Literal> votes = pair.getS().getVotes();
            votes.keySet().forEach(key -> retVal.put(new Predicate(key.predicate(), key.arity()), votes.get(key)));
        });

        if (verbose) {
            retVal.keySet().forEach(key -> System.out.println(key + "\n\t" + retVal.get(key).stream().sorted().collect(Collectors.toList())));
        }

        return retVal;
    }


    public void compare(Path first, Path second, int k) {
        compare(first, k, KCut.K_CUT, second, k, KCut.K_CUT);
    }

    public void compare(Path first, int firstK, Cut fCut, Path second, int secondK, Cut sCut) {
        Data<Integer, VECollector> fd = Data.loadResults(first, firstK);
        Data<Integer, VECollector> sd = Data.loadResults(second, secondK);
        IntStream.range(0, Math.min(fd.size(), sd.size()))
                .forEach(idx -> {
                    VECollector fData = fd.getData().get(idx).s.cutCollector(fCut, false);
                    VECollector sData = sd.getData().get(idx).s.cutCollector(sCut, false);
                    Set<Literal> fdAll = fData.cut(KCut.K_CUT);
                    Set<Literal> sdAll = sData.cut(KCut.K_CUT);
                    System.out.println("processing " + idx + "-th x coordinate\t" + fdAll.size() + "\t" + sdAll.size());

                    if (fData.getVotes().keySet().stream().anyMatch(literal -> literal.predicate().equals("complex"))) {
                        System.out.println("here complex/2");
                    }

                    boolean hardEvidenceSame = fData.getEvidence().equals(sData.getEvidence());
                    if (!hardEvidenceSame) {
                        System.out.println("\t hard evidence are not the same:\t" + fData.getEvidence().size() + "\t" + sData.getEvidence().size());
                    }

                    Set<Literal> firstMinusSecond = Sugar.setDifference(fdAll, sdAll);
                    Set<Literal> secondMinusFirst = Sugar.setDifference(fdAll, sdAll);
                    if (!firstMinusSecond.isEmpty()) {
                        System.out.println("\tsecond does not contain\t" + firstMinusSecond.size() + "\t" + firstMinusSecond);
                    }
                    if (!secondMinusFirst.isEmpty()) {
                        System.out.println("\tfirst does not contain\t" + secondMinusFirst.size() + "\t" + secondMinusFirst);
                    }
                });
    }

    public String plotConstantsSizes(List<Path> paths) {
        List<Data<Integer, Integer>> data = paths.stream()
                .map(path -> Data.loadResults(path, 0))
                .map(queries -> new Data<>(queries.getData().stream()
                        .map(pair -> new Pair<>(pair.r, pair.s.constantsSize()))
                        .collect(Collectors.toList())
                        , queries.getName()))
                .collect(Collectors.toList());
        return plot(data, "size of constants w.randomGenerator.t. evidence size (query number)");
    }

    // if null == predicate, than aggregation happens, otherwise pairwise for each predicate
    public String plotFPFN(Set<Literal> groundTruth, List<Data<Integer, VECollector>> data, boolean cumulative, Predicate predicate) {
        boolean mask = true;
        PredicateCut pCut = null == predicate ? PredicateCut.create((p) -> true) : PredicateCut.create(predicate);
        Set<Literal> predicateMaskedGT = Sugar.parallelStream(groundTruth).filter(literal -> pCut.isEntailed(literal, 0, 0)).collect(Collectors.toSet());
        List<Data<Integer, Pair<Integer, Integer>>> fpfn = Sugar.parallelStream(data)
                .map(predicted -> new Data<>(Sugar.parallelStream(predicted.getData())
                        .map(pair -> {
                            Pair<Integer, Integer> fpn = fpfn(mask
                                            ? mask(predicateMaskedGT, pair.s.getConstants())
                                            : predicateMaskedGT
                                    , Sugar.parallelStream(pair.getS().cut(KCut.K_CUT))
                                            .filter(literal -> pCut.isEntailed(literal, 0, 0)).collect(Collectors.toSet()));
                            return new Pair<>(pair.getR(), fpn);
                        }).collect(Collectors.toList())
                        , predicted.getName())
                ).collect(Collectors.toList());

        List<Data<Integer, Integer>> fp = Sugar.parallelStream(fpfn)
                .map(dato -> new Data<>(Sugar.parallelStream(dato.getData())
                        .map(fpn -> new Pair<>(fpn.getR(), fpn.getS().getR()))
                        .collect(Collectors.toList())
                        , "FP " + dato.getName().replace("_", ":").replace("\\", "\\textbackslash{}")))
                .collect(Collectors.toList());

        List<Data<Integer, Integer>> fn = Sugar.parallelStream(fpfn)
                .map(dato -> new Data<>(Sugar.parallelStream(dato.getData())
                        .map(fpn -> new Pair<>(fpn.getR(), fpn.getS().getS()))
                        .collect(Collectors.toList())
                        , "FN " + dato.getName().replace("_", ":").replace("\\", "\\textbackslash{}")))
                .collect(Collectors.toList());

        if (cumulative) {
            fp = cumulate(fp);
            fn = cumulate(fn);
        }

        /* whe you want to se either diff or sum for quickler noticion of differnce (it is good to use cumulative=false)
        List<Data<Integer, Integer>> finalFp = fp;
        List<Data<Integer, Integer>> finalFn = fn;
        List<Data<Integer, Integer>> sum = IntStream.range(0, fp.size()).mapToObj(idx -> sum(finalFp.get(idx), finalFn.get(idx))).collect(Collectors.toList());
        return plot(sum, (predicate + " sum").replace("_", ":").replace("\\", "\\textbackslash{}"));
        */

        return plot(fp, (predicate + " FP").replace("_", ":").replace("\\", "\\textbackslash{}"))
                + "\n\n"
                + plot(fn, (predicate + " FN").replace("_", ":").replace("\\", "\\textbackslash{}"));

    }

    public String plotFPFN(Path groundTruth, Pair<Path, Integer> predicted, List<Cut> cuts, boolean cumulative, Predicate predicate) {
        Data<Integer, VECollector> data = Data.loadResults(predicted);
        return plotFPFN(LogicUtils.loadEvidence(groundTruth)
                , Sugar.parallelStream(cuts)
                        .map(cut -> this.cut(data, cut))
                        .collect(Collectors.toList())
                , cumulative
                , predicate);
    }

    public String plotFPFN(Path groundTruth, Pair<Path, Integer> predicted, List<Cut> cuts, boolean cumulative) {
        Set<Literal> evidence = LogicUtils.loadEvidence(groundTruth);
        Set<Predicate> predicates = Sugar.union(predicates(evidence), predicatesFromCuts(cuts));
        Data<Integer, VECollector> data = Data.loadResults(predicted);
        List<Data<Integer, VECollector>> loaded = Sugar.parallelStream(cuts)
                .map(cut -> this.cut(data, cut))
                .collect(Collectors.toList());

        return Sugar.parallelStream(predicates)
                .map(predicate -> plotFPFN(evidence, loaded, cumulative, predicate))
                .collect(Collectors.joining("\n\n"));
    }


    public String plotFPFN(Path groundTruth, List<Data<Integer, VECollector>> predicted, boolean cumulative) {
        Set<Literal> evidence = LogicUtils.loadEvidence(groundTruth);
        Set<Predicate> predicates = Sugar.union(predicates(evidence), predicates(predicted));
        return Sugar.parallelStream(predicates)
                .map(predicate -> plotFPFN(evidence, predicted, cumulative, predicate))
                .collect(Collectors.joining("\n\n"));
    }


    private Set<Predicate> predicates(List<Data<Integer, VECollector>> datas) {
        return Sugar.parallelStream(datas)
                .flatMap(data -> predicates(data).stream())
                .collect(Collectors.toSet());
    }

    public void compareVE(Path first, Path second, int k) throws IOException {
        compareVE(first, second, k, Integer.MAX_VALUE);
    }

    public void compareVE(Path first, Path second, int k, int atMost) throws IOException {
        compareVE(first, second, k, 0, atMost);
    }

    public void compareVE(Path first, Path second, int k, int minimalFileNumber, int atMost) throws IOException {
        List<Path> streamFirst = Files.list(first)
                .filter(dir -> dir.toFile().isFile())
                .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                .filter(dir -> {
                    int fileNumber = evidenceFileNumber(dir);
                    return minimalFileNumber <= fileNumber && fileNumber <= atMost;
                }).collect(Collectors.toList());
        List<Path> streamSecond = Files.list(second)
                .filter(dir -> dir.toFile().isFile())
                .sorted(Comparator.comparingInt(TestRunner::evidenceFileNumber))
                .filter(dir -> {
                    int fileNumber = evidenceFileNumber(dir);
                    return minimalFileNumber <= fileNumber && fileNumber <= atMost;
                }).collect(Collectors.toList());

        for (int idx = 0; idx < Math.min(streamFirst.size(), streamSecond.size()); idx++) {
            Path f = streamFirst.get(idx);
            Path s = streamSecond.get(idx);

//            System.out.println("\t" + f + "\t" + s);

            if (evidenceFileNumber(f) != evidenceFileNumber(s)) {
                throw new IllegalStateException("cannot compare files with different names\n\t" + f + "\n\t" + s);
            }

            VECollector fdata = VECollector.load(f, k);
            VECollector sdata = VECollector.load(s, k);


//            System.out.println("\n\nfirst\t" + fdata.asOutput() + "\n\n");
//            System.out.println("\n\nsecond\t" + sdata.asOutput() + "\n\n");

            compareVE(f, s, fdata, sdata);

        }
    }

    public void compareVE(Path f, Path s, VECollector fdata, VECollector sdata) {
        if (!fdata.getEvidence().equals(sdata.getEvidence())) {
            System.out.println("comparing\t" + f + "\n\t" + s);
            System.out.println("problem ");
        }

        Set<Literal> keys = Sugar.union(fdata.getVotes().keySet(), sdata.getVotes().keySet());
        Set<Pair<Literal, Double>> inFirstNotInSecond = Sugar.set();
        Set<Pair<Literal, Double>> inSecondNotInFirst = Sugar.set();
        Set<Triple<Literal, Double, Double>> differentVotes = Sugar.set();

        for (Literal key : keys) {
            Double fVal = fdata.getVotes().get(key);
            Double sVal = sdata.getVotes().get(key);
            if (!fdata.getVotes().keySet().contains(key)) {
                inSecondNotInFirst.add(new Pair<>(key, sVal));
            } else if (!sdata.getVotes().keySet().contains(key)) {
                inFirstNotInSecond.add(new Pair<>(key, fVal));
            } else if (0 != Double.compare(fVal, sVal)) {
                differentVotes.add(new Triple<>(key, fVal, sVal));
            }
        }

        String name = "comparing\t" + f + "\t" + s;
        if (!inFirstNotInSecond.isEmpty()) {
            System.out.println(name + "\nin first not in second");
            name = "";
            inFirstNotInSecond.forEach(p -> System.out.println("\t" + p.r + "\t" + p.s));
        }

        if (!inSecondNotInFirst.isEmpty()) {
            System.out.println(name + "\nin second not in first");
            name = "";
            inSecondNotInFirst.forEach(p -> System.out.println("\t" + p.r + "\t" + p.s));
        }

        if (!differentVotes.isEmpty()) {
            System.out.println(name + "\ndifferent votes");
            name = "";
            differentVotes.forEach(t -> System.out.println("\t" + t.r + "\t" + t.s + "\t" + t.t));
        }
    }


    public <T> String canon(Collection<T> elements, String delimiter) {
        return canon(elements.stream(), delimiter);
    }

    public <T> String canon(Stream<T> elements, String delimiter) {
        return canon(elements, Object::toString, delimiter);
    }

    public <T> String canon(Stream<T> elements, java.util.function.Function<T, String> mapper, String delimiter) {
        return elements
                .map(mapper)
                .sorted()
                .collect(Collectors.joining(delimiter));
    }


    public <T extends Number> Pair<Data<Double, Integer>, Data<Double, Integer>> fpFnHistogram(Set<Literal> groundTruth, Data<T, VECollector> predicted, Predicate predicate) {
        groundTruth = groundTruth.parallelStream().filter(l -> l.getPredicate().equals(predicate.getPair())).collect(Collectors.toSet());
        Counters<Double> fp = new Counters<>();
        Counters<Double> fn = new Counters<>();

        PredicateCut pc = PredicateCut.create(predicate);

        predicted = new Data<>(predicted.getData().parallelStream()
                .map(pair -> new Pair<>(pair.getR(), pair.getS().cutCollector(pc, true)))
                , predicted.getName());

        Set<Literal> finalGroundTruth = groundTruth;
        List<Pair<Set<Literal>, VECollector>> masked = predicted.getData().parallelStream()
                .map(pair -> new Pair<>(finalGroundTruth.stream().filter(l -> pair.getS().getConstants().containsAll(LogicUtils.constantsFromLiteral(l))).collect(Collectors.toSet())
                        , pair.s))
                .collect(Collectors.toList());

        Set<Pair<Double, Double>> gammasAndRatios = Sugar.parallelStream(predicted.getData())
                .flatMap(pair -> votesAndGammas(pair.getS(), predicate).stream())
                .collect(Collectors.toSet());


        /*System.out.println(gammasAndRatios.stream().sorted(Comparator.comparingDouble(Pair::getS)).collect(Collectors.toList()));

        gammasAndRatios.clear();
        gammasAndRatios.add(new Pair<>(0.0, 0.0));
        gammasAndRatios.add(new Pair<>(0.0000001, 0.0000001));
        gammasAndRatios.add(new Pair<>(0.000001, 0.000001));
        gammasAndRatios.add(new Pair<>(0.00001, 0.00001));
        gammasAndRatios.add(new Pair<>(0.0001, 0.0001));
        gammasAndRatios.add(new Pair<>(0.001, 0.001));
        gammasAndRatios.add(new Pair<>(0.01, 0.01));
        gammasAndRatios.add(new Pair<>(0.1, 0.1));
        gammasAndRatios.add(new Pair<>(1.0, 1.0));

        System.out.println(gammasAndRatios);*/

//        groundTruth = groundTruth.stream().filter(l -> predicatesOnly.getConstants().containsAll(LogicUtils.constantsFromLiteral(l))).collect(Collectors.toSet());
        for (Pair<Double, Double> voteGamma : gammasAndRatios) {
//            for (Pair<T, VECollector> pair : predicted.getData()) {
//                VECollector collector = pair.getS();
            for (Pair<Set<Literal>, VECollector> groundTruthCollector : masked) {
                Set<Literal> gt = groundTruthCollector.getR();
                VECollector collector = groundTruthCollector.getS();
                Pair<Integer, Integer> fpfn = fpfn(gt, collector.cut(GammasCut.create(predicate, voteGamma.getS(), collector.getK())));
                fp.add(voteGamma.s, fpfn.getR());
                fn.add(voteGamma.s, fpfn.getS());
            }
        }

        return new Pair<>(counterToPlot(fp), counterToPlot(fn));
    }

    private Set<Pair<Double, Double>> votesAndGammas(VECollector collector, Predicate predicate) {
        return Sugar.parallelStream(collector.getVotes().entrySet())
                .filter(e -> e.getKey().getPredicate().equals(predicate.getPair()))
                .map(entry -> new Pair<>(entry.getValue()
                                , entry.getValue() / collector.getCToX(entry.getKey().terms().size(), collector.getK())
                        )
                ).collect(Collectors.toSet());
    }

    private <T extends Number> Data<T, Integer> counterToPlot(Counters<T> counter) {
        return new Data<>(Sugar.parallelStream(counter.keySet())
                .sorted()
                .map(key -> new Pair<>(key, counter.get(key)))
                , "");
    }

    public Set<Clause> loadClauses(Path path) {
        try {
            return Files.lines(path)
                    .filter(line -> line.trim().length() > 0
                            && !line.startsWith("#"))
                    .map(Clause::parse)
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("some problem occurred while parsing\t" + path);
    }

    public String scatterCuts(Path groundTruth, Path evidence, List<Triple<Path, Integer, List<Cut>>> data, int binSize) {
        return scatterCuts(groundTruth, evidence, data, binSize, Integer.MAX_VALUE);
    }

    public String scatterCuts(Path groundTruth, Path evidence, List<Triple<Path, Integer, List<Cut>>> data, int binSize, int atMostEvidence) {
        List<Data<Integer, VECollector>> loaded = data.stream()
                .flatMap(triple -> {
                    Data<Integer, VECollector> result = Data.loadResults(triple.r, triple.s);
                    Data<Integer, VECollector> finalResult = result.sublist(0, atMostEvidence, result.getName());
                    return triple.t.stream().map(cut -> cut(finalResult, cut));
                })
                .collect(Collectors.toList());
        return scatter(groundTruth, evidence, loaded, binSize);
    }


    public String scatter(Path groundTruth, Path evidence, List<Data<Integer, VECollector>> data, int binSize) {
        return scatter(LogicUtils.loadEvidence(groundTruth), Data.loadResults(evidence, 0), data, binSize);
    }

    private String scatter(Set<Literal> literals, Data<Integer, VECollector> evidence, List<Data<Integer, VECollector>> data, int binSize) {
        List<Data<Double, Pair<Double, Double>>> plots = data.stream()
                .map(dato -> scatter(literals, evidence, dato, binSize))
                .collect(Collectors.toList());
        return plotScatter(plots, "FP", "TP", "evidence size");
    }

    // zatim je to vsechno x=TP, y=FP, z=evidence size
    private Data<Double, Pair<Double, Double>> scatter(Set<Literal> groundTruth, Data<Integer, VECollector> evidence, Data<Integer, VECollector> prediction, int binSize) {
        // just a speed-up
        Map<Integer, VECollector> evidenceMap = evidence.getData().stream().collect(Collectors.toMap(Pair::getR, Pair::getS));

        List<Pair<Double, Pair<Double, Double>>> values = Sugar.list();

        for (int idx = 0; idx < prediction.size(); idx += binSize) {
            // this can be changed for other statistics
            double z = 0.0;
            int fp = 0;
            int tp = 0;

            for (int innerIdx = idx; innerIdx < Math.min(idx + binSize, prediction.getData().size() - 1); innerIdx++) {
                Pair<Integer, VECollector> predicted = prediction.getData().get(innerIdx);
                if (!evidenceMap.containsKey(predicted.getR())) {
                    throw new IllegalStateException("evidence not provided for prediction\t" + predicted.getR() + "\t" + prediction.getName());
                }
                VECollector originalEvidence = evidenceMap.get(predicted.getR());
                z += originalEvidence.originalEvidenceSize();
                Set<Literal> maskedGT = mask(groundTruth, predicted.getS().getConstants());
                Set<Literal> predictedLiterals = predicted.getS().cut(KCut.K_CUT);
                fp += Sugar.setDifference(predictedLiterals, maskedGT).size();
                // original evidence is supposed to by just the evidence, nothing more
                tp += Sugar.intersection(maskedGT, Sugar.setDifference(predictedLiterals, originalEvidence.getEvidence())).size();
            }

            z = z / binSize;
            double x = (fp * 1.0) / binSize;
            double y = (tp * 1.0) / binSize;
            values.add(new Pair<>(z, new Pair<>(x, y)));
        }

        return new Data<>(values, prediction.getName());
    }

    // data in form <Z,<X,Y>>
    private <T extends Number> String plotScatter(List<Data<T, Pair<T, T>>> plots, String xLabel, String ylabel, String zLabel) {
        StringBuilder sb = new StringBuilder();
        sb.append("\\begin{figure}\n" +
                "\t\\begin{tikzpicture}\n" +
                "\t\\begin{axis}[xlabel=" + xLabel + "\n\t\t\t,ylabel=" + ylabel + "\n\t\t\t,zlabel=" + zLabel + "\n\t\t\t,colorbar\n\t\t\t,scatter src=explicit\n\t\t\t,scatter\n\t\t\t,only marks\n\t\t\t,legend pos=outer north east\n\t\t\t,cycle list name=scatterMarks\n\t\t\t%,ymode=log\n\t\t\t%,xmode=log\n\t\t]\n");

        plots.forEach(plot -> {
            String coordinates = plot.getData().stream().map(p -> "(" + p.s.r.doubleValue() + "," + p.s.s.doubleValue() + ") [" + p.r.doubleValue() + "]").collect(Collectors.joining(" "));
            sb.append("\t\\addplot+ coordinates {" + coordinates + "};\n\t\\addlegendentry{" + plot.getName() + "}\n\n");
        });

        sb.append("\n\t\\end{axis}\n" +
                "\t\\end{tikzpicture}\n" +
                "\\end{figure}\n");
        return sb.toString();
    }

    public Map<Literal, Set<Literal>> loadHitsTest(Path path) {
        HashMap<Literal, Set<Literal>> retVal = new HashMap<>();
        Pair<Literal, Set<Literal>> cache = new Pair<>(null, null);
        try {
            Files.lines(path).filter(line -> line.trim().length() > 0)
                    .forEach(line -> {
                        if (line.startsWith("query")) {
                            if (null != cache.r) {
                                retVal.put(cache.r, cache.s);
                            }
                            Literal goal = Sugar.chooseOne(Clause.parse(line.split("\\s+", 2)[1]).literals());
                            cache.r = goal;
                            cache.s = Sugar.set(goal);
                        } else {
                            cache.s.add(Sugar.chooseOne(Clause.parse(line).literals()));
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (null != cache.r) {
            retVal.put(cache.r, cache.s);
        }
        return retVal;
    }


    public List<Pair<Literal, Double>> scoreHitsR(Path testLiterals, Path predictions) {
        Map<Literal, Set<Literal>> queries = loadHitsTest(testLiterals);
        Map<Literal, Integer> ranking = loadRanks(predictions);
        Random random = new Random();
        return Sugar.parallelStream(queries.entrySet())
                .flatMap(entry -> {
                    // this is definition from Hierarchical Random Walk Inference in Knowledge Graphs
                    Set<Literal> head = entry.getValue().stream().filter(l -> l.arguments()[0].equals(entry.getKey().arguments()[0])).collect(Collectors.toSet());
                    Set<Literal> tail = Sugar.setDifference(entry.getValue(), head);
                    // tady je chyba, spravne to ma byt ze vrati jeden rank z leveho-corrupted a pak druhy z praveho-corrupted
                    //return new Pair<>(entry.getKey(), (rankAmong(entry.getKey(), head, ranking, random) + rankAmong(entry.getKey(), tail, ranking, random)) / 2.0);
                    // takhle by to melo byt
                    double rankingL = rankAmong(entry.getKey(), head, ranking, random);
                    double rankingR = rankAmong(entry.getKey(), tail, ranking, random);
                    return Sugar.list(new Pair<>(entry.getKey(), rankingL), new Pair<>(entry.getKey(), rankingR)).stream();
                })
                .collect(Collectors.toList());
    }

    public List<Double> scoreHits(Map<Literal, Set<Literal>> testQueries, Map<Literal, Double> predictions, String mode) {
        if (!Sugar.set("randomEpsilon", "lexi", "mean", "randomTies").contains(mode)) {
            throw new IllegalArgumentException("unknown mode " + mode);
        }
        Random rnd = new Random();

        if ("randomEpsilon".equals(mode)) {
            predictions = predictions.entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue() + rnd.nextDouble() * 0.0001));
        }
        mode = "lexi";

        Map<Literal, Double> finalPredictions = predictions;
        String finalMode = mode;
        return Sugar.parallelStream(testQueries.entrySet())
                .map(entry -> {
                    // this definition comes from Hierarchical Random Walk Inference in Knowledge Graphs
                    Term first = entry.getKey().arguments()[0];
                    Set<Literal> head = entry.getValue().stream().filter(l -> l.arguments()[0].equals(first)).collect(Collectors.toSet());
                    Set<Literal> tail = Sugar.setDifference(entry.getValue(), head);
                    return (rankAmong(entry.getKey(), head, finalPredictions, rnd, finalMode) + rankAmong(entry.getKey(), tail, finalPredictions, rnd, finalMode)) / 2.0;
                })
                .collect(Collectors.toList());
    }

    public List<Double> scoreHits(Path testLiterals, Path predictions, String mode) {
        Map<Literal, Set<Literal>> queries = loadHitsTest(testLiterals);
        Map<Literal, Integer> ranking = loadRanks(predictions);
        Random random = new Random();
        List<Double> hits = computeRanks(queries, ranking, random);

        System.out.println("MRR\t" + (hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size()));
        for (Integer hit : Sugar.list(1, 3, 5, 10, 20, 30, 40, 50, 100)) {
            System.out.println("hits " + hit + "\t" + (hits.stream().filter(val -> val <= hit).count() * 1.0 / hits.size()));
        }
//        System.out.println("hits 1\t" + (hits.stream().filter(val -> val == 1).count() * 1.0 / hits.size()));
//        System.out.println("hits 3\t" + (hits.stream().filter(val -> val <= 3).count() * 1.0 / hits.size()));
//        System.out.println("hits 5\t" + (hits.stream().filter(val -> val <= 5).count() * 1.0 / hits.size()));
//        System.out.println("hits 10\t" + (hits.stream().filter(val -> val <= 10).count() * 1.0 / hits.size()));
//        System.out.println("hits 10\t" + (hits.stream().filter(val -> val <= 10).count() * 1.0 / hits.size()));
        return hits;
    }

    public void scoreHits(Path testLiterals, List<Path> predictions, List<Integer> limits, String mode) {
        Map<Literal, Set<Literal>> testQueries = loadHitsTest(testLiterals);
        DecimalFormat df2 = new DecimalFormat("#.###");

        List<String> result = predictions.stream().map(pred -> {
            List<Double> hits = scoreHits(testQueries, VECollector.load(pred, 0).getVotes().toMap(), mode);
            String results = limits.stream()
                    .map(border -> hits.stream().filter(val -> val <= border).count() * 1.0 / hits.size())
                    .map(d -> df2.format(d))
                    .collect(Collectors.joining("\t"));
            double mrr = hits.stream().mapToDouble(val -> 1.0 / val).sum() / hits.size();
//            System.out.println(mrr + "\t" + hits);
            return pred.toFile().getName() + "\t" + df2.format(mrr) + "\t" + results;// + "\n";
        }).collect(Collectors.toList());

        /*
        System.out.println("\n\nscore hits");
        System.out.println("MRR\t\t" + limits.stream().map(Object::toString).collect(Collectors.joining("\t\t")));
        result.forEach(System.out::println);
        */
        System.out.println(mode + "\t\t" + result.stream().collect(Collectors.joining("\t")));
    }


    private double rankAmong(Literal query, Set<Literal> corrupted, Map<Literal, Double> predicted, Random random, String mode) {
        List<Literal> sameLevel = Sugar.list();
        sameLevel.add(query);
        int better = 0;
        Double predictedValue = predicted.containsKey(query) ? predicted.get(query) : Double.MIN_VALUE;

        for (Literal literal : corrupted) {
            if (literal.equals(query)) {
                continue;
            }
            Double currentValue = predicted.containsKey(literal) ? predicted.get(literal) : Double.MIN_VALUE;
            if (currentValue > predictedValue) {
                better++;
            } else if (0 == Double.compare(currentValue, predictedValue)) {
                sameLevel.add(literal);
            }
        }

        switch (mode) {
            case "lexi":
                Collections.sort(sameLevel, Comparator.comparing(Object::toString)); // modified merge inside
                return better + 1 + sameLevel.indexOf(query);

            case "mean":
                System.out.println("really???? priklady si napsat a podle toho to oddebugovat");
                return ((better + 1) + (better + sameLevel.size())) / 2;

            case "randomTies":
                return better + 1 + random.nextInt(sameLevel.size());

            default:
                throw new IllegalStateException();
        }

    }


    public Map<String, Double> getRanks(Map<Literal, Set<Literal>> queries, Map<Literal, Integer> ranking, Random random) {
        Map<String, Double> retVal = new HashMap<>();
        queries.entrySet()
                .stream()
                .sorted(Comparator.comparing(o -> o.getKey().toString()))
                .forEach(entry -> {
                    Set<Literal> tail = entry.getValue().stream().filter(l -> l.get(0).equals(entry.getKey().get(0))).collect(Collectors.toSet());
                    Set<Literal> head = Sugar.setDifference(entry.getValue(), tail);
                    double r = rankAmong(entry.getKey(), head, ranking, random);
                    double l = rankAmong(entry.getKey(), tail, ranking, random);
                    retVal.put(entry.getKey() + "-h", r);
                    retVal.put(entry.getKey() + "-t", l);
                });
        return retVal;
    }

    public List<Double> computeRanks(Map<Literal, Set<Literal>> queries, Map<Literal, Integer> ranking, Random random) {
        throw new IllegalStateException();//NotImplementedException(); // just a quick place holder for debubg purpose, change back afterwards
        //return computeRanks(queries, ranking, random, null);
    }


    public List<Double> computeRanks(Map<Literal, Set<Literal>> queries, Map<Literal, Integer> ranking, Random random, Entailed predicted) {
        //return Sugar.parallelStream(queries.entrySet())
//        System.out.println(" debug tady! nezapomenout pridat pocitani tail ranku a dat ho i na vystup!");
//        List<String> collector = Sugar.list();
        return queries.entrySet()
                //List<Double> t = queries.entrySet()
                .stream()
                .sorted(Comparator.comparing(o -> o.getKey().toString()))
                //.filter(e -> e.getKey().toString().equals("affects(amino_acid_peptide_or_protein, biologic_function)"))
                .flatMap(entry -> {
//                    if(!ranking.containsKey(entry.getKey())){
//                        throw new IllegalStateException(); // this may happen when you want to evaluate a literal (using hits) without having it predicted at all
//                    }
                    // this definition comes from Hierarchical Random Walk Inference in Knowledge Graphs
                    Set<Literal> tail = entry.getValue().stream().filter(l -> l.get(0).equals(entry.getKey().get(0))).collect(Collectors.toSet());
                    Set<Literal> head = Sugar.setDifference(entry.getValue(), tail);
                    //double r = (rankAmong(entry.getKey(), head, ranking, random) + rankAmong(entry.getKey(), tail, ranking, random)) / 2.0;

//                    double r = (rankAmong(entry.getKey(), head, ranking, random) + rankAmong(entry.getKey(), tail, ranking, random)) / 2.0;
//                    if (r < 2.0) {
                    // System.out.println(entry.getKey() + "\thas rank\t" + r);
//                    }
//                    return r;
//                    return (rankAmong(entry.getKey(), head, ranking, random) + rankAmong(entry.getKey(), tail, ranking, random)) / 2.0;

                    // takhle je to ale spatne, ma se vratit rank z leveho-corrupted a pak z praveho corrupted
//                    System.out.println("head");
                    double r = rankAmong(entry.getKey(), head, ranking, random, predicted);
//                    System.out.println("tail");
//                    collector.add(entry.getKey() + "\t" + r);
                    double l = rankAmong(entry.getKey(), tail, ranking, random, predicted);
//                    System.out.println(entry.getKey() + "\t" + r + " " + l);
//                    return (r + l) / 2.0; this is bug (underestimating the true value)
//                    System.out.println(entry.getKey() + "\t" + r + "\t" + l);
//                    System.out.println(entry.getKey() + "H\t" + (int) r);
//                    System.out.println(entry.getKey() + "L\t" + (int) l);
//                    System.out.println(entry.getKey() + "\t" + r + "\t" + l);
//                    if (true) {
//                    if("adjacent_to(tissue, body_space_or_junction)".equals(entry.getKey().toString())){
                    //System.exit(-2);
//                    }
//                    return Sugar.list(r).stream();
                    return Sugar.list(r, l).stream();
                })
                .collect(Collectors.toList());

//        Collections.sort(collector);
//        collector.forEach(System.out::println);
//
//        return t;
    }

    public List<Double> computeRanksOptimized(Map<Triple<Integer, Integer, Integer>, Set<Triple<Integer, Integer, Integer>>> queries, Map<Triple<Integer, Integer, Integer>, Integer> ranking, Random random) {
        return queries.entrySet()
                .stream()
                .parallel()
//                .sorted(Comparator.comparing(o -> o.getKey().toString()))
                //.filter(e -> e.getKey().toString().equals("affects(amino_acid_peptide_or_protein, biologic_function)"))
                .flatMap(entry -> {
                    Set<Triple<Integer, Integer, Integer>> tail = entry.getValue().stream().filter(l -> l.getR().equals(entry.getKey().getR())).collect(Collectors.toSet());
                    Set<Triple<Integer, Integer, Integer>> head = Sugar.setDifference(entry.getValue(), tail);
                    // head
                    double r = rankAmongOptimized(entry.getKey(), head, ranking, random);
                    // tail
                    double l = rankAmongOptimized(entry.getKey(), tail, ranking, random);
                    return Sugar.list(r, l).stream();
                })
                .collect(Collectors.toList());

    }

    private double rankAmong(Literal query, Set<Literal> corrupted, Map<Literal, Integer> ranking, Random random) {
        throw new IllegalStateException();//NotImplementedException(); // just a place holder
    }

    private double rankAmong(Literal query, Set<Literal> corrupted, Map<Literal, Integer> ranking, Random random, Entailed ent) {
        int sameLevel = 1;
        int lowerLevel = 0;
        int queryRank = ranking.containsKey(query) ? ranking.get(query) : Integer.MAX_VALUE;

        /*
        List<Pair<String, Double>> v = Sugar.union(corrupted, Sugar.list(query)).stream().map(l -> {
            int currentRank = ranking.containsKey(l) ? ranking.get(l) : Integer.MAX_VALUE;
            return new Pair<>(l + "\t" + ent.getEntailedValue(l) + "\t" + currentRank, -ent.getEntailedValue(l));
        }).collect(Collectors.toList());
        v.stream().sorted(Comparator.comparing(p -> p.s))
                .map(p -> p.getR())
                .forEach(System.out::println);
        */

        for (Literal literal : corrupted) {
            if (query.equals(literal)) {
                continue;
            }
            int literalRank = ranking.containsKey(literal) ? ranking.get(literal) : Integer.MAX_VALUE;
            if (literalRank < queryRank) {
                lowerLevel++;
            } else if (literalRank == queryRank) {
                sameLevel++;
            }
        }

//        System.out.println(sameLevel + " " + lowerLevel);

        boolean deterministic = true;
        if (deterministic) {
            if (sameLevel % 2 == 0) {
                return lowerLevel + (sameLevel / 2) + 0.5;
            } else {
                return lowerLevel + ((sameLevel + 1) / 2);
            }
        } else { // ties broken at random
            if (sameLevel != 0) { // same level contains +1 for the target literal
                synchronized (random) {
                    return random.nextInt(sameLevel) + 1 + lowerLevel;
                }
            }
            return lowerLevel + 1;
        }
    }

    private double rankAmongOptimized(Triple<Integer, Integer, Integer> query, Set<Triple<Integer, Integer, Integer>> corrupted, Map<Triple<Integer, Integer, Integer>, Integer> ranking, Random random) {
        int sameLevel = 0;
        int lowerLevel = 1;
        int queryRank = ranking.containsKey(query) ? ranking.get(query) : Integer.MAX_VALUE;

        for (Triple<Integer, Integer, Integer> triple : corrupted) {
            if (query.equals(triple)) {
                continue;
            }
            int literalRank = ranking.containsKey(triple) ? ranking.get(triple) : Integer.MAX_VALUE;
            if (literalRank < queryRank) {
                lowerLevel++;
            } else if (literalRank == queryRank) {
                sameLevel++;
            }
        }

        boolean deterministic = true;
        if (deterministic) {
            return lowerLevel + sameLevel / 2.0;
        } else {
            throw new IllegalStateException();//NotImplementedException();
        }
    }


    // most likely obsolete!
    public Map<Literal, Integer> loadRanks(Path predictions) {
        Map<Literal, Integer> retVal = new HashMap<>();
        try {
            Files.list(predictions)
                    .filter(p -> p.toFile().getName().endsWith(".entailed"))
                    .forEach(path -> {
                        System.out.println(path);
                        String name = path.toFile().getName();
                        int rank = Integer.parseInt(name.substring(0, name.length() - ".entailed".length()));
                        LogicUtils.loadEvidence(path).forEach(literal -> {
                            /*if (!retVal.containsKey(literal)
                                    || (retVal.containsKey(literal) && retVal.get(literal) > rank)) {
                                retVal.put(literal, rank);
                            }*/
                            if (retVal.containsKey(literal)) {
//                                System.out.println("min\t" + literal + "\t" + rank + "\t" + retVal.get(literal));
                                retVal.put(literal, Math.min(rank, retVal.get(literal)));
                            } else {
//                                System.out.println("adding\t" + literal + "\t" + rank);
                                retVal.put(literal, rank);
                            }
                        });
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return retVal;
    }

    // this does not returns things that are in the evidence !
    public Map<Literal, Integer> loadRanks(VECollector predicted) {
        // simple as a chicken, just map the weights to integer and transforms VECollector accordingly
        Map<Double, Integer> weightToLevel = new HashMap<>();
        predicted.getVotes().counts().stream().distinct().sorted(Comparator.reverseOrder())
                .forEach(d -> weightToLevel.put(d, weightToLevel.size() + 1)); // tady nevim jestli to pocitat od jednicky nebo nuly, asi od jednicky
        return predicted.getVotes().entrySet().stream().collect(Collectors.toMap(e -> e.getKey(), e -> weightToLevel.get(e.getValue())));
    }

    // this is implemented because of large KG and whole embeddings model evaluation (it would not scale otherwise)
    // lteralsOfnterest contain all literals for which we want to compute unique ranks
    public Map<Literal, Integer> loadRanks(Entailed predicted, Set<Literal> literalsOfInterest) {
        // simple as a chicken, just map the weights to integer and transforms VECollector accordingly
        Map<Double, Integer> weightToLevel = new HashMap<>();
        Map<Literal, Double> literalToValue = new HashMap<>();
        Pair<Integer, Integer> size = new Pair<>(0, 0);
        literalsOfInterest.stream().map(literal -> {
//            if("accusation(china, uk)".equals(literal.toString())){
//                System.out.println("tady!");
//            }
            Double value = predicted.getEntailedValue(literal);
            if (null == value) {
                value = Double.MIN_VALUE + 10000;
            }
            literalToValue.put(literal, value); // caching so background interference wouldn't have to be executed multiple times (good embeddingScorer, not so awesome for train.log-txt way).
//            size.r = size.r + 1;
//            if (size.r % 100000 == 0) {
//                System.out.println(size.r / 100000);
//            }
            return value;
        }).distinct()
                .sorted(Comparator.reverseOrder())
                .forEach(value -> weightToLevel.put(value, weightToLevel.size() + 1)); // indexed from 1
        return literalsOfInterest.stream()
                .collect(Collectors.toMap(literal -> literal, literal -> weightToLevel.get(literalToValue.get(literal))));
    }

    public Map<Triple<Integer, Integer, Integer>, Integer> loadRanksOptimized(EntailedOptimized predicted, Set<Triple<Integer, Integer, Integer>> literalsOfInterest) {
        // simple as a chicken, just map the weights to integer and transforms VECollector accordingly
        Map<Double, Integer> weightToLevel = new HashMap<>();
        Map<Triple<Integer, Integer, Integer>, Double> literalToValue = new HashMap<>();
//        Pair<Integer, Integer> size = new Pair<>(0, 0);
        literalsOfInterest.stream().map(triple -> {
//            if(triple.equals(new Triple<>(3979, 160, 14933))){
//                System.out.println("debug here!");
//            }
            Double value = predicted.getEntailedValue(triple);
            if (null == value) {
                value = Double.MIN_VALUE + 10000;
            }
            literalToValue.put(triple, value); // caching so background interference wouldn't have to be executed multiple times (good embeddingScorer, not so awesome for train.log-txt way).
//            size.r = size.r + 1;
//            if (size.r % 100000 == 0) {
//                System.out.println(size.r / 100000);
//            }
            return value;
        }).distinct()
                .sorted(Comparator.reverseOrder())
                .forEach(value -> weightToLevel.put(value, weightToLevel.size() + 1)); // indexed from 1
        return literalsOfInterest.stream()
                .collect(Collectors.toMap(literal -> literal, literal -> weightToLevel.get(literalToValue.get(literal))));
    }


    public String plotPRs(Path groundEvidence, List<Pair<Path, Integer>> paths, int modulo, Integer precise) {
        int min = sharedMinimum(paths, (x) -> true);
        System.out.println("minimum\t" + min);
        java.util.function.Predicate<Pair<Integer, Path>> filter = null;
        if (null == precise) {
            filter = (p) -> p.r <= min && (p.r % modulo == 0);
        } else {
            filter = (p) -> p.r <= min && (Integer.compare(p.r, precise) == 0);
        }
        java.util.function.Predicate<Pair<Integer, Path>> finalFilter = filter;
        List<Data<Integer, VECollector>> loaded = paths.stream().map(p -> Data.loadDirectory(p.r, p.s, finalFilter, false)).collect(Collectors.toList());

        Set<Literal> evidence = LogicUtils.untype(LogicUtils.loadEvidence(groundEvidence));
        StringBuilder sb = new StringBuilder();
        for (int idx = 0; idx < loaded.get(0).size(); idx++) {
            List<Data<Number, Number>> plots = Sugar.list();
            for (Data<Integer, VECollector> data : loaded) {
                plots.add(createPRCurve(evidence, data.getData().get(idx).getS().untype(), data.getName()));
            }
            sb.append("idx\t" + idx + "\n\n" + plot(plots, "PR " + (modulo * (1 + idx))));
        }


        return sb.toString();

    }

    public Data<Number, Number> createPRCurve(Set<Literal> evidence, VECollector collector, String name) {
        return createPRCurve(evidence, collector, name, true); // a quick hack for backward compatability
    }

    public Data<Number, Number> createTestPRCurveExact(Set<Literal> train, Set<Literal> valid, Set<Literal> test, Entailed collector, String name) {
        Set<Pair<Number, Number>> points = Sugar.set();

        //System.out.println(collector.getVotes().counts().size() + "\t" + Sugar.setFromCollections(collector.getVotes().counts()).size());
        /*if (collector.getVotes().counts().isEmpty()) {
            return new Data<>(Sugar.list(), "PR\t" + name);
        }*/

        MultiMap<Double, Literal> map = new MultiMap<>();
        if (!(collector instanceof VECollector)) {
            throw new IllegalStateException();//NotImplementedException();
        }
        VECollector veCollector = (VECollector) collector;
        veCollector.getVotes().entrySet().stream()
                .filter(e -> !train.contains(e.getKey()) && !valid.contains(e.getKey())) // most likely redundant, but better be save than sorry
                .forEach(e -> map.put(e.getValue(), e.getKey()));

        if (map.keySet().isEmpty()) {
            return new Data<>(Sugar.list(new Pair<>(0.0, 0.0)), "PR\t" + name);
        }

        double max = map.keySet().stream().mapToDouble(d -> d).max().orElseThrow(IllegalStateException::new);
        List<Double> thresholds = Sugar.listFromCollections(Sugar.list(max + 1), map.keySet());
        Collections.sort(thresholds);
        Collections.reverse(thresholds);

        long tp = 0;
        long fp = 0;
        for (Double threshold : thresholds) {
            for (Literal literal : map.get(threshold)) {
                if (valid.contains(literal) || train.contains(literal)) {
                    throw new IllegalStateException();
                } else if (test.contains(literal)) {
//                    System.out.println("tp increase!\t" + tp);
                    tp += 1;
                } else {
                    fp += 1;
                }
            }

            if (tp + fp > 0) {

                double precision = (1.0 * tp) / (tp + fp); // jina by to zapocitavalo i train a valid data :(
//                long fn = test.size() - tp;
//                double recall = (1.0 * tp) / (tp + fn);
                double recall = (1.0 * tp) / test.size();


                if (true) { // rounding
                    int decimalPlaces = 3;
                    precision = new BigDecimal(precision).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
                    recall = new BigDecimal(recall).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
                }

                points.add(new Pair<>(recall, precision));
            }
        }
        List<Pair<Number, Number>> listPoints = Sugar.listFromCollections(points);
        listPoints.sort(Comparator.comparing(p -> p.getR().doubleValue()));


        if (true) { // removing same precision values
            List<Pair<Number, Number>> list = Sugar.list();
            Pair<Number, Number> previous = null;
            int index = 0;
            for (Pair<Number, Number> point : listPoints) {
                index++;
                if (index < 15 || index + 15 > listPoints.size()) {
                    list.add(point); // add first and last few points as they are
                } else if (list.isEmpty()) {
                    list.add(point);
                    previous = null;
                } else if (null == previous) {
                    previous = point;
                } else {
                    Pair<Number, Number> last = list.get(list.size() - 1);
                    if (Double.compare(last.s.doubleValue(), previous.s.doubleValue()) == 0
                            && Double.compare(point.s.doubleValue(), previous.s.doubleValue()) == 0) {
                        previous = point;
                    } else {
                        list.add(point);
                        previous = null;
                    }
                }
            }
            if (null != previous) {
                list.add(previous);
            }
            //System.out.println("sizes\t" + listPoints.size() + "\t" + list.size());
            listPoints = list;
        }

        return new Data<>(listPoints, "PR\t" + name);
    }

    public Data<Number, Number> createTestPRCurveExact(Set<Triple<Integer, Integer, Integer>> train, Set<Triple<Integer, Integer, Integer>> valid, Set<Triple<Integer, Integer, Integer>> test, EntailedOptimized collector, String name) {
        Set<Pair<Number, Number>> points = Sugar.set();

        //System.out.println(collector.getVotes().counts().size() + "\t" + Sugar.setFromCollections(collector.getVotes().counts()).size());
        /*if (collector.getVotes().counts().isEmpty()) {
            return new Data<>(Sugar.list(), "PR\t" + name);
        }*/

        MultiMap<Double, Triple<Integer, Integer, Integer>> map = new MultiMap<>();
        if (!(collector instanceof VECollectorOptimized)) {
            throw new IllegalStateException();//NotImplementedException();
        }
        VECollectorOptimized veCollector = (VECollectorOptimized) collector;
        veCollector.getVotes().entrySet().stream()
                .filter(e -> !train.contains(e.getKey()) && !valid.contains(e.getKey())) // most likely redundant, but better be save than sorry
                .forEach(e -> map.put(e.getValue(), e.getKey()));


        double max = map.keySet().stream().mapToDouble(d -> d).max().orElseThrow(IllegalStateException::new);
        List<Double> thresholds = Sugar.listFromCollections(Sugar.list(max + 1), map.keySet());
        Collections.sort(thresholds);
        Collections.reverse(thresholds);

        long tp = 0;
        long fp = 0;
        for (Double threshold : thresholds) {
            for (Triple<Integer, Integer, Integer> triple : map.get(threshold)) {
                if (valid.contains(triple) || train.contains(triple)) {
                    throw new IllegalStateException();
                } else if (test.contains(triple)) {
//                    System.out.println("tp increase!\t" + tp);
                    tp += 1;
                } else {
                    fp += 1;
                }
            }

            if (tp + fp > 0) {

                double precision = (1.0 * tp) / (tp + fp); // jina by to zapocitavalo i train a valid data :(
//                long fn = test.size() - tp;
//                double recall = (1.0 * tp) / (tp + fn);
                double recall = (1.0 * tp) / test.size();


                if (true) { // rounding
                    int decimalPlaces = 3;
                    precision = new BigDecimal(precision).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
                    recall = new BigDecimal(recall).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
                }

                points.add(new Pair<>(recall, precision));
            }
        }
        List<Pair<Number, Number>> listPoints = Sugar.listFromCollections(points);
        listPoints.sort(Comparator.comparing(p -> p.getR().doubleValue()));


        if (true) { // removing same precision values
            List<Pair<Number, Number>> list = Sugar.list();
            Pair<Number, Number> previous = null;
            int index = 0;
            for (Pair<Number, Number> point : listPoints) {
                index++;
                if (index < 15 || index + 15 > listPoints.size()) {
                    list.add(point); // add first and last few points as they are
                } else if (list.isEmpty()) {
                    list.add(point);
                    previous = null;
                } else if (null == previous) {
                    previous = point;
                } else {
                    Pair<Number, Number> last = list.get(list.size() - 1);
                    if (Double.compare(last.s.doubleValue(), previous.s.doubleValue()) == 0
                            && Double.compare(point.s.doubleValue(), previous.s.doubleValue()) == 0) {
                        previous = point;
                    } else {
                        list.add(point);
                        previous = null;
                    }
                }
            }
            if (null != previous) {
                list.add(previous);
            }
            //System.out.println("sizes\t" + listPoints.size() + "\t" + list.size());
            listPoints = list;
        }

        return new Data<>(listPoints, "PR\t" + name);
    }


    public Data<Number, Number> createTestPRCurveExactOnline(Set<Triple<Integer, Integer, Integer>> train, Set<Triple<Integer, Integer, Integer>> valid, Set<Triple<Integer, Integer, Integer>> test, EntailedOptimized collector, String name) {
        Set<Pair<Number, Number>> points = Sugar.set();

        //System.out.println(collector.getVotes().counts().size() + "\t" + Sugar.setFromCollections(collector.getVotes().counts()).size());
        /*if (collector.getVotes().counts().isEmpty()) {
            return new Data<>(Sugar.list(), "PR\t" + name);
        }*/

        MultiMap<Double, Triple<Integer, Integer, Integer>> map = new MultiMap<>(MultiMap.CONCURENT_HASH_SET);

        Pair<Integer, Integer> counter = new Pair<>(0, 0);
        Set<Triple<Integer, Integer, Integer>> forbidden = Sugar.union(train, valid, test);
        Set<Integer> entities = forbidden.stream().flatMap(triple -> Sugar.list(triple.r, triple.t).stream()).collect(Collectors.toSet());
        test.forEach(triple -> {
            System.out.println(counter.r + "\t" + test.size());
            counter.r = counter.r + 1;
            Double value = collector.getEntailedValue(triple);
            if (null == value) {
                value = Double.MIN_VALUE;
            }
            map.put(value, triple);
            entities.stream().parallel().forEach(entity -> {
                Triple<Integer, Integer, Integer> corruptedHead = new Triple<>(entity, triple.s, triple.t);
                if (!forbidden.contains(corruptedHead)) {
                    Double val = collector.getEntailedValue(corruptedHead);
                    val = null == val ? Double.MAX_VALUE : val;
                    map.put(val, corruptedHead);
                }

                Triple<Integer, Integer, Integer> corruptedTail = new Triple<>(triple.r, triple.s, entity);
                if (!forbidden.contains(corruptedTail)) {
                    Double val = collector.getEntailedValue(corruptedTail);
                    val = null == val ? Double.MAX_VALUE : val;
                    map.put(val, corruptedTail);
                }
            });
        });

//        System.out.println(map.keySet().size() + "\t is number of keys!");
//        System.out.println(map.values().stream().flatMap(l -> l.stream()).distinct().count() + "\t is number of unique values");

        double max = map.keySet().stream().mapToDouble(d -> d).max().orElseThrow(IllegalStateException::new);
        List<Double> thresholds = Sugar.listFromCollections(Sugar.list(max + 1), map.keySet());
        Collections.sort(thresholds);
        Collections.reverse(thresholds);

        long tp = 0;
        long fp = 0;
        for (Double threshold : thresholds) {
            for (Triple<Integer, Integer, Integer> triple : map.get(threshold)) {
                if (valid.contains(triple) || train.contains(triple)) {
                    throw new IllegalStateException();
                } else if (test.contains(triple)) {
//                    System.out.println("tp increase!\t" + tp);
                    tp += 1;
                } else {
                    fp += 1;
                }
            }

            if (tp + fp > 0) {

                double precision = (1.0 * tp) / (tp + fp); // jina by to zapocitavalo i train a valid data :(
//                long fn = test.size() - tp;
//                double recall = (1.0 * tp) / (tp + fn);
                double recall = (1.0 * tp) / test.size();


                if (true) { // rounding
                    int decimalPlaces = 3;
                    precision = new BigDecimal(precision).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
                    recall = new BigDecimal(recall).setScale(decimalPlaces, BigDecimal.ROUND_HALF_UP).doubleValue();
                }

                points.add(new Pair<>(recall, precision));
            }
        }
        List<Pair<Number, Number>> listPoints = Sugar.listFromCollections(points);
        listPoints.sort(Comparator.comparing(p -> p.getR().doubleValue()));


        if (true) { // removing same precision values
            List<Pair<Number, Number>> list = Sugar.list();
            Pair<Number, Number> previous = null;
            int index = 0;
            for (Pair<Number, Number> point : listPoints) {
                index++;
                if (index < 20 || index + 20 > listPoints.size()) {
                    list.add(point); // add first and last few points as they are
                } else if (list.isEmpty()) { // this is most likely redundant due to te first and last batch
                    list.add(point);
                    previous = null;
                } else if (null == previous) {
                    previous = point;
                } else {
                    Pair<Number, Number> last = list.get(list.size() - 1);
                    if (Double.compare(last.s.doubleValue(), previous.s.doubleValue()) == 0 // this is kind of weird, most likely a bug since only .s should be compared (but not twice)
                            && Double.compare(point.s.doubleValue(), previous.s.doubleValue()) == 0) {
                        previous = point;
                    } else {
                        list.add(point);
                        previous = null;
                    }
                }
            }
            if (null != previous) {
                list.add(previous);
            }
            //System.out.println("sizes\t" + listPoints.size() + "\t" + list.size());
            listPoints = list;
        }

        return new Data<>(listPoints, "PR\t" + name);
    }


    public Data<Number, Number> createTestPRCurve(Set<Literal> train, Set<Literal> valid, Set<Literal> test, VECollector collector, String name, boolean mask) {
        if (mask) {
            throw new IllegalStateException();//NotImplementedException();
//            evidence = mask(evidence, collector.getConstants());
        }
        List<Pair<Number, Number>> points = Sugar.list();

        System.out.println(collector.getVotes().counts().size() + "\t" + Sugar.setFromCollections(collector.getVotes().counts()).size());
        if (collector.getVotes().counts().isEmpty()) {
            return new Data<>(Sugar.list(), "PR\t" + name);
        }

        double max = collector.getVotes().counts().stream().mapToDouble(d -> d).max().orElseThrow(IllegalStateException::new);
        double min = collector.getVotes().counts().stream().mapToDouble(d -> d).min().orElseThrow(IllegalStateException::new);
        Set<Double> thresholds = Sugar.union(collector.getVotes().counts(), max + 1, min - 1);
        int maxThresholds = 1000;
        if (thresholds.size() > maxThresholds) {
            List<Double> list = Sugar.listFromCollections(thresholds);
            list.sort(Double::compareTo);


            if (false) {// vezmi jen prvni
                thresholds = Sugar.setFromCollections(list.subList(list.size() - 1000, list.size() - 1));
            } else { // subsampling
                Set<Double> sampled = Sugar.set();
                sampled.add(max + 1);
                sampled.add(min - 1);
                sampled.addAll(list.subList(list.size() - 1000, list.size() - 1));
            /*int step = thresholds.size() / maxThresholds;
            for (int where = 0; where < list.size(); where += step) {
                sampled.add(list.get(where));
            }*/
                double step = (1.0 * thresholds.size()) / maxThresholds;
                for (int where = 0; where < maxThresholds; where++) {
                    sampled.add(list.get((int) step * where));
                }
                sampled.add(list.get(list.size() - 1));
                thresholds = sampled;
            }
        }
        List<Triple<Double, Double, Double>> triplets = Sugar.list();
        System.out.println(thresholds.size());
        // embarrsingly naive!
        thresholds.stream().sorted().forEach(value -> {
            VotesThresholdCut cut = VotesThresholdCut.create(value);
            Set<Literal> cutEntailed = collector.cut(cut);
            int tp = 0;
            int fp = 0;
            for (Literal lit : cutEntailed) {
                if (valid.contains(lit) || train.contains(lit)) {
                    continue;
                } else if (test.contains(lit)) {
                    tp += 1;
                } else {
                    fp += 1;
                }
            }

            // puvodni
//            double precision = (1.0 * tp) / cutEntailed.size();
//            double recall = (1.0 * tp) / finalEvidence.size();
            // oprava? co je vlastne tp a fn?, definice FP, TP, recall a precision!

            if (tp + fp > 0) { // removing nans
                double precision = (1.0 * tp) / (tp + fp); // jina by to zapocitavalo i train a valid data :(
                int fn = test.size() - tp;
                double recall = (1.0 * tp) / (tp + fn);
                points.add(new Pair<>(recall, precision));
            }
//            triplets.add(new Triple<>(recall, precision, value));
        });

        points.sort(Comparator.comparing(p -> p.getR().doubleValue()));
//      removing 0.0
        List<Pair<Number, Number>> removedDoubles = Sugar.list();
        points.forEach(pair -> {
            if (!removedDoubles.isEmpty()) {
                Pair<Number, Number> last = removedDoubles.get(removedDoubles.size() - 1);
                if (Double.compare(last.getR().doubleValue(), pair.getR().doubleValue()) == 0
                        && Double.compare(last.getS().doubleValue(), pair.getS().doubleValue()) == 0) {
                    //skip
                } else {
                    removedDoubles.add(pair);
                }
            } else {
                removedDoubles.add(pair);
            }
        });

//        triplets.sortAnyBURL(Comparator.comparing(p -> -p.getR().doubleValue()));
//        System.out.println("triplets for debug");
//        System.out.println("[" + triplets.stream().map(t -> t.getS().toString()).collect(Collectors.joining("\t")) + "]");
//        System.out.println("[" + triplets.stream().map(t -> t.getR().toString()).collect(Collectors.joining("\t")) + "]");
//        System.out.println("[" + triplets.stream().map(t -> t.getT().toString()).collect(Collectors.joining("\t")) + "]");

        return new Data<>(points, "PR\t" + name);
    }


    public Data<Number, Number> createPRCurve(Set<Literal> evidence, VECollector collector, String name, boolean mask) {
        /*if (true) {
            System.out.println("there is most likely a bug in computing recall -- we need positive values as well (or at least the number)");
            throw new IllegalStateException();
        }*/
        if (mask) {
            evidence = mask(evidence, collector.getConstants());
        }
        /*Literal l = new Literal("", false, Sugar.listFromCollections(collector.getConstants()));
        Set<Pair<String, Integer>> predicates = LogicUtils.predicates(new Clause(Sugar.iterable(evidence, collector.cut(KCut.K_CUT))));
        Set<Literal> allPossibleLiterals = Sugar.set();
        Matching world = Matching.get(new Clause(l), Matching.THETA_SUBSUMPTION);
//        System.out.println("constants introduction literal\t" + l);
//        System.out.println("gt is\t" + groundTruth);
        for (Pair<String, Integer> predicate : predicates) {
            Literal wanna = new Literal(predicate.r, IntStream.range(0, predicate.s).mapToObj(i -> Variable.construct("X" + i)).collect(Collectors.toList()));
//            System.out.println("wanna\t" + wanna);
            Pair<Term[], List<Term[]>> subt = world.allSubstitutions(new Clause(wanna.negation()), 0, Integer.MAX_VALUE);
            for (Term[] terms : subt.getS()) {
                Literal substituted = LogicUtils.substitute(wanna, subt.getR(), terms);
                allPossibleLiterals.add(substituted);
            }
        }*/
        List<Pair<Number, Number>> points = Sugar.list();

        Set<Literal> finalEvidence = evidence;
        System.out.println(collector.getVotes().counts().size() + "\t" + Sugar.setFromCollections(collector.getVotes().counts()).size());
        Set<Double> thresholds = Sugar.union(collector.getVotes().counts(), 1.1);
        int maxThresholds = 3000;
        if (thresholds.size() > maxThresholds) {
            List<Double> list = Sugar.listFromCollections(thresholds);
            list.sort(Double::compareTo);
            Set<Double> sampled = Sugar.set();
            /*int step = thresholds.size() / maxThresholds;
            for (int where = 0; where < list.size(); where += step) {
                sampled.add(list.get(where));
            }*/
            double step = (1.0 * thresholds.size()) / maxThresholds;
            for (int where = 0; where < maxThresholds; where++) {
                sampled.add(list.get((int) step * where));
            }
            sampled.add(list.get(list.size() - 1));
            thresholds = sampled;
        }
        System.out.println(thresholds.size());
        thresholds.stream().sorted().forEach(value -> {
            VotesThresholdCut cut = VotesThresholdCut.create(value);
            Set<Literal> cutEntailed = collector.cut(cut);
            int tp = 0;
            int fn = 0;
            for (Literal lit : cutEntailed) {
                if (finalEvidence.contains(lit)) {
                    tp += 1;
                } else {
                    fn += 1;
                }
            }

            // puvodni
            double precision = (1.0 * tp) / cutEntailed.size();
            double recall = (1.0 * tp) / finalEvidence.size();
            // oprava? co je vlastne tp a fn?, definice FP, TP, recall a precision!
//            double precision = (1.0 * tp) / cutEntailed.size(); // tady to vypada na chybu
//            double recall = (1.0 * tp) / cutEntailed.size(); the correct version would incorporate masked evidence w.r.t. constants in the subsample :) coz by mel delat ten masked=true
            points.add(new Pair<>(recall, precision));
        });

        points.sort(Comparator.comparing(p -> p.getR().doubleValue()));

        return new Data<>(points, "PR\t" + name);
    }

    public Map<Literal, Set<Literal>> generateHitsCompanion(Set<Literal> literals, Set<Literal> forbidden, Set<String> entities) {
        Map<Literal, Set<Literal>> result = new HashMap<>();
        literals.forEach(literal -> {
            Set<Literal> set = Sugar.set();
            entities.forEach(entity -> {
                Literal corruptedRight = new Literal(literal.predicate(), Sugar.list(literal.get(0), new Constant(entity)));
                if (!forbidden.contains(corruptedRight)) {
                    set.add(corruptedRight);
                }
                Literal corruptedLeft = new Literal(literal.predicate(), Sugar.list(new Constant(entity), literal.get(1)));
                if (!forbidden.contains(corruptedLeft)) {
                    set.add(corruptedLeft);
                }
            });
            result.put(literal, set);
        });

        return result;
    }

    public Map<Triple<Integer, Integer, Integer>, Set<Triple<Integer, Integer, Integer>>> generateHitsCompanionTriplets(Set<Triple<Integer, Integer, Integer>> literals, Set<Triple<Integer, Integer, Integer>> forbidden, Set<Integer> entities) {
//        Triple<Integer, Integer, Integer> debugTriple = new Triple<>(0, 0, 100);
//        System.out.println("debug, corrupting only the triple\t" + debugTriple);

        Map<Triple<Integer, Integer, Integer>, Triple<Integer, Integer, Integer>> cache = new HashMap<>();
        Map<Triple<Integer, Integer, Integer>, Set<Triple<Integer, Integer, Integer>>> result = new HashMap<>();
        literals
                .stream()
                .parallel()
//                .filter(triple -> triple.equals(debugTriple))
                .forEach(triplet -> {
                    Set<Triple<Integer, Integer, Integer>> set = Sugar.set();
                    entities.forEach(entity -> {
                        //Literal corruptedRight = new Literal(literal.predicate(), Sugar.list(literal.get(0), new Constant(entity)));
                        Triple<Integer, Integer, Integer> corruptedRight = new Triple<>(triplet.r, triplet.s, entity);
                        Triple<Integer, Integer, Integer> corruptedLeft = new Triple<>(entity, triplet.s, triplet.t);
                        if (!forbidden.contains(corruptedRight)) {
                            synchronized (cache) {
                                if (cache.containsKey(corruptedRight)) {
                                    corruptedRight = cache.get(corruptedRight);
                                } else {
                                    cache.put(corruptedRight, corruptedRight);
                                }
                            }
                            set.add(corruptedRight);
                        }
//                Literal corruptedLeft = new Literal(triplet.predicate(), Sugar.list(new Constant(entity), triplet.get(1)));
                        if (!forbidden.contains(corruptedLeft)) {
                            synchronized (cache) {
                                if (cache.containsKey(corruptedLeft)) {
                                    corruptedLeft = cache.get(corruptedLeft);
                                } else {
                                    cache.put(corruptedLeft, corruptedLeft);
                                }
                            }
                            set.add(corruptedLeft);
                        }
                    });
                    result.put(triplet, set);
                });

        return result;
    }

    public double computeRank(EntailedOptimized embedding, Triple<Integer, Integer, Integer> triplet, Set<Triple<Integer, Integer, Integer>> forbidden, Set<Integer> entities, boolean corruptTail) {
        Double groundValue = embedding.getEntailedValue(triplet);
        int sameLevel = 0;
        int betterLevel = 1;
        for (Integer entity : entities) {
            Triple<Integer, Integer, Integer> corrupted = corruptTail ? new Triple<>(triplet.r, triplet.getS(), entity) : new Triple<>(entity, triplet.getS(), triplet.t);
            if (!forbidden.contains(corrupted)) {
                Double currentValue = embedding.getEntailedValue(corrupted);
                if (0 == Double.compare(currentValue, groundValue)) {
                    sameLevel++;
                } else if (currentValue > groundValue) {
                    betterLevel++;
                }
            }
        }
        return betterLevel + sameLevel / 2.0;
    }
}
