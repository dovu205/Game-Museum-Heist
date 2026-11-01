package nightchase;

import java.io.*;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.util.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Server-owned SQLite storage. Legacy types retain their serialized identities. */
public final class Store {
 public static final int PAGE_SIZE=10;
 public static final class User implements Serializable {
  private static final long serialVersionUID=1;
  public final String name;final byte[] salt,hash;public int points,wins,losses,played;
  User(String n,String p){name=n;salt=new byte[16];new SecureRandom().nextBytes(salt);hash=hash(p,salt);}
  User(String n,byte[] s,byte[] h,int p,int w,int l,int count){name=n;salt=s;hash=h;points=p;wins=w;losses=l;played=count;}
 }
 public record Match(String id,String first,String second,int scoreFirst,int scoreSecond,int rounds,String winner,long start,long end,String reason) implements Serializable {}
 private static final class Data implements Serializable {
  private static final long serialVersionUID=1;
  Map<String,User> users=new HashMap<>();List<Match> matches=new ArrayList<>();
 }
 public record Page<T>(List<T> items,int page,int pages,int total) {}
 private final Path directory;
 private final String url;
 // Online peers hold these objects. Publish statistics only after COMMIT succeeds.
 private final Map<String,User> identities=new HashMap<>();
 private static final String MATCH_SELECT="SELECT m.*,a.user_name AS first_name,a.score AS first_score,b.user_name AS second_name,b.score AS second_score FROM matches m JOIN match_players a ON a.match_id=m.id AND a.seat=0 JOIN match_players b ON b.match_id=m.id AND b.seat=1";
 private static final String RANK_ORDER=" ORDER BY points DESC,wins DESC,CASE WHEN played=0 THEN 0.0 ELSE 1.0*wins/played END DESC,name ASC";

 public Store(Path dir)throws IOException {
  directory=dir.toAbsolutePath().normalize();Files.createDirectories(directory);
  url="jdbc:sqlite:"+directory.resolve("museumheist.db");
  try {Class.forName("org.sqlite.JDBC");}
  catch(ClassNotFoundException e){throw new IOException("Thiếu thư viện SQLite JDBC trong thư mục lib.",e);}
  try(Connection c=open()) {
   int version;
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery("PRAGMA user_version")){r.next();version=r.getInt(1);}
   if(version>1)throw new IOException("Database thuộc phiên bản game mới hơn; không thể hạ cấp.");
   c.setAutoCommit(false);
   try {
    if(version==0)createSchema(c);
    if(metadata(c,"legacy_import")==null)importLegacy(c);
    c.commit();
   }catch(SQLException|IOException|RuntimeException e){rollback(c,e);throw e;}
  }catch(SQLException e){throw failure(e);}
 }
 private Connection open()throws SQLException {
  Connection c=DriverManager.getConnection(url);
  try(Statement s=c.createStatement()) {
   s.execute("PRAGMA foreign_keys=ON");s.execute("PRAGMA busy_timeout=5000");s.execute("PRAGMA synchronous=FULL");
  }catch(SQLException e){c.close();throw e;}
  return c;
 }
 private void createSchema(Connection c)throws IOException,SQLException {
  try(InputStream in=Store.class.getResourceAsStream("/db/schema.sql")) {
   if(in==null)throw new IOException("Thiếu schema SQLite trong game.");
   String sql=new String(in.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
   try(Statement s=c.createStatement()){for(String part:sql.split(";"))if(!part.isBlank())s.execute(part);}
  }
 }
 private static String metadata(Connection c,String key)throws SQLException {
  try(PreparedStatement s=c.prepareStatement("SELECT value FROM metadata WHERE key=?")){s.setString(1,key);try(ResultSet r=s.executeQuery()){return r.next()?r.getString(1):null;}}
 }
 private void importLegacy(Connection c)throws IOException,SQLException {
  Path old=directory.resolve("nightchase.dat");String source="none";
  if(Files.exists(old)) {
   byte[] bytes=Files.readAllBytes(old);String digest;
   try{digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}
   catch(NoSuchAlgorithmException e){throw new IOException(e);}
   Path backups=directory.resolve("legacy-backups");Files.createDirectories(backups);
   Path backup=backups.resolve("nightchase-"+digest+".dat");
   if(!Files.exists(backup))Files.write(backup,bytes,StandardOpenOption.CREATE_NEW);
   if(!Arrays.equals(bytes,Files.readAllBytes(backup)))throw new IOException("Bản sao lưu dữ liệu cũ không khớp.");
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT count(*) FROM users")) {
    r.next();
    if(r.getInt(1)!=0)throw new IOException("Database đã có tài khoản nhưng chưa đánh dấu nhập dữ liệu; dừng để tránh ghi trùng.");
   }
   Data data;
   try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(bytes))) {
    in.setObjectInputFilter(ObjectInputFilter.Config.createFilter("maxdepth=20;maxrefs=1000000;java.base/*;nightchase.Store$*;!*"));
    data=(Data)in.readObject();
   }catch(ClassNotFoundException|ClassCastException e){throw new IOException("Không đọc được dữ liệu cũ; bản gốc được giữ nguyên.",e);}
   if(data==null||data.users==null||data.matches==null)throw new IOException("Dữ liệu cũ không hợp lệ.");
   for(var entry:data.users.entrySet()) {
    User u=entry.getValue();
    if(u==null||!Objects.equals(entry.getKey(),u.name)||u.salt==null||u.hash==null||u.salt.length!=16||u.hash.length!=32)throw new IOException("Tài khoản cũ không hợp lệ.");
    insertUser(c,u);
   }
   for(Match m:data.matches){validate(m);insertMatch(c,m);}
   source=digest;
  }
  try(PreparedStatement s=c.prepareStatement("INSERT INTO metadata(key,value) VALUES('legacy_import',?)")){s.setString(1,source);s.executeUpdate();}
 }
 static byte[] hash(String p,byte[] salt) {
  PBEKeySpec spec=new PBEKeySpec(p.toCharArray(),salt,120000,256);
  try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}
  catch(GeneralSecurityException e){throw new IllegalStateException(e);}finally{spec.clearPassword();}
 }
 private static IOException failure(SQLException e){return new IOException("Không truy cập được SQLite: "+e.getMessage(),e);}
 private static void rollback(Connection c,Exception cause){try{c.rollback();}catch(SQLException e){cause.addSuppressed(e);}}
 private static void insertUser(Connection c,User u)throws SQLException {
  try(PreparedStatement s=c.prepareStatement("INSERT INTO users(name,salt,password_hash,points,wins,losses,played) VALUES(?,?,?,?,?,?,?)")) {
   s.setString(1,u.name);s.setBytes(2,u.salt);s.setBytes(3,u.hash);s.setInt(4,u.points);s.setInt(5,u.wins);s.setInt(6,u.losses);s.setInt(7,u.played);s.executeUpdate();
  }
 }
 private static User readUser(ResultSet r)throws SQLException{return new User(r.getString("name"),r.getBytes("salt"),r.getBytes("password_hash"),r.getInt("points"),r.getInt("wins"),r.getInt("losses"),r.getInt("played"));}
 private static User find(Connection c,String name)throws SQLException {
  try(PreparedStatement s=c.prepareStatement("SELECT * FROM users WHERE name=?")){s.setString(1,name);try(ResultSet r=s.executeQuery()){return r.next()?readUser(r):null;}}
 }
 private User publish(User u) {
  if(u==null)return null;User current=identities.get(u.name);
  if(current==null){identities.put(u.name,u);return u;}
  current.points=u.points;current.wins=u.wins;current.losses=u.losses;current.played=u.played;return current;
 }
 public synchronized boolean register(String n,String p)throws IOException {
  try(Connection c=open()) {
   if(find(c,n)!=null)return false;
   User u=new User(n,p);insertUser(c,u);publish(u);return true;
  }catch(SQLException e){throw failure(e);}
 }
 public synchronized User login(String n,String p)throws IOException {
  try(Connection c=open()){User u=find(c,n);return u!=null&&MessageDigest.isEqual(u.hash,hash(p,u.salt))?publish(u):null;}
  catch(SQLException e){throw failure(e);}
 }
 public synchronized List<User> ranking()throws IOException {
  try(Connection c=open();Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT * FROM users"+RANK_ORDER)) {
   List<User> result=new ArrayList<>();while(r.next())result.add(publish(readUser(r)));return List.copyOf(result);
  }catch(SQLException e){throw failure(e);}
 }
 private static int count(Connection c,String sql,String user)throws SQLException {
  try(PreparedStatement s=c.prepareStatement(sql)){if(user!=null)s.setString(1,user);try(ResultSet r=s.executeQuery()){r.next();return r.getInt(1);}}
 }
 private static int checkedPage(int page,int total){if(page<0)throw new IllegalArgumentException("Trang không hợp lệ.");return Math.min(page,Math.max(0,(total-1)/PAGE_SIZE));}
 public synchronized Page<User> rankingPage(int requested)throws IOException {
  try(Connection c=open()) {
   c.setAutoCommit(false);int total=count(c,"SELECT count(*) FROM users",null),page=checkedPage(requested,total);
   List<User> users=new ArrayList<>();
   try(PreparedStatement s=c.prepareStatement("SELECT * FROM users"+RANK_ORDER+" LIMIT ? OFFSET ?")) {
    s.setInt(1,PAGE_SIZE);s.setInt(2,page*PAGE_SIZE);try(ResultSet r=s.executeQuery()){while(r.next())users.add(publish(readUser(r)));}
   }
   c.commit();return new Page<>(List.copyOf(users),page,Math.max(1,(total+PAGE_SIZE-1)/PAGE_SIZE),total);
  }catch(SQLException e){throw failure(e);}
 }
 private static Match readMatch(ResultSet r)throws SQLException {
  return new Match(r.getString("id"),r.getString("first_name"),r.getString("second_name"),r.getInt("first_score"),r.getInt("second_score"),r.getInt("rounds"),Objects.requireNonNullElse(r.getString("winner"),""),r.getLong("started_at"),r.getLong("ended_at"),r.getString("reason"));
 }
 public synchronized List<Match> history(String user)throws IOException {
  try(Connection c=open();PreparedStatement s=c.prepareStatement(MATCH_SELECT+" WHERE m.id IN (SELECT match_id FROM match_players WHERE user_name=?) ORDER BY m.ended_at DESC,m.id DESC LIMIT 100")) {
   s.setString(1,user);List<Match> list=new ArrayList<>();try(ResultSet r=s.executeQuery()){while(r.next())list.add(readMatch(r));}return List.copyOf(list);
  }catch(SQLException e){throw failure(e);}
 }
 public synchronized Page<Match> historyPage(String user,int requested)throws IOException {
  try(Connection c=open()) {
   c.setAutoCommit(false);int total=count(c,"SELECT count(*) FROM match_players WHERE user_name=?",user),page=checkedPage(requested,total);
   List<Match> list=new ArrayList<>();
   try(PreparedStatement s=c.prepareStatement(MATCH_SELECT+" WHERE m.id IN (SELECT match_id FROM match_players WHERE user_name=?) ORDER BY m.ended_at DESC,m.id DESC LIMIT ? OFFSET ?")) {
    s.setString(1,user);s.setInt(2,PAGE_SIZE);s.setInt(3,page*PAGE_SIZE);try(ResultSet r=s.executeQuery()){while(r.next())list.add(readMatch(r));}
   }
   c.commit();return new Page<>(List.copyOf(list),page,Math.max(1,(total+PAGE_SIZE-1)/PAGE_SIZE),total);
  }catch(SQLException e){throw failure(e);}
 }
 private static void validate(Match m)throws IOException {
  if(m==null||m.id==null||m.id.isBlank()||m.first==null||m.second==null||m.first.equals(m.second)||m.winner==null||m.reason==null||m.scoreFirst<0||m.scoreSecond<0||m.rounds<0||(!m.winner.isEmpty()&&!m.winner.equals(m.first)&&!m.winner.equals(m.second)))throw new IOException("Kết quả trận không hợp lệ.");
 }
 private static void insertMatch(Connection c,Match m)throws SQLException {
  try(PreparedStatement s=c.prepareStatement("INSERT INTO matches(id,rounds,winner,started_at,ended_at,reason) VALUES(?,?,?,?,?,?)")) {
   s.setString(1,m.id);s.setInt(2,m.rounds);if(m.winner.isEmpty())s.setNull(3,Types.VARCHAR);else s.setString(3,m.winner);s.setLong(4,m.start);s.setLong(5,m.end);s.setString(6,m.reason);s.executeUpdate();
  }
  try(PreparedStatement s=c.prepareStatement("INSERT INTO match_players(match_id,user_name,seat,score) VALUES(?,?,?,?)")) {
   for(int k=0;k<2;k++){s.setString(1,m.id);s.setString(2,k==0?m.first:m.second);s.setInt(3,k);s.setInt(4,k==0?m.scoreFirst:m.scoreSecond);s.executeUpdate();}
  }
 }
 public synchronized void finish(Match m)throws IOException {
  validate(m);
  try(Connection c=open()) {
   c.setAutoCommit(false);
   try {
    try(PreparedStatement s=c.prepareStatement("SELECT 1 FROM matches WHERE id=?")) {
     s.setString(1,m.id);try(ResultSet r=s.executeQuery()){if(r.next()){c.rollback();return;}}
    }
    insertMatch(c,m);
    try(PreparedStatement s=c.prepareStatement("UPDATE users SET points=points+?,played=played+1,wins=wins+?,losses=losses+? WHERE name=?")) {
     for(int k=0;k<2;k++) {
      String name=k==0?m.first:m.second;
      s.setInt(1,k==0?m.scoreFirst:m.scoreSecond);s.setInt(2,m.winner.equals(name)?1:0);s.setInt(3,!m.winner.isEmpty()&&!m.winner.equals(name)?1:0);s.setString(4,name);
      if(s.executeUpdate()!=1)throw new SQLException("Không tìm thấy người chơi.");
     }
    }
    User a=find(c,m.first),b=find(c,m.second);c.commit();publish(a);publish(b);
   }catch(SQLException|RuntimeException e){rollback(c,e);throw e;}
  }catch(SQLException e){throw failure(e);}
 }
 public static void main(String[] args)throws Exception {
  Path dir=Path.of(args.length==0?"data":args[0]);Store store=new Store(dir);
  try(Connection c=store.open()) {
   System.out.println("SQLite ready: "+dir.toAbsolutePath().resolve("museumheist.db"));
   System.out.println("Users: "+count(c,"SELECT count(*) FROM users",null)+", matches: "+count(c,"SELECT count(*) FROM matches",null));
   try(Statement s=c.createStatement();ResultSet r=s.executeQuery("PRAGMA integrity_check")){r.next();System.out.println("Integrity: "+r.getString(1));}
  }
 }
}
