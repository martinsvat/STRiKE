package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import logicStuff.theories.GroundTheorySolver;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 10. 5. 2018.
 */
public class TheorySolver2 {

    private final int subsumptionMode = Matching.THETA_SUBSUMPTION;

    public Set<Literal> solve(Set<Clause> theory) {
        Set<Clause> groundRules = theory.stream().filter(LogicUtils::isGround).collect(Collectors.toSet());
        Set<Literal> state = Sugar.set();
        Set<Clause> rules = Sugar.setDifference(theory, groundRules);
        System.out.println("what are the init rules in Ondra's implementation?");
        /*????
                    violatedRules = Sugar.<Clause, Clause>funcallAndRemoveNulls(violatedRules, new Sugar.Fun<Clause, Clause>() {
                @Override
                public Clause apply(Clause clause) {
                    if (isGroundClauseVacuouslyTrue(clause, deterministic)) {
                        System.out.println("weird: " + clause + ", ~~~" + LogicUtils.flipSigns(clause));
                        return null;
                    } else {
                        return removeSpecialAndDeterministicPredicates(clause);
                    }
                }
            });
         */
        Set<Clause> activeRules = Sugar.set();
        activeRules.addAll(groundRules);
        Set<Constant> constants = Sugar.set();
        groundRules.forEach(rule -> constants.addAll(LogicUtils.constants(rule)));
        while (true) {
            state = satSolve(activeRules);
            if (null == state) {
                return null;
            }
            // can here be added only non-ground rules?
            List<Clause> violatedGround = violatedGround(theory, state,constants);
            if (violatedGround.isEmpty()) {
                break;
            }
            activeRules.addAll(violatedGround);
        }
        return state;
    }

    private List<Clause> violatedGround(Set<Clause> rules, Set<Literal> state, Set<Constant> constants) {
        Set<Constant> arguments = Sugar.set();
        for(Literal l : state){
            arguments.removeAll(LogicUtils.constantsFromLiteral(l));
        }

        Literal introductionLiteral = new Literal("", true, Sugar.listFromCollections(arguments));
        Iterable<Literal> literals = Sugar.iterable(state, Sugar.list(introductionLiteral));
        Matching world = Matching.create(new Clause(literals), subsumptionMode);
        List<Clause> violated = Sugar.list();
        for (Clause rule : rules) {
            Pair<Term[], List<Term[]>> substitutions = world.allSubstitutions(LogicUtils.flipSigns(rule), 0, Integer.MAX_VALUE);
            for (Term[] subs : substitutions.s) {
                violated.add(LogicUtils.substitute(rule, substitutions.r, subs));
            }
        }
        return violated;
    }

    private Set<Literal> satSolve(Set<Clause> satProblem) {
        return new GroundTheorySolver(Sugar.setFromCollections(satProblem)).solve();
    }

    public static TheorySolver2 create() {
        System.out.println("TS2: watchout, special predicates are not welcomed here, even not tested!");
        return new TheorySolver2();
    }

    public static void main() {
        // place for testing and debugging with TheorySolver


    }
}
