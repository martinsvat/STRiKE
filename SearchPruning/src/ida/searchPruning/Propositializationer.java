package ida.searchPruning;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
//import jdk.nashorn.internal.runtime.regexp.joni.exception.ValueException;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.MEDataset;

import java.io.IOException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 21. 1. 2018.
 */
public class Propositializationer {

    public static final String SEPARATOR = " ; ";

    public static final int MODE_EXISTENTIAL = 0;
    public static final int MODE_COUNTING = 1;
    public static final int MODE_SAMPLING = 2;

    public static final int FILTER_NONE = 0;
    public static final int FILTER_UNIQUE_VALUES_SHORTER = 1;
    public static final int FILTER_EXISTENTIAL_UNIQUE_VALUES_SHORTER = 2;

    private final boolean conjunctionMode;
    private final List<Pair<BiFunction<Pair<MEDataset, Integer>, Clause, Long>, BiFunction<MultiMap<String, Integer>, List<Clause>, List<Clause>>>> featuresFilter;

    private final BiFunction<Pair<MEDataset, Integer>, Clause, Long> propositionalize;
    private final int subsumption;

    /**
     * if the popositialization method in the feature filters is null, then the propositionalize method given to this function is take instead (as the base one)
     *
     * @param method
     * @param featureFilters
     * @param conjunctionMode
     */
    public Propositializationer(BiFunction<Pair<MEDataset, Integer>, Clause, Long> method, List<Pair<BiFunction<Pair<MEDataset, Integer>, Clause, Long>, BiFunction<MultiMap<String, Integer>, List<Clause>, List<Clause>>>> featureFilters, boolean conjunctionMode) {
        this.featuresFilter = featureFilters;
        this.conjunctionMode = conjunctionMode;
        this.propositionalize = method;
        this.subsumption = Matching.OI_SUBSUMPTION;
    }

    public void propositionalizeHypothesesOnData(String hypothesesDir, String trainDatasetPath, List<String> testDataPaths, Function<List<Path>, List<Path>> hypothesesFileFilter) throws IOException {
        propositionalizeHypothesesOnData(Paths.get(hypothesesDir), Paths.get(trainDatasetPath), testDataPaths.stream().map(Paths::get).collect(Collectors.toList()), hypothesesFileFilter);
    }

    public void propositionalizeHypothesesOnData(String hypothesesDir, String trainDatasetPath, List<String> testDataPaths) throws IOException {
        propositionalizeHypothesesOnData(Paths.get(hypothesesDir), Paths.get(trainDatasetPath), testDataPaths.stream().map(Paths::get).collect(Collectors.toList()), (input) -> input);
    }

    private void propositionalizeHypothesesOnData(Path hypothesesDir, Path train, List<Path> test, Function<List<Path>, List<Path>> hypothesesFileFliter) throws IOException {
        System.out.println(hypothesesDir.toAbsolutePath());
        List<Clause> conjunctionFeatures = load(hypothesesDir, hypothesesFileFliter).stream().map(clause -> (this.conjunctionMode) ? clause : LogicUtils.flipSigns(clause)).collect(Collectors.toList());
        MEDataset trainDataset = MEDataset.create(train.toString(), subsumption);
        // here can be added some kind of caching for case when filter propositialization is the same as the propo method in the final (grounding/storing) phase
        for (Pair<BiFunction<Pair<MEDataset, Integer>, Clause, Long>, BiFunction<MultiMap<String, Integer>, List<Clause>, List<Clause>>> pair : featuresFilter) {
            BiFunction<Pair<MEDataset, Integer>, Clause, Long> propo = pair.r;
            if (null == propo) {
                propo = this.propositionalize;
            }
            BiFunction<MultiMap<String, Integer>, List<Clause>, List<Clause>> filter = pair.s;
            MultiMap<String, Integer> map = propoSupport(conjunctionFeatures, trainDataset, propo);
            conjunctionFeatures = filter.apply(map, conjunctionFeatures);
        }
        List<Clause> selectedFeatures = conjunctionFeatures;
        store(train, selectedFeatures, hypothesesDir);
        Files.write(Paths.get(hypothesesDir.toString() + File.separator + "selectedHypotheses"), selectedFeatures.stream().map(Clause::toString).collect(Collectors.toList()));
        test.forEach(path -> store(path, selectedFeatures, hypothesesDir));
    }

    private MultiMap<String, Integer> propoSupport(List<Clause> conjunctions, MEDataset dataset, BiFunction<Pair<MEDataset, Integer>, Clause, Long> propo) {
        List<List<Long>> matrix = propMatrix(dataset, conjunctions, propo);
        MultiMap<String, Integer> map = new MultiMap<>();
        IntStream.range(0, conjunctions.size())
                .forEach(featureIdx -> map.put(matrix.stream()
                                .map(list -> list.get(featureIdx) + "")
                                .collect(Collectors.joining(" ")),
                        featureIdx));
        return map;
    }


    // this is also somewhere in utils
    private void store(Path examples, List<Clause> conjunctionFeatures, Path outputDir) {
        try {
            MEDataset dataset = MEDataset.create(examples.toString(), subsumption);
            List<String> output = prop(conjunctionFeatures, dataset);
            if (Files.exists(outputDir)) {
                outputDir.toFile().mkdirs();
            }
            Files.write(Paths.get(outputDir.toString(), examples.getFileName() + ".pro"), output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * creates first line of csv
     *
     * @param conjunctions
     * @return
     */
    public String header(List<Clause> conjunctions) {
        return conjunctions.stream().map(Clause::toString).collect(Collectors.joining(SEPARATOR))
                + SEPARATOR + " class ";
        /*StringBuilder sb = new StringBuilder();
        conjunctions.forEach(sb::append);
        sb.append(SEPARATOR).append(" class ");
        return sb.toString();*/
    }

    private List<List<Long>> propMatrix(MEDataset dataset, List<Clause> conjunctions, BiFunction<Pair<MEDataset, Integer>, Clause, Long> method) {
        return IntStream.range(0, dataset.getExamples().size())
                .mapToObj(idxExample -> propExample(dataset, idxExample, conjunctions, method))
                .collect(Collectors.toList());
    }

    private List<Long> propExample(MEDataset dataset, int exampleIdx, List<Clause> conjunctions, BiFunction<Pair<MEDataset, Integer>, Clause, Long> method) {
        if (true) {// parallel version
            return conjunctions.stream()
                    .parallel()
                    .map(conjunction -> method.apply(new Pair<>(MEDataset.create(Sugar.list(Clause.parse(dataset.getExamples().get(exampleIdx).toString())), Sugar.list(0.0), dataset.getSubstitutionType()), 0),
                            Clause.parse(conjunction.toString())))
                    .collect(Collectors.toList());
        }
        return conjunctions.stream()
                .map(conjunction -> method.apply(new Pair<>(dataset, exampleIdx), Clause.parse(conjunction.toString())))
                .collect(Collectors.toList());
    }

    /**
     * it also adds class value to the example at the end
     *
     * @param conjunctions
     * @param dataset
     * @param method
     * @return
     */
    private List<String> prop(List<Clause> conjunctions, MEDataset dataset, BiFunction<Pair<MEDataset, Integer>, Clause, Long> method, boolean headIncluded) {
        List<String> result = Sugar.list();
        if (headIncluded) {
            result.add(header(conjunctions));
        }
        IntStream.range(0, dataset.getExamples().size())
                .forEach(exampleIdx -> result.add(propExample(dataset, exampleIdx, conjunctions, method).stream()
                        .map(val -> "" + val).collect(Collectors.joining(SEPARATOR))
                        + SEPARATOR + (dataset.getTargets()[exampleIdx] > 0.5 ? "1" : "0")));
        return result;
    }

    public List<String> prop(List<Clause> conjunctions, MEDataset dataset, boolean headIncluded) {
        return prop(conjunctions, dataset, propositionalize, headIncluded);
    }

    public List<String> prop(List<Clause> conjunctions, MEDataset dataset) {
        return prop(conjunctions, dataset, true);
    }


    private Stream<Clause> loadClauses(Path path) {
        try {
            return Files.readAllLines(path).stream()
                    .filter(line -> line.trim().length() > 1 && !line.toLowerCase().equals("#emptyclause"))
                    .map(Clause::parse);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

//    private List<Clause> load(String hypothesesDir) throws IOException {
//        return load(Paths.get(hypothesesDir));
//    }

    private List<Clause> load(Path hypothesesDir, Function<List<Path>, List<Path>> hypothesesFileFliter) throws IOException {
        return hypothesesFileFliter.apply(Files.list(hypothesesDir).collect(Collectors.toList()))
                .stream()
                .filter(path -> path.toString().endsWith(".hypotheses"))
                .flatMap(this::loadClauses)
                .filter(clause -> clause.countLiterals() > 0) // trimming empty clause
                .map(IsoClauseWrapper::create)
                .collect(Collectors.toSet())
                .stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList());
    }

    public static Propositializationer create(int mode, int filter, boolean conjunction) {
        BiFunction<Pair<MEDataset, Integer>, Clause, Long> method;
        switch (mode) {
            case MODE_EXISTENTIAL:
                method = Propositializationer::existential;
                break;
            case MODE_COUNTING:
                method = Propositializationer::counting;
                break;
            case MODE_SAMPLING:
                method = Propositializationer::sampling;
                break;
            default:
                throw new IllegalArgumentException("mode type is unknown:\t" + mode);
        }

        List<Pair<BiFunction<Pair<MEDataset, Integer>, Clause, Long>, BiFunction<MultiMap<String, Integer>, List<Clause>, List<Clause>>>> filterList = Sugar.list();
        switch (filter) {
            case FILTER_NONE:
                break;
            case FILTER_UNIQUE_VALUES_SHORTER:
                filterList.add(new Pair<>(null, Propositializationer::shorter));
                break;
            case FILTER_EXISTENTIAL_UNIQUE_VALUES_SHORTER:
                filterList.add(new Pair<>(Propositializationer::existential, Propositializationer::shorter));
                filterList.add(new Pair<>(null, Propositializationer::shorter));
                break;
            default:
                throw new IllegalArgumentException("filter type is unknown:\t" + filter);
        }
        return new Propositializationer(method, filterList, conjunction);
    }

    public static Propositializationer create() {
        int mode = MODE_EXISTENTIAL;
        int filter = FILTER_NONE;
        String givenMode = System.getProperty("ida.searchPruning.propositialization.method", "existential").toLowerCase();
        switch (givenMode) {
            case "existential":
                mode = MODE_EXISTENTIAL;
                break;
            case "counting":
                mode = MODE_COUNTING;
                break;
            case "sampling":
                mode = MODE_SAMPLING;
                break;
            default:
                System.out.println("unknown method given in ida.searchPruning.propositialization.method:\t" + givenMode);
                break;
        }

        String givenFilter = System.getProperty("ida.searchPruning.propositialization.filter", "none").toLowerCase();
        switch (givenFilter) {
            case "none":
                filter = FILTER_NONE;
                break;
            case "existentialuniquevaluesshorter":
                filter = FILTER_EXISTENTIAL_UNIQUE_VALUES_SHORTER;
                break;
            case "uniquevaluesshorter":
                filter = FILTER_UNIQUE_VALUES_SHORTER;
                break;
            default:
                System.out.println("unknown method given in ida.searchPruning.propositialization.filter:\t" + givenFilter);
                break;
        }

        boolean conjunction = Boolean.parseBoolean(System.getProperty("ida.searchPruning.propositialization.conjunctionData", "false"));
        return create(mode, filter, conjunction);
    }

    public static Long existential(Pair<MEDataset, Integer> pair, Clause clause) {
        MEDataset dataset = pair.r;
        Integer idx = pair.s;
        return (long) dataset.numExistentialMatches(HornClause.create(LogicUtils.flipSigns(clause)), 1, CoverageFactory.getInstance().take(idx));
    }

    public static Long sampling(Pair<MEDataset, Integer> pair, Clause conjunction) {
        MEDataset dataset = pair.r;
        Integer idx = pair.s;
        Matching matching = Matching.create(dataset.getExamples().get(idx), Matching.OI_SUBSUMPTION);
        Double samples = matching.searchTreeSampler(conjunction, 0, 1000, 10).t;
        if (samples.equals(Double.NEGATIVE_INFINITY)) {
            return Long.MIN_VALUE;
        } else if (samples.equals(Double.POSITIVE_INFINITY)) {
            return Long.MAX_VALUE;
        }
        return samples.longValue();
    }

    public static Long counting(Pair<MEDataset, Integer> pair, Clause clause) {
        MEDataset dataset = pair.r;
        Integer idx = pair.s;
        Matching matching = Matching.create(dataset.getExamples().get(idx), Matching.OI_SUBSUMPTION);
        return (long) matching.allSubstitutions(clause, 0, Integer.MAX_VALUE).s.size();
    }

    public static List<Clause> shorter(MultiMap<String, Integer> map, List<Clause> hypotheses) {
        return map.entrySet().stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(idx -> hypotheses.get(idx))
                        .sorted((c1, c2) -> -Integer.compare(c1.countLiterals(), c2.countLiterals())).limit(1))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        // ida.searchPruning.propositialization.input path to a folder of which all subfolders having at least one *.hypotheses file will be processed
        // ida.searchPruning.propositialization.train path to train file
        // ida.searchPruning.propositialization.test by | separated paths to files which should be transformed

        // deprected
        // ida.searchPruning.propositialization.hypotheses path to hypotheses folder in which all files with ending *.hypotheses are traversed

        System.out.println("Propositializationer starting");
        System.getProperties().forEach((property, value) -> {
            if (property.toString().startsWith("ida.")) {
                System.out.println(property + "\t" + value);
            }
        });

//        "-Dida.searchPruning.propositialization.method=existential -Dida.searchPruning.propositialization.filter=uniqueValuesShorter"
//        "-Dida.searchPruning.propositialization.train=../datasets/splitted/nci_transformed/gi50_screen_KM20L2/split7030/train"
//        "-Dida.searchPruning.propositialization.test../datasets/splitted/nci_transformed/gi50_screen_KM20L2/split7030/test"

        // we do expect dataset of interpretations
        /*System.setProperty("ida.searchPruning.propositialization.method",
                "existential"
                //"sampling"
                //"counting"
        );
        System.setProperty("ida.searchPruning.propositialization.filter",
                //"none"
                "uniqueValuesShorter"
                //"existentialUniqueValuesShorter"
        );*/

        /*
        if (args.length < 1) {
            throw new IllegalStateException("the first argument on the input is the place to with the data");
        }

        Path inputDir = Paths.get(args[0]);
        */

        boolean forceReevaluation = Boolean.parseBoolean(System.getProperty("ida.searchPruning.propositialization.forceReeavluation", "false"));

        String oneInput = System.getProperty("ida.searchPruning.propositialization.inputDir", "");
        Stream<Path> directories = null;
        if (!oneInput.isEmpty()) {
            Path inputDir = Paths.get(oneInput);
            System.out.println("prop path\t" + inputDir);
            directories = selectFolders(inputDir, forceReevaluation);
        } else {
            String recursiveInput = System.getProperty("ida.searchPruning.propositialization.inputDirR", "");
            if (recursiveInput.isEmpty()) {
                throw new UnsupportedOperationException("Either ida.searchPruning.propositialization.inputDir or ida.searchPruning.propositialization.inputDirR must target the folder(s) you want to process.");
            }
            throw new UnsupportedEncodingException();
            //directories = selectFoldersRecursively(Paths.get(recursiveInput),forceReevaluation);
        }

        boolean skipLastHypothesisFile = Boolean.parseBoolean(System.getProperty("ida.searchPruning.propositialization.skipLastHypothesisFile", "false"));

        System.out.println("parallelization is turn on by default, so watch out ;)");

        Propositializationer propositializationer = Propositializationer.create();

        directories.parallel()
                .forEach(folder -> {
                    try {
                        System.out.println("prop of " + folder);
                        propositializationer.propositionalizeHypothesesOnData(
                                folder.toString(),
                                Sugar.path(System.getProperty("ida.searchPruning.propositialization.train", "")),
                                Sugar.list(System.getProperty("ida.searchPruning.propositialization.test", "")),
                                // just a hack to avoid the last hypotheses file which is in the directory, but is produced from the last, not-fully computed layer
                                (list) -> {
                                    Comparator<? super Path> comparator = Comparator.comparingInt(e -> Integer.parseInt(e.toFile().getName().substring(0, e.toFile().getName().indexOf('.'))));
                                    List<Path> filtered = list.stream().filter(path -> path.toFile().getName().endsWith(".hypotheses")).collect(Collectors.toList());
                                    if (filtered.isEmpty()) {
                                        return Sugar.list();
                                    }
                                    if (skipLastHypothesisFile) {
                                        System.out.println("remember that the last *.hypotheses is thrown away... change it ad libitum (it is overfitted for our special case, not for general purpose");
                                        return filtered.stream().sorted(comparator).limit(filtered.size() - 1).collect(Collectors.toList());
                                    }
                                    return filtered.stream().sorted(comparator).collect(Collectors.toList());
                                }
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }

    private static Stream<Path> selectFolders(Path inputDir, boolean forceReevaluation) throws IOException {
        return Files.list(inputDir)
                .filter(folder -> {
                    // folders only
                    if (!Files.isDirectory(folder)) {
                        return false;
                    }
                    try { // just a cache
                        if (forceReevaluation) {
                            // this folder may be reevaluated
                            return Files.list(folder).anyMatch(file -> file.toFile().isFile() && file.toFile().getName().endsWith(".hypotheses"));
                        }
                        return Files.list(folder).anyMatch(file -> file.toFile().isFile() && file.toFile().getName().endsWith(".hypotheses"))
                                && !Files.list(folder).anyMatch(file -> file.toFile().isFile() && file.toFile().getName().endsWith(".pro"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return false;
                });
    }

}
