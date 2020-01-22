package us.tfg.p2pmessenger.model;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.gc.GCPastContentHandle;

/**
 * Created by FPiriz on 19/6/17.
 */
public class ManejadorGrupoCifrado implements GCPastContentHandle
{
    static final long serialVersionUID = 5695389735768047625L;

    private NodeHandle nodeHandle;
    private Id id;
    private long version;
    private long expiration;


    public ManejadorGrupoCifrado(NodeHandle handle, Id id, long version, long expiration)
    {
        this.id=id;
        this.nodeHandle=handle;
        this.expiration=expiration;
        this.version=version;
    }
    @Override
    public Id getId()
    {
        return this.id;
    }

    @Override
    public long getVersion()
    {
        return version;
    }

    @Override
    public NodeHandle getNodeHandle()
    {
        return nodeHandle;
    }

    @Override
    public long getExpiration()
    {
        return expiration;
    }
}
