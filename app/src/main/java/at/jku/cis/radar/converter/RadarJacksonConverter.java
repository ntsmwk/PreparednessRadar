package at.jku.cis.radar.converter;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;

import retrofit.converter.ConversionException;
import retrofit.converter.JacksonConverter;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

public class RadarJacksonConverter extends JacksonConverter {

    private static final String ENCODING = "UTF-8";
    private static final String MIME_TYPE = "application/json; charset=" + ENCODING;

    @Override
    public Object fromBody(TypedInput body, Type type) throws ConversionException {
        if (isJsonObjectType(type)) {
            return fromBody(body);
        }
        return super.fromBody(body, type);
    }

    private boolean isJsonObjectType(Type type) {
        return type.toString().endsWith(JSONObject.class.getSimpleName());
    }

    private JSONObject fromBody(TypedInput body) throws ConversionException {
        try {
            return new JSONObject(IOUtils.toString(body.in(), ENCODING));
        } catch (JSONException | IOException e) {
            throw new ConversionException(e);
        }
    }

    @Override
    public TypedOutput toBody(Object object) {
        if (object instanceof JSONObject) {
            return toBody((JSONObject) object);
        }
        return super.toBody(object);
    }

    private TypedOutput toBody(JSONObject jsonObject) {
        try {
            return new TypedByteArray(MIME_TYPE, jsonObject.toString().getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
