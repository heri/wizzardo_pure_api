package com.wizzardo.heri;

import com.wizzardo.epoll.ByteBufferProvider;
import com.wizzardo.epoll.ByteBufferWrapper;
import com.wizzardo.http.HttpConnection;
import com.wizzardo.http.framework.Controller;
import com.wizzardo.http.framework.template.Renderer;
import com.wizzardo.http.request.Header;
import com.wizzardo.http.response.Status;
import com.wizzardo.tools.json.JsonTools;
import io.reactiverse.pgclient.PgIterator;
import io.reactiverse.pgclient.PgPool;
import io.reactiverse.pgclient.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class DBController extends Controller {

    DBService dbService;

    public void index() {
        User[] users = new User[10];

        AtomicInteger counter = new AtomicInteger(0);
        AtomicBoolean failed = new AtomicBoolean(false);
        PgPool pool = dbService.getClient();

        response.async();

        pool.preparedQuery("SELECT * FROM users LIMIT 10"), res -> {
            if (res.succeeded()) {
                PgIterator resultSet = res.result().iterator();
                Tuple row = resultSet.next();
                users[index] = new User(row.getString(0), row.getString(1), row.get);
            } else {
                res.cause().printStackTrace();
                if (failed.compareAndSet(false, true)) {
                    response.status(Status._500).body(res.cause().getMessage());
                    commitAsyncResponse();
                }
            }

            if (counter.incrementAndGet() == queries && !failed.get()) {
                response.appendHeader(Header.KV_CONTENT_TYPE_APPLICATION_JSON);
                response.body(JsonTools.serializeToBytes(users));
                commitAsyncResponse();
            }
        });
    }

    public void webhook() {
        static String firstName = (String)params().get("firstName");
        static String lastName = (String)params().get("lastName");
        static String id = (String)params().get("id");
        User[] users = new User[];
        PgPool pool = dbService.getClient();

        response.async();

        pool.preparedBatch("UPDATE users SET firstName=$1, lastName=$2 WHERE id=$3", Tuple.of(firstName, lastName, id), res -> {
            if (res.failed()) {
                response.status(Status._500).body(res.cause().getMessage());
            } else {
                PgIterator resultSet = res.result().iterator();
                Tuple row = resultSet.next();
                users[index] = new User(row.getString(0), row.getString(1), row.getString(2));
                response.appendHeader(Header.KV_CONTENT_TYPE_APPLICATION_JSON);
                response.body(JsonTools.serializeToBytes(users));
            }
            commitAsyncResponse();
        });
    }

    static ThreadLocal<ByteBufferProvider> byteBufferProviderThreadLocal = ThreadLocal.<ByteBufferProvider>withInitial(() -> {
        ByteBufferWrapper wrapper = new ByteBufferWrapper(64 * 1024);
        return () -> wrapper;
    });

    protected void commitAsyncResponse() {
        ByteBufferProvider bufferProvider = byteBufferProviderThreadLocal.get();
        HttpConnection connection = request.connection();
        response.commit(connection, bufferProvider);
        connection.flush(bufferProvider);
        response.reset();
    }

    protected int getRandomNumber() {
        return 1 + ThreadLocalRandom.current().nextInt(10000);
    }

    public static final class User implements Comparable<User> {
        public String id;
        public String firstName;
        public String lastName;

        public User(String id, String firstName, String lastName) {
            this.id = id;
            this.firstName = firstName;
            this.lastName = lastName;
        }

        @Override
        public int compareTo(User o) {
            return Integer.compare(id, o.id);
        }
    }
}
