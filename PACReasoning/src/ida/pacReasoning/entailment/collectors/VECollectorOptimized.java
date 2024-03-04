package ida.pacReasoning.entailment.collectors;

import ida.ilp.logic.*;
import ida.pacReasoning.data.RenamingFactory;
import ida.utils.Sugar;
import ida.utils.collections.DoubleCounters;
import ida.utils.tuples.Triple;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Note that constantsSize (# of constantsSize in the evidence) is inherited from parent in case of a cut -- so that gamma cut is still consistent :))
 * Also note that there is no check on whether constants from votes is a subset of constants from evidence (as this is the underlying assumption).
 * <p>
 * <p>
 * Created by martin.svatos on 7. 6. 2018.
 */
public class VECollectorOptimized implements EntailedOptimized {

    private final DoubleCounters<Triple<Integer, Integer, Integer>> votes;

    // this is more like a prediction part, asking only for data and cuts
    public VECollectorOptimized(DoubleCounters<Triple<Integer, Integer, Integer>> votes) {
        this.votes = votes;
    }

    @Override
    public Double getEntailedValue(Triple<Integer, Integer, Integer> query) {
        //return this.votes.get(query);
        if (votes.keySet().contains(query)) {
            return votes.get(query);
        }
        return null;
    }

    @Override
    public String asOutput() {
        throw new IllegalStateException();// NotImplementedException();
    }


    public static VECollectorOptimized load(Path file) {
        return load(file, null, null);
    }

    public static VECollectorOptimized load(Path file, RenamingFactory entityRenaming, RenamingFactory relationRenaming) {
        DoubleCounters<Triple<Integer, Integer, Integer>> votes = new DoubleCounters<>();
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
                            if (splitted[0].equals("true")) {
                                //allTimeEvidence.add(literal);
                            } else {
                                try {
                                    Integer.parseInt(literal.get(0).name());
                                } catch (Exception e) {
                                    // we have to use renaming factories
                                    literal = new Literal(relationRenaming.get(literal.predicate()),
                                            Sugar.list(Constant.construct(entityRenaming.get(literal.get(0).name())),
                                                    Constant.construct(entityRenaming.get(literal.get(1).name()))));
                                }
                                Triple<Integer, Integer, Integer> triple = new Triple<>(Integer.parseInt(literal.get(0).name()),
                                        Integer.parseInt(literal.predicate()),
                                        Integer.parseInt(literal.get(1).name()));
                                votes.add(triple, Double.parseDouble(splitted[1]));
                            }
                        } else {
                            //allTimeEvidence.addAll(LogicUtils.loadEvidence(line));
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new VECollectorOptimized(votes);
    }

    public DoubleCounters<Triple<Integer, Integer, Integer>> getVotes() {
        return votes;
    }
}
