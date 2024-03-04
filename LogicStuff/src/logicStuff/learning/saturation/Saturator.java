/*
 * Copyright (c) 2015 Ondrej Kuzelka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package logicStuff.learning.saturation;


import ida.ilp.logic.Clause;
import ida.ilp.logic.Literal;
import logicStuff.learning.datasets.Coverage;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;


/**
 * Created by kuzelkao_cardiff on 24/01/17.
 */
public interface Saturator<T> {

    T saturate(T t);

    T saturate(T t, Predicate<Literal> forbidden);

    T saturate(T t, Coverage parentsCoverage);

    T saturate(T t, Coverage parentsCoverages, Predicate<Literal> forbidden);

    /**
     * is the give clause inconsistent with the theory given?
     *
     * @param t
     * @return
     */
    boolean isPruned(T t);

    boolean isPruned(T t, Coverage examples);

    // TODO
    // ma najit saturator pro dany covered a ten bude obsahovat constrainy jen s negacema
    //boolean nonEmptyLFC(Coverage covered);

    Collection<Clause> getTheory();
}
