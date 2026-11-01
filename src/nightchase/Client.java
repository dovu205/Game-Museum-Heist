package nightchase;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

public final class Client extends JFrame {
 static final Color BG=new Color(9,15,25),PANEL=new Color(18,28,44),SOFT=new Color(33,48,66),INK=new Color(234,242,251),MUTED=new Color(145,166,186),BLUE=new Color(86,176,255),MINT=new Color(89,225,184),GOLD=new Color(255,200,109),RED=new Color(255,113,133);
 static final Font BODY=new Font("Segoe UI",Font.PLAIN,14);
 final boolean hosting;final Audio audio=new Audio();
 final JPanel root=new JPanel(new BorderLayout()),content=new JPanel(new BorderLayout());
 JPanel sidePanel,footerPanel;JLabel authStatus;boolean signupScreen;
 final JLabel status=new JLabel("Sẵn sàng."),identity=new JLabel("MUSEUM HEIST");
 ClientService service;Server localServer;
 JTextField host=new JTextField("127.0.0.1"),port=new JTextField("5555"),name=new JTextField();
 JPasswordField password=new JPasswordField(),confirm=new JPasswordField();
 JButton login,register,send,again,exit;JDialog invitation;
 JTable online;DefaultTableModel onlineModel;
 String username="",view="LOGIN",match="",opponent="",role="POLICE",kind="MOVE",direction="STAY",ending="";
 int round,turn,score,otherScore,pos,exitCell=-1,relicMask=-1,carrying=-1,scans=2,decoys=1,lastTick=-1,connectionVersion;
 long deadline,serverOffset,scanTime,animationStart;int animationFrom;int[] animationPath={};
 int enemyPos=-1;boolean locked=true,finished;String scanResult="";
 JLabel roleLabel,roundLabel,turnLabel,countdown,scoreLabel,selectionLabel,mission;
 JTextArea log=new JTextArea();JProgressBar timebar;MapCanvas canvas;
 final java.util.List<JButton> actionButtons=new ArrayList<>();final Map<String,JButton> modeButtons=new LinkedHashMap<>();
 final java.util.List<Museum.Clue> clues=new ArrayList<>();
 final Map<String,DataView> dataViews=new HashMap<>();
 final javax.swing.Timer uiTimer;
 public Client(){this(false);}
 public Client(boolean hosting){
  super("Museum Heist • Truy bắt trong bảo tàng");this.hosting=hosting;
  setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);setSize(640,760);setLocationRelativeTo(null);
  root.setBackground(BG);setContentPane(root);sidePanel=sidebar();root.add(sidePanel,BorderLayout.WEST);content.setOpaque(false);root.add(content);
  footerPanel=new JPanel(new BorderLayout());footerPanel.setBackground(PANEL);footerPanel.setBorder(new EmptyBorder(9,20,9,20));status.setForeground(MUTED);footerPanel.add(status);root.add(footerPanel,BorderLayout.SOUTH);
  addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){
   if(!match.isEmpty()&&!finished&&JOptionPane.showConfirmDialog(Client.this,"Rời trận sẽ tính là đầu hàng. Tiếp tục?","Rời trận",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
   shutdown();dispose();
  }});
  uiTimer=new javax.swing.Timer(33,e->updateClock());uiTimer.start();showLogin();audio.music("menu");
 }
 JPanel sidebar(){
  JPanel side=new JPanel();side.setLayout(new BoxLayout(side,BoxLayout.Y_AXIS));side.setPreferredSize(new Dimension(190,0));side.setBackground(PANEL);side.setBorder(new EmptyBorder(30,18,22,18));
  side.add(label("MH /",34,GOLD));side.add(Box.createVerticalStrut(9));side.add(label("MUSEUM HEIST",17,INK));side.add(label("BẢO TÀNG LÚC NỬA ĐÊM",10,MUTED));side.add(Box.createVerticalStrut(32));
  identity.setForeground(MINT);identity.setFont(BODY.deriveFont(12f));side.add(identity);side.add(Box.createVerticalStrut(20));
  JButton home=button("Sảnh chờ",SOFT,()->{if(!username.isEmpty()&&match.isEmpty()){showLobby();service.send("LIST");}});
  JButton rank=button("Bảng xếp hạng",SOFT,()->request("RANKING"));
  JButton history=button("Lịch sử đấu",SOFT,()->request("HISTORY"));
  JButton help=button("Luật bảo tàng",SOFT,this::rules);
  for(JButton b:new JButton[]{home,rank,history,help}){b.setMaximumSize(new Dimension(160,43));side.add(b);side.add(Box.createVerticalStrut(9));}
  side.add(Box.createVerticalGlue());side.add(label("ÂM THANH",11,MUTED));side.add(Box.createVerticalStrut(8));
  JCheckBox mute=new JCheckBox("Tắt âm thanh");mute.setOpaque(false);mute.setForeground(INK);mute.setFont(BODY.deriveFont(12f));mute.addActionListener(e->audio.mute(mute.isSelected()));side.add(mute);
  JSlider volume=new JSlider(0,100,55);volume.setOpaque(false);volume.setMaximumSize(new Dimension(150,30));volume.addChangeListener(e->audio.volume(volume.getValue()));side.add(volume);
  side.add(Box.createVerticalStrut(20));side.add(label("7 × 7  /  2 VÁN ĐỔI VAI",10,GOLD));return side;
 }
 static JLabel label(String text,int size,Color c){JLabel l=new JLabel(text);l.setFont(BODY.deriveFont(size>=19?Font.BOLD:Font.PLAIN,(float)size));l.setForeground(c);l.setAlignmentX(Component.LEFT_ALIGNMENT);return l;}
 static JButton button(String text,Color c,Runnable action){
  JButton b=new JButton(text);b.setFont(BODY.deriveFont(Font.BOLD,14f));b.setBackground(c);b.setForeground(c.equals(BLUE)||c.equals(MINT)||c.equals(GOLD)?BG:INK);b.setFocusPainted(false);b.setBorder(new EmptyBorder(12,16,12,16));b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));b.addActionListener(e->action.run());return b;
 }
 JPanel box(){JPanel p=new JPanel();p.setBackground(PANEL);p.setBorder(new CompoundBorder(new LineBorder(SOFT,1,true),new EmptyBorder(22,24,22,24)));return p;}
 void clear(String title,String subtitle){
  content.removeAll();JPanel h=new JPanel(new GridLayout(2,1,0,4));h.setOpaque(false);h.setBorder(new EmptyBorder(0,0,22,0));h.add(label(title,29,INK));h.add(label(subtitle,13,MUTED));content.add(h,BorderLayout.NORTH);
 }
 void refresh(){content.revalidate();content.repaint();}
 boolean isAuth(){return view.equals("LOGIN")||view.equals("REGISTER");}
 void showLogin(){showAuth(false);}
 void showAuth(boolean signup){
  closeDataViews();
  signupScreen=signup;view=signup?"REGISTER":"LOGIN";setShell(false);
  password.setText("");confirm.setText("");password.setEchoChar('●');confirm.setEchoChar('●');
  content.removeAll();content.setLayout(new GridBagLayout());
  JPanel card=box();card.setLayout(new BoxLayout(card,BoxLayout.Y_AXIS));card.setBorder(new CompoundBorder(new LineBorder(SOFT,1,true),new EmptyBorder(30,34,26,34)));
  card.setPreferredSize(new Dimension(410,signup?660:600));
  JLabel brand=label("MUSEUM HEIST",25,INK);card.add(brand);card.add(Box.createVerticalStrut(7));card.add(label("TRUY BẮT TRONG BẢO TÀNG",11,BLUE));card.add(Box.createVerticalStrut(27));
  card.add(label(signup?"Tạo tài khoản":"Đăng nhập",22,INK));card.add(Box.createVerticalStrut(7));
  card.add(label(signup?"Tham gia cuộc truy đuổi cùng bạn bè.":"Chào mừng bạn quay lại.",13,MUTED));card.add(Box.createVerticalStrut(23));
  authField(card,"Tên tài khoản",name);authField(card,"Mật khẩu",password);
  if(signup)authField(card,"Xác nhận mật khẩu",confirm);
  JCheckBox showPassword=new JCheckBox("Hiện mật khẩu");showPassword.setOpaque(false);showPassword.setForeground(MUTED);showPassword.setFont(BODY.deriveFont(12f));showPassword.setAlignmentX(Component.LEFT_ALIGNMENT);
  showPassword.addActionListener(e->{password.setEchoChar(showPassword.isSelected()?(char)0:'●');confirm.setEchoChar(showPassword.isSelected()?(char)0:'●');});card.add(showPassword);card.add(Box.createVerticalStrut(15));
  login=button("Đăng nhập",BLUE,()->authenticate(false));register=button("Tạo tài khoản",BLUE,()->authenticate(true));
  JButton primary=signup?register:login;primary.setAlignmentX(Component.LEFT_ALIGNMENT);primary.setMaximumSize(new Dimension(1000,44));card.add(primary);getRootPane().setDefaultButton(primary);
  card.add(Box.createVerticalStrut(10));
  JButton toggle=button(signup?"Đã có tài khoản? Đăng nhập":"Chưa có tài khoản? Đăng ký",PANEL,()->showAuth(!signup));toggle.setBorder(new EmptyBorder(8,0,8,0));toggle.setForeground(BLUE);toggle.setFont(BODY.deriveFont(13f));toggle.setAlignmentX(Component.LEFT_ALIGNMENT);card.add(toggle);
  if(!hosting){
   JButton connection=button("Kết nối phòng chơi…",PANEL,this::connectionDialog);connection.setBorder(new EmptyBorder(5,0,5,0));connection.setFont(BODY.deriveFont(12f));connection.setForeground(MUTED);connection.setAlignmentX(Component.LEFT_ALIGNMENT);card.add(connection);
  }
  card.add(Box.createVerticalStrut(10));authStatus=label(" ",12,MUTED);authStatus.setPreferredSize(new Dimension(340,48));authStatus.setMinimumSize(new Dimension(340,48));authStatus.setMaximumSize(new Dimension(340,48));authStatus.setVerticalAlignment(SwingConstants.TOP);card.add(authStatus);
  content.add(card,new GridBagConstraints());refresh();name.requestFocusInWindow();
 }
 void authField(JPanel card,String title,JTextField input){
  card.add(label(title,12,MUTED));card.add(Box.createVerticalStrut(6));
  input.setFont(BODY);input.setBackground(BG);input.setForeground(INK);input.setCaretColor(BLUE);input.setBorder(new CompoundBorder(new LineBorder(SOFT),new EmptyBorder(9,12,9,12)));input.setAlignmentX(Component.LEFT_ALIGNMENT);input.setMaximumSize(new Dimension(1000,42));input.setPreferredSize(new Dimension(340,42));
  card.add(input);card.add(Box.createVerticalStrut(14));
 }
 void connectionDialog(){
  String oldHost=host.getText(),oldPort=port.getText();JPanel fields=new JPanel(new GridLayout(2,2,12,12));fields.setBackground(PANEL);
  field(fields,"IP Radmin của máy chủ phòng",host);field(fields,"Cổng kết nối",port);
  int result=JOptionPane.showConfirmDialog(this,fields,"Kết nối phòng chơi",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
  if(result==JOptionPane.OK_OPTION)notice("Phòng chơi: "+host.getText().trim()+":"+port.getText().trim());else{host.setText(oldHost);port.setText(oldPort);}
 }
 void setShell(boolean playing){
  sidePanel.setVisible(playing);footerPanel.setVisible(playing);
  content.setLayout(new BorderLayout());content.setBorder(new EmptyBorder(playing?28:20,playing?32:20,playing?12:20,playing?32:20));
  if(playing){getRootPane().setDefaultButton(null);if(getWidth()<1080){Rectangle screen=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();setMinimumSize(new Dimension(Math.min(1080,screen.width),Math.min(740,screen.height)));setSize(Math.min(1280,screen.width),Math.min(880,screen.height));setLocationRelativeTo(null);}}
  else{getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(KeyStroke.getKeyStroke("ENTER"));root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(KeyStroke.getKeyStroke("ENTER"));Rectangle screen=GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();setMinimumSize(new Dimension(Math.min(500,screen.width),Math.min(740,screen.height)));setSize(Math.min(640,screen.width),Math.min(760,screen.height));setLocationRelativeTo(null);}
 }
 void field(JPanel p,String title,JTextField f){
  p.add(label(title,13,MUTED));f.setFont(BODY);f.setBackground(BG);f.setForeground(INK);f.setCaretColor(BLUE);f.setBorder(new CompoundBorder(new LineBorder(SOFT),new EmptyBorder(7,10,7,10)));p.add(f);
 }
 void authenticate(boolean signup){
  String user=name.getText().trim(),pass=new String(password.getPassword());
  if(!user.matches("[A-Za-z0-9_]{3,20}")||pass.length()<6){notice("Tên 3–20 ký tự chữ/số/_, mật khẩu tối thiểu 6 ký tự.");return;}
  if(signup&&!pass.equals(new String(confirm.getPassword()))){notice("Hai mật khẩu không trùng nhau.");return;}
  int n;try{n=Integer.parseInt(port.getText().trim());if(n<1||n>65535)throw new NumberFormatException();}catch(Exception e){notice("Cổng không hợp lệ.");return;}
  login.setEnabled(false);register.setEnabled(false);notice("Đang kết nối…");
  if(service!=null)service.close();
  int version=++connectionVersion;service=new ClientService(m->{if(version==connectionVersion)receive(m);});ClientService current=service;
  current.connect(host.getText().trim(),n,()->current.send(signup?"REGISTER":"LOGIN",user,pass));
 }
 void showLobby(){
  setShell(true);view="LOBBY";match="";finished=false;audio.music("menu");getRootPane().setDefaultButton(null);
  clear("Bảo tàng lúc nửa đêm","Một người lấy báu vật. Một người lần theo dấu vết.");
  JPanel body=box();body.setLayout(new BorderLayout(0,18));body.add(label("NGƯỜI CHƠI TRỰC TUYẾN",13,GOLD),BorderLayout.NORTH);
  onlineModel=model(new String[]{"Người chơi","Điểm","Trạng thái"});online=table(onlineModel);body.add(scroll(online));
  JPanel actions=new JPanel(new FlowLayout(FlowLayout.LEFT,10,0));actions.setOpaque(false);
  actions.add(button("Thách đấu →",BLUE,()->{
   int row=online.getSelectedRow();if(row<0){notice("Chọn một người chơi trong danh sách.");return;}
   String target=String.valueOf(online.getValueAt(row,0));
   if(target.equals(username)||!online.getValueAt(row,2).equals("Rỗi")){notice("Chọn người khác đang rỗi.");return;}
   service.send("INVITE",target);
  }));
  actions.add(button("Làm mới",SOFT,()->service.send("LIST")));
  actions.add(button("Đăng xuất",SOFT,()->service.send("LOGOUT")));body.add(actions,BorderLayout.SOUTH);content.add(body);refresh();
 }
 static DefaultTableModel model(String[] cols){return new DefaultTableModel(cols,0){public boolean isCellEditable(int r,int c){return false;}};}
 static JTable table(DefaultTableModel m){
  JTable t=new JTable(m);t.setFont(BODY);t.setRowHeight(42);t.setBackground(PANEL);t.setForeground(INK);t.setSelectionBackground(new Color(39,78,107));t.setSelectionForeground(INK);t.setGridColor(SOFT);t.setShowVerticalLines(false);t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
  t.getTableHeader().setBackground(SOFT);t.getTableHeader().setForeground(INK);t.getTableHeader().setFont(BODY.deriveFont(Font.BOLD,12f));t.getTableHeader().setPreferredSize(new Dimension(100,38));return t;
 }
 static JScrollPane scroll(JTable t){JScrollPane s=new JScrollPane(t);s.setColumnHeaderView(t.getTableHeader());s.setBorder(new LineBorder(SOFT));s.getViewport().setBackground(PANEL);return s;}
 void fill(DefaultTableModel m,String data){m.setRowCount(0);for(String line:data.split("\n"))if(!line.isBlank())m.addRow(line.split("\\|",-1));}
 void request(String type){if(service!=null&&!username.isEmpty())service.send(type);}
 void showData(String type,String title,String[] columns,String[] message){
  if(username.isEmpty())return;
  DataView d=dataViews.get(type);
  if(d==null||!d.isDisplayable()){d=new DataView(type,title,columns);dataViews.put(type,d);}
  d.update(message);d.setVisible(true);
 }
 void closeDataViews(){for(DataView d:dataViews.values())d.dispose();dataViews.clear();}
 final class DataView extends JDialog {
  final DefaultTableModel rows;final JButton previous,next;final JLabel pageLabel;final String type;int page;
  DataView(String type,String title,String[] columns){
   super(Client.this,title,false);this.type=type;setDefaultCloseOperation(DISPOSE_ON_CLOSE);
   rows=model(columns);add(scroll(table(rows)),BorderLayout.CENTER);
   JPanel navigation=new JPanel(new FlowLayout(FlowLayout.CENTER,14,10));navigation.setBackground(PANEL);
   previous=button("← Trang trước",SOFT,()->load(page-1));next=button("Trang sau →",SOFT,()->load(page+1));pageLabel=label("",13,INK);
   navigation.add(previous);navigation.add(pageLabel);navigation.add(next);add(navigation,BorderLayout.SOUTH);
   setSize(columns.length>7?1100:850,450);setLocationRelativeTo(Client.this);
  }
  void load(int target){if(service!=null&&!username.isEmpty())service.send(type,target);}
  void update(String[] message){
   fill(rows,message[1]);page=Integer.parseInt(message[2]);int pages=Integer.parseInt(message[3]),total=Integer.parseInt(message[4]);
   pageLabel.setText("Trang "+(page+1)+" / "+pages+" · "+total+" bản ghi");previous.setEnabled(page>0);next.setEnabled(page+1<pages);
  }
 }
 void showGame(){
  setShell(true);view="GAME";clear("TRUY BẮT TRONG BẢO TÀNG","Hành động bí mật • Cập nhật đồng thời • Mỗi nhịp 5 giây");
  JPanel body=new JPanel(new BorderLayout(16,12));body.setOpaque(false);
  JPanel stats=box();stats.setBorder(new EmptyBorder(13,18,13,18));stats.setLayout(new GridLayout(1,5,12,0));
  roleLabel=label("",16,BLUE);roundLabel=label("",16,INK);turnLabel=label("",16,INK);scoreLabel=label("",16,MINT);countdown=label("",21,GOLD);
  for(JLabel l:new JLabel[]{roleLabel,roundLabel,turnLabel,scoreLabel,countdown})stats.add(l);body.add(stats,BorderLayout.NORTH);
  JPanel mapArea=new JPanel(new BorderLayout(0,10));mapArea.setOpaque(false);canvas=new MapCanvas();mapArea.add(canvas);
  JLabel legend=label("Vàng: báu vật / tiếng động   ·   Xanh: vùng quét   ·   Dấu chân: ô cũ",12,MUTED);mapArea.add(legend,BorderLayout.SOUTH);body.add(mapArea);
  JPanel tools=box();tools.setBorder(new EmptyBorder(17,17,16,17));tools.setLayout(new BoxLayout(tools,BoxLayout.Y_AXIS));
  mission=label("",13,INK);mission.setPreferredSize(new Dimension(240,50));mission.setMinimumSize(new Dimension(240,50));mission.setMaximumSize(new Dimension(240,50));mission.setVerticalAlignment(SwingConstants.TOP);tools.add(mission);tools.add(Box.createVerticalStrut(12));
  tools.add(label("HÀNH ĐỘNG",11,GOLD));tools.add(Box.createVerticalStrut(8));modeButtons.clear();actionButtons.clear();
  for(String mode:new String[]{"MOVE","DASH","DECOY","SCAN"}){
   String text=switch(mode){case "MOVE"->"Di chuyển";case "DASH"->"Chạy nhanh · 2 ô";case "DECOY"->"Tung mồi nhử";default->"Quét khu vực";};
   JButton b=button(text,SOFT,()->chooseMode(mode));b.setMaximumSize(new Dimension(1000,31));b.setBorder(new EmptyBorder(6,11,6,11));b.setAlignmentX(Component.LEFT_ALIGNMENT);modeButtons.put(mode,b);tools.add(b);tools.add(Box.createVerticalStrut(5));actionButtons.add(b);
  }
  tools.add(Box.createVerticalStrut(8));JPanel pad=new JPanel(new GridLayout(3,3,5,5));pad.setOpaque(false);pad.setMaximumSize(new Dimension(240,96));pad.setPreferredSize(new Dimension(240,96));pad.setAlignmentX(Component.LEFT_ALIGNMENT);
  String[] cells={"","↑","","←","•","→","","↓",""};String[] dirs={"","UP","","LEFT","STAY","RIGHT","","DOWN",""};
  for(int i=0;i<9;i++){if(cells[i].isEmpty()){JPanel gap=new JPanel();gap.setOpaque(false);pad.add(gap);}else{String d=dirs[i];JButton b=button(cells[i],SOFT,()->chooseDirection(d));b.setBorder(new EmptyBorder(3,6,3,6));pad.add(b);actionButtons.add(b);}}
  tools.add(pad);tools.add(Box.createVerticalStrut(10));selectionLabel=label("",12,MINT);selectionLabel.setMaximumSize(new Dimension(240,25));tools.add(selectionLabel);
  tools.add(Box.createVerticalStrut(9));send=button("Khóa hành động →",BLUE,this::submit);send.setMaximumSize(new Dimension(1000,42));send.setAlignmentX(Component.LEFT_ALIGNMENT);tools.add(send);
  tools.add(Box.createVerticalStrut(9));timebar=new JProgressBar(0,5000);timebar.setForeground(BLUE);timebar.setBackground(BG);timebar.setBorderPainted(false);timebar.setMaximumSize(new Dimension(1000,5));timebar.setAlignmentX(Component.LEFT_ALIGNMENT);tools.add(timebar);
  tools.add(Box.createVerticalStrut(15));tools.add(label("NHẬT KÝ CỦA BẠN",11,MUTED));tools.add(Box.createVerticalStrut(6));
  log.setEditable(false);log.setLineWrap(true);log.setWrapStyleWord(true);log.setFont(BODY.deriveFont(12f));log.setBackground(PANEL);log.setForeground(MUTED);log.setBorder(new EmptyBorder(5,0,5,0));
  JScrollPane ls=new JScrollPane(log);ls.setBorder(null);ls.getViewport().setBackground(PANEL);ls.setAlignmentX(Component.LEFT_ALIGNMENT);ls.setPreferredSize(new Dimension(240,105));tools.add(ls);
  JScrollPane toolScroll=new JScrollPane(tools,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);toolScroll.setPreferredSize(new Dimension(278,0));toolScroll.setBorder(null);toolScroll.getViewport().setBackground(PANEL);styleScroll(toolScroll);toolScroll.getVerticalScrollBar().setUnitIncrement(14);body.add(toolScroll,BorderLayout.EAST);
  JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,10,0));actions.setOpaque(false);
  exit=button("Rời trận",SOFT,()->{if(finished||JOptionPane.showConfirmDialog(this,"Rời trận sẽ tính là đầu hàng. Tiếp tục?","Rời trận",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)service.send("EXIT");});
  again=button("Chơi lại",MINT,()->{service.send("AGAIN");again.setEnabled(false);});again.setVisible(false);actions.add(exit);actions.add(again);body.add(actions,BorderLayout.SOUTH);
  content.add(body);bindKeys();audio.music("chase");refresh();
 }
 static void styleScroll(JScrollPane pane){
  JScrollBar bar=pane.getVerticalScrollBar();bar.setPreferredSize(new Dimension(9,0));
  bar.setUI(new javax.swing.plaf.basic.BasicScrollBarUI(){
   protected void configureScrollBarColors(){thumbColor=SOFT;trackColor=PANEL;}
   protected JButton createDecreaseButton(int o){return tiny();}
   protected JButton createIncreaseButton(int o){return tiny();}
   JButton tiny(){JButton b=new JButton();b.setPreferredSize(new Dimension(0,0));b.setMinimumSize(new Dimension(0,0));b.setMaximumSize(new Dimension(0,0));return b;}
   protected void paintThumb(Graphics g,JComponent c,Rectangle r){if(r.isEmpty())return;Graphics2D a=(Graphics2D)g.create();a.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);a.setColor(new Color(68,91,111));a.fillRoundRect(r.x+1,r.y+2,Math.max(3,r.width-2),r.height-4,7,7);a.dispose();}
  });
 }
 void chooseMode(String mode){
  if(locked||finished)return;
  if((mode.equals("SCAN")&&(!role.equals("POLICE")||scans==0))||((mode.equals("DASH")||mode.equals("DECOY"))&&!role.equals("THIEF"))||(mode.equals("DECOY")&&decoys==0))return;
  kind=mode;if(mode.equals("SCAN"))direction="STAY";else if(direction.equals("STAY")&&!mode.equals("MOVE"))direction="UP";
  updateControls();audio.effect("click");
 }
 void chooseDirection(String d){
  if(locked||finished)return;
  if(d.equals("STAY"))kind="MOVE";else if(kind.equals("SCAN"))kind="MOVE";
  direction=d;updateControls();audio.effect("click");
 }
 void submit(){
  if(locked||finished)return;
  Museum preview=new Museum(exitCell>=0?exitCell:42);preview.cop=pos;preview.thief=pos;preview.scans=scans;preview.decoys=decoys;
  String error=preview.validate(role.equals("POLICE"),new Museum.Action(kind,direction));
  if(error!=null){notice(error);appendLog(error);return;}
  locked=true;updateControls();service.send("ACTION",match,round,turn,kind,direction);audio.effect("lock");notice("Đã gửi hành động. Chờ server phân xử.");
 }
 void updateControls(){
  boolean cop=role.equals("POLICE");
  modeButtons.get("DASH").setVisible(!cop);modeButtons.get("DECOY").setVisible(!cop);modeButtons.get("SCAN").setVisible(cop);
  for(var e:modeButtons.entrySet()){
   String k=e.getKey();e.getValue().setBackground(k.equals(kind)?BLUE:SOFT);e.getValue().setForeground(k.equals(kind)?BG:INK);
   e.getValue().setEnabled(!locked&&!finished&&(!k.equals("SCAN")||scans>0)&&(!k.equals("DECOY")||decoys>0));
  }
  modeButtons.get("SCAN").setText("Quét khu vực · còn "+Math.max(0,scans)+"/2");modeButtons.get("DECOY").setText("Tung mồi nhử · còn "+Math.max(0,decoys)+"/1");
  for(JButton b:actionButtons)if(!modeButtons.containsValue(b))b.setEnabled(!locked&&!finished);
  send.setEnabled(!locked&&!finished);send.setText(locked?"Đã khóa · chờ phân xử":"Khóa hành động →");
  String action=switch(kind){case "DASH"->"Chạy";case "DECOY"->"Mồi nhử";case "SCAN"->"Quét 3×3";default->"Đi";};
  String dir=switch(direction){case "UP"->"lên";case "RIGHT"->"phải";case "DOWN"->"xuống";case "LEFT"->"trái";default->"đứng yên";};
  selectionLabel.setText(kind.equals("SCAN")?action:action+" · "+dir);
  String text=cop?"Tìm và bắt trộm trong 20 nhịp.<br>Vị trí trộm và cửa thoát được giữ kín.":carrying==1?"Đã có báu vật!<br>Đến cửa thoát "+Museum.cellName(exitCell)+" để thắng.":"Lấy một báu vật rồi đến cửa thoát.<br>Chạy nhanh sẽ gây tiếng động.";
  mission.setText("<html><div style='width:168px'>"+text+"</div></html>");canvas.repaint();refresh();
 }
 void bindKeys(){
  String[] keys={"UP","RIGHT","DOWN","LEFT","SPACE"};
  for(int i=0;i<5;i++){String d=Museum.DIRECTIONS[i],key=keys[i];root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key),key);root.getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){if(view.equals("GAME"))chooseDirection(d);}});}
  root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"),"SUBMIT");root.getActionMap().put("SUBMIT",new AbstractAction(){public void actionPerformed(ActionEvent e){if(view.equals("GAME"))submit();}});
 }
 void appendLog(String message){if(message==null||message.isBlank())return;String prefix="V"+round+" · "+turn+": ";log.append((log.getText().isBlank()?"":"\n\n")+prefix+message);log.setCaretPosition(log.getDocument().getLength());}
 void readClues(String text){clues.clear();for(String part:text.split(";"))if(!part.isBlank()){String[] c=part.split(",");clues.add(new Museum.Clue(Integer.parseInt(c[0]),c[1],Integer.parseInt(c[2])));}}
 int[] parsePath(String text){return text.isBlank()?new int[0]:Arrays.stream(text.split(",")).mapToInt(Integer::parseInt).toArray();}
 void receive(String[] m){
  try{switch(m[0]){
   case "LOGIN_OK"->{username=m[1];identity.setText("● "+username);password.setText("");confirm.setText("");showLobby();notice("Chào "+username+"! Chọn đối thủ để thách đấu.");}
   case "REGISTERED"->{showLogin();notice("Tạo tài khoản thành công. Bạn có thể đăng nhập.");}
   case "ERROR","NOTICE"->{notice(m[1]);if(isAuth()){login.setEnabled(true);register.setEnabled(true);}}
   case "ACTION_REJECTED"->{locked=false;updateControls();notice(m[1]);appendLog(m[1]);}
   case "LOBBY"->{if(onlineModel!=null)fill(onlineModel,m[1]);}
   case "RANKING"->showData("RANKING","Bảng xếp hạng",new String[]{"Hạng","Người chơi","Điểm","Thắng","Thua","Số trận","Tỷ lệ"},m);
   case "HISTORY"->showData("HISTORY","Lịch sử",new String[]{"Mã trận","Người A","Người B","Điểm","Ván","Thắng","Kết thúc (UTC)","Lý do"},m);
   case "INVITED"->invite(m[1]);
   case "INVITE_CLOSED"->{if(invitation!=null)invitation.dispose();notice("Lời mời đã hủy hoặc hết hạn.");}
   case "STATE"->{
    if(invitation!=null)invitation.dispose();
    boolean fresh=!match.equals(m[1])||round!=Integer.parseInt(m[2]);match=m[1];round=Integer.parseInt(m[2]);turn=Integer.parseInt(m[3]);role=m[4];opponent=m[5];score=Integer.parseInt(m[6]);otherScore=Integer.parseInt(m[7]);pos=Integer.parseInt(m[8]);exitCell=Integer.parseInt(m[9]);relicMask=Integer.parseInt(m[10]);carrying=Integer.parseInt(m[11]);scans=Integer.parseInt(m[12]);decoys=Integer.parseInt(m[13]);readClues(m[14]);deadline=Long.parseLong(m[15]);serverOffset=Long.parseLong(m[16])-System.currentTimeMillis();
    locked=false;finished=false;ending="";enemyPos=-1;kind="MOVE";direction="STAY";scanResult="";lastTick=-1;animationPath=new int[0];
    if(!view.equals("GAME"))showGame();
    if(fresh){log.setText("");appendLog(m[17]);audio.effect("round");}
    roleLabel.setText(role.equals("POLICE")?"● CẢNH SÁT":"● KẺ TRỘM");roleLabel.setForeground(role.equals("POLICE")?BLUE:GOLD);roundLabel.setText("Ván "+round+" / 2");turnLabel.setText("Nhịp "+turn+" / 20");scoreLabel.setText(score+" : "+otherScore);
    again.setVisible(false);updateControls();updateClock();notice("Chọn hành động rồi nhấn Khóa hoặc Enter.");audio.music(turn>=17?"finale":"chase");
   }
   case "LOCKED"->{locked=true;updateControls();notice("Đã khóa hành động. Lựa chọn đối thủ vẫn bí mật.");}
   case "UPDATE"->{
    if(!match.equals(m[1]))return;
    locked=true;deadline=0;animationFrom=pos;animationPath=parsePath(m[4]);animationStart=System.currentTimeMillis();pos=Integer.parseInt(m[5]);readClues(m[6]);relicMask=Integer.parseInt(m[7]);carrying=Integer.parseInt(m[8]);scans=Integer.parseInt(m[9]);decoys=Integer.parseInt(m[10]);appendLog(m[11]);scanResult=m[12];if(!scanResult.isBlank()){scanTime=System.currentTimeMillis();audio.effect("scan");}
    ending=m[13];enemyPos=Integer.parseInt(m[14]);exitCell=Integer.parseInt(m[16]);countdown.setText("PHÂN XỬ");updateControls();
    if(m[11].contains("Đã lấy báu vật"))audio.effect("drop");else if(m[11].contains("tiếng động"))audio.effect("click");
   }
   case "ROUND_END"->{
    locked=true;deadline=0;score=Integer.parseInt(m[3]);otherScore=Integer.parseInt(m[4]);scoreLabel.setText(score+" : "+otherScore);enemyPos=role.equals("POLICE")?Integer.parseInt(m[7]):Integer.parseInt(m[6]);exitCell=Integer.parseInt(m[8]);
    appendLog(m[5]);notice(m[5]);countdown.setText(m[1].equals("WIN")?"THẮNG VÁN":"THUA VÁN");audio.effect(ending.equals("CAPTURE")?"caught":ending.equals("ESCAPE")?"escape":"lose");updateControls();
   }
   case "FINISHED"->{
    locked=true;finished=true;deadline=0;score=Integer.parseInt(m[2]);otherScore=Integer.parseInt(m[3]);scoreLabel.setText(score+" : "+otherScore);countdown.setText(m[1].equals("DRAW")?"HÒA":m[1].equals("WIN")?"THẮNG":"THUA");
    appendLog("Kết thúc trận "+score+":"+otherScore+". Cả hai chọn Chơi lại để đấu trận mới.");again.setVisible(true);again.setEnabled(true);updateControls();audio.music("menu");audio.effect(m[1].equals("WIN")?"victory":"round");
   }
   case "BACK"->{match="";showLobby();notice(m[1]);if(service!=null)service.send("LIST");}
   case "LOGGED_OUT"->{username="";match="";identity.setText("MUSEUM HEIST");showLogin();notice("Đã đăng xuất.");}
   case "DISCONNECTED"->{match="";username="";identity.setText("MUSEUM HEIST");showLogin();notice(m[1]);audio.music("menu");}
  }}catch(RuntimeException e){notice("Không đọc được thông điệp: "+e.getClass().getSimpleName());}
 }
 void invite(String from){
  if(invitation!=null)invitation.dispose();audio.effect("invite");
  JDialog d=new JDialog(this,"Lời thách đấu",false);invitation=d;d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
  JPanel p=box();p.setLayout(new BorderLayout(10,18));p.add(label(from+" mời bạn truy bắt trong bảo tàng",17,INK),BorderLayout.NORTH);
  JPanel buttons=new JPanel(new GridLayout(1,2,10,0));buttons.setOpaque(false);
  buttons.add(button("Chấp nhận",MINT,()->{service.send("ACCEPT",from);d.dispose();}));buttons.add(button("Từ chối",SOFT,()->{service.send("REJECT",from);d.dispose();}));p.add(buttons);d.setContentPane(p);d.pack();d.setLocationRelativeTo(this);d.setVisible(true);
 }
 void updateClock(){
  if(canvas!=null&&view.equals("GAME"))canvas.repaint();
  if(!view.equals("GAME")||deadline==0||finished)return;
  long left=Math.max(0,deadline-System.currentTimeMillis()-serverOffset);int sec=(int)Math.ceil(left/1000.0);
  countdown.setText(locked?"ĐÃ KHÓA":String.format("%02d giây",sec));timebar.setValue((int)left);timebar.setForeground(sec<=2?RED:BLUE);
  if(!locked&&sec<=2&&sec>0&&sec!=lastTick){audio.effect("tick");lastTick=sec;}
 }
 void notice(String s){
  status.setText(s);status.setToolTipText(s);
  if(isAuth()&&authStatus!=null){String safe=s.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");authStatus.setText("<html><div style='width:245px'>"+safe+"</div></html>");authStatus.setToolTipText(s);}
 }
 void rules(){
  JTextArea t=new JTextArea("TRUY BẮT TRONG BẢO TÀNG\n\nHai ván đổi vai, mỗi ván tối đa 20 nhịp. Mỗi nhịp 5 giây.\nChọn hành động rồi nhấn Khóa / Enter; hết giờ tự đứng yên.\n\nTRỘM: lấy một trong hai báu vật, sau đó đến cửa thoát màu xanh.\nCửa thoát được chọn bí mật mỗi ván, chỉ trộm biết vị trí.\nChạy nhanh: tối đa 2 ô, dừng trước tường, gây tiếng động ở ô cũ.\nMồi nhử: tạo tiếng động ở ô kề, không di chuyển; 1 lần mỗi ván.\n\nCẢNH SÁT: bắt trộm hoặc giữ được bảo tàng hết 20 nhịp.\nQuét: ở yên, kiểm tra vùng 3×3 kể cả góc; 2 lần mỗi ván.\nChỉ nhận có/không có trộm, không nhận tọa độ trộm.\nTiếng động có thể do chạy nhanh hoặc mồi nhử.\nMỗi nhịp thứ 3, trộm đi bộ để lại dấu chân ở ô xuất phát.\n\nĐỨNG YÊN: nút giữa hoặc Space. Phím mũi tên chọn hướng.\nNhấp ô kề trên bản đồ cũng chọn hướng; chưa tự gửi hành động.\n\nBẮT: chung ô hoặc đổi chỗ qua nhau. Chạy nhanh cũng bị bắt\nnếu đi qua ô cảnh sát trong một bước con. Bắt ưu tiên hơn thoát.\nMỗi ván thắng +1 điểm. Hai ván bằng điểm thì hòa.\nVị trí đối thủ chỉ hiện khi ván đã kết thúc.");
  t.setFont(BODY);t.setBackground(PANEL);t.setForeground(INK);t.setEditable(false);t.setBorder(new EmptyBorder(18,18,18,18));JOptionPane.showMessageDialog(this,t,"Luật chơi",JOptionPane.PLAIN_MESSAGE);
 }
 void shutdown(){uiTimer.stop();closeDataViews();if(service!=null)service.close();if(localServer!=null)localServer.close();audio.close();}
 final class MapCanvas extends JPanel{
  int tile,ox,oy,hover=-1;
  MapCanvas(){
   setOpaque(false);setPreferredSize(new Dimension(650,630));setToolTipText("");
   MouseAdapter mouse=new MouseAdapter(){
    public void mouseMoved(MouseEvent e){hover=cellAt(e.getX(),e.getY());repaint();}
    public void mouseExited(MouseEvent e){hover=-1;repaint();}
    public void mousePressed(MouseEvent e){
     int cell=cellAt(e.getX(),e.getY());if(cell<0||locked)return;
     if(cell==pos){chooseDirection("STAY");return;}
     for(String d:Museum.DIRECTIONS)if(!d.equals("STAY")&&Museum.adjacent(pos,d)==cell){chooseDirection(d);break;}
    }
   };addMouseListener(mouse);addMouseMotionListener(mouse);
  }
  int cellAt(int px,int py){if(tile<=0||px<ox||py<oy||px>=ox+tile*7||py>=oy+tile*7)return -1;return (py-oy)/tile*7+(px-ox)/tile;}
  public String getToolTipText(MouseEvent e){int cell=cellAt(e.getX(),e.getY());return cell<0?null:Museum.cellName(cell)+(Museum.walkable(cell)?"":" · Tường");}
  void geometry(){tile=Math.max(15,Math.min((getWidth()-42)/7,(getHeight()-64)/7));ox=(getWidth()-tile*7)/2;oy=(getHeight()-tile*7)/2+8;}
  double cx(int c){return ox+(Museum.x(c)+.5)*tile;}
  double cy(int c){return oy+(Museum.y(c)+.5)*tile;}
  protected void paintComponent(Graphics raw){
   super.paintComponent(raw);Graphics2D g=(Graphics2D)raw.create();g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);geometry();
   int w=getWidth(),h=getHeight();g.setPaint(new GradientPaint(0,0,new Color(15,27,41),w,h,new Color(11,20,31)));g.fillRoundRect(0,0,w,h,18,18);
   g.setFont(BODY.deriveFont(Font.BOLD,10f));g.setColor(MUTED);g.drawString("MUSÉE / PLAN 07",16,20);
   String privacy=enemyPos<0?"VỊ TRÍ ĐỐI THỦ: BÍ MẬT":"VÁN ĐÃ KẾT THÚC";g.setColor(enemyPos<0?MINT:GOLD);g.drawString(privacy,Math.max(160,w-g.getFontMetrics().stringWidth(privacy)-17),20);
   for(int y=0;y<7;y++)for(int x=0;x<7;x++){
    int c=y*7+x,px=ox+x*tile,py=oy+y*tile;
    if(!Museum.walkable(c)){
     g.setColor(new Color(39,54,70));g.fillRoundRect(px+1,py+1,tile-2,tile-2,5,5);
     g.setColor(new Color(62,78,93));g.drawLine(px+5,py+4,px+tile-6,py+4);g.drawLine(px+4,py+5,px+4,py+tile-6);
     g.setColor(new Color(19,31,44));g.fillRect(px+8,py+tile-7,tile-13,4);
     g.setColor(new Color(49,65,80));g.drawLine(px+7,py+tile/2,px+tile-8,py+tile/2);
    }else{
     g.setColor((x+y)%2==0?new Color(25,40,52):new Color(29,44,56));g.fillRect(px+1,py+1,tile-2,tile-2);
     g.setColor(new Color(49,65,77,100));g.drawRect(px+6,py+6,tile-13,tile-13);
     if(c%9==0){g.setColor(new Color(91,110,112,30));g.fillOval(px+tile/4,py+tile/4,tile/2,tile/2);}
    }
   }
   g.setFont(BODY.deriveFont(11f));g.setColor(MUTED);
   for(int i=0;i<7;i++){center(g,String.valueOf((char)('A'+i)),ox+i*tile+tile/2,oy-7);center(g,String.valueOf(i+1),ox-12,oy+i*tile+tile/2+4);}
   for(int i=0;i<Museum.RELICS.length;i++){
    int c=Museum.RELICS[i];boolean visible=role.equals("THIEF")&&(relicMask&(1<<i))!=0;
    int x=(int)cx(c),y=(int)cy(c);
    g.setColor(new Color(9,19,28));g.fillRoundRect(x-17,y-13,34,32,6,6);g.setColor(new Color(78,112,130));g.drawRoundRect(x-17,y-15,34,30,6,6);
    if(visible){
     g.setColor(new Color(255,202,103,23));g.fillOval(x-24,y-24,48,48);g.setColor(GOLD);g.fillPolygon(new int[]{x,x+11,x,x-11},new int[]{y-13,y,y+12,y},4);g.setColor(new Color(255,236,185));g.drawLine(x,y-10,x-6,y);
    }else{g.setColor(new Color(75,107,125));g.drawOval(x-6,y-6,12,12);}
   }
   if(exitCell>=0){
    int x=(int)cx(exitCell),y=(int)cy(exitCell);g.setColor(new Color(89,225,184,35));g.fillRoundRect(x-tile/2+3,y-tile/2+3,tile-6,tile-6,7,7);
    g.setColor(MINT);g.setStroke(new BasicStroke(2));g.drawRoundRect(x-12,y-17,24,32,3,3);g.drawLine(x+6,y-3,x+10,y-3);
    g.setFont(BODY.deriveFont(Font.BOLD,8f));center(g,"THOÁT",x,y+tile/2-6);
   }
   if(role.equals("POLICE")){
    long elapsed=System.currentTimeMillis()-scanTime;
    if(kind.equals("SCAN")&&!locked||elapsed<2200&&!scanResult.isEmpty()){
     Color tint=elapsed<2200&&!scanResult.isEmpty()?(scanResult.equals("YES")?GOLD:MINT):BLUE;
     for(int yy=Math.max(0,Museum.y(pos)-1);yy<=Math.min(6,Museum.y(pos)+1);yy++)for(int xx=Math.max(0,Museum.x(pos)-1);xx<=Math.min(6,Museum.x(pos)+1);xx++){
      int px=ox+xx*tile,py=oy+yy*tile;g.setColor(new Color(tint.getRed(),tint.getGreen(),tint.getBlue(),40));g.fillRect(px+1,py+1,tile-2,tile-2);g.setColor(tint);g.setStroke(new BasicStroke(1));g.drawRect(px+3,py+3,tile-6,tile-6);
     }
     if(elapsed<2200&&!scanResult.isEmpty()){g.setColor(tint);g.setStroke(new BasicStroke(2));double radius=(elapsed%1100)/1100.0*tile*1.7;g.draw(new Ellipse2D.Double(cx(pos)-radius,cy(pos)-radius,radius*2,radius*2));}
    }
    for(Museum.Clue c:clues){
     int x=(int)cx(c.cell()),y=(int)cy(c.cell());g.setColor(c.kind().equals("NOISE")?GOLD:MUTED);
     if(c.kind().equals("NOISE")){
      double pulse=8+4*Math.sin(System.currentTimeMillis()/180.0);g.setStroke(new BasicStroke(2));g.draw(new Ellipse2D.Double(x-pulse,y-pulse,pulse*2,pulse*2));g.drawOval(x-18,y-18,36,36);g.fillOval(x-3,y-3,6,6);
     }else{g.fillOval(x-9,y-3,5,9);g.fillOval(x+3,y+3,5,9);}
     g.setFont(BODY.deriveFont(9f));center(g,String.valueOf(c.until()),x,y+tile/2-6);
    }
   }
   if(!locked&&!finished&&(kind.equals("MOVE")||kind.equals("DASH"))){
    java.util.List<Integer> path=Museum.path(pos,direction,kind.equals("DASH")?2:1);
    if(!path.isEmpty()){
     Path2D line=new Path2D.Double();line.moveTo(cx(pos),cy(pos));for(int c:path)line.lineTo(cx(c),cy(c));
     g.setColor(new Color(86,176,255,185));g.setStroke(new BasicStroke(3,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND,1,new float[]{4,7},0));g.draw(line);
     int last=path.get(path.size()-1);g.setStroke(new BasicStroke(2));g.drawOval((int)cx(last)-13,(int)cy(last)-13,26,26);
    }
   }
   if(!locked&&kind.equals("DECOY")){
    int target=Museum.adjacent(pos,direction);if(target!=pos){g.setColor(GOLD);g.setStroke(new BasicStroke(2));g.drawOval((int)cx(target)-16,(int)cy(target)-16,32,32);}
   }
   if(hover>=0&&!locked&&Museum.walkable(hover)){g.setColor(new Color(86,176,255,25));g.fillRect(ox+Museum.x(hover)*tile+1,oy+Museum.y(hover)*tile+1,tile-2,tile-2);}
   Point2D animated=animatedPosition();
   if(enemyPos>=0)actor(g,cx(enemyPos)+(enemyPos==pos?tile*.18:0),cy(enemyPos),!role.equals("POLICE"),false);
   actor(g,animated.getX()-(enemyPos==pos?tile*.18:0),animated.getY(),role.equals("POLICE"),carrying==1);
   g.setFont(BODY.deriveFont(Font.BOLD,9f));g.setColor(INK);center(g,"BẠN", (int)animated.getX(),(int)animated.getY()-24);
   if(!ending.equals("NONE")&&!ending.isEmpty()){
    String banner=switch(ending){case "CAPTURE"->"ĐÃ BẮT ĐƯỢC TRỘM";case "ESCAPE"->"BÁU VẬT ĐÃ RỜI BẢO TÀNG";default->"BẢO VỆ THÀNH CÔNG";};
    int width=Math.min(w-28,390);g.setColor(new Color(6,13,22,230));g.fillRoundRect((w-width)/2,h/2-26,width,52,14,14);g.setColor(ending.equals("ESCAPE")?GOLD:BLUE);g.setFont(BODY.deriveFont(Font.BOLD,18f));center(g,banner,w/2,h/2+6);
   }
   long remaining=deadline-System.currentTimeMillis()-serverOffset;
   if(!locked&&remaining>0&&remaining<=2000){int alpha=(int)(80+50*Math.sin(System.currentTimeMillis()/100.0));g.setColor(new Color(255,113,133,alpha));g.setStroke(new BasicStroke(3));g.drawRoundRect(2,2,w-5,h-5,16,16);}
   g.dispose();
  }
  Point2D animatedPosition(){
   if(animationPath.length==0||System.currentTimeMillis()-animationStart>650)return new Point2D.Double(cx(pos),cy(pos));
   double progress=Math.min(.999,(System.currentTimeMillis()-animationStart)/650.0)*animationPath.length;int step=(int)progress;double f=progress-step;f=f*f*(3-2*f);
   int from=step==0?animationFrom:animationPath[step-1],to=animationPath[step];return new Point2D.Double(cx(from)+(cx(to)-cx(from))*f,cy(from)+(cy(to)-cy(from))*f);
  }
  void actor(Graphics2D g,double px,double py,boolean cop,boolean hasRelic){
   double scale=Math.min(1.2,tile/57.0);Graphics2D a=(Graphics2D)g.create();a.translate(px,py);a.scale(scale,scale);
   a.setColor(new Color(0,6,12,140));a.fillOval(-15,6,30,14);a.setColor(new Color(15,22,34));a.fillRoundRect(-9,3,7,13,4,4);a.fillRoundRect(2,3,7,13,4,4);
   a.setColor(cop?BLUE:new Color(218,225,231));a.fillRoundRect(-12,-9,24,19,8,8);
   if(!cop){a.setColor(new Color(33,42,54));for(int i=0;i<3;i++)a.fillRect(-11,-6+i*5,22,2);}
   a.setColor(new Color(232,186,150));a.fillOval(-9,-23,18,18);
   if(cop){a.setColor(new Color(28,77,137));a.fillRoundRect(-11,-26,22,9,4,4);a.fillRect(-12,-19,24,3);a.setColor(GOLD);a.fillOval(-3,-24,6,5);a.fillOval(3,-5,5,5);}
   else{a.setColor(new Color(22,28,38));a.fillRoundRect(-9,-25,18,7,4,4);a.fillRect(-9,-17,18,5);a.setColor(INK);a.fillRect(-5,-16,3,1);a.fillRect(3,-16,3,1);if(hasRelic){a.setColor(GOLD);a.fillOval(9,-1,14,17);a.setColor(BG);a.setFont(BODY.deriveFont(Font.BOLD,10f));a.drawString("◆",11,11);}}
   a.dispose();
  }
  void center(Graphics2D g,String text,int x,int y){g.drawString(text,x-g.getFontMetrics().stringWidth(text)/2,y);}
 }
 void snapshot(Path path)throws IOException{
  root.setSize(getWidth(),getHeight());invalidateDeep(root);layoutDeep(root);BufferedImage image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();root.printAll(g);g.dispose();ImageIO.write(image,"png",path.toFile());
 }
 void invalidateDeep(Container c){for(Component x:c.getComponents())if(x instanceof Container cc)invalidateDeep(cc);c.invalidate();}
 void layoutDeep(Container c){c.doLayout();for(Component x:c.getComponents())if(x instanceof Container cc)layoutDeep(cc);}
 public static void main(String[] args){launch(args,null);}
 static String[] connectionDefaults(){
  Properties defaults=new Properties();
  try(InputStream in=Client.class.getResourceAsStream("/client.properties")){
   if(in!=null)defaults.load(new InputStreamReader(in,java.nio.charset.StandardCharsets.UTF_8));
  }catch(IOException e){System.err.println("Không đọc được cấu hình client: "+e.getMessage());}
  return new String[]{System.getProperty("nightchase.host",defaults.getProperty("server.host","127.0.0.1")),System.getProperty("nightchase.port",defaults.getProperty("server.port","5555"))};
 }
 public static void launch(String[] args,Server embedded){
  try{UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());}catch(Exception ignored){}
  UIManager.put("Label.font",BODY);UIManager.put("Button.font",BODY);UIManager.put("OptionPane.background",PANEL);UIManager.put("Panel.background",PANEL);UIManager.put("OptionPane.messageForeground",INK);
  SwingUtilities.invokeLater(()->{
   Client c=new Client(embedded!=null);c.localServer=embedded;
   if(embedded!=null)c.port.setText(String.valueOf(embedded.port()));
   else{String[] endpoint=connectionDefaults();c.host.setText(endpoint[0]);c.port.setText(endpoint[1]);}
   if(args.length==2&&args[0].equals("--preview")){
    try{
     Path dir=Path.of(args[1]);Files.createDirectories(dir);c.snapshot(dir.resolve("login.png"));c.showAuth(true);c.snapshot(dir.resolve("signup.png"));c.showLogin();c.notice("Tên tài khoản cần 3–20 ký tự, mật khẩu tối thiểu 6 ký tự.");c.snapshot(dir.resolve("login-error.png"));
     c.username="Tuan886";c.identity.setText("● Tuan886");c.showLobby();c.fill(c.onlineModel,"Tuan886|12|Rỗi\nNightFox|8|Rỗi");c.snapshot(dir.resolve("lobby.png"));
     long now=System.currentTimeMillis();
     c.receive(new String[]{"STATE","preview","1","5","THIEF","NightFox","0","0","30","42","1","0","-1","1","",String.valueOf(now+5000),String.valueOf(now),"Tìm báu vật."});c.chooseMode("DASH");c.chooseDirection("LEFT");c.snapshot(dir.resolve("thief.png"));c.setSize(1080,740);c.snapshot(dir.resolve("compact.png"));c.setSize(1280,880);
     c.receive(new String[]{"STATE","preview","2","8","POLICE","NightFox","1","0","17","-1","-1","-1","1","-1","29,NOISE,2;24,TRACE,1;",String.valueOf(now+5000),String.valueOf(now),"Tiếng động ở B5 có thể là mồi nhử."});c.chooseMode("SCAN");c.snapshot(dir.resolve("police.png"));
     c.scanResult="YES";c.scanTime=System.currentTimeMillis();c.appendLog("QUÉT: có trộm trong vùng 3×3.");c.snapshot(dir.resolve("scan.png"));
     c.ending="CAPTURE";c.enemyPos=c.pos;c.locked=true;c.snapshot(dir.resolve("capture.png"));c.shutdown();c.dispose();
    }catch(Exception e){e.printStackTrace();c.shutdown();c.dispose();}
   }else c.setVisible(true);
  });
 }
}
