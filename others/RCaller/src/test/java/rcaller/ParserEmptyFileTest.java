package rcaller;


import org.junit.Test;
import static org.junit.Assert.*;

public class ParserEmptyFileTest {

    @Test(expected = rcaller.exception.ParseException.class)
    public void EmptyOutputInParser(){
    	RCaller caller = new RCaller();
         caller.setRscriptExecutable("/usr/bin/Rscript");

    	RCode code = new RCode();
    	code.addRCode("Some meaningless code");
    	caller.runAndReturnResult("requestedvar");
    }
}
