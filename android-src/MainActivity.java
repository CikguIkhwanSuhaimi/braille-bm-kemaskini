package my.edu.smkpks.titik;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(TitikPrintPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
