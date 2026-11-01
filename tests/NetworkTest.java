package nightchase;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
public final class NetworkTest{
 static int checks;
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
 static final class Bot implements AutoCloseable{
  Socket socket;BufferedWriter writer;BlockingQueue<String[]> inbox=new LinkedBlockingQueue<>();
  Bot(int port)throws Exception{
   socket=new Socket("127.0.0.1",port);writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
   Thread reader=new Thread(()->{try{BufferedReader r=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));String line;while((line=Wire.read(r))!=null)inbox.add(Wire.decode(line));}catch(Exception ignored){}});
   reader.setDaemon(true);reader.start();
  }
  void send(String type,Object...args)throws Exception{writer.write(Wire.encode(type,args));writer.newLine();writer.flush();}
  String[] await(String type)throws Exception{
   long end=System.currentTimeMillis()+10000;while(System.currentTimeMillis()<end){String[] m=inbox.poll(100,TimeUnit.MILLISECONDS);if(m!=null&&m[0].equals(type))return m;}throw new AssertionError("Missing "+type);
  }
  boolean has(String type){return inbox.stream().anyMatch(m->m[0].equals(type));}
  public void close()throws Exception{socket.close();}
 }
 static Server.Room room(Server s){return s.rooms.get(0);}
 static void advance(Server s){synchronized(s){room(s).phaseEnd=0;room(s).tick(System.currentTimeMillis());}}
 static void act(Bot b,String[] state,String kind,String dir)throws Exception{b.send("ACTION",state[1],state[2],state[3],kind,dir);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of("work");Files.createDirectories(root);Path dir=Files.createTempDirectory(root,"museum-network-");
  try(Server s=new Server(0,dir)){
   Thread thread=new Thread(()->{try{s.run();}catch(IOException e){throw new RuntimeException(e);}});thread.setDaemon(true);thread.start();
   try(Bot a=new Bot(s.port());Bot b=new Bot(s.port());Bot c=new Bot(s.port())){
    a.send("ACTION","x",1,1,"MOVE","STAY");a.await("ERROR");checks++;
    a.send("REGISTER","alice","secret123");a.await("REGISTERED");b.send("REGISTER","bob","secret123");b.await("REGISTERED");
    c.send("REGISTER","alice","secret123");check(c.await("ERROR")[1].contains("tồn tại"),"duplicate registration rejected");
    a.send("LOGIN","alice","wrong123");a.await("ERROR");checks++;
    a.send("LOGIN","alice","secret123");a.await("LOGIN_OK");b.send("LOGIN","bob","secret123");b.await("LOGIN_OK");
    c.send("LOGIN","alice","secret123");c.await("ERROR");checks++;
    a.send("INVITE","bob");b.await("INVITED");b.send("REJECT","alice");Thread.sleep(80);
    a.send("INVITE","bob");b.await("INVITED");b.send("ACCEPT","alice");
    String[] sa=a.await("STATE"),sb=b.await("STATE");String id=sa[1];
    check(sa[2].equals("1")&&sa[3].equals("1"),"round one starts at turn one");
    check(!sa[4].equals(sb[4]),"players have opposite roles");
    check(sa[15].equals(sb[15]),"shared authoritative deadline");
    boolean aCop=sa[4].equals("POLICE");Bot cop=aCop?a:b,thief=aCop?b:a;String[] cs=aCop?sa:sb,ts=aCop?sb:sa;
    check(cs[9].equals("-1")&&cs[10].equals("-1")&&cs[11].equals("-1"),"officer is not sent secret exit, loot state or carrying status");
    check(Integer.parseInt(ts[9])>=0&&ts[14].isEmpty()&&ts[12].equals("-1"),"thief receives exit but no officer clues or resources");
    act(cop,cs,"DASH","RIGHT");cop.await("ACTION_REJECTED");checks++;
    act(thief,ts,"SCAN","STAY");thief.await("ACTION_REJECTED");checks++;
    cop.send("ACTION",id,1,999,"MOVE","STAY");cop.await("ERROR");checks++;
    act(cop,cs,"MOVE","RIGHT");cop.await("LOCKED");Thread.sleep(100);
    check(!cop.has("UPDATE")&&!thief.has("UPDATE"),"one submission does not reveal or resolve positions");
    act(cop,cs,"MOVE","DOWN");cop.await("ERROR");checks++;
    act(thief,ts,"DASH","LEFT");thief.await("LOCKED");
    String[] cu=cop.await("UPDATE"),tu=thief.await("UPDATE");
    check(cu[14].equals("-1")&&cu[15].isEmpty(),"officer never receives hidden enemy position or path");
    check(tu[14].equals("-1")&&tu[15].isEmpty(),"thief never receives officer position or path");
    check(cu[6].contains("48,NOISE")&&tu[6].isEmpty(),"dash clue sent only to police");
    check(tu[5].equals("46"),"server executes two-cell dash");
    advance(s);sa=a.await("STATE");sb=b.await("STATE");cs=aCop?sa:sb;ts=aCop?sb:sa;
    // Both do nothing. Wait the actual server deadline of five seconds.
    long start=System.currentTimeMillis();cu=cop.await("UPDATE");tu=thief.await("UPDATE");
    check(System.currentTimeMillis()-start>=4500,"actual five-second timeout");
    check(cu[11].contains("đứng yên")&&tu[11].contains("đứng yên"),"missing inputs become stay rather than forged wins");
    advance(s);sa=a.await("STATE");sb=b.await("STATE");cs=aCop?sa:sb;ts=aCop?sb:sa;
    act(cop,cs,"SCAN","STAY");cop.await("LOCKED");act(thief,ts,"DECOY","LEFT");thief.await("LOCKED");
    cu=cop.await("UPDATE");tu=thief.await("UPDATE");
    check(cu[12].equals("NO")&&tu[12].isEmpty(),"scan gives only boolean to police");
    check(cu[6].contains("45,NOISE")&&!cu[6].contains("DECOY"),"fake noise not labeled as decoy");
    check(tu[10].equals("0")&&cu[9].equals("1"),"skill counters reduced server-side");
    // Force a legal adjacent encounter as a fixture and broadcast the corresponding state.
    synchronized(s){Server.Room r=room(s);r.board.cop=0;r.board.thief=1;r.beginTurn("Test capture");}
    sa=a.await("STATE");sb=b.await("STATE");cs=aCop?sa:sb;ts=aCop?sb:sa;
    act(cop,cs,"MOVE","RIGHT");act(thief,ts,"MOVE","STAY");cop.await("LOCKED");thief.await("LOCKED");
    cu=cop.await("UPDATE");tu=thief.await("UPDATE");check(cu[13].equals("CAPTURE")&&cu[14].equals("1"),"enemy coordinates only released after capture");
    String[] ce=cop.await("ROUND_END"),te=thief.await("ROUND_END");check(ce[1].equals("WIN")&&te[1].equals("LOSE"),"capture scores for officer");
    advance(s);sa=a.await("STATE");sb=b.await("STATE");check(sa[2].equals("2")&&!sa[4].equals(aCop?"POLICE":"THIEF"),"second round swaps roles");
    cop=aCop?b:a;thief=aCop?a:b;
    synchronized(s){Server.Room r=room(s);r.board.cop=0;r.board.thief=43;r.board.exit=42;r.board.carrying=true;r.beginTurn("Test escape");}
    sa=a.await("STATE");sb=b.await("STATE");cs=aCop?sb:sa;ts=aCop?sa:sb;
    act(cop,cs,"MOVE","STAY");act(thief,ts,"MOVE","LEFT");cop.await("LOCKED");thief.await("LOCKED");cu=cop.await("UPDATE");tu=thief.await("UPDATE");
    check(tu[13].equals("ESCAPE")&&tu[8].equals("1"),"thief escapes with relic");
    cop.await("ROUND_END");thief.await("ROUND_END");advance(s);
    String[] fa=a.await("FINISHED"),fb=b.await("FINISHED");check(!fa[1].equals(fb[1]),"two-round score decides winner");
    check(s.store.history("alice").size()==1,"only complete match recorded once");
    a.send("AGAIN");a.await("NOTICE");Thread.sleep(50);check(room(s).phase.equals("FINISHED"),"one replay request insufficient");
    b.send("AGAIN");sa=a.await("STATE");sb=b.await("STATE");check(!sa[1].equals(id),"rematch uses fresh room and board");
    a.send("EXIT");a.await("FINISHED");b.await("FINISHED");a.await("BACK");b.await("BACK");
    a.send("INVITE","bob");b.await("INVITED");b.send("ACCEPT","alice");a.await("STATE");b.await("STATE");b.close();
    a.await("FINISHED");a.await("BACK");a.send("RANKING");check(a.await("RANKING")[1].contains("alice"),"ranking available");
    a.send("HISTORY");check(a.await("HISTORY")[1].split("\n").length==3,"history includes surrender and disconnect");
    Store reopened=new Store(dir);check(reopened.login("alice","secret123")!=null&&reopened.history("alice").size()==3,"accounts and results survive restart");
    check(!new String(Files.readAllBytes(dir.resolve("museumheist.db")),StandardCharsets.ISO_8859_1).contains("secret123"),"password never stored in plaintext");
    c.send("LOGIN","bob","secret123");c.await("LOGIN_OK");a.send("INVITE","bob");c.await("INVITED");c.send("ACCEPT","alice");a.await("STATE");c.await("STATE");
    for(int round=1;round<=2;round++){
     synchronized(s){Server.Room r=room(s);r.board.turn=20;r.board.cop=0;r.board.thief=48;r.beginTurn("Test twentieth beat");}
     sa=a.await("STATE");sb=c.await("STATE");act(a,sa,"MOVE","STAY");act(c,sb,"MOVE","STAY");a.await("LOCKED");c.await("LOCKED");
     check(a.await("UPDATE")[13].equals("TIMEOUT"),"twentieth beat preserves police win");c.await("UPDATE");a.await("ROUND_END");c.await("ROUND_END");advance(s);
     if(round==1){a.await("STATE");c.await("STATE");}
    }
    fa=a.await("FINISHED");fb=c.await("FINISHED");check(fa[1].equals("DRAW")&&fb[1].equals("DRAW")&&fa[2].equals("1")&&fa[3].equals("1"),"two police wins produce a one-one draw");

    // A transient database failure must remain retriable after players leave the room.
    StoreTest.sql(dir,"CREATE TRIGGER reject_result BEFORE UPDATE ON users BEGIN SELECT RAISE(ABORT,'injected result write failure'); END");
    a.send("AGAIN");c.send("AGAIN");sa=a.await("STATE");c.await("STATE");String pendingId=sa[1];
    int playedBefore=s.store.login("alice","secret123").played;
    a.send("EXIT");a.await("ERROR");c.await("ERROR");a.await("FINISHED");c.await("FINISHED");a.await("BACK");c.await("BACK");
    synchronized(s){check(s.pendingResults.containsKey(pendingId),"failed result queued even after room closes");}
    check(s.store.login("alice","secret123").played==playedBefore,"failed server result does not change cached statistics");
    StoreTest.sql(dir,"DROP TRIGGER reject_result");
    synchronized(s){s.nextSaveRetry=0;s.tick();check(s.pendingResults.isEmpty(),"server tick retries and removes saved result");}
    a.await("NOTICE");c.await("NOTICE");
    check(new Store(dir).login("alice","secret123").played==playedBefore+1,"server retry saved exactly once");

   }
  }
  System.out.println("PASS: "+checks+" real TCP assertions.");
 }
}
