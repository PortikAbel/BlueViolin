package Server.Parser.Select;

import Server.DbStructure.Attribute;
import Server.DbStructure.DbExceptions;
import Server.DbStructure.Table;
import Server.Filter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;

import static Server.Parser.Select.TableParser.getTableOfAttribute;

public class Condition {
    protected static boolean conditionParser(
            Matcher condMatcher,
            ArrayList<Table> selectedTables,
            HashMap< Table, HashMap<Attribute, ArrayList<Filter> >> filtersOnAttributeInTable,
            HashMap<String, Table> tableAS,
            HashMap< Table, HashMap< String, Attribute> > attributeByNameInTable
    ) throws DbExceptions.DataManipulationException
    {
        String tableAlias, attributeName, operator, rightOperand;
        if (condMatcher.find()) {
            tableAlias = condMatcher.group(1);
            attributeName = condMatcher.group(2);
            operator = condMatcher.group(3);
            rightOperand = condMatcher.group(4);
        } else {
            return false;
        }
        // if containing table was not given to attribute
        if (tableAlias == null) {
            tableAlias = getTableOfAttribute(selectedTables, attributeName);
            if (tableAlias == null)
                throw new DbExceptions.DataManipulationException(
                        "Could not determine to which table attribute belongs: " + attributeName);
        }
        Filter filter = new Filter(operator, rightOperand);
        filtersOnAttributeInTable
                .get(tableAS.get(tableAlias))
                .get(attributeByNameInTable
                        .get(tableAS.get(tableAlias))
                        .get(attributeName))
                .add(filter);
        return true;
    }
}
