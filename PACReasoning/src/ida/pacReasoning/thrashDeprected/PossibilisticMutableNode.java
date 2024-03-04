package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;
import ida.utils.Sugar;

import java.util.Set;

/**
 * Created by martin.svatos on 27. 5. 2018.
 */
public class PossibilisticMutableNode {

    private final Subset constants;

    private boolean wasTheoryTrimmed;
    private boolean evaluate;
    private Set<Literal> impliedLiterals;
    private PossibilisticTheory theory;

    // stateful node which changes its content due to hash and equals
    public PossibilisticMutableNode(Subset subset, boolean evaluate, Set<Literal> impliedLiteral, PossibilisticTheory theory) {
        this.constants = subset;
        this.evaluate = evaluate;
        this.impliedLiterals = impliedLiteral;
        this.theory = theory;
        this.wasTheoryTrimmed = false;
    }

    public static PossibilisticMutableNode create(Subset subset, boolean evaluate, Set<Literal> impliedLiteral, PossibilisticTheory theory) {
        return new PossibilisticMutableNode(subset, evaluate, impliedLiteral, theory);
    }


    public boolean isEvaluate() {
        return evaluate;
    }

    public PossibilisticTheory getTheory() {
        return theory;
    }

    public Set<Literal> getImpliedLiterals() {
        return impliedLiterals;
    }

    public boolean wasTheoryTrimmed() {
        return wasTheoryTrimmed;
    }

    public Subset getConstants() {
        return this.constants;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PossibilisticMutableNode that = (PossibilisticMutableNode) o;
        boolean subsetEquals = constants.equals(that.constants);


        // wasTheoryTrimmed vyjadruje jestli vsechny kandidati maji stejne dlouhou teorii
        // impliedLiterals -- vsechny literaly ktere byly dokaz pomoci dane teorie na dane masce
        // asi jsou nektere prommene tady redundanti
        if (subsetEquals) {
            if (this.getTheory().size() != that.getTheory().size()) {
                this.setWasTheoryTrimmed(true);
                that.setWasTheoryTrimmed(true);
                if (this.getTheory().size() > that.getTheory().size()) {
                    this.setTheory(that.getTheory());
                } else {
                    that.setTheory(this.getTheory());
                }
            }

            if (this.wasTheoryTrimmed() || that.wasTheoryTrimmed()) {
                // pokud nejaka teorie jinak dlouha nez jine, tak stejnak musime prepocitat dokazane (nebo si to pamatovat jinak nez doted)
                this.impliedLiterals = Sugar.set();
                that.setImpliedLiterals(this.impliedLiterals);

            }
            if (isEvaluate() || that.isEvaluate()) {
                this.setEvaluate(true);
                that.setEvaluate(true);
                this.impliedLiterals = Sugar.union(this.getImpliedLiterals(), that.getImpliedLiterals());
                that.setImpliedLiterals(this.getImpliedLiterals());
            }
        }

        return subsetEquals;
    }

    @Override
    public int hashCode() {
        return constants.hashCode();
    }

    public void setEvaluate(boolean evaluate) {
        this.evaluate = evaluate;
    }

    public void setImpliedLiterals(Set<Literal> impliedLiterals) {
        this.impliedLiterals = impliedLiterals;
    }

    public void setWasTheoryTrimmed(boolean wasTheoryTrimmed) {
        this.wasTheoryTrimmed = wasTheoryTrimmed;
    }

    public void setTheory(PossibilisticTheory theory) {
        this.theory = theory;
    }
}
