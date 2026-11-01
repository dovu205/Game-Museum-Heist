package nightchase;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.security.*;
import java.sql.*;
import java.util.*;

/** Import and verify all legacy fields without printing credentials or account names. */
public final class MigrationAudit {
 @SuppressWarnings("unchecked")
 public static void main(String[] args)throws Exception {
  Path directory=Path.of(args[0]);Path legacy=directory.resolve("nightchase.dat");
  byte[] original=Files.readAllBytes(legacy);Object data;
  try(ObjectInputStream in=new ObjectInputStream(new ByteArrayInputStream(original))) {
   in.setObjectInputFilter(ObjectInputFilter.Config.createFilter("maxdepth=20;maxrefs=1000000;java.base/*;nightchase.Store$*;!*"));
   data=in.readObject();
  }
  Field usersField=data.getClass().getDeclaredField("users"),matchesField=data.getClass().getDeclaredField("matches");usersField.setAccessible(true);matchesField.setAccessible(true);
  Map<String,Store.User> oldUsers=(Map<String,Store.User>)usersField.get(data);
  List<Store.Match> oldMatches=(List<Store.Match>)matchesField.get(data);
  Store store=new Store(directory);List<Store.User> users=store.ranking();
  if(users.size()!=oldUsers.size())throw new AssertionError("Account count differs");
  for(Store.User u:users) {
   Store.User old=oldUsers.get(u.name);
   if(old==null||!Arrays.equals(old.salt,u.salt)||!Arrays.equals(old.hash,u.hash)||old.points!=u.points||old.wins!=u.wins||old.losses!=u.losses||old.played!=u.played)throw new AssertionError("Account fields differ");
  }
  Set<Store.Match> imported=new HashSet<>();
  for(Store.User u:users){Store.Page<Store.Match> first=store.historyPage(u.name,0);imported.addAll(first.items());for(int page=1;page<first.pages();page++)imported.addAll(store.historyPage(u.name,page).items());}
  if(!imported.equals(new HashSet<>(oldMatches)))throw new AssertionError("Match fields differ");
  if(!Arrays.equals(original,Files.readAllBytes(legacy)))throw new AssertionError("Legacy file changed");
  String digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(original));
  if(!Arrays.equals(original,Files.readAllBytes(directory.resolve("legacy-backups/nightchase-"+digest+".dat"))))throw new AssertionError("Backup differs");
  try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+directory.toAbsolutePath().resolve("museumheist.db"));Statement s=c.createStatement()) {
   try(ResultSet r=s.executeQuery("PRAGMA integrity_check")){r.next();if(!r.getString(1).equals("ok"))throw new AssertionError("Integrity failed");}
   try(ResultSet r=s.executeQuery("PRAGMA foreign_key_check")){if(r.next())throw new AssertionError("Foreign keys failed");}
  }
  System.out.println("VERIFIED: "+users.size()+" accounts and "+oldMatches.size()+" matches; all credential hashes, statistics and match fields unchanged.");
  System.out.println("Legacy and backup SHA-256: "+digest);
  System.out.println("SQLite integrity and foreign keys: OK");
 }
}
