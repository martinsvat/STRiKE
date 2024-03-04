package ida.gnns;

import ida.ilp.logic.Constant;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.utils.Sugar;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by martin.svatos on 5. 3. 2021.
 */
public class ComparatorUtils {
    public static VECollector filterForTest(Set<Literal> evidence, Set<Literal> valid, Set<Literal> test, Path path) {
        return filterForTest(evidence, valid, test, VECollector.load(path, 1));
    }

    public static VECollector filterForTest(Set<Literal> evidence, Set<Literal> valid, Set<Literal> test, VECollector collector) {
        VECollector retVal = VECollector.create(evidence, 1);
        Set<Literal> all = Sugar.union(evidence, valid, test);
        Set<Constant> constants = LogicUtils.constants(all);

        Set<Literal> relevantCorrupted = Sugar.set();
        for (Literal literal : test) {
            for (Literal corrupted : corrupted(literal, constants)) {
                if (evidence.contains(corrupted) || valid.contains(corrupted)) {
                    continue;// filtered
                }
                if (collector.getVotes().keySet().contains(corrupted)) {
                    //retVal.getVotes().add(corrupted, collector.getVotes().get(corrupted));
//                    if (collector.getVotes().keySet().contains(corrupted)) {
//                        System.out.println("bug here!");
//                    }
                    relevantCorrupted.add(corrupted);
                }
            }
        }

        for (Literal corrupted : relevantCorrupted) {
            retVal.getVotes().add(corrupted, collector.getVotes().get(corrupted));
        }

        return retVal;
    }

    private static List<Literal> corrupted(Literal literal, Set<Constant> constants) {
        List<Literal> retVal = Sugar.list();
        for (Constant constant : constants) {
            retVal.add(new Literal(literal.predicate(), literal.get(0), constant));
            retVal.add(new Literal(literal.predicate(), constant, literal.get(1)));
        }
        return retVal;
    }

    public static VECollector normalize(VECollector collector) {
        VECollector retVal = VECollector.create(Sugar.set(), 1);
        /*List<Double> values = collector.getVotes().entrySet().stream()
                .filter(entry -> !collector.getEvidence().contains(entry.getKey()))
                .map(entry -> entry.getValue())
                .collect(Collectors.toList());
        */
        List<Double> values = Sugar.list();
        for (Map.Entry<Literal, Double> entry : collector.getVotes().entrySet()) {
            if (!collector.getEvidence().contains(entry.getKey())) {
                values.add(entry.getValue());
            }
        }
        if (values.isEmpty()) {
            return retVal;
        }
        double min = values.stream().mapToDouble(d -> d).min().orElse(0);
        double max = values.stream().mapToDouble(d -> d).max().orElse(0);

//        collector.getVotes().entrySet().stream()
//                .filter(e -> !collector.getEvidence().contains(e.getKey()))
//                .forEach(e -> retVal.getVotes().add(e.getKey(), normalize(min, max, e.getValue())));
        for (Map.Entry<Literal, Double> entry : collector.getVotes().entrySet()) {
            if (!collector.getEvidence().contains(entry.getKey())) {
                retVal.getVotes().add(entry.getKey(), normalize(min, max, entry.getValue()));
                //System.out.println(1.0 * retVal.getVotes().size() / collector.getVotes().size());
            }
        }

        return retVal;
    }

    private static double normalize(double min, double max, Double value) {
        return (value - min) / (max - min);
    }
}
