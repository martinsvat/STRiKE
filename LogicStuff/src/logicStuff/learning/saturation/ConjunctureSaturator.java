package logicStuff.learning.saturation;

import ida.ilp.logic.Clause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.Predicate;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.Coverage;
import logicStuff.typing.Type;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Saturator for conjuctions. Be aware that pos and neg saturations are wrt to clause literals (negation), not for literals of conjunction.
 * <p>
 * Created by martin.svatos on 26. 11. 2017.
 */
public class ConjunctureSaturator implements Saturator<Clause> {
    private final boolean negativeClauseSaturation;
    private final boolean positiveClauseSaturation;
    private final List<Clause> constraints;
    private final RuleSaturator ruleSaturator;

    public ConjunctureSaturator(List<Clause> constraints, boolean positiveClauseSaturation, boolean negativeClauseSaturation, Set<Pair<String, Integer>> allowedPredicates, Map<Pair<Predicate, Integer>, Type> types) {
        this.constraints = constraints;
        this.positiveClauseSaturation = positiveClauseSaturation;
        this.negativeClauseSaturation = negativeClauseSaturation;
        this.ruleSaturator = RuleSaturator.create(constraints, allowedPredicates, types, false);
    }

    @Override
    public Clause saturate(Clause conjunction) {
        return saturate(conjunction, (l) -> false);
    }

    @Override
    public Clause saturate(Clause conjunction, java.util.function.Predicate<Literal> isForbidden) {
//        Clause disjunction = LogicUtils.flipSigns(conjunction);
        Clause saturation = conjunction;

        if (this.negativeClauseSaturation && this.positiveClauseSaturation) {
            saturation = this.ruleSaturator.saturate(conjunction, isForbidden);
        } else if (this.negativeClauseSaturation) {
            saturation = this.ruleSaturator.negativeSaturation(conjunction, isForbidden);
        } else if (this.positiveClauseSaturation) {
            saturation = this.ruleSaturator.positiveSaturation(conjunction, isForbidden);
        }

        if (null == saturation) {
            return null;
        }

        return saturation;//LogicUtils.flipSigns(saturation);
    }

    @Override
    public Clause saturate(Clause clause, Coverage parentsCoverage) {
        return saturate(clause, parentsCoverage, (l) -> false);
    }

    @Override
    public Clause saturate(Clause clause, Coverage parentsCoverages, java.util.function.Predicate<Literal> forbidden) {
        return saturate(clause, forbidden);
    }


    @Override
    public boolean isPruned(Clause clause) {
        return this.ruleSaturator.isPruned(clause);
    }

    @Override
    public boolean isPruned(Clause clause, Coverage examples) {
        return isPruned(clause);
    }

    @Override
    public Collection<Clause> getTheory() {
        return this.ruleSaturator.getTheory();
    }

    public static ConjunctureSaturator create(List<Clause> theory, Pair<Boolean, Boolean> negativePositiveSaturationsMode, Collection<Predicate> allowedPredicates) {
        return create(theory, negativePositiveSaturationsMode.s, negativePositiveSaturationsMode.r, allowedPredicates, null);
    }

    public static ConjunctureSaturator create(List<Clause> theory, Pair<Boolean, Boolean> negativePositiveSaturationsMode, Collection<Predicate> allowedPredicates, Map<Pair<Predicate, Integer>, Type> types) {
        return create(theory, negativePositiveSaturationsMode.s, negativePositiveSaturationsMode.r, allowedPredicates, types);
    }

    public static ConjunctureSaturator create(List<Clause> theory, boolean positiveClauseSaturation, boolean negativeClauseSaturation, Collection<Predicate> allowedPredicates, Map<Pair<Predicate, Integer>, Type> types) {
        // it should have name positiveDisjunctionSaturations, etc....
        /*if(!positiveClauseSaturation && !negativeClauseSaturation){
            throw new IllegalStateException("does not give any sense ;)");
        }*/
        return new ConjunctureSaturator(theory,
                positiveClauseSaturation,
                negativeClauseSaturation,
                allowedPredicates.stream().map(predicate -> (Pair<String, Integer>) predicate.getPair()).collect(Collectors.toSet()),
                types);
    }

    public static ConjunctureSaturator create(List<Clause> theory, boolean positiveClauseSaturation, boolean negativeClauseSaturation, Map<Pair<Predicate, Integer>, Type> types) {
        return create(theory, positiveClauseSaturation, negativeClauseSaturation, Sugar.list(), types);
    }

    public static ConjunctureSaturator create(List<Clause> theory, boolean positiveClauseSaturation, boolean negativeClauseSaturation) {
        return create(theory, positiveClauseSaturation, negativeClauseSaturation, null);
    }

    public static ConjunctureSaturator create(List<Clause> theory) {
        return create(theory, false, false);
    }
}
