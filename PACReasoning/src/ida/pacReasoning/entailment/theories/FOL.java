package ida.pacReasoning.entailment.theories;

import ida.ilp.logic.Clause;
import ida.ilp.logic.LogicUtils;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Triple;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 8. 6. 2018.
 */
public class FOL implements Theory {
//    vymyslet nejake hezci nazvy
    // stateful hacks for quicker operation
    // important assumption -- FOL instances used in same collection (e.g. set) are subparts of the same origin FOL, otherwise it fails all

    private final Set<Clause> hardRules;
    private final Set<Clause> implications;
    private final Set<Clause> all;
    private final int size;
    private final String name;
    private final Triple<Set<Clause>, Set<Clause>, Set<Clause>> cache;

    private FOL(Set<Clause> hardRules, Set<Clause> implications, String name) {
        this.hardRules = hardRules;
        this.implications = implications;
        Set<Clause> all = Sugar.union(hardRules, implications);
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

    public String getName() {
        return name;
    }

    public Set<Clause> allRules() {
        return all;
    }

    public Set<Clause> getHardRules() {
        return hardRules;
    }

    public Set<Clause> getImplications() {
        return implications;
    }

    @Override
    public Triple<Set<Clause>, Set<Clause>, Set<Clause>> getConstrainsHornOthers() {
        return cache;
    }

    public int size() {
        return this.size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FOL that = (FOL) o;

        return size == that.size;
    }

    @Override
    public int hashCode() {
        return size;
    }


    @Override
    public String toString() {
        return Sugar.list(Sugar.list("hard rules")
                , hardRules
                , Sugar.list("implications")
                , implications).stream()
                .flatMap(c -> c.stream())
                .map(Object::toString)
                .collect(Collectors.joining("\n"));
    }

    /**
     * by default, firstly hard rules; thereafter implications
     *
     * @param file
     * @return
     */
    public static FOL create(Path file) {
        Set<Clause> hardRules = Sugar.set();
        Set<Clause> implications = Sugar.set();
        // aweful builder like
        String HARD_RULES = "hard rules";
        String IMPLICATIONS = "implications";
        Pair<Set<Clause>, String> push = new Pair<>(hardRules, HARD_RULES);
        Pair<Boolean, Double> p = new Pair<>(false, null);
        try {
            Files.readAllLines(file).stream()
                    .filter(line -> line.trim().length() > 0
                            && !line.startsWith("---")
                            && !line.startsWith("#"))
                    .forEach(line -> {
                        line = line.trim();
                        if (line.toLowerCase().startsWith("level")) {
                            p.r = true;
                            p.s = Double.parseDouble(line.split(" ", 2)[1]);
                        } else if (p.r) {
                            implications.add(Clause.parse(line));
                            p.r = false;
                            p.s = null;
                        } else if (line.equals(HARD_RULES)) {
                            push.set(hardRules, HARD_RULES);
                        } else if (line.equals(IMPLICATIONS)) {
                            push.set(implications, IMPLICATIONS);
                        } else {
                            push.r.add(Clause.parse(line));
                        }
//                        System.out.println("pushing to\t" + push.s + "\t" + line);
                    });
//            System.exit(-1);
        } catch (IOException e) {
            e.printStackTrace();
            throw new IllegalArgumentException();
        }
        return create(hardRules, implications, file.toFile().getName());
    }

    public static FOL create(Set<Clause> hardRules, Set<Clause> implications) {
        return create(hardRules, implications, "");
    }

    public static FOL create(Set<Clause> hardRules, Set<Clause> implications, String name) {
        return new FOL(hardRules, implications, name);
    }

}
