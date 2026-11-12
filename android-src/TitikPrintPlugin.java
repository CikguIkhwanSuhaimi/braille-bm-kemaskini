package my.edu.smkpks.titik;

import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.InetSocketAddress;
import java.net.Socket;

/** Hantar BRF mentah terus ke embosser melalui TCP port 9100. */
@CapacitorPlugin(name = "TitikPrint")
public class TitikPrintPlugin extends Plugin {
    @PluginMethod
    public void print(final PluginCall call) {
        final String ip = call.getString("ip");
        final String data = call.getString("data");
        if (ip == null || !ip.matches("\\d{1,3}(\\.\\d{1,3}){3}") || data == null || data.isEmpty()) {
            call.reject("IP atau data tidak sah.");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] b = Base64.decode(data, Base64.DEFAULT);
                    if (b.length > 20 * 1024 * 1024) { call.reject("Fail terlalu besar."); return; }
                    Socket s = new Socket();
                    s.connect(new InetSocketAddress(ip, 9100), 6000);
                    OutputStream o = s.getOutputStream();
                    o.write(b);
                    o.flush();
                    s.close();
                    JSObject r = new JSObject();
                    r.put("ok", true);
                    r.put("bytes", b.length);
                    call.resolve(r);
                } catch (Exception e) {
                    call.reject("Tak dapat sambung ke embosser " + ip + ":9100. Semak embosser dihidupkan, IP betul dan telefon di Wi-Fi yang sama. (" + e.getMessage() + ")");
                }
            }
        }).start();
    }

    /** Hantar BRF kepada pelayan cetak di PC (http://PC:8080/print?printer=...), PC menghantar ke pencetak USB. */
    @PluginMethod
    public void printPc(final PluginCall call) {
        final String host = call.getString("host");
        final String printer = call.getString("printer");
        final String data = call.getString("data");
        if (host == null || !host.matches("\\d{1,3}(\\.\\d{1,3}){3}") || printer == null || printer.isEmpty() || data == null || data.isEmpty()) {
            call.reject("Alamat PC, nama pencetak atau data tidak sah.");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    byte[] b = Base64.decode(data, Base64.DEFAULT);
                    if (b.length > 20 * 1024 * 1024) { call.reject("Fail terlalu besar."); return; }
                    int port = 8080;
                    try {
                        HttpURLConnection pc = (HttpURLConnection) new URL("http://" + host + ":8080/ping").openConnection();
                        pc.setConnectTimeout(2500); pc.setReadTimeout(2500);
                        java.io.InputStream pin = pc.getInputStream();
                        byte[] pb = new byte[256]; int pn = pin.read(pb); pin.close();
                        String ptxt = pn > 0 ? new String(pb, 0, pn, "UTF-8") : "";
                        if (!ptxt.contains("pantau")) port = 8765;
                    } catch (Exception pe) { port = 8765; }
                    URL u = new URL("http://" + host + ":" + port + "/print?printer=" + URLEncoder.encode(printer, "UTF-8"));
                    HttpURLConnection c = (HttpURLConnection) u.openConnection();
                    c.setConnectTimeout(6000);
                    c.setReadTimeout(30000);
                    c.setRequestMethod("POST");
                    c.setDoOutput(true);
                    OutputStream o = c.getOutputStream();
                    o.write(b);
                    o.flush();
                    o.close();
                    int code = c.getResponseCode();
                    if (code != 200) { call.reject("PC menjawab kod " + code + ". Semak nama pencetak dan pelayan cetak di PC."); return; }
                    JSObject r = new JSObject();
                    r.put("ok", true);
                    r.put("bytes", b.length);
                    call.resolve(r);
                } catch (Exception e) {
                    call.reject("Tak dapat sambung ke PC " + host + ":8080. Pastikan pelayan cetak jalan di PC dan telefon di Wi-Fi yang sama. (" + e.getMessage() + ")");
                }
            }
        }).start();
    }
}
