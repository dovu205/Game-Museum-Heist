package nightchase;
import java.awt.*;
import java.awt.image.*;
import java.nio.file.*;
import javax.swing.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import javax.imageio.metadata.*;
public final class DemoPreview{
 public static void main(String[] args)throws Exception{
  SwingUtilities.invokeAndWait(()->{
   Client c=new Client(true);ImageWriter writer=ImageIO.getImageWritersByFormatName("gif").next();
   try{
    Path target=Path.of(args[0]);Files.createDirectories(target.getParent());Museum b=new Museum(21);
    c.username="Player";long now=System.currentTimeMillis();
    c.receive(new String[]{"STATE","demo","1","1","THIEF","Officer","0","0","48","21","3","0","-1","1","",String.valueOf(now+5000),String.valueOf(now),"Get the relic"});
    c.root.setSize(1280,880);c.invalidateDeep(c.root);c.layoutDeep(c.root);
    String[] kinds={"DASH","DASH","DASH","MOVE","MOVE","MOVE","MOVE","MOVE"};
    String[] dirs={"LEFT","LEFT","LEFT","UP","UP","RIGHT","LEFT","UP"};
    try(ImageOutputStream out=ImageIO.createImageOutputStream(target.toFile())){
     writer.setOutput(out);writer.prepareWriteSequence(null);int frame=0;
     for(int i=0;i<dirs.length;i++){
      int old=b.thief;b.turn=i+1;Museum.Result r=b.resolve(Museum.Action.stay(),new Museum.Action(kinds[i],dirs[i]));
      c.pos=b.thief;c.carrying=b.carrying?1:0;c.relicMask=b.relicMask;c.kind=kinds[i];c.direction=dirs[i];c.animationFrom=old;c.animationPath=r.thiefPath().stream().mapToInt(Integer::intValue).toArray();c.ending=r.ending();c.locked=true;c.deadline=0;if(!r.ending().equals("NONE"))c.enemyPos=b.cop;
      for(int f=0;f<10;f++){
       c.animationStart=System.currentTimeMillis()-f*80;BufferedImage img=new BufferedImage(c.canvas.getWidth(),c.canvas.getHeight(),BufferedImage.TYPE_INT_RGB);
       Graphics2D g=img.createGraphics();c.canvas.printAll(g);g.dispose();
       ImageWriteParam param=writer.getDefaultWriteParam();IIOMetadata metadata=writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(img),param);String format=metadata.getNativeMetadataFormatName();IIOMetadataNode root=(IIOMetadataNode)metadata.getAsTree(format);
       IIOMetadataNode control=node(root,"GraphicControlExtension");control.setAttribute("disposalMethod","none");control.setAttribute("userInputFlag","FALSE");control.setAttribute("transparentColorFlag","FALSE");control.setAttribute("delayTime","8");control.setAttribute("transparentColorIndex","0");
       if(frame++==0){IIOMetadataNode ext=node(root,"ApplicationExtensions"),loop=new IIOMetadataNode("ApplicationExtension");loop.setAttribute("applicationID","NETSCAPE");loop.setAttribute("authenticationCode","2.0");loop.setUserObject(new byte[]{1,0,0});ext.appendChild(loop);}
       metadata.setFromTree(format,root);writer.writeToSequence(new IIOImage(img,null,metadata),param);
      }
     }
     writer.endWriteSequence();
    }
    System.out.println("Museum demo GIF created");
   }catch(Exception e){throw new RuntimeException(e);}finally{writer.dispose();c.shutdown();c.dispose();}
  });
 }
 static IIOMetadataNode node(IIOMetadataNode root,String name){for(int i=0;i<root.getLength();i++)if(root.item(i).getNodeName().equals(name))return (IIOMetadataNode)root.item(i);IIOMetadataNode n=new IIOMetadataNode(name);root.appendChild(n);return n;}
}
