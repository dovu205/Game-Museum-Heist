package nightchase;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;
import java.util.function.Consumer;
import javax.swing.SwingUtilities;

/** Network I/O stays off Swing's event thread. */
public final class ClientService implements AutoCloseable {
 private final ExecutorService sender=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"network-send");t.setDaemon(true);return t;});
 private final ScheduledExecutorService heartbeat=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"heartbeat");t.setDaemon(true);return t;});
 private final Consumer<String[]> receiver;private Socket socket;private BufferedWriter writer;private volatile boolean closed;
 public ClientService(Consumer<String[]> receiver){this.receiver=receiver;}
 public void connect(String host,int port,Runnable connected){
  sender.execute(()->{
   try{
    socket=new Socket();socket.connect(new InetSocketAddress(host,port),5000);socket.setTcpNoDelay(true);socket.setSoTimeout(45000);
    writer=new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8));
    Thread read=new Thread(()->{
     try(BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8))){
      String line;while(!closed&&(line=Wire.read(in))!=null){String[] m=Wire.decode(line);if(!m[0].equals("PONG"))SwingUtilities.invokeLater(()->receiver.accept(m));}
      if(!closed)notifyLost();
     }catch(Exception e){if(!closed)notifyLost();}
    },"network-read");read.setDaemon(true);read.start();
    heartbeat.scheduleAtFixedRate(()->send("PING"),10,10,TimeUnit.SECONDS);
    SwingUtilities.invokeLater(connected);
   }catch(Exception e){if(!closed){close();String detail=e instanceof UnknownHostException?"Không tìm thấy địa chỉ máy chủ.":e instanceof SocketTimeoutException?"Hết thời gian chờ; kiểm tra Wi-Fi và IP máy chủ.":e instanceof ConnectException?"Kết nối bị từ chối; kiểm tra chủ phòng đã mở và đúng cổng.":"Lỗi mạng: "+e.getMessage();SwingUtilities.invokeLater(()->receiver.accept(new String[]{"DISCONNECTED","Không kết nối được "+host+":"+port+". "+detail}));}}
  });
 }
 public void send(String op,Object...args){
  if(closed)return;
  try{sender.execute(()->{try{if(writer==null)throw new IOException();writer.write(Wire.encode(op,args));writer.newLine();writer.flush();}catch(IOException e){if(!closed)notifyLost();}});}catch(RejectedExecutionException ignored){}
 }
 private void notifyLost(){close();SwingUtilities.invokeLater(()->receiver.accept(new String[]{"DISCONNECTED","Mất kết nối server. Hãy đăng nhập lại."}));}
 public void close(){closed=true;heartbeat.shutdownNow();sender.shutdown();try{if(socket!=null)socket.close();}catch(IOException ignored){}}
}
