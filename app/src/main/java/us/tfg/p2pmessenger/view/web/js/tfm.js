var jsConnector = {
    notificarError: function(mensajeError){
        mostrarBloqueNotificacion(idBloque_notifError, idBloque_textoError, mensajeError);
    },
    notificarUsuarioCreadoCorrectamente: function(){        
        mostrarBloqueNotificacionOK(idBloque_notifOk);
        this.comprobarEstadoCallback();
    },
    comprobarEstadoCallback: function () {
        javaConnector.comprobarEstado();
    }
};

function getJsConnector() {
    return jsConnector;
};

function mostrarBloqueNotificacion(idBloque, idUbicacionMensaje = null, mensaje = null){
    if (idUbicacionMensaje != null && mensaje != null){
        $(idUbicacionMensaje).text(mensaje);
    }

    $(idBloque).addClass("d-flex");
    $(idBloque).removeClass("d-none");
}

function ocultarBloqueNotificacion(idBloque){
    $(idBloque).addClass("d-none");
    $(idBloque).removeClass("d-flex");
}
