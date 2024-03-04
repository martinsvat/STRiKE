package ida.pacReasoning.entailment;

import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.entailment.cuts.*;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.evaluation.Data;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Sugar;
import ida.utils.collections.DoubleCounters;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 21. 6. 2018.
 */
public class GammaFitter {

    public static final int GAMMAS = 1;
    public static final int RATIOS_MIN = 2;
    public static final int RATIOS_AVG = 3;
    public static final int RATIOS_MED = 4;
    public static final int RATIOS_MAX = 5;
    public static final int CONSTANT_RATIO = 6;

    private final List<Predicate> predicatesToBeFitted;
    private final boolean learnAll;
    private final int mode;

    public GammaFitter(List<Predicate> predicates, int mode) {
        this.predicatesToBeFitted = predicates;
        this.learnAll = null == predicates;
        this.mode = mode;
    }

    // this can be further parametrized by using different method for finding best gammas
    public BinCut fit(Path groundTruth, Pair<Path, Integer> prediction, int evidenceBinSize) {
        return fit(groundTruth, Data.loadResults(prediction), evidenceBinSize);
    }

    public BinCut fit(Path groundTruth, Data<Integer, VECollector> data, int evidenceBinSize) {
        boolean maskGT = true;
        Utils utils = Utils.create();
        Set<Literal> evidence = LogicUtils.loadEvidence(groundTruth);

        Set<Predicate> predicates = Sugar.union(utils.predicates(data), utils.predicates(evidence));
        int k = data.getData().get(0).s.getK();

        List<Cut> cuts = Sugar.list();

        //evidenceBinSize = CONSTANT_RATIO == mode ? Integer.MAX_VALUE : evidenceBinSize;
        if (CONSTANT_RATIO == mode) {
            MultiMap<Predicate, Double> dataGammas = utils.gammas(data, false);
            Map<Pair<String, Integer>, List<Pair<Integer, Double>>> ratios = Sugar.parallelStream(predicates, true)
                    .filter(predicate -> learnAll || predicatesToBeFitted.contains(predicate))
                    .filter(predicate -> dataGammas.containsKey(predicate) && dataGammas.get(predicate) != null && !dataGammas.get(predicate).isEmpty())
                    .collect(Collectors.toMap(Predicate::getPair, predicate -> bestConstantRatio(evidence, data, predicate)));

            cuts.add(ConstantRatioCut.create(ratios, k, data.getName() + " w.r.t. " + groundTruth));
            return BinCut.create(cuts, evidenceBinSize, k, data.getName() + " w.r.t. " + groundTruth);
        }

        for (int binStart = 1; binStart < data.size(); binStart += evidenceBinSize) {

            //int max = Math.min(binStart + evidenceBinSize, data.size());
            int max = (Integer.MAX_VALUE == evidenceBinSize) ? Integer.MAX_VALUE : binStart + evidenceBinSize;
            System.out.println("solving bin\t" + binStart + "-" + max);
            //data<Integer, VECollector> bin = data.sublist(binStart, max, "bin " + binStart + "-" + max);
            int finalBinStart = binStart;
            Data<Integer, VECollector> bin = new Data<>(data.getData().stream()
                    .filter(pair -> finalBinStart <= pair.r && pair.r < max)
                    .collect(Collectors.toList())
                    , "bin " + binStart + "-" + max);

//            String canon = bin.getData().stream()
//                    .sorted(Comparator.comparingInt(Pair::getR))
//                    .map(d -> "(" + d.getR() + "," + d.getS().getConstants().size() + ")")
//                    .collect(Collectors.joining("_"));
//            System.out.println(canon.hashCode() + "\n" + canon);

            MultiMap<Predicate, Double> dataGammas = utils.gammas(bin, false);
//            Map<Pair<String, Integer>, Double> gammas = new HashMap<>();
//            predicatesToBeFitted.forEach(predicate -> gammas.put(new Pair<>(predicate.getName(), predicate.getArity())
//                    , bestGamma(evidence, bin, predicate, dataGammas.get(predicate))));
            Map<Pair<String, Integer>, Double> gammas = Sugar.parallelStream(predicates, true)
                    .filter(predicate -> learnAll || predicatesToBeFitted.contains(predicate))
                    .filter(predicate -> dataGammas.containsKey(predicate) && dataGammas.get(predicate) != null && !dataGammas.get(predicate).isEmpty())
                    .collect(Collectors.toMap(Predicate::getPair, predicate -> {
                        switch (mode) {
                            case GAMMAS:
                                return bestGamma(evidence, bin, predicate, dataGammas.get(predicate), k, maskGT);
                            case RATIOS_MIN:
                            case RATIOS_AVG:
                            case RATIOS_MED:
                            case RATIOS_MAX:
                                return bestRatio(evidence, bin, predicate);
                            default:
                                throw new IllegalStateException("unknown type of learning option");
                        }
                    }));
//                    .filter(predicate -> predicate.getName().equals("Post_Quals"))
//            System.out.println("debug fit\todstranit ten filter");

            if (GAMMAS == mode) {
                cuts.add(GammasCut.create(gammas, k, "gammas learned w.r.t. " + binStart + "-" + max + " from " + data.getName() + " given " + groundTruth));
            } else {
                cuts.add(AdaptiveRatioCut.create(gammas, k, "gammas learned w.r.t. " + binStart + "-" + max + " from " + data.getName() + " given " + groundTruth));
            }

            if (Integer.MAX_VALUE == evidenceBinSize) {
                break;
            }
        }

        return BinCut.create(cuts, evidenceBinSize, k, data.getName() + " w.r.t. " + groundTruth);
    }


    public Cut fit(Path groundTruth, Pair<Path, Integer> prediction) {
        BinCut bc = fit(groundTruth, prediction, Integer.MAX_VALUE);
        return bc.getCuts().get(0);
    }

    private List<Pair<Integer, Double>> bestConstantRatio(Set<Literal> evidence, Data<Integer, VECollector> bin, Predicate predicate) {
        ConcurrentMap<Integer, List<Pair<Integer, Double>>> evdRatios = Sugar.parallelStream(bin.getData())
                .map(pair -> new Pair<>(pair.s.constantsSize()
                        , Sugar.parallelStream(pair.s.getVotes().keySet())
                        .filter(evidence::contains)
                        .count() * 1.0 / pair.s.getVotes().size()))
                .collect(Collectors.groupingByConcurrent(Pair::getR));
        return Sugar.parallelStream(evdRatios.entrySet())
                .map(entry -> new Pair<>(entry.getKey(),
                        entry.getValue().stream().mapToDouble(p -> p.s).average().orElse(0))
                ).collect(Collectors.toList());
    }

    private Double bestRatio(Set<Literal> evidence, Data<Integer, VECollector> bin, Predicate predicate) {
        System.out.println("tady by se melo jeste zavest TP/FP/FN oproti GT(evidence)");
        throw new IllegalStateException();// NotImplementedException();
//        DoubleStream reduced = Sugar.parallelStream(bin.getData())
//                .map(pair -> new Pair<>(pair.s.originalEvidenceSize(), entailed(predicate, pair.s.getVotes())))
//                .filter(evdLit -> evdLit.s > 0)
//                .mapToDouble(pair -> (1.0 * pair.s) / pair.r);
//
//        switch (mode) {
//            case RATIOS_MIN:
//                return reduced.min().orElse(0);
//            case RATIOS_AVG:
//                return reduced.average().orElse(0);
//            case RATIOS_MED:
//                List<Double> list = reduced.sorted().boxed().collect(Collectors.toList());
//                if (list.isEmpty()) {
//                    return 0.0;
//                } else if (list.size() % 2 == 0) {
//                    return list.get(list.size() / 2);
//                }
//                return (list.get(list.size() / 2) + list.get(1 + list.size() / 2)) / 2.0;
//            case RATIOS_MAX:
//                return reduced.max().orElse(0);
//            default:
//                throw new IllegalStateException("unknown type of learning option");
//        }
    }

    private long entailed(Predicate predicate, DoubleCounters<Literal> votes) {
        return Sugar.parallelStream(votes.keySet())
                .filter(literal -> literal.getPredicate().equals(predicate.getPair()))
                .count();
    }


    private Double bestGamma(Set<Literal> evidence, Data<Integer, VECollector> data, Predicate predicate, Set<Double> gammas, int k, boolean maskGT) {
        System.out.println("solving\t" + predicate + "\t\t" + gammas.size() + "\t" + maskGT + "\t" + gammas);

        evidence = evidence.stream().filter(literal -> literal.arity() == predicate.getArity() && literal.predicate().equals(predicate.getName())).collect(Collectors.toSet());

//        Double bestThreshold = null;
//        Double bestVal = null;

        Utils utils = Utils.create();
        data = utils.cut(data, PredicateCut.create(predicate), true);

//        if (null == gammas) {
//            gammas = Sugar.set(0.0d, Double.POSITIVE_INFINITY);
//        } else {
//            gammas.add(Double.POSITIVE_INFINITY);
//        }

//        System.out.println("pokus debugu s nulou");
//        if (null != gammas) {
//            gammas.add(0.0);
//        }

        if (null == gammas) {
            // better be safe than sorry
            return null;
        } else if (gammas.size() == 1) {
            return Sugar.chooseOne(gammas);
        }
//            gammas.add(Double.POSITIVE_INFINITY);

//        System.out.println("debug");
//        if(predicate == null || !predicate.getName().toLowerCase().equals("faculty")){
//            System.out.println("odstran toto!!!");
//            return -1.0;
//        }
//        System.out.println("" + predicate);

//        String s = "";
/*
        for (Double gamma : gammas.stream().sorted().collect(Collectors.toList())) {
            // tady to predelat aby to bezelo trochu jinak, mohlo by to potom byt rychlejsi (jako pri vykreslovani to predelat)
//            kvuli tomuhle cutu to znamena ze pro vsechny ostatni predikaty tam je k-entailment
            double cumulatedError = utils.hammingStream(evidence, data, GammasCut.get(predicate, gamma, k), maskGT)
                    .mapToDouble(p -> p.s).sum();
            if (null == bestVal || bestVal > cumulatedError) {
                bestVal = cumulatedError;
                bestThreshold = gamma;
            }
//            System.out.println(gamma + "\t" + cumulatedError);
//            s += " (" + gamma + "," + cumulatedError + ")";
        }
//        System.out.println("best gamma\t" + bestThreshold + "\t" + bestVal);
//        System.out.println(s);
*/
//        return bestThreshold;
        Set<Literal> finalEvidence = evidence;
        Data<Integer, VECollector> finalData = data;
        return Sugar.parallelStream(gammas, true)
                .map(gamma -> new Pair<>(gamma, utils.hammingStream(finalEvidence, finalData, GammasCut.create(predicate, gamma, k), maskGT).mapToDouble(p -> p.s).sum()))
                .min(Comparator.comparingDouble(Pair::getS))
                /*.min((p1, p2) -> { // ok, tak tohle dava horsi vysledky
                    int comparedValues = Double.compare(p1.s, p2.s);
                    if (0 == comparedValues) {
                        return comparedValues;
                    }
                    return Double.compare(p1.r, p2.r);
                })
                */
                .orElse(new Pair<>(-1.0, -1.0)) // redundantni, null case je nahore
                .r;
    }


    public static GammaFitter create(List<Predicate> predicates, int mode) {
        return new GammaFitter(predicates, mode);
    }

    public static GammaFitter create(int mode) {
        return new GammaFitter(null, mode);
    }
}
