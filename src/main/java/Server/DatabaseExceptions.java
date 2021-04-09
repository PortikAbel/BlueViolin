package Server;


public class DatabaseExceptions {
    public static class DataDefinitionException extends Exception {
        public DataDefinitionException(String errormsg) {
            super(errormsg);
        }
    }

    public static class UnknownCommandException extends Exception{
        public UnknownCommandException(String errormsg) {
            super(errormsg);
        }
    }

    public static class UnsuccesfulDeleteException extends Exception{
        public UnsuccesfulDeleteException(String errormsg) {
            super(errormsg);
        }
    }
}
