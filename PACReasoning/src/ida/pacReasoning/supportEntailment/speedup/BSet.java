package ida.pacReasoning.supportEntailment.speedup;

import ida.utils.Sugar;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * Own implementation, hopefully faster than java.util.BitSet.
 * <p>
 * Should be something both of former Subset and java.util.BitSet
 * <p>
 * <p>
 * It is assumed that one uses BSets of the same size during one run!
 * <p>
 * Created by martin.svatos on 10. 2. 2021.
 */
public class BSet {

    private final int maxElements;
    private final long[] bitset;
    private int cardinality;
    private int hash;
    private int minimalValue;

    public BSet(int maxElements, long[] bitset) {
        this.maxElements = maxElements;
        this.bitset = bitset;
        this.minimalValue = -1;
        this.hash = -1;
    }

    public BSet(int maxElements, List<Integer> distinctValues) {
        this.maxElements = maxElements;
        this.bitset = new long[(maxElements + 64) / 64];
        for (Integer value : distinctValues) {
            this.set(value);
        }
        this.hash = -1;
        this.minimalValue = -1;
    }

    private void set(Integer value) {
        int chunk = getChunk(value);
        this.bitset[chunk] |= 1L << (value - chunk * 64);
    }

    private int getChunk(Integer value) {
        return value / 64;
    }

    // taken from java.util.BitSet
    public Integer cardinality() {
        if (-1 == hash) {
            int sum = 0;
            for (long word : bitset) {
                sum += Long.bitCount(word);
            }
            this.cardinality = sum;
        }
        return this.cardinality;
    }

    // taken from java.util.BitSet
    @Override
    public int hashCode() {
        if (-1 == hash) {
            this.cardinality();
            long h = 1234;
            for (int idx = bitset.length; --idx >= 0; ) {
                h ^= bitset[idx] * (idx + 1);
            }
            this.hash = (int) ((h >> 32) ^ h);
        }
        return this.hash;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BSet))
            return false;
        if (this == obj)
            return true;

        BSet other = (BSet) obj;

        for (int idx = 0; idx < bitset.length; idx++) {
            if (this.bitset[idx] != other.bitset[idx]) {
                return false;
            }
        }
        return true;
    }


    public boolean isSubsetOf(BSet another) {
        for (int idx = 0; idx < this.bitset.length; idx++) {
            if (0 != ~((~this.bitset[idx]) ^ (this.bitset[idx] & another.bitset[idx]))) {
                return false;
            }
        }
        return true;
    }

    public Integer getMaxPossibleCardinality() {
        return maxElements;
    }


    /**
     * This operation statefully changes this object and returns it.
     *
     * @param other
     * @return
     */
    public BSet statefulOr(BSet other) {
        for (int idx = 0; idx < this.bitset.length; idx++) {
            this.bitset[idx] |= other.bitset[idx];
        }
        this.cardinality = -1;
        this.hash = -1;
        this.minimalValue = -1;
        return this;
    }

    /**
     * Returns a new union of these two bitsets. Assuming both have same size! (This is not checked.)
     *
     * @param another
     * @return
     */
    public BSet union(BSet another) {
        //long[] union = new long[(maxElements + 64) / 64];
        long[] union = new long[bitset.length];
        for (int idx = 0; idx < bitset.length; idx++) {
            union[idx] = this.bitset[idx] | another.bitset[idx];
        }
        return new BSet(this.maxElements, union);
    }

    /**
     * Returns -2 if the set is empty.
     *
     * @return
     */
    public int getMinimalValue() {
        if (-1 == this.minimalValue) {
            for (int idx = 0; idx < this.bitset.length; idx++) {
                if (0 != this.bitset[idx]) {
                    this.minimalValue = 64 * idx + Long.numberOfTrailingZeros(this.bitset[idx]);
                    break;
                }
            }
        }
        return this.minimalValue;
    }

    // taken from java.util.BitSet
    public int nextSetBit(int fromIndex) {
        if (fromIndex < 0) {
            throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
        }

        int u = fromIndex >> 6;
        if (u >= this.bitset.length) {
            return -1;
        }

        long word = this.bitset[u] & (0xffffffffffffffffL << fromIndex);

        while (true) {
            if (word != 0) {
                return (u * 64) + Long.numberOfTrailingZeros(word);
            }
            if (++u == this.bitset.length) {
                return -1;
            }
            word = this.bitset[u];
        }
    }

    // TODO tohle případně předělat při procházení, aby se nedělal ten drahý stream atd..
    // taken from java.util.BitSet
    public IntStream stream() {
        class BitSetIterator implements PrimitiveIterator.OfInt {
            int next = nextSetBit(0);

            @Override
            public boolean hasNext() {
                return next != -1;
            }

            @Override
            public int nextInt() {
                if (next != -1) {
                    int ret = next;
                    next = nextSetBit(next + 1);
                    return ret;
                } else {
                    throw new NoSuchElementException();
                }
            }
        }

        return StreamSupport.intStream(
                () -> Spliterators.spliterator(
                        new BitSetIterator(), cardinality(),
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED),
                Spliterator.SIZED | Spliterator.SUBSIZED |
                        Spliterator.ORDERED | Spliterator.DISTINCT | Spliterator.SORTED,
                false);
    }


    public String toString() {
        return "{" + this.stream().mapToObj(i -> "" + i).collect(Collectors.joining(", ")) + "}";
    }

    public static BSet get(Integer maxElements) {
        return get(maxElements, Sugar.list());
    }

    public static BSet get(int maxElements, List<Integer> list) {
        return new BSet(maxElements, list);
    }
}
