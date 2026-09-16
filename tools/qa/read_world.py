"""Read Java 1.20.1 Anvil NBT without modifying a world (standard library only)."""
from pathlib import Path
import struct, zlib, gzip, io

class Reader:
    def __init__(self,data): self.f=io.BytesIO(data)
    def number(self,fmt): return struct.unpack('>'+fmt,self.f.read(struct.calcsize(fmt)))[0]
    def text(self): return self.f.read(self.number('H')).decode('utf-8')
    def payload(self,t):
        if t in (1,2,3,4,5,6): return self.number({1:'b',2:'h',3:'i',4:'q',5:'f',6:'d'}[t])
        if t==7: return self.f.read(self.number('i'))
        if t==8: return self.text()
        if t==9:
            kind,n=self.number('B'),self.number('i');return [self.payload(kind) for _ in range(n)]
        if t==10:
            d={}
            while (kind:=self.number('B')): name=self.text();d[name]=self.payload(kind)
            return d
        if t in (11,12):
            n=self.number('i');fmt='i' if t==11 else 'q';return struct.unpack('>'+str(n)+fmt,self.f.read(n*(4 if t==11 else 8)))
        raise ValueError(t)
    def root(self):
        t=self.number('B');self.text();return self.payload(t)

def chunks(region_dir):
    for file in sorted(Path(region_dir).glob('r.*.*.mca')):
        raw=file.read_bytes()
        for i in range(1024):
            entry=int.from_bytes(raw[i*4:i*4+4],'big');offset=entry>>8
            if not offset: continue
            start=offset*4096;n=int.from_bytes(raw[start:start+4],'big');kind=raw[start+4];data=raw[start+5:start+4+n]
            if kind==2: data=zlib.decompress(data)
            elif kind==1: data=gzip.decompress(data)
            elif kind!=3: raise ValueError(('Unsupported region compression',kind))
            yield Reader(data).root()

def packed(values,index,bits):
    per=64//bits;return ((values[index//per]&0xffffffffffffffff)>>((index%per)*bits))&((1<<bits)-1)

def state(chunk,x,y,z):
    for section in chunk['sections']:
        if section['Y']!=y//16: continue
        bs=section.get('block_states',{});palette=bs.get('palette',[])
        if not palette:return {'Name':'minecraft:air'}
        idx=0 if len(palette)==1 else packed(bs['data'],(y%16)*256+(z%16)*16+x%16,max(4,(len(palette)-1).bit_length()))
        return palette[idx]
    return {'Name':'minecraft:air'}

if __name__=='__main__':
    import sys,json
    positions=[(int(a),int(b)) for a,b in (s.split(',') for s in sys.argv[2:])]
    for chunk in chunks(sys.argv[1]):
        for x,z in positions:
            if x//16==chunk['xPos'] and z//16==chunk['zPos']:
                print(x,z,json.dumps([(y,state(chunk,x,y,z)) for y in range(60,140) if state(chunk,x,y,z)['Name']!='minecraft:air'],ensure_ascii=False))
