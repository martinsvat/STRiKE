package logicStuff.typing;

import ida.ilp.logic.*;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialBinaryPredicates;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.utils.Combinatorics;
import ida.utils.MutableInteger;
import ida.utils.Sugar;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.Typing;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 26. 10. 2018.
 */
public class TypesInducer {


    public Map<Pair<Predicate, Integer>, Type> induce(MEDataset med) {
        return induce(med.getExamples().stream().flatMap(c -> c.literals().stream()).collect(Collectors.toSet()));
    }

    // for constants only (we would need some kind of legal renaming of variables to make it sense ;))
    public Map<Pair<Predicate, Integer>, Type> induce(Collection<Literal> literals) {
        Map<Constant, Type> lastType = new HashMap<>();
        MultiMap<Type, Pair<Predicate, Integer>> typeToPosition = new MultiMap<>(MultiMap.LINKED_HASH_SET);
        Map<Pair<Predicate, Integer>, Type> positionToType = new HashMap<>();
        MutableInteger typesCount = new MutableInteger(0);

        for (Literal literal : literals) {
            Predicate predicate = Predicate.create(literal.getPredicate());
            IntStream.range(0, literal.arity()).forEach(idx -> {
                Constant constant = (Constant) literal.get(idx); // we said, it is implemented for constants only
                Pair<Predicate, Integer> position = new Pair<>(predicate, idx);

                Type typeByPosition = positionToType.get(position);
                if (null == typeByPosition) {
                    typeByPosition = new Type(typesCount.value(), position);
                    typesCount.increment();
                    positionToType.put(position, typeByPosition);
                    assert !typeToPosition.containsKey(typeByPosition);
                    typeToPosition.put(typeByPosition, position);
                }

                Type typeByLast = lastType.get(constant);
                if (null == typeByLast) {
                    lastType.put(constant, typeByPosition); // nothing needs to be done :))
                } else if (!typeByLast.equals(typeByPosition)) {
                    // union these two types; add type by position to this last one (for optimalization take the smaller one)

                    typeToPosition.putAll(typeByLast, typeToPosition.get(typeByPosition));
                    typeToPosition.get(typeByPosition).forEach(pos -> positionToType.put(pos, typeByLast));
                    Type finalTypeByPosition = typeByPosition;
                    List<Constant> remapConstants = lastType.entrySet().parallelStream().filter(e -> e.getValue().equals(finalTypeByPosition)).map(Map.Entry::getKey).collect(Collectors.toList());
                    remapConstants.forEach(c -> lastType.put(c, typeByLast));

                    lastType.put(constant, typeByLast);
                }
            });

        }

        return positionToType;
    }

    // determistic renaming
    public Map<Pair<Predicate, Integer>, Type> rename(Map<Pair<Predicate, Integer>, Type> mapping) {
        MutableInteger typesCount = new MutableInteger(1);
        Map<Type, Type> oldToNewType = new HashMap<>();

        Map<Pair<Predicate, Integer>, Type> retVal = new HashMap<>();
        mapping.keySet().stream().sorted((p1, p2) -> {
            int comparingPredicates = p1.r.toString().compareTo(p2.r.toString());
            if (0 == comparingPredicates) {
                return Integer.compare(p1.s, p2.s);
            }
            return comparingPredicates;
        }).forEach(key -> {
            Type oldType = mapping.get(key);
            if (!oldToNewType.containsKey(oldType)) {
                oldToNewType.put(oldType, new Type(typesCount.value(), key));
                typesCount.increment();
            }
            Type newType = oldToNewType.get(oldType);
            newType.add(new Type(-1, key));

            retVal.put(key, newType);
        });

        return retVal;
    }


    public Collection<Clause> generateRules(Map<Pair<Predicate, Integer>, Type> mapping) {
        List<Clause> retVal = mapping.entrySet().stream().map(e -> generateHornRule(e.getKey().r, e.getKey().s, e.getValue())).collect(Collectors.toList());
        retVal.addAll(generateConstraints(mapping));
        return retVal;
    }

    public Set<Clause> generateConstraints(Map<Pair<Predicate, Integer>, Type> mapping) {
        Set<Predicate> predicates = mapping.keySet().stream().map(Pair::getR).collect(Collectors.toSet());
        Set<IsoClauseWrapper> singleLiteral = literalConstraints(mapping, predicates);
        Set<IsoClauseWrapper> retVal = Sugar.set();

        Set<IsoClauseWrapper> closed = Sugar.set();
        // two-literal clauses
        List<Predicate> sortedPredicates = Sugar.listFromCollections(predicates);
        for (int outer = 0; outer < sortedPredicates.size(); outer++) {
            Literal l1 = new Literal(sortedPredicates.get(outer).getName(), true, IntStream.range(0, sortedPredicates.get(outer).getArity()).mapToObj(idx -> Variable.construct("V" + idx)).collect(Collectors.toList()));
            for (int inner = outer; inner < sortedPredicates.size(); inner++) {
                Literal l2 = new Literal(sortedPredicates.get(inner).getName(), true, IntStream.range(l1.arity(), l1.arity() + sortedPredicates.get(inner).getArity()).mapToObj(idx -> Variable.construct("V" + idx)).collect(Collectors.toList()));
                Set<Variable> vars = Sugar.union(LogicUtils.variables(l1), LogicUtils.variables(l2));
                Clause clause = new Clause(Sugar.list(l1, l2));
                List<Variable> sortedVariables = Sugar.listFromCollections(vars);
                for (List<Variable> variables : Combinatorics.variations(vars, l1.arity() + l2.arity())) {
                    Clause substituted = LogicUtils.substitute(clause, IntStream.range(0, sortedVariables.size()).boxed().collect(Collectors.toMap(sortedVariables::get, variables::get)));
                    IsoClauseWrapper icw = IsoClauseWrapper.create(substituted);

                    if (closed.add(icw)) {
                        continue;
                    }

                    //System.out.println(substituted + "\t" + typesOk(substituted, mapping) + "\t" + substituted.connectedComponents().size());
                    if (substituted.countLiterals() > 1 && typesOk(substituted, mapping) && substituted.connectedComponents().size() == 1) {
                        boolean add = true;
                        for (Literal literal : substituted.literals()) {
                            if (singleLiteral.contains(IsoClauseWrapper.create(new Clause(literal)))) {
                                //System.out.println("tady tokoncim \t" + literal + "\tkvuli\t" + substituted);
                                add = false;
                                break;
                            }
                        }
                        if (add) {
                            retVal.add(icw);
                        }
                    }
                }
            }
        }


        retVal.addAll(singleLiteral);
        return retVal.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toSet());
    }

    private Set<IsoClauseWrapper> literalConstraints(Map<Pair<Predicate, Integer>, Type> mapping, Set<Predicate> predicates) {
        Set<IsoClauseWrapper> retVal = Sugar.set();
        for (Predicate predicate : predicates) {
            List<Variable> variables = IntStream.range(0, predicate.getArity()).mapToObj(idx -> Variable.construct("V" + idx)).collect(Collectors.toList());
            for (List<Variable> variablesOrder : Combinatorics.variations(Sugar.setFromCollections(variables), variables.size())) {
                Literal lit = new Literal(predicate.getName(), true, variablesOrder);
                if (typesOk(lit, mapping)) {
                    retVal.add(IsoClauseWrapper.create(new Clause(lit)));
                }
            }
        }
        return retVal;
    }

    private boolean typesOk(Clause clause, Map<Pair<Predicate, Integer>, Type> mapping) {
        return typesOk(clause.literals(), mapping);
    }

    private boolean typesOk(Collection<Literal> literals, Map<Pair<Predicate, Integer>, Type> mapping) {
        Map<Variable, Type> map = new HashMap<>();
        for (Literal literal : literals) {
            for (int idx = 0; idx < literal.arity(); idx++) {
                Pair<Pair<String, Integer>, Integer> key = new Pair<>(literal.getPredicate(), idx);
                Variable variable = (Variable) literal.get(idx);
                Type alreadyType = map.get(variable);
                if (null == alreadyType) {
                    map.put(variable, mapping.get(key));
                } else if (!mapping.get(key).equals(alreadyType)) {

                    System.out.println(literals);
                    System.out.println(key + "\t" + mapping.get(key) + "\t" + alreadyType);
                    System.exit(-200);
                    return false;
                }
            }
        }


        return true;
    }

    private boolean typesOk(Literal literal, Map<Pair<Predicate, Integer>, Type> mapping) {
        return typesOk(Sugar.list(literal), mapping);
    }

    public Clause generateHornRule(Predicate predicate, Integer argumentIdx, Type type) {
        List<Variable> arguments = IntStream.range(0, predicate.getArity()).mapToObj(idx -> Variable.construct("V" + idx)).collect(Collectors.toList());
        Literal bodyLiteral = new Literal(predicate.getName(), true, arguments);
        Literal head = new Literal(type.predicate().getName(), arguments.get(argumentIdx));
        return new Clause(Sugar.set(head, bodyLiteral));
    }

    public void writeDown(Map<Pair<Predicate, Integer>, Type> mapping) {
        mapping.keySet().stream().map(Pair::getR).distinct().sorted(Comparator.comparing(Object::toString))
                .forEach(predicate -> {
                    List<Constant> arguments = IntStream.range(0, predicate.getArity())
                            .mapToObj(idx -> Constant.construct("t" + mapping.get(new Pair<>(predicate, idx)).getId()))
                            .collect(Collectors.toList());
                    Literal literal = new Literal(predicate.getName(), arguments);
                    System.out.println(literal);
                });
    }

    public MEDataset extendDataset(MEDataset med, Map<Pair<Predicate, Integer>, Type> typing) {
        return extendDataset(med, generateRules(typing));
    }

    public MEDataset extendDataset(MEDataset med, Collection<Clause> clauses) {
        return MEDataset.create(med.getExamples().stream().map(clause -> extend(clause, clauses)).collect(Collectors.toList())
                , Arrays.stream(med.getTargets()).boxed().collect(Collectors.toList())
                , med.getSubstitutionType());
    }

    // adds types by adding unary predicates
    private Clause extend(Clause example, Collection<Clause> theory) {
        Matching world = Matching.create(example, Matching.THETA_SUBSUMPTION);
        List<Literal> literals = Sugar.list();
        literals.addAll(example.literals());
        for (Clause rule : theory) {
            Literal head = Sugar.chooseOne(LogicUtils.positiveLiterals(rule));
            if (null == head) {
                throw new IllegalStateException("only definite rules are allowed for dataset extension, but given:\t" + rule);
            }
            Pair<Term[], List<Term[]>> substitutions = world.allSubstitutions(LogicUtils.flipSigns(rule), 0, Integer.MAX_VALUE);
            for (Term[] terms : substitutions.s) {
                literals.add(LogicUtils.substitute(head, substitutions.r, terms));
            }
        }
        return new Clause(literals);
    }

    public MEDataset typeDatasetByTyping(MEDataset med, Map<Pair<Predicate, Integer>, Type> typing) {
        return typeDataset(med, simplify(typing));
    }

    public MEDataset typeDataset(MEDataset med, Map<Pair<Predicate, Integer>, String> simplifiedTyping) {
        List<Clause> examples = med.getExamples().stream().map(c -> LogicUtils.addTyping(c, simplifiedTyping)).collect(Collectors.toList());
        List<Literal> queries = null == med.getQueries() ? null : med.getQueries().stream().map(l -> LogicUtils.addTyping(l, simplifiedTyping)).collect(Collectors.toList());
        List<Double> targets = Arrays.stream(med.getTargets()).boxed().collect(Collectors.toList());
        return new MEDataset(examples, queries, targets, med.getSubstitutionType());
    }


    public static Map<Pair<Predicate, Integer>, String> simplify(Map<Pair<Predicate, Integer>, Type> typing) {
        return typing.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().getStrId()));
    }

    public static void store(Map<Pair<Predicate, Integer>, Type> typing, Path path) {
        Map<Pair<Predicate, Integer>, String> simplified = simplify(typing);
        try {
            Files.write(path, simplified.entrySet().stream().map(e -> asOutput(e.getKey(), e.getValue())).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // predelat na citelnejsi format f(1,1) a tak :))
    private static String asOutput(Pair<Predicate, Integer> predicatePosition, String type) {
        return predicatePosition.r.toString() + "\t" + predicatePosition.s + "\t" + type;
    }

    // predelat na citelnejsi format f(1,1) a tak :))
    public static Map<Pair<Predicate, Integer>, String> load(Path path) {
        try {
            return Files.lines(path).map(l -> parse(l)).collect(Collectors.toMap(Pair::getR, Pair::getS));
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }

    private static Pair<Pair<Predicate, Integer>, String> parse(String line) {
        String[] splitted = line.trim().split("\\s+");
        if (splitted.length != 3) {
            throw new IllegalArgumentException("cannot parse predicate-position-type in: " + line);
        }

        Predicate predicate = Predicate.construct(splitted[0]);
        int position = Integer.parseInt(splitted[1]);
        Pair<Predicate, Integer> predicatePosition = new Pair<>(predicate, position);
        String type = splitted[2];
        return new Pair<>(predicatePosition, type);
    }

    public static Map<Pair<Predicate, Integer>, Type> transform(Map<Pair<Predicate, Integer>, String> simplified) {
        Map<String, Type> cache = new HashMap<>();
        return simplified.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey
                        , e -> {
                            if (!cache.containsKey(e.getValue())) {
                                cache.put(e.getValue(), new Type(Integer.parseInt(e.getValue() + ""), e.getKey()));
                            }
                            return cache.get(e.getValue());
                        }));
    }

    public boolean validTypes(Set<Clause> clauses, Map<Pair<Predicate, Integer>, String> simplified) {
        for (Clause clause : clauses) {
            if (!validTypes(clause, simplified)) {
                return false;
            }
        }
        return true;
    }

    private boolean validTypes(Clause clause, Map<Pair<Predicate, Integer>, String> simplified) {
        //
        Map<String, String> map = new HashMap<>();
        for (Literal literal : clause.literals()) {
            Predicate predicate = Predicate.create(literal.getPredicate());
            for (int idx = 0; idx < literal.arity(); idx++) {
                Term term = literal.get(idx);
                if (term instanceof Function) {
                    throw new IllegalStateException(); //NotImplementedException();
                }
                if (null == term.type() || term.type().length() < 1) {
                    throw new IllegalStateException("this term is not typed\t" + term + "\tof literal\t" + literal + "\tof clause\t" + clause);
                }
                if ((map.containsKey(term.name()) && !map.get(term.name()).equals(term.type()))
                        || (!simplified.get(new Pair<>(predicate, idx)).equals(term.type()))) {
                    return false;
                }
                map.put(term.name(), term.type());
            }
        }
        return true;
    }

    public static TypesInducer create() {

        return new TypesInducer();
    }

    public Map<Pair<Predicate, Integer>, String> collect(Collection<Literal> literals) {
        Map<Pair<Predicate, Integer>, String> retVal = new HashMap<>();
        for (Literal literal : literals) {
            Predicate predicate = Predicate.create(literal.predicate(), literal.arity());
            if (SpecialVarargPredicates.SPECIAL_PREDICATES.contains(predicate.getName()) || SpecialBinaryPredicates.SPECIAL_PREDICATES.contains(predicate.getName())) {
                continue;
            }
            for (int idx = 0; idx < literal.arity(); idx++) {
                Pair<Predicate, Integer> key = new Pair<>(predicate, idx);
                String type = literal.get(idx).type();
                if (!retVal.containsKey(key)) {
                    retVal.put(key, type);
                    //}else if(null == type || type.equals(""){
                    // not type at all
                } else if (!type.equals(retVal.get(key))) {
                    throw new IllegalStateException();
                }
            }
        }
        return retVal;
    }

    public static boolean typingSubsetEq(Collection<Literal> literals, Map<Pair<Predicate, Integer>, String> typing) {
        TypesInducer inducer = TypesInducer.create();
        Map<Pair<Predicate, Integer>, String> data = inducer.collect(literals);
        return typingSubsetEq(data, typing);
    }

    private static boolean typingSubsetEq(Map<Pair<Predicate, Integer>, String> data, Map<Pair<Predicate, Integer>, String> typing) {
        if (data.values().stream().anyMatch(o -> null == o) || data.values().stream().anyMatch(o -> null == o)) {
            if (data.values().stream().allMatch(o -> null == o) && data.values().stream().allMatch(o -> null == o)) {
                return true;
            }
            return false;
        }
        // prvni musi byt podmnozinou druheho
        for (Pair<Predicate, Integer> key : data.keySet()) {
            if (!data.get(key).equals(typing.get(key))) {
                return false;
            }
        }
        return true;

    }

    // just a holder so we can make faster unions and statefull hacks
    private class TypeWrapper {

        private Set<Constant> constants;
        private Type type;

        public TypeWrapper(Type type) {
            this(type, Sugar.set());
        }

        public TypeWrapper(Type type, Set<Constant> constants) {
            this.type = type;
            this.constants = constants;
        }

        public void addAll(Collection<Constant> elements) {
            this.constants.addAll(elements);
        }

        public void add(Constant constant) {
            this.constants.add(constant);
        }

        public Set<Constant> getConstants() {
            return constants;
        }

        public void setConstants(Set<Constant> constants) {
            this.constants = constants;
        }

        public Type getType() {
            return type;
        }

        public void setType(Type type) {
            this.type = type;
        }
    }


    public static void main(String[] args) {
        comparingTest();
    }

    private static void comparingTest() {
        Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", "uwcs", "train.db"));
        //Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", "mlns","imdb", "merged.db"));
        //Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", "protein", "train.db.oneLine"));

        TypesInducer inducer = new TypesInducer();
        Map<Pair<Predicate, Integer>, Type> r1 = inducer.induce(evidence);
        Map<Pair<Predicate, Integer>, Type> r2 = inducer.rename(r1);

        System.out.println("inducer result");
        inducer.writeDown(r2);


        System.out.println("\n\n");
        inducer.generateRules(r2).stream().map(HornClause::create).forEach(System.out::println);

        System.out.println("\n\n");

        System.out.println("old typing");
        Typing typing = Typing.create(new Clause(evidence));
        typing.getPredicatesDefinition().stream().sorted(Comparator.comparing(Object::toString)).forEach(System.out::println);

    }


}

