package nightchase;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Separate accounts can share one server and play independent matches. */
public final class MultiClientTest {
 static int checks;
 static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);checks++;}
 public static void main(String[] args)throws Exception {
  Files.createDirectories(Path.of("work"));Path directory=Files.createTempDirectory(Path.of("work"),"multi-client-");
  try(Server server=new Server(0,directory)) {
   Thread runner=new Thread(()->{try{server.run();}catch(IOException e){throw new RuntimeException(e);}});runner.setDaemon(true);runner.start();
   List<NetworkTest.Bot> bots=new ArrayList<>();
   try {
    for(int i=0;i<8;i++) {
     NetworkTest.Bot bot=new NetworkTest.Bot(server.port());bots.add(bot);
     bot.send("REGISTER","player"+i,"secret123");bot.await("REGISTERED");checks++;
    }
    // Submit all logins before waiting for any reply.
    for(int i=0;i<8;i++)bots.get(i).send("LOGIN","player"+i,"secret123");
    for(int i=0;i<8;i++)check(bots.get(i).await("LOGIN_OK")[1].equals("player"+i),"each distinct account logs in");
    synchronized(server){check(server.online.size()==8,"eight simultaneous logged-in clients");}
    try(NetworkTest.Bot duplicate=new NetworkTest.Bot(server.port())) {
     duplicate.send("LOGIN","player0","secret123");
     check(duplicate.await("ERROR")[1].contains("đang online"),"only duplicate account rejected");
    }
    String[][] states=new String[4][];
    for(int pair=0;pair<2;pair++) {
     int a=pair*2,b=a+1;
     bots.get(a).send("INVITE","player"+b);bots.get(b).await("INVITED");bots.get(b).send("ACCEPT","player"+a);
     states[a]=bots.get(a).await("STATE");states[b]=bots.get(b).await("STATE");
     check(states[a][1].equals(states[b][1]),"two players assigned same room");
    }
    check(!states[0][1].equals(states[2][1]),"two independent rooms");
    synchronized(server){check(server.rooms.size()==2&&server.online.size()==8,"two matches coexist with four waiting players");}
    for(int i=0;i<4;i++)NetworkTest.act(bots.get(i),states[i],"MOVE","STAY");
    for(int i=0;i<4;i++) {
     bots.get(i).await("LOCKED");String[] update=bots.get(i).await("UPDATE");
     check(update[1].equals(states[i][1]),"each player receives own room update");
    }
    bots.get(7).send("LIST");String[] lobby;
    do{lobby=bots.get(7).await("LOBBY");}while(!lobby[1].contains("player0|0|Đang chơi"));
    check(lobby[1].split("\n").length==8&&lobby[1].contains("player7|0|Rỗi"),"waiting clients stay in eight-player lobby");
    for(NetworkTest.Bot bot:bots){bot.send("PING");bot.await("PONG");checks++;}
   }finally{for(NetworkTest.Bot bot:bots)bot.close();}
  }
  System.out.println("PASS: "+checks+" multi-client assertions (8 accounts online, 2 matches, 4 waiting).");
 }
}
