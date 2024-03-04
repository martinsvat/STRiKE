package ida.pacReasoning.entailment;

import java.util.BitSet;
import java.util.List;
import java.util.stream.Collectors;
//import ida.pacReasoning.supportEntailment.BitSet;

/**
 * Created by martin.svatos on 3. 5. 2018.
 */
public class Subset implements Comparable<Subset> {
    private final BitSet subset;
    private final int hash;
    private final int size;
    private final String string;

    private final static int ADDRESS_BITS_PER_WORD = 6;
    private final static int BITS_PER_WORD = 1 << ADDRESS_BITS_PER_WORD;
    private List<Integer> vals;
    private Integer min;

    private Subset(BitSet bitset) {
        this.subset = bitset;
        this.hash = bitset.hashCode();
        this.size = bitset.cardinality();
        this.string = bitset.toString();

        this.vals = this.subset.stream().boxed().collect(Collectors.toList());
        this.min = this.vals.isEmpty() ? null : this.vals.get(0);
    }

    public BitSet getBitset() {
        return subset;
    }

    public int size() {
        return this.size;
    }

    public Subset union(Subset another) {
        BitSet copy = (BitSet) this.subset.clone();
        copy.or(another.subset);
        return SubsetFactory.getInstance().get(copy);
    }

    public boolean isSubsetOf(BitSet another) {
//        if (another.cardinality() < this.size()) {
//            return false;
//        }
        BitSet copy = (BitSet) this.subset.clone(); // this can be done more efficiently than by cloning ;) (i.e. iterate over values of another.subset)
        copy.and(another);
        return this.subset.cardinality() == copy.cardinality();

        // comming undone
        /*if(this.subset == another){
            return true;
        }
        */
    }

    public boolean isSubsetOf(Subset another) {
        if (another.size() < this.size()) {
            return false;
        }
        if (this == another) {
            return true;
        }
        BitSet copy = (BitSet) this.subset.clone(); // this can be done mor efficiently than by cloning ;) (i.e. iterate over values of another.subset)
        copy.and(another.subset);
        return this.subset.cardinality() == copy.cardinality();

        // speedup hack
        /*if(another.subset.size() != this.subset.size()){
            throw new IllegalStateException(); // you have to hack the implementation to get this running properly
        }
        for (int i = 0; i < this.wordsInUse; i++)
            words[i] &= set.words[i];
        */
    }

    public Subset minus(Subset another) {
        BitSet copy = (BitSet) this.subset.clone();
        copy.andNot(another.subset);
        return SubsetFactory.getInstance().get(copy);
    }


    public static Subset create(BitSet bitset) {
        return new Subset(bitset);
    }

    public static Subset create(int maxElements) {
        return new Subset(new BitSet(maxElements));
    }

    public static Subset create(int maxElements, int valPositive) {
        BitSet bitset = new BitSet(maxElements);
        bitset.set(valPositive);
        return new Subset(bitset);
    }

    public boolean isNonEmpty() {
        return this.subset.cardinality() > 0;
    }

    public boolean contains(Subset another) {
        BitSet copy = (BitSet) this.subset.clone();
        copy.and(another.subset);
        return copy.cardinality() > 0;
    }

    @Override
    public String toString() {
        return this.subset.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Subset subset1 = (Subset) o;

        return this.subset.equals(subset1.subset);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    //todo vyhodit
    public int debugSize() {
        return this.subset.size();
    }

    public static void main(String[] args) {
        System.out.println("thrash");
        /*
        BitSet s = new BitSet(10);
        s.set(0);
        BitSet s2 = new BitSet(10);
        s2.set(0);
        s.or(s2);
        System.out.println(s);
        System.out.println(s.size());
        */
        Subset s1 = Subset.create(10, 0);
        Subset s2 = Subset.create(10, 2);
        System.out.println(s1);
        System.out.println(s2);
        Subset s3 = s1.union(s2);
        System.out.println("----\n" + s1 + "\n" + s2 + "\n" + s3);
    }

    @Override
    public int compareTo(Subset o) {
        int lengths = Integer.compare(this.size, o.size);
        if (0 != lengths) {
            return lengths;
        }
        return string.compareTo(o.string);
    }

    public Integer getLowestValue() {
        // returns null if the subset is empty, otherwise returns the lowest number in the subset :))
        return min;
    }

    public List<Integer> values() {
        // returns sorted list of values contained in the subset
        return this.vals;
    }
}
