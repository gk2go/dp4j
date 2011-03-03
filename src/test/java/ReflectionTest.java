
import java.util.*;

public class ReflectionTest
{
@com.dp4j.Reflect(all=false)
 public static void main(String[] args)
      {
           Set<String> myStr = new HashSet<String>();
           myStr.add("obj1");
           Iterator itr = myStr.iterator();
           System.out.println(itr.hasNext());
      }
}
