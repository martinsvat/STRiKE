/*
 * Copyright (c) 2015 Ondrej Kuzelka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package logicStuff.learning.datasets;

import ida.ilp.logic.Clause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.utils.Combinatorics;
import ida.utils.Sugar;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by ondrejkuzelka on 12/02/17.
 */
public class Yeast {


    public static Clause yeast() {
        return yeast("train/all");
    }

    public static Clause yeast(String db) {
        System.out.println("hardcoded, TODO dodelat, parametrizovat");
        //String path = "/home/svatoma1/development/lrnn/LRNN-WF/MLNStructureLearning/data/" + db + ".db";
        String path = "C:\\data\\school\\development\\LRNN\\MLNStructureLearning\\data\\" + db + ".db"; // original "/Users/kuzelkao_cardiff/Dropbox/Experiments/IJCAI17/protein/" + db + ".db"
        return yeastParseFromFile(path);
    }

    public static Clause yeastParseFromFile(String path) {
        List<Literal> literals = new ArrayList<Literal>();

        //long max = 200l;
        //System.out.println("watch out, taking in account only first " + max + " atoms from evidence");
        try {
            //long lines = 0l;
            for (String line : Sugar.readLines(new FileReader(path))) {
                line = line.trim();
                boolean exampleStart = false;
                if (line.startsWith("+ ") || line.startsWith("- ")) {
                    line = line.substring(1).trim();
                    exampleStart = true;
                }
                if (line.length() > 0) {
                    //lines++;
                    List<Literal> liters = Sugar.list();
                    if (exampleStart) {
                        liters.addAll(Clause.parse(line).literals());
                    } else {
                        liters.add(Literal.parseLiteral(line));
                    }
                    for (Literal l : liters) {
                        if (l.predicate().equals("interaction") || l.predicate().startsWith("enzyme") || l.predicate().startsWith("complex")) {
                            Literal newL = Sugar.chooseOne(LogicUtils.constantizeClause(new Clause(l)).literals());
                            literals.add(newL);
                            if (l.predicate().equals("interaction")) {
                                literals.add(new Literal(newL.predicate(), newL.get(1), newL.get(0)));
                            }
                        }
                        if (l.predicate().startsWith("function") || l.predicate().startsWith("phenotype") || l.predicate().startsWith("location") ||
                                l.predicate().startsWith("protein_class")) {
                            literals.add(Sugar.chooseOne(LogicUtils.constantizeClause(new Clause(l)).literals()));
                            //literals.add(new Literal(l.get(1).name(), Constant.construct(l.get(0).name().toLowerCase())));
                        }
                        if (l.predicate().startsWith("taughtBy") || l.predicate().startsWith("ta")
                                || l.predicate().startsWith("courseLevel") || l.predicate().startsWith("hasPosition")
                                || l.predicate().startsWith("projectMember") || l.predicate().startsWith("advisedBy")
                                || l.predicate().startsWith("inPhase") || l.predicate().startsWith("tempAdvised")
                                || l.predicate().startsWith("yearsIn") || l.predicate().startsWith("professor")
                                || l.predicate().startsWith("student") || l.predicate().startsWith("publication")) {
                            literals.add(Sugar.chooseOne(LogicUtils.constantizeClause(new Clause(l)).literals()));
                        }
                    }
                }
//                if(lines > max){
//                    break;
//                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return new Clause(literals);
    }

    public static void writeEvidenceForQueries(File folder) throws Exception {
        Random random = new Random(1);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        List<Literal> literals = Sugar.listFromCollections(yeast("test").literals());
        for (int i = 1; i < literals.size() - 1; i++) {
            PrintWriter pw = new PrintWriter(new FileWriter(folder.getAbsoluteFile() + "/queries" + i + ".db"));
            for (Literal l : Combinatorics.randomCombination(literals, i, random).toList()) {
                pw.println(LogicUtils.variabilizeClause(new Clause(l)));
            }
            pw.close();
        }
    }

    public static void main(String[] args) throws Exception {

        //Clause clause = yeastParseFromFile(Sugar.path("..", "datasets", "mlns", "yeast", "train.db"));
        Clause clause = yeastParseFromFile(Sugar.path("..", "datasets", "mlns", "uwcs", "all.db.transformed"));
        clause.literals().stream()
                .map(p -> p.predicate() + "/" + p.arity())
                .collect(Collectors.toSet())
                .forEach(System.out::println);

        //writeEvidenceForQueries(new File("/Users/kuzelkao_cardiff/Dropbox/Experiments/IJCAI17/yeast/yeast/queries/"));

//        Set<Literal> allLiterals = new HashSet<Literal>();
//        for (Literal l : LogicUtils.variabilizeClause(yeast("train/yeast.1")).literals()){
//            allLiterals.add(l);
//        }
//        for (Literal l : LogicUtils.variabilizeClause(yeast("train/yeast.2")).literals()){
//            allLiterals.add(l);
//        }
//        for (Literal l : LogicUtils.variabilizeClause(yeast("train/yeast.3")).literals()){
//            allLiterals.add(l);
//        }
//        for (Literal l : LogicUtils.variabilizeClause(yeast("train/yeast.4")).literals()){
//            allLiterals.add(l);
//        }
//        for (Literal l : LogicUtils.variabilizeClause(yeast("test/yeast.1")).literals()){
//            allLiterals.add(l);
//        }
//        for (Literal l : LogicUtils.variabilizeClause(yeast("test/yeast.2")).literals()){
//            allLiterals.add(l);
//        }
//        for (Literal l : LogicUtils.variabilizeClause(yeast("test/yeast.3")).literals()){
//            allLiterals.add(l);
//        }
//        for (Literal l : LogicUtils.variabilizeClause(yeast("test/yeast.4")).literals()){
//            allLiterals.add(l);
//        }
//        Set<Constant> ctrain = new HashSet<Constant>();
//        Set<Constant> ctest = new HashSet<Constant>();
//
//        Clause all = LogicUtils.constantizeClause(new Clause(allLiterals));
//
//        for (Constant c : LogicUtils.constants(all)){
//            if (Math.random() < 0.5){
//                ctrain.add(c);
//            } else {
//                ctest.add(c);
//            }
//        }
//        for (Literal l : LogicUtils.induced(all, ctrain).literals()){
//            System.out.println(l);
//        }
//        System.out.println("---------------\n\n\n");
//        for (Literal l : LogicUtils.induced(all, ctest).literals()){
//            System.out.println(l);
//        }
    }

}
