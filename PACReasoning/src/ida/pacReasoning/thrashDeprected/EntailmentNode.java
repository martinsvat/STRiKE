package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.Literal;

import java.util.Set;

/**
 * Created by martin.svatos on 28. 4. 2018.
 */
public class EntailmentNode<T> {

    private final T constants;
    private final Set<Literal> queries;

    public EntailmentNode(T constants, Set<Literal> queries) {
        this.constants = constants;
        this.queries = queries;
    }

    public T getConstants() {
        return constants;
    }

    public Set<Literal> getQueries() {
        return queries;
    }

    public static <T> EntailmentNode<T> create(T constants, Set<Literal> queries){
        return new EntailmentNode(constants,queries);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EntailmentNode<?> that = (EntailmentNode<?>) o;

        if (constants != null ? !constants.equals(that.constants) : that.constants != null) return false;
        return queries != null ? queries.equals(that.queries) : that.queries == null;
    }

    @Override
    public int hashCode() {
        int result = constants != null ? constants.hashCode() : 0;
        result = 31 * result + (queries != null ? queries.hashCode() : 0);
        return result;
    }
}
