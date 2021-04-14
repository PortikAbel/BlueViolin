package Server;


public class DbExceptions {
    public static class DataDefinitionException extends Exception {
        public DataDefinitionException(String errormsg) {
            super(errormsg);
        }
    }

    public static class UnknownCommandException extends Exception{
        public UnknownCommandException() {
            super("Unknown command");
        }
    }

    public static class DataManipulationException extends Exception{
        public DataManipulationException(String errormsg) {
            super(errormsg);
        }
    }

    public static class UnsuccessfulDeleteException extends Exception{
        public UnsuccessfulDeleteException(String errormsg) {
            super(errormsg);
        }
    }
}
