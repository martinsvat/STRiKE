package logicStuff.learning.languageBias;

import ida.ilp.logic.Clause;

import ida.ilp.logic.LogicUtils;
import ida.utils.tuples.Pair;

import java.util.Set;
import java.util.function.Predicate;

/**
 * Created by martin.svatos on 19. 12. 2017.
 */
public class DummyBias implements LanguageBias<Clause> {
    private final int maxPosLiterals;
    private final int maxNegLiterals;
    private final int maxLiteral;
    private final int maxVariables;
    private final Set<Pair<String, Integer>> forbiddenPredicates;
    private final Predicate<Clause> predicate;

    public DummyBias(int maxPosLiterals, int maxNegLiterals, int maxLiteral, int maxVariables, Set<Pair<String, Integer>> forbiddenPredicates) {
        this.maxPosLiterals = maxPosLiterals;
        this.maxNegLiterals = maxNegLiterals;
        this.maxLiteral = maxLiteral;
        this.maxVariables = maxVariables;
        this.forbiddenPredicates = forbiddenPredicates;
        //old version for clause
        this.predicate = (clause) -> clause.literals().size() <= maxLiteral
                && LogicUtils.negativeLiterals(clause).size() <= maxNegLiterals
                && LogicUtils.positiveLiterals(clause).size() <= maxPosLiterals
                && clause.variables().size() <= maxVariables
                && clause.literals().stream().filter(literal -> forbiddenPredicates.contains(new Pair<>(literal.predicate(), literal.arity()))).count() < 1;

        /*// this won't work for horn clause, not for
        this.predicate = (hornClause) -> {
            return hornClause.countLiterals() <= maxLiteral
                    && hornClause.body().countLiterals() <= maxNegLiterals
                    && (maxPosLiterals < 1 ? null == hornClause.head() : true)
                    && hornClause.variables().size() <= maxVariables
                    && hornClause.body().literals().stream().filter(literal -> forbiddenPredicates.contains(new Pair<>(literal.predicate(), literal.arity()))).count() < 1
                    && (null == hornClause.head() || !forbiddenPredicates.contains(hornClause.head().getPredicate()));
        };*/
    }

    @Override
    public Predicate<Clause> predicate() {
        return this.predicate;
    }


    public static DummyBias create(int maxPosLiterals, int maxNegLiterals, int maxLiteral, int maxVariables, Set<Pair<String, Integer>> forbiddenLiterals) {
        return new DummyBias(maxPosLiterals, maxNegLiterals, maxLiteral, maxVariables, forbiddenLiterals);
    }
}
