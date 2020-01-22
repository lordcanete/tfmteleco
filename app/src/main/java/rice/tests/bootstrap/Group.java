package rice.tests.bootstrap;

import java.util.Date;
import java.util.Vector;

import rice.p2p.commonapi.Id;
import rice.p2p.past.ContentHashPastContent;
import rice.p2p.past.PastContent;
import rice.p2p.past.PastException;
import rice.p2p.past.gc.GCPast;
import rice.p2p.past.gc.GCPastContent;
import rice.p2p.past.gc.GCPastContentHandle;
import rice.p2p.past.gc.GCPastMetadata;

/**
 * Created by FPiriz on 10/6/17.
 */
public class Group extends ContentHashPastContent implements GCPastContent
{
    /**
     * Store the content.
     *
     * Note that this class is Serializable, so any non-transient field will
     * automatically be stored to disk.
     */
    private Vector<Id> components;
    private Id groupId;
    private String name;
    private Date lastModification;
    private long version;
    private GCPastMetadata metadata;

    @Override
    public long getVersion()
    {
        return this.version;
    }

    @Override
    public GCPastContentHandle getHandle(GCPast local, long expiration)
    {
        return new GroupHandle(local.getLocalNodeHandle(),getId(),getVersion(),expiration);
    }

    @Override
    public GCPastMetadata getMetadata(long expiration)
    {
        this.metadata=new GCPastMetadata(expiration);
        return metadata;
    }

    /**
     * Takes an environment for the timestamp
     * An IdFactory to generate the hash
     * The content to be stored.
     *
     * @param id to generate a hash of the content
     */
    public Group(Id id, String name) {
        super(id);
        this.groupId=id;
        this.name=name;
        this.lastModification=new Date();
        components=new Vector<>();
        this.version=1;
    }

    /**
     * A descriptive toString()
     */
    @Override
    public String toString() {
        StringBuilder list= new StringBuilder();
        for (Id component : components)
        {
            list.append(", ").append(component);
        }
        return "List: [ "+list.toString()+" ] Last updated "+getDate().toString();
    }

    /**
     * Insert new element in the list
     */
    public Group insert(Id id)
    {
        System.out.println("insert( "+id+" )");
        this.components.add(id);
        this.lastModification=new Date();
        this.version++;
        return this;
    }

    /**
     * Delete an element by the given id
     */
    public Group remove(Id id)
    {
        int index=this.components.indexOf(id);
        if(index!=-1){
            this.components.remove(index);
            this.lastModification=new Date();
            this.version++;
        }
        return this;
    }

    /**
     * Return the list of id of the group
     */
    public Vector<Id> getComponents()
    {
        return this.components;
    }

    /**
     * Return group Id
     */
    public Id getGroupId()
    {
        return this.groupId;
    }

    public String getGroupName()
    {
        return this.name;
    }

    public Group setGroupName(String name)
    {
        this.name=name;
        this.lastModification=new Date();
        this.version++;
        return this;
    }
    /**
     * Return group date of last modification
     */
    public Date getDate()
    {
        return this.lastModification;
    }

    @Override
    public PastContent checkInsert(Id id, PastContent existingContent) throws PastException
    {
        Group ret=this;
        if(existingContent!=null)
        {
            Group existing = (Group) existingContent;
            if (existing.getVersion() < this.getVersion())
                ret= this;
            else
            {
                throw new PastException("You are trying to insert an object with a lower version"+
                        " number than the existing one");

            }

        }
        if (!id.equals(getId())) {
            throw new PastException("ContentHashPastContent: can't insert, content hash incorrect");
        }
        return ret;
    }

    @Override
    /**
     * States if this content object is mutable. Mutable objects are not subject to dynamic caching in Past.
     *
     * @return true if this object is mutable, else false
     */
    public boolean isMutable() {
        return true;
    }
}
