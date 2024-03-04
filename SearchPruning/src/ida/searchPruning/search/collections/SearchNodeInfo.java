package ida.searchPruning.search.collections;

import ida.ilp.logic.Clause;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.SpecialVarargPredicates;
import ida.utils.Sugar;
import logicStuff.learning.datasets.Coverage;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * Created by Admin on 03.05.2017.
 */
public class SearchNodeInfo implements Comparable {

    // non-final just because of optimisations
    private Coverage positiveCoveredExamples;
    private Coverage negativeCoveredExamples;
    private Coverage covered;
    private double accuracy;
    private final HornClause rule;
    private final HornClause saturatedRule;
    private final IsoClauseWrapper ruleICW;
    private final IsoClauseWrapper saturatedICW;
    private final Literal newLiteral;
    private boolean isAllowed;
    private List<Long> lengths;
    private final SearchNodeInfo parent;
    private final Set<Literal> forbidden;

    public static boolean saturationSpaceExploration = Boolean.parseBoolean(System.getProperty("ida.searchPruning.search.collections.SearchNodeInfo.saturationSpaceExploration", "true"));

    public SearchNodeInfo(HornClause rule, IsoClauseWrapper ruleICW, HornClause saturated, IsoClauseWrapper saturatedICW, SearchNodeInfo parent, Literal newLiteral, Coverage positiveCoveredExamples, Coverage negativeCoveredExamples) {
        this.parent = parent;
        this.rule = rule;
        this.ruleICW = ruleICW;
        this.saturatedRule = saturated;
        this.saturatedICW = saturatedICW;
        this.newLiteral = newLiteral;
        this.positiveCoveredExamples = positiveCoveredExamples;
        this.negativeCoveredExamples = negativeCoveredExamples;
        this.forbidden = ConcurrentHashMap.newKeySet();
    }

    public boolean isAllowed() {
        return isAllowed;
    }

    public Coverage getPositiveCoveredExamples() {
        return positiveCoveredExamples;
    }

    public Coverage getNegativeCoveredExamples() {
        return negativeCoveredExamples;
    }


    public Literal getNewLiteral() {
        return newLiteral;
    }

    public double getAccuracy() {
        return accuracy;
    }

    /**
     * Returns set of indices of covered examples (union of posCovered and negCovered).
     *
     * @return
     */
    public Coverage getCovered() {
        if (null == this.covered) {
            covered = negativeCoveredExamples.addAll(positiveCoveredExamples);
        }
        return covered;
    }

    public void setCoverages(Coverage pos, Coverage neg) {
        this.positiveCoveredExamples = pos;
        this.negativeCoveredExamples = neg;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public void setAllowability(boolean allowability) {
        this.isAllowed = allowability;
    }

    /**
     * Returns set of indices it was tested on, i.e. set of indices its parent matches; (the learnFrom of its first parent).
     *
     * @return
     */
    public Coverage getParentsCoverage() {
        return this.parent.getCovered();
    }

    public HornClause getRule() {
        return rule;
    }

    public Clause saturatedBody() {
        return this.saturatedRule.body();
    }

    public HornClause getSaturatedRule() {
        return saturatedRule;
    }


    public IsoClauseWrapper getRuleICW() {
        return ruleICW;
    }

    public IsoClauseWrapper getSaturatedICW() {
        return saturatedICW;
    }

    // returns complete ICW of the rule, because the others representations are lacking the head // this is outdated since other representations posses head as well
    /*public IsoClauseWrapper getCompleteRule() {
        return IsoClauseWrapper.create(this.rule.toClause());
    }*/


    @Override
    public int hashCode() {
        return saturatedICW.hashCode();
    }

    public int getNumberOfCovered() {
        return null == this.covered ? positiveCoveredExamples.size() + negativeCoveredExamples.size() : getCovered().size();
    }

    // ok, tohle nevim k cemu je
    public List<Long> getLengths() {
        return lengths;
    }

    public void setLengths(List<Long> lengths) {
        this.lengths = lengths;
    }

    /*
    / * *
     * Updates subsumed examples by this clause. Given the search node, it updates this search node to contain only intersection of subsumed examples by these two search nodes; as each of the search node in fact represents a parent of the clause.
     *
     * @param t
     * @param <T>
     * /
    public <T extends SearchNodeInfo> void update(T t) {
        // System.out.println("this will probably never work, since it is a set of examples subsumed by parent"); // would be applicable in some other search strategies that we do not use (e.g. best successor search)
        // should not it take get covered instead???
        // yes, here should be learnFrom, since it is covered of parents
        // this is deprecated now; it should speed-up the search process in case of heuristic/DFS-like approach, but has none effect for conjunctive queries BFS, thus it is switched of by hardcoding
        /*if (false) {
            this.learnFrom = learnFrom.removeAll(t.getLearnFrom());
        }* /
    }*/


    public void addToForbidden(Literal literal) {
        this.forbidden.add(literal);
    }

    public void addToForbidden(Collection<Literal> literals) {
        this.forbidden.addAll(literals);
    }

    public boolean isForbidden(Literal literal) {
        if (null != this.forbidden && this.forbidden.contains(literal)) {
            return true;
        }
        return null != this.parent && this.parent.isForbidden(literal);
    }

    public boolean isRedundant(Literal literal) {
        return saturationSpaceExploration ? saturatedRule.body().containsLiteral(literal) : rule.body().containsLiteral(literal);
    }


    @Override
    public int compareTo(Object o) {
        SearchNodeInfo o2 = (SearchNodeInfo) o;
        int allowedComparison = Boolean.compare(isAllowed, o2.isAllowed());
        if (0 == allowedComparison) {
            return -Double.compare(getAccuracy(), o2.getAccuracy());
        }
        return -allowedComparison;
    }

    @Override
    public String toString() {
        return "searchWrapper{" +
                "acc=" + accuracy +
                ", allowed=" + isAllowed +
                ", #pos=" + (null == positiveCoveredExamples ? "null" : positiveCoveredExamples.size()) +
                ", #neg=" + (null == negativeCoveredExamples ? "null" : negativeCoveredExamples.size()) +
                ", rule=" + rule +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SearchNodeInfo that = (SearchNodeInfo) o;

        return saturatedICW.equals(that.saturatedICW);
    }


    public static SearchNodeInfo create(HornClause rule, IsoClauseWrapper ruleIcw, HornClause saturated, SearchNodeInfo parent, Literal newLiteral) {
        return create(rule, ruleIcw, saturated, rule.body().countLiterals() != saturated.body().countLiterals() ? IsoClauseWrapper.create(saturated.body()) : ruleIcw, parent, newLiteral);
    }

    public static SearchNodeInfo create(HornClause rule, IsoClauseWrapper ruleIcw, HornClause saturated, IsoClauseWrapper saturatedIcw, SearchNodeInfo parent, Literal newLiteral) {
        return create(rule, ruleIcw, saturated, saturatedIcw, parent, newLiteral, null, null);
    }

    public static SearchNodeInfo create(HornClause rule, IsoClauseWrapper ruleIcw, HornClause saturated, IsoClauseWrapper saturatedIcw, SearchNodeInfo parent, Literal newLiteral, Coverage positiveCoveredExamples, Coverage negativeCoveredExamples) {
        return new SearchNodeInfo(rule, ruleIcw, saturated, saturatedIcw, parent, newLiteral, positiveCoveredExamples, negativeCoveredExamples);
    }

    public static SearchNodeInfo create(HornClause rule, SearchNodeInfo parent, Coverage positiveCoveredExamples, Coverage negativeCoveredExamples) {
        IsoClauseWrapper icw = IsoClauseWrapper.create(rule.toExistentiallyQuantifiedConjunction());
        return create(rule, icw, rule, icw, parent, null, positiveCoveredExamples, negativeCoveredExamples);
    }

    public static SearchNodeInfo create(HornClause rule, SearchNodeInfo parent, Coverage pos, Coverage neg, double accuracy, boolean isAllowed) {
        SearchNodeInfo sni = create(rule, parent, pos, neg);
        sni.setAccuracy(accuracy);
        sni.setAllowability(isAllowed);
        return sni;
    }

    public void addAlldiff() {
        Literal diff = new Literal(SpecialVarargPredicates.ALLDIFF, false, Sugar.listFromCollections(this.rule.body().variables()));
        List<Literal> foundDiffs = this.rule.body().literals().stream().filter(l -> l.predicate().equals(SpecialVarargPredicates.ALLDIFF)).collect(Collectors.toList());
        for (Literal foundDiff : foundDiffs) {
            this.rule.body().removeLiteral(foundDiff);
            this.rule.body().removeLiteral(foundDiff);
        }
        this.rule.body().addLiteral(diff);
        this.saturatedRule.body().addLiteral(diff);

    }
}
