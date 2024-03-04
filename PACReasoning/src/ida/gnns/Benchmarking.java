package ida.gnns;

import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.SubsetFactory;
import ida.pacReasoning.supportEntailment.speedup.BSet;
import ida.pacReasoning.supportEntailment.speedup.FastSubsetFactory;
import ida.utils.Sugar;
import ida.utils.collections.Counters;
import ida.utils.tuples.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 11. 2. 2021.
 */
public class Benchmarking {


    private static int times = 10;

    private static Map<String, String> trueSolutions = new HashMap<>();

    public static void main(String[] args) {
        subsets();
//        dev();


    }

    private static void dev() {
        //{66}	{82}
        /*BSet set1 = FastSubsetFactory.getInstance().get(134, 66);
        BSet set2 = FastSubsetFactory.getInstance().get(134, 82);
        System.out.println("s1");
        System.out.println(set1.toString() + "\t" + set1.getMinimalValue());

        System.out.println("s2");
        System.out.println(set2.toString() + "\t" + set2.getMinimalValue());

        System.out.println(set1.isSubsetOf(set2));
        System.out.println(set2.isSubsetOf(set1));*/

        BSet set = new BSet(125, Sugar.list(111, 51, 97, 38, 16));
//        BSet set = new BSet(125, Sugar.list(111));
        System.out.println(set);
        System.out.println(set.cardinality());
        set.stream().forEach(i -> System.out.println("\t" + i));
    }

    private static void subsets() {
        // generating subsests
        int universum = 125;
        int k = 5;
        Random generator = new Random();
        int samples1 = 500;
        List<List<Integer>> forbidden = generate(samples1, universum, k, generator, 1);
        int samples2 = 1000000;
        List<List<Integer>> candidates = generate(samples2, universum, k, generator, 0);


        Counters<Integer> fCounter = new Counters<>();
        forbidden.forEach(l -> fCounter.increment(l.size()));
        Counters<Integer> cCounter = new Counters<>();
        candidates.forEach(l -> cCounter.increment(l.size()));

        System.out.println(fCounter);
        System.out.println(cCounter);

        System.out.println(k + "\t" + universum + "\t" + samples1 + "\t" + samples2 + "\t" + times);

        long start = System.nanoTime();
        long time = System.nanoTime();
        filterSubsets(candidates, forbidden, universum);
        System.out.println("\t" + (System.nanoTime() - time) / 1000000000.0);
        time = System.nanoTime();
        filterBSets(candidates, forbidden, universum);
        System.out.println("\t" + (System.nanoTime() - time) / 1000000000.0);
        time = System.nanoTime();
        filterBSetsFaster(candidates, forbidden, universum);
        System.out.println("\t" + (System.nanoTime() - time) / 1000000000.0);
        time = System.nanoTime();
        filterBSetsFaster2(candidates, forbidden, universum);
        System.out.println("\t" + (System.nanoTime() - time) / 1000000000.0);

        time = System.nanoTime() - start;
        System.out.println("total time\t\t" + time);

        int filteredOut = 0;
        time = System.nanoTime();
        List<Set<Integer>> refForbidden = forbidden.stream().map(l -> Sugar.setFromCollections(l)).collect(Collectors.toList());
        for (List<Integer> candidate : candidates) {
            Set<Integer> set = Sugar.setFromCollections(candidate);
            for (Set<Integer> banned : refForbidden) {
                if (set.containsAll(banned)) {
                    filteredOut++;
                    break;
                }
            }
        }
        System.out.println((System.nanoTime() - time) / 1000000000.0);
        System.out.println(filteredOut / (1.0 * candidates.size()) + "\t" + filteredOut);
    }

    private static void filterBSetsFaster2(List<List<Integer>> candidates, List<List<Integer>> forbidden, int constants) {
        System.out.println("faster BSets 2");
        long start = System.nanoTime();
        List<BSet> forb = convertToBSet(forbidden, constants);
        List<BSet> cand = convertToBSet(candidates, constants);
        Map<Integer, List<BSet>> map = new HashMap<>();
        forb.forEach(bset -> {
            int key = bset.getMinimalValue();
            if (null == map.get(key)) {
                map.put(key, Sugar.list());
            }
            map.get(key).add(bset);
        });

        long converting = System.nanoTime() - start;
        System.out.println("converting done");


        start = System.nanoTime();
        long filteredOut = 0;
        for (int iter = 0; iter < times; iter++) {
            for (BSet c : cand) {
                boolean goOn = true;
                int next = c.nextSetBit(0);
                int atMost = c.cardinality();
                while (-1 != next) {
                    List<BSet> possibleSubsets = map.get(next);
                    if (null != possibleSubsets) {
                        for (BSet possibleSubset : possibleSubsets) {
                            if (possibleSubset.cardinality() <= atMost && possibleSubset.isSubsetOf(c)) {
                                goOn = false;
                                filteredOut++;
                                break;
                            }
                        }
                        if (!goOn) {
                            break;
                        }
                    }

                    next = c.nextSetBit(next + 1);
                    atMost--;
                }

            }
        }
        System.out.println(filteredOut);
        long filtering = System.nanoTime() - start;
        System.out.println("converting time\t" + converting);
        System.out.println("filtering time\t" + filtering + "\t" + (filtering / 1000000000.0));
        System.out.println(filteredOut / (1.0 * candidates.size()) + "\t" + filteredOut);
    }


    private static void filterBSetsFaster(List<List<Integer>> candidates, List<List<Integer>> forbidden, int constants) {
        System.out.println("faster BSets");
        long start = System.nanoTime();
        List<BSet> forb = convertToBSet(forbidden, constants);
        List<BSet> cand = convertToBSet(candidates, constants);
        Map<Integer, List<BSet>> map = new HashMap<>();
        forb.forEach(bset -> {
            int key = bset.getMinimalValue();
            /*if (key != bset.stream().boxed().collect(Collectors.toList()).get(0)) {
                System.out.println(key);
                bset.stream().boxed().forEach(s -> System.out.print(" " + s));
                System.out.println();
                throw new IllegalStateException();
            }*/
            if (null == map.get(key)) {
                map.put(key, Sugar.list());
            }
            map.get(key).add(bset);
        });

        long converting = System.nanoTime() - start;
        System.out.println("converting done");


        start = System.nanoTime();
        long filteredOut = 0;
        for (int iter = 0; iter < times; iter++) {
            for (BSet c : cand) {
                boolean goOn = true;
                int next = c.nextSetBit(0);
                while (-1 != next) {
                    List<BSet> possibleSubsets = map.get(next);
                    if (null != possibleSubsets) {
                        for (BSet possibleSubset : possibleSubsets) {
                            if (possibleSubset.isSubsetOf(c)) {
                                goOn = false;
                                filteredOut++;
                                break;
                            }
                        }
                        if (!goOn) {
                            break;
                        }
                    }

                    next = c.nextSetBit(next + 1);
                }

            }
        }
        System.out.println(filteredOut);
        long filtering = System.nanoTime() - start;
        System.out.println("converting time\t" + converting);
        System.out.println("filtering time\t" + filtering + "\t" + (filtering / 1000000000.0));
        System.out.println(filteredOut / (1.0 * candidates.size()) + "\t" + filteredOut);
    }

    private static void filterBSets(List<List<Integer>> candidates, List<List<Integer>> forbidden, int constants) {
        System.out.println("BSets");

        //udelat tady na ferovku pair s puvodnimi a oproti tomu to kontrolovat jestli to vychazi stejne nebo ne :))
        long start = System.nanoTime();
        List<BSet> forb = convertToBSet(forbidden, constants);
        List<BSet> cand = convertToBSet(candidates, constants);
        System.out.println("converting done");

        long converting = System.nanoTime() - start;
        start = System.nanoTime();
        long filteredOut = 0;
        for (int iter = 0; iter < times; iter++) {
            for (BSet c : cand) {
//            for (Pair<Set<Integer>, BSet> c : cand) {
//                String cs = c.toString();
                for (BSet f : forb) {
//                for (Pair<Set<Integer>, BSet> f : forb) {
//                    boolean groundTruth = c.r.containsAll(f.getR());
//                    boolean result = f.getS().isSubsetOf(c.s);

//                    if (groundTruth != result) {
//                        System.out.println("c");
//                        System.out.println(c.getR() + "\t" + c.s);
//                        System.out.println("f");
//                        System.out.println(f.getR() + "\t" + f.s);
//                        FastSubsetFactory.getInstance().get(125,Sugar.list(111,51,97,38,16))
//                        System.exit(-1);
//                    }

                    if (f.isSubsetOf(c)) {

//                        String fs = f.toString();
//
//                        if (!trueSolutions.containsKey(cs)) {
//                            System.out.println("problem tady!");
//                            System.out.println(cs + "\t" + fs);
//                            System.out.println(trueSolutions.get(cs));
//                        }
//
//                        cs = null; // ze jsme skoncili drive

                        filteredOut++;
                        break;
                    }
                }

                /*if (null != cs) { dev debug
                    cs = c.toString();
                    if (trueSolutions.containsKey(cs)) {
                        System.out.println("problem 2");
                        System.out.println("" + cs + "\t" + trueSolutions.get(cs) + "\tnull");
                    }
                }*/
            }
        }
        System.out.println(filteredOut);
        long filtering = System.nanoTime() - start;
        System.out.println("converting time\t" + converting);
        System.out.println("filtering time\t" + filtering + "\t" + (filtering / 1000000000.0));
        System.out.println(filteredOut / (1.0 * cand.size()) + "\t" + filteredOut);
    }

    private static List<Pair<Set<Integer>, BSet>> convertToBSetDebug(List<List<Integer>> candidates, int constants) {
        FastSubsetFactory factory = FastSubsetFactory.getInstance();
        return candidates.stream()
                .map(set -> new Pair<>(Sugar.setFromCollections(set), factory.get(constants, set)))
                .collect(Collectors.toList());
    }

    private static List<BSet> convertToBSet(List<List<Integer>> candidates, int constants) {
        FastSubsetFactory factory = FastSubsetFactory.getInstance();
        return candidates.stream()
                .map(set -> factory.get(constants, set))
                .collect(Collectors.toList());
    }

    private static void filterSubsets(List<List<Integer>> candidates, List<List<Integer>> forbidden, int constants) {
        System.out.println("Subsets");
        long start = System.nanoTime();
        List<Subset> forb = convertToSubset(forbidden, constants);
        List<Subset> cand = convertToSubset(candidates, constants);
        System.out.println("converting done");

        long converting = System.nanoTime() - start;
        start = System.nanoTime();
        long filteredOut = 0;
        for (int iter = 0; iter < times; iter++) {
            for (Subset c : cand) {
                for (Subset f : forb) {
                    if (f.isSubsetOf(c)) {
                        trueSolutions.put(c.toString(), f.toString());// ukladani spravnych reseni
                        filteredOut++;
                        break;
                    }
                }
            }
        }
        System.out.println(filteredOut);
        long filtering = System.nanoTime() - start;
        System.out.println("converting time\t" + converting);
        System.out.println("filtering time\t" + filtering + "\t" + (filtering / 1000000000.0));
        System.out.println(filteredOut / (1.0 * candidates.size()) + "\t" + filteredOut);
    }

    private static List<Subset> convertToSubset(List<List<Integer>> candidates, int constants) {
        SubsetFactory factory = SubsetFactory.getInstance();
        return candidates.stream()
                .map(set -> factory.union(set.stream().map(val -> factory.get(constants, val)).collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    private static List<List<Integer>> generate(int samplesSize, int universum, int k, Random generator, int minimal) {
        List<List<Integer>> retVal = Sugar.list();
        for (int iter = 0; iter < samplesSize; iter++) {
            int size = generator.nextInt(k - minimal) + 1 + minimal;
            Set<Integer> current = Sugar.set();
            for (int i = 0; i < size; i++) {
                current.add(generator.nextInt(universum));
            }
            retVal.add(Sugar.listFromCollections(current));
        }
        return retVal;
    }
}
