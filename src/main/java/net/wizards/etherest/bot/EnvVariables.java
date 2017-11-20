package net.wizards.etherest.bot;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.wizards.etherest.database.Redis;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class EnvVariables {
    private static OkHttpClient client = new OkHttpClient();
    private static Gson gson = new Gson();
    private static Redis redis = Redis.getInstance();

    public static String getBtc2Eth() {
        final String envKey = "env:btc2eth";
        final int expiry = 3600;
        String res = redis.get(envKey);

        if (res != null) {
            return res;
        }

        Request request = new Request.Builder()
                .url("http://shapeshift.io/rate/btc_eth")
                .build();
        try {
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (body != null) {
                JsonObject result = gson.fromJson(body.string(), JsonObject.class);
                if (result.has("rate")) {
                    res = result.get("rate").getAsString();
                    redis.set(envKey, res, expiry);
                    return res;
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

}
