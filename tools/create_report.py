"""Build the editable Vietnamese project report from verified local sources."""
from pathlib import Path
from math import atan2, cos, sin
from docx import Document
from docx.shared import Cm, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'bao_cao'
QA = ROOT / 'work/report'
OUT.mkdir(exist_ok=True)
QA.mkdir(parents=True, exist_ok=True)
FONT = Path('C:/Windows/Fonts')

def graphic(name, size):
    im = Image.new('RGB', size, 'white')
    return im, ImageDraw.Draw(im)

def font(n=28, bold=False):
    return ImageFont.truetype(str(FONT / ('arialbd.ttf' if bold else 'arial.ttf')), n)

def box(draw, rect, title, lines=(), fill='#eef3f8'):
    draw.rounded_rectangle(rect, 14, fill=fill, outline='#455a70', width=3)
    x, y, x2, y2 = rect
    draw.text(((x+x2)/2, y+30), title, font=font(30, True), fill='black', anchor='mt')
    for i, line in enumerate(lines):
        draw.text(((x+x2)/2, y+85+i*42), line, font=font(25), fill='#263645', anchor='mt')

def arrow(draw, xy, label='', above=True):
    x,y,x2,y2=xy
    draw.line(xy, fill='#304d68', width=4)
    a=atan2(y2-y,x2-x)
    draw.polygon([(x2,y2),(x2-18*cos(a-.45),y2-18*sin(a-.45)),(x2-18*cos(a+.45),y2-18*sin(a+.45))], fill='#304d68')
    if label:
        draw.text(((x+x2)/2,(y+y2)/2+(-28 if above else 28)),label,font=font(24),fill='black',anchor='mm')

# Repository-native diagrams are deterministic and contain no external artwork.
im,d=graphic('architecture',(1800,900))
box(d,(35,90,465,355),'CLIENT NGƯỜI CHƠI 1',('Java Swing','GameController','GameClient'))
box(d,(665,60,1135,440),'SERVER',('TcpServer và ClientHandler','Room và GameSession','Kiểm tra và xử lý luật','SQLite lưu tài khoản và trận'))
box(d,(1335,90,1765,355),'CLIENT NGƯỜI CHƠI 2',('Java Swing','GameController','GameClient'))
arrow(d,(465,180,665,180),'ACTION')
arrow(d,(665,300,465,300),'STATE / UPDATE',False)
arrow(d,(1335,180,1135,180),'ACTION')
arrow(d,(1135,300,1335,300),'STATE / UPDATE',False)
box(d,(450,595,1350,815),'MODULE SHARED',('GameRules và các kiểu dữ liệu dùng chung','Packet mã hóa giao thức  |  Không truyền toàn bộ GameState'))
arrow(d,(250,355,550,595));arrow(d,(900,440,900,595));arrow(d,(1550,355,1250,595))
im.save(QA/'architecture.png')

im,d=graphic('sequence',(1800,1050))
xs=[160,620,1180,1640]
for x,t in zip(xs,['Client cảnh sát','Server','Bộ xử lý luật','Client trộm']):
    d.rounded_rectangle((x-140,25,x+140,90),8,fill='#e8eef5',outline='#50647a',width=2)
    d.text((x,58),t,font=font(27,True),fill='black',anchor='mm')
    d.line((x,90,x,1010),fill='#aab4be',width=2)
for xy,txt in [((620,150,160,150),'STATE nhịp n'),((620,205,1640,205),'STATE cùng deadline'),((160,290,620,290),'ACTION bí mật'),((620,350,160,350),'LOCKED'),((1640,425,620,425),'ACTION bí mật'),((620,495,1180,495),'Đủ 2 hành động hoặc hết 8 giây'),((1180,650,620,650),'Kết quả và thông tin theo vai'),((620,735,160,735),'UPDATE riêng cho cảnh sát'),((620,800,1640,800),'UPDATE riêng cho trộm'),((620,920,160,920),'STATE nhịp n cộng 1')]:
    arrow(d,xy,txt)
d.rounded_rectangle((965,540,1395,605),8,fill='#f3f5f7',outline='#50647a',width=2)
d.text((1180,571),'Xử lý từng bước con',font=font(27),fill='black',anchor='mm')
im.save(QA/'sequence.png')

im,d=graphic('er',(1800,900))
box(d,(30,70,500,460),'users',('PK name','salt  |  password_hash','points  |  wins  |  losses','played'))
box(d,(665,110,1135,540),'match_players',('PK match_id và user_name','FK match_id → matches.id','FK user_name → users.name','seat  |  score'))
box(d,(1300,70,1770,460),'matches',('PK id','FK winner → users.name','rounds  |  started_at','ended_at  |  reason'))
arrow(d,(500,220,665,220),'1 đến nhiều');arrow(d,(1300,360,1135,360),'1 đến nhiều')
box(d,(515,655,1285,825),'metadata',('PK key  |  value','Thông tin nhập dữ liệu và phiên bản'))
im.save(QA/'er.png')

for name in ['thief','police']:
    Image.open(ROOT/f'work/preview-v2/{name}.png').crop((210,200,1250,775)).save(QA/f'{name}-detail.png')

doc=Document()
sec=doc.sections[0]
sec.page_width=Cm(21);sec.page_height=Cm(29.7)
sec.top_margin=Cm(2);sec.bottom_margin=Cm(2)
sec.left_margin=Cm(2.6);sec.right_margin=Cm(2)
sec.header_distance=Cm(.8);sec.footer_distance=Cm(.8)
for style in ['Normal','Title','Subtitle','Heading 1','Heading 2','Heading 3','Caption']:
    st=doc.styles[style]
    st.font.name='Times New Roman';st.font.color.rgb=RGBColor(0,0,0)
    st.element.get_or_add_rPr().append(OxmlElement('w:rFonts'))
    rf=st.element.rPr.rFonts
    for k in ['ascii','hAnsi','eastAsia','cs']:rf.set(qn('w:'+k),'Times New Roman')
normal=doc.styles['Normal']
normal.font.size=Pt(13)
normal.paragraph_format.line_spacing=1.18
normal.paragraph_format.space_after=Pt(6)
for name,size in [('Title',22),('Heading 1',17),('Heading 2',14),('Heading 3',13)]:
    st=doc.styles[name];st.font.size=Pt(size);st.font.bold=True
    st.paragraph_format.space_before=Pt(10);st.paragraph_format.space_after=Pt(8)
    st.paragraph_format.keep_with_next=True
doc.styles['Caption'].font.size=Pt(11)
doc.styles['Caption'].font.italic=True
doc.styles['Caption'].paragraph_format.space_before=Pt(5)
doc.styles['Caption'].paragraph_format.space_after=Pt(9)
sec.different_first_page_header_footer=True
footer=sec.footer.paragraphs[0];footer.alignment=WD_ALIGN_PARAGRAPH.CENTER
rr=footer.add_run();fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'PAGE');rr._r.addnext(fld)
doc.core_properties.title='Báo cáo bài tập lớn trò chơi Cảnh sát và Trộm'
doc.core_properties.subject='Ứng dụng Java TCP Client Server và xử lý hành động đồng thời'
doc.core_properties.author=''
doc.core_properties.keywords='Java, TCP, Client Server, Swing, SQLite, Cảnh sát và Trộm'

def p(text='',bold=False,align=None):
    follows_table=len(doc.element.body)>1 and doc.element.body[-2].tag==qn('w:tbl')
    pp=doc.add_paragraph();rr=pp.add_run(text);rr.bold=bold
    if follows_table:pp.paragraph_format.space_before=Pt(7)
    pp.alignment=align if align is not None else WD_ALIGN_PARAGRAPH.JUSTIFY
    return pp

def h(text,level=1):return doc.add_paragraph(text,'Heading '+str(level))
def page(title):
    if title=='Mục lục':
        pp=p(title,True,WD_ALIGN_PARAGRAPH.LEFT);pp.runs[0].font.size=Pt(17)
    else:pp=h(title)
    pp.paragraph_format.page_break_before=True
def bullets(lines):
    for text in lines:
        pp=doc.add_paragraph(style='List Bullet');pp.add_run(text);pp.paragraph_format.space_after=Pt(4)

def table(headers,rows,widths):
    tt=doc.add_table(rows=1,cols=len(headers));tt.alignment=WD_TABLE_ALIGNMENT.CENTER;tt.autofit=False
    for cell,w in zip(tt.columns,widths):cell.width=Cm(w)
    for i,label in enumerate(headers):tt.rows[0].cells[i].text=label
    for row in rows:
        cells=tt.add_row().cells
        for i,value in enumerate(row):cells[i].text=str(value)
    for ri,row in enumerate(tt.rows):
        trpr=row._tr.get_or_add_trPr()
        no=OxmlElement('w:cantSplit');trpr.append(no)
        if ri==0:
            repeat=OxmlElement('w:tblHeader');trpr.append(repeat)
        for ci,c in enumerate(row.cells):
            c.width=Cm(widths[ci]);c.vertical_alignment=WD_CELL_VERTICAL_ALIGNMENT.CENTER
            pr=c._tc.get_or_add_tcPr();b=OxmlElement('w:tcBorders')
            for edge in ['top','left','bottom','right']:
                e=OxmlElement('w:'+edge);e.set(qn('w:val'),'single');e.set(qn('w:sz'),'4');e.set(qn('w:color'),'D9D9D9');b.append(e)
            pr.append(b)
            pad=OxmlElement('w:tcMar')
            for k,v in [('top',80),('bottom',80),('left',100),('right',100)]:
                e=OxmlElement('w:'+k);e.set(qn('w:w'),str(v));e.set(qn('w:type'),'dxa');pad.append(e)
            pr.append(pad)
            shade=OxmlElement('w:shd');shade.set(qn('w:fill'),'E7EDF4' if ri==0 else ('F4F6F8' if ri%2==0 else 'FFFFFF'));pr.append(shade)
            for pp in c.paragraphs:
                pp.paragraph_format.space_after=Pt(2);pp.paragraph_format.line_spacing=1.05
                for rr in pp.runs:rr.font.size=Pt(11);rr.bold=ri==0
    return tt

def picture(path,caption,width=16):
    pp=doc.add_paragraph();pp.alignment=WD_ALIGN_PARAGRAPH.CENTER
    pp.paragraph_format.keep_with_next=True
    rr=pp.add_run();rr.add_picture(str(path),width=Cm(width))
    # Descriptive alternative text is retained in the DOCX.
    for el in rr._r.xpath('.//wp:docPr'):el.set('descr',caption)
    pp=doc.add_paragraph(caption,'Caption');pp.alignment=WD_ALIGN_PARAGRAPH.CENTER

def code(text):
    for line in text.strip().splitlines():
        pp=doc.add_paragraph();pp.paragraph_format.space_after=Pt(1)
        pp.paragraph_format.line_spacing=1.0
        rr=pp.add_run(line);rr.font.name='Consolas';rr.font.size=Pt(10)

# PAGE 1 — academic cover with intentionally blank identifying information.
p('TRƯỜNG ................................................................',True,WD_ALIGN_PARAGRAPH.CENTER)
p('KHOA .....................................................................',True,WD_ALIGN_PARAGRAPH.CENTER)
p('')
p('BÁO CÁO BÀI TẬP LỚN',True,WD_ALIGN_PARAGRAPH.CENTER).runs[0].font.size=Pt(18)
p('MÔN LẬP TRÌNH MẠNG',True,WD_ALIGN_PARAGRAPH.CENTER)
p('')
title=doc.add_paragraph('Xây dựng trò chơi\nCảnh sát và Trộm','Title');title.alignment=WD_ALIGN_PARAGRAPH.CENTER
p('Ứng dụng Java theo mô hình Client Server',False,WD_ALIGN_PARAGRAPH.CENTER)
p('Phiên bản 2',False,WD_ALIGN_PARAGRAPH.CENTER)
p('')
for text in ['Giảng viên hướng dẫn  ........................................................',
             'Lớp học phần  .......................................................................',
             'Nhóm thực hiện  ..................................................................',
             'Sinh viên và mã số sinh viên',
             '1  ...........................................................................................',
             '2  ...........................................................................................',
             '3  ...........................................................................................']:
    pp=p(text);pp.alignment=WD_ALIGN_PARAGRAPH.LEFT;pp.paragraph_format.left_indent=Cm(1.1)
p('')
p('Địa điểm  ............................    Năm  ............................',False,WD_ALIGN_PARAGRAPH.CENTER)

# PAGE 2
page('Mục lục')
toc=doc.add_paragraph();fld=OxmlElement('w:fldSimple');fld.set(qn('w:instr'),'TOC \\o "1-1" \\h \\z \\u');toc._p.append(fld)
p('')
h('Tóm tắt nội dung',2)
p('Báo cáo trình bày việc xây dựng trò chơi đối kháng Cảnh sát và Trộm bằng Java, sử dụng giao tiếp TCP giữa hai client và một server. Trọng tâm của đề tài là tổ chức kết nối nhiều người chơi, quản lý phòng đấu, bảo vệ thông tin bí mật và xử lý các hành động di chuyển đồng thời theo từng nhịp.')
p('Sản phẩm gồm ba module Maven shared, server và client. Luật phiên bản 2 bổ sung hai cửa thoát, kỹ năng đặt bẫy và luồn lách, tầm nhìn theo hàng hoặc cột và tin tình báo định kỳ. Hệ thống đã được kiểm tra bằng các bộ test luật chơi, cơ sở dữ liệu, TCP và giao diện. Phần cuối báo cáo hướng dẫn chạy hai người trên cùng máy hoặc qua mạng LAN và đánh giá các giới hạn còn lại.')

# PAGE 3
page('1 Giới thiệu đề tài')
h('1 1 Bối cảnh và bài toán',2)
p('Trong một trò chơi trực tuyến có thông tin ẩn, mỗi máy người chơi chỉ nên nhận những dữ liệu cần thiết để điều khiển và hiển thị nhân vật của mình. Nếu để client tự quyết định vị trí hoặc kết quả, hai máy có thể có trạng thái khác nhau và việc kiểm tra hành động trở nên khó kiểm soát. Vì vậy, đề tài lựa chọn mô hình server giữ trạng thái chính thức và làm trọng tài cho cả hai người.')
p('Cảnh sát có nhiệm vụ truy tìm trộm; trộm cần lấy báu vật rồi tới cửa thoát. Hai người chọn hành động độc lập trong cùng một khoảng thời gian. Sau khi nhận đủ hai hành động hoặc hết hạn, server xử lý chúng theo cùng một bộ quy tắc, xác định vị trí mới và gửi kết quả phù hợp với quyền xem của từng vai.')
h('1 2 Mục tiêu',2)
bullets(['Xây dựng ứng dụng Java có giao diện trực quan, hỗ trợ đăng ký, đăng nhập, thách đấu và chơi trực tuyến.',
         'Thiết kế giao thức TCP có định dạng rõ ràng và giới hạn độ dài gói tin.',
         'Xử lý nhất quán hai hành động bí mật, bao gồm các di chuyển có hai bước con.',
         'Lưu tài khoản, điểm và lịch sử trận đấu bằng SQLite.',
         'Tách mã nguồn thành các module để thuận tiện kiểm thử, bảo trì và mở rộng.'])
h('1 3 Phạm vi thực hiện',2)
p('Mỗi phòng có hai người; một server có thể phục vụ nhiều tài khoản và nhiều phòng độc lập. Ứng dụng hoạt động trên cùng máy hoặc mạng LAN với cổng TCP được cấu hình. Giao diện sử dụng Java Swing, bản đồ cố định 7 × 7 và dữ liệu trận được lưu tập trung trên máy chủ. Phiên bản hiện tại không cung cấp chat, ghép trận tự động hoặc tiếp tục trận sau khi client mất kết nối.')
h('1 4 Công nghệ sử dụng',2)
table(['Thành phần','Công nghệ và vai trò'],[
('Ngôn ngữ và môi trường','Java 17 trở lên; kiểm thử build bằng JDK 21 với target Java 17'),
('Giao diện','Java Swing và Java2D để vẽ bản đồ, nhân vật và hiệu ứng'),
('Giao tiếp','TCP Socket, dòng UTF 8 và mã hóa trường Base64'),
('Dữ liệu','SQLite qua JDBC; thư viện đi kèm trong thư mục lib'),
('Build và test','Maven nhiều module; JUnit và các bộ kiểm tra chạy trực tiếp bằng JDK')],[3.7,12.7])

# PAGE 4
page('2 Phân tích yêu cầu hệ thống')
h('2 1 Tác nhân và luồng sử dụng',2)
p('Hai tác nhân chính là người chơi và server. Người chơi đăng ký hoặc đăng nhập, xem người đang online, gửi lời mời và chấp nhận một trận đấu. Server xác thực tài khoản, kiểm tra trạng thái sẵn sàng của hai bên, tạo phòng và phân vai ngẫu nhiên cho ván đầu. Sau khi hai ván kết thúc, người chơi có thể xem kết quả, chọn chơi lại hoặc trở về sảnh.')
table(['Mã','Yêu cầu chức năng'],[
('FR01','Đăng ký, đăng nhập, đăng xuất; không cho cùng tài khoản online nhiều nơi.'),
('FR02','Hiển thị danh sách online, gửi và phản hồi lời thách đấu.'),
('FR03','Tạo trận hai ván, phân vai và đổi vai sau ván thứ nhất.'),
('FR04','Chọn hành động, xem đường dự kiến, khóa và gửi quyết định.'),
('FR05','Kiểm tra hành động, xử lý đồng thời, chấm thắng và thông báo riêng theo vai.'),
('FR06','Hiển thị bẫy, dấu vết, tầm nhìn, tình báo và nhật ký được phép xem.'),
('FR07','Lưu điểm, lịch sử và hiển thị các bảng có phân trang.'),
('FR08','Xử lý hết giờ, đầu hàng, mất kết nối và chơi lại khi cả hai đồng ý.')],[1.8,14.6])
h('2 2 Yêu cầu phi chức năng',2)
p('Tính nhất quán được bảo đảm bằng một trạng thái chính thức tại server. Các thay đổi gameplay được tuần tự hóa dưới cùng monitor, còn việc cập nhật Swing thực hiện trên Event Dispatch Thread. Server không sử dụng tọa độ client tự khai báo để quyết định vị trí mới.')
p('Tính riêng tư yêu cầu lọc gói tin ngay trước khi gửi, thay vì gửi toàn bộ trạng thái rồi chỉ giấu trên giao diện. Tính ổn định yêu cầu kiểm tra định dạng, giới hạn kích thước gói, từ chối hành động sai vai hoặc hết hạn và cho phép chọn lại khi còn thời gian. Tính lưu trữ yêu cầu thao tác ghi kết quả và thống kê phải hoàn tất trong cùng transaction.')
h('2 3 Kịch bản sử dụng tiêu biểu',2)
p('Người A và B đăng nhập bằng hai tài khoản khác nhau, cùng tham gia sảnh. A gửi lời mời, B chấp nhận. Mỗi nhịp, cả hai lựa chọn hành động và khóa. Server gửi kết quả riêng cho từng người. Khi ván đầu kết thúc, hai bên đổi vai. Sau ván thứ hai, server lưu kết quả và mở tùy chọn chơi lại.')

# PAGE 5
page('3 Thiết kế luật chơi')
h('3 1 Cấu trúc trận và bản đồ',2)
p('Một trận gồm hai ván đổi vai, không có hiệp phụ. Mỗi ván tối đa 32 nhịp; người chơi có tối đa 8 giây để gửi một hành động hợp lệ. Nhận đủ hai hành động thì server xử lý ngay. Người chưa gửi hợp lệ khi hết hạn tự đứng yên. Mỗi ván thắng được một điểm; tổng điểm 1–1 là hòa.')
p('Bản đồ 7 × 7 có ô đường đi và tường. Hai cửa cố định tại A7 và G1, đều được cả hai người nhìn thấy. Đầu ván, hai nhân vật và hai báu vật được chọn ngẫu nhiên tại bốn ô đường đi khác nhau, không trùng cửa thoát. MapGenerator kiểm tra tính kết nối trước khi lựa chọn vị trí.')
table(['Hành động','Vai sử dụng','Quy tắc chính'],[
('Đi bộ','Cả hai','Một ô lên, xuống, trái hoặc phải; bước bị chặn phải chọn lại.'),
('Chạy nhanh','Cả hai','Tối đa hai ô thẳng hàng, dừng trước vật cản. Chỉ trộm tạo tiếng động.'),
('Đứng yên','Cả hai','Không đổi vị trí; dùng chủ động hoặc tự áp dụng khi hết giờ.'),
('Đặt bẫy','Cảnh sát','Tối đa hai lần mỗi ván; đặt tại ô kề, thay cho di chuyển.'),
('Luồn lách','Trộm','Tối đa ba lần mỗi ván; hai bước vuông góc, không tạo tiếng động.')],[3.1,2.9,10.4])
h('3 2 Điều kiện thắng',2)
p('Cảnh sát thắng khi bắt được trộm hoặc hết nhịp 32 mà trộm chưa thoát. Trộm thắng khi đã có ít nhất một báu vật và tới một cửa hợp lệ. Báu vật tự được thu thập khi trộm đi vào hoặc đi qua ô chứa nó. Đến cửa khi chưa mang báu vật không kết thúc ván.')
p('Hai nhân vật cùng ô hoặc đổi chỗ trong cùng bước con được tính là bắt giữ. Nếu trộm vừa tới cửa vừa bị bắt trong một bước, bắt giữ được ưu tiên. Khi bắt hoặc thoát đã xảy ra, không thực hiện các bước còn lại. Rời trận hoặc mất kết nối được tính đầu hàng; không tự cộng điểm cho ván đang chơi dở.')

# PAGE 6
page('4 Kỹ năng và cơ chế thông tin')
h('4 1 Đặt bẫy và giữ trộm',2)
p('Cảnh sát chọn hướng để đặt bẫy tại ô đường đi kề. Không đặt tại cửa hoặc ô sát cửa, kể cả ô chéo; không đặt vào tường hoặc ô đã có bẫy. Mỗi lần hợp lệ trừ một lượt kỹ năng. Bẫy bắt đầu hoạt động từ nhịp kế tiếp và tồn tại đến hết ván, kể cả sau khi kích hoạt.')
p('Khi trộm bước vào bẫy đang hoạt động, server cắt phần đường còn lại, ghi nhận tọa độ và buộc trộm đứng yên ở nhịp tiếp theo. Sau một nhịp giữ, trộm được di chuyển bình thường. Bẫy chỉ kích hoạt khi đi vào; đứng yên trên đó không giữ trộm liên tục. Cảnh sát nhìn thấy mọi bẫy của mình, còn trộm chỉ biết bẫy đã mắc.')
h('4 2 Luồn lách',2)
p('Trộm chọn lần lượt hai hướng vuông góc. Hướng đầu quyết định bước một, hướng sau quyết định bước hai. Ví dụ B7 → B6 → A6 thể hiện việc đi lên rồi rẽ trái. Nếu bước đầu bị chặn thì hành động không hợp lệ; nếu chỉ bước thứ hai bị chặn thì dừng sau bước đầu và vẫn tiêu hao lượt kỹ năng. Trong từng bước, trộm vẫn có thể bị bắt hoặc mắc bẫy.')
h('4 3 Tầm nhìn và dấu vết',2)
table(['Nguồn thông tin','Người được thấy','Thời điểm và giới hạn'],[
('Tầm nhìn','Cả hai','Sau xử lý, cùng hàng hoặc cột, cách tối đa hai ô và không có tường.'),
('Bẫy kích hoạt','Cảnh sát và trộm mắc bẫy','Lộ tọa độ trộm khi mắc và trong nhịp bị giữ.'),
('Tiếng động','Cảnh sát','Trộm chạy nhanh tạo dấu tại ô xuất phát, tồn tại hai nhịp.'),
('Dấu chân','Cảnh sát','Đi bộ ở nhịp chia hết cho ba, tồn tại ba nhịp.'),
('Chuông báu vật','Cảnh sát','Chỉ báo có báu vật bị lấy; không gửi vị trí hoặc trạng thái còn lại.'),
('Tình báo','Cảnh sát','Sau nhịp 8, 16, 24; chỉ báo Bắc, Trung tâm hoặc Nam.')],[3.4,3.5,9.5])
p('Khi cảnh sát ở B1 và trộm ở C1, cả hai được nhìn thấy nhau vì đáp ứng tầm nhìn. Đây là thông tin hợp lệ theo luật. Ra khỏi phạm vi sau nhịp xử lý tiếp theo thì đối thủ bị ẩn lại, trừ trường hợp có hiệu lực phát hiện từ bẫy.')

# PAGE 7
page('5 Kiến trúc tổng thể')
p('Hệ thống tách phần hiển thị, giao tiếp mạng và xử lý luật. Hai client gửi ý định hành động tới server; server giữ trạng thái thật, kiểm tra, xử lý và trả kết quả theo vai. Module shared cung cấp hằng số, hình học bản đồ, kiểu hành động và định dạng gói tin cho cả hai phía.')
picture(QA/'architecture.png','Hình 1 Kiến trúc hai client và một server dùng chung module shared')
h('5 1 Trách nhiệm của client',2)
p('ClientMain cấu hình điểm kết nối rồi tạo MainFrame. GameClient quản lý socket và luồng nhận tin. GameController chuyển sự kiện mạng thành trạng thái giao diện, xử lý chọn kỹ năng và gửi ACTION. BoardPanel vẽ bản đồ cùng các đối tượng được phép xem; ActionPanel chứa khu vực thao tác và nhật ký riêng.')
h('5 2 Trách nhiệm của server',2)
p('TcpServer tiếp nhận kết nối, xác thực, quản lý người online và điều phối yêu cầu. Mỗi ClientHandler có luồng đọc và luồng ghi riêng. Room quản lý vòng đời trận, GameSession chứa bàn cờ, TurnResolver thực hiện các bước con. Store phụ trách dữ liệu bền vững bằng SQLite.')
h('5 3 Ranh giới phụ thuộc',2)
p('Module shared không phụ thuộc client hay server. Mã production của client không phụ thuộc server; riêng test của client dùng server để kiểm tra luồng TCP thật. ServerMain chạy độc lập. HostMain là điểm chạy tương thích cho bộ đầy đủ, mở server kèm giao diện khi cả hai phần có trên classpath.')

# PAGE 8
page('6 Tổ chức mã nguồn')
p('Project Maven có tên police-thief-java, phiên bản 2.0.0. Thư mục gốc trên máy giữ tên Game_Police_vs_Thief. Package chính được chuyển sang vn.edu.policethief; mỗi module có thư mục src/main/java và src/main/resources theo quy ước Maven.')
code('''police-thief-java
  pom.xml
  shared/src/main/java/vn/edu/policethief/shared
    GameRules.java
    model/       Role, Position, Direction, PlayerAction
    protocol/    Packet, PacketType
  server/src/main/java/vn/edu/policethief/server
    ServerMain.java, HostMain.java, Store.java
    network/     TcpServer, ClientHandler
    lobby/       Room, RoomManager
    game/        GameSession, TurnManager, TurnResolver
  client/src/main/java/vn/edu/policethief/client
    ClientMain.java
    network/     GameClient
    controller/  GameController, ClientActions
    model/       ClientState, OnlinePlayer
    ui/          MainFrame, LobbyPanel, GamePanel
  docs/          architecture.md, rules.md, protocol.md
  tools/         build.ps1, package-client.ps1
  lib/           sqlite-jdbc-3.53.4.0.jar''')
h('6 1 Phân chia các dịch vụ luật',2)
table(['Lớp','Nhiệm vụ'],[
('TurnManager','Tính deadline, bổ sung đứng yên và kiểm tra đủ hành động.'),
('ActionValidator','Kiểm tra hợp lệ phía server dựa trên trạng thái hiện tại.'),
('TurnResolver','Xử lý vị trí, bắt, bẫy, thu thập và thoát theo bước con.'),
('MapGenerator','Kiểm tra bản đồ kết nối và chọn vị trí không trùng nhau.'),
('TrapService','Đặt, kích hoạt, giữ nhịp và lọc danh sách bẫy.'),
('VisibilityService','Tầm nhìn, tọa độ được phép hiện và tình báo khu vực.'),
('WinConditionService','Kiểm tra bắt, thoát và kết thúc sau nhịp 32.')],[4.4,12])
p('Ảnh và âm thanh nằm trong resources của client. Bản đồ và schema SQL nằm trong resources của server. Các test và fixture dữ liệu cũ được đặt tại src/test tương ứng, không dùng dữ liệu tài khoản thật.')

# PAGE 9
page('7 Giao thức truyền thông TCP')
h('7 1 Định dạng gói tin',2)
p('Mỗi gói là một dòng UTF 8. Tên lệnh đứng đầu, các trường sau được mã hóa Base64 và ngăn bằng ký tự tab. Cách tổ chức này bảo toàn dấu tiếng Việt, tab và xuống dòng bên trong trường dữ liệu. Packet giới hạn dòng ở 16.384 ký tự và tối đa 32 trường tính cả tên lệnh. Giao thức không giải tuần tự hóa đối tượng Java từ mạng.')
code('''TYPE<TAB>Base64(field1)<TAB>Base64(field2)...
ACTION(matchId, round, turn, kind, first, second)
kind = MOVE | DASH | TRAP | SLIP
first, second = UP | RIGHT | DOWN | LEFT | STAY''')
p('Base64 chỉ là mã hóa biểu diễn dữ liệu, không phải mã hóa bảo mật. Kết nối TCP hiện tại chưa có TLS. Vì vậy, các thông tin đăng nhập chưa được bảo vệ trên đường truyền như khi sử dụng một kênh mã hóa; môi trường triển khai hiện tại phù hợp cho thử nghiệm cùng máy hoặc LAN.')
table(['Nhóm lệnh','Ý nghĩa'],[
('REGISTER và LOGIN','Tạo và xác thực tài khoản; trả REGISTERED hoặc LOGIN_OK.'),
('LIST và LOBBY','Yêu cầu và cập nhật danh sách người chơi online.'),
('INVITE và ACCEPT hoặc REJECT','Mời đấu và phản hồi, kiểm tra người chơi đang rảnh.'),
('ACTION và LOCKED','Gửi quyết định của nhịp và xác nhận khóa hợp lệ.'),
('STATE và UPDATE','Mở nhịp mới và gửi kết quả đã phân quyền theo vai.'),
('ACTION_REJECTED','Thông báo hành động sai, cho chọn lại nếu còn thời hạn.'),
('ROUND_END và FINISHED','Kết thúc ván hoặc cả trận, trả điểm và kết quả.'),
('AGAIN, EXIT, PING và PONG','Chơi lại, rời phòng và duy trì kết nối.')],[6,10.4])
h('7 2 Kiểm tra hành động',2)
p('Server so sánh mã trận, số ván và nhịp, trạng thái phòng và deadline trước khi nhận hành động. Mỗi người chỉ khóa được một hành động hợp lệ trong một nhịp. Trạng thái giữ bởi bẫy, số lượt kỹ năng, hướng di chuyển và ô cấm đặt bẫy đều được kiểm tra lại trên server, kể cả khi client đã kiểm tra preview.')

# PAGE 10
page('8 Luồng xử lý một nhịp')
picture(QA/'sequence.png','Hình 2 Luồng trao đổi hành động bí mật và kết quả theo từng nhịp')
h('8 1 Mở và khóa nhịp',2)
p('Room.beginTurn tạo hai ô hành động trống, tính deadline bằng thời gian server và gửi STATE cho hai phía. STATE chứa thời gian server để client hiệu chỉnh đồng hồ hiển thị. Người chơi xem đường dự kiến rồi nhấn Khóa hoặc Enter. Client khóa điều khiển ngay sau khi gửi; server xác nhận bằng LOCKED hoặc trả ACTION_REJECTED để chọn lại.')
h('8 2 Phân xử và chuyển nhịp',2)
p('Khi nhận đủ hành động, server xử lý ngay; nếu hết 8 giây, hành động còn thiếu được thay bằng đứng yên. UPDATE chứa đường thực tế của bản thân và các thông tin được phép biết. Nếu chưa kết thúc ván, server chuyển qua pha phân xử ngắn để hiển thị rồi mở nhịp mới. Nếu đã kết thúc, gửi ROUND_END và chuyển ván hoặc FINISHED.')
h('8 3 Đồng bộ nhiều luồng',2)
p('Luồng đọc client và bộ hẹn giờ có thể cùng tác động tới trận, vì vậy các thay đổi được thực hiện dưới monitor của TcpServer. Nhờ đó, việc kiểm tra đủ hành động và chấm kết quả không chạy xen kẽ tùy ý. Luồng ghi dùng hàng đợi riêng, giúp tác vụ gửi dữ liệu tách khỏi việc đọc và kiểm tra yêu cầu.')

# PAGE 11
page('9 Thuật toán xử lý đồng thời')
h('9 1 Mô hình bước con',2)
p('Server tạo đường đi dự kiến cho từng nhân vật từ trạng thái chính thức. Đi bộ có tối đa một bước, chạy nhanh có tối đa hai bước thẳng và luồn lách có tối đa hai bước vuông góc. Nhân vật không còn bước được giữ tại vị trí cuối. Bẫy đang hoạt động có thể cắt ngắn phần đường của trộm.')
code('''kiemTraHopLe(hanhDongCanhSat, hanhDongTrom)
taoDuongDiChoHaiNhanVat()
datBayMoiVoiHieuLucTuNhipSau()
for moi buocCon:
    luuViTriTruocBuoc()
    capNhatViTriHaiBenDongThoi()
    if cungO() or doiCho():
        ketThucVan(CAPTURE); break
    if tromDiVaoBayDangHoatDong():
        catDuongConLai(); giuTromNhipSau()
    thuThapBauVatTrenODiQua()
    if tromCoBauVatVaDenCua():
        ketThucVan(ESCAPE); break
if chuaKetThucVaNhipBang32():
    ketThucVan(TIMEOUT)
locThongTinTheoVaiVaGuiKetQua()''')
h('9 2 Các trường hợp biên',2)
table(['Tình huống','Cách xử lý'],[
('Hai nhân vật đổi chỗ','Bắt giữ ngay trong bước con tương ứng.'),
('Trộm chạy qua cảnh sát đứng yên','Bị bắt ở bước đi vào ô cảnh sát, không chạy xuyên.'),
('Bẫy ở bước đầu của chạy nhanh','Dừng trộm, cảnh sát vẫn có thể đi bước còn lại để bắt.'),
('Báu vật nằm ở ô trung gian','Nhặt ngay khi đi qua; có thể tiếp tục đến cửa.'),
('Tới cửa và bị bắt cùng bước','Ưu tiên CAPTURE, không công nhận thoát.'),
('Thoát ở bước đầu nhịp 32','Thoát thắng ngay, không tiếp tục bước hoặc xử lý TIMEOUT.')],[6,10.4])
p('Việc kiểm tra theo bước con tránh chỉ so sánh vị trí cuối nhịp, vốn có thể bỏ sót tình huống đổi chỗ hoặc chạy xuyên qua đối thủ. Thứ tự ưu tiên được dùng thống nhất cho đi bộ, chạy nhanh và luồn lách.')

# PAGE 12
page('10 Thiết kế cơ sở dữ liệu')
p('SQLite lưu tập trung trên server tại data/museumheist.db. Thiết kế tách tài khoản, trận đấu và quan hệ người tham gia, giúp truy vấn lịch sử theo tài khoản và hạn chế lặp thông tin trận. Các ràng buộc khóa ngoại, khóa duy nhất và CHECK ngăn một số trạng thái dữ liệu không hợp lệ.')
picture(QA/'er.png','Hình 3 Các bảng chính và quan hệ lưu trữ trận đấu')
table(['Bảng','Trường và ý nghĩa'],[
('users','name là khóa chính; salt, password_hash; points, wins, losses, played.'),
('matches','id là khóa chính; rounds, winner, started_at, ended_at, reason.'),
('match_players','Khóa chính ghép match_id và user_name; seat, score. Mỗi trận có hai chỗ ngồi.'),
('metadata','key và value; lưu thông tin quản lý, nhập dữ liệu và phiên bản.')],[3.3,13.1])
h('10 1 Mật khẩu và truy vấn',2)
p('Mật khẩu không lưu trực tiếp. Store sử dụng PBKDF2WithHmacSHA256 với 120.000 vòng, salt 16 byte và kết quả băm 256 bit. Khi đăng nhập, server tính lại và so sánh kết quả. Các truy vấn có tham số sử dụng PreparedStatement. Xếp hạng và lịch sử được phân trang, mỗi trang 10 bản ghi.')
h('10 2 Transaction và khôi phục ghi',2)
p('Lưu trận, người tham gia và cập nhật thống kê được thực hiện trong một transaction. Nếu có lỗi, rollback để không cập nhật dở dang. Kiểm tra mã trận trước khi ghi giúp tránh lưu trùng. Kết quả ghi thất bại được giữ trong hàng đợi bộ nhớ để thử lại; cơ chế này không thay thế một hàng đợi bền vững khi tiến trình server bị dừng đột ngột.')

# PAGE 13
page('11 Giao diện và điều khiển')
h('11 1 Đăng nhập và sảnh chờ',2)
p('Màn hình xác thực cho phép đăng ký, đăng nhập và cấu hình địa chỉ server. Đăng ký yêu cầu xác nhận mật khẩu. Sau đăng nhập, sảnh hiển thị danh sách người online, điểm và trạng thái để lựa chọn đối thủ. Lời mời có thời hạn và chỉ bắt đầu trận khi được chấp nhận.')
picture(ROOT/'work/preview-v2/lobby.png','Hình 4 Sảnh chờ với danh sách online và thao tác thách đấu',15.5)
h('11 2 Thành phần màn hình chơi',2)
p('Thanh thông tin hiển thị vai, ván, nhịp, điểm và thời gian. Bản đồ ở giữa biểu diễn tường, bệ báu vật, hai cửa và nhân vật. Cột bên phải chứa nhiệm vụ, kỹ năng, nút chọn hướng, đường dự kiến và nhật ký riêng. Thanh điều hướng cho phép xem luật, xếp hạng, lịch sử và điều chỉnh âm thanh.')
table(['Thao tác','Chức năng'],[
('Phím mũi tên hoặc nút hướng','Chọn hướng, chưa tự gửi hành động.'),
('Enter hoặc Khóa hành động','Gửi và khóa quyết định của nhịp.'),
('Space hoặc nút giữa','Chọn đứng yên.'),
('Nhấp ô kề trên bản đồ','Chọn hướng; với bước hai của luồn lách, tính từ ô sau bước đầu.')],[7.3,9.1])

# PAGE 14
page('12 Minh họa kỹ năng trên giao diện')
picture(QA/'thief-detail.png','Hình 5 Trộm chọn luồn lách và xem đường dự kiến rẽ vuông góc',15.4)
p('Trộm tại A7 chọn phải rồi lên. Nét đứt biểu diễn đường rẽ dự kiến; nút kỹ năng cho biết số lượt còn lại. Hai cửa A7 và G1 luôn hiện. Server xác định đường thực tế khi phân xử.')
picture(QA/'police-detail.png','Hình 6 Cảnh sát chọn ô đặt bẫy và xem các dấu vết được phép biết',15.4)
p('Vòng có dấu chéo đỏ là bẫy đã đặt; vòng tại ô kề là mục tiêu dự kiến. Cảnh sát không nhận trạng thái báu vật bị lấy. Nút kỹ năng bị khóa khi hết lượt hoặc đã gửi hành động.')

# PAGE 15
page('13 Quyền xem thông tin trên hai client')
p('Quyền xem được áp dụng tại server trước khi mã hóa gói tin. Với cảnh sát, relicMask, carrying và slipsLeft không được cung cấp giá trị thật. Với trộm, dấu vết cảnh sát quan sát và tình báo được để trống; bẫy chưa kích hoạt không được gửi. Cách làm này bảo vệ thông tin cả khi người dùng kiểm tra gói tin ngoài giao diện.')
source=Path('C:/Users/Admin/AppData/Local/Temp/codex-clipboard-52313d2b-2fcc-4eff-857b-967fa37b07f1.png')
if source.exists():
    picture(source,'Hình 7 Hai client chạy cùng máy và cùng thấy đối thủ trong tầm nhìn',16.2)
else:
    picture(ROOT/'work/preview-v2/trap.png','Hình 7 Cảnh sát thấy trộm sau khi bẫy kích hoạt',16.2)
h('13 1 Giải thích tình huống nhìn thấy nhau',2)
p('Ở ảnh hai client, cảnh sát tại B1 và trộm tại C1. Hai ô cùng hàng, cách một bước và không bị tường chắn nên cả hai được thấy đối thủ theo luật. Dòng ĐỐI THỦ ĐANG LỘ DIỆN thể hiện trạng thái này. Đây không phải việc công khai toàn bộ bản đồ; sau khi ra khỏi phạm vi ở nhịp xử lý kế tiếp, tọa độ đối thủ lại được ẩn.')
h('13 2 Phát hiện từ bẫy và tình báo',2)
p('Bẫy cho cảnh sát tọa độ chính xác tại thời điểm trộm mắc và trong nhịp bị giữ; trộm cũng biết vị trí bẫy vừa gặp. Tin tình báo chỉ trả khu vực ở nhịp 8, 16 hoặc 24, không được sử dụng như tọa độ theo dõi liên tục. Khi kết thúc ván, hệ thống mới công khai vị trí cuối cùng của cả hai nhân vật.')
h('13 3 Điều khiển sau đổi nhịp',2)
p('Phím hướng, Space và Enter được gắn cho cả thành phần đang nhận focus trong vùng chơi. Nhờ đó, ô nhật ký và vùng cuộn không chặn điều khiển sau khi nút Khóa mất focus. Sang nhịp mới, trạng thái khóa được xóa và lựa chọn về Đi bộ hoặc Đứng yên; riêng trộm đang mắc bẫy chỉ được gửi đứng yên.')

# PAGE 16
page('14 Kiểm thử và kết quả')
p('Các bộ kiểm thử được chạy trên mã nguồn phiên bản 2. Báo cáo kiểm chứng tại docs/verification.md và các báo cáo Surefire ghi nhận Maven build thành công cho shared, server và client. Tổng cộng có 9 phương thức kiểm thử JUnit trong ServerSuite và ClientSuite. Các phương thức này gọi các bộ kiểm tra thực thi nhiều assertions, không phải 25.000 test case JUnit riêng biệt.')
table(['Bộ kiểm tra','Số assertions','Nội dung chính'],[
('EngineTest','24.650','Luật mới, 1.000 bản đồ và 1.000 ván ngẫu nhiên.'),
('StoreTest','43','Nhập dữ liệu cũ, transaction và rollback SQLite.'),
('NetworkTest','43','TCP, hết giờ, đổi vai, đầu hàng và thử ghi lại.'),
('NewRulesNetworkTest','18','Bẫy, giữ nhịp, lọc thông tin, tầm nhìn và tình báo qua TCP.'),
('MultiClientTest','35','8 tài khoản, 2 trận độc lập, 4 người chờ.'),
('PaginationTest','400','Phân trang lịch sử và bảng xếp hạng qua TCP.'),
('UiTest','126','Kỹ năng, trạng thái giữ, phím hướng và Enter khi focus nhật ký.'),
('ClientFlowTest','13','Đăng ký, đăng nhập, mời đấu, chơi và mất kết nối qua client thật.'),
('ClientConfigTest','Kiểm tra cấu hình','Địa chỉ mặc định và ghi đè IP, cổng.')],[5.1,2.6,8.7])
p('Tám bộ có bộ đếm ở trên ghi nhận tổng cộng 25.328 assertions đạt. ClientConfigTest được ghi nhận riêng. Dữ liệu kiểm thử được tạo tại work, không dùng database tài khoản thật. Test NetworkTest chủ động tạo lỗi ghi SQLite để xác minh rollback và retry; stack trace của tình huống này không đồng nghĩa toàn bộ test thất bại.')
h('14 1 Các kết quả đáng chú ý',2)
bullets(['Bắt giữ được phát hiện khi đổi chỗ, khi chạy qua cảnh sát và khi vừa đến cửa.',
         'Bẫy chỉ có hiệu lực từ nhịp sau, giữ đúng một nhịp và không tự kích hoạt khi đứng yên.',
         'Không gửi bẫy chưa biết cho trộm hoặc trạng thái báu vật cho cảnh sát.',
         'Hai vai chạy nhanh được; luồn lách kiểm tra hai hướng vuông góc và vật cản.',
         'Build bằng Maven và bằng bộ script JDK đều thành công.'])

# PAGE 17
page('15 Hướng dẫn cài đặt và chạy thử')
h('15 1 Yêu cầu môi trường và build',2)
p('Máy chạy cần Java 17 trở lên. Máy build cần JDK; có thể dùng build.bat mà không cần Maven hoặc tải thêm SQLite JDBC vì thư viện đã nằm trong lib. Chạy mvn package nếu muốn build ba module và thực thi suite Maven. Chạy test.bat để kiểm tra qua bộ JAR đóng gói. Test Swing cần desktop, test mạng cần quyền kết nối localhost.')
code('''build.bat
test.bat
mvn package''')
p('Kết quả gồm MuseumHeist.jar cho bộ đầy đủ, build/police-thief-server.jar cho server độc lập và build/police-thief-client.jar cho client. JAR module Maven nằm tại target của từng module và không gộp phụ thuộc. Giữ lib bên cạnh bộ đầy đủ để nạp SQLite JDBC.')
h('15 2 Chơi hai người trên cùng máy',2)
bullets(['Mở run-server.bat và giữ cửa sổ server hoạt động ở cổng 5555.',
         'Mở run-client.bat hai lần để có hai cửa sổ game.',
         'Cấu hình cả hai tới 127.0.0.1 và cổng 5555.',
         'Đăng nhập bằng hai tài khoản khác nhau, gửi và chấp nhận lời mời.',
         'Nhấp cửa sổ người chơi tương ứng trước khi điều khiển bằng bàn phím.'])
h('15 3 Chơi qua mạng LAN',2)
p('Máy chủ chạy run-server.bat hoặc May1-ChuPhong.bat để mở server kèm giao diện. Máy khách nhập IP LAN máy chủ và cổng tương ứng. Hai máy phải dùng cùng phiên bản giao thức và kết nối được TCP tới server. Dòng thông báo server đang nghe cổng 5555 là trạng thái chờ bình thường, không phải lỗi.')
h('15 4 Một số tình huống thường gặp',2)
table(['Hiện tượng','Kiểm tra hoặc thao tác'],[
('Không kết nối được','Xác nhận server còn mở, đúng IP/cổng và đường mạng cho phép TCP.'),
('Tài khoản đang online','Dùng hai tài khoản khác nhau hoặc đóng phiên đăng nhập trước.'),
('Thấy đối thủ giữa ván','Kiểm tra tầm nhìn cùng hàng/cột hoặc cơ chế bẫy.'),
('Bị khóa hướng ở nhịp mới','Kiểm tra trộm có đang phải đứng yên do mắc bẫy hay không.')],[5.3,11.1])

# PAGE 18
page('16 Đánh giá và hướng phát triển')
h('16 1 Kết quả đạt được',2)
p('Đề tài đã xây dựng được trò chơi hai người với cơ chế hành động bí mật, phân xử đồng thời và đổi vai sau mỗi ván. Các module tách rõ phần dùng chung, server và client. Server thực thi luật, quản lý nhiều phòng và lọc thông tin theo vai; giao diện hỗ trợ quan sát bản đồ, preview đường đi, thao tác kỹ năng và theo dõi sự kiện.')
p('Phần lưu trữ có xác thực mật khẩu bằng hàm dẫn xuất khóa, transaction ghi kết quả và hỗ trợ dữ liệu cũ. Các kiểm tra tự động bao phủ luật, TCP, phân trang, nhiều phòng và luồng giao diện thực. Đây là bằng chứng chức năng trong phạm vi các kịch bản đã kiểm tra, không thay thế đánh giá tải lớn hoặc khảo sát cân bằng khi chơi lâu dài.')
h('16 2 Hạn chế hiện tại',2)
bullets(['Bản đồ cố định 7 × 7, chưa có trình chỉnh sửa bản đồ hoặc lựa chọn nhiều bản đồ.',
         'Chưa có TLS; Base64 không bảo vệ bí mật dữ liệu trên đường truyền.',
         'Mất kết nối đang được tính đầu hàng, chưa hỗ trợ reconnect và khôi phục trận.',
         'Kết quả chờ ghi lại nằm trong bộ nhớ; chưa có hàng đợi bền vững khi server dừng.',
         'Kiểm thử nhiều client hiện chứng minh các kịch bản nhỏ, chưa xác lập sức chứa cực đại.',
         'Các thông số 32 nhịp, 8 giây và số lượt kỹ năng cần được tiếp tục đánh giá cân bằng bằng chơi thử.'])
h('16 3 Hướng phát triển',2)
p('Ưu tiên bổ sung kênh truyền mã hóa, cơ chế khôi phục phiên và hàng đợi ghi kết quả bền vững. Có thể mở rộng giao thức với trường phiên bản rõ ràng, thêm log phục vụ chẩn đoán và test tải có kiểm soát. Ở phần gameplay, hướng phát triển là nhiều bản đồ, chế độ quan sát sau trận, phát lại diễn biến và cân bằng kỹ năng dựa trên dữ liệu các ván chơi thử.')
h('16 4 Kết luận',2)
p('Sản phẩm đáp ứng yêu cầu chính của bài tập lớn lập trình mạng: giao tiếp socket, tổ chức nhiều luồng, đồng bộ trạng thái, giao thức ứng dụng, xử lý lỗi kết nối và lưu trữ dữ liệu. Bài toán trò chơi có thông tin ẩn làm rõ vai trò của server như trọng tài và cho thấy việc bảo vệ thông tin cần thực hiện ở tầng dữ liệu gửi đi, không chỉ ở lớp hiển thị.')

# PAGE 19
page('17 Tài liệu tham khảo và đối chiếu')
p('Các nội dung mô tả triển khai và kết quả trong báo cáo được đối chiếu với các tài liệu, mã nguồn và đầu ra kiểm thử đi kèm project. Danh sách dưới đây giúp tra cứu lại thiết kế và tái lập các bước kiểm tra.')
for i,title,desc in [
    (1,'README.md','Cấu trúc Maven, cách build và cách chạy các bộ JAR.'),
    (2,'docs/rules.md','Luật chơi phiên bản 2, kỹ năng và cơ chế thông tin.'),
    (3,'docs/architecture.md','Phân chia trách nhiệm giữa shared, server và client.'),
    (4,'docs/protocol.md','Định dạng TCP, ACTION, STATE, UPDATE và trường được lọc theo vai.'),
    (5,'docs/verification.md','Thống kê assertions và kết quả kiểm thử đã thực hiện.'),
    (6,'shared/src/main/java/vn/edu/policethief/shared/GameRules.java','Hình học bản đồ, hằng số và kiểm tra chung.'),
    (7,'server/src/main/java/vn/edu/policethief/server/game','Mã xử lý luật, bản đồ, bẫy, tầm nhìn và điều kiện thắng.'),
    (8,'server/src/main/resources/db/schema.sql','Schema SQLite, khóa ngoại và chỉ mục.'),
    (9,'server/src/test và client/src/test','Các bộ kiểm thử chạy trực tiếp và suite JUnit.'),
    (10,'server/target/surefire-reports và client/target/surefire-reports','Đầu ra Maven ghi nhận 6 test server và 3 test client, không lỗi.'),
    (11,'work/preview-v2','Ảnh minh họa giao diện xuất từ ứng dụng.'),
    (12,'Ảnh hai client chạy cùng máy','Ảnh chụp thực tế trong quá trình thử game; minh họa tầm nhìn B1 và C1.')]:
    pp=p(f'[{i}] {title}',True);pp.alignment=WD_ALIGN_PARAGRAPH.LEFT
    pp=p(desc);pp.paragraph_format.space_after=Pt(9)
settings=doc.settings.element
upd=OxmlElement('w:updateFields');upd.set(qn('w:val'),'true');settings.append(upd)
# Remove inherited template rules and theme overrides before Word updates fields.
for root in [doc.element, doc.styles.element]:
    for border in list(root.xpath('.//w:pBdr')):border.getparent().remove(border)
    for rfonts in root.xpath('.//w:rFonts'):
        for key in list(rfonts.attrib):
            if key.endswith('Theme'):del rfonts.attrib[key]
    for color in root.xpath('.//w:color'):
        for key in list(color.attrib):
            if key.endswith('themeColor') or key.endswith('themeTint') or key.endswith('themeShade'):del color.attrib[key]
        color.set(qn('w:val'),'000000')
target=OUT/'Bao_cao_Canh_sat_va_Trom.docx'
doc.save(target)
print(target)
print('Planned pages: 19; check final pagination after rendering.')
