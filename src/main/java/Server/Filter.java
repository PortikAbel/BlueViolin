package Server;

import com.mongodb.client.model.Filters;
import org.bson.conversions.Bson;

public class Filter {
    private final String operator, rightOperand;

    public Filter(String operator, String rightOperand) {
        this.operator = operator;
        this.rightOperand = rightOperand;
    }

    public boolean eval(String leftOperand) {
        switch (operator) {
            case "=" : return leftOperand.equals(rightOperand);
            case "!=" : return  !leftOperand.equals(rightOperand);
            default : return true;
        }
    }
    public boolean eval(Integer leftOperand) {
        switch (operator) {
            case "=" : return leftOperand == Integer.parseInt(rightOperand);
            case "!=" : return  leftOperand != Integer.parseInt(rightOperand);
            case "<" : return leftOperand < Integer.parseInt(rightOperand);
            case "<=" : return leftOperand <= Integer.parseInt(rightOperand);
            case ">" : return leftOperand > Integer.parseInt(rightOperand);
            case ">=" : return leftOperand >= Integer.parseInt(rightOperand);
            default : return true;
        }
    }

    public Bson asMongoFilterOnID() {
        switch (operator) {
            case "=" : return Filters.eq("_id", rightOperand);
            case "!=" : return Filters.ne("_id", rightOperand);
            default: return null;
        }
    }

    public Bson asMongoFilterOnIntID() {
        switch (operator) {
            case "=": return Filters.eq("_id", Integer.parseInt(rightOperand));
            case "!=": return Filters.ne("_id", Integer.parseInt(rightOperand));
            case "<": return Filters.lt("_id", Integer.parseInt(rightOperand));
            case "<=": return Filters.lte("_id", Integer.parseInt(rightOperand));
            case ">": return Filters.gt("_id", Integer.parseInt(rightOperand));
            case ">=": return Filters.gte("_id", Integer.parseInt(rightOperand));
            default: return null;
        }
    }
}
