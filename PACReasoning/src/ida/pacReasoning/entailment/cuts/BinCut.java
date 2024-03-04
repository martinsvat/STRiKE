package ida.pacReasoning.entailment.cuts;

import ida.ilp.logic.Literal;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.Outputable;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;
import ida.utils.tuples.Quadruple;
import ida.utils.tuples.Triple;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 22. 6. 2018.
 */
public class BinCut implements Cut, Outputable {

    private final List<? extends Cut> cuts;
    private final int binWidth;
    private final String name;
    private final int k;

    public BinCut(List<? extends Cut> cuts, int binWidth, int k, String name) {
        this.cuts = cuts;
        this.binWidth = binWidth;
        this.name = name;
        this.k = k;
    }

    @Override
    public boolean isEntailed(Literal literal, double votes, long constants) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEntailed(Predicate predicate, int a, double votes, long constants) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEntailed(String predicate, int arity, int a, double votes, long constants) {
        throw new UnsupportedOperationException();
    }

    @Override
    public VECollector entailed(VECollector data) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Cut cut(int evidenceSize) {
        int bin = evidenceSize / binWidth;
        bin = Math.min(bin, this.cuts.size() - 1);
        return cuts.get(bin);
    }

    @Override
    public Set<Predicate> predicates() {
     return Sugar.parallelStream(cuts)
             .flatMap(cut -> cut.predicates().stream())
             .collect(Collectors.toSet());
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String asOutput() {
        return "binWidth:\t" + binWidth + "\n" +
                "k:\t" + k + "\n"
                + cuts.stream()
                .map(cut -> {
                    String plainText = "next cut:\t" + cut.getClass() + "\t" + cut.name() + "\n";
                    if (cut instanceof Outputable) {
                        plainText += ((Outputable) cut).asOutput();
                    } else {
                        plainText += "is not outputable :(";
                    }
                    return plainText;
                }).collect(Collectors.joining("\n"));
    }

    public List<? extends Cut> getCuts() {
        return cuts;
    }

    // awful
    public static BinCut load(Path path) {
        Triple<Integer, Integer, List<Cut>> loading = new Triple<>(null, null, Sugar.list());
        Pair<Map<Pair<String, Integer>, Double>, Integer> cuts = new Pair<>(null, null);
        Pair<Integer, Boolean> statefulCutTyp = new Pair<>(null, null);
        List<String> lines = Sugar.list();

        try {
            final int gamma = 0;
            final int adaptive = 1;
            final int cnst = 2;
            Files.readAllLines(path).stream()
                    .filter(line -> line.trim().length() > 0 || line.startsWith("#"))
                    .forEach(line -> {
                        if (line.startsWith("binWidth:")) {
                            loading.r = Integer.parseInt(line.split("\t")[1]);
                        } else if (line.startsWith("k:") && null != cuts.r) {
                            cuts.s = Integer.parseInt(line.split("\t")[1]);
                        } else if (line.startsWith("k:")) {
                            loading.s = Integer.parseInt(line.split("\t")[1]);
                        } else if (line.startsWith("next cut:")) {
                            if (null != cuts.r) {
                                switch (statefulCutTyp.r) {
                                    case gamma:
                                        loading.t.add(GammasCut.create(cuts.r, cuts.s));
                                        break;
                                    case adaptive:
                                        loading.t.add(AdaptiveRatioCut.create(cuts.r, cuts.s));
                                        break;
                                    case cnst:
                                        // tahkle by to melo byt i u zbyvajicich -- netuplovat tady slozite a spatne parsovani
                                        loading.t.add(ConstantRatioCut.load(lines, "", cuts.s));
                                        break;
                                    default:
                                        break;
                                }
                            }


                            if (line.split("\t")[1].equals(GammasCut.class.toString())) {
                                statefulCutTyp.r = gamma;
                            } else if (line.split("\t")[1].equals(AdaptiveRatioCut.class.toString())) {
                                statefulCutTyp.r = adaptive;
                            } else if (line.split("\t")[1].equals(ConstantRatioCut.class.toString())) {
                                statefulCutTyp.r = cnst;
                                lines.clear();
                            } else {
                                System.out.println("do not know how to parse cut of class:\t" + line);
                                //throw new NotImplementedException();
                                throw new UnsupportedOperationException("Not implemented yet");
                            }

                            cuts.r = new HashMap<>();

                        } else {
                            switch (statefulCutTyp.r) {
                                case gamma:
                                    Pair<Pair<String, Integer>, Double> pG = GammasCut.parseLine(line);
                                    cuts.r.put(pG.r, pG.s);
                                    break;
                                case adaptive:
                                    Pair<Pair<String, Integer>, Double> pA = AdaptiveRatioCut.parseLine(line);
                                    cuts.r.put(pA.r, pA.s);
                                    break;
                                case cnst:
                                    lines.add(line);
                                    break;
                                default:
                                    break;
                            }

                        }
                    });
            if (null != cuts.r) {// we have to add the last cut
                loading.t.add(GammasCut.create(cuts.r, cuts.s));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return create(loading.t, loading.r, loading.s, path.toString());
    }

    public static BinCut create(List<? extends Cut> cuts, int binWidth, int k) {
        return create(cuts, binWidth, k, "");
    }

    public static BinCut create(List<? extends Cut> cuts, int binWidth, int k, String name) {
        return new BinCut(cuts, binWidth, k, name);
    }
}
