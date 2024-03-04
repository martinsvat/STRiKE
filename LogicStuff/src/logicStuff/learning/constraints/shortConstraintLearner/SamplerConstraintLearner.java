package logicStuff.learning.constraints.shortConstraintLearner;

import ida.ilp.logic.Clause;
import ida.ilp.logic.special.IsoClauseWrapper;
import ida.utils.Sugar;
import ida.utils.TimeDog;
import ida.utils.tuples.Pair;
import logicStuff.learning.datasets.DatasetInterface;
import logicStuff.learning.languageBias.LanguageBias;
import logicStuff.learning.languageBias.NoneBias;

import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 9. 12. 2017.
 */
public class SamplerConstraintLearner {


    private final int absoluteAllowedSubsumption;
    private final DatasetInterface dataset;
    private final LanguageBias bias;
    private boolean verbose = true;
    private final int maxRoundsWithoutAddingSingleConstraint = Integer.parseInt(System.getProperty("ida.logicStuff.samplerConstraintLearner.maxRounds", "2"));

    public SamplerConstraintLearner(DatasetInterface dataset, int absoluteAllowedSubsumption, LanguageBias bias) {
        this.dataset = dataset;
        this.absoluteAllowedSubsumption = absoluteAllowedSubsumption;
        this.bias = bias;
    }


    public Pair<List<Clause>, List<Clause>> learnConstraints(int rounds, int sampledBeam, int maxVariable, int maxLiterals, int maxPosLiterals, int maxNegLiterals, int maxComponents, Set<IsoClauseWrapper> constraints, Set<IsoClauseWrapper> minSuppConstraints, int minSupport, boolean useSaturations) {
        return learnConstraints(rounds, sampledBeam, maxVariable, maxLiterals, maxPosLiterals, maxNegLiterals, maxComponents, constraints, minSuppConstraints, minSupport, null, useSaturations);
    }

    public Pair<List<Clause>, List<Clause>> learnConstraints(int rounds, int sampledBeam, int maxVariable, int maxLiterals, int maxPosLiterals, int maxNegLiterals, int maxComponents, Set<IsoClauseWrapper> constraints, Set<IsoClauseWrapper> minSuppConstraints, int minSupport, TimeDog time, boolean useSaturations) {
        return learnConstraints(rounds, sampledBeam, maxVariable, maxLiterals, maxPosLiterals, maxNegLiterals, maxComponents, constraints, minSuppConstraints, minSupport, time, null, useSaturations);
    }

    /**
     * returns pair composed of domain constraints (for saturation) and constraints with minsupport (only for pruning)
     *
     * @param rounds
     * @param sampledBeam
     * @param maxVariable
     * @param maxLiterals
     * @param maxPosLiterals
     * @param maxNegLiterals
     * @param maxComponents
     * @param minSupport
     * @return
     */
    public Pair<List<Clause>, List<Clause>> learnConstraints(int rounds, int sampledBeam, int maxVariable, int maxLiterals, int maxPosLiterals, int maxNegLiterals, int maxComponents, Set<IsoClauseWrapper> constraints, Set<IsoClauseWrapper> minSuppConstraints, int minSupport, TimeDog time, BiConsumer<Set<IsoClauseWrapper>, Set<IsoClauseWrapper>> outputPipe, boolean useSaturations) {
        if (null == time) {
            time = new TimeDog(0, true);
        }
        UltraShortConstraintLearnerFaster ultrascl = UltraShortConstraintLearnerFaster.create(this.dataset, maxLiterals, maxVariable, this.absoluteAllowedSubsumption, maxComponents, maxPosLiterals, maxNegLiterals, outputPipe, bias);
        Set<IsoClauseWrapper> theory = Sugar.setFromCollections(constraints);
        Set<IsoClauseWrapper> minSuppTheory = Sugar.setFromCollections(minSuppConstraints);
        ultrascl.setVerbose(this.verbose);
        List<Integer> constraintsDiffs = Sugar.list();
        constraintsDiffs.add(theory.size() + minSuppTheory.size());
        for (int round = 0; round < rounds; round++) {
            // could delete ultraSCLF stateful statistic


            // add here early stopping for constraints learning

            Pair<List<Clause>, List<Clause>> learned = ultrascl.learnConstraints(useSaturations, false, false,
                    sampledBeam, Sugar.set(), false,
                    0, false, theory, minSuppTheory, minSupport, time);
            theory.addAll(learned.r.stream().map(IsoClauseWrapper::create).collect(Collectors.toList()));
            minSuppTheory.addAll(learned.s.stream().map(IsoClauseWrapper::create).collect(Collectors.toList()));

            if (verbose) {
                System.out.println("*******\nround\t" + round + "\twith theory size\t" + theory.size() + "\twith minsupp theory\t" + minSuppTheory.size() + "********\n");
            }
            constraintsDiffs.add((theory.size() + minSuppTheory.size()) - constraintsDiffs.get(constraintsDiffs.size() - 1));

            if (maxRoundsWithoutAddingSingleConstraint >= constraintsDiffs.size()
                    && IntStream.range(constraintsDiffs.size() - maxRoundsWithoutAddingSingleConstraint, constraintsDiffs.size())
                    .allMatch(idx -> constraintsDiffs.get(idx) < 1)) {
                if (verbose) {
                    System.out.println("ending in this round since last " + maxRoundsWithoutAddingSingleConstraint + " none constraint was added");
                }
                break;
            }

        }
        return new Pair<>(theory.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()),
                minSuppTheory.stream().map(IsoClauseWrapper::getOriginalClause).collect(Collectors.toList()));
    }

    public Pair<List<Clause>, List<Clause>> learnConstraints(int rounds, int sampledBeam, int maxVariable, int maxLiterals, int maxPosLiterals, int maxNegLiterals, int maxComponents, int minSupport, boolean useSaturations) {
        return learnConstraints(rounds, sampledBeam, maxVariable, maxLiterals, maxPosLiterals, maxNegLiterals, maxComponents, Sugar.set(), Sugar.set(), minSupport, useSaturations);
    }

    public static SamplerConstraintLearner create(DatasetInterface dataset, int absoluteAllowedSubsumption, LanguageBias bias) {
        return new SamplerConstraintLearner(dataset,absoluteAllowedSubsumption,bias);
    }

    public static SamplerConstraintLearner create(DatasetInterface dataset, int absoluteAllowedSubsumption) {
        return create(dataset,absoluteAllowedSubsumption, NoneBias.create());
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
