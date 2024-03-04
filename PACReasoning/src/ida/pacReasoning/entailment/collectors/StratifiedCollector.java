package ida.pacReasoning.entailment.collectors;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;


import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by martin.svatos on 12. 2. 2019.
 */
public class StratifiedCollector implements Entailed {
    private final Set<Literal> evidence;
    private final int k;
    private final Map<Literal, Double> memory;
    private long time;
    private String mp;

    public StratifiedCollector(Set<Literal> evidence, int k) {
        this.evidence = evidence;
        this.k = k;
        this.memory = new HashMap<>();
    }

    public StratifiedCollector setTime(long time) {
        this.time = time;
        return this;
    }

    @Override
    public long getTime() {
        return this.time;
    }

    @Override
    public Double getEntailedValue(Literal query) {
        throw new UnsupportedOperationException();
    }

    public void add(Literal literal, double ruleWeight) {
        if (!this.evidence.contains(literal) && (!memory.containsKey(literal) || memory.get(literal) < ruleWeight)) { // I'm storing max of ruleWeights
            memory.put(literal, ruleWeight);
        }
    }

    @Override
    public void add(Literal query, Subset subset) {
        throw new IllegalStateException();// NotImplementedException();
    }

    @Override
    public String asOutput() {
        StringBuilder sb = new StringBuilder();
        evidence.stream().map(Literal::toString).sorted().forEach(l -> sb.append("true\t-1.0\t" + l + "\n"));
        //memory.entrySet().stream().forEach(e -> sb.append("entailedByValue\t" + e.getValue() + "\t" + e.getKey() + "\n"));
        memory.entrySet().stream().map(e -> e.getValue() + "\t" + e.getKey()).sorted().forEach(s -> sb.append("entailedByValue\t" + s + "\n"));
        return sb.toString() + (null != this.mp ? ("\n------SPLIT HERE------\n" + this.mp): "");
    }

    @Override
    public Entailed removeK(int k) {
        return this;
    }

    // debug

    public Map<Literal, Double> getMemory() {
        return memory;
    }

    public void setMP(String str){
        this.mp = str;
    }
}
