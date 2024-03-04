package ida.pacReasoning.entailment;

import ida.utils.Sugar;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 8. 6. 2018.
 */
public class EntailmentSetting {

    // TODO make this nicer, a little bit of junk right now
    // move constants here

    private final int entailmentMode;
    private final boolean dataDrivenApproach;
    private final boolean PLlogic;
    private final boolean cuttingPlanesPrune;
    private final boolean cuttingPlanesRelaxation;
    private final boolean classicalLogic;
    private final boolean initByScout;
    private final int k;
    private final int saturationMode;
    private final boolean megaScout;
    private final String algorithm;

    public EntailmentSetting(int entailmentMode, int k, boolean classicalLogic, boolean PLlogic, boolean dataDrivenApproach, boolean cuttingPlanesPrune, boolean cuttingPlanesRelaxation, boolean initByScout, int saturationMode, boolean megaScout, String algorithm) {
        this.entailmentMode = entailmentMode;
        this.dataDrivenApproach = dataDrivenApproach;
        this.PLlogic = PLlogic;
        this.cuttingPlanesPrune = cuttingPlanesPrune;
        this.cuttingPlanesRelaxation = cuttingPlanesRelaxation;
        this.classicalLogic = classicalLogic;
        this.initByScout = initByScout;
        this.k = k;
        this.saturationMode = saturationMode;
        this.megaScout = megaScout;
        this.algorithm = algorithm;
    }

    public int k() {
        return this.k;
    }

    public int entailmentMode() {
        return entailmentMode;
    }

    public boolean usePossLogic() {
        return PLlogic;
    }

    public boolean useClassLogic() {
        return classicalLogic;
    }

    public boolean isDataDrivenApproach() {
        return dataDrivenApproach; // aka going only throught subset with bigger mask than parents
    }

    public boolean initialByScout() {
        return initByScout;
    }

    public boolean cuttingPlanesRelaxation() {
        // returns true iff literals are not tested for entailment
        return cuttingPlanesRelaxation;
    }

    public boolean useScout() { // cutting planes scout for minimilization of SAT calls
        // returns true iff nodes entailed literals are tested only iff scout is used
        return cuttingPlanesPrune;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public boolean useMegaScout() {
        // should prune the search space
        return this.megaScout;
    }


    public int saturationMode() {
        return saturationMode;
    }

    public String canon() {
        Map<String, String> setting = new HashMap<>();
        setting.put("k", k + "");
        if (Entailment.K_ENTAILMENT == entailmentMode) {
            setting.put("em", "K");
        } else if (Entailment.V_ENTAILMENT == entailmentMode) {
            setting.put("em", "V");
        } else if (Entailment.ONE_CHAIN_ENTAILMENT == entailmentMode) {
            setting.put("em", "O");
        } else if (Entailment.ONE_STEP == entailmentMode) {
            setting.put("em", "OS");
        } else if (Entailment.All_EVIDENCE_WITHOUT_CONSTRAINTS == entailmentMode) {
            setting.put("em", "AWC");
        } else if (Entailment.K_WITHOUT_CONSTRAINTS == entailmentMode) {
            setting.put("em", "KWC");
        } else if (Entailment.K_CONSTRAINTS_FILTER == entailmentMode) {
            setting.put("em", "kf");
        } else if (Entailment.All_EVIDENCE_CONSTRAINTS_FILTER == entailmentMode) {
            setting.put("em", "Af");
        } else if (Entailment.All_EVIDENCE == entailmentMode) {
            setting.put("em", "A");
        } else if (Entailment.R_ENTAILMENT == entailmentMode) {
            setting.put("em", "R");
        }
        setting.put("l", useClassLogic() ? "c" : "PL");
        setting.put("is", initialByScout() ? "t" : "f");
        setting.put("dd", isDataDrivenApproach() ? "t" : "f");
        setting.put("cpr", cuttingPlanesRelaxation() ? "t" : "f");
        setting.put("scout", useScout() ? "t" : "f");
        setting.put("ms", useMegaScout() ? "t" : "f");
        setting.put("alg", "" + this.algorithm);
        String satVal;
        switch (saturationMode) {
            case Entailment.SYMMETRY_SATURATION:
                satVal = "sym";
                break;
            case Entailment.FULL_SATURATION:
                satVal = "full";
                break;
            case Entailment.NONE_SATURATION:
            default:
                satVal = "none";
                break;
        }
        setting.put("sat", satVal);
        // em entailment mode
        // l logic
        // is initial by scout
        // dd data driven
        // cpr cutting planes relaxation
        // scout
        // algorithm
        List<String> keys = Sugar.list("k", "em", "l", "is", "dd", "cpr", "scout", "sat", "ms", "alg");
        return keys.stream().map(key -> key + "-" + setting.get(key)).collect(Collectors.joining("_"));
    }


    public static EntailmentSetting create(int entailmentMode, int k, boolean classicalLogic, boolean PLlogic, boolean dataDrivenApproach, boolean cuttingPlanesPrune, boolean cuttingPlanesRelaxation, boolean initByScout, int saturationMode, boolean megaScout, String algorithm) {
        return new EntailmentSetting(entailmentMode, k, classicalLogic, PLlogic, dataDrivenApproach, cuttingPlanesPrune, cuttingPlanesRelaxation, initByScout, saturationMode, megaScout, algorithm);
    }

    public static EntailmentSetting create() {
        String mode = System.getProperty("ida.pacReasoning.entailment.mode", "").toLowerCase();
        Integer kVal = Integer.parseInt(System.getProperty("ida.pacReasoning.entailment.k", "0"));
        int modeVal;
        if ("k".equals(mode) || "ke".equals(mode)) {
            modeVal = Entailment.K_ENTAILMENT;
        } else if ("ve".equals(mode) || "v".equals(mode) || "voting".equals(mode)) {
            modeVal = Entailment.V_ENTAILMENT;
        } else if ("allevidence".equals(mode) || "ae".equals(mode) || "a".equals(mode)) {
            modeVal = Entailment.All_EVIDENCE;
            kVal = 0;
        } else if ("r".equals(mode)) {
            modeVal = Entailment.R_ENTAILMENT;
            kVal = 0;
        } else if ("one".equals(mode) || "o".equals(mode)) {
            modeVal = Entailment.ONE_CHAIN_ENTAILMENT;
            kVal = 0;
        } else if ("oneS".equals(mode) || "ones".equals(mode) || "os".equals(mode)) {
            modeVal = Entailment.ONE_STEP;
            kVal = 0;
        } else if ("awc".equals(mode) || "withoutconstraints".equals(mode)) {
            modeVal = Entailment.All_EVIDENCE_WITHOUT_CONSTRAINTS;
            kVal = 0;
        } else if ("kwc".equals(mode) || "kwithoutconstraints".equals(mode)) {
            modeVal = Entailment.K_WITHOUT_CONSTRAINTS;
        } else if ("kf".equals(mode) || "kconstriantsfilter".equals(mode)) {
            modeVal = Entailment.K_CONSTRAINTS_FILTER;
        } else if ("af".equals(mode) || "allevidenceconstraintsfilter".equals(mode)) {
            modeVal = Entailment.All_EVIDENCE_CONSTRAINTS_FILTER;
            kVal = 0;
        } else {
            throw new IllegalArgumentException("unknown type of mode\t'" + mode + "'");
        }
        boolean classLogic = false;
        boolean plLogic = false;

        String logic = System.getProperty("ida.pacReasoning.entailment.logic", "").toLowerCase();
        if (logic.equals("") || "classical".equals(logic)) {
            classLogic = true;
        } else if ("pl".equals(logic) || "possibilistic".equals(logic) || "poss".equals(logic)) {
            plLogic = true;
        } else {
            throw new IllegalArgumentException("unknown type of logic");
        }

        boolean dd = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.entailment.dataDriven", "false"));
        // aka scout / useScout
        boolean cpPrune = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.entailment.cpPrune", "false"));
        boolean megaScout = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.entailment.megaScout", "false"));
        boolean cpRelaxation = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.entailment.cpRelaxation", "false"));
        boolean scoutInitialization = Boolean.parseBoolean(System.getProperty("ida.pacReasoning.entailment.scoutInit", "false"));

        String satMode = System.getProperty("ida.pacReasoning.entailment.saturation", "none").toLowerCase();
        int saturationVal;
        if ("none".equals(satMode) || "".equals(satMode)) {
            saturationVal = Entailment.NONE_SATURATION;
        } else if ("sym".equals(satMode) || "symmetry".equals(satMode)) {
            saturationVal = Entailment.SYMMETRY_SATURATION;
            System.out.println("symmetry saturation: interaction/2 implemented only right now due to speed-up");
        } else if ("full".equals(satMode) || "sat".equals(satMode) || "saturation".equals(satMode)) {
            saturationVal = Entailment.FULL_SATURATION;
        } else {
            throw new IllegalArgumentException("unknown type of mode");
        }

        String algorithm = System.getProperty("ida.pacReasoning.entailment.algorithm");

        return create(modeVal, kVal, classLogic, plLogic, dd, cpPrune, cpRelaxation, scoutInitialization, saturationVal, megaScout, algorithm);
    }

}
