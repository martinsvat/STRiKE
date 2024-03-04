package ida.pacReasoning.data;

import ida.ilp.logic.*;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 19. 7. 2018.
 */
public class DatasetSampler {

    public static final int SNOWBALL = 1;
    public static final int EDGE_SAMPLING = 2;
    public static final int RANDOM_WALK = 3;
    public static final int VERTEX_SAMPLING = 4;
    public static final int UNIFORM_RANDOM = 5;
    public static final int RANDOM_WALK_WITH_FULL_EVIDENCE = 6;

    private final Random random;
    private final int mode;
    private final int snowballK;
    private final boolean extend; // workaround for adding symmetricity of interaction, etc.
    private Utils utils = Utils.create();
    private final boolean rewrite;

    public DatasetSampler(int mode, int snowballK, boolean extend, boolean rewrite) {
        this(mode, snowballK, extend, rewrite, new Random());
    }

    public DatasetSampler(int mode, int snowballK, boolean extend, boolean rewrite, Random random) {
        this.random = random;
        this.snowballK = snowballK;
        this.mode = mode;
        this.extend = extend;
        this.rewrite = rewrite;
        if (mode == EDGE_SAMPLING) {
            System.out.println("the needed number of constant does not have to meet the criterion");
        }
    }

    public Collection<Constant> sampleConstantFromEvidence(Set<Literal> evidence, int k) {
        return sampleConstant(LogicUtils.constants(new Clause(evidence)), k);
    }

    public <T> List<T> shuffle(Collection<T> collection) {
        List<T> list = Sugar.listFromCollections(collection);
        Collections.shuffle(list, random);
        return list;
    }

    public Collection<Constant> sampleConstant(Set<Constant> constants, int k) {
        return shuffle(constants).subList(0, Math.min(k, constants.size()));
    }

    public Stream<Pair<Integer, List<Set<Literal>>>> sample(Set<Integer> constantSizes, Path evidence, int times) {
        switch (mode) {
            case EDGE_SAMPLING:
            case RANDOM_WALK_WITH_FULL_EVIDENCE:
                return sample(constantSizes, LogicUtils.loadEvidence(evidence), times);
            case SNOWBALL:
                return sampleSnowball(constantSizes, LogicUtils.loadEvidence(evidence), times);
            default:
                throw new IllegalArgumentException("unknown mode (or not implemented):\t" + mode);
        }
    }

    private Stream<Pair<Integer, List<Set<Literal>>>> sampleSnowball(Set<Integer> constantSizes, Set<Literal> evidence, int times) {
        return Sugar.parallelStream(constantSizes)
                .map(size -> new Pair<>(size
                        , IntStream.range(0, times)
                        .mapToObj(idx -> snowball(evidence, size))
                        .collect(Collectors.toList()))
                );
    }

/*  old implementation
    private Set<Literal> snowball(Set<Literal> evidence, Integer constantsSize) {
        List<Constant> list = shuffle(LogicUtils.constants(new Clause(evidence)));
        Set<Constant> constants = Sugar.set(list.get(0));
        while (constants.size() != constantsSize) {
            // ta evidence by se tady dala pokazde ukrajovat, aby se to zrychlovalo
            Set<Constant> near = neighbor(constants, evidence);
            if (near.size() + constants.size() <= constantsSize) {
                constants.addAll(near);
            } else { // we use only predicates of arity 2, so it is okey (moreover)
                List<Constant> nearest = Sugar.listFromCollections(near);
                constants.addAll(nearest.subList(0, constantsSize - constants.size()));
            }
        }
        Set<Literal> retVal = utils.mask(evidence, constants);
        int check = LogicUtils.constants(new Clause(retVal)).size();
        if (check != constantsSize) {
            System.out.println("not returning the precise wanted size of constants\t" + constantsSize + "\t" + check);
        }
        return extend(retVal);
    }

    private Set<Constant> neighbor(Set<Constant> constants, Set<Literal> evidence) {
        return Sugar.parallelStream(evidence)
                .flatMap(literal -> {
                    Set<Constant> retVal = Sugar.set();
                    Set<Constant> arguments = LogicUtils.constantsFromLiteral(literal);
                    boolean containsAll = true;
                    boolean containsAtLeastOne = false;
                    for (Constant argument : arguments) {
                        if (constants.contains(argument)) {
                            containsAtLeastOne = true;
                        } else {
                            retVal.add(argument);
                            containsAll = false;
                        }
                    }
                    if (containsAll || !containsAtLeastOne) {
                        retVal.clear();
                    }
                    return retVal.stream();
                }).collect(Collectors.toSet());
    }
*/

    private Set<Literal> snowballLiterals(Set<Literal> evidence, Integer maxLiterals) {
        List<Constant> list = shuffle(LogicUtils.constants(new Clause(evidence)));
        // if there are only a little of constant wanted, less than the snowball (times 2, since most of the predicates are assumed to be binary) then sample just one constants as an epicenter
        Set<Constant> constants = Sugar.setFromCollections(list.subList(0, 1));
        Set<Constant> lastConstants = constants;
        Set<Literal> literals = Sugar.set();
        while (literals.size() < maxLiterals) {
            // ta evidence by se tady dala pokazde ukrajovat, aby se to zrychlovalo
            Set<Constant> near = neighbor(lastConstants, constants, evidence, snowballK);
            if (near.isEmpty()) {
                lastConstants = Sugar.set(shuffle(Sugar.collectionDifference(list, constants)).get(0));
                constants.addAll(lastConstants);
            } else {
                constants.addAll(near);
                lastConstants = near;
            }
            literals = utils.mask(evidence, constants);
        }

        return extend(Sugar.setFromCollections(shuffle(literals).subList(0, maxLiterals)));
    }


    private Set<Literal> snowball(Set<Literal> evidence, final Integer constantsSize) {
        List<Constant> list = shuffle(LogicUtils.constants(new Clause(evidence)));
        // if there are only a little of constant wanted, less than the snowball (times 2, since most of the predicates are assumed to be binary) then sample just one constants as an epicenter
        Set<Constant> constants = Sugar.setFromCollections(list.subList(0, snowballK * 4 >= constantsSize ? 1 : snowballK));
        Set<Constant> lastConstants = constants;
        while (LogicUtils.constants(new Clause(utils.mask(evidence, constants))).size() < constantsSize) {
            // ta evidence by se tady dala pokazde ukrajovat, aby se to zrychlovalo
            Set<Constant> near = neighbor(lastConstants, constants, evidence, snowballK);
            if (near.isEmpty()) {
                lastConstants = Sugar.set(shuffle(Sugar.collectionDifference(list, constants)).get(0));
                constants.addAll(lastConstants);
            } else if (near.size() + constants.size() <= constantsSize) {
                constants.addAll(near);
                lastConstants = near;
            } else { // we use only predicates of arity 2, so it is okey (moreover)
                List<Constant> shuffled = shuffle(near);
                int trueConstantSize = LogicUtils.constants(new Clause(utils.mask(evidence, constants))).size();
                List<Constant> nearest = shuffled.subList(0, Math.min(constantsSize - trueConstantSize, near.size()));
                constants.addAll(nearest);
                lastConstants = Sugar.setFromCollections(nearest);
            }
        }

        Set<Constant> realConstants = LogicUtils.constants(new Clause(utils.mask(evidence, constants)));
        if (realConstants.size() != constantsSize) { // the only possibility is that check > constantsSize
            for (int idx = 0; idx < realConstants.size(); idx++) {
                List<Constant> removed = Sugar.list();
                removed.addAll(realConstants);
                removed.remove(idx);

                int currentSize = LogicUtils.constants(new Clause(utils.mask(evidence, removed))).size();
                if (currentSize < realConstants.size() && currentSize >= constantsSize) {
                    constants = Sugar.setFromCollections(removed);
                    break;
                }
            }
        }

        Set<Literal> retVal = utils.mask(evidence, constants);
        Integer check = LogicUtils.constants(new Clause(retVal)).size();
        if (Integer.compare(check, constantsSize) != 0) {
            System.out.println("not returning the precise wanted size of constants\t" + constantsSize + "\t" + check);
        }
        return extend(retVal);
    }

    private Set<Constant> neighbor(Set<Constant> constants, Set<Constant> alreadyIn, Set<Literal> evidence, int snowballMax) {
        return Sugar.parallelStream(evidence)
                .flatMap(literal -> {
                    Set<Constant> retVal = Sugar.set();
                    Set<Constant> arguments = LogicUtils.constantsFromLiteral(literal);
                    boolean containsAll = true;
                    boolean containsAtLeastOne = false;
                    for (Constant argument : arguments) {
                        if (constants.contains(argument)) {
                            containsAtLeastOne = true;
                        } else if (!alreadyIn.contains(argument)) {
                            retVal.add(argument);
                            containsAll = false;
                        }
                    }
                    if (containsAll || !containsAtLeastOne) {
                        retVal.clear();
                    }
                    if (retVal.size() > snowballMax) {
                        return shuffle(retVal).subList(0, snowballMax).stream();
                    }
                    return retVal.stream();
                }).collect(Collectors.toSet());
    }

    public Stream<Pair<Integer, List<Set<Literal>>>> sample(Set<Integer> constantSizes, Set<Literal> evidence, int times) {
        return Sugar.parallelStream(constantSizes)
                .map(size -> new Pair<>(size
                        , IntStream.range(0, times)
                        //.mapToObj(idx -> extend(utils.mask(evidence, sampleConstantFromEvidence(evidence, size))))
                        .mapToObj(idx -> {
                            switch (mode) {
                                case EDGE_SAMPLING:
                                    return extend(sampleRandom(evidence, size));
                                case RANDOM_WALK_WITH_FULL_EVIDENCE:
                                    return extend(randomWalkWithFullEvidence(evidence, size));
                                default:
                                    throw new IllegalStateException("unknown type:\t" + mode);
                            }
                        })
                        .collect(Collectors.toList()))
                );
    }

    private Set<Literal> randomWalkWithFullEvidence(Set<Literal> evidence, final Integer constantSize) {
        Set<Literal> selected = randomWalkWithConstantsLimit(evidence, constantSize);
        return utils.mask(evidence, LogicUtils.constants(new Clause(selected)));
    }

    private Set<Literal> sampleRandom(Set<Literal> evidence, final int constantSize) {
        //System.out.println("goal\t" + constantSize);
        // eager, naive sampling
        Set<Constant> selectedConstants = Sugar.set();

        List<Literal> literals = shuffle(evidence);
        for (Literal literal : literals) {
            Set<Constant> currentConstants = LogicUtils.constantsFromLiteral(literal);
            int in = 0;
            for (Constant currentConstant : currentConstants) {
                if (!selectedConstants.contains(currentConstant)) {
                    in++;
                }
            }

            if (in + selectedConstants.size() <= constantSize) {
                selectedConstants.addAll(currentConstants);
            }

            if (selectedConstants.size() == constantSize) {
                break;
            }
        }
        return utils.mask(evidence, selectedConstants);
    }

    // to slow for the life
    /*private Set<Literal> sample(Set<Literal> evidence, final int constantSize) {
        System.out.println("goal\t" + constantSize);
        List<Constant> constants = Sugar.listFromCollections(LogicUtils.constants(new Clause(evidence)));
        Collections.shuffle(constants, random);
        List<Constant> selectedConstants = constants.subList(0, constantSize);
        Set<Literal> retVal = utils.mask(evidence, Sugar.setFromCollections(selectedConstants));
        int idx = constantSize;
        while (LogicUtils.constants(new Clause(retVal)).size() < constantSize && idx < constants.size()) {
            selectedConstants.add(constants.get(idx));
            idx++;
            retVal = utils.mask(evidence, Sugar.setFromCollections(selectedConstants));
            System.out.println("+\t" + LogicUtils.constants(new Clause(retVal)).size() + "\t" + selectedConstants);
        }

        int tryToRemove = selectedConstants.size() - 1;
        while (LogicUtils.constants(new Clause(retVal)).size() > constantSize) {
            if (tryToRemove < 0) {
                break;
            }
            List<Constant> shorther = Sugar.listFromCollections(selectedConstants);
            shorther.remove(tryToRemove);
            Set<Literal> shorterMasked = utils.mask(evidence, Sugar.setFromCollections(shorther));
            Set<Constant> shorterTrueConstants = LogicUtils.constants(new Clause(shorterMasked));

            System.out.println("-\t" + shorterTrueConstants.size());
            if (shorterTrueConstants.size() >= constantSize) {
                selectedConstants = shorther;
                retVal = shorterMasked;
            }

            tryToRemove--;
        }


        return retVal;
    }*/

    // workaround for adding symmetricity of interaction, etc.
    private Set<Literal> extend(Set<Literal> evidence) {
        if (!extend) {
            return evidence;
        }
        Set<Literal> retVal = Sugar.setFromCollections(evidence);
        Pair<String, Integer> interaction = new Pair<>("interaction", 2);
        evidence.stream()
                .filter(literal -> literal.getPredicate().equals(interaction))
                .forEach(literal -> retVal.add(LogicUtils.reverseArguments(literal)));
        return retVal;
    }

    /**
     * returns number of constants occurring within *.db files in the given folder
     *
     * @param path
     * @return
     */
    public Set<Integer> constant(Path path) {
        String suffix = ".db";
        try {
            return Files.list(path)
                    .parallel()
                    .filter(file -> file.toFile().isFile()
                            && file.toFile().getName().endsWith(suffix))
                    .map(filePath -> LogicUtils.constants(new Clause(LogicUtils.loadEvidence(filePath))).size())
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("some problem while traversing the folder:\t" + path);
    }

    public void sampleAndStoreKGEWrtEvidenceSize(Path evidenceFile, Path outputFolder, double fixedTrainRatio, List<Integer> evidenceSize) throws IOException {
        if (!outputFolder.toFile().exists()) {
            outputFolder.toFile().mkdirs();
        }
        Set<Literal> evidence = LogicUtils.loadEvidence(evidenceFile);

        Set<Literal> fixedTrain = null; // aka 'base.nl'
        Path fixedTrainPath = Paths.get(outputFolder.toFile().toString(), "base.nl");
        if (this.rewrite || !fixedTrainPath.toFile().exists()) {
            fixedTrain = sampleLiterals(evidence, (int) (evidence.size() * fixedTrainRatio));
            Files.write(fixedTrainPath, fixedTrain.stream().map(Literal::toString).collect(Collectors.toList()));
        } else {
            fixedTrain = LogicUtils.loadEvidence(fixedTrainPath);
        }

        Set<Literal> restEvidence = Sugar.setDifference(evidence, fixedTrain);
        evidenceSize.stream()
                .map(idx -> new Pair<>(idx, Paths.get(outputFolder.toString(), "queries" + idx + ".db")))
                .filter(pair -> this.rewrite || !pair.s.toFile().exists())
                .filter(pair -> pair.r < restEvidence.size())
                .forEach(pair -> {
                    Set<Literal> sampled = sampleLiterals(evidence, pair.getR());
                    try {
                        Files.write(pair.getS(), sampled.stream().map(Object::toString).collect(Collectors.toList()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private Set<Literal> sampleLiterals(Set<Literal> evidence, int size) {
        switch (mode) {
            case RANDOM_WALK:
                return randomWalk(evidence, size);
            case UNIFORM_RANDOM:
                return uniformLiterals(evidence, size);
            case SNOWBALL:
                return snowballLiterals(evidence, size);
        }
        throw new IllegalStateException("sample and store wrt evidence size implementes only ranodm walk a uniform literal sampling, nothing else");
    }


    public void sampleAndStoreWrtEvidenceSize(Path evidenceFile, Path outputFolder, List<Integer> evidenceSize) {
        if (!outputFolder.toFile().exists()) {
            outputFolder.toFile().mkdirs();
        }
        Set<Literal> evidence = LogicUtils.loadEvidence(evidenceFile);
        evidenceSize.stream()
                .map(idx -> new Pair<>(idx, Paths.get(outputFolder.toString(), "queries" + idx + ".db")))
                .filter(pair -> this.rewrite || (!this.rewrite && !pair.s.toFile().exists()))
                .forEach(pair -> {
                    Set<Literal> sampled = null;
                    switch (mode) {
                        case RANDOM_WALK:
                            sampled = randomWalk(evidence, pair.getR());
                            break;
                        case UNIFORM_RANDOM:
                            sampled = uniformLiterals(evidence, pair.getR());
                            break;
                        case SNOWBALL:
                            sampled = snowballLiterals(evidence, pair.getR());
                            break;

                        default:
                            throw new IllegalStateException("sample and store wrt evidence size implementes only ranodm walk a uniform literal sampling, nothing else");
                    }
                    try {
                        Files.write(pair.getS(), sampled.stream().map(Object::toString).collect(Collectors.toList()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }


    // todo instead of using maxLiterals, insert some kind of object producing "unsatisfied" and "oversatisfied" and "satisfied"
    private Set<Literal> uniformLiterals(Set<Literal> evidence, int maxLiterals) {
        List<Literal> list = shuffle(evidence);
        if (maxLiterals > list.size()) {
            throw new IllegalStateException("cannot sample more (" + maxLiterals + ") elements than is in the list (" + list.size() + ")");
        }
        return Sugar.setFromCollections(list.subList(0, maxLiterals));
    }


    // todo instead of using maxLiterals, insert some kind of object producing "unsatisfied" and "oversatisfied" and "satisfied"
    private Set<Literal> randomWalk(Set<Literal> evidence, int maxLiterals) {
        if (maxLiterals > evidence.size()) {
            throw new IllegalStateException("cannot sample more (" + maxLiterals + ") elements than is in the list (" + evidence.size() + ")");
        }

        Set<Literal> selected = Sugar.set();
        List<Literal> list = shuffle(evidence);
        Literal last = list.remove(0);
        while (selected.size() < maxLiterals) {
            list = shuffle(list);
            Set<Constant> lastConstants = LogicUtils.constantsFromLiteral(last);
            boolean in = false;
            for (int idx = 0; idx < list.size(); idx++) {
                Literal currentLiteral = list.get(idx);
                for (Constant constant : LogicUtils.constantsFromLiteral(currentLiteral)) {
                    if (lastConstants.contains(constant)) {
                        in = true;
                        break;
                    }
                }
                if (in) {
                    list.remove(idx);
                    last = currentLiteral;
                    break;
                }
            }

            if (!in) {
                last = list.remove(0);
            }
            selected.add(last);
        }

        return selected;
    }

    // awful, unify with the one above; or may be this can be optimalized in some other way
    private Set<Literal> randomWalkWithConstantsLimit(Set<Literal> evidence, int maxConstants) {
        if (maxConstants > LogicUtils.constants(new Clause(evidence)).size()) {
            throw new IllegalStateException("cannot sample more constants (" + maxConstants + ") elements than is in the list (" + LogicUtils.constants(new Clause(evidence)).size() + ")");
        }

        Set<Literal> selected = Sugar.set();
        List<Literal> list = shuffle(evidence);
        Literal last = list.remove(0);
        Set<Constant> selectedConstants = Sugar.setFromCollections(LogicUtils.constantsFromLiteral(last));
        while (selectedConstants.size() < maxConstants) {
            list = shuffle(list);
            Set<Constant> lastConstants = LogicUtils.constantsFromLiteral(last);
            boolean in = false;
            for (int idx = 0; idx < list.size(); idx++) {
                Literal currentLiteral = list.get(idx);
                for (Constant constant : LogicUtils.constantsFromLiteral(currentLiteral)) {
                    if (lastConstants.contains(constant)) {
                        in = true;
                        break;
                    }
                }
                if (in) {
                    list.remove(idx);
                    last = currentLiteral;
                    break;
                }
            }

            if (!in) {
                last = list.remove(0);
            }
            selected.add(last);
            selectedConstants.addAll(LogicUtils.constantsFromLiteral(last));
        }

        return selected;
    }

    public void sampleAndStoreWrtTest(Path train, int times, List<Path> tests, Path output) {
        Set<Integer> constantSizes = tests.stream()
                .flatMap(path -> constant(path).stream())
                .distinct()
                .collect(Collectors.toSet());
        sampleAndStoreWrtTest(train, times, constantSizes, output);
    }

    public void sampleAndStoreWrtTest(Path train, int times, Path test, Path output) {
        sampleAndStoreWrtTest(train, times, constant(test), output);
    }


    public void sampleAndStoreWrtTest(Path train, int times, Set<Integer> constantSizes, Path output) {
        if (!rewrite) {
            constantSizes = constantSizes.stream()
                    .filter(idx -> !Paths.get(output.toString(), "" + idx).toFile().exists())
                    .collect(Collectors.toSet());
        }

        Stream<Pair<Integer, List<Set<Literal>>>> sampled = sample(constantSizes, train, times);
        store(sampled, output);
    }

    private void store(Stream<Pair<Integer, List<Set<Literal>>>> sampled, Path output) {
        if (rewrite) {
            System.out.println("already stored data will be rewritten");
        } else {
            System.out.println("existing folders are assumed to be fully filled (by the number of times samples)");
        }
        sampled.forEach(pair -> {
            Path currentOutput = Paths.get(output.toString(), "" + pair.r);
            if (!currentOutput.toFile().exists()) {
                currentOutput.toFile().mkdirs();
            }
            System.out.println("storing to\t" + currentOutput);
            IntStream.range(0, pair.s.size())
                    .parallel()
                    .forEach(idx -> {
                        Path outputFile = Paths.get(currentOutput.toString(), "" + idx + ".db");
                        try {
                            Files.write(outputFile
                                    , pair.s.get(idx).stream()
                                            .map(literal -> literal.toString())
                                            .collect(Collectors.toList()));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        });
    }

    public Pair<Set<Literal>, Set<Literal>> trainTestSplit(Path source, double trainTestConstantRatio) {
        return trainTestSplit(LogicUtils.loadEvidence(source), trainTestConstantRatio);
    }

    private Pair<Set<Literal>, Set<Literal>> trainTestSplit(Set<Literal> evidence, double trainTestConstantRatio) {
        if (UNIFORM_RANDOM != mode) {
            System.out.println("only uniform random is implemented for train test constants split");
            throw new IllegalStateException();// NotImplementedException();
        }

        List<Constant> constants = Sugar.listFromCollections(LogicUtils.constants(evidence));
        constants = shuffle(constants);
        int border = (int) Math.round(constants.size() * trainTestConstantRatio);
        List<Constant> trainConstants = constants.subList(0, border);
        List<Constant> testConstants = constants.subList(border, constants.size());

        Set<Literal> train = ConcurrentHashMap.newKeySet();
        Set<Literal> test = ConcurrentHashMap.newKeySet();

        evidence.stream().parallel().forEach(l -> {
            Set<Constant> constant = LogicUtils.constantsFromLiteral(l);
            if (testConstants.containsAll(constant)) {
                test.add(l);
            } else if (trainConstants.containsAll(constant)) {
                train.add(l);
            }
        });

        return new Pair<>(train, test);
        //return new Pair<>(utils.mask(evidence, trainConstants), utils.mask(evidence, testConstants));
    }


    public static DatasetSampler create(int mode, boolean extend, boolean rewrite) {
        return new DatasetSampler(mode, 0, extend, rewrite);
    }

    public static DatasetSampler snowball(int k, boolean extend, boolean rewrite) {
        return new DatasetSampler(SNOWBALL, k, extend, rewrite);
    }


    public static void main(String[] args) throws IOException {
        System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "10");
        System.out.println("threads:\t" + System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));

//        sampleTestX();
//        sampleTrainX();
//        subsampleTrainX();
//        sampleTest();
//        sampleTrain();
//        trainTestSplit();

        transformHitsTest();
//        createHitsTest();
        //sampleHitsTest();
//        subsample();
    }

    private static void subsample() {
        Path input = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\protein\\train.symmetric.db.typed");

        int k = 10;
        DatasetSampler sampler = new DatasetSampler(SNOWBALL, k, false, false);
        Set<Literal> sampled = sampler.snowball(LogicUtils.loadEvidence(input), 50);

        Path output = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\protein\\subsampledSnowballK5.train.symmetric.db.typed");
        try {
            Files.write(output, sampled.stream().map(Object::toString).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void transformHitsTest() {
        for (String domain : Sugar.list(
                //        "kinshipsA2", "nationsA2", "umlsA2"
                //"kinships", "umls"
                //"protein"
                "nations",
                "kinships"
        )) {

            String domainFolder = domain + "-ntp";
            Path outputQueries = Paths.get("..", "datasets", domainFolder, "hitsQueries.test2.txt");
            Path outputDevQueries = Paths.get("..", "datasets", domainFolder, "hitsQueries.testDev2.txt");
            //Path train = Paths.get("..", "datasets", domain, "train.db");
            Path all = Paths.get("..", "datasets", domainFolder, domain + ".nl");
            // do not forget the validation set as well
            Path test = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + domainFolder + "\\test.nl");
            Path validation = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + domainFolder + "\\dev.nl");

            System.out.println(domain);
            if (outputQueries.toFile().exists() && outputQueries.toFile().exists()) {
                System.out.println("skipping\ttarget is full\t" + outputQueries);
                continue;
            }

            Set<Literal> evidence = LogicUtils.loadEvidence(all);
            Set<Literal> validationEvidence = LogicUtils.loadEvidence(validation);
            Set<Literal> takenLiterals = LogicUtils.loadEvidence(test);

            Set<Constant> constants = LogicUtils.constants(evidence);

            Map<Literal, Set<Literal>> testQueries = generateCorrupted(evidence, takenLiterals, constants);
            Map<Literal, Set<Literal>> testValidationQueries = generateCorrupted(evidence, validationEvidence, constants);
            testQueries.entrySet().forEach(entry -> testValidationQueries.put(entry.getKey(), entry.getValue()));

            try {
                Files.write(outputQueries, Sugar.list(testQueries.entrySet().stream()
                        .map(e -> "query\t" + e.getKey() + "\n" + e.getValue().stream().map(Object::toString).collect(Collectors.joining("\n")))
                        .collect(Collectors.joining("\n\n"))));

                Files.write(outputDevQueries, Sugar.list(testValidationQueries.entrySet().stream()
                        .map(e -> "query\t" + e.getKey() + "\n" + e.getValue().stream().map(Object::toString).collect(Collectors.joining("\n")))
                        .collect(Collectors.joining("\n\n"))));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private static Map<Literal, Set<Literal>> generateCorrupted(Set<Literal> evidence, Set<Literal> test, Set<Constant> constants) {
        return test.stream()
                .collect(Collectors.toMap(Function.identity(), literal ->
                        constants.stream().flatMap(c -> {
                                    // TODO generalize for arbitrary long predicate
                                    Term first = literal.get(0);
                                    Term second = literal.get(1);
                                    List<Literal> corrupted = Sugar.list();
                                    Literal l1 = new Literal(literal.predicate(), Sugar.list(first, c));
                                    if (LogicUtils.areSameTypes(second, c) && !evidence.contains(l1)) {
                                        corrupted.add(l1);
                                    }
                                    Literal l2 = new Literal(literal.predicate(), Sugar.list(c, second));
                                    if (LogicUtils.areSameTypes(first, c) && !evidence.contains(l2)) {
                                        corrupted.add(l2);
                                    }
                                    return corrupted.stream();
                                }
                        ).filter(Objects::nonNull)
                                //  .filter(l -> !evidence.contains(l)) appendix from previous implementation
                                .collect(Collectors.toSet())
                ));
    }


    // predelat, zaclenit do sampleru, at to vypada hezceji
    private static void sampleHitsTest() {
        int depth = 5;
        double ratioSampling = 0.1;
        for (String domain : Sugar.list(
                //        "kinshipsA2", "nationsA2", "umlsA2"
                //"kinships", "umls"
                "protein"
        )) {

            /*Path outputQueries = Paths.get("..", "datasets", domain, "test.symmetric.db.hitsQueries.db");
            Path outputEvidence = Paths.get("..", "datasets", domain, "test.symmetric.db.QueriesEvidence.db");
            Path evidencePath = Paths.get("..", "datasets", domain, "test.symmetric.db");
            */
            Path outputQueries = Paths.get("..", "datasets", domain, "test.symmetric.db.typed.hitsQueries.db");
            Path outputEvidence = Paths.get("..", "datasets", domain, "test.symmetric.db.typed.QueriesEvidence.db");
            Path evidencePath = Paths.get("..", "datasets", domain, "test.symmetric.db.typed");


            System.out.println(domain);
            if (outputEvidence.toFile().exists() && outputQueries.toFile().exists()) {
                System.out.println("skipping");
                continue;
            }

            Set<Literal> evidence = LogicUtils.loadEvidence(evidencePath);

            DatasetSampler ds = DatasetSampler.create(UNIFORM_RANDOM, false, false);
            List<Literal> shuffled = ds.shuffle(evidence);
            int split = (int) (shuffled.size() * ratioSampling);

            List<Literal> removedHeads = Sugar.listFromCollections(shuffled.subList(0, split)); // not asymptotically effective
            List<Literal> testEvidence = shuffled.subList(split, shuffled.size());
            //Set<Constant> constants = LogicUtils.constantsFromLiterals(shuffled);
            Set<Constant> evdsConstants = LogicUtils.constantsFromLiterals(testEvidence);
            Set<Literal> toRemove = Sugar.set();
            Iterator<Literal> iterator = removedHeads.iterator();
            while (iterator.hasNext()) {
                Literal literal = iterator.next();
                for (Term term : literal.arguments()) {
                    Constant constant = (Constant) term;
                    if (!evdsConstants.contains(constant)) {
                        // remove this literal since we the constant is not in the training data
                        toRemove.add(literal);
                        continue;
                    }
                }
            }
            removedHeads.removeAll(toRemove);
/*            System.out.println("check");
            for (Constant constant : LogicUtils.constantsFromLiterals(removedHeads)) {
                if (!evdsConstants.contains(constant)) {
                    System.out.println("missing\t" + constant);
                }
            }
*/

            Map<Literal, Set<Literal>> queries = removedHeads.stream()
                    .collect(Collectors.toMap(Function.identity(), literal ->
                            evdsConstants.stream().flatMap(c -> {
                                        // generalize for arbitrary long predicate
                                        Term first = literal.get(0);
                                        Term second = literal.get(1);
                                        List<Literal> corrupted = Sugar.list();
                                        if (LogicUtils.areSameTypes(second, c)) {
                                            corrupted.add(new Literal(literal.predicate(), Sugar.list(first, c)));
                                        }
                                        if (LogicUtils.areSameTypes(first, c)) {
                                            corrupted.add(new Literal(literal.predicate(), Sugar.list(c, second)));
                                        }
                                        return corrupted.stream();
                                    }
                            ).filter(Objects::nonNull)
                                    .filter(l -> !evidence.contains(l)).collect(Collectors.toSet())
                    ));

            try {
                Files.write(outputQueries, Sugar.list(queries.entrySet().stream()
                        .map(e -> "query\t" + e.getKey() + "\n" + e.getValue().stream().map(Object::toString).collect(Collectors.joining("\n")))
                        .collect(Collectors.joining("\n\n"))));
                Files.write(outputEvidence, testEvidence.stream().map(Object::toString).collect(Collectors.toList()));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    private static void createHitsTest() {
        int depth = 5;
//        String domain = "kinships-ntp";
//        String domain = "umls-ntp";
        String domain = "nations-ntp";
        Path outputPath = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + domain + "\\hitsTestD" + depth);
//        Path test = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + domain + "\\test.db");
        // do not forget the validation set as well
        Path test = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + domain + "\\testPlusDev.txt");
        Path trainEvd = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\" + domain + "\\train.db");
        Set<Literal> evd = LogicUtils.loadEvidence(trainEvd);

        if (!outputPath.toFile().exists()) {
            outputPath.toFile().mkdirs();
        }

        try {
            List<Literal> testLits = LogicUtils.loadEvidence(test).stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList());
            for (int idx = 1; idx <= testLits.size(); idx++) {
                Path query = Paths.get(outputPath.toString(), "query" + idx + ".db");
                Path evidence = Paths.get(outputPath.toString(), "evidence" + idx + ".db");
                if (query.toFile().exists() && evidence.toFile().exists()) {
                    continue;
                }
                Literal literal = testLits.get(idx - 1);

                Set<Literal> neighbours = spamEdges(literal, evd, depth);
                Files.write(query, Sugar.list(literal.toString()));
                Files.write(evidence, neighbours.stream().map(Object::toString).collect(Collectors.toList()));
//                System.exit(-1);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // should be either inside or in reformater ;)
    private static Set<Literal> spamEdges(Literal literal, Set<Literal> evidence, int depth) {
        Set<Constant> all = LogicUtils.constantsFromLiteral(literal);
        Set<Constant> layer = Sugar.setFromCollections(all);
        System.out.println(literal);
        Utils u = Utils.create();
        for (int d = 0; d < depth; d++) {
            System.out.println(d + "\t" + all.size());
            Set<Constant> finalLayer = layer;
            layer = evidence.parallelStream()
                    .flatMap(l -> {
                        Set<Constant> constants = LogicUtils.constantsFromLiteral(l);

                        boolean atLeastOne = false;
                        for (Constant constant : constants) {
                            if (finalLayer.contains(constant)) {
                                atLeastOne = true;
                                //System.out.println(finalLayer + "\t" + l);
//                                System.out.println(l + "\t" + constant);
                                break;
                            }
                        }

                        if (!atLeastOne) {
                            return null;
                        }
                        if (all.containsAll(constants)) {
                            return null;
                        }

                        constants.removeAll(all);
                        return constants.stream();
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            all.addAll(layer);
//            System.exit(-10);
        }
        return u.mask(evidence, all);
    }

    private static void sampleTestX() {
        boolean rewrite = false;
        //String domain = "kinships";
        int max = 1900;
//            String mode = "snowball";
        //String mode = "randomWalk";
        String mode = "uniform";
        DatasetSampler ds = null;
        if (mode.equals("snowball")) {
            int k = 5;
            ds = DatasetSampler.snowball(k, false, rewrite);
            mode = mode + k;
        } else if (mode.equals("randomWalk")) {
            ds = DatasetSampler.create(DatasetSampler.RANDOM_WALK, false, rewrite);
        } else if (mode.equals("uniform")) {
            ds = DatasetSampler.create(DatasetSampler.UNIFORM_RANDOM, false, rewrite);
        } else {
            System.out.println("unknown sampling mode (or not implemented)");
            System.exit(-1);
        }
//        for (String domain : Sugar.list("yago-people", "yago-film", "yago-politics", "yago-states", "yago-musician")) {
        //for (String domain : Sugar.list("umls")) {//"kinships", "protein", "umls", "uwcs"
        for (Pair<Integer, String> pair : Sugar.list(new Pair<>(489, "nationsA2")
                //    , new Pair<>(1410, "umls")
        )) {
            String domain = pair.s;
            max = pair.r;
            // mlns
//        Path groundTruthTest = Paths.get("..", "datasets", "mlns", domain, "test.db");
//        Path output = Paths.get("..", "datasets", "mlns", domain, "test-" + mode);

            Path groundTruthTest = Paths.get("..", "datasets", domain, "test.db");
            Path output = Paths.get("..", "datasets", domain, "test-" + mode);
            System.out.println(output);
            ds.sampleAndStoreWrtEvidenceSize(groundTruthTest, output, IntStream.range(1, max).boxed().collect(Collectors.toList()));
        }

    }

    private static void trainTestSplit() throws IOException {
        double ratio = 0.5;
        String domain = "umls";
        String logicFileName = "umls.txt.db";
        Path source = Paths.get("..", "datasets", domain, logicFileName);
        Pair<Set<Literal>, Set<Literal>> trainTest = DatasetSampler.create(UNIFORM_RANDOM, false, false).trainTestSplit(source, ratio);
        Files.write(Paths.get("..", "datasets", domain, "train.db"), trainTest.getR().stream().map(Object::toString).collect(Collectors.toList()));
        Files.write(Paths.get("..", "datasets", domain, "test.db"), trainTest.getS().stream().map(Object::toString).collect(Collectors.toList()));
    }


    private static void sampleTrain() {
        String domain = true ? "protein-queries" : "uwcs-queries";

        boolean rewrite = false;

//        String mode = "snowball";
        String mode = "randomWalk";
        DatasetSampler ds = null;
        if ("snowball".equals(mode)) {
            int snowballK = 5;
            ds = snowball(snowballK, true, rewrite);
            mode = mode + snowballK;
        } else if ("randomWalk".equals(mode)) {
            ds = DatasetSampler.create(DatasetSampler.RANDOM_WALK_WITH_FULL_EVIDENCE, true, rewrite);
        }

        String dbInput = true ? "train" : "test";

        List<Path> constantsSizes = Sugar.list(Paths.get("..", "datasets", domain, "test-randomWalk")
                , Paths.get("..", "datasets", domain, "test-snowball5"));
        Path train = Paths.get("..", "datasets", domain, dbInput + ".db");
        Path output = Paths.get("..", "datasets", domain, dbInput + "-" + mode);
        int times = 1;

        ds.sampleAndStoreWrtTest(train, times, constantsSizes, output);
    }

    private static void sampleTrainX() {
        //String domain = "kinships";
//        String domain = "yago-states";
//        String domain = "yago-politics";
//        String domain = "yago-musician";
//        String domain = "yago-film";
//        String domain = "yago-people";


        boolean rewrite = false;
        //for (String domain : Sugar.list("yago-states", "yago-politics", "yago-musician", "yago-film", "yago-people")) {
        //for (String domain : Sugar.list("umls", "nations", "kinships", "uwcs", "protein")) {
        for (String domain : Sugar.list("protein")) {
            for (String mode : Sugar.list("randomWalk")) { // "randomWalk" , "snowball"

//        String mode = "snowball";
                //String mode = "randomWalk";
                DatasetSampler ds = null;
                if ("snowball".equals(mode)) {
                    int snowballK = 5;
                    ds = snowball(snowballK, true, rewrite);
                    mode = mode + snowballK;
                } else if ("randomWalk".equals(mode)) {
                    ds = DatasetSampler.create(DatasetSampler.RANDOM_WALK_WITH_FULL_EVIDENCE, true, rewrite);
                }

                System.out.println(domain + "\t" + mode);

                String dbInput = true ? "train" : "test";

                //List<Path> constantsSizes = Sugar.list(Paths.get("..", "datasets", "mlns", domain, "test-uniform") ** mlns
                List<Path> constantsSizes = Sugar.list(Paths.get("..", "datasets", domain, (domain.contains("uwcs") || domain.contains("protein")) ? "queries" : "test-uniform")
                );
                //mlns
//        Path train = Paths.get("..", "datasets", "mlns", domain, dbInput + ".db");
//        Path output = Paths.get("..", "datasets", "mlns", domain, dbInput + "-" + mode);
                Path train = Paths.get("..", "datasets", domain, dbInput + ".db");
                Path output = Paths.get("..", "datasets", domain, dbInput + "-" + mode);

                int times = 1;

                System.out.println(train);
                System.out.println(output);

                ds.sampleAndStoreWrtTest(train, times, constantsSizes, output);
            }
        }
    }


    private static void subsampleTrainX() {
        //String domain = "kinships";
        //String domain = "yago-states";

        boolean rewrite = false;

        for (String domain : Sugar.list("yago-states", "yago-musician", "yago-people", "yago-politics", "yago-film")) {
            DatasetSampler ds = DatasetSampler.create(DatasetSampler.UNIFORM_RANDOM, true, rewrite);

            int maxLit = 4000;

            Path input = Paths.get("..", "datasets", domain, "train.db");
            Path output = Paths.get("..", "datasets", domain, "train.db." + maxLit + ".uniformLit.subampled");

            if (!output.toFile().exists()) {
                Set<Literal> evidence = LogicUtils.loadEvidence(input);
                Set<Literal> sampledEdges = ds.uniformLiterals(evidence, maxLit);
                Set<Literal> inducedSubgraph = Utils.create().mask(evidence, LogicUtils.constants(sampledEdges));
                try {
                    Files.write(output, inducedSubgraph.stream().map(Literal::toString).collect(Collectors.toList()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            Set<Literal> sampled = LogicUtils.loadEvidence(output);
            System.out.println(output + "\nconstants\t" + LogicUtils.constants(sampled).size() + "\nliterals\t" + sampled.size());
        }
    }

    private static void sampleTest() {
        boolean rewrite = false;
        String domain = true ? "protein-queries" : "uwcs-queries";
        int max = domain.contains("protein") ? 2001 : 501;
        String mode = "snowball";
//        String mode = "randomWalk";
        DatasetSampler ds = null;
        if (mode.equals("snowball")) {
            int k = 5;
            ds = DatasetSampler.snowball(k, false, rewrite);
            mode = mode + k;
        } else if (mode.equals("randomWalk")) {
            ds = DatasetSampler.create(DatasetSampler.RANDOM_WALK, false, rewrite);
        } else {
            System.exit(-1);
        }

        Path groundTruthTest = Paths.get("..", "datasets", domain, "test.db");
        Path output = Paths.get("..", "datasets", domain, "test-" + mode);
        ds.sampleAndStoreWrtEvidenceSize(groundTruthTest, output, IntStream.range(1, max).boxed().collect(Collectors.toList()));
    }

    public Pair<Collection<Literal>, Collection<Literal>> sampleAndStore(Path input, Path outputDir, double probabilityTreshold) {
//        if (outputDir.equals(input.getParent())) {
//            throw new IllegalStateException("the input data will be rewritten because the input data is in the same directory as output");
//        }

        if (UNIFORM_RANDOM != this.mode) {
            throw new IllegalStateException();// NotImplementedException(); // implement for other variants
        }
        Set<Literal> complement = Sugar.set();
        Set<Literal> selected = Sugar.set();


        String method = ".uni" + probabilityTreshold;
        Path outputEvidence = Paths.get(outputDir.toString(), input.getFileName().toString() + method + ".db");
        Path outputComplement = Paths.get(outputEvidence.toString() + ".complement");
        if (outputEvidence.toFile().exists() && !rewrite) {
            selected = LogicUtils.loadEvidence(outputEvidence);
            complement = LogicUtils.loadEvidence(outputComplement);
        } else {
            Set<Literal> evidence = LogicUtils.loadEvidence(input);

            // uniform sampling
            Random rnd = new Random();
            for (Literal literal : evidence) {
                if (rnd.nextDouble() > probabilityTreshold) {
                    complement.add(literal);
                } else {
                    selected.add(literal);
                }
            }

            // storing
            try {
                Files.write(outputEvidence, selected.stream().map(Literal::toString).collect(Collectors.toList()));
                Files.write(outputComplement, complement.stream().map(Literal::toString).collect(Collectors.toList()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return new Pair<>(selected, complement);
    }
}
