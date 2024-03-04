package ida.searchPruning.search;


import ida.searchPruning.search.collections.SearchNodeInfo;
import ida.utils.TimeDog;
import logicStuff.learning.datasets.Coverage;

/**
 * Created by Admin on 03.05.2017.
 */
@FunctionalInterface
public interface SearchExpander {

    public SearchNodeInfo learnRule(Coverage learnFrom, TimeDog time, MutableStats stats);

}
