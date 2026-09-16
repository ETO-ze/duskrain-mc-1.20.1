import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import net.minecraftforge.renamer.relocated.org.objectweb.asm.*;
import net.minecraftforge.renamer.relocated.org.objectweb.asm.commons.*;
/** Private dev-only conversion of globally unique SRG member names inside nested libraries. */
public class RemapNested {
 public static void main(String[] a)throws Exception{
  Map<String,String> members=new HashMap<>();for(String line:Files.readAllLines(Path.of(a[0]))){String[] v=line.split(" ");if(v[0].equals("MD:")||v[0].equals("FD:")){String from=v[1].substring(v[1].lastIndexOf('/')+1),to=v[v[0].equals("MD:")?3:2];to=to.substring(to.lastIndexOf('/')+1);if(from.matches("[mf]_\\d+_")){String prior=members.putIfAbsent(from,to);if(prior!=null&&!prior.equals(to))throw new IllegalStateException("Ambiguous SRG name");}}}
  Remapper remap=new Remapper(){public String mapMethodName(String o,String n,String d){return members.getOrDefault(n,n);}public String mapFieldName(String o,String n,String d){return members.getOrDefault(n,n);}};
  try(ZipFile in=new ZipFile(a[1]);ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(Path.of(a[2])))){var e=in.entries();while(e.hasMoreElements()){var entry=e.nextElement();byte[] data=in.getInputStream(entry).readAllBytes();if(entry.getName().endsWith(".class")){ClassReader r=new ClassReader(data);ClassWriter w=new ClassWriter(0);r.accept(new ClassRemapper(w,remap),0);data=w.toByteArray();}out.putNextEntry(new ZipEntry(entry.getName()));out.write(data);out.closeEntry();}}
 }
}
