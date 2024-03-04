package ida.searchPruning.graphs;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.learning.datasets.MEDataset;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Class for computation something like support (or confidence), but was dropped out and should be checked before next usage in graph-pruning part of the project.
 *
 * Created by martin.svatos on 30. 9. 2017.
 */
public class ConfidenceComputer {


    private final String pathToRules;
    private final String pathToDataset;
    private static final double MAX_SUPPORT = 0.21;

    public ConfidenceComputer(String pathToDataset, String pathToRules) {
        this.pathToDataset = pathToDataset;
        this.pathToRules = pathToRules;
    }

    private static ConfidenceComputer create(String pathToDataset, String pathToRules) {
        return new ConfidenceComputer(pathToDataset, pathToRules);
    }

    public static void main(String[] args) {
        /*
        try {
            List<Clause> list = loadInterpretations("C:\\data\\school\\development\\Xpruning\\Xpruning\\graphMining\\data\\gi50_screen_U251.txt");

            // vychazi z car(X1),!car(X0),!car(X2),!c2(X3),!ar_bond(X0,X1),!ar_bond(X1,X2),!ar_bond(X1,X0),!1_bond(X1,X3),!ar_bond(X2,X1),!1_bond(X3,X1)
            //Clause c = Clause.parse("car(X0),car(X2),c2(X3),ar_bond(X0,X1),ar_bond(X1,X2),ar_bond(X1,X0),1_bond(X1,X3),ar_bond(X2,X1),1_bond(X3,X1)");
            // cely podgraf
            //Clause c = Clause.parse("car(X1),car(X0),car(X2),c2(X3),ar_bond(X0,X1),ar_bond(X1,X2),ar_bond(X1,X0),1_bond(X1,X3),ar_bond(X2,X1),1_bond(X3,X1)");

            // negated horn
            Clause c = Clause.parse("!car(X1),car(X0),car(X2),c2(X3),ar_bond(X0,X1),ar_bond(X1,X2),ar_bond(X1,X0),1_bond(X1,X3),ar_bond(X2,X1),1_bond(X3,X1)");

            Matching m = new Matching();
            m.setSubsumptionMode(Matching.OI_SUBSUMPTION);
            int count = 0;
            int trued = 0;
            for (Clause clause : list){
                if(m.subsumption(c,clause)){
                    trued++;
                    //System.out.println(clause);//
                }
                count++;
            }
            System.out.println(count);
            System.out.println(trued);

        } catch (IOException e) {
            e.printStackTrace();
        }



        System.exit(-1);
        */
        //args = new String[]{"C:\\data\\school\\development\\graphMining\\graphMining\\data\\gi50_screen_786_0_partial.txt_reformatted_bonds", "C:\\data\\school\\development\\graphMining\\graphMining\\data\\rules.txt"};
        //args = new String[]{"C:\\data\\school\\development\\graphMining\\graphMining\\data\\gi50_screen_786_0_partial.txt", "C:\\data\\school\\development\\graphMining\\graphMining\\data\\rules.txt"};
        //args = new String[]{"C:\\data\\school\\development\\graphMining\\graphMining\\data\\gi50_screen_P388_ADR.txt", "C:\\data\\school\\development\\graphMining\\graphMining\\tmp"};
        /*args = new String[]{"C:\\data\\school\\development\\Xpruning\\Xpruning\\graphMining\\data\\gi50_screen_U251.txt",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\graphMining\\data\\rules.txt"};
    */
        /*args = new String[]{"C:\\data\\school\\development\\Xpruning\\Xpruning\\graphMining\\data\\gi50_screen_U251withoutCarC2.txt",
                "C:\\data\\school\\development\\Xpruning\\Xpruning\\graphMining\\data\\rules.txt"};*/


        if (args.length != 2) {
            System.out.println("use the confidence computer as *.jar pathToDataset pathToRules; wrong number of parameters right now");
            System.exit(-1);
        }
        System.out.println("hard-coded constants: max support:\t" + MAX_SUPPORT);
        ConfidenceComputer confidence = ConfidenceComputer.create(args[0], args[1]);
        confidence.computeConfidences().forEach(triplet -> System.out.println(triplet.r + " " + triplet.s + " " + triplet.t));
        System.out.println("overit ze se zobrazuje to co ma (asi jo, ale je to spis support nez confidence)");
    }

    private static void test() {
        //Clause h = Clause.parse("c3(X0), !o3(X1), !1_bond(X0, X1), !1_bond(X1, X0)");
        //Clause c = Clause.parse("c3(X1), !car(X0), !1_bond(X0, X1), !1_bond(X1, X0)");

        Clause h = Clause.parse("o2(X0), !c2(X1), !2_bond(X0, X1), !2_bond(X1, X0)".replaceAll("!", "~"));
        Clause c = Clause.parse("o2(X1), !c3(X0), !1_bond(X0, X1), !1_bond(X1, X0)".replaceAll("!", "~"));
        List<Clause> l = Sugar.list();
        l.add(h);
        Matching m = new Matching(l);
        m.setSubsumptionMode(Matching.OI_SUBSUMPTION);
        System.out.println(m.subsumption(c, 0));
        System.exit(-10);
    }

    /**
     * returns theory of triplets of <ruleConfidence, antecedentConfidence, stringRuleRepresentation>
     *
     * @return
     */
    private Stream<Triple<Double, Double, String>> computeConfidences() {
        List<Pair<Clause, String>> rulesMapping = null;
        try {
            rulesMapping = load(this.pathToRules);
        } catch (IOException e) {
            System.err.println("rules file cannot be opened (probably)");
            e.printStackTrace();
            System.exit(-10);
        }
        List<Clause> datasetClauses = null;
        try {
            datasetClauses = loadInterpretations(this.pathToDataset);
        } catch (IOException e) {
            System.err.println("dataset file cannot be opened (probably)");
            e.printStackTrace();
            System.exit(-11);
        }

        List<Double> targets = IntStream.range(0, datasetClauses.size()).mapToObj(x -> 1.0d).collect(Collectors.toList());
        MEDataset dataset = new MEDataset(new ArrayList<>(datasetClauses), targets);
        int datasetSize = dataset.getExamples().size();

        Set<IsoClauseWrapper> set = Sugar.set();
        rulesMapping = rulesMapping.stream()
                .filter(pair -> {
                    IsoClauseWrapper iso = new IsoClauseWrapper(pair.r);
                    if (set.contains(iso)) {
                        System.out.println("1 throwing away since it is isomorphic\t" + pair.r);
                        return false;
                    }
                    set.add(iso);
                    return true;
                }).collect(Collectors.toList());

        // uncomment for filter rules by subsumption
        // rulesMapping = filterSubsumed(rulesMapping);
        int hardThreshold = (int) (datasetSize * MAX_SUPPORT);

        return rulesMapping.stream()
                .map(pair -> {
                    Clause clause = pair.r;
                    List<Literal> negativeLiterals = clause.literals().stream().filter(literal -> literal.isNegated()).collect(Collectors.toList());
                    List<Literal> positiveLiterals = clause.literals().stream().filter(literal -> !literal.isNegated()).collect(Collectors.toList());
                    assert positiveLiterals.size() == 1;
                    Clause positiveBodyConjunction = LogicUtils.flipSigns(new Clause(negativeLiterals));
                    HornClause hc = new HornClause(positiveLiterals.get(0), positiveBodyConjunction);

                    // zkontrolovat numExistentialMatches
                    System.out.println("check out matching/dataset usage");
                    int ruleExistentialMatches = dataset.numExistentialMatches(hc, hardThreshold);
                    double ruleConfidence = ruleExistentialMatches / (double) datasetSize;

                    double antecedentSupport = -1d;
                    if (ruleExistentialMatches < hardThreshold) {
                        // zkontrolovat jestli se vola numexsmat !!!!!
                        antecedentSupport = dataset.numExistentialMatches(HornClause.create(LogicUtils.flipSigns(positiveBodyConjunction)), datasetSize) / (double) datasetSize;
                    }

                    return new Triple<>(ruleConfidence, antecedentSupport, pair.s);
                })
                //.filter(triple -> triple.r < MAX_SUPPORT)
                ;
    }

    private List<Pair<Clause, String>> filterSubsumed(List<Pair<Clause, String>> rulesMapping) {
        List<Pair<Clause, String>> filtered = Sugar.list();
        for (int idx = 0; idx < rulesMapping.size(); idx++) {
            List<Clause> example = Sugar.list();
            example.add(changeNegation(rulesMapping.get(idx).r));
            Matching matching = new Matching(example);
            matching.setSubsumptionMode(Matching.OI_SUBSUMPTION);
            boolean isSubsumed = false;

            for (int upper = idx + 1; upper < rulesMapping.size(); upper++) {
                if (matching.subsumption(changeNegation(rulesMapping.get(upper).r), 0)) {
                    //System.out.println("is subsumed by another " + rulesMapping.get(idx).s + "\t | \t" + rulesMapping.get(upper).s);
                    isSubsumed = true;
                    break;
                }
            }

            if (!isSubsumed) {
                filtered.add(rulesMapping.get(idx));
            } else {
                //System.out.println("is subsumed by another; throwing away");
            }
        }
        return filtered;
    }

    // move to utils (or generalize)
    static public List<Pair<Clause, String>> load(String dataPath) throws IOException {
        List<Pair<Clause, String>> list = Sugar.list();
        try (Stream<String> stream = Files.lines(Paths.get(dataPath))) {
            stream.forEach(line -> {
                int firstSpace = line.indexOf(' ');
                if (line.indexOf('(') > firstSpace && firstSpace != -1) {
                    // removing label
                    line = line.substring(line.indexOf(' '));
                }
                list.add(new Pair<>(Clause.parse(line), line));
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    static public List<Clause> loadInterpretations(String dataPath) throws IOException {
        return load(dataPath).stream().map(pair -> pair.r).collect(Collectors.toList());
    }

    private Clause changeNegation(Clause c) {
        if (c.toString().contains("~")) {
            System.out.println("problem will arise here since ~ is used within");
        }
        return Clause.parse(c.toString().replaceAll("!", "~"));
    }
}
