package ida.pacReasoning.evaluation;

import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.utils.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 18. 6. 2018.
 */
public class Data<X, Y> {

    private final List<Pair<X, Y>> data;
    private final String name;
    private String attributes;

    public Data(Stream<Pair<X, Y>> data, String name) {
        this(data.collect(Collectors.toList()), name);
    }

    public Data(List<Pair<X, Y>> data, String name) {
        this.data = data;
        this.name = name;
        this.attributes = "";
    }

    public String getName() {
        return name;
    }

    public List<Pair<X, Y>> getData() {
        return data;
    }

    public int size() {
        return this.data.size();
    }


    public Data<X, Y> sublist(int fromInclusive, int toExclusive, String name) {
        return new Data<>(data.subList(fromInclusive, toExclusive), name);
    }


    // that is not much of a nice to have such method in generics... rather move it to utils or something like that
    public static Data<Integer, VECollector> loadResults(Pair<Path, Integer> pair) {
        return loadResults(pair.r, pair.s);
    }

    public static Data<Integer, VECollector> loadResults(Path path, int k) {
        if (!path.toFile().exists()) {
            System.out.println("file not found... should be file not found exception ;)");
            System.out.println("could not found\t" + path);
            throw new IllegalArgumentException();
        }
//        System.out.println("loading from\t" + path.toFile().isDirectory() + "\t" + path);
        return path.toFile().isDirectory() ? loadDirectory(path, k, (p) -> true, true) : loadFile(path, k);
    }

    private static Data<Integer, VECollector> loadFile(Path file, int k) {
        try {
            List<Set<Literal>> list = Files.lines(file)
                    .parallel()
                    .filter(line -> line.trim().length() > 0)
                    .map(line -> LogicUtils.loadEvidence(line.substring(1, line.length() - 1)))
                    .collect(Collectors.toList());
            List<Pair<Integer, VECollector>> data = IntStream.range(0, list.size())
                    .mapToObj(idx -> new Pair<>(idx + 1, VECollector.create(list.get(idx), k)))
                    .collect(Collectors.toList());
            return new Data<>(data, file.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    public static Data<Integer, Double> loadDirectoryExecutionTime(Path path) {
        Utils utils = Utils.create();
        List<Pair<Integer, Double>> retVal = null;
        try {
            Stream<Pair<Integer, Path>> stream = Files.list(path)
                    .filter(f -> f.toFile().getName().endsWith(".time"))
                    .map(f -> new Pair<>(utils.evidenceFileNumber(f), f));
            retVal = stream.sorted(Comparator.comparingInt(Pair::getR))
                    .map(p -> {
                        try {
                            String line = Files.lines(p.s).collect(Collectors.toList()).get(0);
                            double time = Long.parseLong(line) / (1000000000.0);
                            return new Pair<>(p.r, time);
                        } catch (IOException e) {
                            return null;
                        }
                    }).filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Data<>(retVal, path.toFile().getName());
    }


    public static Data<Integer, VECollector> loadDirectory(Path path, int k, Predicate<Pair<Integer, Path>> filter, boolean checkConsequent) {
        Utils utils = Utils.create();
        List<Pair<Integer, VECollector>> retVal = null;
        try {
//            System.out.println("debug pro data load, tady oriznuti na pouze nekolik evidenci aby se to lepe debugovalo");
            Stream<Pair<Integer, Path>> stream = Files.list(path)
                    .filter(f -> f.toFile().getName().endsWith(".db"))
                    .map(f -> new Pair<>(utils.evidenceFileNumber(f), f));
            if (null != filter) {
                stream = stream.filter(filter);
            }
//                    .filter(p -> 27 == p.randomGenerator) // debug
            retVal = stream.sorted(Comparator.comparingInt(Pair::getR))
                    .map(p -> new Pair<>(p.r, VECollector.load(p.s, k)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        if (true) {
//            System.out.println("dalsi debug, tenhle if odmazat");
//            return new Data<>(retVal, path.toFile().getName());
//        }

        if (!checkConsequent) {
            return new Data<>(retVal, path.toFile().getName());
        }

        int idx = 0;
        for (; idx < retVal.size(); idx++) {
            if (retVal.get(idx).r != idx + 1) {
                break;
            }
        }

        return new Data<>(retVal.subList(0, idx), path.toFile().getName());
    }

    public static Data<Integer, VECollector> loadResultsSubPart(Path path, Integer k, int from, int toExclusive) {
        List<Pair<Integer, VECollector>> retVal = null;
        Utils utils = Utils.create();
        try {
//            System.out.println("debug pro data load, tady oriznuti na pouze nekolik evidenci aby se to lepe debugovalo");
            retVal = Files.list(path)
                    .filter(f -> f.toFile().getName().endsWith(".db"))
                    .map(f -> new Pair<>(utils.evidenceFileNumber(f), f))
                    .filter(p -> p.r >= from && p.r < toExclusive)
                    .sorted(Comparator.comparingInt(Pair::getR))
                    .map(p -> new Pair<>(p.r, VECollector.load(p.s, k)))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (int idx = 1; idx < retVal.size(); idx++) {
            if (retVal.get(idx - 1).r + 1 != retVal.get(idx).r || retVal.get(idx).r != idx + from) {
                System.out.println("there is a gap in the data");
                return null;
            }
        }

        return new Data<>(retVal, path.toFile().getName());

    }

    public void setAttributes(String attributes) {
        this.attributes = attributes;
    }

    public String getAttributes() {
        return attributes;
    }
}
