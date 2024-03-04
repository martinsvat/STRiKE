package logicStuff.typing;

import ida.ilp.logic.Predicate;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;

import java.util.Set;

/**
 * Just a placeholder for unification of types.
 * <p>
 * Created by martin.svatos on 26. 10. 2018.
 */
public class Type {

    private final int id;
    private final Set<Pair<Predicate, Integer>> positions;
    private Predicate predicate;

    public Type(int id, Set<Pair<Predicate, Integer>> positions) {
        this.id = id;
        this.positions = positions;
        this.predicate = Predicate.create("_type_" + id, 1);
    }

    public Type(int id, Pair<Predicate, Integer> position) {
        this(id, Sugar.set(position));
    }


    public Type(int id, Predicate predicate, int argIdx) {
        this(id, new Pair<>(predicate, argIdx));
    }

    public int getId() {
        return id;
    }

    public Set<Pair<Predicate, Integer>> getPositions() {
        return positions;
    }

    public void add(Type type) {
        this.positions.addAll(type.getPositions());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Type type = (Type) o;

        return id == type.id;
    }

    @Override
    public int hashCode() {
        return id;
    }

    public Predicate predicate() {
        return this.predicate;
    }

    public String getStrId() {
        return "" + this.id;
    }
}
