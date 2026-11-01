package nightchase;
import java.util.*;
public final class EngineTest{
 static int checks;
 static void check(boolean b,String message){if(!b)throw new AssertionError(message);checks++;}
 static Museum.Action move(String d){return new Museum.Action("MOVE",d);}
 static Museum.Result step(Museum b,Museum.Action c,Museum.Action t){return b.resolve(c,t);}
 public static void main(String[] args){
  Set<Integer> reached=new HashSet<>();ArrayDeque<Integer> q=new ArrayDeque<>();q.add(0);reached.add(0);
  while(!q.isEmpty()){int p=q.remove();for(String d:Museum.DIRECTIONS){int n=Museum.adjacent(p,d);if(reached.add(n))q.add(n);}}
  int open=0;for(int c=0;c<49;c++)if(Museum.walkable(c))open++;
  check(reached.size()==open,"all walkable cells are connected");
  for(int relic:Museum.RELICS)check(reached.contains(relic),"relic reachable");
  for(int exit:Museum.EXITS)check(reached.contains(exit),"exit reachable");
  check(Museum.adjacent(6,"RIGHT")==6,"cannot wrap right edge");
  check(Museum.adjacent(0,"UP")==0,"cannot leave top edge");
  check(Museum.path(3,"DOWN",2).equals(List.of(10)),"dash stops before a wall");
  Museum b=new Museum(42);b.cop=0;b.thief=1;
  check(step(b,move("RIGHT"),Museum.Action.stay()).ending().equals("CAPTURE"),"same-cell capture");
  b=new Museum(42);b.cop=0;b.thief=1;check(step(b,move("RIGHT"),move("LEFT")).ending().equals("CAPTURE"),"swapping cells causes capture");
  b=new Museum(42);b.cop=1;b.thief=3;check(step(b,Museum.Action.stay(),new Museum.Action("DASH","LEFT")).ending().equals("CAPTURE"),"dash cannot pass through a stationary officer");
  b=new Museum(42);b.thief=30;Museum.Result r=step(b,Museum.Action.stay(),move("LEFT"));
  check(r.collected()&&b.carrying&&b.relicMask==2,"automatic relic collection");
  b=new Museum(42);b.thief=43;b.carrying=true;check(step(b,Museum.Action.stay(),move("LEFT")).ending().equals("ESCAPE"),"carried relic allows escape");
  b=new Museum(42);b.thief=43;check(step(b,Museum.Action.stay(),move("LEFT")).ending().equals("NONE"),"exit requires relic");
  b=new Museum(27);b.thief=20;b.carrying=true;r=step(b,Museum.Action.stay(),new Museum.Action("DASH","DOWN"));
  check(r.ending().equals("ESCAPE")&&b.thief==27,"dash stops as soon as carried relic reaches exit");
  b=new Museum(42);b.cop=42;b.thief=43;b.carrying=true;check(step(b,Museum.Action.stay(),move("LEFT")).ending().equals("CAPTURE"),"capture takes priority at exit");
  b=new Museum(42);b.turn=20;check(step(b,Museum.Action.stay(),Museum.Action.stay()).ending().equals("TIMEOUT"),"officer wins at twenty turns");
  b=new Museum(42);b.cop=16;b.thief=24;check(Boolean.TRUE.equals(step(b,new Museum.Action("SCAN","STAY"),Museum.Action.stay()).scan()),"scan detects diagonal in 3x3");
  check(b.scans==1&&b.cop==16,"scan consumes use and does not move");
  b.thief=48;check(Boolean.FALSE.equals(step(b,new Museum.Action("SCAN","STAY"),Museum.Action.stay()).scan()),"scan does not detect distant thief");
  check(b.validate(true,new Museum.Action("SCAN","STAY"))!=null,"third scan rejected");
  b=new Museum(42);int original=b.thief;step(b,Museum.Action.stay(),new Museum.Action("DECOY","LEFT"));
  check(b.thief==original&&b.decoys==0,"decoy replaces movement and consumes only use");
  check(b.clues.get(0).cell()==47&&b.clues.get(0).kind().equals("NOISE"),"decoy looks like a generic noise");
  check(b.validate(false,new Museum.Action("DECOY","LEFT"))!=null,"second decoy rejected");
  b=new Museum(42);step(b,Museum.Action.stay(),new Museum.Action("DASH","LEFT"));
  check(b.thief==46&&b.clues.get(0).cell()==48&&b.clues.get(0).kind().equals("NOISE"),"dash advances two cells and reveals origin as noise");
  b.turn=4;step(b,Museum.Action.stay(),Museum.Action.stay());check(b.clues.isEmpty(),"old noise expires");
  b=new Museum(42);b.turn=3;step(b,Museum.Action.stay(),move("LEFT"));check(b.clues.get(0).kind().equals("TRACE")&&b.clues.get(0).cell()==48,"walking on third turn leaves old footprint");
  check(b.validate(true,new Museum.Action("DASH","RIGHT"))!=null,"officer cannot dash");
  check(b.validate(false,new Museum.Action("SCAN","STAY"))!=null,"thief cannot scan");
  check(b.validate(false,new Museum.Action("TELEPORT","UP"))!=null,"unknown skills rejected");
  Random random=new Random(7);
  for(int game=0;game<100;game++){
   b=new Museum(Museum.EXITS[game%4]);
   for(int turn=1;turn<=20;turn++){
    b.turn=turn;List<Museum.Action> ca=actions(b,true),ta=actions(b,false);
    r=step(b,ca.get(random.nextInt(ca.size())),ta.get(random.nextInt(ta.size())));
    check(Museum.walkable(b.cop)&&Museum.walkable(b.thief),"random valid play never enters walls");
    check(b.scans>=0&&b.decoys>=0,"resources never negative");
    check(r.copPath().size()<=1&&r.thiefPath().size()<=2,"movement bounded per role");
    if(!r.ending().equals("NONE"))break;
   }
  }
  System.out.println("PASS: "+checks+" engine assertions.");
 }
 static List<Museum.Action> actions(Museum b,boolean cop){
  List<Museum.Action> result=new ArrayList<>();
  for(String kind:new String[]{"MOVE","DASH","SCAN","DECOY"})for(String dir:Museum.DIRECTIONS){Museum.Action a=new Museum.Action(kind,dir);if(b.validate(cop,a)==null)result.add(a);}return result;
 }
}
