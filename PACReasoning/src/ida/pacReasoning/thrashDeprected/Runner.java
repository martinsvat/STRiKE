package ida.pacReasoning.thrashDeprected;

import ida.ilp.logic.Clause;
import ida.ilp.logic.Literal;
import ida.ilp.logic.LogicUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by martin.svatos on 18. 5. 2018.
 */
public class Runner {


    public static void main(String[] args) throws InterruptedException, IOException {
        System.out.println("pac reasoning runner starting");

        System.getProperties().forEach((property, value) -> {
            if (property.toString().startsWith("ida.")) {
                System.out.println(property + "\t" + value);
            }
        });


        if (!System.getProperty("os.name").toLowerCase().contains("win")
                && System.getProperty("ida.grid", "on").toLowerCase().equals("on")) {
            System.out.println("probably running on the grid, thus 2 minutes no rush\t" + System.getProperty("os.name"));
            Thread.sleep(2 * 60 * 1000);
        }

        String mode = System.getProperty("ida.pacReasoning.mode", "");
        if (mode.toLowerCase().equals("kEntailmentInterference".toLowerCase())) {
            kEntailmentInterference();
        } else {
            System.out.println("unknown mode");
        }
        System.out.println("ending");
    }

    private static void kEntailmentInterference() throws IOException {
        Path theoryPath = Paths.get(System.getProperty("ida.pacReasoning.theory"));
        Path evidencePath = Paths.get(System.getProperty("ida.pacReasoning.evidence"));
        int offset = Integer.parseInt(System.getProperty("ida.pacReasoning.kEntailmentOffset"));


        System.setProperty("ida.pacReasoning.subset.cache", "true");
        System.out.println("subset cache\t" + System.getProperty("ida.pacReasoning.subset.cache"));
        System.out.println("evaluate of PAC-reasoning");

        Set<Clause> theory = Files.lines(theoryPath)
                .map(Clause::parse)
                .filter(c -> c.countLiterals() > 0).collect(Collectors.toSet());


        Set<Literal> evidence = LogicUtils.loadEvidence(evidencePath);
        PSLearning pac = PSLearning.create();
        Set<Literal> entailed = pac.computeXE(evidence, theory, offset, 2, false, null);
        //entailed = Sugar.union(evidence, entailed);
        try {
            Path outputPath = Paths.get(theoryPath.toString() + "_out","offset" + offset,evidencePath.getFileName().toString());
            File dir = new File(outputPath.toFile().getParent());
            if(!dir.exists()){
                dir.mkdirs();
            }
            System.out.println(outputPath);
            System.out.println(dir);
            Files.write(outputPath, entailed.stream().map(l -> "" + l).collect(Collectors.toList()));
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
