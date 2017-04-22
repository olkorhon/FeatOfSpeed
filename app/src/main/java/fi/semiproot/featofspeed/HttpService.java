package fi.semiproot.featofspeed;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

public class HttpService {
    private static HttpService instance;
    private static Context context;
    private RequestQueue queue;

    private HttpService(Context context) {
        this.context = context;
        this.queue = getRequestQueue();
    }

    static HttpService getInstance(Context context) {
        if (instance == null) {
            instance = new HttpService(context);
        }
        return instance;
    }

    public RequestQueue getRequestQueue() {
        if (queue == null) {
            queue = Volley.newRequestQueue(context.getApplicationContext());
        }
        return queue;
    }

    public <T> void addToRequestQueue(Request<T> req) {
        getRequestQueue().add(req);
    }
}
