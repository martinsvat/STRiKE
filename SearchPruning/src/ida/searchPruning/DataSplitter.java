package ida.searchPruning;


import ida.ilp.logic.io.PseudoPrologParser;
import ida.searchPruning.util.Utils;
import ida.utils.Sugar;
import ida.utils.tuples.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Created by martin.svatos on 16. 1. 2018.
 */
public class DataSplitter {
    private static final int K_SPLIT = 1;
    private static final int SPLIT_70_30 = 2;


    /*
     * TODO GENERALIZE FOR ANYTHING GIVEN CLASS, NOTHING ELSE ;)
     * only for classification
     * there might be some kind random permutation of samples
     */

    private void stratifiedSplit(String datasetPath, String outputFolderPath, int folds) {
        split(datasetPath, outputFolderPath, folds, true, K_SPLIT);
    }


    private void stratifiedSpilt7030(String datasetPath, String outputFolderPath) {
        split(datasetPath, outputFolderPath, 2, true, SPLIT_70_30);
    }

    private void split(String datasetPath, String outputFolderPath, int folds, boolean stratified, int mode) {
        List<Pair<String, String>> labeledExamples = parseClassification(datasetPath);
        Function<Pair<String, String>, String> aggregator = (stratified) ? Pair::getR : (pair) -> "1";
        Map<String, List<String>> classes = labeledExamples.stream().collect(Collectors.groupingBy(aggregator, Collectors.mapping(Pair::getS, Collectors.toList())));
        if (folds > classes.values().stream().mapToInt(List::size).min().orElse(0)) {
            throw new IllegalStateException("Cannot split some category since there are fewer examples than folds.");
        }

        Map<String, List<List<String>>> folded = classes.entrySet().stream().collect(Collectors.toMap(Entry::getKey, entry -> {
            switch (mode) {
                case SPLIT_70_30:
                    return splitByPercent(entry.getValue(), 1 - 0.7);
                case K_SPLIT:
                default:
                    return splitExamples(entry.getValue(), folds);
            }
        }));

        IntStream.range(0, (SPLIT_70_30 == mode) ? 1 : folds).forEach(idx -> {
            String outputDir = Sugar.path(outputFolderPath, (SPLIT_70_30 == mode) ? "split7030" : idx + "");
            File dir = new File(outputDir);
            dir.mkdirs();
            Stream<String> train = folded.values().stream()
                    .flatMap(l -> IntStream.range(0, folds).filter(currentIdx -> idx != currentIdx)
                            .mapToObj(currentIdx -> l.get(currentIdx)).flatMap(s -> s.stream()));
            Stream<String> test = folded.values().stream().flatMap(l -> l.get(idx).stream());
            try {
                Utils.writeToFile(Sugar.path(outputDir, "train"), train.collect(Collectors.joining("\n")));
                Utils.writeToFile(Sugar.path(outputDir, "test"), test.collect(Collectors.joining("\n")));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * percent says the percentage of the first returned class; to make split to two groups, e.g. train-test with 70-30 ratio use percent=70
     *
     * @param elements
     * @param percent
     */
    private List<List<String>> splitByPercent(List<String> elements, double percent) {
        if(percent <= 0.0 || percent >= 1.0){
            throw new IllegalArgumentException("percent must be between 0 to 100, e.g. for 70-30 split use percent=70");
        }
        elements = Sugar.listFromCollections(elements);
        Collections.shuffle(elements); // heavily adviced
        int threshold = Math.min((int)Math.ceil(percent * elements.size()),elements.size());
        return Sugar.list(elements.subList(0,threshold),elements.subList(threshold,elements.size()));
    }

    private List<List<String>> splitExamples(List<String> elements, int folds) {
        List<List<String>> list = Sugar.list();
        int step = elements.size() / folds;
        for (int fold = 0; fold < folds; fold++) {
            int start = fold * step;
            int ceil = Math.min(start + step, elements.size());
            if (fold == folds - 1) {
                ceil = elements.size();
            }
            list.add(elements.subList(start, ceil));
        }
        return list;
    }

    /***
     * list of pair of <class, class + example>
     * @param datasetPath
     * @return
     */
    private List<Pair<String, String>> parseClassification(String datasetPath) {
        try {
            return PseudoPrologParser.read(new FileReader(datasetPath)).stream()
                    .map(pair -> new Pair<>(pair.s, pair.s + " " + pair.r.toString())).collect(Collectors.toList());
        } catch (IOException e) {
            System.out.println("cannot parse the file");
            e.printStackTrace();
        }
        return Sugar.list();
    }


    private static DataSplitter create() {
        return new DataSplitter();
    }

    public static void main(String[] args) {
        DataSplitter splitter = DataSplitter.create();

        Sugar.list(
//                "gi50_screen_U251.txt"
//                ,"gi50_screen_SW_620.txt"
//                ,"gi50_screen_SF_295.txt"
//                , "gi50_screen_SF_268.txt"
//                , "gi50_screen_SNB_19.txt"
                //, "gi50_screen_HCT_15.txt"
                 "gi50_screen_HCT_116.txt"
                ,"gi50_screen_OVCAR_8.txt"
                ,"gi50_screen_HT29.txt"
                ,"gi50_screen_NCI_H23.txt"
                ,"gi50_screen_KM12.txt"
                ,"gi50_screen_IGROV1.txt"
                ,"gi50_screen_UACC_257.txt"
                ,"gi50_screen_UACC_62.txt"
                ,"gi50_screen_OVCAR_3.txt"
                ,"gi50_screen_EKVX.txt"
                ,"gi50_screen_OVCAR_5.txt"
                ,"gi50_screen_SK_MEL_5.txt"
                ,"gi50_screen_NCI_H322M.txt"
                ,"gi50_screen_SN12C.txt"
                ,"gi50_screen_COLO_205.txt"
                ,"gi50_screen_HOP_62.txt"
                ,"gi50_screen_SK_MEL_2.txt"
                ,"gi50_screen_K_562.txt"
                ,"gi50_screen_NCI_H522.txt"
                ,"gi50_screen_OVCAR_4.txt"
                ,"gi50_screen_NCI_H460.txt"
                ,"gi50_screen_LOX_IMVI.txt"
                ,"gi50_screen_CAKI_1.txt"
                ,"gi50_screen_RPMI_8226.txt"
                ,"gi50_screen_MOLT_4.txt"
                ,"gi50_screen_M14.txt"
                ,"gi50_screen_SK_OV_3.txt"
                ,"gi50_screen_ACHN.txt"
                ,"gi50_screen_SNB_75.txt"
                ,"gi50_screen_CCRF_CEM.txt"
                ,"gi50_screen_MALME_3M.txt"
                ,"gi50_screen_A498.txt"
                )
                .forEach(domain -> {
                            String dataset = Sugar.path("..", "datasets", "nci_transformed", domain);
                            String output = Sugar.path("..", "datasets", "splitted", "nci_transformed", domain);
                            splitter.stratifiedSplit(dataset, output, 10);
                        });
        //splitter.stratifiedSpilt7030(dataset, output);
        //String dataset =  Sugar.path("test", "datasetSplitter", "input","examples");
        //String output = Sugar.path("test","datasetSplitter","output");
        //splitter.split(dataset,output,10,false);
    }


}
