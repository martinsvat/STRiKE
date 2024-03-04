package logicStuff.learning.saturation;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.utils.VectorUtils;
import ida.utils.collections.Counters;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import logicStuff.learning.constraints.shortConstraintLearner.ConstraintLearnerProperties;
import logicStuff.learning.constraints.shortConstraintLearner.SamplerConstraintLearner;
import logicStuff.learning.constraints.shortConstraintLearner.UltraShortConstraintLearnerFaster;
import logicStuff.learning.languageBias.NoneBias;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Stateful saturator distributor which combines saturation given by initial theory and sub theories which are learned during query patter miners.
 * <p>
 * Awful implementation. Instead of that nonsense with update, select, etc., there should be a list of update/select/... with lambda functions for example.
 * <p>
 * <p>
 * Created by martin.svatos on 20. 1. 2018.
 */
public class SaturatorDistributor implements StatefullSaturator<HornClause> {

    /**
     * currently, memory_dep is implemented only, nothing else
     * <p>
     * most of the implementation tries to use laziness as much as possible
     */

    public static final int NONE = -1;
    public static final int UPDATE_EVER_LAYER = 0;
    public static final int UPDATE_ODD = 1;
    public static final int UPDATE_TO = 2;
    public static final int UPDATE_FROM = 3;
    public static final int UPDATE_ODD_FROM = 4;
    public static final int SELECT_EVERY = 0;
    public static final int SELECT_MEDIAN = 1;
    public static final int SELECT_TOPX = 2;
    public static final int LEARNER_COMPLETE = 0;
    public static final int LEARNER_SAMPLING = 1;
    public static final int MEMORY_LOW = 0;
    public static final int MEMORY_DEP = 1;


    private final Set<IsoClauseWrapper> minSuppConstraints;
    private final Set<IsoClauseWrapper> constraints;
    private final int selectMode;
    private final int updateThreshold;
    private final int updateMode;
    private final int computationMode;
    private final DatasetInterface dataset;
    // all pair of constraints/saturators in this class should be of tuple/ordere (minsupp,domainTheoryForSaturation)
    // pair filter minsupp constraints and saturation constraints
    private final Map<Coverage, Pair<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>>> constraintsCache;
    private final Map<Coverage, Pair<? extends Saturator<HornClause>, ? extends Saturator<HornClause>>> saturatorsCache;
    private final Pair<RuleSaturator, RuleSaturator> baseFilterSaturator;
    private final Pair<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> basePair;
    private final Coverage allExamples;
    private final int learnerMode;
    private final int memoryMode;
    private final int topX;
    private int depth;
    private Counters<Coverage> nextLayer;
    private Counters<Coverage> previousLayer;
    private Set<Coverage> compute;
    private MultiMap<Coverage, Coverage> childParentDependency;
    private TimeDog time;


    public SaturatorDistributor(DatasetInterface dataset, Collection<Clause> constraints, Collection<Clause> minSuppConstraints, int computationMode, int updateMode, int updateThreshold, int selectMode, int learnerMode, int memoryMode, int topX) {
        if (MEMORY_DEP != memoryMode && NONE != computationMode) {
            System.out.println("this memory dep and computation mode is rather appendix, not implemented yet");
            throw new IllegalStateException(); //NotImplementedException();
        }
        this.dataset = dataset;
        this.computationMode = computationMode;
        this.updateMode = updateMode;
        this.selectMode = selectMode;
        this.learnerMode = learnerMode;
        this.memoryMode = memoryMode;
        Set<IsoClauseWrapper> msc = minSuppConstraints.stream().map(IsoClauseWrapper::create).collect(Collectors.toSet());
        Set<IsoClauseWrapper> con = constraints.stream().map(IsoClauseWrapper::create).collect(Collectors.toSet());
        this.basePair = new Pair<>(msc, con);
        this.constraints = con;
        this.minSuppConstraints = msc;
        this.constraintsCache = new HashMap<>();
        Coverage allExamples = CoverageFactory.getInstance().get(dataset.size());
        this.allExamples = allExamples;
        this.constraintsCache.put(allExamples, new Pair<>(msc, con));
        System.out.println("parametrize here whether to use conjuncture od dijunction input ;)");
        this.baseFilterSaturator = new Pair<>(RuleSaturator.create(msc), RuleSaturator.create(con));
        this.saturatorsCache = new HashMap<>();
        this.depth = 0;
        this.nextLayer = new Counters<>();
        this.previousLayer = new Counters<>();
        this.childParentDependency = new MultiMap<>();
        this.updateThreshold = updateThreshold;
        this.topX = topX;
    }


    /**
     * by using this method the distributor stores the information to sub-dataset constraint learning
     * <p>
     * be careful, this method is not thread-safe and for the right funcionality it is important to use the same parentsCovering as used in saturate(horn,parentsCovering) method
     *
     * @param horn
     * @param coveredExamples
     */
    public void update(HornClause horn, Coverage coveredExamples, Coverage parentsCovering) {
        if (NONE == computationMode) {
            return;
        }
        nextLayer.increment(coveredExamples);

        if (!constraintsCache.containsKey(parentsCovering)) {
            // just a quick hack in case someone would used different parentsCovering
            constraintsCache.put(parentsCovering, basePair);
            System.out.println("hc\t" + horn.toClause() + "\t" + horn);
            System.out.println("pc\t" + parentsCovering.hashCode() + "\t" + parentsCovering.toString());
            System.out.println("pc\t" + coveredExamples.hashCode() + "\t" + coveredExamples.toString());
            throw new IllegalStateException();
        }

        childParentDependency.put(coveredExamples, parentsCovering);
    }

    private Pair<? extends Saturator<HornClause>, ? extends Saturator<HornClause>> findFilterSaturator(HornClause horn, Coverage parentsCoverage) {
        if (NONE == computationMode
                || (UPDATE_FROM == updateMode && depth < updateThreshold)
                || (UPDATE_ODD_FROM == updateMode && depth < updateThreshold)
                || (UPDATE_TO == updateMode && depth > updateThreshold)) {
            return baseFilterSaturator;
        }

        if (null != compute && compute.contains(parentsCoverage)) {
            saturatorsCache.put(parentsCoverage, createSaturators(parentsCoverage));
            compute.remove(parentsCoverage);
            return saturatorsCache.get(parentsCoverage);
        }

        // here should be some kind of remembering what to and what not to GC -- move it to update method

        // here find saturator given the parents coverage and the dependency tree built in update
        // also, another laziness
        if (constraintsCache.containsKey(parentsCoverage) && !saturatorsCache.containsKey(parentsCoverage)) {
            Pair<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> pair = constraintsCache.get(parentsCoverage);
            System.out.println("parametrize here whether to use conjuncture od dijunction input ;)");
            saturatorsCache.put(parentsCoverage, new Pair<>(RuleSaturator.create(pair.r), RuleSaturator.create(pair.s)));
//            System.out.println("-----------adding " + pair.r.size() + "\t" + pair.s.size() + "\tfor\t" + parentsCoverage.size() + " " + parentsCoverage.hashCode() + "\t" + saturatorsCache.get(parentsCoverage));
        }
        if (saturatorsCache.containsKey(parentsCoverage)) {
            return saturatorsCache.get(parentsCoverage);
        }
        // defensive programming, in any case it returns at least the base saturator
        return baseFilterSaturator;
    }


    // also stores the learned theory into the memory
    private Pair<? extends Saturator<HornClause>, ? extends Saturator<HornClause>> createSaturators(Coverage parentsCoverage) {
        if (NONE == computationMode) { // defensive programming
            return baseFilterSaturator;
        }

        Set<IsoClauseWrapper> initialConstraints = Sugar.setFromCollections(constraints);
        Set<IsoClauseWrapper> initialMinSuppConstraints = Sugar.setFromCollections(minSuppConstraints);

        // here used the dependency tree build in update, it is enough to remember last two layers ;)
        // smt like...
        if (constraintsCache.containsKey(parentsCoverage)) {
            Pair<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> pair = constraintsCache.get(parentsCoverage);
            initialMinSuppConstraints.addAll(pair.r);
            initialConstraints.addAll(pair.s);
        }

        // this should be also generalized (if one dataset instance wont be used in parallel, then it is ok to have only one dataset and starts the constraints learning only on a subset of examples (use Coverage for parent's (emptyclause) coverage)
        DatasetInterface subdataset = dataset.subDataset(parentsCoverage);

        System.out.println("*****\nlearning on " + subdataset.size() + "\t" + previousLayer.get(parentsCoverage));

        // memory_low
        // TODO this is hardcoded for our molecular 2-arity dataset, change it to some nic property as maxLiterals * dataset.maxArity
        // TODO add parametrize depth
        int maxLiterals = depth + 2;
        int maxVariabes = maxLiterals * 2;
        // saturator theory and minsupp theory for filtering
        Pair<List<Clause>, List<Clause>> learned = new Pair<>(Sugar.list(), Sugar.list());
        int minSupport = Integer.parseInt(System.getProperty("ida.logicStuff.constraints.minSupport", "1"));

        // why this cannot be delegated to ConstraintsLearnerProperties (or something like that)?
        if (LEARNER_COMPLETE == learnerMode) {
            UltraShortConstraintLearnerFaster learner = new UltraShortConstraintLearnerFaster(subdataset, maxLiterals, maxVariabes, 0, 1,
                    1, Integer.MAX_VALUE, null, NoneBias.create());
            learned = learner.learnConstraints(true, false, false, Integer.MAX_VALUE, Sugar.list(), false, UltraShortConstraintLearnerFaster.ONLY_NEGATIVE_SATURATIONS,
                    false,
                    initialConstraints,
                    initialMinSuppConstraints,
                    minSupport, time);
        } else if (LEARNER_SAMPLING == learnerMode) {
            // hardcoded values of rounds, sampled beams, etc...
            SamplerConstraintLearner learner = new SamplerConstraintLearner(subdataset, 0, NoneBias.create());
            learner.setVerbose(false);
            learned = learner.learnConstraints(4, 75, maxVariabes, maxLiterals, 1, Integer.MAX_VALUE, 1,
                    initialConstraints,
                    initialMinSuppConstraints,
                    minSupport, time, Boolean.parseBoolean(System.getProperty("ida.logicStuff.constraints.useSaturations", "true")));
        }

        // storing because of memory_dep
        // the pair is filter minsupp theory and saturate theory (quite the opposite what learnConstraints returns)
        constraintsCache.put(parentsCoverage, new Pair<>(learned.s.stream().map(IsoClauseWrapper::create).collect(Collectors.toSet()),
                learned.r.stream().map(IsoClauseWrapper::create).collect(Collectors.toSet())));
//        System.out.println("****addds to consc\t" + parentsCoverage.size() + " " + parentsCoverage.hashCode() + "\t" + constraintsCache.get(parentsCoverage));

        System.out.println("diffs\t" + learned.r.size() + "-" + learned.s.size() + "\t\t" + this.constraints.size() + "-" + this.minSuppConstraints.size());
        return new Pair<>(RuleSaturator.create(learned.s), RuleSaturator.create(learned.r));
    }

    /**
     * tell the distributor that you have finished searching within the layer
     */
    public void nextDepth(TimeDog time) {
        this.time = time;
        System.out.println("************* nextDepth ************************");
        if (NONE == computationMode) { // the none mode could be implemented by another class with no other of functionality
            return;
        }

        Set<Coverage> compute = Sugar.set();
        if (SELECT_EVERY == this.selectMode) {
            compute = nextLayer.keySet();
            System.out.println("compute for\t" + compute.size());
        } else if (SELECT_MEDIAN == this.selectMode) {
            int median = VectorUtils.median(nextLayer.counts().stream().mapToInt(i -> i).toArray());
            Predicate<Coverage> takeHigher = (key) -> {
                if (key.size() > 1 && nextLayer.get(key) > 1 && nextLayer.get(key) > median) {
                    return true;
                }
                return false;
            };
            compute = nextLayer.keySet().stream().filter(key -> takeHigher.test(key)).collect(Collectors.toSet());
            //hold = nextLayer.keySet().stream().filter(key -> !takeHigher.test(key)).collect(Collectors.toSet());
            System.out.println("compute for\t" + compute.size() + "\thold for\t" + (nextLayer.keySet().size() - compute.size()));
        } else if (SELECT_TOPX == this.selectMode) {
            List<Map.Entry<Coverage, Integer>> sorted = nextLayer.toMap().entrySet().stream().sorted((e1, e2) -> {
                        if (e1.getValue() == e2.getValue()) {
                            return -Integer.compare(e1.getKey().size(), e2.getKey().size());
                        }
                        return -Integer.compare(e1.getValue(), e2.getValue());
                    }
            ).collect(Collectors.toList());
            Set<Coverage> finalCompute = compute;
            IntStream.range(0, Math.min(this.topX, sorted.size()))
                    .filter(idx -> sorted.get(idx).getValue() > 1 && sorted.get(idx).getKey().size() > 1)
                    .forEach(idx -> {
                        System.out.println("\t" + sorted.get(idx).getKey().size() + "\t" + sorted.get(idx).getValue());
                        finalCompute.add(sorted.get(idx).getKey());
                    });
        }

        // usual way
        //this.compute = compute;
        // hack for updateOddFrom
        this.compute = ((UPDATE_ODD_FROM == updateMode && depth % 2 == 0) || UPDATE_ODD_FROM != updateMode) ? compute : Sugar.set();
        this.previousLayer = nextLayer;
        this.nextLayer = new Counters<>();

        System.out.println("compute size is\t" + compute.size() + "");
        compute.forEach(s -> System.out.println("\t" + s.size() + " #" + s.hashCode()));

        // memory_dep
        // here might be some kind of GC
        childParentDependency.entrySet().forEach(entry -> {
            Coverage childCovering = entry.getKey();
            Set<IsoClauseWrapper> filter = Sugar.set();
            Set<IsoClauseWrapper> theory = Sugar.set();

            if (constraintsCache.containsKey(childCovering)) {
                Pair<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> pair = constraintsCache.get(childCovering);
                filter.addAll(pair.r);
                theory.addAll(pair.s);
            }
            entry.getValue().forEach(parentsCoverage -> {
                if (constraintsCache.containsKey(parentsCoverage)) {
                    Pair<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> pair = constraintsCache.get(parentsCoverage);
                    filter.addAll(pair.r);
                    theory.addAll(pair.s);
                }
            });
            constraintsCache.put(childCovering, new Pair<>(filter, theory));
        });
        childParentDependency = new MultiMap<>();

        System.out.println(" ****************************** going to another layer *****************");
        // TODO add some kind of GC to constraintsCache and saturatorCache

        depth++;
    }

    @Override
    public HornClause saturate(HornClause hornClause) {
        return saturate(hornClause, (l) -> false);
    }

    @Override
    public HornClause saturate(HornClause hornClause, Predicate<Literal> forbidden) {
        return saturate(hornClause, allExamples, forbidden);
    }

    public HornClause saturate(HornClause hornClause, Coverage parentsCoverage) {
        return saturate(hornClause, parentsCoverage, (l) -> false);
    }

    @Override
    public HornClause saturate(HornClause hornClause, Coverage parentsCoverages, Predicate<Literal> forbidden) {
        Pair<? extends Saturator<HornClause>, ? extends Saturator<HornClause>> filterSaturator = findFilterSaturator(hornClause, parentsCoverages);
        if (filterSaturator.r.isPruned(hornClause)) {
            return null;
        }
        return filterSaturator.s.saturate(hornClause, forbidden);
    }

    @Override
    public boolean isPruned(HornClause hornClause) {
        return isPruned(hornClause, allExamples);
    }

    @Override
    public boolean isPruned(HornClause hornClause, Coverage examples) {
        Pair<? extends Saturator<HornClause>, ? extends Saturator<HornClause>> filterSaturator = findFilterSaturator(hornClause, examples);
        return filterSaturator.r.isPruned(hornClause) || filterSaturator.s.isPruned(hornClause);
    }

    @Override
    public Collection<Clause> getTheory() {
        throw new IllegalStateException(); //NotImplementedException();
    }

    public static SaturatorDistributor create(DatasetInterface dataset, Collection<Clause> constraints, Collection<Clause> minSuppConstraints, int computationMode, int updateMode, int updateThreshold, int selectMode, int learner, int memory, int topX) {
        // big TODO this won't work if the dataset is composed BK and a list of head/query atoms
        return new SaturatorDistributor(dataset.deepCopy(Matching.THETA_SUBSUMPTION),
                constraints, minSuppConstraints,
                computationMode, updateMode, updateThreshold, selectMode,
                learner, memory, topX);
    }

    public static SaturatorDistributor create(DatasetInterface dataset, Collection<Clause> constraints, Collection<Clause> minSuppConstraints, String setting) {
        int computationMode = NONE;
        int updateMode = NONE;
        int selectMode = NONE;
        int learner = NONE;
        int memory = NONE;
        int topX = 0;
        int updateThreshold = 0;
        if (setting.toLowerCase().equals("parallel")) {
            throw new IllegalStateException("You cannot create parallel version of saturatorDistributor by calling this setting here.");
        }
        if (!setting.toLowerCase().equals("none")) {
            String[] splitted = setting.toLowerCase().split("-");
            try {
                if (splitted[0].equals("updateeverylayer")) {
                    updateMode = UPDATE_EVER_LAYER;
                } else if (splitted[0].startsWith("updateoddfrom")) {
                    updateMode = UPDATE_ODD_FROM;
                    updateThreshold = Integer.parseInt(splitted[0].substring("updateOddFrom".length()));
                } else if (splitted[0].equals("updateodd")) {
                    updateMode = UPDATE_ODD;
                } else if (splitted[0].startsWith("updatefrom")) {
                    updateMode = UPDATE_FROM;
                    updateThreshold = Integer.parseInt(splitted[0].substring("updateFrom".length()));
                } else if (splitted[0].startsWith("updateto")) {
                    updateMode = UPDATE_TO;
                    updateThreshold = Integer.parseInt(splitted[0].substring("updateTo".length()));
                } else {
                    System.out.println("unknown value for updateMode in SaturatorDistributor");
                }

                switch (splitted[1]) {
                    case "selectevery":
                        selectMode = SELECT_EVERY;
                        break;
                    case "selectmedian":
                        selectMode = SELECT_MEDIAN;
                        break;
                    default:
                        if (splitted[1].toLowerCase().startsWith("selecttop")) {
                            selectMode = SELECT_TOPX;
                            topX = Integer.parseInt(splitted[1].substring("selecttop".length()));
                            break;
                        }
                        System.out.println("unknown value for selectMode in SaturatorDistributor");
                }

                switch (splitted[2]) {
                    case "complete":
                        learner = LEARNER_COMPLETE;
                        break;
                    case "sampling":
                        learner = LEARNER_SAMPLING;
                        break;
                    default:
                        System.out.println("unknown value for learner in SaturatorDistributor");
                }

                switch (splitted[3]) {
                    case "low":
                        memory = MEMORY_LOW;
                        break;
                    case "dep":
                        memory = MEMORY_DEP;
                        break;
                    default:
                        System.out.println("unknown value for learner in SaturatorDistributor");
                }

                if (NONE == updateMode || NONE == selectMode || NONE == learner || NONE == memory) {
                    System.out.println("some of the update, select, learner or memory mode in SaturatorDistributor none, thus whole saturator is set to do nothing");
                    System.out.println(updateMode + " " + selectMode + " " + learner + " " + memory);
                } else {
                    computationMode = 0;
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw new IllegalStateException("Cannot parse setting for SaturatorDistributor.");
            }
        }
        return create(dataset, constraints, minSuppConstraints, computationMode, updateMode, updateThreshold, selectMode, learner, memory, topX);
    }

    public static SaturatorDistributor create(DatasetInterface dataset, Collection<Clause> constraints, Collection<Clause> minSuppConstraints) {
        return create(dataset, constraints, minSuppConstraints, ConstraintLearnerProperties.create().getAdaptiveConstraints());
    }

}
