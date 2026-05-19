package practice2;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;

@WebServlet("/system")
public class SystemInfoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Runtime runtime = Runtime.getRuntime();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        int cpuCores = runtime.availableProcessors();
        double loadAvg = osBean.getSystemLoadAverage();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long maxMemory = runtime.maxMemory();
        String javaVersion = System.getProperty("java.version");
        String userName = System.getProperty("user.name");

        resp.setContentType("text/html; charset=UTF-8");
        PrintWriter out = resp.getWriter();
        out.println("<!DOCTYPE html>");
        out.println("<html><head><title>System Info</title></head><body>");
        out.println("<h1>System Information</h1>");
        out.println("<table border='1' cellpadding='8'>");
        row(out, "OS", osName + " " + osVersion);
        row(out, "Architecture", osArch);
        row(out, "CPU Cores", String.valueOf(cpuCores));
        row(out, "System Load Average", String.format("%.2f", loadAvg));
        row(out, "JVM Total Memory", formatBytes(totalMemory));
        row(out, "JVM Free Memory", formatBytes(freeMemory));
        row(out, "JVM Max Memory", formatBytes(maxMemory));
        row(out, "Java Version", javaVersion);
        row(out, "User", userName);
        out.println("</table>");
        out.println("</body></html>");
    }

    private void row(PrintWriter out, String label, String value) {
        out.println("<tr><td><b>" + label + "</b></td><td>" + value + "</td></tr>");
    }

    private String formatBytes(long bytes) {
        return String.format("%d MB", bytes / (1024 * 1024));
    }
}
