package hev.sockstun;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * User-initiated SOCKS5 diagnostics. CONNECT tests a real TCP relay;
 * UDP ASSOCIATE tests command support, NOT end-to-end game UDP traffic.
 */
public final class ProxyProbe {
    private static final int TIMEOUT_MS = 4500;

    private ProxyProbe() {}

    public static String check(String host, int port, String user, String pass) {
        if (host == null || host.trim().isEmpty()) return "Informe o servidor SOCKS5.";
        if (port < 1 || port > 65535) return "Porta SOCKS5 invalida.";
        String tcp = probeTcp(host.trim(), port, user, pass);
        String udp = probeUdpAssociate(host.trim(), port, user, pass);
        return tcp + "\n" + udp + "\nNota: teste UDP real do jogo ainda nao realizado.";
    }

    private static Socket open(String host, int port) throws IOException {
        Socket socket = new Socket();
        try {
            socket.connect(new InetSocketAddress(host, port), TIMEOUT_MS);
            socket.setSoTimeout(TIMEOUT_MS);
            return socket;
        } catch (IOException ex) {
            try { socket.close(); } catch (IOException ignored) {}
            throw ex;
        }
    }

    private static int readByte(InputStream in) throws IOException {
        int value = in.read();
        if (value < 0) throw new IOException("Servidor encerrou a conexao inesperadamente.");
        return value;
    }

    private static void discard(InputStream in, int count) throws IOException {
        for (int i = 0; i < count; ++i) readByte(in);
    }

    private static void negotiate(Socket socket, String user, String pass) throws IOException {
        InputStream in = socket.getInputStream();
        OutputStream out = socket.getOutputStream();
        out.write(new byte[] { 5, 2, 0, 2 });
        out.flush();
        int version = readByte(in);
        int method = readByte(in);
        if (version != 5) throw new IOException("O destino nao respondeu como SOCKS5.");
        if (method == 255) throw new IOException("O servidor recusou os metodos de autenticacao.");
        if (method == 0) return;
        if (method != 2) throw new IOException("Autenticacao SOCKS5 nao suportada: " + method);
        byte[] ub = user.getBytes(StandardCharsets.UTF_8);
        byte[] pb = pass.getBytes(StandardCharsets.UTF_8);
        if (ub.length == 0 || pb.length == 0) throw new IOException("Servidor exige usuario e senha.");
        if (ub.length > 255 || pb.length > 255) throw new IOException("Credenciais SOCKS5 muito longas.");
        out.write(1);
        out.write(ub.length);
        out.write(ub);
        out.write(pb.length);
        out.write(pb);
        out.flush();
        if (readByte(in) != 1 || readByte(in) != 0)
            throw new IOException("O servidor rejeitou as credenciais SOCKS5.");
    }

    private static int readReply(Socket socket) throws IOException {
        InputStream in = socket.getInputStream();
        int version = readByte(in);
        int status = readByte(in);
        readByte(in);  // reserved byte
        int atyp = readByte(in);
        if (version != 5) throw new IOException("Resposta SOCKS5 invalida.");
        if (atyp == 1) discard(in, 4);
        else if (atyp == 4) discard(in, 16);
        else if (atyp == 3) discard(in, readByte(in));
        else throw new IOException("Formato de endereco SOCKS5 inesperado.");
        discard(in, 2); // proxy bind port
        return status;
    }

    private static String probeTcp(String host, int port, String user, String pass) {
        try (Socket socket = open(host, port)) {
            negotiate(socket, user, pass);
            byte[] destination = "example.com".getBytes(StandardCharsets.US_ASCII);
            OutputStream out = socket.getOutputStream();
            out.write(new byte[] { 5, 1, 0, 3, (byte) destination.length });
            out.write(destination);
            out.write(new byte[] { 0, 80 });
            out.flush();
            int status = readReply(socket);
            if (status == 0)
                return "TCP: encaminhamento SOCKS5 CONNECT confirmado (example.com:80).";
            return "TCP: handshake OK, mas CONNECT ao destino recusado (codigo " + status + ").";
        } catch (java.net.UnknownHostException ex) {
            return "TCP: endereco do servidor nao encontrado.";
        } catch (java.net.SocketTimeoutException ex) {
            return "TCP: tempo de resposta esgotado.";
        } catch (IOException ex) {
            return "TCP: " + ex.getMessage();
        } catch (Exception ex) {
            return "TCP: falha de teste (" + ex.getClass().getSimpleName() + ").";
        }
    }

    private static String probeUdpAssociate(String host, int port, String user, String pass) {
        try (Socket socket = open(host, port)) {
            negotiate(socket, user, pass);
            OutputStream out = socket.getOutputStream();
            // RFC 1928 UDP ASSOCIATE; zero address/port when local UDP endpoint is not yet assigned.
            out.write(new byte[] { 5, 3, 0, 1, 0, 0, 0, 0, 0, 0 });
            out.flush();
            int status = readReply(socket);
            if (status == 0)
                return "UDP: servidor aceitou UDP ASSOCIATE; datagramas ainda nao testados.";
            return "UDP: servidor recusou UDP ASSOCIATE (codigo " + status + ").";
        } catch (java.net.SocketTimeoutException ex) {
            return "UDP: tempo de resposta esgotado.";
        } catch (IOException ex) {
            return "UDP: " + ex.getMessage();
        } catch (Exception ex) {
            return "UDP: falha de teste (" + ex.getClass().getSimpleName() + ").";
        }
    }
}
