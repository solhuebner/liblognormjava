import java.io.IOException;
import sh.tools.liblognormjava.*;

public class test {

    public static void main(String[] args) {
        
        try {
            
            SWIGTYPE_p_ln_ctx_s ctx = null;

            String errorLogFile = "error.log";
            String debugLogFile = "debug.log";

            // String message = "Quantity: 555";
            // String message = "myhostname: code=23";
            // String message = "Weight: 42kg";
            String message = "first field,second field,third field,fourth field";
            // String message = "CSV: field1,,field3";
            
            cz.adamh.utils.NativeUtils.loadLibraryFromJar("/liblognormjava-1.4.0.so");

            System.out.println("Loaded liblognorm library");

            ctx = liblognorm.ln_initCtx();

            // Comment out if not needed
            liblognorm.ln_setErrMsgCB_sh(ctx, errorLogFile);

            // Comment out if not needed
            liblognorm.ln_setDbgMsgCB_sh(ctx, debugLogFile);

            if ( liblognorm.ln_loadSamples(ctx, "sample.rulebase") == 0) {

                System.out.println("Loaded sample rulebase");

                // normalizeMessageJsonWithEventTags
                System.out.println("JsonET: " + liblognorm.ln_normalize_sh(ctx, message, "json", 1));

                // normalizeMessageJson
                System.out.println("Json  : " + liblognorm.ln_normalize_sh(ctx, message, "json", 0));

                // normalizeMessageXML
                System.out.println("XML   : " + liblognorm.ln_normalize_sh(ctx, message, "xml", 0));

                // normalizeMessageCEESyslog
                System.out.println("CEE   : " + liblognorm.ln_normalize_sh(ctx, message, "cee-syslog", 0));

            } else {

                System.out.println("Could NOT load sample rulebase");

            }

        } catch (IOException exception) {
            
            System.out.println("Could NOT load liblognorm library: " + exception);
            
        }
    }
}
