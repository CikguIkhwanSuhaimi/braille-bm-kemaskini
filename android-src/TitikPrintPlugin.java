package my.edu.smkpks.titik;

import android.util.Base64;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.io.OutputStream;
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
}
