package com.wizzardo.heri;

import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.framework.WebApplication;
import com.wizzardo.http.request.ByteTree;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.request.Request;
import com.wizzardo.http.response.JsonResponseHelper;

public class App {

    public static void main(String[] args) {
        WebApplication webApplication = new WebApplication(args) {
            @Override
            protected void initHttpPartsCache() {
                ByteTree tree = httpStringsCache.getTree();
                for (Request.Method method : Request.Method.values()) {
                    tree.append(method.name());
                }
                tree.append(HttpConnection.HTTP_1_1);
            }
        };

        webApplication.onSetup(app -> {
                    app.getUrlMapping()
                            .append("/index", DBController.class, "index")
                            .append("/webhook", DBController.class, "webhook")
                    ;
                }
        );

        webApplication.start();
    }
}
