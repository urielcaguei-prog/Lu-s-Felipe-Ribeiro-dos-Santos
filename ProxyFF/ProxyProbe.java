package hev.sockstun;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/** SOCKS5 control-channel probe; this verifies a proxy handshake but not UDP game traffic. */
public final class ProxyProbe {
    private ProxyProbe() {}
    public static String check(String host,int port,String user,String pass) {
        try (Socket socket=new Socket()) {
            socket.connect(new InetSocketAddress(host,port),4500);
            socket.setSoTimeout(4500);
            InputStream in=socket.getInputStream();
            OutputStream out=socket.getOutputStream();
            // Offer both no-auth and username/password, as defined by RFC 1928.
            out.write(new byte[]{5,2,0,2});out.flush();
            int version=in.read(),method=in.read();
            if(version!=5)return "O servidor respondeu, mas não é um SOCKS5 válido.";
            if(method==255)return "O servidor SOCKS5 recusou os métodos de autenticação.";
            if(method==0)return "● SOCKS5 respondeu: conexão TCP e negociação sem senha OK. UDP ainda não verificado.";
            if(method!=2)return "SOCKS5 respondeu com método de autenticação não compatível: "+method;
            byte[] ub=user.getBytes(StandardCharsets.UTF_8),pb=pass.getBytes(StandardCharsets.UTF_8);
            if(ub.length==0||pb.length==0)return "SOCKS5 exige usuário e senha. Preencha os campos e teste novamente.";
            if(ub.length>255||pb.length>255)return "Usuário ou senha SOCKS5 muito longos.";
            out.write(1);out.write(ub.length);out.write(ub);
            out.write(pb.length);out.write(pb);out.flush();
            int authVersion=in.read(),authStatus=in.read();
            if(authVersion==1&&authStatus==0)return "● SOCKS5 respondeu: autenticação validada. UDP ainda não verificado.";
            return "SOCKS5 respondeu, mas a autenticação foi rejeitada.";
        } catch(java.net.UnknownHostException ex) {
            return "Servidor não encontrado. Verifique o host/IP.";
        } catch(java.net.SocketTimeoutException ex) {
            return "Tempo de conexão esgotado. Verifique rede e servidor.";
        } catch(Exception ex) {
            return "Sem conexão SOCKS5: "+ex.getClass().getSimpleName();
        }
    }
}
