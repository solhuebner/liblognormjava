import java.io.IOException;
import sh.tools.liblognormjava.*;

public class test {

    public static void main(String[] args) {
        
        try {
            
            // liblognorm context
            SWIGTYPE_p_ln_ctx_s ctx = null;

            String errorLogFile = "error.log";
            String debugLogFile = "debug.log";

            // Sample messages based on the included rulebase
            // String message = "Quantity: 555";
            // String message = "myhostname: code=23";
            // String message = "Weight: 42kg";
            String message = "first field,second field,third field,fourth field";
            // String message = "CSV: field1,,field3";
            
            // Load library
            cz.adamh.utils.NativeUtils.loadLibraryFromJar("/liblognormjava-1.4.0.so");

            System.out.println("Loaded liblognorm library");

            // Initialize liblognorm context
            ctx = liblognorm.ln_initCtx();

            // Comment out if not needed
            liblognorm.ln_setErrMsgCB_sh(ctx, errorLogFile);

            // Comment out if not needed (if used runs liblognorm in debug mode => slow!)
            liblognorm.ln_setDbgMsgCB_sh(ctx, debugLogFile);

            // Try to load rulebase
            if ( liblognorm.ln_loadSamples(ctx, "sample.rulebase") == 0) {

                System.out.println("Loaded sample rulebase");

                // normalize Message and output Json with event tags
                System.out.println("JsonET: " + liblognorm.ln_normalize_sh(ctx, message, "json", 1));

                // normalize Message and output Json
                System.out.println("Json  : " + liblognorm.ln_normalize_sh(ctx, message, "json", 0));

                // normalize Message and output XML
                System.out.println("XML   : " + liblognorm.ln_normalize_sh(ctx, message, "xml", 0));

                // normalize Message and output CEE Syslog
                System.out.println("CEE   : " + liblognorm.ln_normalize_sh(ctx, message, "cee-syslog", 0));

            } else {

                System.out.println("Could NOT load sample rulebase");

            }

        } catch (IOException exception) {
            
            System.out.println("Could NOT load liblognorm library: " + exception);
            
        }
    }
}
