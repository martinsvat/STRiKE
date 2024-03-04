package ida.pacReasoning;

import ida.ilp.logic.*;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Just a container for debuging of minimal proofs.
 * <p>
 * Created by martin.svatos on 10. 4. 2019.
 */
public class MP {

    private final Map<Literal, List<Set<Constant>>> data;
    private Utils u = Utils.create();
    private LeastHerbrandModel lhm = new LeastHerbrandModel();

    public MP(Map<Literal, List<Set<Constant>>> data) {
        this.data = data;
    }

    public boolean isSomeSubsetNonminimal() {
        boolean someError = false;
        for (Map.Entry<Literal, List<Set<Constant>>> entry : this.data.entrySet()) {
//            System.out.println("\t" + entry.getKey());
            someError = isThereSomeNonminimalSubset(entry) || someError;
        }
        return someError;
    }

    private boolean isThereSomeNonminimalSubset(Map.Entry<Literal, List<Set<Constant>>> entry) {
        long nonMinimal = 0l;
        List<Set<Constant>> sets = Sugar.list();
        sets.addAll(entry.getValue());
        for (int outer = 0; outer < sets.size(); outer++) {
            Set<Constant> s1 = sets.get(outer);
            for (int inner = 1 + outer; inner < sets.size(); inner++) {
                Set<Constant> s2 = sets.get(inner);
                if (subsumes(s1, s2)) {
                    System.out.println("lit\t" + entry.getKey() + "\tthese two are nonminimals (1)\n\t" + toCanon(s1) + "\n\t" + toCanon(s2));
                    nonMinimal++;
                }
                if (subsumes(s2, s1)) {
                    System.out.println("lit\t" + entry.getKey() + "\tthese two are nonminimals (2)\n\t" + toCanon(s2) + "\n\t" + toCanon(s1));
                    nonMinimal++;
                }
            }
        }

        if (nonMinimal > 0) {
            System.out.println("there are " + nonMinimal + " non-minimals proofs in the db");
        }
        return nonMinimal > 0;
    }

    private boolean subsumes(Set<Constant> s1, Set<Constant> s2) {
        Set<Constant> intersection = Sugar.intersection(s1, s2);
        return intersection.size() == s1.size();
    }

    private String toCanon(Set<Constant> s1) {
        return "{" + s1.stream().map(Constant::toString).sorted().collect(Collectors.joining(", ")) + "}";
    }


    public static MP parse(Path minimalProofsSrc) throws IOException {
        return parse(minimalProofsSrc, true, (l) -> true);
    }

    public static MP parse(Path minimalProofsSrc, boolean untype, Predicate<String> filter) throws IOException {
        Map<Literal, List<Set<Constant>>> data = new HashMap<>();
        Pair<Literal, List<Set<Constant>>> p = new Pair<>(null, null);
        Files.lines(minimalProofsSrc).filter(l -> l.trim().length() > 0).forEach(line -> {
            if (null == p.r) {
                p.r = Literal.parseLiteral(line);
                if (untype) {
                    p.r = LogicUtils.untype(p.r);
                }
            } else {
                line = line.trim();
                line = line.substring(0, line.length() - 1);
                line = line.replace("{", "");
                List<Set<Constant>> constants = Arrays.asList(line.trim().split("},")).stream()
                        .map(String::trim)
                        .map(s -> {
                            String[] splitted = s.split(",");
                            if (untype) {
                                return Arrays.stream(splitted).map(String::trim).map(c -> (Constant) Sugar.chooseOne(LogicUtils.constants(LogicUtils.untype(Clause.parse("p(" + c + ")"))))).collect(Collectors.toSet());
                            }
                            return Arrays.stream(splitted).map(String::trim).map(c -> (Constant) Sugar.chooseOne(LogicUtils.constants(Clause.parse("p(" + c + ")")))).collect(Collectors.toSet());
                        }).collect(Collectors.toList());
                if (filter.test(p.r.toString())) {
                    data.put(p.r, constants);
                }
                p.r = null;
            }
        });
        return new MP(data);
    }

    public boolean isSomeDerivationIncorrect(Collection<Clause> rules, Set<Literal> evidence) {
        boolean someError = false;
        for (Map.Entry<Literal, List<Set<Constant>>> entry : data.entrySet()) {
//            System.out.println("\t" + entry.getKey());
            someError = !areAllDerivationsCorrect(entry.getKey(), entry.getValue(), rules, evidence) || someError;
        }
        return someError;
    }

    private boolean areAllDerivationsCorrect(Literal targetLiteral, List<Set<Constant>> subsets, Collection<Clause> rules, Set<Literal> evidence) {
        long incorrect = 0;
        for (Set<Constant> subset : subsets) {
            Set<Literal> fragment = u.mask(evidence, subset);
            Set<Literal> derived = lhm.herbrandModel(rules, fragment);
            if (!derived.contains(targetLiteral)) {
                System.out.println(targetLiteral + "\t" + fragment.size() + "\t" + derived.size() + "\t" + derived.contains(targetLiteral));
            } else {
                for (Constant constant : subset) {
                    Set<Constant> shorther = Sugar.setDifference(subset, constant);
                    Set<Literal> frg = u.mask(fragment, shorther);
                    derived = lhm.herbrandModel(rules, frg);
                    if (derived.contains(targetLiteral)) {
                        System.out.println(targetLiteral + "\thas non minimal fragment\t" + this.toCanon(shorther));
                        incorrect++;
                        break;
                    }
                }
            }
        }
        return 0 == incorrect;
    }

    public boolean isNotSameAs(MP another) {
        boolean someError = false;
        Set<Literal> keys = Sugar.union(this.data.keySet(), another.data.keySet());
        StringBuilder me = new StringBuilder();
        StringBuilder him = new StringBuilder();

        for (Literal head : keys) {
            List<Set<Constant>> mine = this.data.get(head);
            List<Set<Constant>> his = another.data.get(head);

            if (null == mine || null == his) {
                me.append("\n" + head + "\n" + (null == mine ? "" : mine.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "))));
                him.append("\n" + head + "\n" + (null == his ? "" : his.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "))));
                someError = true;
                continue;
            }
            String m1 = mine.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "));
            String h1 = his.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "));
            if (!m1.equals(h1)) {
                me.append("\n" + head + "\n" + (null == mine ? "" : mine.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "))));
                him.append("\n" + head + "\n" + (null == his ? "" : his.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "))));
                someError = true;
            }
        }

        String s1 = me.toString();
        String s2 = him.toString();
        if (s1.length() > 0) {
            System.out.println("these two do not have the same minimal proofs");
            System.out.println("me" + s1);
            System.out.println("\nhim" + s2);
        }
        return someError;
    }

    // muze byt snadno upravena pro to aby delala jine veci ;)
    public List<Literal> returnThoseMissinInAtLeastOne(MP another) {
        Set<Literal> keys = Sugar.union(this.data.keySet(), another.data.keySet());
        StringBuilder me = new StringBuilder();
        StringBuilder him = new StringBuilder();
        List<Literal> different = Sugar.list();
        for (Literal head : keys) {
            List<Set<Constant>> mine = this.data.get(head);
            List<Set<Constant>> his = another.data.get(head);

            if (null == mine || null == his) {
                me.append("\n" + head + "\n" + (null == mine ? "" : mine.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "))));
                him.append("\n" + head + "\n" + (null == his ? "" : his.stream().map(another::toCanon).sorted().collect(Collectors.joining(", "))));
                different.add(head);
                continue;
            }

            String m1 = mine.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "));
            String h1 = his.stream().map(another::toCanon).sorted().collect(Collectors.joining(", "));
            if (!m1.equals(h1)) {
                different.add(head);
                me.append("\n" + head + "\n" + (null == mine ? "" : mine.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "))));
                him.append("\n" + head + "\n" + (null == his ? "" : his.stream().map(this::toCanon).sorted().collect(Collectors.joining(", "))));
            }
        }

        String s1 = me.toString();
        String s2 = him.toString();
        if (s1.length() > 0) {
            System.out.println("these two do not have the same minimal proofs");
            System.out.println("me" + s1);
            System.out.println("\nhim" + s2);
        }

        return different;
    }
}
