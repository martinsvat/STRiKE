package ida.pacReasoning.entailment;

import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.subsumption.Matching;
import ida.pacReasoning.entailment.collectors.Entailed;
import ida.pacReasoning.entailment.theories.FOL;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.entailment.theories.Theory;
import ida.pacReasoning.evaluation.Utils;
import ida.pacReasoning.supportEntailment.SupportEntailmentInference;
import ida.pacReasoning.supportEntailment.speedup.FastSubsetFactory;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import logicStuff.learning.saturation.ConjunctureSaturator;
import logicStuff.theories.TheorySolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 15. 6. 2018.
 */
public class Inference {

//    {
//        System.out.println("ukladat vypocetni cast bez overheadu, ktery je potreba");
//    }

    private final Map<Pair<Predicate, Integer>, String> typing;
    public boolean verbose = true;
    private boolean parallelInside = true;
    private Theory theory;
    private final Predicate allowedPredicate;
    private final boolean forceRewrite;

    public Inference(Theory theory, boolean forceRewrite) {
        this(theory, null, forceRewrite);
    }

    public Inference(Theory theory, Predicate allowedPredicate, boolean forceRewrite, Map<Pair<Predicate, Integer>, String> typing) {
        this.theory = theory;
        this.allowedPredicate = allowedPredicate;
        this.forceRewrite = forceRewrite;
        this.typing = typing;
    }

    public Inference(Theory theory, Predicate allowedPredicate, boolean forceRewrite) {
        this(theory, allowedPredicate, forceRewrite, null);
    }

    public void inferFolder(Path evidenceFolder, Path outputFolder, EntailmentSetting setting) {
        inferFolder(evidenceFolder, outputFolder, setting, null, 0);
    }

    public void inferFolder(Path evidenceFolder, Path outputFolder, EntailmentSetting setting, Integer atMost) {
        inferFolder(evidenceFolder, outputFolder, setting, atMost, 0);
    }

    public void inferFolder(Path evidenceFolder, Path outputFolder, EntailmentSetting setting, Integer atMost, java.util.function.Function<Integer, Path> storingLevelByLevel) {
        inferFolder(evidenceFolder, outputFolder, setting, atMost, 0, storingLevelByLevel, Sugar.set());
    }


    public void inferFolder(Path evidenceFolder, Path outputFolder, EntailmentSetting setting, Integer atMost, java.util.function.Function<Integer, Path> storingLevelByLevel, Set<Literal> baseEvidence) {
        inferFolder(evidenceFolder, outputFolder, setting, atMost, 0, storingLevelByLevel, baseEvidence);
    }

    public void inferFolder(Path evidenceFolder, Path outputFolder, EntailmentSetting setting, Integer atMost, int skip) {
        inferFolder(evidenceFolder, outputFolder, setting, atMost, skip, null, Sugar.set());
    }

    public void inferFolder(Path evidenceFolder, Path outputFolder, EntailmentSetting setting, Integer atMost, int skip, java.util.function.Function<Integer, Path> storingLevelByLevel, Set<Literal> baseEvidence) {
        if (!outputFolder.toFile().exists()) {
            outputFolder.toFile().mkdirs();
        }
        try {
            Stream<Path> stream = Files.list(evidenceFolder)
                    //.filter(file -> file.toFile().getName().startsWith("queries") && file.toFile().getName().endsWith(".db"))
                    .filter(file -> file.toFile().getName().endsWith(".db"))
                    .map(file -> new Pair<>(file, Utils.evidenceFileNumber(file)))
                    .sorted(Comparator.comparingInt(p -> p.getS()))
                    .filter(p -> p.s >= skip)
                    .map(p -> p.r);

            if (!forceRewrite) {
                stream = stream.filter(file -> !Paths.get(outputFolder.toString(), file.toFile().getName()).toFile().exists());
            }
//            System.out.println("at most natvrdo!");
//            stream = stream.filter(file -> Utils.evidenceFileNumber(file) <= 100);
            if (null != atMost) {
                stream = stream.filter(file -> Utils.evidenceFileNumber(file) <= atMost);
            }

            String modulo = System.getProperty("inference.modulo");
            if (null != modulo) {
                int mod = Integer.parseInt(modulo);
                stream = stream.filter(file -> 0 == Utils.evidenceFileNumber(file) % mod);
            }

            stream.forEach(file -> {
                if (verbose) {
                    System.out.println("# processing\t" + file.toFile().getName() + "\t" + file);
                }

                Function<Integer, Path> finalStoringLevelByLevel = (null == storingLevelByLevel) ? null
                        : (currentK) -> {
                    Path folder = storingLevelByLevel.apply(currentK);
                    if (!folder.toFile().exists()) {
                        folder.toFile().mkdirs();
                    }
                    return Paths.get(folder.toString(), file.toFile().getName());
                };

                FastSubsetFactory.getInstance().clear();
                Entailed entailed = infer(file, setting, finalStoringLevelByLevel, baseEvidence);
                Path output = Paths.get(outputFolder.toString(), file.toFile().getName());
                Path time = Paths.get(output + ".time");

                try {
                    if (null == storingLevelByLevel || setting.entailmentMode() == Entailment.All_EVIDENCE) { // otherwise this is done inside ;)
                        String entailedLiterals = entailed.asOutput();
                        if (entailedLiterals.contains("------SPLIT HERE------")) {
                            String[] splitted = entailedLiterals.split("------SPLIT HERE------");
                            Files.write(output, Sugar.list(splitted[0]));
                            Files.write(Paths.get(output + ".mp"), Sugar.list(splitted[1]));
                        } else {
                            Files.write(output, Sugar.list(entailedLiterals));
                        }

                    }
                    Files.write(time, Sugar.list(entailed.getTime() + ""));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Entailed infer(Path file, EntailmentSetting setting, java.util.function.Function<Integer, Path> storingLevelByLevel) {
        return infer(LogicUtils.loadEvidence(file), setting, storingLevelByLevel, Sugar.set());
    }


    public Entailed infer(Path file, EntailmentSetting setting, java.util.function.Function<Integer, Path> storingLevelByLevel, Set<Literal> baseEvidence) {
        return infer(LogicUtils.loadEvidence(file), setting, storingLevelByLevel, baseEvidence);
    }

    public Entailed infer(Set<Literal> evidence, EntailmentSetting setting, java.util.function.Function<Integer, Path> storingLevelByLevel, Set<Literal> baseEvidence) {
        long start = System.nanoTime();
        evidence = Sugar.union(evidence, baseEvidence);
        if (null != this.typing) {
            evidence = evidence.stream().map(l -> LogicUtils.addTyping(l, this.typing)).collect(Collectors.toSet());
        }

        if (setting.entailmentMode() == Entailment.R_ENTAILMENT) {
            REntailment re = REntailment.create(evidence);
            throw new IllegalStateException();//throw new NotImplementedException();
            //return re.entails(theory);
        }

        if ("support".equals(setting.getAlgorithm())) {
            /*if (setting.entailmentMode() == Entailment.All_EVIDENCE) {
                if (this.theory.getHardRules().stream().anyMatch(c -> LogicUtils.positiveLiterals(c).size() > 0)) {
                    throw new IllegalStateException("not implemented for support algorithm and all evidence inference");
                }
            }

            if (!(theory instanceof FOL)) {
                throw new IllegalStateException("support algorithm is implemented for FOL theory only");
            }*/

            // this is one big shit!
            SupportEntailmentInference sei = SupportEntailmentInference.create(evidence);
            if (setting.entailmentMode() == Entailment.All_EVIDENCE) {
                if (theory instanceof FOL) {
                    return sei.entails((FOL) theory).setTime(System.nanoTime() - start);
                }
                return sei.entails((Possibilistic) theory).setTime(System.nanoTime() - start);
            } else if (setting.entailmentMode() == Entailment.K_WITHOUT_CONSTRAINTS) {
                if (theory instanceof FOL) {
                    theory = FOL.create(LogicUtils.definiteRules(theory.getHardRules()), ((FOL) theory).getImplications());
                } else if (theory instanceof Possibilistic) {
                    theory = Possibilistic.create(LogicUtils.definiteRules(theory.getHardRules()), ((Possibilistic) theory).getSoftRules());
                } else {
                    throw new IllegalStateException();
                }
                return sei.entails(theory, setting.k()).setTime(System.nanoTime() - start);
            } else if (setting.entailmentMode() == Entailment.ONE_CHAIN_ENTAILMENT) {
                return sei.oneChain(theory).setTime(System.nanoTime() - start);
            } else if (setting.entailmentMode() == Entailment.ONE_STEP) {
                return sei.oneStep(theory).setTime(System.nanoTime() - start);
            } else if (setting.entailmentMode() == Entailment.All_EVIDENCE_WITHOUT_CONSTRAINTS) {
                if (theory instanceof FOL) {
                    FOL fol = (FOL) theory;
                    System.out.println("this should not be called at all");
                    return sei.entails(FOL.create(LogicUtils.definiteRules(fol.getHardRules()), fol.getImplications())).setTime(System.nanoTime() - start);
                } else if (theory instanceof Possibilistic) {
                    Possibilistic pos = (Possibilistic) theory;
                    return sei.entails(Possibilistic.create(LogicUtils.definiteRules(pos.getHardRules()), pos.getSoftRules())).setTime(System.nanoTime() - start);
                }
                throw new IllegalStateException();//throw new NotImplementedException();
            } else if (setting.entailmentMode() == Entailment.K_CONSTRAINTS_FILTER) {
                if (theory instanceof Possibilistic) {
                    theory = filterTheoryFromInconsistentConstraints((Possibilistic) theory, evidence);
                    return sei.entails(theory, setting.k()).setTime(System.nanoTime() - start);
                }
                throw new IllegalStateException();//throw new NotImplementedException();
            } else if (setting.entailmentMode() == Entailment.All_EVIDENCE_CONSTRAINTS_FILTER) {
                if (theory instanceof Possibilistic) {
                    theory = filterTheoryFromInconsistentConstraints((Possibilistic) theory, evidence);
                    return sei.entails((Possibilistic) theory).setTime(System.nanoTime() - start);
                }
                throw new IllegalStateException();//throw new NotImplementedException();
            }

            return sei.entails(theory, setting.k()).setTime(System.nanoTime() - start);
        }
        //System.out.println("without guarantees");

        Pair<Set<Literal>, Theory> pair = saturate(evidence, theory, setting);
        evidence = pair.r;
        this.theory = pair.s;

        Entailment engine = Entailment.create(evidence, setting);
        Entailed entailed = setting.entailmentMode() == Entailment.All_EVIDENCE // this is not nice
                ? engine.entails(theory, parallelInside, allowedPredicate, storingLevelByLevel)
                : engine.entails(theory, setting.k(), parallelInside, allowedPredicate, storingLevelByLevel);
        entailed = entailed.setTime(System.nanoTime() - start);
        SubsetFactory.getInstance().clear();
        return entailed;
    }

    // removes constraints which are inconsisten with the evidence
    private Possibilistic filterTheoryFromInconsistentConstraints(Possibilistic theory, Set<Literal> evidence) {
        Matching world = Matching.create(evidence, Matching.THETA_SUBSUMPTION);
        Set<Clause> nonviolatedConstraints = Sugar.set();
        for (Clause clause : theory.getHardRules()) {
            if (LogicUtils.isConstraint(clause)) {
                if (!world.subsumption(LogicUtils.flipSigns(clause), 0)) {
                    nonviolatedConstraints.add(clause);
                }
            } else {
                nonviolatedConstraints.add(clause);
            }
        }

        if (nonviolatedConstraints.size() != theory.getHardRules().size()) {
            System.out.println("constraints trimmed\t" + theory.getHardRules().size() + "\t->\t" + nonviolatedConstraints.size());
        }
        return Possibilistic.create(nonviolatedConstraints, theory.getSoftRules());
    }

    // that is the question where to put this... entailment is a instance w.r.t. evidence, theory is only a parameter, thus it would need to recompute mapping from constant to literals each time
    private Pair<Set<Literal>, Theory> saturate(Set<Literal> evidence, Theory theory, EntailmentSetting setting) {
        switch (setting.saturationMode()) {
            case Entailment.SYMMETRY_SATURATION:
                return symmetrySaturation(evidence, theory);
            case Entailment.FULL_SATURATION:
                ConjunctureSaturator saturator = ConjunctureSaturator.create(Sugar.listFromCollections(theory.getHardRules()), false, true);
                Clause saturated = saturator.saturate(new Clause(evidence));
                if (null != saturated) {
                    evidence = saturated.literals().stream().map(LogicUtils::constantize).collect(Collectors.toSet());
                }
                return new Pair<>(evidence, theory);
            case Entailment.NONE_SATURATION:
                return new Pair<>(evidence, theory);
        }
        throw new IllegalStateException("unknown type of saturation");
    }

    private Pair<Set<Literal>, Theory> symmetrySaturation(Set<Literal> evidence, Theory theory) {
        Pair<Set<Clause>, Set<Pair<String, Integer>>> pair = findSymmetries(theory.getHardRules());
        Set<Pair<String, Integer>> symmetricPredicates = pair.getS();
        Set<Clause> symmetric = pair.getR();

        // evidence saturation
        Set<Literal> saturatedEvidence = saturateEvidence(evidence, theory.getHardRules(), symmetricPredicates);

        return new Pair<>(saturatedEvidence, theory);
        // skipping this part for now
        /*
        //rule multiplication
        Set<Clause> nonSymetricRules = Sugar.setDifference(theory.getHardRules(), symmetric);
        // symmetric rules are given here so that no other literal is entailed by the theory
        Set<Clause> extendedRules = saturateHornHeads(symmetricPredicates, nonSymetricRules, symmetric);

        aka roznasobeni symetrii v implikovanych hlavach, aby se mohlo to symetricke pravidlo zahodit... asi zbytecnost

        nonSymetricRules.addAll(extendedRules);
        return new Pair<>(saturatedEvidence, nonSymetricRules);
        */
    }

    private Set<Clause> saturateHornHeads(Set<Pair<String, Integer>> symmetricPredicates, Set<Clause> rules, Collection<Clause> theory) {
        ConjunctureSaturator saturator = ConjunctureSaturator.create(Sugar.listFromCollections(theory), false, true);
        return rules.stream()
                .filter(clause -> {
                    Set<Literal> headPart = LogicUtils.positiveLiterals(clause);
                    if (headPart.size() != 1) {
                        return false;
                    }
                    return symmetricPredicates.contains(Sugar.chooseOne(headPart).getPredicate());
                })
                .flatMap(clause -> {
                    Set<Literal> posLit = LogicUtils.positiveLiterals(clause);
                    Literal lit = Sugar.chooseOne(posLit);
                    Clause conjuction = new Clause(posLit);
                    Clause saturatedHead = saturator.saturate(conjuction);
                    Set<Literal> body = LogicUtils.negativeLiterals(clause);
                    //saturatedHead.literals().forEach(c -> System.out.println("saturatedBody\t" + c));
                    return saturatedHead.literals().stream()
                            .filter(literal -> literal.getPredicate().equals(lit.predicate())) // just a check that only head of symmetricity is added.... to avoid problem when putting more theory than just symmetric rules
                            .map(head -> new Clause(Sugar.union(body, head)));
                })
                .collect(Collectors.toSet());
    }

    private Set<Literal> saturateEvidence(Set<Literal> evidence, Set<Clause> theory, Set<Pair<String, Integer>> symmetricPredicates) {
        Set<Literal> saturated = Sugar.setFromCollections(evidence);

        if (true) {
            // interaction only
            return Sugar.union(saturated,
                    saturated.stream()
                            .filter(l -> l.getPredicate().r.equals("interaction"))
                            .map(lit -> {
                                List<Term> arguments = lit.argumentsStream().collect(Collectors.toList());
                                Collections.reverse(arguments);
                                return new Literal(lit.predicate(), false, arguments);
                            })
                            .collect(Collectors.toList()));

        } else {
            List<Clause> merged = Sugar.listFromCollections(Sugar.union(evidence.stream().map(l -> new Clause(l)).collect(Collectors.toList()), theory));
            TheorySolver ts = new TheorySolver();
            ts.setSubsumptionMode(Matching.THETA_SUBSUMPTION);
            ts.setMode(TheorySolver.CUTTING_PLANES);
            Set<Literal> solved = ts.solve(merged);
            if (null != solved) {
                Set<Literal> implied = solved.stream()
                        .parallel()
                        .filter(literal -> !literal.isNegated() && symmetricPredicates.contains(literal.getPredicate())
                                        && true
                                // hack here for speed !!!!!!!!!
                                //&& TheorySimplifier.isGroundLiteralImplied(literal, merged, Matching.THETA_SUBSUMPTION, TheorySolver.CUTTING_PLANES)
                        )
                        .collect(Collectors.toSet());
                saturated.addAll(implied);
            }
            return saturated;
        }
    }

    private Pair<Set<Clause>, Set<Pair<String, Integer>>> findSymmetries(Collection<Clause> theory) {
        // add
        if (theory.stream().mapToInt(clause -> LogicUtils.positiveLiterals(clause).size()).anyMatch(size -> size > 1)) {
            // rules can have at most one positive literal
//            theory.stream().forEach(c -> {
//                if(LogicUtils.positiveLiterals(c).size() > 1){
//                    System.out.println(c);
//                }
//            });
            throw new IllegalStateException();//throw new NotImplementedException();
        }
        Set<Clause> symmetric = theory.stream()
                .filter(clause -> {
                    if (clause.countLiterals() != 2) {
                        return false;
                    }
                    Set<Literal> posLits = LogicUtils.positiveLiterals(clause);
                    if (posLits.isEmpty()) {
                        return false;
                    }
                    Literal lit = Sugar.chooseOne(posLits);
                    List<Term> arguments = lit.argumentsStream().collect(Collectors.toList());
                    Collections.reverse(arguments);
                    return clause.literals().contains(new Literal(lit.predicate(), true, arguments));
                })
                .collect(Collectors.toSet());

        Set<Pair<String, Integer>> symmetricPredicates = symmetric.stream()
                .map(clause -> Sugar.chooseOne(LogicUtils.positiveLiterals(clause)).getPredicate())
                .collect(Collectors.toSet());
        return new Pair<>(symmetric, symmetricPredicates);
    }

    private Set<Clause> multiplySymmetries(Clause clause, Collection<Clause> hardConstraints) {
        Pair<Set<Clause>, Set<Pair<String, Integer>>> pair = findSymmetries(hardConstraints);
        Set<Pair<String, Integer>> symmetricPredicates = pair.getS();
        Set<Clause> symmetric = pair.getR();
        // symmetric rules are given here so that no other literal is entailed by the theory
        return saturateHornHeads(symmetricPredicates, Sugar.set(clause), symmetric);
    }

}
