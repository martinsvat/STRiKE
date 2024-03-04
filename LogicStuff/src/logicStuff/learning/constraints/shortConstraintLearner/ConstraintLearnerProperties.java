package logicStuff.learning.constraints.shortConstraintLearner;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.Predicate;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.utils.collections.FakeMap;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.learning.languageBias.LanguageBias;
import logicStuff.learning.languageBias.NoneBias;
import logicStuff.learning.saturation.*;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;

//throw new NotImplementedException();
//throw new UnsupportedOperationException("Not implemented yet");

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 29. 12. 2017.
 */
public class ConstraintLearnerProperties {


    private final int maxLiterals;
    private final int maxVariables;
    private final int maxComponents;
    private final int maxPosLit;
    private final int maxNegLit;
    private final int saturationMode;
    private final String learnerVersion;
    private final boolean typeRefinement;
    private final int minSupport;
    private final int sampledBeam;
    private final String adaptiveConstraintMining; // should be probably separate; now it's here only because of my laziness
    private final boolean useSaturations;
    private final boolean oneLiteralHCrefinement;
    private final boolean hcExtendedRefinement;


    public ConstraintLearnerProperties(int maxLiterals, int maxVariables, int maxComponents, int maxPosLit, int maxNegLit, int saturationMode, String learnerVersion, int minSupport, int sampledBeam, String adaptiveConstraintMining, boolean useSaturations, boolean typeRefinement, boolean oneLiteralHCrefinement, boolean hcExtendedRefinement) {
        this.maxLiterals = maxLiterals;
        this.maxVariables = maxVariables;
        this.maxComponents = maxComponents;
        this.maxPosLit = maxPosLit;
        this.maxNegLit = maxNegLit;
        this.saturationMode = saturationMode;
        this.learnerVersion = learnerVersion;
        this.minSupport = minSupport;
        this.sampledBeam = sampledBeam;
        this.adaptiveConstraintMining = adaptiveConstraintMining;
        this.useSaturations = useSaturations;
        this.typeRefinement = typeRefinement;
        this.oneLiteralHCrefinement = oneLiteralHCrefinement;
        this.hcExtendedRefinement = hcExtendedRefinement;
    }

    public List<String> propertiesToList() {
        return Sugar.list(maxLiterals, maxVariables, maxComponents, maxPosLit,
                maxNegLit, saturationMode, learnerVersion, minSupport, sampledBeam, adaptiveConstraintMining,
                useSaturations, typeRefinement, oneLiteralHCrefinement, hcExtendedRefinement).stream()
                .map(e -> "" + e).collect(Collectors.toList());
    }


    public static ConstraintLearnerProperties create() {
        // create default
        String learner = System.getProperty("ida.logicStuff.constraints.learner", "complete");
        int maxLiterals = Integer.parseInt(System.getProperty("ida.logicStuff.constraints.maxLiterals", "0"));
        if (learner.startsWith("sampling")) {
            maxLiterals = Integer.parseInt(learner.split("-")[0].substring("sampling".length()));
        }
        return new ConstraintLearnerProperties(
                maxLiterals,
                Integer.parseInt(System.getProperty("ida.logicStuff.constraints.maxVariables", (2 * maxLiterals) + "")),
                Integer.parseInt(System.getProperty("ida.logicStuff.constraints.maxComponents", "1")),
                Integer.parseInt(System.getProperty("ida.logicStuff.constraints.maxPosLit", "1")),
                Integer.parseInt(System.getProperty("ida.logicStuff.constraints.maxNegLit", "" + Integer.MAX_VALUE)),
                Integer.parseInt(System.getProperty("ida.logicStuff.constraints.saturationMode", "2")),
                learner,
                Integer.parseInt(System.getProperty("ida.logicStuff.constraints.minSupport", "1")),
                Integer.parseInt(System.getProperty("ida.logicStuff.constraints.sampledBeam", "100")),
                System.getProperty("ida.searchPruning.mining.bfs.adaptiveConstraints", "none"),
                Boolean.parseBoolean(System.getProperty("ida.logicStuff.constraints.useSaturation", "true")),
                Boolean.parseBoolean(System.getProperty("ida.logicStuff.constraints.useTypes", "false")),
                Boolean.parseBoolean(System.getProperty("ida.logicStuff.constraints.oneLiteralHCrefinement", "false")),
                Boolean.parseBoolean(System.getProperty("ida.logicStuff.constraints.hcExtendedRefinement", "false"))
        );
    }

    public Pair<List<Clause>, List<Clause>> learnConstraints(DatasetInterface med) {
        return learnConstraints(med, NoneBias.create());
    }

    /**
     * returns pair composed of domain constraints (for saturation) and constraints with minsupport (only for pruning)
     *
     * @param med
     * @return
     */
    public Pair<List<Clause>, List<Clause>> learnConstraints(DatasetInterface med, LanguageBias bias) {
        if (learnerVersion.startsWith("none")) {
            return new Pair<>(Sugar.list(), Sugar.list());
        } else if (learnerVersion.startsWith("smarter")) {
            Set<Clause> typesTheory = Sugar.set();
            Map<Pair<Predicate, Integer>, Type> types = alwaysOneTypeMap();

            String typesSource = System.getProperty("ida.logicStuff.constraints.loadTypes");
            if (typeRefinement && null != typesSource) {
                types = TypesInducer.transform(TypesInducer.load(Paths.get(typesSource)));
            } else if (typeRefinement) {
                if (!(med instanceof MEDataset)) {
                    throw new IllegalStateException();//NotImplementedException();
                }
                TypesInducer inducer = new TypesInducer();
                Map<Pair<Predicate, Integer>, Type> r1 = inducer.induce((MEDataset) med);
                Map<Pair<Predicate, Integer>, Type> r2 = inducer.rename(r1);
                //typesTheory = Sugar.setFromCollections(inducer.generateRules(r2));

                System.out.println("todo : tady to generateConstraints nefunguje jak by melo se mi zda");

                //typesTheory = Sugar.setFromCollections(inducer.generateConstraints(r2));

                System.out.println("types theory");
                inducer.generateRules(r2).forEach(t -> System.out.println("\t" + t));

                System.out.println("theory");
                typesTheory.forEach(t -> System.out.println(t));
                types = r2; // oh, shit :(

                System.out.println("\njust a recap");
                types.entrySet().stream().map(e -> e.getKey() + "\t" + e.getValue().getId()).sorted().forEach(System.out::println);
                med = inducer.typeDatasetByTyping((MEDataset) med, types);
                //System.exit(-10000);

                //med = inducer.extendDataset((MEDataset) med, typesTheory);

                /*System.out.println("first");
                r1.entrySet().forEach(e-> System.out.println(e.getKey() + "\t" + e.getValue().getId()));
                System.out.println("second");
                r2.entrySet().forEach(e-> System.out.println(e.getKey() + "\t" + e.getValue().getId()));

                System.out.println("first");
                inducer.generateRules(r1).forEach(System.out::println);

                System.out.println("second");
                inducer.generateRules(r2).forEach(System.out::println);
                System.exit(10);*/
            }

            UltraShortConstraintLearnerFasterSmarter learner = UltraShortConstraintLearnerFasterSmarter.create(med, this.maxLiterals, this.maxVariables, 0,
                    this.maxComponents, this.maxPosLit, this.maxNegLit, null, bias, types, this.oneLiteralHCrefinement, this.hcExtendedRefinement);
            return learner.learnConstraints(this.useSaturations, false, false, Integer.MAX_VALUE, Sugar.list(),
                    false, this.saturationMode, false, Sugar.set(), this.minSupport);

        } else if (learnerVersion.startsWith("complete")) {
            UltraShortConstraintLearnerFaster learner = UltraShortConstraintLearnerFaster.create(med, this.maxLiterals, this.maxVariables, 0,
                    this.maxComponents, this.maxPosLit, this.maxNegLit, null, bias);
            return learner.learnConstraints(this.useSaturations, false, false, Integer.MAX_VALUE, Sugar.list(),
                    false, this.saturationMode, false, Sugar.set(), this.minSupport);
        } else if (learnerVersion.startsWith("sampling")) {

            System.out.println("tady doplnit to ze sampler constraint lerner by mel v sobe zahrnout to, ze bude pouzivat smarter verzi oproti fast verzi USCL");

            int literals = this.maxLiterals;
            int rounds = 10;
            int samplingBeam = sampledBeam;
            List<Integer> parameters = Arrays.stream(learnerVersion.substring("sampling".length()).split("-"))
                    .map(Integer::parseInt).collect(Collectors.toList());
            if (parameters.size() != 3) {
                throw new IllegalArgumentException("resolve parsing of parameters from samplingLiterals-rounds-beam");
            }
            literals = parameters.get(0);
            rounds = parameters.get(1);
            samplingBeam = parameters.get(2);
            SamplerConstraintLearner sampler = SamplerConstraintLearner.create(med, 0, bias);
            return sampler.learnConstraints(rounds, samplingBeam,
                    this.maxVariables, literals, this.maxPosLit, this.maxNegLit, this.maxComponents, minSupport, this.useSaturations);
        } else {
            System.out.println("unknown parametrization of constraint learner:\t" + learnerVersion);
        }
        return new Pair<>(Sugar.list(), Sugar.list());
    }

    private Map<Pair<Predicate, Integer>, Type> alwaysOneTypeMap() {
        Type type = new Type(0, Sugar.set());

        return new Map<Pair<Predicate, Integer>, Type>() {

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
            public Type get(Object key) {
                return type;
            }

            @Override
            public Type put(Pair<Predicate, Integer> key, Type value) {
                return type;
            }

            @Override
            public Type remove(Object key) {
                return type;
            }

            @Override
            public void putAll(Map<? extends Pair<Predicate, Integer>, ? extends Type> m) {

            }

            @Override
            public void clear() {

            }

            @Override
            public Set<Pair<Predicate, Integer>> keySet() {
                return Sugar.set(new Pair<>(new Predicate("_dummyType_", -1), -1));
            }

            @Override
            public Collection<Type> values() {
                return Sugar.set(type);
            }

            @Override
            public Set<Entry<Pair<Predicate, Integer>, Type>> entrySet() {
                return null;
            }
        };
    }

    public String getAdaptiveConstraints() {
        return adaptiveConstraintMining;
    }

    public SaturatorProvider<HornClause, StatefullSaturator<HornClause>> getProvider(DatasetInterface med, TimeDog time) {
        return getProviderAndConstraints(med, time, NoneBias.create()).r;
    }

    public Pair<SaturatorProvider<HornClause, StatefullSaturator<HornClause>>, Pair<List<Clause>, List<Clause>>> getProviderAndConstraints(DatasetInterface med, TimeDog time, LanguageBias bias) {
        // don't forget that dataset must be with theta subsumption
        // just an awful hack to make hard copy for paralellization, etc. (if it is query, example or x-based)... make it more readable
        med = med.flatten(Matching.THETA_SUBSUMPTION);

        System.out.println("which extension of saturation use (parallel, adaptive):\t" + ConstraintLearnerProperties.create().getAdaptiveConstraints());
        SaturatorProvider<HornClause, StatefullSaturator<HornClause>> saturatorProvider;
        Pair<List<Clause>, List<Clause>> constraints = new Pair<>();
        if (this.learnerVersion.toLowerCase().equals("parallel")) {
            System.out.println("parallel constraints mining");
            LearningSaturationProvider learningProvider = LearningSaturationProvider.create(med, time, bias);
            saturatorProvider = learningProvider;
            Thread thread = new Thread(learningProvider);
            thread.start();
            // here should be some no rush
            long minNoRush = Math.max(0l, Long.parseLong(System.getProperty("ida.logicStuff.constraints.noRush", "0"))); // in seconds
            try {
                Thread.sleep(minNoRush * 1000); // wait so long
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.out.println("no rush finished for learningSaturationProvider (" + minNoRush + " s)");
        } else {
            System.out.println("default constraints mining");
            constraints = this.learnConstraints(med, bias);
            System.out.println("constraints learned: " + constraints.r.size() + " / " + constraints.s.size() + " \t with setting " + maxLiterals + " : " + maxVariables);
            // this would be without adaptive learning :(
            // saturatorProvider = ConstantSaturationProvider.createFilterSaturator(constraints.s, constraints.r);
            saturatorProvider = ConstantSaturationProvider.create(SaturatorDistributor.create(med, constraints.r, constraints.s));

        }
        return new Pair<>(saturatorProvider, constraints);
    }
}

