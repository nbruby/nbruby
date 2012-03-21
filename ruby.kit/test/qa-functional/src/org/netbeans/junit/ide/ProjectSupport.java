package org.netbeans.junit.ide;

import java.util.concurrent.Future;
import org.netbeans.modules.parsing.api.ParserManager;
import org.netbeans.modules.parsing.api.ResultIterator;
import org.netbeans.modules.parsing.api.UserTask;

public class ProjectSupport {
    
    private ProjectSupport() {}
    
    public static void waitScanFinished() {
        // modified from version originally in java.j2seproject / java.source
        try {
            class T extends UserTask {
                @Override
                public void run(ResultIterator resultIterator) throws Exception {
                    // no-op
                }

            }
            Future<Void> f = ParserManager.parseWhenScanFinished("text/x-ruby", new T());
            if (!f.isDone()) {
                f.get();
            }
        } catch (Exception ex) {
        }
    }
    
}
