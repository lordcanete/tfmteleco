package us.tfg.p2pmessenger.controller;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import rice.p2p.commonapi.Id;

/**
 * Observador que recibira la respuesta de si se ha obtenido
 * respuesta o no a la peticion de eco
 */
public class PingWithTimeout
{

    private ObservadorPing observador;
    private Timer timer;

    private Date inicio;
    private String carga;

    public PingWithTimeout(ObservadorPing observador, long timeout, final Id objetivo, String carga) {
        System.out.println(getClass()+".<init>("+observador+", timeout = "
                +timeout+", "+objetivo.toStringFull()+", "+carga+")");

        this.observador =observador;

        this.inicio=new Date();
        this.carga=carga;
        // Start the timer.

        Timer timer=new Timer(true);
        timer.schedule(new TimerTask() {

            private PingWithTimeout observador;
            @Override
            public void run()
            {
                System.out.println(getClass()+".run()");
                this.observador.observador.notificarPing(us.tfg.p2pmessenger.model.Mensaje.PING_TIMEOUT,objetivo,getCarga());
            }

            public TimerTask init(PingWithTimeout observador)
            {
                System.out.println(getClass()+".init("+observador+")");
                this.observador=observador;
                return this;
            }

        }.init(this), timeout);

    }

    public void cancel()
    {
        System.out.println(getClass()+".cancel()");
        timer.cancel();
    }

    public Date getInicio()
    {
        System.out.println(getClass()+".getInicio()");
        return this.inicio;
    }

    public String getCarga()
    {
        System.out.println(getClass()+".getCarga()");
        return this.carga;
    }

}