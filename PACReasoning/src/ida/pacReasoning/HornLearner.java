package ida.pacReasoning;

import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.pacReasoning.data.MLNsDataset;
import ida.pacReasoning.data.PACAccuracyDataset;
import ida.pacReasoning.data.SubsampledDataset;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.evaluation.Utils;
import ida.searchPruning.evaluation.BreadthResults;
import ida.searchPruning.search.MutableStats;
import ida.searchPruning.search.SimpleLearner;
import ida.searchPruning.search.collections.SearchNodeInfo;
import ida.utils.MutableDouble;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.utils.collections.MultiList;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.learning.constraints.shortConstraintLearner.UltraShortConstraintLearnerFaster;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.learning.languageBias.LanguageBias;
import logicStuff.learning.saturation.ConstantSaturationProvider;
import logicStuff.learning.saturation.RuleSaturator;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 10. 8. 2018.
 */
public class HornLearner {

    private final boolean rangeRestricted = false;
    private Set<String> predicatesDone;
    private List<Pair<Double, HornClause>> rules;
    private boolean fasteringSomething;

    public HornLearner() {
        this.fasteringSomething = false;
        String data = "[0.5296296296296297, term9(V1, V2) <- term9(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.7339449541284404, term9(V1, V2) <- term11(V2, V1), term8(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.3890020366598778, term9(V1, V2) <- term9(V1, V5), term8(V3, V2), term9(V2, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.3678646934460888, term9(V1, V2) <- term15(V1, V3), term5(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.0050335570469798654, term9(V1, V2) <- term8(V3, V2), term15(V1, V3), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5230263157894737, term10(V1, V2) <- term10(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.36786786786786785, term10(V1, V2) <- term13(V3, V2), term16(V4, V3), term15(V4, V1), @alldiff(V1, V2, V3, V4)]\n" +
                "\t[0.7518248175182481, term10(V1, V2) <- term11(V2, V1), term7(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5285714285714286, term10(V1, V2) <- term11(V2, V5), term10(V1, V5), term10(V3, V2), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.002, term10(V1, V2) <- term11(V1, V3), term7(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.7105263157894737, term21(V1, V2) <- term21(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.10429447852760736, term21(V1, V2) <- term6(V2, V5), term7(V3, V1), term10(V3, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.14965986394557823, term21(V1, V2) <- term7(V3, V2), term21(V4, V1), term11(V4, V2), @alldiff(V1, V2, V3, V4)]\n" +
                "\t[6.101281269066504E-4, term21(V1, V2) <- term6(V3, V1), term20(V2, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.0012658227848101266, term21(V1, V2) <- term7(V3, V1), term16(V2, V3), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5, term5(V1, V2) <- term5(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.6653543307086615, term5(V1, V2) <- term15(V2, V5), term9(V3, V2), term15(V3, V1), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.7126865671641791, term5(V1, V2) <- term11(V3, V1), term5(V3, V2), term5(V1, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.8064516129032258, term5(V1, V2) <- term15(V2, V1), term8(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.0050335570469798654, term5(V1, V2) <- term8(V3, V2), term15(V1, V3), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5755813953488372, term12(V1, V2) <- term12(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.35403726708074534, term12(V1, V2) <- term18(V6, V2), term11(V4, V2), term12(V1, V4), @alldiff(V6, V1, V2, V4)]\n" +
                "\t[0.24473684210526317, term12(V1, V2) <- term1(V3, V1), term12(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5681818181818182, term12(V1, V2) <- term12(V1, V5), term12(V3, V2), term12(V3, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.002635046113306983, term12(V1, V2) <- term16(V1, V3), term8(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5053763440860215, term7(V1, V2) <- term7(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.7699530516431925, term7(V1, V2) <- term16(V2, V1), term7(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5843023255813954, term7(V1, V2) <- term7(V3, V2), term11(V2, V4), term16(V4, V1), @alldiff(V1, V2, V3, V4)]\n" +
                "\t[0.0034602076124567475, term7(V1, V2) <- term7(V3, V2), term16(V4, V1), term16(V4, V2), @alldiff(V1, V2, V3, V4)]\n" +
                "\t[0.5425531914893617, term7(V1, V2) <- term8(V1, V3), term10(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5, term25(V1, V2) <- term25(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.04878048780487805, term25(V1, V2) <- term20(V3, V2), term25(V1, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.5, term14(V1, V2) <- term14(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.06936416184971098, term14(V1, V2) <- term14(V1, V3), term8(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.2, term14(V1, V2) <- term9(V3, V2), term12(V2, V1), @alldiff(V1, V2, V3)]\n" +
                "\t[8.179959100204499E-4, term14(V1, V2) <- term12(V1, V5), term8(V3, V2), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.10344827586206896, term14(V1, V2) <- term11(V1, V5), term12(V2, V5), term8(V3, V2), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.5050675675675675, term15(V1, V2) <- term15(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.6501650165016502, term15(V1, V2) <- term11(V4, V2), term15(V1, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.785234899328859, term15(V1, V2) <- term6(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.71875, term15(V1, V2) <- term5(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.03788546255506608, term15(V1, V2) <- term15(V1, V4), term2(V2, V5), @alldiff(V1, V2, V4, V5)]\n" +
                "\t[0.6538461538461539, term1(V1, V2) <- term1(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.5589225589225589, term1(V1, V2) <- term1(V6, V2), term1(V1, V4), term1(V4, V6), @alldiff(V6, V1, V2, V4)]\n" +
                "\t[0.6986301369863014, term1(V1, V2) <- term2(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.024255788313120176, term1(V1, V2) <- term2(V3, V1), term5(V2, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.5465116279069767, term1(V1, V2) <- term10(V1, V4), term2(V2, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.6837606837606838, term17(V1, V2) <- term17(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.5065274151436031, term17(V1, V2) <- term17(V6, V2), term17(V1, V4), term17(V6, V4), @alldiff(V6, V1, V2, V4)]\n" +
                "\t[0.0025015634771732333, term17(V1, V2) <- term20(V3, V1), term20(V2, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.5203252032520326, term17(V1, V2) <- term17(V6, V2), term17(V1, V4), term17(V4, V6), @alldiff(V6, V1, V2, V4)]\n" +
                "\t[0.001953125, term17(V1, V2) <- term7(V3, V2), term8(V3, V1), @alldiff(V1, V2, V3)]\n" +
                "\t[0.7045454545454546, term3(V1, V2) <- term3(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.004171406901782328, term3(V1, V2) <- term8(V3, V2), term3(V3, V1), @alldiff(V1, V2, V3)]\n" +
                "\t[0.22580645161290322, term3(V1, V2) <- term11(V2, V4), term3(V1, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[8.741258741258741E-4, term3(V1, V2) <- term16(V1, V4), term16(V2, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.0013623978201634877, term3(V1, V2) <- term11(V1, V4), term16(V2, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.05747126436781609, term19(V1, V2) <- term8(V2, V4), term19(V1, V6), term16(V6, V2), @alldiff(V6, V1, V2, V4)]\n" +
                "\t[0.009900990099009901, term19(V1, V2) <- term9(V3, V2), term7(V1, V3), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5, term19(V1, V2) <- term19(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.017142857142857144, term19(V1, V2) <- term0(V3, V1), term8(V2, V3), @alldiff(V1, V2, V3)]\n" +
                "\t[0.15625, term19(V1, V2) <- term0(V3, V1), term21(V3, V5), term18(V5, V2), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.5354330708661418, term8(V1, V2) <- term8(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.7621621621621621, term8(V1, V2) <- term16(V2, V1), term8(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5425, term8(V1, V2) <- term9(V3, V2), term16(V3, V1), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5444444444444444, term8(V1, V2) <- term20(V3, V2), term5(V1, V3), @alldiff(V1, V2, V3)]\n" +
                "\t[0.002849002849002849, term8(V1, V2) <- term11(V1, V3), term9(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.8040540540540541, term20(V1, V2) <- term20(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.0011025358324145535, term20(V1, V2) <- term7(V6, V1), term20(V2, V4), @alldiff(V6, V1, V2, V4)]\n" +
                "\t[0.35094339622641507, term20(V1, V2) <- term20(V6, V1), term9(V6, V2), term16(V2, V4), @alldiff(V6, V1, V2, V4)]\n" +
                "\t[0.3688888888888889, term20(V1, V2) <- term16(V2, V5), term8(V3, V2), term5(V5, V1), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.3076923076923077, term20(V1, V2) <- term9(V4, V1), term20(V2, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.5100401606425703, term11(V1, V2) <- term11(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.3221415607985481, term11(V1, V2) <- term11(V2, V5), term11(V1, V5), term15(V3, V2), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.0010615711252653928, term11(V1, V2) <- term15(V1, V4), term16(V2, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.6904761904761905, term11(V1, V2) <- term10(V2, V1), term15(V1, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.5887850467289719, term11(V1, V2) <- term9(V2, V1), term9(V2, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.8174603174603174, term22(V1, V2) <- term22(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.3953488372093023, term22(V1, V2) <- term10(V2, V5), term7(V3, V2), term22(V1, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.0020927799093128706, term22(V1, V2) <- term22(V3, V1), term20(V2, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.42857142857142855, term22(V1, V2) <- term22(V2, V5), term0(V1, V5), term22(V3, V1), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.003006012024048096, term22(V1, V2) <- term13(V3, V1), term20(V2, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.8333333333333334, term4(V1, V2) <- term4(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.2680851063829787, term4(V1, V2) <- term10(V1, V3), term4(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.026010877275951763, term4(V1, V2) <- term4(V3, V1), term18(V2, V3), @alldiff(V1, V2, V3)]\n" +
                "\t[0.589041095890411, term13(V1, V2) <- term13(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.3907766990291262, term13(V1, V2) <- term13(V1, V3), term10(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.4492753623188406, term13(V1, V2) <- term13(V3, V2), term13(V1, V5), term13(V3, V5), @alldiff(V1, V2, V3, V5)]\n" +
                "\t[0.0016778523489932886, term13(V1, V2) <- term7(V3, V2), term7(V1, V3), @alldiff(V1, V2, V3)]\n" +
                "\t[0.002044989775051125, term13(V1, V2) <- term16(V1, V3), term10(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.5100671140939598, term6(V1, V2) <- term6(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.6428571428571429, term6(V1, V2) <- term15(V3, V1), term10(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.7669172932330827, term6(V1, V2) <- term15(V2, V1), term6(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.7024793388429752, term6(V1, V2) <- term11(V3, V1), term6(V3, V2), @alldiff(V1, V2, V3)]\n" +
                "\t[0.007317073170731708, term6(V1, V2) <- term7(V3, V2), term11(V4, V3), term15(V4, V1), @alldiff(V1, V2, V3, V4)]\n" +
                "\t[0.5175879396984925, term16(V1, V2) <- term16(V2, V1), @alldiff(V1, V2)]\n" +
                "\t[0.5126050420168067, term16(V1, V2) <- term11(V1, V5), term16(V5, V2), term5(V2, V4), @alldiff(V1, V2, V4, V5)]\n" +
                "\t[9.950248756218905E-4, term16(V1, V2) <- term15(V1, V5), term5(V5, V4), term5(V2, V4), @alldiff(V1, V2, V4, V5)]\n" +
                "\t[0.0011160714285714285, term16(V1, V2) <- term15(V2, V4), term15(V1, V4), @alldiff(V1, V2, V4)]\n" +
                "\t[0.01437908496732026, term16(V1, V2) <- term4(V5, V4), term5(V2, V5), term15(V1, V4), @alldiff(V1, V2, V4, V5)]\n" +
                "\t[0.8771929824561403, term0(V1, V2) <- term0(V2, V1), @alldiff(V1, V2)]";
        /*String data = "\t[0.3813038130381304, location(1:V0, 5:V1) <- location(1:V2, 5:V1), interaction(1:V2, 1:V0), interaction(1:V0, 1:V2), @alldiff(1:V2, 1:V0, 5:V1)]\n" +
                "\t[0.3227307990690458, location(1:V0, 5:V1) <- location(1:V4, 5:V1), function(1:V0, 4:V3), interaction(1:V4, 1:V4), function(1:V4, 4:V3), @alldiff(1:V4, 1:V0, 5:V1, 4:V3)]\n" +
                "\t[0.5230769230769231, location(1:V0, 5:V1) <- interaction(1:V0, 1:V3), interaction(1:V3, 1:V0), location(1:V3, 5:V1), function(1:V3, 4:V5), @alldiff(1:V3, 1:V0, 5:V1, 4:V5)]\n" +
                "\t[0.7916666666666666, location(1:V0, 5:V1) <- location(1:V4, 5:V1), interaction(1:V4, 1:V0), phenotype(1:V0, 6:V3), phenotype(1:V4, 6:V3), @alldiff(1:V4, 1:V0, 5:V1, 6:V3)]\n" +
                "\t[0.5416666666666666, location(1:V0, 5:V1) <- location(1:V4, 5:V1), phenotype(1:V0, 6:V3), interaction(1:V4, 1:V4), phenotype(1:V4, 6:V3), @alldiff(1:V4, 1:V0, 5:V1, 6:V3)]\n" +
                "\t[0.7, location(1:V0, 5:V1) <- location(1:V4, 5:V1), interaction(1:V0, 1:V4), function(1:V0, 4:V3), interaction(1:V4, 1:V4), @alldiff(1:V4, 1:V0, 5:V1, 4:V3)]\n" +
                "\t[0.5, location(1:V0, 5:V1) <- location(1:V4, 5:V1), interaction(1:V4, 1:V0), function(1:V0, 4:V3), interaction(1:V4, 1:V4), @alldiff(1:V4, 1:V0, 5:V1, 4:V3)]\n" +
                "\t[0.13238095238095238, function(1:V0, 4:V1) <- function(1:V4, 4:V1), function(1:V0, 4:V3), @alldiff(1:V4, 1:V0, 4:V1, 4:V3)]\n" +
                "\t[0.27494287890327496, function(1:V0, 4:V1) <- interaction(1:V0, 1:V3), interaction(1:V3, 1:V0), function(1:V3, 4:V1), @alldiff(1:V3, 1:V0, 4:V1)]\n" +
                "\t[0.31275100401606426, function(1:V0, 4:V1) <- function(1:V2, 4:V1), function(1:V0, 4:V4), interaction(1:V2, 1:V2), function(1:V2, 4:V4), @alldiff(1:V2, 1:V0, 4:V1, 4:V4)]\n" +
                "\t[0.8, function(1:V0, 4:V1) <- function(1:V2, 4:V1), complex(1:V0, 2:V4), complex(1:V2, 2:V4), @alldiff(2:V4, 1:V2, 1:V0, 4:V1)]\n" +
                "\t[0.5960591133004927, function(1:V0, 4:V1) <- function(1:V2, 4:V1), function(1:V0, 4:V4), function(1:V2, 4:V4), interaction(1:V0, 1:V2), interaction(1:V2, 1:V0), @alldiff(1:V2, 1:V0, 4:V1, 4:V4)]\n" +
                "\t[0.7380952380952381, function(1:V0, 4:V1) <- function(1:V2, 4:V1), protein_class(1:V0, 7:V4), protein_class(1:V2, 7:V4), @alldiff(1:V2, 1:V0, 7:V4, 4:V1)]\n" +
                "\t[0.53125, function(1:V0, 4:V1) <- enzyme(1:V2, 3:V4), function(1:V2, 4:V1), enzyme(1:V0, 3:V4), @alldiff(1:V2, 1:V0, 4:V1, 3:V4)]\n" +
                "\t[0.025101513473606497, complex(1:V0, 2:V1) <- complex(1:V2, 2:V1), complex(1:V0, 2:V4), @alldiff(2:V4, 1:V2, 2:V1, 1:V0)]\n" +
                "\t[0.14356435643564355, complex(1:V0, 2:V1) <- complex(1:V2, 2:V1), interaction(1:V2, 1:V0), interaction(1:V0, 1:V2), @alldiff(1:V2, 2:V1, 1:V0)]\n" +
                "\t[0.4, complex(1:V0, 2:V1) <- interaction(1:V0, 1:V4), phenotype(1:V0, 6:V3), complex(1:V4, 2:V1), phenotype(1:V4, 6:V3), @alldiff(1:V4, 1:V0, 2:V1, 6:V3)]\n" +
                "\t[0.3333333333333333, complex(1:V0, 2:V1) <- interaction(1:V4, 1:V0), interaction(1:V0, 1:V4), complex(1:V4, 2:V1), interaction(1:V4, 1:V2), interaction(1:V2, 1:V0), interaction(1:V0, 1:V2), @alldiff(1:V4, 1:V2, 1:V0, 2:V1)]\n" +
                "\t[0.45, complex(1:V0, 2:V1) <- location(1:V5, 5:V3), complex(1:V5, 2:V1), location(1:V0, 5:V3), interaction(1:V0, 1:V5), interaction(1:V5, 1:V0), @alldiff(2:V1, 1:V0, 5:V3, 1:V5)]\n" +
                "\t[0.011111111111111112, complex(1:V0, 2:V1) <- phenotype(1:V0, 6:V3), interaction(1:V4, 1:V4), complex(1:V4, 2:V1), @alldiff(1:V4, 1:V0, 2:V1, 6:V3)]\n" +
                "\t[0.25, complex(1:V0, 2:V1) <- interaction(1:V4, 1:V0), complex(1:V4, 2:V1), interaction(1:V4, 1:V4), location(1:V0, 5:V3), @alldiff(1:V4, 1:V0, 2:V1, 5:V3)]\n" +
                "\t[0.03306451612903226, enzyme(1:V0, 3:V1) <- enzyme(1:V4, 3:V1), enzyme(1:V0, 3:V3), @alldiff(1:V4, 1:V0, 3:V1, 3:V3)]\n" +
                "\t[0.36666666666666664, enzyme(1:V0, 3:V1) <- enzyme(1:V4, 3:V1), interaction(1:V0, 1:V4), interaction(1:V4, 1:V0), function(1:V0, 4:V3), @alldiff(1:V4, 1:V0, 3:V1, 4:V3)]\n" +
                "\t[1.0, enzyme(1:V0, 3:V1) <- enzyme(1:V4, 3:V1), interaction(1:V4, 1:V0), interaction(1:V0, 1:V4), function(1:V0, 4:V3), function(1:V4, 4:V3), @alldiff(1:V4, 1:V0, 3:V1, 4:V3)]\n" +
                "\t[1.0, enzyme(1:V0, 3:V1) <- phenotype(1:V3, 6:V5), interaction(1:V3, 1:V0), phenotype(1:V0, 6:V5), enzyme(1:V3, 3:V1), @alldiff(1:V3, 1:V0, 6:V5, 3:V1)]\n" +
                "\t[0.46153846153846156, enzyme(1:V0, 3:V1) <- enzyme(1:V4, 3:V1), interaction(1:V0, 1:V4), function(1:V0, 4:V3), function(1:V4, 4:V3), @alldiff(1:V4, 1:V0, 3:V1, 4:V3)]\n" +
                "\t[0.04411764705882353, protein_class(1:V0, 7:V1) <- protein_class(1:V2, 7:V1), protein_class(1:V0, 7:V4), @alldiff(1:V2, 1:V0, 7:V1, 7:V4)]\n" +
                "\t[0.2857142857142857, protein_class(1:V0, 7:V1) <- interaction(1:V0, 1:V4), interaction(1:V4, 1:V0), function(1:V0, 4:V3), protein_class(1:V4, 7:V1), function(1:V4, 4:V3), @alldiff(1:V4, 1:V0, 7:V1, 4:V3)]\n" +
                "\t[0.5, protein_class(1:V0, 7:V1) <- interaction(1:V0, 1:V3), interaction(1:V3, 1:V0), protein_class(1:V3, 7:V1), protein_class(1:V4, 7:V1), interaction(1:V4, 1:V3), @alldiff(1:V4, 1:V3, 1:V0, 7:V1)]\n" +
                "\t[0.25, protein_class(1:V0, 7:V1) <- interaction(1:V0, 1:V3), interaction(1:V3, 1:V0), complex(1:V0, 2:V5), protein_class(1:V3, 7:V1), @alldiff(2:V5, 1:V3, 1:V0, 7:V1)]\n" +
                "\t[0.11005291005291006, phenotype(1:V0, 6:V1) <- interaction(1:V0, 1:V3), interaction(1:V3, 1:V0), phenotype(1:V3, 6:V1), @alldiff(1:V3, 1:V0, 6:V1)]\n" +
                "\t[0.05505050505050505, phenotype(1:V0, 6:V1) <- phenotype(1:V0, 6:V4), phenotype(1:V2, 6:V1), @alldiff(1:V2, 1:V0, 6:V1, 6:V4)]\n" +
                "\t[0.5, phenotype(1:V0, 6:V1) <- complex(1:V0, 2:V4), complex(1:V2, 2:V4), phenotype(1:V2, 6:V1), @alldiff(2:V4, 1:V2, 1:V0, 6:V1)]\n" +
                "\t[0.4146341463414634, phenotype(1:V0, 6:V1) <- phenotype(1:V0, 6:V4), interaction(1:V0, 1:V2), interaction(1:V2, 1:V0), phenotype(1:V2, 6:V1), @alldiff(1:V2, 1:V0, 6:V1, 6:V4)]\n" +
                "\t[0.44642857142857145, phenotype(1:V0, 6:V1) <- function(1:V0, 4:V4), function(1:V2, 4:V4), interaction(1:V0, 1:V2), interaction(1:V2, 1:V0), phenotype(1:V2, 6:V1), @alldiff(1:V2, 1:V0, 6:V1, 4:V4)]\n" +
                "\t[0.23076923076923078, phenotype(1:V0, 6:V1) <- enzyme(1:V2, 3:V4), enzyme(1:V0, 3:V4), phenotype(1:V2, 6:V1), @alldiff(1:V2, 1:V0, 6:V1, 3:V4)]\n" +
                "\t[0.48148148148148145, phenotype(1:V0, 6:V1) <- complex(1:V0, 2:V4), interaction(1:V0, 1:V2), interaction(1:V2, 1:V0), phenotype(1:V2, 6:V1), @alldiff(2:V4, 1:V2, 1:V0, 6:V1)]\n" +
                "\t[0.0038334853877816755, interaction(1:V0, 1:V1) <- interaction(1:V4, 1:V1), function(1:V0, 4:V3), @alldiff(1:V4, 1:V1, 1:V0, 4:V3)]\n" +
                "\t[0.09814572017231692, interaction(1:V0, 1:V1) <- interaction(1:V5, 1:V1), function(1:V1, 4:V3), interaction(1:V0, 1:V5), interaction(1:V5, 1:V0), @alldiff(1:V1, 1:V0, 4:V3, 1:V5)]\n" +
                "\t[0.06493868450390189, interaction(1:V0, 1:V1) <- location(1:V1, 5:V3), interaction(1:V1, 1:V5), interaction(1:V0, 1:V5), interaction(1:V5, 1:V0), @alldiff(1:V1, 1:V0, 5:V3, 1:V5)]\n" +
                "\t[0.059846317796847834, interaction(1:V0, 1:V1) <- interaction(1:V1, 1:V2), interaction(1:V2, 1:V1), interaction(1:V0, 1:V2), @alldiff(1:V2, 1:V1, 1:V0)]\n" +
                "\t[0.10531574740207834, interaction(1:V0, 1:V1) <- interaction(1:V2, 1:V1), location(1:V1, 5:V4), interaction(1:V2, 1:V0), interaction(1:V0, 1:V2), @alldiff(1:V2, 1:V1, 1:V0, 5:V4)]";
        */

        this.predicatesDone = Sugar.set();
        this.rules = Arrays.stream(data.split("\n")).map(String::trim).filter(l -> l.length() > 0)
                .map(l -> {
                    String[] splitted = l.substring(1, l.length() - 1).split(",", 2);
                    double confidenc = Double.parseDouble(splitted[0]);
                    HornClause rule = HornClause.parse(splitted[1]);
                    predicatesDone.add(Predicate.create(rule.head().getPredicate()).toString());
                    return new Pair<Double, HornClause>(confidenc, rule);
                }).collect(Collectors.toList());
    }

    {
        System.out.println("jak je to s tim ktery prostor se prohledava, je to spravne nebo ne to jak je to ted?");
    }

    // last minute hack
    public List<Pair<Double, HornClause>> learnEntropyDriven(SubsampledDataset dataset, int rounds, List<Pair<Double, HornClause>> rules) { //  Map<Pair<Predicate, Integer>, Type> typing,
        System.out.println("ten cas nejak nefunguje");
        TimeDog dog = new TimeDog(15 * 60 * 1000_000_000, true);
        int minSupp = 1;

        MEDataset med = MEDataset.create(Sugar.list(new Clause(dataset.getEvidence())), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);
        Set<Pair<String, Integer>> predicates = med.allPredicates();

        Coverage zeroCover = CoverageFactory.getInstance().take();
        Coverage totalCoverage = CoverageFactory.getInstance().take(0); // we have only one predicate

        Set<String> skipPredicates = Sugar.set();//Sugar.set("interaction/2");

        if (this.fasteringSomething) {
            System.out.println("skipping part of the learning since it is here!");
            skipPredicates.addAll(this.predicatesDone);
            dataset.addRulesDeprected(this.rules);
            System.out.println("skip\t" + skipPredicates);
        }
        MultiMap<Pair<String, Integer>, Pair<Double, HornClause>> rulesPerPredicate = new MultiMap<>();
        rules.stream().forEach(pair -> {
            rulesPerPredicate.put(pair.s.head().getPredicate(), new Pair<>(pair.r, pair.s));
        });

        Set<IsoClauseWrapper> selectedRules = Sugar.set();
        List<Pair<Double, HornClause>> retVal = Sugar.list();
        for (Map.Entry<Pair<String, Integer>, Set<Pair<Double, HornClause>>> entry : rulesPerPredicate.entrySet()) {
            dataset.setTargetPredicate(entry.getKey());
            System.out.println("******\t\t\t\tpredicate " + entry.getKey().toString());
            for (int roundIdx = 0; roundIdx < rounds; roundIdx++) {
                System.out.println("*****\t\t\tround " + roundIdx);
                double bestEntropy = 0;
                Pair<Double, HornClause> best = null;
                for (Pair<Double, HornClause> rule : entry.getValue()) {
                    Triple<Double, Double, Pair<Integer, Integer>> triple = dataset.computeConditionalAndCrossEntropy(
                            SearchNodeInfo.create(rule.getS(),
                                    IsoClauseWrapper.create(rule.getS()),
                                    rule.getS(), null, null, null));
                    System.out.println(rule.getS() + "\t" + triple + "\t" + LogicUtils.isRangeRestricted(rule.getS()));

                    if (null == triple) {
                        continue;
                        //throw new IllegalStateException();
                    }

                    Double conditional = triple.r;
                    Double crossEntropy = triple.s;

                    if (null == best || bestEntropy < crossEntropy) { // in fact, crossEntropy here is multiplied by -1, so we are maximizing it to minimize CW
                        best = rule;
                        bestEntropy = crossEntropy;
                    }
                }
                IsoClauseWrapper icw = IsoClauseWrapper.create(best.s);
                if (selectedRules.contains(icw)) {
                    throw new IllegalStateException();
                }
                entry.getValue().remove(best);
                selectedRules.add(icw);
                retVal.add(best);
                dataset.finishLevel(true);
                dataset.predicateFinished();
            }
        }

        return retVal;
    }


    public List<Pair<Double, HornClause>> learnEntropyDriven(SubsampledDataset dataset, int rounds, int beamSize, int maxDepth, int maxVariables, Set<Clause> constraints, Map<Pair<Predicate, Integer>, Type> typing, boolean crossentropyDriven) {
        System.out.println("ten cas nejak nefunguje");
        TimeDog dog = new TimeDog(15 * 60 * 1000_000_000, true);
        int minSupp = 1;

        if (!crossentropyDriven) {
            System.out.println("use all the date for learning");
            dataset = SubsampledDataset.create(Sugar.union(dataset.getEvidence(), dataset.getGoldComplement()), dataset.getSrc());
            // awful
            dataset.setMode(SubsampledDataset.ACCURACY);
        }

        MEDataset med = MEDataset.create(Sugar.list(new Clause(dataset.getEvidence())), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);
        Set<Pair<String, Integer>> predicates = med.allPredicates();

        Coverage zeroCover = CoverageFactory.getInstance().take();
        Coverage totalCoverage = CoverageFactory.getInstance().take(0); // we have only one predicate

        Map<Pair<Predicate, Integer>, String> simplified = null != typing ? TypesInducer.simplify(typing) : null;
        Set<String> skipPredicates = Sugar.set();//Sugar.set("interaction/2");

        if (this.fasteringSomething) {
            System.out.println("skipping part of the learning since it is here!");
            skipPredicates.addAll(this.predicatesDone);
            dataset.addRulesDeprected(this.rules);
            System.out.println("skip\t" + skipPredicates);
        }

        long timeEvaluationThreshold = 30 * 1000_000_000l;

        for (Pair<String, Integer> predicate : predicates) {
//                if (predicate.r.equals("location")) {
//                    System.out.println("skipping interaction");
//                    continue;
//                }
            System.out.println("wanna evaluate\t" + skipPredicates.contains(Predicate.create(predicate).toString()));
            if (skipPredicates.contains(Predicate.create(predicate).toString())) {
                System.out.println("skipping\t" + predicate);
                continue;
            }

            dataset.setTargetPredicate(predicate);
            Set<IsoClauseWrapper> topSnis = Sugar.set(); // to jsou zakazane jejichz deti se nemaji prohledavat uz
            for (int round = 0; round < rounds; round++) {
                System.out.println("starting round\t" + round);


                int finalBeamSize = predicate.r.equals("interaction") ? 3 : beamSize;


                System.out.println("\n\n*******\nsolving for predicate " + predicate.getR() + "/" + predicate.getS() + "\trun\t" + round + "\twith beam\t" + finalBeamSize);

                MutableStats stats = new MutableStats();
                SimpleLearner sl = new SimpleLearner(med,
                        ConstantSaturationProvider.createFilterSaturator(constraints, Sugar.set(), typing),
                        CoverageFactory.getInstance().get(med.size()),
                        null,
                        dog,
                        stats,
                        minSupp,
                        false,
                        typing,
                        false // because we have the SubsampledDataset ;)
                );

                List<Variable> variables = LogicUtils.freshVariables(simplified, Predicate.create(predicate));
                HornClause head = HornClause.create(new Literal(predicate.r, false, variables), Sugar.set());
                LanguageBias<HornClause> lb = () -> (hornClause) -> {
                    if (hornClause.variables().size() > maxVariables) {
                        return false;
                    }
                    Set<Variable> body = hornClause.body().variables();
                    Set<Variable> headVariables = LogicUtils.variables(hornClause.head());

                    for (Variable headVariable : headVariables) {
                        if (body.contains(headVariable)) {
                            return true;
                        }
                        // here can be other restrictions, this one is the simplest one pruning the space reasonable while allowing non-range restricted rules
                    }
                    return false;
                };

                SubsampledDataset finalDataset = dataset;
                SubsampledDataset finalDataset1 = dataset;
                Function<SearchNodeInfo, SearchNodeInfo> evalStrategy = (candidate) -> {

                    //candidate.addAlldiff();

                    long time = System.nanoTime();
                    long start = System.nanoTime();
                    Triple<Double, Double, Pair<Integer, Integer>> triple = finalDataset.computeConditionalAndCrossEntropy(candidate);
                    System.out.println(candidate.getRule() + "\t" + triple + "\t" + LogicUtils.isRangeRestricted(candidate.getRule()));

                    time = System.nanoTime() - time;
                    if (time > timeEvaluationThreshold) {
                        finalDataset.predicateFinished();
                    }

                    if (null == triple) {
                        return null;
//                        candidate.setCoverages(totalCoverage, zeroCover);
//                        candidate.setAccuracy(Double.NEGATIVE_INFINITY);
//                        candidate.setAllowability(false);
//                        return candidate;
                    }

                    Double conditional = triple.r;
                    Double crossEntropy = triple.s;

                    candidate.setCoverages(totalCoverage, zeroCover);
                    candidate.setAccuracy(crossEntropy);
                    candidate.setAllowability(true);
                    return candidate;
                };

                // monotonicity that comes from testing whether a query is a model of some evidence
                // redundancy here, a big one, hopefully it will work
                java.util.function.Predicate<SearchNodeInfo> levelFilter = sni -> sni.isAllowed();

                BreadthResults result = sl.breadthFirstSearch(maxDepth, System.nanoTime(), 0, lb, finalBeamSize, head,
                        evalStrategy, topSnis, levelFilter);

                System.out.println("selecting the best rule");
                Triple<SearchNodeInfo, Double, Double> bestRule = dataset.finishLevel(true);
                if (null == bestRule) {
                    System.out.println("no rule is added, just end this loop");
                    break;
                } else {
                    System.out.println("adding rule\t" + bestRule.r.getRule() + "\n\t" + bestRule.getR().getSaturatedICW().hashCode() + "\n\tcrossentropy\t" + bestRule.s + "\n\tconditional\t" + bestRule.t);
                    System.out.println("was already there\t" + dataset.getRules().stream().anyMatch(p -> p.getS().toString().equals(bestRule.r.getRule().toString())));
                    System.out.println("inside");
                    dataset.getRules().forEach(p -> System.out.println("\t" + p));
                }

                topSnis.add(bestRule.r.getRuleICW());

                // round end
                // tohle bylo docela rychle taky
                dataset.predicateFinished();
            }
            /*if(predicate.r.equals("Pre_Quals")){
                System.exit(-1);
            }*/
            System.out.println("ending predicate\t" + predicate);
//            System.out.println("ted pro debug");
//            System.exit(-1111111);
//            dataset.predicateFinished();
        }

        dataset.getRules().stream().sorted(Comparator.comparingDouble(Pair<Double, HornClause>::getR).reversed())
                .forEach(p -> System.out.println(p.r + "\t" + p.s));

        return dataset.getRules();
    }

    public List<Pair<Double, HornClause>> selectEntropyDriven(SubsampledDataset dataset, int rounds, Set<IsoClauseWrapper> amieMinedRules) {
        System.out.println("ten cas nejak nefunguje");
        TimeDog dog = new TimeDog(15 * 60 * 1000_000_000, true);
        int minSupp = 1;


        MEDataset med = MEDataset.create(Sugar.list(new Clause(Sugar.union(dataset.getEvidence(), dataset.getGoldComplement()))), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);
        Set<Pair<String, Integer>> predicates = med.allPredicates();

        Coverage zeroCover = CoverageFactory.getInstance().take();
        Coverage totalCoverage = CoverageFactory.getInstance().take(0); // we have only one predicate

        Set<String> skipPredicates = Sugar.set();//Sugar.set("interaction/2");

        if (this.fasteringSomething) {
            System.out.println("skipping part of the learning since it is here!");
            skipPredicates.addAll(this.predicatesDone);
            dataset.addRulesDeprected(this.rules);
            System.out.println("skip\t" + skipPredicates);
        }

        long timeEvaluationThreshold = 30 * 1000_000_000l;

        Map<Pair<String, Integer>, List<HornClause>> amieRules = new HashMap<>();
        for (IsoClauseWrapper icw : amieMinedRules) {
            HornClause rule = HornClause.create(icw.getOriginalClause());
            if (null == rule.head() || null == rule.body() || rule.body().literals().isEmpty()) {
                System.out.println("throwing " + icw.getOriginalClause() + " away since it is not a horn clause");
                continue;
            }
            Pair<String, Integer> predicate = rule.head().getPredicate();
            if (!amieRules.containsKey(predicate)) {
                amieRules.put(predicate, Sugar.list());
            }
            amieRules.get(predicate).add(rule);
        }

        Set<IsoClauseWrapper> topSnis = Sugar.set(); // to jsou zakazane jejichz deti se nemaji prohledavat uz
        for (Pair<String, Integer> predicate : predicates) {
            System.out.println("wanna evaluate\t" + skipPredicates.contains(Predicate.create(predicate).toString()));
            if (skipPredicates.contains(Predicate.create(predicate).toString())) {
                System.out.println("skipping\t" + predicate);
                continue;
            }

            dataset.setTargetPredicate(predicate);
            for (int round = 0; round < rounds; round++) {
                System.out.println("starting round\t" + round);

                System.out.println("\n\n*******\nsolving for predicate " + predicate.getR() + "/" + predicate.getS() + "\trun\t" + round);


                SubsampledDataset finalDataset = dataset;
                SubsampledDataset finalDataset1 = dataset;

                if (!amieRules.containsKey(predicate)) {
                    continue;
                }
                for (HornClause hornClause : amieRules.get(predicate)) {

                    long time = System.nanoTime();
                    SearchNodeInfo sni = SearchNodeInfo.create(hornClause, IsoClauseWrapper.create(hornClause), hornClause, null, null);
                    Triple<Double, Double, Pair<Integer, Integer>> triple = finalDataset.computeConditionalAndCrossEntropy(sni);
                    System.out.println(sni.getRule() + "\t" + triple + "\t" + LogicUtils.isRangeRestricted(sni.getRule()));

                    time = System.nanoTime() - time;
                    if (time > timeEvaluationThreshold) {
                        finalDataset.predicateFinished();
                    }

                    if (null == triple) {
                        continue;
                    }
                    Double conditional = triple.r;
                    Double crossEntropy = triple.s;
                    sni.setAccuracy(crossEntropy);
                }

                System.out.println("selecting the best rule");
                Triple<SearchNodeInfo, Double, Double> bestRule = dataset.finishLevel(true);
                if (null == bestRule) {
                    System.out.println("no rule is added, just end this loop");
                    break;
                } else {
                    System.out.println("adding rule\t" + bestRule.r.getRule() + "\n\t" + "\n\tcrossentropy\t" + bestRule.s + "\n\tconditional\t" + bestRule.t);
                    System.out.println("was already there\t" + dataset.getRules().stream().anyMatch(p -> p.getS().toString().equals(bestRule.r.getRule().toString())));
                    System.out.println("inside");
                    dataset.getRules().forEach(p -> System.out.println("\t" + p));
                }
                topSnis.add(bestRule.r.getRuleICW());

                // round end
                // tohle bylo docela rychle taky
                dataset.predicateFinished();
            }
            /*if(predicate.r.equals("Pre_Quals")){
                System.exit(-1);
            }*/
            System.out.println("ending predicate\t" + predicate);
//            System.out.println("ted pro debug");
//            System.exit(-1111111);
//            dataset.predicateFinished();
        }

        System.out.println("AMIE-CE rule list learned!");
        dataset.getRules().stream().sorted(Comparator.comparingDouble(Pair<Double, HornClause>::getR).reversed())
                .forEach(p -> System.out.println(p.r + "\t" + p.s));

        return dataset.getRules();
    }


    public List<Pair<Double, HornClause>> selectAcc(SubsampledDataset dataset, Set<IsoClauseWrapper> rulesToWalkThrough) {
        System.out.println("ten cas nejak nefunguje");
        TimeDog dog = new TimeDog(15 * 60 * 1000_000_000, true);
        int minSupp = 1;


        MEDataset med = MEDataset.create(Sugar.list(new Clause(Sugar.union(dataset.getEvidence(), dataset.getGoldComplement()))), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);
        Set<Pair<String, Integer>> predicates = med.allPredicates();


        long timeEvaluationThreshold = 30 * 1000_000_000l;

        List<SearchNodeInfo> results = Sugar.list();
        Map<Pair<String, Integer>, List<HornClause>> knownRules = new HashMap<>();
        for (IsoClauseWrapper icw : rulesToWalkThrough) {
            HornClause rule = HornClause.create(icw.getOriginalClause());
            if (null == rule.head() || null == rule.body() || rule.body().literals().isEmpty()) {
                System.out.println("throwing " + icw.getOriginalClause() + " away since it is not a horn clause");
                continue;
            }
            Pair<String, Integer> predicate = rule.head().getPredicate();
            if (!knownRules.containsKey(predicate)) {
                knownRules.put(predicate, Sugar.list());
            }
            knownRules.get(predicate).add(rule);
        }

        for (Pair<String, Integer> predicate : predicates) {
            dataset.setTargetPredicate(predicate);
            System.out.println("\n\n*******\nsolving for predicate " + predicate.getR() + "/" + predicate.getS());

            if (!knownRules.containsKey(predicate)) {
                continue;
            }
            for (HornClause hornClause : knownRules.get(predicate)) {
                long time = System.nanoTime();
                SearchNodeInfo sni = SearchNodeInfo.create(hornClause, IsoClauseWrapper.create(hornClause), hornClause, null, null);
                Triple<Double, Double, Pair<Integer, Integer>> triple = dataset.computeConditionalAndCrossEntropy(sni);
                System.out.println(sni.getRule() + "\t" + triple + "\t" + LogicUtils.isRangeRestricted(sni.getRule()));

                time = System.nanoTime() - time;
                if (time > timeEvaluationThreshold) {
                    dataset.predicateFinished();
                }

                if (null == triple) {
                    continue;
                }
                Double conditional = triple.r;
                Double crossEntropy = triple.s;
                sni.setAccuracy(conditional);
                results.add(sni);
            }
        }

        System.out.println("AMIE-all rules list learned!");
        List<Pair<Double, HornClause>> out = results.stream().map(sni -> new Pair<>(sni.getAccuracy(), sni.getRule())).collect(Collectors.toList());
        out.stream().sorted(Comparator.comparingDouble(Pair<Double, HornClause>::getR).reversed())
                .forEach(p -> System.out.println(p.r + "\t" + p.s));
        return out;
    }


    public List<Pair<Double, HornClause>> learnLevelWise(SubsampledDataset dataset, int bestN, int beamSize, int maxDepth, Set<Clause> constraints, Map<Pair<Predicate, Integer>, Type> typing) {
        TimeDog dog = new TimeDog(15 * 60 * 1000_000_000, true);
        int minSupp = 1;

        MEDataset med = MEDataset.create(Sugar.list(new Clause(dataset.getEvidence())), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);

        Coverage zeroCover = CoverageFactory.getInstance().take();
        Coverage totalCoverage = CoverageFactory.getInstance().take(0); // we have only one predicate

        List<Pair<Double, HornClause>> theory = Sugar.list();
        Map<Pair<Predicate, Integer>, String> simplified = TypesInducer.simplify(typing);
        Set<String> skipPredicates = Sugar.set();


        // pro podminenou pravdepodobnost a entropii to zmenit :))
        med.allPredicates().stream()
                .filter(predicate -> !skipPredicates.contains(Predicate.create(predicate).toString()))
                .forEach(predicate -> {
                    Set<IsoClauseWrapper> topSnis = Sugar.set(); // to jsou zakazane jejichz deti se nemaji prohledavat uz

                    for (int run = 4; run < maxDepth; run++) {


                        System.out.println("\n\n*******\nsolving for predicate " + predicate.getR() + "/" + predicate.getS() + "\trun\t" + run);

                        MutableStats stats = new MutableStats();
                        SimpleLearner sl = new SimpleLearner(med,
                                ConstantSaturationProvider.createFilterSaturator(constraints, Sugar.set(), typing),
                                CoverageFactory.getInstance().get(med.size()),
                                null,
                                dog,
                                stats,
                                minSupp,
                                false,
                                typing);

                        int maxVariables = med.allPredicates().stream().mapToInt(p -> p.getS()).max().orElse(1) * run;
                        List<Variable> variables = LogicUtils.freshVariables(simplified, Predicate.create(predicate));
                        HornClause head = HornClause.create(new Literal(predicate.r, false, variables), Sugar.set());
                        LanguageBias<HornClause> lb = () -> (hornClause) -> {
                            if (hornClause.variables().size() > maxVariables) {
                                return false;
                            }
                            Set<Variable> body = hornClause.body().variables();
                            Set<Variable> headVariables = LogicUtils.variables(hornClause.head());

                            for (Variable headVariable : headVariables) {
                                if (body.contains(headVariable)) {
                                    return true;
                                }
                                // here can be multiple other restrictions
                            }
                            return false;
                        };

                        Function<SearchNodeInfo, SearchNodeInfo> evalStrategy = (candidate) -> {

                            System.out.println(candidate.getRule());

                            Pair<Set<Literal>, Set<Literal>> posNeg = dataset.evaluate(candidate.getRule());
                            int pos = posNeg.r.size();
                            int neg = posNeg.s.size();
                            //System.out.println("\t" + pos + "\t" + neg + "\t" + LogicUtils.isRangeRestricted(candidate.getRule()) + "\t" + candidate.saturatedBody());


                            candidate.setCoverages(totalCoverage, zeroCover);
                            if (0 == pos + neg) {
                                candidate.setAccuracy(-1.1);
                                candidate.setAllowability(true);
                                return candidate;
                            }

                            double acc = (1.0 * pos) / (pos + neg);
                            /*boolean rangeRestricted = LogicUtils.isRangeRestricted(candidate.getRule());
                            if (rangeRestricted) {
                                acc = acc - 1.0;
                            }*/

                            candidate.setAccuracy(acc);
                            candidate.setAllowability(true);
                            return candidate;
                        };

                        // monotonicity that comes from testing whether a query is a model of some evidence
                        // redundancy here, a big one, hopefully it will work
                        java.util.function.Predicate<SearchNodeInfo> levelFilter = sni -> sni.isAllowed();

                        BreadthResults result = sl.breadthFirstSearch(maxDepth, System.nanoTime(), 0, lb, beamSize, head, evalStrategy, topSnis, levelFilter);
                        System.out.println("HC found\t" + result.getRules(result.depths() - 1).size()
                                + "\t" + result.getRules(result.depths() - 1).stream().filter(sni -> LogicUtils.isRangeRestricted(sni.getRule()) && sni.getAccuracy() > 0).count());
                        result.getRules(result.depths() - 1).stream()
                                .filter(sni -> LogicUtils.isRangeRestricted(sni.getRule())
                                        && sni.getAccuracy() > 0)
                                .sorted(Comparator.comparing(SearchNodeInfo::getAccuracy).reversed())
                                .limit(bestN)
                                .forEach(sni -> {
                                    System.out.println("adding\t" + sni.getAccuracy() + "\t" + sni.getRule());
                                    theory.add(new Pair<>(sni.getAccuracy(), sni.getRule()));
                                });

                    }

                    System.out.println("\nending predicate\t" + predicate);

                });

        System.out.println("\n\n-------\nall final rules");
        theory.forEach(p -> System.out.println(p.r + "\t" + p.s));

        return theory;
    }

    public void learn(Set<Literal> evidence, Set<Clause> hardConstraints) {
        System.out.println("zkontrolovat jeste ten rule miner, asi tam je nejaka chyba");
        TimeDog dog = new TimeDog(15 * 60 * 1000_000_000, true); // divne toto
        int minSupp = 1;

        Clause evidenceClause = new Clause(evidence);
        long constraintTime = System.nanoTime();
        MEDataset med = MEDataset.create(Sugar.list(evidenceClause), Sugar.list(1.0), Matching.THETA_SUBSUMPTION);


        Map<Pair<Predicate, Integer>, Type> typing = null;

        if (null == hardConstraints) {
            UltraShortConstraintLearnerFaster cl = UltraShortConstraintLearnerFaster.create(med, 2, 4, 0, 1, 1, Integer.MAX_VALUE);
            Pair<List<Clause>, List<Clause>> constraints = cl.learnConstraints(true, false, false, Integer.MAX_VALUE, Sugar.list(), true, 2, false, Sugar.set(), minSupp);
            hardConstraints = Sugar.setFromCollections(constraints.getR());
        }
        long finalConstraintTime = System.nanoTime() - constraintTime;

        Coverage zeroCover = CoverageFactory.getInstance().take();
        Coverage totalCoverage = CoverageFactory.getInstance().take(0);

        Set<IsoClauseWrapper> theory = ConcurrentHashMap.newKeySet();

        int takeTopRules = 2;
        int numberOfSamples = 1000;
        int numberOfRuleSearchRuns = 1;
        Random random = new Random(99);

        boolean useNegativeSamples = false;

        int k = 4;
        int maxSubstitutions = 10000;

        Set<String> evaluatedPredicatesInFile1 = Sugar.set();/*"unpaymentdelinq/1",
                "politicalleadership1/1",
                "arable/1",
                "blockpositionindex/2",
                "agriculturalpop/1",
                "violentactions/2",
                "politicalparties/1",
                "censorship2/1",
                "accusations/1",
                "politicalleadership2/1",
                "negativebehavior/2",
                "seabornegoods/1",
                "age/1",
                "immigrantsmigrants/1",
                "freedomofopposition2/1",
                "rainfall/1",
                "protein/1",
                "timesinceally/2",
                "relngo/2",
                "relbooktranslations/2",
                "relexportbooks/2",
                "commonbloc1/2",
                "exports/1",
                "imports/1",
                "investments/1",
                "killeddomesticviolence/1",
                "censorship0/1",
                "incomeabs/1",
                "censorship1/1",
                "eExports/2",
                "militaryactions/2",
                "bureaucracy0/1"
        );*/

        Set<Clause> finalHardConstraints = hardConstraints;
        med.allPredicates().stream()
                //.filter(predicate -> predicate.getS() == 2 && predicate.getR().equals("interaction")) // debug filter
//                .filter(predicate -> !Sugar.set("location","function","complex","enzyme","protein_class","interaction").contains(predicate.getR()))
//                .filter(predicate -> !predicate.r.equals("phenotype"))
//                .filter(predicate -> predicate.r.equals("location"))
                .filter(predicate -> !evaluatedPredicatesInFile1.contains(Predicate.create(predicate).toString()))
                .forEach(predicate -> {
                    Set<IsoClauseWrapper> topSnis = Sugar.set();

                    for (int run = 0; run < numberOfRuleSearchRuns; run++) {

                        System.out.println("\n\n*******\nsolving for predicate " + predicate.getR() + "/" + predicate.getS() + "\trun\t" + run);

                        DatasetInterface dataset = null;
                        if (useNegativeSamples) {
                            System.out.println("using negative samples");
                            dataset = MLNsDataset.createDataset(evidenceClause, Predicate.create(predicate), numberOfSamples, finalHardConstraints, random);

                            if (dataset.getNegIdxs().isEmpty() || dataset.getPosIdxs().isEmpty()) {
                                System.out.println("skipping this dataset since it contains only one class (accuracy would be null thereafter)");
                                continue;
                            }
                        } else {
                            System.out.println("using PACAccuracyDataset with k\t" + k);
                            dataset = PACAccuracyDataset.create(evidence, k, maxSubstitutions);
                        }

                        MutableStats stats = new MutableStats();
                        SimpleLearner sl = new SimpleLearner(dataset,
                                ConstantSaturationProvider.createFilterSaturator(finalHardConstraints, Sugar.set()),
                                CoverageFactory.getInstance().get(dataset.size()),
                                null,
                                dog,
                                stats,
                                minSupp,
                                rangeRestricted,
                                typing);

                        int maxDepth = 1;
                        int maxVariables = k; // change k above, it is quite important to have same maxVariables as k, otherwise inefficiency of the computation scheme
                        int beam = 10;
                        HornClause head = HornClause.create(Clause.parse(predicate.getR() + "(" + IntStream.range(0, predicate.getS()).mapToObj(i -> "V" + i).collect(Collectors.joining(",")) + ")"));

                        //LanguageBias lb = () -> (clause) -> clause.variables().size() <= maxVariables;
                        // want a little bit more advanced, at least one variable from head must be in the body, otherwise it blows up
                        LanguageBias<HornClause> lb = () -> (hornClause) -> hornClause.variables().size() <= maxVariables;

                        Function<SearchNodeInfo, SearchNodeInfo> evalStrategy;
                        DatasetInterface finalDataset = dataset;
                        if (useNegativeSamples) {
                            evalStrategy = (candidate) -> {
                                Triple<Coverage, Coverage, MLNsDataset.EvaluationMetrics> triple = ((MLNsDataset) finalDataset).evaluate(candidate.getRule(), candidate.getParentsCoverage());
                                Coverage posCovered = triple.getR();
                                Coverage negCovered = triple.getS();
                                if (posCovered.size() + negCovered.size() < 1) {
                                    // takovy pravidlo by melo byt uplne zahozeno kvuli monotonicite
                                    return null;
                                    //return new SearchNodeInfo(zeroCover, zeroCover, horn, ....., false, totalCoverage);
                                }
                                double score = triple.getT().accuracy();
                                candidate.setCoverages(posCovered, negCovered);
                                candidate.setAccuracy(score);
                                candidate.setAllowability(true);
                                return candidate;
                            };
                        } else {
                            evalStrategy = (candidate) -> {
                                Pair<Coverage, Coverage> fakeCover = ((PACAccuracyDataset) finalDataset).classify(candidate.getRule(), candidate.getParentsCoverage());
                                Coverage posCovered = fakeCover.getR();
                                Coverage negCovered = fakeCover.getS();
                                double accuracy = ((PACAccuracyDataset) finalDataset).accuracyApprox(candidate.getRule());
                                boolean doNotRefine = Double.compare(1.0, accuracy) == 0;
                                candidate.setCoverages(posCovered, negCovered);
                                candidate.setAccuracy(accuracy);
                                candidate.setAllowability(!doNotRefine);
                                return candidate;
                            };
                        }

                        // monotonicity that comes from testing whether a query is a model of some evidence
                        // redundancy here, a big one, hopefully it will work
                        java.util.function.Predicate<SearchNodeInfo> levelFilter = sni -> sni.isAllowed();

                        BreadthResults result = sl.breadthFirstSearch(maxDepth, System.nanoTime(), finalConstraintTime, lb, beam, head, evalStrategy, topSnis, levelFilter);
                        result.getRules().stream()
                                .flatMap(sni -> sni.stream())
                                .forEach(sni -> System.out.println(sni.getAccuracy() + "\t" + sni.getRule()));

                        List<SearchNodeInfo> sorted = result.getRules().stream()
                                .flatMap(snis -> snis.stream())
                                .filter(sni -> sni.getAccuracy() > -1 || sni.getRule().body().countLiterals() < 1) // just a hack, -1 is for not range restricted clauses, -2 is for no existential matches at all
                                .filter(sni -> sni.getAccuracy() > 0.0) // just an evaluate hack to evaluate those which have at least one sampled true grounding
                                .sorted(Comparator.comparingDouble(SearchNodeInfo::getAccuracy).reversed())
                                .collect(Collectors.toList());

                        sorted.forEach(sni -> System.out.println(sni.getAccuracy() + "\t" + sni.getRule()));

                        System.out.println("# reasonable HC found\t" + sorted.size());
                        if (sorted.size() > 0) {
                            //firstSnis.add(sorted.get(0).getIsoClauseWrapper());
                            topSnis.addAll(sorted.subList(0, Math.min(sorted.size(), takeTopRules)).stream().map(SearchNodeInfo::getRuleICW).collect(Collectors.toList()));
                        }
                    }


                    System.out.println("\nending predicate\t" + predicate);
                    topSnis.forEach(iso -> System.out.println(iso.getOriginalClause()));
                    System.out.println("**************\n\n");

                    theory.addAll(topSnis);
                });

        System.out.println("\n\n-------\nall final rules");
        theory.forEach(iso -> System.out.println(iso.getOriginalClause()));
    }


    public static void main(String[] args) {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");
        System.out.println("threads:\t" + System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));


//        learn();
//        filterEquals();
//        parse();
//        retrievePredicates();
        fromLogToTheory();
    }

    private static void fromLogToTheory() {
        Path src = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\rules\\kinships-ntp_g\\ce_r5_v10_599.theory");
        try {
            List<Pair<Double, Clause>> pairs = Files.lines(src).map(String::trim)
                    .filter(line -> line.startsWith("["))
                    .map(line -> {
                        line = line.substring(1, line.length() - 1);
                        String[] split = line.split(",", 2);
                        assert 2 == split.length;
                        double weight = Double.parseDouble(split[0]);
                        Clause clause = HornClause.parse(split[1]).toClause();
                        return new Pair<>(weight, clause);
                    }).collect(Collectors.toList());
            Set<Clause> constraints = Utils.create().loadClauses(Paths.get("..", "datasets", "kinships-ntp_g", "train.nl_2_4.constraints"));
            Possibilistic theory = Possibilistic.create(constraints, pairs);
            System.out.println(theory.asOutput());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void filterEquals() {
        System.out.println("there is cut of to depth 3 only (3,4M rules, 166h of computation for filtering needed for level 4)");
        String domain = "nationsA2";
        Path constraintsFile = Paths.get("..", "datasets", domain, "constraints.txt");
        Path train = Paths.get("..", "datasets", domain, "train.db");
        Path rulePair = Paths.get(".", "pac", domain, "hornLearner.logic.withoutEqualFilter.pairs");
        Path output = Paths.get(".", "pac", domain, "hornLearner.logic");

        // puvodne to melo delat to zakomentovany, ale trvalo by to cele dny

        int k = 5;
        double minAcc = 0.90;

        Utils u = Utils.create();
        Set<Clause> constraints = u.loadClauses(constraintsFile);
        Matching matching = Matching.create(new Clause(LogicUtils.loadEvidence(train)), Matching.THETA_SUBSUMPTION);

        MutableDouble processed = new MutableDouble(0);

        Set<IsoClauseWrapper> in = ConcurrentHashMap.newKeySet();
        RuleSaturator saturator = RuleSaturator.create(constraints);
        try {
            long allLines = Files.lines(rulePair).count();
            List<Pair<Double, Clause>> pairs = Files.lines(rulePair)
                    .map(line -> {
                        processed.increment();
                        if (line.contains("#EmptyClause")) {
                            return null;
                        }
                        String[] splitted = line.trim().split("\t", 2);
                        if (splitted.length != 2) {
                            return null;
                        }
                        try {
                            double acc = Double.parseDouble(splitted[0]);
                            Clause clause = Clause.parse(splitted[1]);

                            if (acc < minAcc) {
                                return null;
                            }

                            if (clause.literals().size() > 4) {
                                return null;
                            }

                            Pair<Term[], List<Term[]>> substitutions = matching.allSubstitutions(new Clause(LogicUtils.flipSigns(LogicUtils.negativeLiterals(clause))), 0, Integer.MAX_VALUE);
                            boolean oneGroundSubstitutionsWithLQK = false;
                            for (Term[] terms : substitutions.s) {
                                if (Arrays.stream(terms).map(t -> (Constant) t).distinct().count() <= k) {
                                    oneGroundSubstitutionsWithLQK = true;
                                    break;
                                }
                            }
                            if (!oneGroundSubstitutionsWithLQK) {
                                return null;
                            }

                            System.out.println(processed.value() / allLines);

                            /*
                            return new Pair<>(acc, clause);
                            /**/
                            HornClause saturated = saturator.saturate(HornClause.create(clause));
                            if (null == saturated) {
                                System.out.println("");
                                return null;
                            }
                            Clause saturatedClause = saturated.toClause();
                            IsoClauseWrapper icw = IsoClauseWrapper.create(saturatedClause);
                            if (in.add(icw)) {
                                System.out.println(processed.value() / allLines);
                                return new Pair<>(acc, clause);
                            }
                            System.out.print(",");
                            return null;/**/
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .distinct()
                    .sorted(Comparator.comparingDouble(Pair<Double, Clause>::getR).reversed())
                    .collect(Collectors.toList());

            Files.write(Paths.get(output.toString() + ".pairs"), pairs.stream().map(p -> p.r + "\t" + p.s).collect(Collectors.toList()));
            Files.write(output, pairs.stream().map(p -> p.getS().toString()).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void retrievePredicates() {
        Path input = Paths.get(".", "pac", "nations", "hornLearner.2.logic");
        try {
            Files.lines(input)
                    .filter(line -> line.trim().length() > 0)
                    .map(line -> Predicate.create(Sugar.chooseOne(LogicUtils.positiveLiterals(Clause.parse(line))).getPredicate())).filter(Objects::nonNull)
                    .distinct()
                    .forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void parse() {
        double minAcc = Double.NEGATIVE_INFINITY;
        int maxK = 5;
        System.out.println("also trimming rules with accuracy below " + minAcc + "\t with maxK\t" + maxK);
        for (String domain : Sugar.list("kinships", "umls", "nationsA2")) {//,uwcs", "protein", "nations"
            //String domain = "umls-ntp";
            //{
            Path train = Paths.get("..", "datasets", domain, "train.db");
            Utils u = Utils.create();
            Matching matching = Matching.create(new Clause(LogicUtils.loadEvidence(train)), Matching.THETA_SUBSUMPTION);

            System.out.println(domain);
            //Path input = Paths.get(".", "pac", domain, "hornLearner.out." + domain + ".text");
            //Path output = Paths.get(".", "pac", domain, "hornLearner.logic");
            Path input = Paths.get(".", "pac", domain, "hornLearner.out." + domain + ".depth1.saturations.text");
            Path output = Paths.get(".", "pac", domain, "hornLearner.depth1.saturations.logic");
            try {
                List<Pair<Double, Clause>> pairs = Files.lines(input)
                        .map(line -> {
                            if (line.contains("#EmptyClause")) {
                                return null;
                            }
                            String[] splitted = line.trim().split("\t", 2);
                            if (splitted.length != 2) {
                                return null;
                            }
                            try {
                                double acc = Double.parseDouble(splitted[0]);

                                if (acc < minAcc) {
                                    System.out.println(line + "\t" + acc + "\t" + minAcc);
                                    return null;
                                }


                                HornClause horn = HornClause.parse(splitted[1]);
                                Pair<Term[], List<Term[]>> substitution = matching.allSubstitutions(horn.body(), 0, Integer.MAX_VALUE);
                                boolean atLeastOneKSizeGround = substitution.s.stream().anyMatch(arr -> Arrays.stream(arr).distinct().count() <= maxK);
                                if (!atLeastOneKSizeGround) {
                                    System.out.println("!");
                                    return null;
                                }

                                return new Pair<>(acc, horn.toClause());
                            } catch (Exception e) {
                                System.out.println("!");
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .distinct()
                        .sorted(Comparator.comparingDouble(Pair<Double, Clause>::getR).reversed())
                        .collect(Collectors.toList());
                Files.write(Paths.get(output.toString() + ".pairs"), pairs.stream().map(p -> p.r + "\t" + p.s).collect(Collectors.toList()));
                Files.write(output, pairs.stream().map(p -> p.getS().toString()).collect(Collectors.toList()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public static void learn() {
        // u vsech beam 10, max depth 4, max var 4, 10000 ground subsumpci hledani; u nations vypnout saturace (jinak to bezi prislis dlouho)

        String domain = "umls";//"kinships" "nations" "protein" "umls" "uwcs" "umls-ntp"
        Path evidencePath = Paths.get("..", "datasets", domain, "train.db");
        Path constraints = Paths.get("..", "datasets", domain, "constraints.txt");

//        Path evidencePath = Paths.get(".", "pac", "yago2", "train.db");
//        Path constraints = Paths.get(".", "pac", "yago2", "trainConstraintsLearned.txt");

        Utils u = Utils.create();

        HornLearner learner = new HornLearner();
        Set<Literal> evidence = LogicUtils.loadEvidence(evidencePath);
        Set<Clause> hardConstraints = u.loadClauses(constraints);
//        System.out.println("forget hard constraints for now :))");
//        hardConstraints = Sugar.set();
        learner.learn(evidence, hardConstraints);
    }


}
