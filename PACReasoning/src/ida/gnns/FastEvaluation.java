package ida.gnns;


import java.nio.file.Paths;
import java.util.Arrays;

/**
 * Created by martin.svatos on 27. 5. 2021.
 */
public class FastEvaluation {


    public static void main(String[] args) {
        System.out.println("Running fast evaluation with parameters");
        Arrays.stream(args).forEach(arg -> System.out.println("\t" + arg));

        String mode = args[0].toLowerCase();
            throw new IllegalStateException(); // not implemented
        /*if ("mrr".equals(mode)) {
            String dataset = args[1];
            String embeddingName = args[2];
            String basicEmbeddingRankingName = args.length == 3 ? "" : args[3];
            KREvaluation.onlineMRR(Paths.get(dataset), embeddingName, basicEmbeddingRankingName);
        } else if ("pr".equals(mode)) {
            String dataset = args[1];
            String embeddingName = args[2];
            String basicEmbeddingRankingName = args.length == 3 ? "" : args[3];
            KREvaluation.plotPROptimizedOnline(Paths.get(dataset), embeddingName, basicEmbeddingRankingName);
        }else {
            System.out.println("unknown mode\t" + mode);
        }
        System.out.println("finished");
         */
    }
}
