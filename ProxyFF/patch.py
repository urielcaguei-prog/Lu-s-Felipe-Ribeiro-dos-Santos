#!/usr/bin/env python3
from pathlib import Path
import sys
import re
root=Path(sys.argv[1])
app=root/"app"
build=app/"build.gradle"
s=build.read_text()
s=s.replace('applicationId "hev.sockstun"','applicationId "br.com.luisfreefire.proxy"')
# Native JNI registration continues under original Java package hev/sockstun.
build.write_text(s)
root_gradle=root/"build.gradle"
s=root_gradle.read_text().replace("jcenter()","mavenCentral()")
root_gradle.write_text(s)
manifest=app/"src/main/AndroidManifest.xml"
s=manifest.read_text()
needle='<application android:label="@string/app_name" android:icon="@mipmap/sockstun"'
assert needle in s, "Manifest format changed"
s=s.replace(needle,'<application android:label="@string/app_name" android:icon="@drawable/skull_neon"')
s=s.replace('''<activity android:name=".MainActivity" android:label="@string/app_name"
				android:excludeFromRecents="true"
			android:exported="true">
			<intent-filter>
				<action android:name="android.intent.action.MAIN"/>
				<category android:name="android.intent.category.LAUNCHER"/>
				<category android:name="android.intent.category.LEANBACK_LAUNCHER" />
			</intent-filter>''',
'''<activity android:name=".GateActivity"
            android:label="@string/app_name"
            android:theme="@android:style/Theme.Material.NoActionBar"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN"/>
                <category android:name="android.intent.category.LAUNCHER"/>
                <category android:name="android.intent.category.LEANBACK_LAUNCHER"/>
            </intent-filter>
        </activity>
        <activity android:name=".MainActivity" android:label="@string/app_name"
                android:excludeFromRecents="false" android:exported="true">''')
assert '.GateActivity' in s
manifest.write_text(s)
src=app/"src/main/java/hev/sockstun/MainActivity.java"
s=src.read_text()
key='super.onCreate(savedInstanceState);'
assert key in s
s=s.replace(key,key+'''
        if (!"luisfreefire".equals(getSharedPreferences("nx_gate",0).getString("access",""))) {
            startActivity(new Intent(this,GateActivity.class));
            finish();return;
        }''',1)
# Always use the defined static red palette instead of Android Monet dynamic colors.
s=s.replace("DynamicColors.applyToActivityIfAvailable(this);","// Custom black and crimson appearance.")
# Refuse to create a VPN without an endpoint, rather than displaying a false active status.
s=s.replace('''boolean isEnable = prefs.getEnable();
			prefs.setEnable(!isEnable);''','''boolean isEnable = prefs.getEnable();
            if (!isEnable && edittext_socks_addr.getText().toString().trim().isEmpty()) {
                Toast.makeText(this,"Configure primeiro o endereço do seu servidor SOCKS5",Toast.LENGTH_LONG).show();
                return;
            }
            prefs.setEnable(!isEnable);''')
src.write_text(s)
strings=app/"src/main/res/values/strings.xml"
s=strings.read_text()
for old,new in {
 "SocksTun":"Netflix X Proxy",
 "Socks Address":"Servidor SOCKS5 • endereço/IP",
 "Socks UDP Address":"Endereço UDP alternativo",
 "Socks Port":"Porta SOCKS5",
 "Socks Username":"Usuário do servidor (opcional)",
 "Socks Password":"Senha SOCKS5 (opcional)",
 "DNS IPv4":"DNS IPv4",
 "DNS IPv6":"DNS IPv6",
 "UDP relay over TCP":"UDP sobre TCP",
 "Remote DNS":"DNS remoto",
 "Global":"Todo o telefone",
 "Apps":"Apps",
 "Save":"Salvar",
 "Enable":"Conectar VPN",
 "Disable":"Desconectar VPN",
 "Search":"Buscar",
 }.items():
  s=s.replace(">"+old+"</string>",">"+new+"</string>")
strings.write_text(s)
theme=app/"src/main/res/values/themes.xml"
theme.write_text('''<?xml version="1.0" encoding="utf-8"?>
<resources>
  <style name="Theme.SocksTun" parent="Theme.Material3.DayNight">
    <item name="colorPrimary">#E50914</item>
    <item name="colorOnPrimary">#FFFFFF</item>
    <item name="colorSecondary">#F23E52</item>
    <item name="colorSurface">#121721</item>
    <item name="colorOnSurface">#F5F6FA</item>
    <item name="android:windowBackground">#090B12</item>
    <item name="android:statusBarColor">#090B12</item>
    <item name="android:navigationBarColor">#090B12</item>
  </style>
</resources>''')
layout=app/"src/main/res/layout/main.xml"
s=layout.read_text()
# Insert a clearly labeled header; avoid making claims about aimbot, ESP or anti-ban.
needle='''android:padding="16dp">'''
header='''android:padding="16dp">
        <LinearLayout
            android:layout_width="match_parent"
            android:layout_height="wrap_content"
            android:orientation="horizontal"
            android:gravity="center_vertical"
            android:padding="12dp"
            android:layout_marginBottom="18dp"
            android:background="#171B25">
            <ImageView android:layout_width="68dp" android:layout_height="68dp"
                android:src="@drawable/skull_neon" android:contentDescription="Ícone do painel"/>
            <LinearLayout android:layout_width="0dp" android:layout_height="wrap_content"
                android:layout_weight="1" android:orientation="vertical" android:paddingStart="12dp">
                <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:text="NETFLIX X PROXY" android:textStyle="bold" android:textColor="#F5F6FA" android:textSize="19sp"/>
                <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
                    android:text="VPN SOCKS5 REAL • REQUER SERVIDOR"
                    android:textSize="10sp" android:textColor="#FA4E60"/>
            </LinearLayout>
        </LinearLayout>
        <TextView android:layout_width="match_parent" android:layout_height="wrap_content"
            android:text="Preencha o IP e porta de um servidor SOCKS5 confiável. Depois selecione aplicativos e toque em Conectar VPN. Isto não dá aimbot nem ESP."
            android:textSize="12sp" android:textColor="#B9BDCA" android:padding="7dp" android:layout_marginBottom="12dp"/>'''
assert needle in s
s=s.replace(needle,header,1)
layout.write_text(s)
print("Customization applied: main, key gate, icon, VPN config, strings, dark theme.")
