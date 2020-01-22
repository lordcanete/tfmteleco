package rice.tests.launch;

/**
 * Created by FPiriz on 20/6/17.
 */
public class Launcher
{
    public static void main(String args[])
    {
        System.out.println("Launching class rice.tests.launch.Launched");
        String arguments[]={"9001","192.168.1.42","9001"};
        try
        {
            rice.tests.bootstrap.Component.main(arguments);
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        System.out.println("After the executing the Component");
    }
}
