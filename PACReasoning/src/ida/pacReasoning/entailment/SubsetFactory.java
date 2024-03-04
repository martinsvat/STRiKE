package ida.pacReasoning.entailment;

//import ida.pacReasoning.supportEntailment.BitSet;
import ida.utils.Sugar;
import logicStuff.learning.datasets.Cache;

import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by martin.svatos on 3. 5. 2018.
 */
public class SubsetFactory {
    private static SubsetFactory ourInstance = new SubsetFactory();
    private final Map<Integer, Cache<Subset>> cache;
    private final boolean useCache;

    public static SubsetFactory getInstance() {
        return ourInstance;
    }

    private SubsetFactory() {
        this.cache = new HashMap<>();
        this.useCache = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.subset.cache", "true"));
//        this.useCache = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.subset.cache", "false"));
        //System.out.println("cache is\t" + this.useCache);
        //System.out.println("is subset cache used?\t" + this.useCache);
    }

    public Subset get(BitSet bitset) {
        return get(Subset.create(bitset));
    }

    public Subset get(Subset subset) {
        if (useCache) {
            synchronized (cache) {
                /*if (!cache.containsKey(subset.size())) {
                    cache.put(subset.size(), new Cache<>());
                }
                return cache.get(subset.size()).get(subset);*/
                Cache<Subset> val = cache.get(subset.size());
                if (null == val) {
                    val = new Cache<>();
                    cache.put(subset.size(), val);
                }
                return val.get(subset);
            }
        }
        return subset;
    }

    public Subset get(int maxElements, int valPositive) {
        return get(Subset.create(maxElements, valPositive));
    }

    public Subset get(int maxElements) {
        return get(Subset.create(maxElements));
    }

    public void clear(int size) {
        synchronized (cache) {
            cache.remove(size);
        }
    }

    public void clear() {
        synchronized (cache) {
            for (Integer key : Sugar.setFromCollections(cache.keySet())) {
                cache.remove(key);
            }
        }
    }

    // should be little bit faster than of the hand ;)
    public Subset union(Collection<Subset> subsets) {
        BitSet retVal = null;
        for (Subset subset : subsets) {
            if (null == retVal) {
                retVal = (BitSet) subset.getBitset().clone();
            } else {
                retVal.or(subset.getBitset());
            }
        }
        return get(retVal);
    }
}
