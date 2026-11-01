package nightchase;
import java.nio.file.*;

/** Compile/run against the original pre-SQLite JAR to create a compatibility fixture. */
public final class LegacyFixture {
 public static void main(String[] args)throws Exception {
  Path directory=Path.of(args[0]);Files.createDirectories(directory);
  Store store=new Store(directory);
  store.register("legacy_alice","old-alice-pass");store.register("legacy_bob","old-bob-pass");store.register("legacy_idle","old-idle-pass");
  store.finish(new Store.Match("legacy-win","legacy_alice","legacy_bob",2,0,2,"legacy_alice",100,200,"Bảo tàng · Hoàn tất hai ván đổi vai"));
  store.finish(new Store.Match("legacy-draw","legacy_alice","legacy_bob",1,1,2,"",300,400,"Hòa · dữ liệu cũ"));
  System.out.println("Legacy fixture created with original Store implementation.");
 }
}
