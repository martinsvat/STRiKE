package logicStuff.learning.modeBias;

import ida.ilp.logic.Clause;

import java.util.function.BiPredicate;

/**
 * Created by martin.svatos on 2. 3. 2018.
 */
public interface ModeDeclaration {

    public BiPredicate<Clause,Clause> predicate();
}
