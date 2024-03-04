package ida.pacReasoning.entailment.collectors;

import ida.ilp.logic.*;
import ida.pacReasoning.entailment.Subset;
import ida.pacReasoning.entailment.cuts.Cut;
import ida.utils.Sugar;
import ida.utils.collections.DoubleCounters;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Note that constantsSize (# of constantsSize in the evidence) is inherited from parent in case of a cut -- so that gamma cut is still consistent :))
 * Also note that there is no check on whether constants from votes is a subset of constants from evidence (as this is the underlying assumption).
 * <p>
 * <p>
 * Created by martin.svatos on 7. 6. 2018.
 */
public class VECollector implements Entailed {

    private final Set<Literal> evidence;
    private final Integer k;
    private final DoubleCounters<Literal> votes;
    private final Map<Pair<Integer, Integer>, Double> precomputedVotes;
    private final int constantSize;
    private final Map<Integer, Double> cachedCtoX = new ConcurrentHashMap<>();
    private final Set<Constant> constants;
    private final Integer originalEvidenceSize; // tohle se musi dodelat, ale muze se to tam plnit i jinak nez predavanim pri konstrukci, napr pomoci prepocitani a drzeni jine hodnoty nez -1.0 u entailovanych pomoci hard rules
    private long time;

    protected VECollector(Set<Literal> evidence, DoubleCounters<Literal> votes, int k, int constantSize, Integer originalEvidenceSize) {
        this(evidence, votes, k, constantSize, LogicUtils.constants(new Clause(evidence)), originalEvidenceSize);
    }

    // this is more like a prediction part, asking only for data and cuts
    private VECollector(Set<Literal> evidence, DoubleCounters<Literal> votes, int k, int constantSize, Set<Constant> constants, Integer originalEvidenceSize) {
        this.evidence = evidence;
        this.k = k;
        this.votes = votes;
        //this.precomputedVotes = null;
        this.precomputedVotes = null;
        this.constantSize = constantSize;
        this.constants = constants;
        this.originalEvidenceSize = originalEvidenceSize;
    }

    private VECollector(Set<Literal> evidence, int k, Map<Pair<Integer, Integer>, Double> precomputedVotes, int constantSize, Integer originalEvidenceSize) {
        this(evidence, k, precomputedVotes, constantSize, LogicUtils.constants(new Clause(evidence)), originalEvidenceSize);
    }

    // this is more like a inference part, storing the data while computing kX-entailment
    private VECollector(Set<Literal> evidence, int k, Map<Pair<Integer, Integer>, Double> precomputedVotes, int constantSize, Set<Constant> constants, Integer originalEvidenceSize) {
        this.evidence = evidence;
        this.k = k;
        this.votes = new DoubleCounters<>();
        this.precomputedVotes = precomputedVotes;
        this.constantSize = constantSize;
        this.constants = constants;
        this.originalEvidenceSize = originalEvidenceSize;
    }

    public VECollector setTime(long time) {
        this.time = time;
        return this;
    }

    @Override
    public long getTime() {
        return this.time;
    }

    @Override
    public Double getEntailedValue(Literal query) {
        //return this.votes.get(query);
        if (votes.keySet().contains(query)) {
            return votes.get(query);
        }
        return null;
    }

    public Set<Literal> getEvidence() {
        return evidence;
    }

    public int originalEvidenceSize() {
        return originalEvidenceSize;
    }

    public DoubleCounters<Literal> getVotes() {
        return votes;
    }

    @Override
    public void add(Literal query, Subset subset) {
        Set<Term> terms = query.terms();
        int a = terms.size();
        int usedConstants = subset.size();

        double impliedSuccessors = precomputedVotes.get(new Pair<>(usedConstants, a));
        // if this is the bottleneck, then try to split literals to according to predicates to lower the synchronization need
//        System.out.println("ents\t" + query + "\t" + impliedSuccessors + "\ts=" + usedConstants + "\ta=" + a + "\tc=" + this.constantsSize);
//        if (subset.size() > 4) {
//            System.out.println("entailed\t" + query + "\t" + impliedSuccessors + "\t" + subset.hashCode());
//        }
        synchronized (votes) {
//            System.out.println("entailed\t" + query + "\t" + impliedSuccessors + "\t" + subset.hashCode());
            votes.add(query, impliedSuccessors);
        }
    }

    @Override
    public String asOutput() {
        return evidence.stream()
                .map(literal -> "true\t-1.0\t" + literal)
                .collect(Collectors.joining("\n"))
                + "\n"
                + votes.keySet().stream()
                .filter(literal -> !evidence.contains(literal))
                .map(literal -> "false\t" + votes.get(literal) + "\t" + literal)
                .sorted()
                .collect(Collectors.joining("\n"));
    }

    @Override
    public Entailed removeK(int k) {
        if (k == this.k) {
            return this;
        }
        throw new IllegalStateException("given k is not the same as my:\t" + k + "\tvs my\t" + this.k);
    }

    public int constantsSize() {
        return constantSize;
    }

    public Set<Constant> getConstants() {
        return constants;
    }

    public Integer getK() {
        return k;
    }

    public double getCToX(int a) {
        return getCToX(a, this.k);
    }

    public double getCToX(int a, int k) {
        Double val = cachedCtoX.get(k - a);
        if (null == val) {
            // more granular way
//            val = 1.0;
//            for (int i = this.constantsSize - k + a; i <= this.constantsSize; i++) {
//                val *= i;
//            }
            // according to UAI paper
            val = Math.pow(constantSize, k - a);
            cachedCtoX.put(k - a, val);
        }
//        System.out.println(a+"\t" + val);
        return val;
    }

    public static VECollector create(Collection<Literal> evidence, int k) {
        return create(evidence, k, k);
    }

    public static VECollector create(Collection<Literal> evidence, int k, int maxArity) {
        Set<Literal> set = Sugar.setFromCollections(evidence);
        int constants = LogicUtils.constants(new Clause(evidence)).size();
        Map<Pair<Integer, Integer>, Double> precomputedVotes = computeVotes(constants, k, maxArity);
        return new VECollector(set, k, precomputedVotes, constants, null);
    }

    private static Map<Pair<Integer, Integer>, Double> computeVotes(int constants, int k, int maxArity) {
        // s... size of subset
        // c... constantsSize
        // a... unique constantsSize in entailed literal
        Map<Integer, BigDecimal> cachedFactorial = new HashMap<>(); // f(x) = (x)!
        cachedFactorial.put(0, BigDecimal.ONE);
        Map<Integer, BigDecimal> product = new HashMap<>(); // f(s) = \product_{i=c-k+1}^{c-s} i

        BigDecimal factorial = BigDecimal.ONE;
        for (int i = 1; i <= k; i++) {
            factorial = factorial.multiply(BigDecimal.valueOf(i));
            cachedFactorial.put(i, factorial);
        }

        for (int s_i = 1; s_i <= k; s_i++) {
            BigDecimal val = BigDecimal.ONE;
            for (int i = constants - k + 1; i <= constants - s_i; i++) {
                val = val.multiply(BigDecimal.valueOf(i));
            }
            product.put(s_i, val);
        }

        Map<Pair<Integer, Integer>, Double> retVal = new HashMap<>();
        for (int s = 1; s <= k; s++) {
            for (int a = 1; a <= Math.min(s, maxArity); a++) {
                // add precision here to avoid java.lang.ArithmeticException: Non-terminating decimal expansion
                double partialVotes = cachedFactorial.get(s - a).multiply(product.get(s)).doubleValue();
                retVal.put(new Pair<>(s, a), partialVotes);
            }
        }
        return retVal;
    }

    public static VECollector load(Pair<Path, Integer> pair) {
        return load(pair.r, pair.s);
    }


    public static VECollector load(Path file, int k) {
        return load(file, k, (literal) -> true);
    }

    public static VECollector load(Path file, int k, java.util.function.Predicate<Literal> literalsFilter) {
        Set<Literal> allTimeEvidence = Sugar.set();
        DoubleCounters<Literal> votes = new DoubleCounters<>();
        System.out.println("loading\t" + file);
        try {
            Files.readAllLines(file)
                    .stream()
                    .filter(line -> !line.startsWith("#") && line.trim().length() > 0)
                    .forEach(line -> {
                        if (line.startsWith("true") || line.startsWith("false") || line.startsWith("entailedByValue")) { // the last case is for weight in output of PL
                            String[] splitted = line.split("\t", 3);
                            if (splitted.length != 3) {
                                throw new IllegalStateException();
                            }
                            Literal literal = Sugar.chooseOne(LogicUtils.loadEvidence(splitted[2]));

                            if (literalsFilter.test(literal)) {
                                if (splitted[0].equals("true")) {
                                    allTimeEvidence.add(literal);
                                } else {
                                    votes.add(literal, Double.parseDouble(splitted[1]));
                                }
                            }

                        } else {
                            allTimeEvidence.addAll(LogicUtils.loadEvidence(line));
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        String name = file.toFile().getName();
        if (name.startsWith("queries")) {
            name = name.substring("queries".length());
        }
        if (name.contains(".db")) {
            name = name.substring(0, name.indexOf(".db"));
        }

        int evidenceSize = 0;
        try {
            evidenceSize = Integer.parseInt(name);
        } catch (Exception e) {
            //System.out.println("the parsing of data is not bulletproof, only queries123.db or 123.db is allowed");
            if (true) {
                evidenceSize = 0;
            } else {
                e.printStackTrace();
            }
        }

        // better be safe than sorry -- no occurrence of a literal in both evidence and predicted literals
        for (Literal literal : allTimeEvidence) {
            if (votes.keySet().contains(literal)) {
                votes.remove(literal);
            }
        }
        return new VECollector(allTimeEvidence, votes, k, LogicUtils.constants(new Clause(allTimeEvidence)).size(), evidenceSize);
    }

    public Set<Literal> cut(Cut cut) {
        Set<Literal> retVal = Sugar.set();
        retVal.addAll(evidence);
        retVal.addAll(Sugar.parallelStream(this.votes.keySet())
                .filter(key -> cut.isEntailed(key, this.votes.get(key), this.constantSize)).collect(Collectors.toSet()));
        return retVal;
    }

    /**
     * Note that constantsSize (# of constantsSize in the evidence) is inherited from parent in case of a cut -- so that gamma cut is still consistent :))
     * <p>
     * Cut evidence=true is important only for predicate cut or constant like filter :))
     *
     * @param cut
     * @param cutHardEvidence
     * @return
     */
    public VECollector cutCollector(Cut cut, boolean cutHardEvidence) {
        Set<Literal> evd = cutHardEvidence
                ? evidence.stream().filter(literal -> cut.isEntailed(literal, -1, this.constantSize)).collect(Collectors.toSet())
                : evidence;

        VECollector vots = cut.entailed(this);
//        DoubleCounters<Literal> vots = new DoubleCounters<>();
//        this.votes.keySet().stream()
//                .filter(literal -> cut.isEntailed(literal, this.votes.get(literal), this.constantSize))
//                .forEach(literal -> vots.add(literal, this.votes.get(literal)));
        return new VECollector(evd, vots.votes, this.k, this.constantSize, this.constants, this.originalEvidenceSize()); // this.constantsSize je tady dulezite
    }

    public static VECollector create(Set<Literal> evidence, DoubleCounters<Literal> votes, int k, int constantSize, Set<Constant> constants, Integer originalEvidenceSize) {
        return new VECollector(evidence, votes, k, constantSize, constants, originalEvidenceSize);
    }


    public VECollector untype() {
        Set<Literal> evd = LogicUtils.untype(this.evidence);
        DoubleCounters<Literal> untypedVotes = new DoubleCounters<>();
        for (Map.Entry<Literal, Double> entry : this.votes.entrySet()) {
            untypedVotes.addPost(LogicUtils.untype(entry.getKey()), entry.getValue());
        }
        return new VECollector(evd, untypedVotes, this.k, LogicUtils.constants(new Clause(evd)).size(), evd.size());
    }

    public EntailedOptimized toOptimized() {
        DoubleCounters<Triple<Integer, Integer, Integer>> tripleVotes = new DoubleCounters<>();
        votes.keySet().forEach(literal -> tripleVotes.add(new Triple<>(Integer.parseInt(literal.get(0).name()),
                        Integer.parseInt(literal.predicate()),
                        Integer.parseInt(literal.get(1).name())),
                votes.get(literal)));
        return new VECollectorOptimized(tripleVotes);
    }
}
