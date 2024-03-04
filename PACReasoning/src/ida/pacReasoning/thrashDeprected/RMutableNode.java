package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;
import ida.utils.Sugar;

import java.util.Set;

/**
 * Created by martin.svatos on 20. 5. 2018.
 */
public class RMutableNode {
    // watch out, value forced of evaluation, so we do not need to keep map or similar process

    private final Subset constants;
    private Set<Literal> proved;
    private boolean evaluate;

    public RMutableNode(Subset constants, boolean evaluate, Set<Literal> proved) {
        this.constants = constants;
        this.evaluate = evaluate;
        this.proved = proved;
    }

    public Subset getConstants() {
        return constants;
    }

    public boolean isEvaluate() {
        return evaluate;
    }

    public void setEvaluate(boolean evaluate) {
        this.evaluate = evaluate;
    }

    public Set<Literal> getProved() {
        return proved;
    }

    public void setProved(Set<Literal> proved) {
        this.proved = proved;
    }

    @Override
    public boolean equals(Object o) {
        // watch out, value forced so we do not need to keep map or similar process
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RMutableNode that = (RMutableNode) o;

        boolean subsetsEqual = constants.equals(that.constants);
        if(subsetsEqual){
            if(isEvaluate() || that.isEvaluate()){
                this.setEvaluate(true);
                that.setEvaluate(true);
            }
            this.proved = Sugar.union(this.getProved(),that.getProved());
            that.setProved(this.proved);
        }

        return subsetsEqual;
    }

    @Override
    public int hashCode() {
        return constants.hashCode();
    }

    public static RMutableNode create(Subset constants, boolean evaluate, Set<Literal> proved){
        return new RMutableNode(constants,evaluate, proved);
    }
}
