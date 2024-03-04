package ida.pacReasoning.supportEntailment;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by martin.svatos on 15. 10. 2018.
 */
public class Support {
    //    private final long iteration;
    private final Subset subset;
    private final Set<Literal> literals; // literals the subset entails
    private final int hashcode;

    public Support(Set<Literal> literals, Subset subset) {
        this.literals = literals;
        this.subset = subset;
        this.hashcode = subset.hashCode();
    }

    public Support(Subset subset) {//, long iteration) {
        this.hashcode = subset.hashCode();
        this.subset = subset;
//        this.iteration = iteration;
        this.literals = ConcurrentHashMap.newKeySet();
    }

    public Subset getSubset() {
        return subset;
    }


//    public long getIteration() {
//        return iteration;
//    }

    public Set<Literal> getLiterals() {
        return literals;
    }

    public void addLiteral(Literal literal) {
        this.literals.add(literal);
    }

    public Support merge(Support support) {
        return create(support.subset.union(this.subset));
    }

//    public Support merge(Support support, long iteration) {
//        return get(support.subset.union(this.subset), iteration);
//    }

//    public static Support get(Subset subset, long iteration) {
//        return new Support(subset, iteration);
//    }

    public static Support create(Subset subset) {
        return new Support(subset);
    }

    public int size() {
        return this.subset.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Support support = (Support) o;

        return subset != null ? subset.equals(support.subset) : support.subset == null;
    }

    @Override
    public int hashCode() {
        return this.hashcode;
    }

    @Override
    public String toString() {
        return "Support{" +
//                "iteration=" + iteration +
                ", subset=" + subset +
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
}
