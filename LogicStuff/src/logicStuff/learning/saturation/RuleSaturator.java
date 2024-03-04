package logicStuff.learning.saturation;

import ida.ilp.logic.*;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;
import logicStuff.learning.datasets.Coverage;
import logicStuff.theories.TypedTheorySolver;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 9. 6. 2017.
 */
public class RuleSaturator implements Saturator<HornClause> {

    private final Collection<Clause> constraints;
    private final int theorySolverSubsumptionMode;
    private final TypedTheorySolver solver;
    //        private final TheorySolver solver;
    private final Map<String, Set<Literal>> herbrandCache = new HashMap<>();
    private final Set<Pair<String, Integer>> theoryPredicates;
    private final boolean parallel = 1 < Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1"))
            || Boolean.parseBoolean(System.getProperty("ida.searchPruning.parallel", "false"));
    private final Set<Pair<String, Integer>> allowedPredicates;
    private final boolean emptyAllowedPredicates;
    private final Map<Pair<Predicate, Integer>, Type> types;
    private final boolean noTypesRestriction;
    private final boolean disjunctionInput; // true->disjunction as input, false-> conjunction as input
    private final Map<Pair<Predicate, Integer>, String> typing;

    //private final boolean useForbidden = Boolean.parseBoolean(System.getProperty("ida.searchPruning.ruleSaturator.useForbidden", "false"));

    {
//        System.out.println("test jestli ten problem neni zpusoben spatnou implementaci theory solver");
//        System.out.println("hardcoded types here=null, stejnak je to nefukcni ted :))");
        //  System.out.println("useForbidden = " + Boolean.parseBoolean(System.getProperty("ida.searchPruning.ruleSaturator.useForbidden", "false")));
    }

    public RuleSaturator(Collection<Clause> constraints, int theorySolverSubsumptionMode, Set<Pair<String, Integer>> allowedPredicates, Map<Pair<Predicate, Integer>, Type> types, boolean disjunctionInput) {
        Map<Pair<Predicate, Integer>, String> typing = (null != types) && types.keySet().size() > 1 ? TypesInducer.simplify(types) : null;
        this.typing = typing;
        this.constraints = (null != types) && types.keySet().size() > 1 ? LogicUtils.addTyping(constraints, typing) : constraints; // in case that types is created by the fake make, or there is only one type, we drop the functionality
        this.theorySolverSubsumptionMode = theorySolverSubsumptionMode;

        this.solver = new TypedTheorySolver(TypedTheorySolver.CUTTING_PLANES, theorySolverSubsumptionMode);
//        this.solver = new TheorySolver();
//        this.solver.setMode(TheorySolver.CUTTING_PLANES);
//        this.solver.setSubsumptionMode(theorySolverSubsumptionMode);

        this.theoryPredicates = LogicUtils.predicates(constraints);
        this.allowedPredicates = allowedPredicates;
        this.emptyAllowedPredicates = allowedPredicates.isEmpty();
        this.types = types;
        this.noTypesRestriction = null == types || types.keySet().size() < 2;
        this.disjunctionInput = disjunctionInput;
    }


    /**
     * Returns empty set (or literals in the clause) iff no literals are implied, or clause is empty or null, or both positiveLiterals and negativeLiterals are se to false.
     * Returns null iff the clause contradicts the theory.
     *
     * @param clause
     * @return
     */
    private List<Literal> impliedLiterals(Clause clause, boolean positiveLiterals, boolean negativeLiterals, java.util.function.Predicate<Literal> isForbidden) {
        if (null == constraints || constraints.isEmpty()) {
            return Sugar.list();
        }

        /*if (LogicUtils.flipSigns(clause).toString().equals("complex(V1, V2), complex(V2, V1)")) {
            System.out.println("am here baby");
        }*/

        //List<Clause> theory = collectTheory(clause); // B \cup \lnot C\theta
        //Set<Literal> solved = literalHints(theory);

        if (!noTypesRestriction) {// TBD???? needed?
            clause = LogicUtils.addTyping(clause, typing);
        }

        List<Literal> transformedClause = transformClause(clause, this.disjunctionInput); // B \cup \lnot C\theta

        Set<Literal> solved = literalHints(this.constraints, transformedClause); // debug 0 test tedX
        if (null == solved) {
            return null;
        }

        Set<Literal> groundNegClause = Sugar.setFromCollections(transformedClause); // debug 0 test tedX

//         instead of this parallel.foreach.addAll it can be computed as the old way addAll(parallel.map.collectoToSet) -- test which one is faster
        Set<Literal> impliedLiterals = ConcurrentHashMap.newKeySet(); //  \lnot C'... it used to be \lnot (C' \ C)
        if (negativeLiterals) {// brute force method would used HerbrandBase.herbrandBase(theory).theory() instead
            Sugar.parallelStream(solved, this.parallel)
                    .filter(literal ->
                                    groundNegClause.contains(literal.negation())  // tenhle radek jde proti myslence ze v implied literals jsou \lnot (C' \ C), a sice ze tam je \lnot (C')
                                            || ((this.emptyAllowedPredicates || this.allowedPredicates.contains(literal.getPredicate()))
                                            && this.solver.isGroundLiteralImplied(literal, this.constraints, transformedClause)// debug 0 test tedX
//                                    && TheorySimplifier.isGroundLiteralImplied(literal, theory, Matching.THETA_SUBSUMPTION)// debug 0 test ted

                                    )
                    )
                    //.map(LogicUtils::variabilizeLiteral)
                    .forEach(impliedLiterals::add);
            /*impliedLiterals.addAll(Sugar.parallelStream(solved, this.parallel)
                    .filter(literal ->
                            groundNegClause.contains(literal.negation()) // fix here, without this, it would try literals from the clause
                                    || ((this.emptyAllowedPredicates || this.allowedPredicates.contains(literal.getPredicate()))
                                    && this.solver.isGroundLiteralImplied(literal, this.constraints, transformedClause)))
                    .map(LogicUtils::variabilizeLiteral)
                    .collect(Collectors.toSet()));*/
        }

        Set<Literal> modelAndImplied = solved.size() == impliedLiterals.size() ? solved : Sugar.union(solved, impliedLiterals); // optimization workaround

        // todo pokud mame constrain !location(V1,V1) tak se to prida do klauzule pro kazdou promennou :(, ale pokud to oriznem v refinement operatoru, tak to tady nemusime pocitat ani cele :))
        //System.out.println("tady zrychleni zanesenim constraints z orezaneho REF");
        if (positiveLiterals) {

            //tady je nejspise chyba pri pocitani implikovanych, rozhodne musime zamezit tomu aby aby se vytvarely tautologie -- filter po prochazeni HB
            Map<Term, Type> typesMap = this.noTypesRestriction ? null : constructTypesMap(clause);

            Stream<Literal> stream = cachedNegatedHerbrandBase(LogicUtils.predicates(clause), LogicUtils.constantsFromLiterals(groundNegClause)).stream();
            Sugar.parallelStream(stream, this.parallel)
                    /*.map(literal -> {
                        //System.out.println("trying to test\t" + l + "\n\t" + !groundNegClause.contains(l) + "\t" + !solved.contains(LogicUtils.variabilizeLiteral(l).negation()));
                        System.out.println(literal + "\t" +
                                (this.emptyAllowedPredicates || this.allowedPredicates.contains(literal.getPredicate())) + "\t" +
                                (this.noTypesRestriction || this.typesOk(literal, typesMap, types)) + "\t" +
                                !modelAndImplied.contains(literal.negation()) + "\t" +
                                TheorySimplifier.isGroundLiteralImplied(literal, theory, Matching.THETA_SUBSUMPTION)
                        );
                        return literal;
                    })*/
                    .filter(literal -> (this.emptyAllowedPredicates || this.allowedPredicates.contains(literal.getPredicate()))
                                    && (this.noTypesRestriction || this.typesOk(literal, typesMap, types)) // if type is not ok, then such literal wouldn't be added to the clause, thus there is no point of testing (and potentially adding) it here
                                    && !groundNegClause.contains(literal) // if (constantinized) literal is in the groundNegClause, it means that negation of the literal is in the original clause, which implies tautology (or contradiction)
                                    // we must not variabilize the literal
                                    //&& !solved.contains(LogicUtils.variabilizeLiteral(literal).negation()) // the same reason as above just looking to literals of a model (tautology or contradiction would occur)
                                    && !modelAndImplied.contains(literal.negation()) // the same reason as above just looking to literals of a model (tautology or contradiction would occur)
                                    // here the fact is that all of the literals forms a model, so they are either entailed (these are in the impliedLiterals) or they are not entailed but occur in at least one model, thus their negation cannot be entailed
                                    && this.solver.isGroundLiteralImplied(literal, this.constraints, transformedClause) // debug 0 test tedX
//                            && TheorySimplifier.isGroundLiteralImplied(literal, theory, Matching.THETA_SUBSUMPTION) // debug 0 test ted
                    )
                    // useForbidden je rozsireni, neco jako LB na saturace aby se nemusely pocitat vsechny ale nektery aby bylo jasny ze jsou trivialne splneny
                    // tohle nebude fungovat spravne protoze se musi rozlisovat mezi tim co je odstraneno pomoci constraints a co pomoci jinych veci (napr nulovy support)
                    //           && ((this.useForbidden && isForbidden.test(LogicUtils.variabilizeLiteral(literal).negation())) // tady se testuje litereal v konjunci, takze pro nase nastaveni je to takhle spravne, ale obecne by tam mela byt negace
                    //          || TheorySimplifier.isGroundLiteralImpliedWithOptimisation(literal, theory, this.theoryImplierSubsumptionMode))
/*                    ).map(literal -> { // debug tohle cele
                        if(isForbidden.test(LogicUtils.variabilizeLiteral(literal).negation())
                            && !TheorySimplifier.isGroundLiteralImpliedWithOptimisation(literal, theory, this.theoryImplierSubsumptionMode)){
                            System.out.println("divnost here\t" + literal + "\t" + clause);
                        }
                        return literal;
                    })*/
                    //.map(LogicUtils::variabilizeLiteral)
                    .forEach(impliedLiterals::add);
            //        .collect(Collectors.toSet())); // or do this to use the paralel->computation->collectorsToSet->addToImplied
        }

        Stream<Literal> retVal = impliedLiterals.stream().map(LogicUtils::variabilizeLiteral);
        if (disjunctionInput) {
            retVal = retVal.map(Literal::negation); // C'... it used to be C' \ C
        }
        return retVal.collect(Collectors.toList());
        // add varibialize to the line below
        //return disjunctionInput ? LogicUtils.flipSigns(impliedLiterals) : Sugar.listFromCollections(impliedLiterals); // C'... it used to be C' \ C
    }

    {
        System.out.println("tady je nejspise chyba pri pocitani implikovanych, rozhodne musime zamezit tomu aby aby se vytvarely tautologie -- filter po prochazeni HB");
    }

    private boolean typesOk(Literal literal, Map<Term, Type> typesMap, Map<Pair<Predicate, Integer>, Type> types) {
        Predicate pred = Predicate.create(literal.getPredicate());
        for (int idx = 0; idx < literal.arity(); idx++) {
            Type position = types.get(new Pair<>(pred, idx));
            if (!position.equals(typesMap.get(literal.get(idx)))) {
                return false;
            }
        }
        return true;
    }


    private Map<Term, Type> constructTypesMap(Clause clause) {
        Map<Term, Type> types = new HashMap<>();
        clause.literals().stream().forEach(literal -> IntStream.range(0, literal.arity()).forEach(idx -> types.put(literal.get(idx), this.types.get(new Pair<>(Predicate.create(literal.getPredicate()), idx)))));
        return types;
    }

    private Set<Literal> literalHints(Collection<Clause> theory, List<Literal> transformedClause) {
//        return solver.solve(theory);
        return solver.solve(theory, transformedClause);
    }

    private Pair<Collection<Clause>, List<Literal>> collectTheory(Clause clause, boolean disjunctionInserted) {
        return new Pair<>(this.constraints // B
                , transformClause(clause, disjunctionInserted)) // \lnot C\theta
                ;// B \cup \lnot C\theta

    }

    private List<Literal> transformClause(Clause clause, boolean disjunctionInserted) {
        Stream<Literal> stream = LogicUtils.constantizeClause(clause).literals().stream();
        if (disjunctionInserted) {
            stream = stream.map(Literal::negation);
        }
        return stream.collect(Collectors.toList()); // \lnot C\theta
    }

    private Set<Literal> cachedNegatedHerbrandBase(Set<Pair<String, Integer>> predicates, Set<Constant> constants) {
        synchronized (this.theoryPredicates) {
            if (this.theoryPredicates.containsAll(predicates)) {
                predicates = this.theoryPredicates;
            } else {
                predicates.addAll(this.theoryPredicates);
            }
            String canonic = String.join(",", constants.stream().sorted(Comparator.comparing(Object::toString)).collect(Collectors.toList())
                    + "|" + String.join(",", predicates.stream().map(p -> p.r + "/" + p.s).sorted().collect(Collectors.toList())));
            if (!this.herbrandCache.containsKey(canonic)) {
                this.herbrandCache.put(canonic, HerbrandBase.herbrandBaseFromPair(predicates, constants, typing).stream().map(Literal::negation).collect(Collectors.toSet()));
            }
            return this.herbrandCache.get(canonic);
        }
    }

    private Clause saturate(Clause clause, boolean positiveSaturations, boolean negativeSaturations, java.util.function.Predicate<Literal> isForbidden) {
        if (null == clause || clause.countLiterals() < 1) {
            return Clause.parse("");
        }
        List<Literal> extended = impliedLiterals(clause, positiveSaturations, negativeSaturations, isForbidden);
        if (null == extended) {
            return null;
        }
        extended.addAll(clause.literals());
        return new Clause(extended);

    }

    /**
     * returns null if constraints are violated by the candidate clause
     *
     * @param clause
     * @return
     */
    public Clause saturate(Clause clause, java.util.function.Predicate<Literal> isForbidden) {
        return saturate(clause, true, true, isForbidden);
    }

    public Clause saturate(Clause clause) {
        return saturate(clause, (l) -> false);
    }

    public Clause positiveSaturation(Clause clause, java.util.function.Predicate<Literal> isForbidden) {
        return saturate(clause, true, false, isForbidden);
    }

    public Clause positiveSaturation(Clause clause) {
        return positiveSaturation(clause, (l) -> false);
    }

    public Clause negativeSaturation(Clause clause, java.util.function.Predicate<Literal> isForbidden) {
        return saturate(clause, false, true, isForbidden);
    }

    public Clause negativeSaturation(Clause clause) {
        return negativeSaturation(clause, (l) -> false);
    }

    /**
     * Returns the rule with saturated body if the body can be saturated wrt the domain theory. Returns null if the body of the rule cannot hold wrt the theory.
     *
     * @param horn
     * @return
     */
    public HornClause saturate(HornClause horn, java.util.function.Predicate<Literal> isForbidden) {
        if (null == horn || horn.countLiterals() < 1) {
            return HornClause.create(Clause.parse(""));
        }
        List<Literal> impliedLiterals = impliedLiterals(disjunctionInput ? horn.toClause() : horn.toExistentiallyQuantifiedConjunction(), false, true, isForbidden);
        if (null == impliedLiterals) {
            return null;
        }
        impliedLiterals.remove(disjunctionInput ? horn.head() : horn.head().negation());
        if (disjunctionInput) {
            impliedLiterals = LogicUtils.flipSigns(impliedLiterals);
        }
        return HornClause.create(horn.head(), impliedLiterals);
    }

    public HornClause saturate(HornClause horn) {
        return saturate(horn, (l) -> false);
    }


    public boolean isRuleInconsistent(HornClause horn) {
        return null == impliedLiterals(disjunctionInput ? horn.toClause() : horn.toExistentiallyQuantifiedConjunction(), false, true, (l) -> false);
    }

    @Override
    public HornClause saturate(HornClause horn, Coverage parentsCoverage) {
        return saturate(horn, parentsCoverage, (l) -> false);
    }

    @Override
    public HornClause saturate(HornClause hornClause, Coverage parentsCoverages, java.util.function.Predicate<Literal> forbidden) {
        return saturate(hornClause, forbidden);
    }

    @Override
    public boolean isPruned(HornClause horn) {
        return isPruned(disjunctionInput ? horn.toClause() : horn.toExistentiallyQuantifiedConjunction());
    }

    @Override
    public boolean isPruned(HornClause hornClause, Coverage examples) {
        return isPruned(hornClause);
    }

    @Override
    public Collection<Clause> getTheory() {
        return this.constraints;
    }

    public boolean isPruned(Clause clause) {
        if (constraints.isEmpty()) {
            return false;
        }
        return null == literalHints(this.constraints, transformClause(clause, this.disjunctionInput));
    }


    public static RuleSaturator create(Collection<Clause> constraints, Set<Pair<String, Integer>> allowedPredicates, Map<Pair<Predicate, Integer>, Type> types, boolean disjunctionInput) {
        return new RuleSaturator(constraints, Matching.THETA_SUBSUMPTION, allowedPredicates, types, disjunctionInput);
    }

    public static RuleSaturator create(Collection<Clause> constraints) {
        return create(constraints, Sugar.set(), null, true);
    }

    public static RuleSaturator create(Set<IsoClauseWrapper> constraints) {
        return create(constraints.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()), Sugar.set(), null, true);
    }

    public static void main(String[] args) {
        //tests();
        //t3();

        //HornClause hc = HornClause.parse("phenotype(A, B) <- interaction(C, A), phenotype(C, B), protein_class(C, D), protein_class(A, D)");
        HornClause hc = HornClause.parse("phenotype(V0, V2) <- interaction(V0, V1), phenotype(V1, V2), protein_class(V0, V3), protein_class(V1, V3)");
        HornClause hc2 = HornClause.parse("phenotype(V1, V2) <- interaction(V0, V1), phenotype(V0, V2), protein_class(V1, V3), protein_class(V0, V3)");
        RuleSaturator saturator = RuleSaturator.create(Sugar.list("interaction(A,B),!interaction(B,A)").stream().map(Clause::parse).collect(Collectors.toList()));
        HornClause saturated = saturator.saturate(hc);
        HornClause saturated2 = saturator.saturate(hc2);
        System.out.println(hc + "\n" + saturated + "\n" + hc2 + "\n" + saturated2 + "\nare equal\t" + IsoClauseWrapper.create(saturated.toClause()).equals(IsoClauseWrapper.create(saturated2.toClause())));
    }

    private static void tests() {

        Sugar.list(new Triple<>("bond(X,Y)", Sugar.list("!bond(A,B),bond(B,A)"), "bond(X,Y),bond(Y,X)"),
                new Triple<>("!bond(X,Y)", Sugar.list("!bond(A,B),bond(B,A)"), "!bond(X,Y),!bond(Y,X)"),
                new Triple<>("bond(X,Y)", Sugar.list("!bond(X,Y),p(X)"), "bond(X,Y)"),
                new Triple<>("!bond(X,Y)", Sugar.list("!bond(X,Y),p(X)"), "!bond(X,Y),!p(X)"),
                new Triple<>("bond(X,Y)", Sugar.list("!bond(A,B),bond(B,A)", "!bond(A,B),p(A)"), "bond(X,Y),bond(Y,X)"),
                new Triple<>("!bond(X,Y)", Sugar.list("!bond(A,B),bond(B,A)", "!bond(A,B),p(A)"), "!bond(X,Y),!bond(Y,X),!p(X),!p(Y)"),
                new Triple<>("!bond(X,Y),bond(Y,Z)", Sugar.list("!bond(A,B),bond(B,A)", "!bond(A,B),p(A)"), "!bond(X,Y),bond(Y,Z),bond(Z,Y),!bond(Y,X),!p(X),!p(Y)"),
                new Triple<>("bond(X,Y),p(X)", Sugar.list("!bond(X,Y),p(X)"), "bond(X,Y),p(X),bond(X,X)"),
                new Triple<>("!s(X),!p(X)", Sugar.list("!p(A),!s(A)"), null),
                new Triple<>("s(X),p(X)", Sugar.list("!p(A),!s(A)"), "s(X),p(X)"),
                new Triple<>("s(X)", Sugar.list("bond(A,A)"), "s(X),!bond(X,X)"),
                new Triple<>("!s(X)", Sugar.list("bond(A,A)"), "!s(X),!bond(X,X)"),
                new Triple<>("s(X)", Sugar.list("!bond(A,A)"), "s(X),bond(X,X)"),
                new Triple<>("!s(X)", Sugar.list("!bond(A,A)"), "!s(X),bond(X,X)"),
                new Triple<>("!p(X)", Sugar.list("!bond(A,B),p(A)", "!bond(A,B),bond(B,A)"), "!p(X)"),
                new Triple<>("!s(X)", Sugar.list("b(A,A)", "q(A)"), "!s(X),!b(X,X),!q(X)")
        )
                .forEach(t -> {
                    Triple<String, List<String>, String> triple = (Triple<String, List<String>, String>) t;
                    test(triple.r, triple.s, triple.t);
                });

    }


    private static void test(String clause, List<String> constraints, String saturatedGoal) {
        System.out.println("\nstarting test\n==========");
        List<Clause> rules = constraints.stream().map(str -> Clause.parse(str)).collect(Collectors.toList());
        RuleSaturator saturator = RuleSaturator.create(rules, Sugar.set(), null, true);
        Clause rule = Clause.parse(clause);
        Clause saturated = saturator.saturate(rule, (l) -> false);

        System.out.println("theory");
        rules.forEach(r -> System.out.println("\t" + r));
        System.out.println("clause\n\t" + rule);
        System.out.println("goal\n\t" + saturatedGoal);
        System.out.println("saturated clause\n\t" + saturated);
        boolean equals = false;
        if (null == saturated || null == saturatedGoal) {
            equals = null == saturated && null == saturatedGoal;
        } else {
            equals = IsoClauseWrapper.create(saturated).equals(IsoClauseWrapper.create(Clause.parse(saturatedGoal)));
        }
        System.out.println("TEST RESULT:\t" + (equals ? "OK" : "WRONG !") + "\n==========\n");

    }

    private static void t3() {
        List<String> strConstraintsConstraints = Sugar.list("o3(V1)", "        pd(V1)", "        oco2(V1)", "        s3(V1)", "        !am_bond(V2, V2)", "        ar_bond(V2, V2)", "        as(V1)", "        hg(V1)", "        se(V1)", "        so(V1)", "        ar_bond(V1, V2)", "        nar(V1)", "        eu(V1)", "        cr(V1)", "        am_bond(V2, V2)", "        i(V1)", "       o2(V1)", "        !3_bond(V2, V2)", "        c3(V1)", "        npl3(V1)", "        s2(V1)", "        br(V1)", "        n2(V1)", "        p3(V1)", "        1_bond(V2, V2)", "        3_bond(V2, V2)", "        c2(V1)", "        ge(V1)", "        n4(V1)", "        3_bond(V1, V2)", "        cl(V1)", "        car(V1)", "        f(V1)", "        zn(V1)", "        !1_bond(V2, V2)", "        !ar_bond(V2, V2)", "        sn(V1)", "        cd(V1)", "        ni(V1)", "        nam(V1)", "        au(V1)", "       am_bond(V1, V2)", "        !2_bond(V2, V2)", "        so2(V1)", "        pb(V1)", "        c1(V1)", "        1_bond(V1, V2)", "        cu(V1)", "        n1(V1)", "        fe(V1)", "        ru(V1)", "        n3(V1)", "        2_bond(V1, V2)", "        2_bond(V2, V2)", "        pt(V1)");
        List<Clause> constraints = strConstraintsConstraints.stream().map(str -> Clause.parse(str)).collect(Collectors.toList());

        //RuleSaturator saturator = RuleSaturator.create(constrain, Matching.THETA_SUBSUMPTION);
        RuleSaturator saturator = RuleSaturator.create(constraints, Sugar.set(), null, true);
        //HornClause horn = new HornClause(Clause.parse("!bond(V1, V2), bond(V2, V1)"));
        Clause rule = LogicUtils.flipSigns(Clause.parse("c1(V1), !s2(V2)"));

        Clause saturated = saturator.saturate(rule, (l) -> false);

        System.out.println("horn\n\t" + rule);
        System.out.println("saturated\n\t" + saturated);
    }
}