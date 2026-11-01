package nightchase;

import java.util.*;

/** Pure referee for a simultaneous, hidden-information 7x7 pursuit. */
public final class Museum {
 public static final int SIZE=7,MAX_TURNS=20,TURN_MS=5000;
 public static final String[] MAP={".......",".##.#..","...#...",".#...#.","...#...","..#.##.","......."};
 public static final int COP_START=0,THIEF_START=48;
 public static final int[] RELICS={29,19},EXITS={42,6,21,27};
 public static final String[] DIRECTIONS={"UP","RIGHT","DOWN","LEFT","STAY"};
 static final int[] DX={0,1,0,-1,0},DY={-1,0,1,0,0};
 public record Action(String kind,String direction){
  public Action{Objects.requireNonNull(kind);Objects.requireNonNull(direction);}
  public static Action stay(){return new Action("MOVE","STAY");}
 }
 public record Clue(int cell,String kind,int until){}
 public record Result(String ending,Boolean scan,boolean collected,boolean noise,boolean trace,int oldCop,int oldThief,List<Integer> copPath,List<Integer> thiefPath){}
 public int cop=COP_START,thief=THIEF_START,exit,turn=1,relicMask=3,scans=2,decoys=1;
 public boolean carrying;
 public final List<Clue> clues=new ArrayList<>();
 public Museum(int exit){if(!walkable(exit))throw new IllegalArgumentException("Invalid exit");this.exit=exit;}
 public static int x(int cell){return cell%7;}
 public static int y(int cell){return cell/7;}
 public static boolean walkable(int cell){return cell>=0&&cell<49&&MAP[y(cell)].charAt(x(cell))!='#';}
 public static int direction(String d){for(int i=0;i<DIRECTIONS.length;i++)if(DIRECTIONS[i].equals(d))return i;throw new IllegalArgumentException("Hướng không hợp lệ.");}
 public static int adjacent(int cell,String dir){
  int d=direction(dir),nx=x(cell)+DX[d],ny=y(cell)+DY[d];
  return nx<0||nx>6||ny<0||ny>6||!walkable(ny*7+nx)?cell:ny*7+nx;
 }
 public static List<Integer> path(int cell,String dir,int steps){
  List<Integer> cells=new ArrayList<>();for(int i=0;i<steps;i++){int next=adjacent(cell,dir);if(next==cell)break;cells.add(next);cell=next;}return cells;
 }
 public String validate(boolean police,Action action){
  int pos=police?cop:thief;
  try{direction(action.direction);}catch(IllegalArgumentException e){return e.getMessage();}
  return switch(action.kind){
   case "MOVE" -> !action.direction.equals("STAY")&&adjacent(pos,action.direction)==pos?"Đường bị tường chắn. Chọn hướng khác.":null;
   case "DASH" -> police?"Cảnh sát không dùng Chạy nhanh.":action.direction.equals("STAY")||adjacent(pos,action.direction)==pos?"Chọn hướng chạy không bị chắn.":null;
   case "SCAN" -> !police?"Chỉ cảnh sát được quét.":scans<=0?"Bạn đã dùng hết hai lượt quét.":!action.direction.equals("STAY")?"Quét thay cho di chuyển.":null;
   case "DECOY" -> police?"Chỉ trộm được tung mồi nhử.":decoys<=0?"Mồi nhử đã được dùng.":action.direction.equals("STAY")||adjacent(pos,action.direction)==pos?"Chọn ô lân cận không bị chắn.":null;
   default -> "Hành động không hợp lệ.";
  };
 }
 public Result resolve(Action copAction,Action thiefAction){
  if(validate(true,copAction)!=null||validate(false,thiefAction)!=null)throw new IllegalArgumentException("Invalid action");
  clues.removeIf(c->c.until<turn);
  int oldCop=cop,oldThief=thief;boolean noise=false,trace=false,collected=false;
  if(thiefAction.kind.equals("DASH")){clues.add(new Clue(thief,"NOISE",turn+1));noise=true;}
  if(thiefAction.kind.equals("DECOY")){decoys--;clues.add(new Clue(adjacent(thief,thiefAction.direction),"NOISE",turn+1));noise=true;}
  if(thiefAction.kind.equals("MOVE")&&!thiefAction.direction.equals("STAY")&&turn%3==0){clues.add(new Clue(thief,"TRACE",turn+2));trace=true;}
  List<Integer> cp=copAction.kind.equals("MOVE")?path(cop,copAction.direction,1):List.of();
  List<Integer> tp=thiefAction.kind.equals("MOVE")?path(thief,thiefAction.direction,1):thiefAction.kind.equals("DASH")?path(thief,thiefAction.direction,2):List.of();
  List<Integer> actualCop=new ArrayList<>(),actualThief=new ArrayList<>();
  boolean caught=false,escaped=false;
  for(int step=0;step<Math.max(1,Math.max(cp.size(),tp.size()));step++){
   int prevCop=cop,prevThief=thief;
   if(step<cp.size()){cop=cp.get(step);actualCop.add(cop);}
   if(step<tp.size()){thief=tp.get(step);actualThief.add(thief);}
   if(cop==thief||(prevCop==thief&&prevThief==cop)){caught=true;break;}
   for(int i=0;i<RELICS.length;i++)if(thief==RELICS[i]&&(relicMask&(1<<i))!=0){carrying=true;relicMask&=~(1<<i);collected=true;}
   if(carrying&&thief==exit){escaped=true;break;}
  }
  Boolean scan=null;
  if(copAction.kind.equals("SCAN")){scans--;scan=Math.abs(x(cop)-x(thief))<=1&&Math.abs(y(cop)-y(thief))<=1;}
  String ending=caught?"CAPTURE":escaped?"ESCAPE":turn>=MAX_TURNS?"TIMEOUT":"NONE";
  return new Result(ending,scan,collected,noise,trace,oldCop,oldThief,List.copyOf(actualCop),List.copyOf(actualThief));
 }
 public static String grid(){return String.join("",MAP);}
 public String clueText(){StringBuilder s=new StringBuilder();for(Clue c:clues)if(c.until>=turn)s.append(c.cell).append(',').append(c.kind).append(',').append(c.until-turn+1).append(';');return s.toString();}
 public static String cellName(int cell){return cell<0?"?":String.valueOf((char)('A'+x(cell)))+(y(cell)+1);}
}
