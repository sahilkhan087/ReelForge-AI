import os, uuid, json, shutil, subprocess, threading
from pathlib import Path
from fastapi import FastAPI, UploadFile, File, HTTPException
from fastapi.responses import FileResponse
from fastapi.middleware.cors import CORSMiddleware

BASE=Path(__file__).resolve().parent
JOBS=BASE/"jobs"; JOBS.mkdir(exist_ok=True)
app=FastAPI(title="ReelForge AI API",version="5.0")
app.add_middleware(CORSMiddleware,allow_origins=["*"],allow_methods=["*"],allow_headers=["*"])
jobs={}

def process(job_id, src):
    out=JOBS/job_id/"output"; out.mkdir(parents=True,exist_ok=True)
    jobs[job_id]["status"]="processing"
    try:
        from faster_whisper import WhisperModel
        model=WhisperModel(os.getenv("WHISPER_MODEL","small"),device="cpu",compute_type="int8")
        segs,_=model.transcribe(str(src),vad_filter=True)
        segs=[{"start":float(s.start),"end":float(s.end),"text":s.text.strip()} for s in segs if s.text.strip()]
        candidates=[]
        for i,s in enumerate(segs):
            text=[]; end=s["end"]; start=s["start"]; chars=0
            for k in range(i,min(i+12,len(segs))):
                text.append(segs[k]["text"]); end=segs[k]["end"]; chars+=len(segs[k]["text"])
                if end-start>=40 or chars>=650: break
            if end-start>=12:
                t=" ".join(text)
                score=70+min(25,len(t)//120)+min(5,t.count("!")+t.count("?"))
                candidates.append({"start":start,"end":end,"transcript":t,"score":min(99,score)})
        candidates.sort(key=lambda x:x["score"],reverse=True)
        chosen=[]
        for c in candidates:
            if all(abs(c["start"]-x["start"])>25 for x in chosen):
                chosen.append(c)
            if len(chosen)==5: break
        shorts=[]
        for n,c in enumerate(chosen,1):
            dest=out/f"short_{n:02d}.mp4"
            dur=max(1,c["end"]-c["start"])
            cmd=["ffmpeg","-y","-ss",str(c["start"]),"-i",str(src),"-t",str(dur),
                 "-vf","scale=1080:-2,crop=1080:1920","-c:v","libx264","-preset","veryfast",
                 "-crf","23","-c:a","aac","-b:a","128k",str(dest)]
            subprocess.run(cmd,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.PIPE)
            words=c["transcript"].split()
            hook="Wait—this is the part you should not miss: "+" ".join(words[:14])
            shorts.append({"index":n,"score":c["score"],"start":c["start"],"end":c["end"],
                           "hook":hook[:180],"title":"Key insight from your video",
                           "hashtags":["#shorts","#reels","#ReelForgeAI"],
                           "file":f"/jobs/{job_id}/short_{n:02d}.mp4"})
        result={"job_id":job_id,"shorts":shorts,"content_factory":"30-day plan ready"}
        (out/"result.json").write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding="utf-8")
        jobs[job_id].update(status="completed",result=result)
    except Exception as e:
        jobs[job_id].update(status="failed",error=str(e))

@app.get("/health")
def health(): return {"ok":True,"version":"5.0"}

@app.post("/upload")
async def upload(file:UploadFile=File(...)):
    job_id=uuid.uuid4().hex[:12]; folder=JOBS/job_id; folder.mkdir(parents=True)
    src=folder/"input.mp4"
    with open(src,"wb") as f: shutil.copyfileobj(file.file,f)
    jobs[job_id]={"status":"queued","filename":file.filename}
    threading.Thread(target=process,args=(job_id,src),daemon=True).start()
    return {"job_id":job_id,"status":"queued"}

@app.get("/status/{job_id}")
def status(job_id:str):
    if job_id not in jobs: raise HTTPException(404,"Job not found")
    return jobs[job_id]

@app.get("/jobs/{job_id}/{filename}")
def media(job_id:str,filename:str):
    p=JOBS/job_id/"output"/filename
    if not p.exists(): raise HTTPException(404,"File not found")
    return FileResponse(p,media_type="video/mp4")
