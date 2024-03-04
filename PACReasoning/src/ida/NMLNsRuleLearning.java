package ida;

import ida.ilp.logic.Clause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/*
 * Created by martin.svatos on 14. 2. 2022.
 */

public class NMLNsRuleLearning {

    public static void main(String[] args) throws IOException {
        System.out.println("executing NMLNsRuleLearning with parameters");
        Arrays.stream(args).forEach(s -> System.out.println("\t" + s));
        Path folDataset = Paths.get(args[0]);
        Path store = Paths.get(args[1]);
        Path currentRules = Paths.get(args[2]);

        List<Clause> dataset = Files.lines(folDataset).filter(l -> !l.trim().isEmpty())
                .map(l -> Clause.parse(l))
                .collect(Collectors.toList());

        Set<IsoClauseWrapper> forbidden = Files.lines(folDataset).filter(l -> !l.trim().isEmpty())
                .map(l -> parseNmlnsRule(l))
                .map(IsoClauseWrapper::create)
                .collect(Collectors.toSet());

        if (false) {
            System.out.println("dataset is");
            dataset.forEach(l -> System.out.println("\t" + l));
            System.out.println("forbidden is");
            forbidden.forEach(l -> System.out.println("\t" + l));
        }

        int depth = 3;
        int beamSize = 10;
        int rounds = 5;
        int maxVariables = 2 * rounds;

        System.out.println("for now, just debug data1");
//        SubsampledDataset subsampled = SubsampledDataset.create()
//
//        Pipeline.runHornLearner(null, Sugar.set(), subsampled, beamSize, depth, rounds, maxVariables, target, true);

        List<Pair<Double, String>> learnedRules = Sugar.list(new Pair<>(1.0, "single(X, Y) -> single(Y, X)"),
                new Pair<>(0.9, "c(X) -> single(Y, X)"),
                new Pair<>(0.8, "single(X, Y) -> double(Y, X)"),
                new Pair<>(0.7, "double(X, Y) -> c(X)")
        );


        List<Pair<Double, String>> result = learnedRules.stream()
                .filter(pair -> !forbidden.contains(IsoClauseWrapper.create(parseNmlnsRule((pair.s)))))
                .collect(Collectors.toList());

        Files.write(store,
                Sugar.list(result.stream()
                        .map(p -> p.getR() + "\t:\t" + p.getS())
                        .collect(Collectors.joining("\n"))));
    }

    private static Clause parseNmlnsRule(String line) {
        String delimiter = " -> ";
        Set<Literal> literals = Sugar.set();
        if (line.contains(delimiter)) {
            String[] splitted = line.split(delimiter, 2);
            literals.add(Literal.parseLiteral(splitted[1]));
            line = splitted[0];
        }
        literals.addAll(LogicUtils.flipSigns(Clause.parse(line)).literals());
        return new Clause(literals);
    }
}
