package Server;

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
            case "<" : return Integer.parseInt(leftOperand) < Integer.parseInt(rightOperand);
            case "<=" : return Integer.parseInt(leftOperand) <= Integer.parseInt(rightOperand);
            case ">" : return Integer.parseInt(leftOperand) > Integer.parseInt(rightOperand);
            case ">=" : return Integer.parseInt(leftOperand) >= Integer.parseInt(rightOperand);
            default : return true;
        }
    }
}
