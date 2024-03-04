package ida.searchPruning.search.collections;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * not thread-safe
 * <p>
 * Created by Admin on 03.05.2017.
 */
public class CollectionWithMemory<T extends SearchNodeInfo> {

    private MyCollection<T> collection;
    private final int beamWidth;
    private final Map<T, T> inside;
    private final boolean chceckDuplicity;

    public CollectionWithMemory(MyCollection<T> collection, int beamWidth, boolean checkDuplicity) {
        this.collection = collection;
        this.beamWidth = beamWidth;
        this.inside = new HashMap<>();
        this.chceckDuplicity = checkDuplicity;
    }

    public CollectionWithMemory(MyCollection<T> collection) {
        this(collection, -1, true);
    }

    public CollectionWithMemory(MyCollection<T> collection, boolean checkDuplicity) {
        this(collection, -1, checkDuplicity);
    }

    /**
     * Adds t into the collection.
     *
     * @param t
     */
    public void push(T t) {
        /*if(t instanceof SearchNodeInfo){
            SearchNodeInfo info = (SearchNodeInfo) t;
            if(Double.isInfinite(info.getAccuracy())){
                return;
            }
        }*/
        if (!this.chceckDuplicity || !inside.containsKey(t)) {
            forcePush(t);
        //} else {
            //T node = inside.get(t);
            //node.update(t); // tenhle update by fungoval kdyby strategie byla takova ze nejdrive vygeneruju vsechny refinement cele vrstvy, pote udelam pruniky pokryti a pote pocitam support; ale to by potrebovalo
            // spoustu pameti mit vsechny refinementy vrstvy najednou v pameti
            // navic strategie je jina -- udelej refinementy jednoho rodice, spocitej jejich support a pokracuj dal
            // update nezmeni razeni v collection (nebylo to tak navrzeno)
        }
    }

    public void forcePush(T t) {
        if (!chceckDuplicity) {
            inside.put(t, t);
        }
        collection.add(t);
    }

    public void checkSize() {
        if (beamWidth > 0 && collection.size() > beamWidth) {
            // hardcoded for queue only
            QueueCollection<T> trimmed = new QueueCollection<T>();
            for (int idx = 0; idx < beamWidth; idx++) {
                trimmed.add(collection.removeFirst());
            }
            this.collection = trimmed;
        }
    }

    public boolean hasContained(T t) {
        return inside.containsKey(t);
    }

    public T poll() {
        return collection.removeFirst();
    }

    public boolean isNotEmpty() {
        return !collection.isEmpty();
    }

    public Set<T> getMemory() {
        return inside.keySet();
    }

    public void addMemory(Set<T> outerMemory) {
        // assuming this is done only at the beginning
        outerMemory.forEach(t -> inside.put(t, t));
    }

    public int size() {
        return this.collection.size();
    }


    public static void main(String[] args) {
        CollectionWithMemory<SearchNodeInfo> queue = new CollectionWithMemory<>(new QueueCollection<SearchNodeInfo>());
        Coverage empty = CoverageFactory.getInstance().take();
        queue.push(SearchNodeInfo.create(new HornClause(Clause.parse("q(Y),p(X)")), null, empty, empty, 1.0, false));
        queue.push(SearchNodeInfo.create(new HornClause(Clause.parse("p(X,Y)")), null, empty, empty, 0.0, true));
        queue.push(SearchNodeInfo.create(new HornClause(Clause.parse("q(X,Y)")), null, empty, empty, 0.0, false));
        queue.push(SearchNodeInfo.create(new HornClause(Clause.parse("p(X)")), null, empty, empty, 1.0, true));

        while (queue.isNotEmpty()) {
            System.out.println(queue.size() + " " + queue.poll());
        }
    }
}
