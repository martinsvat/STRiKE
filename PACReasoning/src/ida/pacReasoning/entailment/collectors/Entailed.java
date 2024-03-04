package ida.pacReasoning.entailment.collectors;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;

/**
 * Created by martin.svatos on 7. 6. 2018.
 */
public interface Entailed {

    /**
     * Returns double value if a query is entailed; otherwise returns null.
     * @param query
     * @return
     */
    Double getEntailedValue(Literal query);
//zkontrolovat tady aby nebylo underflow v pripade double.min_value - penalta za ranking! to mohlho delat ty rozdilne vysledky
    void add(Literal query, Subset subset);

    String asOutput();

    Entailed removeK(int k);

    <T extends Entailed> T setTime(long time);

    long getTime();

}
