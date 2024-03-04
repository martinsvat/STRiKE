package ida.searchPruning;

import ida.ilp.logic.Clause;
import ida.ilp.logic.LeastHerbrandModel;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.collections.MultiMap;
import logicStuff.learning.saturation.RuleSaturator;
import logicStuff.theories.TheorySolver;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 26. 9. 2017.
 */
public class GraphSaturator {


    public static void main(String args[]) {
        /*args = new String[]{
                //"bond_1(a),c(3),o(2),bond_1(b),bond(a,3),bond(a,b),bond(a,1),bond(a,2),bond(1,a),bond(3,a),bond(2,a),bond(b,a)",
                "bond_1(Xa),c(X3),o(X2),bond_1(Xb),bond(Xa,X3),bond(Xa,Xb),bond(Xa,X1),bond(Xa,X2),bond(X1,Xa),bond(X3,Xa),bond(X2,Xa),bond(Xb,Xa)",
                "c(X0),!o(X1),!bond_1(X2),!bond(X2,X0),!bond(X0,X2),!bond(X3,X1),!bond_1(X3),!bond(X1,X3)",
                "c(X0),!o(X1),!c(X2),!bond(X2,X6),!bond_1(X4),!bond_1(X5),!bond(X3,X0),!bond(X1,X4),!bond(X6,X2),!bond(X4,X1),!bond(X0,X3),!bond(X5,X1),!bond_1(X6),!bond(X1,X5),!bond_1(X3)",
                "o(X1),!c(X0),!bond_1(X2),!bond(X2,X0),!bond(X0,X2),!bond(X3,X1),!bond_1(X3),!bond(X1,X3)",
                "c(X2),!c(X0),!o(X1),!bond(X2,X6),!bond_1(X4),!bond_1(X5),!bond(X3,X0),!bond(X1,X4),!bond(X6,X2),!bond(X4,X1),!bond(X0,X3),!bond(X5,X1),!bond_1(X6),!bond(X1,X5),!bond_1(X3)",
                "o(X1),!c(X0),!c(X2),!bond(X2,X6),!bond_1(X4),!bond_1(X5),!bond(X3,X0),!bond(X1,X4),!bond(X6,X2),!bond(X4,X1),!bond(X0,X3),!bond(X5,X1),!bond_1(X6),!bond(X1,X5),!bond_1(X3)",
        };*/
        args = new String[]{
                "1(X3),1(X2),bond(X1,X5),bond(X1,X2),bond(X1,X4),bond(X3,X5),bond(X3,X2),bond(X3,X4),bond(X2,X5),bond(X2,X1),bond(X2,X3),bond(X5,X1),bond(X5,X2),bond(X5,X3),bond(X5,X4),bond(X4,X5),bond(X4,X1),bond(X4,X3)",
                "2(X3),!1(X0),!1(X1),!1(X2),!1(X4),!1(X5),!bond(X0,X1),!bond(X1,X0),!bond(X1,X2),!bond(X2,X3),!bond(X2,X1),!bond(X3,X4),!bond(X3,X2),!bond(X4,X3),!bond(X4,X5),!bond(X5,X4)",
                //"1_bond(Xb2),1_bond(Xb3),o(Xx2),c(Xx3),c(Xx0),c(Xx1),bond(Xb1,Xx2),bond(Xb1,Xx0),bond(Xb2,Xx1),bond(Xb2,Xx0),bond(Xb3,Xx2),bond(Xb3,Xx3),bond(Xx2,Xb3),bond(Xx2,Xb1),bond(Xx3,Xb3),bond(Xx0,Xb1),bond(Xx0,Xb2),bond(Xx1,Xb2)",
                //"c(X2),!c(X0),!c(X1),!o(X3),!c(X4),!bond(X6,X1),!bond(X2,X6),!bond(X0,X5),!bond(X7,X3),!bond(X7,X2),!bond(X8,X3),!bond(X1,X5),!bond(X6,X2),!bond(X1,X6),!bond(X5,X1),!bond(X5,X0),!1_bond(X5),!1_bond(X8),!1_bond(X7),!bond(X8,X4),!bond(X3,X8),!1_bond(X6),!bond(X4,X8),!bond(X2,X7),!bond(X3,X7)",
                /*"c(X4),!c(X0),!c(X1),!c(X2),!c(X3),!o(X5),!bond(X6,X1),!1_bond(X10),!bond(X5,X10),!bond(X1,X7),!bond(X4,X9),!1_bond(X7),!bond(X9,X3),!bond(X3,X9),!bond(X8,X3),!bond(X10,X4),!bond(X7,X1),!1_bond(X8),!bond(X2,X7),!bond(X6,X0),!bond(X9,X4),!bond(X8,X2),!bond(X1,X6),!bond(X3,X8),!1_bond(X6),!bond(X4,X10),!bond(X0,X6),!bond(X7,X2),!bond(X2,X8),!bond(X10,X5),!1_bond(X9)",
                "c(X2),!c(X0),!c(X1),!c(X3),!c(X4),!o(X5),!bond(X6,X1),!1_bond(X10),!bond(X1,X7),!1_bond(X7),!bond(X9,X3),!bond(X3,X9),!bond(X10,X4),!bond(X2,X9),!bond(X7,X1),!1_bond(X8),!bond(X2,X7),!bond(X6,X0),!bond(X8,X2),!bond(X3,X10),!bond(X5,X8),!bond(X1,X6),!bond(X9,X2),!1_bond(X6),!bond(X4,X10),!bond(X0,X6),!bond(X7,X2),!bond(X8,X5),!bond(X2,X8),!1_bond(X9),!bond(X10,X3)",
                "c(X3),!c(X0),!c(X1),!c(X2),!c(X4),!o(X5),!bond(X6,X1),!1_bond(X10),!bond(X5,X10),!bond(X1,X7),!bond(X4,X9),!1_bond(X7),!bond(X9,X3),!bond(X3,X9),!bond(X8,X3),!bond(X7,X1),!1_bond(X8),!bond(X2,X7),!bond(X6,X0),!bond(X9,X4),!bond(X8,X2),!bond(X3,X10),!bond(X1,X6),!bond(X3,X8),!1_bond(X6),!bond(X0,X6),!bond(X7,X2),!bond(X2,X8),!bond(X10,X5),!1_bond(X9),!bond(X10,X3)",
                "c(X2),!c(X0),!c(X1),!c(X3),!o(X4),!bond(X6,X1),!bond(X2,X6),!bond(X0,X5),!bond(X8,X2),!bond(X7,X2),!bond(X8,X3),!bond(X1,X5),!bond(X6,X2),!bond(X1,X6),!bond(X2,X8),!bond(X5,X1),!bond(X5,X0),!1_bond(X5),!1_bond(X8),!1_bond(X7),!bond(X4,X7),!bond(X3,X8),!1_bond(X6),!bond(X2,X7),!bond(X7,X4)",
                "c(X3),!c(X0),!c(X1),!c(X2),!o(X4),!bond(X7,X3),!bond(X6,X1),!bond(X2,X6),!bond(X0,X5),!bond(X8,X3),!bond(X7,X2),!bond(X1,X5),!bond(X6,X2),!bond(X1,X6),!bond(X5,X1),!bond(X5,X0),!1_bond(X5),!1_bond(X8),!1_bond(X7),!bond(X8,X4),!bond(X3,X8),!1_bond(X6),!bond(X4,X8),!bond(X2,X7),!bond(X3,X7)",
                "c(X1),!c(X0),!c(X2),!o(X3),!bond(X6,X1),!bond(X2,X6),!bond(X1,X5),!bond(X1,X4),!bond(X0,X4),!bond(X1,X6),!1_bond(X4),!bond(X5,X1),!bond(X4,X0),!1_bond(X5),!bond(X4,X1),!bond(X6,X2),!1_bond(X6),!bond(X5,X3),!bond(X3,X5)",
                "c(X1),!c(X0),!o(X2),!c(X3),!bond(X2,X6),!bond(X6,X3),!bond(X3,X6),!bond(X1,X5),!bond(X1,X4),!bond(X0,X4),!bond(X4,X1),!1_bond(X4),!bond(X5,X1),!bond(X4,X0),!1_bond(X5),!bond(X6,X2),!bond(X5,X2),!bond(X2,X5),!1_bond(X6)"
                */
        };

        Clause example = LogicUtils.constantizeClause(Clause.parse(args[0]));// better to be safe than sorry.... chovam se k tomu vstupu jako ke ground examplu ikdyby
        String[] finalArgs = args;
        List<Clause> rules = IntStream.range(1, args.length).mapToObj(i -> Clause.parse(finalArgs[i])).collect(Collectors.toList());
        example.literals().forEach(literal -> rules.add(Clause.parse(literal.toString())));

        LeastHerbrandModel grounder = new LeastHerbrandModel();
        Set<Literal> output = grounder.herbrandModel(rules);

        Clause result = new Clause(output);
        //result = LogicUtils.variabilizeClause(result);// vlastne by nemuselo byt
        System.out.println(result.toString());
        if (constantsHasMultipleLabels(result)) {
            System.out.println("saturation is empty since one constant is within multiple unary predicates (one vertex has multiple labels)");
        }
    }

    private static boolean constantsHasMultipleLabels(Clause clause) {
        MultiMap<String, String> map = new MultiMap<>();
        clause.literals().stream()
                .filter(literal -> literal.arity() == 1).
                forEach(literal -> map.put(literal.arguments()[0].name(), literal.predicate()));

        for (Map.Entry<String, Set<String>> entry : map.entrySet()) {
            if (entry.getValue().size() > 1) {
                return true;
            }
        }

        return false;
    }

}
