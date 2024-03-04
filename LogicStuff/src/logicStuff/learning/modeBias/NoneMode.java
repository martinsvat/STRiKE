package logicStuff.learning.modeBias;

import ida.ilp.logic.Clause;

import java.util.function.BiPredicate;

/**
 * Created by martin.svatos on 2. 3. 2018.
 */
public class NoneMode implements ModeDeclaration {
    @Override
    public BiPredicate<Clause, Clause> predicate() {
        return (child, parent) -> true;
    }

    public static ModeDeclaration create() {
        return new NoneMode();
    }
}
