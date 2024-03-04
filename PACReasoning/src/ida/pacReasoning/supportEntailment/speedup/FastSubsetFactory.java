package ida.pacReasoning.supportEntailment.speedup;


import ida.utils.Sugar;
import logicStuff.learning.datasets.Cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Should be faster than using standard java.util.BitSet
 * <p>
 * Created by martin.svatos on 3. 5. 2018.
 */
public class FastSubsetFactory {
    private static FastSubsetFactory ourInstance = new FastSubsetFactory();
    private final Map<Integer, Cache<BSet>> cache;
    private final boolean useCache;

    public static FastSubsetFactory getInstance() {
        return ourInstance;
    }

    private FastSubsetFactory() {
        this.cache = new HashMap<>();
        // TODO change this in near future
        this.useCache = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.subset.cache", "true"));
//        this.useCache = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.subset.cache", "false"));
        //System.out.println("cache is\t" + this.useCache);
        //System.out.println("is subset cache used?\t" + this.useCache);
    }

    // no longer available since BSet is merge of Subset and BitSet
    /*public Subset get(BSet set) {
        return get(Subset.get(bitset));
    }*/

    public BSet get(BSet set) {
        if (useCache) {
            synchronized (cache) {
                Cache<BSet> val = cache.get(set.cardinality());
                if (null == val) {
                    val = new Cache<>();
                    cache.put(set.cardinality(), val);
                }
                return val.get(set);
            }
        }
        return set;
    }

    public BSet get(int maxElements, int valPositive) {
//        System.out.println(maxElements + "\t" + valPositive);
        return get(BSet.get(maxElements, Sugar.list(valPositive)));
    }

    public BSet get(int maxElements) {
        return get(BSet.get(maxElements, Sugar.list()));
    }

    public BSet get(int maxElements, List<Integer> positiveValues) {
        return get(BSet.get(maxElements, positiveValues));
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
    public BSet union(Collection<BSet> sets) {
        if (0 == sets.size()) {
            throw new IllegalArgumentException();
        }
        BSet retVal = null;
        for (BSet set : sets) {
            if (null == retVal) {
                retVal = BSet.get(set.getMaxPossibleCardinality());
            } else {
                retVal.statefulOr(set);
            }
        }
        return get(retVal);
    }
}
