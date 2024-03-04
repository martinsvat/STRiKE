package ida.pacReasoning.data;

import ida.utils.Sugar;

import java.util.*;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 29. 7. 2020.
 */
public class RenamingFactory {

    private final Map<String, String> map;
    private final String prefix;

    public RenamingFactory(String prefix, Map<String,String> map){
        this.map = map;
        this.prefix = prefix;
    }

    public RenamingFactory(String prefix) {
        this(prefix,new HashMap<>());
    }

    public String get(String name) {
        if (!this.map.containsKey(name)) {
            this.map.put(name, prefix + map.entrySet().size());
        }
        return this.map.get(name);
    }

    public Set<String> getValues() {
        return Sugar.setFromCollections(this.map.values());
    }

    public Stream<String> serialize() {
        return this.map.entrySet().stream()
                .map(entry -> entry.getKey() + "\t" + entry.getValue());
    }


    public static RenamingFactory create(String prefix) {
        return new RenamingFactory(prefix);
    }
    public static RenamingFactory create(String prefix, Map<String,String> dictionary) {
        return new RenamingFactory(prefix, dictionary);
    }

    public static RenamingFactory createIdentityRenamingFactory(){
        Reformatter r = Reformatter.create();
        return create("",r.identityDictionary());
    }

    public List<String> toSortedDict() {
        List<String> result = Sugar.list();
        this.map.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getValue))
                .forEach(entry -> {
                    result.add(result.size() + "\t" + entry.getValue());
                });
        return result;
    }

    public Set<Map.Entry<String, String>> entries() {
        return this.map.entrySet();
    }
}
