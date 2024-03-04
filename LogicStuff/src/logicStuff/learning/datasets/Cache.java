package logicStuff.learning.datasets;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by martin.svatos on 22. 2. 2018.
 */
public class Cache<T> {

    private final Map<T, T> map;

    public Cache() {
        this.map = new ConcurrentHashMap<>();
    }

    public T get(T t) {
        synchronized (map) {
            /*if (!map.containsKey(t)) {
                map.put(t, t);
            }
            return map.get(t);*/
            T val = map.get(t);
            if (null == val) {
                map.put(t, t);
            }
            return t;
        }
    }

}
