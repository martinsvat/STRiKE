package logicStuff.learning.saturation;

import ida.ilp.logic.HornClause;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import logicStuff.learning.languageBias.LanguageBias;
import logicStuff.learning.languageBias.NoneBias;
import logicStuff.learning.constraints.shortConstraintLearner.SamplerConstraintLearner;
import logicStuff.learning.datasets.DatasetInterface;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 1. 2. 2018.
 */
public class LearningSaturationProvider implements SaturatorProvider<HornClause, StatefullSaturator<HornClause>>, Runnable {

    private final Set<IsoClauseWrapper> hardRules;
    private final Set<IsoClauseWrapper> minSuppRules;
    private final DatasetInterface med;
    private final Object lock = new Object();
    private final TimeDog timeDog;
    private final int maxArity;
    private final boolean verbose = true;
    private final LanguageBias bias;

    public LearningSaturationProvider(DatasetInterface dataset, TimeDog timeDog, LanguageBias bias) {
        this.hardRules = ConcurrentHashMap.newKeySet();
        this.minSuppRules = ConcurrentHashMap.newKeySet();
        this.med = dataset;
        this.timeDog = timeDog;
        this.maxArity = med.allPredicates().stream().mapToInt(p -> p.s).max().orElse(0);
        this.bias = bias;
    }


    @Override
    public StatefullSaturator<HornClause> getSaturator() {
        synchronized (lock) {
            // there may occur some problems with subsumption engine, in such a case try to create a brand new copy of clauses to avoid the problem caused by parallelization
            return ConstantSaturationProvider.createFilterSaturator(
                    hardRules.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()),
                    minSuppRules.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList())
            ).getSaturator();
        }
    }

    @Override
    public boolean isUpdatable() {
        return true;
    }

    @Override
    public void run() {
        System.out.println("watch out here, hardcoded values for learning in steps");
        int step = 3;
        int max = Integer.parseInt(System.getProperty("ida.searchPruning.maxDepth", "14"));
        for (int maxDepth = 3; maxDepth < max; maxDepth += step) {
            System.out.println("LearningSaturationProvider:\tstarting run to max depth\t" + maxDepth);
            mineTheory(maxDepth);
        }
    }

    private void mineTheory(int maxDepth) {
        SamplerConstraintLearner learner = SamplerConstraintLearner.create(med, 0, bias);
        learner.setVerbose(false);
        learner.learnConstraints(5, 100,
                maxDepth * this.maxArity, maxDepth,
                1, Integer.MAX_VALUE, 1,
                Sugar.setFromCollections(hardRules), Sugar.setFromCollections(minSuppRules),
                Integer.parseInt(System.getProperty("ida.logicStuff.constraints.minSupport", "1")),
                timeDog, this::flush, Boolean.parseBoolean(System.getProperty("ida.logicStuff.constraints.useSaturations", "true")));
    }

    /**
     * by calling this method you add hard rules and minSuppRules to the actually learned constraints
     *
     * @param newHardRules
     * @param newMinSuppRules
     * @return
     */
    private void flush(Set<IsoClauseWrapper> newHardRules, Set<IsoClauseWrapper> newMinSuppRules) {
        synchronized (hardRules) {
            synchronized (minSuppRules) {
                this.hardRules.addAll(newHardRules);
                this.minSuppRules.addAll(newMinSuppRules);
                System.out.println("LearningSaturationProvider: \tinserting new rules, sizes:\t" + this.hardRules.size() + "\t" + this.minSuppRules.size());
            }
        }
    }

    public static LearningSaturationProvider create(DatasetInterface dataset, TimeDog timeDog) {
        return create(dataset, timeDog, NoneBias.create());
    }

    public static LearningSaturationProvider create(DatasetInterface dataset, TimeDog timeDog, LanguageBias bias) {
        return new LearningSaturationProvider(dataset, timeDog, bias);
    }

}
