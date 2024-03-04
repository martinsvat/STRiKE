package ida.gnns;

import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;
import ida.pacReasoning.data.DatasetSampler;
import ida.pacReasoning.data.Reformatter;
import ida.pacReasoning.Pipeline;
import ida.utils.Sugar;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * The intention of this class is to have one-class & one-click-run for dataset generation.
 * <p>
 * Created by martin.svatos on 3. 2. 2021.
 */
public class DatasetPreparation {


    // expects NTP like data
    public void prepareForGradualComparison(Path base, double trainBasicProportion, double trainProportionForRuleLearning, int step, boolean rewrite) {
        Path train = Paths.get(base.toString(), "train.nl");
        Path test = Paths.get(base.toString(), "test.nl");
        Path val = Paths.get(base.toString(), "dev.nl");
        DatasetSampler ds = DatasetSampler.create(DatasetSampler.UNIFORM_RANDOM, false, rewrite);
        Path output = Paths.get(base + "_g");
        // generate basic.nl (contains only a part of train data) and queriesX (containing a sample of original train data not occurring in base.nl
        // also generates subsampled dataset for CE
        try {
            Set<Literal> trainEvidence = LogicUtils.loadEvidence(train);
            ds.sampleAndStoreKGEWrtEvidenceSize(train,
                    output,
                    trainBasicProportion,
                    IntStream.range(1, trainEvidence.size()).filter(i -> 0 == i % step).boxed().collect(Collectors.toList())
            );
            Files.write(Paths.get(output.toString(), "train.nl"), LogicUtils.loadEvidence(train).stream().map(Literal::toString).collect(Collectors.toList()));
            Files.write(Paths.get(output.toString(), "test.nl"), LogicUtils.loadEvidence(test).stream().map(Literal::toString).collect(Collectors.toList()));
            Files.write(Paths.get(output.toString(), "dev.nl"), LogicUtils.loadEvidence(val).stream().map(Literal::toString).collect(Collectors.toList()));
            Pipeline.datasetSubsampling(Paths.get(output.toString(), "train.nl"),
                    base.getParent(),
                    trainProportionForRuleLearning,
                    true);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // reformatte input for different rule learners
        Reformatter r = Reformatter.create();
        Path amie = Paths.get(output.toString() + "--amieTriple");
        r.renameAndStore(r.identityDictionary(), output, amie, Sugar.list("base.nl", "train.nl", "dev.nl", "test.nl"), r::amieRenaming, rewrite);
        Path anyburl = Paths.get(output.toString() + "--anyBurlTriple");
        r.renameAndStore(r.identityDictionary(), output, anyburl, Sugar.list("base.nl", "train.nl", "dev.nl", "test.nl"), r::anyBurlRenaming, rewrite);

        // renames queries and *.nl to KGE input
        Path kgeDir = Paths.get(output + "_kge");
        try {
            List<String> files = Files.list(output)
                    .map(path -> path.getFileName().toString())
                    .filter(path -> path.endsWith(".nl") || path.endsWith(".db"))
                    .collect(Collectors.toList());
            r.renameAndStore(r.identityDictionary(), output, kgeDir, files, r::anyBurlRenaming, rewrite);

            r.createKGEDictAndStore(Sugar.list(Paths.get(base.toString(), "train.nl"),
                    Paths.get(base.toString(), "dev.nl"),
                    Paths.get(base.toString(), "test.nl")),
                    kgeDir,
                    rewrite);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    private static DatasetPreparation create() {
        return new DatasetPreparation();
    }

    public static void main(String args[]) {
        DatasetPreparation preparator = DatasetPreparation.create();
        boolean rewrite = false;
        double trainBasicProportion = 0.5;
        double trainProportionForRuleLearning = 0.5;
        int step = 10;
        Path originFolder = Paths.get("C:\\data\\school\\development\\Xpruning\\Xpruning\\datasets\\umls-ntp");
        preparator.prepareForGradualComparison(originFolder, trainBasicProportion, trainProportionForRuleLearning, step, rewrite);
    }
}
