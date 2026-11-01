from pathlib import Path
import wave, math, struct, random
ROOT=Path(__file__).resolve().parents[1]/"assets"/"audio"
ROOT.mkdir(parents=True,exist_ok=True)
RATE=22050
def save(name,seconds,fn):
    rng=random.Random(17)
    samples=bytearray()
    for i in range(int(seconds*RATE)):
        t=i/RATE
        value=max(-.95,min(.95,fn(t,rng)))
        samples.extend(struct.pack("<h",int(value*28000)))
    with wave.open(str(ROOT/(name+".wav")),"wb") as f:
        f.setnchannels(1);f.setsampwidth(2);f.setframerate(RATE);f.writeframes(samples)
def note(n):return 440*2**((n-69)/12)
def synth(t,chase,rng):
    # 120 BPM, exact 8-bar loop. Original minor-key arpeggio, bass and light percussion.
    beat=t*2;step=int(beat*2);p=(beat*2)%1
    harmony=[45,41,48,43][int(beat//8)%4]
    n=harmony+12+[0,7,12,3,7,10,7,3][step%8]
    arp=.14*math.sin(2*math.pi*note(n)*t)*math.exp(-5*p)
    bp=beat%1
    bass=.17*math.sin(2*math.pi*note(harmony)*t)*(.75+.25*math.cos(2*math.pi*bp))
    pad=.07*(math.sin(2*math.pi*note(harmony+12)*t)+.6*math.sin(2*math.pi*note(harmony+19)*t))
    drums=0
    if chase:
        drums=.26*math.sin(2*math.pi*(50*bp+14*(1-math.exp(-25*bp))))*math.exp(-15*bp)
        drums+=(rng.random()*2-1)*.08*math.exp(-25*p)
        if int(beat)%2:drums+=(rng.random()*2-1)*.12*math.exp(-18*bp)
        arp*=1.2
    # periodic fade avoids clicks at the loop boundary
    edge=min(1,t/.025,(16-t)/.025)
    return edge*(arp+bass+pad+drums)*.65
save("menu",16,lambda t,r:synth(t,False,r))
save("chase",16,lambda t,r:synth(t,True,r))
def tone(name,notes,length=.18):
    seconds=len(notes)*length
    def f(t,r):
        k=min(len(notes)-1,int(t/length));u=t%length
        env=min(1,u/.01)*max(0,1-u/length)**1.5
        return .35*math.sin(2*math.pi*note(notes[k])*t)*env
    save(name,seconds,f)
tone("click",[76],.055)
tone("lock",[57,64],.09)
tone("round",[57,64,69],.10)
tone("tick",[81],.075)
tone("invite",[69,76,69,76],.13)
tone("drop",[64,69,76,81],.10)
tone("win",[69,72,76],.15)
tone("lose",[60,55,48],.16)
tone("victory",[57,60,64,69,72,76,81],.14)


# Short police siren and escaping engine cues.
save("caught",.85,lambda t,r:.22*math.sin(2*math.pi*(660*t+90*.85/(2*math.pi)*math.sin(2*math.pi*t/.85)))*min(1,t/.02)*max(0,1-t/.85))
save("escape",.8,lambda t,r:.25*math.sin(2*math.pi*(95*t+130*t*t))*(1-t/.8)+.04*(r.random()*2-1)*(1-t/.8))

save("finale",16,lambda t,r:synth(t,True,r)*.9+.11*math.sin(2*math.pi*note(69+[0,3,7,10][int(t*4)%4])*t)*math.exp(-7*((t*4)%1))*min(1,t/.03,(16-t)/.03))
save("tension",3,lambda t,r:.19*math.sin(2*math.pi*(95*t+20*t*t))*math.exp(-9*((t*5)%1))*min(1,t/.01,(3-t)/.03))
save("scan",.6,lambda t,r:.22*math.sin(2*math.pi*(350*t+650*t*t))*math.sin(math.pi*t/.6))
save("smoke",.7,lambda t,r:.2*(r.random()*2-1)*math.sin(math.pi*t/.7))
for i,n in enumerate([60,64,69]):tone("memory"+str(i),[n],.18)
print("Generated",len(list(ROOT.glob("*.wav"))),"original WAV assets")
