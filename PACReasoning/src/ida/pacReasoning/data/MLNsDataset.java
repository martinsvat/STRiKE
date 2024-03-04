package ida.pacReasoning.data;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.utils.Combinatorics;
import ida.utils.MutableDouble;
import ida.utils.Sugar;
import ida.utils.collections.MultiList;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.theories.TheorySolver;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * From Ondra's MLNDataset.... hacked to have it quickly working
 * <p>
 * Created by martin.svatos on 10. 8. 2018.
 */
public class MLNsDataset implements DatasetInterface {

    private double positiveWeight = 0.5, negativeWeight = 0.5;

    private double numPositiveExamples, numNegativeExamples;

    private Clause example;

    private Set<Pair<String, Integer>> allPredicates;

    private Set<Pair<String, Integer>> queryPredicates = new HashSet<Pair<String, Integer>>();

    private Matching matching;
    private List<Sample> samples;
    private boolean parallel = true;


    public MLNsDataset(Clause example, Collection<Literal> positiveExamples, Collection<Literal> negativeExamples) {
        List<Literal> queries = new ArrayList<Literal>();
        queries.addAll(positiveExamples);
        queries.addAll(negativeExamples);
        List<Boolean> targets = new ArrayList<Boolean>();
        for (int i = 0; i < positiveExamples.size(); i++) {
            targets.add(Boolean.TRUE);
        }
        for (int i = 0; i < negativeExamples.size(); i++) {
            targets.add(Boolean.FALSE);
        }
        set(example, queries, targets);
    }

    private void set(Clause example, List<Literal> queries, List<Boolean> targets) {
        this.example = example;
        this.matching = new Matching(Sugar.<Clause>list(example));

        this.allPredicates = LogicUtils.predicates(this.example);
        assert queries.size() == targets.size();
        this.samples = Sugar.list();
        for (int idx = 0; idx < queries.size(); idx++) {
            Literal l = queries.get(idx);
            boolean target = targets.get(idx);
            if (target) {
                this.numPositiveExamples++;
            } else {
                this.numNegativeExamples++;
            }
            this.samples.add(new Sample(l, target, example));
            this.queryPredicates.add(new Pair<String, Integer>(l.predicate(), l.arity()));
        }
    }

    public void setPositiveWeight(double positiveWeight) {
        this.positiveWeight = positiveWeight;
    }

    public void setNegativeWeight(double negativeWeight) {
        this.negativeWeight = negativeWeight;
    }

    public double positiveWeight() {
        return this.positiveWeight;
    }

    public double negativeWeight() {
        return this.negativeWeight;
    }


    @Override
    public int numExistentialMatches(HornClause hc, int maxNum) {
        return numExistentialMatches(hc, maxNum, CoverageFactory.getInstance().get(samples.size()));
    }

    @Override
    public int numExistentialMatches(HornClause hc, int maxNum, Coverage checkOnly) {
        if (parallel) {
            return (int) Sugar.parallelStream(checkOnly.stream(), this.parallel).filter(idx -> matches(hc, idx)).count();
        }
        // sequential version
        int matches = 0;
        for (Integer idx : checkOnly) {
            if (matches(hc, idx)) {
                matches++;
            }
            if (matches >= maxNum) {
                return matches;
            }
        }
        return matches;
    }

    private boolean matches(HornClause hc, Integer idx) {
        Sample sample = samples.get(idx);
        Clause unified = hc.unify(sample.getQuery());
        if (null == unified) {
            throw new IllegalStateException("cannot unify different predicates");
        }
        Clause c = new Clause(Sugar.iterable(unified.literals(), headAvoidingLiterals(sample.getQuery(), unified)));
        return sample.getMatching().subsumption(LogicUtils.flipSigns(c), 0);
    }

    private List<Literal> headAvoidingLiterals(Literal query, Clause clause) {
        List<Literal> lits = new ArrayList<Literal>();
        for (Literal l : clause.literals()) {
            if (l.arity() == query.arity() && l.predicate().equals(query.predicate())) {
                int arity = l.arity();
                Literal tuple = new Literal(SpecialVarargPredicates.TUPLE, 2 * arity);
                for (int i = 0; i < arity; i++) {
                    tuple.set(l.get(i), i);
                    tuple.set(query.get(i), arity + i);
                }
                lits.add(tuple);
            }
        }
        return lits;
    }

    public Triple<Coverage, Coverage, EvaluationMetrics> evaluate(HornClause hc, Coverage checkOnly) {
        List<Integer> covered = Sugar.parallelStream(checkOnly.stream(), this.parallel).filter(idx -> matches(hc, idx)).boxed().collect(Collectors.toList());
        Coverage posCovered = CoverageFactory.getInstance().get(covered.stream().filter(idx -> samples.get(idx).getTarget()).mapToInt(i -> i));
        covered.removeAll(posCovered.asSet());
        Coverage negCovered = CoverageFactory.getInstance().get(covered);
        return new Triple<>(posCovered, negCovered, new EvaluationMetrics(posCovered.size(), negCovered.size()));
    }


    @Override
    public Pair<Coverage, Coverage> classify(HornClause hc, Coverage checkOnly) {
        Set<Integer> covered = Sugar.parallelStream(checkOnly.stream(), this.parallel).filter(idx -> matches(hc, idx)).boxed().collect(Collectors.toSet());
        Coverage posCovered = CoverageFactory.getInstance().get(covered.stream().filter(idx -> samples.get(idx).getTarget()).mapToInt(i -> i));
        covered.removeAll(posCovered.asSet());
        Coverage negCovered = CoverageFactory.getInstance().get(covered);
        return new Pair<>(posCovered, negCovered);
    }

    @Override
    public Set<Pair<String, Integer>> queryPredicates() {
        return samples.stream().map(sample -> sample.getQuery().getPredicate()).collect(Collectors.toSet());
    }

    @Override
    public Set<Pair<String, Integer>> allPredicates() {
        return Sugar.union(queryPredicates, example.literals().stream().map(l -> l.getPredicate()).collect(Collectors.toList()));
    }

    @Override
    public Coverage getPosIdxs() {
        return CoverageFactory.getInstance().get(IntStream.range(0, samples.size()).filter(idx -> samples.get(idx).getTarget()));
    }

    @Override
    public Coverage getNegIdxs() {
        return CoverageFactory.getInstance().get(IntStream.range(0, samples.size()).filter(idx -> !samples.get(idx).getTarget()));
    }

    @Override
    public int size() {
        return samples.size();
    }

    @Override
    public boolean matchesAtLeastOne(Clause clause) {
        throw new UnsupportedOperationException("implemented for horns only");
    }

    @Override
    public Coverage subsumed(Clause clause, Coverage scount) {
        throw new UnsupportedOperationException("implemented for horns only");
    }

    @Override
    public boolean allNegativeExamples(Coverage learnFrom) {
        return learnFrom.stream().allMatch(idx -> samples.get(idx).getTarget());
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

    public static MLNsDataset createDataset(Clause example, Predicate query, int numExamples, Collection<Clause> hardRules, Random random) {
        List<Literal> positiveCandidates = example.getLiteralsByPredicate(query.getName()).stream()
                .filter(l -> l.arity() == query.getArity()).collect(Collectors.toList());

        //positive examples
        List<Literal> sampleOfPositiveExamples = Combinatorics.randomCombination(positiveCandidates, Math.min(positiveCandidates.size(), numExamples)).toList();

        //negative examples
        Set<Literal> possibleExamples = new HashSet<>();
        MutableDouble estimatedNegativeCount = new MutableDouble();
        possibleExamples.addAll(MLNsDataset.possibleExamples(example, query.getName(), query.getArity(), hardRules, numExamples, random, estimatedNegativeCount));
        Set<Literal> sampleOfnegativeExamples = Sugar.setDifference(possibleExamples, example.literals());
        if (estimatedNegativeCount != null) {
            estimatedNegativeCount.set(estimatedNegativeCount.value() * sampleOfnegativeExamples.size() / (double) possibleExamples.size());
        }
        MLNsDataset retVal = new MLNsDataset(example, sampleOfPositiveExamples, sampleOfnegativeExamples);

        retVal.negativeWeight = estimatedNegativeCount.value() / (double) sampleOfnegativeExamples.size();
        if(retVal.negativeWeight == Double.NaN || sampleOfnegativeExamples.isEmpty()){
            System.out.println("problem here");
        }
        retVal.positiveWeight = positiveCandidates.size() / (double) sampleOfPositiveExamples.size();
        retVal.numPositiveExamples = sampleOfPositiveExamples.size();
        retVal.numNegativeExamples = sampleOfnegativeExamples.size();

        return retVal;
    }


    private static List<Literal> possibleExamples(Clause db, String predicateName, int arity, Collection<Clause> hardRules, int desiredNum, Random random, MutableDouble estimatedCount) {
        List<Clause> theory = Sugar.list();
        theory.addAll(hardRules);

        TheorySolver ts = new TheorySolver();
        List<Clause> theoryAndEvidence = Sugar.list();
        theoryAndEvidence.addAll(theory);
        for (Literal l : db.literals()) {
            theoryAndEvidence.add(new Clause(l));
        }

        Clause fastCheckDB = new Clause(ts.solve(theoryAndEvidence));

        MultiList<Pair<String, Integer>, Constant> typing = typing(db);
        int max = 1;
        for (int i = 0; i < arity; i++) {
            max *= typing.get(new Pair<String, Integer>(predicateName, i)).size();
        }
        desiredNum = (int) (0.8 * Math.min(max, desiredNum));

        List<Constant> candidateConstants = typing.get(new Pair<String, Integer>(predicateName, 0));

        Set<Literal> retVal0 = new HashSet<Literal>();
        int maxTries = desiredNum * 5;
        int ok = 0;
        int chunk = 100;
        int tries = 0;
        for (int i = 0; i < maxTries; i++) {
            Set<Literal> newLs = new HashSet<Literal>();
            Set<Literal> additionalDBLits = new HashSet<Literal>();
            for (int j = 0; j < chunk; j++) {
                Literal newL = new Literal(predicateName, arity);
                for (int k = 0; k < arity; k++) {
                    List<Constant> t = typing.get(new Pair<String, Integer>(predicateName, k));
                    newL.set(t.get(random.nextInt(t.size())), k);
                }
                newLs.add(newL);
                Set<Literal> sol = ts.solve(theory, Sugar.set(newL));
                if (sol != null) {
                    additionalDBLits.addAll(sol);
                }
            }

            if (ts.findViolatedRules(theory, Sugar.union(fastCheckDB.literals(), additionalDBLits, newLs)).isEmpty()) {
                retVal0.addAll(newLs);
                ok += newLs.size();
                tries += chunk;
                //System.out.println("ok chunk");
            } else {
                for (Literal newL : newLs) {
                    List<Clause> testTheory = new ArrayList<Clause>();
                    testTheory.addAll(theory);
                    testTheory.add(new Clause(newL));
                    for (Literal dbLit : db.literals()) {
                        testTheory.add(new Clause(dbLit));
                    }
                    if (ts.solve(testTheory) != null) {
                        ok++;
                        retVal0.add(newL);
                    }
                    tries++;
                }
            }
            if (retVal0.size() >= desiredNum) {
                double est = (double) ok / (double) tries;
                for (int j = 0; j < arity; j++) {
                    est *= typing.get(new Pair<String, Integer>(predicateName, j)).size();
                }
                estimatedCount.set(est);
                return Sugar.listFromCollections(retVal0);
            }
            if (tries > 30 && retVal0.size() / (double) tries < 0.2) {
                break;
            }
        }

        List<Literal> current = new ArrayList<Literal>();
        current.add(LogicUtils.newLiteral(predicateName, arity));

        double mult = 1.0;
        for (int i = 0; i < arity; i++) {
            List<Clause> slice = theorySlice(theory, predicateName, arity, 0, i + 1);

            List<Clause> sliceTheory = new ArrayList<Clause>(slice);

            for (Literal l : db.literals()) {
                slice.add(new Clause(l));
            }
            List<Literal> next = new ArrayList<Literal>();
            List<Pair<Literal, Constant>> candidates = new ArrayList<Pair<Literal, Constant>>();

            for (Constant c : i == 0 ? candidateConstants : typing.get(new Pair<String, Integer>(predicateName, i))/*possibleConstants(db, candidateConstants, theory, predicateName, arity, i)*/) {
                for (Literal l : current) {
                    candidates.add(new Pair<Literal, Constant>(l, c));
                }
            }
            Collections.shuffle(candidates, random);
            int k = 0;
            for (Pair<Literal, Constant> pair : candidates) {
                Literal newL = new Literal(predicateName, i + 1);
                for (int j = 0; j < i; j++) {
                    newL.set(pair.r.get(j), j);
                }
                newL.set(pair.s, i);


                if (ts.findViolatedRules(sliceTheory, Sugar.union(fastCheckDB.literals(), newL)).isEmpty()) {
                    next.add(newL);
                    //System.out.println("pass: "+newL);
                } else if (ts.solve(Sugar.union(slice, new Clause(newL))) != null) {
                    next.add(newL);
                }
                if (next.size() >= desiredNum) {
                    mult *= (candidates.size() / (double) desiredNum);
                    break;
                }
                k++;
            }
            current = next;
        }
        estimatedCount.set(current.size() * mult);
        return current;
    }


    private static List<Clause> theorySlice(List<Clause> theory, String predicate, int arity, int start, int end) {
        List<Clause> retVal = new ArrayList<Clause>();
        outerLoop:
        for (Clause c : theory) {
            if (!c.predicates().contains(predicate)) {
                retVal.add(c);
            } else {
                List<Literal> l1 = new ArrayList<Literal>();
                List<Literal> l2 = new ArrayList<Literal>();
                for (Literal l : c.literals()) {
                    if (l.predicate().equals(predicate) && l.arity() == arity) {
                        l1.add(l);
                    } else {
                        l2.add(l);
                    }
                }
                List<Literal> l3 = new ArrayList<Literal>();
                for (Literal l : l1) {
                    for (int i : outerIntervals(start, end, arity)) {
                        int count = 0;
                        for (Literal byTerm : c.getLiteralsByTerm(l.get(i))) {
                            for (int j = 0; j < byTerm.arity(); j++) {
                                if (byTerm.get(j).equals(l.get(i))) {
                                    count++;
                                }
                            }
                            if (count > 1) {
                                continue outerLoop;
                            }
                        }
                    }
                    Literal shorter = new Literal(l.predicate(), l.isNegated(), end - start);
                    int j = 0;
                    for (int i = start; i < end; i++) {
                        shorter.set(l.get(i), j++);
                    }
                    l3.add(shorter);
                }
                retVal.add(new Clause(Sugar.union(l2, l3)));
            }
        }
        return retVal;
    }


    private static int[] outerIntervals(int start, int end, int length) {
        int[] retVal = new int[start + length - end];
        int j = 0;
        for (int i = 0; i < start; i++) {
            retVal[j] = i;
            j++;
        }
        for (int i = end; i < length; i++) {
            retVal[j] = i;
            j++;
        }
        return retVal;
    }

    public static MultiList<Pair<String, Integer>, Constant> typing(Clause db) {
        MultiMap<Pair<String, Integer>, Constant> mm = new MultiMap<Pair<String, Integer>, Constant>();
        for (Literal l : db.literals()) {
            for (int i = 0; i < l.arity(); i++) {
                mm.put(new Pair<String, Integer>(l.predicate(), i), (Constant) l.get(i));
            }
        }
        MultiMap<Pair<String, Integer>, Constant> retVal = new MultiMap<Pair<String, Integer>, Constant>();
        for (Map.Entry<Pair<String, Integer>, Set<Constant>> entry1 : mm.entrySet()) {
            for (Map.Entry<Pair<String, Integer>, Set<Constant>> entry2 : mm.entrySet()) {
                if (!Sugar.intersection(entry1.getValue(), entry2.getValue()).isEmpty()) {
                    retVal.putAll(entry1.getKey(), entry1.getValue());
                    retVal.putAll(entry1.getKey(), entry2.getValue());
                    retVal.putAll(entry2.getKey(), entry1.getValue());
                    retVal.putAll(entry2.getKey(), entry2.getValue());
                }
            }
        }
        return retVal.toMultiList();
    }


    public class EvaluationMetrics {

        private final long numNegCovered;
        private final long numPosCovered;
        private double accuracy, precision, recall;

        private EvaluationMetrics(long numPosCovered, long numNegCovered) {
            this.accuracy = (positiveWeight * numPosCovered + negativeWeight * (numNegativeExamples - numNegCovered))
                    /
                    (positiveWeight * numPositiveExamples + negativeWeight * numNegativeExamples);
            if (numPosCovered + numNegCovered == 0) {
                precision = 1;
            } else {
                this.precision = positiveWeight * numPosCovered / (positiveWeight * numPosCovered + negativeWeight * numNegCovered);
            }
            this.recall = numPosCovered / numPositiveExamples;
            this.numPosCovered = numPosCovered;
            this.numNegCovered = numNegCovered;

        }

        public long getNumNegCovered() {
            return numNegCovered;
        }

        public long getNumPosCovered() {
            return numPosCovered;
        }

        public double accuracy() {
            return this.accuracy;
        }

        public double precision() {
            return this.precision;
        }

        public double recall() {
            return this.recall;
        }

        public double f1() {
            return 2 * precision * recall / (precision + recall);
        }

        public double fBeta(double beta) {
            return (1 + beta * beta) * precision * recall / (beta * beta * precision + recall);
        }

        public String toString() {
            return "[accuracy=" + accuracy + ", precision=" + precision + ", recall=" + recall + ", f1=" + f1() + ", #pos=" + numPosCovered + ", #neg=" + numNegCovered + "]";
        }


    }

}

class Sample {
    private final Literal query;
    private final boolean target;
    private final Matching m;

    public Sample(Literal query, boolean target, Clause example) {
        this.query = query;
        this.target = target;
        this.m = Matching.create(example, Matching.THETA_SUBSUMPTION);
    }

    public Literal getQuery() {
        return query;
    }

    public boolean getTarget() {
        return target;
    }

    public Matching getMatching() {
        return m;
    }
}

