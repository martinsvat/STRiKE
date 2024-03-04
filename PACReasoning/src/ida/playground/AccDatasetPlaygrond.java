package ida.playground;

import ida.ilp.logic.Literal;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Quadruple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Handling dataset reformatting for ACC version (like in https://github.com/dddoss/tensorflow-socher-ntn)
 * <p>
 * Created by martin.svatos on 9. 11. 2021.
 */
public class AccDatasetPlaygrond {


    public static <T> void main(String[] args) throws IOException {
        String dataset = "Freebase";
        Path base = Paths.get("C:\\data\\school\\development\\datasetAcc\\tensorflow-socher-ntn\\data\\" + dataset + "\\");
        Path trainPath = Paths.get(base.toString(), "train.txt");
        Path testPath = Paths.get(base.toString(), "test.txt");
        Path validPath = Paths.get(base.toString(), "dev.txt");

        Set<String> entities = Sugar.set();
        Set<String> relations = Sugar.set();

        List<Quadruple<String, String, String, Boolean>> trainTriples = loadTriples(trainPath, entities, relations, false);
        List<Quadruple<String, String, String, Boolean>> testTriples = loadTriples(testPath, entities, relations, true);
        List<Quadruple<String, String, String, Boolean>> validTriples = loadTriples(validPath, entities, relations, true);


        Path anyBurlOutputBase = Paths.get("C:\\data\\school\\development\\anyburl\\data", "acc" + dataset, "anyBurlTriple");
        if (!anyBurlOutputBase.toFile().exists()) {
            anyBurlOutputBase.toFile().mkdirs();
        }

        Files.write(Paths.get(anyBurlOutputBase.toString(), "train.nl"),
                trainTriples.stream().map(q -> q.r + "\t" + q.s + "\t" + q.t).collect(Collectors.toList()));
        Files.write(Paths.get(anyBurlOutputBase.toString(), "test.nl"),
                testTriples.stream().map(q -> q.r + "\t" + q.s + "\t" + q.t).collect(Collectors.toList()));
        Files.write(Paths.get(anyBurlOutputBase.toString(), "dev.nl"),
                validTriples.stream().map(q -> q.r + "\t" + q.s + "\t" + q.t).collect(Collectors.toList()));

        Path output = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets", "acc" + dataset);
        if (!output.toFile().exists()) {
            output.toFile().mkdirs();
        }
        Files.write(Paths.get(output.toString(), "train.logic.nl"),
              trainTriples.stream().map(q -> Literal.parseLiteral(q.s.trim() + "(" + q.r.trim() + "," + q.t + ")").toString()).collect(Collectors.toList()));
        Files.write(Paths.get(output.toString(), "test.logic.nl"),
                testTriples.stream().map(q -> q.u + "\t" + Literal.parseLiteral(q.s.trim() + "(" + q.r.trim() + "," + q.t + ")")).collect(Collectors.toList()));
        Files.write(Paths.get(output.toString(), "dev.logic.nl"),
                validTriples.stream().map(q -> q.u + "\t" + Literal.parseLiteral(q.s.trim() + "(" + q.r.trim() + "," + q.t + ")")).collect(Collectors.toList()));


        Files.write(Paths.get(output.toString(), "train.nl"),
                trainTriples.stream().map(q -> q.r + "\t" + q.s + "\t" + q.t).collect(Collectors.toList()));
        Files.write(Paths.get(output.toString(), "test.nl"),
                testTriples.stream().map(q -> q.r + "\t" + q.s + "\t" + q.t + "\t" + q.u).collect(Collectors.toList()));
        Files.write(Paths.get(output.toString(), "dev.nl"),
                validTriples.stream().map(q -> q.r + "\t" + q.s + "\t" + q.t + "\t" + q.u).collect(Collectors.toList()));


        for (Pair<String, Set<String>> targetValues : Sugar.list(new Pair<>("entities.dict", entities), new Pair<>("relations.dict", relations))) {
            List<String> values = Sugar.listFromCollections(targetValues.getS());
            Collections.sort(values);
            Files.write(Paths.get(output.toString(), targetValues.r),
                    IntStream.range(0, values.size())
                            .mapToObj(i -> i + "\t" + values.get(i))
                            .collect(Collectors.toList())
            );
        }
    }

    private static List<Quadruple<String, String, String, Boolean>> loadTriples(Path path, Set<String> entitiesCollector, Set<String> relationsCollector, boolean readSing) throws IOException {
        // return triple subject, predicate, object (as in file) + true/false if it is positive example or not
        return Files.lines(path).map(line -> {
            String[] split = line.split("\\s+");
            entitiesCollector.add(split[0]);
            entitiesCollector.add(split[2]);
            relationsCollector.add(split[1]);
            return new Quadruple<>(split[0], split[1], split[2], readSing ? split[3].equals("1") : true);
        }).collect(Collectors.toList());
    }
}
