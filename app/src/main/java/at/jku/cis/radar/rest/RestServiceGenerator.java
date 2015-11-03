package at.jku.cis.radar.rest;

import at.jku.cis.radar.converter.RadarJacksonConverter;
import retrofit.RestAdapter;

public class RestServiceGenerator {

    private static final String BASE_URL = "http://192.168.137.1:8080/rest";

    public static <S> S createService(Class<S> serviceClass) {
        RestAdapter.Builder builder = new RestAdapter.Builder();
        builder.setEndpoint(BASE_URL);
        builder.setConverter(new RadarJacksonConverter());
        return builder.build().create(serviceClass);
    }
}