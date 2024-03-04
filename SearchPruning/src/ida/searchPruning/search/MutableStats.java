package ida.searchPruning.search;

import ida.utils.Sugar;

import java.util.List;

/**
 * It's thread-safe from now on :)
 * <p>
 * Created by Admin on 09.05.2017.
 */
public class MutableStats {

    private long searchedNodes;
    private long prunedNodes;
    private final List<Long> lengths = Sugar.list();
    private long killedHypothesis;
    private long extendedHypothesis;


    public long getSearchedNodes() {
        return searchedNodes;
    }

    public long getPrunedNodes() {
        return prunedNodes;
    }

    public void nodeExpanded() {
        synchronized (this) {
            this.searchedNodes++;
        }
    }

    public void nodePruned() {
        {
            synchronized (this) {
                this.prunedNodes++;
            }
        }
    }

    public void nodesPruned(int i) {
        synchronized (this) {
            this.prunedNodes += i;
        }
    }

    public List<Long> getLengths() {
        return this.lengths;
    }

    public void addLength(long hypothesesLength) {
        synchronized (this) {
            this.lengths.add(hypothesesLength);
        }
    }

    public void hypothesesExtended() {
        synchronized (this) {
            this.extendedHypothesis++;
        }
    }

    public void hypothesesKilled() {
        synchronized (this) {
            this.killedHypothesis++;
        }
    }

    public long getKilledHypothesis() {
        return killedHypothesis;
    }

    public long getExtendedHypothesis() {
        return extendedHypothesis;
    }

}
