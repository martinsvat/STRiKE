package ida.pacReasoning.supportEntailment.speedup;

import ida.ilp.logic.Literal;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Support just with BSet instead of Subset (and java.util.Bitset).
 *
 * Created by martin.svatos on 15. 10. 2018.
 */
public class BSupport {
    //    private final long iteration;
    private final BSet set;
    private final Set<Literal> literals; // literals the set entails
    private final int hashcode;

    public BSupport(Set<Literal> literals, BSet set) {
        this.literals = literals;
        this.set = set;
        this.hashcode = this.set.hashCode();
    }

    public BSupport(BSet set) {//, long iteration) {
        this.hashcode = set.hashCode();
        this.set = set;
        this.literals = ConcurrentHashMap.newKeySet();
    }

    public BSet getSet() {
        return set;
    }


    public Set<Literal> getLiterals() {
        return literals;
    }

    public void addLiteral(Literal literal) {
        this.literals.add(literal);
    }

    // it is not used in SupportsHolderOpt5
    /*public BSupport merge(BSupport another) {
        return get(support.set.union(this.set));
    }*/

    public static BSupport create(BSet set) {
        return new BSupport(set);
    }


    // cardinality of the support set (not the head it entails)
    public int size() {
        return this.set.cardinality();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BSupport support = (BSupport) o;

        return set != null ? set.equals(support.set) : support.set == null;
    }

    @Override
    public int hashCode() {
        return this.hashcode;
    }

    @Override
    public String toString() {
        return "Support{" +
//                "iteration=" + iteration +
                ", set=" + set +
                ", literals=" + literals +
                '}';
    }


    public void setLiterals(Set<Literal> literals) {
        this.literals.clear();
        this.literals.addAll(literals);
    }

    public void removeHead(Literal head) {
        this.literals.remove(head);
    }

    public boolean isSubsetOf(BSet another) {
        return this.set.isSubsetOf(another);
    }
}
