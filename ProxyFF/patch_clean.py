#!/usr/bin/env python3
from pathlib import Path
import re,sys
root=Path(sys.argv[1]); main=root/"app/src/main"
manifest=main/"AndroidManifest.xml"
s=manifest.read_text()
s,n=re.subn(r'\s*<service android:name="\.FloatingProxyService"[\s\S]*?</service>','',s,count=1)
assert n==1, "Expected overlay service declaration"
s,n=re.subn(r'\s*<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"\s*/>','',s,count=1)
assert n==1, "Expected overlay permission"
manifest.write_text(s)

src=main/"java/hev/sockstun/MainActivity.java"
s=src.read_text()
s,n=re.subn(r'\n        android.widget.Button overlayButton = \(android.widget.Button\)findViewById\(R.id.overlay_button\);[\s\S]*?\n        }\);', '',s,count=1)
assert n==1, "Expected overlay listener to be removed"
extra=r"""
        final android.widget.Button ffOnly=findViewById(R.id.ff_mode_button);
        final android.widget.Button probeButton=findViewById(R.id.test_proxy_button);
        final android.widget.TextView statusLabel=findViewById(R.id.nx_connection_status);
        ffOnly.setOnClickListener(v -> {
            if (prefs.getEnable()) {
                Toast.makeText(this,"Desconecte a VPN antes de alterar o modo",Toast.LENGTH_LONG).show();
                return;
            }
            java.util.Set<String> selected=new java.util.HashSet<String>();
            selected.add("com.dts.freefireth");
            selected.add("com.dts.freefiremax");
            prefs.setApps(selected);
            prefs.setGlobal(false);
            checkbox_global.setChecked(false);
            updateUI();
            statusLabel.setText("Modo FF salvo: somente Free Fire e Free Fire MAX.");
            Toast.makeText(this,"Tráfego dos dois jogos selecionado para VPN",Toast.LENGTH_LONG).show();
        });
        probeButton.setOnClickListener(v -> {
            final String host=edittext_socks_addr.getText().toString().trim();
            final String portStr=edittext_socks_port.getText().toString().trim();
            final String username=edittext_socks_user.getText().toString();
            final String password=edittext_socks_pass.getText().toString();
            if (host.length()==0 || host.equals("127.0.0.1")) {
                statusLabel.setText("Informe um IP/domínio SOCKS5 real; 127.0.0.1 exige um servidor local.");
                if(host.length()==0)return;
            }
            int port;
            try {port=Integer.parseInt(portStr);if(port<1||port>65535)throw new NumberFormatException();}
            catch(Exception e){statusLabel.setText("Porta inválida. Use 1–65535.");return;}
            probeButton.setEnabled(false);
            statusLabel.setText("Testando acesso ao servidor SOCKS5...");
            new Thread(() -> {
                String result=ProxyProbe.check(host,port,username,password);
                runOnUiThread(() -> {
                    probeButton.setEnabled(true);
                    statusLabel.setText(result);
                });
            }).start();
        });
"""
# Visual-only feature switches: persist UI preference; never claim game integration.
extra += r"""
        final android.content.SharedPreferences demoPrefs=getSharedPreferences("nx_visual",MODE_PRIVATE);
        final android.widget.Switch espVisual=findViewById(R.id.nx_esp_visual);
        final android.widget.Switch aimVisual=findViewById(R.id.nx_aim_visual);
        final android.widget.TextView demoStatus=findViewById(R.id.nx_visual_status);
        espVisual.setChecked(demoPrefs.getBoolean("esp",false));
        aimVisual.setChecked(demoPrefs.getBoolean("aim",false));
        espVisual.setOnCheckedChangeListener((button,checked)->{
            demoPrefs.edit().putBoolean("esp",checked).apply();
            demoStatus.setText("ESP: "+(checked?"visual ligado":"desligado")+" • sem efeito no jogo");
        });
        aimVisual.setOnCheckedChangeListener((button,checked)->{
            demoPrefs.edit().putBoolean("aim",checked).apply();
            demoStatus.setText("AIMBOT: "+(checked?"visual ligado":"desligado")+" • sem efeito no jogo");
        });
"""
anchor="setContentView(R.layout.main);"
assert anchor in s
s=s.replace(anchor,anchor+extra,1)
s=s.replace("Integer.parseInt(edittext_socks_port.getText().toString())","parsePort(edittext_socks_port.getText().toString())")
helper=r"""
    private int parsePort(String val) {
        try {
            int port=Integer.parseInt(val.trim());
            if(port>=1 && port<=65535)return port;
        } catch (Exception ignored) {}
        Toast.makeText(this,"Porta inválida: mantendo 1080",Toast.LENGTH_LONG).show();
        return 1080;
    }
"""
assert s.endswith("}\n")
s=s[:-2]+helper+"}\n"
# Prevent the UI from claiming a real authenticated network connection merely because VPN starts.
s=s.replace("button_control.setText(R.string.control_disable);","button_control.setText(R.string.control_disable);")
src.write_text(s)

layout=main/"res/layout/main.xml"
s=layout.read_text()
s,n=re.subn(r'\s*<Button android:id="@\+id/overlay_button"[\s\S]*?android:backgroundTint="#E50914"\s*/>','',s,count=1)
assert n==1, "Expected the floating button element"
s=s.replace('Ícone do painel','Ícone da proxy')
s=s.replace("NETFLIX X PROXY","NETFLIX X PROXY FF")
s=s.replace("VPN SOCKS5 REAL • REQUER SERVIDOR","VPN SOCKS5 NATIVA  •  CONFIGURAÇÃO DE REDE")
s=s.replace("Preencha o IP e porta de um servidor SOCKS5 confiável. Depois selecione aplicativos e toque em Conectar VPN. Isto não dá aimbot nem ESP.","Uma proxy de rede real requer um servidor SOCKS5 confiável. Selecione o modo FF, teste a conexão e conecte a VPN. Aimbot e ESP não são funções de SOCKS5.")
where='<TextView android:layout_width="match_parent" android:layout_height="wrap_content"\n            android:text="Uma proxy de rede real'
assert where in s, "Header anchor changed"
card='''<LinearLayout
            android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" android:padding="14dp"
            android:background="#171B25" android:layout_marginBottom="16dp">
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="CENTRAL DA CONEXÃO" android:textColor="#FA4E60"
                android:textSize="12sp" android:textStyle="bold"/>
            <TextView android:id="@+id/nx_connection_status"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="VPN desligada • Configure um servidor SOCKS5."
                android:layout_marginTop="10dp" android:layout_marginBottom="10dp"
                android:textColor="#F5F6FA" android:textSize="13sp"/>
            <Button android:id="@+id/ff_mode_button" android:layout_width="match_parent"
                android:layout_height="52dp" android:text="USAR SOMENTE FREE FIRE / MAX"
                android:textColor="#FFFFFF" android:backgroundTint="#A01C28"/>
            <Button android:id="@+id/test_proxy_button" android:layout_width="match_parent"
                android:layout_height="52dp" android:text="TESTAR SERVIDOR SOCKS5"
                android:textColor="#FFFFFF" android:backgroundTint="#E50914"/>
        </LinearLayout>
        '''
feature_card='''<LinearLayout android:layout_width="match_parent" android:layout_height="wrap_content"
            android:orientation="vertical" android:padding="18dp" android:layout_marginBottom="16dp"
            android:background="#181818">
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="🫵  NETFLIX X • CENTRAL FF" android:textColor="#E50914"
                android:textSize="19sp" android:textStyle="bold"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="SEUS CONTROLES" android:textColor="#FFFFFF"
                android:textSize="12sp" android:layout_marginTop="10dp" android:layout_marginBottom="12dp"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="ESP • VISUAL" android:textColor="#FFFFFF" android:textSize="17sp" android:textStyle="bold"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Interface de ativação. Não rastreia inimigos."
                android:textColor="#B3B3B3" android:textSize="12sp"/>
            <Switch android:id="@+id/nx_esp_visual" android:layout_width="match_parent"
                android:layout_height="wrap_content" android:text="Ativar ESP"
                android:textColor="#FFFFFF" android:layout_marginTop="8dp"
                android:thumbTint="#E50914" android:trackTint="#5A2025"/>
            <View android:layout_width="match_parent" android:layout_height="1dp"
                android:layout_marginTop="16dp" android:layout_marginBottom="16dp"
                android:background="#383838"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="AIMBOT • VISUAL" android:textColor="#FFFFFF" android:textSize="17sp" android:textStyle="bold"/>
            <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Interface de ativação. Não altera a mira."
                android:textColor="#B3B3B3" android:textSize="12sp"/>
            <Switch android:id="@+id/nx_aim_visual" android:layout_width="match_parent"
                android:layout_height="wrap_content" android:text="Ativar Aimbot"
                android:textColor="#FFFFFF" android:layout_marginTop="8dp"
                android:thumbTint="#E50914" android:trackTint="#5A2025"/>
            <TextView android:id="@+id/nx_visual_status"
                android:layout_width="match_parent" android:layout_height="wrap_content"
                android:text="Os controles ESP e Aimbot são apenas visuais."
                android:textColor="#B3B3B3" android:textSize="12sp"
                android:layout_marginTop="16dp"/>
        </LinearLayout>
        '''
s=s.replace(where,feature_card+card+where,1)
layout.write_text(s)

strings=main/"res/values/strings.xml"
s=strings.read_text().replace(">Netflix X Proxy</string>",">Netflix X Proxy FF</string>")
strings.write_text(s)
print("Netflix X Proxy FF: Netflix-style visual ESP/AIMBOT switches, FF routing, SOCKS5 probe.")
