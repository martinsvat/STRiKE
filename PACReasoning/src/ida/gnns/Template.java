package ida.gnns;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.Literal;
import ida.utils.Sugar;

import java.util.List;
import java.util.Set;

/**
 * Created by martin.svatos on 9. 10. 2020.
 */
public class Template {


    private final Set<String> entities;
    private final Set<String> relations;
    private final List<Clause> rules;

    private Template(Set<String> entities, Set<String> relations, List<Clause> rules) {
        this.entities = entities;
        this.relations = relations;
        this.rules = rules;
    }

    public String printOut(String targetPredicate, int dimension) {
        StringBuilder sb = new StringBuilder();
        this.entities.forEach(entity -> {
            sb.append("{" + dimension + "} ee(" + entity + ") :- e(" + entity + ").\n");
        });
        sb.append("\n");
        this.relations.forEach(relation -> {
            sb.append("{" + dimension + "} er(" + relation + ") :- r(" + relation + ").\n");
        });
        sb.append("\n");
        // TODO u vystupu zkontrolovat jestli odpovidaji dimenze !!!!
        this.rules.forEach(rule -> {
            HornClause horn = HornClause.create(rule);
            if (horn.head().predicate().equals(targetPredicate)) {
                sb.append("{1," + dimension + "} ");
            }else if(horn.head().predicate().startsWith("u")){
                sb.append("");
            } else {
                sb.append("{" + dimension + "," + dimension + "} ");
//                sb.append("");
            }
            sb.append(horn.head() + " :- ");

            boolean first = true;
            for (Literal literal : horn.body().literals()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                if (literal.predicate().startsWith("*")
                        || (!first && (literal.predicate().equals("am") ||literal.predicate().equals("at") ))
                        || (first && literal.predicate().startsWith("u"))
                        ) {
                    sb.append(literal);
                }else if(literal.predicate().equals("p")) {
                    sb.append("{1," + dimension + "} " + literal);
                } else if (false && ("ee".equals(literal.predicate()) || "er".equals(literal.predicate()))){
                    sb.append(literal);
                } else {
                    sb.append("{" + dimension + "," + dimension + "} " + literal);
                }
            }
            sb.append(".\n");
        });
        return sb.toString();
    }

    public static Template create(Set<String> entities, Set<String> relations, List<Clause> rules) {
        return new Template(entities, relations, rules);
    }
}
