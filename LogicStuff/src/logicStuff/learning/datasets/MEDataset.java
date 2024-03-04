package logicStuff.learning.datasets;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.io.PseudoPrologParser;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.utils.Sugar;
import ida.utils.VectorUtils;
import ida.utils.tuples.Pair;


import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * big TODO takovej trash.... uklidit jednou !!!!
 * several of the things with MED is done by taking the i-th example by force, this does not have to hold in the case that queries are -> should be fixed now
 * <p>
 * <p>
 * Created by martin.svatos on 27. 6. 2017.
 */
public class MEDataset implements DatasetInterface {
    private final int substitutionType;
    private double[] targets;

    //one query per example, or no queries at all
    private List<Literal> queries;

    private List<Clause> examples;

    private Set<Pair<String, Integer>> allPredicates;

    private Set<Pair<String, Integer>> queryPredicates = new HashSet<Pair<String, Integer>>();

    private List<Matching> matchings;
    //private Matching matching;

    private final boolean parallel = 1 < Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1"))
            || Boolean.parseBoolean(System.getProperty("logicStuff.learning.datasets.MEDataset.parallel", "false"));
    private boolean headAvoiding = Boolean.parseBoolean(System.getProperty("logicStuff.learning.dataset.MEDataset.headAvoiding", "true"));

    public MEDataset(List<Clause> example, List<Double> target, int substitutionType) {
        this(example, target, substitutionType, null);
    }

    public MEDataset(List<Clause> examples, List<Literal> queries, List<Double> targets) {
        this(examples, targets, Matching.THETA_SUBSUMPTION, queries);
    }

    public MEDataset(List<Clause> example, List<Double> target, int substitutionType, List<Literal> queries) {
        this.targets = VectorUtils.toDoubleArray(target);
        this.examples = example;
        // can be this simplified?
        if (null == queries) {
            this.matchings = example.stream().map(sample -> Matching.create(sample, substitutionType)).collect(Collectors.toList());
        } else {
            this.matchings = (parallel) ?
                    IntStream.range(0, queries.size()).mapToObj(i -> Matching.create(example.get(0), substitutionType)).collect(Collectors.toList())
                    : example.stream().map(sample -> Matching.create(sample, substitutionType)).collect(Collectors.toList());
        }
        // there was && in the if below, but it should be || for paralellization at each time stamp
//        this.matchings = (null == queries && !parallel) ?
//                example.stream().map(sample -> Matching.create(sample, substitutionType)).collect(Collectors.toList())
//                : IntStream.range(0, queries.size()).mapToObj(i -> Matching.create(example.get(0), substitutionType)).collect(Collectors.toList());
        this.allPredicates = LogicUtils.predicates(examples);
        this.substitutionType = substitutionType;
        this.queries = queries;
        if (null != queries) {
            this.queryPredicates.addAll(queries.stream().map(Literal::getPredicate).collect(Collectors.toList()));
        }
    }

    /**
     * Creates new MEDataset with OI_subsumption by default.
     *
     * @param example
     * @param target
     */
    public MEDataset(List<Clause> example, List<Double> target) {
        this(example, target, Matching.OI_SUBSUMPTION);
    }

    public MEDataset(List<Clause> examples, List<Literal> queries, List<Double> targets, int substitutionType) {
        this(examples, targets, substitutionType);
        this.queries = queries;
    }

    public List<Literal> getQueries() {
        return queries;
    }

    public int getSubstitutionType() {
        return substitutionType;
    }

    @Override
    public Set<Pair<String, Integer>> queryPredicates() {
        return queryPredicates;
    }

    @Override
    public Set<Pair<String, Integer>> allPredicates() {
        return allPredicates;
    }

    public List<Clause> getExamples() {
        return examples;
    }

    // awful hack for targets
    public Pair<Coverage, Coverage> classify(HornClause hc, Coverage learnFrom) {
        Set<Integer> covered = Sugar.parallelStream(learnFrom.stream(), this.parallel).filter(idx -> classify(hc, idx)).boxed().collect(Collectors.toSet());
        Coverage posCovered = CoverageFactory.getInstance().get(covered.stream().filter(idx -> targets[idx] > 0.5).mapToInt(i -> i));
        covered.removeAll(posCovered.asSet());
        Coverage negCovered = CoverageFactory.getInstance().get(covered);
        return new Pair<>(posCovered, negCovered);
    }

    public boolean classify(HornClause rule, int dbIndex) {
        if (queries == null) {
            Clause unified = rule.body(); // conjunction
            unified = this.parallel ? new Clause(unified.literals()) : unified;
            if (unified != null) {
                if (matchings.get(dbIndex).subsumption(unified, 0)) {
                    return true;
                }
            }
            return false;
        } else {
            Literal query = queries.get(dbIndex);
            Clause unified = rule.unify(query); // disjunction
            if (null == unified) {
                return false;
            }
            if (headAvoiding) {
                unified = new Clause(Sugar.iterable(unified.literals(), headAvoidingLiterals(query, unified)));
            }
            unified = LogicUtils.flipSigns(unified); // conjunction
            return matchings.get(0).subsumption(unified, 0); // in this case
        }
    }

    private int existentialMatchesOnExamples(HornClause hc, int maxNum, Coverage coverage) {
        int retVal = 0;
        Clause c = LogicUtils.flipSigns(hc.toClause());
        if (parallel) {
            retVal = getSubsumed(c, coverage.stream()).size();
        } else { // just a little speed up for sequential version
            for (Integer idx : coverage.asSet()) {
                if (matches(c, idx)) {
                    retVal++;
                    if (retVal >= maxNum) {
                        break;
                    }
                }
            }
        }
        return retVal;
    }

    private int existentialMatchesOnQueries(HornClause hc, int maxNum, Coverage coverage) {
        int retVal = 0;
        // parallelization here is probably not possible, because the instance of matching is only one, shared
//        if (parallel) {
//            return coverage.stream().filter(idx -> {
//                Clause c = LogicUtils.flipSigns(LogicUtils.flipSigns(hc.unify(queries.get(idx))));
//
//            }).count();
//        } else {
//
        /*//something like this was the original hint
        for (int idx : coverage.asSet()) {
            //Clause c = LogicUtils.flipSigns(LogicUtils.flipSigns(hc.unify(queries.get(idx))));
            Clause c = hc.unify(queries.get(idx));
            if (null != c && matches(c, 0)) { // in this case
                retVal++;
                if (retVal >= maxNum) {
                    break;
                }
            }
        }
//        }
*/
        if (parallel) {
            retVal = coverage.stream().filter(idx -> {
                Literal query = queries.get(idx);
                Clause unified = hc.unify(query);
                if (null == unified) {
                    return false;
                }
                if (headAvoiding) {
                    unified = new Clause(Sugar.iterable(unified.literals(), headAvoidingLiterals(query, unified)));
                }
                unified = LogicUtils.flipSigns(unified);
                return matchings.get(idx).subsumption(unified, 0);
            }).sum();
        } else {

            for (int idx : coverage.asSet()) {
                Literal query = queries.get(idx);
                Clause unified = hc.unify(query);
                if (null == unified) {
                    continue;
                }
                if (headAvoiding) {
                    unified = new Clause(Sugar.iterable(unified.literals(), headAvoidingLiterals(query, unified)));
                }
                unified = LogicUtils.flipSigns(unified);
                if (matchings.get(0).subsumption(unified, 0)) { // in this case
                    // speedup for matches(unified, 0) (skipping setting for parallelization)
                    retVal++;
                    if (retVal >= maxNum) {
                        break;
                    }
                }
            }
        }
        return retVal;
    }

    @Override
    public int numExistentialMatches(HornClause hc, int maxNum, Coverage coverage) {
        return (queries == null) ? existentialMatchesOnExamples(hc, maxNum, coverage) : existentialMatchesOnQueries(hc, maxNum, coverage);
    }

    @Override
    public int numExistentialMatches(HornClause hc, int maxNum) {
        return numExistentialMatches(hc, maxNum, Coverage.create(IntStream.range(0, (null == queries) ? examples.size() : queries.size())));
    }

    public Coverage getPosIdxs() {
        return CoverageFactory.getInstance().get(IntStream.range(0, targets.length).filter(idx -> targets[idx] >= 0.5));
    }

    public Coverage getNegIdxs() {
        return CoverageFactory.getInstance().get(IntStream.range(0, targets.length).filter(idx -> targets[idx] < 0.5));
    }

    @Override
    public int size() {
        return (queries == null) ? examples.size() : queries.size();
    }

    // true iff all examples negative from the view
    public boolean allNegativeExamples(Coverage coverage) {
        return coverage.stream().allMatch(idx -> targets[idx] < 0.5);
    }

    // why is this here? who does need it?
    public boolean isPredictionCorrect(List<HornClause> rules, Integer idx) {
        boolean prediction = false;

        for (HornClause rule : rules) {
            if (classify(rule, idx)) {
                prediction = true;
                break;
            }
        }

        if (prediction && targets[idx] >= 0.5) {
            return true;
        } else if (!prediction && targets[idx] < 0.5) {
            return true;
        }
        return false;
    }

    public double[] getTargets() {
        return targets;
    }

    /**
     * Creates new MEDataset by parsing the path
     *
     * @param dataPath
     * @param subsumptionMode
     * @return
     * @throws IOException
     */
    public static MEDataset create(String dataPath, int subsumptionMode) throws IOException {
        return create(new FileReader(dataPath), subsumptionMode);
    }

    public static MEDataset create(Reader fileReader, int subsumptionMode) throws IOException {
        Pair<List<Clause>, List<Double>> pair = load(fileReader);
        return new MEDataset(pair.r, pair.s, subsumptionMode);
    }

    public static MEDataset create(Path input, int subsumptionMode) throws IOException {
        return create(input.toString(), subsumptionMode);
    }

    public static MEDataset createOnePositiveExample(String dataPath, int subsumptionType) throws IOException {
        System.out.println("Merging all facts from the input file to big positive sexample.");
        Set<Literal> literals = Sugar.set();
        Files.lines(Paths.get(dataPath)).filter(line -> line.trim().length() > 0)
                .forEach(line -> literals.addAll(Clause.parse(line).literals()));
        return new MEDataset(Sugar.list(new Clause(literals)), Sugar.list(1.0), subsumptionType);
    }


    // this is not so much nice
    public static Pair<List<Clause>, List<Double>> load(Path path) throws IOException {
        return load(new FileReader(path.toString()));
    }

    private static Pair<List<Clause>, List<Double>> load(Reader fileReader) throws IOException {
        List<Double> targets = new ArrayList<Double>();
        List<Clause> clauses = new ArrayList<Clause>();
        for (Pair<Clause, String> pair : PseudoPrologParser.read(fileReader)) {
            clauses.add(pair.r); // or transfer...
            if (pair.s.equals("+") || pair.s.equals("1.0")) {
                targets.add(1.0);
            } else {
                targets.add(0.0);
            }
        }
        return new Pair<>(clauses, targets);
    }

    public static MEDataset create(List<Clause> clauses, List<Double> targets, int subsumptionMode) {
        return new MEDataset(clauses, targets, subsumptionMode);
    }

    public Coverage subsumed(Clause clause) {
        if (queries == null) {
            return subsumed(clause, IntStream.range(0, examples.size()));
        }
        throw new UnsupportedOperationException("You do not use subsumption in case of queries given");
    }

    public Coverage subsumed(Clause clause, Coverage coverage) {
        return subsumed(clause, coverage.stream());
    }

    private Coverage subsumed(Clause clause, IntStream coverage) {
        return CoverageFactory.getInstance().get(getSubsumed(clause, coverage));
    }

    // use this for getting support if needed
    private Set<Integer> getSubsumed(Clause clause, IntStream intStream) {
        return Sugar.parallelStream(intStream, this.parallel)
                .filter(sampleIdx -> matches(clause, sampleIdx))
                .boxed()
                .collect(Collectors.toSet());
    }


    /**
     * Returns true iff the given clause subsumes the example. This can be used for example when we test whether a conjuction covers an interpretation.
     *
     * @param clause
     * @param sampleIdx
     * @return
     */
    public boolean matches(Clause clause, int sampleIdx) {
        return matches(clause, matchings.get(sampleIdx));
    }

    public boolean matches(Clause clause, Matching matching) {
        // because of the statefulness of the Clause implementation
        Clause candidate = this.parallel ? new Clause(clause.literals()) : clause;
        return matching.subsumption(candidate, 0);
    }

    public boolean matchesAtLeastOne(Clause conjunction, Coverage coverage) {
        if (null == queries) {
            if (parallel) {
                return Sugar.parallelStream(coverage.stream(), parallel)
                        .anyMatch(idx -> matches(conjunction, idx));
            } else {
                for (int idx = 0; idx < size(); idx++) {
                    if (matches(conjunction, idx)) {
                        return true;
                    }
                }
                return false;
            }
        }
        throw new IllegalStateException(); //NotImplementedException();
    }

    public boolean matchesAtLeastOne(Clause conjunction) {
        return matchesAtLeastOne(conjunction, CoverageFactory.getInstance().get(size()));
    }

    public MEDataset subDataset(Coverage parentsCoverage) {
        if (null == queries) {
            return new MEDataset(parentsCoverage.stream().sorted().mapToObj(idx -> examples.get(idx)).collect(Collectors.toList()),
                    parentsCoverage.stream().sorted().mapToObj(idx -> targets[idx]).collect(Collectors.toList()),
                    substitutionType);
        } else {
            return new MEDataset(examples.stream().map(c -> Clause.parse(c.toString())).collect(Collectors.toList()),
                    parentsCoverage.stream().sorted().mapToObj(idx -> queries.get(idx)).collect(Collectors.toList()),
                    parentsCoverage.stream().sorted().mapToObj(idx -> targets[idx]).collect(Collectors.toList()),
                    substitutionType);
        }
    }

    public MEDataset deepCopy(int substitutionType) {
        return new MEDataset(examples.stream().map(c -> Clause.parse(c.toString())).collect(Collectors.toList()),
                (queries == null) ? null : queries.stream().map(l -> Literal.parseLiteral(l.toString())).collect(Collectors.toList()),
                Arrays.stream(targets).boxed().collect(Collectors.toList()),
                substitutionType);
    }

    @Override
    public DatasetInterface flatten(int subsumptionMode) {
        if (null == queries) {
            return deepCopy(subsumptionMode);
        }
        return new MEDataset(
                queries.stream().map(literal -> new Clause(Sugar.iterable(Sugar.list(literal), examples.get(0).literals()))).collect(Collectors.toList()),
                Arrays.stream(targets).boxed().collect(Collectors.toList()),
                subsumptionMode);
    }

    private List<Literal> headAvoidingLiterals(Literal query, Clause clause) {
        List<Literal> lits = new ArrayList<>();
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

    public List<String> asOutput() {
        if (queries == null) {
            return IntStream.range(0, examples.size()).mapToObj(idx ->
                    (targets[idx] < 0.5 ? "-" : "+") + " "
                            + examples.get(idx).toString()
            ).collect(Collectors.toList());
        }
        throw new IllegalStateException(); //NotImplementedException();
    }

}
