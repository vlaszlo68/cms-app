package hu.laci.cms.servlet;

import com.google.gson.Gson;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStreamReader;

public abstract class JsonServletSupport extends HttpServlet {

    protected final Gson gson = new Gson();

    protected <T> T readJsonBody(InputStreamReader reader, Class<T> targetType) {
        return gson.fromJson(reader, targetType);
    }

    protected void writeJsonResponse(HttpServletResponse response, int status, Object payload)
            throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);
        response.getWriter().write(gson.toJson(payload));
    }
}
