package at.jku.cis.radar.rest;

import at.jku.cis.radar.converter.RadarJacksonConverter;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;

public class RestServiceGenerator {

    private static final String BASE_URL = "http://10.0.0.12:8080/rest";

    private static RestAdapter.Builder builder = new RestAdapter.Builder()
            .setEndpoint(BASE_URL).setConverter(new RadarJacksonConverter());

    public static <S> S createService(Class<S> serviceClass) {
        return createService(serviceClass, null);
    }

    public static <S> S createService(Class<S> serviceClass, final String authToken) {
        if (authToken != null) {
            builder.setRequestInterceptor(new RequestInterceptor() {
                @Override
                public void intercept(RequestFacade request) {
                    request.addHeader("Authorization", authToken);
                }
            });
        }

        return builder.build().create(serviceClass);
    }
}