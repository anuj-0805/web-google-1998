import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

public class Main {
    private static final int TOTAL_ROLLS = 100_000;
    private static final String[] WEBSITES = {
        "pune-food-guide.com",
        "best-pizza-pune.com",
        "college-canteen-blog.com",
        "pizza-recipes-home.com",
        "cheap-eats-pune.com",
        "pizza-pizza-pizza.com"
    };

    private static final Path ROOT = Path.of(".");
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/", exchange -> {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendFile(exchange, "index.html", "text/html; charset=UTF-8");
            } else {
                sendText(exchange, 405, "Method not allowed", "text/plain; charset=UTF-8");
            }
        });

        server.createContext("/style.css", exchange ->
            sendFile(exchange, "style.css", "text/css; charset=UTF-8")
        );

        server.createContext("/roll", exchange -> {
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendText(exchange, 405, "Method not allowed", "text/plain; charset=UTF-8");
                return;
            }

            int[] counts = new int[6];
            for (int i = 0; i < TOTAL_ROLLS; i++) {
                int dice = RANDOM.nextInt(6); // 0 to 5, equally likely
                counts[dice]++;
            }

            int winner = 0;
            for (int i = 1; i < counts.length; i++) {
                if (counts[i] > counts[winner]) {
                    winner = i;
                }
            }

            StringBuilder rows = new StringBuilder();
            for (int i = 0; i < WEBSITES.length; i++) {
                double percentage = counts[i] * 100.0 / TOTAL_ROLLS;
                int barWidth = (int) Math.round(percentage * 4); // scale for a readable bar
                rows.append("<div class='result-row'>")
                    .append("<div class='site-line'><span class='site-name'>")
                    .append(i + 1).append(". ").append(escapeHtml(WEBSITES[i]))
                    .append("</span><strong>").append(String.format("%,d", counts[i]))
                    .append("</strong></div>")
                    .append("<div class='bar-track'><div class='bar-fill' style='width:")
                    .append(barWidth).append("%'></div></div>")
                    .append("<div class='percentage'>")
                    .append(String.format(java.util.Locale.US, "%.2f%%", percentage))
                    .append(" of simulated rolls</div></div>");
            }

            String page = Files.readString(ROOT.resolve("index.html"), StandardCharsets.UTF_8)
                .replace("<!-- RESULTS -->", rows.toString())
                .replace("<!-- WINNER -->",
                    "<div class='winner-card'><span class='winner-label'>Highest result in this simulation</span>"
                    + "<h2>" + escapeHtml(WEBSITES[winner]) + "</h2>"
                    + "<p><strong>" + String.format("%,d", counts[winner])
                    + " simulated visits</strong> (" 
                    + String.format(java.util.Locale.US, "%.2f%%", counts[winner] * 100.0 / TOTAL_ROLLS)
                    + ")</p></div>")
                .replace("<!-- TOTAL -->", String.format("%,d", TOTAL_ROLLS));

            sendText(exchange, 200, page, "text/html; charset=UTF-8");
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Open http://localhost:8080 in your browser.");
    }

    private static void sendFile(HttpExchange exchange, String fileName, String contentType) throws IOException {
        Path file = ROOT.resolve(fileName);
        if (!Files.exists(file)) {
            sendText(exchange, 404, "File not found: " + fileName, "text/plain; charset=UTF-8");
            return;
        }
        sendText(exchange, 200, Files.readString(file, StandardCharsets.UTF_8), contentType);
    }

    private static void sendText(HttpExchange exchange, int status, String body, String contentType)
            throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(bytes);
        }
    }

    private static String escapeHtml(String value) {
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#39;");
    }
}
