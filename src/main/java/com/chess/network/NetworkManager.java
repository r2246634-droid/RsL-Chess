package com.chess.network;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * TCP socket wrapper. Protocol: pipe-delimited lines.
 *   HELLO|playerName|WHITE          — handshake
 *   MOVE|fromRow|fromCol|toRow|toCol|promo   — promo: "" | "Queen" | "Rook" | "Bishop" | "Knight"
 *   CHAT|playerName|message text
 *   RESIGN                          — sender resigns
 *   DRAW_OFFER                      — sender offers a draw
 *   DRAW_RESPONSE|1|0               — 1 = accepted, 0 = declined
 *   BYE                             — graceful disconnect
 */
public class NetworkManager {

    public static final int PORT = 5000;

    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private String localPlayerName = "Oyuncu";
    private String localColor;

    // Süre ayarı: host kendi ayarını HELLO ile yollar, misafir bunu kullanır (iki saat aynı kalsın).
    private long timeInitialMs = -1;
    private long timeIncrementMs = 0;

    // disconnect() bizden geldiyse okuma döngüsünün kapanması "rakip ayrıldı" sayılmaz.
    private volatile boolean closing = false;

    // Callbacks — all fired on the UI thread via uiExecutor
    private Consumer<String> onMoveReceived;   // "fromRow,fromCol,toRow,toCol,promo"
    private Consumer<String> onChatReceived;   // "Name: message"
    private Runnable onConnected;
    private Runnable onDisconnected;
    private Runnable onResignReceived;
    private Runnable onDrawOffered;
    private Consumer<Boolean> onDrawResponse;
    private Consumer<Runnable> uiExecutor = Runnable::run;

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setLocalPlayerName(String name) { localPlayerName = name; }
    public void setOnMoveReceived(Consumer<String> c) { onMoveReceived = c; }
    public void setOnChatReceived(Consumer<String> c) { onChatReceived = c; }
    public void setOnConnected(Runnable r)    { onConnected = r; }
    public void setOnDisconnected(Runnable r) { onDisconnected = r; }
    public void setOnResignReceived(Runnable r)          { onResignReceived = r; }
    public void setOnDrawOffered(Runnable r)             { onDrawOffered = r; }
    public void setOnDrawResponse(Consumer<Boolean> c)   { onDrawResponse = c; }
    public void setUiExecutor(Consumer<Runnable> e) { uiExecutor = e; }

    public String getLocalColor() { return localColor; }
    public String getLocalPlayerName() { return localPlayerName; }

    public void setTimeControl(long initialMs, long incrementMs) {
        timeInitialMs = initialMs;
        timeIncrementMs = incrementMs;
    }
    /** Host'tan gelen (misafirde) ya da kendi ayarlanan (host'ta) süre; bilinmiyorsa -1. */
    public long getTimeInitialMs()   { return timeInitialMs; }
    public long getTimeIncrementMs() { return timeIncrementMs; }

    // ── Host (WHITE) ─────────────────────────────────────────────────────────

    public void startAsHost(Runnable onServerReady) {
        localColor = "WHITE";
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                ui(onServerReady);
                socket = serverSocket.accept(); // blocks until client connects
                setupIO();
                send("HELLO|" + localPlayerName + "|WHITE|" + timeInitialMs + "|" + timeIncrementMs);
                readLoop();
            } catch (IOException e) {
                if (socket == null || closing) return; // cancelled deliberately
                ui(() -> { if (onDisconnected != null) onDisconnected.run(); });
            }
        }, "net-host").start();
    }

    // ── Client (BLACK) ───────────────────────────────────────────────────────

    public void connectToHost(String address) throws IOException {
        localColor = "BLACK";
        socket = new Socket(address, PORT);
        setupIO();
        send("HELLO|" + localPlayerName + "|BLACK");
        new Thread(this::readLoop, "net-client").start();
    }

    // ── Messaging ────────────────────────────────────────────────────────────

    public void sendMove(int fromRow, int fromCol, int toRow, int toCol, String promotionType) {
        send("MOVE|" + fromRow + "|" + fromCol + "|" + toRow + "|" + toCol
                + "|" + (promotionType == null ? "" : promotionType));
    }

    public void sendChat(String message) {
        send("CHAT|" + localPlayerName + "|" + message.replace("|", " "));
    }

    public void sendResign()               { send("RESIGN"); }
    public void sendDrawOffer()            { send("DRAW_OFFER"); }
    public void sendDrawResponse(boolean accepted) { send("DRAW_RESPONSE|" + (accepted ? "1" : "0")); }

    public void disconnect() {
        closing = true;
        try {
            send("BYE");
            if (socket != null)       socket.close();
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void setupIO() throws IOException {
        writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
    }

    private void readLoop() {
        try {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] p = line.split("\\|", -1);
                switch (p[0]) {
                    case "HELLO":
                        if (p.length >= 5 && "WHITE".equals(p[2])) {
                            try {
                                timeInitialMs   = Long.parseLong(p[3]);
                                timeIncrementMs = Long.parseLong(p[4]);
                            } catch (NumberFormatException ignored) {}
                        }
                        ui(() -> { if (onConnected != null) onConnected.run(); });
                        break;
                    case "MOVE":
                        if (p.length >= 5) {
                            String promo = p.length >= 6 ? p[5] : "";
                            String coords = p[1] + "," + p[2] + "," + p[3] + "," + p[4] + "," + promo;
                            ui(() -> { if (onMoveReceived != null) onMoveReceived.accept(coords); });
                        }
                        break;
                    case "CHAT":
                        if (p.length >= 3) {
                            String msg = p[1] + ": " + p[2];
                            ui(() -> { if (onChatReceived != null) onChatReceived.accept(msg); });
                        }
                        break;
                    case "RESIGN":
                        ui(() -> { if (onResignReceived != null) onResignReceived.run(); });
                        break;
                    case "DRAW_OFFER":
                        ui(() -> { if (onDrawOffered != null) onDrawOffered.run(); });
                        break;
                    case "DRAW_RESPONSE":
                        if (p.length >= 2) {
                            boolean accepted = "1".equals(p[1]);
                            ui(() -> { if (onDrawResponse != null) onDrawResponse.accept(accepted); });
                        }
                        break;
                    case "BYE":
                        if (!closing) ui(() -> { if (onDisconnected != null) onDisconnected.run(); });
                        return;
                }
            }
        } catch (IOException ignored) {}
        if (!closing) ui(() -> { if (onDisconnected != null) onDisconnected.run(); });
    }

    private void send(String msg) {
        if (writer != null && !writer.checkError()) writer.println(msg);
    }

    private void ui(Runnable r) { uiExecutor.accept(r); }

    // ── Static util ──────────────────────────────────────────────────────────

    public static String localAddress() {
        try { return InetAddress.getLocalHost().getHostAddress(); }
        catch (UnknownHostException e) { return "127.0.0.1"; }
    }
}
