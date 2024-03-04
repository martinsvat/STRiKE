package logicStuff.theories;

import ida.ilp.logic.*;
import ida.ilp.logic.subsumption.Matching;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.utils.IntegerFunction;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;

import java.util.*;
import java.util.stream.Collectors;


/**
 * No support for special predicates.
 * <p>
 * Created by martin.svatos on 6. 12. 2018.
 */
public class TypedTheorySolver {

    public static boolean DEBUG = false;

    private int subsumptionMode = Matching.THETA_SUBSUMPTION;

    public final static int CUTTING_PLANES = 1, GROUND_ALL = 2;

    private int mode = CUTTING_PLANES; // GROUND_ALL;

    private int activeRuleSubsample = Integer.MAX_VALUE;

    private int activeRuleSubsamplingLevelStep = 1;

    private IntegerFunction restartSequence = new IntegerFunction.ConstantFunction(Integer.MAX_VALUE);

    private SatSolver satSolver = new SatSolver() {

        @Override
        public Set<Literal> solve(Collection<Clause> satProblem) {
            return new GroundTheorySolver(Sugar.setFromCollections(satProblem)).solve();
        }

        @Override
        public List<Set<Literal>> solveAll(Collection<Clause> satProblem, int maxCount) {
            return new GroundTheorySolver(Sugar.setFromCollections(satProblem)).solveAll(maxCount);
        }

        @Override
        public List<Set<Literal>> solveAll(Collection<Clause> satProblem, Set<Literal> groundAtoms, int maxCount) {
            return new GroundTheorySolver(Sugar.setFromCollections(satProblem), groundAtoms).solveAll(maxCount);
        }
    };

    public TypedTheorySolver(int mode, int subsumptionMode) {
        this.mode = mode;
        this.subsumptionMode = subsumptionMode;
    }

    // use when types are not given in evidence literals and rules
    public Set<Literal> solve(Collection<Clause> rules, final Set<Literal> evidence, Map<Pair<Predicate, Integer>, Type> typing) {
        Map<Pair<Predicate, Integer>, String> types = TypesInducer.simplify(typing);
        return solve(rules.stream().map(c -> LogicUtils.addTyping(c, types)).collect(Collectors.toList()),
                evidence.stream().map(l -> LogicUtils.addTyping(l, types)).collect(Collectors.toSet()));
    }


    // by faith we think that all rules are
    public Set<Literal> solve(Collection<Clause> rules, final Collection<Literal> evidence) {
        List<Constant> constants = Sugar.listFromCollections(LogicUtils.constantsFromLiterals(evidence));

        // hack here, because when clause is used for testing subsumption in parallel, then some error can occur in c.variableDomains[index].size() (dynamic creation of index, Ondra's email]
        Set<Pair<Clause, Clause>> cachedRulesWithNegations = rules.stream().map(c -> new Pair<>(c, LogicUtils.flipSigns(c))).collect(Collectors.toSet());

        Set<Literal> state = new HashSet<>();
        Set<Pair<Clause, Clause>> initRules = new HashSet<>();
        boolean allEvidencePositive = true;
        for (Literal e : evidence) {
            if (!e.isNegated()) {
                state.add(e);
            }
            Clause c = new Clause(Sugar.list(e));
            initRules.add(new Pair<>(c, LogicUtils.flipSigns(c)));
            allEvidencePositive = allEvidencePositive && !e.isNegated();
        }

        List<Pair<Clause, Clause>> groundRules = Sugar.list();
        List<Pair<Clause, Clause>> constraints = Sugar.list();
        List<Pair<Clause, Clause>> horn = Sugar.list();
        List<Pair<Clause, Clause>> moreThanOnePositiveLiteral = Sugar.list();
        for (Pair<Clause, Clause> pair : cachedRulesWithNegations) {
            Triple<Boolean, Boolean, Boolean> stats = ruleStats(pair.r);
            if (stats.r) {
                groundRules.add(pair);
            } else if (stats.s && !stats.t) {
                horn.add(pair);
            } else if (!stats.s && !stats.t) {
                constraints.add(pair);
            } else if (stats.t) {
                moreThanOnePositiveLiteral.add(pair);
            }
        }

        // just a speed-up for special case
        if (moreThanOnePositiveLiteral.isEmpty() && allEvidencePositive) {
            return solveHornTheoryOnly(evidence, constants, horn, constraints);
        }

        cachedRulesWithNegations = groundRules.isEmpty() ? cachedRulesWithNegations : Sugar.collectionDifference(cachedRulesWithNegations, groundRules);

        initRules.addAll(groundRules);
        if (this.mode == GROUND_ALL) {
            Matching world = constructState(initRules.stream().map(p -> Sugar.chooseOne(p.getR().literals())).collect(Collectors.toSet()), constants);
            initRules.addAll(groundAll(cachedRulesWithNegations, world)
                    .stream().map(c -> new Pair<>(c, LogicUtils.flipSigns(c))).collect(Collectors.toList()));
        }

        Set<Clause> activeRules = initRules.stream().map(Pair::getR).collect(Collectors.toSet());

        int iteration = 1;
        int restart = 0;
        while (true) {
            if (DEBUG) {
                System.out.println("Active rules: " + activeRules.size() + ", iteration: " + iteration);
            }
            if ((state = satSolver.solve(activeRules)) == null) {
                return null;
            }

            // s tim prvnim radkem to neprochazelo jednim testem
            List<Clause> violatedRules = findViolatedRules(Sugar.iterable(cachedRulesWithNegations, initRules), constructState(state, constants));
//            Set<Clause> violatedRules = Sugar.setFromCollections(findViolatedRules(rules, state)); // puvodni verze
//            activeRules.addAll(initRules); // tohle taky puvodni verze, melo by to byt spolecne s rakdme 141 pryc (radky uz nesouhlasi ;))
            activeRules.addAll(violatedRules);

            iteration++;
            if (violatedRules.isEmpty()) {
                break;
            }
            if (iteration >= this.restartSequence.f(restart)) {
                iteration = 0;
                restart++;
            }
        }
        return state;
    }

    private Matching constructState(Set<Literal> evidence, List<Constant> constants) {
        // the constantsIntroductionLiteral does contain all constants, not only the ones which are not in the current state ;)
        Literal constantsIntroductionLiteral = new Literal("", true, constants);
        return Matching.create(new Clause(Sugar.union(evidence, constantsIntroductionLiteral)), this.subsumptionMode);
    }

    public List<Clause> groundAll(Collection<Pair<Clause, Clause>> rules, Matching matching) {
        List<Clause> groundRules = Sugar.list();
        for (Pair<Clause, Clause> pair : rules) {
            Clause stub = pair.s;
            Pair<Term[], List<Term[]>> substitutions = matching.allSubstitutions(stub, 0, Integer.MAX_VALUE);
            for (Term[] subs : substitutions.s) {
                groundRules.add(LogicUtils.substitute(pair.r, substitutions.r, subs));
            }
        }
        return groundRules;
    }


    private Set<Literal> solveHornTheoryOnly(Collection<Literal> evidence, List<Constant> constants, List<Pair<Clause, Clause>> horn, List<Pair<Clause, Clause>> constraints) {
        Set<Literal> entailedLiterals = Sugar.set();
        Literal constantsIntroduction = new Literal("", constants); // constants introduction literal for no-range restricted clauses
        entailedLiterals.add(constantsIntroduction);
        entailedLiterals.addAll(evidence);
        List<Pair<Literal, Clause>> hornsExistentiallyQuantified = horn.stream().map(p -> new Pair<>(Sugar.chooseOne(LogicUtils.positiveLiterals(p.r)), p.s)).collect(Collectors.toList());
        Matching world = Matching.create(new Clause(entailedLiterals), this.subsumptionMode);
        int before = entailedLiterals.size();

        while (true) {

            before = entailedLiterals.size();
            for (Pair<Literal, Clause> hornPair : hornsExistentiallyQuantified) {
                Pair<Term[], List<Term[]>> subts = world.allSubstitutions(hornPair.s, 0, Integer.MAX_VALUE);
                for (Term[] image : subts.s) {
                    entailedLiterals.add(LogicUtils.substitute(hornPair.r, subts.r, image));
                }
            }

            for (Pair<Clause, Clause> constraintsPair : constraints) {
                Pair<Term[], List<Term[]>> subts = world.allSubstitutions(constraintsPair.s, 0, Integer.MAX_VALUE);
                if (!subts.s.isEmpty()) {
                    return null; // a constraint violated
                }
            }


            if (entailedLiterals.size() == before) {
                entailedLiterals.remove(constantsIntroduction);
                return entailedLiterals;
            }

            world = Matching.create(new Clause(entailedLiterals), this.subsumptionMode);
        }
    }

    // boolean of isGround, hasAtLeastOnePositiveLiteral, hasMoreThanOnePositiveLiterals
    private Triple<Boolean, Boolean, Boolean> ruleStats(Clause clause) {
        boolean isGround = true;
        int positiveLiterals = 0;
        for (Literal literal : clause.literals()) {
            isGround = isGround && LogicUtils.variables(literal).isEmpty();
            if (!literal.isNegated()) {
                positiveLiterals++;
            }
        }

        boolean horn = 1 == positiveLiterals;
        boolean moreThanHorn = positiveLiterals > 1;
        return new Triple<>(isGround, horn, moreThanHorn);
    }

    private List<Clause> findViolatedRules(Iterable<Pair<Clause, Clause>> rules, Matching matching) {
        List<Clause> violated = Sugar.list();
        for (Pair<Clause, Clause> pair : rules) {
            Clause rule = pair.r;
            Clause negatedRule = pair.s;
            Pair<Term[], List<Term[]>> substitutions = violatedSubstitutions(matching, negatedRule);
            for (Term[] subs : substitutions.s) {
//                violated.add(LogicUtils.substitute(rule, substitutions.r, subs));
                violated.add(new Clause(LogicUtils.substitute(rule, substitutions.r, subs).literals().stream().filter(l -> !l.predicate().equals(SpecialVarargPredicates.ALLDIFF)).collect(Collectors.toList())));
            }
        }
        return violated;
    }

    private Pair<Term[], List<Term[]>> violatedSubstitutions(Matching matching, Clause negatedRule) {
        // the trick is that the rules is existentially quantified, i.e. it is a negation of universally quantified rule
//        synchronized (negatedRule) {
        if (this.activeRuleSubsample == Integer.MAX_VALUE) {
            return matching.allSubstitutions(negatedRule, 0, Integer.MAX_VALUE);
        }
        Pair<Term[], List<Term[]>> substitutions0 = matching.allSubstitutions(negatedRule, 0, this.activeRuleSubsample);
        if (substitutions0.s.size() < this.activeRuleSubsample) {
            return substitutions0;
        } else {
            Triple<Term[], List<Term[]>, Double> substitutions = matching.searchTreeSampler(negatedRule, 0, this.activeRuleSubsample, this.activeRuleSubsamplingLevelStep);
            return new Pair<>(substitutions.r, substitutions.s);
        }
//        }
    }

    public boolean isGroundLiteralImplied(Literal l, Collection<Clause> theory, Collection<Literal> evidence) {
        if (!LogicUtils.isGround(l)) {
            throw new IllegalArgumentException("The first argument must be a ground literal.");
        }
        return null == this.solve(theory, Sugar.union(evidence, l.negation()));
    }


    public static void main(String[] args) {
        // untyped test
//        t1();
//        t2();
        t3();
        // typed test
//        tt1();
//        tt2();
//        tt3();
    }


    private static void t2() {
        List<Clause> theory = Sugar.list("!b(X,Y), b(Y,X)", "!b(X,Y)").stream().map(Clause::parse).collect(Collectors.toList());
        Set<Literal> evidence = Sugar.list("b(a,b)", "b(c,b)", "b(d,e)").stream().map(Literal::parseLiteral).collect(Collectors.toSet());

        TypedTheorySolver ts = new TypedTheorySolver(TypedTheorySolver.CUTTING_PLANES, Matching.THETA_SUBSUMPTION);
        System.out.println(ts.solve(theory, evidence));
    }

    private static void t1() {
        List<Clause> theory = Sugar.list("!b(X,Y), b(Y,X)").stream().map(Clause::parse).collect(Collectors.toList());
        Set<Literal> evidence = Sugar.list("b(a,b)", "b(c,b)", "b(d,e)").stream().map(Literal::parseLiteral).collect(Collectors.toSet());

        TypedTheorySolver ts = new TypedTheorySolver(TypedTheorySolver.CUTTING_PLANES, Matching.THETA_SUBSUMPTION);
        System.out.println(ts.solve(theory, evidence));
    }


    private static void t3() {
        List<Clause> theory = Sugar.list("!b(X,Y), b(Y,Z)").stream().map(Clause::parse).collect(Collectors.toList());
        Set<Literal> evidence = Sugar.list("b(a,b)", "b(b,a)", "a(g)").stream().map(Literal::parseLiteral).collect(Collectors.toSet());

        TypedTheorySolver ts = new TypedTheorySolver(TypedTheorySolver.CUTTING_PLANES, Matching.THETA_SUBSUMPTION);
        System.out.println(ts.solve(theory, evidence));
    }


    private static void tt1() {
        List<Clause> theory = Sugar.list("!b(X,Y), b(Y,Z)").stream().map(Clause::parse).collect(Collectors.toList());
        Set<Literal> evidence = Sugar.list("b(a,b)", "b(b,a)", "a(g)").stream().map(Literal::parseLiteral).collect(Collectors.toSet());

        TypedTheorySolver ts = new TypedTheorySolver(TypedTheorySolver.CUTTING_PLANES, Matching.THETA_SUBSUMPTION);
        Map<Pair<Predicate, Integer>, Type> typing = (new TypesInducer()).induce(evidence);
        System.out.println(ts.solve(theory, evidence, typing));
    }

    private static void tt2() {
        List<Clause> theory = Sugar.list("!b(1:X,1:Y), b(1:Y,1:Z)").stream().map(Clause::parse).collect(Collectors.toList());
        Set<Literal> evidence = Sugar.list("b(1:a,1:b)", "b(1:b,1:a)", "a(2:g)").stream().map(Literal::parseLiteral).collect(Collectors.toSet());

        TypedTheorySolver ts = new TypedTheorySolver(TypedTheorySolver.CUTTING_PLANES, Matching.THETA_SUBSUMPTION);
        System.out.println(ts.solve(theory, evidence));
    }

}
