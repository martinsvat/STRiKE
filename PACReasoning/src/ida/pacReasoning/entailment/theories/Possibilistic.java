package ida.pacReasoning.entailment.theories;

import ida.ilp.logic.Clause;
import ida.ilp.logic.LogicUtils;
import ida.ilp.logic.Predicate;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 8. 6. 2018.
 */
public class Possibilistic implements Theory {
    // stateful hacks for quicker operation
    // important assumption -- PT instances used in same collection (e.g. set) are subparts of the same origin PT, otherwise it fails all

    private final Set<Clause> hardRules;
    private final List<Pair<Double, Clause>> softRules;
    private final Set<Clause> all;
    private final int size;
    private final String name;
    private final Triple cache;

    private Possibilistic(Set<Clause> hardRules, List<Pair<Double, Clause>> softRules, String name) {
        this.hardRules = hardRules;
        this.softRules = softRules;
        Set<Clause> all = Sugar.union(hardRules, softRules.stream().map(pair -> pair.getS()).collect(Collectors.toList()));
        this.all = all;
        this.size = all.size();
        this.name = name;
        Set<Clause> hard = Sugar.set();
        Set<Clause> horn = Sugar.set();
        Set<Clause> other = Sugar.set();
        all.forEach(c -> {
            int posLits = LogicUtils.positiveLiterals(c).size();
            if (posLits < 1) {
                hard.add(c);
            } else if (1 == posLits) {
                horn.add(c);
            } else {
                other.add(c);
            }
        });
        this.cache = new Triple<>(hard, horn, other);
    }

    @Override
    public Triple<Set<Clause>, Set<Clause>, Set<Clause>> getConstrainsHornOthers() {
        return cache;
    }


    public String getName() {
        return name;
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


    public int size() {
        return this.size;
    }

    public String asOutput() {
        StringBuilder sb = new StringBuilder();
        hardRules.stream().forEach(c -> sb.append(c + "\n"));
        softRules.stream().forEach(p -> sb.append("Level " + p.r + "\n" + p.s + "\n"));
        return sb.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Possibilistic that = (Possibilistic) o;

        return size == that.size;
    }

    @Override
    public int hashCode() {
        return size;
    }


    public static Possibilistic create(Path file) {
        Set<Clause> hardRules = Sugar.set();
        List<Pair<Double, Clause>> softRules = Sugar.list();
        // aweful builder like
        Pair<Double, Clause> p = new Pair<>();
        try {
            Files.readAllLines(file).stream()
                    .filter(line -> line.trim().length() > 0
                            && !line.startsWith("---")
                            && !line.contains("Hard rules:")
                            && !line.contains("falsity weight")
                            && !line.contains("hard rules")
                            && !line.contains("implications")
                    )
                    .forEach(line -> {

                        if (line.startsWith("Level")) {
                            p.r = Double.parseDouble(line.split(" ", 2)[1]);
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
        return create(hardRules, softRules, file.toFile().getName());
    }

    public static Possibilistic create(Set<Clause> hardRules, List<Pair<Double, Clause>> softRules) {
        return create(hardRules, softRules, "");
    }

    public static Possibilistic create(Set<Clause> hardRules, List<Pair<Double, Clause>> softRules, String name) {
        return new Possibilistic(hardRules,
                // decreasing order
                softRules.stream().sorted((o1, o2) -> Double.compare(o2.getR(), o1.getR())).collect(Collectors.toList())
                , name);
    }


    public Possibilistic addTyping(Map<Pair<Predicate, Integer>, String> typing) {
        Set<Clause> hard = hardRules.stream().map(c -> LogicUtils.addTyping(c, typing)).collect(Collectors.toSet());
        List<Pair<Double, Clause>> soft = softRules.stream().map(p -> new Pair<>(p.r, LogicUtils.addTyping(p.s, typing))).collect(Collectors.toList());
        return create(hard, soft, this.name + ".typed");
    }

    public Possibilistic untype() {
        Set<Clause> hard = hardRules.stream().map(c -> LogicUtils.untype(c)).collect(Collectors.toSet());
        List<Pair<Double, Clause>> soft = softRules.stream().map(p -> new Pair<>(p.r, LogicUtils.untype(p.s))).collect(Collectors.toList());
        return create(hard, soft, this.name + ".untyped");
    }
}
