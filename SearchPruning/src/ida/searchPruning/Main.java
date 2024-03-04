package ida.searchPruning;

import java.io.*;

/**
 * Created by martin.svatos on 17.04.2017.
 */
public class Main {

    /**
     * arg1 -- path to dataset
     * arg2 -- type of search [breadth | beam | best]
     * arg3 -- maximal number of literals for constraint learner
     * arg4 -- maximal number of variables for constraint learner
     * arg5 -- max depth
     * arg6 -- query/rule minimal frequency (# of absolute occurrences)
     * arg7 -- number of folds for crossvalidation; optional
     *
     * @param args
     * @throws IOException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        //args = new String[]{"C:\\data\\school\\development\\pruningHypothesis\\datasets\\nci_transformed\\gi50_screen_HOP_18.txt", "breadth" ,"3" ,"6" ,"2" ,"1" ,"0"};
        /** /
        args = new String[]{
                Sugar.path("..", "datasets", "nci_transformed", "gi50_screen_KM20L2.txt"),
                "breadth",
                //"0", "0",
                "2" , "2" ,
                "2", // "3"
                "1",
                "0"};
        /**/

        System.out.println("java.util.concurrent.ForkJoinPool.common.parallelism\t" + System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism","undef"));


        System.out.println("tady zadratovat vstupy at to mam jednodussi ;)");

        String dataPath = args[0];
        String method = args[1];
        int maxLength = Integer.valueOf(args[2]);
        int maxVariables = Integer.valueOf(args[3]);

        System.out.println("add sleeep at start!!!!!!!!!!!!!");
        Thread.sleep(2 * 60    * 1000);

        int maxDepth = 6;
        int minFrequency = 1;
        if (args.length >= 6) {
            maxDepth = Integer.valueOf(args[4]);
            minFrequency = Integer.valueOf(args[5]);
        }

        if (args.length >= 7) {
            int fold = Integer.valueOf(args[6]);
            Runner.run(dataPath, method, maxLength, maxVariables, maxDepth, minFrequency, fold);
        } else {
            Runner.run(dataPath, method, maxLength, maxVariables, maxDepth, minFrequency, -1);
        }
    }


}
