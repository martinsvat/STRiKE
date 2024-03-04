package ida.gnns;

//import com.sun.org.apache.xml.internal.utils.StringComparable;
import ida.ilp.logic.*;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.pacReasoning.data.Reformatter;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.utils.Combinatorics;
import ida.utils.Sugar;
import ida.utils.collections.Counters;
import ida.utils.collections.MultiList;
import ida.utils.tuples.Pair;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 8. 10. 2020.
 */
public class RulesLike {

    public List<LearnedRule> loadAnyBurl(Path path) {
        try {// Charset.forName("latin1") due to curaçao
            return Files.lines(path, Charset.forName("latin1")).filter(l -> l.trim().length() > 0).map(line -> {
                String[] split = line.split("\\s+", 4);
                assert 4 == split.length;
                Map<String, String> property = new HashMap<>();
                property.put("predicted", split[0].trim());
                property.put("correctlyPredicted", split[1].trim());
                property.put("confidence", split[2].trim());
                // horn parsing
                String[] ruleSplit = split[3].split("<=");
                assert 2 == ruleSplit.length;
                Clause head = Clause.parse(ruleSplit[0]);
                Clause body = Clause.parse(ruleSplit[1]);
                Clause rule = new Clause(Sugar.listFromCollections(head.literals(), body.literals().stream().map(Literal::negation).collect(Collectors.toList())));
                return LearnedRule.create(property, rule);
            }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException();
    }


    public List<LearnedRule> loadAmie(Path source) {
        Reformatter r = new Reformatter();
        System.out.println(source);
        try {
            return Files.lines(source, Charset.defaultCharset())
                    .filter(line -> line.startsWith("?"))
                    .map(line -> {
                        Pair<Set<Literal>, List<String>> pair = r.parseAmieLine(line);
                        Map<String, String> properties = new HashMap<>();
                        for (int idx = 0; idx < pair.getS().size(); idx++) {
                            properties.put("crit" + (idx + 1), pair.getS().get(idx));
                        }
                        properties.put("confidence", pair.getS().get(1).replace(",", "."));
                        properties.put("pcaconf", pair.getS().get(2).replace(",", "."));
                        // [1] is std confidence (standard)
                        // [2] is PCA confidence
                        return LearnedRule.create(properties, new Clause(pair.getR()));
                    }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("error while parsing the file");
    }

    public Template createAmAtCombinatioinTemplate(List<LearnedRule> rules, Path dictionaryPath, boolean addBaseEmbeddings) {
        return createAmAtCombinatioinTemplate(rules, new Reformatter().loadDictionary(dictionaryPath), addBaseEmbeddings);
    }

    public Template createAmAtCombinatioinTemplate(List<LearnedRule> rules, Map<String, String> dictionary, boolean addBaseEmbeddings) {
        // assuming the dictionary contains all entities and relations... and that entities start with e and relations with r
        Set<String> entities = Sugar.set();
        Set<String> relations = Sugar.set();

        dictionary.entrySet().forEach(entry -> {
            String element = entry.getValue();
            if (element.startsWith("e")) {
                entities.add(element);
            } else if (element.startsWith("r")) {
                relations.add(element);
            } else {
                throw new IllegalStateException();
            }
        });
        return createAmAtCombinatioinTemplate(rules, entities, relations, addBaseEmbeddings, true);
    }

    // each antecedent is extended by * and for each term there is a new parametrized e(T) added to the body
    public Template createRuleEmbeddingTemplate(List<LearnedRule> rules, Set<String> entities, Set<String> relations, boolean addBaseEmbeddings, boolean addRelationEmbeddings) {
        Set<IsoClauseWrapper> clauses = Sugar.set();

        String targetLiteral = "p";
        String auxiliarTarget = "g";

        // only for entity embeddings; not done for relations
        // creating new auxiliar predicate per variable-appearance of body
        for (LearnedRule rule : rules) {
            HornClause horn = HornClause.create(rule.getRule());
            System.out.println("v predchozich experimentech byla chyba protoze v toAdd chybi entity z hlavy");
            LinkedHashSet<String> toAdd = new LinkedHashSet<>();
            horn.head().terms().forEach(t -> toAdd.add(t.toString()));
            horn.body().literals().forEach(literal -> {
                for (int idx = 0; idx < literal.arity(); idx++) {
                    toAdd.add("!ee(" + literal.get(idx).toString() + ")");
                }
                if (addRelationEmbeddings) {
                    toAdd.add("!er(" + literal.predicate() + ")");
                }
            });
            toAdd.forEach(s -> horn.body().addLiteral(Literal.parseLiteral(s)));

            Literal head = horn.head();
            Literal newHead = new Literal(targetLiteral, Sugar.list(head.get(0), Constant.construct(head.predicate()), head.get(1)));

            List<Literal> literals = Sugar.listFromCollections(Sugar.list(newHead),
                    horn.body().literals().stream().map(literal -> {
                        if ("ee".equals(literal.predicate()) || "er".equals(literal.predicate())) {
                            return literal;
                        } else {
                            // to non-learnable R notation
                            return new Literal("*r",
                                    true,
                                    Sugar.list(literal.get(0), Constant.construct(literal.predicate()), literal.get(1)));
                        }
                    }).collect(Collectors.toList()));
            literals.add(new Literal("*" + auxiliarTarget, true, newHead.get(0), newHead.get(1), newHead.get(2)));
            clauses.add(IsoClauseWrapper.create(new Clause(literals)));
        }

        if (addBaseEmbeddings) {
            return Template.create(entities, relations, clauses.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        } else {
            return Template.create(Sugar.set(), Sugar.set(), clauses.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        }
    }


    // rule :- am/X ; am/X :- at/2, at/2 :- either fixed or lifted
    public Template createAmAtCombinatioinTemplate(List<LearnedRule> rules, Set<String> entities, Set<String> relations, boolean addBaseEmbeddings, boolean embeddingForEachTuple) {
        Set<IsoClauseWrapper> clauses = Sugar.set();

        String targetLiteral = "p";
        String auxiliarTarget = "g";

        // only for entity embeddings; not done for relations
        // creating new auxiliar predicate per variable-appearance of body
        String auxiliarPredicate = "am";
        Set<IsoClauseWrapper> auxiliars = Sugar.set();
        for (LearnedRule rule : rules) {
            HornClause horn = HornClause.create(rule.getRule());
            LinkedHashSet<Term> terms = new LinkedHashSet<>();
            horn.body().literals().forEach(literal -> {
                for (int idx = 0; idx < literal.arity(); idx++) {
//                    if (literal.get(idx) instanceof Variable) {
//                        terms.add((Variable) literal.get(idx));
//                    }
                    terms.add(literal.get(idx));
                }
            });
            // version for variables, terms, ...
//            Literal extraLiteral = new Literal(auxiliarPredicate, terms.stream().collect(Collectors.toList()));
            // LinkedHashSet<Term> terms2 = terms;
            // lift version (only variables within the auxiliar variables)
            Variable longest = null;
            for (Term term : terms) {
                if (term instanceof Variable && (null == longest || longest.name().length() < term.name().length())) {
                    longest = (Variable) term;
                }
            }
            LinkedHashSet<Term> terms2 = new LinkedHashSet<>();
            int added = 0;
            for (Term term : terms) {
                if (term instanceof Variable) {
                    terms2.add(term);
                } else {
                    terms2.add(Variable.construct(longest.name() + "_" + added));
                    added++;
                }
            }
            Literal extraLiteral = new Literal(auxiliarPredicate, terms2.stream().collect(Collectors.toList()));
            auxiliars.add(IsoClauseWrapper.create(new Clause(extraLiteral)));

            Literal head = horn.head();
            Literal newHead = new Literal(targetLiteral, Sugar.list(head.get(0), Constant.construct(head.predicate()), head.get(1)));
            List<Literal> literals = Sugar.listFromCollections(Sugar.list(newHead, extraLiteral.negation()),
                    horn.body().literals().stream().map(literal -> {
                        // to non-learnable R notation
                        return new Literal("*r",
                                true,
                                Sugar.list(literal.get(0), Constant.construct(literal.predicate()), literal.get(1)));
                    }).collect(Collectors.toList()));
            literals.add(new Literal("*" + auxiliarTarget, true, newHead.get(0), newHead.get(1), newHead.get(2)));
            clauses.add(IsoClauseWrapper.create(new Clause(literals)));
        }

        // creating body for auxiliar heads
        // only every two besides (we can do more permutation here in the future)
        String auxiliarTuple = "at";
        for (IsoClauseWrapper auxiliar : auxiliars) {
            Literal head = Sugar.chooseOne(auxiliar.getOriginalClause().literals());
            for (int idx = 0; idx < head.arity() - 1; idx++) {
                Literal body = new Literal(auxiliarTuple, true, Sugar.list(head.get(idx), head.get(idx + 1)));
                clauses.add(IsoClauseWrapper.create(new Clause(Sugar.list(head, body))));
            }
        }

        if (embeddingForEachTuple) {
            for (List<String> variation : Combinatorics.variationsWithRepetition(entities, 2)) {
                Constant c1 = Constant.construct(variation.get(0));
                Constant c2 = Constant.construct(variation.get(1));
                Clause clause = new Clause(new Literal("at", false, c1, c2),
                        new Literal("ee", true, c1),
                        new Literal("ee", true, c2));
                clauses.add(IsoClauseWrapper.create(clause));
            }
        } else {
            clauses.add(IsoClauseWrapper.create(Clause.parse("at(X,Y), !ee(X), !ee(Y).")));
        }

        if (addBaseEmbeddings) {
            return Template.create(entities, relations, clauses.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        } else {
            return Template.create(Sugar.set(), Sugar.set(), clauses.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        }
    }

    public static List<LearnedRule> selectTopKPerHead(List<LearnedRule> rules, int k) {
        Comparator<? super LearnedRule> comparator = Comparator.comparingDouble(e -> Double.parseDouble(e.getProperty("confidence")));
        MultiList<IsoClauseWrapper, LearnedRule> map = new MultiList<>();
        rules.forEach(rule -> map.put(IsoClauseWrapper.create(new Clause(LogicUtils.positiveLiterals(rule.getRule()))), rule));
        return map.values().stream().flatMap(list -> {
                    Collections.sort(list, comparator.reversed());
                    return list.stream().limit(k);
                }
        ).collect(Collectors.toList());
    }

    public static void variablesHistogram(List<LearnedRule> rules) {
        Counters<Integer> counter = new Counters<>();
        rules.forEach(rule -> counter.increment(rule.getRule().terms().size()));
        counter.keySet().stream().sorted().forEach(key -> System.out.println(key + "\t" + counter.get(key)));

        System.exit(-11);
    }

    public static List<LearnedRule> selectHighRecall(List<LearnedRule> rules, int k, int total) {
        Comparator<? super LearnedRule> comparator = Comparator.comparingDouble(e -> Double.parseDouble(e.getProperty("predicted")));
        MultiList<IsoClauseWrapper, LearnedRule> map = new MultiList<>();
        rules.forEach(rule -> map.put(IsoClauseWrapper.create(new Clause(LogicUtils.positiveLiterals(rule.getRule()))), rule));
        List<LearnedRule> selected = selectTopKPerHead(rules, k);
        Collections.sort(selected, comparator.reversed());
        selected = selected.stream().limit(total).collect(Collectors.toList());
        //variablesHistogram(selected);
        return selected;
    }

    public Template createTuplesEmbeddingTemplate(List<LearnedRule> rules, Set<String> entities, Set<String> relations, boolean addBaseEmbeddings, boolean addRelationEmbeddings) {
        Set<IsoClauseWrapper> clauses = Sugar.set();

        String targetLiteral = "p";
        String auxiliarTarget = "g";
        String tupleCombination = "t";
        String symmetry = "s";
        String ordering = "*o";

        // only for entity embeddings; not done for relations
        // creating new auxiliar predicate per variable-appearance of body
        for (LearnedRule rule : rules) {
            HornClause horn = HornClause.create(rule.getRule());
            LinkedHashSet<String> toAdd = new LinkedHashSet<>();
            Set<String> terms = Sugar.set();
            horn.head().terms().forEach(t -> terms.add(t.toString()));
            horn.body().literals().forEach(literal -> {
                for (int idx = 0; idx < literal.arity(); idx++) {
                    terms.add(literal.get(idx).toString());
                }
                if (addRelationEmbeddings) {
                    toAdd.add("!er(" + literal.predicate() + ")");
                }
            });
            Combinatorics.subset(Sugar.listFromCollections(terms), 2).forEach(tuple -> {
                toAdd.add("!" + symmetry + "(" + tuple.get(0) + "," + tuple.get(1) + ")");
            });
            toAdd.forEach(s -> horn.body().addLiteral(Literal.parseLiteral(s)));

            Literal head = horn.head();
            Literal newHead = new Literal(targetLiteral, Sugar.list(head.get(0), Constant.construct(head.predicate()), head.get(1)));

            List<Literal> literals = Sugar.listFromCollections(Sugar.list(newHead),
                    horn.body().literals().stream().map(literal -> {
                        if ("ee".equals(literal.predicate())
                                || "er".equals(literal.predicate())
                                || symmetry.equals(literal.predicate())
                                || tupleCombination.equals(literal.predicate())) {
                            return literal;
                        } else {
                            // to non-learnable R notation
                            return new Literal("*r",
                                    true,
                                    Sugar.list(literal.get(0), Constant.construct(literal.predicate()), literal.get(1)));
                        }
                    }).collect(Collectors.toList()));
            literals.add(new Literal("*" + auxiliarTarget, true, newHead.get(0), newHead.get(1), newHead.get(2)));
            clauses.add(IsoClauseWrapper.create(new Clause(literals)));
        }

        // adding symmetric

        // generating base non-lifted tuple-combination embeddings
        if (true) {
            Combinatorics.subset(Sugar.listFromCollections(entities), 2).forEach(tuple -> {
                String e1 = tuple.get(0);
                String e2 = tuple.get(1);
                if (e1.compareTo(e2) < 0) {
                    e1 = tuple.get(1);
                    e2 = tuple.get(0);
                }
                assert e1.compareTo(e2) < 0;
                clauses.add(IsoClauseWrapper.create(Clause.parse(tupleCombination + "(" + e1 + ", " + e2 + "), !ee(" + e1 + "), !ee(" + e2 + ").")));
            });
            clauses.add(IsoClauseWrapper.create(Clause.parse(symmetry + "(X,Y), !" + tupleCombination + "(X,Y).")));
            clauses.add(IsoClauseWrapper.create(Clause.parse(symmetry + "(X,Y), !" + tupleCombination + "(Y,X).")));
            clauses.add(IsoClauseWrapper.create(Clause.parse(symmetry + "(X,X), !ee(X).")));
            System.out.println("\n!!! odstranit manualne {dimenze} z pravidel s(X, Y) :- {50,50} t(Y, X).\n!!!");
        } else { // or lifted (which most likely will not work)
            clauses.add(IsoClauseWrapper.create(Clause.parse(symmetry + "(X,Y), !" + tupleCombination + "(X,Y), !" + ordering + "(X,Y).")));
            clauses.add(IsoClauseWrapper.create(Clause.parse(symmetry + "(X,Y), !" + tupleCombination + "(Y,X), !" + ordering + "(Y,X).")));

            clauses.add(IsoClauseWrapper.create(Clause.parse(tupleCombination + "(X, Y), !ee(X), !ee(Y).")));
            System.out.println("!should be used only if there are ordering in the example file among entities to get rid of symmetric cases!");
        }

        if (addBaseEmbeddings) {
            return Template.create(entities, relations, clauses.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        } else {
            return Template.create(Sugar.set(), Sugar.set(), clauses.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        }
    }

    public static List<LearnedRule> selectBaseRules(List<LearnedRule> rules, Set<String> relations) {
        Map<IsoClauseWrapper, LearnedRule> map = new HashMap<>();
        rules.forEach(rule -> {
            Set<Literal> positives = LogicUtils.positiveLiterals(rule.getRule());
            assert positives.size() == 1;
            Literal head = Sugar.chooseOne(positives);
            if (head.get(0) instanceof Variable && head.get(1) instanceof Variable) {
                IsoClauseWrapper key = IsoClauseWrapper.create(new Clause(positives));
                if (map.containsKey(key)) {
                    LearnedRule alreadyIn = map.get(key);
                    if (Double.parseDouble(alreadyIn.getProperty("predicted")) < Double.parseDouble(rule.getProperty("predicted"))) {
                        map.put(key, rule);
                    }
                } else {
                    map.put(key, rule);
                }
            }
        });

        if (relations.size() != map.size()) {
            System.out.println(relations.size() + "\t" + map.size());
            throw new IllegalStateException("some relation is missing!");
        }

        return Sugar.listFromCollections(map.values());
    }

    public Template createSoftUnificationEmbeddingTemplate(List<LearnedRule> rules, List<LearnedRule> baseRules, Set<String> entities, Set<String> relations, boolean addBaseEmbeddings) {
        Set<IsoClauseWrapper> clauses = Sugar.set();

        String targetLiteral = "p";
        String auxiliarTarget = "g";
        String specAux = "u";
        Set<String> auxiliars = Sugar.set();
        int ruleIdx = 0;

        baseRules.forEach(rule -> {
            HornClause horn = HornClause.create(rule.getRule());
            Set<Term> headTerms = horn.head().terms();
//            horn.body().literals().forEach(literal -> {
//                boolean isThere = headTerms.contains(literal.get(0)) || headTerms.contains(literal.get(1));
//                if (isThere) {
//                    rules.add(LearnedRule.get(rule.getProperties(), new Clause(Sugar.list(horn.head(), literal.negation()))));
//                }
//            });
            rules.add(LearnedRule.create(rule.getProperties(), new Clause(Sugar.list(horn.head()))));
        });

        // only for entity embeddings; not done for relations
        // creating new auxiliar predicate per variable-appearance of body
        for (LearnedRule rule : rules) {
            HornClause horn = HornClause.create(rule.getRule());
            Set<String> variables = Sugar.set();
            horn.head().terms().stream().filter(term -> term instanceof Variable).forEach(t -> variables.add(t.toString()));
            horn.body().literals().stream().flatMap(literal -> literal.terms().stream())
                    .filter(term -> term instanceof Variable)
                    .map(Term::toString)
                    .forEach(variables::add);

            List<String> sorted = Sugar.listFromCollections(variables);
            int finalRuleIdx = ruleIdx;
            IntStream.range(0, sorted.size()).forEach(idx -> {
                String currentPredicate = specAux + finalRuleIdx + "_" + idx;
                auxiliars.add(currentPredicate);
                horn.body().addLiteral(Literal.parseLiteral("!" + currentPredicate + "(" + sorted.get(idx) + ")"));
            });
            Literal head = horn.head();
            Literal newHead = new Literal(targetLiteral, Sugar.list(head.get(0), Constant.construct(head.predicate()), head.get(1)));

            List<Literal> literals = toRNotation(auxiliarTarget, specAux, horn, newHead);
            clauses.add(IsoClauseWrapper.create(new Clause(literals)));
            ruleIdx++;
        }

        // add u_x_y
        auxiliars.forEach(unary -> {
            entities.forEach(entity -> clauses.add(IsoClauseWrapper.create(Clause.parse(unary + "(" + entity + "), !ee(" + entity + ")"))));
        });


        if (addBaseEmbeddings) {
            return Template.create(entities, relations, clauses.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        } else {
            return Template.create(Sugar.set(), Sugar.set(), clauses.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
        }
    }

    private List<Literal> toRNotation(String auxiliarTarget, String specAux, HornClause horn, Literal newHead) {
        List<Literal> literals = Sugar.listFromCollections(Sugar.list(newHead),
                horn.body().literals().stream().map(literal -> {
                    if ("ee".equals(literal.predicate())
                            || "er".equals(literal.predicate())
                            || literal.predicate().startsWith(specAux)) {
                        return literal;
                    } else {
                        // to non-learnable R notation
                        return new Literal("*r",
                                true,
                                Sugar.list(literal.get(0), Constant.construct(literal.predicate()), literal.get(1)));
                    }
                }).collect(Collectors.toList()));
        literals.add(new Literal("*" + auxiliarTarget, true, newHead.get(0), newHead.get(1), newHead.get(2)));
        return literals;
    }

    public static List<LearnedRule> loadSelected(List<LearnedRule> rules, Path template, String start) {
        List<LearnedRule> result = Sugar.list();
        HashMap<IsoClauseWrapper, LearnedRule> map = new HashMap<>();
        System.out.println("putting into map");
        rules.forEach(rule -> map.put(IsoClauseWrapper.create(rule.getRule()), rule));
        System.out.println("going through rules");
        try {
            Files.lines(template).filter(line -> line.startsWith("{1,50} p(") && !line.contains("p(X,R,Y) :- embed_item1(X)"))
                    .forEach(line -> {
                        List<Literal> literals = Sugar.list();
                        line = line.replace("{1,50}", "");
                        line = line.replace("{50,50}", "");
                        String[] split = line.split(":-");
                        // head
                        Literal head = Sugar.chooseOne(Clause.parse(split[0]).literals());
                        literals.add(new Literal(head.get(1).toString(), Sugar.list(head.get(0), head.get(2))));

                        // body
                        Clause.parse(split[1]).literals().stream().filter(literal -> literal.predicate().equals("*r"))
                                .forEach(literal -> literals.add(new Literal(literal.get(1).toString(), true, Sugar.list(literal.get(0), literal.get(2)))));
                        result.add(map.get(IsoClauseWrapper.create(new Clause(literals))));
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }


        return result;
    }


    // just for binary predicates!
    public static Clause rename(Clause clause, Map<String, String> dict) {
        return new Clause(clause.literals().stream()
                .filter(literal -> !literal.predicate().startsWith("wanna"))
                .map(literal -> {
                    return new Literal(dict.get(literal.predicate()),
                            literal.isNegated(), rename(literal, dict));
                }).collect(Collectors.toList()));
    }

    private static List<Term> rename(Literal literal, Map<String, String> dict) {
        return Sugar.list(
                dict.containsKey(literal.get(0).toString()) ? Constant.construct(dict.get(literal.get(0).toString())) : literal.get(0),
                dict.containsKey(literal.get(1).toString()) ? Constant.construct(dict.get(literal.get(1).toString())) : literal.get(1)
        );
    }

    public static Possibilistic rename(Possibilistic theory, Map<String, String> dict) {
        List<Pair<Double, Clause>> rules = theory.getSoftRules().stream()
                .map(pair -> {
                    return new Pair<>(pair.getR(), rename(pair.getS(), dict));
                }).collect(Collectors.toList());

        return Possibilistic.create(Sugar.set(), rules);
    }

    public void sortAnyBURL(Path rules, double threshold) {
        Path out = Paths.get(rules + "_" + threshold);
        List<Pair<Double, String>> lines = Sugar.list();
        try {
            lines = Files.lines(rules).filter(l -> l.trim().length() > 0).map(line -> {
                String[] split = line.split("\\s+", 4);
                assert 4 == split.length;
                double confidence = -Double.parseDouble(split[2]);
                return new Pair<>(confidence, line);
            })
                    .filter(pair -> -pair.getR() >= threshold)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.write(out, lines.stream()
                    .sorted(Comparator.comparing(Pair::getR))
                    .map(Pair::getS)
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sortAnyBURL(Path rules) {
        Path out = Paths.get(rules + ".sorted");
        List<Pair<Double, String>> lines = Sugar.list();
        try {
            lines = Files.lines(rules).filter(l -> l.trim().length() > 0).map(line -> {
                String[] split = line.split("\\s+", 4);
                assert 4 == split.length;
                double confidence = -Double.parseDouble(split[2]);
                return new Pair<>(confidence, line);
            }).collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Files.write(out, lines.stream()
                    .sorted(Comparator.comparing(Pair::getR))
                    .map(Pair::getS)
                    .collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void splitFOL(Path path) {
        List<String> fol = Sugar.list();
        List<String> nonFol = Sugar.list();
        try {
            Files.lines(path).filter(l -> l.trim().length() > 0).forEach(line -> {
                String[] split = line.split("\\s+", 4);
                assert 4 == split.length;
                String[] ruleSplit = split[3].split("<=");
                assert 2 == ruleSplit.length;
                Clause head = Clause.parse(ruleSplit[0]);
                Clause body = Clause.parse(ruleSplit[1]);
                Clause rule = new Clause(Sugar.listFromCollections(head.literals(), body.literals().stream().map(Literal::negation).collect(Collectors.toList())));
                if (LogicUtils.constants(rule).isEmpty()) {
                    fol.add(line);
                } else {
                    nonFol.add(line);
                }
            });
            Files.write(Paths.get(path + ".fol"), fol);
            Files.write(Paths.get(path + ".partiallyGrounded"), nonFol);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}











