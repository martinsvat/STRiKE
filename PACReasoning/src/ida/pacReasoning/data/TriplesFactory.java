package ida.pacReasoning.data;

import ida.ilp.logic.Constant;
import ida.ilp.logic.Literal;
import ida.utils.Sugar;

import java.util.Set;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 13. 5. 2021.
 */
public class TriplesFactory {

    public static Stream<Literal> generatAllPossibleTriplets(Set<Constant> constants, Set<String> binaryRelations) {
        return constants.stream().flatMap(e1 ->
                constants.stream().flatMap(e2 ->
                        binaryRelations.stream().map(relation -> new Literal(relation, Sugar.list(e1, e2)))));
    }
}
