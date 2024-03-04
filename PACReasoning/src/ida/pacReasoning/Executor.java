package ida.pacReasoning;

import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.entailment.EntailmentSetting;
import ida.pacReasoning.entailment.Inference;
import ida.pacReasoning.entailment.theories.FOL;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.entailment.theories.Theory;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import logicStuff.typing.TypesInducer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

public class Executor {

    public static void main(String[] args) {
        Executor executor = new Executor();
        Theory theory = Possibilistic.create(Paths.get(System.getProperty("ida.pacReasoning.theory", "")));
        theory = executor.setEntailmentModeAndChangeTheory((Possibilistic) theory);
        String p = System.getProperty("ida.pacReasoning.inputFolder");
        if(null == p || !Paths.get(p).toFile().exists()){
            throw new IllegalStateException("Input folder not set up or unknownd: ida.pacReasoning.inputFolder=" + System.getProperty("ida.pacReasoning.inputFolder"));
        }
        Path queries = Paths.get(p);
        executor.runInference(theory, queries, null);
    }

    private Theory setEntailmentModeAndChangeTheory(Possibilistic pl) {
        String entailment = System.getProperty("ida.pacReasoning.entailment");
        String k = System.getProperty("ida.pacReasoning.entailment.k");
        Theory theory;
        if ("classical".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "a");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "classical");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = FOL.create(pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() < 1).collect(Collectors.toSet()),
                    Sugar.union(pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toList()), pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toList())),
                    pl.getName());
        } else if ("k".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "k");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "classical");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = FOL.create(pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() < 1).collect(Collectors.toSet()),
                    Sugar.union(pl.getSoftRules().stream().map(Pair::getS).collect(Collectors.toList()), pl.getHardRules().stream().filter(c -> LogicUtils.positiveLiterals(c).size() == 1).collect(Collectors.toList())),
                    pl.getName());
        } else if ("PL".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "a");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
//            System.setProperty("ida.pacReasoning.entailment.algorithm", "serachBased");
            theory = pl;
        } else if ("PLWC".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "awc");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
//            System.setProperty("ida.pacReasoning.entailment.algorithm", "serachBased");
            theory = pl;
        } else if ("kPL".equals(entailment) || "strike".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "k");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("kPLWC".equals(entailment) || "strikeWC".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "kWC");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("kPLf".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "kf");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("PLf".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "Af");
            System.setProperty("ida.pacReasoning.entailment.k", "" + k);
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else if ("oneS".equals(entailment)) {
            System.setProperty("ida.pacReasoning.entailment.mode", "oneS");
            System.setProperty("ida.pacReasoning.entailment.k", "0");
            System.setProperty("ida.pacReasoning.entailment.logic", "pl");
            System.setProperty("ida.pacReasoning.entailment.algorithm", "support");
            theory = pl;
        } else {
            System.out.println("ending since inference set to none");
            throw new IllegalStateException("Can't continue: there is an unknown inference mode set up: -Dida.pacReasoning.entailment=" + entailment);
        }
        return theory;
    }

    public void runInference(Theory theory, Path queries, Path typed) {
        Map<Pair<Predicate, Integer>, String> typing = null == typed ? null : TypesInducer.load(typed);
        EntailmentSetting setting = EntailmentSetting.create();

        String modulo = System.getProperty("inference.modulo", "");
        String inputDir = queries.toFile().getName();
        Path outputEvidence = Paths.get(System.getProperty("ida.pacReasoning.outputFolder", ".\\"));
        System.out.println("# input evidence\t" + inputDir);
        System.out.println("# output evidence\t" + outputEvidence);
        System.out.println("# entailment mode \t" + setting.entailmentMode());
        System.out.println("# executing inference");
        java.util.function.Function<Integer, Path> storingLevelByLevel = null;

        Inference inference = new Inference(theory, null, true, typing);
        inference.inferFolder(queries, outputEvidence, setting, Integer.MAX_VALUE, storingLevelByLevel);
    }


}
