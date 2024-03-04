package ida.gnns;

import ida.utils.Sugar;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Created by martin.svatos on 12. 5. 2021.
 */
public class Table {

    private final List<List<String>> list;
    private final List<String> zeroColumn;
    private int idx;
    private int column;
    private final String emptySpaceFiller = "|\tx\t|";

    public Table() {
        this.zeroColumn = Sugar.list();
        this.list = Sugar.list();
        this.idx = 0;
        this.column = 0;
    }

    public void nextColumn() {
        for (int currentIndex = idx; currentIndex < this.list.size(); currentIndex++) {
            int currentRowLength = this.list.get(currentIndex).size();
            this.list.get(currentIndex)
                    .addAll(IntStream.range(currentRowLength, this.column)
                            .mapToObj(i -> emptySpaceFiller)
                            .collect(Collectors.toList()));
        }
        this.column++;
        this.idx = 0;
    }

    private void addMissingRow() {
        if (list.size() == this.idx) {
            this.list.add(Sugar.list());
        }
        addMissingColumns();
    }

    private void addMissingColumns() {
        int currentRowLength = list.get(idx).size();
        IntStream.range(currentRowLength, column)
                .forEach(i -> list.get(idx).add(emptySpaceFiller));
    }

    public void addCellBelow(String string, String zeroColumn) {
        addMissingRow();
        list.get(idx).add(string);
        this.idx++;
        if (idx > this.zeroColumn.size()) {
            this.zeroColumn.add(zeroColumn);
        }
    }

    public void addRows(StringBuilder sb) {
        Arrays.stream(sb.toString().split("\n")).forEach(this::addRow);
    }

    public void addRow(String row) {
        addRow(row, "");
    }

    public void addRow(String row, String zeroColumn) {
        addCellBelow(row, zeroColumn);
    }

    public String build() {
        return list.stream().map(line -> line.stream().collect(Collectors.joining("\t")))
                .collect(Collectors.joining("\n"));
    }

    public String texBuild(String caption) {
        if(0 != idx){
            nextColumn();
        }
        boolean nonEmptyZeroColumn = zeroColumn.stream().anyMatch(s -> s.length() > 0);
        String extraColumn = nonEmptyZeroColumn ? " c || " : "";
        String content = "";
        if (nonEmptyZeroColumn) {
            content = IntStream.range(0, list.size())
                    .mapToObj(idx -> zeroColumn.get(idx)
                            + " & "
                            + list.get(idx).stream().collect(Collectors.joining(" & ")))
                    .collect(Collectors.joining(" \\\\ \\hline \n"));
        } else {
            content = list.stream().map(line -> line.stream().collect(Collectors.joining(" & ")))
                    .collect(Collectors.joining(" \\\\ \\hline \n"));
        }
        return ("\\begin{table}\n" +
                " \\begin{tabular}{ " + extraColumn + IntStream.range(0, column).mapToObj(i -> " c ").collect(Collectors.joining("|")) + " }\n"
                + content
                + "\n \\end{tabular}\n" +
                " \\caption{"+caption+"}\n" +
                "\\end{table}").replaceAll(">=", "\\$\\\\ge\\$")
                .replaceAll("" + Double.NEGATIVE_INFINITY,"-oo");
    }


    public static Table create() {
        return new Table();
    }

    public static void main(String[] args) {
        Table table = Table.create();
        table.addRow("0-0");
        table.addRow("1-0");
        table.addRow("2-0");
        table.nextColumn();
        System.out.println(table.build());

        table.addRow("0-1");
        table.addRow("1-1");
        table.addRow("2-1");
        table.addRow("3-1");
        System.out.println("\n");
        System.out.println(table.build());
    }
}
