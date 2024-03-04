package ida.pacReasoning.entailment.nodes;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.theories.Theory;
import ida.utils.Sugar;

import java.util.Set;

/**
 * Created by martin.svatos on 9. 6. 2018.
 */
public class PossLogicNode implements MutableNode {
    private final Subset subset;
    private Theory theory;
    private Set<Literal> entailedByAncestor;
    private Set<Literal> entailed;
    private boolean needToEvaluate;
    private boolean theoriesWithDifferentSizes;

    public PossLogicNode(Subset subset, Theory theory, Set<Literal> entailedByDirectAncestor, Set<Literal> entailed, boolean needToEvaluate) {
        this.subset = subset;
        this.theory = theory;
        this.entailedByAncestor = entailedByDirectAncestor;
        this.entailed = entailed;
        this.needToEvaluate = needToEvaluate;
        this.theoriesWithDifferentSizes = false; // i.e. by default we have to check consistency of the mask and entailed literals
    }

    @Override
    public Subset getConstants() {
        return subset;
    }

    @Override
    public Theory getTheory() {
        return theory;
    }

    @Override
    public Set<Literal> entailed() {
        return entailed;
    }

    @Override
    public Set<Literal> entailedByAncestor() {
        return entailedByAncestor;
    }

    @Override
    public boolean evaluate() {
        return needToEvaluate;
    }

    @Override
    public boolean checkConsistency() {
        return !theoriesWithDifferentSizes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PossLogicNode that = (PossLogicNode) o;

        boolean equals = subset != null ? subset.equals(that.subset) : that.subset == null;

        if (equals) {
            synchronized (this) {
                synchronized (that) {
                    Set<Literal> unionOfAncestors = null;
                    // if theories in the layer from all nodes are not of the same length, then forget what was implied by ancestors
                    if (this.theoriesWithDifferentSizes || that.theoriesWithDifferentSizes || this.getTheory().size() != that.getTheory().size()) {
                        this.theoriesWithDifferentSizes = true;
                        that.theoriesWithDifferentSizes = true;
                        this.needToEvaluate = true;
                        unionOfAncestors = Sugar.set();
                        if (this.theory.size() < that.theory.size()) {
                            that.theory = this.theory;
                        } else {
                            this.theory = that.theory;
                        }
                    }

                    // no need to set this/that.theoriesWithDifferentSizes (to or value of this and that) since if at least one is true, they are set upe in the if section

                    unionOfAncestors = null == unionOfAncestors ? Sugar.union(this.entailedByAncestor, that.entailedByAncestor) : unionOfAncestors;
                    this.entailedByAncestor = unionOfAncestors;
                    that.entailedByAncestor = unionOfAncestors;

                    Set<Literal> unionOfEntailed = Sugar.union(this.entailed, that.entailed);
                    this.entailed = unionOfEntailed;
                    that.entailed = unionOfEntailed;
                    this.needToEvaluate = this.needToEvaluate || that.needToEvaluate;
                    that.needToEvaluate = this.needToEvaluate;
                }
            }
        }

        return equals;
    }

    @Override
    public int hashCode() {
        return subset != null ? subset.hashCode() : 0;
    }

    public static PossLogicNode create(Subset subset, Theory theory, Set<Literal> entailedByDirectAncestor, Set<Literal> entailed, boolean needToEvaluate) {
        return new PossLogicNode(subset, theory, entailedByDirectAncestor, entailed, needToEvaluate);
    }

}
