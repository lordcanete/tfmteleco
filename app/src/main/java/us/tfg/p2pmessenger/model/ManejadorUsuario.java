package us.tfg.p2pmessenger.model;

import rice.p2p.commonapi.Id;
import rice.p2p.commonapi.NodeHandle;
import rice.p2p.past.PastContentHandle;
import rice.p2p.past.gc.GCPastContentHandle;

/**
 * Clase utilizada por pastry para crear un enlace a un objeto
 * almacenado en la red
 */
public class ManejadorUsuario implements GCPastContentHandle, PastContentHandle
{
    static final long serialVersionUID = 7964919460967037042L;

    public static final short TYPE=315;
    private NodeHandle nodeHandle;
    private Id id;
    private long version;
    private long expiration;


    public ManejadorUsuario(NodeHandle handle, Id id, long version, long expiration)
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
    public long getExpiration()
    {
        return expiration;
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
}
