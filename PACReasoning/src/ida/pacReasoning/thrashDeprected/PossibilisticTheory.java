package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.Clause;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 27. 5. 2018.
 */
public class PossibilisticTheory {
    // stateful hacks for quicker operation
    // important assumption -- PT instances used in same collection (e.g. set) are subparts of the same origin PT, otherwise it fails all

    private final Set<Clause> hardRules;
    private final List<Pair<Double, Clause>> softRules;
    private final Set<Clause> all;
    private final int size;

    private PossibilisticTheory(Set<Clause> hardRules, List<Pair<Double, Clause>> softRules) {
        this.hardRules = hardRules;
        this.softRules = softRules;
        Set<Clause> all = Sugar.union(hardRules, softRules.stream().map(pair -> pair.getS()).collect(Collectors.toList()));
        this.all = all;
        this.size = all.size();
    }


    public Set<Clause> allRules() {
        return all;
    }

    public Set<Clause> getHardRules() {
        return hardRules;
    }

    // rules are sorted in decreasing order of weights
    public List<Pair<Double, Clause>> getSoftRules() {
        return softRules;
    }

    public static PossibilisticTheory create(Path file,boolean multipleWeightsByMinusOne) {
        Set<Clause> hardRules = Sugar.set();
        List<Pair<Double, Clause>> softRules = Sugar.list();
        // aweful builder like
        Pair<Double, Clause> p = new Pair<>();
        try {
            Files.readAllLines(file).stream()
                    .filter(line -> line.trim().length() > 0
                            && !line.startsWith("---")
                            && !line.contains("Hard rules:")
                            && !line.contains("falsity weight"))
                    .forEach(line -> {

                        if (line.startsWith("Level")) {
                            p.r = Double.parseDouble(line.split(" ", 2)[1]);
                            if(multipleWeightsByMinusOne){
                                p.r = - p.r;
                            }
                        } else {
                            if (p.r != null) {
                                softRules.add(new Pair<>(p.getR(), Clause.parse(line)));
                                p.r = null;
                            } else {
                                hardRules.add(Clause.parse(line));
                            }
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException();
        }
        return create(hardRules, softRules);
    }

    public static PossibilisticTheory create(Set<Clause> hardRules, List<Pair<Double, Clause>> softRules) {
        return new PossibilisticTheory(hardRules, // decreasing order
                softRules.stream().sorted((o1, o2) -> {
                    // awful, I know
//                    if (multiplyWeightsByMinusOne) {
//                        return Double.compare(o1.getR(), o2.getR());
//                    }
                    return Double.compare(o2.getR(), o1.getR());
                }).collect(Collectors.toList()));
    }

    public int size() {
        return this.size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PossibilisticTheory that = (PossibilisticTheory) o;

        return size == that.size;
    }

    @Override
    public int hashCode() {
        return size;
    }
}
