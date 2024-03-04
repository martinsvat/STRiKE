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
import ida.utils.Sugar;
import ida.utils.collections.MultiList;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.Coverage;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Created by kuzelkao_cardiff on 24/01/17.
 */
public class SymmetrySaturator implements Saturator<Clause> {

    private MultiList<Pair<String, Integer>, int[]> symmetries = new MultiList<Pair<String, Integer>, int[]>();

    @Override
    public Clause saturate(Clause c) {
        Set<Literal> literals = new HashSet<Literal>();
        literals.addAll(c.literals());
        Pair<String, Integer> pair = new Pair<String, Integer>();
        for (Literal l : c.literals()) {
            pair.set(l.predicate(), l.arity());
            List<int[]> permutations;
            if ((permutations = this.symmetries.get(pair)) != null) {
                literals.addAll(induced(l, permutations));
            }
        }
        return new Clause(literals);
    }

    @Override
    public Clause saturate(Clause clause, Predicate<Literal> forbidden) {
        throw new IllegalStateException(); //NotImplementedException();
    }

    @Override
    public Clause saturate(Clause clause, Coverage parentsCoverage) {
        return saturate(clause, parentsCoverage, (l) -> false);
    }

    @Override
    public Clause saturate(Clause clause, Coverage parentsCoverages, Predicate<Literal> forbidden) {
        return saturate(clause, forbidden);
    }

    @Override
    public boolean isPruned(Clause clause) {
        return false; // this method does not prune any candide by deleting it!
    }

    @Override
    public boolean isPruned(Clause clause, Coverage examples) {
        return isPruned(clause);
    }

    @Override
    public Collection<Clause> getTheory() {
        throw new IllegalStateException(); //NotImplementedException();
    }

    //EMBARASSINGLY NAIVE!
    public Set<Literal> induced(Literal literal, List<int[]> permutations) {
        Set<Literal> retVal = new HashSet<Literal>();
        Set<Literal> newSet = new HashSet<Literal>();
        newSet.add(literal);
        Set<Literal> lastSet;
        do {
            lastSet = newSet;
            retVal.addAll(lastSet);
            newSet = new HashSet<Literal>();
            for (Literal l : lastSet) {
                for (int[] permutation : permutations) {
                    newSet.add(apply(l, permutation));
                }
            }
            newSet.removeAll(retVal);
        } while (!newSet.isEmpty());
        return retVal;
    }

    private Literal apply(Literal literal, int[] permutation) {
        Literal retVal = literal.copy();
        retVal.allowModifications(true);
        {
            System.out.println("TBD: check this out, this may not be working anymore because of the allowModifications call");
        }
        for (int i = 0; i < literal.arity(); i++) {
            retVal.set(literal.get(permutation[i]), i);
        }
        return retVal;
    }

    public void setSymmetries(String predicate, int arity, Collection<int[]> symmetries) {
        this.symmetries.set(new Pair<String, Integer>(predicate, arity), symmetries);
    }

    public static void main(String[] args) {
        Clause c = Clause.parse("atom(a,B,c)");
        SymmetrySaturator s = new SymmetrySaturator();
        s.setSymmetries("atom", 3, Sugar.<int[]>list(new int[]{1, 2, 0}));
        s.setSymmetries("bond", 4, Sugar.<int[]>list(new int[]{1, 0, 3, 2}));
        s.setSymmetries("bond", 3, Sugar.<int[]>list(new int[]{1, 0, 2}));
        System.out.println(s.saturate(c));
    }
}
