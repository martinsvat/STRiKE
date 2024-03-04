package ida.pacReasoning;

import ida.ilp.logic.*;
import ida.pacReasoning.entailment.collectors.VECollector;
import ida.pacReasoning.entailment.theories.Possibilistic;
import ida.pacReasoning.evaluation.Utils;
import ida.utils.Sugar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 3. 4. 2019.
 */
public class Debug {
    public static void main(String[] args) throws IOException {
        //clauseEquals();
//        firstDifferent();
//        hashMapIterationModification();
//        testMinimals();
//        testSoudness();
//        checkFolder();
//        compareTwoFolders();
        //handCheck();
//        writeOutDifferences();
        timeTelling();
    }

    private static void timeTelling() throws IOException {
        Utils u = Utils.create();
        List<String> data = Sugar.list("timeSubL_CurOpt5","timeSubCS_CurOpt5","timeSubMyMinSort_CurOpt5", "timeSubMyBitset_CurOpt5", "timeSub_CurOpt5", "timeSub_AllOpt5", "timeSub_APOAllOpt5", "time_CurOpt5", "time_AllOpt5", "time_APOAllOpt5");
//        List<String> data = Sugar.list("time_CurOpt5", "time_AllOpt5", "time_APOAllOpt5");

        StringBuilder result = new StringBuilder();
        int atMost = 490;
        for (String where : data) {
            Path folder = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + where);
            List<Long> times = Files.list(folder).filter(f -> u.evidenceFileNumber(f) <= atMost && f.toFile().toString().endsWith(".time")).sorted(Comparator.comparing(Utils::evidenceFileNumber)).
                    map(f -> {
                        try {
                            return Files.lines(f).filter(l -> l.trim().length() > 0).mapToLong(Long::parseLong).limit(1).max().orElseThrow(IllegalStateException::new);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        throw new IllegalStateException();
                    })
                    .collect(Collectors.toList());
            if (times.size() != atMost) {
                System.out.println("not all times are here\t" + where + "\tonly " + times.size() + " instead of " + atMost);
            }
            double average = (times.stream().mapToLong(l -> l).sum() * 1.0) / times.size();
            result.append("\n" + where + "\t" + (average / 1_000_000_000));
        }


        System.out.println(result);
    }

    private static void writeOutDifferences() throws IOException {
        String reference = "reference_burteforce";
        String testing = "39_280debug_Opt";
//        String testing = "dev_AllOpt5";
        int idx = 300;
        Path entailedSrcReference = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + reference + "\\queries" + idx + ".db.mp");
        Path entailedSrcTesting = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + testing + "\\queries" + idx + ".db.mp");
        MP refMP = MP.parse(entailedSrcReference);
        MP testMP = MP.parse(entailedSrcTesting);

        List<Literal> lits = refMP.returnThoseMissinInAtLeastOne(testMP);

        lits.stream().map(Literal::toString).sorted().forEach(l -> System.out.println("\t" + l));

        System.out.println("Set<String> lits = Sugar.set(" + lits.stream().map(Literal::toString).sorted().map(l -> "\"" + l + "\"").collect(Collectors.joining(", ")) + ");");
    }

    private static void handCheck() {
        Path rulePath = Paths.get(".", "pac2", "umlsSpeedupTest", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.mc0.59.ptt");
        Collection<Clause> rules = Possibilistic.create(rulePath).allRules().stream()
                .filter(LogicUtils::isDefiniteRule)
                .map(LogicUtils::untype)
                .collect(Collectors.toList());

    }

    private static void compareTwoFolders() throws IOException {
        System.out.println("this test assumes that both of the reference and testing folders were teste by checkFolder on minimality and correct derrivations");
        Path rulePath = Paths.get(".", "pac2", "umlsSpeedupTest", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.mc0.59.ptt");
        Collection<Clause> rules = Possibilistic.create(rulePath).allRules().stream()
                .filter(LogicUtils::isDefiniteRule)
                .map(LogicUtils::untype)
                .collect(Collectors.toList());
//        String reference = "reference_burteforce";
//        String testing = "dev_AllOpt5";
//        String testing = "dev_CurOpt5";
//        String testing = "fixdev_CurOpt5";
        //String testing = "39debug_CurOpt5";
//        String testing = "test_allprev_ALLOpt5";
        String reference = "time_CurOpt5";
        String testing = "timeSub_CurOpt5";


        System.out.println("reference is\t" + reference);
        System.out.println("testing is\t" + testing);

        int k = 5;
        //for (int idx = 1; idx <= 300; idx++) {
        for (int idx = 10; idx <= 300; idx += 10) {
//        int idx = 39;{
            System.out.println("idx\t" + idx);
            Path entailedSrcReference = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + reference + "\\queries" + idx + ".db");
            Path entailedSrcTesting = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + testing + "\\queries" + idx + ".db");

            if (!entailedSrcReference.toFile().exists() || !entailedSrcTesting.toFile().exists()) {
                System.out.println("this data do not exist\t" + idx + "\n\t" + entailedSrcReference.toFile().exists() + "\t" + entailedSrcReference + "\n\t" + entailedSrcTesting.toFile().exists() + "\t" + entailedSrcTesting);
                System.exit((-78547));
            }

            Path minimalProofsSrcReference = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + reference + "\\queries" + idx + ".db.mp");
            Path minimalProofsSrcTesting = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + testing + "\\queries" + idx + ".db.mp");
            Path dataSrc = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\umls\\test-uniform\\queries" + idx + ".db");
            Set<Literal> data = LogicUtils.untype(LogicUtils.loadEvidence(dataSrc));
            VECollector entailedReference = VECollector.load(entailedSrcReference, k).untype();
            VECollector entailedTesting = VECollector.load(entailedSrcTesting, k).untype();
            boolean someError = false;
            if (!entailedReference.asOutput().equals(entailedTesting.asOutput())) {
                System.out.println("these two do not have the same entailed output");
                System.out.println("reference\n'" + entailedReference.asOutput() + "'\ntesting\n'" + entailedTesting.asOutput() + "'");
                someError = true;
            }

            MP mpReference = MP.parse(minimalProofsSrcReference);
            MP mpTesting = MP.parse(minimalProofsSrcTesting);
            if (mpReference.isNotSameAs(mpTesting)) {
                someError = true;
            }

            if (someError) {
                System.out.println("\nexiting at query\t" + idx);
                System.exit(-7789);
            }
        }


    }

    //pro checking minimalnich  dukazu vybrat
    private static void checkFolder() throws IOException {
        Path rulePath = Paths.get(".", "pac2", "umlsSpeedupTest", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.mc0.59.ptt");
        Collection<Clause> rules = Possibilistic.create(rulePath).allRules().stream()
                .filter(LogicUtils::isDefiniteRule)
                .map(LogicUtils::untype)
                .collect(Collectors.toList());

        String which = "reference_burteforce";
//        String which = "39_280debug_Opt";
//        String which = "dev_AllOpt5";
        //String which = "dev_CurOpt5";
//        String which = "debugDebug";
//        String which = "test_allprev_ALLOpt5";


        System.out.println("target folder\t" + which);

        int k = 5;
        for (int idx = 1; idx <= 221; idx++) {
//        int idx = 280;
//        int idx = 10;
//        {
            System.out.println("idx\t" + idx);
            Path entailedSrc = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + which + "\\queries" + idx + ".db");

            if (!entailedSrc.toFile().exists()) {
                System.out.println("this data do not exist\t" + idx + "\t" + entailedSrc);
                System.exit((-7854));
            }

            Path minimalProofsSrc = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\" + which + "\\queries" + idx + ".db.mp");
            Path dataSrc = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\umls\\test-uniform\\queries" + idx + ".db");
            Set<Literal> data = LogicUtils.untype(LogicUtils.loadEvidence(dataSrc));
            //Set<String> interesting = Sugar.set("co-occurs_with(clinical_Attribute, human_caused_Phenomenon_or_Process)", "co-occurs_with(human_caused_Phenomenon_or_Process, clinical_Attribute)", "exhibits(disease_or_Syndrome, behavior)", "exhibits(disease_or_Syndrome, social_Behavior)", "exhibits(mental_Process, behavior)", "exhibits(mental_Process, social_Behavior)", "exhibits(organism_Function, behavior)", "exhibits(organism_Function, social_Behavior)", "measurement_of(clinical_Attribute, human_caused_Phenomenon_or_Process)", "measurement_of(human_caused_Phenomenon_or_Process, clinical_Attribute)", "performs(disease_or_Syndrome, behavior)", "performs(disease_or_Syndrome, social_Behavior)", "performs(mental_Process, behavior)", "performs(mental_Process, social_Behavior)", "performs(organism_Function, behavior)", "performs(organism_Function, social_Behavior)", "property_of(clinical_Attribute, human_caused_Phenomenon_or_Process)", "property_of(human_caused_Phenomenon_or_Process, clinical_Attribute)", "uses(disease_or_Syndrome, drug_Delivery_Device)", "uses(disease_or_Syndrome, regulation_or_Law)", "uses(mental_Process, drug_Delivery_Device)", "uses(mental_Process, regulation_or_Law)", "uses(organism_Function, drug_Delivery_Device)", "uses(organism_Function, regulation_or_Law)");
//            Set<String> interesting = Sugar.set("affects(cell_or_Molecular_Dysfunction, anatomical_Abnormality)", "affects(cell_or_Molecular_Dysfunction, clinical_Attribute)", "affects(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "affects(cell_or_Molecular_Dysfunction, human_caused_Phenomenon_or_Process)", "affects(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "affects(cell_or_Molecular_Dysfunction, mental_or_Behavioral_Dysfunction)", "affects(cell_or_Molecular_Dysfunction, natural_Phenomenon_or_Process)", "affects(clinical_Attribute, cell_or_Molecular_Dysfunction)", "affects(clinical_Attribute, disease_or_Syndrome)", "affects(clinical_Attribute, injury_or_Poisoning)", "affects(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "affects(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "affects(disease_or_Syndrome, clinical_Attribute)", "affects(disease_or_Syndrome, injury_or_Poisoning)", "affects(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "affects(human_caused_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "affects(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "affects(injury_or_Poisoning, disease_or_Syndrome)", "affects(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "affects(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "affects(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "associated_with(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "associated_with(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "associated_with(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "co-occurs_with(cell_Function, pathologic_Function)", "co-occurs_with(cell_or_Molecular_Dysfunction, clinical_Attribute)", "co-occurs_with(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "co-occurs_with(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "co-occurs_with(cell_or_Molecular_Dysfunction, mental_or_Behavioral_Dysfunction)", "co-occurs_with(cell_or_Molecular_Dysfunction, natural_Phenomenon_or_Process)", "co-occurs_with(clinical_Attribute, cell_or_Molecular_Dysfunction)", "co-occurs_with(clinical_Attribute, disease_or_Syndrome)", "co-occurs_with(clinical_Attribute, human_caused_Phenomenon_or_Process)", "co-occurs_with(clinical_Attribute, injury_or_Poisoning)", "co-occurs_with(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "co-occurs_with(disease_or_Syndrome, clinical_Attribute)", "co-occurs_with(disease_or_Syndrome, human_caused_Phenomenon_or_Process)", "co-occurs_with(disease_or_Syndrome, injury_or_Poisoning)", "co-occurs_with(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "co-occurs_with(disease_or_Syndrome, natural_Phenomenon_or_Process)", "co-occurs_with(human_caused_Phenomenon_or_Process, clinical_Attribute)", "co-occurs_with(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "co-occurs_with(injury_or_Poisoning, clinical_Attribute)", "co-occurs_with(injury_or_Poisoning, disease_or_Syndrome)", "co-occurs_with(injury_or_Poisoning, mental_or_Behavioral_Dysfunction)", "co-occurs_with(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "co-occurs_with(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "co-occurs_with(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "co-occurs_with(mental_or_Behavioral_Dysfunction, injury_or_Poisoning)", "co-occurs_with(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "co-occurs_with(natural_Phenomenon_or_Process, clinical_Attribute)", "co-occurs_with(natural_Phenomenon_or_Process, disease_or_Syndrome)", "co-occurs_with(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "co-occurs_with(organ_or_Tissue_Function, clinical_Attribute)", "co-occurs_with(pathologic_Function, cell_Function)", "co-occurs_with(pathologic_Function, clinical_Attribute)", "co-occurs_with(pathologic_Function, organism_Function)", "complicates(cell_or_Molecular_Dysfunction, clinical_Attribute)", "complicates(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "complicates(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "complicates(cell_or_Molecular_Dysfunction, mental_or_Behavioral_Dysfunction)", "complicates(cell_or_Molecular_Dysfunction, natural_Phenomenon_or_Process)", "complicates(clinical_Attribute, cell_or_Molecular_Dysfunction)", "complicates(clinical_Attribute, disease_or_Syndrome)", "complicates(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "complicates(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "complicates(disease_or_Syndrome, clinical_Attribute)", "complicates(disease_or_Syndrome, human_caused_Phenomenon_or_Process)", "complicates(disease_or_Syndrome, injury_or_Poisoning)", "complicates(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "complicates(disease_or_Syndrome, natural_Phenomenon_or_Process)", "complicates(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "complicates(injury_or_Poisoning, clinical_Attribute)", "complicates(injury_or_Poisoning, disease_or_Syndrome)", "complicates(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "complicates(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "complicates(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "complicates(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "complicates(natural_Phenomenon_or_Process, clinical_Attribute)", "complicates(natural_Phenomenon_or_Process, disease_or_Syndrome)", "complicates(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "complicates(pathologic_Function, cell_Function)", "complicates(pathologic_Function, clinical_Attribute)", "complicates(pathologic_Function, organism_Function)", "degree_of(cell_Function, pathologic_Function)", "degree_of(cell_or_Molecular_Dysfunction, clinical_Attribute)", "degree_of(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "degree_of(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "degree_of(cell_or_Molecular_Dysfunction, mental_or_Behavioral_Dysfunction)", "degree_of(cell_or_Molecular_Dysfunction, natural_Phenomenon_or_Process)", "degree_of(clinical_Attribute, cell_or_Molecular_Dysfunction)", "degree_of(clinical_Attribute, disease_or_Syndrome)", "degree_of(clinical_Attribute, injury_or_Poisoning)", "degree_of(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "degree_of(clinical_Attribute, natural_Phenomenon_or_Process)", "degree_of(clinical_Attribute, pathologic_Function)", "degree_of(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "degree_of(disease_or_Syndrome, clinical_Attribute)", "degree_of(disease_or_Syndrome, human_caused_Phenomenon_or_Process)", "degree_of(disease_or_Syndrome, injury_or_Poisoning)", "degree_of(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "degree_of(disease_or_Syndrome, natural_Phenomenon_or_Process)", "degree_of(human_caused_Phenomenon_or_Process, disease_or_Syndrome)", "degree_of(human_caused_Phenomenon_or_Process, natural_Phenomenon_or_Process)", "degree_of(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "degree_of(injury_or_Poisoning, clinical_Attribute)", "degree_of(injury_or_Poisoning, disease_or_Syndrome)", "degree_of(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "degree_of(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "degree_of(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "degree_of(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "degree_of(natural_Phenomenon_or_Process, clinical_Attribute)", "degree_of(natural_Phenomenon_or_Process, disease_or_Syndrome)", "degree_of(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "degree_of(organism_Function, pathologic_Function)", "degree_of(pathologic_Function, cell_Function)", "degree_of(pathologic_Function, clinical_Attribute)", "degree_of(pathologic_Function, organism_Function)", "diagnoses(cell_or_Molecular_Dysfunction, clinical_Attribute)", "diagnoses(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "diagnoses(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "diagnoses(cell_or_Molecular_Dysfunction, natural_Phenomenon_or_Process)", "diagnoses(clinical_Attribute, cell_or_Molecular_Dysfunction)", "diagnoses(clinical_Attribute, disease_or_Syndrome)", "diagnoses(clinical_Attribute, injury_or_Poisoning)", "diagnoses(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "diagnoses(clinical_Attribute, natural_Phenomenon_or_Process)", "diagnoses(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "diagnoses(disease_or_Syndrome, natural_Phenomenon_or_Process)", "diagnoses(human_caused_Phenomenon_or_Process, disease_or_Syndrome)", "diagnoses(human_caused_Phenomenon_or_Process, natural_Phenomenon_or_Process)", "diagnoses(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "diagnoses(injury_or_Poisoning, clinical_Attribute)", "diagnoses(injury_or_Poisoning, disease_or_Syndrome)", "diagnoses(injury_or_Poisoning, mental_or_Behavioral_Dysfunction)", "diagnoses(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "diagnoses(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "diagnoses(mental_or_Behavioral_Dysfunction, injury_or_Poisoning)", "diagnoses(natural_Phenomenon_or_Process, clinical_Attribute)", "diagnoses(natural_Phenomenon_or_Process, disease_or_Syndrome)", "diagnoses(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "exhibits(disease_or_Syndrome, behavior)", "exhibits(disease_or_Syndrome, social_Behavior)", "exhibits(mental_Process, behavior)", "exhibits(mental_Process, social_Behavior)", "exhibits(organism_Function, behavior)", "exhibits(organism_Function, social_Behavior)", "location_of(body_Location_or_Region, disease_or_Syndrome)", "location_of(embryonic_Structure, disease_or_Syndrome)", "manifestation_of(cell_Function, clinical_Attribute)", "manifestation_of(cell_or_Molecular_Dysfunction, clinical_Attribute)", "manifestation_of(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "manifestation_of(clinical_Attribute, cell_or_Molecular_Dysfunction)", "manifestation_of(clinical_Attribute, disease_or_Syndrome)", "manifestation_of(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "manifestation_of(clinical_Attribute, natural_Phenomenon_or_Process)", "manifestation_of(clinical_Attribute, pathologic_Function)", "manifestation_of(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "manifestation_of(disease_or_Syndrome, clinical_Attribute)", "manifestation_of(disease_or_Syndrome, human_caused_Phenomenon_or_Process)", "manifestation_of(disease_or_Syndrome, injury_or_Poisoning)", "manifestation_of(disease_or_Syndrome, natural_Phenomenon_or_Process)", "manifestation_of(human_caused_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "manifestation_of(human_caused_Phenomenon_or_Process, disease_or_Syndrome)", "manifestation_of(human_caused_Phenomenon_or_Process, natural_Phenomenon_or_Process)", "manifestation_of(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "manifestation_of(injury_or_Poisoning, clinical_Attribute)", "manifestation_of(injury_or_Poisoning, disease_or_Syndrome)", "manifestation_of(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "manifestation_of(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "manifestation_of(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "manifestation_of(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "manifestation_of(natural_Phenomenon_or_Process, clinical_Attribute)", "manifestation_of(natural_Phenomenon_or_Process, disease_or_Syndrome)", "manifestation_of(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "measurement_of(cell_Function, pathologic_Function)", "measurement_of(cell_or_Molecular_Dysfunction, clinical_Attribute)", "measurement_of(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "measurement_of(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "measurement_of(clinical_Attribute, cell_or_Molecular_Dysfunction)", "measurement_of(clinical_Attribute, disease_or_Syndrome)", "measurement_of(clinical_Attribute, human_caused_Phenomenon_or_Process)", "measurement_of(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "measurement_of(clinical_Attribute, natural_Phenomenon_or_Process)", "measurement_of(clinical_Attribute, pathologic_Function)", "measurement_of(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "measurement_of(disease_or_Syndrome, clinical_Attribute)", "measurement_of(disease_or_Syndrome, human_caused_Phenomenon_or_Process)", "measurement_of(disease_or_Syndrome, injury_or_Poisoning)", "measurement_of(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "measurement_of(disease_or_Syndrome, natural_Phenomenon_or_Process)", "measurement_of(human_caused_Phenomenon_or_Process, clinical_Attribute)", "measurement_of(human_caused_Phenomenon_or_Process, disease_or_Syndrome)", "measurement_of(human_caused_Phenomenon_or_Process, natural_Phenomenon_or_Process)", "measurement_of(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "measurement_of(injury_or_Poisoning, clinical_Attribute)", "measurement_of(injury_or_Poisoning, disease_or_Syndrome)", "measurement_of(injury_or_Poisoning, mental_or_Behavioral_Dysfunction)", "measurement_of(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "measurement_of(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "measurement_of(mental_or_Behavioral_Dysfunction, injury_or_Poisoning)", "measurement_of(natural_Phenomenon_or_Process, clinical_Attribute)", "measurement_of(natural_Phenomenon_or_Process, disease_or_Syndrome)", "measurement_of(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "measurement_of(organism_Function, pathologic_Function)", "occurs_in(anatomical_Abnormality, disease_or_Syndrome)", "occurs_in(biologic_Function, disease_or_Syndrome)", "occurs_in(cell_Function, clinical_Attribute)", "occurs_in(cell_Function, mental_or_Behavioral_Dysfunction)", "occurs_in(cell_Function, pathologic_Function)", "occurs_in(cell_or_Molecular_Dysfunction, clinical_Attribute)", "occurs_in(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "occurs_in(cell_or_Molecular_Dysfunction, mental_or_Behavioral_Dysfunction)", "occurs_in(cell_or_Molecular_Dysfunction, natural_Phenomenon_or_Process)", "occurs_in(clinical_Attribute, cell_or_Molecular_Dysfunction)", "occurs_in(clinical_Attribute, disease_or_Syndrome)", "occurs_in(clinical_Attribute, injury_or_Poisoning)", "occurs_in(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "occurs_in(clinical_Attribute, natural_Phenomenon_or_Process)", "occurs_in(clinical_Attribute, pathologic_Function)", "occurs_in(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "occurs_in(disease_or_Syndrome, clinical_Attribute)", "occurs_in(disease_or_Syndrome, injury_or_Poisoning)", "occurs_in(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "occurs_in(disease_or_Syndrome, natural_Phenomenon_or_Process)", "occurs_in(human_caused_Phenomenon_or_Process, disease_or_Syndrome)", "occurs_in(human_caused_Phenomenon_or_Process, natural_Phenomenon_or_Process)", "occurs_in(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "occurs_in(injury_or_Poisoning, clinical_Attribute)", "occurs_in(injury_or_Poisoning, disease_or_Syndrome)", "occurs_in(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "occurs_in(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "occurs_in(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "occurs_in(mental_or_Behavioral_Dysfunction, natural_Phenomenon_or_Process)", "occurs_in(mental_or_Behavioral_Dysfunction, pathologic_Function)", "occurs_in(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "occurs_in(natural_Phenomenon_or_Process, disease_or_Syndrome)", "occurs_in(organ_or_Tissue_Function, disease_or_Syndrome)", "occurs_in(organ_or_Tissue_Function, natural_Phenomenon_or_Process)", "occurs_in(organism_Function, mental_or_Behavioral_Dysfunction)", "occurs_in(organism_Function, pathologic_Function)", "occurs_in(pathologic_Function, clinical_Attribute)", "occurs_in(pathologic_Function, disease_or_Syndrome)", "occurs_in(pathologic_Function, organism_Function)", "occurs_in(physiologic_Function, mental_Process)", "occurs_in(physiologic_Function, organism_Function)", "performs(disease_or_Syndrome, behavior)", "performs(disease_or_Syndrome, social_Behavior)", "performs(mental_Process, behavior)", "performs(mental_Process, social_Behavior)", "performs(organism_Function, behavior)", "performs(organism_Function, social_Behavior)", "precedes(anatomical_Abnormality, cell_or_Molecular_Dysfunction)", "precedes(biologic_Function, disease_or_Syndrome)", "precedes(cell_Function, clinical_Attribute)", "precedes(cell_Function, pathologic_Function)", "precedes(cell_or_Molecular_Dysfunction, anatomical_Abnormality)", "precedes(cell_or_Molecular_Dysfunction, clinical_Attribute)", "precedes(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "precedes(cell_or_Molecular_Dysfunction, fungus)", "precedes(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "precedes(cell_or_Molecular_Dysfunction, mental_Process)", "precedes(cell_or_Molecular_Dysfunction, mental_or_Behavioral_Dysfunction)", "precedes(cell_or_Molecular_Dysfunction, natural_Phenomenon_or_Process)", "precedes(cell_or_Molecular_Dysfunction, virus)", "precedes(clinical_Attribute, cell_Function)", "precedes(clinical_Attribute, cell_or_Molecular_Dysfunction)", "precedes(clinical_Attribute, disease_or_Syndrome)", "precedes(clinical_Attribute, injury_or_Poisoning)", "precedes(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "precedes(clinical_Attribute, natural_Phenomenon_or_Process)", "precedes(clinical_Attribute, pathologic_Function)", "precedes(disease_or_Syndrome, biologic_Function)", "precedes(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "precedes(disease_or_Syndrome, clinical_Attribute)", "precedes(disease_or_Syndrome, human_caused_Phenomenon_or_Process)", "precedes(disease_or_Syndrome, injury_or_Poisoning)", "precedes(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "precedes(disease_or_Syndrome, natural_Phenomenon_or_Process)", "precedes(disease_or_Syndrome, organ_or_Tissue_Function)", "precedes(fungus, cell_or_Molecular_Dysfunction)", "precedes(human_caused_Phenomenon_or_Process, disease_or_Syndrome)", "precedes(human_caused_Phenomenon_or_Process, natural_Phenomenon_or_Process)", "precedes(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "precedes(injury_or_Poisoning, clinical_Attribute)", "precedes(injury_or_Poisoning, disease_or_Syndrome)", "precedes(mental_Process, cell_or_Molecular_Dysfunction)", "precedes(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "precedes(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "precedes(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "precedes(mental_or_Behavioral_Dysfunction, natural_Phenomenon_or_Process)", "precedes(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "precedes(natural_Phenomenon_or_Process, clinical_Attribute)", "precedes(natural_Phenomenon_or_Process, disease_or_Syndrome)", "precedes(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "precedes(natural_Phenomenon_or_Process, mental_or_Behavioral_Dysfunction)", "precedes(natural_Phenomenon_or_Process, organ_or_Tissue_Function)", "precedes(organ_or_Tissue_Function, disease_or_Syndrome)", "precedes(organ_or_Tissue_Function, natural_Phenomenon_or_Process)", "precedes(organism_Function, pathologic_Function)", "precedes(pathologic_Function, cell_Function)", "precedes(pathologic_Function, clinical_Attribute)", "precedes(pathologic_Function, organism_Function)", "precedes(virus, cell_or_Molecular_Dysfunction)", "process_of(anatomical_Abnormality, clinical_Attribute)", "process_of(anatomical_Abnormality, disease_or_Syndrome)", "process_of(cell_or_Molecular_Dysfunction, clinical_Attribute)", "process_of(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "process_of(cell_or_Molecular_Dysfunction, human_caused_Phenomenon_or_Process)", "process_of(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "process_of(cell_or_Molecular_Dysfunction, mental_or_Behavioral_Dysfunction)", "process_of(cell_or_Molecular_Dysfunction, natural_Phenomenon_or_Process)", "process_of(clinical_Attribute, cell_or_Molecular_Dysfunction)", "process_of(clinical_Attribute, disease_or_Syndrome)", "process_of(clinical_Attribute, injury_or_Poisoning)", "process_of(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "process_of(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "process_of(disease_or_Syndrome, clinical_Attribute)", "process_of(disease_or_Syndrome, human_caused_Phenomenon_or_Process)", "process_of(disease_or_Syndrome, injury_or_Poisoning)", "process_of(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "process_of(human_caused_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "process_of(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "process_of(injury_or_Poisoning, disease_or_Syndrome)", "process_of(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "process_of(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "process_of(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "process_of(natural_Phenomenon_or_Process, clinical_Attribute)", "process_of(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "process_of(physiologic_Function, mammal)", "property_of(cell_or_Molecular_Dysfunction, clinical_Attribute)", "property_of(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "property_of(cell_or_Molecular_Dysfunction, injury_or_Poisoning)", "property_of(cell_or_Molecular_Dysfunction, mental_or_Behavioral_Dysfunction)", "property_of(clinical_Attribute, cell_or_Molecular_Dysfunction)", "property_of(clinical_Attribute, disease_or_Syndrome)", "property_of(clinical_Attribute, human_caused_Phenomenon_or_Process)", "property_of(clinical_Attribute, injury_or_Poisoning)", "property_of(clinical_Attribute, mental_or_Behavioral_Dysfunction)", "property_of(clinical_Attribute, natural_Phenomenon_or_Process)", "property_of(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "property_of(disease_or_Syndrome, clinical_Attribute)", "property_of(disease_or_Syndrome, human_caused_Phenomenon_or_Process)", "property_of(disease_or_Syndrome, injury_or_Poisoning)", "property_of(disease_or_Syndrome, mental_or_Behavioral_Dysfunction)", "property_of(disease_or_Syndrome, natural_Phenomenon_or_Process)", "property_of(human_caused_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "property_of(human_caused_Phenomenon_or_Process, clinical_Attribute)", "property_of(human_caused_Phenomenon_or_Process, disease_or_Syndrome)", "property_of(human_caused_Phenomenon_or_Process, natural_Phenomenon_or_Process)", "property_of(injury_or_Poisoning, cell_or_Molecular_Dysfunction)", "property_of(injury_or_Poisoning, clinical_Attribute)", "property_of(injury_or_Poisoning, disease_or_Syndrome)", "property_of(injury_or_Poisoning, mental_or_Behavioral_Dysfunction)", "property_of(injury_or_Poisoning, natural_Phenomenon_or_Process)", "property_of(injury_or_Poisoning, plant)", "property_of(mental_or_Behavioral_Dysfunction, cell_or_Molecular_Dysfunction)", "property_of(mental_or_Behavioral_Dysfunction, clinical_Attribute)", "property_of(mental_or_Behavioral_Dysfunction, disease_or_Syndrome)", "property_of(mental_or_Behavioral_Dysfunction, injury_or_Poisoning)", "property_of(natural_Phenomenon_or_Process, cell_or_Molecular_Dysfunction)", "property_of(natural_Phenomenon_or_Process, clinical_Attribute)", "property_of(natural_Phenomenon_or_Process, disease_or_Syndrome)", "property_of(natural_Phenomenon_or_Process, human_caused_Phenomenon_or_Process)", "property_of(organ_or_Tissue_Function, natural_Phenomenon_or_Process)", "property_of(organism_Function, cell_Function)", "property_of(pathologic_Function, clinical_Attribute)", "result_of(cell_or_Molecular_Dysfunction, disease_or_Syndrome)", "result_of(clinical_Attribute, disease_or_Syndrome)", "result_of(disease_or_Syndrome, cell_or_Molecular_Dysfunction)", "result_of(disease_or_Syndrome, clinical_Attribute)", "result_of(disease_or_Syndrome, natural_Phenomenon_or_Process)", "result_of(natural_Phenomenon_or_Process, disease_or_Syndrome)", "uses(disease_or_Syndrome, drug_Delivery_Device)", "uses(disease_or_Syndrome, regulation_or_Law)", "uses(mental_Process, drug_Delivery_Device)", "uses(mental_Process, regulation_or_Law)", "uses(organism_Function, drug_Delivery_Device)", "uses(organism_Function, regulation_or_Law)");

//            MP mp = MP.parse(minimalProofsSrc,true,(l) -> interesting.contains(l));
            MP mp = MP.parse(minimalProofsSrc);
            VECollector entailed = VECollector.load(entailedSrc, k).untype();
            boolean someError = false;
            if (evidenceNotCorrect(data, entailed)) {
                someError = true;
            }
            System.out.println("checking minimality");
            /**/
            if (mp.isSomeSubsetNonminimal()) {
                someError = true;
            }
/**/
            System.out.println("checking derivations");
            if (mp.isSomeDerivationIncorrect(rules, data)) {
                someError = true;
            }

            if (someError) {
                System.out.println("\nexiting at query\t" + idx + "\t" + entailed);
                System.exit(-7789);
            }
        }
    }


    private static boolean evidenceNotCorrect(Set<Literal> data, VECollector entailed) {
        boolean someError = false;
        Set<Literal> s1 = Sugar.setDifference(data, entailed.getEvidence());
        if (!s1.isEmpty()) {
            someError = true;
            System.out.println("literals that are in evidence given but not in evidence of entailed\t" + s1.size());
            System.out.println(s1.stream().map(Literal::toString).sorted().collect(Collectors.joining(", ")));
        }
        Set<Literal> s2 = Sugar.setDifference(entailed.getEvidence(), data);
        if (!s1.isEmpty()) {
            someError = true;
            System.out.println("literals that are in evidence of the result but not in the evidence of the query\t" + s2.size());
            System.out.println(s2.stream().map(Literal::toString).sorted().collect(Collectors.joining(", ")));
        }
        return someError;
    }

    private static void testSoudness() {
        System.out.println("I expect that these are already minimal ones ;)");

        /*
        List<String> sets = Sugar.list("{1:alga, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:physiologic_Function}",
                "{1:anatomical_Abnormality, 1:cell_Function, 1:cell_or_Molecular_Dysfunction, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:biologic_Function, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:biologic_Function, 1:disease_or_Syndrome, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:cell_Function, 1:cell_or_Molecular_Dysfunction, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:cell_Function, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:vitamin}",
                "{1:cell_Function, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:vitamin}",
                "{1:cell_Function, 1:mental_or_Behavioral_Dysfunction, 1:organism_Function, 1:pathologic_Function, 1:vitamin}",
                "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}",
                "{1:cell_or_Molecular_Dysfunction, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}",
                "{1:cell_or_Molecular_Dysfunction, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:plant}",
                "{1:cell_or_Molecular_Dysfunction, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:cell_or_Molecular_Dysfunction, 1:mental_or_Behavioral_Dysfunction, 1:organism_Function, 1:pathologic_Function, 1:vitamin}",
                "{1:disease_or_Syndrome, 1:embryonic_Structure, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:drug_Delivery_Device, 1:mammal, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}",
                "{1:drug_Delivery_Device, 1:mental_Process, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}",
                "{1:drug_Delivery_Device, 1:mental_Process, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:physiologic_Function}",
                "{1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:organism_Function, 1:pathologic_Function}"
        );
        Literal targetLiteral = Literal.parseLiteral("co-occurs_with(pathologic_Function, mental_or_Behavioral_Dysfunction)");
        Path rulePath = Paths.get(".", "pac2", "umlsSpeedupTest", "debugCut44_src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.mc0.59.ptt");
        */
  /*      List<String> sets = Sugar.list("{1:acquired_Abnormality, 1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:virus}"
                , "{1:acquired_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:virus}"
                , "{1:alga, 1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:alga, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:anatomical_Abnormality, 1:biologic_Function, 1:disease_or_Syndrome, 1:invertebrate, 1:organ_or_Tissue_Function}"
                , "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:invertebrate, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:biomedical_or_Dental_Material, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:biomedical_or_Dental_Material, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:biologic_Function, 1:cell_Function, 1:clinical_Attribute, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:cell_Function, 1:disease_or_Syndrome, 1:food, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:cell_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:biologic_Function, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:chemical_Viewed_Structurally, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:fungus, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:immunologic_Factor, 1:natural_Phenomenon_or_Process, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:invertebrate, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:mammal, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:mental_Process, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function, 1:physiologic_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:pathologic_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function, 1:quantitative_Concept}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:plant}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:vertebrate}"
                , "{1:biomedical_or_Dental_Material, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function}"
                , "{1:biomedical_or_Dental_Material, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:cell_Function, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:chemical_Viewed_Structurally, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:element_Ion_or_Isotope, 1:organ_or_Tissue_Function, 1:pathologic_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:fish, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:food, 1:organ_or_Tissue_Function, 1:pathologic_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:fungus, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:immunologic_Factor, 1:natural_Phenomenon_or_Process, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:invertebrate, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mammal, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_Process, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function, 1:physiologic_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:pathologic_Function, 1:steroid}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function, 1:quantitative_Concept}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:plant}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:vertebrate}"
                , "{1:disease_or_Syndrome, 1:mental_Process, 1:organ_or_Tissue_Function, 1:pathologic_Function, 1:physiologic_Function}"
                , "{1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}");
        Literal targetLiteral = Literal.parseLiteral("affects(organ_or_Tissue_Function, disease_or_Syndrome)");
        Path rulePath = Paths.get(".", "pac2", "umlsSpeedupTest", "debugCut70_src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.mc0.59.ptt");
*/
        //List<String> sets = Sugar.list("{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:environmental_Effect_of_Humans, 1:mental_Process, 1:pathologic_Function}", "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:environmental_Effect_of_Humans, 1:organism_Function, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}");
        List<String> sets = Sugar.list("{1:acquired_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:environmental_Effect_of_Humans, 1:mental_Process, 1:pathologic_Function}", "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:environmental_Effect_of_Humans, 1:organism_Function, 1:pathologic_Function}", "{1:biologic_Function, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:body_Substance, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:clinical_Attribute, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:drug_Delivery_Device, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:enzyme, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:injury_or_Poisoning, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:medical_Device, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:mental_Process, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:natural_Phenomenon_or_Process, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:organ_or_Tissue_Function, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:organism_Function, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function, 1:phenomenon_or_Process}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function, 1:physiologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function, 1:plant}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function, 1:vitamin}");
        Literal targetLiteral = Literal.parseLiteral("co-occurs_with(environmental_Effect_of_Humans, pathologic_Function)");//co-occurs_with(1:environmental_Effect_of_Humans, 1:pathologic_Function)
        Path rulePath = Paths.get(".", "pac2", "umlsSpeedupTest", "src_train.db.typed.uni0.5.db_cDtrue_theory_r5_bs4_d3_mv5_ttrue_c2878.fol.poss.k5.CS.mc0.59.ptt");
        List<Set<Constant>> constants = sets.stream().map(s -> {
            s = s.substring(1, s.length() - 1);
//            System.out.println(s);
            String[] splitted = s.split(",");
            return Arrays.stream(splitted).map(String::trim).map(c -> (Constant) Sugar.chooseOne(LogicUtils.constants(LogicUtils.untype(Clause.parse("p(" + c + ")"))))).collect(Collectors.toSet());
        }).collect(Collectors.toList());

        constants.forEach(c -> System.out.println("\t" + c));

        Set<Literal> evidence = LogicUtils.loadEvidence(Paths.get("..", "datasets", "umls", "test-uniform", "queries140.db"));
        Utils u = Utils.create();
        Collection<Clause> rules = Possibilistic.create(rulePath).allRules().stream()
                .filter(LogicUtils::isDefiniteRule)
                .map(LogicUtils::untype)
                .collect(Collectors.toList());

        rules.forEach(c -> System.out.println("\t" + c));

        for (Set<Constant> subset : constants) {
            Set<Literal> fragment = u.mask(evidence, subset);
            System.out.println(fragment.size() + "\t" + fragment);
            LeastHerbrandModel lhm = new LeastHerbrandModel();
            Set<Literal> model = lhm.herbrandModel(rules, fragment);
            System.out.println(model.size() + "\t" + model.contains(targetLiteral) + "\t" + model);
        }

    }

    private static void testMinimals() {
        /*List<String> sets = Sugar.list("{1:alga, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:physiologic_Function}",
                "{1:anatomical_Abnormality, 1:cell_Function, 1:cell_or_Molecular_Dysfunction, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:biologic_Function, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:biologic_Function, 1:disease_or_Syndrome, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:cell_Function, 1:cell_or_Molecular_Dysfunction, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:cell_Function, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:vitamin}",
                "{1:cell_Function, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:vitamin}",
                "{1:cell_Function, 1:mental_or_Behavioral_Dysfunction, 1:organism_Function, 1:pathologic_Function, 1:vitamin}",
                "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}",
                "{1:cell_or_Molecular_Dysfunction, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}",
                "{1:cell_or_Molecular_Dysfunction, 1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:plant}",
                "{1:cell_or_Molecular_Dysfunction, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:cell_or_Molecular_Dysfunction, 1:mental_or_Behavioral_Dysfunction, 1:organism_Function, 1:pathologic_Function, 1:vitamin}",
                "{1:disease_or_Syndrome, 1:embryonic_Structure, 1:injury_or_Poisoning, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}",
                "{1:drug_Delivery_Device, 1:mammal, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}",
                "{1:drug_Delivery_Device, 1:mental_Process, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}",
                "{1:drug_Delivery_Device, 1:mental_Process, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function, 1:physiologic_Function}",
                "{1:drug_Delivery_Device, 1:mental_or_Behavioral_Dysfunction, 1:organism_Function, 1:pathologic_Function}");
        */
        /*List<String> sets = Sugar.list("{1:acquired_Abnormality, 1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:virus}"
                , "{1:acquired_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:virus}"
                , "{1:alga, 1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:alga, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:anatomical_Abnormality, 1:biologic_Function, 1:disease_or_Syndrome, 1:invertebrate, 1:organ_or_Tissue_Function}"
                , "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:invertebrate, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:biomedical_or_Dental_Material, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:biomedical_or_Dental_Material, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:biologic_Function, 1:cell_Function, 1:clinical_Attribute, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:cell_Function, 1:disease_or_Syndrome, 1:food, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:cell_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:biologic_Function, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:chemical_Viewed_Structurally, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:fungus, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:immunologic_Factor, 1:natural_Phenomenon_or_Process, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:invertebrate, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:mammal, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:mental_Process, 1:organ_or_Tissue_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function, 1:physiologic_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:pathologic_Function}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function, 1:quantitative_Concept}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:plant}"
                , "{1:biologic_Function, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:vertebrate}"
                , "{1:biomedical_or_Dental_Material, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function}"
                , "{1:biomedical_or_Dental_Material, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:cell_Function, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:chemical_Viewed_Structurally, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:element_Ion_or_Isotope, 1:organ_or_Tissue_Function, 1:pathologic_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:fish, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:food, 1:organ_or_Tissue_Function, 1:pathologic_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:fungus, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:immunologic_Factor, 1:natural_Phenomenon_or_Process, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:invertebrate, 1:organ_or_Tissue_Function, 1:physiologic_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mammal, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_Process, 1:organ_or_Tissue_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:organism_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:organism_Function, 1:physiologic_Function}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:pathologic_Function, 1:steroid}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:physiologic_Function, 1:quantitative_Concept}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:plant}"
                , "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:organ_or_Tissue_Function, 1:vertebrate}"
                , "{1:disease_or_Syndrome, 1:mental_Process, 1:organ_or_Tissue_Function, 1:pathologic_Function, 1:physiologic_Function}"
                , "{1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction, 1:organ_or_Tissue_Function, 1:pathologic_Function}");
        */

//        List<String> sets = Sugar.list("{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:environmental_Effect_of_Humans, 1:mental_Process, 1:pathologic_Function}", "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:environmental_Effect_of_Humans, 1:organism_Function, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}");
        List<String> sets = Sugar.list("{1:acquired_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:environmental_Effect_of_Humans, 1:mental_Process, 1:pathologic_Function}", "{1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction, 1:environmental_Effect_of_Humans, 1:organism_Function, 1:pathologic_Function}", "{1:biologic_Function, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:body_Substance, 1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:clinical_Attribute, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:drug_Delivery_Device, 1:environmental_Effect_of_Humans, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:enzyme, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:injury_or_Poisoning, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:medical_Device, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:mental_Process, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:mental_or_Behavioral_Dysfunction, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:natural_Phenomenon_or_Process, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:organ_or_Tissue_Function, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:organism_Function, 1:pathologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function, 1:phenomenon_or_Process}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function, 1:physiologic_Function}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function, 1:plant}", "{1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome, 1:environmental_Effect_of_Humans, 1:pathologic_Function, 1:vitamin}");
        List<Set<String>> constants = sets.stream().map(s -> {
            s = s.substring(1, s.length() - 1);
//            System.out.println(s);
            String[] splitted = s.split(",");
            return Arrays.stream(splitted).map(String::trim).collect(Collectors.toSet());
        }).collect(Collectors.toList());

        for (int outer = 0; outer < constants.size(); outer++) {
            if (constants.get(outer).size() > 5) {
                System.out.println("hey, this one is larger than five\t" + constants.get(outer));
            }

            for (int inner = 0; inner < constants.size(); inner++) {
                if (inner == outer) {
                    continue;
                }

                Set<String> intersection = Sugar.intersection(constants.get(outer), constants.get(inner));
                //System.out.println(outer + "\t" + inner + "\t->\t" + intersection.size());
                if (intersection.size() == constants.get(outer).size()) {
                    System.out.println("problem here since the first one subsumes the second one\n\t" + constants.get(outer) + "\n\t" + constants.get(inner));
                }

            }
        }
        System.out.println("testing of minimals ends");
    }

    private static void hashMapIterationModification() {
        Map<Integer, List<Integer>> map = new HashMap<>();

        map.put(1, Sugar.list(1, 2, 3));
        map.put(2, Sugar.list(1, 2));
        map.put(4, Sugar.list(7, 8, 3));

        for (Map.Entry<Integer, List<Integer>> integerListEntry : map.entrySet()) {
            System.out.println(integerListEntry.getKey() + "\t:\t" + integerListEntry.getValue().stream().map(Object::toString).collect(Collectors.joining(", ")));
        }

        for (Map.Entry<Integer, List<Integer>> integerListEntry : map.entrySet()) {
            //integerListEntry.setValue(Sugar.list(1, 2, 4, 4, 4, 4));
            integerListEntry.getValue().remove(1);
        }


        for (Map.Entry<Integer, List<Integer>> integerListEntry : map.entrySet()) {
            System.out.println(integerListEntry.getKey() + "\t:\t" + integerListEntry.getValue().stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
    }


    private static void firstDifferent() {
        IntStream.range(1, 24).forEach(idx -> {
            //Path f = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\toOpt4\\queries" + (10 * idx) + ".db");
            //Path f = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\splittedFullyAlll_Opt5\\queries" + (10 * idx) + ".db");
//            Path f = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\opt5fixExtraPruning3_Opt5\\queries" + (10 * idx) + ".db");
            Path f = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\unionPreviousAndNew_Opt5\\queries" + (10 * idx) + ".db");
//            Path f = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\debugDevTestOpt3\\queries" + (10*idx) + ".db");
            //Path s = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\holderOpt\\queries" + (10 * idx) + ".db");
//            Path s = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\standard-backup\\queries" + (10 * idx) + ".db");
            Path s = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\hypothesisPruning\\pac2\\umlsSpeedupTest\\uOnlyNewlyAdded_Opt5\\queries" + (10 * idx) + ".db");
            long l1 = 0;
            try {
                l1 = Files.lines(f).filter(l -> !l.trim().isEmpty()).count();
                long l2 = Files.lines(s).filter(l -> !l.trim().isEmpty()).count();
                if (l1 != l2) {
                    System.out.println(idx + "\n\t" + f + "\tvs\t" + s + "\n\t" + l1 + "\t" + l2);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        });
    }

    private static void clauseEquals() {
        Clause c1 = Clause.parse("affects(1:disease_or_Syndrome, 1:cell_or_Molecular_Dysfunction), affects(1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome), affects(1:cell_or_Molecular_Dysfunction, 1:pathologic_Function), result_of(1:natural_Phenomenon_or_Process, 1:phenomenon_or_Process), affects(1:disease_or_Syndrome, 1:alga), affects(1:physiologic_Function, 1:mental_Process), manifestation_of(1:laboratory_or_Test_Result, 1:disease_or_Syndrome), affects(1:mental_Process, 1:fish), result_of(1:physiologic_Function, 1:biologic_Function), associated_with(1:laboratory_or_Test_Result, 1:anatomical_Abnormality), process_of(1:biologic_Function, 1:alga), affects(1:cell_or_Molecular_Dysfunction, 1:plant), affects(1:disease_or_Syndrome, 1:pathologic_Function), result_of(1:physiologic_Function, 1:cell_Function), performs(1:organism_Function, 1:governmental_or_Regulatory_Activity), affects(1:biomedical_or_Dental_Material, 1:biologic_Function), affects(1:natural_Phenomenon_or_Process, 1:cell_or_Molecular_Dysfunction), affects(1:nucleic_Acid_Nucleoside_or_Nucleotide, 1:pathologic_Function), affects(1:physiologic_Function, 1:alga), manifestation_of(1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction), affects(1:mental_or_Behavioral_Dysfunction, 1:invertebrate), affects(1:cell_Function, 1:biologic_Function), affects(1:physiologic_Function, 1:fish), affects(1:disease_or_Syndrome, 1:invertebrate), affects(1:disease_or_Syndrome, 1:plant), occurs_in(1:physiologic_Function, 1:organism_Function), affects(1:physiologic_Function, 1:biologic_Function), affects(1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction), result_of(1:physiologic_Function, 1:disease_or_Syndrome), result_of(1:physiologic_Function, 1:mental_or_Behavioral_Dysfunction), affects(1:cell_Function, 1:plant), performs(1:patient_or_Disabled_Group, 1:governmental_or_Regulatory_Activity), affects(1:disease_or_Syndrome, 1:fish), result_of(1:natural_Phenomenon_or_Process, 1:biologic_Function), affects(1:organism_Function, 1:physiologic_Function), affects(1:physiologic_Function, 1:disease_or_Syndrome), affects(1:disease_or_Syndrome, 1:physiologic_Function), affects(1:cell_or_Molecular_Dysfunction, 1:mental_Process), process_of(1:organism_Function, 1:physiologic_Function), affects(1:disease_or_Syndrome, 1:fungus), affects(1:disease_or_Syndrome, 1:mental_Process), affects(1:cell_or_Molecular_Dysfunction, 1:biologic_Function), affects(1:cell_or_Molecular_Dysfunction, 1:fungus), affects(1:cell_or_Molecular_Dysfunction, 1:organ_or_Tissue_Function), result_of(1:natural_Phenomenon_or_Process, 1:disease_or_Syndrome), process_of(1:natural_Phenomenon_or_Process, 1:cell_or_Molecular_Dysfunction), process_of(1:physiologic_Function, 1:disease_or_Syndrome), affects(1:physiologic_Function, 1:behavior), affects(1:physiologic_Function, 1:plant), affects(1:cell_or_Molecular_Dysfunction, 1:mental_or_Behavioral_Dysfunction), affects(1:cell_or_Molecular_Dysfunction, 1:behavior), affects(1:mental_or_Behavioral_Dysfunction, 1:mental_Process), interacts_with(1:fungus, 1:virus), affects(1:disease_or_Syndrome, 1:behavior), manifestation_of(1:pathologic_Function, 1:physiologic_Function), causes(1:food, 1:anatomical_Abnormality), affects(1:cell_or_Molecular_Dysfunction, 1:invertebrate), causes(1:chemical_Viewed_Structurally, 1:disease_or_Syndrome), affects(1:physiologic_Function, 1:cell_or_Molecular_Dysfunction), affects(1:cell_or_Molecular_Dysfunction, 1:physiologic_Function), process_of(1:cell_Function, 1:mental_or_Behavioral_Dysfunction), affects(1:biologic_Function, 1:alga), affects(1:physiologic_Function, 1:organ_or_Tissue_Function), consists_of(1:body_Substance, 1:steroid), location_of(1:acquired_Abnormality, 1:fungus), affects(1:disease_or_Syndrome, 1:organ_or_Tissue_Function), precedes(1:physiologic_Function, 1:organism_Function), result_of(1:natural_Phenomenon_or_Process, 1:organism_Function), affects(1:physiologic_Function, 1:invertebrate), result_of(1:natural_Phenomenon_or_Process, 1:physiologic_Function), result_of(1:physiologic_Function, 1:natural_Phenomenon_or_Process), affects(1:disease_or_Syndrome, 1:biologic_Function), affects(1:cell_or_Molecular_Dysfunction, 1:alga), affects(1:acquired_Abnormality, 1:alga), affects(1:physiologic_Function, 1:mental_or_Behavioral_Dysfunction), interacts_with(1:organic_Chemical, 1:steroid), process_of(1:disease_or_Syndrome, 1:organ_or_Tissue_Function), affects(1:physiologic_Function, 1:fungus), affects(1:social_Behavior, 1:behavior), affects(1:cell_Function, 1:mental_or_Behavioral_Dysfunction), result_of(1:natural_Phenomenon_or_Process, 1:cell_Function), affects(1:cell_or_Molecular_Dysfunction, 1:fish)");
        Clause c2 = Clause.parse("affects(1:disease_or_Syndrome, 1:cell_or_Molecular_Dysfunction), affects(1:cell_or_Molecular_Dysfunction, 1:disease_or_Syndrome), affects(1:cell_or_Molecular_Dysfunction, 1:pathologic_Function), result_of(1:natural_Phenomenon_or_Process, 1:phenomenon_or_Process), affects(1:disease_or_Syndrome, 1:alga), affects(1:physiologic_Function, 1:mental_Process), manifestation_of(1:laboratory_or_Test_Result, 1:disease_or_Syndrome), affects(1:mental_Process, 1:fish), result_of(1:physiologic_Function, 1:biologic_Function), associated_with(1:laboratory_or_Test_Result, 1:anatomical_Abnormality), process_of(1:biologic_Function, 1:alga), affects(1:disease_or_Syndrome, 1:pathologic_Function), affects(1:cell_or_Molecular_Dysfunction, 1:plant), result_of(1:physiologic_Function, 1:cell_Function), performs(1:organism_Function, 1:governmental_or_Regulatory_Activity), affects(1:biomedical_or_Dental_Material, 1:biologic_Function), affects(1:natural_Phenomenon_or_Process, 1:cell_or_Molecular_Dysfunction), affects(1:nucleic_Acid_Nucleoside_or_Nucleotide, 1:pathologic_Function), affects(1:physiologic_Function, 1:alga), manifestation_of(1:anatomical_Abnormality, 1:cell_or_Molecular_Dysfunction), affects(1:mental_or_Behavioral_Dysfunction, 1:invertebrate), affects(1:cell_Function, 1:biologic_Function), affects(1:disease_or_Syndrome, 1:invertebrate), affects(1:disease_or_Syndrome, 1:plant), affects(1:physiologic_Function, 1:fish), occurs_in(1:physiologic_Function, 1:organism_Function), affects(1:physiologic_Function, 1:biologic_Function), affects(1:disease_or_Syndrome, 1:mental_or_Behavioral_Dysfunction), result_of(1:physiologic_Function, 1:disease_or_Syndrome), result_of(1:physiologic_Function, 1:mental_or_Behavioral_Dysfunction), affects(1:cell_Function, 1:plant), performs(1:patient_or_Disabled_Group, 1:governmental_or_Regulatory_Activity), affects(1:disease_or_Syndrome, 1:fish), result_of(1:natural_Phenomenon_or_Process, 1:biologic_Function), affects(1:organism_Function, 1:physiologic_Function), affects(1:physiologic_Function, 1:disease_or_Syndrome), affects(1:disease_or_Syndrome, 1:physiologic_Function), affects(1:cell_or_Molecular_Dysfunction, 1:mental_Process), process_of(1:organism_Function, 1:physiologic_Function), affects(1:disease_or_Syndrome, 1:fungus), affects(1:disease_or_Syndrome, 1:mental_Process), affects(1:cell_or_Molecular_Dysfunction, 1:biologic_Function), affects(1:cell_or_Molecular_Dysfunction, 1:fungus), affects(1:cell_or_Molecular_Dysfunction, 1:organ_or_Tissue_Function), result_of(1:natural_Phenomenon_or_Process, 1:disease_or_Syndrome), process_of(1:natural_Phenomenon_or_Process, 1:cell_or_Molecular_Dysfunction), process_of(1:physiologic_Function, 1:disease_or_Syndrome), affects(1:physiologic_Function, 1:behavior), affects(1:physiologic_Function, 1:plant), affects(1:cell_or_Molecular_Dysfunction, 1:mental_or_Behavioral_Dysfunction), affects(1:cell_or_Molecular_Dysfunction, 1:behavior), affects(1:mental_or_Behavioral_Dysfunction, 1:mental_Process), interacts_with(1:fungus, 1:virus), affects(1:disease_or_Syndrome, 1:behavior), manifestation_of(1:pathologic_Function, 1:physiologic_Function), causes(1:food, 1:anatomical_Abnormality), affects(1:cell_or_Molecular_Dysfunction, 1:invertebrate), causes(1:chemical_Viewed_Structurally, 1:disease_or_Syndrome), affects(1:physiologic_Function, 1:cell_or_Molecular_Dysfunction), affects(1:cell_or_Molecular_Dysfunction, 1:physiologic_Function), process_of(1:cell_Function, 1:mental_or_Behavioral_Dysfunction), affects(1:biologic_Function, 1:alga), affects(1:physiologic_Function, 1:organ_or_Tissue_Function), consists_of(1:body_Substance, 1:steroid), location_of(1:acquired_Abnormality, 1:fungus), affects(1:disease_or_Syndrome, 1:organ_or_Tissue_Function), precedes(1:physiologic_Function, 1:organism_Function), result_of(1:natural_Phenomenon_or_Process, 1:organism_Function), affects(1:physiologic_Function, 1:invertebrate), result_of(1:physiologic_Function, 1:natural_Phenomenon_or_Process), result_of(1:natural_Phenomenon_or_Process, 1:physiologic_Function), affects(1:disease_or_Syndrome, 1:biologic_Function), affects(1:cell_or_Molecular_Dysfunction, 1:alga), affects(1:acquired_Abnormality, 1:alga), affects(1:physiologic_Function, 1:mental_or_Behavioral_Dysfunction), interacts_with(1:organic_Chemical, 1:steroid), process_of(1:disease_or_Syndrome, 1:organ_or_Tissue_Function), affects(1:physiologic_Function, 1:fungus), affects(1:social_Behavior, 1:behavior), affects(1:cell_Function, 1:mental_or_Behavioral_Dysfunction), result_of(1:natural_Phenomenon_or_Process, 1:cell_Function), affects(1:cell_or_Molecular_Dysfunction, 1:fish)");

        System.out.println(c1.literals().equals(c2.literals()));
    }
}
