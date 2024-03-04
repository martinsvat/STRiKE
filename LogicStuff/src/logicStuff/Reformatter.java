package logicStuff;

import ida.ilp.logic.*;
import ida.ilp.logic.io.PseudoPrologParser;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Combinatorics;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.theories.TheorySimplifier;
//import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 28. 9. 2017.
 */
public class Reformatter {

    public Reformatter() {
    }

    private List<Clause> load(String dataPath) throws IOException {
        List<Clause> clauses = Sugar.list();
        try (Stream<String> stream = Files.lines(Paths.get(dataPath))) {
            stream.forEach(line -> {
                int firstSpace = line.indexOf(' ');
                if (line.indexOf('(') > firstSpace && firstSpace != -1) {
                    // removing label
                    line = line.substring(line.indexOf(' '));
                }
                clauses.add(Clause.parse(line));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return clauses;
    }

    /**
     * Use for transformation into graph-like notation. Instead of c=c-o notation uses graph notation with edges label as a vertex in the middle of the two edge's vertices so, input of c=c-o produces c-bond2-c-bond1-o....
     * Be careful, most cases are unchecked (creating of new bonds predicates and so on).
     *
     * @param path
     * @return
     */
    public List<Clause> enumerateBonds(String path) throws IOException {
        List<Clause> clauses = load(path);
        Set<String> constants = clauses.stream()
                .map(clause -> clause.terms().stream().filter(term -> term instanceof Constant).map(term -> term.toString()).collect(Collectors.toSet()))
                .collect(() -> new HashSet<>(), Set::addAll, Set::addAll);
        ConstantFactory factory = new ConstantFactory(constants);
        return clauses.stream()
                .map(clause -> trasform(clause, factory))
                .collect(Collectors.toList());
    }

    private Clause trasform(Clause clause, ConstantFactory factory) {
        Set<Pair<String, String>> memory = new HashSet<>();
        Stream<String> stream = clause.literals().stream()
                .map(literal -> transformLiteral(literal, factory, memory))
                .collect(() -> new HashSet<String>(), Set::addAll, Set::addAll)
                .stream();
        return Clause.parse(stream.collect(Collectors.joining(",")));
    }

    private Set<String> transformLiteral(Literal literal, ConstantFactory factory, Set<Pair<String, String>> memory) {
        if (literal.predicate().contains("bond")) {
            if (literal.arity() != 2) {
                //throw new NotImplementedException();
                throw new IllegalStateException();
            }
            Iterator<Term> iter = literal.terms().iterator();
            Term arg1 = iter.next();
            Term arg2 = iter.next();

            Set<String> set = Sugar.set();
            Pair<String, String> key = new Pair<>(arg1.toString(), arg2.toString());
            if (memory.contains(key)) {
                return set;
            }
            memory.add(key);
            memory.add(new Pair<>(arg2.toString(), arg1.toString()));

            String bondConstant = factory.getFreshConstant();
            set.add("bond(" + arg1 + "," + bondConstant + ")");
            set.add("bond(" + bondConstant + "," + arg1 + ")");
            set.add("bond(" + arg2 + "," + bondConstant + ")");
            set.add("bond(" + bondConstant + "," + arg2 + ")");
            set.add(literal.predicate() + "(" + bondConstant + ")");
            return set;
        } else {
            return Sugar.set(literal.toString());
        }
    }

    /**
     * Stores lines with at most maxLiterals to pathToFile.processedMaxLiterals file.
     *
     * @param pathToFile
     * @param maxLiterals
     * @throws IOException
     */
    public void preprocessFarmerAtMost(String pathToFile, int maxLiterals) throws IOException {
        List<String> lines = new LinkedList<>();
        for (String line : Files.lines(Paths.get(pathToFile)).collect(Collectors.toList())) {
            if (line.trim().isEmpty() || line.trim().startsWith("[")) {
                lines.add(line);
                //System.out.println(line);
                continue;
            }
            String[] splitted = line.trim().split("\\s+", 3);
            if (splitted.length != 3) {
                System.out.println("Wierd line:\t" + line);
                continue;
            }
            Clause conjunction = Clause.parse(splitted[2]);
            if (conjunction.literals().size() > maxLiterals) {
                continue;
            }
            //System.out.println(line);
            lines.add(line);
            // GC
            conjunction = null;
            splitted = null;
        }
        Files.write(Paths.get(pathToFile + ".processed" + maxLiterals), lines);
    }

    // rather move to logic.constant
    class ConstantFactory {
        private final Set<String> constants;

        public ConstantFactory(Set<String> constants) {
            this.constants = constants;
        }

        public String getFreshConstant() {
            int idx = constants.size();
            while (true) {
                String constant = "c" + idx;
                if (constants.contains(constant)) {
                    idx++;
                    continue;
                }
                constants.add(constant);
                return constant;
            }
        }
    }

    public static Reformatter create() {
        return new Reformatter();
    }

    public static void main(String args[]) throws IOException {
//        Path kml = Paths.get("..", "datasets", "nci_transformed", "gi50_screen_KM20L2.txt");
//        Path datasetsPath = Paths.get("..", "..","pddl", "grip", "examplesHalf");
//        Path datasetsPath = Paths.get("E:\\experiments\\theoryMinersDeeper\\theoryMinersDeeper\\formatted\\protein\\proteinDataset");
//        Pair<List<Clause>, List<Double>> dataset = MEDataset.load(datasetsPath);
//        System.out.println(dataset.r.stream().mapToInt(Clause::countLiterals).sum());

        Reformatter reformatter = Reformatter.create();
        /*
        Path file = Paths.get(path + "_reformatted_bonds");
        Reformatter reformatter = Reformatter.create();
        List<Clause> clauses = reformatter.enumerateBonds(path);
        Files.write(file, clauses.theory().map(Clause::toString).collect(Collectors.toList()), Charset.forName("UTF-8"));
        */

        //reduceKML();
        //typelessMuta();
        //String path = Sugar.path("..", "..", "datasets", "mlns", "uwcs", "all.db");
        //String path = Sugar.path("..", "..", "datasets", "mlns", "imdb", "merged.db");
        //String path = Sugar.path("..", "datasets", "mlns", "uwcs", "all.db");
//        String path = Sugar.path("..", "datasets", "mlns", "yeast", "train.db");
        //rawMLNsToOneLine(path, path + ".uw2.transformed");

        Path path = Paths.get("..", "datasets", "nci_transformed", "gi50_screen_BT_549.txt");
        MEDataset med = MEDataset.create(path, Matching.THETA_SUBSUMPTION);
        System.out.println(reformatter.toClaudien(med.getExamples().stream().flatMap(c -> c.literals().stream()).collect(Collectors.toSet())));


        /** reformatting datat to prolog/aleph **/
        /** /
         Sugar.list("train", "test").stream().forEach(fileName -> {
         Path input = Paths.get("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", fileName);
         Path output = Paths.get("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", fileName + ".prolog_lines");
         try {
         reformatter.moleculeToPrologLike(input, output);
         } catch (IOException e) {
         e.printStackTrace();
         }
         });


         Sugar.list("train.prolog_lines", "test.prolog_lines").stream()
         .forEach(fileName -> {
         Path input = Paths.get("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030", fileName);
         Path output = Paths.get("..", "datasets", "splitted", "nci_transformed", "gi50_screen_KM20L2", "split7030");
         try {
         reformatter.toProlog(input, output);
         } catch (IOException e) {
         e.printStackTrace();
         }
         });
         /**/

        /** /
         // TODO inverse of moleculeToPrologLike
         //        Path input = Paths.get("..", "..", "experiments", "aleph", "connected", "kml", "featuresRunner.ms100.sh.o4626725");
         //        Path outputDir = Paths.get("..", "..", "experiments", "aleph", "connected", "kml", "aleph_100");
         Path input = Paths.get("..", "..", "experiments", "aleph", "connectedMode3", "kml", "endFeaturesMs100D4.out.txt");
         Path outputDir = Paths.get("..", "..", "experiments", "aleph", "connectedMode3", "kml", "aleph_100");
         //Collection<Clause> alephClauses = reformatter.loadRawAlephClauses(input, true).stream().map(reformatter::alephToMolecule).collect(Collectors.toList());
         Collection<Clause> alephClauses = reformatter.loadAlephFeatureOutput(input).stream().map(reformatter::alephToMolecule).collect(Collectors.toList());
         alephClauses.forEach(System.out::println);
         IntStream.range(1, 4).forEach(idx -> System.out.println(idx + "\t" + alephClauses.stream().filter(c -> c.countLiterals() == idx).count()));
         //reformatter.storeAsGridResult(alephClauses, outputDir);
         /**/

        /** /
         //Path input = Paths.get("..", "..", "experiments", "farmer", "kml", "4690793.arien-pro.ics.muni.cz.OU");
         //Path outputDir = Paths.get("..", "..", "experiments", "farmer", "kml", "farmer_1");
         //Path input = Paths.get("..", "..", "experiments", "farmer", "kml", "8-8", "outputFeaturs100mod3d8.txt.processed9");
         //Path outputDir = Paths.get("..", "..", "experiments", "farmer", "kml", "8-8", "farmer_100");
         //         Path input = Paths.get("E:", "experiments", "farmerBonds", "bonds", "outputFeatures100.bonds.4");
         //         Path outputDir = Paths.get("E:", "experiments", "farmerBonds", "bonds", "farmerB4_100");
         //         Collection<Clause> farmerClauses = reformatter.loadFarmerClauses(input, 100).stream().map(reformatter::alephToMolecule).collect(Collectors.toList());
         //         reformatter.storeAsGridResult(farmerClauses, outputDir);
         for (int idx = 2; idx < 7; idx++) {
         Path input = Paths.get("E:\\experiments\\farmerBondsMinSup1\\bonds\\outputFeatures100.bonds." + idx);
         System.out.println(idx + ":\t" + reformatter.rawNumberOfFarmerHypotheses(input));
         }
         /**/

        /** /
         for (int maxB = 2; maxB <= 8; maxB++) {
         Path inputDir = Paths.get("E:\\experiments\\modeBondsMs1\\_\\__\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train\\output_bfs-bond-2-" + maxB+ "_1_OI_15_45_1_3_6_1_1_2147483647_2_none_1_100_none");
         long numberOfHypotheses = Files.list(inputDir).filter(p -> p.toString().endsWith(".hypotheses"))
         .flatMap(file -> {
         try {
         return Files.readAllLines(file).stream()
         .map(line -> {
         if (line.contains("emptyClause")) {
         return Clause.parse("");
         }
         return Clause.parse(line);
         });
         } catch (IOException e) {
         e.printStackTrace();
         }
         throw new IllegalStateException();
         })
         //.map(clause -> IsoClauseWrapper.create(clause))
         .collect(Collectors.toSet())
         .size();
         System.out.println("inputDir of\t" + inputDir + "\n#\t" + numberOfHypotheses);
         }
         /**/


        /** / //farmer output check -- more queries than iso bfs
         Path input = Paths.get("E:\\experiments\\farmerBonds\\bonds\\farmerB4_100\\9.hypotheses");
         Map<IsoClauseWrapper, IsoClauseWrapper> cache = new HashMap<>();
         int n = Files.readAllLines(input).stream()
         .filter(line -> !line.contains("emptyClause"))
         .map(Clause::parse)
         //.map(IsoClauseWrapper::create)
         //         .forEach(icw -> {
         //         if (cache.containsKey(icw)) {
         //         System.out.println("\ndiff here\n" + icw.getOriginalClause() + "\n" + cache.get(icw).getOriginalClause());
         //         } else {
         //         cache.put(icw, icw);
         //         }
         //         })
         .collect(Collectors.toSet())
         .size();
         System.out.println("\t" + n);
         /**/

        /** / // theory simplification
         String dt = true ? "satDtl" : "dtl";
         String domain = "protein";
         //Path input = Paths.get("E:\\experiments\\theoryMinersDeeper\\theoryMinersDeeper\\formatted\\imdb\\"+dt+"\\constraintsFancy.txt");
         Path input = Paths.get("E:\\experiments\\theoryMinersDeeper\\theoryMinersDeeper\\formatted\\"+domain+"\\"+dt+"\\constraintsRaw.txt");
         Path output = Paths.get(input.toString() + "constraintsSimplified.txt");
         simplifyTheory(input, output);
         /**/

    }

    private String toClaudien(Set<Literal> literals) {
        StringBuilder retVal = new StringBuilder();
        Set<Predicate> predicates = literals.stream().map(Literal::getPredicate).distinct().map(Predicate::create).collect(Collectors.toSet());

        retVal.append("\n% === CORES ===");
        predicates.stream()
                .flatMap(p -> Combinatorics.variationsWithRepetition(IntStream.range(0, p.getArity()).mapToObj(idx -> Variable.construct("V" + idx)).collect(Collectors.toSet()), p.getArity()).stream()
                        .map(arguments -> new Literal(p.getName(), arguments))
                        .map(l -> IsoClauseWrapper.create(new Clause(l)))
                        .distinct()
                        .map(icw -> Sugar.chooseOne(icw.getOriginalClause().literals()))
                )
                .flatMap(l -> Sugar.list("\n" + l + ".", "\n\\+" + l + ".").stream())
                .forEach(retVal::append);


        retVal.append("\n% === TYPES ===");
        predicates.stream()
                .map(p -> new Literal(p.getName(), IntStream.range(0, p.getArity()).mapToObj(idx -> Constant.construct("p")).collect(Collectors.toList())))
                .map(l -> "\n" + l + ".")
                .forEach(retVal::append);

        retVal.append("\n% === MODES ===");
        predicates.stream()
                .flatMap(p -> Combinatorics.variationsWithRepetition(Sugar.set(Constant.construct("'+'"), Constant.construct("'-'")), p.getArity()).stream()
                        .map(arguments -> new Literal(p.getName(), arguments)))
                .flatMap(l -> Sugar.list("\n" + l + ".", "\n\\+" + l + ".").stream())
                .filter(s -> !s.contains("'-'") || !s.contains("\\+"))
                .forEach(retVal::append);

        // % === BACKGROUND === is empty

        // evidence / facts
        retVal.append("\n% ============");
        literals.forEach(l -> retVal.append("\n" + l + "."));
        return retVal.toString();
    }

    private void atomize(String path) throws IOException {
        List<String> data = Files.readAllLines(Paths.get(path)).stream()
                .map(line -> {
                    String[] splitted = line.split(" ", 2);
                    return new Pair<>(splitted[0], Clause.parse(splitted[1]));
                })
                .map(pair -> {
                    Clause clause = pair.getS();
                    Clause c = new Clause(clause.literals().stream()
                            .filter(l -> {
                                try {
                                    Integer.parseInt(l.predicate());
                                } catch (Exception e) {
                                    return true;
                                }
                                return false;
                            }).map(l -> {
                                if (l.predicate().equals("bond")) {
                                    String bondType = Sugar.chooseOne(clause.literals().stream()
                                            .filter(literal -> !literal.predicate().equals("bond") && literal.argumentsStream().anyMatch(arg -> arg.toString().equals(l.get(2).toString())))
                                            .collect(Collectors.toList())).predicate();
                                    return new Literal(l.predicate(), Sugar.list(l.get(0), l.get(1), Constant.construct(bondType)));
                                }
                                return new Literal("atm", Sugar.list(l.get(0), Constant.construct(l.predicate())));
                            }).collect(Collectors.toList()));
                    return new Pair<>(pair.getR(), c);
                })
                .map(p -> p.getR() + " " + p.getS())
                .collect(Collectors.toList());

        Files.write(Paths.get(path + ".atomized"), data);
    }

    private static void simplifyTheory(Path inputFile, Path output) throws IOException {
        List<Clause> clauses = Files.readAllLines(inputFile).stream()
                .map(line -> {
                    if (line.contains("emptyClause")) {
                        return Clause.parse("");
                    }
                    return Clause.parse(line);
                }).collect(Collectors.toList());
        int maxVariables = 1 + clauses.stream().mapToInt(c -> LogicUtils.variables(c).size()).max().orElse(0);
        List<Clause> simplified = TheorySimplifier.simplify(clauses, maxVariables);
        Files.write(output, simplified.stream().map(Clause::toString).collect(Collectors.toList()));
        System.out.println(inputFile);
        System.out.println(simplified.size());
    }

    private long rawNumberOfFarmerHypotheses(Path input) throws IOException {
        return Files.lines(input).filter(line -> {
                    if (line.trim().isEmpty() || line.trim().startsWith("[")) {
                        return false;
                    }
                    return true;
                }
        ).count();
    }


    private void storeAsGridResult(Collection<Clause> clauses, Path outputDir) {
        if (!outputDir.toFile().exists()) {
            outputDir.toFile().mkdirs();
        }
        Map<Integer, List<Clause>> grouped = clauses.stream().collect(Collectors.groupingBy(Clause::countLiterals, Collectors.toList()));
        if (!grouped.containsKey(0)) {
            grouped.put(0, Sugar.list(Clause.parse("")));
        }
        List<Integer> lengths = Sugar.listFromCollections(grouped.keySet());
        Collections.sort(lengths);

        lengths.forEach(length -> {
            try {
                List<String> hypotheses = IntStream.range(0, length + 1)
                        .filter(grouped::containsKey)
                        .boxed()
                        .flatMap(idx -> grouped.get(idx).stream().map(Clause::toString))
                        .collect(Collectors.toList());
                Files.write(Paths.get(outputDir.toString(), length + ".hypotheses"), hypotheses);
                String statsDir = outputDir.toString() + "_" + length;
                File statsd = new File(outputDir.toString() + "_" + length);
                if (!statsd.exists()) {
                    statsd.mkdirs();
                }
                Files.write(Paths.get(statsDir, "stats.txt"),
                        Sugar.list("value", "# hypotheses: " + (hypotheses.size() * 1.0)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

    }

    private Collection<Clause> loadAlephFeatureOutput(Path input) throws IOException {
        Set<String> processed = Sugar.set();
        List<Clause> result = Sugar.list();
        boolean skipViolation = false;
        for (String line : Files.lines(input).collect(Collectors.toList())) { // can be done by some stateful consumer
            if (!line.startsWith("'$aleph_feature'(")) {
                continue;
            }
            // '$aleph_feature'(1,[664,0,2,664],A,active(B),atm(B,C,o2)).
            // -----------0------|--1-|2|3|-4--|5|---6----|
            String bodyPart = line.split(",", 8)[7].trim();
            bodyPart = bodyPart.substring(0, bodyPart.length() - 2);
            if (bodyPart.startsWith("(") && bodyPart.endsWith(")")) {
                bodyPart = bodyPart.substring(1, bodyPart.length() - 1);
            }
            Clause conjunction = Clause.parse(bodyPart);
            Clause clause = LogicUtils.flipSigns(conjunction);
            result.add(clause);
        }
        return result;
    }


    /**
     * parses result of feature_induce, from which it parses only the bodies of the rules and returns it in as disjunctions
     *
     * @param input
     * @return
     * @throws IOException
     */
    private Collection<Clause> loadRawAlephClauses(Path input, boolean noDuplicity) throws IOException {
        Set<String> processed = Sugar.set();
        List<Clause> result = Sugar.list();
        String currentClause = null;
        boolean skipViolation = false;
        for (String line : Files.lines(input).collect(Collectors.toList())) { // can be done by some stateful consumer
            if (line.startsWith("[clausetype violation]") && !result.isEmpty()) {
                if (!skipViolation) {
                    result.remove(result.size() - 1);
                }
                skipViolation = false;
                continue;
            }
//            if noDuplicity == True
//            if (line.startsWith("[clauses constructed]") && !result.isEmpty()) {
//                result.remove(result.size() - 1);
//                continue;
//            }
            if (line.startsWith("active(A) :-")) {
                currentClause = "";
                continue;
            }
            if (null == currentClause) {
                continue;
            }
            currentClause += line.trim();
            if (currentClause.charAt(currentClause.length() - 1) == '.') {
                Clause clause = LogicUtils.flipSigns(Clause.parse(currentClause));
                String str = clause.toString();
                if (processed.contains(str) && noDuplicity) {
//                    System.out.println("noadding\t" + str);
                    currentClause = null;
                    skipViolation = true;
                    continue;
                } else {
                    skipViolation = false;
                }
                processed.add(str);
                result.add(clause);
                currentClause = null;
//                System.out.println("adding\t" + str);
            }
        }
        return result;
    }

    private Collection<Clause> loadFarmerClauses(Path input, int maxLength) throws IOException {
        List<Clause> result = new LinkedList<>();
        for (String line : Files.lines(input).collect(Collectors.toList())) {
            if (line.trim().isEmpty() || line.trim().startsWith("[")) {
                continue;
            }
            String[] splitted = line.trim().split("\\s+", 3);
            if (splitted.length != 3) {
                System.out.println("Wierd line:\t" + line);
                continue;
            }
            Clause conjunction = Clause.parse(splitted[2]);
            Clause disjunction = LogicUtils.flipSigns(new Clause(conjunction.literals().stream().filter(l -> !l.predicate().equals("active")).collect(Collectors.toSet())));
            if (disjunction.literals().size() > maxLength) {
                continue;
            }
            result.add(disjunction);
        }
        return result;
    }


    /**
     * inverse of moleculeToPrologLike
     *
     * @param clause
     * @return
     */
    private Clause alephToMolecule(Clause clause) {
        return new Clause(clause.literals().stream()
                .map(literal -> {
                    if (literal.predicate() == "atm") {
                        return new Literal(literal.arguments()[2].name(), literal.isNegated(), Sugar.list(literal.arguments()[1]));
                    } else if (literal.predicate() == "bond") {
                        Term[] arguments = literal.arguments();
                        return new Literal(arguments[3].name().substring(1), literal.isNegated(), Sugar.list(arguments[1], arguments[2]));
                    }
                    throw new IllegalStateException("some unknown predicate from prolog-like format:\t" + literal);
                })
                .collect(Collectors.toList()));
    }

    /**
     * reformates back to atm/2 and bond/3 representation from atm/1 and bond/2
     *
     * @param clause
     * @return
     */
    private Clause moleculeToPrologLike(Clause clause, String constPrefix) {
        return new Clause(clause.literals().stream()
                .map(literal -> {
                    Literal result = null;
                    if (1 == literal.arity()) {
                        result = new Literal("atm", Sugar.list(Constant.construct(constPrefix + Sugar.chooseOne(literal.terms()).name()),
                                Constant.construct(literal.predicate())));
                    } else if (2 == literal.arity()) {
                        List<Term> args = literal.argumentsStream().map(term -> Constant.construct(constPrefix + term.name())).collect(Collectors.toList());
                        //args.add(Constant.construct(literal.predicate().substring(0,literal.predicate().indexOf("_bond"))));
                        args.add(Constant.construct("b" + literal.predicate()));
                        result = new Literal("bond", args);
                    } else {
                        System.out.println("unknown predicate:\t" + literal);
                    }
                    return result;
                })
                .collect(Collectors.toList()));
    }

    private void moleculeToPrologLike(Path input, Path output) throws IOException {
        MEDataset dataset = MEDataset.create(input, Matching.THETA_SUBSUMPTION);
        List<String> examples = IntStream.range(0, dataset.size())
                .mapToObj(idx -> new Pair<String, Clause>((dataset.getTargets()[idx] > 0.5) ? "+" : "-",
                        moleculeToPrologLike(dataset.getExamples().get(idx), "ex" + idx + "_")))
                .map(pair -> pair.r + " " + pair.s)
                .collect(Collectors.toList());
        if (!output.toFile().exists()) {
            output.getParent().toFile().mkdirs();
        }
        System.out.println("writing to\t" + output);
        Files.write(output, examples);
    }

    private void toProlog(Path inputFile, Path outputDir) throws IOException {
        String prefix = "";
        MEDataset dataset = MEDataset.create(inputFile, Matching.THETA_SUBSUMPTION);
        List<Triple<Boolean, String, List<String>>> prologLike = IntStream.range(0, dataset.getExamples().size())
                .mapToObj(idx -> interpretationToProlog(dataset, idx, prefix)).collect(Collectors.toList());
        List<String> examples = prologLike.stream().flatMap(triple -> triple.t.stream()).collect(Collectors.toList());
        List<String> pos = prologLike.stream().filter(triple -> triple.r).map(triple -> triple.s).collect(Collectors.toList());
        List<String> neg = prologLike.stream().filter(triple -> !triple.r).map(triple -> triple.s).collect(Collectors.toList());

        if (!outputDir.toFile().exists()) {
            outputDir.toFile().mkdirs();
        }
        Function<String, Path> baseOutput = (fileName) -> Paths.get(outputDir.toString() + File.separator + inputFile.getFileName() + "." + fileName);
        Files.write(baseOutput.apply("data"), examples);
        Files.write(baseOutput.apply("f"), pos);
        Files.write(baseOutput.apply("n"), neg);

        List<String> outputModes = Sugar.list();
        outputModes.add(":- modeh(1,active(+drug)).");
        dataset.allPredicates().forEach(pair -> {
            List<String> modes = Sugar.list();
            if (2 == pair.s) {
                modes.add((prefix + pair.r) + "(+drug,-atomid,#atmtype)");
                modes.add((prefix + pair.r) + "(+drug,+atomid,#atmtype)");
            } else if (3 == pair.s) {
                modes.add((prefix + pair.r) + "(+drug,+atomid,-atomid,#bondtype)");
                modes.add((prefix + pair.r) + "(+drug,+atomid,+atomid,#bondtype)");
            } else {
                System.out.println("unknown predicate type:\t" + pair);
                return;
            }
            modes.forEach(mode -> outputModes.add(":- modeb(*," + mode + ")."));
            outputModes.add(":- determination(active/1," + (prefix + pair.r) + "/" + (pair.s + 1) + ").");
        });
        outputModes.stream().sorted(Collections.reverseOrder()).forEach(System.out::println);
    }

    /**
     * reformates simple molecular interpretation to prolog examples, by introducing new atom active/1 to express the class
     * <p>
     * returns triple of boolean (pos/neg class), string representation in class file, list of fact of the example
     *
     * @param dataset
     * @param idx
     * @return
     */
    private Triple<Boolean, String, List<String>> interpretationToProlog(MEDataset dataset, int idx, String prefix) {
        Clause interpretation = dataset.getExamples().get(idx);
        String idToken = "ex" + idx;
        List<String> facts = interpretation.literals().stream()
                .map(literal -> new Literal(prefix + literal.predicate(), Sugar.linkedListFromCollections(Sugar.list(Constant.construct(idToken)), Arrays.asList(literal.arguments()))))
                .map(literal -> literal + ".")
                .collect(Collectors.toList());
        return new Triple<>(dataset.getTargets()[idx] > 0.5,
                "active(" + idToken + ").",
                facts);
    }

    /**
     * Converts standard MLNs input to pseudo-prolog-like (hopefully) format.
     *
     * @param inputFilePath
     * @param outputFilePath
     */
    private static void rawMLNsToOneLine(String inputFilePath, String outputFilePath) throws IOException {
        Reformatter reformattor = Reformatter.create();
        reformattor.rawMLNsToPseudoProlog(inputFilePath, outputFilePath);
    }

    private void rawMLNsToPseudoProlog(String inputFilePath, String outputFilePath) throws IOException {
        List<Literal> literals = Sugar.list();
        Set<String> attributeLiterals = Sugar.set(
                //"phenotype", "complex", "function", "protein_class", // "location",
                "gender", "genre",
                "courseLevel"
                //"yearsInProgram", "inPhase", "hasPosition", "courseLevel"
        );


        // uwcs dataset preformatovani pro dalsi experimenty
        // taughtBy(Course158, Person240, Spring_0304) --> taughtBy(Course158, Person240)
        // ta(Course156, Person257, Autumn_0304) --> ta(Course156, Person257)
        // courseLevel(Course21, Level_400) --> courseLevel_Level_400(Course21)
        // delete yearsInProgram

        Set<String> removeLast = Sugar.set("ta", "taughtBy");

        // publication/2 and projectMember/2 are reversed than these others
        Set<String> forbidden = Sugar.set("sameGender", "samePerson", "sameMovie", "sameTitle"
                //, "location"
                , "yearsInProgram"
        );

        try {
            for (String line : Sugar.readLines(new FileReader(inputFilePath))) {
                if (line.isEmpty()) {
                    continue;
                }
                Clause clause = Clause.parse(line);
                clause = LogicUtils.constantizeClause(clause);
                Literal literal = Sugar.chooseOne(clause.literals());
                //if ("taughtBy".equals(literal.predicate()) || "ta".equals(literal.predicate())) {
                if (removeLast.contains(literal.predicate())) {
                    literal = new Literal(literal.predicate(), literal.get(0), literal.get(1));
                }

                if (attributeLiterals.contains(literal.predicate())) {
                    literal = new Literal(literal.predicate() + "_" + literal.get(1), literal.get(0));
                }

                if (literal.predicate().equals("interaction")) {
                    literals.add(new Literal(literal.predicate(), literal.get(1), literal.get(0)));
                }

                if (forbidden.contains(literal.predicate()) || literal.predicate().startsWith("same")) {
                    continue;
                }

                literals.add(literal);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        Files.write(Paths.get(outputFilePath), ("+ " + (new Clause(literals)).toString()).getBytes());
    }

    /**
     * hardcoded simplification of molecular dataset KM20L2
     *
     * @throws IOException
     */
    private static void reduceKML() throws IOException {
        String path = Sugar.path("..", "..", "datasets", "nci_transformed", "gi50_screen_KM20L2.txt");
        Path finalDestination = Paths.get(path + "_simpler");

        Reformatter reformatter = Reformatter.create();
        List<Pair<String, Clause>> data = reformatter.parse(path);

        Map<String, String> mapping = new HashMap<>();
        mapping.put("s2", "s");
        mapping.put("s3", "s");
        mapping.put("so2", "so");
        mapping.put("am_bond", "bond");
        mapping.put("ar_bond", "bond");
        mapping.put("1_bond", "bond");
        mapping.put("2_bond", "bond");
        mapping.put("3_bond", "bond");
        mapping.put("c1", "c");
        mapping.put("c2", "c");
        mapping.put("c3", "c");
        mapping.put("c3", "c");
        mapping.put("car", "c");
        mapping.put("n1", "n");
        mapping.put("n2", "n");
        mapping.put("n3", "n");
        mapping.put("n4", "n");
        mapping.put("nam", "n");
        mapping.put("nar", "n");
        mapping.put("o2", "o");
        mapping.put("o3", "o");
        List<Pair<String, Clause>> simplerData = reformatter.renamePredicates(data, mapping);

        System.out.println("\npredicates");
        simplerData.stream()
                .map(p -> p.s.predicates())
                .collect(HashSet::new, HashSet::addAll, HashSet::addAll)
                .stream()
                .sorted()
                .forEach(predicate -> System.out.println(predicate));
        System.out.println("");

        Files.write(finalDestination, simplerData.stream().map(pair -> pair.r + "\t" + pair.s).collect(Collectors.toList()), Charset.forName("UTF-8"));
        System.out.println("final destination for simpler data\t" + finalDestination);

        Path subsampledPath = Paths.get(finalDestination + "_subsampled2");
        int samples = 100;
        Files.write(subsampledPath, Combinatorics.randomSelect(simplerData, samples).stream().map(pair -> pair.r + "\t" + pair.s).collect(Collectors.toList()), Charset.forName("UTF-8"));
        System.out.println("subsampled data in\t" + subsampledPath);
    }

    /**
     * hardcoded case for removing bond types in muta dataset, should work with other molecular dataset
     *
     * @throws IOException
     */
    private static void typelessMuta() throws IOException {
        String path = Sugar.path("..", "..", "datasets", "muta", "examples.txt");
        Path typeBonds = Paths.get(path + "_typeOfBondsInPredicates");
        Path typeless = Paths.get(path + "_typeless");

        Reformatter reformatter = Reformatter.create();
        List<Pair<String, Clause>> bondsTypes = reformatter.typeOfBondToPredicate(path);

        bondsTypes.forEach(System.out::println);
        System.out.println("");
        Files.write(typeBonds, bondsTypes.stream().map(pair -> pair.r + "\t" + pair.s).collect(Collectors.toList()), Charset.forName("UTF-8"));


        Map<String, String> mapping = new HashMap<>();
        mapping.put("7_bond", "bond");
        mapping.put("1_bond", "bond");
        mapping.put("3_bond", "bond");
        mapping.put("2_bond", "bond");
        mapping.put("4_bond", "bond");
        mapping.put("5_bond", "bond");
        List<Pair<String, Clause>> typelessData = reformatter.renamePredicates(bondsTypes, mapping);

        System.out.println("\npredicates");
        typelessData.stream()
                .map(p -> p.s.predicates())
                .collect(HashSet::new, HashSet::addAll, HashSet::addAll)
                .forEach(predicate -> System.out.println(predicate));
        System.out.println("");

        typelessData.forEach(System.out::println);
        Files.write(typeless, typelessData.stream().map(pair -> pair.r + "\t" + pair.s).collect(Collectors.toList()), Charset.forName("UTF-8"));
    }

    private Clause renamePredicates(Clause clause, Map<String, String> mapping) {
        return new Clause(clause.literals().stream()
                .map(literal -> {
                    if (mapping.containsKey(literal.predicate())) {
                        return new Literal(mapping.get(literal.predicate()), literal.arguments());
                    } else {
                        return literal;
                    }
                })
                .collect(Collectors.toSet()));
    }

    private List<Pair<String, Clause>> renamePredicates(List<Pair<String, Clause>> dataset, Map<String, String> mapping) {
        return dataset.stream()
                .map(pair -> new Pair<>(pair.r, renamePredicates(pair.s, mapping)))
                .collect(Collectors.toList());
    }

    private List<Pair<String, Clause>> parse(String path) throws IOException {
        List<Pair<Clause, String>> dataset = PseudoPrologParser.read(new FileReader(path));
        return dataset.stream().map(p -> new Pair<>(p.s, p.r)).collect(Collectors.toList());
    }

    private List<Pair<String, Clause>> typeOfBondToPredicate(String path) throws IOException {
        List<Pair<Clause, String>> dataset = PseudoPrologParser.read(new FileReader(path));
        return dataset.stream()
                .map(pair -> new Pair<>(pair.s, bondsTypeToPredicate(pair.r)))
                .collect(Collectors.toList());
    }

    private Clause bondsTypeToPredicate(Clause clause) {
        Set<Literal> literals = Sugar.setFromCollections(clause.literals());
        Set<Literal> remove = Sugar.set();
        Set<Literal> unary = literals.stream().filter(l -> l.arity() == 1).collect(Collectors.toSet());
        // suppose that bond(X,Y,Type).... type(Type)
        Set<Literal> add = literals.stream()
                .filter(literal -> literal.predicate().startsWith("bond") && literal.arity() == 3)
                .map(literal -> {
                    Term typeConst = literal.get(2);
                    Literal type = unary.stream().filter(l -> l.get(0) == typeConst).findFirst().orElse(null);
                    if (null == type) {
                        System.out.println("unable to find unary literal with argument \"" + type + "\".");
                        return literal;
                    }
                    remove.add(literal);
                    remove.add(type);
                    return new Literal(type.predicate() + "_" + literal.predicate(), literal.get(0), literal.get(1));
                }).collect(Collectors.toSet());

        literals.removeAll(remove);
        literals.addAll(add);
        return new Clause(literals);
    }

}
