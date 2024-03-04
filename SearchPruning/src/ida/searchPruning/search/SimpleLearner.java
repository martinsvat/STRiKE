/*
 * Copyright (c) 2015 Ondrej Kuzelka
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ida.searchPruning.search;

import ida.ilp.logic.*;
import ida.ilp.logic.HornClause;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.ilp.logic.subsumption.Matching;
import ida.searchPruning.evaluation.BreadthResults;
import ida.searchPruning.search.criterion.Accuracy;
import ida.searchPruning.search.criterion.Criterion;
import ida.searchPruning.search.collections.*;
import ida.utils.tuples.Quadruple;
import logicStuff.learning.languageBias.LanguageBias;
import ida.utils.Sugar;
import ida.utils.collections.MultiMap;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.Coverage;
import logicStuff.learning.datasets.CoverageFactory;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.datasets.MEDataset;
import logicStuff.learning.modeBias.ModeDeclaration;
import logicStuff.learning.modeBias.MolecularMode;
import logicStuff.learning.modeBias.NoneMode;
import logicStuff.learning.saturation.*;

import ida.utils.TimeDog;
import logicStuff.typing.Type;
import logicStuff.typing.TypesInducer;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Does not handle constants. Constants need to be handled as unary literals. But you're free to implement this functionality :)
 * <p>
 * Created by kuzelkao_cardiff on 20/01/17 and extended by MS.
 */
public class SimpleLearner {

    // TODO why is constraintTime parameter of a search? replace

    private final boolean parallel = 1 < Integer.parseInt(System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism", "1"))
            || Boolean.parseBoolean(System.getProperty("ida.searchPruning.parallel", "false"));


    private final Coverage learnFrom;
    private final TimeDog time;
    private final MutableStats stats;
    private final SaturatorProvider<HornClause, StatefullSaturator<HornClause>> saturatorProvider;
    private final boolean rangeRestricted;


    private final Map<Pair<ida.ilp.logic.Predicate, Integer>, Type> types;
    private final Map<Pair<ida.ilp.logic.Predicate, Integer>, String> simplifiedTyping;
    private final boolean useTyping;
    private final boolean canEvaluateNodesInParallel;

    private boolean connectedOnly = true;

    private final int minSupport;

    DatasetInterface dataset;

    private List<ida.ilp.logic.Predicate> allAllowedPredicates;

    public static boolean VERBOUSE = false;
    private final Criterion criterion;
    private Random random = new Random(13);
    private final ModeDeclaration mode = System.getProperty("ida.searchPruning.modeDeclaration", "").toLowerCase().equals("molecular")
            ? MolecularMode.create() : NoneMode.create();

//    private final boolean hcExtendedRefinement;
//    nastavit v kontruktoru podle parametrizace nastavit v konstrukturu podle teorii,taky se to musi aktualizovat kdyz distributor da jinou teorii :)) nebo proste
//    private boolean oneLiteralHCrefinement;

    private MultiMap<Pair<String, Integer>, String> oneLiteralConstraints;
    private final boolean oneLiteralHCrefinement = true;

    {
        System.out.println("todo: tady udelat lepsi prepinac pro nastaveni compareByAccuracy a randomSelect (uz jenom proto ze je to vylucujici nebo)");
    }

    private final boolean compareByAccuracy = Boolean.parseBoolean(System.getProperty("ida.searchPruning.simpleLearner.compareByAccuracy", "false"));
    private final boolean randomSelect = Boolean.parseBoolean(System.getProperty("ida.searchPruning.simpleLearner.randomSelect", "false"));


    public SimpleLearner(DatasetInterface dataset, SaturatorProvider<HornClause, StatefullSaturator<HornClause>> saturatorProvider, Coverage learnFrom, Criterion criterion, TimeDog time, MutableStats stats, int minSupport) {
        this(dataset, saturatorProvider, learnFrom, criterion, time, stats, minSupport, false, null);
        // hopefully correct workaround
    }

    public SimpleLearner(DatasetInterface dataset, SaturatorProvider<HornClause, StatefullSaturator<HornClause>> saturatorProvider, Coverage learnFrom, Criterion criterion, TimeDog time, MutableStats stats, int minSupport, boolean rangeRestricted, Map<Pair<ida.ilp.logic.Predicate, Integer>, Type> types) {
        this(dataset, saturatorProvider, learnFrom, criterion, time, stats, minSupport, rangeRestricted, types, false);
    }

    public SimpleLearner(DatasetInterface dataset, SaturatorProvider<HornClause, StatefullSaturator<HornClause>> saturatorProvider, Coverage learnFrom, Criterion criterion, TimeDog time, MutableStats stats, int minSupport, boolean rangeRestricted, Map<Pair<ida.ilp.logic.Predicate, Integer>, Type> types, boolean canEvaluateNodesInParallel) {
        this.dataset = dataset;
        this.allAllowedPredicates = dataset.allPredicates().stream().map(ida.ilp.logic.Predicate::create).collect(Collectors.toList());
        this.saturatorProvider = saturatorProvider;
        this.learnFrom = learnFrom;
        this.criterion = criterion;
        this.time = time;
        this.stats = stats;
        this.minSupport = minSupport;
        this.rangeRestricted = rangeRestricted;
        if (this.rangeRestricted) {
            System.out.println("implemented only for breadthFirstSearch");
        }

        this.types = types;
        this.useTyping = null != types && types.keySet().size() > 1;
        this.simplifiedTyping = this.useTyping ? TypesInducer.simplify(types) : null;
        this.canEvaluateNodesInParallel = canEvaluateNodesInParallel;

        // todo -- remake this (MEDataset), this will not work on general!
        /*if (!(dataset instanceof MEDataset)) {
            throw new NotImplementedException();
        }*/
        //this.saturatorDistributor = SaturatorDistributor.create((MEDataset) dataset, constraints, minSuppConstraints);j
    }

    //+1 = positive classifier, -1 = negative classifier, 0 = both
    public SearchNodeInfo bestFirstSearch(long maxNodesSearched, TimeDog ruleLearningTime) {
        if (canEvaluateNodesInParallel) {
            System.out.println("canEvaluateNodesInParallel not implemented for this method (however, it can be done probably quite easilly)");
        }

        CollectionWithMemory<SearchNodeInfo> queue = new CollectionWithMemory<>(new QueueCollection<SearchNodeInfo>());
        Set<IsoClauseWrapper> isoProcessedNonSaturated = new HashSet<>(); // non-saturated ICW

        Pair<Coverage, Coverage> data = selectExample();
        Coverage pos = data.r;
        Coverage neg = data.s;
        HornClause empty = new HornClause(new Clause());
        queue.forcePush(SearchNodeInfo.create(empty, null, pos, neg));

        SearchNodeInfo best = null;
        for (long i = 1; i <= maxNodesSearched && queue.isNotEmpty() && time.enough() && ruleLearningTime.enough(); i++) {
            SearchNodeInfo head = queue.poll();

            if (VERBOUSE) {
                System.out.println("resolving node\t" + i + "\t" + queue.size() + "\tleft time " + time.left());
            }
            if (head.getCovered().size() < 2) {
                continue;
            }

            Collection<SearchNodeInfo> candidates = refinements(head, isoProcessedNonSaturated);

            for (SearchNodeInfo refinement : candidates) {
                SearchNodeInfo evaluated = evaluate(refinement);
                if (evaluated.getNumberOfCovered() < 1) { // here can be evaluated.getPositiveCoveredExamples().size() < 1, etc.
                    continue;
                }
                queue.push(evaluated);
                best = update(best, evaluated);
            }

        }
        best.setLengths(queue.getMemory().stream().map(hypotheses -> new Long(hypotheses.getRule().body().literals().size())).collect(Collectors.toList()));
        return best;
    }

    public SearchNodeInfo beamSearch(int maxDepth, int beamWidth, TimeDog ruleLearningTime) {
        if (canEvaluateNodesInParallel) {
            System.out.println("canEvaluateNodesInParallel not implemented for this method (however, it can be done)");
        }
        System.out.println("todo: beamSearch -- update :))");
        CollectionWithMemory<SearchNodeInfo> queue = new CollectionWithMemory<>(new QueueCollection<>(), beamWidth, true);

        Pair<Coverage, Coverage> data = selectExample();
        Coverage pos = data.r;
        Coverage neg = data.s;
        HornClause empty = new HornClause(new Clause());
        queue.forcePush(SearchNodeInfo.create(empty, null, pos, neg));


        SearchNodeInfo best = null;
        for (int i = 1; i <= maxDepth && queue.isNotEmpty() && time.enough() && ruleLearningTime.enough(); i++) {
            CollectionWithMemory<SearchNodeInfo> next = new CollectionWithMemory<>(new QueueCollection<SearchNodeInfo>());
            next.addMemory(queue.getMemory());
            Set<IsoClauseWrapper> isoProcessedNonSaturated = new HashSet<>(); // non-saturated ICW

            if (VERBOUSE) {
                System.out.println("resolving depth\t" + i + "\t" + queue.size() + "\tleft time " + time.left());
            }
            while (queue.isNotEmpty() && time.enough() && ruleLearningTime.enough()) {
                SearchNodeInfo head = queue.poll();

                if (head.getCovered().size() < 2) {
                    continue;
                }

                Collection<SearchNodeInfo> candidates = refinements(head, isoProcessedNonSaturated);

                for (SearchNodeInfo candidate : candidates) {
                    SearchNodeInfo evaluated = evaluate(candidate);
                    if (evaluated.getNumberOfCovered() < 1) { // evaluated.getPositiveCoveredExamples().size() < 1, etc...
                        continue;
                    }
                    next.push(evaluated);
                    best = update(best, evaluated);
                }
            }
            queue = next;
            queue.checkSize();
        }
        best.setLengths(queue.getMemory().stream()
                .map(hypotheses -> new Long(hypotheses.getRule().body().literals().size()))
                .collect(Collectors.toList()));
        return best;
    }

    private SearchNodeInfo update(SearchNodeInfo best, SearchNodeInfo head) {
        if (null == head) {
            return best;
        }
        if ((null == best || best.getAccuracy() < head.getAccuracy())
                && head.isAllowed()
                && head.getNumberOfCovered() > 0) {
            return head;
        }
        return best;
    }

    private Pair<Coverage, Coverage> selectExample() {
        Coverage pos = dataset.getPosIdxs();
        pos = pos.intersection(learnFrom);
        Coverage neg = dataset.getNegIdxs();
        neg = neg.intersection(learnFrom);
        return new Pair<>(pos, neg);
    }

    public BreadthResults breadthFirstSearch(int maxDepth, long nanoSearchStart, long constraintTime, int beamSize) {
        return breadthFirstSearch(maxDepth, nanoSearchStart, constraintTime, () -> (hornClause) -> true, beamSize);
    }

    // there should be ? extends Results and a class should be given instead of that, incorporate method thereafter should work inside on its own
    public BreadthResults breadthFirstSearch(int maxDepth, long nanoSearchStart, long constraintTime, LanguageBias<HornClause> bias, int beamSize) {
        return breadthFirstSearch(maxDepth, nanoSearchStart, constraintTime, bias, beamSize, initialClause(), this::evaluate, Sugar.set(), null);
    }

    public BreadthResults breadthFirstSearch(int maxDepth, long nanoSearchStart, long constraintTime, LanguageBias<HornClause> bias, int beamSize, HornClause initialHorn, Set<IsoClauseWrapper> subsumptionFilter) {
        return breadthFirstSearch(maxDepth, nanoSearchStart, constraintTime, bias, beamSize, initialHorn, this::evaluate, subsumptionFilter, null);
    }

    // todo co je levelFilter? neni to nejaky apendix? Nedalo by se to nahradit pomoci LB?
    public BreadthResults breadthFirstSearch(int maxDepth, long nanoSearchStart, long constraintTime, LanguageBias<HornClause> bias, int beamSize, HornClause initialHorn, java.util.function.Function<SearchNodeInfo, SearchNodeInfo> evaluationStrategy, Set<IsoClauseWrapper> subsumptionFilter, Predicate<SearchNodeInfo> levelFilter) {
        // it is important to notice that the search uses the initial horn clause as root and thus its head is ommitted in some parts (because it is always the same); therefore, if one would like to use search with more roots, e.g. different horn heads, the implementation has to be changed
        // Predicate<SearchNodeInfo> levelFilter is just another workaround to have control over the search, it filters evaluated noted as returns only those which should be evaluated in the next layer
        // so for example, it can be used to stop evaluating successors of a node of which they inherite some monotonic property, i.e. the same accuracy
        System.out.println("momentalni nastaveni je pro top-k-beam, nutne upravit/parametrizovat pro tezeni featur");

        //java.util.function.Function<SearchNodeInfo, SearchNodeInfo> evaluationStrategy
        // vyhodnoti coverage pravidla a napocita prislusne acc podle ceho se potom rozhoduje prohledavani :)) bud vrati upraveny/novy SNI nebo vrati null (takovy kandidat se zahodi)

        // avoiding tautologies (and contradictions) is done by adding negations of heads to forbidden explicitly (as a bad refinement)


        List<Clause> subsumptionFilterPrepared = subsumptionFilter.stream()
                .map(icw -> canEvaluateNodesInParallel
                        ? icw.getOriginalClause()
                        : LogicUtils.replaceNegationSing(icw.getOriginalClause()))
                .collect(Collectors.toList()); // speed-up hack; if the evaluation is done in sequential, then it we can prepare the clauses before; otherwise we need to have one clause for each of the matching call (for safe)

        System.out.println("co udelat s not range restricted???? pro PAC?");

        CollectionWithMemory<SearchNodeInfo> list = new CollectionWithMemory<>(new ListCollection<SearchNodeInfo>());

        //Set<Matching> subsumptionForbidden = subsumptionFilter.stream().map(icw -> new Matching(Sugar.list(LogicUtils.replaceNegationSing(icw.getOriginalClause())))).collect(Collectors.toSet());

        BreadthResults result = new BreadthResults(nanoSearchStart);

        Pair<Coverage, Coverage> data = selectExample();
        Coverage pos = data.r;
        Coverage neg = data.s;
        SearchNodeInfo initial = SearchNodeInfo.create(initialHorn, null, pos, neg);
        // to be hones, I do not know which polarity to add there; it should be the same as in head, because body is explicitly beeing kept as positive literals
        initial.addToForbidden(initialHorn.head());

        initial.setAccuracy(computeAccuracy(pos, neg, initialHorn));
        initial.setAllowability(computeAllowablitiy(pos, neg, initialHorn));
        list.forcePush(initial);

        Map<Integer, CollectionWithMemory> history = new HashMap<>();
        history.put(0, list);
        List<Integer> levelSizes = Sugar.list();


        // todo -- this should be
        this.oneLiteralConstraints = this.oneLiteralHCrefinement ? prepareOneLiteralConstraints() : new MultiMap<>();

        // hard/minsupp/rules are ad hoc, throw it away
        result.join(0, new HashSet<>(list.getMemory()), stats, constraintTime, 0, 0);//hardRules.size(), minSupportHardRules.size());
        System.out.println("cas\t" + time.enough());
        for (int layer = 1; layer <= maxDepth && list.isNotEmpty() && time.enough(); layer++) {
            //Set<IsoClauseWrapper> isoProcessedNonSaturated = new HashSet<>(); // non-saturated ICW
            Set<IsoClauseWrapper> isoProcessedNonSaturated = ConcurrentHashMap.newKeySet(); // non-saturated ICW
            CollectionWithMemory<SearchNodeInfo> next = new CollectionWithMemory<>(
                    compareByAccuracy ? new QueueCollection<>(Comparator.comparingDouble(SearchNodeInfo::getAccuracy).reversed())
                            : new ListCollection<>());
            //next.addMemory(list.getMemory()); this does not have to be done since we are going through level by level
            if (VERBOUSE) {
                System.out.println("resolving depth\t" + layer + "\t" + list.size() + "\tleft time " + time.left());
            }

            long originalLength = list.size();
            System.out.println("original length\t" + originalLength + "\tat depth\t" + layer);
            while (list.isNotEmpty() && time.enough()) {
                SearchNodeInfo head = list.poll();

                if (VERBOUSE) {
                    System.out.println("resolving\t" + head.getRule() + "\t" + head);
                }

                if (dataset.size() > 1 && head.getNumberOfCovered() < 2) {
                    continue;
                }

                Set<SearchNodeInfo> candidates = refinements(head, isoProcessedNonSaturated);

                /*for (SearchNodeInfo sni : candidates) {
                    if (sni.getRule().toString().contains("location(1:V0, 5:V1)") && sni.getRule().toString().contains("interaction")) {
                        System.out.println("i am here\t" + bias.predicate().test(sni.getRule()));
                    }
                    //bias.predicate().test(sni.getRule())); // horn rule is tested here
                    System.out.println(bias.predicate().test(sni.getRule()) + "\t" + sni.getRule());
                    System.out.println(bias.predicate().test(sni.getRule()));

                }*/

                Stream<SearchNodeInfo> candidateStream = candidates.stream();
                if (this.canEvaluateNodesInParallel) {
                    candidateStream = candidateStream.parallel();
                }
                candidateStream = candidateStream.filter(sni -> bias.predicate().test(sni.getRule()));

                if (rangeRestricted) { // just a quick here patch here, it would be nicer to get it more compact
                    candidateStream = candidateStream.filter(sni -> LogicUtils.isRangeRestricted(sni.getSaturatedRule()));
                }

                if (subsumptionFilterPrepared.size() > 0) {
                    candidateStream = candidateStream
                            .filter(sni -> {
                                Matching m = Matching.create(LogicUtils.replaceNegationSing(sni.getRule().toClause()), Matching.THETA_SUBSUMPTION);
                                return subsumptionFilterPrepared.stream()
                                        .noneMatch(forbidden -> m.subsumption(canEvaluateNodesInParallel ? LogicUtils.replaceNegationSing(forbidden) : forbidden, 0)); // speed-up hack
                            });
                }

                candidateStream = candidateStream.map(sni -> {
                    if (time.enough()) {

                        /*System.out.println(sni.getRule() + "\n\t" +
                                sni.getRuleICW().getOriginalClause() + "\n\t" +
                                sni.getSaturatedICW().getOriginalClause() + "\n\t" +
                                sni.getSaturatedRule()
                        );*/

                        // tak a ta u packy bude prave jedinecna :))
                        stats.nodeExpanded();
                        stats.addLength(sni.getRule().body().countLiterals());
                        SearchNodeInfo evaluated = evaluationStrategy.apply(sni);

                        if (null == evaluated) { // just a speedup hack to narrow the space by forbidding literals that are bad now... there will be bad in the future
                            head.addToForbidden(sni.getNewLiteral());
                        }

                        return evaluated;
                    }
                    return null;
                }).filter(Objects::nonNull);

                if (null != levelFilter) {
                    candidateStream = candidateStream.filter(levelFilter);
                }

                candidateStream.forEach(sni -> {
                    if (sni.getNumberOfCovered() < this.minSupport) { // because of monotonicity
                        head.addToForbidden(sni.getNewLiteral()); // we can add here all the refinements which lead to these isomorphic ones (but, in fact, we do not have these anywhere)
                    } else {
                        next.push(sni);
                    }
                });
            }

            System.out.println("after (termination) length\t" + list.size() + "\tmeaning that " + ((originalLength - 1.0 * list.size()) / originalLength) + "% was completed\tnext generated\t" + next.size());
            result.join(layer, new HashSet<>(next.getMemory()), stats, constraintTime, 0, 0);// hardRules.size(), minSupportHardRules.size());
            history.put(layer, next);

            if (!time.enough()) {
                break;
            }

            // selection of candidates for another line
            boolean update = !(this.saturatorProvider instanceof ConstantSaturationProvider);
            if (Integer.MAX_VALUE != beamSize || update) { // if we want to traverse the whole layer and there isn't a learning saturator provider, then all of the operations inside are just a waste of resources
                List<SearchNodeInfo> snis = Sugar.list();
                if (compareByAccuracy) {
                    for (int n = 0; n < beamSize && next.isNotEmpty(); n++) {
                        snis.add(next.poll());
//                        System.out.println(snis.get(snis.size() - 1).getAccuracy());
                    }
                } else {
                    while (next.isNotEmpty()) {
                        snis.add(next.poll());
                    }
                }

                Stream<SearchNodeInfo> stream = snis.stream();
//                if (compareByAccuracy) { // pro top-k-beam search tu má být tohle
//                    stream = stream.sorted(Comparator.comparing(SearchNodeInfo::getAccuracy).reversed());
//                } else
                if (randomSelect) { // tady to parametrizovat pro ruzne pripad, pro tezeni featur tu ma byt tohle
                    Collections.shuffle(snis, random);
                    stream = snis.stream();
                }

                if (Integer.MAX_VALUE != beamSize) {
                    stream = stream.limit(beamSize);
                }

                list = new CollectionWithMemory<>(new ListCollection<>(), false); // here, we know that we are adding different refinements into to the collection, thus there is no need for duplicity checking
                CollectionWithMemory<SearchNodeInfo> finalList = list;
                stream.forEach(sni -> {
                    finalList.push(sni);
                    if (update) {// it would be waste of time if !update would hold
                        this.saturatorProvider.getSaturator().update(sni.getRule(), sni.getCovered(), sni.getParentsCoverage());
                    }
                });
                this.saturatorProvider.getSaturator().nextDepth(time);
            }
            levelSizes.add(list.size());
            System.out.println("size\t" + list.size());

        }
        System.out.println("successors");
        //int previous = 0;
        /*for (int idx = 0; idx <= maxDepth; idx++) {
            if (history.keySet().contains(idx)) {
                //int current = history.get(idx).getMemory().size();
                //System.out.println("\t" + idx + " " + current + " -- " + (current - previous));
                //previous = history.get(idx).getMemory().size();
            }
        }*/
        int cumSum = 0;
        for (int idx = 0; idx < Math.min(levelSizes.size(), maxDepth); idx++) {
            System.out.println("\t" + idx + " " + levelSizes.get(idx) + " -- " + cumSum);
            cumSum += levelSizes.get(idx);
        }

        System.out.println("chce to dodelat nejruznejsi upravy LB atd aby byla yajistena minimalita a podobne :)) -- to i v DLT");
        return result;
    }

    {
        System.out.println("zkontrolovat razeni v queueu :))");
    }

    private MultiMap<Pair<String, Integer>, String> prepareOneLiteralConstraints() {
        MultiMap<Pair<String, Integer>, String> retVal = new MultiMap<>(MultiMap.LINKED_HASH_SET);
        this.saturatorProvider.getSaturator().getTheory().stream()
                .filter(c -> c.countLiterals() == 1)
                .forEach(c -> {
                    Literal literal = Sugar.chooseOne(c.literals());
                    oneLiteralConstraints.put(literal.getPredicate(), toCanon(literal));
                });
        return retVal;
    }


    private HornClause initialClause() {
        ida.ilp.logic.Predicate predicate = ida.ilp.logic.Predicate.construct(System.getProperty("ida.searchPruning.targetPredicate", null));
        List<Variable> head = LogicUtils.freshVariables(this.simplifiedTyping, predicate);
        return HornClause.create(new Literal(predicate.getName(), false, head), Sugar.list());
    }

    private double computeAccuracy(Coverage pos, Coverage neg, HornClause rule) {
        return null == criterion ? 0.0d : criterion.compute(pos.size(), neg.size(), rule);
    }

    private boolean computeAllowablitiy(Coverage pos, Coverage neg, HornClause rule) {
        return null == criterion || criterion.isAllowed(pos.size(), neg.size(), rule);
        //return null == criterion ? true : criterion.isAllowed(pos.size(), neg.size(), rule);
    }

/*    private Pair<List<HornClause>, Set<Literal>> filterCandidates(List<HornClause> candidates, Clause originalClause, Coverage covered) {
        Pair<List<HornClause>, Set<Literal>> saturated = saturationRefinement(candidates, originalClause, covered);
        int saturatedSize = saturated.r.size();
        candidates = filterIsomorphic(saturated.r);
        int unique = candidates.size();
        stats.nodesPruned(saturatedSize - unique);
        return new Pair<>(candidates, saturated.s);
    }

    private Pair<List<HornClause>, Set<Literal>> saturationRefinement(List<HornClause> candidates, Clause parent, Coverage covered) {
        List<HornClause> saturated = Sugar.list();
        Set<Literal> badRefinement = Sugar.set();

        for (HornClause candidate : candidates) {
            if (this.saturatorProvider.getSaturator().isPruned(candidate, covered)) {
                stats.hypothesesKilled();
                badRefinement.addAll(Sugar.setDifference(candidate.toClause().literals(), parent.literals()));
                continue;
            }
            HornClause extended = this.saturatorProvider.getSaturator().saturate(candidate, covered);
            if (null == extended) {
                stats.hypothesesKilled();
                badRefinement.addAll(Sugar.setDifference(candidate.toClause().literals(), parent.literals()));
            } else {
                saturated.add(extended);
                if (extended.body().countLiterals() > candidate.body().countLiterals()) {
                    stats.hypothesesExtended();
                }
            }
        }
        return new Pair<>(saturated, badRefinement);
    }

    private List<HornClause> filterIsomorphic(Collection<HornClause> coll) {
        Set<IsoClauseWrapper> set = coll.stream().map(hc -> IsoClauseWrapper.create(hc.toClause())).collect(Collectors.toSet());
        return set.stream().map(icw -> HornClause.create(icw.getOriginalClause())).collect(Collectors.toList());
    }

*/

    private SearchNodeInfo evaluate(SearchNodeInfo node) {
        Pair<Coverage, Coverage> coverages = dataset.classify(node.getRule(), node.getParentsCoverage());
        Coverage pos = coverages.r;
        Coverage neg = coverages.s;
        node.setCoverages(pos, neg);
        node.setAccuracy(computeAccuracy(coverages.r, coverages.s, node.getRule()));
        node.setAllowability(computeAllowablitiy(pos, neg, node.getRule()));
        return node;
    }


    private Set<SearchNodeInfo> refinements(SearchNodeInfo parent, Set<IsoClauseWrapper> isoProcessedNonSaturated) {
        List<Quadruple<HornClause, IsoClauseWrapper, Literal, IsoClauseWrapper>> rawRefinements = Sugar.list(); // quadruple of hc, ICW of rule (existentially quantified), added literal, ICW of non-saturated rule (existentially quantified)
        //todo: refinement extension add here
        //Matching world = (this.hcExtendedRefinement && this.saturatorProvider.getSaturator().nonEmptyLFC(parent.getCovered())) ? Matching.create(LogicUtils.constantizeClause(parent.saturatedBody()), Matching.THETA_SUBSUMPTION) : null;
        Matching world = null;
        Set<Variable> variables = parent.getRule().variables();


        for (ida.ilp.logic.Predicate predicate : allAllowedPredicates) {
            //rawRefinements.addAll(
            refinements(parent, variables, predicate, world)
                    .map(pair -> {
                        IsoClauseWrapper nonSaturatedICW = SearchNodeInfo.saturationSpaceExploration ? pair.r : IsoClauseWrapper.create(new Clause(Sugar.iterable(parent.getRule().toExistentiallyQuantifiedConjunction().literals(), Sugar.list(pair.s)))); // not nice but should be memory and speed friendly
                        // we store the horn rule as existentially quantified conjunction of literals
                        return new Quadruple<>(
                                HornClause.create(parent.getRule().head(), SearchNodeInfo.saturationSpaceExploration
                                        ? new Clause(LogicUtils.positiveLiterals(pair.r.getOriginalClause()))
                                        : new Clause(Sugar.iterable(Sugar.list(pair.s), parent.getRule().body().literals()))),
                                pair.r,
                                pair.s,
                                nonSaturatedICW);
                    })
                    .filter(quadruple -> {
                        synchronized (isoProcessedNonSaturated) {
                            return isoProcessedNonSaturated.add(quadruple.u);
                        }
                    })
                    .forEach(q -> {
                        synchronized (rawRefinements) {// it would be better to use some concurrent list
                            rawRefinements.add(q);
                        }
                    });
            //.forEach(rawRefinements::add);
            //.collect(Collectors.toList()));
        }

        // or do this by some kind of flatMap
        Set<SearchNodeInfo> retVal = Sugar.set();

        Sugar.parallelStream(rawRefinements, this.parallel)
                .map(quadruple -> {
                    HornClause rule = quadruple.r;
                    IsoClauseWrapper saturated = quadruple.s;
                    Literal newLiteral = quadruple.t;
                    IsoClauseWrapper nonSaturated = quadruple.u;

                    if (this.saturatorProvider.getSaturator().isPruned(rule, parent.getCovered())) {
                        stats.hypothesesKilled();
                        parent.addToForbidden(newLiteral);
                    }

                    HornClause extended = this.saturatorProvider.getSaturator().saturate(rule, parent.getCovered());
                    IsoClauseWrapper extendedICW = saturated;
                    if (null == extended) {
                        stats.hypothesesKilled();
                        parent.addToForbidden(newLiteral);
                        return null;
                    } else if (extended.body().countLiterals() > rule.body().countLiterals()) {
                        extendedICW = IsoClauseWrapper.create(extended.toExistentiallyQuantifiedConjunction());
                        stats.hypothesesExtended();
                    }

                    return SearchNodeInfo.create(rule, nonSaturated, extended, extendedICW, parent, newLiteral);
                }).filter(Objects::nonNull)
                .forEach(sni -> { // done by this awful approach because we can't add to a set ICW in parallel (or there should be different synchronization)
                    synchronized (retVal) {//
                        retVal.add(sni);
                    }
                });

        stats.nodesPruned(rawRefinements.size() - retVal.size());

        return retVal;
    }


    {
        System.out.println("nebude chyba podobna tomu co hledam tady i v USCLFSmarter");
    }

    // returns non-isomorphical existentially quantified refined bodies of the parent's saturated body
    private Stream<Pair<IsoClauseWrapper, Literal>> refinements(SearchNodeInfo parent, Set<Variable> variables, ida.ilp.logic.Predicate predicate, Matching existentiallyQuantifiedConstantinizedBody) {
        // watch out, linked hash set needs to be as the underlying collection (since order is preserved); in the case different data structure would be used, we would need to use map<ICW, Pair<literal, Set<literal>>> where the the first literal of the pair would be the one literal inserted to the original clause, so the literal would be in the clause represented by ICW
        MultiMap<IsoClauseWrapper, Literal> refinements = new MultiMap<>(MultiMap.LINKED_HASH_SET);
        List<Variable> freshVariables = useTyping ? LogicUtils.freshVariables(variables, simplifiedTyping, predicate) : Sugar.listFromCollections(LogicUtils.freshVariables(variables, predicate.getArity()));
        Literal freshLiteral = new Literal(predicate.getName(), false, freshVariables); // we are working with existentially  quantified conjunctions
        Clause originalClause = parent.getSaturatedRule().toExistentiallyQuantifiedConjunction();
        Clause init = new Clause(Sugar.iterable(originalClause.literals(), Sugar.list(freshLiteral)));
        refinements.put(new IsoClauseWrapper(init), freshLiteral);

        for (int i = 0; i < predicate.getArity(); i++) {
            MultiMap<IsoClauseWrapper, Literal> newRefinements = new MultiMap<>(MultiMap.LINKED_HASH_SET);
            for (Map.Entry<IsoClauseWrapper, Set<Literal>> entry : refinements.entrySet()) {
                Literal refLiteral = entry.getValue().iterator().next();
                Variable x = (Variable) refLiteral.get(i);

                for (Variable v : entry.getKey().getOriginalClause().variables()) {
                    if (v != x && (!this.useTyping || areSameTypes(v, x))) {
                        Literal newLiteral = LogicUtils.substitute(refLiteral, x, v);

                        if (predicate.getArity() == i + 1    // just a hack to run the minimum number of tests for forbidden and saturated
                                && (parent.isForbidden(newLiteral) // I can do this here because literals containing only variables from parent are in forbidden; and from such literal I cannot (by this method) generate literals with newly added variables
                                || parent.isRedundant(newLiteral)) // whether the literal is redundant depends whether we are traversing space of saturations or non-saturations
                                ) {
                            continue;
                        }
                        Clause substituted = LogicUtils.substitute(entry.getKey().getOriginalClause(), x, v);
                        //if (substituted.countLiterals() > parent.getNonSaturated().countLiterals() && !substituted.containsLiteral(newLiteral.negation())) { // this two conditions are solved elsewhere; the first one is solved in parent.isRedundant, while the second one is solved in parent.isForbidden (it should contain the head of the rule ;))
                        newRefinements.put(IsoClauseWrapper.create(substituted), newLiteral);
                            /* this would not add any speed-up ;)
                            //HornClause candidate = new HornClause(substituted);
                            if (exampleCoveredByParents.size() >= minSupport
                                    && (dataset.numExistentialMatches(candidate, minSupport, exampleCoveredByParents) >= minSupport)) {
                                Clause candClause = candidate.toClause();
                                newRefinements.put(new IsoClauseWrapper(candClause), newLiteral);
                            } else {
                                badRefinements.put(hc, newLiteral);
                            }*/
                    }
                }
            }
            refinements.putAll(newRefinements);
        }

        Stream<Map.Entry<IsoClauseWrapper, Set<Literal>>> retVal = Sugar.parallelStream(refinements.entrySet(), parallel)
                .filter(entry -> (!this.connectedOnly || entry.getKey().getOriginalClause().connectedComponents().size() == 1)
                        && this.mode.predicate().test(entry.getKey().getOriginalClause(), originalClause));

        // TODO tady by melo byt this.saturatorProvider.getSaturator().get(coverage).getOneLiteralConstraints()
        // the same extension as in UltraShortConstraintLearnerFasterSmarter
        if (this.oneLiteralHCrefinement && oneLiteralConstraints.keySet().size() > 0) {
            retVal = retVal.map(entry -> {
                Literal l = Sugar.chooseOne(entry.getValue());
                Set<String> forbiddens = oneLiteralConstraints.get(l.getPredicate());
                if (!forbiddens.isEmpty() && forbiddens.contains(toCanon(l))) {
                    parent.addToForbidden(entry.getValue());
                    return null;
                }
                return entry;
            }).filter(Objects::nonNull);
        }

        /* TODO --> nonEmptyLFC(Coverage covered);
        tady se musi tohle jeste upravit :))aby saturatorProvide poskytoval to co potrebujeme pro longer constraints a
        oneLiteralHCrefinement
        // !negated because we are operating can use this pruning only on existentially conjunctions
        if (this.hcExtendedRefinement && !longerConstraints.keySet().isEmpty()) {
            retVal = retVal
                    .sequential() // because we can't run matching (world) in parallel
                    .map(pair -> {

                        Literal l = Sugar.chooseOne(pair.getValue());
                        List<Pair<Literal, Clause>> constraints = longerConstraints.get(l.getPredicate());
                        if (null == constraints || constraints.isEmpty()) {
                            return pair;
                        }

                        for (Literal literal : pair.getValue()) {
                            if (pair.getKey().getOriginalClause().literals().contains(literal)) {
                                l = literal; // kind of overloading, not nice
                                break;
                            }
                        }

                        for (Pair<Literal, Clause> constraintPair : constraints) {
                            Map<Term, Term> partialSubstitutions = unify(constraintPair.getR(), LogicUtils.constantize(l));
                            if (null == partialSubstitutions) {
                                continue;
                            }
                            Clause substitutedBody = LogicUtils.substitute(constraintPair.getS(), partialSubstitutions); // existentially quantified
                            if (existentiallyQuantifiedConstantinizedBody.subsumption(substitutedBody, 0)) {
                                parent.addToForbidden(pair.getValue());
                                return null;
                            }
                        }

                        return pair;
                    }).filter(Objects::nonNull);
        }
        */

        return retVal.map(entry -> new Pair<>(entry.getKey(), entry.getValue().iterator().next()));
        //.distinct() keys from the map are unique, therefore no need for distinct here ;)
    }

    /*
    // returns null iff unification is not possible
    private Map<Term, Term> unify(Literal nonGround, Literal ground) {
        if (!nonGround.getPredicate().equals(ground.getPredicate()) || nonGround.isNegated() != ground.isNegated()) {
            return null;
        }
        Map<Term, Term> retVal = new HashMap<>();
        for (int idx = 0; idx < nonGround.arity(); idx++) {
            Term src = nonGround.get(idx);
            Term img = ground.get(idx);
            Term alreadyImg = retVal.get(src);
            if (null == alreadyImg) {
                retVal.put(src, img);
            } else if (!alreadyImg.equals(img)) {
                return null;
            }
        }

        return retVal;
    }
    */

    private String toCanon(Literal literal) {
        StringBuilder sb = new StringBuilder();
        //sb.append(literal.predicate() + "_");
        Map<Term, Integer> map = new HashMap<>();
        for (int idx = 0; idx < literal.arity(); idx++) {
            Term term = literal.get(idx);
            Integer originalPosition = map.get(term);
            if (null == originalPosition) {
                originalPosition = map.keySet().size() + 1;
                map.put(term, originalPosition);
            }
            if (idx > 0) {
                sb.append("," + originalPosition);
            }
        }
        return sb.toString();
    }

    private boolean areSameTypes(Variable v, Variable x) {
        return (null == v.type() && null == x.type())
                || (null != v.type() && v.type().equals(x.type()));
    }


    public void setDataset(MEDataset dataset) {
        this.dataset = dataset;
    }

    private static SimpleLearner create(MEDataset dataset, List<Clause> constraints, Coverage learnFrom, Criterion criterion, TimeDog timeDog, MutableStats mutableStats, int minSupport, List<Clause> minSuppRules) {
        System.out.println("there is a possibility of a bug");
        return new SimpleLearner(dataset, ConstantSaturationProvider.createFilterSaturator(constraints, minSuppRules), learnFrom, criterion, timeDog, mutableStats, minSupport);
    }

    public static void main(String[] args) throws IOException {
        //satT();
        refinementT();
    }

    private static void refinementT() throws IOException {
        SimpleLearner sl = SimpleLearner.create(MEDataset.create(Paths.get(".\\datasets\\splitted\\nci_transformed\\gi50_screen_KM20L2\\split7030\\train"), Matching.OI_SUBSUMPTION), Sugar.list(),
                CoverageFactory.getInstance().take(), new Accuracy(), new TimeDog(1000), new MutableStats(),
                100, Sugar.list());
        Clause parent = Clause.parse("!ar_bond(X, Z),  !2_bond(X, Y), !2_bond(Y, X), !car(Z)");
        System.out.println("parent\t" + parent);
        for (Literal literal : parent.literals()) {
            Clause shorther = new Clause(Sugar.collectionDifference(parent.literals(), literal));
            if (shorther.connectedComponents().size() != 1) {
                continue;// this wont be a parent
            }

            System.out.println("\ntesting shorther\t" + literal + "\n\t" + shorther);
            SearchNodeInfo sni = SearchNodeInfo.create(HornClause.create(shorther), null, CoverageFactory.getInstance().take(), CoverageFactory.getInstance().take());
            Set<IsoClauseWrapper> iso = Sugar.set();
            Collection<SearchNodeInfo> refinements = sl.refinements(sni, iso);
            refinements.stream()
                    .filter(node -> Sugar.chooseOne(Sugar.collectionDifference(node.getRule().toClause().literals(), shorther.literals()))
                            .predicate().equals(literal.predicate())
                    )
                    .forEach(node -> System.out.println("ref\t" + node.getRule().toClause()));

        }
    }

    private static void satT() {
        HornClause hc = new HornClause(null, Clause.parse("a(X),b(X),c(X,Y)"));

        List<Clause> constraints = Sugar.list(Clause.parse("c(X,Y),!c(Y,X)")
                , Clause.parse("!c(X,Y),a(X)")
                //, Clause.parse("!a(X),!b(X)") // should be inconsistent with this line uncommented
        );

        List<Clause> data = Sugar.list(Clause.parse("a(a),b(b),c(a,b)"));
        List<Double> targets = Sugar.list(1.0);
        MEDataset dataset = new MEDataset(data, targets);
        SimpleLearner learner = SimpleLearner.create(dataset,
                constraints,
                CoverageFactory.getInstance().get(dataset.size()),
                new Accuracy(),
                new TimeDog(Long.MAX_VALUE),
                new MutableStats(),
                1, Sugar.list());

        //HornClause res = learner.saturationRefinement(hc, constraints);
        // sem uz se muze dat treba i conjencture saturator
        HornClause res = RuleSaturator.create(constraints).saturate(hc);

        System.out.println(res);

    }


}