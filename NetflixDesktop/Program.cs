using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Diagnostics;
using System.IO;
using System.Net.Sockets;
using System.Text.Json;
using System.Windows.Forms;
using System.Threading.Tasks;

namespace NetflixPainelPC;

static class Palette {
 public static Color Bg=Color.FromArgb(9,10,14), Card=Color.FromArgb(22,25,31), Ink=Color.FromArgb(240,242,246),
 Muted=Color.FromArgb(155,161,173), Red=Color.FromArgb(229,9,20), Line=Color.FromArgb(55,58,68), Green=Color.FromArgb(64,216,137);
 public static Font Font(float size,bool bold=false) => new("Segoe UI",size,bold?FontStyle.Bold:FontStyle.Regular);
 public static Label Text(string s,int x,int y,int w,int h,float size=11,bool bold=false,Color? color=null) =>
  new(){Text=s,Location=new(x,y),Size=new(w,h),ForeColor=color??Ink,BackColor=Color.Transparent,Font=Font(size,bold),TextAlign=ContentAlignment.MiddleLeft};
 public static Button Btn(string s,int x,int y,int w,int h,Action a,bool accent=false){
  var b=new Button{Text=s,Location=new(x,y),Size=new(w,h),Font=Font(9.3f,true),
   FlatStyle=FlatStyle.Flat,BackColor=accent?Red:Color.FromArgb(42,46,55),ForeColor=Ink,Cursor=Cursors.Hand};
  b.FlatAppearance.BorderSize=0;b.Click+=(_,_)=>a();return b;
 }
 public static Panel Box(int x,int y,int w,int h)=>new(){Location=new(x,y),Size=new(w,h),BackColor=Card};
 public static TrackBar Slider(int x,int y,int w,int min,int max,int val,Action<int> change){
  var s=new TrackBar{Location=new(x,y),Size=new(w,48),Minimum=min,Maximum=max,Value=Math.Clamp(val,min,max),
   TickStyle=TickStyle.None,BackColor=Card,ForeColor=Red};
  s.Scroll+=(_,_)=>change(s.Value);return s;
 }
 public static void Round(Control c,int radius){
  if(c.Width<1||c.Height<1)return;
  using var path=new GraphicsPath();int r=Math.Min(radius,Math.Min(c.Width,c.Height)/2);
  path.AddArc(0,0,r*2,r*2,180,90);path.AddArc(c.Width-r*2,0,r*2,r*2,270,90);
  path.AddArc(c.Width-r*2,c.Height-r*2,r*2,r*2,0,90);path.AddArc(0,c.Height-r*2,r*2,r*2,90,90);
  path.CloseFigure();c.Region?.Dispose();c.Region=new Region(path);
 }
}
sealed class AppOptions {
 public int Opacity {get;set;}=95;
 public int BubbleSize {get;set;}=70;
 public int Spin {get;set;}=65;
 public bool Rotate {get;set;}=true;
 public bool KeepOnTop {get;set;}=true;
 public string Target {get;set;}="CABEÇA";
 public string EmulatorPath {get;set;}="";
 public string SocksHost {get;set;}="";
 public int SocksPort {get;set;}=1080;
 public static string FileName=>Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData),"NetflixFloatingPanel","settings.json");
 public static AppOptions Load(){try{return JsonSerializer.Deserialize<AppOptions>(File.ReadAllText(FileName))??new();}catch{return new();}}
 public void Save(){try{Directory.CreateDirectory(Path.GetDirectoryName(FileName)!);File.WriteAllText(FileName,JsonSerializer.Serialize(this,new JsonSerializerOptions{WriteIndented=true}));}catch{}}
}
sealed class ModelView:Control {
 readonly AppOptions config;
 readonly System.Windows.Forms.Timer timer=new(){Interval=36};
 double angle=0;
 public ModelView(AppOptions s){
  config=s;DoubleBuffered=true;BackColor=Color.FromArgb(12,19,25);
  timer.Tick+=(_,_)=>{if(config.Rotate){angle+=.016*Math.Max(1,config.Spin)/65d;Invalidate();}};
  timer.Start();
 }
 PointF P(double x,double y,double z,float cx,float cy,float scale){
  double xx=x*Math.Cos(angle)+z*Math.Sin(angle);
  double zz=-x*Math.Sin(angle)+z*Math.Cos(angle);
  double factor=2.6/(2.6+zz*.25);
  return new((float)(cx+xx*scale*factor),(float)(cy-y*scale*factor));
 }
 protected override void OnPaint(PaintEventArgs e){
  base.OnPaint(e);var g=e.Graphics;g.SmoothingMode=SmoothingMode.AntiAlias;
  g.Clear(BackColor);var cx=Width*.53f;var cy=Height*.54f;float scale=Math.Min(Width*.27f,Height*.29f);
  using var gridPen=new Pen(Color.FromArgb(33,52,62),1);
  for(int x=0;x<Width;x+=27)g.DrawLine(gridPen,x,0,x,Height);
  for(int y=0;y<Height;y+=27)g.DrawLine(gridPen,0,y,Width,y);
  using var ring=new Pen(Color.FromArgb(90,229,9,20),1.5f);
  g.DrawEllipse(ring,cx-scale*.9f,cy-scale*1.65f,scale*1.8f,scale*1.8f);
  g.DrawEllipse(gridPen,cx-scale*.85f,cy+scale*1.02f,scale*1.7f,scale*.22f);
  void Segment(double x,double y,double z,double x2,double y2,double z2,Color clr,float thickness){
   using var pen=new Pen(clr,thickness){StartCap=LineCap.Round,EndCap=LineCap.Round};
   g.DrawLine(pen,P(x,y,z,cx,cy,scale),P(x2,y2,z2,cx,cy,scale));
  }
  void Joint(double x,double y,double z,Color clr,int r){
   var pt=P(x,y,z,cx,cy,scale);using var b=new SolidBrush(clr);
   g.FillEllipse(b,pt.X-r,pt.Y-r,r*2,r*2);
  }
  Segment(-.25,-.12,0,-.3,-.82,.02,Color.FromArgb(102,133,150),14);
  Segment(.25,-.12,0,.3,-.82,.02,Color.FromArgb(133,159,176),14);
  Segment(-.3,-.82,.02,-.33,-1.30,-.05,Color.FromArgb(93,119,135),12);
  Segment(.3,-.82,.02,.33,-1.30,-.05,Color.FromArgb(115,143,161),12);
  Segment(-.44,.9,0,-.64,.32,0,Color.FromArgb(125,151,164),12);
  Segment(.44,.9,0,.64,.32,0,Color.FromArgb(143,168,179),12);
  Segment(-.64,.32,0,-.59,-.13,.2,Color.FromArgb(99,128,143),10);
  Segment(.64,.32,0,.59,-.13,.2,Color.FromArgb(121,152,168),10);
  Segment(0,-.07,0,0,1.06,0,Color.FromArgb(124,151,167),31);
  Segment(-.25,.73,0,.25,.73,0,Color.FromArgb(208,228,235),2);
  Segment(0,1.06,0,0,1.41,0,Color.FromArgb(165,187,201),11);
  Joint(0,1.41,0,Color.FromArgb(199,220,230),17);
  Joint(0,1.4,.19,Palette.Red,3);
  var mark=config.Target=="PEITO"?P(0,.61,.1,cx,cy,scale):config.Target=="PESCOÇO"?P(0,1.06,0,cx,cy,scale):P(0,1.41,0,cx,cy,scale);
  using var rp=new Pen(Palette.Red,2);
  g.DrawEllipse(rp,mark.X-24,mark.Y-24,48,48);
  g.DrawLine(rp,mark.X-36,mark.Y,mark.X-19,mark.Y);
  g.DrawLine(rp,mark.X+19,mark.Y,mark.X+36,mark.Y);
  g.DrawLine(rp,mark.X,mark.Y-36,mark.X,mark.Y-19);
  g.DrawLine(rp,mark.X,mark.Y+19,mark.X,mark.Y+36);
  using var f=Palette.Font(10,true);using var brush=new SolidBrush(Palette.Muted);
  g.DrawString("3D MODEL  //  VISUAL PREVIEW",f,brush,13,13);
  using var redBrush=new SolidBrush(Palette.Red);g.DrawString(config.Target+"  •  360°",f,redBrush,13,33);
  g.DrawString("NOT CONNECTED TO GAME",f,brush,13,Height-30);
 }
 protected override void Dispose(bool disposing){if(disposing){timer.Stop();timer.Dispose();}base.Dispose(disposing);}
}
sealed class BubbleForm:Form {
 readonly Action open;Point pressed;Point startLoc;bool dragged=false;
 public BubbleForm(AppOptions s,Action onClick){open=onClick;FormBorderStyle=FormBorderStyle.None;ShowInTaskbar=false;TopMost=true;
  StartPosition=FormStartPosition.Manual;Location=new Point(30,145);BackColor=Palette.Bg;Size=new(s.BubbleSize,s.BubbleSize);DoubleBuffered=true;
  MouseDown+=(_,e)=>{pressed=Cursor.Position;startLoc=Location;dragged=false;};
  MouseMove+=(_,e)=>{if(e.Button!=MouseButtons.Left)return;var delta=new Size(Cursor.Position.X-pressed.X,Cursor.Position.Y-pressed.Y);
   if(Math.Abs(delta.Width)+Math.Abs(delta.Height)>6)dragged=true;
   if(dragged)Location=new(startLoc.X+delta.Width,startLoc.Y+delta.Height);
  };
  MouseUp+=(_,e)=>{if(e.Button==MouseButtons.Right)Close();else if(!dragged)open();};
  Resize+=(_,_)=>{Palette.Round(this,Math.Min(Width,Height)/2);Invalidate();};Palette.Round(this,Size.Width/2);
 }
 public void ResizeBubble(int size){Size=new(Math.Clamp(size,52,112),Math.Clamp(size,52,112));Invalidate();}
 protected override void OnPaint(PaintEventArgs e){
  e.Graphics.SmoothingMode=SmoothingMode.AntiAlias;e.Graphics.Clear(Palette.Bg);
  using var p=new Pen(Palette.Red,3);e.Graphics.DrawEllipse(p,2,2,Width-5,Height-5);
  using var f=new Font("Arial",Math.Max(26,Width*.55f),FontStyle.Bold);
  using var b=new SolidBrush(Palette.Red);
  using var sf=new StringFormat{Alignment=StringAlignment.Center,LineAlignment=StringAlignment.Center};
  e.Graphics.DrawString("N",f,b,new RectangleF(0,-2,Width,Height),sf);
 }
}
sealed class FloatingForm:Form {
 readonly AppOptions config;readonly Action minimize;readonly Action stop;readonly Action<Action>? openTools;
 Panel body=new();ModelView? model;Point down;Point old;bool dragging;
 public FloatingForm(AppOptions o,Action min,Action shutdown,Action<Action>? unused=null){
  config=o;minimize=min;stop=shutdown;openTools=unused;
  Text="Netflix • Flutuante";FormBorderStyle=FormBorderStyle.None;ShowInTaskbar=false;TopMost=config.KeepOnTop;
  StartPosition=FormStartPosition.Manual;Location=new(70,100);Size=new(380,595);BackColor=Palette.Bg;
  Opacity=Math.Clamp(config.Opacity/100d,.40,1);
  var heading=new Panel{Dock=DockStyle.Top,Height=52,BackColor=Palette.Card};Controls.Add(heading);
  heading.Controls.Add(Palette.Text("N",12,4,38,40,22,true,Palette.Red));
  heading.Controls.Add(Palette.Text("NETFLIX   /   FLOAT",57,6,210,36,12,true));
  heading.Controls.Add(Palette.Btn("—",300,8,52,35,()=>minimize()));
  heading.MouseDown+=(_,e)=>{down=Cursor.Position;old=Location;dragging=true;};
  heading.MouseMove+=(_,e)=>{if(dragging&&e.Button==MouseButtons.Left){var d=new Size(Cursor.Position.X-down.X,Cursor.Position.Y-down.Y);Location=new(old.X+d.Width,old.Y+d.Height);}};
  heading.MouseUp+=(_,_)=>dragging=false;
  body=new Panel{Dock=DockStyle.Fill,AutoScroll=true,BackColor=Palette.Bg};Controls.Add(body);body.BringToFront();
  SetPage(0);
 }
 void SetPage(int index){
  if(model!=null){model.Dispose();model=null;}
  body.Controls.Clear();
  var tabs=new[]{"PRÉVIA","AJUSTES","STATUS"};
  for(int k=0;k<3;k++){int num=k;body.Controls.Add(Palette.Btn(tabs[k],10+118*k,10,111,38,()=>SetPage(num),k==index));}
  if(index==0)Preview();else if(index==1)Settings();else Status();
  body.Controls.Add(Palette.Btn("MINIMIZAR",10,493,173,43,()=>minimize()));
  body.Controls.Add(Palette.Btn("ENCERRAR",193,493,173,43,()=>stop()));
  body.Controls.Add(Palette.Text("APP INDEPENDENTE  •  NÃO ALTERA PARTIDAS",13,545,350,23,8,false,Palette.Muted));
 }
 void Preview(){
  body.Controls.Add(Palette.Text("MODELO ANIMADO  /  VISUAL",12,61,350,28,11,true,Palette.Muted));
  model=new ModelView(config){Location=new(10,89),Size=new(356,264)};body.Controls.Add(model);
  body.Controls.Add(Palette.Text("ÁREA DESTACADA",12,363,350,25,10,true,Palette.Muted));
  int x=10;foreach(var tgt in new[]{"CABEÇA","PESCOÇO","PEITO"}){
   var key=tgt;body.Controls.Add(Palette.Btn(tgt,x,397,110,39,()=>{config.Target=key;config.Save();SetPage(0);},config.Target==tgt));x+=118;
  }
  body.Controls.Add(Palette.Text("Marcadores são ilustrativos, não dados do jogo.",12,449,355,35,10,false,Palette.Muted));
 }
 void Settings(){
  body.Controls.Add(Palette.Text("OPACIDADE  /  APLICADA À JANELA",13,75,350,26,10,true));
  var alpha=Palette.Slider(15,108,350,45,100,config.Opacity,v=>{config.Opacity=v;Opacity=v/100d;config.Save();});body.Controls.Add(alpha);
  body.Controls.Add(Palette.Text("TAMANHO DO ÍCONE",13,169,340,26,10,true));
  body.Controls.Add(Palette.Slider(15,202,350,52,112,config.BubbleSize,v=>{config.BubbleSize=v;config.Save();}));
  body.Controls.Add(Palette.Text("VELOCIDADE DE ROTAÇÃO",13,266,340,26,10,true));
  body.Controls.Add(Palette.Slider(15,301,350,15,150,config.Spin,v=>{config.Spin=v;config.Save();}));
  var check=new CheckBox{Text="Animar boneco 360°",Location=new(18,371),Size=new(300,35),
   ForeColor=Palette.Ink,Font=Palette.Font(10),Checked=config.Rotate};
  check.CheckedChanged+=(_,_)=>{config.Rotate=check.Checked;config.Save();};body.Controls.Add(check);
  body.Controls.Add(Palette.Text("Alterações são salvas automaticamente.",14,440,340,35,10,false,Palette.Muted));
 }
 void Status(){
  string[] lines={
   "●  Janela flutuante: ativa",
   $"●  Plataforma: Windows {Environment.OSVersion.Version}",
   $"●  Processo do painel: {Environment.ProcessId}",
   "○  Aimbot: não implementado",
   "○  ESP sobre jogadores: não implementado",
   "○  Antiban: não implementado",
   "○  Proxy do jogo: não implementado",
   "●  Visualização 360°: disponível"
  };
  int y=83;foreach(string line in lines){body.Controls.Add(Palette.Text(line,17,y,350,36,10,false,
   line.StartsWith("●")?Palette.Green:Palette.Muted));y+=47;}
 }
}
sealed class MainForm:Form {
 readonly AppOptions config=AppOptions.Load();
 BubbleForm? bubble;FloatingForm? floating;Panel page=new();int index=0;
 public MainForm(){
  Text="Netflix • Float Panel for Windows";StartPosition=FormStartPosition.CenterScreen;Size=new(1080,725);
  MinimumSize=new(980,660);BackColor=Palette.Bg;ForeColor=Palette.Ink;
  var left=new Panel{Dock=DockStyle.Left,Width=235,BackColor=Color.FromArgb(15,17,23)};Controls.Add(left);
  left.Controls.Add(Palette.Text("N",27,24,49,56,42,true,Palette.Red));
  left.Controls.Add(Palette.Text("NETFLIX",76,36,130,37,20,true));
  left.Controls.Add(Palette.Text("FLOAT PANEL  •  WINDOWS",25,88,190,33,9,false,Palette.Muted));
  string[] nav={"VISÃO GERAL","APARÊNCIA","FERRAMENTAS","DIAGNÓSTICO"};
  for(int i=0;i<4;i++){int j=i;left.Controls.Add(Palette.Btn(nav[i],20,158+i*62,194,47,()=>Switch(j),i==index));}
  left.Controls.Add(Palette.Text("VISUAL PERSONALIZADO",25,550,190,32,9,true,Palette.Red));
  left.Controls.Add(Palette.Text("Sem integração com memória,\ninjeção ou anticheat do jogo.",25,583,197,56,8.6f,false,Palette.Muted));
  var workspace=new Panel{Dock=DockStyle.Fill,BackColor=Palette.Bg};Controls.Add(workspace);
  var top=new Panel{Dock=DockStyle.Top,Height=89,BackColor=Palette.Bg};workspace.Controls.Add(top);
  top.Controls.Add(Palette.Text("PAINEL DE CONTROLE",25,14,650,43,23,true));
  top.Controls.Add(Palette.Text("Interface nativa para Windows • versão 2.0",26,56,680,25,10,false,Palette.Muted));
  page=new Panel{Dock=DockStyle.Fill,AutoScroll=true,BackColor=Palette.Bg};workspace.Controls.Add(page);page.BringToFront();
  page.Padding=new Padding(20);Switch(0);
  FormClosing+=(_,_)=>{bubble?.Close();floating?.Close();};
 }
 void Switch(int n){index=n;page.Controls.Clear();
  if(n==0)Overview();else if(n==1)Appearance();else if(n==2)Tools();else Diagnostics();
 }
 Panel PanelAt(int x,int y,int w,int h){var c=Palette.Box(x,y,w,h);page.Controls.Add(c);return c;}
 void Heading(Panel p,string title,string caption){
  p.Controls.Add(Palette.Text(title,22,16,p.Width-42,36,17,true));
  p.Controls.Add(Palette.Text(caption,22,53,p.Width-42,33,9.5f,false,Palette.Muted));
 }
 void Overview(){
  var intro=PanelAt(14,16,768,114);intro.Controls.Add(Palette.Text("ÍCONE FLUTUANTE NATIVO",20,12,480,36,20,true));
  intro.Controls.Add(Palette.Text("Controle a aparência sobre outros programas, sem precisar abrir um navegador.",21,56,680,35,10,false,Palette.Muted));
  var modelCard=PanelAt(14,147,415,446);Heading(modelCard,"PRÉVIA 360°","Boneco ilustrativo — não rastreia inimigos.");
  var model=new ModelView(config){Location=new(18,95),Size=new(380,330)};modelCard.Controls.Add(model);
  var right=PanelAt(445,147,337,446);Heading(right,"INICIAR PAINEL","Ative e personalize seu menu.");
  right.Controls.Add(Palette.Btn("ATIVAR ÍCONE FLUTUANTE",20,110,296,49,StartBubble,true));
  right.Controls.Add(Palette.Btn("ABRIR MENU FLUTUANTE",20,171,296,48,OpenFloating));
  right.Controls.Add(Palette.Btn("ENCERRAR ÍCONE",20,232,296,46,StopFloating));
  right.Controls.Add(Palette.Text("● Tamanho e opacidade ajustáveis",19,297,306,32,10,false,Palette.Green));
  right.Controls.Add(Palette.Text("● Prévia 3D e indicador de região",19,332,306,32,10,false,Palette.Green));
  right.Controls.Add(Palette.Text("○ Sem Aimbot / ESP no Free Fire",19,367,306,32,10,false,Palette.Muted));
 }
 void Appearance(){
  var card=PanelAt(14,16,768,512);Heading(card,"APARÊNCIA E ANIMAÇÕES","Todas essas configurações alteram o aplicativo.");
  void SetSlider(string label,int top,int min,int max,int value,Action<int> action){
   card.Controls.Add(Palette.Text(label,24,top,450,28,11,true));
   card.Controls.Add(Palette.Slider(24,top+36,680,min,max,value,v=>{action(v);config.Save();}));
  }
  SetSlider("OPACIDADE DA JANELA FLUTUANTE",99,45,100,config.Opacity,v=>{config.Opacity=v;if(floating!=null)floating.Opacity=v/100d;});
  SetSlider("TAMANHO DO ÍCONE",194,52,112,config.BubbleSize,v=>{config.BubbleSize=v;bubble?.ResizeBubble(v);});
  SetSlider("VELOCIDADE DA ROTAÇÃO 360°",289,15,150,config.Spin,v=>config.Spin=v);
  var check=new CheckBox{Text="Animar o boneco",Location=new(27,392),Size=new(240,40),
   ForeColor=Palette.Ink,Font=Palette.Font(11),Checked=config.Rotate};
  check.CheckedChanged+=(_,_)=>{config.Rotate=check.Checked;config.Save();};card.Controls.Add(check);
  var topmost=new CheckBox{Text="Janela flutuante sempre no topo",Location=new(288,392),Size=new(390,40),
   ForeColor=Palette.Ink,Font=Palette.Font(11),Checked=config.KeepOnTop};
  topmost.CheckedChanged+=(_,_)=>{config.KeepOnTop=topmost.Checked;if(floating!=null)floating.TopMost=config.KeepOnTop;config.Save();};card.Controls.Add(topmost);
  card.Controls.Add(Palette.Btn("RESTAURAR PADRÕES",25,456,217,42,()=>{StopFloating();var defaults=new AppOptions();config.Opacity=defaults.Opacity;config.BubbleSize=defaults.BubbleSize;
   config.Spin=defaults.Spin;config.Rotate=defaults.Rotate;config.KeepOnTop=defaults.KeepOnTop;config.Target=defaults.Target;config.Save();Switch(1);}));
 }
 void Tools(){
  var select=PanelAt(14,16,768,212);Heading(select,"MARCADOR DA PRÉVIA","Selecione a região destacada no boneco. É apenas visual.");
  int x=24;foreach(var t in new[]{"CABEÇA","PESCOÇO","PEITO"}){string key=t;select.Controls.Add(Palette.Btn(t,x,119,200,46,()=>{config.Target=key;config.Save();Switch(2);},config.Target==t));x+=224;}
  var emu=PanelAt(14,247,768,265);Heading(emu,"ATALHOS DO EMULADOR","Escolha um programa Windows para abrir pelo painel.");
  var box=new TextBox{Location=new(24,123),Size=new(480,37),Font=Palette.Font(10),Text=config.EmulatorPath,BackColor=Color.FromArgb(37,40,49),ForeColor=Palette.Ink,BorderStyle=BorderStyle.FixedSingle};emu.Controls.Add(box);
  emu.Controls.Add(Palette.Btn("LOCALIZAR .EXE",520,121,218,42,()=>{using var dlg=new OpenFileDialog{Filter="Aplicativo Windows (*.exe)|*.exe"};
   if(dlg.ShowDialog(this)==DialogResult.OK){config.EmulatorPath=dlg.FileName;config.Save();box.Text=config.EmulatorPath;}}));
  emu.Controls.Add(Palette.Btn("INICIAR PROGRAMA CONFIGURADO",24,188,714,47,()=>{
   if(File.Exists(config.EmulatorPath)){try{Process.Start(new ProcessStartInfo(config.EmulatorPath){UseShellExecute=true});}
    catch(Exception ex){MessageBox.Show(this,ex.Message,"Erro ao abrir");}}
   else MessageBox.Show(this,"Escolha primeiro o arquivo executável do seu emulador.","Programa não encontrado");
  },true));
 }
 void Diagnostics(){
  var card=PanelAt(14,16,768,548);Heading(card,"DIAGNÓSTICOS","Verificações do sistema e rede. Sem alterar o tráfego de jogos.");
  var state=Palette.Text("Aguardando verificação...",22,97,720,35,11,false,Palette.Muted);card.Controls.Add(state);
  card.Controls.Add(Palette.Btn("VERIFICAR EMULADORES INSTALADOS/ATIVOS",22,140,715,45,()=>{
   string[] names={"HD-Player","dnplayer","Nox","MEmu","AndroidEmulator","GameLoop"};
   var found=new System.Collections.Generic.List<string>();
   foreach(var name in names){try{foreach(var p in Process.GetProcessesByName(name)){found.Add(name+" (PID "+p.Id+")");p.Dispose();}}catch{}}
   state.Text=found.Count>0?"● Encontrado: "+string.Join("; ",found):"○ Nenhum processo de emulador conhecido foi encontrado.";
  }));
  card.Controls.Add(Palette.Text("TESTE DE SOCKS5 (CONEXÃO PRÓPRIA)",22,210,650,29,11,true));
  var host=new TextBox{Location=new(22,245),Size=new(475,38),Text=config.SocksHost,Font=Palette.Font(10),BackColor=Color.FromArgb(37,40,49),ForeColor=Palette.Ink};card.Controls.Add(host);
  var port=new NumericUpDown{Location=new(513,245),Size=new(215,37),Minimum=1,Maximum=65535,Value=Math.Clamp(config.SocksPort,1,65535),Font=Palette.Font(10)};card.Controls.Add(port);
  var result=Palette.Text("Sem conexão testada.",22,342,715,78,10,false,Palette.Muted);card.Controls.Add(result);
  card.Controls.Add(Palette.Btn("TESTAR SERVIDOR SOCKS5",22,296,715,45,async()=>{
   string h=host.Text.Trim();int n=(int)port.Value;config.SocksHost=h;config.SocksPort=n;config.Save();
   if(h.Length==0){result.Text="Informe o endereço do servidor.";return;}
   result.Text="Testando conexão...";try{
    using var tcp=new TcpClient();await tcp.ConnectAsync(h,n).WaitAsync(TimeSpan.FromSeconds(4));
    using var stream=tcp.GetStream();await stream.WriteAsync(new byte[]{5,1,0});
    var buf=new byte[2];int read=await stream.ReadAsync(buf).AsTask().WaitAsync(TimeSpan.FromSeconds(4));
    result.Text=read==2&&buf[0]==5&&buf[1]==0?"● Servidor SOCKS5 respondeu (método sem autenticação).":
     "○ Resposta recebida, mas não houve confirmação de SOCKS5 sem autenticação.";
   }catch(Exception ex){result.Text="○ Falha na conexão: "+ex.GetType().Name;}
  },true));
  card.Controls.Add(Palette.Text("Este teste verifica apenas um servidor informado por você. Não cria VPN nem contorna anticheat.",22,451,713,50,9,false,Palette.Muted));
 }
 void StartBubble(){if(bubble!=null&&!bubble.IsDisposed){bubble.Show();bubble.BringToFront();return;}
  bubble=new BubbleForm(config,OpenFloating);bubble.Show(this);}
 void OpenFloating(){if(floating==null||floating.IsDisposed)floating=new FloatingForm(config,MinimizeFloating,StopFloating);
  if(bubble!=null&&!bubble.IsDisposed)bubble.Hide();
  floating.TopMost=config.KeepOnTop;floating.Opacity=config.Opacity/100d;floating.Show(this);floating.BringToFront();}
 void MinimizeFloating(){floating?.Hide();StartBubble();}
 void StopFloating(){if(floating!=null&&!floating.IsDisposed){floating.Close();floating=null;}if(bubble!=null&&!bubble.IsDisposed){bubble.Close();bubble=null;}}
}
static class Program {
 [STAThread]static void Main(){ApplicationConfiguration.Initialize();Application.Run(new MainForm());}
}