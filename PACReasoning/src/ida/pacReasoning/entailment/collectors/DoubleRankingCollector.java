package ida.pacReasoning.entailment.collectors;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;

/**
 * Creates new collector which firstly returns value of the first collector given if a literal is entailed; if it isn't, value with some great penalty is taken from the second collector.
 * <p>
 * Created by martin.svatos on 17. 5. 2021.
 */
public class DoubleRankingCollector implements Entailed {

    {
        System.out.println("Watch out, DoubleRankingCollector is in sigmoid mode, meaning it works correctly only iff input is within the (0,1) interval!");
    }

    private final Entailed upperValues;
    private final Entailed basePenaltyValues;

    public DoubleRankingCollector(Entailed upperValues, Entailed basePenaltyValues) {
        this.upperValues = upperValues;
        this.basePenaltyValues = basePenaltyValues;
    }

    @Override
    public Double getEntailedValue(Literal query) {
        Double value = this.upperValues.getEntailedValue(query);
        if (null == value) {
            value = this.basePenaltyValues.getEntailedValue(query) - 1.0; // -1.0 cause floating point
        }
        return value;
    }

    @Override
    public void add(Literal query, Subset subset) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String asOutput() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Entailed removeK(int k) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends Entailed> T setTime(long time) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getTime() {
        return 0;
    }

    public static Entailed create(Entailed upperValues, Entailed basePenaltyValues) {
        return new DoubleRankingCollector(upperValues, basePenaltyValues);
    }
}
