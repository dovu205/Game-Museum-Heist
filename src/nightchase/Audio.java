package nightchase;
import javax.sound.sampled.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** Original synthesized WAV assets; gracefully silent when an audio device is unavailable. */
public final class Audio implements AutoCloseable {
 private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"audio");t.setDaemon(true);return t;});
 private final Map<String,Clip> clips=new HashMap<>();private String theme="menu";private boolean muted;private float volume=.55f;
 public void music(String name){worker.execute(()->{if(theme.equals(name)&&clips.get(name)!=null&&clips.get(name).isRunning())return;stop(theme);theme=name;if(!muted)play(name,true);});}
 public void effect(String name){worker.execute(()->{if(!muted)play(name,false);});}
 public void mute(boolean value){worker.execute(()->{muted=value;if(muted)clips.values().forEach(Clip::stop);else play(theme,true);});}
 public void volume(int value){worker.execute(()->{volume=value/100f;clips.values().forEach(this::gain);});}
 private void stop(String name){Clip c=clips.get(name);if(c!=null)c.stop();}
 private void gain(Clip c){if(c.isControlSupported(FloatControl.Type.MASTER_GAIN)){FloatControl g=(FloatControl)c.getControl(FloatControl.Type.MASTER_GAIN);g.setValue(Math.max(g.getMinimum(),Math.min(g.getMaximum(),(float)(20*Math.log10(Math.max(.001,volume))))));}}
 private void play(String name,boolean loop){
  try{
   Clip c=clips.get(name);
   if(c==null){
    InputStream raw=Audio.class.getResourceAsStream("/audio/"+name+".wav");if(raw==null)return;
    try(AudioInputStream stream=AudioSystem.getAudioInputStream(new BufferedInputStream(raw))){c=AudioSystem.getClip();c.open(stream);}
    clips.put(name,c);
   }
   c.stop();c.setFramePosition(0);gain(c);if(loop)c.loop(Clip.LOOP_CONTINUOUSLY);else c.start();
  }catch(Exception ignored){/* Audio is optional on headless/remote machines. */}
 }
 public void close(){worker.execute(()->{for(Clip c:clips.values())c.close();clips.clear();});worker.shutdown();}
}
