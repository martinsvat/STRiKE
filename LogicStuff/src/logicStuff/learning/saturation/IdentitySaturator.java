package logicStuff.learning.saturation;


import ida.ilp.logic.Clause;
import ida.ilp.logic.Literal;
import ida.utils.Sugar;
import logicStuff.learning.datasets.Coverage;

import java.util.Collection;
import java.util.function.Predicate;

/**
 * Simply returning the same clause as the input one (as a dummy saturator for not needing any work arounds).
 * <p>
 * Created by martin.svatos on 5. 12. 2017.
 */
public class IdentitySaturator<T> implements Saturator<T> {
    @Override
    public T saturate(T t) {
        return t;
    }

    @Override
    public T saturate(T t, Predicate<Literal> forbidden) {
        return t;
    }

    @Override
    public T saturate(T t, Coverage parentsCoverage) {
        return saturate(t);
    }

    @Override
    public T saturate(T t, Coverage parentsCoverages, Predicate<Literal> forbidden) {
        return t;
    }

    @Override
    public boolean isPruned(T t) {
        return false;
    }

    @Override
    public boolean isPruned(T t, Coverage examples) {
        return isPruned(t);
    }

    @Override
    public Collection<Clause> getTheory() {
        return Sugar.set();
    }

    public static <T> IdentitySaturator create() {
        return new IdentitySaturator();
    }
}
