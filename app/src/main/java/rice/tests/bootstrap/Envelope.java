package rice.tests.bootstrap;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.Message;

/**
 * Created by FPiriz on 19/6/17.
 */
public class Envelope implements Message
{
    public static final int APAGAR=10;
    private int type;
    private String content;

    /**
     * Where the Envelope came from.
     */
    Id from;
    /**
     * Where the Envelope is going.
     */
    Id to;

    public Envelope(Id from,Id to,String content,int type)
    {
        this.to=to;
        this.from=from;
        this.type=type;
        this.content=content;
    }

    public String toString() {
        return "Message:"+this.getType()+" [ "+from+" -> "+to+" --- "+content+" ]";
    }

    public int getType()
    {
        return this.type;
    }

    @Override
    public int getPriority()
    {
        return Message.LOW_PRIORITY;
    }
}
