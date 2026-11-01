package nightchase;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
public final class UiTest{
 static int checks;static void check(boolean b,String m){if(!b)throw new AssertionError(m);checks++;}
 static void inspect(Component c){
  if(c instanceof JButton b)check(!b.getText().toLowerCase().contains("server"),"no server controls inside player UI");
  if(c instanceof Container parent)for(Component child:parent.getComponents())inspect(child);
 }
 static String[] state(boolean cop,String id,int round,int turn,int pos){
  long now=System.currentTimeMillis();
  return new String[]{"STATE",id,String.valueOf(round),String.valueOf(turn),cop?"POLICE":"THIEF","Opponent","0","0",String.valueOf(pos),cop?"-1":"42",cop?"-1":"3",cop?"-1":"0",cop?"2":"-1",cop?"-1":"1",cop?"29,NOISE,2;":"",String.valueOf(now+5000),String.valueOf(now),"Ready"};
 }
 public static void main(String[] args)throws Exception{
  SwingUtilities.invokeAndWait(()->{
   for(boolean hosting:new boolean[]{true,false}){
    Client c=new Client(hosting);try{
     check(c.view.equals("LOGIN")&&!c.sidePanel.isVisible()&&!c.footerPanel.isVisible(),"login is separate from game shell");
     check(!SwingUtilities.isDescendingFrom(c.confirm,c.root),"login does not include password confirmation");
     check(SwingUtilities.isDescendingFrom(c.login,c.root)&&!SwingUtilities.isDescendingFrom(c.register,c.root),"login has only its primary action");
     check(!SwingUtilities.isDescendingFrom(c.host,c.root)&&!SwingUtilities.isDescendingFrom(c.port,c.root),"network configuration outside authentication form");
     c.showAuth(true);check(c.view.equals("REGISTER")&&SwingUtilities.isDescendingFrom(c.confirm,c.root),"signup has confirmation");
     check(SwingUtilities.isDescendingFrom(c.register,c.root)&&!SwingUtilities.isDescendingFrom(c.login,c.root),"signup has only its primary action");
     c.name.setText("alice");c.password.setText("secret123");c.confirm.setText("different");c.authenticate(true);
     check(c.authStatus.getText().contains("không trùng"),"signup validation shown inline");
     c.receive(new String[]{"REGISTERED","OK"});check(c.view.equals("LOGIN")&&c.name.getText().equals("alice"),"registration returns to login with name preserved");
     check(c.password.getPassword().length==0,"password cleared on form switch");
     c.showLobby();check(c.sidePanel.isVisible()&&c.online!=null,"lobby appears only after authentication");inspect(c.root);
     c.receive(state(false,"ui",1,1,48));
     check(c.view.equals("GAME")&&c.enemyPos==-1,"opponent hidden during play");
     check(c.modeButtons.get("DASH").isVisible()&&!c.modeButtons.get("SCAN").isVisible(),"thief gets thief skills");
     c.chooseMode("DASH");c.chooseDirection("LEFT");check(c.kind.equals("DASH")&&c.direction.equals("LEFT"),"dash selection keeps two-step mode");
     c.chooseDirection("STAY");check(c.kind.equals("MOVE")&&c.direction.equals("STAY"),"stay cancels directional skill");
     c.receive(state(true,"ui",2,1,0));check(c.modeButtons.get("SCAN").isVisible()&&!c.modeButtons.get("DASH").isVisible(),"police gets scan instead of dash");
     c.chooseMode("SCAN");check(c.kind.equals("SCAN")&&c.direction.equals("STAY"),"scan consumes movement action");
     c.receive(new String[]{"LOCKED"});check(c.locked&&!c.send.isEnabled(),"locked input cannot be resubmitted");
     c.receive(new String[]{"ACTION_REJECTED","Wall"});check(!c.locked&&c.send.isEnabled(),"rejected action allows retry before deadline");
     c.receive(new String[]{"UPDATE","ui","2","1","1","1","29,NOISE,2;","-1","-1","1","-1","Scan says no","NO","NONE","-1","","-1","0"});
     check(c.enemyPos==-1&&c.exitCell==-1&&c.carrying==-1,"UI retains privacy sentinel fields");
     check(c.scanResult.equals("NO")&&c.clues.size()==1,"private scan and clues displayed");
     c.receive(new String[]{"ROUND_END","WIN","2","1","0","Captured","1","1","42"});
     check(c.enemyPos==1&&c.exitCell==42,"round result can reveal final positions");
     c.receive(new String[]{"FINISHED","WIN","1","0","Complete"});check(c.finished&&c.again.isVisible()&&c.again.isEnabled(),"replay available after full match only");
     c.receive(state(false,"new",1,1,48));check(c.enemyPos==-1&&c.ending.isEmpty()&&!c.again.isVisible(),"rematch removes revealed position and results");
     c.username="alice";
     c.receive(new String[]{"HISTORY","id|alice|bob|1:0|1|alice|2026-10-26|Win\n","0","13","125"});
     Client.DataView history=c.dataViews.get("HISTORY");
     check(history!=null&&history.rows.getRowCount()==1,"history page displayed");
     check(!history.previous.isEnabled()&&history.next.isEnabled(),"first page navigation");
     c.receive(new String[]{"HISTORY","last|alice|bob|1:1|2|Hòa|2026-10-26|Draw\n","12","13","125"});
     check(c.dataViews.get("HISTORY")==history&&history.page==12,"page response reuses history window");
     check(history.previous.isEnabled()&&!history.next.isEnabled()&&history.pageLabel.getText().contains("13 / 13"),"last page navigation and page count");
     c.receive(new String[]{"RANKING","1|alice|10|1|0|1|100%\n","0","1","1"});
     check(c.dataViews.size()==2&&!c.dataViews.get("RANKING").next.isEnabled(),"ranking and history keep separate windows");
     c.showLogin();check(!c.sidePanel.isVisible()&&!SwingUtilities.isDescendingFrom(c.confirm,c.root),"logout returns to clean login");
     check(c.dataViews.isEmpty()&&!history.isDisplayable(),"logout closes account data windows");
    }finally{c.shutdown();c.dispose();}
   }
  });
  System.out.println("PASS: "+checks+" UI assertions.");
 }
}
