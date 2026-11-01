"""Rasterize the Word-exported PDF with the packaged document renderer."""
from pathlib import Path
import sys
sys.stdout.reconfigure(encoding='utf-8')
import importlib.util, tempfile, shutil, os
from pypdf import PdfReader
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[1]
QA=ROOT/'work/report'
QA.mkdir(parents=True,exist_ok=True)
tempfile.tempdir=str(QA)
renderer=Path('C:/Users/Admin/.codex/plugins/cache/openai-primary-runtime/documents/26.1026.10807/skills/documents/render_docx.py')
spec=importlib.util.spec_from_file_location('packaged_render_docx',renderer)
module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module)
pdf=ROOT/'bao_cao/Bao_cao_Canh_sat_va_Trom.pdf'
# LibreOffice is not bundled on this Windows host. Word exported the actual
# document; use the canonical PDF-to-PNG pipeline without modifying the skill.
def native_word_pdf(doc_path,profile,outdir,stem,verbose=False):
    target=Path(outdir)/(stem+'.pdf');shutil.copyfile(pdf,target)
    return str(target),'Native Microsoft Word PDF export'
module.convert_to_pdf=native_word_pdf
os.environ['PATH']='C:/Users/Admin/.cache/codex-runtimes/codex-primary-runtime/dependencies/native/poppler/Library/bin'+os.pathsep+os.environ['PATH']
pages=module.rasterize(str(ROOT/'bao_cao/Bao_cao_Canh_sat_va_Trom.docx'),str(QA/'render'),130,False,False)
reader=PdfReader(pdf)
summaries=[]
for i,page in enumerate(reader.pages,1):
    text=page.extract_text() or ''
    lines=[s.strip() for s in text.splitlines() if s.strip()]
    summaries.append({'page':i,'characters':len(text),'start':' | '.join(lines[:3]),'end':' | '.join(lines[-3:])})
    print(i,len(text),' | '.join(lines[:2]),' END ',' | '.join(lines[-2:]))
(QA/'page_summary.txt').write_text('\n'.join(str(x) for x in summaries),encoding='utf-8')
for start in range(0,len(pages),6):
    sheet=Image.new('RGB',(1260,1710),'#d6d6d6');d=ImageDraw.Draw(sheet)
    for j,path in enumerate(pages[start:start+6]):
        im=Image.open(path);im.thumbnail((410,805))
        x=(j%3)*420+(420-im.width)//2;y=(j//3)*855+30
        sheet.paste(im,(x,y));d.text(((j%3)*420+12,(j//3)*855+8),str(start+j+1),fill='black')
    sheet.save(QA/f'contact-{start//6+1}.jpg')
print('Rendered',len(pages),'pages')
