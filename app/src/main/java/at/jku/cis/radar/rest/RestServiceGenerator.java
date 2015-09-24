package at.jku.cis.radar.rest;

import retrofit.RestAdapter;
import retrofit.android.AndroidApacheClient;

public class RestServiceGenerator {

    private static final String BASE_URL = "http://localhost:8080/rest";

    public static <S> S createService(Class<S> serviceClass) {
        RestAdapter.Builder builder = new RestAdapter.Builder()
                .setEndpoint(BASE_URL)
                .setClient(new AndroidApacheClient());
        return builder.build().create(serviceClass);
    }
}