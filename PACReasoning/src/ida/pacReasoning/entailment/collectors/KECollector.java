package ida.pacReasoning.entailment.collectors;

import ida.ilp.logic.Literal;
import ida.pacReasoning.entailment.Subset;
import ida.utils.Sugar;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 7. 6. 2018.
 */
public class KECollector implements Entailed {

    private final Collection<Literal> evidence;
    private final Set<Literal> entailed;
    private long time;

    private KECollector(Collection<Literal> evidence) {
        this.evidence = evidence;
        this.entailed = ConcurrentHashMap.newKeySet();
    }

    public Set<Literal> getEntailed() {
        return entailed;
    }

    public KECollector setTime(long time) {
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

    @Override
    public void add(Literal query, Subset subset) {
        this.entailed.add(query);
    }

    @Override
    public String asOutput() {
        return Sugar.union(entailed, evidence).stream()
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public Entailed removeK(int k) {
        return this;
    }




    public static KECollector create(Collection<Literal> evidence) {
        return new KECollector(evidence);
    }

}
