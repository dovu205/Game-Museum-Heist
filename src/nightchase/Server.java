package nightchase;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/** All gameplay mutations are serialized under this server's monitor. */
public final class Server implements AutoCloseable {
 final Store store; final ServerSocket listener;
 final Map<String,Peer> online=new HashMap<>();
 final Set<Peer> peers=new HashSet<>();
 final List<Room> rooms=new ArrayList<>();
 final Map<String,Invite> invites=new HashMap<>();
 final Map<String,Store.Match> pendingResults=new LinkedHashMap<>();long nextSaveRetry;
 final ScheduledExecutorService timer=Executors.newSingleThreadScheduledExecutor();
 final Random random=new java.security.SecureRandom();
 volatile boolean running=true;
 record Invite(String from,long until){}
 public Server(int port,Path dir)throws IOException{
  store=new Store(dir);
  listener=new ServerSocket();listener.setReuseAddress(true);listener.bind(new InetSocketAddress("0.0.0.0",port));
  timer.scheduleAtFixedRate(()->{synchronized(this){try{tick();}catch(Exception e){e.printStackTrace();}}},50,50,TimeUnit.MILLISECONDS);
 }
 public int port(){return listener.getLocalPort();}
 public void run()throws IOException{
  System.out.println("MUSEUM HEIST server • TCP "+port()+" • data saved locally");
  while(running)try{
   Socket s=listener.accept();s.setTcpNoDelay(true);s.setKeepAlive(true);s.setSoTimeout(45000);
   synchronized(this){
    if(peers.size()>=128){s.close();continue;}
    Peer p=new Peer(s);peers.add(p);p.start();
   }
  }catch(SocketException e){if(running)throw e;}
 }
 final class Peer{
  final Socket socket; final BlockingQueue<String> out=new ArrayBlockingQueue<>(256);
  Store.User user;Room room;volatile boolean closed;int failures;long retryAt;
  Peer(Socket s){socket=s;}
  void start(){
   Thread writer=new Thread(()->{
    try(BufferedWriter w=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8))){
     while(!closed){String line=out.take();w.write(line);w.newLine();w.flush();}
    }catch(Exception e){disconnect(this);}
   },"client-writer");writer.setDaemon(true);writer.start();
   Thread reader=new Thread(()->{
    try(BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8))){
     String line;while(!closed&&(line=Wire.read(r))!=null){
      try{String[] m=Wire.decode(line);synchronized(Server.this){handle(this,m);}}
      catch(IllegalArgumentException|IndexOutOfBoundsException e){send("ERROR","Yêu cầu không hợp lệ.");}
     }
    }catch(IOException e){}finally{disconnect(this);writer.interrupt();}
   },"client-reader");reader.setDaemon(true);reader.start();
  }
  void send(String type,Object...args){if(!closed&&!out.offer(Wire.encode(type,args)))disconnect(this);}
 }
 void need(String[] m,int count){if(m.length!=count)throw new IllegalArgumentException();}
 void handle(Peer p,String[] m){
  try{dispatch(p,m);}catch(IOException e){p.send("ERROR","Không truy cập được dữ liệu server. Hãy thử lại.");e.printStackTrace();}
 }
 int requestedPage(String[] m){if(m.length==1)return 0;need(m,2);int page=Integer.parseInt(m[1]);if(page<0)throw new IllegalArgumentException();return page;}
 // Legacy values may contain long text or table delimiters. Bound the display only.
 String display(String value,int maxBytes){
  String clean=value.replace('|','/').replace('\n',' ').replace('\r',' ');StringBuilder b=new StringBuilder();int bytes=0;
  for(int i=0;i<clean.length();){int cp=clean.codePointAt(i);String ch=new String(Character.toChars(cp));int size=ch.getBytes(StandardCharsets.UTF_8).length;if(bytes+size>maxBytes){b.append('…');break;}b.append(ch);bytes+=size;i+=Character.charCount(cp);}
  return b.toString();
 }
 void dispatch(Peer p,String[] m)throws IOException{
  String op=m[0];
  if(op.equals("PING")){p.send("PONG",System.currentTimeMillis());return;}
  if(op.equals("LOGIN")||op.equals("REGISTER")){
   need(m,3);if(p.user!=null){p.send("ERROR","Bạn đã đăng nhập.");return;}
   if(System.currentTimeMillis()<p.retryAt){p.send("ERROR","Thử lại sau vài giây.");return;}
   if(!m[1].matches("[A-Za-z0-9_]{3,20}")||m[2].length()<6||m[2].length()>128){p.send("ERROR","Tên 3–20 ký tự chữ/số/_, mật khẩu 6–128 ký tự.");return;}
   try{
    if(op.equals("REGISTER")){boolean ok=store.register(m[1],m[2]);p.send(ok?"REGISTERED":"ERROR",ok?"Đăng ký thành công, hãy đăng nhập.":"Tên tài khoản đã tồn tại.");return;}
    Store.User u=store.login(m[1],m[2]);
    if(u==null){if(++p.failures>=5){p.retryAt=System.currentTimeMillis()+10000;p.failures=0;}p.send("ERROR","Sai tài khoản hoặc mật khẩu.");return;}
    if(online.containsKey(u.name)){p.send("ERROR","Tài khoản đang online trên máy khác.");return;}
    p.user=u;p.failures=0;online.put(u.name,p);p.send("LOGIN_OK",u.name);lobbies();
   }catch(IOException e){p.send("ERROR","Không lưu được dữ liệu server.");e.printStackTrace();}
   return;
  }
  if(p.user==null){p.send("ERROR","Hãy đăng nhập trước.");return;}
  switch(op){
   case "LIST" -> lobby(p);
   case "RANKING" -> {
    Store.Page<Store.User> page=store.rankingPage(requestedPage(m));
    StringBuilder b=new StringBuilder();int rank=page.page()*Store.PAGE_SIZE;
    for(Store.User u:page.items())b.append(++rank).append('|').append(display(u.name,80)).append('|').append(u.points).append('|').append(u.wins).append('|').append(u.losses).append('|').append(u.played).append('|').append(u.played==0?"0%":String.format(Locale.ROOT,"%.1f%%",100.0*u.wins/u.played)).append('\n');
    p.send("RANKING",b,page.page(),page.pages(),page.total());
   }
   case "HISTORY" -> {
    Store.Page<Store.Match> page=store.historyPage(p.user.name,requestedPage(m));
    StringBuilder b=new StringBuilder();
    for(Store.Match x:page.items())b.append(display(x.id(),80)).append('|').append(display(x.first(),80)).append('|').append(display(x.second(),80)).append('|').append(x.scoreFirst()+":"+x.scoreSecond()).append('|').append(x.rounds()).append('|').append(x.winner().isEmpty()?"Hòa":display(x.winner(),80)).append('|').append(java.time.Instant.ofEpochMilli(x.end())).append('|').append(display(x.reason(),320)).append('\n');
    p.send("HISTORY",b,page.page(),page.pages(),page.total());
   }
   case "INVITE" -> {
    need(m,2);Peer target=online.get(m[1]);
    if(target==null||target==p||target.room!=null||p.room!=null){p.send("ERROR","Đối thủ không sẵn sàng.");return;}
    if(invites.containsKey(m[1])||invites.containsKey(p.user.name)||invites.values().stream().anyMatch(i->i.from.equals(p.user.name)||i.from.equals(m[1]))){p.send("ERROR","Một trong hai người đang có lời mời chờ xử lý.");return;}
    invites.put(m[1],new Invite(p.user.name,System.currentTimeMillis()+20000));target.send("INVITED",p.user.name);p.send("NOTICE","Đã gửi lời mời (hết hạn sau 20 giây).");
   }
   case "ACCEPT","REJECT" -> {
    need(m,2);Invite i=invites.get(p.user.name);
    if(i==null||!i.from.equals(m[1])||i.until<System.currentTimeMillis()){p.send("ERROR","Lời mời đã hết hạn.");return;}
    invites.remove(p.user.name);Peer from=online.get(i.from);
    if(from==null)return;
    if(op.equals("REJECT")){from.send("NOTICE",p.user.name+" đã từ chối.");return;}
    if(from.room!=null||p.room!=null){p.send("ERROR","Người chơi đang bận.");return;}
    start(from,p);
   }
   case "ACTION" -> {
    need(m,6);Room r=p.room;if(r==null)return;
    if(!r.id.equals(m[1])||r.round!=Integer.parseInt(m[2])||r.board.turn!=Integer.parseInt(m[3])||!r.phase.equals("TURN")){p.send("ERROR","Nhịp đã thay đổi. Hãy chọn ở nhịp hiện tại.");return;}
    int k=r.index(p);long now=System.currentTimeMillis();
    if(now>=r.deadline||r.actions[k]!=null){p.send("ERROR","Nhịp đã hết hạn hoặc bạn đã khóa hành động.");return;}
    Museum.Action action=new Museum.Action(m[4],m[5]);String error=r.board.validate(k==r.police,action);
    if(error!=null){p.send("ACTION_REJECTED",error);return;}
    r.actions[k]=action;p.send("LOCKED");
    if(r.actions[0]!=null&&r.actions[1]!=null)r.resolve();
   }
   case "AGAIN" -> {
    Room r=p.room;if(r!=null&&r.phase.equals("FINISHED")){r.again[r.index(p)]=true;p.send("NOTICE","Đang chờ đối thủ đồng ý chơi lại.");if(r.again[0]&&r.again[1]){Peer a=r.players[0],b=r.players[1];rooms.remove(r);a.room=null;b.room=null;start(a,b);}}
   }
   case "EXIT" -> {if(p.room!=null)leave(p);}
   case "LOGOUT" -> {if(p.room!=null)leave(p);removeInvites(p.user.name);online.remove(p.user.name);p.user=null;p.send("LOGGED_OUT");lobbies();}
   default -> p.send("ERROR","Lệnh không được hỗ trợ.");
  }
 }
 void start(Peer a,Peer b){Room r=new Room(a,b);a.room=r;b.room=r;rooms.add(r);lobbies();r.prepare();}
 void lobby(Peer p){
  StringBuilder b=new StringBuilder();online.values().stream().sorted(Comparator.comparing(x->x.user.name)).forEach(x->b.append(x.user.name).append('|').append(x.user.points).append('|').append(x.room==null?"Rỗi":"Đang chơi").append('\n'));
  p.send("LOBBY",b);
 }
 void lobbies(){for(Peer p:online.values())lobby(p);}
 void removeInvites(String name){
  Iterator<Map.Entry<String,Invite>> it=invites.entrySet().iterator();
  while(it.hasNext()){var e=it.next();if(e.getKey().equals(name)||e.getValue().from.equals(name)){
   Peer a=online.get(e.getKey()),b=online.get(e.getValue().from);
   if(a!=null)a.send("INVITE_CLOSED");if(b!=null)b.send("NOTICE","Lời mời đã hủy.");it.remove();
  }}
 }
 void tick(){
  long now=System.currentTimeMillis();
  if(!pendingResults.isEmpty()&&now>=nextSaveRetry){nextSaveRetry=now+5000;retryResults();}
  Iterator<Map.Entry<String,Invite>> it=invites.entrySet().iterator();
  while(it.hasNext()){var e=it.next();if(e.getValue().until<=now){
   Peer a=online.get(e.getKey()),b=online.get(e.getValue().from);
   if(a!=null)a.send("INVITE_CLOSED");if(b!=null)b.send("NOTICE","Lời mời hết hạn.");it.remove();
  }}
  for(Room r:new ArrayList<>(rooms))r.tick(now);
 }
 void retryResults(){
  for(Store.Match result:new ArrayList<>(pendingResults.values())){
   try{
    store.finish(result);pendingResults.remove(result.id());
    for(String name:new String[]{result.first(),result.second()}){Peer p=online.get(name);if(p!=null)p.send("NOTICE","Đã lưu lại thành công kết quả trận đang chờ.");}
    lobbies();
   }catch(IOException e){System.err.println("SQLite result still pending: "+result.id()+": "+e.getMessage());}
  }
 }
 synchronized void disconnect(Peer p){
  if(p.closed)return;p.closed=true;try{p.socket.close();}catch(IOException ignored){}
  peers.remove(p);if(p.user!=null){if(p.room!=null)leave(p);removeInvites(p.user.name);online.remove(p.user.name);lobbies();}
 }
 void leave(Peer p){
  Room r=p.room;if(r==null)return;
  if(!r.phase.equals("FINISHED"))r.finish(1-r.index(p),"Đầu hàng / mất kết nối");
  for(Peer q:r.players){q.room=null;q.send("BACK","Đã trở về sảnh. "+p.user.name+" rời phòng.");}
  rooms.remove(r);lobbies();
 }
 final class Room{
  final String id=UUID.randomUUID().toString();final long start=System.currentTimeMillis();final Peer[] players;
  final int firstPolice=random.nextInt(2);int round=1,police;final int[] score={0,0};String phase="";long deadline,phaseEnd;
  Museum board;Museum.Action[] actions=new Museum.Action[2];boolean[] again=new boolean[2];
  Room(Peer a,Peer b){players=new Peer[]{a,b};}
  int index(Peer p){return players[0]==p?0:1;}
  void both(String type,Object...args){for(Peer p:players)p.send(type,args);}
  void prepare(){
   police=(firstPolice+round-1)%2;board=new Museum(Museum.EXITS[random.nextInt(Museum.EXITS.length)]);
   beginTurn("Ván "+round+": "+(round==1?"Giữ bí mật hành động của bạn.":"Hai người đã đổi vai."));
  }
  void beginTurn(String note){
   phase="TURN";actions=new Museum.Action[2];long now=System.currentTimeMillis();deadline=now+Museum.TURN_MS;
   for(int k=0;k<2;k++){
    boolean cop=k==police;
    players[k].send("STATE",id,round,board.turn,cop?"POLICE":"THIEF",players[1-k].user.name,score[k],score[1-k],cop?board.cop:board.thief,
     cop?-1:board.exit,cop?-1:board.relicMask,cop?-1:board.carrying?1:0,cop?board.scans:-1,cop?-1:board.decoys,cop?board.clueText():"",
     deadline,now,note);
   }
  }
  String pathText(List<Integer> path){StringJoiner text=new StringJoiner(",");for(int p:path)text.add(String.valueOf(p));return text.toString();}
  void resolve(){
   if(!phase.equals("TURN"))return;
   boolean[] timeout={actions[0]==null,actions[1]==null};
   for(int k=0;k<2;k++)if(actions[k]==null)actions[k]=Museum.Action.stay();
   Museum.Result result=board.resolve(actions[police],actions[1-police]);
   boolean ended=!result.ending().equals("NONE");
   for(int k=0;k<2;k++){
    boolean cop=k==police;String note="";
    if(timeout[k])note="Hết 5 giây: bạn tự động đứng yên. ";
    if(cop){
     if(result.scan()!=null)note+=result.scan()?"QUÉT: có trộm trong vùng 3×3. ":"QUÉT: không có trộm trong vùng 3×3. ";
     if(result.noise())note+="Có tiếng động mới trên bản đồ. ";
     if(result.trace())note+="Phát hiện dấu chân. ";
     if(result.collected())note+="Chuông bảo tàng: một báu vật đã bị lấy. ";
    }else{
     if(result.collected())note+="Đã lấy báu vật! Hãy tới cửa thoát. ";
     if(actions[k].kind().equals("DASH"))note+="Bạn chạy nhanh và để lại tiếng động tại ô xuất phát. ";
     if(actions[k].kind().equals("DECOY"))note+="Đã tung mồi nhử. ";
    }
    if(note.isBlank())note="Đã thực hiện hành động. Vị trí đối thủ vẫn được giữ kín.";
    players[k].send("UPDATE",id,round,board.turn,pathText(cop?result.copPath():result.thiefPath()),cop?board.cop:board.thief,
     cop?board.clueText():"",cop?-1:board.relicMask,cop?-1:board.carrying?1:0,cop?board.scans:-1,cop?-1:board.decoys,note,
     cop&&result.scan()!=null?(result.scan()?"YES":"NO"):"",result.ending(),ended?(cop?board.thief:board.cop):-1,
     ended?pathText(cop?result.thiefPath():result.copPath()):"",cop?-1:board.exit,System.currentTimeMillis());
   }
   if(ended){
    int winner=result.ending().equals("ESCAPE")?1-police:police;score[winner]++;
    phase="ROUND_END";phaseEnd=System.currentTimeMillis()+3800;
    String reason=switch(result.ending()){case "CAPTURE"->"Cảnh sát đã bắt được trộm.";case "ESCAPE"->"Trộm đã mang báu vật ra khỏi bảo tàng.";default->"Hết 20 nhịp: cảnh sát bảo vệ thành công.";};
    for(int k=0;k<2;k++)players[k].send("ROUND_END",k==winner?"WIN":"LOSE",round,score[k],score[1-k],reason,board.cop,board.thief,board.exit);
   }else{phase="RESOLVE";phaseEnd=System.currentTimeMillis()+1200;}
  }
  void finish(int winner,String reason){
   if(phase.equals("FINISHED"))return;phase="FINISHED";phaseEnd=System.currentTimeMillis()+60000;
   Store.Match result=new Store.Match(id,players[0].user.name,players[1].user.name,score[0],score[1],round,winner<0?"":players[winner].user.name,start,System.currentTimeMillis(),"Bảo tàng · "+reason);
   try{store.finish(result);}
   catch(IOException e){pendingResults.put(id,result);nextSaveRetry=System.currentTimeMillis()+5000;both("ERROR","Chưa lưu được kết quả. Server sẽ thử lại; hãy kiểm tra thư mục data và giữ chủ phòng mở.");e.printStackTrace();}
   for(int k=0;k<2;k++)players[k].send("FINISHED",winner<0?"DRAW":winner==k?"WIN":"LOSE",score[k],score[1-k],reason);
   lobbies();
  }
  void tick(long now){
   switch(phase){
    case "TURN" -> {if(now>=deadline)resolve();}
    case "RESOLVE" -> {if(now>=phaseEnd){board.turn++;beginTurn("Chọn hành động cho nhịp tiếp theo.");}}
    case "ROUND_END" -> {if(now>=phaseEnd){if(round==1){round=2;prepare();}else finish(score[0]==score[1]?-1:score[0]>score[1]?0:1,"Hoàn tất hai ván đổi vai");}}
    case "FINISHED" -> {if(now>=phaseEnd){for(Peer p:players){p.room=null;p.send("BACK","Phòng đã đóng sau 60 giây.");}rooms.remove(this);lobbies();}}
   }
  }
 }
 public synchronized void close(){
  running=false;timer.shutdownNow();for(Peer p:new ArrayList<>(peers))disconnect(p);
  retryResults();
  try{listener.close();}catch(IOException ignored){}
 }
 public static void main(String[] args)throws Exception{
  int port=args.length>0?Integer.parseInt(args[0]):5555;
  Server s=new Server(port,Path.of("data"));
  Runtime.getRuntime().addShutdownHook(new Thread(s::close));s.run();
 }
}


