package logicStuff.learning.modeBias;

import ida.ilp.logic.*;
import ida.utils.Sugar;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Created by martin.svatos on 2. 3. 2018.
 */
public class MolecularMode implements ModeDeclaration {
    private final Map<Predicate, Integer> maxOccurences;

    public MolecularMode(Map<Predicate, Integer> maxOccurences) {
        // ok this is not mode, it is rather LB, but for simplicity put here
        this.maxOccurences = maxOccurences;
    }

    @Override
    public BiPredicate<Clause, Clause> predicate() {
        return (children, parent) -> {
            // mode(Xatom(-)).
            // mode(Xbond(-,-)).
            if (parent.countLiterals() < 1) {
                return true;
            }
            Set<Literal> diff = Sugar.setDifference(children.literals(), parent.literals());
            if (diff.size() != 1) {
                throw new IllegalStateException("'" + children + "' no child of '" + parent + "'");
            }

            // maxPredicates(bond,X).
            if (violatesMaxOccurences(children)) {
                return false;
            }

            Literal literal = Sugar.chooseOne(diff);
            if (literal.arity() == 1) {
                // mode(Xatom(+)).
                return parent.variables().contains(Sugar.chooseOne(LogicUtils.variables(literal)));
            } else if (literal.arity() == 2) {
                Variable var1 = (Variable) literal.arguments()[0];
                Variable var2 = (Variable) literal.arguments()[1];
                Set<Variable> parentsVaraibles = parent.variables();
                if (parentsVaraibles.contains(var1) || parentsVaraibles.contains(var2)) {
                    // either
                    // mode(Xbond(+,+)).
                    // mode(Xbond(-,+)).
                    // mode(Xbond(+,-)).
                    return true;
                }
                return false;
            }
            System.out.println(literal);
            System.out.println(literal.arity());
            System.out.println(parent);
            System.out.println(children);
            throw new IllegalStateException("No rule for predicate " + literal.predicate() + "/" + literal.arity());
        };
    }

    private boolean violatesMaxOccurences(Clause clause) {
        return maxOccurences.entrySet().stream()
                .anyMatch(entry -> entry.getValue()
                        < clause.literals().stream()
                        .filter(l -> l.arity() == entry.getKey().getArity()
                                // the endsWith is here just a hack for am_bond/2, ar_bond/2
                                && l.predicate().endsWith(entry.getKey().getName()))
                        .count());
    }

    public static MolecularMode create() {
        // ok this is not mode, it is rather LB, but for simplicity put here
        String params = System.getProperty("ida.logicStuff.mode", "");
        Map<Predicate, Integer> maxOccurences = new HashMap<>();
        if (params.length() > 0) {
            System.out.println("constructing molecular mode with ida.logicStuff.mode=" + params);
            System.out.println("TODO: do not forget that the current setting takes simplified input -- bond-arity-maxOcc is applied to all predicates ending with the 'bond' part, e.g. bond-2-2 is applied for ar_bond as well as am_bond,...");
            String[] splitted = params.split("-");
            if (splitted.length % 3 != 0) {
                throw new IllegalArgumentException("format of mode declaration for max predicate is '[predicate-arity-maxOccurence[-predicate2-arity2-maxOccurences[-...]]]'; but obtained instead '" + params + "'");
            }
            for (int idx = 0; idx < splitted.length; idx += 3) {
                maxOccurences.put(PredicateFactory.getInstance().create(splitted[idx], Integer.parseInt(splitted[idx + 1])),
                        Integer.parseInt(splitted[idx + 2]));
            }
        }
        return new MolecularMode(maxOccurences);
    }


    public static void main(String[] args) {
        // maxOcc test
        System.setProperty("ida.logicStuff.mode", "bond-2-2");
        MolecularMode md = MolecularMode.create();
//        Clause parent = Clause.parse("bond(Y,Z),bond(Z,W)");
//        Clause child = Clause.parse("bond(X,Y),bond(Y,Z),bond(Z,W)");
        Clause parent = Clause.parse("ar_bond(Y,Z),ar_bond(Z,W)");
        Clause child = Clause.parse("ar_bond(X,Y),ar_bond(Y,Z),ar_bond(Z,W)");
        System.out.println(child + "\n" + parent + "\nres:\t" + md.predicate().test(child, parent));
        System.out.println(md.violatesMaxOccurences(child));
    }
}





















