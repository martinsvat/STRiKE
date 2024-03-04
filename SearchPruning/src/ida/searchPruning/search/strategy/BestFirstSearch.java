package ida.searchPruning.search.strategy;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.searchPruning.search.GeneralSearch;
import ida.searchPruning.search.SearchExpander;
import ida.searchPruning.search.SimpleLearner;
import ida.searchPruning.search.criterion.Criterion;
import ida.searchPruning.evaluation.SearchStatsWrapper;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.learning.saturation.ConstantSaturationProvider;


import java.util.List;

import static ida.utils.Sugar.list;

/**
 * Created by Admin on 03.05.2017.
 */
public class BestFirstSearch {

    private final MEDataset dataset;
    private final Criterion criterion;
    private final List<Clause> constraints;
    private final TimeDog overallTime;
    private final TimeDog ruleLearningTime;
    private final int minSupport;
    private final List<Clause> minSuppConstraints;

    public BestFirstSearch(MEDataset dataset, Criterion criterion, List<Clause> constraints, TimeDog overallTime, TimeDog ruleLearningTime, int minSupport) {
        this(dataset, criterion, constraints, overallTime, ruleLearningTime, minSupport, Sugar.list());
    }

    public BestFirstSearch(MEDataset dataset, Criterion criterion, List<Clause> constraints, TimeDog overallTime, TimeDog ruleLearningTime, int minSupport, List<Clause> minSuppConstraints) {
        this.dataset = dataset;
        this.criterion = criterion;
        this.constraints = constraints;
        this.overallTime = overallTime;
        this.ruleLearningTime = ruleLearningTime;
        this.minSupport = minSupport;
        this.minSuppConstraints = minSuppConstraints;

        System.out.println("change the awfull constraints and minSuppConstraints to saturationProvider");
        throw new IllegalStateException();// NotImplementedException();
    }


    public SearchStatsWrapper<List<HornClause>> search(long maxNodes) {
        SearchExpander expander = (learnFrom, timeDog, stats) -> {
            SimpleLearner ruleLearner = new SimpleLearner(dataset,
                    ConstantSaturationProvider.createFilterSaturator(constraints, minSuppConstraints),
                    CoverageFactory.getInstance().get(learnFrom),
                    criterion,
                    timeDog,
                    stats,
                    minSupport);
            return ruleLearner.bestFirstSearch(maxNodes, ruleLearningTime.fromNow());
        };
        GeneralSearch general = new GeneralSearch(dataset, expander, overallTime.fromNow());
        return general.search();
    }

}
