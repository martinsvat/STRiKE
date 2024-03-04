package logicStuff.learning.saturation;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.Literal;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.Coverage;
import logicStuff.typing.Type;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Created by martin.svatos on 1. 2. 2018.
 */
public class ConstantSaturationProvider<T, K extends Saturator<T>> implements SaturatorProvider<T, K> {


    private final K saturator;

    public ConstantSaturationProvider(K saturator) {
        this.saturator = saturator;
    }

    @Override
    public K getSaturator() {
        return this.saturator;
    }

    @Override
    public boolean isUpdatable() {
        return false;
    }

    public static <T, K extends Saturator<T>> ConstantSaturationProvider create(K saturator) {
        return new ConstantSaturationProvider<>(saturator);
    }


    public static ConstantSaturationProvider<HornClause, StatefullSaturator<HornClause>> createFilterSaturator(Collection<Clause> hardRules, Collection<Clause> minSuppRules) {
        return createFilterSaturator(hardRules, minSuppRules, null);
    }

    public static ConstantSaturationProvider<HornClause, StatefullSaturator<HornClause>> createFilterSaturator(Collection<Clause> hardRules, Collection<Clause> minSuppRules, Map<Pair<ida.ilp.logic.Predicate, Integer>, Type> typing) {
        Saturator filter = minSuppRules.isEmpty() ? IdentitySaturator.create() : RuleSaturator.create(minSuppRules,Sugar.set(),typing,false); // another speed-up
        RuleSaturator saturator = RuleSaturator.create(hardRules,Sugar.set(),typing,false);

        return new ConstantSaturationProvider<>(new StatefullSaturator<HornClause>() {

            @Override
            public void nextDepth(TimeDog time) {
                return;
            }

            @Override
            public void update(HornClause horn, Coverage coveringExamples, Coverage parentsCovering) {
                return;
            }

            @Override
            public HornClause saturate(HornClause hornClause) {
                return saturate(hornClause, (l) -> false);
            }

            @Override
            public HornClause saturate(HornClause hornClause, Predicate<Literal> forbidden) {
                return saturator.saturate(hornClause, forbidden);
            }

            @Override
            public HornClause saturate(HornClause hornClause, Coverage parentsCoverage) {
                return saturate(hornClause, parentsCoverage, (l) -> false);
            }

            @Override
            public HornClause saturate(HornClause hornClause, Coverage parentsCoverages, Predicate<Literal> forbidden) {
                return this.saturate(hornClause);
            }

            @Override
            public boolean isPruned(HornClause hornClause) {
                return filter.isPruned(hornClause);// || saturator.isPruned(hornClause); // just because of speed-up of the process... the saturation may return null, meaning the rule is pruned
            }

            @Override
            public boolean isPruned(HornClause hornClause, Coverage examples) {
                return this.isPruned(hornClause);
            }

            @Override
            public Collection<Clause> getTheory() {
                return Sugar.set();
            }
        });
    }
}
