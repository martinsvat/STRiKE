package logicStuff.learning.datasets;

import java.util.Arrays;
import java.util.Collection;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 22. 2. 2018.
 */
public class CoverageFactory {
    private static CoverageFactory ourInstance = new CoverageFactory();
    private final Cache<Coverage> cache;
    private final boolean useCache;

    public static CoverageFactory getInstance() {
        return ourInstance;
    }

    private CoverageFactory() {
        this.cache = new Cache<>();
        this.useCache = Boolean.parseBoolean(System.getProperty("ida.logicStuff.dataset.cache","true"));
    }

    public Coverage get(Collection<Integer> coverage){
        return get(Coverage.create(coverage));
    }

    public Coverage get(Coverage coverage){
        if(useCache){
            return cache.get(coverage);
        }
        return coverage;
    }

    public Coverage get(IntStream intStream) {
        return get(Coverage.create(intStream));
    }

    public Coverage get(int size) {
        return get(0,size);
    }

    private Coverage get(int start, int size) {
        return get(IntStream.range(start,start+size));
    }

    public Coverage take(int... idxs) {
        return get(Arrays.stream(idxs));
    }
}
