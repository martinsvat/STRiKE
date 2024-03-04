package logicStuff.learning.saturation;

import ida.ilp.logic.HornClause;
import ida.utils.TimeDog;
import logicStuff.learning.datasets.Coverage;

/**
 * Created by martin.svatos on 2. 2. 2018.
 */
public interface StatefullSaturator<T> extends Saturator<T> {

    public void nextDepth(TimeDog time);

    public void update(HornClause horn, Coverage coveringExamples, Coverage parentsCovering);
}
