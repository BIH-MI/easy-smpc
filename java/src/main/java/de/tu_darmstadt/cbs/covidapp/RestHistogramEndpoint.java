package de.tu_darmstadt.cbs.covidapp;

import net.freeutils.httpserver.HTTPServer.Context;
import net.freeutils.httpserver.HTTPServer.Request;
import net.freeutils.httpserver.HTTPServer.Response;
import net.freeutils.httpserver.HTTPServer.ContextHandler;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class RestHistogramEndpoint implements ContextHandler {
    private CovidApp app;

    public RestHistogramEndpoint(CovidApp app) {
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
            String responseMsg = app.getResultMessage();
            resp.getHeaders().add("Content-Type", "text/plain");
            resp.getHeaders().add("Access-Control-Allow-Origin", "*");
            if (responseMsg == "NotReadyYet") {
                resp.getHeaders().add("Retry-After", "60");
                resp.getHeaders().add("Connection", "close");
                try {
                    resp.send(503, "The result is not yet available. Try again later.\n\r");
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            } else {
                resp.getHeaders().add("Connection", "close");
                resp.send(200, app.getResultMessage() + "\r\n");
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return 0;
    }
}
