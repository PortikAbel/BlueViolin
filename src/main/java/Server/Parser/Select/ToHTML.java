package Server.Parser.Select;

import java.util.List;

public class ToHTML {
    public static String toHTMLTable(List<String> header, List<List<String>> data) {
        StringBuilder table = new StringBuilder();
        table.append("<table>");
        table.append("<tr>");
        header.forEach(th -> {
            table.append("<th>");
            table.append(th);
            table.append("</th>");
        });
        table.append("</tr>");
        data.forEach(tr -> {
            table.append("<tr>");
            tr.forEach(td -> {
                table.append("<td>");
                table.append(td);
                table.append("</td>");
            });
            table.append("</tr>");
        });
        table.append("</table>");

        return table.toString();
    }
}
