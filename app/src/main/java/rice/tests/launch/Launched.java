package rice.tests.launch;

/**
 * Created by FPiriz on 20/6/17.
 */
public class Launched
{
    public static void main(String args[])
    {
        System.out.println("I'm the launched class");
        System.out.println("List of arguments");
        for(int i=0;i<args.length;i++)
        {
            System.out.println("arg 1 -> "+args[i]);
        }
    }
}
