package ida.pacReasoning.entailment.cuts;

import ida.ilp.logic.Literal;
import ida.ilp.logic.Predicate;
import ida.pacReasoning.Outputable;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.utils.Sugar;
import ida.utils.collections.DoubleCounters;
import ida.utils.collections.FakeMap;
import ida.utils.tuples.Pair;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 21. 6. 2018.
 */
public class GammasCut implements Cut, Outputable {

    /*
    public static GammasCut ALL_BANNED = new GammasCut(new Map<Pair<String, Integer>, Double>() {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return false;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Double get(Object key) {
            return Double.POSITIVE_INFINITY;
        }

        @Override
        public Double put(Pair<String, Integer> key, Double value) {
            return null;
        }

        @Override
        public Double remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends Pair<String, Integer>, ? extends Double> m) {

        }

        @Override
        public void clear() {

        }

        @Override
        public Set<Pair<String, Integer>> keySet() {
            return null;
        }

        @Override
        public Collection<Double> values() {
            return Sugar.set(Double.POSITIVE_INFINITY);
        }

        @Override
        public Set<Entry<Pair<String, Integer>, Double>> entrySet() {
            return null;
        }
    }, 1, "all banned cut (all gammas are set up to infinity)");
    */

    public static GammasCut ALL_BANNED = new GammasCut(new HashMap<>(), 1, "all banned cut (none gamma is present; if gamma is not present for a predicate then it is assumed to be set to maximal value)");
    // if no gamma is in the cToPowCache, then 0 is taken by default
    // the opposite behavior can be done by setting gamma to infinity ;)

    private final Map<Pair<String, Integer>, Double> gammas;
    private final String name;
    private final Map<Pair<Long, Integer>, Double> cToPowCache;
    private final int k;


    public GammasCut(Map<Pair<String, Integer>, Double> gammas, Integer k, String name) {
        this.gammas = gammas;
        this.name = name;
        this.cToPowCache = new ConcurrentHashMap<>();
        this.k = k;
    }

    @Override
    public boolean isEntailed(Literal literal, double votes, long constants) {
        return isEntailed(literal.predicate(), literal.arity(), literal.terms().size(), votes, constants);
    }

    @Override
    public boolean isEntailed(Predicate predicate, int a, double votes, long constants) {
        return isEntailed(predicate.getName(), predicate.getArity(), a, votes, constants);
    }

    @Override
    public boolean isEntailed(String predicate, int arity, int a, double votes, long constants) {
        Pair<String, Integer> pair = new Pair<>(predicate, arity);
        Double gamma = gammas.get(pair);
        if (null == gamma) {
            return false;
//            return votes >= 1.0; // change here from now
        }

        if (Double.compare(Double.POSITIVE_INFINITY, gamma) == 0) {
            return false;
        }

        Double cToKMinusA = cToX(constants, a);
//        System.out.println(votes + "\t>=\t" + cToKMinusA * gamma);
        return votes >= cToKMinusA * gamma;
    }

    @Override
    public VECollector entailed(VECollector data) {
        return VECollector.create(data.getEvidence()
                , DoubleCounters.createCounters(Sugar.parallelStream(data.getVotes().entrySet(), true)
                        .filter(e -> this.isEntailed(e.getKey(), e.getValue(), data.constantsSize()))
                        .map(e -> new Pair<>(e.getKey(), e.getValue()))
                        .collect(Collectors.toSet()))
                , data.getK()
                , data.constantsSize()
                , data.getConstants()
                , data.originalEvidenceSize());
    }

    private Double cToX(long constants, int a) {
        Pair<Long, Integer> key = new Pair<>(constants, a);
        Double val = cToPowCache.get(key);
        if (null == val) {
            // according to UAI paper
            val = Math.pow(constants, k - a);
            // more granular approach
//            val = 1.0;
//            for (long i = constants - k + a; i <= constants; i++) {
//                val *= i;
//            }
            cToPowCache.put(key, val);
            if(Double.POSITIVE_INFINITY == val){
                System.err.println("this should not happen here, GammasCut::cToX producted infinity");
            }
        }
        return val;
    }

    @Override
    public Cut cut(int evidenceSize) {
        return this;
    }

    @Override
    public Set<Predicate> predicates() {
        return Sugar.parallelStream(gammas.keySet())
                .map(Predicate::create)
                .collect(Collectors.toSet());
    }

    @Override
    public String name() {
        return (name.length() < 1) ? "gammas" : name;
    }

    @Override
    public String asOutput() {
        return "k:\t" + this.k + "\n"
                + this.gammas.entrySet().stream()
                .map(entry -> entry.getKey().r + "/" + entry.getKey().s + "\t" + entry.getValue())
                .sorted()
                .collect(Collectors.joining("\n"));
    }


    public Map<Pair<String, Integer>, Double> getGammas() {
        return gammas;
    }

    public static Pair<Pair<String, Integer>, Double> parseLine(String line) {
        String[] splitted = line.split("\t");
        if (splitted.length != 2) {
            System.out.println("cannot parse line:\t" + line);
            throw new IllegalArgumentException();
        }
        String[] predicateSplitted = splitted[0].split("/");
        if (predicateSplitted.length != 2) {
            System.out.println("cannot parse predicate:\t" + splitted[0]);
            throw new IllegalArgumentException();
        }
        Pair<String, Integer> predicate = new Pair<>(predicateSplitted[0], Integer.parseInt(predicateSplitted[1]));
        return new Pair<>(predicate, Double.parseDouble(splitted[1]));
    }

    public static GammasCut load(List<String> lines, Integer k, String name) {
        Pair<Integer, Object> cacheK = new Pair<>(k, null);
        Map<Pair<String, Integer>, Double> gammas = lines.stream()
                .map(line -> {
                    if (line.startsWith("k:")) {
                        cacheK.r = Integer.parseInt(line.split("\t")[1]);
                        return null;
                    }
                    String[] splitted = line.split("\t");
                    if (splitted.length != 2) {
                        System.out.println("cannot parse line:\t" + line);
                        throw new IllegalArgumentException();
                    }
                    return new Pair<>(Predicate.construct(splitted[0]).getPair(), Double.parseDouble(splitted[1]));
                })
                .filter(p -> null != p)
                .collect(Collectors.toMap(Pair::getR, Pair::getS));
        return create(gammas, cacheK.r, name);
    }

    public static GammasCut load(Path path) {
        try {
            return load(Files.lines(path)
                            .parallel()
                            .filter(line -> line.trim().length() > 0 && !line.startsWith("#"))
                            .collect(Collectors.toList())
                    , null, path.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("some problem during loading and parsing the file");
    }

    /**
     * one gamma threshold for all possible predicates
     *
     * @param gamma
     * @param k
     * @return
     */
    public static GammasCut create(Double gamma, Integer k) {
        Map<Pair<String, Integer>, Double> map = new Map<Pair<String, Integer>, Double>() {

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public boolean containsKey(Object key) {
                return true;
            }

            @Override
            public boolean containsValue(Object value) {
                return false;
            }

            @Override
            public Double get(Object key) {
                return gamma;
            }

            @Override
            public Double put(Pair<String, Integer> key, Double value) {
                return null;
            }

            @Override
            public Double remove(Object key) {
                return null;
            }

            @Override
            public void putAll(Map<? extends Pair<String, Integer>, ? extends Double> m) {

            }

            @Override
            public void clear() {

            }

            @Override
            public Set<Pair<String, Integer>> keySet() {
                return null;
            }

            @Override
            public Collection<Double> values() {
                return null;
            }

            @Override
            public Set<Entry<Pair<String, Integer>, Double>> entrySet() {
                return null;
            }
        };
        return create(map, k, "");
    }

    public static GammasCut create(Map<Pair<String, Integer>, Double> gammas, int k) {
        return create(gammas, k, "");
    }

    public static GammasCut create(Map<Pair<String, Integer>, Double> gammas, Integer k, String name) {
        return new GammasCut(gammas, k, name);
    }

    public static GammasCut create(Predicate predicate, Double gamma, int k) {
        return create(predicate, gamma, k, k + ": " + predicate + ": " + gamma);
    }

    public static GammasCut create(Predicate predicate, Double gamma, int k, String name) {
        Map<Pair<String, Integer>, Double> map = new HashMap<>();
        map.put(predicate.getPair(), gamma);
        return create(map, k, name);
    }

}
