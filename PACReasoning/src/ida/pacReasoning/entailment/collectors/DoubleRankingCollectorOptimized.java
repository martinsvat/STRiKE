package ida.pacReasoning.entailment.collectors;

import ida.utils.tuples.Triple;

/**
 * Creates new collector which firstly returns value of the first collector given if a literal is entailed; if it isn't, value with some great penalty is taken from the second collector.
 * <p>
 * Created by martin.svatos on 17. 5. 2021.
 */
public class DoubleRankingCollectorOptimized implements EntailedOptimized {
    private final EntailedOptimized upperValues;
    private final EntailedOptimized basePenaltyValues;

    public DoubleRankingCollectorOptimized(EntailedOptimized upperValues, EntailedOptimized basePenaltyValues) {
        this.upperValues = upperValues;
        this.basePenaltyValues = basePenaltyValues;
    }

    @Override
    public Double getEntailedValue(Triple<Integer,Integer,Integer> query) {
        Double value = this.upperValues.getEntailedValue(query);
        if (null == value) {
            value = this.basePenaltyValues.getEntailedValue(query) - 100.0;
        }
        return value;
    }

    @Override
    public String asOutput() {
        throw new IllegalStateException();// NotImplementedException();
    }

    public static EntailedOptimized create(EntailedOptimized upperValues, EntailedOptimized basePenaltyValues) {
        return new DoubleRankingCollectorOptimized(upperValues, basePenaltyValues);
    }
}
