package ida.pacReasoning.entailment.nodes;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.theories.Theory;
import ida.utils.Sugar;

import java.util.Set;

/**
 * Created by martin.svatos on 9. 6. 2018.
 */
public class ClassicalLogicNode implements MutableNode {
    private final Subset constants;
    private final Theory theory;
    private Set<Literal> entailed;
    private boolean needToEvaluate;

    public ClassicalLogicNode(Subset subset, Theory theory, Set<Literal> entailed, boolean needToEvaluate) {
        this.constants = subset;
        this.theory = theory;
        this.entailed = entailed;
        this.needToEvaluate = needToEvaluate;
    }

    @Override
    public Subset getConstants() {
        return constants;
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
        return entailed;
    }

    @Override
    public boolean evaluate() {
        return this.needToEvaluate;
    }

    @Override
    public boolean checkConsistency() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassicalLogicNode that = (ClassicalLogicNode) o;

        boolean equals = constants.equals(that.constants);

        if (equals) { // that's the statefullness :))
            synchronized (this){
                synchronized (that){
                    Set<Literal> union = Sugar.union(this.entailed, that.entailed);
                    this.entailed = union;
                    that.entailed = union;
                }
            }
        }

        return equals;
    }

    @Override
    public int hashCode() {
        return constants.hashCode();
    }

    public static ClassicalLogicNode create(Subset subset, Theory theory, Set<Literal> entailedByDirectAncestor, Set<Literal> entailed, boolean needToEvaluate) {
        return new ClassicalLogicNode(subset, theory, entailed, needToEvaluate);
    }
}
