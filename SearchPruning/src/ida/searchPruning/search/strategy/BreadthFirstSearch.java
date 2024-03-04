package ida.searchPruning.search.strategy;

import ida.ilp.logic.HornClause;
import ida.searchPruning.search.MutableStats;
import ida.searchPruning.search.SimpleLearner;
import ida.searchPruning.search.collections.SearchNodeInfo;
import ida.searchPruning.evaluation.BreadthResults;
import logicStuff.learning.languageBias.LanguageBias;
import ida.utils.TimeDog;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.saturation.SaturatorProvider;
import logicStuff.learning.saturation.StatefullSaturator;

import java.util.*;

/**
 * Created by Admin on 03.05.2017.
 */
public class BreadthFirstSearch {

    private final DatasetInterface dataset;
    private final TimeDog time;
    private final int minSupport;
    private final long constraintTime;
    private final int beamSize;
    private final SaturatorProvider<HornClause,StatefullSaturator<HornClause>> saturatorProvider;

    // the constraintTime is really awful appendix, todo remove
    public BreadthFirstSearch(DatasetInterface dataset, SaturatorProvider<HornClause,StatefullSaturator<HornClause>> saturatorProvider, TimeDog timer, int minSupport, long constraintTime, int beamSize) {
        this.dataset = dataset;
        this.time = timer;
        this.minSupport = minSupport;
        this.constraintTime = constraintTime;
        this.saturatorProvider = saturatorProvider;
        this.beamSize = beamSize;
    }


    public BreadthResults search(int maxDepth, long searchStart, LanguageBias bias) {
        MutableStats stats = new MutableStats();
        SimpleLearner ruleLearner = new SimpleLearner(dataset,
                saturatorProvider,
                CoverageFactory.getInstance().get(dataset.size()),
                null,
                time,
                stats,
                minSupport);
        BreadthResults all = ruleLearner.breadthFirstSearch(maxDepth, searchStart, constraintTime, bias, beamSize);

        /*System.out.println("what is this?");
        for (int idx = 0; idx < all.depths(); idx++) {
            Set<SearchNodeInfo> features = all.getRules(idx);
            all.setReducedFeatures(idx, featureExtraction(features));
        }*/
        return all;
    }

    private Set<SearchNodeInfo> featureExtraction(Set<SearchNodeInfo> all) {
        Set<SearchNodeInfo> filtered = new HashSet<>();
        List<SearchNodeInfo> sorted = new ArrayList<>(all);
        Collections.sort(sorted, (o1, o2) -> Integer.compare(o1.getRule().countLiterals(), o2.getRule().countLiterals()));
        Set<Set<Integer>> memory = new HashSet<>();
        for (SearchNodeInfo info : sorted) {
            Set<Integer> covered = info.getCovered().asSet();
            if (!memory.contains(covered)) {
                memory.add(covered);
                filtered.add(info);
            }
        }
        return filtered;
    }

}
