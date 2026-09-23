package com.chess.network;

import java.io.*;
import java.net.*;
import java.util.function.Consumer;

/**
 * TCP socket wrapper. Protocol: pipe-delimited lines.
 *   HELLO|playerName|WHITE   — handshake
 *   MOVE|fromRow|fromCol|toRow|toCol
 *   CHAT|playerName|message text
 *   BYE                      — graceful disconnect
 */
public class NetworkManager {

    public static final int PORT = 5000;

    private ServerSocket serverSocket;
    private Socket socket;
    private PrintWriter writer;
    private BufferedReader reader;

    private String localPlayerName = "Oyuncu";
    private String localColor;

    // Callbacks — all fired on the UI thread via uiExecutor
    private Consumer<String> onMoveReceived;   // "fromRow,fromCol,toRow,toCol"
    private Consumer<String> onChatReceived;   // "Name: message"
    private Runnable onConnected;
    private Runnable onDisconnected;
    private Consumer<Runnable> uiExecutor = Runnable::run;

    // ── Setters ──────────────────────────────────────────────────────────────

    public void setLocalPlayerName(String name) { localPlayerName = name; }
    public void setOnMoveReceived(Consumer<String> c) { onMoveReceived = c; }
    public void setOnChatReceived(Consumer<String> c) { onChatReceived = c; }
    public void setOnConnected(Runnable r)    { onConnected = r; }
    public void setOnDisconnected(Runnable r) { onDisconnected = r; }
    public void setUiExecutor(Consumer<Runnable> e) { uiExecutor = e; }

    public String getLocalColor() { return localColor; }
    public String getLocalPlayerName() { return localPlayerName; }

    // ── Host (WHITE) ─────────────────────────────────────────────────────────

    public void startAsHost(Runnable onServerReady) {
        localColor = "WHITE";
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                ui(onServerReady);
                socket = serverSocket.accept(); // blocks until client connects
                setupIO();
                send("HELLO|" + localPlayerName + "|WHITE");
                readLoop();
            } catch (IOException e) {
                if (socket == null) return; // cancelled deliberately
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

    public void sendMove(int fromRow, int fromCol, int toRow, int toCol) {
        send("MOVE|" + fromRow + "|" + fromCol + "|" + toRow + "|" + toCol);
    }

    public void sendChat(String message) {
        send("CHAT|" + localPlayerName + "|" + message.replace("|", " "));
    }

    public void disconnect() {
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
                        ui(() -> { if (onConnected != null) onConnected.run(); });
                        break;
                    case "MOVE":
                        if (p.length >= 5) {
                            String coords = p[1] + "," + p[2] + "," + p[3] + "," + p[4];
                            ui(() -> { if (onMoveReceived != null) onMoveReceived.accept(coords); });
                        }
                        break;
                    case "CHAT":
                        if (p.length >= 3) {
                            String msg = p[1] + ": " + p[2];
                            ui(() -> { if (onChatReceived != null) onChatReceived.accept(msg); });
                        }
                        break;
                    case "BYE":
                        ui(() -> { if (onDisconnected != null) onDisconnected.run(); });
                        return;
                }
            }
        } catch (IOException ignored) {}
        ui(() -> { if (onDisconnected != null) onDisconnected.run(); });
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
