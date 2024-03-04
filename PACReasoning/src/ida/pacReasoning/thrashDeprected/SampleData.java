package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.Clause;
import ida.ilp.logic.Literal;
import ida.utils.Sugar;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 23. 3. 2018.
 */
public class SampleData {

    public static Set<Clause> t1Theory = Sugar.set("!fr(X,Y),!sm(X),sm(Y)").stream().map(Clause::parse).collect(Collectors.toSet());
    public static Set<Literal> t1Evidence = Sugar.set(
            "fr(bob,alice)", "sm(bob)",
            "fr(burtik,alice)", "sm(burtik)",
            "fr(d,alice)", "sm(d)"
    ).stream().map(Literal::parseLiteral).collect(Collectors.toSet());
    public static Set<Literal> t1Queries = Sugar.set("sm(alice)", "sm(burtik)","sm(q)").stream().map(Literal::parseLiteral).collect(Collectors.toSet());


}
