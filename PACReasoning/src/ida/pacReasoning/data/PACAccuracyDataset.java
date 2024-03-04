package ida.pacReasoning.data;

import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.subsumption.Matching;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Combinatorics;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;


import java.math.BigDecimal;
import java.math.MathContext;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Just a quick workaround to have a dataset which compute accuracy defined in Relationa Marginals by Ondra. It is kind
 * of different from the standard way of computing acc here, but we need an object that can be
 * <p>
 * Created by martin.svatos on 15. 8. 2018.
 */
public class PACAccuracyDataset implements DatasetInterface {

    private final Set<Literal> evidence;
    private final Matching matching;
    private final int k;
    private final List<Constant> constants;
    private final Map<Constant, Integer> constantToIdx;
    private final BigDecimal allPossibleSubsets;
    private final MathContext precision = MathContext.DECIMAL128;
    private final int maxGroundSubstitutions;

    private PACAccuracyDataset(Set<Literal> evidence, int k, int maxGroundSubstitutions) {
        this.evidence = evidence;
        this.k = k;
        List<Constant> constants = Sugar.listFromCollections(LogicUtils.constants(evidence));
        Collections.sort(constants, Comparator.comparing(Constant::name));
        this.constants = constants;
        this.matching = Matching.create(new Clause(evidence), Matching.THETA_SUBSUMPTION); // hardcoded, parametrize if needed
        Map<Constant, Integer> map = IntStream.range(0, constants.size()).boxed().collect(Collectors.toMap(constants::get, java.util.function.Function.identity()));
        this.constantToIdx = map;
        this.allPossibleSubsets = Combinatorics.binomialBig(constants.size(), k, precision);
        this.maxGroundSubstitutions = maxGroundSubstitutions;
    }


    @Override
    public int numExistentialMatches(HornClause hc, int maxNum) {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public int numExistentialMatches(HornClause hc, int maxNum, Coverage checkOnly) {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public Set<Pair<String, Integer>> queryPredicates() {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public Set<Pair<String, Integer>> allPredicates() {
        return Utils.create().predicates(evidence).stream().map(Predicate::getPair).collect(Collectors.toSet());
    }

    @Override
    public Pair<Coverage, Coverage> classify(HornClause hc, Coverage learnFrom) {
        return new Pair<>(CoverageFactory.getInstance().take(0, 1)
                , CoverageFactory.getInstance().take(0, 1));
    }

    // returns #pos and #all subsets
    public Pair<BigDecimal, BigDecimal> classifySize(HornClause hc) {
        Set<Set<Constant>> violated = findViolatedSubsets(hc);
        System.out.println("violated & constants\t" + violated.size() + "\t" + constants.size());
        BigDecimal violatedKSize = violatedKsizeSet(violated);
//        int violatedKSizeBrute = violatedKsizeSetBrute(violated);
//        System.out.println("violated\t" + violatedKSize + "\t" + violatedKSizeBrute);
//                System.out.println("violated\t" + violatedKSize  + "\t" + violated.size() + "\t" + allPossibleSubsets);
//        System.exit(-1);
        return new Pair<>(allPossibleSubsets.subtract(violatedKSize)
                , allPossibleSubsets);
    }

    public double accuracy(HornClause horn) {
        Pair<BigDecimal, BigDecimal> classification = classifySize(horn);
        BigDecimal pos = classification.getR();
        BigDecimal all = classification.getS();
        return pos.divide(all, precision).doubleValue();
    }

    public double accuracy(Clause clause) {
        return accuracy(HornClause.create(clause));
    }

    public double accuracyApprox(Clause clause) {
        return accuracyApprox(HornClause.create(clause));
    }


    public double accuracyApprox(HornClause horn) {
        Pair<BigDecimal, BigDecimal> classification = classifySizeApprox(horn);
        BigDecimal pos = classification.getR();
        BigDecimal all = classification.getS();
        return pos.divide(all, precision).doubleValue();
    }

    public Pair<BigDecimal, BigDecimal> classifySizeApprox(HornClause hc) {
        Set<Set<Constant>> violated = findViolatedSubsets(hc);
//        System.out.println("violated & constants\t" + violated.size() + "\t" + constants.size());
        BigDecimal violatedKSize = violatedKsizeSetApprox(violated);
//        BigDecimal prev = violatedKsizeSet(violated);
//        System.out.println(prev+ "\n" + violatedKSize);
//        int violatedKSizeBrute = violatedKsizeSetBrute(violated);
//        System.out.println("violated\t" + violatedKSize + "\t" + violatedKSizeBrute);
//                System.out.println("violated\t" + violatedKSize  + "\t" + violated.size() + "\t" + allPossibleSubsets);
//        System.exit(-1);
        return new Pair<>(allPossibleSubsets.subtract(violatedKSize)
                , allPossibleSubsets);
    }

    private BigDecimal violatedKsizeSetApprox(Set<Set<Constant>> violatedSets) {
        Map<Constant, Integer> ordering = new HashMap<>();
        List<Set<Constant>> list = Sugar.listFromCollections(violatedSets);
        Collections.sort(list, Comparator.comparing(Set::size));

        for (int idx = list.size() - 1; idx >= 0; idx--) {
            Set<Constant> current = list.get(idx);
            for (int below = 0; below < idx; below++) {
                if (list.get(below).containsAll(current)) {
                    list.remove(idx);
                    break;
                }
            }
        }

        list.stream().sorted(Comparator.comparingInt(Set::size))
                .forEach(set -> set.stream().forEach(constant -> {
                    if (!ordering.containsKey(constant)) {
                        ordering.put(constant, ordering.size());
                    }
                }));

        Comparator<? super Constant> innerComparator = Comparator.comparingInt(ordering::get);
        List<List<Constant>> violated = list.stream()
                .map(set -> {
                    List<Constant> l = Sugar.listFromCollections(set);
                    Collections.sort(l, innerComparator);
                    return l;
                }).collect(Collectors.toList());

        Comparator<? super List<Constant>> comparator = (l1, l2) -> {
            int sizeCompare = Integer.compare(l1.size(), l2.size());
            if (0 != sizeCompare) {
                return sizeCompare;
            }
            int lowerCommon = Math.min(l1.size(), l2.size());
            for (int idx = 0; idx < lowerCommon; idx++) {
                int compare = Integer.compare(ordering.get(l1.get(idx)), ordering.get(l2.get(idx)));
                if (0 != compare) {
                    return compare;
                }
            }
            throw new IllegalStateException("it cannot reach this point ;)");
        };
        Collections.sort(violated, comparator);
        BigDecimal retVal = BigDecimal.ZERO;
        for (List<Constant> violating : violated) {
            int kPrime = k - violating.size(); // # of empty places behind current violating subset
            if (0 == kPrime) { // the violating subset is big as k, thus we increment the retval by one ;)
                retVal = retVal.add(BigDecimal.ONE);
            } else {
                // cannot do this, something would be skipped, e.g. (a,b) (d,e,f), then (a,d,e,f) would be skipped :(
                //int nPrime = constants.size() - (ordering.get(violating.get(violating.size()-1)) + 1); // # of empty constants above in the ordering; +1 is there because ordering is indexed from zero
                int nPrime = constants.size() - violating.size(); // # of empty constants above in the ordering; +1 is there because ordering is indexed from zero
                retVal = retVal.add(Combinatorics.binomialBig(nPrime, kPrime, precision));
            }
        }

        return retVal;
    }

    // returns number of size-k sets which are supersets of those given
    private BigDecimal violatedKsizeSet(Set<Set<Constant>> violatedSets) {
        // for sure, there is a possibility for some caching and hacks to speed it up, e.g. throwing out violated subsets which are skipped already, or it may not (we need the violated to be sorted by the order of constants we have and to remove only the first one

        BigDecimal retVal = BigDecimal.ZERO;

        int[] state = new int[k];
        for (int idx = 0; idx < k; idx++) {
            state[idx] = idx;
        }

        Map<Constant, Integer> ordering = new HashMap<>();
        violatedSets.stream().sorted(Comparator.comparingInt(Set::size))
                .forEach(set -> set.stream().forEach(constant -> {
                    if (!ordering.containsKey(constant)) {
                        ordering.put(constant, ordering.size());
                    }
                }));
        int biggestViolated = ordering.size() - 1; // it describes the index of the biggest from the violated
        // the ordering needs to describe also constants which are not in the violated
        constants.forEach(constant -> {
            if (!ordering.containsKey(constant)) {
                ordering.put(constant, ordering.size());
            }
        });

        Comparator<? super Constant> innerComparator = Comparator.comparingInt(ordering::get);
        List<List<Constant>> violated = violatedSets.stream()
                .map(set -> {
                    List<Constant> list = Sugar.listFromCollections(set);
                    Collections.sort(list, innerComparator);
                    return list;
                })
                .collect(Collectors.toList());

        Comparator<? super List<Constant>> comparator = (l1, l2) -> {
            int sizeCompare = Integer.compare(l1.size(), l2.size());
            if (0 != sizeCompare) {
                return sizeCompare;
            }
            int lowerCommon = Math.min(l1.size(), l2.size());
            for (int idx = 0; idx < lowerCommon; idx++) {
                int compare = Integer.compare(ordering.get(l1.get(idx)), ordering.get(l2.get(idx)));
                if (0 != compare) {
                    return compare;
                }
            }
            throw new IllegalStateException("it cannot reach this point ;)");
        };
        Collections.sort(violated, comparator);

        List<Constant> currentElements = Sugar.listFromCollections(constants);
        Collections.sort(currentElements, innerComparator);

        Integer smallestLastIndex;
        do {
            smallestLastIndex = smallestLastIndex(state, violated, ordering, currentElements);
            if (null == smallestLastIndex) {
                // => no rules is violated, thus find first smallest or equal value than biggestViolated (the ones after can be skipped)
                for (int idx = k - 1; idx >= 0; idx--) {
                    if (state[idx] <= biggestViolated) {
                        smallestLastIndex = idx;
                        break;
                    }
                }

                if (null == smallestLastIndex) {
                    // we can end here, there is any other possibility
//                    System.out.println("final end");
                    break;
                }
            } else {
                retVal = retVal.add(skipable(state, smallestLastIndex));

                // a speedup hack; throwing away the subset of which all supersets are pruned by the skipable :))
                for (int eIdx = violated.size() - 1; eIdx >= 0; eIdx--) {
                    boolean allMatched = true;
                    List<Constant> subset = violated.get(eIdx);
                    for (int sIdx = subset.size() - 1; allMatched && sIdx >= 0; sIdx--) {
                        if (ordering.get(subset.get(sIdx)) != state[sIdx]) {
                            allMatched = false;
                        }
                    }
                    if (allMatched) {
                        violated.remove(eIdx);
                    }
                }
            }

        } while ((state = next(state, smallestLastIndex, currentElements)) != null);

        return retVal;
    }

    private String canon(int[] state) {
        return Arrays.stream(state).boxed().map(Object::toString).collect(Collectors.joining(", "));
    }

    private String canon(int[] state, List<Constant> elements) {
        return Arrays.stream(state).boxed().map(elements::get).map(Object::toString).sorted().collect(Collectors.joining(", "));
    }


    // old violated ksizeSet
    private int violatedKsizeSetOld(Set<Set<Constant>> violated) {
        // for sure, there is a possibility for some caching and hacks to speed it up, e.g. throwing out violated subsets which are skipped already, or it may not (we need the violated to be sorted by the order of constants we have and to remove only the first one
        int retVal = 0;

        int[] state = new int[k];
        for (int idx = 0; idx < k; idx++) {
            state[idx] = idx;
        }

        Map<Constant, Integer> ordering = new HashMap<>();
        constants.stream().sorted(Comparator.comparing(Constant::name)).forEach(c -> ordering.put(c, ordering.size()));

        List<List<Constant>> violatedList = violated.stream()
                .map(s -> s.stream().sorted(Comparator.comparing(Constant::name)).collect(Collectors.toList()))
                .collect(Collectors.toList());

        Integer smallestLastIndex;
        do {
            smallestLastIndex = smallestLastIndex(state, violatedList, ordering, constants);
            if (null == smallestLastIndex) {
                smallestLastIndex = k - 1;
            } else {
//                System.out.println(canon(state, constants));
//                System.out.println(canon(state));
//                Set<Constant> currentConstants = Sugar.set();
//                for (int constIdx : state) {
//                    currentConstants.add(constants.get(constIdx));
//                }
                retVal += skipable(state, smallestLastIndex).doubleValue();
//                System.out.println("skipable\t" + skipable(state, smallestLastIndex));
//                System.out.println("tady muzu preskocit i dalsi, napriklad kdyz prvni prvek v k-tici (generujem je postupne) je uz vyssi nez nejvyssi z konstant z violated, tak to muzu oriznout");
            }

        } while ((state = next(state, smallestLastIndex, constants)) != null);

        return retVal;
    }
//     */

    private int violatedKsizeSetBrute(Set<Set<Constant>> violated) {
        // for sure, there is a possibility for some caching and hacks to speed it up, e.g. throwing out violated subsets which are skipped already, or it may not (we need the violated to be sorted by the order of constants we have and to remove only the first one
        int retVal = 0;

        int[] state = new int[k];
        for (int idx = 0; idx < k; idx++) {
            state[idx] = idx;
        }

        Map<Constant, Integer> ordering = new HashMap<>();
        constants.stream().sorted(Comparator.comparing(Constant::name)).forEach(c -> ordering.put(c, ordering.size()));

        Integer smallestLastIndex;
        do {
            smallestLastIndex = k - 1;
            Set<Constant> constStats = Arrays.stream(state).boxed().map(constants::get).collect(Collectors.toSet());
            boolean containsViolated = violated.stream().anyMatch(s -> constStats.containsAll(s));
            if (containsViolated) {
                retVal += 1;
//                allViolated.add(constStats);
            }
        } while ((state = next(state, smallestLastIndex, constants)) != null);

        return retVal;
    }

    private BigDecimal skipable(int[] state, Integer lastIdx) {
        if (lastIdx == state.length - 1) {
            return BigDecimal.ONE;
        }
        int n = constants.size() - (state[lastIdx] + 1); // the -1s are there because we are working with indexes from zero almost everywhere
        int kPrime = k - (lastIdx + 1);
        if (1 == kPrime) { // a speed-up hack, no need to compute trivial thing by computing two factorials of n and n-1
            return BigDecimal.valueOf(n);
        }
        return Combinatorics.binomialBig(n, kPrime, precision);
    }

    private int[] next(int[] state, Integer smallestLastIndex, List<Constant> elements) {
        int last = lastIncrementable(state, smallestLastIndex, elements.size());
        if (last < 0) {
            return null;
        }

        int startingVal = state[last] + 1;
        for (int idx = last; idx < k; idx++) {
            state[idx] = startingVal;
            startingVal += 1;
        }

        return state;
    }

    private int lastIncrementable(int[] state, int idx, int elements) {
        // state is a array of indexes (indexed from 0)
        for (; idx >= 0; idx--) {
            int aboveP = k - (idx + 1);
            if ((state[idx] + 1) + aboveP < elements) { // <=
                return idx;
            }
        }
        return -1;
    }

    private Integer smallestLastIndex(int[] state, List<List<Constant>> violated, Map<Constant, Integer> ordering, List<Constant> currentElements) {
        Set<Constant> currentConstants = Sugar.set();
        for (int constIdx : state) {
            currentConstants.add(currentElements.get(constIdx));
        }

        Integer minimalBiggestFromSubsets = null; // idx of the constant which is the biggest in the comparison, but smallest between those subsets of violated which subsumes currentConstants
        for (List<Constant> violating : violated) {
            if (currentConstants.containsAll(violating)) {
                Integer biggestCurrent = ordering.get(violating.get(violating.size() - 1));
                if (null == minimalBiggestFromSubsets
                        || biggestCurrent < minimalBiggestFromSubsets) {
                    minimalBiggestFromSubsets = biggestCurrent;
                }
            }
        }
        // getting the real idx of the minimal last from violating ones
        if (null != minimalBiggestFromSubsets) {
            for (int idx = 0; idx < state.length; idx++) {
                if (state[idx] == minimalBiggestFromSubsets) {
                    return idx;
                }
            }
            throw new IllegalStateException("bug somwehere");
        }
        return null;
    }

    private Set<Set<Constant>> findViolatedSubsets(HornClause hc) {
        Pair<Term[], List<Term[]>> substitutions = matching.allSubstitutions(LogicUtils.flipSigns(hc.toClause()), 0, maxGroundSubstitutions);
        Set<Set<Constant>> violated = Sugar.set();
        for (Term[] terms : substitutions.s) {
            Set<Constant> constants = Arrays.stream(terms).map(t -> (Constant) t).collect(Collectors.toSet());
            if (constants.size() <= k) {
                violated.add(constants);
            }
        }
        return violated;
    }

    @Override
    public Coverage getPosIdxs() {
        return CoverageFactory.getInstance().take(0, 1);
    }

    @Override
    public Coverage getNegIdxs() {
        return CoverageFactory.getInstance().take(0, 1);
    }

    @Override
    public int size() {
        return 2;
    }

    @Override
    public boolean matchesAtLeastOne(Clause clause) {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public Coverage subsumed(Clause clause, Coverage scount) {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public boolean allNegativeExamples(Coverage learnFrom) {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public double[] getTargets() {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public DatasetInterface subDataset(Coverage parentsCoverage) {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public DatasetInterface deepCopy(int subsumptionType) {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public DatasetInterface flatten(int subsumptionMode) {
        throw new IllegalStateException();// NotImplementedException();
    }


    public static PACAccuracyDataset create(Collection<Literal> evidence, int k) {
        return create(evidence, k, Integer.MAX_VALUE);
    }

    public static PACAccuracyDataset create(Collection<Literal> evidence, int k, int maxGroundSubstitutitons) {
        return new PACAccuracyDataset(Sugar.setFromCollections(evidence), k, maxGroundSubstitutitons);
    }


    public static void main(String[] args) {
//        List<String> q = Sugar.list("a", "b", "c", "d", "e", "q", "z");
//        List<List<String>> perms = Combinatorics.variations(Sugar.setFromCollections(q), 4);
//        Set<List<String>> set = Sugar.set();
//        for (List<String> perm : perms) {
//            List<String> s = perm.stream().sorted(Comparator.comparing(String::toString)).collect(Collectors.toList());
//            if(set.contains(s)){
//                continue;
//            }
//            set.add(s);
//            System.out.println(s);
//        }
        test();
    }

    private static void test() {
        int k = 4;
        HornClause rule = HornClause.create(Clause.parse("!p(X,Y),q(Y)"));
        Set<Literal> evidence = Clause.parse(
                "p(a,b),q(b),p(a,c),q(d),q(e)" // k=4, acc=0.4
//                "p(a,b),p(a,c),q(d),q(e)" // k=4, acc=0.2
//                "p(a,b),p(a,c),q(d),q(e),q(b),q(c)" // k=4, acc=1
//                "p(a,b),p(a,c),q(d),q(e),p(q,z),p(b,a)" // k=4, acc=
        ).literals();


        PACAccuracyDataset pad = PACAccuracyDataset.create(evidence, k);
        Pair<BigDecimal, BigDecimal> classification = pad.classifySize(rule);

        System.out.println("pos\t" + classification.getR()
                + "\nneg\t" + (classification.getS().subtract(classification.getR()))
                + "\nacc\t" + (classification.getR().divide(classification.getS(), MathContext.DECIMAL128)));


    }
}
