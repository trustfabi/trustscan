import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class TrustScanCLI {

    private static final int TIMEOUT = 200;
    private static final String VERSION = "1.1.0";

    public static void main(String[] args) {
        if (args.length == 0 || contains(args, "--help")) {
            showHelp();
            return;
        }

        if (contains(args, "--version")) {
            System.out.println("trustscan v" + VERSION + " by @TrustFabi");
            return;
        }

        String host;
        int startPort = 1;
        int endPort = 1024;
        boolean grabBanner = contains(args, "--banner");
        String outputFile = getArgValue(args, "--output");
        boolean jsonOutput = contains(args, "--json");

        int aggression = 1; // Default
        String aggressionStr = getArgValue(args, "--aggression");
        if (aggressionStr != null) {
            try {
                aggression = Math.max(0, Math.min(4, Integer.parseInt(aggressionStr)));
            } catch (NumberFormatException ignored) {}
        }

        List<String> ipRange = getArgValues(args, "--range");
        if (ipRange != null && ipRange.size() == 2) {
            scanIPRange(ipRange.get(0), ipRange.get(1), startPort, endPort, grabBanner, outputFile, jsonOutput, aggression);
            return;
        }

        host = args[0];

        if (contains(args, "--start")) startPort = Integer.parseInt(getArgValue(args, "--start"));
        if (contains(args, "--end")) endPort = Integer.parseInt(getArgValue(args, "--end"));

        showBanner();
        System.out.printf("\u001B[36m📡 Scanning %s from port %d to %d...\n\u001B[0m\n", host, startPort, endPort);

        List<String> results = Collections.synchronizedList(new ArrayList<>());

        if (aggression >= 3) {
            int threads = (aggression == 3) ? 10 : 50;
            ExecutorService executor = Executors.newFixedThreadPool(threads);
            for (int port = startPort; port <= endPort; port++) {
                final int p = port;
                executor.submit(() -> {
                    String result = scanPort(host, p, grabBanner);
                    if (result != null) results.add(result);
                });
            }
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.HOURS);
            } catch (InterruptedException ignored) {}
        } else {
            for (int port = startPort; port <= endPort; port++) {
                String result = scanPort(host, port, grabBanner);
                if (result != null) results.add(result);
                delayBasedOnAggression(aggression);
            }
        }

        outputResults(results, outputFile, jsonOutput);
    }

    private static void scanIPRange(String startIP, String endIP, int startPort, int endPort, boolean banner, String outputFile, boolean jsonOutput, int aggression) {
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        String[] start = startIP.split("\\.");
        String[] end = endIP.split("\\.");

        int s = Integer.parseInt(start[3]);
        int e = Integer.parseInt(end[3]);
        String prefix = start[0] + "." + start[1] + "." + start[2] + ".";

        showBanner();

        for (int i = s; i <= e; i++) {
            String host = prefix + i;
            System.out.printf("\u001B[34m📍 Scanning %s...\u001B[0m\n", host);

            if (aggression >= 3) {
                int threads = (aggression == 3) ? 10 : 50;
                ExecutorService executor = Executors.newFixedThreadPool(threads);
                for (int port = startPort; port <= endPort; port++) {
                    final int p = port;
                    final String h = host;
                    executor.submit(() -> {
                        String result = scanPort(h, p, banner);
                        if (result != null) results.add(result);
                    });
                }
                executor.shutdown();
                try {
                    executor.awaitTermination(1, TimeUnit.HOURS);
                } catch (InterruptedException ignored) {}
            } else {
                for (int port = startPort; port <= endPort; port++) {
                    String result = scanPort(host, port, banner);
                    if (result != null) results.add(result);
                    delayBasedOnAggression(aggression);
                }
            }
        }

        outputResults(results, outputFile, jsonOutput);
    }

    private static String scanPort(String host, int port, boolean grabBanner) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT);
            socket.setSoTimeout(300);
            String service = getService(port);
            String banner = grabBanner ? grabBanner(socket) : "";
            String result = String.format("[OPEN] %s:%d (%s) %s", host, port, service, banner);
            System.out.println("\u001B[32m" + result + "\u001B[0m");
            return result;
        } catch (Exception ignored) {}
        return null;
    }

    private static String grabBanner(Socket socket) {
        try {
            OutputStream os = socket.getOutputStream();
            os.write("HEAD / HTTP/1.0\r\n\r\n".getBytes());
            os.flush();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String line = in.readLine();
            return (line != null && !line.isBlank()) ? "→ " + line.trim() : "";
        } catch (Exception ignored) {}
        return "";
    }

    private static String getService(int port) {
        return switch (port) {
            case 21 -> "FTP";
            case 22 -> "SSH";
            case 23 -> "Telnet";
            case 25 -> "SMTP";
            case 53 -> "DNS";
            case 80 -> "HTTP";
            case 110 -> "POP3";
            case 143 -> "IMAP";
            case 443 -> "HTTPS";
            case 3306 -> "MySQL";
            case 3389 -> "RDP";
            default -> "Unknown";
        };
    }

    private static void outputResults(List<String> results, String outputFile, boolean jsonOutput) {
        try {
            if (outputFile != null) {
                if (jsonOutput) {
                    String json = "[\n" + String.join(",\n", results.stream().map(r -> "\"" + r + "\"").toList()) + "\n]";
                    Files.write(Path.of(outputFile), json.getBytes());
                } else {
                    Files.write(Path.of(outputFile), results);
                }
                System.out.println("\u001B[33m📁 Saved results to " + outputFile + "\u001B[0m");
            }
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    private static boolean contains(String[] args, String key) {
        return Arrays.asList(args).contains(key);
    }

    private static String getArgValue(String[] args, String key) {
        List<String> list = Arrays.asList(args);
        int idx = list.indexOf(key);
        return (idx >= 0 && idx + 1 < list.size()) ? list.get(idx + 1) : null;
    }

    private static List<String> getArgValues(String[] args, String key) {
        String val = getArgValue(args, key);
        if (val != null && val.contains("-")) {
            return List.of(val.split("-"));
        }
        return null;
    }

    private static void delayBasedOnAggression(int level) {
        int delay;
        switch (level) {
            case 0 -> delay = 500;
            case 1 -> delay = 200;
            case 2 -> delay = 100;
            default -> delay = 0;
        }
        if (delay > 0) {
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ignored) {}
        }
    }

    private static void showBanner() {
        String banner = """
\u001B[35m
 _____               _   _____     _     _     
|_   _| __ _   _ ___| |_|  ___|_ _| |__ (_)_ __  
  | || '__| | | / __| __| |_ / _` | '_ \\| | '_ \\ 
  | || |  | |_| \\__ \\ |_|  _| (_| | |_) | | | | |
  |_||_|   \\__,_|___/\\__|_|   \\__,_|_.__/|_|_| |_|
                                                 
      ⚡ trustscan by \u001B[36m@TrustFabi\u001B[35m – follow me on Instagram ⚡
\u001B[0m
""";
        System.out.println(banner);
    }

    private static void showHelp() {
        showBanner();
        System.out.println("📖 USAGE:");
        System.out.println("  trustscan <host> [--start N] [--end M] [--banner] [--output file] [--json] [--aggression N]");
        System.out.println("  trustscan --range 192.168.0.1-192.168.0.10 [--start N] [--end M]");
        System.out.println("\n📌 OPTIONS:");
        System.out.println("  --start       Start port (default: 1)");
        System.out.println("  --end         End port (default: 1024)");
        System.out.println("  --banner      Try to grab banners (e.g. HTTP headers)");
        System.out.println("  --output      Save result to file");
        System.out.println("  --json        Output in JSON format");
        System.out.println("  --range       IP range e.g. 192.168.1.1-192.168.1.10");
        System.out.println("  --aggression  Scan speed: 0 (slow) to 4 (fastest), default = 1");
        System.out.println("  --version     Show version info");
        System.out.println("  --help        Show this help");
    }
}
