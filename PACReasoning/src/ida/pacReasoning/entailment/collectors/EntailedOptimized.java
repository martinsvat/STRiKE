package ida.pacReasoning.entailment.collectors;

import ida.utils.tuples.Triple;

/**
 * Memory optimized version based on using triplet <o, r, s> of integers
 *
 * Created by martin.svatos on 7. 6. 2018.
 */
public interface EntailedOptimized {

    /**
     * Returns double value if a query is entailed; otherwise returns null.
     * @param query
     * @return
     */
    Double getEntailedValue(Triple<Integer,Integer,Integer> query);

    String asOutput();


}
