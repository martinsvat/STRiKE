package ida.searchPruning.search.collections;

import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.function.Function;

/**
 * Created by Admin on 03.05.2017.
 */
public class QueueCollection<T> implements MyCollection<T> {

    private final PriorityQueue<T> queue;

    public QueueCollection() {
        this.queue = new PriorityQueue<T>();
    }

    public QueueCollection(Comparator<T> comparator) {
        this.queue = new PriorityQueue<T>(comparator);
    }

    @Override
    public void add(T t) {
        this.queue.add(t);
    }

    @Override
    public T removeFirst() {
        return this.queue.poll();
    }

    public int size(){
        return queue.size();
    }

    public boolean isEmpty(){
        return queue.isEmpty();
    }



    public static void main(String[] args){
        t1();
    }

    private static void t1() {
        Function<String,Double> t = (x) -> Double.valueOf(x);
        QueueCollection<String> q = new QueueCollection<String>(Comparator.comparing(t).reversed());

        q.add("10");
        q.add("0");
        q.add("11");

        while(!q.isEmpty()){
            String head = q.removeFirst();
            System.out.println(head);
        }
    }
}
