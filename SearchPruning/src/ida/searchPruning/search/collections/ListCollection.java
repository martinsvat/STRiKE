package ida.searchPruning.search.collections;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Admin on 03.05.2017.
 */
public class ListCollection<T> implements MyCollection<T> {
    private final List<T> list;

    public ListCollection() {
        this.list = new LinkedList<>(); // it would be horrible to use array list here !
    }

    @Override
    public void add(T t) {
        list.add(t);
    }

    @Override
    public T removeFirst() {
        return list.remove(0);
    }

    public int size(){
        return list.size();
    }

    public boolean isEmpty(){
        return list.isEmpty();
    }
}
