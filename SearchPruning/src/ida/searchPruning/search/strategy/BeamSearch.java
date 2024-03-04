package ida.searchPruning.search.strategy;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.searchPruning.search.GeneralSearch;
import ida.searchPruning.search.SearchExpander;
import ida.searchPruning.search.SimpleLearner;
import ida.searchPruning.search.collections.SearchNodeInfo;
import ida.searchPruning.search.criterion.Criterion;
import ida.searchPruning.evaluation.SearchStatsWrapper;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.saturation.ConstantSaturationProvider;
//import org.apache.commons.lang3.NotImplementedException;

import java.util.List;

/**
 * Created by Admin on 03.05.2017.
 */
public class BeamSearch {

    private final DatasetInterface dataset;
    private final List<Clause> constraints;
    private final Criterion criterion;
    private final TimeDog overallTime;
    private final TimeDog ruleLearningTime;
    private final int minSupport;
    private final List<Clause> minSuppConstraints;

    public BeamSearch(DatasetInterface dataset, List<Clause> constraints, Criterion criterion, TimeDog overallTime, TimeDog ruleLearningTime, int minSupport) {
        this(dataset, constraints, criterion, overallTime, ruleLearningTime, minSupport, Sugar.list());
    }

    public BeamSearch(DatasetInterface dataset, List<Clause> constraints, Criterion criterion, TimeDog overallTime, TimeDog ruleLearningTime, int minSupport, List<Clause> minSuppConstraints) {
        this.dataset = dataset;
        this.constraints = constraints;
        this.criterion = criterion;
        this.overallTime = overallTime;
        this.ruleLearningTime = ruleLearningTime;
        this.minSupport = minSupport;
        this.minSuppConstraints = minSuppConstraints;

        System.out.println("change the awful constraints and minsupconstraints to saturator provider");
        throw new IllegalStateException();// NotImplementedException();
    }


    public SearchStatsWrapper<List<HornClause>> search(int maxDepth, int beamWidth) {
        SearchExpander expander = (learnFrom, timeDog, stats) -> {
            SimpleLearner ruleLearner = new SimpleLearner(dataset,
                    ConstantSaturationProvider.createFilterSaturator(constraints, minSuppConstraints),
                    CoverageFactory.getInstance().get(learnFrom),
                    criterion,
                    timeDog,
                    stats,
                    minSupport);
            SearchNodeInfo best = ruleLearner.beamSearch(maxDepth, beamWidth, ruleLearningTime.fromNow());
            return best;
        };
        GeneralSearch general = new GeneralSearch(dataset, expander, overallTime.fromNow());
        return general.search();
    }

}
