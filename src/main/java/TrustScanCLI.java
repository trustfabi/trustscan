import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;

public class TrustScanCLI {

    private static final int TIMEOUT = 200;
    private static final String VERSION = "1.0.0";

    public static void main(String[] args) {
        if (args.length == 0 || contains(args, "--help")) {
            showHelp();
            return;
        }

        if (contains(args, "--version")) {
            System.out.println("trustscan v" + VERSION + " by @TrustFabi");
            return;
        }

        String host = null;
        int startPort = 1;
        int endPort = 1024;
        boolean grabBanner = contains(args, "--banner");
        boolean stealth = contains(args, "--stealth");
        String outputFile = getArgValue(args, "--output");
        boolean jsonOutput = contains(args, "--json");

        List<String> ipRange = getArgValues(args, "--range");
        if (ipRange != null && ipRange.size() == 2) {
            scanIPRange(ipRange.get(0), ipRange.get(1), startPort, endPort, grabBanner, stealth, outputFile, jsonOutput);
            return;
        }

        host = args[0];

        if (contains(args, "--start")) startPort = Integer.parseInt(getArgValue(args, "--start"));
        if (contains(args, "--end")) endPort = Integer.parseInt(getArgValue(args, "--end"));

        showBanner();
        System.out.printf("\u001B[36m📡 Scanning %s from port %d to %d...\n\u001B[0m\n", host, startPort, endPort);

        List<String> results = new ArrayList<>();

        for (int port = startPort; port <= endPort; port++) {
            String result = scanPort(host, port, grabBanner);
            if (result != null) {
                results.add(result);
            }
            if (stealth) {
                try {
                    Thread.sleep(new Random().nextInt(300));
                } catch (InterruptedException ignored) {}
            }
        }

        outputResults(results, outputFile, jsonOutput);
    }

    private static void scanIPRange(String startIP, String endIP, int startPort, int endPort, boolean banner, boolean stealth, String outputFile, boolean jsonOutput) {
        List<String> results = new ArrayList<>();
        String[] start = startIP.split("\\.");
        String[] end = endIP.split("\\.");

        int s = Integer.parseInt(start[3]);
        int e = Integer.parseInt(end[3]);
        String prefix = start[0] + "." + start[1] + "." + start[2] + ".";

        showBanner();

        for (int i = s; i <= e; i++) {
            String host = prefix + i;
            System.out.printf("\u001B[34m📍 Scanning %s...\u001B[0m\n", host);
            for (int port = startPort; port <= endPort; port++) {
                String result = scanPort(host, port, banner);
                if (result != null) results.add(result);
                if (stealth) {
                    try {
                        Thread.sleep(new Random().nextInt(300));
                    } catch (InterruptedException ignored) {}
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
                System.out.println("\u001B[33m📁 Ergebnisse gespeichert in " + outputFile + "\u001B[0m");
            }
        } catch (IOException e) {
            System.err.println("Fehler beim Schreiben in Datei: " + e.getMessage());
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
        System.out.println("  trustscan <host> [--start N] [--end M] [--banner] [--output file] [--json] [--stealth]");
        System.out.println("  trustscan --range 192.168.0.1-192.168.0.10 [--start N] [--end M]");
        System.out.println("\n📌 OPTIONS:");
        System.out.println("  --start       Startport (default: 1)");
        System.out.println("  --end         Endport (default: 1024)");
        System.out.println("  --banner      Versucht Banner-Grabbing (z. B. HTTP Header)");
        System.out.println("  --output      Speichert Ergebnis in Datei");
        System.out.println("  --json        Ausgabe im JSON-Format");
        System.out.println("  --stealth     Verzögert zufällig die Scans für Tarnung");
        System.out.println("  --range       IP-Bereich, z. B. 192.168.1.1-192.168.1.10");
        System.out.println("  --version     Zeigt Versionsinfo");
        System.out.println("  --help        Zeigt diese Hilfe");
    }
}
