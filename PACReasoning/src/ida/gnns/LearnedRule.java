package ida.gnns;

import ida.ilp.logic.Clause;

import java.util.Map;

/**
 * Created by martin.svatos on 9. 10. 2020.
 */
public class LearnedRule {
    private final Map<String, String> properties;
    private final Clause rule;

    private LearnedRule(Map<String, String> properties, Clause rule) {
        this.properties = properties;
        this.rule = rule;
    }

    public String getProperty(String key) {
        return this.properties.get(key);
    }

    public Clause getRule() {
        return rule;
    }

    public static LearnedRule create(Map<String, String> propeties, Clause rule) {
        return new LearnedRule(propeties, rule);
    }

    public Map<String, String> getProperties() {
        return properties;
    }
}
