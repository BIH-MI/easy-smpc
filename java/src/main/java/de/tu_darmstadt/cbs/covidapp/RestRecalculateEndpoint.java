package de.tu_darmstadt.cbs.covidapp;

import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;
import net.freeutils.httpserver.HTTPServer.ContextHandler;
import java.util.Iterator;
import java.util.Map;

public class RestRecalculateEndpoint implements ContextHandler {
    private CovidApp app;
    
    public RestRecalculateEndpoint(CovidApp app) {
    	this.app = app;
    }
    public int serve(Request req, Response resp) {
        Map<String, String> params;
        try {
            params = req.getParams();

            Iterator it = params.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry p = (Map.Entry) it.next();
                System.out.println(p.getKey() + " - " + p.getValue());
                it.remove();
            }
            resp.getHeaders().add("Connection", "close");
            if (app.isHost()) {
            	app.rerunComputation.countDown();
            	resp.send(200, "Recalculation requested.\r\n");
            } else {
            	resp.sendError(400, "This node runs in client mode. Please request recalculation at the host node.\r\n");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }

}
