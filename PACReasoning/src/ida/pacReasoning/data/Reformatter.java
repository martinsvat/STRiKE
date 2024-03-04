package ida.pacReasoning.data;

import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.subsumption.Matching;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Sugar;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Quadruple;
import ida.utils.tuples.Triple;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 8. 8. 2018.
 */
public class Reformatter {

    private final Map<String, String> cache;
    private final String turtleOneArgumentLiteralPrefix = "myOwnProperty_";

    public Reformatter() {
        this.cache = new HashMap<>();
    }


    public Collection<Clause> loadAmieOutput(Path source) {
        try {
            return Files.lines(source)
                    .filter(line -> line.startsWith("?"))
                    .map(line -> {
                        Pair<Set<Literal>, List<String>> pair = parseAmieLine(line);
                        return new Clause(pair.r);
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("error while parsing the file");
    }

    public Pair<Set<Literal>, List<String>> parseAmieLine(String line) {
        Map<String, Variable> map = new HashMap<>();
        String[] splitted = line.split("\\s+");
        Set<Literal> literals = Sugar.set();
        int idx = 0;
        boolean headPart = false;
        while (true) {
            if ("=>".equals(splitted[idx])) {
                idx++;
                headPart = true;
                continue;
            }

            String predicate = stripLRangles(splitted[idx + 1]);

            if (predicate.startsWith(turtleOneArgumentLiteralPrefix)) {
                predicate = predicate.substring(turtleOneArgumentLiteralPrefix.length());
                literals.add(new Literal(transformPredicate(predicate), !headPart,
                        Sugar.list(getVariable(splitted[idx], map))));
            } else {
                literals.add(new Literal(transformPredicate(predicate),
                        !headPart,
                        Sugar.list(getVariable(splitted[idx], map), getVariable(splitted[idx + 2], map))));
            }


            idx += 3;
            if (headPart) {
                break;
            }
        }
        List<String> rest = IntStream.range(idx, splitted.length).mapToObj(index -> splitted[index]).collect(Collectors.toList());
        return new Pair<>(literals, rest);
    }

    private Variable getVariable(String amieString, Map<String, Variable> factory) {
        if (!factory.containsKey(amieString)) {
            factory.put(amieString, Variable.construct("V" + factory.size()));
        }
        return factory.get(amieString);
    }


    private String transformGroundArgument(String s) {
        if (!cache.containsKey(s)) {
            cache.put(s, "id" + cache.size());
        }
        return cache.get(s);
        // or it can map each argument to a shorter id
        //return "c" + s.replace("'", "_ap_")
        //        .replace("\"", "_dap_");
    }

    private String transformPredicate(String s) {
        // all is assumed to lowercased
        return s.replace("'", "_ap_")
                .replace("\"", "_dap_");
    }

    public Set<Literal> turleEvidenceToLogic(Path path) {
        try {
            return Files.lines(path)
                    .filter(line -> line.trim().length() > 0)
                    .map(line -> {
                        line = line.trim();
                        String[] splitted = line.substring(0, line.length() - 1).split("\t"); // removing the ending dot
                        String subject = stripLRangles(splitted[0]);
                        String predicate = stripLRangles(splitted[1]);
                        String object = stripLRangles(splitted[2]);
                        assert splitted.length == 3;
                        return Literal.parseLiteral(transformPredicate(predicate) + "(" + transformGroundArgument(subject) + "," + transformGroundArgument(object) + ")");
                    }).collect(Collectors.toSet());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("crash while parsing the file");
    }

    private String stripLRangles(String s) {
        s = s.trim();
        if(s.startsWith("<") && s.endsWith(">")){
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("(", "_openingBracket_")
                .replace(")", "_closingBracket_")
                .replace(",", "_comma_");
    }

    public List<String> logicToTurtle(Path source) {
        return logicToTurtle(source, Paths.get(source.toString() + ".turtle"));
    }

    private List<String> logicToTurtle(Path source, Path output) {
        Set<Literal> evidence = LogicUtils.loadEvidence(source);
        List<String> turtle = Sugar.parallelStream(evidence).map(this::logicToTurtle).collect(Collectors.toList());
        try {
            //System.out.println("storing to\t" + output + "\n" + turtle);
            Files.write(output, turtle);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return turtle;
    }

    private String logicToTurtle(Literal literal) {
        if (literal.arity() == 2) {
            return wrapToTurtle(literal.arguments()[0]) + "\t" + wrapToTurtle(literal.predicate()) + "\t" + wrapToTurtle(literal.arguments()[1]);
        } else if (literal.arity() == 1) {
            String predicate = turtleOneArgumentLiteralPrefix + literal.predicate();
            return wrapToTurtle(literal.arguments()[0])
                    + "\t" + wrapToTurtle(predicate)
                    + "\t" + wrapToTurtle(predicate);
        }
        System.out.println(literal);
        System.out.println("reformattor not implemented to transform predictaset of different arity than 2 to turtle format");
        throw new IllegalStateException();// NotImplementedException();
    }

    private String wrapToTurtle(Object o) {
        if (o instanceof Predicate) {
            return "<" + o.toString() + ">";
        } else if (o instanceof Term) {
            if (o instanceof ida.ilp.logic.Function) {
                throw new IllegalStateException();// NotImplementedException();
            }
            return "<" + ((Term) o).name() + ">";
        }

        return "<" + o.toString() + ">";
    }

    public void oneLine(Path path, boolean example) {
        Set<Literal> evidence = LogicUtils.loadEvidence(path);
        try {
            Files.write(Paths.get(path.toString() + ".oneLine"),
                    Sugar.list((example ? "+ " : "") + evidence.stream().map(Object::toString).collect(Collectors.joining(", "))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Literal mlnsToLogic(Literal literal) {
        if (literal.arity() == 3) {
            return new Literal(firstLowercase(literal.get(0).toString()), false
                    , toConst(literal.get(1))
                    , toConst(literal.get(2))
            );
        } else if (literal.arity() == 2) {
//            return new Literal("myOwnProperty_" + firstLowercase(literal.get(1).toString()), false
//                    , toConst(literal.get(0))
//                    , toConst(literal.get(1))
            //);
            return new Literal(firstLowercase(literal.get(1).toString()), false
                    , toConst(literal.get(0))
            );
        }
        System.out.println("mlnsToLogic not implemented for arity of " + literal.arity());
        throw new IllegalStateException();// NotImplementedException();
    }

    private Constant toConst(Term term) {
        return Constant.construct(firstLowercase(term.toString()));
    }

    private String firstLowercase(String s) {
        return s.substring(0, 1).toLowerCase() + s.substring(1);
    }

    public Set<Literal> mlnsToLogic(Path path, boolean store) {
        System.out.println("created only for kinship and nations");
        Set<Literal> literals = LogicUtils.loadEvidence(path);

        literals = Sugar.parallelStream(literals)
                .map(this::mlnsToLogic)
                .collect(Collectors.toSet());

        if (store) {
            try {
                Files.write(Paths.get(path + ".db"), literals.stream().map(Object::toString).collect(Collectors.toList()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return literals;
    }


    private List<String> filterTVSbyPredicates(Path tvsSource, Path predicatesFilter) {
        try {
            Set<String> predicates = Files.lines(predicatesFilter).filter(line -> line.trim().length() > 0)
                    .map(line -> Predicate.construct(line.trim()).getName())
                    .collect(Collectors.toSet());

            return Files.lines(tvsSource)
                    .filter(line -> {
                        String[] splitted = line.split("\t");
                        if (splitted.length == 3) {
                            if (predicates.contains(stripLRangles(splitted[1]))) {
                                return true;
                            }
                        }
                        return false;
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("some problem while reading the file");
    }


    public static Reformatter create() {
        return new Reformatter();
    }


    public static void main(String[] args) throws IOException {
        Reformatter r = Reformatter.create();


        /* * /
//        Path source = Paths.get("C:\\data\\school\\development\\amie\\kbs\\yago2\\yago2core.10kseedsSample.compressed.notypes.tsv");
        Path source = Paths.get("C:\\data\\school\\development\\amie\\yago2\\yago2core_facts.clean.notypes.tsv");
        Set<Literal> literals = r.turleEvidenceToLogic(source);

        Path output = Paths.get("C:\\data\\school\\development\\amie\\yago2\\coreFacts.logic");
        Files.write(output, literals.stream().map(Object::toString).collect(Collectors.toList()));
        Path outputDict = Paths.get("C:\\data\\school\\development\\amie\\yago2\\coreFacts.logic.dictionary");
        Files.write(outputDict, r.cache.entrySet().stream().map(e -> e.getKey() + "\t" + e.getValue()).sorted().collect(Collectors.toList()));
        //r.cache.entrySet().stream().map(e -> e.getKey() + "\t" + e.getValue()).sorted().forEach(System.out::println);
//        Path output = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac\\yago2\\yago2core.10kseedsSample.compressed.notypes");
//        Files.write(output, literals.stream().map(Object::toString).collect(Collectors.toList()));
//        Path output = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac\\yago2\\yago2core.10kseedsSample.compressed.notypes.oneLine");
//        Files.write(output, Sugar.list(literals.stream().map(Object::toString).collect(Collectors.joining(", "))));
        /**/

//         amie output to logic
        //Path source = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\mlns\\kinships\\rulesSelected.amieOutput");
//        Path source = Paths.get("C:\\data\\school\\development\\amie\\sortedYago2rules.txt");
//        Set<Clause> rules = r.loadAmieOutput(source);
//        rules.forEach(System.out::println);

        // logic to turtle
//        Path source = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\mlns\\nations\\train.db");
//        r.logicToTurtle(source);

//        r.oneLine(Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\umls-ntp\\train.db"),true);
//        r.oneLine(Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\nations-ntp\\train.db"),true);
//        r.oneLine(Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\kinships-ntp\\train.db"),true);

        //r.oneLine(Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\kinships-ntp\\train.db"),true);

//        r.mlnsToLogic(Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\mlns\\kinships\\kinships.txt"), true);
//        r.mlnsToLogic(Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\umls\\umls.txt"), true);


        /*  yago sub-dataset creation * /
        Path source = Paths.get("C:\\data\\school\\development\\amie\\yago2\\yago2core_facts.clean.notypes.tsv");
        for (String folder : Sugar.list("yago-film", "yago-musician", "yago-people", "yago-politics", "yago-states")) {
//            prepareYago(source, Paths.get("..", "datasets", folder, "predicates.txt"));

            // amie rules to logic
            Set<Clause> rules = r.loadAmieOutput(Paths.get(".", "pac", folder, "amieRuleDepth4MS.amieOutput"));
            Files.write(Paths.get(".", "pac", folder, "amieRuleDepth4MS.logic"), rules.stream().map(Clause::toString).collect(Collectors.toList()));
        }/**/

//        binaryRelationships();

        /*
        String domain = "protein";
        Path train = Paths.get("..", "datasets", domain, "train.symmetric.db");
        List<String> alsoConvert = Sugar.list(
                "test.symmetric.db"
                , "train.symmetric.db.oneLine"
        );
        Path storedTypes = Paths.get(train.getParent().toString(), "typing.txt");
        r.addTypes(train, storedTypes, alsoConvert);
        */


        //Path source = Paths.get("..", "datasets", "protein", "train.symmetric.db.typed");
        //Path target = Paths.get("C:\\data\\school\\development\\amie\\protein.train.symmetric.db.untyped.tsv");
        //r.logicToTurtle(source, target);
        Path output = Paths.get("C:\\data\\school\\development\\amie\\protein.symmetric.postprocess.amieOutput");
        Collection<Clause> rules = r.loadAmieOutput(output);
        rules.stream().map(HornClause::create).forEach(System.out::println);
    }

    {
        System.out.println("delat to potom v jejich transduktivnim (nebo jak se mu rika) nastaveni");
    }

    public Path addTypes(Path trainEvidence, Path storedTypes, List<String> convert) {
        Path dir = trainEvidence.getParent();

        //Path storedTypes = Paths.get(dir.toString(), "typing.txt");

        Map<Pair<Predicate, Integer>, String> st = null;
        TypesInducer inducer = new TypesInducer();
        if (!storedTypes.toFile().exists()) {
            Set<Literal> facts = LogicUtils.loadEvidence(trainEvidence);
            Map<Pair<Predicate, Integer>, Type> typing = inducer.rename(inducer.induce(facts));
            st = TypesInducer.simplify(typing);
            System.out.println("storing types to\t" + storedTypes);
            TypesInducer.store(typing, storedTypes);
        } else {
            System.out.println("loading types from\t" + storedTypes);
            st = TypesInducer.load(storedTypes);
        }
        Map<Pair<Predicate, Integer>, String> simplified = st;


        Sugar.union(convert, trainEvidence.getFileName().toString()).stream().forEach(file -> {
            Path source = Paths.get(dir.toString(), file);
            Path output = Paths.get(dir.toString(), file + ".typed");

            if (!output.toFile().exists()) {
                if (file.endsWith(".oneLine")) {
                    try {
                        output = Paths.get(dir.toString(), file.substring(0, file.length() - ".oneLine".length()) + ".typed.oneLine");
                        MEDataset med = MEDataset.create(source, Matching.THETA_SUBSUMPTION);
                        MEDataset typed = inducer.typeDataset(med, simplified);
                        System.out.println("type introduction in\t" + output);
                        Files.write(output, typed.asOutput());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Set<Literal> world = LogicUtils.loadEvidence(source);
                    try {
                        System.out.println("type introduction in\t" + output);
                        Files.write(output, world.stream().map(l -> LogicUtils.addTyping(l, simplified).toString()).collect(Collectors.toList()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        return Paths.get(trainEvidence.toString() + ".typed");
    }

    private static void prepareYago(Path tvsSource, Path predicatesFilter) throws IOException {
        Reformatter r = Reformatter.create();
        Path outputDir = Paths.get(predicatesFilter.toFile().getParent().toString());
        // filtering
        Path filteredFile = Paths.get(outputDir.toString(), "data.turtle");
        if (!filteredFile.toFile().exists()) {
            List<String> filtered = r.filterTVSbyPredicates(tvsSource, predicatesFilter);
            Files.write(filteredFile, filtered);
        }

        // to logic
        Path logicData = Paths.get(outputDir.toString(), "data.db");
        if (!logicData.toFile().exists() || !Paths.get(outputDir.toString(), "data.logic.turtle.dictionary.txt").toFile().exists()) {
            Set<Literal> logic = r.turleEvidenceToLogic(filteredFile);
            Files.write(logicData, logic.stream().map(Literal::toString).collect(Collectors.toList()));
            Files.write(Paths.get(outputDir.toString(), "data.logic.turtle.dictionary.txt"), r.cache.entrySet().stream().map(e -> e.getKey() + "\t" + e.getValue()).sorted().collect(Collectors.toList()));
        }

        // split train test
        double ratio = 0.5;
        Path train = Paths.get(outputDir.toString(), "train.db");
        Path test = Paths.get(outputDir.toString(), "test.db");
        if (!train.toFile().exists() || test.toFile().exists()) {
            Pair<Set<Literal>, Set<Literal>> trainTest = DatasetSampler.create(DatasetSampler.UNIFORM_RANDOM, false, false).trainTestSplit(logicData, ratio);
            Files.write(train, trainTest.getR().stream().map(Object::toString).collect(Collectors.toList()));
            Files.write(test, trainTest.getS().stream().map(Object::toString).collect(Collectors.toList()));

        }
        // train to one line for constraints learner
        Files.write(Paths.get(outputDir.toString(), "train.db.oneLine"), Sugar.list("+ " + LogicUtils.loadEvidence(train).stream().map(Object::toString).collect(Collectors.joining(", "))));


        // train to turtle
        r.logicToTurtle(train);


        // stats
        Utils u = Utils.create();
        System.out.println("\nresulting stats for\n\t" + predicatesFilter);
        System.out.println("overall predicates\t" + u.predicates(logicData).size());
        System.out.println("overall evidence size\t" + LogicUtils.loadEvidence(logicData).size());
        System.out.println("overall constants size\t" + LogicUtils.constants(LogicUtils.loadEvidence(logicData)).size());

        System.out.println("train evidence size\t" + LogicUtils.loadEvidence(train).size());
        System.out.println("train constants size\t" + LogicUtils.constants(LogicUtils.loadEvidence(train)).size());

        System.out.println("test evidence size\t" + LogicUtils.loadEvidence(test).size());
        System.out.println("test constants size\t" + LogicUtils.constants(LogicUtils.loadEvidence(test)).size());

        System.out.println("\n");
    }

    /*
    smysl to ma jenom pro nations a mozna uwcs (ale tam se zase prijde o hodne informace
    domain	nations
    166	55
    domain	kinships
    25	25
    domain	uwcs
    15	4
    domain	protein
    7	7
    domain	umls
    45	45
     */
    private static void binaryRelationships() throws IOException {
        Function<String, String> mapper = (line) -> Clause.parse(line).literals().stream().filter(l -> 2 == l.arity()).map(Object::toString).collect(Collectors.joining(", "));

        Utils u = Utils.create();
        for (String domain : Sugar.list("nations", "kinships", "uwcs", "protein", "umls")) {
            System.out.println("domain\t" + domain);
            Path base = Paths.get("..", "datasets", domain);
            Path next = Paths.get("..", "datasets", domain + "A2");

            if (!next.toFile().exists()) {
                next.toFile().mkdirs();
            }

            for (String file : Sugar.list("test.db", "train.db", "train.db.oneLine")) {
                Path src = Paths.get(base.toString(), file);
                Path trg = Paths.get(next.toString(), file);

                if (trg.toFile().exists()) {
                    continue;
                }

                Files.write(trg, Files.lines(src)
                        .filter(line -> line.trim().length() > 0)
                        .map(line -> {
                            if (file.endsWith(".oneLine")) {
                                String[] splitted = line.split("\\s+", 2);
                                return splitted[0] + "\t" + mapper.apply(splitted[1]);
                            }
                            return mapper.apply(line);
                        })
                        .filter(line -> line.length() > 0)
                        .collect(Collectors.toList())
                );

            }

            Set<Predicate> all = u.predicates(Sugar.union(LogicUtils.loadEvidence(Paths.get(base.toString(), "train.db")), LogicUtils.loadEvidence(Paths.get(base.toString(), "test.db"))));
            Set<Predicate> a2 = u.predicates(Sugar.union(LogicUtils.loadEvidence(Paths.get(next.toString(), "train.db")), LogicUtils.loadEvidence(Paths.get(next.toString(), "test.db"))));
            System.out.println(all.size() + " " + a2.size());
        }
    }


    public void convertToLrnn20(Path trainData, Path queryData, Path outputDir) throws IOException {
        Path outputSample = Paths.get(outputDir.toString(), "examples.txt");
        Set<Literal> trainEvidence = LogicUtils.loadEvidence(trainData);

        // sample formatting
        Set<String> attributes = Sugar.set();
        Set<String> relations = Sugar.set();
        Set<String> entities = Sugar.set();
        List<String> sample = trainEvidence.stream().map(literal -> {
            if (literal.arity() == 1) {
                String entity = literal.get(0).toString();
                entities.add(entity);
                String attribute = literal.getPredicate().getR();
                attributes.add(attribute);
                return LogicUtils.toRNotation(literal);
            } else if (literal.arity() == 2) {
                String arg1 = literal.get(0).toString();
                String arg2 = literal.get(1).toString();
                entities.add(arg1);
                entities.add(arg2);
                String relation = literal.getPredicate().r;
                relations.add(relation);
                return LogicUtils.toRNotation(literal);
            } else {
                throw new IllegalStateException();// NotImplementedException();
            }
        }).collect(Collectors.toList());
        attributes.forEach(attribute -> sample.add("att(" + attribute + ")"));
        entities.forEach(entity -> sample.add("ent(" + entity + ")"));
        relations.forEach(relation -> sample.add("rel(" + relation + ")"));
        List<String> sample2 = sample.stream().map(str -> str + ", ").collect(Collectors.toList());


        String last = sample.get(sample2.size() - 1);
        sample2.remove(sample2.size() - 1);
        last = last.substring(0, last.length()) + ".";
        sample2.add(last);
        Files.write(outputSample, sample2);

        // TODO nektere z tech casti budou chtit predelat aby generovane veci davaly smyslu vuci train-test-val ;)
        // queries generation
        Utils u = Utils.create();
        Map<Literal, Set<Literal>> queries = u.loadHitsTest(queryData);
        Set<Literal> positive = Sugar.set();
        Set<Literal> negative = Sugar.set();
        queries.forEach((key, value) -> {
            positive.add(key);
            negative.addAll(value);
        });

        List<String> toPredict = Sugar.list();
        trainEvidence.forEach(literal -> toPredict.add("1.0 " + LogicUtils.toRNotation(literal, "predict") + "."));
        positive.forEach(literal -> toPredict.add("1.0 " + LogicUtils.toRNotation(literal, "predict") + "."));
        toPredict.add("");
        toPredict.add("");
        negative.forEach(literal -> toPredict.add("0.0 " + LogicUtils.toRNotation(literal, "predict") + "."));
        Files.write(Paths.get(outputDir.toString(), "queries.txt"), toPredict);

        // embeddings generation
        int embeddingSize = 3;
        List<String> embeddings = Sugar.list();
        entities.forEach(entity -> embeddings.add("{" + embeddingSize + "} embedding_entity(" + entity + ") :- entity(" + entity + ")."));
        attributes.forEach(attribute -> embeddings.add("{" + embeddingSize + "} attribute_entity(" + attribute + ") :- attribute(" + attribute + ")."));
        relations.forEach(relation -> embeddings.add("{" + embeddingSize + "} embedding_relations(" + relation + ") :- relation(" + relation + ")."));

        Path p2 = Paths.get(outputDir.toString(), "templates");
        if (!p2.toFile().exists()) {
            p2.toFile().mkdirs();
        }
        Files.write(Paths.get(p2.toString(), "embeddings.txt"), embeddings);
        // basic template generation


    }

    // non nice since these methods below do reformatting and (negative) sample generation, which should be in a different class
    public void convertToLrnn202(Path trainData, Path outputDir) throws IOException {
        Path outputSample = Paths.get(outputDir.toString(), "examples.txt");
        Set<Literal> trainEvidence = LogicUtils.loadEvidence(trainData);

        // sample formatting
        Set<String> attributes = Sugar.set();
        Set<String> relations = Sugar.set();
        Set<String> entities = Sugar.set();
        Set<Literal> positive = Sugar.set();

        List<String> sample = trainEvidence.stream().map(literal -> {
            if (literal.arity() == 1) {
                String entity = literal.get(0).toString();
                entities.add(entity);
                String attribute = literal.getPredicate().getR();
                attributes.add(attribute);
                return LogicUtils.toRNotation(literal);
            } else if (literal.arity() == 2) {
                String arg1 = literal.get(0).toString();
                String arg2 = literal.get(1).toString();
                entities.add(arg1);
                entities.add(arg2);
                String relation = literal.getPredicate().r;
                relations.add(relation);
                positive.add(literal);
                return LogicUtils.toRNotation(literal);
            } else {
                throw new IllegalStateException();// NotImplementedException();
            }
        }).collect(Collectors.toList());
        attributes.forEach(attribute -> sample.add("a(" + attribute + ")"));
        entities.forEach(entity -> sample.add("e(" + entity + ")"));
        relations.forEach(relation -> sample.add("re(" + relation + ")"));


        // TODO nektere z tech casti budou chtit predelat aby generovane veci davaly smyslu vuci train-test-val ;)
        // queries generation -- zatim je naivni, protoze nebere v uvahu test a val -- nekontroluje oproti nim


        List<String> sortedEntities = Sugar.listFromCollections(entities);
        Random r = new Random();
        int howManyCorruptedPerPositive = 2;
        Set<Literal> negative = Sugar.set();
        for (Literal literal : positive) {
            for (int corruptionRun = 0; corruptionRun < howManyCorruptedPerPositive; corruptionRun += 2) {
                for (int ithTry = 0; ithTry < 10; ithTry++) {
                    String entity = sortedEntities.get(r.nextInt(sortedEntities.size()));
                    Literal corrupted = new Literal(literal.predicate(), Sugar.list(new Constant(entity), literal.get(1)));
                    if (trainEvidence.contains(corrupted)) {
                        continue;
                    }
                    negative.add(corrupted);
                    break;
                }
                for (int ithTry = 0; ithTry < 10; ithTry++) {
                    String entity = sortedEntities.get(r.nextInt(sortedEntities.size()));
                    Literal corrupted = new Literal(literal.predicate(), Sugar.list(literal.get(0), new Constant(entity)));
                    if (trainEvidence.contains(corrupted)) {
                        continue;
                    }
                    negative.add(corrupted);
                    break;
                }
            }
        }


        List<String> toPredict = Sugar.list();
        trainEvidence.forEach(literal -> toPredict.add("1.0 " + LogicUtils.toRNotation(literal, "p") + "."));
        positive.forEach(literal -> {
            toPredict.add("1.0 " + LogicUtils.toRNotation(literal, "p") + ".");
            sample.add(LogicUtils.toRNotation(literal, "q"));
        });
        negative.forEach(literal -> {
            toPredict.add("0.0 " + LogicUtils.toRNotation(literal, "p") + ".");
            sample.add(LogicUtils.toRNotation(literal, "q"));
        });
        Files.write(Paths.get(outputDir.toString(), "queries.txt"), toPredict);

        List<String> sample2 = sample.stream().map(str -> str + ", ").collect(Collectors.toList());
        String last = sample.get(sample2.size() - 1);
        sample2.remove(sample2.size() - 1);
        last = last.substring(0, last.length()) + ".";
        sample2.add(last);
        Files.write(outputSample, sample2);


    }


    public void convertToLrnn2021(Path trainData, Path outputDir) throws IOException {

        Set<Literal> trainEvidence = LogicUtils.loadEvidence(trainData);

        RenamingFactory relationRenaming = RenamingFactory.create("r");
        RenamingFactory entityRenaming = RenamingFactory.create("e");

        // sample formatting
        //Set<String> attributes = Sugar.set();
        MultiMap<Pair<String, String>, String> positive = new MultiMap<>();

        List<String> sample = trainEvidence.stream().map(literal -> {
            if (literal.arity() == 1) {
                /*
                String entity = entityRenaimg.get(literal.get(0).toString());
                String attribute = attributeRenaming.get(literal.getPredicate().r);

                return LogicUtils.toRNotation(literal);
                */
                throw new IllegalStateException();// NotImplementedException();
            } else if (literal.arity() == 2) {
                String arg1 = entityRenaming.get(literal.get(0).toString());
                String arg2 = entityRenaming.get(literal.get(1).toString());
                String relation = relationRenaming.get(literal.getPredicate().r);
                positive.put(new Pair(relation, arg1), arg2);
                return LogicUtils.toRNotation(new Triple<>(arg1, relation, arg2), "r");
            } else {
                throw new IllegalStateException();// NotImplementedException();
            }
        }).collect(Collectors.toList());
        //attributes.forEach(attribute -> sample.add("a(" + attribute + ")"));
        entityRenaming.getValues().forEach(entity -> sample.add("e(" + entity + ")"));
        relationRenaming.getValues().forEach(relation -> sample.add("re(" + relation + ")"));

        List<String> sortedEntities = Sugar.listFromCollections(entityRenaming.getValues());


        for (Integer perPositive : Sugar.list(1, 10, 50, 100, entityRenaming.getValues().size())) {
            Path dir = Paths.get(outputDir.toString() + "_" + perPositive);
            Path outputSample = Paths.get(dir.toString(), "examples.txt");
            if (!dir.toFile().exists()) {
                dir.toFile().mkdirs();
            }
            Set<String> currentSample = Sugar.set();
            Set<String> negative = Sugar.set();
            Set<String> positives = Sugar.set();

            positive.entrySet().forEach(entry -> {
                String relation = entry.getKey().r;
                String arg1 = entry.getKey().s;
                entry.getValue().forEach(arg2 -> positives.add(LogicUtils.toRNotation(new Triple<>(arg1, relation, arg2), "p")));

                int limit = entry.getValue().size() * perPositive;
                for (String entity : sortedEntities) {
                    if (limit <= 0) {
                        break;
                    }
                    if (entry.getValue().contains(entity)) {
                        continue;
                    }
                    negative.add(LogicUtils.toRNotation(new Triple<>(arg1, relation, entity), "p"));
                    limit--;
                }
            });

            List<String> toPredict = Sugar.list();
            positives.forEach(literal -> {
                toPredict.add("1.0 " + literal + ".");
                Literal l = Literal.parseLiteral(literal);
                currentSample.add(LogicUtils.toRNotation(new Triple<>(l.get(0).toString(), l.get(1).toString(), l.get(2).toString()), "g"));
            });
            negative.forEach(literal -> {
                toPredict.add("0.0 " + literal + ".");
                Literal l = Literal.parseLiteral(literal);
                currentSample.add(LogicUtils.toRNotation(new Triple<>(l.get(0).toString(), l.get(1).toString(), l.get(2).toString()), "g"));
            });
            Files.write(Paths.get(dir.toString(), "queries.txt"), toPredict);

            List<String> sample2 = sample.stream().map(str -> str + ",").collect(Collectors.toList());
            currentSample.forEach(s -> sample2.add(s + ","));

            String last = sample2.get(sample2.size() - 1);
            sample2.remove(sample2.size() - 1);
            last = last.substring(0, last.length() - 1) + ".";
            sample2.add(last);

            Files.write(outputSample, sample2);
        }
    }

    public void convertLogicToAnonymous(Path trainData, Path outputDir) throws IOException {
        if (!outputDir.toFile().exists()) {
            outputDir.toFile().mkdirs();
        }
        List<String> trainSamples = Sugar.list();
        List<String> valSamples = Sugar.list();
        List<String> testSamples = Sugar.list();
        RenamingFactory relationRenaming = RenamingFactory.create("r");
        RenamingFactory entityRenaming = RenamingFactory.create("e");
        for (Pair<String, List<String>> pair : Sugar.list(new Pair<>("train.nl", trainSamples),
                new Pair<>("dev.nl", valSamples),
                new Pair<>("test.nl", testSamples))) {
            Path file = Paths.get(trainData.toString(), pair.r);
            List<String> collector = pair.getS();
            Set<Literal> trainEvidence = LogicUtils.loadEvidence(file);
            trainEvidence.forEach(literal -> {
                if (literal.arity() == 1) {
                /*
                String entity = entityRenaimg.get(literal.get(0).toString());
                String attribute = attributeRenaming.get(literal.getPredicate().r);

                return LogicUtils.toRNotation(literal);
                */
                    throw new IllegalStateException();// NotImplementedException();
                } else if (literal.arity() == 2) {
                    String arg1 = entityRenaming.get(literal.get(0).toString());
                    String arg2 = entityRenaming.get(literal.get(1).toString());
                    String relation = relationRenaming.get(literal.getPredicate().r);
                    collector.add(arg1 + "\t" + relation + "\t" + arg2);
                } else {
                    throw new IllegalStateException();// NotImplementedException();
                }
            });
            Files.write(Paths.get(outputDir.toString(), pair.r), collector);
        }

        Files.write(Paths.get(outputDir.toString(), "origin.dict"), Sugar.list(relationRenaming, entityRenaming).stream()
                .flatMap(RenamingFactory::serialize)
                .collect(Collectors.toList()));
        Files.write(Paths.get(outputDir.toString(), "relation.dict"), relationRenaming.toSortedDict());
        Files.write(Paths.get(outputDir.toString(), "entity.dict"), entityRenaming.toSortedDict());

    }


    public void convertToLrnn2021(Path trainData, Path devData, Path testData, Path outputDir, String generationMode) throws IOException {

        Set<Literal> trainEvidence = LogicUtils.loadEvidence(trainData);
        Set<Literal> devEvidence = LogicUtils.loadEvidence(devData);
        Set<Literal> testEvidence = LogicUtils.loadEvidence(testData);

        RenamingFactory relationRenaming = RenamingFactory.create("r");
        RenamingFactory entityRenaming = RenamingFactory.create("e");

        // sample formatting
        //Set<String> attributes = Sugar.set();
        MultiMap<Pair<String, String>, String> train = new MultiMap<>();
        MultiMap<Pair<String, String>, String> validation = new MultiMap<>();
        MultiMap<Pair<String, String>, String> test = new MultiMap<>();

        Sugar.list(new Pair<>(trainEvidence, train),
                new Pair<>(devEvidence, validation),
                new Pair<>(testEvidence, test))
                .stream().forEach(pair -> {
            Set<Literal> evidence = pair.getR();
            MultiMap<Pair<String, String>, String> collector = pair.getS();
            evidence.stream().forEach(literal -> {
                if (literal.arity() == 1) {
                /*
                String entity = entityRenaimg.get(literal.get(0).toString());
                String attribute = attributeRenaming.get(literal.getPredicate().r);

                return LogicUtils.toRNotation(literal);
                */
                    throw new IllegalStateException();// NotImplementedException();
                } else if (literal.arity() == 2) {
                    String arg1 = entityRenaming.get(literal.get(0).toString());
                    String arg2 = entityRenaming.get(literal.get(1).toString());
                    String relation = relationRenaming.get(literal.getPredicate().r);
                    collector.put(new Pair<>(arg1, relation), arg2);
                } else {
                    throw new IllegalStateException();// NotImplementedException();
                }
            });
        });
        //attributes.forEach(attribute -> sample.add("a(" + attribute + ")"));
        Set<String> common = Sugar.set();
        entityRenaming.getValues().forEach(entity -> common.add("e(" + entity + ")"));
        relationRenaming.getValues().forEach(relation -> common.add("re(" + relation + ")"));

        List<String> sortedEntities = Sugar.listFromCollections(entityRenaming.getValues());

        Set<String> trainEvd = train.entrySet().stream().flatMap(entry -> {
            String arg1 = entry.getKey().r;
            String relation = entry.getKey().s;
            return entry.getValue().stream().map(arg2 -> LogicUtils.toRNotation(new Triple<>(arg1, relation, arg2), "r"));
        }).collect(Collectors.toSet());

        Set<String> valEvd = validation.entrySet().stream().flatMap(entry -> {
            String arg1 = entry.getKey().r;
            String relation = entry.getKey().s;
            return entry.getValue().stream().map(arg2 -> LogicUtils.toRNotation(new Triple<>(arg1, relation, arg2), "r"));
        }).collect(Collectors.toSet());

        Set<String> testEvd = test.entrySet().stream().flatMap(entry -> {
            String arg1 = entry.getKey().r;
            String relation = entry.getKey().s;
            return entry.getValue().stream().map(arg2 -> LogicUtils.toRNotation(new Triple<>(arg1, relation, arg2), "r"));
        }).collect(Collectors.toSet());

        // generation of negative samples for val and test
        Set<String> valQuer = Sugar.set();
        Set<String> testQuer = Sugar.set();
        Sugar.list(new Pair<>(valQuer, validation), new Pair<>(testQuer, test)).forEach(pair -> {
            Set<String> collector = pair.r;
            MultiMap<Pair<String, String>, String> positiveQueries = pair.s;
            positiveQueries.entrySet().forEach(entry -> {
                String arg1 = entry.getKey().r;
                String rel = entry.getKey().s;
                entry.getValue().forEach(arg2 -> {
                    collector.add("1.0\t" + LogicUtils.toRNotation(new Triple<>(arg1, rel, arg2), "p") + ".");
                    sortedEntities.forEach(entity -> {
                        if (!train.get(new Pair<>(arg1, rel)).contains(entity)
                                && !test.get(new Pair<>(arg1, rel)).contains(entity)
                                && !validation.get(new Pair<>(arg1, rel)).contains(entity)) {
                            collector.add("0.0\t" + LogicUtils.toRNotation(new Triple<>(arg1, rel, entity), "p") + ".");
                        }

                        if (!train.get(new Pair<>(entity, rel)).contains(arg2)
                                && !test.get(new Pair<>(entity, rel)).contains(arg2)
                                && !validation.get(new Pair<>(entity, rel)).contains(arg2)) {
                            collector.add("0.0\t" + LogicUtils.toRNotation(new Triple<>(entity, rel, arg2), "p") + ".");
                        }
                    });
                });
            });

        });
        List<String> validationQueries = Sugar.listFromCollections(valQuer);
        List<String> testQueries = Sugar.listFromCollections(testQuer);
        Collections.sort(validationQueries);
        Collections.sort(testQueries);

        // generation of negative samples for train
//        for (Integer perPositive : Sugar.list(1, 10, 25)) {   // b
//        for (Integer perPositive : Sugar.list(1, 10, 50, entityRenaming.getValues().size())) { // t
        for (Integer perPositive : Sugar.list(1, 2, 10)) {    // r
            Path dir = Paths.get(outputDir.toString() + "_" + perPositive);
            if (!dir.toFile().exists()) {
                dir.toFile().mkdirs();
            }
            List<String> trainQueries = null;
            if ("r".equals(generationMode)) { // completely random
                trainQueries = randomGeneration(perPositive, train, validation, test, sortedEntities);
            } else if ("t".equals(generationMode)) { // tucker
                trainQueries = tuckerGeneration(perPositive, train, validation, test, sortedEntities);
            } else if ("b".equals(generationMode)) { // change only in one of the argument
                trainQueries = oneChangeGeneration(perPositive, train, validation, test, sortedEntities);
            } else {
                throw new IllegalArgumentException();
            }

            storeEvidenceAndQueries(common, trainEvd, trainQueries, Paths.get(dir.toString(), "trainExamples.txt"), Paths.get(dir.toString(), "trainQueries.txt"));
            storeEvidenceAndQueries(common, trainEvd, validationQueries, Paths.get(dir.toString(), "valExamples.txt"), Paths.get(dir.toString(), "valQueries.txt"));
            storeEvidenceAndQueries(common, trainEvd, testQueries, Paths.get(dir.toString(), "testExamples.txt"), Paths.get(dir.toString(), "testQueries.txt"));
            Files.write(Paths.get(dir.toString(), "dict.txt"), Sugar.list(relationRenaming, entityRenaming).stream()
                    .flatMap(RenamingFactory::serialize)
                    .collect(Collectors.toList()));
        }
    }

    // when generating a corrupted triple, only one only one entity is corrupted
    // it generates twice the number perPositive (one for arg1 and the second for arg2 corruption)
    private List<String> oneChangeGeneration(Integer perPositive, MultiMap<Pair<String, String>, String> train, MultiMap<Pair<String, String>, String> validation, MultiMap<Pair<String, String>, String> test, List<String> sortedEntities) {
        List<String> output = Sugar.list();
        List<String> ents = Sugar.listFromCollections(sortedEntities);
        Random rnd = new Random();

        train.entrySet().forEach(entry -> {
            String relation = entry.getKey().s;
            String arg1 = entry.getKey().r;
            entry.getValue().forEach(arg2 -> {
                output.add("1.0\t" + LogicUtils.toRNotation(new Triple<>(arg1, relation, arg2), "p") + ".");

                int added = 0;
                for (int round = 0; round < perPositive * 10 && added < perPositive; round++) {
                    String ent2 = ents.get(rnd.nextInt(ents.size()));
                    if (!train.get(new Pair<>(arg1, relation)).contains(ent2)
                            && !validation.get(new Pair<>(arg1, relation)).contains(ent2)
                            && !test.get(new Pair<>(arg1, relation)).contains(ent2)) {
                        output.add("0.0\t" + LogicUtils.toRNotation(new Triple<>(arg1, relation, ent2), "p") + ".");
                        added++;
                    }
                }

                added = 0;
                for (int round = 0; round < perPositive * 10 && added < perPositive; round++) {
                    String ent2 = ents.get(rnd.nextInt(ents.size()));
                    if (!train.get(new Pair<>(ent2, relation)).contains(arg2)
                            && !validation.get(new Pair<>(ent2, relation)).contains(arg2)
                            && !test.get(new Pair<>(ent2, relation)).contains(arg2)) {
                        output.add("0.0\t" + LogicUtils.toRNotation(new Triple<>(ent2, relation, arg2), "p") + ".");
                        added++;
                    }
                }


            });
        });
        return output;
    }

    private List<String> tuckerGeneration(Integer perPositive, MultiMap<Pair<String, String>, String> train, MultiMap<Pair<String, String>, String> validation, MultiMap<Pair<String, String>, String> test, List<String> sortedEntities) {
        List<String> output = Sugar.list();
        List<String> ents = Sugar.listFromCollections(sortedEntities);
        Random rnd = new Random();

        train.entrySet().forEach(entry -> {
            String arg1 = entry.getKey().r;
            String relation = entry.getKey().s;
            entry.getValue().forEach(arg2 -> output.add("1.0\t" + LogicUtils.toRNotation(new Triple<>(arg1, relation, arg2), "p") + "."));

            Collections.shuffle(ents, rnd);
            int limit = entry.getValue().size() * perPositive;
            for (String entity : sortedEntities) {
                if (limit <= 0) {
                    break;
                }
                if (entry.getValue().contains(entity)) {
                    continue;
                }
                output.add("0.0\t" + LogicUtils.toRNotation(new Triple<>(arg1, relation, entity), "p") + ".");
                limit--;
            }
        });
        return output;
    }

    private List<String> randomGeneration(Integer perPositive, MultiMap<Pair<String, String>, String> train, MultiMap<Pair<String, String>, String> validation, MultiMap<Pair<String, String>, String> test, List<String> sortedEntities) {
        List<String> output = Sugar.list();
        List<String> ents = Sugar.listFromCollections(sortedEntities);
        Random rnd = new Random();

        train.entrySet().forEach(entry -> {
            String arg1 = entry.getKey().r;
            String relation = entry.getKey().s;
            entry.getValue().forEach(arg2 -> output.add("1.0\t" + LogicUtils.toRNotation(new Triple<>(arg1, relation, arg2), "p") + "."));

            int added = 0;
            while (added < perPositive * entry.getValue().size()) {
                String ent1 = ents.get(rnd.nextInt(ents.size()));
                String ent2 = ents.get(rnd.nextInt(ents.size()));
                if (!train.get(new Pair<>(ent1, relation)).contains(ent2)
                        && !validation.get(new Pair<>(ent1, relation)).contains(ent2)
                        && !test.get(new Pair<>(ent1, relation)).contains(ent2)) {
                    output.add("0.0\t" + LogicUtils.toRNotation(new Triple<>(ent1, relation, ent2), "p") + ".");
                    added++;
                }
            }
        });
        return output;
    }

    private void storeEvidenceAndQueries(Set<String> common, Set<String> evidence, List<String> queries, Path evidencePath, Path queriesPath) throws IOException {
        System.out.println("storing to\t" + evidencePath);
        List<String> content = Sugar.list();
        common.forEach(e -> content.add(e + ","));
        evidence.forEach(e -> content.add(e + ","));
        queries.forEach(query -> {
            String[] split = query.split("\\t", 2);
            assert split.length == 2;
            Literal literal = split[1].trim().endsWith(".") ? Literal.parseLiteral(split[1].substring(0, split[1].length() - 1)) : Literal.parseLiteral(split[1]);
            assert literal.arity() == 3;
            Literal changed = new Literal("g", Sugar.list(literal.get(0), literal.get(1), literal.get(2)));
            content.add(changed + ",");
        });
        String last = content.get(content.size() - 1);
        last = last.trim();
        last = last.replace("),", ").");
        content.add(last);
        Files.write(evidencePath, content);
        Files.write(queriesPath, queries);
    }


    public void convertToLrnn20FromBigDataset(Path trainData, Path testData, Path validData, Path outputDir) throws IOException {
        Path outputSample = Paths.get(outputDir.toString(), "examples.txt");
        RenamingFactory relationRenaming = RenamingFactory.create("r");
        RenamingFactory entityRenaming = RenamingFactory.create("e");

        Set<Triple<String, String, String>> trainEvidence = loadKBC(trainData, relationRenaming, entityRenaming);
        Set<Triple<String, String, String>> testEvidence = loadKBC(testData, relationRenaming, entityRenaming);
        Set<Triple<String, String, String>> validEvidence = loadKBC(validData, relationRenaming, entityRenaming);

        List<String> sample = trainEvidence.stream()
                .map(triplet -> LogicUtils.toRNotation(triplet, "r"))
                .collect(Collectors.toList());
        relationRenaming.getValues().forEach(relation -> sample.add("rel(" + relation + ")"));
        entityRenaming.getValues().forEach(entity -> sample.add("ent(" + entity + ")"));


        // TODO nektere z tech casti budou chtit predelat aby generovane veci davaly smyslu vuci train-test-val ;)
        // queries generation
        // simplified generation of queries ;))
        List<String> toPredict = Sugar.list();

        List<Triple<String, String, String>> trEvidence = Sugar.listFromCollections(trainEvidence);
        for (int idx = 0; idx < trainEvidence.size(); idx++) {
            Triple<String, String, String> triplet = trEvidence.get(idx);
//            if(idx%2 == 0){
//                continue;
//            }
            toPredict.add("1.0 " + LogicUtils.toRNotation(triplet, "predict") + ".");
            sample.add(LogicUtils.toRNotation(triplet, "g"));
        }
        List<String> entities = Sugar.listFromCollections(entityRenaming.getValues());
        Random r = new Random();
        List<Triple<String, String, String>> negative = Sugar.list();
        trainEvidence.forEach(triplet -> {
            for (int k = 0; k < 1; k++) {

                for (int i = 0; i < 10; i++) {
                    String entity = entities.get(r.nextInt(entities.size()));
                    Triple<String, String, String> corrupted = new Triple<>(entity, triplet.getS(), triplet.getT());
                    if (trainEvidence.contains(corrupted) || testEvidence.contains(corrupted) || validEvidence.contains(corrupted)) {
                        continue;
                    }
                    negative.add(corrupted);
                    break;
                }
                for (int i = 0; i < 10; i++) {
                    String entity = entities.get(r.nextInt(entities.size()));
                    Triple<String, String, String> corrupted = new Triple<>(triplet.getR(), triplet.getS(), entity);
                    if (trainEvidence.contains(corrupted) || testEvidence.contains(corrupted) || validEvidence.contains(corrupted)) {
                        continue;
                    }
                    negative.add(corrupted);
                    break;
                }
            }

        });
        toPredict.add("");
        toPredict.add("");
        negative.forEach(literal -> {
            toPredict.add("0.0 " + LogicUtils.toRNotation(literal, "predict") + ".");
            sample.add(LogicUtils.toRNotation(literal, "g"));
        });


        List<String> sample2 = sample.stream().map(str -> str + ", ").collect(Collectors.toList());

        String last = sample.get(sample2.size() - 1);
        sample2.remove(sample2.size() - 1);
        last = last.substring(0, last.length()) + ".";
        sample2.add(last);
        Files.write(outputSample, sample2);

        Files.write(Paths.get(outputDir.toString(), "queries.txt"), toPredict);
        Files.write(Paths.get(outputDir.toString(), "dict.txt"), Sugar.list(relationRenaming, entityRenaming).stream()
                .flatMap(factory -> factory.serialize())
                .collect(Collectors.toList()));

    }

    private Set<Triple<String, String, String>> loadKBC(Path data, RenamingFactory relationRenaming, RenamingFactory entityRenaming) throws IOException {
        return Files.lines(data).filter(line -> line.trim().length() > 0)
                .map(line -> {
                    String[] splitted = line.split("\\s");
                    assert splitted.length == 3;
                    return new Triple<>(entityRenaming.get(splitted[0].trim()), relationRenaming.get(splitted[1].trim()), entityRenaming.get(splitted[2].trim()));
                })
                .collect(Collectors.toSet());
    }

    public void generateFragments(Path base, Path examplesPath, Path queries) throws IOException {
        Utils u = Utils.create();
        Clause example = new Clause(Files.lines(examplesPath)
                .filter(l -> l.trim().length() > 0)
                .map(l -> Literal.parseLiteral(l.trim().substring(0, l.trim().length() - 1)))
                .collect(Collectors.toList()));
        Set<String> entities = Sugar.set();
        Set<Literal> relations = Sugar.set();
        MultiMap<String, Pair<Literal, String>> edges = new MultiMap<>();
        example.literals().forEach(literal -> {
            if (literal.predicate().equals("ent")) {
                entities.add(literal.get(0).toString());
            } else if (literal.predicate().equals("r")) {
                edges.put(literal.get(0).toString(), new Pair<>(literal, literal.get(2).toString()));
                edges.put(literal.get(2).toString(), new Pair<>(literal, literal.get(0).toString()));
            } else if (literal.predicate().equals("rel")) {
                relations.add(literal);
            } else if (literal.predicate().equals("q") && literal.arity() == 3) {
                // do nothing
            } else {
                throw new IllegalStateException();
            }
        });
        List<Pair<Literal, Set<String>>> trueQueries = Files.lines(queries).filter(l -> l.trim().length() > 0)
                .filter(l -> !l.startsWith("0.0"))
                .map(line -> {
                    String[] splitted = line.split("\\s");
                    Literal literal = Sugar.chooseOne(Clause.parse(splitted[1]).literals());
                    return new Pair<>(literal, Sugar.set(literal.get(0).toString(), literal.get(2).toString()));
                }).collect(Collectors.toList());


        List<String> sortedEntities = Sugar.listFromCollections(entities);

        int rounds = 0;
        while (true) {
            rounds++;
            System.out.println("generating " + rounds + " round.");
            Set<String> foundEntities = Sugar.set();
            Set<Literal> selectedEdges = Sugar.set();
            // BFS
            Set<String> currentLayer = Sugar.setFromCollections(sortedEntities.subList(0, rounds + 1));
            for (int depth = 0; depth < rounds; depth++) {
                Set<String> nextLayer = Sugar.set();
                for (String entity : currentLayer) {
                    for (Pair<Literal, String> pair : edges.get(entity)) {
                        if (!foundEntities.contains(pair.s)) {
                            nextLayer.add(pair.s);
                            selectedEdges.add(pair.r);
                        }
                    }
                }

                foundEntities.addAll(nextLayer);
                currentLayer = nextLayer;
            }
            // store what was found :)
            int selectedEntities = selectedEdges.size();
            Set<Literal> ents = Sugar.set();
            selectedEdges.stream().forEach(literal -> {
                ents.add(new Literal("ent", Sugar.list(literal.get(0))));
                ents.add(new Literal("ent", Sugar.list(literal.get(2))));
            });
            selectedEdges.addAll(ents);
            selectedEdges.addAll(relations);
            Clause fragment = new Clause(selectedEdges);
            Set<Literal> currentQueries = trueQueries.stream()
                    .filter(pair -> foundEntities.containsAll(pair.s))
                    .map(Pair::getR)
                    .collect(Collectors.toSet());


            Random rdn = new Random();
            List<Literal> queries1 = Sugar.list();
            List<String> sortedCurrent = Sugar.listFromCollections(foundEntities);
            currentQueries.forEach(literal -> {
                for (int tryI = 0; tryI < 10; tryI++) {
                    String selectedEntity = sortedCurrent.get(rdn.nextInt(sortedCurrent.size()));
                    Literal query = queries1.size() % 2 == 0 ? new Literal(literal.predicate(), Sugar.list(literal.get(0), literal.get(1), new Constant(selectedEntity))) : new Literal(literal.predicate(), Sugar.list(new Constant(selectedEntity), literal.get(1), literal.get(2)));
                    if (currentQueries.contains(query) || example.containsLiteral(query)) {
                        continue;
                    }
                    queries1.add(query);
                    break;
                }
            });

            List<String> queriesX = Sugar.list();
            currentQueries.forEach(l -> queriesX.add("1.0\t" + l + "."));
            queries1.stream().forEach(l -> queriesX.add("0.0\t" + l + "."));
            // out dir of form rounds #entities #facts (samples size = #literals) #queries

            Path outDir = Paths.get(base.toString(), rounds + "_" + ents.size() + "_" + selectedEdges.size() + "_" + queriesX.size());
            System.out.println("storing to\t" + outDir);
            System.out.println(outDir.toAbsolutePath());
            if (!outDir.toFile().exists()) {
                outDir.toFile().mkdirs();
            }
            List<String> sample = fragment.literals().stream().map(str -> str + ", ").collect(Collectors.toList());
            Sugar.listFromCollections(currentQueries, queries1).forEach(literal -> sample.add("g(" + literal.get(0) + "," + literal.get(1) + "," + literal.get(2) + "), "));
            String last = sample.get(sample.size() - 1);
            sample.remove(sample.size() - 1);
            last = last.replace("),", ").");
            sample.add(last);
            Files.write(Paths.get(outDir.toString(), "examples.txt"), sample);
            Files.write(Paths.get(outDir.toString(), "queries.txt"), queriesX);


            // the 0.0 predict... are just fake... they may appear in the test-valid data, so be aware of that


            if (foundEntities.size() == entities.size() || rounds >= entities.size() - 2 || rounds >= 8) {
                System.out.println("ending at " + rounds + " rounds");
                break;
            }

        }

    }


    public void appendFromNTPToKG(Path trainData, Path devData, Path testData) {
        String dir = trainData.toFile().getParent();
        Set<String> relations = Sugar.set();
        Set<String> entities = Sugar.set();
        Sugar.list(new Pair<>(trainData, Paths.get(dir, "train.txt")),
                new Pair<>(devData, Paths.get(dir, "valid.txt")),
                new Pair<>(testData, Paths.get(dir, "test.txt")))
                .forEach(pair -> this.appendFromNTPToKG(pair.getR(), pair.getS(), relations, entities));
        try {
            Files.write(Paths.get(dir, "entities.dict"), toDictLike(entities));
            Files.write(Paths.get(dir, "relations.dict"), toDictLike(relations));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void appendFromNTPToKG(Path source, Path target, Set<String> relationsAccumulator, Set<String> entitiesAccumulator) {
        try {
            List<String> data = Files.lines(source).filter(line -> line.trim().length() > 0)
                    .map(line -> {
                        Clause clause = Clause.parse(line);
                        assert clause.literals().size() == 1;
                        Literal literal = Sugar.chooseOne(clause.literals());
                        assert literal.arity() == 2;
                        entitiesAccumulator.add(literal.get(0).toString());
                        entitiesAccumulator.add(literal.get(1).toString());
                        relationsAccumulator.add(literal.predicate());
                        return literal.get(0) + "\t" + literal.predicate() + "\t" + literal.get(1);
                    })
                    .collect(Collectors.toList());
            Files.write(target, data);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private List<String> toDictLike(Set<String> elements) {
        List<String> list = Sugar.listFromCollections(elements);
        Collections.sort(list);
        return IntStream.range(0, list.size())
                .mapToObj(idx -> idx + "\t" + list.get(idx))
                .collect(Collectors.toList());
    }


    public void appendKGFromAnonymous(Path source, Path output) throws IOException {
        Files.write(output, Files.lines(source).filter(line -> line.trim().length() > 0 && line.startsWith("1.0"))
                .map(line -> {
                    String[] split = line.split("\\s+", 2);
                    assert split.length == 2;
                    Literal literal = Sugar.chooseOne(Clause.parse(split[1]).literals());
                    return literal.get(0) + "\t" + literal.get(1) + "\t" + literal.get(2);
                }).collect(Collectors.toList()));

    }

    public void appendKGFromAnonymous(Path trainData, Path devData, Path testData) {
        String dir = trainData.toFile().getParent();
        Sugar.list(new Pair<>(trainData, Paths.get(dir, "train.txt")),
                new Pair<>(devData, Paths.get(dir, "valid.txt")),
                new Pair<>(testData, Paths.get(dir, "test.txt")))
                .forEach(pair -> {
                    try {
                        this.appendKGFromAnonymous(pair.getR(), pair.getS());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
    }


    public void renameAndStore(Path dictionaryPath, Path base, Path outBase, List<String> files, BiFunction<Map<String, String>, Stream<Literal>, List<String>> renaming, boolean rewrite) {
        Map<String, String> dictionary = loadDictionary(dictionaryPath);
        renameAndStore(dictionary, base, outBase, files, renaming, rewrite);
    }

    public void renameAndStore(Map<String, String> dictionary, Path base, Path outBase, List<String> files, BiFunction<Map<String, String>, Stream<Literal>, List<String>> renaming, boolean rewrite) {
        if (!outBase.toFile().exists()) {
            outBase.toFile().mkdirs();
        }
        files.forEach(file -> {
            try {
                Path target = Paths.get(outBase.toString(), file);
                if (target.toFile().exists() && !rewrite) {
                    System.out.println("skipping\t" + target + "\t because it already exists");
                } else {
                    Stream<Literal> lines = Files.lines(Paths.get(base.toString(), file))
                            .filter(l -> l.trim().length() > 0)
                            .map(l -> {
                                Clause clause = Clause.parse(l);
                                assert 1 != clause.countLiterals();
                                return Sugar.chooseOne(clause.literals());
                            });
                    Files.write(target, renaming.apply(dictionary, lines));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public List<String> anyBurlRenaming(Map<String, String> dictionary, Stream<Literal> literals) {
        return literals.map(literal -> {
            assert 2 == literal.arity();
            return dictionary.get(literal.get(0).toString()) + "\t" + dictionary.get(literal.predicate()) + "\t" + dictionary.get(literal.get(1).toString());
        }).collect(Collectors.toList());
    }

    public List<String> amieRenaming(Map<String, String> dictionary, Stream<Literal> literals) {
        return literals.map(literal -> {
            assert 2 == literal.arity();
            return "<" + dictionary.get(literal.get(0).toString()) + ">\t<" + dictionary.get(literal.predicate()) + ">\t<" + dictionary.get(literal.get(1).toString()) + ">";
        }).collect(Collectors.toList());
    }

    public Map<String, String> loadDictionary(Path dictionaryPath) {
//        System.out.println("loading from");
//        System.out.println(dictionaryPath);
//        System.out.println(dictionaryPath.toAbsolutePath());
        Map<String, String> map = new HashMap<>();
        try {
            Files.lines(dictionaryPath).filter(l -> l.trim().length() > 0).forEach(line -> {
                String[] split = line.split("\\s+");
                assert 2 == split.length;
                String original = split[0].trim();
                String renamed = split[1].trim();
                assert !map.containsKey(original);
                map.put(original, renamed);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }

    public RenamingFactory loadInverseDictionary(Path dictionaryPath) {
        Map<String, String> dictionary = loadDictionary(dictionaryPath);
        Map<String, String> map = new HashMap<>();
        dictionary.entrySet().forEach(entry -> map.put(entry.getValue(), entry.getKey()));
        return RenamingFactory.create("", map);
    }


    public Map<String, String> identityDictionary() {
        return new Map<String, String>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean containsKey(Object key) {
                return true;
            }

            @Override
            public boolean containsValue(Object value) {
                return true;
            }

            @Override
            public String get(Object key) {
                return key.toString();
            }

            @Override
            public String put(String key, String value) {
                return null;
            }

            @Override
            public String remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends String, ? extends String> m) {

            }

            @Override
            public void clear() {

            }

            @Override
            public Set<String> keySet() {
                return null;
            }

            @Override
            public Collection<String> values() {
                return null;
            }

            @Override
            public Set<Entry<String, String>> entrySet() {
                return null;
            }
        };


    }

    public void createKGEDictAndStore(List<Path> symbolicInput, Path outputDir, boolean rewrite) {
        if (!outputDir.toFile().exists()) {
            outputDir.toFile().mkdirs();
        }
        RenamingFactory relationRenaming = RenamingFactory.create("r");
        RenamingFactory entityRenaming = RenamingFactory.create("e");
        for (Path path : symbolicInput) {
            LogicUtils.loadEvidence(path).forEach(literal -> {
                if (literal.arity() == 1) {
                /*
                String entity = entityRenaming.get(literal.get(0).toString());
                String attribute = attributeRenaming.get(literal.getPredicate().r);

                return LogicUtils.toRNotation(literal);
                */
                    throw new IllegalStateException();// NotImplementedException();
                } else if (literal.arity() == 2) {
                    String arg1 = entityRenaming.get(literal.get(0).toString());
                    String arg2 = entityRenaming.get(literal.get(1).toString());
                    String relation = relationRenaming.get(literal.getPredicate().r);
                } else {
                    throw new IllegalStateException();// NotImplementedException();
                }
            });
        }

        Sugar.list(new Pair<>(Paths.get(outputDir.toString(), "relations.dict"), relationRenaming),
                new Pair<>(Paths.get(outputDir.toString(), "entities.dict"), entityRenaming)).stream()
                .forEach(pair -> {
                            Path path = pair.r;
                            if (!path.toFile().exists() || rewrite) {
                                try {
                                    Files.write(path, pair.getS().toSortedDict());
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                System.out.println("skipping\t" + path + "\t because it already exists");
                            }
                        }
                );
    }

    /**
     * Returns content of a file, line by line. No elements swapping is done, so what is loaded is returned, e.g.
     * e1 r1 e2
     * ....
     *
     * @param path
     * @return
     */
    public List<Triple<String, String, String>> loadTriples(Path path) {
        try {
            System.out.println("here\t" + path);
            return Files.lines(path)
                    .map(line -> {
                        String[] split = line.split("\\s+");
                        assert 3 == split.length;
                        return new Triple<>(split[0], split[1], split[2]);
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    /**
     * triplets are in form [<e1, r2, e2>,....]
     *
     * @param triplets
     * @param entityRenaming
     * @param relationRenaming
     * @return
     */
    public List<Triple<String, String, String>> renameKGTriplet(List<Triple<String, String, String>> triplets, RenamingFactory entityRenaming, RenamingFactory relationRenaming) {
        return triplets.stream().map(triple -> new Triple<>(entityRenaming.get(triple.getR())
                , relationRenaming.get(triple.getS())
                , entityRenaming.get(triple.getT())))
                .collect(Collectors.toList());
    }

    /**
     * Input is in KG format, e.g. <e1, r1, e2>.
     * Output is in FOL format, e.g. r1(e1, e2).
     *
     * @param triples
     * @return
     */
    public List<Literal> convertKGTripletsToFol(List<Triple<String, String, String>> triples) {
        return triples.stream().map(triple -> new Literal(triple.getS(), Sugar.list(Constant.construct(triple.getR()),
                Constant.construct(triple.getT()))))
                .collect(Collectors.toList());
    }

    /**
     * removes first letter of each entity and relation
     *
     * @param clause
     * @return
     */
    public static Clause stripFirstLettersFromNonVariables(Clause clause) {
        return new Clause(clause.literals().stream().map(Reformatter::stripFirstLettersFromNonVariables).collect(Collectors.toList()));
    }

    public static Term stripFirstLettersFromNonVariables(Term term) {
        if (term instanceof Variable) {
//                        return Variable.construct(term.name().substring(1),term.type());
            return term;
        } else if (term instanceof Constant) {
            return Constant.construct(term.name().substring(1), term.type());
        }
        throw new IllegalStateException();// NotImplementedException();
    }

    public static Literal stripFirstLettersFromNonVariables(Literal literal) {
        return new Literal(literal.predicate().substring(1), literal.isNegated(),
                literal.argumentsStream().map(Reformatter::stripFirstLettersFromNonVariables).collect(Collectors.toList()));
    }

    public static Quadruple<Set<Literal>, Set<Literal>, Set<Literal>, Boolean> loadData(String dataset) {
        return loadData(Paths.get("..", "datasets", dataset + "-ntp-o"));
    }

    public static Quadruple<Set<Literal>, Set<Literal>, Set<Literal>, Boolean> loadData(Path base) {
        boolean renameToIndexes = Paths.get(base.toString(), "rename.true").toFile().exists();
        return loadDataAndRename(base, renameToIndexes);
    }

    public static Quadruple<Set<Literal>, Set<Literal>, Set<Literal>, Boolean> loadDataAndRename(Path base, boolean renameToIndexes) {
        Set<Literal> evidence;
        Set<Literal> valid;
        Set<Literal> test;
        if (renameToIndexes) {
            System.out.println("loading renamed version of the dataset from tiplets!");
            Reformatter r = Reformatter.create();
            Pair<RenamingFactory, RenamingFactory> pair = r.loadDictionaries(base);
            RenamingFactory entityRenaming = pair.r;
            RenamingFactory relationRenaming = pair.s;

            Path trainPath = Paths.get(base.toString(), "triples.train.nl");
            List<Triple<String, String, String>> trainTriplets;
            List<Triple<String, String, String>> validTriplets;
            List<Triple<String, String, String>> testTriplets;
            if (trainPath.toFile().exists()) {
                trainTriplets = r.loadTriples(trainPath);
                validTriplets = r.loadTriples(Paths.get(base.toString(), "triples.dev.nl"));
                testTriplets = r.loadTriples(Paths.get(base.toString(), "triples.test.nl"));
            } else {
                System.out.println("triples.train.nl doesn't exist, switching to pure symbolic data and renaming them afterwards");
                trainTriplets = LogicUtils.loadEvidence(Paths.get(base.toString(), "train.nl")).stream().map(l -> new Triple<>(l.get(0).toString(), l.predicate(), l.get(1).toString())).collect(Collectors.toList());
                validTriplets = LogicUtils.loadEvidence(Paths.get(base.toString(), "dev.nl")).stream().map(l -> new Triple<>(l.get(0).toString(), l.predicate(), l.get(1).toString())).collect(Collectors.toList());
                testTriplets = LogicUtils.loadEvidence(Paths.get(base.toString(), "test.nl")).stream().map(l -> new Triple<>(l.get(0).toString(), l.predicate(), l.get(1).toString())).collect(Collectors.toList());
            }
            evidence = r.convertKGTripletsToFol(r.renameKGTriplet(trainTriplets, entityRenaming, relationRenaming)).stream().collect(Collectors.toSet());
            valid = r.convertKGTripletsToFol(r.renameKGTriplet(validTriplets, entityRenaming, relationRenaming)).stream().collect(Collectors.toSet());
            test = r.convertKGTripletsToFol(r.renameKGTriplet(testTriplets, entityRenaming, relationRenaming)).stream().collect(Collectors.toSet());
        } else {
            evidence = LogicUtils.loadEvidence(Paths.get(base.toString(), "train.nl"));
            valid = LogicUtils.loadEvidence(Paths.get(base.toString(), "dev.nl"));
            test = LogicUtils.loadEvidence(Paths.get(base.toString(), "test.nl"));
        }
        return new Quadruple<>(evidence, valid, test, renameToIndexes);
    }

    // return entity and relation renaming factories (inverse of int->folName dict)
    public Pair<RenamingFactory, RenamingFactory> loadDictionaries(Path base) {
        RenamingFactory entityRenaming = loadInverseDictionary(Paths.get(base.toString(), "entities.dict"));
        RenamingFactory relationRenaming = loadInverseDictionary(Paths.get(base.toString(), "relations.dict"));
        return new Pair<>(entityRenaming, relationRenaming);
    }

}
