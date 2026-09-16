"""Query Minecraft's real server-list protocol; does not log in or create a player."""
import socket,struct,json,sys
def var(n):
 b=bytearray()
 while True:
  k=n&127;n>>=7;b.append(k|(128 if n else 0))
  if not n:return bytes(b)
def readvar(s):
 n=0
 for i in range(5):
  b=s.recv(1)
  if not b:raise EOFError()
  n|=(b[0]&127)<<(7*i)
  if b[0]<128:return n
 raise ValueError('varint too long')
with socket.create_connection(('127.0.0.1',25566),timeout=10) as s:
 host=b'localhost';h=b'\x00'+var(763)+var(len(host))+host+struct.pack('>H',25566)+b'\x01';s.sendall(var(len(h))+h+b'\x01\x00')
 readvar(s);assert readvar(s)==0;length=readvar(s);data=b''
 while len(data)<length:
  part=s.recv(length-len(data))
  if not part:raise EOFError()
  data+=part
 result=json.loads(data.decode('utf8'))
 if len(sys.argv)>1:
  from pathlib import Path
  Path(sys.argv[1]).write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf8')
 print(json.dumps({'version':result['version'],'players':result['players'],'description':result.get('description')},ensure_ascii=False))
