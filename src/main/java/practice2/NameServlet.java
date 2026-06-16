package practice2;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import practice2.util.TextUtils;
import java.io.IOException;
import java.io.PrintWriter;

@WebServlet("/name")
public class NameServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        String fullName = TextUtils.capitalize("litvinchuk") + TextUtils.capitalize("roman");
        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>Name</title></head><body>");
        out.println("<h1>" + fullName + "</h1>");
        out.println("</body></html>");
    }
}
