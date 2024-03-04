package logicStuff.learning.constraints.shortConstraintLearner;

import ida.ilp.logic.Clause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.utils.Sugar;

import java.util.Set;


/**
 * Wrapper for refinement, which is hashed according to ICW of the saturated. (Similar structure to SearchNodeInfo, unify one day.)
 * <p>
 * Created by martin.svatos on 29. 10. 2017.
 */
public class Refinement {

    private final Clause saturated;
    private final Clause nonSaturated;
    private Refinement parent;
    private Set<Literal> addedLiterals;
    private IsoClauseWrapper saturatedICW;
    private IsoClauseWrapper nonsaturatedICW;
    private final boolean isSaturationBigger;
    //private final Set<Literal> newLiterals;
    private Set<Literal> badRefinements; // this is statefull, so watch out


    public Refinement(Clause saturated, Clause nonSaturated, Refinement parent, Set<Literal> addedLiterals, Set<Literal> badRefinements) {
        this.saturated = saturated;
        this.nonSaturated = nonSaturated;
        this.isSaturationBigger = saturated.literals().size() != nonSaturated.literals().size();
        //this.newLiterals = newLiterals;
        this.badRefinements = badRefinements;
        this.parent = parent;
        this.addedLiterals = addedLiterals;
    }

    public Clause getNonSaturated() {
        return nonSaturated;
    }

    /**
     * True iff saturation is has more literals than the non-saturated original clause.
     *
     * @return
     */
    public boolean isSaturationBigger() {
        return isSaturationBigger;
    }

    public Clause getSaturated() {
        return saturated;
    }

    public IsoClauseWrapper getSaturatedICW() {
        if (null == this.saturatedICW) {
            this.saturatedICW = IsoClauseWrapper.create(this.saturated);
        }
        return saturatedICW;
    }

    public IsoClauseWrapper getNonSaturatedICW() {
        if (null == this.nonsaturatedICW) {
            this.nonsaturatedICW = IsoClauseWrapper.create(this.nonSaturated);
        }
        return nonsaturatedICW;
    }


    // call this method if there were no bad refinements in the layer to speed up the process
    public void adjustEmptyBadRefinements() {
        if (null != parent && parent.badRefinements.isEmpty()) {
            this.parent = this.parent.parent;
        }
    }

    public Set<Literal> addedLiterals() {
        return this.addedLiterals;
    }

    public boolean isForbidden(Literal literal) {
        if (this.badRefinements.size() > 0) {
            if (this.badRefinements.contains(literal)) {
                return true;
            }
        }
        return null != parent && parent.isForbidden(literal);
    }

//    public Set<Pair<String,Integer>> getNewlyAddedPredicates(){
//        return this.newLiterals.stream().map(Literal::getPredicate).collect(Collectors.toSet());
//    }
//
//    public Set<Literal> getNewlyAddedLiterals(){
//        return this.newLiterals;
//    }

    //public Counters<IsoClauseWrapper> addedLiteralsStats(){
    //    return new Counters(this.newLiterals.stream().map(literal -> IsoClauseWrapper.create(new Clause(literal))).collect(Collectors.toList()));
    //}

    // this may return null if bad refinements are emtpy!
        /*public Set<Literal> getBadRefinements() {
        return badRefinements;
    }*/

    //public void setBadRefinements(Set<Literal> badRefinements) {
    //  this.badRefinements = badRefinements;
    //}

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Refinement that = (Refinement) o;

        return saturatedICW.equals(that.saturatedICW);
    }

    @Override
    public int hashCode() {
        return getSaturatedICW().hashCode();
    }

    public static Refinement create(Clause saturated, Clause nonSaturated) {
        return create(saturated, nonSaturated, null);
    }

    public static Refinement create(Clause saturated, Clause nonSaturated, Set<Literal> badRefinements) {
        //return new Refinement(saturated, nonSaturated, badRefinements);
        return create(saturated, nonSaturated, null, Sugar.set(), badRefinements);
    }


    public static Refinement create(Clause saturated, Clause nonSaturated, Refinement parent, Set<Literal> addedLiterals, Set<Literal> finalBadLiterals) {
        return new Refinement(saturated, nonSaturated, parent, addedLiterals, finalBadLiterals);
    }

    public void gc() {
        this.addedLiterals = null;
    }
}
